package org.church.steward.ui.view.report.composer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Cell;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 예산을 한 줄씩 나열하는 표. 회계 연도 예산안과 회계 연도 결산 세부 내역서가 함께 쓴다. 칸의 순서는 보고서 양식이 정하며 예산 항목은
 * 항상 첫 번째 칸이다.
 * <p>
 * 모든 칸은 머리글과 내용을 한 줄에 적는 폭(예산 항목은 들여쓰기를 포함한 가장 긴 예산명이되 표 폭의 1/5 이상)에서 시작하고, 남는
 * 폭은 모든 칸이 똑같이 나눠 갖는다. 칸의 좌우 여백은 표 글자 한 자("가") 폭이다. 폭이 모자라면 먼저 비고를 머리글이 잘리지 않는
 * 폭까지 줄이고, 그래도 모자라면 예산 항목을 모자란 만큼(최소 폭보다는 좁히지 않고) 줄이고, 칸에 들어가지 않는 예산명은 여러 줄로 나눠
 * 적는다.
 * <p>
 * 금액·비율 칸은 숫자를 오른쪽 정렬한 덩어리로 보고 칸의 가운데에
 * 놓는다({@link ReportTable#setBlockCentered}). 그래서 칸이 넓어져도 숫자가 오른쪽 끝에 붙어 머리글과
 * 멀어지지 않고, 가장 넓은 숫자는 머리글과 같은 가운데에 온다. 숫자 대신 적는 "신규"는 칸의 가운데에 적는다.
 * <p>
 * 비고는 줄을 바꾸지 않고, 칸에 들어가지 않으면 말줄임표로 줄인 뒤 그 예산 행 바로 아래에 "비고: …"처럼 전체 내용을 다시
 * 적는다(주간 회계 결산 보고서의 내역과 같은 방식). 예산 행과 다시 적는 줄은 페이지 사이에서 갈라지지 않는다. 비고도 왼쪽 정렬한
 * 덩어리로 칸의 가운데에 놓아, 비고가 짧으면 머리글 쪽으로 모인다(줄어든 비고가 있으면 칸의 왼쪽 끝에서 시작한다).
 */
final class BudgetListTable {

    /**
     * 표의 한 행
     *
     * @param depth    예산 계층에서의 깊이. 예산 항목을 그만큼 들여쓴다.
     * @param values   칸마다 적을 글자. 비어 있는 칸은 빠져 있어도 된다.
     * @param centered 칸의 정렬 대신 가운데 정렬할 칸. 금액 칸에 숫자 대신 "신규" 같은 글자를 적는 경우와, 총계 행의 예산
     *                 항목 칸("총계")이다.
     */
    record Row(RowStyle style, int depth, Map<ReportColumn, String> values, Set<ReportColumn> centered) {

        Row(RowStyle style, int depth, Map<ReportColumn, String> values) {
            this(style, depth, values, Set.of());
        }
    }

    /** 예산 항목의 최소 폭(표의 폭에 대한 비율) */
    private static final double NAME_MIN_RATIO = 0.2;
    /** 칸의 좌우 여백으로 쓸 글자. 칸의 왼쪽과 오른쪽에 각각 이 글자의 폭만큼 띄운다. */
    private static final String PADDING_TEXT = "가";

    private BudgetListTable() {
        // 인스턴스화 방지
    }

    /**
     * 현재 위치부터 페이지 전체 폭으로 표를 그리고 표 아래로 이동한다. 페이지가 모자라면 다음 페이지로 이어 그린다.
     *
     * @param columns 칸의 순서와 숨김
     */
    static void render(ReportCanvas canvas, ReportTemplateTable table, List<ReportColumnDto> columns,
            List<Row> rows) {
        List<ReportColumn> visible = columns.stream()
                .filter(column -> !column.isHidden())
                .map(ReportColumnDto::getColumn)
                .toList();

        ReportTable reportTable = new ReportTable(canvas.getStyle(), ReportStyles.BUDGET_TABLE_SIZE, visible.stream()
                .map(column -> new Column(table.labelOf(column), 0, alignOf(column)))
                .toArray(Column[]::new));
        // 오른쪽 정렬한 금액 바로 뒤에 왼쪽 정렬한 비고가 오면 붙어 보이므로 칸마다 좌우에 여백을 둔다. 여백은 글자 폭으로 정해
        // 글자 크기를 키우면 함께 커진다.
        double padding = ReportText.width(PADDING_TEXT, canvas.getStyle()
                .regular(ReportStyles.BUDGET_TABLE_SIZE));
        for (int i = 0; i < visible.size(); i++) {
            reportTable.setPaddingX(i, padding);
            // 예산 항목은 들여쓰기로 계층을 보이므로 칸의 왼쪽 끝에서 시작한다.
            if (visible.get(i) != ReportColumn.BUDGET_NAME) {
                reportTable.setBlockCentered(i);
            }
        }
        for (Row row : rows) {
            reportTable.addRow(row.style(), row.depth(), visible.stream()
                    .map(column -> cellOf(table, column, row))
                    .toArray(Cell[]::new));
        }

        reportTable.setWidths(resolveWidths(reportTable, visible, canvas.getContentWidth()));
        reportTable.render(canvas);
    }

    /**
     * 글자를 적는 칸(예산 항목, 비고)인지 여부. 나머지는 금액과 비율을 적는 칸이다.
     */
    private static boolean isText(ReportColumn column) {
        return column == ReportColumn.BUDGET_NAME || column == ReportColumn.REMARKS;
    }

    private static ReportAlign alignOf(ReportColumn column) {
        return isText(column) ? ReportAlign.LEFT : ReportAlign.RIGHT;
    }

    private static Cell cellOf(ReportTemplateTable table, ReportColumn column, Row row) {
        String value = row.values()
                .get(column);
        if (column == ReportColumn.REMARKS) {
            return ReportTable.clipped(value, table.labelOf(column));
        }
        return row.centered()
                .contains(column) ? ReportTable.text(value, ReportAlign.CENTER) : ReportTable.text(value);
    }

    /**
     * 칸의 폭을 정한다. 내용에 맞춘 폭이 표의 폭보다 좁으면 남는 폭을 모든 칸에 똑같이 더하고, 넓으면 비고와 예산 항목을 줄인다.
     */
    private static double[] resolveWidths(ReportTable table, List<ReportColumn> visible, double tableWidth) {
        int count = visible.size();
        double[] widths = new double[count];
        double[] minWidths = new double[count];
        double shortage = -tableWidth;
        for (int i = 0; i < count; i++) {
            widths[i] = table.measureNaturalWidth(i);
            ReportColumn column = visible.get(i);
            // 금액과 비율은 줄을 바꾸거나 줄이지 않는다.
            minWidths[i] = switch (column) {
            case REMARKS -> table.measureHeaderWidth(i);
            // 예산명이 짧아도 표의 왼쪽이 비좁아 보이지 않게 최소 폭을 둔다.
            case BUDGET_NAME -> Math.max(table.measureHeaderWidth(i), tableWidth * NAME_MIN_RATIO);
            default -> widths[i];
            };
            widths[i] = Math.max(widths[i], minWidths[i]);
            shortage += widths[i];
        }

        if (shortage < 0) {
            double extra = -shortage / count;
            for (int i = 0; i < count; i++) {
                widths[i] += extra;
            }
            return widths;
        }

        shortage = shrink(widths, minWidths, visible.indexOf(ReportColumn.REMARKS), shortage);
        shrink(widths, minWidths, visible.indexOf(ReportColumn.BUDGET_NAME), shortage);
        return widths;
    }

    /**
     * 모자란 폭만큼 칸을 줄이되 최소 폭보다는 줄이지 않는다.
     *
     * @param index    줄일 칸의 순번. 그 칸이 없으면 -1
     * @param shortage 모자란 폭. 0 이하이면 줄이지 않는다.
     * @return 줄이고도 남은 모자란 폭
     */
    private static double shrink(double[] widths, double[] minWidths, int index, double shortage) {
        if (index < 0 || shortage <= 0) {
            return shortage;
        }
        double reduced = Math.min(shortage, Math.max(0, widths[index] - minWidths[index]));
        widths[index] -= reduced;
        return shortage - reduced;
    }
}
