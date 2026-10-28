package org.church.steward.domain.report.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 보고서의 글꼴. 어느 PC에서나 배치가 같도록 프로그램에 포함한 글꼴만 고를 수 있다.
 */
@Getter
@AllArgsConstructor
public enum ReportFontFamily {

    NANUM_GOTHIC("나눔고딕"),
    NANUM_MYEONGJO("나눔명조");

    private final String label;
}
