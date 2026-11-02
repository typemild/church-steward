package org.church.steward.ui.view.financialrecord.period;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 조회 기간 단위. "기타"는 사용자가 직접 고른 기간이며 정해진 길이가 없으므로 이 클래스의 계산을 쓰지 않고
 * {@link CustomPeriod}로 계산한다.
 */
@Getter
@AllArgsConstructor
public enum PeriodUnit {
    DAY("1일"),
    WEEK("주간"),
    MONTH("월간"),
    QUARTER("분기"),
    YEAR("연간"),
    CUSTOM("기타");

    private static final int MONTHS_PER_QUARTER = 3;

    private final String label;

    /**
     * 기준일이 포함되는 기간의 시작일을 반환한다. 주간 단위는 결산 요일의 다음 날부터 시작해 결산 요일에 끝난다(예: 결산 요일이 일요일이면
     * 월요일 ~ 일요일).
     *
     * @param base             기준일
     * @param weeklyClosingDay 주간 결산 요일. 주간 단위에서만 쓴다.
     * @return 기간의 시작일
     */
    public LocalDate startOf(LocalDate base, DayOfWeek weeklyClosingDay) {
        return switch (this) {
        case DAY -> base;
        case WEEK -> base.with(TemporalAdjusters.previousOrSame(weeklyClosingDay.plus(1)));
        case MONTH -> base.withDayOfMonth(1);
        case QUARTER -> {
            int firstMonth = ((base.getMonthValue() - 1) / MONTHS_PER_QUARTER) * MONTHS_PER_QUARTER + 1;
            yield LocalDate.of(base.getYear(), firstMonth, 1);
        }
        case YEAR -> base.withDayOfYear(1);
        case CUSTOM -> throw unsupportedCustom();
        };
    }

    /**
     * 시작일에 대응하는 기간의 종료일을 반환한다. 주간 단위는 시작일로부터 6일 뒤(결산 요일)다.
     *
     * @param start 기간의 시작일
     * @return 기간의 종료일
     */
    public LocalDate endOf(LocalDate start) {
        return switch (this) {
        case DAY -> start;
        case WEEK -> start.plusDays(6);
        case MONTH -> start.with(TemporalAdjusters.lastDayOfMonth());
        case QUARTER -> start.plusMonths(MONTHS_PER_QUARTER)
                .minusDays(1);
        case YEAR -> start.with(TemporalAdjusters.lastDayOfYear());
        case CUSTOM -> throw unsupportedCustom();
        };
    }

    /**
     * 시작일을 이 단위만큼 이동시킨다.
     *
     * @param start     기간의 시작일
     * @param direction 이동 횟수. 음수이면 과거로, 양수이면 미래로 이동한다.
     * @return 이동된 기간의 시작일
     */
    public LocalDate shift(LocalDate start, int direction) {
        return switch (this) {
        case DAY -> start.plusDays(direction);
        case WEEK -> start.plusWeeks(direction);
        case MONTH -> start.plusMonths(direction);
        case QUARTER -> start.plusMonths((long) direction * MONTHS_PER_QUARTER);
        case YEAR -> start.plusYears(direction);
        case CUSTOM -> throw unsupportedCustom();
        };
    }

    private static UnsupportedOperationException unsupportedCustom() {
        return new UnsupportedOperationException("기타 단위의 기간은 CustomPeriod로 계산해야 합니다.");
    }
}
