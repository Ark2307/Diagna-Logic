"""Unit tests for mised_transform.py.

These tests are the primary defense against the dataset's sharpest edge: the
raw files are proto3-JSON, so `False`/`0`-valued fields are simply absent from
the record. Every test that touches `isUnanswerable`, `isContextDependent`, or
an attribution range's `startIndex`/`endIndex` exercises that omission
directly, using fixtures shaped exactly like the records this omission
produces in the wild (verified against the real dataset before writing these).
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from mised_transform import (  # noqa: E402
    build_dialog_doc,
    build_meeting_doc,
    classify,
    estimate_tokens,
    merge_meeting,
    normalise_query_type,
    parse_range,
    parse_record,
    range_segment_count,
    speaker_stats,
)


# --------------------------------------------------------------------------
# normalise_query_type
# --------------------------------------------------------------------------


class TestNormaliseQueryType:
    @pytest.mark.parametrize(
        "raw, expected",
        [
            ("QUERY_TYPE_SPECIFIC", "SPECIFIC"),
            ("QUERY_TYPE_YES_NO", "YES_NO"),
            ("QUERY_TYPE_GENERAL", "GENERAL"),
        ],
    )
    def test_strips_known_prefix(self, raw, expected):
        assert normalise_query_type(raw) == expected

    def test_rejects_unrecognised_value(self):
        with pytest.raises(ValueError, match="Unrecognised queryType"):
            normalise_query_type("QUERY_TYPE_MYSTERY")


# --------------------------------------------------------------------------
# parse_range — the proto3 default-omission gotcha
# --------------------------------------------------------------------------


class TestParseRange:
    def test_both_fields_present(self):
        assert parse_range({"startIndex": 108, "endIndex": 108}) == {
            "startIndex": 108,
            "endIndex": 108,
        }

    def test_missing_start_index_defaults_to_zero(self):
        """A range citing segment 0 through N serializes with startIndex
        OMITTED (proto3 int32 default). Absence must mean 0, not an error
        or a dropped range."""
        assert parse_range({"endIndex": 3}) == {"startIndex": 0, "endIndex": 3}

    def test_missing_end_index_defaults_to_zero(self):
        """A single-segment citation of exactly segment 0 omits BOTH keys."""
        assert parse_range({}) == {"startIndex": 0, "endIndex": 0}

    def test_range_segment_count_is_inclusive(self):
        assert range_segment_count({"startIndex": 108, "endIndex": 108}) == 1
        assert range_segment_count({"startIndex": 5, "endIndex": 9}) == 5
        assert range_segment_count({"startIndex": 0, "endIndex": 0}) == 1


# --------------------------------------------------------------------------
# classify
# --------------------------------------------------------------------------


class TestClassify:
    @pytest.mark.parametrize(
        "meeting_id, corpus, domain",
        [
            ("ES2002c", "AMI", "PRODUCT"),
            ("IS1002b", "AMI", "PRODUCT"),
            ("TS3005d", "AMI", "PRODUCT"),
            ("Bmr019", "ICSI", "ACADEMIC"),
            ("Bro028", "ICSI", "ACADEMIC"),
            ("Bed014", "ICSI", "ACADEMIC"),
            ("Bdb001", "ICSI", "ACADEMIC"),
            ("Buw001", "ICSI", "ACADEMIC"),
            ("covid10", "PARLIAMENT", "PARLIAMENTARY"),
            ("education3", "PARLIAMENT", "PARLIAMENTARY"),
        ],
    )
    def test_known_prefixes(self, meeting_id, corpus, domain):
        assert classify(meeting_id) == (corpus, domain)

    def test_unknown_prefix_raises_instead_of_guessing(self):
        with pytest.raises(ValueError, match="Unrecognised meeting id prefix"):
            classify("XYZ999")


# --------------------------------------------------------------------------
# speaker_stats / estimate_tokens
# --------------------------------------------------------------------------


class TestSpeakerStats:
    def test_rollup_counts_and_ordering(self):
        segments = [
            {"speakerName": "Alice", "text": "hello"},
            {"speakerName": "Bob", "text": "hi there"},
            {"speakerName": "Alice", "text": "how are you"},
        ]
        stats = speaker_stats(segments)
        assert stats == [
            {"name": "Alice", "segmentCount": 2, "charCount": len("hello") + len("how are you")},
            {"name": "Bob", "segmentCount": 1, "charCount": len("hi there")},
        ]

    def test_empty_segments(self):
        assert speaker_stats([]) == []


class TestEstimateTokens:
    def test_rough_four_chars_per_token(self):
        assert estimate_tokens(400) == 100
        assert estimate_tokens(0) == 0


# --------------------------------------------------------------------------
# build_meeting_doc / build_dialog_doc
# --------------------------------------------------------------------------


SAMPLE_SEGMENTS = [
    {"speakerName": "Professor B", "text": "Let's discuss the digits task."},
    {"speakerName": "PhD A", "text": "I ran the SRI system on it."},
]


class TestBuildMeetingDoc:
    def test_shape_and_derived_fields(self):
        doc = build_meeting_doc("Bmr019", SAMPLE_SEGMENTS, "train", "train.jsonl")
        assert doc["_id"] == "Bmr019"
        assert doc["corpus"] == "ICSI"
        assert doc["domain"] == "ACADEMIC"
        assert doc["split"] == "train"
        assert doc["segmentCount"] == 2
        assert doc["charCount"] == sum(len(s["text"]) for s in SAMPLE_SEGMENTS)
        assert doc["speakerCount"] == 2
        assert doc["dialogCount"] == 1
        assert doc["sourceFile"] == "train.jsonl"
        # segment index must equal array position (this IS the attribution key)
        assert [s["index"] for s in doc["transcriptSegments"]] == [0, 1]
        assert doc["transcriptSegments"][0]["speakerName"] == "Professor B"


class TestBuildDialogDoc:
    def _record(self, turns):
        return {
            "dialogId": "abc123",
            "meeting": {"meetingId": "Bmr019", "transcriptSegments": SAMPLE_SEGMENTS},
            "dialog": {"dialogTurns": turns},
        }

    def test_unanswerable_absent_means_false(self):
        """proto3 omission: a turn with no isUnanswerable key is answerable."""
        record = self._record([
            {"query": "q1", "response": "r1", "queryMetadata": {"queryType": "QUERY_TYPE_GENERAL"}},
        ])
        doc = build_dialog_doc(record, "train")
        assert doc["turns"][0]["unanswerable"] is False
        assert doc["turns"][0]["contextDependent"] is False
        assert doc["stats"]["unanswerableCount"] == 0

    def test_unanswerable_true_is_counted(self):
        record = self._record([
            {
                "query": "q1",
                "response": "r1",
                "queryMetadata": {"queryType": "QUERY_TYPE_YES_NO", "isUnanswerable": True},
            },
        ])
        doc = build_dialog_doc(record, "train")
        assert doc["turns"][0]["unanswerable"] is True
        assert doc["stats"]["unanswerableCount"] == 1
        # unanswerable turns still have no responseAttribution
        assert doc["turns"][0]["attributionRanges"] == []
        assert doc["stats"]["attributedTurnCount"] == 0

    def test_attribution_ranges_and_segment_count(self):
        record = self._record([
            {
                "query": "What did Professor B recommend?",
                "response": "Running a test on TI digits.",
                "queryMetadata": {"queryType": "QUERY_TYPE_SPECIFIC"},
                "responseAttribution": {
                    "indexRanges": [{"startIndex": 0, "endIndex": 0}, {"endIndex": 1}],
                },
            },
        ])
        doc = build_dialog_doc(record, "train")
        turn = doc["turns"][0]
        assert turn["attributionRanges"] == [
            {"startIndex": 0, "endIndex": 0},
            {"startIndex": 0, "endIndex": 1},
        ]
        # 1 segment from the first range + 2 segments from the second = 3
        assert turn["attributedSegmentCount"] == 3
        assert doc["stats"]["attributedTurnCount"] == 1

    def test_query_type_counts_and_turn_index(self):
        record = self._record([
            {"query": "q1", "response": "r1", "queryMetadata": {"queryType": "QUERY_TYPE_SPECIFIC"}},
            {"query": "q2", "response": "r2", "queryMetadata": {"queryType": "QUERY_TYPE_SPECIFIC"}},
            {"query": "q3", "response": "r3", "queryMetadata": {"queryType": "QUERY_TYPE_YES_NO"}},
        ])
        doc = build_dialog_doc(record, "train")
        assert doc["turnCount"] == 3
        assert [t["turnIndex"] for t in doc["turns"]] == [0, 1, 2]
        assert doc["stats"]["queryTypeCounts"] == {"SPECIFIC": 2, "YES_NO": 1}

    def test_denormalises_corpus_and_domain_from_meeting_id(self):
        record = self._record([
            {"query": "q1", "response": "r1", "queryMetadata": {"queryType": "QUERY_TYPE_GENERAL"}},
        ])
        doc = build_dialog_doc(record, "train")
        assert doc["corpus"] == "ICSI"
        assert doc["domain"] == "ACADEMIC"
        assert doc["meetingId"] == "Bmr019"


# --------------------------------------------------------------------------
# parse_record (integration of the two builders)
# --------------------------------------------------------------------------


class TestParseRecord:
    def test_parses_meeting_and_dialog(self):
        record = {
            "dialogId": "xyz789",
            "meeting": {"meetingId": "ES2002c", "transcriptSegments": SAMPLE_SEGMENTS},
            "dialog": {
                "dialogTurns": [
                    {"query": "q", "response": "r", "queryMetadata": {"queryType": "QUERY_TYPE_GENERAL"}},
                ]
            },
        }
        meeting_doc, dialog_doc = parse_record(json.dumps(record), "validation", "validation.jsonl")
        assert meeting_doc["_id"] == "ES2002c"
        assert meeting_doc["corpus"] == "AMI"
        assert meeting_doc["split"] == "validation"
        assert dialog_doc["_id"] == "xyz789"
        assert dialog_doc["meetingId"] == "ES2002c"
        assert dialog_doc["split"] == "validation"


# --------------------------------------------------------------------------
# merge_meeting
# --------------------------------------------------------------------------


class TestMergeMeeting:
    def _meeting(self, dialog_count=1):
        doc = build_meeting_doc("Bmr019", SAMPLE_SEGMENTS, "train", "train.jsonl")
        doc["dialogCount"] = dialog_count
        return doc

    def test_merges_identical_transcripts_and_sums_dialog_count(self):
        a = self._meeting(dialog_count=1)
        b = self._meeting(dialog_count=1)
        merged = merge_meeting(a, b)
        assert merged["dialogCount"] == 2
        assert merged["_id"] == "Bmr019"
        assert merged["transcriptSegments"] == a["transcriptSegments"]

    def test_raises_on_id_mismatch(self):
        a = self._meeting()
        b = build_meeting_doc("ES2002c", SAMPLE_SEGMENTS, "train", "train.jsonl")
        with pytest.raises(ValueError, match="different ids"):
            merge_meeting(a, b)

    def test_raises_on_conflicting_transcript(self):
        a = self._meeting()
        b = self._meeting()
        b["transcriptSegments"] = [{"index": 0, "speakerName": "Someone Else", "text": "different"}]
        with pytest.raises(ValueError, match="conflicting transcripts"):
            merge_meeting(a, b)


# --------------------------------------------------------------------------
# Idempotent ids — dialogId/meetingId are stable, deterministic primary keys
# --------------------------------------------------------------------------


class TestIdempotentIds:
    def test_reparsing_the_same_line_yields_the_same_ids(self):
        record = {
            "dialogId": "stable-id-1",
            "meeting": {"meetingId": "Bmr019", "transcriptSegments": SAMPLE_SEGMENTS},
            "dialog": {
                "dialogTurns": [
                    {"query": "q", "response": "r", "queryMetadata": {"queryType": "QUERY_TYPE_GENERAL"}},
                ]
            },
        }
        line = json.dumps(record)
        meeting_doc_1, dialog_doc_1 = parse_record(line, "train", "train.jsonl")
        meeting_doc_2, dialog_doc_2 = parse_record(line, "train", "train.jsonl")
        assert meeting_doc_1["_id"] == meeting_doc_2["_id"]
        assert dialog_doc_1["_id"] == dialog_doc_2["_id"]
        assert meeting_doc_1 == meeting_doc_2
        assert dialog_doc_1 == dialog_doc_2
