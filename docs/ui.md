# 화면과 표시 기준

[README](../README.md) · [구조와 검증](refactoring.md) · [음악](audio.md)

## 공통 기준

- GUI2를 크기·간격·정렬의 기준으로 사용하며 다른 배율·창 크기는 호환성을 검사합니다.
- 원본 이미지의 비율·프레임·투명도를 보존합니다. 사진·아이콘·텍스트는 각 셀의 경계 안에 표시합니다.
- 한국어는 TFR 한글패치의 키와 국가별 덮어쓰기를 따릅니다. 명령·저장 ID는 번역하지 않습니다.
- 툴팁의 원문 색상·빈줄·소수 자릿수·아이콘을 유지하고 단독 `---`는 빈줄로 표시합니다. 효과 수치는 서버 계산을 사용합니다.
- 미상 값을 화면에서 임의 추정하거나 게임 상태를 초기화하지 않습니다. 원본 참고 설명과 실제 적용되는 효과를 구분합니다.
- 상단 HUD 툴팁은 왼쪽 정렬로 전체 내용을 표시하며 스크롤 안내나 행 수 제한을 넣지 않습니다. 패널별 긴 설명의 스크롤 규칙과 구분합니다.

## 결정 문구의 국기

결정 이름·설명·분류 문구·결정 패널과 툴팁의 서버 발급 `[TAG.GetFlag]`를 작은 국기 그림으로 표시한다. 태그는 대문자 세 글자이며 `GetNameWithFlag`가 보내는 국기 바로 뒤 나라 이름도 같은 경로를 사용한다. 이벤트 문구는 이 변환을 사용하지 않는다. 문장의 글자는 Minecraft 기본 글꼴·기존 크기로 유지하고 국기에만 외부 팩의 `hoi:decision_flags` 비트맵 글리프를 적용한다. 기존 이름칸의 중앙 정렬·폭 맞춤과 설명/툴팁의 줄바꿈·스크롤 규칙에 국기 폭도 포함하며 줄 높이는 늘리지 않는다. 국기는 흰색·그림자 없음으로 그려 원래 색을 유지하고 앞뒤 글자의 원본 색 코드·정밀도·공백·빈줄은 보존한다.

원본 HOI4 `common/defines/00_graphics.lua:944–945`의 `COUNTRY_FLAG_SMALL_TEX_WIDTH=10`, `COUNTRY_FLAG_SMALL_TEX_HEIGHT=7`과 TFR `common/defines/TFR_00_magic.lua:3`의 작은 국기 스트립 폭 10을 근거로 기존 팩의 `textures/gui/unit_hud/flags/<tag>.png`(10×7)를 재사용한다. 입력 경로는 TFR `gfx/flags/small/<TAG>.tga`이며 이미지 해시는 HOI-resourcepack `provenance.json`에 있다. KOR·PRK는 설치된 TFRKP에 동명 작은 국기 덮어쓰기가 없으므로 TFR 원본이다. 비트맵 목록은 `scripts/build_decision_flags.py`가 검증된 기존 그림 586개에서 생성하며 새 그림을 복제하거나 JAR에 넣지 않는다.

국기가 든 원작 결정 줄의 지정 스크린샷은 아직 없다. `GetFlag`의 엔진 내부 문구 배율·간격·기준선은 위 정의로 확인되지 않으므로 **10×7 GUI 크기, 9픽셀 글자 줄 안의 y+1 배치, 뒤 1GUI advance 간격은 가정**이다. GUI2의 폭 맞춤 없는 국기는 화면 20×14픽셀이다. 원문 공백이 있으면 그 공백도 유지한다. 국기가 없거나 국기 manifest가 없는 경우 `[FRS.GetFlag]`·`[ZZX.GetFlag]`처럼 토큰 글자를 그대로 표시하며 다른 국기를 대신 쓰거나 빈칸으로 숨기지 않는다. FRS는 현재 작은 국기 목록에도 없으며 서버의 이름 함수가 미확정인 원본 항목과 구분한다. 리소스 재로드 때 바인딩과 문구 캐시를 다시 읽는다. 현재 프로토콜에 이념/대체 국기 식별자는 없으므로 태그의 기본 원본 국기를 쓰며 대체 국기를 추정하지 않는다.

집중 재현은 `test runGameTest remapJar -PhoiTestSuite=decisions`에 검증된 서버 소스/프로토콜 사본을 지정한다. 서버 `FabricDecisionPresentationTest`의 `kor-kfxx`·`prc-eadi` 입력도 provenance 검증 대상으로 포함한다. 실제 클라이언트 렌더러로 GUI2 2560×1440·1280×720 창과 국기 줄·툴팁을 캡처하고 국기 폭 11(10+advance), 연속 색 코드, 빈줄, 줄바꿈, 미등록 국기 표시를 검사한다. 기존 결정 검사는 권한 회수·반복 입력 잠금·분류 접기·스크롤·패널 입력을 함께 실행한다. 이 검사는 운영 접속·멀티플레이나 원작 `GetFlag`의 픽셀 단위 일치 검증을 뜻하지 않는다.

`python scripts/compare_decision_flags.py --run <runDirectory> --baseline <이전-전체-runDirectory> --output <증거-폴더>`는 네 창·국기 줄의 3배 최근접 crop, 전체 캡처의 이름/발생 순서별 변경·추가·누락 해시, JUnit/producer 수와 JAR/팩 해시를 기록한다. 2560×1440의 한국·중국 국기 줄은 기존 원본 PNG를 최근접 2배 확대한 20×14픽셀과 실제 렌더 픽셀이 같은지 검사한다. 1280×720의 긴 이름은 기존 폭 맞춤 규칙을 따르므로 작은 국기도 이름과 함께 줄어들며, 그 결과를 20×14 검증과 혼동하지 않는다. 이벤트 창·툴팁 측정값도 이전 전체 실행과 별도로 비교하며, 동적인 배경을 포함한 전체 PNG 해시 차이를 해당 창의 UI 변경으로 단정하지 않는다.

사본-8의 실행용 복사본으로 결정 집중 검증은 337.79초, 필터 없는 전체 `test runGameTest remapJar`는 1336.24초에 통과했다. 클라이언트 JUnit 111개는 집중 실행에서 재실행했고 프로토콜 115개는 기존 성공 결과를 재사용했다. 전체는 두 JUnit 결과를 재사용하고 producer 45개와 실제 전체 렌더·입력 검사를 실행했다. 실패·오류·건너뜀은 0이며 자동 입력 82개·공유 프로토콜 소스 46개를 확인했다. 이전 CMP-15의 성공한 수동 입력 53/17/3/1개는 소스·콘텐츠 1177개 해시가 사본-8과 같아 재사용했다. Python 작은 국기 검사 1개도 통과했다.

전체 PNG는 1860장으로 CMP-15 마지막 전체 1838장 대비 원시 해시 변경 228장·국기 fixture 추가 22장·누락 0장이다. 비교 전 CMP-15 원본 1838장 해시를 확인했다. 이벤트 창·툴팁 측정값은 CMP-15와 같으며 한국·중국 큰 창의 국기 픽셀은 원본과 완전히 같다. 네 국기 창과 확대 crop을 직접 열람했다. 동적 배경을 포함하는 변경 228장 전체의 UI 동등성을 직접 확인한 결과는 아니다. 변경·추가 목록과 재현 명령·시간·출처·JAR/팩 해시는 CMP-16 첨부 `render-verification.json`에 남긴다. 기본 팩은 98510225바이트(SHA-1 `db57f506fcf852afa3bd1c2cd88abe7519f636ee`), UI 팩은 255528651바이트(SHA-1 `21516a985ae33cc1e2b658f2ae20f73a1e52264a`)로 각각 3254/4203바이트 늘었으며 나머지 네 팩은 같다. 모든 manifest 팩을 함께 적용한다. 운영 멀티플레이·직접 효과음 청취·원작 국기 줄의 배율/간격 일치는 미수행이다.

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

### 날짜와 배속 렌더 검증

스코어보드 미리보기의 맨 위 행은 색을 추가하지 않은 `날짜: 2020년 1월 1일 00시`, 둘째 행은 `게임 속도: `와 배속별 색상 글자이다. PAUSED·X1·X3·X5·X7·X10을 각각 캡처하고 X10에서 `날짜: 2022년 12월 31일 23시`도 캡처한다. GUI2 2560×1440의 7장에서 날짜가 잘리지 않고 가장 긴 날짜의 사이드바도 화면 안에 있으며 다른 HUD와 겹치지 않았다. 실제 배속 글자 픽셀은 각각 `0xAAAAAA`, `0x55FF55`, `0xFFFF55`, `0xFFAA00`, `0xFF5555`, `0xAA0000`과 일치했다. 미리보기는 날짜·배속·참가자 3행을 사용하므로 최대 참가자 수, 페이지 전환, 서버의 10틱 갱신과 주기 액션바 제거의 네트워크 동작을 검증한 결과는 아니다. 메뉴·연구 화면의 `date` 필드는 현재 `client/src/main/java`에서 그리는 곳이 없다.

사본-6의 `china-705`·`china-182` 원본 fixture 2개를 재생성하고 GUI2 2560×1440·1280×720에서 실제 렌더를 확인했다. `china-182`의 기본 날짜 `날짜: 2020년 1월 1일 00시`와 둘째 원문 행만 바꾼 최장 합성 날짜 `날짜: 2022년 12월 31일 23시`는 모두 한 줄이다. 이전 분 포함 최장 날짜와 비교해 1280 창의 본문/뷰포트는180→168 GUI, 선택지 y는289→277 GUI, 창 높이는278→266 GUI로 돌아온다. 기본 날짜의168 GUI 본문·y277 선택지·266 GUI 창과 같고2560에서는 둘 다132 GUI 본문·y403 선택지·256 GUI 창이다. 두 해상도의 발신·날짜·빈 줄·본문·선택지 순서와 기존 토큰·리비전·입력 회귀를 유지한다. 본문 글자 높이9 GUI·행 간격12 GUI·시작 위치는 바꾸지 않았고 제목 높이는13.5 GUI로 확대했다. 작은 지휘관 제목 영역만13→14 GUI로 늘려 초상화/본문 전에 끝나도록 했다.

집중 재현 명령은 다음과 같다. 두 명령은 같은 클라이언트 체크아웃에서 순차 실행한다. 사본-6은 소스 입력으로만 사용하며 원본/사본의 소스는 수정하지 않는다. 기본·최장 날짜와 네 이벤트 제목의 검증은 지휘관 묶음에서 함께 실행한다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=leader-event "-PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI" "-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI/protocol/src/main/java" --console=plain --no-daemon
.\gradlew.bat runGameTest -PhoiTestSuite=scoreboard "-PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI" "-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-6/HOI/protocol/src/main/java" --console=plain --no-daemon
```

처음에는 각각61.88초·37.95초에 종료 코드0으로 통과했다. 당시 스코어보드는 분까지 표시했다. 분을 뺀 스코어보드 재검사는42.47초에 종료 코드0으로 통과했으며 GUI2 2560×1440 PNG7장을 직접 열어 날짜 행·잘림·HUD 겹침을 확인하고 배속별 실제 글자 RGB도 대조했다. 이 재검사는 `ResearchScreenTest` 진입점1회이며 JUnit·서버 fixture producer·지휘관 묶음은 재실행하지 않았다. 첫 실행의 `ProtocolCampaignStyleTest` 2개·서버 fixture producer 3개는 실패/오류/건너뜀0이고 공유 프로토콜46개·fixture2개의 출처 해시를 확인했다. Fabric 클라이언트 진입점 `ResearchScreenTest`는 묶음마다1회 실행되어 지휘관 PNG20장(전체 화면13·창 crop7), 스코어보드 PNG7장을 생성했다. 리소스팩은 기존2026-10-12 08:22 KST 산출물을 재사용했다. 이전 날짜 집중 검증은 아래의 이벤트 제목·분 없는 날짜 전체 출하 검증과 구분한다. 운영 접속·다중 사용자 동기화·직접 효과음 청취는 미수행이며 서버/사본 소스·승인된 화면·팩 자산은 바꾸지 않았다. 사본-3 대비 원본 fixture 차이는 `china-182.body`의 날짜뿐이다. 그 밖의 캡처 픽셀 차이는 수정 없이 이슈 첨부 `render-verification.json`에 목록으로 기록하며 동적 배경을 포함하는 차이를 UI 변경으로 단정하지 않는다.

### 지휘관 이벤트 보고서

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

`EVENT`의 `presentation = leader_event`는 `ScreenDialogStack`에서 `ScreenLeaderEvent`로 표시한다. 원본 TFR `interface/eventwindow.gui`의 `EventWindow_leader`처럼 제목, 왼쪽 초상화와 틀, 오른쪽 본문, 한 열의 선택지를 사용하며 이벤트 그림·국기·국가 이름 줄을 추가하지 않는다. 초상화 영역은 표시 전용이다. `image`의 지휘관 초상화가 없으면 기존 `politics/empty/leader`(`GFX_leader_unknown`)를 사용한다. presentation이 빈 옛 대기 이벤트는 기존 국가 이벤트 화면을 사용한다. 국제 뉴스는 종이 배경의 제목·부제·본문·타일 그림자를 제거하며 슈퍼 이벤트는 제목 외의 기존 렌더를 유지하고 완료 알림은 그대로다. 선택은 원래 `ScreenDialogCoordinator.choose`로 토큰·리비전·선택 ID를 보내며 대기 중 반복 제출과 같은/오래된 리비전의 잠금 해제를 막는다.

원본 좌표는 제목 `(14,70) 551×32`, 본문 `(198,121) 350×350`, 초상화 `(43,125) 156×210×0.85`, 틀 `(35,116) 172×232×0.85`, 선택지 시작 x=115·아래 영역 y=10·한 칸 300×47이다. 원본 버튼 그림은 352×48이며 원본 격자의 47 단위 간격을 유지한다. 실제 DDS는 top 605×123, middle/options 605×80, end 605×68로 GUI 선언의 581×121·605×235·581×85와 다르다. 기존 이벤트의 화면 크기 환산과 최소 폭 240 GUI 단위를 사용하고 2560×1440 GUI2에서 원본 단위당 약 0.5 GUI 단위로 그린다. 가운데/선택지 배경은 세로 가장자리 16 원본 픽셀을 보존하고 가운데를 반복한다. TFR의 중간 스프라이트 border x=350은 실제 폭의 절반보다 크므로 가로 타일 규칙을 추정하지 않고 원본 전체 폭을 유지한다.

원작 인게임 창을 관측하지 못해 아래 영역의 GUI 기본 y=221과 끝 조각의 y=0을 실제 최종 배치로 단정하지 않는다. 렌더는 최소 235 원본 단위의 초상화/본문 영역 뒤에 선택지를 놓고 본문이 길면 영역을 늘린다. 화면 높이가 부족하면 본문만 스크롤하고 24개까지의 선택지는 한 열의 페이지로 유지한다. 원본 루트는 CENTER 기준 `(-282,-310)`, 크기650×0이고 moveable=yes다. 드래그로 창을 이동시키지 않으며 원작 엔진의 동적 높이·이동 기능을 완전히 재현한 것으로 간주하지 않는다. 선택지가 있는 지휘관·국가 이벤트와 국제 뉴스는 원본 최소화 단추와 Esc로 접고 사건 목록에서 다시 연다. 세션 유지와 읽기용 뉴스·슈퍼 이벤트의 예외는 아래 효과 툴팁과 최소화 규칙을 따른다.

사용자 지시로 지휘관·국가 이벤트·국제 뉴스·슈퍼 이벤트의 맨 위 제목만 볼드체와 본문 대비1.5배를 사용한다. Minecraft 기본 글꼴을 유지하며 새 글꼴 에셋을 추가하지 않는다. 본문은9 GUI 픽셀(배율2에서18 화면 픽셀), 본문 행 간격은12 GUI 단위이며 선택지는 기존 `UiMenuButton` 글자 크기를 사용한다. `ScreenDialog.EVENT_TITLE_SCALE` 하나로 제목 확대 비율을 바꿀 수 있다. 확대 상태에서 한 줄로 들어가지 않는 제목은 볼드를 유지한 본문 크기로 돌아가며 그래도 넘치면 볼드 글자 폭으로 측정해 끝에 `…`를 붙인다. 분류 띠·부제·본문·선택지·완료창과 STATE/DIPLOMACY/POLITICS/COMMANDER 제목에는 적용하지 않는다.

원본 TFR `interface/eventwindow.gui`의 국가 이벤트 제목은 `Electro_20`(20), 지휘관 제목은 `hoi4_typewriter22`(22, lineHeight22), 본문은 `Electro`(`Electro_0.fnt` size=-16, lineHeight19), 선택지는 `hoi_20bs`(20, lineHeight20)이다. 원본 제목/본문 크기 비율1.25~1.4를 근거로 GUI2 영문·숫자가 정수 픽셀 배율인1.5배를 기본으로 한다. 실제1.5배/2배 제목을 최근접3배로 잘라 나란히 비교하며 원본 글꼴 동등성을 주장하지 않는다.1.5배는 작은 지휘관 창에서도 확대 제목을 유지하고 본문과 구분되어 기본으로 권한다. 한글의 원래 글리프 특성은 남아 있으며 새 에셋 없이 획 전체를 다른 글꼴처럼 고르게 만드는 변경은 하지 않았다. 제목은 자기 영역 중앙에 배치하고 기준점을 정수 GUI 좌표에 맞춰 추가적인 반 픽셀 이동을 피한다. 지휘관 제목 영역은 원본32 단위 높이와 확대 글자 높이 중 큰 값을 사용하며 본문 시작은 원본121 단위를 유지한다. 국가·뉴스 제목은 기존 제목/국기 영역 안의 y32부터18 GUI 높이에 놓고, 상수를2배로 바꾸면 부제 y48을 넘지 않도록 제목 시작만y30으로 올린다. 슈퍼 이벤트는 기존 띠 내부 y2부터21 GUI 높이에 맞추며1.5배 제목이 띠 안에 들어간다. 본문·부제·선택지 좌표는 바꾸지 않는다.

서버에서 온 `china.705.d`와 미지원 대괄호 함수는 그대로 표시한다. `[GetDateText]` 날짜 확장은 서버에서 처리하며 기본 `2020년 1월 1일 00시`, 최장 `2022년 12월 31일 23시`처럼 분을 쓰지 않는다. 클라이언트는 발급된 문구를 바꾸지 않는다.

지휘관 창은 원본 CENTER(-282,-310)를 적용해 2560×1440 GUI2에서 왼쪽 위를 (998,410) 화면 픽셀에 고정한다. 본문 길이는 루트 위치를 바꾸지 않으며 작은 화면에서는 창 전체가 화면 안에 들어오도록 위치를 보정한다. 일반 국가 이벤트의 과거 전체 창 중앙정렬은 아래 세 이벤트 창의 원본 프레임 규칙으로 교체했다. 스택은 종류별 마지막 이벤트·슈퍼 이벤트·완료 창을 선택하고 메뉴와 함께 표시하며 위치에 의존하지 않는다. Minecraft 버튼 영역은 정수 GUI 좌표이므로 원본 47px 간격의 23.5 GUI 단위를 표현하지 못한다. 2560×1440에서는 두 선택지 간격이 48 화면 픽셀로 반올림된다.

지휘관 제목·본문·선택지는 그림자 없이 그린다. Minecraft 26.3의 text/centeredText 기본 호출은 shadow=true여서 밝은 패널의 어두운 영문·숫자가 두 겹으로 보였다. 같은 경로의 종이 배경인 일반 국가 이벤트(EVENT)와 국제 뉴스(GLOBAL_EVENT)의 제목·부제·본문·타일 글자도 그림자 없이 그린다. 어두운 STATE/DIPLOMACY/POLITICS/COMMANDER 패널과 슈퍼 이벤트의 띠·인용문, 완료 창의 기존 그림자 설정은 유지한다. 제목은 core.gfx의 hoi4_typewriter22 색 0xff000000, 본문은 fonts.gfx의 Electro 흰색 tint와 Electro_0.dds의 알파>0 글자 픽셀 15,438개가 모두 RGB(0,0,0)인 근거로 검정색을 사용한다. 선택지의 기존 글자 색상은 유지한다. 일반 이벤트 그림자 수정 대조에는 비교 명령에 `--allow-event-text-change`를 추가한다. 이 옵션은 제목 변경 전의 일반 이벤트 그림자 비교용이며 나머지 세 창은0픽셀을 요구한다. 현재 볼드 제목 변경과 국제 뉴스 그림자 제거를 포함하는 비교에는 그대로 사용할 수 없다.

지휘관·일반 국가 이벤트의 긴 선택지는 공통 `UiMenuButton.wrapLabel()`로 전체 문장을 가운데 줄바꿈한다. 기존 버튼 글자 높이 10 GUI 단위와 글꼴을 유지하고 줄 수에 맞춰 버튼·선택지 영역을 늘리며 부족한 본문 영역은 기존 스크롤을 사용한다. 지휘관 버튼 배경은 높이를 늘릴 때만 기존 9분할 도우미로 원본 테두리를 보존한다. 1280×720 GUI2의 china-705는 두 줄씩 표시하며 버튼 높이19→24 GUI 단위, 선택지 간격19→26, 창 높이209→224로 바뀐다. 두 2560×1440 창과 china-182의 작은 화면 배치는 그대로다. 일반 이벤트의 과거 두 열은 아래 원본 프레임의 한 열로 교체했다. `--allow-event-choice-change`는 일반 이벤트의 하단 본문·선택지 영역만 비교에서 허용하고 제목 변경 전 비교의 뉴스·슈퍼 이벤트·완료 창은0픽셀을 요구한다.

`leaderEvent` fixture 그룹은 고정 또는 지정된 서버 소스의 `FabricPresentationViewsTest`가 생성한 `china-705`·`china-182`를 가져오며 `ScenarioLeaderEventSourceTest.java`도 출처 해시에 포함한다. 집중 재현 명령은 다음과 같다. 서버 원본을 다른 작업자가 수정 중일 때 두 속성을 같은 검증된 서버 소스 사본으로 지정한다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=leader-event -PhoiServerSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-2/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/Temp/claude-cmp3-server-snapshot-2/HOI/protocol/src/main/java --console=plain --no-daemon
```

`ScreenLeaderEventTest`는 실제 GUI2 두 해상도의 전체/창 PNG, 초상화 누락 fallback, 요청 값·왼쪽/오른쪽 클릭·드래그·반복 클릭·비활성 상태·리비전·Enter·긴 본문과 페이지를 검사하고 실행 폴더에 `leader-event-measurements.json`을 쓴다. 선택지의 모든 글자·마지막 문장부호, 기존 글자 크기에서의 가로·세로 여유와 버튼 겹침을 검사하고 china-705의 서버 문구를 일반 이벤트로 표시한 1280×720 전체/창 PNG도 생성한다. `-PhoiTestSuite=leader-event-regression`은 외부 팩을 로드한 기존 국가 이벤트·뉴스·슈퍼 이벤트·연구 완료 알림을 촬영한다. `python scripts/compare_leader_events.py --before <기준 screenshots> --after <집중 screenshots> --measurements <leader-event-measurements.json> --output <비교 폴더>`는 실제 창 내부의 전후 픽셀, 원본 정의/실제 렌더 나란히 비교와 SHA-256 manifest를 생성한다. 비교의 정의 표는 실제 원작 캡처로 표시하지 않는다. 지정 Steam 스크린샷506장의 contact sheet에서 보고서 형태의 지휘관 이벤트는 찾지 못했다. 원작 런타임의 최종 배치·hover shader 일치·직접 효과음 청취·운영 멀티플레이는 별도 검증이다.

최종 선택지 줄바꿈 수정본은 위 snapshot-2로 집중 `test runGameTest remapJar -PhoiTestSuite=leader-event`를1분3초, 필터 없는 전체 명령을20분3초에 통과했다. JUnit224개(클라이언트110·프로토콜114)와 자동 producer42개는 실패·오류·건너뜀0이며 자동 fixture75개·프로토콜46개·실제 PNG1474장을 확인했다. 변경되지 않은 JUnit 결과는 전체 실행에서 UP-TO-DATE로 재사용했고 전체 producer는 재실행했다. 집중/전체의 두 fixture·두 해상도 좌표는 동일하다. china-182의 발신·날짜·빈 줄·본문 순서와 전체 선택지·마지막 문장부호·버튼 겹침·입력 경로를 검사했다. 두2560 창은 선택지 수정 전과 변경 픽셀0이다. 작은china-182의 좌표는 같으나 crop에는23137픽셀 차이가 있다. 그중23125개는 RGB 채널별1~2 차이이고 나머지12개는 왼쪽 바깥 경계이며 원인은 확정하지 않았다. 일반 이벤트는 그림자 제거 뒤 기준과 비교해 하단 본문·선택지 영역에서만11918픽셀 변경됐고 허용 영역 밖과 뉴스·슈퍼 이벤트·완료 창은0이다. JAR SHA-256은 `30d211591908ef1c1f0a86e25f80d2be0fdb844dc4f2979902739adf1c8acd88`이며 내장 이미지/음향은 기존 mod-list icon 하나뿐이다.

서버 함수 확장·장비 순서 고정 사본(snapshot-3)은 두 소스 속성을 같은 사본으로 지정한 `test --rerun runGameTest remapJar --console=plain --no-daemon`으로16분31초에 통과했다. JUnit224개를 재실행했고 이전 사본-3 producer43개·fixture75개는 현재 provenance를 확인해 재사용했다. 별도 새 JVM의 장비 export는8개 검사·46초에 통과했다. 모두 실패·오류·건너뜀0이며 실제 PNG1474장을 생성했다. 당시 GUI2 두 해상도의 china-182 날짜(이후 위의 분 단위 한국어 표기로 교체)는 한 줄이고 본문·선택지·하단과 겹치지 않는다. CMP-8 대비 네 창의 좌표·글자 높이는 같고 china-705 두 창은0픽셀, china-182 두 창은 날짜 영역만 각각878픽셀 차이다. 포획·HQ의144개 목록11종과 무기대여의13개 목록4종은 두 export에서 순서·장비 값이 모두 같다. resources 키 순서763건은 값 변경 없이 남겼다. 지휘관 밖 fixture51개·캡처140장 차이를 수정 없이 기록했으며 배경·마우스 툴팁·미확정 픽셀 차이를 포함한다. 전체 캡처 차이146개 중4개는 원본 지휘관 전체/창이고2개는 합성 지휘관 검사다.140장의 원인을 모두 직접 대조한 결과로 간주하지 않는다. JAR 해시는 위 수정본과 같으며 승인된 UI·프로토콜·팩 자산 소스는 변경하지 않았다. 팩은 중단 실행의2026-10-12 08:21:46~08:22:30 KST 빌드를 재사용했다. 원작 런타임·직접 효과음 청취·운영 멀티플레이는 미수행이다.

이전 줄바꿈 수정 전 사본의 전체19분40초·집중1분1초·PNG1472장과 그림자 수정의 일반 이벤트 글자 영역12956픽셀 차이는 별도 단계의 기록이다. snapshot-2는 이벤트 문구·결정 자료 등을 바꾼 서버 입력으로, 지휘관 밖 fixture49개(결정12·교리10·전술7·포로6·무기대여5·최고사령부5·의용군4)의 필드 차이를 기록하고 수정하지 않았다. 전체 캡처의 원본 픽셀/크기 차이222개는 배경·동적 상태를 포함하는 목록이며 모두 UI 변경으로 단정하지 않는다. 전체 수동 입력은 기존 검증 사본의 `HOI_COMMANDER_PREVIEW`, `HOI_OFFICER_PREVIEW`, `HOI_LAND_PLAN_PREVIEW`, `HOI_MISSION_OVERLAY_FIXTURES`를 사용했고 육군 동반 `land-plan-preview.json`도 보존했다. 팩 재빌드는2026-10-12 06:57:22~06:58:11 KST, UI ZIP 수정06:57:49이며 SHA-1 `260c22ccec3b7deb690ad5c319a083b0dc8346d7`은 같다. 이 단계의 자산 소스 변경은 없다. 원작 런타임·직접 효과음 청취·운영 멀티플레이는 미수행이다.

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

경험치 연결·계획 중앙 재정렬은 같은 고정 사본과 r12 육군 입력으로 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout`을 실행해 5분50초에 통과했다. 두 해상도 GUI2의 기존 헤더(화면 좌표 6,80~431,290) 변경 픽셀은0이며 사단 행의 변경은 경험치·계획 영역에 한정된다. 기존 게이지 채움·좌우 클릭·반복 입력·선택·스크롤 회귀를 유지했다. 첫 실행은 제공자 오류로 실제 화면262장 후 중단되었으므로 통과로 세지 않는다. 재실행은 앞서 성공한 단위 검사 결과를 재사용했다. 이후 원본 경험치 프레임·막대 크기 복원은 같은 입력과 명령으로 팩 빌드 후 6분3초에 통과했다. 실제 PNG402장, client127·protocol121 단위 검사 실패·오류·건너뜀0이며 변경 없는 protocol·생산자 성공 결과는 재사용했다. 두 해상도의 이전 헤더 변경 픽셀은0이고 본문의 차이는 경험치 영역에 한정된다. 이 집중 검증은 최신 서버 입력·필터 없는 전체 검사·운영 접속·저장/재접속·멀티플레이·직접 청취를 포함하지 않는다.

사단 목록 본문의 경험치 아이콘은 TFR `unitview.gui/unit_level`과 `unitview.gfx/GFX_unit_level`의 원본 27×27 프레임 전체(투명 여백 포함)를 GUI2의 13.5×13.5GUI로 표시한다. `GFX_unit_xp_bar`의 28×6 원본 막대는 세로 3×14GUI로 회전해 프레임 오른쪽에 붙인다. 두 요소의 전체 폭 16.5GUI를 경험치 헤더 열(행 시작 기준 48~61GUI)의 중앙에 맞추며, 레벨마다 다른 투명 여백을 잘라 아이콘을 확대하거나 이동하지 않는다. 원본 `20261017220719_1.jpg`와 함께 배치를 비교한다. 계획 게이지는 다음 헤더 열(61~71GUI), 참호 아이콘·게이지 묶음은 그 다음 열(71~86GUI)의 중심에 맞춘다. 경험치 묶음과 계획 게이지는 반올림된 열 경계의 중점을 실수 좌표로 계산하며, 계획·참호의 기존 게이지 높이와 모든 진행률·색을 유지한다. 같은 활동 아이콘 칸의 이동·후퇴·훈련·전투 그림도 이 중심을 따른다. 점령지 본문의 국가 이름·국기 영역은 툴팁을 표시하지 않고 기존 펼치기/접기 입력을 유지한다. 저항도·순응도·경제·법률·주둔 편제의 툴팁은 유지한다. 클라이언트 클래스에 직접 넣은 포획 지휘관·생산 효율 툴팁의 하이픈 구분선은 빈 줄로 표시한다. 서버가 생성하는 구분선은 서버 후속 변경 범위다. 집중 검증은 `army-layout`과 `occupation`을 사용하며 서버 전체 통과 고정 사본 확인 전의 합성/이전 서버 입력 검증은 최신 서버 연동 완료로 보지 않는다.

이 보정은 팩 빌드 후 Java25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout` 6분4초와 `-PhoiTestSuite=occupation` 49초에 통과했다. 서버·프로토콜 경로는 모두 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42/synthetic-server/HOI` 사본을 지정하며 육군 입력은 기존 r12 `HOI_ARMY_REVIEW_PREVIEW`를 쓴다. 클라이언트127·protocol121개 단위 검사는 실패·오류·건너뜀0이고 점령지 실행은 앞서 성공한 단위 결과를 재사용했다. 실제 PNG는 육군402·점령지29장이다. 두 해상도 GUI2에서 기존 집단군 헤더 영역의 변경 픽셀은0이고 계획·참호 막대의 채움 픽셀과 국가 호버 화면을 확인했다. 첫 실행은 새 테스트의 존재하지 않는 `Button.getTooltip()` 호출로 컴파일에 실패했고, 해당 호출 제거 후 실제 호버 캡처로 확인했다. 서버 스킬/특성·혼합 후보의 최신 입력, 서버 구분선 교체, 필터 없는 전체 검사·운영 접속·저장/재접속·멀티플레이·직접 청취는 이 결과에 포함하지 않는다.

육군 머리줄의 군 사단 수는 y=4GUI를 유지하고, 군 이름 라벨은 y=5→4GUI로 1GUI 올려 사단 수와 같은 높이11.5GUI 칸에 놓는다. 집단군 이름과 군 수/한도(예: 3/5)의 기존 위치는 유지한다.

머리줄 상향은 팩 빌드 후 cmp50 고정 서버·protocol과 cmp41-r5 팩의 `test runGameTest remapJar -PhoiTestSuite=navy-layout`로4분4초에 통과했다. GUI2 2560×1440·1280×720 모두 육군 이름의 글자 픽셀이 정확히2px 올라갔고 사단 수는 그대로다. 해군 이름과 머리줄 태스크포스 수는 모두 정확히2px 올라가 같은 y87~100px에 놓인다. 기존 글자 픽셀과 가로 좌표는 같다. 육군 이름에는 한글 글리프 아래쪽2px가 더 있어 본문 경계 중심 차이가1px지만 렌더 기준 칸과 글자 시작 높이는 같다. PNG116장, client138·protocol124 실패/오류/건너뜀0이며 변경 없는 protocol 결과는 재사용했다. 실행 중 소스·빌드361개와 지정 입력93개 해시는 불변이다. 재생성 입력의 제독이 달라져 초상·이름·능력치·특성 내용은 이전 캡처와 다르며 이 차이를 배치 변경으로 세지 않는다. 제독 보정 뒤 시작한 필터 없는 전체 실행은 새 머리줄 요청 시점에 종료 기록 없이 중단되어 통과로 세지 않는다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이다.

이번 군 사단 수 1GUI 하향 수정은 기존 리소스팩 ZIP과 r12 서버 사본을 재사용한 `:client:test --tests dev.hoi.client.map.MapCommanderGroupsTest runGameTest -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`으로 7m50s에 통과했다. 실행 전 `HOI_ARMY_REVIEW_PREVIEW`는 같은 r12 사본의 `fabric/build/army-review-fixtures`로 지정한다. 관련 클라이언트 JUnit4개와 입력 생산자49개는 실패·오류·건너뜀0이며 실제 PNG359장을 생성했다. GUI2 2560×1440/1280×720 모두 19/18의 글자 픽셀252개가 y85~98→87~100으로 정확히2px 내려갔고, 집단군3/5와 나머지 머리줄 내부 픽셀은 유지됐다. 이전 전체 검사와 집중 검사의 월드 배경 차이는 비교 영역에서 제외했다. 리뷰 입력 경로를 지정하기 전 첫 준비 실행은 중단했고 통과로 세지 않는다. 사용자 지정 범위에 따라 팩 재빌드·전체 검증·remapJar·설치와 실제 소켓·저장/재접속·멀티·직접 청취는 이번 검사에 포함하지 않는다.

직전 검증에서는 군·집단군 모두 y=3GUI였다. 집단군 군 수/한도(예: 3/5)는 직전 y=2GUI에서 1GUI 아래로, 군 사단 수는 직전 y=6GUI에서 3GUI 위로 이동한다. 숫자의 가로 위치·크기·색과 캡슐/월계수는 유지한다. 연속된 동일 요청은 한 번만 적용한다. 선택 사단 수 도구줄, 전구 제목/본문, 특성/훈장과 HQ 좌표는 앞 검증본을 유지한다. 이 숫자 보정은 팩 빌드 후 Java25·r12 사본의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`에서8m8s 통과했다(PNG359장, JUnit122+119 실패/오류/건너뜀0). 두 해상도에서 집단군+2px·군−6px 이동을 실제 글자 픽셀 집합으로 확인한다. 이 숫자 보정까지 포함한 필터 없는 전체 `test runGameTest remapJar`는 Claude의 일정 카드13535287 회신 후 27m 41s에 통과했다(PNG2222장). 앞27m36s는 이번 보정 전 결과이며 집중8m8s와 이번 전체 결과를 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취·실제 전투 전구 툴팁과 공식 가 시각 리뷰는 별도다.

육군 선택 도구줄의 원본 unit_list_header 배경은 고정196GUI 대신 현재 패널의 안쪽 폭을 채운다. 병과 요약 때문에 늘어난 폭은 선택 수 글자 영역에 주고, 오른쪽 전략적 재배치·통폐합·수송·절반 선택·정지·배정 해제 버튼과 호버는 같은 폭만큼 이동한다. 미지정 목록도 같은 배경 폭 규칙을 사용한다. 특성/훈장은 어두운 능력치 칸에서 첫 훈장까지의 여백을 오른쪽 끝에도 확보하고 기존 크기·서버 순서·넘침 겹침/호버를 유지한다. 마지막 사용자 정정에 따라 선택 사단 수 글자만 기존 위치보다2GUI 아래·1GUI 왼쪽으로 옮긴다(앞1GUI와 누적하지 않는다). 전구 이름은 가로 위치를 유지하고3GUI 위로 옮겨 기존 이름 칸 안에 맞춘다. 배경·버튼에는 이 글자 오프셋을 적용하지 않는다. 팩 빌드와 Java25 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 최종 좌표를 포함해5m40s 통과했다(PNG359장). 두 해상도 픽셀 대조에서 선택 수−2px/+4px, 전구 제목0px/−6px와 글자 크기 유지를 확인했다. 앞8m2s 집중은 최종 글자 좌표 적용 전 중간 결과다. 이 추가 수정은 Claude의 서버 검증 종료·일정 회신 후 r12 사본과 수동 입력을 포함한 필터 없는 전체 `test runGameTest remapJar`에서 27m 36s에 통과했다(PNG2222장). 이전 전체27m14s 결과와 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취 및 실제 전투 전구 툴팁은 미검증이며 공식 가 시각 리뷰는 별도다.

사용자 추가 보정으로 집단군 머리줄 군 수/한도(예: 3/5) 글자를 y=3→2GUI로 1GUI 더 올린다. 오른쪽 전구 본문의 집단군·군 배지, 선택 강조, 숫자, 경고 아이콘과 클릭/호버 영역은 1GUI 내리고 전구 전체 사단 수도 함께 내린다. 전구 머리줄·설정 창·본문 배경과 HQ·특성·▼ 좌표는 유지한다. 같은 문구로 반복된 요청은 한 번만 적용한다. 이 보정은 팩 빌드 후 Java25·r12 사본의 육군 집중 test runGameTest remapJar -PhoiTestSuite=army-layout에서 8m 24s에 통과했다(PNG359장, JUnit122+119개 실패·오류·건너뜀0). 두 해상도 픽셀 대조에서 3/5 −2px, 전구 집단군·군 숫자 +2px와 글자 크기 유지를 확인했다. 기존 좌우/반복 입력·선택·스크롤·툴팁 검사도 통과했다. 첫 전체 검증은 26m9s에 이전 커서의 사단 툴팁이 1280×720 경험치 막대 픽셀을 가려 실패했다(PNG2000장). MapCommanderGroupsRenderTest의 검사 전에 커서를 빈 공간으로 옮기고 두 틱 기다리도록 준비만 수정했으며 막대 좌표·색상 판정은 유지했다. 수정 후 육군 집중7m57s와 필터 없는 전체27m 14s가 통과했다(PNG2222장). 사용자 승인 슬롯을 사용했고 실패 로그·메타데이터·화면도 보존했다. 이전 y=3GUI 전체26m57s 결과와 구분한다. 실제 소켓·저장/재접속·멀티·직접 청취와 공식 가 시각 리뷰는 별도다.

직전 머리줄은 군/집단군 수 캡슐의 높이를 유지하면서 원본 가로 폭을 복원하고, 군 소속 집단군 번호의 월계수에 집단군과 같은 금색을 쓴다. 군·집단군 카드 오른쪽 끝을 맞추며, 집단군 3/5 글자는 직전 위치보다 추가1GUI 위(y=3GUI)에 둔다. 특성/호버 높이와 ▼ 중앙 정렬은 유지한다. 새 r12 서버 사본의 기본 군 색(빨강→청록→보라)을 사용한다. 이 색 배정은 서버의 근사 규칙이며 원본 엔진 규칙을 확정한 결과가 아니다. 이 수정은 팩 빌드 후 Java25·r12 사본의 육군 집중 8m 44s, 필터 없는 전체 26m 57s에 통과했다(PNG2222장). 새 사본 표시 생산자도 통과했으며 변경 없는 성공 결과는 재사용했다. 전체 검증은 지정된 서버 verify-1018j/full.log의 exit=0 확인 뒤 진행했다. 실제 소켓·저장/재접속·멀티·직접 청취는 이 렌더 검사에 포함하지 않으며 공식 가 시각 리뷰는 별도다.

육군 군·집단군 패널의 폭은 현재 스냅샷의 두 역할에 필요한 병과 요약 폭 중 최댓값을 공유한다. 추가 목록 폭은 이름 열에만 배분한다. 특성 훈장과 호버 영역은 최신 사용자 지시에 따라 직전 위치에서 추가로 2GUI 위로 이동한다(머리줄 기준 y=21GUI). 군 사단 수는 마지막 사용자 요청대로 y=4GUI, 집단군 군 수/한도는 y=3GUI에 둔다. 해당 배경·월계수와 숫자의 가로 위치는 유지한다. ▼는 각 열의 좌우 중앙에 둔다. 목록 배경은 원본 `GFX_tiled_window_1b_thin_border`의 `borderSize=64`와 타일링을 사용하며, 작은 테두리로 중앙의 오른쪽 장식까지 늘리지 않는다. 조직력/내구도 첫 칸과 별도 경고 칸을 포함한 9열은 유지한다. 지역 사령부의 `unit_hud/headquarters` 표시에는 TFR `GFX_unit_bus_icon_medium`의 첫 76×42 프레임(초록 별·무전기)을 사용하고, 사단 행과 병과 요약이 같은 그림을 공유한다. 실제 HQ는 군 전용 배치 줄에만 표시하며 지역 사령부 일반 사단은 목록에서 제거하지 않는다.

### 중점 상세 원본 화면 대조

중점 상세 창은 TFR `interface/nationalfocusview.gui`의 `national_focus_detail_view`와 사용자 Steam 캡처 `20261014201913_1.jpg`부터 `20261014201945_1.jpg`까지 16장을 기준으로 한다. 기본 위치 (250,50), 크기 275×383은 원본 좌표의 절반이며 GUI2에서 화면 안으로 제한한다. 작은 화면에서는 설명·효과 높이를 줄이고 각 영역의 독립 스크롤로 전체 문장을 읽는다. 제목·기간·시작·효과 글자에 자동 축소를 적용하지 않으며 Minecraft 기본 글꼴과 현재 크기를 유지한다. 중점 그림은 원본 170×150 비율의 85×75 영역에 표시한다.

배경·상단·조건 칸·효과 제목·진행 게이지·체크/X·스크롤·시작/닫기 단추는 기존 외부 팩의 원본 자산을 재사용한다. 조건·설명·효과에는 서버의 문구·순서·색·정밀도·빈 줄을 유지한다. 조건의 `£trigger_` 토큰이 이미 있으면 별도 평가 아이콘을 중복하지 않는다. 원본 단추의 활성/비활성/마우스 상태는 기존 `UiDiplomacyAssets`의 buttonstate 처리 경로를 사용하고, 시작은 `ui.focus.select`, 닫기는 `ui.close`를 재생한다. 스크롤의 화살표·트랙·손잡이는 왼쪽 클릭/드래그로 조작하고 해제·선택 변경·화면 종료 시 드래그를 끝낸다. 완료 표시는 서버 Node의 `완료` 상태를 따른다. Detail의 `complete`는 상세 문구의 해석 완전성을 뜻하며 중점 완료 여부로 쓰지 않는다. 고정 표시의 원본 키는 `FOCUS_STATUS_COMPLETED`·`FOCUS_REWARD`·`START_FOCUS`·`CLOSE`이며 HOI4 `localisation/korean/core_l_korean.yml`의 완료됨·효과·시작·닫기를 사용한다. 설치 TFR 한국어 패치 3411756715에는 이 네 키의 덮어쓰기가 없다.

팩 빌드 후 집중 재현 명령은 Java 25의 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=focus-detail --console=plain --no-daemon`이다. `ScreenFocusTest`는 인증·같은 리비전 반복 시작 차단·늦은 응답·권한 회수·창 이동·전체 문구·게이지/종이의 실제 GPU 픽셀을 검사한다. `ScreenFocusDetailTest`는 GUI2 2560×1440/1280×720에서 독립 휠·화살표·트랙·손잡이·해제와 화면 경계를 검사한다. `tree-only` 캡처는 보관된 KOR View의 해당 중점 선택이며 서버 Detail이 없음을 확인한다. `synthetic-scroll`은 합성 입력의 렌더·스크롤 검사이며 원작 장면의 증거가 아니다.

상세 장면은 서버 `FabricFocusDetailFixturesTest`의 `kor-start-view.json`/`kor-start-details.json`(시작 중점 21개)과 `kor-oil-shock-view.json`/`kor-oil-shock-details.json`(오일 쇼크 이후 41개)을 사용한다. 집중 검사 전에 `$env:HOI_FOCUS_PREVIEW='<서버 export 사본 폴더>'`를 설정하면 `ScreenFocusDetailTest`가 오일 쇼크 장면에서 원본 파일명과 연결한 16장×두 해상도를 실제 렌더한다. `201923`은 효과를 스크롤한 상태다. 입력을 지정하지 않은 기본 검사는 기존 tree-only·합성 회귀를 유지한다. 중립 외교 노선(`201927`)은 `KOR_follow_up_measures_fake`다. View의 짧은 이름과 Detail의 원본 이름이 다를 수 있으므로 ID로 연결한다.

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

원본 16장에 대응하는 두 해상도 실제 캡처의 시각 검토는 남은 차이 없음으로 회신되었다. 이전 기본 서버 경로의 사전검사는 13초에 `CountryPoliticsCatalog.java`의 오래된 출처 해시로 실패했으며, 서버 담당자가 `server-src-20261016a`에서 필수 9개 그룹을 재export했다. 서버 검증 종료 회신 뒤 위 두 명령을 순차 실행해 사전검사는 12초, 필터 없는 전체 검사는 16분50초에 종료코드0으로 통과했다. 중첩 서버 producer는 실행하지 않았고, 소스725개씩·입력92개·공유 protocol Java48개의 검증은 유지했다. 수동 입력은 성공한 서버 실행의 지휘관54개·장교단17개·임무3개·육군 계획1개이며 경로와 파일별 SHA-256을 검증 증거에 보존했다.

전체 실행 ID는 `2026-10-15T21-00-41.052385600Z-4e46dd07-d273-4b25-ac78-8deee8df5da9`다. JUnit은 클라이언트117개·protocol119개(실패·오류·건너뜀0)의 기존 성공 결과를 UP-TO-DATE로 재사용했고 실제 렌더링을 새로 실행했다. 실제 GPU PNG2007장과 별도 CPU 기대 PNG32장을 생성했으며 원본16장×GUI2 두 해상도의 새 실제 캡처32장이 포함된다. 중점 게이지·종이 GPU 비교는 해상도별18개씩 실패0이며 원작 framebuffer 전체의 픽셀 동일성을 뜻하지 않는다. 실행 중 채취한 클라이언트 소스·빌드 정의434개는 종료 시점과 해시가 같았고, 런타임에 복사한 외부 팩6개의 크기·SHA-1도 manifest와 일치했다. client 전용 JAR은 SHA-256 `f373850eb862f4d14a343f051fa00fe89c8fcfc323c549dc37411dfacb08a6c3`이며 내장 이미지·음원은 허용된 `hoi-client-icon.png` 하나다. 서버 문구 차이와 미지원 효과의 보완은 별도 서버 작업이며, 이 화면은 전송된 줄을 그대로 그린다. 전체2039장의 직접 열람·원작 모든 장면의 픽셀 동등성·운영 멀티플레이·실제 소켓 재접속·장치 청취는 확인하지 않았다.

### 결정 개인 설정과 지도 요청

`config/hoi-decisions.properties`는 클라이언트 개인 설정이다. 추적 점은 접속 서버 주소(통합 서버는 월드 경로)·나라 TAG·기본 결정 ID로 저장한다. `@c:`/`@s:` 접미사는 빼므로 같은 결정의 대상 행은 설정을 공유한다. 기본은 켜짐이며 범주 점은 그 범주의 결정을 함께 전환한다. 세션·리비전·재접속으로 초기화하지 않는다. 임시 파일 뒤 atomic move로 저장하며 실패 시 원본을 보존하고 로그를 남긴다. 상단 결정 경보 feed는 미구현이므로 점은 개인 설정이며 경보 효과를 주장하지 않는다.

팝업 기본은 작은 사건 꺼짐, 사건·뉴스 켜짐이다. 이는 원본 캡처 1842·1847·1851·1856 관측이며 엔진 기본값 확인과 다르다. `POPUP_MINOR_EVENTS`·`POPUP_EVENTS`·`POPUP_NEWS`와 세 `*_POPUP_CHECKBOX_TOOLTIP`는 설치 한글패치에 해당 키가 없어 HOI4 `localisation/korean/decisions_l_korean.yml` 원문을 유지한다. `EVENT`의 빈 presentation/`leader_event`, `GLOBAL_EVENT`, `minor_flavor`에 각각 적용한다. 꺼진 팝업은 사건 목록에 같은 서버 토큰으로 남고 클릭하면 같은 창을 연다. 서버 Close는 최소화/재개 창을 모두 지운다. 슈퍼 이벤트·연구/중점 완료·외교·주 정보에는 적용하지 않는다. 자동 선택을 보내지 않으며 사건 기한은 서버가 관리한다. 설정은 위 로컬 파일에 저장되고 국가 자산·캠페인 저장과 무관하다.

비용은 서버 `costText`의 원문 색을 유지한다. 균형 잡힌 중첩 색 코드(`§A§Y자주포§! 250대 필요§!`)는 안쪽을 닫을 때 바깥 색을 복원한다. 닫히지 않은 기존 색 코드의 덮어쓰기·줄바꿈·구분선 규칙도 유지한다. 비용만 있는 설명은 같은 글자의 서버 비용 색을 사용하며 색 코드 차이로 비용을 두 번 붙이지 않는다. 숫자 정치력 비용만 `costMet=false`이면 원본 `DECISION_COST_NOT_ENOUGH`의 부족 색을 사용한다. `enabled=false`로 자원 부족을 추정하지 않는다. ACTIVE의 `progress=GOOD/BAD`와 `remainingHours/totalHours`로 원본 진행바를 그린다. 분류나 총 기간을 모르면 막대를 만들지 않는다. 코로나 세 패널은 TFRKP `TFR_Korea_GUI.gfx`·`TFR_scripted_guis_KOR.txt`의 두 텍스처·크기·세로 방향과 서버 백분율 frame/100을 사용한다. `horizontal=no` 원본 텍스처는 가로로 저장되어 있으므로 두 레이어에 원본 shader의V축 반전과 반시계90도 변환을 함께 적용하고 아래에서 위로 채운다. 회전만 하면 백신 병·주사기와 주먹이 원작 화면에 비해 좌우 반전되므로 원본1842의 방향을 함께 대조한다. 예를 들어 저항 그림은 원본124×85에서 화면85×124가 되며 주먹은 위를 향한다. 팩의 원본 픽셀·파일·출처는 바꾸지 않는다. 설치 TFR `gfx/FX/progress.shader`의 `PixelTexture`는 텍스처 x축과 CurrentState를 비교한다. 원본20261014201842 화면으로 방향을 확인하며, 이 소스 기반 합성을 원작 런타임과의 전체 shader 픽셀 동등성 검증으로 표시하지 않는다. `ScreenDecisionPanelTest`는 GUI2 두 해상도에서 세 이미지의0/50/100% 합성 장면을 실제 렌더하고 원본의 알파 합성과 회전 좌표·채움 레이어를 실제 픽셀과 비교한다. 홀수 크기에서도 반올림한 절취 길이와 UV 끝점을 맞춰 텍스처를 압축하지 않는다. 합성 픽셀 회귀 화면은 실제 서버 캠페인 장면과 별도로 표시한다. 코로나 패널의 음수 원본 y 좌표는 설명 끝 예약 빈 줄을 사용하므로 같은 공간을 두 번 더하지 않는다. 원본 1842의 다섯 줄 라벨을 보존하고 중앙 정렬 텍스트의 실제 글자 폭으로 이 패널의 캔버스를 계산한다. 국회·일본·인물 패널의 기존 사각형과 clipping은 유지한다.

주 강조가 있는 행의 호버는 현재 token/session/revision과 전체 행 ID로 `HIGHLIGHT`를 보낸다(4틱 간격, 같은 리비전·행은 한 번). 서버 페이지를 모아 기존 지도 타일 메시로 렌더한다. token/session/revision·요청한 행·현재 차원이 맞아야 게시한다. 기본 흰색과 원본 `BORDER_COLOR_CUSTOM_HIGHLIGHTS` 표의 지정 색만 사용한다. 화면 도형 캐시는 최대 64행·전체 262,144상자이며 넘어서 퇴출된 행은 새 리비전에서 다시 받는다. 새 리비전·나라 권한 회수·화면 닫기·차원 변경은 도형과 대기 페이지를 지운다. 지도 버튼은 `GOTO`를 10틱 간격으로 보내며 이동은 서버 atlas 경로가 수행한다. 클라이언트 Camera/player 위치를 변경하거나 주 정보 창을 열지 않는다. `highlightStates`가 비면 지도 버튼도 없다.

`FabricDecisionPresentationTest`의 `kor-scene`, `kor-scene-active`, `kor-scene-ownership-lost`, `kor-scene-mission-timeout`, `kor-scene-target-active`는 실제 캠페인·효과·take에서 생성한다. 날짜는 2020-01-13이며 원본 2020-11-01과 다르다. 국회→코로나19 대응→방산수출→제5차 국토종합계획 순서와 서버 의석·백신/감염·정치력·남은 시간을 표시한다. 미지원 `remove_effect/send_equipment` 수출 3행은 서버 목록에 없으며 클라이언트에서 추가하지 않는다. 원본 TOP SECRET 서류철의 현재 소스 키는 미확정이며 다른 메뉴 그림으로 대체하지 않는다. 대상 소멸·cancel_trigger의 실제 장면은 CMP-27 계약의 서버 경계를 유지한다. 합성 지도 상자·팝업 검사는 실제 캠페인 fixture와 구분하며 실제 다중접속·소켓 재접속·장치 청취를 증명하지 않는다. 최종 집중/전체 입력 대조에서 한국5장면의 안정도 HUD 코로나 효과2줄이 JVM별로 뒤집혔다. 효과별 수치·결정 데이터는 같고, 원시 차이와 서버 후속CMP-28을 검증 자료에 보존한다. 이 화면은 서버가 발급한 툴팁 순서를 바꾸지 않는다.

### 사건·결정 원본 화면 대조

이전 사건·결정 사이드바 검증 기준은 사용자 Steam 캡처 `20261014201842_1.jpg`, `20261014201847_1.jpg`, `20261014201851_1.jpg`, `20261014201856_1.jpg`, `20261014201859_1.jpg`(2560×1440)다. 국회·코로나19 대응, 방산수출·제5차 국토종합계획 및 선택 전후를 각각 비교하며 중점 상세 화면은 별도 작업이다. 글꼴·글자 크기를 줄이지 않고 결정 이름·비용과 분류 이름을 줄바꿈해 행 높이를 확보한다. 설명은 원문 색·공백·줄바꿈을 유지해 왼쪽에 표시한다.

제목 띠는 외부 팩의 원본 `gfx/interface/header_bg.dds`(`decision/ui/window_header`)이며, 바탕은 기존 `diplomacy/dialog/panel_dark`를 재사용한다. 결정 설명 배경은 원본 `GFX_tiled_decisions_bg_small`의 48×48·16픽셀 테두리·중앙 반복을 유지한다. `UiAssets.tiledNineSlice`는 결정에서만 사용하는 추가 경로이며 기존 `nineSlice` 호출은 바꾸지 않는다. 국회 그래프의 `progress_radial_legco.shader`는 중심 y=.982, 반지름 .5 밖을 투명하게 하므로 방사형 쿼드의 위쪽 .482를 패널의 보이는 경계에 포함하지 않는다. 원본 쿼드·도형 자체의 좌표나 프레임 값은 바꾸지 않는다.

`KOR_korea_defense_business_category_desc`는 TFRKP `localisation/korean/replace/00 TFRKP Main l_korean.yml:12897`의 `£GFX_kor_diffusion` 앞 5개·뒤 7개 빈 줄로 큰 text-icon의 공간을 확보한다. 이를 일반 본문 빈 줄과 별도 이미지 높이로 모두 더하면 배너·설명 사이에 과도한 여백이 생긴다. 앞뒤 각각 3개 이상 빈 줄이 있는 단독 큰 원본 이미지에만 이 영역을 한 이미지 블록으로 해석한다. 원문·툴팁·일반 문단의 빈 줄은 유지한다. 배너는 자기 원본 폭과 패널 비율을 넘겨 확대하지 않으며 상하 여백은 패널 550 기준 10이다. 지정 원본 `20261014201851_1.jpg`의 그림 테두리는 x=30..484, y=217..218/384..385에서 확인했다. source-kor 두 해상도 검사는 국회를 접은 실제 화면을 추가 캡처하고 분류 아래 12..49 물리픽셀 안에 배너 테두리가 나타나는지 검사해 이중 여백 회귀를 막는다. 이 접힌 국회 장면은 원본의 코로나·국토계획 상태를 재현한 입력이 아니다.

재현은 팩 빌드 후 Java 25의 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=decisions --console=plain --no-daemon`이다. `ScreenDecisionPanelTest`는 원본 서버 패널과 GUI2 두 해상도, 툴팁·재로드·미상 프레임·해제 후 텍스처 정리를 검사한다. 합성 대상 입력은 서버 원본 fixture와 구분한다. 코로나·국토계획 값, 의석수와 임무 막대의 GOOD/BAD 분류는 최신 서버 producer를 사용한다. 원본 11월 수치에 맞추려고 서버 값을 바꾸지 않는다. 필터 없는 전체 검사·실제 재접속·다중접속·장치 청취는 이 집중 검사와 별도다.

이름과 비용의 원문 폭이 기존 결정 패널에 들어가지 않으면 해당 화면만 오른쪽으로 넓힌다. 범주·본문·행 바탕도 같은 폭을 사용하며 최대 폭은 화면 오른쪽 16GUI 전까지다. 그 폭보다 긴 문장은 기존 글꼴·크기에서 줄바꿈하고 세로 스크롤로 읽는다. 원본 나라 윤곽의 끝 토큰은 등록된 기존 글리프만 두 배로 표시하고 그 폭을 이름과 비용 사이에 확보한다. 기본 행은 원본 `decision_item_bg`의 회갈색 프레임 1을 쓰고 비활성 이름에 회색을 덧씌우지 않는다. 비용의 원문 색 코드는 유지하며, 비정치력 비용의 기본 글자는 흰색이다. 실행 불가라는 사실만으로 자원 부족을 추정해 숫자를 붉게 바꾸지 않는다. 부족 수량의 빨강은 서버 `costText`에 원본 조건별 색이 있어야 한다. 마름모는 원본 40×28 비율을 유지한다.

오른쪽 스크롤바는 기존 원본 트랙·위/아래 화살표·손잡이를 재사용한다. 왼쪽 화살표 클릭은 28GUI, 트랙 클릭은 한 화면을 이동하고 손잡이 드래그는 처음부터 마지막 내용까지 이동한다. 오른쪽 클릭은 결정 요청이나 스크롤을 만들지 않는다. 버튼 해제·새 리비전 적용·화면 제거 때 드래그를 끝낸다. `ScreenDecisionSourceTest`는 최신 서버 입력을 실제 렌더러에 넣어 두 해상도의 배너 여백과 스크롤바 조작을 검사한다. 후속 서버 장면·추적 점·팝업 설정·진행막대·개인 지도 계약은 [현재 연결](#결정-개인-설정과-지도-요청)을 따른다.

추적·팝업·지도 계약 연결 전 `decisions` 집중 명령은1분26초에 통과했다. 직전5분3초 실행에서 최신 서버 입력을 재생성했고 최종 실행은 같은 입력의 출처 해시를 다시 검증했다. 클라이언트 JUnit115개와 protocol118개는 실패·오류·건너뜀0이며 최종 PNG122장을 생성했다. 한국 초기2분류/5행의 패널은2560×1440·1280×720 GUI2 모두290GUI다. 국회를 접은 배너와 최장 튀르키예 방산수출 이름/비용·조건 툴팁을 두 해상도에서 실제로 촬영했다. 스크롤바 위 화살표·우클릭 무동작·끝까지 드래그·해제 후 드래그 무동작과 기존 요청 잠금·권한 회수 검사를 통과했다. staged 서버·클라이언트 JAR은 각 server/client 전용이며 nested protocol269개 클래스가 같고 정적 이미지/음원은 각 기존 모드아이콘 외에는 없다. 이 결과는 원본11월 상태의 재현·원작 글꼴 동등성·실제 소켓 재접속·다중접속·장치 청취를 증명하지 않는다.

필터 없는 전체 클라이언트 명령은23분3초에 통과했고 실제 PNG1893장을 생성했다. 최신 서버 입력 생산자45개는 실패·오류·건너뜀0이고 JUnit233개의 기존 성공 결과를 재사용했다. 최신 자동 입력과 README의 수동 fixture4경로를 사용했다. 집중/전체의16개 결정 사례(8입력×두 해상도)는 패널 폭·행/분류 수·작은 국기 행 좌표·도달 가능한 이름·원본 자산 누락 기록이 모두 같았다. 동적 배경을 포함한 전체 PNG의 픽셀 동등성이나 운영 다중접속을 검증한 결과로 해석하지 않는다. 이번 전체 명령은 클라이언트 gate이며 서버 전체 JUnit/GameTest 재검증을 대신하지 않는다.

결정 대상 행은 `ProtocolDecisionPayloads.Row.target`의 서버 이름을 기존 결정 이름 뒤에 표시한다. 국가 대상 국기는 외부 팩 `ui/story_assets.json`의 해당 국가·정확한 서버 키로만 조회하고, 미등록 키/파일은 대상 국가의 기본 국기로 대체한다. 이념 접미사를 클라이언트에서 조립하지 않는다. 툴팁 본문·비용·기간·조건·불가 사유는 서버 값을 유지하며 `TAKE`에는 `decisionID@c:TAG` 또는 `decisionID@s:state` 전체 줄 ID를 그대로 보낸다. 국가 소속과 다른 열린 결정 화면은 다음 tick에 폐기한다. 새 리비전의 전체 페이지가 모이기 전에는 기존 행을 표시하되 요청을 잠근다.

`test runGameTest remapJar -PhoiTestSuite=decision-targets --console=plain --no-daemon`은 서버 producer를 생성하지 않는 합성 대상 입력 회귀 검사다. 실제 Minecraft GUI2 두 해상도에서 국가/주 대상, 정확한 국기 키·기본 국기 fallback, 좌/우클릭·드래그·반복 입력·실행음·동일 리비전 잠금·대상 소멸·ACTIVE·기한 소멸·권한 회수를 검사한다. `ScreenDecisionChecks`를 통해 기존 전체/decisions 검사에도 포함한다. 합성 행 갱신은 실제 서버 임무 실행·취소·시간 경과를 증명하지 않으며 캡처 파일명에 `synthetic`을 붙인다.

현재 집중 명령은 팩 재빌드 뒤 통과했다. 클라이언트 JUnit 115개와 protocol 118개는 실패·오류·건너뜀 0이며, 공유 protocol main 48개 일치와 client 전용 JAR·정적 이미지/음원 미포함(사용자 모드 아이콘 제외)을 확인했다. 변경되지 않은 JUnit은 통과한 결과를 재사용했고 story-signals 실제 렌더를 재실행했다. 팩 Python 51개와 6 ZIP 해시/크기/한도 검증도 통과했다. 초기 합성 행 이름 중복과 원본 무음 음악의 0 등록 음량 문제를 수정한 뒤 실제 대상 렌더·원본 결정 실행음을 검증했다. 무음 음악의 원본 바이트·60초 PCM 보존은 [음악 문서](audio.md#무음-원본과-minecraft-등록-음량)를 따른다. 원본 대상 결정 장면과의 픽셀 일치나 실제 서버 대상 실행의 증거로 해석하지 않는다.

서버 `FabricDecisionSessions.issue`는 현재 나라·주 대상 행과 대상별 이름·국기 키를 보내며, 주 대상에는 `highlightStates`로 해당 주 번호를 넣는다. 선택형 임무는 READY/TAKE, 진행 중 임무는 ACTIVE/남은 시간으로 표시한다. 클라이언트의 합성 상태 갱신 검사는 서버의 실제 시작·취소·기한·대상 소멸 시험과 구분한다. 실제 원본 서버 결정 시험은 `ScenarioDecisionTargetsTest`·`ScenarioStateScopeTest`에 있으며, 클라이언트가 그 실행 결과를 소비하는 통합 검증과 결정 전용 개인 지도 geometry 연결은 별도다. 국가 표·원본 음악/SFX 소비는 [서버 국가 표시 표와 원본 자산](#서버-국가-표시-표와-원본-자산)과 [음향](audio.md#서버-스토리-신호-소비)을 따른다. 필터 없는 전체 서버·클라이언트 검증은 담당 이슈에서 슬롯을 조율한 뒤 수행하며, 운영 접속·소속 변경/실제 재접속·다중접속과 사용자 장치 소리 청취는 별도 미수행이다.

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

The server issues the 47-statistic army/navy/air/special-forces grid, its exact source tooltip lines, and eight spirit slots. The client preserves source colour codes and blank-line separators; statistic values scale within their cells without clipping percent/hour suffixes. Army/navy/air backgrounds and spirit choice banners use external original artwork. Officer choice windows use two columns and the server-issued currency, candidate IDs, token and revision. A pending submission disables repeated clicks until the next authoritative revision. All six government appointment slots use two columns; law choices use one column.

`ScreenOfficerSourceTest` reads server-generated fixtures from `HOI_OFFICER_PREVIEW`, renders KOR/PRK/PRC initial and KOR selected states at GUI scale2, captures all47 statistic tooltips and all eight spirit-choice windows with candidate hovers, and tests actual left-button events, duplicate suppression and authoritative re-enabling. It also captures the source IDEA_WINDOW_SWITCH replacement prompt and selected-tile state. Identical naval/air tooltip titles are matched by their occurrence in the issued grid. Fixtures and injected request collection do not prove real network transport, two-client synchronization or socket reconnection.

Exact original state comparison still requires the PRC2040 save and active playset. The source replacement prompt and candidate percentages/precision are rendered; unsupported internal condition keys are replaced with a readable unavailable status. Preferred tactic controls issue bounded private requests and use server command-power quotes, selected-row highlighting and duplicate-click prevention. The 47 source definitions remain separate from supported runtime tactics; unsupported combat phases cannot be selected. The four officer roster controls and their dedicated routes are removed by user request. Actual army/marshal appointments and fleet organization remain available through map and unit controls. Engine formula/rounding uncertainties remain server-side evidence limits; the client must not infer values. Doctrine details/mastery, map commander appointment and branch missions follow the officer stage; see [server officer-corps boundaries](../../HOI/docs/state-affairs/officer-corps.md).

## Diplomatic justification status

The existing left relations column displays pending and usable war-goal rows with the source UI initiator/receiver icon and the other country’s flag. Hovering the row shows the server-supplied direction and remaining game days or validity. A fresh server response updates the row; selecting another country, cancellation, expiry or revoked sessions clears it. The flag retains its existing country-navigation behavior. The optional Relation.detail field is shared with HOI and accepts legacy JSON without the field. No client-side clock, war-goal creation or asset granting is added.

ScreenJustificationTest renders GUI2 pending, ready and outgoing cases, including saved day134/ready260 = 126 days, and checks refresh, unrelated target, cancellation/expiry and stale reconnect responses. Its synthetic renderer fixtures do not prove actual two-client multiplayer behavior; confirm the real saved goal after deploying both synchronized JARs. Existing timed_wargoal/wargoal artwork is a common HOI4 UI dependency recorded in HOI-resourcepack provenance; it supplies no vanilla gameplay rules.

## Commander views and original-role boundaries

`ScreenCommanderRoster` renders an authoritative COMMANDER dialog beside the officer panel. It sends only an issued choice/token/revision, disables repeat submission while pending, ignores older replies and unlocks after an authoritative refresh. Source portraits retain their aspect ratio. Army commander/field-officer and navy admiral/headquarters buttons use different server lists; no air general or air field marshal is introduced.

`MapCommanderHud` shows private army, fleet or wing cards using actual saved assignments. An empty-country server snapshot, country/dimension changes and disconnection clear the cards and cached order context; delayed snapshots alone do not expire an authorized selection. The army appointment and marshal controls use server quotes. A single bottom-aligned card row supports local paging and wheel navigation and exact selected-unit keys in CREATE_ARMY/MOVE_DIVISIONS requests; the count row opens a server-issued move quote when own map units are selected. Portrait and marshal clicks remain distinct. Unassigned cards show their count without inventing an assigned command bonus; army/marshal rows no longer overlap. Army and army-group card dragging sends the server card-order/formation contract; original visual review and actual multiplayer validation remain separate. Navy headquarters and field-officer views are still read-only until their original assignment costs/generation rules are confirmed.

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

금색 능력 4개(force_attack, last_stand, staff_office_plan, extra_suplies)는 원본 아트·이름·설명을 유지한 직접 요청을 보낸다. 서버가 사용 조건·과금·실제 효과를 최종 검증하며 미확정 효과는 거절한다. 클라이언트는 자체적으로 지휘력을 소비하거나 능력을 활성 상태로 표시하지 않는다. 기존 조건·계수 조회 자료와 HQ 관계·지속 시간 기록을 실제 HQ 배치나 능력 효과로 표시하지 않는다.

해군 대상 팝업은 원본 9함종의 2행 배치와 서버의 실제 제외 집합을 사용한다. 체크된 항목은 교전 제외이며 구세이브의 수송선 제외 플래그도 복원한다. 임시 태스크 포스 선택 창 없이 현재 선택된 태스크 포스에 같은 함종 설정을 원자적으로 적용한다. 선택 집합과 각 항목의 기존 제외 상태를 서버에서 확인하며 하나라도 바뀌었으면 모두 거절한다. 선택 집합의 제외 상태가 섞인 경우 첫 클릭은 모두 제외하고 다음 클릭은 모두 허용한다. 이 혼합 선택 처리 규칙의 원작 엔진 일치 관측은 별도다. 혼합 함종 전투의 사격 배분을 원본과 동일하다고 표시하지 않으며 적에게 공격받는 상황을 취소하지 않는다.

원본 전역 제거 아이콘을 클릭하면 지도에서 제거할 전역을 선택하고 우클릭하면 현재 선택 부대가 속한 함대들의 전체 배정 전역을 제거한다. 대상 함대를 다시 고르는 임시 창과 전역 목록·확인 창을 거치지 않는다. 현재 발급된 태스크 포스 선택 집합과 함대별 배정 상태를 다시 검증하고 전체 작업을 한 군사 트랜잭션으로 적용한다. 배정 전역 제거는 실제 함선 위치·이동 경로·편성을 변경하지 않으며, 기존 임무의 해당 전역 기여를 중단한다. 육군 전체 계획 삭제의 사용자 요청 확인 창은 유지한다.

육해공 전용 명령 키는 등록하거나 실행하지 않는다. 초계·타격부대·통상파괴·수송선 호위·기뢰 부설·소해·상륙지원 대기, 편성·정지·배정 해제·전략 이동·결속도는 각 버튼의 마우스 요청으로 처리한다. 버튼의 Shift/Ctrl+클릭 조합과 일반 창 닫기·초안 확정은 유지한다. 실제로 지원하지 않는 원본 결속도나 상륙 지원 행동을 완료로 표시하지 않는다.

항공집단 생성·해제·병합은 실제 저장된 집단 ID와 비행단 구성원을 사용하며 육군 부속 및 기체 병합과 독립적이다. 비행단 툴팁은 실제 설계별 기체 수와 부속군 추종 결과를 표시한다. 점검 주기·내부 카운터 같은 진단 설명은 명령 툴팁에 삽입하지 않으며 실제 대기·실패 이유는 유지한다.

`MapCommanderPanelsTest`와 `MapCommanderOrdersTest`는 서버 원본 fixture로 GUI2 2560×1440·1280×720의 스킬 직접 입력·반복 클릭·오른쪽 클릭·구형 요청 전환을 검사한다. `ScreenDeterminateCommandDialogsTest`는 해역·보충 조회창의 경계와 입력을 검사하며 옛 스킬 조회창을 촬영하지 않는다. 새로운 캡처는 검증 실행마다 다시 생성해 직접 확인해야 하며, 테스트 정의나 원본 아트 수입만으로 렌더 통과·한국어 원본 글꼴 동등·실제 다중 사용자 동기화를 주장하지 않는다. 일반 UI 클릭·호버음 검사는 실제 능력 효과 적용이나 장치 청취의 증거가 아니다.

긴 읽기 전용 능력 설명은 원문·빈줄·색상을 보존한 채 화면 안에 배치한다. 목록 옆 공간을 우선 사용하여 다른 행을 가리지 않으며 행 전환 시 해당 변종의 설명으로 갱신한다. 화면보다 긴 본문에는 기존 휠 스크롤을 사용한다. 지도 지휘 패널·초상화 툴팁은 HUD 추출 뒤 즉시 그려 일반 화면의 이미 끝난 툴팁 처리에 누락되지 않게 한다. 비행단 원장과 이름은 실제 장비 이름을 표시하며 내부 설계 UUID를 사용자 문구로 대신하지 않는다. 공군 진단 설명의 실제 호버 캡처도 두 GUI2 해상도에서 확인한다.

### GUI2 카드와 지도 직접 명령

군집단·군·함대 카드 행은 하단 액션바 위에 놓고 펼친 카드·부속 비행단·함대 선택과 팝업도 액션바 클릭 영역을 침범하지 않게 배치한다. GUI2 큰 화면과 작은 화면에서 같은 배치 규칙을 사용하며 사용자의 미선택 상태를 자동 선택으로 바꾸지 않는다. 직접 명령 안내문은 카드 상단 장식과 도구막대 제목을 포함한 전체 하단 영역 위에 놓고 팝업 클릭 영역도 피한다. 목표 입력 안내는 해당 입력 상태가 끝날 때까지 유지한다.

군·군집단 카드 클릭은 서버가 제공한 전체 구성원 ID를 사단 선택 모델에 반영한다. 접힌 하위 군이나 화면에서 생략한 행도 구성원에 포함되며, 우클릭 이동과 선택 표시는 같은 ID 집합을 사용한다. Shift 카드 선택은 군 구성원 합집합을 갱신한다. 사용자가 행·지도에서 부분 선택한 뒤에는 일반 스냅샷 갱신이나 새 사단 추가가 그 선택을 전체로 확대하지 않는다. 닫기·선택 해제·국가·군종 전환은 해당 선택과 명령 범위를 제거한다.

지도 우클릭은 서버 세션·리비전에 묶인 방향 벡터를 보낸다. 서버가 실제 시점과 지형에서 목표를 계산하고 자국 권한·범위·부대 상태를 검증한다. 진행 중 중복 입력은 보내지 않으며 선택·모드·국가·세계·카메라 변경은 진행 중 제스처를 폐기한다. 공군 배정과 육해군 이동 화살표는 서버의 저장된 상태를 사용한다. 사단 이동과 함대 이동·초계는 원본 `gfx/mapitems/movearrow_move.dds`(노란 몸통·검정 테두리, 진행한 구간은 밝은 색)를 사용하며 선분으로 그린 단순 화살표는 사용하지 않는다. 공군 배정은 이동 화살표와 구분되는 전용 화살표를 사용한다. 원작 화면(공군 지도 모드 캡처)대로 테두리 없는 납작한 띠에 띠보다 넓은 삼각형 화살촉을 붙이며, 공군 지도에서는 자국 비행단의 모든 임무 배정을 표시한다. 색은 원본 `NGraphics.AIR_MISSION_ARROW_ACTIVE_COLOR`(초록)와 `AIR_MISSION_ARROW_SELECTED_COLOR`(노랑)를 따라 선택하지 않은 배정은 초록, 선택한 비행단의 배정은 노랑으로 그린다. 색과 불투명도는 원본 값(초록 `0,1,0,0.5`, 노랑 `1,1,0,0.8`)을 흰 콘크리트 텍스처에 곱해 그리므로 텍스처 밝기만큼 약간 어둡다. `AIR_MISSION_ARROW_NONACTIVE_COLOR`(흰색 0.2)에 해당하는 "공역만 배정되고 활성 임무가 없는" 상태는 hoi 비행단 모델에 없어 표시하지 않는다. 실제 렌더 검사와 모의 전송 검사는 운영 다중 접속·실제 소켓 재접속 검증을 대신하지 않는다.

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

장교단 영역 제목과 정신·참모 선택창 제목·부제, 국가 선호 전술 창 제목·필터 탭, 포획된 지휘관 제목·탭은 각각의 표시 칸 중앙에 배치한다. 정신·참모·법률 선택창의 후보 이름과 비용은 사용자가 지정한 원본 화면(Steam `20261011144045_1.jpg`~`20261011144528_1.jpg`)과 원본 GUI 정의를 따른다. 원본 2픽셀을 GUI2의 1단위로 옮긴다.

- 참모총장·최고사령부(`advisor/<슬롯>`): `command_character_view.gui`의 `command_character_view_window`. 창 500×590, 제목 y5, 부제 y30, 배너(`GFX_overview_top_strip_small` 4프레임: 육군·해군·공군·최고사령부) y45 부근에 창 폭 99%로 원본 502×59 비율을 유지해 배치, 후보 격자 시작 y114, 칸 231×74 2열. 칸 안에서 초상화 중심 (42,38), 이름 (76,10) 왼쪽 정렬, 특성 두 줄 (76,28), 비용은 오른쪽 끝 222에 맞춘 오른쪽 정렬 y47. 현재 인물은 `GFX_idea_entry_bg_2`의 둘째 프레임을 쓰고 비용을 표시하지 않는다. 정치 탭과 장교단 탭에서 같은 위치·같은 배치로 연다.
- 정신(`officer/<군종>`): `countryofficercorpview.gui`의 `spirit_window`. 제목 y2, 부제 y24, 장식 배너 y50, 후보 격자 시작 y122, 이름 (76,10) 왼쪽 정렬, 경험치 비용은 칸 오른쪽 아래.
- 법률(목록): `countrypoliticsview.gui`의 `political_ideas_window`와 `political_selectable_idea_entry_list`. 칸 462×75 1열, 이름 x84 왼쪽 정렬·세로 중앙, 비용은 x271을 중심으로 한 가운데 정렬.
- 장관(`minister/<슬롯>`): 정부수반·외무·재무·내무·정보·국방 모두 같은 창의 `political_selectable_idea_entry_grid`. 칸 231×75 2열, 이름·특성 x67, 초상화 중심 (42,38), 비용 오른쪽 끝222. Steam `20261011144059_1.jpg`는 정부수반, `20261011144101_1.jpg`는 정보부장, `20261011144102_1.jpg`는 국방장관의 실제 두 열 선택창이다. 부제는 서버가 해석한 `IDEA_WINDOW_ADD`, `IDEA_WINDOW_SWITCH`, `IDEA_WINDOW_CANNOT_SWITCH` 원문을 사용한다. 특성 요약이 비용 영역을 넘으면 글자 크기를 줄이지 않고 말줄임하며 서버가 발급한 원본 효과 본문은 툴팁에 유지한다.

비용 아이콘과 숫자는 한 묶음으로 배치하고 숫자 폭을 실제 글꼴 폭으로 계산해 칸 밖으로 나가거나 이름·특성과 겹치지 않게 한다. 조건 때문에 선택할 수 없는 후보는 숫자 앞에 원본 `trigger_no` 아이콘을 두고, 비용이 부족한 후보는 숫자를 붉게 표시한다. 글자는 기본 배율(1.0)로 그리며 칸 폭을 넘는 이름만 폭에 맞춰 줄인다.

참모·법률·장관 선택창은 두 부모 탭에서 원본 show_position (710,80)을 GUI2의 (355,40)에 사용한다. 창과 안쪽 후보 판은 원본 `GFX_tiled_window_thin_border2`와 `GFX_tiled_generic_overlay_bg1_small`을 기존 nine-slice로 그린다. 정치력 비용은 바닐라 HUD 아이콘 대신 TFR `GFX_pol_power`의 보라색 `gfx/texticons/pol_power_20x20.dds`를 사용한다. 초상화는 투명 여백을 자르지 않고 원본 62×67 크기의 프레임과 종횡비를 보존한다. 제목·이름은 흰색과 굵은 스타일로 구분한다.

후보 상태는 서버 `ProtocolDialogView.Tile.overlay`로만 그린다. `politics/choice/unavailable`이면 X·회색 비용·흑백 초상화/법률 아이콘을 표시한다. `politics/choice/locked`이면 X 없이 흑백 초상화/아이콘을 표시하고 비용은 충분할 때 노란색, 부족할 때 서버 `§c`에 따라 빨간색이다. 빈 overlay는 컬러이며 정치력 부족만으로 X나 흑백을 적용하지 않는다. 현재 인물과 공석의 빈 틀도 잠김이면 흑백이다. 현재 인물의 녹색 칸 배경은 유지한다. 클릭 가능 여부는 기존 서버 `choices`만 사용하며 overlay가 요청 권한을 바꾸지 않는다.

`UiAssets.drawGrayscale`는 기존 외교 화면의 `UiDiplomacyAssets.shade`를 재사용한다. 읽기 전용 원본 `gfx/FX/buttonstate.shader`의 `PixelShaderDisable`과 같은 RGB 계수(0.212671, 0.715160, 0.072169)를 적용하고 원본 크기·픽셀 수·알파를 유지한다. 큰 인물 사진을 빈 틀 위에 기울여 합성하는 기존 경로는 사진과 틀을 각각 변환한다. 동적 텍스처는 이미지별로 재사용하고 리소스 재로딩 시 해제·재생성한다. 외부 원본 자산은 계속 리소스팩에서 읽으며 팩 파일 추가·용량 증가는 없다.

원본 글꼴은 한국어 패치의 `TFR_fonts_korean.gfx`가 선택한 Neo둥근모 bitmap이다. `VCR02_20`의 행 높이는20px, `VCR02_12`는 실제14px 글꼴/행 높이15px, `VCR02_10`은10px 글꼴/행 높이11px이다. Minecraft 기본 글꼴을 GUI2에서 원본 특성·비용 크기로 줄이면 픽셀 배율이 분수가 된다. 2026-10-11 CMP-3의 보드 결정 "지금 글꼴 유지"에 따라 기본 글꼴·크기·특성 2줄 말줄임·칸 배치를 유지한다. 남은 차이는 원본과 다른 글자 크기/모양, 흰색 대신 회색인 특성 글씨, "육군 공…"처럼 잘린 특성이다. 원본 bitmap atlas를 외부 팩의 전용 font provider로 가져오는 대안은 사용자 승인 전에는 도입하지 않는다. 대안의 작업 범위는 세 글꼴의 단일 DDS atlas(각 2,727글리프) 변환과 출처 기록, 글리프별 간격·오프셋·외곽선 대응, 한글 누락 글리프 대체 및 배치·줄바꿈·선명도 회귀 검증이다. 원본 세 atlas는 1024×1024, 1024×2000, 1024×2200이며 단순 RGBA 저장 시 약20.4MiB다. 최종 provider의 메모리·성능과 구현 시간은 측정하지 않았다.

PRC 선택창 집중 검증은 `./gradlew.bat test runGameTest remapJar -PhoiTestSuite=politics-choice --console=plain --no-daemon`이다. `FabricPoliticsChoiceFixturesTest`가 최신 서버의 실제 원본 후보·순서·초상화·조건·비용·상태 키와 현재 선택/정치력 부족 상태를 생성하고 기존 provenance 검증을 거쳐 렌더한다. 실제 GameTest 캡처를 `python scripts/compare_politics_choices.py --originals <Steam screenshots> --renders <run screenshots> --fixtures client/build/generated/authoritative-fixtures/politicsChoice --output build/politics-choice-comparison`에 넣으면 리사이즈 없는 1:1 비교PNG12장(정부수반·정보부장·국방장관 포함), 장관6창/참모 상태/잠긴 칸과 법률 상태 보조3장, 원본/렌더/fixture/결과 SHA-256 manifest를 만든다. 네 참모 창의 두 부모 탭 내부 픽셀 동일성과 동적 텍스처 해제·재생성 전후의 픽셀 동일성도 검사한다. 실제 렌더링 GameTest는 흑백 텍스처 전체의 무채색·크기·알파, 재로딩 시 해제, 비활성 클릭 차단·우클릭·드래그·반복 클릭·리비전 응답을 검사한다. 여섯 정부 슬롯은 실제 후보·초상화로 각각 렌더하고 잠김 상태와 두 번째 후보의 오른쪽 열 좌표를 검증한다. 외무·재무·내무장관은 제공된 원본 사진에 해당 장면이 없어 원본 사진과의 1:1 비교 대상에는 포함하지 않는다. 시험 캠페인2020 상태와 원본 스크린샷의2023 상태·현재 인물·비용·후보 구성·교체 가능 여부는 맞추지 않는다. 이 검증은 운영 접속·실제 팩 다운로드·다중 플레이어 검증을 대신하지 않는다.

2020 새 캠페인의 PRC 최고사령부는 서버의 초기 인물 숨김 처리를 반영해 후중밍·황밍·먀오화·왕하이장·쉬치량·장유샤·창딩추·왕하이·우야난·쿵쥔 순서의 10칸이다. 쑨젠궈·장메이위 칸은 없다. 마지막 네 인물은 조건 미충족 overlay에 따라 X·흑백·회색 비용으로 표시한다. 2023 원본에서 조건을 충족한 쿵쥔의 일곱째 칸과 2020의 마지막 칸 차이, 시험 fixture의 장유샤 임명 상태와 원본의 미임명 상태는 맞추지 않는다. 충분 상태의 최고사령부 fixture는 정치력2000에서 장유샤 임명비110을 차감한1890이며 부족 상태는0이다. 비교 시트의 정치력 표시는 시작값 대신 각 응답의 `body`를 확인한다. 후보 수·순서·조건은 서버 응답을 따르며 클라이언트에 고정하지 않는다. 이전 실행과 비교할 때 창 내부(720,85)-(1200,660)를 사용하고 주변 월드 배경과 창 바깥 픽셀 차이를 UI 변화로 취급하지 않는다. 집중 실행과 전체 실행의 재무장관 툴팁은 건설 속도 아이콘 오른쪽 경계(x769/y192~198) 7픽셀의 RGB 값이 달랐고 문구·수치·배치는 같았다. 경계 샘플링 차이로 보이지만 원인은 별도로 검증하지 않았으며 모든 툴팁의 완전한 픽셀 동일성을 주장하지 않는다.

선택창 자산은 외부 UI 팩에 있다. `UiAssets.draw`는 리소스가 없으면 크기(0,0)를 캐시하고 `false`를 반환해 해당 이미지를 그리지 않는다. 배너·후보 배경·현재 인물의 녹색 배경만 동시에 누락되면 실제 받은 팩에 `assets/hoi/textures/gui/politics/choice/`가 있는지와 manifest의 SHA-1을 먼저 대조한다. 과거 팩 누락과 현재 설치 상태를 구분하며 코드/JAR 변경만으로 팩 적용을 추정하지 않는다.

교리 군종 탭·대교리명·분류명·하위 교리 이름판·선택창 제목과 버튼 이름, 필요 통달치 숫자도 해당 칸 중앙에 정렬한다. 현재 통달치는 아이콘과 숫자의 표시 폭을 합친 묶음을 계수판 중앙에 배치한다. 긴 설명·목록·툴팁 본문은 왼쪽 정렬을 유지하며, 단독 `---`(세 개 이상의 `-`)와 기존 빈줄은 빈 행으로 남긴다. 장교단의 교리·참모·포획된 지휘관·육군 사령부 배지 툴팁은 서버가 보낸 원본 문구를 제목을 덧붙이지 않고 그대로 표시하며, 지정된 인물이 없는 참모 칸은 `클릭하여 … 추가` 한 줄만 표시한다. 중점 상세창의 제목·기간·조건·설명·효과 글자는 기본 배율(1.0)로 그리고 넘치는 내용은 각 영역의 스크롤로 본다. 글자 크기·GUI2 배율·클릭 영역·비활성 색과 초상화·훈장·통화 아이콘의 위치는 유지하고, 원문 색상 코드·명시된 인라인 아이콘·들여쓰기·수치 정밀도를 보존한다.

통달 단계 수치는 서버의 보상별 요구량을 누적한다. 단계 번호로 군종 팔레트를 순환하지 않는다. 숫자 배경은 원본 `doctrines_view.gui`의 `mastery_text_bg` 기본 프레임과 육군 원본 화면에서 확인한 중립 프레임을 사용한다. 숫자의 노란 강조색은 구매 비용·조건의 빨강/초록 판정과 구분한다. `doctrine_folders.txt`와 배경 DDS에는 육군·해군·공군·특수부대 팔레트가 있지만, 숫자 배경을 달성 상태에 따라 군종색으로 전환하는 엔진 규칙은 설치 텍스트 및 지정 60장에 없다. 요청된 100·200 등 단계 숫자 자체의 군종별 색 구분은 확인 필요 항목이다. 숫자 글자는 노란색, 작은 숫자 배경판은 중립 프레임이며 달성 상태색을 추정 적용하지 않는다. 대교리·하위 교리 이름판의 군종 색을 유지한다. 보상 배경은 원본 8프레임이 군종별 달성·잠금 순서로 교차 배치되어 있으므로 육군 0/1, 해군 2/3, 공군 4/5, 특수부대 6/7에 연결한다. 기존의 군종+잠금 오프셋 4 계산으로 다른 군종 배경이 나오던 오류를 수정했다. 달성 배경은 육군 녹색·해군 청색·공군 적색·특수부대 금색이고 잠금 배경은 각 군종의 회색 프레임이다. 보상 아이콘·잠금 판정과 실제 진행은 유지한다.


### 명령 셀과 건설 상태색

육군 명령 아이콘과 클릭·툴팁 영역은 원본 `bpt_bg` 내부 각 셀을 함께 사용합니다. 훈련도 첫 셀 안에 배치하며 투명 여백을 제외한 아이콘 내용을 종횡비를 유지해 맞춥니다. 해군 임무와 공군 임무·편성 버튼은 인접 셀 사이의 간격을 보존합니다. 스킬·명령·군 카드·액션키·핫바의 기존 독립 표시 공간은 유지합니다. 육군 미선택 상태에서도 스킬 틀과 명령 줄은 남고 선택 안내를 표시합니다. 이 상태의 클릭은 군 선택이나 서버 요청을 만들지 않습니다. 실제 미선택 GUI2 캡처에는 일반 핫바를 함께 표시하며 하단 키 안내는 표시하지 않습니다.

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

집단군 카드의 펼침 상태는 선택과 분리해 국가·차원·군종 및 안정 ID로 유지한다. 집단군을 처음 선택하면 소속 군을 표시하고, 이후 명시적으로 접은 상태는 날짜 갱신이나 재선택으로 다시 열리지 않는다. 펼친 소속 군은 집단군 바로 오른쪽에 연속한 묶음으로 모인다. 큰 집단군은 페이지마다 집단군 카드를 함께 표시한다. 집단군 색상은 서버 ArmyGroup의 RGB 상태를 저장·동기화하며 색상 선택 요청에 사용한다. 카드 전체 바탕과 상시 색 띠는 표시하지 않는다. 집단군 표식 클릭으로 원본 COMMANDGROUP_PRESET_COLORS_HSV의 10색을 선택할 수 있다. 구세이브의 없는 색상은 미지정으로 보존하고 중립 회색으로 표시한다. 자동 색상 배정은 추정하지 않는다. 색상 선택 후 서버 응답 전에는 임의로 색을 바꾸지 않는다. 집단군 카드는 접기 버튼만 있고 X는 없으며, 군·사단에는 접기 버튼이 없다. 함대의 펼침 버튼은 상세 목록만 접고 선택·소속·임무를 유지한다. 군 카드 X는 사단을 보존하는 군 해체의 서버 확인창을 열고, 함대 카드 X는 선택만 해제한다. 해체는 서버 확인 절차에서만 실행한다. 카드 제어 버튼의 우클릭은 편성 이동이나 지도 입력으로 전달하지 않는다.


`MapCommanderGroupsRenderTest`는 군·함대의 상세 목록과 하단 임무 대상 아이콘을 별개로 검사한다. 펼친 목록의 존재, 접은 목록의 숨김, 다시 펼친 뒤 복원, 정확한 선택 ID 보존과 하단 임무 제어의 유지를 GUI2 두 해상도에서 실제 클릭으로 확인한다. 같은 부대명 툴팁을 가진 하단 아이콘을 상세 목록 행으로 간주하지 않는다.

`MapMovementOverlayTest`의 실제 서버 시계 진행 자료를 `MapMissionOverlayRenderTest`가 읽어 육군·해군 각각 시작/중간/다음 프로빈스의 화살표와 부대 카운터를 함께 촬영한다. 시작과 중간의 카운터 화면 좌표는 같고 다음 프로빈스에서 바뀌어야 한다. 게임 분·구간 진행률·프로빈스·화면 좌표를 같은 시퀀스 JSON에 기록한다. 평면 시험 월드에는 실제 경로를 평행 이동해 표시하며 이 렌더 검증을 운영 지도·실제 소켓 동기화나 원본 곡선 형태의 완전한 재현으로 간주하지 않는다. 집중 실행은 `runGameTest -PhoiTestSuite=movement-controls`이다.

### 공군 기지별 선택 패널

공군 임무 줄은 원본 `airselectionview.gui`와 지정 Steam 화면의 공중 우세 → 근접항공지원 → 요격 순서를 사용한다. 기지 헤더는 원본 배경·국기·기지 이름·수용량·창설·닫기 배치를 연결한다. `MapCommanderAirBases`는 서버의 기지 ID로 묶고 `AIRBASE_CAPACITY`, `AIRBASE_AIRPLANES_KNOWN`, `AIRBASE_CAPACITY_COUNTS_DESCRIPTION` 원문에 전체 주둔 기체 수와 실제 수용량을 넣는다. 구형 응답의 누락 값은 0으로 만들지 않고 미확인으로 표시한다. 이름이 같아도 다른 기지를 합치지 않는다.

기지의 창설 버튼은 해당 기지 ID를 보내며 X는 해당 기지에서 선택한 비행단만 선택 해제한다. 다른 기지의 선택과 모든 임무·기체 상태는 유지한다. `MapAirBasePanelsTest`는 GUI2의 2560×1440 및 1280×720에서 서로 이름이 같은 두 기지의 독립 헤더, 선택 수와 다른 실제 수용량, 근접항공지원 버튼 요청, 기지 지정 창설 요청과 선택 해제 후 갱신을 실제 클라이언트 입력·렌더 경로로 검사한다. 수용량 호버는 자국·전체 수량의 구분, 원본 색상·문장·빈줄, 치환되지 않은 변수 없음과 실제 툴팁의 화면 경계를 검사한다. 요청 관측기를 사용하는 패널 검증은 운영 서버의 다중 사용자 접속 검증과 구분한다.

비행단 행은 원본 장비 정보 버튼, 실제 기체 수/충원 한도, 보충 정책, 부속 군, 임무, 주야간 설정과 닫기를 별도 영역으로 사용한다. 장비 이미지는 실제 서버 장비 연결을 사용하고 함재기 표시는 실제 설계에만 붙인다. 주야간 설정·보충 정책·장비 상세는 누른 비행단 하나의 ID로 요청하며 행의 X는 같은 기지의 다른 비행단까지 선택 해제하지 않는다. 구형 응답에 행 정보가 없으면 기존 행을 유지한다. 서버 확인 전에는 시간대나 정책을 임의로 바꾸지 않는다.

기지 연료 아이콘은 원본 위치와 이미지를 사용하며 툴팁은 현재 국가의 권위 연료 정보를 표시한다. 기지별 연료 탱크를 별도로 계산한 것으로 표시하지 않는다. `MapAirBasePanelsTest`의 실제 GUI2 입력 검사는 비행단별 시간대·정책·상세 요청과 같은 기지 및 다른 기지의 선택 보존을 포함한다. 육해공 하단 키 안내와 전용 단축키는 표시·연결하지 않으며 스킬·명령·군 카드·액션 버튼은 유지하며 전략 지도 차원의 일반 핫바는 아래 지도 HUD 규칙에 따라 숨긴다. 원본 기지 관계 아이콘의 프레임 의미, 비행단 공격 성향·에이스 조작은 확인·구현이 남아 있다.

명령·임무·스킬 툴팁은 원본 문장 뒤에 구현 해설이나 일괄 지도 입력 안내를 덧붙이지 않는다. 상시 하단 목표 지정 안내도 표시하지 않는다. 서버가 거절한 이유와 일시적인 연결·선택 오류는 유지하며 권한 검증이나 기능 제한을 숨기지 않는다. 해군 직접 배치 코어와 기존 회귀 16개가 불변 의존성 스냅샷에서 통과했고, 서버 입력·상태 회귀 18개는 2026-10-09 14:03:34 UTC 전체 코어 체크포인트로 재검증했다. 클라이언트 명령 코드 3개와 관련 GUI 회귀 소스 3개는 컴파일을 통과했다. 실제 GUI2 렌더·조작과 최신 전체 합본 gate는 별도로 수행해야 한다.

의용군 파견·귀환 수송 중인 사단은 `divisionPresent` 판정에 따라 지도 모델·카운터·지도 선택키·시야 기여에서 제외한다. 사단 자체와 군 편성·목록은 유지하며, 도착 후 ACTIVE 상태에서는 기존 소유국의 지도 선택과 시야를 복원한다. `MapVolunteerPresenceTest`는 저장형 상태를 복원하여 이 경계와 타국 선택 거절을 검사한다. 수송 중인 상세행은 원본 `alert_volunteer_transfer_instant`의 “§H이송 중인 의용군§!”을 표시하고 대기·임무 미배정 접미문구를 붙이지 않는다. `FabricCommanderVolunteerStatusTest`는 파견·귀환·도착 상태의 실제 HUD 응답과 프로토콜 왕복을 검사한다. 실제 지도 모델 렌더 실행은 별도 검증한다.

## 복수 장비 무기대여와 의용군 화면

`ScreenDiplomacyLease`와 `ScreenDiplomacyVolunteers`는 원본 개별 패널·버튼·아이콘·스크롤 자산으로 GUI2에서 조립합니다. 전체 스크린샷을 배경으로 사용하지 않습니다. `UiDiplomacyAssets`는 원본 64픽셀 테두리와 반복 중앙 영역, 스크롤 경계, TFR 버튼 셰이더의 명암·시간 변화를 처리하며 화면 종료·리소스 재로드 때 생성 텍스처를 해제합니다. 원본의 192 선언/190 실제 이미지 차이는 현재 실제 크기로 처리하므로 원작 엔진의 UV 해석까지 동일하다고 주장하지 않습니다. 제작국·함재기 배지·군 방패 문양과 색은 권위 있는 상태가 없으면 임의로 표시하지 않습니다.

무기대여는 서버가 발급한 장비를 최대 32종 선택하고 각 수량·일회/월별 방식을 편집합니다. 최신 PREVIEW와 동일한 내용만 SUBMIT하며, 시간 초과 후 변경 요청을 자동 재전송하지 않습니다. 원본 생산 비율·연료 항목은 규칙 미확정 사유와 함께 비활성입니다. 실제 재고·생산 수치를 사용하지만 원작의 K/M 축약과 기간 전환까지 검증한 것은 아닙니다. 의용군은 실제 군·사단·지휘관 상태를 표시하고 군 선택·국가·세션·리비전을 검증합니다. 군사 파견·귀환 연결과 한도·위협이 미확정이면 실행을 비활성으로 유지합니다. 취소·완료 복귀 시 국가 화면을 다시 조회하고 권한 회수 뒤 응답으로 화면을 다시 열지 않습니다.

`ScreenDiplomacyLeaseTest`와 `ScreenDiplomacyVolunteersTest`는 `diplomacy` 시험 묶음에 포함됩니다. 의용군 입력은 `FabricDiplomacyVolunteerSessionsTest`가 실제 원본 부대를 재편한 `unselected`, `selected`, `multiple-selected`, `air-disabled` 4종이며 기존 부대·병력·장비가 불변임을 검사합니다. `gradle/fixture-groups.gradle`에 등록한 생산자를 실행하고 현재 소스·입력 해시를 확인하므로 수동으로 과거 JSON을 놓는 것으로 정식 검증을 대신할 수 없습니다.

현재 검증은 프로토콜 114개·클라이언트 109개 JUnit 통과, GUI 테스트 소스 컴파일, 클라이언트 전용 JAR 생성과 공유 프로토콜 46개 일치입니다. 버튼 효과·샘플링·타일링 단위 검사 8개가 포함됩니다. 실제 GUI2 캡처·원작 픽셀 대조·효과음 청취·다중 접속은 아직 실행하지 않았습니다. 서버 공통 군사 API 체크포인트와 최신 입력 자동 생성 뒤 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy --console=plain --no-daemon`으로 검증합니다.

무기대여만 독립적으로 검사하려면 서버의 `FabricDiplomacyLeaseRenderFixtureTest`가 생성한 디렉터리를 `HOI_LEASE_PREVIEW`로 지정하고 `.\gradlew.bat :client:runClientGameTest -PhoiTestSuite=diplomacy-lease --console=plain --no-daemon`을 실행한다. 이 검사는 `empty.json`, `once.json`, `monthly.json`, `multiple.json`, `fixture-source.json`이 모두 있어야 하며 뷰 파일과 세션 소스의 SHA-256을 대조한다. 자료는 실제 초기 캠페인의 재고·생산에서 발급하고 PREVIEW 전후 전체 저장 상태 불변과 저장 콜백 미호출을 검사한다. 생성기 시험의 출력은 `hoiLeasePreview` 시스템 속성 또는 `HOI_LEASE_PREVIEW` 환경 변수로 지정한다. 고정 군사 의존성으로 만든 독립 자료에는 해당 runtime baseline을 기록하며 최신 전체 서버 소스 검증으로 간주하지 않는다. `all`·`diplomacy` 검사는 `diplomacyLease` 그룹의 최신 서버 생성·해시 검증을 그대로 사용한다.

입력 회귀용 합성 뷰의 캡처명은 `hoi-diplomacy-lease-synthetic-*`, 실제 서버 발급 뷰는 `hoi-diplomacy-lease-source-*`로 구분한다. 실제 자료 렌더 중 뷰·초안·제출 권한을 고치거나 전송 요청을 만들지 않는다. 분류 탭과 실제 비활성 연료·생산 비율 툴팁도 원래 발급 상태로 검사한다.

### 세 이벤트 창의 원본 프레임

국가 이벤트·국제 뉴스·슈퍼 이벤트는 `ScreenDialog`의 EVENT/GLOBAL_EVENT/SUPER_EVENT 분기에서 원본 TFR 틀을 사용한다. 지휘관 `leader_event`·완료 창과 다른 종류의 대화창은 이 분기를 사용하지 않는다. 원본 글꼴을 추가하지 않고 Minecraft 기본 글꼴, 제목 볼드체·본문 대비 1.5배·긴 제목 규칙, 본문 9 GUI 픽셀과 12 GUI 픽셀 줄 간격을 유지한다. 종이 위 글과 세 창의 제목은 그림자 없이 그린다. 서버 날짜 표기·선택 요청·슈퍼 이벤트 음원 경로는 바꾸지 않는다.

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

`ScreenEventFrameTest`는 두 해상도의 실제 전체 화면·창 crop, 전체 선택지 도달, 문장·문장부호 보존, 그림 비율, 본문만 스크롤, 좌우 클릭·드래그·반복 클릭·비활성·오래된 리비전·Esc를 검사한다. 원본 카탈로그의 사례는 `korea.21`, `korea.1`, `korea.2`, `korea.79`, `korea.23`, `korea.54`, 선택지3/4개, `russia.454`23개, 그림 없음, `Australia.0`, `news.113`, `news.131`, 뉴스1/4개, `gma_news.5`, TFR/TFRKP 슈퍼와 `PRC_eadi_victory_super_event`의 긴 인용문이다. `news.131`의 스크린샷 대조는 TFR `TFR_events_ZZZ_news.txt`의 `NOT tag=USA`인 `news.131.a`만 표시하는 합성 입력이며 서버가 조건을 실행했다고 주장하지 않는다.

재현은 읽기 전용 사본-7을 run scratch에 복사한 후 같은 클라이언트 체크아웃에서 순차 실행한다. 원본 사본·운영 서버·설치 묶음·프로토콜은 수정하지 않는다.

```powershell
python scripts/export_event_frame_fixtures.py --catalog <사본-7>/content/hoi/scenario/tfr-presentations.json --output client/src/gametest/resources/event-frame-fixtures.json
.\gradlew.bat test runGameTest remapJar "-PhoiServerSource=<사본-7 실행용 복사본>" "-PhoiProtocolSource=<같은 복사본>/protocol/src/main/java" --console=plain --no-daemon
.\gradlew.bat runGameTest -PhoiTestSuite=event-frames "-PhoiServerSource=<사본-7 실행용 복사본>" "-PhoiProtocolSource=<같은 복사본>/protocol/src/main/java" --console=plain --no-daemon
python scripts/compare_event_frames.py --run <현재 전체 runDirectory> --baseline <CMP-11 마지막 전체 runDirectory> --shots <제공된 원본 crop 폴더> --output build/event-frame-review --pack ../HOI-resourcepack
```

원본과의 대조는 국가(900,270)에서720×1000, 뉴스(900,320)에서720×860, 슈퍼(770,400)에서1040×680을 사용하며 제공된 짧은 국가 crop은 같은 실제 크기에 맞춘다. 비교 PNG는 왼쪽 원작 실제 화면, 오른쪽 Minecraft 실제 렌더다. 같은 영역 crop에서 긴 창 일부가 잘릴 수 있어 별도의 전체 창 crop을 함께 제공한다. `render-verification.json`은 원본 아홉 좌표와 CMP-11 전체 실행 대비 변경·추가·누락 캡처(같은 이름의 반복은 발생 순서)를 기록한다. 동적 게임 배경의 변화는 UI 수정으로 단정하지 않는다. 원본 글꼴 동등성·운영 멀티플레이·직접 효과음 청취는 별도 미수행 항목이다.

세 이벤트 창의 틀 배치 집중 검증은 사본-7 실행용 복사본과 외부 팩6개를 사용한 `test runGameTest remapJar -PhoiTestSuite=event-frames`로342.18초에 통과했다. JUnit은클라이언트110개·프로토콜114개(실패/오류/건너뜀0), 실제GUI2 입력/렌더 사례는19×두 해상도이며 전체/창·스크롤/마지막 페이지 PNG152장을 생성했다. 국가 선택지 앞의 보이는 여백30px, 뉴스20px, 국가 그림의 선택지 앞끝 기준144px(원작143px, GUI 반올림1px)을 확인했다. 첫 전체 실행은768.66초에 기존 `AudioMusicRoutingChecks`의 MAP_CONSTRUCTION 곡/채널 유지 검사에서 실패했고 음원 코드를 수정하지 않은 재검사를 별도로 기록한다.

긴 본문 잘림 보완 후 	est runGameTest remapJar -PhoiTestSuite=event-frames는67초에 통과했다(Gradle 표시). 실제GUI2 19×두 해상도·PNG152장을 다시 생성하고 줄 단위 휠·PageUp/PageDown·Home/End, 첫 줄과 마지막 글리프의 scissor 경계, 선택지 전체 도달을 확인했다. JUnit224개는 이전 통과 결과를 재사용했다. 앞선 전체 실행의 ScreenDialogChecks 둘째 슈퍼 선택지 조회는 새 한 페이지 규칙에 맞춰 다음 페이지를 연 뒤 검증하도록 변경했다.

최종 필터 없는 클라이언트 	est runGameTest remapJar는 전용 검증 시간에서16분49초(Gradle 표시), 1008.42초(run 시각 기준)에 통과했다. 전체 실제 렌더 PNG1482장을 생성했고 이벤트19종×두 해상도의 측정값은 긴 본문 수정 후 집중 검사와 모두 같았다. JUnit224개는 기존 통과 결과를 재사용하고 현재 사본-7의 자동 renderer fixture를 재생성·검증했다. CMP-11 마지막 전체 실행과 같은 이름/발생순서의 원시 PNG 해시는334개 변경·304개 추가·318개 누락이었다. 동적인 월드/배경 차이는 UI 회귀의 증거로 단정하지 않으며 정확한 목록과 로그는 CMP-12 검증 첨부물의 ull-validation.json을 사용한다. 전체 실행의 이벤트 프레임 검사는 기존 두 호출 경로에서 각각 수행되어 새 PNG304장(152장씩 두 회)이 포함된다. 기준 실행에만 있는318개 캡처의 UI 동등성은 이번 범위에서 전부 확인하지 않았다. 작업 시작 때 이미 삭제 상태였던 MapArmyRenderChecks·AudioKoreanMusicChecks와 다른 미커밋 변경은 복구·변경하지 않았다. 음원 코드는 수정하지 않았다.

동일한 사본-7 실행용 복사본에서 서버의 필터 없는 `test runGameTest remapJar -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip`는1381.66초에 통과했다. 서버JUnit은core1607개·fabric297개·protocol129개(실패/오류0)이며 core의4개는 선택적인 기록 성능 자료·프로파일링·검증된 운영 백업·운영자가 제공한 캠페인 fixture가 없어 건너뛰었다. mock GameTest169개는 모두 통과했다. 원본 사본과 실행용 복사본의 소스/콘텐츠/빌드 정의1181개는 바이트가 같았다. 서버JAR은server 전용 진입점과 사용자 모드아이콘만 포함한 것을 확인했으며 운영 배포·접속 검증은 하지 않았다. 이 서버 검사로 실제 멀티플레이나 TPS 측정 결과를 주장하지 않는다.

### 이벤트 선택지 효과 툴팁과 최소화

`ProtocolDialogView.Choice(id,label,tooltip)`의 효과 원문은 최대 3,000자이며 생략/null은 빈 문자열이다. 국가 이벤트·국제 뉴스·지휘관 이벤트의 선택지 호버는 `ScreenDecisionText`와 기존 `production/tooltip` 상자를 재사용한다. 원본 색 코드, 명시된 아이콘, LF 줄바꿈, 빈 줄, 두 칸 들여쓰기와 수치 표기를 보존하고 엔진 효과를 클라이언트에서 추가하지 않는다. 효과가 없으면 상자를 표시하지 않으며, 잘린 선택지 문구의 보조 툴팁은 효과가 없을 때만 사용한다. 효과 상자는 선택지 아래 3 GUI px에서 시작하고 화면 경계 안으로 이동한다. 화면보다 긴 상자는 기존 휠 스크롤로 끝까지 읽는다. 공용 상자/다른 화면의 툴팁 외형은 변경하지 않는다.

원본 `3350890356/interface/eventwindow.gui`의 `btn_minimize`는 국가 창 top_Window의 (35,45), 국제 뉴스의 (45,20), `EventWindow_leader`의 (10,45)이며 기존 `GFX_event_button_minimize` 33×33을 사용한다. 답할 선택지가 있는 세 창의 단추와 Esc는 서버로 빈 선택을 보내지 않고 창만 숨긴다. 한 선택지의 Enter는 기존 선택 제출을 유지하고, 여러 선택지에서 포커스 없는 Enter는 접기다. 선택지는 서버의 token/revision/choice ID로 한 번 제출하고 같은 revision으로 재활성화하지 않는다. 새 이벤트는 기존처럼 바로 표시한다. 선택지가 없는 읽기용 뉴스와 슈퍼 이벤트의 닫기·음악 동작은 유지한다.

사건과 결정의 사건 행은 `countrydecisionview.gui`의 `event_header`/`event_item`, 기존 `GFX_event_header_bg`/`GFX_event_item_bg`/`GFX_event_type_strip`를 사용한다. 행은 원본 512×33, 아이콘은 30×26 비율을 유지한다. 제목은 왼쪽에 표시하고 서버가 보내지 않는 남은 날수 자리는 비워 둔다. 행을 누르면 같은 세션 창을 다시 열거나 맨 앞으로 가져온다. 서버 Close(선택·기한 만료·국가 변경)는 접힌 세션까지 제거하고 연결/캠페인 reset도 목록을 비운다. 사건 머리글의 작은 사건·사건·뉴스 팝업 체크 상자는 [개인 설정](#결정-개인-설정과-지도-요청)을 따른다. `DECISIONSVIEW_EVENT_HEADER`의 사건 및 `MINIMIZE_EVENT`의 설명은 설치된 한글패치에 덮어쓰기가 없어 기본 한국어 `decisions_l_korean.yml`/`events_l_korean.yml`의 원문을 따른다. command/save ID는 바꾸지 않는다.

원본 `212816`과의 나란히 비교에서 공용 상자의 거의 검은 바탕과 우상단 자홍색→좌하단 보라색 테두리를 유지했다. GUI2에서는 테두리와 글리프가 정수 픽셀 단위이고 한 줄 상자는 224×36 화면 px, 원본 비교 crop은 221×48px다. Minecraft 기본 글꼴의 폭·줄높이에 따른 차이를 보존하며 원본 글꼴 동등성으로 해석하지 않는다. 세 줄 비교는 서로 다른 사건의 효과 문구를 사용하므로 상자 외형 비교다. 답하지 않은 사건 아이콘은 원본 국가 사건 행에 보이는 느낌표 프레임을 사용하고 국가·뉴스의 중요도나 남은 시간은 추정하지 않는다.

두 줄 선택지의 기본 글꼴·크기·줄 간격은 유지하고 버튼 높이 계산의 총 세로 여백만 2→6 GUI px로 늘린다. 한 줄 버튼과 이벤트 제목의 볼드체 1.5배·날짜·기존 틀 좌표는 유지한다. GUI2 1280의 `news-four`는 두 줄 선택지 세 개가 각각 4 GUI px 늘어나 본문 표시 높이가 48→36 GUI px, 마지막 줄까지의 스크롤 이동량이 216→228 GUI px가 된다. 본문 전체는 기존 줄 단위 스크롤로 읽는다.

재현은 팩을 먼저 빌드하고 Java 25로 아래 명령을 순차 실행한다. 원본 사본-8은 읽기 전용으로 보존하고 source/content/protocol을 그대로 복사한 실행용 디렉터리를 사용한다. 서버 프로토콜 소스 46개와 클라이언트 소스는 바이트까지 같다. 전체 검사 전에 같은 사본에서 수동 입력을 생성하고 `HOI_COMMANDER_PREVIEW`(지휘관 53개), `HOI_OFFICER_PREVIEW`(성공한 장교단 mock GameTest의 입력 17개), `HOI_MISSION_OVERLAY_FIXTURES`(임무 3개), `HOI_LAND_PLAN_PREVIEW`(육군 계획 1개)를 설정해야 전체 범위를 검사한다. 수동 입력 생성·경로 규칙은 [README](../README.md)의 추가 화면 입력 전체 검사 안내를 따른다.

```powershell
.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=event-tooltips -PhoiServerSource=C:/HOI-client/client/build/cmp15-server-source -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/server-snapshot-8/HOI/protocol/src/main/java --console=plain --no-daemon
.\gradlew.bat test runGameTest remapJar -PhoiServerSource=C:/HOI-client/client/build/cmp15-server-source -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/server-snapshot-8/HOI/protocol/src/main/java --console=plain --no-daemon
python scripts/compare_event_tooltips.py --run <성공한 전체 실행 폴더> --baseline <CMP-12 마지막 전체 실행 폴더> --output <비교 결과 폴더>
```

`ScreenEventTooltipTest`는 권위 있는 사본-8의 다섯 입력에서 각 선택지 호버·빈 효과·아이콘과 GUI2 2560×1440/1280×720 경계를 검사한다. 최소화·재열기·두 사건·서버 Close·refresh/revision·반복 제출·좌/우클릭·드래그·reset은 실제 클라이언트 입력 경로와 관측 요청으로 검사한다. 국제 뉴스 접기 입력은 기존 원본 프레임 카탈로그의 합성 클라이언트 사례이며 운영 뉴스 발급/기한 만료를 증명하지 않는다. 기본 Minecraft 글꼴의 폭·줄높이와 원본 글꼴은 같다고 주장하지 않는다. 운영 접속·다중 사용자 동기화·직접 효과음 청취는 별도 미수행이다. 비교 이미지는 실제 렌더 캡처와 원본 Steam 화면을 사용하며 3배 확대는 최근접 보간이다.

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

팩 `ui/story_assets.json`은 국가별 원본 국기 키와 raw 초상 GFX/경로, 동적 아이콘을 실제 외부 텍스처에 잇는 준비 자료다. 이 작업은 Java·protocol·서버 projection을 바꾸지 않았다. 새 목록의 클라이언트 소비와 서버 필드 연결은 계약의 다음 UI 작업이다. 원본 에셋 미리보기를 Minecraft 구현 캡처로 제시하지 않는다. 실제 GUI2 입력/화면·멀티플레이·직접 청취는 이 에셋 작업에서 미수행이다.

## 서버 국가 표시 표와 원본 자산

`hoi:country_display_v1`의 공개 `Entry(tag,name,flag)` 표는 서버가 정한 이름과 정확한 원본 국기 키를 보관한다. 정치/HUD 탭·외교·관계 국기와 지도 부대 국기는 같은 조회를 사용한다. `country/<tag>/flag`를 그리는 기존 렌더 경로도 이 표를 적용하므로 명령·선택 대상 ID는 그대로 유지한다. 국가 메뉴 이름·외교 제목/국가 선택 목록·지도자 툴팁은 최신 표 이름을 사용하고 표에 없는 국가는 기존 서버 스냅샷 이름을 유지한다. 외형/이념 접미사를 클라이언트에서 조립하지 않는다. 팩에 받은 키가 없으면 원래 태그 기본 국기를 쓴다. 표는 disconnect에서 비우고 리소스 재로드에서는 보존한다. 기존 결정 문장 비트맵 GetFlag 글리프는 기본 작은 국기를 유지하며 이념·외형 국기로 교체한 검증은 없다.

`ui/story_assets.json`의 원본 초상 GFX/경로와 동적 수정치 키는 기존 UiAssets 렌더 경로에서 실제 외부 텍스처에 연결한다. 서버가 정규화한 `politics/art/<GFX 소문자>` 동적 아이콘도 원본 키와 대응한다. 미등록/모호한 원본은 기존 누락 이미지 경로를 유지한다. 숫자·툴팁·인물 역할·효과는 계산하거나 보완하지 않는다.

`UiCountryDisplayRenderTest`는 실제 GUI2 2560×1440·1280×720에서 합성 country_display 표를 기존 국가 메뉴/외교 화면에 넣고 외형 변경·이름·국기 복원·누락 키 fallback·asset reload 보존·disconnect 정리를 검사한다. 원본 초상과 서버 정규화 동적 아이콘의 실제 팩 조회 및 기존 정치 메뉴 두 해상도의 렌더도 확인한다. 초상·아이콘을 추가한 입력과 캡처는 합성 검증으로 표시하고 인물 역할·수정치 효과 검증으로 간주하지 않는다. 이는 실제 서버 효과 실행/국가 외형 lifecycle·원작 장면 픽셀 동등성·다중 접속·실제 재접속 증거가 아니다. GUI 설정은 검사 전 값으로 복원한다. 집중 명령은 [음향](audio.md#서버-스토리-신호-소비)의 story-signals를 사용한다.

지도 이동 `ProtocolMapFocus.State(state,x,z,minX,minZ,maxX,maxZ)`는 주 안의 중심 칸과 주의 경계를 지도 블록 좌표로 보낸다. 원본 goto 효과는 열린 지도 관측 위치만 옮기며 주 강조를 추가하지 않는다. 현재 서버 지도 클릭은 플레이어 눈 위치에서 계산하므로 클라이언트 Camera만 이동하면 클릭 원점·청크 로딩이 어긋난다. 서버가 보낸 신호에 연결된 bounded 요청을 검증하고 기존 atlas 관측 이동 경로를 사용하도록 서버 담당에게 요청했다. 결정 화면의 HIGHLIGHT/GOTO 소비는 [현재 지도 계약](#결정-개인-설정과-지도-요청)을 따른다. 서버 스토리 goto_state 신호의 별도 수명/화면 계약과 혼동하지 않으며 atlas scene box에서 주를 역추정하지 않는다. change_tag_from·create_entity·핵 화면은 계획 8판 범위에 포함하지 않는다. 필터 없는 전체 클라이언트/서버 검증과 staged server JAR·운영 다중 접속은 집중 검사와 구분해 기록한다.


## 육군 지휘 패널 원본 배치

장군·원수 스킬 줄은 기본 스킬 3개를 포함해 합계 최대 4칸 표시한다. 추가 특성 스킬은 획득 순서의 첫 1개만 표시한다. 획득 순서는 서버 `Panel.abilities`가 정하며 클라이언트는 정렬하지 않는다. 복수 선택에서는 각 지휘관의 앞 4개 안에 있는 공통 스킬만 첫 지휘관의 서버 순서로 표시하고, 숨긴 다섯째 스킬로 빈칸을 채우지 않는다. 숨긴 스킬에는 클릭·호버 영역을 만들지 않는다. 집단군 기본+원수 특성 스킬, 군 기본+장군 특성 스킬의 기존 구분은 유지한다. `MapCommanderAbilityTest`와 `MapCommanderAbilitiesRenderTest`는 두 역할의 기본·추가 목록, 초과 입력, 순서, 복수 선택과 회색 표시를 확인한다. 합성 입력의 순서 보존은 실제 저장된 특성 획득 순서의 서버 검증과 구분한다.

지휘관 임명 목록은 집단군에서 원수→장군, 군에서 장군→원수 순으로 계급을 먼저 묶는다. 계급 안에서는 기존 기술 내림차순과 이름·ID 동률 순서를 유지하며, 정렬 헤더를 바꾸어도 계급 우선순위는 유지한다. 검색·임명된 지휘관 필터·선택 가능 여부와 서버 요청 ID는 그대로 사용한다. 서버가 보내지 않은 후보를 만들거나 장군의 원수 임명 자격을 부여하지 않는다. `ScreenCommanderRosterTest`는 합성 입력과 서버 `kor-appointment-army-mixed`·`kor-appointment-army-group-mixed` 후보를 GUI2 두 해상도로 검사한다. 서버 후보의 계급·기술 순서, 검색한 실제 장군의 진급 비용·툴팁과 단일 선택 요청을 확인한다. 원본 fixture의 빈 토큰에는 기존 방식대로 테스트용 issued 세션을 붙이며 실제 서버의 진급·비용 집행이나 소켓 연결 검증을 뜻하지 않는다.

`FabricCommanderHudDetailsTest.traitSkillsInactiveMarshalTraitsAndMixedAppointmentsReachThePanels`가 생성하는 `kor-command-panel-army-trait-skills`·`kor-command-panel-army-probing-active`와 위 임명 입력 두 개는 `armyHud`의 자동 생성·출처 검사 대상이다. `MapCommanderSourceStateTest`는 이 실제 응답으로 군 지휘 원수의 비활성 특성3개·붉은 픽셀과 경고 원문, 집단군의 기본3개+원수 특성 스킬만 표시, 군 `mil_1`의 먼저 획득한 탐색 공격과 군 `mil_3`의 먼저 획득한 글라이더가 각각 넷째 칸에 표시되는지 확인한다. 글라이더 회색 픽셀, 탐색 공격 취소 툴팁 원문과 색·빈 줄, 카드 기술 블록 앞 빈 줄 하나도 검사한다. 활성 스킬도 첫 4개 밖이면 숨기며, 미지원 두 스킬의 회색 픽셀은 합성 입력에서 각각 네 번째 칸에 배치해 추가로 검사한다. 활성 특성 그림 자체에 있는 붉은 픽셀은 비활성 판정 근거로 쓰지 않는다. 취소 제목의 표시 문장은 중간 색 코드를 제외해 확인하되 렌더러가 받는 원문은 그대로 비교한다. 실제 서버 테스트가 작성한 상태·특성 조합이며 운영 게임에서 플레이한 상태를 수집한 자료와 구분한다.

전체 검증의 수동 입력을 격리해서 복사할 때는 `HOI_LAND_PLAN_PREVIEW`가 가리키는 `land-plan-overlay.json`과 함께 `land-plan-preview.json`도 같은 폴더에 보존한다. `MapSurfaceRenderChecks`가 저장된 계획과 편집 중인 계획을 이 두 파일에서 각각 읽는다. 선택 입력인 `HOI_FOCUS_PREVIEW`·`HOI_PRODUCTION_PREVIEW`·`HOI_CONSTRUCTION_PALETTE_PREVIEW`도 전달한 파일의 출처·해시를 보존한다. `HOI_AGENCY_PREVIEW`·`HOI_POLITICS_PREVIEW`를 생략하면 그 별도 서버 원문 캡처는 실행하지 않으므로 기본/합성 검사의 성공과 구분한다.

서버의 실제 건설 블록 상태 입력으로 색을 검사할 때는 `src/gametest/resources/atlas-client-scene.json`도 같은 검증 서버가 내보낸 자료로 맞춘다. `loadPack`이 이 자료의 `blockstates`·`generatedAssets`를 테스트 전용 carrier ZIP으로 만든다. Polymer의 note block 배정은 등록 순서에 따라 바뀔 수 있으므로 지리 `sourceHash`가 같아도 블록·그림 연결까지 같다는 뜻은 아니다. 예전 자료의 `snare/note=23/powered=false`·`snare/note=24/powered=true`가 `supply/0`·`supply/1`을 가리키던 것과 달리 최신 출력은 건설 예약용 `construction/101`·`construction/102`다. 자료 전체를 성공한 서버 run의 출력과 일치시키고 원본/수정 SHA256·블록 연결·새 자산을 검증 근거에 보존한다. 이 자료는 GameTest 전용이며 운영 클라이언트 JAR의 게임 자산으로 넣지 않는다.

`HOI_PRODUCTION_PREVIEW`로 받은 최신 생산 응답은 빈 생산 목록도 허용한다. 일반 산업 화면과 연구된 후속 장비 검사는 그 응답을 사용한다. `ScreenProductionPresentationTest`의 합성 재고색·라인 우선순위·장비 그림 배경 입력 검사는 생산 라인이 있는 기존 bundled fixture를 명시적으로 사용한다. 해당 검사를 위해 실제 서버 응답에 임의 라인을 추가하거나 빈 응답을 생산 중인 상태라고 표시하지 않는다. 이 분리는 `ScreenIndustryChecks.bundledFixture`를 기존 리소스 읽기 경로에서 추출해 처리하며 서버의 생산·저장 동작을 바꾸지 않는다.

재현은 리소스팩 빌드 뒤 Java25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout`으로 집중 검사를 하고, 같은 소스에 `test runGameTest remapJar`를 필터 없이 실행한다. 고정 서버 사본은 `-PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42-server/HOI`와 `-PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp42-server/HOI/protocol/src/main/java`를 함께 지정한다. 드래그·57개 목록의 기존/합성 입력은 `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`로 지정하며, 최신 특성·스킬·임명 응답과 구분한다. 전체 검사의 수동 입력은 위 복사 규칙과 [README의 환경 변수](../README.md#사용과-빌드)를 따른다. 실행별 결과·실패/중단 로그·원본 비교는 CMP-42 검증 자료에 보존한다. 실제 렌더 검사는 운영 접속·다중 사용자·저장/재접속·직접 청취를 검증하지 않으며 이 항목들은 미수행이다.

단일 군·집단군 선택과 드래그로 군 둘 이상을 선택한 압축 패널 모두 각 패널 뒤에 `unitview.gui/army_color_button`의 `GFX_group_marker3`(원본 44×88px)를 놓아 오른쪽 색 띠를 표시한다. 공통 `armyHeader`가 기존 팩의 원본 픽셀과 드래그 패널의 좌표·크기를 재사용한다. 단일 선택의 근거는 `20261017214413_1.jpg`·`20261017214419_1.jpg`, 드래그의 근거는 `20261017221918_1.jpg`다. 군은 서버 `armyColor`를 사용하고 없는 색은 만들어 넣지 않는다. 집단군 금색 tint `#806000`은 원본 화면의 관측색에 맞춘 표시값이며 원본 엔진 상수로 확인된 값은 아니다. 패널의 기존 폭·머리줄 간격과 해군 패널은 유지한다.

작은 사단 목록 스크롤바는 `unitview.gui/army_division_list_view/slider`가 참조하는 `GFX_scrollbar_vertical_bg`를 화살표 뒤에 그린다. 원본 `general_stuff.gfx`의 18×156px와 세로 경계 52px를 보존하여 위·아래 네모 틀을 함께 표시하고, 가운데만 늘린다. `scroll_up`·`scroll_down`·`scroll_drager`는 기존 16×16px를 유지한다. 이 자산은 기존 팩에 포함되어 추가 가져오기가 필요 없다.

스킬 그림은 `affordable=false` 또는 `enabled=false && affordable=null`일 때 회색으로 표시한다. 비용을 낼 수 있지만 재사용 대기 중인 `affordable=true`는 이 조건으로 회색 처리하지 않는다. 서버의 `AbilityDetail.tooltip`이 있으면 색·빈 줄·취소 제목을 포함한 원문을 그대로 쓰며, 없는 구형 응답만 기존 원본 문구와 사용 가능 응답으로 보완한다. 글라이더·상륙 계획 가속의 회색 표시는 미구현 서버 효과의 구현을 뜻하지 않는다. `MapCommanderAbilityTest`는 여섯 상태 조합을 검사하고 `MapCommanderAbilitiesRenderTest`는 합성 비활성 입력의 실제 회색 픽셀과 서버 툴팁 보존을 검사한다. 합성 렌더 입력은 실제 소켓·전투·저장/재접속·멀티플레이 검증과 구분한다.

현재 실제 탐색 공격 응답의 툴팁에는 `dont_lose_dig_in_on_attack` 미번역 키가 남아 있다. 클라이언트는 이를 받은 원문대로 표시하며, 원본 한국어 패치와의 번역 일치를 확인한 항목으로 취급하지 않는다. 수치·효과 구현 여부와 표시 이름의 번역 누락은 구분한다.

현재 선택에 대한 별도 서버 사용 가능 응답이 새 거절 사유를 보내면 원문 아래에 추가한다. 원문에 이미 있는 사유는 반복하지 않으며, 세션·revision·선택 범위가 다른 응답으로 입력을 다시 허용하지 않는다. 원본 제목의 색이나 취소 표기가 바뀌어도 입력 검사는 표시 제목 대신 스킬 ID로 단추를 찾는다.

좌측 군·집단군 머리줄과 드래그 패널의 훈장은 서버 `LeaderTrait.active=false`일 때만 원본 그림에 붉은 tint를 곱한다. `true`와 구형 응답의 `null`은 원래 색이다. 클라이언트는 특성 유형이나 지휘관 계급으로 활성 여부를 재계산하지 않으며, 비활성 안내 `TRAIT_NOT_ACTIVE_WHEN_COMMANDING_AN_ARMY`는 서버 툴팁을 그대로 표시한다. 원본 `unitview.gui/traits_grid`와 `unitleaderwindow.gui/unit_leader_trait_entry`는 기존 특성 스프라이트를 사용하고, `unitleaderwindow.gfx`의 방어적 교리·완고한 방어가 정의에 별도 붉은 그림은 없다. 원본 `buttonstate.shader`의 색 곱하기 경로와 `20261017214437_1.jpg`를 근거로 기존 `drawTinted`를 사용한다. tint `#bf5050`은 사진에서 대조한 표시값이며 원본 C++ 엔진 상수로 확인된 값은 아니다. `MapCommanderTraitRenderTest`는 기존 입력에 합성한 비활성 상태·경고·드래그를, `MapCommanderSourceStateTest`는 새 서버 응답의 군 지휘 원수와 집단군을 각각 검사한다. 원본 사진의 붉은 훈장2개와 새 fixture의3개는 지휘관·특성 조합이 다르므로 같은 상태라고 주장하지 않는다.

드래그 복수 선택·카드 선택에 따른 명령 표시·작은 스크롤바·스킬 연결 배치·지휘력 부족 회색 그림의 집중 검증은 Java25에서 5m51s에 통과했다. GUI2 2560×1440·1280×720의 실제 PNG392장과 클라이언트 단위12개·protocol120개·격리 입력 생산자49개의 실패·오류·건너뜀0을 확인했다. 기존 리소스팩의 관련 그림13개는 유효 원본 DDS의 픽셀과 일치하며 팩을 다시 만들지 않았다. 이 결과는 해당 집중 검증에 한정한다. 필터 없는 전체 검증은 서버 담당자의 시작 승인을 기다리며, 실제 운영 접속·멀티플레이·청취와 최신 서버의 지휘력 비용·특성 스킬 판정은 별도 검증이다.

지도 드래그가 둘 이상의 군 사단을 고르면 군 카드 순서대로 지휘관 패널의 윗부분만 쌓는다. 모든 군이 같은 집단군에 속할 때만 그 집단군을 맨 위에 붙인다. 미지정 사단은 이 스택에 패널을 만들지 않고 공통 집단군 판정에서도 제외한다. 따라서 같은 집단군의 군 둘 이상과 미지정 사단을 섞어도 집단군 패널을 유지한다. 군 하나의 드래그는 기존 전체 군 패널을 유지한다. 카드 클릭과 직접 사단 선택은 이 스택으로 바꾸지 않는다. 패널 X는 해당 군의 선택 사단만, 집단군 X는 해당 집단군 소속 군의 선택 사단을 모두 뺀다. 원본 `20261017221918_1.jpg`의 패널은 보이는 테두리가 이어진다. 기존 55GUI 머리줄의 투명 여백을 겹쳐 시작점을 52GUI씩 이동하며 배치·사단 도구줄·사단 목록은 그리지 않는다.

전투 계획 막대와 스킬 칸은 하단 군·집단군 카드를 선택한 동안만 표시한다. 지도 드래그·직접 사단 선택·미지정 사단·빈 선택에서는 그리기와 클릭 영역을 없애고 계획 확인 단축키도 차단한다. 스킬 칸은 명령 막대의 위치를 유지한 채 바로 위로 붙이며, 정지 화면에서 막대가 오른쪽으로 이동할 때 같은 좌표 기준을 따른다. 스킬 그림은 위의 비용 부족·미지원 상태 조건에 따라 기존 무채색 렌더러로 표시한다. 툴팁과 서버 요청·거절 처리는 유지한다.

사단 목록은 원본 `GFX_scroll_track` 12×12(모서리 4px), `GFX_scroll_drager` 16×16, `GFX_scroll_up`·`GFX_scroll_down` 16×16을 사용한다. GUI2에서 폭과 손잡이·화살표 크기는 원본 픽셀을 유지하고 트랙 중앙만 목록 높이에 맞춰 늘린다. 손잡이는 표시 비율로 확대하지 않으며 맨 위에서 ▲ 아래에 붙는다. 군·집단군·미지정 목록이 같은 렌더러와 휠·트랙 클릭·손잡이 끌기를 사용한다. 아홉 스킬 `force_attack`, `last_stand`, `staff_office_plan`, `extra_suplies`, `siege_artillery`, `faster_naval_invasion_planning`, `probing_attack`, `glider_planes`, `makeshift_bridges`의 원본 GFX 그림은 기존 팩에 있으며 서버 목록 순서대로 채운다.

집중 재현은 Java25에서 `:client:test --tests dev.hoi.client.map.MapCommanderPanelOrderTest --tests dev.hoi.client.map.MapCommanderGroupsTest --tests dev.hoi.client.map.MapCommanderCardDragTest :protocol:test`와 `runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp39/r12-protocol/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp39/r12-protocol/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`이다. `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`를 지정한다. r12의 독립 검증 사본에 선택적 affordable 필드가 추가된 공유 파일만 동기화하며, 비용·특성 스킬의 최신 서버 구현 검증을 뜻하지 않는다. 드래그 입력은 실제 화면을 거치되 사단 상세 응답·지휘력 부족은 합성 입력이다. 운영 접속·멀티플레이와 필터 없는 전체 검증은 별도다.

육군 마지막 배치 보정은 팩 `python scripts/build_pack.py` 후 Java25·r12 고정 서버 사본으로 필터 없는 `test runGameTest remapJar`를 27m 16s에 통과했다. 실제 PNG 2222장, 클라이언트 JUnit 122개·protocol 119개·자동 입력 생산자 46개에서 실패·오류·건너뜀0이다. 클라이언트 단위 검사·생산자는 재실행했고 protocol 결과는 재사용했다. 집중 검사는 5m 38s·PNG359장에 통과했으며, 그 이후 새 전구 버튼의 마지막 높이·세로 위치 보정은 이번 전체 실행으로 검증했다. 전투 수 0 숨김/양수 배치, 올리브 전구 배정 줄, 하위 군 줄 높이, 집단군 소속 군을 선택했을 때의 새 전구 버튼, 수송 캡슐 폭과 특성 오른쪽 여백을 검사한다. 두 해상도 GUI2 원본 비교는 CMP-34의 첨부와 `army-stage-review` 문서로 제공한다. 원본 전체 픽셀 일치 승인과 실제 소켓·저장/재접속·멀티·직접 청취는 별도이며, 해군·공군·핵·전쟁 목표 화면은 이 육군 단계의 완료 범위가 아니다.

최신 사용자 보정은 집단군 머리줄의 군 수/한도 글자(예: 3/5)만 2GUI 위로, 군·집단군 특성 훈장과 호버 영역을 직전 위치에서 추가2GUI 위로 옮기고, ▼9개를 각각의 열 가운데 배치한다. 칸 경계·아이콘 크기·HQ 위치와 훈장의 서버 순서/겹침 규칙은 유지한다. 아래 26m51s 전체 통과 기록은 이 추가 보정 전 결과다. 이 추가 보정의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 5m 45s, 사용자 승인 슬롯의 필터 없는 전체 `test runGameTest remapJar`는 27m 32s에 통과했다. 전체 실제 PNG2222장을 생성했다. 첫 집중8m12s는 숫자 이동 전 컴파일본으로 별도 보존한다. 기존 합성12특성의 최상단 툴팁·순서/크기와 선택·스크롤·좌우/반복 입력 회귀를 유지한다. 실제 소켓·저장/재접속·멀티·직접 청취와 공식 가 시각 리뷰는 별도다.

직전 HQ 틀 +2GUI, 특성 −1GUI, 군·집단군 공통 폭과 목록 경계·스크롤바·연속 배경, 부대 상자 −1GUI 및 지역 사령부 원본 bus 그림을 포함한 필터 없는 전체 `test runGameTest remapJar`가 Java25·r9 서버 사본으로 26m 51s에 통과했다. GUI2 두 해상도를 포함한 전체 실제 PNG2222장을 생성했다. 직전 육군 집중 검사는 5m 38s 통과했으며 변경 없는 단위/생산자 검사 결과는 재사용했다. 이번 수정 묶음의 첫 전체 실행은 창 크기 변경 검사의 옛 축소 배율 가정에서 실패했다. 표시 코드를 바꾸지 않고 검사 여백을 최소 배율1에 맞춘 뒤 재검증했으며 실패·중단 기록도 보존했다. HQ 버튼의 배경·테두리·호버만 이동하고 아이콘·텍스트는 유지한다. 운영 접속·저장/재접속·멀티·직접 청취와 실제 전투 전구 툴팁은 이 렌더 검증에 포함하지 않는다. 원작 기본 색 배정·최종 shader·자동 편제 그림 규칙의 기존 미확정 사항도 남아 있으며 공식 가 시각 리뷰는 별도다.

카드와 전구 줄은 공통 선택 안내·이름·병과 수·설명 뒤에 서버 `Card.tooltip`을 그대로 붙인다. 기술/경험, 지휘관 효과, 원수 부분 혜택의 색·아이콘·순서·정밀도를 다시 계산하지 않는다. 단독 `---` 구분선은 기존 원문 렌더러에서 빈 줄로 바꾸며 초상 약력 `Card.detail`과 분리한다. 상단 장군·원수 특성은 서버가 배경 특성을 뺀 `traitDetails`를 그대로 그린다. 기술 상한9와 능력치 상한10, 9레벨의 가득 찬 경험 막대는 서버 표시값을 따른다. 프로토콜 확정 SHA-256은 `bc387936630d3dc0a6f310e8fd50d1f50b64fea1ee8c54bdb5f262ebf2a67d6a`다. 최종 계약의 r8 격리 서버 사본으로 육군 집중 검사가 8m 15s에 통과했다. GUI2 두 해상도 PNG351장, 클라이언트122·protocol119·자동 입력 생산자49·별도 표시/기술 상한 검사5개에서 실패·오류·건너뜀0을 확인했다. 변경 없는 단위 테스트 성공 결과는 재사용했다. 첫 집중 검사 통과 후 추가 장면 실행은 입력 생성 중 실행 중단되어 새 로그로 재개했다. 이 집중 검사 이후의 전체 검증 결과는 이 절 첫 문단을 따른다. 실제 소켓·저장/재접속·멀티·직접 청취와 실제 전투가 있는 전구 툴팁은 미검증이다.

최신 가 계약의 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`가 5m 16s에 통과했다. GUI2 실제 PNG323장, 클라이언트 JUnit122개·protocol119개·격리 생산자47개·추가 표시 생산자1개에서 최종 실패·오류·건너뜀0을 확인했다. 변경 없는 성공 결과는 재사용했다. 집단군 클릭/Shift 직접 해산, 우클릭·반복 차단, 전구 우선권 세 요청과 NORMAL/LOW/HIGH/MIXED 응답 표시, 선택하지 않은 카드의 계획·지역 방어·훈련, 병과 글리프와 두 해상도 툴팁 경계를 검사했다. 처음 두 실행의 시험 조건/화면 재생성 대기 실패와 글리프 리소스 크기 수정 기록은 보존했다. 최종 실행은 정상 종료했다. 전체 클라이언트 검증·실제 소켓·저장/재접속·멀티·직접 청취는 이 결과에 포함하지 않는다. 표시용 View는 실제 시험 차원과 running=true만 치환하며 서버 원본 입력을 보존한다. r7 생산자 사본의 복사 시점1439파일은 전후 해시 변경0건으로 고정했다.

군·집단군의 사단 목록은 `unitview.gui/army_division_list_view`의 `GFX_tiled_window_1b_thin_border`를 도구줄부터 목록·스크롤바 끝까지 이어 그린다. 테두리와 중앙 영역은 원본 크기로 반복해 늘어난 폭의 우측 상단에 회색 장식이 늘어나지 않게 한다. 군·집단군 폭은 현재 카드들의 병과 요약에 필요한 최대 폭을 공유하며 추가 폭은 이름 열에만 준다. 목록 배경은 첫 ▼ 왼쪽−3px부터 마지막 ▼ 오른쪽+20px까지이며 오른쪽에는 스크롤바 자리만 남긴다(GUI2). 카드·HQ 행·목록은 배경 사이로 지도가 보이지 않게 이어진다. 선택 수 뒤에는 원본 `GFX_unit_list_header`를 사용한다. 정렬 칸은 x=0,19,38,95,121,141,171,333,357,388 경계에서 반올림한 끝 좌표를 공유해 틈을 없애며, 화살표는 최신 사용자 지시에 따라 각 칸의 좌우 중앙에 둔다. 첫 조직력·내구도 칸은 `division_list_column_button`의 점 두 개가 있는 frame_2다. 부대 그림 상자만 기존 위치보다 1GUI 왼쪽에 둔다.

지역 사령부의 `unit_hud/headquarters`는 TFR `gfx/interface/counters/divisions_large/unit_bus_icon.png`의 첫 76×42 프레임을 사용한다(`GFX_unit_bus_icon_medium`, 152×42 원본의 2프레임 중 frame_0). 목록의 큰 그림과 카드 병과 요약 모두 `commander/commands/unit_bus_icon_medium/frame_0`에 연결한다. 원본 SHA-256은 `a66bddf2a6317c1dbc9c50f4d67c18f86c117fc4feffef73db13ae8b1e654e00`이며 팩 provenance에 입력·출력 해시를 보존한다. 이 일반 사단의 그림과 군 전용 HQ 배치 행은 별개다.

전구 첫 줄에는 집단군과 독립 군을, 집단군의 펼친 줄에는 월계수·군 수와 소속 군을 표시한다. 집단군 전체 선택은 원본 `theatre_dropdown_bottom` frame_1의 초록 바탕과 소속 군의 선택 휘장을 사용하고, 개별 군 선택은 그 군만 강조한다. 군 목록 모드에도 집단군 머리줄을 유지해 같은 전환 단추로 사단 목록에 돌아온다. 집단군에는 HQ 행을 표시하지 않는다. 현재 서버에는 별도 HQ 부대가 없으며 `지역 사령부`·`Regional Command` 같은 보통 사단을 이름이나 대대로 제외하지 않는다. 초상·특성·HQ·편제·머리줄 도구 단추의 원문 툴팁은 색과 단축키 안내, 빈 줄을 보존한다.

`MapArmySourceReviewTest`는 `HOI_ARMY_REVIEW_PREVIEW`의 현재 서버 출력으로 두 해상도 GUI2의 지휘관/HQ·세 차량화 단계·편제별 툴팁·집단군 군/사단 목록·전구 선택을 촬영한다. 이 입력은 기존 합성 경계 회귀와 구분한다. 생산자 [FabricArmyReviewTest](../scripts/fixtures/FabricArmyReviewTest.java)는 실제 한국 OOB의 사단들을 시험용 세 군으로 나누고 기존 서버 배정 API로 지휘관·원수를 배치한다. 이름·편제·능력치·특성·HQ·문구·휘장은 서버 표시 로직에서 생성하며 원본 사진의 날짜·숫자로 바꾸지 않는다. 시험 편성은 실제 캠페인 세이브가 아니다.

추가 `settled` 입력은 시험 상태를 메모리에서 복제하고 지휘관 이동 대기만 해제하여 장군 효과와 원수 부분 혜택 전체를 검사한다. `skill-cap`은 그 복제 상태의 한국 지휘관 기술을 9, 경험치를 0으로 지정한 경계 입력이다. 네 능력치는 원본값을 유지한다. 두 입력도 서버를 통해 표시를 생성하며 실제 저장 파일을 수정하지 않는다. 렌더 검사는 카드/전구 본문 일치, 구분선의 빈 줄 변환, 특성 목록 일치, 기술 9(최대)와 경험 막대 1.0, 두 해상도 화면 경계를 검사한다.

재현할 때 현재 HOI의 소스·콘텐츠를 별도 폴더로 고정하고 위 생산자를 그 사본의 `fabric/src/test/java/dev/hoi/fabric/`에 복사한다. 원래 서버 작업 트리는 변경하지 않는다. Java25에서 사본의 `.\gradlew.bat :fabric:test --tests dev.hoi.fabric.FabricArmyReviewTest -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 실행하면 `fabric/build/army-review-fixtures`에 아홉 입력이 생긴다. 세 추가 입력은 실제 서버 계획 API로 만든 실행 대기·지역 방어·훈련 상태다. 팩 빌드 후 클라이언트에서 `HOI_ARMY_REVIEW_PREVIEW`를 그 폴더로 지정하고 `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=<고정 사본> -PhoiProtocolSource=<고정 사본>/protocol/src/main/java --console=plain --no-daemon`을 실행한다. 기본 생산자·프로토콜 출처 검증은 그대로 수행한다. 이 환경 변수가 없는 일반 실행에서는 추가 원본 비교 장면을 촬영하지 않으므로 해당 검증을 완료로 보고하지 않는다.

집단군 머리줄의 문 단추는 서버 `GROUP_DISSOLVE`의 활성 상태·`UNASSIGN_ARMY_GROUP` 원문을 사용한다. 클릭은 `FORMATION/DISSOLVE_GROUP`, Shift+클릭은 `FORMATION/DISSOLVE_GROUP_ALL`, 선택은 집단군 ID 하나이며 확인창을 열지 않는다. 우클릭은 무동작이고 반복 요청은 기존 revision 제한을 따른다. 해산 결과는 서버 응답 이후에만 반영한다. HQ 배치 실행과 전투 계획 상세 연결은 각각 사·자 단계의 별도 작업이며, CMP-33은 육군 가 단계까지만 맡는다.

현재 대조 기준은 사용자 지정 2026-10-17 Steam 원본 92장과 2026-10-18 032953~033219·033348~033353, 별도 첨부 033538이다. 이전 10-02·10-04 육군 원본과 10-14 중점/결정 원본은 현재 대조 기준에서 제외한다. 육군 보완 뒤 전투 계획(명령 막대·스킬·지도), 중점 상세, 사건과 결정, 육군 사령부·장교단, 알림, 해군, 공군, 핵·전쟁 목표 순서로 검토한다. 국가·날짜에 따른 이름·숫자는 복제하지 않으며 스코어보드 날짜 줄·오른쪽 아래 1~9 지도 모드·진행 중에만 바닐라 HUD 숨김은 사용자 지정대로 유지한다.

아래 카드 줄은 집단군을 왼쪽에, 소속 군을 오른쪽에 배치한다. 집단군을 선택하면 소속 군의 실제 사단을 선택하며 원수와 장군의 역할을 구분한다. 선택 카드는 원본 `leader_group_frame_selected`와 `leader_army_group_frame_selected` 금색 테두리를 사용하고 실행 중지·실행 버튼은 선택과 무관하게 그 카드 자신의 `ACTIVATE`/`DEACTIVATE`가 있을 때만 표시한다. 집단군에 휘하 군 컨트롤을 합치지 않는다. `DEFENDING_AREA`는 클릭 없는 방패만 표시하고 `STOP_EXERCISE`는 훈련 중지·훈련 표시를 사용한다. 계획 평가도 실행 버튼이 있는 카드에만 겹친다. 집단군 카드는 지휘관 유무와 관계없이 군 수/상한을 표시한다. 군은 지휘관이 있을 때만 사단 수/상한을 표시하며 상한 초과는 빨강이다. 지휘관 없는 군은 사단 수만 표시한다. 카드 아래 계획 막대는 `Panel.planPower`만 사용하고 알 수 없는 진행률은 채우지 않는다. 집단군과 소속 군은 원본 `LG_army_group_bg`·`LG_army_mid_bg`·`LG_army_end_bg`로 이어지는 틀을 사용한다. 175×151 2프레임 집단군 atlas는 절반 UV를 사용한다. 현재 금색 tint `0xffb29854`는 원본 화면에 맞춘 표시값이며 엔진의 최종 shader 계산과 동일하다고 확정한 값은 아니다. 군 카드 양옆의 `group_marker2`만 서버 `armyColor`로 칠한다. 집단군 카드는 원본 214447처럼 금색 걸쇠만 유지하며 색 띠를 그리지 않는다. 펼친 집단군의 접기 삼각형은 왼쪽을 가리키는 frame_1이다. 색상 선택은 선택한 집단군 카드의 왼쪽 금색 손잡이에서 열고 서버 응답 뒤에만 갱신한다. 비선택 카드에도 원본 `execute_stop_small_bg` 빈 탭 둘을 표시한다. 새 군의 + 카드는 닫기 그림이 없는 `army_border`를 사용한다.

군 카드의 X는 TFR `leadergroups.gui`의 프레임 (-1,-12)과 버튼 (62,-7)의 차이를 사용해 GUI2 프레임 기준 (31.5,2.5), 8×8로 놓는다. 집단군 접기는 프레임 (-1,-21)과 버튼 (73,-15)의 차이인 (37,3), 7×10이다. 사용자 지시에 따라 집단군 X와 군·사단 접기는 표시하지 않는다. 원본 `unitview.gfx`의 `group_marker4.dds`(85×29)와 `group_marker2.dds`(73×23)는 출처와 함께 보존한다. 최신 원본 214447에 따라 군의 `group_marker2`만 카드 색 띠로 사용한다. 이 표식은 전역 목록의 `theatre_army_group_shield`와 별개다.

펼침 패널은 TFR `interface/unitview.gui`의 `army_badge_view` 437×109와 `army_division_view` 390×33을 GUI2 기준 절반 크기로 배치한다. 사단 배지 (15,0)·숫자 (25,9), 제목 (66,10), 지휘관 계급·이름 (70,38)/(70,54), 원본 특성 시작 (302,48; 현재는 아래 능력치 배경 경계 지시에 맞춰 이동), 군 수 배지 (311,3/6)를 원본 기준으로 사용한다. 제목과 사단 이름은 왼쪽 정렬하며 긴 이름은 같은 글자 크기를 유지해 말줄임한다. 지휘관이 없으면 원본 `UNIT_LEADER_NO_LEADER` 문구와 `CLICK_TO_ASSIGN` 안내를 각각 초상 오른쪽과 초상 프레임 안에 기본 글자 크기로 표시한다. `leaderEnRouteDays`는 원본 `UNITLEADER_IS_EN_ROUTE`/`UNITLEADER_IS_EN_ROUTE_SINGULAR`의 이름·날수에만 대입하여 초상 툴팁에 표시한다. 서버가 제공하지 않는 능력치·진행률은 임의로 채우지 않는다.

명령 막대의 공수 강하는 원본 `UNITS_VIEW_BTN_DRAW_PARADROP` 아이콘과 설명을 사용한다. `battleplantools.gui`의 원본 버튼 묶음·공백 순서를 유지하며 부유항 상륙 버튼의 71×32 두 프레임 그림은 atlas의 절반 UV로 표시한다. 원본에 없는 막대 오른쪽 위 X는 표시하지 않는다. 전선 제목은 `Panel.commandCaption`만 사용하며 서버가 보낸 “전투 계획”을 표시하고 빈 문자열은 빈칸으로 남긴다. 군 이름을 원본 전선 이름으로 간주하거나 사단 병력 평균을 계획 막대로 대체하지 않는다.

확정된 `ProtocolCommanderHud` 계약의 `Panel.experience`는 지휘관 경험 막대, `divisionTypes`는 전체 병과 수, `manpower`는 전체 인력, `planPower`는 계획 막대에 연결한다. 사단 줄의 `MemberData.experienceLevel`은 원본 `unit_level` 0..4 프레임, `activity`는 대기·이동·전투·후퇴·훈련·수송에 해당하는 원본 상태 그림을 고른다. 오른쪽 원통 그림은 원본 `fuel_strip`이다. 마지막 그림은 상태 ! 대신 원본 `btn_army`의 `theatre_army_shield`를 사용한다. `MemberData.fuel`의 실제 연료 비율은 원통 그림 호버에 표시하며 null은 ?로 남긴다. 기본 게임 한국어 `FUEL`(core_l_korean.yml:1792)의 명칭을 사용한다. 비율의 소수 한 자리 표시는 HOI 표시 규칙이고 원본 연료 경고색 경계는 미확정이므로 기본 frame_0을 유지한다. `MemberData.insignia`가 있으면 소속 군 방패의 0..80 프레임으로 사용하고 null이면 원본 기본 프레임을 유지한다. 사단의 `attachedArmy`와 일치하는 군의 서버 `ARMY_INSIGNIA` 컨트롤로 `Open(ARMY_TOOLBAR,"ARMY_INSIGNIA",[군 id])`를 요청한다. 오른쪽 클릭은 요청하지 않으며 휘장은 서버 응답을 받은 뒤에만 바뀐다. 81개 선택지는 기존 지휘관 선택 목록으로 열리며 원작 선택창과의 시각 대조는 별도다. 서버에 없는 사단 훈장·편제 고유 그림은 만들어 넣지 않으며 `Member.icon`은 서버가 고른 병과 그림이다. 재배치·통폐합·수송 중량 칸·절반 선택을 분리하고 통폐합은 실행 계약이 없어 회색으로 표시한다. 원본 통폐합 문구는 설치된 한글패치에 해당 키가 없어 기본 게임 한국어 `CONSOLIDATE`·`CONSOLIDATE_TOOLTIP`을 사용한다.

이름은 Minecraft 기본 글자 크기로 왼쪽 정렬하고 긴 이름만 말줄임한다. 선택 문구는 숫자만 초록색인 한 줄이며 원본처럼 오른쪽 전략적 재배치 버튼 경계에서 가린다. 초상 위 `CLICK_TO_ASSIGN`은 33×40 GUI 프레임 안에서 중앙 줄바꿈하며 크기를 줄이지 않는다. 집단군에 속한 군은 머리줄에 월계수와 해제 버튼을 표시한다. 부모 이름이 숫자로 시작할 때 그 숫자를 표시하고 임의 번호를 만들지 않는다. 해제는 `FORMATION/DETACH_ARMIES`, 선택 `[군 id]`, 빈 rays로 보낸다.

군 지휘관 아래 HQ 행은 `unitview.gui`의 `hq_window/empty_slot/deploy_dropdown`과 원본 `army_hq_bg`·`deploy_hq_button`을 사용한다. `DEPLOY_HQ_BUTTON_LABEL`·`DEPLOY_HQ_BUTTON_NO_COMMANDER`·`DEPLOY_HQ_BUTTON_COST`는 기본 한국어 core_l_korean.yml이며 설치 한글패치에 같은 키가 없다. 서버 `Panel.headquarters`의 state·label·text·cost·affordable·tooltip을 표시한다. `NO_COMMANDER`는 회색 틀과 어두운 회색 바탕에 굵은 배치/미배정 문구, `READY`는 금색 틀·철모·편제명·지휘력 비용을 사용한다. 최신 사용자 지정에 따라 배치 버튼의 배경·테두리와 호버 영역만 2GUI 아래로 옮긴다. 아이콘·텍스트와 바깥 HQ 행은 이동하지 않는다. 부족 비용은 빨강이며 서버 원문 툴팁을 보존한다. 배치 실행 컨트롤은 후속 계약 대기다. 레거시 null과 지휘관 없는 경우만 기존 commander 값으로 미배정 문구를 표시하며, 지휘관 보유 null은 자료 대기를 알린다. 비용 7이나 사령부 이름을 임의로 채우지 않는다.

최신 사용자 정렬 지시에 따라 사단 행의 조직력은 행 기준 x+1.5GUI, 내구도는 x+5.5GUI에 놓고 좁은 첫 ▼칸 하나를 함께 사용한다. 경고 열을 별도로 복원해 정렬 칸은 9개다. 원본 TFR unitview.gui의 column_heads 시작점 0/19/38/95/121/141/171/333/357px와 폭을 사용하며, 행 테두리 바로 오른쪽에 첫 칸을 맞추고 추가 패널 폭은 이름 열에만 배분한다. 경고 아이콘과 호버 영역은 x+12GUI에 놓는다. 숙련 휘장·진행 막대·명령 그림·이름·보급 통·군 표시는 대응 열에 맞추며 기존 그림 크기를 유지한다. 군·집단군·미지정 사단이 같은 행 렌더러를 사용한다. HQ 철모는 배치 라벨 끝과 편제명 시작 사이의 중심에 기존 38×21GUI 크기로 놓으며 위쪽 -3GUI 오프셋을 제거한다. 배경 특성은 서버가 상단 HUD용 traitDetails에서 제외한다. 클라이언트는 LeaderTrait의 icon/name/tooltip으로 종류를 추정하지 않고 받은 목록을 그대로 그린다. 지휘관 배경은 원본 그림의 오른쪽 밝은 영역만 늘리고, 어두운 능력치 칸은 레벨 상자부터 보급 아이콘 끝+3GUI까지만 유지한다. 특성 훈장은 그 오른쪽 밝은 바탕에서 서버 순서(장군→원수→기타)를 유지한다. 외교 국기 줄의 UiFlagStack.left/visibleWidth를 재사용해 넘치면 간격만 줄이며, 나중에 그려진 오른쪽 훈장의 툴팁이 겹침 영역을 소유한다. 크기를 줄이거나 끝 특성을 생략하지 않는다. 합성 특성12개 장면은 실제 지휘관 보유 상태와 구분한다.

최신 9열 정렬·능력치 배경·특성 겹침은 Java25 육군 집중 `test runGameTest remapJar -PhoiTestSuite=army-layout`에서 6m 3s에 통과했다. 실제 PNG359장, 클라이언트 JUnit122·protocol119·자동 생산자49·별도 표시 생산자11개가 실패·오류·건너뜀0이다. 변경 없는 성공 결과는 재사용했다. 이전 진행 막대 좌표를 검사하던 첫 실패를 새 열 위치로 수정했고 두 로그를 보존했다. 합성 특성12개에서 크기 유지·서버 순서·겹친 최상단 툴팁을 두 해상도로 검사했다. 최종 필터 없는 전체 검증은 별도 슬롯 회신 후 수행한다.

미지정 사단만 선택했을 때는 군 머리줄·지휘관·HQ·전투 계획 막대를 생략하고 사단 수 도구줄, 정렬 칸·목록, `UNASSIGNED_UNIT_DESCRIPTION` 안내를 표시한다. 목록은 2560에서 최대 11행이며 작은 화면에서는 안내 문구와 카드 줄이 겹치지 않는 범위로 줄인다. 도구줄부터 안내 상자까지 연속된 원본 어두운 배경을 사용한다. 도구줄 X는 전체 선택, 행 끝 X는 해당 사단 선택만 해제한다. 수송 중량은 선택 사단의 서버 값을 합하며 누락 시 사용자 지정대로 0이다. 편제 변경은 선택 사단 중 하나라도 서버 `templateBlock` 사유가 있으면 회색으로 표시하고 첫 사유를 그대로 보여 준다. 빨간 휴지통은 선택된 사단의 `unit/` ID만 `ARMY_TOOLBAR/DIVISIONS_DISBAND`로 보내 서버 확인창을 연다. 사단이 없으면 회색이며 요청하지 않는다. 군이 전혀 없는 View의 아래 카드는 생성 카드 둘만 남는다. 두 생성 카드의 호버는 `CREATE_GROUP_*`/`CREATE_ARMY_GROUP_*`, `OPERATION_NEW_GROUP`/`OPERATION_NEW_ARMY_GROUP`, `COMMAND_GROUP_DESC`/`COMMAND_ARMY_GROUP_DESC`의 원문을 사용한다.

집단군 목록 전환은 소속 군의 지휘관 머리줄을 나열하고 사단 선택 범위를 유지한다. 집단군 카드를 다시 누르면 사단 목록으로 돌아간다. 원본 실루엣 호버의 편제별 대대·지원 중대와 Shift 선택은 `Panel.templateSummaries`의 원문 툴팁·사단 ID를 사용한다. 오른쪽 전구는 `View.theaters`, 사단의 경고·적응 표시는 `MemberData`의 서버 값에 연결한다. 경험 휘장·노란 막대는 기존 `experienceLevel`/`experienceProgress`를 그대로 사용한다.

스킬 버튼은 선택한 실제 사단 ID로 `ABILITY/<스킬>`을 바로 요청하며 집단군은 `ABILITY/MARSHAL/<스킬>`을 사용한다. 왼쪽 반복 클릭은 새 revision 전까지 한 번만 전송하고 오른쪽 클릭은 조회창을 열지 않는다. 구형 `ARMY_ABILITIES` 요청도 같은 직접 경로를 사용하며 `ALL`은 조회창이나 임의 스킬 실행으로 이어지지 않는다. 화면 검사는 옛 능력 조회창 대신 이 입력 경로를 검증한다. 서버의 사용 조건·비용·효과 검증과 거절 이유를 유지하며 클라이언트가 성공 효과를 만들지 않는다.

스킬은 `ability_list_begin/mid/end` 배경 안에 아이콘만 그리며 개별 `nv_btn_select` 단추 틀을 덧씌우지 않는다. 아이콘의 원본 크기·위치, 클릭·툴팁 영역과 `ui.click` 소리는 유지한다. `affordable=false` 또는 `enabled=false && affordable=null`이면 같은 아이콘을 회색으로 그리고, 사용 불가 사유가 있으면 기존 어두운 덮개를 유지한다. 기본 스킬을 포함해 최대 4개를 서버 목록 순서로 표시한다. `MapCommanderSourceStateTest`의 GUI2 두 해상도 장군 탐색 공격·미지원 글라이더·집단군 추가 보급 장면과 기존 `army-layout` 입력 검사를 함께 사용한다. 이는 서버 테스트 응답의 렌더 검증이며 운영 접속·저장/재접속·멀티플레이·직접 청취의 증거가 아니다.

집중 검증 명령은 `test runGameTest remapJar -PhoiTestSuite=army-layout`이다. 동시 서버 변경 시 `-PhoiServerSource=<검증 HOI 사본 경로>`와 `-PhoiProtocolSource=<같은 사본의 protocol/src/main/java>`를 함께 지정한다. 권위 fixture와 프로토콜 일치 확인 후 GUI2 2560×1440·2560×1369·1280×720의 실제 렌더·선택·반복 클릭·오른쪽 클릭·해제·비활성 상태를 검사한다. 이 집중 검사는 필터 없는 전체 검사와 운영 다중 접속·직접 효과음 청취를 대신하지 않는다.

이 집중 범위는 카드·패널 입력과 원본 레이아웃이며 지도 드래그·운영 접속·다른 사용자 권한·실제 스킬 효과는 별도 검사 대상이다. 권위 fixture의 지휘관 경험은 0이고 집단군은 사단이 없으므로 인력·계획은 미상이다. 값이 없는 칸은 미상으로 유지하되, 사용자가 지정한 수송 중량 칸만 0으로 표시한다. 합성 계층의 긴 사단 이름·병과·경험 배지·인력·계획 0.75는 렌더 경계 확인용이며 캠페인 값이 아니다.

원본 `divisions_summary`의 “82”는 전체 사단 수이며 지휘관 경험치가 아니다. 왼쪽 `army_org_str_indicator`는 전체 사단의 조직력·병력 막대다. 초상 아래 특성·장교 역할 버튼과 오른쪽 아래 전체 선택·보급 차량화·목록 전환/철도포 그림을 표시한다. 실행 계약이 없는 버튼은 다른 기능으로 대신 실행하지 않는다. `traitDetails`의 실제 목록을 가로로 표시하며 일부 원본 훈장 그림 자체에 포함된 톱니는 별도 설정 버튼으로 취급하지 않는다.

사단 행은 `unitlist_unitinfo_button` 배경과 실제 병과의 `GFX_unit_<종류>_icon_medium` 첫 프레임을 사용한다. 병과 ID와 원본 GFX 이름이 다른 차량화·기계화·전차·포병 등을 명시적으로 대응한다. 경험 등급 2의 표식과 3/4의 월계관은 다른 원본 프레임이다. 노란 막대는 `unit_xp_bar`, 파란 막대는 `unitlist_bar_prepare`, 회색 그림은 `unitlist_dig_in_icon`이다. `MemberData.experienceProgress`는 현재 경험 등급 내부 진행 0..1, `planPower`는 계획 진행 0..1로 각각 노란 경험 막대와 파란 계획 막대를 채운다. `entrenchment`는 참호 단계이며 최대치 없는 단계 값을 비율로 쓰지 않는다. `entrenchmentProgress`는 서버의 현재/최대 참호 비율로 `dig_in_bonus`의 파란 막대를 채운다. null이면 틀만 유지한다. 육군 명령은 사용할 수 없어도 원본 밝기를 유지하며 툴팁·요청 검증으로 거절 이유를 알린다. `BPT_execute_*`·`BPT_cohesion_*`는 원본 GUI의 둘째 프레임을 사용한다.

원본 도구줄의 숫자 칸은 `transport_weight`와 `GFX_topbar_convoys`다. `Panel.manpower`는 인력 합으로 보존하되 수송 중량으로 표시하지 않는다. 선택 사단 행이 모두 도착하고 각 `transportWeight`가 있을 때만 실제 사단 중량을 합해 표시한다. 사용자 지정에 따라 누락 행·null 수송 중량은 0으로 표시한다. 이는 서버가 실제 중량 0을 확인했다는 뜻이 아니다. 서버 값은 원본 대대 `weight` 합이고 수송선 수가 아니다. `TRANSPORT_WEIGHT_VAL`은 원본에서 별도 소수 형식을 지정하지 않아 HOI는 합산 뒤 정수 반올림하며 원본 엔진의 반올림 방식과 같다는 주장은 하지 않는다. 호버의 전체 인력은 이 중량과 별도로 유지한다. 실행 화살표의 초록 체크는 `plan_evaluation` / `GFX_plan_value_strip`의 계획 평가다. 평가 없는 상태에 초록 체크를 만들지 않는다. 공세 버튼은 서버 `Panel.executionMode`의 `CAREFUL`/`BALANCED`/`AGGRESSIVE`를 원본 `bpt_execute_slow`/`mid`/`fast`에 대응한다. 비었거나 여러 카드의 자세가 다르면 기본 mid 프레임과 미상 안내를 유지한다. `Panel.organization`·`strength`는 선택 사단의 각 비율을 사단 수로 나눈 서버 평균이며 왼쪽 집계 막대를 채운다. 사단이 없으면 null이다. 차량화 우선순위는 현재 서버 `Panel.motorizationPriority`를 사용한다.

군 방패는 TFR `theatreselector.gfx`가 참조하는 원본 `gfx/army_icons/army_icons.dds`의 81개 33×32 프레임이다. `unitview.gui`의 `btn_army`는 같은 atlas를 참조하며 GUI의 frame=1은 추출된 frame_0이다. 자체 모양·색이 있는 휘장과 집단군 지도 RGB를 같은 값으로 취급하지 않는다. `plan_value_strip`도 실제로는 4프레임이며 frame_0=빈칸, 1=빨강 !, 2=노랑, 3=초록 ✓다. 서버 `Panel.planEvaluation` 0..2는 atlas의 1..3 프레임에 대응하고 null은 빈칸 frame_0을 유지한다. 원본 15×21 그림을 GUI2 기준 7.5×10.5 비율로 실행 화살표 위에 겹쳐 그린다. 원본에 없는 평가 결과를 실행 가능 상태에서 추정하지 않는다.

`motorization_level_toggle_strip`은 3프레임이며 `ARMY_MOTORIZATION_PRIORITY`는 클릭해 우선순위를 순환하는 설정이다. 원본 AI 갱신 주기(TFR 168시간)나 허브 최대 차량화 보너스(2.2)는 이 설정의 산출 공식이 아니다. 군은 서버의 `Panel.motorizationPriority`와 `ARMY_MOTORIZATION` 명령을 사용하며 응답 전에 표시 값을 바꾸지 않는다. 집단군의 혼합 상태·일괄 변경은 아직 별도 계약이 없다.

계획 평가의 원본 `PLAN_VALUE_*` 문구는 지상군·공중 우열, 강·해협·요새, 준비·미배치를 구분한다. 원작 `MAX_ESTIMATED_PLAN_UNITS_NOT_IN_PLACE_FACTOR=-0.6`은 미배치 비율에 곱하지만 전체 가중치와 색 경계는 확정되지 않았다. 서버의 현재 근사는 clamp(아군/적 전투력 − 1, −1, 1) − 0.6×미배치 비율이다. 전투력은 (연성+경성+방어+돌파)×병력×조직력 비율이며 적은 전선과 인접 프로빈스의 교전국 사단이다. 점수 <−0.1은 불리, −0.1..0.1은 위험, 그 위는 유리이고 적이 없으면 null이다. 전투력 구성과 ±0.1 경계는 HOI 근사이며 원본 공식으로 확인되지 않았다. 클라이언트는 점수를 계산하지 않고 서버 평가만 표시한다.

`operation_name`과 기본 제목 `BATTLE_PLANS`는 별개다. “만주”는 한글패치 `STRATEGICREGION_155`다. 원작 `common/names/00_names.txt`는 전선에 접한 적 프로빈스의 전략 지역별 작전명 풀이 국가 기본 풀보다 우선한다고 설명하며 `use_geographical_default_operation_names`도 정의한다. TFR의 같은 파일은 default의 작전명 필드를 제거했고 TFR/TFRKP KOR 이름 목록에도 작전명 풀이 없다. 복수 지역의 엔진 우선순위와 수동 이름·사진의 제목 선택 조건은 확인되지 않았으므로 집단군 전역 이름을 무조건 제목으로 쓰지 않는다. 서버가 보낸 최신 `commandCaption`은 선택 계획의 전선·목표 프로빈스를 전략 지역별로 세어 최다 지역 이름(동률이면 작은 지역 번호), 계획이 없으면 “전투 계획”이다. 이 서버 근사식과 확인된 원본 규칙을 구분한다.

사용자 지시에 따라 running이 true인 카드 끝을 화면 아래에 붙이고 false/null은 82GUI 위로 올린다. 집단군 카드 뒤에 소속 군을 배치하고 원본 프레임의 투명 여백을 제외한 폭을 누적해 보이는 테두리가 맞닿게 한다. `group_frame` 90×126의 좌우 여백은 각각 4px, `army_frame` 84×117은 각각 1px이며 원본 그림은 자르거나 바꾸지 않는다. 겹치는 투명 여백은 초상과 하단 숫자의 클릭 영역에서도 제외한다. 묶음 사이의 추가 간격도 없다. 긴 목록은 기존 페이지 버튼·휠로 이동한다. `MapCommanderGroupsRenderTest`는 두 해상도에서 인접 테두리와 끝 좌표를 검사하고 `hoi-army-hotbar-measurement-<해상도>.json`에 좌표를 기록한다. 핫바 사각형과의 교차는 숨겨진 바닐라 영역에 대한 참고값이다.

집단군 펼침 머리줄 왼쪽은 원본 반쪽 월계수 `theatre_army_group_shield`와 지휘 중인 군 수 `Card.count`를 표시한다. 오른쪽 칸은 군 수/한도다. 군 머리줄과 사단 끝은 `theatre_army_shield`를 유지한다. 문 모양 `uv_btn_unassign`은 집단군과 집단군에 소속된 군 머리줄에 표시한다. 이 군종 분기는 사용자 원본 두 장면의 대조를 반영하며 GUI 텍스트 자체에 실행 시 표시 조건이 적혀 있다는 뜻은 아니다.

지휘관이 없으면 원본 `UNIT_LEADER_NO_LEADER`의 빨간 “지휘관 없음”을 이름 칸에 표시하고 `CLICK_TO_ASSIGN`은 초상 안에서 기본 글자 크기로 줄바꿈한다. 두 키는 설치 한글패치에 덮어쓰기가 없어 기본 게임 `localisation/korean/unit_l_korean.yml`을 사용하며 명령 자산 보고서의 키 출처를 유지한다. 병과 요약은 `divisionTypes`의 수 내림차순·동률 asset ID 순으로 최대 11종을 놓고 원본 큰 병과 그림 아래에 수를 표시한다. 각 그림 칸은 숫자의 기본 글자 폭에 4GUI 간격을 더한 폭과 그림 폭 중 큰 값을 사용하고, 숫자를 그림 아래 가운데에 놓는다. 필요한 폭만큼 머리줄과 오른쪽 버튼 줄을 넓힌다. 합성 57개 사단의 10종 요약(15·12·7·7·5·4·2·2·2·1)은 원본 숫자 배치의 경계 검사이며 캠페인 수치가 아니다. 표시 이름은 `army-source-names.json`에 원본 OOB 경로·해시와 함께 보존한 KOR 57개 실제 이름이다. 이름 외의 순서·병과 조합·경험 등급/진행과 57/18 상한 장면은 합성 입력이며 같은 원본 캠페인 상태를 재현한 것이 아니다. 이 시험 전용 이름 파일은 배포 JAR에서 제외한다. 지도 fixture의 A/B/C 내부 ID는 유지하고 표시 이름만 `1 군`·`2 군`·`3 군`으로 구분한다.

설치 TFR `subuniticons.gfx`의 `GFX_unit_infantry_icon_medium` 원본 자체는 철모다. 차량화는 `GFX_unit_motorized_icon_medium`의 트럭, 공수는 `GFX_unit_paratrooper_icon_medium`의 해골이며 같은 원본 그림을 사단 행과 요약에서 사용한다. 그림에 추가 초록 tint를 곱하지 않는다. 서버 `VisualDivisionStacks.type`의 지도용 주력 병과 판정을 원본 엔진의 편제 그림 선택 공식으로 확정하지 않는다. 수동 편제 그림이 필요하면 별도 원본 GFX/asset ID와 그 그림 기준의 전체 집계 계약을 서버에서 받아야 한다.

`unitview.gui:344`의 머리줄 `army_item_bg`와 `:1663`의 사단 끝 `btn_army`는 모두 `GFX_theatre_army_shield`다. 군 머리줄은 이 atlas를 사용하고, 집단군은 원본 캡처의 반쪽 월계수에 맞춰 집단군 atlas를 사용한다. `army_icons.dds`의 기본 frame_0은 흰 방패, frame_80은 자체 색이 있는 톱니형 휘장이다. 이전 합성 입력의 insignia=80이 톱니를 표시한 것이며 상태 경고로 해석하지 않는다. 원본 캡처의 빨강/초록은 기본 방패에 군 색상을 적용한 것으로 보이나 `.gui`는 지휘관 유무·문제 상태에 따른 색 분기를 정의하지 않는다. 같은 파일 :453의 `army_color_button`은 `GFX_group_marker3` 색 선택 버튼이며, 설치 실행 파일의 문자열에도 같은 버튼 이름이 있다. 이 증거는 색 설정의 존재를 뒷받침하지만 기본 팔레트 배정 순서나 최종 shader tint를 확정하지 않는다. 군 휘장과 군 RGB의 서버 표시 계약이 없는 동안 임의 경고색을 만들지 않는다. `Panel.color`의 집단군 지도 RGB와도 구분한다. 확정된 서버 `Panel.insignia`와 `Panel.armyColor`를 군 머리줄에, `MemberData.insignia`와 `MemberData.armyColor`를 사단 끝 방패에 적용한다. null 휘장은 frame_0, null 색은 흰색을 유지한다. 집단군의 두 값은 null이며 집단군 RGB로 대신 칠하지 않는다. 군 색은 서버가 정한 원본 팔레트의 나라별 군 ID 순 배정이다. 이 기본 배정 순서와 전체 그림의 곱셈 tint는 HOI 규칙이며 원본 엔진의 기본 배정·shader와 동일하다는 주장은 하지 않는다. 군 색은 `Panel.palette`가 있는 군의 머리줄 방패를 왼쪽 클릭해 선택하며 `ARMY_COLOR`, `[군 id,"rgb/<0..16777215>"]`, 빈 rays를 보낸다. 기존 집단군 팔레트를 재사용하고 오른쪽 클릭은 전송하지 않는다. 새 revision 전 반복 요청을 차단하고 `Panel.armyColor`·`MemberData.armyColor` 수신 전에는 그림 색을 바꾸지 않는다. 원본 `army_color_button/GFX_group_marker3`의 (405,10),25×25px는 X 좌표와 겹친다. 설치 `hoi4.exe` SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`의 초기화 함수 `0x1416aed50`에서 `army_item_bg`(문자열 주소 `0x1429ea8d8`, 필드 +0x38)와 `army_color_button`(참조 `0x1416af655`, 필드 +0x80)이 `0x1416af082`·`0x1416af6a3`에서 같은 +0x16c8 핸들러에 연결됨을 확인했다. 이 정적 분석을 근거로 방패 입력을 연결했으며, 겹치는 별도 버튼의 실행 시 표시·재배치 조건과 원작 팔레트 창 배치는 미확정이다. 재현은 해당 SHA-256 실행 파일의 위 함수 구간을 `llvm-objdump -d --no-show-raw-insn --start-address=0x1416aed50 --stop-address=0x1416b05b8 <hoi4.exe>`로 읽는다. 원본 실행 파일은 수정하지 않는다. 수동 편제 그림과 자동 병과 선택 공식은 아직 미확정이다.

사단 0개여도 전략적 재배치·정지 등 도구줄을 유지한다. 군의 지휘관 아래 버튼은 전체 선택·차량화·철도포·부속 비행단 순서로 붙이고 집단군은 전체 선택·차량화·군/사단 전환 순서다. 차량화는 `army_motorization_priority_button_overlay`의 상태 프레임이며 집단군이라는 이유만으로 겹친 차량 프레임을 강제하지 않는다. 군의 `Panel.motorizationPriority` 0/1/2는 원본 말·트럭·겹친 차량 프레임과 대응한다. 왼쪽 클릭은 `ARMY_MOTORIZATION`, `[군 id]`, 빈 rays를 보내며 새 revision 전 반복/오른쪽 클릭은 전송하지 않는다. 집단군의 혼합 우선순위와 순환 명령은 별도 계약 대기다. 사단 목록과 정렬 열은 병과 요약 때문에 늘어난 머리줄 폭을 함께 사용하며 이름 열을 넓힌다. 스킬이 없을 때 목록을 명령 막대 바로 위까지 채운다. 스크롤바의 현재 크기·원본 트랙·손잡이 규칙은 이 절 앞의 작은 스크롤바 설명을 따른다. 실행 체크는 `Panel.planEvaluation`의 0/1/2를 빨강/노랑/초록 프레임 1/2/3으로 대응하고 null은 빈 프레임이다. 선택하지 않은 군 카드에도 빈 실행 버튼 바탕 둘을 유지한다. 카드 수는 초상 아래 칸 안에 고정 크기로 표시하고 계획 null/0 막대의 내부는 원본 화면의 거의 검정인 `#020305`이며 원본 둥근 `plan_frame`을 겹친다. 이 내부색은 화면 대조값이며 원본 progress shader를 복원했다는 주장이 아니다.

육군 머리줄·색상 입력·스크롤의 집중 검증은 팩 빌드 후 Java 25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 4분에 통과했다. 클라이언트 122개·protocol 119개·입력 생산자 46개는 실패·오류·건너뜀 0, 실제 PNG는 199장이다. 이번 작업의 첫 실행에서 JUnit/producer를 재생성했고 마지막 실행은 변경 없는 성공 결과를 재사용했다. 별도 생산자 사본은 r8 코드에 확정된 `ProtocolCommanderHud.java` SHA-256 `ad7110f7ede4b5ab293fa60f7b6a367d51eed7640484a1ff12f66778c0530e6a`만 복사한 것으로, 최신 서버 구현의 검증이 아니다. 공유 소스 48개와 입력 provenance를 검사했다. Python 명령 자산 가져오기 7개 검사도 통과했다.

GUI2 두 해상도에서 회색 배치 행, 색 띠 없는 집단군 카드·펼침 화살표, 거의 검정인 빈 계획 막대, 머리줄과 같은 폭의 사단 목록, 명령 막대 직전까지의 좁은 화면 목록, 미지정 목록 최대 11줄·연결된 배경·스크롤바·수송 칸·닫기·행 X와 카드 바로 위 생성 툴팁을 확인했다. 기존 군 색·카드 드래그·선택·스크롤 회귀와 차량화 0/1/2 프레임·우클릭 무전송·revision 전 중복 차단·정확한 군 ID 요청을 검사한다. HQ 이름/비용/부족 상태와 이동 중 15일 문구는 새 필드를 명시한 합성 계약 입력으로 확인했다. 이 장면의 지휘관 상세/초상은 미제공이고 비용 7·진행률·수송 중량·경험 등도 합성이다. 57개 표시 이름만 원본 OOB 출처가 있다. 새로운 실제 서버 명령·저장·소켓·재접속·다중접속·직접 청취와 전체 클라이언트 검사는 미수행이다.

중간 실행은 199장 캡처 뒤 Fabric `TestSingleplayerContextImpl.close`에서 렌더 스레드가 `IntegratedServer.halt`를 기다리고 서버 스레드가 테스트 Phaser를 기다려 종료되지 않았다. 해당 테스트 프로세스만 종료하고 스레드 덤프·실패 로그를 보존했다. 최종 재실행은 정상 종료했으며 종료 대기 실행을 성공으로 계산하지 않는다. 최종 JAR SHA-256은 `79ba4c91df8a4837474f10350c2450f4aa219a1648d07c6cd01ed5113e2b6b2f`다. JAR와 내장 protocol JAR에는 프로젝트 모드 아이콘 외 게임 정적 이미지/음향, 서버 군사 코드, 시험 차원과 이름 fixture가 없다.

군 카드 X는 원본 `lg_disband_group_btn`의 군 해체 의미로 `ARMY_TOOLBAR/DISSOLVE_ARMY [군 id]` 서버 확인창을 연다. 서버가 확인 후 군을 해체하고 사단을 보존해 미지정으로 돌린다. 기존 서버의 계획·지휘관·부속 비행단 처리와 비용 검증을 유지한다. 사단 삭제는 별도로 군·집단군·미지정 목록의 휴지통에서 `DIVISIONS_DISBAND [unit/id…]` 확인창을 사용한다. 두 요청은 기존 Open/ProtocolDialogView 경로이며 클라이언트에서 군·사단·선택 상태를 먼저 바꾸지 않는다. 미지정 사단 행의 X는 이미 군 소속이 없는 해당 행의 선택 해제이며, 패널 닫기와 함대 카드 X도 기존 선택 해제다. 편제 변경 활성 상태·후보 계약은 아직 대기 중이다. 실제 HQ 배치 실행, 경고/적응, 편제별 구성 툴팁과 전구는 다음 계약 대기다. 따라서 이 검증은 가 단계 전체 완료나 재리뷰 제출이 아니다.

전구 설정은 `THEATER_SETTINGS/LOW|NORMAL|HIGH|MIXED`를 읽어 머리줄 왼쪽 원본 설정 창을 연다. 이름은 표시만 하며 전구 해체는 이 창의 `THEATER_DISBAND` 서버 확인 요청으로 보낸다. 우선권은 `FORMATION/THEATER_PRIORITY/LOW|NORMAL|HIGH`와 전구 ID 하나로 보내고 응답 전에는 선택 그림을 바꾸지 않는다. 원본 `div_priority_strip`의 비선택 0~2·선택 3~5 프레임이며 MIXED는 모두 비선택이다. X·Escape는 설정 창만 닫고 국가/차원/branch 변경은 팝업을 초기화한다. 공격·방어 컨트롤의 전체/문제 수를 표시하고 0은 회색, 문제 있음은 빨강, 그 밖에는 초록으로 칠한다. 색의 정확한 엔진 RGBA는 미확정이며 전투 이력 창은 차 단계다.

카드와 전구 줄은 같은 툴팁 생성 경로를 쓰며 병과 수에 기존 원본 그림을 키워드 글리프로 표시한다. 군 줄 우클릭은 선택한 사단의 `FORMATION/MOVE_DIVISIONS`이고 집단군 줄 우클릭은 입력하지 않는다. 현재 `Card.detail`에는 전체 약력이 포함되고 원작의 기술·경험 표시/원수 부분 혜택의 권위 효과 목록은 없으므로 이 블록의 원본 일치 검증은 서버 계약 대기다. 누락된 효과를 클라이언트에서 계산하지 않는다. 전체 클라이언트 검증 슬롯은 사용자 답변613d306e로 승인받았으나 전체 실행 전 남은 서버 계약 대기로 반환했다. 새 실행 때 동시 작업자와 슬롯을 맞추고 시작·종료를 알린다.

### 전투 계획 원본 대조의 남은 범위

현재 `MapCommanderPanels.actionbar`는 훈련/중지, 전선·공세·선봉·후퇴·지역 방어, 배정/해제·편집/삭제, 실행 방식 셋을 그린다. `MapCommanderOrders`는 기존 인증 세션·revision으로 `LAND/FRONT`, `OFFENSIVE`, `SPEARHEAD`, `FALLBACK`, `AREA_DEFENSE`, `ASSIGN_UNITS`, `EDIT_PLAN`, `DELETE_PLAN`과 `ARMY/*` 요청을 보낸다. 이 경로의 존재는 새 원본 221104~221223, 221820~221907과 시각·동작이 같다는 검증이 아니다.

새 대조에서는 모든 명령 툴팁의 색·단축키 키캡·금지 이유와 실행/결속도/HQ 근접도 팝업을 확인해야 한다. 현재 실행 방식 셋은 작은 버튼만 있고 원본 제목·틀이 없다. 결속도는 기존 조회 경로이며 원본 4버튼 팝업은 미구현이다. HQ 근접도 0/1/2/3/자유 팝업은 미구현이다. `UiCommandLabels.withoutShortcuts`를 쓰는 전선 계열은 원본 키캡을 지우므로 새 요구에 맞춰 변경해야 한다. 공수와 부유항 상륙은 원본 그림만 있고 실행 계약이 없다. 서버가 기능을 제공하기 전 이를 성공한 명령으로 표시하지 않는다.

현재 `Panel.abilities`는 ID 목록이며 원수/장군의 대대 수·기본 비용·능력 비용 배율·보유 지휘력·사령부별 총비용을 포함하지 않는다. 서버의 확정된 상세 툴팁/사용 가능 상태 계약이 필요하다. 기존 서버 원문 툴팁은 활용하되 스크린샷의 숫자를 하드코딩하지 않는다.

지도 `PlanOverlay`는 `ProtocolAtlasScene.Page`의 박스와 `MovementQuad`를 받아 `MapSceneAssembler`→`MapSceneMesh`로 그린다. 곡선 라벨의 원문·경로, 선 종류/실행 상태, 빗금 영역의 경계에 해당하는 명시적 데이터는 없다. 기존 박스로 그린 선이 원본 굽은 라벨·빗금·초록 실행 화살표·점선과 동일하다고 볼 수 없다. 선택 사각형과 사단 행 상세 요청은 존재하지만 원본 주 툴팁의 현재 선택 요약·장비 수·진입 금지 이유 및 사단 카운터의 손실 문단은 별도 서버 표시 계약과 실제 지도 렌더 검증이 필요하다.

스킬 도중 사단 증가 시 효과 감소, 미표시 기본 재사용 대기, 실행 방식/결속도 이동 규칙, 사상자 전쟁 지지도 감소의 엔진 계산은 아직 분석하지 않았다. 공개 defines의 대대당 비용·168시간·근접도 표와 이 미확정 계산을 구분한다. 원본 실행 파일의 해시·함수/주소·호출 조건을 확인한 근거 없이 원작 공식이나 근사치를 확정하지 않는다.

### 전략 지도 HUD와 키캡 안내

`MapSelectorHud`는 유효한 국가·차원의 지휘 카드 줄을 표시하고 서버 `View.running == true`일 때만 바닐라 핫바·관전자 메뉴·체력·방어력·허기·공기·탈것 체력·경험치/로케이터 정보 막대·경험치 레벨·선택 아이템 이름을 숨긴다. 지도 안팎 모두 같은 조건이다. false/null(정지·미시작·이전 서버)에서는 기존 Fabric HUD 요소를 호출하고 카드 줄·명령·팝업을 82GUI 위로 올린다. true로 바뀌면 다시 화면 아래에 붙이며 카드가 없어지거나 카드를 그리지 않는 화면에서도 바닐라를 유지한다. 좁은 정지 화면의 명령 막대는 왼쪽 패널 오른쪽으로 이동해 버튼 영역이 겹치지 않게 한다. 서버는 1초마다 상태를 보내므로 이 표시는 수신한 상태를 따르며 클라이언트가 시계를 추정하지 않는다. 플레이어 GUI 설정을 바꾸지 않는다. 오른쪽 안내는 전략 지도 차원에서 실제 핫바의 `hoi:map/selector/` 아이템만 슬롯 순서대로 표시한다. 원본 외부 팩의 `nv_btn_select` 테두리와 숫자를 합쳐 키캡으로 그리고, 실제 아이템 모델과 서버가 지정한 금색/흰색 이름을 사용한다. 원본 HOI4의 목록을 복원한 기능이 아니라 사용자가 추가한 안내다. 선택한 슬롯 강조는 추가하지 않는다. 화면 오른쪽 아래 여백 6GUI에 붙이고 카드 제어와 가로 범위가 겹치면 카드 위로 올린다. 키캡·모델·이름 및 1→9 순서는 유지한다.

육군 지휘 영역은 Minecraft 기본 글꼴을 읽을 수 있도록 최소 GUI2 기준 1배 크기를 유지한다. 글자를 해상도 비율로 축소하지 않고 패널·행·칸을 함께 키운다. 긴 사단명은 같은 크기에서 말줄임하고 미배치 안내만 초상 안에서 줄바꿈한다. 선택 수는 한 줄로 그린다. null/0 계획 막대는 어두운 바탕과 원본 `plan_frame` 둥근 테두리를 표시하고 실제 진행률이 있을 때 초록색을 채운다. 스킬은 원본 `abilitylist.gui` 첫 버튼 위치 (29,8)px와 36px 간격을 GUI2에 대응한 (14.5,4)+18px 고정 칸을 사용한다. 명령 막대 첫 묶음의 넷째 빈칸은 이 스킬 컨테이너가 아니다. `Panel.abilities`의 원본 정의 순서대로 지휘관 보유 스킬을 왼쪽부터 채운다. null은 미제공, 빈 목록은 0개이며 두 경우 모두 임의 버튼이나 빈 스킬 컨테이너를 그리지 않는다. 복수 지휘관은 첫 서버 목록 순서의 공통 스킬만 표시한다. 집단군 선택은 원수 목록을 사용하고 사용 가능 여부·거절 이유는 별도 권위 검사로 유지한다. `MapCommanderAbilitiesRenderTest`는 합성 0·1·4개와 null 목록을 두 해상도에서 렌더하며 실제 스킬 효과 검증과 구분한다.

`MapSelectorHudTest`는 일반 차원과 합성 지도 차원에서 true→false→null→true 상태의 HUD와 카드 위치를 두 해상도로 검사하고 카드 제거 후 복원도 캡처한다. 별도 합성 `hoi:atlas_ui_test` 차원을 테스트 리소스로만 등록하고 통합 테스트 서버의 플레이어를 잠시 이동해 실제 HUD 숨김과 아홉 아이템 모델을 렌더한다. 테스트 종료 시 차원·좌표·인벤토리를 복원한다. 운영 지도·사용자 저장·다중 접속 검증과 구분한다.


### 육군 목록 하단과 지휘 단추

집단군 사단 목록은 현재 서버 HUD의 `view.cards()`에서 해당 집단군 소속 군을 하단 카드의 왼쪽 순서로 모으고, 각 군의 기존 사단 ID 순서 목록을 이어 붙인다. 어느 소속 군에도 대응하지 않는 행은 기존 순서를 유지하여 맨 뒤에 둔다. 단일 집단군 선택과 복수 카드 선택의 집단군 행에 같은 규칙을 적용한다. `CARD_ORDER`, `MOVE_ARMIES`, `DETACH_ARMIES`의 소속·순서 응답이 반영되면 최신 카드 목록을 기준으로 다시 계산하며, 군·미지정 목록 순서, 선택 수와 선택 상태는 유지한다.

관련 단위 검사는 `:client:test --tests dev.hoi.client.map.MapCommanderPanelOrderTest --tests dev.hoi.client.map.MapCommanderGroupsTest --tests dev.hoi.client.map.MapCommanderCardDragTest`다. 기존 팩·r12 사본을 쓰는 육군 집중 검사는 이 명령 뒤에 `runGameTest -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/protocol/src/main/java -PhoiResourcePack=C:/HOI-resourcepack/build/resources.zip --console=plain --no-daemon`을 붙인다. Java25와 `HOI_ARMY_REVIEW_PREVIEW=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r12-server/HOI/fabric/build/army-review-fixtures`가 필요하다. `MapArmySourceReviewTest`는 실제 렌더에서 57개 사단의 맨 위·19/20행 경계·38/39행 경계·맨 끝, 재정렬 응답과 복수 카드 선택의 순서를 두 해상도 GUI2로 검사하고 방패 색과 소속 군을 대조한다. 고정 서버 입력과 합성 재정렬 응답을 사용하므로 실제 소켓·저장/재접속·멀티플레이 검증을 대체하지 않는다. 이 변경의 범위에는 팩 재빌드·필터 없는 전체 검사·운영 설치가 포함되지 않는다.

집단군 순서 변경의 최종 집중 검사는 5분45초에 통과했다. 관련 단위 11개 실패·오류·건너뜀0, 실제 PNG381장이다. 두 해상도에서 빨강19행→청록19행→보라19행과 재정렬·복수 선택을 검사했다. 직전 배치 대비 머리줄·57개 선택 수 영역의 변경 픽셀은0이고, 2560×1440의30행·마지막 행/끝 y1262/1268, 1280×720의4행·y378/384와 하단6px를 유지한다. 첫 실행은 새 테스트가 스크롤바 끝점 바깥을 눌러 작은 화면 마지막 행 검사에서 실패했다. 클릭을 경계 안쪽으로 고친 뒤 재실행했으며 실패 로그도 보존했다. 실제 네트워크·저장/재접속·멀티플레이와 필터 없는 전체 검사는 이 결과에 포함하지 않는다.

군·집단군·미지정 사단 목록은 마지막 사단 줄 아래에 어두운 바탕 5px와 끝선 1px, 합계 6px(3GUI)를 둔다. 군·집단군 목록 오른쪽 스크롤바 칸도 같은 y에서 끝난다. 원본 `20261017214326`의 y1209/1215와 `20261017214419`의 y1263/1269를 기준으로 하며, 앞선 2px 해석을 대체한다. 넓은 화면에서는 기존 표시 행 수를 유지하면서 하단 허용 범위를 4px 늘리고, 좁은 화면에서는 여백을 먼저 확보한 뒤 행 수를 계산한다. 미지정 목록의 기존 안내문은 보존하고 마지막 행 아래 6px 뒤에 표시한다.

펼친 집단군의 `lg_army_group_bg`·`lg_army_mid_bg`·`lg_army_end_bg`는 윗부분을 자르지 않는다. 판은 집단군 정지·진격 칸의 위쪽부터 마지막 소속 군 카드까지 이어지며, 소속 군의 칸 위로 판과 오른쪽 사선 모서리가 보인다. 정지·진격 칸은 카드에 붙은 위치를 유지한다.

판 복구와 6px 보정은 기존 팩·r12 고정 서버 사본을 재사용한 Java25 `:client:test --tests dev.hoi.client.map.MapCommanderGroupsTest --tests dev.hoi.client.map.MapCommanderCardDragTest runGameTest -PhoiTestSuite=army-layout`로 5분55초에 통과했다. 소스 속성과 `HOI_ARMY_REVIEW_PREVIEW`는 위 동일 사본 재현 규칙을 따른다. 단위 8개 실패·오류·건너뜀0, 실제 PNG369장이다. GUI2 2560×1440에서 군19개의 마지막 행/목록 끝은 y932/938, 집단군57개는 y1262/1268이며 오른쪽 칸도 같은 y에서 끝난다. 1280×720에서는 각각 y354/360과 y378/384다. 미지정 목록·직접 선택·지도 드래그는 큰 화면11줄(y512/518), 작은 화면10줄(y478/484)과 6px를 유지한다. 군·집단군 머리줄은 CMP-36 대비 두 해상도 모두 변경 픽셀0이다. 앞선 픽셀 실행의 끝선 잘림/행 수 감소를 수정했고, 5분49초 중간 통과 뒤 미지정 여백 줄의 폭을 테두리 안으로 다듬어 최종 재검증했다. 필터 없는 전체 검사·팩 재빌드·remapJar·운영 접속·저장/재접속·멀티플레이·직접 청취는 이번 검사에 포함하지 않는다. 합성 집단군 계획 칸은 표시·입력 검사이며 실제 집단군 계획 실행 증거가 아니다.

군·집단군 목록은 마지막 표시 행까지 배경을 늘리고, 왼쪽 패널과 명령 막대가 가로로 겹치지 않는 넓은 화면에서는 카드 위 공간까지 사용한다. `unitview.gui`의 `army_division_list_view` 높이는 원본 엔진에서 정하므로 GUI 파일의 430px 값을 전체 화면의 고정 한도로 쓰지 않는다. 넓은 화면 하단은 사용자 제공 20261017214419의 관측값에 맞추며, 좁은 화면에서는 기존 명령 막대·스킬 영역 위에서 멈춘다. 미지정 사단이 포함된 직접·Ctrl 전환·지도 드래그·혼합 선택은 최대 11줄이며 선택 자체와 서버 명령 대상은 잘라내지 않는다.

지휘관 단추 줄은 맨 오른쪽 부속 비행단/군·사단 전환 단추 위치를 기준으로 왼쪽 단추를 붙인다. 군·집단군 머리줄 닫기 X와 아래 군 카드 해산 X는 최신 사용자 정정에 따라 이 보정 전 위치에서 왼쪽·아래 각각 화면 1px 이동한다. 이전 1GUI 이동과 누적하지 않으며 실제 GUI 배율의 역수로 그림과 클릭 영역을 함께 보정한다. 아래 집단군 카드 펼치기·접기는 1GUI 왼쪽으로 옮긴다. 함대 카드의 접기 버튼은 제거하고 X를 군 카드와 같은 상대 위치에 놓으며, 왼쪽 클릭은 선택 해제·오른쪽 클릭은 무동작을 유지한다. 아래 정지·진격 칸은 카드 위 경계와 카드 폭에 맞추며 원본 그림과 기존 서버 명령·비활성 이유를 사용한다.

이 보정은 팩 재빌드 후 Java25·r12 고정 서버 입력으로 `:client:test --tests dev.hoi.client.map.MapCommanderGroupsTest --tests dev.hoi.client.map.MapCommanderCardDragTest runGameTest remapJar -PhoiTestSuite=army-layout`를 실행해 5분42초에 통과했다. 단위 테스트 8개 실패·오류·건너뜀 0, 실제 PNG369장이다. `HOI_ARMY_REVIEW_PREVIEW`와 `-PhoiServerSource`·`-PhoiProtocolSource`는 위의 동일 사본 재현 규칙을 따른다. GUI2 두 해상도에서 기존 좌우·반복 입력·드래그 검사와 미지정 직접/Ctrl/혼합/지도 드래그의 최대11줄, 함대 접기 부재와 X 선택 해제를 검사했다. 좁은 화면에서는 공간에 맞춰 11줄보다 적게 표시한다. 군19개 장면의 마지막 행/배경 중앙 아래 끝은 y932/934px, 집단군57개 장면은 y1262/1264px다. 집단군 정지·진격 표시는 서버 군 계획 입력을 집단군에 적용한 합성 장면이며 실제 집단군 계획 실행 증거가 아니다. 필터 없는 전체 검증·운영 접속·저장/재접속·멀티플레이·직접 효과음 청취는 이번 집중 검증에 포함하지 않는다.

### 지휘 카드 수평 드래그

카드 초상을 왼쪽 버튼으로 누르고 가로로 3GUI 이상 움직이면 드래그한다. 세로 좌표는 고정하며 현재 페이지의 카드 줄 양 끝 안에서 움직인다. 놓으면 가까운 카드 칸에 끼우고 나머지 카드는 밀어 표시한다. 집단군은 펼친 소속 군 블록과 함께 움직이며 접힌 소속 군도 전체 순서 요청에서 보존한다. 클릭만 하고 떼면 기존 선택을 수행한다. X·접기·색상·생성 카드는 기존 동작을 유지한다. 함대의 순서 요청 경로도 공유하지만 함대 원본 대조는 별도 단계다.

육군 지도에서 펼친 집단군 묶음 밖으로 군을 옮기면 `FORMATION/DETACH_ARMIES`, 다른 묶음 안으로 옮기면 `FORMATION/MOVE_ARMIES`를 보낸다. 소속 변경 뒤에는 동일 세션의 새 revision과 변경된 소속 HUD를 모두 확인하고 같은 종류의 전체 ID 목록으로 `CARD_ORDER`를 보낸다. 선택·나라·차원·군종·세션 변경, 응답의 소속 불일치 또는 100클라이언트 tick 동안 미확인 시 후속 요청을 버린다. HUD는 revision 필드가 없어 이 순서는 두 메시지의 수신 확인이며 네트워크 응답의 원자성을 증명하지 않는다. 순서나 군 이름·소속·집단군 수를 클라이언트 저장 상태에 먼저 반영하지 않는다. 모든 요청은 빈 rays와 기존 서버 권한·revision 검사를 사용한다.

`MapCommanderCardDragTest`는 전체 형제 목록·군 소속·끝 경계·함대 예외를 검사한다. `MapCommanderCardDragRenderTest`는 GUI2 두 해상도에서 실제 화면 입력으로 가로 드래그·클릭·집단군 블록 이동·이탈/편입·두 메시지 순서·Escape·거절 HUD 뒤 후속 요청 폐기를 검사한다. 응답은 합성 HUD와 Context이므로 운영 지도 드래그·실제 네트워크/저장/재접속·다중 사용자 동기화의 증거는 아니다. 집중 명령은 팩 빌드 뒤 `test runGameTest remapJar -PhoiTestSuite=army-layout`이며 마지막 전체 검증과 나·다·라 원본 대조는 별도로 수행한다.

이전 카드 드래그 집중 검사는 클라이언트 JUnit 122개·프로토콜 119개·권위 producer 45개에서 실패·오류·건너뜀 0이며 실제 PNG 151장을 생성했다. 권위 입력은 최신 확정 계약을 복사한 고정 서버 사본에서 생성했고 재실행 시 변경되지 않은 JUnit/producer의 성공 결과를 재사용했다. 초기 컴파일 오류, 고정 네 스킬을 가정하던 검사와 첫 칸 이동 오류의 실패 로그도 이슈 검토 ZIP에 보존한다.

육군 집중 범위에는 재리뷰를 위한 현재 해군 함대·공군 기지/비행단 펼침 칸도 포함한다. `MapCommanderPanelsTest.runArmyLayout`에서 navy/air 권위 입력을 같은 두 해상도로 그리지만 이 캡처는 나·다 단계의 원본 일치 완료가 아니다. 정지 상태의 바닐라 HUD 유지와 군 RGB 표시, 기본 글자 크기·선택·요청 경계를 함께 확인한다. 운영 접속·실제 소켓의 정지/재개 전파·저장/재접속·다중 접속·직접 음향 청취는 별도다.

정지 상태의 왼쪽 목록은 위로 올라온 명령 팝업의 높이와 별개로 첫 카드 위까지 사용한다. 부속 비행단을 선택하면 좁은 화면의 명령 막대도 공군 도구줄 오른쪽으로 옮긴다. 페이지 버튼은 카드 위 높이 안에서 같은 글자 크기로 그리며, 온전한 한 줄도 없는 경우 기존 마우스 휠로 이동한다. 지도 삭제·배정·계층 선택의 회귀 입력은 재배치된 버튼과 겹치지 않는 오른쪽 지도 지점을 사용한다.

앞선 running 조건·군 RGB·육해공 패널 검증을 포함한 `test runGameTest remapJar -PhoiTestSuite=army-layout`는 3분24초에 통과했다. 실제 PNG176장, JUnit122+119개, 고정 서버 사본의 권위 producer46개에서 실패·오류·건너뜀0이다. 31개 권위 입력과48개 공유 protocol 소스를 검증했고 변경 없는 성공 JUnit/producer 결과는 재사용했다. JAR와 내장 protocol JAR에는 게임 정적 이미지/음향·서버 군사 클래스·테스트 차원이 없다. 두 모듈 목록 아이콘은 같은 사용자 제공 그림(SHA-256 `3da4c74a23c33a7aba8b7e1d507b2c4ed8a85cbcd3eeb6e30b015acaa44cd0c2`)이다. 이는 나·다·라 원본 일치나 필터 없는 전체 검사·운영 소켓 검증 완료가 아니다.

좁은 정지 화면에서 명령 막대를 오른쪽으로 옮긴 경우 날짜 스코어보드도 82GUI 위로 올려 서로 겹치지 않게 한다. running true·카드 미표시·넓은 화면에서는 바닐라 스코어보드 위치를 유지한다. 정지 캡처의 속도 문구는 서버와 같은 `ProtocolCampaignStyle.speed("PAUSED")`를 사용한다.

편제 변경 두 화살표는 `unitview.gui`의 `divisions/expand_button/GFX_change_template_button`이다. 원본 DDS 자체는 초록이며 원본 화면 214313·214326·214413의 회색은 비활성 표시다. 설치 `hoi4.exe` SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`에서 툴팁 처리 `0x14169f9db`는 버튼의 가상 함수 +0x208 결과로 활성 설명 `CHANGE_TEMPLATE_UNIT`과 비활성 이유 포매터 `0x141698040`을 나눈다. 포매터의 이유 번호 0/1/2/3/4/5는 각각 `EXPEDITIONARIES_CHAGE_TEMPLATE`, `CHANGE_TEMPLATE_DISABLED_BORDERWAR`, `CHANGE_TEMPLATE_DISABLED_SCRIPT`, `CHANGE_TEMPLATE_DISABLED_FAKE_UNITS`, `CHANGE_TEMPLATE_DISABLED_ENCIRCLED`, `SPECIAL_UNIT_CHAGE_TEMPLATE`이다. 선택 부대 순회 `0x1416b7241..0x1416b7352`에서 특수 부대 판정, +0x1dc 원정군 필드, 국경 분쟁, 위장 부대, 편제 잠금, 포위/보급 조건에 대응하는 이유를 기록하고 허용 플래그를 내리는 분기를 확인했다. 내부 함수 의미는 해당 이유 키와 분기 연결로 해석한 것이며 모든 상위 예외/국가 권한 판정까지 복원한 것은 아니다. 재현은 같은 실행 파일에 `llvm-objdump -d --no-show-raw-insn --start-address=0x141698040 --stop-address=0x1416983c0 <hoi4.exe>`와 `--start-address=0x1416b71a0 --stop-address=0x1416b73c5`를 사용한다. TFRKP `history/units/KOR_2020.txt`의 `jiyeop command`는 `is_locked=yes`이고 첫 두 사단이 이 편제를 참조한다. 이것은 원본 선택에 잠긴 편제가 포함될 가능성의 근거이며 사진 속 모든 사단의 실제 저장 상태나 전체 활성 조건을 확정하지 않는다. 후속 `templateBlock` 계약으로 서버가 제공한 사전 비활성 사유를 사용한다. 군 소속 목록 열기로 대신 실행하지 않는다.

`unitview.gui:1608`의 삽 왼쪽 `bar_prepare`와 `:1649`의 오른쪽 `dig_in_bonus`는 같은 파란 `GFX_unitlist_bar_prepare`를 회전해 사용한다. 각각 기존 `planPower`와 `entrenchmentProgress`에 연결하며 값을 받지 못하면 채움을 그리지 않는다. 57사단 경계 입력에는 두 진행률 0.5와 참호 값 3을 명시해 작은 파란 막대를 검사한다. 실제 캠페인 값으로 제시하지 않는다. 생성 카드 툴팁은 카드 바로 위의 공간을 먼저 사용하고 뷰포트 안에 제한한다. 미지정 사단처럼 명령 막대가 없는 경우 그 가상 영역을 피하지 않는다.


군 카드 X·휴지통 분리의 집중 검증은 Java 25에서 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r3-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 3분34초에 통과했다. GUI2 PNG199장과 JUnit122+119개에서 실패·오류·건너뜀0이며 변경 없는 protocol/생산자 성공 결과는 재사용했다. `MapCommanderGroupsRenderTest`는 군·집단군·미지정 목록의 휴지통 선택 범위, 우클릭 무전송, 군 카드 X의 서버 확인 요청과 응답 전 상태 유지, 함대 X의 기존 선택 해제를 검사한다. 삭제 확인창 요청을 캡처하는 합성 입력 검사이며 실제 서버의 삭제/취소, 세션 권한, 저장·재접속·다중 접속 검증을 대체하지 않는다. 고정 생산자는 새 `DIVISIONS_DISBAND` 구현 이전의 사본이다. 전체 클라이언트 검사와 가 전체 원본 대조는 남아 있다.

4차 표시 계약은 `ProtocolCommanderHud.java` SHA-256 `9e4aaf06d92808a9c2ec97ee6d28df3dfd71441acecb4b7bf66b008d0a2c93f5`이며 현재는 위의 최종 Card.tooltip 계약으로 갱신했다. 서버 작업 트리의 미공지 변경 대신 공개 hash와 일치하는 보존 사본을 동기화했다. `Panel.templateSummaries`는 같은 아이콘이어도 편제를 합치지 않고 서버 순서·개수·원문 툴팁을 유지한다. 왼쪽 클릭은 해당 `divisionIds`만 선택하고 Shift는 해당 집합만 전환한다. 편제 변경 버튼은 선택 사단이 있으면 `Open(ARMY_TOOLBAR,"DIVISIONS_TEMPLATE",[unit/id...])`으로 서버 후보 창을 요청한다. `MemberData.templateBlock`이 있는 사단을 하나라도 선택하면 버튼을 회색으로 그리며 사단 ID 순 첫 사유를 색·줄바꿈·단축키 문구까지 그대로 표시한다. 왼쪽·오른쪽·반복 클릭 모두 후보 요청을 보내지 않는다. 잠긴 사단을 선택에서 빼면 나머지 선택 사단만 후보 창에 보낸다. 선택이 없어도 회색이다. 서버의 최종 권한 판정은 그대로 유지한다.

`MemberData.orderWarning`과 `orderTooltip`은 원본 `unitview.gui`의 (23,4) 위치에서 NO_ORDERS/INVALID_ORDERS 그림과 원문을 표시한다. 적응 그림은 (140,16),12×12px이며 값이 없거나 둘 다 0이면 숨긴다. 설치 실행 파일 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5`의 `0x141695225`가 `unitlist_acclimatization` 포인터를 +0xc0에 저장한다. `0x1416b959f..0x1416b95fc`는 `0x140614bc0`으로 양수 최댓값의 첫 적응 종류를 찾고, 없으면 숨기며 `0x14169b310`의 프레임을 사용한다. 프레임 함수는 종류 인덱스×2+1(원본 1-based)에 적응값≥100000이면 1을 더한다. 공개 `common/acclimatation.txt`의 cold→hot 순서 및 DDS 네 프레임과 대조한 클라이언트 0-based 규칙은 추위 0/1, 더위 2/3이며 수신 비율≥1에서 완성 그림이다. 동률은 추위를 우선한다. 재현 명령은 `llvm-objdump -d --no-show-raw-insn --start-address=0x14169b310 --stop-address=0x14169b3f3 <hoi4.exe>` 및 `--start-address=0x140614bc0 --stop-address=0x140614c3d`, `--start-address=0x1416b959f --stop-address=0x1416b95fe`이다. 날씨별 증가·불이익 계산과 상세 적응 툴팁은 이 표시 프레임 분석에 포함되지 않는다.

사단 자동 그림의 정적 분석은 설치 실행 파일 SHA-256 `7dc947be34970da1e1c787bcddbe7f62b610ff258aebf8f06aa8b348f3a031d5` 기준이다. `priority` 토큰 0x8d는 파서 `0x14101f253` → `0x14142cfee`에서 병과 객체 +0x60으로 연결되고, `ai_priority`(+0x64)는 별도다. `0x140ba41f0`은 `regiments`(+0x48) → `support`(+0x68) → `regimental_support`(+0x88)의 유효 병과를 `0x140b97cd0`으로 처음 등장한 순서대로 집계한다. `0x140ba45b2..0x140ba462b`는 `개수 × priority`의 양수 최댓값을 template+0x190에 저장하고 동률은 앞 항목을 유지한다. 실제 사단 목록 `0x1416b913f..0x1416b91b6`이 getter `0x140b9f5c0`의 이 값을 그림 함수 `0x140b458f0`으로 전달한다. 전달된 수동 그림 ID의 조회가 성공하면 그 그림을 우선하며, 자동 키는 `GFX_unit_` + 병과 이름 + `_icon_medium`이다. 수동 ID는 표시 설정에 따라 -1로 바뀌기도 한다. 편제 파서 `0x140ba29f0`의 토큰 0x2fa3/0x2f9c/0x41e7과 등록 문자열을 연결해 이 그리드 이름과 집계 순서를 확인했다. 지원 두 종류도 집계에 포함된다. 집계 이전 연대 지원 정리 정책·표시 설정 값의 의미와 원본 게임 실행 대조는 미확정이다. 이 클라이언트 작업에서는 서버 병과 집계/그림 규칙을 변경하지 않았다. 서버 반영은 담당자의 원본 정의·편제 순서 검토 범위이며, 클라이언트에서 별도로 다시 계산하지 않는다. 재현은 `llvm-objdump -d --no-show-raw-insn --start-address=0x140ba41f0 --stop-address=0x140ba47a9 <hoi4.exe>`와 위 getter·소비 함수 구간을 사용한다. 전체 토큰/주소 연결과 미확정 범위는 CMP-34의 `division-icon-evidence` 문서에 기록한다.

`MapCommanderTheaters`는 `View.theaters`와 원본 `theatreselector.gui`의 `theater_group_item`/`army_group_box`/`new_theater_group_item`을 사용한다. 이름·총 사단·군/집단군 구성·군별 경고는 서버값이다. 전구 배정 줄의 왼쪽 클릭은 소속 전체를 선택하고, 우클릭은 선택한 군/집단군과 `target/theater/N`으로 `FORMATION/THEATER_ASSIGN`을 요청한다. 새 전구는 선택한 편성을 `FORMATION/THEATER_CREATE`로 보낸다. 버튼은 20261017 원본 214413·214434·214437과 220659~220755의 관찰에 따라 집단군 소속 군을 선택했을 때만 표시한다. 집단군·전구 직속 군 선택에서는 숨긴다. 이 조건은 원본 엔진에서 확인한 조건이 아니라 사용자 제공 화면의 관찰에 따른다. 응답 전에는 전구 구성을 변경하지 않는다. 설정은 `THEATER_SETTINGS/<우선권>` 계약에 따라 로컬 창을 열고, 삭제 확인 요청은 이 창에만 배치한다. 전투 기록 및 공격전/방어전 지표는 클릭 실행 기능이 아니다. 전투 수는 1 이상일 때만 기본 글자 크기로 표시하고 0·미상에서는 숫자를 숨긴다. 현재 표시 공간 밖의 전구는 휠로 이동한다. 편제 요약은 기존 머리줄의 11칸 한도를 유지하므로 그 이상의 편제 표시와 전구 내 많은 군의 겹침/접기 원작 동작은 추가 대조가 필요하다.

전구 배정 줄은 TFR `assign_group_button.dds`의 첫 299×35 영역을 기존 atlas에서 직접 그린다. DDS SHA-256은 `ed104f93190b9cbf85590749a486214875dfe6e39dc7af88e3ecef2543a0f263`이다. 유효 `theatreselector.gfx:98`은 4프레임을 선언하지만 실제 DDS는 897×35이고 299px 간격의 온전한 버튼 그림 세 개가 있다. 첫 영역의 올리브 배경은 사용자 원본의 배정 줄과 대조해 선택했으며, 모든 상태의 엔진 프레임 선택 규칙을 확정한 것은 아니다. 원본 선언·atlas·출처는 변경하지 않는다. 하위 군 줄과 새 전구 버튼을 원본 높이에 맞추되 칸 안 배지·경고·숫자의 사용자 지정 1GUI 아래 오프셋은 유지한다. 전구 생성/배정의 서버 집단군 이동 근사 규칙을 원작 엔진 동작으로 해석하지 않는다. 군·집단군의 수송 캡슐은 같은 폭으로 넓혀 숫자 오른쪽 여백을 확보하고, 특성 12개가 겹치는 경우에도 훈장 크기·순서·개별 툴팁을 유지하며 오른쪽 여백 안으로 겹침 폭을 제한한다.

전구·편제 추가 계약의 집중 검증은 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r4-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r4-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 3m 57s에 통과했다. PNG205장, 클라이언트122·protocol119·r4 생산자46개, 명령 importer8개는 실패·오류·건너뜀0이다. 변경 없는 성공 결과는 재사용했다. 두 해상도 GUI2에서 같은 그림의 다른 편제 선택, 머리줄을 유지한 연속 Shift 전환, 빈 선택의 비활성 버튼과 닫기, 서버 원문 툴팁, 전구 우클릭/반복 요청·전체 선택·생성·32번째 전구 스크롤을 검사한다. 새 계약 장면의 두 사단 이름은 기존 TFRKP OOB fixture에서 가져오고 편제·경고·적응·전구 상태는 합성이다. 초기 테스트의 마우스 숫자 가정은 Minecraft26.3의 LEFT=1/RIGHT=3 상수로 고쳤으며 실패 로그를 보존했다. 고정 생산자는 이전 서버 구현에 공지된 e4a2c3f2 프로토콜을 합친 것이므로 최신 서버의 실제 소켓·저장·재접속·멀티·직접 청취를 검증하지 않는다. 전체 클라이언트 검사와 가 전체 원본 일치는 미완료다.

편제 잠금과 4차 계약의 집중 검증은 Java25 `test runGameTest remapJar -PhoiTestSuite=army-layout -PhoiServerSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r5-server/HOI -PhoiProtocolSource=C:/Users/Kiruy/AppData/Local/claude-hoi/cmp33-r5-server/HOI/protocol/src/main/java --console=plain --no-daemon`으로 7m 5s에 통과했다. PNG207장, 클라이언트122·protocol119·r5 생산자46개에서 실패·오류·건너뜀0이다. 잠긴/허용 사단 혼합 선택의 좌우·반복 무전송, 원문 사유 보존, 잠긴 사단 제외 뒤 재활성화를 두 해상도 GUI2로 검사했다. HQ 실제 배치는 서버 담당자의 명시적 사 단계 이관에 따라 표시/툴팁만 제공한다. 당시 미노출이던 전구 설정·삭제는 현재 위의 최신 계약으로 연결되어 있다. 4차 protocol의 abilityDetails와 PlanOverlay.plans/v3는 동기화됐지만 해당 표시·입력은 자 단계 작업이며 구현 완료가 아니다. r5는 이전 격리 서버 소스에 공지 protocol만 합쳤으므로 최신 서버 게임 동작·실제 소켓·저장/재접속·멀티·직접 청취 검증을 대체하지 않는다. 전체 클라이언트 검증은 아직 수행하지 않았다.

카드·전구 전체 툴팁은 약력과 별개라는 사용자 답변을 적용한다. `----` 같은 구분선은 기존 `sourceSpacing` 경로에서 빈 줄로 표시하며 원문 저장값은 고치지 않는다. 기술·경험 및 원수 부분 혜택 전체 블록은 최종 Card.tooltip 계약으로 연결했다. 카드·전구는 선택 안내·이름·병과·설명 뒤에 이 블록을 붙이고, 초상용 Card.detail 약력은 기존 초상 툴팁에 보존한다. 최종 검증 완료 여부는 위 검증 상태를 따른다. 병과 툴팁 글리프는 기존 원본 그림만 재사용하고 국기·부대 상태 그림을 불필요하게 등록하지 않는다. 가져오기 결과가 클라이언트의 262,144바이트 제한을 넘으면 빌드 전에 거절한다.

### 함대 원본 배치와 구형 카드 순서

해군 카드 줄은 원본 `20261004213252_1`·`20261004213751_1`·`20261004213243_1` 대조에 따라 기존 위치보다3.5GUI 아래에 그린다. 임무 막대와 기동부대 띠의 그림 위치는 유지한다. 카드 그림의 소수 좌표는 보존하고 정수 클릭 상자는 안쪽 경계에서 시작해 `cardsBottom`에서 자른다. 일시정지·구형 snapshot에서는 바닐라 HUD 예약 영역에도 그림과 클릭 상자가 들어가지 않는다. 기동부대 띠의 입력 영역은 배경 기준 y3~20GUI이며 카드와 겹치지 않는다. 원본 2560×1440의 새 함대 칸 틀 위 끝은1331±1px, + 그림은1363~1391px이며 띠 아래 밝은 선은1327~1328px이다. `MapCommanderPanelsTest.assertCardLayout`은 해군 명령 줄에서 카드까지67.5GUI와 모든 패널 hit 비겹침을 검사하고, `navy-layout`도 실행 중·일시정지·구형 snapshot의 같은 검사를 수행한다. 육군·공군 카드 위치는 이 보정에 포함하지 않는다.

이 보정의 두 해상도 실제 렌더에서 새 함대 칸 위 끝은2560의1331px·1280의611px이며 띠 아래 밝은 선은각각1328px·608px이다. 공통 `UiUnits`의 최소 배율1 때문에 GUI2의 카드 원본 그림 크기는1280에서도 동일하며 화면 높이가720px 줄어든 만큼 좌표가 달라진다. 따라서1280의 목표를2560 좌표의 절반인665~666px로 잡으면 기존 크기 유지와 일치하지 않는다. 카드의3.5GUI 이동은 양쪽 모두7px이며, + 그림의 녹색 픽셀 경계는2560에서1362~1390px·1280에서642~670px이다(녹색 판정 기준과 외곽 그림 경계를 구분한다). GUI2 두 해상도에서는 화면 아래 끝에서 같은 거리로 배치하는 이 좌표를 기준으로 삼는다. 1280의611px·608px와2560의1331px·1328px를 유지하며 + 그림을 추가 보정하지 않는다. 원본 `20261004213252_1`에 있는 “활동 중인 지역: 0” 표시도 유지한다. 집중 `navy-layout`의 실행 중·일시정지·구형 상태 클릭 경계 검증과 필터 없는 전체 검증은 별개다.

자동 분리 수리의 개별 단추는 `autoSplit=true`에서만 원본 `nv_btn_toggle_split_for_repair/frame_0`의 초록 불을 켜고, false·미상은 `frame_1`의 꺼진 그림을 쓴다. 함대와 기동부대의 함종 칸은 `FleetData.hullAir`·`NavalData.hullAir`에 키가 있을 때만 함재기 둘째 줄을 그린다. 값은 서버의 탑재기 수/갑판 크기 비율이며 0은 빈 막대, 누락은 줄 없음이다. 기존 `GFX_navieslist_bar_planes`와 `GFX_NV_XS_plane_icon`(`gfx/interface/naviesview/xs_plane_icon.dds`, 15×6)을 재사용한다. 원본 `ship_entry_compact`의 bar_planes y24·plane_icon y27 순서를 따라 기존 함종 막대 아래에 폭9.5GUI 막대와 가운데7.5×3GUI 비행기 그림을 놓는다. 기동부대에도 같은 위젯을 적용하는 것은 리뷰 계약이며 해당 원본 항모 기동부대 사진을 확인한 결과는 아니다.

`MapNavalContractTest`는 항모 입력의 빈 함재기 비율과 비항모의 키 부재를 검사한다. 별도 `hoi-navy-synthetic-air-split-*` 장면은 자동 분리 켜짐/꺼짐/미상과 함재기0.5/0/누락 표시를 위한 합성 응답이며 실제 보유량이나 서버 상태 변경을 뜻하지 않는다.

함대 상세 행은 설치된 TFR `naviesview.gui`의 `taskforce_view_entry` 좌표를 기준으로 첫 줄에 함선 수·왼쪽 정렬 이름·소속 함대 배지·선택·X, 둘째 줄에 원본 임무 원형 그림과 상태·자동 분리 수리·수리 우선순위·교전 설정, 셋째 줄에 함종별 수와 편성 설정을 놓는다. 증원 설정은 전체 도구 줄에서 선택 기동부대에 일괄 적용한다. 원본에 없는 세로 상태 막대·셋째 줄 추가 증원 단추·임무 막대 X는 표시하지 않는다. 함종 수는 현재 수와 `NavalData.targets` 목표 수를 두 줄로 표시하며 목표가 없으면 현재 수를 반복한다. 이 기본값은 화면 계약의 결정이며 원작 엔진의 목표 계산을 추정하지 않는다. 선택/X/모두 선택은 클라이언트 선택만 변경하고 실제 함선이나 편성을 수정하지 않는다. 함대 이름은 서버의 영어 원문을 유지한다. 글자는 축소하지 않고 긴 이름을 사용 가능한 폭에서 말줄임한다.

아래 임무 막대는 원본 `action_bar_window`의 초계·타격부대·통상파괴 위 지배력 그림을 표시한다. 기동부대 띠는 전체 함선 수 닻 배지와 기동부대별 함선 수를 한 배경에 놓고, 폭을 넘을 때만 페이지 단추를 만든다. 함대 초상은 84×117 원본 프레임 비율을 유지하며 가로로 붙는다. 카드 순서 요청은 기존 `CARD_ORDER`를 사용한다. 구형 서버의 해군 카드 `role=null`도 해군 화면 문맥에서 순서 변경을 허용하며 명시적 `NAVY_HQ` 미배정 카드는 제외한다. 군·집단군의 소속 변경 경로는 그대로다.

함대 머리줄의 함종 수와 아래 띠의 전체 척수는 `Panel.divisionTypes`의 서버 전체 집계를 우선 사용한다. 이 집계가 없는 구형 snapshot은 상세 기동부대가 전부 포함된 경우에만 합산한다. 전체 집계가 없는 축약 snapshot은 부분 members 합계를 전체 수로 표시하지 않는다.

`Panel.fleet`의 활동 지역·모항·전체 척수·함종별 막대/툴팁과 `Member.data.navy`의 활동 문구·아이콘·경고를 표시한다. 모항과 자동 모항은 전달된 값·원문 툴팁을 표시하며 변경 요청은 서버 계약에 없다. 자동 모항 선택은 서버 미지원이다. 연료통과 항공기 그림은 원본 위치에 놓고 미제공 수치를 만들지 않는다. 함대 머리줄·기동부대·띠의 방패는 `Panel.color`로, 소속 함대 닻과 띠 첫 닻은 금색으로 표시한다. 휘장 단추는 색상 팔레트와 서버 휘장 선택으로 연결하며 `FLEET_COLOR`와 `FLEET_INSIGNIA`를 구별한다. 예비 함대는 서버 `NAVY_HQ` 카드와 원본 `reserve_portrait`/`reserve_frame`으로 표시하고 드래그 중에도 맨 뒤에 유지하며 `CARD_ORDER`에서 제외한다.

도구의 원본 15칸 배치는 유지하며 표시한 12개 도구는 `NAVAL_TB/*` 판정이 도착해야 사용할 수 있다. 서버 미지원 개장은 숨긴다. 새 기동부대·반 선택은 원본 GUI에 선언돼 있으나 엔진의 표시 조건을 확인하지 못해 숨기며 빈 칸과 나머지 좌표를 유지한다. 생성·분할·병합·예비 이동·해체는 서버 창으로 연결하고 해체는 확인창에서 확정한다. 자동 편성은 서버가 허용한 `BALANCE` 창으로 연결한다. 해상 재보급·개장은 서버의 미지원 사유를 유지한다. 증원·자동 분리·수리/교전 정책·항모 태세는 각 기동부대의 기대 상태를 순번과 함께 `NAVAL_SETTING/BATCH/*`로 보내며 새 revision 전 반복 요청을 억제한다. 수리/교전/태세 우클릭은 이전 단계다. 서버는 미설정 수리 정책을 실제 기본 동작인 0으로 발급하며 클라이언트는 `policy/0`부터 직접 순환한다. 구형 snapshot의 미상 정책에는 요청을 보내지 않는다. 절반 선택은 현재 기동부대 목록의 앞 절반(홀수는 올림)을 클라이언트에서 선택하는 계약상의 동작이며 개별 함선 절반 선택의 원작 동작과 같다고 주장하지 않는다.

해군 임무 `Decision.details`는 완전한 원문이므로 기존 설명에 덧붙이지 않고 그대로 표시한다. 경고 `Decision.warning`은 원본 노란 glow로 표시한다. 경고 아이콘·국기·활동 토큰과 단축키 키캡은 외부 팩의 원본 그림을 사용하며 툴팁 문자열·색·빈줄·순서를 보존한다. 키캡은 표시이며 기존 미등록 키 바인딩을 실제 사용 가능 단축키로 검증하지 않는다.

바다 hover는 현재 해군 지도에서 HUD 밖 커서 광선을 `SeaHover`로 보낸다. 위치·카메라·국가·차원·화면 변경은 이전 응답을 무효화한다. 응답이 없으면 3클라이언트 틱마다 마지막 위치를 다시 보내 서버의 2틱 제한으로 버려진 요청을 복구하고, 응답 후 정지 커서는 20틱마다 갱신한다. 빈 region/text는 툴팁을 지운다. `SeaHoverTooltip.text`의 지배율·현재 지배·초계 기본값과 추가 기여 줄은 서버값 그대로이며 클라이언트 계산을 하지 않는다. 기준 원본 세 장에 없는 최소 지배력 줄과 이 서버가 제공하지 않는 일수별 증감 줄은 넣지 않는다.

집중 재현은 팩 빌드 후 Java 25에서 `test runGameTest remapJar -PhoiTestSuite=navy-layout -PhoiServerSource=<검증 서버 사본> -PhoiProtocolSource=<같은 사본>/protocol/src/main/java --console=plain --no-daemon`이다. `armyHud` 생산자가 기존 입력과 `FabricNavalHudTest`의 임무 판정·초계/예비 함대·바다 툴팁 입력 및 출처를 생성하고 같은 프로토콜을 확인한다. `MapNavyLayoutTest`는 GUI2 2560×1440·1280×720의 실제 화면 선택·우클릭 무동작·선택 해제·모두 선택·초계 대상·함대 가로 드래그와 기존 수리 정책 반복 클릭을 검사한다. HUD/Context 주입·요청 관찰자 검증이므로 운영 접속·소켓·저장/재접속·멀티플레이·직접 음향 청취를 대신하지 않는다.

필터 없는 전체 재현은 같은 명령에서 `-PhoiTestSuite=navy-layout`을 생략한다. 이 실행에도 함대 레이아웃·계약 검사가 포함된다. 전체 실행은 `armyHud` 입력을 자동 생성하지 않으므로 `HOI_COMMANDER_PREVIEW`에 같은 서버 계약으로 생성한 전체 지휘관 fixture 폴더를 지정한다. 함대 기본·초계/예비·수송선 제외·항모 판정·바다 툴팁뿐 아니라 임명·제독·에이스·특성 입력도 함께 있어야 한다. `HOI_OFFICER_PREVIEW`는 성공한 서버 GameTest의 `officer-source-fixtures`로 지정한다. `HOI_LAND_PLAN_PREVIEW`는 서버의 `land-plan-overlay.json`으로 지정하고 같은 폴더의 `land-plan-preview.json`도 함께 보존한다. 지도 검사는 `resolveSibling`으로 초안 파일을 읽으므로 오버레이 파일만 복사하면 실패한다. `HOI_MISSION_OVERLAY_FIXTURES`는 서버의 `mission-overlay-source-fixtures`로 지정한다. 동시 작업이 있으면 이 네 입력을 별도 폴더로 복사하고 해시·원본 실행 경로를 보존하며, `-PhoiResourcePack=<고정 팩>/resources.zip`과 같은 폴더의 manifest·분할 ZIP을 함께 사용한다. 명령 성공과 별개로 입력 누락에 따른 건너뜀을 로그에서 확인한다.

`navy-layout`은 일반 `MapCommanderPanelsTest`의 해군 기본·수송선 제외 장면도 GUI2의 2560×1440·2560×1369·1280×720에서 실행한다. 이 검사는 `kor-navy-assessment.json`의 서버 판정을 주입하고 `naval_tool_SPLIT`·`naval_tool_MERGE` 아이콘 ID로 버튼을 찾아 원문 툴팁과 정확히 한 건의 `NAVAL_ORGANIZE` 요청, `force/` 범위와 `half` 부재를 확인한다. 숨긴 `CREATE`·`SELECT_HALF`·`UPGRADE`는 입력 영역도 없어야 한다. `MapCommanderDirectInputTest`는 개별 행의 증원 hit 부재와 같은 서버 판정의 원문 툴팁, `naval_tool_REINFORCE`의 정확히 한 건인 `NAVAL_SETTING/BATCH/REINFORCE` 요청(`id`, `0/state/<기대 상태>`)을 검사한다. 해군 상태가 없으면 자동 분리 수리·교전 규칙 두 설정과 9개 대상의 설명 11개를 확인하고, 서버가 허용한 증원 도구를 눌러도 빈 일괄 목록에서 요청·대화창·선택 변화가 없어야 한다. 이 직접 입력 검사는 `movement-controls` 집중 검증과 필터 없는 전체 검증에 포함된다.

같은 집중 실행의 `MapNavalContractTest`는 12개 표시 도구와 3개 숨김 도구의 입력 영역 부재, 서버 임무/활동/모항 툴팁, 미지원 도구 차단, 일괄 증원의 반복 클릭 억제, 교전 정책 우클릭, 해체 확인창 요청, 함대 색상 요청, 선택 변경 후 오래된 판정 거절, 예비 카드의 순서 제외, 바다 hover의 오래된 응답 거절·재시도·빈 응답 처리를 검사한다. 색상 선택 캡처는 커서를 팔레트 밖으로 옮겨 이전 툴팁이 가리지 않게 한다. `-PhoiResourcePack=<고정 팩>/resources.zip`을 함께 지정하고 같은 폴더의 manifest·분할 ZIP을 유지하면 동시 팩 변경을 피할 수 있다. 1280×720 GUI2에서는 기동부대 목록을 한 행씩 페이지로 넘기며 긴 초계 활동 문구는 칸 안에서 말줄임된다. 공통 상단 HUD 오른쪽 일부 값의 화면 밖 잘림도 남아 있다. 이 장면의 함대 단추·색상 팔레트·초계/바다 툴팁 경계 확인과 공통 HUD 전체 해상도 대응은 구분한다.

함대 머리줄의 빈 초상에는 원본 `CLICK_TO_ASSIGN` 문구를 겹쳐 표시한다. 함대 색 띠는 패널 오른쪽 바깥의 5.5×42.5GUI 영역이며 색상/휘장 선택 입력도 이 영역을 사용한다. 함종 그림은 지도용 작은 표식 대신 `GFX_unit_<함종>_icon_medium_white`, 막대는 원본 `navieslist_bar_str_compact` 위·`navieslist_bar_org_compact` 아래로 폭9.5GUI에 표시한다. 기동부대 수/목표 사이에는 원본 `NV_h_divider`를 놓는다. 활동 문구의 국기만 4.5×3GUI로 표시하고 문구 글꼴 크기와 툴팁 원문은 유지한다.

정박·할당 해역 없음·임무 수행·위치 유지의 원형 그림은 `NV_all_missions`이며 정박은 frame0 닻이다. 이동·수리·교전·분리 증원은 기존 `ship_activity_*`를 유지한다. 소속 함대 배지는 `army_shield_bg` 위 원본 해군 선택 휘장에 금색을 적용하고 하단 띠 첫 닻에도 같은 해군 그림을 쓴다. 서버의 구형 31 이상 휘장 ID는 기존 육군 휘장 자산으로 보존한다. 오른쪽 설정 단추 셋에는 `NV_btn_base_partial_toggle` 틀을 표시한다.

`NavalData.dominance`가 양수일 때만 지배 기여 아이콘 frame1을 쓰고 나머지는 frame0이다. 툴팁은 `dominanceTooltip` 그대로다. 연료통은 머리줄과 같은 frame0이며 단계별 프레임 규칙은 미확정이다. 값·설명은 `ClientCampaignHud`의 국가 연료를 읽는 `MapCommanderAirBases.fuelTooltip`을 재사용하고 기동부대 연료로 추정하지 않는다. 정박 임무의 선택 그림은 체크 삼각형인 frame1 대신 초록 불의 frame2이며, 임무 배경 중앙은 원본 그물 무늬를 늘이지 않고 타일링한다. 수리 단추는 원본 `btn_repair_now_small.dds` 64×31 중 첫32×31 셀을 사용한다. 두 번째 셀은 렌치와 빨간 X로, 방어 태세 스트립의 일부가 아니다.

계약4판의 기동부대 함종 막대는 `NavalData.hullStrength/hullOrganization`의 `unit_hud/navy_<함종>`별 실제 비율을 읽어 함대 머리줄과 같은 위 주황·아래 초록 배치로 채운다. 구형 snapshot의 누락 값은 미상으로 유지하며 전체 평균을 각 함종으로 복제하지 않는다. 334칸은 선택 중 `carrierStance`가 있는 항모 기동부대의 다수 태세를 frame0~4로 표시하며 동률은 서버에 발행한 선택 순서(ID 정렬)를 따른다. 항모 없는 기동부대도 일괄 요청에 `stance/none`으로 포함한다. 항모가 없으면 같은 칸에 수리 atlas의 오른쪽32×31 셀을 표시하고 `NAVAL_TB/REPAIR_CANCEL` 원문·판정과 `NAVAL_SETTING/REPAIR_CANCEL_BATCH`에 연결한다. 오른쪽 셀은 기존 `frame_0`64×31 안에 이미 보존되어 있어 별도 원본 추출은 하지 않는다. 비활성 도구는 공용 `UiAssets.drawGrayscale`의 원본 disabled 변환을 쓰며 단순 검은 덮개를 쓰지 않는다.

계약3판의 첫 집중 실행은 팩 빌드 후 Java25 `test runGameTest remapJar -PhoiTestSuite=navy-layout`으로4분19초 통과했다. 고정 서버는 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`, 새 팩 사본은 `C:/Users/Kiruy/AppData/Local/codex-hoi/cmp41-r3/pack/resources.zip`이다. client136·protocol124개의 실패/오류/건너뜀은0이며 protocol의 변경 없는 성공 결과를 재사용했다. 실제 GUI2 두 해상도22장을 만들고 초계 경고 설명 두 문단, 숨김3개 도구의 hit 영역 부재, 지배 기여/국가 연료 툴팁을 추가 검사했다. 이미지 열람에서 색 띠가 비율 유지 때문에 짧아지는 것을 찾아 원본44×88을 정상 비율로 그린 후 오른쪽5.5×42.5GUI만 노출하도록 보정했다. 이 기록은 함종 막대 입력의 인계나 필터 없는 전체 검증 완료를 뜻하지 않는다.

색 띠 보정 후 같은 함대 집중 명령은1분에 통과했다. 최종 실행은 GUI2 두 해상도22장을 생성했으며 외부 세로 색 띠와 초계 두 설명 문단을 직접 열람했다. 입력 생산자56개의 실패/오류/건너뜀은0이고 입력38개·공유 protocol48개 검증을 유지했다. 클라이언트 전용 staged JAR에는 게임 그림/음원 자산을 넣지 않았다.

계약4판 집중 검증은 같은 `navy-layout` 명령에 고정 서버 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`와 고정 팩 `C:/Users/Kiruy/AppData/Local/codex-hoi/cmp41-r4/pack/resources.zip`을 지정해 5분38초에 통과했다. 클라이언트137·protocol124·입력 생산자57개에서 실패/오류/건너뜀0이며 변경 없는 protocol 결과는 재사용했다. 입력39개·공유 protocol48개를 검증하고 실제 GUI2 두 해상도26장을 생성했다. 단위 검사는 태세 다수값·동률 순서·항모 없는 기동부대의 `stance/none` 보존을 확인한다. 렌더 검사는 1st Fleet의 수리 취소·항모 선택의 태세 전환·서버 원문 툴팁, 좌우 클릭·revision 전 반복 무전송을 확인한다. 개별 수리 검사는 실제 서버 fixture의 기본 `policy/0`에서 시작한다. 직접 열람한 두 해상도 함대/항모 장면에는 함종별 막대·수리 취소·균형 저울·비활성 회색조·수리0 프레임이 표시된다. 긴 항모 함대/기동부대 이름은 기존 칸 안에서 말줄임된다. staged JAR는 client 진입점이며 내장 protocol을 포함해 게임 그림/음원은 없다. 이 결과는 서버 응답 주입과 요청 관찰자를 이용한 검증이며 실제 항모 전투 효과·수리 취소 상태 변화·저장/재접속·운영 소켓·멀티플레이·직접 음향 청취와 필터 없는 전체 검증을 대신하지 않는다.

### 지도 모드의 카드별 부대 선택

해군의 임무 막대·기동부대 띠와 해당 입력 영역만 기존 위치에서 1GUI 위로 이동한다. 함대 카드 본체·클릭 영역은 이동 전 위치를 유지해 카드와 위 띠 사이를 넓힌다. 이미 원본 중심에 맞춘 카드 숫자의 세로 위치는 별도로 유지한다. 지도 지휘 UI의 최소 배율1 때문에 GUI2의2560×1440·1280×720 모두 위 두 칸의 이동량은2px다. 함대 카드를 누르면 해당 함대의 기동부대만 목록·띠·임무 대상에 포함되고 미선택 때는 숨긴다. Shift를 이용한 명시적인 여러 카드 선택은 유지한다.

공군 지도에서 군·집단군 카드의 본문과 하단 숫자 칸은 같은 카드 선택 요청을 사용하고, 역할 없는 비행단 카드의 하단은 `AIR_MISSION` 요청을 유지한다. 군 카드 하단은 실제 카드 선택에 맞게 군 카드 원문 툴팁을 표시한다.

공군 지도 모드의 아래 군·집단군 카드는 육군 카드다. 군은 `Member.data().attachedArmy()`가 군 id와 같은 비행단, 집단군은 소속 군 id에 배속된 비행단을 선택한다. 군이 없는 비행단이나 다른 군의 비행단을 섞지 않는다. 카드 아래 숫자 영역도 같은 선택 경로를 사용하며 공군 모드에서는 육군 명령 막대·육군 부대 패널을 대신 열지 않는다. 카드 선택 중 서버 스냅샷이 갱신되면 현재 배속을 다시 반영한다. 선택하지 않았거나 배속된 비행단이 없으면 비행단 목록·공군 임무 막대를 숨긴다. 지도 표식·비행장·부속 비행단 버튼으로 직접 비행단을 고르는 기존 경로는 별도로 유지한다. 육군의 기존 선택 동작은 유지한다.

`navy-layout` 집중 묶음의 `MapCommanderCardScopeTest`는 서버가 생성한 `kor-command-panel-air-diagnostics`, `air-attached`, `air-detached` 입력으로 군 카드 좌클릭·반복 선택·임무 우클릭 무전송·임무 대상 범위·선택 해제·서버 배속 해제 갱신을 실제 렌더러에서 확인한다. `MapCommanderPanelOrderTest`는 여러 군과 집단군 소속 변경·중복 없는 비행단 필터를 검사한다. 해당 검사는 서버 스냅샷 주입과 요청 관찰자 검증이며 운영 접속·저장/재접속·멀티플레이 검증을 대신하지 않는다.

고정 서버 `C:/Users/Kiruy/AppData/Local/claude-hoi/cmp40-server/HOI`를 사용한 집중 검증은 클라이언트138개·protocol124개·입력 생성57개의 실패/오류/건너뜀0, 공유 protocol48개·fixture39개 검증과 GUI2 실제 렌더42장으로 통과했다. 당시 카드까지 함께 이동한 검사는 최신 지시의 적용 범위와 달랐으며 현재는 위 두 칸만 이동한다. 해당 검사에서 함대 교체 후 다른 함대의 기동부대가 제외됨을 확인했다. 공군의 군 카드 본문/하단 숫자 클릭·선택 해제·서버 배속 해제를 확인했다. 집단군의 복수 군 배속은 단위 검사이며 해당 서버 렌더 fixture는 아직 제공되지 않았다. 필터 없는 전체 실행·운영 접속·멀티플레이·직접 음향 청취는 수행하지 않았다. 첫 공군 렌더 검사는 존재하지 않는 버튼을 찾는 테스트 오류로 중단되었고, 실제 제공권 버튼으로 수정한 재실행이 통과했다.

### 함대 상세 패널과 공군 배속 칸

해군 제독 칸은 빨간 기술 배경을 기준으로 육군 `armyHeader`와 같은 기술 숫자(+1.75GUI), 능력 그림(+0.75GUI), 능력 숫자(+9.75GUI) 상대 높이를 사용한다. 기술 숫자의 추가1.5GUI 이동을 제거하고 기존 공용 글자 렌더러의 그림자와 크기를 유지한다. 능력치 검은 칸은 `fleet_bg`의 원본 구간을 사용해 숫자 영역까지 받친다. 특성은 원본 `traits_grid`의 패널 상대 (302,60)px에서 시작하고110px 안에서 `UiFlagStack`으로 겹치며, 마지막 특성의 캔버스는 상대 x412px를 넘지 않는다. 보이는 폭만 클릭·툴팁을 받는다. 서버가 배경 특성을 제외한 `traitDetails`를 보내며 클라이언트는 받은 순서를 그대로 그린다. `MapNavyLayoutTest`는 실제 제독, 특성7개 합성 입력, 육군 비교 장면을 GUI2 두 해상도로 캡처하고 특성별 좌우클릭과 툴팁 영역을 검사한다. 합성 입력은 렌더 검증용이며 저장된 특성을 바꾸지 않는다. 팩 빌드 후 `navy-layout` 최종 집중 검증은1분35초에 통과했고 PNG116장을 생성했다. GUI2 2560×1440·1280×720에서 기술 숫자 흰 픽셀은 x207~216/y135~148로 가로 중앙 오차−1px, 빨간 칸 대비 세로 위치는 육군과 같다. 능력치 숫자 흰 픽셀 y151~164는 검은 칸 아래 경계 y166 안에 있다. 첫 특성 금색 띠는 원본 x307/y138, 현재 x313/y138이며 패널 시작 x6을 빼면 상대 오차0px다. 7특성 장면의 마지막 변경 픽셀은 패널 상대 x408이다. 제안된 특성 높이+2.25GUI는 실제 원본보다4px 위로 나와 원본 정의 y60px를 채택했다. 앞4분54초 집중 결과는 이 마지막 특성 높이 보정 전 단계다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 수행하지 않았다.

함종 칸은 원본 `ship_entry_compact` 폭46px에 맞춰 첫 칸37.5GUI와 칸 내부 좌표를 유지하고23GUI씩 배치한다. 같은 고정 입력의 `navy-layout` 집중 검증은 입력 생산자 재실행을 포함해4분3초에 통과했다. GUI2 제독 유무·2560/1280에서 내구도와 조직력 막대의 시작 x는 각각90·136·182·228·274px(5종),90·136·182px(3종)이며 모든 간격이46px다. 좌표는0부터 센다. 첫 칸 위치와23×3px 막대 크기는 유지한다. 제독 없는 장면은 기존 서버 fixture에서 임무만 꺼짐으로 바꾼 합성 입력이며 운영 접속 검증이 아니다. 입력 재생성으로 제독이 달라질 수 있으므로 비교의 제독 이름·초상·능력치 차이는 간격 변경과 구분한다.

함대 카드의 X 그림은 원본16×16px이며 기존 버튼 자리와 클릭 범위를 유지한다. 예비 함대 초상은68×91px, 새 함대 배경·틀은61×83px·79×111px 원본 캔버스를 사용한다. 상세 패널은 `naviesview.gui`의 초상·틀, 왼쪽 정렬 계급/이름, 기술 수준 빨간 배경, 해군 능력 아이콘4종과 `UiFlagStack`의 전체 특성을 표시한다. 계급은 `leader.role()`이고 함종 간격은46px다. 함종 요약은 원본 `ships_compact`(75,95)와 `ship_entry_compact`의 막대(7,4)/(7,8), 그림(7,12), 수량(34,2)을 따른다. 내구도·조직력 막대는 TFR `naviesview.gfx`의23×3px이며 28×6 텍스처를 칸 전체에 매핑하고 서버 비율만큼 UV를 자른다. 비율 유지 이미지 축소로 막대 폭까지 줄이지 않는다. 항공기 막대는 원본30×3px다. 제독의 초록 +는 특성 배정 가능 상태다. `LEADER_TRAITS`·`LEADER_ADVISOR` 서버 control의 그림·툴팁·활성 여부·요청을 카드와 상세 패널에서 공유하며 계약이 없으면 상태를 추정하지 않는다.

함대 이름은 `fleet_bg` 상단 검은 이름판의 가로 중심을 유지한다. 이름과 머리줄 태스크포스 수는 같은 높이11.5GUI 칸으로 세로 중심을 맞춘 뒤 함께1GUI 올린 y=4GUI에 놓는다. 글꼴·크기를 유지하고 긴 이름은 가운데 정렬한 말줄임으로 표시한다. 이 요소별 상향 지시가 이전 이름판 세로 중앙 배치보다 우선한다. 머리줄 태스크포스 수는 최신 요소별 지시에 따라 기존 x=159GUI에서 x=160GUI로1GUI 오른쪽에 놓으며, 이름과 아래 카드의10/10 위치는 유지한다. 해군 임무 버튼은 선택한 기동부대 중 해당 임무를 수행하는 부대가 없으면 frame0, 하나라도 있으면 frame2를 사용한다. 혼합 임무에도 frame2를 쓰고 마우스 호버용 흰 사선 frame1을 일반 상태에 쓰지 않는다. 서버 판정상 비활성이면 선택 여부와 무관하게 frame0에 원본 buttonstate 회색 처리를 적용해 초록 상태 원과 활성 강조가 남지 않는다. 서버 이유와 요청 차단은 유지한다. 공군 임무 배경은 별도의 순서(frame0 켜짐, frame1 꺼짐)를 유지한다.

같은 고정 cmp50 서버·protocol·cmp41-r5 팩으로 상태 원 후속 `navy-layout` 집중 검증은1분22초·PNG106장으로 통과했다. GUI2 두 해상도에서 제독 유무 두 함대의 이름 본문 중심은(177.5,95.5)px, 검은 이름판 중심은(178.5,96.5)px로 각각1px 이내다. 함종별 내구도·조직력 막대의 실제 채움은23×3px다. 타격 임무 없음/일부/전부와 서버 비활성 주입 장면의 상태 원 초록 픽셀은0/64/64/0이며 일부와 전부의 단추 픽셀이 같다. 비활성 좌클릭·반복 클릭·우클릭은 요청을 보내지 않는다. 임무 상태 세 장면은 서버 fixture의 mission만 바꾼 합성 렌더이며 실제 임무 수행·저장 검증이 아니다. 최초 추가 캡처는 검사에서 폐기한 토큰을 재사용해 전부 비활성으로 표시했고, 새 토큰과 활성 응답 검사를 넣은 뒤 통과했다. 앞선 에이스 수정 후 필터 없는 전체 실행은 완료 기록 없이 중단되어 성공 결과로 세지 않는다. 운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이다.

공군 카드별 배속 칸은 TFR `leadergroups.gui/group_wing_type_box`의 `attached_air_bg`(39×45, scale .8)·`attached_air_types`를 사용한다. 군마다 원본 `ATTACH_AIR_WING_DESC` 또는 `ATTACH_AIR_WING_SELECT_SOME`를 표시하고 기존 `AIR_ATTACH` 요청의 `army/<id>`와 선택 wing id를 보낸다. 부속 비행단 칸은 좌클릭으로 해당 비행단만 선택하고 우클릭으로 `action/detach` 요청을 보낸다. 수량/상한/비율은 서버 수량과 target으로 계산하며 `AIRWING_ATTACHED`·`AIRWING_G_STR`·`ATTACH_AIR_WING_SELECT_DESC`의 순서를 사용한다. 요청만으로 배속을 로컬에서 확정하지 않는다. 집단군에 별도의 + 칸을 만드는 정의는 원본에서 확인되지 않아 군 카드에만 붙인다.

공군 상세 패널의 출처는 TFR `airselectionview.gui`다. 임무 칸은 훈련·제공·CAS·요격·전략폭격·목표 선택6개와 해상공격·가미카제·항구·보급로·수송·기뢰부설·소해·정찰·해상초계9개의 원본 배열이다. 원본 `tiled_plain_bg_small` 틀, 선택 도구 줄652×48, 비행장·비행단548×48 기준을 GUI2에서 같은 픽셀 비율로 사용한다. 공군의 선택 문구·기지·비행단 이름·숫자는 Minecraft 기본 글꼴의 기본 글자 크기를 사용하고 폭 때문에 계속 작아지지 않는다. 넘치는 이름은 `…`로 표시하고 전체 이름과 상태는 툴팁에서 확인한다. 이 공군 예외는 해군 글자 크기를 바꾸지 않는다.

비행단 줄의 `ace_btn`은 원본 `GFX_ace_none`42×46px만 표시한다. `nativeButton`을 사용해 별도 회색 틀을 제거하고 왼쪽 보충 정책의 원을 가리지 않는다. 원본 상대 위치(165,-3)px, 기존21×23GUI 클릭 영역·툴팁·해당 비행단의 `AIR_REINFORCEMENT/aces` 요청은 유지한다. `ace_kia`는 서버 상태가 없어 표시하지 않는다.

가미카제·전략폭격 목표 우선순위·공군 공격성·비행단 특화·빠른 배치 단축 버튼과 지역 전체 선택은 현재 지원 계약이 없는 기능이다. 원본 그림을 비활성으로 표시하고 툴팁에서 구현 제한을 구분한다. 선택 도구 줄의 출격 시간 변경은 기존 서버가 지원하는 단일 비행단에만 연결하며 여러 비행단을 한 번에 바꾸지 않는다. 원문 키 `AGGRESSIVE_CLICK_TO`·`DAY_NIGHT_CLICK_TO`·`QUICK_DEPLOY_SELECTION_WINDOW_OPEN_DESC`·`AIR_STRATEGIC_PRIORITIES`·`AIR_SELECT_ALL_REGION`는 번역 패치3411756715에서 검색한 뒤 해당 키가 없는 경우 원본 `localisation/korean/air_l_korean.yml`을 기준으로 한다. 가운데 큰 비행단 배치 창은 이 변경 범위에 포함하지 않는다.

카드별 배속 칸의 존재, 선택 wing id만 담은 배속 요청, 우클릭 해제 요청과 두 툴팁의 실제 렌더는 `MapCommanderCardScopeTest`가 두 해상도에서 검사한다. 비행단 미선택·직접 두 비행단 선택·군 카드 선택·미구현 임무 무전송도 확인한다. 현재 집중 검증은 지정된 cmp50 서버·protocol 및 cmp41-r5 팩을 사용한다. 갱신된 고정 서버에서 fixture를 재생성해 지휘관 특성 버튼·경험치·임무 상태를 검증하며, 필터 없는 전체 검증은 별도 지시 전 실행하지 않는다. 드래그·음향 청취·운영 접속·저장/재접속·실제 멀티플레이 검증은 이 집중 실행의 증거가 아니다.

갱신된 서버의 `equipmentIcon`은 생산 화면과 같은 `production/source/<hash>` 규칙이며 cmp41-r5 팩의 실제 사진을 사용한다. `missionState`·`missionStateTooltip`은 이름 아래 줄과 해당 호버에 표시하고, 경험치 단계·진행률은 `MemberData.experienceLevel/experienceProgress`를 그대로 사용한다. 둥근 위쪽 표시는 비행단이 속한 `View.theaters()`의 교체 우선순위 LOW/NORMAL/HIGH를 `deploy_priority` frame1/2/3으로 그린다. 전구 값이 없으면 임의의 NORMAL을 채우지 않는다. 아래쪽 보충 정책 표시는 별개다.

`air_mission_efficiency_strip`은 원본 6프레임 중 시계가 없는 빨강·노랑·초록 0/1/2를 사용한다. 서버 efficiency 0~1을 `[0,1/3)`, `[1/3,2/3)`, `[2/3,1]`로 나누는 표시 규칙이다. 이는 원본 엔진의 공개되지 않은 임계값 재현이 아닌 클라이언트 시각 매핑이며, 서버 계산이나 상세 툴팁의 효율 값은 바꾸지 않는다. 시계가 붙은 3/4/5는 의미가 확정되지 않아 사용하지 않는다. 원본 213605 카드 아래 흰 비행기와 두 도구 줄 사이 삼각형은 지도 요소로 확인되어 카드·도구 줄에 그리지 않는다.

선택 줄 문구는 현재 공군 글자 크기로 전체 폭을 측정해 말줄임 없이 표시한다. 왼쪽 단추 네 개만 글 끝 뒤로 옮기고 오른쪽 단추는 고정한다. 비행장 국기는 9×7 GUI 칸을 원본처럼 채우며 GUI2의 실제 픽셀 x22~39/y216~229를 원본과 비교한다. 임무·선택 줄은 `GFX_tiled_plain_bg_small`의 16px 테두리와 중앙 타일 반복을 사용한다. 임무 단추는 각 `air_mission_entry_N`의 일반/체크박스 배경을 사용한다. 행동 줄은 그 위 (27,7)px에 `GFX_unit_list_header_air`(471×35px)를 더한다. 캡처의 RGB(40,42,39)는 이 띠의 색이며 바탕 `tiled_plain_bg_small`의 RGB(26,26,26)를 임의로 보정하지 않는다. 일반 임무 배경의 네 프레임은 켜짐·꺼짐·켜짐 호버·꺼짐 호버이며 비활성 단추는 원본 `buttonstate` 회색 변환을 사용한다.

공군 임무 가능 여부는 현재 선택·세션·리비전에 맞는 서버 `AIR/<임무>` 판정만 따른다. 비활성 상태에는 회색 원본 그림과 서버 reason을 표시하고 요청을 보내지 않는다. 회신 대기와 누락된 AIR 판정도 비활성이다. 클라이언트가 기종·설계 모듈을 추측해 재판정하지 않는다. 혼합 선택은 정확한 전체 선택 목록을 서버에 보내고 실제 적용 대상 필터링은 서버가 맡는다. 지역 없는 비행단은 서버 pending 응답 뒤 기존 지도 공역 선택으로 이어진다.

갱신된 고정 서버·protocol과 cmp41-r5 팩을 사용한 현재 `navy-layout` 집중 검증은 1분22초에 통과했고 실제 PNG98장을 남겼다. 클라이언트138·protocol124·fixture 생산자64개는 실패/오류/건너뜀0이며 변경 없는 검사 결과는 재사용했다. 당시 GUI2 두 해상도의 기술 숫자 본문은 x207~216/y140~153으로 빨간 칸 중심에 있었으나, 이후 육군과 같은 상대 배치 지시에 따라 아래의 제독 칸 규칙으로 대체했다. 카드 `10/10` 본문은 y1421~1434 및 y701~714를 유지한다. 10개 기동부대의 띠 간격은29px, 전구 줄은25px로 각 줄 안에서 일정하다. 닻 그림 왼쪽 여백/마지막 숫자 오른쪽 여백은 띠7/15px, 전구5/14px다. 단일·혼합 비행단 선택의 서버 판정 응답을 주입해 회색 비활성 상태·reason·반복 클릭 무전송, 활성 요청의 정확한 선택 목록, 지역 없는 비행단의 pending 응답 후 공역 선택 전환을 확인했다. 실제 서버의 무장 계산이나 멀티플레이를 대신하는 검사는 아니다. 제독 특성·고문 버튼도 서버의 요청을 그대로 보내는지 검사했다. staged JAR는 client 진입점만 가지며 내장 게임 그림·음원은 없고 허용된 mod-list icon만 포함한다. 수동 입력을 갱신하기 전의 필터 없는 전체 검증은 공군 카드 검사에서 입력 계약 불일치로 실패했다. 공군 부속 칸과 카드 줄 생성 단추 부재, 육군의 기존 배속 띠를 함께 재현하는 집중 범위는 `-PhoiTestSuite=movement-controls`다. 비행장 머리줄 생성 요청은 `MapAirBasePanelsTest`가 검증한다. 집중 실행은 `armyHud` 생산자로 `HOI_COMMANDER_PREVIEW`를 생성 입력 경로로 바꾸지만, `armyHud.suites`에 `all`이 없어 필터 없는 실행은 지정한 수동 경로를 유지한다. 이전 cmp50 수동 폴더의 `kor-command-panel-air-diagnostics.json`·`air-attached`·`air-detached`·`air-theaters`에는 `experienceLevel`·`experienceProgress`·`missionState`·`efficiency`가 빠져 전체 실행이 실패했다. 갱신된 고정 폴더64개는 전달된 SHA-256 목록과 일치하고 네 입력의 모든 비행단에 해당 필드가 있다. 전체 실행의 수동 경로와 Gradle 연결은 유지한다. 입력 해시 일치와 집중 검증 통과만으로 전체 입력의 최신 계약을 보장하지 않는다. 필수 표시 필드 검사는 유지하며, 아래 최종 전체 검증에서 갱신된 수동 입력으로 해당 검사와 후속 장면까지 통과했다. 직접 음향 청취와 운영 접속은 수행하지 않았다.

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

상세 기술 칸은 TFR `interface/unitview.gui:556–572`와 `interface/naviesview.gui:1116–1132`의 `GFX_land_battle_leader_skill_bg`0.7배를 따른다. 능력 그림은 기존 크기를 유지한다. 검은 바탕은 외부 `army_bg`/`fleet_bg` 원본 영역을 사용해 육군98×34px·해군96×34px로 받친다. 기술 숫자는 기존 Minecraft 글꼴14px를 유지하고 실제 글리프의 가로 중앙에 놓으며, 재측정 지시에 따라 기존 위치보다2px 위인 칸 위+5~+18px로 옮긴다. 능력치 숫자는 원본9px 재측정에 따라 GUI2에서 기본 글꼴의9/14배(기존7px 대비9/7배)로 최근접 표본 렌더링한다. 글자 시작점은 실제 화면의 정수 픽셀에 둔다. 해군은 칸 위+24~+32px, 육군은+22~+30px를 목표로 하며 다른 글자와 플레이어 설정은 바꾸지 않는다. 특성 줄은 기존 위치보다 육군14px·해군5px 왼쪽으로 옮기고 세로 위치·가용 폭·겹침 간격을 유지한다. 서버에서 배경 특성을 제외한 traitDetails를0번부터 연속 배치해 숨은 배경 특성의 빈 슬롯을 남기지 않는다. 원본 비교는 `20261004213231_1.jpg`와 `20261009015126_1.jpg`이며 실제 제독·장군 및 특성7개 합성 입력은 MapNavyLayoutTest의 두 해상도 캡처를 사용한다. 합성 특성은 저장된 상태와 구분한다. 아래7px 측정 기록은 재측정 이전 단계다.

`MapCommanderTheaterCardsTest`를 `navy-layout` 집중 묶음에 연결했다. 새 서버 생산 입력 `kor-command-panel-army-two-theaters`·`kor-command-panel-air-army-theaters`의 육군 전구2 군 줄 선택→공군 전환·전구1 복귀와 배속 비행단 지도 선택의 전구 전환을 GUI2 2560×1440·1280×720에서 확인한다. 요격기60/지원기20 분리, 묶음별 툴팁·좌클릭 선택·우클릭 해제, 선택 중에만 + 표시, 전략폭격기·정찰기 혼합 선택의 반복 + 클릭·군 카드 우클릭 무전송을 검사한다. 역할8개를 바꾼 합성 입력은 허용된 요격기80/지원기104만 배속하며 저장된 기종을 바꾸지 않는다. 수송기 혼합 선택도 금지 툴팁·반복 클릭 무전송으로 검사한다. 수량 합1200·9999·10000·1234·12345의 두 해상도 장면에서 숫자 표시 경계와 원본 툴팁/비행단 범위 보존을 검사한다. 육군·공군 드래그는 숨은 형제 카드를 만들기 위해 테스트 사본의 군 parent만 비운 합성 입력을 쓰고 해군은 기존2전구 입력을 쓴다. 세 군종 모두 실제 화면의 누르기·끌기·놓기로 변경된 전체 `CARD_ORDER`를 관찰하고 숨은 카드의 인덱스가 유지되는지 검사한다.

팩 빌드 후 고정 cmp50 서버/protocol·cmp41-r5 팩의 최종 집중 `test runGameTest remapJar -PhoiTestSuite=navy-layout`는2분33초에 통과했다. PNG148장, client140·protocol124·fixture 생산자66개는 실패/오류/건너뜀0이며 변경 없는 성공 결과는 재사용했다. 생성 입력44개와 공유 protocol48개를 확인했고 최종 실행 중 Java/fixture 설정352개 해시는 변하지 않았다. 실제 두 해상도에서60/20과 합성80/104/40/42를 열람했으며 그림 칸의 화면 간격은27px다. 소수 좌표를 유지해 정수 GUI 반올림으로 간격이28/26px로 번갈아 바뀌지 않게 한다. 초기 컴파일의 blit 정수 좌표 오류와 새 검사에 사용한 구버전 마우스 버튼 번호 오류는 수정 후 재검증했다. 이26.3 빌드의 InputConstants 좌/우클릭은1/3이며 숫자를 직접 쓰지 않는다. staged JAR는 client 진입점만 포함하고 허용된 모드 아이콘 외 게임 그림/음원은 없다. 채워진 원작 배속 화면, GUI2 외 렌더, 필터 없는 전체 검증·운영 접속·저장/재접속·멀티플레이·직접 음향 청취는 미수행이며 전체 검증은 Claude 검토 후 별도 지시를 따른다.
재측정 보정 뒤 같은 집중 명령은1분50초에 통과했고 PNG148장을 남겼다. GUI2 두 해상도에서 육군 능력치 본문은y136~144(칸 위+22~+30), 해군은y152~160(+24~+32)로 모두9px다. 기술 본문은 육군y119~132·해군y133~146으로14px이며 각각 칸 위+5~+18이다. 육군 검은 칸98×34px·해군96×34px, 특성 줄 좌측 이동14px·5px를 적용했고 숫자는 검은 칸 안에서 그림과 분리된다. 실행 중 Java/fixture 설정 해시는 불변이었다. 두 해군 전구에 보이는 같은 지구본 그림은 동일 카드의 중복이 아니라 전구1의mil_1·전구2의mil_2라는 서로 다른 예비 함대다. 표시 이름과 그림이 같으며 서버 소속을 그대로 따른다.
