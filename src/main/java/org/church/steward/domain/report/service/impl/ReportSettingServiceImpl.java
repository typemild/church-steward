package org.church.steward.domain.report.service.impl;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportSetting;
import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.domain.report.entity.ReportSettingId;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.mapper.ReportSettingMapper;
import org.church.steward.domain.report.repository.ReportSettingRepository;
import org.church.steward.domain.report.service.ReportSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 보고서별 사용자 설정. 화면의 편의를 위한 설정이므로 활동 기록을 남기지 않는다(선택한 회계 연도의 저장과 같다).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReportSettingServiceImpl implements ReportSettingService {

    private final ReportSettingRepository reportSettingRepository;
    private final ReportSettingMapper reportSettingMapper;

    @Override
    @Transactional(readOnly = true)
    public ReportSettingDto getReportSetting(String userId, ReportType reportType) {
        ReportSetting setting = reportSettingRepository.findById(new ReportSettingId(userId, reportType))
                .orElseGet(() -> ReportSetting.defaults(userId, reportType));
        return reportSettingMapper.toDto(setting);
    }

    @Override
    public void updateReportSetting(String userId, ReportSettingDto setting) {
        validate(setting);

        ReportSetting entity = reportSettingRepository.findById(new ReportSettingId(userId, setting.getReportType()))
                .orElseGet(() -> ReportSetting.defaults(userId, setting.getReportType()));
        entity.update(setting.getFontFamily(), setting.getFontSizeStep(), setting.getLineSpacing(),
                setting.isAutoFit(), setting.isThumbnailsVisible(), setting.getViewMode(), setting.getZoomMode(),
                setting.getZoomPercent());
        reportSettingRepository.save(entity);
    }

    private static void validate(ReportSettingDto setting) {
        if (setting.getReportType() == null || setting.getFontFamily() == null || setting.getViewMode() == null
                || setting.getZoomMode() == null) {
            throw new IllegalArgumentException("보고서 설정에 빠진 항목이 있습니다.");
        }
        if (Math.abs(setting.getFontSizeStep()) > ReportSettingConstraints.FONT_SIZE_STEP_LIMIT) {
            throw new IllegalArgumentException("글자 크기를 더 바꿀 수 없습니다.");
        }
        if (setting.getLineSpacing() < ReportSettingConstraints.LINE_SPACING_MIN
                || setting.getLineSpacing() > ReportSettingConstraints.LINE_SPACING_MAX) {
            throw new IllegalArgumentException(String.format("행 간격은 %d%% ~ %d%% 사이로 지정해 주세요.",
                    ReportSettingConstraints.LINE_SPACING_MIN, ReportSettingConstraints.LINE_SPACING_MAX));
        }
        if (setting.getZoomPercent() < ReportSettingConstraints.ZOOM_MIN
                || setting.getZoomPercent() > ReportSettingConstraints.ZOOM_MAX) {
            throw new IllegalArgumentException(String.format("배율은 %d%% ~ %d%% 사이로 지정해 주세요.",
                    ReportSettingConstraints.ZOOM_MIN, ReportSettingConstraints.ZOOM_MAX));
        }
    }
}
