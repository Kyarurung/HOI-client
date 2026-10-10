# TFR 음악과 효과음

음원은 HOI-resourcepack의 `sound-catalog.json`·`sound-provenance.json`으로 관리하며 서버·클라이언트 JAR에 넣지 않는다. `hoi:audio_cue_v1`은 서버에서 클라이언트로 보내는 제한된 효과음 enum이다. 해당 채널을 지원하지 않는 이전 클라이언트에는 보내지 않는다.

## 기본 플레이리스트와 특별 음악

국가에 관계없이 TFRKP Music Pack(3779650567)의 한국 플레이리스트 81곡을 기본 배경 음악으로 순차 반복한다. 국가 HUD를 처음 받거나 선택·시작 신호를 받으면 재생을 활성화한다. HUD 오른쪽 부채 표시 아래의 원본 음악 아이콘으로 곡 선택 창을 연다. `/hoi-music`도 같은 창을 연다. 마우스 선택은 국가 메뉴 등 커서가 있는 HOI 화면에서 가능하다.

선택 단계에서 타오르는 불길 주제가를 한 번 재생하며 `/시작`는 이를 중단하지 않는다. 서버의 새 중점·이벤트 음악이나 슈퍼 이벤트 음악은 현재 음악을 교체한다. 팝업을 닫아도 특별 음악은 계속 재생하고, 곡이 끝나면 한국 플레이리스트로 돌아간다. 이전 특별 음악을 다시 재생하는 대기열은 없다. 곡 선택 창에서 다른 곡으로 교체할 수 있다. 재접속·국가 변경 시 지난 중점 음악을 소급 재생하지 않는다.

육군·해군·공군·보급·건설 지도 모드가 바뀌어도 현재 곡과 재생 위치, 수동 일시정지·정지 및 방송국 OFF 상태를 유지한다. 지도 모드는 차원 이동을 사용하므로 Minecraft의 월드 교체 시 전체 음원 중단 경로에서 기존 플레이리스트·중점·슈퍼 이벤트 인스턴스와 오디오 채널을 보존한다. 이전 차원의 다른 소리와 지연 재생 요청은 정리하며 접속 종료·리소스 재로딩의 전체 정리는 유지한다. 지도 전환은 해당 효과음만 재생하며 특별 음악과 대기 요청에도 영향을 주지 않는다. 같은 지도 모드를 다시 선택해도 음악이나 전환 효과음을 다시 내지 않는다. `/종료`, 접속 종료, 월드 이탈은 재생과 대기 요청을 정리한다.

플레이리스트·주제가·중점 지정 음악·슈퍼 이벤트·인터페이스 효과음은 모두 Minecraft의 Master 음량을 따른다. 음악 슬라이더가 0%여도 Master 음량이 켜져 있으면 재생한다. Minecraft 음량 설정 자체를 변경하지 않는다. 원본 곡은 외부 팩에서 스트리밍하며 실제 HOI 음악을 재생할 때 기본 Minecraft 음악을 중단한다.

현재 실행 가능한 국가 음악 매핑은 HOI의 `content/hoi/scenario/tfr-music.json`에 등록된 26곡이다. 미등록 원본 곡을 한국 플레이리스트 곡으로 대체하지 않으며 해당 효과는 기존 미지원 검사에 걸린다. 슈퍼 이벤트 음원 연결은 25개다. 원본 자료 수집과 음원 연결만으로 모든 이벤트·중점 실행을 구현한 것은 아니다.

재생 순서는 사용자가 지정한 81곡 목록을 그대로 따른다. OST 5곡(만주 전선·약속된 미래·예정폭심·우리의 청춘·전선을 간다)이 앞이고 마지막은 한로로 입춘이다. `HOI-resourcepack/korean-playlist-order.json`의 명시적 순서·곡명을 가져오며 자동 재정렬하지 않는다. 기존 한국 음악팩은 선택 목록에서 제외하되 이미 변환한 Ogg 파일과 중점용 이벤트 연결은 보존한다. 음악 창은 TFR 원본 헤더·재생 버튼·목록 및 TFRKP 방송국·앨범 이미지를 사용한다. 원본에 앨범 이미지가 없는 곡은 다른 앨범 이미지를 임의로 넣지 않는다.

## 선택·시작·지도 효과음

국가 영토를 우클릭해 선택을 저장한 뒤 `COUNTRY_SELECT`로 `ui.country.select`를 한 번 재생한다. 원본은 HOI4 `select_country`의 `menu/click_mouse_over_03.wav`이다. 거절된 선택은 효과음을 내지 않는다.

`/준비`는 시작 효과음을 내지 않는다. 전원이 준비한 뒤 `/시작`가 성공하면 중앙 선택 화면을 닫고 `ui.game_start`와 `ui.game_start_signal`을 각각 한 번 재생한다. 두 원본 표본을 별도 이벤트로 두어 무작위 선택으로 하나가 누락되지 않게 한다. 거절된 시작 요청은 소리나 음악 상태를 바꾸지 않는다.

공통 메뉴, 연구 분류·상세·시작, 국가중점·사단 선택은 원본 효과음을 사용한다. 연구·중점 완료는 현재 국가의 새 완료만 개인별로 알린다. 완료 팝업이 처음 추가될 때 원본 완료음을 Master 채널로 한 번 재생하며, 같은 팝업의 갱신·중첩 해제에서는 다시 재생하지 않는다. 건설 종류 선택은 일반 `ui.click`, 승인된 설치는 원본 건설 04/05를 사용한다. 육군·해군·공군·보급 전환은 TFR `mapmodes_interface.gui`, 건설 지도는 원본 기반시설 지도 소리를 사용한다.

## 유닛 효과음과 검증

유닛 효과음은 실제 배치·인력이 있는 부대의 이동·전투 상태를 사용한다. 3차원 거리 7블록 이내에서 가까운 음원 최대 3개를 전달한다. 5틱마다 변화, 변화가 없으면 20틱마다 유지 신호를 보낸다. 거리 이탈·정지·차원 전환·접속 종료·40클라이언트틱 상태 만료 때 반복을 취소한다. TFR 전투·차량음과 TFR이 의존하는 기본 게임 발걸음을 사용한다. 소총 변형 10·11은 원본 파일명 불일치로 제외한다. 다른 HOI 소리와 동일하게 Minecraft Master 음량을 따른다. 향후 추가되는 HOI 소리도 Master 채널을 사용하며, 유닛의 7블록 거리 제한은 유지한다.

외부 팩을 빌드한 뒤 서버·클라이언트 각각 `gradlew test runGameTest remapJar --console=plain --no-daemon`을 실행한다. 단위 테스트는 재생 순서·곡 종료·중단·리로드와 제한된 패킷을 검사한다. 클라이언트 GameTest는 실제 음원 로딩·재생과 다섯 지도 모드 전환 전후 동일 곡·오디오 채널 유지, 수동 일시정지·정지·방송국 OFF, 특별 음악 우선순위 및 운영 서버와 분리된 화면을 검사한다. 서버 mock 플레이어와 클라이언트 fixture는 운영 멀티플레이의 음향 동기화·주관적인 청취 품질 검증을 대신하지 않는다.

슈퍼 이벤트 음악은 팝업이 일시 정지 메뉴 뒤에서 대기하더라도 즉시 시작합니다. 수동 곡 선택·지도 변경·새 국가 중점 음악은 재생 중인 슈퍼 이벤트를 끊지 않습니다. 국가 중점 음악은 해당 국가에 할당된 각 플레이어에게만 전달하고 슈퍼 이벤트 중에는 대기합니다. 일반 플레이리스트의 선택·정지·음량은 클라이언트별 상태이며 다른 플레이어에게 전송하지 않습니다. 음량 슬라이더는 클릭·드래그 시 조작음을 재생하지 않습니다. 메뉴 버튼 표기는 플레이리스트 목록입니다.

MusicRoutingChecks와 DialogScreenChecks는 실제 Minecraft SoundManager의 중단·복귀 및 팝업이 대기하는 경우를 검사합니다. 서버의 SourceMusicProgressTest는 두 명의 한국 플레이어와 다른 국가의 전달 상태를 분리하여 검사합니다. 운영 서버의 실제 다중 접속 검사는 수행하지 않았습니다.

플레이리스트 재생 버튼은 TFR의 GFX_musicplayer_play_pause_button 두 프레임을 사용합니다. 재생 요청 중에는 일시정지, 정지·일시정지 상태에서는 재생 아이콘과 툴팁을 표시합니다. 일시정지는 선택 곡의 OpenAL 채널만 멈추고 재개하므로 다른 효과음을 멈추지 않습니다. 일시정지 중에는 자동으로 다음 곡으로 넘어가지 않습니다. 본문 재생 표시는 기존 위치에서 2GUI 왼쪽에 배치합니다. PlaylistPlaybackTest와 KoreanMusicChecks는 같은 스트림의 일시정지·재개 및 아이콘 전환을 검사합니다.

수동 일시정지·정지는 음악 창을 닫거나 지도 모드·HUD가 갱신되어도 유지한다. Minecraft의 전체 소리 재개 뒤에도 일시정지한 플레이리스트 채널은 다시 멈춘다. 곡 선택·재생·방송국 켜기는 명시적으로 재생을 다시 시작하며, 월드 이탈 시 수동 상태를 초기화한다. KoreanMusicChecks는 화면 종료와 SoundManager.resume 이후 실제 오디오 채널이 재생되지 않는지 검사한다.

국가 중점 음악 재생 중에는 플레이리스트의 재생 버튼도 선택된 곡으로 전환한다. 이때 버튼은 일시정지가 아닌 재생으로 표시한다. 곡 직접 선택·이전 곡·다음 곡도 중점 음악을 중단하고 선택한 곡을 재생한다. 슈퍼 이벤트 음악의 우선순위는 유지한다. AudioMusicRoutingChecks는 실제 음악 창의 재생·곡 선택 버튼과 SoundManager의 전환을 검사한다. 플레이리스트 기본 음량은 기존 100%를 유지한다.

음악 변경 검증은 `./gradlew test runGameTest remapJar -PhoiTestSuite=audio --console=plain --no-daemon`으로 실행한다. 전체 화면 순회 대신 기존 AudioMusicRoutingChecks를 실행하며 실제 네더·오버월드 왕복에서 선택 곡·채널·수동 일시정지·중점·슈퍼 이벤트 유지와 이전 차원 소리 정리를 검사한다. 운영 서버의 지도 차원·다른 클라이언트 모드 조합에 대한 접속 검증은 별도다.
첩보 개선 버튼은 원본의 정보·정보 순환·수집 분야·휴민트·암호 분류별 효과음을 사용하며 비활성 개선은 소리를 내지 않습니다. 생산 분류·구형 표시·자원/함종 필터는 원본 체크박스 소리, 공장 할당 칸은 `click_scroll`, 닫기 표시는 닫기 소리를 사용합니다. 음원 원본 경로와 변환 이력은 리소스팩의 기존 sound-imports와 sound-provenance에 보존합니다.

## Playlist volume across music channels

The playlist slider persists in the client `config/hoi-audio.properties` and applies to playlist tracks, national theme/story music, focus completion cues and super-event music. Its default is 100%; invalid or nonfinite persisted values fall back to the default. Every active routed sound reads the live gain, including silent-start and unmute, while Minecraft's Master gain is applied once by the native sound engine. Ordinary interface clicks and research effects keep their existing gains. Reopening menus and reconnecting do not reset this local preference.

`AudioPlaylistVolumeTest`, `AudioPlaylistGainTest` and `ScreenDialogVolumeTest` check persistence and actual OpenAL channel/listener gains at zero, 35% and full volume, live changes, repeated packets, mismatched cancellation, stopped-channel cleanup and focus completion lifecycle. These are local automated playback checks; they do not replace listening on the user's audio device.

육해공 지도 부대의 좌클릭 선택·드래그 선택, 군/함대 카드와 상세 목록 선택은 기존 `ui.division.select`를 입력당 한 번 재생한다. 우클릭 사단 검사에서 보내던 `DIVISION_SELECT`는 제거했다. 이동·위치 보기의 별도 피드백, 지도 전환음과 반복 유닛 음향은 유지한다. 렌더·선택 상세 동기화는 선택음을 재생하지 않는다.
