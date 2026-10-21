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
public class FinancialRecordResponseDto {
    private Long id;
    private FinancialRecordType type;
    private Long fiscalYearId;
    private LocalDate date;
    private Long budgetId;
    private String budgetName;
    private String description;
    private long amount;
    private String remarks;

    private List<Long> contactIds;
    private List<String> contactNames;
}
