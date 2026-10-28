package org.church.steward.domain.report.dto;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인한 사용자가 보고서 하나에 대해 정한 서식과 미리보기 설정
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ReportSettingDto {
    private ReportType reportType;
    private ReportFontFamily fontFamily;
    /** 글자 크기 조정 단계. 0이 기본 크기다. */
    private int fontSizeStep;
    /** 행 간격(%) */
    private int lineSpacing;
    private boolean autoFit;
    private boolean thumbnailsVisible;
    private ReportViewMode viewMode;
    private ReportZoomMode zoomMode;
    /** 직접 지정한 배율(%) */
    private int zoomPercent;
}
