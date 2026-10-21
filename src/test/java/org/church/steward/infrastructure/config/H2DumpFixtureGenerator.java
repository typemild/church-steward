package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.io.TempDir;

/**
 * H2 덤프 호환성 테스트({@link H2DumpCompatibilityTest})가 되살릴 고정 덤프를 지금의 H2로 만든다.
 * <p>
 * 평소의 테스트에서는 실행되지 않고 {@code ./gradlew generateH2DumpFixture}로만 실행된다. 릴리스할 때 실행해
 * 그 H2 버전의 고정 덤프가 없으면 만들고, 만들어진 파일을 함께 커밋한다. 파일 이름은 "h2-{H2 버전}.sql"이며, 이미 있으면
 * 아무것도 하지 않는다.
 */
@EnabledIfSystemProperty(named = "h2DumpFixture.generate", matches = "true")
class H2DumpFixtureGenerator {

    /** 고정 덤프를 두는 디렉터리. Gradle은 저장소 루트에서 테스트를 실행한다. */
    private static final Path FIXTURE_DIR = Path.of("src/test/resources/" + H2DumpCompatibilityTest.FIXTURE_RESOURCE);

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("지금의 H2로 고정 덤프를 만든다")
    void generate_CurrentH2_WritesFixture() throws Exception {
        // Given
        DatabaseManager databaseManager = new DatabaseManager(tempDir.resolve("data").toString(), "unused",
                new AppSettingManager(tempDir.resolve("application.yml")));
        try (Connection connection = H2DumpFixtureData.open(tempDir.resolve("source"))) {
            Path fixture = FIXTURE_DIR.resolve("h2-" + h2Version(connection) + ".sql");
            assumeFalse(Files.exists(fixture), () -> "이 H2 버전의 고정 덤프가 이미 있습니다: " + fixture);
            H2DumpFixtureData.create(connection);
            Files.createDirectories(FIXTURE_DIR);

            // When
            databaseManager.writeDump(connection, fixture, H2DumpFixtureData.FILE_PASSWORD);

            // Then
            assertThat(fixture).exists();
        }
    }

    /**
     * "2.4.240 (2025-09-22)" 형식의 제품 버전에서 버전 번호만 꺼낸다.
     */
    private String h2Version(Connection connection) throws Exception {
        return connection.getMetaData()
                .getDatabaseProductVersion()
                .split(" ", 2)[0];
    }

}
