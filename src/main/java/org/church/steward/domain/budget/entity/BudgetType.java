package org.church.steward.domain.budget.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum BudgetType {

    INCOME("수입"),
    EXPENSE("지출");

    private final String label;

}
