# External HOI/TFR audio

Audio is provided by HOI-resourcepack (`sound-catalog.json` / `sound-provenance.json`), never embedded in either JAR.
`hoi:audio_cue_v1` is a bounded, server-to-client enum. Older clients without the channel remain compatible.
Successful `/hoi select` requests the streamed, looping TFR `maintheme`. Successful `/hoi start` cancels a pending theme request and stops the playing theme before `start_game_01`. Rejected starts leave the theme alone. Disconnect and lobby close stop it; pack reload can resume it only while still selected in the lobby. Vanilla music is suppressed while the theme is requested and available. The theme follows Minecraft Music volume; interface cues follow Master volume.

Common menu buttons, eight research categories, technology details/start, national focus and division clicks use external original sound events. Research/focus completion is emitted only for the listener's current active assigned country, even with menus closed; joining/changing countries never replays old completions. Visible atlas division displays can be picked with the map selector.

Construction icons use original construction variants 01/02/03 for state/shared/province. Only accepted placement plays 04/05. This is an explicit HOI mapping: the installed source did not expose the original category-specific assignments. Army/navy/air/supply transitions use the events named by TFR `mapmodes_interface.gui`; the HOI construction atlas uses the original infrastructure map sound. Re-selecting the current map does not replay it.

Validation: `gradlew test runGameTest remapJar --console=plain --no-daemon` after building the external pack. Unit tests cover pending theme cancellation, stop-before-start, pack reload and private completion baselines. Client GameTests load real sound definitions and round-trip every cue. Renderer fixtures and server mock players do not establish multiplayer audio synchronization or subjective listening quality.
