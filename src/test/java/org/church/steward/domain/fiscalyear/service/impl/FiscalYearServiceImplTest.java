package org.church.steward.domain.fiscalyear.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.church.steward.domain.fiscalyear.mapper.FiscalYearMapper;
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
class FiscalYearServiceImplTest {

    @Mock
    private FiscalYearRepository fiscalYearRepository;

    @Mock
    private FiscalYearMapper fiscalYearMapper;

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private FinancialRecordRepository financialRecordRepository;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private EntitySnapshotCapturer entitySnapshotCapturer;

    @InjectMocks
    private FiscalYearServiceImpl fiscalYearService;

    private static final Long ID = 5L;
    private static final String NAME = "2025년";
    private static final LocalDate START_DATE = LocalDate.of(2024, 12, 1);
    private static final LocalDate END_DATE = LocalDate.of(2025, 11, 30);

    private FiscalYear createFiscalYear(Long id, String name, LocalDate startDate, LocalDate endDate) {
        return FiscalYear.builder()
                .id(id)
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .build();
    }

    /**
     * 총 수입 5,000원, 총 지출 3,000원으로 마감된 회계 연도를 만든다.
     */
    private FiscalYear createClosedFiscalYear(Long id, String name) {
        FiscalYear fiscalYear = createFiscalYear(id, name, START_DATE, END_DATE);
        fiscalYear.close(5_000L, 3_000L);
        return fiscalYear;
    }

    private FinancialRecord createRecord(LocalDate date) {
        return FinancialRecord.builder()
                .date(date)
                .build();
    }

    @Nested
    @DisplayName("listFiscalYears 테스트")
    class ListFiscalYearsTest {

        @Test
        @DisplayName("시작일이 늦은 순서로 조회한 회계 연도를 DTO로 바꿔 반환한다")
        void listFiscalYears_Success() {
            // Given
            FiscalYear fy1 = FiscalYear.builder().id(2L).name("2024년").build();
            FiscalYear fy2 = FiscalYear.builder().id(1L).name("2023년").build();
            FiscalYearDto dto1 = FiscalYearDto.builder().id(2L).name("2024년").build();
            FiscalYearDto dto2 = FiscalYearDto.builder().id(1L).name("2023년").build();

            when(fiscalYearRepository.findAllByOrderByStartDateDesc()).thenReturn(Arrays.asList(fy1, fy2));
            when(fiscalYearMapper.toDto(fy1)).thenReturn(dto1);
            when(fiscalYearMapper.toDto(fy2)).thenReturn(dto2);

            // When
            List<FiscalYearDto> fiscalYears = fiscalYearService.listFiscalYears();

            // Then
            assertThat(fiscalYears).containsExactly(dto1, dto2);
        }
    }

    @Nested
    @DisplayName("createFiscalYear 테스트")
    class CreateFiscalYearTest {

        @Test
        @DisplayName("새로운 회계 연도를 추가하면 입력한 이름과 기간으로 저장한다")
        void createFiscalYear_Success() {
            // Given
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            ArgumentCaptor<FiscalYear> fiscalYearCaptor = ArgumentCaptor.forClass(FiscalYear.class);

            // When
            fiscalYearService.createFiscalYear(NAME, START_DATE, END_DATE);

            // Then
            verify(fiscalYearRepository).save(fiscalYearCaptor.capture());
            FiscalYear saved = fiscalYearCaptor.getValue();
            assertThat(saved.getName()).isEqualTo(NAME);
            assertThat(saved.getStartDate()).isEqualTo(START_DATE);
            assertThat(saved.getEndDate()).isEqualTo(END_DATE);
        }

        @Test
        @DisplayName("새로운 회계 연도는 마감되지 않은 상태로 저장한다")
        void createFiscalYear_SavesAsNotClosed() {
            // Given
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            ArgumentCaptor<FiscalYear> fiscalYearCaptor = ArgumentCaptor.forClass(FiscalYear.class);

            // When
            fiscalYearService.createFiscalYear(NAME, START_DATE, END_DATE);

            // Then
            verify(fiscalYearRepository).save(fiscalYearCaptor.capture());
            assertThat(fiscalYearCaptor.getValue().isClosed()).isFalse();
        }

        @Test
        @DisplayName("이름의 앞뒤 공백은 없애고 저장한다")
        void createFiscalYear_NameWithSpaces_SavesTrimmedName() {
            // Given
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            ArgumentCaptor<FiscalYear> fiscalYearCaptor = ArgumentCaptor.forClass(FiscalYear.class);

            // When
            fiscalYearService.createFiscalYear("  " + NAME + " ", START_DATE, END_DATE);

            // Then
            verify(fiscalYearRepository).save(fiscalYearCaptor.capture());
            assertThat(fiscalYearCaptor.getValue().getName()).isEqualTo(NAME);
        }

        @Test
        @DisplayName("한 해에 두 번째 회계 연도도 이름과 기간이 다르면 추가할 수 있다")
        void createFiscalYear_SecondFiscalYearInSameYear_Success() {
            // Given
            LocalDate startDate = LocalDate.of(2025, 7, 1);
            LocalDate endDate = LocalDate.of(2025, 12, 31);
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, "2025년 하반기", startDate, endDate));
            ArgumentCaptor<FiscalYear> fiscalYearCaptor = ArgumentCaptor.forClass(FiscalYear.class);

            // When
            fiscalYearService.createFiscalYear("2025년 하반기", startDate, endDate);

            // Then
            verify(fiscalYearRepository).save(fiscalYearCaptor.capture());
            assertThat(fiscalYearCaptor.getValue().getName()).isEqualTo("2025년 하반기");
        }

        @Test
        @DisplayName("새로운 회계 연도 추가 시 로그 기록 성공")
        void createFiscalYear_LogCreation_Success() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.save(any(FiscalYear.class))).thenReturn(fiscalYear);

            // When
            fiscalYearService.createFiscalYear(NAME, START_DATE, END_DATE);

            // Then
            verify(activityLogService).logCreation(fiscalYear);
        }

        @Test
        @DisplayName("이름이 비어 있으면 예외 발생")
        void createFiscalYear_BlankName_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear("   ", START_DATE, END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 이름을 입력해주세요.");
        }

        @Test
        @DisplayName("이름이 null이면 예외 발생")
        void createFiscalYear_NullName_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(null, START_DATE, END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 이름을 입력해주세요.");
        }

        @Test
        @DisplayName("이름이 30자를 넘으면 예외 발생")
        void createFiscalYear_TooLongName_ThrowsException() {
            // Given
            String tooLongName = "가".repeat(31);

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(tooLongName, START_DATE, END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 이름은 30자를 넘을 수 없습니다.");
        }

        @Test
        @DisplayName("같은 이름의 회계 연도가 이미 있으면 예외 발생")
        void createFiscalYear_DuplicatedName_ThrowsException() {
            // Given
            when(fiscalYearRepository.existsByName(NAME)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(NAME, START_DATE, END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("같은 이름의 회계 연도가 이미 있습니다.");
        }

        @Test
        @DisplayName("시작일이나 종료일이 없으면 예외 발생")
        void createFiscalYear_NullPeriod_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(NAME, START_DATE, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 시작일과 종료일을 입력해주세요.");
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외 발생")
        void createFiscalYear_StartDateAfterEndDate_ThrowsException() {
            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(NAME, END_DATE, START_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 시작일이 종료일보다 늦습니다.");
        }

        @Test
        @DisplayName("기간이 다른 회계 연도와 겹치면 겹치는 회계 연도를 알려주는 예외 발생")
        void createFiscalYear_PeriodOverlapsAnotherFiscalYear_ThrowsException() {
            // Given
            FiscalYear overlapped = createFiscalYear(4L, "2024년", LocalDate.of(2023, 12, 1),
                    LocalDate.of(2024, 12, 31));
            when(fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(END_DATE, START_DATE))
                    .thenReturn(List.of(overlapped));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.createFiscalYear(NAME, START_DATE, END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("\"2024년\" 회계 연도의 기간(2023-12-01 ~ 2024-12-31)과 겹칩니다.");
        }

        @Test
        @DisplayName("시작일과 종료일이 같은 하루짜리 기간도 추가할 수 있다")
        void createFiscalYear_SingleDayPeriod_Success() {
            // Given
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, START_DATE));

            // When
            fiscalYearService.createFiscalYear(NAME, START_DATE, START_DATE);

            // Then
            verify(fiscalYearRepository).save(any(FiscalYear.class));
        }
    }

    @Nested
    @DisplayName("createFiscalYearWithCopy 테스트")
    class CreateFiscalYearWithCopyTest {

        private static final Long SOURCE_ID = 4L;

        private Budget createRootBudget() {
            return Budget.builder()
                    .id(1L)
                    .name("Root")
                    .amount(1000L)
                    .type(BudgetType.INCOME)
                    .sortOrder(1)
                    .build();
        }

        @Test
        @DisplayName("회계 연도를 복사해 추가하면 입력한 이름과 기간으로 저장한다")
        void createFiscalYearWithCopy_Success() {
            // Given
            Budget rootBudget = createRootBudget();
            Budget childBudget = Budget.builder()
                    .id(2L)
                    .name("Child")
                    .amount(500L)
                    .type(BudgetType.INCOME)
                    .sortOrder(1)
                    .parent(rootBudget)
                    .build();
            ArgumentCaptor<FiscalYear> fiscalYearCaptor = ArgumentCaptor.forClass(FiscalYear.class);

            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            when(budgetRepository.findAllByFiscalYearId(SOURCE_ID)).thenReturn(Arrays.asList(rootBudget, childBudget));

            // When
            fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID);

            // Then
            verify(fiscalYearRepository).save(fiscalYearCaptor.capture());
            FiscalYear saved = fiscalYearCaptor.getValue();
            assertThat(saved.getName()).isEqualTo(NAME);
            assertThat(saved.getStartDate()).isEqualTo(START_DATE);
            assertThat(saved.getEndDate()).isEqualTo(END_DATE);
        }

        @Test
        @DisplayName("회계 연도 복사 시 예산 저장 성공")
        void createFiscalYearWithCopy_BudgetSave_Success() {
            // Given
            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            when(budgetRepository.findAllByFiscalYearId(SOURCE_ID)).thenReturn(List.of(createRootBudget()));

            // When
            fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID);

            // Then
            verify(budgetRepository, atLeastOnce()).save(any(Budget.class));
        }

        @Test
        @DisplayName("복사한 예산은 새 회계 연도에 속하고 원본 예산의 이름, 금액, 유형, 순서를 그대로 가진다")
        void createFiscalYearWithCopy_CopiedBudgetBelongsToNewFiscalYear() {
            // Given
            FiscalYear newFiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            ArgumentCaptor<Budget> budgetCaptor = ArgumentCaptor.forClass(Budget.class);

            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.save(any(FiscalYear.class))).thenReturn(newFiscalYear);
            when(budgetRepository.findAllByFiscalYearId(SOURCE_ID)).thenReturn(List.of(createRootBudget()));

            // When
            fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID);

            // Then
            verify(budgetRepository).save(budgetCaptor.capture());
            Budget copied = budgetCaptor.getValue();
            assertThat(copied.getFiscalYear()).isSameAs(newFiscalYear);
            assertThat(copied.getName()).isEqualTo("Root");
            assertThat(copied.getAmount()).isEqualTo(1000L);
            assertThat(copied.getType()).isEqualTo(BudgetType.INCOME);
            assertThat(copied.getSortOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("원본 회계 연도가 마감되어 있어도 예산을 복사할 수 있다")
        void createFiscalYearWithCopy_ClosedSource_Success() {
            // Given
            // 서비스는 원본 회계 연도의 마감 여부를 보지 않고 존재하는지만 확인한다.
            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.save(any(FiscalYear.class)))
                    .thenReturn(createFiscalYear(ID, NAME, START_DATE, END_DATE));
            when(budgetRepository.findAllByFiscalYearId(SOURCE_ID)).thenReturn(List.of(createRootBudget()));

            // When
            fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID);

            // Then
            verify(budgetRepository).save(any(Budget.class));
        }

        @Test
        @DisplayName("회계 연도 복사 실패 - 같은 이름의 회계 연도가 이미 있을 때")
        void createFiscalYearWithCopy_Failure_DuplicatedName() {
            // Given
            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.existsByName(NAME)).thenReturn(true);

            // When & Then
            assertThatThrownBy(
                    () -> fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("같은 이름의 회계 연도가 이미 있습니다.");
        }

        @Test
        @DisplayName("회계 연도 복사 실패 - 원본 회계 연도가 존재하지 않을 때")
        void createFiscalYearWithCopy_Failure_SourceDoesNotExist() {
            // Given
            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(false);

            // When & Then
            assertThatThrownBy(
                    () -> fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 원본 회계 연도입니다.");
        }

        @Test
        @DisplayName("회계 연도 복사 실패 - 기간이 다른 회계 연도와 겹칠 때")
        void createFiscalYearWithCopy_Failure_PeriodOverlapsAnotherFiscalYear() {
            // Given
            FiscalYear overlapped = createFiscalYear(SOURCE_ID, "2024년", LocalDate.of(2023, 12, 1),
                    LocalDate.of(2024, 12, 31));
            when(fiscalYearRepository.existsById(SOURCE_ID)).thenReturn(true);
            when(fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(END_DATE, START_DATE))
                    .thenReturn(List.of(overlapped));

            // When & Then
            assertThatThrownBy(
                    () -> fiscalYearService.createFiscalYearWithCopy(NAME, START_DATE, END_DATE, SOURCE_ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("\"2024년\" 회계 연도의 기간(2023-12-01 ~ 2024-12-31)과 겹칩니다.");
        }
    }

    @Nested
    @DisplayName("updateFiscalYear 테스트")
    class UpdateFiscalYearTest {

        private static final String NEW_NAME = "2025년 전체";
        private static final LocalDate NEW_START_DATE = LocalDate.of(2025, 1, 1);
        private static final LocalDate NEW_END_DATE = LocalDate.of(2025, 12, 31);

        @Test
        @DisplayName("수정하면 회계 연도의 이름과 시작일, 종료일이 바뀐다")
        void updateFiscalYear_ValidInput_UpdatesNameAndPeriod() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE);

            // Then
            assertThat(fiscalYear.getName()).isEqualTo(NEW_NAME);
            assertThat(fiscalYear.getStartDate()).isEqualTo(NEW_START_DATE);
            assertThat(fiscalYear.getEndDate()).isEqualTo(NEW_END_DATE);
        }

        @Test
        @DisplayName("수정하면 수정 전의 스냅샷과 함께 로그를 기록한다")
        @SuppressWarnings("unchecked")
        void updateFiscalYear_ValidInput_LogsUpdateWithOldSnapshot() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            EntitySnapshot<FiscalYear> oldSnapshot = mock(EntitySnapshot.class);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(entitySnapshotCapturer.capture(fiscalYear)).thenReturn(oldSnapshot);
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE);

            // Then
            verify(activityLogService).logUpdate(oldSnapshot, fiscalYear);
        }

        @Test
        @DisplayName("기간이 자기 자신의 기존 기간과 겹치는 것은 문제 삼지 않는다")
        void updateFiscalYear_PeriodOverlapsOnlyItself_UpdatesPeriod() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(NEW_END_DATE,
                    NEW_START_DATE)).thenReturn(List.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.updateFiscalYear(ID, NAME, NEW_START_DATE, NEW_END_DATE);

            // Then
            assertThat(fiscalYear.getStartDate()).isEqualTo(NEW_START_DATE);
            assertThat(fiscalYear.getEndDate()).isEqualTo(NEW_END_DATE);
        }

        @Test
        @DisplayName("기간이 등록된 수입/지출의 가장 이른 날짜와 가장 늦은 날짜에 딱 맞아도 수정할 수 있다")
        void updateFiscalYear_PeriodExactlyCoversRecords_UpdatesPeriod() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateAsc(ID))
                    .thenReturn(Optional.of(createRecord(NEW_START_DATE)));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateDesc(ID))
                    .thenReturn(Optional.of(createRecord(NEW_END_DATE)));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.updateFiscalYear(ID, NAME, NEW_START_DATE, NEW_END_DATE);

            // Then
            assertThat(fiscalYear.getStartDate()).isEqualTo(NEW_START_DATE);
            assertThat(fiscalYear.getEndDate()).isEqualTo(NEW_END_DATE);
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도를 수정하면 예외 발생")
        void updateFiscalYear_NotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도를 수정하면 예외 발생")
        void updateFiscalYear_Closed_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(createClosedFiscalYear(ID, NAME)));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도는 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("다른 회계 연도가 같은 이름을 쓰고 있으면 예외 발생")
        void updateFiscalYear_DuplicatedName_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.existsByNameAndIdNot(NEW_NAME, ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("같은 이름의 회계 연도가 이미 있습니다.");
        }

        @Test
        @DisplayName("시작일이 종료일보다 늦으면 예외 발생")
        void updateFiscalYear_StartDateAfterEndDate_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NAME, NEW_END_DATE, NEW_START_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("회계 연도의 시작일이 종료일보다 늦습니다.");
        }

        @Test
        @DisplayName("기간이 다른 회계 연도와 겹치면 예외 발생")
        void updateFiscalYear_PeriodOverlapsAnotherFiscalYear_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            FiscalYear overlapped = createFiscalYear(6L, "2026년", LocalDate.of(2025, 12, 1),
                    LocalDate.of(2026, 11, 30));
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(NEW_END_DATE,
                    NEW_START_DATE)).thenReturn(List.of(fiscalYear, overlapped));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("\"2026년\" 회계 연도의 기간(2025-12-01 ~ 2026-11-30)과 겹칩니다.");
        }

        @Test
        @DisplayName("시작일이 등록된 수입/지출의 가장 이른 날짜보다 늦으면 예외 발생")
        void updateFiscalYear_StartDateAfterEarliestRecord_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateAsc(ID))
                    .thenReturn(Optional.of(createRecord(LocalDate.of(2024, 12, 31))));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("시작일은 등록된 수입/지출의 가장 이른 날짜(2024-12-31)보다 늦을 수 없습니다.");
        }

        @Test
        @DisplayName("종료일이 등록된 수입/지출의 가장 늦은 날짜보다 이르면 예외 발생")
        void updateFiscalYear_EndDateBeforeLatestRecord_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, LocalDate.of(2026, 1, 31));
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateAsc(ID))
                    .thenReturn(Optional.of(createRecord(LocalDate.of(2025, 3, 1))));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateDesc(ID))
                    .thenReturn(Optional.of(createRecord(LocalDate.of(2026, 1, 1))));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("종료일은 등록된 수입/지출의 가장 늦은 날짜(2026-01-01)보다 이를 수 없습니다.");
        }

        @Test
        @DisplayName("검증에 실패하면 회계 연도의 이름과 기간을 바꾸지 않는다")
        void updateFiscalYear_ValidationFails_KeepsNameAndPeriod() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(financialRecordRepository.findFirstByFiscalYearIdOrderByDateAsc(ID))
                    .thenReturn(Optional.of(createRecord(LocalDate.of(2024, 12, 31))));

            // When
            assertThatThrownBy(() -> fiscalYearService.updateFiscalYear(ID, NEW_NAME, NEW_START_DATE, NEW_END_DATE))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            assertThat(fiscalYear.getName()).isEqualTo(NAME);
            assertThat(fiscalYear.getStartDate()).isEqualTo(START_DATE);
        }
    }

    @Nested
    @DisplayName("deleteFiscalYear 테스트")
    class DeleteFiscalYearTest {

        @Test
        @DisplayName("존재하지 않는 회계 연도 삭제 시 예외 발생")
        void deleteFiscalYear_NotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.deleteFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도 삭제 시 예외 발생")
        void deleteFiscalYear_Closed_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(createClosedFiscalYear(ID, NAME)));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.deleteFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도는 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("마지막 남은 회계 연도 삭제 시 예외 발생")
        void deleteFiscalYear_LastOne_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.count()).thenReturn(1L);

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.deleteFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("최소 하나의 회계 연도는 유지되어야 합니다.");
        }

        @Test
        @DisplayName("예산 계획이 존재하는 회계 연도 삭제 시 예외 발생")
        void deleteFiscalYear_WithBudgets_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.count()).thenReturn(2L);
            when(budgetRepository.findAllByFiscalYearId(ID)).thenReturn(List.of(Budget.builder().build()));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.deleteFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회계 연도에 설정된 예산 계획이 존재하여 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("수입 또는 지출 내역이 존재하는 회계 연도 삭제 시 예외 발생")
        void deleteFiscalYear_WithTransactions_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.count()).thenReturn(2L);
            when(budgetRepository.findAllByFiscalYearId(ID)).thenReturn(List.of());
            when(financialRecordRepository.existsByFiscalYearId(ID)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.deleteFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 회계 연도에 등록된 수입/지출 내역이 존재하여 삭제할 수 없습니다.");
        }

        @Test
        @DisplayName("예산과 수입/지출이 없는 회계 연도는 삭제한다")
        void deleteFiscalYear_Success() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.count()).thenReturn(2L);
            when(budgetRepository.findAllByFiscalYearId(ID)).thenReturn(List.of());
            when(financialRecordRepository.existsByFiscalYearId(ID)).thenReturn(false);

            // When
            fiscalYearService.deleteFiscalYear(ID);

            // Then
            verify(fiscalYearRepository).delete(fiscalYear);
        }

        @Test
        @DisplayName("회계 연도를 삭제하면 로그를 기록한다")
        void deleteFiscalYear_LogDeletion_Success() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.count()).thenReturn(2L);
            when(budgetRepository.findAllByFiscalYearId(ID)).thenReturn(List.of());
            when(financialRecordRepository.existsByFiscalYearId(ID)).thenReturn(false);

            // When
            fiscalYearService.deleteFiscalYear(ID);

            // Then
            verify(activityLogService).logDeletion(fiscalYear);
        }
    }

    @Nested
    @DisplayName("closeFiscalYear 테스트")
    class CloseFiscalYearTest {

        private void stubTotals(long totalIncome, long totalExpense) {
            when(financialRecordRepository.sumAmountByFiscalYearIdAndType(ID, FinancialRecordType.INCOME))
                    .thenReturn(totalIncome);
            when(financialRecordRepository.sumAmountByFiscalYearIdAndType(ID, FinancialRecordType.EXPENSE))
                    .thenReturn(totalExpense);
        }

        @Test
        @DisplayName("마감하면 마감 상태가 되고 그 시점의 총 수입, 총 지출, 차액을 저장한다")
        void closeFiscalYear_Open_ClosesWithTotals() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);
            stubTotals(700_000L, 450_000L);

            // When
            fiscalYearService.closeFiscalYear(ID);

            // Then
            assertThat(fiscalYear.isClosed()).isTrue();
            assertThat(fiscalYear.getTotalIncome()).isEqualTo(700_000L);
            assertThat(fiscalYear.getTotalExpense()).isEqualTo(450_000L);
            assertThat(fiscalYear.getBalance()).isEqualTo(250_000L);
        }

        @Test
        @DisplayName("지출이 수입보다 많으면 차액을 음수로 저장한다")
        void closeFiscalYear_ExpenseExceedsIncome_SavesNegativeBalance() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);
            stubTotals(100_000L, 130_000L);

            // When
            fiscalYearService.closeFiscalYear(ID);

            // Then
            assertThat(fiscalYear.getBalance()).isEqualTo(-30_000L);
        }

        @Test
        @DisplayName("수입/지출이 없는 회계 연도를 마감하면 금액을 모두 0으로 저장한다")
        void closeFiscalYear_NoRecords_SavesZeroTotals() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);
            stubTotals(0L, 0L);

            // When
            fiscalYearService.closeFiscalYear(ID);

            // Then
            assertThat(fiscalYear.getTotalIncome()).isZero();
            assertThat(fiscalYear.getTotalExpense()).isZero();
            assertThat(fiscalYear.getBalance()).isZero();
        }

        @Test
        @DisplayName("마감하면 마감 전의 스냅샷과 함께 마감 로그를 기록한다")
        @SuppressWarnings("unchecked")
        void closeFiscalYear_Open_LogsClosingWithOldSnapshot() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            EntitySnapshot<FiscalYear> oldSnapshot = mock(EntitySnapshot.class);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(entitySnapshotCapturer.capture(fiscalYear)).thenReturn(oldSnapshot);
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);
            stubTotals(700_000L, 450_000L);

            // When
            fiscalYearService.closeFiscalYear(ID);

            // Then
            verify(activityLogService).logClosing(oldSnapshot, fiscalYear);
        }

        @Test
        @DisplayName("이미 마감된 회계 연도를 마감하면 예외 발생")
        void closeFiscalYear_AlreadyClosed_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(createClosedFiscalYear(ID, NAME)));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.closeFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 마감된 회계 연도입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도를 마감하면 예외 발생")
        void closeFiscalYear_NotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.closeFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }
    }

    @Nested
    @DisplayName("reopenFiscalYear 테스트")
    class ReopenFiscalYearTest {

        @Test
        @DisplayName("마감을 취소하면 마감되지 않은 상태가 되고 총 수입, 총 지출, 차액이 null이 된다")
        void reopenFiscalYear_Closed_ReopensAndClearsTotals() {
            // Given
            FiscalYear fiscalYear = createClosedFiscalYear(ID, NAME);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.reopenFiscalYear(ID);

            // Then
            assertThat(fiscalYear.isClosed()).isFalse();
            assertThat(fiscalYear.getTotalIncome()).isNull();
            assertThat(fiscalYear.getTotalExpense()).isNull();
            assertThat(fiscalYear.getBalance()).isNull();
        }

        @Test
        @DisplayName("마감을 취소하면 취소 전의 스냅샷과 함께 마감 취소 로그를 기록한다")
        @SuppressWarnings("unchecked")
        void reopenFiscalYear_Closed_LogsReopeningWithOldSnapshot() {
            // Given
            FiscalYear fiscalYear = createClosedFiscalYear(ID, NAME);
            EntitySnapshot<FiscalYear> oldSnapshot = mock(EntitySnapshot.class);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));
            when(entitySnapshotCapturer.capture(fiscalYear)).thenReturn(oldSnapshot);
            when(fiscalYearRepository.save(fiscalYear)).thenReturn(fiscalYear);

            // When
            fiscalYearService.reopenFiscalYear(ID);

            // Then
            verify(activityLogService).logReopening(oldSnapshot, fiscalYear);
        }

        @Test
        @DisplayName("마감되지 않은 회계 연도의 마감을 취소하면 예외 발생")
        void reopenFiscalYear_NotClosed_ThrowsException() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.reopenFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감되지 않은 회계 연도입니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도의 마감을 취소하면 예외 발생")
        void reopenFiscalYear_NotFound_ThrowsException() {
            // Given
            when(fiscalYearRepository.findById(ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.reopenFiscalYear(ID))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 회계 연도입니다.");
        }
    }

    @Nested
    @DisplayName("getLatestFiscalYearId 테스트")
    class GetLatestFiscalYearIdTest {

        @Test
        @DisplayName("시작일이 가장 늦은 회계 연도의 ID를 반환한다")
        void getLatestFiscalYearId_Success() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(ID, NAME, START_DATE, END_DATE);
            when(fiscalYearRepository.findFirstByOrderByStartDateDesc()).thenReturn(Optional.of(fiscalYear));

            // When
            Long latestFiscalYearId = fiscalYearService.getLatestFiscalYearId();

            // Then
            assertThat(latestFiscalYearId).isEqualTo(ID);
        }

        @Test
        @DisplayName("등록된 회계 연도가 없을 때 예외 발생")
        void getLatestFiscalYearId_Empty_ThrowsException() {
            // Given
            when(fiscalYearRepository.findFirstByOrderByStartDateDesc()).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> fiscalYearService.getLatestFiscalYearId())
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("등록된 회계 연도가 없습니다.");
        }
    }

    @Nested
    @DisplayName("existsById 테스트")
    class ExistsByIdTest {

        @Test
        @DisplayName("회계 연도가 존재할 때 true 반환")
        void existsById_True() {
            // Given
            when(fiscalYearRepository.existsById(ID)).thenReturn(true);

            // When
            boolean exists = fiscalYearService.existsById(ID);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("회계 연도가 존재하지 않을 때 false 반환")
        void existsById_False() {
            // Given
            when(fiscalYearRepository.existsById(ID)).thenReturn(false);

            // When
            boolean exists = fiscalYearService.existsById(ID);

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("ID가 null이면 리포지토리를 조회하지 않는다")
        void existsById_NullId_DoesNotQueryRepository() {
            // When
            fiscalYearService.existsById(null);

            // Then
            verify(fiscalYearRepository, never()).existsById(any());
        }
    }

}
