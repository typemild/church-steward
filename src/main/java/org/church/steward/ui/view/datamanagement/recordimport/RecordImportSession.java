package org.church.steward.ui.view.datamanagement.recordimport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.church.steward.domain.datatransfer.dto.ImportMappingDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.view.datamanagement.importing.ImportSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 진행 중인 수입/지출 가져오기 한 번의 상태. 파일과 백업은 {@link ImportSession}이 가지고, 여기서는 열 지정,
 * 사용자가 직접 한 예산·관계자 매핑, 분석 결과, 가져올 행의 선택을 가진다.
 * 분석 결과와 그에 따른 선택은 {@link #applyAnalysis}로 언제든 다시 계산한다.
 */
@Getter
public class RecordImportSession extends ImportSession {

    /** 단계별 예산명으로 지정할 수 있는 열의 최대 수 */
    public static final int MAX_LEVELS = 5;

    private static final Pattern LEVEL_HEADER = Pattern.compile("(\\d+)단계");
    private static final Set<String> TYPE_HEADERS = Set.of("구분", "수입/지출", "종류");
    private static final Set<String> DATE_HEADERS = Set.of("일자", "날짜", "일시", "거래일", "거래일자");
    private static final Set<String> YEAR_HEADERS = Set.of("연", "년", "연도", "년도");
    private static final Set<String> INCOME_AMOUNT_HEADERS = Set.of("수입", "수입금액", "수입액", "입금", "입금액");
    private static final Set<String> EXPENSE_AMOUNT_HEADERS = Set.of("지출", "지출금액", "지출액", "출금", "출금액");
    private static final Set<String> BUDGET_HEADERS = Set.of("예산", "예산명", "분류", "계정", "계정과목", "항목", "과목");
    private static final List<String> CONTACT_HEADER_WORDS = List.of("봉헌자", "수취인", "성명", "이름", "관계자", "거래처",
            "헌금자");

    /**
     * 구분과 금액을 읽는 방식.
     */
    public enum TypeMode {
        /** 파일 전체가 수입 */
        ALL_INCOME,
        /** 파일 전체가 지출 */
        ALL_EXPENSE,
        /** 구분이 적힌 열이 있음 */
        TYPE_COLUMN,
        /** 수입 금액과 지출 금액의 열이 따로 있음 */
        SPLIT_AMOUNTS
    }

    /**
     * 고른 행들의 요약.
     */
    public record SelectionSummary(int incomeCount, long incomeAmount, int expenseCount, long expenseAmount) {
    }

    /**
     * 다시 분석했을 때 이전과 달라진 것.
     *
     * @param changedItemCount     상태나 연결이 달라졌거나 새로 생기거나 없어진 행의 수
     * @param releasedMappingCount 대상을 더는 쓸 수 없어 해제된, 사용자가 직접 한 매핑의 수
     */
    public record AnalysisChange(int changedItemCount, int releasedMappingCount) {
        public boolean hasChanges() {
            return changedItemCount > 0 || releasedMappingCount > 0;
        }
    }

    // ---- 열 지정 (열은 0부터 세는 위치)
    @Setter
    private TypeMode typeMode = TypeMode.ALL_INCOME;
    @Setter
    private Integer typeColumn;
    @Setter
    private Integer amountColumn;
    @Setter
    private Integer incomeAmountColumn;
    @Setter
    private Integer expenseAmountColumn;
    /** 일자를 연/월/일이 따로 적힌 열에서 읽을지 여부. false이면 일자가 한 칸에 적힌 열에서 읽는다. */
    @Setter
    private boolean separateDate;
    @Setter
    private Integer dateColumn;
    @Setter
    private Integer yearColumn;
    @Setter
    private Integer monthColumn;
    @Setter
    private Integer dayColumn;
    /** 예산을 전체 예산명이 적힌 열 하나에서 읽을지 여부. false이면 단계별 열에서 읽는다. */
    @Setter
    private boolean budgetPathMode;
    private List<Integer> budgetLevelColumns = List.of();
    @Setter
    private Integer budgetPathColumn;
    @Setter
    private Integer descriptionColumn;
    @Setter
    private Integer contactColumn;
    @Setter
    private Integer remarksColumn;
    /** 지금의 파일과 머리글 행에 대해 열을 자동으로 추측했는지 여부 */
    private boolean mappingGuessed;

    // ---- 사용자가 직접 한 매핑 (매핑 항목의 식별자 -> 구분, 예산 ID 또는 관계자 ID 목록)
    private final Map<String, FinancialRecordType> typeMappings = new LinkedHashMap<>();
    private final Map<String, Long> budgetMappings = new LinkedHashMap<>();
    private final Map<String, List<Long>> contactMappings = new LinkedHashMap<>();

    // ---- 분석
    private RecordImportAnalysisDto analysis;
    /** 사용자가 직접 바꾼 선택. 행의 식별자 -> 선택 여부 */
    private final Map<String, Boolean> explicitSelections = new HashMap<>();
    private final Set<String> selectedKeys = new LinkedHashSet<>();

    @Override
    public void reset() {
        resetFile();
        resetColumns();
        mappingGuessed = false;
        clearMappings();
        clearAnalysis();
    }

    private void clearMappings() {
        typeMappings.clear();
        budgetMappings.clear();
        contactMappings.clear();
    }

    private void resetColumns() {
        typeMode = TypeMode.ALL_INCOME;
        typeColumn = null;
        amountColumn = null;
        incomeAmountColumn = null;
        expenseAmountColumn = null;
        separateDate = false;
        dateColumn = null;
        yearColumn = null;
        monthColumn = null;
        dayColumn = null;
        budgetPathMode = false;
        budgetLevelColumns = List.of();
        budgetPathColumn = null;
        descriptionColumn = null;
        contactColumn = null;
        remarksColumn = null;
    }

    /**
     * 파일이나 머리글 행이 바뀌었으므로 열 추측과 직접 한 매핑, 분석 결과를 버린다.
     */
    @Override
    protected void onDataChanged() {
        mappingGuessed = false;
        clearMappings();
        clearAnalysis();
    }

    @Override
    public boolean hasAnalysis() {
        return analysis != null;
    }

    @Override
    public void clearAnalysis() {
        analysis = null;
        explicitSelections.clear();
        selectedKeys.clear();
        clearBackup();
    }

    // ------------------------------------------------------------------ 열 지정

    /**
     * 단계별 예산명의 열을 지정한다. 지정하지 않은 단계(null)는 빼고 순서대로 담는다.
     */
    public void setBudgetLevelColumns(List<Integer> budgetLevelColumns) {
        this.budgetLevelColumns = budgetLevelColumns.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 머리글 이름으로 열을 추측해 열 지정을 채운다. 같은 파일과 머리글 행에 대해서는 한 번만 한다.
     * 프로그램이 내보낸 파일과 빈 양식의 머리글은 모두 알아본다.
     */
    public void guessMappingIfNeeded() {
        if (mappingGuessed) {
            return;
        }
        mappingGuessed = true;
        resetColumns();

        Map<Integer, Integer> numberedLevels = new TreeMap<>();
        Integer budgetColumn = null;
        for (int column = 0; column < getColumnCount(); column++) {
            String header = getHeader(column).replace(" ", "");
            Matcher levelMatcher = LEVEL_HEADER.matcher(header);
            if (header.isEmpty()) {
                continue;
            }
            if (levelMatcher.find()) {
                numberedLevels.putIfAbsent(Integer.parseInt(levelMatcher.group(1)), column);
            } else if (TYPE_HEADERS.contains(header)) {
                typeColumn = firstOf(typeColumn, column);
            } else if (DATE_HEADERS.contains(header)) {
                dateColumn = firstOf(dateColumn, column);
            } else if (YEAR_HEADERS.contains(header)) {
                yearColumn = firstOf(yearColumn, column);
            } else if (header.equals("월")) {
                monthColumn = firstOf(monthColumn, column);
            } else if (header.equals("일")) {
                dayColumn = firstOf(dayColumn, column);
            } else if (INCOME_AMOUNT_HEADERS.contains(header)) {
                incomeAmountColumn = firstOf(incomeAmountColumn, column);
            } else if (EXPENSE_AMOUNT_HEADERS.contains(header)) {
                expenseAmountColumn = firstOf(expenseAmountColumn, column);
            } else if (header.contains("금액")) {
                amountColumn = firstOf(amountColumn, column);
            } else if (header.contains("내용") || header.contains("적요")) {
                descriptionColumn = firstOf(descriptionColumn, column);
            } else if (CONTACT_HEADER_WORDS.stream().anyMatch(header::contains)) {
                contactColumn = firstOf(contactColumn, column);
            } else if (header.contains("비고") || header.contains("메모")) {
                remarksColumn = firstOf(remarksColumn, column);
            } else if (header.contains("전체예산")) {
                budgetPathColumn = firstOf(budgetPathColumn, column);
            } else if (BUDGET_HEADERS.contains(header)) {
                budgetColumn = firstOf(budgetColumn, column);
            }
        }

        if (incomeAmountColumn != null && expenseAmountColumn != null) {
            typeMode = TypeMode.SPLIT_AMOUNTS;
        } else if (typeColumn != null) {
            typeMode = TypeMode.TYPE_COLUMN;
        }
        separateDate = dateColumn == null && monthColumn != null && dayColumn != null;

        List<Integer> levels = new ArrayList<>(numberedLevels.values());
        if (levels.isEmpty() && budgetColumn != null) {
            levels.add(budgetColumn);
        }
        budgetLevelColumns = levels.stream()
                .limit(MAX_LEVELS)
                .toList();
        budgetPathMode = budgetLevelColumns.isEmpty() && budgetPathColumn != null;
    }

    private Integer firstOf(Integer current, int column) {
        return current != null ? current : Integer.valueOf(column);
    }

    /**
     * 열 지정에 빠진 것이 있는지 확인한다.
     *
     * @return 고쳐야 할 내용. 문제가 없으면 null
     */
    public String validateMapping() {
        if (typeMode == TypeMode.TYPE_COLUMN && typeColumn == null) {
            return "구분이 있는 열을 지정해주세요.";
        }
        if (typeMode == TypeMode.SPLIT_AMOUNTS && (incomeAmountColumn == null || expenseAmountColumn == null)) {
            return "수입 금액과 지출 금액이 있는 열을 지정해주세요.";
        }
        if (typeMode != TypeMode.SPLIT_AMOUNTS && amountColumn == null) {
            return "금액이 있는 열을 지정해주세요.";
        }
        if (separateDate && (monthColumn == null || dayColumn == null)) {
            return "월과 일이 있는 열을 지정해주세요.";
        }
        if (!separateDate && dateColumn == null) {
            return "일자가 있는 열을 지정해주세요.";
        }
        if (budgetPathMode && budgetPathColumn == null) {
            return "전체 예산명이 있는 열을 지정해주세요.";
        }
        if (!budgetPathMode && budgetLevelColumns.isEmpty()) {
            return "예산이 있는 열을 하나 이상 지정해주세요.";
        }
        if (!budgetPathMode && new HashSet<>(budgetLevelColumns).size() != budgetLevelColumns.size()) {
            return "같은 열을 여러 단계에 지정할 수 없습니다.";
        }
        return null;
    }

    /**
     * 분석과 반영에 넘길 입력을 만든다. 머리글 아래의 행과, 지금 고른 방식에 해당하는 열만 넘긴다.
     */
    public RecordImportRequestDto toRequest() {
        boolean split = typeMode == TypeMode.SPLIT_AMOUNTS;
        FinancialRecordType fixedType = switch (typeMode) {
        case ALL_INCOME -> FinancialRecordType.INCOME;
        case ALL_EXPENSE -> FinancialRecordType.EXPENSE;
        default -> null;
        };

        return RecordImportRequestDto.builder()
                .rows(getDataRows())
                .firstRowNumber(getFirstDataRowNumber())
                .fixedType(fixedType)
                .typeColumn(typeMode == TypeMode.TYPE_COLUMN ? typeColumn : null)
                .amountColumn(split ? null : amountColumn)
                .incomeAmountColumn(split ? incomeAmountColumn : null)
                .expenseAmountColumn(split ? expenseAmountColumn : null)
                .dateColumn(separateDate ? null : dateColumn)
                .yearColumn(separateDate ? yearColumn : null)
                .monthColumn(separateDate ? monthColumn : null)
                .dayColumn(separateDate ? dayColumn : null)
                .budgetLevelColumns(budgetPathMode ? List.of() : budgetLevelColumns)
                .budgetPathColumn(budgetPathMode ? budgetPathColumn : null)
                .descriptionColumn(descriptionColumn)
                .contactColumn(contactColumn)
                .remarksColumn(remarksColumn)
                .typeMappings(Map.copyOf(typeMappings))
                .budgetMappings(Map.copyOf(budgetMappings))
                .contactMappings(Map.copyOf(contactMappings))
                .build();
    }

    // ------------------------------------------------------------------ 매핑

    /**
     * 구분 열의 값들을 수입 또는 지출로 직접 매핑한다. 다시 분석해야 행에 적용된다.
     * 구분이 정해져야 그 행의 예산과 관계자를 연결할 수 있으므로, 다시 분석하면 예산과 관계자의 매핑 항목이 늘 수 있다.
     *
     * @param keys 매핑 항목의 식별자들 (구분 열에 적힌 값)
     * @param type 읽을 구분. null이면 직접 한 매핑을 해제한다.
     */
    public void setTypeMapping(Collection<String> keys, FinancialRecordType type) {
        for (String key : keys) {
            if (type == null) {
                typeMappings.remove(key);
            } else {
                typeMappings.put(key, type);
            }
        }
    }

    /**
     * 파일의 예산 값들을 예산 하나에 직접 매핑한다. 다시 분석해야 행에 적용된다.
     *
     * @param keys     매핑 항목의 식별자들
     * @param budgetId 매핑할 예산의 ID. null이면 직접 한 매핑을 해제한다.
     */
    public void setBudgetMapping(Collection<String> keys, Long budgetId) {
        setMapping(budgetMappings, keys, budgetId);
    }

    /**
     * 파일의 관계자 이름들(또는 빈 칸)을 관계자들에 직접 매핑한다. 다시 분석해야 행에 적용된다.
     * 수입 항목은 여러 명에 매핑할 수 있고, 지출 항목은 한 명이어야 한다(아니면 다시 분석할 때 해제된다).
     *
     * @param keys       매핑 항목의 식별자들
     * @param contactIds 매핑할 관계자들의 ID. 비어 있으면 직접 한 매핑을 해제한다.
     */
    public void setContactMapping(Collection<String> keys, List<Long> contactIds) {
        setMapping(contactMappings, keys, contactIds.isEmpty() ? null : List.copyOf(contactIds));
    }

    private <T> void setMapping(Map<String, T> mappings, Collection<String> keys, T targetId) {
        for (String key : keys) {
            if (targetId == null) {
                mappings.remove(key);
            } else {
                mappings.put(key, targetId);
            }
        }
    }

    /**
     * 연결되지 않은 구분 값, 예산 값, 관계자 이름의 수.
     */
    public int countUnmapped() {
        if (analysis == null) {
            return 0;
        }
        return (int) Stream.of(analysis.getTypeMappings(), analysis.getBudgetMappings(), analysis.getContactMappings())
                .flatMap(List::stream)
                .filter(mapping -> !mapping.isMapped())
                .count();
    }

    // ------------------------------------------------------------------ 분석과 선택

    /**
     * 새 분석 결과를 받아들이고 선택을 다시 계산한다.
     * 사용자가 직접 바꾼 선택은 유지하고, 손대지 않은 행은 가져올 수 있고 중복 의심이 아니면 선택한다.
     * 직접 한 매핑 중 서비스가 따르지 않은 것(대상이 삭제되었거나 쓸 수 없게 된 것)은 해제한다.
     *
     * @return 이전 분석 결과와 비교해 달라진 것. 이전 분석 결과가 없었으면 달라진 행은 0으로 본다.
     */
    public AnalysisChange applyAnalysis(RecordImportAnalysisDto newAnalysis) {
        int changedItemCount = analysis == null ? 0 : countChangedItems(analysis, newAnalysis);
        // 구분 매핑은 대상이 없어지는 일이 없으므로, 파일에 더는 나오지 않는 값의 매핑만 지운다.
        Set<String> typeKeys = new HashSet<>();
        for (ImportMappingDto mapping : newAnalysis.getTypeMappings()) {
            typeKeys.add(mapping.getKey());
        }
        typeMappings.keySet().retainAll(typeKeys);
        int releasedMappingCount = releaseIgnoredMappings(budgetMappings, newAnalysis.getBudgetMappings(),
                ImportMappingDto::getTargetId)
                + releaseIgnoredMappings(contactMappings, newAnalysis.getContactMappings(),
                        ImportMappingDto::getTargetIds);
        analysis = newAnalysis;
        recomputeSelection();
        return new AnalysisChange(changedItemCount, releasedMappingCount);
    }

    private int countChangedItems(RecordImportAnalysisDto oldAnalysis, RecordImportAnalysisDto newAnalysis) {
        Map<String, String> oldStates = new HashMap<>();
        for (RecordImportItemDto item : oldAnalysis.getItems()) {
            oldStates.put(item.getKey(), stateOf(item));
        }

        int changedCount = 0;
        for (RecordImportItemDto item : newAnalysis.getItems()) {
            String oldState = oldStates.remove(item.getKey());
            if (!stateOf(item).equals(oldState)) {
                changedCount++;
            }
        }
        // 새 분석 결과에 없는 행
        return changedCount + oldStates.size();
    }

    private String stateOf(RecordImportItemDto item) {
        return item.getStatus() + "|" + item.getMessage() + "|" + item.getBudgetId() + "|" + item.getContactIds();
    }

    /**
     * 직접 한 매핑 중에서, 분석 결과에 직접 한 매핑으로 반영되지 않은 것을 지운다.
     *
     * @param targetOf 분석 결과의 매핑 항목에서 직접 한 매핑과 비교할 대상(예산 ID 또는 관계자 ID 목록)을 꺼내는 함수
     * @return 대상을 쓸 수 없어 해제된 매핑의 수. 파일에 더는 나오지 않는 값의 매핑은 세지 않고 지우기만 한다.
     */
    private <T> int releaseIgnoredMappings(Map<String, T> mappings, List<ImportMappingDto> analyzedMappings,
            Function<ImportMappingDto, T> targetOf) {
        Map<String, ImportMappingDto> analyzed = new HashMap<>();
        for (ImportMappingDto mapping : analyzedMappings) {
            analyzed.put(mapping.getKey(), mapping);
        }

        int releasedCount = 0;
        Iterator<Map.Entry<String, T>> iterator = mappings.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, T> entry = iterator.next();
            ImportMappingDto mapping = analyzed.get(entry.getKey());
            if (mapping == null) {
                iterator.remove();
            } else if (!mapping.isManual() || !entry.getValue().equals(targetOf.apply(mapping))) {
                iterator.remove();
                releasedCount++;
            }
        }
        return releasedCount;
    }

    public void setSelected(String key, boolean selected) {
        explicitSelections.put(key, selected);
        recomputeSelection();
    }

    /**
     * 가져올 수 있는 행을 모두 고르거나 모두 뺀다.
     */
    public void selectAll(boolean selected) {
        for (RecordImportItemDto item : analysis.getItems()) {
            if (item.isSelectable()) {
                explicitSelections.put(item.getKey(), selected);
            }
        }
        recomputeSelection();
    }

    /**
     * 직접 바꾼 선택을 모두 버리고 처음 분석했을 때의 선택으로 되돌린다. 오류 행과 중복 의심 행을 뺀 행이 선택된다.
     */
    public void resetSelection() {
        explicitSelections.clear();
        recomputeSelection();
    }

    public boolean isSelected(String key) {
        return selectedKeys.contains(key);
    }

    /**
     * 분석 결과와 사용자가 직접 바꾼 선택으로 선택된 행을 다시 정한다.
     * 가져올 수 없는 행은 빼고, 손대지 않은 행은 중복 의심이 아닐 때만 선택한다.
     */
    private void recomputeSelection() {
        selectedKeys.clear();
        Set<String> keys = new HashSet<>();
        for (RecordImportItemDto item : analysis.getItems()) {
            keys.add(item.getKey());
            if (item.isSelectable() && explicitSelections.getOrDefault(item.getKey(), !item.isDuplicate())) {
                selectedKeys.add(item.getKey());
            }
        }
        explicitSelections.keySet().retainAll(keys);
    }

    public SelectionSummary summarize() {
        int incomeCount = 0;
        long incomeAmount = 0;
        int expenseCount = 0;
        long expenseAmount = 0;
        for (RecordImportItemDto item : analysis.getItems()) {
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            if (item.getType() == FinancialRecordType.INCOME) {
                incomeCount++;
                incomeAmount += item.getAmount();
            } else {
                expenseCount++;
                expenseAmount += item.getAmount();
            }
        }
        return new SelectionSummary(incomeCount, incomeAmount, expenseCount, expenseAmount);
    }

}
