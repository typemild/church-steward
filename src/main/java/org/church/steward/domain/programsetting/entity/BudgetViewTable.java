package org.church.steward.domain.programsetting.entity;

import java.util.List;
import java.util.Set;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.report.entity.ColumnOrderRule;
import org.church.steward.domain.report.entity.ReportColumn;

import lombok.Getter;

/**
 * 수입/지출 관리의 "예산별 보기"에서 칸의 순서와 숨김을 정할 수 있는 표. 칸의 구성과 규칙, 기본 순서는 주간 회계 결산 보고서의 주간
 * 내역과 같지만, 설정은 보고서 양식과 따로 저장한다.
 */
@Getter
public enum BudgetViewTable implements ColumnOrderRule {

    INCOME(FinancialRecordType.INCOME, "수입 내역",
            List.of(ReportColumn.CONTACT, ReportColumn.AMOUNT, ReportColumn.DESCRIPTION, ReportColumn.REMARKS)),
    EXPENSE(FinancialRecordType.EXPENSE, "지출 내역",
            List.of(ReportColumn.DESCRIPTION, ReportColumn.AMOUNT, ReportColumn.CONTACT, ReportColumn.REMARKS));

    /** 첫 번째 칸에 둘 수 있는 칸. 일자는 이 칸의 왼쪽에 붙여 적는다. */
    private static final Set<ReportColumn> FIRST_COLUMN_CANDIDATES = Set.of(ReportColumn.CONTACT,
            ReportColumn.DESCRIPTION);
    private static final Set<ReportColumn> HIDEABLE_COLUMNS = Set.of(ReportColumn.DESCRIPTION, ReportColumn.REMARKS);

    /** 이 표에 보이는 내역의 구분 */
    private final FinancialRecordType recordType;
    /** 표의 이름 */
    private final String label;
    /** 모든 칸의 기본 순서. 기본으로는 숨긴 칸이 없다. */
    private final List<ReportColumn> defaultColumns;

    BudgetViewTable(FinancialRecordType recordType, String label, List<ReportColumn> defaultColumns) {
        this.recordType = recordType;
        this.label = label;
        this.defaultColumns = defaultColumns;
    }

    /**
     * 구분에 해당하는 표
     */
    public static BudgetViewTable of(FinancialRecordType recordType) {
        return recordType == FinancialRecordType.INCOME ? INCOME : EXPENSE;
    }

    @Override
    public Set<ReportColumn> getHideableColumns() {
        return HIDEABLE_COLUMNS;
    }

    @Override
    public boolean canBeFirst(ReportColumn column) {
        return FIRST_COLUMN_CANDIDATES.contains(column);
    }

    @Override
    public String labelOf(ReportColumn column) {
        return switch (column) {
        case CONTACT -> recordType == FinancialRecordType.INCOME ? "봉헌자" : "수취인";
        case DESCRIPTION -> "내용";
        case AMOUNT -> "금액";
        case REMARKS -> "비고";
        default -> throw new IllegalArgumentException("예산별 보기에 없는 칸입니다: " + column);
        };
    }
}
