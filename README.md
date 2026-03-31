# android-subagent-skill

<p align="center">
  <strong>Markdown-driven Android sub-agent workflow for PRD → guides → implementation → review</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/status-active-1f883d?style=for-the-badge" alt="Status: Active" />
  <img src="https://img.shields.io/badge/mode-project--delivery%20%7C%20skill--pipeline--validation-0969da?style=for-the-badge" alt="Modes" />
  <img src="https://img.shields.io/badge/platform-Android%20%2B%20NDK-ff6b35?style=for-the-badge" alt="Platform: Android + NDK" />
  <img src="https://img.shields.io/badge/runtime-Claude%20Code-6e44ff?style=for-the-badge" alt="Runtime: Claude Code" />
</p>

<img width="1156" height="628" alt="image" src="https://github.com/user-attachments/assets/9e7dbc96-f1cc-4118-ad24-446d61d606f2" />

> **Platform:** 이 파이프라인은 **Claude Code** 전용입니다. `context: fork` (서브에이전트 격리), Hook (사전 조건 검증), Agent tool (fork 실행) 등 Claude Code 런타임 기능에 의존합니다.

`android-subagent-skill` is a Claude Code skill-based agent workflow for Android projects.
It defines how specialized agents share context, hand off work, loop on review, and validate the pipeline with a minimal local harness.

## 💡 Why this exists

Most agent workflows break when the next agent cannot reconstruct context.
This repository fixes that by making the contract explicit:

- every stage writes a canonical handoff manifest
- all agents append to a shared `session-context.md`
- one orchestrator controls start timing and review loops
- validation mode proves the skill chain works before using it on a real project

## 📦 What you get

- 1 orchestrator skill + 4 worker skills, each with its own `SKILL.md`
- a shared session contract ([`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md))
- a canonical agent registry with stage/folder/agent-name mapping ([`agent_registry.py`](./harness/agent_registry.py))
- a minimal Python harness for contract validation ([`harness/`](./harness))
- a PreToolUse hook that blocks pipeline start when `docs/PRD.md` or `docs/TRD.md` is missing ([`.claude/hooks/`](./.claude/hooks))

## 🔀 Pipeline flow

> `pipeline-orchestrator` → `document-review` → `guide-generation` → `implementation` → `review`

| # | Stage | Execution | Direction | Condition |
| :---: | --- | :---: | :---: | --- |
| 1 | `pipeline-orchestrator` | `inline` | ➡️ | Always starts here — dispatches first worker |
| 2 | `document-review` | `fork`* | ➡️ | Normalize PRD/TRD for downstream |
| 3 | `guide-generation` | `fork`* | ➡️ | Produce design intent + quality rubric |
| 4 | `implementation` | `inline` | ➡️ | Write code and tests |
| 5 | `review` | `inline` | ✅ / 🔄 | `APPROVED` or `DONE_WITH_CONCERNS` → **stop** |
| | | | | `REJECTED` → **back to step 4** (max 3 cycles) |

> *`fork` = Claude Code Agent tool로 격리된 서브에이전트 실행. `context: fork` frontmatter 선언 필요.

### 📏 Core loop rules

- `Rejected` returns control to `implementation` via orchestrator re-dispatch
- orchestrator allows up to **3 automatic review loops**
- `skill-pipeline-validation` does not fail only because the full PRD is not implemented
- `DONE_WITH_CONCERNS` requires `CONTEXT_BREAK = 0` and `SCOPE_BLOCKER = 0`

## 🏗️ Hybrid execution architecture (Claude Code)

Skills run in two execution modes based on their `context` frontmatter declaration. This architecture requires **Claude Code**'s Agent tool and subagent infrastructure:

| Mode | Behavior | Context access |
| --- | --- | --- |
| `fork` | Isolated subagent context | Handoff files only. No conversation history. Summary returned to main |
| `inline` | Main conversation | Full access to prior agents' reasoning and code decisions |

```
orchestrator (main conversation)
  ├── document-review (fork) ──→ summary returned
  ├── guide-generation (fork) ──→ summary returned
  ├── implementation (inline) ──→ full reasoning accumulated
  └── review (inline) ──→ sees implementation context directly
        ├── APPROVED / DONE_WITH_CONCERNS → stop
        └── REJECTED → implementation (inline) → review (inline) → loop
```

**Why this split:**
- Early stages (`document-review`, `guide-generation`) only need input documents — forking saves context window
- `implementation` and `review` share the main conversation so the review loop converges without context loss
- Claude Code의 서브에이전트는 다른 서브에이전트를 생성할 수 없으므로, orchestrator는 반드시 inline 실행

## ⚙️ Operating modes

Every skill supports two modes, declared in its `SKILL.md` frontmatter:

| Mode | Purpose | Review behavior |
| --- | --- | --- |
| `project-delivery` | Build the actual product from PRD/TRD | Reviews against product completeness and quality expectations |
| `skill-pipeline-validation` | Validate the agent chain, handoff quality, and representative evidence | Reviews contract integrity, session continuity, and proof of execution |

## 🧩 Skills

### 🏷️ Naming system

The pipeline uses three naming systems. The canonical mapping lives in [`agent_registry.py`](./harness/agent_registry.py) and is used by the validation harness to verify `completed_agent` in every handoff manifest.

| Stage name | Skill folder | Agent name (`completed_agent`) | Execution mode |
| --- | --- | --- | :---: |
| `pipeline-orchestrator` | `pipeline-orchestrator-agent` | `android-pipeline-orchestrator-agent` | `inline` |
| `document-review` | `document-reviewer-agent` | `android-document-reviewer-agent` | `fork` |
| `guide-generation` | `code-quality-guide-generator` | `android-code-quality-guide-generator` | `fork` |
| `implementation` | `implementation-agent` | `android-implementation-agent` | `inline` |
| `review` | `review-agent` | `android-review-agent` | `inline` |

### 🎛️ pipeline-orchestrator-agent

**Role:** Control-plane agent — the single entry point and dispatcher for the entire pipeline.

- **SKILL:** [`skills/pipeline-orchestrator-agent/SKILL.md`](./skills/pipeline-orchestrator-agent/SKILL.md)
- **Upstream:** User or external caller
- **Downstream:** Any worker stage based on current state
- **Output:** `docs/generated/orchestrator-handoff.md`
- **Key files:** [`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md) (shared contract for all agents)
- **Dispatch rules:**
  - Reads `session-context.md` and the latest handoff to determine next worker
  - On `Rejected` review: dispatches `implementation` (up to 3 cycles)
  - On `APPROVED` or `DONE_WITH_CONCERNS`: dispatches `stop`
  - Resumes from the exact stage if the session was interrupted

### 📋 document-reviewer-agent

**Role:** First worker — normalizes and refines PRD/TRD documents for downstream consumption.

- **SKILL:** [`skills/document-reviewer-agent/SKILL.md`](./skills/document-reviewer-agent/SKILL.md)
- **Upstream:** `pipeline-orchestrator`
- **Downstream:** `guide-generation` (via orchestrator dispatch)
- **Output:** `docs/generated/document-reviewer-handoff.md`, `docs/generated/context-snapshot.md`
- **What it does:**
  - Reviews initial docs for logical gaps, ambiguity, and Android-specific constraints
  - Produces a corrected, AI-friendly document set for downstream agents
  - In `skill-pipeline-validation`, produces minimal consistent docs rather than expanding scope

### 📐 code-quality-guide-generator

**Role:** Second worker — produces design intent and code quality standards from refined documents.

- **SKILL:** [`skills/code-quality-guide-generator/SKILL.md`](./skills/code-quality-guide-generator/SKILL.md)
- **Upstream:** `pipeline-orchestrator` (reads `document-reviewer-handoff.md`)
- **Downstream:** `implementation` (via orchestrator dispatch)
- **Output:** `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`, `docs/generated/guide-generator-handoff.md`
- **Key files:** [`adr.md`](./skills/code-quality-guide-generator/adr.md), [`code-convention.md`](./skills/code-quality-guide-generator/code-convention.md) (reference templates)
- **What it does:**
  - Synthesizes PRD/TRD + ADR + code conventions into architecture design intent
  - Generates a quality rubric that the review agent uses as its scoring baseline

### 🛠️ implementation-agent

**Role:** Third worker — writes production code and tests based on design intent and quality guide.

- **SKILL:** [`skills/implementation-agent/SKILL.md`](./skills/implementation-agent/SKILL.md)
- **Upstream:** `pipeline-orchestrator` (reads `guide-generator-handoff.md` or `review-handoff-manifest.md` on re-entry)
- **Downstream:** `review` (via orchestrator dispatch)
- **Output:** `docs/generated/handoff-manifest.md`
- **What it does:**
  - Implements code following `design-intent.md` and `code-quality-guide.md`
  - Records `changed_files`, `test_results`, `test_coverage`, `declared_gaps`
  - In `skill-pipeline-validation`, builds a representative path (minimal scaffold + unit tests) rather than full PRD scope

### 🔍 review-agent

**Role:** Fourth worker — classifies issues and decides approval status.

- **SKILL:** [`skills/review-agent/SKILL.md`](./skills/review-agent/SKILL.md)
- **Upstream:** `pipeline-orchestrator` (reads `handoff-manifest.md`)
- **Downstream:** `stop` or `implementation` re-entry (via orchestrator dispatch)
- **Output:** `docs/generated/review-report.md`, `docs/generated/review-handoff-manifest.md`
- **Issue classification system:**

| Classification | Meaning | Blocks approval? |
| --- | --- | :---: |
| `CONTEXT_BREAK` | handoff 필수 키 누락, session 경로 불일치 등 계약/문맥 자체가 깨진 경우 | ✅ Yes |
| `SCOPE_BLOCKER` | 대표 경로 빌드 불가, 핵심 테스트 실패 등 필수 요구사항 미충족 | ✅ Yes |
| `DECLARED_GAP` | 구현 에이전트가 handoff manifest의 `declared_gaps`에 미구현 사유와 함께 명시적으로 선언한 항목. validation 모드에서는 선언 자체가 계약 이행으로 인정됨 | ❌ No |
| `FOLLOW_UP` | deprecation 경고, 버전 권고 등 계약·기능에 영향 없는 참고 사항. 다음 스프린트에서 처리 가능 | ❌ No |

- **Terminal results:** `APPROVED`, `DONE_WITH_CONCERNS`, `REJECTED`
  - `REJECTED` triggers re-implementation loop (max 3 cycles)
  - `DONE_WITH_CONCERNS` requires `CONTEXT_BREAK = 0` and `SCOPE_BLOCKER = 0`

## 📄 Documents used in each stage

| Stage | Main inputs | Main outputs |
| --- | --- | --- |
| `pipeline-orchestrator` | `docs/PRD.md`, `docs/TRD.md`, `session-context.md`, latest handoff, [`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md) | `orchestrator-handoff.md` |
| `document-review` | `orchestrator-handoff.md`, `docs/PRD.md`, `docs/TRD.md`, [`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md) | `document-reviewer-handoff.md`, `context-snapshot.md` |
| `guide-generation` | `session-context.md`, `document-reviewer-handoff.md`, [`adr.md`](./skills/code-quality-guide-generator/adr.md), [`code-convention.md`](./skills/code-quality-guide-generator/code-convention.md) | `design-intent.md`, `code-quality-guide.md`, `guide-generator-handoff.md` |
| `implementation` | `session-context.md`, `guide-generator-handoff.md`, `design-intent.md`, `code-quality-guide.md`, [`adr.md`](./skills/code-quality-guide-generator/adr.md), [`code-convention.md`](./skills/code-quality-guide-generator/code-convention.md) | `handoff-manifest.md` |
| `review` | `session-context.md`, `handoff-manifest.md`, `design-intent.md`, `code-quality-guide.md`, [`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md) | `review-report.md`, `review-handoff-manifest.md` |

All generated files are written to `docs/generated/`.

## 🗂️ Repository layout

```text
.
├── README.md
├── skills/
│   ├── pipeline-orchestrator-agent/
│   │   ├── SKILL.md
│   │   └── agent-session-contract.md
│   ├── document-reviewer-agent/
│   │   └── SKILL.md
│   ├── code-quality-guide-generator/
│   │   ├── SKILL.md
│   │   ├── adr.md
│   │   └── code-convention.md
│   ├── implementation-agent/
│   │   └── SKILL.md
│   └── review-agent/
│       └── SKILL.md
├── .claude/
│   ├── hooks/
│   │   └── check-pipeline-docs.sh  # PRD/TRD pre-validation hook
│   └── settings.json
├── harness/
│   ├── __init__.py
│   ├── agent_registry.py      # canonical naming + manifest specs
│   ├── manifest_parser.py     # markdown → dict parser
│   ├── validation.py          # contract validation logic
│   ├── run_validation.py      # CLI entry point
│   └── tests/
└── test-folder/               # user-created local self-test target (not committed)
```

`test-folder` is not a bundled sample project. It is a local self-test target that users create before validating the skill pipeline against a real Android project.

## 📜 Session and handoff contract

All parser-facing keys are canonical `snake_case`.
The shared contract lives in [`agent-session-contract.md`](./skills/pipeline-orchestrator-agent/agent-session-contract.md).

### 🎯 Design principle

- agents do not rely on chat history
- agents rely on durable markdown artifacts
- every handoff manifest includes `completed_agent` validated against the registry

### 🔑 Handoff manifest required keys (13 common + stage-specific)

Common keys shared by all manifests:

`pipeline_id`, `session_id`, `parent_session_id`, `run_mode`, `review_cycle`, `session_context_path`, `previous_handoff`, `in_scope`, `out_of_scope`, `decision_summary`, `evidence_paths`, `unresolved_issues`, `completed_agent`

Each stage adds its own required keys (e.g., `dispatch_target` for orchestrator, `review_result` for review). See [`agent_registry.py`](./harness/agent_registry.py) for the full spec per stage.

### 🔑 Session context required keys (16 per section)

`pipeline_id`, `run_mode`, `current_stage`, `review_cycle`, `session_id`, `parent_session_id`, `previous_handoff`, `latest_handoff`, `in_scope`, `out_of_scope`, `decision_summary`, `resolved_issues`, `unresolved_issues`, `next_agent_focus`, `evidence_paths`, `carry_forward_rules`

### 📝 Standard generated files

- `docs/generated/orchestrator-handoff.md`
- `docs/generated/session-context.md`
- `docs/generated/document-reviewer-handoff.md`
- `docs/generated/context-snapshot.md`
- `docs/generated/design-intent.md`
- `docs/generated/code-quality-guide.md`
- `docs/generated/guide-generator-handoff.md`
- `docs/generated/handoff-manifest.md`
- `docs/generated/review-report.md`
- `docs/generated/review-handoff-manifest.md`

## 🚀 Quick start

### 1. Prepare project docs

Create `docs/PRD.md` and `docs/TRD.md` in your target project.

### 2. Start from the orchestrator

The workflow always starts from [`skills/pipeline-orchestrator-agent/SKILL.md`](./skills/pipeline-orchestrator-agent/SKILL.md).

It decides:

- whether this is a fresh run or a resumed run
- which worker should run next
- whether the review loop should continue

### 3. Validate contracts

```bash
python3 -m harness.run_validation --project <your-project-dir>
```

For local self-test runs, users commonly validate a git-ignored `test-folder` target before applying the skills to a real Android project.

## 🧪 Validation harness

The harness is intentionally small — it validates skill contracts, not application behavior.

**Current scope:**

- parse canonical markdown manifests into structured dicts
- load and validate session context sections
- verify worker stage sequence against `WORKER_SEQUENCE`
- verify `completed_agent` against `STAGE_TO_AGENT_NAME` registry
- check `pipeline_id` and `run_mode` consistency across all artifacts
- validate orchestrator dispatch logic (stop after terminal review, re-dispatch on reject)
- validate `DONE_WITH_CONCERNS` requires `CONTEXT_BREAK = 0`, `SCOPE_BLOCKER = 0`
- separate build evidence findings from contract findings (`check_build_evidence` flag)
- return a deterministic pass/fail result

**Non-goals:**

- live LLM orchestration
- parallel execution
- deployment automation
- generalized workflow engine
- evidence content validation (harness checks file existence only, not correctness of content)

## 📌 Recommended adoption path

1. Copy the `skills/` folder into your Android project.
2. Create `docs/PRD.md` and `docs/TRD.md`.
3. Start all runs from the orchestrator.
4. Keep generated handoffs in `docs/generated/`.
5. Use the harness to validate contract integrity after each pipeline run.
6. Add stricter delivery rules only after validation mode is stable.

## 📊 Current status

| Component | Status |
| --- | :---: |
| 5-stage orchestrator-based workflow | ✅ |
| Hybrid execution (fork + inline) | ✅ |
| Canonical manifest keys with 3-way naming registry | ✅ |
| Validation harness with `completed_agent` verification | ✅ |
| Shared session contract | ✅ |
| PreToolUse Hook (PRD/TRD pre-validation) | ✅ |

## 📄 License

No license file is currently included in this repository.
