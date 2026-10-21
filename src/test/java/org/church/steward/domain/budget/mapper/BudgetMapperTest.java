package org.church.steward.domain.budget.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetMapperTest {

    private final BudgetMapper budgetMapper = new BudgetMapper();

    private FiscalYear createFiscalYear() {
        return FiscalYear.builder()
                .id(7L)
                .name("2024년")
                .build();
    }

    @Nested
    @DisplayName("toDto 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("Budget 엔티티가 null이면 null을 반환해야 한다")
        void toDto_whenEntityIsNull_thenReturnsNull() {
            // Given
            Budget budget = null;

            // When
            BudgetDto dto = budgetMapper.toDto(budget);

            // Then
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("Budget 엔티티를 BudgetDto로 변환할 때, 모든 필드가 올바르게 매핑되어야 한다")
        void toDto_whenEntityIsProvided_thenAllFieldsAreMapped() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(createFiscalYear())
                    .type(BudgetType.INCOME)
                    .name("테스트 예산")
                    .amount(10000L)
                    .sortOrder(1)
                    .build();

            // When
            BudgetDto dto = budgetMapper.toDto(budget);

            // Then
            assertThat(dto.getId()).isEqualTo(budget.getId());
            assertThat(dto.getFiscalYearId()).isEqualTo(7L);
            assertThat(dto.getType()).isEqualTo(budget.getType());
            assertThat(dto.getName()).isEqualTo(budget.getName());
            assertThat(dto.getAmount()).isEqualTo(budget.getAmount());
            assertThat(dto.getSortOrder()).isEqualTo(budget.getSortOrder());
            assertThat(dto.getTotalAmount()).isEqualTo(budget.getTotalAmount());
        }

        @Test
        @DisplayName("Budget 엔티티를 BudgetDto로 변환할 때, 부모가 없는 경우 parentId는 null이어야 한다")
        void toDto_whenParentIsNull_thenParentIdIsNull() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(createFiscalYear())
                    .parent(null)
                    .build();

            // When
            BudgetDto dto = budgetMapper.toDto(budget);

            // Then
            assertThat(dto.getParentId()).isNull();
        }

        @Test
        @DisplayName("Budget 엔티티를 BudgetDto로 변환할 때, 부모가 있는 경우 parentId는 부모의 ID여야 한다")
        void toDto_whenParentIsPresent_thenParentIdIsMatch() {
            // Given
            FiscalYear fiscalYear = createFiscalYear();
            Budget parentBudget = Budget.builder()
                    .id(10L)
                    .fiscalYear(fiscalYear)
                    .build();

            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .parent(parentBudget)
                    .build();

            // When
            BudgetDto dto = budgetMapper.toDto(budget);

            // Then
            assertThat(dto.getParentId()).isEqualTo(parentBudget.getId());
        }

        @Test
        @DisplayName("진행 현황 없이 변환하면 actualAmount는 null이어야 한다")
        void toDto_whenActualAmountIsNotGiven_thenActualAmountIsNull() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(createFiscalYear())
                    .build();

            // When
            BudgetDto dto = budgetMapper.toDto(budget);

            // Then
            assertThat(dto.getActualAmount()).isNull();
        }

        @Test
        @DisplayName("진행 현황과 함께 변환하면 넘겨받은 금액이 actualAmount에 담겨야 한다")
        void toDto_whenActualAmountIsGiven_thenActualAmountIsMapped() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(createFiscalYear())
                    .amount(10000L)
                    .build();

            // When
            BudgetDto dto = budgetMapper.toDto(budget, 2500L);

            // Then
            assertThat(dto.getActualAmount()).isEqualTo(2500L);
        }
    }

    @Nested
    @DisplayName("toAutocompleteDto 테스트")
    class ToAutocompleteDtoTest {

        @Test
        @DisplayName("Budget 엔티티가 null이면 null을 반환해야 한다")
        void toAutocompleteDto_whenEntityIsNull_thenReturnsNull() {
            // Given
            Budget budget = null;

            // When
            BudgetAutocompleteDto dto = budgetMapper.toAutocompleteDto(budget);

            // Then
            assertThat(dto).isNull();
        }

        @Test
        @DisplayName("Budget 엔티티를 변환하면 ID, 이름, 상위 예산을 포함한 전체 예산명이 담겨야 한다")
        void toAutocompleteDto_whenEntityIsProvided_thenHierarchyNameIsMapped() {
            // Given
            FiscalYear fiscalYear = createFiscalYear();
            Budget parentBudget = Budget.builder()
                    .id(10L)
                    .fiscalYear(fiscalYear)
                    .name("상위 예산")
                    .build();
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .parent(parentBudget)
                    .name("하위 예산")
                    .build();

            // When
            BudgetAutocompleteDto dto = budgetMapper.toAutocompleteDto(budget);

            // Then
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("하위 예산");
            assertThat(dto.getHierarchyName()).isEqualTo("상위 예산 > 하위 예산");
        }
    }
}
