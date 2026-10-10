# HOI-client

Minecraft 26.3 · Fabric · Java 25용 HOI 전용 클라이언트 모드입니다. 국가 메뉴·연구·생산·장교단·교리·군종 작전과 지도 정보를 표시합니다. 게임 계산과 저장은 HOI 서버가 담당하고, UI·초상화·음향은 외부 리소스팩에서 읽습니다.

공통 장기 목표와 완료 기준은 [HOI 프로젝트 목표](../HOI/GOAL.md)를 따릅니다. 목표는 현재 구현 완료 상태와 구분합니다.

## 관련 저장소

| 저장소 | 역할 |
| --- | --- |
| [HOI](https://github.com/Kyarurung/HOI) | 서버 시뮬레이션·Fabric 통합·공유 프로토콜·게임 콘텐츠 |
| [HOI-client — 현재 저장소](https://github.com/Kyarurung/HOI-client) | 클라이언트 화면·입력·지도 렌더링·음향 |
| [HOI-server](https://github.com/Kyarurung/HOI-server) | 운영 실행 파일·모드·설정·검증 도구 |
| [HOI-resourcepack](https://github.com/Kyarurung/HOI-resourcepack) | UI·지도·모델·초상화·음향 자산과 팩 빌드 |

## 사용과 빌드

Minecraft 26.3, Java 25, Fabric Loader 0.19.5 이상과 Fabric API 0.161.0+26.3을 사용합니다. 빌드한 `build/libs/hoi-client-1.0.0+26.3.jar`를 플레이어의 `mods/`에 넣고 HOI 서버의 리소스팩을 모두 수락합니다. 포함된 공유 프로토콜은 따로 설치하지 않습니다.

먼저 인접한 HOI-resourcepack에서 팩을 빌드한 뒤 이 저장소에서 실행합니다.

```powershell
./gradlew test runGameTest remapJar
```

GameTest는 실행마다 `client/build/run/clientGameTest-runs/<실행 ID>/`를 새로 사용하며 이전 월드·로그·캡처를 삭제하지 않습니다. 실행 경로와 Gradle PID·시작 시각·결과는 각 폴더의 `hoi-run.json`과 `client/build/run/clientGameTest-latest.json`에 기록합니다. 일반 실패도 원래 종료 코드를 유지하고 기록을 보존합니다. Gradle을 강제 종료하면 상태가 `running`으로 남을 수 있으므로 실제 PID·생성 시각·명령줄을 확인하세요. 자동으로 프로세스나 `session.lock`을 삭제하지 않습니다. 같은 체크아웃의 Gradle 빌드는 순차 실행하세요. 실행 폴더 격리는 중단된 테스트의 잠금 충돌을 막으며 빌드 산출물의 동시 수정까지 허용하는 기능은 아닙니다. 구성 캐시는 이 실행 작업에서 지원하지 않습니다. 격리·자식 종료 회귀 검사는 python scripts/verify_gametest_isolation.py로 별도 실행합니다. 이 검사는 임시 JavaExec 프로브를 사용하며 실제 Minecraft GameTest를 대체하지 않습니다.

최신 실행 폴더는 PowerShell에서 `(Get-Content .\client\build\run\clientGameTest-latest.json -Raw | ConvertFrom-Json).runDirectory`로 확인합니다. 클라이언트 스크린샷은 해당 폴더의 `screenshots/`에 있습니다. 디스크 정리는 테스트가 끝난 뒤 필요한 증거를 보존하고 별도로 수행합니다.

Linux에서는 `./gradlew`, 화면 없는 환경에서는 `xvfb-run -a ./gradlew`를 사용합니다. 다른 팩은 `-PhoiResourcePack=<resources.zip 절대 경로>`로 지정하며 같은 빌드의 manifest와 모든 분할 ZIP이 필요합니다.

전술·교리를 포함하는 `:client:runClientGameTest`와 `runGameTest`는 인접한 HOI의 권위 있는 서버 테스트를 먼저 실행해 현재 소스의 전술 입력 7종·교리 입력 10종을 `client/build/generated/authoritative-fixtures/`에 생성합니다. 수동 `HOI_TACTIC_PREVIEW`·`HOI_DOCTRINE_PREVIEW` 설정이나 기존 서버 `build/` 출력은 필요하지 않습니다. 준비 작업은 해당 환경 변수의 경로를 생성된 입력으로 덮어쓰고, Minecraft 실행 전에 원본 소스·입력 해시와 양쪽 프로토콜을 확인합니다. Gradle은 소스·콘텐츠·출력 변경을 추적하므로 누락되거나 변경된 입력을 다시 생성합니다. HOI가 다른 위치에 있으면 `-PhoiServerSource=<HOI 저장소 경로>`를 지정합니다. HOI 소스가 없으면 렌더러를 띄우기 전에 실패하며, 고정 JSON으로 대체하거나 검사를 생략하지 않습니다.

외교는 `./gradlew :client:runClientGameTest -PhoiTestSuite=diplomacy`로 재현합니다. 무기대여 뷰 4종과 출처 메타데이터, 의용군 입력 4종도 같은 자동 생성·출처 검증을 거치며, 서버 공통 API가 컴파일 가능한 상태여야 합니다. 새 화면의 실제 렌더링 검증 한계는 [UI 안내](docs/ui.md#복수-장비-무기대여와-의용군-화면)를 참고하세요.

전술만 재현하려면 `./gradlew :client:runClientGameTest -PhoiTestSuite=tactics`를 실행합니다. 이 과정은 개발 테스트만 실행하며 운영 HOI-server를 시작하지 않습니다.

캠페인 HUD에서 `F`로 커서를 열고 상단 국기로 국가 메뉴를 엽니다. GUI scale 2가 화면 검증 기준이며 플레이어 배율을 강제하지 않습니다. 하단은 스킬 → 명령 → 군·함대 카드 → 액션키 안내 → 핫바 순서입니다. 카드는 좌측 패널과 독립된 한 줄에 표시하고, 긴 목록은 페이지 버튼·휠로 이동합니다. 시작·재개 전 빈 국가 응답은 생성 버튼과 명령 선택 캐시까지 숨깁니다. 실제 렌더링 회귀 검사는 운영 멀티플레이 검증과 별도입니다.

## 저장소 안내

`client/`는 화면·입력·렌더링, `protocol/`은 HOI 서버와 공유하는 통신 모듈입니다. JUnit 보고서는 각 모듈의 `build/reports/tests/test/`, 렌더링 캡처는 `client/build/run/clientGameTest-runs/<실행 ID>/screenshots/`에 생성됩니다. 화면과 구현 범위는 [UI 안내](docs/ui.md), [구조](docs/refactoring.md), [HOI 문서](https://github.com/Kyarurung/HOI/blob/main/docs/README.md)를 참고하세요.

개발 렌더링 검사는 운영 접속·다중 사용자 동기화·팩 다운로드 검증과 구분합니다. 실행 중인 클라이언트 JAR은 교체하지 않습니다.

## 검증 상태

최신 전체 `test runGameTest remapJar`는 15분 3초에 클라이언트·프로토콜 JUnit 140개와 실제 Minecraft 렌더링·입력 검사를 통과했다. 서버가 생성한 화면 입력과 공유 프로토콜 40개를 검증했으며 전체 실행 캡처 1,168장을 보존했다. 서버 성공 실행에서 갱신한 atlas/region 입력도 해시 검증 후 사용했다. 전체 캡처를 모두 직접 열람했다는 뜻은 아니며, 이번 변경 화면은 별도로 직접 열어 확인했다.

GUI2 1280×720·2560×1440에서 스킬·명령·군 카드·액션키·핫바가 분리되어 보이며 긴 편집 툴팁은 하단 조작부 옆에 배치된다. 직접 군·집단군 편성의 요청과 흰색 현재 수량 표시도 검사했다. 서버의 실제 예약 1/5·예약 5/5·유료 완공 5/5 입력은 각각 노란 사선·파란 사선·파란 단색으로 렌더된다. 실제 캡처를 직접 열고 상태별 색·사선 픽셀을 수량 자료와 대조했다. 앞선 구형 블록 형식의 빈 캡처와 초기 부대 누락 실행은 실패 기록이며 완료 증거에 포함하지 않는다. 운영 접속·다중 사용자 동기화·원본 계산 전체 일치 검증과는 별도다.

HQ는 원본 18종 아이콘·기본 내구도·지휘관 혜택 미리보기, 실제 연구·저장된 DLC 상태에 따른 후보 검사, 최대 전열 4칸·지원 4칸의 초안 추가·제거를 연결한다. 첫 칸의 계열 변경은 같은 열 전체를 전환하며 다른 계열 혼합·지원중대 중복·마지막 대대 제거를 거절한다. 사용자 최신 지정대로 HQ의 3D 영역은 사용하지 않고 원본 지역 변동치 11개를 표시한다. 값은 HQ 편성과 국가 연구·교리 부대 효과에서 계산한다. 포획 조회창은 장교단 좌/우클릭, 두 탭, 서버 전체 검색·정렬·페이지와 재시도·권한 회수를 지원한다.

HQ·포획 집중 검사는 `test runGameTest remapJar -PhoiTestSuite=captivity`를 사용한다. 서버 원본 응답으로 GUI2 두 해상도, Esc/Enter/숫자패드 Enter, 추가·제거 좌표 클릭, 화면 경계, 지역 툴팁과 긴 후보 증감의 화면 내 표시·스크롤을 검사한다. 전체 서버·클라이언트 출하 검사는 필터 없는 `test runGameTest remapJar`로 실행하며 집중 검사 결과와 구분한다.

추가 화면 입력을 포함한 전체 검사는 `HOI_COMMANDER_PREVIEW=C:\HOI\fabric\build\commander-source-fixtures`, `HOI_OFFICER_PREVIEW=<성공한 서버 실행의 runDirectory>\officer-source-fixtures`, `HOI_LAND_PLAN_PREVIEW=C:\HOI\fabric\build\land-plan-overlay.json`, `HOI_MISSION_OVERLAY_FIXTURES=C:\HOI\fabric\build\mission-overlay-source-fixtures`를 사용한다. HQ·포획·전술·교리 입력은 Gradle이 최신 서버 검사로 자동 생성한다. `-PhoiTestSuite=missions`와 `-PhoiTestSuite=movement-controls`에서는 `armyHud` 그룹이 지휘 패널 입력 31개를, `movement-controls`에서는 `missionOverlay` 그룹이 육·해·공 임무 오버레이 입력 3개를 같은 방식으로 생성하고 `HOI_COMMANDER_PREVIEW`·`HOI_MISSION_OVERLAY_FIXTURES`를 그 출력으로 지정한다. 필터 없는 전체 검사는 지휘관 임명·제독·에이스·특성 화면 입력이 이 그룹에 없으므로 위 수동 경로를 계속 사용한다. 이 자동 입력으로 `test runGameTest remapJar -PhoiTestSuite=missions`와 `:client:runClientGameTest -PhoiTestSuite=movement-controls`가 GUI2 실제 렌더링·입력 검사를 통과했다. 필터 없는 전체 검사와 운영 멀티플레이 검증은 이 결과에 포함되지 않는다.

HQ의 장비·연구·교리·국가 보정을 포함한 공통 전투 수치 28개와 후보 증감, 캐릭터 `is_dead` 효과·조건·임명 정리·저장 연결을 구현했다. HQ 수치는 원본 기본 장비 프로필 기반이며 실제 지급 장비 및 원본 엔진 전체 일치를 뜻하지 않는다. 실제 유료 편성·배치·철수·효과 및 지휘관 스킬의 미확인 비용·범위·시간 처리는 사용자 선택대로 원본 관측을 기다린다. 운영 다중 접속·실제 소켓 재접속, 원본 전체 픽셀 일치는 별도 검증이다. 미선택 상태·플레이어 GUI 배율·원본 설치·사용자 세이브는 보존한다. 운영 JAR 교체·기동·커밋은 별도 승인 범위다.

장교단 영역 제목·HQ/포획 버튼·선택창 제목, 교리명·분류명·이름판·필요 통달치는 각 셀의 중앙에 배치한다. 현재 통달치는 아이콘과 숫자의 실제 표시 폭을 합쳐 묶음 전체를 중앙에 배치한다. 긴 설명과 목록 본문은 왼쪽 정렬을 유지한다. 해당 원본 툴팁은 왼쪽 정렬과 빈줄, 색상·명시된 인라인 아이콘을 보존한다. 교리 단계 숫자 배경의 군종색 순환 오류를 제거했다. 대교리·하위 교리 이름판의 군종색을 유지하며, 보상 배경의 군종별 달성·잠금 프레임 연결 오류를 바로잡았다. 요청된 100·200 등 단계 숫자 자체의 군종별 색 구분은 아직 확인되지 않았다. 숫자 글자는 노란색, 작은 배경판은 원본 GUI 기본 중립 프레임이며, 해군·공군 단계 숫자의 원작 화면 대조가 남아 있다.

## 라이선스

by Kiruya_Momochi. 사용자 소유 자체 코드·도구는 [AGPL-3.0-only](LICENSE)입니다. 제3자 코드·의존성, TFR/TFRKP/HOI4 원본과 파생 콘텐츠·자산 및 소유권 불명확 자료는 제외하고 원래 권리와 출처를 유지합니다. 정확한 적용 범위는 [LICENSE-SCOPE.md](LICENSE-SCOPE.md)를 참고하세요.
