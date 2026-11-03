# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-crashhistory-core` is the engine behind the local crash history. It captures uncaught exceptions, accepts errors reported by the host app, imports what the OS recorded about earlier runs, and keeps a bounded list of `CrashRecord`s in app-private storage. It has no UI: `devview-crashhistory` (screens, copy/share/export) and future crash-SDK adapters depend on it.

Nothing is uploaded or redacted. Records are raw and only leave the device when the user shares them.

## Commands

```shell
.\gradlew.bat :devview-crashhistory-core:testAndroidHostTest "-Pandroidx.baselineprofile.skipgeneration"
```

Regenerate the public API signature after changing a public symbol:

```shell
.\gradlew.bat :devview-crashhistory-core:metalavaGenerateSignature "-Pandroidx.baselineprofile.skipgeneration"
```

`iosMain` compiles on Windows too (`compileKotlinIosSimulatorArm64`, klib only), but `iosTest` can only run on macOS: CI runs `compileKotlinIosArm64 iosSimulatorArm64Test` there.

## Public API Surface

| Class / Object | Role |
|---|---|
| `CrashRecorder` | Singleton. `@Composable install(maxRecords)` (called by `CrashHistory.initModule()`), `records: StateFlow<List<CrashRecord>>`, `record(...)` for handled errors / SDK adapters, `clear()`, size limit constants |
| `CrashRecord` | `@Serializable` record: id, timestamp, `kind`, exception type/message/stack trace, thread, process id, `source`, `isCrash`, `metadata`, `truncated` |
| `CrashRecordKind` | `UNCAUGHT_EXCEPTION` (in-process handler), `APP_REPORTED` (`CrashRecorder.record`), `SYSTEM_REPORTED` (Android `ApplicationExitInfo`, iOS MetricKit) |
| `CrashMetadata` | Platform, OS version, device model, app version, build number |
| `toReportText()` / `toJson()` | Formatters used for copy, share and export |
| `CrashHistoryIos` | iOS entry point: `start()` / `start(maxRecords)` from Swift, so crashes before the first composition are captured |

## Architecture

- `CrashRecorderEngine` holds all logic and is internal so tests can build isolated instances with a fake `CrashPlatform` and their own scope. `CrashRecorder` is a thin singleton over one engine.
- The engine is lock-free (`MutableStateFlow.update`, `AtomicBoolean`): the crash handler runs inside a dying process and must not block. Only the first crash of a process is written.
- `CrashStore` writes one JSON file per record (temp file + `atomicMove`) in `noBackupFilesDir` (Android) or Application Support (iOS), plus small state files (`enabled`, `exitinfo-cursor`, `metrickit-seen`). DataStore is deliberately not used: it is asynchronous and needs Compose to initialise.
- `CrashPlatform` is the platform seam: handler installation and history import. Android: chained `Thread` default handler + `ApplicationExitInfo` (API 30+). iOS: chained Kotlin unhandled-exception hook + `NSSetUncaughtExceptionHandler` + MetricKit (`MXCrashDiagnostic`, iOS 14+).
- Handlers always call the previously installed handler (Crashlytics, ...) after recording, and never throw.
- Android auto-start: `CrashHistoryInitProvider` (declared in this module's manifest, merged automatically) starts recording before `Application.onCreate`, but only when the `enabled` marker file written by `install()` exists, so merely having the dependency records nothing on first launch. The provider has no `initOrder` on purpose so Firebase's provider (100) runs first and its Crashlytics handler gets wrapped.

## Testing

- `commonTest`: report formatting, MetricKit JSON mapping (pure common code, so it runs on every host).
- `androidHostTest`: store, engine, handler chaining, `ApplicationExitInfo` mapping, init provider.
- `iosTest`: hook chaining invoked directly. Only runs on macOS.

## Detekt notes

`NamedArguments` allows 0 unnamed arguments: every call with an argument must name it, including stdlib calls (`take(n = ...)`, `abs(n = ...)`). Caught exceptions must be named `ignored` to pass `SwallowedException`.
