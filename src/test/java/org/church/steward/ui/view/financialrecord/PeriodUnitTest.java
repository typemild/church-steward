package org.church.steward.ui.view.financialrecord;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.church.steward.ui.view.financialrecord.period.PeriodUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("PeriodUnit 테스트")
class PeriodUnitTest {

    @Nested
    @DisplayName("startOf 테스트")
    class StartOfTest {

        @Test
        @DisplayName("1일 단위는 기준일을 그대로 시작일로 반환한다")
        void startOf_Day_ReturnsBaseDate() {
            // Given
            LocalDate base = LocalDate.of(2026, 10, 6);

            // When
            LocalDate result = PeriodUnit.DAY.startOf(base);

            // Then
            assertThat(result).isEqualTo(base);
        }

        @Test
        @DisplayName("주간 단위는 기준일이 포함된 주의 일요일을 시작일로 반환한다")
        void startOf_WeekWithWeekday_ReturnsPreviousSunday() {
            // Given : 2026-10-06은 화요일이다.
            LocalDate tuesday = LocalDate.of(2026, 10, 6);

            // When
            LocalDate result = PeriodUnit.WEEK.startOf(tuesday);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 4));
        }

        @Test
        @DisplayName("주간 단위는 기준일이 일요일이면 그 날을 시작일로 반환한다")
        void startOf_WeekWithSunday_ReturnsSameDay() {
            // Given
            LocalDate sunday = LocalDate.of(2026, 10, 4);

            // When
            LocalDate result = PeriodUnit.WEEK.startOf(sunday);

            // Then
            assertThat(result).isEqualTo(sunday);
        }

        @Test
        @DisplayName("월간 단위는 기준일이 포함된 달의 1일을 시작일로 반환한다")
        void startOf_Month_ReturnsFirstDayOfMonth() {
            // Given
            LocalDate base = LocalDate.of(2024, 2, 15);

            // When
            LocalDate result = PeriodUnit.MONTH.startOf(base);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2024, 2, 1));
        }

        @Test
        @DisplayName("분기 단위는 기준일이 포함된 분기의 첫날을 시작일로 반환한다")
        void startOf_Quarter_ReturnsFirstDayOfQuarter() {
            // Given
            LocalDate base = LocalDate.of(2026, 5, 20);

            // When
            LocalDate result = PeriodUnit.QUARTER.startOf(base);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("분기 단위는 12월 31일을 4분기로 보고 10월 1일을 시작일로 반환한다")
        void startOf_QuarterWithLastDayOfYear_ReturnsFirstDayOfFourthQuarter() {
            // Given
            LocalDate base = LocalDate.of(2026, 12, 31);

            // When
            LocalDate result = PeriodUnit.QUARTER.startOf(base);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1));
        }

        @Test
        @DisplayName("연간 단위는 기준일이 포함된 해의 1월 1일을 시작일로 반환한다")
        void startOf_Year_ReturnsFirstDayOfYear() {
            // Given
            LocalDate base = LocalDate.of(2026, 10, 6);

            // When
            LocalDate result = PeriodUnit.YEAR.startOf(base);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 1, 1));
        }
    }

    @Nested
    @DisplayName("endOf 테스트")
    class EndOfTest {

        @Test
        @DisplayName("1일 단위는 시작일을 그대로 종료일로 반환한다")
        void endOf_Day_ReturnsStartDate() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 6);

            // When
            LocalDate result = PeriodUnit.DAY.endOf(start);

            // Then
            assertThat(result).isEqualTo(start);
        }

        @Test
        @DisplayName("주간 단위는 시작일인 일요일로부터 6일 뒤인 토요일을 종료일로 반환한다")
        void endOf_Week_ReturnsSaturday() {
            // Given
            LocalDate sunday = LocalDate.of(2026, 10, 4);

            // When
            LocalDate result = PeriodUnit.WEEK.endOf(sunday);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 10));
        }

        @Test
        @DisplayName("월간 단위는 윤년 2월의 말일인 29일을 종료일로 반환한다")
        void endOf_MonthInLeapYear_ReturnsLastDayOfMonth() {
            // Given
            LocalDate start = LocalDate.of(2024, 2, 1);

            // When
            LocalDate result = PeriodUnit.MONTH.endOf(start);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2024, 2, 29));
        }

        @Test
        @DisplayName("분기 단위는 분기의 마지막 날을 종료일로 반환한다")
        void endOf_Quarter_ReturnsLastDayOfQuarter() {
            // Given
            LocalDate start = LocalDate.of(2026, 4, 1);

            // When
            LocalDate result = PeriodUnit.QUARTER.endOf(start);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 6, 30));
        }

        @Test
        @DisplayName("연간 단위는 12월 31일을 종료일로 반환한다")
        void endOf_Year_ReturnsLastDayOfYear() {
            // Given
            LocalDate start = LocalDate.of(2026, 1, 1);

            // When
            LocalDate result = PeriodUnit.YEAR.endOf(start);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 12, 31));
        }
    }

    @Nested
    @DisplayName("shift 테스트")
    class ShiftTest {

        @Test
        @DisplayName("1일 단위는 과거 방향으로 하루 이동한다")
        void shift_DayBackward_MovesOneDayBefore() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 1);

            // When
            LocalDate result = PeriodUnit.DAY.shift(start, -1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 9, 30));
        }

        @Test
        @DisplayName("주간 단위는 과거 방향으로 한 주 이동한다")
        void shift_WeekBackward_MovesOneWeekBefore() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 4);

            // When
            LocalDate result = PeriodUnit.WEEK.shift(start, -1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 9, 27));
        }

        @Test
        @DisplayName("월간 단위는 1월에서 과거 방향으로 이동하면 전년도 12월이 된다")
        void shift_MonthBackwardFromJanuary_MovesToDecemberOfPreviousYear() {
            // Given
            LocalDate start = LocalDate.of(2026, 1, 1);

            // When
            LocalDate result = PeriodUnit.MONTH.shift(start, -1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2025, 12, 1));
        }

        @Test
        @DisplayName("분기 단위는 4분기에서 미래 방향으로 이동하면 다음 해 1분기가 된다")
        void shift_QuarterForwardFromFourthQuarter_MovesToFirstQuarterOfNextYear() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 1);

            // When
            LocalDate result = PeriodUnit.QUARTER.shift(start, 1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2027, 1, 1));
        }

        @Test
        @DisplayName("연간 단위는 미래 방향으로 한 해 이동한다")
        void shift_YearForward_MovesOneYearAfter() {
            // Given
            LocalDate start = LocalDate.of(2026, 1, 1);

            // When
            LocalDate result = PeriodUnit.YEAR.shift(start, 1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2027, 1, 1));
        }
    }
}
