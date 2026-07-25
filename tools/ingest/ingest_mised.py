#!/usr/bin/env python3
"""CLI that turns the raw MISeD JSONL files into the `meetings` and `dialogs`
Mongo collections used by Diagna-Logic.

This is a thin orchestration layer over the pure functions in
`mised_transform.py`: read lines -> transform -> merge duplicate meetings ->
validate referential integrity -> (optionally) write to Mongo -> print a
summary report. All parsing/business logic lives in mised_transform.py so it
can be unit tested without a database; this file owns I/O only.

Usage
-----
    # sanity-check the raw files without touching Mongo
    python ingest_mised.py --dir data/raw --dry-run

    # full load (drops existing collections first)
    python ingest_mised.py --dir data/raw --uri mongodb://localhost:27017 \\
        --db diagna --drop

    # also emit a handful of trimmed fixture documents for the Java-side
    # mapping tests (see backend/src/test/resources/fixtures/README.md)
    python ingest_mised.py --dir data/raw --drop \\
        --emit-fixtures backend/src/test/resources/fixtures/

Exit codes: 0 on success, 1 on any validation failure (referential integrity,
unexpected record counts, etc). A non-zero exit is meant to fail a CI step or
a Makefile target loudly rather than leave a half-loaded database silently.
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from mised_transform import build_dialog_doc, merge_meeting, parse_record

SPLITS = ("train", "validation", "test")

# Known-good totals for the published MISeD dataset, used as a post-load
# sanity check. If these ever legitimately change (a new dataset release),
# update them deliberately rather than silence the check.
EXPECTED = {
    "dialogs": 432,
    "meetings": 225,
    "turns": 4161,
    "attributionRanges": 8651,
    "segments": 118148,
    "unanswerableTurns": 1274,
    "contextDependentTurns": 548,
    "attributedTurns": 2857,
}


def read_split(raw_dir: Path, split: str) -> list[str]:
    path = raw_dir / f"{split}.jsonl"
    if not path.exists():
        raise SystemExit(f"Missing {path} — run tools/download_mised.sh first.")
    with path.open("r", encoding="utf-8") as f:
        return [line for line in (l.strip() for l in f) if line]


def transform_all(raw_dir: Path) -> tuple[dict[str, dict[str, Any]], list[dict[str, Any]]]:
    """Parse every split, merging duplicate meetings as they're encountered.

    Returns (meetings_by_id, dialog_docs).
    """
    meetings_by_id: dict[str, dict[str, Any]] = {}
    dialog_docs: list[dict[str, Any]] = []

    for split in SPLITS:
        source_file = f"{split}.jsonl"
        lines = read_split(raw_dir, split)
        # Stored as UPPERCASE ("TRAIN"/"VALIDATION"/"TEST") so it maps 1:1 to
        # the Java `DatasetSplit` enum's name() under Spring Data's default
        # enum conversion — no custom converter needed on the Java side.
        # Filenames themselves (train.jsonl etc.) stay lowercase.
        for line in lines:
            meeting_doc, dialog_doc = parse_record(line, split.upper(), source_file)
            meeting_id = meeting_doc["_id"]
            if meeting_id in meetings_by_id:
                meetings_by_id[meeting_id] = merge_meeting(meetings_by_id[meeting_id], meeting_doc)
            else:
                meetings_by_id[meeting_id] = meeting_doc
            dialog_docs.append(dialog_doc)

    return meetings_by_id, dialog_docs


def stamp_ingested_at(meetings_by_id: dict[str, Any], dialog_docs: list[dict[str, Any]]) -> None:
    """Stamp every document with the current UTC time as an ISO-8601 string.

    Stored (and mapped on the Java side) as a plain String, not a BSON Date —
    this field is purely informational ("when was this loaded"), nothing
    queries or sorts on it, so a string keeps ingest_mised.py's JSON fixture
    output and the real Mongo write identical without any datetime/BSON
    conversion machinery. One timestamp per run, applied to every doc, so a
    single ingest is trivially identifiable as one batch.
    """
    ingested_at = datetime.now(timezone.utc).isoformat()
    for doc in meetings_by_id.values():
        doc["ingestedAt"] = ingested_at
    for doc in dialog_docs:
        doc["ingestedAt"] = ingested_at


def validate_referential_integrity(meetings_by_id: dict[str, Any], dialog_docs: list[dict[str, Any]]) -> list[str]:
    """Check that every dialog references a real meeting and every
    attribution index falls within that meeting's segment count.

    Returns a list of human-readable problems (empty list = all good). This
    is a repository-integrity check that a schema-less document store can't
    enforce for us, so the ETL enforces it explicitly instead.
    """
    problems: list[str] = []
    for dialog in dialog_docs:
        meeting = meetings_by_id.get(dialog["meetingId"])
        if meeting is None:
            problems.append(f"dialog {dialog['_id']!r} references unknown meetingId {dialog['meetingId']!r}")
            continue
        segment_count = meeting["segmentCount"]
        for turn in dialog["turns"]:
            for r in turn["attributionRanges"]:
                if r["startIndex"] < 0 or r["endIndex"] >= segment_count or r["startIndex"] > r["endIndex"]:
                    problems.append(
                        f"dialog {dialog['_id']!r} turn {turn['turnIndex']} has out-of-bounds "
                        f"attribution range {r} against meeting {meeting['_id']!r} "
                        f"(segmentCount={segment_count})"
                    )
    return problems


def summarize(meetings_by_id: dict[str, Any], dialog_docs: list[dict[str, Any]]) -> dict[str, int]:
    turns = [t for d in dialog_docs for t in d["turns"]]
    ranges = [r for t in turns for r in t["attributionRanges"]]
    return {
        "dialogs": len(dialog_docs),
        "meetings": len(meetings_by_id),
        "turns": len(turns),
        "attributionRanges": len(ranges),
        "segments": sum(m["segmentCount"] for m in meetings_by_id.values()),
        "unanswerableTurns": sum(1 for t in turns if t["unanswerable"]),
        "contextDependentTurns": sum(1 for t in turns if t["contextDependent"]),
        "attributedTurns": sum(1 for t in turns if t["attributionRanges"]),
    }


def print_report(actual: dict[str, int]) -> bool:
    """Prints a summary report comparing actual vs. expected counts.
    Returns True if everything matches.
    """
    ok = True
    print("\n=== Ingest summary ===")
    for key, expected_value in EXPECTED.items():
        actual_value = actual.get(key)
        status = "OK" if actual_value == expected_value else "MISMATCH"
        if status == "MISMATCH":
            ok = False
        print(f"  {key:<22} actual={actual_value:<8} expected={expected_value:<8} [{status}]")
    print("=======================\n")
    return ok


def write_to_mongo(uri: str, db_name: str, meetings_by_id: dict[str, Any], dialog_docs: list[dict[str, Any]], drop: bool) -> None:
    from pymongo import MongoClient, ReplaceOne
    from pymongo.errors import PyMongoError

    client = MongoClient(uri, serverSelectionTimeoutMS=5000)
    try:
        client.admin.command("ping")
    except PyMongoError as e:
        raise SystemExit(f"Could not reach MongoDB at {uri}: {e}")

    db = client[db_name]

    if drop:
        db.meetings.drop()
        db.dialogs.drop()
        print(f"Dropped existing 'meetings' and 'dialogs' collections in db={db_name!r}")

    if meetings_by_id:
        ops = [ReplaceOne({"_id": m["_id"]}, m, upsert=True) for m in meetings_by_id.values()]
        result = db.meetings.bulk_write(ops, ordered=False)
        print(f"meetings: upserted={result.upserted_count} matched={result.matched_count}")

    if dialog_docs:
        ops = [ReplaceOne({"_id": d["_id"]}, d, upsert=True) for d in dialog_docs]
        result = db.dialogs.bulk_write(ops, ordered=False)
        print(f"dialogs:  upserted={result.upserted_count} matched={result.matched_count}")

    create_indexes(db)
    client.close()


def create_indexes(db) -> None:
    db.meetings.create_index("corpus")
    db.meetings.create_index("domain")
    db.meetings.create_index("split")
    db.meetings.create_index("segmentCount")
    db.meetings.create_index([("transcriptSegments.text", "text")])

    db.dialogs.create_index("meetingId")
    db.dialogs.create_index("split")
    db.dialogs.create_index("turns.queryType")
    db.dialogs.create_index("stats.unanswerableCount")
    db.dialogs.create_index([("turns.query", "text"), ("turns.response", "text")])
    print("Indexes created on 'meetings' and 'dialogs'.")


def emit_fixtures(out_dir: Path, meetings_by_id: dict[str, Any], dialog_docs: list[dict[str, Any]]) -> None:
    """Write a small, hand-picked set of trimmed documents for the Java-side
    mapping tests (`MisedDocumentMappingTest`). These are the anti-drift
    guard between this Python transform and the Java POJOs — if the Java
    mapper doesn't reproduce these shapes exactly, the mapping is wrong.

    Picks:
      - meeting_Bmr019.json    : the ground-truth cross-check meeting. Its
                                 dialog turn "What did Professor B recommend
                                 to do during the discussion of digits?" cites
                                 segments 108, 118, 189, 224, 327 — used to
                                 verify AttributionResolver end to end.
      - meeting_sample_ami.json / meeting_sample_parliament.json:
                                 one meeting from the other two corpora, to
                                 exercise `classify()` mapping in Java too.
      - dialogs_sample.json    : a handful of dialogs chosen to cover the
                                 proto3-omission edge cases: an unanswerable
                                 turn, a context-dependent turn, and a turn
                                 whose attribution range omits startIndex.
    """
    out_dir.mkdir(parents=True, exist_ok=True)

    def write(name: str, payload: Any) -> None:
        path = out_dir / name
        path.write_text(json.dumps(payload, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        print(f"  wrote {path}")

    if "Bmr019" in meetings_by_id:
        write("meeting_Bmr019.json", meetings_by_id["Bmr019"])

    by_corpus: dict[str, str] = {}
    for mid, doc in meetings_by_id.items():
        by_corpus.setdefault(doc["corpus"], mid)
    if "AMI" in by_corpus:
        write("meeting_sample_ami.json", meetings_by_id[by_corpus["AMI"]])
    if "PARLIAMENT" in by_corpus:
        write("meeting_sample_parliament.json", meetings_by_id[by_corpus["PARLIAMENT"]])

    sample_dialogs: list[dict[str, Any]] = []
    added_dialog_ids: set[str] = set()
    seen_kinds: set[str] = set()

    def add_once(dialog: dict[str, Any], kind: str) -> None:
        seen_kinds.add(kind)
        if dialog["_id"] not in added_dialog_ids:
            sample_dialogs.append(dialog)
            added_dialog_ids.add(dialog["_id"])

    for d in dialog_docs:
        if d["meetingId"] == "Bmr019" and "gold_bmr019" not in seen_kinds:
            for t in d["turns"]:
                if t["attributionRanges"] == [
                    {"startIndex": 108, "endIndex": 108},
                    {"startIndex": 118, "endIndex": 118},
                    {"startIndex": 189, "endIndex": 189},
                    {"startIndex": 224, "endIndex": 224},
                    {"startIndex": 327, "endIndex": 327},
                ]:
                    add_once(d, "gold_bmr019")
                    break
        if d["stats"]["unanswerableCount"] > 0 and "unanswerable" not in seen_kinds:
            add_once(d, "unanswerable")
        if any(t["contextDependent"] for t in d["turns"]) and "context_dependent" not in seen_kinds:
            add_once(d, "context_dependent")
        if seen_kinds.issuperset({"gold_bmr019", "unanswerable", "context_dependent"}):
            break
    write("dialogs_sample.json", sample_dialogs)

    readme = out_dir / "README.md"
    readme.write_text(
        "# ETL fixtures\n\n"
        "Generated by `tools/ingest/ingest_mised.py --emit-fixtures`. These are trimmed,\n"
        "real documents from the transform pipeline, used by `MisedDocumentMappingTest` on\n"
        "the Java side to catch drift between the Python transform and the Java POJOs.\n\n"
        "- `meeting_Bmr019.json` — the ground-truth cross-check meeting: its dialog turn\n"
        "  \"What did Professor B recommend to do during the discussion of digits?\" cites\n"
        "  segments 108, 118, 189, 224, 327. `AttributionResolver` and the UI highlighter\n"
        "  must agree on exactly this set.\n"
        "- `meeting_sample_ami.json`, `meeting_sample_parliament.json` — one meeting from\n"
        "  each of the other two corpora, to exercise `classify()` in both languages.\n"
        "- `dialogs_sample.json` — dialogs covering the proto3-omission edge cases: an\n"
        "  unanswerable turn (`isUnanswerable` absent = false, present = true), a\n"
        "  context-dependent turn, and the gold Bmr019 attribution above.\n"
        "\n"
        "Do not hand-edit these files — regenerate them via the ingest CLI so they stay a\n"
        "faithful snapshot of what the ETL actually produces.\n",
        encoding="utf-8",
    )
    print(f"  wrote {readme}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dir", required=True, type=Path, help="Directory containing train/validation/test.jsonl")
    parser.add_argument("--uri", default="mongodb://localhost:27017", help="MongoDB connection URI")
    parser.add_argument("--db", default="diagna", help="Target database name")
    parser.add_argument("--drop", action="store_true", help="Drop the meetings/dialogs collections before loading")
    parser.add_argument("--dry-run", action="store_true", help="Parse and validate only; do not connect to Mongo")
    parser.add_argument(
        "--emit-fixtures",
        type=Path,
        default=None,
        help="Directory to write trimmed JSON fixtures for the Java mapping tests",
    )
    args = parser.parse_args()

    print(f"Reading raw JSONL from {args.dir} ...")
    meetings_by_id, dialog_docs = transform_all(args.dir)
    stamp_ingested_at(meetings_by_id, dialog_docs)

    print("Validating referential integrity ...")
    problems = validate_referential_integrity(meetings_by_id, dialog_docs)
    if problems:
        print(f"\n{len(problems)} referential integrity problem(s) found:", file=sys.stderr)
        for p in problems[:20]:
            print(f"  - {p}", file=sys.stderr)
        if len(problems) > 20:
            print(f"  ... and {len(problems) - 20} more", file=sys.stderr)
        raise SystemExit(1)
    print("  OK — every dialog resolves to a known meeting and every attribution is in bounds.")

    actual = summarize(meetings_by_id, dialog_docs)
    matched_expected = print_report(actual)

    if args.emit_fixtures:
        print(f"Writing fixtures to {args.emit_fixtures} ...")
        emit_fixtures(args.emit_fixtures, meetings_by_id, dialog_docs)

    if args.dry_run:
        print("Dry run: skipping MongoDB write.")
    else:
        print(f"Writing to MongoDB at {args.uri}, db={args.db!r} ...")
        write_to_mongo(args.uri, args.db, meetings_by_id, dialog_docs, args.drop)
        print("Done.")

    if not matched_expected:
        print(
            "WARNING: actual counts did not match the known-good MISeD totals. "
            "The upstream dataset may have changed — review before trusting this load.",
            file=sys.stderr,
        )
        raise SystemExit(1)


if __name__ == "__main__":
    main()
