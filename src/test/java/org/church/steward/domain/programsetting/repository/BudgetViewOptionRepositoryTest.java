package org.church.steward.domain.programsetting.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.programsetting.entity.BudgetViewOption;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.entity.RecordOrder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BudgetViewOptionRepositoryTest {

    @Autowired
    private BudgetViewOptionRepository budgetViewOptionRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("마이그레이션 직후에는 표의 설정 행이 없다")
        void findById_AfterMigration_ReturnsEmpty() {
            // When & Then
            assertThat(budgetViewOptionRepository.findById(BudgetViewTable.INCOME)).isEmpty();
        }

        @Test
        @DisplayName("저장한 같은 날짜의 순서를 그대로 읽어 온다")
        void findById_Saved_ReadsRecordOrder() {
            // Given
            budgetViewOptionRepository.save(BudgetViewOption.builder()
                    .tableType(BudgetViewTable.EXPENSE)
                    .recordOrder(RecordOrder.ENTRY)
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            BudgetViewOption found = budgetViewOptionRepository.findById(BudgetViewTable.EXPENSE)
                    .orElseThrow();

            // Then
            assertThat(found.getRecordOrder()).isEqualTo(RecordOrder.ENTRY);
        }
    }
}
