# Plan

A staged build order. **Stage 1 is specified in full**; Stages 2–6 are one-paragraph
outlines to be detailed when their turn comes. See [`CLAUDE.md`](CLAUDE.md) for the
working agreement this plan runs under — most importantly: **do not start the next stage
until the user has confirmed the current one works on their phone.**

---

## Stage 1 — Project skeleton, Gradle config, and a release pipeline

### Status (2026-08-25)

**Code complete. CI's debug build (`assembleDebug`) passes cleanly; `testDebugUnitTest`
has one known, documented failure out of ~40 tests across the whole project — see
[`BLOCKERS.md`](BLOCKERS.md) — everything else, including all of `:core:model`,
`:core:data`, `:stream`, and `:core:media`'s tests plus 13 of `:app`'s 14 Compose tests,
passes.** All Stage 1 files are written. Versions were confirmed by web search, not
memory — see the PR description for the exact numbers and confidence per dependency.
**The release (signed APK) path stays unverified until this PR merges to `main`** —
that job only runs on push to `main`, per this stage's own design (see "The workflow"
below). Do not start Stage 4 until the release APK installs and opens on your phone,
playback works end to end, and background playback survives screen-off — see "How you
verify this stage" below and the PR description's on-device checklist.

**Note on stage numbering:** this PR actually covers Stage 1 (Gradle skeleton, this
section) *and* the substance of Stage 2 (Media3 playback service) and Stage 3 (Compose
UI) below, built together in one overnight session under an explicit "Phases A–D" brief
rather than strictly one stage at a time. Each phase is still its own reviewable unit —
see the commit history — and CLAUDE.md's "confirm on your phone before the next stage"
rule still applies at the Stage 4 boundary: nothing about YouTube sync starts until you've
confirmed Stages 1–3's combined result works on your device.

### Goal

An Android project that contains no app logic yet, but that CI can compile, package,
sign, and publish end to end. This stage exists to prove the toolchain and the release
pipeline work *before* any feature code depends on them. If Stage 1's CI is broken, every
later stage inherits that breakage silently — better to find it now, against an app that
does nothing.

### What "done" looks like

- A launchable app with a single empty Compose screen — no player, no library, nothing
  from Stage 2 onward. Just proof the app installs and opens.
- A Gradle module structure that matches [`ARCHITECTURE.md`](ARCHITECTURE.md)'s module
  list, even though most modules are near-empty at this stage. Setting up the boundaries
  now means later stages drop code into place rather than restructuring around it.
- A GitHub Actions workflow that, on push to `main`, builds a **signed release APK** and
  publishes it to a GitHub Release automatically.

### Files this stage adds

```
settings.gradle.kts              # module list
build.gradle.kts                 # root: plugin versions only
gradle.properties
gradle/wrapper/...               # the Gradle wrapper — see note below
gradle/libs.versions.toml        # version catalog: one place for every dependency version
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/kotlin/.../MainActivity.kt   # single empty Compose screen
core/model/build.gradle.kts               # empty for now, wired into settings.gradle.kts
core/data/build.gradle.kts                # empty for now
core/media/build.gradle.kts               # empty for now
feature/library/build.gradle.kts          # empty for now
feature/player/build.gradle.kts           # empty for now
sync/youtube/build.gradle.kts             # empty for now
stream/build.gradle.kts                   # empty for now
.github/workflows/release.yml
```

**On the Gradle wrapper:** Gradle 8.14.3 exists in this sandbox, but the sandbox can't
build the project (see [`ENVIRONMENT.md`](ENVIRONMENT.md)), and GitHub Actions runners
don't ship a fixed Gradle version. The wrapper (`gradlew`, `gradlew.bat`,
`gradle/wrapper/gradle-wrapper.jar` + `.properties`) pins the exact Gradle version so CI
and any future local build (yours, or another AI session's) use the identical version.
This is committed even though we can't run `./gradlew` here to verify it — the wrapper
jar and properties file are generated content, not something to hand-write; if the
version needs bumping later, that's a `gradle wrapper --gradle-version <x>` run inside a
CI step or on your machine, not a manual edit.

### Key configuration decisions

**`versionCode` derives from `github.run_number`.** GitHub increments `run_number` on
every workflow run in the repo and never resets it, so it's a strictly increasing integer
for free — exactly what `versionCode` requires (Android refuses to install an "upgrade"
whose `versionCode` doesn't increase). `app/build.gradle.kts` reads it from an environment
variable with a local fallback so a non-CI Gradle invocation doesn't hard-fail:

```kotlin
versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
versionName = "1.0.${System.getenv("GITHUB_RUN_NUMBER") ?: "dev"}"
```

The workflow sets `GITHUB_RUN_NUMBER` explicitly from `${{ github.run_number }}` (GitHub
does export this automatically to the runner, but setting it explicitly in the `env:`
block keeps the dependency visible in the workflow file rather than implicit).

**Signing reads from secrets only, never a file.** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`,
`KEY_ALIAS`, `KEY_PASSWORD` are assumed to already exist as repository secrets (per your
brief). The workflow base64-decodes `KEYSTORE_BASE64` into a temp file *outside* the
working tree at build time; the signing config in `app/build.gradle.kts` reads all four
values from environment variables. No keystore path, password, or the key material itself
is ever written to a tracked file, and the decode step doesn't echo its output.

**Min SDK 26, per [`ARCHITECTURE.md`](ARCHITECTURE.md).** Target SDK tracks whatever's
current and supported by the Android Gradle Plugin version pinned in the version catalog.

### The workflow, `.github/workflows/release.yml`

Two triggers, deliberately doing different amounts of work:

| Trigger | Job | Touches signing secrets? |
|---|---|---|
| `pull_request` | `assembleDebug` + `testDebugUnitTest` | No |
| `push` to `main` | `assembleRelease`, sign, publish to a GitHub Release | Yes |

This split was a deliberate choice, confirmed with you: PRs get real build and unit-test
verification without ever touching signing secrets, since secrets are available to a
pull-request-triggered run in a way that's easy to get wrong. Only a push to `main` — which
requires a merge, which requires review — triggers signing.

Release job outline:

1. `actions/checkout@v4`
2. `actions/setup-java@v4`, Temurin **17** — the AGP-supported baseline. (The sandbox
   happens to have JDK 21; that's irrelevant since the sandbox never builds — see
   [`ENVIRONMENT.md`](ENVIRONMENT.md). CI picks its own JDK independently.)
3. `gradle/actions/setup-gradle@v4` for dependency and build caching across runs.
4. Decode `KEYSTORE_BASE64` to a temp path.
5. `./gradlew assembleRelease` with the four signing values and `GITHUB_RUN_NUMBER`
   passed as env vars.
6. Publish the signed APK to a GitHub Release via `softprops/action-gh-release@v2`,
   tagged `v1.0.${{ github.run_number }}`.

`permissions: contents: write` is scoped to the release job only (needed to create a
Release), not granted workflow-wide.

**Assumption to confirm on the first real run:** `ubuntu-latest` GitHub-hosted runners
ship the Android SDK preinstalled with `ANDROID_HOME` already set, so no `setup-android`
step should be needed. This can't be verified from the sandbox — it's the first thing to
check if the release job fails on something SDK-related.

### Acceptance criteria

- [ ] `pull_request` job runs and passes: debug build compiles, unit tests pass (even if
      there are close to zero tests at this stage — the job succeeding is the point).
- [ ] Merging to `main` triggers the release job.
- [ ] The release job produces a GitHub Release with a downloadable, signed APK attached.
- [ ] `versionCode` in two consecutive releases is strictly increasing.
- [ ] No secret value appears anywhere in workflow logs.

### How you verify this stage

1. Download the signed APK from the GitHub Release.
2. Sideload it onto your phone (`adb install`, or transfer and open it directly).
3. Confirm it installs without a signature error and opens to the empty Compose screen.
4. That's the whole test — there's no functionality yet to exercise beyond "it installs
   and opens." Confirm this to move to Stage 2.

---

## Stage 2 — Media3 playback service with a local file

### Status (2026-08-25)

**Done, this PR — all `:core:media` tests pass in CI; awaiting your on-device check.**
`PlaybackService` (the name
used in code instead of `MusicService`) lives in `:core:media`, wired to a single
`ExoPlayer`/`MediaSession` pair playing a bundled local WAV. Robolectric tests cover
service lifecycle, manifest correctness, controller connection, and (added in the Phase D
adversarial pass) rapid connect/disconnect churn, a controller disconnecting mid-playback,
and playback against an empty queue. See `ARCHITECTURE.md`'s "Testing boundaries" section
for what these tests do and don't prove — doze survival past 40+ minutes, OEM battery
killers, audio focus contention, and whether sound actually comes out of a speaker are
on-device-only checks, not something this CI run can certify.

Build `MusicService : MediaSessionService` in `:core:media`, wired to a single `ExoPlayer`
instance playing one audio file bundled in `app/src/main/res/raw/` — no network, no
`StreamResolver` yet, just proof that the session/service pattern from
[`ARCHITECTURE.md`](ARCHITECTURE.md) actually delivers M1 and M2: playback that survives
screen-off, backgrounding, and doze, with working lockscreen and notification controls
generated from the `MediaSession`. A trivial Compose screen with a play/pause button talks
to the service only through a `MediaController`, never touching the player directly,
establishing the ownership boundary the rest of the app depends on. This is the stage
that proves or disproves the hardest requirement, so it stays deliberately narrow —
one file, one track, no queue — until background playback is confirmed solid by hand on
the device.

## Stage 3 — Compose UI: library, player, queue

### Status (2026-08-25)

**Done, this PR — 13 of 14 `:app` Compose tests pass in CI; one known failure in
`QueueScreenTest` is documented in [`BLOCKERS.md`](BLOCKERS.md) rather than hidden;
awaiting your on-device check.** Library, player, and queue
screens, all thin functions of `PlaybackUiState` + callbacks talking to playback only
through the `PlaybackController` interface (never `MediaController`/`MediaSession`
directly, per `ARCHITECTURE.md` constraint 2 — verified by grep during the Phase D
architecture audit, see `FINDINGS.md`). Seeded from `FakeLibrary` (six hardcoded tracks)
rather than Room — deliberately deferred to Stage 4, see the Phase C commit message for
why adding Room for six rows tonight would have been unplanned scope. Queue reorder uses
up/down buttons rather than drag-and-drop, deliberately, since there's no way to test a
drag gesture in this sandbox (see `ENVIRONMENT.md` — no emulator, ever). Robolectric
Compose tests cover every screen's rendering and callback wiring.

Build out the real UI screens — library browse/search, now-playing, and queue — against
fake/seeded local data in Room (no YouTube sync yet), delivering M3 (queue, shuffle,
repeat) and M6 (clean, fast UI) using the `FakeStreamResolver` from
[`ARCHITECTURE.md`](ARCHITECTURE.md) so multiple local tracks can be queued and played
without any external dependency. This is the largest UI stage and the first one a
non-developer would recognize as "the app," so it's split from Stage 2 specifically so
playback stability gets confirmed in isolation first.

## Stage 4 — YouTube API OAuth and liked-videos sync

Implement `:sync:youtube`: OAuth against the user's own Google account, then the
documented `channels.list` → `playlistItems.list` walk described in
[`ARCHITECTURE.md`](ARCHITECTURE.md) to pull liked-video metadata into Room, delivering
M5. This is metadata only — deliberately built and shipped before Stage 5's stream
resolution exists, proving the architectural separation actually holds: a real, synced
library that browses and searches correctly while every track still plays through the
fake resolver.

## Stage 5 — Stream resolution layer

Implement the real `StreamResolver` behind the interface fixed in Stage 1/
[`ARCHITECTURE.md`](ARCHITECTURE.md), replacing `FakeStreamResolver` via a one-line Hilt
binding change, plus the `CacheDataSource` wiring for M4's size-capped offline cache keyed
on `TrackId`. This is the stage most likely to need iteration after the fact, precisely
because it's the volatile layer the whole architecture was built to isolate — nothing
outside `:stream` should need to change no matter how many times this stage's internals
are revisited.

## Stage 6 — Automatic library clustering

A nice-to-have (see [`REQUIREMENTS.md`](REQUIREMENTS.md)): group synced tracks by
inferred similarity without manual tagging, surfaced as an additional browse view
alongside the flat library from Stage 3. Deliberately last and least specified up front —
the approach (metadata-based heuristics vs. an on-device model vs. something simpler)
gets decided when this stage starts, informed by what the real synced library from Stage 4
actually looks like.
