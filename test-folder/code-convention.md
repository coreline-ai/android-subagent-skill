# Test Folder Code Convention

## 1. Android
- UI는 Jetpack Compose를 사용한다.
- MVP 상태 관리는 MVVM을 기본으로 사용한다.
- ViewModel은 `StateFlow<UiState>`를 외부에 노출한다.
- 로컬 저장소 접근은 `Dispatchers.IO`에서 수행한다.
- 추천 계산은 `Dispatchers.Default` 또는 백그라운드 스레드에서 수행한다.

## 2. Recommendation Engine
- scorer 인터페이스는 Kotlin 구현과 Native 구현을 교체 가능하게 설계한다.
- Native 구현 사용 시 JNI 호출은 UI 계층에서 직접 수행하지 않는다.
- JNI 예외는 앱 크래시 대신 실패 상태로 변환한다.

## 3. Compose
- 재사용 가능한 Composable은 `modifier: Modifier = Modifier`를 제공한다.
- 화면은 ViewModel 상태를 단방향으로 소비한다.

## 4. Testing
- RecommendationUseCase 단위 테스트 필수
- 제외 재료/알레르기 필터 테스트 필수
- 결과 0건 상태 테스트 필수
- Compose 결과 화면 테스트 권장

## 5. Security
- API 키 하드코딩 금지
- cleartext traffic 비허용
- 불필요한 위험 권한 금지
