package org.church.steward.domain.report.dto;

import java.util.List;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 연도 수입/지출 결산 보고서의 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnnualSettlementReportDto {
    private String fiscalYearName;

    /** 직전 회계 연도의 이름. 직전 회계 연도가 없으면 null */
    private String previousFiscalYearName;
    /** 직전 회계 연도의 수입/지출 총액. 직전 회계 연도가 없으면 null */
    private ReportSummaryDto previousYear;
    private ReportSummaryDto currentYear;

    /** 월별 수입/지출. 대시보드의 월별 차트와 같은 데이터다. */
    private List<MonthlyTrendDto> monthlyTrend;

    /** 최상위 예산별 현황 */
    private List<BudgetExecutionDto> incomeStatus;
    private List<BudgetExecutionDto> expenseStatus;
}
