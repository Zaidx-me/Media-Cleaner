# Building WhatsAppCleaner

This document describes the requirements and repeatable steps to build this project.

## Requirements

- OS: Windows, macOS, or Linux
- JDK: 17 (required)
- Android SDK:
  - Platform Tools
  - Build Tools 34.0.0+
  - Android Platform API 36 (current project compile target)
- Network access for first build (Gradle dependencies and missing SDK packages)

## Project Build Configuration

- Module: `app`
- `minSdk`: 24
- `compileSdk`: 36
- `targetSdk`: 36

## One-Time Setup

### 1. Set Java and Android SDK paths

Set environment variables so Gradle can find Java and SDK tools.

Windows (PowerShell, current session):

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:ANDROID_HOME = "C:\Users\<you>\AppData\Local\Android\Sdk"
```

macOS/Linux (bash/zsh):

```bash
export JAVA_HOME="/path/to/jdk-17"
export ANDROID_HOME="$HOME/Android/Sdk"
```

### 2. Add `local.properties`

Create `local.properties` in the repository root:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
```

Use forward slashes in `sdk.dir`.

## Build Commands

From repository root:

```bash
./gradlew assembleDebug
./gradlew assembleRelease
./gradlew assemblePlay
./gradlew testDebug
./gradlew spotlessCheck
./gradlew spotlessApply
```

On Windows Command Prompt / PowerShell:

```powershell
.\gradlew assembleDebug
.\gradlew assembleRelease
.\gradlew assemblePlay
.\gradlew testDebug
.\gradlew spotlessCheck
.\gradlew spotlessApply
```

## Build Outputs

- Debug APK: `app/build/outputs/apk/debug/app-debug.apk`
- Release APK: `app/build/outputs/apk/release/`
- Play variant APK: `app/build/outputs/apk/play/`

## Troubleshooting

### `JAVA_HOME is not set`

- Ensure JDK 17 is installed.
- Set `JAVA_HOME` to JDK root (not `bin`).

### `SDK location not found`

- Ensure `local.properties` exists with valid `sdk.dir`.
- Or set `ANDROID_HOME`/`ANDROID_SDK_ROOT`.

### `Failed to find target with hash string 'android-XX'`

- Install the required Android platform in SDK Manager.
- Re-run build after installation.

### Corrupted/missing SDK platform files

- Reinstall the affected Android platform package from SDK Manager.
- Retry `./gradlew assembleDebug`.
