package org.church.steward.ui.view.report.layout;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.church.steward.ui.util.UIColors;

/**
 * 머리글과 여러 행으로 이루어진 표. 칸의 내용이 길면 칸 안에서 줄을 바꾼다. 세로선 없이 가로선과 머리글의 회색 바탕만으로 구분한다.
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

    private record TextCell(String text) implements Cell {
    }

    private record BarCell(Double ratio) implements Cell {
    }

    private record Row(RowStyle style, int indent, List<Cell> cells) {
    }

    /**
     * 줄바꿈까지 마쳐 높이가 정해진 행
     *
     * @param cellLines 칸별로 나눈 줄. 막대 칸은 null
     */
    private record LaidOutRow(Row row, Font font, List<List<String>> cellLines, double height) {
    }

    private static final double PADDING_X = 3;
    /** 첫 열을 한 단계 들여쓸 때의 폭 */
    private static final double INDENT_STEP = 8;
    /** 막대의 굵기가 한 줄 높이에서 차지하는 비율 */
    private static final double BAR_HEIGHT_RATIO = 0.55;

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
     * @param style    글꼴, 글자 크기, 행 간격. 행의 높이는 글자 높이에 행 간격을 곱한 값이다.
     * @param baseSize 글자의 기본 크기
     */
    public ReportTable(ReportStyle style, float baseSize, Column... columns) {
        this.columns = List.of(columns);
        this.style = style;
        this.regularFont = style.regular(baseSize);
        this.boldFont = style.bold(baseSize);
    }

    /**
     * 글자 칸을 만든다.
     */
    public static Cell text(String text) {
        return new TextCell(text);
    }

    /**
     * 비율을 채워진 길이로 보여주는 막대 칸을 만든다.
     *
     * @param ratio 채울 비율(1이 가득). 1을 넘으면 가득 채우고, null이면 빈 막대를 그린다.
     */
    public static Cell bar(Double ratio) {
        return new BarCell(ratio);
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
     * 행을 추가한다. 칸은 {@link #text(String)}와 {@link #bar(Double)}로 만든다.
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

        canvas.moveDown(drawHeader(canvas, left, canvas.getY(), widths));
        boolean firstOnPage = true;

        for (LaidOutRow row : layOut(widths)) {
            if (!canvas.fits(row.height())) {
                canvas.line(left, canvas.getY(), right, canvas.getY(), STRONG_LINE, UIColors.REPORT_LINE);
                canvas.newPage();
                canvas.moveDown(drawHeader(canvas, left, canvas.getY(), widths));
                firstOnPage = true;
            }
            drawRow(canvas, row, left, canvas.getY(), widths, firstOnPage);
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
        double y = top + drawHeader(canvas, left, top, widths);

        boolean first = true;
        for (LaidOutRow row : layOut(widths)) {
            drawRow(canvas, row, left, y, widths, first);
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
        List<LaidOutRow> laidOutRows = new ArrayList<>();
        for (Row row : rows) {
            Font font = fontOf(row);

            List<List<String>> cellLines = new ArrayList<>();
            int lineCount = 1;
            for (int i = 0; i < widths.length; i++) {
                if (row.cells()
                        .get(i) instanceof TextCell textCell) {
                    List<String> lines = ReportText.wrap(textCell.text(), font,
                            widths[i] - PADDING_X * 2 - indentOf(row, i));
                    cellLines.add(lines);
                    lineCount = Math.max(lineCount, lines.size());
                } else {
                    cellLines.add(null);
                }
            }
            laidOutRows.add(new LaidOutRow(row, font, cellLines, lineCount * style.lineHeight(font)));
        }
        return laidOutRows;
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
     * @param firstBelowHeader 머리글 바로 아래의 행인지 여부. 머리글의 아랫선이 있으므로 행 위에 선을 긋지 않는다.
     */
    private void drawRow(ReportCanvas canvas, LaidOutRow laidOutRow, double left, double top, double[] widths,
            boolean firstBelowHeader) {
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

            if (row.cells()
                    .get(i) instanceof BarCell barCell) {
                drawBar(canvas, barCell, cellX, top, cellWidth, laidOutRow.height(), ReportText.lineHeight(font));
            } else {
                ReportAlign align = columns.get(i)
                        .align();
                double anchorX = switch (align) {
                case LEFT -> cellX + PADDING_X;
                case CENTER -> cellX + cellWidth / 2;
                case RIGHT -> cellX + cellWidth - PADDING_X;
                };
                double baseline = top + style.baselineOffset(font);
                for (String line : laidOutRow.cellLines()
                        .get(i)) {
                    canvas.text(line, font, align, anchorX, baseline);
                    baseline += lineHeight;
                }
            }
            x += widths[i];
        }
    }

    /**
     * 테두리만 있는 막대 안을 비율만큼 진한 회색으로 채운다. 색 없이 채움과 비움만으로 읽을 수 있다.
     */
    private void drawBar(ReportCanvas canvas, BarCell cell, double cellX, double top, double cellWidth,
            double rowHeight, double lineHeight) {
        double barHeight = lineHeight * BAR_HEIGHT_RATIO;
        double barX = cellX + PADDING_X;
        double barY = top + (rowHeight - barHeight) / 2;
        double barWidth = cellWidth - PADDING_X * 2;
        if (barWidth <= 0) {
            return;
        }

        double ratio = cell.ratio() == null ? 0 : Math.max(0, Math.min(1, cell.ratio()));
        if (ratio > 0) {
            canvas.fillRect(barX, barY, barWidth * ratio, barHeight, UIColors.REPORT_BAR);
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
     * 폭이 정해지지 않은 열들이 남은 폭을 똑같이 나눠 갖도록 열의 폭을 정한다. 폭이 정해진 열이라도 글자를 키워 머리글이나 내용이 한 줄에
     * 들어가지 않으면 들어갈 만큼 넓힌다.
     */
    private double[] resolveWidths(double tableWidth) {
        double[] widths = new double[columns.size()];
        double fixedWidth = 0;
        int flexibleCount = 0;
        for (int i = 0; i < widths.length; i++) {
            Column column = columns.get(i);
            if (column.width() > 0) {
                widths[i] = Math.max(column.width(), requiredWidth(i));
                fixedWidth += widths[i];
            } else {
                flexibleCount++;
            }
        }

        double flexibleWidth = flexibleCount == 0 ? 0 : Math.max(0, tableWidth - fixedWidth) / flexibleCount;
        for (int i = 0; i < widths.length; i++) {
            if (columns.get(i)
                    .width() <= 0) {
                widths[i] = flexibleWidth;
            }
        }
        return widths;
    }

    /**
     * 열의 머리글과 글자 칸을 모두 한 줄에 적는 데 필요한 폭. 막대 칸은 세지 않는다.
     */
    private double requiredWidth(int columnIndex) {
        double width = ReportText.width(columns.get(columnIndex)
                .header(), boldFont);
        for (Row row : rows) {
            if (row.cells()
                    .get(columnIndex) instanceof TextCell textCell && textCell.text() != null) {
                width = Math.max(width, ReportText.width(textCell.text(), fontOf(row)) + indentOf(row, columnIndex));
            }
        }
        return width > 0 ? width + PADDING_X * 2 : 0;
    }
}
