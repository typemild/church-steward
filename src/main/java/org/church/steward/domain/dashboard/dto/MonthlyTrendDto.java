package org.church.steward.domain.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDto {
    /**
     * 연도와 달. "2023-10" 형식이다. 회계 연도의 기간은 달력의 해와 어긋날 수 있으므로(예: 2023년 2월 ~ 2024년 1월)
     * 달만이 아니라 연도까지 담는다.
     */
    private String yearMonth;
    private long income;
    private long expense;
}
