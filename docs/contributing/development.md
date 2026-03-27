# Development Setup

Guide for contributing to DevView.

> _[Placeholder: Insert screenshot or diagram of the development environment setup. Use a device frame if relevant.]_

## Prerequisites
- JDK 17 or higher
- Android Studio Hedgehog or newer
- Xcode 14+ (for iOS development)
- Git
- Kotlin Multiplatform and Compose Multiplatform plugins

## Clone Repository
```bash
git clone https://github.com/worldline/devview.git
cd devview
```

## Build
```bash
./gradlew build
```

## Run Tests
```bash
./gradlew test
```

## Code Quality
```bash
./gradlew detekt
./gradlew ktlintCheck
```

## Generate Documentation
```bash
./gradlew dokkaHtml
```

## Platform-Specific Notes
### Android
- Minimum API level 21
- Use Android Studio for best Compose support

### iOS
- Minimum iOS 14.0
- Use Xcode 15+ and latest Compose Multiplatform plugin
- Run on simulator or real device

## Troubleshooting
- For build errors, check JDK and plugin versions
- For platform issues, verify toolchain and dependencies
- For documentation issues, ensure Dokka is installed and configured

## Next Steps
- See [Code Style](code-style.md) for formatting and linting rules
- Review [Pull Requests](pull-requests.md) for contribution process

---

_If you encounter issues not covered here, open an issue or start a discussion on GitHub._
