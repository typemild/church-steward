package org.church.steward.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서 종류. 회계 연도 예산안과 회계 연도 결산 세부 내역서는 수입과 지출의 보고서를 차례로 묶은 것이며, 묶인 보고서마다 새 페이지에서
 * 시작한다.
 */
@Getter
@AllArgsConstructor
public enum ReportType {

    WEEKLY_SETTLEMENT("주간 회계 결산 보고서"),
    /** 수입 예산안, 지출 예산안 순 */
    BUDGET_PLAN("회계 연도 예산안"),
    ANNUAL_SETTLEMENT("회계 연도 결산 보고서"),
    /** 수입 결산 세부 내역서, 지출 결산 세부 내역서 순 */
    SETTLEMENT_DETAIL("회계 연도 결산 세부 내역서");

    private final String label;

    /**
     * 회계 연도 외에 주간 범위를 함께 지정해야 하는 보고서인지 여부
     */
    public boolean isWeekly() {
        return this == WEEKLY_SETTLEMENT;
    }

    /**
     * 보고서 양식에 결재란이 있는지 여부. 결재란은 보고서마다 따로 저장한다.
     */
    public boolean hasApproval() {
        return this == WEEKLY_SETTLEMENT || this == ANNUAL_SETTLEMENT;
    }

    /**
     * 보고서 양식에서 정할 것(결재란, 칸의 순서와 숨김)이 있는지 여부
     */
    public boolean hasTemplate() {
        return hasApproval() || !ReportTemplateTable.of(this)
                .isEmpty();
    }
}
