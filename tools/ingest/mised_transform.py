"""Pure, I/O-free transform functions from raw MISeD JSONL records into the
Mongo document shapes used by Diagna-Logic (`meetings` and `dialogs` collections).

Every function here takes plain dicts/strings in and returns plain dicts out —
no file access, no network, no database calls. That is deliberate: it makes
the one genuinely tricky part of this dataset (see below) exhaustively unit
testable, and it is the anti-drift contract against the Java POJOs (see
`ingest_mised.py --emit-fixtures`, which serializes a few of these documents
verbatim for a Java-side mapping test).

THE CRITICAL GOTCHA — proto3-JSON default omission
---------------------------------------------------
The MISeD source files are protobuf messages serialized to JSON, and proto3
JSON serialization OMITS any field left at its default value:
  - bool fields default to `false`      -> `isUnanswerable`/`isContextDependent`
                                            are simply absent when false
  - int32 fields default to `0`         -> `startIndex`/`endIndex` are absent
                                            when they would be 0

This was verified against the full dataset before writing this module:
  - 1,274 of 4,161 turns carry `isUnanswerable: true`; the other 2,887 have no
    `isUnanswerable` key at all (not `false` — ABSENT).
  - 137 of 8,651 attribution ranges omit `startIndex`; 49 omit `endIndex`.

Treating an absent key as anything other than its proto3 default (`False` /
`0`) would silently corrupt the data — e.g. dropping a valid citation to
segment 0, or miscounting unanswerable turns. Every place below that reads
one of these fields uses `.get(key, default)`, never a bare `[key]` or a
`is None` check.
"""
from __future__ import annotations

import json
from typing import Any

# --- query type -------------------------------------------------------------

_QUERY_TYPE_PREFIX = "QUERY_TYPE_"

# Verified against the dataset: exactly these three values occur.
VALID_QUERY_TYPES = frozenset({"SPECIFIC", "YES_NO", "GENERAL"})


def normalise_query_type(raw: str) -> str:
    """Strip the protobuf enum prefix: 'QUERY_TYPE_YES_NO' -> 'YES_NO'."""
    value = raw[len(_QUERY_TYPE_PREFIX):] if raw.startswith(_QUERY_TYPE_PREFIX) else raw
    if value not in VALID_QUERY_TYPES:
        raise ValueError(f"Unrecognised queryType: {raw!r}")
    return value


# --- attribution ranges ------------------------------------------------------


def parse_range(raw: dict[str, Any]) -> dict[str, int]:
    """Parse one attribution index range, applying proto3 int32 defaults.

    `endIndex` is INCLUSIVE in the source data (verified: ranges with
    startIndex == endIndex are single-segment citations, and no endIndex
    ever equals a meeting's segmentCount, which it would if exclusive).
    That inclusive-end contract is preserved unchanged all the way through
    to the Mongo document and the Java `AttributionResolver`.
    """
    return {
        "startIndex": raw.get("startIndex", 0),
        "endIndex": raw.get("endIndex", 0),
    }


def range_segment_count(r: dict[str, int]) -> int:
    """Number of segments covered by an inclusive [startIndex, endIndex] range."""
    return r["endIndex"] - r["startIndex"] + 1


# --- meeting id -> corpus/domain classification ------------------------------

# (id prefixes, corpus, domain). Order matters: checked top to bottom.
# Verified by classifying all 225 unique meeting ids in the real dataset —
# every id matched exactly one of these three buckets.
#   ES2*/IS1*/TS3*      -> AMI meetings (Augmented Multi-party Interaction corpus)
#   Bmr*/Bro*/Bed*/
#   Bdb*/Buw* (any B*)  -> ICSI meetings (International Computer Science Institute)
#   covid*/education*   -> Parliamentary committee meetings (Canada / Wales)
_CORPUS_PREFIXES: tuple[tuple[tuple[str, ...], str, str], ...] = (
    (("ES", "IS", "TS"), "AMI", "PRODUCT"),
    (("B",), "ICSI", "ACADEMIC"),
    (("covid", "education"), "PARLIAMENT", "PARLIAMENTARY"),
)


def classify(meeting_id: str) -> tuple[str, str]:
    """Classify a meeting id into (corpus, domain).

    Raises ValueError on an unrecognised prefix. An unknown corpus should
    stop the ingest for investigation, not be silently guessed or dropped.
    """
    for prefixes, corpus, domain in _CORPUS_PREFIXES:
        if meeting_id.startswith(prefixes):
            return corpus, domain
    raise ValueError(f"Unrecognised meeting id prefix, cannot classify: {meeting_id!r}")


# --- speaker rollups ----------------------------------------------------------


def speaker_stats(segments: list[dict[str, str]]) -> list[dict[str, Any]]:
    """Per-speaker segment/char rollups, ordered by segment count descending."""
    stats: dict[str, dict[str, Any]] = {}
    for seg in segments:
        name = seg["speakerName"]
        entry = stats.setdefault(name, {"name": name, "segmentCount": 0, "charCount": 0})
        entry["segmentCount"] += 1
        entry["charCount"] += len(seg["text"])
    return sorted(stats.values(), key=lambda s: (-s["segmentCount"], s["name"]))


def estimate_tokens(char_count: int) -> int:
    """Rough token estimate (~4 chars/token). Used only for context-budget
    planning (ChunkPlanner/ContextPacker on the Java side), never for billing.
    """
    return char_count // 4


# --- document builders ---------------------------------------------------


def build_meeting_doc(
    meeting_id: str,
    segments: list[dict[str, str]],
    split: str,
    source_file: str,
) -> dict[str, Any]:
    """Build a `meetings` collection document from one record's transcript.

    `dialogCount` is initialised to 1 (this record contributes one dialog)
    and summed by `merge_meeting` when the same meeting recurs across records.
    """
    corpus, domain = classify(meeting_id)
    indexed_segments = [
        {"index": i, "speakerName": s["speakerName"], "text": s["text"]}
        for i, s in enumerate(segments)
    ]
    char_count = sum(len(s["text"]) for s in segments)
    speakers = speaker_stats(segments)
    return {
        "_id": meeting_id,
        "corpus": corpus,
        "domain": domain,
        "split": split,
        "segmentCount": len(segments),
        "charCount": char_count,
        "estimatedTokens": estimate_tokens(char_count),
        "speakerCount": len(speakers),
        "dialogCount": 1,
        "speakers": speakers,
        "transcriptSegments": indexed_segments,
        "sourceFile": source_file,
    }


def build_dialog_doc(record: dict[str, Any], split: str) -> dict[str, Any]:
    """Build a `dialogs` collection document from one raw MISeD record."""
    meeting_id = record["meeting"]["meetingId"]
    corpus, domain = classify(meeting_id)

    turns: list[dict[str, Any]] = []
    unanswerable_count = 0
    attributed_count = 0
    query_type_counts: dict[str, int] = {}

    for i, turn in enumerate(record["dialog"]["dialogTurns"]):
        meta = turn.get("queryMetadata", {})
        query_type = normalise_query_type(meta["queryType"])
        # proto3 default omission: absent means False, not "unknown".
        unanswerable = bool(meta.get("isUnanswerable", False))
        context_dependent = bool(meta.get("isContextDependent", False))

        query_type_counts[query_type] = query_type_counts.get(query_type, 0) + 1
        if unanswerable:
            unanswerable_count += 1

        attribution = turn.get("responseAttribution")
        ranges: list[dict[str, int]] = []
        attributed_segment_count = 0
        if attribution:
            ranges = [parse_range(r) for r in attribution["indexRanges"]]
            attributed_segment_count = sum(range_segment_count(r) for r in ranges)
            attributed_count += 1

        turns.append({
            "turnIndex": i,
            "query": turn["query"],
            "response": turn["response"],
            "queryType": query_type,
            "unanswerable": unanswerable,
            "contextDependent": context_dependent,
            "attributionRanges": ranges,
            "attributedSegmentCount": attributed_segment_count,
        })

    return {
        "_id": record["dialogId"],
        "meetingId": meeting_id,
        "split": split,
        "corpus": corpus,
        "domain": domain,
        "turnCount": len(turns),
        "turns": turns,
        "stats": {
            "unanswerableCount": unanswerable_count,
            "attributedTurnCount": attributed_count,
            "queryTypeCounts": query_type_counts,
        },
    }


def parse_record(line: str, split: str, source_file: str) -> tuple[dict[str, Any], dict[str, Any]]:
    """Parse one JSONL line into (meeting_doc, dialog_doc)."""
    record = json.loads(line)
    meeting = record["meeting"]
    meeting_doc = build_meeting_doc(meeting["meetingId"], meeting["transcriptSegments"], split, source_file)
    dialog_doc = build_dialog_doc(record, split)
    return meeting_doc, dialog_doc


def merge_meeting(existing: dict[str, Any], incoming: dict[str, Any]) -> dict[str, Any]:
    """Merge two meeting docs seen for the same meetingId.

    207 of the 225 unique meetings in MISeD are referenced by two dialogs
    (18 by only one), and the transcript embedded in each record is
    byte-identical (verified across the full dataset). This function
    encodes that assumption explicitly and RAISES if it is ever violated,
    rather than silently keeping one copy and discarding a conflicting one.
    """
    if existing["_id"] != incoming["_id"]:
        raise ValueError("Cannot merge meeting documents with different ids")
    if existing["transcriptSegments"] != incoming["transcriptSegments"]:
        raise ValueError(
            f"Meeting {existing['_id']!r} has conflicting transcripts across records "
            "— the no-conflict assumption this ETL relies on does not hold, investigate "
            "before ingesting."
        )
    merged = dict(existing)
    merged["dialogCount"] = existing["dialogCount"] + incoming["dialogCount"]
    return merged
