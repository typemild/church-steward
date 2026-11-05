package org.church.steward.domain.programsetting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.church.steward.domain.programsetting.dto.BudgetViewColumnsDto;
import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.programsetting.entity.BudgetViewOption;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.domain.programsetting.mapper.ProgramSettingMapper;
import org.church.steward.domain.programsetting.repository.BudgetViewColumnRepository;
import org.church.steward.domain.programsetting.repository.BudgetViewOptionRepository;
import org.church.steward.domain.programsetting.repository.ProgramSettingRepository;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
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
class ProgramSettingServiceImplTest {

    @Mock
    private ProgramSettingRepository programSettingRepository;

    @Mock
    private BudgetViewColumnRepository budgetViewColumnRepository;

    @Mock
    private BudgetViewOptionRepository budgetViewOptionRepository;

    @Spy
    private ProgramSettingMapper programSettingMapper = new ProgramSettingMapper();

    @InjectMocks
    private ProgramSettingServiceImpl programSettingService;

    @Captor
    private ArgumentCaptor<List<BudgetViewColumn>> columnsCaptor;

    private static ProgramSetting createSetting(DayOfWeek weeklyClosingDay) {
        return ProgramSetting.builder()
                .id(ProgramSettingConstraints.SETTING_ID)
                .weeklyClosingDay(weeklyClosingDay)
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
     * 예산별 보기의 칸 설정. 주어진 표 말고는 기본 순서이고, 같은 날짜의 순서는 모두 가나다순이다.
     */
    private static BudgetViewColumnsDto createBudgetViewColumns(BudgetViewTable table,
            List<ReportColumnDto> columns) {
        Map<BudgetViewTable, List<ReportColumnDto>> tables = new EnumMap<>(BudgetViewTable.class);
        for (BudgetViewTable each : BudgetViewTable.values()) {
            tables.put(each, each == table ? columns : createColumns(each.getDefaultColumns()));
        }
        return BudgetViewColumnsDto.builder()
                .tables(tables)
                .recordOrders(createRecordOrders(BudgetViewTable.INCOME, RecordOrder.FIRST_COLUMN))
                .build();
    }

    /**
     * 같은 날짜의 순서. 주어진 표 말고는 가나다순이다.
     */
    private static Map<BudgetViewTable, RecordOrder> createRecordOrders(BudgetViewTable table,
            RecordOrder recordOrder) {
        Map<BudgetViewTable, RecordOrder> recordOrders = new EnumMap<>(BudgetViewTable.class);
        for (BudgetViewTable each : BudgetViewTable.values()) {
            recordOrders.put(each, each == table ? recordOrder : RecordOrder.FIRST_COLUMN);
        }
        return recordOrders;
    }

    /**
     * 칸은 모두 기본 순서이고 같은 날짜의 순서만 주어진 대로인 예산별 보기의 칸 설정
     */
    private static BudgetViewColumnsDto createBudgetViewColumns(Map<BudgetViewTable, RecordOrder> recordOrders) {
        Map<BudgetViewTable, List<ReportColumnDto>> tables = new EnumMap<>(BudgetViewTable.class);
        for (BudgetViewTable each : BudgetViewTable.values()) {
            tables.put(each, createColumns(each.getDefaultColumns()));
        }
        return BudgetViewColumnsDto.builder()
                .tables(tables)
                .recordOrders(recordOrders)
                .build();
    }

    private static BudgetViewOption createSavedOption(BudgetViewTable table, RecordOrder recordOrder) {
        return BudgetViewOption.builder()
                .tableType(table)
                .recordOrder(recordOrder)
                .build();
    }

    private static BudgetViewColumn createSavedColumn(BudgetViewTable table, ReportColumn column, int sortOrder,
            boolean hidden) {
        return BudgetViewColumn.builder()
                .tableType(table)
                .columnType(column)
                .sortOrder(sortOrder)
                .hidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("getWeeklyClosingDay 테스트")
    class GetWeeklyClosingDayTest {

        @Test
        @DisplayName("저장된 설정이 있으면 그 결산 요일을 반환한다")
        void getWeeklyClosingDay_Existing_ReturnsSaved() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID))
                    .thenReturn(Optional.of(createSetting(DayOfWeek.SATURDAY)));

            // When
            DayOfWeek actual = programSettingService.getWeeklyClosingDay();

            // Then
            assertThat(actual).isEqualTo(DayOfWeek.SATURDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없으면(기존 데이터나 과거 버전의 백업) 기본값인 일요일을 반환한다")
        void getWeeklyClosingDay_NotSaved_ReturnsSunday() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());

            // When
            DayOfWeek actual = programSettingService.getWeeklyClosingDay();

            // Then
            assertThat(actual).isEqualTo(DayOfWeek.SUNDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없어도 기본값을 저장하지는 않는다")
        void getWeeklyClosingDay_NotSaved_DoesNotSave() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());

            // When
            programSettingService.getWeeklyClosingDay();

            // Then
            verify(programSettingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateWeeklyClosingDay 테스트")
    class UpdateWeeklyClosingDayTest {

        @Test
        @DisplayName("저장된 설정이 있으면 그 설정의 결산 요일을 바꿔 저장한다")
        void updateWeeklyClosingDay_Existing_UpdatesSetting() {
            // Given
            ProgramSetting existing = createSetting(DayOfWeek.SUNDAY);
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID))
                    .thenReturn(Optional.of(existing));
            ArgumentCaptor<ProgramSetting> captor = ArgumentCaptor.forClass(ProgramSetting.class);

            // When
            programSettingService.updateWeeklyClosingDay(DayOfWeek.SATURDAY);

            // Then
            verify(programSettingRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue()
                    .getWeeklyClosingDay()).isEqualTo(DayOfWeek.SATURDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없으면 정해진 ID로 설정을 새로 만들어 저장한다")
        void updateWeeklyClosingDay_NotSaved_CreatesSetting() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());
            ArgumentCaptor<ProgramSetting> captor = ArgumentCaptor.forClass(ProgramSetting.class);

            // When
            programSettingService.updateWeeklyClosingDay(DayOfWeek.WEDNESDAY);

            // Then
            verify(programSettingRepository).save(captor.capture());
            assertThat(captor.getValue()
                    .getId()).isEqualTo(ProgramSettingConstraints.SETTING_ID);
            assertThat(captor.getValue()
                    .getWeeklyClosingDay()).isEqualTo(DayOfWeek.WEDNESDAY);
        }

        @Test
        @DisplayName("요일이 없으면 예외를 던진다")
        void updateWeeklyClosingDay_Null_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> programSettingService.updateWeeklyClosingDay(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 결산 요일을 골라 주세요.");
        }

        @Test
        @DisplayName("요일이 없으면 저장하지 않는다")
        void updateWeeklyClosingDay_Null_DoesNotSave() {
            // When
            assertThatThrownBy(() -> programSettingService.updateWeeklyClosingDay(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(programSettingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getBudgetViewColumns 테스트")
    class GetBudgetViewColumnsTest {

        @Test
        @DisplayName("저장한 적이 없으면 수입 표는 숨긴 칸 없이 보고서의 주간 수입 내역과 같은 기본 순서다")
        void getBudgetViewColumns_NotSaved_IncomeMatchesReportDefault() {
            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getTables()
                    .get(BudgetViewTable.INCOME)).isEqualTo(
                            createColumns(ReportTemplateTable.WEEKLY_INCOME_RECORDS.getDefaultColumns()));
        }

        @Test
        @DisplayName("저장한 적이 없으면 지출 표는 숨긴 칸 없이 보고서의 주간 지출 내역과 같은 기본 순서다")
        void getBudgetViewColumns_NotSaved_ExpenseMatchesReportDefault() {
            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getTables()
                    .get(BudgetViewTable.EXPENSE)).isEqualTo(
                            createColumns(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS.getDefaultColumns()));
        }

        @Test
        @DisplayName("저장된 칸이 있으면 그 순서와 숨김을 반환한다")
        void getBudgetViewColumns_Saved_ReturnsSavedOrder() {
            // Given
            BudgetViewTable table = BudgetViewTable.INCOME;
            List<BudgetViewColumn> saved = List.of(createSavedColumn(table, ReportColumn.DESCRIPTION, 0, false),
                    createSavedColumn(table, ReportColumn.AMOUNT, 1, false),
                    createSavedColumn(table, ReportColumn.CONTACT, 2, false),
                    createSavedColumn(table, ReportColumn.REMARKS, 3, true));
            when(budgetViewColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());

            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getTables()
                    .get(table)).isEqualTo(createColumns(
                            List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT,
                                    ReportColumn.CONTACT, ReportColumn.REMARKS),
                            ReportColumn.REMARKS));
        }

        @Test
        @DisplayName("저장된 칸이 지금의 칸 구성과 맞지 않으면 기본 순서를 반환한다")
        void getBudgetViewColumns_SavedColumnsMissing_ReturnsDefaultColumns() {
            // Given
            BudgetViewTable table = BudgetViewTable.EXPENSE;
            List<BudgetViewColumn> saved = List.of(createSavedColumn(table, ReportColumn.DESCRIPTION, 0, false),
                    createSavedColumn(table, ReportColumn.AMOUNT, 1, false));
            when(budgetViewColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());

            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getTables()
                    .get(table)).isEqualTo(createColumns(table.getDefaultColumns()));
        }

        @Test
        @DisplayName("저장한 적이 없어도 기본 순서를 저장하지는 않는다")
        void getBudgetViewColumns_NotSaved_DoesNotSave() {
            // When
            programSettingService.getBudgetViewColumns();

            // Then
            verify(budgetViewColumnRepository, never()).saveAll(any());
        }

        @Test
        @DisplayName("같은 날짜의 순서를 저장한 적이 없으면 두 표 모두 기본값인 첫 번째 칸의 가나다순이다")
        void getBudgetViewColumns_RecordOrderNotSaved_ReturnsFirstColumn() {
            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getRecordOrders()).containsOnly(
                    Map.entry(BudgetViewTable.INCOME, RecordOrder.FIRST_COLUMN),
                    Map.entry(BudgetViewTable.EXPENSE, RecordOrder.FIRST_COLUMN));
        }

        @Test
        @DisplayName("같은 날짜의 순서를 저장한 표는 저장된 순서를 반환한다")
        void getBudgetViewColumns_RecordOrderSaved_ReturnsSaved() {
            // Given
            when(budgetViewOptionRepository.findById(BudgetViewTable.INCOME)).thenReturn(Optional.empty());
            when(budgetViewOptionRepository.findById(BudgetViewTable.EXPENSE))
                    .thenReturn(Optional.of(createSavedOption(BudgetViewTable.EXPENSE, RecordOrder.ENTRY)));

            // When
            BudgetViewColumnsDto actual = programSettingService.getBudgetViewColumns();

            // Then
            assertThat(actual.getRecordOrders()).containsOnly(
                    Map.entry(BudgetViewTable.INCOME, RecordOrder.FIRST_COLUMN),
                    Map.entry(BudgetViewTable.EXPENSE, RecordOrder.ENTRY));
        }

        @Test
        @DisplayName("같은 날짜의 순서를 저장한 적이 없어도 기본값을 저장하지는 않는다")
        void getBudgetViewColumns_RecordOrderNotSaved_DoesNotSave() {
            // When
            programSettingService.getBudgetViewColumns();

            // Then
            verify(budgetViewOptionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateBudgetViewColumns 테스트")
    class UpdateBudgetViewColumnsTest {

        @Test
        @DisplayName("표마다 칸을 목록의 순서대로 순번과 숨김을 매겨 저장한다")
        void updateBudgetViewColumns_Columns_SavesSortOrderAndHidden() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(BudgetViewTable.INCOME, createColumns(
                    List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.REMARKS),
                    ReportColumn.REMARKS));

            // When
            programSettingService.updateBudgetViewColumns(columns);

            // Then
            verify(budgetViewColumnRepository, times(2)).saveAll(columnsCaptor.capture());
            List<BudgetViewColumn> income = columnsCaptor.getAllValues()
                    .stream()
                    .filter(saved -> saved.get(0)
                            .getTableType() == BudgetViewTable.INCOME)
                    .findFirst()
                    .orElseThrow();
            assertThat(income).extracting(BudgetViewColumn::getColumnType, BudgetViewColumn::getSortOrder,
                    BudgetViewColumn::isHidden)
                    .containsExactly(tuple(ReportColumn.DESCRIPTION, 0, false), tuple(ReportColumn.AMOUNT, 1, false),
                            tuple(ReportColumn.CONTACT, 2, false), tuple(ReportColumn.REMARKS, 3, true));
        }

        @Test
        @DisplayName("저장된 칸이 있으면 같은 칸의 행을 고쳐 저장한다")
        void updateBudgetViewColumns_ExistingColumns_UpdatesSavedRows() {
            // Given
            BudgetViewTable table = BudgetViewTable.EXPENSE;
            BudgetViewColumn savedRemarks = createSavedColumn(table, ReportColumn.REMARKS, 3, false);
            List<BudgetViewColumn> saved = List.of(createSavedColumn(table, ReportColumn.DESCRIPTION, 0, false),
                    createSavedColumn(table, ReportColumn.AMOUNT, 1, false),
                    createSavedColumn(table, ReportColumn.CONTACT, 2, false), savedRemarks);
            when(budgetViewColumnRepository.findAllByTableTypeOrderBySortOrder(any()))
                    .thenAnswer(invocation -> invocation.getArgument(0) == table ? saved : List.of());
            BudgetViewColumnsDto columns = createBudgetViewColumns(table,
                    createColumns(table.getDefaultColumns(), ReportColumn.REMARKS));

            // When
            programSettingService.updateBudgetViewColumns(columns);

            // Then
            verify(budgetViewColumnRepository, times(2)).saveAll(columnsCaptor.capture());
            assertThat(columnsCaptor.getAllValues()).anySatisfy(each -> assertThat(each).contains(savedRemarks));
            assertThat(savedRemarks.isHidden()).isTrue();
        }

        @Test
        @DisplayName("첫 번째 칸에 올 수 없는 칸을 두면 예외를 던진다")
        void updateBudgetViewColumns_InvalidFirstColumn_ThrowsException() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(BudgetViewTable.INCOME, createColumns(
                    List.of(ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.DESCRIPTION,
                            ReportColumn.REMARKS)));

            // When & Then
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수입 내역의 첫 번째 칸에는 봉헌자 또는 내용만 둘 수 있습니다.");
        }

        @Test
        @DisplayName("숨길 수 없는 칸을 숨기면 예외를 던진다")
        void updateBudgetViewColumns_HideContact_ThrowsException() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(BudgetViewTable.EXPENSE,
                    createColumns(BudgetViewTable.EXPENSE.getDefaultColumns(), ReportColumn.CONTACT));

            // When & Then
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지출 내역의 수취인 칸은 숨길 수 없습니다.");
        }

        @Test
        @DisplayName("표가 빠져 있으면 예외를 던진다")
        void updateBudgetViewColumns_MissingTable_ThrowsException() {
            // Given
            BudgetViewColumnsDto columns = BudgetViewColumnsDto.builder()
                    .tables(Map.of(BudgetViewTable.INCOME,
                            createColumns(BudgetViewTable.INCOME.getDefaultColumns())))
                    .recordOrders(createRecordOrders(BudgetViewTable.INCOME, RecordOrder.FIRST_COLUMN))
                    .build();

            // When & Then
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산별 보기의 칸 설정에 빠진 항목이 있습니다.");
        }

        @Test
        @DisplayName("같은 날짜의 순서가 빠진 표가 있으면 예외를 던진다")
        void updateBudgetViewColumns_MissingRecordOrder_ThrowsException() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(
                    Map.of(BudgetViewTable.INCOME, RecordOrder.ENTRY));

            // When & Then
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산별 보기의 칸 설정에 빠진 항목이 있습니다.");
        }

        @Test
        @DisplayName("같은 날짜의 순서가 빠진 표가 있으면 같은 날짜의 순서를 저장하지 않는다")
        void updateBudgetViewColumns_MissingRecordOrder_DoesNotSaveOption() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(
                    Map.of(BudgetViewTable.INCOME, RecordOrder.ENTRY));

            // When
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(budgetViewOptionRepository, never()).save(any());
        }

        @Test
        @DisplayName("표마다 같은 날짜의 순서를 저장한다")
        void updateBudgetViewColumns_RecordOrders_SavesEachTable() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(
                    createRecordOrders(BudgetViewTable.INCOME, RecordOrder.ENTRY));
            ArgumentCaptor<BudgetViewOption> captor = ArgumentCaptor.forClass(BudgetViewOption.class);

            // When
            programSettingService.updateBudgetViewColumns(columns);

            // Then
            verify(budgetViewOptionRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).extracting(BudgetViewOption::getTableType,
                    BudgetViewOption::getRecordOrder)
                    .containsExactlyInAnyOrder(tuple(BudgetViewTable.INCOME, RecordOrder.ENTRY),
                            tuple(BudgetViewTable.EXPENSE, RecordOrder.FIRST_COLUMN));
        }

        @Test
        @DisplayName("같은 날짜의 순서를 저장한 표는 저장된 행을 고쳐 저장한다")
        void updateBudgetViewColumns_ExistingOption_UpdatesSavedRow() {
            // Given
            BudgetViewOption saved = createSavedOption(BudgetViewTable.EXPENSE, RecordOrder.FIRST_COLUMN);
            when(budgetViewOptionRepository.findById(BudgetViewTable.INCOME)).thenReturn(Optional.empty());
            when(budgetViewOptionRepository.findById(BudgetViewTable.EXPENSE)).thenReturn(Optional.of(saved));
            BudgetViewColumnsDto columns = createBudgetViewColumns(
                    createRecordOrders(BudgetViewTable.EXPENSE, RecordOrder.ENTRY));
            ArgumentCaptor<BudgetViewOption> captor = ArgumentCaptor.forClass(BudgetViewOption.class);

            // When
            programSettingService.updateBudgetViewColumns(columns);

            // Then
            verify(budgetViewOptionRepository, times(2)).save(captor.capture());
            assertThat(captor.getAllValues()).contains(saved);
            assertThat(saved.getRecordOrder()).isEqualTo(RecordOrder.ENTRY);
        }

        @Test
        @DisplayName("표 하나라도 규칙에 맞지 않으면 아무 표도 저장하지 않는다")
        void updateBudgetViewColumns_InvalidExpense_DoesNotSave() {
            // Given
            BudgetViewColumnsDto columns = createBudgetViewColumns(BudgetViewTable.EXPENSE, createColumns(
                    List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT, ReportColumn.AMOUNT, ReportColumn.REMARKS)));

            // When
            assertThatThrownBy(() -> programSettingService.updateBudgetViewColumns(columns))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(budgetViewColumnRepository, never()).saveAll(any());
        }
    }
}
