# Blockers

Things a fix was attempted on (per CLAUDE.md's retry rule: 3 attempts, root-caused from
the actual error each time) that did not resolve, or that are genuinely out of this
sandbox's reach. Anything logged here was **stopped**, not worked around — see
[`FINDINGS.md`](FINDINGS.md) for everything that *was* resolved.

---

## No open blockers as of this PR

Every CI failure hit during Phases A–D was root-caused and fixed within the retry
budget (see the commit history and [`FINDINGS.md`](FINDINGS.md) for the specifics — the
Gradle plugin-resolution collision, the `:app` missing-module-dependency failure, and
the `:app` Compose test failures Phase D root-caused and fixed).

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
