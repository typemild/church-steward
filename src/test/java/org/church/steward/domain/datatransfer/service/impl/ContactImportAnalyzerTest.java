package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.util.Arrays;
import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactConstraints;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactImportAnalyzerTest {

    // 등록된 관계자
    private final Contact hong = contact(10L, ContactType.INDIVIDUAL, "홍길동", "1구역");
    private final Contact bank = contact(11L, ContactType.VENDOR, "한빛은행", null);
    private final List<Contact> contacts = List.of(hong, bank);

    private static Contact contact(Long id, ContactType type, String name, String remarks) {
        return Contact.builder()
                .id(id)
                .type(type)
                .name(name)
                .remarks(remarks)
                .isHidden(false)
                .build();
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    /**
     * 열 구성이 "구분 | 이름 | 비고 | 상태"인 파일의 열 지정. 데이터는 파일의 2행부터 시작한다.
     */
    private ContactImportRequestDto.ContactImportRequestDtoBuilder standardRequest(List<List<Object>> rows) {
        return ContactImportRequestDto.builder()
                .rows(rows)
                .firstRowNumber(2)
                .typeColumn(0)
                .nameColumn(1)
                .remarksColumn(2)
                .hiddenColumn(3);
    }

    private ContactImportAnalysisDto analyze(ContactImportRequestDto request) {
        return ContactImportAnalyzer.analyze(request, contacts);
    }

    private ContactImportItemDto firstItem(ContactImportAnalysisDto analysis) {
        return analysis.getItems().getFirst();
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("구분, 이름, 비고, 상태를 읽어 등록된 관계자와 겹치지 않으면 정상으로 본다")
        void analyze_NewContact_ReadsRowAsOk() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("단체", "새빛선교회", "해외 선교", "숨김")))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(ContactImportItemDto::getRowNumber, ContactImportItemDto::getType,
                            ContactImportItemDto::getName, ContactImportItemDto::getRemarks,
                            ContactImportItemDto::isHidden, ContactImportItemDto::getStatus)
                    .containsExactly(2, ContactType.ORGANIZATION, "새빛선교회", "해외 선교", true, ImportItemStatus.OK);
        }

        @Test
        @DisplayName("파일 전체의 구분을 정하면 구분 열 없이 그 구분으로 읽는다")
        void analyze_FixedType_UsesFixedType() {
            // Given
            ContactImportRequestDto request = ContactImportRequestDto.builder()
                    .rows(List.of(row("김찬양")))
                    .firstRowNumber(2)
                    .fixedType(ContactType.INDIVIDUAL)
                    .nameColumn(0)
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(ContactImportItemDto::getType, ContactImportItemDto::getRemarks,
                            ContactImportItemDto::isHidden)
                    .containsExactly(ContactType.INDIVIDUAL, null, false);
        }

        @Test
        @DisplayName("구분은 띄어쓰기를 무시하고 읽는다")
        void analyze_TypeWithSpace_ReadsType() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("내부 부서", "청년부", null, null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis).getType()).isEqualTo(ContactType.INTERNAL_DEPARTMENT);
        }

        @Test
        @DisplayName("상태 칸은 숨김을 뜻하는 값이면 숨김으로, 비어 있거나 표시를 뜻하는 값이면 표시로 읽는다")
        void analyze_HiddenValues_ReadsHiddenFlag() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row("개인", "가", null, "숨김"),
                    row("개인", "나", null, "Y"),
                    row("개인", "다", null, "true"),
                    row("개인", "라", null, "표시"),
                    row("개인", "마", null, null),
                    row("개인", "바", null, "N")))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::isHidden)
                    .containsExactly(true, true, true, false, false, false);
        }

        @Test
        @DisplayName("이름이 빈 행은 건너뛰고 그 수를 센다")
        void analyze_BlankName_SkipsRow() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row("개인", "김찬양", null, null),
                    row("개인", "  ", "비고만 있음", null),
                    row(null, null, null, null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis)
                    .extracting(result -> result.getItems().size(), ContactImportAnalysisDto::getBlankRowCount)
                    .containsExactly(1, 2);
        }

        @Test
        @DisplayName("구분이 비어 있거나 읽을 수 없는 값이면 오류다")
        void analyze_InvalidType_Error() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row(null, "김찬양", null, null),
                    row("교인", "이믿음", null, null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::getStatus, ContactImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "구분이 비어 있습니다."),
                            tuple(ImportItemStatus.ERROR, "구분을 읽을 수 없습니다: 교인 (개인, 내부부서, 업체, 단체, 기타 중 하나여야 합니다)"));
        }

        @Test
        @DisplayName("상태를 읽을 수 없는 값이면 오류다")
        void analyze_InvalidHidden_Error() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("개인", "김찬양", null, "보류"))).build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(ContactImportItemDto::getStatus, ContactImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "상태를 읽을 수 없습니다: 보류 (숨김 또는 표시여야 합니다)");
        }

        @Test
        @DisplayName("이름이 최대 길이를 넘으면 오류다")
        void analyze_NameTooLong_Error() {
            // Given
            String name = "가".repeat(ContactConstraints.NAME_MAX_LENGTH + 1);
            ContactImportRequestDto request = standardRequest(List.of(row("개인", name, null, null))).build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(ContactImportItemDto::getStatus, ContactImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "이름이 100자를 넘습니다.");
        }

        @Test
        @DisplayName("비고가 최대 길이를 넘으면 잘라서 저장하고 경고로 알린다")
        void analyze_RemarksTooLong_TruncatesWithWarning() {
            // Given
            String remarks = "나".repeat(ContactConstraints.REMARKS_MAX_LENGTH + 5);
            ContactImportRequestDto request = standardRequest(List.of(row("개인", "김찬양", remarks, null))).build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(item -> item.getRemarks().length(), ContactImportItemDto::getStatus,
                            ContactImportItemDto::getMessage)
                    .containsExactly(ContactConstraints.REMARKS_MAX_LENGTH, ImportItemStatus.WARNING,
                            "비고가 100자를 넘어 잘립니다.");
        }

        @Test
        @DisplayName("구분, 이름, 비고가 모두 같은 관계자가 이미 있으면 중복 의심이다")
        void analyze_SameContactExists_MarksDuplicate() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("개인", "홍길동", "1구역", null))).build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(ContactImportItemDto::isDuplicate, ContactImportItemDto::getStatus,
                            ContactImportItemDto::getMessage)
                    .containsExactly(true, ImportItemStatus.WARNING, "같은 관계자가 이미 있습니다. (중복 의심)");
        }

        @Test
        @DisplayName("비고가 없는 관계자는 비고 칸이 빈 행과 같은 관계자로 본다")
        void analyze_ExistingWithoutRemarks_MatchesBlankRemarks() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("업체", "한빛은행", "", null))).build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis).isDuplicate()).isTrue();
        }

        @Test
        @DisplayName("이미 있는 관계자와는 건수로 비교하고, 파일 안에서 같은 관계자가 다시 나오면 중복 의심이다")
        void analyze_SameContactRepeatedInFile_MarksAllAsDuplicate() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row("개인", "홍길동", "1구역", null),
                    row("개인", "홍길동", "1구역", null),
                    row("개인", "김찬양", null, null),
                    row("개인", "김찬양", null, null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::isDuplicate, ContactImportItemDto::getMessage)
                    .containsExactly(
                            tuple(true, "같은 관계자가 이미 있습니다. (중복 의심)"),
                            tuple(true, "파일의 앞쪽 행에 같은 관계자가 있습니다. (중복 의심)"),
                            tuple(false, ""),
                            tuple(true, "파일의 앞쪽 행에 같은 관계자가 있습니다. (중복 의심)"));
        }

        @Test
        @DisplayName("이름만 같은 관계자가 있으면 동명이인일 수 있다고 경고하되 중복 의심으로 보지 않는다")
        void analyze_SameNameOnly_WarnsHomonym() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row("개인", "홍길동", "2구역", null),
                    row("개인", "김찬양", "1구역", null),
                    row("개인", "김찬양", "청년부", null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::isDuplicate, ContactImportItemDto::getStatus,
                            ContactImportItemDto::getMessage)
                    .containsExactly(
                            tuple(false, ImportItemStatus.WARNING, "이름이 같은 관계자가 이미 있습니다. (동명이인)"),
                            tuple(false, ImportItemStatus.OK, ""),
                            tuple(false, ImportItemStatus.WARNING, "파일의 앞쪽 행에 이름이 같은 관계자가 있습니다. (동명이인)"));
        }

        @Test
        @DisplayName("행마다 파일의 행 번호로 식별자를 붙인다")
        void analyze_Rows_AssignsKeysByRowNumber() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(
                    row("개인", "김찬양", null, null),
                    row(null, null, null, null),
                    row("개인", "이믿음", null, null)))
                    .build();

            // When
            ContactImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(ContactImportItemDto::getKey)
                    .containsExactly("row:2", "row:4");
        }

        @Test
        @DisplayName("등록된 관계자가 바뀌어 분석 결과가 달라지면 요약 값도 달라진다")
        void analyze_ContactsChanged_ChangesSignature() {
            // Given
            ContactImportRequestDto request = standardRequest(List.of(row("개인", "김찬양", null, null))).build();
            String before = ContactImportAnalyzer.analyze(request, List.of()).getSignature();

            // When
            String after = ContactImportAnalyzer.analyze(request,
                    List.of(contact(20L, ContactType.INDIVIDUAL, "김찬양", null))).getSignature();

            // Then
            assertThat(after).isNotEqualTo(before);
        }

        @Test
        @DisplayName("이름이 있는 열을 지정하지 않으면 분석하지 않는다")
        void analyze_NoNameColumn_Failure() {
            // Given
            ContactImportRequestDto request = ContactImportRequestDto.builder()
                    .rows(List.of())
                    .fixedType(ContactType.INDIVIDUAL)
                    .build();

            // When & Then
            assertThatThrownBy(() -> analyze(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이름이 있는 열을 지정해주세요.");
        }

        @Test
        @DisplayName("구분을 지정하지 않으면 분석하지 않는다")
        void analyze_NoType_Failure() {
            // Given
            ContactImportRequestDto request = ContactImportRequestDto.builder()
                    .rows(List.of())
                    .nameColumn(0)
                    .build();

            // When & Then
            assertThatThrownBy(() -> analyze(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("구분을 지정해주세요.");
        }
    }

}
