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

Exact original state comparison still requires the PRC2040 save and active playset. The source replacement prompt and candidate percentages/precision are rendered; unsupported internal condition keys are replaced with a readable unavailable status. Preferred tactic controls issue bounded private requests and use server command-power quotes, selected-row highlighting and duplicate-click prevention. The 47 source definitions are separate from the nine executable runtime tactics; unsupported entries cannot be selected. Roster/commander controls, complete tactic execution and exact native roster/tactic artwork and layout remain unfinished. Engine formula/rounding uncertainties remain server-side evidence limits; the client must not infer values. Doctrine details/mastery, map commander appointment and branch missions follow the officer stage; see [server officer-corps boundaries](../../HOI/docs/state-affairs/officer-corps.md).

## Diplomatic justification status

The existing left relations column displays pending and usable war-goal rows with the source UI initiator/receiver icon and the other country’s flag. Hovering the row shows the server-supplied direction and remaining game days or validity. A fresh server response updates the row; selecting another country, cancellation, expiry or revoked sessions clears it. The flag retains its existing country-navigation behavior. The optional Relation.detail field is shared with HOI and accepts legacy JSON without the field. No client-side clock, war-goal creation or asset granting is added.

ScreenJustificationTest renders GUI2 pending, ready and outgoing cases, including saved day134/ready260 = 126 days, and checks refresh, unrelated target, cancellation/expiry and stale reconnect responses. Its synthetic renderer fixtures do not prove actual two-client multiplayer behavior; confirm the real saved goal after deploying both synchronized JARs. Existing timed_wargoal/wargoal artwork is a common HOI4 UI dependency recorded in HOI-resourcepack provenance; it supplies no vanilla gameplay rules.

## Commander views and original-role boundaries

`ScreenCommanderRoster` renders an authoritative COMMANDER dialog beside the officer panel. It sends only an issued choice/token/revision, disables repeat submission while pending, ignores older replies and unlocks after an authoritative refresh. Source portraits retain their aspect ratio. Army commander/field-officer and navy admiral/headquarters buttons use different server lists; no air general or air field marshal is introduced.

`MapCommanderHud` shows private army, fleet or wing cards using actual saved assignments. Country/dimension changes, disconnection and expiry clear the cards. The army appointment and marshal controls use server quotes. Two-row cards support local paging and exact selected-unit keys in CREATE_ARMY/MOVE_DIVISIONS requests; the count row opens a server-issued move quote when own map units are selected. Portrait and marshal clicks remain distinct. Unassigned cards show their count without inventing an assigned command bonus; army/marshal rows no longer overlap. Full original army-group selection/drag layout remains unfinished. Navy headquarters and field-officer views are still read-only until their original assignment costs/generation rules are confirmed.

`ScreenCommanderSourceTest` uses unchanged snapshots generated by `FabricCommanderDialogSessionsTest`. Set `HOI_COMMANDER_PREVIEW` to the server `fabric/build/commander-source-fixtures` directory and `HOI_OFFICER_PREVIEW` to `fabric/build/run/gameTest/officer-source-fixtures` for actual GUI2 appointment, selected-state and tooltip screenshots plus pending/duplicate/stale-reply clicks. Renderer tests do not establish actual socket reconnect, two-client synchronization, full tooltip parity or an original engine cost formula.

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

계획 사단 배정 화면은 사단별 배정·해제와 자동 배정 복귀를 제공합니다. 직접 배정 이동, 정지된 계획의 배정 유지, 군 변경과 저장 복원은 서버가 처리합니다. 원본 초기 사단 이름은 저장 ID를 바꾸지 않고 표시하며 번역 키가 없는 영문 리터럴은 보존합니다. 서버 원본 fixture를 이용한 목록·배정·입력 렌더 시험은 실제 다중 사용자 지도 시험을 대신하지 않습니다. 저장된 전선·화살표의 지속 표시와 연속 국경 확장, 원본 계획 패널 배치는 남아 있습니다.

Shift + left click on a server-issued exercise row sends the separate issued EXERCISE_UNTIL_TRAINED choice once. The server preserves that mode through naval target selection and saves it; the client does not infer completion from an XP display. Legacy snapshots without that optional choice retain ordinary clicking. ScreenBranchMissionSourceTest checks real GUI2 Shift clicks for both branches, duplicate suppression and the authoritative automatic-completion status. Full native mission-panel geometry and multi-region/simultaneous mission selection remain separate gaps.

## 군집단·함대·복수 지역 임무

군집단 원수 카드는 군 카드와 별개이며 군집단 관리 화면에서 군 선택·편입·분리·해체를 요청한다. 공군 지도에서도 실제 육군 군집단/군 지휘관 카드를 유지한다. 함대 카드의 상단은 함대 조직, 하단은 태스크 포스 임무이며 추가 버튼에서 태스크 포스를 선택해 함대를 만든다. 해군/공군 임무 버튼은 여러 부대별 임무와 목표 지역을 선택한 뒤 확인하는 화면을 연다. 이 화면은 명령 초안만 전달하며 상태는 서버 확인 전 바뀌지 않는다.

군집단과 함대 관리, 여러 지역 임무 초안은 서버가 생성한 fixture로 GUI2에서 렌더하고 실제 위젯 클릭·중복 클릭 차단을 검사한다. 같은 리비전의 응답은 전송 대기 중인 선택을 다시 열지 않는다. 원본 함대의 복수 해역 자동 배분, 한 비행단의 동시 임무, 제독 임명 비용 관찰과 전체 원본 배치 일치는 이 검증에 포함되지 않는다.

지휘관 창의 세션 검증용 `configuration/` 타일 중 설명이 없는 내부 표식은 표시하지 않는다. 임무 초안의 선택 개수처럼 사용자에게 의미가 있는 설명·값은 유지하며 서버가 발급한 원본 view와 revision 검증에는 영향을 주지 않는다.

Grand-doctrine replacement candidate tooltips now preserve the installed Korean `SWITCH_GRAND_DOCTRINE_MASTERY_RESET` warning, including its original icon, colors and blank-line separator. `ScreenDoctrineSourceTest` renders the fresh server-issued warning at GUI2 and verifies that inspecting and closing the choice issues no grand-doctrine selection. This warning does not establish the unimplemented native lifecycle for progressed tracks, reserved mastery, completion history or one-time milestone effects.
### 지휘관 목록 특성 표시

서버가 제공한 특성 아트·이름·개별 툴팁을 GUI2 기준 x=63/y=30, 12×16 슬롯으로 표시합니다. 장군·원수·제독의 실제 역할을 사용하고 원문 설명의 색상·내용 빈줄을 보존합니다. 원본 sprite 없는 항목과 레거시 행은 텍스트 표시를 유지합니다. 원작 특성 트리·획득/해금과 미확인 조건부 효과를 모두 구현했다는 의미는 아닙니다.

### 장교 특성 트리 창

원본 `armytraittreewindow.gui`·`navytraittreewindow.gui`·`traittreewindow_common.gui`와 직접 확인한 2560×1440 Steam 화면을 기준으로 특성 트리 창을 추가했다. 원본 1343×783 배경과 166×39 상태 프레임, 23×33 특성 아이콘의 비율을 유지한다. 트리 행 간격은 55 원본 픽셀이며 일반/지형 영역의 스크롤은 독립적이다. 보유 특성은 파란 프레임, 상호배타 관계는 원본 표시, 선행 연결선은 보유/호버 상태에 맞춰 표시한다. 구매는 서버 발급 확인창과 취소를 거치며 중복 클릭은 새 revision을 받을 때까지 잠긴다. 레거시 장교는 원본 트리 자료가 없으면 선택을 발급하지 않는다.

원본 명칭·효과·고문 역할 해금 이름과 툴팁 색상/문단을 사용한다. 개별 XP가 없으므로 장교 XP를 대신 표시하지 않는다. 원본 폰트의 자간·커닝, 모든 조건부 문장 순서, 역할 탭·선호 전술·일부 스크롤바 및 전체 UI 픽셀 일치는 남아 있다. 이 창은 기존 Minecraft 폰트를 사용한다. 설치본에는 OFL 고지와 별도의 이사만루 재배포 금지 고지가 함께 있으며 atlas별 글리프의 실제 서체 대응은 미확정이므로 새 원본 폰트 atlas는 복사하지 않았다.

긴 특성명과 효과 수치는 원본 크기의 칸 안에서 온전한 문장을 가로로 맞춰 표시한다. 확인·취소·돌아가기 문구는 특성 화면의 축척과 함께 줄어들며 접근성 이름과 서버 발급 입력은 유지한다. GUI2 원수/장군/제독·확인·구매 후 상태·독립 스크롤·작은 창을 실제 렌더링하고 직접 확인했다. 원본 지급형 효과 48종 중 실제 계산에 연결된 것은 20종이며 나머지 28종, 역할 탭·장교별 선호 전술과 원본 글꼴의 자폭·커닝은 미완료다.
