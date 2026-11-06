package org.church.steward.ui.view.report.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.church.steward.ui.util.UIColors;

/**
 * 머리글과 여러 행으로 이루어진 표. 칸의 내용이 길면 칸 안에서 줄을 바꾼다. 세로선 없이 가로선과 머리글의 회색 바탕만으로 구분한다.
 * 줄을 바꾸지 않고 말줄임표로 줄이는 칸({@link #clipped(String, String)})은 줄어든 내용을 그 행의 아랫줄에 표의
 * 전체 폭으로 다시 적는다.
 * <p>
 * 페이지 전체 폭으로 흘려 그리면({@link #render(ReportCanvas)}) 행이 페이지 끝에 걸릴 때 행 전체를 다음 페이지로
 * 넘기며 머리글을 다시 그린다. 정해진 자리에 그리면({@link #renderAt}) 페이지를 넘기지 않으므로 여러 표를 좌우로 나란히
 * 놓을 수 있다.
 */
public final class ReportTable {

    /**
     * 표의 열
     *
     * @param header 머리글
     * @param width  열의 폭. 0 이하이면 폭이 정해진 열들을 뺀 나머지를 차지한다. 머리글이나 내용이 이 폭의 한 줄에 들어가지
     *               않으면 들어갈 만큼 넓힌다(글자 크기를 키운 경우).
     * @param align  내용의 정렬. 머리글은 항상 가운데 정렬한다.
     */
    public record Column(String header, double width, ReportAlign align) {
    }

    public enum RowStyle {
        NORMAL,
        /** 굵은 글씨로 쓰는 행 */
        EMPHASIS,
        /** 굵은 글씨에 회색 바탕을 깔고 위에 굵은 선을 긋는 합계 행 */
        TOTAL
    }

    /**
     * 칸의 내용
     */
    public sealed interface Cell {
    }

    /**
     * @param align 이 칸만의 정렬. null이면 열의 정렬을 따른다.
     */
    private record TextCell(String text, ReportAlign align) implements Cell {
    }

    /**
     * 왼쪽에 막대, 오른쪽에 글자를 적는 칸
     *
     * @param textWidth 글자를 적을 자리의 폭. 같은 열의 막대 길이가 같도록 열에서 가장 넓은 글자에 맞춘 값을 넘긴다.
     */
    private record BarTextCell(Double ratio, String text, double textWidth) implements Cell {
    }

    /**
     * 한 줄에 들어가지 않으면 말줄임표로 줄이고 아랫줄에 다시 적는 칸
     *
     * @param label 다시 적는 줄 앞에 붙여 어느 칸의 내용인지 알리는 이름
     */
    private record ClippedCell(String text, String label) implements Cell {
    }

    private record Row(RowStyle style, int indent, List<Cell> cells) {
    }

    /**
     * 줄바꿈까지 마쳐 높이가 정해진 행
     *
     * @param cellLines         칸별로 나눈 줄
     * @param continuationLines 줄어든 칸의 내용을 다시 적는 아랫줄
     */
    private record LaidOutRow(Row row, Font font, List<List<String>> cellLines, List<String> continuationLines,
            double height) {
    }

    /** 칸의 기본 좌우 여백 */
    private static final double DEFAULT_PADDING_X = 3;
    /** 첫 열을 한 단계 들여쓸 때의 폭 */
    private static final double INDENT_STEP = 8;
    /** 줄어든 내용을 다시 적는 줄의 들여쓰기. 행의 들여쓰기에 더한다. */
    private static final double CONTINUATION_INDENT = 10;
    /** 막대의 굵기가 한 줄 높이에서 차지하는 비율 */
    private static final double BAR_HEIGHT_RATIO = 0.55;
    /** 막대와 글자 사이의 간격 */
    private static final double BAR_GAP = 3;
    /** 막대를 곁들인 칸에서 막대가 차지할 최소 폭. 열의 폭이 모자라도 막대가 사라지지 않게 그만큼 열을 넓힌다. */
    private static final double MIN_BAR_WIDTH = 12;

    private static final double STRONG_LINE = 0.8;
    private static final double HEADER_LINE = 0.5;
    private static final double GRID_LINE = 0.3;
    private static final double BAR_LINE = 0.4;

    private final List<Column> columns;
    private final ReportStyle style;
    private final Font regularFont;
    private final Font boldFont;
    private final List<Row> rows = new ArrayList<>();
    /**
     * {@link #setWidths}로 정한 열의 폭. 정하지 않았으면 null이며 열마다 정한
     * 폭({@link Column#width()})을 쓴다.
     */
    private double[] fixedWidths;
    /** 열마다의 좌우 여백. 칸의 왼쪽과 오른쪽에 각각 이만큼 둔다. */
    private final double[] paddings;
    /** 열마다 내용을 한 덩어리로 칸 가운데에 놓는지 여부({@link #setBlockCentered}) */
    private final boolean[] blockCentered;

    /**
     * @param style    글꼴, 글자 크기, 행 간격. 행의 높이는 글자 높이에 행 간격을 곱한 값이다.
     * @param baseSize 글자의 기본 크기
     */
    public ReportTable(ReportStyle style, float baseSize, Column... columns) {
        this.columns = List.of(columns);
        this.style = style;
        this.regularFont = style.regular(baseSize);
        this.boldFont = style.bold(baseSize);
        this.paddings = new double[columns.length];
        Arrays.fill(paddings, DEFAULT_PADDING_X);
        this.blockCentered = new boolean[columns.length];
    }

    /**
     * 글자 칸을 만든다.
     */
    public static Cell text(String text) {
        return new TextCell(text, null);
    }

    /**
     * 열의 정렬 대신 정한 정렬로 적는 글자 칸을 만든다. 금액 칸에 숫자 대신 "신규" 같은 글자를 적을 때 쓴다.
     */
    public static Cell text(String text, ReportAlign align) {
        return new TextCell(text, align);
    }

    /**
     * 왼쪽에 비율을 채워진 길이로 보여주는 막대를, 오른쪽에 글자를 적는 칸을 만든다. 글자는 열의 정렬에 따라 글자 자리 안에서 정렬하고,
     * 막대는 칸에서 글자 자리를 뺀 폭을 모두 차지한다.
     *
     * @param ratio     채울 비율(1이 가득). 1을 넘으면 가득 채우고, null이면 빈 막대를 그린다.
     * @param textWidth 글자를 적을 자리의 폭. 같은 열의 막대 길이가 같도록 열에서 가장 넓은 글자에 맞춘 값을 넘긴다.
     */
    public static Cell barText(Double ratio, String text, double textWidth) {
        return new BarTextCell(ratio, text, textWidth);
    }

    /**
     * 줄을 바꾸지 않는 글자 칸을 만든다. 칸에 한 줄로 들어가지 않으면 말줄임표로 줄이고, 그 행 바로 아래에 "비고: …"처럼 칸 이름을
     * 붙여 표의 전체 폭으로 내용을 모두 적는다(필요하면 여러 줄). 다시 적는 줄은 그 행과 함께 페이지를 넘긴다.
     *
     * @param label 다시 적는 줄 앞에 붙일 칸 이름
     */
    public static Cell clipped(String text, String label) {
        return new ClippedCell(text, label);
    }

    /**
     * 열의 좌우 여백을 바꾼다(기본 3pt). 칸의 왼쪽과 오른쪽에 같은 여백을 두므로, 오른쪽 정렬한 숫자도 가운데 정렬한 머리글과 치우치지
     * 않은 채 옆 칸과 떨어진다. 내용의 폭을 재는 메서드({@link #measureNaturalWidth},
     * {@link #measureHeaderWidth})가 이 여백을 포함하므로 폭을 재기 전에 정한다.
     */
    public void setPaddingX(int columnIndex, double paddingX) {
        paddings[columnIndex] = paddingX;
    }

    /**
     * 오른쪽이나 왼쪽으로 정렬하는 열의 내용을 한 덩어리로 보고 칸의 가운데에 놓는다. 덩어리의 폭은 그 열에서 가장 넓은 글자의 폭이므로,
     * 가장 넓은 글자는 가운데 정렬한 머리글과 같은 가운데에 오고 나머지 글자는 덩어리 안에서 열의 정렬대로 끝을 맞춘다(오른쪽 정렬이면
     * 덩어리의 오른쪽 끝, 왼쪽 정렬이면 왼쪽 끝). 고정폭 글꼴에서 숫자 앞에 공백을 채워 가운데 정렬한 것과 같다. 칸이 넓어도 숫자나 글이
     * 칸의 한쪽 끝에 붙어 머리글과 멀어지지 않게 할 때 쓴다. 가장 넓은 글자가 칸에 들어가지 않으면(줄을 바꾸거나 줄이는 경우) 그냥 열의
     * 정렬을 따른다. 정렬을 따로 정한 칸({@link #text(String, ReportAlign)})은 덩어리에 넣지 않고 그 정렬을
     * 따른다.
     */
    public void setBlockCentered(int columnIndex) {
        blockCentered[columnIndex] = true;
    }

    /**
     * 열의 폭을 직접 정한다. 정한 폭은 넓히지 않으므로 글자 칸은 줄을 바꾸고, 줄이는 칸은 말줄임표로 줄인다. 0 이하인 열은 정한 폭들을
     * 뺀 나머지를 나눠 갖는다. 열의 폭은 보통 {@link #measureNaturalWidth}와
     * {@link #measureHeaderWidth}로 잰 값으로 정한다.
     */
    public void setWidths(double... widths) {
        if (widths.length != columns.size()) {
            throw new IllegalArgumentException("폭의 개수가 표의 열 수와 다릅니다.");
        }
        this.fixedWidths = widths.clone();
    }

    /**
     * 열의 머리글과 지금까지 추가한 행의 내용을 모두 한 줄에 적는 데 필요한 폭(여백과 들여쓰기 포함). 줄이는 칸은 줄이지 않은 내용으로
     * 잰다.
     */
    public double measureNaturalWidth(int columnIndex) {
        return requiredWidth(columnIndex);
    }

    /**
     * 열의 머리글을 한 줄에 적는 데 필요한 폭(여백 포함)
     */
    public double measureHeaderWidth(int columnIndex) {
        return ReportText.width(columns.get(columnIndex)
                .header(), boldFont) + paddings[columnIndex] * 2;
    }

    /**
     * 글자로만 이루어진 행을 추가한다.
     *
     * @param indent 첫 열의 들여쓰기 단계. 계층의 깊이를 나타낸다.
     * @param cells  열 순서대로의 내용
     */
    public void addRow(RowStyle style, int indent, String... cells) {
        addRow(style, indent, Arrays.stream(cells)
                .map(ReportTable::text)
                .toArray(Cell[]::new));
    }

    /**
     * 행을 추가한다. 칸은 {@link #text(String)}, {@link #barText(Double, String, double)},
     * {@link #clipped(String, String)}로 만든다.
     */
    public void addRow(RowStyle style, int indent, Cell... cells) {
        if (cells.length != columns.size()) {
            throw new IllegalArgumentException("행의 칸 수가 표의 열 수와 다릅니다.");
        }
        rows.add(new Row(style, indent, List.of(cells)));
    }

    /**
     * 작업대의 현재 위치부터 페이지 전체 폭으로 표를 그리고 표 아래로 이동한다. 페이지가 모자라면 다음 페이지로 이어 그린다.
     */
    public void render(ReportCanvas canvas) {
        double left = canvas.getLeft();
        double right = canvas.getRight();
        double[] widths = resolveWidths(right - left);
        double[] blockWidths = measureBlockWidths();

        canvas.moveDown(drawHeader(canvas, left, canvas.getY(), widths));
        boolean firstOnPage = true;

        for (LaidOutRow row : layOut(widths)) {
            if (!canvas.fits(row.height())) {
                canvas.line(left, canvas.getY(), right, canvas.getY(), STRONG_LINE, UIColors.REPORT_LINE);
                canvas.newPage();
                canvas.moveDown(drawHeader(canvas, left, canvas.getY(), widths));
                firstOnPage = true;
            }
            drawRow(canvas, row, left, canvas.getY(), widths, blockWidths, firstOnPage);
            canvas.moveDown(row.height());
            firstOnPage = false;
        }

        canvas.line(left, canvas.getY(), right, canvas.getY(), STRONG_LINE, UIColors.REPORT_LINE);
    }

    /**
     * 현재 페이지의 정해진 자리에 표 전체를 그린다. 페이지를 넘기지 않으며 작업대의 위치도 바꾸지 않는다.
     *
     * @return 표의 높이
     */
    public double renderAt(ReportCanvas canvas, double left, double top, double width) {
        double[] widths = resolveWidths(width);
        double[] blockWidths = measureBlockWidths();
        double y = top + drawHeader(canvas, left, top, widths);

        boolean first = true;
        for (LaidOutRow row : layOut(widths)) {
            drawRow(canvas, row, left, y, widths, blockWidths, first);
            y += row.height();
            first = false;
        }

        canvas.line(left, y, left + width, y, STRONG_LINE, UIColors.REPORT_LINE);
        return y - top;
    }

    /**
     * 주어진 폭으로 그렸을 때의 표의 높이
     */
    public double measureHeight(double width) {
        double height = headerHeight();
        for (LaidOutRow row : layOut(resolveWidths(width))) {
            height += row.height();
        }
        return height;
    }

    private List<LaidOutRow> layOut(double[] widths) {
        double tableWidth = sum(widths);
        List<LaidOutRow> laidOutRows = new ArrayList<>();
        for (Row row : rows) {
            Font font = fontOf(row);

            List<List<String>> cellLines = new ArrayList<>();
            List<String> continuationLines = new ArrayList<>();
            int lineCount = 1;
            for (int i = 0; i < widths.length; i++) {
                double textWidth = widths[i] - paddings[i] * 2 - indentOf(row, i);
                List<String> lines = switch (row.cells()
                        .get(i)) {
                case TextCell textCell -> ReportText.wrap(textCell.text(), font, textWidth);
                case BarTextCell barTextCell -> ReportText.wrap(barTextCell.text(), font, barTextCell.textWidth());
                case ClippedCell clippedCell -> List.of(clip(row, clippedCell, font, textWidth, tableWidth,
                        continuationLines));
                };
                cellLines.add(lines);
                lineCount = Math.max(lineCount, lines.size());
            }
            double height = lineCount * style.lineHeight(font) + continuationLines.size() * style.lineHeight(
                    regularFont);
            laidOutRows.add(new LaidOutRow(row, font, cellLines, continuationLines, height));
        }
        return laidOutRows;
    }

    /**
     * 줄이는 칸에 넣을 글자를 정한다. 한 줄에 들어가면 그대로 쓰고, 넘치면 말줄임표로 줄인 뒤 전체 내용을 다시 적는 줄을 추가한다.
     *
     * @param continuationLines 다시 적는 줄을 추가할 목록
     */
    private String clip(Row row, ClippedCell cell, Font font, double textWidth, double tableWidth,
            List<String> continuationLines) {
        String text = cell.text();
        if (text == null || text.isBlank()) {
            return "";
        }
        if (ReportText.width(text, font) <= textWidth) {
            return text;
        }

        // 다시 적는 줄의 오른쪽 끝은 마지막 칸의 여백에 맞춘다.
        double continuationWidth = tableWidth - continuationIndentOf(row) - paddings[paddings.length - 1];
        continuationLines.addAll(ReportText.wrap(cell.label() + ": " + text, regularFont, continuationWidth));
        return ReportText.clip(text, font, textWidth);
    }

    /**
     * 다시 적는 줄의 들여쓰기. 계층이 보이도록 첫 열의 들여쓰기에 더한다.
     */
    private static double continuationIndentOf(Row row) {
        return row.indent() * INDENT_STEP + CONTINUATION_INDENT;
    }

    private Font fontOf(Row row) {
        return row.style() == RowStyle.NORMAL ? regularFont : boldFont;
    }

    private double headerHeight() {
        return style.lineHeight(boldFont);
    }

    /**
     * @return 머리글의 높이
     */
    private double drawHeader(ReportCanvas canvas, double left, double top, double[] widths) {
        double right = left + sum(widths);
        double height = headerHeight();

        canvas.fillRect(left, top, right - left, height, UIColors.REPORT_FILL);
        canvas.line(left, top, right, top, STRONG_LINE, UIColors.REPORT_LINE);
        canvas.line(left, top + height, right, top + height, HEADER_LINE, UIColors.REPORT_LINE);

        double baseline = top + style.baselineOffset(boldFont);
        double x = left;
        for (int i = 0; i < widths.length; i++) {
            canvas.text(columns.get(i)
                    .header(), boldFont, ReportAlign.CENTER, x + widths[i] / 2, baseline);
            x += widths[i];
        }
        return height;
    }

    /**
     * 열마다 내용을 한 덩어리로 가운데에 놓을 때의 덩어리 폭. 그 열의 글자 칸과 줄이는 칸 중 가장 넓은 글자의 폭이며(머리글과 정렬을 따로
     * 정한 칸은 빼고, 줄이는 칸은 줄이지 않은 내용으로 잰다), 덩어리로 놓지 않는 열은 0이다.
     */
    private double[] measureBlockWidths() {
        double[] blockWidths = new double[columns.size()];
        for (int i = 0; i < blockWidths.length; i++) {
            if (!blockCentered[i]) {
                continue;
            }
            for (Row row : rows) {
                String text = switch (row.cells()
                        .get(i)) {
                case TextCell textCell -> textCell.align() == null ? textCell.text() : null;
                case ClippedCell clippedCell -> clippedCell.text();
                case BarTextCell _ -> null;
                };
                if (text != null) {
                    blockWidths[i] = Math.max(blockWidths[i], ReportText.width(text, fontOf(row)));
                }
            }
        }
        return blockWidths;
    }

    /**
     * @param blockWidths      열마다 덩어리로 가운데에 놓을 내용의 폭({@link #measureBlockWidths()})
     * @param firstBelowHeader 머리글 바로 아래의 행인지 여부. 머리글의 아랫선이 있으므로 행 위에 선을 긋지 않는다.
     */
    private void drawRow(ReportCanvas canvas, LaidOutRow laidOutRow, double left, double top, double[] widths,
            double[] blockWidths, boolean firstBelowHeader) {
        Row row = laidOutRow.row();
        Font font = laidOutRow.font();
        double right = left + sum(widths);
        double lineHeight = style.lineHeight(font);

        if (row.style() == RowStyle.TOTAL) {
            canvas.fillRect(left, top, right - left, laidOutRow.height(), UIColors.REPORT_FILL);
            canvas.line(left, top, right, top, STRONG_LINE, UIColors.REPORT_LINE);
        } else if (!firstBelowHeader) {
            canvas.line(left, top, right, top, GRID_LINE, UIColors.REPORT_GRID);
        }

        double x = left;
        for (int i = 0; i < widths.length; i++) {
            double cellX = x + indentOf(row, i);
            double cellWidth = widths[i] - indentOf(row, i);

            // 글자를 적을 자리. 막대를 곁들인 칸은 오른쪽의 글자 자리이고 나머지에 막대를 그린다.
            double paddingX = paddings[i];
            double textLeft = cellX + paddingX;
            double textWidth = cellWidth - paddingX * 2;
            if (row.cells()
                    .get(i) instanceof BarTextCell barTextCell) {
                textWidth = barTextCell.textWidth();
                textLeft = cellX + cellWidth - paddingX - textWidth;
                drawBar(canvas, barTextCell.ratio(), cellX + paddingX, top,
                        textLeft - BAR_GAP - (cellX + paddingX), laidOutRow.height(), ReportText.lineHeight(font));
            }

            ReportAlign ownAlign = row.cells()
                    .get(i) instanceof TextCell textCell ? textCell.align() : null;
            ReportAlign align = ownAlign != null ? ownAlign
                    : columns.get(i)
                            .align();
            double anchorX = switch (align) {
            case LEFT -> textLeft;
            case CENTER -> textLeft + textWidth / 2;
            case RIGHT -> textLeft + textWidth;
            };
            // 덩어리로 가운데에 놓는 열은 덩어리의 끝(오른쪽 정렬이면 오른쪽 끝, 왼쪽 정렬이면 왼쪽 끝)에 맞춘다. 칸이 덩어리보다
            // 좁으면 그냥 열의 정렬이다.
            if (ownAlign == null && blockWidths[i] > 0) {
                double blockInset = Math.max(0, (textWidth - blockWidths[i]) / 2);
                if (align == ReportAlign.RIGHT) {
                    anchorX -= blockInset;
                } else if (align == ReportAlign.LEFT) {
                    anchorX += blockInset;
                }
            }
            double baseline = top + style.baselineOffset(font);
            for (String line : laidOutRow.cellLines()
                    .get(i)) {
                canvas.text(line, font, align, anchorX, baseline);
                baseline += lineHeight;
            }
            x += widths[i];
        }

        // 줄어든 칸의 내용은 칸들의 줄 아래에 흐린 글자로 다시 적는다.
        int lineCount = 1;
        for (List<String> lines : laidOutRow.cellLines()) {
            lineCount = Math.max(lineCount, lines.size());
        }
        double continuationTop = top + lineCount * lineHeight;
        double continuationLineHeight = style.lineHeight(regularFont);
        for (String line : laidOutRow.continuationLines()) {
            canvas.text(line, regularFont, ReportAlign.LEFT, left + continuationIndentOf(row),
                    continuationTop + style.baselineOffset(regularFont), UIColors.REPORT_TEXT_SUB);
            continuationTop += continuationLineHeight;
        }
    }

    /**
     * 테두리만 있는 막대 안을 비율만큼 진한 회색으로 채운다. 색 없이 채움과 비움만으로 읽을 수 있다.
     *
     * @param ratio 채울 비율(1이 가득). 1을 넘으면 가득 채우고, null이면 빈 막대를 그린다.
     */
    private void drawBar(ReportCanvas canvas, Double ratio, double barX, double top, double barWidth,
            double rowHeight, double lineHeight) {
        double barHeight = lineHeight * BAR_HEIGHT_RATIO;
        double barY = top + (rowHeight - barHeight) / 2;
        if (barWidth <= 0) {
            return;
        }

        double fill = ratio == null ? 0 : Math.max(0, Math.min(1, ratio));
        if (fill > 0) {
            canvas.fillRect(barX, barY, barWidth * fill, barHeight, UIColors.REPORT_BAR);
        }
        canvas.strokeRect(barX, barY, barWidth, barHeight, BAR_LINE, UIColors.REPORT_BAR);
    }

    /**
     * 들여쓰기는 첫 열에만 적용한다.
     */
    private static double indentOf(Row row, int columnIndex) {
        return columnIndex == 0 ? row.indent() * INDENT_STEP : 0;
    }

    private static double sum(double[] widths) {
        double total = 0;
        for (double width : widths) {
            total += width;
        }
        return total;
    }

    /**
     * 폭이 정해지지 않은 열들이 남은 폭을 똑같이 나눠 갖도록 열의 폭을 정한다. 열의 폭을 직접 정했으면({@link #setWidths})
     * 그 폭을 그대로 쓰고, 그렇지 않으면 폭이 정해진 열이라도 글자를 키워 머리글이나 내용이 한 줄에 들어가지 않을 때 들어갈 만큼 넓힌다.
     */
    private double[] resolveWidths(double tableWidth) {
        double[] widths = new double[columns.size()];
        double fixedWidth = 0;
        int flexibleCount = 0;
        for (int i = 0; i < widths.length; i++) {
            double width = fixedWidths != null ? fixedWidths[i]
                    : columns.get(i)
                            .width();
            if (width > 0) {
                widths[i] = fixedWidths != null ? width : Math.max(width, requiredWidth(i));
                fixedWidth += widths[i];
            } else {
                widths[i] = -1;
                flexibleCount++;
            }
        }

        double flexibleWidth = flexibleCount == 0 ? 0 : Math.max(0, tableWidth - fixedWidth) / flexibleCount;
        for (int i = 0; i < widths.length; i++) {
            if (widths[i] < 0) {
                widths[i] = flexibleWidth;
            }
        }
        return widths;
    }

    /**
     * 열의 머리글과 글자를 모두 한 줄에 적는 데 필요한 폭. 막대를 곁들인 칸은 글자 자리에 막대의 최소 폭을 더한다.
     */
    private double requiredWidth(int columnIndex) {
        double width = ReportText.width(columns.get(columnIndex)
                .header(), boldFont);
        for (Row row : rows) {
            Cell cell = row.cells()
                    .get(columnIndex);
            String text = switch (cell) {
            case TextCell textCell -> textCell.text();
            case ClippedCell clippedCell -> clippedCell.text();
            case BarTextCell _ -> null;
            };
            if (text != null) {
                width = Math.max(width, ReportText.width(text, fontOf(row)) + indentOf(row, columnIndex));
            } else if (cell instanceof BarTextCell barTextCell) {
                double textWidth = barTextCell.text() == null ? barTextCell.textWidth()
                        : Math.max(barTextCell.textWidth(), ReportText.width(barTextCell.text(), fontOf(row)));
                width = Math.max(width, MIN_BAR_WIDTH + BAR_GAP + textWidth);
            }
        }
        return width > 0 ? width + paddings[columnIndex] * 2 : 0;
    }
}
