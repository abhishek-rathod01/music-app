# Blockers

Things a fix was attempted on (per CLAUDE.md's retry rule: 3 attempts, root-caused from
the actual error each time) that did not resolve, or that are genuinely out of this
sandbox's reach. Anything logged here was **stopped**, not worked around — see
[`FINDINGS.md`](FINDINGS.md) for everything that *was* resolved.

---

## Open: `QueueScreenTest > clicking a queued track reports its index` still fails in CI

**Status: retired per the retry rule, one test left red, not disabled.** Every *other*
CI failure hit during Phases A–D was root-caused and fixed within the retry budget (see
the commit history and [`FINDINGS.md`](FINDINGS.md)) — this is the one exception, and
it is being reported plainly rather than hidden or worked around by disabling the test.

**Exact error**, unchanged across all three attempts:
```
QueueScreenTest > clicking a queued track reports its index FAILED
    java.lang.AssertionError: expected index 2, got null
        at com.abhishekrathod.musicapp.ui.QueueScreenTest.clicking a queued track reports its index(...)
```
`performClick()` never throws — it completes normally every time — but the
`onItemClick` callback it should trigger is never invoked, so the test's own
`clickedIndex` variable stays `null`. Every other test in the same file (five of them,
including three that click sibling `IconButton`s inside the exact same row) passes.

**What was tried, in order, each root-caused from the actual CI log before the next
attempt (not guessed):**

1. **`onNodeWithText("Song C")`** (the original code). Reasoned this was ambiguous
   because the row also contains three nested independently-clickable `IconButton`s,
   and text-based matching could resolve to the wrong place in the merged semantics
   tree. **Failed identically.**
2. **`onNodeWithContentDescription("Play Song C by Artist C")`** — targets the exact
   same node as the `onClick` action via a property set directly on it, not merged
   in from a descendant. **Failed identically.**
3. **`onNodeWithTag("queue_item_2")`** — added `Modifier.testTag(...)` directly to the
   row in `QueueScreen.kt`. `testTag` is documented as excluded from Compose's
   ancestor-merging entirely, so this should have been immune to whatever the first
   two attempts ran into. **Failed identically.**
4. **`onNodeWithTag("queue_item_2", useUnmergedTree = true)`** — researched via
   Android's own semantics-merging documentation
   (`developer.android.com/develop/ui/compose/accessibility/merging-clearing`), which
   names `useUnmergedTree = true` as the specific recommendation for testing a
   clickable container with nested independently-clickable children. **Failed
   identically.** (This was attempt 3 of 3 under the retry rule — see FINDINGS.md
   finding 8 for the full writeup of all four attempts.)

**Best remaining hypothesis, not verified:** since changing *which semantics property
identifies the node* across four different, individually well-reasoned approaches never
changed the outcome even once, the targeting mechanism is probably not the actual
variable. The one thing common to all four attempts is `performClick()`'s own
resolution/dispatch against a node whose subtree contains other independently-clickable
descendants and that lives inside a `LazyColumn` `items(count)` block. Two unverified
possibilities worth a future session's time: (a) a real interaction between this
project's `createComposeRule()` (the deprecated v1 API, `UnconfinedTestDispatcher`) and
how `performClick()`'s semantics action dispatch resolves for this specific node shape
— the compiler's own deprecation warning for this API explicitly says "tests relying on
immediate execution may require explicit synchronization," which was never directly
tested as a fix (e.g. an explicit `composeTestRule.waitForIdle()` after `performClick()`
before asserting); (b) a version-specific bug or edge case in this project's exact
Compose BOM (2026.08.00) around merged-node click-action resolution for a `ListItem`
with a `trailingContent` full of clickables, which no attempt here isolated from the
`ListItem`/`LazyColumn` combination.

**Why this stops here rather than a fourth attempt:** the explicit failure policy caps
retries at three per item specifically so one hard problem doesn't consume the whole
session at the expense of everything else still to do. Per that same policy, the test
was **not** deleted, `@Ignore`d, weakened, or worked around — it stays in the suite,
still asserting the real, intended behavior (clicking a queued track should call
`onItemClick` with that track's index), and CI will show it red until a future session
picks this back up with either a local reproduction environment or a different
diagnostic angle than the four tried here.

**What to check first, if you pick this up:** try `composeTestRule.waitForIdle()`
immediately after `performClick()` and before the assertion (tests explicit
synchronization, hypothesis (a) above); or migrate this one test file to the v2
`androidx.compose.ui.test.junit4.v2.createComposeRule()`/`StandardTestDispatcher` API
the deprecation warning points at, in isolation, to see if that changes the outcome.

## Standing, sandbox-level limitation (not new, not a retry-rule item)

**`MediaControllerPlaybackController` cannot get a Robolectric regression test for the
Phase D `connect()` reentrancy fix (see FINDINGS.md).** That class's `connect()` builds
its `SessionToken` via `SessionToken(context, ComponentName(context,
PlaybackService::class.java))` — the same constructor that `PlaybackServiceControllerTest`
found NPE's under this project's Robolectric setup during Phase B (see that test's KDoc:
`PackageManager.getApplicationInfo`/`queryIntentServices` resolution doesn't fully hold up
here). `PlaybackServiceControllerTest` and `PlaybackServiceAdversarialTest` sidestep this
by connecting via `session.token` directly instead of that constructor — but
`MediaControllerPlaybackController` is production code and rightly keeps the standard,
correct, real-device constructor (changing it to dodge a test-environment limitation
would be exactly the kind of workaround the failure policy bans). The result: the
reentrancy bug itself was found by code review and is fixed, but there is no automated
test proving it stays fixed. Flagged rather than hidden — worth revisiting once
`PlaybackController` gets an injectable controller-factory seam (a real, larger design
change, not something to force in tonight), which would let a fake stand in for the
`MediaController.Builder`/`SessionToken` machinery entirely.

**On-device verification is unavoidable, not a blocker.** Everything CLAUDE.md already
says applies: doze survival past 40+ minutes, OEM battery killers, audio focus
contention, and whether sound actually comes out of a speaker are outside what this
sandbox or Robolectric can ever prove. See ARCHITECTURE.md's "Testing boundaries"
section and the PR description's on-device checklist.
