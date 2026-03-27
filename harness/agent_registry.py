from __future__ import annotations

from dataclasses import dataclass


SESSION_REQUIRED_KEYS = (
    "pipeline_id",
    "run_mode",
    "current_stage",
    "review_cycle",
    "session_id",
    "parent_session_id",
    "previous_handoff",
    "latest_handoff",
    "in_scope",
    "out_of_scope",
    "decision_summary",
    "resolved_issues",
    "unresolved_issues",
    "next_agent_focus",
    "evidence_paths",
    "carry_forward_rules",
)

HANDOFF_REQUIRED_KEYS = (
    "pipeline_id",
    "session_id",
    "parent_session_id",
    "run_mode",
    "review_cycle",
    "session_context_path",
    "previous_handoff",
    "in_scope",
    "out_of_scope",
    "decision_summary",
    "evidence_paths",
    "unresolved_issues",
)

WORKER_SEQUENCE = (
    "document-review",
    "guide-generation",
    "implementation",
    "review",
)

TERMINAL_REVIEW_RESULTS = {"APPROVED", "DONE_WITH_CONCERNS"}


@dataclass(frozen=True)
class ManifestSpec:
    file_name: str
    required_keys: tuple[str, ...]
    stage: str
    skill_dir: str
    agent_name: str
    execution_mode: str  # "fork" or "inline" — Claude Code Agent tool 전용. fork는 격리된 서브에이전트 실행.


MANIFEST_SPECS = {
    "orchestrator": ManifestSpec(
        file_name="orchestrator-handoff.md",
        stage="pipeline-orchestrator",
        skill_dir="pipeline-orchestrator-agent",
        agent_name="android-pipeline-orchestrator-agent",
        execution_mode="inline",
        required_keys=HANDOFF_REQUIRED_KEYS
        + (
            "completed_agent",
            "dispatch_target",
            "dispatch_reason",
            "next_agent_required_actions",
        ),
    ),
    "document_review": ManifestSpec(
        file_name="document-reviewer-handoff.md",
        stage="document-review",
        skill_dir="document-reviewer-agent",
        agent_name="android-document-reviewer-agent",
        execution_mode="fork",
        required_keys=HANDOFF_REQUIRED_KEYS
        + (
            "completed_agent",
            "updated_documents",
            "corrected_inconsistency_count",
            "context_snapshot_path",
            "next_agent_context",
            "next_agent_required_actions",
        ),
    ),
    "guide_generation": ManifestSpec(
        file_name="guide-generator-handoff.md",
        stage="guide-generation",
        skill_dir="code-quality-guide-generator",
        agent_name="android-code-quality-guide-generator",
        execution_mode="fork",
        required_keys=HANDOFF_REQUIRED_KEYS
        + (
            "completed_agent",
            "generated_artifacts",
            "next_agent_context",
            "next_agent_required_actions",
        ),
    ),
    "implementation": ManifestSpec(
        file_name="handoff-manifest.md",
        stage="implementation",
        skill_dir="implementation-agent",
        agent_name="android-implementation-agent",
        execution_mode="inline",
        required_keys=HANDOFF_REQUIRED_KEYS
        + (
            "completed_agent",
            "implemented_scope",
            "declared_gaps",
            "changed_files",
            "test_results",
            "test_coverage",
            "resolved_issue_counts",
            "next_agent_context",
            "next_agent_required_actions",
        ),
    ),
    "review": ManifestSpec(
        file_name="review-handoff-manifest.md",
        stage="review",
        skill_dir="review-agent",
        agent_name="android-review-agent",
        execution_mode="inline",
        required_keys=HANDOFF_REQUIRED_KEYS
        + (
            "completed_agent",
            "review_result",
            "verified_files",
            "issue_counts",
            "issue_classification_counts",
            "next_agent_required_actions",
            "test_coverage_status",
            "security_checklist_status",
        ),
    ),
}

STAGE_TO_HANDOFF = {
    "document-review": "document-reviewer-handoff.md",
    "guide-generation": "guide-generator-handoff.md",
    "implementation": "handoff-manifest.md",
    "review": "review-handoff-manifest.md",
}

STAGE_TO_SKILL_DIR = {spec.stage: spec.skill_dir for spec in MANIFEST_SPECS.values()}

STAGE_TO_AGENT_NAME = {spec.stage: spec.agent_name for spec in MANIFEST_SPECS.values()}

STAGE_TO_EXECUTION_MODE = {spec.stage: spec.execution_mode for spec in MANIFEST_SPECS.values()}

