package org.church.steward.domain.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 한 예산에 속한 수입/지출 내역 묶음
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRecordGroupDto {
    /** 전체 예산명("예산A > 예산B > 예산C") */
    private String budgetName;
    private long totalAmount;
    /** 날짜순(같은 날짜는 입력순)으로 정렬된 내역 */
    private List<ReportRecordDto> records;
}
