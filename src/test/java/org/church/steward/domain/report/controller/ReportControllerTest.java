package org.church.steward.domain.report.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportType;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.service.ReportService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportControllerTest {

    private static final Long FISCAL_YEAR_ID = 7L;
    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 4);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 10, 10);

    @Mock
    private ReportService reportService;

    @InjectMocks
    private ReportController reportController;

    @Nested
    @DisplayName("getWeeklySettlementReport 테스트")
    class GetWeeklySettlementReportTest {

        @Test
        @DisplayName("회계 연도와 주간 범위로 서비스가 집계한 주간 회계 결산 보고서 데이터를 그대로 반환한다")
        void getWeeklySettlementReport_ValidParameters_ReturnsServiceResult() {
            // Given
            WeeklySettlementReportDto expected = WeeklySettlementReportDto.builder()
                    .build();
            when(reportService.getWeeklySettlementReport(FISCAL_YEAR_ID, WEEK_START, WEEK_END)).thenReturn(expected);

            // When
            WeeklySettlementReportDto actual = reportController.getWeeklySettlementReport(FISCAL_YEAR_ID, WEEK_START,
                    WEEK_END);

            // Then
            assertThat(actual).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("getBudgetPlanReport 테스트")
    class GetBudgetPlanReportTest {

        @Test
        @DisplayName("회계 연도와 구분으로 서비스가 집계한 회계 연도 예산안 데이터를 그대로 반환한다")
        void getBudgetPlanReport_ValidParameters_ReturnsServiceResult() {
            // Given
            BudgetPlanReportDto expected = BudgetPlanReportDto.builder()
                    .build();
            when(reportService.getBudgetPlanReport(FISCAL_YEAR_ID, BudgetType.INCOME)).thenReturn(expected);

            // When
            BudgetPlanReportDto actual = reportController.getBudgetPlanReport(FISCAL_YEAR_ID, BudgetType.INCOME);

            // Then
            assertThat(actual).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("getAnnualSettlementReport 테스트")
    class GetAnnualSettlementReportTest {

        @Test
        @DisplayName("회계 연도로 서비스가 집계한 회계 연도 수입/지출 결산 보고서 데이터를 그대로 반환한다")
        void getAnnualSettlementReport_ValidYear_ReturnsServiceResult() {
            // Given
            AnnualSettlementReportDto expected = AnnualSettlementReportDto.builder()
                    .build();
            when(reportService.getAnnualSettlementReport(FISCAL_YEAR_ID)).thenReturn(expected);

            // When
            AnnualSettlementReportDto actual = reportController.getAnnualSettlementReport(FISCAL_YEAR_ID);

            // Then
            assertThat(actual).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("getSettlementDetailReport 테스트")
    class GetSettlementDetailReportTest {

        @Test
        @DisplayName("회계 연도와 구분으로 서비스가 집계한 결산 세부 내역서 데이터를 그대로 반환한다")
        void getSettlementDetailReport_ValidParameters_ReturnsServiceResult() {
            // Given
            SettlementDetailReportDto expected = SettlementDetailReportDto.builder()
                    .build();
            when(reportService.getSettlementDetailReport(FISCAL_YEAR_ID, BudgetType.EXPENSE)).thenReturn(expected);

            // When
            SettlementDetailReportDto actual = reportController.getSettlementDetailReport(FISCAL_YEAR_ID,
                    BudgetType.EXPENSE);

            // Then
            assertThat(actual).isSameAs(expected);
        }
    }

    @Nested
    @DisplayName("logOutput 테스트")
    class LogOutputTest {

        @Test
        @DisplayName("보고서 출력 기록을 요청하면 받은 값 그대로 서비스에 넘긴다")
        void logOutput_ValidParameters_CallsService() {
            // Given
            ReportType reportType = ReportType.WEEKLY_SETTLEMENT;
            ReportOutputType outputType = ReportOutputType.PRINT;

            // When
            reportController.logOutput(reportType, outputType, FISCAL_YEAR_ID, WEEK_START, WEEK_END);

            // Then
            verify(reportService).logOutput(reportType, outputType, FISCAL_YEAR_ID, WEEK_START, WEEK_END);
        }
    }
}
