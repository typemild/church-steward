package org.church.steward.domain.budget.transformer;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetAmountTransformerTest {

    private final BudgetAmountTransformer transformer = new BudgetAmountTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("입력값이 null이면 하위 예산 항목이 등록되었다는 메시지를 반환한다")
        void transform_null_returnsSubItemMessage() {
            // Given

            // When
            Object result = transformer.transform(null);

            // Then
            assertThat(result).isEqualTo("(하위 예산 항목이 등록됨)");
        }

        @Test
        @DisplayName("입력값이 null이 아니면 입력된 값을 그대로 반환한다")
        void transform_notNull_returnsOriginalValue() {
            // Given
            Long amount = 5000L;

            // When
            Object result = transformer.transform(amount);

            // Then
            assertThat(result).isEqualTo(amount);
        }
    }
}
