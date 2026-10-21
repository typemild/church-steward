package org.church.steward.domain.report.dto;

import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회계 연도 수입 결산 세부 내역서 또는 회계 연도 지출 결산 세부 내역서의 데이터
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementDetailReportDto {
    private String fiscalYearName;
    private BudgetType type;

    /** 예산 관리 화면의 트리와 같은 순서로 나열한 예산 */
    private List<BudgetExecutionDto> rows;
    private long totalBudgetedAmount;
    private long totalActualAmount;
    /** 예산 총액 대비 집행 총액의 비율(%). 예산 총액이 0이면 null */
    private Double totalExecutionRate;
}
