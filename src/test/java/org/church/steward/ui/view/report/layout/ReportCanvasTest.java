package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;

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
}
