package org.church.steward.domain.datatransfer.service.impl;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.datatransfer.service.DataExportService;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetTable;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DataExportServiceImpl implements DataExportService {

    // 표의 이름. 엑셀의 시트 이름이 되므로 빗금을 쓰지 않는다.
    private static final String BUDGET_TABLE_NAME = "예산";
    private static final String RECORD_TABLE_NAME = "수입지출";
    private static final String CONTACT_TABLE_NAME = "관계자";

    // 활동 기록에 적는 대상명
    private static final String BUDGET_TARGET_NAME = "예산";
    private static final String RECORD_TARGET_NAME = "수입/지출";
    private static final String CONTACT_TARGET_NAME = "관계자";

    private static final String HEADER_TYPE = "구분";
    private static final String HEADER_DATE = "일자";
    private static final String HEADER_BUDGET_LEVEL_PREFIX = "예산 ";
    private static final String HEADER_BUDGET_LEVEL_SUFFIX = "단계";
    private static final String HEADER_BUDGET_AMOUNT = "예산액";
    private static final String HEADER_ACTUAL_AMOUNT = "수입액/집행액";
    private static final String HEADER_AMOUNT = "금액";
    private static final String HEADER_DESCRIPTION = "내용";
    private static final String HEADER_CONTACTS = "봉헌자/수취인";
    private static final String HEADER_REMARKS = "비고";
    private static final String HEADER_NAME = "이름";
    private static final String HEADER_STATUS = "상태";

    // 관계자의 상태. 관계자 관리 화면의 "상태" 칸과 같은 글자다.
    private static final String STATUS_HIDDEN = "숨김";
    private static final String STATUS_VISIBLE = "표시";

    private static final String CONTACT_SEPARATOR = ", ";

    /** 관계자를 내보내는 순서: 구분(관계자 관리 화면의 구분 순서), 이름, 등록 순 */
    private static final Comparator<Contact> CONTACT_ORDER = Comparator.comparing(Contact::getType)
            .thenComparing(Contact::getName)
            .thenComparing(Contact::getId);

    /** 빈 양식에 두는 예산 단계의 수 */
    private static final int TEMPLATE_BUDGET_LEVELS = 3;

    private final FiscalYearRepository fiscalYearRepository;
    private final FinancialRecordRepository financialRecordRepository;
    private final ContactRepository contactRepository;
    private final BudgetService budgetService;
    private final ActivityLogService activityLogService;
    private final SpreadsheetWriter spreadsheetWriter;

    /**
     * 예산과 그 예산까지의 단계별 이름(최상위 예산부터).
     */
    private record BudgetRow(BudgetDto budget, List<String> levelNames) {
    }

    @Override
    public int exportBudgets(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);

        List<BudgetRow> budgetRows = new ArrayList<>();
        for (BudgetType type : BudgetType.values()) {
            List<BudgetDto> budgets = budgetService.getBudgetsWithActualAmount(fiscalYearId, type);
            collectBudgetRows(budgets, null, List.of(), budgetRows);
        }

        int levels = budgetRows.stream()
                .mapToInt(row -> row.levelNames().size())
                .max()
                .orElse(1);
        List<List<Object>> rows = new ArrayList<>();
        for (BudgetRow budgetRow : budgetRows) {
            List<Object> row = new ArrayList<>();
            row.add(budgetRow.budget().getType().getLabel());
            addLevelNames(row, budgetRow.levelNames(), levels);
            row.add(budgetRow.budget().getTotalAmount());
            row.add(budgetRow.budget().getActualAmount());
            rows.add(row);
        }

        spreadsheetWriter.write(target, format, new SpreadsheetTable(BUDGET_TABLE_NAME, budgetHeaders(levels), rows),
                password);
        activityLogService.logExport(BUDGET_TARGET_NAME, fiscalYear.getName(), target.getFileName().toString(),
                rows.size());
        return rows.size();
    }

    /**
     * 상위 예산 바로 아래의 예산들을 정렬 순서대로, 각 예산 다음에 그 하위 예산이 오도록 모은다.
     */
    private void collectBudgetRows(List<BudgetDto> budgets, Long parentId, List<String> parentLevelNames,
            List<BudgetRow> result) {
        List<BudgetDto> children = budgets.stream()
                .filter(budget -> Objects.equals(budget.getParentId(), parentId))
                .sorted(Comparator.comparingInt(BudgetDto::getSortOrder))
                .toList();
        for (BudgetDto child : children) {
            List<String> levelNames = new ArrayList<>(parentLevelNames);
            levelNames.add(child.getName());
            result.add(new BudgetRow(child, levelNames));
            collectBudgetRows(budgets, child.getId(), levelNames, result);
        }
    }

    @Override
    public int exportFinancialRecords(Long fiscalYearId, Path target, SpreadsheetFormat format, String password) {
        FiscalYear fiscalYear = findFiscalYear(fiscalYearId);
        List<FinancialRecord> records = financialRecordRepository
                .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYearId);

        List<List<String>> levelNamesOfRecords = records.stream()
                .map(financialRecord -> levelNames(financialRecord.getBudget()))
                .toList();
        int levels = levelNamesOfRecords.stream()
                .mapToInt(List::size)
                .max()
                .orElse(1);

        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            FinancialRecord financialRecord = records.get(i);
            List<Object> row = new ArrayList<>();
            row.add(financialRecord.getType().getLabel());
            row.add(financialRecord.getDate());
            addLevelNames(row, levelNamesOfRecords.get(i), levels);
            row.add(financialRecord.getAmount());
            row.add(financialRecord.getDescription());
            // 내역에 기록된 당시의 이름을 적는다.
            row.add(financialRecord.getContacts()
                    .stream()
                    .map(FinancialRecordContact::getContactName)
                    .collect(Collectors.joining(CONTACT_SEPARATOR)));
            row.add(financialRecord.getRemarks());
            rows.add(row);
        }

        spreadsheetWriter.write(target, format, new SpreadsheetTable(RECORD_TABLE_NAME, recordHeaders(levels), rows),
                password);
        activityLogService.logExport(RECORD_TARGET_NAME, fiscalYear.getName(), target.getFileName().toString(),
                rows.size());
        return rows.size();
    }

    @Override
    public int exportContacts(Path target, SpreadsheetFormat format, String password) {
        List<List<Object>> rows = new ArrayList<>();
        for (Contact contact : contactRepository.findAll()
                .stream()
                .sorted(CONTACT_ORDER)
                .toList()) {
            List<Object> row = new ArrayList<>();
            row.add(contact.getType().getLabel());
            row.add(contact.getName());
            row.add(contact.getRemarks());
            row.add(contact.isHidden() ? STATUS_HIDDEN : STATUS_VISIBLE);
            rows.add(row);
        }

        spreadsheetWriter.write(target, format, new SpreadsheetTable(CONTACT_TABLE_NAME, contactHeaders(), rows),
                password);
        activityLogService.logExport(CONTACT_TARGET_NAME, null, target.getFileName().toString(), rows.size());
        return rows.size();
    }

    @Override
    @Transactional(readOnly = true)
    public void exportBudgetTemplate(Path target, SpreadsheetFormat format) {
        spreadsheetWriter.write(target, format,
                new SpreadsheetTable(BUDGET_TABLE_NAME, budgetHeaders(TEMPLATE_BUDGET_LEVELS), List.of()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportFinancialRecordTemplate(Path target, SpreadsheetFormat format) {
        spreadsheetWriter.write(target, format,
                new SpreadsheetTable(RECORD_TABLE_NAME, recordHeaders(TEMPLATE_BUDGET_LEVELS), List.of()), null);
    }

    @Override
    @Transactional(readOnly = true)
    public void exportContactTemplate(Path target, SpreadsheetFormat format) {
        spreadsheetWriter.write(target, format, new SpreadsheetTable(CONTACT_TABLE_NAME, contactHeaders(), List.of()),
                null);
    }

    private FiscalYear findFiscalYear(Long fiscalYearId) {
        return fiscalYearRepository.findById(fiscalYearId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회계 연도입니다."));
    }

    /**
     * 최상위 예산부터 주어진 예산까지의 이름.
     */
    private List<String> levelNames(Budget budget) {
        List<String> names = new ArrayList<>();
        Budget current = budget;
        while (current != null) {
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return names;
    }

    /**
     * 단계별 예산명을 칸마다 하나씩 넣고, 단계가 모자라면 남은 칸을 비워 둔다.
     */
    private void addLevelNames(List<Object> row, List<String> levelNames, int levels) {
        for (int level = 0; level < levels; level++) {
            row.add(level < levelNames.size() ? levelNames.get(level) : null);
        }
    }

    private List<String> budgetHeaders(int levels) {
        List<String> headers = new ArrayList<>();
        headers.add(HEADER_TYPE);
        addLevelHeaders(headers, levels);
        headers.add(HEADER_BUDGET_AMOUNT);
        headers.add(HEADER_ACTUAL_AMOUNT);
        return headers;
    }

    private List<String> recordHeaders(int levels) {
        List<String> headers = new ArrayList<>();
        headers.add(HEADER_TYPE);
        headers.add(HEADER_DATE);
        addLevelHeaders(headers, levels);
        headers.add(HEADER_AMOUNT);
        headers.add(HEADER_DESCRIPTION);
        headers.add(HEADER_CONTACTS);
        headers.add(HEADER_REMARKS);
        return headers;
    }

    private List<String> contactHeaders() {
        return List.of(HEADER_TYPE, HEADER_NAME, HEADER_REMARKS, HEADER_STATUS);
    }

    /**
     * "예산 1단계", "예산 2단계", ... 머리글을 넣는다.
     */
    private void addLevelHeaders(List<String> headers, int levels) {
        for (int level = 1; level <= levels; level++) {
            headers.add(HEADER_BUDGET_LEVEL_PREFIX + level + HEADER_BUDGET_LEVEL_SUFFIX);
        }
    }

}
