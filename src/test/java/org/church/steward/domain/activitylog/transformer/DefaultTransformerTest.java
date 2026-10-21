package org.church.steward.domain.activitylog.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DefaultTransformerTest {

    private final DefaultTransformer transformer = new DefaultTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("null이 입력되었을 때 null을 그대로 반환한다")
        void transform_whenNullInput_returnsNull() {
            // Given
            Object value = null;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("문자열이 입력되었을 때 입력된 문자열을 그대로 반환한다")
        void transform_whenStringInput_returnsOriginalString() {
            // Given
            String value = "test";

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo(value);
        }

        @Test
        @DisplayName("정수가 입력되었을 때 입력된 정수를 그대로 반환한다")
        void transform_whenIntegerInput_returnsOriginalInteger() {
            // Given
            Integer value = 123;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo(value);
        }

        @Test
        @DisplayName("불리언이 입력되었을 때 입력된 불리언을 그대로 반환한다")
        void transform_whenBooleanInput_returnsOriginalBoolean() {
            // Given
            Boolean value = true;

            // When
            Object result = transformer.transform(value);

            // Then
            assertThat(result).isEqualTo(value);
        }
    }
}
