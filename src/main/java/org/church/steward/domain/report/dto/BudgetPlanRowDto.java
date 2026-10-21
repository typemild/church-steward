package org.church.steward.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 연도 예산안의 예산 한 줄
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetPlanRowDto {
    private String name;
    /** 예산 계층에서의 깊이. 최상위 예산이 0이다. */
    private int depth;
    private long amount;
    /** 같은 구분의 예산 총액에서 이 예산이 차지하는 비율(%). 예산 총액이 0이면 null */
    private Double shareRate;
    /** 직전 회계 연도에서 전체 경로가 같은 예산의 금액. 그런 예산이 없으면 null */
    private Long previousAmount;

    /**
     * 직전 회계 연도 대비 증감액. 직전 회계 연도에 같은 예산이 없으면 null
     */
    public Long getChange() {
        return previousAmount == null ? null : amount - previousAmount;
    }
}
