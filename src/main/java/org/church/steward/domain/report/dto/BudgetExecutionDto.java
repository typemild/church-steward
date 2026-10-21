package org.church.steward.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예산 하나의 예산액과 집행액(수입 예산이면 실제 수입액).
 * 하위 예산이 있는 예산은 하위 예산들의 합계를 담는다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetExecutionDto {
    private String name;
    /** 예산 계층에서의 깊이. 최상위 예산이 0이다. */
    private int depth;
    private long budgetedAmount;
    private long actualAmount;
    /** 같은 구분의 예산 총액에서 이 예산이 차지하는 비율(%). 예산 총액이 0이면 null */
    private Double shareRate;
    /** 예산액 대비 집행액의 비율(%). 예산액이 0이면 null */
    private Double executionRate;
}
