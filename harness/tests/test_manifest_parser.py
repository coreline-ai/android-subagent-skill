from __future__ import annotations

import tempfile
import unittest
from pathlib import Path

from harness.manifest_parser import parse_markdown_bullets, parse_session_context


class ManifestParserTest(unittest.TestCase):
    def test_parse_bullets_with_nested_lists_and_maps(self) -> None:
        text = """
## Handoff Manifest
- **completed_agent:** android-review-agent
- **review_result:** DONE_WITH_CONCERNS (review 1)
- **verified_files:**
  - `docs/generated/session-context.md`
  - `docs/generated/handoff-manifest.md`
- **issue_classification_counts:**
  - `CONTEXT_BREAK`: 0
  - `SCOPE_BLOCKER`: 0
"""
        parsed = parse_markdown_bullets(text)

        self.assertEqual(parsed["completed_agent"], "android-review-agent")
        self.assertEqual(parsed["review_result"], "DONE_WITH_CONCERNS (review 1)")
        self.assertEqual(
            parsed["verified_files"],
            ["docs/generated/session-context.md", "docs/generated/handoff-manifest.md"],
        )
        self.assertEqual(
            parsed["issue_classification_counts"],
            {"CONTEXT_BREAK": "0", "SCOPE_BLOCKER": "0"},
        )

    def test_parse_session_context_sections(self) -> None:
        text = """
# Session Context

## Session Update - Orchestrator Start
- **current_stage:** `pipeline-orchestrator`
- **pipeline_id:** `pipe-1`

## Session Update - Review
- **current_stage:** `review`
- **pipeline_id:** `pipe-1`
"""
        with tempfile.TemporaryDirectory() as tmpdir:
            path = Path(tmpdir) / "session-context.md"
            path.write_text(text, encoding="utf-8")
            parsed = parse_session_context(path)

        self.assertEqual(len(parsed), 2)
        self.assertEqual(parsed[0]["current_stage"], "pipeline-orchestrator")
        self.assertEqual(parsed[1]["current_stage"], "review")


if __name__ == "__main__":
    unittest.main()
