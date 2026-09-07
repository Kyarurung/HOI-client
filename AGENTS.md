# Repository Guidelines

HOI-client is a Java 25, Minecraft 26.2 Fabric client mod. The Gradle modules are `client` and `protocol`. Keep `dev.hoi.client` and `dev.hoi.protocol`, stable `hoi:` payload IDs, and UTF-8 Java with four-space indentation.

Run `./gradlew test runGameTest remapJar --console=plain --no-daemon` before delivering code/build changes. PowerShell uses `.\gradlew.bat`. `runGameTest` invokes actual client rendering and requires a display (use Xvfb in Linux CI). The root output is only `build/libs/hoi-client-0.1.0-SNAPSHOT.jar`. Never downgrade Minecraft or introduce obsolete mappings; 26.2 is unobfuscated.

The server owns research, time, country assignments, validation and persistence. This client sends bounded requests and renders private snapshots. Do not add core simulation, server worlds, Polymer or client-side rewards. Call `ResearchProtocol.registerPayloadTypes()` before registering receivers; repeated calls must be safe. Synchronize shared protocol changes with Kyarurung/HOI's `protocol` module and bump incompatible wire versions.

Preserve all eight research categories, the 2020-01-01 campaign epoch, 10 server ticks/hour at X1 and PAUSED/X1..X5. Timeline labels are independent of the campaign epoch: infantry begins in 2000; armor uses 1980, 2018, 2020, 2022, 2024, 2028, 2032. Never invent unregistered technologies, equipment or battalion combat values from reference images.

Generated builds, runs, logs, credentials and caches stay out of commits. UI fixture GameTests are actual renderer checks, not proof of multiplayer end-to-end synchronization. Record limits in README.md. Use short `feat:`, `fix:`, `build:` or `docs:` commits. The linked HOI-server repository is the operational server folder; do not start or restart it without an explicit user request.
