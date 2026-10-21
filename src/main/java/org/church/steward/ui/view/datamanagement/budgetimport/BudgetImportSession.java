package org.church.steward.ui.view.datamanagement.budgetimport;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.ui.view.datamanagement.importing.ImportSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 진행 중인 예산 가져오기 한 번의 상태. 파일과 백업은 {@link ImportSession}이 가지고, 여기서는 열 지정과
 * 분석 결과, 가져올 건의 선택을 가진다. 분석 결과와 그에 따른 선택은 {@link #applyAnalysis}로 언제든 다시 계산한다.
 */
@Getter
public class BudgetImportSession extends ImportSession {

    /** 단계별 예산명으로 지정할 수 있는 열의 최대 수 */
    public static final int MAX_LEVELS = 5;

    private static final Pattern LEVEL_HEADER = Pattern.compile("(\\d+)\\s*단계");
    /** "관/항/목"처럼 숫자 없이 단계를 나타내는 머리글. 앞에 있을수록 상위 단계다. */
    private static final List<String> NAMED_LEVEL_HEADERS = List.of("관", "항", "목", "세목");

    /**
     * 고른 건들의 요약.
     *
     * @param createCount 추가할 예산의 수
     * @param updateCount 금액을 갱신할 예산의 수
     * @param totalAmount 고른 말단 예산의 금액 합계
     */
    public record SelectionSummary(int createCount, int updateCount, long totalAmount) {
    }

    // ---- 열 지정 (열은 0부터 세는 위치)
    /** 파일 전체의 구분. 구분을 열에서 읽으면 null */
    @Setter
    private BudgetType fixedType = BudgetType.INCOME;
    @Setter
    private Integer typeColumn;
    /** 예산명을 전체 경로가 적힌 열 하나에서 읽을지 여부. false이면 단계별 열에서 읽는다. */
    @Setter
    private boolean pathMode;
    private List<Integer> levelColumns = List.of();
    @Setter
    private Integer pathColumn;
    @Setter
    private Integer amountColumn;
    @Setter
    private boolean fillDown;
    @Setter
    private boolean updateExistingAmounts;
    /** 지금의 파일과 머리글 행에 대해 열을 자동으로 추측했는지 여부 */
    private boolean mappingGuessed;

    // ---- 분석
    private BudgetImportAnalysisDto analysis;
    /** 사용자가 직접 바꾼 선택. 건의 식별자 -> 선택 여부 */
    private final Map<String, Boolean> explicitSelections = new HashMap<>();
    private final Set<String> selectedKeys = new LinkedHashSet<>();

    @Override
    public void reset() {
        resetFile();
        fixedType = BudgetType.INCOME;
        typeColumn = null;
        pathMode = false;
        levelColumns = List.of();
        pathColumn = null;
        amountColumn = null;
        fillDown = false;
        updateExistingAmounts = false;
        mappingGuessed = false;
        clearAnalysis();
    }

    /**
     * 파일이나 머리글 행이 바뀌었으므로 열 추측과 분석 결과를 버린다.
     */
    @Override
    protected void onDataChanged() {
        mappingGuessed = false;
        clearAnalysis();
    }

    @Override
    public boolean hasAnalysis() {
        return analysis != null;
    }

    // ------------------------------------------------------------------ 열 지정

    /**
     * 단계별 예산명의 열을 지정한다. 지정하지 않은 단계(null)는 빼고 순서대로 담는다.
     */
    public void setLevelColumns(List<Integer> levelColumns) {
        this.levelColumns = levelColumns.stream()
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 머리글 이름으로 열을 추측해 열 지정을 채운다. 같은 파일과 머리글 행에 대해서는 한 번만 한다.
     * 프로그램이 내보낸 파일과 빈 양식의 머리글("구분", "예산 1단계", "예산액")은 모두 알아본다.
     */
    public void guessMappingIfNeeded() {
        if (mappingGuessed) {
            return;
        }
        mappingGuessed = true;

        typeColumn = null;
        pathColumn = null;
        amountColumn = null;
        Map<Integer, Integer> numberedLevels = new TreeMap<>();
        Map<Integer, Integer> namedLevels = new TreeMap<>();
        Integer nameColumn = null;

        for (int column = 0; column < getColumnCount(); column++) {
            String header = getHeader(column).replace(" ", "");
            Matcher levelMatcher = LEVEL_HEADER.matcher(header);
            if (header.isEmpty()) {
                continue;
            }
            if (levelMatcher.find()) {
                numberedLevels.putIfAbsent(Integer.parseInt(levelMatcher.group(1)), column);
            } else if (NAMED_LEVEL_HEADERS.contains(header)) {
                namedLevels.putIfAbsent(NAMED_LEVEL_HEADERS.indexOf(header), column);
            } else if (header.contains("구분") && typeColumn == null) {
                typeColumn = column;
            } else if (header.equals("예산액") || (header.contains("금액") && amountColumn == null)) {
                amountColumn = column;
            } else if ((header.contains("예산명") || header.contains("항목") || header.contains("과목"))
                    && nameColumn == null) {
                nameColumn = column;
            }
        }

        List<Integer> guessedLevels = new ArrayList<>(
                numberedLevels.isEmpty() ? namedLevels.values() : numberedLevels.values());
        if (guessedLevels.isEmpty() && nameColumn != null) {
            guessedLevels.add(nameColumn);
        }
        levelColumns = guessedLevels.stream()
                .limit(MAX_LEVELS)
                .toList();
        pathMode = false;
        fixedType = typeColumn == null ? BudgetType.INCOME : null;
    }

    /**
     * 열 지정에 빠진 것이 있는지 확인한다.
     *
     * @return 고쳐야 할 내용. 문제가 없으면 null
     */
    public String validateMapping() {
        if (fixedType == null && typeColumn == null) {
            return "구분이 있는 열을 지정해주세요.";
        }
        if (pathMode && pathColumn == null) {
            return "전체 예산명이 있는 열을 지정해주세요.";
        }
        if (!pathMode && levelColumns.isEmpty()) {
            return "예산명이 있는 열을 하나 이상 지정해주세요.";
        }
        if (!pathMode && new HashSet<>(levelColumns).size() != levelColumns.size()) {
            return "같은 열을 여러 단계에 지정할 수 없습니다.";
        }
        if (amountColumn == null) {
            return "금액이 있는 열을 지정해주세요.";
        }
        return null;
    }

    /**
     * 분석과 반영에 넘길 입력을 만든다. 머리글 아래의 행만 넘긴다.
     */
    public BudgetImportRequestDto toRequest() {
        return BudgetImportRequestDto.builder()
                .rows(getDataRows())
                .firstRowNumber(getFirstDataRowNumber())
                .fixedType(fixedType)
                .typeColumn(fixedType == null ? typeColumn : null)
                .levelColumns(pathMode ? List.of() : levelColumns)
                .pathColumn(pathMode ? pathColumn : null)
                .amountColumn(amountColumn)
                .fillDown(fillDown)
                .updateExistingAmounts(updateExistingAmounts)
                .build();
    }

    // ------------------------------------------------------------------ 분석과 선택

    /**
     * 새 분석 결과를 받아들이고 선택을 다시 계산한다.
     * 사용자가 직접 바꾼 선택은 유지하고, 손대지 않은 건은 고를 수 있으면 선택한다.
     * 분석 결과가 달라졌을 수 있으므로 받아 둔 백업은 다시 확인받도록 서비스에 맡긴다(여기서는 지우지 않는다).
     *
     * @return 이전 분석 결과와 비교해 상태, 할 일, 금액, 설명이 달라졌거나 새로 생기거나 없어진 건의 수.
     *         이전 분석 결과가 없었으면 0
     */
    public int applyAnalysis(BudgetImportAnalysisDto newAnalysis) {
        int changedCount = analysis == null ? 0 : countChanges(analysis, newAnalysis);
        analysis = newAnalysis;
        recomputeSelection();
        return changedCount;
    }

    private int countChanges(BudgetImportAnalysisDto oldAnalysis, BudgetImportAnalysisDto newAnalysis) {
        Map<String, String> oldStates = new HashMap<>();
        for (BudgetImportItemDto item : oldAnalysis.getItems()) {
            oldStates.put(item.getKey(), stateOf(item));
        }

        int changedCount = 0;
        for (BudgetImportItemDto item : newAnalysis.getItems()) {
            String oldState = oldStates.remove(item.getKey());
            if (!stateOf(item).equals(oldState)) {
                changedCount++;
            }
        }
        // 새 분석 결과에 없는 건
        return changedCount + oldStates.size();
    }

    private String stateOf(BudgetImportItemDto item) {
        return item.getStatus() + "|" + item.getAction() + "|" + item.getAmount() + "|" + item.getMessage();
    }

    /**
     * 한 건의 선택을 바꾼다. 하위 예산을 고르면 새로 추가되는 상위 예산도 함께 고르고,
     * 상위 예산을 빼면 그 하위 예산도 함께 뺀다.
     */
    public void setSelected(String key, boolean selected) {
        Map<String, BudgetImportItemDto> items = itemsByKey();
        if (selected) {
            BudgetImportItemDto item = items.get(key);
            while (item != null) {
                if (item.isSelectable()) {
                    explicitSelections.put(item.getKey(), true);
                }
                item = item.getParentKey() == null ? null : items.get(item.getParentKey());
            }
        } else {
            for (BudgetImportItemDto item : analysis.getItems()) {
                if (isSameOrDescendant(item, key, items)) {
                    explicitSelections.put(item.getKey(), false);
                }
            }
        }
        recomputeSelection();
    }

    /**
     * 고를 수 있는 건을 모두 고르거나 모두 뺀다.
     */
    public void selectAll(boolean selected) {
        for (BudgetImportItemDto item : analysis.getItems()) {
            if (item.isSelectable()) {
                explicitSelections.put(item.getKey(), selected);
            }
        }
        recomputeSelection();
    }

    /**
     * 직접 바꾼 선택을 모두 버리고 처음 분석했을 때의 선택으로 되돌린다. 고를 수 있는 건이 모두 선택된다.
     */
    public void resetSelection() {
        explicitSelections.clear();
        recomputeSelection();
    }

    public boolean isSelected(String key) {
        return selectedKeys.contains(key);
    }

    private boolean isSameOrDescendant(BudgetImportItemDto item, String ancestorKey,
            Map<String, BudgetImportItemDto> items) {
        BudgetImportItemDto current = item;
        while (current != null) {
            if (current.getKey().equals(ancestorKey)) {
                return true;
            }
            current = current.getParentKey() == null ? null : items.get(current.getParentKey());
        }
        return false;
    }

    private Map<String, BudgetImportItemDto> itemsByKey() {
        Map<String, BudgetImportItemDto> items = new HashMap<>();
        for (BudgetImportItemDto item : analysis.getItems()) {
            items.put(item.getKey(), item);
        }
        return items;
    }

    /**
     * 분석 결과와 사용자가 직접 바꾼 선택으로 선택된 건을 다시 정한다.
     * 고를 수 없는 건은 빼고, 새로 추가되는 상위 예산이 빠져 있으면 그 하위 예산도 뺀다.
     */
    private void recomputeSelection() {
        selectedKeys.clear();
        Map<String, BudgetImportItemDto> items = itemsByKey();
        explicitSelections.keySet().retainAll(items.keySet());

        // 분석 결과는 상위 예산이 먼저 오므로, 상위 예산의 선택이 정해진 뒤에 하위 예산을 본다.
        for (BudgetImportItemDto item : analysis.getItems()) {
            if (!item.isSelectable() || !explicitSelections.getOrDefault(item.getKey(), true)) {
                continue;
            }
            BudgetImportItemDto parent = item.getParentKey() == null ? null : items.get(item.getParentKey());
            boolean parentMissing = parent != null && parent.getAction() == BudgetImportAction.CREATE
                    && !selectedKeys.contains(parent.getKey());
            if (!parentMissing) {
                selectedKeys.add(item.getKey());
            }
        }
    }

    public SelectionSummary summarize() {
        int createCount = 0;
        int updateCount = 0;
        long totalAmount = 0;
        for (BudgetImportItemDto item : analysis.getItems()) {
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            if (item.getAction() == BudgetImportAction.CREATE) {
                createCount++;
            } else {
                updateCount++;
            }
            if (item.isLeaf()) {
                totalAmount += item.getAmount();
            }
        }
        return new SelectionSummary(createCount, updateCount, totalAmount);
    }

    @Override
    public void clearAnalysis() {
        analysis = null;
        explicitSelections.clear();
        selectedKeys.clear();
        clearBackup();
    }

}
