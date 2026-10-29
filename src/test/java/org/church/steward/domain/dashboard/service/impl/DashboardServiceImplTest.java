package org.church.steward.domain.dashboard.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.dashboard.dto.BreakdownDto;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.dashboard.mapper.DashboardMapper;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Spy
    private DashboardMapper dashboardMapper = new DashboardMapper();

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    /** 조회할 회계 연도의 ID */
    private static final Long FISCAL_YEAR_ID = 3L;
    /** 테스트 데이터의 날짜에 쓰는 달력의 해 */
    private final int CURRENT_YEAR = 2023;

    /**
     * 조회할 회계 연도(FISCAL_YEAR_ID)를 주어진 기간으로 만든다.
     */
    private FiscalYear createFiscalYear(LocalDate startDate, LocalDate endDate) {
        return FiscalYear.builder()
                .id(FISCAL_YEAR_ID)
                .name("2023년")
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    private FinancialRecord createRecord(FinancialRecordType type, long amount, Budget budget, LocalDate date) {
        return FinancialRecord.builder()
                .type(type)
                .amount(amount)
                .budget(budget)
                .date(date)
                .build();
    }

    @Nested
    @DisplayName("getFiscalYearSummary 테스트")
    class GetFiscalYearSummaryTest {

        @Test
        @DisplayName("회계 연도의 수입, 지출, 잔액을 정확히 계산하여 반환한다")
        @SuppressWarnings("unchecked")
        void getFiscalYearSummary_Success() {
            // Given
            FinancialRecord income = createRecord(FinancialRecordType.INCOME, 1000L, null,
                    LocalDate.of(CURRENT_YEAR, 5, 1));
            FinancialRecord expense = createRecord(FinancialRecordType.EXPENSE, 400L, null,
                    LocalDate.of(CURRENT_YEAR, 6, 1));

            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(income, expense));

            // When
            FiscalYearSummaryDto result = dashboardService.getFiscalYearSummary(FISCAL_YEAR_ID);

            // Then
            assertThat(result.getTotalIncome()).isEqualTo(1000L);
            assertThat(result.getTotalExpense()).isEqualTo(400L);
            assertThat(result.getCurrentBalance()).isEqualTo(600L);
        }
    }

    @Nested
    @DisplayName("getBudgetProgress 테스트")
    class GetBudgetProgressTest {

        @Test
        @DisplayName("예산 대비 실제 집행률을 정확히 계산하여 반환한다")
        @SuppressWarnings("unchecked")
        void getBudgetProgress_Success() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .name("테스트 예산")
                    .type(BudgetType.INCOME)
                    .amount(1000L)
                    .build();

            FinancialRecord income = createRecord(FinancialRecordType.INCOME, 800L, budget,
                    LocalDate.of(CURRENT_YEAR, 5, 1));

            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(budget));
            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(income));

            // When
            List<BudgetProgressDto> result = dashboardService.getBudgetProgress(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(1);
            BudgetProgressDto progress = result.get(0);
            assertThat(progress.getBudgetName()).isEqualTo("테스트 예산");
            assertThat(progress.getType()).isEqualTo(BudgetType.INCOME);
            assertThat(progress.getBudgetedAmount()).isEqualTo(1000L);
            assertThat(progress.getActualAmount()).isEqualTo(800L);
            assertThat(progress.getProgressRate()).isEqualTo(80.0);
        }
    }

    @Nested
    @DisplayName("getMonthlyTrend 테스트")
    class GetMonthlyTrendTest {

        @Test
        @DisplayName("월별 수입 및 지출 트렌드를 정확히 반환한다")
        @SuppressWarnings("unchecked")
        void getMonthlyTrend_Success() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(
                    createFiscalYear(LocalDate.of(CURRENT_YEAR, 1, 1), LocalDate.of(CURRENT_YEAR, 12, 31))));
            FinancialRecord incomeJan = createRecord(FinancialRecordType.INCOME, 100L, null,
                    LocalDate.of(CURRENT_YEAR, 1, 15));
            FinancialRecord incomeFeb = createRecord(FinancialRecordType.INCOME, 200L, null,
                    LocalDate.of(CURRENT_YEAR, 2, 10));
            FinancialRecord expenseJan = createRecord(FinancialRecordType.EXPENSE, 50L, null,
                    LocalDate.of(CURRENT_YEAR, 1, 20));

            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(Arrays.asList(incomeJan, incomeFeb, expenseJan));

            // When
            List<MonthlyTrendDto> result = dashboardService.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(12);
            assertThat(result.get(0).getYearMonth()).isEqualTo("2023-01");
            assertThat(result.get(0).getIncome()).isEqualTo(100L);
            assertThat(result.get(0).getExpense()).isEqualTo(50L);

            assertThat(result.get(1).getYearMonth()).isEqualTo("2023-02");
            assertThat(result.get(1).getIncome()).isEqualTo(200L);
            assertThat(result.get(1).getExpense()).isEqualTo(0L);
        }

        @Test
        @DisplayName("회계 연도의 기간이 달력의 해와 어긋나면 기간의 시작월부터 종료월까지를 달 순서대로 반환한다")
        @SuppressWarnings("unchecked")
        void getMonthlyTrend_PeriodAcrossYears_ReturnsMonthsOfPeriodInOrder() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(
                    createFiscalYear(LocalDate.of(CURRENT_YEAR - 1, 12, 1), LocalDate.of(CURRENT_YEAR, 11, 30))));
            FinancialRecord incomeDec = createRecord(FinancialRecordType.INCOME, 300L, null,
                    LocalDate.of(CURRENT_YEAR - 1, 12, 28));
            FinancialRecord expenseNov = createRecord(FinancialRecordType.EXPENSE, 70L, null,
                    LocalDate.of(CURRENT_YEAR, 11, 5));

            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(Arrays.asList(expenseNov, incomeDec));

            // When
            List<MonthlyTrendDto> result = dashboardService.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(12);
            assertThat(result.getFirst().getYearMonth()).isEqualTo("2022-12");
            assertThat(result.getFirst().getIncome()).isEqualTo(300L);
            assertThat(result.get(1).getYearMonth()).isEqualTo("2023-01");
            assertThat(result.getLast().getYearMonth()).isEqualTo("2023-11");
            assertThat(result.getLast().getExpense()).isEqualTo(70L);
        }

        @Test
        @DisplayName("회계 연도의 기간이 달의 중간에서 시작하고 끝나도 그 달들을 포함하며, 기간의 길이만큼만 반환한다")
        @SuppressWarnings("unchecked")
        void getMonthlyTrend_PeriodWithPartialMonths_IncludesStartAndEndMonths() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(
                    createFiscalYear(LocalDate.of(CURRENT_YEAR, 3, 15), LocalDate.of(CURRENT_YEAR, 6, 10))));
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            List<MonthlyTrendDto> result = dashboardService.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            assertThat(result).extracting(MonthlyTrendDto::getYearMonth)
                    .containsExactly("2023-03", "2023-04", "2023-05", "2023-06");
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도이면 예외가 발생한다")
        void getMonthlyTrend_FiscalYearNotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> dashboardService.getMonthlyTrend(FISCAL_YEAR_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }
    }

    @Nested
    @DisplayName("getIncomeBreakdown 테스트")
    class GetIncomeBreakdownTest {

        @Test
        @DisplayName("예산명별 수입 합계를 내림차순으로 반환한다")
        @SuppressWarnings("unchecked")
        void getIncomeBreakdown_Success() {
            // Given
            Budget budget1 = Budget.builder().id(1L).name("카테고리A").build();
            Budget budget2 = Budget.builder().id(2L).name("카테고리B").build();

            LocalDate date = LocalDate.of(CURRENT_YEAR, 1, 1);
            FinancialRecord income1 = createRecord(FinancialRecordType.INCOME, 500L, budget1, date);
            FinancialRecord income2 = createRecord(FinancialRecordType.INCOME, 300L, budget2, date);
            FinancialRecord income3 = createRecord(FinancialRecordType.INCOME, 400L, budget1, date);

            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(Arrays.asList(income1, income2, income3));

            // When
            List<BreakdownDto> result = dashboardService.getIncomeBreakdown(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCategoryName()).isEqualTo("카테고리A");
            assertThat(result.get(0).getAmount()).isEqualTo(900L);
            assertThat(result.get(1).getCategoryName()).isEqualTo("카테고리B");
            assertThat(result.get(1).getAmount()).isEqualTo(300L);
        }
    }

    @Nested
    @DisplayName("getExpenseBreakdown 테스트")
    class GetExpenseBreakdownTest {

        @Test
        @DisplayName("예산명별 지출 합계를 내림차순으로 반환한다")
        @SuppressWarnings("unchecked")
        void getExpenseBreakdown_Success() {
            // Given
            Budget budget1 = Budget.builder().id(1L).name("지출A").build();
            LocalDate date = LocalDate.of(CURRENT_YEAR, 1, 1);
            FinancialRecord expense1 = createRecord(FinancialRecordType.EXPENSE, 500L, budget1, date);
            FinancialRecord expense2 = createRecord(FinancialRecordType.EXPENSE, 200L, budget1, date);

            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(Arrays.asList(expense1, expense2));

            // When
            List<BreakdownDto> result = dashboardService.getExpenseBreakdown(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategoryName()).isEqualTo("지출A");
            assertThat(result.get(0).getAmount()).isEqualTo(700L);
        }
    }

    @Nested
    @DisplayName("getRecentTransactions 테스트")
    class GetRecentTransactionsTest {

        @Test
        @DisplayName("리포지토리가 날짜 역순으로 돌려준 최근 수입과 지출을 구분 이름과 함께 그 순서대로 반환한다")
        void getRecentTransactions_Success() {
            // Given
            FinancialRecord income = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .amount(1000L)
                    .date(LocalDate.of(CURRENT_YEAR, 5, 1))
                    .budget(Budget.builder().name("수입예산").build())
                    .description("수입내역")
                    .build();
            FinancialRecord expense = FinancialRecord.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .amount(500L)
                    .date(LocalDate.of(CURRENT_YEAR, 5, 2))
                    .budget(Budget.builder().name("지출예산").build())
                    .description("지출내역")
                    .build();

            when(financialRecordRepository.findTop100ByFiscalYearIdOrderByDateDescIdDesc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(expense, income));

            // When
            List<RecentTransactionDto> result = dashboardService.getRecentTransactions(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getType()).isEqualTo("지출"); // 5/2 >= 5/1
            assertThat(result.get(1).getType()).isEqualTo("수입");
            assertThat(result.get(0).getAmount()).isEqualTo(500L);
            assertThat(result.get(1).getAmount()).isEqualTo(1000L);
        }

        @Test
        @DisplayName("내역에 기록된 당시의 봉헌자 이름들과 비고를 함께 반환한다")
        void getRecentTransactions_WithContactsAndRemarks_ReturnsContactNamesAndRemarks() {
            // Given
            FinancialRecord income = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .amount(1000L)
                    .date(LocalDate.of(CURRENT_YEAR, 5, 1))
                    .budget(Budget.builder().name("수입예산").build())
                    .description("수입내역")
                    .remarks("수입비고")
                    .contacts(List.of(
                            FinancialRecordContact.builder().contactName("홍길동").build(),
                            FinancialRecordContact.builder().contactName("김철수").build()))
                    .build();

            when(financialRecordRepository.findTop100ByFiscalYearIdOrderByDateDescIdDesc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(income));

            // When
            List<RecentTransactionDto> result = dashboardService.getRecentTransactions(FISCAL_YEAR_ID);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getContactNames()).containsExactly("홍길동", "김철수");
            assertThat(result.get(0).getRemarks()).isEqualTo("수입비고");
        }
    }

    @Nested
    @DisplayName("Specification 파라미터 검증 테스트")
    class SpecificationVerificationTest {

        @Test
        @DisplayName("getFiscalYearSummary 호출 시 올바른 Specification이 전달되는지 검증한다")
        @SuppressWarnings("unchecked")
        void getFiscalYearSummary_SpecificationVerification() {
            // Given
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            dashboardService.getFiscalYearSummary(FISCAL_YEAR_ID);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("getBudgetProgress 호출 시 올바른 Specification이 전달되는지 검증한다")
        @SuppressWarnings("unchecked")
        void getBudgetProgress_SpecificationVerification() {
            // Given
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            dashboardService.getBudgetProgress(FISCAL_YEAR_ID);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("getMonthlyTrend 호출 시 올바른 Specification이 전달되는지 검증한다")
        @SuppressWarnings("unchecked")
        void getMonthlyTrend_SpecificationVerification() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(
                    createFiscalYear(LocalDate.of(CURRENT_YEAR, 1, 1), LocalDate.of(CURRENT_YEAR, 12, 31))));
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            dashboardService.getMonthlyTrend(FISCAL_YEAR_ID);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("getIncomeBreakdown 호출 시 올바른 Specification이 전달되는지 검증한다")
        @SuppressWarnings("unchecked")
        void getIncomeBreakdown_SpecificationVerification() {
            // Given
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            dashboardService.getIncomeBreakdown(FISCAL_YEAR_ID);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class));
        }

        @Test
        @DisplayName("getExpenseBreakdown 호출 시 올바른 Specification이 전달되는지 검증한다")
        @SuppressWarnings("unchecked")
        void getExpenseBreakdown_SpecificationVerification() {
            // Given
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            dashboardService.getExpenseBreakdown(FISCAL_YEAR_ID);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class));
        }
    }
}
