package org.church.steward.domain.budget.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.budget.entity.BudgetType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetTypeTransformerTest {

    private final BudgetTypeTransformer transformer = new BudgetTypeTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("BudgetType이 INCOME인 경우 '수입'을 반환한다")
        void transform_incomeType_returnsIncomeLabel() {
            // Given
            BudgetType type = BudgetType.INCOME;

            // When
            Object result = transformer.transform(type);

            // Then
            assertThat(result).isEqualTo("수입");
        }

        @Test
        @DisplayName("BudgetType이 EXPENSE인 경우 '지출'을 반환한다")
        void transform_expenseType_returnsExpenseLabel() {
            // Given
            BudgetType type = BudgetType.EXPENSE;

            // When
            Object result = transformer.transform(type);

            // Then
            assertThat(result).isEqualTo("지출");
        }

        @Test
        @DisplayName("BudgetType이 아닌 객체를 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_invalidType_throwsIllegalArgumentException() {
            // Given
            Object invalidType = "not a type";

            // When & Then
            assertThatThrownBy(() -> transformer.transform(invalidType))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null을 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_nullValue_throwsIllegalArgumentException() {
            // Given
            Object nullValue = null;

            // When & Then
            assertThatThrownBy(() -> transformer.transform(nullValue))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
