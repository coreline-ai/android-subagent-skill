from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Any

from .agent_registry import (
    MANIFEST_SPECS,
    SESSION_REQUIRED_KEYS,
    STAGE_TO_HANDOFF,
    TERMINAL_REVIEW_RESULTS,
    WORKER_SEQUENCE,
)
from .manifest_parser import parse_manifest, parse_session_context


@dataclass
class ValidationResult:
    ok: bool
    findings: list[str]
    summary: dict[str, Any]


def _normalize_status(value: str) -> str:
    text = value.strip()
    if not text:
        return ""
    return text.split()[0].split("(")[0].upper()


def _parse_count(value: str) -> int:
    match = re.search(r"\d+", value)
    return int(match.group()) if match else 0


def _as_list(value: Any) -> list[str]:
    if isinstance(value, list):
        return [str(item) for item in value]
    if value in ("", None):
        return []
    return [str(value)]


def _collect_scalar(values: list[Any]) -> list[str]:
    return [str(value) for value in values if isinstance(value, str) and value]


def _require_keys(target_name: str, data: dict[str, Any], required_keys: tuple[str, ...], findings: list[str]) -> None:
    for key in required_keys:
        if key not in data:
            findings.append(f"{target_name}: missing required key '{key}'")


def validate_project(project_root: Path) -> ValidationResult:
    generated_dir = project_root / "docs" / "generated"
    findings: list[str] = []
    manifests: dict[str, dict[str, Any]] = {}

    for name, spec in MANIFEST_SPECS.items():
        path = generated_dir / spec.file_name
        if not path.exists():
            findings.append(f"missing manifest: {spec.file_name}")
            continue
        manifest = parse_manifest(path)
        manifests[name] = manifest
        _require_keys(spec.file_name, manifest, spec.required_keys, findings)

    session_context_path = generated_dir / "session-context.md"
    session_sections: list[dict[str, Any]] = []
    if not session_context_path.exists():
        findings.append("missing session-context.md")
    else:
        session_sections = parse_session_context(session_context_path)
        if not session_sections:
            findings.append("session-context.md: no session update sections found")
        for index, section in enumerate(session_sections, start=1):
            _require_keys(f"session section #{index}", section, SESSION_REQUIRED_KEYS, findings)

    pipeline_ids = _collect_scalar(
        [section.get("pipeline_id", "") for section in session_sections]
        + [manifest.get("pipeline_id", "") for manifest in manifests.values()]
    )
    run_modes = _collect_scalar(
        [section.get("run_mode", "") for section in session_sections]
        + [manifest.get("run_mode", "") for manifest in manifests.values()]
    )

    if len(set(pipeline_ids)) > 1:
        findings.append(f"inconsistent pipeline_id values: {sorted(set(pipeline_ids))}")
    if len(set(run_modes)) > 1:
        findings.append(f"inconsistent run_mode values: {sorted(set(run_modes))}")

    worker_sequence = [
        section.get("current_stage", "")
        for section in session_sections
        if section.get("current_stage") in WORKER_SEQUENCE
    ]
    if worker_sequence != list(WORKER_SEQUENCE):
        findings.append(
            "worker stage sequence mismatch: expected "
            f"{list(WORKER_SEQUENCE)}, got {worker_sequence}"
        )

    if session_sections:
        if session_sections[0].get("current_stage") != "pipeline-orchestrator":
            findings.append("first session stage must be pipeline-orchestrator")
        if session_sections[-1].get("current_stage") != "pipeline-orchestrator":
            findings.append("last session stage must be pipeline-orchestrator")

    for stage, expected_handoff in STAGE_TO_HANDOFF.items():
        matching = [section for section in session_sections if section.get("current_stage") == stage]
        if not matching:
            findings.append(f"session-context: missing stage '{stage}'")
            continue
        latest = matching[-1].get("latest_handoff", "")
        if latest != f"docs/generated/{expected_handoff}":
            findings.append(f"{stage}: latest_handoff should be docs/generated/{expected_handoff}, got {latest}")

    implementation_manifest = manifests.get("implementation")
    review_manifest = manifests.get("review")
    orchestrator_manifest = manifests.get("orchestrator")

    if implementation_manifest:
        for rel_path in _as_list(implementation_manifest.get("evidence_paths")):
            if not (project_root / rel_path).exists():
                findings.append(f"implementation evidence path missing: {rel_path}")

    if review_manifest:
        for rel_path in _as_list(review_manifest.get("evidence_paths")):
            if not (project_root / rel_path).exists():
                findings.append(f"review evidence path missing: {rel_path}")

    if orchestrator_manifest:
        for rel_path in _as_list(orchestrator_manifest.get("evidence_paths")):
            if not (project_root / rel_path).exists():
                findings.append(f"orchestrator evidence path missing: {rel_path}")

    review_result = ""
    if review_manifest:
        review_result = _normalize_status(str(review_manifest.get("review_result", "")))

    if review_manifest and orchestrator_manifest:
        dispatch_target = str(orchestrator_manifest.get("dispatch_target", ""))
        review_cycle = _parse_count(str(review_manifest.get("review_cycle", "0")))
        if review_result in TERMINAL_REVIEW_RESULTS and dispatch_target != "stop":
            findings.append("orchestrator should stop after terminal review result")
        if review_result == "REJECTED" and review_cycle < 3 and dispatch_target != "implementation":
            findings.append("orchestrator should dispatch implementation after rejected review")

    if review_manifest and str(review_manifest.get("run_mode", "")) == "skill-pipeline-validation":
        classification = review_manifest.get("issue_classification_counts", {})
        if review_result == "DONE_WITH_CONCERNS":
            context_breaks = _parse_count(str(classification.get("CONTEXT_BREAK", "0")))
            scope_blockers = _parse_count(str(classification.get("SCOPE_BLOCKER", "0")))
            if context_breaks != 0:
                findings.append("DONE_WITH_CONCERNS requires CONTEXT_BREAK count to be 0")
            if scope_blockers != 0:
                findings.append("DONE_WITH_CONCERNS requires SCOPE_BLOCKER count to be 0")

    summary = {
        "pipeline_id": pipeline_ids[0] if pipeline_ids else "",
        "run_mode": run_modes[0] if run_modes else "",
        "review_result": review_result,
        "worker_sequence": worker_sequence,
        "session_updates": len(session_sections),
    }
    return ValidationResult(ok=not findings, findings=findings, summary=summary)

