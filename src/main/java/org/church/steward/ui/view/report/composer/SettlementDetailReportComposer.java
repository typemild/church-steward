package org.church.steward.ui.view.report.composer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 회계 연도 결산 세부 내역서를 배치한다. 수입 결산 세부 내역서와 지출 결산 세부 내역서를 차례로 묶은 보고서이며, 지출 내역서는 새
 * 페이지에서 시작한다. 두 내역서는 구분만 다르고 구성이 같다. 제목은 "회계 연도 결산 세부 내역서 (수입)"처럼 구분을 붙이고 그 아래에
 * 회계 연도의 이름과 기간을 적으며, 예산 표({@link BudgetListTable})의 칸 순서는 보고서
 * 양식({@link ReportTemplateDto})이 구분마다 정한다. 수입은 예산을 채운 정도(달성), 지출은 예산을 쓴 정도(집행)로
 * 부른다.
 */
public final class SettlementDetailReportComposer {

    private static final String TITLE = "회계 연도 결산 세부 내역서";

    private SettlementDetailReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(SettlementDetailReportDto income, SettlementDetailReportDto expense,
            ReportTemplateDto template, ReportStyle style) {
        ReportCanvas canvas = new ReportCanvas(String.format("%s %s", income.getFiscalYearName(), TITLE), style);
        draw(canvas, income, template);
        canvas.startNextPart();
        draw(canvas, expense, template);
        return canvas.finish();
    }

    /**
     * 수입 결산 세부 내역서나 지출 결산 세부 내역서 하나를 현재 위치부터 배치한다.
     */
    static void draw(ReportCanvas canvas, SettlementDetailReportDto report, ReportTemplateDto template) {
        String title = String.format("%s (%s)", TITLE, report.getType()
                .getLabel());

        ReportStyles.drawAnnualTitle(canvas, title, report.getFiscalYearName(), report.getFiscalYearStartDate(),
                report.getFiscalYearEndDate());
        ReportStyles.drawUnit(canvas, ReportStyles.BUDGET_TABLE_UNIT_SIZE);

        List<BudgetListTable.Row> rows = new ArrayList<>();
        for (BudgetExecutionDto row : report.getRows()) {
            // 최상위 예산은 굵게 써서 그 아래 하위 예산들과 구분한다.
            rows.add(new BudgetListTable.Row(row.getDepth() == 0 ? RowStyle.EMPHASIS : RowStyle.NORMAL,
                    row.getDepth(), values(row.getName(), ReportFormats.amount(row.getBudgetedAmount()),
                            ReportFormats.rate(row.getShareRate()), ReportFormats.amount(row.getActualAmount()),
                            ReportFormats.rate(row.getExecutionRate()), row.getRemarks())));
        }
        rows.add(new BudgetListTable.Row(RowStyle.TOTAL, 0, values("총계",
                ReportFormats.amount(report.getTotalBudgetedAmount()),
                report.getTotalBudgetedAmount() > 0 ? ReportFormats.rate(100.0) : ReportFormats.NONE,
                ReportFormats.amount(report.getTotalActualAmount()),
                ReportFormats.rate(report.getTotalExecutionRate()), null)));

        ReportTemplateTable table = report.getType() == BudgetType.INCOME
                ? ReportTemplateTable.INCOME_SETTLEMENT_DETAIL
                : ReportTemplateTable.EXPENSE_SETTLEMENT_DETAIL;
        BudgetListTable.render(canvas, table, template.getTables()
                .get(table), rows);
    }

    private static Map<ReportColumn, String> values(String name, String budgetedAmount, String shareRate,
            String actualAmount, String executionRate, String remarks) {
        Map<ReportColumn, String> values = new EnumMap<>(ReportColumn.class);
        values.put(ReportColumn.BUDGET_NAME, name);
        values.put(ReportColumn.BUDGETED_AMOUNT, budgetedAmount);
        values.put(ReportColumn.SHARE_RATE, shareRate);
        values.put(ReportColumn.ACTUAL_AMOUNT, actualAmount);
        values.put(ReportColumn.RATE, executionRate);
        values.put(ReportColumn.REMARKS, remarks);
        return values;
    }
}
