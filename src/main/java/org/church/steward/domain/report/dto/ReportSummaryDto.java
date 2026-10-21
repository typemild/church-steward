package org.church.steward.domain.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입 총액과 지출 총액
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {
    private long income;
    private long expense;

    /**
     * 차액(수입 - 지출)
     */
    public long getDifference() {
        return income - expense;
    }
}
