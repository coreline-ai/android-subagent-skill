---
name: android-code-quality-guide-generator
description: "[Android App Development] [Claude Code] sub-agent가 안드로이드 앱의 PRD, TRD 등을 분석하여 안드로이드 아키텍처(Kotlin/NDK) 기반 설계 의도 문서(design-intent.md)와 코드 품질 평가 기준(code-quality-guide.md)을 수립하도록 안내하는 안드로이드 전용 스킬입니다."
context: fork
---

# Android Code Quality Guide Generator Skill

이 스킬은 서브 에이전트(sub-agent)가 기존에 정의된 요구사항 및 설계 문서(PRD, TRD)와 코딩 컨벤션(code-convention.md), 아키텍처 결정 사항(adr.md)을 바탕으로, 아키텍처 구현 목적을 담은 **설계 의도 문서(`design-intent.md`)**와 **코드 품질 가이드라인(`code-quality-guide.md`)**을 자동으로 생성하도록 돕습니다.

## 🎯 목표 (Goal)
에이전트가 프로젝트 내 핵심 문서들을 분석하여 다음 내용을 달성하는 것입니다:
1. PRD와 TRD를 기반으로 구현의 목적과 구체적 아키텍처 설계를 담은 **설계 의도 문서(design-intent.md)** 도출
2. 위 문서와 ADR, 코딩 컨벤션을 융합하여 실무 리뷰의 기준이 되는 **평가 가이드라인(code-quality-guide.md)** 도출
3. 최종적으로 **리뷰 전담 Agent (Review Agent)**가 이 설계 문서들과 품질 가이드를 기반으로 프로젝트 코드 및 산출물을 완벽히 리뷰할 수 있도록 핸드오프(Handoff) 처리

## 🧭 운영 모드 (Operating Modes)
*   **`project-delivery` (기본):** 실제 제품 구현/리뷰를 위한 설계 의도와 품질 기준을 생성합니다.
*   **`skill-pipeline-validation`:** 스킬 파이프라인 자체를 검증하는 상황에서는, **다음 Agent가 구현 및 리뷰 계약을 테스트할 수 있을 만큼 충분한 최소 기준**을 생성합니다. 이 모드에서는 제품 전체 출시 기준표를 무조건 전부 강제하지 않습니다.

## 🔁 파이프라인 위치 (Pipeline Position)
이 Agent는 표준 순서 `pipeline-orchestrator -> document-review -> guide-generation -> implementation -> review`에서 **두 번째 worker 단계**입니다.

*   **upstream:** `pipeline-orchestrator`가 `document-reviewer-handoff`를 읽고 dispatch
*   **downstream:** worker 관점에서 다음 단계는 `implementation`이지만, 실제 dispatch 결정은 `pipeline-orchestrator`가 수행
*   **시작 조건:** `docs/generated/document-reviewer-handoff.md`가 존재하고 orchestrator가 설계 의도/품질 기준 생성이 필요하다고 판단했을 때 시작

## 🔗 공통 세션 전달 규약 (Shared Session Transfer Contract)
이 Agent는 반드시 `docs/generated/session-context.md`와 이전 Handoff Manifest를 읽고, 자신의 판단을 다시 `session-context.md`에 append 해야 합니다.
세부 필드와 루프 원칙은 `skills/pipeline-orchestrator-agent/agent-session-contract.md`를 기준으로 맞춥니다.

*   **읽기 필수:** `docs/generated/orchestrator-handoff.md`(있다면), `docs/generated/session-context.md`, `docs/generated/document-reviewer-handoff.md`
*   **갱신 필수 항목:** `current_stage`, `session_id`, `parent_session_id`, `previous_handoff`, `latest_handoff`, `decision_summary`, `unresolved_issues`, `next_agent_focus`, `evidence_paths`
*   **목적:** 다음 Agent가 "무엇을 만들었는지" 뿐 아니라 "왜 그런 판단을 했는지"와 "현재 루프가 몇 차인지"를 잃지 않게 합니다.
*   **시작 원칙:** 기본적으로 worker Agent는 직접 시작하지 않으며, `pipeline-orchestrator-agent`의 dispatch 또는 명시적 수동 디버깅 지시가 있을 때만 시작합니다.

## 📋 프로세스 (Workflow)

에이전트는 이 스킬을 호출받았을 때 다음 순서대로 작업을 수행해야 합니다.

### 1단계: 문서 수집 및 선택적 컨텍스트 참고 (Context Gathering) 🔗
*   **최신 dispatch 확인:** `docs/generated/orchestrator-handoff.md`가 존재하면 먼저 읽어 orchestrator가 현재 어떤 worker 단계로 보냈는지 확인합니다.
*   **필수 세션 컨텍스트 로드:** `docs/generated/session-context.md`를 먼저 읽어 이전 Agent의 실행 모드와 범위를 확인합니다.
*   **선택적 컨텍스트 참고:** `docs/generated/context-snapshot.md`가 존재하면 참고할 수 있지만, 필수 입력은 아닙니다.
*   **분석 대상 읽기:** 프로젝트의 `docs/PRD.md`, `docs/TRD.md`, `code-convention.md`, `adr.md`를 `view_file` 툴을 통해 각각 읽어옵니다. (참조 템플릿은 `skills/code-quality-guide-generator/adr.md`, `skills/code-quality-guide-generator/code-convention.md`에 있습니다.)
*   **핵심 원칙 추출:**
    *   `PRD` & `TRD`: 비즈니스 로직, 요구되는 제품 스펙, 소프트웨어 아키텍처 제약 사항 등 의도를 파악합니다.
    *   `code-convention.md` & `adr.md`: 디자인 패턴, 명명 규칙, 비동기 제어 구조 및 기타 기술적 합의 사항을 식별합니다.

### 2단계: 설계 의도 문서 생성 (Generating Design Intent)
수집된 PRD와 TRD를 종합하여, 시스템이 "왜 이렇게 설계되었으며, 어떤 비즈니스/기술적 제약을 만족해야 하는지"를 명시합니다.
*   `write_to_file`을 사용해 `docs/generated/design-intent.md`를 생성합니다.
*   *포함 내용:* 핵심 목적, 주요 유스케이스 시나리오, 설계된 데이터 흐름/아키텍처, 예외 상황에 대한 방어 로직 기준 등.

### 3단계: 코드 품질 평가 기준 수립 및 가이드 생성 (Quality Guide Formulation)
`docs/generated/design-intent.md`와 기존 컨벤션 기반으로 구체화된 체크리스트를 구성하여 `docs/generated/code-quality-guide.md`를 생성합니다.
*   **설계 의도 부합성:** 코드가 `docs/generated/design-intent.md`에서 지시한 구현 목적과 비즈니스 룰을 누락 없이 정확히 따르고 있는가?
*   **구조적 일관성:** TRD/ADR에서 채택된 시스템 아키텍처(예: MVI가 명시적으로 채택된 경우 해당 패턴)를 오용하지 않고 제대로 반영했는가?
*   **메모리/성능/안전성:** 비동기 및 통신 스레드 스코프 준수 여부, 메모리 관리에 문제가 없는가?
*   **클린 코드 준수:** 변수명 작성 규칙과 포매터를 따르는가?
*   **검증 모드 제한:** `skill-pipeline-validation` 모드에서는 품질 가이드를 작성할 때, "파이프라인 검증에 필요한 필수 체크리스트"와 "후속 실제 제품 구현 시 채워야 할 항목"을 구분해 표현합니다.

### 4단계: 세션 컨텍스트 갱신 및 선택적 스냅샷 기록 (Session Update) 🔗
`docs/generated/session-context.md`를 **반드시 갱신**하여 자신의 판단을 누적 기록합니다.

> **⚠️ CRITICAL:** 아래 템플릿의 **모든 키(16개)는 필수**입니다. 하나라도 누락되면 파이프라인 검증이 실패합니다. 섹션 제목은 반드시 `## Session Update - Guide Generation` 형식(h2 + "Session Update -" 접두사)을 사용해야 합니다.

```markdown
## Session Update - Guide Generation
- **pipeline_id:** [프로젝트 또는 실행 단위 식별자]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **current_stage:** `guide-generation`
- **review_cycle:** [현재 값]
- **session_id:** `guide-gen-001`
- **parent_session_id:** [orchestrator session_id]
- **previous_handoff:** `docs/generated/document-reviewer-handoff.md`
- **latest_handoff:** `docs/generated/guide-generator-handoff.md`
- **in_scope:** [이번 실행 범위]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [설계 의도와 품질 기준의 핵심 생성 판단]
- **resolved_issues:** [없으면 "없음"]
- **unresolved_issues:** [없으면 "없음"]
- **next_agent_focus:** [implementation-agent가 구현 시 주의할 범위]
- **evidence_paths:** [`docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`]
- **carry_forward_rules:** [`skills/pipeline-orchestrator-agent/agent-session-contract.md` 기준]
```

`docs/generated/context-snapshot.md`가 이미 존재하고 추가 판단 맥락이 꼭 필요할 때만 자신의 의사결정 로그를 **Append(추가 기록)** 합니다.
```markdown
### [2] Code Quality Guide Generator Agent 세션 기록
- **시간:** 2026-XX-XX
- **핵심 판단 및 결정 사유:**
  - [판단 1]: `docs/generated/design-intent.md`에 XX 아키텍처 제약을 반영. 이유: ...
  - [판단 2]: `docs/generated/code-quality-guide.md`에 YY 체크리스트 항목 추가. 이유: ...
- **생성된 문서:** `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`
- **다음 Agent에게 특별 전달:** [후속 Agent가 반드시 숙지해야 할 맥락]
```

### 5단계: 리뷰 Agent 연계 체계 수립 (Handoff to Review Agent)
최종 완성된 두 산출물(`docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`)을 향후 호출될 **리뷰 Agent**가 어떻게 활용할지 컨텍스트를 제공합니다.
*   실제 다음 dispatch는 `pipeline-orchestrator-agent`가 수행하며, orchestrator가 이후 worker인 `implementation-agent`를 시작할 수 있게 handoff를 남깁니다.

## 📦 Handoff Manifest (구현 Agent로 인계 시 필수 포맷)

> **⚠️ CRITICAL:** 아래 템플릿의 **모든 키(16개)는 필수**입니다. 특히 `completed_agent`, `generated_artifacts`, `next_agent_context`를 절대 누락하지 마세요. 하나라도 빠지면 파이프라인 검증이 실패합니다.

```markdown
## Handoff Manifest
- **completed_agent:** android-code-quality-guide-generator
- **pipeline_id:** [값]
- **session_id:** [값]
- **parent_session_id:** [이전 session_id]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **review_cycle:** [현재 값]
- **session_context_path:** `docs/generated/session-context.md`
- **previous_handoff:** `docs/generated/document-reviewer-handoff.md`
- **generated_artifacts:**
  - `docs/generated/design-intent.md`
  - `docs/generated/code-quality-guide.md`
- **in_scope:** [이번 실행 범위]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [설계 의도 및 품질 기준 생성 핵심 판단]
- **evidence_paths:** [`docs/PRD.md`, `docs/TRD.md`, `code-convention.md`, `adr.md`]
- **next_agent_context:** [설계 의도 및 품질 기준 요약]
- **next_agent_required_actions:** [`docs/generated/session-context.md`, `docs/generated/design-intent.md`, `docs/generated/code-quality-guide.md`를 먼저 로드]
- **unresolved_issues:** [내용]
```

## 💡 실행 시 주의사항
*   생성되는 산출물들은 이론보다는 즉시 리뷰 Agent가 조건문(Checklist)으로 활용할 수 있게끔 **매우 명확하고 검증 가능한 형태(Actionable Test-cases)**로 기재되어야 합니다.
*   모든 산출물은 `docs/generated/` 디렉토리에 저장해야 합니다.

## ⛑️ 에러 처리 (Error Handling)
*   이전 Agent(document-reviewer-agent)의 Handoff Manifest가 없으면 `docs/` 폴더에서 직접 문서를 탐색합니다. 그래도 PRD/TRD가 없으면 사용자에게 즉시 알립니다.
*   `code-convention.md` 또는 `adr.md`가 누락된 경우, 해당 문서 없이 생성 가능한 범위만 작성하고 누락 사실을 Handoff Manifest의 '주의 사항'에 명시합니다.
