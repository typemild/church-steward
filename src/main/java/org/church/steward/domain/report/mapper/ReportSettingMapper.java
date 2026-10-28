package org.church.steward.domain.report.mapper;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportSetting;
import org.springframework.stereotype.Component;

@Component
public class ReportSettingMapper {

    public ReportSettingDto toDto(ReportSetting setting) {
        return ReportSettingDto.builder()
                .reportType(setting.getReportType())
                .fontFamily(setting.getFontFamily())
                .fontSizeStep(setting.getFontSizeStep())
                .lineSpacing(setting.getLineSpacing())
                .autoFit(setting.isAutoFit())
                .thumbnailsVisible(setting.isThumbnailsVisible())
                .viewMode(setting.getViewMode())
                .zoomMode(setting.getZoomMode())
                .zoomPercent(setting.getZoomPercent())
                .build();
    }
}
