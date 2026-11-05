package org.church.steward.domain.report.controller;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.service.ReportService;
import org.church.steward.domain.report.service.ReportSettingService;
import org.church.steward.domain.report.service.ReportTemplateService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.UserSessionHolder;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;
    private final ReportSettingService reportSettingService;
    private final ReportTemplateService reportTemplateService;
    private final UserSessionHolder userSessionHolder;

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

    /**
     * 로그인한 사용자가 보고서에 대해 정한 설정을 조회한다. 정한 적이 없으면 기본 설정이다.
     */
    public ReportSettingDto getReportSetting(ReportType reportType) {
        return reportSettingService.getReportSetting(getLoggedInUserId(), reportType);
    }

    /**
     * 로그인한 사용자가 보고서에 대해 정한 설정을 저장한다.
     */
    public void updateReportSetting(ReportSettingDto setting) {
        reportSettingService.updateReportSetting(getLoggedInUserId(), setting);
    }

    /**
     * 보고서의 양식을 조회한다. 프로그램 전체 설정이므로 사용자와 무관하다.
     */
    public ReportTemplateDto getReportTemplate(ReportType reportType) {
        return reportTemplateService.getReportTemplate(reportType);
    }

    /**
     * 보고서의 양식을 저장한다. 관리자뿐 아니라 모든 사용자가 바꿀 수 있으므로 권한을 확인하지 않는다.
     */
    public void updateReportTemplate(ReportTemplateDto template) {
        reportTemplateService.updateReportTemplate(template);
    }

    private String getLoggedInUserId() {
        return userSessionHolder.getLoggedInUser()
                .id();
    }
}
