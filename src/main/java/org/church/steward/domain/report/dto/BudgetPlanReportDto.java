package org.church.steward.domain.report.dto;

import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 연도 수입 예산안 또는 회계 연도 지출 예산안의 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPlanReportDto {
    private String fiscalYearName;
    private BudgetType type;

    /** 예산 관리 화면의 트리와 같은 순서로 나열한 예산 */
    private List<BudgetPlanRowDto> rows;
    private long totalAmount;

    /** 직전 회계 연도의 예산 총액. 직전 회계 연도가 없으면 null이며, 이때는 모든 예산을 신규로 본다. */
    private Long previousTotalAmount;
    /**
     * 직전 회계 연도에만 있고 이번 회계 연도에는 없는 예산들의 직전 회계 연도 금액 합계. 그런 예산이 없으면 null. 상위 예산째 없어진
     * 경우에는 상위 예산의 금액만 세어 하위 예산과 겹치지 않게 한다.
     */
    private Long removedAmount;

    /**
     * 직전 회계 연도 대비 총액의 증감액. 직전 회계 연도가 없으면 null
     */
    public Long getTotalChange() {
        return previousTotalAmount == null ? null : totalAmount - previousTotalAmount;
    }
}
