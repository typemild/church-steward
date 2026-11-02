package org.church.steward.domain.programsetting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;

import org.church.steward.domain.programsetting.service.ProgramSettingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramSettingControllerTest {

    @Mock
    private ProgramSettingService programSettingService;

    @InjectMocks
    private ProgramSettingController programSettingController;

    @Nested
    @DisplayName("getWeeklyClosingDay 테스트")
    class GetWeeklyClosingDayTest {

        @Test
        @DisplayName("서비스가 반환한 주간 결산 요일을 그대로 반환한다")
        void getWeeklyClosingDay_Called_ReturnsServiceResult() {
            // Given
            when(programSettingService.getWeeklyClosingDay()).thenReturn(DayOfWeek.SATURDAY);

            // When
            DayOfWeek actual = programSettingController.getWeeklyClosingDay();

            // Then
            assertThat(actual).isEqualTo(DayOfWeek.SATURDAY);
        }
    }

    @Nested
    @DisplayName("updateWeeklyClosingDay 테스트")
    class UpdateWeeklyClosingDayTest {

        @Test
        @DisplayName("권한 확인 없이 고른 주간 결산 요일을 서비스에 그대로 넘긴다")
        void updateWeeklyClosingDay_Called_DelegatesToService() {
            // When
            programSettingController.updateWeeklyClosingDay(DayOfWeek.MONDAY);

            // Then
            verify(programSettingService).updateWeeklyClosingDay(DayOfWeek.MONDAY);
        }
    }
}
