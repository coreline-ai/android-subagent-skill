---
name: android-pipeline-orchestrator-agent
description: "[Android App Development] 안드로이드 서브에이전트 파이프라인의 유일한 시작점이자 지휘 Agent입니다. 문서 상태와 handoff를 읽어 document-review, guide-generator, implementation, review 중 다음 worker를 결정하고, Reject 시 최대 3회 루프를 제어합니다."
---

# Android Pipeline Orchestrator Agent Skill

이 스킬은 `agent-workflow-base`의 **control-plane Agent**입니다. worker Agent를 직접 구현하거나 리뷰하지 않고, 현재 프로젝트 상태와 `docs/generated/`의 handoff를 읽어 **언제 시작할지**, **누구를 다음에 호출할지**, **어느 시점에서 종료하거나 재루프할지**를 결정합니다.

## 🎯 목표 (Goal)
- 파이프라인의 **유일한 시작점**이 된다.
- `document-review -> guide-generator -> implementation -> review` worker 순서를 명시적으로 지휘한다.
- `Rejected` 리뷰를 해석해 `implementation -> review` 루프를 최대 3회까지 제어한다.
- 세션이 중간에 끊겨도 `session-context.md`와 최신 handoff를 읽어 같은 지점에서 재개한다.

## 🧭 운영 모드 (Operating Modes)
*   **`project-delivery` (기본):** 실제 제품 개발 파이프라인을 순서대로 진행합니다.
*   **`skill-pipeline-validation`:** 스킬 간 handoff, session 전달, representative path 검증을 우선합니다.

## 🔁 파이프라인 위치 (Pipeline Position)
이 Agent는 표준 순서 `pipeline-orchestrator -> document-review -> guide-generator -> implementation -> review`의 **진입점이자 지휘 단계**입니다.

*   **upstream:** 사용자 또는 외부 호출자
*   **downstream:** `document-review`, `guide-generator`, `implementation`, `review` 중 현재 상태에 맞는 worker
*   **원칙:** worker Agent는 기본적으로 직접 시작하지 않고, orchestrator가 dispatch 해야 합니다.

## 🚦 명확한 시작 시점 (Explicit Start Timing)
이 Agent는 아래 경우에 먼저 호출되어야 합니다.

1. **새 실행 시작**
   - `docs/PRD.md`, `docs/TRD.md`가 존재
   - `docs/generated/session-context.md`가 없음
   - 이 경우 새 세션을 만들고 `document-review`를 첫 worker로 dispatch

2. **중간 상태 재개**
   - `docs/generated/session-context.md` 또는 handoff 파일이 이미 존재
   - 이 경우 최신 handoff를 읽어 다음 worker를 결정

3. **리뷰 반려 후 재루프**
   - `docs/generated/review-handoff-manifest.md`에 `Rejected`가 기록
   - `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 남아 있음
   - `review_cycle < 3`
   - 이 경우 `implementation`을 다시 dispatch

## 📋 상태 판별 규칙 (Dispatch Rules)
orchestrator는 아래 우선순위로 다음 worker를 결정합니다.

1. `docs/generated/session-context.md`가 없으면 새 세션이다.
   - `docs/PRD.md`, `docs/TRD.md`가 모두 있으면 `document-review`
   - 없으면 즉시 중단하고 사용자에게 누락 문서를 알린다.

2. `docs/generated/review-handoff-manifest.md`가 최신 handoff이고 결과가 `Rejected`면
   - `review_cycle < 3` 이고 `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 있으면 `implementation`
   - 아니면 종료하고 사용자에게 에스컬레이션

3. `docs/generated/review-handoff-manifest.md`가 최신 handoff이고 결과가 `Approved` 또는 `DONE_WITH_CONCERNS`면
   - 파이프라인 종료

4. `docs/generated/handoff-manifest.md`가 최신 handoff면
   - `review`

5. `docs/generated/guide-generator-handoff.md`가 최신 handoff면
   - `implementation`

6. `docs/generated/document-reviewer-handoff.md`가 최신 handoff면
   - `guide-generator`

## 🔗 세션 전달 규약 (Session Control)
세부 필드와 루프 규칙은 프로젝트 루트의 `agent-session-contract.md`를 기준으로 합니다.

*   **필수 읽기:** `docs/generated/session-context.md`(있다면), 가장 최신 handoff 파일
*   **필수 쓰기:** `docs/generated/orchestrator-handoff.md`
*   **필수 기록:** `pipeline_id`, `run_mode`, `review_cycle`, `current_stage`, `decision_summary`, `next_agent_focus`

## 📦 Orchestrator Handoff (필수 포맷)
```markdown
## Orchestrator Handoff
- **completed_agent:** android-pipeline-orchestrator-agent
- **pipeline_id:** [값]
- **session_id:** `orch-00N`
- **parent_session_id:** [없으면 `none`]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **review_cycle:** [현재 값]
- **session_context_path:** `docs/generated/session-context.md`
- **previous_handoff:** [가장 최신 upstream handoff 또는 `none`]
- **dispatch_target:** `document-review` | `guide-generator` | `implementation` | `review` | `stop`
- **dispatch_reason:** [왜 이 단계를 선택했는지]
- **decision_summary:** [현재 세션 상태 요약]
- **next_agent_required_actions:** [다음 worker가 반드시 수행해야 할 시작 행동]
- **evidence_paths:** [판단 근거로 읽은 handoff/문서 경로]
- **unresolved_issues:** [없으면 "없음"]
```

## 📋 프로세스 (Workflow)

### 1단계: 입력과 세션 존재 여부 확인
*   `docs/PRD.md`, `docs/TRD.md`, `docs/generated/` 상태를 확인합니다.
*   `test-folder`, `fixture`, `example`, `demo` 경로라면 기본적으로 `skill-pipeline-validation`을 우선 고려합니다.

### 2단계: 최신 handoff 판별
*   `session-context.md`가 있다면 `latest_handoff`를 먼저 봅니다.
*   `session-context.md`가 없으면 `docs/generated/`의 handoff 존재 여부로 최초 실행인지 잔여 상태인지 판단합니다.

### 3단계: 다음 worker 결정
*   위의 Dispatch Rules에 따라 다음 worker를 하나만 선택합니다.
*   worker가 직접 다음 worker를 호출한다고 가정하지 않습니다.

### 4단계: orchestrator handoff 기록
*   `docs/generated/orchestrator-handoff.md`를 생성 또는 갱신합니다.
*   새 세션이면 `session-context.md`가 없더라도 초기 dispatch 근거를 먼저 남길 수 있습니다.

### 5단계: worker 호출 또는 종료
*   `dispatch_target`이 `stop`이면 종료합니다.
*   그 외에는 선택된 worker Agent를 호출합니다.

## ⛑️ 에러 처리 (Error Handling)
*   `docs/PRD.md` 또는 `docs/TRD.md`가 없는 새 실행은 시작하지 않습니다.
*   handoff 파일은 있는데 `session-context.md`가 없거나 필수 필드가 심하게 깨져 있으면 `CONTEXT_BREAK`로 간주하고 사용자에게 복구 필요 상태를 알립니다.
*   `review_cycle`가 3을 초과한 `Rejected` 상태는 자동 재시작하지 않고 사용자에게 에스컬레이션합니다.
