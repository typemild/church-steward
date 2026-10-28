package org.church.steward.domain.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.service.DashboardService;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.mapper.ReportMapper;
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
class ReportServiceImplTest {

    private static final Long CURRENT_ID = 6L;
    private static final Long PREVIOUS_ID = 5L;
    private static final LocalDate WEEK_START = LocalDate.of(2026, 10, 4);
    private static final LocalDate WEEK_END = LocalDate.of(2026, 10, 10);

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private ActivityLogService activityLogService;

    /** 집계 결과가 DTO에 제대로 담기는지까지 확인하기 위해 실제 매퍼를 쓴다. */
    @Spy
    private ReportMapper reportMapper = new ReportMapper();

    @InjectMocks
    private ReportServiceImpl reportService;

    /** 보고서를 조회할 회계 연도 */
    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(CURRENT_ID)
            .name("2026년")
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 12, 31))
            .build();

    /** 직전 회계 연도. 한 해에 회계 연도가 둘 이상일 수 있으므로 연도가 아니라 시작일로 찾는다. */
    private final FiscalYear previousFiscalYear = FiscalYear.builder()
            .id(PREVIOUS_ID)
            .name("2025년 하반기")
            .startDate(LocalDate.of(2025, 7, 1))
            .endDate(LocalDate.of(2025, 12, 31))
            .build();

    /**
     * 보고서를 조회할 회계 연도를 찾을 수 있게 한다.
     */
    private void givenFiscalYear() {
        when(fiscalYearRepository.findById(CURRENT_ID)).thenReturn(Optional.of(fiscalYear));
    }

    /**
     * 보고서를 조회할 회계 연도와 그 직전 회계 연도를 찾을 수 있게 한다.
     * 직전 회계 연도를 따로 두지 않으면 리포지토리가 빈 Optional을 돌려주어 직전 회계 연도가 없는 것이 된다.
     */
    private void givenPreviousFiscalYear() {
        givenFiscalYear();
        when(fiscalYearRepository.findFirstByStartDateLessThanOrderByStartDateDesc(fiscalYear.getStartDate()))
                .thenReturn(Optional.of(previousFiscalYear));
    }

    private Budget budget(Long id, String name, BudgetType type, Budget parent, Long amount, int sortOrder) {
        return Budget.builder()
                .id(id)
                .fiscalYear(fiscalYear)
                .name(name)
                .type(type)
                .parent(parent)
                .amount(amount)
                .sortOrder(sortOrder)
                .build();
    }

    private FinancialRecord record(Long id, FinancialRecordType type, long amount, Budget budget, LocalDate date) {
        return FinancialRecord.builder()
                .id(id)
                .type(type)
                .amount(amount)
                .budget(budget)
                .date(date)
                .build();
    }

    /**
     * 보고서를 조회할 회계 연도의 수입 예산과 지출 예산을 찾을 수 있게 한다.
     */
    private void stubBudgets(List<Budget> incomeBudgets, List<Budget> expenseBudgets) {
        when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.INCOME))
                .thenReturn(incomeBudgets);
        when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.EXPENSE))
                .thenReturn(expenseBudgets);
    }

    @SuppressWarnings("unchecked")
    private void stubRecords(List<FinancialRecord> records) {
        when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(records);
    }

    @Nested
    @DisplayName("getWeeklySettlementReport 테스트")
    class GetWeeklySettlementReportTest {

        @Test
        @DisplayName("시작일 전의 내역은 전주 잔액으로, 시작일부터의 내역은 주간 수입과 지출로 합산하고 현재 잔액을 계산한다")
        void getWeeklySettlementReport_RecordsBeforeAndWithinWeek_SplitsIntoPreviousBalanceAndWeekly() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);
            Budget office = budget(2L, "사무비", BudgetType.EXPENSE, null, 500L, 0);
            givenFiscalYear();
            stubBudgets(List.of(offering), List.of(office));
            stubRecords(List.of(
                    record(1L, FinancialRecordType.INCOME, 700L, offering, WEEK_START.minusDays(1)),
                    record(2L, FinancialRecordType.EXPENSE, 200L, office, LocalDate.of(2026, 1, 5)),
                    record(3L, FinancialRecordType.INCOME, 300L, offering, WEEK_START),
                    record(4L, FinancialRecordType.EXPENSE, 50L, office, WEEK_END)));

            // When
            WeeklySettlementReportDto result = reportService.getWeeklySettlementReport(CURRENT_ID, WEEK_START,
                    WEEK_END);

            // Then
            assertThat(result.getPreviousBalance()).isEqualTo(500L);
            assertThat(result.getWeekly()
                    .getIncome()).isEqualTo(300L);
            assertThat(result.getWeekly()
                    .getExpense()).isEqualTo(50L);
            assertThat(result.getCurrentBalance()).isEqualTo(750L);
        }

        @Test
        @DisplayName("현황은 최상위 예산별로 하위 예산의 예산액과 종료일까지의 집행액을 합산한다")
        void getWeeklySettlementReport_BudgetWithChildren_SumsChildrenIntoTopLevelStatus() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, null, 0);
            Budget tithe = budget(2L, "십일조", BudgetType.INCOME, offering, 600L, 0);
            Budget thanks = budget(3L, "감사헌금", BudgetType.INCOME, offering, 400L, 1);
            givenFiscalYear();
            stubBudgets(List.of(thanks, offering, tithe), List.of());
            stubRecords(List.of(
                    record(1L, FinancialRecordType.INCOME, 150L, tithe, LocalDate.of(2026, 3, 1)),
                    record(2L, FinancialRecordType.INCOME, 100L, thanks, WEEK_START)));

            // When
            WeeklySettlementReportDto result = reportService.getWeeklySettlementReport(CURRENT_ID, WEEK_START,
                    WEEK_END);

            // Then
            assertThat(result.getIncomeStatus())
                    .extracting(BudgetExecutionDto::getName, BudgetExecutionDto::getBudgetedAmount,
                            BudgetExecutionDto::getActualAmount, BudgetExecutionDto::getExecutionRate)
                    .containsExactly(tuple("헌금", 1000L, 250L, 25.0));
        }

        @Test
        @DisplayName("주간 내역은 예산 관리의 예산 순서대로 전체 예산명과 함께 묶고, 내역이 없는 예산은 싣지 않는다")
        void getWeeklySettlementReport_WeeklyRecords_GroupsByBudgetInDisplayOrder() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, null, 0);
            Budget tithe = budget(2L, "십일조", BudgetType.INCOME, offering, 600L, 0);
            Budget thanks = budget(3L, "감사헌금", BudgetType.INCOME, offering, 400L, 1);
            Budget interest = budget(4L, "이자", BudgetType.INCOME, null, 10L, 1);
            givenFiscalYear();
            stubBudgets(List.of(interest, thanks, offering, tithe), List.of());
            stubRecords(List.of(
                    record(1L, FinancialRecordType.INCOME, 100L, thanks, WEEK_START),
                    record(2L, FinancialRecordType.INCOME, 150L, tithe, WEEK_START),
                    record(3L, FinancialRecordType.INCOME, 50L, tithe, WEEK_START)));

            // When
            WeeklySettlementReportDto result = reportService.getWeeklySettlementReport(CURRENT_ID, WEEK_START,
                    WEEK_END);

            // Then
            assertThat(result.getIncomeRecords())
                    .extracting(ReportRecordGroupDto::getBudgetName, ReportRecordGroupDto::getTotalAmount)
                    .containsExactly(tuple("헌금 > 십일조", 200L), tuple("헌금 > 감사헌금", 100L));
        }

        @Test
        @DisplayName("한 예산 안의 내역은 날짜순으로 정렬하고 기록된 당시의 관계자 이름을 담는다")
        void getWeeklySettlementReport_RecordsInSameBudget_SortsByDateWithContactNames() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);
            FinancialRecord later = FinancialRecord.builder()
                    .id(1L)
                    .type(FinancialRecordType.INCOME)
                    .amount(100L)
                    .budget(offering)
                    .date(WEEK_END)
                    .contacts(List.of(FinancialRecordContact.builder()
                            .contactName("홍길동")
                            .build()))
                    .build();
            FinancialRecord earlier = record(2L, FinancialRecordType.INCOME, 200L, offering, WEEK_START);
            givenFiscalYear();
            stubBudgets(List.of(offering), List.of());
            stubRecords(List.of(later, earlier));

            // When
            WeeklySettlementReportDto result = reportService.getWeeklySettlementReport(CURRENT_ID, WEEK_START,
                    WEEK_END);

            // Then
            List<ReportRecordDto> records = result.getIncomeRecords()
                    .getFirst()
                    .getRecords();
            assertThat(records).extracting(ReportRecordDto::getDate, ReportRecordDto::getContactNames)
                    .containsExactly(tuple(WEEK_START, List.of()), tuple(WEEK_END, List.of("홍길동")));
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외를 던진다")
        void getWeeklySettlementReport_StartAfterEnd_ThrowsException() {
            // Given
            LocalDate startDate = WEEK_END;
            LocalDate endDate = WEEK_START;

            // When & Then
            assertThatThrownBy(() -> reportService.getWeeklySettlementReport(CURRENT_ID, startDate, endDate))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보고서 기간의 시작일이 종료일보다 늦습니다.");
        }

        @Test
        @DisplayName("기간을 지정하지 않으면 예외를 던진다")
        void getWeeklySettlementReport_NullPeriod_ThrowsException() {
            // Given
            LocalDate startDate = null;

            // When & Then
            assertThatThrownBy(() -> reportService.getWeeklySettlementReport(CURRENT_ID, startDate, WEEK_END))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보고서의 기간을 지정해주세요.");
        }
    }

    @Nested
    @DisplayName("getBudgetPlanReport 테스트")
    class GetBudgetPlanReportTest {

        @Test
        @DisplayName("직전 회계 연도에서 전체 경로가 같은 예산의 금액을 찾아 담고, 이름만 같고 경로가 다른 예산은 신규로 본다")
        void getBudgetPlanReport_PreviousYearHasSamePath_MatchesByFullPath() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, null, 0);
            Budget tithe = budget(2L, "십일조", BudgetType.INCOME, offering, 700L, 0);
            Budget etc = budget(3L, "기타", BudgetType.INCOME, offering, 300L, 1);

            Budget previousOffering = budget(11L, "헌금", BudgetType.INCOME, null, null, 0);
            Budget previousTithe = budget(12L, "십일조", BudgetType.INCOME, previousOffering, 500L, 0);
            // 이름은 "기타"로 같지만 최상위 예산이므로 "헌금 > 기타"와는 다른 예산이다.
            Budget previousEtc = budget(13L, "기타", BudgetType.INCOME, null, 90L, 1);

            givenPreviousFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.INCOME))
                    .thenReturn(List.of(offering, tithe, etc));
            when(budgetRepository.findAllByFiscalYearIdAndType(PREVIOUS_ID, BudgetType.INCOME))
                    .thenReturn(List.of(previousOffering, previousTithe, previousEtc));

            // When
            BudgetPlanReportDto result = reportService.getBudgetPlanReport(CURRENT_ID, BudgetType.INCOME);

            // Then
            assertThat(result.getRows())
                    .extracting(BudgetPlanRowDto::getName, BudgetPlanRowDto::getDepth, BudgetPlanRowDto::getAmount,
                            BudgetPlanRowDto::getChange)
                    .containsExactly(
                            tuple("헌금", 0, 1000L, 500L),
                            tuple("십일조", 1, 700L, 200L),
                            tuple("기타", 1, 300L, null));
        }

        @Test
        @DisplayName("직전 회계 연도에만 있는 예산의 금액을 합산하되, 상위 예산째 없어진 경우에는 상위 예산의 금액만 센다")
        void getBudgetPlanReport_BudgetsOnlyInPreviousYear_SumsTopmostRemovedBudgets() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);

            Budget previousOffering = budget(11L, "헌금", BudgetType.INCOME, null, null, 0);
            Budget previousTithe = budget(12L, "십일조", BudgetType.INCOME, previousOffering, 500L, 0);
            Budget previousBazaar = budget(13L, "바자회", BudgetType.INCOME, null, null, 1);
            Budget previousBazaarFood = budget(14L, "음식", BudgetType.INCOME, previousBazaar, 40L, 0);
            Budget previousBazaarGoods = budget(15L, "물품", BudgetType.INCOME, previousBazaar, 60L, 1);

            givenPreviousFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.INCOME))
                    .thenReturn(List.of(offering));
            when(budgetRepository.findAllByFiscalYearIdAndType(PREVIOUS_ID, BudgetType.INCOME))
                    .thenReturn(List.of(previousOffering, previousTithe, previousBazaar, previousBazaarFood,
                            previousBazaarGoods));

            // When
            BudgetPlanReportDto result = reportService.getBudgetPlanReport(CURRENT_ID, BudgetType.INCOME);

            // Then
            // 헌금 > 십일조(500) + 바자회(100). 바자회의 하위 예산은 따로 세지 않는다.
            assertThat(result.getRemovedAmount()).isEqualTo(600L);
            assertThat(result.getPreviousTotalAmount()).isEqualTo(600L);
            assertThat(result.getTotalChange()).isEqualTo(400L);
        }

        @Test
        @DisplayName("직전 회계 연도가 없으면 모든 예산이 신규가 되도록 직전 회계 연도 금액을 모두 비운다")
        void getBudgetPlanReport_NoPreviousYear_LeavesPreviousAmountsEmpty() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);
            givenFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.INCOME))
                    .thenReturn(List.of(offering));

            // When
            BudgetPlanReportDto result = reportService.getBudgetPlanReport(CURRENT_ID, BudgetType.INCOME);

            // Then
            assertThat(result.getPreviousTotalAmount()).isNull();
            assertThat(result.getTotalChange()).isNull();
            assertThat(result.getRemovedAmount()).isNull();
            assertThat(result.getRows()
                    .getFirst()
                    .getPreviousAmount()).isNull();
        }

        @Test
        @DisplayName("점유율은 같은 구분의 예산 총액에서 각 예산이 차지하는 비율이다")
        void getBudgetPlanReport_MultipleBudgets_CalculatesShareOfTotal() {
            // Given
            Budget salary = budget(1L, "사례비", BudgetType.EXPENSE, null, 750L, 0);
            Budget office = budget(2L, "사무비", BudgetType.EXPENSE, null, 250L, 1);
            givenFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.EXPENSE))
                    .thenReturn(List.of(office, salary));

            // When
            BudgetPlanReportDto result = reportService.getBudgetPlanReport(CURRENT_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result.getTotalAmount()).isEqualTo(1000L);
            assertThat(result.getRows()).extracting(BudgetPlanRowDto::getName, BudgetPlanRowDto::getShareRate)
                    .containsExactly(tuple("사례비", 75.0), tuple("사무비", 25.0));
        }

        @Test
        @DisplayName("예산 총액이 0이면 점유율을 정할 수 없으므로 비워 둔다")
        void getBudgetPlanReport_ZeroTotal_LeavesShareRateEmpty() {
            // Given
            Budget salary = budget(1L, "사례비", BudgetType.EXPENSE, null, 0L, 0);
            givenFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.EXPENSE))
                    .thenReturn(List.of(salary));

            // When
            BudgetPlanReportDto result = reportService.getBudgetPlanReport(CURRENT_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result.getRows()
                    .getFirst()
                    .getShareRate()).isNull();
        }
    }

    @Nested
    @DisplayName("getAnnualSettlementReport 테스트")
    class GetAnnualSettlementReportTest {

        @Test
        @DisplayName("해당 회계 연도와 직전 회계 연도의 수입 총액, 지출 총액, 차액을 각각 계산한다")
        @SuppressWarnings("unchecked")
        void getAnnualSettlementReport_PreviousYearExists_SummarizesBothYears() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);
            Budget office = budget(2L, "사무비", BudgetType.EXPENSE, null, 500L, 0);
            givenPreviousFiscalYear();
            stubBudgets(List.of(offering), List.of(office));
            when(financialRecordRepository.findAll(any(Specification.class)))
                    .thenReturn(List.of(
                            record(1L, FinancialRecordType.INCOME, 900L, offering, LocalDate.of(2026, 2, 1)),
                            // 집계는 날짜가 아니라 내역이 속한 회계 연도를 기준으로 한다.
                            record(2L, FinancialRecordType.EXPENSE, 400L, office, LocalDate.of(2027, 1, 3))))
                    .thenReturn(List.of(
                            record(3L, FinancialRecordType.INCOME, 80L, offering, LocalDate.of(2025, 2, 1)),
                            record(4L, FinancialRecordType.EXPENSE, 30L, office, LocalDate.of(2025, 3, 1))));

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getCurrentYear()
                    .getIncome()).isEqualTo(900L);
            assertThat(result.getCurrentYear()
                    .getExpense()).isEqualTo(400L);
            assertThat(result.getCurrentYear()
                    .getDifference()).isEqualTo(500L);
            assertThat(result.getPreviousYear()
                    .getDifference()).isEqualTo(50L);
        }

        @Test
        @DisplayName("직전 회계 연도가 없으면 직전 회계 연도 총액을 비워 둔다")
        void getAnnualSettlementReport_NoPreviousYear_LeavesPreviousYearEmpty() {
            // Given
            givenFiscalYear();
            stubBudgets(List.of(), List.of());
            stubRecords(List.of());

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getPreviousYear()).isNull();
        }

        @Test
        @DisplayName("해당 회계 연도와 직전 회계 연도의 이름을 담는다")
        @SuppressWarnings("unchecked")
        void getAnnualSettlementReport_PreviousYearExists_ContainsFiscalYearNames() {
            // Given
            givenPreviousFiscalYear();
            stubBudgets(List.of(), List.of());
            when(financialRecordRepository.findAll(any(Specification.class))).thenReturn(List.of());

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getFiscalYearName()).isEqualTo("2026년");
            assertThat(result.getPreviousFiscalYearName()).isEqualTo("2025년 하반기");
        }

        @Test
        @DisplayName("직전 회계 연도가 없으면 직전 회계 연도의 이름을 비워 둔다")
        void getAnnualSettlementReport_NoPreviousYear_LeavesPreviousFiscalYearNameEmpty() {
            // Given
            givenFiscalYear();
            stubBudgets(List.of(), List.of());
            stubRecords(List.of());

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getPreviousFiscalYearName()).isNull();
        }

        @Test
        @DisplayName("직전 회계 연도는 해당 회계 연도의 시작일보다 먼저 시작한 회계 연도 중에서 찾는다")
        void getAnnualSettlementReport_FindsPreviousFiscalYearByStartDate() {
            // Given
            givenFiscalYear();
            stubBudgets(List.of(), List.of());
            stubRecords(List.of());

            // When
            reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            verify(fiscalYearRepository).findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate.of(2026, 1, 1));
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도이면 예외를 던진다")
        void getAnnualSettlementReport_FiscalYearNotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(CURRENT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> reportService.getAnnualSettlementReport(CURRENT_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }

        @Test
        @DisplayName("월별 수입/지출은 대시보드와 같은 데이터를 담는다")
        void getAnnualSettlementReport_MonthlyTrend_UsesDashboardData() {
            // Given
            List<MonthlyTrendDto> trend = List.of(MonthlyTrendDto.builder()
                    .yearMonth("2026-01")
                    .income(10L)
                    .build());
            givenFiscalYear();
            stubBudgets(List.of(), List.of());
            stubRecords(List.of());
            when(dashboardService.getMonthlyTrend(CURRENT_ID)).thenReturn(trend);

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getMonthlyTrend()).isEqualTo(trend);
        }

        @Test
        @DisplayName("현황은 구분별로 최상위 예산의 예산액과 집행액을 담는다")
        void getAnnualSettlementReport_Budgets_BuildsStatusPerType() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 1000L, 0);
            Budget office = budget(2L, "사무비", BudgetType.EXPENSE, null, 500L, 0);
            givenFiscalYear();
            stubBudgets(List.of(offering), List.of(office));
            stubRecords(List.of(
                    record(1L, FinancialRecordType.INCOME, 900L, offering, LocalDate.of(2026, 2, 1)),
                    record(2L, FinancialRecordType.EXPENSE, 600L, office, LocalDate.of(2026, 3, 1))));

            // When
            AnnualSettlementReportDto result = reportService.getAnnualSettlementReport(CURRENT_ID);

            // Then
            assertThat(result.getIncomeStatus())
                    .extracting(BudgetExecutionDto::getName, BudgetExecutionDto::getActualAmount,
                            BudgetExecutionDto::getExecutionRate)
                    .containsExactly(tuple("헌금", 900L, 90.0));
            assertThat(result.getExpenseStatus())
                    .extracting(BudgetExecutionDto::getName, BudgetExecutionDto::getActualAmount,
                            BudgetExecutionDto::getExecutionRate)
                    .containsExactly(tuple("사무비", 600L, 120.0));
        }
    }

    @Nested
    @DisplayName("getSettlementDetailReport 테스트")
    class GetSettlementDetailReportTest {

        @Test
        @DisplayName("예산 계층 전체를 표시 순서대로 나열하고 상위 예산에는 하위 예산의 예산액과 집행액을 합산한다")
        void getSettlementDetailReport_BudgetHierarchy_ListsAllBudgetsWithSubtotals() {
            // Given
            Budget ministry = budget(1L, "사역비", BudgetType.EXPENSE, null, null, 0);
            Budget education = budget(2L, "교육부", BudgetType.EXPENSE, ministry, 300L, 1);
            Budget mission = budget(3L, "선교부", BudgetType.EXPENSE, ministry, 100L, 0);
            Budget office = budget(4L, "사무비", BudgetType.EXPENSE, null, 100L, 1);
            givenFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.EXPENSE))
                    .thenReturn(List.of(office, education, ministry, mission));
            stubRecords(List.of(
                    record(1L, FinancialRecordType.EXPENSE, 150L, education, LocalDate.of(2026, 2, 1)),
                    record(2L, FinancialRecordType.EXPENSE, 50L, mission, LocalDate.of(2026, 3, 1)),
                    // 구분이 다른 내역은 집행액에 들어가지 않는다.
                    record(3L, FinancialRecordType.INCOME, 999L, office, LocalDate.of(2026, 3, 1))));

            // When
            SettlementDetailReportDto result = reportService.getSettlementDetailReport(CURRENT_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result.getRows())
                    .extracting(BudgetExecutionDto::getName, BudgetExecutionDto::getDepth,
                            BudgetExecutionDto::getBudgetedAmount, BudgetExecutionDto::getActualAmount,
                            BudgetExecutionDto::getShareRate, BudgetExecutionDto::getExecutionRate)
                    .containsExactly(
                            tuple("사역비", 0, 400L, 200L, 80.0, 50.0),
                            tuple("선교부", 1, 100L, 50L, 20.0, 50.0),
                            tuple("교육부", 1, 300L, 150L, 60.0, 50.0),
                            tuple("사무비", 0, 100L, 0L, 20.0, 0.0));
        }

        @Test
        @DisplayName("예산 총액과 집행 총액, 그 비율을 계산한다")
        void getSettlementDetailReport_Records_CalculatesTotals() {
            // Given
            Budget offering = budget(1L, "헌금", BudgetType.INCOME, null, 800L, 0);
            givenFiscalYear();
            when(budgetRepository.findAllByFiscalYearIdAndType(CURRENT_ID, BudgetType.INCOME))
                    .thenReturn(List.of(offering));
            stubRecords(List.of(
                    record(1L, FinancialRecordType.INCOME, 150L, offering, LocalDate.of(2026, 2, 1)),
                    record(2L, FinancialRecordType.INCOME, 50L, offering, LocalDate.of(2026, 3, 1))));

            // When
            SettlementDetailReportDto result = reportService.getSettlementDetailReport(CURRENT_ID, BudgetType.INCOME);

            // Then
            assertThat(result.getTotalBudgetedAmount()).isEqualTo(800L);
            assertThat(result.getTotalActualAmount()).isEqualTo(200L);
            assertThat(result.getTotalExecutionRate()).isEqualTo(25.0);
        }
    }

    @Nested
    @DisplayName("logOutput 테스트")
    class LogOutputTest {

        @Test
        @DisplayName("회계 연도 단위 보고서는 회계 연도의 이름을 대상으로, 출력 방식을 구분해 활동 기록에 남긴다")
        void logOutput_AnnualReport_LogsFiscalYearNameAsTarget() {
            // Given
            ReportType reportType = ReportType.ANNUAL_SETTLEMENT;
            givenFiscalYear();

            // When
            reportService.logOutput(reportType, ReportOutputType.PDF, CURRENT_ID, null, null);

            // Then
            verify(activityLogService).logReportPrint("회계 연도 수입/지출 결산 보고서", "2026년", "PDF 저장");
        }

        @Test
        @DisplayName("주간 보고서는 회계 연도의 이름과 주간 범위를 대상으로 활동 기록에 남긴다")
        void logOutput_WeeklyReport_LogsFiscalYearNameAndPeriodAsTarget() {
            // Given
            ReportType reportType = ReportType.WEEKLY_SETTLEMENT;
            givenFiscalYear();

            // When
            reportService.logOutput(reportType, ReportOutputType.PRINT, CURRENT_ID, WEEK_START, WEEK_END);

            // Then
            verify(activityLogService).logReportPrint("주간 회계 결산 보고서", "2026년 (2026-10-04 ~ 2026-10-10)", "인쇄");
        }
    }
}
