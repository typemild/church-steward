package org.church.steward.domain.budget.service.impl;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.mapper.BudgetMapper;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.BudgetAmountProjection;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class BudgetServiceImpl implements BudgetService {
    /** 예산명의 최대 길이. {@code Budget.name} 칼럼의 길이와 같다. */
    private static final int NAME_MAX_LENGTH = 50;

    private final BudgetRepository budgetRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final BudgetMapper budgetMapper;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer snapshotCapturer;

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsByFiscalYearAndType(Long fiscalYearId, BudgetType type) {
        return budgetRepository.findAllByFiscalYearIdAndType(fiscalYearId, type).stream()
                .map(budgetMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetDto> getBudgetsWithActualAmount(Long fiscalYearId, BudgetType type) {
        FinancialRecordType recordType = type == BudgetType.INCOME ? FinancialRecordType.INCOME
                : FinancialRecordType.EXPENSE;
        Map<Long, Long> amountByBudgetId = financialRecordRepository.sumAmountGroupByBudget(fiscalYearId, recordType)
                .stream()
                .collect(Collectors.toMap(BudgetAmountProjection::getBudgetId, BudgetAmountProjection::getAmount));

        return budgetRepository.findAllByFiscalYearIdAndType(fiscalYearId, type)
                .stream()
                .map(budget -> budgetMapper.toDto(budget, sumActualAmount(budget, amountByBudgetId)))
                .toList();
    }

    /**
     * 예산과 그 하위 예산에 등록된 내역의 금액을 모두 더한다.
     *
     * @param amountByBudgetId 예산에 직접 등록된 내역의 금액 합계
     */
    private long sumActualAmount(Budget budget, Map<Long, Long> amountByBudgetId) {
        long total = amountByBudgetId.getOrDefault(budget.getId(), 0L);
        for (Budget child : budget.getChildren()) {
            total += sumActualAmount(child, amountByBudgetId);
        }
        return total;
    }

    @Override
    public void createBudget(Long fiscalYearId, Long parentId, String name, long amount, BudgetType type) {
        Budget newBudget = createBudgetWithoutLog(fiscalYearId, parentId, name, amount, type);
        activityLogService.logCreation(newBudget);
    }

    @Override
    public Budget createBudgetWithoutLog(Long fiscalYearId, Long parentId, String name, long amount,
            BudgetType type) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
        validateNotClosed(fiscalYear);
        validateName(name);

        Budget parent = null;
        if (parentId != null) {
            parent = budgetRepository.findById(parentId)
                    .orElseThrow(() -> new IllegalArgumentException("지정한 상위 예산 항목이 존재하지 않습니다."));

            // 제약 조건: 상위 예산은 같은 회계 연도의 예산이어야 함
            if (!parent.getFiscalYear().getId().equals(fiscalYearId)) {
                throw new IllegalArgumentException("상위 예산과 같은 회계 연도의 예산만 추가할 수 있습니다.");
            }

            // 제약 조건: 수입/지출 내역이 있으면 자식 추가 불가
            if (financialRecordRepository.existsByBudgetId(parent.getId())) {
                throw new IllegalArgumentException("수입/지출 내역이 있는 항목에는 하위 예산을 추가할 수 없습니다.");
            }

            // 제약 조건: 부모와 자식의 유형(INCOME/EXPENSE)이 일치해야 함
            if (!parent.getType().equals(type)) {
                throw new IllegalArgumentException("상위 예산과 동일한 유형(수입 또는 지출)의 예산만 추가할 수 있습니다.");
            }

            // 부모 항목은 amount가 null이 됨
            parent.setAmount(null);
            budgetRepository.save(parent);
        }

        Integer maxSortOrder = budgetRepository.findMaxSortOrder(fiscalYearId, parentId);
        int nextSortOrder = (maxSortOrder == null) ? 0 : maxSortOrder + 1;

        return budgetRepository.save(Budget.builder()
                .fiscalYear(fiscalYear)
                .name(name)
                .parent(parent)
                .amount(amount)
                .type(type)
                .sortOrder(nextSortOrder)
                .build());
    }

    @Override
    public void updateBudget(Long id, String newName, long newAmount) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
        validateNotClosed(budget.getFiscalYear());
        validateName(newName);

        // 변경 전 상태를 스냅샷으로 캡처
        EntitySnapshot<Budget> oldSnapshot = snapshotCapturer.capture(budget);

        applyUpdate(budget, newName, newAmount);
        activityLogService.logUpdate(oldSnapshot, budget);
    }

    @Override
    public Budget updateBudgetWithoutLog(Long id, String newName, long newAmount) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
        validateNotClosed(budget.getFiscalYear());
        validateName(newName);

        applyUpdate(budget, newName, newAmount);
        return budget;
    }

    private void applyUpdate(Budget budget, String newName, long newAmount) {
        budget.setName(newName);

        if (budget.hasChildren()) {
            // 부모 항목은 하위 항목들의 합으로 금액이 결정되므로, 금액을 직접 수정할 수 없다.
            if (newAmount != budget.getTotalAmount()) {
                throw new IllegalArgumentException("하위 항목이 있는 예산은 금액을 직접 수정할 수 없습니다.");
            }
        } else {
            budget.setAmount(newAmount);
        }

        budgetRepository.save(budget);
    }

    @Override
    public void deleteBudget(Long id) {
        // 1. 존재 여부 확인 및 예외 처리
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
        validateNotClosed(budget.getFiscalYear());

        // 2. 제약 조건 체크 (하위 예산 또는 수입/지출 내역이 있으면 삭제 불가)
        if (budgetRepository.existsByParentId(id)) {
            throw new IllegalArgumentException("하위 예산 항목이 존재하여 삭제할 수 없습니다.");
        }

        if (financialRecordRepository.existsByBudgetId(id)) {
            throw new IllegalArgumentException("수입/지출 내역이 존재하여 삭제할 수 없습니다.");
        }

        Budget parent = budget.getParent();
        budgetRepository.deleteById(id);
        activityLogService.logDeletion(budget);

        if (parent != null && !budgetRepository.existsByParentId(parent.getId())) {
            parent.setAmount(0L);
            budgetRepository.save(parent);
        }
    }

    @Override
    public void moveBudgetUp(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
        validateNotClosed(budget.getFiscalYear());

        List<Budget> siblings = getSortedSiblings(budget);
        int currentIndex = findIndexInSiblings(siblings, id);

        if (currentIndex > 0) {
            Budget previous = siblings.get(currentIndex - 1);
            int currentSortOrder = budget.getSortOrder();

            budget.setSortOrder(previous.getSortOrder());
            previous.setSortOrder(currentSortOrder);
            budgetRepository.save(budget);
            budgetRepository.save(previous);
        }
    }

    @Override
    public void moveBudgetDown(Long id) {
        Budget budget = budgetRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예산 항목을 찾을 수 없습니다."));
        validateNotClosed(budget.getFiscalYear());

        List<Budget> siblings = getSortedSiblings(budget);
        int currentIndex = findIndexInSiblings(siblings, id);

        if (currentIndex != -1 && currentIndex < siblings.size() - 1) {
            Budget next = siblings.get(currentIndex + 1);
            int currentSortOrder = budget.getSortOrder();

            budget.setSortOrder(next.getSortOrder());
            next.setSortOrder(currentSortOrder);
            budgetRepository.save(budget);
            budgetRepository.save(next);
        }
    }

    @Override
    public List<BudgetAutocompleteDto> getBudgetsForAutocomplete(Long fiscalYearId, BudgetType type, String name,
            int limit) {
        List<Budget> matchingNodes = budgetRepository.findAllByFiscalYearIdAndTypeAndNameContaining(fiscalYearId, type,
                name);
        Set<Budget> leafNodes = new HashSet<>();

        for (Budget node : matchingNodes) {
            collectLeafDescendants(node, leafNodes);
        }

        return leafNodes.stream()
                .sorted(Comparator.comparing(Budget::getHierarchyName))
                .limit(limit)
                .map(budgetMapper::toAutocompleteDto)
                .toList();
    }

    private void collectLeafDescendants(Budget node, Set<Budget> leafNodes) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            leafNodes.add(node);
        } else {
            for (Budget child : node.getChildren()) {
                collectLeafDescendants(child, leafNodes);
            }
        }
    }

    private List<Budget> getSortedSiblings(Budget budget) {
        Long parentId = (budget.getParent() != null) ? budget.getParent().getId() : null;
        return budgetRepository.findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(budget.getFiscalYear()
                .getId(), parentId);
    }

    /**
     * 마감된 회계 연도의 예산은 추가, 수정, 삭제하거나 순서를 바꿀 수 없다.
     */
    private void validateNotClosed(FiscalYear fiscalYear) {
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }
    }

    /**
     * 예산명은 비워 둘 수 없고 {@value #NAME_MAX_LENGTH}자 이내여야 한다.
     * {@code <}와 {@code >}는 전체 예산명("예산A > 예산B")의 구분 문자와 섞이지 않도록 쓸 수 없다.
     */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("예산명을 입력해주세요.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("예산명은 " + NAME_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
        if (name.contains("<") || name.contains(">")) {
            throw new IllegalArgumentException("예산명에는 '<', '>' 문자를 사용할 수 없습니다.");
        }
    }

    private int findIndexInSiblings(List<Budget> siblings, Long id) {
        for (int i = 0; i < siblings.size(); i++) {
            if (siblings.get(i).getId().equals(id)) {
                return i;
            }
        }
        return -1;
    }

}
