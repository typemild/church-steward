package org.church.steward.domain.budget.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.service.BudgetService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetControllerTest {

    @Mock
    private BudgetService budgetService;

    @InjectMocks
    private BudgetController budgetController;

    @Nested
    @DisplayName("getBudgetsByFiscalYearAndType 테스트")
    class GetBudgetsByFiscalYearAndTypeTest {

        @Test
        @DisplayName("회계 연도와 타입으로 예산 목록을 조회하면 예산 DTO 목록을 반환한다.")
        void getBudgetsByFiscalYearAndType_whenValidParameters_shouldReturnBudgetDtos() {
            // Given
            Long fiscalYearId = 7L;
            BudgetType type = BudgetType.INCOME;
            BudgetDto budget = BudgetDto.builder().id(1L).name("수입항목").type(type).build();
            when(budgetService.getBudgetsByFiscalYearAndType(fiscalYearId, type)).thenReturn(List.of(budget));

            // When
            List<BudgetDto> result = budgetController.getBudgetsByFiscalYearAndType(fiscalYearId, type);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("수입항목");
        }

        @Test
        @DisplayName("회계 연도와 타입으로 예산 목록을 조회하면 서비스의 메소드를 호출한다.")
        void getBudgetsByFiscalYearAndType_whenValidParameters_shouldCallService() {
            // Given
            Long fiscalYearId = 7L;
            BudgetType type = BudgetType.INCOME;

            // When
            budgetController.getBudgetsByFiscalYearAndType(fiscalYearId, type);

            // Then
            verify(budgetService).getBudgetsByFiscalYearAndType(fiscalYearId, type);
        }
    }

    @Nested
    @DisplayName("getBudgetsWithActualAmount 테스트")
    class GetBudgetsWithActualAmountTest {

        @Test
        @DisplayName("진행 현황과 함께 예산 목록을 조회하면 서비스가 돌려준 예산 DTO 목록을 그대로 반환한다.")
        void getBudgetsWithActualAmount_whenValidParameters_shouldReturnBudgetDtos() {
            // Given
            Long fiscalYearId = 7L;
            BudgetType type = BudgetType.EXPENSE;
            List<BudgetDto> budgets = List.of(BudgetDto.builder().id(1L).name("지출항목").actualAmount(3000L).build());
            when(budgetService.getBudgetsWithActualAmount(fiscalYearId, type)).thenReturn(budgets);

            // When
            List<BudgetDto> result = budgetController.getBudgetsWithActualAmount(fiscalYearId, type);

            // Then
            assertThat(result).isSameAs(budgets);
        }
    }

    @Nested
    @DisplayName("createBudget 테스트")
    class CreateBudgetTest {

        @Test
        @DisplayName("새 예산을 생성하면 서비스의 생성 메소드를 호출한다.")
        void createBudget_whenValidParameters_shouldCallService() {
            // Given
            Long fiscalYearId = 7L;
            Long parentId = 1L;
            String name = "새 예산";
            long amount = 50000L;
            BudgetType type = BudgetType.EXPENSE;

            // When
            budgetController.createBudget(fiscalYearId, parentId, name, amount, type);

            // Then
            verify(budgetService).createBudget(eq(fiscalYearId), eq(parentId), eq(name), eq(amount), eq(type));
        }
    }

    @Nested
    @DisplayName("updateBudget 테스트")
    class UpdateBudgetTest {

        @Test
        @DisplayName("예산을 수정하면 서비스의 수정 메소드를 호출한다.")
        void updateBudget_whenValidId_shouldCallService() {
            // Given
            Long id = 1L;
            String newName = "수정된 이름";
            long newAmount = 60000L;

            // When
            budgetController.updateBudget(id, newName, newAmount);

            // Then
            verify(budgetService).updateBudget(id, newName, newAmount);
        }
    }

    @Nested
    @DisplayName("deleteBudget 테스트")
    class DeleteBudgetTest {

        @Test
        @DisplayName("예산을 삭제하면 서비스의 삭제 메소드를 호출한다.")
        void deleteBudget_whenValidId_shouldCallService() {
            // Given
            Long id = 1L;

            // When
            budgetController.deleteBudget(id);

            // Then
            verify(budgetService).deleteBudget(id);
        }
    }

    @Nested
    @DisplayName("moveBudgetUp 테스트")
    class MoveBudgetUpTest {

        @Test
        @DisplayName("예산을 위로 이동시키면 서비스의 이동 메소드를 호출한다.")
        void moveBudgetUp_whenValidId_shouldCallService() {
            // Given
            Long id = 1L;

            // When
            budgetController.moveBudgetUp(id);

            // Then
            verify(budgetService).moveBudgetUp(id);
        }
    }

    @Nested
    @DisplayName("moveBudgetDown 테스트")
    class MoveBudgetDownTest {

        @Test
        @DisplayName("예산을 아래로 이동시키면 서비스의 이동 메소드를 호출한다.")
        void moveBudgetDown_whenValidId_shouldCallService() {
            // Given
            Long id = 1L;

            // When
            budgetController.moveBudgetDown(id);

            // Then
            verify(budgetService).moveBudgetDown(id);
        }
    }

    @Nested
    @DisplayName("autocompleteBudgets 테스트")
    class AutocompleteBudgetsTest {

        @Test
        @DisplayName("자동완성 검색 시 조건에 맞는 예산 목록을 반환한다.")
        void autocompleteBudgets_whenValidParameters_shouldReturnAutocompleteDtos() {
            // Given
            Long fiscalYearId = 7L;
            BudgetType type = BudgetType.INCOME;
            String name = "수입";
            int limit = 5;
            BudgetAutocompleteDto budget = BudgetAutocompleteDto.builder()
                    .id(1L)
                    .name("수입항목")
                    .hierarchyName("Root > 수입항목")
                    .build();
            when(budgetService.getBudgetsForAutocomplete(fiscalYearId, type, name, limit)).thenReturn(List.of(budget));

            // When
            List<BudgetAutocompleteDto> result = budgetController.autocompleteBudgets(fiscalYearId, type, name, limit);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("수입항목");
            assertThat(result.get(0).getHierarchyName()).isEqualTo("Root > 수입항목");
        }

        @Test
        @DisplayName("자동완성 검색 시 조건에 맞는 예산 목록을 반환하면 서비스의 메소드를 호출한다.")
        void autocompleteBudgets_whenValidParameters_shouldCallService() {
            // Given
            Long fiscalYearId = 7L;
            BudgetType type = BudgetType.INCOME;
            String name = "수입";
            int limit = 5;

            // When
            budgetController.autocompleteBudgets(fiscalYearId, type, name, limit);

            // Then
            verify(budgetService).getBudgetsForAutocomplete(fiscalYearId, type, name, limit);
        }
    }
}
