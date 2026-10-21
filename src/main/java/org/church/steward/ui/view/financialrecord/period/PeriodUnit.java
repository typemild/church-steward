package org.church.steward.ui.view.financialrecord.period;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 조회 기간 단위
 */
@Getter
@AllArgsConstructor
public enum PeriodUnit {
    DAY("1일"),
    WEEK("주간"),
    MONTH("월간"),
    QUARTER("분기"),
    YEAR("연간");

    private static final int MONTHS_PER_QUARTER = 3;

    private final String label;

    /**
     * 기준일이 포함되는 기간의 시작일을 반환한다.
     * 주간 단위는 일요일부터 시작한다.
     *
     * @param base 기준일
     * @return 기간의 시작일
     */
    public LocalDate startOf(LocalDate base) {
        return switch (this) {
        case DAY -> base;
        case WEEK -> base.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        case MONTH -> base.withDayOfMonth(1);
        case QUARTER -> {
            int firstMonth = ((base.getMonthValue() - 1) / MONTHS_PER_QUARTER) * MONTHS_PER_QUARTER + 1;
            yield LocalDate.of(base.getYear(), firstMonth, 1);
        }
        case YEAR -> base.withDayOfYear(1);
        };
    }

    /**
     * 시작일에 대응하는 기간의 종료일을 반환한다.
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
        };
    }
}
