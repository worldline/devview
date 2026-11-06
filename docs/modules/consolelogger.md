# Console Logger Module

Displays the app's native console output inside the DevView overlay — logcat on Android, a
`stdout`/`stderr` redirect on iOS — like viewing Logcat in Android Studio, with per-level
filter chips and a text filter.

## Overview

Native capture works on an **untethered** device — no debugger required — which is the
main requirement this module is built around: a tester installs a release build and opens
DevView without ever plugging into a computer.

| Log source | Android | iOS |
|---|---|---|
| `android.util.Log` (incl. third-party libs) | ✅ logcat tail | n/a |
| Kotlin `println` | ✅ (ART redirects stdout → logcat) | ✅ stdout pipe |
| Swift `print`, C `printf` | n/a | ✅ stdout pipe |
| `NSLog` / `os_log` / Swift `Logger` | n/a | ❌ **not capturable** |
| Kermit, via `DevViewLogWriter` | ✅ | ✅ (in-process, no debugger needed) |

The one real gap is `NSLog`/`os_log` on iOS: since iOS 10, `NSLog` only reaches `stderr`
when a debugger is attached, and `os_log`/Swift `Logger` bypass the file descriptor
entirely. Closing that gap fully would require reading the unified log via `OSLogStore`,
which needs an `@objc` Swift shim and cinterop wiring — out of scope for now. The practical
mitigation is `DevViewLogWriter` (see below): route your app's logging through Kermit and
it reaches the console reliably on both platforms, tethered or not.

Retained history is bounded (default 1000 entries, oldest evicted first) so a logcat-speed
firehose can't grow memory unbounded.

## Installation

```kotlin
dependencies {
    implementation("com.worldline.devview:devview-consolelogger:<version>")
}
```

## Core API

### `Console`

The DevView module:

```kotlin
public class Console(
    public val maxEntries: Int = ConsoleLogger.DEFAULT_MAX_ENTRIES,
    public val captureNativeConsole: Boolean = true,
    defaultExcludedPatterns: List<String> = Console.DEFAULT_EXCLUDED_PATTERNS
)
```

```kotlin
val modules = rememberModules {
    module(Console())
}
```

Set `captureNativeConsole = false` to disable the native tail and only show entries logged
manually via `ConsoleLogger.log()` or `DevViewLogWriter`.

### `ConsoleLogger`

The log sink, in the same shape as `AnalyticsLogger`:

```kotlin
public object ConsoleLogger {
    public fun log(log: ConsoleLog)
    public val logs: List<ConsoleLog>
    public val hasLogs: Boolean
}
```

`log()` is safe to call from any thread, including the background thread(s) driving native
capture. History is a read-only snapshot: read `ConsoleLogger.logs` in composition, not
inside `remember`. Submit new entries through `log()`; each recording gets a fresh internal
UUID, separate from value equality. `LocalConsoleLogs` has been removed.

Console content is internal and supplied by module registration. Render `DevView` and open
Console; do not call its screen directly. Search, levels, expanded rows and follow mode
survive saved-state restoration. Eviction and Clear prune expansion; filtering does not.
Capacity must be between 1 and `Int.MAX_VALUE - 1`; `Int.MAX_VALUE` would create an
unlimited channel and is rejected.

### Noise exclusions

The built-in defaults are `setRequestedFrameRate` and `setFrameRateCategory`. Matching is a case-insensitive substring
of either the tag or full message. Exclusions hide entries; they never drop captured history.

```kotlin
// Append to the built-in defaults.
module(Console(defaultExcludedPatterns = Console.DEFAULT_EXCLUDED_PATTERNS + "My noisy tag"))
// Replace defaults, or pass emptyList() to start without exclusions.
module(Console(defaultExcludedPatterns = listOf("My noisy tag")))
```

The bottom filter panel lets testers add, remove and enable patterns. Device additions,
removed defaults and disabled patterns persist in DataStore across restarts and app updates.
New host defaults merge without case-insensitive duplicates; removed defaults stay removed.
**Clear filters** disables rules without deleting them. **Reset to defaults** discards device
edits and restores the current host defaults. A fresh install starts from host defaults.
Loading or saving failures keep the saved configuration intact and offer Retry. Log entries
are in memory only; preferences contain rules, not log messages.

### `DevViewLogWriter`

An optional [Kermit](https://github.com/touchlab/kermit) `LogWriter` that forwards
structured log calls into the same sink:

```kotlin
Logger.addLogWriter(DevViewLogWriter)
```

This is the reliable way to see logs on an iOS device with no debugger attached. On
Android, using this **alongside** Kermit's own `LogcatWriter` will show each entry twice —
once via this writer, once via the native logcat tail picking up what `LogcatWriter` wrote.
Use one or the other for a given log call, or accept the duplication.

## Theming

Log-level colors (a debug teal, a warning yellow, an error red, etc.) are a fixed,
hand-tuned palette per theme, not derived from `MaterialTheme.colorScheme` — see the
[Theming guide](../guides/theming.md#console-log-level-colors) for how to provide and
override them.

## Usage

```kotlin
val modules = rememberModules {
    module(Console())
}

// Optional: route Kermit-based logging into the same view.
Logger.addLogWriter(DevViewLogWriter)
```

Opening DevView → Console shows newest-first compact rows with severity, time, tag and the first
message line. Tap a row to expand the complete message, preserving explicit newlines.
Long lines scroll horizontally; expanded text is selectable and **Copy log** copies the
header and full message. Native capture still emits physical lines as separate entries;
`DevViewLogWriter` preserves each complete multiline Kermit call. Native grouping and
broader capture-I/O hardening are deferred. Idle native readers now cancel
without waiting for another log line; Android terminates logcat and iOS restores redirected
output descriptors. A stalled original iOS output destination can still block the tee write.

Search stays at the bottom. Open **Filters** to choose visible levels; all are selected
initially, and selecting none shows no entries, with a clear recovery action. The collapsed
filter toggle counts hidden levels plus enabled noise rules. Query matching includes the tag and full
message, including text beyond the collapsed first line.

Scrolling vertically or expanding a row pauses follow mode. **Resume following latest log**
jumps to the newest matching row at the top, not the end of a long stack trace. While paused,
new arrivals keep the inspected row in place. Following also handles new entries when the
retained history is already at capacity.

The module owns a single collector/capture lifetime. Ordinary recomposition keeps it active;
removal cancels it, and re-entry drains pending entries. Reconfiguration preserves buffered
entries within the new limit and immediately trims retained history. Clear removes retained
and queued entries and prevents an already received older entry from reappearing.

## Sample

See `sample/shared/.../DevViewApp.kt` for a runnable end-to-end example, including
`LocalLogColorScheme` wired to the sample's dark-mode toggle.

## API Reference
> _[Dokka API Reference](../api/devview-consolelogger/index.html)_
