# 클라이언트 구조와 검증

클라이언트는 서버의 비공개 응답을 표시하고 인증된 동작을 요청한다. 서버 시뮬레이션, 저장 상태와 경제 계산은 소유하지 않는다. GUI2를 크기·정렬·여백·시각 검사의 기준으로 유지하며 플레이어의 GUI 설정은 변경하지 않는다.

## 책임과 이름

`client`는 초기화와 캠페인 HUD 수명, `network`는 요청·응답과 늦은 응답 폐기, `input`은 입력 소유권, `map`은 장면·메시·부대 표시, `research`는 연구 배치·상세·조회, `screen`은 화면과 해당 화면의 순수 계산, `ui`는 공용 셀·글꼴·툴팁·아트, `audio`는 음악·효과음 수명을 맡는다. 이 경계를 유지하고 타입 이름은 패키지 마지막 부분의 PascalCase 접두어와 책임 단어 최대 두 개로 맞췄다. 테스트는 해당 이름에 `Test` 또는 `Checks`를 붙인다. 예를 들어 `map.MapSceneMesh`, `screen.ScreenIndustry`, `ui.UiFlagStack`이다.

Fabric 등록 계약인 `HoiClient`와 `ResearchScreenGameTest`는 유지한다. 공유 `protocol`의 기존 공개 DTO·중첩 타입은 두 배포물의 소스 API이므로 이름을 유지한다. 내부 JSON 해석기는 `ProtocolJsonCodec`, 새 공용 형식은 `ProtocolNumberBounds`와 `ProtocolTextBounds`다. mixin 타입 이름과 JSON 등록을 함께 바꿨으며 Minecraft 대상 메서드·접근자 시그니처는 유지한다. 통신·저장·명령·콘텐츠 ID는 변경하지 않는다.

[타입·경로 대응표](type-renames.json)는 클라이언트 최상위 타입/파일 95개, 클래스별 내부 이름 대응 및 공용 내부 타입·테스트 4개의 변경을 보관한다. [클래스 검토 목록](code-review.csv)은 파일·타입·책임·의존성·검증 근거·결정·이름 예외를 기록한다. 파일은 main 78개, test 13개, gametest 25개, benchmark 2개이며 내부 선언을 포함한 타입은 각각 116·14·27·4개다. protocol 검토는 HOI의 공유 프로토콜 범위에 속한다.

main 77개 파일과 모든 JUnit·GameTest·benchmark 파일은 메서드 본문까지 검토했다. `ResearchSourcePlacements`는 데이터 구조와 입출력을 검토했으며 고정 좌표 하나하나를 TFR 원본과 다시 대조하지 않았다. 전체 161개 타입 가운데 160개는 본문을 검토했고 이 좌표 데이터 타입 1개는 데이터 구조 검토로 표시한다. 고정 fixture와 원본 자료의 전체 재대조는 제외한다. `ScreenCountryChecks`의 선택적 정치 원본 분기는 본문 검토와 런타임 실행을 구분하며 이번 검증에서는 실행하지 않았다. CSV의 검토 상태는 테스트 통과 여부와 구분한다.

## 공통화와 계산 경계

`ProtocolNumberBounds`는 기존 `Locale.ROOT` 소수점·백분율·부호 표시를 공유한다. 각 호출부의 정밀도, 미지값, 영 값 정규화, 배율과 색상은 그대로 둔다. 연구 효과의 `RoundingMode.DOWN`, 정수 내림, 단위 축약, 자릿수 구분과 원본 문단 처리는 서로 다른 계약이므로 유지한다. `ProtocolTextBounds`는 문자 수 제한에서 보조 평면 문자를 자르지 않는다.

`UiText.ellipsize`는 기관·국가·대화·메뉴 화면에 중복됐던 픽셀 폭 제한을 처리한다. 기존 네 화면의 9px 말줄임 여백은 유지한다. 연구 화면의 실제 glyph 폭 기준은 별도 계약이다. GUI2의 기존 셀 정렬, 강조색, 툴팁 문단과 원본 아이콘을 유지한다.

`ScreenTradeAmounts`는 산업 화면에서 무역 수량, 공장 단위의 슬라이더 왕복, 수송선 표기를 분리했다. 서버 무역 규칙을 새로 계산하지 않고 기존 UI 계산을 옮겼다. JUnit은 자원량 1..128의 모든 공장 단계, 마지막 부분 수입, 범위 제한, 기존 수입 교체와 값 부재를 검사한다. 산업 화면의 사용처 없는 메서드 세 개를 제거했다. 산업 화면은 여전히 여러 화면 모드와 설계 흐름을 조정한다. 추가 렌더 분리는 개별 동작과 검증 단위를 정해 진행할 유지보수 후보다.

## 반복 비용과 측정

`UiFlagStack`은 현재 국기의 기존 순서를 유지하고 새 국기를 입력 순서로 붙인다. 중첩 List 탐색을 한 번의 `LinkedHashSet` 차집합으로 바꿔 기존/입력 길이 n·m에 대한 기대 시간을 O(n+m)으로 낮춘다. 입력 크기 O(m)의 임시 색인을 사용한다. 중복·삭제·재삽입·불변 반환값과 배치 경계를 회귀 검사한다.

`ResearchDetails`는 자원 로딩 시 장비 ID 색인을 만들고 상세 캐시와 함께 폐기한다. 반복 조회는 카드 전체 순회에서 기대 O(1)로 바뀌고 추가 메모리는 카드 수에 비례한다. 첫 항목 우선 규칙, 4 MB 입력 제한, 최대 512개 기술·기술당 64개 카드 제한을 유지한다. 렌더링 검사는 모든 카드 조회와 누락/null 조회, 캐시 재로딩을 검사한다. `ResearchLayout.yearGuides`는 반복하던 방향 판정을 한 번만 한다.

`MapSceneMesh`는 변하지 않는 면 좌표를 공유하고 박스마다 sin/cos를 한 번 계산한다. 메시 크기에 비례하는 시간·공간 복잡도는 동일하며 셀 색인·가시 범위·좌표·UV·법선·면 순서를 보존한다. `MapMeshBaseline`은 변경 전 구현을 benchmark 소스셋에 남겨 모든 재질·회전의 float 배열, 타일 경계, 도시, 셀 색인과 큰 메시를 완전히 비교한다. 일반 배포 JAR에는 benchmark가 포함되지 않는다.

Java 25.0.2, Windows 11 amd64, 1 GB heap, warmup 10회, 교차 실행 15회 중앙값이다. 다른 성능 작업이 끝난 뒤 측정했다. 할당량은 현재 스레드의 `ThreadMXBean` 바이트 수이며 렌더 프레임률이나 전체 프로세스 메모리가 아니다.

| 박스 수 | 변경 전 ms | 변경 후 ms | 변경 전 할당 B | 변경 후 할당 B |
|---:|---:|---:|---:|---:|
| 128 | 0.336 | 0.269 | 593,832 | 486,952 |
| 8,192 | 4.649 | 4.044 | 36,045,368 | 29,189,048 |
| 65,536 | 44.059 | 39.603 | 292,044,680 | 237,195,720 |

큰 입력에서 메시 생성 중앙값은 10.1%, 할당은 18.8% 감소했다. 이 클라이언트 변경은 Java만 사용한다. 측정한 작업에서 FFM/Rust 경계와 별도 배포를 추가할 필요를 입증하지 못했으므로 native 구현을 추가하지 않았다. 다른 운영체제·JDK와 실제 플레이의 프레임률 개선은 측정하지 않았다.

## 재현과 검증 범위

Java 25와 먼저 빌드한 HOI-resourcepack을 사용한다. Windows PowerShell에서 다음을 실행한다.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-25.0.2'
.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon
.\gradlew.bat :client:benchmarkMesh --console=plain --no-daemon
```

변경 전 전체 명령은 5분 19초에 통과했다. 최종 공유 내부 이름까지 반영한 전체 명령은 5분 25초에 통과했다. 클라이언트 JUnit 50개와 protocol JUnit 35개에 실패·오류가 없으며 렌더링 캡처는 486개, 그중 GUI2는 260개다. 빌드 시간은 작업 부하와 캐시 영향을 받으므로 성능 비교값으로 사용하지 않는다.

클라이언트 렌더링 GameTest는 실제 Minecraft 클라이언트 렌더러에서 fixture를 실행한다. 운영 서버는 실행하지 않았다. 다중 접속, 서버 팩 다운로드와 실제 운영 캠페인의 상태 동기화는 검증 범위에 없다. `HOI_POLITICS_PREVIEW`가 필요한 선택적 원본 정치 fixture 검사는 이 명령에서 활성화하지 않았다. 일반 정치·외교 fixture와 593개 국가 국기의 배치 검사는 실행한다. 고정 fixture와 TFR 원본의 완전한 동일성이나 원작 전체 기능 구현을 의미하지 않는다.

### GUI2 비교와 배포물

최종 렌더링의 연구 툴팁·생산·전차 설계·주 상세·정치 화면을 직접 확인했다. 변경 전 보관한 GUI2 캡처와 다음 영역을 RGB 픽셀로 비교했으며 모두 차이 0픽셀이었다. 좌표는 2560×1440 이미지에서 `(왼쪽, 위, 오른쪽, 아래)`이고 오른쪽·아래는 제외한다. 배경 세계 전체나 모든 캡처에 대한 자동 동일성 검사는 아니다.

| 캡처 파일 | 비교 영역 | 다른 픽셀 |
|---|---|---:|
| `0076_hoi-research-overview-tooltip-1-gui2.png` | (0, 70, 670, 410) | 0 |
| `0116_hoi-tank-designer-ready-gui2.png` | (492, 327, 2067, 1103) | 0 |
| `0156_hoi-industry-production-gui2.png` | (0, 70, 550, 1440) | 0 |
| `0298_hoi-map-detail-3-2560-gui2.png` | (0, 520, 620, 1440) | 0 |
| `0409_hoi-menu-politics-gui2.png` | (0, 70, 726, 1440) | 0 |

캡처는 `client/build/run/clientGameTest/screenshots`에 생성되며 다음 실행 시 교체되는 로컬 산출물이다. 위 픽셀 비교 결과와 검증 범위는 이 문서에 보존한다. 임시 캡처 사본·JUnit XML·로그·해시 목록은 정리했다. 당시 검증한 `build/libs/hoi-client-0.1.0-SNAPSHOT.jar`는 837,034바이트이며 SHA-256은 `fdd3630b04722f125113fffc9b4576e8ea48193ec92e3e8b30b4d933a63f1bbe`다. Fabric 진입점, 이름을 바꾼 mixin 8개와 중첩 protocol JAR를 확인했다. 이전 타입, benchmark 클래스와 정적 텍스처는 배포 JAR에 없다. protocol main 소스는 HOI와 바이트 단위로 같으며, 기존 `ProtocolMenuViewTest`의 저장소별 추가 검사는 그대로 유지한다.

CodeGraph는 소스 변경 후 동기화했고 `ScreenTradeAmounts` 관계를 실제 소스와 대조했다. `git diff --check`는 통과했다. RTK 결과의 대량 삭제 표시는 아직 stage하지 않은 파일 이름 변경의 한쪽만 포함하므로 코드 축소량으로 사용하지 않는다.

## Shared protocol responsibility names

All shared top-level protocol types now use the `Protocol` package prefix followed by their feature and responsibility, such as `ProtocolResearchPayloads` and `ProtocolIndustryView`. The server and client use the same 33 source files. Existing payload channel IDs, JSON fields, enum values, and wire versions remain unchanged; Java consumers must rebuild against the renamed types. Client screen layouts and interaction rules were not changed by this rename.

Validation used Java 25.0.2 and `.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon` after rebuilding HOI-resourcepack. The command succeeded in 7m 41s: 58 client unit tests and 39 protocol unit tests passed without skips or failures. The renderer run generated 659 captures, including 422 with GUI2 in their names. These are automated fixture rendering checks, not a live multiplayer synchronization test or a new pixel-by-pixel comparison against every original screenshot.

The resulting client JAR is 910,378 bytes, SHA-256 `7607fea727990407cb673663c8b8ac3d46c9cab6eb938aaf8332b1c73c0165ab`. The operational server was not started, restarted, or deployed to. The earlier measurements and image comparisons above remain historical evidence for their own revisions.
