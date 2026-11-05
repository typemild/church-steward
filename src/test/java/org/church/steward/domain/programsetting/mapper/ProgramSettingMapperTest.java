package org.church.steward.domain.programsetting.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProgramSettingMapperTest {

    private final ProgramSettingMapper programSettingMapper = new ProgramSettingMapper();

    private static BudgetViewColumn createColumn(ReportColumn column, int sortOrder, boolean hidden) {
        return BudgetViewColumn.builder()
                .tableType(BudgetViewTable.EXPENSE)
                .columnType(column)
                .sortOrder(sortOrder)
                .hidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("toColumnDto 테스트")
    class ToColumnDtoTest {

        @Test
        @DisplayName("숨긴 칸은 칸과 숨김 여부를 그대로 옮긴다")
        void toColumnDto_HiddenColumn_CopiesColumnAndHidden() {
            // Given
            BudgetViewColumn column = createColumn(ReportColumn.REMARKS, 3, true);

            // When
            ReportColumnDto result = programSettingMapper.toColumnDto(column);

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
            BudgetViewColumn column = createColumn(ReportColumn.CONTACT, 2, false);

            // When
            ReportColumnDto result = programSettingMapper.toColumnDto(column);

            // Then
            assertThat(result).isEqualTo(ReportColumnDto.builder()
                    .column(ReportColumn.CONTACT)
                    .hidden(false)
                    .build());
        }
    }
}
