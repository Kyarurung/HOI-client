# HOI-client

**Minecraft 26.2 · Fabric · Java 25용 HOI 전용 클라이언트 모드입니다.** 국가 메뉴·연구·생산·설계·지도와 음향을 표시하고, 게임 상태와 계산·저장은 HOI 서버가 관리합니다.

[설치와 조작](#설치와-조작) · [빌드와 검증](#빌드와-검증) · [화면 기준](docs/ui.md) · [구조](docs/refactoring.md) · [음악](docs/audio.md)

## 저장소 구분

| 저장소 | 역할 |
| --- | --- |
| **HOI-client — 현재 저장소** | 클라이언트 화면·입력·지도 렌더링·음향 |
| [HOI](https://github.com/Kyarurung/HOI) | 서버 모드·시뮬레이션·게임 콘텐츠·공유 프로토콜 |
| [HOI-resourcepack](https://github.com/Kyarurung/HOI-resourcepack) | 비공개 UI·지도·모델·음향 자산 |
| [HOI-server](https://github.com/Kyarurung/HOI-server) | 서버 실행 파일·모드·설정·운영 도구 |

## 설치와 조작

1. Minecraft **26.2**, Fabric Loader **0.19.5 이상**, Fabric API **0.159.0+26.2**를 설치합니다. 버전 기준은 [gradle.properties](gradle.properties)입니다.
2. Minecraft를 완전히 종료한 뒤 `hoi-client-0.1.0-SNAPSHOT.jar`를 플레이어의 `mods/`에 넣습니다. 공유 `hoi_protocol`은 JAR에 포함되어 별도로 설치하지 않습니다.
3. HOI 서버에 접속하고 서버가 제공하는 리소스팩을 모두 수락합니다. UI·지도·모델·음향은 외부 팩에서 읽습니다.
4. `/hoi select`로 국가를 선택하고 `/hoi ready`로 준비합니다. 관리자가 `/hoi start`로 캠페인을 시작합니다.

| 조작 | 기능 |
| --- | --- |
| `F` | 캠페인 HUD의 마우스 커서 열기·닫기 |
| `/hoi menu` | 국가 메뉴 열기 |
| 메뉴 연구 아이콘·`/hoi research`·`/hoi-research` | 연구 화면 열기 |
| 연구 트리 휠·가로 휠·우클릭 드래그 | 트리 이동 |
| 생산 공장 ±에서 `Shift` | 공장 10개 단위 요청 |

HUD가 없는 상태에서는 F의 기본 보조 손 교체를 유지합니다. R 연구 단축키와 클라이언트 `/hoi` 루트는 등록하지 않습니다. 캠페인 시작·정지·배속과 저장 규칙은 [HOI 조작 안내](https://github.com/Kyarurung/HOI#operations)를 따릅니다.

**실행 중인 클라이언트의 JAR을 교체하지 않습니다.** `ZipFile invalid LOC header` 또는 `client-sided command 'hoi start'` 오류가 있으면 호환되는 JAR을 설치하고 클라이언트를 완전히 다시 실행합니다. 서버와 클라이언트의 통신 형식도 맞아야 합니다.

## 화면 범위

국가 정보·결정·정보기관·연구·무역과 경제·건설·생산·모병과 배치·군수·장교단을 서버의 비공개 응답으로 표시합니다. 기갑·해군·공군 설계는 서버가 발급한 부품과 연구 조건을 사용합니다. 원본 이미지와 설명이 존재하는 것만으로 해당 원작 기능 전체가 구현된 것은 아닙니다.

모든 셀의 크기·여백·정렬과 주 시각 검증은 **GUI scale 2**가 기준입니다. 플레이어의 GUI 배율 설정을 강제로 바꾸지 않습니다. 한국어와 툴팁은 TFR 한글패치의 원문 키·색상·빈줄을 따르며 `---`는 빈줄로 표시합니다. 세부 표시 규칙과 미지원 범위는 [화면 기준](docs/ui.md), 게임 계산 범위는 [HOI 문서](https://github.com/Kyarurung/HOI/blob/main/docs/README.md)를 참고하세요.

## 빌드와 검증

먼저 인접한 HOI-resourcepack에서 의존성을 설치하고 팩을 빌드합니다.

```powershell
python -m pip install numpy==2.3.5 Pillow==12.3.0
python scripts/build_pack.py
```

이후 **HOI-client 루트**에서 Java 25로 실행합니다.

```powershell
.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon
```

| 목적 | 명령·위치 |
| --- | --- |
| 설계 화면 집중 검증 | `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=designers --console=plain --no-daemon` |
| 음악 집중 검증 | `.\gradlew.bat test runGameTest remapJar -PhoiTestSuite=audio --console=plain --no-daemon` |
| 개발 클라이언트 | `.\gradlew.bat :client:runClient` |
| 배포 JAR | `build/libs/hoi-client-0.1.0-SNAPSHOT.jar` |
| JUnit 보고서 | 각 모듈의 `build/reports/tests/test/` |
| 렌더링 캡처 | `client/build/run/clientGameTest/screenshots/` |

다른 팩 경로는 `-PhoiResourcePack=<resources.zip 절대 경로>`로 지정합니다. 같은 폴더의 manifest와 모든 분할 ZIP이 필요합니다. Linux에서는 `./gradlew`, 화면이 없는 환경에서는 `xvfb-run -a ./gradlew test runGameTest remapJar`를 사용합니다.

렌더링 GameTest는 실제 Minecraft 렌더러와 서버 응답 fixture를 사용합니다. 운영 서버의 실제 접속·다중 사용자 동기화·HTTP 팩 다운로드·추가 렌더링 모드 조합은 별도 확인 대상입니다. 빌드와 검증은 운영 서버를 실행하거나 배포하지 않습니다.

CI는 비공개 팩의 읽기 전용 키 `HOI_RESOURCEPACK_SSH_KEY`와 워크플로에 고정한 팩 커밋을 사용합니다. 자격 증명이 없는 외부 PR에서는 전체 렌더링 검증을 실행할 수 없습니다.

## 소스 구조

| 경로 | 책임 |
| --- | --- |
| [client/src/main/java/dev/hoi/client](client/src/main/java/dev/hoi/client) | 화면·공통 UI·지도·입력·음향·네트워크 |
| [protocol](protocol) | HOI 서버와 동일하게 관리하는 통신 계약 |
| [client/src/test](client/src/test) | 단위 검사 |
| [client/src/gametest](client/src/gametest) | 실제 클라이언트 렌더링 검사 |
| [docs](docs/ui.md) | 화면 규칙·구조·음향 문서 |

서버 응답의 권한·세션·리비전을 유지하고 늦은 응답이 닫힌 화면을 다시 열지 않도록 합니다. 공유 프로토콜 변경은 HOI의 복사본과 함께 적용하며 페이로드 타입을 수신기보다 먼저 등록합니다. 정적 자산과 서버 계산을 클라이언트 JAR에 복제하지 않습니다.
