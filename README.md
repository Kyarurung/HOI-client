# HOI-client

Minecraft **26.2 · Fabric · Java 25**용 HOI 전용 클라이언트 모드 개발 저장소입니다. 각 플레이어의 연구 트리·슬롯·상세창을 표시하고 연구 시작·중단 요청을 서버로 보냅니다.

| 저장소 | 역할 |
|---|---|
| [HOI-client](https://github.com/Kyarurung/HOI-client) | 이 저장소. 클라이언트 모드 소스·빌드·UI 테스트 |
| [HOI-server](https://github.com/Kyarurung/HOI-server) | 서버 실행 파일·모드·설정이 들어가는 운영 폴더 |
| [HOI](https://github.com/Kyarurung/HOI) | 서버 모드 소스·전략 시뮬레이션·게임 콘텐츠·위키 참고 문서 |

## 설치

1. 플레이어의 Minecraft 26.2에 Fabric Loader 0.19.5 이상과 Fabric API 0.159.0+26.2를 설치합니다.
2. `hoi-client-0.1.0-SNAPSHOT.jar`를 플레이어의 `mods/` 폴더에 넣습니다.
3. HOI 서버에 접속하여 `/hoi start`로 국가를 선택합니다.
4. `/hoi ready`로 전원 준비를 마치면 게임이 일시정지 상태로 시작합니다. **R**, **`/hoi research`** 또는 `/hoi-research`로 연구 화면을 엽니다. 키는 조작 설정에서 변경할 수 있습니다.

클라이언트 모드에는 Polymer·KubeJS·전략 시뮬레이션을 포함하지 않습니다. 지도와 사단을 위한 서버 리소스팩은 서버의 안내를 따릅니다. 공유 `hoi_protocol`은 클라이언트 JAR에 내장됩니다. 서버의 HOI JAR을 플레이어의 `mods/`에 함께 넣을 필요는 없습니다.

## 빌드와 테스트

Java 25로 저장소 루트에서 실행합니다.

```bash
./gradlew test runGameTest remapJar --console=plain --no-daemon
```

Windows PowerShell:

```powershell
.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon
```

출력물은 **`build/libs/hoi-client-0.1.0-SNAPSHOT.jar` 하나**입니다. `test`는 프로토콜·레이아웃 JUnit 4개, `runGameTest`는 실제 Minecraft 클라이언트에서 연구 화면·키보드·창 크기 변경을 검사합니다. 개발 실행은 `./gradlew :client:runClient`입니다. Linux의 화면 없는 환경에서는 `xvfb-run -a ./gradlew test runGameTest remapJar`를 사용합니다. GitHub Actions도 같은 경로로 검사하고 `hoi-client-26.2` 아티팩트를 올립니다.

테스트 이미지는 `client/build/run/clientGameTest/screenshots/`에 생성됩니다. UI 테스트는 시험용 데이터로 렌더링을 확인하며, 실제 다중 접속 서버에서 연구 완료까지의 전체 연동 검사는 별도입니다.

## 화면과 게임 규칙

8개 분야, 연구 슬롯, 기술 검색, 연도 축, 선행 연구 연결선, 진행 막대, 효과·해금 장비 상세를 제공합니다. 휠로 세로 이동, 가로 휠·우클릭 드래그로 트리를 이동합니다. 기술을 클릭하면 상세창이 열립니다. 서버가 선택 국가·수도·항복·슬롯·선행 연구·중복 요청을 검증합니다.

게임 시작일은 **2020-01-01**이며 X1은 **10틱/시간**, 속도는 일시정지·X1~X5입니다. 보병 연도 축은 **2000년부터**, 기갑은 **1980·2018·2020·2022·2024·2028·2032년**입니다. 게임 시작일과 연구 기준 연도는 별개입니다. 화면은 서버 카탈로그의 등록 기술을 표시하며, 현재 내장 예제는 8개입니다. TFR 전체 기술 데이터·장비 사진·3D 미리보기는 아직 포함하지 않습니다.

## 소스와 프로토콜

| 경로 | 내용 |
|---|---|
| [client](client/src/main/java/dev/hoi/client) | 연구 UI·입력·클라이언트 네트워크 |
| [protocol](protocol/src/main/java/dev/hoi/protocol) | 공유 연구 패킷·읽기 전용 화면 데이터 |
| [클라이언트 테스트](client/src/gametest) | 실제 Minecraft 렌더러 테스트 |

기존 [HOI의 `2567c2f`](https://github.com/Kyarurung/HOI/commit/2567c2fb7b439835e64d1220690be22dbc30792d)에서 분리했습니다. 원래 개발 이력은 HOI에 남아 있습니다. `protocol/`은 [HOI의 같은 모듈](https://github.com/Kyarurung/HOI/tree/main/protocol)과 동일한 소스로 시작합니다. 공유 모듈을 수정할 때는 두 저장소에 함께 반영하고 호환되지 않는 변경은 패킷 버전도 올립니다. 자동으로 다른 저장소의 소스를 가져오지는 않습니다.

수신기 등록 전 `ResearchProtocol.registerPayloadTypes()`를 호출해야 합니다. 독립 모드 초기화 순서에 의존하면 서버 시작이 실패합니다. 게임 수치·비용·완료 여부는 서버만 결정합니다.

## 서버 명령과 시간 표시

클라이언트는 /hoi 루트를 등록하지 않습니다. 이전 버전의 client-sided command 오류는 클라이언트 루트가 서버의 start·map·ready·stop을 가로챈 것이 원인입니다. 최신 클라이언트 JAR로 교체하고 클라이언트를 재실행하세요. /hoi research는 서버가 화면 열기를 요청하고 클라이언트가 기존 인증·요청 제한을 적용하는 OPEN 요청을 보냅니다. R과 /hoi-research는 독립적인 단축 입력입니다.

날짜·시간은 2020년 1월 1일 01시, 배속은 1배속~5배속으로 표시합니다. 배속 색상은 초록·노랑·주황·빨강·진한 빨강이며 | 구분선은 진한 회색입니다. 배속과 일시정지는 관리자만 변경합니다. 관리자 /hoi stop은 국가 선택과 캠페인을 초기화하고 2020년 1월 1일 00시·일시정지로 되돌립니다. 초기화 백업은 서버에서 보관하며 Minecraft 서버 자체는 종료하지 않습니다.

클라이언트 GameTest는 실제 등록 함수를 호출하여 /hoi를 등록하지 않고 /hoi-research만 등록하는지 확인하며, 연구 헤더의 한국어 날짜·배속을 실제 렌더러로 검사합니다. 운영 서버와 플레이어 클라이언트의 재접속 검증은 별도입니다.
