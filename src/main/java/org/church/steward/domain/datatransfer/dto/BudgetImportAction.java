package org.church.steward.domain.datatransfer.dto;

/**
 * 예산 가져오기에서 한 건을 반영할 때 하는 일
 */
public enum BudgetImportAction {

    /** 예산을 새로 추가함 */
    CREATE("추가"),
    /** 이미 있는 예산의 금액을 파일의 값으로 바꿈 */
    UPDATE_AMOUNT("금액 갱신"),
    /** 반영할 것이 없음 */
    NONE("");

    private final String label;

    BudgetImportAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
