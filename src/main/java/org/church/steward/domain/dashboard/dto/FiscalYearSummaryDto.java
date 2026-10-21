package org.church.steward.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FiscalYearSummaryDto {
    private long totalIncome;
    private long totalExpense;
    private long currentBalance; // (수입 - 지출)
}
