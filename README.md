# HOI-client

Minecraft 26.3 · Fabric · Java 25용 HOI 전용 클라이언트 모드입니다. 국가 메뉴·연구·생산·장교단·교리·군종 작전과 지도 정보를 표시합니다. 게임 계산과 저장은 HOI 서버가 담당하고, UI·초상화·음향은 외부 리소스팩에서 읽습니다.

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
.\gradlew.bat test runGameTest remapJar --console=plain --no-daemon
```

Linux에서는 `./gradlew`, 화면 없는 환경에서는 `xvfb-run -a ./gradlew`를 사용합니다. 다른 팩은 `-PhoiResourcePack=<resources.zip 절대 경로>`로 지정하며 같은 빌드의 manifest와 모든 분할 ZIP이 필요합니다.

캠페인 HUD에서 `F`로 커서를 열고 상단 국기로 국가 메뉴를 엽니다. GUI scale 2가 화면 검증 기준이며 플레이어 배율을 강제하지 않습니다.

## 저장소 안내

`client/`는 화면·입력·렌더링, `protocol/`은 HOI 서버와 공유하는 통신 모듈입니다. JUnit 보고서는 각 모듈의 `build/reports/tests/test/`, 렌더링 캡처는 `client/build/run/clientGameTest/screenshots/`에 생성됩니다. 화면과 구현 범위는 [UI 안내](docs/ui.md), [구조](docs/refactoring.md), [HOI 문서](https://github.com/Kyarurung/HOI/blob/main/docs/README.md)를 참고하세요.

개발 렌더링 검사는 운영 접속·다중 사용자 동기화·팩 다운로드 검증과 구분합니다. 실행 중인 클라이언트 JAR은 교체하지 않습니다.

## 라이선스

by Kiruya_Momochi. 사용자 소유 자체 코드·도구는 [AGPL-3.0-only](LICENSE)입니다. 제3자 코드·의존성, TFR/TFRKP/HOI4 원본과 파생 콘텐츠·자산 및 소유권 불명확 자료는 제외하고 원래 권리와 출처를 유지합니다. 정확한 적용 범위는 [LICENSE-SCOPE.md](LICENSE-SCOPE.md)를 참고하세요.
