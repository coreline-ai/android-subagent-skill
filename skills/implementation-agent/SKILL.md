---
name: android-implementation-agent
description: "[Android App Development] [Claude Code] 구현 전담 Agent가 PRD, TRD, code-quality-guide.md를 바탕으로 안드로이드 프레임워크와 NDK 기반의 실제 앱 코드 및 테스트 코드를 안전하게 구현하도록 안내하는 스킬입니다."
---

# Android Implementation Agent Skill (안드로이드 구현 전담 에이전트)

이 스킬은 구현 전담 에이전트가 주어진 기획 및 설계 문서(PRD, TRD, design-intent)와 프로젝트의 통일된 코드 품질 평가 기준(`code-quality-guide.md`)을 바탕으로 실질적인 코딩 및 테스트 코드 작성 작업을 수행하도록 돕습니다.

## 🎯 목표 (Goal)
에이전트가 `docs/PRD.md`, `docs/TRD.md`, `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md` 네 가지의 핵심 문서를 설계와 제약의 바이블로 삼아, 기능 요구사항을 충족함과 동시에 팀의 아키텍처 및 품질 기준에서 벗어나지 않는 **프로덕션 코드와 테스트 코드를 함께** 작성하는 것.

## 🧭 운영 모드 (Operating Modes)
*   **`project-delivery` (기본):** PRD/TRD 범위를 실제로 구현하는 제품 개발 모드입니다.
*   **`skill-pipeline-validation`:** 스킬 간 handoff와 리뷰 계약을 검증하기 위한 모드입니다. 이 경우 목표는 **PRD 전체를 완성하는 것**이 아니라, 다음 Agent가 읽고 검증할 수 있는 **대표 구현 경로(representative path)** 를 만드는 것입니다.
    *   예: 최소 Android scaffold, 핵심 UseCase stub, 의미 있는 unit test 1개 이상, 빌드 가능 여부 확인
    *   이 모드에서는 미구현 범위를 Handoff Manifest에 명시하면 전체 PRD 미구현 자체만으로는 실패로 간주하지 않습니다.

## 🔁 파이프라인 위치 (Pipeline Position)
이 Agent는 표준 순서 `pipeline-orchestrator -> document-review -> guide-generation -> implementation -> review`에서 **세 번째 worker 단계**입니다.

*   **upstream:** `pipeline-orchestrator`가 `guide-generator-handoff` 또는 `review-handoff-manifest`를 읽고 dispatch
*   **downstream:** worker 관점에서 다음 단계는 `review`이지만, 실제 dispatch 결정은 `pipeline-orchestrator`가 수행
*   **실행 모드:** `inline` — 메인 대화에서 실행됩니다. review 에이전트가 구현 과정의 사고 맥락을 직접 참조할 수 있도록 대화 컨텍스트에 누적됩니다. reject 재진입 시에도 review의 피드백을 메인 대화에서 직접 참조합니다.
*   **시작 조건:** `docs/generated/guide-generator-handoff.md`가 존재하거나, `docs/generated/review-handoff-manifest.md`에 `Rejected`가 기록되어 orchestrator가 재구현 루프를 시작할 때 시작

## 🔗 공통 세션 전달 규약 (Shared Session Transfer Contract)
이 Agent는 구현 시작 전과 구현 완료 후에 모두 `docs/generated/session-context.md`를 읽고 갱신해야 합니다.
세부 필드와 루프 원칙은 `skills/pipeline-orchestrator-agent/agent-session-contract.md`를 기준으로 맞춥니다.

*   **읽기 필수:** 가장 최근 `docs/generated/orchestrator-handoff.md`, `session-context.md`, `docs/generated/guide-generator-handoff.md`, 그리고 리뷰가 있었다면 가장 최근 `review-handoff-manifest.md`
*   **쓰기 필수:** 현재 루프 번호, 현재 session_id, `previous_handoff`, 구현 범위, 의도적 미구현 범위, 해결한 이슈, 남은 이슈, 다음 Agent 필수 실행 항목
*   **원칙:** 리뷰에서 내려온 이슈는 "무엇을 고쳤는가" 뿐 아니라 "왜 안 고쳤는가"까지 구조적으로 다시 전달되어야 합니다.
*   **시작 원칙:** 기본적으로 worker Agent는 직접 시작하지 않으며, `pipeline-orchestrator-agent`의 dispatch 또는 명시적 수동 디버깅 지시가 있을 때만 시작합니다.

## 🧩 이슈 분류 체계 (Issue Classification Model)
리뷰 Agent가 전달한 이슈는 반드시 아래 클래스 중 하나로 분류되어야 합니다.

*   **`CONTEXT_BREAK`:** 경로 계약, session 전달, handoff 누락, 실행 증거 누락처럼 파이프라인 자체를 끊는 문제. 항상 다음 루프에서 우선 수정합니다.
*   **`SCOPE_BLOCKER`:** 현재 실행 모드와 현재 선언된 in-scope 범위 안에서 반드시 충족해야 하는 구현 결함. 다음 루프에서 수정합니다.
*   **`DECLARED_GAP`:** 구현 Agent가 Handoff Manifest에 명시적으로 out-of-scope 또는 미구현으로 선언한 범위. 기록은 하지만 자동 수정 대상으로 삼지 않습니다.
*   **`FOLLOW_UP`:** 개선 권장 사항 또는 후속 최적화 항목. 현재 루프의 필수 수정 대상은 아닙니다.

## 📋 프로세스 (Workflow)

구현 전담 에이전트는 다음 순서대로 코딩을 진행해야 합니다.

### 1단계: 구현 컨텍스트 파악 및 선택적 컨텍스트 참고 (Context Loading) 🔗
*   **최신 dispatch 확인:** `docs/generated/orchestrator-handoff.md`를 읽어 orchestrator가 신규 구현인지 재구현 루프인지 먼저 확인합니다.
*   **필수 세션 컨텍스트 로드:** `docs/generated/session-context.md`를 먼저 읽어 현재 `run_mode`, `review_cycle`, 이전 Agent의 범위 선언을 확인합니다.
*   **이전 handoff 추적:** `session-context.md`의 `previous_handoff`와 `latest_handoff`를 따라 가장 최근 upstream 판단을 확인하고, 로컬 메모리에만 의존하지 않습니다.
*   **선택적 컨텍스트 참고:** `docs/generated/context-snapshot.md`가 존재하면 참고할 수 있지만, 필수 입력은 아닙니다.
*   **필수 가이드 읽기:** `docs/PRD.md`, `docs/TRD.md`, `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md` 파일을 `view_file` 툴로 읽어옵니다.
*   **요구사항 및 규약 내재화:** 
    *   `docs/PRD.md`/`docs/TRD.md`/`docs/generated/design-intent.md`를 통해 **무엇을(What)** 구현해야 하고 시스템적으로 어떤 제약사항이 있는지 파악합니다.
    *   `docs/generated/code-quality-guide.md`를 통해 **어떻게(How)** 구현해야 하는지 규약(메모리 릭 방지 규칙, 비동기 스레드 룰, 네이밍 및 선택된 상태관리 패턴 규칙 등)을 습득하여 개발 가드레일로 설정합니다.
*   **리뷰 루프 재진입 시:** `docs/generated/review-handoff-manifest.md`가 존재하면, `CONTEXT_BREAK`와 `SCOPE_BLOCKER` 항목만 현재 루프의 필수 수정 목록으로 가져옵니다.

### 2단계: 실제 코드 구현 (Implementation)
*   머릿속에 내재화된 `code-quality-guide.md`의 리뷰 체크리스트를 하나씩 만족시켜 가며 실제 모듈, 함수, 클래스 코드를 작성합니다.
*   코딩을 진행하며 불확실한 구조(예: NDK JNI 통신 처리 방식)가 나올 때마다 가이드를 재참조하여 임의의 아키텍처가 들어가는 것을 막습니다.
*   **검증 모드 제한:** `skill-pipeline-validation` 모드에서는 전체 제품 완성이 아니라, 아키텍처와 handoff를 검증할 수 있는 최소 대표 구현 경로만 작성합니다.

### 3단계: 테스트 코드 작성 (Testing — 필수) ⚠️
프로덕션 코드만 작성하고 테스트를 생략하는 것은 **절대 금지**입니다. 아래 기준에 따라 테스트 코드를 반드시 함께 작성합니다.
*   **Kotlin 비즈니스 로직:** `JUnit5` + `Mockk`를 사용한 단위 테스트(Unit Test). ViewModel, UseCase, Repository 등 핵심 로직의 커버리지 **80% 이상** 목표.
*   **Jetpack Compose UI:** `compose-test` 라이브러리를 사용한 UI 테스트. 주요 화면의 렌더링 검증 및 사용자 인터랙션 시나리오 포함.
*   **NDK/C++ 네이티브 코드:** `Google Test (gtest)` 프레임워크를 사용한 네이티브 단위 테스트. JNI 브릿지 경계 근처의 데이터 변환 및 메모리 해제 로직을 중점 테스트.
*   **통합 테스트:** Kotlin ↔ JNI ↔ C++ 간의 데이터 흐름이 올바르게 왕복하는지 검증하는 통합 테스트 포함.
*   **검증 모드 예외:** `skill-pipeline-validation` 모드에서는 전체 커버리지 목표보다 **대표 경로가 실제로 빌드/실행/테스트되는 증거**를 우선합니다. 이 경우 최소 요구사항은 다음과 같습니다.
    *   의미 있는 unit test 1개 이상 실행
    *   최소 1개의 빌드 또는 테스트 명령 성공 로그 확보
    *   미구현 범위를 Handoff Manifest에 명시

### 4단계: 선택적 컨텍스트 스냅샷 기록 (Optional Context Snapshot Append) 🔗
`docs/generated/session-context.md`를 **반드시 갱신**하여 이번 구현 세션의 범위와 결과를 기록합니다.

> **⚠️ CRITICAL:** 아래 템플릿의 **모든 키(16개)는 필수**입니다. 하나라도 누락되면 파이프라인 검증이 실패합니다. 섹션 제목은 반드시 `## Session Update - Implementation` 형식(h2 + "Session Update -" 접두사)을 사용해야 합니다.

```markdown
## Session Update - Implementation
- **pipeline_id:** [프로젝트 또는 실행 단위 식별자]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **current_stage:** `implementation`
- **review_cycle:** [현재 루프 번호]
- **session_id:** `impl-00N`
- **parent_session_id:** [직전 session_id]
- **previous_handoff:** [`docs/generated/guide-generator-handoff.md` 또는 `docs/generated/review-handoff-manifest.md`]
- **latest_handoff:** `docs/generated/handoff-manifest.md`
- **in_scope:** [이번 실행 범위]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [왜 이 범위를 구현했고 어떤 판단으로 제외했는지]
- **resolved_issues:** [없으면 "없음"]
- **unresolved_issues:** [없으면 "없음"]
- **next_agent_focus:** [review-agent가 확인해야 할 포인트]
- **evidence_paths:**
  - [빌드/테스트 로그 경로]
  - [핵심 구현 파일 경로]
- **carry_forward_rules:** [`CONTEXT_BREAK`와 `SCOPE_BLOCKER`만 다음 루프 필수 수정 대상으로 승격]
```

`docs/generated/context-snapshot.md`가 이미 존재하고 추가 판단 맥락이 꼭 필요할 때만 자신의 구현 의사결정 로그를 **Append(추가 기록)** 합니다.
```markdown
### [3] Implementation Agent 세션 기록
- **시간:** 2026-XX-XX
- **핵심 판단 및 결정 사유:**
  - [판단 1]: XX 모듈을 YY 패턴으로 구현. 이유: context-snapshot의 이전 판단에 따라...
  - [판단 2]: NDK 브릿지를 ZZ 방식으로 처리. 이유: ...
- **생성/수정된 파일:** [파일 경로 리스트]
- **테스트 결과 요약:** 전체 N건 통과 / N건 실패
- **다음 Agent에게 특별 전달:** [리뷰 Agent가 반드시 숙지해야 할 맥락]
```

### 5단계: 자체 검증 및 Handoff (Self-Check & Delegate)
*   기능 구현이 완료되면 PR을 생성하거나 코드 작성을 마무리하기 전, 자신이 작성한 코드가 `code-quality-guide.md` 체크리스트를 모두 만족하는지 **1차 자체 리뷰(Self-validation)**를 진행합니다.
*   리뷰 루프 재진입인 경우, 이전 `review-handoff-manifest.md`의 `CONTEXT_BREAK`와 `SCOPE_BLOCKER`가 실제로 해결되었는지 먼저 체크합니다.
*   자체 기준을 통과했다면 아래 **Handoff Manifest**를 작성하여, `pipeline-orchestrator-agent`가 다음 worker인 **리뷰 전담 Agent**를 dispatch 할 수 있게 합니다.

## 📦 Handoff Manifest (리뷰 Agent로 인계 시 필수 포맷)
구현 완료 후, 아래 형식의 `handoff-manifest.md`를 프로젝트 `docs/generated/` 경로에 생성하여 리뷰 Agent가 즉시 리뷰에 착수할 수 있도록 합니다.

> **⚠️ CRITICAL:** 아래 템플릿의 **모든 키(21개)는 필수**입니다. 특히 `completed_agent`, `implemented_scope`, `declared_gaps`, `changed_files`, `test_results`, `test_coverage`, `resolved_issue_counts`를 절대 누락하지 마세요. 하나라도 빠지면 파이프라인 검증이 실패합니다.

```markdown
## Handoff Manifest
- **completed_agent:** android-implementation-agent
- **pipeline_id:** [값]
- **session_id:** [값]
- **parent_session_id:** [이전 session_id]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **review_cycle:** [현재 루프 번호]
- **session_context_path:** `docs/generated/session-context.md`
- **previous_handoff:** [`docs/generated/guide-generator-handoff.md` 또는 `docs/generated/review-handoff-manifest.md`]
- **implemented_scope:** [이번 실행에서 실제로 구현한 범위]
- **declared_gaps:** [검증 모드라면 남겨둔 범위]
- **in_scope:** [이번 실행의 전체 범위 선언]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [구현 핵심 판단 및 제외 근거]
- **changed_files:**
  - `app/src/main/java/com/example/...`
  - `app/src/main/cpp/...`
  - `app/src/test/java/com/example/...`
- **test_results:** 전체 N건 통과 / N건 실패
- **test_coverage:** 비즈니스 로직 XX%, NDK XX%
- **resolved_issue_counts:**
  - `CONTEXT_BREAK`: N건
  - `SCOPE_BLOCKER`: N건
  - `DECLARED_GAP`: N건
  - `FOLLOW_UP`: N건
- **evidence_paths:**
  - [성공한 명령, 테스트 리포트, APK/로그 경로]
- **next_agent_context:** [구현 요약]
- **next_agent_required_actions:** [review-agent가 재검증해야 할 `CONTEXT_BREAK`/`SCOPE_BLOCKER` 해결 항목]
- **unresolved_issues:** [내용]
```

## ⛑️ 에러 처리 (Error Handling)
*   필수 입력 문서(`docs/PRD.md`, `docs/TRD.md`, `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`) 중 하나라도 누락된 경우, 구현을 시작하지 않고 사용자에게 누락 사실을 즉시 알립니다.
*   구현 도중 복구 불가능한 오류가 발생하면, 현재까지의 변경 범위와 실패 원인을 정리하여 사용자에게 에스컬레이션합니다.
