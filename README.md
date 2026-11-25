# AuraScan - PDF Scanner and OCR

An offline, privacy-first Android document scanner, PDF builder, on-device OCR extractor, and photo/signature resizer built with Kotlin, Jetpack Compose, and Material 3.

## Highlights

- **100% On-Device & Offline**: No login, no backend, no analytics, and zero dangerous permissions (`CAMERA`, `READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, and `READ_MEDIA_*` are never requested).
- **ML Kit Document Scanner** (`SCANNER_MODE_FULL`, gallery import, 20-page limit, JPEG + PDF output) stores documents privately in `filesDir/documents/<id>/` indexed by Room.
- **On-Device OCR (ML Kit Text Recognition v2)**: Supports **Latin** and **Hindi (Devanagari)** scripts with selectable text, Copy, Share as `.txt`, and Save as `.txt` (`FREE_OCR_PAGE_LIMIT = 3`, `isProUnlocked()` hook).
- **Images to PDF**: Multi-image Photo Picker, up/down reordering, A4 or Fit-to-Image sizing, configurable margins, built with Android `PdfDocument`.
- **Photo & Signature Resizer**: Presets for Exam Photo (`50 KB`, `200 × 230 px`), Exam Signature (`20 KB`, `140 × 60 px`), Passport Photo (`413 × 531 px`), interactive crop step, and a pure-Kotlin binary quality search + 5% step-downscale optimizer with JVM unit tests.
- **English & Hindi Localization**: Full `res/values/strings.xml` and `res/values-hi/strings.xml`.
- **Release Ready**: R8 minification, resource shrinking, and ProGuard keep rules for Room, ML Kit, and Coil 3.

## Requirements

- **Android Studio**: Ladybug / Meerkat / Narwhal or newer (supporting AGP 8.13.2 & Android 16 / API 36)
- **JDK**: 17
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 24`

## Build & Test Steps

1. **Open the Project**
   Open this root folder in Android Studio (`File -> Open...`) and let Gradle sync (`gradle-wrapper.properties` targets Gradle 8.13).
   *(If building from CLI without a pre-generated `gradle-wrapper.jar`, run `gradle wrapper` once with a local Gradle 8.13+ installation, or open in Android Studio which provisions the wrapper automatically.)*

2. **Customize Constants & Package Name**
   - Update `Constants.kt` (`app/src/main/java/com/yourname/aurascan/Constants.kt`) with your real Privacy Policy URL, Terms of Use URL, and support email.
   - Refactor `com.yourname.aurascan` (and `applicationId` / `namespace` in `app/build.gradle.kts`) to your own reverse-domain package name before publishing to Google Play.

3. **Run Unit Tests (Pure Kotlin Resizer Optimizer)**
   ```bash
   ./gradlew testDebugUnitTest
   ```

4. **Build Debug APK**
   ```bash
   ./gradlew assembleDebug
   ```

5. **Build Release AAB / APK (with R8 Minification & Resource Shrinking)**
   ```bash
   ./gradlew bundleRelease
   ```
   *(Configure your release signing keystore in `app/build.gradle.kts` or Android Studio's Generate Signed App Bundle wizard.)*
