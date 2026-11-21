# 화면과 표시 기준

## 원본 문구의 £ 텍스트 아이콘

서버의 결정·분류·이벤트·중점·정치 문구는 기존 `TooltipKeywordIcons`로 `hoi:ui/keyword_icons.json`을 조회한다. `GFX_` 접두어가 있는 토큰과 없는 토큰, 1부터 시작하는 `|N` 프레임을 같은 글꼴로 처리한다. 알 수 없는 토큰은 숨기고, 뒤의 공백·한글 조사·구두점·색·정밀도는 유지한다. 단, `|N` 프레임 번호 바로 뒤의 마침표는 원본처럼 번호와 함께 읽혀 표시하지 않는다. 예로 `EFFECT_MODIFY_BUILDING_RESOURCES`의 `£resources_strip|$FRAME$.`는 원본 `20261121090447_1.jpg`·`20261121090455_1.jpg`에서 아이콘 뒤 마침표가 없다. 실제 원본이 없는 토큰과 출처는 HOI-resourcepack의 `keyword-icons-import-report.json`에서 확인한다.

공통 스타일·줄바꿈·단일행 표시·단추 문구는 너비 측정과 잘라내기 전에 아이콘으로 변환한다. 칸보다 긴 글자는 자르거나 말줄임하거나 가로로 줄이지 않고 기존 글자 크기로 칸 안에서 가로 스크롤한다(2026-11-19 사용자 지시, 모든 화면 공통). 시작과 끝에서 1초씩 멈추고 초당 24 글꼴 픽셀로 왕복하며, 시각은 현재 화면이 열린 뒤의 클라이언트 틱이다. 칸 높이에 맞춘 축소는 유지한다. 클라이언트 GameTest는 시점이 다른 화면 비교가 흔들리지 않도록 가로 스크롤을 멈춘 상태(`UiText.holdMarquee`)로 실행하고, `ui-headers` 묶음의 `UiMenuHeadingTest`에서만 풀어 칸 안 스크롤과 칸 밖 미표시를 검사한다. 이벤트 제목도 글꼴 스타일을 유지하며, 표시할 수 있는 줄보다 길면 마지막 줄에 남은 제목을 이어 가로 스크롤한다. 결정 비용과 행 이름에는 원시 £ 토큰을 남기지 않는다. 독립된 큰 배너·초상은 기존 `DecisionText.sourceRows`와 이미지 블록 경로를 유지하고, 실제로 글자를 그리는 경로에서 남은 미등록 토큰을 제거한다. 글꼴 그림은 외부 팩에서 공급하며 JAR에 포함하지 않는다.

팩을 먼저 빌드하고 `runGameTest -PhoiTestSuite=decisions`로 결정 경로를 검사한다. `HOI_TEXT_ICON_PREVIEW=<서버가 export한 prk-pages.json·prk-metadata.json·prk-details.json 폴더>`가 있으면 같은 원본 응답 검사에 PRK 방역 분류·비용·효과 툴팁의 GUI2 2560×1440·1280×720 캡처를 추가한다. 서버 fixture의 시험용 상태와 실제 운영 플레이는 구분한다. 전체 검증은 README의 고정 입력을 설정한 뒤 필터 없는 `test runGameTest remapJar`로 실행한다.

결정 행과 분류는 원본 그림이 없으면 그림을 그리지 않으며, 상단 `menu/decisions` 버튼이나 다른 분류 그림으로 대체하지 않는다. TFR `common/decisions/TFR_decisions_PRK.txt:714`의 `prk_proclaim_reunification_of_korea`는 `icon = democracy`지만 설치 원본에 `GFX_decision_democracy`가 없다. 미확정 그림은 결정 렌더 증거의 `missingSourceAssets`에 `omitted`로 남긴다. 그림이 없는 이름의 시작점은 기존34GUI를 유지한다. 원본 `countrydecisionview.gui/decision_item/name_text`가 아이콘 존재와 무관한 고정 x=55를 사용하기 때문이다.

원본 그림→대상 국가의 국기→이름 순서로 한 행의 스크롤 영역에 놓고 사이 간격4GUI와 세로 중심을 유지한다. 이는 원본 `targeted_decision_item`의 아이콘 x=15, 국기 x=30, 이름 x=62의 좌우 순서다. 원본 그림과 이름은 함께 이동하며 비용 칸·추적/실행 단추·진행 막대·기존 행 높이는 유지한다. 이름 끝의 £ 그림도 별도 고정 그림으로 떼지 않고 글꼴 안에서 함께 이동한다. 이름/비용의 글자 그림은 각 칸의 경계에서 잘린다. `DecisionMarqueeTest`는 원본 그림의 유무·긴 이름 중간 위치·inline £·비용 색을 GUI2 두 해상도에서 검사하고, PRK source 재현은 정치국과 방역 분류를 함께 캡처한다.

## 외교와 첩보 장부 원본 배치

외교 행동은 서버 `CountryView.actions.entries`를 발급 순서대로 한 번씩 표시한다. `send_volunteers`는 원본 GUI의 파병 창 이름이며 클라이언트 목록 슬롯이 아니다. 목록의 `volunteers`는 `DIPLOMACY_SEND_VOLUNTEERS_TITLE`(의용군 파병), `recall_volunteers`는 `RECALL_VOLUNTEERS_TITLE`(의용군 소환)에 대응한다. 소환을 별도 고정 슬롯에 추가하거나 클라이언트에서 정렬하지 않는다. 사용자 지정 순서는 서버가 파병 바로 다음에 소환을 발급하는 방식으로 적용한다. `open_logistics`만 기존 하단 단추로 분리한다.

`diplomacyActions` fixture 그룹은 지정된 서버 소스의 `FabricCountryActionsTest`와 `DiplomacyActionsPresentationTest`를 실행해 현재 외교 응답을 생성한다. `DiplomacyVolunteerActionsTest`는 GUI2 두 해상도에서 실제 목록의 순서·제목 중복·스크롤 도달·비활성/우클릭 무동작과 각 행동 툴팁을 검사·촬영한다. 활성 소환의 확인 및 반복 클릭 검사는 명시적인 합성 활성 상태로 구분한다. 이 경로는 운영 화면의 중복 발생 원인을 입증하거나 서버의 미구현 소환을 활성화하지 않는다. 원작 사진 `20261009214916_1.jpg`~`20261009215104_1.jpg`의 소환 맨 위 배치는 사용자 변경 전 순서다. 사진의 실행 조건·수치와 현재 서버 캠페인의 조건·`HOI 적용` 문구는 구분하며 원문·비용을 사진 값으로 덮어쓰지 않는다.

외교 머리칸은 TFR `countrydiplomacyview.gui/country_info`의 y50, 탭 y70을 사용한다. 국가명·세력·지도자는 기본 글꼴과 크기를 유지하며 긴 문구만 같은 칸 안에서 가로 스크롤한다. 세력 그림이 없으면 그 그림의 예약 폭을 문구에 돌려준다. 외교 행동 이름은 원본처럼 왼쪽 정렬하고, 두 목록은 `GFX_tiled_window_1b_thin_border`의 64픽셀 경계를 타일링한다. 정당 원형 그래프는 `GFX_political_chart_big`의 반지름30 원판과 TFR `GFX_pol_piechart_overlay`의71×71 원본 덮개를 같은 중심으로 합성한다. 배경 원본399×167의 왼쪽 위 어두운 칸(x5..104/y5..90)을 실제 nine-slice 변환에 대응시키고, 원판·고리를 그 칸의 가로·세로 중앙에 둔다. 홀수 차이는 반 GUI 좌표를 유지한다. GUI 해상도의 채움 줄을 확대하지 않고 화면 픽셀 크기의 캐시된 원판에 안티앨리어싱하고 원본 덮개를 알파 합성한다. 크기·서버 정당 목록·리소스가 바뀌면 캐시를 갱신/해제한다. 조각은 서버 순서·색·비율을 유지하며3시에서 반시계 방향으로 누적하고 호버도 같은 각도를 쓴다. 사진의3시 아래는 마지막 노랑→자홍 조각이다. 설치 TFR 덮개는 팩과 RGBA가 같으나 바닐라 DDS는4,447픽셀이 다르므로 두 원본이 같다고 간주하지 않는다. 색을 임의로 보정하지 않는다. 행동 순서·가능 여부·관계 국기·국민정신과 수치는 서버 응답을 유지한다.

외교 탭(`DIPLOMACY_RELATIONS_TAB` ‘외교’)의 관계 줄(`relation_strip_view` 200×50)은 `GFX_diplo_relations_bg`(235×44, x-1/y3) 위에 관계 아이콘을 원본 크기 그대로 x-3/y12에 왼쪽 위 기준으로 그린다(예: 보장·군사 통행 29×26, 휴전 46×27). 고정 46×27 칸에 맞춰 늘리거나 가운데로 옮기지 않는다. 국기 상자(x40/y5, 160×32)는 기존 겹침 규칙을 유지한다.

세계 긴장도 창(`worldtensionwindowpopup.gui`, 527×534)의 목록 항목은 원본 스크린샷(20261119043223·043227) 실측값 x37/y134, 간격44, 449×41을 쓴다(GUI 정의 합산값 x46과 다르다). 제목·정렬 단추·닫기·빈 목록 문구는 원문 키 `THREAT_WINDOW_NAME`, `DATE_NAME`·`country`·`WORLD_TENSION_NAME`·`WORLD_TENSION_WARS`(툴팁 `SORT_BY_DATE`·`SORT_BY_COUNTRY`·`SORT_BY_TENSION`·`SHOW_WARS`), `CLOSE`, `NO_THREAT_SOURCES_WINDOW`를 쓰고 닫기 단추(x204/y475)는 원본 `GFX_button_123x34`다. 긴장도 항목은 국기를 `GFX_diplo_countrylist_flag_frame`(x2/y2) 안 정중앙에 TFR 마스크·오버레이로 그리고 이름 x55/y4, 날짜 x55/y20, 수치 x185 폭50 오른쪽 정렬, 사유 x240 폭203을 쓴다. 이름은 폭150과 수치 글자 앞 중 좁은 쪽까지 그리고 넘치면 최소 선명 크기에서 가로 스크롤한다. 초기값과 현재값이 다른 추적 항목만 시계(x172/y7)와 두 줄 수치를 표시하고, 선전포고 같은 일회성 항목은 원본처럼 한 줄 수치만 표시한다. 현재 전쟁 항목은 `GFX_current_wars_entry`(9-slice), 강대국 참전 전쟁의 `GFX_tension_war_list_major`(x5/y5, 툴팁 `WAR_HAS_MAJOR`), 전쟁 이름 x45/y2(폭250, 가로 스크롤), 작은 전쟁 점수 막대 x45/y20, 공격·방어 전쟁 지도국 국기(테두리 x321·x396/y3 안 정중앙)를 그리며 항목 툴팁은 `CLICK_FOR_WAR_OVERVIEW`다.

전쟁 상세(`waroverview.gui`, 폭1167)는 전쟁 이름 x406/y23 폭350, `FIELDER_MANPOWER`·`MANPOWER_LOSSES`를 x20·x900/y68·y86(폭250, 가운데)에 둔다. 야전 인력은 참전국 범위 합계를 원본 `*` 형식(유효 숫자 세 자리 K/M)으로 쓰고, 손실은 전쟁별 사상자를 추적하지 않아 원본 기본값 `-`다. 필터는 원본 `GFX_sort_button_140x29`(켜짐 frame1 흰 갈매기표)와 `WAR_OVERVIEW_FILTER_*`·`*_DESC` 툴팁이며 약소국 필터는 서버 강대국 판정을 쓴다. 닫기는 원본 `GFX_closebutton`(x1123/y22)이다. 머리 아이콘은 원본 크기로 공격 측 x24/180/244/303/362/440/504, 방어 측 x592/745/815/868/930/1005/1071(y155~159)에 두고 원본 `waroverview_l_korean.yml` 툴팁을 붙인다. 한쪽에 세력 소속 참전국이 있으면 원본 `war_faction_entry`(`GFX_war_faction_entry`, 펼침 `GFX_strategicair_expand` x9/y11: 펼친 상태 ▲ frame1·접힌 상태 ▼ frame0, 세력명 x56/y12 폭163, 사단·산업 합계, 사상자 `-`) 아래에 회원국을 두고, 세력이 없는 참전국은 `DIPLOMACY_NO_FACTION` 묶음에 둔다. 참전국 행은 x25·x593, y193부터 간격42이고 국기와 강대국 덮개 `GFX_major_country_flag_overlay`를 `GFX_small_flag_frame_thin2`(x4/y3) 안 정중앙에, 항복 국기를 x32/y17에, 이름을 x56/y4 폭180에, 항복 막대와 전쟁 명분 보유국의 `GFX_wargoal_icon`(x237/y7, 툴팁 `WARGOAL`)을 둔다. 수치는 배경 칸(기여도 43·사단 75·산업 71·사상자 46px) 안 가운데에 쓰고 넘치면 최소 선명 크기에서 가로 스크롤하며, 범위는 원본처럼 `a - b`, 사단·산업 툴팁은 `DIVISIONS_COUNTRY`·`INDUSTRY_COUNTRY`다. 호출 가능한 동맹은 `GFX_not_in_war_ally_entry`에 이름·항복 막대만 그리며, 서버 호출 동작이 없어 원본 호출 단추는 그리지 않는다. 글자는 GUI 배율에서 글꼴 1배(물리 픽셀 정수배)를 최소로 쓴다. 원본 043244는 공격국(중국)이 오른쪽에 보여 좌우 기준이 확정되지 않았고 현재는 `.gui` 이름대로 공격 측을 왼쪽에 둔다. `DiplomacyWorldTensionChecks`(`diplomacy` 묶음)는 서버 실제 캠페인 고정 입력 `kor-world-tension-{history,wars}.json`(`HOI_COMMANDER_PREVIEW`)으로 정렬·툴팁·x37 항목·강대국 표시·세력 접기/펼치기·전쟁 명분 아이콘을 검사한다.

첩보 장부는 TFR `countryintelledger.gui`의 자원 y5, 비축 요약 y95, 공장·생산 배분 y175, 민간 그래프 y355와 군종별 머리칸·목록 좌표를 따른다. 민간은 제공된 29개 항목을 표시하고, 숫자 독립 칸은 공통 중앙 정렬을 사용한다. 서버가 발급한 자원 문자열·범위·`?`를 바꾸거나 글자를 줄이지 않는다. 입력8 자원 머리 줄은 서버에서 정수로 발급한다. 1280×720 GUI2에서는 `10.13`·`42.18`과 긴 병력 범위가 칸 너비를 넘으므로 칸 경계에서 잘라 가로 스크롤한다. 이 경우 한 프레임에서 전체 숫자가 동시에 보이는 것은 아니다. 민간 본문은 원본 세로 스크롤로 그래프 하단·기간 단추·연구 항목까지 접근한다. 기간 선택은 화면 로컬 상태이며 없는 시계열을 만들지 않는다. 기간 이름의 원문 키는 `LEDGER_CIV_LINECHART_DETAILS_{3_MONTHS,1_YEAR,3_YEARS}`이며 설치 한글 패치에 없어 HOI4 `localisation/korean/intel_ledger_l_korean.yml`의 `3개월`·`1년`·`3년`을 유지한다. 하단 연구 순서는 산업→공학, 보병→지원→기갑→포이며 정보 부족 상태는 원본 그림의 회색 상태로 표시한다.

장부 하단은 TFR `countrytechtreeview.gfx`의 `*_folder_icon`(91×61)을 사용한다. 전체 연구 화면의 `*_folder_tab`과 구분하며 공학은 `electronics_folder_icon`이다. 국가 목록 단추와 하단 바탕도 원본 `GFX_button_221x34`·`GFX_main_screens_bottom`을 사용한다.

서버 `ideas`는 민간 장관·법률 가로 스크롤과 군종별 참모 칸에 연결하며 `doctrine.rows[0]`의 분류 그림과 서버 툴팁을 오른쪽 칸에 표시한다. 입력8부터 `opinion.ours/theirs`를 보는 나라/대상 국기와 함께 표시하며, 행동의 `costs` 순서·수량을 원본 정치력/지휘력 그림으로 오른쪽 정렬한다. `declined`는 원본 `GFX_accept_decline_icon` frame0 X이며 행동 `enabled`와 구분한다. 값이 없는 옛 응답의 의견·비용·X는 비어 있다. 자원 숫자는 `resources` 머리 줄의 문자열을 그대로 쓰며, `domestic`·`exported`·`imported` 세 값이 모두 있을 때만 원형을 그린다. 원본 `GFX_trade_resource_chart` 순서/색(초록0,.7,0→노랑1,1,0→파랑0,0,1), 반지름10, `GFX_mini_piechart_overlay`를 사용하고 합0이면 파란 원이다. 입력9의 해군 지원(`mtgnavalsupportfolder`) 연구·툴팁을 해군 다음에 표시하며, 원본 `countrytechtreeview.gfx:1862`의 `GFX_mtgnavalsupportfolder_icon`을 쓴다. 육군 교리 단추는 서버 `doctrine.rows[0].icon`을 읽고 민간 법률·장관은 받은 순서를 유지한다. 상대국 군수 열기·무기대여 요청·응답·알림은 입력10 계약으로 연결한다. 범위는 [무기대여 요청 UI](lend-lease-request-ui.md#입력-10-화면-연결과-재현)를 따른다.

`graph_*`가 `shown`이면 `days` 줄과 같은 개수의 선 값을 읽고 기간별 최근7/27/79개를 표시한다. 정보 부족·기록 없음·잘못된 날짜/수량에는 선을 만들지 않는다. y축은0~보이는 최댓값, 축 글자는 기간 단추를 덮지 않는 좌우 칸 안에서 가로 스크롤하며 x축은 첫 기록 날짜와 `TODAY`(한글 패치/TFR 대응 없음, HOI4 `core_l_korean.yml`의 `오늘`)다. 선 크기380×120/굵기1.5는 원본 `GFX_intel_ledger_chart`, 원색은 HOI4 `00_graphics.lua/NGraphics.INTEL_LEDGER_GRAPH_{RED,GREEN}`, 알파0.6은 원본 `linechart.shader`를 따른다. 주선 초록·손실선 빨강의 대응은 엔진 코드/정보 충분 원본 사진을 확인하지 못한 Claude의 추론이며 추후 사진으로 재확인한다. 입력8 실제 PRK/JAP는 정보 부족이므로 실제 캡처는 격자/문구만 보인다. `DiplomacyInputTest`의79기록·의견값은 명시적인 합성 표시 검사이며 실제 캠페인 상태가 아니다. 이 칸을 행동 상세 문자열에서 추출하지 않는다. 설치 TFR/HOI4에 없는 `gfx/interface/traded_factories_icon.dds`는 무역에 사용 중인 민간공장 칸의 다른 그림으로 대체하지 않는다. 공군 종류 행의 원본 배경 `GFX_navy_overview_ship_entry`도 설치 sprite 인덱스에 정적 정의가 없어 대체 그림을 만들지 않는다. 제공된 원본 사진은 2560×1440이며 1280 비교판의 원본 열은 이 사진을 축소한 참고 이미지다. 클라이언트의 현재 렌더와 원작의 날짜·첩보율·수량 차이는 구분한다.

관계 줄 국기는 TFR `relation_strip_view/relation_flags`의 x40/y5·160×32px `OverlappingElementsBoxType`과 `core.gui/countryflag_entry`를 따른다. `GFX_small_flag_frame_thin`은47×32px이며 원본 `GFX_flag_small2`의41×26px 영역은 frame x4/y4와 flag x7/y6의 상대 오프셋(3,2)를 갖는다. 현행 국기는 사용자 지시대로 small2 마스크·TFR 오버레이를 합성하지 않는다. 최신 보드 지시에 따라 현재 국기 사각형은 틀의 가로·세로 중심에 두며, 홀수 GUI 차이는 반 GUI 좌표를 유지해 GUI2 실제 픽셀 여백을 같게 한다. 틀 줄도 원본44px 막대의 중앙으로 옮기고, scissor·hit 세로 영역을 같은 좌표에 맞춘다. 아이콘·화살표·160px 폭·겹침 간격은 유지한다. 들어갈 때는 원래 간격, 넘칠 때는 `CountryFlagStack.left`로 간격을 줄여 마지막 국기의 오른쪽을 칸 끝에 맞춘다. 뒤 국기가 앞 국기를 덮으며 각 국기의 hit 영역은 뒤 국기에 가려지지 않은 부분이다. 앞선 고정 간격 지시는 원본 대조 후 철회됐다. 숫자 가로 스크롤은 글꼴 advance 외 실제 glyph bounds도 포함한다. `22 - 73`의 35GUI 칸과 40GUI 실제 폭은 유지하며 스크롤 양 끝에 도달하게 한다. 한 프레임 동시 전체 표시는 아니다.

육군 장부의 리벳·공장 단추는 `countryintelledger.gui/ledger_tech_entry/button`의 기본 `GFX_industry_folder_tab` frame1(0기반0)이다. TFR 기술 분류의 `old_land_doctrine_folder`에 대응하는 정적 folder sprite가 없으므로 기본 단추가 유지된 것으로 추론한다. GUI에는30% 미만일 때만 이 그림으로 바꾸는 조건이 없으며,30% 이상 원작 그림은 미확정이다. 입력9의 서버 분류 그림을 그대로 사용하며 클라이언트가 첩보율로 다른 그림을 추정하지 않는다. 원본 민간 줄의 식별 가능한 첫8개 슬롯은 safety_laws→welfare_laws→economy→trade_laws→tax_laws→interest_rate_laws→mobilization_laws→female_service_laws다. 입력9는 원본 저장의 법률 순서 뒤에 장관을 보내며 화면은 그 순서를 임의 재배열하지 않는다. 사진에 가려진 장관 위치·표시 모양은 원작과 일치한다고 단정하지 않는다.

외교 오른쪽 세 그림의 원본은 TFRKP `interface/TFRKP/TFRKP_cabinet.gui`의 내각 수반·김일성·김정일 아이콘이다. sprite는 `GFX_PRK_cabinet_premier_kim_jae_ryong_icon`, `GFX_PRK_eternal_leader_kim_il_sung_icon`, `GFX_PRK_eternal_leader_kim_jong_il_icon`이고 위치는 각각(450,117)/(490,117)/(490,157)이다. 40×40은 컨테이너 크기이고 실제 sprite는68×68이다. 원본 container scale0.5를 sprite에 적용한34×34를 그리며, 툴팁 판정도 실제 sprite 사각형을 따른다. 원본 창 show_position x−6과 container margin1의 합인 x−5를 반 GUI 좌표로 보존한다. 세로 원점은 원본 사진의 밝은 문양 중심263/303px에 맞춰 politicsTop + (art.y−111)로 환산한다. 2560의 동일 RGB 기준 문양 경계는 내각총리가 원본·렌더 모두448,249..476,277이다. 김일성·김정일은 가로 경계가 원본·렌더 모두488..516이며, PNG 렌더의 아래쪽 문양 끝은 원본 JPEG보다1px 길다(중심 차이0.5px). 이는 전체 화면의 다른 칸 위치를 바꾸지 않는 해당 그림의 원본 위치 보정이다. `common/scripted_guis/TFR_scripted_guis_KOR.txt`의 세 `*_tt_diplo`는 `selected_country_context`에서 `selected_country_view_diplomacy`에 붙으며 클릭은 항상 비활성이다. visible의 바깥과 FROM 범위 모두 `(original_tag=PRK AND NOT PRK_north_korea_coupdeta_flag) OR (original_tag=KPA AND PRK_north_korea_coupdeta_flag)`를 검사한다. 원본 사진의 한국→북한 표시를 근거로 서버가 FROM도 대상국으로 평가해 보이는 그림만 발급하며 클라이언트가 조건을 재평가하지 않는다. 툴팁 키는 `TFRKP_CABINET_PREMIER_TT1/TT`, `TFRKP_ETERNAL_LEADER_TT1/TT`, `TFRKP_ETERNAL_LEADER_KIM_JONG_IL_TT1/TT`이며 입력9 `CountryView.art`를 표시 전용으로 그린다. 원본 country_info의 politics y110을 현재 politicsTop에 대응시켜 상대 좌표를 보존한다. `NGraphics.TOOLTIP_SHOW_DELAY=0.05`·`TOOLTIP_DELAYED_DELAY=1`에 따라 제목과 상세를 표시하고 긴 본문은 그림 위에서 휠로 이동한다. TFR core.gfx의 A 빨강(205,55,55)·L 연청색(162,185,207), 원문 빈 줄·숫자·작은 글자 그림을 유지한다. 초상172×258은 글꼴 글리프 크기 제한 때문에 원본 PNG를86×129GUI로 직접 그린다. 초상은 원문 토큰 행 중앙에 두며 엔진의 정확한 초상 기준선은 원본 툴팁 사진으로 재확인해야 한다. `import_diplomacy_art.py`가18개 원본 sprite와 출처를 등록하고 `import_assets.py --prefix diplomacy/tfrkp/`가 픽셀을 가져온다.

집중 재현은 팩 선빌드 후 Java25에서 `test runGameTest remapJar -PhoiTestSuite=diplomacy -PhoiServerSource=<입력9>/HOI -PhoiProtocolSource=<입력9>/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`이다. `HOI_COMMANDER_PREVIEW`와 `HOI_AGENCY_OPERATION_PREVIEW`는 같은 입력의 `fixtures/commander-source-fixtures`로 지정한다. `diplomacy-actions`는 이 중 실제 한국→북한 장부·외교 행동을 빠르게 재현한다. GUI2 두 해상도의 실제 렌더·모의 요청 검사는 운영 접속·다중 사용자 동기화·직접 효과음 청취를 대신하지 않는다. 최신 작업 지시에 따라 1부 리뷰를 기다리는 동안 중점·연구 변경을 진행할 수 있으며 무필터 전체 검증은 별도 시작 신호가 필요하다.

무기대여 요청 화면의 GUI별 요소·sprite·필요한 서버 값, 요청자 장비 종류 선택과 받는 쪽 수량/모드 선택의 구분은 [무기대여 요청 원본 UI 계약 조사](lend-lease-request-ui.md)에 유지한다. 입력10의 요청·응답·알림·상대국 군수 화면은 현재 연결되어 있다. `DiplomacyFlagOverlapTest`는 시험 국기의 실제 리소스 존재를 확인하며, 5개(PRK/JAP/KOR/SOV/PRC) 모두 실제 밝은 이미지 픽셀·노출 영역의 hit·툴팁을 검사한다. 이전 시험의 RUS는 TFR 러시아 태그 SOV와 달라 이미지가 없었으므로 시험 입력을 고쳤다. 인도 합성 항목의 누락 태그도 기존 원본 국기가 있는 AST로 바꾸며 임의 별칭·대체 그림은 만들지 않는다. `diplomacy-flags`로 별도 재현한다. `DiplomacyFlagOverlapTest`의3개·5개·18개 관계 줄은 서버 형식의 `무역 금수 조치`/`embargo_receiver` 합성 입력이며, 입력6 그리기 코드 재실행은 과거 전체 클라이언트 실행과 구분한다. 원본 저장 KOR_2020_01_01_01·KOR_2020_07_04_11의 PRK 금수34개 목록을 사용한33개(KOR 제외)·34개 표시 표본도 검사한다. 사진의11월 상태·시청국 포함 여부·실제 줄 순서는 미확정이므로 알파벳순 시험 입력이라고 구별한다. 사진 마지막 EU 모양은 BRS이며 TFR country_tags/TFR_countries.txt:194와 설치 번역00_TFR_countries_l_korean.yml:169–171의 브뤼셀 유럽위원회에 대응한다. 앞64항목 밀도 실험은 보조 증거이며 현재 원본 밀도 검사를 대신하지 않는다. 원본 `GFX_small_flag_frame_thin`의 알파·원본 `maskedflag.shader` 기본 RGB는 변경하지 않는다. 그림자 왼쪽4~5px보다 좁게 쌓이면 국기색이 가려지는 원본 합성과, 서로 다른 목록을 찍은 사진의 밝기를 구별한다. `DiplomacyArtTest`는 두 해상도에서 표시 전용 좌/우 클릭·즉시/지연·끝까지 스크롤·그림 토큰과 실제 초상 픽셀을 검사한다.

## 중점·연구 전체 화면 틀

연구 트리의 원본 배경·연도·노드·연결선과 입력 좌표는 창 왼쪽 끝·탭 바로 아래를 원점으로 사용한다. 보드 요청에 따라 가로·세로 스크롤 막대 그림은 없고 휠·우측/중간 끌기 이동은 유지한다. 상세를 열어도 같은 원점·스크롤·전체 노드가 유지되며 상세 밖 기술의 호버·선택은 동작한다. 원작 사진(20261121053029)대로 연구 중인 기술의 그림 틀은 연구 완료와 같은 초록색이고, 그 기술로 들어오는 연결선은 원본 `GFX_techtree_line_{right,left,up,down}_currently_researching`(`techtree_line_*_strip.dds`, 12프레임·초당 12)의 `>>>` 줄을 선 방향대로 이어 붙여 프레임마다 움직인다(`research/line/<방향>/frame_0..11`). 나머지 연결선도 원본 `GFX_techtree_line_*_states`(TFR `techtree_line_horisontal.dds`·`techtree_line_vertical.dds`, 4프레임)를 선 방향으로 이어 붙이며 색은 들어가는 자식 기술의 상태를 따른다(사용자 확인 5단계): 불가=1번 어두운 회청색(그 기술의 배타 연구가 완료·진행 중이라 서버 `canResearch`가 거부), 다음=2번 회색(선행·국가 조건 미충족), 가능=3번 노랑, 완료=4번 초록, 진행=`>>>`. 회색·노랑·초록은 원작 사진의 선 단면 색과 일치한다. 꺾임·분기·교차점에는 원본 `GFX_techline_center_<종류>_states`(TFR `techline_center_*_researched.dds`) 9종(┌ top_left, ┐ top_right, └ bottom_left, ┘ bottom_right, ┬ down, ┴ up, ┤ left, ├ right, ┼ all)을 선 위에 그리고, 연구 중 경로의 꺾임은 원본 `GFX_techline_center_<종류>_currently_researching`(12프레임·초당 12)을 쓴다. 같은 줄기를 여러 선이 공유하면 불가→다음→가능→완료→진행 순으로 그려 높은 상태가 위에 보이며, 끝점을 공유하지 않고 지나가기만 하는 교차에는 중앙 조각을 그리지 않는다. 선 굵기 16·한 칸 36·중앙 조각 16 원본 px는 배치 좌표와 같은 0.4×트리 배율 GUI로 그리고, 마지막 칸은 남은 길이만큼 원본 텍스처를 잘라 틈 없이 잇는다. `research-catalog` 묶음이 실제 TFR 보병 트리에서 화살표 색 픽셀과 두 캡처 사이 움직임, 노랑 선 채움의 원본 굵기(GUI2 2560×1440에서 9~11px), 모든 선·중앙 조각 그림의 존재를 검사하고, 단위 테스트(`ResearchTreeLinesTest`)가 꺾임·분기·교차 판정과 상태 순서를 검사한다.

중점과 연구 계통도는 HUD 아래 전체 화면을 사용하며 이동 입력을 잡는다. 연구 개요의 슬롯 패널과 계통도 진입은 유지한다. 바깥 배경은 TFR `nationalfocusview.gui`·`countrytechtreeview.gui`의 `GFX_tiled_plain_bg2`를 재사용한다. 중점 본문은 원본 `GFX_tiled_bg` 위에 `GFX_tiled_window_1b_border`, 연구 본문은 `GFX_tiled_window_3b_border`를 원본 borderSize64의 타일링으로 그린다. 팩의 기존 `research/source/`, `doctrine/`, `commander/commands/` 원본 경로를 사용하며 게임 그림을 JAR에 넣지 않는다.

중점 계통도의 확대/축소는 TFR `nationalfocusview.gui`의 `zoom_slider_container`(창 오른쪽 위 (−233,10), 244×37)다. `GFX_focus_zoom_out`(팩 `focus/zoom_out`, −)과 `GFX_focus_zoom_in`(팩 `focus/zoom_in`, +) 42×35를 컨테이너 가운데 (122,18.5)와 (157,18.5)에 중심을 맞춰 원크기×HUD 배율(GUI2에서 원본 1px=물리 1px)로 그린다. 두 그림은 7px 그림자 칸이 겹치므로 클릭 영역은 겹친 칸 가운데에서 나눈다. 툴팁은 `FOCUS_ZOOM_SLIDER_TOOLTIP`, 단축키는 원본처럼 Page Up(확대)·Page Down(축소)이며 한 번에 1.15배다(원작 확대 단계는 엔진 값이라 미확인). 닫기 단추도 원본 `close_button` (−45,13)의 `GFX_closebutton` 32×33을 같은 배율로 둔다. 원작 중점 화면 Steam 사진이 없어 GUI 선언과 원본 그림으로만 대조했다. 두 반쪽 그림은 기호가 가운데 구분선 쪽으로 치우쳐 있어(− 2px 오른쪽, + 3.5px 왼쪽, 원작 20261121090949도 같음) 사용자 v47 지시대로 원본 픽셀의 기호 띠만 행별 단색 배경으로 덮고 − 는 3px 왼쪽, + 는 3px 오른쪽으로 옮겨 각 반쪽 중앙에 둔다(겹침 순서는 원본 선언대로 − 반쪽이 위). 검색칸은 원본 `find_view`(창 오른쪽 위 (−344,8))의 `GFX_FOCUS_FILTER_FIND_BG`(팩 `focus/find_editbox`, 200×39, 돋보기 포함)를 (1,1)에 그리고, 입력 글자는 `to_find_text` (41,14) 150폭, 안내 문구는 `FOCUS_SEARCH_TYPE_TEXT`("입력하여 검색", 흰색), 글자가 있으면 `GFX_focus_clear_text`(팩 `focus/clear_text`, 18×18) 지우기 단추를 (185,21) 중심에 둔다.

중점 계통도는 원본 `national_focus_item`의 그리기 순서를 따른다: 연결선 → 상호 배타 표시(`national_focus_exclusive_item`) → 중점 칸(`bg`, 상태별 `focus_*_bg` 168×50, 위 8줄은 장식 윗칸) → 아이콘(`symbol`) → 이름. 그래서 배타 표시는 사이에 놓인 중점과 이름 칸 뒤로 들어가고, 완료 중점의 아이콘 아래쪽이 완료 칸 윗칸 뒤로 가려지지 않는다(사용자 v47 지적, 원작 `20261017214531_1.jpg`의 배타 표시 대조).

연구 탭은 TFR `countrytechtreeview.gui/folder_tabs`의 x22→734, 간격89px·y7과 86×61px 그림을 GUI2로 환산한다. `countrytechtreeview.gui:30`의 루트 x−3과 `:3895`의 첫 탭 x22를 합친19가 클라이언트 배치식의 시작값이며, 사진에서 재거나 별도 여백을 뺀 값이 아니다. 각 탭은 `round((19 + 89·i) / 2)`GUI, 폭43GUI, HUD 메뉴 아래4GUI에 놓인다. 첫 탭은10GUI이고 반올림 전 간격은44.5GUI다. 순서는 보병·지상 & 항공 지원·기갑·포·해군·해군 지원 장비·공군·공학·산업이다. 위 탭 아래에 본문 프레임을 겹치고 내부 내용은 탭 하단부터 표시한다. 상세 창·툴팁과 연구/MIO 요청의 권한·서버 상태는 기존 경로를 사용한다.

중점·연구의 배경과 본문은 별도 그리기 층에서 시작한다. HUD 단추 줄 오른쪽에 추가했던 배경 채움은 보드 지시로 제거한다. 교리·중점·연구 계통도는 HUD 막대·세계 긴장도를 먼저 그리고 창을 그 위에 올린다. HUD 툴팁은 창을 그린 뒤에 표시한다. 중점·연구는 HUD 높이(41GUI), 교리는 y41GUI 위쪽 경계와 기존 그림을 유지하며 긴장도 입력은 그 경계까지로 제한한다. 교리의 탭 오른쪽 투명 영역도 창 영역이므로 HUD 배경은 같은 경계에서 클립한다. 드러난 DEFCON의 호버·세계 긴장도 열기는 유지하고, 겹친 자리의 호버·클릭은 창이 받는다. 알림 뒤에서도 세 화면이 자기 HUD를 그려 같은 층 순서를 유지한다. 연구 슬롯 개요에서는 기존 HUD를 유지한다. 새 막대·칸·그림을 추가하지 않는다. 중점 제목은 원본 y8px를 GUI2의4GUI로 환산한 HUD 아래 위치에 둔다. 연구 탭·HUD는 상세 창보다 먼저 그리고 상세 단추·툴팁은 그 뒤에 그려 상단 제목이 탭 뒤로 숨지 않게 한다. 상세를 열어도 분야 탭은 보존하며 기존처럼 탭 입력을 비활성화한다. 같은 연구 분야에서 상세를 열 때는 트리의 가로·세로 위치를 유지한다. 슬롯 개요나 다른 분야에서 상세로 진입할 때만 대상 기술을 중앙에 배치한다. 상세 창 밖의 기술 툴팁과 좌클릭은 해당 기술 상세로 이어지며 연구 시작·취소를 보내지 않는다. 상세 위에서는 본문 스크롤과 단추 입력을 유지한다.

`HudFullscreenTest`는 임시 Fabric 테스트 수신기를 등록해 노출 DEFCON의 툴팁·화면 열기·요청 전송, 겹친 자리의 툴팁 차단·좌우 반복 클릭, 중점·연구 닫기 단추와 지표 툴팁을 두 해상도·직접 화면/알림 스택 경로에서 검사한다. 수신기는 검사 뒤 해제하며 운영 서버 검증을 대신하지 않는다. 세 화면의 상단 캡처는 입력8 `kor-selected-menu.json`의 실제 HUD를 공용 fallback으로 사용하고 검사 전 상태를 복원한다. 화면 응답에 HUD가 있으면 그 응답이 우선한다. `ResearchBackgroundTest`는 전체 분야 목록을 사용해 두 해상도에서 상세 창 밖의 기존 트리 픽셀을 비교하고 선택 강조 부분을 제외한다. 창 밖의 다른 기술을 호버·클릭하는 장면도 포함한다. `FocusScreenTest`는 실제 노드 클릭 전/상세 열기/닫기 후 pan·zoom과 노출된 트리 픽셀을 비교한다. 별도의 `tree-only` 새 화면 진입 캡처나 기술3개만 담은 `ResearchSectionsTest` 입력은 실제 클릭 후의 전체 트리 보존 근거로 사용하지 않는다. 중점의 겹친 메뉴 HUD 픽셀 검사와 상세·MIO 입력 검사도 유지한다. HudFullscreenTest는 교리·중점·연구 각각 GUI2 두 해상도에서 노출된 긴장도 입력과 창이 덮는 영역의 입력 분리를 검사하고 동일한 상단 비교용 프레임을 남긴다.

집중 재현은 팩 선빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=fullscreen-ui -PhoiServerSource=<고정 입력>/HOI -PhoiProtocolSource=<고정 입력>/HOI/protocol/src/main/java -PhoiResourcePack=<팩 ZIP> --console=plain --no-daemon`이다. `HOI_COMMANDER_PREVIEW`는 같은 고정 입력의 `fixtures/commander-source-fixtures`이며 실제 중점 상세 장면은 기존 `HOI_FOCUS_PREVIEW` 서버 export를 지정한다. 집중 범위는 중점 입력·상세·원본 픽셀, 연구9탭·본문·상세·MIO이며 모의 요청/리플레이 검사와 실제 운영 접속·다중 사용자·직접 청취를 구분한다. 무필터 전체 검증은 별도 시작 신호가 필요하다.

`-PhoiTestSuite=research-catalog`는 같은 GameTest 등록과 리소스팩 로딩을 사용해 `ResearchScreenTest.fullTfrCatalog`만 실행한다. 2560 라벨·9개 탭의 정확한 순서/좌표/폭, 854×480 라벨·산업 상세, 빈 슬롯 선택, 상세 카드4종과 로딩 중 빈 국기를 끝까지 검사한다. 기존 `all` 경로의 마지막 호출은 유지한다. 위 `fullscreen-ui` 재현 명령에서 필터만 바꾸며 고정 서버 입력·프로토콜·팩 경로도 함께 지정한다. 이 집중 실행은 무필터 전체·운영 접속·다중 사용자 검증을 대신하지 않는다.

## 비행단 이름 표시

비행단 이름은 서버 `Member.name`·`Window.unit`·`Feedback.unit`과 선의 원문 툴팁을 그대로 표시한다. 공군 행의 `airLabel`은 기존 글자 배율·칸 위치·너비를 유지하고 넘치는 문구를 `UiText.scrollingCell`로 가로 스크롤한다. 말줄임이나 글자 축소로 이름을 생략하지 않는다. TFR `airselectionview.gui`의 `unit_name` x233/y7·maxWidth120을 반 배율로 유지한다. 배경은 기존 원본 `airselectionview.gfx/GFX_AV_wing_entry_bg` → `gfx/interface/airview/wing_entry_bg.dds`이며 새 그림·문구를 추가하지 않는다. 기종별 장비 목록은 비행단 이름과 구분한다.

입력4의 제1전투비행단은 글자 폭54GUI로 60GUI 칸에 들어간다.

v26 고정 입력의 영문 합성 해군·공군 목록(`kor-command-panel-navy*.json`·`air*.json`·`air-following-*.json`)을 그리던 이전 목록 더미 테스트(`air-layout` 묶음, `CommanderPanelsTest`의 해군·공군 종류·이름 스크롤·추종 결과)는 사용자 요청(2026-11-19)으로 삭제했다. 현재 해군 배치는 `navy-layout`, 육군 카드는 `army-layout`이 검사한다. `raids`는 실제 중국 선 툴팁을 시험 지도 좌표에 연결해 이름·줄바꿈과 좌우 입력을 검사하며 시험 지도 좌표로 옮긴 장면임을 구분한다. 네 상태 비교는 PRC preparing/prepared/launchable/in-progress 응답 전체를 각각 읽고, 창만 닫아 선을 노출한다. 상태·툴팁·진행률·세션·revision·발급 동작은 원본 응답 그대로 유지한다. 입력4의 한국 이름은 제1전투비행단~제14전투비행단, 중국 습격 이름은 Hongzhaji Shi 1이다. 원작 사진의 Hongzhaji Shi 5와 캠페인 시점·부대가 다르다. 무필터 전체 검증은 최종 입력 및 지정된 시작 신호 뒤 수행하며 운영 접속·다중 사용자·직접 음향 청취는 별도 검증이다.

공군 임무 배정 목록은 서버 `mission/batch`의 `tiles[].name`을 표시하며 `choices[].id`와 같은 행을 클릭해 발급된 token/revision으로 요청한다. 현재 공군 HUD는 실제 군·집단군 카드를 사용하며 구형 비행단 역할의 카드는 `CommanderHud`의 wing 분기와 `AIR_MISSION` 요청을 유지한다. 임무 배정의 이름 칸은 기존224GUI·23GUI를 유지하고, 들어가는 이름은 기존 중앙 표시를 보존한다. 넘치는 `unit/` 이름만 같은 글자 크기의 `UiText.scrollingCell`로 표시한다. 입력4의 제1~제14전투비행단은 모두 칸 안에 들어간다.

## 결정 폭·호버 계층·연구 조합 입력

사건과 결정 사이드바는 TFR `interface/countrydecisionview.gui/countrydecisionview`의 폭550px를 GUI2의275GUI로 사용한다. 화면이 더 좁을 때만 가용 폭으로 제한한다. 제목 길이로 창을 넓히지 않으며 사건 제목·분류·결정 이름은 기존 글자 크기와 원문을 유지한 가로 마키로 읽는다. 결정 행에서는 이름 글자만 흐르고 왼쪽 결정 그림·대상 국기와 오른쪽 비용 아이콘·값은 고정한다. 명시적 줄바꿈과 설명 본문의 기존 줄바꿈·스크롤은 보존한다. 이전의 긴 이름에 맞춘 창 확장·자동 줄바꿈 행 높이 규칙은 대체한다. 비용 숫자는 정치력·사용자 정의 비용 모두 `Row.costMet=false`일 때만 빨강이며 그 밖에는 노랑이다. TFR `interface/core.gfx` 기본 `textcolors`의 `R={255 50 50}`·`Y={255 189 0}`을 사용하며 비용의 원본 아이콘·이름 서식은 보존한다.

Fabric26.3의 `ScreenEvents.afterExtract`는 Minecraft의 기본 툴팁 추출 뒤에 호출된다. 기본 툴팁과 공통 스크롤·정치·결정 툴팁을 최종 `Gui.extractRenderState` 끝에 그리므로 전구 카드·군 패널·지도 모드 같은 후속 HUD가 가리지 않는다. 스크롤 범위는 요청 시점에 계산하고 그림만 늦추며 위치 변환·휠 상태를 보존한다. 캠페인과 무관한 바닐라 툴팁의 추출 순서는 유지한다. 독립 `GuiGraphicsExtractor`로 그림을 관찰하는 테스트는 `extractDeferredElements` 뒤에 최종 GUI 단계와 같은 `TooltipLayer.flush()`를 호출한다.

중점 호버는 설명 본문을 붙이지 않는다. 기존 REFRESH 요청의 중점 ID로 상세를 받아 완료 시간·요구 조건·효과를 표시한다. 원문·색·순서는 보존하며 캐시는 국가·세션·트리·revision 변경 때 갱신한다. 호버는 상세창을 열거나 게임 행동을 실행하지 않는다. 기본 한국어 `core_l_korean.yml`의 `FOCUS_STATUS_TOOLTIP`·`FOCUS_STATUS_INPROGRESS_TOOLTIP`·`FOCUS_STATUS_COMPLETED_TOOLTIP`을 상태에 따라 선택한다. 노드 또는 상세가 완료이면 완료 문구를 우선하고, 남은 시간이 있으면 전체 기간과 남은 일수를 함께 표시한다. 이름 뒤와 완료 시간/상태 뒤에 각각 빈 줄 하나를 둔다. 효과 머리글은 `FOCUS_REWARD_TOOLTIP`의 `§H효과§!:`이며 ‘효과’만 노랑, 콜론은 기본 흰색이다. `CLICK_DETAILS_CLICK`·`CLICK_DETAILS_CTRL`도 같은 파일의 원문을 사용한다. 설치된3411756715 패치에는 해당 키 덮어쓰기가 없다. 상세 응답이 없으면 기간·조건·효과를 만들지 않는다. Ctrl+클릭으로 다른 메뉴를 함께 유지하는 기능은 현재 없으며 안내 문구를 기능 구현으로 간주하지 않는다. 원작 대조는 `20261114222018_1.jpg`다. 원본의 `CLICK_DETAILS_CTRL` 줄은 같은 화면의 상세 창이 하나라도 열려 있을 때만 붙는다(중점 `20261118210837_1.jpg`→`20261118210839_1.jpg`, 연구 `20261118210758_1.jpg`). hoi4.exe에서 이 키는 중점 툴팁 문자열 묶음(`FOCUS_STATUS_*`·`CLICK_DETAILS_SHIFT_START`·`CLICK_DETAILS_SHIFT_BYPASS`·`CLICK_DETAILS_ALT_START`)과 연구 툴팁에서만 쓰이며 다른 화면의 클릭 안내는 상세 창 상태로 바뀌지 않는다. 중점은 상세 창이 없으면 `CLICK_DETAILS_CLICK`만, 상세 창이 열려 있으면 그 다음 줄에 `CLICK_DETAILS_CTRL`을 붙인다. 연구도 상세 창이 열려 있을 때만 Shift/Alt 안내 뒤 마지막 줄에 붙인다. Ctrl+클릭 다중 상세 창·중점 Shift/Alt 시작·Shift 건너뛰기 동작은 아직 없다.

기술 좌클릭은 상세창을 연다. Shift+클릭은 상세창 없이 연구를 시작하고 개요로 돌아가며 Alt+클릭은 트리를 유지한다. Shift+Alt+클릭은 시작 응답 뒤 다음 빈 슬롯을 선택한다. 업체는 `DesignTeam.technologies`의 담당 업체 중 해당 기술의 `rates`가 가장 큰 업체, 동률이면 규모가 큰 업체, 다시 동률이면 서버 목록의 첫 업체를 고른다. 이는 원본 엔진의 선택 공식을 확정한 결과가 아닌 HOI 규칙이다. START에는 현재 고른 슬롯을 보내며 응답에서 해당 기술이 들어간 실제 슬롯을 찾아 최신 revision으로 DESIGN_TEAM을 한 번 보낸다. 중간 날짜 응답에 기술이 없으면 지정을 기다린다. 상세창을 열 때 미리 빈 슬롯으로 바꾸는 별도 자동 배정 규칙은 사용하지 않는다.

조작 안내는 기본 한국어 `core_l_korean.yml`의 `CLICK_DETAILS_SHIFT_RESEARCH`, `CLICK_DETAILS_ALT_RESEARCH`, `CLICK_DETAILS_SHIFT_ALT_RESEARCH`와 각 `_WITH_MIO` 키를 사용한다. 설치된 한국어 패치에는 덮어쓰기가 없고 현재 고정 팩 `command/labels.json`에도 없으므로 확인한 기본 한국어 원문을 fallback으로 보존한다. 업체명·규모는 서버 응답만 치환한다. 플라스크는 원작 `20261114222028_1.jpg`·`20261114222045_1.jpg`처럼 기술 그림의 왼쪽 아래에 표시하며 이전 오른쪽 배치는 대체한다. 외부 팩의 기존 `mio/ui/gfx_design_team_icon/frame_0` 원본을 재사용한다.

중점·연구 호버의 마지막 효과/배타 연구 줄과 클릭 안내 사이에는 `core_l_korean.yml`2행의 `SEPARATION_LINE: "--------------\n"`을 빈 줄 없이 넣는다. 대시14개는 기본 글자색으로 표시하며 원문 끝의 줄바꿈을 중복하지 않는다. `UiCommandLabels`의 대시 제거와 툴팁의 빈 줄 정규화에서도 이 원본 구분선을 보존한다. 다른 길이·모양의 기존 장식 구분선 정규화는 유지한다. 원작 중점 `20261114222018_1.jpg`·연구 `20261114222045_1.jpg`가 대조 기준이다.

개인 화면 요청은 마지막으로 받은 해당 화면 revision을 사용하며 HUD 시간 revision과 비교하지 않는다. 오래된 응답과 중복 응답은 기존 단조 증가 검사로 버린다. 정보기관·점령지 확인창은 날짜 revision 증가만으로 취소하지 않고 발급된 확인 내용·대상의 유효성이 바뀌면 폐기한다. 결정 목록 페이지·외교·무기대여·의용군의 같은 revision 검사와 서버의 재전송·미발급 revision 거절은 유지한다. 계약은 HOI `docs/README.md`의 개인 화면 세션 문단이다. 정보기관 작전 목록의 `network:` 전용 캡션은 제거한다.

집중 재현은 고정 팩과 검증된 서버·프로토콜 경로를 지정한 Java25 `test runGameTest remapJar -PhoiTestSuite=d2b --console=plain --no-daemon`이다. GUI2 2560×1440·1280×720에서 조합 입력의 START→DESIGN_TEAM 순서·실제 슬롯·중간 날짜 응답·동률 업체·마키 두 시점·사용자 정의 비용 두 색·후속 불투명 HUD 위 툴팁·설명 없는 중점 호버를 검사한다. 1280 GUI2에서는275GUI 결정 패널이 뒤쪽 뉴스 창의 최소화 단추를 가릴 수 있다. 기존 창 순서는 유지하며 사건 행을 눌러 창을 앞으로 가져온 뒤 최소화한다. `event-tooltips` 회귀도 실제 입력 대상 계층을 확인하고 이 경로를 사용한다. 합성 경계 검사는 서버 게임 계산이나 실제 소켓·멀티플레이를 대신하지 않는다. 원작과 Minecraft의 글꼴·줄바꿈 차이, Ctrl+클릭의 별도 창 유지와 직접 청취는 별도 범위다.

## 공작원 슬롯·해임과 국가 국기

공작원 지도 카드는 서버 `AgencyView.operativeCapacity`와 실제 인원 중 큰 수만큼 표시한다. 실제 인원이 최대치를 넘으면 모두 보존하며 빈 슬롯만 최대치까지 추가한다. 구형 응답의 최대치가 없으면 실제 인원만 표시한다. 별도 추가 +카드와 카드 아래 X/X 수치는 표시하지 않는다. 빈 슬롯은 기존 원본 `GFX_operative_unknown`을 사용하고, 발급된 모집 항목이 가능할 때 기존 모집창을 연다. 하단 카드의 원본 `dismiss_btn`(x61/y44)과 배지 초상의 빨간 해임 단추는 `confirms`의 발급 문구로 원본 확인창을 연다. 취소·Escape는 요청하지 않고 OK만 `dismiss:<id>`를 한 번 보낸다. 상단 선택 배지 X는 서버 요청 없이 선택만 해제한다. busy 비활성·대기 중 반복 클릭·낡은 응답 잠금·확인 중 행동 상태 변경과 국가 권한 회수 검사를 유지한다. 해임 요청은 `disband` 원본 음향 연결을 사용한다. 코드명·레벨·계급·경험치 툴팁은 서버 필드를 표시하며 클라이언트에서 계산하거나 저장하지 않는다. 모집창은 처음에 서버 후보 순서를 유지해 해고 후 복귀한 후보를 맨 위에 보이고, 기술·이름 정렬은 사용자가 눌렀을 때만 적용한다.

작전 상세는 준비 전 `OperationOffer.phases`와 진행 중 `Operation.phases`에 같은 원본 단계 그림·설명을 사용한다. `preparation`의 민간공장 수·기간·진행률·문구, `outlookLabel`과 완성된 `outlook` 툴팁을 그대로 표시한다. 보상 앞 국기는 대상국 `country`를 사용한다. 출처는 TFR `operationoverview.gui`, `operativeleader.gui`, `popupwindow.gui/dismiss_operative_popup`과 한글패치 `OPERATIVE_DISBAND_TITLE`·`OPERATIVE_DISBAND_DESC`다. 해고 창의 원본 `GFX_agency_popup_bg`·머리글·하단 자산은 기존 `completion` 자산을 재사용한다. `Network.nodes`의 `parent` 주 중심에서 해당 주 중심으로 흰 점선을 그리며, 빈 parent는 선을 만들지 않는다. 구형 수도 테두리 `segments`는 새 정보망 도형으로 사용하지 않는다. 원본 무늬·마스크·UV 0.9와 속도 0.2를 유지한다.

지도에서 연 해고 확인창은 지도 배경을 유지하고 취소하면 지도로 돌아간다. 정보기관 패널에서 연 확인창은 해당 패널로 돌아간다. 지도 공작원·임무 툴팁은 화면의 후행 렌더 단계에서 직접 그리며 현재 마우스 위치와 화면 경계를 따른다. 긴 원문은 휠로 스크롤하고 같은 항목의 스크롤 위치를 유지한다.

집중 재현은 팩 선빌드 뒤 Java25의 `test runGameTest remapJar -PhoiTestSuite=agency-map` 및 `-PhoiTestSuite=agency-operations`이며 같은 검증된 `-PhoiServerSource`, `-PhoiProtocolSource`와 `HOI_AGENCY_OPERATION_PREVIEW`를 지정한다. 지도 시험은 임시 차원 복구가 클라이언트에 도착한 뒤 종료해 다음 연구 화면이 차원 전환으로 닫히지 않게 한다. 지도 집중 묶음은 이 연구 화면 검사를 바로 이어 실행하고, 실제 툴팁 테두리 픽셀과 해고창 취소·OK·반복 입력·드래그·권한 회수를 검사한다. GUI2 2560×1440·1280×720의 모의 요청/실제 렌더 검사는 운영 서버 접속·멀티플레이·직접 효과음 청취를 입증하지 않는다.

외교 목록·국기 묶음, 첩보 작전 목록·상세·보상·모집/배정·지도 배지·수도 암호학, 포획 지휘관의 국기는 기존 정치 국기 선택 후 원본 국기만 그린다. `CountryFlags`의 small2/small/smallest 마스크·TFR 오버레이 합성은 보드의 철회 지시에 따라 제거했다. 원본에 따로 있는 프레임과 지도 줄무늬, 상단 외교/메뉴 국기의 기존 자산은 각 화면의 원본 기준을 따른다. 원소속국과 포획국의 위치·의미는 바꾸지 않는다. 밝은 포획 행의 이름·역할·기술·능력치·수용량은 검정이며 제목·탭과 툴팁 원문 색은 유지한다.

검증 재현은 팩 선빌드, 서버 전체 검증 후 클라이언트 `test runGameTest remapJar`다. `agency-map`은 슬롯 5/1/0 표시 입력과 실제 3명 보존, 발급 X의 좌우·반복 클릭·낡은 응답·실패·권한 회수를 검사한다. `captivity`는 실제 장교단 통계가 포함된 `kor-selected-menu.json`으로 열기 전후 부모 패널 픽셀을 대조한다. 이전 합성 메뉴 캡처의 통계 공란만으로 런타임 소실을 확정하지 않는다. `design-team-candidates`는 기존 서버 연구 슬롯과 서버 formatter의 4.51/4.00 경계 입력을 payload 왕복 후 실제 연도 호버로 확인한다. 경계 입력은 서식 검사이며 캠페인을 해당 날짜까지 진행한 증거가 아니다. 실제 운영 접속·멀티플레이·직접 음향 청취는 별도 검증이다.

교리 선택창은 TFR `interface/doctrines/doctrines_view.gui/doctrine_selection_list`의 560×576 원본 영역을 GUI2에서 280×288로 표시한다. 창·설명·후보 목록은 `countrytechtreeview.gfx`의 `GFX_tiled_window_w_close`·`GFX_tiled_paper_bg`·`GFX_tiled_paper_small_bg`를 원본 64px 테두리와 중앙 타일 반복으로 그린다. 목록 배경은 원본 x20/y170부터 너비520·높이346이며, 후보 위치·간격은 유지한다. 후보 배경은 16px, 선택 강조는 32px 원본 테두리를 쓴다. `-PhoiTestSuite=doctrine`은 후보 미리보기·확정 단추·후보별 보상과 원문 툴팁을 함께 검사하며 최종 무필터 검증과 구분한다.

연구 상세의 닫기는 TFR `countrytechtreeview.gui/technology_info_window/close`의 오른쪽 -43px·위 10px를 GUI2에 맞춰 배치한다. 트리의 군수산업체 플라스크와 해당 호버 영역은 보드 지시에 따라 오른쪽 아래에 놓는다. 상세 연구 단추와 트리 모두 서버 `DesignTeam.technologies`에 해당 기술이 있는 업체가 있을 때만 플라스크를 표시한다. 업체가 없는 기술은 기존 업체 없는 연구 요청 경로를 사용한다. 포 장비 크기는 제공된 `20261030140549_1`의 왼쪽 연구 슬롯을 기준으로 한다. 트리 그림 변경으로 해석하지 않으며 슬롯 원본 크기 규칙은 아래에 기술한다. 재현은 `screen-polish`·`research-background` 집중 묶음이고 실제 운영 접속·멀티플레이 및 최종 무필터 검증과 구분한다.

HOI 화면과 캠페인 HUD에서는 Minecraft `AbstractWidget.handleCursor`가 비활성 위젯에 요청하는 `CursorTypes.NOT_ALLOWED`를 공용 `GuiGraphicsExtractor.requestCursor`에서 화살표로 바꾼다. 원래 활성 여부·클릭 차단·툴팁은 유지한다. 적용 대상은 공용 메뉴/패널 버튼의 권한·비용 부족/응답 대기 상태, 연구 슬롯·분류/상태 버튼, 교리 후보와 군종 버튼, 생산·사단·HQ 설계 버튼, 정보기관의 비활성 집계/작전/업그레이드 버튼이다. `UiHoverRegion`은 원래 커서를 요청하지 않는다. 일반 Minecraft 메뉴의 커서는 유지한다. 렌더 캡처에는 운영체제 커서가 포함되지 않으므로 사단 설계 검사는 실제 호버 뒤 Window의 커서 상태도 확인한다.

육군 색상 선택 영역은 원본 `unitview.gui/army_color_button`의 오른쪽 x405/y10 위치를 따른다. 왼쪽 군 방패와 사단 수는 그대로다. 해군은 색상 팔레트의 추가 함대 휘장 선택 단추와 태스크포스 표식 선택 클릭을 제거하고 원본 휘장 표시·함대 색 변경은 유지한다. 사단 편제의 빈 대대 배경은 호버 프레임으로 바꾸지 않는다. 편제/이름 태그 목록은 대대 위젯 뒤에 배경과 항목을 함께 그리며, 원본 `tiled_window_1b_thin_border`의 64px 테두리와 불투명한 중앙을 사용한다. 기존 위치·간격·선택 및 스크롤 요청은 유지한다. HQ의 대대·사령부 지원 글자는 원본 composition 배경 이름판의 상하 중앙에 둔다. `division-designer`·`headquarters`·`navy-layout`·`army-layout`은 렌더/모의 요청 검사이며 실제 서버의 HQ 추가 승인·복제 이름·저장 동작은 서버 검증과 별개다.

포획 지휘관 예시 첨부 `3583217c-d816-4b3c-8863-2d079c752ceb`는 사용자가 확인한 원본 게임 화면이며 TFR 실행 사진으로 분류하지 않는다. 설치된 바닐라와 TFR `unitleaderwindow.gui/capturedarmyleaderentry`의 초상 x56/y5·scale .42, 철창 x49/y-2, 특성 x126/y60, 원소속 국기 x7/y59와 포획국 국기 x100/y66은 같다. TFRKP에는 해당 GUI 덮어쓰기가 없다. 두 탭은 같은 행 선언을 사용하되 인물·국가·특성은 서버 응답을 표시한다. TFR의 실제 배경·초상·특성 그림은 바닐라 예시와 다를 수 있으므로 사진 전체가 일치한다고 주장하지 않는다. 원본에 없는 하단 새로고침·페이지 단추 대신 세로 스크롤과 검색 Enter 재시도를 사용한다. 지휘관·제독 목록 하단은 TFR의 `GFX_button_261x34`·`GFX_button_221x34`를 사용한다. 재현은 `-PhoiTestSuite=captivity`와 `-PhoiTestSuite=commander-layout`이며 운영 접속·멀티플레이·직접 효과음 청취는 별도 확인 대상이다.

[README](../README.md) · [구조와 검증](refactoring.md) · [음악](audio.md)

## 공통 기준

설계 군수산업체는 `IndustryTankDesigner.designTeam`의 후보·현재 배정·구식 여부를 표시한다. 업체 칸의 왼쪽 클릭은 후보 목록, 배정된 칸의 오른쪽 클릭은 `TANK_DESIGN_TEAM(other="")`, 후보 선택은 같은 동작의 업체 ID, 노란 개량 화살표는 `TANK_DESIGN_TEAM_UPGRADE`를 보낸다. 후보와 개량 호버의 능력치 변화는 서버 `changes`·`upgradeChanges`만 사용한다. 연구 슬롯의 연도 호버는 `Slot.yearTooltip`을 그대로 표시한다. 입력 대기 중 중복 요청은 차단하며 저장은 기존 `TANK_SAVE`를 유지한다.

정보원 모집은 서버 `candidates`가 준비된 경우 원본 `unitleaderwindow.gui/operativeleaderwindow`와 `operativeleader.gui`의 후보 창을 연다. 후보 선택은 `CALL item=hire:candidate/N`이고 이전 즉시 `recruit` 실행은 보내지 않는다. 검색·정렬은 표시 목록에만 적용하며 초상·특성·국기·계급·이름은 응답에서 가져온다. 열려 있는 후보 창의 비공개 갱신은 원래 기관 화면으로 전달한다. `-PhoiTestSuite=design-team-candidates`는 고정 서버 입력의 한국 설계 업체 세 상태·중국 모집 후보·중국 연구 슬롯을 GUI2 두 해상도에서 재현한다. 실제 운영 접속·멀티플레이·직접 효과음 청취는 미수행이며 집중 검사와 최종 전체 결과는 별도로 기록한다.

모집 후보 행은 원본 TFR 사진 `20261028132043_1`의 회색 `leader_selection_entry_bg`를 사용한다. GUI의 기본 배경 선언은 해군의 파란 행이지만 실제 모집 사진은 회색이므로 화면 사진을 우선한다. `STATUS_RECRUIT_OPERATIVE`와 `RECRUIT_OPERATIVE_TITLE`은 한글패치 `TFR_replace_operatives_l_korean.yml`의 원문이며, 오른쪽 준비 표시는 기본 게임 `core_l_english.yml/READY`와 같은 사진의 한국어 표기를 따른다. 업체 목록의 위치는 `industrial_organization_list.gui/position_equipment_designer` x265/y80이고 원본 550×667 크기를 GUI2에 맞춘다. 서버 `designTeam.empty`는 원본 `INDUSTRIAL_ORG_EMPTY_GENERIC`의 `군수산업체 미사용`을 그대로 표시한다. 생산·연구의 빈 문구와 구분한다.

HQ 스크롤 툴팁 위에서 누른 클릭은 뒤쪽 버튼으로 전달하지 않는다. `captivity` 연계 검사의 직접 입력은 새 창을 연 뒤 커서를 옮겨 다른 항목의 툴팁이 남지 않게 하고 Shift/Ctrl 제거·중복 입력을 확인한다. 창을 열기 전 커서 이동만으로는 게임 화면의 마우스 고정 해제 후 위치까지 보장하지 않는다.

HQ 설계는 원본 `divisiondesignerview.gui`와 `hq_support.txt`에 따라 맨 위 첫 대대, 아래 세 대대, 지원 네 칸을 배치하고 둥근 숫자에 서버 전장 너비를 표시한다. 빈 칸의 `+`와 자물쇠 및 클릭 허용 여부는 `Slot.reason`을 따른다. 아래 `+`는 첫 대대 ID와 `battalions.size()`로 `HQ_SLOT`을 보내고, 채운 아래 칸은 해당 index와 빈 item으로 바로 제거한다. 맨 위·지원 칸은 후보를 열며 전열 후보는 서버 `Choice.group`과 `group_*_title`로 보병·기동·기갑 대분류와 소분류를 오간다. 서버가 발급한 제거 후보를 첫 칸에 두고 현재 후보는 `(현재 선택됨)`으로 비활성 표시한다. 뒤로·Esc는 소분류→대분류→설계 순서다. `Slot.tooltip`과 `Choice.tooltip`은 추가 문구 없이 유지한다.

채운 아래 칸의 호버는 원본 `GFX_remove_icon`, 맨 위와 채운 지원 칸은 `GFX_replace_regiment_slot`을 사용한다. 슬롯 배경의 흰 호버 프레임은 사용하지 않는다. Shift+왼쪽 클릭은 채운 맨 위·지원 칸에서 빈 item과 `line`/`support`, 해당 index를 보내며, Ctrl+왼쪽 클릭은 맨 위 칸에서 `line_all`, amount=0을 보낸다. 아래 칸의 수정키와 수정키+오른쪽 클릭은 요청하지 않는다. 반복 클릭은 다음 권위 있는 revision 응답까지 차단한다. GUI2 두 해상도의 재현 명령은 기존 `headquarters` 묶음이며 원본 비교는 Steam `20261028095229_1.jpg`~`20261028095310_1.jpg`를 사용한다. 자동 렌더/모의 요청은 운영 서버 접속·멀티플레이·직접 음향 청취 검증과 구분한다.

HQ 설계 하단에는 원본 `GFX_button_221x34` 저장 단추만 둔다(x487/y667, 221×36 원본 픽셀). 표시는 서버 `DESIGNER_ARMY_SAVE`와 `experience`를 사용하며 `saveReason`이 있으면 비활성이다. 오른쪽 위 X(x1156/y6)와 Esc는 기존 미저장 확인 및 `HQ_DISCARD` 경로를 공유한다. 장교단 HQ 목록의 원본 설계·편집·삭제 단추는 설계 뒤에서도 보이며 각각 `HQ_NEW`·`HQ_EDIT(item=ID)`·`HQ_DELETE(item=ID)`를 보낸다. 설계와 겹친 뒤쪽 단추는 입력을 받지 않는다. 후보의 `Choice.experience` 증감은 음수도 그대로 유지하고, `Slot.groups`의 대분류 추가 비용과 서버 labels의 비용 없음 문구를 구분한다. 혜택 문구의 원본 £토큰은 기존 키워드 글꼴로 그린다. `server-input-2`의 307개 파일 해시와 공유 프로토콜 48개를 확인한 계약 렌더는 GUI2 2560×1440·1280×720에서 통과했다. `headquarters` 집중 검사는 1분 10초이며 X/Esc, Shift/Ctrl 제거, 반복 클릭, 우클릭·잠긴 칸, 세션·revision·권한 회수를 포함한다. 같은 이름의 대대도 맨 위·아래 칸을 실제 위치로 구분해 각 서버 툴팁을 검사한다.

흰색·밝은 회색 호버 테두리를 추가하지 않는다. HQ 대대 후보, 무역 국가 행, 국가 선택, 연구 분류 탭, 세계 긴장도와 공용 control의 코드 호버 외곽선은 표시하지 않는다. 원본 배경 그림, 툴팁, 클릭 요청과 클릭·키보드로 선택된 연구/건설 항목의 상태 표시는 유지한다.

툴팁 공용 경로는 `단축키:`·`키 설정:`·`Shortcut:`·`Hotkey:` 줄과 `또는 £ctrl + £b` 같은 키 안내 꼬리를 제거한다. 해군 임무의 키 그림 변환도 사용하지 않는다. 삭제 뒤 가운데 빈 줄은 최대 한 줄이며 문장 색·들여쓰기·수치 정밀도는 유지한다. `test runGameTest remapJar -PhoiTestSuite=tooltip-spacing`은 서버 고정 입력의 초계·기뢰 부설·명령 대기, 외교·특성 툴팁과 스타일이 나뉜 단축키 줄을 GUI2 두 해상도로 검사한다. 실제 운영 접속·다중 사용자 검증과는 별개다.

모든 툴팁에서 색 코드와 공백을 제거한 뒤 가로선 문자 세 자 이상만 남는 줄은 빈 줄로 바꾼다. 하이픈·대시, `─`·`━`·`═`·`ㅡ`·`ᅳ`·`−`·`_`·`=`·`~`·`～`와 이들의 혼합 및 사이 공백도 포함한다. 연속 빈 줄은 하나로 합치고 툴팁 맨 앞·뒤 빈 줄은 제거한다. `-`·`--` 같은 짧은 표식, 음수 수치, 날짜, 글이 섞인 줄은 유지한다. 스크롤 툴팁의 최소 80GUI 폭과 그림 없는 정치 툴팁의 고정 폭을 제거하고, 줄바꿈 후 최장 줄의 실제 글자 폭에 기존 테두리 여백을 더한다. `ArmyDivisionTemplates`의 184 같은 인수는 여전히 최대 줄바꿈 폭이며 배경의 고정 폭이 아니다. 이미지·국기 툴팁은 그림과 국기 폭도 포함한다. 결정 배너·일반 본문의 높이 예약용 앞뒤 빈 줄은 이 툴팁 여백 제거의 대상이 아니다.

사건·결정 툴팁은 원문 구획을 합친 뒤뿐 아니라 글꼴 줄바꿈을 마친 표시 블록에서도 연속 빈 행을 한 줄로 제한한다. 긴 들여쓰기가 별도의 빈 행으로 감겨도 같은 규칙을 적용한다. 일본 자민당 파벌 툴팁의 배너 앞 14개·뒤 10개 줄바꿈은 원본 입력에 남겨 두되 툴팁 앞뒤 여백은 제거하고 구획 사이는 한 줄로 표시한다. 툴팁의 원본 `texticons/banners/` 이미지는 기존 `UiAssets` 알파 경계 캐시와 `drawContent`를 사용해 완전히 투명한 테두리를 제외한다. 일본 자민당 배너는 원본 300×235 중 y0..107이 투명하므로 반 배율의 표시 높이가 118GUI에서 64GUI로 줄고 보이는 300×127 그림은 같은 배율을 유지한다. 원본 파일·해시, 초상화 이름표, 일반 패널의 이미지 배치, 원문 색·아이콘·숫자는 유지한다. `decisions` 집중 검사는 실제 일본 파벌 입력과 긴 들여쓰기 합성 입력을 80/160/324GUI 폭에서 검사한다. 사건 선택지 검사는 같은 렌더 경로의 빈 행 제한을 확인한다. 실제 운영 일본 캠페인 접속은 별도 검증 대상이다.

재현은 팩 빌드 뒤 Java25에서 `HOI_COMMANDER_PREVIEW=<검증된 완전한 fixtures>`를 지정하고 `test runGameTest remapJar -PhoiTestSuite=tooltip-layout -PhoiServerSource=<고정 서버> -PhoiProtocolSource=<고정 서버>/protocol/src/main/java --console=plain --no-daemon`으로 실행한다. GUI2 2560×1440·1280×720에서 실제 외교·특성·편제 툴팁과 편제/해산 확인창을 렌더하고, 합성 문구로 짧은 폭·혼합 구분선·앞뒤 여백·색상·정밀도를 검사한다. 서버 요청은 모의 수집이며 실제 접속·비용·저장·다중 사용자 검증은 별도다.

`-PhoiTestSuite=trait-experience`는 `HOI_COMMANDER_PREVIEW`의 `kor-jeon-trait-tree.json`·`kor-navy-trait-tree.json`에서 사막의 여우·특공대·요새 파괴자·바다의 늑대 툴팁을 GUI2 두 해상도로 촬영한다. 시작 경험치 0과 획득 조건은 서버 입력 그대로 표시하며 클라이언트가 기본값이나 조건을 만들지 않는다. 요새 파괴자의 선행·상호 배타·지휘력 조건과 경험치 조건도 각각 서버 문구를 유지한다. 캡처 옆 JSON에 원문·커서·툴팁 경계를 보존한다. `review-tooltips`는 단축키·빈 줄 검사와 이 네 특성 검사를 함께 실행한다. 이 검사는 서버의 전투 경험치 획득·특성 해금 로직 검증을 대신하지 않는다.

- GUI2를 크기·간격·정렬의 기준으로 사용하며 다른 배율·창 크기는 호환성을 검사합니다.
- 원본 이미지의 비율·프레임·투명도를 보존합니다. 사진·아이콘·텍스트는 각 셀의 경계 안에 표시합니다.
- 한국어는 TFR 한글패치의 키와 국가별 덮어쓰기를 따릅니다. 명령·저장 ID는 번역하지 않습니다.
- 툴팁의 원문 색상·소수 자릿수·아이콘을 유지하고 가로선 형태의 구분선은 빈 줄로 표시합니다. 효과 수치는 서버 계산을 사용합니다. 모든 여러 줄 본문과 툴팁에서 가운데 연속 빈 줄은 최대 한 줄로 표시합니다. 일반 본문의 맨 앞·뒤 예약 빈 줄은 유지하고 툴팁의 맨 앞·뒤 빈 줄은 제거합니다. 공백·서식 코드만 있는 줄과 구분선은 `TooltipText.blank()` 기준으로 판단합니다. 서버 응답·저장값은 수정하지 않습니다.
- 미상 값을 화면에서 임의 추정하거나 게임 상태를 초기화하지 않습니다. 원본 참고 설명과 실제 적용되는 효과를 구분합니다.
- 상단 HUD 툴팁은 왼쪽 정렬과 원문 전체를 유지합니다. 공통 최대 폭181GUI로 줄을 바꾼 뒤 화면 높이를 넘는 GDP 등의 설명은 같은 항목을 가리킨 채 세로 휠로 끝까지 읽습니다. 글자 크기·문구·색·빈 줄을 바꾸거나 스크롤 안내를 추가하지 않습니다. 가로 휠은 기존 HUD 이동을 유지하며 항목·화면이 바뀌거나 커서가 떠나면 설명 스크롤을 초기화합니다. 긴 설명은 기존 공통 스크롤 렌더러로 후행 툴팁 단계에 그려 패널 위에 표시합니다. `runGameTest -PhoiTestSuite=hud-tooltips`는 실제 HUD 원문으로 GUI2 2560×1440·1920×1080·1280×720의 폭·화면 경계·맨 끝 접근·되돌림·호버 해제를 검사합니다. 실제 운영 접속과 다중 사용자는 별도 확인입니다.
- 상단 HUD 바는 TFR `topbar.gui` 좌표를 원본 1px=0.5 GUI(GUI2의 물리 1px)로 그린다. 배경 `GFX_topbar_background_extended`(`gfx/interface/topbar/background_extended.dds`, 2346×87)는 원본 x=-4·y=-1처럼 4열·1행부터 화면 왼쪽 끝을 채운다. 국기·단추 부분(1~82행, 41 GUI)과 원본 사선(680~728열) 뒤에는 위 띠(1~38행)만 화면 끝까지 잇고, 화면이 그림의 띠(1612열)보다 넓을 때만 띠를 늘린다. HUD 높이는 그림의 그림자가 끝나는 82px인 41 GUI다. 원본 정치·연구·교리 창(`show_position` y 78)은 상단 바 아래에서 시작해 보이는 위쪽 끝이 이 그림자 아래이므로, HUD 아래 화면은 41 GUI부터 그린다. 국기(`GFX_shield_medium`)는 원본 (1,6) 98×67이며 HUD에서는 별도 금속 바탕 없이 상단 바 그림 위에 그린다. `사건과 결정`~`장교단` 단추는 `country_menu_window`(217,36)·`after_intel_agency_button`(+56) 좌표로 계산한 x 109·164·220·275·331·387·442·497·553, y 35, 55×41(원본 캡처와 일치)이며 열린 메뉴는 각 단추 그림의 둘째 프레임(`menu/<id>_selected`)으로 표시한다. 정치력부터 부채까지 수치 칸은 원본 위치처럼 y 2 GUI에서 `GFX_generic_box_smallest`(26px=13 GUI)를 9-slice로 칸 폭에 맞춘다. 단추와 DEFCON 판·플레이리스트 단추가 겹치지 않도록 GUI 폭 363.5 미만에서만 HUD 그림 전체를 같은 비율로 줄인다.
- 결정 단추의 숫자는 원본 `decisionview_amount_to_take_bg`(`GFX_topbar_alert_bg` 첫 프레임)이며 `TOPBAR_DECISIONS_CAN_TAKE_DESC`("내릴 수 있는 결정")의 수다. 서버 HUD의 `availableDecisions`를 결정 화면의 `실행 가능` 행과 같은 계산으로 받고, 0이거나 값이 없으면 그리지 않는다. 원본 17px 원에는 최소 크기의 Minecraft 글꼴 두 자리가 들어가지 않아 MIO 대기열 배지와 같은 13 GUI 원으로 그리고 원본 배지의 왼쪽 아래 모서리(57, 36 GUI)에 맞춘다. 시간제한 배지(`decisionview_amount_timeout_bg`)·알림 반짝임(`GFX_decisions_glow`)·단추 툴팁의 결정 목록은 구현하지 않았다. `HudTooltipTest`는 2560×1440 GUI2에서 결정 수 18이면 배지 영역에 흰 숫자 픽셀이 있고 0이면 없는지 검사한다.
- 세계 긴장도는 원본 `GFX_armyoverview_buttons_bg`(403×101)의 DEFCON 구역(289~362열, 3~96행)을 금속 배경으로 화면 오른쪽 끝에 붙여 그린다. 원본에서 그 오른쪽에 있는 둥근 단추 홈 세 개(메뉴·도움말·숨긴 알림)는 클라이언트 기능이 없어 제외했다. DEFCON 그림(44×34)은 원본 `threat_button` 위치(판 기준 16, 25)에, 백분율은 판 아래 알약 모양 칸 가운데에 그린다. 툴팁 본문은 서버 원문 `THREAT_TOOLTIP`("세계 긴장도\n현재 값")으로 시작하므로 클라이언트 제목을 따로 붙이지 않는다(v47, 제목이 두 번 나오던 문제). 서버 원문의 첫 줄이 HUD 항목 이름과 같으면 제목을 생략하는 규칙(`HudTooltip.sourceTitled`, GDP·부채는 항상 생략)이며 `HudTooltipTest`가 검사한다. 국기 단추 툴팁은 국가명 다음 줄에 본편 공식 한국어 `TOPBAR_FLAG_DESC`(클릭 정치·우클릭 자국 외교)를 표시하고, 원작의 구분선과 `단축키: [Q]` 줄은 사용자 지시로 넣지 않는다. 우클릭은 자국 외교 화면(`DiplomacyCountryScreen`)을 열고, 자국 외교 화면이 열려 있으면 닫는다. GDP·부채는 십억 달러 값이 1조 미만이면 억 단위(`7,043억`), 이상이면 조 단위(`1.707조`)로 표시한다.
- 모든 툴팁은 현재 마우스 좌표를 따라가며 화면 끝에서 배경까지 안쪽으로 보정합니다. 최초 호버 위치나 버튼 아래 고정 위치를 저장하지 않습니다. 긴 툴팁은 같은 항목을 가리키는 동안 스크롤 위치를 보존하며 커서를 따라갑니다. 지도 카드·임무 막대를 피하기 위한 고정 높이 이동은 사용하지 않습니다. v47에서 마우스를 따르지 않던 툴팁(연구 설계국 로고·설계국 선택 행, 교리 세부 단추, 사단 편제 칸, 첩보 지도 아래쪽 항목, 군수산업체 특성)을 모두 마우스 좌표로 바꿨고, 위쪽 고정 그리기 함수는 없앴습니다. 마인크래프트는 단추를 누른 뒤 키보드를 쓰면 포커스된 단추 툴팁을 마우스가 올라가 있지 않아도 표시하므로, HOI 화면에서는 `GuiWidgetTooltip` Mixin이 포커스 표시를 끄고 마우스가 올라간 위젯만 툴팁을 띄웁니다. 함대 카드는 육군과 같이 호버만으로 선택 테두리를 표시하지 않고 클릭으로 선택된 카드에만 노란 테두리를 표시합니다.

결정 범주 배너의 원문 패딩은 일반 빈 줄 축소 전에 해석한다. 그림 앞뒤의 높이 예약 줄은 배너 배치에만 사용하고 일반 본문 가운데 연속 빈 줄은 한 줄로 그린다. 음수 좌표 패널의 끝 예약 줄도 보존한다. `DecisionScreenSourceTest`의 방산수출 배너 위치 검사는 범주 머리 바로 아래 배치를 두 해상도에서 확인하며 `-PhoiTestSuite=decisions`로 재현한다.

## 지휘관 임명과 상세 화면

능력 ID는 원본 대소문자를 보존한다. PRC_force_attack의 그림은 원본 GFX_ability_chi_force_attack, super_force_attack의 그림은 GFX_onmap_unit_counter_active_ability이며 팩의 소문자 경로에 따로 연결한다. 일반 능력 이름으로 그림 키를 추정해 이 두 원본을 기본 그림으로 바꾸지 않는다. 서버의 빈 이벤트 제목·본문·선택 문구는 빈 상태로 표시하고 번역 키나 대체 문구를 만들지 않는다.

`advisor-politics`는 `HOI_COMMANDER_PREVIEW`의 `kor-advisor-roles-{before,after}-promote`, `kor-politics-staff-{before,after}-promote`, `kor-army-roster-tooltips`, `ins-menu-open-screen`을 사용한다. 정치 후보의 추가·유효 선택지·반복 클릭 잠금, 두 방향 고문 정렬, 원문 국민정신 툴팁을 GUI2 두 해상도로 검사한다. 시험 입력은 `InputConstants.MOUSE_BUTTON_LEFT`를 사용한다. SDL 입력의 버튼 번호를 GLFW 숫자로 가정하지 않는다.

그림 없는 국민정신 툴팁은 기존 `sourceLines`로 원본 `§G`·`§!` 색 코드와 필드별 소수 자릿수를 보존한다. 원문에 없는 아이콘을 추정해 끼우지 않는다. 정치의 다른 항목은 기존 표시 경로를 유지한다.

지휘관 목록은 `Leader.skillTooltip`·`capacityTooltip`·`assignTooltip`을 각각 큰 기술 숫자·수용량·행의 빈 영역에 표시하고, 이름·초상화에는 기존 `Tile.detail`을 표시한다. 기술·수용량·배정 안내를 클라이언트가 재계산하거나 빈 문구를 채우지 않는다. Shift+좌클릭은 서버가 `deploy/<id>` 선택지를 발급한 경우에만 사령부 배치 요청을 보내며, 없으면 일반 배정으로 처리한다. 특성·고문·진급 단추는 행 선택보다 우선하고 한 클릭으로 두 요청을 보내지 않는다. TFR `unitleaderwindow.gui`의 `sort_skills`·`sort_advisor`는 정렬 단추다. 고문 정렬 단추는 원본 GFX_sort_adviser 첫 프레임과 click_scroll(ui.scroll)을 쓴다. 첫 클릭은 서버 Leader.advisorState 2(기존 역할) → 1(추가 가능) → 0(없음), 동률이면 기술 내림차순이며 두 번째 클릭은 전체 역순이다. 이 정렬에서는 장군/원수 우선 정렬을 덧붙이지 않는다. 순서는 원본 엔진을 확인하지 못해 정한 서버 계약이며 원본 동일성을 주장하지 않는다.

고문 추가·임명과 장군 진급은 `CommanderConfirmation`의 공통 확인창을 사용한다. TFR `popupwindow.gui`의 `default_confirmation_popup`(480×322, 제목 10/16, 본문 24/90, 취소 90/250, OK 260/250), `popupwindow.gfx`의 테두리 180×85를 GUI2 기준으로 적용한다. 기존 외부 팩의 `mio/ui/gfx_tiled_window_pol_goal/frame_0`와 `commander/commands/button_148x34/frame_0`가 각각 원본 배경·단추다. 긴 한국어 본문에는 창 높이를 늘리고 화면을 넘으면 본문만 스크롤한다. 원작 장군 진급 사진은 `20261024073659_1.jpg`·`20261119042832_1.jpg`, 야전 장교 진급은 `20261119042841_1.jpg`, 고문 추가는 `20261119043913_1.jpg`다. 창은 240×161 GUI, 본문 폭 215, 취소·OK 단추 74×17(x45·x130, y125)이고 단추 글자는 원본처럼 일반 굵기다. 제독의 장교단 역할 임명도 같은 창과 원문을 쓰며, 국가 선호 전술 변경에는 원본 재확인 창(관련 원문 키)이 없어 추가하지 않는다. 이름·역할·비용·색·빈 줄은 서버의 `UNIT_LEADER_ADD_ADVISOR_TITLE`·`UNIT_LEADER_ADD_ADVISOR_DESC`·`UNIT_LEADER_PROMOTE_POPUP_DESC` 결과를 사용한다. 발급된 확인·취소만 허용하며 요청 뒤에는 새 revision까지 잠근다. 비용 부족으로 확인 선택지가 없으면 확인 단추도 만들지 않는다.

`HOI_COMMANDER_PREVIEW`를 검증된 서버 fixture 폴더로 설정하고 Java25에서 `test runGameTest remapJar -PhoiTestSuite=commander-contract -PhoiServerSource=<고정 서버> -PhoiProtocolSource=<고정 서버>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`으로 재현한다. `kor-army-roster-tooltips`, `kor-advisor-promote-confirm`, `kor-promote-confirm`, `kor-command-panel-hq-deploying`을 사용한다. GUI2 2560×1440·1280×720에서 실제 호버와 렌더, 화면 MouseButtonEvent, Shift 배치/일반 배정 전환, 비용 부족·취소·반복 클릭·같은 revision 재응답을 검사한다. 사령부의 `DEPLOYING` 라벨·text·남은 일수 툴팁도 서버 상태를 따른다. 요청 전송은 모의 수집이므로 실제 서버 비용 차감·저장·정치 후보 추가의 종단간 검증과 구분한다. 기존 목록·역할·특성 회귀는 `commander-layout`으로 검사한다. 추가 입력의 고문 역할 전후·정치 후보 4명→5명(안병석 추가)·정렬 양방향·인도네시아 국민정신은 advisor-politics 집중 검사로 확인한다. INS 입력은 OpenScreen wrapper를 포함한다. 원본 한글패치 TFR_country_localisation_INS_l_korean.yml:4의 INS_jungle_warfare_tt 두 줄(정글 10.0%, 산악 10.8%)을 그대로 표시한다. 산악 문구 10.8%와 실제 기술 계산 10%의 차이는 원본 자료에 있으며 화면에서 수정하지 않는다. 실제 고문 비용 차감·저장·멀티플레이·직접 음향 청취와 전체 무필터 검사는 별도다.

연속 빈 줄은 `TooltipText.collapseBlankLines`를 공통으로 사용한다. 문자열 원문, 스타일이 나뉜 Component, HUD·정치·중점처럼 제목과 본문을 합친 최종 줄 목록에도 적용하며 글자·색·수치·아이콘 순서를 바꾸지 않는다. 줄 수를 사용하는 본문 높이와 스크롤 계산도 같은 정규화 경로를 따른다. `test runGameTest remapJar -PhoiTestSuite=tooltip-spacing`는 `HOI_COMMANDER_PREVIEW`의 `kor-navy-assessment.json`, `kor-diplomacy-PRK.json`, `kor-navy-trait-tree.json`을 읽어 기뢰 부설·독립 보장·연안 싸움꾼을 GUI2 2560×1440·1280×720에서 실제 호버 렌더한다. 줄 축소 전후의 보이는 글자와 스타일 순서를 검사하고 앞뒤 여백·서식 조각 경계는 별도 회귀로 확인한다. 이 입력 검사는 운영 접속·다중 사용자·직접 효과음 청취 검증을 대신하지 않는다.

임명 행과 특성·장교단 역할 머리칸의 이름·직책은 최종 글자 조각마다 볼드를 적용한다. 원문 색상 코드나 자식 스타일이 굵기를 해제하지 않는다. 제독 수용량은 원본처럼 숫자만 캡슐 가운데에 표시하고, 장군·원수 수용량은 메달을 유지한다. 임명 행의 `commander/traits/trait_add_new`는 서버 `traitDetails` 마지막 그림으로 기존 겹침·툴팁·특성 창 요청을 그대로 사용하며 보유 특성 수에 더하지 않는다. 역할 창의 첫 탭 문구는 서버 `traits` 선택지 라벨을 그대로 쓴다.

볼드 래스터 대조는 `commander-layout`의 `hoi-header-bold-comparison-synthetic-*` 캡처로 같은 한글의 보통체·볼드체를 흰색·검정에서 비교한다. Minecraft 기본 Unihex 볼드는 오른쪽 0.5GUI 오프셋을 더하므로 GUI2에서 세로획이 1픽셀 넓어지고 가로획 높이는 유지된다. 실제 임명 행·특성 머리칸의 글자와 이 진단의 볼드 픽셀을 대조하며 스타일 플래그 검사와 실제 획 변화를 구분한다.

역할 목록의 `trait_adviser_disabled`·`trait_adviser_picked`에는 각각 회색·크림색 서류 한 장이 이미 포함되어 있어 빈 서류를 덧그리지 않는다. `owned=true`는 인물 원본에 정의된 고유 역할로, 고용 전후 모두 파란 배경과 크림색 서류를 쓴다. 나머지 역할은 회색 서류다. 고용 여부와 고문 추가 가능 여부는 해당 section에 발급된 선택지의 라벨로 구분한다. 클릭 권한은 발급된 선택지만 따른다. 왼쪽 보유 목록은 선호 전술 상자 위에서 자르고 온전한 글자 줄만 표시한다. 넘치면 스크롤 표시와 휠로 마지막 특성 선택 줄까지 볼 수 있으며 글자는 1배를 유지한다. `commander-layout`의 렌더 스타일 검사와 `hoi-commander-advisor-left-scrolled-*` 캡처로 재현한다. `CommanderRosterTest`는 실제 서버 입력의 특성 추가 그림·원문 툴팁·인증된 특성 요청과 중복 클릭 차단을 확인한다. 특성 추가 그림은 서버가 남은 슬롯과 선택 가능한 특성을 모두 확인한 경우에만 보내며 클라이언트는 이를 재계산하지 않는다.

군·집단군·함대의 임명 목록은 서버 `Leader`와 발급된 선택지를 사용한다. 장군의 `promotion` 문구가 있으면 진급 단추를 표시하고 `promote/<id>`가 있을 때만 활성화한다. 원수·제독에는 진급 단추를 만들지 않는다. `traits/<id>`는 세부 사항 창을 열며, 행 선택과 단추 입력을 구분해 한 번의 클릭으로 두 요청을 보내지 않는다. 진급 확인창은 서버 본문·비용과 진급·취소 선택지를 공통 확인창에 표시한다. 원본 `20261024073659_1.jpg`처럼 목록 행이나 초상화를 본문에 덧붙이지 않는다. 임명 목록의 스크롤 막대는 끌어서 이동할 수 있으며 모든 행이 읽히도록 행 단위로 이동한다. 야전 장교 보기·함선 지휘관 보기는 원본 하단 위치에 표시하고 18나·다 서버 계약 전에는 비활성화한다. 배정 해제는 지휘관이 없어도 원본처럼 밝은 단추로 남기며 실제 해제 요청은 서버가 발급한 선택지가 있을 때만 전송한다. 하단 단추는 목록 오른쪽 끝에 맞추고 아래 테두리와 간격을 둔다. 임명된 지휘관 체크를 끄면 검색 여부와 무관하게 Leader.assigned 행을 숨긴다. 진급 확인창은 기존 목록 위에 뜨며 취소 뒤 원래 화면 계층을 복원한다. 신규 지휘관은 서버 기능이 없으면 표시하지 않는다.

육군 상세의 비행단 단추는 배속 유무와 관계없이 표시한다. 기본 지도 군 카드 위의 배속 비행단 띠는 숨기고 공군 지도에서는 군별 원본 배속 칸을 표시한다. 사단을 고르고 + 군 카드를 누르면 보낸 사단 ID 집합과 기존 군 카드 ID를 기억하고, 다음 스냅숏에서 그 사단들로 새로 만들어진 군 하나가 확인될 때 일반 군 상세를 선택한다. 실패·불일치·국가/군종 변경에는 군을 추정하지 않는다.

사령부 단추는 `army_hq_bg`의 오른쪽 밝은 테두리를 제외한 어두운 무늬 영역을 기준으로 정렬한다. 내부 투구·문구·지휘력 아이콘·숫자는 원본 이미지의 투명 여백과 실제 글자 폭을 반영해 오른쪽 칸 가운데에 둔다. 배치 배경과 클릭 영역에는 사용자 지정 +2GUI 세로 보정을 적용한다. 비행단 단추는 옆 철도포 단추와 같은 원본 44×23 비율이다. 무지휘관 안내 문구는 TFR `unitview.gui`의 `click_to_assign`에 존재하며 현재 글자 크기에서는 네 줄이 초상화 밖으로 넘친다. 글자 크기 변경·문구 생략은 이 정렬 변경에 포함하지 않는다.

특성 창은 육군·제독 모두 임무 막대와 스킬 칸보다 앞에 그리며 겹친 배경 입력을 차단한다. 특성·장교단 역할 탭, 능력치·특성 이름, 일반 특성과 지형 특성의 독립 스크롤은 서버 자료를 따른다. 사단 행 툴팁은 `ARMY_LIST_CLICK_SELECT_DIVISION`, `ARMY_LIST_RIGHT_CLICK_PAN`, `ARMY_LIST_DOUBLE_CLICK_TEMPLATE`, `ARMY_LIST_CTRL_CLICK_TOGGLE` 순서의 클릭 안내 → 빈 줄 → 사단 이름 → 서버 본문 순서이며 별도 `status` 줄을 붙이지 않는다. 조직력 토큰은 기존 `organization_texticon` 글리프로 표시한다. 함대·비행단 행의 본문은 원본 스크린샷 근거가 없어 유지하고, 모든 공용 툴팁의 대시만 있는 줄은 빈 줄로 바꾼다.

특성 창의 스크롤 범위는 실제 특성·역할 행과 보유 특성 본문의 끝을 기준으로 정한다. 육군 상세의 스크롤 사례를 따라 임의의 빈 영역이나 추가 이동 거리를 만들지 않는다. 특성 툴팁이 길어져도 트리와 보유 목록의 스크롤 범위를 늘리지 않는다.

특성 창의 `leader` 타일에 `gauge`가 있으면 서버 `fraction`만큼 경험치 막대를 채우고 `tooltip` 원문을 표시한다. 원본 `GFX_leader_exp_bg`는 비율 0에서도 표시하며 투명한 진행 막대 배경 텍스처와 구분한다. `gauge`가 없으면 배경과 미확인 안내만 표시하며 레벨·진급 경험치를 클라이언트에서 계산하지 않는다. 제독의 뇌격 전문가까지 스크롤 없이 표시하고, 왼쪽 특성 선택 안내·선호 전술 아이콘과 요구 기술을 유지한다. 위 머리칸의 이름·직책·기술·능력치는 흰색이고 기술 숫자는 크게 굵게 표시한다. 밝은 왼쪽 칸의 절 제목·특성 이름은 굵은 검정, 효과 라벨·값은 검정이며 내용 없는 절은 숨긴다. 특성 선택 안내는 흐린 금색이다. 선호 전술 제목은 상자 안 윗띠의 흰색이며 요구 기술 숫자는 원본 노랑을 유지한다. 절 제목 양옆은 원본 `trait_separator`를 사용하고 연결선은 회백색이다. 창 내부의 불투명 바탕은 뒤의 HUD 글자도 가린다.

보유 특성의 효과는 이름 바로 다음 줄 또는 빈 줄 뒤에서 시작할 수 있다. 두 형식 모두 서버가 준 효과를 표시하고 역할 해금·설명·구매 조건을 효과 목록에 섞지 않는다. 원본 화면에만 있는 수치는 추가하지 않는다. 특성·역할 창은 GUI 단위 671.5×391.5를 기준으로 화면에 들어가지 않는 만큼만 줄인다. 글자는 1배 미만으로 줄이지 않고 긴 이름은 가로 스크롤하며 전체 내용 툴팁을 유지한다. 왼쪽 내용의 줄 간격은 글자 높이를 보존하며 넘치는 목록은 스크롤한다. 1280×720 GUI2에서도 플레이어 설정을 바꾸지 않는다.

임명 목록의 `Leader.advisor`는 장교단 역할 단추의 원문 툴팁이고 `advisor/<id>` 선택지가 있어야 요청을 보낸다. 비활성 단추도 원본 색으로 그려 호버할 수 있으며 클릭을 소비해 뒤의 지휘관을 임명하지 않는다. 15판 원본 대조 지시를 따라 제목줄의 임명된 지휘관 보기 문구·원본 GFX_generic_checkbox를 표시한다. 기본값은 체크이며 해제하면 Leader.assigned 후보만 로컬 목록에서 숨긴다. 이 필터는 서버 배정을 변경하지 않는다. 집단군 기본 정렬은 기술 내림차순, 같은 기술이면 원수 우선이며 동순위는 서버 순서를 유지한다. 이름으로 재정렬하지 않는다. 기술 숫자는 굵은 흰색이고 수용량 옆에는 원본 `GFX_medal_collection`을 그린다. 세부 사항·진급은 같은 폭에 기존 글자 크기를 유지하며 집단군의 장군 진급은 적색 바탕이다. 군·집단군·함대 초상화 아래의 특성/고문 단추는 서버 `LEADER_TRAITS`·`LEADER_ADVISOR` controls의 그림·활성·툴팁·요청을 사용한다. 선택 가능한 특성의 + 그림을 클라이언트가 추측하지 않는다.

`Leader.advisorIcon`이 비어 있으면 모자 단추를, 값이 있으면 원본 서류 바탕 위에 해당 역할 칸을 표시한다. 임명 목록의 서류 위치는 `unitleaderwindow.gui` x3/y45와 중심 x28/y70의 절반 배율을 따른다. 카드·상세 패널의 `LEADER_ADVISOR.icon`이 `commander/commands/idea_traits_strip/`로 시작할 때도 같은 합성 그리기를 사용한다. 그림이 있어도 발급된 선택지·control의 활성 상태를 확인하며, 비활성 고문 단추 클릭이 뒤의 임명 행으로 넘어가지 않게 한다.

고문 임명 상태의 서류 그림을 검증하려면 실제 서버 응답에 비어 있지 않은 `Leader.advisorIcon` 또는 `LEADER_ADVISOR`의 `idea_traits_strip` 그림이 있어야 한다. 역할 목록의 `/dismiss`만으로 해당 그림 경로가 검증되지는 않는다. 실제 서류 상태는 `kor-army-appointment-advisors.json`과 `kor-command-panel-army-advisor.json`으로 임명 목록·카드·상세 패널의 그림과 입력을 검증한다.

장교단 역할은 특성 창 틀을 재사용하고 서버 `leader` 타일의 초상화·기술·능력치·경험치와 역할 목록을 표시한다. 보유 `traitNode.kind`의 `advisor`는 왼쪽 맨 위 장교단 절, `personality_trait`·`status_trait`는 뒷배경 절, 나머지는 특성 절이다. 이름으로 분류를 추정하거나 역할을 특성으로 만들지 않는다. 계약 14의 `advisor_role`은 `column` 0/1에 따라 최고사령부/참모총장 열, `row`에 따라 열 안 순서를 정하며 `owned`는 파란 타일을 선택한다. 이름의 원본 줄바꿈과 빈 서류 그림을 유지하고 선택지는 해당 `role/<종류>/<묶음>` section과 정확히 일치해야 한다. 이전 계약의 슬롯별 역할 응답은 `/dismiss`로 임명 상태를 표시한다. `traits`·`preferred-tactic`은 서버가 발급한 선택지만 보낸다. 특성 창의 두 탭은 항상 보이며 역할 선택지가 없으면 요청을 보내지 않는다. 원본 인물 자료가 없어 `leader`·보유 특성·선호 전술이 빠진 역할 응답은 해당 상세 영역 없이 목록을 표시한다. 확인창은 기존 `COMMANDER_ADVISOR_CONFIRM` 화면을 유지한다. 역할 상태·비용·선택지는 서버 응답만 사용하며 지휘관을 새 고문 후보로 승격시키는 동작은 추가하지 않는다.

역할 열과 왼쪽 장교단 절은 `advisor_role`·`advisor` 서버 타일로 표시한다. 고유 역할의 `owned`는 임명·해임 전후에 같으므로 고용 여부나 클릭 가능 여부로 해석하지 않는다. 클릭 가능 여부는 발급된 선택지만으로 결정한다. 전동진의 `kor-jeon-advisor-roles.json`은 21개 역할을 보여 주되 역할 선택지를 만들지 않는다. 임명·해임·특성 탭·선호 전술·인물 자료 누락 합성 응답은 `commander-layout`과 `commander-traits`로 재현한다. 육군 21개·해군 13개 역할, 장교단 절과 실제 서류 상태도 같은 묶음에서 검사한다. 경험치 비율 0은 원본 바탕과 서버의 현재 진행도 툴팁으로 확인하며, 값이 없는 응답과 구분한다.

임명 행은 TFR `unitleaderwindow.gui`의 510×98 배경을 255×49GUI로 표시한다. 배경 기준 초상화 (28, 2.5), 직책 (64, 6), 이름 (64, 15), 특성 (64, 28), 세부 사항 (159, 28), 진급 (207, 28)을 따른다. 정수 클릭 영역의 반올림 오차는 0.5GUI 이내다. 능력치는 원본 그림과 바로 아래 숫자를 사용하며 수용량 숫자는 메달 왼쪽에 붙인다. 점선은 원본 행 배경에 포함되어 있다. 글자는 1배 미만으로 줄이지 않으며 긴 이름은 가로 스크롤하고 행 툴팁에 전체 이름을 유지한다. 특성은 이름 시작점부터 세부 사항 단추까지의 폭에서 `CountryFlagStack`으로 겹치며 맨 위에 보이는 아이콘이 호버를 받는다. 실제 최다 특성 인물과 12개로 늘린 명시적 합성 입력을 각각 검사한다.

특성·역할 머리칸은 `armytraittreewindow.gui`·`navytraittreewindow.gui`의 좌표를 따른다. 초상화는 (5.5, 32.5)에서 156×210의 0.195배, 테두리는 (2.25, 28.5)에서 71×104의 0.5배다. 기술 배경은 (40, 36.5)의 35×50 0.6배이며 숫자는 중심 x50.5에 놓는다. 경험치 바탕은 (42, 74)의 171×11 0.5배로 소수 크기를 보존한다. 1280에서는 창의 필요 축소만 적용하며 글자는 1배를 유지한다. 비율 0은 회색 바탕으로 보이고 채움은 서버 값이 양수일 때만 나타난다. 메달 리본은 설치 원본의 단일 프레임과 이전 스크린샷의 색 차이가 남아 있으며 임의로 재색칠하지 않는다.

함대·비행단 행 툴팁은 노란 이름 한 줄 다음에 서버 `Member.detail`을 그대로 붙인다. 별도 상태·수량·이름 줄을 반복하지 않는다. 특성·경험치·능력치·선호 전술을 포함한 공용 스크롤 툴팁의 모든 줄은 왼쪽 정렬하며 원문 색·빈줄·수치는 유지한다.

외교 행동은 수신한 한 목록을 원본 행 간격으로 표시하고 넘치면 스크롤한다. 클라이언트가 다음 페이지를 만들어 끼워 넣지 않는다. 서버가 62개 초과 목록에 명시적으로 발급한 페이지 선택지는 유지한다.

집중 재현은 팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=commander-controls`, `-PhoiTestSuite=headquarters-layout`, `-PhoiTestSuite=commander-layout`, `-PhoiTestSuite=country-assets`, `-PhoiTestSuite=diplomacy`를 사용한다. 지휘관 묶음에는 검증된 서버의 `HOI_COMMANDER_PREVIEW`를 지정하며 진급 확인 입력 `kor-promote-confirm.json`이 필요하다. 실제 제독 임명은 `kor-navy-admirals-source.json`으로 목록을 촬영한 뒤 검색으로 후보를 찾아 입력을 검증한다. 배정된 제독 카드 단추 검사는 `kor-command-panel-navy-ten-forces.json`을 사용한다. `kor-command-panel-navy.json`은 시작 상태의 무지휘관 함대이므로 배정 상태 검사를 대신하지 않는다. 실제 한국→북한 외교는 `kor-diplomacy-PRK.json`으로 국민정신 네 개와 `market_access`까지의 한 목록을 확인한다. 서버 생산 입력을 생성하는 묶음에는 같은 사본의 `-PhoiServerSource`와 `-PhoiProtocolSource`를 함께 지정한다. `country-assets`는 원본 그림 바인딩을 합성 정치 화면에서 확인하는 검사로 국가 효과 검증이 아니다. 실제 GUI2 2560×1440·1280×720의 렌더·입력 검사는 운영 서버 접속·다중 사용자 동기화·직접 효과음 청취를 대신하지 않는다. 필터 없는 전체 검증은 검토 이후 별도로 실행한다.

커서 추적과 카드 선택은 같은 고정 입력으로 `test runGameTest remapJar -PhoiTestSuite=cursor-tooltips`를 사용한다. 일반 위젯 툴팁의 양축 이동·화면 끝 보정, 긴 툴팁의 스크롤 유지, 같은 카드·특성 안에서 커서 이동, 육군·함대 호버/클릭 선택/선택 해제를 확인한다. `commander-traits`는 특성 창·구매/취소 및 GUI2 2560×1369를 포함한 HUD 겹침 입력을 따로 검사한다.

## 결정 문구의 국기

결정 이름·설명·분류 문구·결정 패널과 툴팁의 서버 발급 `[TAG.GetFlag]`를 작은 국기 그림으로 표시한다. 태그는 대문자 세 글자이며 `GetNameWithFlag`가 보내는 국기 바로 뒤 나라 이름도 같은 경로를 사용한다. 이벤트 문구는 이 변환을 사용하지 않는다. 문장의 글자는 Minecraft 기본 글꼴·기존 크기로 유지하고 국기의 줄바꿈·폭 계산에는 외부 팩의 `hoi:decision_flags` 비트맵 글리프를 유지하고 실제 그리기는 `CountryDisplay.Entry.flag`의 현재 이념/대체 국기 텍스처를 사용한다. 표시 자료나 해당 원본이 없으면 기존 태그 국기를 사용한다. 캐시된 문장도 그리는 시점에 현재 국가 표시 자료를 조회하므로 표 갱신·복원·리소스 재로드 뒤 이전 국기를 유지하지 않는다. 기존 이름칸의 중앙 정렬·폭 맞춤과 설명/툴팁의 줄바꿈·스크롤 규칙에 국기 폭도 포함하며 줄 높이는 늘리지 않는다. 국기는 흰색·그림자 없음으로 그려 원래 색을 유지하고 앞뒤 글자의 원본 색 코드·정밀도·공백·빈줄은 보존한다.

원본 HOI4 `common/defines/00_graphics.lua:944–945`의 `COUNTRY_FLAG_SMALL_TEX_WIDTH=10`, `COUNTRY_FLAG_SMALL_TEX_HEIGHT=7`과 TFR `common/defines/TFR_00_magic.lua:3`의 작은 국기 스트립 폭 10을 근거로 기존 팩의 `textures/gui/unit_hud/flags/<tag>.png`(10×7)를 재사용한다. 입력 경로는 TFR `gfx/flags/small/<TAG>.tga`이며 이미지 해시는 HOI-resourcepack `provenance.json`에 있다. KOR·PRK는 설치된 TFRKP에 동명 작은 국기 덮어쓰기가 없으므로 TFR 원본이다. 비트맵 목록은 `scripts/build_decision_flags.py`가 검증된 기존 그림 586개에서 생성하며 새 그림을 복제하거나 JAR에 넣지 않는다.

국기가 든 원작 결정 줄의 지정 스크린샷은 아직 없다. `GetFlag`의 엔진 내부 문구 배율·간격·기준선은 위 정의로 확인되지 않으므로 **10×7 GUI 크기, 9픽셀 글자 줄 안의 y+1 배치, 뒤 1GUI advance 간격은 가정**이다. GUI2의 폭 맞춤 없는 국기는 화면 20×14픽셀이다. 원문 공백이 있으면 그 공백도 유지한다. 국기가 없거나 국기 manifest가 없는 경우 `[FRS.GetFlag]`·`[ZZX.GetFlag]`처럼 토큰 글자를 그대로 표시하며 다른 국기를 대신 쓰거나 빈칸으로 숨기지 않는다. FRS는 현재 작은 국기 목록에도 없으며 서버의 이름 함수가 미확정인 원본 항목과 구분한다. 리소스 재로드 때 바인딩과 문구 캐시를 다시 읽는다. 현재 공개 `CountryDisplay.Entry.flag`에 발급된 키가 있으면 그 텍스처를 사용한다. 기본 키·자료 누락·없는 텍스처는 기존 작은 비트맵 글리프를 그대로 쓰며 이념/대체 접미사를 추정하지 않는다. 동적 텍스처도 기존 10×7 크기와 y+1 위치·글리프 advance를 유지한다.

집중 재현은 `test runGameTest remapJar -PhoiTestSuite=decisions`에 검증된 서버 소스/프로토콜 사본을 지정한다. 서버 `DecisionPresentationTest`의 `kor-kfxx`·`prc-eadi` 입력도 provenance 검증 대상으로 포함한다. 실제 클라이언트 렌더러로 GUI2 2560×1440·1280×720 창과 국기 줄·툴팁을 캡처하고 국기 폭 11(10+advance), 연속 색 코드, 빈줄, 줄바꿈, 미등록 국기 표시를 검사한다. 기존 결정 검사는 권한 회수·반복 입력 잠금·분류 접기·스크롤·패널 입력을 함께 실행한다. 이 검사는 운영 접속·멀티플레이나 원작 `GetFlag`의 픽셀 단위 일치 검증을 뜻하지 않는다.

`python scripts/compare_decision_flags.py --run <runDirectory> --baseline <이전-전체-runDirectory> --output <증거-폴더>`는 네 창·국기 줄의 3배 최근접 crop, 전체 캡처의 이름/발생 순서별 변경·추가·누락 해시, JUnit/producer 수와 JAR/팩 해시를 기록한다. 2560×1440의 한국·중국 국기 줄은 기존 원본 PNG를 최근접 2배 확대한 20×14픽셀과 실제 렌더 픽셀이 같은지 검사한다. 1280×720의 긴 이름은 기존 폭 맞춤 규칙을 따르므로 작은 국기도 이름과 함께 줄어들며, 그 결과를 20×14 검증과 혼동하지 않는다. 이벤트 창·툴팁 측정값도 이전 전체 실행과 별도로 비교하며, 동적인 배경을 포함한 전체 PNG 해시 차이를 해당 창의 UI 변경으로 단정하지 않는다.

사본-8의 실행용 복사본으로 결정 집중 검증은 337.79초, 필터 없는 전체 `test runGameTest remapJar`는 1336.24초에 통과했다. 클라이언트 JUnit 111개는 집중 실행에서 재실행했고 프로토콜 115개는 기존 성공 결과를 재사용했다. 전체는 두 JUnit 결과를 재사용하고 producer 45개와 실제 전체 렌더·입력 검사를 실행했다. 실패·오류·건너뜀은 0이며 자동 입력 82개·공유 프로토콜 소스 46개를 확인했다. 이전 CMP-15의 성공한 수동 입력 53/17/3/1개는 소스·콘텐츠 1177개 해시가 사본-8과 같아 재사용했다. Python 작은 국기 검사 1개도 통과했다.

전체 PNG는 1860장으로 CMP-15 마지막 전체 1838장 대비 원시 해시 변경 228장·국기 fixture 추가 22장·누락 0장이다. 비교 전 CMP-15 원본 1838장 해시를 확인했다. 이벤트 창·툴팁 측정값은 CMP-15와 같으며 한국·중국 큰 창의 국기 픽셀은 원본과 완전히 같다. 네 국기 창과 확대 crop을 직접 열람했다. 동적 배경을 포함하는 변경 228장 전체의 UI 동등성을 직접 확인한 결과는 아니다. 변경·추가 목록과 재현 명령·시간·출처·JAR/팩 해시는 CMP-16 첨부 `render-verification.json`에 남긴다. 기본 팩은 98510225바이트(SHA-1 `db57f506fcf852afa3bd1c2cd88abe7519f636ee`), UI 팩은 255528651바이트(SHA-1 `21516a985ae33cc1e2b658f2ae20f73a1e52264a`)로 각각 3254/4203바이트 늘었으며 나머지 네 팩은 같다. 모든 manifest 팩을 함께 적용한다. 운영 멀티플레이·직접 효과음 청취·원작 국기 줄의 배율/간격 일치는 미수행이다.

## 국가 메뉴와 HUD

정치·연구 패널 제목 줄에는 날짜를 표시하지 않는다(날짜는 HUD에만 둔다). 메뉴의 보이지 않는 클릭 영역은 자기 이름(`이름 · 값`)을 툴팁으로 띄우지 않고, 원본 툴팁이 있는 항목만 툴팁을 보인다. `세부 사항` 단추는 닫기 단추처럼 "세부 사항" 툴팁을 띄우며, 행 전체 툴팁보다 앞에 그린다.

메뉴 순서는 국가 정보·결정·정보기관·연구·무역과 경제·건설·생산·모병 및 배치·군수·장교단입니다. 군종 경험치는 육군·해군·공군 순서입니다. 날짜·배속은 서버의 게임 시간을 한국어로 표시합니다.

정치 화면 위쪽은 TFR `countrypoliticsview`의 `GFX_pol_view_bg_new`(팩 `politics/view_background`, 719×394)를 창 (2,40)에 원본 그대로 깔고, 각 내용을 원본 좌표에 둡니다. 좌표는 726폭 원본 창(원점 −5,78) 기준이며 `PoliticsLayout`이 창 원점과 배율을 함께 적용합니다: 지도자 틀 (6,49)·사진 (13,55) 156×210·이름 (15,275) 155×20 가운데, 국가 중점 배경 (187,52) 359×107·선택 단추 (280,64) 258×83·중점 아이콘 가운데 (237,105) 원크기·제목 (307,92) 200 금색·진행 막대 (302,122) 212×6(배경 `politics/focus_progress_bg`), 국제기구 (573,51) 121×114, 이념 (193,174) 66×68, 하위 이념 이름 (192,264) 175폭, 선거 (192,300) 140폭, 국민정신 칸 (291+68j,170) 68×68과 아래 `bottom_horizontal_slider_2`(팩 `commander/commands/scroll_*_2`, y231~251), 정치 체제 (178,337)과 이름 (253,354) 125폭 가운데, 경제 모델 (15,322)과 이름 (9,398) 175폭, 세력 (105,325), 원형 그래프 덮개 (398,327) 90×87, 권력 균형 단추 (379,253) 129×57(팩 `politics/bop/button`), 점령지·협력정부 (315,398)·(350,398), 규칙 아이콘 `GFX_rules_sum_icon`(팩 `politics/ui/rules`) (625,7) 38×42. 정당 목록은 원본 `parties_grid`(450,251)의 행 구성(`GFX_pol_party_colour_bg` 21×21 (507,+2), 정당색 `GFX_pol_party_colour` 13×13 (511,+6), 지지율 1위 정당은 `leading_pol_party_bg` 196×15 (510,+5), 이름 (530) 180폭, 표기는 원작처럼 `이름 (52)`)을 따르되, MC 글꼴 높이 때문에 행 간격을 원본 15px 대신 16px로 두고 10행이 넘으면 휠과 오른쪽 스크롤바로 넘긴다. 법률·장관 영역은 y435부터 `GFX_tiled_window_2b_border` 위에 분류마다 원본 `GFX_category_header`(팩 `politics/category_header`, 660×52)를 (27,465+110i), 분류 아이콘을 (36,473+110i) 원크기, 이름을 (102,477+110i)에 그리고, 칸은 (104+80j,509+110i) 63×63으로 왼쪽부터 최대 7개 둡니다(원작 20261118205348에서 정부 초상·경제법 아이콘 x105~165, 간격 80·110). 국가 중점은 선택 단추 영역을 눌러 선택합니다. 정부·장관·군사참모는 소형 철제 프레임 초상화를 우선하며 원본이 없으면 빈 프레임을 사용합니다. 국민정신·지도자 설명은 서버가 제공한 현재 상태만 표시합니다. 정치 고문·장교·장관 선택 칸의 이름과 특성 줄은 칸 폭에 맞춰 줄이지 않고 최소 글자 크기(GUI2 2px)로 그리며, 칸보다 길면 `UiText.scrollingCell`로 좌우로 흐른다(예: 이낙연 고문 특성).

정치·장교단 칸의 후광은 서버 `MenuView.Entry.alert`(지금 바꿀 수 있는 칸)일 때만 그린다. 사용자 지시(2026-11-19)에 따라 애니메이션 없이 프레임 0만 쓰고, 원본 위치(정치 `political_idea_entry`의 x45/y-8, 장교단 x73/y92 등) 대신 칸 아이콘의 정중앙에 둔다. 정치 슬롯·장교단 참모 칸은 `GFX_ongoing_generic_glow_yellow`(141×115 한 프레임, 0.65배)를 원본 63px 칸 대비 크기로 아이콘 뒤에 깔고, 장교단 정신 칸은 `GFX_spirit_glow`(159×115)를 원본처럼 정신 배경(168×70) 위·훈장 아래에 같은 비율로 그린다. 후광 뒤 칸 순서는 이웃 아이콘을 덮지 않도록 같은 줄의 후광을 먼저 모두 그린 다음 아이콘을 그린다. `ScreenAlertGlowTest`(`politics` 묶음)는 GUI2 2560에서 켠 칸의 밝아진 픽셀이 칸 중앙에 대칭인지(정신 칸은 가로만), 끈 칸에 변화가 없는지 검사한다.

정치 화면 정당 원형 그래프는 TFR `political_pie_chart`의 `GFX_political_chart`(반지름39)와 `GFX_pol_piechart_overlay_63x63`(TFR 90×87, 팩 `politics/pie_overlay`)을 쓴다. 덮개를 칸 폭에 맞춰 세로 가운데에 두고, 원판은 덮개 고리 중심(45,44)에 반지름39 비율로 그린다. 조각은 정당 목록 순서대로 3시에서 반시계 방향으로 누적한다(원본 `20261118205348_1.jpg`: 중국공산당 52%가 3시에서 위로 9시 너머까지, 이어 민혁·구삼학사·치공당·민맹·무소속·민건·인민해방군). 호버는 같은 중심·반지름·각도(`PoliticsPartyChart`)를 쓰며 외교 머리칸 원형 그래프와 각도 계산을 공유한다. `ScreenPoliticsTooltipsTest`는 각 정당 조각의 가운데 각도에서 호버가 그 정당을 고르는지와 덮개 크기를 검사한다.

장교단 정신 칸에서 채택한 정신 아이콘은 원본 `spirit_icon`처럼 원본 크기(대부분 45×45)를 정신 배경(168×70)과 같은 비율로 줄여 배경 중심에서 (+1,+3) 위치에 그린다. 칸 높이 38GUI에 맞춰 키우지 않는다. 정신 선택 창 항목은 정치 선택과 같은 `political_selectable_idea_entry_grid`(231×74)를 따른다: `GFX_idea_entry_bg_2` 프레임0(적용 중이면 프레임1, 이전 초록 덧칠 대신), 아이콘은 원본 크기의 0.5배로 중심 x42/y38, 이름 x67/y10, 비용은 오른쪽 아래 y47. `ScreenOfficerSpiritTest`(`politics` 묶음)는 채택 아이콘의 크기·위치를, `ScreenOfficerSourceTest`(`officers` 묶음)는 선택 창 첫 항목 아이콘이 원본 크기를 넘지 않는지 검사한다.

정당 지지도는 목록과 상단에서 정수, 정치·외교 툴팁에서 소수점 두 자리로 표시합니다. 원문에서 정한 경제·인력·안정도 등의 개별 정밀도를 공통 반올림으로 덮어쓰지 않습니다. 연료가 최대 수용량에 도달하면 원본 `FUEL_STOCKPILE_MAX_SHORT`의 `가득 참` 표시가 일수 예측보다 우선합니다.

국가·글로벌·슈퍼 이벤트와 연구·중점 완료는 서버가 발급한 수신 범위와 선택지만 표시합니다. 종료·권한 회수 후 도착한 응답으로 화면이나 HUD를 다시 열지 않습니다. 이미지 카탈로그 전체가 실행 가능한 이벤트나 정치 루트 전체를 의미하지 않습니다.

## 연구·건설·산업

생산 이름 머리행과 건설 대기열의 이름·작업 영역을 왼쪽 버튼으로 드래그하면 놓은 행의 최종 목록 인덱스로 `MOVE` 요청을 한 번 보낸다. `item`은 서버 발급 행 ID, `amount`는 0부터 시작하는 인덱스이며 건설은 `projects`의 주·건물 묶음 순서다. 서버 응답 전 목록은 바꾸지 않는다. 클릭만 하거나 같은 행에 놓기, 영역 밖 놓기, Esc, 화면 닫기 및 새 revision 수신은 이동을 취소한다. 응답 대기 중 반복 요청을 막고 기존 위/아래/처음·끝 버튼은 유지한다. 화면 끝을 향한 드래그는 목록을 스크롤한다.

생산 접기는 화면 로컬 상태다. 원본 `PRODUCTION_COLLAPSE`·`PRODUCTION_EXPAND`에 따라 클릭은 한 행, Control은 같은 장비 분류, Shift는 모든 행에 적용한다. 접힌 행은 원본 512×60 배경과 작은 장비 그림·생산량·재고·자원 배치를 사용한다. 펼친 행의 우선순위 버튼은 원본 y49/y76과 28px 높이를 사용해 108px 행 안에 둔다. 헤더는 TFR `countryproductionlineview.gui`의 `GFX_header_bg` x−245 배치에 따라 794×41 원본의 보이는 x245~794 영역을 사용한다. 주 상세 자원 목록은 숫자가 세 자리 이하이면 이전 −1GUI 오프셋을 해제하고 네 자리 이상이면 유지한다.

드래그 집중 재현은 팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=queue-drag`다. 생산은 기존 서버 생산 입력, 건설은 묶음 순서·인증 요청·취소·반복 차단을 확인하는 합성 입력이다. 합성 건설 입력은 실제 국가의 건설 상황이나 서버 비용·저장 동작을 증명하지 않는다. GUI2 2560×1440·1280×720 렌더와 입력 검사는 운영 접속·멀티플레이·직접 효과음 청취와 구분한다.

연구는 서버가 제공하는 실제 슬롯과 선행 조건·배타 선택·혜택을 사용합니다. 기술 툴팁의 미완료 선행 조건 줄 `기술 필요 (…)`는 원문 `RESEARCH_REQUIREMENTS_SINGLE`(줄 전체 §R)과 같은 글자를 쓰되, 사용자 지정에 따라 필요한 기술·장비 이름(예: 2세대·3세대 주력전차)만 TFR 노랑(§Y, 255·189·0)으로 구분하고 나머지 글자·쉼표·"또는"·괄호는 TFR 빨강(§R, 255·50·50)으로 둡니다. `ResearchTextChecks`가 두 색을 확인합니다. 트리는 원본 좌표와 지정된 보정 배치를 따르며, 카드·상세·하위 기술은 국가별 이미지와 원문 키를 공유합니다. 상세의 원작 장비 참고값은 서버의 적용 효과와 구분합니다. 연구 중인 슬롯은 노란 기술 이름 → 효과 → 서버 부대 묶음의 원본 아이콘·이름·수치 → 빈 줄 → 원문 설명을 표시합니다. 슬롯 툴팁에는 연구 시간·앞선 연도·선행 조건을 덧붙이지 않습니다. 빈 슬롯은 툴팁이 없으며 업체 아이콘은 기존 업체 문구·기술별 자금 툴팁을 사용합니다. 연도 영역은 서버 `Slot.yearTooltip`을 그대로 표시하며 빈 문자열에는 효과 툴팁을 대신 붙이지 않습니다. 기술 노드·카테고리·혜택 툴팁과 접근성 이름, 슬롯 클릭 동작은 유지합니다.

건설은 서버가 발급한 주·프로빈스·철도 선택지에 요청합니다. 예약·공장 배정·수리·전력은 서버가 계산합니다. 공장 선택의 호버 미리보기는 상태를 바꾸지 않으며 온전한 원본 아이콘으로 표시합니다. 주 상세의 수치·진행 게이지·변동치도 서버 응답을 사용합니다.

생산 장비·라인은 계열별 국가 장비 사진과 이름을 표시합니다. 실제 부족 자원은 서버의 `missingResources`로 판단하며 해당 숫자와 아이콘만 붉게 표시합니다. 구형 장비의 원본 붉은 마스크는 사진·이름 영역에 적용하고 자원·버튼 영역은 제외합니다. 자원 옆 역할 아이콘은 설계창의 종류 아이콘과 같은 원본 계열을 사용합니다.

군수는 원본 `countrylogisticsview`(560폭, 원작 20261119021334·20261009185901) 좌표로 그립니다. 국기 (445,13)·틀 (440,8)은 자국도 표시하고, 머리 띠 `GFX_deployment_binding` (5,47)의 아이콘·"장비 유형"·"상태"·"자원"은 띠 세로 중심 64.5에 맞춥니다. 장비 행은 `logistics/ui/equipment_entry` 510×57을 (13,77+58i)에 통째로 놓고 효율 막대 (16,12) 6×28, 그림 중심 (92,23) ×0.55(해군 ×0.4), 이름 (37,45) 115, 생산 (153..189)·수요 (256..292) 36폭, 균형 (290)·비축 (347) 50폭, 상태 막대 (192,19) 62×14와 표시 `progress_marker`, 자원 칸은 행 배경의 자원 상자(안쪽 x417..506, y3..53)를 3×2로 나눈 1/6 칸마다 27×27 원본 아이콘을 가운데 놓습니다(사용자 지시, 가로 먼저). MC 글자가 VCR02_10보다 넓어 생산·수요 칸은 막대와 겹치지 않게 원본 상자 안쪽 36폭을 씁니다. 원작 사진(060324·060346)대로 생산·수요는 일일 값(10 미만 소수 1자리), 균형은 생산−평균 수요(부호·색), 비축량은 비축−미지급 요청(부호·색, 1000 이상 K)입니다. 상태 막대는 수요 중 생산이 충당하는 비율(수요 0이면 생산이 있을 때 가득)만큼 초록으로 채우고, 생산이 없으면 회색 빈 막대입니다. ▼▲ 표시는 r=(수요−생산)/(수요+생산)일 때 막대 시작에서 r≥0이면 62·r, r<0이면 5·r px 이동합니다(수요 0이면 시작점, 생산 0이면 오른쪽 끝, 비축 중이면 0~5px 왼쪽 — 원작 측정값에 맞춘 식이며 엔진 식은 공개되지 않았습니다). 머리글(생산 제외)을 누르면 정렬하고 다시 누르면 순서를 뒤집습니다(`LOGISTICS_*_SORT_BY`). 행을 누르면 정보 창을 엽니다. 목록은 연료 머리 띠(B−210) 위에서 잘리고 x527 스크롤바와 휠(58px)로 움직이며, 생산 화면에서 장비를 눌러 들어오면 그 행으로 스크롤합니다(원본에 없는 행 강조는 없앴습니다). 연료 영역은 머리 띠 (7,B−210)·아이콘 (23,B−206)·"연료 우선순위"(띠 중심), 상자 4개 (20+127k,B−177) 120×68 `GFX_mini_tooltip`입니다. 육·해·공 상자는 이름 (10,8), 우선순위 점 3개 (7/27/47,25)(`priority_strip` 선택 프레임, 툴팁 `FUEL_*_PRIORITY_DESC`, 이미 선택된 점은 비활성), 종류 아이콘 (78,10), 충족 막대 (10,46) 100×14(`LOGISTICS_FUEL_PERCENTAGE`)이고, 네 번째 상자의 `비축량`(`PRODUCTION_OUTPUT`) 단추 (7,13) 105×23은 연료 이력 창을 열며 막대는 연료 비축률입니다. 군수공장·해군 조선소 상자 (20/170,B−94)는 이름·아이콘·사용률 막대(배정 공장÷보유 공장)입니다. 원본의 원자폭탄 상자 (391,B−94)는 자원 배경 (366,B−90)에 완전히 가려 원작 사진에 보이지 않으므로 그리지 않습니다. `비축량` 상자의 연료 종류 아이콘(frame 4)은 단추 그림 위에 그립니다. 아래 자원 칸 3×2(세로 먼저, 배경 칸 x1/52/102·y1/33)에는 생산 수요가 보유량보다 큰 자원만 아이콘(칸 왼쪽+2, 세로 중앙)과 부족량(빨강, 오른쪽 정렬)으로 놓고, 툴팁은 `LOGISTICS_RESOURCES_SUMMERY`·`LOGISTICS_RESOURCES_LAND/NAVAL/AIR`(배정 공장 기준)·`LOGISTICS_RESOURCE_NEED_SUMMERY`입니다. 원작 사진은 부족이 없어 비어 있습니다. 연료 이력 창은 원본 (539,78)(군수 창 기준 (545,0)) 395폭, 화면 높이 70%입니다: 머리 (1,7)·제목 `PRODUCTION_OUTPUT`·닫기 `GFX_NV_btn_close` (360,8), 그래프 바탕 (14,33) 356×260, `비축량`/`소모된 양` (77/196,39), `일`/`월`/`년` (77/156/234,69), 그래프 (85,105) 224×150 꺾은선과 틀 (76,103), 최댓값 (310,104)·최솟값 (310,245)·x축 0/개수−1·단위 (104,263)입니다. 일은 최근 31일, 월은 최근 13개 월 표본, 년은 연도별이며 소모 그래프의 월·년 값은 하루 평균입니다. 그래프는 위아래 3px 여백을 둔 224×150 영역입니다. 아래 `lines` 목록은 원작 사진(061953·061955)대로 비축량이면 `총비축량` 한 줄, 소모된 양이면 `총생산`·`총소비량`·`해군/공군/육군/기타 소비량`(`fuel_line_grid_entry_*`) 줄을 (18,308+30i) `logistics_fuel_graph_entry`에 놓고, 색 칸 (7,7)은 흰 원본 그림을 측정색(회색 CCCCCC·빨강 CC0000·파랑 0000CC·청록 00CCCC·초록 00CC00·노랑 CCCC00)으로 칠합니다. 색 칸을 누르면 그 선을 숨기고 원본 `GFX_accept_decline_icon` frame 1(✗)을 겹칩니다. 최댓값은 `LOGISTICS_VIEW_GRAPH_VALUE`(537.2K)·`LOGISTICS_VIEW_FUEL_CONSUMPTION_VALUE`(9.9K/매일)입니다. Esc와 닫기로 닫습니다.

장비 행을 누르면 같은 자리에 정보 창(`logistics_info_window`)이 열리고 연료 창과는 함께 열리지 않습니다. 제목 `PRODUCTION_OUTPUT`, `일/월/년` (77/154/230,39), 그래프 (80,78)는 비축 잉여/부족 이력(빨강 952523, 측정색)을 ±최댓값 대칭 축으로 그리며 최댓값·0·−최댓값을 (316,78/142/210)에 씁니다. 머리 (1,261)의 `장비`·`비축량`·`매주 생산` 아래 변형 행 (13,308+38i) `logistics_info_equip_variant_entry`에 이름(노랑)·비축량(`LOGISTICS_IN_STOCK_HIGH_VALUE`)·매주 생산(`LOGISTICS_WEEKLY_SPEED_VALUE`)을 각 상자 가운데에 쓰고, 비축이 있는 변형만 `GFX_NV_btn_delete`(322,6, 0.9배) 파괴 단추가 켜집니다. 파괴는 원작 확인 창(`LOGISTICS_DELETE_EQUIPMENT_CONFIRM_TITLE/DESC`, 90일)을 거쳐 `DELETE_EQUIPMENT`를 보내며 확인 창이 떠 있는 동안 산업 세션을 닫지 않습니다. 변형 툴팁은 이름·능력치·빈 줄·`LOGISTICS_AMOUNT_PRODUCTION_LINES`입니다. 목록은 휠과 스크롤바로 움직이고 Esc는 정보 창부터 닫습니다. `logistics` 묶음(`IndustryLogisticsTest`)이 행 위치·우선순위 점 위치·요청·`비축량` 위치·연료 창 단추·선 목록 6줄·계열 전환·수요/균형/비축/막대/표시 값·정렬·정보 창 단추와 기간 전환·변형 툴팁·파괴 확인과 요청·Esc·작은 창 경계를 검사합니다. 무역·모병·편제 조작은 서버가 발급한 선택지와 비용만 사용합니다. 미등록 대대나 참고 표의 내구도부터 장비까지의 수치를 임의로 추가하지 않습니다.

## 기갑·해군·공군 설계

전차·함선·함체·비행기 설계 하단의 경험치·자원·생산 비용은 TFR `tank_designer_view.gui`의 초기화 단추(y502, 높이 34)와 같은 세로 중앙선에 둔다. 저장 단추는 원본 그림 전체를 보존해 원본 사각형(x400, y502, 123×36)을 유지한다. 교리 상세 맨 아래 잠금 해제 단추는 육해공 모두 같은 경로로 `hud/<군종>_experience` 그림과 "N 잠금 해제" 글자를 단추 세로 중앙에 두고, 글자가 넘치면 크기를 줄인다.

해군 설계창은 이름 바로 아래 분류칸을 표시하지 않으며 닫기 버튼은 기존 위치에서 왼쪽으로 1 GUI 이동합니다. 기갑·공군 종류 칸과 모든 설계도면 내부 종류 라벨은 유지합니다. 장비 사진 옆과 저장된 설계의 생산 선택 이미지에는 작은 설계도 배지를 표시하지 않습니다. 새 차체·함체·기체와 미완성 장비의 배지는 유지합니다. 설계도면 자체와 장착 부품의 강조 레이어는 그대로 표시합니다.

초기 생산 가능 장비는 서버가 제공한 원본 국가 파생형의 부품을 장착한 상태로 엽니다. 서버가 설계 구성을 제공하지 못한 일반 장비를 클라이언트가 임의 설계로 만들지 않습니다. 초기화는 열 당시의 구성으로 돌아가며 화면 조회는 국가 자산이나 경험치를 변경하지 않습니다. 저장은 서버의 국가 설계 저장이며 파일로 저장·불러오기 버튼은 제공하지 않습니다.

서버는 연구·슬롯·군종·필수 부품 조건과 경험치 비용을 검증합니다. 기존 장착 부품은 선택 목록에 표시하되 새 부품의 연구 조건을 우회하지 않습니다. 공통 계산에서 군종별 수식과 조정치를 분기하고 화면은 발급된 수치와 차이를 표시합니다. 해군 생산 비용은 항상 `navprod_cost`를 사용합니다.

해군과 공군의 여러 분류 슬롯은 분류 → 부품 선택 순서입니다. 기갑의 단일 선택 계열은 해당 부품 목록으로 바로 엽니다. 기갑·공군에는 장착 모듈 제거 버튼이 없으며 공군 상단 슬롯 옆 역할 실루엣도 표시하지 않습니다.

능력치·슬롯·분류·부품·저장·경험치 툴팁은 `equipment_design_details.json`에 기록된 원문 키를 사용합니다. 부품 가산/배율, 연구 조건, 비용, 평균 관통, 임무 허용과 임무별 효과를 구분합니다. 공군 조정치 목록은 현재 주무장과 보조무장이 허용하는 임무에 따라 달라집니다. 임무 미리보기 선택 자체는 국가 상태를 변경하지 않습니다.

해군 조정치는 기갑의 좌우 기준선 안에서 좌측 상단부터 배치합니다. 작은 아이콘은 프로펠러 이동, 검·방패의 공격/방어, `…` 환경요소 순서이며 각 묶음의 효과를 툴팁으로 표시합니다. 단순 호버는 기본 커서를 유지하고 선택 가능한 공군 임무만 버튼 커서를 사용합니다. 보조함·주력함 설명은 원본 함종·효과·모듈 선택 안내의 세 문단과 강조색을 보존합니다.

## 지도·음향과 검증 한계

지도는 서버 장면을 타일별 메시와 표시 객체로 렌더링합니다. 프로빈스 경계는 회색, 주·해역·공역·해안 경계는 검은색입니다. 공통 거리 기준에는 높이 차이도 포함합니다. 지도 국가명 전용 글꼴은 지형 투과와 물 위 표시를 함께 처리하며 다른 글꼴에는 적용하지 않습니다.

재생 채널·곡 유지·차원 이동·전투와 부대 음향의 범위는 [음악 문서](audio.md)를 따릅니다. 원본 음원은 외부 팩에서 읽습니다.

`IndustryScreenChecks`는 서버 fixture로 설계 슬롯·부품 요청·초기화·저장·조정치·툴팁과 작은 창의 경계를 검사합니다. 지도·연구·정치·음악은 각 기능의 GameTest를 사용합니다. 이 검사는 실제 클라이언트 렌더러를 사용하지만 운영 서버 접속, 모든 원본 화면의 픽셀 단위 일치, 다중 사용자 동기화, 추가 렌더링 모드 조합을 보증하지 않습니다. 군수산업체 개량은 `TANK_DESIGN_TEAM_UPGRADE`와 서버 `upgradeChanges`로 연결하며 일반 장비 파생형 편집 등 다른 미지원 기능과 구분합니다.

해역·공역 이름은 서버가 생성한 전체 구역 배치 fixture에서 한국·동해·동중국해·셀레베스해·자와해·오스트레일리아 북부를 GUI2로 렌더링합니다. 글꼴·회전·크기·간격과 섬 위 겹침을 실제 렌더러에서 확인하며, 운영 서버에서 시선에 따라 표시를 전환하는 검증은 별도입니다.

## 지도 부대 표식

육군·해군·공군 표식은 TFR `interface/mapicons.gui`의 `unit_counter`, `unit_counter_navy`, `air_wing_counter`와 원본 DDS를 GUI2 기준 1:2로 표시합니다. 회색 원본 바탕과 육군 일반 병종 그림에 서버가 판정한 아군 초록색·동맹 파란색·적군 빨간색·제3국 회색을 적용하고 병종 뒤 검은 overlay는 별도로 유지합니다. 색상은 원본 `COUNTER_MODE_ALLEGIANCE_*` 값이며 `hoi:unit_hud_v4`로 전달합니다. 국기와 조직력·내구도 막대는 고유 색을 유지합니다. 관계 갱신은 기존 표식 가시성을 넓히지 않으며, 국가 미배정 관전자는 회색으로 봅니다. 네 관계의 GUI2 캡처와 프로토콜 왕복을 검증합니다. NATO 프레임은 사용하지 않습니다.

육군은 일반 병종 그림 아래 조직력·내구도 2줄과 그 왼쪽을 덮는 국기, 오른쪽 수량을 표시합니다. 해군은 주력함·보조함 수를 각각 위·아래에 표시하고 조직력·내구도와 국기를 아래 칸에 둡니다. 공군은 조직력 없이 하단 내구도 막대와 국기를 표시합니다. 같은 위치의 비행단은 각각 한 줄로 쌓습니다. 수치·상태는 서버의 비공개 `unit_hud_v4` 응답을 사용합니다.

`runGameTest -PhoiTestSuite=unit-hud`는 실제 클라이언트 렌더러로 GUI2·작은 창·사이드바·카메라 뒤 표식 제외·스냅샷 만료를 확인합니다. 원본 비교는 생성된 `hoi-unit-hud-counters-and-hover-gui2` 스크린샷을 사용합니다. 이 검사는 고정 데이터 검증이며 운영 서버의 다중 플레이 실측 검증은 아닙니다.

사단·함대·비행단 표식은 하단 육해공·첩보 카드보다 먼저 그려 카드가 항상 위에 보인다. `CommanderUnitHud`를 다른 하단 HUD보다 뒤에 등록하는 것은 Fabric `attachElementAfter`가 나중 등록을 먼저 그리기 때문이다. 카드 줄·명령 패널·첩보 조작 영역(`CommanderHud.covers`) 아래에 있는 표식은 클릭 선택·드래그 범위 선택·호버 툴팁에서 제외한다.

지도 부대 표식은 원본 `gfx/flags/small`의 10×7 국기를 사용한다. 작은 국기가 없는 국가만 기존 국기를 사용한다. 해군·공군 수량의 과거 개별 1GUI 하향은 제거하고 공통 숫자 보정과 숫자 칸 중앙 정렬을 사용한다. 육군도 같은 공통 숫자 렌더 경로를 사용한다.

## 반응형 GUI 단위

생산 라인은 충원·개선·비축의 원본 아이콘과 별도 툴팁, 장비 역할 배지를 표시한다. 상태와 숫자는 클라이언트에서 다시 계산하지 않고 서버 `IndustryView.Line.needState/needValue`를 그대로 쓴다. 충원(부족 수량)·개선(교체할 수량)은 원본 `PRODUCTION_EQUIPMENT_TOTAL_NEED`·`_UPGRADE`의 노란색, 비축(해당 모델 재고)은 `_FILLED`의 초록색이다. 두 필드가 없는 구형 응답은 상태 아이콘 없이 해당 모델 재고만 초록 숫자로 표시한다. 공군 생산 선택 행에는 설계에서 가능한 임무만 원본 작은 임무 아이콘으로 표시한다. 장비 능력치·설명은 서버의 실제 장비/설계 값과 원본 설명을 사용하며 줄 순서·빈 줄·색상·소수점 정밀도를 보존한다. 주 변동치에도 같은 원본 간격 처리를 적용한다. 원본의 일일 순손익·충족 예상일은 필요한 손실 이력이 없으므로 현재 생산량·부족 수량으로 구분해 표시한다.

공장 배정 격자(28×22 칸)의 군수공장·조선소 그림은 크기를 유지한 채 칸 정중앙에 둔다(사용자 지시 2026-11-19). 이전의 군수공장 오른쪽 1단위 이동과 조선소의 x+3/y+2 고정 위치(칸 오른쪽 1px 넘침)는 쓰지 않는다. 줄 머리의 공장 그림(원본 `factories_used`, 긴 칸 왼쪽 끝 x358/y7) 위치는 바꾸지 않는다. `IndustryScreenChecks`(`industry` 묶음)는 GUI2 2560에서 `military · 공장 1칸`·`naval · 조선소 1칸`의 보이는 그림 경계가 칸 중앙에서 1px 이내인지 검사한다.

생산 수요와 공통 계열 부족량은 스냅샷마다 한 번 집계해 모든 라인에서 공유한다. 장비 E개·생산 라인 L개에서 반복 집계 O(E×L)를 O(E+L)로 줄이고 O(E) 인덱스를 사용한다. 스냅샷이 바뀔 때 다시 계산하며 알려지지 않은 수요를 0으로 바꾸지 않는다. 책임 분리만으로 성능이 개선됐다고 판단하지 않는다.

설계창의 배경 생산 라인은 수동 렌더링 목록에 유지하고 입력 위젯에서는 제외한다. `-PhoiTestSuite=designers`는 GUI2 생산 이미지의 픽셀 유지, 배경 버튼 입력 차단, 한국·중국 공군 임무 목록과 중국 휴대용 대공 미사일 사진을 검사한다. 전체 `runGameTest`는 기존 화면·권한·입력 회귀 범위를 유지한다. 이 검증은 서버에서 생성한 스냅샷을 사용하는 실제 클라이언트 렌더링이며 운영 멀티플레이 접속 검증은 아니다.

공통 `UiUnits`는 2560×1440 화면의 GUI2 좌표인 1280×720을 기준으로 한다. 현재 GUI 뷰포트로 `new UiUnits(width, height)`를 만들고 `scale()`로 균일 배율, `length(n)`으로 반응형 nGUI 길이, `size(n)`으로 그 길이를 포함하는 정수 영역을 얻는다. 배율은 `min(width / 1280, height / 720)`이며, 화면 비율이 바뀌어도 가로·세로를 다르게 늘리지 않는다. 음수 좌표 이동에는 `length`를 사용하고, 아이콘 크기·행 높이·마우스 영역에는 `size`를 사용한다.

지도 부대 HUD에 이 단위를 적용했다. 국기·아이콘·조직력·내구도·숫자를 한 배율로 그리며, 같은 배율의 영역을 행 쌓기와 툴팁 판정에 사용한다. 수량은 공통 숫자 보정을 사용하며 해군·공군의 과거 개별 1GUI 하향을 중복 적용하지 않는다. 화면 좌표 투영과 사이드바 가림 영역은 실제 뷰포트 기준이다. 기존 메뉴의 개별 레이아웃은 아직 이 단위로 전환하지 않았다. 사용자의 Minecraft GUI 배율 설정은 변경하지 않는다.

`UiUnitsTest`는 기준 크기, 세로·가로 화면과 GUI 좌표 환산을 검사한다. `-PhoiTestSuite=unit-hud` 렌더링 검사는 GUI2의 2560×1440, 1920×1080, 2560×1080, 1080×1440과 GUI3의 1920×1080에서 그림과 마우스 영역을 확인한다. 운영 서버의 다중 사용자 동기화 확인과는 별도다.

지도 위 표식을 좌클릭하면 원본 `onmap_unit_counter_selected.dds`(육군·공군) 또는 `onmap_mission_unit_counter_selected.dds`(해군) 외곽선을 표시한다. 자유 커서와 화면 중앙 조준점의 좌클릭 모두 같은 표시 영역을 판정한다. 선택은 클라이언트의 일시적인 상태이며, 동일 부대의 스냅샷 갱신에는 유지하고 빈 지도 클릭·부대 제거·차원 변경·스냅샷 만료·접속 해제에는 지운다. 가려진 사이드바와 숨긴 HUD는 선택할 수 없다. 이 선택만으로 서버의 부대 이동·임무가 변경되지는 않는다.

Completion details and menu responses replace content through `DialogCoordinator.setContent`, preserving the current event stack and unresolved event tokens. Old content is removed once, and clearing content reveals the same event. Mouse regressions assert the exact issued action token, revision and choice rather than treating a click sound as proof of a server action. Server acknowledgements remain authoritative. The separate server queue tests and client rendering checks do not constitute operational multiplayer verification. Agency navigation and the dedicated agency_upgrade_complete popup are connected through server DialogCompletionProgress and client DialogCompletion.

Production role badges are limited to air defense, tank destroyers and artillery (including rockets). Their GUI2 offsets are 3 units left and 1 down; line equipment pictures move 1 unit down. Reinforcement, upgrade and reserve indicators move 1 unit down with their original horizontal position; the right-aligned stock count below moves 1 unit left. Blueprint and unfinished chassis show their blueprint marker without a role badge. The obsolete conversion marker beside the destination icon and its tooltip are absent; the actual efficiency gauge in the line header remains. These offsets use `UiUnits` without changing the player's GUI scale.

The production-source and production-china fixtures are generated server snapshots. Existing industry/designer fixtures retain their regression scenario state, including unlocks, stock, conversion inputs and trade partners, while equipment presentation fields are refreshed from the source import. These fixtures are renderer inputs, not migrated campaign saves.

### 권위 스냅샷 표시 연결

`IndustryView.Equipment.resources`의 삽입 순서가 생산 장비 행·자원 툴팁의 순서다. 서버는 OIL·ALUMINUM·RUBBER·TUNGSTEN·IRON·CHROMIUM·COAL 중 실제 항목만 넣는다. 클라이언트는 `entrySet()`을 그대로 순회하고 재정렬하지 않으며 선택 목록의 기존 세 아이콘 제한도 유지한다. `authoritative-fields`는 현재 서버의 연구/설계 업체 fixture를 새로 생성하여 복수 자원 행을 포함한 전체 장비의 순서를 검사한다.

외교 의용군 군 방패는 `ArmyRow.insignia/color`로 기존 `diplomacy/volunteers/shield/frame_N`을 그린다. 원본 `countrydiplomacyview.gui/diplomacy_send_units_group_entry/army/army_shield`의 x5/y4·33×32 크기와 사단 수를 유지한다. 프레임을 발급하지 않은 군에는 임의의 기본 휘장을 넣지 않는다. 서버 `ArmyInsignia`가 제공하는 원본 프레임은 81개이며 프로토콜 한도 255가 모든 프레임의 자산 존재를 뜻하지 않는다.

독립 `HudPayloads.State`가 열린 메뉴의 오래된 `hud`보다 우선한다. 일시정지·확인창·메뉴 재열기 여부에 관계없이 다음 HUD의 인력과 툴팁을 사용하고, live HUD가 숨겨지거나 unknown이면 해당 화면의 스냅샷을 사용한다. 이것은 서버가 다음 틱에 재전송한 값을 표시하는 규칙이며 클라이언트가 편제/해산 비용이나 인력을 계산하는 기능이 아니다. 실제 서버의 변경 승인·일시정지 틱 전송과 운영 멀티플레이 검증은 별도다.

연구 빈 슬롯의 절약 일수 문구는 상시 표시와 호버 툴팁 모두 근거 대기다. 원작 `20261028132110_1.jpg`·`20261028132111_1.jpg`의 빈 슬롯에는 상시 문구가 없다. `3411756715/localisation/korean/replace/TFR_replace_research_l_korean.yml`의 `DAYS_SAVED`·`DAYS_SAVED_DESC`는 존재하지만 Base/TFR/TFRKP의 interface 및 TFR scripted_guis에서 해당 키를 이 슬롯 툴팁에 연결한 정의를 찾지 못했다. 번역 키만으로 배치를 만들지 않으며 실제 원작 호버 장면이나 연결 정의가 확인될 때까지 빈 슬롯 툴팁에도 넣지 않는다. 받은 savedDays의 기존 진행막대·연구 계산과 연구 중인 슬롯의 연도·남은 일수·업체 배치는 유지한다. 3D 미리보기·기후 위장·공군 임무 카드에 필요한 모델/위장/임무 데이터는 현재 ResearchView에 없으므로 만들지 않는다.

### 날짜와 배속 렌더 검증

스코어보드 미리보기의 맨 위 행은 색을 추가하지 않은 `날짜: 2020년 1월 1일 00시`, 둘째 행은 `게임 속도: `와 배속별 색상 글자이다. PAUSED·X1·X3·X5·X7·X10을 각각 캡처하고 X10에서 `날짜: 2022년 12월 31일 23시`도 캡처한다. GUI2 2560×1440의 7장에서 날짜가 잘리지 않고 가장 긴 날짜의 사이드바도 화면 안에 있으며 다른 HUD와 겹치지 않았다. 실제 배속 글자 픽셀은 각각 `0xAAAAAA`, `0x55FF55`, `0xFFFF55`, `0xFFAA00`, `0xFF5555`, `0xAA0000`과 일치했다. 미리보기는 날짜·배속·참가자 3행을 사용하므로 최대 참가자 수, 페이지 전환, 서버의 10틱 갱신과 주기 액션바 제거의 네트워크 동작을 검증한 결과는 아니다. 메뉴·연구 개요의 `date`는 각 창의 기존 머리글 오른쪽에서 받은 문자열 그대로 표시한다. 원본 `topbar.gui/time_control_window`의 날짜와 서버 스코어보드 날짜는 각각 별도 표시이며, 이 머리글 배치는 클라이언트 창 구조에 맞춘 배치다. 제목·닫기 단추 경계 안에서 날짜 폭을 제한하고 제목 폭을 먼저 예약한다. 비어 있는 날짜를 만들거나 클라이언트 시계로 계산하지 않는다.

사본-6의 `china-705`·`china-182` 원본 fixture 2개를 재생성하고 GUI2 2560×1440·1280×720에서 실제 렌더를 확인했다. `china-182`의 기본 날짜 `날짜: 2020년 1월 1일 00시`와 둘째 원문 행만 바꾼 최장 합성 날짜 `날짜: 2022년 12월 31일 23시`는 모두 한 줄이다. 이전 분 포함 최장 날짜와 비교해 1280 창의 본문/뷰포트는180→168 GUI, 선택지 y는289→277 GUI, 창 높이는278→266 GUI로 돌아온다. 기본 날짜의168 GUI 본문·y277 선택지·266 GUI 창과 같고2560에서는 둘 다132 GUI 본문·y403 선택지·256 GUI 창이다. 두 해상도의 발신·날짜·빈 줄·본문·선택지 순서와 기존 토큰·리비전·입력 회귀를 유지한다. 본문 글자 높이9 GUI·행 간격12 GUI·시작 위치는 바꾸지 않았고 제목 높이는13.5 GUI로 확대했다. 작은 지휘관 제목 영역만13→14 GUI로 늘려 초상화/본문 전에 끝나도록 했다.

집중 재현 명령은 다음과 같다. 두 명령은 같은 클라이언트 체크아웃에서 순차 실행한다. 사본-6은 소스 입력으로만 사용하며 원본/사본의 소스는 수정하지 않는다. 기본·최장 날짜와 네 이벤트 제목의 검증은 지휘관 묶음에서 함께 실행한다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=leader-event "-PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI" "-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI/protocol/src/main/java" --console=plain --no-daemon
.\gradlew.bat runGameTest -PhoiTestSuite=scoreboard "-PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI" "-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI/protocol/src/main/java" --console=plain --no-daemon
```

처음에는 각각61.88초·37.95초에 종료 코드0으로 통과했다. 당시 스코어보드는 분까지 표시했다. 분을 뺀 스코어보드 재검사는42.47초에 종료 코드0으로 통과했으며 GUI2 2560×1440 PNG7장을 직접 열어 날짜 행·잘림·HUD 겹침을 확인하고 배속별 실제 글자 RGB도 대조했다. 이 재검사는 `ResearchScreenTest` 진입점1회이며 JUnit·서버 fixture producer·지휘관 묶음은 재실행하지 않았다. 첫 실행의 `ProtocolCampaignStyleTest` 2개·서버 fixture producer 3개는 실패/오류/건너뜀0이고 공유 프로토콜46개·fixture2개의 출처 해시를 확인했다. Fabric 클라이언트 진입점 `ResearchScreenTest`는 묶음마다1회 실행되어 지휘관 PNG20장(전체 화면13·창 crop7), 스코어보드 PNG7장을 생성했다. 리소스팩은 기존2026-10-12 08:22 KST 산출물을 재사용했다. 이전 날짜 집중 검증은 아래의 이벤트 제목·분 없는 날짜 전체 출하 검증과 구분한다. 운영 접속·다중 사용자 동기화·직접 효과음 청취는 미수행이며 서버/사본 소스·승인된 화면·팩 자산은 바꾸지 않았다. 사본-3 대비 원본 fixture 차이는 `china-182.body`의 날짜뿐이다. 그 밖의 캡처 픽셀 차이는 수정 없이 이슈 첨부 `render-verification.json`에 목록으로 기록하며 동적 배경을 포함하는 차이를 UI 변경으로 단정하지 않는다.

### 지휘관 이벤트 보고서

국가·지휘관·국제 뉴스·슈퍼 이벤트 본문과 선택지 툴팁은 기존 `DecisionText`의 대소문자 구분 원본 색을 사용한다. TFR `interface/core.gfx`의 `§A=CD3737`, `§g=B0B0B0`, `§8=CCA333`, `§3=518FDC`를 Minecraft 색 코드로 소문자 변환하지 않는다. `£GFX_x`와 `£x`는 외부 팩의 동일 글리프를 사용하며 미등록 아이콘 토큰은 숨긴다. 결정 팩의 `textImages` 또는 `sprites`에 등록된 배너·큰 그림 토큰은 그림 렌더용으로 유지한다. `GFX_kor_diffusion`처럼 `sprites`에만 있는 배너도 미등록 아이콘으로 지우지 않는다. 한국 국회·무소속 글리프는 TFRKP `TFR_interface_texticons_KOR.gfx`에서 추가하고 기존 국민의힘·더불어민주당 글리프는 재사용한다. 숫자·문장·공백·빈줄 순서는 원문을 유지한다.

국제 뉴스의 서버 `ack` 선택지도 기존 국가 이벤트 선택지 배경·입력 경로로 그린다. 선택은 서버 토큰·리비전·선택 ID를 보내고 `Close` 응답이 해당 이벤트 창과 사건·결정 목록을 제거한다. 오류 후 재시도에는 서버의 새 리비전 응답이 필요하며 같은/오래된 리비전의 갱신으로는 중복 클릭 잠금을 풀지 않는다. 최소화는 선택을 보내지 않고 사건·결정 목록에서 다시 열 수 있다.

이 흐름의 집중 재현은 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=event-tooltips -PhoiServerSource=<고정 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java -PhoiResourcePack=<팩>/resources.zip --console=plain --no-daemon`이다. `leaderEvent` 그룹은 서버 `DialogPresentationViewsTest`가 내보낸 `event-korea-356`(권성동 복당)·`news-turkey-2`(터키 공화국의 NATO 탈퇴)를 포함한다. GUI2 2560×1440·1280×720에서 KOR 지정·PAUSED 지도 HUD를 주입한 상태로 실제 이벤트 원문·아이콘·색, 뉴스의 서버 `ack`, 두 이벤트의 최소화·재열기, 좌우/반복 클릭, 같은/오래된 리비전 잠금, 새 리비전 재시도, 선택한 이벤트만 목록·창에서 제거를 검사한다. 국가는 화면 검사용 지정이며 상단 미제공 수치는 UNKNOWN을 유지한다. 오류 문구 오버레이·재발행·닫기 응답은 렌더 검사에서 주입한다. `Show(view, false)`의 동일 리비전 잠금 유지와 증가 리비전 해제는 실제 클라이언트 수신 처리기로도 검사한다. 이 검증은 운영 소켓·저장/재접속·멀티플레이·직접 음향 청취를 증명하지 않는다.

제목 1.5배 확정과 국제 뉴스 그림자 제거를 포함한 최종 소스는 아래 필터 없는 전체 명령을 871.27초에 종료 코드 0으로 통과했다. 클라이언트 JUnit 110개를 재실행했고 프로토콜 114개·자동 producer 43개·수동 producer 56개·장교단 mock GameTest 1개는 기존 성공 결과를 재사용했다. 실패·오류·건너뜀은 모두 0이며 자동 입력 75개·수동 입력 74개·공유 프로토콜 46개를 확인했다. PNG 1496장으로 이전 전체 실행과 같은 수이고 CMP-9 대비 추가 22장·누락 0장이다.

국제 뉴스 창 내부의 변경 19548픽셀은 모두 그림자 RGB(10,8,10)이 종이 배경 RGB(231,229,232)으로 바뀐 픽셀이다. 3배 최근접 확대의 제목 `19` 주변 943개·본문 `WHO` 주변 430개 그림자 픽셀은 0이 됐으며 글자 RGB(41,33,43)의 픽셀 수는 각각 1400/486으로 동일하다. 국가 이벤트·슈퍼 이벤트·완료 창, GUI2 두 해상도의 지휘관 네 창, 작은 국가 이벤트·최장 날짜·최장 제목 두 창의 내부는 그림자 수정 전과 0픽셀 차이다. 전체 화면 해시 차이 197장 가운데 국제 뉴스는 6장이고 그 밖 191장은 수정 없이 목록에 남겼다. 그중 27장은 RGB 채널별 차이가 최대 2이며 나머지 배경·애니메이션·툴팁 등을 포함하는 차이의 원인을 모두 검증한 결과는 아니다. CMP-9 대비 차이 212장과 작업 시작 시 fixture 차이 44개도 이슈 첨부 `render-verification.json`에서 구분한다. 같은 캡처명이 반복되는 8장도 발생 순서로 구분해 전체 수와 목록에서 누락하지 않는다.

최종 클라이언트 JAR SHA-1은 `52863f9ec96f87027a83c5f2a1ece0d02267d21c`, 빌드 시각은 2026-10-12 20:15:49 KST다. 클라이언트 전용 진입점·Java 25 바이트코드와 내장 이미지가 기존 mod-list icon 하나뿐임을 확인했다. 팩 자산 변경이 없어 기존 08:22 KST 빌드를 재사용했으며 서버/사본 소스·프로토콜·운영 설치는 바꾸지 않았다. 원작 런타임 일치·운영 접속·다중 사용자 동기화·직접 효과음 청취와 전체 1496장의 직접 열람은 미수행이다.

전체 재현은 위 사본-6 소스·프로토콜 속성을 유지하고 `-PhoiTestSuite`를 생략한다. 기존 수동 입력의 출처·해시를 확인한 다음 클라이언트 저장소에서 실행한다.

```powershell
$env:HOI_COMMANDER_PREVIEW = 'C:/HOI-client/client/build/generated/cmp11-manual-fixtures/commander'
$env:HOI_OFFICER_PREVIEW = 'C:/HOI-client/client/build/generated/cmp11-manual-fixtures/officer'
$env:HOI_LAND_PLAN_PREVIEW = 'C:/HOI-client/client/build/generated/cmp11-manual-fixtures/build/land-plan-overlay.json'
$env:HOI_MISSION_OVERLAY_FIXTURES = 'C:/HOI-client/client/build/generated/cmp11-manual-fixtures/missions'
.\gradlew.bat test runGameTest remapJar "-PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI" "-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI/protocol/src/main/java" --console=plain --no-daemon
```

국제 뉴스 그림자 수정 전의 제목·분 없는 날짜 검증은 사본-6으로 `test runGameTest remapJar`를 필터 없이861.22초에 통과했다. `HOI_COMMANDER_PREVIEW`, `HOI_OFFICER_PREVIEW`, `HOI_LAND_PLAN_PREVIEW`, `HOI_MISSION_OVERLAY_FIXTURES`를 사본-6의 원래 시험에서 생성한 `client/build/generated/cmp11-manual-fixtures` 입력으로 지정했다. 수동 입력은 지휘관53개·장교단17개·임무3개·육군 계획1개다. 클라이언트/프로토콜 JUnit224개·자동 producer43개·수동 producer56개·장교단 mock GameTest1개는 실패·오류·건너뜀0이며 자동 입력75개·공유 소스46개를 검증했다. 최종 실행은 이미 통과한 JUnit/producer 결과를 UP-TO-DATE로 재사용하고 실제 렌더링 진입점은 다시 실행했다. PNG1496장은 CMP-9의1474장보다22장 많고 누락0장이다. 네 이벤트 제목·두 해상도의 지휘관4장·최장 날짜·최장 제목2장과 후보 비교를 직접 열람했다. 전체1496장을 모두 직접 열람한 검증은 아니다. 파일 해시가 달라진256장과 작업 시작 시 보존한 fixture 대비 차이44개는 이슈 첨부 `render-verification.json`에 이름·경로·원래/현재 해시·필드로 남기며 고치지 않았다. 재발급된 session/token/군·계획 ID 및 출처 경로·해시 차이와 `china-182.body`의 날짜를 구분한다. 전체 화면의 해시 차이를 UI 변경으로 단정하지 않는다.

국가 이벤트·뉴스·슈퍼 이벤트 창 내부는 제목에서만 각각1744·3079·2579픽셀이 달랐고 제목 밖은0픽셀, 완료창은0픽셀이다. 두 후보 제목 crop은 검정과 배경의2색만 포함해 흐림이 없었다. 리뷰에서1.5배를 확정했다. 이 대조는 국제 뉴스 그림자 제거 전의 제목 변경 단계다. 본문·부제·선택지 시작 좌표는 유지한다. 리소스팩은 기존08:22 KST 산출물을 재사용하며 원본 글꼴 동등성·운영 접속·다중 사용자 동기화·직접 효과음 청취는 확인하지 않았다. 원본 그림·글꼴 에셋·프로토콜과 운영 설치는 바꾸지 않았다.

`EVENT`의 `presentation = leader_event`는 `DialogStack`에서 `DialogLeaderEvent`로 표시한다. 원본 TFR `interface/eventwindow.gui`의 `EventWindow_leader`처럼 제목, 왼쪽 초상화와 틀, 오른쪽 본문, 한 열의 선택지를 사용하며 이벤트 그림·국기·국가 이름 줄을 추가하지 않는다. 초상화 영역은 표시 전용이다. `image`의 지휘관 초상화가 없으면 기존 `politics/empty/leader`(`GFX_leader_unknown`)를 사용한다. presentation이 빈 옛 대기 이벤트는 기존 국가 이벤트 화면을 사용한다. 국제 뉴스는 종이 배경의 제목·부제·본문·타일 그림자를 제거하며 슈퍼 이벤트는 제목 외의 기존 렌더를 유지하고 완료 알림은 그대로다. 선택은 원래 `DialogCoordinator.choose`로 토큰·리비전·선택 ID를 보내며 대기 중 반복 제출과 같은/오래된 리비전의 잠금 해제를 막는다.

원본 좌표는 제목 `(14,70) 551×32`, 본문 `(198,121) 350×350`, 초상화 `(43,125) 156×210×0.85`, 틀 `(35,116) 172×232×0.85`, 선택지 시작 x=115·아래 영역 y=10·한 칸 300×47이다. 원본 버튼 그림은 352×48이며 원본 격자의 47 단위 간격을 유지한다. 실제 DDS는 top 605×123, middle/options 605×80, end 605×68로 GUI 선언의 581×121·605×235·581×85와 다르다. 기존 이벤트의 화면 크기 환산과 최소 폭 240 GUI 단위를 사용하고 2560×1440 GUI2에서 원본 단위당 약 0.5 GUI 단위로 그린다. 가운데/선택지 배경은 세로 가장자리 16 원본 픽셀을 보존하고 가운데를 반복한다. TFR의 중간 스프라이트 border x=350은 실제 폭의 절반보다 크므로 가로 타일 규칙을 추정하지 않고 원본 전체 폭을 유지한다.

원작 인게임 창을 관측하지 못해 아래 영역의 GUI 기본 y=221과 끝 조각의 y=0을 실제 최종 배치로 단정하지 않는다. 렌더는 최소 235 원본 단위의 초상화/본문 영역 뒤에 선택지를 놓고 본문이 길면 영역을 늘린다. 화면 높이가 부족하면 본문만 스크롤하고 24개까지의 선택지는 한 열의 페이지로 유지한다. 원본 루트는 CENTER 기준 `(-282,-310)`, 크기650×0이고 moveable=yes다. 지휘관 보고서는 제목 영역의 좌클릭 드래그로 이동하며 창과 단추를 화면 안쪽 8GUI 경계에 유지한다. 우클릭 드래그는 이동하지 않고 최소화·재열기·revision 갱신은 위치를 유지한다. 긴 본문은 휠과 PageUp/PageDown·Home/End로 읽는다. 이는 원본 moveable 연결이며 엔진의 미관측 동적 높이와 완전한 동일성을 뜻하지 않는다. 선택지가 있는 지휘관·국가 이벤트와 국제 뉴스는 원본 최소화 단추와 Esc로 접고 사건 목록에서 다시 연다. 세션 유지와 읽기용 뉴스·슈퍼 이벤트의 예외는 아래 효과 툴팁과 최소화 규칙을 따른다.

사용자 지시로 지휘관·국가 이벤트·국제 뉴스·슈퍼 이벤트의 맨 위 제목은 볼드체와 본문 대비1.5배를 항상 사용하며 본문 크기로 돌아가지 않는다. Minecraft 기본 글꼴을 유지하며 새 글꼴 에셋을 추가하지 않는다. 본문은9 GUI 픽셀(배율2에서18 화면 픽셀), 본문 행 간격은12 GUI 단위이며 선택지는 기존 `UiMenuButton` 글자 크기를 사용한다. `DialogScreen.EVENT_TITLE_SCALE` 하나로 제목 확대 비율을 바꿀 수 있다. 한 줄로 들어가지 않는 제목은 1.5배 그대로 볼드 글자 폭으로 단어 단위 줄바꿈하며 줄 간격은 `ceil(9×1.5)`=14 GUI이다. 국가 이벤트와 지휘관 보고는 제목 상자 아래에 본문 한 줄(12 GUI)의 빈 줄을 두고 본문을 시작하며, 제목 줄이 늘면 아래 내용(지휘관 초상·틀 포함)을 그만큼 내린다. 국제 뉴스는 늘어난 제목 높이만큼 그림·덮개·본문을 내린다. 슈퍼 이벤트는 원본 상단 제목 띠(원본 5~65px) 안에 들어가는 줄까지만 그리고 남는 내용은 마지막 줄 끝에 `…`를 붙인다. 네 창의 본문과 선택지 글자는 볼드체이고 선택지 높이도 볼드 폭으로 계산한다. 볼드 글자가 넓어 1280×720 GUI2의 지휘관 보고(본문 칸 139 GUI)에서는 가장 긴 날짜 `날짜: 2022년 12월 31일 23시`가 두 줄이 될 수 있으며, 2560×1440에서는 한 줄이다. 키워드 아이콘·국기 글리프는 볼드로 겹쳐 그리지 않는다. 분류 띠·부제·완료창과 STATE/DIPLOMACY/POLITICS/COMMANDER 대화창에는 적용하지 않는다.

원본 TFR `interface/eventwindow.gui`의 국가 이벤트 제목은 `Electro_20`(20), 지휘관 제목은 `hoi4_typewriter22`(22, lineHeight22), 본문은 `Electro`(`Electro_0.fnt` size=-16, lineHeight19), 선택지는 `hoi_20bs`(20, lineHeight20)이다. 원본 제목/본문 크기 비율1.25~1.4를 근거로 GUI2 영문·숫자가 정수 픽셀 배율인1.5배를 기본으로 한다. 실제1.5배/2배 제목을 최근접3배로 잘라 나란히 비교하며 원본 글꼴 동등성을 주장하지 않는다.1.5배는 작은 지휘관 창에서도 확대 제목을 유지하고 본문과 구분되어 기본으로 권한다. 한글의 원래 글리프 특성은 남아 있으며 새 에셋 없이 획 전체를 다른 글꼴처럼 고르게 만드는 변경은 하지 않았다. 제목은 자기 영역 중앙에 배치하고 기준점을 정수 GUI 좌표에 맞춰 추가적인 반 픽셀 이동을 피한다. 지휘관 제목 영역은 원본32 단위 높이와 확대 글자 높이 중 큰 값을 사용하며 본문 시작은 원본121 단위를 유지한다. 국가·뉴스 제목은 기존 제목/국기 영역 안의 y32부터18 GUI 높이에 놓고, 상수를2배로 바꾸면 부제 y48을 넘지 않도록 제목 시작만y30으로 올린다. 슈퍼 이벤트는 기존 띠 내부 y2부터21 GUI 높이에 맞추며1.5배 제목이 띠 안에 들어간다. 본문·부제·선택지 좌표는 바꾸지 않는다.

서버에서 온 `china.705.d`와 미지원 대괄호 함수는 그대로 표시한다. `[GetDateText]` 날짜 확장은 서버에서 처리하며 기본 `2020년 1월 1일 00시`, 최장 `2022년 12월 31일 23시`처럼 분을 쓰지 않는다. 클라이언트는 발급된 문구를 바꾸지 않는다.

지휘관 창은 원본 CENTER(-282,…)의 가로 위치를 적용해 2560×1440 GUI2에서 왼쪽을 998 화면 픽셀에 둔다. 사용자 지시(2026-11-17)에 따라 세로는 본문 길이로 정해진 창 높이 기준으로 화면 중앙에 두며, 작은 화면에서는 창 전체가 화면 안에 들어오도록 위치를 보정한다. 국가·뉴스 이벤트 창도 같은 지시로 실제 창 높이 기준 세로 중앙에 두고, 슈퍼 이벤트는 원본 982×594 중심과 TFRKP 표시 오프셋을 유지한다. 일반 국가 이벤트의 과거 전체 창 중앙정렬은 아래 세 이벤트 창의 원본 프레임 규칙으로 교체했다. 스택은 종류별 마지막 이벤트·슈퍼 이벤트·완료 창을 선택하고 메뉴와 함께 표시하며 위치에 의존하지 않는다. Minecraft 버튼 영역은 정수 GUI 좌표이므로 원본 47px 간격의 23.5 GUI 단위를 표현하지 못한다. 2560×1440에서는 두 선택지 간격이 48 화면 픽셀로 반올림된다.

지휘관 제목·본문·선택지는 그림자 없이 그린다. Minecraft 26.3의 text/centeredText 기본 호출은 shadow=true여서 밝은 패널의 어두운 영문·숫자가 두 겹으로 보였다. 같은 경로의 종이 배경인 일반 국가 이벤트(EVENT)와 국제 뉴스(GLOBAL_EVENT)의 제목·부제·본문·타일 글자도 그림자 없이 그린다. 어두운 STATE/DIPLOMACY/POLITICS/COMMANDER 패널과 슈퍼 이벤트의 띠·인용문, 완료 창의 기존 그림자 설정은 유지한다. 제목은 core.gfx의 hoi4_typewriter22 색 0xff000000, 본문은 fonts.gfx의 Electro 흰색 tint와 Electro_0.dds의 알파>0 글자 픽셀 15,438개가 모두 RGB(0,0,0)인 근거로 검정색을 사용한다. 선택지의 기존 글자 색상은 유지한다. 일반 이벤트 그림자 수정 대조에는 비교 명령에 `--allow-event-text-change`를 추가한다. 이 옵션은 제목 변경 전의 일반 이벤트 그림자 비교용이며 나머지 세 창은0픽셀을 요구한다. 현재 볼드 제목 변경과 국제 뉴스 그림자 제거를 포함하는 비교에는 그대로 사용할 수 없다.

지휘관·일반 국가 이벤트의 긴 선택지는 공통 `UiMenuButton.wrapLabel()`로 전체 문장을 가운데 줄바꿈한다. 기존 버튼 글자 높이 10 GUI 단위와 글꼴을 유지하고 줄 수에 맞춰 버튼·선택지 영역을 늘리며 부족한 본문 영역은 기존 스크롤을 사용한다. 지휘관 버튼 배경은 높이를 늘릴 때만 기존 9분할 도우미로 원본 테두리를 보존한다. 1280×720 GUI2의 china-705는 두 줄씩 표시하며 버튼 높이19→24 GUI 단위, 선택지 간격19→26, 창 높이209→224로 바뀐다. 두 2560×1440 창과 china-182의 작은 화면 배치는 그대로다. 일반 이벤트의 과거 두 열은 아래 원본 프레임의 한 열로 교체했다. `--allow-event-choice-change`는 일반 이벤트의 하단 본문·선택지 영역만 비교에서 허용하고 제목 변경 전 비교의 뉴스·슈퍼 이벤트·완료 창은0픽셀을 요구한다.

`leaderEvent` fixture 그룹은 고정 또는 지정된 서버 소스의 `DialogPresentationViewsTest`가 생성한 `china-705`·`china-182`를 가져오며 `SourceLeaderEventTest.java`도 출처 해시에 포함한다. 집중 재현 명령은 다음과 같다. 서버 원본을 다른 작업자가 수정 중일 때 두 속성을 같은 검증된 서버 소스 사본으로 지정한다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=leader-event -PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-2/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-2/HOI/protocol/src/main/java --console=plain --no-daemon
```

`DialogLeaderEventTest`는 실제 GUI2 두 해상도의 전체/창 PNG, 초상화 누락 fallback, 요청 값·왼쪽/오른쪽 클릭·드래그·반복 클릭·비활성 상태·리비전·Enter·긴 본문과 페이지를 검사하고 실행 폴더에 `leader-event-measurements.json`을 쓴다. 선택지의 모든 글자·마지막 문장부호, 기존 글자 크기에서의 가로·세로 여유와 버튼 겹침을 검사하고 china-705의 서버 문구를 일반 이벤트로 표시한 1280×720 전체/창 PNG도 생성한다. `-PhoiTestSuite=leader-event-regression`은 외부 팩을 로드한 기존 국가 이벤트·뉴스·슈퍼 이벤트·연구 완료 알림을 촬영한다. `python scripts/compare_leader_events.py --before <기준 screenshots> --after <집중 screenshots> --measurements <leader-event-measurements.json> --output <비교 폴더>`는 실제 창 내부의 전후 픽셀, 원본 정의/실제 렌더 나란히 비교와 SHA-256 manifest를 생성한다. 비교의 정의 표는 실제 원작 캡처로 표시하지 않는다. 지정 Steam 스크린샷506장의 contact sheet에서 보고서 형태의 지휘관 이벤트는 찾지 못했다. 원작 런타임의 최종 배치·hover shader 일치·직접 효과음 청취·운영 멀티플레이는 별도 검증이다.

최종 선택지 줄바꿈 수정본은 위 snapshot-2로 집중 `test runGameTest remapJar -PhoiTestSuite=leader-event`를1분3초, 필터 없는 전체 명령을20분3초에 통과했다. JUnit224개(클라이언트110·프로토콜114)와 자동 producer42개는 실패·오류·건너뜀0이며 자동 fixture75개·프로토콜46개·실제 PNG1474장을 확인했다. 변경되지 않은 JUnit 결과는 전체 실행에서 UP-TO-DATE로 재사용했고 전체 producer는 재실행했다. 집중/전체의 두 fixture·두 해상도 좌표는 동일하다. china-182의 발신·날짜·빈 줄·본문 순서와 전체 선택지·마지막 문장부호·버튼 겹침·입력 경로를 검사했다. 두2560 창은 선택지 수정 전과 변경 픽셀0이다. 작은china-182의 좌표는 같으나 crop에는23137픽셀 차이가 있다. 그중23125개는 RGB 채널별1~2 차이이고 나머지12개는 왼쪽 바깥 경계이며 원인은 확정하지 않았다. 일반 이벤트는 그림자 제거 뒤 기준과 비교해 하단 본문·선택지 영역에서만11918픽셀 변경됐고 허용 영역 밖과 뉴스·슈퍼 이벤트·완료 창은0이다. JAR SHA-256은 `30d211591908ef1c1f0a86e25f80d2be0fdb844dc4f2979902739adf1c8acd88`이며 내장 이미지/음향은 기존 mod-list icon 하나뿐이다.

서버 함수 확장·장비 순서 고정 사본(snapshot-3)은 두 소스 속성을 같은 사본으로 지정한 `test --rerun runGameTest remapJar --console=plain --no-daemon`으로16분31초에 통과했다. JUnit224개를 재실행했고 이전 사본-3 producer43개·fixture75개는 현재 provenance를 확인해 재사용했다. 별도 새 JVM의 장비 export는8개 검사·46초에 통과했다. 모두 실패·오류·건너뜀0이며 실제 PNG1474장을 생성했다. 당시 GUI2 두 해상도의 china-182 날짜(이후 위의 분 단위 한국어 표기로 교체)는 한 줄이고 본문·선택지·하단과 겹치지 않는다. CMP-8 대비 네 창의 좌표·글자 높이는 같고 china-705 두 창은0픽셀, china-182 두 창은 날짜 영역만 각각878픽셀 차이다. 포획·HQ의144개 목록11종과 무기대여의13개 목록4종은 두 export에서 순서·장비 값이 모두 같다. resources 키 순서763건은 값 변경 없이 남겼다. 지휘관 밖 fixture51개·캡처140장 차이를 수정 없이 기록했으며 배경·마우스 툴팁·미확정 픽셀 차이를 포함한다. 전체 캡처 차이146개 중4개는 원본 지휘관 전체/창이고2개는 합성 지휘관 검사다.140장의 원인을 모두 직접 대조한 결과로 간주하지 않는다. JAR 해시는 위 수정본과 같으며 승인된 UI·프로토콜·팩 자산 소스는 변경하지 않았다. 팩은 중단 실행의2026-10-12 08:21:46~08:22:30 KST 빌드를 재사용했다. 원작 런타임·직접 효과음 청취·운영 멀티플레이는 미수행이다.

이전 줄바꿈 수정 전 사본의 전체19분40초·집중1분1초·PNG1472장과 그림자 수정의 일반 이벤트 글자 영역12956픽셀 차이는 별도 단계의 기록이다. snapshot-2는 이벤트 문구·결정 자료 등을 바꾼 서버 입력으로, 지휘관 밖 fixture49개(결정12·교리10·전술7·포로6·무기대여5·최고사령부5·의용군4)의 필드 차이를 기록하고 수정하지 않았다. 전체 캡처의 원본 픽셀/크기 차이222개는 배경·동적 상태를 포함하는 목록이며 모두 UI 변경으로 단정하지 않는다. 전체 수동 입력은 기존 검증 사본의 `HOI_COMMANDER_PREVIEW`, `HOI_OFFICER_PREVIEW`, `HOI_LAND_PLAN_PREVIEW`, `HOI_MISSION_OVERLAY_FIXTURES`를 사용했고 육군 동반 `land-plan-preview.json`도 보존했다. 팩 재빌드는2026-10-12 06:57:22~06:58:11 KST, UI ZIP 수정06:57:49이며 SHA-1 `260c22ccec3b7deb690ad5c319a083b0dc8346d7`은 같다. 이 단계의 자산 소스 변경은 없다. 원작 런타임·직접 효과음 청취·운영 멀티플레이는 미수행이다.

## 군수 툴팁과 해군 배치

군수의 평균 생산 효율·장비 유형·생산·상태·수요·균형·비축량·자원 헤더는 본편 공식 한국어(`localisation/korean/logistics_l_korean.yml`, 2026-11-20 설치)의 LOGISTICS_* 제목/설명/정렬 키를 사용합니다. 제목의 금색, 생산 효율 아이콘과 문단 사이 빈 줄을 유지합니다. 행의 각 수치와 자원 아이콘은 별도 호버 영역을 가지며 문구는 원본 키로 만듭니다: 생산 `LOGISTICS_ACQUIRED_TOTAL_*`·`LOGISTICS_EFFICIENCY_NOT_HUNDRED_*`·`LOGISTICS_PRODUCING`/`LOGISTICS_VARIANT_SPEED`(생산 없음 `LOGISTICS_NO_EQUIPMENT_PRODUCTION`), 상태 `LOGISTICS_STATUS_*`, 수요 `LOGISTICS_NEED_*`+`LOGISTICS_NEED_ARMY/AIR_REINFORCEMENTS_*`(원작 사진 060324와 같은 두 줄), 균형 `LOGISTICS_SURPLUS/DEFICIT_*`+`LOGISTICS_BALANCE`, 비축량 `LOGISTICS_INSTOCK_*`+`LOGISTICS_STOCK_REQUEST`+`LOGISTICS_ESTIMATED_*`, 자원 아이콘 `LOGISTICS_RESOURCE`·`LOGISTICS_RESOURCE_OTHER_LINES`·`LOGISTICS_RESOURCE_SHORTAGE`. 10 미만은 LOW(소수), 이상은 HIGH 키를 씁니다. 수요 외 칸의 문구 조합은 원작 사진이 없어 키 이름으로 정한 것이며 원작 화면과 대조하지 못했습니다.

완성된 함정은 생산 라인의 이미지 게이지를 100%로 유지합니다. 별도의 완성 함선 버튼은 없습니다. 인력이나 항구가 부족하면 원본 PRODUCTION_CAN_NOT_DEPLOY 및 해당 사유 키로 툴팁을 표시하고, 국가 인력이 확보되면 다음 서버 시간 처리에서 지정 항구에 자동 배치합니다. 생산 라인별 배치 대상은 서버에 저장합니다. 수리 대기열의 개별 항구 펼침은 전체 펼침/접기 버튼의 모드를 변경하지 않습니다.

IndustryScreenChecks는 8개 군수 헤더의 원문·색상·빈 줄 및 GUI2 캡처와 수리 개별/전체 버튼 순서를 검사합니다. IndustryNavalDeploymentTest는 GUI2 가득 찬 생산 게이지·원본 인력 부족 툴팁·선택 항구와 전송 요청을 검사합니다. 실제 운영 서버 연결이나 모든 원본 화면의 픽셀 단위 일치를 의미하지 않습니다.

해군 생산 대기 화면만 재현하려면 `./gradlew runGameTest -PhoiTestSuite=naval-production`을 실행합니다. fixture의 국가 인력과 HUD 인력은 같은 99명으로 설정합니다.

The agency sidebar uses a 270-unit GUI2 baseline. Its five groups and 23 upgrades use original source sprites, per-group upgrade sounds and server-generated rich tooltips. The sidebar indicator and detail button share each item's tooltip. Current and next level effects preserve explicit decimal precision, source icon tokens, colors and blank lines. The agency render suite accepts server-generated fixtures through HOI_AGENCY_PREVIEW; synthetic rendering is separate from operational multiplayer verification. The player's GUI scale is never changed by the screen.

Agency names render bold at 115% of the normal label size. The upgrade-window heading is bold at its existing size and centered position. The five branch headers remain left-aligned and are vertically centered in their 14-unit strips. Each civilian-factory icon stays above its cost and is horizontally centered in the same 13-unit column as the number. The cryptology header has separate cipher-strength and decryption-power hover targets with source icon tokens, paragraph spacing and per-field precision. Values come from the private server view and share the simulation calculation. The source fixture suite captures both tooltips and the complete upgrade window at GUI2; other GUI scales remain compatibility checks. Creation and upgrade factory costs use white when sufficient and red when insufficient.

Production rendering fixtures are generated by HOI's IndustryProductionEquipmentTest and bundled with the client tests. The default suite includes freight trains II/III, floating harbors and both powered exoskeleton models; it also checks train/exoskeleton successor obsolescence without HOI_PRODUCTION_PREVIEW. That variable only overrides fixture input for source comparisons. Disconnect callbacks schedule private screen, HUD, map and audio cleanup on the client thread. Menu and dialog regression checks originate outside that thread, verify removal on it, and reject delayed replies from the cleared menu session. Run `./gradlew runGameTest -PhoiTestSuite=disconnect --console=plain --no-daemon` to isolate the disconnect regressions; the default full suite retains both checks. These fixture checks do not replace operational multiplayer verification.

Validation: the focused disconnect suite and the full client `test runGameTest remapJar --console=plain --no-daemon` command passed with the bundled production fixtures. All 104 client JUnit tests passed. The final client JAR contains all seven rebuilt disconnect handlers, and its 170 nested protocol classes match the validated server JAR. GUI2 rendering and original armored-train pixels were checked; operational multiplayer and deployment were not performed.

## UI preservation constraint

All existing and future UI layouts and cells use Minecraft GUI scale 2 (GUI2) as the reference for sizing, spacing, alignment, text, icons, gauges and primary visual validation. Preserve the user-defined per-cell alignment rules; GUI2 is the layout baseline, not permission to overwrite the player's GUI scale setting. Other scales and viewport sizes remain compatibility checks.

## Localisation constraint

Current and future Korean UI text and tooltips follow the TFR Korean translation patch (workshop 3411756715). Resolve original localisation keys before writing translations; use TFRKP-specific Korean names only where the patch has no matching key. Preserve source-key provenance and command/save IDs.

## Client implementation constraints

경험치 연결·계획 중앙 재정렬은 같은 고정 사본과 r12 육군 입력으로 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout`을 실행해 5분50초에 통과했다. 두 해상도 GUI2의 기존 헤더(화면 좌표 6,80~431,290) 변경 픽셀은0이며 사단 행의 변경은 경험치·계획 영역에 한정된다. 기존 게이지 채움·좌우 클릭·반복 입력·선택·스크롤 회귀를 유지했다. 첫 실행은 제공자 오류로 실제 화면262장 후 중단되었으므로 통과로 세지 않는다. 재실행은 앞서 성공한 단위 검사 결과를 재사용했다. 이후 원본 경험치 프레임·막대 크기 복원은 같은 입력과 명령으로 팩 빌드 후 6분3초에 통과했다. 실제 PNG402장, client127·protocol121 단위 검사 실패·오류·건너뜀0이며 변경 없는 protocol·생산자 성공 결과는 재사용했다. 두 해상도의 이전 헤더 변경 픽셀은0이고 본문의 차이는 경험치 영역에 한정된다. 이 집중 검증은 최신 서버 입력·필터 없는 전체 검사·운영 접속·저장/재접속·멀티플레이·직접 청취를 포함하지 않는다.

사단 목록 본문의 경험치 아이콘은 TFR `unitview.gui/unit_level`과 `unitview.gfx/GFX_unit_level`의 원본 27×27 프레임 전체(투명 여백 포함)를 GUI2의 13.5×13.5GUI로 표시한다. `GFX_unit_xp_bar`의 28×6 원본 막대는 세로 3×14GUI로 회전해 프레임 오른쪽에 붙인다. 두 요소의 전체 폭 16.5GUI를 경험치 헤더 열(행 시작 기준 48~61GUI)의 중앙에 맞추며, 레벨마다 다른 투명 여백을 잘라 아이콘을 확대하거나 이동하지 않는다. 원본 `20261017220719_1.jpg`와 함께 배치를 비교한다. 계획 게이지는 다음 헤더 열(61~71GUI), 참호 아이콘·게이지 묶음은 그 다음 열(71~86GUI)의 중심에 맞춘다. 경험치 묶음과 계획 게이지는 반올림된 열 경계의 중점을 실수 좌표로 계산하며, 계획·참호의 기존 게이지 높이와 모든 진행률·색을 유지한다. 같은 활동 아이콘 칸의 이동·후퇴·훈련·전투 그림도 이 중심을 따른다. 점령지 본문의 국가 이름·국기 영역은 툴팁을 표시하지 않고 기존 펼치기/접기 입력을 유지한다. 저항도·순응도·경제·법률·주둔 편제의 툴팁은 유지한다. 클라이언트 클래스에 직접 넣은 포획 지휘관·생산 효율 툴팁의 하이픈 구분선은 빈 줄로 표시한다. 현행 공용 툴팁은 서버 구분선도 표시할 때 빈 줄로 정규화하며 원문 저장값은 바꾸지 않는다. 집중 검증은 `army-layout`과 `occupation`을 사용하며 서버 전체 통과 고정 사본 확인 전의 합성/이전 서버 입력 검증은 최신 서버 연동 완료로 보지 않는다.

이 보정은 팩 빌드 후 Java25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout` 6분4초와 `-PhoiTestSuite=occupation` 49초에 통과했다. 서버·프로토콜 경로는 모두 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42/synthetic-server/HOI` 사본을 지정하며 육군 입력은 기존 r12 `HOI_ARMY_REVIEW_PREVIEW`를 쓴다. 클라이언트127·protocol121개 단위 검사는 실패·오류·건너뜀0이고 점령지 실행은 앞서 성공한 단위 결과를 재사용했다. 실제 PNG는 육군402·점령지29장이다. 두 해상도 GUI2에서 기존 집단군 헤더 영역의 변경 픽셀은0이고 계획·참호 막대의 채움 픽셀과 국가 호버 화면을 확인했다. 첫 실행은 새 테스트의 존재하지 않는 `Button.getTooltip()` 호출로 컴파일에 실패했고, 해당 호출 제거 후 실제 호버 캡처로 확인했다. 서버 스킬/특성·혼합 후보의 최신 입력, 당시 서버 구분선 생성 변경, 필터 없는 전체 검사·운영 접속·저장/재접속·멀티플레이·직접 청취는 이 결과에 포함하지 않는다.

육군 머리줄의 군 사단 수는 y=4GUI를 유지하고, 군 이름 라벨은 y=5→4GUI로 1GUI 올려 사단 수와 같은 높이11.5GUI 칸에 놓는다. 집단군 이름과 군 수/한도(예: 3/5)의 기존 위치는 유지한다.

머리줄 상향은 팩 빌드 후 cmp50 고정 서버·protocol과 cmp41-r5 팩의 `test runGameTest remapJar -PhoiTestSuite=navy-layout`로4분4초에 통과했다. GUI2 2560×1440·1280×720 모두 육군 이름의 글자 픽셀이 정확히2px 올라갔고 사단 수는 그대로다. 해군 이름과 머리줄 태스크포스 수는 모두 정확히2px 올라가 같은 y87~100px에 놓인다. 기존 글자 픽셀과 가로 좌표는 같다. 육군 이름에는 한글 글리프 아래쪽2px가 더 있어 본문 경계 중심 차이가1px지만 렌더 기준 칸과 글자 시작 높이는 같다. PNG116장, client138·protocol124 실패/오류/건너뜀0이며 변경 없는 protocol 결과는 재사용했다. 실행 중 소스·빌드361개와 지정 입력93개 해시는 불변이다. 재생성 입력의 제독이 달라져 초상·이름·능력치·특성 내용은 이전 캡처와 다르며 이 차이를 배치 변경으로 세지 않는다. 제독 보정 뒤 시작한 필터 없는 전체 실행은 새 머리줄 요청 시점에 종료 기록 없이 중단되어 통과로 세지 않는다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이다.

이번 군 사단 수 1GUI 하향 수정은 기존 리소스팩 ZIP과 r12 서버 사본을 재사용한 `:client:test --tests dev.hoi.client.map.commander.CommanderGroupsTest runGameTest -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`으로 7m50s에 통과했다. 실행 전 `HOI_ARMY_REVIEW_PREVIEW`는 같은 r12 사본의 `fabric/build/army-review-fixtures`로 지정한다. 관련 클라이언트 JUnit4개와 입력 생산자49개는 실패·오류·건너뜀0이며 실제 PNG359장을 생성했다. GUI2 2560×1440/1280×720 모두 19/18의 글자 픽셀252개가 y85~98→87~100으로 정확히2px 내려갔고, 집단군3/5와 나머지 머리줄 내부 픽셀은 유지됐다. 이전 전체 검사와 집중 검사의 월드 배경 차이는 비교 영역에서 제외했다. 리뷰 입력 경로를 지정하기 전 첫 준비 실행은 중단했고 통과로 세지 않는다. 사용자 지정 범위에 따라 팩 재빌드·전체 검증·remapJar·설치와 실제 소켓·저장/재접속·멀티·직접 청취는 이번 검사에 포함하지 않는다.

이전 보정 기록이며 현행 숫자 위치는 군 y=4GUI·집단군 y=3GUI다. 직전 검증에서는 군·집단군 모두 y=3GUI였다. 집단군 군 수/한도(예: 3/5)는 직전 y=2GUI에서 1GUI 아래로, 군 사단 수는 직전 y=6GUI에서 3GUI 위로 이동한다. 숫자의 가로 위치·크기·색과 캡슐/월계수는 유지한다. 연속된 동일 요청은 한 번만 적용한다. 선택 사단 수 도구줄, 전구 제목/본문, 특성/훈장과 HQ 좌표는 앞 검증본을 유지한다. 이 숫자 보정은 팩 빌드 후 Java25·r12 사본의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`에서8m8s 통과했다(PNG359장, JUnit122+119 실패/오류/건너뜀0). 두 해상도에서 집단군+2px·군−6px 이동을 실제 글자 픽셀 집합으로 확인한다. 이 숫자 보정까지 포함한 필터 없는 전체 `test runGameTest remapJar`는 Claude의 일정 카드13535287 회신 후 27m 41s에 통과했다(PNG2222장). 앞27m36s는 이번 보정 전 결과이며 집중8m8s와 이번 전체 결과를 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취·실제 전투 전구 툴팁과 공식 가 시각 리뷰는 별도다.

육군 선택 도구줄의 원본 unit_list_header 배경은 고정196GUI 대신 현재 패널의 안쪽 폭을 채운다. 병과 요약 때문에 늘어난 폭은 선택 수 글자 영역에 주고, 오른쪽 전략적 재배치·통폐합·수송·절반 선택·정지·배정 해제 버튼과 호버는 같은 폭만큼 이동한다. 미지정 목록도 같은 배경 폭 규칙을 사용한다. 특성/훈장은 어두운 능력치 칸에서 첫 훈장까지의 여백을 오른쪽 끝에도 확보하고 기존 크기·서버 순서·넘침 겹침/호버를 유지한다. 마지막 사용자 정정에 따라 선택 사단 수 글자만 기존 위치보다2GUI 아래·1GUI 왼쪽으로 옮긴다(앞1GUI와 누적하지 않는다). 전구 이름은 가로 위치를 유지하고3GUI 위로 옮겨 기존 이름 칸 안에 맞춘다. 배경·버튼에는 이 글자 오프셋을 적용하지 않는다. 팩 빌드와 Java25 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 최종 좌표를 포함해5m40s 통과했다(PNG359장). 두 해상도 픽셀 대조에서 선택 수−2px/+4px, 전구 제목0px/−6px와 글자 크기 유지를 확인했다. 앞8m2s 집중은 최종 글자 좌표 적용 전 중간 결과다. 이 추가 수정은 Claude의 서버 검증 종료·일정 회신 후 r12 사본과 수동 입력을 포함한 필터 없는 전체 `test runGameTest remapJar`에서 27m 36s에 통과했다(PNG2222장). 이전 전체27m14s 결과와 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취 및 실제 전투 전구 툴팁은 미검증이며 공식 가 시각 리뷰는 별도다.

이전 보정 기록이며 현행 숫자 위치는 군 y=4GUI·집단군 y=3GUI다. 사용자 추가 보정으로 집단군 머리줄 군 수/한도(예: 3/5) 글자를 y=3→2GUI로 1GUI 더 올린다. 오른쪽 전구 본문의 집단군·군 배지, 선택 강조, 숫자, 경고 아이콘과 클릭/호버 영역은 1GUI 내리고 전구 전체 사단 수도 함께 내린다. 전구 머리줄·설정 창·본문 배경과 HQ·특성·▼ 좌표는 유지한다. 같은 문구로 반복된 요청은 한 번만 적용한다. 이 보정은 팩 빌드 후 Java25·r12 사본의 육군 집중 test runGameTest remapJar -PhoiTestSuite=army-layout에서 8m 24s에 통과했다(PNG359장, JUnit122+119개 실패·오류·건너뜀0). 두 해상도 픽셀 대조에서 3/5 −2px, 전구 집단군·군 숫자 +2px와 글자 크기 유지를 확인했다. 기존 좌우/반복 입력·선택·스크롤·툴팁 검사도 통과했다. 첫 전체 검증은 26m9s에 이전 커서의 사단 툴팁이 1280×720 경험치 막대 픽셀을 가려 실패했다(PNG2000장). CommanderGroupsRenderTest의 검사 전에 커서를 빈 공간으로 옮기고 두 틱 기다리도록 준비만 수정했으며 막대 좌표·색상 판정은 유지했다. 수정 후 육군 집중7m57s와 필터 없는 전체27m 14s가 통과했다(PNG2222장). 사용자 승인 슬롯을 사용했고 실패 로그·메타데이터·화면도 보존했다. 이전 y=3GUI 전체26m57s 결과와 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취와 공식 가 시각 리뷰는 별도다.

이전 보정 기록이며 현행 숫자 위치는 군 y=4GUI·집단군 y=3GUI다. 직전 머리줄은 군/집단군 수 캡슐의 높이를 유지하면서 원본 가로 폭을 복원하고, 군 소속 집단군 번호의 월계수에 집단군과 같은 금색을 쓴다. 군·집단군 카드 오른쪽 끝을 맞추며, 집단군 3/5 글자는 직전 위치보다 추가1GUI 위(y=3GUI)에 둔다. 특성/호버 높이와 ▼ 중앙 정렬은 유지한다. 새 r12 서버 사본의 기본 군 색(빨강→청록→보라)을 사용한다. 이 색 배정은 서버의 근사 규칙이며 원본 엔진 규칙을 확정한 결과가 아니다. 이 수정은 팩 빌드 후 Java25·r12 사본의 육군 집중 8m 44s, 필터 없는 전체 26m 57s에 통과했다(PNG2222장). 새 사본 표시 생산자도 통과했으며 변경 없는 성공 결과는 재사용했다. 전체 검증은 지정된 서버 verify-1018j/full.log의 exit=0 확인 뒤 진행했다. 실제 소켓·저장/재접속·멀티·직접 청취는 이 렌더 검사에 포함하지 않으며 공식 가 시각 리뷰는 별도다.

육군 군·집단군 패널의 폭은 현재 스냅샷의 두 역할에 필요한 병과 요약 폭 중 최댓값을 공유한다. 추가 목록 폭은 이름 열에만 배분한다. 특성 훈장과 호버 영역은 최신 사용자 지시에 따라 직전 위치에서 추가로 2GUI 위로 이동한다(머리줄 기준 y=21GUI). 군 사단 수는 마지막 사용자 요청대로 y=4GUI, 집단군 군 수/한도는 y=3GUI에 둔다. 해당 배경·월계수와 숫자의 가로 위치는 유지한다. ▼는 각 열의 좌우 중앙에 둔다. 목록 배경은 원본 `GFX_tiled_window_1b_thin_border`의 `borderSize=64`와 타일링을 사용하며, 작은 테두리로 중앙의 오른쪽 장식까지 늘리지 않는다. 조직력/내구도 첫 칸과 별도 경고 칸을 포함한 9열은 유지한다. 지역 사령부의 `unit_hud/headquarters` 표시에는 TFR `GFX_unit_bus_icon_medium`의 첫 76×42 프레임(초록 별·무전기)을 사용하고, 사단 행과 병과 요약이 같은 그림을 공유한다. 실제 HQ는 군 전용 배치 줄에만 표시하며 지역 사령부 일반 사단은 목록에서 제거하지 않는다.

### 중점 상세 원본 화면 대조

진행 중인 중점의 상세 창은 기존 시작 단추 자리에서 `CANCEL_FOCUS`의 `취소`를 표시하고 서버 `cancelable`과 요청 대기 상태로 활성 여부를 결정한다. 취소 요청에는 선택한 중점 ID와 기존 token/session/revision을 보낸다. 별도의 화면 왼쪽 아래 시작·취소 단추와 같은 이름의 성공 안내는 표시하지 않는다. 다른 실패 안내는 유지한다. 이벤트 창과 중점 화면을 함께 그릴 때 중점의 불투명 바탕은 공통 메뉴 아래에서 시작한다. `FocusScreenTest`는 단독/이벤트 스택의 상단 메뉴 픽셀과 상세 취소의 중복 요청 차단을 검사한다.

팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=focus-detail -x :client:prepareRendererFixtures -PhoiServerSource=<고정 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`으로 GUI2 두 해상도의 실제 렌더·입력 검사를 통과했다. 이 묶음은 자동 서버 입력 생성 대상이 없으며 번들 원본 트리와 명시적 합성 진행 상태를 사용했다. 단독/스택 비교는 상단 통계와 메뉴 도크를 대상으로 하며 투명한 배경 하늘은 제외한다. 무필터 전체·실제 접속·직접 음향 청취 검증은 아직 수행하지 않았다. 조건 분기와 동적 문구의 해석은 서버 상세 입력의 책임이다. 현재 고정 입력에 남은 `if/else_if` 안내를 클라이언트 문자열 필터로 숨기지 않는다.

중점 상세 창은 TFR `interface/nationalfocusview.gui`의 `national_focus_detail_view`와 사용자 Steam 캡처 `20261014201913_1.jpg`부터 `20261014201945_1.jpg`까지 16장을 기준으로 한다. 기본 위치 (250,50), 크기 275×383은 원본 좌표의 절반이며 GUI2에서 화면 안으로 제한한다. 작은 화면에서는 설명·효과 높이를 줄이고 각 영역의 독립 스크롤로 전체 문장을 읽는다. 제목·기간·시작·효과 글자에 자동 축소를 적용하지 않으며 Minecraft 기본 글꼴과 현재 크기를 유지한다. 중점 그림은 원본 170×150 비율의 85×75 영역에 표시한다.

배경·상단·조건 칸·효과 제목·진행 게이지·체크/X·스크롤·시작/닫기 단추는 기존 외부 팩의 원본 자산을 재사용한다. 조건·설명·효과에는 서버의 문구·순서·색·정밀도·빈 줄을 유지한다. 조건의 `£trigger_` 토큰이 이미 있으면 별도 평가 아이콘을 중복하지 않는다. 원본 단추의 활성/비활성/마우스 상태는 기존 `UiDiplomacyAssets`의 buttonstate 처리 경로를 사용한다. 시작 단추는 클라이언트에서 소리를 내지 않고 서버가 시작 성공 때 보내는 `FOCUS_SELECT` 신호로 `ui.focus.select`를 한 번만 재생한다. `중점 취소`는 `ui.focus.select`, 닫기는 `ui.close`를 재생한다. 호버로 받은 상세가 있으면 선택할 때 그대로 쓰고, 선택한 중점의 상세가 오지 않으면 40틱 뒤 다시 요청한다. 상세가 오기 전의 기간 칸은 `0` 대신 빈칸이다. 스크롤의 화살표·트랙·손잡이는 왼쪽 클릭/드래그로 조작하고 해제·선택 변경·화면 종료 시 드래그를 끝낸다. 완료 표시는 서버 Node의 `완료` 상태를 따른다. Detail의 `complete`는 상세 문구의 해석 완전성을 뜻하며 중점 완료 여부로 쓰지 않는다. 고정 표시의 원본 키는 `FOCUS_STATUS_COMPLETED`·`FOCUS_REWARD`·`START_FOCUS`·`CLOSE`이며 HOI4 `localisation/korean/core_l_korean.yml`의 완료됨·효과·시작·닫기를 사용한다. 설치 TFR 한국어 패치 3411756715에는 이 네 키의 덮어쓰기가 없다.

팩 빌드 후 집중 재현 명령은 Java 25의 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=focus-detail --console=plain --no-daemon`이다. `FocusScreenTest`는 인증·같은 리비전 반복 시작 차단·늦은 응답·권한 회수·창 이동·전체 문구·게이지/종이의 실제 GPU 픽셀을 검사한다. `FocusScreenPixelsTest`는 현재 생산 경로 `FocusDetail.progress(graphics, detail, completed)`를 호출하며 진행률 픽셀 비교에서 `completed=false`를 전달한다. 삭제한 두 인수 래퍼를 되살리지 않으며 원본 oracle과 픽셀 허용 오차는 유지한다. `FocusDetailTest`는 GUI2 2560×1440/1280×720에서 독립 휠·화살표·트랙·손잡이·해제와 화면 경계를 검사한다. `tree-only` 캡처는 보관된 KOR View의 해당 중점 선택이며 서버 Detail이 없음을 확인한다. `synthetic-scroll`은 합성 입력의 렌더·스크롤 검사이며 원작 장면의 증거가 아니다.

상세 장면은 서버 `FocusDetailFixturesTest`의 `kor-start-view.json`/`kor-start-details.json`(시작 중점 21개)과 `kor-oil-shock-view.json`/`kor-oil-shock-details.json`(오일 쇼크 이후 41개)을 사용한다. 집중 검사 전에 `$env:HOI_FOCUS_PREVIEW='<서버 export 사본 폴더>'`를 설정하면 `FocusDetailTest`가 원본 20261028003540_1~20261028003600_1의 15개 중점과 기존 16개 장면을 두 해상도로 렌더하며 누락된 ID는 실패로 처리한다. `cmp50-server/focus-ui`의 권위 입력으로 62개 장면 렌더와 상세 원문 보존 검사를 통과했다. `201923`은 효과를 스크롤한 상태다. 입력을 지정하지 않은 기본 검사는 기존 tree-only·합성 회귀를 유지한다. 중립 외교 노선(`201927`)은 `KOR_follow_up_measures_fake`다. View의 짧은 이름과 Detail의 원본 이름이 다를 수 있으므로 ID로 연결한다.

조건의 체크/X는 서버의 명시적 `£trigger_yes`/`£trigger_no` 토큰만 표시한다. `assessment`만으로 아이콘을 추가하면 원본의 선행 중점 조건과 다른 표시가 된다. 서버가 보낸 미지원 효과·동적 변수·제한 문구를 숨기거나 원본 문장으로 대체하지 않는다. 코로나 대응책 설명의 문단 구분, 완화 대응책의 조건/효과 문구, 완료 중점의 조건과 경제 효과의 미지원 표시는 서버 입력의 대조 과제이며 클라이언트에서 만들지 않는다.

전체 검사의 `prepareRendererFixtures`는 기본적으로 서버 체크아웃에서 중첩 Gradle을 실행한다. 서버 검증과 겹쳐 실행을 금지한 세션에서는 담당자가 전체 fixture를 먼저 export하고, export에 사용한 소스를 고정 사본으로 보존한다. 클라이언트에서 `-x :client:prepareRendererFixtures`로 producer를 제외하고 `-PhoiServerSource=<export에 사용한 고정 사본>`을 지정한다. `verifyRendererFixtures`의 소스·입력 해시와 공유 프로토콜 검증은 유지하며, provenance를 바꿔서 통과시키지 않는다. 서버 담당자의 명시적인 검증 종료 회신을 받은 뒤 클라이언트 전체 검증을 시작한다. 경과 시간이나 실행 중인 프로세스가 없다는 사실만으로 슬롯을 사용하지 않는다.

현재 중점 상세 입력과 전체 fixture의 재현 명령은 다음과 같다. 이 명령은 `C:/HOI-client`에서만 실행하며 `C:/HOI`의 Gradle을 시작하지 않는다.

```powershell
$env:HOI_FOCUS_PREVIEW='C:/Users/Kiruy/AppData/Local/claude-hoi/cmp24/focus-ui-20261015'
$env:HOI_COMMANDER_PREVIEW='C:/HOI/fabric/build/commander-source-fixtures'
$env:HOI_OFFICER_PREVIEW='C:/HOI/fabric/build/run/gameTest-runs/2026-10-15T19-43-52.799015900Z-b55a2090-afcf-455d-a953-1b134d8aa45e/officer-source-fixtures'
$env:HOI_LAND_PLAN_PREVIEW='C:/HOI/fabric/build/land-plan-overlay.json'
$env:HOI_MISSION_OVERLAY_FIXTURES='C:/HOI/fabric/build/mission-overlay-source-fixtures'
.\gradlew.bat :client:verifyRendererFixtures -x :client:prepareRendererFixtures -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp24/server-src-20261016a --console=plain --no-daemon
.\gradlew.bat test runGameTest remapJar -x :client:prepareRendererFixtures -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp24/server-src-20261016a --console=plain --no-daemon
```

이 검사는 운영 서버 접속·실제 시작 효과·재접속·다중 플레이어 동기화·장치에서의 직접 청취를 증명하지 않는다. 운영 서버는 실행하지 않는다.

원본 16장에 대응하는 두 해상도 실제 캡처의 시각 검토는 남은 차이 없음으로 회신되었다. 이전 기본 서버 경로의 사전검사는 13초에 `PoliticsCatalog.java`의 오래된 출처 해시로 실패했으며, 서버 담당자가 `server-src-20261016a`에서 필수 9개 그룹을 재export했다. 서버 검증 종료 회신 뒤 위 두 명령을 순차 실행해 사전검사는 12초, 필터 없는 전체 검사는 16분50초에 종료코드0으로 통과했다. 중첩 서버 producer는 실행하지 않았고, 소스725개씩·입력92개·공유 protocol Java48개의 검증은 유지했다. 수동 입력은 성공한 서버 실행의 지휘관54개·장교단17개·임무3개·육군 계획1개이며 경로와 파일별 SHA-256을 검증 증거에 보존했다.

전체 실행 ID는 `2026-10-15T21-00-41.052385600Z-4e46dd07-d273-4b25-ac78-8deee8df5da9`다. JUnit은 클라이언트117개·protocol119개(실패·오류·건너뜀0)의 기존 성공 결과를 UP-TO-DATE로 재사용했고 실제 렌더링을 새로 실행했다. 실제 GPU PNG2007장과 별도 CPU 기대 PNG32장을 생성했으며 원본16장×GUI2 두 해상도의 새 실제 캡처32장이 포함된다. 중점 게이지·종이 GPU 비교는 해상도별18개씩 실패0이며 원작 framebuffer 전체의 픽셀 동일성을 뜻하지 않는다. 실행 중 채취한 클라이언트 소스·빌드 정의434개는 종료 시점과 해시가 같았고, 런타임에 복사한 외부 팩6개의 크기·SHA-1도 manifest와 일치했다. client 전용 JAR은 SHA-256 `f373850eb862f4d14a343f051fa00fe89c8fcfc323c549dc37411dfacb08a6c3`이며 내장 이미지·음원은 허용된 `hoi-client-icon.png` 하나다. 서버 문구 차이와 미지원 효과의 보완은 별도 서버 작업이며, 이 화면은 전송된 줄을 그대로 그린다. 전체2039장의 직접 열람·원작 모든 장면의 픽셀 동등성·운영 멀티플레이·실제 소켓 재접속·장치 청취는 확인하지 않았다.

### 결정 개인 설정과 지도 요청

사건 머리의 "중요하지 않은 알림" 체크 상자는 글자가 잘리지 않도록 폭 90GUI를 쓰고, 사건 팝업·뉴스 팝업 체크 상자는 x152부터 같은 오른쪽 끝까지 둔다. 체크 칸과 라벨은 사용자 지정대로 체크 칸을 1GUI 아래로, 라벨을 1GUI 위로 옮겨 맞춘다. 사건 팝업·뉴스 팝업은 칸 위쪽과 라벨 줄 위쪽이 모두 영역 y+1이고, 중요하지 않은 알림은 같은 관계로 26GUI 영역의 라벨 줄(가운데에서 1GUI 위)에 칸을 맞춘다. 같은 규칙으로 점령지 "저항하지 않는 국가 보기"는 칸 원본 y161·글자 y167, 첩보 후보 "임무 재개"는 칸 top+280·글자 top+281이다. 작전 "자동 반복"은 "자동 시작"과 칸 위치를 같이 쓰고 "자동 시작"이 이미 가운데이므로 칸은 두고 글자만 1GUI 올린다(y+h−20). `DecisionMarqueeTest`는 두 해상도에서 세 체크 표시와 라벨 글자의 세로 가운데 차이가 1.5px 이하인지 촬영 화소로 확인한다.

`config/hoi-decisions.properties`는 클라이언트 개인 설정이다. 추적 점은 접속 서버 주소(통합 서버는 월드 경로)·나라 TAG·기본 결정 ID로 저장한다. `@c:`/`@s:` 접미사는 빼므로 같은 결정의 대상 행은 설정을 공유한다. 기본은 켜짐이며 범주 점은 그 범주의 결정을 함께 전환한다. 세션·리비전·재접속으로 초기화하지 않는다. 임시 파일 뒤 atomic move로 저장하며 실패 시 원본을 보존하고 로그를 남긴다. 상단 결정 경보 feed는 미구현이므로 점은 개인 설정이며 경보 효과를 주장하지 않는다.

팝업 기본은 작은 사건 꺼짐, 사건·뉴스 켜짐이다. 이는 원본 캡처 1842·1847·1851·1856 관측이며 엔진 기본값 확인과 다르다. `POPUP_MINOR_EVENTS`·`POPUP_EVENTS`·`POPUP_NEWS`와 세 `*_POPUP_CHECKBOX_TOOLTIP`는 설치 한글패치에 해당 키가 없어 HOI4 `localisation/korean/decisions_l_korean.yml` 원문을 유지한다. `EVENT`의 빈 presentation/`leader_event`, `GLOBAL_EVENT`, `minor_flavor`에 각각 적용한다. 꺼진 팝업은 사건 목록에 같은 서버 토큰으로 남고 클릭하면 같은 창을 연다. 서버 Close는 최소화/재개 창을 모두 지운다. 슈퍼 이벤트·연구/중점 완료·외교·주 정보에는 적용하지 않는다. 자동 선택을 보내지 않으며 사건 기한은 서버가 관리한다. 설정은 위 로컬 파일에 저장되고 국가 자산·캠페인 저장과 무관하다.

비용은 서버 `costText`의 원문 색을 유지한다. 균형 잡힌 중첩 색 코드(`§A§Y자주포§! 250대 필요§!`)는 안쪽을 닫을 때 바깥 색을 복원한다. 닫히지 않은 기존 색 코드의 덮어쓰기·줄바꿈·구분선 규칙도 유지한다. 비용만 있는 설명은 같은 글자의 서버 비용 색을 사용하며 색 코드 차이로 비용을 두 번 붙이지 않는다. 숫자 정치력 비용만 `costMet=false`이면 원본 `DECISION_COST_NOT_ENOUGH`의 부족 색을 사용한다. `enabled=false`로 자원 부족을 추정하지 않는다. ACTIVE의 `progress=GOOD/BAD`와 `remainingHours/totalHours`로 원본 진행바를 그린다. 분류나 총 기간을 모르면 막대를 만들지 않는다. 코로나 세 패널은 TFRKP `TFR_Korea_GUI.gfx`·`TFR_scripted_guis_KOR.txt`의 두 텍스처·크기·세로 방향과 서버 백분율 frame/100을 사용한다. `horizontal=no` 원본 텍스처는 가로로 저장되어 있으므로 두 레이어에 원본 shader의V축 반전과 반시계90도 변환을 함께 적용하고 아래에서 위로 채운다. 회전만 하면 백신 병·주사기와 주먹이 원작 화면에 비해 좌우 반전되므로 원본1842의 방향을 함께 대조한다. 예를 들어 저항 그림은 원본124×85에서 화면85×124가 되며 주먹은 위를 향한다. 팩의 원본 픽셀·파일·출처는 바꾸지 않는다. 설치 TFR `gfx/FX/progress.shader`의 `PixelTexture`는 텍스처 x축과 CurrentState를 비교한다. 원본20261014201842 화면으로 방향을 확인하며, 이 소스 기반 합성을 원작 런타임과의 전체 shader 픽셀 동등성 검증으로 표시하지 않는다. `DecisionPanelTest`는 GUI2 두 해상도에서 세 이미지의0/50/100% 합성 장면을 실제 렌더하고 원본의 알파 합성과 회전 좌표·채움 레이어를 실제 픽셀과 비교한다. 홀수 크기에서도 반올림한 절취 길이와 UV 끝점을 맞춰 텍스처를 압축하지 않는다. 합성 픽셀 회귀 화면은 실제 서버 캠페인 장면과 별도로 표시한다. 코로나 패널의 음수 원본 y 좌표는 설명 끝 예약 빈 줄을 사용하므로 같은 공간을 두 번 더하지 않는다. 원본 1842의 다섯 줄 라벨을 보존하고 중앙 정렬 텍스트의 실제 글자 폭으로 이 패널의 캔버스를 계산한다. 국회·일본·인물 패널의 기존 사각형과 clipping은 유지한다.

주 강조가 있는 행의 호버는 현재 token/session/revision과 전체 행 ID로 `HIGHLIGHT`를 보낸다(4틱 간격, 같은 리비전·행은 한 번). 서버 페이지를 모아 기존 지도 타일 메시로 렌더한다. token/session/revision·요청한 행·현재 차원이 맞아야 게시한다. 기본 흰색과 원본 `BORDER_COLOR_CUSTOM_HIGHLIGHTS` 표의 지정 색만 사용한다. 화면 도형 캐시는 최대 64행·전체 262,144상자이며 넘어서 퇴출된 행은 새 리비전에서 다시 받는다. 새 리비전·나라 권한 회수·화면 닫기·차원 변경은 도형과 대기 페이지를 지운다. 원본 사진에 없는 돋보기 지도 버튼과 클릭 영역은 제거한다. 호버 강조는 유지하며 클라이언트 Camera/player 위치를 변경하거나 주 정보 창을 열지 않는다.

`DecisionPresentationTest`의 `kor-scene`, `kor-scene-active`, `kor-scene-ownership-lost`, `kor-scene-mission-timeout`, `kor-scene-target-active`는 실제 캠페인·효과·take에서 생성한다. 날짜는 2020-01-13이며 원본 2020-11-01과 다르다. 국회→코로나19 대응→방산수출→제5차 국토종합계획 순서와 서버 의석·백신/감염·정치력·남은 시간을 표시한다. 미지원 `remove_effect/send_equipment` 수출 3행은 서버 목록에 없으며 클라이언트에서 추가하지 않는다. 원본 TOP SECRET 서류철의 현재 소스 키는 미확정이며 다른 메뉴 그림으로 대체하지 않는다. 대상 소멸·cancel_trigger의 실제 장면은 CMP-27 계약의 서버 경계를 유지한다. 합성 지도 상자·팝업 검사는 실제 캠페인 fixture와 구분하며 실제 다중접속·소켓 재접속·장치 청취를 증명하지 않는다. 최종 집중/전체 입력 대조에서 한국5장면의 안정도 HUD 코로나 효과2줄이 JVM별로 뒤집혔다. 효과별 수치·결정 데이터는 같고, 원시 차이와 서버 후속CMP-28을 검증 자료에 보존한다. 이 화면은 서버가 발급한 툴팁 순서를 바꾸지 않는다.

### 사건·결정 원본 화면 대조

결정 행 그림은 서버가 보낸 원본 아이콘을 `decision_assets.json`에서 찾고, 그림이 없거나 크기를 읽을 수 없으면 HOI4 기본 `GFX_decision_unknown`을 표시한다. 원본에 아이콘이 있는 결정의 누락 그림은 리소스팩에서 원본 정적 GFX를 가져와 연결하며, 원본 텍스처가 없는 정의는 기본 그림으로 남긴다.

그림이 있는 툴팁은 등록된 `textImages.width`를 GUI2 단위로 환산한 너비에 본문을 줄바꿈한다. 여러 그림이면 가장 넓은 그림을 기준으로 하며, 화면 안쪽 한계를 넘으면 비율을 유지해 축소한다. 그림 없는 툴팁은 기존 글자 너비와 최대324GUI 규칙을 유지한다. 화면 높이를 넘는 본문은 기존 휠 스크롤로 읽는다.

사이드바 바탕은 `diplomacy/dialog/panel_dark`(원본 `GFX_tiled_window2_1b_border`)를 원본 창 원점 (−6,78)부터 화면 아래 끝까지 그린다(배치는 [국가 화면 패널 배경](#국가-화면-패널-배경)). 결정 툴팁은 공통 툴팁처럼 맨 앞·뒤 빈 줄과 고정 최소 폭을 제거하며 본문용 예약 빈 줄에는 적용하지 않는다. `£GFX_kor_diffusion`은 리소스팩 `textImages`의 실제 원본 그림을 사용한다. 국회의장 이름판·정당 그림 크기/툴팁 모양은 해당 Steam 호버 장면의 대조가 남아 있으며 현재 본문 스크린샷으로 원본 일치를 주장하지 않는다.

국회의장 이름판은 TFRKP `TFRKP National Assembly l_korean.yml`의 원문 이름·색을 그대로 쓴다. `texticon_portraits` 출처의 172×258 원본 초상화 다음에 오는 들여쓴 이름 줄은 별도 본문 대신 그림 이름판 중앙(원본 y=235)에 배치한다. `textImages`의 원본 픽셀 크기는 GUI2 기준 절반 GUI로 그리며 초상화는86×129GUI다. 정당 그림도 같은 변환과 원본 비율을 유지한다. 일반 본문의 빈 줄 예약과 글꼴 크기는 바꾸지 않는다. `DecisionPanelTest`는 GUI2 두 해상도에서 이름판 안의 실제 글자 픽셀을 확인한다. Steam `20261028003518_1.jpg`~`20261028003523_1.jpg`를 국회의장·정당 호버의 크기 대조 근거로 사용한다. 그림 크기 보정은 `decisions` 집중 검사에서 두 해상도 이름판 픽셀·호버·리로드까지 통과했다. 정당 툴팁 본문 폭·그림 정렬과 국회 목록 원점은 원본과의 차이가 남아 있으며 전체 외형 일치를 뜻하지 않는다. 운영 서버 접속·멀티플레이·직접 음향 청취는 수행하지 않았다.

팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=decisions -x :client:prepareRendererFixtures -PhoiServerSource=<고정 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`으로 기존 출처 해시가 유효한 결정 입력을 검사한다. 입력이 없거나 해시가 다르면 생성 제외 옵션을 빼고 같은 고정 사본에서 먼저 생성해야 한다. GUI2 두 해상도의 실제 렌더·스크롤·입력 잠금·권한 회수 집중 검사를 통과했다. 국회 패널 검사 입력과 코로나 장면의 상태는 보드 캡처와 다르다. 필터 없는 전체 검사·실제 소켓·직접 음향 청취는 아직 수행하지 않았다.

이전 사건·결정 사이드바 검증 기준은 사용자 Steam 캡처 `20261014201842_1.jpg`, `20261014201847_1.jpg`, `20261014201851_1.jpg`, `20261014201856_1.jpg`, `20261014201859_1.jpg`(2560×1440)다. 국회·코로나19 대응, 방산수출·제5차 국토종합계획 및 선택 전후를 각각 비교하며 중점 상세 화면은 별도 작업이다. 글꼴·글자 크기를 줄이지 않고 결정 이름·비용과 분류 이름을 줄바꿈해 행 높이를 확보한다. 설명은 원문 색·공백·줄바꿈을 유지해 왼쪽에 표시한다.

제목 띠는 외부 팩의 원본 `gfx/interface/header_bg.dds`(`decision/ui/window_header`)이며, 바탕은 기존 `diplomacy/dialog/panel_dark`를 재사용한다. 결정 설명 배경은 원본 `GFX_tiled_decisions_bg_small`의 48×48·16픽셀 테두리·중앙 반복을 유지한다. `UiAssets.tiledNineSlice`는 결정에서만 사용하는 추가 경로이며 기존 `nineSlice` 호출은 바꾸지 않는다. 국회 그래프의 `progress_radial_legco.shader`는 중심 y=.982, 반지름 .5 밖을 투명하게 하므로 방사형 쿼드의 위쪽 .482를 패널의 보이는 경계에 포함하지 않는다. 원본 쿼드·도형 자체의 좌표나 프레임 값은 바꾸지 않는다.

`KOR_korea_defense_business_category_desc`는 TFRKP `localisation/korean/replace/00 TFRKP Main l_korean.yml:12897`의 `£GFX_kor_diffusion` 앞 5개·뒤 7개 빈 줄로 큰 text-icon의 공간을 확보한다. 이를 일반 본문 빈 줄과 별도 이미지 높이로 모두 더하면 배너·설명 사이에 과도한 여백이 생긴다. 앞뒤 각각 3개 이상 빈 줄이 있는 단독 큰 원본 이미지에만 이 영역을 한 이미지 블록으로 해석한다. 원문·툴팁·일반 문단의 빈 줄은 유지한다. 배너는 자기 원본 폭과 패널 비율을 넘겨 확대하지 않으며 상하 여백은 패널 550 기준 10이다. 지정 원본 `20261014201851_1.jpg`의 그림 테두리는 x=30..484, y=217..218/384..385에서 확인했다. source-kor 두 해상도 검사는 국회를 접은 실제 화면을 추가 캡처하고 분류 아래 12..49 물리픽셀 안에 배너 테두리가 나타나는지 검사해 이중 여백 회귀를 막는다. 이 접힌 국회 장면은 원본의 코로나·국토계획 상태를 재현한 입력이 아니다.

재현은 팩 빌드 후 Java 25의 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=decisions --console=plain --no-daemon`이다. `DecisionPanelTest`는 원본 서버 패널과 GUI2 두 해상도, 툴팁·재로드·미상 프레임·해제 후 텍스처 정리를 검사한다. 합성 대상 입력은 서버 원본 fixture와 구분한다. 코로나·국토계획 값, 의석수와 임무 막대의 GOOD/BAD 분류는 최신 서버 producer를 사용한다. 원본 11월 수치에 맞추려고 서버 값을 바꾸지 않는다. 필터 없는 전체 검사·실제 재접속·다중접속·장치 청취는 이 집중 검사와 별도다.

이름과 비용의 원문 폭이 기존 결정 패널에 들어가지 않으면 해당 화면만 오른쪽으로 넓힌다. 범주·본문·행 바탕도 같은 폭을 사용하며 최대 폭은 화면 오른쪽 16GUI 전까지다. 그 폭보다 긴 문장은 기존 글꼴·크기에서 줄바꿈하고 세로 스크롤로 읽는다. 원본 나라 윤곽의 끝 토큰은 등록된 기존 글리프만 두 배로 표시하고 그 폭을 이름과 비용 사이에 확보한다. 기본 행은 원본 `decision_item_bg`의 회갈색 프레임 1을 쓰고 비활성 이름에 회색을 덧씌우지 않는다. 비용의 원문 색 코드는 유지하며, 비정치력 비용의 기본 글자는 흰색이다. 실행 불가라는 사실만으로 자원 부족을 추정해 숫자를 붉게 바꾸지 않는다. 부족 수량의 빨강은 서버 `costText`에 원본 조건별 색이 있어야 한다. 마름모는 원본 40×28 비율을 유지한다.

오른쪽 스크롤바는 기존 원본 트랙·위/아래 화살표·손잡이를 재사용한다. 왼쪽 화살표 클릭은 28GUI, 트랙 클릭은 한 화면을 이동하고 손잡이 드래그는 처음부터 마지막 내용까지 이동한다. 오른쪽 클릭은 결정 요청이나 스크롤을 만들지 않는다. 버튼 해제·새 리비전 적용·화면 제거 때 드래그를 끝낸다. `DecisionScreenSourceTest`는 최신 서버 입력을 실제 렌더러에 넣어 두 해상도의 배너 여백과 스크롤바 조작을 검사한다. 후속 서버 장면·추적 점·팝업 설정·진행막대·개인 지도 계약은 [현재 연결](#결정-개인-설정과-지도-요청)을 따른다.

추적·팝업·지도 계약 연결 전 `decisions` 집중 명령은1분26초에 통과했다. 직전5분3초 실행에서 최신 서버 입력을 재생성했고 최종 실행은 같은 입력의 출처 해시를 다시 검증했다. 클라이언트 JUnit115개와 protocol118개는 실패·오류·건너뜀0이며 최종 PNG122장을 생성했다. 한국 초기2분류/5행의 패널은2560×1440·1280×720 GUI2 모두290GUI다. 국회를 접은 배너와 최장 튀르키예 방산수출 이름/비용·조건 툴팁을 두 해상도에서 실제로 촬영했다. 스크롤바 위 화살표·우클릭 무동작·끝까지 드래그·해제 후 드래그 무동작과 기존 요청 잠금·권한 회수 검사를 통과했다. staged 서버·클라이언트 JAR은 각 server/client 전용이며 nested protocol269개 클래스가 같고 정적 이미지/음원은 각 기존 모드아이콘 외에는 없다. 이 결과는 원본11월 상태의 재현·원작 글꼴 동등성·실제 소켓 재접속·다중접속·장치 청취를 증명하지 않는다.

필터 없는 전체 클라이언트 명령은23분3초에 통과했고 실제 PNG1893장을 생성했다. 최신 서버 입력 생산자45개는 실패·오류·건너뜀0이고 JUnit233개의 기존 성공 결과를 재사용했다. 최신 자동 입력과 README의 수동 fixture4경로를 사용했다. 집중/전체의16개 결정 사례(8입력×두 해상도)는 패널 폭·행/분류 수·작은 국기 행 좌표·도달 가능한 이름·원본 자산 누락 기록이 모두 같았다. 동적 배경을 포함한 전체 PNG의 픽셀 동등성이나 운영 다중접속을 검증한 결과로 해석하지 않는다. 이번 전체 명령은 클라이언트 gate이며 서버 전체 JUnit/GameTest 재검증을 대신하지 않는다.

결정 대상 행은 `DecisionPayloads.Row.target`의 서버 이름을 기존 결정 이름 뒤에 표시한다. 국가 대상 국기는 외부 팩 `ui/story_assets.json`의 해당 국가·정확한 서버 키로만 조회하고, 미등록 키/파일은 대상 국가의 기본 국기로 대체한다. 이념 접미사를 클라이언트에서 조립하지 않는다. 툴팁 본문·비용·기간·조건·불가 사유는 서버 값을 유지하며 `TAKE`에는 `decisionID@c:TAG` 또는 `decisionID@s:state` 전체 줄 ID를 그대로 보낸다. 국가 소속과 다른 열린 결정 화면은 다음 tick에 폐기한다. 새 리비전의 전체 페이지가 모이기 전에는 기존 행을 표시하되 요청을 잠근다.

`test runGameTest remapJar -PhoiTestSuite=decision-targets --console=plain --no-daemon`은 서버 producer를 생성하지 않는 합성 대상 입력 회귀 검사다. 실제 Minecraft GUI2 두 해상도에서 국가/주 대상, 정확한 국기 키·기본 국기 fallback, 좌/우클릭·드래그·반복 입력·실행음·동일 리비전 잠금·대상 소멸·ACTIVE·기한 소멸·권한 회수를 검사한다. `DecisionScreenChecks`를 통해 기존 전체/decisions 검사에도 포함한다. 합성 행 갱신은 실제 서버 임무 실행·취소·시간 경과를 증명하지 않으며 캡처 파일명에 `synthetic`을 붙인다.

현재 집중 명령은 팩 재빌드 뒤 통과했다. 클라이언트 JUnit 115개와 protocol 118개는 실패·오류·건너뜀 0이며, 공유 protocol main 48개 일치와 client 전용 JAR·정적 이미지/음원 미포함(사용자 모드 아이콘 제외)을 확인했다. 변경되지 않은 JUnit은 통과한 결과를 재사용했고 story-signals 실제 렌더를 재실행했다. 팩 Python 51개와 6 ZIP 해시/크기/한도 검증도 통과했다. 초기 합성 행 이름 중복과 원본 무음 음악의 0 등록 음량 문제를 수정한 뒤 실제 대상 렌더·원본 결정 실행음을 검증했다. 무음 음악의 원본 바이트·60초 PCM 보존은 [음악 문서](audio.md#무음-원본과-minecraft-등록-음량)를 따른다. 원본 대상 결정 장면과의 픽셀 일치나 실제 서버 대상 실행의 증거로 해석하지 않는다.

서버 `DecisionSessions.issue`는 현재 나라·주 대상 행과 대상별 이름·국기 키를 보내며, 주 대상에는 `highlightStates`로 해당 주 번호를 넣는다. 선택형 임무는 READY/TAKE, 진행 중 임무는 ACTIVE/남은 시간으로 표시한다. 클라이언트의 합성 상태 갱신 검사는 서버의 실제 시작·취소·기한·대상 소멸 시험과 구분한다. 실제 원본 서버 결정 시험은 `DecisionTargetsTest`·`SourceStateScopeTest`에 있으며, 클라이언트가 그 실행 결과를 소비하는 통합 검증과 결정 전용 개인 지도 geometry는 `MapDecisionOverlay`와 인증된 HIGHLIGHT 요청으로 연결되어 있다. 실제 운영 전송 검증과는 별개다. 국가 표·원본 음악/SFX 소비는 [서버 국가 표시 표와 원본 자산](#서버-국가-표시-표와-원본-자산)과 [음향](audio.md#서버-스토리-신호-소비)을 따른다. 필터 없는 전체 서버·클라이언트 검증은 담당 이슈에서 슬롯을 조율한 뒤 수행하며, 운영 접속·소속 변경/실제 재접속·다중접속과 사용자 장치 소리 청취는 별도 미수행이다.

HOI-client is a Java 25, Minecraft 26.3 Fabric client mod. The Gradle modules are `client` and `protocol`. Keep `dev.hoi.client` and `dev.hoi.protocol`, stable `hoi:` payload IDs, and UTF-8 Java with four-space indentation. Project-owned test classes use the `Test` suffix, including GameTest implementations; preserve their framework interfaces, registration, source sets and execution tasks.

Run `./gradlew test runGameTest remapJar --console=plain --no-daemon` before delivering code/build changes. PowerShell uses `.\gradlew.bat`. `runGameTest` invokes actual client rendering and requires a display (use Xvfb in Linux CI). The root output is only `build/libs/hoi-client-1.0.0+26.3.jar`. Never downgrade Minecraft or introduce obsolete mappings; 26.3 is unobfuscated.

The server owns research, time, country assignments, validation and persistence. This client sends bounded requests and renders private snapshots. Do not add core simulation, server worlds, Polymer or client-side rewards. Call `ResearchPayloads.registerPayloadTypes()` before registering receivers; repeated calls must be safe. Synchronize shared protocol changes with Kyarurung/HOI's `protocol` module and bump incompatible wire versions.

Preserve all eight research categories, the 2020-01-01 campaign epoch, 144 game minutes per 20 server ticks at X1 (2.4 game hours per real second at 20 TPS) and PAUSED/X1..X10 (preserve legacy save compatibility without rewriting source files). Timeline labels are independent of the campaign epoch: infantry begins in 2000; armor uses 1980, 2018, 2020, 2022, 2024, 2028, 2032. Never invent unregistered technologies, equipment or battalion combat values from reference images.

Generated builds, runs, logs, credentials and caches stay out of commits. UI fixture GameTests are actual renderer checks, not proof of multiplayer end-to-end synchronization. Record limits in README.md. Use short `feat:`, `fix:`, `build:` or `docs:` commits. The linked HOI-server repository is the operational server folder; do not start or restart it without an explicit user request.

Never register a client /hoi root: it shadows server map commands. Do not register client /선택, /준비, /시작 or /종료 roots either. Use the research menu icon; F toggles the free mouse cursor while the campaign HUD is visible; Shift+F does not open politics. Outside that campaign context, preserve vanilla offhand input. The research menu uses the authenticated OPEN request; /hoi research and /hoi-research commands are removed. ProtocolCampaignStyle provides shared Korean speed labels/colors and dark gray separators. Server /종료 resets progress/claims and the epoch; the client never performs resets locally.

All UI images come from the private HOI-resourcepack repository. Build its `scripts/build_pack.py` first; rendering tests use `../HOI-resourcepack/build/resources.zip` or `-PhoiResourcePack=<absolute path>`. Keep gameplay images and map models external. The user-provided mod-list icon is the sole image embedded in this JAR. Keep category and tabs art identical, research tabs icon-only, and preserve image aspect ratios with bounded drawing areas. Use UiPanelLayout for original TFR sidebar proportions against the 2560px reference: politics 726, trade/economy 620, logistics 560, and research/intelligence/recruitment/other menus 550. Preserve its compact minimum widths for readable controls. Use the compact left-docked original toolbar art, metal header rails and recessed content surfaces. Omit manual refresh and server-status controls while retaining automatic snapshot refresh. Keep room for at most six visible research slots, render only server-issued slots (currently at most five per country), and allocate spare vertical space to the top banner using aspect-preserving crop-to-fill. Do not show page numbers or navigation arrows; unexpected additional server slots remain accessible by scrolling. Share the navigation bar across menu and research overview/tree/detail screens; omit its country/time/speed status row. Common menu tabs are compact (20-28 GUI units wide, 34-36 units total dock height including a 14-unit indicator strip), with neutral gray selection borders and no yellow accents or visible labels; retain tooltips and accessible names. Research slot artwork stays behind centered technology art; omit saved-day text without changing banked research mechanics. Localisation keys alone do not establish a visible label or a slot-tooltip binding. Retain unknown 3D-preview and camouflage data as unavailable. Agency mutations use server-issued choices and session revisions; stale responses cannot reopen or preserve a revoked private screen.

The country flag spans both toolbar rows: approximately 40% indicators and 60% icon navigation. Order indicators as political power, stability, war support, manpower, factories/energy bar, fuel, logistics/supply-efficiency bar, convoys/transport-efficiency bar, command power, army XP, air XP, navy XP, ruling party support. Also show nuclear bombs, real GDP and debt; no achievements. World tension is fixed at the far-right edge on the DEFCON section of TFR's original `GFX_armyoverview_buttons_bg` (its metal background, screen and percentage pill), without that art's three round button sockets. Draw this display beneath fullscreen doctrine, focus and research windows; their existing top boundaries own overlapping pixels and input, while exposed HUD areas retain tooltips and clicks. Use TFR's ten original DEFCON frames, advancing every 10% (0..9% first, 90..100% last). Preserve original alpha, colours and repeated level numbers; unknown tension must not imply a level. KOR new-campaign real GDP is 1707 billion and debt 704.314 billion; read authoritative stored values, never initialize accounts in the client. Nuclear bombs appear from campaign start, regardless of research. Missing server amounts or ratios remain unknown; never fill missing bars or invent values from screenshots. Compact viewports scroll the indicator strip without shrinking/stretching icons. Optional CountryHud fields preserve legacy JSON compatibility and must stay synchronized with HOI.

## TFR/TFRKP 출처와 적용 범위

기준 대조일: **2026-09-30**. 모드·번역·국가별 우선순위와 설치본/생성 카탈로그의 해시 차이는 [HOI 공통 출처 문서](../../HOI/docs/source-imports.md)에 기록합니다. KOR는 TFRKP, PRK는 TFR 위에 TFRKP 덮어쓰기이며 다른 국가는 TFR을 사용합니다. 기본판은 상속한 공통 엔진·자산 의존성으로 구분합니다.

원본 모드 콘텐츠·이미지와 HOI의 실행/화면·운영 규칙을 구분합니다. TFRKP 공개 페이지·별도 공식 위키는 확인되지 않았으므로 최신 국가 루트나 효과를 추정하지 않습니다. 남북한 소스와 번역 키를 서로 복제하지 않으며 생성 자료를 최신화할 때 해당 importer와 provenance 검증을 따릅니다. 문서 갱신과 JAR·팩·저장 자료의 재생성·배포는 별도 절차입니다.

## Officer corps source rendering

The server issues the 47-statistic army/navy/air/special-forces grid, its exact source tooltip lines, and eight spirit slots. The client preserves source colour codes and blank-line separators; statistic values scale within their cells without clipping percent/hour suffixes. Army/navy/air backgrounds and spirit choice banners use external original artwork. Officer choice windows use two columns and the server-issued currency, candidate IDs, token and revision. A pending submission disables repeated clicks until the next authoritative revision. All six government appointment slots use two columns; law choices use one column.

`ScreenOfficerSourceTest` reads server-generated fixtures from `HOI_OFFICER_PREVIEW`, renders KOR/PRK/PRC initial and KOR selected states at GUI scale2, captures all47 statistic tooltips and all eight spirit-choice windows with candidate hovers, and tests actual left-button events, duplicate suppression and authoritative re-enabling. It also captures the source IDEA_WINDOW_SWITCH replacement prompt and selected-tile state. Identical naval/air tooltip titles are matched by their occurrence in the issued grid. Fixtures and injected request collection do not prove real network transport, two-client synchronization or socket reconnection.

Exact original state comparison still requires the PRC2040 save and active playset. The source replacement prompt and candidate percentages/precision are rendered; unsupported internal condition keys are replaced with a readable unavailable status. Preferred tactic controls issue bounded private requests and use server command-power quotes, selected-row highlighting and duplicate-click prevention. The 47 source definitions remain separate from supported runtime tactics; unsupported combat phases cannot be selected. The four officer roster controls and their dedicated routes are removed by user request. Actual army/marshal appointments and fleet organization remain available through map and unit controls. Engine formula/rounding uncertainties remain server-side evidence limits; the client must not infer values. Doctrine details/mastery, map commander appointment and branch missions follow the officer stage; see [server officer-corps boundaries](../../HOI/docs/state-affairs/officer-corps.md).

## Diplomatic justification status

The existing left relations column displays pending and usable war-goal rows with the source UI initiator/receiver icon and the other country’s flag. Hovering the row shows the server-supplied direction and remaining game days or validity. A fresh server response updates the row; selecting another country, cancellation, expiry or revoked sessions clears it. The flag retains its existing country-navigation behavior. The optional Relation.detail field is shared with HOI and accepts legacy JSON without the field. No client-side clock, war-goal creation or asset granting is added.

DiplomacyJustificationTest renders GUI2 pending, ready and outgoing cases, including saved day134/ready260 = 126 days, and checks refresh, unrelated target, cancellation/expiry and stale reconnect responses. Its synthetic renderer fixtures do not prove actual two-client multiplayer behavior; confirm the real saved goal after deploying both synchronized JARs. Existing timed_wargoal/wargoal artwork is a common HOI4 UI dependency recorded in HOI-resourcepack provenance; it supplies no vanilla gameplay rules.

## Commander views and original-role boundaries

`CommanderRoster` renders an authoritative COMMANDER dialog beside the officer panel. It sends only an issued choice/token/revision, disables repeat submission while pending, ignores older replies and unlocks after an authoritative refresh. Source portraits retain their aspect ratio. Army commander/field-officer and navy admiral/headquarters buttons use different server lists; no air general or air field marshal is introduced.

`CommanderHud` shows private army, fleet or wing cards using actual saved assignments. An empty-country server snapshot, country/dimension changes and disconnection clear the cards and cached order context; delayed snapshots alone do not expire an authorized selection. The army appointment and marshal controls use server quotes. A single bottom-aligned card row supports local paging and wheel navigation and exact selected-unit keys in CREATE_ARMY/MOVE_DIVISIONS requests; the count row opens a server-issued move quote when own map units are selected. Portrait and marshal clicks remain distinct. Unassigned cards show their count without inventing an assigned command bonus; army/marshal rows no longer overlap. Army and army-group card dragging sends the server card-order/formation contract; original visual review and actual multiplayer validation remain separate. Navy headquarters and field-officer views are still read-only until their original assignment costs/generation rules are confirmed.

`CommanderSourceTest` uses unchanged snapshots generated by `CommanderDialogSessionsTest`. Set `HOI_COMMANDER_PREVIEW` to the server `fabric/build/commander-source-fixtures` directory and `HOI_OFFICER_PREVIEW` to `fabric/build/run/gameTest-runs/<run ID>/officer-source-fixtures` for actual GUI2 appointment, selected-state and tooltip screenshots plus pending/duplicate/stale-reply clicks. Renderer tests do not establish actual socket reconnect, two-client synchronization, full tooltip parity or an original engine cost formula.

## Commander source rows and GUI2 checks

The optional `DialogView.Leader` row contains the server-owned role, skill, four abilities, capacity, current assignment and trait labels. Legacy tile JSON without it remains valid. Native rows use the TFR550x590 centered container anchor and original550x607 background,510x98 candidate frames, source ability icons and five complete49-unit rows at GUI2. Search, skill/name/ability/capacity sorting and assigned-candidate visibility only filter the current authoritative rows. They never infer abilities, change assets or invent client-side costs. The original Korean labels resolve from installed Korean source keys (unit_l:UNIT_LEADER_SEARCH/NAME_SORT/CAPACITY_SORT and core_l:SHOW_ACTIVE) where the installed patch has no replacement. Original source biographies and Army/Navy constant-effect tooltip text/icons are connected with source provenance and role-specific screenshot precision. The trait tree and remaining native command-group layout remain unfinished. Four server-owned per-ability hover breakdowns now use the corresponding original typed skill block and current level, excluding total leader skill and trait effects. The shared LEADER_SKILL_DESC key preserves original Korean wording and yellow level; source icons/order/precision follow the verified native screenshots. Optional legacy rows without abilityTooltips retain their complete candidate tooltip. Bounded display-only skill caches distinguish TFR/TFRKP, role, group and level; they never cache dynamic national conditions or gameplay assignments.

`CommanderSourceTest` initializes the actual officer backdrop before opening its server-generated appointment dialog, verifies all original frame assets and five visible rows, searches the issued name and tests pending/duplicate/old-reply behavior. `CommanderHudTest` renders160 private army cards, clicks next/previous pages, selects an actual own map counter, verifies its exact key in a MOVE_DIVISIONS request and revokes the country input. These are actual rendering/input tests with server fixtures; they do not prove socket transport, real two-client assignment or multiplayer reconnect. Use both fixture environment paths above and the latest built resourcepacks for `test runGameTest remapJar`.

The source commander renderer checks include a searched Jeon Dong-jin biography, a server-issued career-officer general-to-marshal30CP quote and one promotion request despite duplicate clicks, and searched Kang Dong-gil Navy hover. Army attack/defense use2 decimal places and supply/planning/promotion factors1; Navy coordination/hit chance use0, retreat/positioning1, damage/defense/critical chance2. These observations were read at original2560x1440, not inferred from reduced previews. Actual Army rows are gray and Navy rows blue. Server fixture click tests remain distinct from actual socket/reconnect verification.

## Doctrine detail tooltips

Private server-issued doctrine views preserve direct-unit percentage displays separately from additive battalion point displays. For example, the installed iron-wall activation is shown as +0.01 without a percent sign; complex-engineering direct rewards display +10.00%. Display formatting never determines the server combat equation. Source/save evidence and remaining formula limits are maintained in [division calculations](../../HOI/docs/warfare/division.md).

Advanced candidate details keep their original text, colors and blank lines in a tooltip bounded by the current viewport. When all rewards exceed the available height, the mouse wheel over the candidate scrolls the text; it does not select a doctrine or spend XP. Ordinary candidate tooltips retain their existing presentation. The renderer verifies the initial and final visible content at 2560x1440 and 2560x800 with GUI scale 2, the iron-wall point display, duplicate-click prevention and stale-response rejection. These fixture checks do not establish native-engine or multiplayer parity. Exact typography, mastery icon and some source category wording remain under review.

## Branch mission selectors

The navy bottom-card count opens its fleet's task forces; legacy wing cards retain AIR_MISSION requests. Current CommanderPanels mission icons send the selected unit IDs directly. Their bounded Open payloads carry only the selected unit IDs; the server checks country ownership, mission capability and valid targets. No air marshal appointment is created. Mission/target/hold actions use server-issued dialog tokens and revisions. A pending mission click remains locked after equal-revision progress updates; only a newer authoritative revision or newly issued view can replace it. Scrolling must keep every issued mission and target reachable.

The issued mission, target and ace selector dialogs have no client render test because their former fixtures were frozen demo copies; server JUnit tests cover their issued choices. CommanderHudTest checks the map entry requests and absence of naval/air marshal slots. Run the focused render with -PhoiTestSuite=missions before the full suite. Renderer fixtures are not multiplayer evidence. Existing source air-mission sprites and branch icons are reused without extra assets. The issued dialog selector retains generic rows, while current branch command panels expose the imported native mission icons. Full native selector-dialog geometry remains a visual gap.

### 육군 계획과 사단 제어

육군 하단 사단 수 영역에서 전투 계획을 열며, 지도 사단 선택 중에는 기존 군 편성 이동을 유지합니다. 장군·원수 초상화의 임명 역할은 유지합니다. 계획·배치 위치·태세·실행·중지·삭제와 소속 사단의 훈련·정지·철수는 서버 발급 선택 ID를 전송합니다. 같은 이름의 새 계획과 기존 계획은 서로 다른 ID를 가지며, 응답 대기 중 같은 revision의 화면 갱신은 중복 클릭을 허용하지 않습니다. 대상 국가·군·사단과 저장 상태는 서버가 검증합니다.

위치 선택 중 패널 밖 지도는 왼쪽 클릭으로 선택·해제하고 오른쪽 드래그로 표본 위치를 추가합니다. 임시 선 미리보기와 최대 64개의 정규화된 하향 광선을 사용하며 카메라 위치·차원이 바뀌면 드래그를 취소합니다. 서버 발급 토큰과 revision만 전달하고 클라이언트가 국가·비용·프로빈스 권한을 결정하지 않습니다. 새 요청 후 같은 revision 응답은 입력 잠금을 해제하지 않습니다. Minecraft 26.3 마우스 상수를 사용해야 하며 이전 GLFW 숫자를 직접 넣지 않습니다.

계획 사단 배정 화면은 사단별 배정·해제와 자동 배정 복귀를 제공합니다. 직접 배정 이동, 정지된 계획의 배정 유지, 군 변경과 저장 복원은 서버가 처리합니다. 원본 초기 사단 이름은 저장 ID를 바꾸지 않고 표시하며 번역 키가 없는 영문 리터럴은 보존합니다. 서버 원본 fixture를 이용한 목록·배정·입력 렌더 시험은 실제 다중 사용자 지도 시험을 대신하지 않습니다. 저장된 전선·화살표는 지도에 지속 표시하며 FRONT 모드의 단일 국경 클릭은 연결된 자기 측 국경을 서버에서 확장합니다. 원본 엔진의 군집단 전선 분할 규칙은 확인이 필요합니다.

Shift + left click on a server-issued exercise row sends the separate issued EXERCISE_UNTIL_TRAINED choice once. The server preserves that mode through naval target selection and saves it; the client does not infer completion from an XP display. Legacy snapshots without that optional choice retain ordinary clicking. Full native selector-dialog geometry remains a visual gap. Multi-region selection uses issued batch choices; it is not inferred locally.

## 군집단·함대·복수 지역 임무

군집단 원수 카드는 군 카드와 별개이며 군집단 관리 화면에서 군 선택·편입·분리·해체를 요청한다. 공군 지도에서도 실제 육군 군집단/군 지휘관 카드를 유지한다. 함대 카드의 상단은 함대 조직, 하단은 태스크 포스 임무이며 추가 버튼에서 태스크 포스를 선택해 함대를 만든다. 해군/공군의 실제 임무 아이콘은 현재 선택한 부대의 서버 검증 요청에 직접 연결된다. 별도의 중간 “임무” 버튼은 표시하지 않는다. 태스크 포스 편성·설정 및 공군 신규 비행단 진입은 유지하며 상태는 서버 확인 전 바뀌지 않는다.

군집단과 함대 관리, 여러 지역 임무 초안은 서버가 생성한 fixture로 GUI2에서 렌더하고 실제 위젯 클릭·중복 클릭 차단을 검사한다. 같은 리비전의 응답은 전송 대기 중인 선택을 다시 열지 않는다. 원본 함대의 복수 해역 자동 배분, 한 비행단의 동시 임무, 제독 임명 비용 관찰과 전체 원본 배치 일치는 이 검증에 포함되지 않는다.

지휘관 창의 세션 검증용 `configuration/` 타일 중 설명이 없는 내부 표식은 표시하지 않는다. 임무 초안의 선택 개수처럼 사용자에게 의미가 있는 설명·값은 유지하며 서버가 발급한 원본 view와 revision 검증에는 영향을 주지 않는다.

Grand-doctrine replacement candidate tooltips now preserve the installed Korean `SWITCH_GRAND_DOCTRINE_MASTERY_RESET` warning, including its original icon, colors and blank-line separator. `DoctrineScreenSourceTest` renders the fresh server-issued warning at GUI2 and verifies that inspecting and closing the choice issues no grand-doctrine selection. The user confirmed that a successful switch to a different grand doctrine resets in-progress subdoctrine mastery. Current server-issued before/after fixtures render this reset with saved bank, compatible subdoctrine selections and other-branch state preserved. Cancellation, reselecting the same grand doctrine and rejected actions retain all progress. Completed-track switching, tracks absent from the replacement, bank consumption, refunds and one-time effect replay still require specific native gameplay observations.
### 지휘관 목록 특성 표시

서버가 제공한 특성 아트·이름·개별 툴팁을 GUI2 기준 x=63/y=30, 12×16 슬롯으로 표시합니다. 장군·원수·제독의 실제 역할을 사용하고 원문 설명의 색상·내용 빈줄을 보존합니다. 원본 sprite 없는 항목과 레거시 행은 텍스트 표시를 유지합니다. 원작 특성 트리·획득/해금과 미확인 조건부 효과를 모두 구현했다는 의미는 아닙니다.

### 장교 특성 트리 창

원본 `armytraittreewindow.gui`·`navytraittreewindow.gui`·`traittreewindow_common.gui`와 직접 확인한 2560×1440 Steam 화면을 기준으로 특성 트리 창을 추가했다. 원본 1343×783 배경과 166×39 상태 프레임, 23×33 특성 아이콘의 비율을 유지한다. 트리 행 간격은 55 원본 픽셀이며 일반/지형 영역의 스크롤은 독립적이다. 보유 특성은 파란 프레임, 상호배타 관계는 원본 표시, 선행 연결선은 보유/호버 상태에 맞춰 표시한다. 구매는 서버 발급 확인창과 취소를 거치며 중복 클릭은 새 revision을 받을 때까지 잠긴다. 레거시 장교는 원본 트리 자료가 없으면 선택을 발급하지 않는다. 지휘관·제독 특성 창에서 칸보다 긴 글자(특성 이름, 왼쪽 보유 특성의 효과 줄, 선호 전술 이름)는 글자 크기를 바꾸거나 여러 줄로 감싸지 않고 한 줄 가로 스크롤(`UiText.marquee`)로 보여준다(사용자 v47 지시).

원본 명칭·효과·고문 역할 해금 이름과 툴팁 색상/문단을 사용한다. 특성 노드별 XP는 없으며 지휘관 전체 경험치 막대는 서버 `leader.gauge.fraction`과 원문 tooltip으로 별도 표시한다. 원본 폰트의 자간·커닝, 모든 조건부 문장 순서, 일부 스크롤바 및 전체 UI 픽셀 일치는 남아 있다. 역할 탭은 `advisor_role`과 발급 선택지로 연결되어 있다. 이 창은 기존 Minecraft 폰트를 사용한다. 설치본에는 OFL 고지와 별도의 이사만루 재배포 금지 고지가 함께 있으며 atlas별 글리프의 실제 서체 대응은 미확정이므로 새 원본 폰트 atlas는 복사하지 않았다.

긴 특성명과 효과 수치는 원본 크기의 칸 안에서 온전한 문장을 가로로 맞춰 표시한다. 확인·취소·돌아가기 문구는 특성 화면의 축척과 함께 줄어들며 접근성 이름과 서버 발급 입력은 유지한다. GUI2 원수/장군/제독·확인·구매 후 상태·독립 스크롤·작은 창을 실제 렌더링하고 직접 확인했다. 원본 지급형 효과 48종 중 실제 계산에 연결된 것은 20종이며 나머지 28종과 원본 글꼴의 자폭·커닝은 이 기록 당시 미완료였다. 현행 역할 탭과 경험치는 서버 표시 계약으로 연결되어 있다.

## Native officer interactions and preferred tactics

Officer statistic cells and doctrine rewards are hover-only regions: mouse clicks and keyboard focus cannot issue commands. Source chief/doctrine artwork uses equal alpha-aware visible dimensions and a shared center; generated orange progress strips are removed. The four officer roster controls are absent. Actual army/marshal assignment and common fleet trait views retain their server-issued authorization and actions.

National and army-leader preferred-tactic selectors share the native card renderer but have separate sessions and state. Original tactic pictures, attack/defense/locked/hover frames, damage, organization, width and movement icons, filters and phase captions come from installed GFX/GUI/localisation. Costs belong to the national quote. Selected and unavailable officer/tactic rows retain bounded source tooltips while selection remains disabled. Leader selection has a separate confirmation/cancel flow; only a newer authoritative revision unlocks a pending choice. Unsupported runtime tactics remain unavailable, and native font/pixel parity is not established by importing artwork.

Doctrine reward cards alternate beside native progress/threshold artwork. The client sums the authoritative per-stage requirements for cumulative labels and uses saved total mastery to fill each stage; it never advances rewards. Empty tracks remain empty. Source mastery tooltips preserve current/bank values, unit/category lists, separators and explanation lines; the unconfirmed engine monthly aggregate is shown as `?`. The selected folder uses the original front frame. Original font metrics, all source-category expansions and full native panel parity remain separate limitations.

Bottom commander HUD anchors do not depend on sidebar width. Map resistance/compliance percentages convert the packet fraction exactly once and retain up to two decimal places. Occupation labels avoid intersecting each other and are clipped to the viewport. `CommanderHudTest`, `MapOccupationHudTest` and `CommanderPreferredTacticsTest` exercise actual local rendering, sidebar transitions, several GUI scales, mode changes, filters and issued requests. Fresh source fixtures are required through `HOI_OFFICER_PREVIEW`, `HOI_DOCTRINE_PREVIEW`, `HOI_COMMANDER_PREVIEW` and `HOI_TACTIC_PREVIEW`; local fixtures do not establish multiplayer synchronization.

The bottom portrait strip uses the source `leadergroups.gui` army/group hierarchy. Current authoritative snapshots place a selected marshal and its visible subordinate armies in the same bottom strip, as shown by the supplied native screenshots. Selecting a group expands its armies. While any child army remains selected, its siblings stay visible for actual Shift-click multi-selection; clearing the selection collapses them. Skills sit above commands, followed by one bottom-aligned commander strip, with the card row at the screen bottom. A valid campaign HUD hides vanilla action messages, the hotbar and survival HUD; ordinary Minecraft screens retain them. Long strips page horizontally and consume wheel input over cards; they never grow into a second row. Both current and legacy snapshots use the same anchor independently of sidebar width. Source army/group frames retain their different aspect ratios and native inner portrait geometry. Bottom portraits do not overlay the `GFX_theatre_army_shield` / army-group shield used by `unitview.gui` detail and theatre lists. The source shield assets and valid attached-wing/task-force detail controls remain intact. Naval cards use both original `GFX_navy_group_frame` frames from `navyleaderview.gui`, with its create-fleet portrait to the left. Air maps retain the same actual army/marshal row and its army/group creation controls; there is no fabricated air-general frame. The existing fleet/task-force and air mission requests, own-unit selection and pagination are preserved. Native tactic explanations wrap and center every line within their body rectangle instead of shrinking the whole paragraph into one line. Chief captions fit their own cell; chief/doctrine alpha-aware icons retain equal 32-unit content boxes and no orange strip is drawn.

UI rendering continues to use the available Minecraft font. Installed source font notices include both redistribution restrictions and OFL fonts; they do not identify which substituted Latin/Hangul glyphs produced each game atlas. No original UI font atlas is copied by these layout changes, and exact native font metrics remain unclaimed.

Bottom commander counts fit within their footer cells, and pagination arrows/counters scale with the same reference units as the portraits. The 160-card test clicks both page directions at GUI 1/2/3 and 1280×720 and rejects any page-button/portrait overlap.


Spirit slots use original `GFX_officer_corp_spirit_bg_enabled`/`disabled` artwork with the168×70 canvas ratio. The selected background comes only from the server entry state; an intentionally empty slot stays empty. Native category frames retain their vertical offset, while selected idea artwork centers on the background. No generated green gradient or guessed affordability glow is applied. Original input hashes and PNG pixel equivalence are tracked by HOI-resourcepack's officer-spirit importer.

## Native branch command panels

Current private commander snapshots include optional group membership, leader details, own divisions/task forces, airbase groups and current mission enums. The client selects and closes these panels locally; selecting a portrait does not appoint a commander. Shift adds/removes selections, Escape clears them, and changing branch/country clears incompatible selection. Native backgrounds, button frames, mission icons and source labels come from the external resourcepack. Fleet counts and bars use the server’s effective ship designs. Air map counters resolve to their actual own wing IDs. Selected and unselected mission frames follow the issued mission enum.

The army plan icons enter private server target modes directly. Front, fallback and area defense use the map gesture; offensive and spearhead use successive front and objective gestures. Navy and air mission buttons send bounded direct commands for the actual selected own units, without target or confirmation dialogs. Air region assignment and naval region selection use map right-click. Base icon hitboxes take priority and request actual rebasing. Unsupported army ability effects and naval invasion execution report their boundary without a fake activation. Organization and policy settings retain their separate screens. Source tooltip wording, colors and blank-line separators remain unchanged.

HUD detail snapshots have a512-member and1,000,000 UTF-8 byte bound. When optional descriptions/rows exceed the budget, identity, current status, requests and total counts remain available; full army/task-force/mission lists remain reachable through the existing paged server dialogs. Rendering fixtures are local evidence and do not establish socket reconnect or two-client synchronization.

The command-panel renderer uses `kor-command-panel-army/navy/air/air-attached/air-detached.json` fixtures under `HOI_COMMANDER_PREVIEW`. Its GameTest captures GUI2 at2560×1440 and1280×720, portrait selection without appointment, exact branch mission requests, close and Shift fleet selection. Original font metrics, private-engine tooltip warning thresholds remain separate limitations. Stored wing-to-army following results are displayed from server snapshots; renderer tests do not establish their simulation behavior. Naval-invasion support currently waits at its home port with no sortie effect; escort/landing transition remains unsupported, and no shore-bombardment alias is presented as naval-invasion support.

Army and army-group portrait clicks select their complete authoritative division membership independently of displayed rows. A manual subset plus an added army selects exactly that subset and the added army, and incomplete membership temporarily suspends actionable selection without erasing explicit IDs. The command connections preserve individual division identity: row click selects one own division, Ctrl toggles it, double-click selects matching templates within the selected command, and right-click requests server-authorized camera focus. Explicit unit selections do not broaden into map stacks or whole armies. A map right-click moves only the explicit selected divisions. Army and group execute, posture, exercise and Shift until-trained issue direct private commands; normal deletion selects a single plan on the map, while bulk removal opens the authoritative scope confirmation. Selection changes cancel pending target input and clear the corresponding movement overlays.

군별 배속 비행단 칸은 공군 지도에서 실제 저장된 배속과 기체 수를 표시한다. 칸 좌클릭은 해당 비행단만 선택하고 우클릭은 `AIR_DETACH` 요청을 보낸다. 기본 지도에서는 이 띠를 숨긴다. 창설·분리·같은 설계 통합·해산·절반 선택은 화면 단추로 연결하며 D/G/S/H/K와 육해공 전용 명령 키는 등록하거나 실행하지 않는다. 버튼의 Shift/Ctrl+클릭은 해당 서버 발급 요청에만 적용한다. O 병합·원본 충원 우선순위는 다른 동작의 별칭으로 만들지 않는다.

Task-force settings project only stored optional repair policy, auto-split, reinforcement and insignia values; legacy unset values remain unset. Actual engagement enums select their matching native frames. The settings dialogs edit repair/engagement/composition and saved frame identities. The hold button requests nearest accessible port return; Ctrl+click holds at the current location. Command keys are not registered or executed. Single-task-force captions name the selected force, and closing a fleet preserves other fleets' selected subsets.

육군 계획 버튼은 선택한 실제 군의 전선·공세·선봉·후퇴선·지역방어 지도 입력으로 연결한다. 삭제 아이콘 좌클릭은 지도에서 한 계획을 지우는 모드이고, 우클릭은 선택 군의 이름·계획 수·보존되는 사단을 명시한 전체 삭제 확인을 연다. 병력 배치·결속도·편제 등 설정은 별도 설정 화면을 유지한다. 상륙 실행의 미확정 조건은 직접 명령에서 안내하며 기존 상륙 초안 편집기를 실제 출항으로 표시하지 않는다.

선택한 비행단은 육군 초상화 우클릭으로 해당 군에 부속할 수 있다. 군 카드 위의 부속 비행단 칸은 좌클릭으로 해당 비행단을 선택하고 우클릭으로 배속 해제를 요청한다. 항공집단 생성·병합 O·집단 해제와 육군 부속은 별개이며 서로 대체하지 않는다. 신규 비행단은 기지의 추가 버튼과 비행단이 없는 경우에도 보이는 신규 비행단 진입에서 실제 재고로 창설한다. 원본의 네 가지 설계 충원 선호도는 실제 저장된 정책과 국가별 현용·구형 상태를 사용해 동일 설계 보충 소비를 제한한다. 기존 충원 허용 스위치 및 낮음/중간/높음의 교체 우선순위와 구별하며, 혼합 설계 실행은 지원하지 않는다.

지휘 패널은 원본 일반 클릭·닫기·사단 선택·전투 계획·해산 효과음을 외부 팩에서 재생한다. 호버 효과음은 클릭 가능한 요소에 진입할 때 한 번 재생하며 같은 요소에서 정지하거나 매 프레임 렌더링할 때 반복하지 않는다. 사용자 Master 볼륨과 음악 상태를 변경하지 않는다.

### 확인된 원본 규칙의 제어 경로와 GUI2 경계

금색 능력 4개(force_attack, last_stand, staff_office_plan, extra_suplies)는 원본 아트·이름·설명을 유지한 직접 요청을 보낸다. 서버가 사용 조건·과금·실제 효과를 최종 검증하며 미확정 효과는 거절한다. 클라이언트는 자체적으로 지휘력을 소비하거나 능력을 활성 상태로 표시하지 않는다. 기존 조건·계수 조회 자료와 HQ 관계·지속 시간 기록을 실제 HQ 배치나 능력 효과로 표시하지 않는다.

해군 대상 팝업은 원본 9함종의 2행 배치와 서버의 실제 제외 집합을 사용한다. 체크된 항목은 교전 제외이며 구세이브의 수송선 제외 플래그도 복원한다. 임시 태스크 포스 선택 창 없이 현재 선택된 태스크 포스에 같은 함종 설정을 원자적으로 적용한다. 선택 집합과 각 항목의 기존 제외 상태를 서버에서 확인하며 하나라도 바뀌었으면 모두 거절한다. 선택 집합의 제외 상태가 섞인 경우 첫 클릭은 모두 제외하고 다음 클릭은 모두 허용한다. 이 혼합 선택 처리 규칙의 원작 엔진 일치 관측은 별도다. 혼합 함종 전투의 사격 배분을 원본과 동일하다고 표시하지 않으며 적에게 공격받는 상황을 취소하지 않는다.

원본 전역 제거 아이콘을 클릭하면 지도에서 제거할 전역을 선택하고 우클릭하면 현재 선택 부대가 속한 함대들의 전체 배정 전역을 제거한다. 대상 함대를 다시 고르는 임시 창과 전역 목록·확인 창을 거치지 않는다. 현재 발급된 태스크 포스 선택 집합과 함대별 배정 상태를 다시 검증하고 전체 작업을 한 군사 트랜잭션으로 적용한다. 배정 전역 제거는 실제 함선 위치·이동 경로·편성을 변경하지 않으며, 기존 임무의 해당 전역 기여를 중단한다. 육군 전체 계획 삭제의 사용자 요청 확인 창은 유지한다.

육해공 전용 명령 키는 등록하거나 실행하지 않는다. 초계·타격부대·통상파괴·수송선 호위·기뢰 부설·소해·상륙지원 대기, 편성·정지·배정 해제·전략 이동·결속도는 각 버튼의 마우스 요청으로 처리한다. 버튼의 Shift/Ctrl+클릭 조합과 일반 창 닫기·초안 확정은 유지한다. 실제로 지원하지 않는 원본 결속도나 상륙 지원 행동을 완료로 표시하지 않는다.

항공집단 생성·해제·병합은 실제 저장된 집단 ID와 비행단 구성원을 사용하며 육군 부속 및 기체 병합과 독립적이다. 비행단 툴팁은 실제 설계별 기체 수와 부속군 추종 결과를 표시한다. 점검 주기·내부 카운터 같은 진단 설명은 명령 툴팁에 삽입하지 않으며 실제 대기·실패 이유는 유지한다.

`CommanderPanelsTest`와 `CommanderOrdersTest`는 서버 원본 fixture로 GUI2 2560×1440·1280×720의 스킬 직접 입력·반복 클릭·오른쪽 클릭·구형 요청 전환을 검사한다. `CommanderDeterminateCommandDialogsTest`는 해역·보충 조회창의 경계와 입력을 검사하며 옛 스킬 조회창을 촬영하지 않는다. 새로운 캡처는 검증 실행마다 다시 생성해 직접 확인해야 하며, 테스트 정의나 원본 아트 수입만으로 렌더 통과·한국어 원본 글꼴 동등·실제 다중 사용자 동기화를 주장하지 않는다. 일반 UI 클릭·호버음 검사는 실제 능력 효과 적용이나 장치 청취의 증거가 아니다.

긴 읽기 전용 능력 설명은 원문·빈줄·색상을 보존한 채 화면 안에 배치한다. 목록 옆 공간을 우선 사용하여 다른 행을 가리지 않으며 행 전환 시 해당 변종의 설명으로 갱신한다. 화면보다 긴 본문에는 기존 휠 스크롤을 사용한다. 지도 지휘 패널·초상화 툴팁은 HUD 추출 뒤 즉시 그려 일반 화면의 이미 끝난 툴팁 처리에 누락되지 않게 한다. 비행단 이름은 서버의 원본 명명 규칙을 따르고, 기종별 원장은 실제 장비 이름을 표시하며 내부 설계 UUID를 사용자 문구로 대신하지 않는다. 공군 진단 설명의 실제 호버 캡처도 두 GUI2 해상도에서 확인한다.

### GUI2 카드와 지도 직접 명령

지도에서 고른 사단·함대가 있으면 카드 구성원보다 우선한다. 함대 카운터의 `<id>|navy_<종류>`는 `navy/<id>`로 정규화하며 카드만 선택했을 때는 그 카드의 구성원을 사용한다. 빈 선택·유효하지 않은 지도 명령 세션·빈 광선은 메시지 없이 입력을 무시한다. 앞 명령 응답을 기다리는 동안의 이동은 마지막 이동만 보관했다가 보낸다.

직접 이동 입력만 검사하는 `direct-movement`는 기존 명령·계층 선택·상세 선택·기본 화면 우클릭 검사를 함께 실행한다. 더 넓은 패널·임무 오버레이 회귀는 `movement-controls`를 유지한다. 집중 재현은 `test runGameTest remapJar -PhoiTestSuite=direct-movement`에 서버·protocol 고정 사본과 외부 팩 경로를 지정한다. 모의 명령 관측으로 미배정 선택·카드 밖 함대 선택, 첫 ray, 카메라 이동, 마지막 대기 명령, 동일 revision 거절을 검사한다. 첫 ray 캡처에는 검증용 전송 완료 문구를 추가하지 않는다. 첫 ray 캡처는 실제 클라이언트 렌더링과 모의 명령 입력의 증거이며 서버 이동 성공·이동선·계획 이탈·멀티플레이 증거가 아니다. 새 서버 계약 입력의 이동선 검증은 별도다.

군집단·군·함대 카드 행은 하단 액션바 위에 놓고 펼친 카드·부속 비행단·함대 선택과 팝업도 액션바 클릭 영역을 침범하지 않게 배치한다. GUI2 큰 화면과 작은 화면에서 같은 배치 규칙을 사용하며 사용자의 미선택 상태를 자동 선택으로 바꾸지 않는다. 직접 명령 안내문은 카드 상단 장식과 도구막대 제목을 포함한 전체 하단 영역 위에 놓고 팝업 클릭 영역도 피한다. 목표 입력 안내는 해당 입력 상태가 끝날 때까지 유지한다.

군·군집단 카드 클릭은 서버가 제공한 전체 구성원 ID를 사단 선택 모델에 반영한다. 접힌 하위 군이나 화면에서 생략한 행도 구성원에 포함되며, 우클릭 이동과 선택 표시는 같은 ID 집합을 사용한다. Shift 카드 선택은 군 구성원 합집합을 갱신한다. 사용자가 행·지도에서 부분 선택한 뒤에는 일반 스냅샷 갱신이나 새 사단 추가가 그 선택을 전체로 확대하지 않는다. 닫기·선택 해제·국가·군종 전환은 해당 선택과 명령 범위를 제거한다.

지도 우클릭은 서버 세션·리비전에 묶인 방향 벡터를 보낸다. 서버가 실제 시점과 지형에서 목표를 계산하고 자국 권한·범위·부대 상태를 검증한다. 일반 우클릭은 카메라가 움직이거나 명령 revision이 갱신되어도 누른 순간의 첫 방향 벡터 하나를 보낸다. LAND/·NAVAL_AREAS/ 도구의 드래그와 카메라 이동 취소는 기존 규칙을 유지한다. 응답을 기다리는 동안의 일반 우클릭은 마지막 한 건만 보관하고, 더 높은 revision을 받은 뒤 같은 선택·세션에서 한 번 보낸다. 선택·도구·국가·세계 변경과 Esc/창 닫기는 보관한 이동을 폐기한다. 서버는 수신 시점의 실제 시점에서 광선을 해석하므로 첫 클릭 월드 좌표를 저장한 것으로 간주하지 않는다. 공군 배정과 육해군 이동 화살표는 서버의 저장된 상태를 사용한다. 사단·함대의 일반 이동은 원본 `gfx/mapitems/movearrow_move.dds`, 전략적 재배치와 해군 임무·초계는 초록 `movearrow_followorders.dds`를 사용한다. 서버가 보낸 진행 구간은 밝은 색이며 선분으로 그린 단순 화살표는 사용하지 않는다. 공군 공역 배정의 목표 색상 규칙은 사용자가 명시한 노랑=항공단 임무 할당 및 지정, 초록=임무 할당 및 미지정, 회색=임무 미할당 및 지정이다. 임무 미할당 상태를 지정 여부만 보고 노랑으로 만들지 않는다. 여기서 지정은 비행단/비행장 선택이며 군 카드 소속이나 공역 배정 자체와 구분한다. 임무 미할당·미지정 조합의 표시는 이 세 조건에서 임의로 추정하지 않는다. 원본 `NGraphics.AIR_MISSION_ARROW_ACTIVE_COLOR`는 `0,1,0,0.5`, `SELECTED_COLOR`는 `1,1,0,0.8`, `NONACTIVE_COLOR`는 `1,1,1,0.2`다. 현행 `AirArrowOverlay`는 서버 SELECTED·ACTIVE·NONACTIVE를 `0xccffff00`·`0x8000ff00`·`0x33ffffff`로 그리고 원본 패턴×마스크 텍스처를 사용한다. 원본 파일을 팩에 가져온 것만으로 실제 적용 완료로 간주하지 않는다. 실제 렌더 검사와 모의 전송 검사는 운영 다중 접속·실제 소켓 재접속 검증을 대신하지 않는다.

원본 에셋은 `gfx/maparrows/maparrows.txt:986-1031`의 `air_mission_direction`(공역 임무)과 `air_transfer`(기지 이송)로 구분한다. 외부 팩 `map/air_mission`·`map/air_mission_mask`는 256×128, `map/air_transfer`·`map/air_transfer_mask`는 102×82 원본 RGBA다. `gfx/FX/maparrow.shader`의 `ArrowPixelShader`는 몸통/화살촉을 가로 절반으로 나누고 몸통 v 범위를 `body_uv_scale=0.4`로 제한하며 패턴과 RGB 마스크의 알파를 결합한다. 현행 `MapAtlasScene.AirArrow`는 kind·state·시작/끝 좌표를 전달하고 `AirArrowOverlay.build`가 임무/이송별 연속 UV 메시를 만든다. 세 색 렌더 연결과 회색 원작 사진·저장/복원 검증은 별개다. 원본 보존·재추출은 [팩 자산 문서](../../HOI-resourcepack/docs/assets.md)를 따른다.

공역 배정 화살표의 원본 비교 화면은 Steam `20261009024936_1.jpg`(선택 해제, 초록)와 `20261009024937_1.jpg`(선택, 노랑)다. `common/defines/00_graphics.lua:50-52`의 ACTIVE/NONACTIVE/SELECTED 색 정의를 함께 확인한다. 비활성 회색은 흰색에 알파 0.2를 적용한 상태이며 별도 회색 RGB를 임의로 만들지 않는다. 서버는 임무 없이 공역만 배정된 비행단을 `NONACTIVE`(회색)로 보낸다. `test runGameTest remapJar -PhoiTestSuite=mission-overlays -PhoiServerSource=<검증된 서버 사본> -PhoiResourcePack=<팩 ZIP> --console=plain --no-daemon`으로 같은 공역 배정의 노랑·초록·회색 화살표와 해제 화면을 GUI2 두 해상도에서 재현한다. 세 색 모두 서버 producer가 생성한 배정 입력이다. 실제 선택 해제의 소켓 전송·서버 상태 변화, 회색 배정의 저장·복원은 이 검사로 입증하지 않는다.

상륙 계획은 `UNITS_VIEW_BTN_DRAW_NAVAL_INV`, 군집단 선택의 전선은 `UNITS_VIEW_BTN_DRAW_DEFENCE_ARMY_GROUP`, 공군 취소는 `HOLD_WINGS` 원문을 사용한다. 상륙 계획의 현재 초안 저장 범위 안내는 원문 뒤 빈줄로 구분한다. 육군 훈련은 서버가 원본 장비 소모·국가 경험치 본문과 Shift 클릭 설명을 제공하며 색상·경험치 아이콘·빈줄을 보존한다. `CommanderPanelsTest`는 군·군집단·공군에서 실제 컨트롤을 호버하여 전달 문구와 렌더된 툴팁의 일치 및 GUI2 두 해상도의 배경 경계를 검사한다. 이 렌더 검증은 운영 캠페인 접속 검증과 별개다.

### 비행단 작전 범위

공군 지도에서 선택된 비행단의 서버 `AirRange`만 표시한다. km는 표시용이며 원의 중심·반지름은 서버의 월드 좌표와 블록 반지름을 그대로 쓴다. 같은 중심과 반지름은 한 번만 그린다. 단일 범위는 원본 장면 `20261009024944_1.jpg`의 초록, 서로 다른 여러 범위는 `20261009025946_1.jpg`의 노랑으로 표시하며 내부 지형을 밝힌다. 선택 해제·국가 권한 해제·지도 모드 전환 시 사라진다.

점선은 원본 `gfx/maparrows/air_range.dds`의 RGBA를 보존한 외부 팩 그림이다. 지형 높이에 맞춘 메시와 반투명 밝기 덮개를 사용하므로 원본 엔진 shader의 조명과 픽셀 단위로 같다는 뜻은 아니다. 서버가 범위를 주지 않은 비행단에 임의의 수치를 만들지 않는다. 범위가 닿지 않는 공역의 붉은 빗금은 미구현이며 이번 범위 원 표시와 구분한다.

집중 재현은 기존 Gradle 명령에 `-PhoiTestSuite=air-range`를 지정한다. 실제 서버 fixture의 km·반지름을 보존하고 모든 기지 좌표에 같은 오프셋만 더해 평탄한 검사 월드에서 두 GUI2 해상도로 렌더한다. 같은 기지의 중복 제거, 다른 기지의 복수 원, 선택 해제와 국가 권한 해제를 검사한다. 범위 내부와 해제 후 지형의 밝기를 비교하기 전에는 커서를 빈 모서리로 옮겨 이전 검사의 툴팁이 측정 픽셀을 가리지 않게 한다. 운영 지형·실제 멀티플레이 검증과 구분한다.

### 기본 화면 우클릭과 이동 게이지

기본 화면에서 선택한 사단·함대·비행단의 우클릭은 화면 중앙의 광선 하나를 기존 `MAP` 명령으로 보낸다. 건설 취소를 먼저 처리하며, 선택이 없거나 나라·차원·명령 세션이 맞지 않으면 기존 입력을 유지한다. F 지도 커서는 기존 누름/놓기 제스처를 유지한다. 두 경로 모두 서버 토큰·revision·선택 게시와 중복 요청 잠금을 공유한다. 응답 대기 중의 기본 화면 우클릭은 바닐라 사용으로 넘어가지 않는다. 클라이언트는 이동 성공이나 기지 변경을 미리 확정하지 않는다.

`hoi:land_plan_overlay_v5`의 `MovementQuad.arrow`는 `MOVE`에 `map/movement_arrow`, `MISSION`에 원본 `followOrdersArrow`의 `map/mission_arrow`, `RETREAT`에 원본 `movearrow_retreat.dds`의 `map/retreat_arrow`를 사용한다. 같은 지도 칸의 세 종류를 별도 메시로 묶고 서버의 몸통/화살촉 UV 및 진행 구간을 그대로 보존한다. 전략적 재배치의 조직력·속도, 후퇴 판정과 취소, 함대의 직접/임무 이동 판정은 서버 책임이다. `hoi:unit_hud_v5`의 `RETREATING`도 서버 상태 그대로 보존한다.

집중 검사에서 육군·함대·비행단의 카드/목록 및 카운터 선택 12가지(두 해상도)는 F 지도와 기본 화면 모두 MAP 한 번만 전송했다. 기본 화면 연결 전 육해군 8가지는 F 지도 1회·기본 화면 0회로 재현했다. 반복 클릭 잠금·새 revision 재시도·선택 없음·3인칭의 기존 입력 유지도 검사한다. 비행단 재배치의 선택 노랑 화살표는 실제 서버 fixture다. 선택하지 않은 비행단의 초록 재배치 화살표는 서버가 보내지만 이송 fixture에 없어 렌더 검사하지 않는다.

기지·공역 우클릭 안내는 기본 게임 `localisation/korean/air_l_korean.yml`의 `AIR_BASE_RIGHT_CLICK`·`AIR_REGION_RIGHT_CLICK` 원문이다. 설치된 TFR 한글패치·TFRKP에는 일치하는 덮어쓰기 키가 없었다. 기존 `SeaHover(sequence, ray)` → `SeaHoverTooltip(sequence, region, text)` 경로를 공군 지도와 비행단만 선택한 육군 지도에도 사용한다. 공군 선택에는 `AIR_BASE/`·`AIR_REGION/` 응답만 받아 서버 문구의 색 코드까지 기존 마우스 옆 툴팁 상자에 보존한다. 빈 대상·다른 접두어·이전 광선 또는 선택의 응답은 숨기며, 선택 해제·나라/차원/지도 전환·3인칭 전환도 이전 안내를 지운다. 기지/공역 판정은 서버 책임이며 탐지력 표와 이송·배치 불가 사유는 현재 계약에 없으므로 추가하지 않는다.

`air-range`의 `NavalAirHoverTest`는 두 GUI2 해상도에서 기본 화면/F 지도, 공군/육군 지도 비행단 선택, 기지/공역 원문과 커서 옆 경계, 늦은 응답·선택 변경·빈/잘못된 대상·선택 해제·3인칭, 기존 해역 응답을 검사한다. 공군 HUD는 서버 producer에서 만들지만 호버 응답과 육군 지도 비행단 선택은 계약을 주입한 검사다. 실제 서버 목표 판정·기지 이동 성공·멀티플레이를 증명하지 않는다. 배속 fixture 세 개는 `CommanderOrderSessionsTest.directAttachAndDetachWriteTheAttachedWingHudFixtures`가 `AIR_ATTACH`/`AIR_DETACH`로 생성한다. `armyHud` 그룹은 이 producer를 사용하며 삭제된 `FabricAirArmyAttachmentOffersTest`에 의존하지 않는다.

필터 없는 `test runGameTest remapJar`에는 기본/F 지도 우클릭, 범위 원·호버와 이동 게이지 검사도 포함된다. 전체 검사의 수동 입력 네 경로는 [전체 재현 절차](#함대-원본-배치와-구형-카드-순서)를 따르며, `HOI_MISSION_OVERLAY_FIXTURES`에는 같은 서버 검증에서 생성한 `army.json`, `navy.json`, `air-arrows.json`, `army-strategic.json`, `navy-mission.json`, `air-transfer.json` 여섯 파일이 모두 필요하다. 지휘관·장교단·육군 계획·임무 입력과 프로토콜 및 팩의 출처를 함께 고정한다. 필터 없는 렌더 검사도 실제 운영 목표 판정·이동 성공·저장/재접속·멀티플레이·직접 음향 청취를 대신하지 않는다.

집중 재현은 `test runGameTest remapJar -PhoiTestSuite=map-orders`와 `-PhoiTestSuite=mission-overlays`를 사용한다. 서버·프로토콜 사본과 외부 팩 경로는 기존 `-PhoiServerSource`, `-PhoiProtocolSource`, `-PhoiResourcePack`으로 지정한다. 전자는 실제 클라이언트의 군/함대 카드·지도 카운터 선택 및 `MapCursor`와 `Minecraft.startUseItem`을 검사한다. HUD 내용은 서버 producer에서 생성하고 카운터 위치·비율 및 해군 카운터 그림은 입력 검사용 합성값이다. 후자는 서버가 생성한 일반 이동·전략적 재배치·함대 임무·비행단 재배치 메시를 GUI2 두 해상도로 렌더한다. 모의 요청 관찰과 로컬 렌더는 운영 소켓·멀티플레이·저장/재접속 검증을 대신하지 않는다.

### 육군 명령 입력과 지역 방어

하단 훈련·공군·해군 임무는 `CommanderOrders.route`에서 기존 서버 검증 요청으로 바로 전달한다. 전선·공세선·후퇴선·지역 방어·배정·편집·개별 삭제는 `LAND/*` 입력 모드로 바로 진입한다. 실행용 중간 모달을 열지 않는다. 군 전체 삭제의 확인과 지역 방어의 하위 옵션 편집은 별도 기능으로 유지한다. 상륙 초안 편집은 실제 상륙 실행과 구별한다.

육해공 전용 키 등록과 지도 명령 단축키 실행 경로는 제거했다. Ctrl·Alt를 누르기만 해서는 배정·편집 모드를 켜지 않는다. 배정·편집 버튼으로 입력 모드를 선택하며, 일반 Esc 취소·Enter 초안 확정은 유지한다. 화면 종료·포커스 상실·선택 변경은 미확정 제스처를 폐기한다. 하단 키 안내는 표시하지 않는다.

지역 방어 군 하나를 선택하면 VP·항구·해안·요새·공군기지·국경·보급 중심지/철도 분기점 옵션을 표시한다. 필요 수는 현재 HOI의 해당 프로빈스당 한 사단 배치 규칙으로 계산하고 실제 배정 수를 함께 보여 준다. 모든 옵션을 해제한 상태도 보존하며 방어 위치가 없으면 실행을 거절한다. 옵션 없는 구세이브는 기존 모든 지정 프로빈스 방어 동작을 유지한다. 주 클릭 추가·제거는 인증된 서버 세션의 초안만 변경한다. Enter로 확정하면 기존 계획 ID·배정을 유지한 채 원자적으로 저장하며 Esc·선택 변경·화면 종료·접속 종료는 초안을 폐기한다. 비어 있는 초안은 확정할 수 없다. 지도에서 기존 영역은 회색, 변경 초안은 파란색으로 구분한다. 선택한 사단의 지역 방어 배정 해제도 군 소속과 다른 사단·계획을 유지한다.

왼쪽 사단 목록의 소속·임무·실행/중지 상태는 서버의 실제 배정 집합을 사용한다. 명령 실행 버튼의 비활성 이유는 공통 서버 검증과 연결한다. 지휘 툴팁은 원본 색상 코드와 수입된 툴팁 배경을 사용하고 본문의 빈줄을 유지한다. GUI2 테스트 클라이언트 캡처와 모의 네트워크 입력 검증은 운영 캠페인의 수동 조작·실제 다중 접속 검증과 구별한다.

계획선 선택 뒤 왼쪽 드래그는 선택한 기존 프로빈스 제어점 하나를 이동하며 나머지 순서·계획 ID·사단 배정을 유지한다. 우클릭 드래그는 기존 전체 선 대체 경로다. 잘못된 시작점·중복 제어점·오래된 계획은 서버에서 거절한다. 배정 모드는 선택 사단 전부가 소속된 군의 계획에 초록색 제어점을, 다른 군 계획에는 빨간색 선을 표시한다. 키 설정 안내와 육해공 전용 키 입력 연결은 제거했으며 명령은 화면 버튼으로 선택한다.

군 상세 패널의 원본 제거 버튼은 해체 대상·사단 보존·계획 제거·부속 비행단 처리·지휘관 해제 비용을 기존 인증 대화창으로 확인한다. 해체는 사단을 삭제하지 않으며 다른 군·집단군은 유지한다. 부속 비행단은 기존 부속 해제 규칙대로 대기 상태가 된다. GUI2 원본 비교 캡처는 설정값뿐 아니라 실제 창 배율도 2인지 검사하고 해상도를 파일명에 기록한다.

Minecraft 26.3 마우스 이벤트는 `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`를 그대로 사용한다. 지휘 패널의 사단 선택·위치 이동, 전체 명령 삭제 확인, 부속 비행단 조작과 지도 제어점 편집에서 이전 0/1 버튼 번호를 사용하지 않는다. 실제 `MapCursor.mouseClicked`를 거친 좌클릭 선택과 우클릭 위치 이동을 회귀 검사한다. 지도 명령 서버 제한은 최대 3개 연속 요청과 4틱당 1개 보충으로, 모드 승인→지도 입력과 임시 모드 취소→배정 해제의 정상 연쇄 요청을 허용하면서 중복 revision 검증을 유지한다.


`CommanderCaptivity` opens from the officer panel's captured-commander button: left click selects our missing leaders and right click selects foreign leaders held by us. `CAPTIVITY_OPEN/QUERY` uses the existing authenticated industry session and returns bounded server-side search, sorting, pages and independent country totals. Querying never changes military state. Loading, failed/retry, empty and no-search-result states differ; input focus survives refresh, stale responses are ignored and revocation closes the private screen. Long row/trait descriptions reuse the bounded scrollable officer tooltip. `CommanderCaptivityViewsTest` generates renderer fixtures from actual server transactions; `CommanderCaptivityTest` checks both GUI2 resolutions and real coordinate clicks. Captivity is distinct from unassigned or retired leaders; a separate death state and natural capture remain unimplemented.

The HQ designer uses original unit icons and server-derived base hitpoints and selected `deployed_leader_modifiers` previews. Each observed modifier keeps its own units/precision, including hours and command-capacity additions. This preview does not activate deployment effects. The user replaced HQ 3D model rendering with eleven source land adjusters: plains, forest, hills, mountain, urban, jungle, marsh, desert, fort, river and amphibious. They reuse existing equipment-designer assets and localisation. Their movement/attack/defence values come from native HQ subunit terrain blocks plus current country unit/category research and doctrine modifiers, using the existing land line-mean plus support-sum policy. HQ candidate eligibility uses actual completed unit unlocks and saved source-DLC flags; unknown or conflicting capability state is reported instead of granted. Draft add/remove respects the native four-line/four-support limit and retains at least one line battalion. The server now supplies 28 shared combat-stat fields with equipment, research, doctrine and country modifiers, and candidate deltas for add/remove/regiment conversion. The GUI2 table and hover tooltips use those values. Equipment values use imported native archetypes and default modules, not the army’s delivered stockpile or a claim of complete native-engine equivalence. Paid composition/deployment remains pending original observations. `-PhoiTestSuite=captivity` exercises both HQ and captivity; the complete renderer suite remains the release check.

서버 스냅샷 수신이 지연되어도 현재 국가·차원의 지휘 카드, 선택한 군/사단 및 명령 세션은 유지한다. 40클라이언트틱 경과는 권한 만료가 아니다. 오래된 지도 부대 카운터만 숨기며 선택 ID는 다음 권위 스냅샷과 대조한다. 빈 국가 스냅샷, 국가·차원·군종 전환, 접속 종료는 기존대로 선택과 토큰을 폐기하고, 군 해체 시 해당 카드와 구성원 참조를 정리한다. 정상 날짜 갱신은 명령 토큰을 재발급하지 않으며 서버 소유권·revision·요청 제한 검증을 유지한다.

선택 사단 상세는 `commander_selection_details_v1`의 서버 권위 페이지로 받습니다. 세션 토큰·선택 sequence·동일 generation의 32행 페이지를 모두 받은 뒤 반영하므로 카드 미리보기 밖의 사단과 미배속 사단도 현재 선택한 행만 표시합니다. 선택 상한은 2,048개이며 서버는 실제 자국 사단만 투영합니다. 늦게 도착한 이전 선택·회수된 세션 응답은 무시합니다. 단일·복수 군 선택과 행 스크롤은 갱신 동안 유지하고, 함대·비행단 지도 카운터는 임시 묶음 키를 실제 기동부대·비행단 ID로 정규화합니다. 선택 해제나 대상 소멸이 전체 함대 선택으로 확대되지 않습니다.

`CommanderSelectionDetailsTest`는 실제 지도 카운터 클릭, 사단 64개 페이지의 역순 수신, 미배속·단일·복수 선택 행, 갱신 지연 후 스크롤·군종별 상세 유지, 대상 제거와 오래된 응답 거절을 검사합니다. 실제 클라이언트 SoundManager에서 지도·카드·행의 왼쪽 선택음이 입력당 한 번이고 갱신 중에는 재생되지 않는지 확인합니다. 전체 렌더 검증의 지휘 HUD 장면은 일반 핫바가 표시되는 게임 모드를 사용해 GUI2의 스킬→명령→카드→액션키→핫바 간격을 확인합니다. 이 테스트는 개발 클라이언트의 자동 조작이며 운영 캠페인의 수동 다중 접속 검증은 별도입니다.

## 장교단·교리 본문 정렬

장교단 창(`countryofficercorpview`, 원점 −6,78)은 원본처럼 위쪽을 고정하고 `officer_core_view`(0,173)만 스크롤한다. 고정부: `GFX_advisor_theorist_entry`(팩 `officer/ui/advisor_theorist_entry`) (11,45), 최고사령부 훈장 (33.5+60i,59.5) 55×55(원작 20261011144045 실측 중심 x61·122·183, y87), 선호 전술 단추 (405,50) 101×60, 사령부 리본 (13,116) 523폭에서 자름, 육군 사령부 편제 단추 (23,121) 200×49, 배지 (223,135)와 배치 수 (243), 포획된 지휘관 단추 (306,116) 218×62. 본문 바탕은 `GFX_tiled_window2_1b_border`(팩 `diplomacy/dialog/panel_dark`)를 (0,173)부터 창 아래까지 192 타일로, 스크롤바는 원본 `right_vertical_slider` 자리(x527)에 둔다. 군종 블록 위쪽 T는 육 191·해 451·공 712·특수 973(스크롤 0 기준): 대리석 배경 (13,T) 516×254(특수 143, 테두리 6), 머리 띠 (13,T−12) 원크기, 군종 아이콘 (9,T−7)(공군 (13,T−6), 특수 (9,T−4)) 119×66, 제목 (123) 글자 중심 T+14.5, 참모총장 칸 육 (118,T+39)·해 (120,T+38)·공 (122,T+39) 61×83과 이름 (183) 110폭, 교리 틀 육 (301,T+34)·해공 (301,T+35)·특수 (401,T+45) 89×77과 아래 `GFX_bronze_number_container`(특수 짧은 판), 정신 칸 0.9배 151×63 at 육·해 x29/195/361·공 x112/278, y T+124(빈 칸 메달은 0.72배·배경 중심 (+1.8,−7.2), 채택한 정신은 0.9배·(+0.9,+2.7)), 능력치 칸 74×32 at 육 (17+72c,T+189+33r)·해공 (20+71c)·특수 (25+72c,T+67+33r) 한 줄 7개(특수 5개). 능력치 값은 최소 글자 크기(배율 1)로 그리고 넘치면 가로로 흐른다.

특수부대 제목 배경은 배너보다 먼저 그린다. 기존 TFR `gfx/interface/officer_corp/special_forces/officer_corp_special_forces_icon.dds`에서 가져온 `officer/special/banner`의 원본 픽셀·종횡비·좌표를 유지하며 제목 띠가 배너를 덮지 않게 한다. `-PhoiTestSuite=officer-comparison`은 GUI2의 2560·1280 화면에서 특수부대까지 스크롤한 실제 캡처의 금색 배너 픽셀을 검사한다. 같은 회귀 검사는 `ScreenOfficerSourceTest`를 통해 무필터 전체 검사에도 포함된다. 원본 비교 사진 `20261011144045_1.jpg`는 중국의 다른 캠페인 상태이므로 아이콘의 앞뒤 관계만 비교하며 국가 수치·인물을 맞추지 않는다. 운영 서버 접속·멀티플레이 검증은 별도다.

정신·참모 선택창 제목·부제, 국가 선호 전술 창 제목·필터 탭, 포획된 지휘관 제목·탭은 각각의 표시 칸 중앙에 배치한다. 장교단의 포획된 지휘관 수는 원본 `captured_generals_amount`(단추 기준 (45,36), `hoi_18mbs`, H색)가 놓이는 `GFX_captured_commanders_button`의 작은 원 칸 중심 (49.5,45.5)에 가운데 정렬한다. 장교단 위에 포로·사령부 목록을 열어도 배경이 그대로인지 보는 GameTest는 원본 창 그림의 투명 여백(가장자리 6px=3GUI) 너머로 비치는 월드를 제외하고 창 안쪽만 픽셀 단위로 비교한다. 정신·참모·법률 선택창의 후보 이름과 비용은 사용자가 지정한 원본 화면(Steam `20261011144045_1.jpg`~`20261011144528_1.jpg`)과 원본 GUI 정의를 따른다. 원본 2픽셀을 GUI2의 1단위로 옮긴다.

- 참모총장·최고사령부(`advisor/<슬롯>`): `command_character_view.gui`의 `command_character_view_window`. 창 500×590, 제목 y5, 부제 y30, 배너(`GFX_overview_top_strip_small` 4프레임: 육군·해군·공군·최고사령부) y45 부근에 창 폭 99%로 원본 502×59 비율을 유지해 배치, 후보 격자 시작 y114, 칸 231×74 2열. 칸 안에서 초상화 중심 (42,38), 이름 (76,10) 왼쪽 정렬, 특성 두 줄 (76,28), 비용은 오른쪽 끝 222에 맞춘 오른쪽 정렬 y47. 현재 인물은 `GFX_idea_entry_bg_2`의 둘째 프레임을 쓰고 비용을 표시하지 않는다. 정치 탭과 장교단 탭에서 같은 위치·같은 배치로 연다.
- 정신(`officer/<군종>`): `countryofficercorpview.gui`의 `spirit_window`. 제목 y2, 부제 y24, 장식 배너 y50, 후보 격자 시작 y122, 이름 (76,10) 왼쪽 정렬, 경험치 비용은 칸 오른쪽 아래.
- 법률(목록): `countrypoliticsview.gui`의 `political_ideas_window`와 `political_selectable_idea_entry_list`. 칸 462×75 1열, 이름 x84 왼쪽 정렬·세로 중앙, 비용은 x271을 중심으로 한 가운데 정렬.
- 장관(`minister/<슬롯>`): 정부수반·외무·재무·내무·정보·국방 모두 같은 창의 `political_selectable_idea_entry_grid`. 칸 231×75 2열, 이름·특성 x67, 초상화 중심 (42,38), 비용 오른쪽 끝222. Steam `20261011144059_1.jpg`는 정부수반, `20261011144101_1.jpg`는 정보부장, `20261011144102_1.jpg`는 국방장관의 실제 두 열 선택창이다. 부제는 서버가 해석한 `IDEA_WINDOW_ADD`, `IDEA_WINDOW_SWITCH`, `IDEA_WINDOW_CANNOT_SWITCH` 원문을 사용한다. 특성 요약이 비용 영역을 넘으면 글자 크기를 줄이지 않고 가로 스크롤하며 서버가 발급한 원본 효과 본문은 툴팁에 유지한다.

비용 아이콘과 숫자는 한 묶음으로 배치하고 숫자 폭을 실제 글꼴 폭으로 계산해 칸 밖으로 나가거나 이름·특성과 겹치지 않게 한다. 조건 때문에 선택할 수 없는 후보는 숫자 앞에 원본 `trigger_no` 아이콘을 두고, 비용이 부족한 후보는 숫자를 붉게 표시한다. 글자는 기본 배율(1.0)로 그리며 칸 폭을 넘는 이름만 폭에 맞춰 줄인다.

참모·법률·장관 선택창은 두 부모 탭에서 원본 show_position (710,80)을 GUI2의 (355,40)에 사용한다. 창과 안쪽 후보 판은 원본 `GFX_tiled_window_thin_border2`와 `GFX_tiled_generic_overlay_bg1_small`을 기존 nine-slice로 그린다. 정치력 비용은 바닐라 HUD 아이콘 대신 TFR `GFX_pol_power`의 보라색 `gfx/texticons/pol_power_20x20.dds`를 사용한다. 초상화는 투명 여백을 자르지 않고 원본 62×67 크기의 프레임과 종횡비를 보존한다. 제목·이름은 흰색과 굵은 스타일로 구분한다.

후보 상태는 서버 `DialogView.Tile.overlay`로만 그린다. `politics/choice/unavailable`이면 X·회색 비용·흑백 초상화/법률 아이콘을 표시한다. `politics/choice/locked`이면 X 없이 흑백 초상화/아이콘을 표시하고 비용은 충분할 때 노란색, 부족할 때 서버 `§c`에 따라 빨간색이다. 빈 overlay는 컬러이며 정치력 부족만으로 X나 흑백을 적용하지 않는다. 현재 인물과 공석의 빈 틀도 잠김이면 흑백이다. 현재 인물의 녹색 칸 배경은 유지한다. 클릭 가능 여부는 기존 서버 `choices`만 사용하며 overlay가 요청 권한을 바꾸지 않는다.

`UiAssets.drawGrayscale`는 기존 외교 화면의 `UiDiplomacyAssets.shade`를 재사용한다. 읽기 전용 원본 `gfx/FX/buttonstate.shader`의 `PixelShaderDisable`과 같은 RGB 계수(0.212671, 0.715160, 0.072169)를 적용하고 원본 크기·픽셀 수·알파를 유지한다. 큰 인물 사진을 빈 틀 위에 기울여 합성하는 기존 경로는 사진과 틀을 각각 변환한다. 동적 텍스처는 이미지별로 재사용하고 리소스 재로딩 시 해제·재생성한다. 외부 원본 자산은 계속 리소스팩에서 읽으며 팩 파일 추가·용량 증가는 없다.

원본 글꼴은 한국어 패치의 `TFR_fonts_korean.gfx`가 선택한 Neo둥근모 bitmap이다. `VCR02_20`의 행 높이는20px, `VCR02_12`는 실제14px 글꼴/행 높이15px, `VCR02_10`은10px 글꼴/행 높이11px이다. Minecraft 기본 글꼴을 GUI2에서 원본 특성·비용 크기로 줄이면 픽셀 배율이 분수가 된다. 2026-10-11 CMP-3의 보드 결정 "지금 글꼴 유지"에 따라 기본 글꼴·크기·특성 2줄·칸 배치를 유지하며 넘치는 줄은 가로 스크롤한다. 남은 차이는 원본과 다른 글자 크기/모양, 흰색 대신 회색인 특성 글씨, "육군 공…"처럼 잘린 특성이다. 원본 bitmap atlas를 외부 팩의 전용 font provider로 가져오는 대안은 사용자 승인 전에는 도입하지 않는다. 대안의 작업 범위는 세 글꼴의 단일 DDS atlas(각 2,727글리프) 변환과 출처 기록, 글리프별 간격·오프셋·외곽선 대응, 한글 누락 글리프 대체 및 배치·줄바꿈·선명도 회귀 검증이다. 원본 세 atlas는 1024×1024, 1024×2000, 1024×2200이며 단순 RGBA 저장 시 약20.4MiB다. 최종 provider의 메모리·성능과 구현 시간은 측정하지 않았다.

PRC 선택창 집중 검증은 `./gradlew.bat test runGameTest remapJar -PhoiTestSuite=politics-choice --console=plain --no-daemon`이다. `PoliticsChoiceFixturesTest`가 최신 서버의 실제 원본 후보·순서·초상화·조건·비용·상태 키와 현재 선택/정치력 부족 상태를 생성하고 기존 provenance 검증을 거쳐 렌더한다. 실제 GameTest 캡처를 `python scripts/compare_politics_choices.py --originals <Steam screenshots> --renders <run screenshots> --fixtures client/build/generated/authoritative-fixtures/politicsChoice --output build/politics-choice-comparison`에 넣으면 리사이즈 없는 1:1 비교PNG12장(정부수반·정보부장·국방장관 포함), 장관6창/참모 상태/잠긴 칸과 법률 상태 보조3장, 원본/렌더/fixture/결과 SHA-256 manifest를 만든다. 네 참모 창의 두 부모 탭 내부 픽셀 동일성과 동적 텍스처 해제·재생성 전후의 픽셀 동일성도 검사한다. 실제 렌더링 GameTest는 흑백 텍스처 전체의 무채색·크기·알파, 재로딩 시 해제, 비활성 클릭 차단·우클릭·드래그·반복 클릭·리비전 응답을 검사한다. 여섯 정부 슬롯은 실제 후보·초상화로 각각 렌더하고 잠김 상태와 두 번째 후보의 오른쪽 열 좌표를 검증한다. 외무·재무·내무장관은 제공된 원본 사진에 해당 장면이 없어 원본 사진과의 1:1 비교 대상에는 포함하지 않는다. 시험 캠페인2020 상태와 원본 스크린샷의2023 상태·현재 인물·비용·후보 구성·교체 가능 여부는 맞추지 않는다. 이 검증은 운영 접속·실제 팩 다운로드·다중 플레이어 검증을 대신하지 않는다.

2020 새 캠페인의 PRC 최고사령부는 서버의 초기 인물 숨김 처리를 반영해 후중밍·황밍·먀오화·왕하이장·쉬치량·장유샤·창딩추·왕하이·우야난·쿵쥔 순서의 10칸이다. 쑨젠궈·장메이위 칸은 없다. 마지막 네 인물은 조건 미충족 overlay에 따라 X·흑백·회색 비용으로 표시한다. 2023 원본에서 조건을 충족한 쿵쥔의 일곱째 칸과 2020의 마지막 칸 차이, 시험 fixture의 장유샤 임명 상태와 원본의 미임명 상태는 맞추지 않는다. 충분 상태의 최고사령부 fixture는 정치력2000에서 장유샤 임명비110을 차감한1890이며 부족 상태는0이다. 비교 시트의 정치력 표시는 시작값 대신 각 응답의 `body`를 확인한다. 후보 수·순서·조건은 서버 응답을 따르며 클라이언트에 고정하지 않는다. 이전 실행과 비교할 때 창 내부(720,85)-(1200,660)를 사용하고 주변 월드 배경과 창 바깥 픽셀 차이를 UI 변화로 취급하지 않는다. 집중 실행과 전체 실행의 재무장관 툴팁은 건설 속도 아이콘 오른쪽 경계(x769/y192~198) 7픽셀의 RGB 값이 달랐고 문구·수치·배치는 같았다. 경계 샘플링 차이로 보이지만 원인은 별도로 검증하지 않았으며 모든 툴팁의 완전한 픽셀 동일성을 주장하지 않는다.

선택창 자산은 외부 UI 팩에 있다. `UiAssets.draw`는 리소스가 없으면 크기(0,0)를 캐시하고 `false`를 반환해 해당 이미지를 그리지 않는다. 배너·후보 배경·현재 인물의 녹색 배경만 동시에 누락되면 실제 받은 팩에 `assets/hoi/textures/gui/politics/choice/`가 있는지와 manifest의 SHA-1을 먼저 대조한다. 과거 팩 누락과 현재 설치 상태를 구분하며 코드/JAR 변경만으로 팩 적용을 추정하지 않는다.

교리 군종 탭·대교리명·분류명·하위 교리 이름판·선택창 제목과 버튼 이름, 필요 통달치 숫자도 해당 칸 중앙에 정렬한다. 현재 통달치는 아이콘과 숫자의 표시 폭을 합친 묶음을 계수판 중앙에 배치한다. 긴 설명·목록·툴팁 본문은 왼쪽 정렬을 유지하며, 단독 `---`(세 개 이상의 `-`)와 기존 빈줄은 빈 행으로 남긴다. 장교단의 교리·참모·포획된 지휘관·육군 사령부 배지 툴팁은 서버가 보낸 원본 문구를 제목을 덧붙이지 않고 그대로 표시하며, 지정된 인물이 없는 참모 칸은 `클릭하여 … 추가` 한 줄만 표시한다. 중점 상세창의 제목·기간·조건·설명·효과 글자는 기본 배율(1.0)로 그리고 넘치는 내용은 각 영역의 스크롤로 본다. 글자 크기·GUI2 배율·클릭 영역·비활성 색과 초상화·훈장·통화 아이콘의 위치는 유지하고, 원문 색상 코드·명시된 인라인 아이콘·들여쓰기·수치 정밀도를 보존한다.

통달 단계 수치는 서버의 보상별 요구량을 누적한다. 단계 번호로 군종 팔레트를 순환하지 않는다. 숫자 글자는 기존 노란색이며 구매 비용·조건의 빨강/초록 판정과 구분한다. 현행 작은 숫자 배경판은 미완료에서 무색 frame0, 해제된 문턱에서 해당 군종 frame1~4를 사용한다. 과거의 중립 프레임 고정 설명은 현행 코드와 다르다. `doctrine_folders.txt`와 배경 DDS에 군종 팔레트가 있는 것과 엔진의 자동 달성 색 판정을 확정한 것은 구분한다. 100·200 등 숫자 글자 자체의 군종별 색 구분은 확인 필요 항목이다. 대교리·하위 교리 이름판의 군종 색을 유지한다. 보상 배경은 원본 8프레임이 군종별 달성·잠금 순서로 교차 배치되어 있으므로 육군 0/1, 해군 2/3, 공군 4/5, 특수부대 6/7에 연결한다. 기존의 군종+잠금 오프셋 4 계산으로 다른 군종 배경이 나오던 오류를 수정했다. 달성 배경은 육군 녹색·해군 청색·공군 적색·특수부대 금색이고 잠금 배경은 각 군종의 회색 프레임이다. 보상 아이콘·잠금 판정과 실제 진행은 유지한다.


### 명령 셀과 건설 상태색

육군 명령 아이콘과 클릭·툴팁 영역은 원본 `bpt_bg` 내부 각 셀을 함께 사용합니다. 훈련도 첫 셀 안에 배치하며 투명 여백을 제외한 아이콘 내용을 종횡비를 유지해 맞춥니다. 해군 임무와 공군 임무·편성 버튼은 인접 셀 사이의 간격을 보존합니다. 스킬·명령·군 카드·액션키·핫바의 기존 독립 표시 공간은 유지합니다. 육군 미선택 상태에서도 스킬 틀과 명령 줄은 남고 선택 안내를 표시합니다. 이 상태의 클릭은 군 선택이나 서버 요청을 만들지 않습니다. 실제 미선택 GUI2 캡처에는 일반 핫바를 함께 표시하며 하단 키 안내는 표시하지 않습니다.

건설 지도는 완공 기준 남은 공간이 0이면 파랑 단색, 예약 때문에 남은 공간이 0이면 파랑 사선, 그 외 예약이 있으면 노랑 사선을 사용합니다. 공유 주 슬롯과 연구 상한에도 같은 우선순위를 적용합니다. 철도 예약은 주 경계를 넘어 양 끝에 표시하며 예약 중인 실제 구간의 시작 레벨과 예약량을 사용합니다. 한 프로빈스에 여러 예약 철도가 있으면 예약 후 여유가 가장 적은 구간을 표시합니다. 철도·항구·보급 허브의 네트워크 아이콘은 유지하면서 건설 바탕색은 보급 충족률과 분리합니다. 예약 취소·완공 후 서버의 사유 지도 투영 갱신을 그대로 사용합니다. 보급 지도 자체의 색상 규칙은 유지합니다.


육군 카드와 상단 군 상세의 현재/최대 수량은 흰색으로 표시하며 지휘관이 없는 군 카드는 실제 현재 사단 수만 표시한다. 군/집단군의 일반 생성·편입은 `FORMATION` 직접 요청을 사용한다. 기존 군 카드 우클릭은 정확한 선택 사단 편입, 기존 집단군 우클릭은 명시적으로 선택한 군 편입, 집단군 생성 칸 좌/우클릭은 새 집단군 생성이다. 수동으로 고른 사단은 소속 군과 무관하게 왼쪽 통합 선택 목록에만 표시하며 닫기는 그 수동 선택을 해제한다. 확인이 필요한 해체·전체 삭제·유료 지휘관 배치는 기존 대화창을 유지한다. `CommanderHierarchySelectionTest`는 GUI2 두 해상도에서 실제 화면 클릭→정확한 요청 대상→선택 유지와 대화창 미개방을 검사한다. 운영 멀티플레이에서 서버 응답을 받은 검증과는 구별한다.

긴 지휘 툴팁은 화면 경계를 지키면서 가능한 경우 스킬·명령·군 카드·액션키·핫바의 하단 영역 옆에 배치한다. 양옆 공간이 부족하고 위에 들어갈 때는 위로 옮긴다. 원문과 글자 크기는 유지하며 화면 자체보다 공간이 부족하면 기존 화면 경계 제한을 우선한다. 실제 호버 렌더 검사는 옆/위 공간이 있는 경우 하단 영역과 툴팁의 비겹침도 확인한다.

### 명령 사용 가능 여부와 응답 범위

`CommanderAvailability`는 현재 선택에 대한 서버 검사 결과를 받아 명령 버튼의 비활성 표시·툴팁에 사용한다. 토큰, 명령 revision, 선택 sequence, 조회 번호와 현재 선택 사단/군 ID가 일치하는 응답만 적용한다. 지원 서버에서 아직 응답을 받지 못한 선택은 확인 중으로 표시하며, 오래된 응답으로 버튼이 다시 활성화되지 않는다. 카드별 실행 버튼은 그 카드의 정확한 군 ID 범위와 일치하는 결과만 사용한다. 다른 카드의 요청에 현재 선택 범위의 거절 이유를 전용하지 않으며, 모든 실행은 서버에서 다시 검사한다.

원본 효과 설명의 문장·색상·빈줄은 보존하고 검사 이유를 뒤에 별도 표시한다. 사용자 요청에 따라 단축키·키 설정 안내 행과 그 바로 앞 빈줄, Ctrl·Alt 키 단독 배정·편집 안내는 제거한다. 버튼·마우스 조합 안내는 유지한다. 목표 지정이 필요한 임무는 지도 대상 지정 안내를 추가한다. 삭제 버튼의 왼쪽 클릭은 단일 계획 선택 모드, 오른쪽 클릭은 선택 군 전체 계획 삭제 확인이며 비활성 이유도 따로 표시한다.

`CommanderAvailabilityTest`는 GUI2 실제 렌더러에서 비활성 버튼·해제된 Ctrl+H·Ctrl 입력의 무동작, 단축키를 제외한 원문 툴팁 보존, 카드 범위 불일치, 전체 삭제 제한, 선택 해제/재선택, 오래된 응답 거절과 명령 확인 후 대기 요청 정리를 검사한다. 해당 테스트의 서버 응답은 명시적인 시험 입력이며 실제 서버 조건은 `CommanderOrderSessionsTest`와 `MilitaryBranchMissionOrdersTest`가 별도로 검사한다. 이 자동 렌더·입력 검증은 운영 서버의 다중 사용자 접속 검증을 대신하지 않는다.

### 해군 수리 우선순위 좌우 클릭

기동부대 행의 수리 정책은 알려진 현재 값에서 좌클릭 다음 단계·우클릭 이전 단계로 직접 요청한다. 툴팁도 같은 원본 다음/이전 정책 이름을 표시한다. 서버 응답 전에는 아이콘을 임의로 바꾸지 않고 연속 입력을 억제한다. 현재 값이 없는 구세이브는 기존 정책 선택 화면을 사용한다. `CommanderNavalRepairInputTest`는 GUI2 두 해상도에서 실제 클릭과 갱신을 검사하며 해당 픽스처 검증을 운영 멀티플레이로 간주하지 않는다.

`CommanderPanelsTest`와 `CommanderOrdersTest`는 GUI2에서 육해공 전용 키와 수정 키 조합이 요청·선택·지도 입력 모드를 바꾸지 않는지, 같은 기능의 마우스 버튼은 정확한 부대 범위로 실행되는지 검사한다. 하단 키 안내를 인위적으로 넣던 시험 표시도 제거했다. `UiCommandLabelsTest`는 단축키 행과 인접 빈줄 제거, 나머지 효과 문단과 마우스 조합 안내 보존을 검사한다.

집단군 카드의 펼침 상태는 선택과 분리해 국가·차원·군종 및 안정 ID로 유지한다. 집단군을 처음 선택하면 소속 군을 표시하고, 이후 명시적으로 접은 상태는 날짜 갱신이나 재선택으로 다시 열리지 않는다. 펼친 소속 군은 집단군 바로 오른쪽에 연속한 묶음으로 모인다. 큰 집단군은 페이지마다 집단군 카드를 함께 표시한다. 집단군 색상은 서버 ArmyGroup의 RGB 상태를 저장·동기화하며 색상 선택 요청에 사용한다. 카드 전체 바탕과 상시 색 띠는 표시하지 않는다. 집단군 표식 클릭으로 원본 COMMANDGROUP_PRESET_COLORS_HSV의 10색을 선택할 수 있다. 구세이브의 없는 색상은 미지정으로 보존하고 중립 회색으로 표시한다. 자동 색상 배정은 추정하지 않는다. 색상 선택 후 서버 응답 전에는 임의로 색을 바꾸지 않는다. 집단군 카드는 접기 버튼만 있고 X는 없으며, 군·사단에는 접기 버튼이 없다. 함대의 펼침 버튼은 상세 목록만 접고 선택·소속·임무를 유지한다. 군 카드 X는 기존 지휘 세션·revision으로 `FORMATION/DISSOLVE_ARMY`와 해당 armyId 하나를 즉시 전송한다. 확인창은 열지 않으며 사단은 서버가 무소속으로 보존한다. 응답 전에는 카드나 사단을 임의로 지우지 않고 반복 클릭을 잠근다. 함대 카드 X는 선택만 해제한다. 카드 제어 버튼의 우클릭은 편성 이동이나 지도 입력으로 전달하지 않는다.


`CommanderGroupsRenderTest`는 군·함대의 상세 목록과 하단 임무 대상 아이콘을 별개로 검사한다. 펼친 목록의 존재, 접은 목록의 숨김, 다시 펼친 뒤 복원, 정확한 선택 ID 보존과 하단 임무 제어의 유지를 GUI2 두 해상도에서 실제 클릭으로 확인한다. 같은 부대명 툴팁을 가진 하단 아이콘을 상세 목록 행으로 간주하지 않는다.

`UnitMovementOverlayTest`의 실제 서버 시계 진행 자료를 `CommanderMissionOverlayRenderTest`가 읽어 육군·해군 각각 시작/중간/다음 프로빈스의 화살표와 부대 카운터를 함께 촬영한다. 시작과 중간의 카운터 화면 좌표는 같고 다음 프로빈스에서 바뀌어야 한다. 게임 분·구간 진행률·프로빈스·화면 좌표를 같은 시퀀스 JSON에 기록한다. 평면 시험 월드에는 실제 경로를 평행 이동해 표시하며 이 렌더 검증을 운영 지도·실제 소켓 동기화나 원본 곡선 형태의 완전한 재현으로 간주하지 않는다. 집중 실행은 `runGameTest -PhoiTestSuite=movement-controls`이다.

### 공군 기지별 선택 패널

기지 머리 줄과 비행단 줄 전체 뒤에는 원본 `GFX_tiled_window_1b_thin_border`(테두리 64, 가운데 타일) 검은 칸을 하나 깐다. 지정 Steam 화면(20261006163945~20261006164033)처럼 줄 왼쪽 2px·위 4px·아래 약 7px 여백과 오른쪽 약 19px 여백을 두며, 칸이 들어갈 자리를 남기고 보이는 줄 수를 정한다. 칸의 빈 여백 클릭은 지도로 넘기지 않는다. `CommanderAirBasePanelsTest`는 네 해상도·배율에서 줄 오른쪽 여백이 이 검은 칸인지 촬영 화소로 확인한다.

다른 나라 비행단은 선택할 수 없고 목록에 줄로 나타내지 않는다. 사용자 지정에 따라 그 비행기는 기지 머리 줄의 수용량(전체 주둔 기체/수용량)과 기지 툴팁의 전체 기체 수에만 포함하며, 나라별 `AIRWING_COUNTRY_HIDDEN` 문구("… 측은 이 공군 기지에 약 N기의 비행기가 있습니다")는 표시하지 않는다.

공군 임무 줄은 원본 `airselectionview.gui`와 지정 Steam 화면의 공중 우세 → 근접항공지원 → 요격 순서를 사용한다. 기지 헤더는 원본 배경·국기·기지 이름·수용량·창설·닫기 배치를 연결한다. `CommanderAirBases`는 서버의 기지 ID로 묶고 `AIRBASE_CAPACITY`, `AIRBASE_AIRPLANES_KNOWN`, `AIRBASE_CAPACITY_COUNTS_DESCRIPTION` 원문에 전체 주둔 기체 수와 실제 수용량을 넣는다. 구형 응답의 누락 값은 0으로 만들지 않고 미확인으로 표시한다. 이름이 같아도 다른 기지를 합치지 않는다.

기지의 창설 버튼은 해당 기지 ID를 보내며 X는 해당 기지에서 선택한 비행단만 선택 해제한다. 다른 기지의 선택과 모든 임무·기체 상태는 유지한다. `CommanderAirBasePanelsTest`는 GUI2의 2560×1440 및 1280×720에서 서로 이름이 같은 두 기지의 독립 헤더, 선택 수와 다른 실제 수용량, 근접항공지원 버튼 요청, 기지 지정 창설 요청과 선택 해제 후 갱신을 실제 클라이언트 입력·렌더 경로로 검사한다. 수용량 호버는 자국·전체 수량의 구분, 원본 색상·문장·빈줄, 치환되지 않은 변수 없음과 실제 툴팁의 화면 경계를 검사한다. 요청 관측기를 사용하는 패널 검증은 운영 서버의 다중 사용자 접속 검증과 구분한다.

비행단 행은 원본 장비 정보 버튼, 실제 기체 수/충원 한도, 보충 정책, 부속 군, 임무, 주야간 설정과 닫기를 별도 영역으로 사용한다. 장비 이미지는 실제 서버 장비 연결을 사용하고 함재기 표시는 실제 설계에만 붙인다. 주야간 설정·보충 정책·장비 상세는 누른 비행단 하나의 ID로 요청하며 행의 X는 같은 기지의 다른 비행단까지 선택 해제하지 않는다. 구형 응답에 행 정보가 없으면 기존 행을 유지한다. 서버 확인 전에는 시간대나 정책을 임의로 바꾸지 않는다.

기지 연료 아이콘은 원본 위치와 이미지를 사용하며 툴팁은 현재 국가의 권위 연료 정보를 표시한다. 기지별 연료 탱크를 별도로 계산한 것으로 표시하지 않는다. `CommanderAirBasePanelsTest`의 실제 GUI2 입력 검사는 비행단별 시간대·정책·상세 요청과 같은 기지 및 다른 기지의 선택 보존을 포함한다. 육해공 하단 키 안내와 전용 단축키는 표시·연결하지 않으며 스킬·명령·군 카드·액션 버튼은 유지하며 전략 지도 차원의 일반 핫바는 아래 지도 HUD 규칙에 따라 숨긴다. 원본 기지 관계 아이콘의 프레임 의미, 비행단 공격 성향·에이스 조작은 확인·구현이 남아 있다.

명령·임무·스킬 툴팁은 원본 문장 뒤에 구현 해설이나 일괄 지도 입력 안내를 덧붙이지 않는다. 상시 하단 목표 지정 안내도 표시하지 않는다. 서버가 거절한 이유와 일시적인 연결·선택 오류는 유지하며 권한 검증이나 기능 제한을 숨기지 않는다. 해군 직접 배치 코어와 기존 회귀 16개가 불변 의존성 스냅샷에서 통과했고, 서버 입력·상태 회귀 18개는 2026-10-09 14:03:34 UTC 전체 코어 체크포인트로 재검증했다. 클라이언트 명령 코드 3개와 관련 GUI 회귀 소스 3개는 컴파일을 통과했다. 실제 GUI2 렌더·조작과 최신 전체 합본 gate는 별도로 수행해야 한다.

의용군 파견·귀환 수송 중인 사단은 `divisionPresent` 판정에 따라 지도 모델·카운터·지도 선택키·시야 기여에서 제외한다. 사단 자체와 군 편성·목록은 유지하며, 도착 후 ACTIVE 상태에서는 기존 소유국의 지도 선택과 시야를 복원한다. `UnitVolunteerPresenceTest`는 저장형 상태를 복원하여 이 경계와 타국 선택 거절을 검사한다. 수송 중인 상세행은 원본 `alert_volunteer_transfer_instant`의 “§H이송 중인 의용군§!”을 표시하고 대기·임무 미배정 접미문구를 붙이지 않는다. `CommanderVolunteerStatusTest`는 파견·귀환·도착 상태의 실제 HUD 응답과 프로토콜 왕복을 검사한다. 실제 지도 모델 렌더 실행은 별도 검증한다.

## 복수 장비 무기대여와 의용군 화면

`DiplomacyLease`와 `DiplomacyVolunteers`는 원본 개별 패널·버튼·아이콘·스크롤 자산으로 GUI2에서 조립합니다. 전체 스크린샷을 배경으로 사용하지 않습니다. `UiDiplomacyAssets`는 원본 64픽셀 테두리와 반복 중앙 영역, 스크롤 경계, TFR 버튼 셰이더의 명암·시간 변화를 처리하며 화면 종료·리소스 재로드 때 생성 텍스처를 해제합니다. 원본의 192 선언/190 실제 이미지 차이는 현재 실제 크기로 처리하므로 원작 엔진의 UV 해석까지 동일하다고 주장하지 않습니다. 제작국·함재기 배지·군 방패 문양과 색은 권위 있는 상태가 없으면 임의로 표시하지 않습니다.

무기대여는 서버가 발급한 장비를 최대 32종 선택하고 각 수량·일회/월별 방식을 편집합니다. 최신 PREVIEW와 동일한 내용만 SUBMIT하며, 시간 초과 후 변경 요청을 자동 재전송하지 않습니다. 원본 생산 비율·연료 항목은 규칙 미확정 사유와 함께 비활성입니다. 실제 재고·생산 수치를 사용하지만 원작의 K/M 축약과 기간 전환까지 검증한 것은 아닙니다. 의용군은 실제 군·사단·지휘관 상태를 표시하고 군 선택·국가·세션·리비전을 검증합니다. 군사 파견·귀환 연결과 한도·위협이 미확정이면 실행을 비활성으로 유지합니다. 취소·완료 복귀 시 국가 화면을 다시 조회하고 권한 회수 뒤 응답으로 화면을 다시 열지 않습니다.

`DiplomacyLeaseTest`와 `DiplomacyVolunteersTest`는 `diplomacy` 시험 묶음에 포함됩니다. 의용군 입력은 `DiplomacyVolunteerSessionsTest`가 실제 원본 부대를 재편한 `unselected`, `selected`, `multiple-selected`, `air-disabled` 4종이며 기존 부대·병력·장비가 불변임을 검사합니다. `gradle/fixture-groups.gradle`에 등록한 생산자를 실행하고 현재 소스·입력 해시를 확인하므로 수동으로 과거 JSON을 놓는 것으로 정식 검증을 대신할 수 없습니다.

현재 검증은 프로토콜 114개·클라이언트 109개 JUnit 통과, GUI 테스트 소스 컴파일, 클라이언트 전용 JAR 생성과 공유 프로토콜 46개 일치입니다. 버튼 효과·샘플링·타일링 단위 검사 8개가 포함됩니다. 실제 GUI2 캡처·원작 픽셀 대조·효과음 청취·다중 접속은 아직 실행하지 않았습니다. 서버 공통 군사 API 체크포인트와 최신 입력 자동 생성 뒤 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy --console=plain --no-daemon`으로 검증합니다.

무기대여만 독립적으로 검사하려면 서버의 `DiplomacyLeaseRenderFixtureTest`가 생성한 디렉터리를 `HOI_LEASE_PREVIEW`로 지정하고 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy-lease --console=plain --no-daemon`을 실행한다. 이 검사는 `empty.json`, `once.json`, `monthly.json`, `multiple.json`, `fixture-source.json`이 모두 있어야 하며 뷰 파일과 세션 소스의 SHA-256을 대조한다. 자료는 실제 초기 캠페인의 재고·생산에서 발급하고 PREVIEW 전후 전체 저장 상태 불변과 저장 콜백 미호출을 검사한다. 생성기 시험의 출력은 `hoiLeasePreview` 시스템 속성 또는 `HOI_LEASE_PREVIEW` 환경 변수로 지정한다. 고정 군사 의존성으로 만든 독립 자료에는 해당 runtime baseline을 기록하며 최신 전체 서버 소스 검증으로 간주하지 않는다. `all`·`diplomacy` 검사는 `diplomacyLease` 그룹의 최신 서버 생성·해시 검증을 그대로 사용한다.

입력 회귀용 합성 뷰의 캡처명은 `hoi-diplomacy-lease-synthetic-*`, 실제 서버 발급 뷰는 `hoi-diplomacy-lease-source-*`로 구분한다. 실제 자료 렌더 중 뷰·초안·제출 권한을 고치거나 전송 요청을 만들지 않는다. 분류 탭과 실제 비활성 연료·생산 비율 툴팁도 원래 발급 상태로 검사한다.

### 세 이벤트 창의 원본 프레임

국가 이벤트·국제 뉴스·슈퍼 이벤트는 `DialogScreen`의 EVENT/GLOBAL_EVENT/SUPER_EVENT 분기에서 원본 TFR 틀을 사용한다. 지휘관 `leader_event`·완료 창과 다른 종류의 대화창은 이 분기를 사용하지 않는다. 원본 글꼴을 추가하지 않고 Minecraft 기본 글꼴, 제목 볼드체·본문 대비 1.5배·긴 제목 규칙, 본문 9 GUI 픽셀과 12 GUI 픽셀 줄 간격을 유지한다. 종이 위 글과 세 창의 제목은 그림자 없이 그린다. 서버 날짜 표기·선택 요청·슈퍼 이벤트 음원 경로는 바꾸지 않는다.

| 창 | 원본 틀과 좌표(원본 px) | 배치 |
| --- | --- | --- |
| 국가 | `EventWindow` CENTER(-355,-450), top(-5,-2) 605×123, middle(-5,121) 605×80, bottom(-5,221) 605×450 | 제목(22,70), 설명 x35(루트 x30), 선택지 bottom(115,-15), 그림 bottom(49,130) |
| 뉴스 | `EventWindow_News` CENTER(-385,-400), 713×935 | 제목(120,106) 폭460, 그림(150,164) 400×150, 덮개(143,160) 415×155, 설명(75,325) 폭560, 선택지 x180 |
| 슈퍼 | TFR 루트 982×594 중앙, TFRKP SHOW y=-317, 그림(0,30), 틀(0,0) 1001×639 | 제목(260,20) 폭490, 인용문(250,435) 폭500·높이100, 단추(350,585) 300×35 |

GUI2 2560×1440에서는 원본 1px가 화면 1px이고 GUI 좌표는 0.5배로 반올림한다. 국가 틀 텍스처 원점은 화면(920,268), 뉴스는(896,320), TFR 슈퍼는(790,424)이다. 루트/자식 위치·홀수 픽셀의 GUI 반올림 때문에 원본 좌표와 1~2px 차이가 남는다. 작은 화면에서는 원본 프레임 전체가 들어오게 축소·위치를 제한하며 글꼴 크기는 유지한다. 1280×720의 실제 좌표는 `event-frame-measurements.json`에 기록한다. 틀의 투명 여백을 포함한 사각형과 화면에서 보이는 가장자리, 글자 영역과 실제 글리프 범위를 구분한다.

원본에서 확인한 규칙:

- 사용자가 제공한 `20261012212814_1.jpg`~`20261012212834_1.jpg`와 `20261012215258_1.jpg`~`20261012215721_1.jpg`는 2560×1440, 원작 UI1이다. 국가 6종·뉴스 2종·슈퍼 1종이 있고 모두 선택지가 하나다.
- 국가 창은 위쪽이 고정되고 아래로 늘어난다. 원작 설명 첫 줄 상자는 y378, 행 간격16이며 `bottom_Window`의 루트 y는151+16×줄 수다. 5·9·12·18·24줄에서 확인했다. 선택지의 보이는 위쪽은 설명 아래+30, 그림은 선택지의 보이는 위쪽+143, 틀의 보이는 아래쪽은 그림 아래+42다. 그림은500×250이며 별도 테두리가 없다.
- 뉴스 틀은 설명16·18줄에서도 고정이다. 제목→그림→설명→선택지 순서이며 선택지의 보이는 위쪽은 설명 아래+20이다. 원본 `choice.png`의 투명 여백2px를 포함해 위젯은 설명 아래+18 원본 단위에 놓는다. 그림(1045,484), 최소화 중심(956,357), 제목 가운데x1245를 비교 기준으로 사용한다.
- TFR 슈퍼 창 원점은(789,423), 그림 위에 금속 틀·제목 띠·인용문 상자가 덮인다. 제목은 흰색, 인용문은 가운데 정렬이다. 틀의 바깥(792,425)~(1788,1060), 제목 가운데x1292, 인용문 첫 줄y860, 단추(1142,1010)~(1435,1040)이 비교 기준이다.
- CMP-12의 사본-7 검증에서는 최소화와 효과 툴팁을 생략했다. 현재 동작은 아래 [효과 툴팁과 최소화](#이벤트-선택지-효과-툴팁과-최소화)를 따른다. 빈 선택 ID가 서버 세션을 지우는 기존 서버 동작 때문에 접기는 요청 없이 클라이언트 표시 상태만 바꾼다.

확인되지 않아 클라이언트에 적용한 규칙:

- 설치된 `Hearts of Iron IV/documentation`에는 이 세 이벤트 창의 자동 높이/다중 선택지 재배치를 설명하는 항목을 찾지 못했다. 원본 GUI는 기본 좌표만 제공한다. 여러 선택지, 그림 없음, 최소70px보다 짧거나 화면을 넘는 설명, 긴 슈퍼 인용문의 원작 런타임 배치는 미확인이다.
- 뉴스는 선택지·페이지 영역의 끝을 원본 y800 안으로 제한해 종이 아래의 번짐을 피하고 본문 뷰포트를 온전한12 GUI 단위 행으로 내림한다. 이 긴 본문/작은 화면 제한은 원작에서 확인하지 못한 클라이언트 규칙이다. 국가 본문은 최소70 원본 단위를 예약하고 화면 아래에 틀·그림·선택지를 둘 수 있는 높이까지만 늘린다. 긴 본문은 본문만 스크롤한다. Minecraft 글꼴은 원작보다 크고 행 간격은24 화면 픽셀이므로 `korea.21` 등 원작에서 스크롤 없는 사례도 여기서는 스크롤할 수 있다. 원작 행 수를 강제로 맞추기 위해 글꼴 크기·원문을 바꾸지 않는다.
- 선택지는 한 열, 국가·뉴스 최대3개씩, 슈퍼1개씩 페이지로 넘긴다. 긴 선택지의 문장·문장부호를 그대로 줄바꿈하고 배경 높이를 늘린다. 그림은 선택지와 페이지 버튼 아래로 내려 겹침을 피한다. 이것을 원작 엔진의 다중 선택지 동작으로 주장하지 않는다.
- 긴 본문·슈퍼 인용문의 뷰포트와 휠 이동량은12 GUI 픽셀의 온전한 줄 단위로 맞춰 첫 줄·마지막 글리프가 부분적으로 잘리는 것을 방지한다. 국가 본문의 최소70 원본 픽셀 예약은 온전한 줄로 올림하며 화면에 들어갈 수 있는 높이는 내림한다. 넘치는 본문 오른쪽에는 위치 표시를 그린다. 본문 위의 휠, PageUp/PageDown, Home/End로 처음부터 마지막 줄까지 읽으며 그림·선택지·서버 선택은 움직이지 않는다. 이 읽기 조작·표시는 긴 글을 위한 클라이언트 규칙이고 원작 화면에서 확인한 요소로 주장하지 않는다.
- 국가 그림은 원본 비율을 유지하고 최대526×300 영역에 맞춘다. 없는 그림은 새 이미지를 만들지 않고250 원본 단위의 빈 자리를 유지한다. 뉴스 그림 영역은400×150, 슈퍼는 틀 안쪽의 원본 비율을 유지한다. 기존 팩 그림의480 제한·256색 정책은 변경하지 않는다.
- 국가 middle은 원본의 세로 끝16px와 가운데48px 타일을 보존하고 bottom은 기존9분할 도우미로 가장자리를 유지한다. 뉴스·슈퍼 틀은 한 장을 그대로 그린다. 원본 셰이더와 Minecraft의 알파 합성 차이·타일 이음새는 3배 최근접 crop으로 리뷰한다. 사용되지 않는 `GFX_Super_Event_Quote_Background`와 `GFX_TFR_event_pic_overlay`는 이 세 GUI에 참조가 없어 추가하지 않는다.

`DialogEventFrameTest`는 두 해상도의 실제 전체 화면·창 crop, 전체 선택지 도달, 문장·문장부호 보존, 그림 비율, 본문만 스크롤, 좌우 클릭·드래그·반복 클릭·비활성·오래된 리비전·Esc를 검사한다. 원본 카탈로그의 사례는 `korea.21`, `korea.1`, `korea.2`, `korea.79`, `korea.23`, `korea.54`, 선택지3/4개, `russia.454`23개, 그림 없음, `Australia.0`, `news.113`, `news.131`, 뉴스1/4개, `gma_news.5`, TFR/TFRKP 슈퍼와 `PRC_eadi_victory_super_event`의 긴 인용문이다. `news.131`의 스크린샷 대조는 TFR `TFR_events_ZZZ_news.txt`의 `NOT tag=USA`인 `news.131.a`만 표시하는 합성 입력이며 서버가 조건을 실행했다고 주장하지 않는다.

재현은 읽기 전용 사본-7을 run scratch에 복사한 후 같은 클라이언트 체크아웃에서 순차 실행한다. 원본 사본·운영 서버·설치 묶음·프로토콜은 수정하지 않는다.

```powershell
python scripts/export_event_frame_fixtures.py --catalog <사본-7>/content/hoi/scenario/tfr-presentations.json --output client/src/gametest/resources/event-frame-fixtures.json
.\gradlew.bat test runGameTest remapJar "-PhoiServerSource=<사본-7 실행용 복사본>" "-PhoiProtocolSource=<같은 복사본>/protocol/src/main/java" --console=plain --no-daemon
.\gradlew.bat runGameTest -PhoiTestSuite=event-frames "-PhoiServerSource=<사본-7 실행용 복사본>" "-PhoiProtocolSource=<같은 복사본>/protocol/src/main/java" --console=plain --no-daemon
python scripts/compare_event_frames.py --run <현재 전체 runDirectory> --baseline <CMP-11 마지막 전체 runDirectory> --shots <제공된 원본 crop 폴더> --output build/event-frame-review --pack ../HOI-resourcepack
```

원본과의 대조는 국가(900,270)에서720×1000, 뉴스(900,320)에서720×860, 슈퍼(770,400)에서1040×680을 사용하며 제공된 짧은 국가 crop은 같은 실제 크기에 맞춘다. 비교 PNG는 왼쪽 원작 실제 화면, 오른쪽 Minecraft 실제 렌더다. 같은 영역 crop에서 긴 창 일부가 잘릴 수 있어 별도의 전체 창 crop을 함께 제공한다. `render-verification.json`은 원본 아홉 좌표와 CMP-11 전체 실행 대비 변경·추가·누락 캡처(같은 이름의 반복은 발생 순서)를 기록한다. 동적 게임 배경의 변화는 UI 수정으로 단정하지 않는다. 원본 글꼴 동등성·운영 멀티플레이·직접 효과음 청취는 별도 미수행 항목이다.

세 이벤트 창의 틀 배치 집중 검증은 사본-7 실행용 복사본과 외부 팩6개를 사용한 `test runGameTest remapJar -PhoiTestSuite=event-frames`로342.18초에 통과했다. JUnit은클라이언트110개·프로토콜114개(실패/오류/건너뜀0), 실제GUI2 입력/렌더 사례는19×두 해상도이며 전체/창·스크롤/마지막 페이지 PNG152장을 생성했다. 국가 선택지 앞의 보이는 여백30px, 뉴스20px, 국가 그림의 선택지 앞끝 기준144px(원작143px, GUI 반올림1px)을 확인했다. 첫 전체 실행은768.66초에 기존 `AudioMusicRoutingChecks`의 MAP_CONSTRUCTION 곡/채널 유지 검사에서 실패했고 음원 코드를 수정하지 않은 재검사를 별도로 기록한다.

긴 본문 잘림 보완 후 	est runGameTest remapJar -PhoiTestSuite=event-frames는67초에 통과했다(Gradle 표시). 실제GUI2 19×두 해상도·PNG152장을 다시 생성하고 줄 단위 휠·PageUp/PageDown·Home/End, 첫 줄과 마지막 글리프의 scissor 경계, 선택지 전체 도달을 확인했다. JUnit224개는 이전 통과 결과를 재사용했다. 앞선 전체 실행의 DialogScreenChecks 둘째 슈퍼 선택지 조회는 새 한 페이지 규칙에 맞춰 다음 페이지를 연 뒤 검증하도록 변경했다.

최종 필터 없는 클라이언트 	est runGameTest remapJar는 전용 검증 시간에서16분49초(Gradle 표시), 1008.42초(run 시각 기준)에 통과했다. 전체 실제 렌더 PNG1482장을 생성했고 이벤트19종×두 해상도의 측정값은 긴 본문 수정 후 집중 검사와 모두 같았다. JUnit224개는 기존 통과 결과를 재사용하고 현재 사본-7의 자동 renderer fixture를 재생성·검증했다. CMP-11 마지막 전체 실행과 같은 이름/발생순서의 원시 PNG 해시는334개 변경·304개 추가·318개 누락이었다. 동적인 월드/배경 차이는 UI 회귀의 증거로 단정하지 않으며 정확한 목록과 로그는 CMP-12 검증 첨부물의 
ull-validation.json을 사용한다. 전체 실행의 이벤트 프레임 검사는 기존 두 호출 경로에서 각각 수행되어 새 PNG304장(152장씩 두 회)이 포함된다. 기준 실행에만 있는318개 캡처의 UI 동등성은 이번 범위에서 전부 확인하지 않았다. 작업 시작 때 이미 삭제 상태였던 MapArmyRenderChecks·AudioKoreanMusicChecks와 다른 미커밋 변경은 복구·변경하지 않았다. 음원 코드는 수정하지 않았다.

동일한 사본-7 실행용 복사본에서 서버의 필터 없는 `test runGameTest remapJar -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`는1381.66초에 통과했다. 서버JUnit은core1607개·fabric297개·protocol129개(실패/오류0)이며 core의4개는 선택적인 기록 성능 자료·프로파일링·검증된 운영 백업·운영자가 제공한 캠페인 fixture가 없어 건너뛰었다. mock GameTest169개는 모두 통과했다. 원본 사본과 실행용 복사본의 소스/콘텐츠/빌드 정의1181개는 바이트가 같았다. 서버JAR은server 전용 진입점과 사용자 모드아이콘만 포함한 것을 확인했으며 운영 배포·접속 검증은 하지 않았다. 이 서버 검사로 실제 멀티플레이나 TPS 측정 결과를 주장하지 않는다.

### 이벤트 선택지 효과 툴팁과 최소화

`DialogEventTooltipTest`는 커서를 옮긴 뒤 실제 화면을 캡처하고 그 렌더에서 계산된 `tooltipBounds`를 검사한다. Fabric의 `waitTicks()`는 틱 진행만 보장하며, `takeScreenshot()`은 화면 추출과 렌더를 직접 실행한다. 틱만 기다린 뒤 렌더 경계를 먼저 읽으면 전체 실행에서 아직 그려지지 않은 툴팁을 누락으로 판정할 수 있다. 실패 캡처와 사건·선택지·버튼·커서 좌표를 함께 보존하며, 효과 문구의 유무와 화면 경계 기대값은 유지한다.

`DialogView.Choice(id,label,tooltip)`의 효과 원문은 최대 3,000자이며 생략/null은 빈 문자열이다. 국가 이벤트·국제 뉴스·지휘관 이벤트의 선택지 호버는 `DecisionText`와 기존 `production/tooltip` 상자를 재사용한다. 원본 색 코드, 명시된 아이콘, LF 줄바꿈, 빈 줄, 두 칸 들여쓰기와 수치 표기를 보존하고 엔진 효과를 클라이언트에서 추가하지 않는다. 효과가 없으면 상자를 표시하지 않으며, 잘린 선택지 문구의 보조 툴팁은 효과가 없을 때만 사용한다. 효과 상자는 선택지 아래 3 GUI px에서 시작하고 화면 경계 안으로 이동한다. 화면보다 긴 상자는 기존 휠 스크롤로 끝까지 읽는다. 공용 상자/다른 화면의 툴팁 외형은 변경하지 않는다.

원본 `3350890356/interface/eventwindow.gui`의 `btn_minimize`는 국가 창 top_Window의 (35,45), 국제 뉴스의 (45,20), `EventWindow_leader`의 (10,45)이며 기존 `GFX_event_button_minimize` 33×33을 사용한다. 답할 선택지가 있는 세 창의 단추와 Esc는 서버로 빈 선택을 보내지 않고 창만 숨긴다. 한 선택지의 Enter는 기존 선택 제출을 유지하고, 여러 선택지에서 포커스 없는 Enter는 접기다. 선택지는 서버의 token/revision/choice ID로 한 번 제출하고 같은 revision으로 재활성화하지 않는다. 새 이벤트는 기존처럼 바로 표시한다. 선택지가 없는 읽기용 뉴스와 슈퍼 이벤트의 닫기·음악 동작은 유지한다.

사건과 결정의 사건 행은 `countrydecisionview.gui`의 `event_header`/`event_item`, 기존 `GFX_event_header_bg`/`GFX_event_item_bg`/`GFX_event_type_strip`를 사용한다. 행은 원본 512×33, 아이콘은 30×26 비율을 유지한다. 제목은 왼쪽에 표시하고 `remainingDays`가 있으면 `timer_text`의 원본 x350/y8·폭150·오른쪽 정렬 구역에 남은 날수를 표시한다. 오른쪽 끝은 원본 x500을 같은 행 배율로 환산한다. 기본 한국어 `core_l_korean.yml/EVENT_AMOUNT_DAYS`의 `$AMOUNT$일`을 사용하며 null은 숨기고 0은 `0일`이다. 클라이언트가 시간을 차감하지 않는다. 같은 token/revision의 기한 갱신도 기존 사건 목록의 재구성을 통해 반영한다. 행을 누르면 같은 세션 창을 다시 열거나 맨 앞으로 가져온다. 서버 Close(선택·기한 만료·국가 변경)는 접힌 세션까지 제거하고 연결/캠페인 reset도 목록을 비운다. 사건 머리글의 작은 사건·사건·뉴스 팝업 체크 상자는 [개인 설정](#결정-개인-설정과-지도-요청)을 따른다. `DECISIONSVIEW_EVENT_HEADER`의 사건 및 `MINIMIZE_EVENT`의 설명은 설치된 한글패치에 덮어쓰기가 없어 기본 한국어 `decisions_l_korean.yml`/`events_l_korean.yml`의 원문을 따른다. command/save ID는 바꾸지 않는다.

원본 `212816`과의 나란히 비교에서 공용 상자의 거의 검은 바탕과 우상단 자홍색→좌하단 보라색 테두리를 유지했다. GUI2에서는 테두리와 글리프가 정수 픽셀 단위이고 한 줄 상자는 224×36 화면 px, 원본 비교 crop은 221×48px다. Minecraft 기본 글꼴의 폭·줄높이에 따른 차이를 보존하며 원본 글꼴 동등성으로 해석하지 않는다. 세 줄 비교는 서로 다른 사건의 효과 문구를 사용하므로 상자 외형 비교다. 답하지 않은 사건 아이콘은 원본 국가 사건 행에 보이는 느낌표 프레임을 사용하고 국가·뉴스의 중요도나 남은 시간은 추정하지 않는다.

두 줄 선택지의 기본 글꼴·크기·줄 간격은 유지하고 버튼 높이 계산의 총 세로 여백만 2→6 GUI px로 늘린다. 한 줄 버튼과 이벤트 제목의 볼드체 1.5배·날짜·기존 틀 좌표는 유지한다. GUI2 1280의 `news-four`는 두 줄 선택지 세 개가 각각 4 GUI px 늘어나 본문 표시 높이가 48→36 GUI px, 마지막 줄까지의 스크롤 이동량이 216→228 GUI px가 된다. 본문 전체는 기존 줄 단위 스크롤로 읽는다.

재현은 팩을 먼저 빌드하고 Java 25로 아래 명령을 순차 실행한다. 원본 사본-8은 읽기 전용으로 보존하고 source/content/protocol을 그대로 복사한 실행용 디렉터리를 사용한다. 서버 프로토콜 소스 46개와 클라이언트 소스는 바이트까지 같다. 전체 검사 전에 같은 사본에서 수동 입력을 생성하고 `HOI_COMMANDER_PREVIEW`(지휘관 53개), `HOI_OFFICER_PREVIEW`(성공한 장교단 mock GameTest의 입력 17개), `HOI_MISSION_OVERLAY_FIXTURES`(임무 3개), `HOI_LAND_PLAN_PREVIEW`(육군 계획 1개)를 설정해야 전체 범위를 검사한다. 수동 입력 생성·경로 규칙은 [README](../README.md)의 추가 화면 입력 전체 검사 안내를 따른다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=event-tooltips -PhoiServerSource=C:/HOI-client/client/build/cmp15-server-source -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/server-snapshot-8/HOI/protocol/src/main/java --console=plain --no-daemon
.\gradlew.bat test runGameTest remapJar -PhoiServerSource=C:/HOI-client/client/build/cmp15-server-source -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/server-snapshot-8/HOI/protocol/src/main/java --console=plain --no-daemon
python scripts/compare_event_tooltips.py --run <성공한 전체 실행 폴더> --baseline <CMP-12 마지막 전체 실행 폴더> --output <비교 결과 폴더>
```

이벤트 선택지 툴팁은 결정 화면과 같은 `hoi:ui/decision_flags.json`·`decision_assets.json`을 `DecisionText.load`로 창마다 한 번 읽는다. 원문 줄에 텍스트 그림 토큰만 있으면 `textImages`의 원본 크기 절반으로 그림 블록을 그리며, 후보 선택(`korea.115`)의 `GFX_KOR_{lee,yoon,sin}_election_poster_texticon`(원본 250×365)이 이 경로로 후보 사진을 표시한다. 등록되지 않은 토큰은 기존 키워드 글리프로 남는다. `decision_assets.json` 읽기 한도는 4 MiB이다.

`DialogEventTooltipTest`는 권위 있는 사본-8의 다섯 입력에서 각 선택지 호버·빈 효과·아이콘과 GUI2 2560×1440/1280×720 경계를 검사한다. 최소화·재열기·두 사건·서버 Close·refresh/revision·반복 제출·좌/우클릭·드래그·reset은 실제 클라이언트 입력 경로와 관측 요청으로 검사한다. 국제 뉴스 접기 입력은 기존 원본 프레임 카탈로그의 합성 클라이언트 사례이며 운영 뉴스 발급/기한 만료를 증명하지 않는다. 기본 Minecraft 글꼴의 폭·줄높이와 원본 글꼴은 같다고 주장하지 않는다. 운영 접속·다중 사용자 동기화·직접 효과음 청취는 별도 미수행이다. 비교 이미지는 실제 렌더 캡처와 원본 Steam 화면을 사용하며 3배 확대는 최근접 보간이다.

필터 없는 사본-8 전체 검사는 21분 29초(실행 시각 기준 1290.02초)에 통과했다. 클라이언트 110개·프로토콜 115개의 JUnit 성공 결과를 재사용하고 자동 producer 45개를 실행했다. 수동 producer 50개·별도 임무 producer 6개·장교단 mock GameTest 1개의 기존 성공 결과와 자동 입력 78개·수동 입력 74개를 확인했다. 실패·오류·건너뜀은 모두 0이다. 실제 PNG 1838장을 생성했고 집중 검사와 이벤트 프레임/툴팁 측정값이 모두 같으며 누락 아이콘은 0개다. CMP-12 마지막 전체 실행의 PNG 1482장을 모두 읽고 보존된 SHA-256 334개와 일치하는지 확인한 뒤 같은 화면 이름·발생 순서로 비교했다. 원시 PNG는 639장 변경·356장 추가·0장 누락이다. 추가분은 새 툴팁·최소화 38장과 이전 전체 검사에서 빠졌던 화면 318장이며, 318개의 이름·발생 횟수는 이전 누락 기록과 정확히 같다. 동적 월드/배경 차이를 포함한 변경 639장 전체의 원인이나 UI 동등성을 직접 판정한 결과는 아니다. 전체 캡처 비교 목록·로그·원본 나란히 비교·확대·두 줄 버튼 전후는 이슈 첨부의 `full-validation.json`과 최종 검증 ZIP을 사용한다. 최종 JAR은 client 전용 진입점, 언어 파일과 사용자 모드 아이콘을 유지하며 게임 이미지·음향은 외부 팩에 남긴다. 포함된 공유 프로토콜 클래스 262개는 사본-8 컴파일 결과와 바이트까지 같고, 원본 사본·운영 서버·설치 묶음은 수정하지 않았다.

## 34개국 이야기 화면의 원본 근거

원본 캡처의 실제 경로는 `C:/Program Files (x86)/Steam/userdata/1398211522/760/remote/394360/screenshots`이다. 작업 설명의 `Steam/steamapps/userdata`는 존재하지 않는다. JPG 530장을 파일명 순 contact sheet 12장으로 검토했다. 전체 원본을 모두 원해상도로 읽은 검사나 Minecraft 구현 화면 대조는 아니다. 기존 506장 조사 뒤 추가된 24장도 포함하며, 파일별 SHA-256·크기와 장면별 판정은 CMP-22의 `source-screen-survey.json` 첨부물에 남긴다.

| 대상 | 확인한 근거와 판정 | 다음 원본 관측 조건 |
| --- | --- | --- |
| 국가/뉴스/슈퍼 이벤트 | `compare_event_frames.py`의 9개 지정 캡처를 유지한다. `20261012212814_1`=korea.21, `20261012212828_1`=korea.1, `20261012212834_1`=korea.2, `20261012215309_1`=korea.79, `20261012215331_1`=korea.23, `20261012215336_1`=korea.54, `20261012215258_1`=news.113, `20261012215316_1`=news.131, `20261012215721_1`=TFR 슈퍼. 이 국가 사건들은 `minor_flavor=yes` 사례가 아니다. | 기존 원본/실제 렌더 대조 절차를 재사용한다. |
| 국가·주 대상 결정 | 결정 패널 캡처는 있으나 대상별 국기/비용/지도 강조를 확정할 장면은 특정하지 못했다. | 원본 JAP의 `JAP_invest_in_south_asia`는 `JAP_cold_war_flag`와 대상 `has_war=no`를 요구하며 BRM·VIN·LAO·CAM·INS·MAL·PHI·SIG·SIA를 열거한다. 조건을 만족한 목록/각 대상 hover/선택 전후 지도, 비활성 대상 및 주 대상 결정도 촬영해야 한다. |
| minor_flavor | 전체 사건 카탈로그의 58정의는 APA·ATW·GER·USC 파일에만 있다. 34개국 전용 파일의 직접 사례와 해당 캡처는 확인되지 않았다. 설치 GUI/GFX에도 같은 이름의 별도 창 선언은 찾지 못했다. | 타국 경유 실제 발급이 확인되거나 원본 사례의 발급 화면·최소화·선택지 hover가 필요하다. 그전에는 계약대로 기존 사건 창을 사용한다. |
| 정당 연립 | 정치창/원형 지지도는 있으나 연립의 정당별 표시·hover를 입증하지 못했다. `TFR_UN_coalition_politics_icon`은 `GFX_UN_Member`와 `TFR_GLOBAL_ASSOCIATION_TT`를 쓰는 국제기구 아이콘이다. | 실제 연립이 구성된 나라에서 정치창·지지도/연립 hover·구성 변경 전후를 촬영한다. 국제기구 가입을 정당 연립으로 해석하지 않는다. |
| 자치 | 원본 `countrydiplomacyview.gui`의 `autonomy_score_icon`/`autonomy_score_gain`과 정치창 자산 선언은 확인했다. 주종국·자치 점수가 함께 식별되는 캡처는 특정하지 못했다. | 실제 주종 관계의 종속국 정치창·종주국 종속국 목록·외교창, 자치 점수/증감 hover와 단계 변경 가능/불가 상태를 촬영한다. |
| 핵 | 상단 핵 보유량과 DEFCON은 발사/표적/결과 화면의 근거가 아니다. 발사 선택·확인·지도 폭발·피격/사용국 결과 알림은 특정하지 못했다. | 원본의 핵 보유·기술·운반 수단·전쟁/제공권 등 실제 조건과 활성 DLC/모드 버전을 기록하고 표적 hover→사용 확인→지도 연출→양측 결과 순서를 촬영한다. 묶음 파 계약 전에는 화면 구현을 확정하지 않는다. |

팩 `ui/story_assets.json`은 국가별 원본 국기 키와 raw 초상 GFX/경로, 동적 아이콘을 실제 외부 텍스처에 잇는다. 처음 가져온 자산 작업에서는 Java·protocol·서버 projection을 바꾸지 않았으며 당시 미수행이던 클라이언트 소비는 아래 `CountryDisplays`·`UiAssets` 경로에 연결되어 있다. 이 과거 에셋 작업만으로 실제 GUI2 입력·멀티플레이·직접 청취를 검증했다고 보지 않으며 원본 에셋 미리보기를 Minecraft 구현 캡처로 제시하지 않는다.

## 서버 국가 표시 표와 원본 자산

`hoi:country_display_v1`의 공개 `Entry(tag,name,flag)` 표는 서버가 정한 이름과 정확한 원본 국기 키를 보관한다. 정치/HUD 탭·외교·관계 국기와 지도 부대 국기는 같은 조회를 사용한다. `country/<tag>/flag`를 그리는 기존 렌더 경로도 이 표를 적용하므로 명령·선택 대상 ID는 그대로 유지한다. 국가 메뉴 이름·외교 제목/국가 선택 목록·지도자 툴팁은 최신 표 이름을 사용하고 표에 없는 국가는 기존 서버 스냅샷 이름을 유지한다. 외형/이념 접미사를 클라이언트에서 조립하지 않는다. 팩에 받은 키가 없으면 원래 태그 기본 국기를 쓴다. 표는 disconnect에서 비우고 리소스 재로드에서는 보존한다. 결정 문장 GetFlag는 기본 작은 국기 글리프의 폭·간격을 유지하며 현재 표의 이념·외형 텍스처로 표시한다. `authoritative-fields`는 기본·대체·누락·복원·리소스 재로드를 같은 캐시된 문장에서 실제 렌더로 비교한다. 이 검사는 공개 표시 표의 클라이언트 적용이며 실제 운영 국가 변경을 재현한 것은 아니다.

`ui/story_assets.json`의 원본 초상 GFX/경로와 동적 수정치 키는 기존 UiAssets 렌더 경로에서 실제 외부 텍스처에 연결한다. 서버가 정규화한 `politics/art/<GFX 소문자>` 동적 아이콘도 원본 키와 대응한다. 미등록/모호한 원본은 기존 누락 이미지 경로를 유지한다. 숫자·툴팁·인물 역할·효과는 계산하거나 보완하지 않는다.

`CountryDisplaysRenderTest`는 실제 GUI2 2560×1440·1280×720에서 합성 country_display 표를 기존 국가 메뉴/외교 화면에 넣고 외형 변경·이름·국기 복원·누락 키 fallback·asset reload 보존·disconnect 정리를 검사한다. 원본 초상과 서버 정규화 동적 아이콘의 실제 팩 조회 및 기존 정치 메뉴 두 해상도의 렌더도 확인한다. 초상·아이콘을 추가한 입력과 캡처는 합성 검증으로 표시하고 인물 역할·수정치 효과 검증으로 간주하지 않는다. 이는 실제 서버 효과 실행/국가 외형 lifecycle·원작 장면 픽셀 동등성·다중 접속·실제 재접속 증거가 아니다. GUI 설정은 검사 전 값으로 복원한다. 집중 명령은 [음향](audio.md#서버-스토리-신호-소비)의 story-signals를 사용한다.

지도 이동 `MapFocus.State(state,x,z,minX,minZ,maxX,maxZ)`는 주 안의 중심 칸과 주의 경계를 지도 블록 좌표로 보낸다. 원본 goto 효과는 열린 지도 관측 위치만 옮기며 주 강조를 추가하지 않는다. 현재 서버 지도 클릭은 플레이어 눈 위치에서 계산하므로 클라이언트 Camera만 이동하면 클릭 원점·청크 로딩이 어긋난다. 서버가 보낸 신호에 연결된 bounded 요청을 검증하고 기존 atlas 관측 이동 경로를 사용하도록 서버 담당에게 요청했다. 결정 화면의 HIGHLIGHT/GOTO 소비는 [현재 지도 계약](#결정-개인-설정과-지도-요청)을 따른다. 서버 스토리 goto_state 신호의 별도 수명/화면 계약과 혼동하지 않으며 atlas scene box에서 주를 역추정하지 않는다. change_tag_from·create_entity·핵 화면은 계획 8판 범위에 포함하지 않는다. 필터 없는 전체 클라이언트/서버 검증과 staged server JAR·운영 다중 접속은 집중 검사와 구분해 기록한다.


## 육군 지휘 패널 원본 배치

군·집단군 카드와 좌측 육군 상세의 초상화는 기존 원본 프레임 안쪽에 종횡비를 유지해 채우고 넘치는 가장자리만 자른다. 빈 군 생성의 `+` 그림은 별도 경로를 유지한다. 지휘관 특성/상세 버튼은 초상화 왼쪽, 참모 버튼은 오른쪽에 맞추며 각 버튼의 하단과 클릭 영역을 같은 초상화 하단에서 계산한다. 참모 자격을 받은 상태는 서버 `LEADER_ADVISOR.icon`의 원본 참모 그림과 기존 종이 배경을 사용한다. 클라이언트가 추가 표식을 만들거나 참모 임명 상태를 추론하지 않는다. 원본 근거는 TFR `leadergroups.gui`·`unitview.gui` 및 팩 `commander-command-import-report.json`의 프레임/버튼 출처다. `CommanderLeaderControlsTest`는 `commander-layout`과 무필터 전체 검증에서 GUI2 두 해상도의 군·집단군·참모 상태를 렌더하고 양 끝/하단 좌표, 원문 툴팁, 좌·우 클릭, 서버 요청과 비활성 차단을 검사한다. 고정 서버 응답의 재생이며 운영 접속·멀티플레이·직접 효과음 청취 검증은 아니다.

절반 선택은 기존 군 상세의 헤더·사령부·본문을 유지하고 선택된 사단을 먼저, 미선택 사단을 뒤에 표시한다. 각 행의 기존 선택 프레임으로 둘을 구분하며 서버의 사단/군 배속을 바꾸지 않는다. 같은 본문의 후속 클릭도 상세 문맥을 유지한다. 여러 군을 Shift+좌클릭하거나 여러 군 소속 사단을 선택하면 드래그 선택과 같은 군 헤더 묶음을 표시한다. 공통 집단군이 있으면 기존 드래그 규칙대로 먼저 표시한다. `CommanderDragSelectionTest.selectionPaths`는 선택형 과거 입력 없이도 `CommanderGroupsRenderTest`에서 실행한다. `test runGameTest remapJar -PhoiTestSuite=headquarters-layout`으로 GUI2 2560×1440·1280×720의 실제 절반 선택/Shift 입력과 본문 행 보존·공통 부모 포함 3개 헤더를 검증했다. 군 두 개의 선택과 사단 수는 명시적 합성 입력이다. 실제 군 생성·저장·다중 접속 검증이 아니다. 공통 부모 헤더의 닫기는 아래의 선택된 군들을 해제하고 개별 군 헤더의 닫기는 그 군만 해제한다. 하단 군 카드의 서버 해산과 구분한다. `CommanderGroupsRenderTest`는 하단 X를 연속 클릭해 단일 armyId·현재 token/revision의 직접 명령 한 건만 전송하는지, 확인창이 열리지 않는지 검사한다. 권위 응답을 주입하기 전에는 카드를 유지하고 이후 대상 카드만 사라지는 합성 상태도 두 해상도로 확인한다. 이 검사는 서버의 실제 해체·사단 보존 검사를 대신하지 않는다.

현재 카드 줄은 `running` 값과 관계없이 화면 하단에 고정된다. 국가·차원·HUD 표시 상태가 유효하면 이벤트 창에서도 핫바·체력·배고픔·경험치 표시를 숨긴다. 국가 해제나 차원 불일치 때는 바닐라 HUD로 돌아간다. 집단군 펼침 단추, 군/함대 카드 X, 집단군 위 정지·전진 칸은 직전 배치에서 1GUI 내려간다. 기존 물리 픽셀 보정은 유지한다.

기본 지도에서는 배속 비행단 띠와 상세 머리줄의 배속 선택 단추를 표시하지 않는다. 공군 지도는 요격기/지원기/+ 세 칸을 유지한다. 사단 상세 목록의 훈련 활동 그림만 생략하며 다른 활동 그림·훈련 명령·경험치 표시는 유지한다.

공군 배속의 +좌클릭과 군 카드 우클릭, 배속 칸·비행단 소속 배지의 우클릭 해제는 대화창을 열지 않고 직접 지휘 명령을 보낸다. `AIR_ATTACH`는 군 ID 원문과 게시된 `air/<id>` 선택 목록, `AIR_DETACH`는 해제할 `air/<id>`만 보내며 rays는 비운다. 서버 리비전 응답 전 반복 송신을 막는다. 공군 지도 툴팁은 기본 마우스 인접 위치를 화면 안으로 제한하며 카드 줄 회피를 위한 강제 상향 이동을 적용하지 않는다.

이 변경의 집중 재현은 팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=army-layout`과 `-PhoiTestSuite=navy-layout`을 각각 사용한다. 서버는 `-PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp50-server/HOI`, protocol은 `-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp50-server/HOI/protocol/src/main/java`, 팩은 `-PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`로 고정한다. 없어진 과거 팩 폴더를 기본값으로 사용하지 않는다. GUI2 2560×1440·1280×720에서 PAUSED·legacy running 누락·재개·이벤트 창의 HUD 숨김, 기본/공군 지도 배속 표시, 사단 TRAINING/MOVING 활동 그림, 카드 접기·해제·드래그와 직접 배속 명령을 검사한다. 비행단이 포함된 기본 지도 비교와 TRAINING/MOVING 비교는 표시 상태를 변환한 합성 입력이다. 직접 배속·해제는 송신 관찰자와 세션·리비전 응답 주입으로 확인하며 실제 서버 소켓·저장·멀티플레이 검증을 뜻하지 않는다. 직접 음향 청취와 필터 없는 전체 검증은 별도 검토 후 실행 범위다.

최종 소스의 `navy-layout`은 1분39초·실제 PNG172장, `army-layout`은 3분53초에 종료 코드0으로 통과했다. 클라이언트140개·protocol124개·fixture 생산자67개는 실패·오류·건너뜀0이며 변경 없는 성공 결과는 재사용했다. 이전 팩 경로 누락 실행과 공군 전구를 기본 지도 테스트에 잘못 재사용한 중간 실행은 실패로 남겼다. 테스트 전구 범위를 바로잡은 뒤 기본 지도와 공군 지도의 표시 차이를 두 해상도에서 확인했다. staged JAR는 client 진입점만 포함하고 허용된 mod-list icon 외 내장 그림·음원은 없다.

장군·원수 스킬 줄은 기본 스킬 3개를 포함해 합계 최대 4칸 표시한다. 추가 특성 스킬은 획득 순서의 첫 1개만 표시한다. 획득 순서는 서버 `Panel.abilities`가 정하며 클라이언트는 정렬하지 않는다. 복수 선택에서는 각 지휘관의 앞 4개 안에 있는 공통 스킬만 첫 지휘관의 서버 순서로 표시하고, 숨긴 다섯째 스킬로 빈칸을 채우지 않는다. 숨긴 스킬에는 클릭·호버 영역을 만들지 않는다. 집단군 기본+원수 특성 스킬, 군 기본+장군 특성 스킬의 기존 구분은 유지한다. `CommanderAbilityTest`와 `CommanderAbilitiesRenderTest`는 두 역할의 기본·추가 목록, 초과 입력, 순서, 복수 선택과 회색 표시를 확인한다. 합성 입력의 순서 보존은 실제 저장된 특성 획득 순서의 서버 검증과 구분한다.

지휘관 임명 목록은 집단군에서 원수→장군, 군에서 장군→원수 순으로 계급을 먼저 묶는다. 계급 안에서는 기존 기술 내림차순과 이름·ID 동률 순서를 유지하며, 정렬 헤더를 바꾸어도 계급 우선순위는 유지한다. 검색·임명된 지휘관 필터·선택 가능 여부와 서버 요청 ID는 그대로 사용한다. 서버가 보내지 않은 후보를 만들거나 장군의 원수 임명 자격을 부여하지 않는다. `CommanderRosterTest`는 합성 입력과 서버 `kor-appointment-army-mixed`·`kor-appointment-army-group-mixed` 후보를 GUI2 두 해상도로 검사한다. 서버 후보의 계급·기술 순서, 검색한 실제 장군의 진급 비용·툴팁과 단일 선택 요청을 확인한다. 원본 fixture의 빈 토큰에는 기존 방식대로 테스트용 issued 세션을 붙이며 실제 서버의 진급·비용 집행이나 소켓 연결 검증을 뜻하지 않는다.

`CommanderHudDetailsTest.traitSkillsInactiveMarshalTraitsAndMixedAppointmentsReachThePanels`가 생성하는 `kor-command-panel-army-trait-skills`·`kor-command-panel-army-probing-active`와 위 임명 입력 두 개는 `armyHud`의 자동 생성·출처 검사 대상이다. `CommanderSourceStateTest`는 이 실제 응답으로 군 지휘 원수의 비활성 특성3개·붉은 픽셀과 경고 원문, 집단군의 기본3개+원수 특성 스킬만 표시, 군 `mil_1`의 먼저 획득한 탐색 공격과 군 `mil_3`의 먼저 획득한 글라이더가 각각 넷째 칸에 표시되는지 확인한다. 글라이더 회색 픽셀, 탐색 공격 취소 툴팁 원문과 색·빈 줄, 카드 기술 블록 앞 빈 줄 하나도 검사한다. 활성 스킬도 첫 4개 밖이면 숨기며, 미지원 두 스킬의 회색 픽셀은 합성 입력에서 각각 네 번째 칸에 배치해 추가로 검사한다. 활성 특성 그림 자체에 있는 붉은 픽셀은 비활성 판정 근거로 쓰지 않는다. 취소 제목의 표시 문장은 중간 색 코드를 제외해 확인하되 렌더러가 받는 원문은 그대로 비교한다. 실제 서버 테스트가 작성한 상태·특성 조합이며 운영 게임에서 플레이한 상태를 수집한 자료와 구분한다.

전체 검증의 수동 입력을 격리해서 복사할 때는 `HOI_LAND_PLAN_PREVIEW`가 가리키는 `land-plan-overlay.json`과 함께 `land-plan-preview.json`도 같은 폴더에 보존한다. `SceneSurfaceRenderChecks`가 저장된 계획과 편집 중인 계획을 이 두 파일에서 각각 읽는다. 선택 입력인 `HOI_FOCUS_PREVIEW`·`HOI_PRODUCTION_PREVIEW`·`HOI_CONSTRUCTION_PALETTE_PREVIEW`도 전달한 파일의 출처·해시를 보존한다. `HOI_AGENCY_PREVIEW`·`HOI_POLITICS_PREVIEW`를 생략하면 그 별도 서버 원문 캡처는 실행하지 않으므로 기본/합성 검사의 성공과 구분한다.

서버의 실제 건설 블록 상태 입력으로 색을 검사할 때는 `src/gametest/resources/atlas-client-scene.json`도 같은 검증 서버가 내보낸 자료로 맞춘다. `loadPack`이 이 자료의 `blockstates`·`generatedAssets`를 테스트 전용 carrier ZIP으로 만든다. Polymer의 note block 배정은 등록 순서에 따라 바뀔 수 있으므로 지리 `sourceHash`가 같아도 블록·그림 연결까지 같다는 뜻은 아니다. 예전 자료의 `snare/note=23/powered=false`·`snare/note=24/powered=true`가 `supply/0`·`supply/1`을 가리키던 것과 달리 최신 출력은 건설 예약용 `construction/101`·`construction/102`다. 자료 전체를 성공한 서버 run의 출력과 일치시키고 원본/수정 SHA256·블록 연결·새 자산을 검증 근거에 보존한다. 이 자료는 GameTest 전용이며 운영 클라이언트 JAR의 게임 자산으로 넣지 않는다.

`HOI_PRODUCTION_PREVIEW`로 받은 최신 생산 응답은 빈 생산 목록도 허용한다. 일반 산업 화면과 연구된 후속 장비 검사는 그 응답을 사용한다. `IndustryProductionPresentationTest`의 합성 재고색·라인 우선순위·장비 그림 배경 입력 검사는 생산 라인이 있는 기존 bundled fixture를 명시적으로 사용한다. 해당 검사를 위해 실제 서버 응답에 임의 라인을 추가하거나 빈 응답을 생산 중인 상태라고 표시하지 않는다. 이 분리는 `IndustryScreenChecks.bundledFixture`를 기존 리소스 읽기 경로에서 추출해 처리하며 서버의 생산·저장 동작을 바꾸지 않는다.

재현은 리소스팩 빌드 뒤 Java25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout`으로 집중 검사를 하고, 같은 소스에 `test runGameTest remapJar`를 필터 없이 실행한다. 고정 서버 사본은 `-PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42-server/HOI`와 `-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42-server/HOI/protocol/src/main/java`를 함께 지정한다. 드래그·57개 목록의 기존/합성 입력은 `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`로 지정하며, 최신 특성·스킬·임명 응답과 구분한다. 전체 검사의 수동 입력은 위 복사 규칙과 [README의 환경 변수](../README.md#사용과-빌드)를 따른다. 실행별 결과·실패/중단 로그·원본 비교는 CMP-42 검증 자료에 보존한다. 실제 렌더 검사는 운영 접속·다중 사용자·저장/재접속·직접 청취를 검증하지 않으며 이 항목들은 미수행이다.

단일 군·집단군 선택과 드래그로 군 둘 이상을 선택한 압축 패널 모두 각 패널 뒤에 `unitview.gui/army_color_button`의 `GFX_group_marker3`(원본 44×88px)를 놓아 오른쪽 색 띠를 표시한다. 공통 `armyHeader`가 기존 팩의 원본 픽셀과 드래그 패널의 좌표·크기를 재사용한다. 단일 선택의 근거는 `20261017214413_1.jpg`·`20261017214419_1.jpg`, 드래그의 근거는 `20261017221918_1.jpg`다. 군은 서버 `armyColor`를 사용하고 없는 색은 만들어 넣지 않는다. 집단군 금색 tint `#806000`은 원본 화면의 관측색에 맞춘 표시값이며 원본 엔진 상수로 확인된 값은 아니다. 패널의 기존 폭·머리줄 간격과 해군 패널은 유지한다.

작은 사단 목록 스크롤바는 `unitview.gui/army_division_list_view/slider`가 참조하는 `GFX_scrollbar_vertical_bg`를 화살표 뒤에 그린다. 원본 `general_stuff.gfx`의 18×156px와 세로 경계 52px를 보존하여 위·아래 네모 틀을 함께 표시하고, 가운데만 늘린다. `scroll_up`·`scroll_down`·`scroll_drager`는 기존 16×16px를 유지한다. 이 자산은 기존 팩에 포함되어 추가 가져오기가 필요 없다. 공용 `UiScrollBar`(생산·연구·군수산업체 목록 등)와 결정 화면 스크롤도 같은 규칙(`UiScrollBar.background`, 위·아래 26 GUI 고정, 가운데만 늘림)을 쓴다. 이전처럼 3px 경계로 전체를 늘리면 위 칸의 밝은 구분선(원본 y16~18px)이 막대 중간에 흰 줄로 나타나므로 쓰지 않는다. `scroll_track`은 원본 `borderSize` 4px로 늘린다.

스크롤바의 위·아래 화살표(`scroll_up`·`scroll_down` 16px), 손잡이(`scroll_drager` 16px), 홈(`scroll_track` 12px)은 모두 좌우 대칭 그림이므로 배경(`scrollbar_vertical_bg` 18px)의 가로 중심선에 맞춘다(사용자 지시 2026-11-19). GUI2에서 배경 9GUI와 그림 8·6GUI의 차이는 반 GUI(1px) 이동으로 맞추며, 원본 GUI의 위 화살표 x2·손잡이 x-2처럼 1px 어긋난 위치는 쓰지 않는다. 공용 `UiScrollBar`, 결정·무기대여·의용군·무기대여 알림·지휘관 사단 목록 스크롤바에 같은 규칙을 적용한다. `IndustryProductionScrollTest`(`production-scrollbars` 묶음)는 GUI2 화면에서 위 화살표·손잡이·아래 화살표의 보이는 경계가 배경 중심에서 1px 이내인지 검사한다.

스킬 그림은 `affordable=false` 또는 `enabled=false && affordable=null`일 때 회색으로 표시한다. 비용을 낼 수 있지만 재사용 대기 중인 `affordable=true`는 이 조건으로 회색 처리하지 않는다. 서버의 `AbilityDetail.tooltip`이 있으면 색·빈 줄·취소 제목을 포함한 원문을 그대로 쓰며, 없는 구형 응답만 기존 원본 문구와 사용 가능 응답으로 보완한다. 글라이더·상륙 계획 가속의 회색 표시는 미구현 서버 효과의 구현을 뜻하지 않는다. `CommanderAbilityTest`는 여섯 상태 조합을 검사하고 `CommanderAbilitiesRenderTest`는 합성 비활성 입력의 실제 회색 픽셀과 서버 툴팁 보존을 검사한다. 합성 렌더 입력은 실제 소켓·전투·저장/재접속·멀티플레이 검증과 구분한다.

현재 실제 탐색 공격 응답의 툴팁에는 `dont_lose_dig_in_on_attack` 미번역 키가 남아 있다. 클라이언트는 이를 받은 원문대로 표시하며, 원본 한국어 패치와의 번역 일치를 확인한 항목으로 취급하지 않는다. 수치·효과 구현 여부와 표시 이름의 번역 누락은 구분한다.

현재 선택에 대한 별도 서버 사용 가능 응답이 새 거절 사유를 보내면 원문 아래에 추가한다. 원문에 이미 있는 사유는 반복하지 않으며, 세션·revision·선택 범위가 다른 응답으로 입력을 다시 허용하지 않는다. 원본 제목의 색이나 취소 표기가 바뀌어도 입력 검사는 표시 제목 대신 스킬 ID로 단추를 찾는다.

좌측 군·집단군 머리줄과 드래그 패널의 훈장은 서버 `LeaderTrait.active=false`일 때만 원본 그림에 붉은 tint를 곱한다. `true`와 구형 응답의 `null`은 원래 색이다. 클라이언트는 특성 유형이나 지휘관 계급으로 활성 여부를 재계산하지 않으며, 비활성 안내 `TRAIT_NOT_ACTIVE_WHEN_COMMANDING_AN_ARMY`는 서버 툴팁을 그대로 표시한다. 원본 `unitview.gui/traits_grid`와 `unitleaderwindow.gui/unit_leader_trait_entry`는 기존 특성 스프라이트를 사용하고, `unitleaderwindow.gfx`의 방어적 교리·완고한 방어가 정의에 별도 붉은 그림은 없다. 원본 `buttonstate.shader`의 색 곱하기 경로와 `20261017214437_1.jpg`를 근거로 기존 `drawTinted`를 사용한다. tint `#bf5050`은 사진에서 대조한 표시값이며 원본 C++ 엔진 상수로 확인된 값은 아니다. `CommanderTraitRenderTest`는 기존 입력에 합성한 비활성 상태·경고·드래그를, `CommanderSourceStateTest`는 새 서버 응답의 군 지휘 원수와 집단군을 각각 검사한다. 원본 사진의 붉은 훈장2개와 새 fixture의3개는 지휘관·특성 조합이 다르므로 같은 상태라고 주장하지 않는다.

드래그 복수 선택·카드 선택에 따른 명령 표시·작은 스크롤바·스킬 연결 배치·지휘력 부족 회색 그림의 집중 검증은 Java25에서 5m51s에 통과했다. GUI2 2560×1440·1280×720의 실제 PNG392장과 클라이언트 단위12개·protocol120개·격리 입력 생산자49개의 실패·오류·건너뜀0을 확인했다. 기존 리소스팩의 관련 그림13개는 유효 원본 DDS의 픽셀과 일치하며 팩을 다시 만들지 않았다. 이 결과는 해당 집중 검증에 한정한다. 이 집중 실행 당시 전체 검증은 대기했으며 후속 전체 통과 기록은 아래에 구분한다.  실제 운영 접속·멀티플레이·청취와 최신 서버의 지휘력 비용·특성 스킬 판정은 별도 검증이다.

지도 드래그가 둘 이상의 군 사단을 고르면 군 카드 순서대로 지휘관 패널의 윗부분만 쌓는다. 모든 군이 같은 집단군에 속할 때만 그 집단군을 맨 위에 붙인다. 미지정 사단은 이 스택에 패널을 만들지 않고 공통 집단군 판정에서도 제외한다. 따라서 같은 집단군의 군 둘 이상과 미지정 사단을 섞어도 집단군 패널을 유지한다. 군 하나의 드래그는 기존 전체 군 패널을 유지한다. 카드 클릭과 직접 사단 선택은 이 스택으로 바꾸지 않는다. 패널 X는 해당 군의 선택 사단만, 집단군 X는 해당 집단군 소속 군의 선택 사단을 모두 뺀다. 원본 `20261017221918_1.jpg`의 패널은 보이는 테두리가 이어진다. 기존 55GUI 머리줄의 투명 여백을 겹쳐 시작점을 52GUI씩 이동하며 배치·사단 도구줄·사단 목록은 그리지 않는다.

전투 계획 막대와 스킬 칸은 하단 군·집단군 카드를 선택한 동안만 표시한다. 지도 드래그·직접 사단 선택·미지정 사단·빈 선택에서는 그리기와 클릭 영역을 없애고 계획 확인 단축키도 차단한다. 스킬 칸은 명령 막대의 위치를 유지한 채 바로 위로 붙이며, 정지 화면에서 막대가 오른쪽으로 이동할 때 같은 좌표 기준을 따른다. 스킬 그림은 위의 비용 부족·미지원 상태 조건에 따라 기존 무채색 렌더러로 표시한다. 툴팁과 서버 요청·거절 처리는 유지한다.

사단 목록은 원본 `GFX_scroll_track` 12×12(모서리 4px), `GFX_scroll_drager` 16×16, `GFX_scroll_up`·`GFX_scroll_down` 16×16을 사용한다. GUI2에서 폭과 손잡이·화살표 크기는 원본 픽셀을 유지하고 트랙 중앙만 목록 높이에 맞춰 늘린다. 손잡이는 표시 비율로 확대하지 않으며 맨 위에서 ▲ 아래에 붙는다. 군·집단군·미지정 목록이 같은 렌더러와 휠·트랙 클릭·손잡이 끌기를 사용한다. 아홉 스킬 `force_attack`, `last_stand`, `staff_office_plan`, `extra_suplies`, `siege_artillery`, `faster_naval_invasion_planning`, `probing_attack`, `glider_planes`, `makeshift_bridges`의 원본 GFX 그림은 기존 팩에 있으며 서버 목록 순서대로 채운다.

집중 재현은 Java25에서 `:client:test --tests dev.hoi.client.map.commander.CommanderPanelOrderTest --tests dev.hoi.client.map.commander.CommanderGroupsTest --tests dev.hoi.client.map.commander.CommanderCardDragTest :protocol:test`와 `runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp39/r12-protocol/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp39/r12-protocol/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`이다. `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`를 지정한다. r12의 독립 검증 사본에 선택적 affordable 필드가 추가된 공유 파일만 동기화하며, 비용·특성 스킬의 최신 서버 구현 검증을 뜻하지 않는다. 드래그 입력은 실제 화면을 거치되 사단 상세 응답·지휘력 부족은 합성 입력이다. 운영 접속·멀티플레이와 필터 없는 전체 검증은 별도다.

육군 마지막 배치 보정은 팩 `python scripts/build_pack.py` 후 Java25·r12 고정 서버 사본으로 필터 없는 `test runGameTest remapJar`를 27m 16s에 통과했다. 실제 PNG 2222장, 클라이언트 JUnit 122개·protocol 119개·자동 입력 생산자 46개에서 실패·오류·건너뜀0이다. 클라이언트 단위 검사·생산자는 재실행했고 protocol 결과는 재사용했다. 집중 검사는 5m 38s·PNG359장에 통과했으며, 그 이후 새 전구 버튼의 마지막 높이·세로 위치 보정은 이번 전체 실행으로 검증했다. 전투 수 0 숨김/양수 배치, 올리브 전구 배정 줄, 하위 군 줄 높이, 집단군 소속 군을 선택했을 때의 새 전구 버튼, 수송 캡슐 폭과 특성 오른쪽 여백을 검사한다. 두 해상도 GUI2 원본 비교는 CMP-34의 첨부와 `army-stage-review` 문서로 제공한다. 원본 전체 픽셀 일치 승인과 실제 소켓·저장/재접속·멀티·직접 청취는 별도이며, 해군·공군·핵·전쟁 목표 화면은 이 육군 단계의 완료 범위가 아니다.

최신 사용자 보정은 집단군 머리줄의 군 수/한도 글자(예: 3/5)만 2GUI 위로, 군·집단군 특성 훈장과 호버 영역을 직전 위치에서 추가2GUI 위로 옮기고, ▼9개를 각각의 열 가운데 배치한다. 칸 경계·아이콘 크기·HQ 위치와 훈장의 서버 순서/겹침 규칙은 유지한다. 아래 26m51s 전체 통과 기록은 이 추가 보정 전 결과다. 이 추가 보정의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 5m 45s, 사용자 승인 슬롯의 필터 없는 전체 `test runGameTest remapJar`는 27m 32s에 통과했다. 전체 실제 PNG2222장을 생성했다. 첫 집중8m12s는 숫자 이동 전 컴파일본으로 별도 보존한다. 기존 합성12특성의 최상단 툴팁·순서/크기와 선택·스크롤·좌우/반복 입력 회귀를 유지한다. 실제 소켓·저장/재접속·멀티·직접 청취와 공식 가 시각 리뷰는 별도다.

직전 HQ 틀 +2GUI, 특성 −1GUI, 군·집단군 공통 폭과 목록 경계·스크롤바·연속 배경, 부대 상자 −1GUI 및 지역 사령부 원본 bus 그림을 포함한 필터 없는 전체 `test runGameTest remapJar`가 Java25·r9 서버 사본으로 26m 51s에 통과했다. GUI2 두 해상도를 포함한 전체 실제 PNG2222장을 생성했다. 직전 육군 집중 검사는 5m 38s 통과했으며 변경 없는 단위/생산자 검사 결과는 재사용했다. 이번 수정 묶음의 첫 전체 실행은 창 크기 변경 검사의 옛 축소 배율 가정에서 실패했다. 표시 코드를 바꾸지 않고 검사 여백을 최소 배율1에 맞춘 뒤 재검증했으며 실패·중단 기록도 보존했다. HQ 버튼의 배경·테두리·호버만 이동하고 아이콘·텍스트는 유지한다. 운영 접속·저장/재접속·멀티·직접 청취와 실제 전투 전구 툴팁은 이 렌더 검증에 포함하지 않는다. 원작 기본 색 배정·최종 shader·자동 편제 그림 규칙의 기존 미확정 사항도 남아 있으며 공식 가 시각 리뷰는 별도다.

카드와 전구 줄은 공통 선택 안내·이름·병과 수·설명 뒤에 서버 `Card.tooltip`을 그대로 붙인다. 기술/경험, 지휘관 효과, 원수 부분 혜택의 색·아이콘·순서·정밀도를 다시 계산하지 않는다. 단독 `---` 구분선은 기존 원문 렌더러에서 빈 줄로 바꾸며 초상 약력 `Card.detail`과 분리한다. 상단 장군·원수 특성은 서버가 배경 특성을 뺀 `traitDetails`를 그대로 그린다. 기술 상한9와 능력치 상한10, 9레벨의 가득 찬 경험 막대는 서버 표시값을 따른다. 프로토콜 확정 SHA-256은 `bc387936630d3dc0a6f310e8fd50d1f50b64fea1ee8c54bdb5f262ebf2a67d6a`다. 최종 계약의 r8 격리 서버 사본으로 육군 집중 검사가 8m 15s에 통과했다. GUI2 두 해상도 PNG351장, 클라이언트122·protocol119·자동 입력 생산자49·별도 표시/기술 상한 검사5개에서 실패·오류·건너뜀0을 확인했다. 변경 없는 단위 테스트 성공 결과는 재사용했다. 첫 집중 검사 통과 후 추가 장면 실행은 입력 생성 중 실행 중단되어 새 로그로 재개했다. 이 집중 검사 이후의 전체 검증 결과는 이 절 첫 문단을 따른다. 실제 소켓·저장/재접속·멀티·직접 청취와 실제 전투가 있는 전구 툴팁은 미검증이다.

최신 가 계약의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`가 5m 16s에 통과했다. GUI2 실제 PNG323장, 클라이언트 JUnit122개·protocol119개·격리 생산자47개·추가 표시 생산자1개에서 최종 실패·오류·건너뜀0을 확인했다. 변경 없는 성공 결과는 재사용했다. 집단군 클릭/Shift 직접 해산, 우클릭·반복 차단, 전구 우선권 세 요청과 NORMAL/LOW/HIGH/MIXED 응답 표시, 선택하지 않은 카드의 계획·지역 방어·훈련, 병과 글리프와 두 해상도 툴팁 경계를 검사했다. 처음 두 실행의 시험 조건/화면 재생성 대기 실패와 글리프 리소스 크기 수정 기록은 보존했다. 최종 실행은 정상 종료했다. 전체 클라이언트 검증·실제 소켓·저장/재접속·멀티·직접 청취는 이 결과에 포함하지 않는다. 표시용 View는 실제 시험 차원과 running=true만 치환하며 서버 원본 입력을 보존한다. r7 생산자 사본의 복사 시점1439파일은 전후 해시 변경0건으로 고정했다.

군·집단군의 사단 목록은 `unitview.gui/army_division_list_view`의 `GFX_tiled_window_1b_thin_border`를 도구줄부터 목록·스크롤바 끝까지 이어 그린다. 테두리와 중앙 영역은 원본 크기로 반복해 늘어난 폭의 우측 상단에 회색 장식이 늘어나지 않게 한다. 군·집단군 폭은 현재 카드들의 병과 요약에 필요한 최대 폭을 공유하며 추가 폭은 이름 열에만 준다. 목록 배경은 첫 ▼ 왼쪽−3px부터 마지막 ▼ 오른쪽+20px까지이며 오른쪽에는 스크롤바 자리만 남긴다(GUI2). 카드·HQ 행·목록은 배경 사이로 지도가 보이지 않게 이어진다. 선택 수 뒤에는 원본 `GFX_unit_list_header`를 사용한다. 정렬 칸은 x=0,19,38,95,121,141,171,333,357,388 경계에서 반올림한 끝 좌표를 공유해 틈을 없애며, 화살표는 최신 사용자 지시에 따라 각 칸의 좌우 중앙에 둔다. 첫 조직력·내구도 칸은 `division_list_column_button`의 점 두 개가 있는 frame_2다. 부대 그림 상자만 기존 위치보다 1GUI 왼쪽에 둔다.

지역 사령부의 `unit_hud/headquarters`는 TFR `gfx/interface/counters/divisions_large/unit_bus_icon.png`의 첫 76×42 프레임을 사용한다(`GFX_unit_bus_icon_medium`, 152×42 원본의 2프레임 중 frame_0). 목록의 큰 그림과 카드 병과 요약 모두 `commander/commands/unit_bus_icon_medium/frame_0`에 연결한다. 원본 SHA-256은 `a66bddf2a6317c1dbc9c50f4d67c18f86c117fc4feffef73db13ae8b1e654e00`이며 팩 provenance에 입력·출력 해시를 보존한다. 이 일반 사단의 그림과 군 전용 HQ 배치 행은 별개다.

전구 첫 줄에는 집단군과 독립 군을, 집단군의 펼친 줄에는 월계수·군 수와 소속 군을 표시한다. 집단군 전체 선택은 원본 `theatre_dropdown_bottom` frame_1의 초록 바탕과 소속 군의 선택 휘장을 사용하고, 개별 군 선택은 그 군만 강조한다. 군 목록 모드에도 집단군 머리줄을 유지해 같은 전환 단추로 사단 목록에 돌아온다. 집단군에는 HQ 행을 표시하지 않는다. 현재 서버에는 별도 HQ 부대가 없으며 `지역 사령부`·`Regional Command` 같은 보통 사단을 이름이나 대대로 제외하지 않는다. 초상·특성·HQ·편제·머리줄 도구 단추의 원문 툴팁은 색과 단축키 안내, 빈 줄을 보존한다.

`CommanderArmySourceReviewTest`는 `HOI_ARMY_REVIEW_PREVIEW`의 현재 서버 출력으로 두 해상도 GUI2의 지휘관/HQ·세 차량화 단계·편제별 툴팁·집단군 군/사단 목록·전구 선택을 촬영한다. 이 입력은 기존 합성 경계 회귀와 구분한다. 생산자 [ArmyReviewTest](../scripts/fixtures/ArmyReviewTest.java)는 실제 한국 OOB의 사단들을 시험용 세 군으로 나누고 기존 서버 배정 API로 지휘관·원수를 배치한다. 이름·편제·능력치·특성·HQ·문구·휘장은 서버 표시 로직에서 생성하며 원본 사진의 날짜·숫자로 바꾸지 않는다. 시험 편성은 실제 캠페인 세이브가 아니다.

추가 `settled` 입력은 시험 상태를 메모리에서 복제하고 지휘관 이동 대기만 해제하여 장군 효과와 원수 부분 혜택 전체를 검사한다. `skill-cap`은 그 복제 상태의 한국 지휘관 기술을 9, 경험치를 0으로 지정한 경계 입력이다. 네 능력치는 원본값을 유지한다. 두 입력도 서버를 통해 표시를 생성하며 실제 저장 파일을 수정하지 않는다. 렌더 검사는 카드/전구 본문 일치, 구분선의 빈 줄 변환, 특성 목록 일치, 기술 9(최대)와 경험 막대 1.0, 두 해상도 화면 경계를 검사한다.

재현할 때 현재 HOI의 소스·콘텐츠를 별도 폴더로 고정하고 위 생산자를 그 사본의 `fabric/src/test/java/dev/hoi/fabric/army/`에 복사한다. 원래 서버 작업 트리는 변경하지 않는다. Java25에서 사본의 `.\gradlew.bat :fabric:test --tests dev.hoi.fabric.army.ArmyReviewTest -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 실행하면 `fabric/build/army-review-fixtures`에 아홉 입력이 생긴다. 세 추가 입력은 실제 서버 계획 API로 만든 실행 대기·지역 방어·훈련 상태다. 팩 빌드 후 클라이언트에서 `HOI_ARMY_REVIEW_PREVIEW`를 그 폴더로 지정하고 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=<고정 사본> -PhoiProtocolSource=<고정 사본>/protocol/src/main/java --console=plain --no-daemon`을 실행한다. 기본 생산자·프로토콜 출처 검증은 그대로 수행한다. 이 환경 변수가 없는 일반 실행에서는 추가 원본 비교 장면을 촬영하지 않으므로 해당 검증을 완료로 보고하지 않는다.

집단군 머리줄의 문 단추는 서버 `GROUP_DISSOLVE`의 활성 상태·`UNASSIGN_ARMY_GROUP` 원문을 사용한다. 클릭은 `FORMATION/DISSOLVE_GROUP`, Shift+클릭은 `FORMATION/DISSOLVE_GROUP_ALL`, 선택은 집단군 ID 하나이며 확인창을 열지 않는다. 우클릭은 무동작이고 반복 요청은 기존 revision 제한을 따른다. 해산 결과는 서버 응답 이후에만 반영한다. HQ 배치 실행과 전투 계획 상세 연결은 각각 사·자 단계의 별도 작업이며, CMP-33은 육군 가 단계까지만 맡는다.

현재 대조 기준은 사용자 지정 2026-10-17 Steam 원본 92장과 2026-10-18 032953~033219·033348~033353, 별도 첨부 033538이다. 이전 10-02·10-04 육군 원본과 10-14 중점/결정 원본은 현재 대조 기준에서 제외한다. 육군 보완 뒤 전투 계획(명령 막대·스킬·지도), 중점 상세, 사건과 결정, 육군 사령부·장교단, 알림, 해군, 공군, 핵·전쟁 목표 순서로 검토한다. 국가·날짜에 따른 이름·숫자는 복제하지 않으며 스코어보드 날짜 줄·오른쪽 아래 1~9 지도 모드·진행 중에만 바닐라 HUD 숨김은 사용자 지정대로 유지한다.

아래 카드 줄은 집단군을 왼쪽에, 소속 군을 오른쪽에 배치한다. 집단군을 선택하면 소속 군의 실제 사단을 선택하며 원수와 장군의 역할을 구분한다. 선택 카드는 원본 `leader_group_frame_selected`와 `leader_army_group_frame_selected` 금색 테두리를 사용하고 실행 중지·실행 버튼은 선택과 무관하게 그 카드 자신의 `ACTIVATE`/`DEACTIVATE`가 있을 때만 표시한다. 집단군에 휘하 군 컨트롤을 합치지 않는다. `DEFENDING_AREA`는 클릭 없는 방패만 표시하고 `STOP_EXERCISE`는 훈련 중지·훈련 표시를 사용한다. 계획 평가도 실행 버튼이 있는 카드에만 겹친다. 집단군 카드는 지휘관 유무와 관계없이 군 수/상한을 표시한다. 군은 지휘관이 있을 때만 사단 수/상한을 표시하며 상한 초과는 빨강이다. 지휘관 없는 군은 사단 수만 표시한다. 카드 아래 계획 막대는 `Panel.planPower`만 사용하고 알 수 없는 진행률은 채우지 않는다. 집단군과 소속 군은 원본 `LG_army_group_bg`·`LG_army_mid_bg`·`LG_army_end_bg`로 이어지는 틀을 사용한다. 175×151 2프레임 집단군 atlas는 절반 UV를 사용한다. 현재 금색 tint `0xffb29854`는 원본 화면에 맞춘 표시값이며 엔진의 최종 shader 계산과 동일하다고 확정한 값은 아니다. 군 카드 양옆의 `group_marker2`만 서버 `armyColor`로 칠한다. 집단군 카드는 원본 214447처럼 금색 걸쇠만 유지하며 색 띠를 그리지 않는다. 펼친 집단군의 접기 삼각형은 왼쪽을 가리키는 frame_1이다. 색상 선택은 선택한 집단군 카드의 왼쪽 금색 손잡이에서 열고 서버 응답 뒤에만 갱신한다. 비선택 카드에도 원본 `execute_stop_small_bg` 빈 탭 둘을 표시한다. 새 군의 + 카드는 일반 군의 `army_frame`·초상 배경과 같은 크기를 사용한다. 프레임의 빈 칸에 클릭 동작을 추가하지 않는다.

군 카드의 X는 TFR `leadergroups.gui`의 프레임 (-1,-12)과 버튼 (62,-7)의 차이를 사용해 GUI2 프레임 기준 (31.5,2.5), 8×8로 놓는다. 집단군 접기는 프레임 (-1,-21)과 버튼 (73,-15)의 차이인 (37,3), 7×10이다. 사용자 지시에 따라 집단군 X와 군·사단 접기는 표시하지 않는다. 원본 `unitview.gfx`의 `group_marker4.dds`(85×29)와 `group_marker2.dds`(73×23)는 출처와 함께 보존한다. 최신 원본 214447에 따라 군의 `group_marker2`만 카드 색 띠로 사용한다. 이 표식은 전역 목록의 `theatre_army_group_shield`와 별개다.

펼침 패널은 TFR `interface/unitview.gui`의 `army_badge_view` 437×109와 `army_division_view` 390×33을 GUI2 기준 절반 크기로 배치한다. 사단 배지 (15,0)·숫자 (25,9), 제목 (66,10), 지휘관 계급·이름 (70,38)/(70,54), 원본 특성 시작 (302,48; 현재는 아래 능력치 배경 경계 지시에 맞춰 이동), 군 수 배지 (311,3/6)를 원본 기준으로 사용한다. 제목과 사단 이름은 왼쪽 정렬하며 긴 이름은 같은 글자 크기로 가로 스크롤한다. 지휘관이 없으면 원본 `UNIT_LEADER_NO_LEADER` 문구와 `CLICK_TO_ASSIGN` 안내를 각각 초상 오른쪽과 초상 프레임 안에 기본 글자 크기로 표시한다. `leaderEnRouteDays`는 원본 `UNITLEADER_IS_EN_ROUTE`/`UNITLEADER_IS_EN_ROUTE_SINGULAR`의 이름·날수에만 대입하여 초상 툴팁에 표시한다. 서버가 제공하지 않는 능력치·진행률은 임의로 채우지 않는다.

명령 막대의 공수 강하는 원본 `UNITS_VIEW_BTN_DRAW_PARADROP` 아이콘과 설명을 사용한다. `battleplantools.gui`의 원본 버튼 묶음·공백 순서를 유지하며 부유항 상륙 버튼의 71×32 두 프레임 그림은 atlas의 절반 UV로 표시한다. 원본에 없는 막대 오른쪽 위 X는 표시하지 않는다. 전선 제목은 `Panel.commandCaption`만 사용하며 서버가 보낸 “전투 계획”을 표시하고 빈 문자열은 빈칸으로 남긴다. 군 이름을 원본 전선 이름으로 간주하거나 사단 병력 평균을 계획 막대로 대체하지 않는다.

확정된 `CommanderHudPayloads` 계약의 `Panel.experience`는 지휘관 경험 막대, `divisionTypes`는 전체 병과 수, `manpower`는 전체 인력, `planPower`는 계획 막대에 연결한다. 사단 줄의 `MemberData.experienceLevel`은 원본 `unit_level` 0..4 프레임, `activity`는 대기·이동·전투·후퇴·훈련·수송에 해당하는 원본 상태 그림을 고른다. 오른쪽 원통 그림은 원본 `fuel_strip`이다. 마지막 그림은 상태 ! 대신 원본 `btn_army`의 `theatre_army_shield`를 사용한다. `MemberData.fuel`의 실제 연료 비율은 원통 그림 호버에 표시하며 null은 ?로 남긴다. 기본 게임 한국어 `FUEL`(core_l_korean.yml:1792)의 명칭을 사용한다. 비율의 소수 한 자리 표시는 HOI 표시 규칙이고 원본 연료 경고색 경계는 미확정이므로 기본 frame_0을 유지한다. `MemberData.insignia`가 있으면 소속 군 방패의 0..80 프레임으로 사용하고 null이면 원본 기본 프레임을 유지한다. 사단의 `attachedArmy`와 일치하는 군의 서버 `ARMY_INSIGNIA` 컨트롤로 `Open(ARMY_TOOLBAR,"ARMY_INSIGNIA",[군 id])`를 요청한다. 오른쪽 클릭은 요청하지 않으며 휘장은 서버 응답을 받은 뒤에만 바뀐다. 81개 선택지는 기존 지휘관 선택 목록으로 열리며 원작 선택창과의 시각 대조는 별도다. 서버에 없는 사단 훈장·편제 고유 그림은 만들어 넣지 않으며 `Member.icon`은 서버가 고른 병과 그림이다. 재배치·통폐합·수송 중량 칸·절반 선택을 분리하고 통폐합은 실행 계약이 없어 회색으로 표시한다. 원본 통폐합 문구는 설치된 한글패치에 해당 키가 없어 기본 게임 한국어 `CONSOLIDATE`·`CONSOLIDATE_TOOLTIP`을 사용한다.

이름은 Minecraft 기본 글자 크기로 왼쪽 정렬하고 긴 이름만 가로 스크롤한다. 선택 문구는 숫자만 초록색인 한 줄이며 원본처럼 오른쪽 전략적 재배치 버튼 경계에서 가린다. 초상 위 `CLICK_TO_ASSIGN`은 33×40 GUI 프레임 안에서 중앙 줄바꿈하며 크기를 줄이지 않는다. 집단군에 속한 군은 머리줄에 월계수와 해제 버튼을 표시한다. 부모 이름이 숫자로 시작할 때 그 숫자를 표시하고 임의 번호를 만들지 않는다. 해제는 `FORMATION/DETACH_ARMIES`, 선택 `[군 id]`, 빈 rays로 보낸다.

군 지휘관 아래 HQ 행은 `unitview.gui`의 `hq_window/empty_slot/deploy_dropdown`과 원본 `army_hq_bg`·`deploy_hq_button`을 사용한다. `DEPLOY_HQ_BUTTON_LABEL`·`DEPLOY_HQ_BUTTON_NO_COMMANDER`·`DEPLOY_HQ_BUTTON_COST`는 기본 한국어 core_l_korean.yml이며 설치 한글패치에 같은 키가 없다. 서버 `Panel.headquarters`의 state·label·text·cost·affordable·tooltip을 표시한다. `NO_COMMANDER`는 회색 틀과 어두운 회색 바탕에 굵은 배치/미배정 문구, `READY`는 금색 틀·철모·편제명·지휘력 비용을 사용한다. 최신 사용자 지정에 따라 배치 버튼의 배경·테두리와 호버 영역만 2GUI 아래로 옮긴다. 아이콘·텍스트와 바깥 HQ 행은 이동하지 않는다. 부족 비용은 빨강이며 서버 원문 툴팁을 보존한다. 원본 툴팁의 네 입력을 연결한다: `READY`에서 Shift+클릭은 `ARMY/DEPLOY_HQ`로 표시된 편제를 바로 배치하고, 클릭·Ctrl+클릭은 육군 사령부 편제 목록, 우클릭은 HQ 설계 화면(같은 `ArmyHeadquarters` 화면)을 연다. `NO_COMMANDER`의 클릭은 지휘관 배정 창을 연다. 레거시 null과 지휘관 없는 경우만 기존 commander 값으로 미배정 문구를 표시하며, 지휘관 보유 null은 자료 대기를 알린다. 비용 7이나 사령부 이름을 임의로 채우지 않는다.

최신 사용자 정렬 지시에 따라 사단 행의 조직력은 행 기준 x+1.5GUI, 내구도는 x+5.5GUI에 놓고 좁은 첫 ▼칸 하나를 함께 사용한다. 경고 열을 별도로 복원해 정렬 칸은 9개다. 원본 TFR unitview.gui의 column_heads 시작점 0/19/38/95/121/141/171/333/357px와 폭을 사용하며, 행 테두리 바로 오른쪽에 첫 칸을 맞추고 추가 패널 폭은 이름 열에만 배분한다. 경고 아이콘과 호버 영역은 x+12GUI에 놓는다. 숙련 휘장·진행 막대·명령 그림·이름·보급 통·군 표시는 대응 열에 맞추며 기존 그림 크기를 유지한다. 군·집단군·미지정 사단이 같은 행 렌더러를 사용한다. HQ 철모는 배치 라벨 끝과 편제명 시작 사이의 중심에 기존 38×21GUI 크기로 놓으며 위쪽 -3GUI 오프셋을 제거한다. 배경 특성은 서버가 상단 HUD용 traitDetails에서 제외한다. 클라이언트는 LeaderTrait의 icon/name/tooltip으로 종류를 추정하지 않고 받은 목록을 그대로 그린다. 지휘관 배경은 원본 그림의 오른쪽 밝은 영역만 늘리고, 어두운 능력치 칸은 레벨 상자부터 보급 아이콘 끝+3GUI까지만 유지한다. 특성 훈장은 그 오른쪽 밝은 바탕에서 서버 순서(장군→원수→기타)를 유지한다. 외교 국기 줄의 CountryFlagStack.left/visibleWidth를 재사용해 넘치면 간격만 줄이며, 나중에 그려진 오른쪽 훈장의 툴팁이 겹침 영역을 소유한다. 크기를 줄이거나 끝 특성을 생략하지 않는다. 합성 특성12개 장면은 실제 지휘관 보유 상태와 구분한다.

최신 9열 정렬·능력치 배경·특성 겹침은 Java25 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`에서 6m 3s에 통과했다. 실제 PNG359장, 클라이언트 JUnit122·protocol119·자동 생산자49·별도 표시 생산자11개가 실패·오류·건너뜀0이다. 변경 없는 성공 결과는 재사용했다. 이전 진행 막대 좌표를 검사하던 첫 실패를 새 열 위치로 수정했고 두 로그를 보존했다. 합성 특성12개에서 크기 유지·서버 순서·겹친 최상단 툴팁을 두 해상도로 검사했다. 최종 이 기록 당시 필터 없는 전체 검증은 별도 슬롯 회신 후 수행한다.

미지정 사단만 선택했을 때는 군 머리줄·지휘관·HQ·전투 계획 막대를 생략하고 사단 수 도구줄, 정렬 칸·목록, `UNASSIGNED_UNIT_DESCRIPTION` 안내를 표시한다. 목록은 2560에서 최대 11행이며 작은 화면에서는 안내 문구와 카드 줄이 겹치지 않는 범위로 줄인다. 도구줄부터 안내 상자까지 연속된 원본 어두운 배경을 사용한다. 도구줄 X는 전체 선택, 행 끝 X는 해당 사단 선택만 해제한다. 수송 중량은 선택 사단의 서버 값을 합하며 누락 시 사용자 지정대로 0이다. 편제 변경은 선택 사단 중 하나라도 서버 `templateBlock` 사유가 있으면 회색으로 표시하고 첫 사유를 그대로 보여 준다. 빨간 휴지통은 선택된 사단의 `unit/` ID만 `ARMY_TOOLBAR/DIVISIONS_DISBAND`로 보내 서버 확인창을 연다. 사단이 없으면 회색이며 요청하지 않는다. 군이 전혀 없는 View의 아래 카드는 생성 카드 둘만 남는다. 두 생성 카드의 호버는 `CREATE_GROUP_*`/`CREATE_ARMY_GROUP_*`, `OPERATION_NEW_GROUP`/`OPERATION_NEW_ARMY_GROUP`, `COMMAND_GROUP_DESC`/`COMMAND_ARMY_GROUP_DESC`의 원문을 사용한다.

집단군 목록 전환은 소속 군의 지휘관 머리줄을 나열하고 사단 선택 범위를 유지한다. 집단군 카드를 다시 누르면 사단 목록으로 돌아간다. 원본 실루엣 호버의 편제별 대대·지원 중대와 Shift 선택은 `Panel.templateSummaries`의 원문 툴팁·사단 ID를 사용한다. 오른쪽 전구는 `View.theaters`, 사단의 경고·적응 표시는 `MemberData`의 서버 값에 연결한다. 경험 휘장·노란 막대는 기존 `experienceLevel`/`experienceProgress`를 그대로 사용한다.

스킬 버튼은 선택한 실제 사단 ID로 `ABILITY/<스킬>`을 바로 요청하며 집단군은 `ABILITY/MARSHAL/<스킬>`을 사용한다. 왼쪽 반복 클릭은 새 revision 전까지 한 번만 전송하고 오른쪽 클릭은 조회창을 열지 않는다. 구형 `ARMY_ABILITIES` 요청도 같은 직접 경로를 사용하며 `ALL`은 조회창이나 임의 스킬 실행으로 이어지지 않는다. 화면 검사는 옛 능력 조회창 대신 이 입력 경로를 검증한다. 서버의 사용 조건·비용·효과 검증과 거절 이유를 유지하며 클라이언트가 성공 효과를 만들지 않는다.

스킬은 `ability_list_begin/mid/end` 배경 안에 아이콘만 그리며 개별 `nv_btn_select` 단추 틀을 덧씌우지 않는다. 아이콘의 원본 크기·위치, 클릭·툴팁 영역과 `ui.click` 소리는 유지한다. `affordable=false` 또는 `enabled=false && affordable=null`이면 같은 아이콘을 회색으로 그리고, 사용 불가 사유가 있으면 기존 어두운 덮개를 유지한다. 기본 스킬을 포함해 최대 4개를 서버 목록 순서로 표시한다. `CommanderSourceStateTest`의 GUI2 두 해상도 장군 탐색 공격·미지원 글라이더·집단군 추가 보급 장면과 기존 `army-layout` 입력 검사를 함께 사용한다. 이는 서버 테스트 응답의 렌더 검증이며 운영 접속·저장/재접속·멀티플레이·직접 청취의 증거가 아니다.

집중 검증 명령은 `test runGameTest remapJar -PhoiTestSuite=army-layout`이다. 동시 서버 변경 시 `-PhoiServerSource=<검증 HOI 사본 경로>`와 `-PhoiProtocolSource=<같은 사본의 protocol/src/main/java>`를 함께 지정한다. 권위 fixture와 프로토콜 일치 확인 후 GUI2 2560×1440·2560×1369·1280×720의 실제 렌더·선택·반복 클릭·오른쪽 클릭·해제·비활성 상태를 검사한다. 이 집중 검사는 필터 없는 전체 검사와 운영 다중 접속·직접 효과음 청취를 대신하지 않는다.

이 집중 범위는 카드·패널 입력과 원본 레이아웃이며 지도 드래그·운영 접속·다른 사용자 권한·실제 스킬 효과는 별도 검사 대상이다. 권위 fixture의 지휘관 경험은 0이고 집단군은 사단이 없으므로 인력·계획은 미상이다. 값이 없는 칸은 미상으로 유지하되, 사용자가 지정한 수송 중량 칸만 0으로 표시한다. 합성 계층의 긴 사단 이름·병과·경험 배지·인력·계획 0.75는 렌더 경계 확인용이며 캠페인 값이 아니다.

원본 `divisions_summary`의 “82”는 전체 사단 수이며 지휘관 경험치가 아니다. 왼쪽 `army_org_str_indicator`는 전체 사단의 조직력·병력 막대다. 초상 아래 특성·장교 역할 버튼과 오른쪽 아래 전체 선택·보급 차량화·목록 전환/철도포 그림을 표시한다. 실행 계약이 없는 버튼은 다른 기능으로 대신 실행하지 않는다. `traitDetails`의 실제 목록을 가로로 표시하며 일부 원본 훈장 그림 자체에 포함된 톱니는 별도 설정 버튼으로 취급하지 않는다.

사단 행은 `unitlist_unitinfo_button` 배경과 실제 병과의 `GFX_unit_<종류>_icon_medium` 첫 프레임을 사용한다. 병과 ID와 원본 GFX 이름이 다른 차량화·기계화·전차·포병 등을 명시적으로 대응한다. 경험 등급 2의 표식과 3/4의 월계관은 다른 원본 프레임이다. 노란 막대는 `unit_xp_bar`, 파란 막대는 `unitlist_bar_prepare`, 회색 그림은 `unitlist_dig_in_icon`이다. `MemberData.experienceProgress`는 현재 경험 등급 내부 진행 0..1, `planPower`는 계획 진행 0..1로 각각 노란 경험 막대와 파란 계획 막대를 채운다. `entrenchment`는 참호 단계이며 최대치 없는 단계 값을 비율로 쓰지 않는다. `entrenchmentProgress`는 서버의 현재/최대 참호 비율로 `dig_in_bonus`의 파란 막대를 채운다. null이면 틀만 유지한다. 육군 명령은 사용할 수 없어도 원본 밝기를 유지하며 툴팁·요청 검증으로 거절 이유를 알린다. `BPT_execute_*`·`BPT_cohesion_*`는 원본 GUI의 둘째 프레임을 사용한다.

원본 도구줄의 숫자 칸은 `transport_weight`와 `GFX_topbar_convoys`다. `Panel.manpower`는 인력 합으로 보존하되 수송 중량으로 표시하지 않는다. 선택 사단 행이 모두 도착하고 각 `transportWeight`가 있을 때만 실제 사단 중량을 합해 표시한다. 사용자 지정에 따라 누락 행·null 수송 중량은 0으로 표시한다. 이는 서버가 실제 중량 0을 확인했다는 뜻이 아니다. 서버 값은 원본 대대 `weight` 합이고 수송선 수가 아니다. `TRANSPORT_WEIGHT_VAL`은 원본에서 별도 소수 형식을 지정하지 않아 HOI는 합산 뒤 정수 반올림하며 원본 엔진의 반올림 방식과 같다는 주장은 하지 않는다. 호버의 전체 인력은 이 중량과 별도로 유지한다. 실행 화살표의 초록 체크는 `plan_evaluation` / `GFX_plan_value_strip`의 계획 평가다. 평가 없는 상태에 초록 체크를 만들지 않는다. 공세 버튼은 서버 `Panel.executionMode`의 `CAREFUL`/`BALANCED`/`AGGRESSIVE`를 원본 `bpt_execute_slow`/`mid`/`fast`에 대응한다. 비었거나 여러 카드의 자세가 다르면 기본 mid 프레임과 미상 안내를 유지한다. `Panel.organization`·`strength`는 선택 사단의 각 비율을 사단 수로 나눈 서버 평균이며 왼쪽 집계 막대를 채운다. 사단이 없으면 null이다. 차량화 우선순위는 현재 서버 `Panel.motorizationPriority`를 사용한다.

군 방패는 TFR `theatreselector.gfx`가 참조하는 원본 `gfx/army_icons/army_icons.dds`의 81개 33×32 프레임이다. `unitview.gui`의 `btn_army`는 같은 atlas를 참조하며 GUI의 frame=1은 추출된 frame_0이다. 자체 모양·색이 있는 휘장과 집단군 지도 RGB를 같은 값으로 취급하지 않는다. `plan_value_strip`도 실제로는 4프레임이며 frame_0=빈칸, 1=빨강 !, 2=노랑, 3=초록 ✓다. 서버 `Panel.planEvaluation` 0..2는 atlas의 1..3 프레임에 대응하고 null은 빈칸 frame_0을 유지한다. 원본 15×21 그림을 GUI2 기준 7.5×10.5 비율로 실행 화살표 위에 겹쳐 그린다. 원본에 없는 평가 결과를 실행 가능 상태에서 추정하지 않는다.

`motorization_level_toggle_strip`은 3프레임이며 `ARMY_MOTORIZATION_PRIORITY`는 클릭해 우선순위를 순환하는 설정이다. 원본 AI 갱신 주기(TFR 168시간)나 허브 최대 차량화 보너스(2.2)는 이 설정의 산출 공식이 아니다. 군은 서버의 `Panel.motorizationPriority`와 `ARMY_MOTORIZATION` 명령을 사용하며 응답 전에 표시 값을 바꾸지 않는다. 집단군의 혼합 상태·일괄 변경은 아직 별도 계약이 없다.

계획 평가의 원본 `PLAN_VALUE_*` 문구는 지상군·공중 우열, 강·해협·요새, 준비·미배치를 구분한다. 원작 `MAX_ESTIMATED_PLAN_UNITS_NOT_IN_PLACE_FACTOR=-0.6`은 미배치 비율에 곱하지만 전체 가중치와 색 경계는 확정되지 않았다. 서버의 현재 근사는 clamp(아군/적 전투력 − 1, −1, 1) − 0.6×미배치 비율이다. 전투력은 (연성+경성+방어+돌파)×병력×조직력 비율이며 적은 전선과 인접 프로빈스의 교전국 사단이다. 점수 <−0.1은 불리, −0.1..0.1은 위험, 그 위는 유리이고 적이 없으면 null이다. 전투력 구성과 ±0.1 경계는 HOI 근사이며 원본 공식으로 확인되지 않았다. 클라이언트는 점수를 계산하지 않고 서버 평가만 표시한다.

`operation_name`과 기본 제목 `BATTLE_PLANS`는 별개다. “만주”는 한글패치 `STRATEGICREGION_155`다. 원작 `common/names/00_names.txt`는 전선에 접한 적 프로빈스의 전략 지역별 작전명 풀이 국가 기본 풀보다 우선한다고 설명하며 `use_geographical_default_operation_names`도 정의한다. TFR의 같은 파일은 default의 작전명 필드를 제거했고 TFR/TFRKP KOR 이름 목록에도 작전명 풀이 없다. 복수 지역의 엔진 우선순위와 수동 이름·사진의 제목 선택 조건은 확인되지 않았으므로 집단군 전역 이름을 무조건 제목으로 쓰지 않는다. 서버가 보낸 최신 `commandCaption`은 선택 계획의 전선·목표 프로빈스를 전략 지역별로 세어 최다 지역 이름(동률이면 작은 지역 번호), 계획이 없으면 “전투 계획”이다. 이 서버 근사식과 확인된 원본 규칙을 구분한다.

사용자 지시에 따라 running이 true인 카드 끝을 화면 아래에 붙이고 false/null은 82GUI 위로 올린다. 집단군 카드 뒤에 소속 군을 배치하고 원본 프레임의 투명 여백을 제외한 폭을 누적해 보이는 테두리가 맞닿게 한다. `group_frame` 90×126의 좌우 여백은 각각 4px, `army_frame` 84×117은 각각 1px이며 원본 그림은 자르거나 바꾸지 않는다. 겹치는 투명 여백은 초상과 하단 숫자의 클릭 영역에서도 제외한다. 묶음 사이의 추가 간격도 없다. 긴 목록은 기존 페이지 버튼·휠로 이동한다. `CommanderGroupsRenderTest`는 두 해상도에서 인접 테두리와 끝 좌표를 검사하고 `hoi-army-hotbar-measurement-<해상도>.json`에 좌표를 기록한다. 핫바 사각형과의 교차는 숨겨진 바닐라 영역에 대한 참고값이다.

집단군 펼침 머리줄 왼쪽은 원본 반쪽 월계수 `theatre_army_group_shield`와 지휘 중인 군 수 `Card.count`를 표시한다. 오른쪽 칸은 군 수/한도다. 군 머리줄과 사단 끝은 `theatre_army_shield`를 유지한다. 문 모양 `uv_btn_unassign`은 집단군과 집단군에 소속된 군 머리줄에 표시한다. 이 군종 분기는 사용자 원본 두 장면의 대조를 반영하며 GUI 텍스트 자체에 실행 시 표시 조건이 적혀 있다는 뜻은 아니다.

지휘관이 없으면 원본 `UNIT_LEADER_NO_LEADER`의 빨간 “지휘관 없음”을 이름 칸에 표시하고 `CLICK_TO_ASSIGN`은 초상 안에서 기본 글자 크기로 줄바꿈한다. 두 키는 설치 한글패치에 덮어쓰기가 없어 기본 게임 `localisation/korean/unit_l_korean.yml`을 사용하며 명령 자산 보고서의 키 출처를 유지한다. 병과 요약은 `divisionTypes`의 수 내림차순·동률 asset ID 순으로 최대 11종을 놓고 원본 큰 병과 그림 아래에 수를 표시한다. 각 그림 칸은 숫자의 기본 글자 폭에 4GUI 간격을 더한 폭과 그림 폭 중 큰 값을 사용하고, 숫자를 그림 아래 가운데에 놓는다. 필요한 폭만큼 머리줄과 오른쪽 버튼 줄을 넓힌다. 합성 57개 사단의 10종 요약(15·12·7·7·5·4·2·2·2·1)은 원본 숫자 배치의 경계 검사이며 캠페인 수치가 아니다. 표시 이름은 `army-source-names.json`에 원본 OOB 경로·해시와 함께 보존한 KOR 57개 실제 이름이다. 이름 외의 순서·병과 조합·경험 등급/진행과 57/18 상한 장면은 합성 입력이며 같은 원본 캠페인 상태를 재현한 것이 아니다. 이 시험 전용 이름 파일은 배포 JAR에서 제외한다. 지도 fixture의 A/B/C 내부 ID는 유지하고 표시 이름만 `1 군`·`2 군`·`3 군`으로 구분한다.

설치 TFR `subuniticons.gfx`의 `GFX_unit_infantry_icon_medium` 원본 자체는 철모다. 차량화는 `GFX_unit_motorized_icon_medium`의 트럭, 공수는 `GFX_unit_paratrooper_icon_medium`의 해골이며 같은 원본 그림을 사단 행과 요약에서 사용한다. 그림에 추가 초록 tint를 곱하지 않는다. 서버 `VisualDivisionStacks.type`의 지도용 주력 병과 판정을 원본 엔진의 편제 그림 선택 공식으로 확정하지 않는다. 수동 편제 그림이 필요하면 별도 원본 GFX/asset ID와 그 그림 기준의 전체 집계 계약을 서버에서 받아야 한다.

`unitview.gui:344`의 머리줄 `army_item_bg`와 `:1663`의 사단 끝 `btn_army`는 모두 `GFX_theatre_army_shield`다. 군 머리줄은 이 atlas를 사용하고, 집단군은 원본 캡처의 반쪽 월계수에 맞춰 집단군 atlas를 사용한다. `army_icons.dds`의 기본 frame_0은 흰 방패, frame_80은 자체 색이 있는 톱니형 휘장이다. 이전 합성 입력의 insignia=80이 톱니를 표시한 것이며 상태 경고로 해석하지 않는다. 원본 캡처의 빨강/초록은 기본 방패에 군 색상을 적용한 것으로 보이나 `.gui`는 지휘관 유무·문제 상태에 따른 색 분기를 정의하지 않는다. 같은 파일 :453의 `army_color_button`은 `GFX_group_marker3` 색 선택 버튼이며, 설치 실행 파일의 문자열에도 같은 버튼 이름이 있다. 이 증거는 색 설정의 존재를 뒷받침하지만 기본 팔레트 배정 순서나 최종 shader tint를 확정하지 않는다. 군 휘장과 군 RGB의 서버 표시 계약이 없는 동안 임의 경고색을 만들지 않는다. `Panel.color`의 집단군 지도 RGB와도 구분한다. 확정된 서버 `Panel.insignia`와 `Panel.armyColor`를 군 머리줄에, `MemberData.insignia`와 `MemberData.armyColor`를 사단 끝 방패에 적용한다. null 휘장은 frame_0, null 색은 흰색을 유지한다. 집단군의 두 값은 null이며 집단군 RGB로 대신 칠하지 않는다. 군 색은 서버가 정한 원본 팔레트의 나라별 군 ID 순 배정이다. 이 기본 배정 순서와 전체 그림의 곱셈 tint는 HOI 규칙이며 원본 엔진의 기본 배정·shader와 동일하다는 주장은 하지 않는다. 군 색은 `Panel.palette`가 있는 군의 머리줄 방패를 왼쪽 클릭해 선택하며 `ARMY_COLOR`, `[군 id,"rgb/<0..16777215>"]`, 빈 rays를 보낸다. 기존 집단군 팔레트를 재사용하고 오른쪽 클릭은 전송하지 않는다. 새 revision 전 반복 요청을 차단하고 `Panel.armyColor`·`MemberData.armyColor` 수신 전에는 그림 색을 바꾸지 않는다. 원본 `army_color_button/GFX_group_marker3`의 (405,10),25×25px는 X 좌표와 겹친다. 설치 `hoi4.exe` SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`의 초기화 함수 `0x1416aed50`에서 `army_item_bg`(문자열 주소 `0x1429ea8d8`, 필드 +0x38)와 `army_color_button`(참조 `0x1416af655`, 필드 +0x80)이 `0x1416af082`·`0x1416af6a3`에서 같은 +0x16c8 핸들러에 연결됨을 확인했다. 이 정적 분석을 근거로 방패 입력을 연결했으며, 겹치는 별도 버튼의 실행 시 표시·재배치 조건과 원작 팔레트 창 배치는 미확정이다. 재현은 해당 SHA-256 실행 파일의 위 함수 구간을 `llvm-objdump -d --no-show-raw-insn --start-address=0x1416aed50 --stop-address=0x1416b05b8 <hoi4.exe>`로 읽는다. 원본 실행 파일은 수정하지 않는다. 수동 편제 그림과 자동 병과 선택 공식은 아직 미확정이다.

사단 0개여도 전략적 재배치·정지 등 도구줄을 유지한다. 군의 지휘관 아래 버튼은 전체 선택·차량화·철도포·부속 비행단 순서로 붙이고 집단군은 전체 선택·차량화·군/사단 전환 순서다. 차량화는 `army_motorization_priority_button_overlay`의 상태 프레임이며 집단군이라는 이유만으로 겹친 차량 프레임을 강제하지 않는다. 군의 `Panel.motorizationPriority` 0/1/2는 원본 말·트럭·겹친 차량 프레임과 대응한다. 왼쪽 클릭은 `ARMY_MOTORIZATION`, `[군 id]`, 빈 rays를 보내며 새 revision 전 반복/오른쪽 클릭은 전송하지 않는다. 집단군의 혼합 우선순위와 순환 명령은 별도 계약 대기다. 사단 목록과 정렬 열은 병과 요약 때문에 늘어난 머리줄 폭을 함께 사용하며 이름 열을 넓힌다. 스킬이 없을 때 목록을 명령 막대 바로 위까지 채운다. 스크롤바의 현재 크기·원본 트랙·손잡이 규칙은 이 절 앞의 작은 스크롤바 설명을 따른다. 실행 체크는 `Panel.planEvaluation`의 0/1/2를 빨강/노랑/초록 프레임 1/2/3으로 대응하고 null은 빈 프레임이다. 선택하지 않은 군 카드에도 빈 실행 버튼 바탕 둘을 유지한다. 카드 수는 초상 아래 칸 안에 고정 크기로 표시하고 계획 null/0 막대의 내부는 원본 화면의 거의 검정인 `#020305`이며 원본 둥근 `plan_frame`을 겹친다. 이 내부색은 화면 대조값이며 원본 progress shader를 복원했다는 주장이 아니다.

육군 머리줄·색상 입력·스크롤의 집중 검증은 팩 빌드 후 Java 25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 4분에 통과했다. 클라이언트 122개·protocol 119개·입력 생산자 46개는 실패·오류·건너뜀 0, 실제 PNG는 199장이다. 이번 작업의 첫 실행에서 JUnit/producer를 재생성했고 마지막 실행은 변경 없는 성공 결과를 재사용했다. 별도 생산자 사본은 r8 코드에 확정된 `CommanderHudPayloads.java` SHA-256 `ad7110f7ede4b5ab293fa60f7b6a367d51eed7640484a1ff12f66778c0530e6a`만 복사한 것으로, 최신 서버 구현의 검증이 아니다. 공유 소스 48개와 입력 provenance를 검사했다. Python 명령 자산 가져오기 7개 검사도 통과했다.

GUI2 두 해상도에서 회색 배치 행, 색 띠 없는 집단군 카드·펼침 화살표, 거의 검정인 빈 계획 막대, 머리줄과 같은 폭의 사단 목록, 명령 막대 직전까지의 좁은 화면 목록, 미지정 목록 최대 11줄·연결된 배경·스크롤바·수송 칸·닫기·행 X와 카드 바로 위 생성 툴팁을 확인했다. 기존 군 색·카드 드래그·선택·스크롤 회귀와 차량화 0/1/2 프레임·우클릭 무전송·revision 전 중복 차단·정확한 군 ID 요청을 검사한다. HQ 이름/비용/부족 상태와 이동 중 15일 문구는 새 필드를 명시한 합성 계약 입력으로 확인했다. 이 장면의 지휘관 상세/초상은 미제공이고 비용 7·진행률·수송 중량·경험 등도 합성이다. 57개 표시 이름만 원본 OOB 출처가 있다. 새로운 실제 서버 명령·저장·소켓·재접속·다중접속·직접 청취와 전체 클라이언트 검사는 미수행이다.

중간 실행은 199장 캡처 뒤 Fabric `TestSingleplayerContextImpl.close`에서 렌더 스레드가 `IntegratedServer.halt`를 기다리고 서버 스레드가 테스트 Phaser를 기다려 종료되지 않았다. 해당 테스트 프로세스만 종료하고 스레드 덤프·실패 로그를 보존했다. 최종 재실행은 정상 종료했으며 종료 대기 실행을 성공으로 계산하지 않는다. 최종 JAR SHA-256은 `79ba4c91df8a4837474f10350c2450f4aa219a1648d07c6cd01ed5113e2b6b2f`다. JAR와 내장 protocol JAR에는 프로젝트 모드 아이콘 외 게임 정적 이미지/음향, 서버 군사 코드, 시험 차원과 이름 fixture가 없다.

군 카드 X는 원본 `lg_disband_group_btn`의 군 해체 의미로 `FORMATION/DISSOLVE_ARMY [군 id]`를 직접 요청한다. 서버가 소유권·세션·revision을 검사하고 군을 해체하며 사단은 미지정으로 보존한다. 확인창은 열지 않는다. 기존 서버의 계획·지휘관·부속 비행단 처리와 비용 검증을 유지한다. 사단 삭제는 별도로 군·집단군·미지정 목록의 휴지통에서 `DIVISIONS_DISBAND [unit/id…]` 확인창을 사용한다. 군 해체는 지휘 명령 경로, 사단 삭제는 기존 Open/DialogView 경로이며 클라이언트에서 군·사단·선택 상태를 먼저 바꾸지 않는다. 미지정 사단 행의 X는 이미 군 소속이 없는 해당 행의 선택 해제이며, 패널 닫기와 함대 카드 X도 기존 선택 해제다. 편제 변경 활성 상태·후보, 경고/적응, 편제별 구성 툴팁과 전구는 아래 최신 표시 계약으로 연결되어 있다. 실제 HQ 배치 실행은 별도 서버 범위다. 따라서 이 검증은 가 단계 전체 완료나 재리뷰 제출이 아니다.

전구 설정은 `THEATER_SETTINGS/LOW|NORMAL|HIGH|MIXED`를 읽어 머리줄 왼쪽 원본 설정 창을 연다. 이름은 표시만 하며 전구 해체는 이 창의 `THEATER_DISBAND` 서버 확인 요청으로 보낸다. 우선권은 `FORMATION/THEATER_PRIORITY/LOW|NORMAL|HIGH`와 전구 ID 하나로 보내고 응답 전에는 선택 그림을 바꾸지 않는다. 원본 `div_priority_strip`의 비선택 0~2·선택 3~5 프레임이며 MIXED는 모두 비선택이다. X·Escape는 설정 창만 닫고 국가/차원/branch 변경은 팝업을 초기화한다. 공격·방어 컨트롤의 전체/문제 수를 표시하고 0은 회색, 문제 있음은 빨강, 그 밖에는 초록으로 칠한다. 색의 정확한 엔진 RGBA는 미확정이며 전투 이력 창은 차 단계다.

카드와 전구 줄은 같은 툴팁 생성 경로를 쓰며 병과 수에 기존 원본 그림을 키워드 글리프로 표시한다. 군 줄 우클릭은 선택한 사단의 `FORMATION/MOVE_DIVISIONS`이고 집단군 줄 우클릭은 입력하지 않는다. `Card.detail`은 초상 약력이며 기술·경험/원수 부분 혜택의 별도 전체 블록은 서버 `Card.tooltip`을 사용한다. 누락된 효과를 클라이언트에서 계산하지 않는다. 전체 클라이언트 검증 슬롯은 사용자 답변613d306e로 승인받았으나 전체 실행 전 남은 서버 계약 대기로 반환했다. 새 실행 때 동시 작업자와 슬롯을 맞추고 시작·종료를 알린다.

### 전투 계획 원본 대조의 남은 범위

현재 `CommanderPanels.actionbar`는 훈련/중지, 전선·공세·선봉·후퇴·지역 방어, 배정/해제·편집/삭제, 실행 방식 셋을 그린다. `CommanderOrders`는 기존 인증 세션·revision으로 `LAND/FRONT`, `OFFENSIVE`, `SPEARHEAD`, `FALLBACK`, `AREA_DEFENSE`, `ASSIGN_UNITS`, `EDIT_PLAN`, `DELETE_PLAN`과 `ARMY/*` 요청을 보낸다. 이 경로의 존재는 새 원본 221104~221223, 221820~221907과 시각·동작이 같다는 검증이 아니다.

새 대조에서는 모든 명령 툴팁의 색·마우스 조합 안내·금지 이유와 실행/결속도/HQ 근접도 팝업을 확인해야 한다. 현재 실행 방식 셋은 작은 버튼만 있고 원본 제목·틀이 없다. 결속도는 기존 조회 경로이며 원본 4버튼 팝업은 미구현이다. HQ 근접도 0/1/2/3/자유 팝업은 미구현이다. 사용자 지시대로 `UiCommandLabels.withoutShortcuts`로 키캡·키 설정 안내를 제거하며 이를 다시 추가하지 않는다. 공수와 부유항 상륙은 원본 그림만 있고 실행 계약이 없다. 서버가 기능을 제공하기 전 이를 성공한 명령으로 표시하지 않는다.

`Panel.abilities`의 ID 목록과 선택적 `abilityDetails`는 서로 다른 계약이다. 현행 `AbilityDetail.tooltip`·`enabled`·`affordable`가 있으면 서버 원문과 가능 여부를 사용한다. 대대 수·기본 비용·배율·보유 지휘력 등 서버가 보내지 않은 비용 상세를 클라이언트가 계산하거나 스크린샷에서 채우지 않는다.

지도 `PlanOverlay`는 `MapAtlasScene.Page`의 박스와 `MovementQuad`를 받아 `SceneAssembler`→`SceneMesh`로 그린다. 곡선 라벨의 원문·경로, 선 종류/실행 상태, 빗금 영역의 경계에 해당하는 명시적 데이터는 없다. 기존 박스로 그린 선이 원본 굽은 라벨·빗금·초록 실행 화살표·점선과 동일하다고 볼 수 없다. 선택 사각형과 사단 행 상세 요청은 존재하지만 원본 주 툴팁의 현재 선택 요약·장비 수·진입 금지 이유 및 사단 카운터의 손실 문단은 별도 서버 표시 계약과 실제 지도 렌더 검증이 필요하다.

스킬 도중 사단 증가 시 효과 감소, 미표시 기본 재사용 대기, 실행 방식/결속도 이동 규칙, 사상자 전쟁 지지도 감소의 엔진 계산은 아직 분석하지 않았다. 공개 defines의 대대당 비용·168시간·근접도 표와 이 미확정 계산을 구분한다. 원본 실행 파일의 해시·함수/주소·호출 조건을 확인한 근거 없이 원작 공식이나 근사치를 확정하지 않는다.

### 전략 지도 HUD와 키캡 안내

`CommanderSelectorHud`는 유효한 국가·차원의 지휘 카드 줄을 표시하고 서버 `View.running == true`일 때만 바닐라 핫바·관전자 메뉴·체력·방어력·허기·공기·탈것 체력·경험치/로케이터 정보 막대·경험치 레벨·선택 아이템 이름을 숨긴다. 지도 안팎 모두 같은 조건이다. false/null(정지·미시작·이전 서버)에서는 기존 Fabric HUD 요소를 호출하고 카드 줄·명령·팝업을 82GUI 위로 올린다. true로 바뀌면 다시 화면 아래에 붙이며 카드가 없어지거나 카드를 그리지 않는 화면에서도 바닐라를 유지한다. 좁은 정지 화면의 명령 막대는 왼쪽 패널 오른쪽으로 이동해 버튼 영역이 겹치지 않게 한다. 서버는 1초마다 상태를 보내므로 이 표시는 수신한 상태를 따르며 클라이언트가 시계를 추정하지 않는다. 플레이어 GUI 설정을 바꾸지 않는다. 오른쪽 안내는 전략 지도 차원에서 실제 핫바의 `hoi:map/selector/` 아이템만 슬롯 순서대로 표시한다. 원본 외부 팩의 `nv_btn_select` 테두리와 숫자를 합쳐 키캡으로 그리고, 실제 아이템 모델과 서버가 지정한 금색/흰색 이름을 사용한다. 원본 HOI4의 목록을 복원한 기능이 아니라 사용자가 추가한 안내다. 선택한 슬롯 강조는 추가하지 않는다. 화면 오른쪽 아래 여백 6GUI에 붙이고 카드 제어와 가로 범위가 겹치면 카드 위로 올린다. 키캡·모델·이름 및 1→9 순서는 유지한다.

육군 지휘 영역은 Minecraft 기본 글꼴을 읽을 수 있도록 최소 GUI2 기준 1배 크기를 유지한다. 글자를 해상도 비율로 축소하지 않고 패널·행·칸을 함께 키운다. 긴 사단명은 같은 크기에서 가로 스크롤하고 미배치 안내만 초상 안에서 줄바꿈한다. 선택 수는 한 줄로 그린다. null/0 계획 막대는 어두운 바탕과 원본 `plan_frame` 둥근 테두리를 표시하고 실제 진행률이 있을 때 초록색을 채운다. 스킬은 원본 `abilitylist.gui` 첫 버튼 위치 (29,8)px와 36px 간격을 GUI2에 대응한 (14.5,4)+18px 고정 칸을 사용한다. 명령 막대 첫 묶음의 넷째 빈칸은 이 스킬 컨테이너가 아니다. `Panel.abilities`의 원본 정의 순서대로 지휘관 보유 스킬을 왼쪽부터 채운다. null은 미제공, 빈 목록은 0개이며 두 경우 모두 임의 버튼이나 빈 스킬 컨테이너를 그리지 않는다. 복수 지휘관은 첫 서버 목록 순서의 공통 스킬만 표시한다. 집단군 선택은 원수 목록을 사용하고 사용 가능 여부·거절 이유는 별도 권위 검사로 유지한다. `CommanderAbilitiesRenderTest`는 합성 0·1·4개와 null 목록을 두 해상도에서 렌더하며 실제 스킬 효과 검증과 구분한다.

`CommanderSelectorHudTest`는 일반 차원과 합성 지도 차원에서 true→false→null→true 상태의 HUD와 카드 위치를 두 해상도로 검사하고 카드 제거 후 복원도 캡처한다. 별도 합성 `hoi:atlas_ui_test` 차원을 테스트 리소스로만 등록하고 통합 테스트 서버의 플레이어를 잠시 이동해 실제 HUD 숨김과 아홉 아이템 모델을 렌더한다. 테스트 종료 시 차원·좌표·인벤토리를 복원한다. 운영 지도·사용자 저장·다중 접속 검증과 구분한다.


### 육군 목록 하단과 지휘 단추

집단군 사단 목록은 현재 서버 HUD의 `view.cards()`에서 해당 집단군 소속 군을 하단 카드의 왼쪽 순서로 모으고, 각 군의 기존 사단 ID 순서 목록을 이어 붙인다. 어느 소속 군에도 대응하지 않는 행은 기존 순서를 유지하여 맨 뒤에 둔다. 단일 집단군 선택과 복수 카드 선택의 집단군 행에 같은 규칙을 적용한다. `CARD_ORDER`, `MOVE_ARMIES`, `DETACH_ARMIES`의 소속·순서 응답이 반영되면 최신 카드 목록을 기준으로 다시 계산하며, 군·미지정 목록 순서, 선택 수와 선택 상태는 유지한다.

관련 단위 검사는 `:client:test --tests dev.hoi.client.map.commander.CommanderPanelOrderTest --tests dev.hoi.client.map.commander.CommanderGroupsTest --tests dev.hoi.client.map.commander.CommanderCardDragTest`다. 기존 팩·r12 사본을 쓰는 육군 집중 검사는 이 명령 뒤에 `runGameTest -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 붙인다. Java25와 `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`가 필요하다. `CommanderArmySourceReviewTest`는 실제 렌더에서 57개 사단의 맨 위·19/20행 경계·38/39행 경계·맨 끝, 재정렬 응답과 복수 카드 선택의 순서를 두 해상도 GUI2로 검사하고 방패 색과 소속 군을 대조한다. 고정 서버 입력과 합성 재정렬 응답을 사용하므로 실제 소켓·저장/재접속·멀티플레이 검증을 대체하지 않는다. 이 변경의 범위에는 팩 재빌드·필터 없는 전체 검사·운영 설치가 포함되지 않는다.

집단군 순서 변경의 최종 집중 검사는 5분45초에 통과했다. 관련 단위 11개 실패·오류·건너뜀0, 실제 PNG381장이다. 두 해상도에서 빨강19행→청록19행→보라19행과 재정렬·복수 선택을 검사했다. 직전 배치 대비 머리줄·57개 선택 수 영역의 변경 픽셀은0이고, 2560×1440의30행·마지막 행/끝 y1262/1268, 1280×720의4행·y378/384와 하단6px를 유지한다. 첫 실행은 새 테스트가 스크롤바 끝점 바깥을 눌러 작은 화면 마지막 행 검사에서 실패했다. 클릭을 경계 안쪽으로 고친 뒤 재실행했으며 실패 로그도 보존했다. 실제 네트워크·저장/재접속·멀티플레이와 필터 없는 전체 검사는 이 결과에 포함하지 않는다.

군·집단군·미지정 사단 목록은 마지막 사단 줄 아래에 어두운 바탕 5px와 끝선 1px, 합계 6px(3GUI)를 둔다. 군·집단군 목록 오른쪽 스크롤바 칸도 같은 y에서 끝난다. 원본 `20261017214326`의 y1209/1215와 `20261017214419`의 y1263/1269를 기준으로 하며, 앞선 2px 해석을 대체한다. 넓은 화면에서는 기존 표시 행 수를 유지하면서 하단 허용 범위를 4px 늘리고, 좁은 화면에서는 여백을 먼저 확보한 뒤 행 수를 계산한다. 미지정 목록의 기존 안내문은 보존하고 마지막 행 아래 6px 뒤에 표시한다.

펼친 집단군의 `lg_army_group_bg`·`lg_army_mid_bg`·`lg_army_end_bg`는 윗부분을 자르지 않는다. 판은 집단군 정지·진격 칸의 위쪽부터 마지막 소속 군 카드까지 이어지며, 소속 군의 칸 위로 판과 오른쪽 사선 모서리가 보인다. 정지·진격 칸은 카드에 붙은 위치를 유지한다.

판 복구와 6px 보정은 기존 팩·r12 고정 서버 사본을 재사용한 Java25 `:client:test --tests dev.hoi.client.map.commander.CommanderGroupsTest --tests dev.hoi.client.map.commander.CommanderCardDragTest runGameTest -PhoiTestSuite=army-layout`로 5분55초에 통과했다. 소스 속성과 `HOI_ARMY_REVIEW_PREVIEW`는 위 동일 사본 재현 규칙을 따른다. 단위 8개 실패·오류·건너뜀0, 실제 PNG369장이다. GUI2 2560×1440에서 군19개의 마지막 행/목록 끝은 y932/938, 집단군57개는 y1262/1268이며 오른쪽 칸도 같은 y에서 끝난다. 1280×720에서는 각각 y354/360과 y378/384다. 미지정 목록·직접 선택·지도 드래그는 큰 화면11줄(y512/518), 작은 화면10줄(y478/484)과 6px를 유지한다. 군·집단군 머리줄은 CMP-36 대비 두 해상도 모두 변경 픽셀0이다. 앞선 픽셀 실행의 끝선 잘림/행 수 감소를 수정했고, 5분49초 중간 통과 뒤 미지정 여백 줄의 폭을 테두리 안으로 다듬어 최종 재검증했다. 필터 없는 전체 검사·팩 재빌드·remapJar·운영 접속·저장/재접속·멀티플레이·직접 청취는 이번 검사에 포함하지 않는다. 합성 집단군 계획 칸은 표시·입력 검사이며 실제 집단군 계획 실행 증거가 아니다.

군·집단군 목록은 마지막 표시 행까지 배경을 늘리고, 왼쪽 패널과 명령 막대가 가로로 겹치지 않는 넓은 화면에서는 카드 위 공간까지 사용한다. `unitview.gui`의 `army_division_list_view` 높이는 원본 엔진에서 정하므로 GUI 파일의 430px 값을 전체 화면의 고정 한도로 쓰지 않는다. 넓은 화면 하단은 사용자 제공 20261017214419의 관측값에 맞추며, 좁은 화면에서는 기존 명령 막대·스킬 영역 위에서 멈춘다. 미지정 사단이 포함된 직접·Ctrl 전환·지도 드래그·혼합 선택은 최대 11줄이며 선택 자체와 서버 명령 대상은 잘라내지 않는다.

지휘관 단추 줄은 맨 오른쪽 부속 비행단/군·사단 전환 단추 위치를 기준으로 왼쪽 단추를 붙인다. 군·집단군 머리줄 닫기 X와 아래 군 카드 해산 X는 최신 사용자 정정에 따라 이 보정 전 위치에서 왼쪽·아래 각각 화면 1px 이동한다. 이전 1GUI 이동과 누적하지 않으며 실제 GUI 배율의 역수로 그림과 클릭 영역을 함께 보정한다. 아래 집단군 카드 펼치기·접기는 1GUI 왼쪽으로 옮긴다. 함대 카드의 접기 버튼은 제거하고 X를 군 카드와 같은 상대 위치에 놓으며, 왼쪽 클릭은 선택 해제·오른쪽 클릭은 무동작을 유지한다. 아래 정지·진격 칸은 카드 위 경계와 카드 폭에 맞추며 원본 그림과 기존 서버 명령·비활성 이유를 사용한다.

이 보정은 팩 재빌드 후 Java25·r12 고정 서버 입력으로 `:client:test --tests dev.hoi.client.map.commander.CommanderGroupsTest --tests dev.hoi.client.map.commander.CommanderCardDragTest runGameTest remapJar -PhoiTestSuite=army-layout`를 실행해 5분42초에 통과했다. 단위 테스트 8개 실패·오류·건너뜀 0, 실제 PNG369장이다. `HOI_ARMY_REVIEW_PREVIEW`와 `-PhoiServerSource`·`-PhoiProtocolSource`는 위의 동일 사본 재현 규칙을 따른다. GUI2 두 해상도에서 기존 좌우·반복 입력·드래그 검사와 미지정 직접/Ctrl/혼합/지도 드래그의 최대11줄, 함대 접기 부재와 X 선택 해제를 검사했다. 좁은 화면에서는 공간에 맞춰 11줄보다 적게 표시한다. 군19개 장면의 마지막 행/배경 중앙 아래 끝은 y932/934px, 집단군57개 장면은 y1262/1264px다. 집단군 정지·진격 표시는 서버 군 계획 입력을 집단군에 적용한 합성 장면이며 실제 집단군 계획 실행 증거가 아니다. 필터 없는 전체 검증·운영 접속·저장/재접속·멀티플레이·직접 효과음 청취는 이번 집중 검증에 포함하지 않는다.

### 지휘 카드 수평 드래그

카드 초상을 왼쪽 버튼으로 누르고 가로로 3GUI 이상 움직이면 드래그한다. 군·집단군 카드는 한 동작으로 현재 페이지의 카드 줄 전체를 지나 왼쪽 집단군 생성 칸부터 오른쪽 새 군 `+` 칸까지 움직이며, 미리보기와 놓기 판정은 같은 범위를 쓴다. 세로 좌표는 고정한다. 놓으면 가까운 카드 칸에 끼우고 나머지 카드는 밀어 표시한다. 집단군은 펼친 소속 군 블록과 함께 움직이며 접힌 소속 군도 전체 순서 요청에서 보존한다. 마지막 집단군 뒤로 가는 집단군의 미리보기 빈칸은 소속 없는 군 앞에 둔다. 집단군을 설정하면 생기는 반투명 배경(`lg_army_group_bg`·`lg_army_mid_bg`·`lg_army_end_bg`)과 `bands`는 드래그 중에도 드래그 전 배치에 고정하고 카드만 움직인다. 클릭만 하고 떼면 기존 선택을 수행한다. X·접기·색상·생성 카드는 기존 동작을 유지한다. 함대의 순서 요청 경로와 기존 이동 범위는 유지한다.

육군 지도에서 펼친 집단군 묶음 밖으로 군을 옮기면 `FORMATION/DETACH_ARMIES`, 다른 묶음 안으로 옮기면 `FORMATION/MOVE_ARMIES`를 보낸다. 소속 변경 뒤에는 동일 세션의 새 revision과 변경된 소속 HUD를 모두 확인하고 같은 종류의 전체 ID 목록으로 `CARD_ORDER`를 보낸다. 선택·나라·차원·군종·세션 변경, 응답의 소속 불일치 또는 100클라이언트 tick 동안 미확인 시 후속 요청을 버린다. HUD의 `orderToken/orderRevision`을 복사와 필터링 뒤에도 보존한다. 보낸 revision 이하가 아닌 같은 토큰의 HUD를 처리 확인으로 보고, 명령 context가 같은 토큰·revision에서 다음 요청을 허용할 때만 후속 순서를 보낸다. 낮은 HUD revision이나 토큰 없는 구형 HUD는 확인으로 쓰지 않는다. 새 토큰·선택·국가·차원·군종 변경은 대기 요청을 취소한다. 이 모의 렌더 검사는 운영 멀티플레이 응답의 원자성을 증명하지 않는다. 순서나 군 이름·소속·집단군 수를 클라이언트 저장 상태에 먼저 반영하지 않는다. 모든 요청은 빈 rays와 기존 서버 권한·revision 검사를 사용한다.

`CommanderCardDragTest`는 전체 형제 목록·군 소속·끝 경계·양 끝 생성 칸의 처음/끝 이동·함대 예외를 검사한다. `CommanderCardDragRenderTest`는 GUI2 두 해상도에서 실제 화면 입력으로 가로 드래그·양 끝 생성 칸 도달·클릭·집단군 블록 이동·소속 없는 군 앞 미리보기·드래그 중 집단군 배경 고정·이탈/편입·두 메시지 순서·Escape·거절 HUD 뒤 후속 요청 폐기를 검사한다. 응답은 합성 HUD와 Context이므로 운영 지도 드래그·실제 네트워크/저장/재접속·다중 사용자 동기화의 증거는 아니다. 집중 명령은 팩 빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=army-layout`이며 마지막 전체 검증과 나·다·라 원본 대조는 별도로 수행한다.

이전 카드 드래그 집중 검사는 클라이언트 JUnit 122개·프로토콜 119개·권위 producer 45개에서 실패·오류·건너뜀 0이며 실제 PNG 151장을 생성했다. 권위 입력은 최신 확정 계약을 복사한 고정 서버 사본에서 생성했고 재실행 시 변경되지 않은 JUnit/producer의 성공 결과를 재사용했다. 초기 컴파일 오류, 고정 네 스킬을 가정하던 검사와 첫 칸 이동 오류의 실패 로그도 이슈 검토 ZIP에 보존한다.

육군 집중 범위에는 재리뷰를 위한 현재 해군 함대·공군 기지/비행단 펼침 칸도 포함한다. `CommanderPanelsTest.runArmyLayout`에서 navy/air 권위 입력을 같은 두 해상도로 그리지만 이 캡처는 나·다 단계의 원본 일치 완료가 아니다. 정지 상태의 바닐라 HUD 유지와 군 RGB 표시, 기본 글자 크기·선택·요청 경계를 함께 확인한다. 운영 접속·실제 소켓의 정지/재개 전파·저장/재접속·다중 접속·직접 음향 청취는 별도다.

정지 상태의 왼쪽 목록은 위로 올라온 명령 팝업의 높이와 별개로 첫 카드 위까지 사용한다. 부속 비행단을 선택하면 좁은 화면의 명령 막대도 공군 도구줄 오른쪽으로 옮긴다. 페이지 버튼은 카드 위 높이 안에서 같은 글자 크기로 그리며, 온전한 한 줄도 없는 경우 기존 마우스 휠로 이동한다. 지도 삭제·배정·계층 선택의 회귀 입력은 재배치된 버튼과 겹치지 않는 오른쪽 지도 지점을 사용한다.

앞선 running 조건·군 RGB·육해공 패널 검증을 포함한 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 3분24초에 통과했다. 실제 PNG176장, JUnit122+119개, 고정 서버 사본의 권위 producer46개에서 실패·오류·건너뜀0이다. 31개 권위 입력과48개 공유 protocol 소스를 검증했고 변경 없는 성공 JUnit/producer 결과는 재사용했다. JAR와 내장 protocol JAR에는 게임 정적 이미지/음향·서버 군사 클래스·테스트 차원이 없다. 두 모듈 목록 아이콘은 같은 사용자 제공 그림(SHA-256 `3da4c74a23c33a7aba8b7e1d507b2c4ed8a85cbcd3eeb6e30b015acaa44cd0c2`)이다. 이는 나·다·라 원본 일치나 필터 없는 전체 검사·운영 소켓 검증 완료가 아니다.

좁은 정지 화면에서 명령 막대를 오른쪽으로 옮긴 경우 날짜 스코어보드도 82GUI 위로 올려 서로 겹치지 않게 한다. running true·카드 미표시·넓은 화면에서는 바닐라 스코어보드 위치를 유지한다. 정지 캡처의 속도 문구는 서버와 같은 `ProtocolCampaignStyle.speed("PAUSED")`를 사용한다.

편제 변경 두 화살표는 `unitview.gui`의 `divisions/expand_button/GFX_change_template_button`이다. 원본 DDS 자체는 초록이며 원본 화면 214313·214326·214413의 회색은 비활성 표시다. 설치 `hoi4.exe` SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`에서 툴팁 처리 `0x14169f9db`는 버튼의 가상 함수 +0x208 결과로 활성 설명 `CHANGE_TEMPLATE_UNIT`과 비활성 이유 포매터 `0x141698040`을 나눈다. 포매터의 이유 번호 0/1/2/3/4/5는 각각 `EXPEDITIONARIES_CHAGE_TEMPLATE`, `CHANGE_TEMPLATE_DISABLED_BORDERWAR`, `CHANGE_TEMPLATE_DISABLED_SCRIPT`, `CHANGE_TEMPLATE_DISABLED_FAKE_UNITS`, `CHANGE_TEMPLATE_DISABLED_ENCIRCLED`, `SPECIAL_UNIT_CHAGE_TEMPLATE`이다. 선택 부대 순회 `0x1416b7241..0x1416b7352`에서 특수 부대 판정, +0x1dc 원정군 필드, 국경 분쟁, 위장 부대, 편제 잠금, 포위/보급 조건에 대응하는 이유를 기록하고 허용 플래그를 내리는 분기를 확인했다. 내부 함수 의미는 해당 이유 키와 분기 연결로 해석한 것이며 모든 상위 예외/국가 권한 판정까지 복원한 것은 아니다. 재현은 같은 실행 파일에 `llvm-objdump -d --no-show-raw-insn --start-address=0x141698040 --stop-address=0x1416983c0 <hoi4.exe>`와 `--start-address=0x1416b71a0 --stop-address=0x1416b73c5`를 사용한다. TFRKP `history/units/KOR_2020.txt`의 `jiyeop command`는 `is_locked=yes`이고 첫 두 사단이 이 편제를 참조한다. 이것은 원본 선택에 잠긴 편제가 포함될 가능성의 근거이며 사진 속 모든 사단의 실제 저장 상태나 전체 활성 조건을 확정하지 않는다. 후속 `templateBlock` 계약으로 서버가 제공한 사전 비활성 사유를 사용한다. 군 소속 목록 열기로 대신 실행하지 않는다.

`unitview.gui:1608`의 삽 왼쪽 `bar_prepare`와 `:1649`의 오른쪽 `dig_in_bonus`는 같은 파란 `GFX_unitlist_bar_prepare`를 회전해 사용한다. 각각 기존 `planPower`와 `entrenchmentProgress`에 연결하며 값을 받지 못하면 채움을 그리지 않는다. 57사단 경계 입력에는 두 진행률 0.5와 참호 값 3을 명시해 작은 파란 막대를 검사한다. 실제 캠페인 값으로 제시하지 않는다. 생성 카드 툴팁은 카드 바로 위의 공간을 먼저 사용하고 뷰포트 안에 제한한다. 미지정 사단처럼 명령 막대가 없는 경우 그 가상 영역을 피하지 않는다.


군 카드 X·휴지통 분리의 집중 검증은 Java 25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 3분34초에 통과했다. GUI2 PNG199장과 JUnit122+119개에서 실패·오류·건너뜀0이며 변경 없는 protocol/생산자 성공 결과는 재사용했다. `CommanderGroupsRenderTest`는 군·집단군·미지정 목록의 휴지통 선택 범위, 우클릭 무전송, 군 카드 X의 서버 확인 요청과 응답 전 상태 유지, 함대 X의 기존 선택 해제를 검사한다. 삭제 확인창 요청을 캡처하는 합성 입력 검사이며 실제 서버의 삭제/취소, 세션 권한, 저장·재접속·다중 접속 검증을 대체하지 않는다. 고정 생산자는 새 `DIVISIONS_DISBAND` 구현 이전의 사본이다. 전체 클라이언트 검사와 가 전체 원본 대조는 남아 있다.

4차 표시 계약은 `CommanderHudPayloads.java` SHA-256 `9e4aaf06d92808a9c2ec97ee6d28df3dfd71441acecb4b7bf66b008d0a2c93f5`이며 현재는 위의 최종 Card.tooltip 계약으로 갱신했다. 서버 작업 트리의 미공지 변경 대신 공개 hash와 일치하는 보존 사본을 동기화했다. `Panel.templateSummaries`는 같은 아이콘이어도 편제를 합치지 않고 서버 순서·개수·원문 툴팁을 유지한다. 왼쪽 클릭은 해당 `divisionIds`만 선택하고 Shift는 해당 집합만 전환한다. 편제 변경 버튼은 선택 사단이 있으면 `Open(ARMY_TOOLBAR,"DIVISIONS_TEMPLATE",[unit/id...])`으로 서버 후보 창을 요청한다. `MemberData.templateBlock`이 있는 사단을 하나라도 선택하면 버튼을 회색으로 그리며 사단 ID 순 첫 사유를 색·줄바꿈·단축키 문구까지 그대로 표시한다. 왼쪽·오른쪽·반복 클릭 모두 후보 요청을 보내지 않는다. 잠긴 사단을 선택에서 빼면 나머지 선택 사단만 후보 창에 보낸다. 선택이 없어도 회색이다. 서버의 최종 권한 판정은 그대로 유지한다.

`MemberData.orderWarning`과 `orderTooltip`은 원본 `unitview.gui`의 (23,4) 위치에서 NO_ORDERS/INVALID_ORDERS 그림과 원문을 표시한다. 적응 그림은 (140,16),12×12px이며 값이 없거나 둘 다 0이면 숨긴다. 설치 실행 파일 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`의 `0x141695225`가 `unitlist_acclimatization` 포인터를 +0xc0에 저장한다. `0x1416b959f..0x1416b95fc`는 `0x140614bc0`으로 양수 최댓값의 첫 적응 종류를 찾고, 없으면 숨기며 `0x14169b310`의 프레임을 사용한다. 프레임 함수는 종류 인덱스×2+1(원본 1-based)에 적응값≥100000이면 1을 더한다. 공개 `common/acclimatation.txt`의 cold→hot 순서 및 DDS 네 프레임과 대조한 클라이언트 0-based 규칙은 추위 0/1, 더위 2/3이며 수신 비율≥1에서 완성 그림이다. 동률은 추위를 우선한다. 재현 명령은 `llvm-objdump -d --no-show-raw-insn --start-address=0x14169b310 --stop-address=0x14169b3f3 <hoi4.exe>` 및 `--start-address=0x140614bc0 --stop-address=0x140614c3d`, `--start-address=0x1416b959f --stop-address=0x1416b95fe`이다. 날씨별 증가·불이익 계산과 상세 적응 툴팁은 이 표시 프레임 분석에 포함되지 않는다.

사단 자동 그림의 정적 분석은 설치 실행 파일 SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5` 기준이다. `priority` 토큰 0x8d는 파서 `0x14101f253` → `0x14142cfee`에서 병과 객체 +0x60으로 연결되고, `ai_priority`(+0x64)는 별도다. `0x140ba41f0`은 `regiments`(+0x48) → `support`(+0x68) → `regimental_support`(+0x88)의 유효 병과를 `0x140b97cd0`으로 처음 등장한 순서대로 집계한다. `0x140ba45b2..0x140ba462b`는 `개수 × priority`의 양수 최댓값을 template+0x190에 저장하고 동률은 앞 항목을 유지한다. 실제 사단 목록 `0x1416b913f..0x1416b91b6`이 getter `0x140b9f5c0`의 이 값을 그림 함수 `0x140b458f0`으로 전달한다. 전달된 수동 그림 ID의 조회가 성공하면 그 그림을 우선하며, 자동 키는 `GFX_unit_` + 병과 이름 + `_icon_medium`이다. 수동 ID는 표시 설정에 따라 -1로 바뀌기도 한다. 편제 파서 `0x140ba29f0`의 토큰 0x2fa3/0x2f9c/0x41e7과 등록 문자열을 연결해 이 그리드 이름과 집계 순서를 확인했다. 지원 두 종류도 집계에 포함된다. 집계 이전 연대 지원 정리 정책·표시 설정 값의 의미와 원본 게임 실행 대조는 미확정이다. 이 클라이언트 작업에서는 서버 병과 집계/그림 규칙을 변경하지 않았다. 서버 반영은 담당자의 원본 정의·편제 순서 검토 범위이며, 클라이언트에서 별도로 다시 계산하지 않는다. 재현은 `llvm-objdump -d --no-show-raw-insn --start-address=0x140ba41f0 --stop-address=0x140ba47a9 <hoi4.exe>`와 위 getter·소비 함수 구간을 사용한다. 전체 토큰/주소 연결과 미확정 범위는 CMP-34의 `division-icon-evidence` 문서에 기록한다.

`CommanderTheaters`는 `View.theaters`와 원본 `theatreselector.gui`의 `theater_group_item`/`army_group_box`/`new_theater_group_item`을 사용한다. 이름·총 사단·군/집단군 구성·군별 경고는 서버값이다. 전구 배정 줄의 왼쪽 클릭은 소속 전체를 선택하고, 우클릭은 선택한 군/집단군과 `target/theater/N`으로 `FORMATION/THEATER_ASSIGN`을 요청한다. 새 전구는 선택한 편성을 `FORMATION/THEATER_CREATE`로 보낸다. 버튼은 20261017 원본 214413·214434·214437과 220659~220755의 관찰에 따라 집단군 소속 군을 선택했을 때만 표시한다. 집단군·전구 직속 군 선택에서는 숨긴다. 이 조건은 원본 엔진에서 확인한 조건이 아니라 사용자 제공 화면의 관찰에 따른다. 응답 전에는 전구 구성을 변경하지 않는다. 설정은 `THEATER_SETTINGS/<우선권>` 계약에 따라 로컬 창을 열고, 삭제 확인 요청은 이 창에만 배치한다. 전투 기록 및 공격전/방어전 지표는 클릭 실행 기능이 아니다. 전투 수는 1 이상일 때만 기본 글자 크기로 표시하고 0·미상에서는 숫자를 숨긴다. 현재 표시 공간 밖의 전구는 휠로 이동한다. 편제 요약은 기존 머리줄의 11칸 한도를 유지하므로 그 이상의 편제 표시와 전구 내 많은 군의 겹침/접기 원작 동작은 추가 대조가 필요하다.

전구 배정 줄은 TFR `assign_group_button.dds`의 첫 299×35 영역을 기존 atlas에서 직접 그린다. DDS SHA-256은 `ed104f93190b9cbf85590749a486214875dfe6e39dc7af88e3ecef2543a0f263`이다. 유효 `theatreselector.gfx:98`은 4프레임을 선언하지만 실제 DDS는 897×35이고 299px 간격의 온전한 버튼 그림 세 개가 있다. 첫 영역의 올리브 배경은 사용자 원본의 배정 줄과 대조해 선택했으며, 모든 상태의 엔진 프레임 선택 규칙을 확정한 것은 아니다. 원본 선언·atlas·출처는 변경하지 않는다. 하위 군 줄과 새 전구 버튼을 원본 높이에 맞추되 칸 안 배지·경고·숫자의 사용자 지정 1GUI 아래 오프셋은 유지한다. 전구 생성/배정의 서버 집단군 이동 근사 규칙을 원작 엔진 동작으로 해석하지 않는다. 군·집단군의 수송 캡슐은 같은 폭으로 넓혀 숫자 오른쪽 여백을 확보하고, 특성 12개가 겹치는 경우에도 훈장 크기·순서·개별 툴팁을 유지하며 오른쪽 여백 안으로 겹침 폭을 제한한다.

전구·편제 추가 계약의 집중 검증은 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r4-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r4-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 3m 57s에 통과했다. PNG205장, 클라이언트122·protocol119·r4 생산자46개, 명령 importer8개는 실패·오류·건너뜀0이다. 변경 없는 성공 결과는 재사용했다. 두 해상도 GUI2에서 같은 그림의 다른 편제 선택, 머리줄을 유지한 연속 Shift 전환, 빈 선택의 비활성 버튼과 닫기, 서버 원문 툴팁, 전구 우클릭/반복 요청·전체 선택·생성·32번째 전구 스크롤을 검사한다. 새 계약 장면의 두 사단 이름은 기존 TFRKP OOB fixture에서 가져오고 편제·경고·적응·전구 상태는 합성이다. 초기 테스트의 마우스 숫자 가정은 Minecraft26.3의 LEFT=1/RIGHT=3 상수로 고쳤으며 실패 로그를 보존했다. 고정 생산자는 이전 서버 구현에 공지된 e4a2c3f2 프로토콜을 합친 것이므로 최신 서버의 실제 소켓·저장·재접속·멀티·직접 청취를 검증하지 않는다. 전체 클라이언트 검사와 가 전체 원본 일치는 미완료다.

편제 잠금과 4차 계약의 집중 검증은 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r5-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r5-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 7m 5s에 통과했다. PNG207장, 클라이언트122·protocol119·r5 생산자46개에서 실패·오류·건너뜀0이다. 잠긴/허용 사단 혼합 선택의 좌우·반복 무전송, 원문 사유 보존, 잠긴 사단 제외 뒤 재활성화를 두 해상도 GUI2로 검사했다. HQ 실제 배치는 서버 담당자의 명시적 사 단계 이관에 따라 표시/툴팁만 제공한다. 당시 미노출이던 전구 설정·삭제는 현재 위의 최신 계약으로 연결되어 있다. 이 기록 당시 후속이던 abilityDetails 표시는 현행 `CommanderPanels.selectedAbilityDetails`에 연결되어 있다. PlanOverlay와 HQ 실제 배치의 서버 동작 완료 여부를 표시 연결로 판단하지 않는다. r5는 이전 격리 서버 소스에 공지 protocol만 합쳤으므로 최신 서버 게임 동작·실제 소켓·저장/재접속·멀티·직접 청취 검증을 대체하지 않는다. 이 집중 실행 당시에는 전체 클라이언트 검증을 수행하지 않았으며 후속 전체 기록과 구분한다.

카드·전구 전체 툴팁은 약력과 별개라는 사용자 답변을 적용한다. `----` 같은 구분선은 기존 `sourceSpacing` 경로에서 빈 줄로 표시하며 원문 저장값은 고치지 않는다. 기술·경험 및 원수 부분 혜택 전체 블록은 최종 Card.tooltip 계약으로 연결했다. 카드·전구는 선택 안내·이름·병과·설명 뒤에 이 블록을 붙이고, 초상용 Card.detail 약력은 기존 초상 툴팁에 보존한다. 최종 검증 완료 여부는 위 검증 상태를 따른다. 병과 툴팁 글리프는 기존 원본 그림만 재사용하고 국기·부대 상태 그림을 불필요하게 등록하지 않는다. 가져오기 결과가 클라이언트의 262,144바이트 제한을 넘으면 빌드 전에 거절한다.

### 함대 원본 배치와 구형 카드 순서

해군 카드 줄은 원본 `20261004213252_1`·`20261004213751_1`·`20261004213243_1` 대조에 따라 기존 위치보다3.5GUI 아래에 그린다. 임무 막대와 기동부대 띠의 그림 위치는 유지한다. 카드 그림의 소수 좌표는 보존하고 정수 클릭 상자는 안쪽 경계에서 시작해 `cardsBottom`에서 자른다. 일시정지·구형 snapshot에서는 바닐라 HUD 예약 영역에도 그림과 클릭 상자가 들어가지 않는다. 기동부대 띠의 입력 영역은 배경 기준 y3~20GUI이며 카드와 겹치지 않는다. 원본 2560×1440의 새 함대 칸 틀 위 끝은1331±1px, + 그림은1363~1391px이며 띠 아래 밝은 선은1327~1328px이다. `CommanderPanelsTest.assertCardLayout`은 해군 명령 줄에서 카드까지67.5GUI와 모든 패널 hit 비겹침을 검사하고, `navy-layout`도 실행 중·일시정지·구형 snapshot의 같은 검사를 수행한다. 육군·공군 카드 위치는 이 보정에 포함하지 않는다.

이 보정의 두 해상도 실제 렌더에서 새 함대 칸 위 끝은2560의1331px·1280의611px이며 띠 아래 밝은 선은각각1328px·608px이다. 공통 `UiUnits`의 최소 배율1 때문에 GUI2의 카드 원본 그림 크기는1280에서도 동일하며 화면 높이가720px 줄어든 만큼 좌표가 달라진다. 따라서1280의 목표를2560 좌표의 절반인665~666px로 잡으면 기존 크기 유지와 일치하지 않는다. 카드의3.5GUI 이동은 양쪽 모두7px이며, + 그림의 녹색 픽셀 경계는2560에서1362~1390px·1280에서642~670px이다(녹색 판정 기준과 외곽 그림 경계를 구분한다). GUI2 두 해상도에서는 화면 아래 끝에서 같은 거리로 배치하는 이 좌표를 기준으로 삼는다. 1280의611px·608px와2560의1331px·1328px를 유지하며 + 그림을 추가 보정하지 않는다. 원본 `20261004213252_1`에 있는 “활동 중인 지역: 0” 표시도 유지한다. 집중 `navy-layout`의 실행 중·일시정지·구형 상태 클릭 경계 검증과 필터 없는 전체 검증은 별개다.

자동 분리 수리의 개별 단추는 `autoSplit=true`에서만 원본 `nv_btn_toggle_split_for_repair/frame_0`의 초록 불을 켜고, false·미상은 `frame_1`의 꺼진 그림을 쓴다. 함대와 기동부대의 함종 칸은 `FleetData.hullAir`·`NavalData.hullAir`에 키가 있을 때만 함재기 둘째 줄을 그린다. 값은 서버의 탑재기 수/갑판 크기 비율이며 0은 빈 막대, 누락은 줄 없음이다. 기존 `GFX_navieslist_bar_planes`와 `GFX_NV_XS_plane_icon`(`gfx/interface/naviesview/xs_plane_icon.dds`, 15×6)을 재사용한다. 원본 `ship_entry_compact`의 bar_planes y24·plane_icon y27 순서를 따라 기존 함종 막대 아래에 폭9.5GUI 막대와 가운데7.5×3GUI 비행기 그림을 놓는다. 기동부대에도 같은 위젯을 적용하는 것은 리뷰 계약이며 해당 원본 항모 기동부대 사진을 확인한 결과는 아니다.

`CommanderNavalContractTest`는 항모 입력의 빈 함재기 비율과 비항모의 키 부재를 검사한다. 별도 `hoi-navy-synthetic-air-split-*` 장면은 자동 분리 켜짐/꺼짐/미상과 함재기0.5/0/누락 표시를 위한 합성 응답이며 실제 보유량이나 서버 상태 변경을 뜻하지 않는다.

함대 상세 행은 설치된 TFR `naviesview.gui`의 `taskforce_view_entry` 좌표를 기준으로 첫 줄에 함선 수·왼쪽 정렬 이름·소속 함대 배지·선택·X, 둘째 줄에 원본 임무 원형 그림과 상태·자동 분리 수리·수리 우선순위·교전 설정, 셋째 줄에 함종별 수와 편성 설정을 놓는다. 증원 설정은 전체 도구 줄에서 선택 기동부대에 일괄 적용한다. 원본에 없는 세로 상태 막대·셋째 줄 추가 증원 단추·임무 막대 X는 표시하지 않는다. 함종 수는 현재 수와 `NavalData.targets` 목표 수를 두 줄로 표시하며 목표가 없으면 현재 수를 반복한다. 이 기본값은 화면 계약의 결정이며 원작 엔진의 목표 계산을 추정하지 않는다. 선택/X/모두 선택은 클라이언트 선택만 변경하고 실제 함선이나 편성을 수정하지 않는다. 함대 이름은 서버의 영어 원문을 유지한다. 글자는 축소하지 않고 긴 이름은 사용 가능한 폭에서 가로 스크롤한다.

아래 임무 막대는 원본 `action_bar_window`의 초계·타격부대·통상파괴 위 지배력 그림을 표시한다. 기동부대 띠는 전체 함선 수 닻 배지와 기동부대별 함선 수를 한 배경에 놓고, 폭을 넘을 때만 페이지 단추를 만든다. 함대 초상은 84×117 원본 프레임 비율을 유지하며 가로로 붙는다. 카드 순서 요청은 기존 `CARD_ORDER`를 사용한다. 구형 서버의 해군 카드 `role=null`도 해군 화면 문맥에서 순서 변경을 허용하며 명시적 `NAVY_HQ` 미배정 카드는 제외한다. 군·집단군의 소속 변경 경로는 그대로다.

함대 머리줄의 함종 수와 아래 띠의 전체 척수는 `Panel.divisionTypes`의 서버 전체 집계를 우선 사용한다. 이 집계가 없는 구형 snapshot은 상세 기동부대가 전부 포함된 경우에만 합산한다. 전체 집계가 없는 축약 snapshot은 부분 members 합계를 전체 수로 표시하지 않는다.

`Panel.fleet`의 활동 지역·모항·전체 척수·함종별 막대/툴팁과 `Member.data.navy`의 활동 문구·아이콘·경고를 표시한다. 모항과 자동 모항은 전달된 값·원문 툴팁을 표시하며 변경 요청은 서버 계약에 없다. 자동 모항 선택은 서버 미지원이다. 연료통과 항공기 그림은 원본 위치에 놓고 미제공 수치를 만들지 않는다. 함대 머리줄·기동부대·띠의 방패는 `Panel.color`로, 소속 함대 닻과 띠 첫 닻은 금색으로 표시한다. 휘장 단추는 색상 팔레트와 서버 휘장 선택으로 연결하며 `FLEET_COLOR`와 `FLEET_INSIGNIA`를 구별한다. 예비 함대는 서버 `NAVY_HQ` 카드와 원본 `reserve_portrait`/`reserve_frame`으로 표시하고 드래그 중에도 맨 뒤에 유지하며 `CARD_ORDER`에서 제외한다.

도구의 원본 15칸 배치는 유지하며 표시한 12개 도구는 `NAVAL_TB/*` 판정이 도착해야 사용할 수 있다. 서버 미지원 개장은 숨긴다. 새 기동부대·반 선택은 원본 GUI에 선언돼 있으나 엔진의 표시 조건을 확인하지 못해 숨기며 빈 칸과 나머지 좌표를 유지한다. 생성·분할·병합·예비 이동·해체는 서버 창으로 연결하고 해체는 확인창에서 확정한다. 자동 편성은 서버가 허용한 `BALANCE` 창으로 연결한다. 해상 재보급·개장은 서버의 미지원 사유를 유지한다. 증원·자동 분리·수리/교전 정책·항모 태세는 각 기동부대의 기대 상태를 순번과 함께 `NAVAL_SETTING/BATCH/*`로 보내며 새 revision 전 반복 요청을 억제한다. 수리/교전/태세 우클릭은 이전 단계다. 서버는 미설정 수리 정책을 실제 기본 동작인 0으로 발급하며 클라이언트는 `policy/0`부터 직접 순환한다. 구형 snapshot의 미상 정책에는 요청을 보내지 않는다. 절반 선택은 현재 기동부대 목록의 앞 절반(홀수는 올림)을 클라이언트에서 선택하는 계약상의 동작이며 개별 함선 절반 선택의 원작 동작과 같다고 주장하지 않는다.

해군 임무 `Decision.details`는 완전한 원문이므로 기존 설명에 덧붙이지 않고 그대로 표시한다. 경고 `Decision.warning`은 원본 노란 glow로 표시한다. 경고 아이콘·국기·활동 토큰과 단축키 키캡은 외부 팩의 원본 그림을 사용하며 툴팁 문자열·색·빈줄·순서를 보존한다. 키캡은 표시이며 기존 미등록 키 바인딩을 실제 사용 가능 단축키로 검증하지 않는다.

바다 hover는 현재 해군 지도에서 HUD 밖 커서 광선을 `SeaHover`로 보낸다. 위치·카메라·국가·차원·화면 변경은 이전 응답을 무효화한다. 응답이 없으면 3클라이언트 틱마다 마지막 위치를 다시 보내 서버의 2틱 제한으로 버려진 요청을 복구하고, 응답 후 정지 커서는 20틱마다 갱신한다. 빈 region/text는 툴팁을 지운다. `SeaHoverTooltip.text`의 지배율·현재 지배·초계 기본값과 추가 기여 줄은 서버값 그대로이며 클라이언트 계산을 하지 않는다. 기준 원본 세 장에 없는 최소 지배력 줄과 이 서버가 제공하지 않는 일수별 증감 줄은 넣지 않는다.

집중 재현은 팩 빌드 후 Java 25에서 `test runGameTest remapJar -PhoiTestSuite=navy-layout -PhoiServerSource=<검증 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java --console=plain --no-daemon`이다. `armyHud` 생산자가 기존 입력과 `NavalHudTest`의 임무 판정·초계/예비 함대·바다 툴팁 입력 및 출처를 생성하고 같은 프로토콜을 확인한다. `CommanderNavyLayoutTest`는 GUI2 2560×1440·1280×720의 실제 화면 선택·우클릭 무동작·선택 해제·모두 선택·초계 대상·함대 가로 드래그와 기존 수리 정책 반복 클릭을 검사한다. HUD/Context 주입·요청 관찰자 검증이므로 운영 접속·소켓·저장/재접속·멀티플레이·직접 음향 청취를 대신하지 않는다.

필터 없는 전체 재현은 같은 명령에서 `-PhoiTestSuite=navy-layout`을 생략한다. 이 실행에도 함대 레이아웃·계약 검사가 포함된다. 전체 실행은 `armyHud` 입력을 자동 생성하지 않으므로 `HOI_COMMANDER_PREVIEW`에 같은 서버 계약으로 생성한 전체 지휘관 fixture 폴더를 지정한다. 함대 기본·초계/예비·수송선 제외·항모 판정·바다 툴팁뿐 아니라 임명·제독·에이스·특성 입력도 함께 있어야 한다. `HOI_OFFICER_PREVIEW`는 성공한 서버 GameTest의 `officer-source-fixtures`로 지정한다. `HOI_LAND_PLAN_PREVIEW`는 서버의 `land-plan-overlay.json`으로 지정하고 같은 폴더의 `land-plan-preview.json`도 함께 보존한다. 지도 검사는 `resolveSibling`으로 초안 파일을 읽으므로 오버레이 파일만 복사하면 실패한다. `HOI_MISSION_OVERLAY_FIXTURES`는 서버의 `mission-overlay-source-fixtures`로 지정한다. 동시 작업이 있으면 이 네 입력을 별도 폴더로 복사하고 해시·원본 실행 경로를 보존하며, `-PhoiResourcePack=<고정 팩>/resources.zip`과 같은 폴더의 manifest·분할 ZIP을 함께 사용한다. 명령 성공과 별개로 입력 누락에 따른 건너뜀을 로그에서 확인한다.

`navy-layout`은 일반 `CommanderPanelsTest`의 해군 기본·수송선 제외 장면도 GUI2의 2560×1440·2560×1369·1280×720에서 실행한다. 이 검사는 `kor-navy-assessment.json`의 서버 판정을 주입하고 `naval_tool_SPLIT`·`naval_tool_MERGE` 아이콘 ID로 버튼을 찾아 원문 툴팁과 정확히 한 건의 `NAVAL_ORGANIZE` 요청, `force/` 범위와 `half` 부재를 확인한다. 숨긴 `CREATE`·`SELECT_HALF`·`UPGRADE`는 입력 영역도 없어야 한다. `CommanderDirectInputTest`는 개별 행의 증원 hit 부재와 같은 서버 판정의 원문 툴팁, `naval_tool_REINFORCE`의 정확히 한 건인 `NAVAL_SETTING/BATCH/REINFORCE` 요청(`id`, `0/state/<기대 상태>`)을 검사한다. 해군 상태가 없으면 자동 분리 수리·교전 규칙 두 설정과 9개 대상의 설명 11개를 확인하고, 서버가 허용한 증원 도구를 눌러도 빈 일괄 목록에서 요청·대화창·선택 변화가 없어야 한다. 이 직접 입력 검사는 `movement-controls` 집중 검증과 필터 없는 전체 검증에 포함된다.

같은 집중 실행의 `CommanderNavalContractTest`는 12개 표시 도구와 3개 숨김 도구의 입력 영역 부재, 서버 임무/활동/모항 툴팁, 미지원 도구 차단, 일괄 증원의 반복 클릭 억제, 교전 정책 우클릭, 해체 확인창 요청, 함대 색상 요청, 선택 변경 후 오래된 판정 거절, 예비 카드의 순서 제외, 바다 hover의 오래된 응답 거절·재시도·빈 응답 처리를 검사한다. 색상 선택 캡처는 커서를 팔레트 밖으로 옮겨 이전 툴팁이 가리지 않게 한다. `-PhoiResourcePack=<고정 팩>/resources.zip`을 함께 지정하고 같은 폴더의 manifest·분할 ZIP을 유지하면 동시 팩 변경을 피할 수 있다. 1280×720 GUI2에서는 기동부대 목록을 한 행씩 페이지로 넘기며 긴 초계 활동 문구는 칸 안에서 가로 스크롤한다. 상단 HUD는 `HudMenuBar.maximumScroll`로 좁은 화면의 지표 줄을 가로 스크롤한다. 칸보다 긴 값의 가로 스크롤과 전체 값 툴팁은 화면 밖 잘림과 구분한다. 이 장면의 함대 단추·색상 팔레트·초계/바다 툴팁 경계 확인과 공통 HUD 전체 해상도 대응은 구분한다.

함대 머리줄의 빈 초상에는 원본 `CLICK_TO_ASSIGN` 문구를 겹쳐 표시한다. 함대 색 띠는 패널 오른쪽 바깥의 5.5×42.5GUI 영역이며 색상/휘장 선택 입력도 이 영역을 사용한다. 함종 그림은 지도용 작은 표식 대신 `GFX_unit_<함종>_icon_medium_white`, 막대는 원본 `navieslist_bar_str_compact` 위·`navieslist_bar_org_compact` 아래로 폭9.5GUI에 표시한다. 기동부대 수/목표 사이에는 원본 `NV_h_divider`를 놓는다. 활동 문구의 국기만 4.5×3GUI로 표시하고 문구 글꼴 크기와 툴팁 원문은 유지한다.

정박·할당 해역 없음·임무 수행·위치 유지의 원형 그림은 `NV_all_missions`이며 정박은 frame0 닻이다. 이동·수리·교전·분리 증원은 기존 `ship_activity_*`를 유지한다. 소속 함대 배지는 `army_shield_bg` 위 원본 해군 선택 휘장에 금색을 적용하고 하단 띠 첫 닻에도 같은 해군 그림을 쓴다. 서버의 구형 31 이상 휘장 ID는 기존 육군 휘장 자산으로 보존한다. 오른쪽 설정 단추 셋에는 `NV_btn_base_partial_toggle` 틀을 표시한다.

`NavalData.dominance`가 양수일 때만 지배 기여 아이콘 frame1을 쓰고 나머지는 frame0이다. 툴팁은 `dominanceTooltip` 그대로다. 연료통은 머리줄과 같은 frame0이며 단계별 프레임 규칙은 미확정이다. 값·설명은 `ClientCampaignHud`의 국가 연료를 읽는 `CommanderAirBases.fuelTooltip`을 재사용하고 기동부대 연료로 추정하지 않는다. 정박 임무의 선택 그림은 체크 삼각형인 frame1 대신 초록 불의 frame2이며, 임무 배경 중앙은 원본 그물 무늬를 늘이지 않고 타일링한다. 수리 단추는 원본 `btn_repair_now_small.dds` 64×31 중 첫32×31 셀을 사용한다. 두 번째 셀은 렌치와 빨간 X로, 방어 태세 스트립의 일부가 아니다.

계약4판의 기동부대 함종 막대는 `NavalData.hullStrength/hullOrganization`의 `unit_hud/navy_<함종>`별 실제 비율을 읽어 함대 머리줄과 같은 위 주황·아래 초록 배치로 채운다. 구형 snapshot의 누락 값은 미상으로 유지하며 전체 평균을 각 함종으로 복제하지 않는다. 334칸은 선택 중 `carrierStance`가 있는 항모 기동부대의 다수 태세를 frame0~4로 표시하며 동률은 서버에 발행한 선택 순서(ID 정렬)를 따른다. 항모 없는 기동부대도 일괄 요청에 `stance/none`으로 포함한다. 항모가 없으면 같은 칸에 수리 atlas의 오른쪽32×31 셀을 표시하고 `NAVAL_TB/REPAIR_CANCEL` 원문·판정과 `NAVAL_SETTING/REPAIR_CANCEL_BATCH`에 연결한다. 오른쪽 셀은 기존 `frame_0`64×31 안에 이미 보존되어 있어 별도 원본 추출은 하지 않는다. 비활성 도구는 공용 `UiAssets.drawGrayscale`의 원본 disabled 변환을 쓰며 단순 검은 덮개를 쓰지 않는다.

계약3판의 첫 집중 실행은 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=navy-layout`으로4분19초 통과했다. 고정 서버는 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`, 새 팩 사본은 `C:/Users/Kiruy/AppData/Local/codex-hoi/cmp41-r3/pack/resources.zip`이다. client136·protocol124개의 실패/오류/건너뜀은0이며 protocol의 변경 없는 성공 결과를 재사용했다. 실제 GUI2 두 해상도22장을 만들고 초계 경고 설명 두 문단, 숨김3개 도구의 hit 영역 부재, 지배 기여/국가 연료 툴팁을 추가 검사했다. 이미지 열람에서 색 띠가 비율 유지 때문에 짧아지는 것을 찾아 원본44×88을 정상 비율로 그린 후 오른쪽5.5×42.5GUI만 노출하도록 보정했다. 이 기록은 함종 막대 입력의 인계나 필터 없는 전체 검증 완료를 뜻하지 않는다.

색 띠 보정 후 같은 함대 집중 명령은1분에 통과했다. 최종 실행은 GUI2 두 해상도22장을 생성했으며 외부 세로 색 띠와 초계 두 설명 문단을 직접 열람했다. 입력 생산자56개의 실패/오류/건너뜀은0이고 입력38개·공유 protocol48개 검증을 유지했다. 클라이언트 전용 staged JAR에는 게임 그림/음원 자산을 넣지 않았다.

계약4판 집중 검증은 같은 `navy-layout` 명령에 고정 서버 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`와 고정 팩 `C:/Users/Kiruy/AppData/Local/codex-hoi/cmp41-r4/pack/resources.zip`을 지정해 5분38초에 통과했다. 클라이언트137·protocol124·입력 생산자57개에서 실패/오류/건너뜀0이며 변경 없는 protocol 결과는 재사용했다. 입력39개·공유 protocol48개를 검증하고 실제 GUI2 두 해상도26장을 생성했다. 단위 검사는 태세 다수값·동률 순서·항모 없는 기동부대의 `stance/none` 보존을 확인한다. 렌더 검사는 1st Fleet의 수리 취소·항모 선택의 태세 전환·서버 원문 툴팁, 좌우 클릭·revision 전 반복 무전송을 확인한다. 개별 수리 검사는 실제 서버 fixture의 기본 `policy/0`에서 시작한다. 직접 열람한 두 해상도 함대/항모 장면에는 함종별 막대·수리 취소·균형 저울·비활성 회색조·수리0 프레임이 표시된다. 긴 항모 함대/기동부대 이름은 기존 칸 안에서 가로 스크롤한다. staged JAR는 client 진입점이며 내장 protocol을 포함해 게임 그림/음원은 없다. 이 결과는 서버 응답 주입과 요청 관찰자를 이용한 검증이며 실제 항모 전투 효과·수리 취소 상태 변화·저장/재접속·운영 소켓·멀티플레이·직접 음향 청취와 필터 없는 전체 검증을 대신하지 않는다.

### 지도 모드의 카드별 부대 선택

지도 선택은 플레이어 국가의 사단·함대·비행단만 받는다(클릭·드래그·스냅숏 갱신 모두 `Counter.country`로 거른다). 미배정 사단과 군 소속 사단을 함께 고르면 그 군의 카드 화면을 보여 준다. 그릴 군 카드가 없을 때만 미배정 목록을 쓰며, 이 목록은 항상 머리 줄(사단 수·해산·편제·무게·닫기)을 갖는다. 군 카드 색 선택 영역은 보이는 색 띠(오른쪽 7GUI, 높이 44GUI)뿐이라 닫기 단추와 겹치지 않는다.

해군의 임무 막대·기동부대 띠와 해당 입력 영역만 기존 위치에서 1GUI 위로 이동한다. 함대 카드 본체·클릭 영역은 이동 전 위치를 유지해 카드와 위 띠 사이를 넓힌다. 이미 원본 중심에 맞춘 카드 숫자의 세로 위치는 별도로 유지한다. 지도 지휘 UI의 최소 배율1 때문에 GUI2의2560×1440·1280×720 모두 위 두 칸의 이동량은2px다. 함대 카드를 누르면 해당 함대의 기동부대만 목록·띠·임무 대상에 포함되고 미선택 때는 숨긴다. Shift를 이용한 명시적인 여러 카드 선택은 유지한다.

공군 지도에서 군·집단군 카드의 본문과 하단 숫자 칸은 같은 카드 선택 요청을 사용하고, 역할 없는 비행단 카드의 하단은 `AIR_MISSION` 요청을 유지한다. 군 카드 하단은 실제 카드 선택에 맞게 군 카드 원문 툴팁을 표시한다.

공군 지도 모드의 아래 군·집단군 카드는 육군 카드다. 군은 `Member.data().attachedArmy()`가 군 id와 같은 비행단, 집단군은 소속 군 id에 배속된 비행단을 선택한다. 군이 없는 비행단이나 다른 군의 비행단을 섞지 않는다. 카드 아래 숫자 영역도 같은 선택 경로를 사용하며 공군 모드에서는 육군 명령 막대·육군 부대 패널을 대신 열지 않는다. 카드 선택 중 서버 스냅샷이 갱신되면 현재 배속을 다시 반영한다. 선택하지 않았거나 배속된 비행단이 없으면 비행단 목록·공군 임무 막대를 숨긴다. 지도 표식·비행장·부속 비행단 버튼으로 직접 비행단을 고르는 기존 경로는 별도로 유지한다. 육군의 기존 선택 동작은 유지한다.

`navy-layout` 집중 묶음의 `CommanderCardScopeTest`는 서버가 생성한 `kor-command-panel-air-diagnostics`, `air-attached`, `air-detached` 입력으로 군 카드 좌클릭·반복 선택·임무 우클릭 무전송·임무 대상 범위·선택 해제·서버 배속 해제 갱신을 실제 렌더러에서 확인한다. `CommanderPanelOrderTest`는 여러 군과 집단군 소속 변경·중복 없는 비행단 필터를 검사한다. 해당 검사는 서버 스냅샷 주입과 요청 관찰자 검증이며 운영 접속·저장/재접속·멀티플레이 검증을 대신하지 않는다.

고정 서버 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`를 사용한 집중 검증은 클라이언트138개·protocol124개·입력 생성57개의 실패/오류/건너뜀0, 공유 protocol48개·fixture39개 검증과 GUI2 실제 렌더42장으로 통과했다. 당시 카드까지 함께 이동한 검사는 최신 지시의 적용 범위와 달랐으며 현재는 위 두 칸만 이동한다. 해당 검사에서 함대 교체 후 다른 함대의 기동부대가 제외됨을 확인했다. 공군의 군 카드 본문/하단 숫자 클릭·선택 해제·서버 배속 해제를 확인했다. 집단군의 복수 군 배속은 단위 검사이며 해당 서버 렌더 fixture는 아직 제공되지 않았다. 필터 없는 전체 실행·운영 접속·멀티플레이·직접 음향 청취는 수행하지 않았다. 첫 공군 렌더 검사는 존재하지 않는 버튼을 찾는 테스트 오류로 중단되었고, 실제 제공권 버튼으로 수정한 재실행이 통과했다.

### 함대 상세 패널과 공군 배속 칸

해군 제독 칸은 빨간 기술 배경을 기준으로 육군 `armyHeader`와 같은 기술 숫자(+1.75GUI), 능력 그림(+0.75GUI), 능력 숫자(+9.75GUI) 상대 높이를 사용한다. 기술 숫자의 추가1.5GUI 이동을 제거하고 기존 공용 글자 렌더러의 그림자와 크기를 유지한다. 능력치 검은 칸은 `fleet_bg`의 원본 구간을 사용해 숫자 영역까지 받친다. 특성은 원본 `traits_grid`의 패널 상대 (302,60)px에서 시작하고110px 안에서 `CountryFlagStack`으로 겹치며, 마지막 특성의 캔버스는 상대 x412px를 넘지 않는다. 보이는 폭만 클릭·툴팁을 받는다. 서버가 배경 특성을 제외한 `traitDetails`를 보내며 클라이언트는 받은 순서를 그대로 그린다. `CommanderNavyLayoutTest`는 실제 제독, 특성7개 합성 입력, 육군 비교 장면을 GUI2 두 해상도로 캡처하고 특성별 좌우클릭과 툴팁 영역을 검사한다. 합성 입력은 렌더 검증용이며 저장된 특성을 바꾸지 않는다. 팩 빌드 후 `navy-layout` 최종 집중 검증은1분35초에 통과했고 PNG116장을 생성했다. GUI2 2560×1440·1280×720에서 기술 숫자 흰 픽셀은 x207~216/y135~148로 가로 중앙 오차−1px, 빨간 칸 대비 세로 위치는 육군과 같다. 능력치 숫자 흰 픽셀 y151~164는 검은 칸 아래 경계 y166 안에 있다. 첫 특성 금색 띠는 원본 x307/y138, 현재 x313/y138이며 패널 시작 x6을 빼면 상대 오차0px다. 7특성 장면의 마지막 변경 픽셀은 패널 상대 x408이다. 제안된 특성 높이+2.25GUI는 실제 원본보다4px 위로 나와 원본 정의 y60px를 채택했다. 앞4분54초 집중 결과는 이 마지막 특성 높이 보정 전 단계다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 수행하지 않았다.

함종 칸은 원본 `ship_entry_compact` 폭46px에 맞춰 첫 칸37.5GUI와 칸 내부 좌표를 유지하고23GUI씩 배치한다. 같은 고정 입력의 `navy-layout` 집중 검증은 입력 생산자 재실행을 포함해4분3초에 통과했다. GUI2 제독 유무·2560/1280에서 내구도와 조직력 막대의 시작 x는 각각90·136·182·228·274px(5종),90·136·182px(3종)이며 모든 간격이46px다. 좌표는0부터 센다. 첫 칸 위치와23×3px 막대 크기는 유지한다. 제독 없는 장면은 기존 서버 fixture에서 임무만 꺼짐으로 바꾼 합성 입력이며 운영 접속 검증이 아니다. 입력 재생성으로 제독이 달라질 수 있으므로 비교의 제독 이름·초상·능력치 차이는 간격 변경과 구분한다.

함대 카드의 X 그림은 원본16×16px이며 기존 버튼 자리와 클릭 범위를 유지한다. 예비 함대 초상은68×91px, 새 함대 배경·틀은61×83px·79×111px 원본 캔버스를 사용한다. 상세 패널은 `naviesview.gui`의 초상·틀, 왼쪽 정렬 계급/이름, 기술 수준 빨간 배경, 해군 능력 아이콘4종과 `CountryFlagStack`의 전체 특성을 표시한다. 계급은 `leader.role()`이고 함종 간격은46px다. 함종 요약은 원본 `ships_compact`(75,95)와 `ship_entry_compact`의 막대(7,4)/(7,8), 그림(7,12), 수량(34,2)을 따른다. 내구도·조직력 막대는 TFR `naviesview.gfx`의23×3px이며 28×6 텍스처를 칸 전체에 매핑하고 서버 비율만큼 UV를 자른다. 비율 유지 이미지 축소로 막대 폭까지 줄이지 않는다. 항공기 막대는 원본30×3px다. 제독의 초록 +는 특성 배정 가능 상태다. `LEADER_TRAITS`·`LEADER_ADVISOR` 서버 control의 그림·툴팁·활성 여부·요청을 카드와 상세 패널에서 공유하며 계약이 없으면 상태를 추정하지 않는다.

함대 이름은 `fleet_bg` 상단 검은 이름판의 가로 중심을 유지한다. 이름과 머리줄 태스크포스 수는 같은 높이11.5GUI 칸으로 세로 중심을 맞춘 뒤 함께1GUI 올린 y=4GUI에 놓는다. 글꼴·크기를 유지하고 긴 이름은 칸 안에서 가로 스크롤한다. 이 요소별 상향 지시가 이전 이름판 세로 중앙 배치보다 우선한다. 머리줄 태스크포스 수는 최신 요소별 지시에 따라 기존 x=159GUI에서 x=160GUI로1GUI 오른쪽에 놓으며, 이름과 아래 카드의10/10 위치는 유지한다. 해군 임무 버튼은 선택한 기동부대 중 해당 임무를 수행하는 부대가 없으면 frame0, 하나라도 있으면 frame2를 사용한다. 혼합 임무에도 frame2를 쓰고 마우스 호버용 흰 사선 frame1을 일반 상태에 쓰지 않는다. 서버 판정상 비활성이면 선택 여부와 무관하게 frame0에 원본 buttonstate 회색 처리를 적용해 초록 상태 원과 활성 강조가 남지 않는다. 서버 이유와 요청 차단은 유지한다. 공군 임무 배경은 별도의 순서(frame0 켜짐, frame1 꺼짐)를 유지한다.

같은 고정 cmp50 서버·protocol·cmp41-r5 팩으로 상태 원 후속 `navy-layout` 집중 검증은1분22초·PNG106장으로 통과했다. GUI2 두 해상도에서 제독 유무 두 함대의 이름 본문 중심은(177.5,95.5)px, 검은 이름판 중심은(178.5,96.5)px로 각각1px 이내다. 함종별 내구도·조직력 막대의 실제 채움은23×3px다. 타격 임무 없음/일부/전부와 서버 비활성 주입 장면의 상태 원 초록 픽셀은0/64/64/0이며 일부와 전부의 단추 픽셀이 같다. 비활성 좌클릭·반복 클릭·우클릭은 요청을 보내지 않는다. 임무 상태 세 장면은 서버 fixture의 mission만 바꾼 합성 렌더이며 실제 임무 수행·저장 검증이 아니다. 최초 추가 캡처는 검사에서 폐기한 토큰을 재사용해 전부 비활성으로 표시했고, 새 토큰과 활성 응답 검사를 넣은 뒤 통과했다. 앞선 에이스 수정 후 필터 없는 전체 실행은 완료 기록 없이 중단되어 성공 결과로 세지 않는다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이다.

공군 카드별 배속 칸은 TFR `leadergroups.gui/group_wing_type_box`의 `attached_air_bg`(39×45, scale .8)·`attached_air_types`를 사용한다. 군마다 원본 `ATTACH_AIR_WING_DESC` 또는 `ATTACH_AIR_WING_SELECT_SOME`를 표시한다. + 좌클릭과 군 카드 우클릭은 대화창 대신 `AIR_ATTACH` 지휘 명령에 군 ID 원문과 게시한 `air/<id>` 목록을 보낸다. 부속 비행단 칸은 좌클릭으로 해당 비행단만 선택하고 우클릭으로 해당 `air/<id>`만 담은 `AIR_DETACH` 명령을 보낸다. 두 명령의 rays는 비우며 서버가 기종·권한·리비전을 검증한다. 수량/상한/비율은 서버 수량과 target으로 계산하며 `AIRWING_ATTACHED`·`AIRWING_G_STR`·`ATTACH_AIR_WING_SELECT_DESC`의 순서를 사용한다. 요청만으로 배속을 로컬에서 확정하지 않는다. 집단군에 별도의 + 칸을 만드는 정의는 원본에서 확인되지 않아 군 카드에만 붙인다. 기본 지도에서는 이 배속 표시와 상세 머리줄의 배속 선택 단추를 숨긴다.

공군 상세 패널의 출처는 TFR `airselectionview.gui`다. 임무 칸은 훈련·제공·CAS·요격·전략폭격·목표 선택6개와 해상공격·가미카제·항구·보급로·수송·기뢰부설·소해·정찰·해상초계9개의 원본 배열이다. 원본 `tiled_plain_bg_small` 틀, 선택 도구 줄652×48, 비행장·비행단548×48 기준을 GUI2에서 같은 픽셀 비율로 사용한다. 공군의 선택 문구·기지·비행단 이름·숫자는 Minecraft 기본 글꼴의 기본 글자 크기를 사용하고 폭 때문에 계속 작아지지 않는다. 넘치는 이름은 칸 안에서 가로 스크롤하고 전체 이름과 상태는 툴팁에서 확인한다. 이 공군 예외는 해군 글자 크기를 바꾸지 않는다.

비행단 줄의 `ace_btn`은 원본 `GFX_ace_none`42×46px만 표시한다. `nativeButton`을 사용해 별도 회색 틀을 제거하고 왼쪽 보충 정책의 원을 가리지 않는다. 원본 상대 위치(165,-3)px, 기존21×23GUI 클릭 영역·툴팁·해당 비행단의 `AIR_REINFORCEMENT/aces` 요청은 유지한다. `ace_kia`는 서버 상태가 없어 표시하지 않는다.

가미카제·전략폭격 목표 우선순위·공군 공격성·비행단 특화·빠른 배치 단축 버튼과 지역 전체 선택은 현재 지원 계약이 없는 기능이다. 원본 그림을 비활성으로 표시하고 툴팁에서 구현 제한을 구분한다. 선택 도구 줄의 출격 시간 변경은 기존 서버가 지원하는 단일 비행단에만 연결하며 여러 비행단을 한 번에 바꾸지 않는다. 원문 키 `AGGRESSIVE_CLICK_TO`·`DAY_NIGHT_CLICK_TO`·`QUICK_DEPLOY_SELECTION_WINDOW_OPEN_DESC`·`AIR_STRATEGIC_PRIORITIES`·`AIR_SELECT_ALL_REGION`는 번역 패치3411756715에서 검색한 뒤 해당 키가 없는 경우 원본 `localisation/korean/air_l_korean.yml`을 기준으로 한다. 가운데 큰 비행단 배치 창은 이 변경 범위에 포함하지 않는다.

카드별 배속 칸의 존재, 선택 wing id만 담은 배속 요청, 우클릭 해제 요청과 두 툴팁의 실제 렌더는 `CommanderCardScopeTest`가 두 해상도에서 검사한다. 비행단 미선택·직접 두 비행단 선택·군 카드 선택·미구현 임무 무전송도 확인한다. 현재 집중 검증은 지정된 cmp50 서버·protocol 및 cmp41-r5 팩을 사용한다. 갱신된 고정 서버에서 fixture를 재생성해 지휘관 특성 버튼·경험치·임무 상태를 검증하며, 필터 없는 전체 검증은 별도 지시 전 실행하지 않는다. 드래그·음향 청취·운영 접속·저장/재접속·실제 멀티플레이 검증은 이 집중 실행의 증거가 아니다.

갱신된 서버의 `equipmentIcon`은 생산 화면과 같은 `production/source/<hash>` 규칙이며 cmp41-r5 팩의 실제 사진을 사용한다. `missionState`·`missionStateTooltip`은 이름 아래 줄과 해당 호버에 표시하고, 경험치 단계·진행률은 `MemberData.experienceLevel/experienceProgress`를 그대로 사용한다. 둥근 위쪽 표시는 비행단이 속한 `View.theaters()`의 교체 우선순위 LOW/NORMAL/HIGH를 `deploy_priority` frame1/2/3으로 그린다. 전구 값이 없으면 임의의 NORMAL을 채우지 않는다. 아래쪽 보충 정책 표시는 별개다.

`air_mission_efficiency_strip`은 원본 6프레임 중 시계가 없는 빨강·노랑·초록 0/1/2를 사용한다. 서버 efficiency 0~1을 `[0,1/3)`, `[1/3,2/3)`, `[2/3,1]`로 나누는 표시 규칙이다. 이는 원본 엔진의 공개되지 않은 임계값 재현이 아닌 클라이언트 시각 매핑이며, 서버 계산이나 상세 툴팁의 효율 값은 바꾸지 않는다. 시계가 붙은 3/4/5는 의미가 확정되지 않아 사용하지 않는다. 원본 213605 카드 아래 흰 비행기와 두 도구 줄 사이 삼각형은 지도 요소로 확인되어 카드·도구 줄에 그리지 않는다.

선택 줄 문구는 현재 공군 글자 크기로 전체 폭을 측정해 말줄임 없이 표시한다. 왼쪽 단추 네 개만 글 끝 뒤로 옮기고 오른쪽 단추는 고정한다. 비행장 국기는 9×7 GUI 칸을 원본처럼 채우며 GUI2의 실제 픽셀 x22~39/y216~229를 원본과 비교한다. 임무·선택 줄은 `GFX_tiled_plain_bg_small`의 16px 테두리와 중앙 타일 반복을 사용한다. 임무 단추는 각 `air_mission_entry_N`의 일반/체크박스 배경을 사용한다. 행동 줄은 그 위 (27,7)px에 `GFX_unit_list_header_air`(471×35px)를 더한다. 캡처의 RGB(40,42,39)는 이 띠의 색이며 바탕 `tiled_plain_bg_small`의 RGB(26,26,26)를 임의로 보정하지 않는다. 일반 임무 배경의 네 프레임은 켜짐·꺼짐·켜짐 호버·꺼짐 호버이며 비활성 단추는 원본 `buttonstate` 회색 변환을 사용한다.

공군 임무 가능 여부는 현재 선택·세션·리비전에 맞는 서버 `AIR/<임무>` 판정만 따른다. 비활성 상태에는 회색 원본 그림과 서버 reason을 표시하고 요청을 보내지 않는다. 회신 대기와 누락된 AIR 판정도 비활성이다. 클라이언트가 기종·설계 모듈을 추측해 재판정하지 않는다. 혼합 선택은 정확한 전체 선택 목록을 서버에 보내고 실제 적용 대상 필터링은 서버가 맡는다. 지역 없는 비행단은 서버 pending 응답 뒤 기존 지도 공역 선택으로 이어진다.

갱신된 고정 서버·protocol과 cmp41-r5 팩을 사용한 현재 `navy-layout` 집중 검증은 1분22초에 통과했고 실제 PNG98장을 남겼다. 클라이언트138·protocol124·fixture 생산자64개는 실패/오류/건너뜀0이며 변경 없는 검사 결과는 재사용했다. 당시 GUI2 두 해상도의 기술 숫자 본문은 x207~216/y140~153으로 빨간 칸 중심에 있었으나, 이후 육군과 같은 상대 배치 지시에 따라 아래의 제독 칸 규칙으로 대체했다. 카드 `10/10` 본문은 y1421~1434 및 y701~714를 유지한다. 10개 기동부대의 띠 간격은29px, 전구 줄은25px로 각 줄 안에서 일정하다. 닻 그림 왼쪽 여백/마지막 숫자 오른쪽 여백은 띠7/15px, 전구5/14px다. 단일·혼합 비행단 선택의 서버 판정 응답을 주입해 회색 비활성 상태·reason·반복 클릭 무전송, 활성 요청의 정확한 선택 목록, 지역 없는 비행단의 pending 응답 후 공역 선택 전환을 확인했다. 실제 서버의 무장 계산이나 멀티플레이를 대신하는 검사는 아니다. 제독 특성·고문 버튼도 서버의 요청을 그대로 보내는지 검사했다. staged JAR는 client 진입점만 가지며 내장 게임 그림·음원은 없고 허용된 mod-list icon만 포함한다. 수동 입력을 갱신하기 전의 필터 없는 전체 검증은 공군 카드 검사에서 입력 계약 불일치로 실패했다. 공군 부속 칸과 카드 줄 생성 단추 부재, 육군의 기존 배속 띠를 함께 재현하는 집중 범위는 `-PhoiTestSuite=movement-controls`다. 비행장 머리줄 생성 요청은 `CommanderAirBasePanelsTest`가 검증한다. 집중 실행은 `armyHud` 생산자로 `HOI_COMMANDER_PREVIEW`를 생성 입력 경로로 바꾸지만, `armyHud.suites`에 `all`이 없어 필터 없는 실행은 지정한 수동 경로를 유지한다. 이전 cmp50 수동 폴더의 `kor-command-panel-air-diagnostics.json`·`air-attached`·`air-detached`·`air-theaters`에는 `experienceLevel`·`experienceProgress`·`missionState`·`efficiency`가 빠져 전체 실행이 실패했다. 갱신된 고정 폴더64개는 전달된 SHA-256 목록과 일치하고 네 입력의 모든 비행단에 해당 필드가 있다. 전체 실행의 수동 경로와 Gradle 연결은 유지한다. 입력 해시 일치와 집중 검증 통과만으로 전체 입력의 최신 계약을 보장하지 않는다. 필수 표시 필드 검사는 유지하며, 아래 최종 전체 검증에서 갱신된 수동 입력으로 해당 검사와 후속 장면까지 통과했다. 직접 음향 청취와 운영 접속은 수행하지 않았다.

해군 머리줄 수량을 x=160GUI로 옮긴 최종 소스에서 팩 빌드 후 같은 cmp50 서버·protocol·cmp41-r5 팩과 수동 지휘 입력64개로 필터 없는 `test runGameTest remapJar`가26분17초·종료 코드0·실제 PNG2,212장으로 통과했다. 이전 중단 실행은 통과로 세지 않았고, 이번 허용된 첫 전체 실행이 통과해 추가 실행하지 않았다. 클라이언트138개·protocol124개는 변경 없는 성공 결과를 재사용했고, 입력 생산자46개는 재실행했으며 모두 실패·오류·건너뜀0이다. 자동 입력92개의 출처 해시와 공유 protocol48개를 확인했고, 실행 전후 소스·빌드361개 및 지정 입력93개 해시는 불변이다. 공군 배속·선택·비활성 요청 차단, 해군 임무·전구·10개 기동부대·제독2/7특성 장면에 도달했다. GUI2 두 해상도 전체 렌더의 머리줄10/10 본문은 x341~398/y87~100px로 집중 검증과 같고 해당 숫자 칸의 변경 픽셀은0이며 하단 숫자 글리프도 같다. 전체 실행은 수동 입력의 제독과 하늘 배경, 집중 실행은 생성 입력의 제독과 지면 배경을 사용하므로 초상·능력치·특성 내용 및 반투명 테두리 픽셀 차이를 UI 이동으로 해석하지 않는다. staged JAR는 client 전용이고 허용 모드 아이콘 외 그림·음원은 없다. 재현 시 위 고정 경로와 README의 네 수동 입력을 함께 지정한다. 운영 소켓·저장/재접속·멀티플레이·직접 음향 청취는 미수행이며 이 렌더 검증으로 완료를 주장하지 않는다.

### 상세 기술·능력치 칸과 전구 카드 선택
해군 검은 선 기준 재배치 검토 후, 고정 cmp50 서버/protocol·지휘 입력66개·cmp41-r5 팩과 README의 나머지 수동 입력3종을 지정한 필터 없는 `test runGameTest remapJar`가26분1초·종료0으로 통과했다. 실제 PNG2,266장, client140·protocol124·입력 생산자46개는 실패/오류/건너뜀0이다. client/protocol 단위 검사는 변경 없는 성공 결과를 재사용했고, 전체용 입력 생산자는 재실행했다. 집중 검사의 생산자66개와 전체 검사의46개는 선택된 생산자 묶음이 다르다. 자동 입력92개와 공유 protocol48개를 검증했고 실행 전후 소스·입력1,792개 해시는 불변이었다. 두 해상도의123/1234/12345,999/9999,1000/10000 숫자 표시 마스크는 각각 같았다. staged JAR는 client 전용이며 허용 모드 아이콘 외 게임 그림·음원이 없다. 이번 전체 결과가 아래의 이전 단계 전체 검증 대기·미수행 기록을 대체한다. 입력 주입과 합성 경계 검사는 운영 소켓·저장/재접속·멀티플레이·직접 음향 청취를 증명하지 않으며 해당 검증은 미수행이다.

해군 상세는 바탕 `fleet_bg`의 기술 칸 왼쪽 검은 가로선을 배치 기준으로 삼는다. GUI2 두 해상도에서 선은 y122이고, 기술 칸 왼쪽 테두리는 선 끝 다음 x200, 위 테두리는 선보다1px 위 y121이다. 기술·능력치 줄 전체를 이전 위치보다7px 올리며 기술14px·능력치9px와 칸 안 상대 위치는 유지한다. 특성은 리본 위 끝을 선+8px에 맞추고, 첫 리본은 기술 칸 왼쪽+108px에 둔다. 해군 특성7개의 간격은 반올림으로14/16px가 번갈아 나오지 않도록15px를 유지하고 더 많은 특성은 가용 폭 안에서 겹친다. 그림은 원본 비율을 보존하며 리본 폭13px·그림 높이27px를 목표로 확대한 뒤 클릭·호버 영역도 함께 옮긴다. 육군은 수정하지 않는다. 원본 `20261004213231_1.jpg`의 선 y130·테두리 x199/y129와 비교할 때 화면 절대좌표 대신 검은 선 높이를 맞춘다. 이전 해군 세로 위치·크기·간격 기록은 이 규칙으로 대체한다. 전체 검증은 이 보정의 집중 렌더 비교를 Claude가 검토한 뒤 명시적으로 지시할 때만 재개한다.

검은 선 기준 해군 보정의 최종 집중 `navy-layout`은1분38초·PNG170장으로 통과했다. GUI2 두 해상도에서 기술 칸 위 y121, 기술 본문 y126~139, 능력치 본문 y145~153을 실제 픽셀로 확인했다. 7특성 리본의 x좌표는308·323·338·353·368·383·398이고 폭은13px다. 원본23×33px 텍스처를 비율 변경 없이 화면 픽셀 단위로 그린다. 이 실행에서 생산된 제독은 `lone_wolf`와 `superior_tactician`을 가지며, 7특성은 두 실제 특성을 반복한 합성 경계 입력이다. 원본 그림마다 투명 여백·메달 윤곽이 달라 리본 위는 y130/y129, 밝은 몸통 높이는28/27px로1px 차이가 있다. 원본 스크린샷의 제독·이전 생산 입력과 값·특성 구성이 다르며 임의로 저장 상태를 바꾸지 않았다. 육군 상세 영역의 픽셀은 앞선 집중 실행과 동일하고, 배속 수 세 자리 경계·전구 필터/드래그 및 특성 좌우클릭·호버 검사를 통과했다. client140·protocol124·생산자66은 실패/오류/건너뜀0이며 변경 없는 성공 결과는 재사용했다. 실행 중355개 소스/설정 해시는 불변이며 client 전용 JAR에는 허용된 모드 아이콘만 포함된다. 앞선 필터 없는 실행은 종료 코드 없이 중단되어 성공으로 세지 않으며, 재검토 전 전체 검증을 재개하지 않는다. 실제 멀티플레이·저장/재접속·운영 소켓·직접 음향 청취는 미수행이다.

세 자리 표시 대안은 팩 빌드 후 같은 고정 서버·protocol·cmp41-r5 팩의 `navy-layout` 집중 검증으로1분46초·PNG170장에 통과했다. GUI2 2560×1440·1280×720에서123/1234/12345,999/9999,1000/10000의 숫자 픽셀이 각각 같고 세 자리 숫자가 인접 지원기104와 겹치지 않는다. 글꼴 크기와 가운데 정렬은 유지한다. 툴팁은 실제 원래 수량과 비행단 범위를 보존하고 가 상세 칸·다 전구 카드 및 드래그 회귀를 통과했다. client140/protocol124/입력 생산자66은 실패·오류·건너뜀0이며 변경 없는 성공 결과를 재사용했다. 아래 네 자리 표시 검증은 이 대안을 적용하기 전의 기록이다. 필터 없는 전체 검증·운영 소켓·저장/재접속·멀티플레이·직접 음향 청취와 구분한다.

이전 네 자리 표시 검증의 두 해상도 경계 장면은 1200·9999를 그대로, 10000을1000으로, 12345를1234로 표시하며 1234/12345의 숫자 픽셀 일치를 확인한다. 배경을 먼저 그려 옆 칸 배경이 네 번째 글자를 덮지 않게 한다. 기존 숫자 크기와 가운데 정렬에서는 4자리 수가24/26px 칸보다 넓어 옆 분류 수량과 겹치는 가독성 한계가 남는다. 숫자 축소 없이 넘침을 허용한 기존 지시와 함께 검토할 항목이며 가독성까지 완료한 것으로 보지 않는다. 집중 `navy-layout` 최종 실행은1분46초·PNG164장으로 통과했으며 client140/protocol124/입력 생산자66은 실패·오류·건너뜀0이다. 필터 없는 전체 검증과 운영 소켓·저장/재접속·멀티플레이·직접 음향 청취는 별도다.

최신 보드·원본 재측정에 따른 배치: 기술 배경은 왼쪽·위쪽에 원본 관측색 `#584d51`의1px 선을 두고, 그 안에22×33px 빨간 면을 표시한다. 원본 빨간 PNG 자체에는 밝은 선이 없어 투명 테두리를 제외한 원본 면을 재사용하며 파일을 고치지 않는다. 검은 바탕과 같은 위 기준에서 그린다. 육군 능력치 그림은 원래 크기·세로 위치를 유지하면서 첫 그림과 끝 그림의 좌우 여백을 맞춘다. 해군은 가로·숫자 위치를 유지하고 그림만2px 올린다. 기술14px·능력치9px·특성 줄은 유지한다. 이전 재측정 좌표 기록보다 이 배치 규칙이 우선한다.

공군 배속은 최신 보드 정정(`요격기/지원기/+`)에 따라 정지·전진 바탕76px 바로 위의 고정된 세 자리를 사용한다. GUI 정수 좌표의 자리 폭은24/26/26px다. 요격기(FIGHTER·HEAVY_FIGHTER)와 지원기(CAS·TACTICAL_BOMBER·NAVAL_BOMBER)는 비행기가 있을 때만 각각 왼쪽·가운데에 그리며 빈 종류는 그림·수량·클릭 영역을 만들지 않는다. 비행단을 선택한 동안에는 오른쪽 세 번째 자리에 + 하나를 표시하고 두 종류가 모두 차 있어도 유지한다. +좌클릭 또는 군 카드 우클릭으로 선택 비행단을 배속하며, 채워진 칸의 좌클릭 분류 선택·우클릭 분류 해제는 유지한다. 수송기·역할 미상·전략폭격기·정찰기 칸은 만들지 않는다. 허용 역할은 FIGHTER·HEAVY_FIGHTER·CAS·TACTICAL_BOMBER·NAVAL_BOMBER이며 다른 역할이 하나라도 섞이면 원본 금지 툴팁을 표시하고 + 클릭·군 카드 우클릭 요청을 보내지 않는다. 배속 칸 아래 끝은 정지·전진 바탕 위 끝에 틈 없이 붙인다. 분류별 수량 합은 최신 보드의 대안에 따라 3자리까지 그대로 표시하고 4자리부터 앞 3자리만 표시한다(999→999, 1000→100, 1200→120, 12345→123). 기존 크기·가운데 정렬·고정3등분에서는 네 자리 숫자 폭이 칸보다 넓어 위치 조정만으로 인접 숫자 겹침을 없앨 수 없어 이 대안을 적용한다. 글자 크기와 가운데 정렬을 유지하며 툴팁 수량·서버 상태·요청 ID는 자르지 않는다. 원본 `20261009024928_1.jpg`의 가운데 +와 앞선 빈 자리 우선 규칙은 최신 보드 배치로 대체됐다. 원작의 채워진 분류 화면 관측은 아직 없다. 이후 문단의27px 간격·수송기/미상/네 번째 +칸은 이전 검증 이력이다.

이 고정3칸과 상세 칸 테두리·여백 보정의 집중 실행은 팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=navy-layout`로1분41초·종료0·PNG152장을 남겼다. Java25와 고정 cmp50 서버/protocol·cmp41-r5 팩을 사용했다. client140·protocol124·입력 생산자66개는 실패/오류/건너뜀0이며 변경 없는 성공 결과는 재사용했다. 실행 중 Java/fixture 설정352개 해시는 불변이었다. GUI2 두 해상도에서 배속 없음·요격기만·두 분류 모두 채움의 오른쪽 +고정, 빈 자리 무전송, 반복 +요청, 분류 선택·해제, 전략폭격기·정찰기 혼합 선택의 무전송과 전구 필터·드래그 회귀를 통과했다. 실제 능력치 글리프는 육군y136~144·해군y152~160의9px이며 기술 칸 왼쪽/위1px 선과22px 빨간 내부 폭을 확인했다. client 전용 JAR에는 허용된 모드 아이콘 외 그림·음원이 없다. 필터 없는 전체 검사·운영 접속·멀티플레이·직접 음향 청취는 별도 미수행이다.

상세 기술 칸은 TFR `interface/unitview.gui:556–572`와 `interface/naviesview.gui:1116–1132`의 `GFX_land_battle_leader_skill_bg`0.7배를 따른다. 능력 그림은 기존 크기를 유지한다. 검은 바탕은 외부 `army_bg`/`fleet_bg` 원본 영역을 사용해 육군98×34px·해군96×34px로 받친다. 기술 숫자는 기존 Minecraft 글꼴14px를 유지하고 실제 글리프의 가로 중앙에 놓으며, 재측정 지시에 따라 기존 위치보다2px 위인 칸 위+5~+18px로 옮긴다. 능력치 숫자는 원본9px 재측정에 따라 GUI2에서 기본 글꼴의9/14배(기존7px 대비9/7배)로 최근접 표본 렌더링한다. 글자 시작점은 실제 화면의 정수 픽셀에 둔다. 해군은 칸 위+24~+32px, 육군은+22~+30px를 목표로 하며 다른 글자와 플레이어 설정은 바꾸지 않는다. 특성 줄은 기존 위치보다 육군14px·해군5px 왼쪽으로 옮기고 세로 위치·가용 폭·겹침 간격을 유지한다. 서버에서 배경 특성을 제외한 traitDetails를0번부터 연속 배치해 숨은 배경 특성의 빈 슬롯을 남기지 않는다. 원본 비교는 `20261004213231_1.jpg`와 `20261009015126_1.jpg`이며 실제 제독·장군 및 특성7개 합성 입력은 CommanderNavyLayoutTest의 두 해상도 캡처를 사용한다. 합성 특성은 저장된 상태와 구분한다. 아래7px 측정 기록은 재측정 이전 단계다.

`CommanderTheaterCardsTest`를 `navy-layout` 집중 묶음에 연결했다. 새 서버 생산 입력 `kor-command-panel-army-two-theaters`·`kor-command-panel-air-army-theaters`의 육군 전구2 군 줄 선택→공군 전환·전구1 복귀와 배속 비행단 지도 선택의 전구 전환을 GUI2 2560×1440·1280×720에서 확인한다. 요격기60/지원기20 분리, 묶음별 툴팁·좌클릭 선택·우클릭 해제, 선택 중에만 + 표시, 전략폭격기·정찰기 혼합 선택의 반복 + 클릭·군 카드 우클릭 무전송을 검사한다. 역할8개를 바꾼 합성 입력은 허용된 요격기80/지원기104만 배속하며 저장된 기종을 바꾸지 않는다. 수송기 혼합 선택도 금지 툴팁·반복 클릭 무전송으로 검사한다. 수량 합1200·9999·10000·1234·12345의 두 해상도 장면에서 숫자 표시 경계와 원본 툴팁/비행단 범위 보존을 검사한다. 육군·공군 드래그는 숨은 형제 카드를 만들기 위해 테스트 사본의 군 parent만 비운 합성 입력을 쓰고 해군은 기존2전구 입력을 쓴다. 세 군종 모두 실제 화면의 누르기·끌기·놓기로 변경된 전체 `CARD_ORDER`를 관찰하고 숨은 카드의 인덱스가 유지되는지 검사한다.

팩 빌드 후 고정 cmp50 서버/protocol·cmp41-r5 팩의 최종 집중 `test runGameTest remapJar -PhoiTestSuite=navy-layout`는2분33초에 통과했다. PNG148장, client140·protocol124·fixture 생산자66개는 실패/오류/건너뜀0이며 변경 없는 성공 결과는 재사용했다. 생성 입력44개와 공유 protocol48개를 확인했고 최종 실행 중 Java/fixture 설정352개 해시는 변하지 않았다. 실제 두 해상도에서60/20과 합성80/104/40/42를 열람했으며 그림 칸의 화면 간격은27px다. 소수 좌표를 유지해 정수 GUI 반올림으로 간격이28/26px로 번갈아 바뀌지 않게 한다. 초기 컴파일의 blit 정수 좌표 오류와 새 검사에 사용한 구버전 마우스 버튼 번호 오류는 수정 후 재검증했다. 이26.3 빌드의 InputConstants 좌/우클릭은1/3이며 숫자를 직접 쓰지 않는다. staged JAR는 client 진입점만 포함하고 허용된 모드 아이콘 외 게임 그림/음원은 없다. 채워진 원작 배속 화면, GUI2 외 렌더, 필터 없는 전체 검증·운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이며 전체 검증은 Claude 검토 후 별도 지시를 따른다.
재측정 보정 뒤 같은 집중 명령은1분50초에 통과했고 PNG148장을 남겼다. GUI2 두 해상도에서 육군 능력치 본문은y136~144(칸 위+22~+30), 해군은y152~160(+24~+32)로 모두9px다. 기술 본문은 육군y119~132·해군y133~146으로14px이며 각각 칸 위+5~+18이다. 육군 검은 칸98×34px·해군96×34px, 특성 줄 좌측 이동14px·5px를 적용했고 숫자는 검은 칸 안에서 그림과 분리된다. 실행 중 Java/fixture 설정 해시는 불변이었다. 두 해군 전구에 보이는 같은 지구본 그림은 동일 카드의 중복이 아니라 전구1의mil_1·전구2의mil_2라는 서로 다른 예비 함대다. 표시 이름과 그림이 같으며 서버 소속을 그대로 따른다.


## 원본 자산 준비와 국기 이름 호버

상단 정치 국기 단추를 가리키면 서버의 현재 국가 표시 테이블에서 읽은 나라 이름만 표시한다. cosmetic 이름이 갱신되면 같은 화면에서도 갱신하며, 이름이 없으면 태그나 추정 이름을 대신 표시하지 않는다. 클릭 동작과 플레이어 GUI 설정은 바꾸지 않는다.

연구 나무의 보병·지원·기갑·포·해군·공군·공학·산업 배경은 TFR `countrytechtreeview.gui`와 GFX가 가리키는 원본 사진을 외부 팩 `research/source/`에서 읽는다. GUI2의 기존 압축된 연구 배치에 맞춰 원본 크기의 0.5배에 연구 배율을 곱하고 나무와 함께 스크롤한다. 지원의 원본 x=-30, 기갑의 `GFX_techtree_stripes`를 반영한다. 원본 GUI의 겹침 수를 보존한다(공학 1장, 공군 3장, 나머지 2장). 원작 좌표·글꼴 전체를 복제한 것이 아니며 기존 카드·글자 크기와 배치는 유지한다. 해군 지원 장비도 `mtgnavalsupportfolder`가 지정한 `GFX_naval_techtree_bg`를 사용하며, 자산이 없는 경우에도 기존 배경으로 돌아간다. 상세 창은 아래 연구 상세 절의 계약을 사용한다.

집중 재현: 팩에서 `python scripts/build_pack.py` 후 클라이언트에서 Java 25로 `./gradlew test runGameTest remapJar -PhoiTestSuite=asset-preparation -PhoiServerSource=<validated-server-copy> -PhoiProtocolSource=<validated-server-copy/protocol/src/main/java> -PhoiResourcePack=<HOI-resourcepack/build/resources.zip> --console=plain --no-daemon`. `CountryDisplaysRenderTest`는 표시 테이블 변경 전후 이름 호버를, `ResearchBackgroundTest`는 원본 연구 fixture의 아홉 탭과 요청 없는 화면 전환을 GUI2 2560×1440/1280×720에서 검사한다. `SceneTransportModelTest`는 원본 화물선으로부터 변환한 9개 모델 자산과 모의 display 엔티티를 검사한다. 이는 서버의 해상 수송 전환·효과음·명령 승인·실제 멀티플레이 검증이 아니다. 군수산업체 가져오기는 화면·상태 구현 완료를 뜻하지 않는다.

The background render fixture activates existing button callbacks with Enter, as the existing research fixture does. It verifies rendering and absence of START/CANCEL requests; it is not proof of physical mouse selection or multiplayer behavior. The protocol comparison explicitly uses the validated server copy while the shared server checkout is being updated.

## 임무 저장 안내와 원본 공군 화살표

공군·해군 임무 단추는 현재 선택의 사전검증 응답이 아직 없어도 활성화한다. 현재 토큰·revision·선택과 일치하는 서버 `enabled=false` 응답만 임무를 막으며, 막힌 단추를 눌러도 메시지를 보내지 않는다. `enabled=true`의 안내 문구는 비활성 조건으로 사용하지 않는다. 실제 요청의 토큰·revision·중복 전송 잠금과 서버 최종 검증은 유지한다. 임무 저장 응답이 `AIR/…` 또는 `NAVY/…` 대기로 바뀌어도 별도 안내 메시지는 보내지 않고, 대상 위의 원본 `AIR_REGION_RIGHT_CLICK` 툴팁만 쓴다. 해군 `ship_activity_no_assignment`는 임무 그림으로 치환하지 않고 원본 할당 없음 그림을 사용한다.

공군 화살표는 `PlanOverlay.air`의 `AirArrow`를 페이지 0에서 받아 전체 장면 조립 후 한 번에 게시한다. 다른 국가·차원과 섞인/순서가 잘못된 페이지를 거절하며 빈 장면·권한 변경·접속 종료는 이전 화살표를 지운다. 이전 공군 상자 선분 재질은 제거되었으며 사거리 원과 결정 강조의 흰 재질은 별도로 유지한다.

`MISSION`은 `air_mission_direction`, `TRANSFER`는 `air_transfer`의 원본 패턴/마스크를 외부 팩에서 읽는다. `AirArrowOverlay`는 원본 마스크의 초록 채널과 알파를 패턴 알파에 곱한 런타임 텍스처를 만들고 팩 재로드·접속 종료에 해제한다. 원본 파일은 수정하지 않는다. 근거는 바닐라 `gfx/maparrows/maparrows.txt:986–1031`, `gfx/FX/maparrow.shader:348–400` 및 이송용 `ArrowPixelShaderNoHead`다. 임무 몸통/머리는 가로 UV의 왼쪽/오른쪽 절반, 몸통 세로 배율은 0.4, 이송은 0.41과 반복 1.5·시간 배율 0.2를 적용한다. 현재 지도 축척 4원본 픽셀/블록으로 너비·머리 길이를 환산한다. 양 끝 좌표와 종류·상태는 서버 권위 값이다.

색은 `00_graphics.lua:50–52`대로 `SELECTED` 노랑(알파 0.8), `ACTIVE` 초록(0.5), `NONACTIVE` 회색(흰색·0.2)이다. 서버의 회색 상태는 선택 여부보다 우선하며 임무와 공역이 모두 없으면 그리지 않는다. 공개 원본에는 엔진의 곡선 생성 코드가 없으므로 지형 위 리본은 양 끝을 잇는 포물선 높이 근사다. 원본의 지형 법선 왜곡·동적 조명·안개·선택 깜박임을 그대로 복제했다고 주장하지 않는다. 기존 콘크리트 선분 캡처는 이 원본 렌더의 증거가 아니다.

팩 빌드 후 Java25 집중 재현은 `./gradlew test runGameTest remapJar -PhoiTestSuite=mission-overlays -PhoiServerSource=<validated-server-copy> -PhoiProtocolSource=<validated-server-copy/protocol/src/main/java> -PhoiResourcePack=<HOI-resourcepack/build/resources.zip> --console=plain --no-daemon`이다. 자동 `missionOverlay` 입력은 `army`, `navy`, `army-strategic`, `navy-mission`, `air-arrows`, `air-transfer`다. 공군 임무의 선택·비선택·임무 없는 배정 상태는 서버 producer 결과이며 이송 fixture는 선택 상태만 제공한다. 로컬 취소/권한/재로드 주입 검사는 실제 운영 소켓·멀티플레이·저장/재접속과 구분한다. `direct-movement`는 기존 입력·대기 명령 회귀와 임무 단추를, `movement-controls`는 지휘 패널과 임무 사전검증을 포함한다. 필터 없는 전체 검증은 별도 출하 단계다.

`CommanderMissionButtonsTest`의 이전 검증은 모의 Context/Availability와 일반 HUD를 사용했고, `hoi-retreat-synthetic-*`는 일반 이동선에 RETREAT 종류만 주입했다. 현재 검사는 아래의 실제 서버 생산 입력으로 이를 보완한다. 회색 공군 원작 사진은 여전히 미확정이며 원본 색상 정의와 서버 fixture를 근거로 삼는다.

고정 cmp50 서버와 같은 protocol을 명시한 집중 `test runGameTest remapJar`의 `direct-movement`, `mission-overlays`, `air-range`가 통과했다. 공군 사거리 채움/점선의 기존 표시와 선택 해제도 검사한다. client 145개·protocol 124개의 단위 검사 실패/오류는 0이며 변경 없는 protocol 성공 결과는 재사용했다. 해군 안내의 제목 겹침을 실제 캡처에서 발견해 임무 창 머리 영역 위로 옮기고 회귀 검사를 추가했다. client 전용 staged JAR에는 기존 모드 아이콘 외 정적 그림·음원이 없다. 전체 무필터 실행·운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이다.


실제 손 후퇴·강제 후퇴 상태와 임무 저장 전후 입력은 `mission-overlays`와 `direct-movement`로 검사한다. `UnitMovementOverlayTest`는 `army-retreat-manual`·`army-retreat-forced`의 실제 후퇴 쿼드, 세 단계 `RETREATING`, 원본 텍스처의 회색 표시와 이동 진행을 확인한다. 강제 후퇴 입력은 같은 전투 저장에서 `manualRetreats`를 비운 것으로 자연스러운 조직력 붕괴 과정까지 재현하지 않는다. `cancelled` 이름의 캡처는 클라이언트 수신 상태 비우기 검사이며 강제 후퇴 취소가 허용된다는 뜻이 아니다. 서버 producer는 손 후퇴 취소와 강제 후퇴 취소 거부를 별도로 검사한다. 원작 후퇴 사진이 없어 원본 텍스처와 서버 상태를 근거로 하며 원작 화면과 픽셀 일치를 주장하지 않는다.

임무 상태 검사는 `CommanderMissionWaitingFixturesTest`·`NavalRightClickTest`가 실제 캠페인에서 명령을 실행해 출력한 지역 없는 임무와 지역 할당 뒤 HUD·Context·Availability를 재생한다. 재생할 때 dimension과 요청 식별자(token/revision/sequence/request)는 시험 환경에 맞춘다. 공군은 비행단 하나, 해군은 대상 기동부대 하나를 선택하여 저장 뒤 안내와 지역 할당 뒤 상태 변화를 확인한다. 화면의 처리 함수를 호출하고 응답을 주입하므로 물리 마우스부터 실제 서버 저장까지 한 연결에서 수행하는 종단간 검증은 아니다.

## 항구 간 해상 수송 표시

기존 육군 MAP/LAND 요청을 유지하며 서버가 승인한 해상 수송의 MOVE 화살표를 기존 이동 렌더러로 표시한다. 첫 해역 도착 이후 수송 모델과 도착 항구의 병과 모델 복원은 서버 Polymer가 결정한다. 클라이언트에서 승선 시점이나 이동 상태를 추정하지 않는다. ORDER_CONVOY_TRANSPORT는 외부 원본 ui.order.convoy_transport를 재생한다.

집중 재현은 HOI_MISSION_OVERLAY_FIXTURES=<고정 서버 입력>/mission-overlay-fixtures를 설정한 뒤 팩 빌드 및 Java25 test runGameTest remapJar -PhoiTestSuite=sea-transfer -PhoiServerSource=<고정 서버> -PhoiProtocolSource=<고정 서버>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon이다. army-sea-transfer.json의 출발·진행 해로를 확대/축소 없이 평탄한 시험장으로 평행 이동하여 실제 GUI2 두 해상도로 렌더한다. 빈 오버레이 제거와 다른 국가 입력 거부는 클라이언트 수신 검사이며 서버 수송 취소 명령의 증거가 아니다.

수송 모델은 원본 화물선의 선체·갑판·화물창·마스트·선교를 나눠 배치한 163큐브 근사다. 텍스처는 보병·전차와 같은 도트 방식이며 원본 재질 색을 바탕으로 978개 면을 각각 독립된 8×8 영역에 그린256×256 아틀라스다. UV 영역을 공유하지 않아 개별 면을 수정할 수 있다. 면별 고해상도 UV 샘플링은 사용하지 않는다. 팩 생성기는 부품명·UV·텍스처를 포함한 `build/army-models/transport.bbmodel` 편집본도 만든다. 배율3의 경계 크기는 약0.400×0.756×2.951블록이며 기존 수송 모델 ID9개를 유지한다. `SceneTransportModelTest`는 `asset-preparation` 집중 검사와 전체 검사에서 실제 지도 fixture의 바다에 모델을 놓고 GUI2 두 해상도로 렌더한다. CommanderSeaTransferTest는 army-sea-transfer.json의 sea 3단계와 signal을 읽어 gameMinute 0·1332·8532의 병과→수송선→병과 모델과 카운터 상태를 재생한다. 각 단계의 원본 위치를 시험 카메라 앞으로 동일하게 평행 이동하고 실제 yaw를사용한 모의 item_display를 그린다. 바다 위를 포함해 세 단계 모두 카운터 선택을 검사한다. 이는 실제 producer 출력의 화면 검증이며 서버 Polymer가 소켓을 통해 모델을 바꾸는 종단간 검증은 아니다. 아홉 모델·효과음 로드와 Master 채널 재생도 별도 검사한다. 운영 접속·물리 입력에서 저장까지의 종단간 검증·멀티플레이·직접 음향 청취는 미수행이다.

진행 화살표는 전체 해로보다 짧으므로 해당 캡처만 카메라를 가까이 옮긴다. 서버 좌표·UV·굵기는 바꾸지 않는다. `mission-overlays`와 전체 오버레이 순회에도 `army-sea-transfer`를 포함하며 고정 fixture 목록에서 파일 존재를 확인한다.


## 군수산업체 목록·세부·정책

정치 연구 & 생산 네 칸은 다른 줄의 2~5열 자리에 놓고 1·6열은 비운다(사용자 v47 지시). 네 칸은 서버 manufacturers의 armor/navy/air/materiel 필터로 목록을 연다. 특성 점수 보유는 points>0이며 전체 목록 필터가 아니다. 명시적으로 정렬하기 전에는 서버의 원본 파일 순서를 유지한다. 규모·이름 정렬, 한 업체의 복수 분류, 서버 로고·규모·유형·장비별 특성 수를 보존한다. 목록은 최대 다섯 행 정도의 높이에서 휠·스크롤바 끌기로 이동하고, 세부 사항은 기존 Commander Open(ORGANIZATION, 업체 ID) 경로를 사용한다. 장비 토큰은 원본 GFX_military_industrial_organization_<token> 또는 GFX_mio_cat_* 정의로 대응한다.

IndustryOrganization은 organization/detail·organization/policies를 기존 대화 세션으로 받는다. summary의 진행도·자금 툴팁, points, 속성, 장비별 설계/생산 수, 정책과 잠금 표시, header/trait 좌표·선행·배타 관계·획득 여부, queue/history 타일은 서버 값을 사용한다. 특성 툴팁은 원문·색·공백·줄바꿈을 유지하며 길면 툴팁 위 또는 Shift+휠로 끝까지 읽는다. 노드 종류 mio_special은 원본 월계관, mio는 톱니 프레임이다. 부모 조건은 needed와 parents를 따르고 호버한 노드의 선행선을 노랗게 표시한다. 나무 빈 곳의 사용 조건은 서버 body가 제공된 경우만 표시하며 없는 조건을 추정하지 않는다.

좌클릭 unlock과 정책 attach는 서버 Choice.tooltip을 확인창 본문으로 사용한다. 취소는 로컬 창만 닫으며 확인 때 발급된 선택지를 현재 token/revision으로 한 번 보낸다. Shift+클릭은 queue/unqueue, 대기열 X는 unqueue다. 끌어 놓기는 up/down 한 건을 보내고 다음 revision에서 실제 순서 진전을 확인한 뒤 필요한 다음 요청을 보낸다. 같은 revision·다른 token·오래된 응답은 대기 잠금을 해제하지 않는다. 거절되어 순서가 그대로인 새 응답은 추가 이동을 중단한다. 미발급 선택지를 만들어 보내지 않으며 UI가 자금·특성·정책을 낙관적으로 변경하지 않는다.

원본 틀은 TFR interface/military_industrial_organization/industrial_organization_list.gui·industrial_organization_detail.gui·industrial_org_policy.gui, 사진 20261024072205~072623 및 072358~072435이다. 정책의 국가별 그림 경로가 팩에 없으면 같은 GFX 이름의 공통 mio/ui 원본을 사용한다. 정책 비용의 GFX_pol_power_icon은 기존 pol_power 글리프와 동일한 원본 정치력 아이콘으로 대응한다. 대기열 안내는 번역 패치 3411756715의 localisation/korean/replace/TFR_misc_l_korean.yml:98 INDUSTRIAL_ORG_QUEUE_WINDOW_INSTRUCTION을 사용한다. 사용자 지시로 특성·이력 탭은 생성하지 않는다. 이력 타일을 보여 주던 이전 구현을 현행 표시로 간주하지 않는다.

아이콘 문법은 `£` 뒤 영문·숫자·밑줄(선택적 `|프레임`)을 그림 이름으로 찾고 바로 다음 한 글자를 구분자로 소비한다. 공백·한글·마침표에 같은 규칙을 쓴다. 따라서 `£command_power의 지휘력이`와 `£command_power. 지휘력을`은 아이콘 뒤 조사/마침표 없이, `£command_power만큼의`는 아이콘 뒤 `큼의`로 표시한다. 야전 장교의 서버 문구는 `£command_power 만큼의`로 보내 `만`을 보존한다. 색상이 나뉜 컴포넌트에 걸친 토큰도 같은 규칙이며 뒤 글자의 스타일은 유지한다.

재현: 팩에서 python scripts/build_pack.py 후 Java25 클라이언트 test runGameTest remapJar -PhoiTestSuite=organizations -PhoiServerSource=<검증된 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon. HOI_COMMANDER_PREVIEW에는 같은 서버의 kor-mio-menu, kor-mio-detail-lig, kor-mio-detail-lig-unlocked/points/policy, kor-mio-policies-lig/ready/attached를 둔다. 지휘관은 commander-contract와 commander-layout으로 재현한다. GUI2 2560×1440·1280×720의 실제 렌더와 합성 MouseButtonEvent/모의 전송 검사이며 물리 입력→소켓→서버 비용·저장 종단간, 운영 접속·멀티플레이·직접 음향 청취·필터 없는 전체 검증은 별도다.

정책·대기열은 기존 세부 창 위에 표시하고, 정책의 back 선택지는 X 닫기에 연결한다. 미획득 노드는 어두운 프레임을 사용하고 대기열 번호는 오른쪽 아래 원본 초록 배경에 표시한다. 왼쪽 속성은 한 줄, 장비 목록은 별도로 스크롤한다. 특성 툴팁은 노드 오른쪽 아래와 공통 최대 폭의 보라→분홍 테두리를 사용한다. 실제 points·ready·attached·policy 고정 입력으로 특성 확인·정책 적용 전후와 잠금을 검사하며, 획득 후 툴팁은 generic_mio_trait_rapid_fire를 직접 호버해 확인한다.

TraitNode.bonuses의 서버 순서대로 research→GFX_design_team_icon, equipment→GFX_industrial_manufacturer_icon, production/organization→GFX_organization_modifier_icon 원본을 사용한다. 마지막 두 범주는 같은 공장 그림을 사용하며 원본 엔진의 내부 그림 선택과 같다는 주장은 하지 않는다. 한국 목록과 LIG 유형을 검증했으며 모든 국가·유형을 검증한 것은 아니다.

## 생산 라인 군수산업체·연구 설계국

생산 라인 헤더는 최신 화면 지시에 따라 업체 유무와 관계없이 TFR `GFX_production_item_has_mio` 원본512×30 배경을 사용한다. 실제 업체 로고·배정 가능 여부는 서버 응답을 따른다. 접기 버튼은 원본 그림의 크기·배치를 유지하되 투명 하단 여백의 클릭 영역을 헤더 끝에서 잘라 장비 교체와 겹치지 않게 한다. 접힌 장비 배경은 원본278×26을 (0,33)에 그리며 장비 그림은 별도로 (80,42)를 중심으로 원본 픽셀의 .37배율을 적용한다. 배경은 진행률보다 먼저 그려 수치·막대를 가리지 않는다. 자원 칸은 원본 `production_resource_item`의 아이콘(5,4)·.7배율과 숫자(18,5)·폭30을 사용하고 펼침 간격24, 접힘 간격46을 유지한다. 숫자의 세로 정렬은 아래 공통 글꼴 기준선을 따르며, 라인별 +1GUI 보정을 중복 적용하지 않는다. 생산·건설 행의 빈 배경에는 `PRODUCTION_DRAG_N_DROP`을 표시하며 작업·공장 진행률·버튼별 툴팁이 우선한다. 재현은 `queue-drag`, `organization-assignment`, `industry` 집중 검사와 필터 없는 전체 검사다. 생산 캡처는 서버 입력이고 건설 드래그 캡처는 모의 요청 검사용 합성 입력이므로 실제 캠페인 화면과 구분한다.

생산 라인 머리의 렌치+ 또는 업체 로고를 좌클릭하면 서버가 발급한 후보와 ‘군수산업체 없이 생산’ 행을 연다. 로고 우클릭은 제거다. 규모·이름 정렬과 기존 조직 세부 창 연결을 제공하며 요청은 MANUFACTURER(session, revision, lineId, organizationId 또는 빈 값, 0)이다. 응답 대기 중 반복 요청을 차단하고 서버 상태를 받기 전에 로고나 혜택을 바꾸지 않는다. 후보 없는 빈 칸은 서버 툴팁을 표시하고 배정 요청을 보내지 않는다. 장비 파생형 설계국 배정은 서버 미지원 범위로 남긴다.

연구 상세의 연구 단추는 적합한 업체가 있으면 선택 창을 열고 Shift+클릭 또는 적합한 업체가 없으면 업체 없이 시작한다. 선택 창은 원본 파일 순서로 시작하며 규모·이름·비용 정렬, 서버 일일 비용·자금·바쁜 업체의 현재 기술을 표시한다. 호버한 업체의 technologies/rates로 남은 일수를 미리 표시하며 현재보다 짧으면 ▼를 붙인다. 진행 중 연구는 DESIGN_TEAM만 보내고 빈 행은 기존 배정을 해제한다. 새 연구에 업체를 고르면 START만 먼저 보내며, 같은 세션·국가의 더 높은 revision에서 정확한 슬롯·기술이 시작됐음을 확인한 뒤 DESIGN_TEAM을 보낸다. 거절·오래된 응답·시간 초과·화면 종료는 후속 배정을 보내지 않는다. 서버가 다른 연구에서 업체를 옮기는 동작은 서버 권위다. 연구 슬롯 로고의 툴팁은 서버 혜택과 해당 기술 자금만 표시하고 할당 안내를 덧붙이지 않는다.

재현은 위 Gradle 명령의 -PhoiTestSuite=organization-assignment를 사용한다. HOI_COMMANDER_PREVIEW에 kor-production-mio-unassigned/assigned와 kor-research-design-team-before/after를 둔다. IndustryOrganizationAssignmentTest는 실제 렌더와 합성 클릭으로 배정·제거·반복 차단, START 승인 후 새 revision 배정, 거절·중복 응답, Shift 시작과 업체 없이 연구를 검사한다. 연구 시작 가능 상태는 실제 연구 중 fixture를 변형한 명시적 합성이며 정책/특성 가능 상태와 배정 전후는 실제 producer 출력이다. 두 해상도의 GUI2 검증과 실제 서버 비용·저장·멀티플레이 검증을 구분한다.


## 군 패널 원본 툴팁·편제 변경·해산

군 카드의 전력 막대는 `Panel.forces`(아군 / 아군과 적군의 합)를 표시한다. 적이 없어 null이면 채우지 않는다. 계획 진행도와 섞지 않으며 `tooltips.forces`를 그대로 표시한다. 실행 단추는 기존 `ACTIVATE`/`DEACTIVATE`의 서버 툴팁과 `planEvaluation` 0/1/2 표시를 유지한다.

군 패널의 보급·연료·평균 조직력/전투력·빈 지휘관 초상·전략적 재배치와 사령부는 서버 `Panel.tooltips`·`Headquarters.tooltip`을 사용한다. 사단 줄은 `MemberData.tooltips`의 참호·상세 참호·적응·조직력·전투력·연료·편제를 각 원본 아이콘/막대에 연결한다. 참호 상세는 같은 삽 아이콘에 1초 머물면 표시하고 다른 영역으로 이동하거나 화면 상태를 초기화하면 지연을 다시 센다. 1초는 원본 엔진의 미확정 지연에 대한 클라이언트 선택이다. 서버가 보내는 기술·교리·국민정신별 조직력·참호 출처와 날씨 줄은 원문 순서로 표시하며 £tech_mod/£doctrine_mod는 원본 아이콘으로 표시한다. 서버에 없는 연료 종류별 상세·기반시설·신뢰도 계산을 만들어 채우지 않는다.

사단 줄 방패는 `DIVISION_VIEW_SELECT_ARMY`로 소속 군을 선택한다. 머리 방패의 `CHOOSE_INSIGNIA`, 세부 `OPEN_LEADER_DETAILS`, 열차포 `RAILWAY_GUN_CLICK_TO_SELECT_ASSIGNED`, 배정 해제 `CONFIRMUNASSIGNUNIT(S)`는 원본 키를 쓴다. 수송 중량 툴팁은 `TRANSPORT_WEIGHT_DESC`만 표시한다. 통폐합은 서버 기능이 없어 원본 불가능 설명과 조건으로 비활성화한다.

편제 후보는 `template/` 타일로 목록을 만들며 같은 ID의 Choice가 없는 행은 회색이고 선택 요청을 보내지 않는다. 원본 TFR `interface/unitview.gui`의 `divisions`·`unit_template_entry`와 이미 가져온 `GFX_tiled_window_1b_thin_border`·`GFX_mini_tooltip`을 재사용한다. 25GUI 행, 5개 표시, 휠·스크롤 막대 끌기를 사용하며 이름은 왼쪽 정렬 두 줄이며 더 긴 내용은 둘째 줄에 이어 가로 스크롤한다. 병과 그림은 48×23GUI, 스크롤바는 ▲/손잡이/▼이며 막힌 행 툴팁은 행 오른쪽에 둔다. 서버가 발급하지 않은 보충 우선순위는 그리지 않는다.

사단 해산·편제 변경 확인창은 기존 TFR 공통 원본 틀과 버튼을 쓰며 본문의 순서·색·빈 줄·인력/장비 수량을 서버 응답 그대로 표시한다. 긴 본문은 버튼을 고정하고 스크롤한다. 발급된 확인만 OK로 표시하고 취소·Esc·반복 클릭·같은 revision 재응답에는 중복 요청을 보내지 않는다. 화면에서 자산을 차감하거나 사단을 미리 제거하지 않는다.

재현: `HOI_COMMANDER_PREVIEW=<검증된 fixtures>`를 지정하고 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=army-tooltips -PhoiServerSource=<고정 서버> -PhoiProtocolSource=<고정 서버>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 실행한다. 운영 서버와 분리된 개발 렌더 검사다. GUI2 2560×1440·1280×720에서 실제 렌더와 합성 클릭/모의 전송을 검사한다. `kor-army-panel-tooltips`는 카드 한 개이므로 View에 감싸 시험 차원으로 바꾸며, `kor-disband-confirm`은 서버 producer 응답이다. 적 없는 카드의 전력 25% 및 적응 아이콘 가시성, 긴 해산 본문과 거절 상태는 합성 경계 검사다. 검증된 편제 목록·변경 확인 producer 입력이 없으면 해당 두 화면은 합성 목록/본문으로 검사하며 실제 특수부대 상한·필요/획득 계산 검증으로 해석하지 않는다. 편제 아이콘의 클릭은 기존 사단 선택/우클릭 집중 동작을 보존하며 툴팁에 있는 편제 설계·모병 단축 입력의 서버 기능을 새로 구현하지 않는다. 실제 서버 소켓·편제 비용/해산 반환·저장·운영 접속·멀티플레이·직접 음향 청취·무필터 전체 검증과 구분한다.


군 카드 전력 막대는 별도 클릭 영역을 만들지 않고 기존 카드 하단 요청 경로의 툴팁만 바꾼다. 기존 부대 배정/계획 동작을 가로채지 않는다. `army-groups`는 `commander-controls`의 군 카드/선택/끌기/사령부 구성 검사만 반복하는 집중 suite이며 기존 전체 호출과 검사를 유지한다. 일부 고정 서버의 자동 생성 `armyHud` 묶음에는 `kor-promote-confirm`, `kor-army-panel-tooltips`, `kor-disband-confirm` 및 편제 입력이 없어 해당 결합 suite나 무필터 검사가 입력 누락으로 중단될 수 있다. 서버 exporter에서 검증된 입력을 추가하기 전에는 위 명령의 `HOI_COMMANDER_PREVIEW`를 완전한 검증 fixtures로 지정한 집중 검증 범위를 보고한다. 입력이 없는 편제 화면을 합성 검사만으로 실제 계산 완료라고 보고하지 않는다.


## 연구 상세·영토 반환·함장과 야전 장교

연구 상세는 `Tech.upgrades`의 범위·능력치·지형 효과와 `Tech.units`의 잠금 해제 부대 능력치를 서버 순서대로 표시한다. 표시 문자열의 정밀도는 다시 계산하지 않으며 지형 비율만 해당 비율 단위의 백분율로 표시한다. null 지형 값은 숫자를 채우지 않는다. 지형 원·장화/칼/방패 및 부호는 원본 `GFX_terrain_mod_*`·`GFX_adjuster_*` 그림을 쓴다. 장비의 자원·사진·능력치는 기존 `research_details.json`에 남으며 국가별 장비 보정은 서버에서 아직 제공하지 않는다. 상세 창은 원본 금속 머리와 어두운 줄무늬 본문, 얇은 테두리로 구성하고 본문만 스크롤한다. 연구 설계국 선택 창은 상세와 상단을 맞추고 작은 화면에서도 두 창이 겹치지 않게 옮긴다. 설계국(군수산업체) 창을 열어도 연구 분류 탭은 숨겼다가 상세 제목 위에 다시 그리지 않으며 상세 창 머리줄이 탭 뒤로 밀리지 않는다. 상세·설계국 창이 덮지 않은 분류 탭과 닫기 단추는 계속 그리고 누를 수 있으며, 분류를 바꾸면 설계국 창을 닫는다. 설계국 창은 원본 `industrial_organization_list.gui`의 `industrial_organisation_list_window`(550×667px, 사진 `20261118210011_1.jpg`)를 GUI2 275×334 GUI로 그리고, 원본 `position_technology`처럼 상세 창 오른쪽 끝에 투명 여백 2 GUI를 겹쳐 붙인다. 배경은 `GFX_tiled_bg`, y42부터 목록 틀 `GFX_tiled_window_thin_border2`(둘 다 64px 모서리·가운데 반복)이고 제목은 흰색 `INDUSTRIAL_ORG_LIST_WINDOW_TITLE`이다(툴팁용 노란 `INDUSTRIAL_ORG_TOOLTIP_HEADER`를 쓰지 않는다). 닫기는 테두리 없는 원본 `GFX_closebutton`(x256/y2, 16×17 GUI)이다. `sort_size`·`sort_name`·`sort_cost`는 x8/x23/x65·y24에 `GFX_sort_skill` 15×15 GUI와 `GFX_sort_up_down_button_83x29`(팩 `mio/ui/gfx_sort_up_down_button_83x29`) 41×15 GUI로 그리며 셋 다 누를 수 있다. 정렬하지 않은 단추는 프레임 0, 현재 정렬 기준은 오름차순 프레임 1·내림차순 프레임 2이고, 원본 `click_scroll`(`ui.scroll`)을 재생한다. 규모 단추는 업체 규모로 정렬하며 원본 툴팁이 없어 툴팁을 두지 않는다. 문구는 원본 키 `mio_sorting_name`·`mio_sorting_task_cost`의 `이름`·`비용`이다. 항목은 원본 520×100px `GFX_mio_entry_bg`(260×50 GUI)를 x8·y48부터 50 GUI 간격으로 놓고, `세부 사항`(`INDUSTRIAL_ORG_OPEN_DETAILS_WINDOW`)은 수용량 칸이 없는 연구용 위치 `details_button_position_no_task_capacity`(항목 x200·y25, 46×16 GUI)이며 글자가 잘리지 않도록 기본 글자 크기(GUI2 2px)로 그린다. 작은 화면에서는 창 오른쪽 끝이 연구 화면 닫기 단추(오른쪽 25 GUI)를 덮지 않도록 상세 창과 함께 왼쪽으로 옮긴다. 공용 항목 그림(`IndustryOrganizationRow`)은 원본 `industrial_organisation_list_item`대로 로고 원본 크기(x5/y10), 규모 배지 x40/y10, 이름·특성 x63, 장비 아이콘 x60+35n·y25(0.8배 19 GUI), 특성 수 x81+35n을 쓴다. 장비 칸은 원본 `OverlappingElementsBox`(350px)처럼 5개까지 35 GUI, 그보다 많으면 140/(개수-1) GUI 간격이며 아이콘 크기는 줄이지 않는다. 원본 사진 `20261118220938_1.jpg`처럼 여섯째 칸은 `세부 사항` 단추 밑에 놓이고 단추가 위에 그려진다, 비용 오른쪽 끝 x233을 쓰며 생산·국가 메뉴 목록도 같은 항목 그림을 쓴다. 원본 `GFX_tiled_generic_overlay_bg1_small` 가장자리 음영은 현재 팩 PNG에 알파가 없어 그리지 않는다. 일수 미리보기의 숫자와 ▼ 사이에는 공백을 두며 일 단위는 흰색이다.

점령지의 영토 반환은 `Administration.returnTerritory && enabled`일 때만 확인창을 연다. 본문은 `confirm`, 툴팁은 `tooltip`이며 취소는 요청을 보내지 않는다. OK는 해당 행의 state/country와 현재 token/revision으로 `RETURN`을 한 번 전송한다. 응답 전에는 행을 지우지 않는다. 반환 대상이나 발급된 확인 내용이 바뀌면 열린 확인창을 폐기하고 다시 확인해야 한다. 날짜만 갱신되면 창을 유지하고 최신 화면 revision으로 요청한다. 확인창 동안 배경 조작과 자동 새로고침을 막는다. 국가 해방은 서버가 발급하지 않아 비활성 상태를 유지한다. 핵심주 수 툴팁은 `OCCUPIED_COUNTRY_CORE_COUNT_TOOLTIP`의 `NUM_CORES`, `NUM_CONTROLLED_CORES`를 실제 전체·통제 주 수로 채운다.

임명 목록의 `officers`/`captains` 선택지를 받았을 때만 야전 장교/함선 지휘관 단추를 켠다. `CommanderOfficerRoster`는 원본 목록 틀과 행에 부대 그림·숙련도·초상·직함/이름·부대명·경험치를 표시하고 숙련도/유형/표창/경험치 정렬, 검색, 휠·막대 끌기, 서버 발급 page/N 이동을 제공한다. `promote/<section>`이 발급된 행만 진급할 수 있으며 세부 사항은 서버가 지원하지 않아 회색이다. 같은 revision·반복 클릭·미발급 요청은 전송하지 않는다. 진급 확인은 장교 목록을 닫고 원래 장군/제독 임명 창 위에 표시하며 OK/취소는 서버 선택을 보낸다. 야전 장교 확인 문구는 서버의 `£command_power 만큼의`를 그대로 받으며, 공통 한 글자 구분자 소비 규칙으로 공백만 먹고 ‘만큼의’를 보존한다. 장교단·역할·인물 선호 전술은 기존 권한과 국가 선호 전술의 독립 상태를 유지한다. 선호 전술 창은 GUI2에서 원본 폭을 유지하고 세로 공간이 부족할 때만 축소한다.

공통 사단/장교 확인창은 233×154GUI(466×308픽셀)를 기준으로 버튼을 고정하고 긴 본문을 스크롤한다. 사단·장교 확인창의 스크롤바는 실제 내용이 표시 영역을 넘칠 때만 표시한다. 사단 해산의 §R은 밝은 빨강, 지휘관 진급 비용/감소는 어두운 빨강을 사용한다. 군 툴팁은 커서 오른쪽 아래에 배치하고 최장 표시 줄에 폭을 맞추며 긴 하이픈 구분선은 공통 규칙으로 빈 줄로 바꾼다. 기본 글꼴과 크기를 유지하므로 원본 엔진의 줄 높이와 픽셀 동일하지 않다. MIO 왼쪽 속성·장비는 하나의 스크롤 영역으로 잘라 정책 단추 위로 새지 않으며 특성 툴팁에는 기존 원본 능력치 아이콘 대응을 적용한다. 트리 머리는 원본 `GFX_tiled_plain_bg_small`의 네 귀 리벳을 사용한다. 장교 오른쪽 방패는 `btn_army`의 소속 군·함대 휘장으로 확인됐다. `unitLevel`은 왼쪽 아래 숙련도에만 사용한다. 오른쪽 휘장은 `Officer.insignia`의 0~80 프레임을 사용하며 null인 미소속 부대에는 방패를 그리지 않는다. 프레임은 `Officer.insignia`를 그대로 사용하며 함장 기본값은 서버 `DEFAULT_FLEET_INSIGNIA=3`이다. 과거 0번 기대값을 현행 기본값으로 사용하지 않는다.

재현: 검증된 서버와 같은 protocol, 팩 빌드를 준비하고 `HOI_COMMANDER_PREVIEW=<완전한 fixtures>`를 지정한다. Java25에서 `test runGameTest remapJar -PhoiTestSuite=final-contract -PhoiServerSource=<고정 서버> -PhoiProtocolSource=<고정 서버>/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 실행한다. `final-review`는 배정·MIO·군 툴팁·편제/해산, `officer-comparison`은 장교단/역할/선호 전술 비교와 기존 전술 입력 검사를 실행한다. 각 suite는 GUI2 2560×1440·1280×720에서 실제 렌더와 합성 입력/모의 요청을 사용한다. wrapper는 provider run 밖에서 띄우고 종료까지 확인한다.

`final-contract` 입력은 `kor-research-sections`, `kor-field-officers`, `kor-field-officer-promote`, `kor-ship-captains`, `kor-ship-captain-promote`, `kor-army-roster-tooltips`, `kor-navy-admirals`, `occupation-return-view-before/after`다. 연구 세 기술은 실제 producer 자료를 빈 슬롯 시험 세션에 넣고, 점령지는 실제 전체 View와 반환 후 View를 재생한다. 이 점령지는 초기 저장에서 도쿄와 아르메니아를 한국에 넘긴 시험 장면으로 실제 전쟁 점령 재현이 아니다. 편제 목록의 허용 7행/상한 거절 4행 및 변경 확인은 `kor-template-list/confirm` producer 입력을 사용한다. 긴 본문·미발급·동일 revision 등의 경계 검사는 합성 상태임을 구분한다. 실제 소켓→서버 비용/반환→저장, 운영 접속, 멀티플레이, 직접 청취, 모든 연구 유형/국가 및 무필터 전체 검증은 별도다. 판 끝 무필터 검증에서는 서버 전체 검증으로 생성한 완전한 fixtures를 `HOI_COMMANDER_PREVIEW`로 고정하며 불완전한 자동 생성 묶음과 섞지 않는다.

### 원본 대조 보정과 전체 재현

연구 상세는 전체 폭 금속 머리, 안쪽 줄무늬 몸통, 흰 제목, 일수 칸과 원본 연구 단추를 사용한다. 설명을 문장 마침표마다 강제로 자르지 않는다. 효과 줄은 툴팁의 `효과:` 머리말 없이 바로 나열한다(원본 `20261121090447_1.jpg`·`20261121090455_1.jpg`). 원본은 국가 수정치를 `RESEARCH_MODIFIERS_HEADER`(전체 효과) 아래 아이콘·이름·오른쪽 정렬 값(`+20.0%`)으로, 툴팁에서는 빈 줄 뒤 `아이콘 이름: +20.00%`로 표시한다. 서버가 수정치를 구조화해 보내지 않아 이 표는 아직 효과 줄로 남은 차이다. `Upgrade.scopeId`가 분류이면 원본 병종 아이콘과 이름, 개별 부대이면 아이콘만 표시한다. 효과와 장비·모듈·부대 값은 오른쪽 정렬한 두 칸 표이며 지형 배열은 서버 순서를 유지한다. 기술 그림이 없으면 범주 대체 그림을 그리지 않는다. 짧은 창도 267GUI 높이를 유지하고 긴 창은 화면 아래까지 확장한 뒤 필요한 경우 본문만 스크롤한다. 원본의 210·263·설계국 적용 192일은 내림 계산과 일치한다. 설계국 호버는 최소 1일의 내림을 사용한다. 공유 `Tech.remainingDays`와 설계국 호버 모두 최소 1일의 내림을 사용한다.

연구 슬롯은 46GUI 간격과 42GUI 높이로 배치하고, 화면에 들어가는 슬롯 수에 맞춰 스크롤한다. 이름·연도·일수·로고가 서로 겹치지 않게 분리한다. 배정된 슬롯은 원본 `GFX_research_line_mio_bg`의 톱니 배경을 쓰며, 진행 중에는 같은 그림의 빈 격자 영역을 UV로 재사용해 중앙 플라스크를 덮는다. PNG 원본은 수정하지 않는다. 로고 툴팁은 슬롯 오른쪽에 표시한다.

점령지의 저항 없는 나라 행에는 `GFX_occupation_country_entry_bg_top_locked`를 사용하고 0인 공장·자원은 `---`로 표시한다. 반환/해방 단추의 흰 글자를 유지하며 권한 없는 클릭은 차단한다. 제목의 주둔군 기록·장비 문서 그림은 원본 자리의 장식이며 관련 서버 기능이 제공되지 않아 새 동작을 연결하지 않는다. 반환 확인은 배경을 어둡게 하지 않고 본문을 가운데 정렬한다. 원본 `countryoccupationview.gui`처럼 점령국 윗줄(`top_bg`, 국기·국가명·점령 주 수)과 각 점령 주 행에 `OCCUPATION_ZOOM_TO_STATE_TOOLTIP`(§G클릭§!하여 점령한 주로 확대)을 붙이고, 클릭하면 서버에 `ZOOM`을 보내 해당 주(국가 윗줄은 그 국가의 첫 점령 주)로 이동한다. 펼치기 단추(`expand`, x9/y38)는 원본처럼 툴팁 없이 펼치기·접기만 한다. 주 행의 이동 영역은 주둔 편제·통치 방식 단추와 저항 효과 툴팁을 가리지 않도록 이름 쪽(280×26)으로 제한한다.

장교단은 작은 해상도에서도 275GUI 폭을 유지하고 세로로 스크롤한다. 사령부 수는 서버 값을 그대로 사용한다. 비교 입력 `kor-selected-menu`는 0/1, `kor-mio-menu`는 0/0이므로 같은 상태로 비교하지 않는다. 선호 전술 설명은 기본 글꼴의 정수 배율을 쓰고 줄 수에 따라 카드 시작 위치를 내린다. 비교 자료는 `kor-leader-tactics-low-skill`과 `kor-leader-tactics-choices`의 실제 producer 카드로 잠금·모두/공격/방어 탭을 검사한다. 국가 전술과 인물 전술의 선택 세션은 별개다.

집중 검사는 `final-contract`, `final-review`, `tooltip-spacing`, `officer-comparison`을 사용한다. 전체 검사는 같은 명령에서 `-PhoiTestSuite`를 생략한다. `HOI_COMMANDER_PREVIEW`에는 검증된 서버가 만든 전체 fixtures를 고정하고, `HOI_MISSION_OVERLAY_FIXTURES`에는 같은 사본의 임무 입력을 둔다. 나머지 producer 입력은 Gradle의 `prepareRendererFixtures`가 같은 `hoiServerSource`에서 생성한다. PowerShell wrapper를 `Invoke-CimMethod -ClassName Win32_Process -MethodName Create`로 provider run 밖에서 시작하고, 로그·exit 파일과 프로세스 종료까지 확인한다. 실제 소켓 요청에 따른 비용/반환/저장, 운영 접속·멀티플레이·직접 음향 청취는 이 렌더 검증의 범위가 아니다. 원본과 국가·인물·상태·글꼴이 다른 비교를 픽셀 동일성으로 보고하지 않는다.

함장 행 왼쪽은 서버의 `officer/ship_unknown`을 원본 `GFX_navalcombat_ship_icon_unknown`으로 연결한다. `navalcombat.gfx`가 선언한 검은 선체·흰 물음표 원본을 외부 팩에서 읽고 CV/CL/DD가 포함된 선체별 그림은 함장 목록에서 사용하지 않는다. 야전 장교의 부대 그림 경로는 유지한다. 중단된 1935 무필터 시도는 전체 검증 완료로 세지 않는다. 최종 검증은 일수 내림과 `Officer.insignia` 계약이 포함된 동일한 고정 서버·protocol·fixtures로 실행한다.

지휘관 임명 목록은 영역 끝의 부분 행을 scissor로 표시하고, 잘린 조작부는 비활성으로 처리한다. 장교 부대명은 행의 y+14에 놓아 배경 장식에서 분리한다. 연구 상세의 긴 설명은 머리 높이에 반영하고, 화면을 넘는 설명은 스크롤 본문으로 이동한다. 미확정 엔진 동작(함대/태스크 포스 휘장 선택, 슬롯 자금 995/1000, 정찰전차 인력 950/450, 교리 트랙 구조와 동적 특성 그림)과 실제 소켓·비용·저장·멀티플레이·직접 음향 청취는 렌더 검증과 구분한다.

육군 계획 목표 대화 창의 드래그는 일반 지도 이동과 별도로 여러 ray를 수집한다. 일반 이동은 누른 첫 ray를 유지하고, 계획선 입력은 최대 64개와 해당 창의 token/revision을 보존한다.

소형 연구 슬롯의 스크롤 접근성은 `test runGameTest remapJar -PhoiTestSuite=research-slots`로 재현한다. 854×480에서 서버가 준 5개·6개 슬롯 각각의 화면 안 배치와 마지막 슬롯까지의 도달성을 검사하며, 휠 입력 한 번은 한 슬롯 이동으로 처리한다.

## 원본 스크롤과 설계 창

사단 편제의 채워진 대대·지원중대 칸은 호버 시 원본 `GFX_replace_regiment_slot`의 `gfx/interface/replace_regiment_slot.dds`를 표시한다. 팩의 기존 `officer/hq/replace`를 재사용하고, 호버가 끝나면 서버가 보낸 부대 그림으로 돌아온다. 빈 대대 칸의 추가 그림과 배경 프레임은 호버해도 유지한다. 잠긴 칸·응답 대기 중인 칸은 교체 그림을 표시하지 않으며 입력 허용 조건과 서버 요청은 바꾸지 않는다. `division-designer` 집중 검사는 GUI2 두 해상도에서 빈 칸 유지·채워진 칸 교체·호버 해제 복원과 기존 후보 선택 경로를 함께 재현한다.

편제 태그·편제 선택 목록은 대대 위젯 뒤의 새 렌더 계층에서 배경과 행을 함께 그린다. 원본 `officer/hq/list_background` 중심색 `(16,16,18)`을 불투명 밑바탕으로 쓰고 원본 테두리·행을 그려 대대 그림이 비치지 않게 한다. 목록 좌표·행 간격·원본 스크롤바 크기는 유지한다. 호버 그림 비교에서는 툴팁이 슬롯의 검사 픽셀을 덮지 않도록 커서를 칸 아래쪽에 둔다.

해군 상세 머리줄과 태스크포스 행의 휘장 선택 입력을 제거한다. 육군 상세 휘장은 TFR `unitview.gui`의 오른쪽 기준 위치를 사용하며 클릭 영역도 표시 위치를 따른다. HOI 화면·캠페인 HUD에서는 Minecraft `GuiGraphicsExtractor.requestCursor`가 요청한 `CursorTypes.NOT_ALLOWED`만 화살표로 표시한다. 실제 단추의 비활성·권한·대기 상태와 요청 차단은 유지한다. 대상은 공용 단추를 쓰는 연구·건설·산업/사단 설계·장교/HQ·교리·첩보 및 기타 HOI 화면의 비활성 조작부다. 바닐라 화면에는 적용하지 않는다. 해군·육군 상세는 `navy-layout`·`army-layout`, HQ는 `headquarters`, 비활성 커서는 `division-designer`로 재현한다.

금지 커서 요청의 소스상 대상은 다음과 같다. 이는 모든 장면의 직접 조작 검증을 뜻하지 않으며, 실제 커서 상태 검사는 사단 설계의 비활성 단추에서 수행한다.

| 화면 | 비활성 조작부 |
| --- | --- |
| 연구 상세 | 조건 미충족·완료·응답 대기 중 연구 단추, 응답 대기 중 업체 선택 |
| 건설 | 서버가 막은 건물, 목록 양 끝의 순서 이동, 수리 우선 공장 감소 한계 |
| 사단·장비 설계 | 잠긴 대대·지원 칸, 저장 불가, 응답 대기 중 조작부 |
| HQ 설계 | `Slot.reason`·`Choice.reason`이 있는 후보, 저장 불가, 응답 대기 |
| 교리 | 서버가 막은 후보, 선택창 뒤의 통달 표시, 응답 대기 |
| 정보기관 | 모집 불가, 비활성 사업·암호학 조작부, 표시 전용 요원 수·첩보장·개선 진행 칸 |

이 화면 변경의 집중 검사는 기존 검증된 `cmp83/naval-final/server-source`와 같은 묶음의 `commander-input`을 사용한다. 새 HQ 추가 거절·사단 복제 이름 규칙의 서버 적용, 교리 후보 미리보기 계약과 수도 암호학 좌표 연동, 해당 입력을 포함한 무필터 전체 검증은 별도 인계 후 확인한다. 운영 서버 접속·저장/재접속·멀티플레이·효과음 직접 청취는 이 렌더 검사에 포함하지 않는다.

HQ 대대 선택창의 닫는 단추는 사용자 지정 문구 `확인`과 원본 `GFX_button_123x34`를 사용한다. 원본 `REGIMENTS_SELECTION_CANCEL`의 한국어는 `뒤로`지만 최신 문구 지시를 우선한다. 확인과 Escape는 후보창만 닫고 서버 변경 요청을 보내지 않는다. 장비 비용 열은 서버가 제공한 실제 필요 장비의 이름을 왼쪽, 수량을 오른쪽에 검은색으로 표시한다. 요구량0과 별도 기본 지휘력 줄은 표시하지 않는다. 실제 요구 목록에 있는 보병장비는 필요 장비로 유지한다. 원본·수정 전·수정 후 비교는 GameTest 렌더이며 운영 서버 접속·다중 플레이 검증은 포함하지 않는다.

사단 공수 상태는 서버 `Designer.paradrop=false`일 때 원본 `GFX_can_paradrop_icon_small`의 0번 프레임(빨간 X), `true`일 때 1번 프레임(체크)을 표시한다. 장비 비용 열에서 아이콘이 없는 행의 이름은 아이콘 왼쪽 끝에 맞추고, 아이콘이 있는 행만 그림 너비를 띄운다. 연구 상세 단추는 상태와 관계없이 “연구”로 표시하며 상태 원문은 툴팁 첫 줄에 둔다. 비활성 회색·서버 요청 조건은 유지한다. 긴 능력치 이름은 글꼴 크기와 값 영역을 보존한 채 가로 스크롤하며, 이는 원본 대비 차이로 남는다.

`screen-polish`에는 공수 가능·불가 합성 응답의 실제 프레임 색 검사와 연구 단추 6상태(가능·선행 필요·완료·진행·빈 슬롯 없음·응답 대기)의 GUI2 2560×1440·1280×720·854×480 너비·경계·비활성 입력·넘친 상태 툴팁 검사가 포함된다. 합성 상태는 서버 게임 규칙 검증과 구분한다. 이 검토 보정의 컴파일·렌더와 무필터 전체 검증은 서버 full6 종료·고정 입력 교체 인계 뒤 수행하며, 그 전의 집중 통과 결과를 보정 후 결과로 세지 않는다.

스크롤 버튼 크기를 임의로 바꾸지 않는다. ▲▼ 버튼과 끄는 손잡이는 원본 `scroll_up`·`scroll_down`·`scroll_drager`의 16×16px(GUI2에서 8×8GUI)을 유지하며, 내용량이나 표시 영역 높이에 비례해 늘리거나 줄이지 않는다. 이 규칙은 생산·건설뿐 아니라 장교·결정·중점 등 모든 화면에 적용한다. 트랙 길이와 내용 이동 범위는 버튼 크기와 별개다. 내용이 표시 영역 밖으로 나갈 때만 스크롤을 표시한다. 원본 버튼의 크기를 지키라는 사용자 정정이며, 트랙 길이를 고정하라는 뜻이 아니다.

건설 대기열과 건물 선택 목록은 독립적으로 넘침을 계산한다. 빈 목록·한 행에는 스크롤을 표시하지 않고, 부분 행을 포함해 넘치면 휠·▲▼·트랙·손잡이 끌기로 끝까지 이동한다. `production-scrollbars` 집중 검사는 생산 8개 화면과 건설의 0·1·24행을 두 해상도에서 검사한다. 장교·결정·중점 손잡이 회귀는 각각 `source-scrollbars`·`decisions`·`focus-detail`로 재현한다.

생산 라인과 생산 추가의 보병/포병·기갑·항공기·함선/선체 목록, 수리 대기열, 항구 설정은 기존 공통 `UiScrollBar`와 외부 팩의 원본 `GFX_scroll_*`를 사용한다. 목록이 넘칠 때만 막대·▲▼·손잡이를 표시하고, 휠·화살표·트랙 클릭·끌기는 같은 최대 이동량으로 제한한다. 생산 머리 영역은 고정하고 생산 목록과 옆 선택창의 이동량을 분리한다. 선택창이 닫히거나 바뀌면 이전 막대의 입력 영역을 지운다. 오른쪽 막대 폭은 행의 조작 영역과 겹치지 않도록 비워 둔다. 목록 수에 따라 창이나 스크롤 영역을 늘리지 않는다.

좌표 근거는 TFR `interface/countryproductionlineview.gui`의 `production_lines`, `production_equipment_window_military/equipments`(y85, 위13/아래12px), 해군 `equipments`(y112, 위/아래14px), `navy_repair_window`(444×600px, 목록 y90/높이505px), `production_deployment_window`(375px/화면70%, 목록 y43, 위14/아래12px) 및 바닐라 `core.gui/right_vertical_slider`(우측 -5px, 폭18px)다. GUI2에서는 원본 픽셀을 절반으로 환산하고 화면 경계를 적용한다. 생산 라인은 사진 `20261024072637_1`과 대조한다. 지정된 `20261009214916_1`~`20261009215104_1` 47장은 외교/무기대여 장면이므로 생산 상세의 사진 근거로 사용하지 않는다. 장비/선체·수리 대기열·항구 설정의 사진 일치는 미확인이고 GUI 선언 대조와 구분한다.

팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=production-scrollbars -PhoiServerSource=<검증된 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java --console=plain --no-daemon`으로 재현한다. 기존 서버 장비를 반복한 명시적 합성 넘침 입력과 한 행 입력으로 8개 화면을 GUI2 2560×1440·1280×720에서 렌더한다. 휠·▲▼·끌기·마지막 행 도달, 목록 간 스크롤 분리 및 게임 상태 변경 요청 부재를 검사한다. 이는 실제 서버의 생산·수리·배치 효과나 다중 접속 검증이 아니다.

목록의 공통 `UiScrollBar`는 원본 `scrollbar_vertical_bg`, `scroll_track`, `scroll_drager`, `scroll_up`, `scroll_down`을 외부 팩에서 읽는다. 휠·화살표·트랙 누르기·끌기는 같은 최대 이동량을 사용한다. 장교·지휘관, 장교단 역할, 선호 전술, 편제 목록과 변경/해산 확인, 업체 목록·특성/혜택·대기열·정책 및 연구/생산 업체 선택창에 적용한다. `source-scrollbars`는 해당 화면들의 GUI2 두 해상도 렌더와 기존 권한·반복 입력 검사를 실행하며 선호 전술 producer도 준비한다. 원본 사진은 2560 기준이고 1280은 화면 경계에 맞춰 위치를 보정한다.

연구 상세 일수 칸은 `GFX_techtree_details_progress_bar_frame`, 단축 일수 표시는 `GFX_increase_reversed_texticon`을 사용한다. 별도 군수산업체 버튼을 두지 않고 연구 클릭은 선택창, Shift+클릭은 업체 없는 START로 이어진다. 진행 중인 연구의 일수 칸은 업체 변경을 연다. 연구 선택창은 268×325GUI, 생산 선택창은 268×326GUI이며 로고는 원본 투명 여백을 제외한 내용 비율을 보존한다. 빈 업체 선택은 원문 색이 있는 큰 행으로 표시한다. START가 확인된 새 revision 뒤에만 DESIGN_TEAM을 보내는 기존 순서를 유지한다. 배정·해제·반복 클릭·거절·동일 revision은 `organization-assignment`로 검사한다.

사단 설계 창은 TFR `interface/divisiondesignerview.gui/countrydivisiondesignerview`와 원본 사진 `20261027084504_1.jpg`~`20261027084742_1.jpg` 23장을 기준으로 구성한다. 1200×720 원본 창을 GUI2에서 600×360GUI로 표시하며 HQ와 같은 `GFX_division_designer_popup_bg`를 사용한다. 원본 배경에 포함된 세로 구분선·지원 틀·능력치 회색 줄을 유지한다. 오른쪽 위 장비 단추와 탭은 표시하지 않는다. 제목은 굵게, 사단 구성과 능력치 제목은 왼쪽 정렬한다. 능력치 값은 `equipment_design_details.json/divisionStats`의 원본 `_VALUE` 정밀도와 필드 단위를 사용한다. 예를 들어 속도는 `10.0km/h`, 회복률은 `0.81`, 평균 신뢰도는 `90.0%`, 전선 복귀는 `0.00%`다. 기갑 비율은 요약 칩에만 두며 전투 능력치 열에 중복하지 않는다. 세 열의 보이는 스크롤바는 없고, 넘친 장비 목록은 해당 열 위의 휠로 이동한다. 긴 이름은 값을 가리지 않게 자르고 전체 문구를 호버로 제공한다.

대대의 원본 격자 x50/y279에 칸 배경의 x-12/y11을 더한 위치는 GUI2 x19/y145이며 열 간격51·행 간격23GUI, 클릭 영역44×21GUI다. 지원중대는 격자 x552/y300과 배경 y21을 합쳐 x276/y160GUI에 32×16GUI로 둔다. 기존 48·41GUI 클릭 영역에서 생겼던 1GUI 겹침은 원본 프레임 크기로 교정한다. 연대 지원은 x27에서51GUI 간격, y267, 제목 y285다. 서버가 제공한 허용 행 수·열 순서·연대 지원 조건과 원문 잠금 사유를 표시하며, 클라이언트가 잠금 해제를 결정하지 않는다. 연대 지원의 실제 배정 기능은 별도 서버 계약 전까지 비활성이다. 저장 단추는 창 하단27GUI 위의18GUI 높이로 최소 창에서도 닿는다. 작은 창에서는 대대·지원·연대 지원과 제목이 함께 스크롤하며 능력치 세 열과 지형 조정치를 그 아래로 이어 배치해 같은 휠로 끝까지 접근한다. 생산비는 작은 창에서 제목과 값 두 줄로 표시한다. 600GUI 이상 너비에서는 능력치 열 위의 휠이 해당 열만 움직인다. 600×360GUI 창에서 편성 영역의 최대 스크롤은0이다. `division-designer`는2560·1280 GUI2 비겹침·경계 클릭·두 목록·우선순위·초기화·복제·저장 요청과 툴팁,854×480의 모든 위젯 경계 및 전체 지형 접근을 검사한다. `industry`는854×480의 마지막 대대·지원 칸 클릭과 연대 지원 도달,2560·1280의 휠 전후 좌표 불변을 포함한다.

3D 모델 자리는 조정치 영역에 합친다. 서버 `draft.adjustments`의 순서를 유지해 원본77×72px 간격을 GUI2의39×36GUI로 옮기고6열로 배치한다. 클라이언트가0값 지형을 임의로 제거하지 않는다. 지형 원과 이동·공격·방어 그림·원문 툴팁은 HQ 공통 `IndustryTerrainAdjustment`를 사용한다. 저장 편제의 빈 조정치와 새 초안의0값은 구분한다.

편제 목록 맨 위는 `DESIGNER_CREATE_NEW_DIVISION`, 이름 그룹의 예시·대체 이름은 `DIVISION_NAMES_GROUP_LIST_*` 키를 사용한다. 이름 그룹의 빈 선택은 사진의 “없음”과 일치하는 원본 `MODULE_NONE_FIXED`를 재사용한다. GUI에는 동적 라벨만 있어 사단 그룹 전용 빈 선택 키는 확인되지 않았으며, 이 키는 본래 모듈 선택 문구라는 차이가 있다. 목록은 바깥 클릭·Escape로 닫히고 우클릭은 선택을 전송하지 않는다. 이름 그룹·우선순위·복제는 서버 초안 요청이며 저장 결과를 클라이언트에서 미리 확정하지 않는다. `SAVE_TEMPLATE`의 덮어쓰기/새 편제 생성 구분도 서버의 baseline/copy 상태를 따른다. 이름을 편집하던 커서·선택은 일반 새로고침에서 유지한다.

아래 줄의 자원 그림은 서버 목록 순서, 산업력은 서버 `costMinimum`/`costMaximum`을 쓴다. 비용 라벨·산업력 아이콘·숫자 묶음은 실제 장식 글 폭을 측정해 창 오른쪽 안쪽 15GUI 여백에 맞춘다. 자원 아이콘의 기존 좌표는 유지하며 작은 창의 두 줄도 각각 오른쪽 정렬한다. 두 수는 자원량과 산업력을 나눈 값이 아니라 장비 생산 비용의 최소·최대다. `DIVISION_PRODUCTION_COST_VAL`·`DIVISION_PRODUCTION_COST_SINGLE_VAL`의 문구·자릿수와 자원/산업력 설명을 보존한다. 없는 계약은 추측한 비용 대신 미상으로 표시한다. 사용 가능 경험치는 `hud.armyExperience`, 저장 비용은 `Template.experienceCost`다.

새 계약의 집중 검사는 최종 protocol48개 바이트 일치 상태에서 `test runGameTest remapJar -PhoiTestSuite=division-designer` 및 `-PhoiTestSuite=industry`로 수행한다. 서버가 제공한 `kor-division-designer.json`, `kor-division-designer-new.json`, `prc-division-designer.json` 폴더를 `HOI_COMMANDER_PREVIEW`에 지정한다. 원본23장 기준의 배치, 두 목록·이름 예시·잠금/비용 툴팁 및854×480 접근성을 실제 Minecraft에서 검사했다. 서버 응답 갱신을 모사한 요청 검증은 실제 서버의 덮어쓰기·복제·장비 반환·저장/재접속·다중 접속 검증을 대신하지 않는다. 원본 사진과 입력의 캠페인 상태가 달라 능력치와 비용·경험치는 같지 않다.

원본23장 재구성 전의 무필터 결과는 새 화면 검증을 대신하지 않는다. 변경 후 필터 없는 전체 검사는 서버 전체 검증과 전체 입력 교체 후 수행해야 하며, 직접 음향 청취·운영 접속·다중 플레이 검증도 별도다. 역할·편제 아이콘 선택과 그 상태 툴팁은 이번 범위에서 제외한다. 원본 그림만 표시하고 비활성 사유를 추정하지 않는다. 원본084541·084543의 기갑 전력 없음 문구는 `DESIGNER_SELECT_NICHE_DISABLED`와 `_DELAYED`에 해당한다. 이름 그룹·우선순위·초기화·복제·저장은 서버 초안 계약을 따르고 응답 전에는 상태를 확정하지 않는다. 새 초안·복제 저장은 새 편제이며 기존 편제의 일반 저장은 서버가 해당 편제를 갱신한다.

`source-layout`은 연구/생산 배정, 장교/함장, 사단 설계 집중 검사를 묶는다. `mio-types`는 서버 `mio-types.json`의 30유형을 두 해상도로 그린다. 24개 실제 소유 유형의 목록 입력과 30개 세부/특성·대기열·정책 입력을 사용한다. 나머지 6유형은 서버가 KOR로 발급한 미소유 표시 입력이고 목록 파일이 없으므로 목록 렌더를 주장하지 않는다. 이는 실제 국가의 업체 소유나 정책 효과·비용·저장 검증을 대신하지 않는다. 전체 검사에는 이 두 신규 범위도 포함되며, 실행은 담당자의 별도 지시 뒤 한 번 수행한다.

군수산업체 상세 창의 긴 유형명은 기본 글자 크기로 줄바꿈한다. 유형명 높이는 장비 혜택의 시작 위치와 휠·화살표·막대 스크롤 범위에 함께 반영한다. `mio-long-type`은 원본 `20261024073006_1.jpg`의 두 줄 유형명에 해당하는 `mio-type-20`을 두 해상도로 집중 검사한다. 24유형의 원본 대조는 회사가 다른 같은 유형과 같은 회사의 고유 특성을 구분한다. 특히 SNT모티브와 병기국 205창은 같은 보병 무기 제조사여도 특성 개수·장비 혜택이 같지 않으므로 사진에 맞춰 서버 값을 바꾸지 않는다. 선행·택일·상호 배타 연결은 서버 `traitNode` 계약을 따른다.

업체 이름은 상세 창 왼쪽 위에 기본 Minecraft 글꼴의 굵은 흰 글씨로 표시한다. 상세 창은 특성 나무만 표시하며 특성·이력 탭과 이력 화면 분기를 두지 않는다. 대기열 열기는 `GFX_button_221x34`, 닫기와 항목 제거는 `menu/close`의 TFR 원본 흰 닫기 버튼을 사용한다. 대기열은 원본 `industrial_org_policy.gui`의 495×720px 창과 53px 행을 GUI2 좌표로 반영하며 작은 화면에서는 높이를 화면 안으로 제한한다. 제목은 업체명과 같은 1.25배 굵은 흰 글씨다. 행 제거 버튼은 원본 `remove_from_queue_button`의 x420/y11 및 30px 그림을 GUI2로 환산한다. 창 오른쪽 위 닫기도 원본 `close_button`의 오른쪽 기준 x−36/y4와 30px 그림을 따른다. 공통 닫기 그림의 2GUI 안쪽 여백을 포함한 클릭 영역은 19×19GUI이고 실제 그림은 15×15GUI다. 행의 아이콘·이름·제거 버튼, 스크롤 범위와 끌어놓기 대상 계산은 같은 행 간격을 따른다. 첫 순번은 원본 초록 원, 이후 순번은 보드가 요청한 양쪽 짧은 초록 막대다. 별도 막대 에셋은 확인되지 않아 두 선으로 그린다. 원본 072358·072402·072506 사진과 함께 `organizations` 입력 회귀 및 `mio-types` 30유형 렌더로 확인한다. 이 검사는 실제 서버의 특성·정책 효과 검증과 구분한다.

군수산업체 세부 창의 왼쪽 로고·점수 판은 `GFX_tiled_plain_bg`, 아래 속성·장비 혜택 판은 `GFX_tiled_window2_1b_border`, 오른쪽 특성 나무는 `GFX_MIO_details_background`를 사용한다. 바닐라·TFR `interface/military_industrial_organization/industrial_organization_detail.gui`의 `aggregated_bonuses_window`는 x5/y150, 310×560px이며 `core.gfx`는 원본 테두리 64px와 `tilingCenter=yes`를 지정한다. 아래 판은 GUI2에서 x3/y75, 폭155GUI로 배치하고 작은 화면에서 높이만 창 안으로 제한한다. 팩의 두 배경 PNG는 TFR DDS 원본 픽셀을 보존하며 새 그림을 만들지 않는다. 오른쪽 그림은 기존 연결을 유지한다.

| 원본 사진의 유형 | 대조 사진 `20261024…_1.jpg` | 왼쪽 아래 / 오른쪽 배경 |
| --- | --- | --- |
| 보병 무기 · SNT모티브 | `072402`, `072414` | `GFX_tiled_window2_1b_border` / `GFX_MIO_details_background` |
| 포 · LIG | `072441` | 동일 |
| 자동차 · 기아 | `072449` | 동일 |
| 기갑 · 현대로템, LS엠트론 | `072557`, `072559` | 동일 |
| 공군 · 한화에어로스페이스, 대한항공, KAI | `072454`, `072506`, `072512` | 동일 |
| 해군 · HD현대중공업, SK오션플랜트 | `072520`, `072541` | 동일 |

특성의 선행·배타 조건과 예약 순서는 서버가 발급한 선택지를 따른다. Shift+클릭은 현재 응답에 `queue/<token>` 또는 `unqueue/<token>`이 있을 때만 전송하며, 끌기 이동은 응답마다 해당 `up/<token>`·`down/<token>`을 다시 검사한다. 없는 선택지를 클라이언트에서 만들어 전송하거나 선행 특성을 자동 예약하지 않는다. 반복 입력·동일 revision·다른 session은 기존 pending 잠금을 유지하고, 거절 응답의 메시지와 툴팁은 서버 문구를 표시한다. `organizations` 집중 검사는 이 입력 경로를, `mio-types`는 실제 소유 업체가 있는 24유형의 세부·대기열·정책을 GUI2 두 해상도로 렌더한다. 선택지를 제거한 합성 입력은 반복 Shift+클릭과 위·아래 드래그 차단, 새 리비전에서 허용된 예약 복구를 검사하며 캡처에 `synthetic`을 붙인다. 이 합성 검사는 실제 서버의 선행 조건 판정이나 거절 메시지 수신을 증명하지 않는다.

생산 라인의 군수공장·조선소 배정 칸은 배정 수·정상·손상·예약을 설명하는 클라이언트 생성 툴팁을 표시하지 않는다. 배정 클릭, 상태 그림, 미리보기와 서버 승인 경로는 유지한다. 사단 설계 요약 칩은 원본 틀과 값 형식을 사용하며, 연대 지원은 별도 원본32px 높이의 프레임을 사용한다.

펼친 생산 라인 배경(`production/line/land`·`naval`·`naval_N`)은 원본 511×108px을 배율×108 높이 칸에 그린다. 행 높이 칸에 비율 맞춤으로 그리면 배경만 세로 가운데로 내려가 조선소 배정 칸·아이콘·n/n이 위로 올라가 보였다. 생산 진행·효율·건설 진행 막대는 원본 `progress_startend`를 따른다. `textureFile2` 배경 위에 `textureFile1`의 가운데 3분의 1을 진행 폭만큼 늘리고, 위 3분의 1은 왼쪽에서, 아래 3분의 1은 오른쪽 끝을 진행 끝에 맞춰 겹친다. 생산 진행 `GFX_prod_progress_bar2`는 펼친 육군·공군 x157/y26, 해군 x158/y31, 접힌 행 x157/y34의 118×21px이다. 효율은 93×9px에 `GFX_efficiency_progressbar_to_max`(서버 조정치 `cap`)와 `GFX_efficiency_progressbar`(라인 효율)를 차례로 그리고 x248/y7 테두리와 아이콘을 덮는다. 원본 GUI 위치는 x252/y8이다. 원본 실행 화면(사진 `20261024072637_1`)은 진행 막대를 GUI 위치보다 1px 아래에 그려 테두리 안쪽 맨 아래 1행이 비고, y8 그대로 그리면 막대 1행이 테두리 위로 나오고 안쪽 아래 2행이 빈다. 그래서 막대를 테두리 안쪽 9행에 맞춰 x252/y10에 그린다. 건설 `GFX_construction_progressbar`의 원본 위치·크기는 x249/y36의 60×8px이지만, 402×54px 행 그림의 칸은 모서리가 둥근 x249~310/y36~44(62×9)다. 남는 칸 픽셀이 보이지 않도록 60×9 원본 텍스처를 칸 전체인 x249/y36의 62×9로 늘린다. 3등분 순서는 원본 `progress_startend.shader`의 위(시작)·가운데·아래(끝) 띠이며, 원본 실행 화면의 효율 막대 행 색과도 일치한다. `queue-drag` 묶음의 `IndustryQueueDragTest`는 GUI2 2560·1280에서 효율 막대가 테두리 위로 나오지 않고 테두리 안쪽 맨 아래 행까지 채우는지, 100% 건설 막대가 칸 전체를 채우는지 실제 렌더 픽셀로 검사한다. 재현은 `industry`·`queue-drag`·`construction` 집중 묶음이다.

### 사본 서버 실제 접속 검사

`network-campaign`은 자동 전체 검사에 포함하지 않는 수동 실행 전용 경로다. 승인된 사본 서버 `127.0.0.1:25591`과 `HoiTestA`→KOR, `HoiTestB`→JAP 배정을 기본으로 사용한다. 인접 국가의 상호 가시성을 검사할 때는 담당자가 승인한 사본 배정과 `HOI_NETWORK_COUNTRY_B=PRK`를 함께 사용한다. 각 프로세스에 `HOI_NETWORK_PLAYER`와 공통 `HOI_NETWORK_DIRECTORY`를 지정한 뒤 `runGameTest -PhoiTestSuite=network-campaign --console=plain --no-daemon`을 실행한다. 별도 실행 폴더와 오프라인 시험 이름을 사용하며 운영 서버에는 연결하지 않는다. 두 프로세스를 동시에 유지할 때도 게임별 렌더 폴더는 `isolated-gametest.gradle`이 분리한다.

접속과 국가 HUD 수신 뒤 `<HOI_NETWORK_DIRECTORY>/<player>/ready.json`을 기록한다. 이후 `001-command.json`부터 순서대로 명령을 읽으며 `operation`은 `menu`(`tab`), `click`(`label`, 선택적 동일 이름의 `index`·`right`·`shift`), `pointer`(`x`·`y`·`right`·`shift`), `cursor`, `scroll`(`x`·`y`·`amount`), `close`, `event`, `snapshot`, `stop`이다. `event`는 완료 알림이 있으면 먼저 확인하고, 원본 이벤트는 첫 선택지를 누른다. 선택 요청의 토큰·원본 선택지 ID·시각을 기록하며 서버 응답 뒤 창이 사라지는지도 확인해야 한다. 파일을 임시 이름으로 쓴 뒤 원자적으로 이름을 바꿔 전달한다. 화면 버튼의 실제 마우스 입력과 `NetworkScreens`의 정상 요청 경로를 사용하고 응답·HUD·위젯·지도 부대/지휘관 스냅샷·카메라 위치·SoundManager의 HOI 재생 이벤트 및 UTC 시각을 대응 `NNN-result.json` 및 스크린샷에 남긴다. `stop`으로 접속을 종료해야 완료된다. 개별 명령 오류는 결과에 기록되므로 프로세스 종료 코드만으로 비용·저장·격리 검증의 성공을 판단하지 않는다.

사본 서버의 필수 팩 핸드셰이크도 실제 접속 조건이다. 외부 팩 주소의 HTTP 502 실패 뒤 사본 주소를 `http://127.0.0.1:25591`로 고쳐 실제 접속을 검증했다. 생산 업체 배정·연구, 참모총장 임명, 장군 진급·고문 역할, 업체 특성·정책의 실제 응답과 저장·재시작 후 상태를 확인했다. 진급 비용은 특성별 할인까지 포함한 확인창 값과 비교한다. 업체 특성·정책 검사는 담당자가 제공한 사본 준비 스크립트로 SNT모티브 크기 6·점수 2를 설정한 조건이다. 대기열 자동 해금과 수동 해금은 각각 1점을 사용했고 정책은 정치력 25를 사용했다. 크기·점수의 자연 획득 검증과 구분한다.

한국·일본은 평시 인접 육군이 없어 상호 가시성 입력으로 부적합하다. 한국·북한의 휴전선 이동으로 위치 변화와 가린 카운터의 출현을 확인한다. 타국 카운터는 병력 수·이동 상태 등이 숨겨지므로 자신의 전체 정보와 같다고 가정하지 않는다. 한국 항구는 제주까지 육로가 이어져 일반 이동이 되므로 일본의 섬 11932→항구 1182로 해상 수송을 검증했다. 실제 SoundManager의 `hoi:ui.order.convoy_transport` 이벤트, 같은 표시 엔티티의 `hoi:army/transport`→`hoi:army/motorized_infantry` 전환과 저장상 도착을 확인했다. 카메라 이동은 관측용이며 부대 위치는 지도 입력과 서버 시간 진행으로만 바꾼다. 운영 접속과 직접 음향 청취는 별도 검증이다.

편제 변경·해산은 화면 고정 입력의 `view`뿐 아니라 실제 요청의 `offer` 라우팅을 검사한다. 최초 사본 접속에서는 `ArmyCommandOrders.Action.DIVISIONS_TEMPLATE`·`DIVISIONS_DISBAND` enum 오류로 확인창이 열리지 않았으나, 서버 라우팅 순서 수정 뒤 한국 군 카드의 편제 요약에서 1개·2개를 선택해 실제 확인·저장까지 재검증했다. 편제 한 개가 변경됐고 두 사단의 해산으로 인력 13,500과 확인창에 적힌 장비가 비축량에 반환됐다. 편제 변경 확인창의 추가 필요량과 즉시 차감·보충은 구분한다. 일시정지 중에는 상단 인력 표시가 확인 직후 응답보다 늦게 갱신될 수 있으므로 저장과 후속 HUD를 함께 대조한다. 화면 고정 입력 검증의 통과가 이 서버 요청 경로의 성공을 뜻하지 않는다. 접속 도구의 개별 오류와 재시도도 결과 JSON에 보존하며, 준비 조건과 수행한 조작별 판정은 작업 첨부 증거에 기록한다.

생산 상세 창은 `Equipment.manufacturerChoices>0`인 장비에 `OFFER(item)`을 요청하고 응답의 `manufacturers`로 장비 옆 선택창을 연다. 업체 선택은 `ADD(item,업체id)`, 업체 없이 생산은 `ADD(item,"")`, Esc·바깥 클릭은 `OFFER("")`를 보낸다. 후보가 0이면 기존 ADD를 사용하며 기존 라인의 SWITCH는 유지한다. 생산 라인에는 배정된 업체를 표시하고 배정 후보가 없으면 원본 `GFX_attach_manufacturer`의 불가 프레임과 `Line.manufacturerTooltip`을 표시한다. 새 OFFER 장면은 지정 원본 사진이 없어 원본 GUI와 기존 업체 선택 틀을 기준으로 검증한다.

연구 상세의 일수 칸은 원본 네모 배경과 `GFX_techtree_details_progress_bar_frame`의 223×30px 비율을 사용한다. GUI2에서 프레임은 창 기준 (79, 30)의 112×15GUI이며, 연구 단추는 (179, 29)의 82×17GUI다. 원본 `government_button.dds`의 172×35px 중 왼쪽 22px는 투명하므로 보이는 두 테두리의 간격은 약 2GUI다. 단추에는 상태와 무관하게 흰색 "연구"와 MIO 아이콘만 표시한다. 연구·선행 연구 필요·연구 완료·연구 중단·빈 슬롯 필요·응답 대기는 툴팁 첫 줄에 두며, 시작할 수 있는 상태에서만 클릭하여 연구 안내를 붙인다. 잠금·완료·빈 슬롯 없음·응답 대기의 비활성 상태와 연구 중단 요청은 유지한다. 긴 설명은 `UiScrollBar`의 원본 화살표와 16×16px 손잡이(GUI2 8×8GUI)로 스크롤하며 휠·화살표·드래그로 마지막 줄까지 접근한다. 사진 075226과의 비교는 같은 UI 요소의 비교이며 기술·일수·설명 값이 같은 장면은 아니다.

사단 설계와 생산·연구 상세는 `test runGameTest remapJar -PhoiTestSuite=division-designer`와 `-PhoiTestSuite=production-offer`, 기존 업체 배정은 `-PhoiTestSuite=organization-assignment`로 집중 재현한다. `HOI_COMMANDER_PREVIEW`에는 검증된 서버의 사단 설계 4종과 OFFER·배정 전후 입력을 둔다. 사단 공수는 false일 때 `paradrop_0` 빨간 X, true일 때 `paradrop_1` 초록 체크를 표시하며 합성 true/false 입력의 실제 렌더 색을 검사한다. 아이콘 없는 장비 비용 라벨은 아이콘의 왼쪽 끝에서 시작한다. 긴 능력치 이름의 가로 스크롤은 기존 글꼴 크기를 유지한 결과이며 원본과 남은 차이다.

사단 설계 그림의 우선순위 꺾쇠는 원본 x105/y56과 16×35px 프레임을 GUI2의 소수 좌표까지 보존한다. 왼쪽 편제 목록에는 서버 Template.priority로 별도 16×52px GFX_div_deploy_priority_icon_strip을 그린다. 예비·정예의 서로 다른 투명 여백을 유지하며 기본 프레임은 비어 있다. 사단 설계의 우선순위 셋은 원본 배경의 세 원 중심에 배치하고 선택 표시가 합쳐진 `GFX_div_priority_strip` 프레임을 사용한다. 원본 36×37px 프레임은 18×18.5GUI로 그리며, 선택된 3~5번도 각각 미선택 0~2번의 원 영역을 정렬 기준으로 쓴다. 선택 테두리의 투명 여백 때문에 원 중심이 이동하지 않게 하고, 클릭 영역은 가장 가까운 정수 GUI의 18×19칸으로 맞춘다. 기갑이 없으면 `GFX_role_icon_bg`의 두 번째 회색 프레임과 `GFX_red_cross_small` 원본 빨간 X를 쓴다. 두 이름 목록의 ▼는 클릭 영역 11×11GUI 안에 원본 폭 19px·높이 22px로 그리며, 첫 줄 (180, 30)·둘째 줄 (180, 59.5)에 두고 이름 배경의 폭은 110GUI다. 긴 이름 그룹은 글꼴·크기를 유지한 채 가로 스크롤하고 전체 이름은 툴팁에 둔다. 능력치 세 열은 배경의 20px 행 간격에 아이콘·검정 글자·값을 맞추며 글꼴과 크기는 유지한다. 대대 아이콘은 슬롯 그림 왼쪽 연결선을 제외한 사각 배경 중심, 지원 아이콘은 지원 칸의 사각 배경 중심에 놓는다. 지형 조정치의 70×62px 원본 프레임과 아래 세 원의 중심에 이동·공격·방어 아이콘을 맞추고 5·5·1 각 줄을 합친 배경의 가운데에 배치한다. 이 조정치 그리기는 HQ와 공유한다.

854×480에서는 생산 상세의 항목을 스크롤하여 마지막 장비와 업체 선택에 접근한다. 화면 하단의 버튼을 가리지 않도록 장비 영역에 64GUI 여유를 둔다. 2560·1280 GUI2의 배치를 함께 검사하며 `production-offer`는 업체 선택·업체 없이 생산·닫기·반복 입력 차단과 기존 ADD·SWITCH 경로를 검사한다. 원본의 생산 상세 배치는 `countryproductionlineview.gui`를 근거로 하며 실제 서버 비용·저장·다중 사용자 동기화 검증과 구분한다.

`-PhoiTestSuite=screen-polish`는 사단 설계 4종과 생산 OFFER·배정·불가·연구 상세를 묶는다. 2560×1440·1280×720 GUI2와 854×480에서 화면 경계·스크롤·연구 단추 여섯 상태의 글자 폭·비활성·상태 툴팁을 검사한다. 전체 검증은 검증된 서버 사본에 `hoiServerSource`·`hoiProtocolSource`를 지정하고 `HOI_COMMANDER_PREVIEW`, `HOI_MISSION_OVERLAY_FIXTURES`, `HOI_OFFICER_PREVIEW`, `HOI_LAND_PLAN_PREVIEW`를 같은 고정 입력으로 설정한 뒤 `-PhoiTestSuite` 없이 실행한다. `HOI_LAND_PLAN_PREVIEW`는 폴더가 아니라 `land-plan/land-plan-overlay.json` 파일이며, 같은 폴더에 `land-plan-preview.json`이 있어야 한다. `research-slots` 집중 검사는 작은 화면의 5·6개 슬롯과 슬롯 툴팁·시작·중단 요청·완료 슬롯 상세를 함께 검사한다. 상태와 관계없이 단추의 표시 이름은 “연구”이며 상태 문구는 툴팁에서 확인한다. 실제 운영 접속·멀티플레이·직접 음향 청취는 이 렌더 검사에 포함하지 않는다.

구현만 검증하는 경우 `HOI_COMMANDER_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp50-server/fixtures`의 고정 입력과 검증된 `HOI-resourcepack/build`를 사용해 `./gradlew.bat test runGameTest -PhoiTestSuite=screen-polish --console=plain --no-daemon`을 실행한다. 이 경로는 서버 입력을 다시 생성하지 않는다. 각 조정치 줄의 중심, 2560의 모든 대대·지원 칸이 854×480에서도 스크롤로 접근되는지, 여섯 연구 상태의 흰색 글자 픽셀과 툴팁 첫 줄을 검사한다. 우선순위만 예비·정예로 바꾼 목록/설계 창과 기갑 없음/있음 변형은 synthetic 캡처로 구분한다. 공유 조정치 렌더러를 사용하는 HQ의 별도 화면 검사, 필터 없는 전체 검증, 배포 JAR·설치 및 사본 서버 접속은 이 집중 범위에 포함하지 않는다.

HQ 요약 네 칸은 `Details.stats`의 HARDNESS·ORGANIZATION·HP·SPEED를 원본 divisiondesignerview.gui의 x86·216·326·436/y192 칸에 표시한다. 하단 경험치는 `CountryHud.armyExperience`이며 미제공은 —로 남긴다. 능력치 값은 사단 편제의 `IndustryEquipmentDesigns.divisionStatValue`를 재사용해 필드별 원본 정밀도·단위를 유지하고 기갑 비율은 요약에만 표시한다. 오른쪽 3D 모델 영역은 사단 편제의 조정치 배경·`IndustryTerrainAdjustment`로 통합하며 5·5·1 각 줄의 중심을 맞춘다. 후보별 경험치 비용·저장 비용은 미지원이며 서버 `saveReason` 차단을 유지한다. 기존 권위 입력의 `headquarters` 집중 검사는 2분37초에 통과했다. 두 해상도 요약4칸·전체능력치/조정치 호버·5/5/1 행 중심·저장 차단·후보 선택·툴팁 스크롤을 검사했다. 새 HQ 비용/기간 프로토콜과 무필터 전체·운영 접속 검증은 포함하지 않는다.

HQ 대대·지원 칸은 사단 설계의 원본 slot_atlas/support_slot과 add/locked 그림을 사용하며 접근성 이름과 기존 HQ_SLOT 요청을 유지한다. 후보 창은 `designerregimentswindow` 원본 660×580 영역을 왼쪽에 배치하고 256×69 간격의 두 열로 표시한다. 오른쪽 능력치와 지형 호버는 그대로 유지하며 후보 비용은 서버가 제공하기 전까지 표시하지 않는다. `REGIMENT_WINDOW_DESC`는 기존 리소스팩 가져오기 경로를 사용한다. 후보 안내문은 기존 글꼴에서 두 줄이 될 수 있으므로 첫 후보 행은 안내문 y27/높이60 아래 y94에서 시작한다. 두 해상도의 후보 두 열·오른쪽 능력치 유지·후보 선택/추가/제거 요청·툴팁 스크롤을 headquarters 집중 검사로 확인한다. 후보 창은 사용자 지시에 따라 원본 그림의 ‘확인’ 단추로 닫고 요청을 보내지 않는다. 오른쪽 장비 열은 검정 글자로 권위 있는 필요 장비와 수량을 표시하며 별도 보병장비 합계·지휘력 행을 덧붙이지 않는다.

HQ 하단은 `Template.deployDays`·`baseCommandPower`·`costMinimum`·`costMaximum`·`resources`를 표시한다. `divisiondesignerview.gui`의 `deploy_time_container`(x380/y-97), `deploy_cost_container`(x480/y-97), `production_resources`(x548/y-92)를 GUI2로 옮기고 생산비 전체 묶음은 오른쪽 안쪽30px 여백에 맞춘다. 배치 설명은 서버 labels의 `HQ_DEPLOY_DAYS`·`HQ_DEPLOY_CP_COST`, 생산비와 자원은 기존 팩의 `DIVISION_PRODUCTION_COST_VAL`·`DIVISION_PRODUCTION_COST_SINGLE_VAL`·`DIVISION_PRODUCTION_COST_DESC`·`DIVISION_PRODUCTION_RESOURCE_DESC` 원문을 사용한다. 생산비는 사단 설계와 같이 소수부를 버리고 자원 목록은 서버 순서를 유지한다. 서버 계약에서 배치와 철수는 같은 인력 공식과 define을 사용한다. labels에 `HQ_DEPLOY_AND_WITHDRAW_DAYS`가 있으면 그 원문을 사용하고, 1일은 `_SINGULAR`를 선택한다. 이전 입력에 해당 키가 없으면 기존 `HQ_DEPLOY_DAYS`만 표시한다.

결정 범주 헤더 호버는 원본 `20261028003512_1.jpg`·`20261028003514_1.jpg`처럼 TFR 노란색(`#FFBD00`) 제목 한 줄만 표시한다. 범주 설명과 그림은 기존 본문에 남긴다. 그림이 있는 툴팁은 `decision_assets.json/textImages`의 설정 너비를 절반 GUI 단위로 환산해 본문 줄바꿈과 배경 폭에 함께 사용하며 화면 안쪽으로 제한한다. 결정 상세의 기간은 서버 `Section.days`를 `MODIFIER` 구획에만 표시하고 순서는 서버 응답을 따른다. `core_l_korean.yml`의 `DECISION_FOR_AMOUNT_DAYS_MODIFIER`와 `DECISION_REMOVE_HEADER`에 해당하는 ‘N일 동안:’·‘제거 시 효과:’를 사용한다. 설치된 TFR 한국어 패치에는 이 두 키의 덮어쓰기가 없다. 원본과 현재 캠페인의 수치는 다를 수 있으며 실제 운영 접속·다중 플레이 검사는 별도다.

HQ 설계 육군 아이콘은 `hq_view/div_templ_symbol_button`의 76×42px를 유지하며 투명 여백을 제외한 내용 중심을 헤더 그림 안쪽 중심에 맞춘다. 대대 줄은 106px 간격, 지원 줄은 82px 간격과 원본 64×32px 칸을 사용한다. 첫 대대와 아래 가운데 대대, 네 지원 칸의 보이는 배경 중심은 composition 그림의 중심을 공유한다. 지원 칸은 원본 subunits_hq/support_grid의 y170+350에 slot 배경 y21을 더한다. 장교단 HQ 띠는 `GFX_officer_corp_army_hq_bg`의 523×85px와 내부 x10/210/230/293 위치를 GUI2로 옮긴다. 서버가 주지 않은 HQ 카운터는 임의로 0으로 채우지 않는다.

HQ 구성·지휘관 혜택 제목은 `hq_header`의 x12/y224, 637×32px `GFX_tiled_header_1` 원본 띠 위에 그린다. 이 띠가 사단 설계 배경의 기존 전장 너비 원을 덮는다. HQ 구성 안의 전장 너비 원에는 `combat_width_view_hq`의 x188/y274에 서버 `Details.stats.COMBAT_WIDTH`를 표시하고, 값이 없으면 —를 사용한다. 두 해상도의 `headquarters`와 `officer-comparison` 집중 렌더로 정렬을 확인하며 실제 운영 접속·다중 사용자 검증과 구분한다.

HQ 특수부대/공수 가능 표시, 우선순위 변경, 열린 초안의 편제 교체, 목록 삭제 및 설계 하단 복제는 원본 `hq_view`·`named_hq_template_entry` 좌표와 기존 사단 설계 에셋을 사용한다. `Template.priority`·`paradrop`·`specialForces`와 두 원문 툴팁을 서버 응답 그대로 표시한다. 우선순위는 `HQ_PRIORITY(amount=0..2)`, 이름 오른쪽 ▼의 편제 교체는 `HQ_EDIT(item=templateId)`, 목록의 삭제는 `HQ_DELETE(item=templateId)`, 하단 복제는 `HQ_COPY(item=templateId)`를 현재 세션·revision으로 보낸다. 요청 대기 중에는 반복 입력을 차단하고, 목록 삭제와 우선순위 표시는 새 서버 응답 뒤 갱신한다. 편제 교체·초기화 응답은 같은 ID여도 서버 이름을 다시 적용한다. 새 미저장 편제의 복제는 저장된 원본 ID가 없으므로 비활성이다. 장비 단추는 사용자 지시로 제외한다. 원본 장교단의 지휘관/장교/제독/사령부 네 진입 단추는 이전 명시적 제거 지시에 따라 빠져 있으며, HQ 목록의 누락과 구분한다. 신규 HQ 계약은 Java25 `test runGameTest remapJar -PhoiTestSuite=headquarters`로 GUI2 2560·1280에서 통과했다. 우선순위 3단계와 반복 요청 차단, 정확한 세션·revision, 편제 교체, 삭제 요청과 오래된 응답 거절을 검사한다. 변경 후 상태는 명시적 모의 응답이며 실제 서버 저장·접속·다중 플레이 및 최종 무필터 전체 검증과 구분한다.

연구 상세는 인접한 부대 업그레이드의 효과·지형 조정치가 같을 때 아이콘을 한 묶음으로 표시하고 공통 효과를 한 번 그린다. 분류(category), 빈 scopeId, 서로 다른 효과, 떨어진 항목은 합치지 않는다. `final-contract`의 `basic_light_shell`·HJ중공업 권위 입력으로 함선6종/효과2줄, 자금250·0.05/일과 해당 기술 연구율, 열기/호버 무요청을 검증한다. 원본 사진의 일수로 서버 값을 덮어쓰지 않는다.

연구 트리의 보라색 플라스크는 `DesignTeam.technologies`에 해당 기술이 있는 업체가 하나 이상일 때만 표시한다. 외부 팩의 원본 `GFX_design_team_icon` 20×20px 비율을 유지하고 기술 그림 왼쪽 아래에 둔다. 호버 목록은 서버 `designTeamText.available_title`(`INDUSTRIAL_ORG_AVAILABLE_TOOLTIP_TITLE`)과 `available_item`(`INDUSTRIAL_ORG_AVAILABLE_TOOLTIP_ITEM`)의 `$NAME$`만 치환한다. 목록 순서·색과 업체 이름을 보존하며 수용량 변형이나 연구 규칙을 추가하지 않는다. 클릭은 기존 기술 상세를 열고 연구 시작·업체 배정은 보내지 않는다. `final-contract`에서 두 해상도의 실제 플라스크 픽셀·목록 원문·지원 업체 없음·클릭 무요청을 검사한다. 원본 `countrytechtreeview.gui`의 `can_assign_design_team_icon`과 사진 `20261028004018_1`은 그림 근거이며, 원본 플라스크 호버 사진은 미제공이다. 원본 엔진의 호버 위치·동작 동일성은 아직 확인하지 못했다.

HOI 화면의 글자 그림자는 공통 텍스트 추출에서 끈다. Minecraft 기본 화면은 기존 렌더를 사용한다. 장교단의 포획 지휘관 문구는 기존 위치에서 아래 1GUI, 왼쪽 포획 수는 원래 위치에서 오른쪽·아래 각 1GUI로 이동한다. HQ·장교단·연구의 GUI2 집중 렌더에서는 새로 판독 불가능해진 글자를 확인하지 못했으며 다른 화면 전체의 그림자 제거 후 판독성은 최종 무필터 렌더 대조 대상이다. 글자색·배경색은 판독성 개선을 이유로 임의 변경하지 않는다.

생산 군수산업체 목록의 정렬 단추는 `industrial_organization_list.gui/sorting_options`의 `GFX_sort_skill`29×29px·`GFX_sort_up_down_button_83x29`83×29px를 기존 팩에서 재사용한다. GUI2에서 규모 아이콘15×15GUI와 이름41×15GUI를 창 기준 x8/x23·y24에 두며 `click_scroll`을 사용한다. 생산·국가 메뉴 목록의 규모·이름 단추도 정렬하지 않으면 프레임 0, 현재 기준이면 오름차순 1·내림차순 2를 그린다. 생산·설계 목록 창은 연구와 같은 `IndustryOrganizationList` 원본 틀(275×334 GUI, 흰 제목, 원본 닫기, y42 목록 틀)을 쓰고, `군수산업체 없이 생산` 빈 칸도 아래 업체 칸과 같은 원본 520×100px(260×50 GUI) `GFX_mio_entry_bg`로 y48부터 50 GUI 간격에 놓는다. 국가 메뉴(연구 & 생산) 목록도 같은 원본 틀·정렬 위치·항목 50 GUI 간격을 쓴다(사진 `20261118220938_1.jpg`, `position_default`). `filter_options`의 특성 점수 필터는 별도 단추 `GFX_icon_button_45x32`(선택 시 프레임 1) 위에 `GFX_refitting`을 원본 x13/y3px(9×9 GUI)로 얹고, 군수품·전차·비행기·함선은 각 `GFX_*_checkbox` 프레임 0/1을 쓴다. 다섯 단추는 원본 x152/173/194/215/236·y25/24/24/24/24의 23×16 GUI이며 `click_checkbox`를 재생한다. 툴팁은 `INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CAN_UNLOCK_TOOLTIP`과 `INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CATEGORY_TOOLTIP`(분류명 `INDUSTRIAL_ORG_LIST_WINDOW_FILTER_CATEGORY_MATERIEL/TANK/PLANE/SHIP`) 원문이다. 접근성 이름과 기존 로컬 정렬 동작은 유지하고 정렬은 생산 요청을 보내지 않는다. `production-offer`는 2560·1280·854 GUI2에서 정렬·중복 입력·업체 선택/미선택·닫기·비활성 업체 호버를 검사한다.

생산 라인의 우클릭은 업체 아이콘뿐 아니라 보이는 라인 본문에서도 `MANUFACTURER(lineId, "")`를 보낸다. 업체가 없는 라인과 요청 응답 대기 중에는 보내지 않는다. 업체 선택 팝업·설계 모달의 입력은 해당 창에 남기며, 해제 표시도 같은 세션의 새 revision 응답을 따른다. `organization-assignment`는 GUI2 두 해상도에서 빈 라인·라인 본문·업체 아이콘 반복 입력·응답 후 해제 상태를 검사한다.

이동 가능한 HOI 사이드바 바깥의 왼쪽 클릭은 기존 지도 선택 경로로 전달한다. 선택 성공 후 본문을 `MapCursor`로 바꿔 커서를 유지하며, HQ 목록에서 나가면 기존 세션·revision으로 `CLOSE`를 한 번 보낸다. HQ 초안·폐기 응답 대기 중에는 통과하지 않는다. 초안 폐기는 별도 확인창 없이 `HQ_DISCARD`로 요청한다. 알림 창이 겹쳐 있으면 알림 영역은 그 창이 입력을 받으며, 영역 밖의 지도 선택은 알림을 유지하고 뒤 본문만 바꾼다. `headquarters`·`army-layout` 집중 검사는 실제 렌더와 모의 요청을 사용하며 운영 접속·다중 사용자 검증은 별도다.

`army-layout` 자동 입력은 군 카드 자료 외에도 `CommanderRosterTest`가 사용하는 승진 확인·장군 고문·원수·제독 목록을 포함한다. `CommanderLeaderButtonsTest`·`CommanderAdvisorSessionsTest`·`CommanderAdmiralOffersTest`와 기존 생산자를 실행하고 manifest에 해당 입력을 기록해 렌더 전에 누락을 확인한다.
장교단 HQ 줄은 `countryofficercorpview.gui/hq_ribbon`의 523×85px 컨테이너 안에 원본 `GFX_officer_corp_army_hq_bg` 그림을 539×59px 비율로 왼쪽 위부터 그리고 컨테이너로 자른다. 컨테이너 크기에 그림을 맞춰 세로 중앙 정렬하지 않는다. HQ 단추는 x10/y5, 포획 단추는 x293/y0 원본 위치를 GUI2 단위로 환산한다. 포획 수의 작은 원은 `GFX_captured_commanders_button` 그림에 포함되어 있으며 별도 배지를 합성하지 않는다. 숫자의 최신 +1GUI/+1GUI와 문구 +1GUI 세로 보정을 유지한다. `officer-comparison`은 전체 줄과 원형 배지를 두 해상도의 실제 렌더로 대조한다.

포획 수는 기존 +1GUI/+1GUI 위치에서 물리 화면 픽셀 기준 오른쪽 1px·위쪽 1px로 추가 보정한다. GUI2에서는 +0.5GUI/-0.5GUI이며 현재 GUI 배율의 역수로 환산한다. 문구의 +1GUI 세로 보정은 유지한다.

HQ 목록은 뒤쪽 장교단 화면을 계속 렌더하며 초기화·창 크기 변경 때 부모 화면의 크기도 맞춘다. `headquarters` 집중 검사는 빈 목록과 저장된 편제 목록에서 장교단 전체 폭의 픽셀을 열기 전후 대조하고, 부모 크기가 달랐던 상태도 검사한다. HQ 목록/설계·포획 배지·군종 능력치의 기존 원본 에셋을 재사용하며 이 표시 수정에는 새 그림이 필요하지 않다. 운영 접속에서의 동일성은 별도 검증 대상이다.

HQ 추가 조작은 `import_branch_design_art.py`의 `division_controls`가 가져온 `production/design/division/priority_bg`, `priority_0..5`, `priority_icon_0..2`, `paradrop_0..1`, `special_0..2`, `expand`를 재사용한다. 각각 원본 `GFX_division_priority_bg`, `GFX_div_priority_strip`, `GFX_division_priority_icon_strip`, `GFX_can_paradrop_icon_small`, `GFX_special_forces_icon`, `GFX_expand_button`에 대응한다. 목록의 편집·삭제는 기존 `recruitment/button`·`recruitment/delete_template`을 사용하므로 추가 그림 가져오기는 필요하지 않다. 1280 GUI2에서는 중앙 HQ 설계창이 장교단 일부를 덮는 기존 배치 한계가 남아 있고, 목록 열기 전후 픽셀 유지 검사와 구분한다.

HQ 목록의 패널은 `armyhqtemplatewindow`의 375px 너비와 화면 높이 70%, `core.gfx`의 64px 타일 테두리를 GUI2에 적용한다. 목록 원점은 x12/y104, 행 간격은 95px이고 행 배경은 x−3의 341×95px 원본 비율을 유지한다. 새 설계 단추는 x188/y56의 `GFX_division_designer_button`, 행의 작은 방패는 `GFX_allow_as_hq_icon` 세 번째 프레임이다. 편집·삭제·이름·일수·지휘력도 같은 행 원점을 쓰며 원본에 없는 목록 갱신 단추는 제거한다. 목록 스크롤은 휠·화살표·손잡이 끌기를 지원한다. HQ 화면은 원본에 없는 하단 상태 문구를 그리지 않는다. 서버 거절 문구·거절 처리와 다른 화면의 상태 줄은 유지한다.

군수산업체 상세의 이름은 원본 x20/y12를 GUI2로 환산한 x10/y6에 유지한다. 상단 로고·레벨 판은 `top_left_window`의 x5/y50, 310×100px이며 레벨은 그 안의 x80/y25, 점수와 자금 막대는 x120/y25·y55를 기준으로 배치한다. 전체 및 상단 배경은 원본 `GFX_tiled_plain_bg`의 64px 타일 테두리를 사용한다. 특성·이력 탭은 생성하지 않는 기존 동작을 모든 상세 입력에 유지한다. `headquarters`, `organizations`, `mio-types` 집중 검사는 실제 GUI2 렌더이며 운영 접속·다중 플레이를 증명하지 않는다.

HQ 목록 아래 빈 공간도 오른쪽 바깥과 같은 지도 클릭 경로를 사용한다. 지도 선택 성공 시 기존 세션을 한 번 닫고 선택한 군 화면으로 전환하며 응답 대기·초안 중에는 통과하지 않는다. 별도 폐기 확인창은 열지 않는다. 목록 스크롤의 전체 높이는 마지막 행의 반올림된 위치와 실제 높이를 합산하여 47.5GUI 행 간격에서도 마지막 항목에 접근할 수 있게 한다. `headquarters`는 20개 합성 편제의 휠·손잡이 이동과 화면 `mouseClicked`를 통한 지도 전환을 검사한다.

결정 원본 비교의 `kor-scene-details.json`은 자동 권위 fixture 생성·해시 검사에 포함한다. 방산수출 여덟 상세를 동일 세션·revision의 화면에 적용하고, 선택 시 → 30일 동안 → 제거 시 순서와 서버 효과 문구의 보존을 검사한다. fixture 화면에서 상세 요청 대기를 모의로 등록한 실제 GameTest 렌더이며 운영 네트워크 접속 검사는 아니다. 생산국·장비 이름·필드별 소수 자릿수의 원본 차이는 서버 문구 영역이며 화면에서 임의로 고치지 않는다.

군수산업체 상세의 레벨 숫자는 빨간 배경의 전체 폭 안에서 가운데 정렬한다. 특성 트리 헤더는 `industrial_organisation_flavortext_background`의 x320/y50, 950×43px 배치를 유지한다. 사용자 투명 헤더 지시에 따라 불투명 `GFX_tiled_plain_bg_small` 대신 원본 `gfx/interface/tiles/marble_tiled_bg_edges.dds`를 사용하고 내부 띠를 덧그리지 않는다. 이 자산 선택은 원본 GUI 선언 그대로의 재현과 구분한다. 중앙 알파 0과 원본 픽셀을 보존하며 빈 헤더도 회색 테두리를 유지한다. 배타 관계는 한쪽 특성에만 선언되어도 양쪽 순서와 무관하게 한 번 표시한다. 원본 `GFX_focus_link_exclusive`의 1·2·3프레임(팩의 0·1·2)과 `GFX_focus_exclusive_line1`을 사용하며 특성 좌표는 서버 값을 보존한다. 발해조선중공업사 비교 기준은 `20261028050107_1.jpg`이며 특성·이력 탭은 사용자 제거 지시를 유지한다. `mio-types`와 `organizations` 집중 검사는 실제 GUI2 렌더/모의 입력 범위이며 운영 접속·멀티플레이 검증은 아니다.

군수산업체 레벨 숫자는 가로 중앙을 유지하고 기존 위치보다 1GUI 내린다. 특성 점수와 자금 막대는 x68GUI에서 시작하여 빨간 칸 오른쪽 끝(x64)과 4GUI 간격을 둔다. 자금 막대는 `interface/widgets/progressbar.gui`의 배경·진행·시작·끝 그림(`GFX_leader_exp_*`)을 사용하며 `summary.gauge.fraction`만 표시한다. 막대는 171×11 원본 픽셀을 0.5배로 그리고 0 진행도에도 원본 시작/끝 표시를 유지한다. TFR의 존재하지 않는 PNG 선언은 원본 vanilla `traittree.gfx`의 DDS 경로로 해석하되 실제 DDS는 TFR 덮어쓰기를 적용한다. 특성 아래 작은 아이콘만 1GUI 올리고 25GUI 폭 안에서 기존 `CountryFlagStack.left` 겹침 규칙을 사용한다. 톱니 본 그림·서버 노드 좌표·특성/이력 탭 제거는 유지한다.

배경 집중 재현은 위 고정 서버/프로토콜/팩 옵션으로 `test runGameTest remapJar -PhoiTestSuite=research-background`를 사용한다. 9개 탭 전환과 GUI2 2560×1440·1280×720을 검사한다. 원본 67장 중 보병 `20261028003612_1`, 지원 `20261028003610_1`, 해군 지원 `20261028004018_1`은 화면 대조에 사용하며 나머지 탭은 원본 사진 미제공이다. MIO의 `mio-types`는 실제 30유형에 더해 프로토콜이 허용하는 최대 4개 작은 아이콘과 0/50/100% 자금의 합성 입력을 별도로 검사한다. 모든 캡처는 실제 Minecraft GameTest이고 운영 접속·멀티플레이 검증은 별도다. 최종 무필터 검증은 서버 HQ 툴팁/결정/중점 입력 연결 뒤 수행한다.

연구 트리의 코드 바탕·분류 글자 뒤 바탕·선택 탭 아래 연결부는 검정(`#000000`)이다. 9개 분류의 원본 그림과 겹침 수(공학 1회, 공군 3회, 나머지 2회), 크기·배치·흰색 곱셈을 유지한다. 이전 청록빛 바탕은 `#101A21` 코드 색이었으며 해군 원본 배경 중앙의 `(212,212,212,9)` RGBA와 구분한다. 상세창 본문의 원본 `GFX_tiled_paper_bg`는 중앙 `(23,23,23,255)`의 무채색이라 유지한다. 상단 `GFX_tech_info_top_win`의 청회색은 TFR 원본 DDS 자체의 색(중앙 `(49,53,63,255)`)이며 검은 바탕 요청에 따라 이 연구 화면에서만 기존 `UiAssets.drawGrayscale`로 무채색 표시한다. 저장된 원본 이미지는 바꾸지 않는다. 상세창 내부 밑바탕과 연구 군수산업체 선택창 본문은 검정, 업체 창의 제목 배경은 무채색 그라데이션으로 표시한다. 기존 테두리·업체 행 그림·글꼴·수치·효과 색·입력은 유지한다.

검은 바탕 비교는 `research-background`와 `final-contract`를 같은 고정 입력으로 수정 전후 실행한다. 상세창은 원본 `20261028004018_1`, 업체 선택창은 `20261028004019_1`과 비교한다. 국가 화면 사이드 패널은 [국가 화면 패널 배경](#국가-화면-패널-배경)의 원본 창 배경을 쓴다. 코드로 그린 `UiMenuStyle.panel`은 아직 원본 창을 정하지 않은 팝업·대화상자에만 남는다. 이 집중 렌더는 운영 서버 접속·멀티플레이·최종 무필터 검증을 대신하지 않는다.

### 서버 원문 툴팁과 중점 인레이

HQ의 채워진 대대 칸은 `Slot.tooltip`, 선택 가능한 후보는 `Choice.tooltip`을 그대로 사용한다. 화면에서 클릭 안내·`편성 변경 시`·`changes`를 덧붙이지 않는다. 빈 문자열은 툴팁을 표시하지 않으며 선택 불가 사유는 서버 `reason`을 사용한다. 방산수출 상세는 이름·설명·구획·비어 있지 않은 `limitation`만 표시하고 결정 행의 비용·상태를 상세 끝에 추가하지 않는다. 생산국·장비 이름·숫자 정밀도는 서버 문구를 유지한다.

중점 상세의 진행 중 취소 단추는 `View.cancelable`이 거짓이면 원본 `buttonstate` 비활성 효과로 회색 표시하고 클릭·Enter로 요청하지 않는다. 툴팁은 취소 시 `cancelTooltip`, 시작 시 `startTooltip`을 원본 색 코드 변환만 거쳐 표시한다. 상세의 `limitation`으로 이 단추 툴팁을 덮어쓰지 않는다. 글자·배치·반복 요청 잠금·세션과 revision 검사는 유지한다.

`FocusInlays`는 서버 `View.inlays`에 포함된 세 한국 인레이 창을 외부 팩 `ui/focus_inlays.json`의 원본 GUI 좌표로 그린다. 계통도의 원본 픽셀은 GUI2에서 0.5배에 나무의 확대율·이동량을 적용한다. GUI에 없는 이미지 이름은 그리지 않는다. 2027 원본 스크립트의 `background_icon`은 해당 GUI에 없는 이름이므로 임의 좌표를 만들지 않는다. 창 내부의 `clipping=no`를 유지하며 전체 나무의 표시 영역으로만 자른다. `cho_bar`는 왼쪽, `lee_jun_bar`는 원본 역방향 효과에 따라 오른쪽부터 서버의 0~1 진행률만큼 표시한다. 자산 리로드는 인레이 배치 캐시도 비운다.

원본 인레이까지 검증하려면 `HOI_FOCUS_PREVIEW`를 설정하고 그 폴더의 `kor-election-2022-view.json`, `kor-oil-shock-view.json`, `kor-oil-shock-details.json`과 인접 `../focus/KOR.json`을 함께 제공한다. `FocusSessionsTest`와 `FocusDetailFixturesTest`가 생성한 입력을 사용하며 선거 원문·수치를 합성해 누락을 메우지 않는다. 복구할 때 생성 당시 서버 소스/JAR·JUnit 기록과 각 파일 SHA-256을 확인하고 별도 provenance를 남긴다. `HOI_FOCUS_PREVIEW` 없는 `focus-detail` 집중 실행은 게이지·종이와 일반 상세만 검사하므로 인레이·원본 시나리오 검증으로 보고하지 않는다. 전체 검증과 같은 외부 입력을 설정한 집중 실행으로 먼저 확인한다.

2022·2027 지지율 글 네 개는 서버 `Inlay.texts(name,text)`와 원본 GUI의 같은 이름인 글 상자를 연결한다. `context_aware_text` 키·상위 컨테이너 이동량·정렬·글꼴을 팩 `texts`에 보존한다. 한글패치 `VCR01_72_WT_glow`·`VCR01_32_glow` 원본 아틀라스와 fnt의 글자 위치·간격을 사용하며 색 코드를 보존한다. 숫자와 % 이외의 미포함 글자는 기존 Minecraft 글꼴로 원문을 표시한다. 없는 문구는 생성하지 않는다. `server-input-2`의 서버 형식으로 만든 2022 선거 표시 입력(50%·50%)과 2027 합성 0/30/100% 입력을 GUI2 두 해상도로 렌더했다. 문구가 있는 입력과 없는 입력의 실제 픽셀 차이, 인레이 제거, 이동·확대 중 요청 없음, 취소 불가 단추의 원문 툴팁·반복 클릭 거절을 포함한 `focus-detail`은 1분 19초 통과했다. 원본 인레이 세 장면과 취소 불가 중점 버튼 호버 사진은 미제공이다. 해당 장면은 원본 GUI/GFX와 원본 이미지로 검증하고 실제 원작 캡처 대조 완료로 세지 않는다. 운영 접속·다중 사용자·직접 음향 청취는 미수행이다.

집중 재현은 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=headquarters`, `focus-detail`, `decisions`를 각각 사용한다. 검증된 서버 사본과 같은 프로토콜 경로를 `-PhoiServerSource`·`-PhoiProtocolSource`에 지정하고 `HOI_COMMANDER_PREVIEW`는 그 입력 묶음의 `fixtures/commander-source-fixtures`, `HOI_FOCUS_PREVIEW`는 `fixtures/focus-ui`로 지정한다. 중점 검사는 이웃 `fixtures/focus/KOR.json`의 취소 불가 원문을 읽으며 인레이 3종과 막대 0/30/100%는 명시적 합성 입력이다. 최종 출하 검사는 필터 없는 `test runGameTest remapJar`로 구분한다.


## 설계창 버튼 대조 범위

원본 `divisiondesignerview.gui`의 버튼 선언 37개, `tank_designer_view.gui` 32개, `equipmentdesignerview.gui` 21개, `plane_designer_view.gui` 24개를 기준으로 현재 화면의 조작을 대조한다. 선언 수에는 반복 행 템플릿·미구현 하위 창도 포함되며 구현된 버튼 수가 아니다. 사단·HQ의 장비 버튼과 장비 허용 목록은 사용자 지정 제외다.

| 화면·조작 | 원본과 연결·검증 범위 |
| --- | --- |
| HQ 초기화·복제·저장·X | 초기화 x20/y620의 123×34, 복제 x150/y620의 123×36, 저장 x487/y667의 221×36, X x1156/y6의 32×33 원본 그림을 사용한다. 기존 `HQ_RESET`·`HQ_COPY`·`HQ_SAVE`·`HQ_DISCARD`를 유지한다. |
| 사단 초기화·복제·저장·X·우선순위 | 기존 원본 하단 세 그림을 유지한다. X는 원본 32×33으로 표시하고 예비·기본은 원본 y110, 정예는 y109에 맞춘다. 기존 편제 선택·대대/지원 선택·제거·뒤로 및 스크롤은 `division-designer`에서 검사한다. |
| 전차·함선·항공기 초기화·저장·X·뒤로 | 초기화 x10/y502의 123×34, 저장 x400/y502의 123×36, X x1048/y6의 32×33, 후보 창 뒤로 x406(해군404)/y71의 123×36을 사용한다. 해군 X의 기존 사용자 지정 왼쪽 1GUI 보정을 유지한다. 초기화·저장은 군종별 원본 효과음이며 공군은 원본 선언대로 전차 소리를 사용한다. |
| 전차 장갑·엔진 증감 | 원본 x323+106n/y460의 33×33 증감 그림을 사용한다. 현재 레벨·연구 최대치와 Shift/Ctrl 단계·비용은 서버 입력을 유지한다. |
| 업체·최신 특성 개량 | 원본 공군 x425의 1.4배 개량 그림과 x455 업체 그림은 영역이 8 원본 픽셀 겹친다. 그림은 유지하고 개량 입력 영역의 오른쪽 끝을 업체 입력 영역 앞에서 잘라 호버·클릭을 분리한다. 서버 `designTeam`의 후보·현재 배정·구식 상태에 따라 목록·우클릭 해제·개량을 연결한다. 계약이 없는 이전 입력에서는 두 단추가 비활성이며 요청을 보내지 않는다. |
| 역할·임무·슬롯·후보·해군 모듈 제거 | 기존 서버 후보·허용 임무·선택/해제·뒤로 경로를 `designers`로 검사한다. 원본 슬롯 그림·후보 카드·툴팁을 재사용한다. |
| 원본에만 있는 버튼 | 자동 설계/자동 개량·자동 갱신·동적 버전·설계 비교·프리셋·3D/사용자 지정 아이콘은 현재 설계 화면에 구현되어 있지 않다. 원본 선언이 존재한다는 이유로 동작하는 버튼이나 서버 기능을 만들지 않는다. 파일 저장/불러오기는 기존 제공 제외를 유지한다. |

HQ의 경험치(x297/y628), 배치 일수(x380/y623), 지휘력(x480/y623)은 원본 선언의 위치를 유지한다. HQ 상태 줄 제거는 `ArmyHeadquarters`에만 적용한다. 원본 HQ는 `20261028095229_1`~`095310_1`, 사단은 `20261027084504_1` 이후 사진을 비교 근거로 사용한다. 육해공 장비 설계의 동일 장면 원본 사진이 없는 경우 GUI 선언 대조와 실제 렌더 검사를 구분하며 사진 일치를 주장하지 않는다.

재현은 고정 서버/프로토콜 입력과 팩 빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=headquarters`, `division-designer`, `designers`이다. `IndustryDesignerButtonsTest`는 GUI2 2560×1440·1280×720에서 초기화/저장/X, 비활성 업체, 우클릭, 반복 클릭, 세션·revision을 검사한다. 기존 군종 검사에는 모듈·역할·임무·증감·툴팁과 작은 창 경계가 포함된다. 실제 원본 효과음 이벤트 연결과 운영 환경 직접 청취는 구분한다. 운영 서버 접속·멀티플레이는 미수행이다.

## 군수산업체 사용 조건 화면 대조

군수산업체 사용 조건은 설계 후보의 `manufacturer.tooltip`과 연구 업체의 `DesignTeam.tooltip`을 그대로 표시한다. 조건 있는 업체는 규모 다음 빈 줄, 사용 가능 문구, 초록색 `✓`와 소유국의 주 지배 조건, 빈 줄, 효과 순서다. 조건 없는 업체에는 조건 문구를 만들지 않는다. `-PhoiTestSuite=mio-availability`는 `HOI_MIO_PREVIEW`에 서버가 생성한 `designer.json`과 `research.json`을 지정해 GUI2 2560×1440·1280×720에서 목록·설계 업체 호버·연구 슬롯 업체 호버와 선택 요청의 ID/세션/revision을 검사한다. 팩 빌드 뒤 Java25 `test runGameTest remapJar`에 이 필터 및 같은 격리 서버/프로토콜 경로를 지정한다. 외부 fixture의 SHA-256과 producer 출처는 실행 전에 별도로 확인하며 이 집중 suite는 자동 fixture 생성 묶음을 사용하지 않는다.

원본 `20261028131749_1`은 3세대 전차, 시작 상태 fixture는 2세대 전차다. 원본 `20261028132110_1`은 열화상 광학장비와 규모 1(19%), 시작 상태의 중국남방공업집단 fixture는 `body_armor3`와 규모 0(0%)다. 수정 전의 주 지배 조건 평가 문제로 보병전차 업체와 중국남방공업집단이 선택 목록에 없었으므로, 실제 수정 전 비교는 고속전차 업체 한 곳 및 기존 연구 fixture의 다른 업체를 보여 준다. 동일 업체의 수정 전 툴팁을 합성해 실제 이전 화면으로 표시하지 않는다. 서버가 장갑·속도 이름 앞에 제공하는 `£armor_texticon`·`£attacker_speed_texticon`은 기존 `TooltipKeywordIcons`로 표시한다. `HOI_MIO_PREVIEW`에 같은 서버 입력의 `designer.json`·`research.json`을 두고 `mio-availability` 집중 검사로 실제 업체 호버를 재현한다. 문자 구분선은 공용 툴팁 규칙에 따라 빈 줄이며, 운영 소켓·멀티플레이·효과음 직접 청취 및 서버 전체 회귀는 별도 검증이다.

## 연구 완료 군수설계사 표시

TFR `interface/popupwindow.gui/research_finished_popup_window/design_team_icon`의 중심 x400/y130을 GUI2에서 사용한다. `research_complete` 알림에 서버가 `DialogView.Tile(section="design_team", icon=원본 업체 이미지)`를 제공한 경우에만 그린다. 현재 연구창의 선택 업체나 다른 국가의 목록으로 추정하지 않으며, 업체 없음·빈 이미지·중점 완료는 표시하지 않는다. 기존 외부 팩의 `mio/country/.../frame_0` 이미지를 원본 비율과 절반 크기로 사용하며 작은 창에서는 내부 폭에 맞춘다. 완료 알림의 표시 자체는 업체 배정·보상 요청을 보내지 않는다.

위치 근거는 보드 첨부 `572e59ca-bfaf-4dd1-ae6d-9f8c26008e81`와 설치된 TFR GUI다. 참조 영상의 9분 5초 장면은 링크를 가져오지 못해 직접 검증하지 않았다. `completion-design-team` 집중 검사는 명시적 합성 업체 있음/없음 입력으로 GUI2 두 해상도의 실제 로고 픽셀 차이와 중점 알림 무표시를 검사한다. `completion` fixture 그룹은 `DialogCompletionProgressTest`가 실제 연구 완료로 생성한 다산기공 있음/없음 및 중국 첩보기관 개선 완료 입력을 재생성하고 원본 소스·fixture 해시를 검증한다. `server-input-4`의 SHA-256을 확인한 격리 서버 사본을 사용한다. 서버 전체 회귀 검증은 Claude가 수행하는 별도 결과이며 클라이언트 렌더 통과로 대신하지 않는다.

연구·중점·첩보기관 개선 완료 창은 TFR `popupwindow.gui`의 500×260 틀을 GUI2에서 250×130으로 표시한다. 연구 제목은 사용자 지시에 따라 느낌표 없는 `RESEARCH_COMPLETED`의 "연구 완료"이고 중앙은 검은 바탕이다. `GFX_agency_popup_bg`는 `agency_upgrade_complete`에만 사용하며 현행 제목은 한글패치 `AGENCY_UPGRADED_TITLE`의 "첩보기관 개선 완료!"다. 개선 그림·이름은 서버가 보낸 값이며 세부 사항은 발급된 선택지와 현재 알림을 확인한 뒤 정보기관 화면을 요청한다. 중점 완료는 원본 사진 `20261028131950_1`과 `focus_finished_popup_window`를 따른다. 후속 원본 사진은 `20261031013533_1.jpg`이며 GUI 선언·해당 사진·실제 렌더를 구분한다.

원본 사진 `20261028132111_1`에 문자 점선이 있어도 연구 슬롯 효과와 설명 사이에는 빈 줄 하나만 둔다. 완료 창·연구·모집 화면에 문자 또는 코드로 그린 텍스트 구분선을 새로 넣지 않는다. 서버 원문은 보존하고 공용 `TooltipText.spacing`에서 표시용 구분선을 정규화한다. `research-slots`는 GUI2 2560×1440·1280×720의 실제 렌더, 합성 부대 아이콘·정밀도·빈 줄·호버, START/CANCEL 요청을 검사한다. 설계 업체/↑와 모집 후보·연도 툴팁은 `design-team-candidates`에서 서버 계약 입력으로 검사한다. `industry`는 업체 계약이 없는 이전 입력의 비활성·요청 차단도 검사한다. 운영 접속·멀티플레이·직접 효과음 청취는 별도 검증이다.

연구 슬롯은 원본 508×99 비율과 제목 x57/y12, 연도 x57/y60, 업체 중심 x450/y49를 유지한다. 업체가 붙은 연구는 톱니가 있는 원본 MIO 배경, 빈 슬롯과 업체 없는 연구는 기본 배경을 사용하고 연구 중에는 중앙 플라스크를 기술 그림으로 교체한다. 원본 게이지 틀 x62/y79와 막대 x65/y81(377×4), TFR 흰색/회색 3단 스트립을 쓴다. 빈 슬롯은 서버 `savedDays/30`, 연구 중에는 서버 `fraction()`으로 채운다. `research-slots`는 GUI2 2560×1440·1280×720 전체 프레임과 0·50·100%/업체 배정 합성 상태를 검사한다.

연구 트리의 장비 분류명·연도와 그룹 설명은 별도의 검은 사각형 없이 원본 연구 배경 위에 그린다. 글자·좌표·색과 상세 팝업 자체의 배경은 유지한다. `research-background`는 원본 기술 입력으로 모든 분류를 GUI2 두 해상도에서 촬영한다. 이 렌더 검사는 운영 접속·멀티플레이 검증을 대신하지 않는다.

연구 트리 하단의 완료·가능·진행·잠김 안내는 표시하지 않는다. 기술 노드의 상태색과 상세·툴팁의 권위 상태는 유지한다. 연구 상세의 조정치는 각 설계창과 동일한 `production/design/adjusters`·`production/design/controls` 자산을 재사용한다. 육군은 `movement/attack/defense`, 해역(`water_*`)은 `ship_movement/attack_defense/other`를 사용한다. 해군 공격·방어의 화살표는 설계창처럼 음수 우선, 그 외 큰 값으로 표시하되 툴팁의 개별 서버 수치는 보존한다. 연구 `TerrainValue`에는 해군 기타 효과가 없어 `other`는 증감 화살표 없는 기본 그림만 표시하고 수치를 만들지 않는다. 원본은 `divisiondesignerview.gfx` 및 `equipmentdesignerview.gfx/GFX_terrain_mod_ship_movement_strip`이며 기존 팩의 출처를 유지한다. 32×29GUI 프레임에 종횡비를 보존해 그려진 32×28 영역의 검은 원 중심을 기준으로 설계창과 같은 10.5GUI 아이콘을 정렬한다. 중립 프레임의 불투명 영역으로 정렬해 양수·음수 화살표의 여백이 기본 그림을 밀지 않는다.

공군 설계는 `plane_designer_view.gui/adjuster_stats`에 임무 그림을 표시하며, 현재 `IndustryScreen.designerAdjustment`는 대응 조정치 값이 없으면 하단 증감 아이콘을 그리지 않는다. 연구 서버 입력은 지형 이동·공격·방어만 제공하고 공군 임무 목록·효과는 제공하지 않는다. 연구에 임무 카드를 임의로 추가하지 않으며 향후 권위 입력이 제공되면 공군 설계 자산과 임무 계약에 연결해야 한다. 군수설계사 특성의 하단 보너스 아이콘은 사용자 지정에 따라 기존보다 1GUI 위(y+27)에 표시하며 크기·간격·겹침 순서·특성 호버는 유지한다. `industrial_organization_detail.gui/bonus_icon_set`의 출처를 보존한다.

이 표시 변경의 GUI2 두 해상도 재현은 `-PhoiTestSuite=final-contract`와 `-PhoiTestSuite=mio-types`이며, 각각 연구 하단 픽셀·서버 입력 상세·연구 선택과 군수설계사 보너스 영역·입력 검사를 포함한다. 해군 아이콘 캡처는 기존 서버 함선 설계의 세 해역 값을 연구 상세에 투영한 합성 표시 검사이며, 실제 연구 기술의 효과나 운영 캠페인 장면으로 취급하지 않는다. 렌더 검증은 운영 접속·멀티플레이·효과음 직접 청취를 대신하지 않는다.

복제 버튼은 원본에 있는 사단 편제와 HQ에서 유지한다. 육해공 장비 설계 UI에는 사용자 정정에 따라 복제 버튼을 추가하지 않는다.

렌더 fixture 전체 내보내기는 같은 JVM 출력 속성을 사용하는 묶음의 임시 생성 폴더를 공유한다. 각 묶음은 자기 파일 목록과 SHA-256 출처를 따로 검증·발행한다. `:client:verifyRendererFixtures`를 필터 없이 실행하면 설계·완료·지휘관 묶음이 함께 생성되는 경로를 확인할 수 있다. 렌더 시작 전 빌드가 실패하면 `clientGameTest-latest.json`은 이전 실행일 수 있으므로 Gradle 종료 코드와 실행 시작 시각을 함께 확인한다.

## 결정·중점·사단 선택의 원본 표시

결정 비용은 `decisions_l_korean.yml`의 `DECISION_COST_DESC`·`DECISION_COST_NOT_ENOUGH`를 마지막 빈 줄 뒤에 표시한다. 서버 `cost`를 원본 `|0` 정밀도로 표시하고 `costMet=false`일 때만 부족 색을 사용한다. ACTIVE/COOLDOWN 행에는 새 선택 비용을 붙이지 않는다. 행의 보라색 `£pol_power`는 기존 원본 키워드 글리프를 숫자와 같은 문장으로 그려 간격을 맞춘다. 설치 한글패치에 이 키의 덮어쓰기는 없다. 임무 머리말의 원본은 `core_l_korean.yml`의 `TIMED_DECISION_PREREQUISITES_HEADER_*`·`COMPLETE_EFFECT_HEADER_*`·`TIMEOUT_EFFECT_HEADER_*`이며 기한은 상세의 `missionDays`를 원본 정수 일수로 표시하며 0(미확정)이면 기한 머리말을 만들지 않는다. 중단 머리말은 `DECISION_CANCEL_HEADER`다. 머리말은 상세의 `mission`(`""`/`GOOD`/`BAD`/`SELECTABLE`)만으로 고른다. 일반 결정은 ACTIVE·progress 값과 관계없이 `DECISION_*`를 사용하며 선택형 임무의 기간 효과는 `TIMED_DECISION_MODIFIER_HEADER_SELECTABLE_*`를 사용한다. `DecisionScreenStateTest`는 네 종류의 READY/ACTIVE, 비용 충족 여부, 기간 있음/미확정을 합성 입력으로 검사한다. 실제 제공된 두 저장에는 SELECTABLE 장면이 없어 합성 검사와 구분한다. 원본 Steam `20261014201851_1`·`20261014201856_1`에 없는 돋보기 지도 버튼과 클릭 영역은 제거하고 인증된 호버 `HIGHLIGHT`와 세션별 도형 정리는 유지한다.

중점 완료 이름은 그림보다 위 계층에서 주황빛 노랑 `FFBD00`과 어두운 외곽선 `16110B`로 표시한다. 원본 `20261028131950_1.jpg`의 이름 가독성을 기준으로 하며 Minecraft 글꼴은 유지한다. 연구·정보기관 완료 이름은 기존 색·그림자를 유지한다.

군·집단군 카드의 배정 초상화는 `leadergroups.gui/leader_icon`의 원본 배율 .39/.45, 좌측 육군 상세는 `unitview.gui/leader_icon`의 .33을 GUI2의 절반으로 적용한다. 가로·세로에 하나의 배율을 적용하고 내부 테두리 밖만 자른다. 지휘관·참모 버튼은 금색 카드 프레임이 아닌 보라색 초상화 그림 사각형의 좌우·하단 끝선에 맞춘다. 카드 내부의 군 그림은 (4.5, 5) 30.5×41.5GUI, 원수 그림은 (5, 6) 35×42.5GUI, 좌측 육군 상세 그림은 (6.5, 17) 24.5×33.5GUI이며 실제 그리기에 사용한 정수 사각형을 기준으로 삼는다. 원본 버튼의 투명 여백을 제외한 보이는 끝선과 클릭 영역을 맞춘다. 참모 임명 후 종이 배경에도 같은 규칙을 적용하며 그림 배율·종횡비를 유지한다. 무지휘관 그림과 해·공군 경로는 기존 표시를 사용한다. 원본 텍스처와 배정 렌더의 비율 대조는 예전 화면 대비 얼굴 높이만 비교하는 것과 구분한다.

재현은 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=decisions`, `-PhoiTestSuite=completion-design-team`, `-PhoiTestSuite=commander-layout`를 검증된 고정 서버 사본에서 각각 실행한다. 실제 캠페인 완료·결정 비교는 `HOI_CAMPAIGN_PREVIEW`와 `-PhoiTestSuite=campaign-source`를 사용한다. GUI2 두 해상도·모의 입력 검사이며 운영 접속·멀티플레이·직접 음향 청취와 원작 전체 픽셀 일치는 별도 검증이다.

메뉴 탭과 각 창의 헤더는 볼드체를 사용한다. 공통 메뉴 제목의 기존 `(font.lineHeight + 2) / font.lineHeight` 확대와 사단·장비 설계 제목의 기존 확대 배율을 유지하며, 굵은 글자의 실제 폭으로 잘림과 중앙 정렬을 계산한다. 별도 렌더 경로인 사건·결정, HQ, 포획/지휘관 목록, 교리·선호 전술, 연구 상세·업체 선택, 중점 계통도·상세, 장비의 모듈 선택, 완료 창도 같은 제목 규칙을 따른다. 본문·수치·원문 툴팁의 볼드 여부는 변경하지 않는다. 사단 대대 선택창의 아래 테두리는 창 하단에서 53GUI 위에 두고 후보 행도 그 안에서만 표시해 자원·경험치 및 초기화·복제 줄을 덮지 않는다. 작은 화면에서 넘치는 후보는 기존 휠 스크롤로 접근한다.

`test runGameTest remapJar -PhoiTestSuite=ui-headers --console=plain --no-daemon`은 GUI2 두 해상도에서 확대 메뉴 제목의 실제 흰색 픽셀을 보통체·볼드체와 비교하고, 굵은 글자의 폭 제한을 검사한다. 정치·장교단 합성 스냅샷의 실제 메뉴 렌더도 캡처한다. `division-designer`는 후보 카드가 자원·경험치 줄 위에 머무는 경계와 기존 작은 창 스크롤을 검사한다. 이 집중 검사는 전체 창의 원본 비교나 운영 접속·다중 사용자 검증을 대신하지 않는다. 전체 회귀는 최종 서버 입력 인계 후 무필터 명령으로 별도 실행한다.

국회 `NatAsm_member_text`는 TFRKP `TFR_Korea_NatAsm.gui`의 글자 크기보다 원본 좌표 기준 2픽셀 크게 그린다. GUI2의 절반 배율에서 약 1픽셀 증가하며 다른 패널 문구와 플레이어 GUI 설정은 바꾸지 않는다. 장관 특성의 `£GFX_KOR_DemocraticParty_texticon` 같은 원본 토큰은 `TooltipKeywordIcons`로 표시한다. 서버가 보낸 이름·색상·수치는 클라이언트에서 추정하지 않는다.

HQ 설계의 X·Esc는 별도 초안 폐기 확인창 없이 `HQ_DISCARD`를 한 번 보낸다. 처리 대기 중 반복 입력과 오래된 응답 차단은 유지한다. 대대 선택창은 원본 `REGIMENTS_SELECTION_CANCEL` 위치를 유지하되 최신 사용자 문구 지시대로 “확인”을 표시하며 요청 없이 후보창만 닫는다. 사단 설계 후보는 같은 GUI의 250×66 카드, 256×69 간격과 그림 위치를 GUI2 절반 크기로 사용한다. 화면에서 조합하던 선택창 제목은 `REGIMENT_WINDOW_DESC`로 교체한다. 사단 후보 경험치 비용은 서버 `Designer.changeCosts`의 `line:<현재>><후보>` 또는 `support:<현재>><후보>` 값만 표시한다. 계열 선택과 미제공 비용에는 숫자를 만들지 않는다.

중점 완료 이름은 원본 그림 다음 렌더 계층에서 그린다. 배타 연결은 원본 `GFX_focus_link_exclusive`의 중앙·좌·우 프레임과 `GFX_focus_exclusive_line1`을 이름판 중앙 높이에 배치한다. 연결을 한쪽 중점만 선언해도 표시하며 서로 선언한 쌍은 한 번 그린다. 사단 수가 0인 `ASSIGN_ARMY` 카드는 표시용 스냅샷에서 제외한다. 지도에서 고른 배정 사단은 기존 군/집단군 카드 정보창을 사용하고, 별도로 만든 “N개 사단 선택됨” 제목창은 표시하지 않는다. 군·집단군 카드 하나를 직접 선택한 경우에는 기존 지휘관 상세·사단 목록·목록 전환 버튼을 유지한다. 사단 수동 복수 선택과 군 카드 복수 선택은 군·집단군 카드 묶음으로 표시한다. 미배정 사단이 포함된 혼합 선택도 기존 미배정 목록과 최대 11개 행·스크롤·선택 동작을 유지한다.

결정의 `ACTIVE` 행은 `GFX_decision_item_bg`의 붉은 첫 프레임을 사용한다. 상세 응답 전에는 서버 이름·설명만 표시하며 임의 정치력·실행 상태·남은 기간을 툴팁 끝에 붙이지 않는다. 제목은 원본 강조색이고, 상세의 선행 조건·선택 시·기간·제거 시 제목과 추적 알림은 팩 `command/labels.json`의 원문 키를 사용한다. 효과 줄 순서·숫자·인라인 아이콘은 서버 입력을 보존한다. country/session/revision 검증 및 상세 요청 제한은 유지하며 텍스트 구분선은 빈 줄이다.

팩 빌드 뒤 Java25 `test runGameTest remapJar -PhoiTestSuite=division-designer --console=plain --no-daemon`으로 GUI2 2560×1440·1280×720과 작은 창의 후보 선택·스크롤을 재현한다. `HOI_COMMANDER_PREVIEW`는 같은 서버 계약의 사단 fixture 폴더다. HQ·완료·중점·결정은 각각 `headquarters`, `completion-design-team`, `focus-detail`, `decisions` 집중 검사를 사용하며 최종 검증은 필터를 생략한다. 합성 완료 이름 픽셀 검사는 실제 캠페인 완료 재현과 구분한다. 비교 이미지에서 해당 원본 사진이 없으면 미제공으로 표시하고, 수정 전 큰 프레임의 축소본을 실제 작은 해상도 촬영으로 표시하지 않는다.

결정 목록의 같은 국가·세션에서 revision이 바뀌면 이미 받은 상세를 유지하고 새 revision으로 다시 요청한다. 새 응답은 현재 revision과 발급 token·country·session·요청 이력·목록 ID를 검사한다. 다른 세션의 상세는 재사용하지 않는다. 서버가 요청을 제한하는 동안에도 기존 원문 툴팁을 유지하며 클라이언트 상태 문구로 바꾸지 않는다.

`HOI_CAMPAIGN_PREVIEW=<검증된 서버 입력>/fixtures`를 지정하면 `campaign-source`와 무필터 전체 검사가 `cmp80/`, `politics-choice-fixtures/`, `commander-source-fixtures/`의 운영 백업 기반 입력을 재생한다. `test runGameTest remapJar -PhoiTestSuite=campaign-source --console=plain --no-daemon`은 실제 역학조사 완료 응답, 정부수반·내무·정보 후보, 72일/504일 결정 목록과 같은 세션·revision의 상세를 GUI2 두 해상도로 렌더한다. 완료창·장관 목록과 호버·결정 상세 총 38장으로 원본 토큰의 글리프 변환과 인증된 상세 수신을 확인한다. `design-team-candidates`는 실제 업체 툴팁의 장갑·속도 토큰에 원본 글리프가 연결되는지도 검사한다. 입력 폴더와 SHA-256 목록은 실행 전에 확인한다. 캠페인 입력 루트에는 cmp80 외에 commander-source-fixtures/kor-selected-menu.json과 politics-choice-fixtures/kor-{minister,interior,intelligence}-ready.json이 필요하다. 분리된 고정 입력을 모을 때는 같은 서버 입력의 파일을 해시 검증 후 그대로 복사하고 출처·해시를 보존한다. 빠진 파일을 이유로 campaign-source 검사를 생략하지 않는다. 백신·휴교령·집합금지 결정은 인계된 두 저장에 없어 실제 장면 대조 범위에 포함하지 않는다. 원본과 캠페인 상태가 다른 비교는 픽셀 동일성의 증거가 아니다. 실제 소켓→서버 비용·저장, 운영 접속·멀티플레이·효과음 직접 청취는 이 입력 재생 검사와 구분한다.

`HOI_FOCUS_PREVIEW=<검증된 서버 입력>/focus-ui`와 `-PhoiTestSuite=focus-detail`은 같은 서버 검증에서 생성된 `kor-oil-shock-view.json`·`kor-oil-shock-details.json`의 실제 중점 상세를 재생한다. 인레이와 취소 불가 검사를 위해 같은 묶음의 `focus/KOR.json`도 필요하다. 이 View는 진행 중 노드와 `cancelable=false`, 서버의 `cancelTooltip`을 포함해야 하며 시작·완료 View로 대신하지 않는다. 입력의 조건·효과 순서와 원문 색상은 그대로 비교하며 오래된 상세 파일을 최신 계약 검증에 사용하지 않는다. 별도의 배타 연결 캡처는 원본 트리 좌표·그림으로 강경/완화 쌍을 화면 안에 배치한다. 이 트리 캡처와 상세 응답 없는 장면은 실제 캠페인 상세 검증과 구분한다.

`CommanderCardScopeTest`의 공군 진단 입력에는 사단 0개 군이 있다. 빈 군 필터를 별도로 검사하고, 비행단 선택·임무·배속 범위 검사는 같은 군 ID의 `air-attached` 실제 군 카드와 `air-diagnostics` 비행단을 합친 입력으로 수행한다. 이 조합은 단일 서버 스냅샷 재현이 아니며, 0개 군을 표시하기 위해 운영 필터나 원본 입력을 변경하지 않는다. `navy-layout` 집중 검사와 무필터 전체 검사에 포함한다.

## 해역 접근 정책 버튼

해역 상세의 허용·회피·봉쇄는 서버가 발급한 `naval_access/ALLOWED`, `naval_access/AVOID`, `naval_access/BLOCKED` 선택을 기존 `DialogPayloads.Action`으로 보낸다. 왼쪽 클릭만 요청하며 창을 유지한다. 선택 강조는 `access` 타일의 `value=selected`만 따르고, 응답 대기 중 중복 요청은 차단한다. 같은 token의 더 높은 revision이 와야 다시 활성화한다. 관전자의 빈 choices는 회색 비활성 버튼으로 표시하며 원본 설명은 유지한다. 원본 강조는 `naval_strategic_region_view.gui/naval_access_rule_highlight`와 `theatreselector.gfx/GFX_naval_access_rule_highlight`의 PNG를 사용한다.

회피·봉쇄 사선과 실제 항로 적용은 서버 담당이다. 클라이언트는 도형·정책을 따로 추정하거나 사선을 중복 렌더하지 않는다. `HOI_CAMPAIGN_PREVIEW/cmp82/sea-detail-kor-76-{allowed,avoid,blocked}.json`의 실제 연속 응답을 사용하는 `-PhoiTestSuite=sea-access`로 GUI2 두 해상도 선택·툴팁·좌우 클릭·반복 클릭·낡은/다른 세션 거부를 검사한다. cmp82 폴더가 없을 때만 기존 cmp80-6 경로를 사용한다. 관전자 응답은 실제 입력에서 choices/선택만 제거한 권한 경계 검사다. 이 검사는 서버 사선 패킷·실제 항로·운영 접속·멀티플레이·직접 음향 청취를 검증하지 않는다.

생산 우선순위 위·아래 버튼은 맞춤 장비 그림의 오른쪽에 붙이고 아래 끝은 같은 내부 영역에 둔다. 그리기와 클릭 좌표는 같다. 생산 장비 그림은 펼친 행의 테두리 안쪽 전체 영역에 원본 종횡비로 맞추고, 검은 마스크와 장비명은 그림의 하단에 겹쳐 그린다. 라벨 글자는 마스크의 폭과 높이에 맞춘다. 접힌 행은 원본 .37배율을 상한으로 하되 장비 영역을 넘으면 비율을 유지해 축소한다. 그림을 늘이거나 잘라 맞추지 않는다. 해군 배경은 원본 116px 높이를 사용한다. 펼침·접힘 모두 장비 그림의 아래 끝은 하단 테두리 안쪽으로 제한한다. `-PhoiTestSuite=queue-drag`는 실제 한국 생산 입력의 육군·해군·항공 행을 각각 표시하고 장비 위젯을 숨긴 렌더와 비교해 그림·라벨의 테두리 바깥 픽셀을 검사한다. 접힘/펼침과 GUI2 두 해상도를 포함하며 입력에서 한 행만 발췌한 표시 검사이다. 실제 개장 진행 중 응답은 이 입력에 없어 별도 검증 대상이다.


## 공통 글자 하단 정렬

한글과 섞인 숫자는 공통 글꼴 보정으로 한글의 보이는 하단 끝선을 맞춘다. 생산 효율 옆 혼합 문장의 숫자도 이 보정을 유지한다. 공통 보정 이전의 숫자별 +1GUI 보정은 중복되지 않도록 제거한다. 독립 숫자 칸(± 사이 공장 수와 상단 공장·조선소·수리 조선소의 사용/전체 수 등)은 공통 UiText 경로에서 보이는 숫자 경계를 칸 중앙에 맞추며, 혼합 문장이나 일반 글자의 배치 전체를 재계산하지 않는다. 숫자·영문이 떠 보이는 문제는 공통 글꼴 렌더 경로에서 보정하며 개별 숫자 좌표에 임시 오프셋을 더하지 않는다. 기존 글꼴 크기·볼드·문구·색·값·원본 숫자 정밀도는 보존한다. 원본 전용 글꼴과 아이콘 글리프를 기본 영문 글꼴로 간주하지 않는다. 기본 영문 글꼴의 기준선을 맞추며 g·y·p·q의 기준선 아래 꼬리는 원래 형태로 유지한다. GUI2 두 해상도의 보통체·볼드체·확대 글자 실제 픽셀 비교는 ui-headers 집중 검사에 포함하며 운영 접속 검증과 구분한다.


## 대기열 이동과 군 카드 표시

집단군의 하위 군 카드는 처음부터 펼쳐 표시한다. `navy-layout`의 육군·공군 전구 전환 검사도 선택 전구의 집단군과 하위 군 전체가 표시되는지 확인한다. 접기 버튼으로 사용자가 접은 상태는 선택과 서버 갱신 뒤에도 유지한다. 군·집단군 목록 안에서 사단을 선택하면 해당 카드의 전체 소속 목록과 원래 순서를 유지하고 선택 배경만 바꾼다. 서버의 선택 사단 상세 응답은 해당 행 값을 갱신하며 다른 소속 행을 제거하지 않는다. 0개 선택에서는 좌상단 창을 숨기는 규칙을 유지한다.

해군 지도 드래그 선택은 좌상단에 선택한 태스크포스와 도구줄을 표시하고 지휘관 머리줄은 표시하지 않는다. 태스크포스를 직접 선택하면 그 함대의 지휘관 머리줄과 소속 목록으로 전환한다. 여러 함대가 선택된 상태에는 태스크포스 목록을 표시한다. 태스크포스 행의 선택 해제로 해당 함대의 마지막 선택이 빠지면 함대 카드 선택도 해제하며, 남은 태스크포스만 명령 대상으로 유지한다. 드래그 목록에서 하나만 남아도 직접 선택하기 전까지 지휘관 머리줄을 표시하지 않는다. `navy-layout`은 마지막 태스크포스 선택 해제 후 패널과 입력 영역이 닫히는지 확인하고, 함대 카드를 다시 연 뒤 단일 선택에서 모두 선택으로 전환하는 실제 입력을 검사한다. `commander-controls`와 무필터 검사는 복수 함대 선택·직접 선택 전환·개별 선택 해제·남은 대상의 명령·함대 창 닫기를 검사한다. 공군은 선택한 비행단이 속한 기지의 모든 비행단을 표시하고, 선택한 행에만 원본 선택 프레임을 사용한다. 도구줄의 선택 수와 명령 대상은 미선택 행을 포함하지 않는다. 기지 ID가 있는 응답은 표시 이름 대신 해당 ID로 묶는다.

`CommanderGroupsRenderTest`는 군·집단군의 한 사단 선택 후 전체 행 유지와 선택 사단만 들어온 상세 갱신을 검사한다. `CommanderSelectionTest`는 GUI2 2560×1440·1280×720에서 해군 복수/단일 선택과 같은 기지 비행단의 부분 선택, 재수신, 선택 효과음과 7개 함종의 원본 그림 리소스를 검사한다. 재현 suite는 `army-groups`와 `selection-review`이며, 실제 운영 접속·멀티플레이·직접 장치 청취는 포함하지 않는다.

하단 태스크포스 표식 줄은 서버 `Member.icon`의 원본 그림을 함대색으로 그린 뒤 같은 프레임의 선택 테두리를 겹친다. TFR `theatreselector.gui:134-149`의 `navy_theater_fleet_item`에는 공통 육군 표식이 선언되어 있지만, `gfx/army_icons/army_icons.txt:724-725,791-792`의 `fleet`·`task_force`가 실제 종류를 `GFX_theatre_task_force_shield`로 지정한다. `theatreselector.gfx:132-141`의 해군 기본/선택 sprite는 `navy_icons.dds`·`navy_selected_icons.dds` 각각31프레임이다. 기본33×32px를 유지하고 선택35×34px 테두리는 왼쪽1px에서 겹치므로 선택해도 내부 그림이 사라지지 않는다. 이전 서버의 육군 sprite 경로도 대응하는 선택 테두리로 표시한다.

원본 `common/defines/00_defines.lua:2108-2126`의 `TASK_FORCE_ROLE_TO_INSIGNIA`는 잠수함대15→frame14, 항모 기동부대22→frame21 등 역할별 표식을 정의한다. `00_graphics.lua:1401-1402`의 기본값은 태스크포스6→frame5, 함대4→frame3이다. 역할 결정·명시 표식 선택은 서버 책임이며 클라이언트는 `MemberData.insignia=null`을 방패0으로 바꾸거나 함종을 추측하지 않는다. 원본 사진의 항공기·잠수함은 이 해군 표식이며 흰 함종 요약 그림과 다른 자산이다. 닻은 미선택 기본 프레임을 서버 함대색으로 그리며, 사진의 갈색이나 선택 노랑을 모든 국가에 고정하지 않는다.

사용자 지시에 따라 하단 줄은 기존 `navalBadges`의 기본15.5GUI 간격·넘침 배치와21GUI 클릭 폭을 유지한다. 배경 폭은 기존 `max(150.5, 22 + 표시 항목 수 × 15)`GUI, 페이지 용량은 기존 `(도구줄 폭 - 36GUI) / 15GUI`를 사용한다. 숫자 최대 폭으로 간격·배경·페이지 용량을 늘리지 않는다. 아이콘을 모두 그린 다음 기존 좌표에 숫자를 그리며 숫자별 좌표 보정은 추가하지 않는다. `selection-review`는 직접 선택·재클릭·재수신 후 기본 그림의 색상 픽셀과 기존 간격·클릭 폭을 GUI2 두 해상도에서 검사한다. `navy-layout`은10개 태스크포스의 일정한 픽셀 간격과 화면 경계·선택 동작을 검사한다. 팩의 기존62프레임을 재사용하며 원본 RGBA를 바꾸지 않는다. 기본15.5GUI 간격은 기존 ceil 픽셀 반올림을 유지하므로 GUI2의 인접 좌표 차이는15/16GUI가 교대로 나온다. 새 역할 표식의 집중 재현은 `test runGameTest remapJar`에 `-PhoiTestSuite=navy-layout`, `selection-review`, `movement-controls`를 각각 지정한다. 마지막 suite는 지휘 패널 검사도 포함한다. `navy-layout`·`movement-controls`의 armyHud 입력은 `-PhoiServerSource=<서버 소스 사본>`에서 자동 생성되며 `HOI_COMMANDER_PREVIEW`를 덮어쓴다. 전달받은 JSON만 새 경로로 지정해서는 새 서버 변경을 검증할 수 없으므로 전달본과 일치하는 서버 소스 사본과 생성 입력의 표식 경로를 확인한다. 이전 간격 변경 화면과 기존 입력의 방패 화면은 새 역할 표식 검증 근거가 아니다.

`CommanderLeaderControlsTest`는 사단이 없는 육군 카드의 카드 특성·고문 버튼을 계속 검사하되 좌상단 초상·특성·고문 hit가 없는지 확인한다. 사단이 있는 군과 해군의 패널 검사는 유지하며, 집단군 패널 배치·능력 검사는 기존 `army-trait-skills` 입력을 쓰는 `CommanderPanelsTest`에서 수행한다.

태스크포스·함대 머리줄의 순양전함 그림은 프로토콜의 `unit_hud/navy_battlecruiser`를 원본 `GFX_unit_battle_cruiser_icon_medium_white`에 연결한다. 원본 GFX 이름의 밑줄을 생략하면 없는 리소스를 찾아 그림이 누락된다. 서버 함종 ID는 바꾸지 않으며 다른 함종과 동일한 원본 30×12px 그림·알파·표시 배율을 유지한다.

원본 전용 화면 대조가 남아 있는 공통 창은 `air/organization`(창설·분할·병합·해체), `air/groups`(그룹 편성·이동·병합), `air/reinforcement`(보충 정책·항공 설계 상태), `air/aces`(에이스 배정), `commander/naval-settings`(태스크포스 편성·표식 등), `navy/organization`(함선 이동·분할·병합), 해군 보충·예비함 설정이다. 현재 이 응답들은 `DialogCoordinator`에서 `CommanderRoster`의 일반 목록으로 전달된다. 원본 키·에셋을 사용한 항목도 전용 창 배치까지 재현한 것은 아니다. 해당 창과 펼친 선택 메뉴의 원본 스크린샷을 받은 뒤 대조해야 하며, 이번 선택 목록 변경이 이 창들의 재현 완료를 뜻하지 않는다.

생산·건설 행은 누른 지점의 세로 오프셋을 유지하며 마우스를 따라간다. 주변 행은 놓을 위치를 비워 주고, 이동 행의 배경·장비·자원·숫자·버튼은 같은 렌더 경로로 다시 그린다. 반투명 테두리 아래에 다른 행이 비치지 않도록 이동 영역의 기존 패널 배경도 함께 그린다. 이 순서는 화면 미리보기이며 권위 있는 `lines`·`projects` 목록은 응답 전까지 유지한다. 놓을 때 기존 `MOVE` 한 번만 전송하고, 같은 자리·영역 밖·Esc·새 revision·화면 닫기는 취소한다. 접힘/펼침, 자동 스크롤, 응답 대기 잠금과 원본 `PRODUCTION_DRAG_N_DROP` 문구는 유지한다.

군 카드 드래그는 왼쪽 집단군 생성 칸과 오른쪽 새 군 `+` 칸까지 표시 범위를 넓힌다. 집단군 생성 칸에 놓으면 기존 소속의 순서 처음, `+` 칸에 놓으면 순서 끝으로 이동하는 `CARD_ORDER`만 허용하고, 이미 그 자리이면 보내지 않는다. 집단군 생성·`CREATE_ARMY`나 소속 변경은 보내지 않는다. `+`의 초상 배경 위치·크기와 외곽 크기는 일반 군 카드와 같은 값을 사용하며, 기존 외부 팩 `commander/hud/army_frame/frame_0`와 `army_background/frame_0`를 재사용한다. 원본 경로와 해시는 팩 `commander-hud-import-report.json`에 있다.

지휘관이 미배정된 군 카드에서는 왼쪽 아래 `LEADER_TRAITS` 그림과 호버/클릭 영역을 만들지 않는다. 지휘관이 있는 카드와 다른 버튼은 유지한다. 육군 지도에서 선택 사단 ID가 0개이면 군/집단군 카드 선택이 남아 있더라도 왼쪽 위 육군 상세 창을 그리지 않는다. 서버 선택·자산을 생성하거나 수정하지 않는다.

해역 상세의 독립 `logbook` 그림·호버를 제거한다. 남은 접근 정책 버튼은 원본 연속 배치 `x=21+i*35`를 그대로 사용하고 제거한 버튼을 위한 빈 항목이나 대체 단추를 만들지 않는다. 오래된 입력에 `logbook` 타일이 있어도 표시하지 않는다.

지도 또는 유효한 캠페인 HUD가 있는 동안 손에 든 아이템 이름(`HELD_ITEM_TOOLTIP`), 관전자 아이템 안내(`SPECTATOR_TOOLTIP`), 액션바(`OVERLAY_MESSAGE`)를 숨긴다. 별도 반투명 명령/임무 안내 패널은 제거하고 메시지는 같은 바닐라 액션바 경로에 맡긴다. 일반 Minecraft 화면에서는 원래 표시를 유지한다. 선택 도구의 `CUSTOM_NAME`·인벤토리 이름·지도 범례와 명령의 권한·대기·거절 처리는 변경하지 않는다.

집중 재현은 팩 빌드 후 Java25의 `test runGameTest remapJar -PhoiTestSuite=presentation-review`이며, `HOI_COMMANDER_PREVIEW`에 지휘 패널 입력, `HOI_CAMPAIGN_PREVIEW`에 `cmp80-6` 해역 입력을 둔다. `queue-drag`와 `army-groups`의 기존 이동/취소/권한 검사도 유지한다. 생산은 서버 원본 입력, 건설 순서·미배정 지휘관·빈 집단군은 표시 경계용 합성 입력이다. GUI2 2560×1440·1280×720의 실제 렌더·모의 요청 검사는 운영 접속·멀티플레이·직접 효과음 청취와 구분한다. 비교용 `HOI_CAPTURE_BEFORE=1`은 수정 전 제품 코드로 찍는 장면에만 쓰며 새 동작의 픽셀 판정은 생략하므로 검증 통과 근거로 사용하지 않는다.

교리 후보 선택은 서버가 보낸 `Choice.rewards`로 왼쪽 트랙을 미리 표시하며, 제목·설명·선택 강조만 로컬에서 바꾼다. 통달치·선택 상태·보상 해제는 서버 응답을 유지한다. 하단 잠금 해제에서만 `SELECT_GRAND`/`SELECT_SUBDOCTRINE`를 한 번 보내고 대기 중 반복 입력을 막는다. `assignDetail`과 `advancedDetail`은 서버 원문 그대로 표시하며 비활성 잠금 해제에도 거절 툴팁을 유지한다. 후보 목록은 종이 영역 안에서만 그리며 넘치는 경우에만 원본 크기의 스크롤바를 표시한다. 휠·화살표·손잡이 드래그로 마지막 후보까지 접근할 수 있다. 경험치 부족 후보도 미리 볼 수 있지만 잠금 해제는 비활성이다. 대교리 미선택 안내는 서버의 `NO_GRAND_DOCTRINE_ASSIGNED`·`GRAND_DOCTRINE_BENEFIT_DESCRIPTION`을 쓰며 통달 아이콘과 전체 문단을 원본 안내 영역 안에 표시한다. 후보 비용은 원본처럼 칸 오른쪽 아래에 작은 글자로 오른쪽 정렬한다. `hud/{army,navy,air}_experience` 별은 경험치가 부족해도 군종 색을 유지하며, 비용은 충분하면 노랑·부족하면 빨강이다. 비활성 후보의 교리 그림에는 원본 회색 셰이더를 유지한다. 빨간 ✕는 `Choice.requirementsMet=false`에만 표시하며 경험치 부족만으로 붙이지 않는다. 다른 트랙과의 배타 조건도 이 서버 필드를 따르지만 해당 ✕ 장면의 원본 사진 대조는 미수행이다. 세부사항 툴팁은 단추 위쪽, 잠금 해제 툴팁은 아래쪽에 둔다. 선택 설명은 가운데 정렬하며, 미리보기 트랙의 I~V 뒤에는 원본 `GFX_ongoing_generic_glow_yellow/frame_0` 한 장을 원본 크기의 절반으로 표시하며 사용자 지정 600ms 순차 fade를 적용한다.

암호학 지도 단추는 현재 비공개 `AgencyView.capitals`의 x/z와 로드된 지형 높이를 지도 차원(`hoi:atlas_`)의 카메라에 투영한다. 화면 밖·뒤쪽·미로드 청크·정보기관 패널 아래에는 클릭 영역을 만들지 않는다. 같은 응답에 있는 `decrypt:<tag>`/`pause:<tag>` 항목만 사용하고, 좌클릭은 현재 세션/revision의 `CALL` 한 번을 보낸다. 우클릭·비활성·응답 대기는 차단하며 탭 전환·새 응답에서 옛 클릭 영역을 지운다. 암호학 진행 숫자는 권위 있는 0..1 진행률의 정수 %이며 상태 문구를 숫자 칸에 넣지 않는다. 실제 기본 지도 설치와 멀티플레이 동기화는 테스트 월드 투영과 별도 검증이다.

암호학 목록은 1280×720에서도 하단의 남은 높이에 행을 그리고 넘치는 부분만 자른다. 원본 크기 스크롤바의 화살표·손잡이 및 휠로 마지막 나라까지 접근하며 잘린 영역 밖에는 조작부를 만들지 않는다. 정보원 모집창은 원본 550×607 패널 비율과 기존 행 간격을 보존한다. 국기는 각 행 오른쪽 둥근 배경 중심에 맞춘다. 이름/기술 정렬 방향, 검색 선택·포커스 유지, 제목줄 드래그를 지원한다. 후보가 다섯 행을 넘을 때만 기존 원본 크기의 ▲▼와 손잡이를 표시한다. 준비 완료 표시는 원본처럼 두 줄이다. 연구 슬롯의 장비 그림은 `countrytechnologyview.gui`의 중심 x252/y49에 원본 크기로 표시하며 고정 70×64 축소를 적용하지 않는다. 연구 슬롯 이름은 같은 원본의 x57/y12에 그림보다 위 층으로 표시한다. 원본 VCR02_14의 14px 행 높이로 Minecraft 글꼴을 축소하면 한글 획이 빠지므로 슬롯 제목만 기본 1배를 사용한다(GUI2에서 한글 글리프의 원본 픽셀을 유지). 공통 글꼴과 다른 화면의 배율은 바꾸지 않는다. 업체 로고와 톱니 배경은 `Slot.designTeam`이 지정된 경우에만 표시하며, 업체 없는 표시용 입력과 실제 업체 배정 상태를 구분한다. 군 카드와 새 군 카드의 초상/배경 x4.5 GUI는 정수 올림 없이 유지해 왼쪽 틈을 덮고, 카드 폭·간격·입력 영역은 유지한다. 집단군·제독의 기존 정수 좌표와 예비 함대의 소수 좌표는 유지한다.

집중 재현은 팩 빌드 후 Java25 `test runGameTest remapJar`에 `-PhoiTestSuite=doctrine`, `agency`, `design-team-candidates`, `screen-polish`, `commander-presentation`을 각각 지정한다. `HOI_DOCTRINE_GRAND_PREVIEW`에는 네 군종의 `doctrine-<folder>-nogrand-ready/nogrand-poor/grand-selected/sub-selected.json`을 두어 미선택 안내 두 원문 키와 대교리→하위 교리의 실제 응답 전이를 검사한다. `HOI_UI_REVIEW_PREVIEW`에는 같은 서버 버전이 내보낸 `doctrine-<folder>-ready/poor.json`과 `agency-cryptology-ready/decrypting.json`을 둔다. 기본 집중 검사는 기존 자동 생성 입력을 사용하며 이 추가 폴더를 지정하면 군종별 원문 툴팁·미리보기·수도 좌표도 재생한다. 합성 수도 입력은 상태/입력 경계 검사이고, 일본 수도 서버 입력의 테스트 월드 렌더도 실제 지도 지형 사진은 아니다. 운영 접속·멀티플레이·음향 직접 청취는 미수행이다.

교리의 `experience_gain_recce_combat_factor`·`experience_gain_recce_training_factor`는 원본 한국어·영어/TFR/설치 번역 패치/TFRKP에 이름 키가 없어 서버 키를 그대로 표시한다. 클라이언트는 이름이나 표시 단위를 추정하지 않는다. 단계별 0.05 및 다섯 단계 합계 0.25의 % 한 자리 서식은 서버 책임이며 서버 입력 버전별 원문을 보존한다.

통달 단계 숫자는 원본 `doctrines_view.gui/mastery_text`의 `hoi_16mbs` 숫자와 원본 사진의 실제 8px 글리프 높이를 기준으로 GUI2에서 글리프 높이 4 GUI를 사용하고 숫자 칸 안에서 가운데 정렬한다. 줄 높이가 아니라 렌더러가 측정한 숫자 경계 상자로 배율을 계산한다. 색 `#E0C956`은 원본 120506 사진의 숫자 색 대조값이며 GUI에 명시된 런타임 색 상수로 확인한 값은 아니다. 단계 값·칸 위치·간격은 유지한다. 함장 목록의 기본 휘장은 서버 `NavalFleetView.DEFAULT_FLEET_INSIGNIA=3`과 발급된 `Officer.insignia`를 따르며 예전 0번 기대값을 사용하지 않는다.

연구 슬롯 그림은 `UiAssets.technologyPath`의 국가 전용 → 공통 경로 순서로 선택한다. 한국 K-21 PIP(`mechanised_infantry3`)는 Korea Plus `TFPKP_Technologies.gfx`의 `GFX_KOR_mechanised_infantry3_medium`을 팩 `technology/country/kor/tfr/technology/mechanised_infantry3`에서 사용한다. 원본 `gfx/interface/technologies/armor/KOR_mechanized3.dds`의 크기·픽셀은 유지하며 다른 국가의 공통 그림을 덮어쓰지 않는다. 업체 비교 입력은 서버의 최신 `DesignTeam.technologies`에 KIA가 K-21 PIP·K-511에, LIG가 풍익에 허용되어야 한다. 세 슬롯 배치는 렌더용 투영이며 실제 캠페인 연구 시작이나 비용 지불을 입증하지 않는다.

최신 서버 입력의 `ResearchPayloads.MAX_JSON=600000`과 프로토콜 48개를 맞춘 뒤 `test runGameTest remapJar -PhoiTestSuite=screen-polish`로 두 해상도를 재현했다. 슬롯 제목은 GUI2에서 본문 색 경계 높이14px이고, `K105A1 풍익`의 본문 색 픽셀480개가 두 해상도에서 같은 모양이다. 밝은 장비 그림 위에서도 제목을 읽도록 슬롯 제목에만 원본 VCR02_14 사진의 검은 테두리를 재현한다. GUI 배율의 역수만큼 8방향으로 검정 글자를 그리고 본문을 다음 층에 그려 화면 1px 테두리를 유지한다. 제목의 기본 그림자는 끄며 x57/y12·1배·본문색 #E0E6E8과 그림 위 층을 유지한다. 공통 글꼴·다른 화면은 바꾸지 않으며 원본 글꼴 자체의 일치를 뜻하지 않는다. 최종 연구 집중 검사와 이전 무필터 전체 검증은 별도 단계다.

제목 테두리 재현은 같은 서버 입력의 팩 빌드 후 `test runGameTest remapJar -PhoiTestSuite=screen-polish` 한 번으로 검사했다. GUI2 두 해상도에서 직전 제목의 본문색 480픽셀은 좌표·모양·색이 같고, 8방향 인접 윤곽 692픽셀은 모두 검정이며 누락은 0이다. 본문 경계 높이14px를 유지한다. 실제 캡처272장과 제목의 최근접 4배 확대를 대조했으며 원본 사진은 기존 2560×1440 사진이고 별도 두 해상도 원작 재실행은 아니다. 클라이언트 단위150개는 재실행했고 프로토콜124개는 변경 없는 성공 결과를 재사용했다. 공통 글꼴이 변경되지 않은 집중 검증이며 이전 전체 검증과 구분한다.


### 초상 모서리·연구 단추·교리 트랙의 원본 기준

육군 상세의 세부사항/장교 단추는 `unitview.gui`의 지휘관 초상 틀 모서리를 기준으로 한다. `UiAssets.visibleBounds`가 실제 화면의 종횡비 맞춤과 알파 경계를 계산하고 `drawLeaderControlAnchored`가 단추 자체의 알파 경계를 그 모서리에 맞춘다. 상태 막대 뒤에 가려지지 않도록 단추를 뒤에 그리며, 서버 패널의 commander가 비어 있으면 두 단추를 만들지 않는다. 연구 상세의 연구 글자는 업체 그림의 폭을 빼지 않고 단추 전체에 수평으로 맞추고, 준비된 글꼴의 실제 위/아래 픽셀 경계로 수직 중심을 정한다.

교리 트랙 위쪽 선택 그림은 원본 `doctrines_view.gui`의 `subdoctrine_icon_frame` 원형 중심에 맞춘다. 통달치의 체스 그림·보상 틀·빛무리와 호버 영역은 복원된 배치를 유지한다. I~V만 최신 사용자 첨부 `1691cd28-9e77-4ac8-ab7a-f1e13a5aed3f`·`07281c87-27bd-4849-8e24-a54bdb32a6b0`처럼 체스의 좌우 아래에 교대로 붙인다. 동일한 23×23GUI 그림 칸에서 y를 12GUI 올리고 왼쪽 숫자만 x를 3GUI 옮긴다. 선택 그림의 가운데 정렬과 보상 선/문턱의 위치는 별개다. 통달치 획득 출처의 부대 그림은 서버의 `£unit_*` 토큰 하나이며 별도 NATO 도형을 그리지 않는다. 팩 importer는 원본 `noOfFrames=2`의 첫 번째 부대 그림만 가져온다.

일부 기존 숫자 표시 경로는 과거 보드 지시에 따라 미제공 값을 화면에서 0으로 대체한다. 이는 모든 unknown에 적용하는 공통 정책이 아니다. HQ 경험치·전장 너비·비용 등 — 또는 미상으로 표시하는 필드는 기존 계약을 유지하고 이번 작업에서 미제공 값을 새 0으로 바꾸지 않는다. 대상은 공군기지 수용량/연료, 지휘관 사단·비행단 수, 함대·전구 수, 국가 통계, 교리 보상 문턱, 중점 기간, 지도 상세 수량·슬롯, 세계 긴장도 인력, 부대 수량·비율, 결정 수량이다. 이는 화면 표시의 대체값이며 서버 자원·조건·전투 계산에 0을 저장하지 않는다. 국가/부대의 미확인 정체, 지원하지 않는 결정 조건과 미제공 그림의 비수치 표시는 이 규칙과 구별한다.

`-PhoiTestSuite=portrait-research`는 지휘관 단추 입력·미배정 패널·연구 상세를 GUI2 두 해상도에서 확인하는 집중 렌더 경로다. `HOI_COMMANDER_PREVIEW`에 기존 서버 원본 fixture를 지정한다. 팩 빌드 후 집중 검사는 성공했고 실제 렌더 78장을 생성했다. 무필터 전체 검증 및 실제 서버 연결·다중 클라이언트 검증은 별도이며 이번 단계에서는 수행하지 않았다.

교리는 `-PhoiTestSuite=doctrine`로 기존 진행·교체·잠금·스크롤·원문 툴팁을 검사하고, `-PhoiTestSuite=doctrine-icons`로 `HOI_DOCTRINE_GRAND_PREVIEW`의 원본 `doctrine-land-grand-selected.json`을 사용해 미래 병사 미리보기와 기계화 통달 툴팁을 두 해상도에서 캡처한다. 미리보기와 호버는 서버 변경 요청을 만들지 않는다. 집중 검사는 통과했으나 21판 서버 fixture에 들어 있는 월 비축 `?`와 일부 효과 키는 화면에서 임의 번역·계산하지 않는다. 원문 키·미제공 값의 추정 번역/계산은 하지 않는다. 후속 계약의 공작원 지도 자료, 작전별 요구량·단계·위치와 교리 완료 presentation은 아래 현행 절에 연결되어 있다. 과거 집중 결과와 후속 동작 검증을 구분한다.

정보기관 개선 완료 제목은 설치 번역 패치 `3411756715/localisation/korean/replace/TFR_replace_operatives_l_korean.yml`의 `AGENCY_UPGRADED_TITLE`과 원본 `20261031013533_1.jpg`를 따라 `첩보기관 개선 완료!`를 사용한다. 이 문구 수정 후의 실제 렌더 비교는 아직 수행하지 않았다. 무필터 전체 검증은 서버 전체 Gradle과의 중첩을 확인해 동결 소스 fixture 생성 단계에서 중단했으며 성공으로 집계하지 않는다. 다음 검증은 서버 입력 전달과 실행 시간 조율 후 수행한다. 선호 전술의 `-PhoiTestSuite=tactics` 집중 검사는 국가 GUI2 두 해상도와 기존 지휘관 선택/확정/선택됨/낮은 기술 잠금을 검사하지만 원작 지휘관 진입 장면의 시각 대조는 미수행이다.

### 상단 교리 이정표와 완료 알림

상단의 회색 그림은 이정표다. 원본 TFR `interface/doctrines/doctrines_view.gui`의 `milestone`은 `milestone_icon`과 별도 `preview_container/milestone_preview_highlight`를 사용한다. `common/doctrines/grand_doctrines/*.txt`의 `milestones`에는 대교리별 추가 효과·전술·스크립트가 있다. 원본 한국어 `doctrines_l_korean.yml`의 `DOCTRINE_MILESTONE_TITLE`·`MILESTONE_UNLOCK_EXPLANATION`·`MILESTONE_UNLOCK_HEADER`가 각각 툴팁 제목·달성 설명·완료 알림 본문의 기준이다. 설치 번역 패치에서 해당 키의 덮어쓰기는 찾지 못했다.

이정표는 `DoctrineView.Track.milestone`의 이름·그림·원문 툴팁과 `unlocked`를 표시한다. 미달성은 원본 회색 프레임1, 달성만 색상 프레임0이며 하위 교리 선택·통달치·보상 수로 달성을 추정하지 않는다. 이전 입력에 필드가 없으면 `Milestone.NONE`으로 회색을 유지한다. 대교리 후보를 미리볼 때는 `Choice.milestones`에서 해당 트랙 자료를 사용하며 확정·효과 지급 요청을 보내지 않는다. 하위 교리 후보 미리보기는 현재 서버 이정표를 유지한다. 통달치 체스 배치와 최신 사용자 첨부의 로마자 상대 배치는 유지한다. 기존 팩의 이정표 두 프레임·출처를 사용한다.

하위 교리의 선택 그림·원형/이름 틀·통달치 표시와 입력 영역은 게이지 중심을 기준으로 정렬한다. 기존 묶음에서 5GUI 왼쪽으로 이동하고 체스·로마자·게이지의 위치와 수직 간격은 유지한다. 부분 통달 게이지는 원본 텍스처를 압축하거나 뒤집지 않고 위에서 아래로 드러낸다. 이정표 계약은 `cmp86/server-input-2`의 동결본과 일치한다. 전달 목록510개와 서버 JAR의 SHA-256을 검증했으며 AgencyView도 같은 입력으로 동기화했다. `-PhoiTestSuite=doctrine-icons`의 합성 false/true 이정표·원문 전달·반복 클릭 검사는 화면 분기를 검증하며 서버 보너스 지급을 증명하지 않는다.

팩 선빌드 뒤 Java25 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`가 통과했다. 이전 입력1의 고정 서버 소스를 별도 사본으로 복사하고 `DoctrineView`만 새 서버 계약으로 동기화해 기존 교리 입력10개를 다시 생성했다. 최신 입력2의 전체 검증 결과로 집계하지 않는다. GUI2 2560×1440·1280×720에서 네 군종의 부분 채움 위쪽 불투명 픽셀은 원본과 일치하고 아래쪽은 비어 있다. 선택 틀의 10화면픽셀 이동과 게이지 중심 일치, 미달성 회색8곳 및 합성 달성/미달성 프레임4곳을 확인했다. 합성 이정표 툴팁은 전달 보존을 검사하기 위해 기존 후보 툴팁을 사용했으므로 실제 이정표 문구 검증이 아니다. 클라이언트150·protocol124개 단위 검사는 실패/오류/건너뜀0이며 변경 없는 성공 결과 재사용을 포함한다. client-only JAR에는 허용된 mod-list icon 외의 이미지·음원이 없다. 이 문단의 합성 검사는 아래 입력2의 실제 이정표 fixture 검사와 별개다. 최종 무필터·운영 연결·멀티플레이·직접 음향 청취는 미검증이다.

대교리가 없거나 다른 대교리를 미리볼 때는 `Choice.previewTracks`의 순서대로 `DoctrineView.previewTracks`에서 트랙을 찾아 본문 전체를 교체한다. 원본 배경·테두리·장식·하위 교리 선택 틀·통달치 바탕은 기존 원본 buttonstate 셰이더 기반 회색 처리로 표시하며, 특수부대 배경의 상단 clipping과 원본 비율도 유지한다. 이정표는 후보의 `Choice.milestones`를 사용한다. 후보에 없는 현재 트랙을 남기거나 현재 교리의 선택·진행·보상을 후보에 복사하지 않는다. 미리보기 트랙의 하위 교리 미선택/보상 빈 목록은 서버 그대로 표시하며 첫 하위 교리를 임의로 선택하지 않는다. 현재 선택된 대교리 자체를 다시 누르면 실제 서버 트랙의 달성 상태와 색을 보존한다. 선택창 밖의 휠은 트랙/이정표 가로 이동에, 선택창 안의 휠은 후보 목록에 적용한다. 이정표 툴팁 위에서는 원문 스크롤을 우선한다. 창을 닫으면 현재 서버 이정표로 복원하며 미리보기·좌우/반복 클릭은 교리 선택 요청을 만들지 않는다.

입력2 재현은 팩 선빌드 뒤 `HOI_DOCTRINE_MILESTONE_PREVIEW=<server-input-2>/fixtures/commander-source-fixtures`와 기존 `HOI_DOCTRINE_GRAND_PREVIEW`를 지정하고 Java25 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons -PhoiServerSource=<검증된 고정 서버> -PhoiProtocolSource=<같은 서버>/protocol/src/main/java --console=plain --no-daemon`으로 실행한다. 미선택·대교리 선택·보병 진행·보병 달성/다른 트랙 미달성 및 모든 대교리 후보의 원문 툴팁을 GUI2 두 해상도로 확인한다. 마지막 트랙 접근·후보 닫기 복원을 포함하며, 현재 트랙 일부가 없는 합성 입력은 이정표 위치 겹침만 검사한다. 이 검사는 서버에서 생성한 상태의 렌더 재생이며 게임 진행이나 보너스 지급의 실접속 검사가 아니다.

입력2 동결본의 실제 이정표 집중 검사는 팩 선빌드 뒤 Java25에서 통과했다. 원본 프레임 렌더34곳·RGBA/출처/출력 해시4개와 두 해상도×선택/진행/달성의 기존 트랙 본문 픽셀 동일6개를 확인했다. client150·protocol124는 실패·오류·건너뜀0이며 변경 없는 성공 결과 재사용을 포함한다. client-only JAR은 모드 목록 아이콘 외 이미지·음원을 포함하지 않는다. 수정 전 소스의 별도 사본에서도 같은 입력으로 미선택 후보 누락·후보 밖 기본 그림 잔존·마지막 이정표 접근·누락 트랙 배치 회귀를 재현했으며, 그 예상 실패를 최종 소스의 통과와 구분한다. 동일 진행 상태의 원작 전체 화면 미제공, 무필터 전체·운영 접속·멀티플레이·직접 음향 청취 한계는 유지한다.

전체 트랙 미리보기 재현도 팩 선빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons -PhoiServerSource=<검증된 고정 서버> -PhoiProtocolSource=<같은 서버>/protocol/src/main/java --console=plain --no-daemon`을 사용한다. `HOI_DOCTRINE_MILESTONE_PREVIEW`에는 `kor-doctrine-preview-{land,naval,air,special_forces}.json`과 이정표 네 상태가 모두 있는 서버 입력을 지정한다. 입력3 전달 전 보존한 서버 fixture 8개와 `protocol-next` 사본으로 GUI2 두 해상도에서 11개 대교리 후보의 순서·마지막 트랙 접근·선택 요청 차단·닫기 복원을 검사했다. 별도 수정 전 사본에서 비대칭전의 기존 트랙 잔존과 미선택 특수부대의 전체 트랙 누락을 재현한 뒤 수정 후 집중 검증이 통과했다. 서버 full4나 입력3 전체 통과로 집계하지 않는다.

원본 배경 네 종류의 RGBA·출처 해시와 실제 렌더 22곳(다른 후보 회색16곳·현재 후보 유지6곳), 기존 선택/진행/달성 상태의 화면6개를 대조했다. 회색 픽셀은 원본의 반투명 알파와 화면 바탕색까지 합성해 검사한다. 최초 독립 검사기는 불투명 픽셀만 기대해 실패했으며 실제 원본 알파를 반영한 뒤 통과했다. 현재 후보 및 기존 진행 상태의 비교 영역은 변경 픽셀0이다. client150·protocol124는 실패·오류·건너뜀0이고 변경 없는 성공 결과 재사용을 포함한다. client-only JAR의 정적 자산 경계도 통과했다. 같은 후보 전체 회색 미리보기의 원작 사진은 미제공이며 운영 접속·다중 클라이언트·직접 음향 청취와 최종 무필터 검증은 남아 있다.

`doctrine_reward_complete`와 `doctrine_milestone_complete` 알림은 각각 보상 알림과 중앙 확인 창으로 동시에 표시한다. `-PhoiTestSuite=doctrine-notifications`는 `HOI_COMPLETION_PREVIEW` 또는 번들 원본 fixture로 GUI2 두 해상도를 검사한다. 작은 화면에서 창이 겹치는 부분은 앞 이정표 창이 입력을 받으며 노출된 보상 영역은 보상 알림이 받는다. 같은 알림의 새 리비전만 확인 대기를 해제한다. 긴 본문 검사는 원문을 반복한 합성 fixture임을 캡처 이름에 표시하며 실제 보너스나 서버 달성 검증과 구분한다. 초기 집중 실행은 가려진 좌표의 입력 소유자를 잘못 기대해 실패했다. 수정 후 같은 토큰의 새 리비전/다른 토큰/낡은 응답, 앞뒤 창 입력, 오른쪽·반복 클릭, 긴 본문 스크롤·드래그 해제 검사는 GUI2 두 해상도에서 통과했다. 무필터 전체 검증은 아직 남아 있다. 실제 운영 연결·다중 사용자·직접 효과음 청취도 미수행이다.

교리의 군종 색은 원본 `common/doctrines/folders/doctrine_folders.txt/color_frame`의 육군1·해군2·공군3·특수부대4를 사용한다. 대교리/하위 교리 틀과 보상 틀, 서버가 발급한 이정표/트랙 그림의 군종별 원본을 보존한다. 완료된 통달치 문턱은 `GFX_mastery_amount_frame`의 군종 색 프레임, 미완료 문턱은 무색0을 사용한다. 게이지 채우기는 `GFX_mastery_progress_bar_animated_{land,naval,air,special_forces}`의 24프레임을 원본 `doctrines.gfx`의 24fps·1초 반복으로 표시한다. 게임 시간·서버 통달치와 독립된 표시 시계이며 숫자의 노란색은 유지한다. 잠긴 체스/이정표의 회색 상태는 군종 색과 별개다. 특수부대 트랙 사진 `GFX_paratroopers_bg`·`GFX_marine_bg`는 원본 자체가 회색이므로 사용자 색상 지시에 따라 렌더링 시 노란색을 곱한다. 색 `#FFDC66`은 원본 노란 통달치 배경(frame_4) 중심 RGB(206,178,82)의 최대 채널을 255로 정규화한 값이며 원본 엔진 상수로 확인한 값이 아니다. 원본 파일·픽셀·종횡비와 다른 군종의 이미 입혀진 색은 보존한다. `doctrine-icons` 집중 묶음은 네 군종의 서버 원본 선택 상태와 색상·완료/반채움 확인을 위한 합성 진행 상태를 두 해상도로 캡처한다. 합성 진행은 실제 서버 달성·보너스 검증이 아니다.

군종 색/로마자 집중 재현은 팩 선빌드 후 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`다. 보관한 수정 전 소스의 별도 사본과 수정 후 모두 통과했고, 보상 틀 5곳의 좌표 유지 및 로마자 5곳의 지정 이동을 실제 렌더 픽셀로 확인했다. 네 군종×두 해상도의 게이지 불투명 픽셀은 각각 원본 첫 프레임과 일치한다. 군종 색상 검사의 합성 진행 상태는 아래 실제 이정표 fixture와 구별하며 최종 무필터/운영 접속 검증을 대신하지 않는다.

특수부대의 `GFX_paratroopers_bg`·`GFX_marine_bg`는 TFR 원본 464×290 가로 그림이다. 세로 트랙 칸에 축소·중앙 배치하지 않고 `doctrines_view.gui`의 `track_bg_container` 여백과 `track_bg_image` x=-15/y=0에 따라 상단에 둔다. 기존 1/2 GUI 배율로 232×145GUI 원본 비율을 유지하며 트랙 안쪽에서만 잘라 그린다. 원본 밖의 아래 영역을 늘리거나 반복해 채우지 않는다. 사용자가 지정한 노란색, 다른 군종 배경, 이정표·하위 교리 틀·게이지·체스·로마자 위치는 유지한다. 두 PNG는 원본 DDS와 RGBA 바이트·입출력 해시가 일치하며 재추출이나 자산 변경 없이 기존 파일을 사용한다.

특수부대에서 다른 트랙에 이미 선택된 같은 하위 교리는 서버 `Choice.enabled=false`·`requirementsMet=false`를 따른다. 미리보기는 가능하지만 잠금 해제 요청을 보내지 않으며 서버의 거절 툴팁을 그대로 표시한다. 해당 선택 규칙은 이번 배경 수정에서 바꾸지 않았다. `doctrine-icons`는 기존 서버 `doctrine-special_forces-sub-selected.json`으로 두 번째 트랙의 중복 후보 좌/우·반복 클릭 차단, 다른 허용 후보의 정확한 트랙 ID·세션·리비전 요청 1회, 낡은 응답 후 잠금 유지를 두 해상도에서 검사한다. 같은 이름인 네 트랙을 테스트에서 이름만으로 선택하지 않는다.

특수부대 배경 수정 후 팩 선빌드와 Java25 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`가 통과했다. 원본 출처·픽셀 검사2개, 두 해상도×배경2종의 실제 렌더 상단 픽셀 검사4개, 육군·해군·공군의 이전 화면과 패널 픽셀 동일 검사6개를 통과했다. 단위 검사 client150·protocol124는 실패/오류/건너뜀0이며 변경 없는 성공 결과 재사용을 포함한다. client-only JAR에는 허용된 mod-list icon 외의 이미지·음원이 없다. 독립 증거 검사기의 첫 실행은 화면 y좌표와 허용 아이콘 경로를 잘못 기대해 실패했고, 실제 배치와 기존 JAR 메타데이터로 정정했다. 실제 렌더/입력 검사는 첫 실행에 통과했다. 특수부대 트랙이 열린 원작 전체 사진은 미제공이며 원작 시각적 완전 일치·새 서버 입력2·무필터 전체·운영 접속·멀티플레이·직접 음향 청취는 미검증이다.

육군 경험치를 쓰는 교리 `잠금 해제` 버튼은 `Font.prepareText`가 반환하는 아이콘·문구의 실제 렌더 경계 중심을 맞춘다. 문구와 전체 묶음의 기존 가로 위치·배율·버튼 배경은 유지하고 아이콘만 세로로 보정한다. 적용 대상은 육군·특수부대의 육군 경험치 버튼이며 다른 군종의 경험치·툴팁 글리프와 공통 숫자 기준선은 그대로다. 원본 TFR `GFX_army_experience`의 `gfx/texticons/army_experience_20x20.dds`와 기존 외부 팩 PNG의 RGBA·입출력 해시는 일치한다.

이 보정은 팩 선빌드 후 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`로 재현한다. GUI2 두 해상도의 실제 캡처에서 육군 아이콘은 아래로1px, 특수부대 아이콘은 아래로2px 이동했으며 문구·버튼 픽셀은 동일하다. 원문 비용의 폭에 따른 기존 배율 차이를 유지하므로 이동량은 고정 픽셀 상수가 아니다. 비활성 특수부대 버튼의 2560 캡처도 같은 이동을 확인했고, 1280의 중복 거절 장면은 툴팁이 버튼을 가려 아이콘 시각 대조에서 제외했다. 기존 좌/우·반복 입력 차단, 허용 선택 요청1회와 낡은 응답 차단 검사는 통과했다. client150·protocol124는 실패/오류/건너뜀0이며 UP-TO-DATE 재사용을 포함한다. client-only JAR의 외부 자산 경계도 확인했다. 이 집중 검사와 기존 서버 fixture 재생은 새 서버 입력2·무필터 전체·운영 접속·멀티플레이 검증을 대신하지 않는다.


### 원본 작전 목록과 진행 상세

작전 목록은 `OperationOffer.item`을 발급된 Item.id와 그대로 연결한다. 원본 작전 id를 분해하거나 예전 enum 이름으로 바꾸지 않는다. 필요 요원·정보망·기간은 `required`, `network`, `days`를 표시한다. 진행 중 항목은 `Operation.item`으로 상세를 열고 `total - days`를 남은 일수로 표시한다. 목록의 미제공 숫자는 기존 보드 지시에 따른 0이며 서버 값을 생성하거나 저장하지 않는다.

상세의 배경·머리줄·대상 영역·클립보드는 TFR `operationoverview.gui/operation_overview_window`의 780×560 원본을 GUI2에서 절반 크기로 표시한다. 단계 그림·이름·설명은 `Operation.phases`의 원본 순서 그대로 쓰고, 원본 `GFX_phase_*` 그림과 작은 아이콘을 팩에서 읽는다. 준비 전 `OperationOffer`에는 단계 자료가 없으므로 임의의 침투/수행/탈출 그림을 넣지 않는다. 배정 초상은 같은 응답의 공작원 id를 연결한다. 본문이 넘칠 때만 원본 크기 스크롤바를 표시하며 휠·화살표·손잡이와 제목줄 드래그를 지원한다. 단계별 완료 상태가 없는 계약으로 완료나 결과 발생을 추정하지 않는다.

요원 선택은 서버 후보만 쓰며 같은 요원을 중복 제출하지 않는다. 준비/취소는 발급된 id와 현재 세션·revision으로 한 번 요청하고, 비활성·우클릭·반복 입력을 차단한다. 갱신으로 매개변수 수가 변하면 선택 목록도 새 계약에 맞춰 조정한다. 국가 권한 회수는 상세창도 닫는다. Minecraft 26.3의 `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`를 사용한다.

재현은 팩 선빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=agency-operations`에 `HOI_AGENCY_OPERATION_PREVIEW=<동결 commander-source-fixtures>`를 지정한다. `agency`와 전체 검사에서도 이 입력을 주면 같은 작전 검사를 실행한다. 수정 전 비교 사본의 `HOI_CAPTURE_BEFORE=1`은 캡처용이며 새 동작 통과 근거가 아니다. 서버 입력3 실제 자료와 표시용 Minecraft 테스트 월드의 렌더/모의 전송을 사용하며 운영 접속·멀티플레이·실제 작전 완료·직접 음향 청취와 구별한다.

입력3의 일부 설명에는 `[From.GetAdjective]`·`[From.GetNameDef]` 같은 미해석 서버 현지화 스코프가 남아 있다. 클라이언트가 임의 국가명으로 치환하지 않으며 서버의 해결된 문구를 받아야 한다. 지도 작전의 완료일·클릭 안내는 입력3의 완료 날짜와 원본 번역 패치 키로 구성하며 별도 서버 필드를 기다리지 않는다. 공작원 지도·경제위기·포획 화면의 동작과 검증 범위는 아래 해당 절에서 구분한다.

### 교리 미리보기의 창 이동과 하위 교리 열람

교리 본문 배경은 TFR `doctrines_view.gui/doctrine_folder_view`의 `GFX_tiled_bg` → `GFX_tiled_window_transparent` → `GFX_tiled_generic_overlay_bg1` 순서로 그린다. 회색 입체 외곽 틀은 첫 192×192 DDS에 포함되어 있으며 별도 창이나 임의 선을 만들지 않는다. 원본 borderSize 64/1/268과 첫째·셋째 레이어의 중앙 반복을 유지한다. 원본 픽셀 좌표에서 그린 뒤 0.5배로 변환해 1px border를 GUI 정수로 올림하지 않는다. 패널은 x0, `top + 20`부터 화면 아래까지이며 탭과 닫기 X, 본문 트랙 위치·간격은 유지한다. 트랙/이정표의 좌우 clip은 원본15px margin을 GUI 정수8로 올림한 안쪽이며, 이정표 상단은 `top + 28`이다. 이는 `tracksTop - 20` 후광을 자르지 않는다. 가로 스크롤한 트랙이 외곽 틀을 덮지 않도록 clip만 제한한다.

외곽 틀은 입력3으로 팩 선빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`를 실행하여 실제 렌더했다. GUI2 2560×1440·1280×720의 전체 프레임과 좌상 탭 접합·우상 X·좌하·우하에서 메탈 틀을 확인했다. 1280에서도 틀은 화면 안에 있고 스크롤한 트랙은 안쪽에서 잘려 틀을 덮지 않는다. 원본2560 사진을1280 비교용으로 축소한 열은 원본1280 실행 증거가 아니다. 특수부대 원본과 육군 fixture의 교리·메달·열 수 차이는 외곽 틀 비교와 구분한다. 집중 검사와 무필터 전체, 실제 운영 접속·멀티플레이는 별개다.

하위 교리 후보의 후광은 `gfx_ongoing_generic_glow_yellow/frame_0` 한 장만 사용한다. 최신 사용자 요청에 따라 위에서 j번째 보상은 600×j ms 뒤에 시작하며 600ms 동안 선형으로 나타난다. I부터 V까지 앞 후광이 완전히 켜진 뒤 다음이 시작한다. 원본의 150ms 지연·3000ms fade를 그대로 재현하는 동작과 구분한다. 다른 후보 선택·같은 후보 반복 클릭·재열기는 다시 시작하고 창 이동·크기 변경은 진행을 유지한다. 후광의 위치·크기와 보상 틀 아래 그리기 순서는 유지한다. `doctrine-icons`의 `hoi-doctrine-preview-glow-{0,300,600,900,1500,2100,2700,3000}ms-{2560,1280}-gui2`는 실제 렌더 경로에 고정 시계를 주입한다. 시간대 비교는 보상 가림을 피하려고 선택 창을 오른쪽으로 옮기며, `hoi-doctrine-future-soldier-*`는 원래 보병 열 옆 위치를 유지한 별도 장면이다.

대교리 미리보기의 이정표는 같은 첫 프레임을 원본 1.5배로 그리며 400ms 동안 함께 켜진다. 이정표와 후광은 트랙 본문의 `top + 95` 클리핑을 끝낸 뒤 별도 패널 내부 클리핑으로 그린다. 입력2의 상단 잘림 검증은 `top + 24`였으며 외곽 틀 추가 소스는 위에서 설명한 `top + 28`을 사용한다. `tracksTop - 20`에 있는 후광 상단 15GUI가 잘리지 않도록 하며 원본 위치·크기와 트랙 본문 영역은 유지한다. 이정표 아이콘의 원본 두 번째 프레임 아래에 놓고 하위 교리 후보의 순차 후광과 시계를 분리한다. `hoi-doctrine-milestone-glow-{0,200,400}ms-*`가 검사 장면이다. 입력2 동결본의 `doctrine-icons` 집중 검사와 GUI2 두 해상도 실제 캡처에서 상단 잘림 해소를 확인했다. 대조 원본 `20261001154308_1.jpg`는 특수부대 확충 네 열, 검증 fixture는 육군 여섯 열이므로 교리·메달·열 수가 같은 장면의 일치 판정은 아니다. 클리핑 수정 전 무필터 전체와 수정 후 집중 결과를 구분하며 운영 접속·멀티플레이·직접 음향 청취는 미수행이다.

TFR `interface/doctrines/doctrines_view.gui:600`의 `doctrine_selection_list`는 560×576이며 `click_to_front = yes`, `moveable = yes`를 선언한다(608~609행). 클라이언트는 기존 크기·내용 간격을 유지하고 제목줄을 끌어 창을 옮긴다. 화면 가장자리에서 창을 제한하며, 이동을 끝내거나 선택창을 닫으면 드래그를 해제한다. 작은 화면에서는 선택창 이동과 기존 트랙 가로 스크롤을 함께 사용해 가려진 트랙을 열람한다. 창 안의 빈 공간·비활성 단추·우클릭은 뒤 트랙으로 전달하지 않는다.

다른 대교리 미리보기의 하위 교리 칸은 서버 `previewTracks[].candidates`를 기존 선택창으로 연다. 후보 설명·고급 세부 사항·보상별 필요 통달치·효과는 서버 원문 그대로 표시하고, 선택 그림·트랙·보상 그림은 회색으로 그린다. 실제 대교리의 선택·통달·달성 상태를 복사하지 않는다. 읽기 전용 하위 교리의 잠금 해제는 항상 비활성이며 닫으면 보던 대교리 후보로 돌아간다. 대교리 선택창까지 닫으면 실제 선택 상태로 복원한다.

원본의 `grand_doctrine_icon`·`grand_doctrine_name` 선언은 같은 파일 110~127행에 있지만 동적 미리보기 때 이름표를 바꾸는 엔진 동작은 선언만으로 확인되지 않는다. 상단 이름표는 현재 선택 대교리를 유지한다. 후보 이름·그림은 선택창에 표시하며 상단 전환의 원작 동작은 관측 대기다.

입력3 전체 검증에서 합성 숫자 배치 fixture가 공통 `previewTracks`를 누락하여 `Unknown doctrine preview track`으로 실패했다. 합성 화면이 수정하지 않는 목록도 원본 응답에서 보존하도록 수정한다. 이 실패는 프로토콜 검증을 약화하거나 참조를 삭제해 우회하지 않는다. 재현은 `HOI_DOCTRINE_MILESTONE_PREVIEW=<입력3 commander-source-fixtures>`를 지정한 `-PhoiTestSuite=doctrine-icons`이며, 창 이동·해제·하위 후보/보상 열람·잠금 해제 요청 차단·원래 후보 복원을 두 해상도에서 검사한다. 전체 검증과 실제 연결·멀티플레이 결과는 별도로 기록한다.

교리 검토 수정의 집중 검사는 팩 선빌드 뒤 Java25 `test runGameTest remapJar -PhoiTestSuite=doctrine-icons`로 통과했다. 실제 11개 대교리 후보와 두 해상도에서 창 이동·읽기 전용 하위 교리·첫 보상 원문·선택 요청 차단을 확인했다. 같은 이름을 가진 마지막 보상을 집던 최초 검사 오류는 첫 보상을 대상으로 바로잡고 재검증했다.

### 공작원 카드·정보망과 작전 지도

공작원 선택 도구 `hoi:map/selector/operatives`와 배정 국가가 유효한 atlas에서 `AgencyView.operatives`를 아래 카드 줄과 선택된 왼쪽 위 배지에 표시한다. 공작원 카드 줄·수용량·임무 진행 막대는 육해공 카드처럼 왼쪽 패널이 열려도 오른쪽으로 밀지 않고 화면 전체 폭 기준으로 가운데에 둔다. 작전 대상 선택 단추만 기존처럼 왼쪽 패널 오른쪽에서 시작한다. 카드 좌클릭은 선택/해제하고 우클릭은 선택하지 않는다. 입력은 Minecraft 26.3 `InputConstants.MOUSE_BUTTON_LEFT`를 사용한다. 미배정·권한 회수·차원 전환에서는 사적인 응답과 입력 영역을 버린다. 기관 창이 열리면 그 창의 발급 세션을 사용한다.

배치·배경 근거는 TFR `interface/operativeleader.gui`의 80×150 카드 슬롯, `operative_leader_portrait_window`와 `operative_badge_view`다. 초상은 발급된 TFR/TFRKP 경로만 사용한다. 카드 배경·배지·모집 틀은 등록된 `/frame_0`을 참조한다. `GFX_operative_frame` 원본은 173×117, 두 프레임이므로 보존한 atlas를 86/87픽셀 경계로 나누어 선택 여부를 표시한다. 서버에 없는 계급·별명은 만들어 넣지 않는다. 현재 작전 단계는 아래의 서버 Progress 계약으로만 연결한다.

선택 배지(`operative_badge_view` 437×109, GUI2 0.5배)의 기관 문양은 원본 `agency_insignia` x2/y0·0.9배(119px 문양 → 54GUI), 국적 국기는 `nationalities_grid`(x324/y34, 오른쪽 정렬)의 `GFX_flag_small` 크기(26×21 → 13×10GUI)를 x201/y18에, 특성 줄은 `traits_grid` y56(→ y28)에 둔다(사용자 지시 2026-11-19). 별명·이름·계급 글자는 원본 `VCR02_12`(약 12px)보다 크지만 GUI2에서 획이 깨지지 않는 최소 크기 규칙에 따라 Minecraft 기본 크기와 기존 10GUI 줄 간격을 유지한다.

정보망은 `Network.segments`의 발급 선분을 그대로 사용하며 로드된 지면 위에 흰 점선으로 표시한다. 원본 `gfx/maparrows/maparrows.txt`의 `intel_network_edge`와 `intel_network_mask.dds`가 마스크·7회 반복·폭 배율의 근거다. 흰색은 사용자 화면 요구이며 원본 황색 패턴의 색을 복제한 것이 아니다. 원본의 시간 애니메이션과 엔진 카메라별 폭 변환까지 동일하다는 검증은 하지 않았다. 끝점 청크가 로드되지 않은 선분은 그리지 않는다. 네트워크는 화면 뷰이며 저장된 영역을 만들거나 바꾸지 않는다.

작전 지도는 `Operation.item/mapIcon/province/x/z`로 원본 작전 그림을 배치하고 실제 `total-days`를 표시한다. 원본 `mapicons.gui:2088`의 `operation_map_icon_entry`와 `20261031013440_1.jpg`가 근거다. 좌클릭은 발급 item의 기존 작전 상세만 열며 게임 동작 요청을 만들지 않는다. 구형 응답에 현재 단계가 없으면 단계별 그림·원호를 표시하지 않는다. 새 Progress 응답의 연결은 아래 작전 단계 절을 따른다. 새 서버 응답의 `Operation.detail`이 있으면 원문·색·문단을 그대로 표시한다. 이 필드가 없는 구형 응답만 서버 `completes`와 설치 번역 패치 `3411756715/localisation/korean/replace/TFR_replace_operatives_l_korean.yml`의 `OPERATION_ONMAP_STATUS_IN_PROGRESS`, `OPERATION_ONMAP_CLICK_FOR_DETAILS`로 기존 툴팁을 조합하며 날짜가 비면 완료 문장을 생략한다. 국가 scope는 서버가 확인된 `Root`/`From`만 치환하며 클라이언트는 미확인 함수를 추정하지 않는다.

집중 재현은 입력3 `commander-source-fixtures`를 `HOI_AGENCY_OPERATION_PREVIEW`로 지정한 `-PhoiTestSuite=agency-map`이다. GUI2 두 해상도에서 실제 공작원 3명·작전 1개·정보망 선분 4개, 카드 선택/해제·우클릭·동일 revision·작전 상세 진입·권한 회수·원본 그림 존재를 검사한다. 테스트 월드의 지면 위에 서버 좌표를 재생하며 실제 atlas 접속·멀티플레이·직접 소리 청취와는 다르다. 수정 전 캡처의 `HOI_CAPTURE_BEFORE=1`은 결함 상태를 촬영하기 위한 경로이고 회귀 통과를 뜻하지 않는다.


### 경제위기 결정과 포획 지휘관 입력 대조

입력3 `decision-ui/kor-economic-crisis-pages.json`과 metadata는 국회 다음에 `KOR_new_econimic_crisis_category`를 보낸다. 클라이언트는 이 순서·원문 설명·그림·발급된 결정 줄을 기존 결정 화면으로 표시한다. 제공 원작 사진의 강도 높은 구조조정·긴급 유동성 공급·기준 금리 인하와 fixture의 제2차 외환위기 대응은 서로 다른 진행 상태다. 원작 사진에 맞추려고 발급되지 않은 결정을 추가하지 않는다.

`-PhoiTestSuite=economic-captivity`는 `HOI_DECISION_PREVIEW=<입력3 decision-ui>`, `HOI_CAPTIVITY_PREVIEW=<입력3 captivity-source-fixtures>`를 사용한다. 경제위기 위치·설명/결정 툴팁·스크롤·원본 그림과 포획 목록 6상태×두 해상도의 초상 존재, 검색·정렬·좌우 탭 진입·반복 요청 차단·권한 회수를 확인한다. 팩 선빌드 뒤 집중 검사는 통과했으며 경제위기 그림 누락은 0개다. 첫 검사의 화면 밖 범주 호버 기대는 해당 범주로 스크롤한 뒤 검사하도록 정정했다.

보관 소스의 동일 fixture 실행도 통과했다. `DecisionScreen`, `DecisionText`, `DecisionPanel`, `CommanderCaptivity` 소스는 두 실행에서 바이트 단위로 같으며 이 단계는 화면 구현 변경이 아니라 입력3 연결 대조다. 별도 캡처의 배경·애니메이션 차이를 구현 변경으로 집계하지 않는다. 포획된 지휘관은 서버 `Row.portrait`를 사용하며 실제 전투 포획을 이 검사에서 발생시키지 않았다. 제공 원작 `20261031014531_1.jpg`는 장교단 진입 버튼의 배치 참고이며 포획 목록 전체 원작 사진은 미제공이다.


공작원 카드의 상태 표시도 발급 응답만 따른다. 진행 작전의 `Operative.operation`이 `Operation.id`와 연결되면 원본 `active_operative_operation_anim`과 해당 작전 그림을 표시한다. 현재 한국어 응답의 `status="포획됨"`은 원본 `leader_portrait_captured` 철창으로 표시하고 활동 화살표를 그리지 않는다. 다른 활동 카드는 기존 임무 표시를 유지한다. GUI2 두 해상도에서 포획 철창 불투명1820픽셀 각각은 원본 RGB와 오차1 이내100%로 일치했다. 지도 툴팁 원본 금색/초록색도 실제 픽셀로 확인했다. 관련 그림·초상14개의 RGBA 및 입출력 해시 대조와 최종 `agency-map` 집중 검사는 통과했다.

전체 `all`/`decisions` renderer 입력 명세도 경제위기 pages/metadata를 포함한다. `HOI_DECISION_PREVIEW` 환경변수만 지정하면 전체 검증에서 생성 경로로 덮어써지므로, `gradle/fixture-groups.gradle`에 두 파일을 추가해 원본 producer 실행·내용 해시·출처 검증에 포함한다.


### 입력3 전체 클라이언트 검증 경계

검증된 입력3의 동결 서버 소스·프로토콜·fixture를 사용하고 팩을 먼저 빌드한 뒤, Java25에서 `test runGameTest remapJar`를 suite 필터 없이 실행했다. `hoiServerSource`·`hoiProtocolSource`는 동결 `cmp50-server/HOI`와 그 protocol 소스를 가리켰으며 TMP/TEMP는 run 밖의 지속 경로를 사용했다. 최종 실행은 2264.908초에 성공했다. JUnit client 150·protocol 124개, 실패/오류0, 건너뜀은 각각 0/0개다. 변경 없는 성공 결과 재사용을 포함한다. 전체 실제 렌더 PNG는 4152장이고 입력3 공유 프로토콜 48개가 일치했다. staged JAR의 environment는 client이며 포함 이미지·음원은 모드 목록 아이콘뿐이다.

이전 전체 실행의 합성 교리 참조 누락 실패와 후속 수정 반영을 위한 중단 두 건은 최종 성공에 합산하지 않는다. 실제 운영 서버 접속·다중 클라이언트·음향 직접 청취는 하지 않았으며 렌더 검사는 mock 연결을 사용한다. 전체 클라이언트 통과는 미전달 서버 변경의 검증이나 원작 사진이 없는 화면의 완전 일치를 뜻하지 않는다.


### 입력4 전체 클라이언트 검증 경계

검증된 입력4를 `hoiServerSource`·`hoiProtocolSource`로 지정하고, 팩 선빌드 뒤 Java25에서 suite 필터 없는 `test runGameTest remapJar --console=plain --no-daemon`을 실행했다. 지속 TMP/TEMP 실행의 종료 코드는0이며 Gradle은38분43초에 성공했다. JUnit client150·protocol124개, 실패/오류0·건너뜀0이며 변경 없는 성공 결과 재사용을 포함한다. 실제 렌더 PNG는4152장이다. 입력4 파일1897개·공유 프로토콜48개·팩 분할ZIP6개의 해시가 검증 전 값과 일치한다. staged 클라이언트 JAR과 포함 protocol JAR에는 각각 모드 목록 아이콘만 있으며 환경은 client다.

이번 전체 실행은 자동 생성11그룹·107개 입력의 소스/fixture 출처 해시를 확인했다. 과거 `armyHud` manifest는 이번 실행에서 사용하지 않았으므로 입력4 생성 자료로 집계하지 않는다. 전체 실행의 지휘관·장교단·계획 등 수동 경로는 `run-full-input4.ps1`에 기록된 이전 동결 자료를 유지하며, 모든 화면 입력을 입력4에서 새로 생성했다고 주장하지 않는다. 작전·공작원과 교리 이정표는 입력4의 실제 fixture를 사용한다. 재현은 같은 수동 입력 환경과 `-PhoiServerSource=C:/HOI/build/cmp87-handoff/server-input-4/HOI -PhoiProtocolSource=C:/HOI/build/cmp87-handoff/server-input-4/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`을 지정한다.

확인된 Root/From 국가 치환과 서버 `Operation.detail` 표시를 포함한 전체 검증이다. 소스는 집중 비교 이후 바꾸지 않았으며 기존 GUI2 두 해상도 비교6장을 유지한다. 운영 접속·멀티플레이·직접 음향 청취는 미수행이다. 입력4 검증 시점에는 공작원 계급/별명·현재 작전 단계/진행 원호·상륙 지도 실행 단추·대교리 머리 칸 원작 관측이 남아 있었다. 이후 승인된 상륙 지도와 작전 단계의 구현·검증은 아래 절에 구분한다. 이 기록 이후 기술·계급·경험치 표시는 서버 필드로 연결되었으며 별명과 대교리 원작 관측은 계속 미확인이다.

## 상륙 계획별 지도 입력

`CommanderNavalInvasion`은 현재 국가·차원·명령 token/revision에 일치하는 계획만 받습니다. 군 카드에서 Shift+실행을 누르면 원본 `UNITS_VIEW_BTN_EXECUTE_POINT_OUT`의 계획 선택 모드로 들어갑니다. 지도에서 선택한 계획만 예약하고, 이미 예약됐으며 서버가 준비됐다고 응답한 계획은 출항시킵니다. Shift+중지 뒤 선택은 예약 해제 또는 이동 중 철수입니다. 일반 실행/중지는 기존 육군 동작을 유지합니다. 선택 모드는 오른쪽 클릭·Esc·창 닫기·선택 변경으로 해제되며 연속 입력은 기존 revision 응답 대기를 따릅니다. 원본 자동 출항 예약 전체를 구현한 것은 아니므로 예약 후 준비가 끝나면 사용자가 출항을 다시 선택합니다.

`NavalInvasionOverlay`는 서버 출발/목표 좌표와 로드된 지면 위에 원본 `invasion` 마스크·무늬를 표시합니다. 군 색상은 기존 `Panel.armyColor`를 쓰고 원본 `maparrow.shader`의 채도 1.5배를 적용합니다. 군 색상이 없는 응답은 원본 무늬를 그대로 표시합니다. 원본 body UV 0.41, head 길이 9와 atlas 4:1 좌표 배율을 사용합니다. 현재 경로는 두 끝점을 잇는 선이며 원작 spline·지형 조명·안개·선택 점멸·시간 무늬 애니메이션과 완전 일치한다고 주장하지 않습니다. 바다 이동 경로·실제 수송은 기존 서버 실행 상태가 결정합니다.

팩 선빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=naval-invasion-map`으로 실제 GUI2 두 해상도를 검사합니다. 군 카드는 현재 서버 `CommanderHudDetailsTest`가 생성하며 상륙 두 계획의 위치·단계는 입력 검사용 합성 응답입니다. `NavalInvasionOrdersTest`는 실제 군 카드의 좌/우·Shift 입력, 특정 화살표 요청, 연속 입력 차단, 오래된 revision, 미준비 출항 차단, 예약 해제·출항·철수, 국가 회수를 검사합니다. 이 검사와 서버 저장/권한 검사는 운영 접속·멀티플레이·직접 음향 청취를 대신하지 않습니다. 최종 전체 검증과 원작/수정 전/후 비교는 별도로 기록합니다.

## 작전의 서버 단계와 지도 진행 원호

`AgencyView.Operation.progress`가 있으면 그 인덱스의 원본 단계 작은 그림과 `numerator/denominator` 진행률만 지도에 표시한다. 전체 days로 현재 단계를 클라이언트에서 추정하지 않는다. 구형 응답의 null에서는 기존 작전 그림·남은 일수·원문 툴팁만 유지한다. 저장된 경과 일수와 선택 목록에서 서버가 원본 규칙으로 계산하므로 별도 클라이언트 저장 상태는 없다.

TFR `interface/mapicons.gui`의 `operation_map_icon_entry` 및 기반 `interface/mapicons.gfx`의 `GFX_phase_circular_progress_pie_chart_on_map`을 사용한다. 기존 원본 배경·채움 텍스처를 공통 `AgencyProgress`로 재사용하며 암호학 경로의 픽셀 계산은 유지한다. 원호 크기25 GUI, 단계 그림21 GUI는 원본 작은 그림×0.72×GUI2 배율이다. 원본 셰이더의 세밀한 안티앨리어싱과 완전한 픽셀 동일성은 검증 범위가 아니다.

`-PhoiTestSuite=agency-map`은 실제 서버23일 입력 외에 그림 선택 검사용 단계0/1/2의 합성 진행1/3을 두 해상도에서 촬영한다. 합성 응답은 전체days와 의도적으로 독립인 서버 필드를 사용해 클라이언트가 전체days로 단계를 추정하지 않는지 시각 대조한다. 권한 회수·좌우/반복 입력·상세 열람과 요청0 검사도 유지한다. 운영 접속·멀티플레이·음향 청취와 원작 동일 진행 상태 관측은 별도다.

집중 검증은 서버45초, GUI2 지도55초, 수정 전 별도 사본2분32초에 성공했다. 합성3단계×2해상도의 원본 그림 불투명826픽셀과 그림 밖 원호 불투명166픽셀은 원본 텍스처 계산값과 RGB 오차1 이내100% 일치했다. 이 값은 지정 픽셀 부분집합의 검증이며 원작 전체 렌더의 완전 일치를 뜻하지 않는다. 같은 자료의 원본/수정 전/수정 후 전체 프레임 비교2개를 CMP-87 artifact로 등록했다. 전체 검증은 별도 진행하며 집중 검사를 전체 성공으로 집계하지 않는다.


### 입력5 전체 클라이언트 검증 경계

상륙 지도 실행과 작전 현재 단계·진행 원호를 포함해 서버 전체 검증·입력5 동결 후 클라이언트의 무필터 `test runGameTest remapJar`를 순차 실행했다. JUnit client150·protocol128개 실패/오류0이며 변경 없는 성공 결과 재사용을 포함한다. 실제 렌더4160장, 입력파일1903개·공유 프로토콜49개·팩ZIP6개 해시를 확인했다. 자동 생성11그룹·107개 fixture의 소스와 출력 해시를 확인했다. 기존 수동 입력 경로는 `run-full-input5.ps1`에 기록한 앞선 검증 자료이며 모두 새 입력으로 생성했다고 주장하지 않는다. JAR은 client 전용이고 프로젝트 mod-list icon 외 이미지·음원은 없다. 재현 시 `-PhoiServerSource=C:/HOI/build/cmp87-handoff/server-input-5/HOI -PhoiProtocolSource=C:/HOI/build/cmp87-handoff/server-input-5/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`을 사용한다.

공작원 계급은 이후 서버 표시 필드로 연결되었다. 별명과 대교리 머리 칸 원작 관측은 미확인이다. 운영 접속·멀티플레이·직접 청취는 미수행이며 원작 spline/조명/애니메이션 완전 일치는 검증하지 않았다.

### 공작원 슬롯·해임과 국기·포획 행 표시

지도 공작원 카드는 서버 `AgencyView.operativeCapacity`와 기존 인원 중 큰 수만큼 표시한다. 별도 추가 +카드와 카드 아래 인원 수치는 표시하지 않는다. 슬롯이 줄어도 초과 인원을 숨기거나 임의 해임하지 않는다. 빈 슬롯은 발급된 모집 항목을 사용한다. 카드 X와 배지 초상의 빨간 해임 단추는 서버가 발급한 `confirms` 문구로 확인창을 열고 OK일 때만 활성 `dismiss:<id>`를 한 번 요청한다. 취소·Escape는 요청하지 않으며 선택 배지 X는 선택만 해제한다. 비활성·국가 권한 회수·대기 중 중복·낡은 응답 차단을 유지한다. 실패 시 서버가 보낸 인원을 보존하고 새 revision에서만 재요청한다.

외교·첩보·포획 국기는 국가별 정치 국기 선택 후 원본 국기만 표시한다. 이전 small2·small·smallest 합성은 보드 철회로 사용하지 않으며 외부 팩의 출처 자산은 보존한다. 포획 행의 큰 국기는 지휘관 원소속국 `Row.originalCountry`, 초상 우측 아래 작은 국기는 포획국 `Row.captor`다. 이름·역할·기술·능력치·수용량은 밝은 행 배경 위 검정 글자로 표시한다.

`CommanderCaptivityTest`는 능력치가 있는 서버 장교단 메뉴를 사용해 창을 열기 전후의 왼쪽 패널 전체 픽셀을 비교한다. GUI2 2560×1440·1280×720의 여섯 상태에서 이 검사를 통과했고, 포획 행의 검정 글자와 뒤쪽 능력치도 실제 캡처에서 확인했다. 이전 포획 시험 입력에는 능력치가 없었으므로 기존 캡처만으로 production 합성 결함을 확정할 수 없다. 실제 운영에서 보고된 소실 현상은 재현되지 않았으며 부모 렌더 production 코드는 이 작업에서 바꾸지 않았다.

앞선 연도는 기존 서버 `ResearchSourceTooltips.yearAhead`의 `$YEARS|2$`와 `ProtocolNumberBounds.fixed(years, 2)`를 유지한다. `ResearchSlotTooltipTest`의 `research-year-4-51`·`research-year-4-00`은 실제 포맷터 결과의 응답 직렬화와 두 해상도 호버 렌더를 검사한다. 캡처에서 `4.51`·`4.00`을 확인했으며 날짜를 직접 진행한 시험은 아니다. 설치 서버 JAR에도 두 자리 포맷터가 있으나 확인한 기본 런처 경로에서 설치 클라이언트 JAR은 발견하지 못해 사용 중인 설치본과의 차이는 미확정이다.

집중 재현은 `agency-map`, `agency-operations`, `captivity`, `design-team-candidates`이며 전체는 필터 없는 `test runGameTest remapJar`다. 팩을 먼저 빌드하고 동결 서버·프로토콜을 `-PhoiServerSource`·`-PhoiProtocolSource`로 지정한다. 현재 작업 입력은 `C:/HOI/build/cmp88/server-input/HOI`이며 수동 fixture 환경은 `C:/HOI/build/cmp88/run-client-retry.ps1`에 보존한다. 실제 운영 접속·멀티플레이·직접 음향 청취와 Claude 시각 검수는 수행하지 않았다.

클라이언트 전체 실행 도중 창이 종료되면 `Client gametests failed with an exception`과 `runOnClient called when no client is running`이 있어도 네이티브 종료 코드 0·`BUILD SUCCESSFUL`·실행 기록 `passed`가 남을 수 있다. 이 세 값만으로 통과를 판정하지 않는다. 로그 예외, 마지막 `hoi-loading-blank-flag` 캡처 및 해당 범위의 필수 캡처까지 대조한다. CMP-88의 첫 클라이언트 실행은 이 이유로 전체 성공에서 제외하며, 완료된 포획·연도 부분 증거와 미도달 공작원·외교 검사를 구분한다.

### 공작원 표시·작전 슬롯·정보망 후속 수정

임무 줄은 TFR `operativeleader.gui:operative_order_bar`의 3·1·2·5·8·4·6·7 프레임 순서, x=5·63·109·155·201·258·304·350과 402×79 배경을 GUI2 반 배율로 표시한다. 공작원을 선택했을 때만 나타난다. 제목은 설치 한국어 패치 `OPERATIVE_MISSION_*_TITLE`에서 확인했다. 지도 배정은 아래 입력6 계약으로 연결하며 발급되지 않은 단추는 비활성이다. 빈 레벨 상자도 원본 배경만 그린다.

선택 배지의 임무 문장은 설치 한국어 패치 `TFR_replace_operatives_l_korean.yml`의 `OPERATIVE_MISSION_*_ACTION`을 사용한다. 서버가 발급한 임무 choice 값과 표시명을 대조하며, 포획·부상 같은 별도 상태는 그대로 둔다. 대상국 이름은 같은 항목의 발급 국가 choice에서 얻고 작은 국기를 붙인다. 이름과 상태가 영역을 넘으면 처음 3초 동안 왼쪽 시작을 보여 준 뒤 실제 경과 시간으로 수평 스크롤한다. 게임 시간의 정지와 무관하게 움직이며 임무 문장의 잘림 영역은 대상국 국기 앞에서 끝난다. 오른쪽 위에는 정보기관 소속국 AgencyView.country의 국기를 테두리 없이 표시한다. X 그림은 operativeleader.gui의 GFX_NV_btn_close 원본을 사용한다. 기술·계급·경험치는 서버 표시 필드를 사용하며 경험치에서 계급을 추정하지 않는다. 계약에 없는 코드명은 표시하지 않는다.

작전 상세는 `operationoverview.gui`의 `operatives_grid` 60×75px 간격을 GUI2의 30×38GUI로 적용한다. 초상 슬롯을 누르면 발급 후보 목록에서 선택하며, 다른 슬롯의 요원은 중복 선택할 수 없다. 우클릭은 선택하거나 요청하지 않는다. 후보 선택은 로컬 초안만 바꾸고 준비 버튼이 기존 세션·revision·item·arguments 요청을 한 번 보낸다. 발급 요원 수·정보망·민간공장 요건과 기간, `Phase.description`을 표시한다. 진행 중인 작전에서 결과를 보여 주는 원본 장면이 확인되지 않아 `Phase.outcome`은 설명 사이에 표시하지 않는다. 긴 단계 설명은 기존 오른쪽 스크롤에 포함한다. 전망·보상과 자동 시작/반복은 아래 입력6 계약을 따르며 발급되지 않은 값은 만들지 않는다. 후보 선택 목록의 배치는 현재 클라이언트 구현이며 원작 선택 팝업과 완전 일치했다고 검증한 것은 아니다.

정보망은 기존에 가져온 HOI4 `gfx/maparrows/maparrows.txt:intel_network_edge`와 `gfx/FX/maparrow.shader:ArrowPixelShaderNoHead`의 패턴·마스크를 함께 사용한다. TFR의 해당 경로 덮어쓰기는 없다. `body_uv_scale=0.9`는 v=0.05..0.95, `texture_loop=7`, `texture_anim_speed=0.2`는 20틱당 0.2 UV 이동으로 연결한다. 원본 노란색은 사용자 지정 흰색으로 바꾸되 패턴 알파·명도를 유지한다. 지형 경계의 양옆 높이와 원본 atlas 경계 메시 위로 선을 올리며, 미로드 청크는 건너뛴다. 긴 선분의 높이 표본 간격은 최대 64등분으로 제한한다. 원본 엔진의 카메라별 폭·지형 조명·선택 깜박임과 픽셀 동일성은 검증하지 않았다.

`agency-map`은 기존 평면 바닥 대신 `atlas-client-scene.json`의 한국 ARMY 지형·경계 메시를 재생한다. 동일한 발급 정보망 주의 셀과 선분 경계로 원점을 맞추며 운영 지도나 저장 상태를 수정하지 않는다. 두 해상도에서 카드/해임/권한 회수, 원본 텍스처 존재, 새 응답 없는 UV 변화와 v 범위를 검사한다. 이전 평면 캡처와 새 atlas 캡처는 카메라·배경이 달라 픽셀 차분으로 동등성을 판정할 수 없다. 운영 지도 접속·다중 플레이·직접 음향 청취는 미수행이다.

재현은 아래 입력6 경로로 Java25 `test runGameTest remapJar -PhoiTestSuite=agency-operations` 및 `agency-map`을 실행한다. 전체 무필터 검증은 서버 검증과 시간 조율 후 별도로 실행해야 한다. 미발급 배지 값과 Claude 시각 검수는 남아 있다.

`finishRunClientGameTest`는 이제 실행 폴더의 `logs/latest.log`에서 `Client gametests failed with an exception`과 `runOnClient called when no client is running`을 검사한다. 네이티브 종료 코드가 0이어도 `hoi-run.json`에 `failed`와 `logFailures`를 기록하고 Gradle을 실패시킨다. 과거 기록을 다시 쓰지는 않는다. `python scripts/verify_gametest_isolation.py`의 정상·이전 파일 잠금·자식 실패·각 예외의 종료 코드 0·복구 재실행 6개 회귀 검사가 통과했다. 이 프로브 검사는 실제 Minecraft 전체 검증을 대신하지 않는다.

전체 검증의 기본 `agency` 경로도 대상국 단추 → 발급 후보 선택 → 준비하기 순서를 따른다. 대상국 단추만 눌러 후보 목록이 열린 동안에는 준비가 비활성이며, 후보를 실제 선택한 뒤 발급 국가 ID가 요청에 들어가는지 검사한다. 배지 수평 스크롤 재현은 `agency-map`의 선택 직후/9초 경과 캡처를 사용한다.

### 입력6 임무 배정과 작전 조작

입력6의 `Operative.statusText`를 배지 상태 문장에 우선 사용하며 구형 응답만 기존 원본 키 조합을 유지한다. 임무 아이콘은 발급된 `assign:<id>:<Mission>`의 현재 임무 표시를 원본 작은 아이콘 프레임에 연결한다. 이름과 상태는 같은 크기를 사용한다. 0.8배 축소에서 한글 획이 빠져 기본 글꼴 크기를 유지하며 긴 문장은 국기 앞 경계에서 수평 스크롤한다. 소속국 국기는 배지 오른쪽 끝에 테두리 없이 그린다. `operative_leader_portrait_window_small/portrait_dismiss_btn`의 빨간 `GFX_NV_btn_delete`는 기존 해임 항목을 사용한다. 후속 서버의 기술·계급·경험치 필드는 그대로 표시하고 값이 없는 구형 응답과 코드명은 비워 둔다. 상단 X는 원본 `deselect_button`처럼 서버 요청 없이 선택을 해제하고, 아래 카드 X와 빨간 초상 단추는 서버 `confirms`의 확인창을 열고 OK에서만 같은 국가·revision의 해임 요청을 보낸다. 확인 계약이 없으면 해임하지 않는다.

활성 임무 단추를 선택한 다음 지도 좌클릭으로 정규화된 하향 `ray:x,y,z`를 보낸다. 서버가 주 지배국과 발급 선택지를 검증한다. 방첩 우클릭은 발급 목록의 자국을 바로 보내고 지도 우클릭·Esc는 배정 선택을 취소한다. 이념 지지는 지도 선택 뒤 발급 이념을 고른 다음 두 인자를 보낸다. 이념 단추의 원본은 `countrydiplomacyview.gui/diplomacy_espionage_mission_ideologygroup_item/GFX_button_123x34`이며 현재 줄 배치는 가용 화면 너비에 맞춘 클라이언트 배치다. 같은 revision에서 중복 배정을 차단하고 서버 거절 후 새 응답의 공작원 상태를 보존한다. `HIGHLIGHT(session, revision, assign 항목, [])`로 서버에 선택 가능한 나라의 실제 주 도형을 요청한다. `hoi:agency_highlight_v1`의 페이지는 세션·revision·항목·차원을 확인하고 모두 조립된 뒤 게시한다. 빈 페이지는 강조를 비우고 임무 취소·변경·권한 회수·차원/세션/revision 변경 때 도형을 지운다. 요청은 4틱 간격을 지키며 대기 중 새 선택이 생기면 마지막 선택을 보낸다. 국가 ID 목록만으로 지형을 추정하지 않는다.

작전 상세는 `stage`·`autoCommence`·`autoRepeat`와 발급 `commence_operation`·`auto_commence`·`auto_repeat`·`cancel_operation`의 활성 여부를 따른다. 준비 중·준비됨·작전 중 상태를 구분하고 작전 중 취소는 비활성이다. 상자 클릭은 다음 서버 응답 전까지 표시 값을 바꾸지 않는다. 전망 등급은 미제공이므로 빈 상자를 유지하고, 전망 호버는 원본 `OPERATION_RISK_HEADER`·`OPERATION_OUTCOME_HEADER`의 0자리 퍼센트와 서버 `outlook`을 사용한다. 기간은 `total-days`, 아래 보상 상자는 서버 `reward`다. 단계 본문에는 `Phase.description`만 표시하고 참가 요원·취소 항목 `detail`을 덧붙이지 않는다. 본문과 보상은 각 영역을 넘을 때만 스크롤한다. 작전 목록에서 개시·상자 항목을 별도 작전으로 중복 나열하지 않는다.

재현 입력은 `HOI_AGENCY_OPERATION_PREVIEW=C:/HOI/build/cmp86/server-input-6/fixtures/commander-source-fixtures`이며 `-PhoiServerSource=C:/HOI/build/cmp86/server-input-6/HOI -PhoiProtocolSource=C:/HOI/build/cmp86/server-input-6/HOI/protocol/src/main/java`를 사용한다. 팩 선빌드 후 `test runGameTest remapJar`에 `-PhoiTestSuite=agency-operations` 또는 `agency-map`을 지정해 집중 검사한다. `kor-agency-operation-stages.json`은 세 단계의 조작을, `kor-agency-operatives.json`은 임무 배정·금지·거절·revision 검사를 재생한다. 렌더와 모의 전송은 실제 운영 접속·다중 플레이·직접 음향 청취 검증을 대신하지 않는다. 정보망 추가 형태 변경은 보드 결정 대기다.

보상에 쓰는 침투 아이콘 4개는 원본 `GFX_text_infiltrate_*_token`을 명시적으로 대응시켜 표시한다. 임의 GFX 이름을 텍스처로 해석하지 않는다. 키워드 목록 읽기 한도는 512KiB로 제한하며 실제 리소스 팩을 읽는 GameTest가 해당 토큰의 글꼴 치환을 확인한다. 전망·임무 툴팁은 원본 서식 코드의 색을 보존한다.

### 작전 제안의 시작 방식

서버가 작전 항목의 마지막 매개변수로 `manual`, `auto_commence`, `auto_repeat`, `auto_commence+auto_repeat` 네 선택지를 발급하면 준비 전에도 원본 `operationoverview.gui`의 두 체크 칸과 반복 그림을 표시한다. 이 매개변수는 요원 초상 슬롯과 중복 요원 검사에서 제외한다. 체크 변경은 로컬 초안만 바꾸고, 준비하기를 누를 때 국가·요원 뒤 마지막 인자로 함께 보낸다. 구형 입력에 이 매개변수가 없으면 체크 칸을 임의로 추가하지 않는다. 문구는 발급 선택지를 사용하며 설치 한국어 패치의 `OPERATION_VIEW_AUTO_COMMENCE`, `OPERATION_VIEW_AUTO_REPEAT`와 대응한다. 체크 효과음은 원본 `decisions_ui_button`을 가져온 `ui.decision.track`이다.

`Preparation.label`의 색상 코드는 그대로 보존하므로 공장 부족 시 서버가 발급한 `§R` 숫자는 빨간색으로 표시한다. `agency-operations` 검사는 GUI2 두 해상도에서 네 시작 방식, 우클릭 무효, 반복 전환, 요청 전송 시점과 중복 요청 차단을 검사한다. 준비 중 작전이 여러 개여도 캡처 이름에 작전 ID를 붙여 각각 보존한다. 실행 입력은 확정 서버 사본과 별도로 표시하며 임시 preview의 통과를 최신 서버 전체 검증으로 간주하지 않는다. 실제 서버 접속·다중 플레이·직접 음향 청취는 별도 검증이다.

기술·계급 경계 검사는 서버 표본 사본의 표시 필드만 1~5와 신병·자산·정보원·비밀 요원·전설로 바꿔 재생한다. 배지의 숫자 12GUI/계급 96GUI와 모집 행의 계급 50GUI 폭을 확인하고 GUI2 두 해상도에서 각 단계를 캡처한다. 이 자료는 `rank-boundary`로 구분하며 실제 서버에서 경험치를 얻어 승급한 증거가 아니다. 기술5에서만 진행률을 -1로 보내는 계약을 따른다.

### 입력7 원본 상태 프레임과 실제 강조 표본

임무 단추마다 `GFX_operative_mission_active`의 0번(검은 표시) 또는 1번(초록 표시와 빛) 프레임을 그린다. 임무 그림 8프레임은 상태별 색상 변형이 아니므로 원본 픽셀을 유지한다. 초록 표시는 발급된 현재 임무를 따르며 아직 지도 대상만 고른 상태를 현재 임무로 표시하지 않는다. 임무 카드 위에는 원본 78×26 배경, 20×20 임무 그림과 44×20 화살표를 각각 GUI2 반 배율로 그린다. 작전 중 카드의 작전 그림과 임무 중 카드의 임무 그림은 구분한다.

배지 특성은 응답 배열의 개수·순서 그대로 표시한다. 입력7 요원 세 명의 개수는 각각 1·0·1이며 원작 사진 속 다른 요원의 특성 세 개를 복제하지 않는다. 배지 초상 왼쪽 아래 `GFX_NV_btn_delete`는 해임 항목이 활성일 때 빨간 원본, 비활성일 때 기존 회색 처리를 사용한다. 작전 초상 오른쪽 아래는 `operationoverview.gui/operation_operative_view_entry`의 `portrait_overlay_bg`와 `mission_icon`이다. 발급된 현재 임무와 기존 임무 choice 표시명에서 원본 작은 임무 프레임을 찾고, 알 수 없으면 만들지 않는다. 임무 복귀 화살표는 복귀 여부 계약이 없어 추정하지 않는다.

단계 설명의 원형 그림은 36GUI 사각 영역(원본 불투명 영역 약 64px), 보상 훈장은 48GUI 사각 영역(원본 불투명 높이 약 69px)으로 그린다. 단계 글자 시작점·행 간격·보상 본문 영역은 유지한다. 입력7 `reward`와 설치 번역 `OPERATION_TOKEN_RECEIVE`에는 문장 앞 `»`가 없으며 클라이언트가 접두어를 임의로 덧붙이지 않는다.

집중 검증의 입력은 `HOI_AGENCY_OPERATION_PREVIEW=C:/HOI/build/cmp86/server-input-7/fixtures/commander-source-fixtures`다. 팩 선빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=agency-map` 또는 `agency-operations`에 `-PhoiServerSource=C:/HOI/build/cmp86/server-input-7/HOI -PhoiProtocolSource=C:/HOI/build/cmp86/server-input-7/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`을 지정한다. 무필터 전체 검증은 `-PhoiTestSuite`만 제외하며 서버 전체 검증과 겹치지 않는다.

지도 검사는 형제 `agency-highlight-source-fixtures/kor-counter-intelligence.json`(1페이지·59상자)과 `kor-build-network.json`(16페이지·16,321상자)을 읽는다. 원본 표본의 session=`fixture`, revision=1, item을 확인한 뒤 테스트 화면의 세션·revision·차원으로만 재생하며 상자 좌표·높이·재질은 그대로 둔다. 모든 페이지를 받은 메시의 정점 수와 취소 시 제거, 두 해상도 실제 렌더를 검사한다. 별도 합성 페이지 검사는 잘못된 세션·revision·항목과 빈 페이지 교체를 계속 검사한다. 첫 실행에서 화면 fixture의 UUID 세션과 강조 fixture의 `fixture` 세션을 동일하다고 가정해 실패한 기록은 보존한다. 실제 서버 연결·멀티플레이·음향 청취를 검증한 것은 아니다.

### 작전 초기 선택과 하단 첩보 툴팁

작전 제안의 요원 슬롯은 선택되지 않은 상태로 열리며 원본 `GFX_operative_unknown`과 `GFX_group_add_operative_anim`을 표시한다. 모든 필수 요원을 고르기 전에는 준비하기가 비활성이고, 새 응답에서 사라진 후보는 빈 선택으로 돌아간다. 이미 고른 유효한 후보는 유지한다. 시작 방식의 기본값은 발급된 `manual`이다.

지도 하단 임무·요원 카드의 툴팁은 해당 단추/카드 상단을 기준으로 위에 배치하고 긴 본문은 그 공간 안에서 스크롤한다. 임무 제목 뒤에는 구분선 대신 빈 줄 하나를 둔다. 원본 `§L` 본문 표시는 Minecraft의 소문자 `§l` 볼드로 해석하지 않는다. 준비 문구는 줄바꿈 없이 막대 위 가운데에 그리며 서버의 빨간 `§R` 숫자와 `GFX_operation_resource_progress_unfinished`를 함께 표시한다. 아이콘 출처는 원본 `interface/operationoverview.gfx`, 배치는 TFR `operationoverview.gui/resource_progress_icon`이다. 글꼴은 기존 1배를 유지한다.

재현 입력은 `C:/HOI/build/cmp86/server-input-10`이며 팩 선빌드 후 `test runGameTest remapJar`에 동일 입력의 `HOI`와 `HOI/protocol/src/main/java`를 각각 `-PhoiServerSource`, `-PhoiProtocolSource`로 지정한다. `HOI_AGENCY_OPERATION_PREVIEW`는 해당 입력의 `fixtures/commander-source-fixtures`다. 집중 검사는 `-PhoiTestSuite=agency-map`과 `agency-operations`이며 툴팁 경계·본문 보통체·빈 초기 선택·후보 선택·네 시작 방식 요청을 검증한다. 실제 운영 접속·멀티플레이·직접 음향 청취는 별도 검증이다.

## 첩보 카드·작전 배경·요원 배정 원본 대조

공작원 프레임은 원본 `GFX_operative_frame`의 173×117 아틀라스에서 두 상태를 나누어 원본 세로 비율을 유지한다. 초상은 틀 안쪽 오른쪽 경계까지 채우고, 위 임무 표시와 프레임은 `operativeleader.gui`의 y17/y40 접합을 따른다. 배지 초상의 `GFX_unit_leader_portrait_frame_badge`를 같은 원본 위치에 그린다. 작전·암호학 탭은 본문 행의 양끝에 맞추며 원본 탭 테두리를 유지하고 중앙만 늘린다.

카드와 임무 선택 줄 사이의 상태칸은 `top_status_bg`·`operative_active_operation_icn`의 78×26 원본 프레임을 사용한다. 위치 (0,17), 임무 그림 (5,20)/19×19, 화살표 (31,19)/44×20을 반 배율 pose로 그려 GUI2에서 홀수 원본 픽셀을 보존한다. 상태칸을 먼저 그리고 초상·카드 프레임을 겹쳐 원본 접합 테두리를 유지한다. 카드 틀은 (-3,40), X 단추는 (61,44)/16×16 원본 좌표다. 작전 그림은 원본 85×85의 0.25배에 해당하는 21×21 칸에 들어간다. 카드 하단 기술 숫자는 표시하지 않으며 배지·배정 목록의 기술 숫자는 유지한다. 현재 임무의 금색 테두리는 `operative_mission_button_background/frame_1`, 초록 불빛은 별도의 `operative_mission_active/frame_1`이다.

임무 화살표는 원본 11프레임을 24fps로 반복한다. 작전 화살표는 `operativeleader.gfx`의 원본 배경·마스크·preparing 텍스처를 사용하는 팩의 `operation_status_scroll`에서 8초 주기의 프레임을 선택한다. 팩은 -90도·(1,1.4) 배율·normal 합성을 24fps로 표본화한다. 원본 엔진의 연속 shader 실행과 같은 시간 해상도라는 뜻은 아니며, 스크린샷마다 화살표 위상이 다를 수 있다.

작전 상세의 파란 바탕은 `GFX_operations_clipboard_top_bg`·`mid_bg`·`bottom_bg`의 이어지는 원본 조각을 한 번씩 그린다. 중앙 그림을 가로·세로 타일로 반복하지 않는다. 작전 행은 `GFX_launch_operation_button_anim`, 암호학 행은 `GFX_cryptology_country_entry_bg`와 `GFX_crypto_decryption_start_stop_button`·`GFX_crypto_decryption_activate_button_animated`를 사용한다.

작전 요원 배정은 `20261102092042_1.jpg`와 한글패치 `SELECT_OPERATIVE_TITLE`을 기준으로 기존 모집창의 독립 창·기술/이름 정렬·검색·스크롤을 재사용한다. 서버가 발급한 매개변수 후보만 표시하며 다른 슬롯에 고른 요원은 비활성이다. 선택은 로컬 초안만 변경하고 준비 요청 때 전송한다. 새 revision에서 후보가 사라지면 기존 초안을 정규화하고, 닫기는 작전 창으로 돌아간다. 원본 하단 ‘임무 재개’는 입력1의 마지막 시작 방식 choice에 `+resumeN`을 붙인다. 기본은 꺼짐이며 자동 시작·자동 반복을 바꾸어도 요원별 선택을 유지한다. 서버가 발급한 choice만 전송한다. `Operation.resume`의 요원에는 원본 재개 표시를 그린다. 완료 작전은 서버 결과·날짜·전망·보상을 유지하며 목록 ‘완료’와 상세 ‘닫기’가 같은 `dismiss_operation:<id>` 요청을 보낸다. 오른쪽·반복 클릭과 오래된 응답은 발급 요청의 잠금을 우회하지 않는다. 습격은 별도 `RaidScreen`과 공작원 지도 모드의 `AgencyRaidHud` 세션에 연결되어 있다.

재현은 팩 선빌드 후 `C:/HOI/build/cmp90/server-input-1`의 `fixtures/commander-source-fixtures`를 `HOI_AGENCY_OPERATION_PREVIEW`, `HOI`를 `-PhoiServerSource`, `HOI/protocol/src/main/java`를 `-PhoiProtocolSource`로 지정한 `test runGameTest remapJar -PhoiTestSuite=agency-operations` 및 `agency-map`이다. GUI2 2560×1440·1280×720의 실제 렌더 비교이며 원본과 fixture의 국가·요원·날짜·진행 상태는 다를 수 있다. 운영 접속·멀티플레이·직접 음향 청취는 별도 검증이다.

민간공장 게이지는 `operation_resources_view_entry` 기준 프레임(110,13)/99×15와 채움(113,17)/93×8을 반 배율 pose로 그린다. GUI2에서 보이는 틀은 원본 알파 경계의 97×11이며, 정수 GUI 크기로 다시 맞추지 않는다. 채움 위에는 어두운 1px 줄을 남긴다. 원본 efficiency 텍스처의 0부터 시작하는 행 9~13·15~17을 순서대로 사용하고 왼쪽부터 자른다. 이는 원본 `20261102092141_1`·`20261102092214_1`의 보이는 8줄에 맞춘다. 민간공장 바탕도 (15,2)/85×27 원본 크기로 그려 세로 중앙 보정 때문에 아래 구분선을 덮지 않게 한다. `AgencyOperationTest.preparationFill`은 발급 fixture의 진행률만 0/0.4/1로 바꾼 경계 표본을 GUI2 두 해상도로 렌더하여 40% 왼쪽이 전체 채움과 같고 오른쪽은 빈 막대와 같은지, 상단 빈 줄·8줄 RGB와 프레임 불투명 픽셀이 원본과 같은지 검사한다. 표본의 서버 문구는 그대로 유지하므로 합성 진행률과 문구의 수치가 다를 수 있으며 운영 상태의 증거가 아니다.

상세 창 왼쪽 위 대상국·작전 그림 칸은 `container_target_info`의 x15와 254×94 배경, 그 안의 (10,3)/85×85 그림을 반 배율로 유지한다. 그림의 투명 가장자리까지 보존하고 뒤 검은 칸에 맞춘다. 이 배경과 오른쪽 진행 설명 바탕이 만나는 부분은 배경을 먼저 그리고 설명 바탕을 겹치는 원본 순서를 따른다. 요원 칸과 행 간격은 바꾸지 않는다. 설명 스크롤 높이는 실제 잘림 영역의 시작점부터 계산하여 본문 시작 여백까지 이동 범위에 포함한다. `hoi-operation-description-bottom-*` 캡처는 휠로 끝까지 이동한 뒤 마지막 문장을 확인하는 증거다. 집중 렌더와 입력1 전체 검증은 구분하며 새 프로토콜 연결을 이전 캡처의 통과 결과로 대신하지 않는다.

조선소 위치는 `production-scrollbars` 집중 검사의 해군 생산 줄로 확인한다. 원본29×23 칸 간격, 그림(3,2)/26×18과 배경28×22를 적용하고 육군 공장 경로는 유지한다. 입력1 전 집중 검사는 전체 검증을 대신하지 않는다.

작전 목록의 기간은 원본 `operation_duration_text`의 오른쪽 정렬을 적용한다. 원본 x390 끝선과 x395 단추 사이의 5px 간격을 현재 단추의 실제 그림 경계 기준으로 유지한다. 요원·정보망 그림은 원본 (98,50)·(145,50), 전체 33×29(투명 여백 포함)를 반 배율로 그리며 숫자는 각각 (134,56)·(181,56) 기준이다. 민간공장 게이지 위 문구는 실제 글자 하단에서 틀의 보이는 윗줄까지 원본 약5px를 두고 게이지 위치는 유지한다. 원본 `GFX_industrial_capacity_icon` 두 번째 프레임의 빨간 !는 시작 전·준비 중·준비 끝·완료 사진에서 공통으로 보이므로 작전 창 단추에 항상 쓴다. 게이지 오른쪽은 시작 전에는 없고, 실제 작전의 `preparation.progress`가 1 미만이면 원본 unfinished, 1이면 finished를 쓴다. 공장이 충분한 준비 중 장면은 미제공이며 unfinished 조건은 그림 이름과 서버 계약을 따른 해석이다.

빈 공작원 칸은 `vacant:<0부터 시작하는 칸 번호>`의 enabled로 회색/초록 원본 + 그림과 클릭 여부를 정한다. 대기 중 숫자는 서버 value 그대로이며 로컬로 날짜를 계산하지 않는다. 배지 활동 막대는 경험치 `progress` 대신 `activity`의 mission/disabled/captured/operation에 맞는 원본 네 막대와 `activityProgress`를 쓴다. 작전 목록 토큰의 가로 위치는 얻는 토큰=제목 뒤, 필요한 토큰=정보망 뒤를 유지한다. 원본 재대조 요청에 따라 육군·해군·공군·민간·편지 토큰은 두 줄 배치로 표시한다. 획득 토큰은 제목 옆 원본 텍스트 아이콘, 필요 토큰은 `container_tokens` (215,50)의 33×29 원본 작전 그림을 반 배율로 그린다. 필요 토큰을 작은 텍스트 아이콘으로 대체하지 않는다. 각 줄 안에서 그림 중심을 맞추며, 원본 투명 여백을 보존한다. 별도 토큰 툴팁은 원본 장면이 없어 만들지 않는다.

입력1 정보망 강조는 고정 서버 fixture `kor-build-network.json`의 3페이지·2,632상자이며 방첩은 1페이지·59상자다. 테스트는 source session/revision/item과 상자 수를 검증한 뒤 전체 페이지가 모였을 때만 동일한 지오메트리를 게시하는지 확인한다. 이전 입력7의 국가 전체 16,321상자는 입력1의 대상 주 강조 기준이 아니다. 실제 운영 접속·멀티플레이·직접 음향 청취는 미수행이다.

완료 날짜가 원본 칸 너비를 넘으면 발급 표시문을 생략하지 않고 같은 글자 크기로 칸 안에서 가로 스크롤한다. 완결된 작전의 자동 시작·반복·취소는 서버가 발급한 enabled를 유지한다. 임무 재개 표시의 툴팁은 한글패치 `OPERATIVE_RESUME_MISSION_AFTER_OPERATION_INDICATOR_TOOLTIP`을 사용한다. 입력1 결과 fixture의 `Operation.resume`는 모두 빈 목록이었다. 입력3은 준비된 fixture-operation-2와 완료된 fixture-operation-5의 재개 요원 ID를 제공하며 동일 원본 아이콘·툴팁 경로로 표시한다. 시작 요청 `+resume1`의 입력/보존/전송과 결과 표시 검사는 구분한다.

작전 목록의 진행 중 기간 칸은 서버 `Operation.total - Operation.days`와 ‘일’만 표시한다. 상태명은 기존 작전 단추에 남기고 완료 행의 발급 완료 날짜는 유지한다. 원본 `20261031015433_1.jpg`의 제목 옆 작은 획득 토큰과 정보망 뒤 큰 필요 토큰을 별도로 비교한다. 재현은 입력2의 동일 fixture·프로토콜 사본을 지정한 `-PhoiTestSuite=agency`이며 필터 없는 전체 검증과 구분한다.

입력2의 작전 목록에서 육군 필요 토큰과 해군·공군·민간 획득 토큰을 GUI2 두 해상도로 실제 렌더했다. 필요 토큰 5종은 원본 DDS와 출력 PNG의 크기·RGBA가 같다. 입력2에는 저항 접선/강화 작전이 없어 편지 토큰의 새 배치 렌더는 미확인이다. 첩보 집중 94장, 외교 집중 32장, 교리 집중 241장의 통과는 각각 입력 재생 검사이며 운영 접속·다중 사용자·직접 음향 청취를 포함하지 않는다. 시작 시 Anisotropic Filtering 옵션 오류와 Realms 인증/밉맵/지연 경고가 있었으며 Gradle·GameTest 최종 종료는 성공했다.

작전·암호학 본문은 원본의 별도 배경을 사용하며 두 탭의 합산 폭을 본문에 맞춘다. 암호학 행은 원본 국기·막대·단추 배치를 사용하고 좌측 하단 서버 메시지를 그리지 않는다. 입력2 당시의 미제공 한계와 달리 현행 `AgencyView.ciphers`는 기간·상태·삭제/공개 확인을 전달하며 아래 계약으로 표시·요청한다. 외교 장부는 서버 `Report.tooltip`의 문구·색·아이콘과 통계/연구 분야 detail을 그대로 표시한다. 본문 공개/정보부족은 서버 `Report.sections`로, 민간 그래프는 서버가 발급한 날짜/시계열로 표시하며 클라이언트가 백분율·그래프 값을 만들지 않는다. 서버가 발급한 `?` 표시도 그대로 보존한다. 대교리 미리보기 원본 사진의 관측 한계와 이미 연결된 암호학·장부 계약을 구분한다.


### 암호학 행과 첩보 장부 구역의 서버 계약

‘첩보가 공개됨’의 가독성을 위한 사용자 요청에 따라 공개 칸은 원본 좌표의 오른쪽 끝 x471을 유지하고 왼쪽을 x360에서 x320으로 넓힌다. 원본 배경의 테두리를 보존하는 9-slice를 쓰고 메달·진행 막대 끝·시작/정지 단추를 함께40px 왼쪽으로 이동한다. 메달과 상태 글자가 겹치지 않게 글은 x328부터135px 영역에 두 줄로 그린다. 원본의 칸 폭을 그대로 복제한 배치와 구분한다. GUI2 1280에서는 목록 중간 스크롤의 `hoi-cipher-active-visible-1280-gui2` 캡처로 공개 상태의 두 줄을 검사한다.

암호학은 `AgencyView.ciphers`의 서버 순서와 decrypting/paused/done/active 상태를 따른다. 상태문·기간·공개 혜택·휴지통 사용 가능 여부는 서버 필드 그대로이며 해독 시간을 클라이언트에서 계산하지 않는다. 원본 `countryintelligenceagencyview.gui/cryptology_country_entry`의 519×83 좌표계를 유지한다. 시작/정지 그림은 원본 프레임0=정지, 프레임1=시작이며 완료/활성화는 비활성 정지 그림을 사용한다. 삭제와 공개는 서버 `confirms` 문구를 기존 확인창에 연결하고 수신 revision·국가가 달라지면 실행하지 않는다. 지도 추가는 `Capital.item`을 보내며 빈 ID는 실행하지 않는다. 이전 입력의 나라별 items는 기존 재생 검사 호환 경로로 남는다.

첩보 장부는 `Report.sections`의 `no_intel`/`shown`을 사용한다. 육군 사단/비축량은 원본 탭 에셋으로 전환하며 해군·공군 유형 줄과 본문은 별도 구역이다. 민간 그래프의 다섯 선택 단추는 원본 아이콘과 각 서버 tooltip에 연결하고 그래프 수치를 만들지 않는다. 정보부족 칸은 원본 `GFX_no_intel`과 서버 text를 표시한다. 모르는 수 `?`와 알려진 수 `0`을 구분하며 특수부대·군 장비 통계도 서버 entries를 사용한다. 입력3의 공개된 본문 행은 airwing_types의 cas 한 줄이고, 많은 정보로 드러나는 편제·비축량·함선·항공기 상세 배치는 입력4 검증 범위다. 원본 GUI가 참조하는 `GFX_navy_overview_ship_entry`는 설치된 TFR/기본 gfx에 정의가 없어 임의 배경으로 대체하지 않았다.

재현: `HOI_AGENCY_OPERATION_PREVIEW=<입력3>/fixtures/commander-source-fixtures`, `HOI_DOCTRINE_MILESTONE_PREVIEW`를 같은 폴더로 지정하고 Java25에서 팩 선빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=agency`, `-PhoiTestSuite=diplomacy`, `-PhoiTestSuite=doctrine-icons`를 순서대로 실행한다. `-PhoiServerSource=<입력3>/HOI`와 `-PhoiProtocolSource=<입력3>/HOI/protocol/src/main/java`를 함께 지정한다. 입력 재생은 실제 운영 접속·다중 사용자·직접 음향 청취를 대체하지 않는다. 이 계약의 최종 집중·전체 결과는 검증 종료 후 기록한다.

### 툴팁 공통 가로 폭

TFR `interface/core.gui`의 `ToolTip` 선언은 `maxWidth=400`, `borderSize={18,18}`, `VCR02_12`다. 설치된 한국어 패치는 같은 글꼴 이름을 덮어쓴다. 원작 2560×1440 사진의 긴 툴팁 가시 테두리는 여러 화면에서 361~362px이며 짧은 글은 최장 행에 맞춰 줄어든다. 선언의 400과 가시 테두리 362가 다른 이유를 비공개 엔진 내부 계산까지 확인한 것은 아니다.

`Tooltips.width`는 사진에서 측정한 가시 상한을 GUI2의 181GUI로 적용하고 각 렌더 경로의 기존 좌우 여백을 뺀다. 위젯·일반 툴팁은 14GUI, 스크롤·결정·정치 툴팁은 8GUI 여백을 유지한다. 짧은 본문은 줄바꿈 후 실제 최장 행에 맞추며, 그림·국기는 폭에 포함하고 그림 종횡비를 보존한다. 본문 글꼴을 줄이지 않는다. 동일한 원문도 원작 글꼴과 Minecraft 글꼴이 달라 줄바꿈 위치가 완전히 같다는 뜻은 아니다. 일반 패널·본문의 `split`과 `sourceLines` 폭은 이 규칙의 대상이 아니다.

화면별 `drawExact`·스크롤 툴팁 최대 폭 인수를 없애고 결정·정치·상단 HUD·연구·지휘관 지도와 기본 위젯 경로에 같은 상한을 적용한다. `GuiTooltip`은 HOI 화면·캠페인 HUD에서만 Minecraft 위젯의 줄바꿈 폭을 바꾸며 바닐라 화면의 기본값은 유지한다. 재현은 팩 선빌드 후 `runGameTest -PhoiTestSuite=tooltip-width`다. 합성 긴/짧은 위젯, 스크롤, 결정, 국기·그림 정치 툴팁을 GUI2 2560×1440·1280×720에서 확인하며 `cursor-tooltips`·`tooltip-spacing`은 커서·경계·원문 스타일 보존을 따로 검사한다. 합성 장면과 원작 대응 사진, 실제 운영·다중 접속 확인은 구분한다.

### 습격 화면과 장비 우선순위

메인 지도 커서 화면 우측 하단에는 별도 습격 바로가기 배경·아이콘·툴팁·클릭 위젯을 표시하지 않는다. `AgencyRaidHud`는 `hoi:map/selector/operatives` 공작원 모드에 들어갈 때 독립 습격 세션을 한 번 OPEN하고 나갈 때 CLOSE한다. 재진입은 새 세션이며 이전 세션의 지연 응답은 받지 않는다. 국가·차원 변경과 권한 회수·연결 해제도 이전 화면을 폐기한다. 공작원 세션과 서버 상태는 합치지 않는다.

`RaidScreen`는 서버 `RaidView`의 표적·부대/출발지·생성·조직·준비·개시·종료 상태를 표시한다. 원본 `military_raids/country_raids_view.gui`와 `raid_map_details_window.gui`의 배경·레버·단추는 외부 팩의 `raids/` 자산을 쓴다. 부대 행은 원본 `GFX_tiled_window_small`의 borderSize32를 적용한다. 공작원 지도에서는 같은 렌더를 중첩하며 표적 아이콘·툴팁·진행 선·우측 상단 분류 줄을 표시한다. 사용자 요청으로 우측 하단 필터는 그림·클릭·툴팁 모두 표시하지 않으며 진입 시 숨긴 분류를 비워 모든 표적을 표시한다. 생성 창이 열리면 분류 줄을 숨기고 공작원 HUD는 창 뒤에 유지한다. 닫기는 서버의 close/setup_close 응답을 받아 공작원 지도로 돌아온다. 다른 지도 모드와 일반 메인 화면에는 습격 표시를 하지 않는다.

습격 창·단추·표적 위 입력만 먼저 소비하고 나머지는 기존 공작원 카드·정보망 대상 주 선택으로 전달한다. 지도 표적과 진행 선은 공작원 HUD 아래에 그리고, 겹친 자리의 클릭·툴팁은 위에 보이는 공작원 조작 영역이 받는다. 습격 부대/생성 창과 분류 줄은 HUD 위에 그린다. 생성 창은 모달이며 지도 이동을 막는다. `RaidScreenHudTest`의 합성 좌표 장면은 세션·오래된 응답·반복 클릭·Shift 클릭·닫기·모드 이탈/재진입을 검사한다. `AgencyPresentationTest`의 atlas 장면은 원본 서버 표적 좌표를 바꾸지 않고 실제 56×56 지도 표본에 투영한다. 이 표본은 전체 운영 지도를 설치한 검증이 아니다.

표적 왼쪽 클릭은 `target:`, Shift 클릭은 `quick:`, 부대·출발지 선택은 발급된 `unit:`·`source:` 요청이다. 위험도·생성·시작·취소·자동 실행·종료 보고 닫기도 서버가 발급한 항목과 인자만 전송한다. 오른쪽 클릭·응답 대기 중 반복 클릭·비활성 항목을 차단하고, 세션·국가·revision을 검사한다. 권한 회수와 연결 해제 시 비공개 화면을 닫는다. 원본에 없는 하단 message는 표시하지 않는다. 창 제목을 드래그하면 화면 안에서 이동하고 긴 부대 목록·장비 표·장부 본문은 스크롤한다.

모병 및 배치의 작전·습격 막대는 `RecruitmentPolicy.equipmentRatios`를, 툴팁은 `equipmentTooltips` 원문을 쓴다. 높은 정보량 장부는 서버 sections의 편제·유형·모델 행과 수량 범위를 그대로 표시한다. 입력5의 함선/항공기 비헤더 행은 서버 설계 이름과 `production/source/<hash>`를 사용한다. 모델명은 추측하지 않는다. 원본 `countryintelledger.gui`의 모델 그림 중심은 함선(110,30), 항공기(60,35)이며 행 원점에 맞춰 각 그림 칸의 가로 중심을 배치한다. 항공 분류 머리줄에는 원본대로 별도 그림을 넣지 않는다. 긴 모델명은 기본 글자 크기를 유지하는 Minecraft 가로 스크롤로 표시한다. 좁은 화면에서 수량 범위나 통계 문구가 칸을 넘으면 같은 가로 스크롤을 쓰며, 칸 안에 들어가는 숫자는 기존 중앙 정렬을 유지한다.

습격 목록은 `Entry.category`로 묶어 원본 `raid_instance_list`의 분류 아래에만 표시하며 별도 현재 습격 목록을 만들지 않는다. V는 그 분류의 행을 펼치거나 접고 표적 필터를 바꾸지 않는다. 분류별 원형 개시 단추는 `Category.launchAll`의 발급 항목·enabled·툴팁을 그대로 사용한다. 빈 목록은 `emptyTooltip`을 V 툴팁에 함께 표시한다. 독립 습격 검사 화면을 포함한 모든 렌더 경로에서 오른쪽 아래 표적 필터의 그림·클릭·툴팁을 제거한다. 원본 목록299×350을 GUI2 반 배율로 적용하며 행은 두 줄 받침과 줄 사이 여백을 보존하도록33px에서40px로 확장한다. 작은 화면에서는 분류 머리줄을 남기고 가용 높이 안에서 각 목록을 스크롤한다. 클릭은 보이는 행에만 연결하고 우클릭·비활성 개시·응답 대기 중 반복 요청을 차단한다. 긴 습격 설명·상태는 두 줄의 원본 시작 위치를 유지하고 기본 크기로 가로 스크롤한다. 분류 펼침·접힘은 서버 요청을 만들지 않으며 활성/비활성 개시는 입력5 `kor-raids-launchable`의 권위 있는 값으로 검사한다.

진행 선의 원본 근거는 HOI4 `gfx/maparrows/maparrows.txt`의 `active_raid_air`와 `gfx/FX/maparrow.shader`다. 외부 팩의 패턴과 마스크를 실행 시 합성하며 mask_inner=(0.5,0.9,0), mask_outer=(1,0,0), body_uv_scale=0.4를 적용한다. 실제 마스크는 초록 채널만 있어 바깥 빨강 마스크는 투명하다. `fade_in=0.3`은 몸통 UV 시작 부분의 공간적 알파 경사이며 0.3초 애니메이션이 아니다. 합성 텍스처는 리소스 재로딩 때 해제·재생성한다. 원본은 body_width=(3,8), head_length=14를 선언한다. 현재 선 폭과 머리 길이는 GUI 화면 투영으로 근사하며 엔진의 거리별 폭 계산과 동일하다는 판정은 아니다.

해당 effect에는 `ANIM_TEXTURE` define이 없어 패턴 자체는 스크롤하지 않는다. Fx 강도와 선택 점멸은 0이다. 추가된 원본 `20261104110601_1.jpg`·`20261104110806_1.jpg`는 근거리 선을 제공하며, 준비·개시 조건 부족은 회색, 개시 가능한 준비 완료는 노랑, 진행 중은 빨강이다. 이전의 중성색 하나만 사용한다는 해석은 이 자료로 정정한다. `RaidView.Line.state`의 PREPARING/BLOCKED는 회색, READY는 노랑, ACTIVE는 빨강을 사용한다. `00_graphics.lua`의 원본 RGB/알파(0.7/0.7/0.7/1, 0.7/0.7/0/0.9, 1/0/0/0.9)를 패턴·마스크에 곱한다. 몸통과 머리는 같은 서버 툴팁을 표시하며 왼클릭은 `open`, 우클릭은 `locate`를 보낸다. 완료된 습격은 서버 `markers`의 조준선으로 표시하고 왼클릭 `open`, 우클릭 `dismiss`를 보낸다. 모든 동작은 현재 국가·세션·revision과 발급 항목·응답 대기 잠금을 유지한다. 엔진 조명·안개·깊이 검사와 실제 지도 전체·운영 접속·다중 사용자·직접 음향 청취는 별도 검증 대상이다.

분류 제목은 검은 배경 내부의 공통 세로 중앙 정렬을 쓴다. 두 줄의 습격 설명·상태와 긴 숫자 범위는 글자를 줄이지 않고 실제 글자 경계로 칸 안에서 스크롤한다. 항공 머리 줄은 원본 `ledger_airwing_header_entry`에 그림이 없으므로 추가하지 않으며 이름 x=16을 유지한다. 함선 머리 줄의 이름은 x=86이다. `cas` 그림은 항공 유형 요약과 개별 모델 그림과 구분한다.

집중 재현은 Java25에서 팩 선빌드 후 `test runGameTest remapJar -PhoiTestSuite=raids -PhoiServerSource=<고정 입력>/HOI -PhoiProtocolSource=<고정 입력>/HOI/protocol/src/main/java -PhoiResourcePack=<팩 ZIP>`이다. `HOI_AGENCY_OPERATION_PREVIEW`와 `HOI_DOCTRINE_MILESTONE_PREVIEW`는 `<고정 입력>/fixtures/commander-source-fixtures`를 지정한다. 현재 연결한 고정 입력은 `C:/Users/Kiruy/AppData/Local/claude-hoi/v24/server-input-4`이다. `RaidView`는 그 입력과 동기화하며 프로토콜 변경 때 새 고정 입력의 검증을 다시 수행한다. GUI2 두 해상도에서 실제 렌더와 모의 요청을 검사하며, 필터를 뺀 전체 검증·운영 접속·다중 사용자·직접 효과음 청취 결과와 구분한다. 종료 보고 원작은 20261104111042_1.jpg다. 보고 배경은 컨테이너700×525로 늘리지 않고 원본638×519의 반 배율로 그리며, 국기 프레임을 별도로 표시한다. 국기는 `GFX_flag_medium`의62×39 원본 크기에 맞춘31×20GUI 안에 그린다. 좌우 결과 본문은 위에서 시작하고 넘칠 때만 각각 스크롤한다. 출발지 이름·거리는 왼쪽 정렬, 긴 부대명은 같은 글자 크기로 가로 스크롤한다. 조직 중은 준비 시간·조직 중 문구만 표시하고 준비 막대는 숨긴다. 준비 중에는 남은 시간을 첫 줄에 표시한다. 결과 제목과 국기 사이 완료 문구는 각각 `Feedback.title`·`header`를 사용한다. 자동 실행은 `toggleAuto` 체크칸과 다섯 단계 `autoOptions`를 분리한다. 꺼져 있으면 서버 `autoDisabled`를 표시하고 선택 목록을 열지 않는다. 준비 스위치는 `Window.keys`의 0/1/2 프레임을 사용한다. 생성/결과 창의 부대명과 표적명(본문 안 원본 아이콘 포함)은 각각 검은 칸의 중앙에 맞추며 긴 이름은 같은 글자 크기로 스크롤한다. 부대 그림은 서버 `Unit.icon`·`Window.unitIcon`이 보낸 설계/사단 그림 키를 그대로 읽는다. 선택 탭은 원본 `GFX_sort_button_83x29`의 두 번째 프레임(금색 꺾쇠)을 사용한다. 출발지 이름은 서버의 `§H` 색을 보존하며 부대 보정값은 목록 스크롤바 왼쪽에 그린다. 추가 장비 이름·숫자는 종이 표 안쪽 여백에 배치하고 숫자는 원본 `raid_equipment_item`의 `format = right`대로 각 열 오른쪽에 맞춘다. 긴 이름은 같은 글자 크기로 스크롤한다. 목록 부대명은 그림 위의 10GUI 높이 영역에 같은 글자 크기로 그려 그림에 덮이지 않게 한다. 둥근 개시 단추에는 별도 글자를 덧씌우지 않고 서버 툴팁과 동작을 유지한다.

장부의 긴 이름·수량 스크롤은 장부/분야 탭을 연 시점을 기준으로 시작한다. 처음 1초는 앞자리에 머물고 이후 초당24GUIpx로 양 끝을 오가며, 위젯 재생성·서버 갱신은 시작 시점을 유지한다. 탭 재진입과 대상국 변경은 처음부터 표시한다. 습격 원본 행은33px·status_text y14지만 Minecraft 한글 기본 글꼴 두 줄은 이 안에서 받침이 잘리거나 서로 닿는다. 사용자 검토 요청에 따라 글자 크기는 유지하고 두 줄 시작을0/18px, 행 높이를40px로 확장한다. 원본 좌표를 그대로 복제한 배치와 구분하며 목록 스크롤 간격도 같은 행 높이를 사용한다. 필터 제거 입력과 장부 첫 화면/이동/재열기는 raids 집중 검사로 재현한다. 표적 hit 좌표 기록은 agency-map 검사 실행 폴더의 raid-target-hits-2560.json 및 1280.json에 남는다. 실제56×56 표본 밖에 투영된 원본 육지 표적은 전체 지형 배치 검증과 구분한다.


습격 계획은 서버 `planTitle`·`plans`·`PlanRow.targets`를 사용한다. 한 위치의 표적을 가리키면 원본 `GFX_construction_province_background` 안에 건물별 줄과 습격 유형 그림을 표시한다. 각 유형은 해당 `Target`의 툴팁·허용 여부·동작을 사용하며 계획 상자 안에 커서가 있으면 펼침을 유지한다. 지도 위 원형 표적과 완료 표식이 겹치면 완료 표식 입력을 우선한다. `RaidMapTest`는 합성 지도에서 네 선 상태, 근거리/원거리 폭, 몸통/머리 hit, 좌우 클릭과 중복 잠금, 완료 표식, 계획 옵션을 검사한다. 실제 운영 지도 전체의 지형·줌·다중 사용자 검증과 구분한다. `RaidScreenTest`는 입력 1의 PRC 장면도 별도로 캡처하며 원작과 다른 시각·수치·부대명은 수정하지 않는다.

원본 부대 그림 조사: `20261104110502_1.jpg`의 중국 H-6은 TFR `interface/Technologies_aircraft.gfx`의 `GFX_PRC_H6` → `gfx/interface/technologies/aircraft/PRC_H6.png`(181×65), 기존 팩 `production/source/5f0741af87b9c41c`다. 범용 `GFX_PRC_strategic_bomber1_medium`의 지상 H-6 사진과 구분한다. `20261103024535_1.jpg`의 한국 공수특전여단은 `gfx/interface/counters/divisions_large/unit_special_forces_icon.dds`의 기본 프레임(76×42), 기존 팩 `production/units/special_forces`다. 지도용 `unit_hud` 그림을 확대하는 대신 서버의 실제 부대/설계 키를 받아야 한다. 입력 2부터 해당 원본 그림 키를 수신하며 기존 팩의 종횡비 보존 렌더를 사용한다. 설계 그림이 없는 공군 부대의 서버 fallback 키는 그대로 보존한다.

출발지 후보 주 사선의 원본은 HOI4 `gfx/FX/standardfuncsgfx.fxh`의 `CalculateOccupationMask`, `constants.fxh`의 `SEC_MAP_TILE=6000`, `pdxmap.shader`의 `Input.uv2` 변환이다. 별도 원본 비트맵이 아니며 각도 π/8의 코사인·smoothstep과 거리별 알파를 계산한다. 기본 색은 (0,1,1,0.4), 호버 색은 (0.3,1,1,0.7)이다. `RaidPayloads.Highlight(session, revision, state, page)`의 주별 bounded page를 현재 setup의 후보 주와 대조하고 완성된 주만 게시한다. 다른 국가/세션/revision/차원과 후보 밖 주를 거절하며 갱신·권한 회수·차원 변경·setup 닫기 때 폐기한다. 출발지 탭에서만 표시하며 목록 또는 지도 위 해당 주를 가리키면 호버 색을 사용한다. 사선은 서버 상자의 윗면 안에서 렌더하고 기존 atlas 타일의 거리/시야 절두체 검사를 재사용한다. 원본 `CalculateOccupationMask`의 코사인·smoothstep·높이별 알파를 각 면의 4×4 분할 정점에 적용한다. 현재 지원하는 아시아 atlas의 crop(3744,384), 4원본px/블록과 원본 지도 텍스처 크기8192×2048을 사용한다. 원본 셰이더의 픽셀별 계산과 달리 정점 사이를 보간하며 Minecraft 지형 기준 상대 카메라 높이를 원본 단위로 환산한다. 따라서 전체 원작 엔진의 안개·조명·줌별 결과까지 동일하다는 뜻은 아니다. `RaidSourcesTest`는 실제 서버 후보50주 입력, 잘못된 세션/revision/주/차원, 순서가 바뀐 페이지와 부분 조립, setup 닫기와 지연 패킷을 검사한다. 화면 검사는 첫 후보 주의 형상을 x-228/z-140, y64.05로 평행 이동한 시험 지도이며 원본 운영 좌표 검증과 구분한다. 별도 서버 시험에서 생성된 두 입력의 세션 UUID는 서로 달라 시험에서만 같은 세션으로 연결한다. 집중 검사의 입력에는 `fixtures/raid-highlight-source-fixtures/prc-raids-setup-highlight.json`도 필요하다. 실제 운영 접속·다중 사용자·직접 효과음 청취는 미검증이다.

습격 생성·진행 창의 머리 부대명은 같은 크기의 한 줄 가로 스크롤을 사용한다. 표적 문구 앞 원본 자원 그림만 공통 9GUI 글리프의 1.5배로 표시하고 이름과 묶어 검은 칸 가운데에 맞춘다. 다른 본문·툴팁 글리프 크기는 유지한다. 그림 띠의 부대 그림은 원본 텍스처의 절반 크기로 그린다(공수특전여단76×42 → 38×21GUI, H-6 181×65 → 90.5×32.5GUI). 큰 텍스처는 기존 91×36GUI 경계 안으로만 축소한다. 가로 위치는 원본 `unit_picture`의 x35를 반 배율로 내림한 x17GUI를 유지한다. 보드의 장비 그림 세로 중앙 지시에 따라 `production/source/`·`production/units/` 그림은 생성·준비·결과 세 창에서 투명 여백을 제외한 실제 그림 중심을 지형 띠의 세로 중심에 맞춘다. 지형 높이는 해당 원본 텍스처에서 읽으며 plains는77px → 38.5GUI다. 장비가 아닌 예비 표식과 오른쪽 표적 그림은 기존 원본 좌표를 유지한다. 목록 그림 크기는 별도 원본 배치를 유지한다. `raids` 집중 검사는 GUI2 두 해상도에서 생성 창 최초 및 30틱 간격 다섯 시점의 이름 스크롤을 캡처한다. 입력 3의 공군기지 위치·거리(중국→센카쿠 첫 여섯 후보435/472/530/722/754/778km)는 서버 입력을 그대로 표시한다. 무필터 전체 검증은 비행단 이름 결정에 따른 최종 입력과 별도 시작 알림 뒤 수행한다. 실제 운영 접속·다중 사용자·직접 음향은 별도 검증이다.

습격 결과 지형 띠는 `GFX_province_view_frame_fail` 또는 `GFX_province_view_frame_success`의 원본 620×101 프레임을 원본 GUI 위치의 반 배율로 겹친다. 실패 프레임도 `import_raid_ui.py`에서 명시적으로 가져와야 한다. 생성·준비 창은 원본 배경의 기존 테두리를 유지하며 결과 전용 틀을 더하지 않는다. 생성 단추는 기존 글자 위치와 전체 가로 배치를 유지하고 지휘력 글리프의 실제 경계 중심을 뒤 문구의 경계 중심에 맞춘다. 부대/출발지 목록은 일부만 보이는 행도 145GUI 목록 경계에서 잘라 그리며 클릭·툴팁도 보이는 부분으로 제한한다. 스크롤바의 화살표·손잡이 크기는 유지한다. `raids` 검사는 제거한 필터 위젯이 없는지와 다섯째 후보 행의 21GUI 노출/입력 경계를 검사한다.

습격 성공 확률의 두 줄은 원본 사진 `20261104110524_1`·`20261104110545_1` 대조에 따라 기존 위치에서 함께2GUI 올린다(y134/147). 숫자 공통 보정·줄 간격·배경·툴팁 영역은 유지한다. 습격 준비율은 원본 게이지 틀(168,223; 141.5×14GUI) 안쪽의 실제 가시 영역을 기준으로 기존 글자만0.5GUI 내린다. GUI2에서 가시 내부16px와 글자14px 사이 위아래1px씩을 확보하며 글자 크기·게이지·클릭 영역은 유지한다. 원본 글자 크기와 한글 기준선을 유지하고 진행률·문구는 서버 응답을 따른다. 자동 실행 기준과 펼친 다섯 항목은 왼쪽 정렬하며 긴 문구만 같은 크기로 가로 스크롤한다. 닫힌 검은 칸은 기존 종횡비와 위치를 유지하고 원본 GFX_expand_button 19×19를 반 배율로 칸 안에 둔다. 체크박스 옆 자동 실행 문구와 기준 문구는 같은 y270·높이15GUI의 실제 글자 경계 정렬을 사용한다. 배경·입력 영역·주변 간격은 유지한다. raids 집중 묶음에서 켜짐/꺼짐·펼침/선택·우클릭·중복 잠금과 GUI2 두 해상도의 닫힌/펼친 목록을 확인한다. 운영 접속·다중 사용자·직접 음향은 별도 검증이다.

입력6의 전체 재현은 팩 선빌드 후 `powershell.exe -NoProfile -ExecutionPolicy Bypass -File C:/Users/Kiruy/AppData/Local/claude-hoi/v25/run-review-full-v25.ps1`를 사용한다. 이 스크립트는 `C:/HOI-client`에서 Java25 `test runGameTest remapJar`를 필터 없이 실행하고, 서버·프로토콜은 `C:/Users/Kiruy/AppData/Local/claude-hoi/v24/server-input-6/HOI` 사본을 참조한다. 수동 fixture는 같은 `server-input-6/fixtures`, `v24/client-fixtures`, `claude-hoi/cmp50-server`의 고정 입력을 사용하며 자동 생성 fixture와 구분한다. 서버 원본 체크아웃의 검증과 겹치지 않도록 실행 시간을 조율하고 종료까지 기다린다. GUI2 확대 비교는 각 해상도의 실제 캡처에서 잘라 입력 크기·자르기 좌표·파일 해시를 기록한다. 같은 크기의 UI는 해상도가 달라도 확대 픽셀이 같을 수 있으므로 확대 이미지의 해시만으로 입력 해상도를 판정하지 않는다.

무기대여 요청·받은 요청 응답·알림·상대국 군수의 입력 10 연결, 원본 GUI 주소와 집중 재현은 [무기대여 요청 UI](lend-lease-request-ui.md#입력-10-화면-연결과-재현)를 따른다.

## 원본에 없는 가운뎃점 안내 정리

`·`로 이어 붙인 클라이언트 생성 안내(“HOI 적용”, “미구현”, “선택 해제 · 이름” 등)는 원작 툴팁에 없으면 표시하지 않는다. 원작 키가 있으면 그 원문으로 바꾼다. 버튼이 비활성인 이유를 원작이 표시하지 않으면 사유 없이 비활성만 유지한다. 지휘 명령은 `Decision(false,false,"")`을 허용하고, 툴팁에는 원본 문구만 남긴다. 주요 대응은 다음과 같다.

- 지휘: 비행단·태스크 포스 행과 기지 머리행의 ×는 `DESELECT_UNIT`이다. 태스크 포스 `선택`은 원본처럼 툴팁이 없다. 부속 군 방패는 `ATTACHED_AIR_WING_DESC`와 `ATTACHED_AIR_WING_DELAYED_DESC`를 쓴다. 집단군 색 칸은 `SELECT_COLOR`이다. 입력 검사는 `air_base_close`, `air_wing_close/ID`, `naval_force_select/ID`, `naval_force_close/ID` 히트 ID로 행을 구분한다.
- 연구·중점: 화면 이름은 `RESEARCH_RESEARCH`, 빈 슬롯은 `RESEARCH_NO_RESEARCH`이다. 배타 연구는 `RESEARCH_EXCLUSIVE_INFO_TITLE_TOOLTIP`과 `RESEARCH_EXCLUSIVE_INFO_ENTRY`로 표시한다. 중점 소요 시간 칸은 `FOCUS_STATUS_TOOLTIP`·`_INPROGRESS_TOOLTIP`·`_COMPLETED_TOOLTIP`이고 저장 시간·정치력 내역은 표시하지 않는다.
- 산업: 생산 + 버튼은 `PRODUCTION_ADD_FACTORIES_CLICK`(해군 `PRODUCTION_ADD_NAVAL_FACTORIES_CLICK`)이며 Ctrl은 5, Shift는 10씩 바꾼다. ×10/×5는 `PRODUCTION_FACTORY_SCALE_UP`/`_DOWN`이다. 건설 수리 칸은 `REPAIR_ASSIGNMENT_INFO_FACTORIES`이다. 모병의 증원·업그레이드·주둔군 줄은 `REINFORCEMENT_DESC`/`_EMPTY`, `UPGRADES_DESC`, `GARRISONS_DESC`를 쓴다. 배치 라인은 `CONVEYOR_SET_LOCATION_DESC`, `CONVEYOR_SERIES_CURRENT_AMOUNT_DESC`, `CONVEYOR_LINE_DEPLOY_DESC`/`DEPLOYMENT_NOT_ALLOWED_REASON_TRAINING`, `CONVEYOR_LINE_CANCEL_DESC`를 쓴다. 무역 화면 머리는 `MenuTab.TRADE` 원본 이름을 쓴다.
- 외교·정치: 관계 행 툴팁은 서버가 보낸 원본 `<관계>_desc`이다. 세력 아이콘은 TFR `TFR_GLOBAL_ASSOCIATION_RANDOM_TT`이다. 권력 균형 막대는 `BOP_TOOLTIP`이다. 구간별 `BOP_RANGE_DESC`는 보정·규칙 자료가 없어 표시하지 않는다.

## 국가 화면 패널 배경

메뉴 10개 탭(정치~장교단)의 창은 `UiMenuStyle.frame`이 원본 좌표계를 GUI로 옮긴다. 배율 s=패널 폭÷원본 창 폭(2560×1440 GUI2에서 모든 탭 0.5, 원본 1px=물리 1px), 창 원점은 원본 `show_position` x×s(정치 −5, 건설 −9, 나머지 −6px)와 `HudMenuBar.windowTop`=39×HUD 배율 GUI(원본 y=78px)이다. 반 GUI 좌표는 `Frame.art`·`Frame.tile`이 pose 이동으로 그대로 그리고 클릭 영역만 반올림한다. 창은 원본처럼 화면 아래 끝까지 차며 아래 테두리가 보인다(왼쪽 바깥 실선은 원점 −6px로 자연히 화면 밖).

TFR 타일(`tiled_window_thin_border2`·`tiled_window2_1b_border`·`tiled_plain_bg`·`tiled_paper_w_frame_one_border` 등)은 원본 dds가 190×190이지만 gfx는 192로 선언되어, 원작은 192 기준 9분할(가운데 반복 64px, 오른쪽·아래 조각은 실제 텍셀 128~189만 그리고 마지막 2px 투명)로 그린다. 원작 사진에서 결정 창 마지막 불투명 행은 화면 y1434, 군수 창 아래 밝은 선은 y1431로 192 기준과 일치한다. `UiAssets.tiledNineSlice(…, declared)`가 이 규칙을 따르며 `UiMenuStyle.TILE`(192)을 쓴다.

제목 띠는 `GFX_header_bg`(팩 `decision/ui/window_header`, 794×41)를 원본 (x,y)에 원크기×s로 그리고 창 폭에서 자른다(무역 창은 원본 `clipping = no`라 오른쪽 4px가 창 밖으로 나온다). 제목은 MC 글꼴 11/9배로 원작 글자 중심 행에 맞춘다: VCR02_24 상자 y+11, VCR02_26(생산·장교단) y+12, 무역(높이 25 상자) y+12, 글자 왼쪽은 상자 x+2(원작 20261014201842·20261024072637·20261118205348 등 실측 88~106행). 무역은 원본처럼 `TRADE_ECON` "경제"(75,6)와 `TRADE_TRADE` "무역"(332,6)을 각 217px 가운데에 둔다. 닫기 단추는 금속 바탕 없이 `GFX_closebutton`(팩 `menu/close`, 32×33)을 원본 위치에 원크기×s로 그려 머리 띠 오른쪽 칸(텍스처 x746~789) 안에 들어간다. 결정 창에서 X 글자 열 507~526·행 93~112가 원작과 같다.

| 화면 | 원본 창 | 배경 | 원점 x | 제목 띠 (x,y) | 제목 (x, 글자 중심 y, 폭) | 닫기 |
| --- | --- | --- | --- | --- | --- | --- |
| 정치 | `countrypoliticsview` 726 | `GFX_tiled_window_thin_border2` | −5 | (−70,4) | (40, 21, 400) | (683,6) |
| 무역 | `countrytradeview` 620 | `GFX_tiled_window_1b_border` | −6 | (−170,6) | (75·332, 18, 217) | (582,8) |
| 건설 | `countryconstructionsview` 550 | `GFX_tiled_window_thin_border2` | −9 | (−248,7) | (45, 19, 240) | (507,9) |
| 생산 | `countryproductionlineview` 550 | `GFX_tiled_plain_bg` | −6 | (−245,7) | (45, 20, 440) | (507,9) |
| 모병 및 배치 | `countrydeploymentview` 550 | `GFX_tiled_window` | −6 | (−248,7) | (45, 19, 440) | (507,9) |
| 군수 | `countrylogisticsview` 560 | `GFX_tiled_window_thin_border2` | −6 | (−240,7) | (45, 19, 240) | (514,10) |
| 정보기관 | `countryintelligenceagencyview` 550 | `GFX_tiled_bg` | −6 | (−245,7) | (45, 19, 440) | (507,9) |
| 장교단 | `countryofficercorpview` 550 | `GFX_tiled_bg` | −6 | (5,7) | (45, 20, 440) | (507,10) |
| 사건과 결정 | `countrydecisionview` 550 | `GFX_tiled_window2_1b_border` | −6 | (−248,7) | (45, 19, 440) | (507,9) |
| 연구 | `countrytechnologyview` 550 | `GFX_tiled_window_thin_border2` | −6 | (−245,7) | (45, 19, 440) | (507,9) |
| 외교 | `countrydiplomacyview` 550 | `GFX_tiled_bg` | 0 | 없음 | 14 | 금속 단추 |
| 점령지 | `countryoccupationview` 550 | `GFX_tiled_bg` | 0 | −249 | 45 | 금속 단추 |

패널 폭(`UiPanelLayout`)은 2560 GUI2에서 정치 363, 무역 310, 군수 280(원본 560), 나머지 275이다. 정보기관은 생산·건설·장교단과 같은 275 분기를 쓰고, 1280×720 GUI2의 축소 폭(정치 248, 무역 244, 군수 300, 연구 등 184)은 유지한다. 외교·점령지는 메뉴 탭 범위 밖이라 이전 그리기를 유지한다. 모병 오른쪽 사단 편제 칸은 원본 `templatedeploymentwindow`의 `GFX_tiled_window_1b_thin_border`를 쓴다. 국가 선택 미리보기는 화면 가운데 놓이므로 정치 창 배경을 네 테두리 모두 보이게 그린다.

사건과 결정 목록은 원본 `decision_grid_container`(5,45, 폭 544, `GFX_tiled_paper_w_frame_one_border` 192 타일)이고 바닥은 원작 사진의 밝은 선(화면 y1428=텍셀 178)에 맞춰 창 높이+2px이다. 항목은 grid x14에서 원본 높이로 쌓는다: 사건 머리 65(그림 515×83), 사건 행 33(512×33, 아이콘 (29,+3), 이름 x64, 남은 일수 오른쪽 끝 514), 분류 머리 60(그림 516×53을 (10,+8), 아이콘 (24,+16), 이름 (88~453) 가운데·글자 중심 +28.5, 추적 (449,+24) 23×23, 접기 (484,+23) 24×25), 분류 설명은 `GFX_tiled_decisions_bg_small`을 (14,−1) 폭 510에, 본문 글 x28 폭 470, 결정 행 39(대상 결정 40, 그림 512×40, 대상은 (13,−1)), 분류 끝 20. 결정 행: 아이콘 가운데 (44,+19)(시한 (46,+20), 대상 왼쪽 위 (29,+0)), 이름 x69(대상·시한 x76), 비용 오른쪽 끝 446(VCR02_12 글자 중심 +18), 추적 점 (451,+7), 선택 마름모 (479,+6)(대상 +4)는 실행 가능 프레임 1·불가 프레임 0, 진행 막대 `GFX_decision_item_progress_*` 502×10을 (19,+28)(시한 16, 대상 23). 남은 기간이 있는 행은 선택·추적 대신 `decision/timer`를 (506,+15) 가운데 0.95배(대상 0.85배)로, 남은 일수를 오른쪽 끝 489에 금색으로 둔다. 팝업 체크박스(34×30)는 원본 (116·256, +10/+38)에서 사용자 규칙대로 1 GUI 아래, 글자는 체크 표시 중심에 맞춘다. 원작 KOR의 "TOP SECRET" 서류철은 TFRKP 스크립트 GUI `Documents_Button_GUI`라 창 배경이 아니며 구현하지 않았다.

연구 개요는 원본 `countrytechnologyview` 슬롯 창이다: 탭 `GFX_tab_large`(팩 `mio/ui/gfx_tab_large/frame_0`) (150,44)에 `RESEARCH_TAB_SLOTS` "연구 슬롯", `research_slots` (0,80) `GFX_tiled_bg`, `slots_window` (0,220) `GFX_tiled_window_thin_border2`, 배너 `GFX_research_top_win` (5,90) 원크기, 혜택 줄(글자 (55,235)·(300,235), 아이콘 (185,232)·(427,232), 값 (220,235)·(460,235)), 슬롯 (15,255) 간격 100. 빈 슬롯은 `GFX_empty_research_slot_glow`(팩 `research/ui/empty_slot_glow/frame_1`)를 (5,5)에 1초 주기로 엇갈려 그린다.

생산은 원본 `countryproductionlineview`다. 위쪽 `production_win_top`(팩 `production/ui/win_top`, 550×253)을 (0,44)에 깔고, 자원 7칸 `GFX_mini_tooltip` 70×30을 (25+70i,61)(아이콘 +(4,2) 27×27, 값 +35 폭 33), 보정치 아이콘·값을 조선소 (22,102)/50·산출 (108,97)/137·한도 (199,101)/227·전환 유지 (281,101)/309·성장 (366,102)/396·폭격 취약도 (446,97)/475에, 공장 요약 아이콘 (15,134)·(258,136)·(440,136), 라벨 `PRODUCTION_OPERATIONAL_ARMS_FACTORIES_LABEL` (45,137) 폭 135·`PRODUCTION_OPERATIONAL_DOCKYARDS_LABEL_SHORT` (290,137) 폭 80, 값 (180)·(375)·(473)에 둔다. 추가 단추 5개는 (25·105·185·350·430, 166) 81×41이고 원본처럼 80px 간격이라 1px 겹치는 열은 뒤 단추가 받는다. 필터 12개는 1줄 y223(x 13+85k, 숫자 오른쪽 정렬 끝 x+78), 2줄 개장 (14,252)·나머지 y255(숫자 가운데)다. 목록 바탕은 `GFX_tiled_window2_1b_border`(팩 `diplomacy/dialog/panel_dark`)를 (0,253)부터 그리고, 행은 (14,294)에서 111px(접힘 63px) 간격, 스크롤 영역은 y289부터 창 아래−13, 스크롤바는 x527이다. 드래그 중인 행 뒤는 같은 목록 바탕으로 덮는다.

보정치 값 서식은 원작 사진 20261024072637·20261024075214·20261028131802·20261029102910·20261104110448에서 확인했다: 한도는 양수에 부호가 없고 나머지는 +/−를 붙이며, 0 이상 초록·음수 빨강이다. 공장 폭격 취약도(`industry_air_damage_factor`, 라벨 "공장 폭격 취약도")는 음수 초록·0 노랑(+0%)·양수 빨강이다. 공장 요약 값은 모두 가동이면 초록, 남는 군수공장은 노랑(17/18), 남는 조선소는 빨강(6/29·12/28), 수리 조선소 0/N은 초록이다. 남는 조선소가 노랑인 경우와 양수 폭격 취약도는 원작 사진이 없어 확인하지 못했다. 툴팁은 원본 GUI 그대로 공장 아이콘 3개와 군수공장 라벨이 `arms_factory_desc`, 조선소 라벨 `dockyard_desc`, 조선소 값 `PRODUCTION_WORKING_FACTORIES`, 수리 값 `PRODUCTION_WORKING_DOCKYARDS_FOR_REPAIRS`이고, 군수공장 값과 추가 단추 5개(TFR `pdx_tooltip` 주석)는 툴팁이 없다. 필터 숫자 툴팁은 1줄 6개만 `PRODUCTION_FILTER_ARMS_FACTORIES`·`PRODUCTION_FILTER_DOCKYARDS`이다. 필터는 클릭으로 해당 분류를 숨기거나 보이고, Shift+클릭은 나머지를 숨기거나 모두 다시 보이며, 우클릭은 해당 분류 행을 모두 접거나 펼친다(`click_checkbox` 소리). 숫자는 분류에 맞는 라인의 공장 수 합계다. 새 장비 반짝임 `GFX_prod_new_equipment_glow`는 새 장비 확인 상태가 서버에 없어 그리지 않는다. 설계창·군수산업체 목록·장비 선택·배치 항구 팝업과 상단 지표 스크롤 판정은 메뉴 창 원점(`HudMenuBar.windowTop`)이 아니라 상단바 아래(`HudMenuBar.height`)를 기준으로 한다.

건설은 원본 `countryconstructionsview`다. 요약 바탕 `GFX_construction_screen_top_bg`(팩 `construction/ui/summary_background`, 433×98)를 (5,37)에 둔다. 머리 띠에는 수리 우선 공장 칸(상자 `GFX_generic_box_80` (365,13), 렌치 공장 (333,14), 숫자 (380,18) 폭 50 가운데, − (361,13)·+ (423,13): Ctrl은 5, Shift는 10과 `subtract_5/10`·`add_5/10` 그림, 툴팁 `REPAIR_ASSIGNMENT_INFO_FACTORIES`·`PRODUCTION_SUB/ADD_FACTORIES_CLICK`), `REPAIR_ORDER_OPEN` 단추 (230,11) 94×31, 최소화/확장 `GFX_production_line_arrow_left/right` (457,12) 32×28이 있다. 요약은 민간공장 (17,55)과 값 (38,58) 폭 87 가운데, `from_trade`·`owned` 라벨 (122,50/70)과 값 (187) 폭 70 오른쪽, 건설 속도 (288,61)·값 (278,60) 폭 70 오른쪽, 에너지 `GFX_mini_tooltip` (353,56) 70×30이다. 열 제목은 y106에 위치 (20,폭150)·작업 (180,80)·공장 아이콘 (274,106)·`CONSTRUCTION_FACTORIES_LABEL` "#" (250,120)·우선순위 (330,110) 가운데다. 목록은 (0,130) 폭 440 `GFX_tiled_window_thin_border2`, 스크롤 영역 y143~창 아래−15, 스크롤바 x417이고 항목은 (14,148)부터 52px 간격이다. 소비재·무역 상품·국가 계획 특수 줄(`GFX_construction_special_entry_bg`, 이름 (20,16), 그림 중심 (210,26), 숫자 (254,17) 폭 50)이 첫 줄들이고 다음 수리 줄, 다음 건설 줄이다. 건설 줄은 이름 (20,4) 폭 150, 그림 (172,5) 45×45, 수량 (162,29) 오른쪽, 수량 +/− `GFX_zoom_in/out` (214,3)/(214,27) 0.9배(+는 Shift로 최대), 공장 "N/20"(TFR `MAX_CIV_FACTORIES_PER_LINE`=20)과 공장이 있을 때 `GFX_ongoing_contruction_anim` (254,9) 12프레임 24fps, 진행 막대 (249,36), ↑↓ (318·341,14), 취소 (370,14)다. 수리 줄은 `GFX_building_repair`(2프레임 1.5fps 교차)를 (174,7)에, 순번 원 `GFX_generic_circle_background` 5번 프레임 1.3배를 (372,16)에, 수동 배치면 `GFX_locked_icon`을 (345,6)에 그리고 끌어 놓으면 수동 배치를 요청한다. 팔레트는 (438,48) 컨테이너의 원본 배경 (438, 54/200/516)·분류 아이콘 (443, 57/203/521)·2열 46×44 격자 (446, 99/244/558)이고 선택 건물은 흰 테두리 대신 `GFX_construction_glow_overlay`(2프레임 1.5fps)로 표시한다. 화면 아래로 넘칠 때만 팔레트가 세로로 스크롤된다.

수리 우선순위 창은 원본 `repair_order_window`(화면 (530,78), 295×640, `GFX_tiled_plain_bg`, 끌어서 이동)다. 제목 `REPAIR_ORDER_HEADER` (0,12) 가운데, 닫기 (−37,9), 설명 (10,42), 안내 (10,86), 열차포 단추 (6,152) 46×46(추가/제거 툴팁), 목록 (0,194) 항목 260×53(`GFX_tiled_decisions_bg_small`, 제거 X (8,11), 순번 (48,18), 아이콘 (78,11) 0.7배, 이름 (120,8) 폭 130), `REPAIR_ORDER_RESET_QUEUE` (21,−44) 123×34, `REPAIR_ORDER_CLEAR_PLACEMENT` (151,−44) 123×34다. 창이 열려 있을 때만 팔레트 클릭은 그 건물 유형을 대기열에 넣고 우클릭은 빼며, 대기열 순번을 팔레트 아이콘 오른쪽 위 (+23,0)에 표시한다(원작 20261118205433 창 닫힘에는 순번이 없고 20261118205435 창 열림에는 철도에 "1"). 팔레트 건물 툴팁은 창이 닫혀 있으면 서버 원문 그대로(끝에 빈 줄 + `CONSTRUCTION_MACRO_DESC` "클릭하여 이 건물을 선택한 다음 …"), 열려 있으면 같은 원문에서 끝의 빈 줄과 건설 안내만 빼고 `REPAIR_ORDER_CLICK_ADD`("§G클릭§!하여 §Y수리 우선순위 대기열에 추가§!")를 바로 붙이며, 이미 대기열에 있으면 `REPAIR_ORDER_RIGHT_CLICK_REMOVE`를 한 줄 더 붙인다(원작 20261121090934 열림·20261121090939 닫힘). 대기열 항목은 끌어서 순서를 바꾼다. 최소화는 GUI `minimized_offset`(420)만큼 창을 왼쪽으로 옮겨 팔레트·최소화/확장·닫기만 남기고 지도 클릭 영역을 넓힌다. 최소화 상태의 원작 사진이 없어 이 이동은 GUI 정의에서 추론했다.

무역은 원본 `countrytradeview`다. 머리 띠의 `GFX_econ_switch_button`(팩 `trade/ui/econ_switch/frame_0|1`, 482×35)을 (70,4)에 그리고 한 단추로 경제/무역을 바꾼다(툴팁 `open_econ_button_tt`). 무역 하위 탭은 머리 칸 `GFX_intel_header_bg` (46,42)에 `TRADE_CIV_FACTORIES` (71,54) 폭 150, 민간공장 아이콘 (236,49)·값 (270) 초록, 소비재 (300,50)·무역 공장 (370,48) 아이콘 0.7배·값 (340)/(410) 노랑, `TRADE_CIV_FACTORIES_AVAIL` (434) 폭 120이다. 자동 수입 줄은 `GFX_tiled_decisions_bg_small` (20,80) 580×50에 체크 (28,89), 라벨 (60,95), 예산 상자 `GFX_generic_box_80` (250,92), 슬라이더 (340) 폭 145(트랙 y99·손잡이 y97, 놓을 때 전송, ←/→는 공장 1개), `우선순위` (490,89) 94×31이다. 자원 표 `GFX_trade_resource_bg` (17,130)의 줄 제목은 (22, 168+19k) 폭 78 오른쪽 정렬, 자원 열 원점은 (108+68i,131)이고 칸은 +5부터 폭 67이다: 아이콘 (+6) 27×27, 잔액은 아이콘 오른쪽 (+34) 폭 37, 값과 잉여분은 칸 폭 (+5) 65에 오른쪽 정렬한다. Minecraft 글자가 VCR 글꼴보다 넓어 원본 상자 폭(값 50·잔액 57) 대신 칸 전체를 쓰고 잔액이 아이콘과 겹치지 않게 했다. 값은 양수 초록·0 흰색·음수 빨강, 잉여분은 양수 흰색·음수 빨강, 석유는 `OIL_NEED_TEXT` C:/M: 두 줄(줄 간격은 MC 글자 높이)과 흰 잔액이다(원작 20261119021254). 수입 취소 `GFX_cancel_import_button` (138+68i,303) 31×31은 수입이 없거나 자동 수입 중이면 회색이다. 자원 탭 `GFX_TRADE_resource_btn` (11+85i,334) 90×53(아이콘 (+32,343)), 필터 바탕 (13,373), 지역 필터 9개 `GFX_diplo_filter_entry` (107+150c,385+35r) 123×34(선택 frame_0), 정렬 4개 y501(클릭 영역은 목록 위 y529까지), 목록 (15,529) 행 576×45(가져오는 중 초록·경로 없음 빨강), 스크롤바 x591, 휠 45px이다. 창이 낮아 목록이 3줄 미만이면 제목 아래 본문 전체가 함께 스크롤한다. 사용자 지시(2026-11-20)로 글자·아이콘·단추는 원작 글꼴의 높이 대신 자기 배경 칸의 세로 중앙선에 둔다: 경제/무역 제목 y21(전환 상자 안쪽 6~27행), 민간공장 라벨 63(밝은 상자 12~29행)과 머리 값·아이콘 62, 자동 수입 줄 105(체크·라벨·예산·손잡이·우선순위), 표 머리 149·각 줄 176+19k·잉여분 282.5, 지역 필터 글자 상자+16.5, 정렬 글자 517, 국가 행 이름 +21.5·값 +22, 우선순위 창 제목 22·연료 라벨은 체크 중심 124·항목 +26.5. `IndustryTradeTest`가 GUI2 캡처에서 주요 글자와 손잡이의 세로 중심이 각 칸 중앙과 0.75 GUI 안에 있는지 확인한다.

수입 우선순위 창 `auto_import_priority_window`는 원작 화면 (762,75)(무역 창 기준 (768,−3)) 295×640이며 끌어서 옮긴다(원작 20261119021257). 제목 (0,12), 닫기 (−38,8), 설명 (10,42) 폭 260, 연료 체크 (10,108)·라벨 (44,113)이고 설명이 MC 글자로 길어지면 아래 요소를 그만큼 내린다. 항목 260×53은 순번 원 (10,18) 1.3배, 아이콘 (40,13), 이름 (72,8) 폭 82, −/+ `GFX_subtract_one`/`GFX_add_one` (168,14)/(228,14)(Ctrl 5·Shift 10), 값 (192,18) 폭 36이며 끌어서 순서를 바꾼다. 초기화는 (86,−44) 123×34, Esc는 무역 화면보다 이 창을 먼저 닫는다. 표 칸 툴팁의 엔진 배정은 공개되지 않아 원문 키의 의미로 배정했다: 추출 `TRADEVIEW_RES_PRODUCED_DETAILS`, 수입 `TRADEVIEW_IMPORT_DETAILS_*`, 수출 `TRADEVIEW_RES_EXPORT_*`, 생산 `TRADEVIEW_RES_USED_DETAILS`, 잔액 `RESOURCE_DETAILS_NET_*`, 잉여분 `RESOURCE_DETAILS_SURPLUS/DEFICIT/FUEL`. 주별 추출 줄과 생산 라인별 소비 줄은 서버 값이 없어 넣지 않았다. 지역 필터를 여러 개 고르면 합집합이며 원작 동작은 확인하지 못했다. `trade` 묶음(`IndustryTradeTest`)이 배치·자동 수입·슬라이더·우선순위 창 입력·필터·정렬·수입 취소·휠·작은 창 경계를 검사한다.

경제 하위 탭은 서버가 보낸 TFR `econ_tab_window_gui` 패널(`IndustryView.economyPanel`)을 결정 화면과 같은 `DecisionPanel` 렌더러로 무역 창 (0,40)에 폭 624 원본 좌표 그대로 그리고 창 아래 끝에서 자른다. 탭이 열리면 `ECONOMY` 1, 무역 탭으로 돌아가면 0을 보내며, 화면을 닫거나 다른 국가 군수 보기로 바뀌면 서버가 구독을 지운다. 패널 단추는 보이지 않는 클릭 영역(효과음 `ui.click`)으로 `ECONOMY_CLICK`에 원본 요소 이름(목록 행은 `이름@행번호`)을 보내고 비활성 단추는 누를 수 없다. 툴팁은 결정 패널과 같은 `DecisionText` 툴팁이다. 원본 모서리 타일(`corneredTileSpriteType`) 배경은 팩 `decision_assets.json`의 `cornered` 테두리(`GFX_tiled_plain_bg` 64, `GFX_tiled_peace_upper_bar` 52)로 9분할해 그리고(모서리는 그림 크기의 절반과 칸 크기의 절반을 넘지 않음), 이런 `background`는 HOI4처럼 부모 창 크기를 채운다. 음수 크기는 부모 크기에서 빼며, 원본 `maxHeight`가 글자 한 줄보다 낮은 칸(`자동 상환`의 10)은 한 줄 높이까지 그린다. 목록 행처럼 원본 단추가 겹치는 곳(경제 행동 행 간격 57·단추 62, 두 투자 단추 2px)은 나중에 그린 단추가 겹친 부분을 맡도록 앞 단추의 클릭 영역을 자른다. 내용이 창보다 길면 휠로 마지막 단추까지 스크롤한다. 패널 fixture는 `DecisionPresentationTest`가 `decision-economy-panel.json`으로 내보내며 `trade` 묶음도 `HOI_DECISION_PREVIEW`를 쓴다. `IndustryTradeTest`가 구독 요청, 대출/상환 단추의 활성 상태와 위치, GDP 칸 글자, 대출 클릭 요청, 해제 요청을 검사한다.

## 실패·거부·안내 메시지

클라이언트와 서버는 채팅·액션바·화면 하단 안내 줄로 실패·거부·일반 안내 메시지를 보내지 않는다. 막힌 입력은 아무 동작도 하지 않고 비활성 상태와 원본 툴팁만 남긴다. 서버 응답의 `message` 필드(연구·생산·건설·교리·중점·결정·정보기관·국가 행동)는 그리지 않으며 배치 지정 완료, 포획 지휘관 목록 자동 갱신 중지 같은 내부 판정에만 쓴다. 배치 장소 지정 모드의 하단 조작 안내 줄도 그리지 않는다. 응답을 해석하지 못하면 해당 화면만 닫고 클라이언트 로그에 남긴다. 불러오는 중 표시와 빈 목록 문구는 메시지가 아닌 화면 상태라 유지한다.

## 원본 근거가 필요한 클라이언트 표시

설치 원본은 읽기 전용이다. 아래 경로의 HOI4는 Steam `steamapps/common/Hearts of Iron IV`, TFR·TFRKP·한글 패치는 각각 Workshop 3350890356·3617039195·3411756715를 뜻한다. 원본의 존재, 외부 팩 등록, 실제 렌더 연결과 원작 런타임 일치를 구분한다. 기존 팩만 사용하는 작업에서는 자산을 새로 가져오지 않는다.

| 항목 | 현행 연결과 남은 근거 |
| --- | --- |
| 가운뎃점 안내의 남은 대조 | **근거 대기(사용자 원본 사진).** 배치 장소 선택 하단 안내, 생산 자원 줄·무역 자원 칸 툴팁, 증원 요청 장비 줄, 배치 라인 훈련·장비 게이지, 특수부대 칸, 편제 카드, 해군 수리 항구 이름 형식, 주 통제권 이양, 정당화 아이콘 뒤 줄, 타국 안정도·전쟁 지지도 칸, 해군 카드 +·초상·×, 항공모함 기지명, 장교단 고문 역할, 예비 함대·교전 수칙 수송선, 함선 분리 행, 공역 결과 툴팁, 복수 군 작전명, 지도 육군 카운터, HQ 편제 삭제, 연구 건물 레벨, 주 기온 아이콘, 지도 초계 함대 글자는 원작 화면 확인 전까지 기존 문구를 유지한다. 태스크 포스 이름의 `함대명 · 순번`은 원작 OOB 이름 저장 구조가 없어 유지한다. |
| 군정신 선택 칸 | **근거 대기(외부 자산 미수입).** HOI4 `interface/countryofficercorpview.gfx:353`은 `GFX_spirit_entry_bg_strip`, 4프레임을 선언한다. 유효 TFR 원본은 `gfx/interface/officer_corp/spirit_entry_bg_strip.dds`, 사용처는 TFR `interface/countryofficercorpview.gui:1725` 등이다. 기존 팩 ZIP에 해당 sprite가 없어 다른 배경으로 대체하지 않는다. 팩 수입과 렌더 연결은 별도 작업이다. |
| 국회·결정 | 동적 카테고리 우선순위는 서버가 확정한 순서만 따른다. 원본 TFRKP `interface/TFRKP/TFR_Korea_NatAsm.gui:81-108`은 member_container x100·grid x70·icon x−23을 선언하며 수신 레이아웃과 일치한다. `20261028003520_1.jpg`의 왼쪽 목록과는 다르므로 캡처 당시 GUI·scripted GUI/버전 및 엔진 grid 원점 확인 전 좌표를 임의 이동하지 않는다. 정당 툴팁은 원본 banner 300×127을 유지하지만 기본 글꼴의 본문 폭·그림 정렬은 **근거 대기**다. 원본 `common/scripted_guis/TFR_scripted_guis_NatAsm.txt`와 한글 패치의 원문·TFRKP banner 정의를 함께 대조한다. |
| 기뢰·군종 임무 선택기 | **근거 대기.** 원본 임무 그림과 현재 직접 명령 단추를 유지하고 발급 대화창의 일반 행과 구분한다. HOI4 `common/defines/00_defines.lua:1935-1940`의 최대 기뢰 수·설치/소해 속도·소해 벌점 감소 계수만으로 최대 불이익 문구의 런타임 수치/반올림을 확정할 수 없다. `interface/navalcombatview.gui`, `interface/naviesview.gui` 및 한국어 localisation의 기뢰 문구를 조사했으며 엔진의 실제 최대치 툴팁 관측이 필요하다. |
| 에이스 정밀도 | **근거 대기.** HOI4 `common/defines/00_defines.lua:1382-1383`은 기준 비행단100·최대 배율2, `localisation/korean/air_l_korean.yml:305-306`은 기준 정수·배율0% 문구를 명시한다. 이것만으로 개별 능력치의 배율 적용·소수 정밀도 전체를 확정하지 않는다. 서버가 보낸 에이스 원문 툴팁을 그대로 표시하며 클라이언트에서 재반올림하지 않는다. |
| 교리 통달 | TFR `interface/doctrines/doctrines.gfx:2-55`의 군종별 24프레임·24fps·looping을 기존 팩 프레임에 연결한다. 필요 통달치 글자는 기존 노란색을 유지한다. 군종별 숫자/달성 색은 GUI·사진에 없는 엔진 판정이므로 **근거 대기**이며 보상 배경의 확인된 군종 프레임과 혼동하지 않는다. |
| 공군 효율 | **근거 대기.** TFR `interface/airselectionview.gui:495-496`과 `GFX_air_mission_efficiency_strip`은 위치·그림을 제공하지만 효율 경계와 시계 프레임3/4/5 선택 조건을 제공하지 않는다. 현재 1/3·2/3 표시 구간은 클라이언트 규칙으로 남기며 원작 엔진 규칙이라고 주장하지 않는다. |
| 외교 단추 shader | **근거 대기.** TFR `gfx/FX/buttonstate.shader:90-108,163-200`의 UV·disabled/hover 식과 `UiDiplomacyAssets`의 색상식은 구분해 유지한다. 스프라이트 분할·cornered tile에서 엔진이 전달하는 UV/Offset과 기본 selected 효과는 GUI/GFX만으로 확정하지 못했으므로 원작 호버·눌림의 같은 프레임 캡처가 필요하다. |
| 배율·잘림 | 부대 HUD의 `UiUnits`와 상단 지표의 가로 스크롤을 유지한다. 다른 메뉴 전체를 임의 축소하지 않는다. 1280 GUI2의 HQ 초안은 원본 사단 설계 영역을 장교단 위에 겹쳐 표시하며 배경을 덮는 것과 조작부가 화면 밖으로 나가는 것을 구분한다. 사단 긴 능력치 이름은 기존 글꼴/값 열을 보존해 가로 스크롤하고 전체 원문은 툴팁으로 읽는다. 플레이어 배율을 바꾸지 않는다. |
| 스토리 지도 goto | **서버 계약 필요.** `MapFocus.State`는 표시 신호이며 현행 클라이언트에는 소비 수신기가 없다. 지도 클릭은 서버 플레이어 시점에서 해석하므로 Camera만 이동시키지 않는다. 국가·지도 차원·유효 범위·신호 수명을 검증하고 관측 위치와 청크 로딩을 함께 이동하는 서버 요청/응답이 필요하다. 결정의 HIGHLIGHT와 별도다. |

지휘관 보고서의 원작 런타임 자동 높이, 여러 선택지·그림 없음·초장문·긴 슈퍼 인용문은 원본 GUI와 클라이언트 경계 검사만으로 일치 판정을 내리지 않는다. 군정신 새 자산, 효율 임계값, 에이스·기뢰 반올림과 외교 UV를 임의로 보완하지 않는다. 실제 클라이언트의 운영 접속·저장/재접속·멀티플레이와 직접 효과음 청취는 렌더 fixture 검사와 별개다.

`-PhoiTestSuite=air-scales`는 서버 fixture 없이 합성 비행단 두 기지의 실제 입력·렌더를 GUI2 2560×1440·1280×720, GUI3 1920×1080, GUI1 2560×1440에서 검사한다. 기지 수용량·CAS/요격 순서, 기지별 창설·선택 해제, 개별 비행단 출격 시간·에이스/보충 정책 요청과 툴팁의 화면 경계를 확인하고 검사 전 배율을 복원한다. 원작 결과 행·상태 색 임계값·항모 함재기 위젯의 사진 일치를 증명하는 검사는 아니다.

교리 `doctrine` 묶음의 애니메이션 회귀는 원본 24프레임의 존재와 1초 반복을 확인하고, 서버 형식 입력의 통달치만 절반으로 바꾼 **합성** 게이지를 고정 시각0/500ms에 실제 렌더해 채움 영역의 픽셀 변화를 비교한다. 이 표시 검사는 서버 통달치 획득·보상 실행·FPS/MSPT 개선을 뜻하지 않는다.

Decision images and target flags stay at the row's left edge; only the name text scrolls, clipped after them. The cost icon and value are right-aligned in their own clipped cell and never scroll. Country-outline text icons retain their original glow and pixels; per-icon bitmap height/ascent matches the visible core to the adjacent cap height. Country target flags fit their visible bounds to `font.lineHeight` and reserve their aspect-correct width plus four pixels. Inline `[TAG.GetFlag]` text uses the original medium country flags at the same height, including tooltip and detail rendering, with cosmetic flags fitted to the glyph advance. Styling an explicit bitmap icon again preserves its source position and does not infer an extra leading keyword icon; label-only text retains inference.
