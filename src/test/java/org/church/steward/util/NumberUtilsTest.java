package org.church.steward.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class NumberUtilsTest {

    @Nested
    @DisplayName("toKoreanAmount 테스트")
    class ToKoreanAmountTest {

        @Test
        @DisplayName("0은 0원으로 변환한다")
        void toKoreanAmount_zero_returnsZeroWon() {
            // Given
            long amount = 0L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("0원");
        }

        @Test
        @DisplayName("만 단위 미만의 금액은 천 단위 쉼표만 붙여 변환한다")
        void toKoreanAmount_belowTenThousand_returnsNumberWithComma() {
            // Given
            long amount = 3_000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("3,000원");
        }

        @Test
        @DisplayName("만 단위와 그 아래 금액을 함께 적는다")
        void toKoreanAmount_tenThousandsWithRest_returnsBothUnits() {
            // Given
            long amount = 53_000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("5만 3,000원");
        }

        @Test
        @DisplayName("만 단위 아래가 0이면 단위 뒤에 바로 원을 붙인다")
        void toKoreanAmount_noRest_appendsWonRightAfterUnit() {
            // Given
            long amount = 10_000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("1만원");
        }

        @Test
        @DisplayName("조, 억, 만 단위를 큰 단위부터 차례로 적고 값이 0인 단위는 건너뛴다")
        void toKoreanAmount_largeAmount_writesUnitsInOrderSkippingZeroUnits() {
            // Given
            long amount = 1_234_0000_5678_0000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("1234조 5,678만원");
        }

        @Test
        @DisplayName("음수는 절댓값을 변환한 표기 앞에 -를 붙인다")
        void toKoreanAmount_negative_prependsMinusSign() {
            // Given
            long amount = -53_000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("-5만 3,000원");
        }

        @Test
        @DisplayName("만 단위 아래가 0인 음수도 단위 뒤에 바로 원을 붙인다")
        void toKoreanAmount_negativeWithoutRest_appendsWonRightAfterUnit() {
            // Given
            long amount = -30_000L;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("-3만원");
        }

        @Test
        @DisplayName("long의 최솟값도 넘치지 않고 음수로 변환한다")
        void toKoreanAmount_longMinValue_returnsNegativeAmount() {
            // Given
            long amount = Long.MIN_VALUE;

            // When
            String result = NumberUtils.toKoreanAmount(amount);

            // Then
            assertThat(result).isEqualTo("-922경 3372조 368억 5,477만 5,808원");
        }
    }
}
