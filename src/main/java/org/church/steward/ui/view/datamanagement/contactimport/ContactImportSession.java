package org.church.steward.ui.view.datamanagement.contactimport;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.ui.view.datamanagement.importing.ImportSession;

import lombok.Getter;
import lombok.Setter;

/**
 * 진행 중인 관계자 가져오기 한 번의 상태. 파일과 백업은 {@link ImportSession}이 가지고, 여기서는 열 지정과 분석 결과,
 * 가져올 행의 선택을 가진다. 분석 결과와 그에 따른 선택은 {@link #applyAnalysis}로 언제든 다시 계산한다. 관계자는 회계
 * 연도에 속하지 않으므로 회계 연도는 늘 null이다.
 */
@Getter
public class ContactImportSession extends ImportSession {

    private static final Set<String> TYPE_HEADERS = Set.of("구분", "종류", "분류");
    private static final List<String> NAME_HEADER_WORDS = List.of("이름", "성명", "관계자", "봉헌자", "수취인", "거래처",
            "헌금자");
    private static final Set<String> HIDDEN_HEADERS = Set.of("상태", "숨김", "숨김여부");

    /**
     * 고른 행들의 요약.
     *
     * @param count       고른 관계자의 수
     * @param hiddenCount 그중 숨김으로 가져올 관계자의 수
     */
    public record SelectionSummary(int count, int hiddenCount) {
    }

    // ---- 열 지정 (열은 0부터 세는 위치)
    /** 파일 전체의 구분. 구분을 열에서 읽으면 null */
    @Setter
    private ContactType fixedType = ContactType.INDIVIDUAL;
    @Setter
    private Integer typeColumn;
    @Setter
    private Integer nameColumn;
    @Setter
    private Integer remarksColumn;
    @Setter
    private Integer hiddenColumn;
    /** 지금의 파일과 머리글 행에 대해 열을 자동으로 추측했는지 여부 */
    private boolean mappingGuessed;

    // ---- 분석
    private ContactImportAnalysisDto analysis;
    /** 사용자가 직접 바꾼 선택. 행의 식별자 -> 선택 여부 */
    private final Map<String, Boolean> explicitSelections = new HashMap<>();
    private final Set<String> selectedKeys = new LinkedHashSet<>();

    @Override
    public void reset() {
        resetFile();
        resetColumns();
        mappingGuessed = false;
        clearAnalysis();
    }

    private void resetColumns() {
        fixedType = ContactType.INDIVIDUAL;
        typeColumn = null;
        nameColumn = null;
        remarksColumn = null;
        hiddenColumn = null;
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

    @Override
    public void clearAnalysis() {
        analysis = null;
        explicitSelections.clear();
        selectedKeys.clear();
        clearBackup();
    }

    // ------------------------------------------------------------------ 열 지정

    /**
     * 머리글 이름으로 열을 추측해 열 지정을 채운다. 같은 파일과 머리글 행에 대해서는 한 번만 한다. 프로그램이 내보낸 파일과 빈 양식의
     * 머리글("구분", "이름", "비고", "상태")은 모두 알아본다. 구분 열이 없으면 파일 전체를 개인으로 읽게 둔다.
     */
    public void guessMappingIfNeeded() {
        if (mappingGuessed) {
            return;
        }
        mappingGuessed = true;
        resetColumns();

        for (int column = 0; column < getColumnCount(); column++) {
            String header = getHeader(column).replace(" ", "");
            if (header.isEmpty()) {
                continue;
            }
            // "관계자구분"처럼 이름을 뜻하는 낱말이 함께 들어 있을 수 있으므로 구분을 먼저 본다.
            if (TYPE_HEADERS.stream().anyMatch(header::contains)) {
                typeColumn = firstOf(typeColumn, column);
            } else if (HIDDEN_HEADERS.contains(header)) {
                hiddenColumn = firstOf(hiddenColumn, column);
            } else if (header.contains("비고") || header.contains("메모")) {
                remarksColumn = firstOf(remarksColumn, column);
            } else if (NAME_HEADER_WORDS.stream().anyMatch(header::contains)) {
                nameColumn = firstOf(nameColumn, column);
            }
        }
        fixedType = typeColumn == null ? ContactType.INDIVIDUAL : null;
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
        if (fixedType == null && typeColumn == null) {
            return "구분이 있는 열을 지정해주세요.";
        }
        if (nameColumn == null) {
            return "이름이 있는 열을 지정해주세요.";
        }
        return null;
    }

    /**
     * 분석과 반영에 넘길 입력을 만든다. 머리글 아래의 행만 넘긴다.
     */
    public ContactImportRequestDto toRequest() {
        return ContactImportRequestDto.builder()
                .rows(getDataRows())
                .firstRowNumber(getFirstDataRowNumber())
                .fixedType(fixedType)
                .typeColumn(fixedType == null ? typeColumn : null)
                .nameColumn(nameColumn)
                .remarksColumn(remarksColumn)
                .hiddenColumn(hiddenColumn)
                .build();
    }

    // ------------------------------------------------------------------ 분석과 선택

    /**
     * 새 분석 결과를 받아들이고 선택을 다시 계산한다. 사용자가 직접 바꾼 선택은 유지하고, 손대지 않은 행은 가져올 수 있고 중복 의심이
     * 아니면 선택한다.
     *
     * @return 이전 분석 결과와 비교해 상태나 설명이 달라졌거나 새로 생기거나 없어진 행의 수. 이전 분석 결과가 없었으면 0
     */
    public int applyAnalysis(ContactImportAnalysisDto newAnalysis) {
        int changedCount = analysis == null ? 0 : countChanges(analysis, newAnalysis);
        analysis = newAnalysis;
        recomputeSelection();
        return changedCount;
    }

    private int countChanges(ContactImportAnalysisDto oldAnalysis, ContactImportAnalysisDto newAnalysis) {
        Map<String, String> oldStates = new HashMap<>();
        for (ContactImportItemDto item : oldAnalysis.getItems()) {
            oldStates.put(item.getKey(), stateOf(item));
        }

        int changedCount = 0;
        for (ContactImportItemDto item : newAnalysis.getItems()) {
            String oldState = oldStates.remove(item.getKey());
            if (!stateOf(item).equals(oldState)) {
                changedCount++;
            }
        }
        // 새 분석 결과에 없는 행
        return changedCount + oldStates.size();
    }

    private String stateOf(ContactImportItemDto item) {
        return item.getStatus() + "|" + item.getMessage();
    }

    public void setSelected(String key, boolean selected) {
        explicitSelections.put(key, selected);
        recomputeSelection();
    }

    /**
     * 가져올 수 있는 행을 모두 고르거나 모두 뺀다.
     */
    public void selectAll(boolean selected) {
        for (ContactImportItemDto item : analysis.getItems()) {
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
     * 분석 결과와 사용자가 직접 바꾼 선택으로 선택된 행을 다시 정한다. 가져올 수 없는 행은 빼고, 손대지 않은 행은 중복 의심이 아닐 때만
     * 선택한다.
     */
    private void recomputeSelection() {
        selectedKeys.clear();
        Set<String> keys = new HashSet<>();
        for (ContactImportItemDto item : analysis.getItems()) {
            keys.add(item.getKey());
            if (item.isSelectable() && explicitSelections.getOrDefault(item.getKey(), !item.isDuplicate())) {
                selectedKeys.add(item.getKey());
            }
        }
        explicitSelections.keySet().retainAll(keys);
    }

    public SelectionSummary summarize() {
        int count = 0;
        int hiddenCount = 0;
        for (ContactImportItemDto item : analysis.getItems()) {
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            count++;
            if (item.isHidden()) {
                hiddenCount++;
            }
        }
        return new SelectionSummary(count, hiddenCount);
    }

}
