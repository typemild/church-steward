package org.church.steward.domain.programsetting.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.entity.ReportColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BudgetViewColumnRepositoryTest {

    @Autowired
    private BudgetViewColumnRepository budgetViewColumnRepository;

    @Autowired
    private TestEntityManager entityManager;

    private BudgetViewColumn createColumn(BudgetViewTable table, ReportColumn column, int sortOrder, boolean hidden) {
        return BudgetViewColumn.builder()
                .tableType(table)
                .columnType(column)
                .sortOrder(sortOrder)
                .hidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("findAllByTableTypeOrderBySortOrder 테스트")
    class FindAllByTableTypeOrderBySortOrderTest {

        @Test
        @DisplayName("마이그레이션 직후에는 칸 행이 없다")
        void findAllByTableTypeOrderBySortOrder_AfterMigration_ReturnsEmpty() {
            // When
            List<BudgetViewColumn> actual = budgetViewColumnRepository
                    .findAllByTableTypeOrderBySortOrder(BudgetViewTable.INCOME);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        @DisplayName("그 표의 칸만 순서대로 숨김 여부와 함께 읽어 온다")
        void findAllByTableTypeOrderBySortOrder_Saved_ReturnsColumnsOfTableInOrder() {
            // Given
            budgetViewColumnRepository.saveAll(List.of(
                    createColumn(BudgetViewTable.INCOME, ReportColumn.REMARKS, 3, true),
                    createColumn(BudgetViewTable.INCOME, ReportColumn.DESCRIPTION, 0, false),
                    createColumn(BudgetViewTable.INCOME, ReportColumn.CONTACT, 2, false),
                    createColumn(BudgetViewTable.INCOME, ReportColumn.AMOUNT, 1, false),
                    createColumn(BudgetViewTable.EXPENSE, ReportColumn.DESCRIPTION, 0, false)));
            entityManager.flush();
            entityManager.clear();

            // When
            List<BudgetViewColumn> actual = budgetViewColumnRepository
                    .findAllByTableTypeOrderBySortOrder(BudgetViewTable.INCOME);

            // Then
            assertThat(actual).extracting(BudgetViewColumn::getColumnType, BudgetViewColumn::isHidden)
                    .containsExactly(tuple(ReportColumn.DESCRIPTION, false), tuple(ReportColumn.AMOUNT, false),
                            tuple(ReportColumn.CONTACT, false), tuple(ReportColumn.REMARKS, true));
        }
    }
}
