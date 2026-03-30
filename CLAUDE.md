# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application built with Kotlin and Jetpack Compose using modern Android development practices. The project follows a single-activity architecture with Compose UI and uses Material 3 design system.

**Key Details:**
- **Package**: `com.example.myapplication`
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 16)
- **Compile SDK**: 36
- **Java Version**: 11
- **Kotlin Version**: 2.3.20

## Build System

**注意：** 更新项目之后不需要执行 `./gradlew` 相关操作。（Note: After updating the project, there is no need to execute `./gradlew` commands.)

The project uses Gradle with Kotlin DSL and a version catalog (`gradle/libs.versions.toml`). Key build files:
- `build.gradle.kts` - Root build configuration
- `app/build.gradle.kts` - App module configuration
- `settings.gradle.kts` - Project settings
- `gradle/libs.versions.toml` - Centralized dependency versions
- `build-convention/` - Gradle convention plugins for standardized module configuration

### Common Gradle Commands

**Building:**
```bash
# Build the project
./gradlew build

# Clean build
./gradlew clean build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease
```

**Testing:**
```bash
# Run all unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test variant
./gradlew testDebugUnitTest
```

**Running:**
```bash
# Install and run debug variant on connected device
./gradlew installDebug

# Install release variant
./gradlew installRelease
```

**Other Useful Commands:**
```bash
# Check dependencies
./gradlew dependencies

# Run lint checks
./gradlew lint

# Generate APK analysis
./gradlew analyzeApkDebug
```

## Architecture

### Single-Activity Architecture
- `MainActivity.kt` is the only activity
- Uses `enableEdgeToEdge()` for edge-to-edge display
- UI is built entirely with Jetpack Compose

### Jetpack Compose Setup
- Compose BOM (Bill of Materials) manages dependency versions
- Material 3 design system with dynamic color support (Android 12+)
- Theme system supports dark/light mode and dynamic colors

### Theme System
Located in `app/src/main/java/com/example/myapplication/ui/theme/`:
- `Theme.kt` - Theme configuration with dynamic color support
- `Color.kt` - Color palette definitions (Purple, PurpleGrey, Pink variants)
- `Type.kt` - Typography definitions

### Dependencies (via version catalog)
Key dependencies include:
- AndroidX Core KTX
- AndroidX Lifecycle Runtime KTX
- AndroidX Activity Compose
- Jetpack Compose (UI, Material3, Tooling)
- Testing: JUnit 4, AndroidJUnit4, Espresso, Compose testing

## Code Structure

### Module Structure
```
MyApplication/
├── app/                    # Main application module
│   ├── src/main/java/com/example/myapplication/
│   │   ├── MainActivity.kt          # Single activity with Compose UI
│   │   ├── entity/                  # Entity classes
│   │   │   ├── Car.kt
│   │   │   └── Notification.kt
│   │   └── ui/theme/               # Compose theme system
│   │       ├── Theme.kt           # Theme configuration
│   │       ├── Color.kt           # Color definitions
│   │       └── Type.kt            # Typography definitions
│   ├── src/main/res/               # Android resources
│   ├── src/test/                   # Unit tests (JUnit 4)
│   └── src/androidTest/            # Instrumented tests (AndroidJUnit4)
├── core/                          # Core functionality modules
│   └── mlkit/                     # ML Kit integration
│       ├── src/main/java/com/mjc/core/mlkit/
│       ├── src/test/              # Unit tests
│       └── src/androidTest/       # Instrumented tests
├── feature/                       # Feature modules
│   ├── camera/                    # Camera feature
│   │   ├── src/main/java/com/mjc/feature/camera/
│   │   │   └── CameraScreen.kt    # Camera UI component
│   │   ├── src/test/              # Unit tests
│   │   └── src/androidTest/       # Instrumented tests
│   └── videoplayer/               # Video player feature
│       ├── src/main/java/com/mjc/feature/videoplayer/
│       ├── src/test/              # Unit tests
│       └── src/androidTest/       # Instrumented tests
├── build-convention/              # Gradle build conventions
│   └── src/main/kotlin/          # Convention plugins
│       ├── android-application.gradle.kts
│       ├── android-library.gradle.kts
│       └── compose-convention.gradle.kts
└── gradle/                       # Gradle configuration
    └── libs.versions.toml        # Version catalog
```

### Module Dependencies
- **app** → feature:camera, feature:videoplayer, core:mlkit
- **feature:camera** → core:mlkit
- **feature:videoplayer** → (standalone feature)
- **core:mlkit** → (core library)
- All modules use build-convention for standardized configuration

## Development Notes

### Adding New Features
1. Create Compose functions in appropriate packages (consider creating separate files for complex UI)
2. Follow Material 3 design guidelines
3. Use the existing theme system (`MyApplicationTheme`)
4. Add tests in corresponding test directories

### Testing
- Unit tests go in `app/src/test/` (JUnit 4)
- Instrumented tests go in `app/src/androidTest/` (AndroidJUnit4)
- Compose UI tests use `androidx.compose.ui.test.junit4`

### Theme Customization
- Modify colors in `Color.kt`
- Update typography in `Type.kt`
- Theme supports dynamic colors on Android 12+ (enabled by default)

### Building for Release
- Release build type has ProGuard configuration (`proguard-rules.pro`)
- Minify is disabled by default (`isMinifyEnabled = false`)
- Update `versionCode` and `versionName` in `app/build.gradle.kts`

## Android Studio Integration
- Project is configured for Android Studio
- Uses standard Android project structure
- Compose previews available in `MainActivity.kt`