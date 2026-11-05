package org.church.steward.domain.programsetting.mapper;

import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.springframework.stereotype.Component;

@Component
public class ProgramSettingMapper {

    public ReportColumnDto toColumnDto(BudgetViewColumn column) {
        return ReportColumnDto.builder()
                .column(column.getColumnType())
                .hidden(column.isHidden())
                .build();
    }
}
