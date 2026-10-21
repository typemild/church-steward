package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * 전체 데이터 백업/복원이 기대는 H2의 동작을 확인한다.
 * 암호화된 데이터베이스를 암호화된 SQL 덤프({@code SCRIPT})로 내보내고, 새로 만든 암호화된 데이터베이스에
 * {@code RUNSCRIPT}로 되살릴 수 있어야 한다.
 * <p>
 * 내보내기와 되살리기를 같은 H2로 하므로, 과거 H2가 만든 덤프를 지금의 H2가 읽을 수 있는지는 이 테스트로 알 수 없다.
 * 그것은 H2 버전마다 만들어 둔 고정 덤프를 되살리는 {@link H2DumpCompatibilityTest}가 확인한다.
 */
class H2ScriptRoundTripTest {

    /** DatabaseManager가 만드는 것과 같은 형식의 비밀번호: "파일 비밀번호 사용자 비밀번호" */
    private static final String FILE_PASSWORD = "ZmlsZS1wYXNzd29yZC0xMjM0NQ==";
    private static final String USER_PASSWORD = "dXNlci1wYXNzd29yZC0xMjM0NQ==";
    private static final String KOREAN_TEXT = "십일조 헌금 (홍길동, 김철수)";

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("암호화된 DB를 덤프로 내보낸 뒤 새 DB에 되살리면 한글이 담긴 행이 그대로 복원된다")
    void script_EncryptedDatabase_RestoresRows() throws SQLException {
        // Given
        createSourceDatabase(tempDir.resolve("source"), "짧은 내용");
        Path dump = tempDir.resolve("backup.sql");
        writeDump(tempDir.resolve("source"), dump);

        // When
        restoreDump(dump, tempDir.resolve("target"), FILE_PASSWORD);

        // Then
        try (Connection connection = open(tempDir.resolve("target"));
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT NAME FROM SAMPLE ORDER BY ID")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo(KOREAN_TEXT);
        }
    }

    @Test
    @DisplayName("크기가 큰 CLOB 값도 덤프를 거쳐 그대로 복원된다")
    void script_LargeClob_RestoresContent() throws SQLException {
        // Given
        String largeText = "가나다라마바사아자차".repeat(100_000);
        createSourceDatabase(tempDir.resolve("source"), largeText);
        Path dump = tempDir.resolve("backup.sql");
        writeDump(tempDir.resolve("source"), dump);

        // When
        restoreDump(dump, tempDir.resolve("target"), FILE_PASSWORD);

        // Then
        try (Connection connection = open(tempDir.resolve("target"));
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT DESCRIPTION FROM SAMPLE ORDER BY ID")) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo(largeText);
        }
    }

    @Test
    @DisplayName("복원한 DB에 행을 추가하면 자동 증가 ID가 기존 값에 이어서 매겨진다")
    void script_IdentityColumn_ContinuesAfterRestore() throws SQLException {
        // Given
        createSourceDatabase(tempDir.resolve("source"), "짧은 내용");
        Path dump = tempDir.resolve("backup.sql");
        writeDump(tempDir.resolve("source"), dump);
        restoreDump(dump, tempDir.resolve("target"), FILE_PASSWORD);

        // When
        long newId;
        try (Connection connection = open(tempDir.resolve("target"));
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO SAMPLE(NAME, DESCRIPTION) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, "새 행");
            statement.setString(2, "새 내용");
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                keys.next();
                newId = keys.getLong(1);
            }
        }

        // Then
        assertThat(newId).isEqualTo(3L);
    }

    @Test
    @DisplayName("덤프 파일에는 데이터가 평문으로 남지 않는다")
    void script_EncryptedDump_DoesNotContainPlainText() throws Exception {
        // Given
        createSourceDatabase(tempDir.resolve("source"), "짧은 내용");
        Path dump = tempDir.resolve("backup.sql");

        // When
        writeDump(tempDir.resolve("source"), dump);

        // Then
        String content = new String(Files.readAllBytes(dump), StandardCharsets.ISO_8859_1);
        String plainText = new String(KOREAN_TEXT.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
        assertThat(content)
                .doesNotContain(plainText)
                .doesNotContain("CREATE");
    }

    @Test
    @DisplayName("덤프를 만들 때와 다른 비밀번호로는 되살릴 수 없다")
    void runScript_WrongPassword_Fails() throws SQLException {
        // Given
        createSourceDatabase(tempDir.resolve("source"), "짧은 내용");
        Path dump = tempDir.resolve("backup.sql");
        writeDump(tempDir.resolve("source"), dump);

        // When & Then
        assertThatThrownBy(() -> restoreDump(dump, tempDir.resolve("target"), "d3JvbmctcGFzc3dvcmQ="))
                .isInstanceOf(SQLException.class);
    }

    /**
     * 암호화된 DB를 만들고 행 두 개를 넣는다. 첫 행의 DESCRIPTION에 주어진 내용을 담는다.
     */
    private void createSourceDatabase(Path database, String description) throws SQLException {
        try (Connection connection = open(database);
                Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE SAMPLE(ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
                    + "NAME VARCHAR(100), DESCRIPTION CLOB)");
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO SAMPLE(NAME, DESCRIPTION) VALUES (?, ?)")) {
                insert.setString(1, KOREAN_TEXT);
                insert.setString(2, description);
                insert.executeUpdate();
                insert.setString(1, "두 번째 행");
                insert.setString(2, "두 번째 내용");
                insert.executeUpdate();
            }
        }
    }

    private void writeDump(Path database, Path dump) throws SQLException {
        try (Connection connection = open(database);
                Statement statement = connection.createStatement()) {
            statement.execute("SCRIPT TO '" + dump + "' COMPRESSION DEFLATE CIPHER AES PASSWORD '" + FILE_PASSWORD
                    + "' CHARSET 'UTF-8'");
        }
    }

    /**
     * 새 암호화된 DB를 만들고 덤프를 실행한다.
     */
    private void restoreDump(Path dump, Path database, String dumpPassword) throws SQLException {
        try (Connection connection = open(database);
                Statement statement = connection.createStatement()) {
            statement.execute("RUNSCRIPT FROM '" + dump + "' COMPRESSION DEFLATE CIPHER AES PASSWORD '"
                    + dumpPassword + "' CHARSET 'UTF-8'");
        }
    }

    private Connection open(Path database) throws SQLException {
        return DriverManager.getConnection("jdbc:h2:file:" + database + ";CIPHER=AES", "sa",
                FILE_PASSWORD + " " + USER_PASSWORD);
    }

}
