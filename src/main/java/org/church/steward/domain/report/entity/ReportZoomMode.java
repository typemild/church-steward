package org.church.steward.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서 미리보기의 배율을 정하는 방식
 */
@Getter
@AllArgsConstructor
public enum ReportZoomMode {

    /** 페이지의 폭이 미리보기 영역의 폭에 맞도록 배율을 정한다. */
    FIT_WIDTH("폭 맞춤"),
    /** 페이지 전체가 미리보기 영역에 들어오도록 배율을 정한다. */
    FIT_PAGE("페이지 맞춤"),
    /** 사용자가 지정한 배율을 쓴다. */
    CUSTOM("직접 지정");

    private final String label;
}
