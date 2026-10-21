package org.church.steward.domain.dashboard.service.impl;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.dashboard.dto.BreakdownDto;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.FiscalYearSummaryDto;
import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.dashboard.mapper.DashboardMapper;
import org.church.steward.domain.dashboard.service.DashboardService;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.financialrecord.specification.FinancialRecordSpecification;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final FinancialRecordRepository financialRecordRepository;
    private final BudgetRepository budgetRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final DashboardMapper dashboardMapper;

    @Override
    public FiscalYearSummaryDto getFiscalYearSummary(Long fiscalYearId) {
        List<FinancialRecord> records = findRecords(fiscalYearId, null);

        long totalIncome = sumAmount(records, FinancialRecordType.INCOME);
        long totalExpense = sumAmount(records, FinancialRecordType.EXPENSE);

        return FiscalYearSummaryDto.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .currentBalance(totalIncome - totalExpense)
                .build();
    }

    @Override
    public List<BudgetProgressDto> getBudgetProgress(Long fiscalYearId) {
        List<Budget> budgets = budgetRepository.findAllByFiscalYearId(fiscalYearId);
        List<FinancialRecord> records = findRecords(fiscalYearId, null);

        return budgets.stream()
                .filter(b -> b.getParent() == null)
                .sorted(Comparator.comparing(Budget::getType)
                        .thenComparingInt(Budget::getSortOrder))
                .map(budget -> {
                    long budgeted = budget.getTotalAmount();
                    long actual = calculateActualAmount(budget, records);
                    double progress = budgeted == 0 ? 0 : (double) actual / budgeted * 100;
                    return dashboardMapper.toBudgetProgressDto(budget, budgeted, actual, progress);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<MonthlyTrendDto> getMonthlyTrend(Long fiscalYearId) {
        FiscalYear fiscalYear = fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
        List<FinancialRecord> records = findRecords(fiscalYearId, null);

        // 회계 연도 기간의 시작월부터 종료월까지는 내역이 없어도 보여준다.
        Map<YearMonth, List<FinancialRecord>> recordsByMonth = new TreeMap<>();
        YearMonth endMonth = YearMonth.from(fiscalYear.getEndDate());
        for (YearMonth month = YearMonth.from(fiscalYear.getStartDate()); !month.isAfter(endMonth); month = month
                .plusMonths(1)) {
            recordsByMonth.put(month, new ArrayList<>());
        }
        for (FinancialRecord record : records) {
            recordsByMonth.computeIfAbsent(YearMonth.from(record.getDate()), _ -> new ArrayList<>())
                    .add(record);
        }

        List<MonthlyTrendDto> monthlyTrendList = new ArrayList<>();
        for (Map.Entry<YearMonth, List<FinancialRecord>> entry : recordsByMonth.entrySet()) {
            monthlyTrendList.add(MonthlyTrendDto.builder()
                    .yearMonth(entry.getKey().toString())
                    .income(sumAmount(entry.getValue(), FinancialRecordType.INCOME))
                    .expense(sumAmount(entry.getValue(), FinancialRecordType.EXPENSE))
                    .build());
        }
        return monthlyTrendList;
    }

    @Override
    public List<BreakdownDto> getIncomeBreakdown(Long fiscalYearId) {
        return getBreakdown(fiscalYearId, FinancialRecordType.INCOME);
    }

    @Override
    public List<BreakdownDto> getExpenseBreakdown(Long fiscalYearId) {
        return getBreakdown(fiscalYearId, FinancialRecordType.EXPENSE);
    }

    @Override
    public List<RecentTransactionDto> getRecentTransactions(Long fiscalYearId) {
        return financialRecordRepository.findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYearId)
                .stream()
                .map(dashboardMapper::toRecentTransactionDto)
                .collect(Collectors.toList());
    }

    /**
     * 회계 연도에 속한 내역을 조회한다. type이 null이면 수입과 지출을 모두 조회한다.
     */
    private List<FinancialRecord> findRecords(Long fiscalYearId, FinancialRecordType type) {
        return financialRecordRepository.findAll(FinancialRecordSpecification.searchBy(
                FinancialRecordSearchRequestDto.builder()
                        .type(type)
                        .fiscalYearId(fiscalYearId)
                        .build(),
                null));
    }

    private long sumAmount(List<FinancialRecord> records, FinancialRecordType type) {
        return records.stream()
                .filter(record -> record.getType() == type)
                .mapToLong(FinancialRecord::getAmount)
                .sum();
    }

    private List<BreakdownDto> getBreakdown(Long fiscalYearId, FinancialRecordType type) {
        return findRecords(fiscalYearId, type).stream()
                .collect(Collectors.groupingBy(record -> record.getBudget().getName(),
                        Collectors.summingLong(FinancialRecord::getAmount)))
                .entrySet().stream()
                .map(e -> BreakdownDto.builder().categoryName(e.getKey()).amount(e.getValue()).build())
                .sorted(Comparator.comparingLong(BreakdownDto::getAmount).reversed())
                .collect(Collectors.toList());
    }

    private long calculateActualAmount(Budget budget, List<FinancialRecord> records) {
        Set<Long> budgetIds = getAllChildBudgetIds(budget);
        FinancialRecordType type = budget.getType() == BudgetType.INCOME ? FinancialRecordType.INCOME
                : FinancialRecordType.EXPENSE;
        return records.stream()
                .filter(record -> record.getType() == type)
                .filter(record -> budgetIds.contains(record.getBudget().getId()))
                .mapToLong(FinancialRecord::getAmount)
                .sum();
    }

    private Set<Long> getAllChildBudgetIds(Budget budget) {
        Set<Long> ids = new HashSet<>();
        ids.add(budget.getId());
        if (budget.getChildren() != null) {
            for (Budget child : budget.getChildren()) {
                ids.addAll(getAllChildBudgetIds(child));
            }
        }
        return ids;
    }
}
