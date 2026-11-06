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
public enum ReportTemplateTable implements ColumnOrderRule {

    WEEKLY_INCOME_STATUS(ReportType.WEEKLY_SETTLEMENT, "수입 현황", BudgetType.INCOME, Kind.STATUS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    WEEKLY_EXPENSE_STATUS(ReportType.WEEKLY_SETTLEMENT, "지출 현황", BudgetType.EXPENSE, Kind.STATUS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    WEEKLY_INCOME_RECORDS(ReportType.WEEKLY_SETTLEMENT, "주간 수입 내역", BudgetType.INCOME, Kind.RECORDS,
            List.of(ReportColumn.CONTACT, ReportColumn.AMOUNT, ReportColumn.DESCRIPTION, ReportColumn.REMARKS),
            Set.of(ReportColumn.CONTACT, ReportColumn.DESCRIPTION),
            Set.of(ReportColumn.DESCRIPTION, ReportColumn.REMARKS)),
    WEEKLY_EXPENSE_RECORDS(ReportType.WEEKLY_SETTLEMENT, "주간 지출 내역", BudgetType.EXPENSE, Kind.RECORDS,
            List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.REMARKS),
            Set.of(ReportColumn.CONTACT, ReportColumn.DESCRIPTION),
            Set.of(ReportColumn.DESCRIPTION, ReportColumn.REMARKS)),
    INCOME_BUDGET_PLAN(ReportType.BUDGET_PLAN, "수입 예산안", BudgetType.INCOME, Kind.BUDGETS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                    ReportColumn.CHANGE, ReportColumn.REMARKS),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    EXPENSE_BUDGET_PLAN(ReportType.BUDGET_PLAN, "지출 예산안", BudgetType.EXPENSE, Kind.BUDGETS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                    ReportColumn.CHANGE, ReportColumn.REMARKS),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    ANNUAL_INCOME_STATUS(ReportType.ANNUAL_SETTLEMENT, "수입 현황", BudgetType.INCOME, Kind.STATUS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    ANNUAL_EXPENSE_STATUS(ReportType.ANNUAL_SETTLEMENT, "지출 현황", BudgetType.EXPENSE, Kind.STATUS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.ACTUAL_AMOUNT, ReportColumn.BUDGETED_AMOUNT,
                    ReportColumn.RATE),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    INCOME_SETTLEMENT_DETAIL(ReportType.SETTLEMENT_DETAIL, "수입 결산 세부 내역", BudgetType.INCOME,
            Kind.BUDGETS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                    ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE, ReportColumn.REMARKS),
            Set.of(ReportColumn.BUDGET_NAME), Set.of()),
    EXPENSE_SETTLEMENT_DETAIL(ReportType.SETTLEMENT_DETAIL, "지출 결산 세부 내역", BudgetType.EXPENSE,
            Kind.BUDGETS,
            List.of(ReportColumn.BUDGET_NAME, ReportColumn.BUDGETED_AMOUNT, ReportColumn.SHARE_RATE,
                    ReportColumn.ACTUAL_AMOUNT, ReportColumn.RATE, ReportColumn.REMARKS),
            Set.of(ReportColumn.BUDGET_NAME), Set.of());

    /**
     * 표의 종류. 칸이 폭을 나누는 방식이 종류마다 다르다.
     */
    public enum Kind {
        /** 현황 표. 모든 칸이 폭을 고르게 나눠 갖고, 비율 칸의 막대를 숨길 수 있다. */
        STATUS,
        /** 주간 내역. 금액까지는 내용에 맞춘 폭이고 금액 뒤의 칸들이 남은 폭을 나눠 갖는다. */
        RECORDS,
        /** 예산을 한 줄씩 나열하는 표(예산안, 결산 세부 내역서). 모든 칸이 내용에 맞춘 폭에서 시작해 남은 폭을 고르게 나눠 갖는다. */
        BUDGETS
    }

    /** 이 표가 들어가는 보고서 */
    private final ReportType reportType;
    /** 표의 이름. 양식 설정의 탭 이름이며, 절 제목이 있는 표는 절 제목과 같다. */
    private final String label;
    /** 수입의 표인지 지출의 표인지. 칸의 이름("수입액"/"집행액", "봉헌자"/"수취인")을 정한다. */
    private final BudgetType budgetType;
    private final Kind kind;
    /** 모든 칸의 기본 순서. 기본으로는 숨긴 칸이 없다. */
    private final List<ReportColumn> defaultColumns;
    /** 첫 번째 칸에 둘 수 있는 칸 */
    private final Set<ReportColumn> firstColumnCandidates;
    /** 숨길 수 있는 칸. 이 칸이라도 첫 번째 칸이면 숨길 수 없다. */
    private final Set<ReportColumn> hideableColumns;

    ReportTemplateTable(ReportType reportType, String label, BudgetType budgetType, Kind kind,
            List<ReportColumn> defaultColumns, Set<ReportColumn> firstColumnCandidates,
            Set<ReportColumn> hideableColumns) {
        this.reportType = reportType;
        this.label = label;
        this.budgetType = budgetType;
        this.kind = kind;
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

    @Override
    public boolean canBeFirst(ReportColumn column) {
        return firstColumnCandidates.contains(column);
    }

    @Override
    public boolean hasRateBarOption() {
        return kind == Kind.STATUS;
    }

    @Override
    public boolean hasRecordOrderOption() {
        return kind == Kind.RECORDS;
    }

    @Override
    public boolean hasDateOption() {
        return kind == Kind.RECORDS;
    }

    @Override
    public String labelOf(ReportColumn column) {
        boolean income = budgetType == BudgetType.INCOME;
        return switch (column) {
        case BUDGET_NAME -> "예산 항목";
        case ACTUAL_AMOUNT -> income ? "수입액" : "집행액";
        case BUDGETED_AMOUNT -> "예산액";
        case RATE -> income ? "달성률" : "집행률";
        case SHARE_RATE -> "점유율";
        // "직전 회계 연도 대비 증감액"은 칸 폭에 비해 길어 줄여 적는다.
        case CHANGE -> "직전 대비 증감액";
        case CONTACT -> income ? "봉헌자" : "수취인";
        case DESCRIPTION -> "내용";
        case AMOUNT -> "금액";
        case REMARKS -> "비고";
        };
    }
}
