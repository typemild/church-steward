package org.church.steward.domain.report.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보고서에 싣는 수입/지출 내역 한 건
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRecordDto {
    private LocalDate date;
    /** 내역에 기록된 당시의 봉헌자/수취인 이름 */
    private List<String> contactNames;
    private String description;
    private long amount;
    private String remarks;
}
