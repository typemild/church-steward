package org.church.steward.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TextUtilsTest {

    @Nested
    @DisplayName("normalize 테스트")
    class NormalizeTest {

        @Test
        @DisplayName("null은 null로 돌려준다")
        void normalize_null_returnsNull() {
            // Given
            String text = null;

            // When
            String result = TextUtils.normalize(text);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("빈 문자열은 null로 바꾼다")
        void normalize_empty_returnsNull() {
            // Given
            String text = "";

            // When
            String result = TextUtils.normalize(text);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("전각 공백과 줄바꿈을 포함해 공백만 있으면 null로 바꾼다")
        void normalize_whitespaceOnly_returnsNull() {
            // Given
            String text = " 　\t\n ";

            // When
            String result = TextUtils.normalize(text);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("앞뒤 공백(전각 공백 포함)을 지우고 가운데 공백은 그대로 둔다")
        void normalize_surroundingWhitespace_returnsStripped() {
            // Given
            String text = "　 주일 헌금 \t";

            // When
            String result = TextUtils.normalize(text);

            // Then
            assertThat(result).isEqualTo("주일 헌금");
        }
    }
}
