# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Module Does

`devview-timecapsule` records the state history of whichever screen is currently visible
under the DevView overlay, and lets a developer restore any earlier state back into that
screen from the DevView UI. It tracks exactly one screen at a time and resets whenever
that screen leaves composition (the host app navigates away). Nothing is persisted to
disk — the history is in-memory only, for the lifetime of the recording composable.

## Public API Surface

| Symbol | Kind | Description |
|---|---|---|
| `TimeCapsuleOwner<S>` | interface | Contract implemented by a screen's state holder: `state: StateFlow<S>` and `fun restoreState(state: S)` |
| `TimeCapsuleEffect(owner, label, subtitle, maxEntries)` | `@Composable` | Records `owner.state` for as long as it stays in composition; registers/unregisters with `TimeCapsule` via `DisposableEffect`. `subtitle` names the recorded screen in the Time Capsule header, defaulting to `owner`'s class name |
| `TimeCapsule` | `object : Module` | Module entry point; registered with no arguments, like `FeatureFlip` |
| `TimeCapsule.DEFAULT_MAX_ENTRIES` | `const Int` | Default retention (50) when `TimeCapsuleEffect` doesn't specify `maxEntries` |
| `TimeCapsuleDestination.Main` | `@Serializable data object` | Only navigation destination; title "Time Capsule" |

`ScreenCapsule<S>` and `Recorded<S>` (the recorder and its entry model) are `internal` —
all screen recording and retention logic lives there, but integrators only ever touch
`TimeCapsuleOwner` and `TimeCapsuleEffect`.

## Internal Architecture

```
TimeCapsuleEffect(owner, label, subtitle, maxEntries)
    ├─ remember { ScreenCapsule(owner, label, maxEntries, subtitleOverride = subtitle) }
    ├─ DisposableEffect: TimeCapsule.register(capsule) → onDispose { TimeCapsule.unregister(capsule) }
    └─ LaunchedEffect: owner.state.collect(capsule::record)

TimeCapsule (object : Module)
    ├─ activeCapsules: SnapshotStateList<ScreenCapsule<*>>
    ├─ current: ScreenCapsule<*>? = activeCapsules.lastOrNull()
    └─ TimeCapsuleScreen reads `current` directly (no CompositionLocal — this module
       has exactly one consumer of the registry, its own screen)

ScreenCapsule<S>
    ├─ recordedEntries: SnapshotStateList<Recorded<S>>
    ├─ subtitle: String? = subtitleOverride ?: owner::class.simpleName
    ├─ record(state): appends, drops oldest at maxEntries
    ├─ restore(id): owner.restoreState(entry.state) — no cast, entry is typed S
    └─ clear()
```

## Why a List, Not a Single Slot, for the Active Registry

During a host-app navigation transition the incoming screen's `TimeCapsuleEffect` can
compose before the outgoing screen's disposes. A single "current capsule" slot would have
the outgoing screen's `onDispose` null it out right after the incoming screen set it. The
registry is a list instead: the outgoing capsule is removed from wherever it sits, and
`current` (`lastOrNull()`) still resolves to whichever registered most recently. See
`TimeCapsuleTest` for the ordering test this protects.

## Non-Obvious Patterns

- **Dedup and initial-value replay are `StateFlow`'s job, not `ScreenCapsule`'s.**
  `record()` is a plain function that always appends when called. The "record the current
  value on subscribe, skip consecutive equal values" behaviour comes for free from
  collecting `owner.state` (a `StateFlow`) in `TimeCapsuleEffect` — `ScreenCapsule` itself
  has no dedup logic and shouldn't need any.
- **Restoring re-records.** `ScreenCapsule.restore()` calls `owner.restoreState(...)`,
  which (if the owner routes it back through the same `StateFlow`) is observed by the same
  `LaunchedEffect` and recorded as a new entry. This is intentional — a restore is a state
  transition — but it does mean repeated restores grow the timeline. Marked with a
  `ponytail:` comment in `ScreenCapsule.kt`.
- **No CompositionLocal.** Unlike `devview-analytics`'s `LocalAnalytics`, `TimeCapsule`'s
  registry is read directly from the object inside `TimeCapsuleScreen`. There's exactly
  one consumer (the module's own screen) and no host-app use case for reading the registry
  elsewhere, so the extra indirection isn't justified here.
- **Row shows both a delta and a wall-clock timestamp.** `TimeCapsuleRow` shows the time
  elapsed since the previous entry as a pill (`+120ms`, or `initial` for the oldest entry)
  and the wall-clock time (`HH:mm:ss`, via `kotlinx.datetime` — this module's only consumer
  of that dependency). They're rendered as visually separate elements (timestamp left, pill
  right) rather than joined with `·`, which read ambiguously as fractional seconds
  (`13:12:07 · +120ms` looked like `13:12:07.120`).
- **The row's headline is a change summary, not the raw label.** `StateDiff.kt` parses a
  `key=value`-shaped label (what `toString()` produces on a data class) and diffs it against
  the previous entry's label, so the row reads `count 3 → 4` instead of the full
  `CounterState(count=4, ...)`. A label with no parseable `key=value` pairs falls back to
  showing itself verbatim, unchanged from the original behavior. Expanding a row reveals the
  full label below a divider, with the changed values highlighted — this is also the only way
  to read a label that's been truncated at two lines.
- **Header subtitle defaults to the owner's class name.** `TimeCapsuleEffect`'s `subtitle`
  parameter is shown as "Recording {subtitle}" above the timeline, so a tester can confirm
  which screen they're looking at. Defaulting to `owner::class.simpleName` (rather than
  `null`) means most integrators get this for free; `simpleName` is `null` for an anonymous
  class, which integrators must override explicitly in that case.

## Platform-Specific Code

There is no `androidMain` or `iosMain` source set in this module — all source lives in
`commonMain`. There are no `expect`/`actual` declarations here.
