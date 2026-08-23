# music-app

A personal Android music player. Single user, not for distribution.

Plays audio from my own YouTube liked videos, in the background, with lockscreen
and notification controls and an offline cache. Audio only — no video.

Built in Kotlin with Jetpack Compose and Media3. Min SDK 26.

## Documents

| File | What it's for |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Working agreement and project context for AI sessions |
| [`REQUIREMENTS.md`](REQUIREMENTS.md) | What this app must do, might do, and will never do |
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | Module boundaries, data flow, and why each decision was made |
| [`PLAN.md`](PLAN.md) | Staged build plan — Stage 1 in detail, Stages 2–6 in outline |
| [`ENVIRONMENT.md`](ENVIRONMENT.md) | What the CI and dev sandbox can and cannot do |

## Building

Builds run in GitHub Actions, not locally. A push to `main` produces a signed
release APK and publishes it to a GitHub Release; pull requests get a debug build
and unit tests. See [`PLAN.md`](PLAN.md) for the details.
