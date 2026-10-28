package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Font;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportStyleTest {

    @Nested
    @DisplayName("size 테스트")
    class SizeTest {

        @Test
        @DisplayName("조정 단계마다 제목과 본문 모두에 0.5pt씩 더한다")
        void size_PositiveStep_AddsHalfPointPerStep() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(3);

            // When & Then
            assertThat(style.size(7f)).isEqualTo(8.5f);
            assertThat(style.size(13f)).isEqualTo(14.5f);
        }

        @Test
        @DisplayName("음수 단계는 모든 글자에서 0.5pt씩 뺀다")
        void size_NegativeStep_SubtractsHalfPointPerStep() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(-2);

            // When & Then
            assertThat(style.size(7f)).isEqualTo(6f);
        }
    }

    @Nested
    @DisplayName("lineHeight 테스트")
    class LineHeightTest {

        @Test
        @DisplayName("한 줄의 높이는 글자 높이에 행 간격을 곱한 값이다")
        void lineHeight_Spacing_MultipliesTextHeight() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withLineSpacing(150);
            Font font = style.regular(10f);

            // When
            double lineHeight = style.lineHeight(font);

            // Then
            assertThat(lineHeight).isCloseTo(ReportText.lineHeight(font) * 1.5, within(1e-9));
        }
    }

    @Nested
    @DisplayName("baselineOffset 테스트")
    class BaselineOffsetTest {

        @Test
        @DisplayName("늘어난 줄 높이는 글자 위아래에 반씩 나눈다")
        void baselineOffset_Spacing_SplitsExtraHeight() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withLineSpacing(200);
            Font font = style.regular(10f);

            // When
            double offset = style.baselineOffset(font);

            // Then
            assertThat(offset).isCloseTo(ReportText.lineHeight(font) / 2 + ReportText.ascent(font), within(1e-9));
        }
    }

    @Nested
    @DisplayName("regular 테스트")
    class RegularTest {

        @Test
        @DisplayName("고른 글꼴로 조정한 크기의 글꼴을 만든다")
        void regular_Myeongjo_UsesMyeongjoFont() {
            // Given
            ReportStyle style = new ReportStyle(ReportFontFamily.NANUM_MYEONGJO, 2, 130);

            // When
            Font font = style.regular(8f);

            // Then
            assertThat(font.getPSName()).contains("NanumMyeongjo");
            assertThat(font.getSize2D()).isEqualTo(9f);
        }
    }

    @Nested
    @DisplayName("minFontSizeStep 테스트")
    class MinFontSizeStepTest {

        @Test
        @DisplayName("가장 작은 글자가 6pt보다 작아지지 않는 단계까지 줄일 수 있다")
        void minFontSizeStep_SmallestSevenPoints_ReturnsMinusTwo() {
            // When & Then
            assertThat(ReportStyle.minFontSizeStep(7f)).isEqualTo(-2);
            assertThat(ReportStyle.minFontSizeStep(7.5f)).isEqualTo(-3);
        }
    }

    @Nested
    @DisplayName("maxFontSizeStep 테스트")
    class MaxFontSizeStepTest {

        @Test
        @DisplayName("가장 작은 글자가 12pt보다 커지지 않는 단계까지 키울 수 있다")
        void maxFontSizeStep_SmallestSevenPoints_ReturnsTen() {
            // When & Then
            assertThat(ReportStyle.maxFontSizeStep(7f)).isEqualTo(10);
            assertThat(ReportStyle.maxFontSizeStep(8f)).isEqualTo(8);
        }
    }
}
