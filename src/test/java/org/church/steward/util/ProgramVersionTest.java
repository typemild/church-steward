package org.church.steward.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

class ProgramVersionTest {

    @Nested
    @DisplayName("current 테스트")
    class CurrentTest {

        @Test
        @DisplayName("설정 파일의 program.info.version 값을 읽는다")
        void current_VersionPresent_ReturnsVersion() {
            // Given
            ProgramVersion programVersion = new ProgramVersion(yaml("""
                    program:
                      info:
                        version: "1.2.3-SNAPSHOT"
                    """));

            // When
            String version = programVersion.current();

            // Then
            assertThat(version).isEqualTo("1.2.3-SNAPSHOT");
        }

        @Test
        @DisplayName("설정 파일에 program.info.version 값이 없으면 빈 문자열을 반환한다")
        void current_VersionMissing_ReturnsEmpty() {
            // Given
            ProgramVersion programVersion = new ProgramVersion(yaml("""
                    program:
                      info:
                        license: "MIT License"
                    """));

            // When
            String version = programVersion.current();

            // Then
            assertThat(version).isEmpty();
        }

        private Resource yaml(String content) {
            return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nested
    @DisplayName("isNewer 테스트")
    class IsNewerTest {

        @Test
        @DisplayName("앞자리 숫자가 더 크면 새 버전이다")
        void isNewer_GreaterMajor_ReturnsTrue() {
            // Given
            String version = "2.0.0";
            String other = "1.9.9";

            // When
            boolean newer = ProgramVersion.isNewer(version, other);

            // Then
            assertThat(newer).isTrue();
        }

        @Test
        @DisplayName("숫자는 글자가 아니라 수로 비교한다 (1.10.0은 1.9.0보다 새 버전)")
        void isNewer_TwoDigitNumber_ComparesNumerically() {
            // Given
            String version = "1.10.0";
            String other = "1.9.0";

            // When
            boolean newer = ProgramVersion.isNewer(version, other);

            // Then
            assertThat(newer).isTrue();
        }

        @Test
        @DisplayName("같은 버전은 새 버전이 아니다")
        void isNewer_SameVersion_ReturnsFalse() {
            // Given
            String version = "1.2.3";

            // When
            boolean newer = ProgramVersion.isNewer(version, version);

            // Then
            assertThat(newer).isFalse();
        }

        @Test
        @DisplayName("더 낮은 버전은 새 버전이 아니다")
        void isNewer_OlderVersion_ReturnsFalse() {
            // Given
            String version = "0.9.0";
            String other = "1.0.0";

            // When
            boolean newer = ProgramVersion.isNewer(version, other);

            // Then
            assertThat(newer).isFalse();
        }

        @Test
        @DisplayName("-SNAPSHOT 같은 접미어는 무시하고 숫자만 비교한다")
        void isNewer_SnapshotSuffix_IgnoresSuffix() {
            // Given
            String version = "0.0.1-SNAPSHOT";
            String other = "0.0.1";

            // When
            boolean newer = ProgramVersion.isNewer(version, other);

            // Then
            assertThat(newer).isFalse();
        }

        @Test
        @DisplayName("자릿수가 다르면 없는 자리를 0으로 본다 (1.0.1은 1.0보다 새 버전)")
        void isNewer_DifferentLength_TreatsMissingAsZero() {
            // Given
            String version = "1.0.1";
            String other = "1.0";

            // When
            boolean newer = ProgramVersion.isNewer(version, other);

            // Then
            assertThat(newer).isTrue();
        }
    }

}
