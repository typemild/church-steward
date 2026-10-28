package org.church.steward.domain.report.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSetting;
import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.domain.report.entity.ReportSettingId;
import org.church.steward.domain.report.entity.ReportType;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;
import org.church.steward.domain.report.mapper.ReportSettingMapper;
import org.church.steward.domain.report.repository.ReportSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportSettingServiceImplTest {

    private static final String USER_ID = "user1";
    private static final ReportType TYPE = ReportType.WEEKLY_SETTLEMENT;

    @Mock
    private ReportSettingRepository reportSettingRepository;

    @Spy
    private ReportSettingMapper reportSettingMapper = new ReportSettingMapper();

    @InjectMocks
    private ReportSettingServiceImpl reportSettingService;

    private static ReportSettingDto createSetting() {
        return ReportSettingDto.builder()
                .reportType(TYPE)
                .fontFamily(ReportFontFamily.NANUM_MYEONGJO)
                .fontSizeStep(3)
                .lineSpacing(150)
                .autoFit(true)
                .thumbnailsVisible(true)
                .viewMode(ReportViewMode.SINGLE_PAGE)
                .zoomMode(ReportZoomMode.CUSTOM)
                .zoomPercent(130)
                .build();
    }

    @Nested
    @DisplayName("getReportSetting 테스트")
    class GetReportSettingTest {

        @Test
        @DisplayName("저장된 설정이 있으면 그 설정을 반환한다")
        void getReportSetting_Existing_ReturnsSaved() {
            // Given
            ReportSetting saved = ReportSetting.builder()
                    .userId(USER_ID)
                    .reportType(TYPE)
                    .fontFamily(ReportFontFamily.NANUM_MYEONGJO)
                    .fontSizeStep(-2)
                    .lineSpacing(110)
                    .viewMode(ReportViewMode.SINGLE_PAGE)
                    .zoomMode(ReportZoomMode.FIT_PAGE)
                    .zoomPercent(200)
                    .build();
            when(reportSettingRepository.findById(new ReportSettingId(USER_ID, TYPE))).thenReturn(Optional.of(saved));

            // When
            ReportSettingDto actual = reportSettingService.getReportSetting(USER_ID, TYPE);

            // Then
            assertThat(actual.getFontFamily()).isEqualTo(ReportFontFamily.NANUM_MYEONGJO);
            assertThat(actual.getFontSizeStep()).isEqualTo(-2);
            assertThat(actual.getLineSpacing()).isEqualTo(110);
            assertThat(actual.getZoomMode()).isEqualTo(ReportZoomMode.FIT_PAGE);
        }

        @Test
        @DisplayName("저장된 설정이 없으면 기본 설정(나눔고딕, 기본 크기, 행 간격 130%, 한 페이지에 맞춤 꺼짐, 이어 보기, 폭 맞춤)을 반환한다")
        void getReportSetting_NoSetting_ReturnsDefaults() {
            // Given
            when(reportSettingRepository.findById(new ReportSettingId(USER_ID, TYPE))).thenReturn(Optional.empty());

            // When
            ReportSettingDto actual = reportSettingService.getReportSetting(USER_ID, TYPE);

            // Then
            assertThat(actual.getReportType()).isEqualTo(TYPE);
            assertThat(actual.getFontFamily()).isEqualTo(ReportFontFamily.NANUM_GOTHIC);
            assertThat(actual.getFontSizeStep()).isZero();
            assertThat(actual.getLineSpacing()).isEqualTo(130);
            assertThat(actual.isAutoFit()).isFalse();
            assertThat(actual.isThumbnailsVisible()).isFalse();
            assertThat(actual.getViewMode()).isEqualTo(ReportViewMode.CONTINUOUS);
            assertThat(actual.getZoomMode()).isEqualTo(ReportZoomMode.FIT_WIDTH);
            assertThat(actual.getZoomPercent()).isEqualTo(100);
        }

        @Test
        @DisplayName("저장된 설정이 없어도 기본 설정을 저장하지 않는다")
        void getReportSetting_NoSetting_DoesNotSave() {
            // Given
            when(reportSettingRepository.findById(new ReportSettingId(USER_ID, TYPE))).thenReturn(Optional.empty());

            // When
            reportSettingService.getReportSetting(USER_ID, TYPE);

            // Then
            verify(reportSettingRepository, never()).save(any(ReportSetting.class));
        }
    }

    @Nested
    @DisplayName("updateReportSetting 테스트")
    class UpdateReportSettingTest {

        @Test
        @DisplayName("저장된 설정이 없으면 사용자와 보고서 종류로 새 설정을 만들어 받은 값으로 저장한다")
        void updateReportSetting_NoSetting_CreatesAndSaves() {
            // Given
            ArgumentCaptor<ReportSetting> captor = ArgumentCaptor.forClass(ReportSetting.class);
            when(reportSettingRepository.findById(new ReportSettingId(USER_ID, TYPE))).thenReturn(Optional.empty());

            // When
            reportSettingService.updateReportSetting(USER_ID, createSetting());

            // Then
            verify(reportSettingRepository).save(captor.capture());
            ReportSetting saved = captor.getValue();
            assertThat(saved.getUserId()).isEqualTo(USER_ID);
            assertThat(saved.getReportType()).isEqualTo(TYPE);
            assertThat(saved.getFontFamily()).isEqualTo(ReportFontFamily.NANUM_MYEONGJO);
            assertThat(saved.getFontSizeStep()).isEqualTo(3);
            assertThat(saved.getLineSpacing()).isEqualTo(150);
            assertThat(saved.isAutoFit()).isTrue();
            assertThat(saved.isThumbnailsVisible()).isTrue();
            assertThat(saved.getViewMode()).isEqualTo(ReportViewMode.SINGLE_PAGE);
            assertThat(saved.getZoomMode()).isEqualTo(ReportZoomMode.CUSTOM);
            assertThat(saved.getZoomPercent()).isEqualTo(130);
        }

        @Test
        @DisplayName("저장된 설정이 있으면 그 설정을 받은 값으로 바꿔 저장한다")
        void updateReportSetting_Existing_UpdatesSameEntity() {
            // Given
            ReportSetting existing = ReportSetting.defaults(USER_ID, TYPE);
            ArgumentCaptor<ReportSetting> captor = ArgumentCaptor.forClass(ReportSetting.class);
            when(reportSettingRepository.findById(new ReportSettingId(USER_ID, TYPE)))
                    .thenReturn(Optional.of(existing));

            // When
            reportSettingService.updateReportSetting(USER_ID, createSetting());

            // Then
            verify(reportSettingRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(existing.getLineSpacing()).isEqualTo(150);
        }

        @Test
        @DisplayName("행 간격이 100%보다 작으면 예외가 발생한다")
        void updateReportSetting_LineSpacingTooSmall_ThrowsException() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .lineSpacing(ReportSettingConstraints.LINE_SPACING_MIN - 10)
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("행 간격");
        }

        @Test
        @DisplayName("행 간격이 200%보다 크면 예외가 발생한다")
        void updateReportSetting_LineSpacingTooLarge_ThrowsException() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .lineSpacing(ReportSettingConstraints.LINE_SPACING_MAX + 10)
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("행 간격");
        }

        @Test
        @DisplayName("배율이 25% ~ 400%를 벗어나면 예외가 발생한다")
        void updateReportSetting_ZoomOutOfRange_ThrowsException() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .zoomPercent(ReportSettingConstraints.ZOOM_MAX + 1)
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("배율");
        }

        @Test
        @DisplayName("글자 크기 조정 단계가 상한을 넘으면 예외가 발생한다")
        void updateReportSetting_FontSizeStepOverLimit_ThrowsException() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .fontSizeStep(-(ReportSettingConstraints.FONT_SIZE_STEP_LIMIT + 1))
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("글자 크기");
        }

        @Test
        @DisplayName("글꼴이 빠져 있으면 예외가 발생한다")
        void updateReportSetting_MissingFontFamily_ThrowsException() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .fontFamily(null)
                    .build();

            // When & Then
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("값이 허용 범위를 벗어나면 저장하지 않는다")
        void updateReportSetting_Invalid_DoesNotSave() {
            // Given
            ReportSettingDto setting = createSetting().toBuilder()
                    .lineSpacing(0)
                    .build();

            // When
            assertThatThrownBy(() -> reportSettingService.updateReportSetting(USER_ID, setting))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(reportSettingRepository, never()).save(any(ReportSetting.class));
        }
    }
}
