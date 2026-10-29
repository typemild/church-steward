package org.church.steward.domain.budget.service;

import java.util.List;

import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;

public interface BudgetService {

    List<BudgetDto> getBudgetsByFiscalYearAndType(Long fiscalYearId, BudgetType type);

    /**
     * 예산을 진행 현황과 함께 조회한다. 예산마다 그 예산과 하위 예산에 등록된 내역의 금액 합계 (수입 예산은 수입액, 지출 예산은 집행액)를
     * {@link BudgetDto#getActualAmount()}에 담는다.
     */
    List<BudgetDto> getBudgetsWithActualAmount(Long fiscalYearId, BudgetType type);

    void createBudget(Long fiscalYearId, Long parentId, String name, long amount, BudgetType type);

    void updateBudget(Long id, String newName, long newAmount);

    /**
     * 활동 기록을 남기지 않고 예산을 추가한다. 검증은 {@link #createBudget}과 같다. 여러 예산을 한꺼번에 처리하고 활동
     * 기록을 한 건으로 남기는 가져오기에서만 쓴다.
     *
     * @return 추가된 예산
     */
    Budget createBudgetWithoutLog(Long fiscalYearId, Long parentId, String name, long amount, BudgetType type);

    /**
     * 활동 기록을 남기지 않고 예산을 수정한다. 검증은 {@link #updateBudget}과 같다. 여러 예산을 한꺼번에 처리하고 활동
     * 기록을 한 건으로 남기는 가져오기에서만 쓴다.
     *
     * @return 수정된 예산
     */
    Budget updateBudgetWithoutLog(Long id, String newName, long newAmount);

    void deleteBudget(Long id);

    void moveBudgetUp(Long id);

    void moveBudgetDown(Long id);

    List<BudgetAutocompleteDto> getBudgetsForAutocomplete(Long fiscalYearId,
            BudgetType type, String name, int limit);

}
