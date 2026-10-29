package org.church.steward.domain.financialrecord.controller;

import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.service.FinancialRecordService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class FinancialRecordController {

    private final FinancialRecordService financialRecordService;

    public List<FinancialRecordResponseDto> listFinancialRecords(FinancialRecordSearchRequestDto request) {
        return financialRecordService.listFinancialRecords(request);
    }

    public FinancialRecordResponseDto createFinancialRecord(FinancialRecordRequestDto dto) {
        return financialRecordService.createFinancialRecord(dto);
    }

    public FinancialRecordResponseDto updateFinancialRecord(Long id, FinancialRecordRequestDto dto) {
        return financialRecordService.updateFinancialRecord(id, dto);
    }

    public void updateFinancialRecordsBudget(List<Long> ids, Long budgetId) {
        financialRecordService.updateFinancialRecordsBudget(ids, budgetId);
    }

    public void updateFinancialRecordsContacts(List<Long> ids, List<Long> contactIds) {
        financialRecordService.updateFinancialRecordsContacts(ids, contactIds);
    }

    public void deleteFinancialRecords(List<Long> ids) {
        financialRecordService.deleteFinancialRecords(ids);
    }
}
