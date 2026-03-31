# AGENT.md

이 문서는 본 프로젝트의 에이전트 파이프라인 아키텍처, 동작 원리, 확장 지침을 상세히 기술합니다.

## 1. 프로젝트 목적

**android-subagent-skill**은 안드로이드 앱 개발의 **구현 파이프라인**을 에이전트 체인으로 자동화하는 Claude Code 전용 스킬셋입니다.

### 해결하는 문제

대부분의 에이전트 워크플로는 다음 에이전트가 이전 에이전트의 컨텍스트를 복원하지 못할 때 실패합니다. 본 프로젝트는 이를 **명시적 계약(explicit contract)**으로 해결합니다:

- 모든 단계는 정규화된 핸드오프 매니페스트를 작성
- 전체 에이전트가 공유하는 `session-context.md`에 결정 사항을 누적
- 단일 오케스트레이터가 디스패치 타이밍과 리뷰 루프를 제어
- 검증 모드(skill-pipeline-validation)로 실제 프로젝트 투입 전에 스킬 체인이 정상 동작함을 증명

### 스코프 경계

| 포함 | 미포함 |
|---|---|
| PRD/TRD 정합성 검증 | PRD/TRD 작성 (사용자 책임) |
| 설계 의도 및 코드 품질 가이드 생성 | Gradle 빌드 설정 |
| Clean Architecture 기반 코드 구현 | APK 패키징 및 서명 |
| 이슈 분류 기반 코드 리뷰 | CI/CD 파이프라인 |
| 계약 정합성 harness 검증 | Play Store 배포 |

## 2. 입력물: PRD와 TRD

### 사용자 책임

PRD(Product Requirements Document)와 TRD(Technical Requirements Document)는 파이프라인의 **전제 조건**입니다.

- 사용자가 **브레인스토밍**을 통해 구체적으로 작성해야 함
- 앱의 기능 요구사항(PRD), 기술 설계(TRD)를 명확히 정의
- 파이프라인은 이 문서의 내용을 리서치하거나 발굴하지 않음

### 파이프라인의 역할

document-review 단계는 PRD/TRD를 **교정하고 고도화**합니다. 새로운 요구사항을 리서치하거나 기능을 추가하지는 않습니다:

- 안드로이드 생태계 정합성 검증 (API 레벨, 권한 전략, 모듈 구조)
- PRD ↔ TRD 간 불일치 탐지 및 교정 (누락된 컴포넌트, 모델, 테스트 파일)
- 논리적 모순, 누락점, 모호한 표현을 찾아 교정
- 후속 에이전트가 오해 없이 수행할 수 있도록 문서를 **상세화 및 AI 친화적 구조로 재편** (체크리스트, 유스케이스, 제약조건표 등)
- `skill-pipeline-validation` 모드에서는 과도한 요구사항 확장보다 정합성과 명확성을 우선

### 사전 검증

`.claude/hooks/check-pipeline-docs.sh` Hook이 `docs/PRD.md`와 `docs/TRD.md` 존재 여부를 확인합니다. 파일이 없으면 스킬 실행이 차단됩니다(exit code 2).

## 3. 파이프라인 아키텍처

### 5단계 워크플로

```
[사용자] ─── PRD/TRD 작성 ───→ [docs/PRD.md, docs/TRD.md]
                                         │
                                         ▼
                            ┌─── pipeline-orchestrator (inline) ───┐
                            │         디스패치 제어, 루프 관리          │
                            └──────────────┬───────────────────────┘
                                           │
                    ┌──────────────────────┼──────────────────────┐
                    ▼                      ▼                      │
          document-review (fork)  guide-generation (fork)         │
          PRD/TRD 정합성 교정      설계 의도 + 품질 가이드           │
                                                                  │
                    ┌─────────────────────────────────────────────┘
                    ▼
          implementation (inline) ←──── REJECTED (최대 3회)
          코드 + 테스트 구현               │
                    │                      │
                    ▼                      │
              review (inline) ─────────────┘
              이슈 분류 + 승인/반려
                    │
                    ▼
          APPROVED / DONE_WITH_CONCERNS → 파이프라인 종료
```

### 실행 모드 (Claude Code 전용)

각 스킬의 SKILL.md frontmatter에 `context: fork` 또는 `context: inline`이 선언됩니다.

| 모드 | 동작 | 컨텍스트 접근 |
|---|---|---|
| `fork` | Claude Code Agent tool로 격리된 서브에이전트 실행 | 핸드오프 파일만 접근. 대화 히스토리 없음. 결과는 summary로 반환. |
| `inline` | 메인 대화 컨텍스트에서 실행 | 이전 에이전트의 추론, 코드 결정에 완전 접근. |

**왜 이렇게 나눴는가:**

- **초기 단계** (document-review, guide-generation): 입력 문서만 있으면 되므로 fork로 컨텍스트 윈도우 절약
- **후기 단계** (implementation, review): 구현 추론을 리뷰가 직접 볼 수 있어야 리뷰 루프가 수렴
- **오케스트레이터**: Claude Code의 서브에이전트는 다른 서브에이전트를 생성할 수 없으므로 반드시 inline

## 4. 에이전트별 상세

### 4.1 pipeline-orchestrator-agent

- **실행**: inline
- **역할**: 파이프라인 진입점. 디스패치 결정, 리뷰 루프 제어, 세션 중단 시 복구.
- **입력**: `docs/PRD.md`, `docs/TRD.md`, `session-context.md`, 최신 핸드오프
- **산출**: `orchestrator-handoff.md`
- **디스패치 규칙**:
  - 최초 실행 → document-review
  - document-review 완료 → guide-generation
  - guide-generation 완료 → implementation
  - implementation 완료 → review
  - APPROVED / DONE_WITH_CONCERNS → stop
  - REJECTED + cycle < 3 → implementation 재진입
  - REJECTED + cycle ≥ 3 → 사용자 에스컬레이션
- **post-fork 검증**: fork 에이전트 완료 후 핸드오프 필수 키 존재 확인. 누락 시 보정.

### 4.2 document-reviewer-agent

- **실행**: fork
- **역할**: PRD/TRD 정합성 검증 및 교정. 내용 리서치는 하지 않음.
- **입력**: `orchestrator-handoff.md`, `docs/PRD.md`, `docs/TRD.md`
- **산출**: `document-reviewer-handoff.md`, `context-snapshot.md`, session-context 첫 번째 워커 섹션
- **검증 항목**:
  - PRD ↔ TRD 기능 매핑 일치
  - TRD 모듈 트리에서 누락된 컴포넌트/테스트
  - 데이터 모델 정의 완전성
  - 안드로이드 API 레벨별 권한 전략 정확성
- **핸드오프 필수 키**: 18개 (공통 13 + `updated_documents`, `corrected_inconsistency_count`, `context_snapshot_path`, `next_agent_context`, `next_agent_required_actions`)

### 4.3 code-quality-guide-generator

- **실행**: fork
- **역할**: 교정된 PRD/TRD로부터 설계 의도와 코드 품질 기준을 생성
- **입력**: `document-reviewer-handoff.md`, `session-context.md`, `docs/PRD.md`, `docs/TRD.md`, `adr.md`, `code-convention.md`
- **산출**: `design-intent.md`, `code-quality-guide.md`, `guide-generator-handoff.md`
- **설계 결정**:
  - ADR 상태가 Accepted인 항목만 가이드에 반영 (Proposed는 제외)
  - 품질 가이드를 Pipeline Essential / Production 2단계로 구분
  - Pipeline Essential: 파이프라인 검증 시 반드시 충족해야 할 항목
  - Production: 제품 출시 시 추가로 필요한 항목
- **핸드오프 필수 키**: 16개 (공통 13 + `generated_artifacts`, `next_agent_context`, `next_agent_required_actions`)

### 4.4 implementation-agent

- **실행**: inline
- **역할**: design-intent.md와 code-quality-guide.md를 기반으로 코드와 테스트를 구현
- **입력**: `guide-generator-handoff.md` (초기) 또는 `review-handoff-manifest.md` (재진입), `session-context.md`, `design-intent.md`, `code-quality-guide.md`, `adr.md`, `code-convention.md`
- **산출**: `handoff-manifest.md`, 소스 코드, 테스트 코드
- **구현 원칙**:
  - Clean Architecture (domain/data/ui 계층 분리)
  - MVVM + StateFlow (ADR-003 MVI는 Proposed이므로 TRD에서 명시 채택한 경우만)
  - viewModelScope + Dispatchers.IO 비동기 패턴
  - 테스트 필수 (비즈니스 로직 80%+ 커버리지 목표)
  - `skill-pipeline-validation` 모드에서는 대표 경로(representative path)만 구현
- **재진입 시**: CONTEXT_BREAK와 SCOPE_BLOCKER 이슈만 수정 대상
- **핸드오프 필수 키**: 21개 (공통 13 + `implemented_scope`, `declared_gaps`, `changed_files`, `test_results`, `test_coverage`, `resolved_issue_counts`, `next_agent_context`, `next_agent_required_actions`)

### 4.5 review-agent

- **실행**: inline
- **역할**: design-intent.md와 code-quality-guide.md를 채점 기준으로 코드 리뷰 수행
- **입력**: `handoff-manifest.md`, `session-context.md`, `design-intent.md`, `code-quality-guide.md`
- **산출**: `review-handoff-manifest.md`, `review-report.md`
- **이슈 분류 체계**:

| 분류 | 의미 | 승인 차단? |
|---|---|:---:|
| `CONTEXT_BREAK` | 계약/문맥 자체가 깨진 경우 (필수 키 누락, 세션 경로 불일치) | Yes |
| `SCOPE_BLOCKER` | 필수 요구사항 미충족 (대표 경로 빌드 불가, 핵심 테스트 실패) | Yes |
| `DECLARED_GAP` | 구현 에이전트가 declared_gaps에 명시적으로 선언한 미구현 항목 | No |
| `FOLLOW_UP` | 참고 사항 (deprecation 경고, 버전 권고) | No |

- **판정 결과**:
  - `APPROVED`: 모든 필수 항목 충족
  - `DONE_WITH_CONCERNS`: CONTEXT_BREAK=0, SCOPE_BLOCKER=0이지만 DECLARED_GAP 또는 FOLLOW_UP 존재
  - `REJECTED`: CONTEXT_BREAK > 0 또는 SCOPE_BLOCKER > 0 → implementation 재진입 유발
- **핸드오프 필수 키**: 20개 (공통 13 + `review_result`, `verified_files`, `issue_counts`, `issue_classification_counts`, `next_agent_required_actions`, `test_coverage_status`, `security_checklist_status`)

## 5. 세션 계약 (Session Contract)

### session-context.md

모든 에이전트가 `docs/generated/session-context.md`에 자신의 결정을 **append** 합니다. 절대 overwrite하지 않습니다.

각 섹션은 반드시 `## Session Update - {스테이지명}` 형식(h2)을 사용해야 합니다.

**필수 키 (16개):**

```
pipeline_id, run_mode, current_stage, review_cycle,
session_id, parent_session_id, previous_handoff, latest_handoff,
in_scope, out_of_scope, decision_summary,
resolved_issues, unresolved_issues, next_agent_focus,
evidence_paths, carry_forward_rules
```

### 핸드오프 매니페스트

각 에이전트가 자신의 핸드오프 파일에 작성하는 구조화된 마크다운입니다.

**공통 필수 키 (13개):**

```
pipeline_id, session_id, parent_session_id, run_mode, review_cycle,
session_context_path, previous_handoff,
in_scope, out_of_scope, decision_summary,
evidence_paths, unresolved_issues, completed_agent
```

각 스테이지는 추가 필수 키를 가집니다 (4.1~4.5 참조).

### evidence_paths 형식

harness parser가 올바르게 파싱하려면 **서브 불릿 리스트** 형식을 사용해야 합니다:

```markdown
- **evidence_paths:**
  - `docs/generated/design-intent.md`
  - `docs/generated/code-quality-guide.md`
```

인라인 형식(`[a, b]`)은 단일 문자열로 파싱되어 검증 실패를 유발합니다.

## 6. 리뷰 루프

```
implementation → review → REJECTED → orchestrator → implementation → review → ...
                                                     (review_cycle 증가)
```

- 최대 3회 자동 루프 (`MAX_REVIEW_CYCLES = 3`)
- 2회차 이상에서는 `next_agent_required_actions`에 명시된 항목만 필수 수정 대상
- 3회차 이후에도 CONTEXT_BREAK 또는 SCOPE_BLOCKER가 남으면 사용자 에스컬레이션

## 7. 검증 harness

### 실행

```bash
python3 -m harness.run_validation --project <디렉토리>
python3 -m harness.run_validation --project <디렉토리> --check-build-evidence
```

### 검증 범위

- 매니페스트 파일 존재 및 필수 키 검증
- completed_agent ↔ 레지스트리 매칭
- pipeline_id / run_mode 전체 일관성
- worker stage 순서 검증 (base + 리뷰 루프 반복 허용)
- 첫/마지막 세션이 pipeline-orchestrator인지 확인
- 각 스테이지의 latest_handoff 경로 검증
- evidence 파일 존재 여부 (내용 검증은 non-goal)
- 오케스트레이터 디스패치 로직 (terminal → stop, rejected → implementation)
- DONE_WITH_CONCERNS 전제조건 (CONTEXT_BREAK=0, SCOPE_BLOCKER=0)

### 네이밍 레지스트리 (3-way)

| Stage | Skill 폴더 | Agent Name | 실행 모드 |
|---|---|---|:---:|
| `pipeline-orchestrator` | `pipeline-orchestrator-agent` | `android-pipeline-orchestrator-agent` | inline |
| `document-review` | `document-reviewer-agent` | `android-document-reviewer-agent` | fork |
| `guide-generation` | `code-quality-guide-generator` | `android-code-quality-guide-generator` | fork |
| `implementation` | `implementation-agent` | `android-implementation-agent` | inline |
| `review` | `review-agent` | `android-review-agent` | inline |

## 8. 레퍼런스 템플릿

파이프라인은 두 개의 레퍼런스 문서를 기반으로 코드 품질 가이드를 생성합니다:

- **`adr.md`** — Architecture Decision Records. ADR-001(JNI 브릿지), ADR-002(Coroutines), ADR-003(MVI, Proposed). `Accepted` 상태만 가이드에 반영.
- **`code-convention.md`** — Kotlin/Compose UI/NDK C++ 코딩 컨벤션, 보안 규칙, 커밋 메시지 규약.

이 파일들은 스킬 프레임워크의 일부이며, 프로젝트별로 커스터마이즈할 수 있습니다.

## 9. 플랫폼 제약

- 본 파이프라인은 **Claude Code 전용**입니다
- `context: fork`는 Claude Code의 Agent tool로만 실행 가능
- PreToolUse Hook은 Claude Code의 hooks 인프라에서만 동작
- 다른 LLM 플랫폼에서 사용하려면 fork/inline 실행 모드와 Hook을 해당 플랫폼의 동등한 메커니즘으로 대체해야 함
- SKILL.md의 핸드오프 키 스키마와 session-context 형식은 플랫폼 무관하게 재사용 가능 (V1 범용 계층)
