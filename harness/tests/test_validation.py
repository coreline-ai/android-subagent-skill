from __future__ import annotations

from pathlib import Path
import unittest

from harness.validation import validate_project


class ValidationTest(unittest.TestCase):
    def test_test_folder_fixture_is_valid(self) -> None:
        """Contract validation must pass from a fresh git clone."""
        project_root = Path(__file__).resolve().parents[2] / "test-folder"
        result = validate_project(project_root)

        self.assertTrue(result.ok, msg="\n".join(result.findings))
        self.assertEqual(result.summary["run_mode"], "skill-pipeline-validation")
        self.assertEqual(
            result.summary["worker_sequence"],
            ["document-review", "guide-generation", "implementation", "review"],
        )
        self.assertEqual(result.summary["review_result"], "DONE_WITH_CONCERNS")

    def test_build_evidence_paths_are_tracked(self) -> None:
        """Build evidence should be reported separately, not block contract validation."""
        project_root = Path(__file__).resolve().parents[2] / "test-folder"
        result = validate_project(project_root)

        # Contract validation passes without build evidence
        self.assertTrue(result.ok, msg="\n".join(result.findings))
        # Build findings are collected but do not affect ok status
        self.assertGreater(len(result.build_findings), 0)

    def test_build_evidence_fails_when_checked(self) -> None:
        """With check_build_evidence=True, missing build artifacts cause failure."""
        project_root = Path(__file__).resolve().parents[2] / "test-folder"
        result = validate_project(project_root, check_build_evidence=True)

        # Build artifacts are not in git, so this should fail
        self.assertFalse(result.ok)
        self.assertTrue(
            any("build evidence" in f for f in result.findings),
            msg="Expected build evidence findings in findings list",
        )


if __name__ == "__main__":
    unittest.main()
