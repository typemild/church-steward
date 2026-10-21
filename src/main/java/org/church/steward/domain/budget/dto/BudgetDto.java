package org.church.steward.domain.budget.dto;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetDto {
    private Long id;
    private Long fiscalYearId;
    private BudgetType type;
    private Long parentId;
    private String name;
    private Long amount;
    private int sortOrder;
    private long totalAmount;
    /**
     * 이 예산과 그 하위 예산에 등록된 수입/지출 내역의 금액 합계(수입 예산은 수입액, 지출 예산은 집행액).
     * 진행 현황을 함께 조회했을 때만 채워지며, 그 외의 경우에는 null이다.
     */
    private Long actualAmount;
}
