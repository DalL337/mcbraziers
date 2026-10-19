# Tooling Register

The living record of what this project builds with and why
(`lore/policies/tooling.md` §2). Read it before any stack question. It grows
by dated addenda; entries are never silently rewritten.

## License Allowlist

*(Adopt-time slot, pending the owner's declaration.)* Until an allowlist is
declared, every addition is an owner decision recorded here with its license
named. Once declared, exceptions are owner decisions only, dated and reasoned
in this file (`lore/policies/tooling.md` §4).

> **Addendum (2026-10-18, owner declaration):** Anything bundled into the
> shipped mod must use a permissive license: MIT, Apache-2.0, BSD, or CC0.
> Dev-only tools that ship in no artifact (for example, Blockbench and its MCP
> server) are exempt. The mod itself is MIT (PRD, `docs/prds/mcbraziers-1.0.md`).
> The platform it runs on (Minecraft, proprietary) is not bundled and is
> outside this rule.

## Mod Stack

Recorded from existing state, 2026-10-18 (brownfield seed,
`lore/policies/tooling.md` §6). Versions come from `gradle.properties`,
`build.gradle`, and `gradle/wrapper/gradle-wrapper.properties`; licenses from
each project's LICENSE file, checked 2026-10-18.

Struck values were superseded by the 26.3 port on 2026-10-18 (addenda below).

| Item | Role | Version and pinning | License |
|---|---|---|---|
| Java | Language; bytecode target | ~~release 17; builds run on JDK 21 per `gradle.properties`~~ → release 25; the Gradle daemon is pinned to a local Java 25 by `gradle/gradle-daemon-jvm.properties` (no JDK path in the build) | — |
| Minecraft | Game target | ~~1.20.1~~ → 26.3 | proprietary (not checked) |
| Fabric Loader | Mod loader | ~~0.17.2~~ → 0.19.5 | Apache-2.0 |
| Fabric API | Mod API | ~~0.92.2+1.20.1~~ → 0.163.0+26.3 | Apache-2.0 |
| Yarn mappings | Deobfuscation mappings | ~~1.20.1+build.10~~ → removed; 26.x is unobfuscated and uses Mojang's names | CC0-1.0 |
| Fabric Loom | Gradle plugin | ~~1.6-SNAPSHOT~~ → 1.18.4, a release pin (plugin id now `net.fabricmc.fabric-loom`) | MIT |
| Gradle | Build tool | ~~8.6~~ → 9.7.1 via the wrapper, with `distributionSha256Sum` pinned | Apache-2.0 |

> **Addendum (2026-10-18, owner decision on the target version):** MC Braziers
> moves to Minecraft 26.3, the newest stable release, on the stack of the
> official Fabric example mod for 26.3: Loader 0.19.5, Fabric API
> 0.163.0+26.3, Gradle 9.7.1, Java 25, the `net.fabricmc.fabric-loom` plugin,
> and Mojang's names (26.x ships unobfuscated, and Yarn ends at 1.21.11). Loom
> is to be pinned to a release rather than a snapshot during the port. The
> table above describes the current build until the port lands; its rows are
> superseded in that same unit of work. Considered and not chosen: 26.2 (one
> release behind from day one), 1.21.11 (largest current Fabric audience, but
> the last obfuscated version, so a later mappings migration is unavoidable),
> and staying on 1.20.1. Evidence: local research journal
> `.research/2026-10-18-minecraft-fabric-target.md`.

> **Addendum (2026-10-18, port landed):** The build now runs on the 26.3
> stack in the table above. Loom is pinned to release 1.18.4 rather than the
> example mod's `1.18-SNAPSHOT`. The Gradle daemon's Java version moved from
> a machine path in `gradle.properties` to `gradle/gradle-daemon-jvm.properties`
> (`toolchainVersion=25`). That file has no download URLs on purpose: a
> missing JDK fails the build instead of downloading one. Evidence: local
> research journal `.research/2026-10-18-port-26-3.md`.

## Dev Tools

Not shipped in the mod.

| Item | Role | Version and pinning | License | Recorded |
|---|---|---|---|---|
| Blockbench (desktop) | Model and texture editor for the mod's assets | 5.0.7, updated by the owner | GPL-3.0 | from existing state, 2026-10-18 |
| Blockbench MCP headless server | Lets Claude Code read, edit, validate, convert, and render Blockbench models without the app | 1.10.1, pinned (below) | GPL-3.0 | owner decision, 2026-10-18 |
| JUnit (Jupiter + Platform launcher) | Runs the recipe contract test in `./gradlew build` | 6.1.3 through `junit-bom`, pinned in `gradle.properties` (`junit_version`) | EPL-2.0 | fast path, 2026-10-18 |
| Node.js | Runs the LORE checks and the brazier art generator (`tools/art`) | 24.14 on the owner's machine; the scripts use only Node's built-in modules, no npm packages | MIT | from existing state, 2026-10-18 |

> **Addendum (2026-10-18, art generator):** `tools/art/brazier-art.mjs` writes
> the brazier textures and floor part models (`docs/plans/art/copper-brazier-art.md`).
> It runs on the Node already used for the LORE checks and imports nothing
> outside Node's standard library. That adds no dependency and no install-time
> code, so it needs no fast-path review.

> **Addendum (2026-10-18, JUnit fast-path check):** JUnit qualifies for the
> fast path (`lore/policies/tooling.md` §4.1). It is test-only and ships in no
> artifact, so the bundled-license rule doesn't apply. It is a Maven jar with
> no install-time scripts, so it adds no execution surface. Its version is
> pinned exactly. It was added for the recipe contract
> (`docs/plans/recipes/brief-recipe-contract.md`, Enforcement). Fabric's
> data-generation and game-test modules come with Fabric API, which is already
> registered, and add no new dependency.

### Blockbench MCP Headless Server (added 2026-10-18, owner decision)

- **Upstream:** `jasonjgardner/blockbench-mcp-plugin`, tag `v1.10.1` → commit
  `16d3631c0572e9439d2273ac2a675e10e6873b58`. The tag and commit are unsigned.
  The LICENSE file is GPL-3.0; GitHub's API reports no license.
- **Pinning:** installed from the tagged tree (`git archive` sha256
  `b2fd8b8bdce425cbe19c9c78d19c5a26009b6e78e2d9c0506f3bbdff1bf8b9cf`), not
  `npx github:…`, which re-resolves caret ranges on every run. Server
  dependencies come from the audited lockfile via `npm ci --omit=dev` (sha256
  `45401bac3ce162ce3aa4070cdf8249add04aeccde36a2b46b84cf496bfb511c3`: MCP SDK
  1.32.1, zod 3.25.76, bun 1.4.3). The render dependencies are pinned by an
  audited `headless/package-lock.json` (sha256
  `b48831c5c9a2cab6bc4d51badfe177e67878c3dd52e2853608aacd4b86ff90bf`), so the
  first render runs `npm ci` instead of an unpinned `npm install`.
- **Location:** `%LOCALAPPDATA%\Programs\blockbench-mcp-headless\1.10.1`,
  outside the repository. Registered in Claude Code at local scope as
  `blockbench-headless`, with workspace roots `src/main/resources` and `temp/`.
- **Install-time code that runs on the machine:** Bun's npm postinstall
  (oven-sh). On first render, the `sharp` install check and the `webgpu`
  postinstall.
- **Security review:** sandboxed audit, 2026-10-18. No malware indicators:
  ClamAV 0 of 15,567 files; 0 OSV malicious-package advisories; the release
  bundle reproduced from source. Known advisories: `sharp` 0.34.5 (libvips,
  libheif, librsvg CVEs) in the render path, relevant only to untrusted images.
- **Not adopted:** the desktop plugin (`mcp.js`, release sha256
  `caf94540e50257eaeb5aa51ab4b34b56aa6ac258a0f8c0bee2025c2cf18d5db9`) is vetted
  but not installed. If adopted, load it from a file and turn off `risky_eval`.
- **Upgrades:** each new version gets the same audit before the pin moves.
- **Rationale:** local research journals
  `.research/2026-10-18-blockbench-agent-control.md` (selection) and
  `.research/2026-10-18-blockbench-mcp-security-audit.md` (audit).
