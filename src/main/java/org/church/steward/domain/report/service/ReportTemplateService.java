package org.church.steward.domain.report.service;

import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportType;

public interface ReportTemplateService {

    /**
     * 보고서의 양식(결재란, 표마다 칸의 순서와 숨김)을 가져온다. 정한 적이 없는 부분은 기본 양식이며 저장하지는 않는다.
     *
     * @param reportType 보고서 종류
     * @return 보고서의 양식. 양식에서 정할 것이 없는 보고서이면 결재란이 null이고 표가 없다.
     */
    ReportTemplateDto getReportTemplate(ReportType reportType);

    /**
     * 보고서의 양식을 저장한다. 처음 저장하면 행을 새로 만든다. 프로그램 전체 설정이며 활동 기록은 남기지 않는다.
     *
     * @param template 보고서의 양식. 결재란이 있는 보고서이면 결재란과, 보고서의 모든 표를 담아야 한다.
     * @throws IllegalArgumentException 빠진 항목이 있거나, 결재란의 칸 수나 직함의 길이가 범위를 벗어나거나, 표의
     *                                  칸 구성이나 순서, 숨김이 규칙에 맞지 않는 경우
     */
    void updateReportTemplate(ReportTemplateDto template);
}
