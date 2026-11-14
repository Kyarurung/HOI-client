# HOI-client

Minecraft 26.3 · Fabric · Java 25용 HOI 전용 클라이언트 모드입니다. 국가 메뉴·연구·생산·장교단·교리·군종 작전과 지도 정보를 표시합니다. 게임 계산과 저장은 HOI 서버가 담당하고, UI·초상화·음향은 외부 리소스팩에서 읽습니다.

공통 장기 목표와 완료 기준은 [HOI 프로젝트 목표](../HOI/GOAL.md)를 따릅니다. 목표는 현재 구현 완료 상태와 구분합니다.

육군 집단군·군의 원본 카드 순서와 펼침 패널은 [육군 지휘 패널 원본 배치](docs/ui.md#육군-지휘-패널-원본-배치)를 따릅니다. 집중 검사는 `-PhoiTestSuite=army-layout`입니다.

카드·전구 전체 툴팁은 서버 Card.tooltip에 연결했고 상단 HUD는 배경 특성을 제외한 서버 traitDetails를 그대로 표시한다. 기술 상한9·네 능력치 상한10과 경험 막대는 서버 표시값을 따른다. 정렬은 ▼9칸과 사단 행을 같은 열에 맞추고 추가 폭을 이름 칸에만 배분한다. 지휘관 능력치의 어두운 바탕을 고정하고 오른쪽 밝은 영역에서 서버 순서의 특성 훈장을 표시한다. 넘침은 외교 국기 줄과 같은 간격/호버 규칙을 사용하며 크기를 줄이거나 뒤 항목을 생략하지 않는다.

육군 머리줄 숫자는 군 사단 수를 y=4GUI, 집단군 군 수/한도(예: 3/5)를 y=3GUI에 둔다.

육군 선택 도구줄의 원본 unit_list_header 배경은 고정196GUI 대신 현재 패널의 안쪽 폭을 채운다. 병과 요약 때문에 늘어난 폭은 선택 수 글자 영역에 주고, 오른쪽 전략적 재배치·통폐합·수송·절반 선택·정지·배정 해제 버튼과 호버는 같은 폭만큼 이동한다. 미지정 목록도 같은 배경 폭 규칙을 사용한다. 특성/훈장은 어두운 능력치 칸에서 첫 훈장까지의 여백을 오른쪽 끝에도 확보하고 기존 크기·서버 순서·넘침 겹침/호버를 유지한다. 선택 사단 수 글자만 기존 위치보다2GUI 아래·1GUI 왼쪽으로 옮긴다. 전구 이름은 가로 위치를 유지하고3GUI 위로 옮겨 기존 이름 칸 안에 맞춘다. 배경·버튼에는 이 글자 오프셋을 적용하지 않는다.

오른쪽 전구 본문의 집단군·군 배지, 선택 강조, 숫자, 경고 아이콘과 클릭/호버 영역은 1GUI 내리고 전구 전체 사단 수도 함께 내린다. 전구 머리줄·설정 창·본문 배경과 HQ·특성·▼ 좌표는 유지한다.

머리줄은 군/집단군 수 캡슐의 높이를 유지하면서 원본 가로 폭을 복원하고, 군 소속 집단군 번호의 월계수에 집단군과 같은 금색을 쓴다. 군·집단군 카드 오른쪽 끝을 맞춘다. 특성/호버 높이와 ▼ 중앙 정렬은 유지한다. 서버의 기본 군 색(빨강→청록→보라)을 사용한다. 이 색 배정은 서버의 근사 규칙이며 원본 엔진 규칙을 확정한 결과가 아니다.

▼9개는 각각의 열 가운데 배치한다. 칸 경계·아이콘 크기·HQ 위치와 훈장의 서버 순서/겹침 규칙은 유지한다.

HQ 버튼은 배경·테두리·호버만 2GUI 내려 배치 줄의 위아래 가운데에 두고 아이콘·텍스트 위치는 유지한다.

HQ 표시/비용·군 차량화·이동 중 남은 날의 첫 서버 계약을 연결했습니다. 군 카드 X는 확인창 없이 해당 군의 해체를 즉시 요청하며, 서버 응답 뒤 카드가 사라지고 소속 사단은 미지정으로 보존됩니다. 빨간 휴지통은 선택 사단 삭제 확인창으로 연결했습니다. 다음 계약의 편제 변경 후보 요청, 편제별 툴팁/선택, 사단 경고/적응, 전구 표시·선택·배정·생성을 연결했습니다. 실제 HQ 배치 실행은 계약 대기입니다. 엔진의 기본 색 배정·shader·자동 편제 그림 규칙도 미확정입니다. 수신 전 색·국가 상태를 바꾸지 않으며 미제공 값은 임의로 채우지 않습니다. 수송 중량 누락만 사용자 지시에 따라 0으로 표시합니다. 국가와 지도가 유효한 동안 일시 정지·이벤트 창 여부와 관계없이 바닐라 HUD를 숨기며 카드 줄을 화면 아래에 붙입니다. 재현·원본 키/주소·합성 입력과 운영/다중 접속 검증 한계는 [육군 지휘 패널 원본 배치](docs/ui.md#육군-지휘-패널-원본-배치)에 보존합니다.

중점 상세 창의 원본 자산·독립 스크롤·단추 상태 및 GUI2 집중 재현은 [중점 상세 원본 화면 대조](docs/ui.md#중점-상세-원본-화면-대조)를 따릅니다. `HOI_FOCUS_PREVIEW`에 서버가 export한 상세 fixture 폴더를 지정하면 원본 파일명별 조건·효과·기간·완료 상태를 두 해상도로 실제 렌더합니다. 서버 문구·미지원 효과와 빠진 장면은 원본 일치와 구분하며, tree-only·합성 캡처도 별도 증거입니다.

결정 이름·설명·분류와 툴팁의 `[TAG.GetFlag]`는 외부 팩의 원본 10×7 작은 국기를 사용한다. 국기만 별도 비트맵 글리프로 그리며 문장은 Minecraft 기본 글꼴과 기존 넘침 규칙을 유지한다. 미등록 국기는 토큰 글자를 그대로 표시한다. 원본 근거·간격 가정·재현 및 검증 한계는 [결정 문구의 국기](docs/ui.md#결정-문구의-국기)를 참고한다.

HQ는 원본 18종 아이콘·기본 내구도·지휘관 혜택 미리보기, 실제 연구·저장된 DLC 상태에 따른 후보 검사, 최대 전열 4칸·지원 4칸의 초안 추가·제거를 연결한다. 첫 칸의 계열 변경은 같은 열 전체를 전환하며 다른 계열 혼합·지원중대 중복·마지막 대대 제거를 거절한다. 사용자 최신 지정대로 HQ의 3D 영역은 사용하지 않고 원본 지역 변동치 11개를 표시한다. 값은 HQ 편성과 국가 연구·교리 부대 효과에서 계산한다. 포획 조회창은 장교단 좌/우클릭, 두 탭, 서버 전체 검색·정렬·페이지와 재시도·권한 회수를 지원한다.

HQ의 장비·연구·교리·국가 보정을 포함한 공통 전투 수치 28개와 후보 증감, 캐릭터 `is_dead` 효과·조건·임명 정리·저장 연결을 구현했다. HQ 수치는 원본 기본 장비 프로필 기반이며 실제 지급 장비 및 원본 엔진 전체 일치를 뜻하지 않는다. 실제 유료 편성·배치·철수·효과 및 지휘관 스킬의 미확인 비용·범위·시간 처리는 사용자 선택대로 원본 관측을 기다린다. 미선택 상태·플레이어 GUI 배율·원본 설치·사용자 세이브는 보존한다. 운영 JAR 교체·기동·커밋은 별도 승인 범위다.

장교단 영역 제목·HQ/포획 버튼·선택창 제목, 교리명·분류명·이름판·필요 통달치는 각 셀의 중앙에 배치한다. 정신·참모·법률 선택창의 후보 이름과 비용 위치는 [원본 GUI 수치](docs/ui.md#client-implementation-constraints)를 따른다. 현재 통달치는 아이콘과 숫자의 실제 표시 폭을 합쳐 묶음 전체를 중앙에 배치한다. 긴 설명과 목록 본문은 왼쪽 정렬을 유지한다. 해당 원본 툴팁은 왼쪽 정렬과 빈줄, 색상·명시된 인라인 아이콘을 보존한다. 대교리·하위 교리 이름판은 군종색을 유지하고, 보상 배경은 군종별 달성·잠금 프레임을 쓴다. 숫자 글자는 노란색, 작은 배경판은 원본 GUI 기본 중립 프레임이다.

## 관련 저장소

| 저장소 | 역할 |
| --- | --- |
| [HOI](https://github.com/Kyarurung/HOI) | 서버 시뮬레이션·Fabric 통합·공유 프로토콜·게임 콘텐츠 |
| [HOI-client — 현재 저장소](https://github.com/Kyarurung/HOI-client) | 클라이언트 화면·입력·지도 렌더링·음향 |
| [HOI-server](https://github.com/Kyarurung/HOI-server) | 운영 실행 파일·모드·설정·검증 도구 |
| [HOI-resourcepack](https://github.com/Kyarurung/HOI-resourcepack) | UI·지도·모델·초상화·음향 자산과 팩 빌드 |

## 사용과 빌드

결정 화면은 서버의 부족 비용 색·임무 진행 상태·대상 주를 표시하고, 호버 강조를 인증된 요청으로 연결합니다. 추적 점·사건/뉴스/작은 사건 팝업은 `config/hoi-decisions.properties`의 개인 설정입니다. [설정·지도 계약과 검증 한계](docs/ui.md#결정-개인-설정과-지도-요청)를 참고하세요. 원본 11월 캡처와 서버 1월 캠페인 상태를 구분하며 미지원 수출 효과·상단 결정 경보를 구현했다고 표시하지 않습니다.

Minecraft 26.3, Java 25, Fabric Loader 0.20.1과 Fabric API 0.164.0+26.3을 사용합니다. 빌드한 `build/libs/hoi-client-1.0.0+26.3.jar`를 플레이어의 `mods/`에 넣고 HOI 서버의 리소스팩을 모두 수락합니다. 포함된 공유 프로토콜은 따로 설치하지 않습니다.

먼저 인접한 HOI-resourcepack에서 팩을 빌드한 뒤 이 저장소에서 실행합니다.

```powershell
./gradlew test runGameTest remapJar
```

GameTest는 실행마다 `client/build/run/clientGameTest-runs/<실행 ID>/`를 새로 사용하며 이전 월드·로그·캡처를 삭제하지 않습니다. 실행 경로와 Gradle PID·시작 시각·결과는 각 폴더의 `hoi-run.json`과 `client/build/run/clientGameTest-latest.json`에 기록합니다. 일반 실패도 원래 종료 코드를 유지하고 기록을 보존합니다. Gradle을 강제 종료하면 상태가 `running`으로 남을 수 있으므로 실제 PID·생성 시각·명령줄을 확인하세요. 자동으로 프로세스나 `session.lock`을 삭제하지 않습니다. 같은 체크아웃의 Gradle 빌드는 순차 실행하세요. 실행 폴더 격리는 중단된 테스트의 잠금 충돌을 막으며 빌드 산출물의 동시 수정까지 허용하는 기능은 아닙니다. 구성 캐시는 이 실행 작업에서 지원하지 않습니다. 격리·자식 종료 회귀 검사는 python scripts/verify_gametest_isolation.py로 별도 실행합니다. 이 검사는 임시 JavaExec 프로브를 사용하며 실제 Minecraft GameTest를 대체하지 않습니다.

필터 없는 전체 검증에는 서버 fixture와 고정 입력을 미리 지정해야 합니다. `HOI_COMMANDER_PREVIEW`·`HOI_CAMPAIGN_PREVIEW`가 없으면 `NullPointerException`으로, `HOI_AGENCY_OPERATION_PREVIEW`·`HOI_MISSION_OVERLAY_FIXTURES`나 레이드 강조 입력이 없으면 검증 실패로 그 검사에서 실행이 멈춥니다. `HOI_OFFICER_PREVIEW`·`HOI_LAND_PLAN_PREVIEW`·`HOI_FOCUS_PREVIEW`는 없으면 관련 검사를 건너뛰므로 명령의 성공만으로 같은 전체 범위를 확인했다고 판단하지 않습니다.

1. 인접한 HOI에서 `test runGameTest`를 먼저 실행합니다.
2. `C:\HOI\fabric\build`의 `*-fixtures` 폴더와 그 서버 실행 폴더(`C:\HOI\fabric\build\run\gameTest-runs\<실행 ID>`)의 `raid-highlight-source-fixtures`·`agency-highlight-source-fixtures`를 한 폴더(`<fixtures>`)에 모읍니다. 레이드 검사는 지휘관 입력 폴더 옆의 `raid-highlight-source-fixtures`를 읽습니다.
3. 다음과 같이 지정합니다.
   - `HOI_COMMANDER_PREVIEW`, `HOI_AGENCY_OPERATION_PREVIEW`, `HOI_DOCTRINE_MILESTONE_PREVIEW`: `<fixtures>\commander-source-fixtures`
   - `HOI_MISSION_OVERLAY_FIXTURES`: `<fixtures>\mission-overlay-source-fixtures`
   - `HOI_OFFICER_PREVIEW`: `<서버 실행 폴더>\officer-source-fixtures`
   - `HOI_LAND_PLAN_PREVIEW`: `C:\HOI\fabric\build\land-plan-overlay.json`
   - `HOI_CAMPAIGN_PREVIEW`: `cmp82` 또는 `cmp80-6` 하위 폴더가 있는 캠페인 고정 입력 폴더(이 PC: `%LOCALAPPDATA%\claude-hoi\v24\client-fixtures\campaign-input`)
   - `HOI_FOCUS_PREVIEW`: 서버가 export한 중점 상세 fixture 폴더(이 PC: `%LOCALAPPDATA%\claude-hoi\cmp50-server\focus-ui`)
4. `HOI_UI_REVIEW_PREVIEW`, `HOI_DOCTRINE_GRAND_PREVIEW`, `HOI_DECISION_PREVIEW`, `HOI_CAPTURE_BEFORE`는 별도 검토 모드를 켜므로 비웁니다.

HQ·포획·전술·교리 입력은 Gradle이 최신 서버 검사로 자동 생성한다. `-PhoiTestSuite=missions`와 `-PhoiTestSuite=movement-controls`에서는 `armyHud` 그룹이 지휘 패널 입력 31개를, `movement-controls`에서는 `missionOverlay` 그룹이 육·해·공 임무 오버레이 입력 3개를 같은 방식으로 생성하고 `HOI_COMMANDER_PREVIEW`·`HOI_MISSION_OVERLAY_FIXTURES`를 그 출력으로 지정한다. 필터 없는 전체 검사는 지휘관 임명·제독·에이스·특성 화면 입력이 이 그룹에 없으므로 위 수동 경로를 계속 사용한다. 2026-10-11에 소유자 요청으로 육군 모델 렌더링 검사(`MapArmyRenderChecks`: 모델 큐브 수·텍스처 존재·대기/이동/전투 캡처)와 한국 음악 전용 플레이리스트 화면 검사를 클라이언트 GameTest에서 제거했다. 별도 `AudioMusicRoutingChecks`의 자동 재생·지도 전환·채널/수동 상태 검사는 `audio`와 전체 묶음에 유지한다. 모델·플레이리스트 자산을 바꾸면 리소스팩의 `test_army_models.py`·`test_korean_playlist.py`와 직접 확인으로 검증한다.

최신 실행 폴더는 PowerShell에서 `(Get-Content .\client\build\run\clientGameTest-latest.json -Raw | ConvertFrom-Json).runDirectory`로 확인합니다. 클라이언트 스크린샷은 해당 폴더의 `screenshots/`에 있습니다. 디스크 정리는 테스트가 끝난 뒤 필요한 증거를 보존하고 별도로 수행합니다.

Linux에서는 `./gradlew`, 화면 없는 환경에서는 `xvfb-run -a ./gradlew`를 사용합니다. 다른 팩은 `-PhoiResourcePack=<resources.zip 절대 경로>`로 지정하며 같은 빌드의 manifest와 모든 분할 ZIP이 필요합니다.

전술·교리를 포함하는 `:client:runClientGameTest`와 `runGameTest`는 인접한 HOI의 권위 있는 서버 테스트를 먼저 실행해 현재 소스의 전술 입력 7종·교리 입력 10종을 `client/build/generated/authoritative-fixtures/`에 생성합니다. 수동 `HOI_TACTIC_PREVIEW`·`HOI_DOCTRINE_PREVIEW` 설정이나 기존 서버 `build/` 출력은 필요하지 않습니다. 준비 작업은 해당 환경 변수의 경로를 생성된 입력으로 덮어쓰고, Minecraft 실행 전에 원본 소스·입력 해시와 양쪽 프로토콜을 확인합니다. Gradle은 소스·콘텐츠·출력 변경을 추적하므로 누락되거나 변경된 입력을 다시 생성합니다. HOI가 다른 위치에 있으면 `-PhoiServerSource=<HOI 저장소 경로>`를 지정합니다. HOI 소스가 없으면 렌더러를 띄우기 전에 실패하며, 고정 JSON으로 대체하거나 검사를 생략하지 않습니다.

외교는 `./gradlew :client:runClientGameTest -PhoiTestSuite=diplomacy`로 재현합니다. 무기대여 뷰 4종과 출처 메타데이터, 의용군 입력 4종도 같은 자동 생성·출처 검증을 거치며, 서버 공통 API가 컴파일 가능한 상태여야 합니다. 새 화면의 실제 렌더링 검증 한계는 [UI 안내](docs/ui.md#복수-장비-무기대여와-의용군-화면)를 참고하세요.

전술만 재현하려면 `./gradlew :client:runClientGameTest -PhoiTestSuite=tactics`를 실행합니다. 이 과정은 개발 테스트만 실행하며 운영 HOI-server를 시작하지 않습니다.

HQ·포획 집중 검사는 `test runGameTest remapJar -PhoiTestSuite=captivity`를 사용한다. 서버 원본 응답으로 GUI2 두 해상도, Esc/Enter/숫자패드 Enter, 추가·제거 좌표 클릭, 화면 경계, 지역 툴팁과 긴 후보 증감의 화면 내 표시·스크롤을 검사한다. 전체 서버·클라이언트 출하 검사는 필터 없는 `test runGameTest remapJar`로 실행하며 집중 검사 결과와 구분한다.

지휘관 이벤트는 `presentation = leader_event`를 원본 보고서 창으로 표시하며 제목·본문·선택지의 그림자를 제거합니다. 지휘관·국가 이벤트·국제 뉴스·슈퍼 이벤트 제목은 기존 Minecraft 글꼴의 볼드체와 본문 대비1.5배를 사용합니다. 긴 제목은 볼드를 유지한 본문 크기로 돌아간 뒤 말줄임합니다. 본문·선택지·완료창과 다른 종류의 제목은 기존 크기입니다. 국가 이벤트·국제 뉴스·슈퍼 이벤트는 원본 틀 그림과 원본 한 열 선택지를 사용하며 수작업 분류 띠·분류 글·닫기 그림을 표시하지 않습니다. 서버 원본 fixture 두 개와 입력 회귀는 `test runGameTest remapJar -PhoiTestSuite=leader-event`로 검사합니다. 서버 작업이 동시에 진행될 때는 검증된 사본에 `-PhoiServerSource=<사본 경로>`와 `-PhoiProtocolSource=<같은 사본의 protocol/src/main/java>`를 함께 지정합니다. 원본 위치·선택지 간격의 반올림·색상 근거·제목 규칙·렌더 검증 한계는 [지휘관 이벤트 보고서](docs/ui.md#지휘관-이벤트-보고서)에 기록합니다.

세 이벤트 창은 GUI2 두 해상도에서 원본 카탈로그 19종을 `runGameTest -PhoiTestSuite=event-frames`로 검사합니다. 국가 이벤트는 위쪽을 고정하고 본문 아래에 선택지와 그림을 놓으며, 뉴스는 고정 틀 안에서 본문을 따라 선택지를 내립니다. 긴 본문·인용문은 스크롤 위치를 표시하고 본문 위의 휠이나 PageUp/PageDown·Home/End로 마지막 줄까지 읽습니다. 본문 영역과 이동량은 온전한 줄 단위이며 그림·선택지는 고정됩니다. 선택지는 한 열로 넘깁니다. 답할 선택지가 있는 국가·국제 뉴스·지휘관 이벤트는 원본 최소화 단추 또는 Esc로 창만 접고 사건과 결정의 사건 목록에서 다시 엽니다. 서버 세션·revision은 유지하고 `remainingDays`가 있는 사건 행에는 남은 날수를 표시합니다. null은 숨기고 0은 `0일`로 표시합니다. 선택지 효과 툴팁은 서버의 선택적 `tooltip` 원문과 공용 결정 툴팁 렌더러를 사용합니다. 집중 재현은 `test runGameTest remapJar -PhoiTestSuite=event-tooltips`이며 규칙과 한계는 [효과 툴팁과 최소화](docs/ui.md#이벤트-선택지-효과-툴팁과-최소화)를 참고하세요. 원본 화면 아홉 사례의 좌표·확인된 규칙·가정·비교 재현은 [세 이벤트 창](docs/ui.md#세-이벤트-창의-원본-프레임)을 참고하세요.

스코어보드 미리보기는 `runGameTest -PhoiTestSuite=scoreboard`로 분을 뺀 `날짜: 2020년 1월 1일 00시`를 맨 위에 놓은 PAUSED·X1·X3·X5·X7·X10과 가장 긴 날짜의 7장을 생성합니다. 이벤트의 `[GetDateText]`도 서버가 같은 분 없는 표기로 발급합니다. 지휘관 시험은 기본 날짜와 가장 긴 합성 날짜 `날짜: 2022년 12월 31일 23시`를 GUI2 두 해상도에서 한 줄인지 검사합니다. [날짜 렌더 검증](docs/ui.md#날짜와-배속-렌더-검증)에 재현 명령과 한계를 기록합니다.

캠페인 HUD에서 `F`로 커서를 열고 상단 국기로 국가 메뉴를 엽니다. GUI scale 2가 화면 검증 기준이며 플레이어 배율을 강제하지 않습니다. 하단은 스킬 → 명령 → 군·함대 카드 순서입니다. 유효한 국가·지도의 캠페인 HUD가 있으면 실행·일시정지 여부와 관계없이 바닐라 핫바·생존 HUD·경험치/로케이터를 숨기고 카드 끝을 화면 아래에 붙입니다. 지도 또는 캠페인 HUD에서는 선택 아이템 이름·관전자 아이템 안내·액션바 설명도 숨깁니다. 일반 Minecraft 화면의 표시는 유지합니다. 카드는 좌측 패널과 독립된 한 줄에 표시하고, 긴 목록은 페이지 버튼·휠로 이동합니다. 시작·재개 전 빈 국가 응답은 생성 버튼과 명령 선택 캐시까지 숨깁니다. 드래그·빈 선택창·설명 글 규칙은 [대기열 이동과 군 카드 표시](docs/ui.md#대기열-이동과-군-카드-표시)를 참고하세요.

GUI2 1280×720·2560×1440에서 스킬·명령·군 카드·액션키·핫바가 분리되어 보이며 긴 편집 툴팁은 하단 조작부 옆에 배치된다. 서버의 실제 예약 1/5·예약 5/5·유료 완공 5/5 입력은 각각 노란 사선·파란 사선·파란 단색으로 렌더된다.

## 저장소 안내

`client/`는 화면·입력·렌더링, `protocol/`은 HOI 서버와 공유하는 통신 모듈입니다. JUnit 보고서는 각 모듈의 `build/reports/tests/test/`, 렌더링 캡처는 `client/build/run/clientGameTest-runs/<실행 ID>/screenshots/`에 생성됩니다. 화면과 구현 범위는 [UI 안내](docs/ui.md), [구조](docs/refactoring.md), [HOI 문서](https://github.com/Kyarurung/HOI/blob/main/docs/README.md)를 참고하세요.

개발 렌더링 검사는 운영 접속·다중 사용자 동기화·팩 다운로드 검증과 구분합니다. 실행 중인 클라이언트 JAR은 교체하지 않습니다.

## 라이선스

by Kyarurung. 사용자 소유 자체 코드·도구는 [AGPL-3.0-only](LICENSE)입니다. 제3자 코드·의존성, TFR/TFRKP/HOI4 원본과 파생 콘텐츠·자산 및 소유권 불명확 자료는 제외하고 원래 권리와 출처를 유지합니다. 정확한 적용 범위는 [LICENSE-SCOPE.md](LICENSE-SCOPE.md)를 참고하세요.
