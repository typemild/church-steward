package org.church.steward.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서 미리보기의 보기 유형
 */
@Getter
@AllArgsConstructor
public enum ReportViewMode {

    /** 모든 페이지를 세로로 늘어놓고 스크롤해서 본다. */
    CONTINUOUS("이어 보기"),
    /** 한 번에 한 페이지만 보고 페이지 이동으로 넘긴다. */
    SINGLE_PAGE("페이지별 보기");

    private final String label;
}
