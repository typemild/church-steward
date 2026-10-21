package org.church.steward.domain.financialrecord.mapper;

import java.util.List;
import java.util.stream.Collectors;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.springframework.stereotype.Component;

@Component
public class FinancialRecordMapper {

    public FinancialRecordResponseDto toResponseDto(FinancialRecord entity) {
        if (entity == null) {
            return null;
        }

        return FinancialRecordResponseDto.builder()
                .id(entity.getId())
                .type(entity.getType())
                .fiscalYearId(entity.getFiscalYear() != null ? entity.getFiscalYear().getId() : null)
                .date(entity.getDate())
                .budgetId(entity.getBudget() != null ? entity.getBudget().getId() : null)
                .budgetName(entity.getBudget() != null ? entity.getBudget().getHierarchyName() : null)
                .description(entity.getDescription())
                .amount(entity.getAmount())
                .remarks(entity.getRemarks())
                .contactIds(entity.getContacts() != null ? entity.getContacts().stream()
                        .map(recordContact -> recordContact.getContact().getId())
                        .collect(Collectors.toList()) : List.of())
                .contactNames(entity.getContacts() != null ? entity.getContacts().stream()
                        .map(FinancialRecordContact::getContactName)
                        .collect(Collectors.toList()) : List.of())
                .build();
    }
}
