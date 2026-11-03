package org.church.steward.ui.view.report.composer;

import java.awt.Color;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 주간 회계 결산 보고서의 주간 수입 내역과 주간 지출 내역. "수입/지출 관리"의 "예산별 보기"처럼 왼쪽 단에 수입, 오른쪽 단에 지출을
 * 놓고, 예산 행 아래에 그 예산의 내역 행들을 둔다.
 * <p>
 * 칸의 순서와 숨김은 보고서 양식이 정하며, 일자는 항상 첫 번째 칸의 왼쪽에 붙여 적는다. 금액 칸까지는 내용에 맞춘 폭(상한
 * 안에서)이고, 금액 뒤의 칸들은 남은 폭을 내용의 길이에 비례해 나눠 갖는다. 그래서 내용이 짧으면 봉헌자와 금액처럼 금액 앞의 칸이
 * 가깝게 붙는다. 금액은 오른쪽 정렬이라 뒤 칸의 글자와 붙어 보이므로 금액 뒤에는 한 글자 폭의 간격을 둔다.
 * <p>
 * 한 장에 많이 담기 위해 글자를 작게 쓰고 내역은 한 줄에 한 건씩 적는다. 줄 높이는 서식의 행 간격을 따른다. 칸에 다 들어가지 않는
 * 내용은 말줄임표로 줄이되, 바로 아랫줄에 들여써서 단의 전체 폭으로 내용을 모두 적는다(필요하면 여러 줄). 숨긴 칸도 값이 있으면 같은
 * 방식으로 아랫줄에 적는다. 줄어든 칸마다 따로 줄을 차지하므로 어떤 내용도 잘린 채로 끝나지 않는다. 내역 행은 한 건씩 번갈아 바탕을
 * 깔며(아랫줄은 그 내역과 같은 바탕), 예산 행은 그보다 짙은 바탕이다.
 * <p>
 * 두 단은 서로 독립적으로 아래로 흘러가며, 페이지 끝에 닿으면 각자 다음 페이지의 같은 단으로 넘어간다.
 */
final class WeeklyRecordSection {

    private static final float FONT_SIZE = ReportFonts.SIZE_DETAIL;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");
    private static final String EMPTY_MESSAGE = "내역이 없습니다.";

    /** 칸의 좌우 여백 */
    private static final double PADDING_X = 2;
    /** 줄어든 내용을 다시 적는 줄의 들여쓰기 */
    private static final double CONTINUATION_INDENT = 10;
    /** 내용에 맞춘 칸의 폭 상한(단의 폭에 대한 비율). 첫 번째 칸은 일자가 붙으므로 조금 더 넓게 둔다. */
    private static final double FIRST_MAX_RATIO = 0.45;
    private static final double TEXT_MAX_RATIO = 0.35;
    /** 금액 뒤의 칸들에 남겨 둘 최소 폭(단의 폭에 대한 비율) */
    private static final double TRAILING_MIN_RATIO = 0.25;
    /** 금액 뒤의 간격으로 쓸 글자. 글자 크기를 키우면 간격도 함께 커진다. */
    private static final String AMOUNT_GAP_TEXT = "가";
    /** 머리글의 글자 위아래 여백 */
    private static final double HEADER_PADDING = 1;

    private static final double STRONG_LINE = 0.8;
    private static final double HEADER_LINE = 0.5;

    private final Font regular;
    private final Font bold;
    /** 한 줄의 높이. 보통 글씨와 굵은 글씨 중 높은 쪽에 행 간격을 적용한다. */
    private final double lineHeight;
    /** 줄의 위쪽 끝에서 기준선까지의 거리. 늘어난 줄간격을 글자 위아래에 반씩 나눈다. */
    private final double baseline;

    /**
     * 한 줄 안의 글자 조각
     *
     * @param anchorX 단의 왼쪽 끝에서 정렬 기준까지의 거리
     */
    private record Fragment(String text, Font font, ReportAlign align, double anchorX, Color color) {
    }

    /**
     * @param background 줄에 까는 바탕색. 바탕이 없으면 null
     */
    private record RowLine(List<Fragment> fragments, Color background) {
    }

    /**
     * 페이지가 넘어갈 때 떨어지면 안 되는 줄들의 묶음
     */
    private record Block(List<RowLine> lines) {
    }

    /**
     * 단을 다 그린 뒤의 위치
     */
    private record End(int pageIndex, double y) {

        boolean isAfter(End other) {
            return pageIndex > other.pageIndex || (pageIndex == other.pageIndex && y > other.y);
        }
    }

    /**
     * 보이는 칸 하나
     *
     * @param header 머리글
     * @param left   단의 왼쪽 끝에서 칸의 왼쪽 끝까지의 거리
     */
    private record Cell(ReportColumn column, String header, double left, double width) {

        ReportAlign align() {
            return column == ReportColumn.AMOUNT ? ReportAlign.RIGHT : ReportAlign.LEFT;
        }

        /**
         * 단의 왼쪽 끝에서 글자의 정렬 기준까지의 거리
         */
        double anchorX() {
            return align() == ReportAlign.RIGHT ? left + width - PADDING_X : left + PADDING_X;
        }
    }

    /**
     * 한 단의 칸 구성
     *
     * @param width           단의 폭
     * @param table           단의 표. 칸의 이름을 정한다.
     * @param cells           보이는 칸. 순서대로이며 첫 번째 칸에 일자를 붙인다.
     * @param hiddenColumns   숨긴 칸. 값이 있으면 아랫줄에 적는다.
     * @param budgetNameWidth 예산 행에서 예산명을 적을 폭(금액 칸의 왼쪽까지)
     * @param amountRight     단의 왼쪽 끝에서 금액의 오른쪽 끝까지의 거리
     */
    private record Layout(double width, ReportTemplateTable table, List<Cell> cells, List<ReportColumn> hiddenColumns,
            double budgetNameWidth, double amountRight) {
    }

    WeeklyRecordSection(ReportStyle style) {
        this.regular = style.regular(FONT_SIZE);
        this.bold = style.bold(FONT_SIZE);
        Font taller = ReportText.lineHeight(bold) > ReportText.lineHeight(regular) ? bold : regular;
        this.lineHeight = style.lineHeight(taller);
        this.baseline = style.baselineOffset(taller);
    }

    /**
     * 현재 위치부터 주간 수입 내역과 주간 지출 내역을 좌우 두 단으로 그리고, 더 길게 내려간 단의 아래로 이동한다.
     *
     * @param incomeColumns  주간 수입 내역의 칸 순서와 숨김
     * @param expenseColumns 주간 지출 내역의 칸 순서와 숨김
     */
    void render(ReportCanvas canvas, List<ReportRecordGroupDto> incomeRecords,
            List<ReportRecordGroupDto> expenseRecords, List<ReportColumnDto> incomeColumns,
            List<ReportColumnDto> expenseColumns) {
        // 절 제목과 머리글만 페이지 끝에 남지 않도록, 내역이 몇 줄은 들어갈 공간이 없으면 다음 페이지에서 시작한다.
        if (!canvas.fits(lineHeight * 6)) {
            canvas.newPage();
        }

        double columnWidth = (canvas.getContentWidth() - BudgetStatusSection.COLUMN_GAP) / 2;
        double expenseLeft = canvas.getLeft() + columnWidth + BudgetStatusSection.COLUMN_GAP;
        int startPage = canvas.getPageIndex();
        double startY = canvas.getY();

        End incomeEnd = renderColumn(canvas, canvas.getLeft(), columnWidth, ReportTemplateTable.WEEKLY_INCOME_RECORDS,
                incomeColumns, incomeRecords);
        canvas.setPosition(startPage, startY);
        End expenseEnd = renderColumn(canvas, expenseLeft, columnWidth, ReportTemplateTable.WEEKLY_EXPENSE_RECORDS,
                expenseColumns, expenseRecords);

        if (incomeEnd.isAfter(expenseEnd)) {
            canvas.setPosition(incomeEnd.pageIndex(), incomeEnd.y());
        }
    }

    private End renderColumn(ReportCanvas canvas, double left, double width, ReportTemplateTable table,
            List<ReportColumnDto> columns, List<ReportRecordGroupDto> groups) {
        long total = 0;
        for (ReportRecordGroupDto group : groups) {
            total += group.getTotalAmount();
        }
        Layout layout = layOut(table, columns, groups, width, total);

        canvas.section(table.getLabel(), ReportFonts.SIZE_SECTION, left);
        drawHeader(canvas, left, layout);

        if (groups.isEmpty()) {
            canvas.text(EMPTY_MESSAGE, regular, ReportAlign.CENTER, left + width / 2, canvas.getY() + baseline,
                    UIColors.REPORT_TEXT_SUB);
            canvas.moveDown(lineHeight);
        }

        boolean atTop = true;
        for (ReportRecordGroupDto group : groups) {
            List<Block> blocks = toBlocks(group, layout);
            for (int i = 0; i < blocks.size(); i++) {
                Block block = blocks.get(i);
                // 예산과 예산 사이는 빈 줄로 띄운다. 머리글 바로 아래에서는 띄우지 않는다.
                double gap = i == 0 && !atTop ? lineHeight : 0;
                if (!canvas.fits(gap + heightOf(block))) {
                    continueOnNextPage(canvas, left, layout);
                    gap = 0;
                }
                canvas.moveDown(gap);
                drawBlock(canvas, left, width, block);
                atTop = false;
            }
        }

        if (!canvas.fits(lineHeight)) {
            continueOnNextPage(canvas, left, layout);
        }
        drawTotal(canvas, left, layout, total);
        return new End(canvas.getPageIndex(), canvas.getY());
    }

    /**
     * 칸의 폭을 정한다. 금액 칸까지는 머리글과 내용을 한 줄에 적는 폭(상한 안에서)이고, 첫 번째 칸은 예산 행의 예산명도 함께 잰다. 금액
     * 뒤에 칸이 있으면 한 글자 폭을 띄우고, 금액 뒤의 칸들이 남은 폭을 내용의 길이에 비례해 나눠 갖는다(머리글의 폭보다는 좁히지 않는다).
     * 금액이 마지막 칸이면 금액 칸이 남은 폭을 가져간다. 금액 뒤의 칸들에 남는 폭이 모자라면 금액 앞의 글자 칸들을 같은 비율로
     * 줄인다(머리글의 폭보다는 줄이지 않는다).
     */
    private Layout layOut(ReportTemplateTable table, List<ReportColumnDto> columns, List<ReportRecordGroupDto> groups,
            double width, long total) {
        List<ReportColumn> visible = new ArrayList<>();
        List<ReportColumn> hidden = new ArrayList<>();
        for (ReportColumnDto column : columns) {
            if (column.isHidden()) {
                hidden.add(column.getColumn());
            } else {
                visible.add(column.getColumn());
            }
        }

        int count = visible.size();
        int amountIndex = visible.indexOf(ReportColumn.AMOUNT);
        String[] headers = new String[count];
        double[] minWidths = new double[count];
        double[] naturalWidths = new double[count];
        double[] widths = new double[count];
        for (int i = 0; i < count; i++) {
            ReportColumn column = visible.get(i);
            headers[i] = i == 0 ? "일자 · " + table.labelOf(column) : table.labelOf(column);
            minWidths[i] = ReportText.width(headers[i], bold) + PADDING_X * 2;
            naturalWidths[i] = Math.max(minWidths[i], measureContent(column, i == 0, groups, total) + PADDING_X * 2);
            double max = column == ReportColumn.AMOUNT ? naturalWidths[i]
                    : width * (i == 0 ? FIRST_MAX_RATIO : TEXT_MAX_RATIO);
            widths[i] = Math.max(minWidths[i], Math.min(naturalWidths[i], max));
        }

        // 금액이 마지막 칸이면 뒤의 칸이 없으므로 간격 없이 금액 칸이 남은 폭을 가져간다.
        if (amountIndex == count - 1) {
            double used = 0;
            for (int i = 0; i < amountIndex; i++) {
                used += widths[i];
            }
            widths[amountIndex] = Math.max(widths[amountIndex], width - used);
            return toLayout(table, width, visible, hidden, headers, widths, 0);
        }

        double amountGap = ReportText.width(AMOUNT_GAP_TEXT, regular);
        double trailingMin = 0;
        for (int i = amountIndex + 1; i < count; i++) {
            trailingMin += minWidths[i];
        }
        trailingMin = Math.max(trailingMin, width * TRAILING_MIN_RATIO);

        double used = amountGap;
        double shrinkable = 0;
        for (int i = 0; i <= amountIndex; i++) {
            used += widths[i];
            if (i != amountIndex) {
                shrinkable += widths[i] - minWidths[i];
            }
        }
        double shortage = used + trailingMin - width;
        if (shortage > 0 && shrinkable > 0) {
            double ratio = Math.min(1, shortage / shrinkable);
            for (int i = 0; i < amountIndex; i++) {
                double reduced = (widths[i] - minWidths[i]) * ratio;
                widths[i] -= reduced;
                used -= reduced;
            }
        }

        divideByContent(widths, minWidths, naturalWidths, amountIndex + 1, Math.max(0, width - used));
        return toLayout(table, width, visible, hidden, headers, widths, amountGap);
    }

    /**
     * 남은 폭을 칸들에 내용의 길이에 비례해 나눠 준다. 비례로 나눈 폭이 머리글의 폭보다 좁은 칸은 머리글의 폭으로 두고 나머지를 다시
     * 나눈다.
     *
     * @param from  나눠 가질 첫 칸의 순번. 이 칸부터 마지막 칸까지 나눠 갖는다.
     * @param total 나눌 폭
     */
    private static void divideByContent(double[] widths, double[] minWidths, double[] naturalWidths, int from,
            double total) {
        boolean[] fixed = new boolean[widths.length];
        boolean changed = true;
        while (changed) {
            changed = false;
            double remaining = total;
            double weight = 0;
            for (int i = from; i < widths.length; i++) {
                if (fixed[i]) {
                    remaining -= minWidths[i];
                } else {
                    weight += naturalWidths[i];
                }
            }
            for (int i = from; i < widths.length; i++) {
                if (fixed[i]) {
                    widths[i] = minWidths[i];
                    continue;
                }
                widths[i] = weight > 0 ? Math.max(0, remaining) * naturalWidths[i] / weight : 0;
                if (widths[i] < minWidths[i]) {
                    fixed[i] = true;
                    changed = true;
                }
            }
        }
    }

    /**
     * 정한 폭으로 칸을 늘어놓는다.
     *
     * @param amountGap 금액 칸 뒤에 띄울 간격
     */
    private static Layout toLayout(ReportTemplateTable table, double width, List<ReportColumn> visible,
            List<ReportColumn> hidden, String[] headers, double[] widths, double amountGap) {
        List<Cell> cells = new ArrayList<>();
        double x = 0;
        double budgetNameWidth = 0;
        double amountRight = 0;
        for (int i = 0; i < visible.size(); i++) {
            Cell cell = new Cell(visible.get(i), headers[i], x, widths[i]);
            cells.add(cell);
            x += widths[i];
            if (cell.column() == ReportColumn.AMOUNT) {
                budgetNameWidth = cell.left();
                amountRight = cell.anchorX();
                x += amountGap;
            }
        }
        return new Layout(width, table, cells, hidden, budgetNameWidth, amountRight);
    }

    /**
     * 칸의 내용을 한 줄에 적는 데 필요한 폭(여백 제외). 금액 칸은 가장 큰 금액인 합계를 굵게 적는 폭이다.
     *
     * @param first 첫 번째 칸인지 여부. 첫 번째 칸은 일자를 붙인 내용과 예산 행의 예산명을 함께 잰다.
     */
    private double measureContent(ReportColumn column, boolean first, List<ReportRecordGroupDto> groups, long total) {
        if (column == ReportColumn.AMOUNT) {
            return ReportText.width(ReportFormats.amount(total), bold);
        }

        double width = 0;
        for (ReportRecordGroupDto group : groups) {
            if (first) {
                width = Math.max(width, ReportText.width(group.getBudgetName(), bold));
            }
            for (ReportRecordDto record : group.getRecords()) {
                String text = first ? withDate(record, valueOf(record, column)) : valueOf(record, column);
                if (text != null) {
                    width = Math.max(width, ReportText.width(text, regular));
                }
            }
        }
        return width;
    }

    private double heightOf(Block block) {
        return block.lines()
                .size() * lineHeight;
    }

    /**
     * 단의 아래를 선으로 닫고 다음 페이지의 같은 단에서 머리글부터 다시 시작한다.
     */
    private void continueOnNextPage(ReportCanvas canvas, double left, Layout layout) {
        canvas.line(left, canvas.getY(), left + layout.width(), canvas.getY(), STRONG_LINE, UIColors.REPORT_LINE);
        canvas.newPage();
        drawHeader(canvas, left, layout);
    }

    private void drawHeader(ReportCanvas canvas, double left, Layout layout) {
        double top = canvas.getY();
        double height = lineHeight + HEADER_PADDING * 2;
        double right = left + layout.width();
        double headerBaseline = top + HEADER_PADDING + baseline;

        canvas.fillRect(left, top, layout.width(), height, UIColors.REPORT_FILL);
        canvas.line(left, top, right, top, STRONG_LINE, UIColors.REPORT_LINE);
        canvas.line(left, top + height, right, top + height, HEADER_LINE, UIColors.REPORT_LINE);

        for (Cell cell : layout.cells()) {
            canvas.text(cell.header(), bold, ReportAlign.CENTER, left + cell.left() + cell.width() / 2,
                    headerBaseline);
        }
        canvas.moveDown(height);
    }

    private void drawBlock(ReportCanvas canvas, double left, double width, Block block) {
        for (RowLine line : block.lines()) {
            double top = canvas.getY();
            if (line.background() != null) {
                canvas.fillRect(left, top, width, lineHeight, line.background());
            }
            for (Fragment fragment : line.fragments()) {
                canvas.text(fragment.text(), fragment.font(), fragment.align(), left + fragment.anchorX(),
                        top + baseline, fragment.color());
            }
            canvas.moveDown(lineHeight);
        }
    }

    private void drawTotal(ReportCanvas canvas, double left, Layout layout, long total) {
        double top = canvas.getY();
        double right = left + layout.width();

        canvas.line(left, top, right, top, HEADER_LINE, UIColors.REPORT_LINE);
        canvas.text("합계", bold, ReportAlign.LEFT, left + PADDING_X, top + baseline);
        canvas.text(ReportFormats.amount(total), bold, ReportAlign.RIGHT, left + layout.amountRight(),
                top + baseline);
        canvas.moveDown(lineHeight);
        canvas.line(left, canvas.getY(), right, canvas.getY(), STRONG_LINE, UIColors.REPORT_LINE);
    }

    /**
     * 한 예산의 내역을 페이지 넘김 단위의 묶음으로 바꾼다. 예산 행은 첫 내역과 한 묶음이어서 예산 행만 페이지 끝에 남는 일이 없고, 내역은
     * 건마다(줄어든 내용을 다시 적는 줄 포함) 한 묶음이다. 내역의 바탕은 예산 행 바로 아래의 첫 내역을 흰 바탕으로 해서 한 건씩 번갈아
     * 깐다.
     */
    private List<Block> toBlocks(ReportRecordGroupDto group, Layout layout) {
        List<Block> blocks = new ArrayList<>();
        List<RowLine> lines = new ArrayList<>(toBudgetLines(group, layout));

        List<ReportRecordDto> records = group.getRecords();
        for (int i = 0; i < records.size(); i++) {
            Color background = i % 2 == 0 ? null : UIColors.REPORT_FILL;
            lines.addAll(toRecordLines(records.get(i), layout, background));
            blocks.add(new Block(lines));
            lines = new ArrayList<>();
        }
        if (!lines.isEmpty()) {
            blocks.add(new Block(lines));
        }
        return blocks;
    }

    /**
     * 예산 행. 전체 예산명을 금액 칸의 왼쪽까지 굵게 적고 금액 칸에 소계를 적는다.
     */
    private List<RowLine> toBudgetLines(ReportRecordGroupDto group, Layout layout) {
        Color background = UIColors.REPORT_FILL_STRONG;
        List<RowLine> continuations = new ArrayList<>();
        String name = fitCell(group.getBudgetName(), bold, layout.budgetNameWidth(), "예산", group.getBudgetName(),
                layout, background, continuations);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new Fragment(name, bold, ReportAlign.LEFT, PADDING_X, UIColors.REPORT_TEXT));
        fragments.add(new Fragment(ReportFormats.amount(group.getTotalAmount()), bold, ReportAlign.RIGHT,
                layout.amountRight(), UIColors.REPORT_TEXT));

        List<RowLine> lines = new ArrayList<>();
        lines.add(new RowLine(fragments, background));
        lines.addAll(continuations);
        return lines;
    }

    /**
     * 내역 행. 보이는 칸을 순서대로 한 줄에 적고(첫 번째 칸 앞에 "[10/4]"처럼 일자를 붙인다), 줄어든 칸과 값이 있는 숨긴 칸은 그
     * 내용을 아랫줄들에 다시 적는다.
     */
    private List<RowLine> toRecordLines(ReportRecordDto record, Layout layout, Color background) {
        List<RowLine> continuations = new ArrayList<>();
        List<Fragment> fragments = new ArrayList<>();

        for (int i = 0; i < layout.cells()
                .size(); i++) {
            Cell cell = layout.cells()
                    .get(i);
            String value = valueOf(record, cell.column());
            // 첫 번째 칸이 넘쳐서 줄어드는 것은 일자 뒤의 값이므로 아랫줄에는 일자 없이 값만 다시 적는다.
            String text = i == 0 ? withDate(record, value) : value;
            String fitted = fitCell(text, regular, cell.width(), layout.table()
                    .labelOf(cell.column()), value, layout, background, continuations);
            fragments.add(new Fragment(fitted, regular, cell.align(), cell.anchorX(), UIColors.REPORT_TEXT));
        }
        for (ReportColumn column : layout.hiddenColumns()) {
            addContinuation(layout.table()
                    .labelOf(column), valueOf(record, column), layout, background, continuations);
        }

        List<RowLine> lines = new ArrayList<>();
        lines.add(new RowLine(fragments, background));
        lines.addAll(continuations);
        return lines;
    }

    private static String valueOf(ReportRecordDto record, ReportColumn column) {
        return switch (column) {
        case CONTACT -> String.join(", ", record.getContactNames());
        case DESCRIPTION -> record.getDescription();
        case AMOUNT -> ReportFormats.amount(record.getAmount());
        case REMARKS -> record.getRemarks();
        default -> throw new IllegalArgumentException("주간 내역에 없는 칸입니다: " + column);
        };
    }

    private static String withDate(ReportRecordDto record, String value) {
        String date = "[" + record.getDate()
                .format(DATE_FORMAT) + "]";
        return value == null || value.isBlank() ? date : date + " " + value;
    }

    /**
     * 칸에 넣을 글자를 정한다. 칸에 다 들어가면 그대로 쓰고, 넘치면 말줄임표로 줄인 뒤 전체 내용을 다시 적는 줄을 추가한다.
     *
     * @param label         다시 적는 줄 앞에 붙여 어느 칸의 내용인지 알리는 이름
     * @param fullText      다시 적을 전체 내용
     * @param background    다시 적는 줄의 바탕. 그 행과 같은 바탕이다.
     * @param continuations 다시 적는 줄을 추가할 목록
     * @return 칸에 넣을 글자
     */
    private String fitCell(String text, Font font, double cellWidth, String label, String fullText, Layout layout,
            Color background, List<RowLine> continuations) {
        if (text == null || text.isBlank()) {
            return "";
        }

        double availableWidth = cellWidth - PADDING_X * 2;
        if (ReportText.width(text, font) <= availableWidth) {
            return text;
        }

        addContinuation(label, fullText, layout, background, continuations);
        return ReportText.clip(text, font, availableWidth);
    }

    /**
     * "내용: …"처럼 칸 이름을 붙여 내용을 단의 전체 폭으로 다시 적는 줄을 추가한다. 내용이 없으면 추가하지 않는다.
     */
    private void addContinuation(String label, String fullText, Layout layout, Color background,
            List<RowLine> continuations) {
        if (fullText == null || fullText.isBlank()) {
            return;
        }
        double continuationWidth = layout.width() - CONTINUATION_INDENT - PADDING_X;
        for (String line : ReportText.wrap(label + ": " + fullText, regular, continuationWidth)) {
            continuations.add(new RowLine(List.of(new Fragment(line, regular, ReportAlign.LEFT, CONTINUATION_INDENT,
                    UIColors.REPORT_TEXT_SUB)), background));
        }
    }
}
