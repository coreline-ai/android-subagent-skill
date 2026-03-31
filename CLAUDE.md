# CLAUDE.md

이 파일은 Claude Code가 본 프로젝트를 다룰 때 참조하는 프로젝트 컨텍스트 문서입니다.

## 프로젝트 정체성

**android-subagent-skill**은 Claude Code 전용 스킬 기반 에이전트 파이프라인입니다.
PRD/TRD 문서로부터 안드로이드 앱의 **설계 → 구현 → 리뷰**까지를 자동화하는 5단계 워크플로를 정의합니다.

## 핵심 설계 철학

### 구현 집중 파이프라인

본 프로젝트는 **코드 구현**에 집중합니다. 빌드(Gradle)와 배포(CI/CD)는 스코프 밖입니다.

- 파이프라인의 목적: PRD/TRD → 설계 의도 도출 → 코드 품질 가이드 생성 → 구현 → 리뷰
- Gradle 빌드 설정, APK 패키징, Play Store 배포는 다루지 않음
- 검증 harness도 계약(contract) 정합성만 확인하며, 빌드 성공 여부는 검증하지 않음

### PRD/TRD는 사용자가 직접 작성

PRD(Product Requirements Document)와 TRD(Technical Requirements Document)는 파이프라인의 **입력물**이지, 파이프라인이 생성하는 산출물이 아닙니다.

- 사용자가 브레인스토밍을 통해 구체적으로 작성해야 함
- 파이프라인의 document-review 단계는 PRD/TRD의 정합성 검증/교정에 더해, 후속 에이전트가 오해 없이 지시를 수행할 수 있도록 문서를 **고도화/상세화/AI 친화적 구조로 재편**함
- 논리적 모순, 누락점, 모호한 표현을 교정하고, 체크리스트/유스케이스/제약조건표 등으로 구조화
- 단, 새로운 기능을 추가하거나 요구사항을 리서치하지는 않음 (특히 `skill-pipeline-validation` 모드에서는 스코프 확장 금지)
- PRD/TRD가 없으면 PreToolUse Hook이 파이프라인 시작을 차단함

### 에이전트 간 계약 기반 통신

- 에이전트는 대화 히스토리에 의존하지 않음
- 모든 컨텍스트는 마크다운 핸드오프 파일과 session-context.md를 통해 전달
- 각 핸드오프에는 필수 키가 정의되어 있으며, 누락 시 harness 검증 실패

## 파이프라인 구조

```
orchestrator (inline)
  ├── document-review (fork) ── PRD/TRD 정합성 검증/교정
  ├── guide-generation (fork) ── 설계 의도 + 코드 품질 가이드 생성
  ├── implementation (inline) ── 코드 + 테스트 구현
  └── review (inline) ── 이슈 분류 및 승인/반려 결정
        ├── APPROVED / DONE_WITH_CONCERNS → 종료
        └── REJECTED → implementation 재진입 (최대 3회)
```

- `fork`: Claude Code Agent tool로 격리 실행. 대화 히스토리 없음. 핸드오프 파일만 접근.
- `inline`: 메인 대화에서 실행. 이전 에이전트의 추론과 코드 결정에 접근 가능.

## 실행 모드

| 모드 | 목적 |
|---|---|
| `project-delivery` | 실제 제품 구현. PRD/TRD 완결성 기준 리뷰. |
| `skill-pipeline-validation` | 파이프라인 계약 검증. 대표 경로(representative path) 구현으로 증거 확보. 전체 PRD 미구현이 자동 실패 사유가 되지 않음. |

## 주요 파일

| 파일 | 역할 |
|---|---|
| `skills/pipeline-orchestrator-agent/SKILL.md` | 오케스트레이터 스킬 정의 (파이프라인 진입점) |
| `skills/pipeline-orchestrator-agent/agent-session-contract.md` | 전 에이전트 공유 계약 (필수 키, 리뷰 루프 규칙, 실행 모드) |
| `skills/document-reviewer-agent/SKILL.md` | 문서 리뷰어 스킬 (fork) |
| `skills/code-quality-guide-generator/SKILL.md` | 가이드 생성기 스킬 (fork) |
| `skills/code-quality-guide-generator/adr.md` | ADR 레퍼런스 템플릿 |
| `skills/code-quality-guide-generator/code-convention.md` | 코딩 컨벤션 레퍼런스 |
| `skills/implementation-agent/SKILL.md` | 구현 에이전트 스킬 (inline) |
| `skills/review-agent/SKILL.md` | 리뷰 에이전트 스킬 (inline) |
| `harness/` | 계약 검증 harness (Python) |
| `.claude/hooks/check-pipeline-docs.sh` | PRD/TRD 사전 검증 Hook |

## 산출물 (docs/generated/)

파이프라인 실행 시 모든 산출물은 대상 프로젝트의 `docs/generated/` 디렉토리에 생성됩니다:

- `orchestrator-handoff.md` — 오케스트레이터 디스패치 결정
- `session-context.md` — 전체 파이프라인 세션 누적 기록
- `document-reviewer-handoff.md` — 문서 리뷰 결과
- `context-snapshot.md` — 문서 스냅샷 (선택)
- `design-intent.md` — 설계 의도
- `code-quality-guide.md` — 코드 품질 가이드 (리뷰 기준)
- `guide-generator-handoff.md` — 가이드 생성 핸드오프
- `handoff-manifest.md` — 구현 핸드오프
- `review-report.md` — 리뷰 상세 보고서
- `review-handoff-manifest.md` — 리뷰 핸드오프

## 검증

```bash
python3 -m harness.run_validation --project <프로젝트 디렉토리>
```

harness는 계약 정합성만 검증합니다:
- 필수 키 존재 여부
- pipeline_id / run_mode 일관성
- worker stage 순서 (리뷰 루프 최대 3회 허용)
- completed_agent 레지스트리 매칭
- evidence 파일 존재 여부 (내용은 검증하지 않음)

## 주의사항

- 이 프로젝트 자체는 스킬 프레임워크이며, 앱 코드를 포함하지 않음
- `test-folder*` 디렉토리는 검증용 fixture이며 `.gitignore`에 등록됨
- `fork` / `inline` 실행 모드는 Claude Code의 Agent tool에서만 동작하는 개념
- SKILL.md 템플릿의 필수 키와 섹션 헤딩 형식은 harness parser와 정확히 일치해야 함
  - 세션 섹션: `## Session Update - {스테이지명}` (h2 필수)
  - evidence_paths: 서브 불릿 리스트 형식 필수 (인라인 `[a, b]` 불가)
