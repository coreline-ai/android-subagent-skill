---
name: android-document-reviewer-agent
description: "[Android App Development] 안드로이드 최초 기획 문서(PRD, TRD 등)를 리뷰하고 안드로이드 생태계 제약사항에 맞춰 정합성을 교정, 문서를 상세화하는 안드로이드 메인 Agent 스킬입니다."
---

# Android Document Reviewer Agent Skill (안드로이드 메인 기획/문서 검토 에이전트)

이 스킬은 최우선적으로 **안드로이드 앱 개발(Android App Development)** 생태계를 기반으로 동작하는 **메인 기획/검토 에이전트용 스킬**입니다. 
초기 제공된 `docs/` 폴더 내의 안드로이드 기획 및 설계 문서(PRD, TRD 등)를 모바일 OS 환경(Lifecycle, 권한, 백그라운드 스레드 제약 등)에 맞추어 다듬고 고도화하여, 전체 안드로이드 프로젝트의 논리적 아키텍처 뼈대를 탄탄하게 다집니다.

## 🎯 목표 (Goal)
초안 상태인 문서들의 논리적 모순, 누락점, 모호한 표현을 찾아내어 먼저 교정하고, 이후 투입될 **'가이드 자동화', '구현', '리뷰' 에이전트들이 오해의 소지 없이 지시를 정확히 수행할 수 있도록** 문서를 AI 친화적이고 극도로 상세하게 재정비(Refining)하는 것.

## 🧭 운영 모드 (Operating Modes)
이 스킬은 아래 두 모드 중 하나로 동작합니다.

*   **`project-delivery` (기본):** 실제 프로젝트 구현을 전제로 PRD/TRD를 상세화합니다.
*   **`skill-pipeline-validation`:** `test-folder`, `fixture`, `example`, `demo` 같은 검증용 경로에서는 **다음 Agent가 문서를 정상 소비할 수 있는지 검증하는 최소/일관된 문서 세트**를 만드는 것을 우선합니다. 이 모드에서는 제품 스코프를 불필요하게 확장하지 않습니다.

## 🔁 파이프라인 위치 (Pipeline Position)
이 Agent는 표준 순서 `document-review -> guide-generator -> implementation -> review`의 **첫 단계**입니다.

*   **upstream:** 없음
*   **downstream:** `guide-generator`

## 🔗 공통 세션 전달 규약 (Shared Session Transfer Contract)
모든 Agent는 공통적으로 `docs/generated/session-context.md` 와 각 단계의 Handoff Manifest를 통해 문맥을 전달해야 합니다.
세부 필드와 루프 원칙은 프로젝트 루트의 `agent-session-contract.md`를 기준으로 합니다.

*   **`session-context.md`는 필수:** 파이프라인 전체의 실행 모드, 세션 ID, 현재 단계, in-scope/out-of-scope, 미해결 이슈, 다음 Agent 주의사항을 누적 기록합니다.
*   **Handoff Manifest는 단계별 계약서:** 현재 Agent의 산출물, 실행 범위, 다음 Agent가 실제로 처리해야 할 항목을 구조화해 전달합니다.
*   **필수 필드:** `pipeline_id`, `session_id`, `parent_session_id`, `run_mode`, `review_cycle`, `session_context_path`, `previous_handoff`, `decision_summary`, `evidence_paths`, `next_agent_required_actions`
*   **루프 공통 원칙:** 이후 리뷰 단계에서 반려가 발생하더라도, 다음 Agent는 `session-context.md`와 가장 최근 Handoff Manifest를 기준으로 현재 루프 상태를 이어받아야 합니다.

## 📋 프로세스 (Workflow)

메인 에이전트는 기획 단계에서 다음의 주요 절차를 수행해야 합니다.

### 1단계: 최초 입력 문서 일괄 로딩 및 분석 (Initial Review)
*   **문서 수집:** 프로젝트의 `docs/` 폴더에 위치한 모든 최초 문서들(예: `docs/PRD.md`, `docs/TRD.md`, 요구사항 기술서 등)을 빠짐없이 읽어들입니다.
*   **초기 의도 파악:** 클라이언트 혹은 기획자가 의도한 서비스의 핵심 목적, 비즈니스 가치, 그리고 주요 기능 스코프를 분석합니다.

### 2단계: 문서 간 정합성 검증 및 교정 (Cross-Document Checking)
*   **불일치(Inconsistency) 탐지:** PRD(기획)에서는 명시했으나 TRD(기술 설계)에서는 반영되지 않았거나, 두 문서 간의 로직이 상충하는 부분(예: 데이터 흐름, 제약조건)을 면밀히 찾아냅니다.
*   **오류 및 빈틈 검수:** 잘못된 정책, 흐름상 단절된 부분, 또는 에러 발생 시 처리(Edge Case)가 누락된 시나리오 등을 도출합니다.

### 3단계: 문서 보강 및 상세화 작업 (Reinforcement & Detailing)
*   **직접 수정(Refining):** 앞서 2단계에서 발견된 문서 간 충돌, 누락점, 잘못된 부분을 논리적으로 보완하여 문서를 직접 고도화하고 편집합니다.
*   **Agent-Friendly 최적화 구조 재편:** 모호한 산문 형태의 줄글보다는, 후속 에이전트가 완벽히 파싱하고 이해할 수 있도록 **명확한 체크리스트, In/Out 데이터 형식, 유스케이스(Use-case) 시나리오, 필수 제약 조건 표(Constraints Table)** 등으로 문서를 구조화하고 상세하게 재정리합니다.
*   **검증 모드 제한:** `skill-pipeline-validation` 모드에서는 실제 제품 전체를 완성하기 위한 과도한 요구사항 확장보다, 후속 Agent가 구현/리뷰 계약을 오해 없이 이어받을 수 있는 수준의 정합성과 명확성을 우선합니다.

### 4단계: 세션 컨텍스트 초기화 및 선택적 스냅샷 기록 (Session Bootstrap) 🔗
파이프라인의 첫 번째 Agent이므로 `docs/generated/session-context.md`를 **반드시 생성**하여 세션을 시작합니다.
추가로 문서 교정 과정의 세부 판단을 자세히 남기고 싶다면 `docs/generated/context-snapshot.md`를 보조 기록으로 생성할 수 있습니다.

```markdown
## Session Context
- **pipeline_id:** [프로젝트 또는 실행 단위 식별자]
- **run_mode:** `project-delivery` | `skill-pipeline-validation`
- **current_stage:** `document-review`
- **review_cycle:** 0
- **session_id:** `doc-review-001`
- **parent_session_id:** `none`
- **previous_handoff:** `none`
- **in_scope:** [이번 실행 범위]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [문서 교정 핵심 판단 요약]
- **resolved_issues:** [없으면 "없음"]
- **latest_handoff:** `docs/generated/document-reviewer-handoff.md`
- **unresolved_issues:** [없으면 "없음"]
- **next_agent_focus:** [guide-generator가 집중해야 할 포인트]
- **evidence_paths:** [핵심 근거 문서 경로]
- **carry_forward_rules:** [`agent-session-contract.md` 기준]
```

```markdown
## Context Snapshot (Fork 컨텍스트 누적 기록)

### [1] Document Reviewer Agent 세션 기록
- **시간:** 2026-XX-XX
- **핵심 판단 및 결정 사유:**
  - [판단 1]: PRD의 'XX' 요건과 TRD의 'YY' 설계가 충돌 → ZZ로 통일. 이유: ...
  - [판단 2]: ...
- **수정된 문서와 변경 요약:**
  - `docs/PRD.md` — 3.2절 데이터 흐름도 전면 수정
  - `docs/TRD.md` — 4.1절 NDK 모듈 구조 상세화
- **다음 Agent에게 특별 전달:** [후속 Agent가 반드시 숙지해야 할 맥락]
```

### 5단계: 다음 파이프라인으로 제어권 위임 (Handoff)
*   모든 구조화 및 정합성 교정 작업이 완료되면 아래 **Handoff Manifest**를 작성하여 **'코드 품질 가이드 생성 에이전트'**에게 제어권을 넘깁니다.

## 📦 Handoff Manifest (가이드 생성 Agent로 인계 시 필수 포맷)
```markdown
## Handoff Manifest
- **작업 완료 Agent:** android-document-reviewer-agent
- **pipeline_id:** [값]
- **session_id:** [값]
- **parent_session_id:** `none`
- **실행 모드:** `project-delivery` | `skill-pipeline-validation`
- **review_cycle:** 0
- **session_context_path:** `docs/generated/session-context.md`
- **previous_handoff:** `none`
- **교정/수정 완료된 문서 목록:**
  - `docs/PRD.md` (변경 사항 요약)
  - `docs/TRD.md` (변경 사항 요약)
- **발견 및 교정한 불일치 항목 수:** N건
- **in_scope:** [이번 실행 범위]
- **out_of_scope:** [이번 실행에서 제외한 범위]
- **decision_summary:** [문서 교정 핵심 판단 요약]
- **evidence_paths:** [`docs/PRD.md`, `docs/TRD.md`]
- **컨텍스트 스냅샷 경로(선택):** `docs/generated/context-snapshot.md`
- **다음 Agent에게 전달할 핵심 컨텍스트:** [교정 완료 요약]
- **다음 Agent 필수 실행 항목:** [`docs/generated/session-context.md`와 본 handoff를 먼저 로드]
- **주의 사항 또는 미해결 이슈:** [내용]
```

## ⛑️ 에러 처리 (Error Handling)
*   `docs/` 폴더가 존재하지 않거나 PRD/TRD 파일이 하나도 없는 경우, 작업을 시작하지 않고 사용자에게 문서 준비를 요청합니다.
*   문서 간 충돌이 해소 불가능한 수준(비즈니스 요구와 기술 제약이 근본적으로 양립 불가)이라면, 양쪽 선택지를 정리하여 사용자에게 의사결정을 에스컬레이션합니다.
