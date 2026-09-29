# Bobby 26.3 compatibility build

This is an unofficial local compatibility build of [Johni0702/Bobby](https://github.com/Johni0702/bobby), pinned to commit `47868a67f24f2dc2670cbbb22fd3e3363cb152a1` (Bobby 5.2.15). It is a separate client-only mod, not code embedded in the HOI client. Preserve the upstream authorship and LGPL-3.0-or-later license in LICENSE.md.

## Supported environment

Minecraft 26.3, Java 25, Fabric Loader 0.19.5 and Sodium 0.9.2. This build explicitly requires Sodium 0.9.2: native-renderer cache tests passed, but distant geometry was not visible in the native-renderer screenshot. Do not describe that combination as supported. Iris shaders and arbitrary remote servers have not been visually tested.

Ordinary multiplayer dimensions cache chunks actually received from the server. Unvisited terrain cannot be invented, entities are not kept alive, and cached blocks may be outdated until the server sends the chunk again. Cache keys retain upstream server, world seed and dimension separation. The upstream dynamic multi-world option remains available for servers with multiple identically named worlds. Existing render distance, GUI scale and other user options are preserved. Singleplayer caching remains opt-in through Bobby's view-distance-overwrite setting.

HOI `hoi:atlas_*` dimensions do not create a Bobby chunk cache. Their private, changing country colours continue to use HOI's server-projected distant terrain instead of stale disk chunks.

## Changes

`26.3.patch` updates the build dependencies, chunk/light packet record accessors, relocated GPU buffer type, consolidated ClientChunkCache packet application and the injection descriptors. It also excludes HOI atlas dimensions and adds a client GameTest. The mod version/name identify this local port. This is not an official upstream 26.3 release.

## Reproduce

Use a new directory and Java 25:

```powershell
git clone https://github.com/Johni0702/bobby.git bobby-26.3
cd bobby-26.3
git checkout 47868a67f24f2dc2670cbbb22fd3e3363cb152a1
git apply C:/HOI-client/third_party/bobby/26.3.patch
.\gradlew.bat build runClientGameTest --console=plain --no-daemon
```

The generated JAR is `build/libs/bobby-5.2.15+mc26.3.jar`; its metadata version is `5.2.15+mc26.3-hoi.1`. Installation uses the latter version in the filename. `provenance.json` records the delivered patch, JAR and corresponding-source archive hashes. The full corresponding source is supplied alongside the JAR in HOI-client/build/bobby. The upstream publishing tasks must not be used for this local fork.

## Validation

The actual client GameTest verifies server-distance-2/client-distance-16 terrain retention, disk cache read, eviction/reload, replacement by live chunks, live block updates, dimension cache separation and HOI atlas exclusion. Sodium screenshots show the gold terrain marker about 160 blocks away after following a continuous received-chunk path. Teleporting across never-received intermediate chunks can leave the isolated cache invisible; this is not evidence that unseen terrain was cached. Tests use an integrated test server and do not establish arbitrary multiplayer/server-plugin compatibility. Test-only config, dimension and classes are excluded from the release JAR. No FPS/MSPT improvement is claimed.
