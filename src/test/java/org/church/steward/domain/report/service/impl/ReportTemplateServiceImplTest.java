package org.church.steward.domain.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTableColumn;
import org.church.steward.domain.report.entity.ReportTableOption;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.mapper.ReportTemplateMapper;
import org.church.steward.domain.report.repository.ReportApprovalRepository;
import org.church.steward.domain.report.repository.ReportTableColumnRepository;
import org.church.steward.domain.report.repository.ReportTableOptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportTemplateServiceImplTest {

    @Mock
    private ReportApprovalRepository reportApprovalRepository;

    @Mock
    private ReportTableColumnRepository reportTableColumnRepository;

    @Mock
    private ReportTableOptionRepository reportTableOptionRepository;

    @Spy
    private ReportTemplateMapper reportTemplateMapper = new ReportTemplateMapper();

    @InjectMocks
    private ReportTemplateServiceImpl reportTemplateService;

    @Captor
    private ArgumentCaptor<List<ReportTableColumn>> columnsCaptor;

    private static ReportApprovalDto createApproval(boolean visible, String... titles) {
        return ReportApprovalDto.builder()
                .visible(visible)
                .titles(Arrays.asList(titles))
                .build();
    }

    /**
     * 칸들을 순서대로 담는다.
     *
     * @param hidden 숨길 칸
     */
    private static List<ReportColumnDto> createColumns(List<ReportColumn> columns, ReportColumn... hidden) {
        Set<ReportColumn> hiddenSet = Set.of(hidden);
        return columns.stream()
                .map(column -> ReportColumnDto.builder()
                        .column(column)
                        .hidden(hiddenSet.contains(column))
                        .build())
                .toList();
    }

    /**
     * 주간 회계 결산 보고서의 양식. 주어진 표 말고는 기본 순서이고 현황 표의 막대는 보인다.
     */
    private static ReportTemplateDto createTemplate(ReportApprovalDto approval,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides) {
        return createTemplate(approval, overrides, true);
    }

    /**
     * @param rateBarVisible 모든 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부
     */
    private static ReportTemplateDto createTemplate(ReportApprovalDto approval,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides, boolean rateBarVisible) {
        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBars = new EnumMap<>(ReportTemplateTable.class);
        for (ReportTemplateTable table : ReportTemplateTable.of(ReportType.WEEKLY_SETTLEMENT)) {
            tables.put(table, overrides.getOrDefault(table, createColumns(table.getDefaultColumns())));
            if (table.hasRateBarOption()) {
                rateBars.put(table, rateBarVisible);
            }
        }
        return ReportTemplateDto.builder()
                .reportType(ReportType.WEEKLY_SETTLEMENT)
                .approval(approval)
                .tables(tables)
                .rateBarVisible(rateBars)
                .build();
    }

    /**
     * 회계 연도 단위 보고서(예산안, 결산 보고서)의 양식. 주어진 표 말고는 기본 순서이고, 결재란이 있으면 직함이 빈 세 칸이며 현황 표의
     * 막대는 보인다.
     */
    private static ReportTemplateDto createFiscalYearTemplate(ReportType reportType,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides) {
        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBars = new EnumMap<>(ReportTemplateTable.class);
        for (ReportTemplateTable table : ReportTemplateTable.of(reportType)) {
            tables.put(table, overrides.getOrDefault(table, createColumns(table.getDefaultColumns())));
            if (table.hasRateBarOption()) {
                rateBars.put(table, true);
            }
        }
        return ReportTemplateDto.builder()
                .reportType(reportType)
                .approval(reportType.hasApproval() ? createApproval(true, null, null, null) : null)
                .tables(tables)
                .rateBarVisible(rateBars)
                .build();
    }

    private static ReportTemplateDto createIncomeRecordsTemplate(List<ReportColumnDto> columns) {
        return createTemplate(createApproval(true, null, null, null),
                Map.of(ReportTemplateTable.WEEKLY_INCOME_RECORDS, columns));
    }

    private static ReportTableColumn createSavedColumn(ReportTemplateTable table, ReportColumn column, int sortOrder,
            boolean hidden) {
        return ReportTableColumn.builder()
                .tableType(table)
                .columnType(column)
                .sortOrder(sortOrder)
                .hidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("getReportTemplate 테스트")
    class GetReportTemplateTest {

        @Test
        @DisplayName("저장한 적이 없으면 결재란은 직함이 빈 세 칸을 보이는 것이다")
        void getReportTemplate_NotSaved_ReturnsDefaultApproval() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getApproval()
                    .isVisible()).isTrue();
            assertThat(actual.getApproval()
                    .getTitles()).containsExactly(null, null, null);
        }

        @Test
        @DisplayName("저장한 적이 없으면 표마다 숨긴 칸 없이 기본 순서다")
        void getReportTemplate_NotSaved_ReturnsDefaultColumns() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getTables()
                    .get(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS)).extracting(ReportColumnDto::getColumn,
                            ReportColumnDto::isHidden)
                    .containsExactly(tuple(ReportColumn.DESCRIPTION, false), tuple(ReportColumn.AMOUNT, false),
                            tuple(ReportColumn.CONTACT, false), tuple(ReportColumn.REMARKS, false));
        }

        @Test
        @DisplayName("저장된 결재란이 있으면 그 표시 여부와 직함을 반환한다")
        void getReportTemplate_SavedApproval_ReturnsSaved() {
            // Given
            when(reportApprovalRepository.findById(ReportType.WEEKLY_SETTLEMENT)).thenReturn(Optional.of(
                    ReportApproval.builder()
                            .reportType(ReportType.WEEKLY_SETTLEMENT)
                            .visible(false)
                            .titles(Arrays.asList("담당", "담임목사"))
                            .build()));

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getApproval()).isEqualTo(createApproval(false, "담당", "담임목사"));
        }

        @Test
        @DisplayName("저장된 칸이 있으면 그 순서와 숨김을 반환한다")
        void getReportTemplate_SavedColumns_ReturnsSavedOrder() {
            // Given
            ReportTemplateTable table = ReportTemplateTable.WEEKLY_INCOME_RECORDS;
            List<ReportTableColumn> saved = List.of(createSavedColumn(table, ReportColumn.DESCRIPTION, 0, false),
                    createSavedColumn(table, ReportColumn.AMOUNT, 1, false),
                    createSavedColumn(table, ReportColumn.CONTACT, 2, false),
                    createSavedColumn(table, ReportColumn.REMARKS, 3, true));
            when(reportTableColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getTables()
                    .get(table)).isEqualTo(createColumns(
                            List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT,
                                    ReportColumn.CONTACT, ReportColumn.REMARKS),
                            ReportColumn.REMARKS));
        }

        @Test
        @DisplayName("저장된 칸이 지금의 칸 구성과 맞지 않으면 기본 순서를 반환한다")
        void getReportTemplate_SavedColumnsMissing_ReturnsDefaultColumns() {
            // Given
            ReportTemplateTable table = ReportTemplateTable.WEEKLY_INCOME_RECORDS;
            List<ReportTableColumn> saved = List.of(createSavedColumn(table, ReportColumn.AMOUNT, 0, false),
                    createSavedColumn(table, ReportColumn.CONTACT, 1, false));
            when(reportTableColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getTables()
                    .get(table)).isEqualTo(createColumns(table.getDefaultColumns()));
        }

        @Test
        @DisplayName("회계 연도 결산 보고서도 저장한 적이 없으면 결재란은 직함이 빈 세 칸을 보이는 것이다")
        void getReportTemplate_AnnualSettlementNotSaved_ReturnsDefaultApproval() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.ANNUAL_SETTLEMENT);

            // Then
            assertThat(actual.getApproval()).isEqualTo(createApproval(true, null, null, null));
        }

        @Test
        @DisplayName("회계 연도 결산 보고서의 결재란은 주간 보고서의 결재란과 따로 읽는다")
        void getReportTemplate_AnnualSettlement_ReadsOwnApproval() {
            // Given
            when(reportApprovalRepository.findById(ReportType.ANNUAL_SETTLEMENT)).thenReturn(Optional.of(
                    ReportApproval.builder()
                            .reportType(ReportType.ANNUAL_SETTLEMENT)
                            .visible(true)
                            .titles(Arrays.asList("재정부장", "감사"))
                            .build()));

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.ANNUAL_SETTLEMENT);

            // Then
            assertThat(actual.getApproval()).isEqualTo(createApproval(true, "재정부장", "감사"));
        }

        @Test
        @DisplayName("결재란이 없는 보고서는 결재란이 null이다")
        void getReportTemplate_ReportWithoutApproval_ReturnsNullApproval() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.BUDGET_PLAN);

            // Then
            assertThat(actual.getApproval()).isNull();
        }

        @Test
        @DisplayName("회계 연도 예산안에는 수입 예산안과 지출 예산안의 두 표가 있다")
        void getReportTemplate_BudgetPlan_HasIncomeAndExpenseTables() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.BUDGET_PLAN);

            // Then
            assertThat(actual.getTables()).containsOnlyKeys(ReportTemplateTable.INCOME_BUDGET_PLAN,
                    ReportTemplateTable.EXPENSE_BUDGET_PLAN);
        }

        @Test
        @DisplayName("저장한 적이 없으면 지출 예산안의 표는 예산 항목, 예산액, 점유율, 직전 대비 증감액, 비고 순이다")
        void getReportTemplate_BudgetPlanNotSaved_ReturnsDefaultColumns() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.BUDGET_PLAN);

            // Then
            assertThat(actual.getTables()
                    .get(ReportTemplateTable.EXPENSE_BUDGET_PLAN)).extracting(ReportColumnDto::getColumn)
                    .containsExactly(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                            ReportColumn.CHANGE, ReportColumn.REMARKS);
        }

        @Test
        @DisplayName("저장한 적이 없으면 수입 결산 세부 내역서의 표는 예산 항목, 예산액, 점유율, 수입액, 달성률, 비고 순이다")
        void getReportTemplate_SettlementDetailNotSaved_ReturnsDefaultColumns() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.SETTLEMENT_DETAIL);

            // Then
            assertThat(actual.getTables()
                    .get(ReportTemplateTable.INCOME_SETTLEMENT_DETAIL)).extracting(ReportColumnDto::getColumn)
                    .containsExactly(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                            ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE, ReportColumn.REMARKS);
        }

        @Test
        @DisplayName("결산 세부 내역서는 비율 칸이 있어도 막대 설정이 없다")
        void getReportTemplate_SettlementDetail_HasNoRateBarOption() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.SETTLEMENT_DETAIL);

            // Then
            assertThat(actual.getRateBarVisible()).isEmpty();
        }

        @Test
        @DisplayName("회계 연도 결산 세부 내역서에는 수입 결산 세부 내역과 지출 결산 세부 내역의 두 표가 있고 결재란은 없다")
        void getReportTemplate_SettlementDetail_HasIncomeAndExpenseTablesWithoutApproval() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.SETTLEMENT_DETAIL);

            // Then
            assertThat(actual.getTables()).containsOnlyKeys(ReportTemplateTable.INCOME_SETTLEMENT_DETAIL,
                    ReportTemplateTable.EXPENSE_SETTLEMENT_DETAIL);
            assertThat(actual.getApproval()).isNull();
        }

        @Test
        @DisplayName("회계 연도 결산 보고서에는 현황 표 둘만 있다")
        void getReportTemplate_AnnualSettlement_HasOnlyStatusTables() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.ANNUAL_SETTLEMENT);

            // Then
            assertThat(actual.getTables()).containsOnlyKeys(ReportTemplateTable.ANNUAL_INCOME_STATUS,
                    ReportTemplateTable.ANNUAL_EXPENSE_STATUS);
        }

        @Test
        @DisplayName("회계 연도 결산 보고서의 현황 표는 주간 보고서의 현황 표와 따로 막대 설정을 읽는다")
        void getReportTemplate_AnnualSettlement_ReadsOwnRateBarOptions() {
            // Given
            when(reportTableOptionRepository.findById(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == ReportTemplateTable.ANNUAL_INCOME_STATUS
                            ? Optional.of(ReportTableOption.builder()
                                    .tableType(ReportTemplateTable.ANNUAL_INCOME_STATUS)
                                    .rateBarVisible(false)
                                    .build())
                            : Optional.empty());

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.ANNUAL_SETTLEMENT);

            // Then
            assertThat(actual.getRateBarVisible()).containsOnly(
                    entry(ReportTemplateTable.ANNUAL_INCOME_STATUS, false),
                    entry(ReportTemplateTable.ANNUAL_EXPENSE_STATUS, true));
        }

        @Test
        @DisplayName("저장한 적이 없어도 기본 양식을 저장하지는 않는다")
        void getReportTemplate_NotSaved_DoesNotSave() {
            // When
            reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            verify(reportApprovalRepository, never()).save(any());
        }

        @Test
        @DisplayName("저장한 적이 없으면 현황 표의 비율 칸에 막대를 함께 그리고, 내역 표에는 막대 설정이 없다")
        void getReportTemplate_NotSaved_ShowsRateBarOnStatusTables() {
            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getRateBarVisible()).containsOnly(
                    entry(ReportTemplateTable.WEEKLY_INCOME_STATUS, true),
                    entry(ReportTemplateTable.WEEKLY_EXPENSE_STATUS, true));
        }

        @Test
        @DisplayName("저장된 막대 설정이 있으면 그 값을 반환한다")
        void getReportTemplate_SavedRateBar_ReturnsSaved() {
            // Given
            when(reportTableOptionRepository.findById(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == ReportTemplateTable.WEEKLY_EXPENSE_STATUS
                            ? Optional.of(ReportTableOption.builder()
                                    .tableType(ReportTemplateTable.WEEKLY_EXPENSE_STATUS)
                                    .rateBarVisible(false)
                                    .build())
                            : Optional.empty());

            // When
            ReportTemplateDto actual = reportTemplateService.getReportTemplate(ReportType.WEEKLY_SETTLEMENT);

            // Then
            assertThat(actual.getRateBarVisible()).containsOnly(
                    entry(ReportTemplateTable.WEEKLY_INCOME_STATUS, true),
                    entry(ReportTemplateTable.WEEKLY_EXPENSE_STATUS, false));
        }
    }

    @Nested
    @DisplayName("updateReportTemplate 테스트")
    class UpdateReportTemplateTest {

        @Test
        @DisplayName("현황 표마다 비율 칸의 막대 설정을 저장한다")
        void updateReportTemplate_RateBar_SavesOptionPerStatusTable() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null), Map.of(), false);
            ArgumentCaptor<ReportTableOption> captor = ArgumentCaptor.forClass(ReportTableOption.class);

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportTableOptionRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(ReportTableOption::getTableType,
                    ReportTableOption::isRateBarVisible)
                    .containsExactly(tuple(ReportTemplateTable.WEEKLY_INCOME_STATUS, false),
                            tuple(ReportTemplateTable.WEEKLY_EXPENSE_STATUS, false));
        }

        @Test
        @DisplayName("현황 표의 막대 설정이 빠져 있으면 예외를 던진다")
        void updateReportTemplate_MissingRateBar_ThrowsException() {
            // Given
            Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
            for (ReportTemplateTable table : ReportTemplateTable.of(ReportType.WEEKLY_SETTLEMENT)) {
                tables.put(table, createColumns(table.getDefaultColumns()));
            }
            ReportTemplateDto template = ReportTemplateDto.builder()
                    .reportType(ReportType.WEEKLY_SETTLEMENT)
                    .approval(createApproval(true, null, null, null))
                    .tables(tables)
                    .rateBarVisible(Map.of(ReportTemplateTable.WEEKLY_INCOME_STATUS, true))
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("보고서 양식에 빠진 항목이 있습니다.");
        }

        @Test
        @DisplayName("결재란을 처음 저장하면 행을 새로 만들고 직함의 앞뒤 공백을 지우며 빈 직함은 null로 저장한다")
        void updateReportTemplate_NewApproval_SavesNormalizedTitles() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(false, " 담당 ", "  ", null, "담임목사"),
                    Map.of());
            ArgumentCaptor<ReportApproval> captor = ArgumentCaptor.forClass(ReportApproval.class);

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportApprovalRepository).save(captor.capture());
            assertThat(captor.getValue()
                    .getReportType()).isEqualTo(ReportType.WEEKLY_SETTLEMENT);
            assertThat(captor.getValue()
                    .isVisible()).isFalse();
            assertThat(captor.getValue()
                    .getTitles()).containsExactly("담당", null, null, "담임목사");
        }

        @Test
        @DisplayName("저장된 결재란이 있으면 그 행을 고쳐 저장한다")
        void updateReportTemplate_ExistingApproval_UpdatesSavedRow() {
            // Given
            ReportApproval existing = ReportApproval.defaults(ReportType.WEEKLY_SETTLEMENT);
            when(reportApprovalRepository.findById(ReportType.WEEKLY_SETTLEMENT)).thenReturn(Optional.of(existing));
            ArgumentCaptor<ReportApproval> captor = ArgumentCaptor.forClass(ReportApproval.class);

            // When
            reportTemplateService.updateReportTemplate(createTemplate(createApproval(true, "회계"), Map.of()));

            // Then
            verify(reportApprovalRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue()
                    .getTitles()).containsExactly("회계");
        }

        @Test
        @DisplayName("표마다 칸을 목록의 순서대로 순번과 숨김을 매겨 저장한다")
        void updateReportTemplate_Columns_SavesSortOrderAndHidden() {
            // Given
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(List.of(ReportColumn.DESCRIPTION,
                    ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.REMARKS), ReportColumn.REMARKS));

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportTableColumnRepository, times(4)).saveAll(columnsCaptor.capture());
            List<ReportTableColumn> incomeRecords = columnsCaptor.getAllValues()
                    .stream()
                    .filter(columns -> columns.get(0)
                            .getTableType() == ReportTemplateTable.WEEKLY_INCOME_RECORDS)
                    .findFirst()
                    .orElseThrow();
            assertThat(incomeRecords).extracting(ReportTableColumn::getColumnType, ReportTableColumn::getSortOrder,
                    ReportTableColumn::isHidden)
                    .containsExactly(tuple(ReportColumn.DESCRIPTION, 0, false), tuple(ReportColumn.AMOUNT, 1, false),
                            tuple(ReportColumn.CONTACT, 2, false), tuple(ReportColumn.REMARKS, 3, true));
        }

        @Test
        @DisplayName("저장된 칸이 있으면 같은 칸의 행을 고쳐 저장한다")
        void updateReportTemplate_ExistingColumns_UpdatesSavedRows() {
            // Given
            ReportTemplateTable table = ReportTemplateTable.WEEKLY_INCOME_RECORDS;
            ReportTableColumn savedRemarks = createSavedColumn(table, ReportColumn.REMARKS, 3, false);
            List<ReportTableColumn> saved = List.of(createSavedColumn(table, ReportColumn.CONTACT, 0, false),
                    createSavedColumn(table, ReportColumn.AMOUNT, 1, false),
                    createSavedColumn(table, ReportColumn.DESCRIPTION, 2, false), savedRemarks);
            when(reportTableColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(table.getDefaultColumns(),
                    ReportColumn.REMARKS));

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportTableColumnRepository, times(4)).saveAll(columnsCaptor.capture());
            assertThat(columnsCaptor.getAllValues()).anySatisfy(columns -> assertThat(columns).contains(savedRemarks));
            assertThat(savedRemarks.isHidden()).isTrue();
        }

        @Test
        @DisplayName("결재란의 칸이 하나도 없으면 예외를 던진다")
        void updateReportTemplate_NoApprovalCell_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(createTemplate(createApproval(true),
                    Map.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결재란은 1칸부터 5칸까지 둘 수 있습니다.");
        }

        @Test
        @DisplayName("결재란의 칸이 다섯 칸을 넘으면 예외를 던진다")
        void updateReportTemplate_TooManyApprovalCells_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(createTemplate(createApproval(true,
                    null, null, null, null, null, null), Map.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결재란은 1칸부터 5칸까지 둘 수 있습니다.");
        }

        @Test
        @DisplayName("공백을 지운 직함이 7자를 넘으면 예외를 던진다")
        void updateReportTemplate_TooLongTitle_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(createTemplate(createApproval(true,
                    "재정위원회위원장"), Map.of())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("결재란의 직함은 7자 이내로 입력해 주세요.");
        }

        @Test
        @DisplayName("첫 번째 칸에 올 수 없는 칸을 두면 예외를 던진다")
        void updateReportTemplate_InvalidFirstColumn_ThrowsException() {
            // Given
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(List.of(ReportColumn.AMOUNT,
                    ReportColumn.CONTACT, ReportColumn.DESCRIPTION, ReportColumn.REMARKS)));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 수입 내역의 첫 번째 칸에는 봉헌자 또는 내용만 둘 수 있습니다.");
        }

        @Test
        @DisplayName("현황 표의 첫 번째 칸이 예산 항목이 아니면 예외를 던진다")
        void updateReportTemplate_StatusFirstColumnNotBudget_ThrowsException() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null),
                    Map.of(ReportTemplateTable.WEEKLY_EXPENSE_STATUS, createColumns(List.of(ReportColumn.ACTUAL_AMOUNT,
                            ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.RATE))));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지출 현황의 첫 번째 칸에는 예산 항목만 둘 수 있습니다.");
        }

        @Test
        @DisplayName("숨길 수 없는 칸을 숨기면 예외를 던진다")
        void updateReportTemplate_HideAmount_ThrowsException() {
            // Given
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(
                    ReportTemplateTable.WEEKLY_INCOME_RECORDS.getDefaultColumns(), ReportColumn.AMOUNT));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 수입 내역의 금액 칸은 숨길 수 없습니다.");
        }

        @Test
        @DisplayName("첫 번째 칸이 내용이면 내용을 숨길 수 없다")
        void updateReportTemplate_HideFirstDescription_ThrowsException() {
            // Given
            ReportTemplateDto template = createTemplate(createApproval(true, null, null, null),
                    Map.of(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS, createColumns(
                            ReportTemplateTable.WEEKLY_EXPENSE_RECORDS.getDefaultColumns(), ReportColumn.DESCRIPTION)));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 지출 내역의 내용 칸은 숨길 수 없습니다.");
        }

        @Test
        @DisplayName("표의 칸이 빠지거나 겹치면 예외를 던진다")
        void updateReportTemplate_DuplicatedColumn_ThrowsException() {
            // Given
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(List.of(ReportColumn.CONTACT,
                    ReportColumn.AMOUNT, ReportColumn.AMOUNT, ReportColumn.REMARKS)));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 수입 내역의 칸 구성이 올바르지 않습니다.");
        }

        @Test
        @DisplayName("회계 연도 결산 보고서의 결재란은 그 보고서의 행으로 저장한다")
        void updateReportTemplate_AnnualSettlementApproval_SavesUnderOwnReportType() {
            // Given
            Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
            Map<ReportTemplateTable, Boolean> rateBars = new EnumMap<>(ReportTemplateTable.class);
            for (ReportTemplateTable table : ReportTemplateTable.of(ReportType.ANNUAL_SETTLEMENT)) {
                tables.put(table, createColumns(table.getDefaultColumns()));
                rateBars.put(table, true);
            }
            ReportTemplateDto template = ReportTemplateDto.builder()
                    .reportType(ReportType.ANNUAL_SETTLEMENT)
                    .approval(createApproval(true, "재정부장", " 감사 "))
                    .tables(tables)
                    .rateBarVisible(rateBars)
                    .build();
            ArgumentCaptor<ReportApproval> captor = ArgumentCaptor.forClass(ReportApproval.class);

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportApprovalRepository).save(captor.capture());
            assertThat(captor.getValue()
                    .getReportType()).isEqualTo(ReportType.ANNUAL_SETTLEMENT);
            assertThat(captor.getValue()
                    .getTitles()).containsExactly("재정부장", "감사");
        }

        @Test
        @DisplayName("예산안의 첫 번째 칸이 예산 항목이 아니면 예외를 던진다")
        void updateReportTemplate_BudgetPlanFirstColumnNotBudget_ThrowsException() {
            // Given
            ReportTemplateDto template = createFiscalYearTemplate(ReportType.BUDGET_PLAN,
                    Map.of(ReportTemplateTable.INCOME_BUDGET_PLAN, createColumns(List.of(
                            ReportColumn.BUDGETED_AMOUNT, ReportColumn.BUDGET_NAME, ReportColumn.SHARE_RATE,
                            ReportColumn.CHANGE, ReportColumn.REMARKS))));

            // When & Then
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수입 예산안의 첫 번째 칸에는 예산 항목만 둘 수 있습니다.");
        }

        @Test
        @DisplayName("결산 세부 내역서의 칸 순서를 저장한다")
        void updateReportTemplate_SettlementDetail_SavesColumns() {
            // Given
            List<ReportColumn> order = List.of(ReportColumn.BUDGET_NAME, ReportColumn.REMARKS,
                    ReportColumn.BUDGETED_AMOUNT, ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE,
                    ReportColumn.SHARE_RATE);
            ReportTemplateDto template = createFiscalYearTemplate(ReportType.SETTLEMENT_DETAIL,
                    Map.of(ReportTemplateTable.EXPENSE_SETTLEMENT_DETAIL, createColumns(order)));

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportTableColumnRepository, times(2)).saveAll(columnsCaptor.capture());
            assertThat(columnsCaptor.getAllValues()).anySatisfy(columns -> assertThat(columns)
                    .allMatch(column -> column.getTableType() == ReportTemplateTable.EXPENSE_SETTLEMENT_DETAIL)
                    .extracting(ReportTableColumn::getColumnType)
                    .containsExactlyElementsOf(order));
        }

        @Test
        @DisplayName("결산 세부 내역서는 막대 설정이 없으므로 저장하지 않는다")
        void updateReportTemplate_SettlementDetail_DoesNotSaveOption() {
            // Given
            ReportTemplateDto template = createFiscalYearTemplate(ReportType.SETTLEMENT_DETAIL, Map.of());

            // When
            reportTemplateService.updateReportTemplate(template);

            // Then
            verify(reportTableOptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("표 하나라도 규칙에 맞지 않으면 결재란도 저장하지 않는다")
        void updateReportTemplate_InvalidColumns_DoesNotSaveApproval() {
            // Given
            ReportTemplateDto template = createIncomeRecordsTemplate(createColumns(
                    ReportTemplateTable.WEEKLY_INCOME_RECORDS.getDefaultColumns(), ReportColumn.CONTACT));

            // When
            assertThatThrownBy(() -> reportTemplateService.updateReportTemplate(template))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(reportApprovalRepository, never()).save(any());
        }
    }
}
