package org.church.steward.domain.programsetting.entity;

import java.time.DayOfWeek;

import org.church.steward.domain.report.entity.RecordOrder;

/**
 * 프로그램 설정 값의 기본값과 칼럼 길이
 */
public final class ProgramSettingConstraints {

    /** 설정은 프로그램 전체에 한 행뿐이며 이 ID로 저장한다. */
    public static final long SETTING_ID = 1L;

    /** 주간 결산 요일의 기본값. 주간은 월요일부터 일요일까지가 된다. */
    public static final DayOfWeek DEFAULT_WEEKLY_CLOSING_DAY = DayOfWeek.SUNDAY;

    /** 예산별 보기에서 내역의 정렬 순서의 기본값 */
    public static final RecordOrder DEFAULT_RECORD_ORDER = RecordOrder.FIRST_COLUMN;

    /** 예산별 보기에서 첫 번째 칸 앞에 일자를 붙여 표시하는지 여부의 기본값 */
    public static final boolean DEFAULT_DATE_VISIBLE = true;

    /** enum 칼럼의 길이 */
    public static final int ENUM_MAX_LENGTH = 10;

    private ProgramSettingConstraints() {
    }

}
