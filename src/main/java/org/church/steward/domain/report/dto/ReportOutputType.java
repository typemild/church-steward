package org.church.steward.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서 출력 방식. 활동 기록에 인쇄와 PDF 저장을 구분해 남기는 데 쓴다.
 */
@Getter
@AllArgsConstructor
public enum ReportOutputType {

    PRINT("인쇄"),
    PDF("PDF 저장");

    private final String label;

}
