package org.church.steward.domain.fiscalyear.service.impl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.entity.FiscalYearConstraints;
import org.church.steward.domain.fiscalyear.mapper.FiscalYearMapper;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FiscalYearServiceImpl implements FiscalYearService {
    private final FiscalYearRepository fiscalYearRepository;
    private final BudgetRepository budgetRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final FiscalYearMapper fiscalYearMapper;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer entitySnapshotCapturer;

    @Override
    @Transactional(readOnly = true)
    public List<FiscalYearDto> listFiscalYears() {
        return fiscalYearRepository.findAllByOrderByStartDateDesc()
                .stream()
                .map(fiscalYearMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public void createFiscalYear(String name, LocalDate startDate, LocalDate endDate) {
        String trimmedName = validateName(name, null);
        validatePeriod(null, startDate, endDate);

        saveNewFiscalYear(trimmedName, startDate, endDate);
    }

    @Override
    public void createFiscalYearWithCopy(String name, LocalDate startDate, LocalDate endDate,
            Long sourceFiscalYearId) {
        if (sourceFiscalYearId == null || !fiscalYearRepository.existsById(sourceFiscalYearId)) {
            throw new IllegalArgumentException("존재하지 않는 원본 회계 연도입니다.");
        }
        String trimmedName = validateName(name, null);
        validatePeriod(null, startDate, endDate);

        // 1. 새 회계 연도 생성
        FiscalYear newFiscalYear = saveNewFiscalYear(trimmedName, startDate, endDate);

        // 2. 기존 예산 복사 (트리 구조 유지). 원본 회계 연도는 바꾸지 않으므로 마감되어 있어도 복사할 수 있다.
        List<Budget> allSourceBudgets = budgetRepository.findAllByFiscalYearId(sourceFiscalYearId);
        Map<Long, List<Budget>> childrenByParent = allSourceBudgets.stream()
                .filter(b -> b.getParent() != null)
                .collect(Collectors.groupingBy(b -> b.getParent().getId()));

        List<Budget> roots = allSourceBudgets.stream()
                .filter(b -> b.getParent() == null)
                .toList();

        for (Budget root : roots) {
            copyRecursive(root, newFiscalYear, null, childrenByParent);
        }
    }

    private void copyRecursive(Budget source, FiscalYear newFiscalYear, Budget newParent,
            Map<Long, List<Budget>> childrenByParent) {
        Budget newBudget = Budget.builder()
                .fiscalYear(newFiscalYear)
                .name(source.getName())
                .amount(source.getAmount())
                .type(source.getType())
                .sortOrder(source.getSortOrder())
                .parent(newParent)
                .build();
        budgetRepository.save(newBudget);

        List<Budget> children = childrenByParent.getOrDefault(source.getId(), List.of());
        for (Budget child : children) {
            copyRecursive(child, newFiscalYear, newBudget, childrenByParent);
        }
    }

    @Override
    public void updateFiscalYear(Long id, String name, LocalDate startDate, LocalDate endDate) {
        FiscalYear fiscalYear = findFiscalYear(id);
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도는 수정할 수 없습니다.");
        }
        String trimmedName = validateName(name, id);
        validatePeriod(id, startDate, endDate);
        validatePeriodCoversRecords(id, startDate, endDate);

        EntitySnapshot<FiscalYear> oldSnapshot = entitySnapshotCapturer.capture(fiscalYear);

        fiscalYear.update(trimmedName, startDate, endDate);

        FiscalYear saved = fiscalYearRepository.save(fiscalYear);
        activityLogService.logUpdate(oldSnapshot, saved);
    }

    private FiscalYear saveNewFiscalYear(String name, LocalDate startDate, LocalDate endDate) {
        FiscalYear fiscalYear = fiscalYearRepository.save(FiscalYear.builder()
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .build());
        activityLogService.logCreation(fiscalYear);
        return fiscalYear;
    }

    private FiscalYear findFiscalYear(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("존재하지 않는 회계 연도입니다.");
        }
        return fiscalYearRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
    }

    /**
     * 이름이 비어 있지 않고 너무 길지 않은지, 다른 회계 연도의 이름과 같지 않은지 확인한다.
     *
     * @param id 이름을 지정하려는 회계 연도의 ID. 중복을 확인할 때 자기 자신은 제외한다. 새 회계 연도이면 null이다.
     * @return 앞뒤 공백을 없앤 이름
     */
    private String validateName(String name, Long id) {
        String trimmedName = name == null ? "" : name.trim();
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("회계 연도의 이름을 입력해주세요.");
        }
        if (trimmedName.length() > FiscalYearConstraints.NAME_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    String.format("회계 연도의 이름은 %d자를 넘을 수 없습니다.", FiscalYearConstraints.NAME_MAX_LENGTH));
        }

        boolean duplicated = id == null ? fiscalYearRepository.existsByName(trimmedName)
                : fiscalYearRepository.existsByNameAndIdNot(trimmedName, id);
        if (duplicated) {
            throw new IllegalArgumentException("같은 이름의 회계 연도가 이미 있습니다.");
        }
        return trimmedName;
    }

    /**
     * 기간이 올바른지, 다른 회계 연도의 기간과 겹치지 않는지 확인한다. 기간의 길이는 제한하지 않는다.
     *
     * @param id 기간을 지정하려는 회계 연도의 ID. 겹침을 확인할 때 자기 자신은 제외한다. 새 회계 연도이면 null이다.
     */
    private void validatePeriod(Long id, LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("회계 연도의 시작일과 종료일을 입력해주세요.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("회계 연도의 시작일이 종료일보다 늦습니다.");
        }

        fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate)
                .stream()
                .filter(overlapped -> !overlapped.getId()
                        .equals(id))
                .findFirst()
                .ifPresent(overlapped -> {
                    throw new IllegalArgumentException(String.format("\"%s\" 회계 연도의 기간(%s ~ %s)과 겹칩니다.",
                            overlapped.getName(), overlapped.getStartDate(), overlapped.getEndDate()));
                });
    }

    /**
     * 이미 등록된 수입/지출이 기간 밖에 놓이지 않도록, 시작일은 가장 이른 내역의 날짜 이전이고 종료일은 가장 늦은 내역의 날짜 이후인지
     * 확인한다.
     */
    private void validatePeriodCoversRecords(Long id, LocalDate startDate, LocalDate endDate) {
        financialRecordRepository.findFirstByFiscalYearIdOrderByDateAsc(id)
                .map(FinancialRecord::getDate)
                .filter(startDate::isAfter)
                .ifPresent(earliestDate -> {
                    throw new IllegalArgumentException(
                            "시작일은 등록된 수입/지출의 가장 이른 날짜(" + earliestDate + ")보다 늦을 수 없습니다.");
                });
        financialRecordRepository.findFirstByFiscalYearIdOrderByDateDesc(id)
                .map(FinancialRecord::getDate)
                .filter(endDate::isBefore)
                .ifPresent(latestDate -> {
                    throw new IllegalArgumentException(
                            "종료일은 등록된 수입/지출의 가장 늦은 날짜(" + latestDate + ")보다 이를 수 없습니다.");
                });
    }

    @Override
    public void deleteFiscalYear(Long id) {
        // 1. 해당 회계 연도가 존재하는지 먼저 확인
        FiscalYear fiscalYear = findFiscalYear(id);

        // 2. 마감된 회계 연도는 삭제할 수 없음
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감된 회계 연도는 삭제할 수 없습니다.");
        }

        // 3. 최소 1개의 회계 연도는 유지되어야 함
        if (fiscalYearRepository.count() <= 1) {
            throw new IllegalArgumentException("최소 하나의 회계 연도는 유지되어야 합니다.");
        }

        // 4. 해당 회계 연도에 설정된 예산 계획이 있는지 확인
        if (!budgetRepository.findAllByFiscalYearId(id).isEmpty()) {
            throw new IllegalArgumentException("해당 회계 연도에 설정된 예산 계획이 존재하여 삭제할 수 없습니다.");
        }

        // 5. 해당 회계 연도의 수입/지출 내역이 있는지 확인
        if (financialRecordRepository.existsByFiscalYearId(id)) {
            throw new IllegalArgumentException("해당 회계 연도에 등록된 수입/지출 내역이 존재하여 삭제할 수 없습니다.");
        }

        fiscalYearRepository.delete(fiscalYear);
        activityLogService.logDeletion(fiscalYear);
    }

    @Override
    public void closeFiscalYear(Long id) {
        FiscalYear fiscalYear = findFiscalYear(id);
        if (fiscalYear.isClosed()) {
            throw new IllegalArgumentException("이미 마감된 회계 연도입니다.");
        }

        EntitySnapshot<FiscalYear> oldSnapshot = entitySnapshotCapturer.capture(fiscalYear);

        fiscalYear.close(financialRecordRepository.sumAmountByFiscalYearIdAndType(id, FinancialRecordType.INCOME),
                financialRecordRepository.sumAmountByFiscalYearIdAndType(id, FinancialRecordType.EXPENSE));

        FiscalYear saved = fiscalYearRepository.save(fiscalYear);
        activityLogService.logClosing(oldSnapshot, saved);
    }

    @Override
    public void reopenFiscalYear(Long id) {
        FiscalYear fiscalYear = findFiscalYear(id);
        if (!fiscalYear.isClosed()) {
            throw new IllegalArgumentException("마감되지 않은 회계 연도입니다.");
        }

        EntitySnapshot<FiscalYear> oldSnapshot = entitySnapshotCapturer.capture(fiscalYear);

        fiscalYear.reopen();

        FiscalYear saved = fiscalYearRepository.save(fiscalYear);
        activityLogService.logReopening(oldSnapshot, saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getLatestFiscalYearId() {
        return fiscalYearRepository.findFirstByOrderByStartDateDesc()
                .map(FiscalYear::getId)
                .orElseThrow(() -> new IllegalStateException("등록된 회계 연도가 없습니다."));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return id != null && fiscalYearRepository.existsById(id);
    }

}
