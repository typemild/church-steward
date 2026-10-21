package org.church.steward.domain.datatransfer.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.domain.datatransfer.dto.ImportMappingDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

/**
 * 수입/지출 가져오기의 분석기. 파일의 행들을 내역으로 읽고, 예산과 관계자를 등록된 것에 연결하고,
 * 행마다 가져올 수 있는지를 정한다. 데이터베이스를 직접 읽지 않고 필요한 것을 인자로 받는다.
 * <p>
 * 분석은 네 단계다.
 * <ol>
 * <li>행 읽기: 구분, 금액, 일자, 예산, 내용, 관계자, 비고를 읽는다. 일자와 금액이 모두 없는 행은 건너뛴다.</li>
 * <li>예산 연결: 파일에 나온 예산 값마다 등록된 말단 예산을 찾는다. 사용자가 직접 한 매핑이 자동 매핑보다 우선한다.</li>
 * <li>관계자 연결: 파일에 나온 이름마다 등록된 관계자를 찾는다. 예산과 관계자는 자동으로 만들지 않는다.</li>
 * <li>행 판정: 연결되지 않은 값을 쓰는 행은 오류, 이미 같은 내역이 있으면 중복 의심으로 본다.</li>
 * </ol>
 */
final class RecordImportAnalyzer {

    /** 내용과 비고의 최대 길이 */
    private static final int TEXT_MAX_LENGTH = 100;
    /** 금액의 최대값. 수입/지출 서비스의 검증과 같다. */
    private static final long MAX_AMOUNT = 100_000_000_000L;

    private static final String BLANK_LABEL = "(빈 칸)";
    private static final String CONTACT_SEPARATOR = ",";
    private static final String PATH_SEPARATOR = ">";
    private static final String LABEL_SEPARATOR = " > ";

    private static final Map<String, FinancialRecordType> TYPE_NAMES = Map.of(
            "수입", FinancialRecordType.INCOME,
            "입금", FinancialRecordType.INCOME,
            "지출", FinancialRecordType.EXPENSE,
            "출금", FinancialRecordType.EXPENSE);

    private static final Pattern DATE_FULL = Pattern
            .compile("^(\\d{4})\\s*[-./년]\\s*(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})\\s*[.일]?$");
    private static final Pattern DATE_COMPACT = Pattern.compile("^(\\d{4})(\\d{2})(\\d{2})$");
    private static final Pattern DATE_SHORT_YEAR = Pattern
            .compile("^(\\d{2})\\s*[-./]\\s*(\\d{1,2})\\s*[-./]\\s*(\\d{1,2})\\s*\\.?$");
    private static final Pattern DATE_MONTH_DAY = Pattern.compile("^(\\d{1,2})\\s*[-./월]\\s*(\\d{1,2})\\s*[.일]?$");

    private final RecordImportRequestDto request;
    private final LocalDate periodStart;
    private final LocalDate periodEnd;
    private final Map<Long, BudgetInfo> budgetsById = new HashMap<>();
    private final Map<Long, Contact> contactsById = new HashMap<>();
    private final Map<String, List<Contact>> contactsByName = new HashMap<>();
    private final List<FinancialRecord> existingRecords;

    private final List<Row> rows = new ArrayList<>();
    private final Map<String, Mapping> typeMappings = new LinkedHashMap<>();
    private final Map<String, Mapping> budgetMappings = new LinkedHashMap<>();
    private final Map<String, Mapping> contactMappings = new LinkedHashMap<>();
    private int blankRowCount;

    /**
     * 등록된 예산 하나.
     *
     * @param names 최상위 예산부터 이 예산까지의 이름
     * @param leaf  하위 예산이 없는지 여부. 내역은 말단 예산에만 등록할 수 있다.
     */
    private record BudgetInfo(Long id, BudgetType type, List<String> names, boolean leaf) {
        private String label() {
            return String.join(LABEL_SEPARATOR, names);
        }
    }

    /**
     * 읽고 있는 파일의 한 행.
     */
    private static final class Row {
        private final int rowNumber;
        private FinancialRecordType type;
        private Long amount;
        private LocalDate date;
        private String budgetKey;
        private String budgetSource = "";
        private String description;
        private String remarks;
        private List<String> contactNames = List.of();
        private List<String> contactKeys = List.of();
        private Long budgetId;
        private String budgetLabel = "";
        private List<Long> contactIds = List.of();
        private String contactLabel = "";
        /** 가져올 수 없는 사유. 가져올 수 있으면 null */
        private String error;
        private final List<String> warnings = new ArrayList<>();
        private boolean duplicate;

        private Row(int rowNumber) {
            this.rowNumber = rowNumber;
        }

        /**
         * 가져올 수 없는 사유를 적는다. 사유가 여럿이면 처음 것만 남긴다.
         */
        private void fail(String message) {
            if (error == null) {
                error = message;
            }
        }
    }

    /**
     * 파일의 값 하나와 등록된 예산 또는 관계자의 연결.
     */
    private static final class Mapping {
        private final String key;
        private final String label;
        private final boolean blank;
        private final String typeLabel;
        private int count;
        private long totalAmount;
        /** 예산을 연결하는 항목에서만 쓴다. */
        private Long targetId;
        /** 관계자를 연결하는 항목에서만 쓴다. 수입의 이름 하나는 여러 명에 연결할 수 있다. */
        private List<Long> targetIds = List.of();
        /** 구분을 연결하는 항목에서만 쓴다. */
        private FinancialRecordType targetType;
        private String targetLabel = "";
        private boolean manual;
        private String note = "";

        /**
         * @param value 파일에 적힌 값. 빈 문자열이면 빈 칸 항목이다.
         */
        private Mapping(String key, String value, String typeLabel) {
            this.key = key;
            this.blank = value.isEmpty();
            this.label = blank ? BLANK_LABEL : value;
            this.typeLabel = typeLabel;
        }
    }

    private RecordImportAnalyzer(RecordImportRequestDto request, LocalDate periodStart, LocalDate periodEnd,
            List<Budget> budgets, List<Contact> contacts, List<FinancialRecord> existingRecords) {
        this.request = request;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.existingRecords = existingRecords;
        for (Budget budget : budgets) {
            budgetsById.put(budget.getId(),
                    new BudgetInfo(budget.getId(), budget.getType(), namesOf(budget), !budget.hasChildren()));
        }
        for (Contact contact : contacts) {
            contactsById.put(contact.getId(), contact);
            contactsByName.computeIfAbsent(contact.getName(), _ -> new ArrayList<>())
                    .add(contact);
        }
    }

    /**
     * 파일의 행들을 분석한다.
     *
     * @param request         파일에서 읽은 행, 열 지정, 사용자가 직접 한 매핑
     * @param periodStart     가져올 회계 연도의 시작일
     * @param periodEnd       가져올 회계 연도의 종료일
     * @param budgets         가져올 회계 연도의 예산 전체
     * @param contacts        등록된 관계자 전체 (숨김 포함)
     * @param existingRecords 가져올 회계 연도에 이미 있는 내역 전체. 중복을 가리는 데 쓴다.
     * @throws IllegalArgumentException 열 지정이 모자랄 때 발생
     */
    static RecordImportAnalysisDto analyze(RecordImportRequestDto request, LocalDate periodStart,
            LocalDate periodEnd, List<Budget> budgets, List<Contact> contacts,
            List<FinancialRecord> existingRecords) {
        validate(request);

        RecordImportAnalyzer analyzer = new RecordImportAnalyzer(request, periodStart, periodEnd, budgets, contacts,
                existingRecords);
        analyzer.readRows();
        analyzer.resolveBudgets();
        analyzer.resolveContacts();
        analyzer.judgeRows();
        analyzer.markDuplicates();

        List<RecordImportItemDto> items = analyzer.rows.stream()
                .map(RecordImportAnalyzer::toDto)
                .toList();
        List<ImportMappingDto> typeMappings = toDtos(analyzer.typeMappings);
        List<ImportMappingDto> budgetMappings = toDtos(analyzer.budgetMappings);
        List<ImportMappingDto> contactMappings = toDtos(analyzer.contactMappings);
        return RecordImportAnalysisDto.builder()
                .items(items)
                .typeMappings(typeMappings)
                .budgetMappings(budgetMappings)
                .contactMappings(contactMappings)
                .blankRowCount(analyzer.blankRowCount)
                .signature(sign(items, List.of(typeMappings, budgetMappings, contactMappings)))
                .build();
    }

    private static void validate(RecordImportRequestDto request) {
        boolean splitAmounts = request.getIncomeAmountColumn() != null && request.getExpenseAmountColumn() != null;
        if (!splitAmounts) {
            if (request.getFixedType() == null && request.getTypeColumn() == null) {
                throw new IllegalArgumentException("구분을 지정해주세요.");
            }
            if (request.getAmountColumn() == null) {
                throw new IllegalArgumentException("금액이 있는 열을 지정해주세요.");
            }
        }
        if (request.getDateColumn() == null
                && (request.getMonthColumn() == null || request.getDayColumn() == null)) {
            throw new IllegalArgumentException("일자가 있는 열을 지정해주세요.");
        }
        if (request.getBudgetPathColumn() == null && request.getBudgetLevelColumns().isEmpty()) {
            throw new IllegalArgumentException("예산이 있는 열을 지정해주세요.");
        }
    }

    private boolean isSplitAmounts() {
        return request.getIncomeAmountColumn() != null && request.getExpenseAmountColumn() != null;
    }

    // ------------------------------------------------------------------ 1. 행 읽기

    private void readRows() {
        for (int i = 0; i < request.getRows().size(); i++) {
            List<Object> values = request.getRows().get(i);
            if (isBlank(values)) {
                blankRowCount++;
                continue;
            }

            Row row = new Row(request.getFirstRowNumber() + i);
            readTypeAndAmount(row, values);
            readDate(row, values);
            readBudget(row, values);
            row.description = truncate(row, "내용", ImportCells.text(values, request.getDescriptionColumn()));
            row.remarks = truncate(row, "비고", ImportCells.text(values, request.getRemarksColumn()));
            readContacts(row, values);
            rows.add(row);
        }
    }

    /**
     * 일자와 금액이 모두 없는 행인지 여부. 합계 행이나 빈 줄처럼 내역이 아닌 행을 걸러낸다.
     */
    private boolean isBlank(List<Object> values) {
        boolean noDate = request.getDateColumn() != null
                ? ImportCells.text(values, request.getDateColumn()).isEmpty()
                : ImportCells.text(values, request.getMonthColumn()).isEmpty()
                        && ImportCells.text(values, request.getDayColumn()).isEmpty();
        boolean noAmount = isSplitAmounts()
                ? ImportCells.text(values, request.getIncomeAmountColumn()).isEmpty()
                        && ImportCells.text(values, request.getExpenseAmountColumn()).isEmpty()
                : ImportCells.text(values, request.getAmountColumn()).isEmpty();
        return noDate && noAmount;
    }

    private void readTypeAndAmount(Row row, List<Object> values) {
        try {
            if (isSplitAmounts()) {
                readSplitAmounts(row, values);
            } else {
                row.amount = ImportCells.amount(ImportCells.cell(values, request.getAmountColumn()));
            }
            if (row.amount == null) {
                row.fail("금액이 비어 있습니다.");
            } else if (row.amount > MAX_AMOUNT) {
                row.fail("금액이 허용된 범위를 초과했습니다. (최대 금액: 1,000억)");
            }
        } catch (IllegalArgumentException e) {
            row.fail(e.getMessage());
        }

        // 구분 매핑 항목에 금액 합계를 보여주기 위해, 구분은 금액을 읽은 뒤에 읽는다.
        if (!isSplitAmounts()) {
            row.type = readType(row, values);
        }
    }

    /**
     * 수입 금액과 지출 금액의 열이 따로 있는 파일에서, 금액이 적힌 쪽으로 구분을 정한다.
     * 한쪽에 0이 적혀 있으면 비어 있는 것으로 본다.
     */
    private void readSplitAmounts(Row row, List<Object> values) {
        Long income = ImportCells.amount(ImportCells.cell(values, request.getIncomeAmountColumn()));
        Long expense = ImportCells.amount(ImportCells.cell(values, request.getExpenseAmountColumn()));
        boolean hasIncome = income != null && income != 0;
        boolean hasExpense = expense != null && expense != 0;

        if (hasIncome && hasExpense) {
            throw new IllegalArgumentException("수입 금액과 지출 금액이 모두 적혀 있습니다.");
        }
        if (hasIncome) {
            row.type = FinancialRecordType.INCOME;
            row.amount = income;
        } else if (hasExpense) {
            row.type = FinancialRecordType.EXPENSE;
            row.amount = expense;
        }
    }

    /**
     * 구분을 읽는다. 구분 열에서 읽을 때는 열에 나온 값마다 매핑 항목을 만들어, 알 수 없는 값을 사용자가
     * 수입 또는 지출로 지정할 수 있게 한다.
     *
     * @return 구분. 구분이 비어 있거나 알 수 없는 값이면 null이며 이때 행은 오류가 된다.
     */
    private FinancialRecordType readType(Row row, List<Object> values) {
        if (request.getFixedType() != null) {
            return request.getFixedType();
        }
        String text = ImportCells.text(values, request.getTypeColumn());
        if (text.isEmpty()) {
            row.fail("구분이 비어 있습니다.");
            return null;
        }

        Mapping mapping = typeMappings.computeIfAbsent(text, this::resolveType);
        mapping.count++;
        mapping.totalAmount += row.amount == null ? 0 : row.amount;
        if (mapping.targetType == null) {
            row.fail("구분이 지정되지 않았습니다: " + text);
        }
        return mapping.targetType;
    }

    /**
     * 구분 열의 값을 어느 구분으로 읽을지 정한다. 직접 한 매핑이 있으면 그것을 따르고,
     * 없으면 "수입"/"입금"은 수입, "지출"/"출금"은 지출로 읽는다.
     */
    private Mapping resolveType(String text) {
        Mapping mapping = new Mapping(text, text, "");
        FinancialRecordType manual = request.getTypeMappings().get(text);
        FinancialRecordType type = manual != null ? manual : TYPE_NAMES.get(text);
        if (type == null) {
            mapping.note = "알 수 없는 구분입니다. 수입 또는 지출로 지정해주세요.";
        } else {
            mapping.targetType = type;
            mapping.targetLabel = type.getLabel();
            mapping.manual = manual != null;
        }
        return mapping;
    }

    private void readDate(Row row, List<Object> values) {
        try {
            LocalDate date = request.getDateColumn() != null
                    ? parseDate(ImportCells.cell(values, request.getDateColumn()))
                    : toDate(readDatePart(values, request.getYearColumn(), "년"),
                            readDatePart(values, request.getMonthColumn(), "월"),
                            readDatePart(values, request.getDayColumn(), "일"));
            if (date.isBefore(periodStart) || date.isAfter(periodEnd)) {
                throw new IllegalArgumentException(
                        String.format("회계 연도 기간(%s ~ %s)을 벗어난 날짜입니다: %s", periodStart, periodEnd, date));
            }
            row.date = date;
        } catch (IllegalArgumentException e) {
            row.fail(e.getMessage());
        }
    }

    /**
     * 날짜를 읽는다. 엑셀의 날짜 셀과 "2026-03-05", "2026.3.5", "2026년 3월 5일", "20260305",
     * "26.3.5" 형식의 글자를 받는다.
     * "3/5", "3월 5일"처럼 연도가 없으면 회계 연도의 기간으로 연도를 정한다.
     */
    private LocalDate parseDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        String text = ImportCells.toText(value);
        if (text.isEmpty()) {
            throw new IllegalArgumentException("일자가 비어 있습니다.");
        }

        Matcher matcher = DATE_FULL.matcher(text);
        if (!matcher.matches()) {
            matcher = DATE_COMPACT.matcher(text);
        }
        if (matcher.matches()) {
            return toDate(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        matcher = DATE_SHORT_YEAR.matcher(text);
        if (matcher.matches()) {
            return toDate(2000 + Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)));
        }
        matcher = DATE_MONTH_DAY.matcher(text);
        if (matcher.matches()) {
            return toDate(null, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
        }
        throw new IllegalArgumentException("일자를 읽을 수 없습니다: " + text);
    }

    /**
     * 연, 월, 일이 따로 적힌 칸에서 수를 읽는다. "3월"처럼 단위가 붙어 있어도 된다.
     *
     * @return 읽은 수. 열을 지정하지 않았거나 칸이 비어 있으면 null
     */
    private Integer readDatePart(List<Object> values, Integer column, String unit) {
        String text = ImportCells.text(values, column).replace(unit, "").strip();
        if (text.isEmpty()) {
            return null;
        }
        if (!text.matches("\\d{1,4}")) {
            throw new IllegalArgumentException("일자를 읽을 수 없습니다: " + ImportCells.text(values, column));
        }
        return Integer.parseInt(text);
    }

    /**
     * @param year 연도. 없으면 null이며 이때는 회계 연도의 기간 안에 드는 연도를 찾는다.
     */
    private LocalDate toDate(Integer year, Integer month, Integer day) {
        if (month == null || day == null) {
            throw new IllegalArgumentException("일자가 비어 있습니다.");
        }
        try {
            if (year != null) {
                return LocalDate.of(year, month, day);
            }
        } catch (DateTimeException e) {
            throw new IllegalArgumentException(String.format("없는 날짜입니다: %d-%d-%d", year, month, day));
        }

        // 연도가 없으면 회계 연도의 기간에 걸친 해마다 맞춰 보아, 기간 안에 드는 날짜가 하나뿐일 때 그 날짜로 정한다.
        List<LocalDate> candidates = new ArrayList<>();
        for (int candidateYear = periodStart.getYear(); candidateYear <= periodEnd.getYear(); candidateYear++) {
            try {
                LocalDate candidate = LocalDate.of(candidateYear, month, day);
                if (!candidate.isBefore(periodStart) && !candidate.isAfter(periodEnd)) {
                    candidates.add(candidate);
                }
            } catch (DateTimeException e) {
                // 그 해에는 없는 날짜(평년의 2월 29일 등)이므로 후보에서 뺀다.
            }
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException(String.format("회계 연도 기간(%s ~ %s)을 벗어난 날짜입니다: %d월 %d일",
                    periodStart, periodEnd, month, day));
        }
        throw new IllegalArgumentException(String.format("연도가 없어 날짜를 정할 수 없습니다: %d월 %d일", month, day));
    }

    private void readBudget(Row row, List<Object> values) {
        List<String> names = new ArrayList<>();
        if (request.getBudgetPathColumn() != null) {
            for (String name : ImportCells.text(values, request.getBudgetPathColumn()).split(PATH_SEPARATOR)) {
                names.add(name.strip());
            }
        } else {
            for (Integer column : request.getBudgetLevelColumns()) {
                names.add(ImportCells.text(values, column));
            }
        }
        names.removeIf(String::isEmpty);

        row.budgetSource = String.join(LABEL_SEPARATOR, names);
        row.budgetLabel = row.budgetSource;
        if (row.type != null) {
            row.budgetKey = keyOf(row.type, row.budgetSource);
        }
    }

    /**
     * 한 칸에 쉼표로 구분해 적힌 이름들을 읽는다. 같은 이름은 한 번만 센다.
     */
    private void readContacts(Row row, List<Object> values) {
        Set<String> names = new LinkedHashSet<>();
        for (String name : ImportCells.text(values, request.getContactColumn()).split(CONTACT_SEPARATOR)) {
            if (!name.isBlank()) {
                names.add(name.strip());
            }
        }
        row.contactNames = List.copyOf(names);
        row.contactLabel = String.join(", ", names);
        if (row.type == null) {
            return;
        }
        // 이름이 없는 행은 구분마다 하나씩 있는 "(빈 칸)" 항목을 쓴다. "무명" 같은 관계자를 여기에 매핑한다.
        row.contactKeys = names.isEmpty() ? List.of(keyOf(row.type, ""))
                : names.stream()
                        .map(name -> keyOf(row.type, name))
                        .toList();
    }

    /**
     * 예산과 관계자의 매핑 항목 식별자. 값이 동일해도 구분이 다르면 다른 항목이다.
     *
     * @param value 파일에 적힌 값. 빈 칸이면 빈 문자열
     */
    private static String keyOf(FinancialRecordType type, String value) {
        return type.name() + ":" + value;
    }

    /**
     * 내용과 비고는 최대 길이를 넘으면 잘라서 저장하고 경고로 알린다.
     */
    private String truncate(Row row, String fieldName, String text) {
        if (text.length() <= TEXT_MAX_LENGTH) {
            return text;
        }
        row.warnings.add(fieldName + "이(가) " + TEXT_MAX_LENGTH + "자를 넘어 잘립니다.");
        return text.substring(0, TEXT_MAX_LENGTH);
    }

    // ------------------------------------------------------------------ 2. 예산 연결

    private void resolveBudgets() {
        for (Row row : rows) {
            if (row.budgetKey == null) {
                continue;
            }
            Mapping mapping = budgetMappings.get(row.budgetKey);
            if (mapping == null) {
                mapping = new Mapping(row.budgetKey, row.budgetSource, row.type.getLabel());
                resolveBudget(mapping, row);
                budgetMappings.put(row.budgetKey, mapping);
            }
            mapping.count++;
            mapping.totalAmount += row.amount == null ? 0 : row.amount;
        }
    }

    private void resolveBudget(Mapping mapping, Row row) {
        BudgetType budgetType = row.type == FinancialRecordType.INCOME ? BudgetType.INCOME : BudgetType.EXPENSE;

        // 직접 한 매핑은 대상이 아직 쓸 수 있는 예산(같은 구분의 말단 예산)일 때만 따른다.
        BudgetInfo manual = budgetsById.get(request.getBudgetMappings().get(mapping.key));
        if (manual != null && manual.leaf() && manual.type() == budgetType) {
            setTarget(mapping, manual.id(), manual.label(), true);
            return;
        }
        if (row.budgetSource.isEmpty()) {
            mapping.note = "예산이 비어 있습니다.";
            return;
        }

        List<BudgetInfo> sameType = budgetsById.values()
                .stream()
                .filter(budget -> budget.type() == budgetType)
                .toList();
        // 전체 이름이 같은 예산을 먼저 찾고, 없으면 이름(마지막 단계)이 같은 말단 예산을 찾는다.
        List<BudgetInfo> candidates = sameType.stream()
                .filter(budget -> budget.label().equals(row.budgetSource))
                .toList();
        if (candidates.isEmpty() && !row.budgetSource.contains(LABEL_SEPARATOR)) {
            candidates = sameType.stream()
                    .filter(budget -> budget.names().getLast().equals(row.budgetSource))
                    .toList();
        }

        List<BudgetInfo> leaves = candidates.stream()
                .filter(BudgetInfo::leaf)
                .toList();
        if (leaves.size() == 1) {
            setTarget(mapping, leaves.getFirst().id(), leaves.getFirst().label(), false);
        } else if (leaves.size() > 1) {
            mapping.note = "같은 이름의 예산이 여럿 있습니다.";
        } else if (!candidates.isEmpty()) {
            mapping.note = "하위 예산이 있는 예산입니다. 말단 예산을 지정해주세요.";
        } else {
            mapping.note = "등록되지 않은 예산입니다.";
        }
    }

    private void setTarget(Mapping mapping, Long targetId, String targetLabel, boolean manual) {
        mapping.targetId = targetId;
        mapping.targetLabel = targetLabel;
        mapping.manual = manual;
    }

    private static List<String> namesOf(Budget budget) {
        List<String> names = new ArrayList<>();
        Budget current = budget;
        while (current != null) {
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return names;
    }

    // ------------------------------------------------------------------ 3. 관계자 연결

    private void resolveContacts() {
        for (Row row : rows) {
            for (int i = 0; i < row.contactKeys.size(); i++) {
                String key = row.contactKeys.get(i);
                Mapping mapping = contactMappings.get(key);
                if (mapping == null) {
                    boolean blank = row.contactNames.isEmpty();
                    mapping = new Mapping(key, blank ? "" : row.contactNames.get(i), row.type.getLabel());
                    resolveContact(mapping, row.type, blank ? null : row.contactNames.get(i));
                    contactMappings.put(key, mapping);
                }
                mapping.count++;
                mapping.totalAmount += row.amount == null ? 0 : row.amount;
            }
        }
    }

    /**
     * @param name 파일에 적힌 이름. 빈 칸 항목이면 null
     */
    private void resolveContact(Mapping mapping, FinancialRecordType type, String name) {
        List<Contact> manual = manualContactsOf(mapping.key, type);
        if (!manual.isEmpty()) {
            setContactTargets(mapping, manual, true);
            return;
        }
        if (name == null) {
            mapping.note = "빈 칸에 쓸 관계자를 지정해주세요.";
            return;
        }

        List<Contact> sameName = contactsByName.getOrDefault(name, List.of());
        // 동명이인이면 숨김 처리되지 않은 관계자 중 가장 최근에 등록된(ID가 가장 큰) 관계자에 연결한다.
        List<Contact> visible = sameName.stream()
                .filter(contact -> !contact.isHidden())
                .sorted(Comparator.comparing(Contact::getId).reversed())
                .toList();
        if (!visible.isEmpty()) {
            setContactTargets(mapping, List.of(visible.getFirst()), false);
            if (visible.size() > 1) {
                mapping.note = "동명이인 " + visible.size() + "명 중 가장 최근에 등록된 관계자입니다.";
            }
        } else if (!sameName.isEmpty()) {
            mapping.note = "숨김 처리된 관계자만 있습니다.";
        } else {
            mapping.note = "등록되지 않은 이름입니다.";
        }
    }

    /**
     * 직접 한 관계자 매핑의 대상을 찾는다. 대상이 하나라도 삭제되었거나, 지출 항목인데 한 명이 아니면 따르지 않는다.
     *
     * @return 매핑할 관계자들. 직접 한 매핑이 없거나 따를 수 없으면 빈 목록
     */
    private List<Contact> manualContactsOf(String key, FinancialRecordType type) {
        List<Long> contactIds = request.getContactMappings().getOrDefault(key, List.of());
        if (contactIds.isEmpty() || (type == FinancialRecordType.EXPENSE && contactIds.size() != 1)) {
            return List.of();
        }
        List<Contact> contacts = new ArrayList<>();
        for (Long contactId : contactIds) {
            Contact contact = contactsById.get(contactId);
            if (contact == null) {
                return List.of();
            }
            contacts.add(contact);
        }
        return contacts;
    }

    private void setContactTargets(Mapping mapping, List<Contact> contacts, boolean manual) {
        mapping.targetIds = contacts.stream()
                .map(Contact::getId)
                .toList();
        mapping.targetLabel = contacts.stream()
                .map(this::labelOf)
                .collect(Collectors.joining(", "));
        mapping.manual = manual;
    }

    private String labelOf(Contact contact) {
        return "[" + contact.getType().getLabel() + "] " + contact.getName();
    }

    // ------------------------------------------------------------------ 4. 행 판정

    private void judgeRows() {
        for (Row row : rows) {
            Mapping budgetMapping = row.budgetKey == null ? null : budgetMappings.get(row.budgetKey);
            if (budgetMapping != null && budgetMapping.targetId != null) {
                row.budgetId = budgetMapping.targetId;
                row.budgetLabel = budgetMapping.targetLabel;
            } else if (budgetMapping != null) {
                row.fail("예산이 지정되지 않았습니다: " + budgetMapping.label);
            }

            // 서로 다른 이름이 같은 관계자에 매핑되었으면 한 명으로 센다.
            Set<Long> contactIds = new LinkedHashSet<>();
            List<String> contactNames = new ArrayList<>();
            for (String key : row.contactKeys) {
                Mapping contactMapping = contactMappings.get(key);
                if (contactMapping.targetIds.isEmpty()) {
                    row.fail("관계자가 지정되지 않았습니다: " + contactMapping.label);
                    contactNames.add(contactMapping.label);
                    continue;
                }
                for (Long contactId : contactMapping.targetIds) {
                    if (contactIds.add(contactId)) {
                        contactNames.add(contactsById.get(contactId).getName());
                    }
                }
            }
            if (!row.contactKeys.isEmpty()) {
                row.contactLabel = String.join(", ", contactNames);
            }
            if (row.error == null) {
                row.contactIds = List.copyOf(contactIds);
                if (row.type == FinancialRecordType.EXPENSE && contactIds.size() != 1) {
                    row.fail("지출의 수취인은 한 명이어야 합니다.");
                }
            }
        }
    }

    /**
     * 일자, 구분, 예산, 금액, 내용, 관계자가 모두 같은 내역이 이미 있는 행을 중복 의심으로 표시한다.
     * 건수로 비교한다: 같은 내역이 이미 2건 있고 파일에 3건 있으면 앞의 2건만 중복 의심이다.
     * 같은 금액의 무명 헌금처럼, 내용이 같은 내역이 여러 건 있는 것은 정상일 수 있기 때문이다.
     */
    private void markDuplicates() {
        Map<String, Integer> existingCounts = new HashMap<>();
        for (FinancialRecord existing : existingRecords) {
            List<String> contacts = existing.getContacts()
                    .stream()
                    .map(this::contactIdentityOf)
                    .sorted()
                    .toList();
            existingCounts.merge(identityOf(existing.getType(), existing.getDate(), existing.getBudget().getId(),
                    existing.getAmount(), existing.getDescription(), contacts), 1, Integer::sum);
        }

        for (Row row : rows) {
            if (row.error != null) {
                continue;
            }
            List<String> contacts = row.contactIds.stream()
                    .map(String::valueOf)
                    .sorted()
                    .toList();
            String identity = identityOf(row.type, row.date, row.budgetId, row.amount, row.description, contacts);
            int remaining = existingCounts.getOrDefault(identity, 0);
            if (remaining > 0) {
                existingCounts.put(identity, remaining - 1);
                row.duplicate = true;
            }
        }
    }

    /**
     * 내역에 연결된 관계자를 비교할 값. 관계자가 삭제된 내역은 기록된 당시의 이름으로 비교한다.
     */
    private String contactIdentityOf(FinancialRecordContact recordContact) {
        return recordContact.getContact() != null ? String.valueOf(recordContact.getContact().getId())
                : "name:" + recordContact.getContactName();
    }

    private String identityOf(FinancialRecordType type, LocalDate date, Long budgetId, long amount,
            String description, List<String> contacts) {
        return type + "|" + date + "|" + budgetId + "|" + amount + "|" + Objects.toString(description, "") + "|"
                + contacts;
    }

    // ------------------------------------------------------------------ 결과

    private static RecordImportItemDto toDto(Row row) {
        ImportItemStatus status;
        String message;
        if (row.error != null) {
            status = ImportItemStatus.ERROR;
            message = row.error;
        } else if (row.duplicate || !row.warnings.isEmpty()) {
            List<String> messages = new ArrayList<>();
            if (row.duplicate) {
                messages.add("같은 내역이 이미 있습니다. (중복 의심)");
            }
            messages.addAll(row.warnings);
            status = ImportItemStatus.WARNING;
            message = String.join(" ", messages);
        } else {
            status = ImportItemStatus.OK;
            message = "";
        }

        return RecordImportItemDto.builder()
                .key("row:" + row.rowNumber)
                .rowNumber(row.rowNumber)
                .type(row.type)
                .date(row.date)
                .budgetId(row.budgetId)
                .budgetLabel(row.budgetLabel)
                .amount(row.amount)
                .description(row.description)
                .contactIds(row.contactIds)
                .contactLabel(row.contactLabel)
                .remarks(row.remarks)
                .status(status)
                .message(message)
                .duplicate(row.error == null && row.duplicate)
                .build();
    }

    private static List<ImportMappingDto> toDtos(Map<String, Mapping> mappings) {
        return mappings.values()
                .stream()
                .map(mapping -> ImportMappingDto.builder()
                        .key(mapping.key)
                        .label(mapping.label)
                        .blank(mapping.blank)
                        .typeLabel(mapping.typeLabel)
                        .count(mapping.count)
                        .totalAmount(mapping.totalAmount)
                        .targetId(mapping.targetId)
                        .targetIds(mapping.targetIds)
                        .targetType(mapping.targetType)
                        .targetLabel(mapping.targetLabel)
                        .manual(mapping.manual)
                        .note(mapping.note)
                        .build())
                .toList();
    }

    /**
     * 분석 결과 전체를 요약한다. 행의 상태나 내용, 구분·예산·관계자의 연결 중 하나라도 다르면 값이 달라진다.
     */
    private static String sign(List<RecordImportItemDto> items, List<List<ImportMappingDto>> mappingLists) {
        String text = items.stream()
                .map(item -> String.join("|", item.getKey(), String.valueOf(item.getStatus()), item.getMessage(),
                        String.valueOf(item.getType()), String.valueOf(item.getDate()),
                        String.valueOf(item.getBudgetId()), String.valueOf(item.getAmount()),
                        String.valueOf(item.getContactIds()), item.getDescription(), item.getRemarks()))
                .collect(Collectors.joining("\n"))
                + "\n--\n"
                + mappingLists.stream()
                        .flatMap(List::stream)
                        .map(mapping -> mapping.getKey() + "|" + mapping.getTargetId() + "|" + mapping.getTargetIds()
                                + "|" + mapping.getTargetType())
                        .collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

}
