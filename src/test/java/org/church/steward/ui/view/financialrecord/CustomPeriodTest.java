package org.church.steward.ui.view.financialrecord;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.church.steward.ui.view.financialrecord.period.CustomPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CustomPeriod 테스트")
class CustomPeriodTest {

    @Nested
    @DisplayName("of 테스트")
    class OfTest {

        @Test
        @DisplayName("시작일과 종료일을 모두 포함한 날 수를 기간의 길이로 삼는다")
        void of_StartAndEnd_CountsBothEnds() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 1);
            LocalDate end = LocalDate.of(2026, 10, 10);

            // When
            CustomPeriod result = CustomPeriod.of(start, end);

            // Then
            assertThat(result.length()).isEqualTo(10);
        }

        @Test
        @DisplayName("시작일과 종료일이 같으면 길이가 하루인 기간이 된다")
        void of_SameDay_ReturnsOneDayPeriod() {
            // Given
            LocalDate day = LocalDate.of(2026, 10, 1);

            // When
            CustomPeriod result = CustomPeriod.of(day, day);

            // Then
            assertThat(result.length()).isEqualTo(1);
        }

        @Test
        @DisplayName("종료일이 시작일보다 이르면 예외가 발생한다")
        void of_EndBeforeStart_ThrowsException() {
            // Given
            LocalDate start = LocalDate.of(2026, 10, 10);
            LocalDate end = LocalDate.of(2026, 10, 1);

            // When & Then
            assertThatThrownBy(() -> CustomPeriod.of(start, end)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("startOf 테스트")
    class StartOfTest {

        @Test
        @DisplayName("기준일이 고른 기간 안에 있으면 고른 기간의 시작일을 반환한다")
        void startOf_BaseInsidePeriod_ReturnsPeriodStart() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

            // When
            LocalDate result = period.startOf(LocalDate.of(2026, 10, 10));

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 1));
        }

        @Test
        @DisplayName("기준일이 고른 기간 뒤에 있으면 같은 길이로 이어지는 기간 중 기준일이 포함된 기간의 시작일을 반환한다")
        void startOf_BaseAfterPeriod_ReturnsStartOfFollowingPeriod() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

            // When
            LocalDate result = period.startOf(LocalDate.of(2026, 10, 25));

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 21));
        }

        @Test
        @DisplayName("기준일이 고른 기간 앞에 있으면 같은 길이로 거슬러 가는 기간 중 기준일이 포함된 기간의 시작일을 반환한다")
        void startOf_BaseBeforePeriod_ReturnsStartOfPrecedingPeriod() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

            // When
            LocalDate result = period.startOf(LocalDate.of(2026, 9, 30));

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 9, 21));
        }
    }

    @Nested
    @DisplayName("endOf 테스트")
    class EndOfTest {

        @Test
        @DisplayName("시작일로부터 기간의 날 수를 채운 날을 종료일로 반환한다")
        void endOf_PeriodStart_ReturnsLastDayOfLength() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

            // When
            LocalDate result = period.endOf(LocalDate.of(2026, 10, 11));

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 20));
        }
    }

    @Nested
    @DisplayName("shift 테스트")
    class ShiftTest {

        @Test
        @DisplayName("미래 방향으로 기간의 날 수만큼 이동한다")
        void shift_Forward_MovesByLength() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 10));

            // When
            LocalDate result = period.shift(LocalDate.of(2026, 10, 1), 1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 10, 11));
        }

        @Test
        @DisplayName("과거 방향으로 기간의 날 수만큼 이동하며 달이 바뀌어도 날 수로 센다")
        void shift_BackwardAcrossMonth_MovesByLength() {
            // Given
            CustomPeriod period = CustomPeriod.of(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 3));

            // When
            LocalDate result = period.shift(LocalDate.of(2026, 3, 1), -1);

            // Then
            assertThat(result).isEqualTo(LocalDate.of(2026, 2, 26));
        }
    }
}
