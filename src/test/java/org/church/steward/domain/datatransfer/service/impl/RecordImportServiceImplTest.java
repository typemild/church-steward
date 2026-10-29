package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportItemDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.financialrecord.service.FinancialRecordService;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
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
class RecordImportServiceImplTest {

    private static final Long FISCAL_YEAR_ID = 7L;
    private static final long BACKUP_LOG_ID = 10L;
    private static final String FILE_NAME = "수입지출.xlsx";

    @Mock
    private FiscalYearRepository fiscalYearRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private FinancialRecordRepository financialRecordRepository;
    @Mock
    private FinancialRecordService financialRecordService;
    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private RecordImportServiceImpl recordImportService;

    @Captor
    private ArgumentCaptor<FinancialRecordRequestDto> recordCaptor;

    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(FISCAL_YEAR_ID)
            .name("2026년")
            .startDate(LocalDate.of(2026, 1, 1))
            .endDate(LocalDate.of(2026, 12, 31))
            .build();

    private final Budget tithe = Budget.builder()
            .id(2L)
            .fiscalYear(fiscalYear)
            .type(BudgetType.INCOME)
            .name("십일조")
            .children(new ArrayList<>())
            .build();

    private final Contact hong = Contact.builder()
            .id(10L)
            .type(ContactType.INDIVIDUAL)
            .name("홍길동")
            .isHidden(false)
            .build();

    /**
     * 파일 내용: 십일조 수입 두 건(1,000원, 2,000원)과 등록되지 않은 예산을 쓰는 한 건. 열 구성은 "일자 | 예산 | 금액 |
     * 관계자"이고 파일 전체가 수입이다.
     */
    private RecordImportRequestDto createRequest() {
        return RecordImportRequestDto.builder()
                .rows(List.of(
                        Arrays.asList(LocalDate.of(2026, 3, 5), "십일조", 1000L, "홍길동"),
                        Arrays.asList(LocalDate.of(2026, 3, 6), "십일조", 2000L, "홍길동"),
                        Arrays.asList(LocalDate.of(2026, 3, 7), "건축헌금", 3000L, "홍길동")))
                .firstRowNumber(2)
                .fixedType(FinancialRecordType.INCOME)
                .dateColumn(0)
                .budgetPathColumn(1)
                .amountColumn(2)
                .contactColumn(3)
                .build();
    }

    /**
     * 고를 수 있는 건을 모두 고른다.
     */
    private Set<String> selectableKeys(RecordImportAnalysisDto analysis) {
        return analysis.getItems()
                .stream()
                .filter(RecordImportItemDto::isSelectable)
                .map(RecordImportItemDto::getKey)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("회계 연도의 예산, 등록된 관계자, 기존 내역과 견주어 분석한 결과를 반환한다")
        void analyze_CurrentData_ReturnsAnalysis() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());

            // When
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());

            // Then
            assertThat(analysis.getItems())
                    .extracting(RecordImportItemDto::getBudgetId, RecordImportItemDto::isSelectable)
                    .containsExactly(
                            tuple(2L, true),
                            tuple(2L, true),
                            tuple(null, false));
        }

        @Test
        @DisplayName("마감된 회계 연도에는 수입/지출을 가져올 수 없다")
        void analyze_ClosedFiscalYear_Failure() {
            // Given
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> recordImportService.analyze(FISCAL_YEAR_ID, createRequest()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도에는 수입/지출을 가져올 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("importRecords 테스트")
    class ImportRecordsTest {

        @Test
        @DisplayName("고른 행마다 연결된 예산과 관계자로 내역을 등록한다")
        void importRecords_SelectedRows_CreatesRecordsWithResolvedTargets() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);

            // When
            recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(financialRecordService, times(2)).createFinancialRecordWithoutLog(recordCaptor.capture());
            assertThat(recordCaptor.getAllValues())
                    .extracting(FinancialRecordRequestDto::getType, FinancialRecordRequestDto::getFiscalYearId,
                            FinancialRecordRequestDto::getDate, FinancialRecordRequestDto::getBudgetId,
                            FinancialRecordRequestDto::getAmount, FinancialRecordRequestDto::getContactIds)
                    .containsExactly(
                            tuple(FinancialRecordType.INCOME, FISCAL_YEAR_ID,
                                    LocalDate.of(2026, 3, 5), 2L, 1000L, List.of(10L)),
                            tuple(FinancialRecordType.INCOME, FISCAL_YEAR_ID,
                                    LocalDate.of(2026, 3, 6), 2L, 2000L, List.of(10L)));
        }

        @Test
        @DisplayName("등록한 건수를 반환한다")
        void importRecords_SelectedRows_ReturnsCreatedCount() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);

            // When
            ImportResultDto result = recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            assertThat(result.getCreatedCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("활동 기록에 가져오기 한 건으로 남기고, 구분별 건수와 금액을 요약에 적는다")
        void importRecords_SelectedRows_LogsOneImportWithSummary() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            List<ActivityLogFieldDto> description = List.of(ActivityLogFieldDto.builder()
                    .name("금액")
                    .value("1,000원")
                    .build());
            when(financialRecordService.createFinancialRecordWithoutLog(any()))
                    .thenReturn(FinancialRecord.builder()
                            .build());
            when(activityLogService.describeCreation(any(FinancialRecord.class))).thenReturn(description);

            // When
            recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(activityLogService).logImport(eq("수입/지출"), eq("2026년"), eq(FILE_NAME),
                    eq("수입 2건 3,000원, 지출 0건 0원"), eq(List.of(description, description)));
        }

        @Test
        @DisplayName("백업한 뒤에 활동 기록이 새로 생겼으면 반영하지 않고 백업을 다시 받게 한다")
        void importRecords_BackupOutdated_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID + 1);

            // When & Then
            assertThatThrownBy(() -> recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(BackupOutdatedException.class);
        }

        @Test
        @DisplayName("분석한 뒤에 데이터가 바뀌어 분석 결과가 달라졌으면 반영하지 않는다")
        void importRecords_AnalysisChanged_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            // 분석한 뒤에 다른 메뉴에서 관계자 "홍길동"을 삭제한 상황
            when(contactRepository.findAll()).thenReturn(List.of());

            // When & Then
            assertThatThrownBy(() -> recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(ImportStateChangedException.class);
        }

        @Test
        @DisplayName("분석 결과가 달라졌으면 내역을 하나도 등록하지 않는다")
        void importRecords_AnalysisChanged_CreatesNothing() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(contactRepository.findAll()).thenReturn(List.of());

            // When
            catchThrowable(() -> recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME));

            // Then
            verify(financialRecordService, never()).createFinancialRecordWithoutLog(any());
        }

        @Test
        @DisplayName("오류인 행을 고르면 반영하지 않는다")
        void importRecords_ErrorRowSelected_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(tithe));
            when(contactRepository.findAll()).thenReturn(List.of(hong));
            when(financialRecordRepository.findAllByFiscalYearIdOrderByDateAscIdAsc(FISCAL_YEAR_ID))
                    .thenReturn(List.of());
            RecordImportAnalysisDto analysis = recordImportService.analyze(FISCAL_YEAR_ID, createRequest());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            Set<String> errorRow = Set.of(analysis.getItems().getLast().getKey());

            // When & Then
            assertThatThrownBy(() -> recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(), errorRow,
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 수 없는 행이 선택되었습니다: 4행");
        }

        @Test
        @DisplayName("가져올 행을 하나도 고르지 않으면 반영하지 않는다")
        void importRecords_NothingSelected_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> recordImportService.importRecords(FISCAL_YEAR_ID, createRequest(), Set.of(),
                    "signature", BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 내역을 선택해주세요.");
        }
    }

}
