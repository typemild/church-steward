package org.church.steward.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppHomeTest {

    @TempDir
    Path tempDir;

    @Nested
    @DisplayName("determine 테스트")
    class DetermineTest {
        @Test
        @DisplayName("determine_jpackageAppPath_returnsLauncherDirectory: jpackage 실행 파일로 실행하면 jar나 작업 디렉터리와 무관하게 실행 파일이 있는 폴더를 프로그램 폴더로 한다.")
        void determine_jpackageAppPath_returnsLauncherDirectory() throws IOException {
            // Given
            Path launcherDir = Files.createDirectories(tempDir.resolve("ChurchSteward"));
            Path launcher = launcherDir.resolve("ChurchSteward.exe");
            Path jar = Files.createFile(Files.createDirectories(launcherDir.resolve("app")).resolve("app.jar"));

            // When
            Path dir = AppHome.determine(launcher.toString(), jar.toFile(), tempDir.resolve("working"));

            // Then
            assertThat(dir).isEqualTo(launcherDir.toAbsolutePath());
        }

        @Test
        @DisplayName("determine_jarFile_returnsJarDirectory: jar로 실행하면 작업 디렉터리와 무관하게 jar가 있는 폴더를 프로그램 폴더로 한다.")
        void determine_jarFile_returnsJarDirectory() throws IOException {
            // Given
            Path jar = Files.createFile(tempDir.resolve("church-steward.jar"));

            // When
            Path dir = AppHome.determine(null, jar.toFile(), tempDir.resolve("working"));

            // Then
            assertThat(dir).isEqualTo(tempDir.toAbsolutePath());
        }

        @Test
        @DisplayName("determine_classesDirectory_returnsWorkingDirectory: IDE나 bootRun처럼 클래스 디렉터리에서 실행하면 작업 디렉터리를 프로그램 폴더로 한다.")
        void determine_classesDirectory_returnsWorkingDirectory() throws IOException {
            // Given
            Path classesDir = Files.createDirectories(tempDir.resolve("build/classes/java/main"));
            Path workingDir = tempDir.resolve("working");

            // When
            Path dir = AppHome.determine(null, classesDir.toFile(), workingDir);

            // Then
            assertThat(dir).isEqualTo(workingDir.toAbsolutePath());
        }

        @Test
        @DisplayName("determine_unknownSource_returnsWorkingDirectory: 클래스를 읽어 들인 위치를 알 수 없으면 작업 디렉터리를 프로그램 폴더로 한다.")
        void determine_unknownSource_returnsWorkingDirectory() {
            // Given
            Path workingDir = tempDir.resolve("working");

            // When
            Path dir = AppHome.determine(null, null, workingDir);

            // Then
            assertThat(dir).isEqualTo(workingDir.toAbsolutePath());
        }

        @Test
        @DisplayName("determine_blankJpackageAppPath_ignoresIt: jpackage 속성이 비어 있으면 없는 것으로 보고 jar가 있는 폴더를 프로그램 폴더로 한다.")
        void determine_blankJpackageAppPath_ignoresIt() throws IOException {
            // Given
            Path jar = Files.createFile(tempDir.resolve("church-steward.jar"));

            // When
            Path dir = AppHome.determine(" ", jar.toFile(), tempDir.resolve("working"));

            // Then
            assertThat(dir).isEqualTo(tempDir.toAbsolutePath());
        }
    }
}
