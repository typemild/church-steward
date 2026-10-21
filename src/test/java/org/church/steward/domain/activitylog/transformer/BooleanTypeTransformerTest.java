package org.church.steward.domain.activitylog.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BooleanTypeTransformerTest {

    private final BooleanTypeTransformer transformer = new BooleanTypeTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("Boolean 값이 true일 때 'O'를 반환한다")
        void transform_true_returns_O() {
            // Given
            Boolean value = true;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo("O");
        }

        @Test
        @DisplayName("Boolean 값이 false일 때 'X'를 반환한다")
        void transform_false_returns_X() {
            // Given
            Boolean value = false;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo("X");
        }

        @Test
        @DisplayName("입력값이 null일 때 빈 문자열을 반환한다")
        void transform_null_returns_emptyString() {
            // Given
            Object value = null;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo("");
        }

        @Test
        @DisplayName("Boolean 타입이 아닌 값이 입력되면 IllegalArgumentException을 발생시킨다")
        void transform_invalidType_throwsIllegalArgumentException() {
            // Given
            Object value = "not a boolean";

            // When & Then
            assertThatThrownBy(() -> transformer.transform(value))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
