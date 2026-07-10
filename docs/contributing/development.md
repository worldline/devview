# Development Setup

Guide for setting up a local environment to contribute to DevView.

## Prerequisites

- JDK 17 or higher
- Android Studio Ladybug or newer
- Git
- [pre-commit](https://pre-commit.com/#install) (`pip install pre-commit` or `brew install pre-commit`)

> iOS development additionally requires Xcode 15+ and a Mac.

## Clone and sync

```bash
git clone https://github.com/worldline/devview.git
cd devview
```

Open the project in Android Studio and let Gradle sync complete. On first sync, the `preCommitCheck` Gradle task runs automatically and installs the pre-commit hooks into your local `.git/hooks/`. This means **the hooks are set up for you** — no manual `pre-commit install` needed.

If you prefer the command line or need to reinstall the hooks manually:

```bash
pre-commit install
```

## Pre-commit hooks

Two hooks run on every `git commit`:

| Hook | What it checks |
|---|---|
| **gitleaks** | Scans staged files for accidentally committed secrets |
| **detekt** | Runs the full Detekt + ktlint analysis across all modules |

If either hook fails, the commit is blocked. Fix the reported issues and commit again.

To run the hooks manually without committing:

```bash
pre-commit run --all-files
```

## Build

=== "Windows"

    ```bat
    .\gradlew.bat :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew :sample:androidApp:assembleDebug -Pandroidx.baselineprofile.skipgeneration
    ```

## Run tests

Unit and host tests (no device required):

=== "Windows"

    ```bat
    .\gradlew.bat cleanTestAndroidHostTest testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew cleanTestAndroidHostTest testAndroidHostTest -Pandroidx.baselineprofile.skipgeneration
    ```

Instrumented tests (requires a connected emulator or device):

=== "Windows"

    ```bat
    .\gradlew.bat connectedAndroidDeviceTest -Pandroidx.baselineprofile.skipgeneration
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew connectedAndroidDeviceTest -Pandroidx.baselineprofile.skipgeneration
    ```

## Code quality

Run the full Detekt + ktlint analysis across all modules:

=== "Windows"

    ```bat
    .\gradlew.bat detektFull
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew detektFull
    ```

Architecture rules (Konsist):

=== "Windows"

    ```bat
    .\gradlew.bat :konsist:test
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew :konsist:test
    ```

## Generate API docs

=== "Windows"

    ```bat
    .\gradlew.bat :internal:dokka:dokkaGenerate -Pandroidx.baselineprofile.skipgeneration
    ```

=== "macOS / Linux"

    ```bash
    ./gradlew :internal:dokka:dokkaGenerate -Pandroidx.baselineprofile.skipgeneration
    ```

## Next steps

- See [Code Style](code-style.md) for formatting and linting rules
- Review [Pull Requests](pull-requests.md) for the contribution process

---

_If you encounter issues not covered here, open an issue or start a discussion on GitHub._
