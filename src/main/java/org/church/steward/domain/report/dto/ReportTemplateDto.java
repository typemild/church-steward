package org.church.steward.domain.report.dto;

import java.util.List;
import java.util.Map;

import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보고서 하나의 양식. 사용자별 서식({@link ReportSettingDto})과 달리 프로그램 전체에 하나이며 모든 사용자가 같은
 * 양식을 쓴다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportTemplateDto {
    private ReportType reportType;
    /** 결재란. 결재란이 없는 보고서이면 null이다. */
    private ReportApprovalDto approval;
    /** 보고서의 표마다 칸의 순서와 숨김. 숨긴 칸도 모두 들어 있다. */
    private Map<ReportTemplateTable, List<ReportColumnDto>> tables;
    /**
     * 현황 표마다 비율 칸의 왼쪽에 막대를 함께 그리는지 여부. 막대를 정할 수 있는
     * 표({@link ReportTemplateTable#hasRateBarOption()})만 들어 있다.
     */
    private Map<ReportTemplateTable, Boolean> rateBarVisible;
}
