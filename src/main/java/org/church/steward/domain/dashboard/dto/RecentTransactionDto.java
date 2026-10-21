package org.church.steward.domain.dashboard.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentTransactionDto {
    private LocalDate date;
    private String type;
    private String category;
    private List<String> contactNames; // 수입의 봉헌자 또는 지출의 수취인. 내역에 기록된 당시 이름
    private long amount;
    private String description;
    private String remarks;
}
