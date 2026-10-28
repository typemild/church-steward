package org.church.steward.ui.view.report.composer;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable;
import org.church.steward.ui.view.report.layout.ReportTable.Column;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 회계 연도 수입 결산 세부 내역서와 회계 연도 지출 결산 세부 내역서를 배치한다. 두 보고서는 구분만 다르고 구성이 같다.
 */
public final class SettlementDetailReportComposer {

    private static final double AMOUNT_WIDTH = ReportPaper.mm(34);
    private static final double RATE_WIDTH = ReportPaper.mm(20);

    private SettlementDetailReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(SettlementDetailReportDto report, ReportStyle style) {
        boolean income = report.getType() == BudgetType.INCOME;
        String title = String.format("%s 회계 연도 %s 결산 세부 내역서", report.getFiscalYearName(), report.getType()
                .getLabel());

        ReportCanvas canvas = new ReportCanvas(title, style);
        canvas.title(title, ReportStyles.ANNUAL_TITLE_SIZE);
        canvas.moveDown(ReportStyles.TITLE_GAP);
        ReportStyles.drawUnit(canvas, ReportStyles.BUDGET_TABLE_UNIT_SIZE);

        // 수입은 예산을 채운 정도(달성), 지출은 예산을 쓴 정도(집행)로 부른다.
        ReportTable table = new ReportTable(style, ReportStyles.BUDGET_TABLE_SIZE,
                new Column("예산 항목", 0, ReportAlign.LEFT),
                new Column("예산액", AMOUNT_WIDTH, ReportAlign.RIGHT),
                new Column("점유율", RATE_WIDTH, ReportAlign.RIGHT),
                new Column(income ? "수입액" : "집행액", AMOUNT_WIDTH, ReportAlign.RIGHT),
                new Column(income ? "달성률" : "집행률", RATE_WIDTH, ReportAlign.RIGHT));

        for (BudgetExecutionDto row : report.getRows()) {
            // 최상위 예산은 굵게 써서 그 아래 하위 예산들과 구분한다.
            table.addRow(row.getDepth() == 0 ? RowStyle.EMPHASIS : RowStyle.NORMAL, row.getDepth(),
                    row.getName(),
                    ReportFormats.amount(row.getBudgetedAmount()),
                    ReportFormats.rate(row.getShareRate()),
                    ReportFormats.amount(row.getActualAmount()),
                    ReportFormats.rate(row.getExecutionRate()));
        }
        table.addRow(RowStyle.TOTAL, 0,
                "총계",
                ReportFormats.amount(report.getTotalBudgetedAmount()),
                report.getTotalBudgetedAmount() > 0 ? ReportFormats.rate(100.0) : ReportFormats.NONE,
                ReportFormats.amount(report.getTotalActualAmount()),
                ReportFormats.rate(report.getTotalExecutionRate()));

        table.render(canvas);
        return canvas.finish();
    }
}
