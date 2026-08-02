# Android build variants

| Variant | App ID | API server | Use |
| --- | --- | --- | --- |
| `debug` | `com.wafflestudio.hangsha_android.debug` | `https://hangsha-api-dev.wafflestudio.com/` | Daily development |
| `release` | `com.wafflestudio.hangsha_android` | `https://hangsha-api.wafflestudio.com/` | Play Store |

## Switching variants in Android Studio

Open **Build Variants** in the lower-left tool window and choose `debug` or `release` for the `app` module. The Run button builds the selected variant.

Both apps can be installed together because debug has the `.debug` package suffix.

## Commands

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat bundleRelease
```

The Play upload file is `app\build\outputs\bundle\release\app-release.aab`.

## Signing before the first Play upload

Use **Build > Generate Signed Bundle / APK > Android App Bundle > release** to create a keystore and signed AAB. Back up the keystore and passwords securely; the same key is needed for future updates.

`keystore.properties.example` is provided as a local record template. The actual file and common keystore extensions are ignored by Git.

## Login configuration

Because debug uses `com.wafflestudio.hangsha_android.debug`, register that package and the debug signing certificate separately in Google, Kakao, and Naver when their console requires Android package names or SHA fingerprints. Register the release package and release signing certificate for the Play build.

Increase `versionCode` for every Play upload.
