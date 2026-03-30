# MyApplication

Android application built with Kotlin and Jetpack Compose using modern Android development practices. The project follows a single-activity architecture with Compose UI and uses Material 3 design system.

## Features

- **Modern Architecture**: Modular architecture with app, core, and feature modules
- **Jetpack Compose UI**: Entire UI built with Compose using Material 3 design system
- **Dynamic Theming**: Supports dark/light mode and dynamic colors (Android 12+)
- **ML Kit Integration**: Camera feature with ML Kit for image analysis
- **Video Player**: Dedicated video player feature module
- **Single Activity**: Follows single-activity architecture best practices

## Tech Stack

- **Language**: Kotlin 2.3.20
- **UI Framework**: Jetpack Compose
- **Design System**: Material 3
- **Build System**: Gradle with Kotlin DSL
- **Dependency Management**: Version catalog (`gradle/libs.versions.toml`)
- **Minimum SDK**: 29 (Android 10)
- **Target SDK**: 36 (Android 16)
- **Java Version**: 11

## Project Structure

```
MyApplication/
├── app/                    # Main application module
│   ├── src/main/java/com/example/myapplication/
│   │   ├── MainActivity.kt          # Single activity with Compose UI
│   │   ├── entity/                  # Entity classes
│   │   └── ui/theme/               # Compose theme system
│   ├── src/main/res/               # Android resources
│   ├── src/test/                   # Unit tests (JUnit 4)
│   └── src/androidTest/            # Instrumented tests (AndroidJUnit4)
├── core/                          # Core functionality modules
│   └── mlkit/                     # ML Kit integration
├── feature/                       # Feature modules
│   ├── camera/                    # Camera feature
│   └── videoplayer/               # Video player feature
└── gradle/                       # Gradle configuration
    └── libs.versions.toml        # Version catalog
```

## Getting Started

### Prerequisites

- **Android Studio** (latest version recommended)
- **Android SDK** (API 29+)
- **Java 11** or higher

### Building the Project

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

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test variant
./gradlew testDebugUnitTest
```

### Running on Device/Emulator

```bash
# Install and run debug variant on connected device
./gradlew installDebug

# Install release variant
./gradlew installRelease
```

## Development

### Adding New Features

1. Create Compose functions in appropriate packages
2. Follow Material 3 design guidelines
3. Use the existing theme system (`MyApplicationTheme`)
4. Add tests in corresponding test directories

### Theme Customization

- Modify colors in `app/src/main/java/com/example/myapplication/ui/theme/Color.kt`
- Update typography in `app/src/main/java/com/example/myapplication/ui/theme/Type.kt`
- Theme supports dynamic colors on Android 12+ (enabled by default)

### Module Dependencies

- **app** → feature:camera, feature:videoplayer, core:mlkit
- **feature:camera** → core:mlkit
- **feature:videoplayer** → (standalone feature)

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgements

- Built with [Jetpack Compose](https://developer.android.com/jetpack/compose)
- Uses [Material 3](https://m3.material.io/) design system
- [ML Kit](https://developers.google.com/ml-kit) integration for camera features
- Gradle with [version catalog](https://docs.gradle.org/current/userguide/platforms.html) for dependency management