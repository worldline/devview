# Crash History Module

Keeps a short, local list of the crashes your app had on this device and shows it inside the
DevView overlay, so a tester can read the stack trace, copy it, or send it to you without
plugging into a computer.

It **complements** Crashlytics, Sentry or whatever you already use. It does not replace them:
nothing is uploaded, grouped, symbolicated or alerted on, and your crash SDK keeps receiving
every crash it received before.

## Overview

Two artifacts, so that other code (for example a future crash-SDK adapter) can use the
recorder without pulling in the feature screens or Material3/Navigation3:

| Artifact | Contains |
|---|---|
| `devview-crashhistory` | The `CrashHistory` module: list and detail screens, copy / share / export |
| `devview-crashhistory-core` | `CrashRecorder`, the record model and the platform capture. No screens; uses Compose runtime/UI for installation. Pulled in by `devview-crashhistory`. |

### What a record is

Every entry is a `CrashRecord` with a `kind`, a `source` and an `isCrash` flag. The `kind` says
how DevView learned about it, which tells you how much to trust it.

| `kind` | Meaning | Confidence |
|---|---|---|
| `UNCAUGHT_EXCEPTION` | Caught by the in-process uncaught-exception handler just before the process died | Confirmed, full stack trace |
| `APP_REPORTED` | Your code called `CrashRecorder.record(...)` (a handled error, or a crash SDK adapter) | As good as what you pass in |
| `SYSTEM_REPORTED` | The OS told us about an earlier run (Android `ApplicationExitInfo`, iOS MetricKit) | Reported by the OS, on a later launch |

`isCrash` is `false` for entries that are not crashes but explain why the app disappeared:
Android *low memory* and *excessive resource usage* kills. The UI labels these **Killed by
system** instead of **Crash**.

## Installation

```kotlin
dependencies {
    implementation("com.worldline.devview:devview-crashhistory:<version>")
}
```

```kotlin
val modules = rememberModules {
    module(module = CrashHistory())
}
```

That is the whole integration on Android. `CrashHistory()` installs the recorder the first time
it is composed.

### `CrashHistory`

```kotlin
public class CrashHistory(
    public val maxRecords: Int = CrashRecorder.DEFAULT_MAX_RECORDS
) : Module
```

It lives in the **Logging** section and shows a **Clear** action in the top bar.
`maxRecords` must be positive; zero and negative values throw `IllegalArgumentException`.
The latest `CrashHistory` initialization, `CrashRecorder.install(...)` or iOS `start(...)`
updates the process-wide limit without reinstalling handlers.

## Android

### Auto-start

Crashes that happen *before* DevView is first composed would normally be missed. To cover them,
`devview-crashhistory-core` declares a `ContentProvider` in its manifest (merged automatically,
nothing to add). It starts recording before `Application.onCreate`.

- It only records once the `enabled` marker file exists. The marker is written the first time
  `CrashHistory()` runs. Merely adding the dependency records nothing, and the very first launch
  after adding it is covered only from the moment DevView is composed.
- It has no `android:initOrder`. Firebase's `FirebaseInitProvider` uses `initOrder=100`, so it
  initialises first and Crashlytics' handler is installed before ours. We wrap it: record, then
  hand over to it.
- To opt out of the early start, remove the provider in your app manifest:

```xml
<provider
    android:name="com.worldline.devview.crashhistory.core.CrashHistoryInitProvider"
    android:authorities="${applicationId}.devview.crashhistory.init"
    tools:node="remove" />
```

### What is captured

| Android version | Captured |
|---|---|
| 8.0 - 10 (API 26-29) | In-process uncaught exceptions only |
| 11+ (API 30+) | The above, plus what `ApplicationExitInfo` reports about earlier runs: crashes, native crashes, ANRs (app not responding), initialization failures, low memory, excessive resource usage |
| 12+ (API 31+) | Native crash entries may carry a tombstone from the OS, which is a protobuf and is not rendered. It can be missing or already overwritten. |

For an ANR, the OS trace is attached as the stack trace when the system provides one.
Reading is bounded to the stack-trace limit, rather than loading the whole OS trace into memory.

DevView does not install its own signal handler, so it does not capture native crashes itself.
On Android 11+ the OS entry for a native crash is the only source.

## iOS

### Build and deployment requirements

Building the Swift bridge requires **Xcode 27 or newer** (including Gradle tasks that generate
iOS bindings, such as Metalava). The app must target **iOS 15 or newer**. Building with Xcode 27
does not require running on iOS 27: older supported versions use the legacy MetricKit API.
The library uses spmForKmp 1.9.7 and system MetricKit/CryptoKit, with no external Swift package.

### Optional early start

`CrashHistory()` is enough, but it only starts when DevView is first composed. To also record
crashes during app start-up, call `CrashHistoryIos.start()` from your app's entry point.

From Swift, export the core module from your shared framework:

```kotlin
// shared/build.gradle.kts
kotlin.sourceSets.commonMain.dependencies {
    api(projects.devviewCrashhistoryCore)
}
iosTarget.binaries.framework {
    export(projects.devviewCrashhistoryCore)
}
```

```swift
import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        CrashHistoryIos.shared.start()
    }

    var body: some Scene {
        WindowGroup { ContentView() }
    }
}
```

Or call it from Kotlin in `iosMain` and skip the export. `start(maxRecords)` is also available.

### What is captured

| Source | Captured |
|---|---|
| Kotlin unhandled exception | In-process, full stack trace, recorded before the app terminates |
| Objective-C / Swift `NSException` | In-process, with the symbols the runtime provides |
| MetricKit crash diagnostics (iOS 14+) | On a later launch. Real devices only, unsymbolicated. A Kotlin crash can show up here as `SIGABRT`, so it may duplicate the in-process record. |

Not captured: nothing watches for Swift `fatalError`, watchdog kills or out-of-memory kills
unless MetricKit reports them. A crash with no record is not proof there was no crash.

A Swift bridge selects the MetricKit API at runtime:

- **iOS 27+:** consumes `MetricManager.diagnosticReports` asynchronously and imports crash results.
- **iOS 15–26:** registers an `MXMetricManagerSubscriber` and replays available past diagnostics.

Imports use deterministic IDs derived from the reporting period, call stack, termination reason
and signal/Mach exception fields, so replaying a diagnostic does not add another record.
App/build/OS information comes from the diagnostic, not the current launch; unavailable values
are shown as `unknown`. MetricKit does not give an exact crash time, so its timestamp is labeled
**Reporting period end** in the detail screen and copied report.

### Kotlin/Native consumers and `publishSafe`

The bridge archive is embedded in the core klib. `publishSafe = true` removes producer-machine
linker search paths; it does not make a downstream Gradle-linked executable independent of its
own Xcode toolchain. A Swift host app supplies the Swift runtime linking automatically (as the
sample does); no generated local SPM package is needed for this bridge.

For a Gradle-linked Kotlin/Native binary or test consuming the core or UI module, supply the
selected Xcode's Swift runtime search directory as a `-L` linker option. Resolve it at link time,
not as a hard-coded path from the machine that published the library:

```kotlin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    val sdk = if (name == "iosArm64") "iphoneos" else "iphonesimulator"
    val swiftRuntime = providers.exec {
        commandLine("xcrun", "--sdk", sdk, "xcode-select", "-p")
    }.standardOutput.asText.map {
        "${it.trim()}/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/$sdk"
    }
    binaries.configureEach {
        linkTaskProvider.configure {
            toolOptions.freeCompilerArgs.addAll(
                swiftRuntime.map { listOf("-linker-option", "-L$it") }
            )
        }
    }
}
```

This is the lazy equivalent of `linkerOpts("-L<selected-Xcode>/Toolchains/" +
"XcodeDefault.xctoolchain/usr/lib/swift/<sdk>")`. Without it, native linking can fail with
`__swift_FORCE_LOAD_$_swiftCompatibility56` undefined symbols.

Kotlin/Native simulator test executables also need an iOS 15+ minimum, otherwise Swift concurrency
can fail to load at runtime even on a newer simulator. In each simulator target:

```kotlin
binaries.getTest("debug").freeCompilerArgs +=
    "-Xoverride-konan-properties=osVersionMin.ios_simulator_arm64=15.0"
```

## Handler chaining

DevView never takes over crash handling.

- It saves the handler that was installed before it (Crashlytics, Sentry, your own).
- When a crash happens it writes the record, then calls the saved handler.
- It never throws from the handler, and only the first crash of a process is written.
- Without a previous handler, Android falls back to killing the process, and iOS terminates
  with the unhandled exception, so a crash is never silently swallowed.

## Records and privacy

- **Storage.** One JSON file per record, written atomically, in app-private storage that is
  excluded from backup: `noBackupFilesDir/devview-crashhistory` on Android. On iOS, the
  Application Support directory is marked with `NSURLIsExcludedFromBackupKey` on each start
  and the value is read back. This is best-effort: filesystem failures log a warning rather
  than preventing app startup.
- **Retention.** The newest 20 records are kept (`CrashRecorder.DEFAULT_MAX_RECORDS`, or
  `CrashHistory(maxRecords = ...)`). The latest positive configuration wins; pruning runs
  during normal recording/startup, not inside the fatal handler.
- **Truncation.** Messages are cut at 2,000 characters and stack traces at 32,000
  (`MAX_MESSAGE_LENGTH`, `MAX_STACK_TRACE_LENGTH`). Cut records have `truncated = true`.
- **No redaction.** Records are raw, like Crashlytics' on-device data. Do not put secrets in
  exception messages. Redacting at share time belongs to the planned bug report feature.
- **Nothing is sent anywhere.** Data leaves the device only when someone taps **Copy**,
  **Share** or **Export**.
- **Clear.** The top bar action, or `CrashRecorder.clear()`, empties the visible list immediately
  and schedules disk deletion. Queued work from before Clear cannot repopulate it. Import
  cursors/seen IDs are retained so already imported OS history is not replayed. Persistence
  and fatal-handler writes are best-effort; a fatal write can race with deletion.
- Intended for debug, QA and internal builds, like the rest of DevView.

## Copy, share and export

The detail screen has three buttons:

| Button | Does |
|---|---|
| Copy | Puts the human-readable report on the clipboard |
| Share | Opens the system share sheet with the report as text |
| Export | Writes a JSON file and opens the share sheet with it (email, chat apps, Save to Files) |

The list screen has **Export all**, which exports every record as one JSON file.

On Android, files are shared through a `FileProvider` that the module declares itself, so it
cannot clash with a `FileProvider` of your own. The exported file is written to
`cacheDir/devview-crashhistory-export` with a unique name for each export, so a later export
cannot overwrite an earlier recipient's attachment. Files older than one hour are removed on
the next export (the OS can also clear the cache). Failed file writes or unavailable share
handlers show **Could not export crash history** on the list, or **Could not share crash report**
on the detail screen, instead of crashing the app.

iOS checks file-write success before presenting the share sheet and anchors popovers on iPad.

## Bring your own crash SDK

Report errors your crash SDK or your own code handles:

```kotlin
CrashRecorder.record(throwable = exception)

CrashRecorder.record(
    exceptionType = "com.acme.PaymentError",
    message = "Card declined",
    stackTrace = trace
)
```

Records created this way have kind `APP_REPORTED`. If you call them before DevView has
installed the recorder, they are buffered (up to 20) and written once it starts.

Crashlytics cannot be used as a source: it has no public API to read crashes back. A Sentry
adapter could be built on top of `CrashRecorder.record(...)`. It is not part of v1, and would
ship as a separate `devview-crashhistory-sentry` module.

## Core API

```kotlin
public object CrashRecorder {
    public const val DEFAULT_MAX_RECORDS: Int = 20
    public const val MAX_MESSAGE_LENGTH: Int = 2_000
    public const val MAX_STACK_TRACE_LENGTH: Int = 32_000

    public val records: StateFlow<List<CrashRecord>>

    @Composable
    public fun install(maxRecords: Int = DEFAULT_MAX_RECORDS)

    public fun record(throwable: Throwable)
    public fun record(
        exceptionType: String,
        message: String?,
        stackTrace: String?,
        timestampMillis: Long = Clock.System.now().toEpochMilliseconds()
    )

    public fun clear()
}

public fun CrashRecord.toReportText(): String
public fun List<CrashRecord>.toJson(): String
```

`CrashHistory.initModule()` calls `install`, so you only call it yourself if you use the core
module without the UI.

## Limits at a glance

- Not a replacement for a crash reporting service: no upload, grouping, symbolication or alerts.
- Native, signal and OS-level crashes are not captured by DevView itself. They appear only
  through `ApplicationExitInfo` (Android 11+) or MetricKit (iOS 14+).
- Without the OS sources, a crash that kills the process before any handler is installed
  leaves no record.
- Android 8-10 has no abnormal-exit detection.
- MetricKit data arrives on a later launch and may duplicate an in-process record.
- Same-period MetricKit diagnostics with identical stacks and exception fields are
  indistinguishable to the available data and share an import ID.

## Sample

The sample app has a **Crash app** button, which throws an exception, and a **Report caught
exception** button, which calls `CrashRecorder.record`.

## API Reference

> _[Dokka API Reference](../api/devview-crashhistory/index.html)_ and
> _[core](../api/devview-crashhistory-core/index.html)_
