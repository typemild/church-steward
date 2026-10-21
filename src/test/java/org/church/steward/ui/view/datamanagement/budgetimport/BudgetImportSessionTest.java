package org.church.steward.ui.view.datamanagement.budgetimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.ui.view.datamanagement.budgetimport.BudgetImportSession.SelectionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class BudgetImportSessionTest {

    private static final String PARENT = "INCOME:헌금";
    private static final String CHILD = "INCOME:헌금 > 십일조";
    private static final String OTHER = "INCOME:기타";

    /**
     * 프로그램이 내보낸 예산 파일과 같은 머리글을 가진 파일을 읽어 둔 세션을 만든다.
     */
    private BudgetImportSession createSessionWithExportedFile() {
        BudgetImportSession session = new BudgetImportSession();
        session.setData(Path.of("예산.xlsx"), null, true, List.of("예산"), "예산", null,
                new SpreadsheetData(List.of(
                        Arrays.asList("구분", "예산 1단계", "예산 2단계", "예산액", "수입액/집행액"),
                        Arrays.asList("수입", "헌금", null, 1300L, 0L),
                        Arrays.asList("수입", "헌금", "십일조", 1000L, 0L)), null));
        return session;
    }

    private BudgetImportSession createSessionWithHeaders(String... headers) {
        BudgetImportSession session = new BudgetImportSession();
        session.setData(Path.of("예산.xlsx"), null, true, List.of("Sheet1"), "Sheet1", null,
                new SpreadsheetData(List.of(Arrays.asList((Object[]) headers)), null));
        return session;
    }

    private BudgetImportItemDto item(String key, String parentKey, boolean leaf, long amount, ImportItemStatus status,
            BudgetImportAction action) {
        return BudgetImportItemDto.builder()
                .key(key)
                .parentKey(parentKey)
                .type(BudgetType.INCOME)
                .levelNames(List.of(key))
                .leaf(leaf)
                .amount(amount)
                .status(status)
                .action(action)
                .message("")
                .build();
    }

    private BudgetImportAnalysisDto analysisOf(BudgetImportItemDto... items) {
        return BudgetImportAnalysisDto.builder()
                .items(List.of(items))
                .signature("signature")
                .build();
    }

    /**
     * 새로 추가되는 "헌금"과 그 아래의 "십일조"(1,000원), 따로 추가되는 "기타"(500원).
     */
    private BudgetImportAnalysisDto createNewHierarchyAnalysis() {
        return analysisOf(
                item(PARENT, null, false, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                item(CHILD, PARENT, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                item(OTHER, null, true, 500L, ImportItemStatus.OK, BudgetImportAction.CREATE));
    }

    @Nested
    @DisplayName("toColumnLetter 테스트")
    class ToColumnLetterTest {

        @Test
        @DisplayName("열 위치를 엑셀의 열 문자로 바꾼다 (0은 A, 25는 Z, 26은 AA, 27은 AB)")
        void toColumnLetter_ColumnIndexes_ReturnsExcelLetters() {
            // Given
            List<Integer> columns = List.of(0, 25, 26, 27, 701, 702);

            // When
            List<String> letters = columns.stream()
                    .map(BudgetImportSession::toColumnLetter)
                    .toList();

            // Then
            assertThat(letters).containsExactly("A", "Z", "AA", "AB", "ZZ", "AAA");
        }
    }

    @Nested
    @DisplayName("getColumnLabel 테스트")
    class GetColumnLabelTest {

        @Test
        @DisplayName("열 문자에 머리글 행의 글자를 덧붙인다")
        void getColumnLabel_WithHeader_AppendsHeaderText() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();

            // When
            String label = session.getColumnLabel(1);

            // Then
            assertThat(label).isEqualTo("B: 예산 1단계");
        }

        @Test
        @DisplayName("머리글이 없다고 지정하면 열 문자만 보여준다")
        void getColumnLabel_NoHeaderRow_ReturnsLetterOnly() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.setHeaderRowNumber(0);

            // When
            String label = session.getColumnLabel(1);

            // Then
            assertThat(label).isEqualTo("B");
        }
    }

    @Nested
    @DisplayName("guessMappingIfNeeded 테스트")
    class GuessMappingIfNeededTest {

        @Test
        @DisplayName("프로그램이 내보낸 파일의 머리글에서 구분, 단계별 예산명, 예산액의 열을 알아본다")
        void guessMappingIfNeeded_ExportedHeaders_GuessesAllColumns() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(BudgetImportSession::getFixedType, BudgetImportSession::getTypeColumn,
                            BudgetImportSession::getLevelColumns, BudgetImportSession::getAmountColumn)
                    .containsExactly(null, 0, List.of(1, 2), 3);
        }

        @Test
        @DisplayName("관/항/목 머리글을 상위 단계부터 순서대로 단계별 예산명으로 본다")
        void guessMappingIfNeeded_NamedLevelHeaders_GuessesLevelsInOrder() {
            // Given
            BudgetImportSession session = createSessionWithHeaders("목", "관", "항", "금액");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session.getLevelColumns()).containsExactly(1, 2, 0);
        }

        @Test
        @DisplayName("구분 열을 찾지 못하면 파일 전체를 수입 예산으로 둔다")
        void guessMappingIfNeeded_NoTypeHeader_DefaultsToIncome() {
            // Given
            BudgetImportSession session = createSessionWithHeaders("예산명", "금액");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(BudgetImportSession::getFixedType, BudgetImportSession::getLevelColumns,
                            BudgetImportSession::getAmountColumn)
                    .containsExactly(BudgetType.INCOME, List.of(0), 1);
        }

        @Test
        @DisplayName("한 번 추측한 뒤에는 사용자가 바꾼 열 지정을 다시 덮어쓰지 않는다")
        void guessMappingIfNeeded_AlreadyGuessed_KeepsUserMapping() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setAmountColumn(4);

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session.getAmountColumn()).isEqualTo(4);
        }

        @Test
        @DisplayName("머리글 행을 바꾸면 새 머리글로 다시 추측한다")
        void guessMappingIfNeeded_HeaderRowChanged_GuessesAgain() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setHeaderRowNumber(0);

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session.getLevelColumns()).isEmpty();
        }
    }

    @Nested
    @DisplayName("validateMapping 테스트")
    class ValidateMappingTest {

        @Test
        @DisplayName("구분, 예산명, 금액의 열이 모두 지정되어 있으면 문제가 없다")
        void validateMapping_CompleteMapping_ReturnsNull() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isNull();
        }

        @Test
        @DisplayName("금액 열을 지정하지 않으면 지정하라고 알려준다")
        void validateMapping_NoAmountColumn_ReturnsProblem() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setAmountColumn(null);

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("금액이 있는 열을 지정해주세요.");
        }

        @Test
        @DisplayName("같은 열을 여러 단계에 지정하면 알려준다")
        void validateMapping_DuplicateLevelColumns_ReturnsProblem() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setLevelColumns(List.of(1, 1));

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("같은 열을 여러 단계에 지정할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("toRequest 테스트")
    class ToRequestTest {

        @Test
        @DisplayName("머리글 아래의 행만 넘기고, 첫 데이터 행의 파일 행 번호를 함께 넘긴다")
        void toRequest_HeaderOnFirstRow_PassesRowsBelowHeader() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            BudgetImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(result -> result.getRows().size(), BudgetImportRequestDto::getFirstRowNumber)
                    .containsExactly(2, 2);
        }

        @Test
        @DisplayName("전체 예산명 열을 쓰면 단계별 열은 넘기지 않는다")
        void toRequest_PathMode_OmitsLevelColumns() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setPathMode(true);
            session.setPathColumn(1);

            // When
            BudgetImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(BudgetImportRequestDto::getLevelColumns, BudgetImportRequestDto::getPathColumn)
                    .containsExactly(List.of(), 1);
        }
    }

    @Nested
    @DisplayName("applyAnalysis 테스트")
    class ApplyAnalysisTest {

        @Test
        @DisplayName("처음 분석하면 고를 수 있는 건을 모두 선택한다")
        void applyAnalysis_FirstAnalysis_SelectsAllSelectableItems() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            BudgetImportAnalysisDto analysis = analysisOf(
                    item(PARENT, null, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(OTHER, null, true, 500L, ImportItemStatus.SKIPPED, BudgetImportAction.NONE));

            // When
            session.applyAnalysis(analysis);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT);
        }

        @Test
        @DisplayName("처음 분석한 것은 달라진 건으로 세지 않는다")
        void applyAnalysis_FirstAnalysis_ReturnsZeroChanges() {
            // Given
            BudgetImportSession session = new BudgetImportSession();

            // When
            int changedCount = session.applyAnalysis(createNewHierarchyAnalysis());

            // Then
            assertThat(changedCount).isZero();
        }

        @Test
        @DisplayName("다시 분석했을 때 상태가 달라진 건의 수를 알려준다")
        void applyAnalysis_StatusChanged_ReturnsChangedCount() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            // 그 사이에 다른 메뉴에서 "기타" 예산을 등록한 상황
            BudgetImportAnalysisDto reanalysis = analysisOf(
                    item(PARENT, null, false, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(CHILD, PARENT, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(OTHER, null, true, 500L, ImportItemStatus.SKIPPED, BudgetImportAction.NONE));

            // When
            int changedCount = session.applyAnalysis(reanalysis);

            // Then
            assertThat(changedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("다시 분석해도 사용자가 직접 뺀 건은 빠진 채로 둔다")
        void applyAnalysis_Reanalysis_KeepsExplicitDeselection() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            session.setSelected(OTHER, false);

            // When
            session.applyAnalysis(createNewHierarchyAnalysis());

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT, CHILD);
        }

        @Test
        @DisplayName("다시 분석해서 고를 수 없게 된 건은 선택에서 뺀다")
        void applyAnalysis_ItemBecameUnselectable_RemovesFromSelection() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            BudgetImportAnalysisDto reanalysis = analysisOf(
                    item(PARENT, null, false, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(CHILD, PARENT, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(OTHER, null, true, 500L, ImportItemStatus.ERROR, BudgetImportAction.NONE));

            // When
            session.applyAnalysis(reanalysis);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT, CHILD);
        }
    }

    @Nested
    @DisplayName("setSelected 테스트")
    class SetSelectedTest {

        @Test
        @DisplayName("새로 추가되는 상위 예산을 빼면 그 하위 예산도 함께 빠진다")
        void setSelected_DeselectParent_DeselectsDescendants() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());

            // When
            session.setSelected(PARENT, false);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(OTHER);
        }

        @Test
        @DisplayName("하위 예산을 고르면 새로 추가되는 상위 예산도 함께 골라진다")
        void setSelected_SelectChild_SelectsNewParent() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            session.setSelected(PARENT, false);

            // When
            session.setSelected(CHILD, true);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT, CHILD, OTHER);
        }

        @Test
        @DisplayName("이미 있는 상위 예산 아래의 하위 예산은 상위 예산을 고르지 않아도 고를 수 있다")
        void setSelected_ChildUnderExistingParent_SelectableAlone() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(analysisOf(
                    item(PARENT, null, false, 1000L, ImportItemStatus.SKIPPED, BudgetImportAction.NONE),
                    item(CHILD, PARENT, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE)));

            // When
            session.setSelected(CHILD, true);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(CHILD);
        }
    }

    @Nested
    @DisplayName("selectAll 테스트")
    class SelectAllTest {

        @Test
        @DisplayName("전체 해제하면 선택된 건이 없어진다")
        void selectAll_False_DeselectsEverything() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());

            // When
            session.selectAll(false);

            // Then
            assertThat(session.getSelectedKeys()).isEmpty();
        }

        @Test
        @DisplayName("전체 선택하면 직접 뺐던 건도 다시 선택된다")
        void selectAll_True_SelectsEverySelectableItem() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            session.setSelected(PARENT, false);

            // When
            session.selectAll(true);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT, CHILD, OTHER);
        }
    }

    @Nested
    @DisplayName("resetSelection 테스트")
    class ResetSelectionTest {

        @Test
        @DisplayName("직접 뺐던 건을 다시 골라, 고를 수 있는 건이 모두 선택된 처음 상태로 되돌린다")
        void resetSelection_ExplicitDeselection_RestoresInitialSelection() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(createNewHierarchyAnalysis());
            session.setSelected(PARENT, false);

            // When
            session.resetSelection();

            // Then
            assertThat(session.getSelectedKeys()).containsExactly(PARENT, CHILD, OTHER);
        }
    }

    @Nested
    @DisplayName("summarize 테스트")
    class SummarizeTest {

        @Test
        @DisplayName("고른 건의 추가/갱신 건수를 세고, 금액은 말단 예산만 더한다")
        void summarize_SelectedItems_CountsActionsAndSumsLeafAmounts() {
            // Given
            BudgetImportSession session = new BudgetImportSession();
            session.applyAnalysis(analysisOf(
                    item(PARENT, null, false, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(CHILD, PARENT, true, 1000L, ImportItemStatus.OK, BudgetImportAction.CREATE),
                    item(OTHER, null, true, 500L, ImportItemStatus.OK, BudgetImportAction.UPDATE_AMOUNT)));

            // When
            SelectionSummary summary = session.summarize();

            // Then
            assertThat(summary).isEqualTo(new SelectionSummary(2, 1, 1500L));
        }
    }

    @Nested
    @DisplayName("setData 테스트")
    class SetDataTest {

        @Test
        @DisplayName("파일을 새로 읽으면 이전의 분석 결과와 받아 둔 백업을 버린다")
        void setData_NewFile_ClearsAnalysisAndBackup() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.applyAnalysis(createNewHierarchyAnalysis());
            session.setBackup(10L, Path.of("backup.csbackup"));

            // When
            session.setData(Path.of("다른 파일.csv"), null, false, List.of(), null, null,
                    new SpreadsheetData(List.of(Arrays.asList("예산명", "금액")), null));

            // Then
            assertThat(session)
                    .extracting(BudgetImportSession::getAnalysis, BudgetImportSession::hasBackup)
                    .containsExactly(null, false);
        }
    }

    @Nested
    @DisplayName("reset 테스트")
    class ResetTest {

        @Test
        @DisplayName("읽어 둔 파일 내용과 열 지정을 모두 버린다")
        void reset_SessionInProgress_ClearsFileAndMapping() {
            // Given
            BudgetImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            session.reset();

            // Then
            assertThat(session)
                    .extracting(BudgetImportSession::hasData, BudgetImportSession::getLevelColumns,
                            BudgetImportSession::getFixedType)
                    .containsExactly(false, List.of(), BudgetType.INCOME);
        }
    }

}
