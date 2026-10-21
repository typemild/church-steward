package org.church.steward.ui.view.financialrecord.budget;

import java.time.format.DateTimeFormatter;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;

import lombok.Getter;

/**
 * "예산별 보기" 탭 테이블의 한 행.
 * 예산 행(하위 내역들의 합계를 가짐)이거나 그 예산에 속한 수입/지출 내역 행이다.
 */
@Getter
public class BudgetRecordRow {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("M/d");

    private final Long budgetId;
    private final String budgetName;
    private final long amount;
    /** 예산 행이면 null, 내역 행이면 해당 수입/지출 내역 */
    private final FinancialRecordResponseDto record;

    private BudgetRecordRow(Long budgetId, String budgetName, long amount, FinancialRecordResponseDto record) {
        this.budgetId = budgetId;
        this.budgetName = budgetName;
        this.amount = amount;
        this.record = record;
    }

    /**
     * 예산 행을 만든다.
     *
     * @param budgetName 전체 예산명("예산A > 예산B > 예산C")
     * @param amount     하위 수입/지출 내역들의 합계
     */
    public static BudgetRecordRow ofBudget(Long budgetId, String budgetName, long amount) {
        return new BudgetRecordRow(budgetId, budgetName, amount, null);
    }

    /**
     * 수입/지출 내역 행을 만든다.
     */
    public static BudgetRecordRow ofRecord(FinancialRecordResponseDto record) {
        return new BudgetRecordRow(record.getBudgetId(), record.getBudgetName(), record.getAmount(), record);
    }

    public boolean isBudgetRow() {
        return record == null;
    }

    /**
     * 첫 번째 컬럼에 표시할 텍스트를 반환한다.
     * 예산 행은 전체 예산명, 내역 행은 "[4/7] 홍길동"과 같이 날짜와 관계자명이다.
     */
    public String getDisplayName() {
        if (isBudgetRow()) {
            return budgetName;
        }
        String date = "[" + record.getDate()
                .format(DATE_FORMAT) + "]";
        String names = FinancialRecordFormats.joinNames(record.getContactNames());
        return names.isEmpty() ? date : date + " " + names;
    }
}
