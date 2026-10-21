package org.church.steward.domain.budget.controller;

import java.util.List;

import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;

    public List<BudgetDto> getBudgetsByFiscalYearAndType(Long fiscalYearId, BudgetType type) {
        return budgetService.getBudgetsByFiscalYearAndType(fiscalYearId, type);
    }

    public List<BudgetDto> getBudgetsWithActualAmount(Long fiscalYearId, BudgetType type) {
        return budgetService.getBudgetsWithActualAmount(fiscalYearId, type);
    }

    public void createBudget(Long fiscalYearId, Long parentId, String name, long amount, BudgetType type) {
        budgetService.createBudget(fiscalYearId, parentId, name, amount, type);
    }

    public void updateBudget(Long id, String newName, long newAmount) {
        budgetService.updateBudget(id, newName, newAmount);
    }

    public void deleteBudget(Long id) {
        budgetService.deleteBudget(id);
    }

    public void moveBudgetUp(Long id) {
        budgetService.moveBudgetUp(id);
    }

    public void moveBudgetDown(Long id) {
        budgetService.moveBudgetDown(id);
    }

    public List<BudgetAutocompleteDto> autocompleteBudgets(Long fiscalYearId, BudgetType type, String name, int limit) {
        return budgetService.getBudgetsForAutocomplete(fiscalYearId, type, name, limit);
    }

}
