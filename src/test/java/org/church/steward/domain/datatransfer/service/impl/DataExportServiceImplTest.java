package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetTable;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataExportServiceImplTest {

    private static final Long FISCAL_YEAR_ID = 7L;
    private static final Path TARGET = Path.of("exports", "내보내기.xlsx");

    @Mock
    private FiscalYearRepository fiscalYearRepository;
    @Mock
    private FinancialRecordRepository financialRecordRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private SpreadsheetWriter spreadsheetWriter;

    @InjectMocks
    private DataExportServiceImpl dataExportService;

    @Captor
    private ArgumentCaptor<SpreadsheetTable> tableCaptor;

    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(FISCAL_YEAR_ID)
            .name("2026년")
            .build();

    private BudgetDto createBudgetDto(Long id, Long parentId, BudgetType type, String name, int sortOrder,
            long totalAmount, long actualAmount) {
        return BudgetDto.builder()
                .id(id)
                .parentId(parentId)
                .type(type)
                .name(name)
                .sortOrder(sortOrder)
                .totalAmount(totalAmount)
                .actualAmount(actualAmount)
                .build();
    }

    /**
     * 수입 예산: 헌금(십일조, 감사헌금). 조회 결과는 일부러 정렬 순서와 다르게 섞어 둔다.
     */
    private List<BudgetDto> createIncomeBudgetDtos() {
        return List.of(
                createBudgetDto(3L, 1L, BudgetType.INCOME, "감사헌금", 1, 300L, 30L),
                createBudgetDto(1L, null, BudgetType.INCOME, "헌금", 0, 1300L, 130L),
                createBudgetDto(2L, 1L, BudgetType.INCOME, "십일조", 0, 1000L, 100L));
    }

    /**
     * 지출 예산: 운영비.
     */
    private List<BudgetDto> createExpenseBudgetDtos() {
        return List.of(createBudgetDto(4L, null, BudgetType.EXPENSE, "운영비", 0, 500L, 50L));
    }

    private Budget createBudget(Long id, String name, Budget parent) {
        return Budget.builder()
                .id(id)
                .fiscalYear(fiscalYear)
                .type(BudgetType.INCOME)
                .name(name)
                .parent(parent)
                .build();
    }

    private FinancialRecord createIncomeRecord(Budget budget, String... contactNames) {
        return FinancialRecord.builder()
                .id(1L)
                .type(FinancialRecordType.INCOME)
                .date(LocalDate.of(2026, 3, 5))
                .budget(budget)
                .amount(1000L)
                .description("3월 첫째 주")
                .remarks("비고")
                .contacts(Arrays.stream(contactNames)
                        .map(name -> FinancialRecordContact.builder()
                                .contactName(name)
                                .build())
                        .toList())
                .build();
    }

    private Contact createContact(Long id, ContactType type, String name, String remarks, boolean hidden) {
        return Contact.builder()
                .id(id)
                .type(type)
                .name(name)
                .remarks(remarks)
                .isHidden(hidden)
                .build();
    }

    @Nested
    @DisplayName("exportBudgets 테스트")
    class ExportBudgetsTest {

        @Test
        @DisplayName("머리글은 구분, 가장 깊은 단계까지의 예산명, 예산액, 수입액/집행액이다")
        void exportBudgets_TwoLevels_WritesHeadersForDeepestLevel() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(createIncomeBudgetDtos());
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(createExpenseBudgetDtos());

            // When
            dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.XLSX), tableCaptor.capture(), isNull());
            assertThat(tableCaptor.getValue().headers())
                    .containsExactly("구분", "예산 1단계", "예산 2단계", "예산액", "수입액/집행액");
        }

        @Test
        @DisplayName("수입 예산 다음에 지출 예산을, 상위 예산 바로 아래에 하위 예산을 정렬 순서대로 적는다")
        void exportBudgets_Hierarchy_WritesRowsInTreeOrder() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(createIncomeBudgetDtos());
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(createExpenseBudgetDtos());

            // When
            dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().rows()).containsExactly(
                    Arrays.asList("수입", "헌금", null, 1300L, 130L),
                    Arrays.asList("수입", "헌금", "십일조", 1000L, 100L),
                    Arrays.asList("수입", "헌금", "감사헌금", 300L, 30L),
                    Arrays.asList("지출", "운영비", null, 500L, 50L));
        }

        @Test
        @DisplayName("내보낸 예산의 수를 반환한다")
        void exportBudgets_FourBudgets_ReturnsCount() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(createIncomeBudgetDtos());
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(createExpenseBudgetDtos());

            // When
            int count = dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            assertThat(count).isEqualTo(4);
        }

        @Test
        @DisplayName("활동 기록에 회계 연도의 이름, 파일 이름, 건수를 남긴다")
        void exportBudgets_Success_LogsExport() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(createIncomeBudgetDtos());
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(createExpenseBudgetDtos());

            // When
            dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(activityLogService).logExport("예산", "2026년", "내보내기.xlsx", 4);
        }

        @Test
        @DisplayName("입력한 암호를 그대로 넘겨 파일을 저장한다")
        void exportBudgets_WithPassword_PassesPassword() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(createIncomeBudgetDtos());
            when(budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(createExpenseBudgetDtos());

            // When
            dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, "secret");

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.XLSX), any(), eq("secret"));
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도의 예산은 내보낼 수 없다")
        void exportBudgets_FiscalYearNotFound_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(
                    () -> dataExportService.exportBudgets(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }
    }

    @Nested
    @DisplayName("exportFinancialRecords 테스트")
    class ExportFinancialRecordsTest {

        @Test
        @DisplayName("머리글은 구분, 일자, 단계별 예산명, 금액, 내용, 봉헌자/수취인, 비고다")
        void exportFinancialRecords_TwoLevels_WritesHeaders() {
            // Given
            Budget tithe = createBudget(2L, "십일조", createBudget(1L, "헌금", null));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createIncomeRecord(tithe, "홍길동")));

            // When
            dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().headers())
                    .containsExactly("구분", "일자", "예산 1단계", "예산 2단계", "금액", "내용", "봉헌자/수취인", "비고");
        }

        @Test
        @DisplayName("내역 하나를 한 행으로 적고, 여러 봉헌자는 쉼표로 이어 적는다")
        void exportFinancialRecords_MultipleContacts_JoinsNamesWithComma() {
            // Given
            Budget tithe = createBudget(2L, "십일조", createBudget(1L, "헌금", null));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createIncomeRecord(tithe, "홍길동", "김철수")));

            // When
            dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().rows()).containsExactly(
                    Arrays.asList("수입", LocalDate.of(2026, 3, 5), "헌금", "십일조", 1000L, "3월 첫째 주", "홍길동, 김철수", "비고"));
        }

        @Test
        @DisplayName("예산의 단계가 얕은 내역은 남는 예산 칸을 비워 둔다")
        void exportFinancialRecords_ShallowBudget_LeavesRemainingLevelsEmpty() {
            // Given
            Budget offering = createBudget(1L, "헌금", null);
            Budget interest = createBudget(3L, "이자수입", null);
            Budget tithe = createBudget(2L, "십일조", offering);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createIncomeRecord(tithe, "홍길동"), createIncomeRecord(interest, "OO은행")));

            // When
            dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().rows().get(1)).containsExactly("수입", LocalDate.of(2026, 3, 5), "이자수입",
                    null, 1000L, "3월 첫째 주", "OO은행", "비고");
        }

        @Test
        @DisplayName("활동 기록에 회계 연도의 이름, 파일 이름, 건수를 남긴다")
        void exportFinancialRecords_Success_LogsExport() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createIncomeRecord(createBudget(1L, "헌금", null), "홍길동")));

            // When
            dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(activityLogService).logExport("수입/지출", "2026년", "내보내기.xlsx", 1);
        }

        @Test
        @DisplayName("내역이 없어도 머리글만 있는 파일을 저장한다")
        void exportFinancialRecords_NoRecords_WritesHeadersOnly() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());

            // When
            dataExportService.exportFinancialRecords(FISCAL_YEAR_ID, TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().rows()).isEmpty();
        }
    }

    @Nested
    @DisplayName("exportContacts 테스트")
    class ExportContactsTest {

        @Test
        @DisplayName("머리글은 구분, 이름, 비고, 상태다")
        void exportContacts_Contacts_WritesHeaders() {
            // Given
            when(contactRepository.findAll()).thenReturn(List.of());

            // When
            dataExportService.exportContacts(TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.XLSX), tableCaptor.capture(), isNull());
            assertThat(tableCaptor.getValue().headers()).containsExactly("구분", "이름", "비고", "상태");
        }

        @Test
        @DisplayName("숨긴 관계자도 포함해 구분 순서, 이름순, 등록 순으로 한 행씩 적는다")
        void exportContacts_Contacts_WritesRowsInOrder() {
            // Given
            when(contactRepository.findAll()).thenReturn(List.of(
                    createContact(3L, ContactType.VENDOR, "한빛은행", "교회 통장", false),
                    createContact(2L, ContactType.INDIVIDUAL, "홍길동", null, true),
                    createContact(1L, ContactType.INDIVIDUAL, "홍길동", "1구역", false),
                    createContact(4L, ContactType.INDIVIDUAL, "김찬양", null, false)));

            // When
            dataExportService.exportContacts(TARGET, SpreadsheetFormat.XLSX, null);

            // Then
            verify(spreadsheetWriter).write(any(), any(), tableCaptor.capture(), any());
            assertThat(tableCaptor.getValue().rows()).containsExactly(
                    Arrays.asList("개인", "김찬양", null, "표시"),
                    Arrays.asList("개인", "홍길동", "1구역", "표시"),
                    Arrays.asList("개인", "홍길동", null, "숨김"),
                    Arrays.asList("업체", "한빛은행", "교회 통장", "표시"));
        }

        @Test
        @DisplayName("내보낸 관계자의 수를 반환한다")
        void exportContacts_TwoContacts_ReturnsCount() {
            // Given
            when(contactRepository.findAll()).thenReturn(List.of(
                    createContact(1L, ContactType.INDIVIDUAL, "홍길동", null, false),
                    createContact(2L, ContactType.OTHER, "무명", null, false)));

            // When
            int count = dataExportService.exportContacts(TARGET, SpreadsheetFormat.CSV, null);

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("활동 기록에 회계 연도 없이 파일 이름과 건수를 남긴다")
        void exportContacts_Success_LogsExportWithoutFiscalYear() {
            // Given
            when(contactRepository.findAll()).thenReturn(List.of(
                    createContact(1L, ContactType.INDIVIDUAL, "홍길동", null, false)));

            // When
            dataExportService.exportContacts(TARGET, SpreadsheetFormat.XLSX, "secret");

            // Then
            verify(activityLogService).logExport("관계자", null, "내보내기.xlsx", 1);
        }
    }

    @Nested
    @DisplayName("exportContactTemplate 테스트")
    class ExportContactTemplateTest {

        @Test
        @DisplayName("구분, 이름, 비고, 상태 머리글만 있는 빈 양식을 암호 없이 저장한다")
        void exportContactTemplate_Format_WritesHeadersOnly() {
            // Given

            // When
            dataExportService.exportContactTemplate(TARGET, SpreadsheetFormat.CSV);

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.CSV), tableCaptor.capture(), isNull());
            assertThat(tableCaptor.getValue())
                    .extracting(SpreadsheetTable::headers, SpreadsheetTable::rows)
                    .containsExactly(List.of("구분", "이름", "비고", "상태"), List.of());
        }
    }

    @Nested
    @DisplayName("exportBudgetTemplate 테스트")
    class ExportBudgetTemplateTest {

        @Test
        @DisplayName("예산 단계가 셋인 머리글만 있는 빈 양식을 암호 없이 저장한다")
        void exportBudgetTemplate_Format_WritesHeadersOnly() {
            // Given

            // When
            dataExportService.exportBudgetTemplate(TARGET, SpreadsheetFormat.CSV);

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.CSV), tableCaptor.capture(), isNull());
            assertThat(tableCaptor.getValue())
                    .extracting(SpreadsheetTable::headers, SpreadsheetTable::rows)
                    .containsExactly(List.of("구분", "예산 1단계", "예산 2단계", "예산 3단계", "예산액", "수입액/집행액"), List.of());
        }
    }

    @Nested
    @DisplayName("exportFinancialRecordTemplate 테스트")
    class ExportFinancialRecordTemplateTest {

        @Test
        @DisplayName("예산 단계가 셋인 머리글만 있는 빈 양식을 암호 없이 저장한다")
        void exportFinancialRecordTemplate_Format_WritesHeadersOnly() {
            // Given

            // When
            dataExportService.exportFinancialRecordTemplate(TARGET, SpreadsheetFormat.CSV);

            // Then
            verify(spreadsheetWriter).write(eq(TARGET), eq(SpreadsheetFormat.CSV), tableCaptor.capture(), isNull());
            assertThat(tableCaptor.getValue())
                    .extracting(SpreadsheetTable::headers, SpreadsheetTable::rows)
                    .containsExactly(
                            List.of("구분", "일자", "예산 1단계", "예산 2단계", "예산 3단계", "금액", "내용", "봉헌자/수취인", "비고"),
                            List.of());
        }
    }

}
