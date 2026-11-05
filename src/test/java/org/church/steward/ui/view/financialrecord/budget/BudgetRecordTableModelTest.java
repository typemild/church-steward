package org.church.steward.ui.view.financialrecord.budget;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetRecordTableModelTest {

    private static final long BUDGET_ID = 1L;
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    private static FinancialRecordResponseDto createRecord(long id, LocalDate date, String contactName,
            String description) {
        return FinancialRecordResponseDto.builder()
                .id(id)
                .type(FinancialRecordType.INCOME)
                .date(date)
                .budgetId(BUDGET_ID)
                .budgetName("주일헌금")
                .description(description)
                .amount(10_000L)
                .contactIds(List.of(id))
                .contactNames(List.of(contactName))
                .build();
    }

    /**
     * 모든 예산을 펼친 뒤 내역 행의 ID를 위에서부터 차례로 꺼낸다.
     */
    private static List<Long> recordIdsOf(BudgetRecordTableModel model) {
        model.expandAll();
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            BudgetRecordRow row = model.getRowValue(i);
            if (!row.isBudgetRow()) {
                ids.add(row.getRecord()
                        .getId());
            }
        }
        return ids;
    }

    @Nested
    @DisplayName("setRecords 테스트")
    class SetRecordsTest {

        @Test
        @DisplayName("기본으로는 같은 날짜의 내역을 첫 번째 칸인 관계자의 가나다순으로 늘어놓는다")
        void setRecords_Default_SortsSameDateByContactName() {
            // Given
            BudgetRecordTableModel model = new BudgetRecordTableModel();
            List<FinancialRecordResponseDto> records = List.of(createRecord(1L, DATE, "홍길동", "감사"),
                    createRecord(2L, DATE, "김철수", "주정"));

            // When
            model.setRecords(records, Map.of(BUDGET_ID, 0));

            // Then
            assertThat(recordIdsOf(model)).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("첫 번째 칸이 내용이면 같은 날짜의 내역을 내용의 가나다순으로 늘어놓는다")
        void setRecords_DescriptionFirst_SortsSameDateByDescription() {
            // Given
            BudgetRecordTableModel model = new BudgetRecordTableModel();
            model.setFirstColumn(ReportColumn.DESCRIPTION);
            List<FinancialRecordResponseDto> records = List.of(createRecord(1L, DATE, "김철수", "주정"),
                    createRecord(2L, DATE, "홍길동", "감사"));

            // When
            model.setRecords(records, Map.of(BUDGET_ID, 0));

            // Then
            assertThat(recordIdsOf(model)).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("가나다순이어도 일자가 앞선 내역이 먼저다")
        void setRecords_Default_SortsByDateFirst() {
            // Given
            BudgetRecordTableModel model = new BudgetRecordTableModel();
            List<FinancialRecordResponseDto> records = List.of(createRecord(1L, DATE.plusDays(1), "김철수", null),
                    createRecord(2L, DATE, "홍길동", null));

            // When
            model.setRecords(records, Map.of(BUDGET_ID, 0));

            // Then
            assertThat(recordIdsOf(model)).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("가나다순에서 값이 같은 내역은 입력순(ID 오름차순)이다")
        void setRecords_SameName_SortsById() {
            // Given
            BudgetRecordTableModel model = new BudgetRecordTableModel();
            List<FinancialRecordResponseDto> records = List.of(createRecord(3L, DATE, "홍길동", null),
                    createRecord(1L, DATE, "홍길동", null));

            // When
            model.setRecords(records, Map.of(BUDGET_ID, 0));

            // Then
            assertThat(recordIdsOf(model)).containsExactly(1L, 3L);
        }

        @Test
        @DisplayName("같은 날짜의 순서가 입력순이면 같은 날짜의 내역을 ID 오름차순으로 늘어놓는다")
        void setRecords_EntryOrder_SortsSameDateById() {
            // Given
            BudgetRecordTableModel model = new BudgetRecordTableModel();
            model.setRecordOrder(RecordOrder.ENTRY);
            List<FinancialRecordResponseDto> records = List.of(createRecord(2L, DATE, "김철수", null),
                    createRecord(1L, DATE, "홍길동", null));

            // When
            model.setRecords(records, Map.of(BUDGET_ID, 0));

            // Then
            assertThat(recordIdsOf(model)).containsExactly(1L, 2L);
        }
    }
}
