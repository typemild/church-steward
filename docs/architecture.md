# 아키텍처

청지기의 구조와, 코드만 봐서는 의도를 알기 어려운 설계 결정을 정리한 문서다.
기능의 기준은 [요구사항 명세서](requirements.md), 화면 규칙은 [UI 가이드라인](ui-guideline.md)에 있다.

## 개요

Spring Boot 4 + Spring Data JPA 위에 Swing(FlatLaf) UI를 얹은 데스크톱 프로그램이다. 데이터베이스는 암호화된 파일 기반 H2이고, Java 25 툴체인과 Gradle(Groovy DSL)로 빌드한다.
웹 계층은 없으며 Spring은 DI, 트랜잭션, JPA 컨테이너로만 쓴다.

## 패키지 구조

```
org.church.steward
├── domain/<기능>/         controller, service (+ service/impl), repository, entity, dto, mapper, specification, transformer
├── infrastructure/        config (DatabaseManager, AppSettingManager, AppHome, SecurityConfig), backup (BackupManager),
│                          spreadsheet (SpreadsheetReader/Writer), security (세션, PasswordPolicy), annotation
├── ui/                    view/<화면>, component/{button,field,label,panel,table}, util
└── util/
```

기능 패키지: `activitylog`, `backup`, `budget`, `contact`, `dashboard`, `datatransfer`, `financialrecord`, `fiscalyear`, `programinfo`, `report`, `user`.

enum은 그 값을 필드로 갖는 클래스와 같은 패키지에 둔다. 엔터티의 필드이면 `entity`(예: `ContactType`), DTO의 필드이면 `dto`(예: `ImportItemStatus`, `ReportType`. `datatransfer`와 `report`는 엔터티가 없다)다. 제약 클래스(`*Constraints`)는 제약하는 엔터티와 같은 패키지(`entity`)에 둔다.

### 호출 방향

`ui/view` → `domain/*/controller` → `service` → `repository` 순서로만 호출한다. 화면은 서비스나 리포지토리를 직접 호출하지 않으며, 컨트롤러 경계를 넘는 것은 DTO뿐이다. 화면은 `entity` 패키지의 enum과 `*Constraints`만 참조할 수 있고 엔터티 클래스는 참조하지 않는다.

- `@UiView` / `@UiController`(`infrastructure/annotation`)는 `@Component` 스테레오타입이다. 화면은 생성자 주입을 받는 Spring 빈이다.
- 컨트롤러는 권한을 검증한 뒤 서비스 인터페이스로 위임하는 얇은 계층이다. 관리자 전용 기능(사용자 관리, 백업/복원, 데이터 내보내기·가져오기)의 컨트롤러는 메서드마다 먼저 `AdminAccessChecker#checkAdmin()`을 호출하며, 로그인한 사용자가 관리자가 아니면 `AccessDeniedException`이 난다. 메뉴를 관리자에게만 보여주는 것과 별개로 컨트롤러 경계에서 한 번 더 막기 위함이다. 누구나 쓰는 기능(예: 본인 비밀번호 변경)은 확인하지 않는다. 관리자 전용 메서드를 새로 만들면 권한 확인을 넣고, 권한이 없을 때 막히는지 컨트롤러 테스트로 확인한다.
- 서비스는 `XxxService` + `impl/XxxServiceImpl`로 구성하며, 클래스에 `@Transactional`을, 조회 메서드에 `@Transactional(readOnly = true)`를 붙인다. 업무 규칙의 검증도 서비스가 맡는다(아래 "검증과 오류 메시지" 참조).
- 엔터티는 Lombok `@Getter`, protected 기본 생성자, `@Builder` 생성자, 명시적인 `update(...)` 메서드를 쓴다(setter 없음).
- `UserSessionHolder`는 로그인한 사용자를 보관하는 싱글톤 빈이다. `User` 엔터티가 아니라 ID와 역할만 담은 불변 객체 `LoggedInUser`를 보관한다. 비밀번호 해시를 메모리에 남기지 않고, 트랜잭션 밖의 엔터티가 쓰이거나 오래된 값으로 저장되는 일을 막기 위함이다. 이 빈은 컨트롤러를 거치지 않고 화면이 직접 읽어도 된다(웹으로 치면 클라이언트도 갖고 있는 자신의 세션 정보다).

### 검증과 오류 메시지

입력값과 업무 규칙의 검증은 서비스가 최종적으로 책임진다. 필수 값과 글자 수, 이름 중복, 회계 연도 기간의 겹침, 마감된 회계 연도의 수정 금지 같은 규칙이 여기에 해당한다. 입력 다이얼로그가 같은 검사를 먼저 하기도 하지만 그것은 사용자를 빨리 돕기 위한 것이고, 화면을 거치지 않는 호출(가져오기 등)까지 막는 기준은 서비스다.

글자 수의 상한은 칼럼 길이와 같으며, 기능마다 엔터티 옆의 제약 클래스(`ContactConstraints`, `FinancialRecordConstraints`, `BudgetConstraints`, `FiscalYearConstraints`, `UserConstraints`)에 상수로 둔다. 엔터티의 `@Column(length = …)`, 서비스의 검증, 가져오기 분석기, 화면 입력란의 최대 길이(`setMaxLength`)가 모두 이 상수를 쓴다. 칼럼 길이를 바꾸면 마이그레이션과 함께 이 상수를 고친다.

규칙을 어긴 요청은 `IllegalArgumentException`으로 거절한다. 이 예외의 메시지는 개발자를 위한 진단 문구가 아니라 **사용자에게 그대로 보여 줄 문장**이다.

1. 서비스가 `throw new IllegalArgumentException("마감된 회계 연도는 수정할 수 없습니다.")`처럼 던진다. 런타임 예외이므로 트랜잭션은 롤백되어 아무것도 바뀌지 않는다.
2. 컨트롤러는 예외를 잡지 않고 그대로 올려 보낸다.
3. 화면은 컨트롤러를 `AsyncTask`로 호출하므로 예외는 백그라운드 스레드에서 난다. `AsyncTask`가 그 예외를 꺼내 화면의 `onError`로 넘긴다.
4. 화면은 메시지를 메시지 창에 보여 준다. 대개 "수정 실패: " 같은 동작 이름을 앞에 붙인다. 입력 다이얼로그는 닫지 않고 입력값을 유지해서 사용자가 고쳐 다시 저장할 수 있게 한다(예: `FiscalYearEntryDialog`).

그래서 메시지는 다음처럼 쓴다.

- 존댓말로, 무엇이 문제인지(필요하면 어떻게 하면 되는지) 사용자가 알아들을 수 있게 쓴다. 예: "회계 연도의 이름을 입력해주세요.", "마감된 회계 연도는 수정할 수 없습니다."
- 내부 ID, 클래스 이름, SQL 같은 기술적인 내용을 넣지 않는다.
- 화면이 예외의 종류에 따라 다르게 대응해야 할 때만 전용 예외를 만든다. 예를 들어 `AccessDeniedException`(관리자 권한 없음), `InvalidPasswordException`(비밀번호 입력란에 다시 포커스), `BackupOutdatedException`(백업을 다시 받게 함), `ImportStateChangedException`(다시 분석해 검토 단계로 돌아감)이 있다.

### 매퍼

엔터티와 DTO 사이의 변환은 기능마다 `domain/<기능>/mapper`의 `XxxMapper`가 맡는다. MapStruct 같은 생성 도구를 쓰지 않고 직접 작성한 `@Component`이며, DTO의 빌더로 값을 옮겨 담는다.

- **서비스만 호출한다.** 컨트롤러와 화면은 매퍼를 쓰지 않고, 서비스가 돌려준 DTO만 받는다. 엔터티가 컨트롤러와 화면으로 나가지 않게 하기 위함이다. 엔터티 패키지에 있는 enum(`FinancialRecordType`, `BudgetType`, `ContactType`, `ActivityType`, `UserRole`)은 DTO의 값으로 그대로 쓴다.
- **트랜잭션 안에서 변환한다.** 연관은 모두 LAZY이므로, 매퍼가 연관을 따라가는 값(예: `Budget#getHierarchyName()`으로 만드는 전체 예산명 "예산A > 예산B", 내역의 관계자 목록)은 서비스의 트랜잭션 안에서 DTO로 바꿔야 한다. 목록 조회라면 그 연관을 리포지토리에서 함께 가져와 N+1을 피한다(아래 "조회" 참조).
- **연관은 ID와 표시용 값으로 푼다.** DTO는 엔터티를 품지 않는다. 예를 들어 `FinancialRecordMapper`는 회계 연도와 예산을 ID로, 예산은 전체 예산명으로, 관계자는 ID 목록과 기록 당시의 이름(스냅샷 `FinancialRecordContact.contactName`) 목록으로 담는다.
- **계산은 하지 않는다.** 금액 합계, 비율, 깊이처럼 집계나 조회가 필요한 값은 서비스가 계산해 인자로 넘기고, 매퍼는 엔터티의 값과 함께 옮겨 담기만 한다(예: `BudgetMapper#toDto(budget, actualAmount)`, `ReportMapper#toBudgetExecutionDto`). 그래서 매퍼는 리포지토리나 다른 서비스를 주입받지 않는다.
- **용도별로 메서드를 나눈다.** 같은 엔터티라도 화면마다 필요한 값이 다르면 DTO와 변환 메서드를 따로 둔다(예: `ContactMapper`의 `toDto`, `toSummaryDto`, `toAutocompleteDto`). 대시보드와 보고서는 자체 엔터티가 없으므로 `DashboardMapper`, `ReportMapper`가 예산과 내역 엔터티를 그 화면의 DTO로 바꾼다.
- **null을 받으면 null을 돌려준다.**
- **DTO → 엔터티 변환은 드물다.** 생성은 대개 서비스가 검증한 뒤 엔터티의 빌더로 직접 만들고, 수정은 엔터티의 `update(...)`로 한다. `ContactMapper#toEntity`처럼 DTO를 그대로 새 엔터티로 옮겨도 되는 경우에만 매퍼가 만든다.
- **예외: 활동 기록.** `ActivityLogMapper`는 항목 목록(`ActivityLogFieldDto`)을 `ActivityLog.description`의 JSON으로 저장하고, 조회할 때 다시 풀어 준다. JSON의 형식(키 이름과 구조)은 이 매퍼만 알며, 서비스와 화면은 항목 목록만 다룬다([활동 기록의 저장 형식](#저장-형식) 참조). JSON을 다루기 위해 이 매퍼만 `ObjectMapper`를 주입받는다.

## 프로그램 폴더

설정 파일(`application.yml`), 데이터베이스(`data/`), 로그(`logs/app.log`)는 모두 **프로그램 폴더**에 생긴다. 이 문서에서 `./application.yml`, `./data/`의 `./`는 프로그램 폴더를 뜻한다.

프로그램 폴더는 `AppHome`(`infrastructure/config`)이 정한다.

| 실행 방식 | 프로그램 폴더 |
|---|---|
| jpackage로 만든 실행 파일 | 실행 파일이 있는 폴더(시스템 속성 `jpackage.app-path`) |
| jar | jar가 있는 폴더 |
| IDE, `bootRun`, 테스트 | 작업 디렉터리(`bootRun`에서는 저장소 루트) |

Windows 배포본은 압축을 푼 폴더 하나에 프로그램과 데이터가 함께 있는 방식이므로, 바로 가기의 "시작 위치"와 무관하게 데이터가 그 폴더에 모여야 한다.

### 로깅

로깅 설정은 `application.yml`의 `logging.*`가 아니라 `src/main/resources/logback.xml`에 둔다. `logback-spring.xml`이나 `logging.*`는 Spring 컨텍스트가 뜰 때에야 적용되는데, 로그인·최초 실행·복원·마이그레이션은 그 전에 일어나기 때문이다. 같은 이유로 Spring Boot의 `base.xml` 등을 include하지 않는다.

Logback은 `main`보다 먼저(로거를 가진 클래스가 초기화될 때) 설정을 읽으므로, 로그 파일 경로는 `logback.xml`의 `<define>`이 `LogFilePropertyDefiner`로 받는다. 그래서 `AppHome`은 로그를 남기지 않는다.

테스트는 `src/test/resources/logback-test.xml`(콘솔만, 우리 코드 DEBUG, 나머지 WARN)이 우선 적용된다. 리포지토리 테스트의 SQL은 `@DataJpaTest`가 켜는 `spring.jpa.show-sql`이 출력하므로 `org.hibernate.SQL` 로거는 따로 켜지 않는다(켜면 같은 SQL이 두 번 찍힌다).

## 기동 절차와 데이터베이스 암호화

DB 비밀번호는 로그인한 뒤에야 알 수 있으므로, `ChurchStewardApplication.main`은 Spring 컨텍스트를 띄우기 *전에* 다음을 수행한다.

1. `DatabaseManager.isInitializationRequired()`가 `./data/church_steward.mv.db`가 있는지 확인한다. 없으면 `DatabaseInitView`로 관리자 계정 정보를 받고, `DatabaseManager.initialize`가 H2(`CIPHER=AES`)의 파일/사용자 비밀번호를 무작위로 만든 뒤, 관리자 계정과 첫 회계 연도(이름 "{올해}년", 기간 올해 1월 1일 ~ 12월 31일)를 만들기 위해 Spring 컨텍스트를 잠깐 띄웠다가 닫는다.
2. `LoginView`로 계정 정보를 받는다 → `DatabaseManager.loadDatabasePassword`가 DB 비밀번호를 복호화한다 → `SpringApplication.run`으로 실제 컨텍스트를 띄운다 → `UserService.login` → `MainView` 빈을 표시한다.

`DatabaseManager`, `AppSettingManager`, `BackupManager`는 `main`에서 만든 정적 싱글톤이며, 애플리케이션 클래스의 `@Bean` 메서드로 Spring에 노출된다(`AppSettingManager`는 노출하지 않는다). `DataSource` 빈은 복호화된 비밀번호로 만든다. 이 클래스들은 `@Component`가 아니다.

기동 절차 자체는 테스트하지 않는다. 화면이 받는 콜백(초기화, 로그인, 복원 동작)을 별도 클래스로 떼어 내면 화면 없이 테스트할 수는 있지만, 대부분 mock으로 호출 순서를 확인하는 데 그쳐 구현을 옮겨 적는 테스트가 되기 때문이다. 대신 각 단계의 실제 동작(암호화 DB 생성, 키 복호화, 마이그레이션, 복원)은 `DatabaseManagerTest`와 `BackupManagerTest`가, Spring 구성 전체는 `ChurchStewardApplicationTests`가 확인한다. 이 테스트는 `dataSource()`를 내장 H2로 바꾸고(`@AutoConfigureTestDatabase`), headless에서 만들 수 없는 `MainView`(`JFrame`)를 mock으로 바꿔 컨텍스트를 띄운다.

### 키 구조

무작위 DB 비밀번호는 사용자마다 `./application.yml`의 `database-manager.<userId>.{file-password,user-password,salt}`에 저장되며, 각 사용자의 비밀번호에서 PBKDF2로 유도한 키로 AES-GCM 암호화되어 있다.

- 사용자 생성, 비밀번호 변경, 사용자 삭제 때는 `User` 행(`PasswordEncoder`의 Argon2id 해시)과 YAML 항목(`DatabaseManager.saveOrUpdateUserPassword` / `removeUserPassword`)을 함께 갱신해야 한다.
- 비밀번호가 틀리면 GCM 복호화가 실패하고 `InvalidPasswordException`이 된다. 보안을 위해 실패 사유를 구분하지 않고 같은 예외 하나로 답한다.
- 비밀번호를 모두 잊으면 데이터를 열 방법이 없다. 관리자는 다른 사용자의 비밀번호를 새로 정할 수 있지만(관리자의 세션이 DB 키를 이미 알고 있으므로), 관리자 비밀번호를 다시 정할 방법은 없다.

### 설정 파일 편집

`AppSettingManager`는 SnakeYAML 노드를 직접 편집해 `./application.yml`을 고친다. 주석과 항목 순서를 유지하고, 임시 파일에 쓴 뒤 원자적으로 교체하며, 인스턴스 단위로 `synchronized`한다(같은 파일은 인스턴스 하나로만 다룬다). `update(editor -> …)` 콜백 안에서는 인자로 받은 `Editor`만 써야 하며, 매니저 자신의 `get/set/remove`를 호출하면 예외가 난다.

## 스키마와 마이그레이션

스키마는 Flyway 마이그레이션(`src/main/resources/db/migration`, `V{번호}__{설명}.sql`)으로 만들고 올린다. Hibernate는 `ddl-auto: validate`로 엔터티와 스키마가 맞는지만 확인하므로, 엔터티의 칼럼을 바꾸면 마이그레이션도 함께 추가해야 기동된다.

- **실행 위치**: 마이그레이션은 Spring 컨텍스트가 아니라 `DatabaseManager`가 Flyway API로 직접 실행한다. 최초 실행은 `initialize`가 DB 파일을 만든 직후, 로그인은 `loadDatabasePassword` 다음 `SpringApplication.run` 전의 `migrate()`, 복원은 `restore`가 덤프를 임시 DB에 되살린 직후(파일을 바꾸기 전)다. 그래서 복원 중 마이그레이션이 실패해도 기존 데이터는 그대로다.
- **마이그레이션 전 복사본**: H2는 DDL을 문장마다 바로 커밋해서 중간에 실패하면 되돌릴 수 없다. 그래서 `migrate()`는 올릴 마이그레이션이 있으면 먼저 DB 파일을 `./data/before-migrate-{일시}/`에 복사해 두고, 실패하면 그 위치를 오류 메시지에 적는다.
- **Spring Boot의 Flyway 자동 구성**(`spring.flyway.enabled: true`)은 켜 둔다. 실행 환경에서는 이미 마이그레이션이 끝나 있어 아무 일도 하지 않고, `@DataJpaTest`에서는 이것이 내장 DB에 스키마를 만든다. 따라서 리포지토리 테스트가 마이그레이션과 엔터티의 일치도 확인한다.
- **백업과의 관계**: `flyway_schema_history` 표도 덤프에 들어가므로, 과거 버전의 백업은 복원할 때 그 버전부터 이어서 올라간다.
- **enum은 VARCHAR로 저장한다**: H2에서 Hibernate가 enum 칼럼을 `ENUM(...)` 타입으로 만들면 상수를 추가할 때마다 스키마를 바꿔야 한다. 그래서 `@Enumerated(EnumType.STRING)`에 `@JdbcTypeCode(SqlTypes.VARCHAR)`와 `length`를 함께 붙이고 허용 값을 DB 제약으로 묶지 않는다.
- **제약 조건 이름**은 `PK_{표}`, `FK_{표}_{대상}`, `UK_{표}_{칼럼}`으로 직접 붙인다(나중에 마이그레이션에서 이름으로 다루기 위함). 인덱스는 `IX_{표}_{칼럼…}`이다.
- **인덱스**: H2는 외래 키마다 참조 칼럼의 인덱스를 자동으로 만들므로 단일 FK 칼럼 인덱스는 만들지 않고, 리포지토리 쿼리의 조건과 정렬에 맞춘 복합 인덱스만 둔다. 쿼리의 조건이나 정렬을 바꾸면 인덱스도 함께 본다.

## 전체 데이터 백업/복원

`infrastructure/backup`의 `BackupManager`가 담당한다.

- **백업 파일**(확장자 `csbackup`)은 ZIP이며 `manifest.json`(형식 버전, 프로그램 버전, 일시, 사용자별 DB 접근 키)과 `database.sql`(H2 `SCRIPT` 덤프)을 담는다. 덤프는 DB 파일 비밀번호로 암호화하고(`DatabaseManager#writeDump`), 그 비밀번호는 사용자별 DB 접근 키 안에 각 사용자의 비밀번호로 암호화되어 있다. 그래서 별도의 백업 암호가 없고, 복원에는 백업한 시점의 계정과 비밀번호가 필요하다.
- **파일 복사가 아니라 덤프를 쓰는 이유**: H2의 파일 형식은 버전 사이에 호환되지 않은 전례가 있고, Spring Boot를 올리면 H2도 올라간다. `H2ScriptRoundTripTest`는 같은 H2 안에서 암호화된 DB의 덤프 왕복을 확인한다. 과거 H2 버전이 만든 덤프를 읽을 수 있는지는 `H2DumpCompatibilityTest`가 확인한다. 이 테스트는 H2 버전마다 만들어 커밋해 둔 고정 덤프(`src/test/resources/h2-dump-fixtures/h2-{H2 버전}.sql`)를 모두 `DatabaseManager#runDump`로 되살리고, 같은 데이터(`H2DumpFixtureData`)를 지금의 H2에 새로 넣은 결과와 표 구조까지 비교한다. 고정 덤프는 실제 스키마와 무관한 임의의 구조(실제 스키마의 자료형, 제약 조건, 인덱스 형태만 흉내 냄)라서 마이그레이션을 실행하지 않으며, 스키마가 바뀌어도 고칠 필요가 없다. 고정 덤프는 릴리스할 때 `./gradlew generateH2DumpFixture`로 만든다(그 H2 버전의 파일이 이미 있으면 만들지 않는다). 과거 버전의 실제 스키마로 만든 백업이 마이그레이션으로 현재 스키마까지 올라가는지는 아직 확인하는 테스트가 없다(첫 V2 마이그레이션을 추가할 때 만든다).
- **백업**은 실행 중에 한다: `BackupController` → `BackupService`(`domain/backup`) → `BackupManager#createBackup`. 성공한 뒤에 활동 기록(`ActivityType.BACKUP`)를 남기므로 그 기록은 방금 만든 백업 파일에는 들어 있지 않다.
- **복원**은 DB 파일을 교체하므로 Spring 컨텍스트가 없을 때만 할 수 있다. 데이터 관리 화면은 복원을 예약만 하고(`./application.yml`의 `scheduled-restore-file`) 프로그램을 끝낸다. 다음 실행 때 `main`이 로그인 전에 `DatabaseRestoreView`를 띄운다(예약은 한 번만 유효하며 취소하면 기존 데이터로 로그인한다). 최초 실행 화면의 "백업에서 복원"도 같은 다이얼로그를 쓴다.
- **복원 절차**(`DatabaseManager#restore`): 덤프를 임시 DB 파일에 먼저 되살려 Flyway로 현재 스키마까지 올리고, 성공하면 기존 DB 파일과 설정 파일을 `./data/before-restore-{일시}/`로 옮긴 뒤 되살린 파일을 제자리에 놓고 `database-manager` 항목을 백업의 키로 바꾼다. 되살리거나 스키마를 올리다가 실패하면 기존 데이터는 그대로다.
- **복원의 활동 기록**: 복원 동작(`ChurchStewardApplication#restoreAction`)은 DB를 교체한 뒤 복원한 DB로 Spring 컨텍스트를 잠깐 띄워 `BackupService#logRestore`로 `ActivityType.RESTORE`를 남기고 닫는다(최초 실행의 관리자 생성과 같은 방식). 로그인 세션이 없으므로 복원한 계정 ID를 기록자로 넘긴다. 백업은 관리자만 할 수 있으므로 복원 화면의 계정은 "관리자"로 고정하고 비밀번호만 받는다. 복원은 이미 끝났으므로 기록에 실패해도 복원은 성공으로 보고, 완료 문구에 실패만 알린다.
- **버전**: 프로그램보다 새 버전에서 만든 백업은 거부한다(`ProgramVersion#isNewer`). 과거 버전의 백업은 복원하면서 현재 스키마까지 올린다.

## 도메인 규칙

### 회계 연도

- **이름과 기간으로 정의한다**: `FiscalYear`의 PK는 연도와 무관한 자동 증가 ID이고 "연도" 값은 없다. 대신 이름(필수, 30자 이내, 중복 불가)을 가지므로 한 해에 회계 연도를 둘 이상 둘 수 있다. 서비스, 컨트롤러, DTO는 회계 연도를 ID(`Long fiscalYearId`)로 주고받고, `Budget`과 `FinancialRecord`는 `FiscalYear`를 `@ManyToOne`으로 참조한다. 화면, 보고서, 활동 기록에는 이름을 적는다(`FiscalYearTransformer`).
- **순서**: 시작일로 정한다. 목록은 시작일이 늦은 것부터이고, "가장 최근 회계 연도"는 시작일이 가장 늦은 것, "직전 회계 연도"는 시작일이 더 앞선 것 중 가장 최근 것이다(사이에 빈 기간이 있어도 직전으로 본다).
- **기간**: 달력의 해와 어긋나도 되고 길이도 제한하지 않는다. 다른 회계 연도와 하루도 겹칠 수 없지만, 사이의 빈 기간은 허용한다(중간 회계 연도를 삭제하면 어차피 생긴다). 수입/지출의 날짜는 속한 회계 연도의 기간 안에 있어야 한다(`FinancialRecordServiceImpl`). 기간을 수정할 때는 등록된 내역이 기간 밖에 놓이지 않도록, 시작일은 가장 이른 내역의 날짜 이전, 종료일은 가장 늦은 내역의 날짜 이후로만 지정할 수 있다(`FiscalYearServiceImpl#updateFiscalYear`).
- **마감**: `FiscalYearService#closeFiscalYear`는 그 시점의 총 수입, 총 지출, 차액을 집계 쿼리로 계산해 저장하고, `reopenFiscalYear`(마감 취소)는 세 값을 다시 null로 만든다. 마감 여부는 화면에서만 막지 않고 서비스가 검증한다. 마감된 회계 연도는 이름·기간 수정과 삭제가 막히고, 그 회계 연도의 예산 추가/수정/삭제/순서 변경(`BudgetServiceImpl`)과 수입/지출 추가/수정/삭제(`FinancialRecordServiceImpl`)도 막힌다. 예외로, 마감된 회계 연도를 원본으로 한 예산 복사는 원본을 바꾸지 않으므로 허용한다.
- **회계 연도 일치**: 수입/지출의 예산은 내역과 같은 회계 연도의 예산이어야 하고, 하위 예산은 상위 예산과 같은 회계 연도에만 추가할 수 있다.

### 예산

- 예산은 계층형이며, 수입/지출은 말단(leaf) 예산에만 등록할 수 있다. 예산 필터는 `BudgetRepository.findAllDescendantIds`(네이티브 재귀 CTE)로 하위 예산까지 넓힌다.
- **예산명**은 50자 이내이고 `<`, `>`를 쓸 수 없다(`BudgetServiceImpl#validateName`). 전체 예산명 "예산A > 예산B"의 구분 문자와 섞이지 않게 해서, 가져오기에서 전체 경로가 적힌 칸을 `>`로 나눌 수 있게 하기 위함이다.
- **진행 현황**: `BudgetService#getBudgetsWithActualAmount`는 예산마다 그 예산과 하위 예산에 등록된 내역의 금액 합계를 `BudgetDto.actualAmount`에 담는다. 예산 관리 화면만 이 조회를 쓰고, 예산 선택 다이얼로그 등은 `actualAmount`가 null인 `getBudgetsByFiscalYearAndType`을 쓴다.

### 수입/지출과 관계자

- 수입과 지출은 하나의 엔터티 `FinancialRecord`(표 `FINANCIAL_RECORD`)에 저장하고 `FinancialRecordType`(INCOME/EXPENSE)으로 구분한다. 구분은 생성할 때 정해지며 수정으로 바꿀 수 없다.
- 관계자(수입의 봉헌자, 지출의 수취인)는 조인 엔터티 `FinancialRecordContact`에 저장한다. "수입은 1명 이상, 지출은 정확히 1명" 제약은 스키마가 아니라 `FinancialRecordServiceImpl`의 검증으로 지킨다.
- **이름 스냅샷**: 기록의 불변성을 위해 이름을 복사해 둔다. 예를 들어 `FinancialRecordContact.contactName`은 저장 시점의 `Contact` 이름이므로, 이후 이름이 바뀌거나 삭제되어도 과거 기록은 변하지 않는다.
- **관계자 그룹 자동 생성**: 그룹을 직접 만들거나 구성원을 고치는 기능은 없다. 수입을 저장할 때 봉헌자가 2명 이상이면 `ContactGroupService#createGroupIfAbsent`가 구성원이 정확히 일치하는 그룹이 없는 경우에만 그룹을 만든다. 이후 수입을 수정하거나 삭제해도 그룹은 지우지 않으며, 그룹은 자동 완성 목록의 휴지통으로만 삭제한다. 예외로, 관계자를 삭제하면 그 관계자를 구성원에서 빼고, 남은 구성원이 1명 이하이거나 다른 그룹과 구성원이 같아진 그룹은 함께 지운다.

### 조회

- **연관 조회와 N+1**: 연관은 모두 LAZY다. 목록 조회에서 함께 쓰는 연관은 리포지토리 메서드에 `@EntityGraph`(Specification은 `root.fetch`)로 함께 가져오고, 나머지 지연 로딩은 `hibernate.default_batch_fetch_size`가 `IN` 조건으로 묶는다. 건수 제한이 있는 쿼리(`findTop100…`)에는 컬렉션을 fetch join하지 않는다(Hibernate가 제한을 메모리에서 적용해 전체를 읽기 때문이다).
- **동적 검색**은 JPA `Specification`(`FinancialRecordSpecification`)으로 한다. 관계자 조건은 `EXISTS` 상관 서브쿼리가 아니라 `ID IN (관계자의 내역 ID)`로 건다. 관계자로 검색할 때(기부금영수증 발급 등)는 기간이 회계 연도 전체라 날짜로 거의 걸러지지 않으므로, 관계자 쪽 인덱스로 내역 ID를 먼저 찾게 하기 위함이다.
- `dashboard`와 `report`는 자체 엔터티가 없고 `financialrecord`와 `budget`을 집계한다. 집계 대상은 회계 연도에 속한 내역 전체다. 월별 집계는 회계 연도 기간의 시작월부터 종료월까지를 내역이 없어도 항상 포함한다.

## 활동 기록

데이터를 바꾸는 모든 서비스 메서드는 `ActivityLogService`로 활동 기록을 남긴다.

- 생성 → `logCreation(saved)`, 삭제 → `logDeletion(entity)`
- 수정 → 값을 바꾸기 **전에** `EntitySnapshotCapturer.capture(entity)`로 `EntitySnapshot`을 만들고, 이후 `logUpdate(oldSnapshot, saved)`를 호출한다.
- 마감/마감 취소 → 수정과 같은 방식으로 `logClosing` / `logReopening`을 호출한다. 활동 유형만 "마감"/"마감 취소"로 다르다.

무엇을 기록할지는 엔터티에 선언한다. 클래스에 `@Loggable(name = …)`, 필드나 게터에 `@LoggableField(name, isIdentifier, transformer, format)`를 붙인다. `isIdentifier` 필드는 항상 기록되고, 나머지는 값이 바뀐 경우에만 기록된다. `LoggableTransformer`(기능별 `transformer/` 패키지)는 연관 엔터티나 enum을 표시용 문자열로 바꾸며 `format`보다 먼저 적용된다. **새 엔터티나 필드는 이 어노테이션을 붙여야 로그에 나타난다.**

엔터티 상태에 따라 논리명이 달라져야 하면 엔터티가 `DynamicLoggable`을 구현한다. `FinancialRecord`가 그 예로, 구분에 따라 대상명을 "수입"/"지출", 필드명을 "수입일"/"지출일", "봉헌자"/"수취인"으로 기록한다. 이때 `EntitySnapshot`은 어노테이션의 논리명을 키로 쓰고, 바뀐 논리명은 로그 문구를 만들 때만 적용한다.

### 저장 형식

기록 내용은 문장이 아니라 항목 목록 JSON으로 `ActivityLog.description`에 저장한다.

```json
{"fields":[{"name":"이름","oldValue":"변경 전","value":"변경 후"}]}
```

`oldValue`는 값이 바뀐 항목에만 있고, 그 유무가 곧 변경 여부다. 로그인, 인쇄, 백업 기록도 같은 구조를 쓴다. JSON 형식은 `ActivityLogMapper`가 관리하며, 서비스는 `ActivityLogFieldDto` 목록만 다룬다. 표시용 문자열은 UI(`ActivityLogFieldFormatter`, `ActivityLogDetailDialog`)가 만든다.

가져오기(`ActivityType.IMPORT`)는 여러 건을 한 번에 처리하므로 로그 한 건에 건별 내용을 함께 담는다: `{"fields":[요약],"items":[{"fields":[…]},…]}`. 보고서는 종이 인쇄와 PDF 저장을 모두 "인쇄"(`ActivityType.PRINT`)로 기록하고 "출력 방식" 항목으로 구분한다.

## 데이터 가져오기/내보내기

요구사항은 [요구사항 명세서 5절](requirements.md#5-데이터-관리-data-management---관리자-전용)에 있다. 화면 구조는 [UI 가이드라인](ui-guideline.md#데이터-관리-화면)에 있다.

- **내보내기**: `DataExportController` → `DataExportService`(`domain/datatransfer`)가 표(`SpreadsheetTable`)를 만들고 `SpreadsheetWriter`가 엑셀(xlsx, Apache POI, 암호 선택) 또는 CSV(UTF-8 BOM)로 쓴다. CSV는 수식 주입을 막기 위해 수식으로 해석될 수 있는 글자(`SpreadsheetWriter.FORMULA_PREFIXES`)로 시작하는 값 앞에 작은따옴표를 붙이고, `SpreadsheetReader`는 작은따옴표 뒤가 그 글자일 때만 떼고 읽는다.
- **파일 읽기**: `SpreadsheetReader`가 xlsx, xls, csv를 `SpreadsheetData`(머리글 구분 없는 행 목록)로 읽는다. 엑셀의 날짜 셀은 `LocalDate`, 정수는 `Long`, 소수는 `Double`로, CSV는 모두 글자로 읽는다. CSV 인코딩은 UTF-8로 해석되면 UTF-8, 아니면 MS949로 판별한다.
- **머리글**: 내보낸 파일과 빈 양식의 머리글(`DataExportServiceImpl`의 `HEADER_*`)은 가져오기에서 열을 자동으로 알아보는 기준이므로, 바꿀 때는 가져오기의 자동 추측도 함께 고친다.
- **가져오기 서비스는 상태를 보관하지 않는다**: 화면이 파일의 행과 열 지정을 보관하다가 `analyze`와 반영 메서드에 매번 넘긴다. 그래서 메뉴를 옮겼다 돌아와도 같은 입력으로 다시 분석하면 된다. 분석 규칙은 DB를 읽지 않는 순수 클래스(`BudgetImportAnalyzer`, `RecordImportAnalyzer`)에 모아 두었다.
- **반영 전 확인**: 반영할 때 다시 분석해서 화면이 본 결과와 요약 값(`signature`)이 다르면 `ImportStateChangedException`을 던지고 아무것도 반영하지 않는다. 반영 전 백업의 유효성은 활동 기록으로 판단한다. 백업이 돌려준 활동 기록 ID가 최신 로그 ID와 다르면(백업 뒤에 무언가 기록됨) `BackupOutdatedException`을 던진다.
- **활동 기록은 한 건**: 예산과 내역은 `…WithoutLog` 메서드로 추가·갱신한다. 검증은 일반 추가·수정과 같고 활동 기록만 남기지 않는다. 가져오기 전체를 `ActivityLogService#logImport` 한 건으로 남기기 위함이다.
- **수입/지출 가져오기**는 예산과 관계자를 자동으로 만들지 않고, 파일의 값을 등록된 말단 예산과 관계자에 연결만 한다. 매핑 항목의 식별자는 `INCOME:홍길동`처럼 구분과 값을 이은 것이다(같은 이름이라도 봉헌자와 수취인이 다른 대상일 수 있고, 항목의 금액 합계에 수입과 지출이 섞이지 않게 하기 위함이다). 직접 한 매핑이 자동 매핑보다 우선하지만, 대상을 쓸 수 없게 되었으면(삭제, 말단이 아님, 구분이 다름) 따르지 않는다. 관계자는 칸 전체가 한 항목이고(`INCOME:홍길동, 김철수`, 공백과 중복을 정리한 값. 순서는 내역에 저장되는 순서이므로 순서만 다른 값은 다른 항목이다) 매핑은 ID 목록이다. 자동 매핑은 이름마다 관계자를 찾아 모두 찾았을 때만 연결하고, 일부만 찾았으면 찾은 관계자(`suggestedTargetIds`)와 찾지 못한 이름(`unresolvedNames`)을 남겨 매핑 다이얼로그가 미리 채우게 한다. 수입 항목은 여러 명에 직접 매핑할 수 있으며, 지출 항목은 한 명이어야 하고 대상 중 한 명이라도 삭제되었으면 따르지 않는다. 관계자 자동 매핑에서 동명이인이면 숨김 처리되지 않은 관계자 중 ID가 가장 큰 관계자를 고른다(`Contact`에 등록 일시가 없어 ID로 판단한다).

## 보고서

보고서의 구성과 집계 기준은 [요구사항 명세서 4절](requirements.md#4-보고서-reporting)에 있다.

- **한 번 배치해 세 곳에 그린다**: `ui/view/report/composer`의 composer가 DTO를 `ReportCanvas`에 배치해 `ReportDocument`를 만든다. 이 문서는 쪽마다 글자, 선, 사각형(`ReportElement`)의 자리가 정해진 목록이며, 미리보기(`ReportPreviewPanel`), 인쇄(`ReportPrinter`), PDF(`ReportPdfExporter`)가 이것을 그대로 그린다. 그래서 세 출력의 줄바꿈과 쪽 나눔이 항상 같다.
- **단위와 글꼴**: 좌표는 포인트(1/72인치), 용지는 A4 세로, 여백은 10mm다(`ReportPaper`). 글자 폭은 화면이나 프린터와 무관한 고정 기준(`ReportText`)으로 잰다. 글꼴은 PC마다 배치가 달라지지 않도록 프로그램에 포함한 나눔고딕(OFL)을 쓴다. 이 글꼴은 라이브러리가 아니어서 라이선스 리포트에 나오지 않으므로 `ProgramInfoServiceImpl`이 오픈소스 목록에 직접 넣는다.
- **요소는 세 종류뿐이다**: `ReportElement`는 `Text`, `Line`, `Rect`만 있는 sealed 인터페이스다. PDF는 Graphics2D를 거치지 않고 요소를 PDFBox 명령으로 직접 옮기므로(글자가 검색되고, 글꼴은 쓰인 글자만 내장된다) 요소 종류를 늘리면 `ReportPdfExporter`도 함께 고친다. 차트와 막대도 이 세 요소로 그린다.
- **흑백 전제**: 색을 쓰지 않는다. 수입은 진한 회색 채움, 지출은 테두리만 있는 막대다.
- **표(`ReportTable`)**: 쪽 전체 폭으로 흘려 그리면(`render`) 행 단위로 쪽을 넘기며 머리글을 반복한다. 정해진 자리에 그리면(`renderAt`) 쪽을 넘기지 않는다.
- **주간 보고서의 내역**: 수입과 지출 두 단이 서로 독립적으로 흘러가고, 쪽 끝에 닿으면 각자 다음 쪽의 같은 단으로 넘어간다. 칸에 넘치는 내용은 말줄임표로 줄이고 바로 아랫줄에 "내용: …"처럼 칸 이름을 붙여 다시 적는다. 한 건의 내역과 그 아랫줄들은 쪽 사이에서 갈라지지 않는다.
- **글자 크기**: 주간 보고서는 제목 12pt 이하, 내역 7.5pt에 줄간격 1.2배다(1배는 위아래 줄의 글자가 맞닿았다). 회계 연도 단위 보고서의 제목은 13pt이고, 예산안과 결산 세부 내역서의 표는 9pt다. 쪽 번호는 9pt다. 이 값들은 실제 인쇄물을 보고 정했다.
- **인쇄**: 여백은 보고서가 직접 두므로 용지 전체를 인쇄 영역으로 요청한다. 프린터가 여백 안쪽을 다 인쇄할 수 없거나 용지가 A4가 아니면 쪽 전체를 같은 비율로 줄인다. 인쇄 대화상자는 EDT에서, 실제 인쇄는 백그라운드에서 한다.
- **출력 대상**: 화면의 선택값이 아니라 미리보기에 실제로 보이는 보고서다. 활동 기록은 출력이 성공한 뒤에만 남긴다.

## 배포

배포 파일은 실행 가능한 jar(`bootJar`)와 Windows용 zip(`packageWindows`)이다. 절차는 [기여 안내의 "릴리스"](../CONTRIBUTING.md#릴리스-관리자용)에 있다.

- zip은 jpackage의 app-image(Java 런타임 포함 실행 폴더)를 묶은 것이다. 설치 프로그램(msi/exe)은 만들지 않는다. 압축을 푼 폴더 하나에 프로그램과 데이터가 함께 있어 옮기기 쉽고, 관리자 권한이 필요 없기 때문이다.
- macOS용 앱은 만들지 않는다. 서명과 공증(Apple Developer Program)이 없으면 Gatekeeper가 실행을 막기 때문이며, macOS와 리눅스에서는 jar를 쓴다.
- jpackage는 실행하는 OS용 결과물만 만들 수 있어 `packageWindows`는 Windows에서만 돈다. 릴리스는 GitHub Actions의 Windows 러너가 만든다.
- 런타임에 넣을 모듈은 `--add-modules`에 모두 직접 적는다(`java.se`와 라이브러리가 쓸 수 있는 JDK 모듈 몇 개). `ALL-DEFAULT`는 JMOD 파일이 없는 JDK(Temurin 25 등)에서 아무 모듈로도 풀리지 않아 `java.base`만 들어가고 실행 파일이 뜨지 않기 때문이다. 새 기능이나 라이브러리가 `java.se` 밖의 JDK 모듈을 쓰면 이 목록에 더한다.
- jpackage는 서비스로만 쓰이는 모듈을 런타임에 넣지 않으므로 그런 모듈도 `--add-modules`에 직접 적는다(`jdk.charsets`: MS949, `jdk.localedata`: 한국어 요일). 다른 문자 인코딩이나 로캘 같은 기능을 새로 쓰면 이 목록도 본다.
- **프로그램 아이콘**: 원본은 `src/main/packaging`에 있다(ico: Windows 실행 파일, icns: macOS, `ChurchSteward.png`: 1024px, `ChurchSteward-{16,24,32,48,64,128,256}.png`: ico에 든 그림을 크기마다 꺼낸 것). ico를 바꾸면 크기별 png도 다시 꺼내 둔다. 실행 파일의 아이콘은 jpackage(`--icon`)가 붙이고, 실행 중인 창의 아이콘은 `AppIcons`가 붙인다(png를 `processResources`가 클래스패스의 `icons/`로 복사한다).
