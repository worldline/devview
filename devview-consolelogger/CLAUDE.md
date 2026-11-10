# Console Logger

## Purpose

In-app console inspection on Android and iOS. Native capture works without a debugger;
see `docs/modules/consolelogger.md` for the capture matrix. iOS `NSLog`/`os_log`/Swift
`Logger` are not captured without a debugger; Kermit's `DevViewLogWriter` is the supported
in-process alternative. Do not promise unified-log capture.

## Public surface

- `Console(maxEntries = 1000, captureNativeConsole = true, defaultExcludedPatterns = Console.DEFAULT_EXCLUDED_PATTERNS)` is the module. It implements RequiresDataStore for noise preferences, not log storage.
- `ConsoleLogger.log(ConsoleLog)` is safe from any thread and records a fresh internal UUID.
- `ConsoleLogger.logs: List<ConsoleLog>` returns a read-only observable snapshot. Read it
  in composition, not in `remember`. Mutation stays private; use `log()`.
- `ConsoleLogger.hasLogs` reports retained history.
- `ConsoleLog(level, tag, message, timestamp)` keeps identity outside value equality and
  constructor/copy arguments.
- `DevViewLogWriter` forwards Kermit calls including complete throwable messages. Register
  it once. Android native capture plus Kermit `LogcatWriter` can produce duplicate records;
  do not deduplicate equal messages because they can be distinct calls.

## Collector lifecycle

`Console.initModule()` uses an effect keyed by capacity and native-capture setting.
`rememberModules` keeps initialization effects in composition, with once-only DataStore
setup. Removal cancels ongoing effects; re-entry starts them again.

`ConsoleLogger.collectInto` holds a coroutine Mutex across the entire collector/capture
lifetime, including cancellation cleanup. Configuration happens after ownership acquisition.
The drain uses one local channel reference. The non-suspending `ConsoleLock` protects queue
handoff, enqueue, clear and append: an Android monitor and iOS recursive Foundation lock.
Do not hold this lock across suspension. Recursive locking permits inline coroutine resumes
from channel operations without deadlocking a same-thread logger callback.

The queue and retained history are both bounded/drop-oldest. Same-capacity setup preserves
the queue; changing capacity migrates pending entries and trims history. Clear increments a
private generation, drains pending entries and clears history, so stale received entries cannot
reappear afterward. Cancellation during delivery still follows the channel's bounded,
best-effort contract; this is not lossless log storage.

## UI

Search and a collapsible filter panel use the public DevView UI kit. All severity levels are
visible initially; hiding all produces a recoverable empty state. The toggle counts hidden
levels plus enabled noise rules. Query, levels, panel, expanded row IDs and follow mode use saved state.

`ConsoleScreen` is internal and owns persistence and rendering. Registration initializes the
shared DataStore delegate before opening it. Rules match case-insensitive tag/message
substrings without discarding captured logs. Three preference sets store additions, removed
defaults and disabled identities; new host defaults merge without duplicates, and removed
defaults stay removed. Clear filters disables rules; Reset discards edits. One ordered pending
queue writes atomically against current preferences. Read/write IOException shows Retry and
does not replace saved configuration with fallback values.

Rows use UUID plus duplicate-occurrence rank for safe explicit lists. Duplicate expansion
is positional within each ID; remove an earlier identical occurrence and expansion may
transfer. Eviction/Clear prune expansion against the full source, not filtered results. Collapsed messages show
one ellipsized line and an animated chevron. The collapsed row toggles expansion; an expanded
row only toggles from its header so selection and Copy remain independent. Messages retain newlines, scroll horizontally, support selection
and copying. Expansion or vertical user scrolling pauses follow. Resume aligns the newest
matching row's start at the top, including full-capacity history replacements. Paused arrivals
keep the inspected row in place. Assign duplicate ranks oldest-first, then reverse rows and keys.

`LogColorScheme` remains a semantic light/dark palette independent of the host hue. Labels
have contrast tests; layout and typography inherit MaterialTheme. `LocalLogColorScheme` is
optional and falls back based on surface luminance.

## Native boundary

`tailNativeConsole()` is expect/actual. Android tails per-process brief logcat; iOS redirects
stdout/stderr with POSIX pipes and tees bytes to the original descriptors. Native capture
currently records physical lines, not grouped traces. Android cancellation terminates logcat
before waiting for the blocked reader; iOS uses nonblocking reads with cancellable idle waits
and restores descriptors on exit. Native grouping, partial writes and streaming UTF-8 handling
are deferred. A stalled original iOS output destination can still block the tee write. Complete multiline messages are reliable through
Kermit's in-process writer. Native failure handling must not cancel manual-log draining.

## Tests

Host tests cover recording identity, immutable snapshots, queue migration/overflow, history
eviction, clear, serialized collector re-entry, metadata, filters, noise preference merging,
removal/reset/restart, parser/writer and palettes.
Android device tests cover query/levels/recovery/restoration, expanded copy, full-capacity
follow, explicit-source replacement, duplicate objects and themes. Core registry tests cover
module-effect recomposition, removal and re-entry. iOS execution requires macOS/device;
Windows checks common metadata, while the macOS CI job compiles and tests Apple targets.
