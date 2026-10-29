package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.budget.service.BudgetService;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
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
class BudgetImportServiceImplTest {

    private static final Long FISCAL_YEAR_ID = 7L;
    private static final long BACKUP_LOG_ID = 10L;
    private static final String FILE_NAME = "예산.xlsx";

    @Mock
    private FiscalYearRepository fiscalYearRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private FinancialRecordRepository financialRecordRepository;
    @Mock
    private BudgetService budgetService;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private EntitySnapshotCapturer snapshotCapturer;

    @InjectMocks
    private BudgetImportServiceImpl budgetImportService;

    @Captor
    private ArgumentCaptor<List<List<ActivityLogFieldDto>>> logItemsCaptor;

    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(FISCAL_YEAR_ID)
            .name("2026년")
            .build();

    /**
     * 파일 내용: 수입 예산 "헌금" 아래에 "십일조"(1,000원). 열 구성은 "구분 | 1단계 | 2단계 | 금액"이다.
     */
    private BudgetImportRequestDto.BudgetImportRequestDtoBuilder createTitheRequest() {
        return BudgetImportRequestDto.builder()
                .rows(List.of(
                        Arrays.asList("수입", "헌금", null, null),
                        Arrays.asList("수입", "헌금", "십일조", 1000L)))
                .firstRowNumber(2)
                .typeColumn(0)
                .levelColumns(List.of(1, 2))
                .amountColumn(3);
    }

    private Budget createBudget(Long id, String name, Long amount) {
        return Budget.builder()
                .id(id)
                .fiscalYear(fiscalYear)
                .type(BudgetType.INCOME)
                .name(name)
                .amount(amount)
                .children(new ArrayList<>())
                .build();
    }

    /**
     * 고를 수 있는 건을 모두 고른다.
     */
    private Set<String> selectableKeys(BudgetImportAnalysisDto analysis) {
        return analysis.getItems()
                .stream()
                .filter(BudgetImportItemDto::isSelectable)
                .map(BudgetImportItemDto::getKey)
                .collect(Collectors.toSet());
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("회계 연도에 이미 있는 예산과 견주어 분석한 결과를 반환한다")
        void analyze_ExistingBudget_ReturnsAnalysisAgainstCurrentData() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createBudget(1L, "헌금", null)));

            // When
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());

            // Then
            assertThat(analysis.getItems())
                    .extracting(BudgetImportItemDto::getExistingBudgetId)
                    .containsExactly(1L, null);
        }

        @Test
        @DisplayName("마감된 회계 연도에는 예산을 가져올 수 없다")
        void analyze_ClosedFiscalYear_Failure() {
            // Given
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetImportService.analyze(FISCAL_YEAR_ID, createTitheRequest().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도에는 예산을 가져올 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도에는 예산을 가져올 수 없다")
        void analyze_FiscalYearNotFound_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> budgetImportService.analyze(FISCAL_YEAR_ID, createTitheRequest().build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }
    }

    @Nested
    @DisplayName("importBudgets 테스트")
    class ImportBudgetsTest {

        @Test
        @DisplayName("상위 예산을 먼저 추가하고, 하위 예산은 방금 추가한 상위 예산 아래에 추가한다")
        void importBudgets_NewHierarchy_CreatesChildUnderCreatedParent() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "헌금", 0L, BudgetType.INCOME))
                    .thenReturn(createBudget(100L, "헌금", null));
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, 100L, "십일조", 1000L, BudgetType.INCOME))
                    .thenReturn(createBudget(101L, "십일조", 1000L));

            // When
            budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(budgetService).createBudgetWithoutLog(FISCAL_YEAR_ID, 100L, "십일조", 1000L, BudgetType.INCOME);
        }

        @Test
        @DisplayName("추가한 건수와 갱신한 건수를 반환한다")
        void importBudgets_NewHierarchy_ReturnsCounts() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "헌금", 0L, BudgetType.INCOME))
                    .thenReturn(createBudget(100L, "헌금", null));
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, 100L, "십일조", 1000L, BudgetType.INCOME))
                    .thenReturn(createBudget(101L, "십일조", 1000L));

            // When
            ImportResultDto result = budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            assertThat(result)
                    .extracting(ImportResultDto::getCreatedCount, ImportResultDto::getUpdatedCount)
                    .containsExactly(2, 0);
        }

        @Test
        @DisplayName("활동 기록에 가져오기 한 건으로 남기고, 추가한 예산마다 건별 내용을 담는다")
        void importBudgets_NewHierarchy_LogsOneImportWithItems() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "헌금", 0L, BudgetType.INCOME))
                    .thenReturn(createBudget(100L, "헌금", null));
            when(budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, 100L, "십일조", 1000L, BudgetType.INCOME))
                    .thenReturn(createBudget(101L, "십일조", 1000L));
            List<ActivityLogFieldDto> description = List.of(ActivityLogFieldDto.builder()
                    .name("예산명")
                    .value("예산")
                    .build());
            when(activityLogService.describeCreation(any(Budget.class))).thenReturn(description);

            // When
            budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(), selectableKeys(analysis),
                    analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            verify(activityLogService).logImport(eq("예산"), eq("2026년"), eq(FILE_NAME), eq("추가 2건, 금액 갱신 0건"),
                    logItemsCaptor.capture());
            assertThat(logItemsCaptor.getValue()).containsExactly(description, description);
        }

        @Test
        @DisplayName("금액 갱신 대상은 기존 예산의 이름을 그대로 두고 금액만 파일의 값으로 바꾼다")
        void importBudgets_UpdateAmount_UpdatesExistingBudgetAmount() {
            // Given
            Budget offering = createBudget(1L, "헌금", null);
            Budget tithe = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("십일조")
                    .amount(500L)
                    .parent(offering)
                    .build();
            offering.getChildren().add(tithe);
            BudgetImportRequestDto request = createTitheRequest()
                    .updateExistingAmounts(true)
                    .build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<Budget> snapshot = mock(EntitySnapshot.class);

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of(offering, tithe));
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID, request);
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            when(budgetRepository.findById(2L)).thenReturn(Optional.of(tithe));
            when(snapshotCapturer.capture(tithe)).thenReturn(snapshot);
            when(budgetService.updateBudgetWithoutLog(2L, "십일조", 1000L)).thenReturn(tithe);

            // When
            ImportResultDto result = budgetImportService.importBudgets(FISCAL_YEAR_ID, request,
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME);

            // Then
            assertThat(result)
                    .extracting(ImportResultDto::getCreatedCount, ImportResultDto::getUpdatedCount)
                    .containsExactly(0, 1);
        }

        @Test
        @DisplayName("백업한 뒤에 활동 기록이 새로 생겼으면 반영하지 않고 백업을 다시 받게 한다")
        void importBudgets_BackupOutdated_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID + 1);

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(BackupOutdatedException.class);
        }

        @Test
        @DisplayName("분석한 뒤에 데이터가 바뀌어 분석 결과가 달라졌으면 반영하지 않는다")
        void importBudgets_AnalysisChanged_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            // 분석한 뒤에 다른 메뉴에서 "헌금" 예산을 등록한 상황
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createBudget(1L, "헌금", 0L)));

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(ImportStateChangedException.class);
        }

        @Test
        @DisplayName("분석 결과가 달라졌으면 예산을 하나도 추가하지 않는다")
        void importBudgets_AnalysisChanged_CreatesNothing() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            // 분석한 뒤에 다른 메뉴에서 "헌금" 예산을 등록한 상황
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createBudget(1L, "헌금", 0L)));

            // When
            catchThrowable(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    selectableKeys(analysis), analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME));

            // Then
            verify(budgetService, never()).createBudgetWithoutLog(any(), any(), any(), anyLong(), any());
        }

        @Test
        @DisplayName("새로 추가되는 상위 예산을 빼고 하위 예산만 고르면 반영하지 않는다")
        void importBudgets_ChildSelectedWithoutNewParent_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID)).thenReturn(List.of());
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            Set<String> onlyChild = Set.of(analysis.getItems().getLast().getKey());

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    onlyChild, analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상위 예산을 함께 선택해야 합니다: 헌금 > 십일조");
        }

        @Test
        @DisplayName("가져올 수 없는 건(이미 있는 예산)을 고르면 반영하지 않는다")
        void importBudgets_UnselectableItemSelected_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findAllByFiscalYearId(FISCAL_YEAR_ID))
                    .thenReturn(List.of(createBudget(1L, "헌금", null)));
            BudgetImportAnalysisDto analysis = budgetImportService.analyze(FISCAL_YEAR_ID,
                    createTitheRequest().build());
            when(activityLogService.getLatestLogId()).thenReturn(BACKUP_LOG_ID);
            Set<String> existingParent = Set.of(analysis.getItems().getFirst().getKey());

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    existingParent, analysis.getSignature(), BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 수 없는 예산이 선택되었습니다: 헌금");
        }

        @Test
        @DisplayName("가져올 건을 하나도 고르지 않으면 반영하지 않는다")
        void importBudgets_NothingSelected_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    Set.of(), "signature", BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("가져올 예산을 선택해주세요.");
        }

        @Test
        @DisplayName("마감된 회계 연도에는 예산을 가져올 수 없다")
        void importBudgets_ClosedFiscalYear_Failure() {
            // Given
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetImportService.importBudgets(FISCAL_YEAR_ID, createTitheRequest().build(),
                    Set.of("key"), "signature", BACKUP_LOG_ID, FILE_NAME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도에는 예산을 가져올 수 없습니다.");
        }
    }

}
