package org.church.steward.domain.budget.mapper;

import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.springframework.stereotype.Component;

@Component
public class BudgetMapper {

    public BudgetDto toDto(Budget budget) {
        return toDto(budget, null);
    }

    /**
     * @param actualAmount 이 예산과 그 하위 예산에 등록된 수입/지출 내역의 금액 합계. 진행 현황을 조회하지 않았으면 null
     */
    public BudgetDto toDto(Budget budget, Long actualAmount) {
        if (budget == null) {
            return null;
        }

        return BudgetDto.builder()
                .id(budget.getId())
                .fiscalYearId(budget.getFiscalYear().getId())
                .type(budget.getType())
                .parentId(budget.getParent() != null ? budget.getParent().getId() : null)
                .name(budget.getName())
                .amount(budget.getAmount())
                .sortOrder(budget.getSortOrder())
                .totalAmount(budget.getTotalAmount())
                .actualAmount(actualAmount)
                .build();
    }

    public BudgetAutocompleteDto toAutocompleteDto(Budget budget) {
        if (budget == null) {
            return null;
        }

        return BudgetAutocompleteDto.builder()
                .id(budget.getId())
                .name(budget.getName())
                .hierarchyName(budget.getHierarchyName())
                .build();
    }
}
