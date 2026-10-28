package org.church.steward.ui.view.report.viewer;

import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;

/**
 * 보고서마다 저장하는 미리보기 설정
 *
 * @param thumbnailsVisible 왼쪽의 페이지 목록을 보여주는지 여부
 * @param zoomPercent       직접 지정한 배율(%). 맞춤 방식에서는 쓰지 않는다.
 */
public record ReportViewerState(boolean thumbnailsVisible, ReportViewMode viewMode, ReportZoomMode zoomMode,
        int zoomPercent) {
}
