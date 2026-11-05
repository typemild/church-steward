package org.church.steward.ui.view.report.composer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.church.steward.ui.view.report.layout.ReportTable.RowStyle;

/**
 * 회계 연도 예산안을 배치한다. 수입 예산안과 지출 예산안을 차례로 묶은 보고서이며, 지출 예산안은 새 페이지에서 시작한다. 두 예산안은
 * 구분만 다르고 구성이 같다. 제목은 "회계 연도 예산안 (수입)"처럼 구분을 붙이고 그 아래에 회계 연도의 이름과 기간을 적으며, 예산
 * 표({@link BudgetListTable})의 칸 순서는 보고서 양식({@link ReportTemplateDto})이 구분마다
 * 정한다.
 */
public final class BudgetPlanReportComposer {

    private static final String TITLE = "회계 연도 예산안";
    private static final String NEW_BUDGET = "신규";

    private BudgetPlanReportComposer() {
        // 인스턴스화 방지
    }

    public static ReportDocument compose(BudgetPlanReportDto income, BudgetPlanReportDto expense,
            ReportTemplateDto template, ReportStyle style) {
        ReportCanvas canvas = new ReportCanvas(String.format("%s %s", income.getFiscalYearName(), TITLE), style);
        draw(canvas, income, template);
        canvas.startNextPart();
        draw(canvas, expense, template);
        return canvas.finish();
    }

    /**
     * 수입 예산안이나 지출 예산안 하나를 현재 위치부터 배치한다.
     */
    static void draw(ReportCanvas canvas, BudgetPlanReportDto report, ReportTemplateDto template) {
        String title = String.format("%s (%s)", TITLE, report.getType()
                .getLabel());

        ReportStyles.drawAnnualTitle(canvas, title, report.getFiscalYearName(), report.getFiscalYearStartDate(),
                report.getFiscalYearEndDate());
        ReportStyles.drawUnit(canvas, ReportStyles.BUDGET_TABLE_UNIT_SIZE);

        List<BudgetListTable.Row> rows = new ArrayList<>();
        for (BudgetPlanRowDto row : report.getRows()) {
            // 최상위 예산은 굵게 써서 그 아래 하위 예산들과 구분한다.
            rows.add(new BudgetListTable.Row(row.getDepth() == 0 ? RowStyle.EMPHASIS : RowStyle.NORMAL,
                    row.getDepth(), values(row.getName(), ReportFormats.amount(row.getAmount()),
                            ReportFormats.rate(row.getShareRate()), change(row.getChange()), row.getRemarks()),
                    centeredOf(row.getChange())));
        }
        if (report.getRemovedAmount() != null) {
            // 이번 회계 연도에는 없는 예산이므로 예산액과 점유율은 비우고, 없어진 만큼을 감소로 적는다.
            rows.add(new BudgetListTable.Row(RowStyle.NORMAL, 0, values("직전 회계 연도에만 있던 예산 (합계)",
                    ReportFormats.NONE, ReportFormats.NONE, ReportFormats.signedAmount(-report.getRemovedAmount()),
                    null)));
        }
        rows.add(new BudgetListTable.Row(RowStyle.TOTAL, 0, values("총계",
                ReportFormats.amount(report.getTotalAmount()),
                report.getTotalAmount() > 0 ? ReportFormats.rate(100.0) : ReportFormats.NONE,
                change(report.getTotalChange()), null), centeredOf(report.getTotalChange())));

        ReportTemplateTable table = report.getType() == BudgetType.INCOME ? ReportTemplateTable.INCOME_BUDGET_PLAN
                : ReportTemplateTable.EXPENSE_BUDGET_PLAN;
        BudgetListTable.render(canvas, table, template.getTables()
                .get(table), rows);
    }

    private static Map<ReportColumn, String> values(String name, String amount, String shareRate, String change,
            String remarks) {
        Map<ReportColumn, String> values = new EnumMap<>(ReportColumn.class);
        values.put(ReportColumn.BUDGET_NAME, name);
        values.put(ReportColumn.BUDGETED_AMOUNT, amount);
        values.put(ReportColumn.SHARE_RATE, shareRate);
        values.put(ReportColumn.CHANGE, change);
        values.put(ReportColumn.REMARKS, remarks);
        return values;
    }

    /**
     * 직전 회계 연도에 같은 예산이 없으면(null) "신규"로 적는다.
     */
    private static String change(Long change) {
        return change == null ? NEW_BUDGET : ReportFormats.signedAmount(change);
    }

    /**
     * 증감액 대신 "신규"를 적는 행은 증감액 칸을 가운데 정렬한다. 숫자처럼 오른쪽에 붙이면 금액으로 잘못 읽기 쉽다.
     */
    private static Set<ReportColumn> centeredOf(Long change) {
        return change == null ? Set.of(ReportColumn.CHANGE) : Set.of();
    }
}
