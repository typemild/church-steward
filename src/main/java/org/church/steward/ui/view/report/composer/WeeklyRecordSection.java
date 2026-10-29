package org.church.steward.ui.view.report.composer;

import java.awt.Color;
import java.awt.Font;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 주간 회계 결산 보고서의 수입 내역과 지출 내역. "수입/지출 관리"의 "예산별 보기"처럼 왼쪽 단에 수입, 오른쪽 단에 지출을 놓고,
 * 예산 행 아래에 그 예산의 내역 행들을 둔다.
 * <p>
 * 한 장에 많이 담기 위해 글자를 작게 쓰고 내역은 한 줄에 한 건씩 적는다. 줄 높이는 서식의 행 간격을 따른다. 칸에 다 들어가지 않는
 * 내용은 말줄임표로 줄이되, 바로 아랫줄에 들여써서 단의 전체 폭으로 내용을 모두 적는다(필요하면 여러 줄). 줄어든 칸마다 따로 줄을
 * 차지하므로 어떤 내용도 잘린 채로 끝나지 않는다.
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
    /** 금액을 뺀 나머지 폭을 일자·관계자, 내용, 비고가 나눠 갖는 비율 */
    private static final double NAME_RATIO = 0.45;
    private static final double DESCRIPTION_RATIO = 0.30;
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
     * @param filled 회색 바탕을 까는 줄(예산 행)인지 여부
     */
    private record RowLine(List<Fragment> fragments, boolean filled) {
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
     * 한 단의 열 구성
     *
     * @param width        단의 폭
     * @param nameWidth    일자·관계자 열의 폭
     * @param descWidth    내용 열의 폭
     * @param amountWidth  금액 열의 폭
     * @param contactLabel 관계자를 부르는 이름("봉헌자" 또는 "수취인")
     */
    private record Layout(double width, double nameWidth, double descWidth, double amountWidth,
            String contactLabel) {

        double amountRight() {
            return nameWidth + descWidth + amountWidth - PADDING_X;
        }

        double remarksLeft() {
            return nameWidth + descWidth + amountWidth;
        }

        double remarksWidth() {
            return width - remarksLeft();
        }
    }

    WeeklyRecordSection(ReportStyle style) {
        this.regular = style.regular(FONT_SIZE);
        this.bold = style.bold(FONT_SIZE);
        Font taller = ReportText.lineHeight(bold) > ReportText.lineHeight(regular) ? bold : regular;
        this.lineHeight = style.lineHeight(taller);
        this.baseline = style.baselineOffset(taller);
    }

    /**
     * 현재 위치부터 수입 내역과 지출 내역을 좌우 두 단으로 그리고, 더 길게 내려간 단의 아래로 이동한다.
     */
    void render(ReportCanvas canvas, List<ReportRecordGroupDto> incomeRecords,
            List<ReportRecordGroupDto> expenseRecords) {
        // 절 제목과 머리글만 페이지 끝에 남지 않도록, 내역이 몇 줄은 들어갈 공간이 없으면 다음 페이지에서 시작한다.
        if (!canvas.fits(lineHeight * 6)) {
            canvas.newPage();
        }

        double columnWidth = (canvas.getContentWidth() - BudgetStatusSection.COLUMN_GAP) / 2;
        double expenseLeft = canvas.getLeft() + columnWidth + BudgetStatusSection.COLUMN_GAP;
        int startPage = canvas.getPageIndex();
        double startY = canvas.getY();

        End incomeEnd = renderColumn(canvas, canvas.getLeft(), columnWidth, "수입 내역", "봉헌자", incomeRecords);
        canvas.setPosition(startPage, startY);
        End expenseEnd = renderColumn(canvas, expenseLeft, columnWidth, "지출 내역", "수취인", expenseRecords);

        if (incomeEnd.isAfter(expenseEnd)) {
            canvas.setPosition(incomeEnd.pageIndex(), incomeEnd.y());
        }
    }

    private End renderColumn(ReportCanvas canvas, double left, double width, String title, String contactLabel,
            List<ReportRecordGroupDto> groups) {
        long total = 0;
        for (ReportRecordGroupDto group : groups) {
            total += group.getTotalAmount();
        }

        // 금액 열은 가장 큰 금액(합계)이 들어갈 만큼만 잡고 나머지를 글자 열에 준다.
        double amountWidth = PADDING_X * 2 + Math.max(ReportText.width(ReportFormats.amount(total), bold),
                ReportText.width("금액", bold));
        double textWidth = width - amountWidth;
        Layout layout = new Layout(width, textWidth * NAME_RATIO, textWidth * DESCRIPTION_RATIO, amountWidth,
                contactLabel);

        canvas.section(title, ReportFonts.SIZE_SECTION, left);
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

        canvas.text("일자 · " + layout.contactLabel(), bold, ReportAlign.CENTER, left + layout.nameWidth() / 2,
                headerBaseline);
        canvas.text("내용", bold, ReportAlign.CENTER, left + layout.nameWidth() + layout.descWidth() / 2,
                headerBaseline);
        canvas.text("금액", bold, ReportAlign.CENTER,
                left + layout.nameWidth() + layout.descWidth() + layout.amountWidth() / 2, headerBaseline);
        canvas.text("비고", bold, ReportAlign.CENTER, left + layout.remarksLeft() + layout.remarksWidth() / 2,
                headerBaseline);
        canvas.moveDown(height);
    }

    private void drawBlock(ReportCanvas canvas, double left, double width, Block block) {
        for (RowLine line : block.lines()) {
            double top = canvas.getY();
            if (line.filled()) {
                canvas.fillRect(left, top, width, lineHeight, UIColors.REPORT_FILL);
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
     * 건마다(줄어든 내용을 다시 적는 줄 포함) 한 묶음이다.
     */
    private List<Block> toBlocks(ReportRecordGroupDto group, Layout layout) {
        List<Block> blocks = new ArrayList<>();
        List<RowLine> lines = new ArrayList<>(toBudgetLines(group, layout));

        for (ReportRecordDto record : group.getRecords()) {
            lines.addAll(toRecordLines(record, layout));
            blocks.add(new Block(lines));
            lines = new ArrayList<>();
        }
        if (!lines.isEmpty()) {
            blocks.add(new Block(lines));
        }
        return blocks;
    }

    /**
     * 예산 행. 전체 예산명을 일자·관계자 열과 내용 열에 걸쳐 굵게 적고 금액 열에 소계를 적는다.
     */
    private List<RowLine> toBudgetLines(ReportRecordGroupDto group, Layout layout) {
        List<RowLine> continuations = new ArrayList<>();
        String name = fitCell(group.getBudgetName(), bold, layout.nameWidth() + layout.descWidth(), "예산", layout,
                continuations);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new Fragment(name, bold, ReportAlign.LEFT, PADDING_X, UIColors.REPORT_TEXT));
        fragments.add(new Fragment(ReportFormats.amount(group.getTotalAmount()), bold, ReportAlign.RIGHT,
                layout.amountRight(), UIColors.REPORT_TEXT));

        List<RowLine> lines = new ArrayList<>();
        lines.add(new RowLine(fragments, true));
        lines.addAll(continuations);
        return lines;
    }

    /**
     * 내역 행. "[10/4] 홍길동, 김철수 | 내용 | 금액 | 비고"를 한 줄에 적고, 줄어든 칸이 있으면 그 내용을 아랫줄들에 다시
     * 적는다.
     */
    private List<RowLine> toRecordLines(ReportRecordDto record, Layout layout) {
        List<RowLine> continuations = new ArrayList<>();

        String date = "[" + record.getDate()
                .format(DATE_FORMAT) + "]";
        String names = String.join(", ", record.getContactNames());
        String dateAndNames = names.isEmpty() ? date : date + " " + names;
        // 이 칸이 넘쳐서 줄어드는 것은 관계자 이름이므로 아랫줄에는 날짜 없이 이름만 다시 적는다.
        String nameCell = fitCell(dateAndNames, regular, layout.nameWidth(), layout.contactLabel(), names, layout,
                continuations);
        String description = fitCell(record.getDescription(), regular, layout.descWidth(), "내용", layout,
                continuations);
        String remarks = fitCell(record.getRemarks(), regular, layout.remarksWidth(), "비고", layout, continuations);

        List<Fragment> fragments = new ArrayList<>();
        fragments.add(new Fragment(nameCell, regular, ReportAlign.LEFT, PADDING_X, UIColors.REPORT_TEXT));
        fragments.add(new Fragment(description, regular, ReportAlign.LEFT, layout.nameWidth() + PADDING_X,
                UIColors.REPORT_TEXT));
        fragments.add(new Fragment(ReportFormats.amount(record.getAmount()), regular, ReportAlign.RIGHT,
                layout.amountRight(), UIColors.REPORT_TEXT));
        fragments.add(new Fragment(remarks, regular, ReportAlign.LEFT, layout.remarksLeft() + PADDING_X,
                UIColors.REPORT_TEXT));

        List<RowLine> lines = new ArrayList<>();
        lines.add(new RowLine(fragments, false));
        lines.addAll(continuations);
        return lines;
    }

    private String fitCell(String text, Font font, double cellWidth, String label, Layout layout,
            List<RowLine> continuations) {
        return fitCell(text, font, cellWidth, label, text, layout, continuations);
    }

    /**
     * 칸에 넣을 글자를 정한다. 칸에 다 들어가면 그대로 쓰고, 넘치면 말줄임표로 줄인 뒤 전체 내용을 다시 적는 줄을 추가한다.
     *
     * @param label         다시 적는 줄 앞에 붙여 어느 칸의 내용인지 알리는 이름
     * @param fullText      다시 적을 전체 내용
     * @param continuations 다시 적는 줄을 추가할 목록
     * @return 칸에 넣을 글자
     */
    private String fitCell(String text, Font font, double cellWidth, String label, String fullText, Layout layout,
            List<RowLine> continuations) {
        if (text == null || text.isBlank()) {
            return "";
        }

        double availableWidth = cellWidth - PADDING_X * 2;
        if (ReportText.width(text, font) <= availableWidth) {
            return text;
        }

        double continuationWidth = layout.width() - CONTINUATION_INDENT - PADDING_X;
        for (String line : ReportText.wrap(label + ": " + fullText, regular, continuationWidth)) {
            continuations.add(new RowLine(List.of(new Fragment(line, regular, ReportAlign.LEFT, CONTINUATION_INDENT,
                    UIColors.REPORT_TEXT_SUB)), false));
        }
        return ReportText.clip(text, font, availableWidth);
    }
}
