package org.church.steward.domain.report.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.dashboard.service.DashboardService;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.financialrecord.specification.FinancialRecordSpecification;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.church.steward.domain.report.dto.AnnualSettlementReportDto;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.BudgetPlanReportDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportOutputType;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.church.steward.domain.report.dto.ReportSummaryDto;
import org.church.steward.domain.report.dto.ReportType;
import org.church.steward.domain.report.dto.SettlementDetailReportDto;
import org.church.steward.domain.report.dto.WeeklySettlementReportDto;
import org.church.steward.domain.report.mapper.ReportMapper;
import org.church.steward.domain.report.service.ReportService;
import org.church.steward.domain.report.service.impl.BudgetTree.Node;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportServiceImpl implements ReportService {

    /** 한 예산 안의 내역은 날짜순으로, 같은 날짜는 입력순으로 정렬한다. */
    private static final Comparator<FinancialRecord> RECORD_ORDER = Comparator.comparing(FinancialRecord::getDate)
            .thenComparing(FinancialRecord::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final FinancialRecordRepository financialRecordRepository;
    private final BudgetRepository budgetRepository;
    private final FiscalYearRepository fiscalYearRepository;
    private final DashboardService dashboardService;
    private final ActivityLogService activityLogService;
    private final ReportMapper reportMapper;

    @Override
    @Transactional(readOnly = true)
    public WeeklySettlementReportDto getWeeklySettlementReport(Long fiscalYearId, LocalDate startDate,
            LocalDate endDate) {
        validatePeriod(startDate, endDate);
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);

        // 종료일까지의 내역을 한 번에 조회해 시작일 전(전주까지)과 해당 주간으로 나눈다.
        List<FinancialRecord> records = findRecords(fiscalYearId, endDate);
        List<FinancialRecord> previousRecords = new ArrayList<>();
        List<FinancialRecord> weeklyRecords = new ArrayList<>();
        for (FinancialRecord record : records) {
            if (record.getDate()
                    .isBefore(startDate)) {
                previousRecords.add(record);
            } else {
                weeklyRecords.add(record);
            }
        }

        BudgetTree incomeTree = loadTree(fiscalYearId, BudgetType.INCOME);
        BudgetTree expenseTree = loadTree(fiscalYearId, BudgetType.EXPENSE);

        return WeeklySettlementReportDto.builder()
                .fiscalYearName(fiscalYear.getName())
                .startDate(startDate)
                .endDate(endDate)
                .previousBalance(summarize(previousRecords).getDifference())
                .weekly(summarize(weeklyRecords))
                .incomeStatus(toExecutionRows(incomeTree, incomeTree.getRoots(), records, FinancialRecordType.INCOME))
                .expenseStatus(
                        toExecutionRows(expenseTree, expenseTree.getRoots(), records, FinancialRecordType.EXPENSE))
                .incomeRecords(groupByBudget(incomeTree, weeklyRecords, FinancialRecordType.INCOME))
                .expenseRecords(groupByBudget(expenseTree, weeklyRecords, FinancialRecordType.EXPENSE))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BudgetPlanReportDto getBudgetPlanReport(Long fiscalYearId, BudgetType type) {
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);
        BudgetTree tree = loadTree(fiscalYearId, type);
        long totalAmount = tree.getTotalBudgetedAmount();

        Optional<FiscalYear> previousFiscalYear = findPreviousFiscalYear(fiscalYear);
        BudgetTree previousTree = previousFiscalYear.map(previous -> loadTree(previous.getId(), type))
                .orElseGet(() -> BudgetTree.of(List.of()));

        Map<List<String>, Node> previousNodesByPath = new HashMap<>();
        for (Node node : previousTree.flatten()) {
            // 같은 경로의 예산이 여럿이면 표시 순서가 앞선 것과 비교한다.
            previousNodesByPath.putIfAbsent(node.getPath(), node);
        }

        Set<List<String>> currentPaths = new HashSet<>();
        List<BudgetPlanRowDto> rows = new ArrayList<>();
        for (Node node : tree.flatten()) {
            List<String> path = node.getPath();
            currentPaths.add(path);

            Node previousNode = previousNodesByPath.get(path);
            rows.add(reportMapper.toBudgetPlanRowDto(node.getBudget(), node.getDepth(), node.getBudgetedAmount(),
                    percent(node.getBudgetedAmount(), totalAmount),
                    previousNode == null ? null : previousNode.getBudgetedAmount()));
        }

        return BudgetPlanReportDto.builder()
                .fiscalYearName(fiscalYear.getName())
                .type(type)
                .rows(rows)
                .totalAmount(totalAmount)
                .previousTotalAmount(previousFiscalYear.isPresent() ? previousTree.getTotalBudgetedAmount() : null)
                .removedAmount(sumRemovedAmount(previousTree, currentPaths))
                .build();
    }

    /**
     * 직전 회계 연도에만 있는 예산들의 금액을 더한다. 상위 예산도 함께 없어진 예산은 상위 예산의 금액에 이미 들어 있으므로 세지 않는다.
     *
     * @return 직전 회계 연도에만 있는 예산이 없으면 null
     */
    private Long sumRemovedAmount(BudgetTree previousTree, Set<List<String>> currentPaths) {
        Long removedAmount = null;
        for (Node node : previousTree.flatten()) {
            if (currentPaths.contains(node.getPath())) {
                continue;
            }
            Node parent = node.getParent();
            if (parent == null || currentPaths.contains(parent.getPath())) {
                removedAmount = (removedAmount == null ? 0L : removedAmount) + node.getBudgetedAmount();
            }
        }
        return removedAmount;
    }

    @Override
    @Transactional(readOnly = true)
    public AnnualSettlementReportDto getAnnualSettlementReport(Long fiscalYearId) {
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);
        List<FinancialRecord> records = findRecords(fiscalYearId, null);
        BudgetTree incomeTree = loadTree(fiscalYearId, BudgetType.INCOME);
        BudgetTree expenseTree = loadTree(fiscalYearId, BudgetType.EXPENSE);

        Optional<FiscalYear> previousFiscalYear = findPreviousFiscalYear(fiscalYear);
        ReportSummaryDto previousSummary = previousFiscalYear
                .map(previous -> summarize(findRecords(previous.getId(), null)))
                .orElse(null);

        return AnnualSettlementReportDto.builder()
                .fiscalYearName(fiscalYear.getName())
                .previousFiscalYearName(previousFiscalYear.map(FiscalYear::getName)
                        .orElse(null))
                .previousYear(previousSummary)
                .currentYear(summarize(records))
                .monthlyTrend(dashboardService.getMonthlyTrend(fiscalYearId))
                .incomeStatus(toExecutionRows(incomeTree, incomeTree.getRoots(), records, FinancialRecordType.INCOME))
                .expenseStatus(
                        toExecutionRows(expenseTree, expenseTree.getRoots(), records, FinancialRecordType.EXPENSE))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementDetailReportDto getSettlementDetailReport(Long fiscalYearId, BudgetType type) {
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);
        FinancialRecordType recordType = toRecordType(type);
        List<FinancialRecord> records = findRecords(fiscalYearId, null);
        BudgetTree tree = loadTree(fiscalYearId, type);

        long totalBudgetedAmount = tree.getTotalBudgetedAmount();
        long totalActualAmount = sumAmount(records, recordType);

        return SettlementDetailReportDto.builder()
                .fiscalYearName(fiscalYear.getName())
                .type(type)
                .rows(toExecutionRows(tree, tree.flatten(), records, recordType))
                .totalBudgetedAmount(totalBudgetedAmount)
                .totalActualAmount(totalActualAmount)
                .totalExecutionRate(percent(totalActualAmount, totalBudgetedAmount))
                .build();
    }

    @Override
    public void logOutput(ReportType reportType, ReportOutputType outputType, Long fiscalYearId, LocalDate startDate,
            LocalDate endDate) {
        String target = findFiscalYear(fiscalYearId).getName();
        if (reportType.isWeekly()) {
            validatePeriod(startDate, endDate);
            target += " (" + startDate + " ~ " + endDate + ")";
        }
        activityLogService.logReportPrint(reportType.getLabel(), target, outputType.getLabel());
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("보고서의 기간을 지정해주세요.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("보고서 기간의 시작일이 종료일보다 늦습니다.");
        }
    }

    private FiscalYear findFiscalYear(Long fiscalYearId) {
        if (fiscalYearId == null) {
            throw new IllegalArgumentException("존재하지 않는 회계 연도입니다.");
        }
        return fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
    }

    /**
     * 직전 회계 연도, 곧 시작일이 주어진 회계 연도보다 앞선 회계 연도 중 가장 최근 것을 찾는다.
     * 두 회계 연도 사이에 빈 기간이 있어도 직전 회계 연도로 본다.
     */
    private Optional<FiscalYear> findPreviousFiscalYear(FiscalYear fiscalYear) {
        return fiscalYearRepository.findFirstByStartDateLessThanOrderByStartDateDesc(fiscalYear.getStartDate());
    }

    /**
     * 회계 연도에 속한 수입/지출 내역을 조회한다.
     *
     * @param endDate 이 날짜까지의 내역만 조회한다. null이면 날짜를 제한하지 않는다.
     */
    private List<FinancialRecord> findRecords(Long fiscalYearId, LocalDate endDate) {
        return financialRecordRepository.findAll(FinancialRecordSpecification.searchBy(
                FinancialRecordSearchRequestDto.builder()
                        .fiscalYearId(fiscalYearId)
                        .endDate(endDate)
                        .build(),
                null));
    }

    private BudgetTree loadTree(Long fiscalYearId, BudgetType type) {
        return BudgetTree.of(budgetRepository.findAllByFiscalYearIdAndType(fiscalYearId, type));
    }

    private ReportSummaryDto summarize(List<FinancialRecord> records) {
        return ReportSummaryDto.builder()
                .income(sumAmount(records, FinancialRecordType.INCOME))
                .expense(sumAmount(records, FinancialRecordType.EXPENSE))
                .build();
    }

    private long sumAmount(List<FinancialRecord> records, FinancialRecordType type) {
        return records.stream()
                .filter(record -> record.getType() == type)
                .mapToLong(FinancialRecord::getAmount)
                .sum();
    }

    /**
     * 지정한 예산들의 예산액과 집행액을 계산한다. 집행액에는 하위 예산에 등록된 내역도 포함한다.
     *
     * @param nodes   결과에 담을 예산들. 최상위 예산만 넘기면 최상위 예산별 현황이 된다.
     * @param records 집행액으로 합산할 내역. 구분이 다른 내역은 무시한다.
     */
    private List<BudgetExecutionDto> toExecutionRows(BudgetTree tree, List<Node> nodes, List<FinancialRecord> records,
            FinancialRecordType type) {
        Map<Long, Long> amountsByBudgetId = new HashMap<>();
        for (FinancialRecord record : records) {
            if (record.getType() == type) {
                amountsByBudgetId.merge(record.getBudget()
                        .getId(), record.getAmount(), Long::sum);
            }
        }

        long totalBudgetedAmount = tree.getTotalBudgetedAmount();
        List<BudgetExecutionDto> rows = new ArrayList<>();
        for (Node node : nodes) {
            long actualAmount = node.sumActualAmount(amountsByBudgetId);
            rows.add(reportMapper.toBudgetExecutionDto(node.getBudget(), node.getDepth(), node.getBudgetedAmount(),
                    actualAmount,
                    percent(node.getBudgetedAmount(), totalBudgetedAmount),
                    percent(actualAmount, node.getBudgetedAmount())));
        }
        return rows;
    }

    /**
     * 내역을 예산별로 묶는다. 예산은 예산 관리에서 정한 순서를 따르고, 내역이 없는 예산은 싣지 않는다.
     */
    private List<ReportRecordGroupDto> groupByBudget(BudgetTree tree, List<FinancialRecord> records,
            FinancialRecordType type) {
        Map<Long, List<FinancialRecord>> recordsByBudgetId = new LinkedHashMap<>();
        for (FinancialRecord record : records) {
            if (record.getType() == type) {
                recordsByBudgetId.computeIfAbsent(record.getBudget()
                        .getId(), _ -> new ArrayList<>())
                        .add(record);
            }
        }

        List<ReportRecordGroupDto> groups = new ArrayList<>();
        for (Node node : tree.flatten()) {
            List<FinancialRecord> budgetRecords = recordsByBudgetId.remove(node.getBudget()
                    .getId());
            if (budgetRecords != null) {
                groups.add(toRecordGroup(node.getHierarchyName(), budgetRecords));
            }
        }
        // 예산 목록에서 찾지 못한 예산의 내역도 합계가 어긋나지 않도록 뒤에 싣는다.
        for (List<FinancialRecord> budgetRecords : recordsByBudgetId.values()) {
            groups.add(toRecordGroup(budgetRecords.getFirst()
                    .getBudget()
                    .getHierarchyName(), budgetRecords));
        }
        return groups;
    }

    /**
     * 한 예산의 내역을 정렬하고 합계를 내어 묶음으로 만든다.
     */
    private ReportRecordGroupDto toRecordGroup(String budgetName, List<FinancialRecord> records) {
        List<FinancialRecord> sortedRecords = records.stream()
                .sorted(RECORD_ORDER)
                .toList();
        long totalAmount = records.stream()
                .mapToLong(FinancialRecord::getAmount)
                .sum();

        return reportMapper.toRecordGroupDto(budgetName, totalAmount, sortedRecords);
    }

    private static FinancialRecordType toRecordType(BudgetType type) {
        return type == BudgetType.INCOME ? FinancialRecordType.INCOME : FinancialRecordType.EXPENSE;
    }

    /**
     * 전체에 대한 비율(%)을 구한다. 전체가 0 이하이면 비율을 정할 수 없으므로 null을 반환한다.
     */
    private static Double percent(long part, long whole) {
        return whole <= 0 ? null : part * 100.0 / whole;
    }
}
