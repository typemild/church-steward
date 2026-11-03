package org.church.steward.ui.view.report.layout;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportTableTest {

    private static List<String> textsOf(ReportDocument document) {
        List<String> texts = new ArrayList<>();
        for (ReportElement element : document.getElements(0)) {
            if (element instanceof ReportElement.Text text) {
                texts.add(text.text());
            }
        }
        return texts;
    }

    @Nested
    @DisplayName("render 테스트")
    class RenderTest {

        @Test
        @DisplayName("폭이 정해진 열에 내용이 한 줄로 들어가지 않으면 줄을 바꾸지 않고 열을 넓힌다")
        void render_FixedColumnTooNarrow_WidensColumn() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("금액", 10, ReportAlign.RIGHT));
            table.addRow(RowStyle.NORMAL, 0, "헌금", "1,234,567,890");

            // When
            table.render(canvas);

            // Then
            assertThat(textsOf(canvas.finish())).contains("1,234,567,890");
        }

        @Test
        @DisplayName("막대를 곁들인 칸은 오른쪽의 글자 자리에 열의 정렬대로 글자를 적고 나머지 폭에 막대를 그린다")
        void render_BarTextCell_DrawsBarLeftOfTextArea() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            double textWidth = 40;
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("비율", 0, ReportAlign.CENTER));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.barText(0.5, "50.0%", textWidth));

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            ReportDocument document = canvas.finish();
            ReportElement.Text text = findText(document, "50.0%");
            double textAreaLeft = canvas.getLeft() + 200 - 3 - textWidth;
            double textCenter = text.x() + ReportText.width(text.text(), text.font()) / 2;
            assertThat(textCenter).isCloseTo(textAreaLeft + textWidth / 2, within(1e-6));
            assertThat(document.getElements(0)).filteredOn(element -> element instanceof ReportElement.Rect rect
                    && UIColors.REPORT_BAR.equals(rect.stroke()))
                    .singleElement()
                    .satisfies(element -> assertThat(((ReportElement.Rect) element).x()
                            + ((ReportElement.Rect) element).width()).isLessThan(textAreaLeft));
        }

        @Test
        @DisplayName("막대를 곁들인 칸의 열은 글자 자리와 막대의 최소 폭이 들어가도록 넓어진다")
        void render_BarTextCellNarrowColumn_WidensColumn() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("비율", 10, ReportAlign.CENTER));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.text("헌금"), ReportTable.barText(0.5, "50.0%", 40));

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            assertThat(canvas.finish()
                    .getElements(0)).anyMatch(
                            element -> element instanceof ReportElement.Rect rect
                                    && UIColors.REPORT_BAR.equals(rect.stroke()) && rect.width() > 0);
        }
    }

    private static ReportElement.Text findText(ReportDocument document, String text) {
        return document.getElements(0)
                .stream()
                .filter(element -> element instanceof ReportElement.Text found && found.text()
                        .equals(text))
                .map(ReportElement.Text.class::cast)
                .findFirst()
                .orElseThrow();
    }

    @Nested
    @DisplayName("measureHeight 테스트")
    class MeasureHeightTest {

        @Test
        @DisplayName("행의 높이는 글자 높이에 행 간격을 곱한 값이다")
        void measureHeight_LineSpacing_UsesSpacingForEveryRow() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT.withLineSpacing(150);
            ReportTable table = new ReportTable(style, 9f,
                    new Column("항목", 0, ReportAlign.LEFT));
            table.addRow(RowStyle.NORMAL, 0, "헌금");
            table.addRow(RowStyle.NORMAL, 0, "감사");

            // When
            double height = table.measureHeight(200);

            // Then
            double header = ReportText.lineHeight(style.bold(9f)) * 1.5;
            double row = ReportText.lineHeight(style.regular(9f)) * 1.5;
            assertThat(height).isCloseTo(header + row * 2, within(1e-6));
        }
    }
}
