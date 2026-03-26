# TRD - TastePick

## 1. 문서 목적
이 문서는 `TastePick` MVP를 구현하기 위한 Android 기술 설계 기준을 정의한다.
PRD의 요구사항을 구현 가능한 구조로 해석하고, Agent가 후속 산출물을 만들 수 있도록 입력/출력과 제약을 명확히 한다.

## 2. 기술 방향
- Android UI는 Jetpack Compose로 구현한다.
- 상태 관리는 MVVM을 기본으로 사용한다.
- 추천 점수 계산은 추후 확장성을 고려해 별도 추천 엔진 계층으로 분리한다.
- 추천 엔진은 Kotlin 구현을 기본으로 시작하되, 점수 계산 로직을 NDK C++ 모듈로 교체 가능하도록 인터페이스를 고정한다.
- 네트워크 의존 없이 로컬 데이터셋 기반으로 동작한다.

## 3. 아키텍처
### 3.1 레이어 구조
- UI Layer
  - Compose Screen
  - ViewModel
- Domain Layer
  - UseCase
  - RecommendationPolicy
- Data Layer
  - Repository
  - Local DataSource
  - Preference DataSource
- Native Layer
  - RecommendationScorer JNI Wrapper
  - C++ scoring engine

### 3.2 상태 관리 원칙
- MVP 단계에서는 MVVM을 사용한다.
- 각 화면은 단일 `UiState` 데이터 클래스를 노출한다.
- ViewModel은 `StateFlow`를 통해 상태를 발행한다.
- 이벤트는 함수 호출 또는 `SharedFlow`로 처리한다.
- MVI는 본 MVP에 기본 적용하지 않는다.

## 4. 주요 모듈 설계
### 4.1 Onboarding
- 입력: 선호 카테고리, 제외 재료, 알레르기 목록
- 저장: DataStore + Room
- 출력: 온보딩 완료 상태

### 4.2 Recommendation
- 입력:
  - 사용자 선호도
  - 현재 식사 조건
  - 메뉴 데이터셋
  - 사용자 피드백 이력
- 처리 순서:
  1. 제외 재료 및 알레르기 필터링
  2. 식사 시간 태그 필터링
  3. 기분/예산 조건 반영
  4. 선호 카테고리 가중치 적용
  5. 피드백 이력 기반 가산/감산
  6. 상위 3개 선택
- 출력:
  - 추천 결과 3개
  - 각 결과별 추천 이유 문자열

### 4.3 Settings
- 선호도 수정
- 데이터 초기화
- 앱 버전 및 데이터셋 버전 표시

## 5. 데이터 모델
### 5.1 MenuEntity
- `id: String`
- `name: String`
- `category: String`
- `mealTimeTags: List<String>`
- `moodTags: List<String>`
- `priceTier: String`
- `ingredients: List<String>`
- `spicyLevel: Int`

### 5.2 UserPreference
- `preferredCategories: List<String>`
- `excludedIngredients: List<String>`
- `allergies: List<String>`
- `spicyTolerance: Int`

### 5.3 RecommendationFeedback
- `menuId: String`
- `liked: Boolean`
- `timestamp: Long`

## 6. 화면별 입력/출력 계약
| 화면 | 입력 | 출력 |
| --- | --- | --- |
| Onboarding | 사용자 선호도 입력 | 저장 성공 여부, 홈 이동 |
| Home | 현재 식사 조건 | 추천 요청 이벤트 |
| Result | 추천 결과 목록 | 피드백 저장 이벤트 |
| Settings | 기존 선호도 | 수정/초기화 결과 |

## 7. 비동기 및 스레딩
- Room/파일 I/O는 `Dispatchers.IO`에서 수행한다.
- 추천 계산은 `Dispatchers.Default` 또는 JNI 호출용 백그라운드 스레드에서 수행한다.
- 메인 스레드에서 JNI 호출을 직접 수행하지 않는다.

## 8. NDK 적용 범위
- `RecommendationScorer` 인터페이스를 기준으로 Kotlin 구현과 Native 구현을 교체 가능하게 한다.
- 초기 MVP는 Kotlin 점수 계산 구현으로 시작할 수 있다.
- Native 구현을 사용할 경우 다음 규칙을 따른다.
  - JNI Wrapper는 Kotlin Repository 또는 UseCase 내부에서만 호출
  - C++ 포인터는 Kotlin `Long`으로 보관
  - 예외는 JNI 계층에서 에러 코드 또는 실패 Result로 변환

## 9. 예외 처리
- 필터링 결과가 0건이면 랜덤 추천이 아니라 명시적 빈 결과 상태를 반환한다.
- 빈 결과 상태에서는 조건 완화 안내 UI를 제공한다.
- JSON seed 실패 시 앱은 치명 오류 대신 재시도/초기화 안내를 제공한다.

## 10. 테스트 전략
- ViewModel 단위 테스트
- RecommendationUseCase 단위 테스트
- 제외 재료 및 알레르기 필터 테스트
- RecommendationScorer Kotlin/NDK 구현 동등성 테스트
- Compose 화면 렌더링 및 피드백 버튼 테스트

## 11. 빌드/배포 제약
- minSdkVersion 29
- targetSdkVersion 최신 안정 버전 기준
- 릴리즈 빌드에서 cleartext traffic 비허용
- JNI 함수가 추가되면 R8 keep rule 검토 필요

## 12. 문서 간 정합성 체크 결과
- PRD의 "오프라인 추천" 요구와 기술 설계의 로컬 데이터셋 전략이 일치함
- PRD의 "추천 이유 표시" 요구에 맞춰 TRD에서 규칙 기반 reason generation을 포함함
- PRD의 "알레르기 보호" 요구에 맞춰 TRD에서 추천 이전 필터링 우선순위를 명시함
