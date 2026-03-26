# 안드로이드 및 NDK (C/C++) 코딩 컨벤션

본 문서는 안드로이드 앱 개발(Kotlin/Java) 및 NDK(Native Development Kit) 기반의 C/C++ 개발 시 일관성을 유지하고 유지보수성을 높이기 위한 코딩 규약을 정의합니다.

## 1. 안드로이드 (Kotlin) 코딩 컨벤션

Android 공식 Kotlin 스타일 가이드(Android Kotlin Style Guide)를 기본으로 따릅니다.

### 1.1 명명 규칙 (Naming Conventions)
*   **클래스 및 인터페이스:** 파스칼 케이스(PascalCase)를 사용합니다. (예: `UserRepository`, `MainActivity`)
*   **함수 및 변수:** 카멜 케이스(camelCase)를 사용합니다. (예: `getUserList()`, `isLoading`)
*   **상수 (Constants):** `const val`을 사용하며, 대문자와 언더스코어(SNAKE_CASE)를 사용합니다. (예: `MAX_RETRY_COUNT`)
*   **리소스 파일 (XML):** 소문자와 언더스코어(snake_case)를 사용하며, 접두사로 뷰 또는 역할을 명시합니다. (예: `activity_main.xml`, `ic_arrow_back.xml`, `btn_submit.xml`)
*   **백킹 프로퍼티 (Backing Properties):** 내부적으로 가변 상태인 MutableLiveData/MutableStateFlow 등은 언더스코어(`_`)로 시작하고, 외부에 노출되는 불변 속성은 언더스코어 없이 선언합니다.
    ```kotlin
    private val _uiState = MutableStateFlow(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    ```

### 1.2 아키텍처 및 디자인 (Architecture)
*   일반적으로 **MVVM (Model-View-ViewModel)** 아키텍처를 기본으로 하며, **MVI (Model-View-Intent)** 는 TRD/ADR에서 명시적으로 채택한 경우에 적용합니다.
*   클린 아키텍처(Clean Architecture) 원칙에 따라 UI 계층, 도메인 계층(UseCases), 데이터 계층(Repository, DataSource)을 명확하게 분리합니다.

### 1.3 코루틴 및 비동기 처리
*   글로벌 스코프(`GlobalScope`) 사용을 지양하고, 안드로이드 생명주기에 맞는 `viewModelScope`나 `lifecycleScope`를 사용합니다.
*   모든 디스크 및 네트워크 I/O 작업은 `Dispatchers.IO` 컨텍스트에서 실행되어야 합니다. 메인 스레드(`Dispatchers.Main`)를 차단하지 마십시오.

### 1.4 Jetpack Compose UI 컨벤션
Jetpack Compose를 사용하는 UI 코드는 다음 규칙을 따릅니다.
*   **Composable 함수 명명:** 파스칼 케이스(PascalCase)를 사용합니다. (예: `UserProfileScreen`, `SettingsCard`)
*   **상태 호이스팅 (State Hoisting):** Composable 함수는 가능한 한 **Stateless**하게 유지합니다. 상태는 호출자(Caller)가 관리하고(State Hoisting) Composable에는 파라미터로 전달합니다.
    ```kotlin
    @Composable
    fun CounterButton(
        count: Int,
        onIncrement: () -> Unit,
        modifier: Modifier = Modifier
    ) { ... }
    ```
*   **`@Preview` 어노테이션 필수:** 모든 재사용 가능한 Composable에 `@Preview`를 반드시 작성하여 디자인 타임에 시각적 확인이 가능하도록 합니다.
*   **Side-effect API 사용 규칙:**
    *   `LaunchedEffect`: Composable 진입 시 또는 키 변경 시 코루틴 실행. 반드시 적절한 `key`를 지정합니다.
    *   `DisposableEffect`: 리소스 해제가 필요한 경우(리스너 등록/해제). `onDispose` 블록에서 반드시 정리합니다.
    *   `SideEffect`: 매 recomposition마다 실행. 외부 상태 동기화에만 제한적으로 사용합니다.
*   **Modifier 체이닝:** Composable 함수의 첫 번째 선택적 파라미터로 항상 `modifier: Modifier = Modifier`를 선언하며, 내부에서 기본 Modifier에 체이닝합니다.
*   **Navigation:** Compose Navigation의 `NavHost`/`composable` 패턴을 따르며, 딥 링크 및 인자(argument) 전달 시 타입 안전성을 보장합니다.

---

## 2. 안드로이드 NDK (C/C++) 코딩 컨벤션

Google의 C++ 스타일 가이드 및 Android AOSP 가이드라인을 기반으로 JNI 기반 고유 설정을 추가하여 따릅니다.

### 2.1 C/C++ 일반 명명 규칙
*   **파일명:** 소문자와 언더스코어를 사용합니다. (예: `video_processor.cpp`, `audio_decoder.h`)
*   **클래스 및 구조체:** 파스칼 케이스(PascalCase)를 사용합니다. (예: `VideoProcessor`)
*   **메서드 및 함수:** 카멜 케이스(camelCase)를 사용합니다. (예: `processFrame()`)
*   **변수:** 소문자와 언더스코어(snake_case)를 사용합니다. (예: `frame_buffer`)
*   **클래스 멤버 변수:** 끝에 언더스코어를 붙입니다. (예: `int current_state_;`)

### 2.2 JNI 명명 규칙 및 함수 정의
JNI (Java Native Interface) 브릿지 함수는 Java/Kotlin과 네이티브 코드를 연결하므로 명확한 네이밍이 필수입니다.

*   **JNI 함수 명명:** `Java_{패키지명_밑줄_구분}_{클래스명}_{메소드명}` 형태를 엄격히 따릅니다.
    *   *예시:* `com.example.app.NativeLib` 클래스의 `initEngine` 메서드
    *   *JNI 정의:* `extern "C" JNIEXPORT void JNICALL Java_com_example_app_NativeLib_initEngine(JNIEnv* env, jobject thiz)`
*   네이티브 코드가 C++로 작성되었더라도 JNI 심볼이 C 방식으로 노출되도록 구현부 전체를 `extern "C"` 블록으로 감싸거나 함수 선언에 표기해야 합니다.

### 2.3 JNIEnv와 메모리 관리 (Memory Management)
JNI 및 C++ 통합 시 메모리 누수(Memory Leak)는 크래시의 주원인이 되므로 엄격하게 관리합니다.

*   **JNIEnv 포인터 캐싱 금지:** `JNIEnv*` 포인터는 각 운영체제 스레드마다 고유합니다. 이를 여러 스레드 간에 공유하거나 전역 변수로 저장하지 마세요. C++ 백그라운드 스레드에서 JNI 콜이 필요하다면 캐싱해둔 `JavaVM*`을 사용하여 `AttachCurrentThread`를 통해 해당 스레드의 `JNIEnv*`를 얻어와야 합니다.
*   **Global/Local Reference 관리:**
    *   전달되거나 생성된 Java 객체는 기본적으로 Local Reference입니다. C++ 함수 실행이 끝나면 자동 해제되나, 루프(for/while) 내에서 무수히 많은 Java 객체를 생성한다면 JNI 레퍼런스 제한(일반적으로 512개)에 걸려 크래시가 발생할 수 있습니다. 이런 경우 루프 안에서 즉시 `env->DeleteLocalRef()`를 호출하십시오.
    *   네이티브 측에서 Java 객체의 라이프사이클을 오래 유지해야 한다면 반드시 `env->NewGlobalRef()`를 사용하고, 사용이 끝나면 소멸자 등에서 `env->DeleteGlobalRef()`를 호출해 메모리를 해제합니다.
*   **문자열 처리 (`jstring`):**
    *   `GetStringUTFChars`를 사용해 네이티브 문자열로 변환한 후, 반드시 짝을 맞춰 `ReleaseStringUTFChars`를 호출하십시오.
    ```cpp
    const char *native_string = env->GetStringUTFChars(java_string, nullptr);
    // C++ 문자열 처리 로직
    env->ReleaseStringUTFChars(java_string, native_string);
    ```

### 2.4 C++ 표준 및 도구
*   **C++ 버전:** C++17 이상을 기본으로 사용합니다. (`build.gradle`의 `cppFlags "-std=c++17"` 확인)
*   **스마트 포인터:** 원시 포인터(`*`)의 raw 할당(`new`/`delete`) 대신 `std::unique_ptr` 및 `std::shared_ptr`을 사용하여 메모리 릭 코드를 원천 차단합니다.
*   **로깅:** `android/log.h`를 임베드하여 C++ 내에서도 logcat 출력을 남깁니다. 다음과 같이 매크로를 만들어 사용하는 것을 권장합니다.
    ```cpp
    #include <android/log.h>
    #define LOG_TAG "NativeLib"
    #define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
    #define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
    ```

---

## 3. 포매팅 및 정적 분석 (Formatting & Linting)

*   **Kotlin/Java:** `ktlint` 또는 `detekt`를 CI 및 로컬 빌드 스크립트에 포함하여 코드의 일관성을 검사합니다.
*   **C/C++ (NDK):** `clang-format`을 사용하여 `Google` 스타일 포맷을 일괄 적용합니다. C++ 소스 파일이 있는 폴더에 `.clang-format` 설정 파일을 두어 컨벤션을 강제합니다.
    *   *예시:* `clang-format -i -style=Google path/to/source.cpp`

## 4. 안드로이드 보안 및 배포 규칙 (Security & Release)

*   **API Key / Secret 관리:** 소스 코드 내에 API 키, 토큰, 비밀번호를 절대 하드코딩하지 않습니다. `BuildConfig` 필드, `local.properties`, 또는 암호화된 저장소(EncryptedSharedPreferences, Android Keystore)를 활용합니다.
*   **네트워크 보안:** `network_security_config.xml`을 구성하여 프로덕션 빌드에서 cleartext 트래픽(HTTP)을 차단합니다. 인증서 피닝(Certificate Pinning)을 권장합니다.
*   **ProGuard/R8 난독화 규칙:** JNI를 통해 호출되는 클래스/메서드는 반드시 `proguard-rules.pro`에 `-keep` 규칙을 추가합니다. 누락 시 릴리즈 빌드에서 `UnsatisfiedLinkError` 크래시가 발생합니다.
*   **런타임 권한 처리:** 위험 권한(CAMERA, LOCATION, RECORD_AUDIO 등)은 `ActivityResultContracts.RequestPermission`을 사용하여 런타임에 요청하며, 거부 시 적절한 대체 UI를 표시합니다.

## 5. 커밋 메시지 컨벤션

Git 커밋 작성 시 컨벤션을 따라야 합니다.
*   **기본 형식:** `<타입>(<스코프>): <제목>`
    *   *예시:* `feat(ndk): 오디오 인코딩 처리용 JNI 브릿지 라이브러리 추가`
*   **타입(Type) 목록:**
    *   `feat`: 새로운 기능 추가
    *   `fix`: 버그 수정
    *   `refactor`: 코드 리팩토링 (기능 변경 없이 내부 구조 변경)
    *   `test`: 테스트 추가 또는 수정
    *   `style`: 포맷팅(공백, 세미콜론 등), 코드 변경 없음
    *   `docs`: 문서 생성 및 수정
    *   `build`/`ci`: 빌드 스크립트(Gradle, CMake) 또는 CI/CD 설정 수정
