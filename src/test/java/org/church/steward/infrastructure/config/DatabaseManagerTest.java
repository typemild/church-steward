package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zaxxer.hikari.HikariDataSource;

class DatabaseManagerTest {

    private static final String ADMIN_ID = "관리자";
    private static final String ADMIN_PASSWORD = "admin_password123!";
    /** 칼럼 하나를 추가하는 가짜 마이그레이션 (src/test/resources) */
    private static final String UPGRADE_MIGRATION_LOCATION = "classpath:db/test-migration/upgrade";
    /** 중간에 실패하는 가짜 마이그레이션 (src/test/resources) */
    private static final String BROKEN_MIGRATION_LOCATION = "classpath:db/test-migration/broken";

    @TempDir
    Path tempDir;

    private AppSettingManager appSettingManager;
    private DatabaseManager databaseManager;
    private final String dbName = "test_db";

    @BeforeEach
    void setUp() throws IOException {
        Path yamlPath = tempDir.resolve("application.yml");
        Files.createFile(yamlPath);
        appSettingManager = new AppSettingManager(yamlPath);
        databaseManager = new DatabaseManager(tempDir.toString(), dbName, appSettingManager);
    }

    @Nested
    @DisplayName("isInitializationRequired 테스트")
    class IsInitializationRequiredTest {

        @Test
        @DisplayName("DB 파일이 없으면 true를 반환한다")
        void isInitializationRequired_NoDbFile_ReturnsTrue() {
            // Given

            // When
            boolean isRequired = databaseManager.isInitializationRequired();

            // Then
            assertThat(isRequired).isTrue();
        }

        @Test
        @DisplayName("DB 파일이 존재하면 false를 반환한다")
        void isInitializationRequired_DbFileExists_ReturnsFalse() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);

            // When
            boolean isRequired = databaseManager.isInitializationRequired();

            // Then
            assertThat(isRequired).isFalse();
        }
    }

    @Nested
    @DisplayName("initialize 테스트")
    class InitializeTest {

        @Test
        @DisplayName("초기화 성공 시 YAML에 설정이 저장되고 DB 파일이 생성된다")
        void initialize_Success_SavesConfigAndCreatesDbFile() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";

            // When
            databaseManager.initialize(adminId, adminPassword);

            // Then
            // 1. YAML 설정 확인
            String saltBase64 = appSettingManager.get("database-manager.관리자.salt");
            String filePass = appSettingManager.get("database-manager.관리자.file-password");
            String userPass = appSettingManager.get("database-manager.관리자.user-password");

            assertThat(saltBase64).isNotNull();
            assertThat(filePass).isNotNull();
            assertThat(userPass).isNotNull();

            // 2. DB 파일 존재 확인
            assertThat(Files.exists(tempDir.resolve(dbName + ".mv.db"))).isTrue();
        }

        @Test
        @DisplayName("초기화하면 스키마가 최신 버전으로 만들어진다")
        void initialize_Success_CreatesSchema() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";

            // When
            databaseManager.initialize(adminId, adminPassword);

            // Then
            assertThat(countTables("USERS")).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("migrate 테스트")
    class MigrateTest {

        @Test
        @DisplayName("이미 최신 스키마이면 아무것도 하지 않고 파일도 복사하지 않는다")
        void migrate_UpToDate_ReturnsEmpty() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);

            // When
            Optional<Path> copiedDir = databaseManager.migrate();

            // Then
            assertThat(copiedDir).isEmpty();
        }

        @Test
        @DisplayName("새 버전의 마이그레이션이 있으면 직전의 데이터베이스 파일을 복사해 둔다")
        void migrate_PendingMigration_CopiesDatabaseFile() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            DatabaseManager upgraded = createUpgradedManager(UPGRADE_MIGRATION_LOCATION);
            upgraded.loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);

            // When
            Optional<Path> copiedDir = upgraded.migrate();

            // Then
            assertThat(copiedDir).isPresent();
            assertThat(copiedDir.get().resolve(dbName + ".mv.db")).exists();
        }

        @Test
        @DisplayName("새 버전의 마이그레이션을 적용해도 기존 데이터는 그대로 남는다")
        void migrate_PendingMigration_KeepsExistingData() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            execute(databaseManager,
                    "INSERT INTO CONTACT (TYPE, NAME, IS_HIDDEN) VALUES ('INDIVIDUAL', '홍길동', FALSE)");
            DatabaseManager upgraded = createUpgradedManager(UPGRADE_MIGRATION_LOCATION);
            upgraded.loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);

            // When
            upgraded.migrate();

            // Then
            assertThat(queryString(upgraded, "SELECT NAME || ':' || TEST_MEMO FROM CONTACT"))
                    .isEqualTo("홍길동:migrated");
        }

        @Test
        @DisplayName("마이그레이션이 중간에 실패하면 복사해 둔 디렉터리를 알리는 SQLException이 발생한다")
        void migrate_Failure_ThrowsSQLExceptionWithCopiedDir() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            DatabaseManager upgraded = createUpgradedManager(BROKEN_MIGRATION_LOCATION);
            upgraded.loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);

            // When & Then
            assertThatThrownBy(() -> upgraded.migrate())
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("before-migrate-");
        }
    }

    /**
     * 같은 데이터베이스를 쓰되 가짜 마이그레이션이 더해진, 새 버전의 프로그램에 해당하는 DatabaseManager를 만든다.
     */
    private DatabaseManager createUpgradedManager(String extraMigrationLocation) {
        return new DatabaseManager(tempDir.toString(), dbName, appSettingManager,
                List.of(DatabaseManager.MIGRATION_LOCATION, extraMigrationLocation));
    }

    private void execute(DatabaseManager manager, String sql) throws SQLException {
        try (HikariDataSource dataSource = (HikariDataSource) manager.createDataSource();
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private String queryString(DatabaseManager manager, String sql) throws SQLException {
        try (HikariDataSource dataSource = (HikariDataSource) manager.createDataSource();
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int countTables(String tableName) throws SQLException {
        // INFORMATION_SCHEMA에도 USERS 같은 이름의 표가 있으므로 스키마를 지정한다.
        return Integer.parseInt(queryString(databaseManager,
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = '"
                        + tableName + "'"));
    }

    @Nested
    @DisplayName("loadDatabasePassword 테스트")
    class LoadDatabasePasswordTest {

        @Test
        @DisplayName("올바른 비밀번호를 입력하면 성공한다")
        void loadDatabasePassword_CorrectPassword_Success() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);
            String inputPassword = "admin_password123!";

            // When
            databaseManager.loadDatabasePassword(adminId, inputPassword);

            // Then
            // 별도의 성공 상태 확인 로직이 없으므로 예외가 발생하지 않음을 검증
        }

        @Test
        @DisplayName("잘못된 비밀번호를 입력하면 InvalidPasswordException이 발생한다")
        void loadDatabasePassword_WrongPassword_ThrowsInvalidPasswordException() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);
            String wrongPassword = "wrong_password";

            // When & Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(adminId, wrongPassword))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("설정 파일의 암호문이 Base64 형식이 아니어도 비밀번호 오류와 같은 InvalidPasswordException이 발생한다")
        void loadDatabasePassword_MalformedCiphertext_ThrowsInvalidPasswordException() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);
            appSettingManager.set("database-manager.관리자.file-password", "not-base64!");

            // When & Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(adminId, adminPassword))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다. 다시 확인해 주세요.")
                    .hasNoCause();
        }

        @Test
        @DisplayName("설정 파일의 salt가 Base64 형식이 아니어도 비밀번호 오류와 같은 InvalidPasswordException이 발생한다")
        void loadDatabasePassword_MalformedSalt_ThrowsInvalidPasswordException() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            appSettingManager.set("database-manager.관리자.salt", "not-base64!");

            // When & Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다. 다시 확인해 주세요.")
                    .hasNoCause();
        }

        @Test
        @DisplayName("설정 파일에 salt가 없어도 비밀번호 오류와 같은 InvalidPasswordException이 발생한다")
        void loadDatabasePassword_MissingSalt_ThrowsInvalidPasswordException() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            appSettingManager.remove("database-manager.관리자.salt", false);

            // When & Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다. 다시 확인해 주세요.")
                    .hasNoCause();
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 입력하면 UserNotFoundException이 발생한다")
        void loadDatabasePassword_UnknownUser_ThrowsUserNotFoundException() {
            // Given
            String unknownUser = "non_existent_user";
            String password = "password";

            // When & Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(unknownUser, password))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("saveOrUpdateUserPassword 테스트")
    class SaveOrUpdateUserPasswordTest {

        @Test
        @DisplayName("비밀번호 변경 시 YAML 설정이 업데이트된다")
        void saveOrUpdateUserPassword_Success_UpdatesYaml() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);
            String newPassword = "new_secret_password_456!";

            // When
            databaseManager.saveOrUpdateUserPassword(adminId, newPassword);

            // Then
            // 1. 새 비밀번호로 로드 가능한지 확인 (성공하면 업데이트된 것)
            databaseManager.loadDatabasePassword(adminId, newPassword);

            // 2. 이전 비밀번호로는 로드 불가능한지 확인
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(adminId, adminPassword))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자의 비밀번호를 설정하면 새로운 설정이 생성된다")
        void saveOrUpdateUserPassword_NewUser_Success_CreatesNewConfig() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);

            String newUser = "new_user";
            String newPassword = "new_password123!";

            // When
            databaseManager.saveOrUpdateUserPassword(newUser, newPassword);

            // Then
            // 1. 새 사용자로 로드 가능한지 확인
            databaseManager.loadDatabasePassword(newUser, newPassword);

            // 2. 기존 사용자(관리자)로도 여전히 로드 가능한지 확인
            databaseManager.loadDatabasePassword(adminId, adminPassword);
        }
    }

    @Nested
    @DisplayName("removeUserPassword 테스트")
    class RemoveUserPasswordTest {

        @Test
        @DisplayName("사용자 정보를 삭제하면 더 이상 로드할 수 없다")
        void removeUserPassword_Success_RemovesUser() throws Exception {
            // Given
            String adminId = "관리자";
            String adminPassword = "admin_password123!";
            databaseManager.initialize(adminId, adminPassword);

            // When
            databaseManager.removeUserPassword(adminId);

            // Then
            assertThatThrownBy(() -> databaseManager.loadDatabasePassword(adminId, adminPassword))
                    .isInstanceOf(UserNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 사용자를 삭제하려고 하면 UserNotFoundException이 발생한다")
        void removeUserPassword_UnknownUser_ThrowsUserNotFoundException() {
            // Given
            String unknownUser = "unknown_user";

            // When & Then
            assertThatThrownBy(() -> databaseManager.removeUserPassword(unknownUser))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("restore 테스트")
    class RestoreTest {

        @Test
        @DisplayName("백업에 담긴 salt가 Base64 형식이 아니면 비밀번호 오류와 같은 InvalidPasswordException이 발생한다")
        void restore_MalformedSalt_ThrowsInvalidPasswordException() throws Exception {
            // Given
            databaseManager.initialize(ADMIN_ID, ADMIN_PASSWORD);
            EncryptedUserKey stored = databaseManager.readUserKeys(List.of(ADMIN_ID)).getFirst();
            EncryptedUserKey broken = new EncryptedUserKey(ADMIN_ID, stored.filePassword(), stored.userPassword(),
                    "not-base64!");
            Path dumpFile = tempDir.resolve("database.sql");

            // When & Then
            assertThatThrownBy(() -> databaseManager.restore(dumpFile, List.of(broken), ADMIN_ID, ADMIN_PASSWORD))
                    .isInstanceOf(InvalidPasswordException.class)
                    .hasMessage("비밀번호가 일치하지 않습니다. 다시 확인해 주세요.")
                    .hasNoCause();
        }
    }
}
