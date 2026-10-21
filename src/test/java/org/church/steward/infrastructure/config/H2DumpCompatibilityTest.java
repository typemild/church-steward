package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * 과거 H2 버전이 만든 백업의 덤프를 지금의 H2로 되살릴 수 있는지 확인한다.
 * <p>
 * 백업의 덤프는 그것을 만든 H2 버전의 형식(SQL 문법, 자료형 표기, 암호화·압축 형식)으로 쓰여 있으므로, 같은 H2로 내보내고
 * 되살리는 {@link H2ScriptRoundTripTest}로는 H2를 올렸을 때 과거 백업을 읽지 못하게 되는 것을 알 수 없다.
 * 그래서 H2 버전마다 만들어 커밋해 둔 고정 덤프({@value #FIXTURE_RESOURCE},
 * {@link H2DumpFixtureGenerator}가 만든다)를
 * 모두 {@link DatabaseManager#runDump}로 되살리고, 같은 데이터({@link H2DumpFixtureData})를
 * 지금의 H2에 새로 넣은 결과와 비교한다.
 * Spring Boot를 올리면 H2도 함께 올라가므로 그때 이 테스트가 실패하지 않는지 본다.
 * <p>
 * 고정 덤프는 프로그램의 실제 스키마와 무관한 임의의 구조이므로 Flyway 마이그레이션은 실행하지 않는다.
 * <p>
 * TODO: 과거 버전의 실제 스키마로 만든 백업이 복원할 때 Flyway 마이그레이션으로 현재 스키마까지 올라가는지는 이 테스트가
 * 확인하지 않는다. 첫 스키마 변경(V2 마이그레이션)을 추가할 때 별도 테스트로 만들어야 한다.
 */
class H2DumpCompatibilityTest {

    /** 고정 덤프를 두는 클래스패스 디렉터리 */
    static final String FIXTURE_RESOURCE = "h2-dump-fixtures";

    @TempDir
    Path tempDir;

    static Stream<Arguments> fixtures() throws Exception {
        URL directory = H2DumpCompatibilityTest.class.getResource("/" + FIXTURE_RESOURCE);
        List<Path> files = List.of();
        if (directory != null) {
            try (Stream<Path> list = Files.list(Path.of(directory.toURI()))) {
                files = list.filter(file -> file.getFileName().toString().endsWith(".sql"))
                        .sorted()
                        .toList();
            }
        }
        if (files.isEmpty()) {
            throw new IllegalStateException("H2 덤프 호환성 테스트에 쓸 고정 덤프가 없습니다."
                    + " ./gradlew generateH2DumpFixture로 만든 뒤 커밋하세요.");
        }
        return files.stream()
                .map(file -> Arguments.of(Named.of(file.getFileName().toString(), file)));
    }

    /**
     * 고정 덤프를 새 데이터베이스에 되살리고 그 경로를 돌려준다.
     */
    private Path restoreFixture(Path fixture) throws SQLException {
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("data").toString(), "unused",
                new AppSettingManager(tempDir.resolve("application.yml")));
        Path restored = tempDir.resolve("restored");
        databaseManager.runDump(fixture, restored.toString(), H2DumpFixtureData.FILE_PASSWORD,
                H2DumpFixtureData.USER_PASSWORD);
        return restored;
    }

    @Nested
    @DisplayName("runDump 테스트")
    class RunDumpTest {

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.church.steward.infrastructure.config.H2DumpCompatibilityTest#fixtures")
        @DisplayName("과거 H2가 만든 덤프를 되살리면 같은 데이터를 지금의 H2에 넣은 것과 구조와 내용이 같다")
        void runDump_FixtureFromPastH2_RestoresSameData(Path fixture) throws Exception {
            // Given
            List<String> expected;
            try (Connection connection = H2DumpFixtureData.open(tempDir.resolve("expected"))) {
                H2DumpFixtureData.create(connection);
                expected = H2DumpFixtureData.readSnapshot(connection);
            }

            // When
            Path restored = restoreFixture(fixture);

            // Then
            try (Connection connection = H2DumpFixtureData.open(restored)) {
                assertThat(H2DumpFixtureData.readSnapshot(connection)).containsExactlyElementsOf(expected);
            }
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("org.church.steward.infrastructure.config.H2DumpCompatibilityTest#fixtures")
        @DisplayName("과거 H2가 만든 덤프를 되살린 뒤 행을 추가하면 자동 증가 ID가 기존 값에 이어서 매겨진다")
        void runDump_FixtureFromPastH2_ContinuesIdentity(Path fixture) throws Exception {
            // Given
            Path restored = restoreFixture(fixture);

            // When
            long newId;
            try (Connection connection = H2DumpFixtureData.open(restored);
                    PreparedStatement statement = connection.prepareStatement("INSERT INTO PARENT_ITEM"
                            + " (CODE, START_DATE, ACTIVE) VALUES ('셋째', DATE '2027-01-01', TRUE)",
                            Statement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    keys.next();
                    newId = keys.getLong(1);
                }
            }

            // Then
            assertThat(newId).isEqualTo(3L);
        }
    }

}
