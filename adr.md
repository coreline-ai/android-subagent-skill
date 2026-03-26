# Architecture Decision Records (ADR) - 기술적 결정 사항 기록

이 문서는 프로젝트 진행 중 발생한 주요 아키텍처 설계와 기술적 결정 사항을 기록합니다.
각 결정은 배경(Context), 결정(Decision), 그리고 그로 인한 파급 효과(Consequences)를 명확히 문서화하여 팀원 간의 컨텍스트를 동기화하고 향후 유지보수를 돕습니다.

---

## [ADR-001] 안드로이드와 C++(NDK) 간의 JNI 브릿지 설계 구조 결정

*   **Status:** Accepted
*   **Date:** 2026-03-26

### Context (배경)
프로젝트 내 코어 엔진(미디어 처리 및 고성능 연산 등)이 점차 무거워짐에 따라, 안드로이드 Kotlin 코드만으로는 성능 요구 사항을 맞추기 어려워졌습니다. C++ 기반 NDK 적용이 불가피한 상황이며, 이에 맞춰 Java/Kotlin과 네이티브(C++) 간의 메모리 및 통신(JNI) 처리를 최적으로 설계할 필요가 있습니다.

### Decision (결정사항)
1.  **클린 아키텍처 기반의 네이티브 모듈 분리:** NDK(C++) 엔진을 Android 프레임워크와 직접적으로 얽히지 않도록 완전히 독립된 Layer로 분리합니다. UI 스레드 개입을 피하기 위해 반드시 백그라운드 스레드에서 JNI 콜을 수행합니다.
2.  **포인터 관리 (Pointer Management):** C++ 객체의 라이프사이클 관리를 위해 메모리 주소(pointer)를 `Long` 타입으로 캐스팅하여 Kotlin의 JNA/JNI Wrapper 클래스 보관합니다. C++의 객체 생성 시 주소를 반환하고, 해제 시 উক্ত 주소를 받아 `delete` (또는 스마트 포인터 해제) 처리합니다.
3.  **예외 처리:** C++에서 발생한 예외(Exception)는 JNI 계층에서 안전하게 캐치(try-catch)하여 안드로이드 앱의 크래시를 방지하고, 에러 코드를 Kotlin으로 반환(`Result` 객체 활용)하여 처리합니다.

### Consequences (결과 및 영향)
*   **Positive:** NDK 로직과 Android 비즈니스 로직 간 결합도가 크게 낮아지고, 고성능 연산이 Java GC(가비지 컬렉터)의 영향을 받지 않습니다.
*   **Negative:** JNI 브릿지 코드를 작성하고 유지보수하는 보일러플레이트 코드가 증가하며, 메모리 프로파일링(메모리 릭 추적) 난이도가 상승합니다.

---

## [ADR-002] 비동기 처리를 위한 Kotlin Coroutines 채택 및 RxJava 마이그레이션 보류

*   **Status:** Accepted
*   **Date:** 2026-03-26

### Context (배경)
현재 모바일 안드로이드 생태계는 비동기 처리의 표준으로 RxJava에서 Coroutines/Flow로 완전히 이동했습니다. 새롭게 추가되는 고비용 JNI 네이티브 호출과 네트워크 통신 스레드 관리에 대해 일관된 비동기 처리 프레임워크를 지정할 필요가 있습니다.

### Decision (결정사항)
1.  모든 비동기 처리 및 네이티브 모듈(NDK) 통신에 **Kotlin Coroutines**과 **StateFlow/SharedFlow**를 전면 도입합니다.
2.  이미 레거시로 존재하는 RxJava 코드는 우선 유지하되 새로운 피처에는 RxJava 사용을 금지하며, 장기적으로 Flow로 마이그레이션합니다.
3.  JNI 코어 엔진 호출 시 Dispatcher는 명시적으로 `Dispatchers.Default` 또는 `Dispatchers.IO`로 강제합니다.

### Consequences (결과 및 영향)
*   **Positive:** Google의 최신 권장 사항을 따르게 되어 Lifecycle과 연계된 안정적인 동시성 제어(`lifecycleScope`, `viewModelScope`)가 가능해집니다. 가독성이 뛰어난 순차적 코드를 작성할 수 있습니다.
*   **Negative:** 기존 RxJava에 익숙한 팀원들에게 Coroutines Flow 관련 학습 곡선이 발생할 수 있습니다.

---

## [ADR-003] UI 상태 관리를 위한 MVI (Model-View-Intent) 아키텍처 도입

*   **Status:** Proposed
*   **Date:** 2026-03-26

### Context (배경)
뷰 상태(UiState)가 복잡해지면서 기존 MVVM 패턴에서 다수의 LiveData/StateFlow가 뷰모델에 혼재되어 예기치 않은 상태 버그(State Bug)가 발생하고 있습니다. 특히 네이티브 콜백이 UI에 반영되는 시점에 뷰모델 상태가 일관되지 않는 문제가 있습니다.

### Decision (결정사항)
1.  새로 작성되는 화면(Screen) 단위부터 순수 상태 기계(State Machine) 형태인 **MVI 아키텍처**를 시범 도입합니다.
2.  사용자의 모든 인터랙션과 C++ 연산 콜백을 `Intent` (또는 `Action`)라는 하나의 스트림으로 통합합니다.
3.  단일 출처(Single Source of Truth)로 구성된 `UiState` 데이터 클래스를 발행하여 뷰를 렌더링합니다.
4.  본 결정이 `Accepted` 되기 전까지는 MVI를 프로젝트 전역의 필수 규칙으로 강제하지 않으며, TRD 또는 후속 ADR에서 명시적으로 채택한 화면/모듈에 한해 적용합니다.

### Consequences (결과 및 영향)
*   **Positive:** UI 생명주기에 따른 상태 버그를 크게 줄일 수 있으며, 네이티브 상태 처리가 예측 가능해집니다. 단방향 데이터 흐름(UDF, Unidirectional Data Flow)을 강제할 수 있어 테스트 용이성이 뛰어납니다.
*   **Negative:** Action과 State를 맵핑해야 하므로 초기 보일러플레이트(의도, 리듀서 작성 등)가 많아집니다.
