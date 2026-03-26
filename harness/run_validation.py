from __future__ import annotations

import argparse
from pathlib import Path
import sys

if __package__ in (None, ""):
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    from harness.validation import validate_project
else:
    from .validation import validate_project


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate the markdown-based skill pipeline fixture.")
    parser.add_argument(
        "--project",
        default="test-folder",
        help="Project directory relative to the repository root. Default: test-folder",
    )
    args = parser.parse_args(argv)

    repo_root = Path(__file__).resolve().parents[1]
    project_root = (repo_root / args.project).resolve()
    result = validate_project(project_root)

    print(f"project_root={project_root}")
    print(f"pipeline_id={result.summary.get('pipeline_id', '')}")
    print(f"run_mode={result.summary.get('run_mode', '')}")
    print(f"review_result={result.summary.get('review_result', '')}")
    print(f"worker_sequence={result.summary.get('worker_sequence', [])}")
    print(f"session_updates={result.summary.get('session_updates', 0)}")

    if result.findings:
        print("findings:")
        for finding in result.findings:
            print(f"- {finding}")
        return 1

    print("validation=ok")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

