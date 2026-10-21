package org.church.steward.ui.view.datamanagement.recordimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.domain.datatransfer.dto.ImportMappingDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.AnalysisChange;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.SelectionSummary;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.TypeMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecordImportSessionTest {

    private static final String BUDGET_KEY = "INCOME:건축헌금";

    private RecordImportSession createSessionWithHeaders(String... headers) {
        RecordImportSession session = new RecordImportSession();
        session.setData(Path.of("수입지출.xlsx"), null, true, List.of("Sheet1"), "Sheet1", null,
                new SpreadsheetData(List.of(
                        Arrays.asList((Object[]) headers),
                        Arrays.asList(new Object[headers.length])), null));
        return session;
    }

    /**
     * 프로그램이 내보낸 수입/지출 파일과 같은 머리글을 가진 파일을 읽어 둔 세션을 만든다.
     */
    private RecordImportSession createSessionWithExportedFile() {
        return createSessionWithHeaders("구분", "일자", "예산 1단계", "예산 2단계", "금액", "내용", "봉헌자/수취인", "비고");
    }

    private RecordImportItemDto item(String key, FinancialRecordType type, long amount, ImportItemStatus status,
            boolean duplicate) {
        return RecordImportItemDto.builder()
                .key(key)
                .rowNumber(2)
                .type(type)
                .date(LocalDate.of(2026, 3, 5))
                .amount(amount)
                .budgetId(2L)
                .contactIds(List.of(10L))
                .status(status)
                .message("")
                .duplicate(duplicate)
                .build();
    }

    private ImportMappingDto mapping(String key, Long targetId, boolean manual) {
        return ImportMappingDto.builder()
                .key(key)
                .label(key)
                .targetId(targetId)
                .manual(manual)
                .note("")
                .build();
    }

    private RecordImportAnalysisDto analysisOf(List<RecordImportItemDto> items,
            List<ImportMappingDto> budgetMappings, List<ImportMappingDto> contactMappings) {
        return RecordImportAnalysisDto.builder()
                .items(items)
                .budgetMappings(budgetMappings)
                .contactMappings(contactMappings)
                .signature("signature")
                .build();
    }

    private RecordImportAnalysisDto analysisOf(RecordImportItemDto... items) {
        return analysisOf(List.of(items), List.of(), List.of());
    }

    @Nested
    @DisplayName("guessMappingIfNeeded 테스트")
    class GuessMappingIfNeededTest {

        @Test
        @DisplayName("프로그램이 내보낸 파일의 머리글에서 모든 항목의 열을 알아본다")
        void guessMappingIfNeeded_ExportedHeaders_GuessesAllColumns() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(RecordImportSession::getTypeMode, RecordImportSession::getTypeColumn,
                            RecordImportSession::getDateColumn, RecordImportSession::getBudgetLevelColumns,
                            RecordImportSession::getAmountColumn, RecordImportSession::getDescriptionColumn,
                            RecordImportSession::getContactColumn, RecordImportSession::getRemarksColumn)
                    .containsExactly(TypeMode.TYPE_COLUMN, 0, 1, List.of(2, 3), 4, 5, 6, 7);
        }

        @Test
        @DisplayName("수입과 지출의 금액 머리글이 따로 있으면 금액 열이 따로 있는 방식으로 본다")
        void guessMappingIfNeeded_IncomeAndExpenseHeaders_GuessesSplitAmounts() {
            // Given
            RecordImportSession session = createSessionWithHeaders("날짜", "적요", "수입", "지출", "잔액", "계정과목", "성명");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(RecordImportSession::getTypeMode, RecordImportSession::getDateColumn,
                            RecordImportSession::getDescriptionColumn, RecordImportSession::getIncomeAmountColumn,
                            RecordImportSession::getExpenseAmountColumn, RecordImportSession::getBudgetLevelColumns,
                            RecordImportSession::getContactColumn)
                    .containsExactly(TypeMode.SPLIT_AMOUNTS, 0, 1, 2, 3, List.of(5), 6);
        }

        @Test
        @DisplayName("일자 머리글이 없고 월과 일의 머리글이 있으면 일자를 따로 적힌 열에서 읽는다")
        void guessMappingIfNeeded_MonthAndDayHeaders_GuessesSeparateDate() {
            // Given
            RecordImportSession session = createSessionWithHeaders("월", "일", "항목", "금액");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(RecordImportSession::isSeparateDate, RecordImportSession::getMonthColumn,
                            RecordImportSession::getDayColumn)
                    .containsExactly(true, 0, 1);
        }
    }

    @Nested
    @DisplayName("validateMapping 테스트")
    class ValidateMappingTest {

        @Test
        @DisplayName("필요한 열이 모두 지정되어 있으면 문제가 없다 (관계자 열은 없어도 된다)")
        void validateMapping_CompleteMapping_ReturnsNull() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setContactColumn(null);

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isNull();
        }

        @Test
        @DisplayName("금액 열이 따로 있는 방식인데 한쪽 열이 없으면 지정하라고 알려준다")
        void validateMapping_SplitAmountsWithoutExpenseColumn_ReturnsProblem() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setTypeMode(TypeMode.SPLIT_AMOUNTS);
            session.setIncomeAmountColumn(4);

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("수입 금액과 지출 금액이 있는 열을 지정해주세요.");
        }

        @Test
        @DisplayName("일자 열을 지정하지 않으면 지정하라고 알려준다")
        void validateMapping_NoDateColumn_ReturnsProblem() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setDateColumn(null);

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("일자가 있는 열을 지정해주세요.");
        }
    }

    @Nested
    @DisplayName("toRequest 테스트")
    class ToRequestTest {

        @Test
        @DisplayName("구분 열에서 읽는 방식이면 구분 열과 금액 열을 넘기고 파일 전체의 구분은 넘기지 않는다")
        void toRequest_TypeColumnMode_PassesTypeAndAmountColumns() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            RecordImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(RecordImportRequestDto::getFixedType, RecordImportRequestDto::getTypeColumn,
                            RecordImportRequestDto::getAmountColumn, RecordImportRequestDto::getIncomeAmountColumn,
                            RecordImportRequestDto::getFirstRowNumber)
                    .containsExactly(null, 0, 4, null, 2);
        }

        @Test
        @DisplayName("파일 전체가 지출인 방식이면 구분을 지출로 고정해 넘긴다")
        void toRequest_AllExpenseMode_PassesFixedType() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setTypeMode(TypeMode.ALL_EXPENSE);

            // When
            RecordImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(RecordImportRequestDto::getFixedType, RecordImportRequestDto::getTypeColumn)
                    .containsExactly(FinancialRecordType.EXPENSE, null);
        }

        @Test
        @DisplayName("직접 한 예산과 관계자의 매핑을 함께 넘긴다")
        void toRequest_ManualMappings_PassesMappings() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setBudgetMapping(List.of(BUDGET_KEY), 3L);
            session.setContactMapping(List.of("INCOME:", "INCOME:무명씨"), 13L);

            // When
            RecordImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(RecordImportRequestDto::getBudgetMappings, RecordImportRequestDto::getContactMappings)
                    .containsExactly(Map.of(BUDGET_KEY, 3L), Map.of("INCOME:", 13L, "INCOME:무명씨", 13L));
        }
    }

    @Nested
    @DisplayName("setTypeMapping 테스트")
    class SetTypeMappingTest {

        @Test
        @DisplayName("직접 한 구분 매핑은 분석과 반영에 넘길 입력에 담긴다")
        void setTypeMapping_Type_IsPassedInRequest() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            session.setTypeMapping(List.of("세입", "수납"), FinancialRecordType.INCOME);

            // Then
            assertThat(session.toRequest().getTypeMappings())
                    .isEqualTo(Map.of("세입", FinancialRecordType.INCOME, "수납", FinancialRecordType.INCOME));
        }

        @Test
        @DisplayName("구분을 null로 주면 직접 한 매핑을 해제한다")
        void setTypeMapping_NullType_RemovesMapping() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.setTypeMapping(List.of("세입"), FinancialRecordType.INCOME);

            // When
            session.setTypeMapping(List.of("세입"), null);

            // Then
            assertThat(session.getTypeMappings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("setBudgetMapping 테스트")
    class SetBudgetMappingTest {

        @Test
        @DisplayName("대상을 null로 주면 직접 한 매핑을 해제한다")
        void setBudgetMapping_NullTarget_RemovesMapping() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.setBudgetMapping(List.of(BUDGET_KEY), 3L);

            // When
            session.setBudgetMapping(List.of(BUDGET_KEY), null);

            // Then
            assertThat(session.getBudgetMappings()).isEmpty();
        }
    }

    @Nested
    @DisplayName("applyAnalysis 테스트")
    class ApplyAnalysisTest {

        @Test
        @DisplayName("처음 분석하면 가져올 수 있는 행 중 중복 의심이 아닌 행만 선택한다")
        void applyAnalysis_FirstAnalysis_SelectsSelectableNonDuplicates() {
            // Given
            RecordImportSession session = new RecordImportSession();
            RecordImportAnalysisDto analysis = analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 1000L, ImportItemStatus.WARNING, true),
                    item("row:4", FinancialRecordType.INCOME, 1000L, ImportItemStatus.ERROR, false));

            // When
            session.applyAnalysis(analysis);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2");
        }

        @Test
        @DisplayName("다시 분석해도 사용자가 직접 고른 중복 의심 행은 선택된 채로 둔다")
        void applyAnalysis_Reanalysis_KeepsExplicitSelectionOfDuplicate() {
            // Given
            RecordImportSession session = new RecordImportSession();
            RecordImportAnalysisDto analysis = analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.WARNING, true));
            session.applyAnalysis(analysis);
            session.setSelected("row:2", true);

            // When
            session.applyAnalysis(analysis);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2");
        }

        @Test
        @DisplayName("다시 분석했을 때 상태가 달라진 행의 수를 알려준다")
        void applyAnalysis_StatusChanged_ReturnsChangedItemCount() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.applyAnalysis(analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false)));

            // When
            AnalysisChange change = session.applyAnalysis(analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 1000L, ImportItemStatus.ERROR, false)));

            // Then
            assertThat(change).isEqualTo(new AnalysisChange(1, 0));
        }

        @Test
        @DisplayName("직접 한 매핑을 서비스가 따르지 않았으면(대상이 삭제됨 등) 그 매핑을 해제하고 해제한 수를 알려준다")
        void applyAnalysis_ManualMappingIgnoredByService_ReleasesMapping() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.setBudgetMapping(List.of(BUDGET_KEY), 3L);
            RecordImportAnalysisDto analysis = analysisOf(List.of(), List.of(mapping(BUDGET_KEY, null, false)),
                    List.of());

            // When
            AnalysisChange change = session.applyAnalysis(analysis);

            // Then
            assertThat(change.releasedMappingCount()).isEqualTo(1);
            assertThat(session.getBudgetMappings()).isEmpty();
        }

        @Test
        @DisplayName("파일에 더는 나오지 않는 구분 값의 매핑은 지우고, 나오는 값의 매핑은 그대로 둔다")
        void applyAnalysis_TypeMappings_KeepsOnlyValuesInFile() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.setTypeMapping(List.of("세입", "예전 값"), FinancialRecordType.INCOME);
            RecordImportAnalysisDto analysis = RecordImportAnalysisDto.builder()
                    .items(List.of())
                    .typeMappings(List.of(mapping("세입", null, true)))
                    .budgetMappings(List.of())
                    .contactMappings(List.of())
                    .build();

            // When
            session.applyAnalysis(analysis);

            // Then
            assertThat(session.getTypeMappings()).containsOnlyKeys("세입");
        }

        @Test
        @DisplayName("직접 한 매핑이 분석 결과에 반영되었으면 그대로 둔다")
        void applyAnalysis_ManualMappingApplied_KeepsMapping() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.setContactMapping(List.of("INCOME:"), 13L);
            RecordImportAnalysisDto analysis = analysisOf(List.of(), List.of(),
                    List.of(mapping("INCOME:", 13L, true)));

            // When
            session.applyAnalysis(analysis);

            // Then
            assertThat(session.getContactMappings()).containsEntry("INCOME:", 13L);
        }
    }

    @Nested
    @DisplayName("countUnmapped 테스트")
    class CountUnmappedTest {

        @Test
        @DisplayName("연결되지 않은 구분 값도 함께 센다")
        void countUnmapped_UnmappedType_IsCounted() {
            // Given
            RecordImportSession session = new RecordImportSession();
            ImportMappingDto unknownType = mapping("세입", null, false);
            ImportMappingDto knownType = ImportMappingDto.builder()
                    .key("수입")
                    .targetType(FinancialRecordType.INCOME)
                    .build();
            session.applyAnalysis(RecordImportAnalysisDto.builder()
                    .items(List.of())
                    .typeMappings(List.of(unknownType, knownType))
                    .budgetMappings(List.of())
                    .contactMappings(List.of())
                    .build());

            // When
            int unmappedCount = session.countUnmapped();

            // Then
            assertThat(unmappedCount).isEqualTo(1);
        }

        @Test
        @DisplayName("연결되지 않은 예산 값과 관계자 이름의 수를 더해 알려준다")
        void countUnmapped_UnmappedBudgetAndContact_ReturnsTotal() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.applyAnalysis(analysisOf(List.of(),
                    List.of(mapping("INCOME:십일조", 2L, false), mapping(BUDGET_KEY, null, false)),
                    List.of(mapping("INCOME:박민수", null, false))));

            // When
            int unmappedCount = session.countUnmapped();

            // Then
            assertThat(unmappedCount).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("selectAll 테스트")
    class SelectAllTest {

        @Test
        @DisplayName("전체 선택하면 중복 의심 행까지 선택하되 오류 행은 선택하지 않는다")
        void selectAll_True_SelectsDuplicatesButNotErrors() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.applyAnalysis(analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 1000L, ImportItemStatus.WARNING, true),
                    item("row:4", FinancialRecordType.INCOME, 1000L, ImportItemStatus.ERROR, false)));

            // When
            session.selectAll(true);

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2", "row:3");
        }
    }

    @Nested
    @DisplayName("resetSelection 테스트")
    class ResetSelectionTest {

        @Test
        @DisplayName("직접 고른 중복 의심 행은 빼고 직접 뺀 행은 다시 골라, 오류와 중복 의심이 아닌 행만 선택된 처음 상태로 되돌린다")
        void resetSelection_ExplicitSelections_RestoresInitialSelection() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.applyAnalysis(analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 1000L, ImportItemStatus.WARNING, true),
                    item("row:4", FinancialRecordType.INCOME, 1000L, ImportItemStatus.ERROR, false)));
            session.setSelected("row:2", false);
            session.setSelected("row:3", true);

            // When
            session.resetSelection();

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2");
        }
    }

    @Nested
    @DisplayName("summarize 테스트")
    class SummarizeTest {

        @Test
        @DisplayName("고른 행의 건수와 금액을 수입과 지출로 나누어 더한다")
        void summarize_SelectedRows_SumsByType() {
            // Given
            RecordImportSession session = new RecordImportSession();
            session.applyAnalysis(analysisOf(
                    item("row:2", FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK, false),
                    item("row:3", FinancialRecordType.INCOME, 2000L, ImportItemStatus.OK, false),
                    item("row:4", FinancialRecordType.EXPENSE, 500L, ImportItemStatus.OK, false),
                    item("row:5", FinancialRecordType.EXPENSE, 700L, ImportItemStatus.ERROR, false)));

            // When
            SelectionSummary summary = session.summarize();

            // Then
            assertThat(summary).isEqualTo(new SelectionSummary(2, 3000L, 1, 500L));
        }
    }

    @Nested
    @DisplayName("setData 테스트")
    class SetDataTest {

        @Test
        @DisplayName("파일을 새로 읽으면 이전 파일의 값에 대한 직접 한 매핑을 버린다")
        void setData_NewFile_ClearsManualMappings() {
            // Given
            RecordImportSession session = createSessionWithExportedFile();
            session.setBudgetMapping(List.of(BUDGET_KEY), 3L);

            // When
            session.setData(Path.of("다른 파일.csv"), null, false, List.of(), null, null,
                    new SpreadsheetData(List.of(Arrays.asList("일자", "금액")), null));

            // Then
            assertThat(session.getBudgetMappings()).isEmpty();
        }
    }

}
