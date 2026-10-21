package org.church.steward.domain.financialrecord.dto;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecordSearchRequestDto {

    private FinancialRecordType type;
    private Long fiscalYearId;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long budgetId;
    private String description;
    private String remarks;
    private List<Long> contactIds;

    /** 금액 범위의 시작 금액(포함). null이면 하한을 적용하지 않는다. */
    private Long minAmount;
    /** 금액 범위의 종료 금액(포함). null이면 상한을 적용하지 않는다. */
    private Long maxAmount;

}
