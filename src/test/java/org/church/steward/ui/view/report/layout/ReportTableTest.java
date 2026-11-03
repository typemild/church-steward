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

        @Test
        @DisplayName("정렬을 정한 글자 칸은 열의 정렬 대신 그 정렬로 적는다")
        void render_TextCellWithAlign_OverridesColumnAlign() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("증감액", 0, ReportAlign.RIGHT));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.text("신규", ReportAlign.CENTER));

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            ReportElement.Text text = findText(canvas.finish(), "신규");
            assertThat(text.x() + ReportText.width(text.text(), text.font()) / 2).isCloseTo(canvas.getLeft() + 100,
                    within(1e-6));
        }

        @Test
        @DisplayName("열의 좌우 여백을 바꾸면 그 열의 오른쪽 정렬한 글자를 칸의 오른쪽 끝에서 그만큼 떨어뜨려 적는다")
        void render_PaddingX_OffsetsTextFromCellEdge() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.setPaddingX(0, 10);
            table.addRow(RowStyle.NORMAL, 0, "1,000");

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            ReportElement.Text text = findText(canvas.finish(), "1,000");
            assertThat(text.x() + ReportText.width(text.text(), text.font())).isCloseTo(canvas.getLeft() + 190,
                    within(1e-6));
        }

        @Test
        @DisplayName("덩어리로 가운데에 놓는 열은 가장 넓은 글자를 칸 가운데에 두고 나머지 글자를 그 오른쪽 끝에 맞춘다")
        void render_BlockCentered_CentersWidestAndRightAlignsOthers() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.setBlockCentered(0);
            table.addRow(RowStyle.NORMAL, 0, "1,234,000");
            table.addRow(RowStyle.NORMAL, 0, "500");
            table.addRow(RowStyle.NORMAL, 0, ReportTable.text("신규", ReportAlign.CENTER));

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            ReportDocument document = canvas.finish();
            ReportElement.Text widest = findText(document, "1,234,000");
            ReportElement.Text narrow = findText(document, "500");
            double center = canvas.getLeft() + 100;
            assertThat(widest.x() + ReportText.width(widest.text(), widest.font()) / 2).isCloseTo(center,
                    within(1e-6));
            assertThat(narrow.x() + ReportText.width(narrow.text(), narrow.font())).isCloseTo(widest.x()
                    + ReportText.width(widest.text(), widest.font()), within(1e-6));
        }

        @Test
        @DisplayName("한 열의 좌우 여백을 바꿔도 다른 열은 기본 여백(3pt)을 쓴다")
        void render_PaddingXOnOneColumn_KeepsOtherColumnsDefault() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 100, ReportAlign.LEFT),
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.setPaddingX(1, 10);
            table.addRow(RowStyle.NORMAL, 0, "헌금", "1,000");

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            assertThat(findText(canvas.finish(), "헌금").x()).isCloseTo(canvas.getLeft() + 3, within(1e-6));
        }

        @Test
        @DisplayName("폭을 직접 정한 열은 넓히지 않고 글자를 여러 줄로 나눈다")
        void render_SetWidths_WrapsTextInsteadOfWidening() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.addRow(RowStyle.NORMAL, 0, "주일 헌금과 감사 헌금", "1,000");
            table.setWidths(40, 0);

            // When
            table.render(canvas);

            // Then
            assertThat(textsOf(canvas.finish())).doesNotContain("주일 헌금과 감사 헌금")
                    .contains("1,000");
        }

        @Test
        @DisplayName("폭을 직접 정할 때 0인 열은 남은 폭을 가져간다")
        void render_SetWidthsWithZero_GivesRemainingWidth() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.addRow(RowStyle.NORMAL, 0, "헌금", "1,000");
            table.setWidths(60, 0);

            // When
            table.renderAt(canvas, canvas.getLeft(), canvas.getY(), 200);

            // Then
            ReportElement.Text amount = findText(canvas.finish(), "1,000");
            assertThat(amount.x() + ReportText.width(amount.text(), amount.font())).isCloseTo(
                    canvas.getLeft() + 200 - 3, within(1e-6));
        }

        @Test
        @DisplayName("줄이는 칸이 한 줄에 들어가면 그대로 적고 아랫줄을 두지 않는다")
        void render_ClippedCellFits_WritesAsIs() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("비고", 0, ReportAlign.LEFT));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.clipped("짧은 비고", "비고"));

            // When
            table.render(canvas);

            // Then
            assertThat(textsOf(canvas.finish())).contains("짧은 비고")
                    .noneMatch(text -> text.startsWith("비고: "));
        }

        @Test
        @DisplayName("줄이는 칸이 넘치면 말줄임표로 줄이고 그 행 아래에 칸 이름을 붙여 전체 내용을 다시 적는다")
        void render_ClippedCellOverflows_ClipsAndAddsContinuation() {
            // Given
            ReportCanvas canvas = new ReportCanvas("시험", ReportStyle.DEFAULT);
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("비고", 0, ReportAlign.LEFT));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.text("헌금"), ReportTable.clipped("아주 긴 비고입니다", "비고"));
            table.setWidths(0, 40);

            // When
            table.render(canvas);

            // Then
            List<String> texts = textsOf(canvas.finish());
            assertThat(texts).anyMatch(text -> text.endsWith("…"))
                    .contains("비고: 아주 긴 비고입니다");
        }
    }

    @Nested
    @DisplayName("measureHeaderWidth 테스트")
    class MeasureHeaderWidthTest {

        @Test
        @DisplayName("좌우 여백을 바꾸면 머리글을 한 줄에 적는 폭에도 양쪽 여백이 들어간다")
        void measureHeaderWidth_PaddingX_IncludesBothSides() {
            // Given
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("금액", 0, ReportAlign.RIGHT));
            table.setPaddingX(0, 10);

            // When
            double width = table.measureHeaderWidth(0);

            // Then
            assertThat(width).isCloseTo(ReportText.width("금액", ReportStyle.DEFAULT.bold(9f)) + 20, within(1e-6));
        }
    }

    @Nested
    @DisplayName("measureNaturalWidth 테스트")
    class MeasureNaturalWidthTest {

        @Test
        @DisplayName("머리글과 내용 중 가장 넓은 것을 한 줄에 적는 폭이며, 들여쓰기와 줄이는 칸의 전체 내용을 포함한다")
        void measureNaturalWidth_Rows_UsesWidestContentWithIndent() {
            // Given
            ReportStyle style = ReportStyle.DEFAULT;
            ReportTable table = new ReportTable(style, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("비고", 0, ReportAlign.LEFT));
            table.addRow(RowStyle.NORMAL, 2, ReportTable.text("헌금"), ReportTable.clipped("아주 긴 비고입니다", "비고"));

            // When
            double nameWidth = table.measureNaturalWidth(0);
            double remarksWidth = table.measureNaturalWidth(1);

            // Then
            assertThat(nameWidth).isCloseTo(Math.max(ReportText.width("항목", style.bold(9f)),
                    ReportText.width("헌금", style.regular(9f)) + 16) + 6, within(1e-6));
            assertThat(remarksWidth).isCloseTo(ReportText.width("아주 긴 비고입니다", style.regular(9f)) + 6,
                    within(1e-6));
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

        @Test
        @DisplayName("다시 적는 줄이 있는 행은 그 줄들만큼 높아진다")
        void measureHeight_ClippedCellOverflows_AddsContinuationHeight() {
            // Given
            ReportTable table = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("비고", 0, ReportAlign.LEFT));
            table.addRow(RowStyle.NORMAL, 0, ReportTable.text("헌금"), ReportTable.clipped("아주 긴 비고입니다", "비고"));
            table.setWidths(0, 40);
            ReportTable shortTable = new ReportTable(ReportStyle.DEFAULT, 9f,
                    new Column("항목", 0, ReportAlign.LEFT),
                    new Column("비고", 0, ReportAlign.LEFT));
            shortTable.addRow(RowStyle.NORMAL, 0, ReportTable.text("헌금"), ReportTable.clipped("짧음", "비고"));
            shortTable.setWidths(0, 40);

            // When
            double height = table.measureHeight(400);
            double shortHeight = shortTable.measureHeight(400);

            // Then
            assertThat(height - shortHeight).isCloseTo(ReportStyle.DEFAULT.lineHeight(ReportStyle.DEFAULT.regular(9f)),
                    within(1e-6));
        }
    }
}
