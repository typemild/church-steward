package org.church.steward.domain.budget.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.budget.entity.Budget;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetTransformerTest {

    private final BudgetTransformer transformer = new BudgetTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("Budget 객체를 전달하면 계층 구조를 ' > ' 구분자로 연결된 문자열로 변환한다")
        void transform_budget_returnsHierarchyName() {
            // Given
            Budget root = Budget.builder().name("Root").build();
            Budget child = Budget.builder().name("Child").parent(root).build();
            Budget grandchild = Budget.builder().name("Grandchild").parent(child).build();

            // When
            Object result = transformer.transform(grandchild);

            // Then
            assertThat(result).isEqualTo("Root > Child > Grandchild");
        }

        @Test
        @DisplayName("Budget이 아닌 객체를 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_notBudget_throwsIllegalArgumentException() {
            // Given
            String invalidInput = "not a budget";

            // When & Then
            assertThatThrownBy(() -> transformer.transform(invalidInput))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("null을 전달하면 '(없음)'을 반환한다")
        void transform_null_returnsEmptyString() {
            // Given
            Object nullInput = null;

            // When
            Object result = transformer.transform(nullInput);

            // Then
            assertThat(result).isEqualTo("(없음)");
        }
    }
}
