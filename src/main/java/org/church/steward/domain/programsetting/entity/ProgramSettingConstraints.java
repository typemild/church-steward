package org.church.steward.domain.programsetting.entity;

import java.time.DayOfWeek;

/**
 * 프로그램 설정 값의 기본값과 칼럼 길이
 */
public final class ProgramSettingConstraints {

    /** 설정은 프로그램 전체에 한 행뿐이며 이 ID로 저장한다. */
    public static final long SETTING_ID = 1L;

    /** 주간 결산 요일의 기본값. 주간은 월요일부터 일요일까지가 된다. */
    public static final DayOfWeek DEFAULT_WEEKLY_CLOSING_DAY = DayOfWeek.SUNDAY;

    /** enum 칼럼의 길이 */
    public static final int ENUM_MAX_LENGTH = 10;

    private ProgramSettingConstraints() {
    }

}
