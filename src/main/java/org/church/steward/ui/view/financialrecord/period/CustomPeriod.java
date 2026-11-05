package org.church.steward.ui.view.financialrecord.period;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * "기타" 단위의 기간. 사용자가 직접 고른 기간의 시작일과 날 수를 기억하고, 그 날 수만큼씩 이어지는 기간들을 단위 기간으로 삼는다(예:
 * 10월 1일 ~ 10월 10일을 고르면 10월 11일 ~ 10월 20일, 9월 21일 ~ 9월 30일 …).
 *
 * @param start  사용자가 고른 기간의 시작일
 * @param length 기간의 날 수(양 끝 포함, 1 이상)
 */
public record CustomPeriod(LocalDate start, long length) {

    public CustomPeriod {
        if (length < 1) {
            throw new IllegalArgumentException("기간의 날 수는 1 이상이어야 합니다.");
        }
    }

    /**
     * 시작일과 종료일로 기간을 만든다.
     *
     * @param start 시작일
     * @param end   종료일(시작일과 같거나 늦어야 한다)
     */
    public static CustomPeriod of(LocalDate start, LocalDate end) {
        return new CustomPeriod(start, ChronoUnit.DAYS.between(start, end) + 1);
    }

    /**
     * 기준일이 포함되는 기간의 시작일을 반환한다.
     *
     * @param base 기준일
     * @return 기간의 시작일
     */
    public LocalDate startOf(LocalDate base) {
        long index = Math.floorDiv(ChronoUnit.DAYS.between(start, base), length);
        return start.plusDays(index * length);
    }

    /**
     * 시작일에 대응하는 기간의 종료일을 반환한다.
     *
     * @param periodStart 기간의 시작일
     * @return 기간의 종료일
     */
    public LocalDate endOf(LocalDate periodStart) {
        return periodStart.plusDays(length - 1);
    }

    /**
     * 시작일을 기간의 날 수만큼 이동시킨다.
     *
     * @param periodStart 기간의 시작일
     * @param direction   이동 횟수. 음수이면 과거로, 양수이면 미래로 이동한다.
     * @return 이동된 기간의 시작일
     */
    public LocalDate shift(LocalDate periodStart, int direction) {
        return periodStart.plusDays(direction * length);
    }
}
