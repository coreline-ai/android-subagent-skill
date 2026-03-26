# Test Folder ADR

## ADR-001 추천 엔진 JNI 분리
- Status: Accepted
- Recommendation scorer는 UI 계층과 분리된 모듈로 유지한다.
- Native 구현 사용 시 포인터 수명주기와 예외 처리를 JNI 계층에서 관리한다.

## ADR-002 Kotlin Coroutines 채택
- Status: Accepted
- 비동기 처리에는 Kotlin Coroutines와 Flow를 사용한다.
- Room/파일 I/O는 `Dispatchers.IO`, 점수 계산은 `Dispatchers.Default`를 사용한다.

## ADR-003 MVI 도입
- Status: Proposed
- MVP 기본 상태 패턴은 MVVM으로 유지한다.
- MVI는 추후 복잡한 상태 화면에 한해 별도 채택 시 적용한다.
