# 기여 안내

청지기에 관심을 가져 주셔서 감사합니다. 버그 제보, 기능 제안, 코드 기여 모두 환영합니다.
이 문서는 개발 환경과 코드·테스트·커밋 규칙, 릴리스 절차를 설명합니다.

## 이슈와 풀 리퀘스트

- 버그는 [Issues](https://github.com/typemild/church-steward/issues)에 재현 방법, 기대한 동작, 실제 동작, 프로그램 버전, 운영체제를 적어 주세요. 로그(`logs/app.log`)를 첨부할 때는 개인 정보가 없는지 확인해 주세요.
- 보안 취약점은 공개 이슈로 올리지 말고 [비공개 제보](https://github.com/typemild/church-steward/security/advisories/new)로 알려 주세요.
- 기능을 바꾸거나 더하는 풀 리퀘스트는 먼저 이슈로 방향을 맞춰 주세요. 기능의 기준은 [요구사항 명세서](docs/requirements.md)입니다.
- 풀 리퀘스트 하나에는 한 가지 변경만 담고, 테스트가 통과하는지 확인해 주세요.

## 개발 환경

| 항목 | 내용 |
|---|---|
| JDK | 25 (Gradle 툴체인이 25를 찾습니다) |
| 빌드 | Gradle(Groovy DSL). 저장소의 Gradle Wrapper(`gradlew`)를 씁니다. |
| 프레임워크 | Spring Boot 4, Spring Data JPA(Hibernate), Flyway |
| 데이터베이스 | H2 (AES 암호화 파일) |
| UI | Swing + FlatLaf |
| 테스트 | JUnit 5, Mockito, AssertJ |
| IDE | 자유입니다. 저장소의 포매터 설정은 Eclipse 형식입니다(아래 "코드 스타일" 참조). |

Spring은 DI, 트랜잭션, JPA 컨테이너로만 쓰며 웹 계층은 없습니다. 구조는 [아키텍처](docs/architecture.md)를 먼저 읽어 주세요.

## 빌드, 실행, 테스트

```
./gradlew bootRun          # 프로그램 실행
./gradlew test             # 테스트
./gradlew bootJar          # 실행 가능한 jar (build/libs)
./gradlew spotlessApply    # 코드 서식 맞추기
./gradlew spotlessCheck    # 코드 서식 확인
```

Windows에서는 `gradlew` 대신 `gradlew.bat`을 씁니다.

- `bootRun`으로 실행하면 저장소 루트에 `application.yml`, `data/`, `logs/`가 생깁니다. 암호화된 데이터베이스 키가 들어 있으므로 커밋하지 마세요(`.gitignore`에 있습니다). 처음부터 다시 시작하려면 이 셋을 지웁니다.
- 빌드할 때마다 `src/main/resources/META-INF/resources/license-report.json`(의존성 라이선스 목록)이 다시 만들어집니다. 생성 파일이므로 커밋하지 않습니다.
- `src/main/resources/application.yml`은 Gradle이 `${version}` 등을 치환하는 파일입니다. `$` 문자를 그대로 쓰려면 이스케이프해야 합니다.

## 코드 스타일

- 서식은 Spotless와 Eclipse 포매터 설정(`preserve-wrap-formatter(eclipse).xml`)으로 맞춥니다. 커밋하기 전에 `./gradlew spotlessApply`를 실행해 주세요.
- 로컬 변수 타입 추론(`var`)을 쓰지 않고 타입을 적습니다.
- `if`, `for`, `while`의 본문이 한 줄이어도 중괄호를 씁니다.
- 메서드 체이닝과 빌더는 호출마다 줄을 바꿉니다.
- 타입은 import한 뒤 단순 이름으로 씁니다. 이름이 겹칠 때만 전체 경로를 씁니다.
- 계층 구조, 엔터티 작성 방식, 활동 기록 같은 설계 규칙은 [아키텍처](docs/architecture.md)를, 화면 규칙은 [UI 가이드라인](docs/ui-guideline.md)을 따릅니다.

### 문구와 어투

- UI 문구, 주석, Javadoc, 테스트의 `@DisplayName`, 커밋 메시지는 한국어로 씁니다.
- 주석과 Javadoc은 평서체(`~한다`, `~이다`)로 쓰고 존댓말을 쓰지 않습니다.
  - 예: `회계 연도를 마감한다.`, `로그인 세션에 보관하는 사용자 정보다.`
- 사용자에게 보이는 UI 문구와 예외 메시지는 존댓말로 씁니다.
  - 예: `비밀번호가 일치하지 않습니다. 다시 확인해 주세요.`
- 주석에는 코드만 봐서는 알 수 없는 이유와 결정을 적습니다.

## 테스트

- 기능을 바꾸면 테스트도 함께 추가하거나 고칩니다.
- 이름은 `메서드명_조건_결과`(영문)로 짓고, `@DisplayName`에 테스트의 의도를 한국어 문장으로 적습니다.
- 테스트 대상 메서드마다 `@Nested` 클래스로 묶습니다. 클래스 이름은 `<메서드명>Test`, `@DisplayName`은 `<메서드명> 테스트`입니다.
- 본문은 `// Given`, `// When`, `// Then` 주석으로 단계를 나눕니다.
- `@BeforeEach`는 테스트 리소스를 정리해야 할 때만 씁니다. 공통 데이터는 팩토리 메서드로 만듭니다.
- 팩토리 메서드(`createBudget(...)`처럼 `create...`로 시작)는 테스트에 쓸 객체를 만들기만 합니다. 스텁(`when(...)`)이나 검증을 `given...` 같은 도우미 메서드로 묶지 않고 각 테스트 메서드 안에 직접 씁니다. 테스트가 무엇을 준비하는지 그 메서드만 읽어도 보여야 하고, 도우미를 고쳤을 때 그것을 쓰는 테스트가 왜 깨지는지 알기 어려워지기 때문입니다.
- 테스트 하나는 한 가지 의도만 확인합니다.
  - 상태 확인(`assertThat`)과 행위 확인(`verify`)을 한 테스트에 섞지 않습니다. 단, `ArgumentCaptor`로 저장된 객체를 꺼내 값을 확인하는 것은 함께 써도 됩니다.
  - 서로 다른 두 Mock을 한 테스트에서 `verify`하지 않습니다.
- 리포지토리는 `@DataJpaTest`로 테스트합니다. 메서드 이름으로 만들어지는 쿼리(파생 쿼리)도 테스트를 작성합니다. 이 테스트가 마이그레이션과 엔터티가 맞는지도 함께 확인합니다.
- Spring 컨텍스트 전체를 띄우는 테스트는 `ChurchStewardApplicationTests` 하나입니다. 빈 연결과 설정을 확인하며, 데이터베이스는 내장 H2로, `MainView`는 mock으로 바꿔 띄웁니다. 기동 절차(`ChurchStewardApplication.main`)는 테스트하지 않습니다(이유는 `docs/architecture.md`의 "기동 절차와 데이터베이스 암호화").

## 데이터베이스 변경

스키마는 Flyway 마이그레이션(`src/main/resources/db/migration/V{번호}__{설명}.sql`)으로 관리합니다.

- 엔터티의 칼럼을 바꾸면 마이그레이션을 함께 추가해야 프로그램이 뜹니다(Hibernate는 `validate`만 합니다).
- **배포된 마이그레이션 파일은 고치지 않습니다.** 변경은 새 번호의 파일로 추가합니다.
- 제약 조건과 인덱스의 이름 규칙, enum 칼럼 저장 방식 등은 [아키텍처의 "스키마와 마이그레이션"](docs/architecture.md#스키마와-마이그레이션)을 따릅니다.

## 커밋 메시지

컨벤셔널 커밋 접두어 뒤에 한국어 요약을 붙입니다.

```
feat: 복원 화면의 사용자 ID를 관리자로 고정
fix: CSV 내보내기의 수식 주입 방지
refactor: AppSettingManager의 경로별 읽기/쓰기 락을 synchronized로 단순화
test: …
docs: …
style: …
build: …
```

## 문서

- 기능의 동작이 바뀌면 [요구사항 명세서](docs/requirements.md)를 함께 고칩니다.
- 구조나 설계 결정이 바뀌면 [아키텍처](docs/architecture.md)를, 화면 규칙이 바뀌면 [UI 가이드라인](docs/ui-guideline.md)을 함께 고칩니다.

### README 스크린샷

README의 화면은 `docs/screenshots/`에 있고, 예시 데이터(가상의 이름과 금액)는 `docs/screenshots/sample-data/`에 있습니다. 데이터의 날짜는 2025-01-01 ~ 2026-10-18로 고정되어 있습니다.

| 파일 | 내용 |
|---|---|
| `contacts.csv` | 봉헌자/수취인 24명(구분, 이름, 비고, 상태) |
| `2025-budget.csv`, `2026-budget.csv` | 회계 연도별 수입/지출 예산 |
| `2025-records.csv`, `2026-records.csv` | 회계 연도별 수입/지출 내역 |

다시 캡처할 때는 다음 순서로 데이터를 넣습니다. 배포본과 같은 모습이 되도록 Windows에서 `packageWindows`로 만든 실행 파일을 빈 폴더에 풀어 실행합니다.

1. 관리자 계정을 만듭니다.
2. 설정 > 회계 연도 관리에서 "2026년"(2026-01-01 ~ 2026-12-31)과 "2025년"(2025-01-01 ~ 2025-12-31)을 추가합니다.
3. 데이터 관리 > 관계자에서 봉헌자/수취인 파일(contacts.csv)를 가져옵니다.
4. 데이터 관리 > 예산에서 회계 연도마다 예산 파일(0000-budget.csv)을 가져옵니다.
5. 데이터 관리 > 수입/지출에서 회계 연도마다 내역 파일(0000-records.csv)을 가져옵니다.
   2026년 수입/지출 내역을 가져올 때 "매핑" 단계에서 화면을 캡처합니다(import.png).
6. 설정 > 회계 연도 관리에서 "2025년"을 마감하고, 사이드바에서 "2026년"을 고릅니다.

| 파일 | 화면과 상태 |
|---|---|
| `dashboard.png` | 대시보드 "최근 현황" |
| `financial-records.png` | 수입/지출 관리 "예산별 보기", 월간 2026년 10월, 두 표에서 행 몇 개 선택 |
| `record-entry.png` | 수입/지출 관리 "건별 보기", 수입 추가 다이얼로그, 봉헌자에 "김"을 입력하고 김찬양에 포커스해 그룹 서브 목록이 열린 상태(저장하지 않음) |
| `budget.png` | 예산 관리, 모두 펼침 |
| `report-weekly.png` | 주간 회계 결산 보고서, 2026-10-11 ~ 10-17 |
| `report-settlement.png` | 회계 연도 수입/지출 결산 보고서, 사이드바에서 "2025년" 선택 |
| `import.png` | 데이터 관리 > 수입/지출 가져오기의 매핑 단계 |
| `update-backup.png` | [업데이트 안내](docs/update.md)용. 데이터 관리 > 백업/복원 탭 |
| `update-init.png` | 업데이트 안내용. 빈 폴더에서 처음 실행했을 때의 "관리자 계정 설정" 화면(입력 없음) |
| `update-restore.png` | 업데이트 안내용. "백업에서 복원" 창, 백업 파일을 고른 뒤(계정 이름이 드러나지 않는 경로) |

창 크기는 내용이 잘리지 않을 만큼 화면마다 알맞게 조절합니다. 마우스 커서가 찍히지 않게 하고, 모든 화면을 같은 디스플레이 배율로 찍습니다.

## 릴리스 (관리자용)

1. `build.gradle`의 `version`을 올립니다(예: `1.1.0`). jpackage가 숫자로만 된 버전을 받으므로 `-SNAPSHOT` 같은 접미어를 붙이지 않습니다.
2. H2 덤프 호환성 테스트용 고정 덤프를 만듭니다. 지금의 H2 버전 파일이 없을 때만 `src/test/resources/h2-dump-fixtures`에 `h2-{H2 버전}.sql`이 생기며, 생기면 함께 커밋합니다. 이후 H2를 올려도 이 버전에서 만든 백업을 되살릴 수 있는지 `H2DumpCompatibilityTest`가 확인합니다.
   ```
   ./gradlew generateH2DumpFixture
   ```
3. 커밋하고 같은 버전의 서명된 태그를 push합니다. 커밋과 태그는 GitHub에 서명 키(Signing Key)로 등록한 키로 서명해, GitHub에서 Verified로 표시되게 합니다.
   ```
   git tag -s v1.1.0 -m "청지기 1.1.0"
   git push origin v1.1.0
   ```
4. GitHub Actions의 "릴리스" 워크플로(`.github/workflows/release.yml`)가 Windows에서 테스트한 뒤 다음 두 파일을 Releases에 **초안**으로 올립니다. 태그와 `version`이 다르면 실패합니다.
   - `church-steward-<버전>.jar`
   - `ChurchSteward-<버전>-windows.zip` (jpackage로 만든, Java 런타임이 포함된 실행 폴더)

   워크플로가 실패하면 태그를 지우고, 고친 내용을 커밋해 push한 뒤 3번부터 다시 합니다.
   ```
   git push origin :v1.1.0
   git tag -d v1.1.0
   ```
5. 초안의 Windows용 zip을 Windows PC에 내려받아 확인합니다. OneDrive와 동기화되지 않는 폴더(예: `C:\ChurchSteward`)에 압축을 풀고 다음을 봅니다.
   - `ChurchSteward.exe`가 실행되고 아이콘이 붙는지, "프로그램 정보"의 버전이 새 버전인지
   - [업데이트 안내](docs/update.md)의 절차(이전 버전의 백업에서 복원)대로 이전 버전의 데이터를 옮겼을 때 데이터가 그대로인지, 되돌리기 절차대로 이전 버전으로 돌아갈 수 있는지
   - 화면 문구나 절차가 바뀌었으면 업데이트 안내도 함께 고칩니다.
   - 보고서의 PDF 저장과 인쇄, CSV(MS949) 가져오기, 백업과 복원 예약
6. 초안의 릴리스 노트를 정리하고 게시합니다. 워크플로가 노트 맨 위에 업데이트 안내 링크를 넣으므로 지우지 않습니다. 게시하기 전까지 초안은 다른 사람에게 보이지 않습니다.

Windows용 zip은 Windows PC에서 직접 만들 수도 있습니다(`gradlew.bat packageWindows`, 결과는 `build/distributions`). jpackage는 실행하는 OS용 결과물만 만들기 때문에 다른 OS에서는 이 태스크가 실패합니다.
