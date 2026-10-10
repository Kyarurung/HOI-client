# 화면과 표시 기준

[README](../README.md) · [구조와 검증](refactoring.md) · [음악](audio.md)

## 공통 기준

- GUI2를 크기·간격·정렬의 기준으로 사용하며 다른 배율·창 크기는 호환성을 검사합니다.
- 원본 이미지의 비율·프레임·투명도를 보존합니다. 사진·아이콘·텍스트는 각 셀의 경계 안에 표시합니다.
- 한국어는 TFR 한글패치의 키와 국가별 덮어쓰기를 따릅니다. 명령·저장 ID는 번역하지 않습니다.
- 툴팁의 원문 색상·빈줄·소수 자릿수·아이콘을 유지하고 단독 `---`는 빈줄로 표시합니다. 효과 수치는 서버 계산을 사용합니다.
- 미상 값을 화면에서 임의 추정하거나 게임 상태를 초기화하지 않습니다. 원본 참고 설명과 실제 적용되는 효과를 구분합니다.
- 상단 HUD 툴팁은 왼쪽 정렬로 전체 내용을 표시하며 스크롤 안내나 행 수 제한을 넣지 않습니다. 패널별 긴 설명의 스크롤 규칙과 구분합니다.

## 국가 메뉴와 HUD

메뉴 순서는 국가 정보·결정·정보기관·연구·무역과 경제·건설·생산·모병 및 배치·군수·장교단입니다. 군종 경험치는 육군·해군·공군 순서입니다. 날짜·배속은 서버의 게임 시간을 한국어로 표시합니다.

정치 화면은 원본 프레임 안에 지도자 사진과 이름판을 배치하고 국민정신은 왼쪽부터 가로 스크롤합니다. 국가 중점은 제목 영역을 눌러 선택합니다. 정부·장관·군사참모는 소형 철제 프레임 초상화를 우선하며 원본이 없으면 빈 프레임을 사용합니다. 국민정신·지도자 설명은 서버가 제공한 현재 상태만 표시합니다.

정당 지지도는 목록과 상단에서 정수, 정치·외교 툴팁에서 소수점 두 자리로 표시합니다. 원문에서 정한 경제·인력·안정도 등의 개별 정밀도를 공통 반올림으로 덮어쓰지 않습니다. 연료가 최대 수용량에 도달하면 원본 `FUEL_STOCKPILE_MAX_SHORT`의 `가득 참` 표시가 일수 예측보다 우선합니다.

국가·글로벌·슈퍼 이벤트와 연구·중점 완료는 서버가 발급한 수신 범위와 선택지만 표시합니다. 종료·권한 회수 후 도착한 응답으로 화면이나 HUD를 다시 열지 않습니다. 이미지 카탈로그 전체가 실행 가능한 이벤트나 정치 루트 전체를 의미하지 않습니다.

## 연구·건설·산업

연구는 서버가 제공하는 실제 슬롯과 선행 조건·배타 선택·혜택을 사용합니다. 트리는 원본 좌표와 지정된 보정 배치를 따르며, 카드·상세·하위 기술은 국가별 이미지와 원문 키를 공유합니다. 상세의 원작 장비 참고값은 서버의 적용 효과와 구분합니다. 연구 슬롯 칸은 호버 툴팁 없이 표시하며 기술 노드·카테고리·혜택 툴팁과 접근성 이름, 슬롯 클릭 동작은 유지합니다.

건설은 서버가 발급한 주·프로빈스·철도 선택지에 요청합니다. 예약·공장 배정·수리·전력은 서버가 계산합니다. 공장 선택의 호버 미리보기는 상태를 바꾸지 않으며 온전한 원본 아이콘으로 표시합니다. 주 상세의 수치·진행 게이지·변동치도 서버 응답을 사용합니다.

생산 장비·라인은 계열별 국가 장비 사진과 이름을 표시합니다. 실제 부족 자원은 서버의 `missingResources`로 판단하며 해당 숫자와 아이콘만 붉게 표시합니다. 구형 장비의 원본 붉은 마스크는 사진·이름 영역에 적용하고 자원·버튼 영역은 제외합니다. 자원 옆 역할 아이콘은 설계창의 종류 아이콘과 같은 원본 계열을 사용합니다.

군수는 계열별 생산량·수요·균형·비축량을 표시합니다. 충원 수요가 0이면 충족률은 100%이며 수요 정보가 없는 구버전 응답은 빈 게이지로 남깁니다. 무역·모병·편제 조작은 서버가 발급한 선택지와 비용만 사용합니다. 미등록 대대나 참고 표의 내구도부터 장비까지의 수치를 임의로 추가하지 않습니다.

## 기갑·해군·공군 설계

해군 설계창은 이름 바로 아래 분류칸을 표시하지 않으며 닫기 버튼은 기존 위치에서 왼쪽으로 1 GUI 이동합니다. 기갑·공군 종류 칸과 모든 설계도면 내부 종류 라벨은 유지합니다. 장비 사진 옆과 저장된 설계의 생산 선택 이미지에는 작은 설계도 배지를 표시하지 않습니다. 새 차체·함체·기체와 미완성 장비의 배지는 유지합니다. 설계도면 자체와 장착 부품의 강조 레이어는 그대로 표시합니다.

초기 생산 가능 장비는 서버가 제공한 원본 국가 파생형의 부품을 장착한 상태로 엽니다. 서버가 설계 구성을 제공하지 못한 일반 장비를 클라이언트가 임의 설계로 만들지 않습니다. 초기화는 열 당시의 구성으로 돌아가며 화면 조회는 국가 자산이나 경험치를 변경하지 않습니다. 저장은 서버의 국가 설계 저장이며 파일로 저장·불러오기 버튼은 제공하지 않습니다.

서버는 연구·슬롯·군종·필수 부품 조건과 경험치 비용을 검증합니다. 기존 장착 부품은 선택 목록에 표시하되 새 부품의 연구 조건을 우회하지 않습니다. 공통 계산에서 군종별 수식과 조정치를 분기하고 화면은 발급된 수치와 차이를 표시합니다. 해군 생산 비용은 항상 `navprod_cost`를 사용합니다.

해군과 공군의 여러 분류 슬롯은 분류 → 부품 선택 순서입니다. 기갑의 단일 선택 계열은 해당 부품 목록으로 바로 엽니다. 기갑·공군에는 장착 모듈 제거 버튼이 없으며 공군 상단 슬롯 옆 역할 실루엣도 표시하지 않습니다.

능력치·슬롯·분류·부품·저장·경험치 툴팁은 `equipment_design_details.json`에 기록된 원문 키를 사용합니다. 부품 가산/배율, 연구 조건, 비용, 평균 관통, 임무 허용과 임무별 효과를 구분합니다. 공군 조정치 목록은 현재 주무장과 보조무장이 허용하는 임무에 따라 달라집니다. 임무 미리보기 선택 자체는 국가 상태를 변경하지 않습니다.

해군 조정치는 기갑의 좌우 기준선 안에서 좌측 상단부터 배치합니다. 작은 아이콘은 프로펠러 이동, 검·방패의 공격/방어, `…` 환경요소 순서이며 각 묶음의 효과를 툴팁으로 표시합니다. 단순 호버는 기본 커서를 유지하고 선택 가능한 공군 임무만 버튼 커서를 사용합니다. 보조함·주력함 설명은 원본 함종·효과·모듈 선택 안내의 세 문단과 강조색을 보존합니다.

## 지도·음향과 검증 한계

지도는 서버 장면을 타일별 메시와 표시 객체로 렌더링합니다. 프로빈스 경계는 회색, 주·해역·공역·해안 경계는 검은색입니다. 공통 거리 기준에는 높이 차이도 포함합니다. 지도 국가명 전용 글꼴은 지형 투과와 물 위 표시를 함께 처리하며 다른 글꼴에는 적용하지 않습니다.

재생 채널·곡 유지·차원 이동·전투와 부대 음향의 범위는 [음악 문서](audio.md)를 따릅니다. 원본 음원은 외부 팩에서 읽습니다.

`ScreenIndustryChecks`는 서버 fixture로 설계 슬롯·부품 요청·초기화·저장·조정치·툴팁과 작은 창의 경계를 검사합니다. 지도·연구·정치·음악은 각 기능의 GameTest를 사용합니다. 이 검사는 실제 클라이언트 렌더러를 사용하지만 운영 서버 접속, 모든 원본 화면의 픽셀 단위 일치, 다중 사용자 동기화, 추가 렌더링 모드 조합을 보증하지 않습니다. 군수산업체 개량·일반 장비 파생형 편집 등 서버에 연결되지 않은 원작 기능은 별도 구현 대상입니다.

해역·공역 이름은 서버가 생성한 전체 구역 배치 fixture에서 한국·동해·동중국해·셀레베스해·자와해·오스트레일리아 북부를 GUI2로 렌더링합니다. 글꼴·회전·크기·간격과 섬 위 겹침을 실제 렌더러에서 확인하며, 운영 서버에서 시선에 따라 표시를 전환하는 검증은 별도입니다.

## 지도 부대 표식

육군·해군·공군 표식은 TFR `interface/mapicons.gui`의 `unit_counter`, `unit_counter_navy`, `air_wing_counter`와 원본 DDS를 GUI2 기준 1:2로 표시합니다. 회색 원본 바탕과 육군 일반 병종 그림에 서버가 판정한 아군 초록색·동맹 파란색·적군 빨간색·제3국 회색을 적용하고 병종 뒤 검은 overlay는 별도로 유지합니다. 색상은 원본 `COUNTER_MODE_ALLEGIANCE_*` 값이며 `hoi:unit_hud_v4`로 전달합니다. 국기와 조직력·내구도 막대는 고유 색을 유지합니다. 관계 갱신은 기존 표식 가시성을 넓히지 않으며, 국가 미배정 관전자는 회색으로 봅니다. 네 관계의 GUI2 캡처와 프로토콜 왕복을 검증합니다. NATO 프레임은 사용하지 않습니다.

육군은 일반 병종 그림 아래 조직력·내구도 2줄과 그 왼쪽을 덮는 국기, 오른쪽 수량을 표시합니다. 해군은 주력함·보조함 수를 각각 위·아래에 표시하고 조직력·내구도와 국기를 아래 칸에 둡니다. 공군은 조직력 없이 하단 내구도 막대와 국기를 표시합니다. 같은 위치의 비행단은 각각 한 줄로 쌓습니다. 수치·상태는 서버의 비공개 `unit_hud_v4` 응답을 사용합니다.

`runGameTest -PhoiTestSuite=unit-hud`는 실제 클라이언트 렌더러로 GUI2·작은 창·사이드바·카메라 뒤 표식 제외·스냅샷 만료를 확인합니다. 원본 비교는 생성된 `hoi-unit-hud-counters-and-hover-gui2` 스크린샷을 사용합니다. 이 검사는 고정 데이터 검증이며 운영 서버의 다중 플레이 실측 검증은 아닙니다.

지도 부대 표식은 원본 `gfx/flags/small`의 10×7 국기를 사용한다. 작은 국기가 없는 국가만 기존 국기를 사용한다. 해군·공군 수량은 원본 배치보다 GUI2 기준 1GUI 아래에 표시하고, 육군 수량은 원본 높이를 유지한다.

## 반응형 GUI 단위

생산 라인은 충원·개선·비축의 원본 아이콘과 별도 툴팁, 장비 역할 배지를 표시한다. 공군 생산 선택 행에는 설계에서 가능한 임무만 원본 작은 임무 아이콘으로 표시한다. 장비 능력치·설명은 서버의 실제 장비/설계 값과 원본 설명을 사용하며 줄 순서·빈 줄·색상·소수점 정밀도를 보존한다. 주 변동치에도 같은 원본 간격 처리를 적용한다. 원본의 일일 순손익·충족 예상일은 필요한 손실 이력이 없으므로 현재 생산량·부족 수량으로 구분해 표시한다.

생산 수요와 공통 계열 부족량은 스냅샷마다 한 번 집계해 모든 라인에서 공유한다. 장비 E개·생산 라인 L개에서 반복 집계 O(E×L)를 O(E+L)로 줄이고 O(E) 인덱스를 사용한다. 스냅샷이 바뀔 때 다시 계산하며 알려지지 않은 수요를 0으로 바꾸지 않는다. 책임 분리만으로 성능이 개선됐다고 판단하지 않는다.

설계창의 배경 생산 라인은 수동 렌더링 목록에 유지하고 입력 위젯에서는 제외한다. `-PhoiTestSuite=designers`는 GUI2 생산 이미지의 픽셀 유지, 배경 버튼 입력 차단, 한국·중국 공군 임무 목록과 중국 휴대용 대공 미사일 사진을 검사한다. 전체 `runGameTest`는 기존 화면·권한·입력 회귀 범위를 유지한다. 이 검증은 서버에서 생성한 스냅샷을 사용하는 실제 클라이언트 렌더링이며 운영 멀티플레이 접속 검증은 아니다.

공통 `UiUnits`는 2560×1440 화면의 GUI2 좌표인 1280×720을 기준으로 한다. 현재 GUI 뷰포트로 `new UiUnits(width, height)`를 만들고 `scale()`로 균일 배율, `length(n)`으로 반응형 nGUI 길이, `size(n)`으로 그 길이를 포함하는 정수 영역을 얻는다. 배율은 `min(width / 1280, height / 720)`이며, 화면 비율이 바뀌어도 가로·세로를 다르게 늘리지 않는다. 음수 좌표 이동에는 `length`를 사용하고, 아이콘 크기·행 높이·마우스 영역에는 `size`를 사용한다.

지도 부대 HUD에 이 단위를 적용했다. 국기·아이콘·조직력·내구도·숫자를 한 배율로 그리며, 같은 배율의 영역을 행 쌓기와 툴팁 판정에 사용한다. 육군 숫자 높이와 해군·공군의 1GUI 하향 조정은 기준 화면에서 유지된다. 화면 좌표 투영과 사이드바 가림 영역은 실제 뷰포트 기준이다. 기존 메뉴의 개별 레이아웃은 아직 이 단위로 전환하지 않았다. 사용자의 Minecraft GUI 배율 설정은 변경하지 않는다.

`UiUnitsTest`는 기준 크기, 세로·가로 화면과 GUI 좌표 환산을 검사한다. `-PhoiTestSuite=unit-hud` 렌더링 검사는 GUI2의 2560×1440, 1920×1080, 2560×1080, 1080×1440과 GUI3의 1920×1080에서 그림과 마우스 영역을 확인한다. 운영 서버의 다중 사용자 동기화 확인과는 별도다.

지도 위 표식을 좌클릭하면 원본 `onmap_unit_counter_selected.dds`(육군·공군) 또는 `onmap_mission_unit_counter_selected.dds`(해군) 외곽선을 표시한다. 자유 커서와 화면 중앙 조준점의 좌클릭 모두 같은 표시 영역을 판정한다. 선택은 클라이언트의 일시적인 상태이며, 동일 부대의 스냅샷 갱신에는 유지하고 빈 지도 클릭·부대 제거·차원 변경·스냅샷 만료·접속 해제에는 지운다. 가려진 사이드바와 숨긴 HUD는 선택할 수 없다. 이 선택만으로 서버의 부대 이동·임무가 변경되지는 않는다.

Completion details and menu responses replace content through `ScreenDialogCoordinator.setContent`, preserving the current event stack and unresolved event tokens. Old content is removed once, and clearing content reveals the same event. Mouse regressions assert the exact issued action token, revision and choice rather than treating a click sound as proof of a server action. Server acknowledgements remain authoritative. The separate server queue tests and client rendering checks do not constitute operational multiplayer verification. Agency navigation is covered, but this does not add an agency-upgrade completion popup producer.

Production role badges are limited to air defense, tank destroyers and artillery (including rockets). Their GUI2 offsets are 3 units left and 1 down; line equipment pictures move 1 unit down. Reinforcement, upgrade and reserve indicators move 1 unit down with their original horizontal position; the right-aligned stock count below moves 1 unit left. Blueprint and unfinished chassis show their blueprint marker without a role badge. The obsolete conversion marker beside the destination icon and its tooltip are absent; the actual efficiency gauge in the line header remains. These offsets use `UiUnits` without changing the player's GUI scale.

The production-source and production-china fixtures are generated server snapshots. Existing industry/designer fixtures retain their regression scenario state, including unlocks, stock, conversion inputs and trade partners, while equipment presentation fields are refreshed from the source import. These fixtures are renderer inputs, not migrated campaign saves.

## 군수 툴팁과 해군 배치

군수의 평균 생산 효율·장비 유형·생산·상태·수요·균형·비축량·자원 헤더는 원본 LOGISTICS_* 제목/설명 키를 사용합니다. 제목의 금색, 생산 효율 아이콘과 문단 사이 빈 줄을 유지하고 장비 유형은 제목만 표시합니다. 행의 각 수치와 자원 아이콘은 별도 호버 영역을 가집니다. 일일 손실 이력이 없는 현재 모델에서 행별 수요·균형은 현재 미충원 요구량과 재고 차액이며 원작의 일일 손익 통계로 꾸미지 않습니다.

완성된 함정은 생산 라인의 이미지 게이지를 100%로 유지합니다. 별도의 완성 함선 버튼은 없습니다. 인력이나 항구가 부족하면 원본 PRODUCTION_CAN_NOT_DEPLOY 및 해당 사유 키로 툴팁을 표시하고, 국가 인력이 확보되면 다음 서버 시간 처리에서 지정 항구에 자동 배치합니다. 생산 라인별 배치 대상은 서버에 저장합니다. 수리 대기열의 개별 항구 펼침은 전체 펼침/접기 버튼의 모드를 변경하지 않습니다.

ScreenIndustryChecks는 8개 군수 헤더의 원문·색상·빈 줄 및 GUI2 캡처와 수리 개별/전체 버튼 순서를 검사합니다. ScreenNavalDeploymentTest는 GUI2 가득 찬 생산 게이지·원본 인력 부족 툴팁·선택 항구와 전송 요청을 검사합니다. 실제 운영 서버 연결이나 모든 원본 화면의 픽셀 단위 일치를 의미하지 않습니다.

해군 생산 대기 화면만 재현하려면 `./gradlew runGameTest -PhoiTestSuite=naval-production`을 실행합니다. fixture의 국가 인력과 HUD 인력은 같은 99명으로 설정합니다.

The agency sidebar uses a 270-unit GUI2 baseline. Its five groups and 23 upgrades use original source sprites, per-group upgrade sounds and server-generated rich tooltips. The sidebar indicator and detail button share each item's tooltip. Current and next level effects preserve explicit decimal precision, source icon tokens, colors and blank lines. The agency render suite accepts server-generated fixtures through HOI_AGENCY_PREVIEW; synthetic rendering is separate from operational multiplayer verification. The player's GUI scale is never changed by the screen.

Agency names render bold at 115% of the normal label size. The upgrade-window heading is bold at its existing size and centered position. The five branch headers remain left-aligned and are vertically centered in their 14-unit strips. Each civilian-factory icon stays above its cost and is horizontally centered in the same 13-unit column as the number. The cryptology header has separate cipher-strength and decryption-power hover targets with source icon tokens, paragraph spacing and per-field precision. Values come from the private server view and share the simulation calculation. The source fixture suite captures both tooltips and the complete upgrade window at GUI2; other GUI scales remain compatibility checks. Creation and upgrade factory costs use white when sufficient and red when insufficient.

Production rendering fixtures are generated by HOI's FabricProductionEquipmentTest and bundled with the client tests. The default suite includes freight trains II/III, floating harbors and both powered exoskeleton models; it also checks train/exoskeleton successor obsolescence without HOI_PRODUCTION_PREVIEW. That variable only overrides fixture input for source comparisons. Disconnect callbacks schedule private screen, HUD, map and audio cleanup on the client thread. Menu and dialog regression checks originate outside that thread, verify removal on it, and reject delayed replies from the cleared menu session. Run `./gradlew runGameTest -PhoiTestSuite=disconnect --console=plain --no-daemon` to isolate the disconnect regressions; the default full suite retains both checks. These fixture checks do not replace operational multiplayer verification.

Validation: the focused disconnect suite and the full client `test runGameTest remapJar --console=plain --no-daemon` command passed with the bundled production fixtures. All 104 client JUnit tests passed. The final client JAR contains all seven rebuilt disconnect handlers, and its 170 nested protocol classes match the validated server JAR. GUI2 rendering and original armored-train pixels were checked; operational multiplayer and deployment were not performed.

## UI preservation constraint

All existing and future UI layouts and cells use Minecraft GUI scale 2 (GUI2) as the reference for sizing, spacing, alignment, text, icons, gauges and primary visual validation. Preserve the user-defined per-cell alignment rules; GUI2 is the layout baseline, not permission to overwrite the player's GUI scale setting. Other scales and viewport sizes remain compatibility checks.

## Localisation constraint

Current and future Korean UI text and tooltips follow the TFR Korean translation patch (workshop 3411756715). Resolve original localisation keys before writing translations; use TFRKP-specific Korean names only where the patch has no matching key. Preserve source-key provenance and command/save IDs.

## Client implementation constraints

HOI-client is a Java 25, Minecraft 26.3 Fabric client mod. The Gradle modules are `client` and `protocol`. Keep `dev.hoi.client` and `dev.hoi.protocol`, stable `hoi:` payload IDs, and UTF-8 Java with four-space indentation. Project-owned test classes use the `Test` suffix, including GameTest implementations; preserve their framework interfaces, registration, source sets and execution tasks.

Run `./gradlew test runGameTest remapJar --console=plain --no-daemon` before delivering code/build changes. PowerShell uses `.\gradlew.bat`. `runGameTest` invokes actual client rendering and requires a display (use Xvfb in Linux CI). The root output is only `build/libs/hoi-client-1.0.0+26.3.jar`. Never downgrade Minecraft or introduce obsolete mappings; 26.3 is unobfuscated.

The server owns research, time, country assignments, validation and persistence. This client sends bounded requests and renders private snapshots. Do not add core simulation, server worlds, Polymer or client-side rewards. Call `ProtocolResearchPayloads.registerPayloadTypes()` before registering receivers; repeated calls must be safe. Synchronize shared protocol changes with Kyarurung/HOI's `protocol` module and bump incompatible wire versions.

Preserve all eight research categories, the 2020-01-01 campaign epoch, 144 game minutes per 20 server ticks at X1 (2.4 game hours per real second at 20 TPS) and PAUSED/X1..X10 (preserve legacy save compatibility without rewriting source files). Timeline labels are independent of the campaign epoch: infantry begins in 2000; armor uses 1980, 2018, 2020, 2022, 2024, 2028, 2032. Never invent unregistered technologies, equipment or battalion combat values from reference images.

Generated builds, runs, logs, credentials and caches stay out of commits. UI fixture GameTests are actual renderer checks, not proof of multiplayer end-to-end synchronization. Record limits in README.md. Use short `feat:`, `fix:`, `build:` or `docs:` commits. The linked HOI-server repository is the operational server folder; do not start or restart it without an explicit user request.

Never register a client /hoi root: it shadows server map commands. Do not register client /선택, /준비, /시작 or /종료 roots either. Use the research menu icon; F toggles the free mouse cursor while the campaign HUD is visible; Shift+F does not open politics. Outside that campaign context, preserve vanilla offhand input. The research menu uses the authenticated OPEN request; /hoi research and /hoi-research commands are removed. ProtocolCampaignStyle provides shared Korean speed labels/colors and dark gray separators. Server /종료 resets progress/claims and the epoch; the client never performs resets locally.

All UI images come from the private HOI-resourcepack repository. Build its `scripts/build_pack.py` first; rendering tests use `../HOI-resourcepack/build/resources.zip` or `-PhoiResourcePack=<absolute path>`. Keep gameplay images and map models external. The user-provided mod-list icon is the sole image embedded in this JAR. Keep category and tabs art identical, research tabs icon-only, and preserve image aspect ratios with bounded drawing areas. Use UiPanelLayout for original TFR sidebar proportions against the 2560px reference: politics 726, trade/economy 620, logistics 560, and research/intelligence/recruitment/other menus 550. Preserve its compact minimum widths for readable controls. Use the compact left-docked original toolbar art, metal header rails and recessed content surfaces. Omit manual refresh and server-status controls while retaining automatic snapshot refresh. Keep room for at most six visible research slots, render only server-issued slots (currently at most five per country), and allocate spare vertical space to the top banner using aspect-preserving crop-to-fill. Do not show page numbers or navigation arrows; unexpected additional server slots remain accessible by scrolling. Share the navigation bar across menu and research overview/tree/detail screens; omit its country/time/speed status row. Common menu tabs are compact (20-28 GUI units wide, 34-36 units total dock height including a 14-unit indicator strip), with neutral gray selection borders and no yellow accents or visible labels; retain tooltips and accessible names. Research slot artwork stays behind centered technology art; omit saved-day text without changing banked research mechanics. Agency mutations use server-issued choices and session revisions; stale responses cannot reopen or preserve a revoked private screen.

The country flag spans both toolbar rows: approximately 40% indicators and 60% icon navigation. Order indicators as political power, stability, war support, manpower, factories/energy bar, fuel, logistics/supply-efficiency bar, convoys/transport-efficiency bar, command power, army XP, air XP, navy XP, ruling party support. Also show nuclear bombs, real GDP and debt; no achievements. World tension is a separate flag-sized two-row display fixed at the far-right edge, with its percentage in a separate recessed cell immediately below the original DEFCON frame. Keep full-width research category tabs clear of this right-hand display. Use TFR's ten original DEFCON frames, advancing every 10% (0..9% first, 90..100% last). Preserve original alpha, colours and repeated level numbers; unknown tension must not imply a level. KOR new-campaign real GDP is 1707 billion and debt 704.314 billion; read authoritative stored values, never initialize accounts in the client. Nuclear bombs appear from campaign start, regardless of research. Missing server amounts or ratios remain unknown; never fill missing bars or invent values from screenshots. Compact viewports scroll the indicator strip without shrinking/stretching icons. Optional ProtocolCountryHud fields preserve legacy JSON compatibility and must stay synchronized with HOI.

## TFR/TFRKP 출처와 적용 범위

기준 대조일: **2026-09-30**. 모드·번역·국가별 우선순위와 설치본/생성 카탈로그의 해시 차이는 [HOI 공통 출처 문서](../../HOI/docs/source-imports.md)에 기록합니다. KOR는 TFRKP, PRK는 TFR 위에 TFRKP 덮어쓰기이며 다른 국가는 TFR을 사용합니다. 기본판은 상속한 공통 엔진·자산 의존성으로 구분합니다.

원본 모드 콘텐츠·이미지와 HOI의 실행/화면·운영 규칙을 구분합니다. TFRKP 공개 페이지·별도 공식 위키는 확인되지 않았으므로 최신 국가 루트나 효과를 추정하지 않습니다. 남북한 소스와 번역 키를 서로 복제하지 않으며 생성 자료를 최신화할 때 해당 importer와 provenance 검증을 따릅니다. 문서 갱신과 JAR·팩·저장 자료의 재생성·배포는 별도 절차입니다.

## Officer corps source rendering

The server issues the 47-statistic army/navy/air/special-forces grid, its exact source tooltip lines, and eight spirit slots. The client preserves source colour codes and blank-line separators; statistic values scale within their cells without clipping percent/hour suffixes. Army/navy/air backgrounds and spirit choice banners use external original artwork. Officer choice windows use two columns and the server-issued currency, candidate IDs, token and revision. A pending submission disables repeated clicks until the next authoritative revision. General politics keeps its existing single-column layout.

`ScreenOfficerSourceTest` reads server-generated fixtures from `HOI_OFFICER_PREVIEW`, renders KOR/PRK/PRC initial and KOR selected states at GUI scale2, captures all47 statistic tooltips and all eight spirit-choice windows with candidate hovers, and tests actual left-button events, duplicate suppression and authoritative re-enabling. It also captures the source IDEA_WINDOW_SWITCH replacement prompt and selected-tile state. Identical naval/air tooltip titles are matched by their occurrence in the issued grid. Fixtures and injected request collection do not prove real network transport, two-client synchronization or socket reconnection.

Exact original state comparison still requires the PRC2040 save and active playset. The source replacement prompt and candidate percentages/precision are rendered; unsupported internal condition keys are replaced with a readable unavailable status. Preferred tactic controls issue bounded private requests and use server command-power quotes, selected-row highlighting and duplicate-click prevention. The 47 source definitions remain separate from supported runtime tactics; unsupported combat phases cannot be selected. The four officer roster controls and their dedicated routes are removed by user request. Actual army/marshal appointments and fleet organization remain available through map and unit controls. Engine formula/rounding uncertainties remain server-side evidence limits; the client must not infer values. Doctrine details/mastery, map commander appointment and branch missions follow the officer stage; see [server officer-corps boundaries](../../HOI/docs/state-affairs/officer-corps.md).

## Diplomatic justification status

The existing left relations column displays pending and usable war-goal rows with the source UI initiator/receiver icon and the other country’s flag. Hovering the row shows the server-supplied direction and remaining game days or validity. A fresh server response updates the row; selecting another country, cancellation, expiry or revoked sessions clears it. The flag retains its existing country-navigation behavior. The optional Relation.detail field is shared with HOI and accepts legacy JSON without the field. No client-side clock, war-goal creation or asset granting is added.

ScreenJustificationTest renders GUI2 pending, ready and outgoing cases, including saved day134/ready260 = 126 days, and checks refresh, unrelated target, cancellation/expiry and stale reconnect responses. Its synthetic renderer fixtures do not prove actual two-client multiplayer behavior; confirm the real saved goal after deploying both synchronized JARs. Existing timed_wargoal/wargoal artwork is a common HOI4 UI dependency recorded in HOI-resourcepack provenance; it supplies no vanilla gameplay rules.

## Commander views and original-role boundaries

`ScreenCommanderRoster` renders an authoritative COMMANDER dialog beside the officer panel. It sends only an issued choice/token/revision, disables repeat submission while pending, ignores older replies and unlocks after an authoritative refresh. Source portraits retain their aspect ratio. Army commander/field-officer and navy admiral/headquarters buttons use different server lists; no air general or air field marshal is introduced.

`MapCommanderHud` shows private army, fleet or wing cards using actual saved assignments. An empty-country server snapshot, country/dimension changes and disconnection clear the cards and cached order context; delayed snapshots alone do not expire an authorized selection. The army appointment and marshal controls use server quotes. A single bottom-aligned card row supports local paging and wheel navigation and exact selected-unit keys in CREATE_ARMY/MOVE_DIVISIONS requests; the count row opens a server-issued move quote when own map units are selected. Portrait and marshal clicks remain distinct. Unassigned cards show their count without inventing an assigned command bonus; army/marshal rows no longer overlap. Full original army-group selection/drag layout remains unfinished. Navy headquarters and field-officer views are still read-only until their original assignment costs/generation rules are confirmed.

`ScreenCommanderSourceTest` uses unchanged snapshots generated by `FabricCommanderDialogSessionsTest`. Set `HOI_COMMANDER_PREVIEW` to the server `fabric/build/commander-source-fixtures` directory and `HOI_OFFICER_PREVIEW` to `fabric/build/run/gameTest-runs/<run ID>/officer-source-fixtures` for actual GUI2 appointment, selected-state and tooltip screenshots plus pending/duplicate/stale-reply clicks. Renderer tests do not establish actual socket reconnect, two-client synchronization, full tooltip parity or an original engine cost formula.

## Commander source rows and GUI2 checks

The optional `ProtocolDialogView.Leader` row contains the server-owned role, skill, four abilities, capacity, current assignment and trait labels. Legacy tile JSON without it remains valid. Native rows use the TFR550x590 centered container anchor and original550x607 background,510x98 candidate frames, source ability icons and five complete49-unit rows at GUI2. Search, skill/name/ability/capacity sorting and assigned-candidate visibility only filter the current authoritative rows. They never infer abilities, change assets or invent client-side costs. The original Korean labels resolve from installed Korean source keys (unit_l:UNIT_LEADER_SEARCH/NAME_SORT/CAPACITY_SORT and core_l:SHOW_ACTIVE) where the installed patch has no replacement. Original source biographies and Army/Navy constant-effect tooltip text/icons are connected with source provenance and role-specific screenshot precision. The trait tree and remaining native command-group layout remain unfinished. Four server-owned per-ability hover breakdowns now use the corresponding original typed skill block and current level, excluding total leader skill and trait effects. The shared LEADER_SKILL_DESC key preserves original Korean wording and yellow level; source icons/order/precision follow the verified native screenshots. Optional legacy rows without abilityTooltips retain their complete candidate tooltip. Bounded display-only skill caches distinguish TFR/TFRKP, role, group and level; they never cache dynamic national conditions or gameplay assignments.

`ScreenCommanderSourceTest` initializes the actual officer backdrop before opening its server-generated appointment dialog, verifies all original frame assets and five visible rows, searches the issued name and tests pending/duplicate/old-reply behavior. `MapCommanderHudTest` renders160 private army cards, clicks next/previous pages, selects an actual own map counter, verifies its exact key in a MOVE_DIVISIONS request and revokes the country input. These are actual rendering/input tests with server fixtures; they do not prove socket transport, real two-client assignment or multiplayer reconnect. Use both fixture environment paths above and the latest built resourcepacks for `test runGameTest remapJar`.

The source commander renderer checks include a searched Jeon Dong-jin biography, a server-issued career-officer general-to-marshal30CP quote and one promotion request despite duplicate clicks, and searched Kang Dong-gil Navy hover. Army attack/defense use2 decimal places and supply/planning/promotion factors1; Navy coordination/hit chance use0, retreat/positioning1, damage/defense/critical chance2. These observations were read at original2560x1440, not inferred from reduced previews. Actual Army rows are gray and Navy rows blue. Server fixture click tests remain distinct from actual socket/reconnect verification.

## Doctrine detail tooltips

Private server-issued doctrine views preserve direct-unit percentage displays separately from additive battalion point displays. For example, the installed iron-wall activation is shown as +0.01 without a percent sign; complex-engineering direct rewards display +10.00%. Display formatting never determines the server combat equation. Source/save evidence and remaining formula limits are maintained in [division calculations](../../HOI/docs/warfare/division.md).

Advanced candidate details keep their original text, colors and blank lines in a tooltip bounded by the current viewport. When all rewards exceed the available height, the mouse wheel over the candidate scrolls the text; it does not select a doctrine or spend XP. Ordinary candidate tooltips retain their existing presentation. The renderer verifies the initial and final visible content at 2560x1440 and 2560x800 with GUI scale 2, the iron-wall point display, duplicate-click prevention and stale-response rejection. These fixture checks do not establish native-engine or multiplayer parity. Exact typography, mastery icon and some source category wording remain under review.

## Branch mission selectors

The navy bottom-card count opens its fleet's task forces and the air bottom-card count opens its wing's missions. Their bounded Open payloads carry only the selected unit IDs; the server checks country ownership, mission capability and valid targets. No air marshal appointment is created. Mission/target/hold actions use server-issued dialog tokens and revisions. A pending mission click remains locked after equal-revision progress updates; only a newer authoritative revision or newly issued view can replace it. Scrolling must keep every issued mission and target reachable.

ScreenBranchMissionSourceTest uses unchanged server fixtures and performs actual GUI2 selection, target and cancellation clicks; MapCommanderHudTest checks the map entry requests and absence of naval/air marshal slots. Run the focused render with -PhoiTestSuite=missions before the full suite. Renderer fixtures are not multiplayer evidence. Existing source air-mission sprites and branch icons are reused without extra assets. Generic rows remain a visual gap from native TFR/TFRKP mission panels.

### 육군 계획과 사단 제어

육군 하단 사단 수 영역에서 전투 계획을 열며, 지도 사단 선택 중에는 기존 군 편성 이동을 유지합니다. 장군·원수 초상화의 임명 역할은 유지합니다. 계획·배치 위치·태세·실행·중지·삭제와 소속 사단의 훈련·정지·철수는 서버 발급 선택 ID를 전송합니다. 같은 이름의 새 계획과 기존 계획은 서로 다른 ID를 가지며, 응답 대기 중 같은 revision의 화면 갱신은 중복 클릭을 허용하지 않습니다. 대상 국가·군·사단과 저장 상태는 서버가 검증합니다.

위치 선택 중 패널 밖 지도는 왼쪽 클릭으로 선택·해제하고 오른쪽 드래그로 표본 위치를 추가합니다. 임시 선 미리보기와 최대 64개의 정규화된 하향 광선을 사용하며 카메라 위치·차원이 바뀌면 드래그를 취소합니다. 서버 발급 토큰과 revision만 전달하고 클라이언트가 국가·비용·프로빈스 권한을 결정하지 않습니다. 새 요청 후 같은 revision 응답은 입력 잠금을 해제하지 않습니다. Minecraft 26.3 마우스 상수를 사용해야 하며 이전 GLFW 숫자를 직접 넣지 않습니다.

계획 사단 배정 화면은 사단별 배정·해제와 자동 배정 복귀를 제공합니다. 직접 배정 이동, 정지된 계획의 배정 유지, 군 변경과 저장 복원은 서버가 처리합니다. 원본 초기 사단 이름은 저장 ID를 바꾸지 않고 표시하며 번역 키가 없는 영문 리터럴은 보존합니다. 서버 원본 fixture를 이용한 목록·배정·입력 렌더 시험은 실제 다중 사용자 지도 시험을 대신하지 않습니다. 저장된 전선·화살표는 지도에 지속 표시하며 FRONT 모드의 단일 국경 클릭은 연결된 자기 측 국경을 서버에서 확장합니다. 원본 엔진의 군집단 전선 분할 규칙은 확인이 필요합니다.

Shift + left click on a server-issued exercise row sends the separate issued EXERCISE_UNTIL_TRAINED choice once. The server preserves that mode through naval target selection and saves it; the client does not infer completion from an XP display. Legacy snapshots without that optional choice retain ordinary clicking. ScreenBranchMissionSourceTest checks real GUI2 Shift clicks for both branches, duplicate suppression and the authoritative automatic-completion status. Full native mission-panel geometry and multi-region/simultaneous mission selection remain separate gaps.

## 군집단·함대·복수 지역 임무

군집단 원수 카드는 군 카드와 별개이며 군집단 관리 화면에서 군 선택·편입·분리·해체를 요청한다. 공군 지도에서도 실제 육군 군집단/군 지휘관 카드를 유지한다. 함대 카드의 상단은 함대 조직, 하단은 태스크 포스 임무이며 추가 버튼에서 태스크 포스를 선택해 함대를 만든다. 해군/공군의 실제 임무 아이콘은 현재 선택한 부대의 서버 검증 요청에 직접 연결된다. 별도의 중간 “임무” 버튼은 표시하지 않는다. 태스크 포스 편성·설정 및 공군 신규 비행단 진입은 유지하며 상태는 서버 확인 전 바뀌지 않는다.

군집단과 함대 관리, 여러 지역 임무 초안은 서버가 생성한 fixture로 GUI2에서 렌더하고 실제 위젯 클릭·중복 클릭 차단을 검사한다. 같은 리비전의 응답은 전송 대기 중인 선택을 다시 열지 않는다. 원본 함대의 복수 해역 자동 배분, 한 비행단의 동시 임무, 제독 임명 비용 관찰과 전체 원본 배치 일치는 이 검증에 포함되지 않는다.

지휘관 창의 세션 검증용 `configuration/` 타일 중 설명이 없는 내부 표식은 표시하지 않는다. 임무 초안의 선택 개수처럼 사용자에게 의미가 있는 설명·값은 유지하며 서버가 발급한 원본 view와 revision 검증에는 영향을 주지 않는다.

Grand-doctrine replacement candidate tooltips now preserve the installed Korean `SWITCH_GRAND_DOCTRINE_MASTERY_RESET` warning, including its original icon, colors and blank-line separator. `ScreenDoctrineSourceTest` renders the fresh server-issued warning at GUI2 and verifies that inspecting and closing the choice issues no grand-doctrine selection. The user confirmed that a successful switch to a different grand doctrine resets in-progress subdoctrine mastery. Current server-issued before/after fixtures render this reset with saved bank, compatible subdoctrine selections and other-branch state preserved. Cancellation, reselecting the same grand doctrine and rejected actions retain all progress. Completed-track switching, tracks absent from the replacement, bank consumption, refunds and one-time effect replay still require specific native gameplay observations.
### 지휘관 목록 특성 표시

서버가 제공한 특성 아트·이름·개별 툴팁을 GUI2 기준 x=63/y=30, 12×16 슬롯으로 표시합니다. 장군·원수·제독의 실제 역할을 사용하고 원문 설명의 색상·내용 빈줄을 보존합니다. 원본 sprite 없는 항목과 레거시 행은 텍스트 표시를 유지합니다. 원작 특성 트리·획득/해금과 미확인 조건부 효과를 모두 구현했다는 의미는 아닙니다.

### 장교 특성 트리 창

원본 `armytraittreewindow.gui`·`navytraittreewindow.gui`·`traittreewindow_common.gui`와 직접 확인한 2560×1440 Steam 화면을 기준으로 특성 트리 창을 추가했다. 원본 1343×783 배경과 166×39 상태 프레임, 23×33 특성 아이콘의 비율을 유지한다. 트리 행 간격은 55 원본 픽셀이며 일반/지형 영역의 스크롤은 독립적이다. 보유 특성은 파란 프레임, 상호배타 관계는 원본 표시, 선행 연결선은 보유/호버 상태에 맞춰 표시한다. 구매는 서버 발급 확인창과 취소를 거치며 중복 클릭은 새 revision을 받을 때까지 잠긴다. 레거시 장교는 원본 트리 자료가 없으면 선택을 발급하지 않는다.

원본 명칭·효과·고문 역할 해금 이름과 툴팁 색상/문단을 사용한다. 개별 XP가 없으므로 장교 XP를 대신 표시하지 않는다. 원본 폰트의 자간·커닝, 모든 조건부 문장 순서, 역할 탭·일부 스크롤바 및 전체 UI 픽셀 일치는 남아 있다. 이 창은 기존 Minecraft 폰트를 사용한다. 설치본에는 OFL 고지와 별도의 이사만루 재배포 금지 고지가 함께 있으며 atlas별 글리프의 실제 서체 대응은 미확정이므로 새 원본 폰트 atlas는 복사하지 않았다.

긴 특성명과 효과 수치는 원본 크기의 칸 안에서 온전한 문장을 가로로 맞춰 표시한다. 확인·취소·돌아가기 문구는 특성 화면의 축척과 함께 줄어들며 접근성 이름과 서버 발급 입력은 유지한다. GUI2 원수/장군/제독·확인·구매 후 상태·독립 스크롤·작은 창을 실제 렌더링하고 직접 확인했다. 원본 지급형 효과 48종 중 실제 계산에 연결된 것은 20종이며 나머지 28종, 역할 탭과 원본 글꼴의 자폭·커닝은 미완료다.

## Native officer interactions and preferred tactics

Officer statistic cells and doctrine rewards are hover-only regions: mouse clicks and keyboard focus cannot issue commands. Source chief/doctrine artwork uses equal alpha-aware visible dimensions and a shared center; generated orange progress strips are removed. The four officer roster controls are absent. Actual army/marshal assignment and common fleet trait views retain their server-issued authorization and actions.

National and army-leader preferred-tactic selectors share the native card renderer but have separate sessions and state. Original tactic pictures, attack/defense/locked/hover frames, damage, organization, width and movement icons, filters and phase captions come from installed GFX/GUI/localisation. Costs belong to the national quote. Selected and unavailable officer/tactic rows retain bounded source tooltips while selection remains disabled. Leader selection has a separate confirmation/cancel flow; only a newer authoritative revision unlocks a pending choice. Unsupported runtime tactics remain unavailable, and native font/pixel parity is not established by importing artwork.

Doctrine reward cards alternate beside native progress/threshold artwork. The client sums the authoritative per-stage requirements for cumulative labels and uses saved total mastery to fill each stage; it never advances rewards. Empty tracks remain empty. Source mastery tooltips preserve current/bank values, unit/category lists, separators and explanation lines; the unconfirmed engine monthly aggregate is shown as `?`. The selected folder uses the original front frame. Original font metrics, all source-category expansions and full native panel parity remain separate limitations.

Bottom commander HUD anchors do not depend on sidebar width. Map resistance/compliance percentages convert the packet fraction exactly once and retain up to two decimal places. Occupation labels avoid intersecting each other and are clipped to the viewport. `MapCommanderHudTest`, `MapOccupationHudTest` and `ScreenPreferredTacticsTest` exercise actual local rendering, sidebar transitions, several GUI scales, mode changes, filters and issued requests. Fresh source fixtures are required through `HOI_OFFICER_PREVIEW`, `HOI_DOCTRINE_PREVIEW`, `HOI_COMMANDER_PREVIEW` and `HOI_TACTIC_PREVIEW`; local fixtures do not establish multiplayer synchronization.

The bottom portrait strip uses the source `leadergroups.gui` army/group hierarchy. Current authoritative snapshots place a selected marshal and its visible subordinate armies in the same bottom strip, as shown by the supplied native screenshots. Selecting a group expands its armies. While any child army remains selected, its siblings stay visible for actual Shift-click multi-selection; clearing the selection collapses them. Skills sit above commands, followed by one bottom-aligned commander strip, vanilla action messages and the hotbar. The strip reserves at least 82 actual GUI pixels at the bottom so vanilla controls remain clear even in small windows. Long strips page horizontally and consume wheel input over cards; they never grow into a second row. Both current and legacy snapshots use the same anchor independently of sidebar width. Source army/group frames retain their different aspect ratios and native inner portrait geometry. Bottom portraits do not overlay the `GFX_theatre_army_shield` / army-group shield used by `unitview.gui` detail and theatre lists. The source shield assets and valid attached-wing/task-force detail controls remain intact. Naval cards use both original `GFX_navy_group_frame` frames from `navyleaderview.gui`, with its create-fleet portrait to the left. Air maps retain the same actual army/marshal row and its army/group creation controls; there is no fabricated air-general frame. The existing fleet/task-force and air mission requests, own-unit selection and pagination are preserved. Native tactic explanations wrap and center every line within their body rectangle instead of shrinking the whole paragraph into one line. Chief captions fit their own cell; chief/doctrine alpha-aware icons retain equal 32-unit content boxes and no orange strip is drawn.

UI rendering continues to use the available Minecraft font. Installed source font notices include both redistribution restrictions and OFL fonts; they do not identify which substituted Latin/Hangul glyphs produced each game atlas. No original UI font atlas is copied by these layout changes, and exact native font metrics remain unclaimed.

Bottom commander counts fit within their footer cells, and pagination arrows/counters scale with the same reference units as the portraits. The 160-card test clicks both page directions at GUI 1/2/3 and 1280×720 and rejects any page-button/portrait overlap.


Spirit slots use original `GFX_officer_corp_spirit_bg_enabled`/`disabled` artwork with the168×70 canvas ratio. The selected background comes only from the server entry state; an intentionally empty slot stays empty. Native category frames retain their vertical offset, while selected idea artwork centers on the background. No generated green gradient or guessed affordability glow is applied. Original input hashes and PNG pixel equivalence are tracked by HOI-resourcepack's officer-spirit importer.

## Native branch command panels

Current private commander snapshots include optional group membership, leader details, own divisions/task forces, airbase groups and current mission enums. The client selects and closes these panels locally; selecting a portrait does not appoint a commander. Shift adds/removes selections, Escape clears them, and changing branch/country clears incompatible selection. Native backgrounds, button frames, mission icons and source labels come from the external resourcepack. Fleet counts and bars use the server’s effective ship designs. Air map counters resolve to their actual own wing IDs. Selected and unselected mission frames follow the issued mission enum.

The army plan icons enter private server target modes directly. Front, fallback and area defense use the map gesture; offensive and spearhead use successive front and objective gestures. Navy and air mission buttons send bounded direct commands for the actual selected own units, without target or confirmation dialogs. Air region assignment and naval region selection use map right-click. Base icon hitboxes take priority and request actual rebasing. Unsupported army ability effects and naval invasion execution report their boundary without a fake activation. Organization and policy settings retain their separate screens. Source tooltip wording, colors and blank-line separators remain unchanged.

HUD detail snapshots have a512-member and1,000,000 UTF-8 byte bound. When optional descriptions/rows exceed the budget, identity, current status, requests and total counts remain available; full army/task-force/mission lists remain reachable through the existing paged server dialogs. Rendering fixtures are local evidence and do not establish socket reconnect or two-client synchronization.

The command-panel renderer uses `kor-command-panel-army/navy/air/air-attached/air-detached.json` fixtures under `HOI_COMMANDER_PREVIEW`. Its GameTest captures GUI2 at2560×1440 and1280×720, portrait selection without appointment, exact branch mission requests, close and Shift fleet selection. Original font metrics, private-engine tooltip warning thresholds and automatic wing following remain separate limitations. Naval-invasion support currently waits at its home port with no sortie effect; escort/landing transition remains unsupported, and no shore-bombardment alias is presented as naval-invasion support.

Army and army-group portrait clicks select their complete authoritative division membership independently of displayed rows. A manual subset plus an added army selects exactly that subset and the added army, and incomplete membership temporarily suspends actionable selection without erasing explicit IDs. The command connections preserve individual division identity: row click selects one own division, Ctrl toggles it, double-click selects matching templates within the selected command, and right-click requests server-authorized camera focus. Explicit unit selections do not broaden into map stacks or whole armies. A map right-click moves only the explicit selected divisions. Army and group execute, posture, exercise and Shift until-trained issue direct private commands; normal deletion selects a single plan on the map, while bulk removal opens the authoritative scope confirmation. Selection changes cancel pending target input and clear the corresponding movement overlays.

Saved wing-to-army associations and actual aircraft counts supply the selectable band above each army portrait. The wing badge selects its assigned army or opens a dedicated detach confirmation. Native create, split, same-design consolidation, disband and half-selection controls are connected; D/G/S/H/K and supported Shift-training route to the same selections. The separate source O group merge and native reinforcement-priority cycle are not aliases for consolidation or the existing reinforcement boolean.

Task-force settings project only stored optional repair policy, auto-split, reinforcement and insignia values; legacy unset values remain unset. Actual engagement enums select their matching native frames. The settings dialogs edit repair/engagement/composition and saved frame identities. The hold button requests nearest accessible port return; Ctrl+click holds at the current location. Command keys are not registered or executed. Single-task-force captions name the selected force, and closing a fleet preserves other fleets' selected subsets.

육군 계획 버튼은 선택한 실제 군의 전선·공세·선봉·후퇴선·지역방어 지도 입력으로 연결한다. 삭제 아이콘 좌클릭은 지도에서 한 계획을 지우는 모드이고, 우클릭은 선택 군의 이름·계획 수·보존되는 사단을 명시한 전체 삭제 확인을 연다. 병력 배치·결속도·편제 등 설정은 별도 설정 화면을 유지한다. 상륙 실행의 미확정 조건은 직접 명령에서 안내하며 기존 상륙 초안 편집기를 실제 출항으로 표시하지 않는다.

선택한 비행단은 육군 초상화 우클릭으로 해당 군에 부속할 수 있다. 부속 비행단의 군 표식은 좌클릭으로 실제 부속 군을 선택하고 우클릭으로 부속 해제 확인을 연다. 항공집단 생성·병합 O·집단 해제와 육군 부속은 별개이며 서로 대체하지 않는다. 신규 비행단은 기지의 추가 버튼과 비행단이 없는 경우에도 보이는 신규 비행단 진입에서 실제 재고로 창설한다. 원본의 네 가지 설계 충원 선호도는 실제 저장된 정책과 국가별 현용·구형 상태를 사용해 동일 설계 보충 소비를 제한한다. 기존 충원 허용 스위치 및 낮음/중간/높음의 교체 우선순위와 구별하며, 혼합 설계 실행은 지원하지 않는다.

지휘 패널은 원본 일반 클릭·닫기·사단 선택·전투 계획·해산 효과음을 외부 팩에서 재생한다. 호버 효과음은 클릭 가능한 요소에 진입할 때 한 번 재생하며 같은 요소에서 정지하거나 매 프레임 렌더링할 때 반복하지 않는다. 사용자 Master 볼륨과 음악 상태를 변경하지 않는다.

### 확인된 원본 규칙의 제어 경로와 GUI2 경계

금색 능력 4개(force_attack, last_stand, staff_office_plan, extra_suplies)는 원본 아트·이름·설명을 유지한 직접 요청을 보낸다. 서버는 미확정 과금 순서·실제 효과를 안내하며 지휘력을 소비하거나 능력을 활성화하지 않는다. 기존 조건·계수 조회 자료는 검증용 조회 화면과 구별하고 HQ 관계·지속 시간 기록을 실제 HQ 배치나 능력 효과로 표시하지 않는다.

해군 대상 팝업은 원본 9함종의 2행 배치와 서버의 실제 제외 집합을 사용한다. 체크된 항목은 교전 제외이며 구세이브의 수송선 제외 플래그도 복원한다. 임시 태스크 포스 선택 창 없이 현재 선택된 태스크 포스에 같은 함종 설정을 원자적으로 적용한다. 선택 집합과 각 항목의 기존 제외 상태를 서버에서 확인하며 하나라도 바뀌었으면 모두 거절한다. 선택 집합의 제외 상태가 섞인 경우 첫 클릭은 모두 제외하고 다음 클릭은 모두 허용한다. 이 혼합 선택 처리 규칙의 원작 엔진 일치 관측은 별도다. 혼합 함종 전투의 사격 배분을 원본과 동일하다고 표시하지 않으며 적에게 공격받는 상황을 취소하지 않는다.

원본 전역 제거 아이콘을 클릭하면 지도에서 제거할 전역을 선택하고 우클릭하면 현재 선택 부대가 속한 함대들의 전체 배정 전역을 제거한다. 대상 함대를 다시 고르는 임시 창과 전역 목록·확인 창을 거치지 않는다. 현재 발급된 태스크 포스 선택 집합과 함대별 배정 상태를 다시 검증하고 전체 작업을 한 군사 트랜잭션으로 적용한다. 배정 전역 제거는 실제 함선 위치·이동 경로·편성을 변경하지 않으며, 기존 임무의 해당 전역 기여를 중단한다. 육군 전체 계획 삭제의 사용자 요청 확인 창은 유지한다.

육해공 전용 명령 키는 등록하거나 실행하지 않는다. 초계·타격부대·통상파괴·수송선 호위·기뢰 부설·소해·상륙지원 대기, 편성·정지·배정 해제·전략 이동·결속도는 각 버튼의 마우스 요청으로 처리한다. 버튼의 Shift/Ctrl+클릭 조합과 일반 창 닫기·초안 확정은 유지한다. 실제로 지원하지 않는 원본 결속도나 상륙 지원 행동을 완료로 표시하지 않는다.

항공집단 생성·해제·병합은 실제 저장된 집단 ID와 비행단 구성원을 사용하며 육군 부속 및 기체 병합과 독립적이다. 비행단 툴팁은 실제 설계별 기체 수와 부속군 추종 결과를 표시한다. 점검 주기·내부 카운터 같은 진단 설명은 명령 툴팁에 삽입하지 않으며 실제 대기·실패 이유는 유지한다.

`MapCommanderPanelsTest`와 `ScreenDeterminateCommandDialogsTest`는 새 서버 원본 fixture로 GUI2 2560×1440·1280×720의 제어 경로, 창 경계, 읽기 전용 조회와 툴팁을 검사한다. 금색 능력 4종·해역·보충 창도 실제 서버 조회 자료를 사용한다. 새로운 캡처는 전체 검증 실행마다 다시 생성해 직접 확인해야 하며, 테스트 정의나 원본 아트 수입만으로 렌더 통과·한국어 원본 글꼴 동등·실제 다중 사용자 동기화를 주장하지 않는다. 능력 정보 조회에 실행 효과음은 붙이지 않으며 현재 일반 UI 호버음을 사용한다.

긴 읽기 전용 능력 설명은 원문·빈줄·색상을 보존한 채 화면 안에 배치한다. 목록 옆 공간을 우선 사용하여 다른 행을 가리지 않으며 행 전환 시 해당 변종의 설명으로 갱신한다. 화면보다 긴 본문에는 기존 휠 스크롤을 사용한다. 지도 지휘 패널·초상화 툴팁은 HUD 추출 뒤 즉시 그려 일반 화면의 이미 끝난 툴팁 처리에 누락되지 않게 한다. 비행단 원장과 이름은 실제 장비 이름을 표시하며 내부 설계 UUID를 사용자 문구로 대신하지 않는다. 공군 진단 설명의 실제 호버 캡처도 두 GUI2 해상도에서 확인한다.

### GUI2 카드와 지도 직접 명령

군집단·군·함대 카드 행은 하단 액션바 위에 놓고 펼친 카드·부속 비행단·함대 선택과 팝업도 액션바 클릭 영역을 침범하지 않게 배치한다. GUI2 큰 화면과 작은 화면에서 같은 배치 규칙을 사용하며 사용자의 미선택 상태를 자동 선택으로 바꾸지 않는다. 직접 명령 안내문은 카드 상단 장식과 도구막대 제목을 포함한 전체 하단 영역 위에 놓고 팝업 클릭 영역도 피한다. 목표 입력 안내는 해당 입력 상태가 끝날 때까지 유지한다.

군·군집단 카드 클릭은 서버가 제공한 전체 구성원 ID를 사단 선택 모델에 반영한다. 접힌 하위 군이나 화면에서 생략한 행도 구성원에 포함되며, 우클릭 이동과 선택 표시는 같은 ID 집합을 사용한다. Shift 카드 선택은 군 구성원 합집합을 갱신한다. 사용자가 행·지도에서 부분 선택한 뒤에는 일반 스냅샷 갱신이나 새 사단 추가가 그 선택을 전체로 확대하지 않는다. 닫기·선택 해제·국가·군종 전환은 해당 선택과 명령 범위를 제거한다.

지도 우클릭은 서버 세션·리비전에 묶인 방향 벡터를 보낸다. 서버가 실제 시점과 지형에서 목표를 계산하고 자국 권한·범위·부대 상태를 검증한다. 진행 중 중복 입력은 보내지 않으며 선택·모드·국가·세계·카메라 변경은 진행 중 제스처를 폐기한다. 공군 배정과 육해군 이동 화살표는 서버의 저장된 상태를 사용한다. 실제 렌더 검사와 모의 전송 검사는 운영 다중 접속·실제 소켓 재접속 검증을 대신하지 않는다.

상륙 계획은 `UNITS_VIEW_BTN_DRAW_NAVAL_INV`, 군집단 선택의 전선은 `UNITS_VIEW_BTN_DRAW_DEFENCE_ARMY_GROUP`, 공군 취소는 `HOLD_WINGS` 원문을 사용한다. 상륙 계획의 현재 초안 저장 범위 안내는 원문 뒤 빈줄로 구분한다. 육군 훈련은 서버가 원본 장비 소모·국가 경험치 본문과 Shift 클릭 설명을 제공하며 색상·경험치 아이콘·빈줄을 보존한다. `MapCommanderPanelsTest`는 군·군집단·공군에서 실제 컨트롤을 호버하여 전달 문구와 렌더된 툴팁의 일치 및 GUI2 두 해상도의 배경 경계를 검사한다. 이 렌더 검증은 운영 캠페인 접속 검증과 별개다.

### 육군 명령 입력과 지역 방어

하단 훈련·공군·해군 임무는 `MapCommanderOrders.route`에서 기존 서버 검증 요청으로 바로 전달한다. 전선·공세선·후퇴선·지역 방어·배정·편집·개별 삭제는 `LAND/*` 입력 모드로 바로 진입한다. 실행용 중간 모달을 열지 않는다. 군 전체 삭제의 확인과 지역 방어의 하위 옵션 편집은 별도 기능으로 유지한다. 상륙 초안 편집은 실제 상륙 실행과 구별한다.

육해공 전용 키 등록과 지도 명령 단축키 실행 경로는 제거했다. Ctrl·Alt를 누르기만 해서는 배정·편집 모드를 켜지 않는다. 배정·편집 버튼으로 입력 모드를 선택하며, 일반 Esc 취소·Enter 초안 확정은 유지한다. 화면 종료·포커스 상실·선택 변경은 미확정 제스처를 폐기한다. 하단 키 안내는 표시하지 않는다.

지역 방어 군 하나를 선택하면 VP·항구·해안·요새·공군기지·국경·보급 중심지/철도 분기점 옵션을 표시한다. 필요 수는 현재 HOI의 해당 프로빈스당 한 사단 배치 규칙으로 계산하고 실제 배정 수를 함께 보여 준다. 모든 옵션을 해제한 상태도 보존하며 방어 위치가 없으면 실행을 거절한다. 옵션 없는 구세이브는 기존 모든 지정 프로빈스 방어 동작을 유지한다. 주 클릭 추가·제거는 인증된 서버 세션의 초안만 변경한다. Enter로 확정하면 기존 계획 ID·배정을 유지한 채 원자적으로 저장하며 Esc·선택 변경·화면 종료·접속 종료는 초안을 폐기한다. 비어 있는 초안은 확정할 수 없다. 지도에서 기존 영역은 회색, 변경 초안은 파란색으로 구분한다. 선택한 사단의 지역 방어 배정 해제도 군 소속과 다른 사단·계획을 유지한다.

왼쪽 사단 목록의 소속·임무·실행/중지 상태는 서버의 실제 배정 집합을 사용한다. 명령 실행 버튼의 비활성 이유는 공통 서버 검증과 연결한다. 지휘 툴팁은 원본 색상 코드와 수입된 툴팁 배경을 사용하고 본문의 빈줄을 유지한다. GUI2 테스트 클라이언트 캡처와 모의 네트워크 입력 검증은 운영 캠페인의 수동 조작·실제 다중 접속 검증과 구별한다.

계획선 선택 뒤 왼쪽 드래그는 선택한 기존 프로빈스 제어점 하나를 이동하며 나머지 순서·계획 ID·사단 배정을 유지한다. 우클릭 드래그는 기존 전체 선 대체 경로다. 잘못된 시작점·중복 제어점·오래된 계획은 서버에서 거절한다. 배정 모드는 선택 사단 전부가 소속된 군의 계획에 초록색 제어점을, 다른 군 계획에는 빨간색 선을 표시한다. 키 설정 안내와 육해공 전용 키 입력 연결은 제거했으며 명령은 화면 버튼으로 선택한다.

군 상세 패널의 원본 제거 버튼은 해체 대상·사단 보존·계획 제거·부속 비행단 처리·지휘관 해제 비용을 기존 인증 대화창으로 확인한다. 해체는 사단을 삭제하지 않으며 다른 군·집단군은 유지한다. 부속 비행단은 기존 부속 해제 규칙대로 대기 상태가 된다. GUI2 원본 비교 캡처는 설정값뿐 아니라 실제 창 배율도 2인지 검사하고 해상도를 파일명에 기록한다.

Minecraft 26.3 마우스 이벤트는 `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`를 그대로 사용한다. 지휘 패널의 사단 선택·위치 이동, 전체 명령 삭제 확인, 부속 비행단 조작과 지도 제어점 편집에서 이전 0/1 버튼 번호를 사용하지 않는다. 실제 `ScreenMapCursor.mouseClicked`를 거친 좌클릭 선택과 우클릭 위치 이동을 회귀 검사한다. 지도 명령 서버 제한은 최대 3개 연속 요청과 4틱당 1개 보충으로, 모드 승인→지도 입력과 임시 모드 취소→배정 해제의 정상 연쇄 요청을 허용하면서 중복 revision 검증을 유지한다.


`ScreenCommanderCaptivity` opens from the officer panel's captured-commander button: left click selects our missing leaders and right click selects foreign leaders held by us. `CAPTIVITY_OPEN/QUERY` uses the existing authenticated industry session and returns bounded server-side search, sorting, pages and independent country totals. Querying never changes military state. Loading, failed/retry, empty and no-search-result states differ; input focus survives refresh, stale responses are ignored and revocation closes the private screen. Long row/trait descriptions reuse the bounded scrollable officer tooltip. `FabricCommanderCaptivityViewsTest` generates renderer fixtures from actual server transactions; `ScreenCommanderCaptivityTest` checks both GUI2 resolutions and real coordinate clicks. Captivity is distinct from unassigned or retired leaders; a separate death state and natural capture remain unimplemented.

The HQ designer uses original unit icons and server-derived base hitpoints and selected `deployed_leader_modifiers` previews. Each observed modifier keeps its own units/precision, including hours and command-capacity additions. This preview does not activate deployment effects. The user replaced HQ 3D model rendering with eleven source land adjusters: plains, forest, hills, mountain, urban, jungle, marsh, desert, fort, river and amphibious. They reuse existing equipment-designer assets and localisation. Their movement/attack/defence values come from native HQ subunit terrain blocks plus current country unit/category research and doctrine modifiers, using the existing land line-mean plus support-sum policy. HQ candidate eligibility uses actual completed unit unlocks and saved source-DLC flags; unknown or conflicting capability state is reported instead of granted. Draft add/remove respects the native four-line/four-support limit and retains at least one line battalion. The server now supplies 28 shared combat-stat fields with equipment, research, doctrine and country modifiers, and candidate deltas for add/remove/regiment conversion. The GUI2 table and hover tooltips use those values. Equipment values use imported native archetypes and default modules, not the army’s delivered stockpile or a claim of complete native-engine equivalence. Paid composition/deployment remains pending original observations. `-PhoiTestSuite=captivity` exercises both HQ and captivity; the complete renderer suite remains the release check.

서버 스냅샷 수신이 지연되어도 현재 국가·차원의 지휘 카드, 선택한 군/사단 및 명령 세션은 유지한다. 40클라이언트틱 경과는 권한 만료가 아니다. 오래된 지도 부대 카운터만 숨기며 선택 ID는 다음 권위 스냅샷과 대조한다. 빈 국가 스냅샷, 국가·차원·군종 전환, 접속 종료는 기존대로 선택과 토큰을 폐기하고, 군 해체 시 해당 카드와 구성원 참조를 정리한다. 정상 날짜 갱신은 명령 토큰을 재발급하지 않으며 서버 소유권·revision·요청 제한 검증을 유지한다.

선택 사단 상세는 `commander_selection_details_v1`의 서버 권위 페이지로 받습니다. 세션 토큰·선택 sequence·동일 generation의 32행 페이지를 모두 받은 뒤 반영하므로 카드 미리보기 밖의 사단과 미배속 사단도 현재 선택한 행만 표시합니다. 선택 상한은 2,048개이며 서버는 실제 자국 사단만 투영합니다. 늦게 도착한 이전 선택·회수된 세션 응답은 무시합니다. 단일·복수 군 선택과 행 스크롤은 갱신 동안 유지하고, 함대·비행단 지도 카운터는 임시 묶음 키를 실제 기동부대·비행단 ID로 정규화합니다. 선택 해제나 대상 소멸이 전체 함대 선택으로 확대되지 않습니다.

`MapSelectionDetailsTest`는 실제 지도 카운터 클릭, 사단 64개 페이지의 역순 수신, 미배속·단일·복수 선택 행, 갱신 지연 후 스크롤·군종별 상세 유지, 대상 제거와 오래된 응답 거절을 검사합니다. 실제 클라이언트 SoundManager에서 지도·카드·행의 왼쪽 선택음이 입력당 한 번이고 갱신 중에는 재생되지 않는지 확인합니다. 전체 렌더 검증의 지휘 HUD 장면은 일반 핫바가 표시되는 게임 모드를 사용해 GUI2의 스킬→명령→카드→액션키→핫바 간격을 확인합니다. 이 테스트는 개발 클라이언트의 자동 조작이며 운영 캠페인의 수동 다중 접속 검증은 별도입니다.

## 장교단·교리 본문 정렬

장교단 영역 제목과 정신·참모 선택창 제목·버튼 이름, 국가 선호 전술 창 제목·필터 탭, 포획된 지휘관 제목·탭은 각각의 표시 칸 중앙에 배치한다. 교리 군종 탭·대교리명·분류명·하위 교리 이름판·선택창 제목과 버튼 이름, 필요 통달치 숫자도 해당 칸 중앙에 정렬한다. 현재 통달치는 아이콘과 숫자의 표시 폭을 합친 묶음을 계수판 중앙에 배치한다. 긴 설명·목록·툴팁 본문은 왼쪽 정렬을 유지하며, 단독 `---`와 기존 빈줄은 빈 행으로 남긴다. 글자 크기·GUI2 배율·클릭 영역·비활성 색과 초상화·훈장·통화 아이콘의 위치는 유지하고, 원문 색상 코드·명시된 인라인 아이콘·들여쓰기·수치 정밀도를 보존한다.

통달 단계 수치는 서버의 보상별 요구량을 누적한다. 단계 번호로 군종 팔레트를 순환하지 않는다. 숫자 배경은 원본 `doctrines_view.gui`의 `mastery_text_bg` 기본 프레임과 육군 원본 화면에서 확인한 중립 프레임을 사용한다. 숫자의 노란 강조색은 구매 비용·조건의 빨강/초록 판정과 구분한다. `doctrine_folders.txt`와 배경 DDS에는 육군·해군·공군·특수부대 팔레트가 있지만, 숫자 배경을 달성 상태에 따라 군종색으로 전환하는 엔진 규칙은 설치 텍스트 및 지정 60장에 없다. 요청된 100·200 등 단계 숫자 자체의 군종별 색 구분은 확인 필요 항목이다. 숫자 글자는 노란색, 작은 숫자 배경판은 중립 프레임이며 달성 상태색을 추정 적용하지 않는다. 대교리·하위 교리 이름판의 군종 색을 유지한다. 보상 배경은 원본 8프레임이 군종별 달성·잠금 순서로 교차 배치되어 있으므로 육군 0/1, 해군 2/3, 공군 4/5, 특수부대 6/7에 연결한다. 기존의 군종+잠금 오프셋 4 계산으로 다른 군종 배경이 나오던 오류를 수정했다. 달성 배경은 육군 녹색·해군 청색·공군 적색·특수부대 금색이고 잠금 배경은 각 군종의 회색 프레임이다. 보상 아이콘·잠금 판정과 실제 진행은 유지한다.


### 명령 셀과 건설 상태색

육군 명령 아이콘과 클릭·툴팁 영역은 원본 `bpt_bg` 내부 각 셀을 함께 사용합니다. 훈련도 첫 셀 안에 배치하며 투명 여백을 제외한 아이콘 내용을 종횡비를 유지해 맞춥니다. 해군 임무와 공군 임무·편성 버튼은 인접 셀 사이의 간격을 보존합니다. 스킬·명령·군 카드·액션키·핫바의 기존 독립 표시 공간은 유지합니다. 육군 미선택 상태에서도 스킬과 명령 줄은 남고 선택 안내를 표시합니다. 이 상태의 클릭은 군 선택이나 서버 요청을 만들지 않습니다. 실제 미선택 GUI2 캡처에는 일반 핫바를 함께 표시하며 하단 키 안내는 표시하지 않습니다.

건설 지도는 완공 기준 남은 공간이 0이면 파랑 단색, 예약 때문에 남은 공간이 0이면 파랑 사선, 그 외 예약이 있으면 노랑 사선을 사용합니다. 공유 주 슬롯과 연구 상한에도 같은 우선순위를 적용합니다. 철도 예약은 주 경계를 넘어 양 끝에 표시하며 예약 중인 실제 구간의 시작 레벨과 예약량을 사용합니다. 한 프로빈스에 여러 예약 철도가 있으면 예약 후 여유가 가장 적은 구간을 표시합니다. 철도·항구·보급 허브의 네트워크 아이콘은 유지하면서 건설 바탕색은 보급 충족률과 분리합니다. 예약 취소·완공 후 서버의 사유 지도 투영 갱신을 그대로 사용합니다. 보급 지도 자체의 색상 규칙은 유지합니다.


육군 카드와 상단 군 상세의 현재/최대 수량은 흰색으로 표시하며 지휘관이 없는 군 카드는 실제 현재 사단 수만 표시한다. 군/집단군의 일반 생성·편입은 `FORMATION` 직접 요청을 사용한다. 기존 군 카드 우클릭은 정확한 선택 사단 편입, 기존 집단군 우클릭은 명시적으로 선택한 군 편입, 집단군 생성 칸 좌/우클릭은 새 집단군 생성이다. 수동으로 고른 사단은 소속 군과 무관하게 왼쪽 통합 선택 목록에만 표시하며 닫기는 그 수동 선택을 해제한다. 확인이 필요한 해체·전체 삭제·유료 지휘관 배치는 기존 대화창을 유지한다. `MapHierarchySelectionTest`는 GUI2 두 해상도에서 실제 화면 클릭→정확한 요청 대상→선택 유지와 대화창 미개방을 검사한다. 운영 멀티플레이에서 서버 응답을 받은 검증과는 구별한다.

긴 지휘 툴팁은 화면 경계를 지키면서 가능한 경우 스킬·명령·군 카드·액션키·핫바의 하단 영역 옆에 배치한다. 양옆 공간이 부족하고 위에 들어갈 때는 위로 옮긴다. 원문과 글자 크기는 유지하며 화면 자체보다 공간이 부족하면 기존 화면 경계 제한을 우선한다. 실제 호버 렌더 검사는 옆/위 공간이 있는 경우 하단 영역과 툴팁의 비겹침도 확인한다.

### 명령 사용 가능 여부와 응답 범위

`MapCommanderAvailability`는 현재 선택에 대한 서버 검사 결과를 받아 명령 버튼의 비활성 표시·툴팁에 사용한다. 토큰, 명령 revision, 선택 sequence, 조회 번호와 현재 선택 사단/군 ID가 일치하는 응답만 적용한다. 지원 서버에서 아직 응답을 받지 못한 선택은 확인 중으로 표시하며, 오래된 응답으로 버튼이 다시 활성화되지 않는다. 카드별 실행 버튼은 그 카드의 정확한 군 ID 범위와 일치하는 결과만 사용한다. 다른 카드의 요청에 현재 선택 범위의 거절 이유를 전용하지 않으며, 모든 실행은 서버에서 다시 검사한다.

원본 효과 설명의 문장·색상·빈줄은 보존하고 검사 이유를 뒤에 별도 표시한다. 사용자 요청에 따라 단축키·키 설정 안내 행과 그 바로 앞 빈줄, Ctrl·Alt 키 단독 배정·편집 안내는 제거한다. 버튼·마우스 조합 안내는 유지한다. 목표 지정이 필요한 임무는 지도 대상 지정 안내를 추가한다. 삭제 버튼의 왼쪽 클릭은 단일 계획 선택 모드, 오른쪽 클릭은 선택 군 전체 계획 삭제 확인이며 비활성 이유도 따로 표시한다.

`MapCommanderAvailabilityTest`는 GUI2 실제 렌더러에서 비활성 버튼·해제된 Ctrl+H·Ctrl 입력의 무동작, 단축키를 제외한 원문 툴팁 보존, 카드 범위 불일치, 전체 삭제 제한, 선택 해제/재선택, 오래된 응답 거절과 명령 확인 후 대기 요청 정리를 검사한다. 해당 테스트의 서버 응답은 명시적인 시험 입력이며 실제 서버 조건은 `FabricCommanderOrderSessionsTest`와 `MilitaryBranchMissionOrdersTest`가 별도로 검사한다. 이 자동 렌더·입력 검증은 운영 서버의 다중 사용자 접속 검증을 대신하지 않는다.

### 해군 수리 우선순위 좌우 클릭

기동부대 행의 수리 정책은 알려진 현재 값에서 좌클릭 다음 단계·우클릭 이전 단계로 직접 요청한다. 툴팁도 같은 원본 다음/이전 정책 이름을 표시한다. 서버 응답 전에는 아이콘을 임의로 바꾸지 않고 연속 입력을 억제한다. 현재 값이 없는 구세이브는 기존 정책 선택 화면을 사용한다. `MapNavalRepairInputTest`는 GUI2 두 해상도에서 실제 클릭과 갱신을 검사하며 해당 픽스처 검증을 운영 멀티플레이로 간주하지 않는다.

`MapCommanderPanelsTest`와 `MapCommanderOrdersTest`는 GUI2에서 육해공 전용 키와 수정 키 조합이 요청·선택·지도 입력 모드를 바꾸지 않는지, 같은 기능의 마우스 버튼은 정확한 부대 범위로 실행되는지 검사한다. 하단 키 안내를 인위적으로 넣던 시험 표시도 제거했다. `UiCommandLabelsTest`는 단축키 행과 인접 빈줄 제거, 나머지 효과 문단과 마우스 조합 안내 보존을 검사한다.

군 카드의 펼침 상태는 선택과 분리해 국가·차원·군종 및 안정 ID로 유지한다. 집단군을 처음 선택하면 소속 군을 표시하고, 이후 명시적으로 접은 상태는 날짜 갱신이나 재선택으로 다시 열리지 않는다. 펼친 소속 군은 사용자 지정대로 집단군 바로 왼쪽에 모이며 같은 배경색을 공유한다. 큰 집단군은 페이지마다 집단군 카드를 함께 표시한다. 집단군 색상은 서버 ArmyGroup의 RGB 상태를 저장·동기화해 소속 군 배경과 집단군 색상 칸에 함께 사용한다. 원본 COMMANDGROUP_PRESET_COLORS_HSV의 10색을 선택할 수 있다. 구세이브의 없는 색상은 미지정으로 보존하고 중립 회색으로 표시한다. 자동 색상 배정은 추정하지 않는다. 색상 선택 후 서버 응답 전에는 임의로 색을 바꾸지 않는다. 군·함대의 오른쪽 펼침 버튼은 상세 목록만 접고 선택·소속·임무를 유지한다. 하단 X는 요청 범위대로 선택 해제이며 원본 disband 버튼의 해체 기능과 구분한다. 해체는 기존 확인 절차에서만 실행한다. 카드 제어 버튼의 우클릭은 편성 이동이나 지도 입력으로 전달하지 않는다.


`MapCommanderGroupsRenderTest`는 군·함대의 상세 목록과 하단 임무 대상 아이콘을 별개로 검사한다. 펼친 목록의 존재, 접은 목록의 숨김, 다시 펼친 뒤 복원, 정확한 선택 ID 보존과 하단 임무 제어의 유지를 GUI2 두 해상도에서 실제 클릭으로 확인한다. 같은 부대명 툴팁을 가진 하단 아이콘을 상세 목록 행으로 간주하지 않는다.

`MapMovementOverlayTest`의 실제 서버 시계 진행 자료를 `MapMissionOverlayRenderTest`가 읽어 육군·해군 각각 시작/중간/다음 프로빈스의 화살표와 부대 카운터를 함께 촬영한다. 시작과 중간의 카운터 화면 좌표는 같고 다음 프로빈스에서 바뀌어야 한다. 게임 분·구간 진행률·프로빈스·화면 좌표를 같은 시퀀스 JSON에 기록한다. 평면 시험 월드에는 실제 경로를 평행 이동해 표시하며 이 렌더 검증을 운영 지도·실제 소켓 동기화나 원본 곡선 형태의 완전한 재현으로 간주하지 않는다. 집중 실행은 `runGameTest -PhoiTestSuite=movement-controls`이다.

### 공군 기지별 선택 패널

공군 임무 줄은 원본 `airselectionview.gui`와 지정 Steam 화면의 공중 우세 → 근접항공지원 → 요격 순서를 사용한다. 기지 헤더는 원본 배경·국기·기지 이름·수용량·창설·닫기 배치를 연결한다. `MapCommanderAirBases`는 서버의 기지 ID로 묶고 `AIRBASE_CAPACITY`, `AIRBASE_AIRPLANES_KNOWN`, `AIRBASE_CAPACITY_COUNTS_DESCRIPTION` 원문에 전체 주둔 기체 수와 실제 수용량을 넣는다. 구형 응답의 누락 값은 0으로 만들지 않고 미확인으로 표시한다. 이름이 같아도 다른 기지를 합치지 않는다.

기지의 창설 버튼은 해당 기지 ID를 보내며 X는 해당 기지에서 선택한 비행단만 선택 해제한다. 다른 기지의 선택과 모든 임무·기체 상태는 유지한다. `MapAirBasePanelsTest`는 GUI2의 2560×1440 및 1280×720에서 서로 이름이 같은 두 기지의 독립 헤더, 선택 수와 다른 실제 수용량, 근접항공지원 버튼 요청, 기지 지정 창설 요청과 선택 해제 후 갱신을 실제 클라이언트 입력·렌더 경로로 검사한다. 수용량 호버는 자국·전체 수량의 구분, 원본 색상·문장·빈줄, 치환되지 않은 변수 없음과 실제 툴팁의 화면 경계를 검사한다. 요청 관측기를 사용하는 패널 검증은 운영 서버의 다중 사용자 접속 검증과 구분한다.

비행단 행은 원본 장비 정보 버튼, 실제 기체 수/충원 한도, 보충 정책, 부속 군, 임무, 주야간 설정과 닫기를 별도 영역으로 사용한다. 장비 이미지는 실제 서버 장비 연결을 사용하고 함재기 표시는 실제 설계에만 붙인다. 주야간 설정·보충 정책·장비 상세는 누른 비행단 하나의 ID로 요청하며 행의 X는 같은 기지의 다른 비행단까지 선택 해제하지 않는다. 구형 응답에 행 정보가 없으면 기존 행을 유지한다. 서버 확인 전에는 시간대나 정책을 임의로 바꾸지 않는다.

기지 연료 아이콘은 원본 위치와 이미지를 사용하며 툴팁은 현재 국가의 권위 연료 정보를 표시한다. 기지별 연료 탱크를 별도로 계산한 것으로 표시하지 않는다. `MapAirBasePanelsTest`의 실제 GUI2 입력 검사는 비행단별 시간대·정책·상세 요청과 같은 기지 및 다른 기지의 선택 보존을 포함한다. 육해공 하단 키 안내와 전용 단축키는 표시·연결하지 않으며 스킬·명령·군 카드·액션 버튼·일반 핫바는 유지한다. 원본 기지 관계 아이콘의 프레임 의미, 비행단 공격 성향·에이스 조작은 확인·구현이 남아 있다.

명령·임무·스킬 툴팁은 원본 문장 뒤에 구현 해설이나 일괄 지도 입력 안내를 덧붙이지 않는다. 상시 하단 목표 지정 안내도 표시하지 않는다. 서버가 거절한 이유와 일시적인 연결·선택 오류는 유지하며 권한 검증이나 기능 제한을 숨기지 않는다. 해군 직접 배치 코어와 기존 회귀 16개가 불변 의존성 스냅샷에서 통과했고, 서버 입력·상태 회귀 18개는 2026-10-09 14:03:34 UTC 전체 코어 체크포인트로 재검증했다. 클라이언트 명령 코드 3개와 관련 GUI 회귀 소스 3개는 컴파일을 통과했다. 실제 GUI2 렌더·조작과 최신 전체 합본 gate는 별도로 수행해야 한다.

의용군 파견·귀환 수송 중인 사단은 `divisionPresent` 판정에 따라 지도 모델·카운터·지도 선택키·시야 기여에서 제외한다. 사단 자체와 군 편성·목록은 유지하며, 도착 후 ACTIVE 상태에서는 기존 소유국의 지도 선택과 시야를 복원한다. `MapVolunteerPresenceTest`는 저장형 상태를 복원하여 이 경계와 타국 선택 거절을 검사한다. 수송 중인 상세행은 원본 `alert_volunteer_transfer_instant`의 “§H이송 중인 의용군§!”을 표시하고 대기·임무 미배정 접미문구를 붙이지 않는다. `FabricCommanderVolunteerStatusTest`는 파견·귀환·도착 상태의 실제 HUD 응답과 프로토콜 왕복을 검사한다. 실제 지도 모델 렌더 실행은 별도 검증한다.

## 복수 장비 무기대여와 의용군 화면

`ScreenDiplomacyLease`와 `ScreenDiplomacyVolunteers`는 원본 개별 패널·버튼·아이콘·스크롤 자산으로 GUI2에서 조립합니다. 전체 스크린샷을 배경으로 사용하지 않습니다. `UiDiplomacyAssets`는 원본 64픽셀 테두리와 반복 중앙 영역, 스크롤 경계, TFR 버튼 셰이더의 명암·시간 변화를 처리하며 화면 종료·리소스 재로드 때 생성 텍스처를 해제합니다. 원본의 192 선언/190 실제 이미지 차이는 현재 실제 크기로 처리하므로 원작 엔진의 UV 해석까지 동일하다고 주장하지 않습니다. 제작국·함재기 배지·군 방패 문양과 색은 권위 있는 상태가 없으면 임의로 표시하지 않습니다.

무기대여는 서버가 발급한 장비를 최대 32종 선택하고 각 수량·일회/월별 방식을 편집합니다. 최신 PREVIEW와 동일한 내용만 SUBMIT하며, 시간 초과 후 변경 요청을 자동 재전송하지 않습니다. 원본 생산 비율·연료 항목은 규칙 미확정 사유와 함께 비활성입니다. 실제 재고·생산 수치를 사용하지만 원작의 K/M 축약과 기간 전환까지 검증한 것은 아닙니다. 의용군은 실제 군·사단·지휘관 상태를 표시하고 군 선택·국가·세션·리비전을 검증합니다. 군사 파견·귀환 연결과 한도·위협이 미확정이면 실행을 비활성으로 유지합니다. 취소·완료 복귀 시 국가 화면을 다시 조회하고 권한 회수 뒤 응답으로 화면을 다시 열지 않습니다.

`ScreenDiplomacyLeaseTest`와 `ScreenDiplomacyVolunteersTest`는 `diplomacy` 시험 묶음에 포함됩니다. 의용군 입력은 `FabricDiplomacyVolunteerSessionsTest`가 실제 원본 부대를 재편한 `unselected`, `selected`, `multiple-selected`, `air-disabled` 4종이며 기존 부대·병력·장비가 불변임을 검사합니다. `gradle/fixture-groups.gradle`에 등록한 생산자를 실행하고 현재 소스·입력 해시를 확인하므로 수동으로 과거 JSON을 놓는 것으로 정식 검증을 대신할 수 없습니다.

현재 검증은 프로토콜 114개·클라이언트 109개 JUnit 통과, GUI 테스트 소스 컴파일, 클라이언트 전용 JAR 생성과 공유 프로토콜 46개 일치입니다. 버튼 효과·샘플링·타일링 단위 검사 8개가 포함됩니다. 실제 GUI2 캡처·원작 픽셀 대조·효과음 청취·다중 접속은 아직 실행하지 않았습니다. 서버 공통 군사 API 체크포인트와 최신 입력 자동 생성 뒤 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy --console=plain --no-daemon`으로 검증합니다.

무기대여만 독립적으로 검사하려면 서버의 `FabricDiplomacyLeaseRenderFixtureTest`가 생성한 디렉터리를 `HOI_LEASE_PREVIEW`로 지정하고 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy-lease --console=plain --no-daemon`을 실행한다. 이 검사는 `empty.json`, `once.json`, `monthly.json`, `multiple.json`, `fixture-source.json`이 모두 있어야 하며 뷰 파일과 세션 소스의 SHA-256을 대조한다. 자료는 실제 초기 캠페인의 재고·생산에서 발급하고 PREVIEW 전후 전체 저장 상태 불변과 저장 콜백 미호출을 검사한다. 생성기 시험의 출력은 `hoiLeasePreview` 시스템 속성 또는 `HOI_LEASE_PREVIEW` 환경 변수로 지정한다. 고정 군사 의존성으로 만든 독립 자료에는 해당 runtime baseline을 기록하며 최신 전체 서버 소스 검증으로 간주하지 않는다. `all`·`diplomacy` 검사는 `diplomacyLease` 그룹의 최신 서버 생성·해시 검증을 그대로 사용한다.

입력 회귀용 합성 뷰의 캡처명은 `hoi-diplomacy-lease-synthetic-*`, 실제 서버 발급 뷰는 `hoi-diplomacy-lease-source-*`로 구분한다. 실제 자료 렌더 중 뷰·초안·제출 권한을 고치거나 전송 요청을 만들지 않는다. 분류 탭과 실제 비활성 연료·생산 비율 툴팁도 원래 발급 상태로 검사한다.