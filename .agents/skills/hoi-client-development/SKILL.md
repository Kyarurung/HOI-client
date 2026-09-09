---
name: hoi-client-development
description: Implement and validate HOI-client screens, input, rendering and private protocol snapshots in the dedicated Fabric client repository.
---

Read [AGENTS.md](../../../AGENTS.md) for UI constraints and [README.md](../../../README.md) for current screens and validation limits. Read [docs/audio.md](../../../docs/audio.md) when changing sound playback.

Trace input through the bounded request and server response to the client snapshot and screen. Keep country state, rewards, time and persistence authoritative on the server. Reject stale private responses; do not let them reopen a revoked screen. Never register a client `/hoi` root.

Synchronize shared protocol sources with HOI, including registration and incompatible wire versions. Read existing layout helpers before changing screen geometry. Preserve source image aspect ratios, bounded drawing areas, tooltips and accessible names; unknown server values stay unknown. Art and sound remain in HOI-resourcepack.

For code/build delivery, first build the sibling resourcepack, then run `.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon` with Java 25. Rendering GameTests need a display; use Xvfb in Linux CI. Supply `-PhoiResourcePack=<absolute ZIP path>` if needed and inspect the client-only root JAR. Documentation-only edits need link and command checks.

Renderer fixtures verify actual drawing but do not prove multiplayer synchronization. Record the relevant limits in README. Install this JAR only in clients; keep HOI-server stopped unless the user explicitly requests startup.
