package org.church.steward.domain.budget.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetTest {

    @Nested
    @DisplayName("getTotalAmount 테스트")
    class GetTotalAmountTest {

        @Test
        @DisplayName("자식 예산이 없고 금액이 설정된 경우 자신의 금액을 반환한다.")
        void getTotalAmount_NoChildrenAndHasAmount_ReturnsAmount() {
            // Given
            Budget budget = Budget.builder()
                    .name("Root")
                    .amount(1000L)
                    .build();

            // When
            long totalAmount = budget.getTotalAmount();

            // Then
            assertThat(totalAmount).isEqualTo(1000L);
        }

        @Test
        @DisplayName("자식 예산이 없고 금액이 설정되지 않은 경우 0을 반환한다.")
        void getTotalAmount_NoChildrenAndNoAmount_ReturnsZero() {
            // Given
            Budget budget = Budget.builder()
                    .name("Root")
                    .amount(null)
                    .build();

            // When
            long totalAmount = budget.getTotalAmount();

            // Then
            assertThat(totalAmount).isEqualTo(0L);
        }

        @Test
        @DisplayName("자식 예산이 있는 경우 자식들의 총 금액 합계를 재귀적으로 반환한다.")
        void getTotalAmount_WithChildren_ReturnsSumOfChildren() {
            // Given
            Budget root = Budget.builder()
                    .name("Root")
                    .build();

            Budget child1 = Budget.builder()
                    .name("Child 1")
                    .amount(500L)
                    .parent(root)
                    .build();

            Budget child2 = Budget.builder()
                    .name("Child 2")
                    .amount(300L)
                    .parent(root)
                    .build();

            root.getChildren().add(child1);
            root.getChildren().add(child2);

            // When
            long totalAmount = root.getTotalAmount();

            // Then
            assertThat(totalAmount).isEqualTo(800L);
        }

        @Test
        @DisplayName("자식 예산이 있는 경우 자식의 자식까지 포함하여 합계를 계산한다.")
        void getTotalAmount_WithGrandchildren_ReturnsRecursiveSum() {
            // Given
            Budget root = Budget.builder().name("Root").build();
            Budget child = Budget.builder().name("Child").parent(root).build();
            Budget grandchild = Budget.builder().name("Grandchild").amount(200L).parent(child).build();

            root.getChildren().add(child);
            child.getChildren().add(grandchild);

            // When
            long totalAmount = root.getTotalAmount();

            // Then
            assertThat(totalAmount).isEqualTo(200L);
        }
    }

    @Nested
    @DisplayName("hasChildren 테스트")
    class HasChildrenTest {

        @Test
        @DisplayName("자식이 있으면 true를 반환한다.")
        void hasChildren_WithChildren_ReturnsTrue() {
            // Given
            Budget root = Budget.builder().name("Root").build();
            Budget child = Budget.builder().name("Child").parent(root).build();
            root.getChildren().add(child);

            // When
            boolean hasChildren = root.hasChildren();

            // Then
            assertThat(hasChildren).isTrue();
        }

        @Test
        @DisplayName("자식이 없으면 false를 반환한다.")
        void hasChildren_NoChildren_ReturnsFalse() {
            // Given
            Budget root = Budget.builder().name("Root").build();

            // When
            boolean hasChildren = root.hasChildren();

            // Then
            assertThat(hasChildren).isFalse();
        }
    }

    @Nested
    @DisplayName("getHierarchyName 테스트")
    class GetHierarchyNameTest {

        @Test
        @DisplayName("부모가 없는 경우 자신의 이름을 반환한다.")
        void getHierarchyName_NoParent_ReturnsItsOwnName() {
            // Given
            Budget root = Budget.builder().name("Root").build();

            // When
            String hierarchyName = root.getHierarchyName();

            // Then
            assertThat(hierarchyName).isEqualTo("Root");
        }

        @Test
        @DisplayName("부모가 있는 경우 부모부터 현재까지의 계층 이름을 ' > '로 구분하여 반환한다.")
        void getHierarchyName_WithParent_ReturnsFullHierarchyName() {
            // Given
            Budget root = Budget.builder().name("Root").build();
            Budget child = Budget.builder().name("Child").parent(root).build();
            root.getChildren().add(child);

            Budget grandchild = Budget.builder().name("Grandchild").parent(child).build();
            child.getChildren().add(grandchild);

            // When
            String rootName = root.getHierarchyName();
            String childName = child.getHierarchyName();
            String grandchildName = grandchild.getHierarchyName();

            // Then
            assertThat(rootName).isEqualTo("Root");
            assertThat(childName).isEqualTo("Root > Child");
            assertThat(grandchildName).isEqualTo("Root > Child > Grandchild");
        }
    }
}
