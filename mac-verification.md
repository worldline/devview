# Mac verification

## Handoff D

- Date: **2026-11-05**; branch: `feat/crash-history`.
- Commit tested: **7975192134dec8de4a8fb29248879a8096d28c2a** (`7975192`). `git pull --ff-only` fast-forwarded `95fffa1` to this baseline, which deleted the previous verification document; this report is newly created as requested.
- Read `CLAUDE.md` and the spmforkmp skill. No production fixes, public API changes, permanent instrumentation, injection APIs, CI changes or roadmap changes.
- Selected toolchain: `/Applications/Xcode.app/Contents/Developer`; **Xcode 27.0, build 27A266a**.
- Available runtimes: iOS **26.5 (23F77)** and **27.0 (24A434)**. Only **iOS 27.0** was used for this handoff's tests and sample smoke/UI checks.
- Selected simulator: **iPhone 18 Pro**, runtime `com.apple.CoreSimulator.SimRuntime.iOS-27-0`, **UDID `1FB16D38-DA93-47B8-A57A-AB6F38B65DC9`**. Booted with `simctl boot` and waited with `bootstatus -b`.

### Results

| Check | Result | Evidence |
| --- | --- | --- |
| Select/boot iOS 27 simulator | **PASS** | simctl device JSON maps the exact UDID to iOS 27.0; screenshots also show `Hello, iOS 27.0!` and crash detail device metadata `iPhone, iOS 27.0`. |
| Core iosSimulatorArm64Test | **PASS on iOS 27.0** | 12 tests, zero failures/errors/skips; forced test execution with `--rerun`, explicit `--device` UDID. |
| UI-module iosSimulatorArm64Test | **PASS on iOS 27.0** | 4 tests, zero failures/errors/skips; same explicit UDID. These unit tests are separate from the interactive UI checks below. |
| Sample framework link | **PASS** | Initial BUILD SUCCESSFUL in 13s; final uninstrumented rebuild BUILD SUCCESSFUL in 32s. |
| Sample Xcode build | **PASS** | Initial, temporary-instrumented and final uninstrumented builds all report BUILD SUCCEEDED; destination uses the exact UDID. |
| Install/launch/live PID | **PASS on iOS 27.0** | Temporary-instrumented PID 39236 remained alive; final uninstrumented PID **39978** remained alive after 10 seconds and appeared in simctl launchctl list. |
| iOS 27 MetricKit branch and Task/loop setup | **PASS** | Runtime logs from PID 39236 show branch entry and Task creation of MetricManager plus evaluation of diagnosticReports immediately before its for-await loop; MetricKit XPC connection also activated. Details below. |
| Startup / Swift concurrency / dyld / serialization errors | **PASS for observed window** | No `Library not loaded`, `dyld[`, Swift fatal/error or `DevView: could not serialize MetricKit crash` matches in collected app logs. App-launch measurement warnings were non-fatal. |
| Report caught exception / entry / details | **PASS** | Tapped sample button; list shows `Reported`, `kotlin.IllegalStateException`, `DevView sample handled error`; detail shows process 39978, app 1.0 (1), iOS 27.0 and stack trace. |
| Copy | **PASS** | Tapped Copy; simctl pbpaste contains `DevView sample handled error` and stack trace. |
| Share | **PASS** | Correctly presented text share sheet; screenshot and UIActivityViewController `ready to interact` / `view did appear` logs. Process survived. No external recipient transfer tested. |
| Export | **PASS** | Correctly presented JSON-file sheet; file `devview-crash-39978-8a0cbdfb2b8cd4dd.json`, 5434 bytes, parses as JSON and contains handled error. No external Files destination save tested. |
| Crash app / termination | **PASS** | Tapped Crash app; PID **41041** terminated as expected. This deliberate sample crash is not a startup failure. |
| Relaunch without reinstall / persisted uncaught crash | **PASS** | Relaunched PID **41105**, alive after 10 seconds. List contains both `Crash: DevView sample crash` and the earlier Reported entry (`Export all (2)`). No install command between crash and relaunch. |
| Clear / relaunch / entries stay gone | **PASS** | Confirmed Clear in confirmation dialog; relaunched without reinstall, PID **41457** alive after 10 seconds. List shows `Crashes will appear here`; persisted crash JSON count is zero. An earlier dialog-only attempt did not clear, as expected. |
| Real MetricKit diagnostic delivery | **NOT OBSERVED / UNVERIFIED** | No diagnostic report delivery observed. Async subscription smoke evidence is not OS diagnostic delivery evidence; local Reported/Crash entries came from the sample recorder buttons, not MetricKit. |
| Injected diagnostic simulation | **SKIPPED** | No supported simulation used; no fabricated report or injection API added. |

### Commands

```sh
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
git pull --ff-only
git rev-parse HEAD
xcodebuild -version
xcrun simctl list runtimes
xcrun simctl list devices available -j
SIM=1FB16D38-DA93-47B8-A57A-AB6F38B65DC9
xcrun simctl boot "$SIM"
xcrun simctl bootstatus "$SIM" -b

./gradlew help --task :devview-crashhistory-core:iosSimulatorArm64Test
./gradlew :devview-crashhistory-core:iosSimulatorArm64Test --device "$SIM" --rerun \
  :devview-crashhistory:iosSimulatorArm64Test --device "$SIM" --rerun \
  -Pandroidx.baselineprofile.skipgeneration --info
./gradlew :sample:shared:linkDebugFrameworkIosSimulatorArm64
xcodebuild -project sample/iosApp/iosApp.xcodeproj -scheme iosApp \
  -sdk iphonesimulator -destination "platform=iOS Simulator,id=$SIM" build

APP=/Users/w112008/Library/Developer/Xcode/DerivedData/iosApp-gfbutbzsvanhntdqfhjhaynmehjo/Build/Products/Debug-iphonesimulator/DevView.app
xcrun simctl install "$SIM" "$APP"
xcrun simctl launch --terminate-running-process "$SIM" com.worldline.devview.DevView
# Parse PID from launch output, require PID > 0, wait and verify:
PID=39978
test "$PID" -gt 0
sleep 10
kill -0 "$PID"
xcrun simctl spawn "$SIM" launchctl list | grep "$PID"
xcrun simctl spawn "$SIM" log show --last 12m --style compact \
  --predicate 'process == "DevView"'
xcrun simctl pbpaste "$SIM"
xcrun simctl io "$SIM" screenshot /tmp/devview-handoff-d/detail.png

# After tapping Crash app, relaunch WITHOUT reinstalling:
xcrun simctl launch "$SIM" com.worldline.devview.DevView
# After confirming Clear, terminate/relaunch WITHOUT reinstalling:
xcrun simctl launch --terminate-running-process "$SIM" com.worldline.devview.DevView
```

Both simulator test tasks support the installed Kotlin Gradle Plugin's `--device` task option (confirmed by Gradle help and KGP 2.3.21 implementation). Each task receives the option separately. `--info` confirms actual execution, not cached older-runtime results:

```text
/usr/bin/xcrun simctl spawn --standalone 1FB16D38-DA93-47B8-A57A-AB6F38B65DC9 /Users/w112008/Documents/Projects/devview/devview-crashhistory-core/build/bin/iosSimulatorArm64/debugTest/test.kexe -- --ktest_no_exit_code --ktest_logger=TEAMCITY
/usr/bin/xcrun simctl spawn --standalone 1FB16D38-DA93-47B8-A57A-AB6F38B65DC9 /Users/w112008/Documents/Projects/devview/devview-crashhistory/build/bin/iosSimulatorArm64/debugTest/test.kexe -- --ktest_no_exit_code --ktest_logger=TEAMCITY
BUILD SUCCESSFUL in 10s
```

### Async branch evidence and instrumentation removal

Temporarily logged inside the iOS 27 availability branch. Inside the existing Task, evaluated `let reports = manager.diagnosticReports`, logged immediately afterwards, and iterated `for await report in reports`. No diagnostics were injected and the loop body was unchanged.

```text
2026-11-05 21:25:18.007 Df DevView[39236:58a83] (Foundation) Handoff D temporary: entered iOS 27 branch
2026-11-05 21:25:18.147 Df DevView[39236:58a82] [com.apple.xpc:connection] [0x10162cb40] activating connection: mach=true listener=false peer=false name=com.apple.metrickit.xpc
2026-11-05 21:25:18.147 Df DevView[39236:58a82] (Foundation) Handoff D temporary: Task obtained MetricManager.diagnosticReports; entering for-await loop
```

This proves the Task ran and obtained the async report sequence at the loop; it does **not** prove a loop-body iteration/report receipt. No report was observed. All temporary changes were removed byte-for-byte; `git diff --exit-code -- devview-crashhistory-core/src/swift/crashHistoryBridge/MetricKitBridge.swift` passed. Rebuilt framework/app, installed and launched the uninstrumented baseline successfully. All interactive Crash History checks used that final uninstrumented build.

### Interactive evidence

macOS System Events UI access initially failed, then succeeded after activating Simulator and retrying. No permission settings or app instrumentation changed. Used `osascript` coordinate clicks against the Simulator window (device viewport origin approximately 1003,114; size 350x761), checking screenshots after transitions. Representative clicks: Report caught exception (1225,552), Open DevView (1172,245), CrashHistory (1120,453), detail (1140,335), Copy (1047,259), Share (1125,259), Export (1205,259), Crash app (1060,552), trash (1332,196), confirmation Clear (1270,556). Coordinates are local display coordinates, not portable automation selectors.

```sh
osascript -e 'tell application "Simulator" to activate' \
  -e 'delay 1' -e 'tell application "System Events" to click at {1225, 552}'
```

Screenshots under `/tmp/devview-handoff-d/`: `history.png`, `detail.png`, `share-ready.png`, `export-ready.png`, `post-crash-history.png`, `cleared.png`, `confirmed-clear-history.png`. Clipboard text in `copied.txt`. Share logs show `UIAVC: ready to interact` and `UIAVC: view did appear`. The screenshot immediately after Share was taken before presentation completed; the later `share-ready.png` confirms the sheet. Clear requires confirmation; the initial relaunch while the confirmation dialog was open correctly preserved records; after confirmed Clear they stayed gone.

### Failure tails / limitations / commits

- Build, test and final launch commands: **no failures**, so no failed build tails.
- Initial UI automation failures (full outputs were shorter than 30 lines):

```text
117:139: execution error: System Events got an error: osascript is not allowed assistive access. (-1728)
36:56: execution error: System Events got an error: osascript is not allowed assistive access. (-10000)
```

Both recovered on retry; not library regressions. An export attempt interrupted by the permission error also produced `find: .../tmp/devview-crashhistory-export: No such file or directory`; the successful retry created and presented valid JSON.

- Non-fatal system warnings: `Failed to send CA Event for app launch measurements` for FirstFramePresentationMetric and ExtendedLaunchMetrics. No startup/concurrency/dyld/MetricKit serialization failures observed.
- Real OS MetricKit crash delivery remains unverified. Sample-button crash persistence is recorder/handler evidence only. External recipient transfer, saving to Files and iPad sheet anchoring were not exercised (**SKIPPED**).
- Fix commits: **none required**. Results-only gitmoji commit creates this report. No changes to `DEVELOPER-TOOLING-ROADMAP.md`, build setup, CI or graph data; restored the graph query's unrelated cache timestamp.
- Full command logs and screenshots are retained locally under `/tmp/devview-handoff-d/`; this document includes the key runtime evidence and failure outputs.
