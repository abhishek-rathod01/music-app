# CLAUDE.md

Project context for AI sessions working on this repo. Read this first, then
[`ENVIRONMENT.md`](ENVIRONMENT.md) for what the sandbox can't do.

---

## Who you're working with

A second-year CS student. Strong in Python, comfortable with programming fundamentals,
**new to Android and Kotlin**. That shapes how you should communicate:

- **Explain what you're doing and why, as you go.** Not just "I added a `StateFlow`" —
  say what a `StateFlow` is for and why it beat the alternative here.
- **Name Android-specific concepts rather than assuming them.** Lifecycle, `Context`,
  foreground services, the manifest, Gradle's module system, `ViewModel` scoping — these
  are unfamiliar. A sentence of context costs little.
- **Draw Python parallels when they genuinely help** (coroutines vs. `asyncio`, Gradle vs.
  `pyproject.toml`, Hilt vs. manual dependency wiring). Don't force one where it misleads —
  a bad analogy is worse than none.
- Don't over-explain general programming. Loops and classes are not the gap; the Android
  platform is.

## The stack

| Concern | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, **single-activity** |
| Playback | Media3 — `media3-exoplayer` + `media3-session` |
| Local storage | Room (library metadata), DataStore (preferences) |
| Dependency injection | Hilt |
| Image loading | Coil |
| Min SDK | **26** |

Don't add a library outside this list without raising it first and saying why.

## How builds and tests actually work here

This is the most important operational fact in this file, so it gets its own section.

- **Builds run in GitHub Actions. Not locally, and not in your sandbox.**
  See [`ENVIRONMENT.md`](ENVIRONMENT.md) — the Android SDK is absent and Google's Maven
  repo is blocked by network policy. This is not fixable.
- **Assume you cannot run the app.** You will never see it launch, never see a
  screenshot, never observe a crash directly.
- **Unit tests run in CI.** Write them, and make them meaningful.
- **Instrumented tests and playback tests do not run anywhere automated.** The user tests
  those by hand on a physical device. Don't write instrumented tests expecting CI to
  catch regressions — it won't.
- **The user is your only feedback channel.** Everything you learn about runtime
  behaviour comes from them telling you.

Because of that last point:

- **Fail loudly.** No silently swallowed exceptions, no empty `catch` blocks, no defaults
  that quietly paper over a broken state. If something is wrong, it should be obvious.
- **Log clearly.** Tag logs consistently, include the state that would let someone
  diagnose from a logcat dump alone. Assume the log is the only evidence you'll get.
- **Never claim something works because it compiles or looks correct.** Say what you
  wrote, say it's unverified, and say precisely what the user should check on their phone.
  Overstating confidence wastes their time and burns the one feedback loop you have.

## Secrets

Never commit secrets. These stay gitignored, permanently, no exceptions:

```
*.jks
*.keystore
local.properties
google-services.json
keystore.properties
client_secret*.json
```

Signing credentials and OAuth client secrets reach the build through **GitHub Actions
secrets and environment variables only** — never a tracked file. Before any commit, check
that nothing matching the list above got staged.

## How we work

- **Work in stages.** [`PLAN.md`](PLAN.md) defines them.
- **Do not start the next stage until the user confirms the current one works on their
  phone.** Not "until CI is green" — CI can't test playback. Wait for the human.
- **Always open a pull request.** Never push directly to `main`.
- Keep each PR to one stage. A reviewer new to Kotlin can't review a thousand-line diff
  meaningfully, and neither can anyone else.
- When you finish a stage, tell the user exactly what to test on the device and what
  correct behaviour looks like. Be specific: "play a track, lock the screen, wait 30
  seconds, confirm audio continues and the lockscreen shows controls" beats "check
  playback works".

## The three architectural rules that don't bend

Full reasoning in [`ARCHITECTURE.md`](ARCHITECTURE.md). In brief:

1. **Stream resolution sits behind one interface**, with a fake implementation for tests.
   It's the only volatile part of the codebase and must be swappable without touching
   anything else.
2. **Playback lives in a `MediaSessionService`** — never in a ViewModel or an Activity.
3. **Library metadata comes from the official YouTube Data API v3 via OAuth**, and stays
   completely separate from stream resolution.

If a change seems to require breaking one of these, stop and raise it. That's a design
conversation, not an implementation detail.
