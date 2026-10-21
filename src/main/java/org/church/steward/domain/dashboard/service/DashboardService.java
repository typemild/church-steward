package org.church.steward.domain.dashboard.service;

import java.util.List;

import org.church.steward.domain.dashboard.dto.BreakdownDto;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;

public interface DashboardService {
    FiscalYearSummaryDto getFiscalYearSummary(Long fiscalYearId);

    List<BudgetProgressDto> getBudgetProgress(Long fiscalYearId);

    /**
     * 회계 연도의 월별 수입/지출 합계를 달 순서대로 반환한다.
     * 회계 연도 기간의 시작월부터 종료월까지는 내역이 없어도 항상 포함한다.
     */
    List<MonthlyTrendDto> getMonthlyTrend(Long fiscalYearId);

    List<BreakdownDto> getIncomeBreakdown(Long fiscalYearId);

    List<BreakdownDto> getExpenseBreakdown(Long fiscalYearId);

    List<RecentTransactionDto> getRecentTransactions(Long fiscalYearId);
}
