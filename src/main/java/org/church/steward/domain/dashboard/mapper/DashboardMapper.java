package org.church.steward.domain.dashboard.mapper;

import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.springframework.stereotype.Component;

/**
 * 예산과 수입/지출 내역 엔터티를 대시보드 DTO로 바꾼다.
 * 금액 합산이나 비율 계산 같은 집계는 서비스가 하고, 여기서는 엔터티의 값과 서비스가 계산해 넘긴 값을 DTO에 옮겨 담기만 한다.
 */
@Component
public class DashboardMapper {

    /**
     * 예산을 진행 현황 한 건으로 바꾼다.
     *
     * @param budgetedAmount 예산액. 하위 예산이 있으면 그 합계
     * @param actualAmount   하위 예산에 등록된 내역까지 합한 금액
     * @param progressRate   예산액 대비 실제 금액의 비율(%)
     */
    public BudgetProgressDto toBudgetProgressDto(Budget entity, long budgetedAmount, long actualAmount,
            double progressRate) {
        if (entity == null) {
            return null;
        }

        return BudgetProgressDto.builder()
                .budgetName(entity.getName())
                .type(entity.getType())
                .budgetedAmount(budgetedAmount)
                .actualAmount(actualAmount)
                .progressRate(progressRate)
                .build();
    }

    /**
     * 수입/지출 내역을 최근 내역 한 건으로 바꾼다. 관계자는 내역에 기록된 당시의 이름을 담는다.
     */
    public RecentTransactionDto toRecentTransactionDto(FinancialRecord entity) {
        if (entity == null) {
            return null;
        }

        return RecentTransactionDto.builder()
                .date(entity.getDate())
                .type(entity.getType().getLabel())
                .category(entity.getBudget().getName())
                .contactNames(entity.getContacts() != null
                        ? entity.getContacts()
                                .stream()
                                .map(FinancialRecordContact::getContactName)
                                .toList()
                        : List.of())
                .amount(entity.getAmount())
                .description(entity.getDescription())
                .remarks(entity.getRemarks())
                .build();
    }
}
