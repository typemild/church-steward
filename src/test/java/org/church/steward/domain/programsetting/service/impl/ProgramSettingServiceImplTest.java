package org.church.steward.domain.programsetting.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.Optional;

import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.domain.programsetting.repository.ProgramSettingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramSettingServiceImplTest {

    @Mock
    private ProgramSettingRepository programSettingRepository;

    @InjectMocks
    private ProgramSettingServiceImpl programSettingService;

    private static ProgramSetting createSetting(DayOfWeek weeklyClosingDay) {
        return ProgramSetting.builder()
                .id(ProgramSettingConstraints.SETTING_ID)
                .weeklyClosingDay(weeklyClosingDay)
                .build();
    }

    @Nested
    @DisplayName("getWeeklyClosingDay 테스트")
    class GetWeeklyClosingDayTest {

        @Test
        @DisplayName("저장된 설정이 있으면 그 결산 요일을 반환한다")
        void getWeeklyClosingDay_Existing_ReturnsSaved() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID))
                    .thenReturn(Optional.of(createSetting(DayOfWeek.SATURDAY)));

            // When
            DayOfWeek actual = programSettingService.getWeeklyClosingDay();

            // Then
            assertThat(actual).isEqualTo(DayOfWeek.SATURDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없으면(기존 데이터나 과거 버전의 백업) 기본값인 일요일을 반환한다")
        void getWeeklyClosingDay_NotSaved_ReturnsSunday() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());

            // When
            DayOfWeek actual = programSettingService.getWeeklyClosingDay();

            // Then
            assertThat(actual).isEqualTo(DayOfWeek.SUNDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없어도 기본값을 저장하지는 않는다")
        void getWeeklyClosingDay_NotSaved_DoesNotSave() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());

            // When
            programSettingService.getWeeklyClosingDay();

            // Then
            verify(programSettingRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("updateWeeklyClosingDay 테스트")
    class UpdateWeeklyClosingDayTest {

        @Test
        @DisplayName("저장된 설정이 있으면 그 설정의 결산 요일을 바꿔 저장한다")
        void updateWeeklyClosingDay_Existing_UpdatesSetting() {
            // Given
            ProgramSetting existing = createSetting(DayOfWeek.SUNDAY);
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID))
                    .thenReturn(Optional.of(existing));
            ArgumentCaptor<ProgramSetting> captor = ArgumentCaptor.forClass(ProgramSetting.class);

            // When
            programSettingService.updateWeeklyClosingDay(DayOfWeek.SATURDAY);

            // Then
            verify(programSettingRepository).save(captor.capture());
            assertThat(captor.getValue()).isSameAs(existing);
            assertThat(captor.getValue()
                    .getWeeklyClosingDay()).isEqualTo(DayOfWeek.SATURDAY);
        }

        @Test
        @DisplayName("저장된 설정이 없으면 정해진 ID로 설정을 새로 만들어 저장한다")
        void updateWeeklyClosingDay_NotSaved_CreatesSetting() {
            // Given
            when(programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)).thenReturn(Optional.empty());
            ArgumentCaptor<ProgramSetting> captor = ArgumentCaptor.forClass(ProgramSetting.class);

            // When
            programSettingService.updateWeeklyClosingDay(DayOfWeek.WEDNESDAY);

            // Then
            verify(programSettingRepository).save(captor.capture());
            assertThat(captor.getValue()
                    .getId()).isEqualTo(ProgramSettingConstraints.SETTING_ID);
            assertThat(captor.getValue()
                    .getWeeklyClosingDay()).isEqualTo(DayOfWeek.WEDNESDAY);
        }

        @Test
        @DisplayName("요일이 없으면 예외를 던진다")
        void updateWeeklyClosingDay_Null_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> programSettingService.updateWeeklyClosingDay(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("주간 결산 요일을 골라 주세요.");
        }

        @Test
        @DisplayName("요일이 없으면 저장하지 않는다")
        void updateWeeklyClosingDay_Null_DoesNotSave() {
            // When
            assertThatThrownBy(() -> programSettingService.updateWeeklyClosingDay(null))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            verify(programSettingRepository, never()).save(any());
        }
    }
}
