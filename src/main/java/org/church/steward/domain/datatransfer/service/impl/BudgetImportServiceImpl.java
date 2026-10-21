package org.church.steward.domain.datatransfer.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.datatransfer.service.BudgetImportService;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetImportServiceImpl implements BudgetImportService {

    /** 활동 기록에 적는 대상명 */
    private static final String TARGET_NAME = "예산";

    private final FiscalYearRepository fiscalYearRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final BudgetService budgetService;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer snapshotCapturer;

    @Override
    @Transactional(readOnly = true)
    public BudgetImportAnalysisDto analyze(Long fiscalYearId, BudgetImportRequestDto request) {
        findOpenFiscalYear(fiscalYearId);
        return analyzeWithCurrentData(fiscalYearId, request);
    }

    @Override
    public ImportResultDto importBudgets(Long fiscalYearId, BudgetImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        FiscalYear fiscalYear = findOpenFiscalYear(fiscalYearId);
        if (selectedKeys.isEmpty()) {
            throw new IllegalArgumentException("가져올 예산을 선택해주세요.");
        }
        if (activityLogService.getLatestLogId() != backupLogId) {
            throw new BackupOutdatedException("백업한 뒤에 데이터가 바뀌었습니다. 백업을 다시 받아주세요.");
        }

        BudgetImportAnalysisDto analysis = analyzeWithCurrentData(fiscalYearId, request);
        if (!analysis.getSignature().equals(expectedSignature)) {
            throw new ImportStateChangedException("가져오기를 준비한 뒤에 데이터가 바뀌었습니다. 내용을 다시 확인해주세요.");
        }

        // 건의 식별자 -> 그 예산의 ID. 이미 있는 예산은 처음부터 알고, 새로 추가한 예산은 추가하면서 채운다.
        Map<String, Long> budgetIds = new HashMap<>();
        // 활동 기록에 담을 건별 내용. 상위 예산의 금액은 하위 예산을 추가하면서 바뀌므로 반영이 모두 끝난 뒤에 만든다.
        List<Supplier<List<ActivityLogFieldDto>>> logItems = new ArrayList<>();
        int createdCount = 0;
        int updatedCount = 0;

        // 분석 결과는 상위 예산이 하위 예산보다 먼저 오므로 순서대로 반영하면 된다.
        for (BudgetImportItemDto item : analysis.getItems()) {
            if (item.getExistingBudgetId() != null) {
                budgetIds.put(item.getKey(), item.getExistingBudgetId());
            }
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            if (!item.isSelectable()) {
                throw new IllegalArgumentException("가져올 수 없는 예산이 선택되었습니다: " + pathOf(item));
            }

            if (item.getAction() == BudgetImportAction.CREATE) {
                Long parentId = null;
                if (item.getParentKey() != null) {
                    parentId = budgetIds.get(item.getParentKey());
                    if (parentId == null) {
                        throw new IllegalArgumentException("상위 예산을 함께 선택해야 합니다: " + pathOf(item));
                    }
                }
                Budget created = budgetService.createBudgetWithoutLog(fiscalYearId, parentId,
                        item.getLevelNames().getLast(), item.isLeaf() ? item.getAmount() : 0L, item.getType());
                budgetIds.put(item.getKey(), created.getId());
                logItems.add(() -> activityLogService.describeCreation(created));
                createdCount++;
            } else {
                Budget existing = budgetRepository.findById(item.getExistingBudgetId())
                        .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
                EntitySnapshot<Budget> oldSnapshot = snapshotCapturer.capture(existing);
                Budget updated = budgetService.updateBudgetWithoutLog(existing.getId(), existing.getName(),
                        item.getAmount());
                List<ActivityLogFieldDto> fields = activityLogService.describeUpdate(oldSnapshot, updated);
                logItems.add(() -> fields);
                updatedCount++;
            }
        }

        activityLogService.logImport(TARGET_NAME, fiscalYear.getName(), fileName,
                String.format("추가 %,d건, 금액 갱신 %,d건", createdCount, updatedCount),
                logItems.stream()
                        .map(Supplier::get)
                        .toList());

        return ImportResultDto.builder()
                .createdCount(createdCount)
                .updatedCount(updatedCount)
                .build();
    }

    private BudgetImportAnalysisDto analyzeWithCurrentData(Long fiscalYearId, BudgetImportRequestDto request) {
        return BudgetImportAnalyzer.analyze(request, budgetRepository.findAllByFiscalYearId(fiscalYearId),
                financialRecordRepository::existsByBudgetId);
    }

    private FiscalYear findOpenFiscalYear(Long fiscalYearId) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도에는 예산을 가져올 수 없습니다.");
        }
        return fiscalYear;
    }

    private String pathOf(BudgetImportItemDto item) {
        return String.join(" > ", item.getLevelNames());
    }

}
