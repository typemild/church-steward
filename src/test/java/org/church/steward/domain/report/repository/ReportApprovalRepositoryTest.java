package org.church.steward.domain.report.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ReportApprovalRepositoryTest {

    @Autowired
    private ReportApprovalRepository reportApprovalRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findById 테스트")
    class FindByIdTest {

        @Test
        @DisplayName("마이그레이션 직후에는 결재란 행이 없다")
        void findById_AfterMigration_ReturnsEmpty() {
            // When & Then
            assertThat(reportApprovalRepository.findById(ReportType.WEEKLY_SETTLEMENT)).isEmpty();
        }

        @Test
        @DisplayName("저장한 표시 여부와 칸마다의 직함을 칸 수만큼 그대로 읽어 온다")
        void findById_Saved_ReadsVisibilityAndTitles() {
            // Given
            reportApprovalRepository.save(ReportApproval.builder()
                    .reportType(ReportType.WEEKLY_SETTLEMENT)
                    .visible(false)
                    .titles(Arrays.asList("담당", null, "재정부장", "담임목사"))
                    .build());
            entityManager.flush();
            entityManager.clear();

            // When
            ReportApproval found = reportApprovalRepository.findById(ReportType.WEEKLY_SETTLEMENT)
                    .orElseThrow();

            // Then
            assertThat(found.isVisible()).isFalse();
            assertThat(found.getTitles()).containsExactly("담당", null, "재정부장", "담임목사");
        }
    }
}
