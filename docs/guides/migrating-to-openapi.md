# Migrating from mocks.json to OpenAPI

DevView 0.2.0 replaces the bespoke `mocks.json` network-mock format with OpenAPI 3.x and
removes the environment axis entirely. This is a **big-bang breaking change** — there is
no dual-format transition period, so every `mocks.json` stops working the moment you
upgrade. This guide covers what changed, why, and how to convert an existing config.

For the rationale in full, see the [NetworkMock Core](../modules/networkmock-core.md)
module page and epic [`worldline/devview#72`](https://github.com/worldline/devview/issues/72).
In short: `mocks.json`'s `environments[]` + `endpointOverrides` existed to let one endpoint
serve different paths/behaviour per deployment target — but that's exactly what OpenAPI's
`paths` already models, one operation per path+method. Modeling it a second time as a
DevView-specific "environment" concept was redundant, and OpenAPI tooling (editors,
linters, codegen) works with a `mocks.json` not at all.

## The fast path

1. Run the conversion script against your existing `mocks.json`:

   ```shell
   python scripts/mocks_json_to_openapi.py path/to/networkmocks/mocks.json
   ```

   This writes one spec file per `apiGroups[]` entry into `path/to/networkmocks/specs/`
   (override with `--out`), pointing at your existing response files in place — nothing on
   disk is moved, renamed, or deleted, so the script is safe to re-run.
2. **Read every `WARNING:` line it prints to stderr.** The script never silently guesses;
   anything it can't unambiguously translate is called out instead — see
   [What the script can't do](#what-the-script-cant-do).
3. Update your `NetworkMock(...)` registration (see [Call-site change](#call-site-change)).
4. Delete `mocks.json`.
5. Run your app, open DevView → Network Mock, and confirm every operation you expect is
   there with working mock responses.

## Field-by-field mapping

| Old (`mocks.json`) | New (OpenAPI spec) |
|---|---|
| `apiGroups[]` entry | one spec file |
| `group.name` | `info.title` — **the runtime spec id becomes `slugify(info.title)`**, not `group.id` |
| `group.defaultDelayMs` | root-level `x-devview.delayMs` |
| `environments[].url` | `servers[].url` (all environments' URLs, in one list) |
| `environments[].id` / `.name` | *gone* — there's no environment identity left to map |
| `endpoints[].id` | `operationId` |
| `endpoints[].name` | `summary` |
| `endpoints[].path` / `.method` | `paths.<path>.<method>` |
| `endpoints[].delayMs` | per-operation `x-devview.delayMs` |
| `endpoints[].queryParams` | `parameters[]` entries with `in: query` and a literal `example` |
| `endpointOverrides[]` | a **second** `paths` entry — see [Environments](#environments) |
| `additionalEndpoints[]` | a normal `paths` entry, unique to that spec |
| response file directory tier + naming convention | declared `responses.<code>.content.*.examples.<name>.externalValue` — see [Response files](#response-files) |

The engine's parser (`OpenApiParser` in `devview-networkmock-core`) ignores unknown keys
and defaults every field it doesn't need, so a minimal, hand-written spec works fine — you
don't need `openapi`/`info.version`/response `description` fields the script adds for
tooling compatibility.

## Environments

This is the conceptual center of the migration. `environments[]` collapses into `servers[]`:
matching happens by request hostname at interception time, with no stored "which
environment is active" selection — exactly like today, minus the environment metadata.

`endpointOverrides` doesn't get a replacement feature, because it doesn't need one: a path
variant that used to live under one environment's override is just **another `paths`
entry with its own `operationId`** in the same document. The sample app's `sample-api`
group is the reference case — see [Worked example](#worked-example) below. The
`Operation.version` display tag (a `/v{n}/` path segment, purely cosmetic — see
[NetworkMock Core § Version Tags](../modules/networkmock-core.md#version-tags)) is the UI
affordance that replaced the environment tab for telling variants apart at a glance.

An override that changes *only* `delayMs`, `name`, or `queryParams` (not `path` or
`method`) has no OpenAPI equivalent — one `(path, method)` pair is one operation, so it
can't behave two different ways depending on which server the request happened to hit.
The conversion script drops these and warns; decide by hand whether the base operation's
value should change, or whether the distinction genuinely doesn't matter anymore.

## Response files

Old convention — a directory tier plus a filename convention, with environment-specific
files shadowing shared ones:

```
responses/{groupId}/[{environmentId}/]{endpointId}/{endpointId}-{statusCode}[-{suffix}].json
```

New: every response variant is **explicitly declared** in the spec via
`responses.<code>.content.<mediaType>.examples.<name>.externalValue` — there is no
probing of status codes or filename suffixes at runtime. `externalValue` resolves
relative to the spec file's own location (or root-relative if it starts with `/`).
Existing response files **don't need to move** — point `externalValue` at wherever they
already live; the sample app's own migration only relocated files for tidiness, not
because the engine requires it.

## Delays

`defaultDelayMs` (group-wide) and `delayMs` (per-endpoint) both become the
[`x-devview` Specification Extension](../modules/networkmock-core.md#x-devview-extension):
document root for the spec-wide default, per-operation to override it. Precedence is
unchanged — operation, then spec, then no delay.

## DataStore reset

The operation-state key shape changed (`{groupId}-{environmentId}-{endpointId}` →
`{specId}-{operationId}`), and so did the persisted `Mock` payload (a response file name →
`(statusCode, mediaType, exampleName)`). On first launch after upgrading, legacy
entries are wiped once — see the
[DataStore Schema](../modules/networkmock-core.md#datastore-schema) table. This is
disabled-by-default developer-tooling state, not user data: previously-selected mocks
reset to "network" rather than being translated, but the global mocking toggle is
unaffected.

## Call-site change

```kotlin
// Before
module(NetworkMock(configPath = "files/networkmocks/mocks.json"))

// After
module(
    NetworkMock(
        resourceLoader = { path -> Res.readBytes(path) },
        specPaths = listOf(
            "files/networkmocks/specs/jsonplaceholder.json",
            "files/networkmocks/specs/sample-api.json"
        )
    )
)
```

One spec path per API group — the `mocks.json`-era `responseSuffixes` configuration
parameter is gone, since discovery now reads declared examples instead of probing
filename suffixes.

## Worked example

The sample app's real pre-0.2.0 `sample-api` group had a staging/prod split where prod
overrode one endpoint's path — the textbook "API version wearing an environment costume"
this migration targets:

```json
{
  "id": "sample-api",
  "name": "Sample API",
  "defaultDelayMs": 200,
  "endpoints": [
    { "id": "getUserProfile", "name": "Get User Profile", "path": "/api/v1/profile/{userId}", "method": "GET" },
    { "id": "updateProfile", "name": "Update Profile", "path": "/api/v1/profile", "method": "PUT" }
  ],
  "environments": [
    { "id": "staging", "name": "Staging", "url": "https://sample.api.staging.com" },
    {
      "id": "prod", "name": "Production", "url": "https://sample.api.com",
      "endpointOverrides": [
        { "id": "getUserProfile", "path": "/api/v2/profile/{userId}" }
      ]
    }
  ]
}
```

became one spec with both server URLs and a second operation for the overridden path
(trimmed to the relevant fields — see
`sample/network/src/commonMain/composeResources/files/networkmocks/specs/sample-api.json`
for the full file with response examples):

```json
{
  "info": { "title": "Sample API" },
  "servers": [
    { "url": "https://sample.api.staging.com" },
    { "url": "https://sample.api.com" }
  ],
  "x-devview": { "delayMs": 200 },
  "paths": {
    "/api/v1/profile/{userId}": { "get": { "operationId": "getUserProfile", "...": "..." } },
    "/api/v2/profile/{userId}": { "get": { "operationId": "getUserProfileV2", "...": "..." } },
    "/api/v1/profile": { "put": { "operationId": "updateProfile", "...": "..." } }
  }
}
```

`getUserProfile` and `getUserProfileV2` are two independent operations, matched purely by
path — either is reachable from either server URL, matching how the engine has no concept
of "which server this operation belongs to" beyond the spec they're declared in.

## What the script can't do

Review each of these manually — the script warns instead of guessing:

- **Group id won't match the runtime spec id.** If `slugify(group.name) != group.id`, the
  engine will assign a different spec id than your old `groupId` — update any hardcoded
  references (dashboards, DataStore inspection tooling) accordingly.
- **A no-op-for-OpenAPI override.** An override that changes only `delayMs`/`name`/
  `queryParams` (not `path`/`method`) is dropped; decide by hand what the base operation's
  value should be.
- **Stray environment-tier response files with no override.** Folded into the base
  operation as extra examples named `{environmentId}[-suffix]`, so nothing on disk is
  lost — but review whether that's the right way to expose them (e.g. maybe they belong on
  a distinct operation you add by hand).
- **An operation with no discovered response files.** Likely a naming-convention mismatch
  in your original tree; the operation is still emitted, just with an empty `responses`.
- **Derived `operationId` collisions.** The script disambiguates automatically, but pick a
  clearer name by hand afterward.

## Related

- [NetworkMock Core](../modules/networkmock-core.md) — the OpenAPI spec format in full.
- [NetworkMock](../modules/networkmock.md) — the module entry point and `specPaths`.
- `scripts/mocks_json_to_openapi.py --selftest` — runs the script's own self-check.
