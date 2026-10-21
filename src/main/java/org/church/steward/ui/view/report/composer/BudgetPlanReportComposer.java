package org.church.steward.ui.view.report.composer;

import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 회계 연도 수입 예산안과 회계 연도 지출 예산안을 배치한다. 두 보고서는 구분만 다르고 구성이 같다.
 */
public final class BudgetPlanReportComposer {

    private static final String NEW_BUDGET = "신규";

    private static final double AMOUNT_WIDTH = ReportPaper.mm(38);
    private static final double RATE_WIDTH = ReportPaper.mm(22);

    private BudgetPlanReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(BudgetPlanReportDto report) {
        String title = String.format("%s 회계 연도 %s 예산안", report.getFiscalYearName(), report.getType()
                .getLabel());

        ReportCanvas canvas = new ReportCanvas(title);
        canvas.title(title, ReportStyles.ANNUAL_TITLE_SIZE);
        canvas.moveDown(ReportStyles.TITLE_GAP);
        ReportStyles.drawUnit(canvas, ReportStyles.BUDGET_TABLE_UNIT_SIZE);

        ReportTable table = new ReportTable(ReportStyles.BUDGET_TABLE_SIZE,
                new Column("예산 항목", 0, ReportAlign.LEFT),
                new Column("예산액", AMOUNT_WIDTH, ReportAlign.RIGHT),
                new Column("점유율", RATE_WIDTH, ReportAlign.RIGHT),
                // "직전 회계 연도 대비 증감액"은 칸 폭에 들어가지 않아 줄여 적는다.
                new Column("직전 대비 증감액", AMOUNT_WIDTH, ReportAlign.RIGHT));

        for (BudgetPlanRowDto row : report.getRows()) {
            // 최상위 예산은 굵게 써서 그 아래 하위 예산들과 구분한다.
            table.addRow(row.getDepth() == 0 ? RowStyle.EMPHASIS : RowStyle.NORMAL, row.getDepth(),
                    row.getName(),
                    ReportFormats.amount(row.getAmount()),
                    ReportFormats.rate(row.getShareRate()),
                    change(row.getChange()));
        }
        if (report.getRemovedAmount() != null) {
            // 이번 회계 연도에는 없는 예산이므로 예산액과 점유율은 비우고, 없어진 만큼을 감소로 적는다.
            table.addRow(RowStyle.NORMAL, 0,
                    "직전 회계 연도에만 있던 예산 (합계)",
                    ReportFormats.NONE,
                    ReportFormats.NONE,
                    ReportFormats.signedAmount(-report.getRemovedAmount()));
        }
        table.addRow(RowStyle.TOTAL, 0,
                "총계",
                ReportFormats.amount(report.getTotalAmount()),
                report.getTotalAmount() > 0 ? ReportFormats.rate(100.0) : ReportFormats.NONE,
                change(report.getTotalChange()));

        table.render(canvas);
        return canvas.finish();
    }

    /**
     * 직전 회계 연도에 같은 예산이 없으면(null) "신규"로 적는다.
     */
    private static String change(Long change) {
        return change == null ? NEW_BUDGET : ReportFormats.signedAmount(change);
    }
}
