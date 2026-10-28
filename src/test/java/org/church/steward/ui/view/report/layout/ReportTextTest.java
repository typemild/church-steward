package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Font;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportTextTest {

    private static final Font FONT = ReportStyle.DEFAULT.regular(ReportFonts.SIZE_BODY);
    /** 딱 맞는 폭에서 소수 오차로 결과가 흔들리지 않도록 더하는 여유 */
    private static final double TOLERANCE = 0.01;

    @Nested
    @DisplayName("wrap 테스트")
    class WrapTest {

        @Test
        @DisplayName("폭 안에 들어가는 글자는 한 줄로 둔다")
        void wrap_TextFits_ReturnsSingleLine() {
            // Given
            String text = "십일조";
            double maxWidth = ReportText.width(text, FONT) + TOLERANCE;

            // When
            List<String> lines = ReportText.wrap(text, FONT, maxWidth);

            // Then
            assertThat(lines).containsExactly("십일조");
        }

        @Test
        @DisplayName("띄어쓰기가 없는 긴 글자는 폭에 맞춰 글자 단위로 나눈다")
        void wrap_LongTextWithoutSpace_BreaksByCharacter() {
            // Given
            double maxWidth = ReportText.width("가나다", FONT) + TOLERANCE;

            // When
            List<String> lines = ReportText.wrap("가나다라마바사", FONT, maxWidth);

            // Then
            assertThat(lines).containsExactly("가나다", "라마바", "사");
        }

        @Test
        @DisplayName("띄어쓰기가 있으면 단어 중간이 아니라 띄어쓰기에서 나눈다")
        void wrap_TextWithSpace_BreaksAtSpace() {
            // Given
            double maxWidth = ReportText.width("가나 다", FONT) + TOLERANCE;

            // When
            List<String> lines = ReportText.wrap("가나 다라마", FONT, maxWidth);

            // Then
            assertThat(lines).containsExactly("가나", "다라마");
        }

        @Test
        @DisplayName("글자가 없으면 빈 줄 하나를 반환한다")
        void wrap_NullText_ReturnsSingleEmptyLine() {
            // Given
            String text = null;

            // When
            List<String> lines = ReportText.wrap(text, FONT, 100);

            // Then
            assertThat(lines).containsExactly("");
        }
    }

    @Nested
    @DisplayName("clip 테스트")
    class ClipTest {

        @Test
        @DisplayName("폭을 넘는 글자는 잘라내고 말줄임표를 붙여 폭 안에 들어가게 한다")
        void clip_TextTooWide_EndsWithEllipsisWithinWidth() {
            // Given
            double maxWidth = ReportText.width("가나다라", FONT);

            // When
            String clipped = ReportText.clip("가나다라마바사", FONT, maxWidth);

            // Then
            assertThat(clipped).endsWith("…");
            assertThat(ReportText.width(clipped, FONT)).isLessThanOrEqualTo(maxWidth);
        }

        @Test
        @DisplayName("폭 안에 들어가는 글자는 그대로 둔다")
        void clip_TextFits_ReturnsSameText() {
            // Given
            String text = "가나다";
            double maxWidth = ReportText.width(text, FONT) + TOLERANCE;

            // When
            String clipped = ReportText.clip(text, FONT, maxWidth);

            // Then
            assertThat(clipped).isEqualTo("가나다");
        }
    }
}
