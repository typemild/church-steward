package org.church.steward.domain.programsetting.controller;

import java.time.DayOfWeek;

import org.church.steward.domain.programsetting.service.ProgramSettingService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

/**
 * 프로그램 전체 설정. 관리자뿐 아니라 모든 사용자가 조회하고 바꿀 수 있다.
 */
@UiController
@RequiredArgsConstructor
public class ProgramSettingController {

    private final ProgramSettingService programSettingService;

    public DayOfWeek getWeeklyClosingDay() {
        return programSettingService.getWeeklyClosingDay();
    }

    public void updateWeeklyClosingDay(DayOfWeek weeklyClosingDay) {
        programSettingService.updateWeeklyClosingDay(weeklyClosingDay);
    }
}
