from __future__ import annotations

from pathlib import Path
import unittest

from harness.validation import validate_project


class ValidationTest(unittest.TestCase):
    def test_test_folder_fixture_is_valid(self) -> None:
        project_root = Path(__file__).resolve().parents[2] / "test-folder"
        result = validate_project(project_root)

        self.assertTrue(result.ok, msg="\n".join(result.findings))
        self.assertEqual(result.summary["run_mode"], "skill-pipeline-validation")
        self.assertEqual(
            result.summary["worker_sequence"],
            ["document-review", "guide-generation", "implementation", "review"],
        )
        self.assertEqual(result.summary["review_result"], "DONE_WITH_CONCERNS")


if __name__ == "__main__":
    unittest.main()
