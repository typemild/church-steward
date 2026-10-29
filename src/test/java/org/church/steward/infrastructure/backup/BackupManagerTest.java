package org.church.steward.infrastructure.backup;

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

import org.church.steward.infrastructure.config.AppSettingManager;
import org.church.steward.infrastructure.config.DatabaseManager;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.zaxxer.hikari.HikariDataSource;

class BackupManagerTest {

    private static final String DB_NAME = "test_db";
    private static final String ADMIN_ID = "관리자";
    private static final String ADMIN_PASSWORD = "admin_password123!";
    private static final String PROGRAM_VERSION = "1.2.0";
    private static final String SAMPLE_NAME = "십일조 헌금";
    /** 칼럼 하나를 추가하는 가짜 마이그레이션 (src/test/resources) */
    private static final String UPGRADE_MIGRATION_LOCATION = "classpath:db/test-migration/upgrade";
    /** 중간에 실패하는 가짜 마이그레이션 (src/test/resources) */
    private static final String BROKEN_MIGRATION_LOCATION = "classpath:db/test-migration/broken";

    @TempDir
    Path tempDir;

    /**
     * 한 대의 PC에 해당하는 실행 환경. 디렉터리마다 설정 파일과 데이터베이스를 따로 가진다.
     */
    private record Installation(AppSettingManager appSettingManager, DatabaseManager databaseManager,
            BackupManager backupManager) {
    }

    private Installation createInstallation(String directoryName, String programVersion) throws IOException {
        return createInstallation(directoryName, programVersion, List.of(DatabaseManager.MIGRATION_LOCATION));
    }

    /**
     * 지정한 마이그레이션 위치를 쓰는 실행 환경을 만든다. 실제 위치에 가짜 마이그레이션의 위치를 더해 넘기면, 마이그레이션이 추가된 새 버전의
     * 프로그램을 흉내 낼 수 있다. 기존 실행 환경과 같은 디렉터리 이름을 주면, 그 환경의 데이터를 새 버전의 프로그램으로 다루는 상황이
     * 된다.
     */
    private Installation createInstallation(String directoryName, String programVersion,
            List<String> migrationLocations) throws IOException {
        Path directory = Files.createDirectories(tempDir.resolve(directoryName));
        AppSettingManager appSettingManager = new AppSettingManager(directory.resolve("application.yml"));
        DatabaseManager databaseManager = new DatabaseManager(directory.resolve("data").toString(), DB_NAME,
                appSettingManager, migrationLocations);
        return new Installation(appSettingManager, databaseManager,
                new BackupManager(databaseManager, appSettingManager, programVersion));
    }

    /**
     * 관리자 계정으로 초기화하고 행 하나가 든 표를 만든 실행 환경을 준비한다.
     */
    private Installation createInitializedInstallation(String directoryName) throws Exception {
        Installation installation = createInstallation(directoryName, PROGRAM_VERSION);
        installation.databaseManager().initialize(ADMIN_ID, ADMIN_PASSWORD);
        installation.databaseManager().loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);
        try (HikariDataSource dataSource = openDataSource(installation);
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE SAMPLE(ID BIGINT PRIMARY KEY, NAME VARCHAR(100))");
            statement.execute("INSERT INTO SAMPLE VALUES (1, '" + SAMPLE_NAME + "')");
        }
        return installation;
    }

    private HikariDataSource openDataSource(Installation installation) {
        return (HikariDataSource) installation.databaseManager().createDataSource();
    }

    private Path createBackup(Installation installation) {
        Path backupFile = tempDir.resolve("backup." + BackupManager.FILE_EXTENSION);
        try (HikariDataSource dataSource = openDataSource(installation)) {
            installation.backupManager().createBackup(backupFile, dataSource, List.of(ADMIN_ID));
        }
        return backupFile;
    }

    private String readSampleName(Installation installation) throws SQLException {
        try (HikariDataSource dataSource = openDataSource(installation);
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT NAME FROM SAMPLE WHERE ID = 1")) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private int countContactColumns(Installation installation, String columnName) throws SQLException {
        try (HikariDataSource dataSource = openDataSource(installation);
                Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS"
                        + " WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'CONTACT'"
                        + " AND COLUMN_NAME = '" + columnName + "'")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    @Nested
    @DisplayName("createBackup 테스트")
    class CreateBackupTest {

        @Test
        @DisplayName("백업 파일에는 프로그램 버전과 사용자의 DB 접근 키가 기록된다")
        void createBackup_InitializedDatabase_WritesManifest() throws Exception {
            // Given
            Installation source = createInitializedInstallation("source");

            // When
            Path backupFile = createBackup(source);

            // Then
            BackupManifest manifest = source.backupManager().inspect(backupFile);
            assertThat(manifest.programVersion()).isEqualTo(PROGRAM_VERSION);
            assertThat(manifest.userKeys())
                    .extracting(key -> key.userId())
                    .containsExactly(ADMIN_ID);
        }

        @Test
        @DisplayName("설정 파일에 DB 접근 키가 있는 사용자가 없으면 백업할 수 없다")
        void createBackup_NoUserKeys_Failure() throws Exception {
            // Given
            Installation source = createInitializedInstallation("source");
            Path backupFile = tempDir.resolve("backup." + BackupManager.FILE_EXTENSION);

            // When & Then
            try (HikariDataSource dataSource = openDataSource(source)) {
                assertThatThrownBy(() -> source.backupManager()
                        .createBackup(backupFile, dataSource, List.of("없는 사용자")))
                        .isInstanceOf(BackupException.class);
            }
        }
    }

    @Nested
    @DisplayName("inspect 테스트")
    class InspectTest {

        @Test
        @DisplayName("백업 파일이 아닌 파일은 거부한다")
        void inspect_NotBackupFile_Failure() throws Exception {
            // Given
            Installation installation = createInstallation("target", PROGRAM_VERSION);
            Path file = Files.writeString(tempDir.resolve("not-a-backup.csbackup"), "백업 파일이 아님");

            // When & Then
            assertThatThrownBy(() -> installation.backupManager().inspect(file))
                    .isInstanceOf(BackupException.class)
                    .hasMessage("청지기의 백업 파일이 아니거나 손상된 파일입니다.");
        }

        @Test
        @DisplayName("없는 파일은 거부한다")
        void inspect_MissingFile_Failure() throws Exception {
            // Given
            Installation installation = createInstallation("target", PROGRAM_VERSION);
            Path file = tempDir.resolve("missing.csbackup");

            // When & Then
            assertThatThrownBy(() -> installation.backupManager().inspect(file))
                    .isInstanceOf(BackupException.class)
                    .hasMessageStartingWith("백업 파일을 찾을 수 없습니다");
        }

        @Test
        @DisplayName("더 새로운 버전의 프로그램에서 만든 백업은 거부한다")
        void inspect_BackupFromNewerProgram_Failure() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation olderInstallation = createInstallation("target", "1.1.9");

            // When & Then
            assertThatThrownBy(() -> olderInstallation.backupManager().inspect(backupFile))
                    .isInstanceOf(BackupException.class)
                    .hasMessageContaining("더 새로운 버전(1.2.0)");
        }

        @Test
        @DisplayName("과거 버전의 프로그램에서 만든 백업은 받아들인다")
        void inspect_BackupFromOlderProgram_Success() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation newerInstallation = createInstallation("target", "2.0.0");

            // When
            BackupManifest manifest = newerInstallation.backupManager().inspect(backupFile);

            // Then
            assertThat(manifest.programVersion()).isEqualTo(PROGRAM_VERSION);
        }
    }

    @Nested
    @DisplayName("restore 테스트")
    class RestoreTest {

        @Test
        @DisplayName("데이터가 없는 새 환경에 복원하면 백업한 시점의 계정으로 데이터를 읽을 수 있다")
        void restore_EmptyInstallation_RestoresData() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation target = createInstallation("target", PROGRAM_VERSION);

            // When
            target.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            target.databaseManager().loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);
            assertThat(readSampleName(target)).isEqualTo(SAMPLE_NAME);
        }

        @Test
        @DisplayName("데이터가 없는 새 환경에 복원하면 보관할 기존 데이터가 없다")
        void restore_EmptyInstallation_ReturnsNoPreservedDirectory() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation target = createInstallation("target", PROGRAM_VERSION);

            // When
            Optional<Path> preservedDir = target.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            assertThat(preservedDir).isEmpty();
        }

        @Test
        @DisplayName("기존 데이터가 있는 환경에 복원하면 기존 데이터가 백업의 내용으로 바뀐다")
        void restore_ExistingInstallation_ReplacesData() throws Exception {
            // Given
            Installation installation = createInitializedInstallation("source");
            Path backupFile = createBackup(installation);
            try (HikariDataSource dataSource = openDataSource(installation);
                    Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("UPDATE SAMPLE SET NAME = '백업 이후에 바꾼 값' WHERE ID = 1");
            }

            // When
            installation.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            installation.databaseManager().loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);
            assertThat(readSampleName(installation)).isEqualTo(SAMPLE_NAME);
        }

        @Test
        @DisplayName("기존 데이터가 있는 환경에 복원하면 기존 데이터베이스 파일과 설정 파일을 보관 디렉터리로 옮겨 둔다")
        void restore_ExistingInstallation_PreservesPreviousData() throws Exception {
            // Given
            Installation installation = createInitializedInstallation("source");
            Path backupFile = createBackup(installation);

            // When
            Optional<Path> preservedDir = installation.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            assertThat(preservedDir).isPresent();
            assertThat(preservedDir.get().resolve(DB_NAME + ".mv.db")).exists();
            assertThat(preservedDir.get().resolve("application.yml")).exists();
        }

        @Test
        @DisplayName("백업한 뒤에 비밀번호를 바꿨어도 복원한 뒤에는 백업한 시점의 비밀번호로 로그인한다")
        void restore_PasswordChangedAfterBackup_UsesPasswordAtBackup() throws Exception {
            // Given
            Installation installation = createInitializedInstallation("source");
            Path backupFile = createBackup(installation);
            installation.databaseManager().saveOrUpdateUserPassword(ADMIN_ID, "changed_password456!");

            // When
            installation.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            assertThatThrownBy(() -> installation.databaseManager()
                    .loadDatabasePassword(ADMIN_ID, "changed_password456!"))
                    .isInstanceOf(InvalidPasswordException.class);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 복원하지 않고 기존 데이터를 그대로 둔다")
        void restore_WrongPassword_KeepsExistingData() throws Exception {
            // Given
            Installation installation = createInitializedInstallation("source");
            Path backupFile = createBackup(installation);
            Path databaseFile = tempDir.resolve("source/data/" + DB_NAME + ".mv.db");

            // When & Then
            assertThatThrownBy(() -> installation.backupManager().restore(backupFile, ADMIN_ID, "wrong_password!"))
                    .isInstanceOf(InvalidPasswordException.class);
            assertThat(databaseFile).exists();
        }

        @Test
        @DisplayName("과거 버전의 백업을 복원하면 스키마가 이 버전으로 올라간다")
        void restore_BackupFromOlderVersion_MigratesSchema() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation upgraded = createInstallation("target", PROGRAM_VERSION,
                    List.of(DatabaseManager.MIGRATION_LOCATION, UPGRADE_MIGRATION_LOCATION));

            // When
            upgraded.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            upgraded.databaseManager().loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);
            assertThat(countContactColumns(upgraded, "TEST_MEMO")).isEqualTo(1);
        }

        @Test
        @DisplayName("과거 버전의 백업을 복원하면서 스키마를 올려도 백업한 데이터는 그대로 남는다")
        void restore_BackupFromOlderVersion_KeepsBackupData() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation upgraded = createInstallation("target", PROGRAM_VERSION,
                    List.of(DatabaseManager.MIGRATION_LOCATION, UPGRADE_MIGRATION_LOCATION));

            // When
            upgraded.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD);

            // Then
            upgraded.databaseManager().loadDatabasePassword(ADMIN_ID, ADMIN_PASSWORD);
            assertThat(readSampleName(upgraded)).isEqualTo(SAMPLE_NAME);
        }

        @Test
        @DisplayName("백업한 데이터의 스키마를 이 버전에 맞게 올리지 못하면 복원하지 않고 기존 데이터를 그대로 둔다")
        void restore_MigrationFailure_KeepsExistingData() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation target = createInitializedInstallation("target");
            try (HikariDataSource dataSource = openDataSource(target);
                    Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement()) {
                statement.execute("UPDATE SAMPLE SET NAME = '복원 전의 값' WHERE ID = 1");
            }
            // 같은 환경에서 중간에 실패하는 마이그레이션을 가진 버전으로 복원한다.
            Installation broken = createInstallation("target", PROGRAM_VERSION,
                    List.of(DatabaseManager.MIGRATION_LOCATION, BROKEN_MIGRATION_LOCATION));

            // When & Then
            assertThatThrownBy(() -> broken.backupManager().restore(backupFile, ADMIN_ID, ADMIN_PASSWORD))
                    .isInstanceOf(BackupException.class);
            assertThat(readSampleName(target)).isEqualTo("복원 전의 값");
        }

        @Test
        @DisplayName("백업에 없는 사용자로는 복원할 수 없다")
        void restore_UnknownUser_Failure() throws Exception {
            // Given
            Path backupFile = createBackup(createInitializedInstallation("source"));
            Installation target = createInstallation("target", PROGRAM_VERSION);

            // When & Then
            assertThatThrownBy(() -> target.backupManager().restore(backupFile, "없는 사용자", ADMIN_PASSWORD))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("scheduleRestore 테스트")
    class ScheduleRestoreTest {

        @Test
        @DisplayName("복원을 예약하면 다음 실행 때 그 백업 파일을 조회할 수 있다")
        void scheduleRestore_File_CanBeReadBack() throws Exception {
            // Given
            Installation installation = createInstallation("target", PROGRAM_VERSION);
            Path backupFile = tempDir.resolve("backup.csbackup");

            // When
            installation.backupManager().scheduleRestore(backupFile);

            // Then
            assertThat(installation.backupManager().getScheduledRestore()).contains(backupFile.toAbsolutePath());
        }
    }

    @Nested
    @DisplayName("getScheduledRestore 테스트")
    class GetScheduledRestoreTest {

        @Test
        @DisplayName("예약한 적이 없으면 비어 있다")
        void getScheduledRestore_NothingScheduled_ReturnsEmpty() throws Exception {
            // Given
            Installation installation = createInstallation("target", PROGRAM_VERSION);

            // When
            Optional<Path> scheduled = installation.backupManager().getScheduledRestore();

            // Then
            assertThat(scheduled).isEmpty();
        }
    }

    @Nested
    @DisplayName("cancelScheduledRestore 테스트")
    class CancelScheduledRestoreTest {

        @Test
        @DisplayName("예약을 취소하면 예약된 백업 파일 정보가 없어진다")
        void cancelScheduledRestore_Scheduled_ClearsSchedule() throws Exception {
            // Given
            Installation installation = createInstallation("target", PROGRAM_VERSION);
            installation.backupManager().scheduleRestore(tempDir.resolve("backup.csbackup"));

            // When
            installation.backupManager().cancelScheduledRestore();

            // Then
            assertThat(installation.backupManager().getScheduledRestore()).isEmpty();
        }
    }

}
