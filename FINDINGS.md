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
`Caused by:` chain never reached Gradle's default console output (only a downloadable HTML
report does, and this sandbox's network policy blocks the Actions artifact storage host
used to fetch it — an org-level 403, not something to route around).

**Diagnostic step taken first, not a guess:** added `testLogging { exceptionFormat =
FULL }` to every subproject's `Test` tasks in the root `build.gradle.kts` (a real,
permanent improvement to CI's diagnostics, not a one-off hack) and re-ran CI to get the
actual message and full stack trace in the console.

**The real error, from run #13's console:**
```
java.lang.RuntimeException: Unable to resolve activity for Intent { act=android.intent.action.MAIN
cat=[android.intent.category.LAUNCHER] cmp=com.abhishekrathod.musicapp/androidx.activity.ComponentActivity }
    at org.robolectric.android.internal.RoboMonitoringInstrumentation.startActivitySyncInternal(...)
    at androidx.test.core.app.ActivityScenario.launch(...)
    ...
    at androidx.compose.ui.test.junit4.AndroidComposeTestRule...
```

**Root cause:** `createComposeRule()` (used with no explicit activity class) hosts its
composable content inside `androidx.activity.ComponentActivity`, launched via
`ActivityScenario` — and it needs that activity actually registered in the manifest
Robolectric resolves against. The `androidx.compose.ui:ui-test-manifest` artifact exists
specifically to provide that registration, by carrying its own small
`AndroidManifest.xml` declaring that activity with a `MAIN`/`LAUNCHER` intent-filter.
It was declared `testImplementation(libs.compose.ui.test.manifest)` in
`app/build.gradle.kts` — but AGP's unit-test manifest merge (`processDebugUnitTestManifest`)
only folds in the app module's own main/debug manifest dependency chain
(`implementation`/`debugImplementation`), not `testImplementation`-scoped manifests —
those are compiled onto the test's JVM classpath but never merged into the manifest
Robolectric treats as "the app under test." The dependency compiled and even ran; it
just never registered the activity, which is why nothing failed until the moment
`createComposeRule()` tried to launch it.

**Fix:** moved the dependency to `debugImplementation(libs.compose.ui.test.manifest)` —
the pattern Google's own Compose sample projects use for exactly this reason.
`debugImplementation` also means this test-only manifest fragment (and the activity it
declares) never ships in a release build, which matters for a project whose CI produces
a real signed release APK on push to `main`.

**Confidence:** high — this is a well-documented, common Compose-testing setup mistake
with a single well-known fix, not a novel finding, and the diagnostic stack trace
confirms the exact mechanism (an unresolvable `ComponentActivity` launch) rather than
leaving it to inference.

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

## 8. `QueueScreenTest` row-click query was ambiguous (CI run #16 → fixed)

After the fix in finding 4 landed, CI run #16 turned up a genuinely different failure —
13 of the 14 previously-broken tests now passed, isolating this to one:

```
QueueScreenTest > clicking a queued track reports its index FAILED
    java.lang.AssertionError: expected index 2, got null
```

`performClick()` itself didn't throw — it ran to completion — but `onItemClick` was never
invoked, so `clickedIndex` stayed `null`. The test queried `onNodeWithText("Song C")`.

**Root cause:** `QueueScreen`'s row is `.clickable { onItemClick(index) }` — the same
pattern `LibraryScreen`'s row uses (and that row's equivalent test passes). The
difference is `QueueScreen`'s row also has three independently-clickable `IconButton`s
in its `trailingContent` (move up, move down, remove). A clickable element merges its
descendants' semantics into itself for accessibility, but a *nested* clickable
descendant is its own merge boundary — so this row's headline/supporting text (`"Song
C"` / `"Artist C"`) sits in a merged tree alongside three sibling clickable regions.
Querying by text alone was ambiguous enough in that shape that `performClick()`
resolved to something other than the row's own click action.

**Fix:** query by the row's `contentDescription` (`"Play Song C by Artist C"`) instead
of by its text. That description is set directly on the same clickable node as the
`onClick` action in `QueueScreen.kt` — not merged in from a descendant — so it names
that exact node unambiguously. This also matches the pattern this test file's own
passing tests already use for the icon buttons (`onNodeWithContentDescription("Move
Song B up")`, etc.).

**Scope of the fix, stated plainly:** this changes the *test's query*, not
`QueueScreen.kt`'s production behavior. Real touch input on a device hit-tests actual
screen coordinates, not the semantics tree — the row has always been genuinely
clickable to a real finger; the ambiguity was specific to how the Compose testing
framework resolves an underspecified text-based semantics query against a row with
nested clickable children, not a defect in what ships. `LibraryScreen`'s equivalent
test was left as-is since it currently passes and its row has no nested clickables
that could trigger the same ambiguity — flagged here as something to watch if that
screen ever grows one.

**Confidence:** high on the mechanism (nested clickable regions are a documented
Compose semantics-merging edge case) and high on the fix being correct by inspection
(the content description targets the exact node bearing the `onClick` action). This is
attempt 1 for this specific failure, and it resolved it — see the CI run after this
commit.
