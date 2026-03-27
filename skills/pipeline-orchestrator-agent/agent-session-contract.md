# Agent Session Contract

`agent-workflow-base`의 모든 Agent는 개별 대화 로그가 아니라 **공통 세션 문서와 Handoff Manifest**를 통해 문맥을 전달해야 한다.

## 목적
- 이전 Agent의 실행 모드, 범위, 판단 근거, 미해결 이슈를 다음 Agent가 대화 로그 없이 이어받게 한다.
- `project-delivery`와 `skill-pipeline-validation`을 구분해 같은 리뷰/구현 루프라도 서로 다른 판정 기준을 적용하게 한다.
- 리뷰 반려가 발생해도 3회 루프 안에서 동일한 세션 체인을 유지하게 한다.

## 표준 파일 경로
- `docs/generated/orchestrator-handoff.md`
- `docs/generated/session-context.md`
- `docs/generated/document-reviewer-handoff.md`
- `docs/generated/guide-generator-handoff.md`
- `docs/generated/handoff-manifest.md`
- `docs/generated/review-handoff-manifest.md`
- `docs/generated/context-snapshot.md` (선택)

## 표준 파이프라인 순서
`agent-workflow-base`의 기본 실행 순서는 아래와 같다.

1. `pipeline-orchestrator`
2. `document-review`
3. `guide-generation`
4. `implementation`
5. `review`

각 Agent는 자신의 upstream/downstream을 이 순서 기준으로 해석해야 한다.

- `pipeline-orchestrator`는 유일한 시작점이며, 각 단계 종료 후 다음 worker를 dispatch 한다.
- `document-review`의 다음 worker 단계는 `guide-generation`
- `guide-generation`의 다음 worker 단계는 `implementation`
- `implementation`의 다음 worker 단계는 `review`
- `review`에서 `Rejected`가 발생하면 orchestrator가 `implementation`으로 되돌리고, 이후 `implementation -> review` worker 루프를 최대 3회 반복한다.

## 시작 시점 규칙
- 새 실행은 기본적으로 `pipeline-orchestrator`에서만 시작한다.
- `docs/generated/session-context.md`가 없고 `docs/PRD.md`, `docs/TRD.md`가 있으면 orchestrator는 새 세션을 시작하고 `document-review`를 첫 worker로 dispatch 한다.
- 새 실행의 첫 worker는 `session-context.md`가 아직 없을 수 있으므로, `docs/generated/orchestrator-handoff.md`를 최초 dispatch 근거로 읽어야 한다.
- `session-context.md`와 최신 handoff가 이미 있으면 orchestrator는 해당 상태를 읽고 다음 worker를 결정한다.
- worker Agent는 기본적으로 **직접 시작점이 아니다.** 명시적인 수동 디버깅 상황이 아니라면 orchestrator의 dispatch 없이 시작하지 않는다.

## session-context.md 필수 필드
아래 키는 모두 **lowercase `snake_case`** 로 고정한다. 한국어 라벨이나 공백이 포함된 키는 parser 대상 필드에 사용하지 않는다.
- `pipeline_id`
- `run_mode`
- `current_stage`
- `review_cycle`
- `session_id`
- `parent_session_id`
- `previous_handoff`
- `latest_handoff`
- `in_scope`
- `out_of_scope`
- `decision_summary`
- `resolved_issues`
- `unresolved_issues`
- `next_agent_focus`
- `evidence_paths`
- `carry_forward_rules`

## Handoff Manifest 필수 필드
아래 키는 모두 **lowercase `snake_case`** 로 고정한다. 사람을 위한 설명은 값이나 본문 prose에 남기고, 키 이름 자체는 바꾸지 않는다.
- `pipeline_id`
- `session_id`
- `parent_session_id`
- `run_mode`
- `review_cycle`
- `session_context_path`
- `previous_handoff`
- `in_scope`
- `out_of_scope`
- `decision_summary`
- `evidence_paths`
- `next_agent_required_actions`
- `unresolved_issues`

## Agent 공통 동작 규칙
1. 작업 시작 전 `docs/generated/session-context.md`와 가장 최근 upstream handoff를 반드시 읽는다.
2. 작업 종료 후 `session-context.md`를 overwrite 하지 않고 append 한다.
3. 자신의 판단을 요약한 `decision_summary`와 증거 경로 `evidence_paths`를 남긴다.
4. 다음 Agent가 반드시 처리해야 할 항목은 `next_agent_required_actions`에 구조적으로 기록한다.
5. 선택적 참고 자료인 `context-snapshot.md`는 있어도 좋지만, 필수 계약은 항상 `session-context.md`와 handoff에 둔다.
6. worker Agent는 handoff를 작성한 뒤 다음 worker를 직접 호출하지 않고, orchestrator가 해당 handoff를 읽어 다음 단계를 dispatch 하게 한다.

## 리뷰 루프 규칙
- `Rejected`는 `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 남아 있을 때만 사용한다.
- `DECLARED_GAP`와 `FOLLOW_UP`는 기본적으로 기록만 하고 자동 Reject 사유로 승격하지 않는다.
- 루프는 최대 3회까지 자동 진행한다.
- 2회차 이상에서는 `next_agent_required_actions`에 적힌 항목만 필수 수정 대상으로 승격한다.
- 3회차 이후에도 `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 남으면 자동 루프를 중단하고 사용자에게 에스컬레이션한다.
- `review_cycle` 증가는 orchestrator가 `Rejected` 리뷰를 해석하고 다음 `implementation` 루프를 시작할 때 수행한다.

## 운영 모드 해석
- `project-delivery`: PRD/TRD 기준의 실제 제품 구현과 품질 완결성을 목표로 한다.
- `skill-pipeline-validation`: 파이프라인 계약, 문맥 전달, representative path, 빌드/테스트 증거를 검증한다. 전체 PRD 미구현만으로 자동 실패시키지 않는다.

## 실행 모드 (Execution Mode) — Claude Code 전용
각 worker 스킬은 SKILL.md frontmatter의 `context` 필드에 따라 두 가지 실행 모드 중 하나로 동작한다. 이 선언은 정적이며 런타임에 동적으로 전환되지 않는다. `fork`/`inline` 실행 모드 구분은 **Claude Code의 Agent tool과 서브에이전트 인프라**에서만 적용된다.

### 허용 값
- `fork`: 격리된 서브에이전트 컨텍스트에서 실행한다. 메인 대화의 기록에 접근하지 않으며, handoff 파일과 SKILL.md만을 입력으로 받는다. 완료 시 요약만 메인 대화로 반환된다.
- `inline` (기본값): 메인 대화에서 실행한다. 이전 에이전트의 전체 사고 과정과 코드 결정에 접근 가능하다.

### 단계별 실행 모드

| 단계 | execution_mode | 근거 |
| --- | --- | --- |
| `pipeline-orchestrator` | `inline` | 메인 대화의 진입점이자 제어 주체 |
| `document-review` | `fork` | PRD/TRD 파일 기반 작업. 이전 대화 맥락 불필요 |
| `guide-generation` | `fork` | document-reviewer handoff 파일 기반. 이전 대화 맥락 불필요 |
| `implementation` | `inline` | review가 구현 과정을 참조해야 하므로 메인 대화에 누적 |
| `review` | `inline` | implementation의 사고 과정을 직접 참조하여 리뷰 |

### 설계 근거
- `document-review`와 `guide-generation`은 입력 문서만으로 충분히 동작하므로, fork로 격리하여 메인 대화의 컨텍스트 윈도우를 절약한다.
- `implementation`과 `review`는 메인 대화에서 실행하여 implementation ↔ review 루프에서 맥락 손실 없이 품질을 수렴시킨다.
- Claude Code의 서브에이전트는 다른 서브에이전트를 생성할 수 없으므로, orchestrator는 반드시 메인 대화에서 실행해야 한다.
