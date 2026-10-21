package org.church.steward.domain.report.mapper;

import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.springframework.stereotype.Component;

/**
 * 예산과 수입/지출 내역 엔터티를 보고서 DTO로 바꾼다.
 * 금액 합산이나 비율 계산 같은 집계는 서비스가 하고, 여기서는 엔터티의 값과 서비스가 계산해 넘긴 값을 DTO에 옮겨 담기만 한다.
 */
@Component
public class ReportMapper {

    /**
     * 수입/지출 내역을 보고서에 싣는 내역 한 건으로 바꾼다. 관계자는 내역에 기록된 당시의 이름을 담는다.
     */
    public ReportRecordDto toRecordDto(FinancialRecord entity) {
        if (entity == null) {
            return null;
        }

        return ReportRecordDto.builder()
                .date(entity.getDate())
                .contactNames(entity.getContacts() != null
                        ? entity.getContacts()
                                .stream()
                                .map(FinancialRecordContact::getContactName)
                                .toList()
                        : List.of())
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .remarks(entity.getRemarks())
                .build();
    }

    /**
     * 한 예산에 속한 내역들을 묶음으로 바꾼다. 내역은 넘겨받은 순서를 그대로 유지한다.
     *
     * @param budgetName  전체 예산명("예산A > 예산B > 예산C")
     * @param totalAmount 내역들의 금액 합계
     */
    public ReportRecordGroupDto toRecordGroupDto(String budgetName, long totalAmount, List<FinancialRecord> entities) {
        return ReportRecordGroupDto.builder()
                .budgetName(budgetName)
                .totalAmount(totalAmount)
                .records(entities.stream()
                        .map(this::toRecordDto)
                        .toList())
                .build();
    }

    /**
     * 예산을 예산액과 집행액을 담은 한 줄로 바꾼다.
     *
     * @param depth          예산 계층에서의 깊이. 최상위 예산이 0이다.
     * @param budgetedAmount 예산액. 하위 예산이 있으면 그 합계
     * @param actualAmount   집행액. 하위 예산에 등록된 내역까지 합한 금액
     * @param shareRate      같은 구분의 예산 총액에서 차지하는 비율(%). 정할 수 없으면 null
     * @param executionRate  예산액 대비 집행액의 비율(%). 정할 수 없으면 null
     */
    public BudgetExecutionDto toBudgetExecutionDto(Budget entity, int depth, long budgetedAmount, long actualAmount,
            Double shareRate, Double executionRate) {
        if (entity == null) {
            return null;
        }

        return BudgetExecutionDto.builder()
                .name(entity.getName())
                .depth(depth)
                .budgetedAmount(budgetedAmount)
                .actualAmount(actualAmount)
                .shareRate(shareRate)
                .executionRate(executionRate)
                .build();
    }

    /**
     * 예산을 회계 연도 예산안의 한 줄로 바꾼다.
     *
     * @param depth          예산 계층에서의 깊이. 최상위 예산이 0이다.
     * @param amount         예산액. 하위 예산이 있으면 그 합계
     * @param shareRate      같은 구분의 예산 총액에서 차지하는 비율(%). 정할 수 없으면 null
     * @param previousAmount 직전 회계 연도에서 전체 경로가 같은 예산의 금액. 그런 예산이 없으면 null
     */
    public BudgetPlanRowDto toBudgetPlanRowDto(Budget entity, int depth, long amount, Double shareRate,
            Long previousAmount) {
        if (entity == null) {
            return null;
        }

        return BudgetPlanRowDto.builder()
                .name(entity.getName())
                .depth(depth)
                .amount(amount)
                .shareRate(shareRate)
                .previousAmount(previousAmount)
                .build();
    }
}
