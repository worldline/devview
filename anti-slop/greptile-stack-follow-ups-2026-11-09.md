# Greptile stack follow-ups

## Scope and decisions

Inventory snapshot: 2026-11-09, across PRs #157, #159, #160, #161, #163, #164, #165 and #166. Twelve inline Greptile comments contained findings; seven review bodies were empty. Two findings were already addressed in the stack. The owner approved changes for the remaining ten before implementation.

This follow-up is based on PR G, commit `df34c02`. It does not rewrite the earlier PRs or automatically resolve their review threads. New comments after the inventory snapshot are outside this record.

Confirmed behaviour:

- Built-in screens and ViewModels are internal. The owner corrected the initial public recovery proposal; `NetworkMockViewModel` is now internal, with no new public write/retry API. Registered UI owns the shared mutation consumer and its existing Retry/Cancel controls. Public loggers and core runtime repositories remain available.
- A newer choice for the same feature supersedes its older failed choice. Another feature's write does not discard that failure. Intent revisions distinguish an On/Off/On sequence from a retry of the first On.
- Console restores paused inspection by retained row key and pixel offset after saved noise rules load. An evicted or filtered anchor falls back to the newest matching row at zero offset, without resuming follow mode.

## Comment dispositions

| Comment | Disposition | Change and evidence |
|---|---|---|
| [#157: large previews create every row](https://github.com/worldline/devview/pull/157#discussion_r4483583191) | Already addressed | Finite-width lazy response panes were introduced earlier in `35c0740`. Existing 5,000-line preview tests remain in the passing NetworkMock device suite. No second lazy-list rewrite. |
| [#159: tab changes erase sequences](https://github.com/worldline/devview/pull/159#discussion_r4493794216) | Already addressed | Sequence editing is a navigation-owned full-screen destination with a saved draft, not a disposable animated sheet tab. Existing draft restoration and Cancel tests pass. Cancel still discards unapplied edits. |
| [#159: wide characters clipped](https://github.com/worldline/devview/pull/159#discussion_r4493794224) | Implemented | `devview-networkmock/src/commonMain/kotlin/com/worldline/devview/networkmock/components/MockResponseDiffContent.kt` measures every logical line's rendered width, retaining lazy vertical rows. A 5,000-line test places shorter-but-wider CJK/emoji text offscreen, verifies it is actually wider, and checks horizontal reach without visual overflow. |
| [#159: module pages miss API updates](https://github.com/worldline/devview/pull/159#discussion_r4493794230) | Implemented | Analytics, Console and FeatureFlip pages explicitly document current `name`, `subtitle` and `icon` identity properties and internal registered UI. The theming guide explains the boundary; historical `moduleName` wording is not copied back into the current API. |
| [#160: failed saves lose Retry](https://github.com/worldline/devview/pull/160#discussion_r4494051286) | Implemented | `devview-featureflip/src/commonMain/kotlin/com/worldline/devview/featureflip/FeatureFlipScreen.kt` retains failures per feature, with keyed recovery rows and per-intent revisions. Device tests cover independent retries, a newer choice dismissing only its own failure, and a suspended On/Off/On write sequence. |
| [#161: main setup example fails](https://github.com/worldline/devview/pull/161#discussion_r4494965834) | Implemented | `README.md` no longer uses removed `LocalAnalytics`; registration supplies analytics UI without a standalone provider. Existing event category names were checked against source. |
| [#163: restoration shows another log](https://github.com/worldline/devview/pull/163#discussion_r4496507785) | Implemented | `devview-consolelogger/src/commonMain/kotlin/com/worldline/devview/consolelogger/ConsoleScreen.kt` saves the paused anchor key and offset and resolves them only after authoritative noise preferences load. Device tests cover arrivals while disposed, delayed preferences, and a missing anchor with a newer excluded row. |
| [#164: saved expansion keeps growing](https://github.com/worldline/devview/pull/164#discussion_r4511986323) | Implemented | `devview-timecapsule/src/commonMain/kotlin/com/worldline/devview/timecapsule/TimeCapsuleScreen.kt` intersects live, saved and restored expansion IDs with retained history, preserving recording identity. Tests cover eviction, Clear, hidden-screen restoration and the live expansion state without saving. |
| [#165: public writes stop after failure](https://github.com/worldline/devview/pull/165#discussion_r4514010332) | Implemented through approved boundary correction | `devview-networkmock/src/commonMain/kotlin/com/worldline/devview/networkmock/viewmodel/NetworkMockViewModel.kt` is internal. Its standalone public constructor/API are removed from the generated signature file. Konsist now requires built-in ViewModels to be internal. Registered recovery retains existing queue Retry/Cancel behaviour. This is an intentional breaking API removal, not a new public recovery contract. |
| [#165: replaced mocks still fail requests](https://github.com/worldline/devview/pull/165#discussion_r4514010342) | Implemented | `devview-networkmock-ktor/src/commonMain/kotlin/com/worldline/devview/networkmock/ktor/plugin/NetworkMockPlugin.kt` rechecks revision on both exceptional and absent response loads. Superseded failures retry fresh state; still-current failures remain fail-closed. Host tests cover both failure paths across mock/sequence replacements, one probability roll and delay, atomic advancement, and cancellation without consuming the replacement sequence. |
| [#165: error buttons fall offscreen](https://github.com/worldline/devview/pull/165#discussion_r4514010359) | Implemented | Error content no longer implicitly consumes all remaining height. Detail and sheet callers reserve recovery controls while long messages scroll in the bounded content area; the full-screen list explicitly requests full size. Device tests assert visible Reload, Retry loading and Close controls and their callbacks in short layouts. |
| [#165: comparisons repeat during updates](https://github.com/worldline/devview/pull/165#discussion_r4514010364) | Implemented | The internal `rememberResponsePreview(primary, second)` factory retains the preview and its lazy diff across unrelated recompositions. A composition test verifies object/diff identity retention and invalidation when either body changes or comparison ends. The existing one-million-cell fallback is unchanged. |

## Implementation commits

All commits ran the normal gitleaks and Detekt hooks without bypassing them.

| Commit | Scope |
|---|---|
| `888103f` | NetworkMock recovery layout, Unicode width and retained comparison cache with device tests. |
| `9391bfa` | Superseded Ktor load failures, host coordination tests and interception documentation. |
| `c4ef6d8` | Internal ViewModel boundary, API signature removal, Konsist and integration documentation. |
| `b3d93bc` | Independent FeatureFlip recovery and intent revisions with device tests. |
| `0e806c8` | Paused Console anchor restoration with device tests. |
| `92eff3a` | Bounded TimeCapsule expansion with device tests. |

## Validation actually performed

| Check | Result |
|---|---|
| Registered clean Android host tests | 437 tests, zero failures/errors/skips. Some unchanged tasks used Gradle's cache; strengthened Ktor cases were executed in the preceding focused run. |
| Root serial connected Android device tests | 210 tests, zero failures/errors/skips, on the connected Android device. |
| Konsist | 15 tests, zero failures/errors/skips, including the internal ViewModel boundary. |
| Detekt | Passed. Compiler-analysis warnings remain for FeatureFlip, NetworkMock, TimeCapsule and sample KMP/generated sources; successful Kotlin compilation is not a claim that Detekt's semantic analysis is complete. |
| API signature check | Passed against the intentionally updated signature baseline. Seventeen `NetworkMockViewModel` signature lines were removed; this does not claim backward compatibility for that API. |
| Sample Android debug build | Passed. |
| Documentation | Dokka generation and versioned Zensical build passed using the Windows equivalent of `scripts/build_docs.sh`; source version placeholders were restored. |
| Local graph | AST update and recovery integrity checks passed: 5,668 nodes and 12,180 links. Generated Graphify output stays ignored and local; semantic wiki regeneration is not claimed. |

Device totals: DevView 25, Analytics 15, Console 23, FeatureFlip 14, NetworkMock 120, TimeCapsule 12 and utils 1. Host totals: DevView 15, Analytics 12, Console 33, FeatureFlip 13, NetworkMock 80, NetworkMock core 195, Ktor 46, TimeCapsule 39, utils 3 and sample 1.

Earlier device attempts exposed test-fixture defects: invoking a detached native semantics callback, trying to expand a non-expandable unchanged timeline entry, and querying a nonexistent preview tag. Those fixtures were corrected; the final complete device run passed. They are not reported as confirmed production defects.

Two read-only reviewers found no confirmed blocking production defect. Their coverage gaps prompted the stronger ABA, filtered-anchor, live-expansion, offscreen Unicode, comparison identity, and stale-load probability/delay/cancellation checks above.

## UI audit evidence and limits

This is a correctness follow-up to the [owner-approved scoped UI audit](report-001-2026-11-09.md), not a new visual design approval or a clean accessibility certificate. The original owner-maintained 52-item Antislop Delivery Gate worksheet remains the item-by-item record; its qualified and NOT VERIFIED entries are not converted to passes by these tests.

The existing sheet direction and ENERGY 1 / RHYTHM 2 / MOTION 1 are unchanged. Recovery layout changes serve reachable actions; rendered-width measurement serves readable inspector content. No new illustration, palette, motion system or unrelated screen redesign was added.

The owner previously deferred spoken TalkBack and further 200% font verification. iOS runtime, exhaustive keyboard/reduced-motion checks, all breakpoints and arbitrary host theme overrides remain unverified. Device assertions are evidence for the tested layouts, not independent pixel approval for every state.

PR G's observed Actions failure was a GitHub internal server error before jobs executed. The owner chose to wait, so no workflow edits, permission bypasses or reruns were made. Local validation does not establish remote CI success.
