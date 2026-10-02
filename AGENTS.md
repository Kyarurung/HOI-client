# Repository Guidelines

## Scope and non-negotiable rules

HOI-client is the Java 25 / Minecraft 26.3 Fabric client mod (`client` and `protocol`). Keep `dev.hoi.client`, `dev.hoi.protocol`, stable `hoi:` IDs and UTF-8/four-space Java. Project-owned tests, including GameTests, use the `Test` suffix; preserve their framework, registration, source sets and tasks. Do not downgrade Minecraft or add obsolete mappings: 26.3 is unobfuscated.

- The server owns country assignments, research, time, validation, persistence and rewards. Send bounded requests and render private authoritative snapshots; keep country/session/revision checks and reject stale responses. Unknown amounts/ratios stay unknown. Never initialize/reset national accounts or grant state in the client.
- Register `ProtocolResearchPayloads.registerPayloadTypes()` before receivers; repeated registration must be safe. Synchronize shared protocol sources/versions with HOI, including optional legacy-compatible HUD fields.
- Never register client `/hoi` or server-owned lobby/reset command roots. Use the research menu icon and authenticated OPEN request. Preserve vanilla input outside campaign context and read the linked command/input rules before changing bindings.
- Preserve the 2020-01-01 epoch, 144 game minutes per 20 server ticks at X1 (2.4 game hours/real second at 20 TPS; X10 is 24 game hours/real second), PAUSED/X1..X10 (preserve legacy save compatibility without rewriting source files) and all eight research categories. Do not invent technologies, equipment or battalion values from reference images. Timeline labels do not redefine the campaign epoch.
- Gameplay art/audio and map models come from HOI-resourcepack and stay external to the JAR. The user-provided mod-list icon is the sole embedded image. Preserve source provenance, original pixels/aspect ratios, bounded drawing areas, accessible names and Korean localisation keys/IDs.
- GUI scale 2 is the layout/primary visual-validation baseline; preserve per-cell alignment without changing the player's setting. Preserve original tooltip wording/effects/order/colors/line breaks/number formatting and blank-line separators.
- Keep generated builds/runs, logs, credentials and caches out of commits. Never start/restart operational HOI-server without an explicit user request. Renderer fixtures do not establish multiplayer synchronization.

Current and future Korean UI text/tooltips follow TFR Korean translation patch workshop 3411756715. Resolve original keys; retain TFRKP-specific names when the patch has no matching key. Preserve command/save IDs and source-key provenance. User instructions take precedence over reference examples.

## Read only the relevant detailed constraints

[Client implementation constraints](docs/ui.md#client-implementation-constraints) retain the exact dimensions, indicator order, input behavior, research slot limits and HUD values. Read the relevant section **before changing its area**; these rules remain binding. Use [README.md](README.md) for build/verification status and [audio](docs/audio.md) for playback. Tool, skill and documentation rules are in [RTK.md](RTK.md#repository-workflow).

## Validation and workflow

Read the applicable [hoi-client-development skill](.agents/skills/hoi-client-development/SKILL.md) before its workflow and inspect sibling AGENTS.md/skills before cross-repository edits. Preserve unrelated working-tree changes.

For code/build delivery, first build HOI-resourcepack with `python scripts/build_pack.py` there, then run `.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon` here with Java 25 and inspect the client-only `build/libs/hoi-client-1.0.0+26.3.jar`. Supply `-PhoiResourcePack=<absolute ZIP path>` if needed. `runGameTest` performs actual rendering and requires a display (Xvfb in Linux CI); record multiplayer/unperformed checks separately in README. Documentation-only work needs link/command consistency checks.

Use targeted `rg` searches. Read [RTK.md](RTK.md) and the linked tool details before filtering or graph queries. Confirm supported commands/schema, root/index freshness/language support and results against source; use native output for exact data/failures. Exclude generated/runtime/secret data from indexes. RTK is instruction-based, not automatic PowerShell rewriting.

Use existing code/stdlib first while completing requested scope and validation. Apply installed Minecraft skills only when applicable. Answer in Korean, result/action first, with short steps; preserve facts/identifiers/links in prose edits. Keep durable behavior/reproduction/verification limits in README or feature docs and completion summaries in responses/commits rather than dated reports. Preserve unique knowledge before deleting old reports. Commit only when authorized, in short logical `feat:`, `fix:`, `build:` or `docs:` changes, and report visual/runtime evidence accurately.
