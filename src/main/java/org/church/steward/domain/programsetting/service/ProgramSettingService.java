package org.church.steward.domain.programsetting.service;

import java.time.DayOfWeek;

public interface ProgramSettingService {

    /**
     * 주간 결산 요일을 가져온다. 정한 적이 없으면 기본값(일요일)을 반환하며 저장하지는 않는다.
     *
     * @return 주간 결산 요일
     */
    DayOfWeek getWeeklyClosingDay();

    /**
     * 주간 결산 요일을 저장한다. 처음 저장하면 설정 행을 새로 만든다.
     *
     * @param weeklyClosingDay 주간 결산 요일
     * @throws IllegalArgumentException 요일이 없는 경우
     */
    void updateWeeklyClosingDay(DayOfWeek weeklyClosingDay);
}
