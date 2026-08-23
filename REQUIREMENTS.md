# Requirements

A personal Android music player. **One user — me. Not for distribution.**

That framing is a real engineering constraint, not a disclaimer. It means no accounts, no
backend, no migration story for other people's data, no Play Store review surface, and no
need to handle a library that isn't mine. Wherever a decision below looks unusually
simple, this is why.

Every "must have" carries an acceptance check phrased as something testable **by hand on a
physical device**, because that is the only way anything here can actually be verified.
See [`ENVIRONMENT.md`](ENVIRONMENT.md).

---

## Must have

### M1 — Background playback that survives screen-off, app-switch, and doze

Audio continues when the screen is off, when another app comes to the foreground, and
after the device enters doze. This is the single hardest requirement and it drives the
architecture: playback must live in a `MediaSessionService`, never in UI-owned components.

> **Test:** Start a track. Lock the screen and wait 60 seconds — audio continues. Switch
> to another app and scroll around for a minute — audio continues. Leave the phone
> untouched and screen-off for 30+ minutes, then start a new track from the lockscreen —
> it plays without the service having been killed.

### M2 — Lockscreen and notification controls

A media notification with play/pause, skip forward, skip back, and track metadata
(title, artist, artwork). Controls work from the lockscreen and from the notification
shade, and stay in sync with actual playback state.

> **Test:** With a track playing, lock the phone. The lockscreen shows title, artist and
> artwork. Pause from the lockscreen — audio stops and the notification updates to show
> play. Skip — the next queued track starts and metadata updates everywhere.

### M3 — Queue, shuffle, repeat

A visible, reorderable play queue. Shuffle toggles on and off. Repeat cycles off →
repeat-all → repeat-one. State survives the app being backgrounded.

> **Test:** Queue five tracks, reorder two by dragging, confirm playback follows the new
> order. Enable shuffle — order changes and doesn't repeat a track until the queue is
> exhausted. Set repeat-one and let a track end — the same track restarts.

### M4 — Offline cache with a size cap

Played audio is cached to disk so replays don't re-download. The cache has a
**user-configurable size cap** and evicts least-recently-used content when it's hit. Cache
size is visible in settings and can be cleared manually.

> **Test:** Play a track, enable airplane mode, play the same track again — it plays from
> cache. Set the cap to a small value, play enough tracks to exceed it, confirm the
> reported cache size stays at or below the cap and old tracks are gone.

### M5 — Library synced from my own YouTube liked videos

The library is populated from the liked videos of my own YouTube account via OAuth. Sync
is incremental where the API allows, runs on demand, and reports clearly when it fails.

> **Test:** Sign in, run a sync, confirm the track count matches my liked videos. Like a
> new video in the YouTube app, re-sync, confirm it appears. Turn off networking and
> sync — a clear error appears, and the existing library is still browsable.

### M6 — Clean, fast UI

Browsing and searching stay responsive on a real library. No blocking work on the main
thread, no jank when scrolling artwork, and the app opens straight into a usable state
rather than a loading spinner.

> **Test:** Scroll the full library fast — no stutter, artwork loads without blank gaps
> persisting. Cold-start the app — previously synced library is visible immediately, not
> after a network round trip. Search — results filter as I type without lag.

---

## Nice to have

Wanted, but not at the cost of anything above. Roughly in priority order.

- **Sleep timer** — stop playback after a chosen duration, with an option to finish the
  current track first. Small, self-contained, high daily value.
- **Automatic clustering of the library** — group tracks by inferred similarity so the
  library has some structure beyond a flat list, without me tagging anything by hand.
  Deliberately last in the plan: it's the least defined and the easiest to get wrong.
- **Android Auto** — browse and control from a car head unit. Media3 gives much of this
  once M1 and M2 are solid, but it carries its own testing burden and a validation surface
  I can't automate.

---

## Out of scope

Listed so future sessions don't reintroduce them by accident. Each has a reason.

- **Multi-user support.** One user, one device, one library. No account system, no
  per-user data partitioning, no sharing.
- **Any backend.** No server, no hosted sync, no analytics. Everything runs on the phone
  and talks directly to Google's APIs. Nothing to deploy, nothing to pay for, nothing to
  keep secure.
- **Play Store distribution.** Installed by sideloading a signed APK from a GitHub
  Release. No store listing, no policy review, no staged rollout, no obligation to support
  anyone else's device.
- **Video playback — audio only.** This is a deliberate battery and data decision, not an
  omission. Decoding and rendering video costs significant battery and bandwidth for
  content I'm listening to with the screen off. Audio-only also keeps the player model
  simple: no surface management, no aspect handling, no picture-in-picture.
