package org.church.steward.domain.report.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTableColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportTemplateMapperTest {

    private final ReportTemplateMapper reportTemplateMapper = new ReportTemplateMapper();

    private static ReportApproval createApproval(boolean visible, String... titles) {
        return ReportApproval.builder()
                .reportType(ReportType.WEEKLY_SETTLEMENT)
                .visible(visible)
                .titles(Arrays.asList(titles))
                .build();
    }

    private static ReportTableColumn createColumn(ReportColumn column, int sortOrder, boolean hidden) {
        return ReportTableColumn.builder()
                .tableType(ReportTemplateTable.WEEKLY_INCOME_RECORDS)
                .columnType(column)
                .sortOrder(sortOrder)
                .hidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("toApprovalDto 테스트")
    class ToApprovalDtoTest {

        @Test
        @DisplayName("표시 여부와 칸 수만큼의 직함을 칸 순서대로 옮긴다")
        void toApprovalDto_Titles_CopiesVisibleAndTitlesInOrder() {
            // Given
            ReportApproval approval = createApproval(false, "담당", "회계", "담임목사");

            // When
            ReportApprovalDto result = reportTemplateMapper.toApprovalDto(approval);

            // Then
            assertThat(result.isVisible()).isFalse();
            assertThat(result.getTitles()).containsExactly("담당", "회계", "담임목사");
        }

        @Test
        @DisplayName("빈 직함은 칸 수에서 빠지지 않고 null로 옮긴다")
        void toApprovalDto_BlankTitles_KeepsNullTitles() {
            // Given
            ReportApproval approval = createApproval(true, null, "회계", null);

            // When
            ReportApprovalDto result = reportTemplateMapper.toApprovalDto(approval);

            // Then
            assertThat(result.isVisible()).isTrue();
            assertThat(result.getTitles()).containsExactly(null, "회계", null);
        }
    }

    @Nested
    @DisplayName("toColumnDto 테스트")
    class ToColumnDtoTest {

        @Test
        @DisplayName("숨긴 칸은 칸과 숨김 여부를 그대로 옮긴다")
        void toColumnDto_HiddenColumn_CopiesColumnAndHidden() {
            // Given
            ReportTableColumn column = createColumn(ReportColumn.REMARKS, 3, true);

            // When
            ReportColumnDto result = reportTemplateMapper.toColumnDto(column);

            // Then
            assertThat(result).isEqualTo(ReportColumnDto.builder()
                    .column(ReportColumn.REMARKS)
                    .hidden(true)
                    .build());
        }

        @Test
        @DisplayName("보이는 칸은 숨기지 않은 칸으로 옮긴다")
        void toColumnDto_ShownColumn_CopiesNotHidden() {
            // Given
            ReportTableColumn column = createColumn(ReportColumn.CONTACT, 0, false);

            // When
            ReportColumnDto result = reportTemplateMapper.toColumnDto(column);

            // Then
            assertThat(result).isEqualTo(ReportColumnDto.builder()
                    .column(ReportColumn.CONTACT)
                    .hidden(false)
                    .build());
        }
    }
}
