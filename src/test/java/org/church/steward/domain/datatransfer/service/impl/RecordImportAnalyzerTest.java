package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.domain.datatransfer.dto.ImportMappingDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecordImportAnalyzerTest {

    private static final LocalDate PERIOD_START = LocalDate.of(2026, 1, 1);
    private static final LocalDate PERIOD_END = LocalDate.of(2026, 12, 31);

    // 등록된 예산: 수입 "헌금 > 십일조", "헌금 > 감사헌금" / 지출 "운영비"
    private final Budget offering = budget(1L, BudgetType.INCOME, "헌금", null);
    private final Budget tithe = budget(2L, BudgetType.INCOME, "십일조", offering);
    private final Budget thanks = budget(3L, BudgetType.INCOME, "감사헌금", offering);
    private final Budget operating = budget(4L, BudgetType.EXPENSE, "운영비", null);
    private final List<Budget> budgets = List.of(offering, tithe, thanks, operating);

    // 등록된 관계자
    private final Contact hong = contact(10L, "홍길동", false);
    private final Contact kim = contact(11L, "김철수", false);
    private final Contact vendor = contact(12L, "OO상사", false);
    private final Contact anonymous = contact(13L, "무명", false);
    private final List<Contact> contacts = List.of(hong, kim, vendor, anonymous);

    private static Budget budget(Long id, BudgetType type, String name, Budget parent) {
        Budget budget = Budget.builder()
                .id(id)
                .type(type)
                .name(name)
                .parent(parent)
                .children(new ArrayList<>())
                .build();
        if (parent != null) {
            parent.getChildren().add(budget);
        }
        return budget;
    }

    private static Contact contact(Long id, String name, boolean hidden) {
        return Contact.builder()
                .id(id)
                .type(ContactType.INDIVIDUAL)
                .name(name)
                .isHidden(hidden)
                .build();
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    /**
     * 열 구성이 "구분 | 일자 | 예산 | 금액 | 내용 | 관계자 | 비고"인 파일의 열 지정. 데이터는 파일의 2행부터 시작한다.
     */
    private RecordImportRequestDto.RecordImportRequestDtoBuilder standardRequest(List<List<Object>> rows) {
        return RecordImportRequestDto.builder()
                .rows(rows)
                .firstRowNumber(2)
                .typeColumn(0)
                .dateColumn(1)
                .budgetPathColumn(2)
                .amountColumn(3)
                .descriptionColumn(4)
                .contactColumn(5)
                .remarksColumn(6);
    }

    private RecordImportAnalysisDto analyze(RecordImportRequestDto request) {
        return analyze(request, contacts, List.of());
    }

    private RecordImportAnalysisDto analyze(RecordImportRequestDto request, List<Contact> registeredContacts,
            List<FinancialRecord> existingRecords) {
        return RecordImportAnalyzer.analyze(request, PERIOD_START, PERIOD_END, budgets, registeredContacts,
                existingRecords);
    }

    private RecordImportItemDto firstItem(RecordImportAnalysisDto analysis) {
        return analysis.getItems().getFirst();
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("예산과 관계자가 모두 등록되어 있으면 행을 정상으로 읽고 등록된 것에 연결한다")
        void analyze_AllRegistered_ReadsRowAndResolvesTargets() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "헌금 > 십일조", 1000L, "3월 첫째 주", "홍길동, 김철수", "비고")))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getRowNumber, RecordImportItemDto::getType,
                            RecordImportItemDto::getDate, RecordImportItemDto::getBudgetId,
                            RecordImportItemDto::getAmount, RecordImportItemDto::getDescription,
                            RecordImportItemDto::getContactIds, RecordImportItemDto::getRemarks,
                            RecordImportItemDto::getStatus)
                    .containsExactly(2, FinancialRecordType.INCOME, LocalDate.of(2026, 3, 5), 2L, 1000L, "3월 첫째 주",
                            List.of(10L, 11L), "비고", ImportItemStatus.OK);
        }

        @Test
        @DisplayName("일자와 금액이 모두 없는 행은 건으로 만들지 않고 건너뛴 행으로 센다")
        void analyze_RowWithoutDateAndAmount_CountsAsBlankRow() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row(null, null, "합계", null, null, null, null),
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis)
                    .extracting(result -> result.getItems().size(), RecordImportAnalysisDto::getBlankRowCount)
                    .containsExactly(1, 1);
        }

        @Test
        @DisplayName("일자는 여러 형식의 글자로 읽고, 연도가 없으면 회계 연도의 기간으로 연도를 정한다")
        void analyze_DateFormats_ParsesTextDates() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", "2026-03-05", "십일조", 1000L, null, "홍길동", null),
                    row("수입", "2026. 3. 5.", "십일조", 1000L, null, "홍길동", null),
                    row("수입", "2026년 3월 5일", "십일조", 1000L, null, "홍길동", null),
                    row("수입", 20260305L, "십일조", 1000L, null, "홍길동", null),
                    row("수입", "26.3.5", "십일조", 1000L, null, "홍길동", null),
                    row("수입", "3/5", "십일조", 1000L, null, "홍길동", null),
                    row("수입", "3월 5일", "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getDate)
                    .containsOnly(LocalDate.of(2026, 3, 5));
        }

        @Test
        @DisplayName("연, 월, 일이 다른 열에 있어도 일자를 읽는다")
        void analyze_SeparateDateColumns_ReadsDate() {
            // Given
            RecordImportRequestDto request = RecordImportRequestDto.builder()
                    .rows(List.of(row(3L, "5일", "십일조", 1000L, "홍길동")))
                    .firstRowNumber(2)
                    .fixedType(FinancialRecordType.INCOME)
                    .monthColumn(0)
                    .dayColumn(1)
                    .budgetPathColumn(2)
                    .amountColumn(3)
                    .contactColumn(4)
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis).getDate()).isEqualTo(LocalDate.of(2026, 3, 5));
        }

        @Test
        @DisplayName("일자가 회계 연도의 기간을 벗어난 행은 오류이며 사유에 기간을 적는다")
        void analyze_DateOutOfPeriod_ErrorWithPeriod() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2027, 1, 1), "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getStatus, RecordImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR,
                            "회계 연도 기간(2026-01-01 ~ 2026-12-31)을 벗어난 날짜입니다: 2027-01-01");
        }

        @Test
        @DisplayName("일자를 읽을 수 없거나 없는 날짜인 행은 오류다")
        void analyze_InvalidDate_Error() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", "삼월 오일", "십일조", 1000L, null, "홍길동", null),
                    row("수입", "2026-02-30", "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getStatus, RecordImportItemDto::getMessage)
                    .containsExactly(
                            tuple(ImportItemStatus.ERROR, "일자를 읽을 수 없습니다: 삼월 오일"),
                            tuple(ImportItemStatus.ERROR, "없는 날짜입니다: 2026-2-30"));
        }

        @Test
        @DisplayName("수입 금액과 지출 금액의 열이 따로 있으면 금액이 적힌 쪽으로 구분을 정한다")
        void analyze_SplitAmountColumns_DeterminesTypeByFilledColumn() {
            // Given
            RecordImportRequestDto request = RecordImportRequestDto.builder()
                    .rows(List.of(
                            row(LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동"),
                            row(LocalDate.of(2026, 3, 6), "운영비", 0L, 500L, "OO상사")))
                    .firstRowNumber(2)
                    .dateColumn(0)
                    .budgetPathColumn(1)
                    .incomeAmountColumn(2)
                    .expenseAmountColumn(3)
                    .contactColumn(4)
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getType, RecordImportItemDto::getAmount,
                            RecordImportItemDto::getStatus)
                    .containsExactly(
                            tuple(FinancialRecordType.INCOME, 1000L, ImportItemStatus.OK),
                            tuple(FinancialRecordType.EXPENSE, 500L, ImportItemStatus.OK));
        }

        @Test
        @DisplayName("수입 금액과 지출 금액이 모두 적힌 행은 오류다")
        void analyze_BothSplitAmountsFilled_Error() {
            // Given
            RecordImportRequestDto request = RecordImportRequestDto.builder()
                    .rows(List.of(row(LocalDate.of(2026, 3, 5), "십일조", 1000L, 500L, "홍길동")))
                    .firstRowNumber(2)
                    .dateColumn(0)
                    .budgetPathColumn(1)
                    .incomeAmountColumn(2)
                    .expenseAmountColumn(3)
                    .contactColumn(4)
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis).getMessage()).isEqualTo("수입 금액과 지출 금액이 모두 적혀 있습니다.");
        }

        @Test
        @DisplayName("구분 열의 '입금'은 수입으로, '출금'은 지출로 읽는다")
        void analyze_TypeSynonyms_ReadsAsIncomeAndExpense() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("입금", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null),
                    row("출금", LocalDate.of(2026, 3, 5), "운영비", 500L, null, "OO상사", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getType)
                    .containsExactly(FinancialRecordType.INCOME, FinancialRecordType.EXPENSE);
        }

        @Test
        @DisplayName("구분 열에 나온 값마다 매핑 항목을 만들고, 아는 값은 자동으로 수입 또는 지출에 연결한다")
        void analyze_TypeColumn_CreatesTypeMappingPerValue() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 6), "십일조", 2000L, null, "홍길동", null),
                    row("출금", LocalDate.of(2026, 3, 5), "운영비", 500L, null, "OO상사", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getTypeMappings())
                    .extracting(ImportMappingDto::getLabel, ImportMappingDto::getCount,
                            ImportMappingDto::getTotalAmount, ImportMappingDto::getTargetType,
                            ImportMappingDto::getTargetLabel, ImportMappingDto::isManual)
                    .containsExactly(
                            tuple("수입", 2, 3000L, FinancialRecordType.INCOME, "수입", false),
                            tuple("출금", 1, 500L, FinancialRecordType.EXPENSE, "지출", false));
        }

        @Test
        @DisplayName("알 수 없는 구분 값은 연결하지 않고, 그 값을 쓰는 행을 오류로 둔다")
        void analyze_UnknownTypeValue_LeavesUnmappedAndRowIsError() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("세입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getTypeMappings().getFirst())
                    .extracting(ImportMappingDto::isMapped, ImportMappingDto::getNote)
                    .containsExactly(false, "알 수 없는 구분입니다. 수입 또는 지출로 지정해주세요.");
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getStatus, RecordImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "구분이 지정되지 않았습니다: 세입");
        }

        @Test
        @DisplayName("직접 한 구분 매핑은 그 값을 쓰는 모든 행에 적용되고, 그 행들의 예산과 관계자도 연결된다")
        void analyze_ManualTypeMapping_AppliesToRowsAndResolvesTheirTargets() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("세입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null),
                    row("세출", LocalDate.of(2026, 3, 5), "운영비", 500L, null, "OO상사", null)))
                    .typeMappings(Map.of("세입", FinancialRecordType.INCOME, "세출", FinancialRecordType.EXPENSE))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getType, RecordImportItemDto::getBudgetId,
                            RecordImportItemDto::getStatus)
                    .containsExactly(
                            tuple(FinancialRecordType.INCOME, 2L, ImportItemStatus.OK),
                            tuple(FinancialRecordType.EXPENSE, 4L, ImportItemStatus.OK));
        }

        @Test
        @DisplayName("직접 한 구분 매핑은 아는 값의 자동 연결보다 우선한다")
        void analyze_ManualTypeMappingForKnownValue_OverridesAutoMapping() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "운영비", 500L, null, "OO상사", null)))
                    .typeMappings(Map.of("수입", FinancialRecordType.EXPENSE))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getTypeMappings().getFirst())
                    .extracting(ImportMappingDto::getTargetType, ImportMappingDto::isManual)
                    .containsExactly(FinancialRecordType.EXPENSE, true);
        }

        @Test
        @DisplayName("구분을 열에서 읽지 않으면 구분 매핑 항목이 없다")
        void analyze_FixedType_HasNoTypeMappings() {
            // Given
            RecordImportRequestDto request = RecordImportRequestDto.builder()
                    .rows(List.of(row(LocalDate.of(2026, 3, 5), "십일조", 1000L, "홍길동")))
                    .firstRowNumber(2)
                    .fixedType(FinancialRecordType.INCOME)
                    .dateColumn(0)
                    .budgetPathColumn(1)
                    .amountColumn(2)
                    .contactColumn(3)
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getTypeMappings()).isEmpty();
        }

        @Test
        @DisplayName("이름만 적힌 예산은 그 이름의 말단 예산이 하나일 때 자동으로 연결한다")
        void analyze_BudgetLeafName_AutoMapsUniqueLeaf() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 6), "십일조", 2000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getBudgetMappings())
                    .extracting(ImportMappingDto::getLabel, ImportMappingDto::getTypeLabel, ImportMappingDto::getCount,
                            ImportMappingDto::getTotalAmount, ImportMappingDto::getTargetId,
                            ImportMappingDto::getTargetLabel, ImportMappingDto::isManual)
                    .containsExactly(tuple("십일조", "수입", 2, 3000L, 2L, "헌금 > 십일조", false));
        }

        @Test
        @DisplayName("등록되지 않은 예산은 자동으로 만들지 않고, 그 예산을 쓰는 행을 오류로 둔다")
        void analyze_UnregisteredBudget_LeavesUnmappedAndRowIsError() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "건축헌금", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getBudgetMappings().getFirst())
                    .extracting(ImportMappingDto::isMapped, ImportMappingDto::getNote)
                    .containsExactly(false, "등록되지 않은 예산입니다.");
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getStatus, RecordImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "예산이 지정되지 않았습니다: 건축헌금");
        }

        @Test
        @DisplayName("예산이 비어 있는 행은 빈 칸 항목으로 모으고, 이름이 적힌 항목과 구별해 표시한다")
        void analyze_BlankBudget_MarksBlankMapping() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), null, 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getBudgetMappings())
                    .extracting(ImportMappingDto::getLabel, ImportMappingDto::isBlank, ImportMappingDto::getNote)
                    .containsExactly(
                            tuple("(빈 칸)", true, "예산이 비어 있습니다."),
                            tuple("십일조", false, ""));
        }

        @Test
        @DisplayName("하위 예산이 있는 예산과 다른 구분의 예산에는 자동으로 연결하지 않는다")
        void analyze_ParentOrOtherTypeBudget_NotAutoMapped() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "헌금", 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 5), "운영비", 1000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getBudgetMappings())
                    .extracting(ImportMappingDto::getLabel, ImportMappingDto::isMapped, ImportMappingDto::getNote)
                    .containsExactly(
                            tuple("헌금", false, "하위 예산이 있는 예산입니다. 말단 예산을 지정해주세요."),
                            tuple("운영비", false, "등록되지 않은 예산입니다."));
        }

        @Test
        @DisplayName("직접 한 예산 매핑은 그 값을 쓰는 모든 행에 적용된다")
        void analyze_ManualBudgetMapping_AppliesToAllRowsWithThatValue() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "건축헌금", 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 6), "건축헌금", 2000L, null, "김철수", null)))
                    .budgetMappings(Map.of("INCOME:건축헌금", 3L))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getBudgetId, RecordImportItemDto::getBudgetLabel,
                            RecordImportItemDto::getStatus)
                    .containsExactly(
                            tuple(3L, "헌금 > 감사헌금", ImportItemStatus.OK),
                            tuple(3L, "헌금 > 감사헌금", ImportItemStatus.OK));
        }

        @Test
        @DisplayName("직접 매핑한 예산이 삭제되었거나 말단 예산이 아니게 되었으면 매핑을 따르지 않는다")
        void analyze_ManualBudgetMappingToInvalidTarget_Ignored() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "건축헌금", 1000L, null, "홍길동", null),
                    row("수입", LocalDate.of(2026, 3, 5), "선교헌금", 1000L, null, "홍길동", null)))
                    .budgetMappings(Map.of("INCOME:건축헌금", 999L, "INCOME:선교헌금", 1L))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getBudgetMappings())
                    .extracting(ImportMappingDto::isMapped)
                    .containsExactly(false, false);
        }

        @Test
        @DisplayName("쉼표로 구분한 이름마다 관계자를 연결하고, 이름은 값마다 한 항목으로 모은다")
        void analyze_CommaSeparatedContacts_ResolvesEachName() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, " 홍길동 ,김철수", null),
                    row("수입", LocalDate.of(2026, 3, 6), "십일조", 2000L, null, "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getContactMappings())
                    .extracting(ImportMappingDto::getLabel, ImportMappingDto::getCount, ImportMappingDto::getTargetId)
                    .containsExactly(
                            tuple("홍길동", 2, 10L),
                            tuple("김철수", 1, 11L));
        }

        @Test
        @DisplayName("동명이인이면 숨김 처리되지 않은 관계자 중 가장 최근에 등록된 관계자에 연결한다")
        void analyze_Homonyms_MapsToLatestVisibleContact() {
            // Given
            List<Contact> homonyms = List.of(contact(20L, "이영희", false), contact(22L, "이영희", true),
                    contact(21L, "이영희", false));
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "이영희", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request, homonyms, List.of());

            // Then
            assertThat(analysis.getContactMappings().getFirst())
                    .extracting(ImportMappingDto::getTargetId, ImportMappingDto::getNote)
                    .containsExactly(21L, "동명이인 2명 중 가장 최근에 등록된 관계자입니다.");
        }

        @Test
        @DisplayName("같은 이름의 관계자가 모두 숨김이면 자동으로 연결하지 않는다")
        void analyze_OnlyHiddenContacts_NotAutoMapped() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "이영희", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request, List.of(contact(20L, "이영희", true)), List.of());

            // Then
            assertThat(analysis.getContactMappings().getFirst())
                    .extracting(ImportMappingDto::isMapped, ImportMappingDto::getNote)
                    .containsExactly(false, "숨김 처리된 관계자만 있습니다.");
        }

        @Test
        @DisplayName("등록되지 않은 이름은 자동으로 만들지 않고, 그 이름을 쓰는 행을 오류로 둔다")
        void analyze_UnregisteredContact_RowIsError() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "홍길동, 박민수", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getStatus, RecordImportItemDto::getMessage)
                    .containsExactly(ImportItemStatus.ERROR, "관계자가 지정되지 않았습니다: 박민수");
        }

        @Test
        @DisplayName("관계자가 비어 있는 행은 구분마다 하나인 빈 칸 항목을 쓰며, 여기에 매핑한 관계자가 적용된다")
        void analyze_BlankContacts_UsesBlankMappingPerType() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, null, null),
                    row("지출", LocalDate.of(2026, 3, 5), "운영비", 500L, null, null, null)))
                    .contactMappings(Map.of("INCOME:", 13L))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getContactMappings())
                    .extracting(ImportMappingDto::getKey, ImportMappingDto::getLabel, ImportMappingDto::isBlank,
                            ImportMappingDto::getTypeLabel, ImportMappingDto::getTargetId, ImportMappingDto::isManual)
                    .containsExactly(
                            tuple("INCOME:", "(빈 칸)", true, "수입", 13L, true),
                            tuple("EXPENSE:", "(빈 칸)", true, "지출", null, false));
        }

        @Test
        @DisplayName("같은 이름이라도 구분이 다르면 다른 항목이며, 금액 합계와 직접 한 매핑도 구분별로 따로 적용된다")
        void analyze_SameNameInIncomeAndExpense_SeparatesMappingPerType() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, null, "박민수", null),
                    row("지출", LocalDate.of(2026, 3, 6), "운영비", 300L, null, "박민수", null)))
                    .contactMappings(Map.of("EXPENSE:박민수", 12L))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(analysis.getContactMappings())
                    .extracting(ImportMappingDto::getKey, ImportMappingDto::getLabel, ImportMappingDto::getTypeLabel,
                            ImportMappingDto::getTotalAmount, ImportMappingDto::getTargetId, ImportMappingDto::isManual)
                    .containsExactly(
                            tuple("INCOME:박민수", "박민수", "수입", 1000L, null, false),
                            tuple("EXPENSE:박민수", "박민수", "지출", 300L, 12L, true));
        }

        @Test
        @DisplayName("지출에 수취인이 두 명 이상이면 오류다")
        void analyze_ExpenseWithMultipleContacts_Error() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("지출", LocalDate.of(2026, 3, 5), "운영비", 500L, null, "OO상사, 홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis).getMessage()).isEqualTo("지출의 수취인은 한 명이어야 합니다.");
        }

        @Test
        @DisplayName("이미 같은 내역이 있으면 그 건수만큼만 중복 의심으로 표시한다")
        void analyze_ExistingSameRecords_MarksDuplicatesByCount() {
            // Given
            FinancialRecord existing = FinancialRecord.builder()
                    .id(100L)
                    .type(FinancialRecordType.INCOME)
                    .date(LocalDate.of(2026, 3, 5))
                    .budget(tithe)
                    .amount(1000L)
                    .description("무명 헌금")
                    .contacts(List.of(FinancialRecordContact.builder()
                            .contact(anonymous)
                            .contactName("무명")
                            .build()))
                    .build();
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, "무명 헌금", "무명", null),
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, "무명 헌금", "무명", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request, contacts, List.of(existing));

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::isDuplicate, RecordImportItemDto::getStatus,
                            RecordImportItemDto::getMessage)
                    .containsExactly(
                            tuple(true, ImportItemStatus.WARNING, "같은 내역이 이미 있습니다. (중복 의심)"),
                            tuple(false, ImportItemStatus.OK, ""));
        }

        @Test
        @DisplayName("내용이 100자를 넘으면 잘라서 저장하고 경고로 알린다")
        void analyze_LongDescription_TruncatesWithWarning() {
            // Given
            RecordImportRequestDto request = standardRequest(List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "십일조", 1000L, "가".repeat(101), "홍길동", null)))
                    .build();

            // When
            RecordImportAnalysisDto analysis = analyze(request);

            // Then
            assertThat(firstItem(analysis))
                    .extracting(RecordImportItemDto::getDescription, RecordImportItemDto::getStatus,
                            RecordImportItemDto::getMessage)
                    .containsExactly("가".repeat(100), ImportItemStatus.WARNING, "내용이(가) 100자를 넘어 잘립니다.");
        }

        @Test
        @DisplayName("예산이나 관계자의 매핑이 달라지면 요약 값도 달라진다")
        void analyze_MappingChanged_ProducesDifferentSignature() {
            // Given
            List<List<Object>> rows = List.of(
                    row("수입", LocalDate.of(2026, 3, 5), "건축헌금", 1000L, null, "홍길동", null));

            // When
            String before = analyze(standardRequest(rows).build()).getSignature();
            String after = analyze(standardRequest(rows)
                    .budgetMappings(Map.of("INCOME:건축헌금", 3L))
                    .build()).getSignature();

            // Then
            assertThat(after).isNotEqualTo(before);
        }

        @Test
        @DisplayName("일자, 구분과 금액, 예산의 열을 지정하지 않으면 분석하지 않는다")
        void analyze_MissingColumnMapping_Failure() {
            // Given
            RecordImportRequestDto request = RecordImportRequestDto.builder()
                    .rows(List.of())
                    .fixedType(FinancialRecordType.INCOME)
                    .amountColumn(1)
                    .budgetPathColumn(2)
                    .build();

            // When & Then
            assertThatThrownBy(() -> analyze(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("일자가 있는 열을 지정해주세요.");
        }
    }

}
