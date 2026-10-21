package org.church.steward.domain.financialrecord.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FinancialRecordType {
    INCOME("수입"),
    EXPENSE("지출");

    private final String label;
}
