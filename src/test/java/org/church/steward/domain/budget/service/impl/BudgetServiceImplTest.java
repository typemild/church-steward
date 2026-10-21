package org.church.steward.domain.budget.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.dto.BudgetAutocompleteDto;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.mapper.BudgetMapper;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.BudgetAmountProjection;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.repository.FiscalYearRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BudgetServiceImplTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private FinancialRecordRepository financialRecordRepository;
    @Mock
    private FiscalYearRepository fiscalYearRepository;
    @Mock
    private BudgetMapper budgetMapper;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private EntitySnapshotCapturer snapshotCapturer;

    @InjectMocks
    private BudgetServiceImpl budgetService;

    private static final Long FISCAL_YEAR_ID = 7L;

    /** 테스트의 예산들이 속한, 마감되지 않은 회계 연도 */
    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(FISCAL_YEAR_ID)
            .name("2024년")
            .build();

    /**
     * 마감된 회계 연도에 속한 예산을 만든다.
     */
    private Budget createBudgetOfClosedFiscalYear(Long id) {
        FiscalYear closedFiscalYear = FiscalYear.builder()
                .id(8L)
                .name("2023년")
                .build();
        closedFiscalYear.close(0L, 0L);

        return Budget.builder()
                .id(id)
                .fiscalYear(closedFiscalYear)
                .type(BudgetType.INCOME)
                .name("마감된 예산")
                .amount(1000L)
                .sortOrder(1)
                .build();
    }

    @Nested
    @DisplayName("getBudgetsByFiscalYearAndType 테스트")
    class GetBudgetsByFiscalYearAndTypeTest {

        @Test
        @DisplayName("수입 예산 목록 조회 성공")
        void getBudgetsByFiscalYearAndType_Income_Success() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childIncomeBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 자식")
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            BudgetDto parentDto = BudgetDto.builder().id(1L).build();
            BudgetDto childDto = BudgetDto.builder().id(2L).build();

            when(budgetRepository.findAllByFiscalYearIdAndType(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(Arrays.asList(parentIncomeBudget, childIncomeBudget));
            when(budgetMapper.toDto(parentIncomeBudget)).thenReturn(parentDto);
            when(budgetMapper.toDto(childIncomeBudget)).thenReturn(childDto);

            // When
            List<BudgetDto> result = budgetService.getBudgetsByFiscalYearAndType(FISCAL_YEAR_ID, BudgetType.INCOME);

            // Then
            assertThat(result).containsExactly(parentDto, childDto);
        }

        @Test
        @DisplayName("지출 예산 목록 조회 성공")
        void getBudgetsByFiscalYearAndType_Expense_Success() {
            // Given
            Budget parentExpenseBudget = Budget.builder()
                    .id(3L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("지출 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childExpenseBudget = Budget.builder()
                    .id(4L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("지출 자식")
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            BudgetDto parentDto = BudgetDto.builder().id(3L).build();
            BudgetDto childDto = BudgetDto.builder().id(4L).build();

            when(budgetRepository.findAllByFiscalYearIdAndType(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(Arrays.asList(parentExpenseBudget, childExpenseBudget));
            when(budgetMapper.toDto(parentExpenseBudget)).thenReturn(parentDto);
            when(budgetMapper.toDto(childExpenseBudget)).thenReturn(childDto);

            // When
            List<BudgetDto> result = budgetService.getBudgetsByFiscalYearAndType(FISCAL_YEAR_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result).containsExactly(parentDto, childDto);
        }
    }

    @Nested
    @DisplayName("getBudgetsWithActualAmount 테스트")
    class GetBudgetsWithActualAmountTest {

        private BudgetAmountProjection amountOf(Long budgetId, Long amount) {
            BudgetAmountProjection projection = mock(BudgetAmountProjection.class);
            when(projection.getBudgetId()).thenReturn(budgetId);
            when(projection.getAmount()).thenReturn(amount);
            return projection;
        }

        @Test
        @DisplayName("말단 예산에는 그 예산에 등록된 내역의 금액 합계를 담는다")
        void getBudgetsWithActualAmount_Leaf_MapsOwnAmount() {
            // Given
            Budget leaf = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("지출")
                    .amount(1000L)
                    .build();
            BudgetDto leafDto = BudgetDto.builder().id(1L).actualAmount(300L).build();
            List<BudgetAmountProjection> amounts = List.of(amountOf(1L, 300L));

            when(financialRecordRepository.sumAmountGroupByBudget(FISCAL_YEAR_ID, FinancialRecordType.EXPENSE))
                    .thenReturn(amounts);
            when(budgetRepository.findAllByFiscalYearIdAndType(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(List.of(leaf));
            when(budgetMapper.toDto(leaf, 300L)).thenReturn(leafDto);

            // When
            List<BudgetDto> result = budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result).containsExactly(leafDto);
        }

        @Test
        @DisplayName("내역이 없는 예산에는 0을 담는다")
        void getBudgetsWithActualAmount_NoRecords_MapsZero() {
            // Given
            Budget leaf = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입")
                    .amount(1000L)
                    .build();
            BudgetDto leafDto = BudgetDto.builder().id(1L).actualAmount(0L).build();

            when(financialRecordRepository.sumAmountGroupByBudget(FISCAL_YEAR_ID, FinancialRecordType.INCOME))
                    .thenReturn(List.of());
            when(budgetRepository.findAllByFiscalYearIdAndType(FISCAL_YEAR_ID, BudgetType.INCOME))
                    .thenReturn(List.of(leaf));
            when(budgetMapper.toDto(leaf, 0L)).thenReturn(leafDto);

            // When
            List<BudgetDto> result = budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.INCOME);

            // Then
            assertThat(result).containsExactly(leafDto);
        }

        @Test
        @DisplayName("상위 예산에는 하위 예산들에 등록된 내역의 금액을 모두 더해 담는다")
        void getBudgetsWithActualAmount_Parent_SumsDescendants() {
            // Given
            Budget parent = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("상위")
                    .build();
            Budget child1 = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("하위 1")
                    .parent(parent)
                    .amount(500L)
                    .build();
            Budget child2 = Budget.builder()
                    .id(3L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.EXPENSE)
                    .name("하위 2")
                    .parent(parent)
                    .amount(500L)
                    .build();
            parent.getChildren().add(child1);
            parent.getChildren().add(child2);

            BudgetDto parentDto = BudgetDto.builder().id(1L).actualAmount(500L).build();
            BudgetDto child1Dto = BudgetDto.builder().id(2L).actualAmount(300L).build();
            BudgetDto child2Dto = BudgetDto.builder().id(3L).actualAmount(200L).build();
            List<BudgetAmountProjection> amounts = List.of(amountOf(2L, 300L), amountOf(3L, 200L));

            when(financialRecordRepository.sumAmountGroupByBudget(FISCAL_YEAR_ID, FinancialRecordType.EXPENSE))
                    .thenReturn(amounts);
            when(budgetRepository.findAllByFiscalYearIdAndType(FISCAL_YEAR_ID, BudgetType.EXPENSE))
                    .thenReturn(Arrays.asList(parent, child1, child2));
            when(budgetMapper.toDto(parent, 500L)).thenReturn(parentDto);
            when(budgetMapper.toDto(child1, 300L)).thenReturn(child1Dto);
            when(budgetMapper.toDto(child2, 200L)).thenReturn(child2Dto);

            // When
            List<BudgetDto> result = budgetService.getBudgetsWithActualAmount(FISCAL_YEAR_ID, BudgetType.EXPENSE);

            // Then
            assertThat(result).containsExactly(parentDto, child1Dto, child2Dto);
        }
    }

    @Nested
    @DisplayName("createBudget 테스트")
    class CreateBudgetTest {

        @Test
        @DisplayName("부모 없이 예산 추가 성공 - 저장 확인")
        void createBudget_NoParent_SaveSuccess() {
            // Given
            String name = "새 예산";
            long amount = 100L;
            BudgetType type = BudgetType.INCOME;
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, null, name, amount, type);

            // Then
            verify(budgetRepository, times(1)).save(any(Budget.class));
        }

        @Test
        @DisplayName("추가한 예산은 지정한 회계 연도에 속한다")
        void createBudget_NoParent_BelongsToFiscalYear() {
            // Given
            ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, null, "새 예산", 100L, BudgetType.INCOME);

            // Then
            verify(budgetRepository).save(budgetCaptor.capture());
            assertThat(budgetCaptor.getValue().getFiscalYear()).isSameAs(fiscalYear);
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도에는 예산을 추가할 수 없다")
        void createBudget_FiscalYearNotFound_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, null, "새 예산", 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도에는 예산을 추가할 수 없다")
        void createBudget_ClosedFiscalYear_Failure() {
            // Given
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, null, "새 예산", 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("예산명이 비어 있으면 예산을 추가할 수 없다")
        void createBudget_BlankName_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, null, " ", 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명을 입력해주세요.");
        }

        @Test
        @DisplayName("예산명이 50자를 넘으면 예산을 추가할 수 없다")
        void createBudget_NameTooLong_Failure() {
            // Given
            String name = "가".repeat(51);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, null, name, 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명은 50자 이내로 입력해주세요.");
        }

        @Test
        @DisplayName("예산명이 정확히 50자이면 예산을 추가할 수 있다")
        void createBudget_NameOfMaxLength_SaveSuccess() {
            // Given
            String name = "가".repeat(50);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, null, name, 100L, BudgetType.INCOME);

            // Then
            verify(budgetRepository, times(1)).save(any(Budget.class));
        }

        @Test
        @DisplayName("예산명에 '<'가 있으면 예산을 추가할 수 없다")
        void createBudget_NameWithLessThanSign_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, null, "헌금<일반", 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명에는 '<', '>' 문자를 사용할 수 없습니다.");
        }

        @Test
        @DisplayName("예산명에 '>'가 있으면 예산을 추가할 수 없다")
        void createBudget_NameWithGreaterThanSign_Failure() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(
                    () -> budgetService.createBudget(FISCAL_YEAR_ID, null, "헌금 > 일반", 100L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명에는 '<', '>' 문자를 사용할 수 없습니다.");
        }

        @Test
        @DisplayName("상위 예산이 다른 회계 연도의 예산이면 추가 실패")
        void createBudget_WithParent_Failure_ParentOfOtherFiscalYear() {
            // Given
            Budget parentOfOtherFiscalYear = Budget.builder()
                    .id(1L)
                    .fiscalYear(FiscalYear.builder().id(9L).name("2025년").build())
                    .type(BudgetType.INCOME)
                    .name("다른 회계 연도의 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentOfOtherFiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("상위 예산과 같은 회계 연도의 예산만 추가할 수 있습니다.");
        }

        @Test
        @DisplayName("부모 없이 예산 추가 성공 - 로그 기록 확인")
        void createBudget_NoParent_LogSuccess() {
            // Given
            String name = "새 예산";
            long amount = 100L;
            BudgetType type = BudgetType.INCOME;

            Budget newBudget = Budget.builder()
                    .fiscalYear(fiscalYear)
                    .name(name)
                    .amount(amount)
                    .type(type)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.save(any(Budget.class))).thenReturn(newBudget);

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, null, name, amount, type);

            // Then
            verify(activityLogService).logCreation(any(Budget.class));
        }

        @Test
        @DisplayName("부모 항목에 자식 예산 추가 성공 - 저장 확인")
        void createBudget_WithParent_SaveSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget newBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("자식 예산")
                    .parent(parentIncomeBudget)
                    .amount(50L)
                    .sortOrder(6)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);
            when(budgetRepository.findMaxSortOrder(FISCAL_YEAR_ID, 1L)).thenReturn(5);
            when(budgetRepository.save(any(Budget.class))).thenReturn(newBudget);

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME);

            // Then
            verify(budgetRepository, times(2)).save(any(Budget.class));
        }

        @Test
        @DisplayName("부모 항목에 자식 예산 추가 성공 - 부모 금액이 null로 변경됨을 확인")
        void createBudget_WithParent_ParentAmountBecomesNull_Success() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);
            when(budgetRepository.findMaxSortOrder(FISCAL_YEAR_ID, 1L)).thenReturn(5);

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME);

            // Then
            assertThat(parentIncomeBudget.getAmount()).isNull();
        }

        @Test
        @DisplayName("부모 항목에 자식 예산 추가 성공 - 로그 기록 확인")
        void createBudget_WithParent_LogSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget newBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("자식 예산")
                    .parent(parentIncomeBudget)
                    .amount(50L)
                    .sortOrder(6)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);
            when(budgetRepository.findMaxSortOrder(FISCAL_YEAR_ID, 1L)).thenReturn(5);
            when(budgetRepository.save(any(Budget.class))).thenReturn(newBudget);

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME);

            // Then
            verify(activityLogService).logCreation(any(Budget.class));
        }

        @Test
        @DisplayName("부모 항목에 수입/지출 내역이 있으면 자식 추가 실패")
        void createBudget_WithParent_Failure_HasTransactions() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("부모와 자식의 유형이 다르면 추가 실패")
        void createBudget_WithParent_Failure_TypeMismatch() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.EXPENSE))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("부모 항목에 자식 예산 추가 시 sortOrder가 maxSortOrder + 1로 설정됨을 확인")
        void createBudget_WithMaxSortOrder_Success() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);
            when(budgetRepository.findMaxSortOrder(FISCAL_YEAR_ID, 1L)).thenReturn(5);

            ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);

            // When
            budgetService.createBudget(FISCAL_YEAR_ID, 1L, "자식 예산", 50L, BudgetType.INCOME);

            // Then
            verify(budgetRepository, times(2)).save(budgetCaptor.capture());
            List<Budget> savedBudgetList = budgetCaptor.getAllValues();
            assertThat(savedBudgetList.get(0)).isSameAs(parentIncomeBudget);
            Budget savedBudget = savedBudgetList.get(1);
            assertThat(savedBudget.getSortOrder()).isEqualTo(6);
        }
    }

    @Nested
    @DisplayName("createBudgetWithoutLog 테스트")
    class CreateBudgetWithoutLogTest {

        @Test
        @DisplayName("저장한 예산을 반환한다")
        void createBudgetWithoutLog_NoParent_ReturnsSavedBudget() {
            // Given
            Budget saved = Budget.builder()
                    .id(10L)
                    .build();
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.save(any(Budget.class))).thenReturn(saved);

            // When
            Budget result = budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "새 예산", 100L,
                    BudgetType.INCOME);

            // Then
            assertThat(result).isSameAs(saved);
        }

        @Test
        @DisplayName("활동 기록을 남기지 않는다")
        void createBudgetWithoutLog_NoParent_DoesNotLog() {
            // Given
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When
            budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "새 예산", 100L, BudgetType.INCOME);

            // Then
            verify(activityLogService, never()).logCreation(any());
        }

        @Test
        @DisplayName("예산을 추가할 때와 같은 검증을 한다 (마감된 회계 연도에는 추가할 수 없다)")
        void createBudgetWithoutLog_ClosedFiscalYear_Failure() {
            // Given
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> budgetService.createBudgetWithoutLog(FISCAL_YEAR_ID, null, "새 예산", 100L,
                    BudgetType.INCOME))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("updateBudgetWithoutLog 테스트")
    class UpdateBudgetWithoutLogTest {

        private Budget createLeafBudget() {
            return Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();
        }

        @Test
        @DisplayName("예산의 이름과 금액을 바꾸고 그 예산을 반환한다")
        void updateBudgetWithoutLog_Leaf_UpdatesAndReturnsBudget() {
            // Given
            Budget budget = createLeafBudget();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // When
            Budget result = budgetService.updateBudgetWithoutLog(1L, "수정된 이름", 2000L);

            // Then
            assertThat(result)
                    .extracting(Budget::getName, Budget::getAmount)
                    .containsExactly("수정된 이름", 2000L);
        }

        @Test
        @DisplayName("활동 기록을 남기지 않는다")
        void updateBudgetWithoutLog_Leaf_DoesNotLog() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createLeafBudget()));

            // When
            budgetService.updateBudgetWithoutLog(1L, "수정된 이름", 2000L);

            // Then
            verify(activityLogService, never()).logUpdate(any(), any());
        }

        @Test
        @DisplayName("예산을 수정할 때와 같은 검증을 한다 (마감된 회계 연도의 예산은 수정할 수 없다)")
        void updateBudgetWithoutLog_ClosedFiscalYear_Failure() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createBudgetOfClosedFiscalYear(1L)));

            // When & Then
            assertThatThrownBy(() -> budgetService.updateBudgetWithoutLog(1L, "수정된 이름", 2000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("updateBudget 테스트")
    class UpdateBudgetTest {

        @Test
        @DisplayName("마감된 회계 연도의 예산은 수정할 수 없다")
        void updateBudget_ClosedFiscalYear_Failure() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createBudgetOfClosedFiscalYear(1L)));

            // When & Then
            assertThatThrownBy(() -> budgetService.updateBudget(1L, "수정된 이름", 2000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("예산명에 '>'가 있으면 예산을 수정할 수 없다")
        void updateBudget_NameWithGreaterThanSign_Failure() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // When & Then
            assertThatThrownBy(() -> budgetService.updateBudget(1L, "헌금 > 일반", 1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명에는 '<', '>' 문자를 사용할 수 없습니다.");
        }

        @Test
        @DisplayName("예산명이 비어 있으면 예산을 수정할 수 없다")
        void updateBudget_BlankName_Failure() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));

            // When & Then
            assertThatThrownBy(() -> budgetService.updateBudget(1L, "", 1000L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("예산명을 입력해주세요.");
        }

        @Test
        @DisplayName("하위 항목이 없는 예산 이름 변경 성공 - 상태 검증")
        void updateBudget_NoChildren_NameUpdateSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            @SuppressWarnings("unchecked")
            EntitySnapshot<Budget> snapshot = mock(EntitySnapshot.class);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(snapshotCapturer.capture(parentIncomeBudget)).thenReturn(snapshot);

            // When
            budgetService.updateBudget(1L, "수정된 이름", 2000L);

            // Then
            assertThat(parentIncomeBudget.getName()).isEqualTo("수정된 이름");
        }

        @Test
        @DisplayName("하위 항목이 없는 예산 금액 변경 성공 - 상태 검증")
        void updateBudget_NoChildren_AmountUpdateSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            @SuppressWarnings("unchecked")
            EntitySnapshot<Budget> snapshot = mock(EntitySnapshot.class);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(snapshotCapturer.capture(parentIncomeBudget)).thenReturn(snapshot);

            // When
            budgetService.updateBudget(1L, "수정된 이름", 2000L);

            // Then
            assertThat(parentIncomeBudget.getTotalAmount()).isEqualTo(2000L);
        }

        @Test
        @DisplayName("하위 항목이 없는 예산 금액 수정 성공 - 저장 행위 검증")
        void updateBudget_NoChildren_SaveSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            @SuppressWarnings("unchecked")
            EntitySnapshot<Budget> snapshot = mock(EntitySnapshot.class);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(snapshotCapturer.capture(parentIncomeBudget)).thenReturn(snapshot);

            // When
            budgetService.updateBudget(1L, "수정된 이름", 2000L);

            // Then
            verify(budgetRepository).save(parentIncomeBudget);
        }

        @Test
        @DisplayName("하위 항목이 없는 예산 금액 수정 시 수정 로그가 기록된다 - 로그 기록 행위 검증")
        void updateBudget_NoChildren_LogSuccess() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            @SuppressWarnings("unchecked")
            EntitySnapshot<Budget> snapshot = mock(EntitySnapshot.class);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));
            when(snapshotCapturer.capture(parentIncomeBudget)).thenReturn(snapshot);

            // When
            budgetService.updateBudget(1L, "수정된 이름", 2000L);

            // Then
            verify(activityLogService).logUpdate(snapshot, parentIncomeBudget);
        }

        @Test
        @DisplayName("하위 항목이 있는 예산의 금액 수정 시도 시 실패")
        void updateBudget_HasChildren_Failure() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childIncomeBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 자식")
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            parentIncomeBudget.setAmount(null);
            parentIncomeBudget.getChildren().add(childIncomeBudget);

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentIncomeBudget));

            // When & Then
            assertThatThrownBy(() -> budgetService.updateBudget(1L, "수정된 이름", 2000L))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("deleteBudget 테스트")
    class DeleteBudgetTest {

        @Test
        @DisplayName("마감된 회계 연도의 예산은 삭제할 수 없다")
        void deleteBudget_ClosedFiscalYear_Failure() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createBudgetOfClosedFiscalYear(1L)));

            // When & Then
            assertThatThrownBy(() -> budgetService.deleteBudget(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("제약 조건이 없는 예산 삭제 성공 - 삭제 행위 검증")
        void deleteBudget_Success_DeleteById() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByParentId(1L)).thenReturn(false);
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);

            // When
            budgetService.deleteBudget(1L);

            // Then
            verify(budgetRepository).deleteById(1L);
        }

        @Test
        @DisplayName("제약 조건이 없는 예산 삭제 시 삭제 로그가 기록된다 - 로그 기록 행위 검증")
        void deleteBudget_Success_LogSuccess() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByParentId(1L)).thenReturn(false);
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(false);

            // When
            budgetService.deleteBudget(1L);

            // Then
            verify(activityLogService).logDeletion(any(Budget.class));
        }

        @Test
        @DisplayName("하위 예산이 존재하는 경우 삭제 실패")
        void deleteBudget_Failure_HasChildren() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByParentId(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> budgetService.deleteBudget(1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("수입/지출 내역이 존재하는 경우 삭제 실패")
        void deleteBudget_Failure_HasTransactions() {
            // Given
            Budget budget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
            when(budgetRepository.existsByParentId(1L)).thenReturn(false);
            when(financialRecordRepository.existsByBudgetId(1L)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> budgetService.deleteBudget(1L))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("하위 예산이 마지막 남은 자식인 경우 삭제 시 부모의 금액을 0으로 설정한다 - 상태 검증")
        void deleteBudget_Success_SetParentAmountToZero() {
            // Given
            Budget parentBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("부모 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("자식 예산")
                    .parent(parentBudget)
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(2L)).thenReturn(Optional.of(childBudget));
            when(budgetRepository.existsByParentId(2L)).thenReturn(false);
            when(financialRecordRepository.existsByBudgetId(2L)).thenReturn(false);
            when(budgetRepository.existsByParentId(1L)).thenReturn(false);

            // When
            budgetService.deleteBudget(2L);

            // Then
            assertThat(parentBudget.getAmount()).isEqualTo(0L);
        }

        @Test
        @DisplayName("하위 예산이 마지막 남은 자식인 경우 삭제 시 삭제 로그가 기록된다 - 로그 기록 행위 검증")
        void deleteBudget_Success_SetParentAmountToZero_LogSuccess() {
            // Given
            Budget parentBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("부모 예산")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childBudget = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("자식 예산")
                    .parent(parentBudget)
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            when(budgetRepository.findById(2L)).thenReturn(Optional.of(childBudget));
            when(budgetRepository.existsByParentId(2L)).thenReturn(false);
            when(financialRecordRepository.existsByBudgetId(2L)).thenReturn(false);
            when(budgetRepository.existsByParentId(1L)).thenReturn(false);

            // When
            budgetService.deleteBudget(2L);

            // Then
            verify(activityLogService).logDeletion(any(Budget.class));
        }
    }

    @Nested
    @DisplayName("moveBudgetUp 테스트")
    class MoveBudgetUpTest {

        @Test
        @DisplayName("마감된 회계 연도의 예산은 위로 이동할 수 없다")
        void moveBudgetUp_ClosedFiscalYear_Failure() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createBudgetOfClosedFiscalYear(1L)));

            // When & Then
            assertThatThrownBy(() -> budgetService.moveBudgetUp(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("예산 항목 위로 이동 성공 - 정렬 순서 변경 확인 - 상태 검증")
        void moveBudgetUp_Success_SortOrderUpdate() {
            // Given
            Budget budget1 = Budget.builder().id(1L).fiscalYear(fiscalYear).sortOrder(1).build();
            Budget budget2 = Budget.builder().id(2L).fiscalYear(fiscalYear).sortOrder(2).build();
            when(budgetRepository.findById(2L)).thenReturn(Optional.of(budget2));
            when(budgetRepository.findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(FISCAL_YEAR_ID, null))
                    .thenReturn(Arrays.asList(budget1, budget2));

            // When
            budgetService.moveBudgetUp(2L);

            // Then
            assertThat(budget2.getSortOrder()).isEqualTo(1);
            assertThat(budget1.getSortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("예산 항목 위로 이동 시 최상단인 경우 이동하지 않는다 - 행위 검증")
        void moveBudgetUp_Failure_AtTop() {
            // Given
            Budget budget1 = Budget.builder().id(1L).fiscalYear(fiscalYear).sortOrder(1).build();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget1));
            when(budgetRepository.findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(FISCAL_YEAR_ID, null))
                    .thenReturn(List.of(budget1));

            // When
            budgetService.moveBudgetUp(1L);

            // Then
            verify(budgetRepository, times(0)).save(any(Budget.class));
        }
    }

    @Nested
    @DisplayName("moveBudgetDown 테스트")
    class MoveBudgetDownTest {

        @Test
        @DisplayName("마감된 회계 연도의 예산은 아래로 이동할 수 없다")
        void moveBudgetDown_ClosedFiscalYear_Failure() {
            // Given
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(createBudgetOfClosedFiscalYear(1L)));

            // When & Then
            assertThatThrownBy(() -> budgetService.moveBudgetDown(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 예산은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("예산 항목 아래로 이동 성공 - 정렬 순서 변경 확인 - 상태 검증")
        void moveBudgetDown_Success_SortOrderUpdate() {
            // Given
            Budget budget1 = Budget.builder().id(1L).fiscalYear(fiscalYear).sortOrder(1).build();
            Budget budget2 = Budget.builder().id(2L).fiscalYear(fiscalYear).sortOrder(2).build();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget1));
            when(budgetRepository.findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(FISCAL_YEAR_ID, null))
                    .thenReturn(Arrays.asList(budget1, budget2));

            // When
            budgetService.moveBudgetDown(1L);

            // Then
            assertThat(budget2.getSortOrder()).isEqualTo(1);
            assertThat(budget1.getSortOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("예산 항목 아래로 이동 시 최하단인 경우 이동하지 않는다 - 행위 검증")
        void moveBudgetDown_Failure_AtBottom() {
            // Given
            Budget budget1 = Budget.builder().id(1L).fiscalYear(fiscalYear).sortOrder(1).build();
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget1));
            when(budgetRepository.findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(FISCAL_YEAR_ID, null))
                    .thenReturn(List.of(budget1));

            // When
            budgetService.moveBudgetDown(1L);

            // Then
            verify(budgetRepository, times(0)).save(any(Budget.class));
        }
    }

    @Nested
    @DisplayName("getBudgetsForAutocomplete 테스트")
    class GetBudgetsForAutocompleteTest {

        @Test
        @DisplayName("자동완성 예산 목록 조회 성공 (하위 항목의 리프 노드들 반환) - 상태 검증")
        void getBudgetsForAutocomplete_Success() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget childWithParent = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 자식")
                    .parent(parentIncomeBudget)
                    .amount(500L)
                    .sortOrder(1)
                    .build();

            parentIncomeBudget.getChildren().add(childWithParent);
            when(budgetRepository.findAllByFiscalYearIdAndTypeAndNameContaining(FISCAL_YEAR_ID, BudgetType.INCOME,
                    "수입")).thenReturn(List.of(parentIncomeBudget));

            BudgetAutocompleteDto expectedDto = BudgetAutocompleteDto.builder()
                    .id(2L)
                    .name("수입 자식")
                    .hierarchyName("수입 부모 > 수입 자식")
                    .build();
            when(budgetMapper.toAutocompleteDto(childWithParent)).thenReturn(expectedDto);

            // When
            List<BudgetAutocompleteDto> result = budgetService.getBudgetsForAutocomplete(FISCAL_YEAR_ID,
                    BudgetType.INCOME, "수입", 10);

            // Then
            assertThat(result).containsExactly(expectedDto);
        }

        @Test
        @DisplayName("자동완성 예산 목록 조회 성공 (검색 결과가 없는 경우) - 상태 검증")
        void getBudgetsForAutocomplete_NoMatch_Success() {
            // Given
            when(budgetRepository.findAllByFiscalYearIdAndTypeAndNameContaining(FISCAL_YEAR_ID, BudgetType.INCOME,
                    "없는")).thenReturn(List.of());

            // When
            List<BudgetAutocompleteDto> result = budgetService.getBudgetsForAutocomplete(FISCAL_YEAR_ID,
                    BudgetType.INCOME, "없는", 10);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("자동완성 예산 목록 조회 성공 (제한된 개수만큼 반환) - 상태 검증")
        void getBudgetsForAutocomplete_Limit_Success() {
            // Given
            Budget parentIncomeBudget = Budget.builder()
                    .id(1L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 부모")
                    .amount(1000L)
                    .sortOrder(1)
                    .build();

            Budget child1 = Budget.builder()
                    .id(2L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 자식 1")
                    .parent(parentIncomeBudget)
                    .amount(500L)
                    .sortOrder(1)
                    .build();
            Budget child2 = Budget.builder()
                    .id(5L)
                    .fiscalYear(fiscalYear)
                    .type(BudgetType.INCOME)
                    .name("수입 자식 2")
                    .parent(parentIncomeBudget)
                    .amount(300L)
                    .sortOrder(2)
                    .build();

            parentIncomeBudget.getChildren().add(child1);
            parentIncomeBudget.getChildren().add(child2);

            when(budgetRepository.findAllByFiscalYearIdAndTypeAndNameContaining(FISCAL_YEAR_ID, BudgetType.INCOME,
                    "수입")).thenReturn(List.of(parentIncomeBudget));

            // When
            List<BudgetAutocompleteDto> result = budgetService.getBudgetsForAutocomplete(FISCAL_YEAR_ID,
                    BudgetType.INCOME, "수입", 1);

            // Then
            assertThat(result).hasSize(1);
        }
    }

}
