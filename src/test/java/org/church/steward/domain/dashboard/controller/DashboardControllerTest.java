package org.church.steward.domain.dashboard.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.dashboard.dto.BreakdownDto;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.dashboard.service.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    private static final Long FISCAL_YEAR_ID = 7L;

    @Nested
    @DisplayName("getFiscalYearSummary 테스트")
    class GetFiscalYearSummaryTest {

        @Test
        @DisplayName("회계 연도 요약 데이터가 존재할 때, 서비스로부터 데이터를 받아 반환한다")
        void getFiscalYearSummary_데이터가존재할때_데이터를반환한다() {
            // Given
            FiscalYearSummaryDto expectedData = FiscalYearSummaryDto.builder().build();
            when(dashboardService.getFiscalYearSummary(FISCAL_YEAR_ID)).thenReturn(expectedData);

            // When
            FiscalYearSummaryDto actualData = dashboardController.getFiscalYearSummary(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("회계 연도 요약을 요청할 때, 서비스를 호출한다")
        void getFiscalYearSummary_서비스를호출한다() {
            // Given
            when(dashboardService.getFiscalYearSummary(FISCAL_YEAR_ID))
                    .thenReturn(FiscalYearSummaryDto.builder().build());

            // When
            dashboardController.getFiscalYearSummary(FISCAL_YEAR_ID);

            // Then
            verify(dashboardService).getFiscalYearSummary(FISCAL_YEAR_ID);
        }
    }

    @Nested
    @DisplayName("getBudgetProgress 테스트")
    class GetBudgetProgressTest {

        @Test
        @DisplayName("예산 진행 데이터가 존재할 때, 서비스로부터 데이터를 받아 반환한다")
        void getBudgetProgress_데이터가존재할때_데이터를반환한다() {
            // Given
            List<BudgetProgressDto> expectedData = List.of(BudgetProgressDto.builder().build());
            when(dashboardService.getBudgetProgress(FISCAL_YEAR_ID)).thenReturn(expectedData);

            // When
            List<BudgetProgressDto> actualData = dashboardController.getBudgetProgress(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("예산 진행 데이터를 요청할 때, 서비스를 호출한다")
        void getBudgetProgress_서비스를호출한다() {
            // Given
            when(dashboardService.getBudgetProgress(FISCAL_YEAR_ID)).thenReturn(List.of());

            // When
            dashboardController.getBudgetProgress(FISCAL_YEAR_ID);

            // Then
            verify(dashboardService).getBudgetProgress(FISCAL_YEAR_ID);
        }
    }

    @Nested
    @DisplayName("getMonthlyTrend 테스트")
    class GetMonthlyTrendTest {

        @Test
        @DisplayName("월별 추이 데이터가 존재할 때, 서비스로부터 데이터를 받아 반환한다")
        void getMonthlyTrend_데이터가존재할때_데이터를반환한다() {
            // Given
            List<MonthlyTrendDto> expectedData = List.of(MonthlyTrendDto.builder().build());
            when(dashboardService.getMonthlyTrend(FISCAL_YEAR_ID)).thenReturn(expectedData);

            // When
            List<MonthlyTrendDto> actualData = dashboardController.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("월별 추이 데이터를 요청할 때, 서비스를 호출한다")
        void getMonthlyTrend_서비스를호출한다() {
            // Given
            when(dashboardService.getMonthlyTrend(FISCAL_YEAR_ID)).thenReturn(List.of());

            // When
            dashboardController.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            verify(dashboardService).getMonthlyTrend(FISCAL_YEAR_ID);
        }
    }

    @Nested
    @DisplayName("getIncomeBreakdown 테스트")
    class GetIncomeBreakdownTest {

        @Test
        @DisplayName("수입 내역 데이터가 존재할 때, 서비스로부터 데이터를 받아 반환한다")
        void getIncomeBreakdown_데이터가존재할때_데이터를반환한다() {
            // Given
            List<BreakdownDto> expectedData = List.of(BreakdownDto.builder().build());
            when(dashboardService.getIncomeBreakdown(FISCAL_YEAR_ID)).thenReturn(expectedData);

            // When
            List<BreakdownDto> actualData = dashboardController.getIncomeBreakdown(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("수입 내역 데이터를 요청할 때, 서비스를 호출한다")
        void getIncomeBreakdown_서비스를호출한다() {
            // Given
            when(dashboardService.getIncomeBreakdown(FISCAL_YEAR_ID)).thenReturn(List.of());

            // When
            dashboardController.getIncomeBreakdown(FISCAL_YEAR_ID);

            // Then
            verify(dashboardService).getIncomeBreakdown(FISCAL_YEAR_ID);
        }
    }

    @Nested
    @DisplayName("getExpenseBreakdown 테스트")
    class GetExpenseBreakdownTest {

        @Test
        @DisplayName("지출 내역 데이터가 존재할 때, 서비스로부터 데이터를 받아 반환한다")
        void getExpenseBreakdown_데이터가존재할때_데이터를반환한다() {
            // Given
            List<BreakdownDto> expectedData = List.of(BreakdownDto.builder().build());
            when(dashboardService.getExpenseBreakdown(FISCAL_YEAR_ID)).thenReturn(expectedData);

            // When
            List<BreakdownDto> actualData = dashboardController.getExpenseBreakdown(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).isEqualTo(expectedData);
        }

        @Test
        @DisplayName("지출 내역 데이터를 요청할 때, 서비스를 호출한다")
        void getExpenseBreakdown_서비스를호출한다() {
            // Given
            when(dashboardService.getExpenseBreakdown(FISCAL_YEAR_ID)).thenReturn(List.of());

            // When
            dashboardController.getExpenseBreakdown(FISCAL_YEAR_ID);

            // Then
            verify(dashboardService).getExpenseBreakdown(FISCAL_YEAR_ID);
        }
    }

    @Nested
    @DisplayName("getRecentTransactions 테스트")
    class GetRecentTransactionsTest {

        @Test
        @DisplayName("최근 수입과 지출 데이터를 통합하여 날짜 역순으로 반환한다")
        void getRecentTransactions_Success() {
            // Given
            RecentTransactionDto income = RecentTransactionDto.builder()
                    .date(LocalDate.of(2024, 5, 1))
                    .category("수입예산")
                    .amount(1000L)
                    .description("수입내역")
                    .type("수입")
                    .build();
            RecentTransactionDto expense = RecentTransactionDto.builder()
                    .date(LocalDate.of(2024, 5, 2))
                    .category("지출예산")
                    .amount(500L)
                    .description("지출내역")
                    .type("지출")
                    .build();

            when(dashboardService.getRecentTransactions(FISCAL_YEAR_ID)).thenReturn(List.of(expense, income));

            // When
            List<RecentTransactionDto> actualData = dashboardController.getRecentTransactions(FISCAL_YEAR_ID);

            // Then
            assertThat(actualData).hasSize(2);
            assertThat(actualData.get(0).getType()).isEqualTo("지출");
            assertThat(actualData.get(1).getType()).isEqualTo("수입");
        }
    }

}
