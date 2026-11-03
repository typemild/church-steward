package org.church.steward.domain.report.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.Getter;

/**
 * 보고서 양식에서 칸의 순서와 숨김을 정할 수 있는 표. 표마다 칸의 구성과 기본 순서, 첫 번째 칸에 둘 수 있는 칸, 숨길 수 있는 칸이
 * 정해져 있다. 첫 번째 칸은 숨길 수 없다.
 */
@Getter
public enum ReportTemplateTable {

    WEEKLY_INCOME_STATUS(ReportType.WEEKLY_SETTLEMENT, "수입 현황", BudgetType.INCOME,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    WEEKLY_EXPENSE_STATUS(ReportType.WEEKLY_SETTLEMENT, "지출 현황", BudgetType.EXPENSE,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    WEEKLY_INCOME_RECORDS(ReportType.WEEKLY_SETTLEMENT, "주간 수입 내역", BudgetType.INCOME,
            List.of(ReportColumn.CONTACT, ReportColumn.AMOUNT, ReportColumn.DESCRIPTION, ReportColumn.REMARKS),
            Set.of(ReportColumn.CONTACT, ReportColumn.DESCRIPTION),
            Set.of(ReportColumn.DESCRIPTION, ReportColumn.REMARKS)),
    WEEKLY_EXPENSE_RECORDS(ReportType.WEEKLY_SETTLEMENT, "주간 지출 내역", BudgetType.EXPENSE,
            List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.REMARKS),
            Set.of(ReportColumn.CONTACT, ReportColumn.DESCRIPTION),
            Set.of(ReportColumn.DESCRIPTION, ReportColumn.REMARKS));

    /** 이 표가 들어가는 보고서 */
    private final ReportType reportType;
    /** 표의 이름. 보고서의 절 제목과 같다. */
    private final String label;
    /** 수입의 표인지 지출의 표인지. 칸의 이름("수입액"/"집행액", "봉헌자"/"수취인")을 정한다. */
    private final BudgetType budgetType;
    /** 모든 칸의 기본 순서. 기본으로는 숨긴 칸이 없다. */
    private final List<ReportColumn> defaultColumns;
    /** 첫 번째 칸에 둘 수 있는 칸 */
    private final Set<ReportColumn> firstColumnCandidates;
    /** 숨길 수 있는 칸. 이 칸이라도 첫 번째 칸이면 숨길 수 없다. */
    private final Set<ReportColumn> hideableColumns;

    ReportTemplateTable(ReportType reportType, String label, BudgetType budgetType, List<ReportColumn> defaultColumns,
            Set<ReportColumn> firstColumnCandidates, Set<ReportColumn> hideableColumns) {
        this.reportType = reportType;
        this.label = label;
        this.budgetType = budgetType;
        this.defaultColumns = defaultColumns;
        this.firstColumnCandidates = firstColumnCandidates;
        this.hideableColumns = hideableColumns;
    }

    /**
     * 보고서에 들어가는 표들. 양식 설정 화면에 보일 순서다.
     */
    public static List<ReportTemplateTable> of(ReportType reportType) {
        return Arrays.stream(values())
                .filter(table -> table.reportType == reportType)
                .toList();
    }

    /**
     * 비율 칸(달성률/집행률)의 왼쪽에 막대를 함께 그릴지 정할 수 있는 표인지 여부. 비율 칸이 있는 표(현황 표)이며, 이 표는 모든 칸이
     * 폭을 고르게 나눠 갖는다.
     */
    public boolean hasRateBarOption() {
        return defaultColumns.contains(ReportColumn.RATE);
    }

    /**
     * 칸이 첫 번째 자리에 올 수 있는지 여부
     */
    public boolean canBeFirst(ReportColumn column) {
        return firstColumnCandidates.contains(column);
    }

    /**
     * 칸을 숨길 수 있는지 여부
     *
     * @param first 그 칸이 첫 번째 칸인지 여부. 첫 번째 칸은 숨길 수 없다.
     */
    public boolean canHide(ReportColumn column, boolean first) {
        return !first && hideableColumns.contains(column);
    }

    /**
     * 이 표에서 칸을 부르는 이름
     */
    public String labelOf(ReportColumn column) {
        boolean income = budgetType == BudgetType.INCOME;
        return switch (column) {
        case BUDGET_NAME -> "예산 항목";
        case ACTUAL_AMOUNT -> income ? "수입액" : "집행액";
        case BUDGETED_AMOUNT -> "예산액";
        case RATE -> income ? "달성률" : "집행률";
        case CONTACT -> income ? "봉헌자" : "수취인";
        case DESCRIPTION -> "내용";
        case AMOUNT -> "금액";
        case REMARKS -> "비고";
        };
    }
}
