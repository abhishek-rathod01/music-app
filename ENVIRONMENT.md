# Environment

What the AI sandbox and CI can and cannot do. Everything here was **measured**, not
assumed — commands were run and the exact output recorded.

**The one-line version: the sandbox cannot build this app, and never will be able to.
GitHub Actions is the only build.** If you are an AI session reading this, do not spend
time trying to get a local Gradle build working. It is blocked by organization network
policy, not by anything you can fix.

Audited 2026-08-23.

---

## Runtime

| Item | Result | Verdict |
|---|---|---|
| OS / arch | Ubuntu 24.04.4 LTS (Noble Numbat), x86_64, kernel 6.18.44 | Fine |
| CPU / RAM | 4 cores, 15 GiB total, ~15 GiB available | Fine |
| Disk | 31 GiB available of a 252 GiB volume | Fine |
| `/dev/kvm` | **Absent** — `ls: cannot access '/dev/kvm': No such file or directory` | Blocker (emulator) |
| Docker | Client v29.3.1 installed, **daemon not running**: `failed to connect to the docker API at unix:///var/run/docker.sock ... connect: no such file or directory` | Blocker (containers) |
| Persistence | **Ephemeral.** `uptime` read 1 minute at session start; the container is reclaimed after inactivity | Fine — commit and push everything |

Ephemerality is worth stating plainly: nothing on disk survives the session. Any work not
pushed to GitHub is gone.

## Toolchain

| Item | Result | Verdict |
|---|---|---|
| Java | OpenJDK **21.0.10** (Ubuntu build), `javac` present, `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` | Fine |
| Android SDK | `/opt/android-sdk` exists but is an **empty shell** — `cmdline-tools/` is an empty directory; no `platforms/`, `build-tools/`, or `platform-tools/`. `ANDROID_HOME` and `ANDROID_SDK_ROOT` are both unset | **Blocker** |
| `sdkmanager` | **Not on PATH and not on disk** | **Blocker** |
| Gradle | **8.14.3** at `/opt/gradle/bin/gradle` | Fine — we ship a wrapper regardless |
| Python | **3.11.15**; `pip` and `pip3` present. `pip install requests` **succeeded** | Fine |
| `gh` CLI | **Not installed** — `gh: command not found` | Workaround below |

## Network egress

Every row below is an actual request made from the sandbox, not a guess.

| Host | Result | Verdict |
|---|---|---|
| `repo.maven.apache.org` | HTTP 200 | Fine |
| `dl.google.com` (Google Maven) | **BLOCKED** — `curl: (56) CONNECT tunnel failed, response 403` | **Blocker** |
| `services.gradle.org` | HTTP 200 | Fine |
| `github.com` | HTTP 400 on the bare root through the proxy, but **git works**: `git ls-remote` and `git fetch` both exit 0 | Fine |
| `api.github.com` | HTTP 200 | Fine |
| `www.googleapis.com` | HTTP 404 on the bare path — 404 is the API answering, so the host is reachable | Fine |
| `oauth2.googleapis.com` | HTTP 404 on the bare path — reachable | Fine |
| `pypi.org` | HTTP 200 | Fine |

The proxy's own status endpoint records the Google Maven denial explicitly:

```json
{
  "ts": "2026-08-23T10:09:01.514Z",
  "kind": "connect_rejected",
  "detail": "gateway answered 403 to CONNECT (policy denial or upstream failure)",
  "host": "dl.google.com:443"
}
```

Per `/root/.ccr/README.md`, a 403 from this proxy is an **organization egress policy
denial**. The documented rule is that it must be reported rather than routed around, and
that TLS verification must never be disabled to get past it. So Google Maven is
unreachable from the sandbox for the life of this project. Treat that as permanent.

## Permissions

| Item | Result | Verdict |
|---|---|---|
| Write to the repo working tree | Yes | Fine |
| Push a branch | Yes — authenticated as `abhishek-rathod01`, HTTPS read and write both work | Fine |
| Open a pull request | Yes, via the GitHub MCP tools | Fine |
| Read Actions run logs and failure output | Yes — the Actions API responds and job logs are retrievable | Fine |
| Create and edit `.github/workflows/` | Yes — ordinary file writes, no restriction found | Fine |

---

## Blockers, and what we do about them

### 1. No local Android build is possible

Three independent causes, any one of which would be sufficient on its own:

1. The Android SDK directory is empty.
2. `sdkmanager` does not exist, so the SDK cannot be populated.
3. `dl.google.com` is blocked by organization policy, so AGP and AndroidX artifacts
   could not be downloaded even if the SDK were installed.

**What we do:** nothing — this is the intended setup. CI is the build. The consequence to
internalise is that **the first CI run is the first time any code is compiled.** No
dependency version, no plugin version, no Gradle config can be verified before then. A
red first run is expected signal, not a surprise. Stage 1 is deliberately minimal so that
run tests the toolchain and nothing else.

### 2. No emulator, ever

No `/dev/kvm` and no Docker daemon, so no Android Virtual Device can run here.

**What we do:** this matches the plan. CI runs unit tests only. Instrumented tests and all
playback verification happen by hand on a physical device.

### 3. No `gh` CLI

**What we do:** use the GitHub MCP tools instead. They cover pull requests, releases,
Actions logs, and comments. No practical impact.

### 4. The repository started completely empty

At audit time `git ls-remote origin` returned zero refs and the branches API returned
`[]` — no commits, no `main`, and no README despite one being expected. GitHub cannot open
a pull request without a base branch to merge into.

**What we did:** pushed a single bootstrap commit to `main` containing only `README.md`
and `.gitignore`, then opened a pull request from the feature branch with everything else.
That was the only direct write to `main`; all real content still goes through review.

---

## What this means for how you work

- **Never claim a build passes because it "looks right".** You cannot compile. Say what
  you wrote, say it is unverified, and let CI be the judge.
- **Read the CI logs after every push.** That is the only feedback loop that exists
  besides the human.
- **Push early.** The sandbox is ephemeral; unpushed work is lost work.
- **Do not disable TLS verification or unset `HTTPS_PROXY`** to get around the Google
  Maven block. It is a policy denial and there is no legitimate way around it from here.
