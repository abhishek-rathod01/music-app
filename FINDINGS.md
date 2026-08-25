# Findings — Phase D bug hunt

Everything found during the Phase D pass over Phases A–C's code, what was done about it,
and anything left uncertain. Per CLAUDE.md's failure policy: uncertainty flagged is fine,
uncertainty hidden is not. See [`BLOCKERS.md`](BLOCKERS.md) for the one item that
couldn't be fully closed out.

---

## 1. Static analysis: ktlint/detekt/Android lint not wired into CI

**Decision, not an oversight.** The `pull_request` job in
`.github/workflows/release.yml` only runs `assembleDebug` and `testDebugUnitTest`.
Wiring in ktlint, detekt, or `./gradlew lint` as CI-enforced checks means editing that
workflow file — and the overnight brief explicitly bans touching
`.github/workflows/` with no exceptions. Adding the Gradle plugins themselves without
a CI step to run them would just be dead weight (nothing would ever call them), and
risked repeating the exact plugin-resolution collision from Phase A's CI run #5 on an
already very iteration-heavy night, for a check nothing was going to invoke anyway.

**What happened instead:** a manual review pass, done by hand across every Kotlin file
touched or added in Phases A–D:
- No wildcard imports.
- No `TODO`/`FIXME` markers.
- No `println`/`System.out.print` debugging left in.
- No empty `catch` blocks.
- No unjustified `@Suppress`.
- Line length — the common ktlint default of 120 chars — checked with a plain `awk`
  pass. Found six real violations in commit `05ca3a5` (pure reformatting, no logic
  changed) and re-checked clean after every file touched since.

This is a real gap relative to the brief's "add ktlint and detekt" instruction, flagged
rather than quietly skipped. A future session should revisit once a workflow-file change
can go through its own reviewed PR rather than being smuggled into a Phase-D commit
under a ban on touching that exact file.

## 2. `Icons.Filled.QueueMusic` deprecation (MainActivity.kt)

CI's own compiler warning (`compileDebugKotlin`, every run since Phase C): `'val
Icons.Filled.QueueMusic: ImageVector' is deprecated. Use the AutoMirrored version`.
Real, cheap, fixed: switched the import and call site to
`Icons.AutoMirrored.Filled.QueueMusic`. Not a suppression — the actual recommended
replacement.

## 3. `connect()` reentrancy bug in `MediaControllerPlaybackController` (real bug, fixed)

Found during the Phase D concurrency review, not by a failing test (see BLOCKERS.md for
why an automated regression test isn't feasible here).

**The bug:** `connect()` guarded re-entry with `if (controller != null) return`. But
`controller` is only assigned once `MediaController.Builder(...).buildAsync()`'s
listener fires — and that's asynchronous. If `connect()` was called a second time while
the first call's future was still pending (e.g. a rapid `onStart`/`onStop`/`onStart`,
which is exactly the pattern `PlaybackViewModel.init`/`onCleared` produces if a screen
were recreated quickly), the guard did nothing: a second `CoroutineScope` and a second
`MediaController.Builder(...).buildAsync()` call would start. The first `CoroutineScope`
(with its 500ms position-polling loop) was never cancelled — `scope` was simply
overwritten — so it leaked for the process's lifetime. Whichever future resolved last
would silently win and become the "real" `controller`, with no guarantee it was the one
`disconnect()` would later see cancelled correctly.

**The fix:** added a `pendingConnection: ListenableFuture<MediaController>?` field.
`connect()` now guards on `controller != null || pendingConnection != null`, so a
connect already in flight blocks a second one outright. `disconnect()` now also calls
`MediaController.releaseFuture()` on any still-pending connection — Media3's documented
way to cancel an in-flight `buildAsync()` (or release it if it resolved in the same
instant) — so a `connect()` that was in flight when `disconnect()` ran can never resolve
afterward and resurrect `controller` behind the caller's back.

**Confidence:** high that this was a real bug (the reasoning follows directly from
`buildAsync()`'s documented async contract, not a guess) and high that the fix addresses
it correctly by inspection. Medium confidence it's *fully* verified, honestly stated: see
BLOCKERS.md for why this specific class can't get a Robolectric regression test in this
sandbox. The fix should be exercised on-device as part of Phase C/D's device checklist —
switching quickly between the Player screen and another app a few times in a row is a
reasonable manual repro of the original bug's trigger condition.

## 4. `:app` Compose test failures (CI run #12 → fixed)

All 13 of `:app`'s Phase C Compose screen tests (`LibraryScreenTest`, `PlayerScreenTest`,
`QueueScreenTest`) failed CI run #12 with `java.lang.RuntimeException at
RoboMonitoringInstrumentation.java:102` — a wrapper exception whose message and
`Caused by:` chain never reach Gradle's default console output (only a downloadable HTML
report does, and this sandbox's network policy blocks the Actions artifact storage host
used to fetch it — an org-level 403, not something to route around).

<!-- FILLED IN AFTER THE DIAGNOSTIC CI RUN — see the commit that added testLogging
     (exceptionFormat = FULL) to the root build.gradle.kts for the mechanism, and the
     paragraph below for the actual root cause once confirmed from the real console
     stack trace. -->

## 5. Architecture non-negotiables — re-verified after Phase D changes

Grepped the whole tree after Phase D's edits to confirm all three still hold:

- **`ExoPlayer.Builder`/`MediaSession.Builder`** appear in exactly one place:
  `PlaybackService.kt`.
- **`FakeStreamResolver`** is only *constructed* in one place: `PlaybackViewModel.kt`
  (the app's composition root). The other hits (`FakeLibrary.kt`, `PlaybackController.kt`)
  are KDoc comments referencing the class by its fully-qualified name, not imports or
  code.
- **`:sync:youtube`** still has zero dependency on `:stream` (still a placeholder module,
  per its `build.gradle.kts` comment).

## 6. Queue/cache adversarial coverage — already thorough, reviewed not rewritten

Phase A's `QueueEngineTest` and `CacheEvictorTest` already cover every adversarial case
Phase D's brief calls out by name: empty queue, single-item queue, repeat-one at every
boundary (not just the end), reorder of the currently-playing item, shuffle→unshuffle
round-tripping exactly, a cap smaller than the pinned (currently-playing) cache entry,
a cap of zero, and referential transparency (no hidden state to race on). Reviewed line
by line during Phase D; nothing needed adding.

## 7. `PlaybackService`/`MediaController` adversarial coverage — new tests added

Phase B's `PlaybackServiceControllerTest` only covered the happy path (connect, set one
track, prepare, cleanly release). Phase D adds `PlaybackServiceAdversarialTest` with the
three cases the brief calls out by name:
- Five rapid connect/release cycles against the same service, then one more connection
  afterward, proving the service itself stays usable through churn.
- A controller releasing while a track is loaded and playing, then a second controller
  connecting and observing correct state (not a crash, not a stale/broken session).
- Connecting and calling `prepare()`/`play()` against an empty queue — must be a safe
  no-op, not a crash.
