package org.church.steward.domain.fiscalyear.mapper;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.springframework.stereotype.Component;

@Component
public class FiscalYearMapper {

    public FiscalYearDto toDto(FiscalYear fiscalYear) {
        if (fiscalYear == null) {
            return null;
        }

        return FiscalYearDto.builder()
                .id(fiscalYear.getId())
                .name(fiscalYear.getName())
                .startDate(fiscalYear.getStartDate())
                .endDate(fiscalYear.getEndDate())
                .closed(fiscalYear.isClosed())
                .totalIncome(fiscalYear.getTotalIncome())
                .totalExpense(fiscalYear.getTotalExpense())
                .balance(fiscalYear.getBalance())
                .build();
    }
}
