# Palia MediaMint

**All-in-One Media & PDF Tools**  
By **PaliaAPK HUB** · Developer: **shanpalia**

Palia MediaMint is a native Android application built with Kotlin and Jetpack Compose, following the approved white + mint-green design.

## Android project

- Native Android: Kotlin + Jetpack Compose (not a WebView)
- Application ID: `com.palia.mediamint`
- Minimum Android version: Android 8.0 (API 26)
- Compile / target SDK: 36
- Java: 17
- File selection: Android Storage Access Framework (system file picker)
- Network permission: INTERNET

## Planned tools

### Video
- Video to MP3
- Video to Image / frame extraction
- Video compression
- Video to GIF

### Image
- Image to PDF
- JPG / PNG conversion
- Image compression and resize

### PDF
- PDF to Image
- Merge, split, rotate and compress PDF

### AI photo tools
- AI watermark remover for images the user owns or is authorized to edit
- Text and object cleanup

## Build locally

Open the repository in Android Studio, allow Gradle sync, then run the `:app` configuration.

With Android SDK and Gradle installed:

```bash
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

## GitHub Actions

Every push to `main` triggers **Android CI**. The workflow builds a debug APK and uploads it as the `Palia-MediaMint-debug` artifact. Open the repository's **Actions** tab and download the artifact after a successful run.

## Development status

The initial native Android foundation, branded vector launcher icon, Compose home screen, bottom navigation and Android system file picker are in place. The file picker is wired, but **online conversion and AI processing are not connected yet**. They require a deployed HTTPS backend and must be integrated before conversion tools can produce downloadable results. No API keys or secrets should be committed to this repository.
