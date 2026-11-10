# Hangsha Android

교내의 다양한 행사 일정을 한곳에서 확인하고, 관심 있는 행사를 자신의 일정과 함께 관리할 수 있는 Hangsha의 Android 애플리케이션입니다.

월별 캘린더에서 행사를 탐색하는 것부터 관심 행사 저장, 행사별 메모 작성, 시간표와 행사 일정 확인까지 하나의 앱에서 제공합니다. 로그인하지 않아도 행사 정보를 둘러볼 수 있으며, 로그인하면 개인화 기능을 사용할 수 있습니다.

## 주요 기능

- **행사 캘린더**: 월별 캘린더와 날짜별 목록으로 행사 일정을 확인할 수 있습니다.
- **검색과 필터**: 행사명으로 검색하고 행사 종류, 주최 기관, 모집 현황 및 제외 키워드로 원하는 행사만 골라볼 수 있습니다.
- **행사 상세 정보**: 행사의 일정과 세부 내용을 확인하고, 잘못된 행사 정보는 앱에서 바로 제보할 수 있습니다.
- **관심 행사 관리**: 관심 있는 행사를 찜하고 마이페이지에서 다시 확인할 수 있습니다.
- **행사 후기와 메모**: 행사별 메모와 태그를 작성하고, 작성한 내용을 한곳에서 조회·수정·삭제할 수 있습니다.
- **관심사 우선순위**: 선호하는 행사 카테고리와 주최 기관을 설정해 먼저 보고 싶은 행사를 개인화할 수 있습니다.
- **시간표 관리**: 시간표를 만들고 수업을 직접 추가하거나 SNUTT 시간표를 가져올 수 있습니다. 주간 시간표 위에서 행사 일정도 함께 확인할 수 있습니다.
- **다양한 로그인 방식**: 이메일과 Google, Kakao, Naver 로그인을 지원하며 게스트로도 시작할 수 있습니다.

## 기술 구성

- Kotlin
- Jetpack Compose
- Hilt
- Retrofit
- Material 3

## 개발 환경

- Android Studio
- JDK 11
- Android SDK (compile SDK 36)
- 최소 지원 Android SDK 24

## 로컬 실행

1. Android Studio에서 이 프로젝트를 엽니다.
2. 프로젝트 루트에 `local.properties` 파일을 만들고, 필요한 로컬 설정값을 추가합니다. 이 파일은 민감한 값이 포함될 수 있으므로 Git으로 관리하지 않습니다.
3. Android Studio에서 `app`의 `debug` 변형을 선택해 실행하거나, 아래 명령을 실행합니다.

```bash
./gradlew.bat assembleDebug
```

## 릴리스 빌드와 R8 QA

`release` 빌드는 R8 코드 축소·최적화·난독화와 미사용 리소스 제거를 적용합니다.
`debug` 빌드에는 적용하지 않습니다. Gson 통신 모델, 로컬 저장 모델, 로그인 SDK에
필요한 보존 규칙은 `app/proguard-rules.pro`에서 관리합니다.

```bash
./gradlew.bat :app:testDebugUnitTest :app:assembleRelease :app:bundleRelease
```

명령행 기본 산출물은 서명되지 않은 `app/build/outputs/apk/release/app-release-unsigned.apk`와
`app/build/outputs/bundle/release/app-release.aab`입니다. 설치·로그인 QA는 Android Studio의
**Build > Generate Signed App Bundle or APK**에서 기존 배포 키로 서명한 `release` 빌드로 진행합니다.
소셜 로그인 서비스에 등록된 패키지명·서명 인증서·앱 키가 해당 빌드와 일치해야 합니다.
Play 앱 서명을 사용한다면 내부 테스트 트랙으로 설치해서 Play 서명 기준으로 확인할 수 있습니다.

휴대폰에 설치한 release 앱의 로그도 Android Studio의 **View > Tool Windows > Logcat**에서
확인할 수 있습니다. 연결한 기기를 선택하고 `package:com.wafflestudio.hangsha_android`로
필터링한 뒤 휴대폰에서 앱을 직접 실행합니다. 일반 release는 중단점 디버깅이 제한되지만
Logcat 수집은 가능합니다.

릴리스에서만 소셜 로그인이 실패하면 실제 설치한 APK의 서명을 기준으로 설정을 확인합니다.

- Google 오류 `10`은 OAuth 설정을 점검합니다. `GOOGLE_SERVER_CLIENT_ID`의 Web 클라이언트와
  같은 프로젝트에 release 패키지명과 서명 SHA-1을 등록한 Android OAuth 클라이언트가 필요합니다.
- Kakao 로그에 `Android keyHash validation failed`가 있으면 `RELEASE_KAKAO_NATIVE_APP_KEY`에
  해당하는 카카오 앱에 release 서명의 키 해시를 등록합니다.
- 직접 서명한 APK와 Play에서 설치한 앱은 서명 인증서가 다를 수 있으므로 각각 확인합니다.
  [Google 서명 안내](https://developers.google.com/android/guides/client-auth),
  [Kakao 키 해시 안내](https://developers.kakao.com/docs/latest/ko/android/getting-started#key-hash)를 참고합니다.

- 기존 앱 위에 업데이트 설치한 뒤 로그인 상태, 찜, 제외 키워드가 유지되는지 확인합니다.
- 이메일·Google·Kakao·Naver 로그인과 로그아웃, 앱 종료 후 재실행을 확인합니다.
- 행사 캘린더·검색·필터·상세·찜과 관심사 설정의 조회 및 저장을 확인합니다.
- 메모·프로필·제외 키워드의 조회 및 변경 후 재실행해 저장 결과를 확인합니다.
- 시간표 조회·수업 추가/수정과 SNUTT 가져오기, 웹 로그인 후 앱 복귀를 확인합니다.

오류 보고에는 앱 버전, 재현 순서, 크래시 로그를 함께 남깁니다. 난독화된 크래시 로그를
해석하려면 **그 빌드를 생성했을 때의** `app/build/outputs/mapping/release/mapping.txt`가 필요하므로,
QA·배포한 APK/AAB와 함께 별도로 보관합니다. 다음 빌드에서 이 파일이 덮어써질 수 있습니다.
Play 배포 시에도 App Bundle Explorer에서 해당 버전의 난독화 매핑 파일 등록 여부를 확인합니다.

## 브랜치 규칙

브랜치는 아래의 순서와 목적에 따라 관리합니다.

| 브랜치 | 목적 | 병합 대상 |
| --- | --- | --- |
| `dgddgd` | 주 작업 브랜치입니다. 기능 개발과 일상적인 작업은 이 브랜치에서 진행합니다. | `develop` |
| `develop` | 새 기능 개발이 완료된 상태를 모아 두는 통합 브랜치입니다. | `deploy` |
| `deploy` | 기존 `master` 브랜치에 해당합니다. 실제 앱스토어에 배포된 코드를 보관하는 배포 브랜치입니다. | 앱스토어 배포 |

기본 작업 흐름은 다음과 같습니다.

1. `dgddgd`에서 기능을 개발하고 검증합니다.
2. 기능 개발이 완료되면 `develop`으로 병합합니다.
3. 배포할 변경사항이 준비되면 `develop`의 내용을 `deploy`로 병합한 뒤 앱스토어에 배포합니다.

배포 안정성을 위해 `deploy`에는 긴급 수정이나 검증된 배포 변경사항만 직접 반영합니다.
