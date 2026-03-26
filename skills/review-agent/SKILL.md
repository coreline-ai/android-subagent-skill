---
name: android-review-agent
description: "[Android App Development] 리뷰 전담 Agent가 설계 의도 문서(design-intent.md)와 품질 가이드(code-quality-guide.md)를 통해 구현된 안드로이드 및 NDK 코드를 엄격히 리뷰하도록 안내하는 스킬입니다."
---

# Android Review Agent Skill (안드로이드 리뷰 전담 에이전트)

이 스킬은 리뷰 전담 에이전트(Review Agent)가 구현 전담 에이전트가 새롭게 작성한 소스 코드나 산출물을 `code-quality-guide.md`와 `design-intent.md`(비즈니스 요구사항)를 결합한 확고한 평가 기준점으로 엄격하게 교차 검증(Cross-validation)하도록 돕습니다.

## 🎯 목표 (Goal)
에이전트 개인의 주관적이고 범용적인 룰이 아닌, 프로젝트 고유의 공통 가이드인 `code-quality-guide.md`와 제품 설계 의도인 `design-intent.md`를 **절대적인 채점 기준표(Rubric)**로 삼아 코드를 리뷰하고 최종 병합(Approve) 여부를 결정하는 것.

## 🧭 운영 모드 (Operating Modes)
*   **`project-delivery` (기본):** 실제 제품 구현을 PRD/TRD 기준으로 완결성 있게 리뷰합니다.
*   **`skill-pipeline-validation`:** 스킬 자체의 handoff/계약/리뷰 체인을 검증하는 모드입니다. 이 경우 리뷰 목표는 **PRD 전체 완성 여부**가 아니라 아래를 확인하는 것입니다.
    *   입력/출력 경로 계약이 정상인가
    *   generated 문서와 구현 파일이 서로 모순되지 않는가
    *   대표 구현 경로가 실제로 빌드/테스트 가능한가
    *   구현 Agent가 선언한 미구현 범위가 정직하게 문서화되었는가
*   따라서 `skill-pipeline-validation` 모드에서는, Handoff Manifest에 명시된 검증 범위를 넘어선 제품 전체 미구현만으로는 자동 Reject 하지 않습니다.

## 🔁 파이프라인 위치 (Pipeline Position)
이 Agent는 표준 순서 `document-review -> guide-generator -> implementation -> review`의 **네 번째 단계**입니다.

*   **upstream:** `implementation`
*   **downstream:** 종료 또는 `implementation` 재진입 루프

## 🔗 공통 세션 전달 규약 (Shared Session Transfer Contract)
이 Agent는 리뷰 시작 전 `docs/generated/session-context.md`를 읽고, 리뷰 종료 후 동일 파일에 이번 판정을 append 해야 합니다.
세부 필드와 루프 원칙은 프로젝트 루트의 `agent-session-contract.md`를 기준으로 맞춥니다.

*   **필수 읽기:** `session-context.md`, 가장 최근 `handoff-manifest.md`, 필요 시 이전 `review-handoff-manifest.md`
*   **필수 쓰기:** `session_id`, `parent_session_id`, `review_cycle`, `previous_handoff`, 분류된 이슈 목록 요약, 다음 루프에서 반드시 해결해야 하는 이슈 목록, 증거 경로
*   **목적:** 다음 구현 Agent가 "왜 반려되었는지"를 대화 로그에 의존하지 않고 구조적으로 이어받게 합니다.

## 🧩 이슈 분류 체계 (Issue Classification Model)
리뷰 Agent는 발견한 모든 이슈를 아래 클래스 중 하나로 분류해야 합니다.

*   **`CONTEXT_BREAK`:** 경로 불일치, session 전달 누락, handoff 필드 부족, 실행 결과 증거 누락처럼 파이프라인을 끊는 문제. 항상 다음 루프의 필수 수정 대상.
*   **`SCOPE_BLOCKER`:** 현재 실행 모드와 현재 구현 범위 안에서 충족되어야 하는 결함. 현재 루프의 필수 수정 대상.
*   **`DECLARED_GAP`:** 구현 Agent가 명시적으로 out-of-scope 또는 미구현으로 선언한 항목. `skill-pipeline-validation` 모드에서는 기록만 하고 자동 Reject 사유로 사용하지 않습니다.
*   **`FOLLOW_UP`:** 품질 향상 또는 후속 작업 제안. 현재 루프의 필수 수정 대상은 아닙니다.

## 📋 프로세스 (Workflow)

리뷰 전담 에이전트는 다음 순서대로 리뷰를 진행해야 합니다.

### 1단계: 평가 기준 로드 및 선택적 컨텍스트 참고 (Standards Sync) 🔗
*   **필수 세션 컨텍스트 로드:** `docs/generated/session-context.md`에서 현재 `run_mode`, `review_cycle`, 직전 구현 범위, 미구현 선언 범위를 확인합니다.
*   **이전 handoff 추적:** `session-context.md`의 `previous_handoff`와 `latest_handoff`를 확인해 어느 루프에서 어떤 이유로 넘어왔는지 먼저 파악합니다.
*   **선택적 컨텍스트 참고:** `docs/generated/context-snapshot.md`가 존재하면 참고할 수 있지만, 필수 입력은 아닙니다.
*   **채점표 장착:** `docs/generated/code-quality-guide.md`, `docs/generated/design-intent.md`(필요시 `docs/PRD.md`, `docs/TRD.md` 포함)를 읽어들여 명시적 룰을 컨텍스트에 셋업합니다.
*   **Handoff Manifest 파싱:** 구현 Agent가 `docs/generated/handoff-manifest.md`에 남긴 **실행 모드**, 변경 파일 목록, 테스트 결과, 미해결 이슈를 파악하여 리뷰 범위를 확정합니다.

### 2단계: 코드 품질 크로스 밸리데이션 (Cross-Validation Review)
가이드라인의 각 조항을 체크리스트로 삼아 아래 요건들을 중점적으로 리뷰합니다.
*   **비즈니스/설계 제약(Design Compliance):** 작성된 로직이 `docs/generated/design-intent.md`에서 의도한 목적(에지 케이스, 시스템 제약)을 빈틈없이 100% 충족하는가?
*   **품질 컨벤션 위반(Quality Guide Breach):** `docs/generated/code-quality-guide.md`에 명시된 주요 아키텍처 원칙(채택된 상태관리 패턴 위반, Coroutine Dispatcher 오용, NDK 환경에서의 메모리 해제 누락 등)을 어긴 곳이 존재하는가?
*   위반 사항을 발견할 경우 "어떤 파일의 몇 번째 줄이, `docs/generated/code-quality-guide.md` 항목 중 어떤 룰을 구체적으로 위반했는지" 명료하게 근거를 제시하여 피드백합니다.
*   **검증 모드 판정 규칙:** `skill-pipeline-validation` 모드에서는 "대표 구현 경로가 handoff 검증 목적에 충분한가"를 우선 판정합니다. 이때 Handoff Manifest에 **의도적으로 미구현한 범위**가 명시되어 있다면, 그 범위를 이유로 자동 Reject 하지 않습니다.
*   **이슈 분류 필수:** 모든 리뷰 이슈는 `CONTEXT_BREAK`, `SCOPE_BLOCKER`, `DECLARED_GAP`, `FOLLOW_UP` 중 하나로 분류해 출력합니다.

### 3단계: 테스트 검증 (Test Verification) ⚠️
*   Handoff Manifest에 기재된 **테스트 실행 결과**를 확인합니다. 실패한 테스트가 1건이라도 있으면 Approve 불가.
*   **테스트 커버리지 기준:** 비즈니스 로직 80% 이상, NDK 코드는 JNI 브릿지 경계 함수 100% 커버 필수.
*   테스트 코드 자체의 품질도 리뷰합니다: 의미 없는 assert, 하드코딩된 테스트 데이터, 비결정적(flaky) 테스트는 지적 대상.
*   **검증 모드 예외:** `skill-pipeline-validation` 모드에서는 전체 커버리지보다 "최소 1개의 의미 있는 테스트 또는 빌드 경로가 실제로 성공했는지"를 우선 확인합니다.

### 4단계: 안드로이드 보안 및 배포 검증 (Security & Release Checklist) 🔒
다음 안드로이드 플랫폼 고유 보안/배포 항목을 **필수 체크**합니다.
*   **API Key / Secret 하드코딩 금지:** 소스 코드 내 API 키, 토큰, 비밀번호가 평문으로 노출되어 있지 않은지 확인. `BuildConfig`, `local.properties`, 또는 암호화된 저장소를 통해 관리되어야 함.
*   **네트워크 보안 설정:** `network_security_config.xml`이 올바르게 구성되어 있는지, cleartext 트래픽 허용이 프로덕션에서 차단되어 있는지 확인.
*   **ProGuard/R8 난독화:** JNI에서 사용되는 클래스/메서드에 대해 `-keep` 규칙이 `proguard-rules.pro`에 포함되어 있는지 확인. 누락 시 릴리즈 빌드에서 JNI 크래시 발생.
*   **AndroidManifest 권한:** 불필요한 위험 권한(CAMERA, LOCATION 등)이 선언되지 않았는지, 런타임 퍼미션 요청 로직이 올바르게 구현되었는지 확인.
*   **minSdkVersion / targetSdkVersion:** TRD에 명시된 SDK 버전 정책을 준수하는지 확인.
*   **검증 모드 제한:** `skill-pipeline-validation` 모드에서는 릴리즈 전수 검증보다, 현재 구현된 범위 안의 명백한 보안/배포 blocker가 있는지 중심으로 확인합니다.

### 5단계: 리뷰 결과 통보 및 피드백 순환 (Feedback & Approval)
*   발견된 문제점과 수정 권장 사항을 포함한 리뷰 리포트를 마크다운으로 작성합니다.
*   `docs/generated/session-context.md`에 아래 형태로 이번 판정을 append 하여 다음 구현 Agent가 동일 세션 체인을 이어받게 합니다.

```markdown
### Session Update - Review
- **current_stage:** `review`
- **session_id:** `review-00N`
- **parent_session_id:** [직전 implementation session_id]
- **review_cycle:** [현재 루프 번호]
- **previous_handoff:** `docs/generated/handoff-manifest.md`
- **decision_summary:** [이번 리뷰 판정과 핵심 이유]
- **resolved_issues:** [없으면 "없음"]
- **unresolved_issues:**
  - `CONTEXT_BREAK`: [없으면 "없음"]
  - `SCOPE_BLOCKER`: [없으면 "없음"]
  - `DECLARED_GAP`: [없으면 "없음"]
  - `FOLLOW_UP`: [없으면 "없음"]
- **latest_handoff:** `docs/generated/review-handoff-manifest.md`
- **next_agent_focus:** [다음 구현 루프가 우선 해결할 항목]
- **evidence_paths:** [리뷰 근거 파일, 테스트 리포트, 로그 경로]
- **carry_forward_rules:** [`CONTEXT_BREAK`와 `SCOPE_BLOCKER`만 다음 루프 필수 수정 대상으로 유지]
```

*   만약 코드가 가이드상의 **모든 체크리스트 조항을 무사히 통과**했다면, 리뷰 승인(Approve) 문구와 함께 검증 완료 리포트를 출력하여 전체 개발 파이프라인을 종료합니다.
*   `Rejected` 판정은 `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 하나 이상 남아 있을 때만 사용합니다.
*   `DECLARED_GAP`와 `FOLLOW_UP`만 남아 있는 경우, `skill-pipeline-validation` 모드에서는 Reject 대신 `DONE_WITH_CONCERNS` 또는 Approve-with-concerns 성격의 결과를 사용할 수 있습니다.

## 🔄 피드백 루프 규칙 (Feedback Loop Policy)
리뷰 결과가 **Reject(반려)**인 경우, 구현 Agent에게 수정을 요청하고 재리뷰를 진행합니다.
*   **최대 반복 횟수: 3회.** 구현 Agent ↔ 리뷰 Agent 간 수정-재리뷰 사이클이 **3회를 초과**할 경우, 더 이상 자동 순환하지 않고 **사용자(Human)에게 즉시 에스컬레이션**합니다.
*   각 반복 시 `CONTEXT_BREAK`와 `SCOPE_BLOCKER`의 미해결 항목을 우선 재검증합니다.
*   `DECLARED_GAP`와 `FOLLOW_UP`는 다음 루프의 필수 수정 목록에 자동 승격하지 않습니다. 단, `project-delivery` 모드에서 in-scope 항목과 충돌하면 `SCOPE_BLOCKER`로 재분류할 수 있습니다.
*   3회차 리뷰에서도 `CONTEXT_BREAK` 또는 `SCOPE_BLOCKER`가 남아있다면 근본 원인 분석(Root Cause)을 포함한 에스컬레이션 리포트를 사용자에게 제출합니다.

## 📦 Handoff Manifest (완료 시 출력 포맷)
```markdown
## Review Handoff Manifest
- **작업 완료 Agent:** android-review-agent
- **pipeline_id:** [값]
- **session_id:** [값]
- **parent_session_id:** [이전 session_id]
- **실행 모드:** `project-delivery` | `skill-pipeline-validation`
- **review_cycle:** [현재 루프 번호]
- **session_context_path:** `docs/generated/session-context.md`
- **previous_handoff:** `docs/generated/handoff-manifest.md`
- **리뷰 결과:** Approved / Rejected (N차 리뷰)
- **검증된 파일 목록:** [파일 경로 리스트]
- **발견된 이슈 수:** Critical: N / Warning: N / Info: N
- **decision_summary:** [이번 리뷰 판정 핵심 이유]
- **이슈 분류 요약:**
  - `CONTEXT_BREAK`: N건
  - `SCOPE_BLOCKER`: N건
  - `DECLARED_GAP`: N건
  - `FOLLOW_UP`: N건
- **다음 루프 필수 수정 항목:** [`CONTEXT_BREAK` + `SCOPE_BLOCKER` 목록]
- **evidence_paths:** [리뷰 근거 파일, 테스트 리포트, 로그 경로]
- **테스트 커버리지 확인:** Pass / Fail
- **보안 체크리스트 확인:** Pass / Fail
- **미해결 이슈:** [내용 또는 "없음"]
```

## ⛑️ 에러 처리 (Error Handling)
*   Handoff Manifest가 존재하지 않거나 필수 필드가 누락된 경우, 구현 Agent에게 Manifest 재생성을 요청합니다.
*   `docs/generated/code-quality-guide.md` 또는 `docs/generated/design-intent.md`가 프로젝트에 존재하지 않으면, 리뷰를 시작하지 않고 사용자에게 누락 사실을 즉시 알립니다.
