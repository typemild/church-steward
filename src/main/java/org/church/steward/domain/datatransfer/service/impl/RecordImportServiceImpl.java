package org.church.steward.domain.datatransfer.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.datatransfer.service.RecordImportService;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.financialrecord.service.FinancialRecordService;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecordImportServiceImpl implements RecordImportService {

    /** 활동 기록에 적는 대상명 */
    private static final String TARGET_NAME = "수입/지출";

    private final FiscalYearRepository fiscalYearRepository;
    private final BudgetRepository budgetRepository;
    private final ContactRepository contactRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final FinancialRecordService financialRecordService;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public RecordImportAnalysisDto analyze(Long fiscalYearId, RecordImportRequestDto request) {
        return analyzeWithCurrentData(findOpenFiscalYear(fiscalYearId), request);
    }

    @Override
    public ImportResultDto importRecords(Long fiscalYearId, RecordImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        FiscalYear fiscalYear = findOpenFiscalYear(fiscalYearId);
        if (selectedKeys.isEmpty()) {
            throw new IllegalArgumentException("가져올 내역을 선택해주세요.");
        }
        if (activityLogService.getLatestLogId() != backupLogId) {
            throw new BackupOutdatedException("백업한 뒤에 데이터가 바뀌었습니다. 백업을 다시 받아주세요.");
        }

        RecordImportAnalysisDto analysis = analyzeWithCurrentData(fiscalYear, request);
        if (!analysis.getSignature().equals(expectedSignature)) {
            throw new ImportStateChangedException("가져오기를 준비한 뒤에 데이터가 바뀌었습니다. 내용을 다시 확인해주세요.");
        }

        List<List<ActivityLogFieldDto>> logItems = new ArrayList<>();
        int incomeCount = 0;
        int expenseCount = 0;
        long incomeAmount = 0;
        long expenseAmount = 0;

        for (RecordImportItemDto item : analysis.getItems()) {
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            if (!item.isSelectable()) {
                throw new IllegalArgumentException("가져올 수 없는 행이 선택되었습니다: " + item.getRowNumber() + "행");
            }

            FinancialRecord created = financialRecordService.createFinancialRecordWithoutLog(
                    FinancialRecordRequestDto.builder()
                            .type(item.getType())
                            .fiscalYearId(fiscalYearId)
                            .date(item.getDate())
                            .budgetId(item.getBudgetId())
                            .description(item.getDescription())
                            .amount(item.getAmount())
                            .remarks(item.getRemarks())
                            .contactIds(item.getContactIds())
                            .build());
            logItems.add(activityLogService.describeCreation(created));

            if (item.getType() == FinancialRecordType.INCOME) {
                incomeCount++;
                incomeAmount += item.getAmount();
            } else {
                expenseCount++;
                expenseAmount += item.getAmount();
            }
        }

        activityLogService.logImport(TARGET_NAME, fiscalYear.getName(), fileName,
                String.format("수입 %,d건 %,d원, 지출 %,d건 %,d원", incomeCount, incomeAmount, expenseCount, expenseAmount),
                logItems);

        return ImportResultDto.builder()
                .createdCount(incomeCount + expenseCount)
                .updatedCount(0)
                .build();
    }

    private RecordImportAnalysisDto analyzeWithCurrentData(FiscalYear fiscalYear, RecordImportRequestDto request) {
        return RecordImportAnalyzer.analyze(request, fiscalYear.getStartDate(), fiscalYear.getEndDate(),
                budgetRepository.findAllByFiscalYearId(fiscalYear.getId()), contactRepository.findAll(),
                financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId()));
    }

    private FiscalYear findOpenFiscalYear(Long fiscalYearId) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도에는 수입/지출을 가져올 수 없습니다.");
        }
        return fiscalYear;
    }

}
