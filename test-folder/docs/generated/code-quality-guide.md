# Code Quality Guide - TastePick

## 1. 문서 목적
이 문서는 `TastePick` 구현 및 리뷰 시 사용할 프로젝트 전용 체크리스트다.

## 2. 구현 체크리스트
### 2.1 문서 부합성
- `docs/PRD.md`의 필수 기능이 모두 코드에 반영되어야 한다.
- `docs/TRD.md`의 데이터 모델과 레이어 분리가 유지되어야 한다.
- `docs/generated/design-intent.md`의 방어 로직 기준이 누락되면 안 된다.

### 2.2 아키텍처
- MVP는 MVVM 기준으로 구현한다.
- ViewModel은 `UiState` 단일 출처를 외부에 노출해야 한다.
- Compose Screen에서 Repository/JNI를 직접 호출하면 안 된다.
- RecommendationUseCase 또는 Repository 내부에서 scorer를 호출해야 한다.

### 2.3 비동기 처리
- Room, DataStore, JSON 로딩은 `Dispatchers.IO`에서 수행한다.
- 추천 점수 계산은 `Dispatchers.Default` 또는 명시적 백그라운드 스레드에서 수행한다.
- 메인 스레드에서 JNI 호출을 직접 수행하면 안 된다.

### 2.4 JNI/NDK
- `JNIEnv*`를 캐싱하지 않는다.
- JNI 계층에서 C++ 예외를 안전하게 처리하고 Kotlin으로 실패 상태를 반환한다.
- Kotlin `Long` 포인터 핸들 수명주기가 명확해야 한다.
- Kotlin scorer와 NDK scorer의 입력/출력 계약이 같아야 한다.

### 2.5 데이터/도메인 규칙
- 알레르기/제외 재료 필터는 항상 가장 먼저 적용해야 한다.
- 추천 결과 0건 시 빈 상태 안내 UI를 제공해야 한다.
- 추천 이유는 결과 카드와 함께 반환되어야 한다.

### 2.6 Compose/UI
- 재사용 가능한 Composable은 `modifier: Modifier = Modifier`를 제공한다.
- ViewModel 상태는 Compose에서 단방향으로 소비한다.
- 피드백 버튼은 로딩/중복 클릭 상태를 고려해야 한다.

### 2.7 보안/배포
- API 키 하드코딩 금지
- cleartext traffic 비활성화
- JNI 호출 대상 keep rule 검토
- 불필요한 위험 권한 추가 금지

## 3. 테스트 체크리스트
- RecommendationUseCase 단위 테스트 존재
- 제외 재료 및 알레르기 필터 테스트 존재
- 추천 결과 0건 처리 테스트 존재
- ViewModel 상태 전이 테스트 존재
- Compose 결과 화면 렌더링 테스트 존재
- NDK scorer를 사용할 경우 Kotlin scorer와의 동등성 테스트 존재

## 4. 리뷰 등급 기준
- Critical:
  - 알레르기 필터 누락
  - 메인 스레드 JNI 호출
  - UI 계층의 Repository/JNI 직접 호출
- Warning:
  - 추천 이유 누락
  - ViewModel 상태 구조 분산
  - 테스트 누락
- Info:
  - 명명 규칙 불일치
  - Preview/Modifier 누락
