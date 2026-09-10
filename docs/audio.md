# External HOI/TFR audio

Audio is provided by HOI-resourcepack (`sound-catalog.json` / `sound-provenance.json`), never embedded in either JAR.
`hoi:audio_cue_v1` is a bounded, server-to-client enum. Older clients without the channel remain compatible.
Successful `/hoi select` requests the streamed, looping TFR `maintheme` (타오르는 불길 - 주제곡, original `TheFireRisesMainTheme.ogg`). Successful `/hoi start` cancels a pending theme request and stops the playing theme before playing both `start_game_01` and `start_game_02` once. The samples have separate sound events so neither is randomly omitted. Rejected starts leave the theme alone. Disconnect and lobby close stop it; pack reload can resume it only while still selected in the lobby. Vanilla music is suppressed while the theme is requested and available. All HOI audio, including the lobby theme and super-event tracks, follows Master volume. Music at zero does not mute HOI audio; vanilla sound settings are not changed.

Common menu buttons, eight research categories, technology details/start, national focus and division clicks use external original sound events. Research/focus completion is emitted only for the listener's current active assigned country, even with menus closed; joining/changing countries never replays old completions. Visible atlas division displays can be picked with the map selector.

주 건물 / 공용 건물 / 지역 건물 palette choices use the ordinary `ui.click`, like division-template controls. Only accepted placement plays construction 04/05. Army/navy/air/supply transitions use the events named by TFR `mapmodes_interface.gui`; the HOI construction atlas uses the original infrastructure map sound. Re-selecting the current map does not replay it.

Validation: `gradlew test runGameTest remapJar --console=plain --no-daemon` after building the external pack. Unit tests cover pending theme cancellation, stop-before-start, pack reload and private completion baselines. Client GameTests load real sound definitions, round-trip every cue and verify the SELECT stream is active with Music muted, then stops on STOP. Renderer fixtures and server mock players do not establish multiplayer audio synchronization or subjective listening quality.

## 국가 선택·시작 소리

`/hoi select`는 대기 음악을 요청합니다. 국가 영토 우클릭 선택을 저장한 뒤 `COUNTRY_SELECT`로 `ui.country.select`를 한 번 재생합니다. 원본은 HOI4 `select_country` 이벤트의 `menu/click_mouse_over_03.wav`이며 외부 팩의 출처·해시 목록에 기록합니다. 거절된 선택은 선택 효과음을 내지 않습니다. 새 국가 선택 채널을 지원하는 클라이언트에만 새 효과음 enum을 보내 기존 클라이언트의 디코딩 오류를 방지합니다.

`/hoi ready`는 시작 효과음을 내지 않습니다. 전원이 준비한 뒤 `/hoi start`가 성공하면 중앙 선택 화면을 닫고 대기 음악을 중단한 다음 `ui.game_start`와 `ui.game_start_signal`을 각각 한 번 재생합니다. 거절된 시작 요청은 소리나 음악 상태를 바꾸지 않습니다. 현재 대기 음악은 Minecraft Master 음량을 따릅니다.
