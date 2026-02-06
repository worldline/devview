# Development Setup
Guide for contributing to DevView.
## Prerequisites
- JDK 17 or higher
- Android Studio Hedgehog or newer
- Xcode 14+ (for iOS development)
- Git
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
See [Contributing Guidelines](index.md)
