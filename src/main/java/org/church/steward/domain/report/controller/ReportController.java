package org.church.steward.domain.report.controller;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportType;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.service.ReportService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    public WeeklySettlementReportDto getWeeklySettlementReport(Long fiscalYearId, LocalDate startDate,
            LocalDate endDate) {
        return reportService.getWeeklySettlementReport(fiscalYearId, startDate, endDate);
    }

    public BudgetPlanReportDto getBudgetPlanReport(Long fiscalYearId, BudgetType type) {
        return reportService.getBudgetPlanReport(fiscalYearId, type);
    }

    public AnnualSettlementReportDto getAnnualSettlementReport(Long fiscalYearId) {
        return reportService.getAnnualSettlementReport(fiscalYearId);
    }

    public SettlementDetailReportDto getSettlementDetailReport(Long fiscalYearId, BudgetType type) {
        return reportService.getSettlementDetailReport(fiscalYearId, type);
    }

    public void logOutput(ReportType reportType, ReportOutputType outputType, Long fiscalYearId, LocalDate startDate,
            LocalDate endDate) {
        reportService.logOutput(reportType, outputType, fiscalYearId, startDate, endDate);
    }
}
