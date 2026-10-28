package org.church.steward.domain.report.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSetting;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportSettingMapperTest {

    private final ReportSettingMapper reportSettingMapper = new ReportSettingMapper();

    /**
     * 여부 항목을 뺀 모든 항목이 기본값과 다른 설정. 두 여부 항목은 서로 다른 값으로 두어 뒤바뀌어 옮겨지면 드러나게 한다.
     */
    private static ReportSetting setting(boolean autoFit, boolean thumbnailsVisible) {
        return ReportSetting.builder()
                .userId("user1")
                .reportType(ReportType.ANNUAL_SETTLEMENT)
                .fontFamily(ReportFontFamily.NANUM_MYEONGJO)
                .fontSizeStep(-3)
                .lineSpacing(170)
                .autoFit(autoFit)
                .thumbnailsVisible(thumbnailsVisible)
                .viewMode(ReportViewMode.SINGLE_PAGE)
                .zoomMode(ReportZoomMode.CUSTOM)
                .zoomPercent(250)
                .build();
    }

    @Nested
    @DisplayName("toDto 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("보고서 종류, 서식, 미리보기 설정을 모두 그대로 옮긴다")
        void toDto_AllFields_CopiesEveryField() {
            // Given
            ReportSetting setting = setting(true, false);

            // When
            ReportSettingDto result = reportSettingMapper.toDto(setting);

            // Then
            assertThat(result.getReportType()).isEqualTo(ReportType.ANNUAL_SETTLEMENT);
            assertThat(result.getFontFamily()).isEqualTo(ReportFontFamily.NANUM_MYEONGJO);
            assertThat(result.getFontSizeStep()).isEqualTo(-3);
            assertThat(result.getLineSpacing()).isEqualTo(170);
            assertThat(result.isAutoFit()).isTrue();
            assertThat(result.isThumbnailsVisible()).isFalse();
            assertThat(result.getViewMode()).isEqualTo(ReportViewMode.SINGLE_PAGE);
            assertThat(result.getZoomMode()).isEqualTo(ReportZoomMode.CUSTOM);
            assertThat(result.getZoomPercent()).isEqualTo(250);
        }

        @Test
        @DisplayName("한 페이지에 맞춤과 페이지 목록 표시를 서로 섞지 않고 각각 옮긴다")
        void toDto_OppositeFlags_KeepsEachFlag() {
            // Given
            ReportSetting setting = setting(false, true);

            // When
            ReportSettingDto result = reportSettingMapper.toDto(setting);

            // Then
            assertThat(result.isAutoFit()).isFalse();
            assertThat(result.isThumbnailsVisible()).isTrue();
        }
    }
}
