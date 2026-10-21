package org.church.steward.domain.dashboard.dto;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BudgetProgressDto {
    private String budgetName;
    private BudgetType type;
    private long budgetedAmount;
    private long actualAmount;
    private double progressRate; // (actual / budgeted) * 100
}
