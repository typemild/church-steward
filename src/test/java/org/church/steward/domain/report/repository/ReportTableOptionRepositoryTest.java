package org.church.steward.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportTableOption;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ReportTableOptionRepositoryTest {

    @Autowired
    private ReportTableOptionRepository reportTableOptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("마이그레이션 직후에는 표의 설정 행이 없다")
        void findById_AfterMigration_ReturnsEmpty() {
            // When & Then
            assertThat(reportTableOptionRepository.findById(ReportTemplateTable.WEEKLY_INCOME_STATUS)).isEmpty();
        }

        @Test
        @DisplayName("저장한 막대 표시 여부를 그대로 읽어 온다")
        void findById_Saved_ReadsRateBarVisible() {
            // Given
            reportTableOptionRepository.save(ReportTableOption.builder()
                    .tableType(ReportTemplateTable.WEEKLY_INCOME_STATUS)
                    .rateBarVisible(false)
                    .recordOrder(RecordOrder.FIRST_COLUMN)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            ReportTableOption found = reportTableOptionRepository.findById(ReportTemplateTable.WEEKLY_INCOME_STATUS)
                    .orElseThrow();

            // Then
            assertThat(found.isRateBarVisible()).isFalse();
        }

        @Test
        @DisplayName("저장한 같은 날짜의 순서를 그대로 읽어 온다")
        void findById_Saved_ReadsRecordOrder() {
            // Given
            reportTableOptionRepository.save(ReportTableOption.builder()
                    .tableType(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS)
                    .rateBarVisible(true)
                    .recordOrder(RecordOrder.ENTRY)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            ReportTableOption found = reportTableOptionRepository.findById(ReportTemplateTable.WEEKLY_EXPENSE_RECORDS)
                    .orElseThrow();

            // Then
            assertThat(found.getRecordOrder()).isEqualTo(RecordOrder.ENTRY);
        }

        @Test
        @DisplayName("저장한 일자 표시 여부를 그대로 읽어 온다")
        void findById_Saved_ReadsDateVisible() {
            // Given
            reportTableOptionRepository.save(ReportTableOption.builder()
                    .tableType(ReportTemplateTable.WEEKLY_INCOME_RECORDS)
                    .rateBarVisible(true)
                    .recordOrder(RecordOrder.FIRST_COLUMN)
                    .dateVisible(false)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            ReportTableOption found = reportTableOptionRepository.findById(ReportTemplateTable.WEEKLY_INCOME_RECORDS)
                    .orElseThrow();

            // Then
            assertThat(found.isDateVisible()).isFalse();
        }
    }
}
