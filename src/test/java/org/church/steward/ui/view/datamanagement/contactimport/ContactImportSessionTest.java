package org.church.steward.ui.view.datamanagement.contactimport;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.ui.view.datamanagement.contactimport.ContactImportSession.SelectionSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactImportSessionTest {

    /**
     * 프로그램이 내보낸 관계자 파일과 같은 머리글을 가진 파일을 읽어 둔 세션을 만든다.
     */
    private ContactImportSession createSessionWithExportedFile() {
        ContactImportSession session = new ContactImportSession();
        session.setData(Path.of("관계자.csv"), null, false, List.of(), null, null,
                new SpreadsheetData(List.of(
                        Arrays.asList("구분", "이름", "비고", "상태"),
                        Arrays.asList("개인", "김찬양", "1구역", "표시"),
                        Arrays.asList("기타", "무명", null, "숨김")), null));
        return session;
    }

    private ContactImportSession createSessionWithHeaders(String... headers) {
        ContactImportSession session = new ContactImportSession();
        session.setData(Path.of("관계자.xlsx"), null, true, List.of("Sheet1"), "Sheet1", null,
                new SpreadsheetData(List.of(Arrays.asList((Object[]) headers)), null));
        return session;
    }

    private ContactImportItemDto item(String key, ImportItemStatus status, boolean duplicate, boolean hidden) {
        return ContactImportItemDto.builder()
                .key(key)
                .type(ContactType.INDIVIDUAL)
                .name(key)
                .hidden(hidden)
                .status(status)
                .message("")
                .duplicate(duplicate)
                .build();
    }

    private ContactImportAnalysisDto analysisOf(ContactImportItemDto... items) {
        return ContactImportAnalysisDto.builder()
                .items(List.of(items))
                .signature("signature")
                .build();
    }

    /**
     * 정상인 행, 숨김으로 가져올 행, 중복 의심인 행, 오류인 행.
     */
    private ContactImportAnalysisDto createAnalysis() {
        return analysisOf(
                item("row:2", ImportItemStatus.OK, false, false),
                item("row:3", ImportItemStatus.OK, false, true),
                item("row:4", ImportItemStatus.WARNING, true, false),
                item("row:5", ImportItemStatus.ERROR, false, false));
    }

    @Nested
    @DisplayName("guessMappingIfNeeded 테스트")
    class GuessMappingIfNeededTest {

        @Test
        @DisplayName("프로그램이 내보낸 파일의 머리글이면 구분, 이름, 비고, 상태 열을 모두 알아보고 구분을 열에서 읽는다")
        void guessMappingIfNeeded_ExportedHeaders_GuessesAllColumns() {
            // Given
            ContactImportSession session = createSessionWithExportedFile();

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(ContactImportSession::getFixedType, ContactImportSession::getTypeColumn,
                            ContactImportSession::getNameColumn, ContactImportSession::getRemarksColumn,
                            ContactImportSession::getHiddenColumn)
                    .containsExactly(null, 0, 1, 2, 3);
        }

        @Test
        @DisplayName("구분 열이 없으면 파일 전체를 개인으로 읽게 두고, 성명이나 거래처 같은 머리글을 이름 열로 본다")
        void guessMappingIfNeeded_NoTypeColumn_FixesTypeToIndividual() {
            // Given
            ContactImportSession session = createSessionWithHeaders("번호", "성명", "메모");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(ContactImportSession::getFixedType, ContactImportSession::getTypeColumn,
                            ContactImportSession::getNameColumn, ContactImportSession::getRemarksColumn)
                    .containsExactly(ContactType.INDIVIDUAL, null, 1, 2);
        }

        @Test
        @DisplayName("이름을 뜻하는 낱말이 함께 있어도 구분이 들어간 머리글은 구분 열로 본다")
        void guessMappingIfNeeded_ContactTypeHeader_GuessesTypeColumn() {
            // Given
            ContactImportSession session = createSessionWithHeaders("관계자 구분", "관계자");

            // When
            session.guessMappingIfNeeded();

            // Then
            assertThat(session)
                    .extracting(ContactImportSession::getTypeColumn, ContactImportSession::getNameColumn)
                    .containsExactly(0, 1);
        }
    }

    @Nested
    @DisplayName("validateMapping 테스트")
    class ValidateMappingTest {

        @Test
        @DisplayName("이름 열을 지정하지 않으면 지정하라고 알린다")
        void validateMapping_NoNameColumn_ReturnsProblem() {
            // Given
            ContactImportSession session = createSessionWithHeaders("번호");
            session.guessMappingIfNeeded();

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("이름이 있는 열을 지정해주세요.");
        }

        @Test
        @DisplayName("구분을 열에서 읽기로 하고 구분 열을 지정하지 않으면 지정하라고 알린다")
        void validateMapping_TypeFromColumnWithoutColumn_ReturnsProblem() {
            // Given
            ContactImportSession session = createSessionWithHeaders("이름");
            session.guessMappingIfNeeded();
            session.setFixedType(null);

            // When
            String problem = session.validateMapping();

            // Then
            assertThat(problem).isEqualTo("구분이 있는 열을 지정해주세요.");
        }
    }

    @Nested
    @DisplayName("toRequest 테스트")
    class ToRequestTest {

        @Test
        @DisplayName("머리글 아래의 행과 열 지정을 넘긴다")
        void toRequest_ExportedFile_PassesDataRowsAndColumns() {
            // Given
            ContactImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();

            // When
            ContactImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(dto -> dto.getRows().size(), ContactImportRequestDto::getFirstRowNumber,
                            ContactImportRequestDto::getFixedType, ContactImportRequestDto::getTypeColumn,
                            ContactImportRequestDto::getNameColumn)
                    .containsExactly(2, 2, null, 0, 1);
        }

        @Test
        @DisplayName("파일 전체의 구분을 정했으면 구분 열을 넘기지 않는다")
        void toRequest_FixedType_OmitsTypeColumn() {
            // Given
            ContactImportSession session = createSessionWithExportedFile();
            session.guessMappingIfNeeded();
            session.setFixedType(ContactType.VENDOR);

            // When
            ContactImportRequestDto request = session.toRequest();

            // Then
            assertThat(request)
                    .extracting(ContactImportRequestDto::getFixedType, ContactImportRequestDto::getTypeColumn)
                    .containsExactly(ContactType.VENDOR, null);
        }
    }

    @Nested
    @DisplayName("applyAnalysis 테스트")
    class ApplyAnalysisTest {

        @Test
        @DisplayName("오류 행과 중복 의심 행을 뺀 행을 선택한다")
        void applyAnalysis_FirstAnalysis_SelectsRowsExceptErrorsAndDuplicates() {
            // Given
            ContactImportSession session = new ContactImportSession();

            // When
            session.applyAnalysis(createAnalysis());

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2", "row:3");
        }

        @Test
        @DisplayName("다시 분석해도 직접 바꾼 선택은 유지한다")
        void applyAnalysis_Reanalyzed_KeepsExplicitSelections() {
            // Given
            ContactImportSession session = new ContactImportSession();
            session.applyAnalysis(createAnalysis());
            session.setSelected("row:2", false);
            session.setSelected("row:4", true);

            // When
            session.applyAnalysis(createAnalysis());

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:3", "row:4");
        }

        @Test
        @DisplayName("이전 분석 결과와 비교해 상태가 달라진 행의 수를 돌려준다")
        void applyAnalysis_StatusChanged_ReturnsChangedCount() {
            // Given
            ContactImportSession session = new ContactImportSession();
            session.applyAnalysis(createAnalysis());

            // When
            int changedCount = session.applyAnalysis(analysisOf(
                    item("row:2", ImportItemStatus.WARNING, true, false),
                    item("row:3", ImportItemStatus.OK, false, true),
                    item("row:4", ImportItemStatus.WARNING, true, false),
                    item("row:5", ImportItemStatus.ERROR, false, false)));

            // Then
            assertThat(changedCount).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("resetSelection 테스트")
    class ResetSelectionTest {

        @Test
        @DisplayName("직접 바꾼 선택을 버리고 처음 분석했을 때의 선택으로 되돌린다")
        void resetSelection_ExplicitSelections_RestoresDefaultSelection() {
            // Given
            ContactImportSession session = new ContactImportSession();
            session.applyAnalysis(createAnalysis());
            session.setSelected("row:2", false);
            session.setSelected("row:4", true);

            // When
            session.resetSelection();

            // Then
            assertThat(session.getSelectedKeys()).containsExactly("row:2", "row:3");
        }
    }

    @Nested
    @DisplayName("summarize 테스트")
    class SummarizeTest {

        @Test
        @DisplayName("고른 관계자의 수와 그중 숨김으로 가져올 관계자의 수를 센다")
        void summarize_DefaultSelection_CountsSelectedAndHidden() {
            // Given
            ContactImportSession session = new ContactImportSession();
            session.applyAnalysis(createAnalysis());

            // When
            SelectionSummary summary = session.summarize();

            // Then
            assertThat(summary).isEqualTo(new SelectionSummary(2, 1));
        }
    }

}
