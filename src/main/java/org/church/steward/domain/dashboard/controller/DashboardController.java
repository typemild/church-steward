package org.church.steward.domain.dashboard.controller;

import java.util.List;

import org.church.steward.domain.dashboard.dto.BreakdownDto;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.dashboard.service.DashboardService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    public FiscalYearSummaryDto getFiscalYearSummary(Long fiscalYearId) {
        return dashboardService.getFiscalYearSummary(fiscalYearId);
    }

    public List<BudgetProgressDto> getBudgetProgress(Long fiscalYearId) {
        return dashboardService.getBudgetProgress(fiscalYearId);
    }

    public List<MonthlyTrendDto> getMonthlyTrend(Long fiscalYearId) {
        return dashboardService.getMonthlyTrend(fiscalYearId);
    }

    public List<BreakdownDto> getIncomeBreakdown(Long fiscalYearId) {
        return dashboardService.getIncomeBreakdown(fiscalYearId);
    }

    public List<BreakdownDto> getExpenseBreakdown(Long fiscalYearId) {
        return dashboardService.getExpenseBreakdown(fiscalYearId);
    }

    public List<RecentTransactionDto> getRecentTransactions(Long fiscalYearId) {
        return dashboardService.getRecentTransactions(fiscalYearId);
    }
}
