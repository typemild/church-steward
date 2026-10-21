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
public class FinancialRecordRequestDto {
    /** 구분. 생성할 때만 쓰이며, 수정할 때는 기존 내역의 구분을 유지한다. */
    private FinancialRecordType type;
    private Long fiscalYearId;
    private LocalDate date;
    private Long budgetId;
    private String description;
    private long amount;
    private String remarks;
    /** 관계자 ID 목록. 수입은 봉헌자 1명 이상, 지출은 수취인 1명이어야 한다. */
    private List<Long> contactIds;
}
