package org.church.steward.domain.report.mapper;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportTableColumn;
import org.springframework.stereotype.Component;

@Component
public class ReportTemplateMapper {

    public ReportApprovalDto toApprovalDto(ReportApproval approval) {
        return ReportApprovalDto.builder()
                .visible(approval.isVisible())
                .titles(approval.getTitles())
                .build();
    }

    public ReportColumnDto toColumnDto(ReportTableColumn column) {
        return ReportColumnDto.builder()
                .column(column.getColumnType())
                .hidden(column.isHidden())
                .build();
    }
}
