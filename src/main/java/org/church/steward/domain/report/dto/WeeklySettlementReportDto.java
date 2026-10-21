package org.church.steward.domain.report.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주간 회계 결산 보고서의 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeeklySettlementReportDto {
    private String fiscalYearName;
    private LocalDate startDate;
    private LocalDate endDate;

    /** 전주 잔액. 회계 연도의 내역 중 시작일 전날까지의 수입 - 지출 */
    private long previousBalance;
    /** 해당 주간의 수입과 지출 */
    private ReportSummaryDto weekly;

    /** 최상위 예산별 현황. 회계 연도의 내역 중 종료일까지를 집행액으로 합산한다. */
    private List<BudgetExecutionDto> incomeStatus;
    private List<BudgetExecutionDto> expenseStatus;

    /** 해당 주간의 내역. 예산 관리에서 정한 예산 순서를 따르며 내역이 있는 예산만 담는다. */
    private List<ReportRecordGroupDto> incomeRecords;
    private List<ReportRecordGroupDto> expenseRecords;

    /**
     * 현재 잔액(전주 잔액 + 주간 수입 - 주간 지출)
     */
    public long getCurrentBalance() {
        return previousBalance + weekly.getDifference();
    }
}
