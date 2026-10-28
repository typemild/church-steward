package org.church.steward.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서 종류
 */
@Getter
@AllArgsConstructor
public enum ReportType {

    WEEKLY_SETTLEMENT("주간 회계 결산 보고서"),
    INCOME_BUDGET_PLAN("회계 연도 수입 예산안"),
    EXPENSE_BUDGET_PLAN("회계 연도 지출 예산안"),
    ANNUAL_SETTLEMENT("회계 연도 수입/지출 결산 보고서"),
    INCOME_SETTLEMENT_DETAIL("회계 연도 수입 결산 세부 내역서"),
    EXPENSE_SETTLEMENT_DETAIL("회계 연도 지출 결산 세부 내역서");

    private final String label;

    /**
     * 회계 연도 외에 주간 범위를 함께 지정해야 하는 보고서인지 여부
     */
    public boolean isWeekly() {
        return this == WEEKLY_SETTLEMENT;
    }
}
