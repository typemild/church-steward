package org.church.steward.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;

import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTableColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ReportTableColumnRepositoryTest {

    @Autowired
    private ReportTableColumnRepository reportTableColumnRepository;

    @Autowired
    private TestEntityManager entityManager;

    private ReportTableColumn createColumn(ReportTemplateTable table, ReportColumn column, int sortOrder,
            boolean hidden) {
        return ReportTableColumn.builder()
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
            List<ReportTableColumn> actual = reportTableColumnRepository
                    .findAllByTableTypeOrderBySortOrder(ReportTemplateTable.WEEKLY_INCOME_RECORDS);

            // Then
            assertThat(actual).isEmpty();
        }

        @Test
        @DisplayName("그 표의 칸만 순서대로 숨김 여부와 함께 읽어 온다")
        void findAllByTableTypeOrderBySortOrder_Saved_ReturnsColumnsOfTableInOrder() {
            // Given
            reportTableColumnRepository.saveAll(List.of(
                    createColumn(ReportTemplateTable.WEEKLY_INCOME_RECORDS, ReportColumn.REMARKS, 3, true),
                    createColumn(ReportTemplateTable.WEEKLY_INCOME_RECORDS, ReportColumn.CONTACT, 0, false),
                    createColumn(ReportTemplateTable.WEEKLY_INCOME_RECORDS, ReportColumn.DESCRIPTION, 2, false),
                    createColumn(ReportTemplateTable.WEEKLY_INCOME_RECORDS, ReportColumn.AMOUNT, 1, false),
                    createColumn(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS, ReportColumn.CONTACT, 0, false)));
            entityManager.flush();
            entityManager.clear();

            // When
            List<ReportTableColumn> actual = reportTableColumnRepository
                    .findAllByTableTypeOrderBySortOrder(ReportTemplateTable.WEEKLY_INCOME_RECORDS);

            // Then
            assertThat(actual).extracting(ReportTableColumn::getColumnType, ReportTableColumn::isHidden)
                    .containsExactly(tuple(ReportColumn.CONTACT, false), tuple(ReportColumn.AMOUNT, false),
                            tuple(ReportColumn.DESCRIPTION, false), tuple(ReportColumn.REMARKS, true));
        }
    }
}
