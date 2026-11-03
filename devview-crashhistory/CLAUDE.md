# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-crashhistory` is the DevView module that shows the local crash history: a list of recorded crashes, a detail screen, and Copy / Share / Export buttons. Capture, storage and limits live in `devview-crashhistory-core` (`CrashRecorder`); this module is only the UI and the platform sharing glue.

Nothing is uploaded. Data leaves the device only when the user taps Copy, Share or Export.

## Commands

```shell
.\gradlew.bat :devview-crashhistory:testAndroidHostTest "-Pandroidx.baselineprofile.skipgeneration"
.\gradlew.bat :devview-crashhistory:connectedAndroidDeviceTest "-Pandroidx.baselineprofile.skipgeneration"
.\gradlew.bat :devview-crashhistory:metalavaGenerateSignature "-Pandroidx.baselineprofile.skipgeneration"
```

## Public API Surface

| Class | Role |
|---|---|
| `CrashHistory` | `Module` in the `LOGGING` section. `maxRecords` constructor parameter. `initModule()` calls `CrashRecorder.install(maxRecords)`, so registering the module starts recording |
| `CrashHistoryDestination` | `Main` (list) and `Detail(id)` navigation keys |

## Architecture

- `CrashListScreen` and `CrashDetailScreen` are stateless (they take records as parameters) so device tests do not touch the `CrashRecorder` singleton. `CrashHistory.registerContent` collects `CrashRecorder.records` and passes them in.
- Copy / Share / Export buttons are inside the screens, not top-bar actions: a top-bar action cannot see the destination id or the platform context needed to share.
- `CrashSharer` (internal, `expect fun rememberCrashSharer()`):
  - Android: `ClipboardManager`, `ACTION_SEND` chooser. Files are written to `cacheDir/devview-crashhistory-export/` and shared through `CrashHistoryFileProvider`, declared in this module's manifest with the authority `${applicationId}.devview.crashhistory.fileprovider` so it merges automatically and cannot clash with the host's own provider.
  - iOS: `UIPasteboard` and `UIActivityViewController` on the top-most presented controller. The popover anchor is set so iPad does not crash. The export file goes to `NSTemporaryDirectory()`.
- `CrashKindChip` colours the record kind: red for confirmed crashes, tertiary for app-reported, neutral for "killed by the system".

## Testing

- `commonTest`: module metadata, destinations, kind labels.
- `androidDeviceTest`: list / detail screens with the `crash_*` test tags.
- The sharers (clipboard, share sheet) are not covered by automated tests: check them by hand on a device.

## Detekt notes

`NamedArguments` allows 0 unnamed arguments: every Kotlin call with an argument must name it, including stdlib calls (`add(element = ...)`). Avoid implicit `it` in nested lambdas (`NoNameShadowing`).
