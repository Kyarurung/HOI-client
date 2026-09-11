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

## 국가 진행 음악

서버가 새로 발생한 국가 음악 신호를 해당 국가 플레이어에게만 보냅니다. 재접속·국가 변경·캠페인 교체 때 지난 신호를 다시 재생하지 않습니다. `StoryMusicAudio`는 등록된 `music.korea.*` 또는 `music.story.*` 이벤트만 받아 한 번 스트리밍합니다. 슈퍼 이벤트 뒤에 재생하고, 일반 플레이리스트보다 우선합니다. 사용자가 다른 곡을 선택하면 지정 음악을 중단합니다. 음악 음량을 0으로 둔 경우 지정 곡을 재생하지 않습니다.

`KoreanMusicChecks`에서 원본 `music.korea.extinction`의 실제 음원 재생과 MUSIC 채널, 재생 종료 정리를 확인합니다. 음원 전송·지연을 포함한 운영 서버 다중 접속 검사는 별도입니다.

## 육군 유닛 위치 효과음

`hoi:unit_audio_v1`은 서버가 선택한 가까운 음원 최대 3개와 차원·위치만 전달합니다. 12종 병종에 보행·트럭·장갑차·전차 이동 및 소총·기관총·기관포·전차포 전투의 9개 음향 유형을 배정합니다. 동일 위치에서 묶인 사단은 병종별로 한 음원을 사용합니다. 훈련이 끝나 실제 인력이 있는 부대만 소리를 내며 일시 정지 중에는 재생하지 않습니다.

서버와 클라이언트 모두 플레이어 위치에서 3차원 직선거리 7블록을 검사합니다. 외부 팩은 모노 음원과 감쇠 거리 7을 사용합니다. Minecraft의 적대적 생명체(HOSTILE) 효과음 음량을 따릅니다. 5서버틱마다 갱신하며, 이동·전투 종료나 범위 이탈 때 반복 예약을 취소합니다. 차원 이동·접속 종료·40클라이언트틱 동안 상태를 받지 못한 경우에도 정리합니다. 변경 없는 상태는 한 서버초마다 유지 신호를 보내고 같은 음원을 처음부터 재시작하지 않습니다.

전투음과 차량음은 설치된 TFR 원본입니다. 보병 발걸음은 TFR의 기본 게임 의존 음원입니다. 소총 10·11번은 TFR 파일의 010·011 표기와 원본 이벤트 정의가 달라 정상 연결되는 1~9번만 사용합니다. 음원 배정은 HOI의 12종 모델에 맞춘 것이며 원작의 모든 국가별 차량 엔진을 재현한 것은 아닙니다.

`UnitAudioChecks`는 실제 전차음 재생과 공간 감쇠, 반복 패킷에서 음원 재사용, 거리 이탈·정지·차원 변경 시 중단을 검사합니다. 단위 테스트는 잘못된 좌표·중복 ID·3개 초과 입력을 거절합니다. 실제 다중 접속 환경의 청취 품질은 별도 확인이 필요합니다.
