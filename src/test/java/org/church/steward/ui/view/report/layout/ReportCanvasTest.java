package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportCanvasTest {

    @Nested
    @DisplayName("finish 테스트")
    class FinishTest {

        @Test
        @DisplayName("가장 작은 글자를 셀 때 크기를 조정하지 않는 페이지 번호는 빼고 센다")
        void finish_PageNumber_ExcludedFromSmallestFontSize() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            canvas.title("시험", 13f);

            // When
            ReportDocument document = canvas.finish();

            // Then
            assertThat(document.getSmallestFontSize()).isEqualTo(13f);
        }

        @Test
        @DisplayName("조정을 뺀 가장 작은 글자의 크기로 글자 크기를 조정할 수 있는 범위를 정한다")
        void finish_AdjustedStyle_ReportsRangeFromBaseSize() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withFontSizeStep(4);
            ReportCanvas canvas = new ReportCanvas("시험", style);
            canvas.text("작은 글자", style.regular(7f), ReportAlign.LEFT, canvas.getLeft(), canvas.getY() + 10);
            canvas.title("시험", 13f);

            // When
            ReportDocument document = canvas.finish();

            // Then
            assertThat(document.getSmallestFontSize()).isEqualTo(9f);
            assertThat(document.getSmallestBaseFontSize()).isEqualTo(7f);
            assertThat(document.getMinFontSizeStep()).isEqualTo(-2);
            assertThat(document.getMaxFontSizeStep()).isEqualTo(10);
        }

        @Test
        @DisplayName("문서에 배치한 서식을 남긴다")
        void finish_Style_KeptInDocument() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withLineSpacing(170);
            ReportCanvas canvas = new ReportCanvas("시험", style);

            // When
            ReportDocument document = canvas.finish();

            // Then
            assertThat(document.getStyle()).isEqualTo(style);
        }
    }

    @Nested
    @DisplayName("shrinkingText 테스트")
    class ShrinkingTextTest {

        private ReportElement.Text findText(ReportDocument document, String text) {
            return document.getElements(0)
                    .stream()
                    .filter(element -> element instanceof ReportElement.Text found && found.text()
                            .equals(text))
                    .map(ReportElement.Text.class::cast)
                    .findFirst()
                    .orElseThrow();
        }

        @Test
        @DisplayName("폭을 넘는 글자는 말줄임표 없이 폭에 맞게 글자 크기를 줄인다")
        void shrinkingText_TooWide_ShrinksFontToFit() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            Font font = ReportStyle.DEFAULT.regular(10f);
            double maxWidth = ReportText.width("담임목사", font) / 2;

            // When
            canvas.shrinkingText("담임목사", font, ReportAlign.CENTER, 100, 100, maxWidth);

            // Then
            ReportElement.Text text = findText(canvas.finish(), "담임목사");
            assertThat(ReportText.width(text.text(), text.font())).isLessThanOrEqualTo(maxWidth + 0.01);
        }

        @Test
        @DisplayName("폭에 들어가는 글자는 크기를 바꾸지 않는다")
        void shrinkingText_Fits_KeepsFont() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            Font font = ReportStyle.DEFAULT.regular(10f);

            // When
            canvas.shrinkingText("담당", font, ReportAlign.CENTER, 100, 100, 200);

            // Then
            assertThat(findText(canvas.finish(), "담당").font()
                    .getSize2D()).isEqualTo(10f);
        }

        @Test
        @DisplayName("가장 작은 글자를 셀 때는 줄이기 전의 크기로 센다")
        void shrinkingText_TooWide_CountsOriginalSizeAsSmallest() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            Font font = ReportStyle.DEFAULT.regular(10f);
            canvas.shrinkingText("담임목사", font, ReportAlign.CENTER, 100, 100, ReportText.width("담임목사", font) / 2);

            // When
            ReportDocument document = canvas.finish();

            // Then
            assertThat(document.getSmallestFontSize()).isEqualTo(10f);
        }
    }
}
