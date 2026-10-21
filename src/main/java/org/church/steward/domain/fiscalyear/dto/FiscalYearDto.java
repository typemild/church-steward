package org.church.steward.domain.fiscalyear.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FiscalYearDto {
    private Long id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean closed;

    /** 마감 시점의 총 수입, 총 지출, 차액. 마감하지 않았으면 null이다. */
    private Long totalIncome;
    private Long totalExpense;
    private Long balance;
}
