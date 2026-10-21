package org.church.steward.domain.financialrecord.repository;

/**
 * 예산 하나에 직접 등록된 수입 또는 지출의 금액 합계
 */
public interface BudgetAmountProjection {

    Long getBudgetId();

    Long getAmount();
}
