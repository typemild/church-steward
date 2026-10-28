package org.church.steward.domain.report.service;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportType;

/**
 * 보고서에 실을 데이터를 집계하는 서비스.
 * 모든 집계는 회계 연도에 속한 내역을 대상으로 한다. 내역의 날짜는 입력할 때 회계 연도의 기간 안으로 제한된다.
 * 직전 회계 연도는 시작일이 해당 회계 연도보다 앞선 회계 연도 중 가장 최근 것이다.
 */
public interface ReportService {

    /**
     * 주간 회계 결산 보고서의 데이터를 집계한다.
     *
     * @param fiscalYearId 회계 연도의 ID
     * @param startDate    주간의 시작일
     * @param endDate      주간의 종료일
     */
    WeeklySettlementReportDto getWeeklySettlementReport(Long fiscalYearId, LocalDate startDate, LocalDate endDate);

    /**
     * 회계 연도 수입 예산안 또는 회계 연도 지출 예산안의 데이터를 집계한다.
     * 직전 회계 연도 대비 증감액은 직전 회계 연도에서 전체 경로가 같은 예산을 찾아 계산한다.
     */
    BudgetPlanReportDto getBudgetPlanReport(Long fiscalYearId, BudgetType type);

    /**
     * 회계 연도 수입/지출 결산 보고서의 데이터를 집계한다.
     */
    AnnualSettlementReportDto getAnnualSettlementReport(Long fiscalYearId);

    /**
     * 회계 연도 수입 결산 세부 내역서 또는 회계 연도 지출 결산 세부 내역서의 데이터를 집계한다.
     */
    SettlementDetailReportDto getSettlementDetailReport(Long fiscalYearId, BudgetType type);

    /**
     * 보고서를 인쇄하거나 PDF로 저장한 사실을 활동 기록에 남긴다.
     *
     * @param startDate 주간 보고서의 시작일. 그 밖의 보고서에서는 쓰지 않는다.
     * @param endDate   주간 보고서의 종료일. 그 밖의 보고서에서는 쓰지 않는다.
     */
    void logOutput(ReportType reportType, ReportOutputType outputType, Long fiscalYearId, LocalDate startDate,
            LocalDate endDate);
}
