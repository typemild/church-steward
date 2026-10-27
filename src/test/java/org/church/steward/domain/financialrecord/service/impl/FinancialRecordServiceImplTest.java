package org.church.steward.domain.financialrecord.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.repository.BudgetRepository;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactGroupService;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordConstraints;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.mapper.FinancialRecordMapper;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class FinancialRecordServiceImplTest {

    @Mock
    private FinancialRecordRepository financialRecordRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private ContactRepository contactRepository;
    @Mock
    private FiscalYearRepository fiscalYearRepository;
    @Mock
    private FinancialRecordMapper financialRecordMapper;
    @Mock
    private ActivityLogService activityLogService;
    @Mock
    private EntitySnapshotCapturer entitySnapshotCapturer;
    @Mock
    private ContactGroupService contactGroupService;

    @InjectMocks
    private FinancialRecordServiceImpl financialRecordService;

    private static final Long FISCAL_YEAR_ID = 5L;

    /** 기간이 달력의 해와 어긋난, 마감되지 않은 회계 연도 "2025년"(2024년 12월 1일 ~ 2025년 11월 30일) */
    private final FiscalYear fiscalYear = FiscalYear.builder()
            .id(FISCAL_YEAR_ID)
            .name("2025년")
            .startDate(LocalDate.of(2024, 12, 1))
            .endDate(LocalDate.of(2025, 11, 30))
            .build();
    private final Budget budget = Budget.builder().id(1L).name("Test Budget").fiscalYear(fiscalYear).build();

    /**
     * 회계 연도 "2025년", 예산 1번에 2025년 3월 2일 날짜로 등록하는 요청을 만든다.
     */
    private FinancialRecordRequestDto createRequest(FinancialRecordType type, List<Long> contactIds) {
        return createRequest(type, contactIds, LocalDate.of(2025, 3, 2));
    }

    /**
     * 회계 연도 "2025년", 예산 1번에 지정한 날짜로 등록하는 요청을 만든다.
     */
    private FinancialRecordRequestDto createRequest(FinancialRecordType type, List<Long> contactIds, LocalDate date) {
        return FinancialRecordRequestDto.builder()
                .type(type)
                .fiscalYearId(FISCAL_YEAR_ID)
                .date(date)
                .budgetId(1L)
                .description("New Description")
                .amount(20000L)
                .remarks("New Remarks")
                .contactIds(contactIds)
                .build();
    }

    /**
     * 이미 저장되어 있는 내역을 만든다.
     */
    private FinancialRecord createExistingRecord(FinancialRecordType type) {
        return FinancialRecord.builder()
                .id(1L)
                .type(type)
                .fiscalYear(fiscalYear)
                .date(LocalDate.of(2025, 3, 1))
                .budget(budget)
                .description("Old Description")
                .amount(1000L)
                .remarks("Old Remarks")
                .build();
    }

    private Contact createContact(Long id, String name) {
        return Contact.builder().id(id).name(name).build();
    }

    /**
     * 요청이 가리키는 회계 연도("2025년")와 예산(1번)을 찾을 수 있게 한다.
     */
    private void givenFiscalYearAndBudgetExist() {
        when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
        when(budgetRepository.findById(1L)).thenReturn(Optional.of(budget));
    }

    /**
     * 조회 결과로 주어진 응답들이 순서대로 나오게 한다.
     */
    @SuppressWarnings("unchecked")
    private void givenRecordsFound(FinancialRecordResponseDto... responses) {
        List<FinancialRecord> records = new ArrayList<>();
        for (FinancialRecordResponseDto response : responses) {
            FinancialRecord record = mock(FinancialRecord.class);
            when(financialRecordMapper.toResponseDto(record)).thenReturn(response);
            records.add(record);
        }
        when(financialRecordRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(records);
    }

    @Nested
    @DisplayName("listFinancialRecords 테스트")
    class ListFinancialRecordsTest {

        @Test
        @DisplayName("수입과 지출이 함께 조회되면, 리포지토리가 돌려준 순서대로 통합된 목록을 반환한다")
        void listFinancialRecords_withIncomeAndExpense_returnsMergedList() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .startDate(LocalDate.of(2023, 1, 1))
                    .endDate(LocalDate.of(2023, 12, 31))
                    .fiscalYearId(3L)
                    .build();
            givenRecordsFound(
                    FinancialRecordResponseDto.builder().id(2L).type(FinancialRecordType.EXPENSE).build(),
                    FinancialRecordResponseDto.builder().id(1L).type(FinancialRecordType.INCOME).build());

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result).extracting(FinancialRecordResponseDto::getId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("내역을 날짜 내림차순, 같은 날짜에서는 ID 내림차순으로 조회한다")
        @SuppressWarnings("unchecked")
        void listFinancialRecords_always_requestsLatestFirstSort() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .build();
            ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);

            // When
            financialRecordService.listFinancialRecords(request);

            // Then
            verify(financialRecordRepository).findAll(any(Specification.class), sortCaptor.capture());
            assertThat(sortCaptor.getValue())
                    .isEqualTo(Sort.by(Sort.Direction.DESC, FinancialRecord::getDate, FinancialRecord::getId));
        }

        @Test
        @DisplayName("데이터가 없을 때, 빈 목록을 반환한다")
        void listFinancialRecords_emptyData_returnsEmptyList() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .build();
            givenRecordsFound();

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("예산 ID가 없는 경우, 하위 예산을 조회하지 않는다")
        void listFinancialRecords_whenBudgetIdIsNull_doesNotFindDescendantBudgets() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .build();

            // When
            financialRecordService.listFinancialRecords(request);

            // Then
            verify(budgetRepository, never()).findAllDescendantIds(anyLong());
        }

        @Test
        @DisplayName("예산 ID가 있는 경우, 해당 예산의 하위 예산 ID들을 조회한다")
        void listFinancialRecords_whenBudgetIdIsPresent_findsDescendantBudgets() {
            // Given
            Long budgetId = 1L;
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .budgetId(budgetId)
                    .build();
            when(budgetRepository.findAllDescendantIds(budgetId)).thenReturn(List.of(1L, 2L));

            // When
            financialRecordService.listFinancialRecords(request);

            // Then
            verify(budgetRepository).findAllDescendantIds(budgetId);
        }

        @Test
        @DisplayName("시작 금액만 지정하면, 시작 금액 이상인 내역만 반환한다")
        void listFinancialRecords_withMinAmountOnly_returnsRecordsAtLeastMinAmount() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .minAmount(1000L)
                    .build();
            givenRecordsFound(
                    FinancialRecordResponseDto.builder().id(3L).amount(999L).build(),
                    FinancialRecordResponseDto.builder().id(2L).amount(1000L).build(),
                    FinancialRecordResponseDto.builder().id(1L).amount(1001L).build());

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result)
                    .extracting(FinancialRecordResponseDto::getId)
                    .containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("종료 금액만 지정하면, 종료 금액 이하인 내역만 반환한다")
        void listFinancialRecords_withMaxAmountOnly_returnsRecordsAtMostMaxAmount() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .maxAmount(1000L)
                    .build();
            givenRecordsFound(
                    FinancialRecordResponseDto.builder().id(3L).amount(999L).build(),
                    FinancialRecordResponseDto.builder().id(2L).amount(1000L).build(),
                    FinancialRecordResponseDto.builder().id(1L).amount(1001L).build());

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result)
                    .extracting(FinancialRecordResponseDto::getId)
                    .containsExactly(3L, 2L);
        }

        @Test
        @DisplayName("시작 금액과 종료 금액을 모두 지정하면, 양 끝을 포함한 범위 안의 내역만 반환한다")
        void listFinancialRecords_withMinAndMaxAmount_returnsRecordsWithinInclusiveRange() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .minAmount(500L)
                    .maxAmount(1000L)
                    .build();
            givenRecordsFound(
                    FinancialRecordResponseDto.builder().id(4L).amount(499L).build(),
                    FinancialRecordResponseDto.builder().id(3L).amount(500L).build(),
                    FinancialRecordResponseDto.builder().id(2L).amount(1000L).build(),
                    FinancialRecordResponseDto.builder().id(1L).amount(1001L).build());

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result)
                    .extracting(FinancialRecordResponseDto::getId)
                    .containsExactly(3L, 2L);
        }

        @Test
        @DisplayName("시작 금액과 종료 금액이 같으면, 금액이 일치하는 내역만 반환한다")
        void listFinancialRecords_withSameMinAndMaxAmount_returnsEqualAmountRecords() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(3L)
                    .minAmount(500L)
                    .maxAmount(500L)
                    .build();
            givenRecordsFound(
                    FinancialRecordResponseDto.builder().id(3L).amount(499L).build(),
                    FinancialRecordResponseDto.builder().id(2L).amount(500L).build(),
                    FinancialRecordResponseDto.builder().id(1L).amount(501L).build());

            // When
            List<FinancialRecordResponseDto> result = financialRecordService.listFinancialRecords(request);

            // Then
            assertThat(result)
                    .extracting(FinancialRecordResponseDto::getId)
                    .containsExactly(2L);
        }
    }

    @Nested
    @DisplayName("createFinancialRecordWithoutLog 테스트")
    class CreateFinancialRecordWithoutLogTest {

        @Test
        @DisplayName("저장한 내역을 반환한다")
        void createFinancialRecordWithoutLog_validRequest_returnsSavedRecord() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            FinancialRecord result = financialRecordService.createFinancialRecordWithoutLog(dto);

            // Then
            assertThat(result)
                    .extracting(FinancialRecord::getType, FinancialRecord::getBudget, FinancialRecord::getAmount)
                    .containsExactly(FinancialRecordType.INCOME, budget, 20000L);
        }

        @Test
        @DisplayName("활동 기록을 남기지 않는다")
        void createFinancialRecordWithoutLog_validRequest_doesNotLog() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.createFinancialRecordWithoutLog(dto);

            // Then
            verify(activityLogService, never()).logCreation(any());
        }

        @Test
        @DisplayName("수입의 봉헌자가 2명 이상이면 일반 등록과 똑같이 그룹 생성을 요청한다")
        void createFinancialRecordWithoutLog_incomeWithTwoContacts_requestsGroupCreationIfAbsent() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 11L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L, 11L)))
                    .thenReturn(List.of(createContact(10L, "Contact 1"), createContact(11L, "Contact 2")));

            // When
            financialRecordService.createFinancialRecordWithoutLog(dto);

            // Then
            verify(contactGroupService).createGroupIfAbsent(any());
        }

        @Test
        @DisplayName("일반 등록과 같은 검증을 한다 (지출의 수취인은 한 명이어야 한다)")
        void createFinancialRecordWithoutLog_expenseWithMultipleContacts_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L, 11L));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecordWithoutLog(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("수취인은 한 명이어야 합니다.");
        }
    }

    @Nested
    @DisplayName("createFinancialRecord 테스트")
    class CreateFinancialRecordTest {

        @Test
        @DisplayName("생성에 성공하면 매퍼가 변환한 응답을 반환한다")
        void createFinancialRecord_validRequest_returnsMappedResponse() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            FinancialRecordResponseDto response = FinancialRecordResponseDto.builder().id(1L).build();
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));
            when(financialRecordMapper.toResponseDto(any(FinancialRecord.class))).thenReturn(response);

            // When
            FinancialRecordResponseDto result = financialRecordService.createFinancialRecord(dto);

            // Then
            assertThat(result).isSameAs(response);
        }

        @Test
        @DisplayName("수입을 생성하면 구분이 수입이고 봉헌자가 모두 연결된 내역을 저장한다")
        void createFinancialRecord_income_savesIncomeWithContacts() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 11L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L, 11L)))
                    .thenReturn(List.of(createContact(10L, "Contact 1"), createContact(11L, "Contact 2")));
            ArgumentCaptor<FinancialRecord> recordCaptor = ArgumentCaptor.forClass(FinancialRecord.class);

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(financialRecordRepository).save(recordCaptor.capture());
            FinancialRecord saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(FinancialRecordType.INCOME);
            assertThat(saved.getFiscalYear()).isSameAs(fiscalYear);
            assertThat(saved.getBudget()).isSameAs(budget);
            assertThat(saved.getDate()).isEqualTo(LocalDate.of(2025, 3, 2));
            assertThat(saved.getDescription()).isEqualTo("New Description");
            assertThat(saved.getAmount()).isEqualTo(20000L);
            assertThat(saved.getRemarks()).isEqualTo("New Remarks");
            assertThat(saved.getContacts()).extracting(FinancialRecordContact::getContactName)
                    .containsExactlyInAnyOrder("Contact 1", "Contact 2");
        }

        @Test
        @DisplayName("지출을 생성하면 구분이 지출이고 수취인이 연결된 내역을 저장한다")
        void createFinancialRecord_expense_savesExpenseWithRecipient() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Recipient")));
            ArgumentCaptor<FinancialRecord> recordCaptor = ArgumentCaptor.forClass(FinancialRecord.class);

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(financialRecordRepository).save(recordCaptor.capture());
            FinancialRecord saved = recordCaptor.getValue();
            assertThat(saved.getType()).isEqualTo(FinancialRecordType.EXPENSE);
            assertThat(saved.getContacts()).extracting(FinancialRecordContact::getContactName)
                    .containsExactly("Recipient");
        }

        @Test
        @DisplayName("같은 관계자가 중복 입력되면 한 번만 연결해 저장한다")
        void createFinancialRecord_duplicateContactIds_savesContactOnce() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));
            ArgumentCaptor<FinancialRecord> recordCaptor = ArgumentCaptor.forClass(FinancialRecord.class);

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(financialRecordRepository).save(recordCaptor.capture());
            assertThat(recordCaptor.getValue().getContacts()).hasSize(1);
        }

        @Test
        @DisplayName("생성에 성공하면 생성 활동 기록을 남긴다")
        void createFinancialRecord_validRequest_logsCreation() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Recipient")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(activityLogService).logCreation(any(FinancialRecord.class));
        }

        @Test
        @DisplayName("수입의 봉헌자가 2명 이상이면 같은 구성원의 그룹이 없을 때 생성하도록 요청한다")
        @SuppressWarnings("unchecked")
        void createFinancialRecord_incomeWithTwoOrMoreContacts_requestsGroupCreationIfAbsent() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 11L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L, 11L)))
                    .thenReturn(List.of(createContact(10L, "Contact 1"), createContact(11L, "Contact 2")));
            ArgumentCaptor<List<Long>> contactIdsCaptor = ArgumentCaptor.forClass(List.class);

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(contactGroupService).createGroupIfAbsent(contactIdsCaptor.capture());
            assertThat(contactIdsCaptor.getValue()).containsExactlyInAnyOrder(10L, 11L);
        }

        @Test
        @DisplayName("수입의 봉헌자가 1명이면 그룹 생성을 요청하지 않는다")
        void createFinancialRecord_incomeWithSingleContact_doesNotRequestGroupCreation() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(contactGroupService, never()).createGroupIfAbsent(any());
        }

        @Test
        @DisplayName("같은 봉헌자가 중복 입력되어 실제로는 1명이면 그룹 생성을 요청하지 않는다")
        void createFinancialRecord_incomeWithDuplicateSingleContact_doesNotRequestGroupCreation() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(contactGroupService, never()).createGroupIfAbsent(any());
        }

        @Test
        @DisplayName("지출은 그룹 기능을 쓰지 않으므로 그룹 생성을 요청하지 않는다")
        void createFinancialRecord_expense_doesNotRequestGroupCreation() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Recipient")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(contactGroupService, never()).createGroupIfAbsent(any());
        }

        @Test
        @DisplayName("구분이 지정되지 않으면 예외가 발생한다")
        void createFinancialRecord_nullType_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(null, List.of(10L));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Financial Record Type");
        }

        @Test
        @DisplayName("금액이 음수이면 예외가 발생한다")
        void createFinancialRecord_negativeAmount_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            dto.setAmount(-1L);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액은 0원 이상이어야 합니다.");
        }

        @Test
        @DisplayName("금액이 허용 범위를 초과하면 예외가 발생한다")
        void createFinancialRecord_amountExceedsMax_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            dto.setAmount(200_000_000_000L);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액이 허용된 범위를 초과했습니다.");
        }

        @Test
        @DisplayName("내용이 최대 길이를 넘으면 예외가 발생한다")
        void createFinancialRecord_descriptionTooLong_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            dto.setDescription("가".repeat(FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH + 1));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("내용은 100자 이내로 입력해주세요.");
        }

        @Test
        @DisplayName("비고가 최대 길이를 넘으면 예외가 발생한다")
        void createFinancialRecord_remarksTooLong_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            dto.setRemarks("나".repeat(FinancialRecordConstraints.REMARKS_MAX_LENGTH + 1));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비고는 100자 이내로 입력해주세요.");
        }

        @Test
        @DisplayName("내용과 비고가 최대 길이와 같으면 저장한다")
        void createFinancialRecord_maxLengthTexts_savesRecord() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            dto.setDescription("가".repeat(FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH));
            dto.setRemarks("나".repeat(FinancialRecordConstraints.REMARKS_MAX_LENGTH));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Kim")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(financialRecordRepository).save(any(FinancialRecord.class));
        }

        @Test
        @DisplayName("수입의 봉헌자가 한 명도 없으면 예외가 발생한다")
        void createFinancialRecord_incomeWithNoContacts_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("봉헌자는 한 명 이상이어야 합니다.");
        }

        @Test
        @DisplayName("수입의 봉헌자 목록이 null이면 예외가 발생한다")
        void createFinancialRecord_incomeWithNullContacts_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, null);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("봉헌자는 한 명 이상이어야 합니다.");
        }

        @Test
        @DisplayName("지출의 수취인이 없으면 예외가 발생한다")
        void createFinancialRecord_expenseWithNoContacts_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수취인은 한 명이어야 합니다.");
        }

        @Test
        @DisplayName("지출의 수취인이 2명 이상이면 예외가 발생한다")
        void createFinancialRecord_expenseWithMultipleContacts_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L, 11L));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수취인은 한 명이어야 합니다.");
        }

        @Test
        @DisplayName("존재하지 않는 회계 연도이면 예외가 발생한다")
        void createFinancialRecord_invalidFiscalYear_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Fiscal Year ID");
        }

        @Test
        @DisplayName("날짜가 회계 연도의 시작일보다 이르면 예외가 발생한다")
        void createFinancialRecord_dateBeforeFiscalYearStart_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L),
                    LocalDate.of(2024, 11, 30));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("날짜가 \"2025년\" 회계 연도의 기간(2024-12-01 ~ 2025-11-30)을 벗어났습니다.");
        }

        @Test
        @DisplayName("날짜가 회계 연도의 종료일보다 늦으면 같은 해의 날짜여도 예외가 발생한다")
        void createFinancialRecord_dateAfterFiscalYearEnd_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L),
                    LocalDate.of(2025, 12, 1));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("날짜가 \"2025년\" 회계 연도의 기간(2024-12-01 ~ 2025-11-30)을 벗어났습니다.");
        }

        @Test
        @DisplayName("날짜가 회계 연도의 시작일이면 달력으로는 앞선 해의 날짜여도 저장한다")
        void createFinancialRecord_dateOnFiscalYearStart_savesRecord() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L),
                    LocalDate.of(2024, 12, 1));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.createFinancialRecord(dto);

            // Then
            verify(financialRecordRepository).save(any(FinancialRecord.class));
        }

        @Test
        @DisplayName("날짜가 없으면 예외가 발생한다")
        void createFinancialRecord_nullDate_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L), null);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("날짜를 입력해주세요.");
        }

        @Test
        @DisplayName("하위 예산이 있는 예산에 수입을 등록하면 예외가 발생한다")
        void createFinancialRecord_incomeBudgetHasChildren_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            Budget parentBudget = mock(Budget.class);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentBudget));
            when(parentBudget.hasChildren()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("하위 예산이 있는 예산에는 수입을 등록할 수 없습니다.");
        }

        @Test
        @DisplayName("하위 예산이 있는 예산에 지출을 등록하면 예외가 발생한다")
        void createFinancialRecord_expenseBudgetHasChildren_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            Budget parentBudget = mock(Budget.class);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentBudget));
            when(parentBudget.hasChildren()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("하위 예산이 있는 예산에는 지출을 등록할 수 없습니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도에는 내역을 등록할 수 없다")
        void createFinancialRecord_closedFiscalYear_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            fiscalYear.close(0L, 0L);
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 수입/지출은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("다른 회계 연도의 예산에 등록하면 예외가 발생한다")
        void createFinancialRecord_budgetOfOtherFiscalYear_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            Budget budgetOfOtherFiscalYear = Budget.builder()
                    .id(1L)
                    .name("Other Budget")
                    .fiscalYear(FiscalYear.builder().id(6L).name("2026년").build())
                    .build();
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(budgetOfOtherFiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("다른 회계 연도의 예산은 선택할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 관계자 ID가 포함되어 있으면 예외가 발생한다")
        void createFinancialRecord_nonExistentContactIds_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(999L));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(999L))).thenReturn(new ArrayList<>());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.createFinancialRecord(dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Contact ID(s) provided.");
        }
    }

    @Nested
    @DisplayName("updateFinancialRecord 테스트")
    class UpdateFinancialRecordTest {

        @Test
        @DisplayName("수정에 성공하면 요청한 값으로 내역이 바뀐다")
        void updateFinancialRecord_validRequest_updatesFields() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            assertThat(financialRecord.getDate()).isEqualTo(LocalDate.of(2025, 3, 2));
            assertThat(financialRecord.getDescription()).isEqualTo("New Description");
            assertThat(financialRecord.getAmount()).isEqualTo(20000L);
            assertThat(financialRecord.getRemarks()).isEqualTo("New Remarks");
        }

        @Test
        @DisplayName("수정하면 기존 관계자를 지우고 요청한 관계자로 바꾼다")
        void updateFinancialRecord_validRequest_replacesContacts() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            financialRecord.addContact(createContact(10L, "Old Recipient"));
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(11L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(11L))).thenReturn(List.of(createContact(11L, "New Recipient")));

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            assertThat(financialRecord.getContacts()).extracting(FinancialRecordContact::getContactName)
                    .containsExactly("New Recipient");
        }

        @Test
        @DisplayName("요청의 구분이 기존 내역과 달라도 기존 내역의 구분을 유지한다")
        void updateFinancialRecord_differentTypeInRequest_keepsExistingType() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Recipient")));

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            assertThat(financialRecord.getType()).isEqualTo(FinancialRecordType.EXPENSE);
        }

        @Test
        @DisplayName("수정에 성공하면 매퍼가 변환한 응답을 반환한다")
        void updateFinancialRecord_validRequest_returnsMappedResponse() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            FinancialRecordResponseDto response = FinancialRecordResponseDto.builder().id(1L).build();
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));
            when(financialRecordMapper.toResponseDto(financialRecord)).thenReturn(response);

            // When
            FinancialRecordResponseDto result = financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            assertThat(result).isSameAs(response);
        }

        @Test
        @DisplayName("수정에 성공하면 내역을 저장한다")
        void updateFinancialRecord_validRequest_savesRecord() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            verify(financialRecordRepository).save(financialRecord);
        }

        @Test
        @DisplayName("수정에 성공하면 변경 전 스냅샷과 함께 수정 활동 기록을 남긴다")
        void updateFinancialRecord_validRequest_logsUpdateWithOldSnapshot() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            @SuppressWarnings("unchecked")
            EntitySnapshot<FinancialRecord> snapshot = mock(EntitySnapshot.class);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Recipient")));
            when(entitySnapshotCapturer.capture(financialRecord)).thenReturn(snapshot);

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            verify(activityLogService).logUpdate(snapshot, financialRecord);
        }

        @Test
        @DisplayName("수입의 봉헌자가 2명 이상이면 같은 구성원의 그룹이 없을 때 생성하도록 요청한다")
        @SuppressWarnings("unchecked")
        void updateFinancialRecord_incomeWithTwoOrMoreContacts_requestsGroupCreationIfAbsent() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L, 11L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L, 11L)))
                    .thenReturn(List.of(createContact(10L, "Contact 1"), createContact(11L, "Contact 2")));
            ArgumentCaptor<List<Long>> contactIdsCaptor = ArgumentCaptor.forClass(List.class);

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            verify(contactGroupService).createGroupIfAbsent(contactIdsCaptor.capture());
            assertThat(contactIdsCaptor.getValue()).containsExactlyInAnyOrder(10L, 11L);
        }

        @Test
        @DisplayName("수입의 봉헌자가 1명이면 그룹 생성을 요청하지 않는다")
        void updateFinancialRecord_incomeWithSingleContact_doesNotRequestGroupCreation() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(10L))).thenReturn(List.of(createContact(10L, "Contact")));

            // When
            financialRecordService.updateFinancialRecord(1L, dto);

            // Then
            verify(contactGroupService, never()).createGroupIfAbsent(any());
        }

        @Test
        @DisplayName("존재하지 않는 내역 ID이면 예외가 발생한다")
        void updateFinancialRecord_recordNotFound_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Financial Record not found");
        }

        @Test
        @DisplayName("금액이 음수이면 예외가 발생한다")
        void updateFinancialRecord_negativeAmount_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            dto.setAmount(-1L);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액은 0원 이상이어야 합니다.");
        }

        @Test
        @DisplayName("금액이 허용 범위를 초과하면 예외가 발생한다")
        void updateFinancialRecord_amountExceedsMax_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(10L));
            dto.setAmount(200_000_000_000L);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("금액이 허용된 범위를 초과했습니다.");
        }

        @Test
        @DisplayName("내용이 최대 길이를 넘으면 예외가 발생한다")
        void updateFinancialRecord_descriptionTooLong_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            dto.setDescription("가".repeat(FinancialRecordConstraints.DESCRIPTION_MAX_LENGTH + 1));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("내용은 100자 이내로 입력해주세요.");
        }

        @Test
        @DisplayName("비고가 최대 길이를 넘으면 예외가 발생한다")
        void updateFinancialRecord_remarksTooLong_throwsException() {
            // Given
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            dto.setRemarks("나".repeat(FinancialRecordConstraints.REMARKS_MAX_LENGTH + 1));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("비고는 100자 이내로 입력해주세요.");
        }

        @Test
        @DisplayName("수입의 봉헌자가 한 명도 없으면 예외가 발생한다")
        void updateFinancialRecord_incomeWithNoContacts_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of());
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("봉헌자는 한 명 이상이어야 합니다.");
        }

        @Test
        @DisplayName("지출의 수취인이 2명 이상이면 예외가 발생한다")
        void updateFinancialRecord_expenseWithMultipleContacts_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L, 11L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("수취인은 한 명이어야 합니다.");
        }

        @Test
        @DisplayName("날짜를 회계 연도의 기간 밖으로 변경하면 예외가 발생한다")
        void updateFinancialRecord_dateOutsideFiscalYearPeriod_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L),
                    LocalDate.of(2025, 12, 1));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("날짜가 \"2025년\" 회계 연도의 기간(2024-12-01 ~ 2025-11-30)을 벗어났습니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도의 내역은 수정할 수 없다")
        void updateFinancialRecord_closedFiscalYear_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            fiscalYear.close(0L, 0L);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 수입/지출은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("마감된 회계 연도의 내역을 수정하려 하면 내역의 값을 바꾸지 않는다")
        void updateFinancialRecord_closedFiscalYear_keepsRecord() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            fiscalYear.close(0L, 0L);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class);

            // Then
            assertThat(financialRecord.getAmount()).isEqualTo(1000L);
            assertThat(financialRecord.getDescription()).isEqualTo("Old Description");
        }

        @Test
        @DisplayName("하위 예산이 있는 예산으로 변경하면 예외가 발생한다")
        void updateFinancialRecord_budgetHasChildren_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.EXPENSE);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.EXPENSE, List.of(10L));
            Budget parentBudget = mock(Budget.class);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            when(fiscalYearRepository.findById(FISCAL_YEAR_ID)).thenReturn(Optional.of(fiscalYear));
            when(budgetRepository.findById(1L)).thenReturn(Optional.of(parentBudget));
            when(parentBudget.hasChildren()).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("하위 예산이 있는 예산으로는 지출을 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 관계자 ID가 포함되어 있으면 예외가 발생한다")
        void updateFinancialRecord_nonExistentContactIds_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            FinancialRecordRequestDto dto = createRequest(FinancialRecordType.INCOME, List.of(999L));
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));
            givenFiscalYearAndBudgetExist();
            when(contactRepository.findAllById(Set.of(999L))).thenReturn(new ArrayList<>());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.updateFinancialRecord(1L, dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid Contact ID(s) provided.");
        }
    }

    @Nested
    @DisplayName("deleteFinancialRecord 테스트")
    class DeleteFinancialRecordTest {

        @Test
        @DisplayName("삭제에 성공하면 리포지토리에서 내역을 삭제한다")
        void deleteFinancialRecord_existingRecord_deletesRecord() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When
            financialRecordService.deleteFinancialRecord(1L);

            // Then
            verify(financialRecordRepository).delete(financialRecord);
        }

        @Test
        @DisplayName("삭제에 성공하면 삭제 활동 기록을 남긴다")
        void deleteFinancialRecord_existingRecord_logsDeletion() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When
            financialRecordService.deleteFinancialRecord(1L);

            // Then
            verify(activityLogService).logDeletion(financialRecord);
        }

        @Test
        @DisplayName("마감된 회계 연도의 내역은 삭제할 수 없다")
        void deleteFinancialRecord_closedFiscalYear_throwsException() {
            // Given
            FinancialRecord financialRecord = createExistingRecord(FinancialRecordType.INCOME);
            fiscalYear.close(0L, 0L);
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.of(financialRecord));

            // When & Then
            assertThatThrownBy(() -> financialRecordService.deleteFinancialRecord(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("마감된 회계 연도의 수입/지출은 변경할 수 없습니다.");
        }

        @Test
        @DisplayName("존재하지 않는 내역 ID이면 예외가 발생한다")
        void deleteFinancialRecord_recordNotFound_throwsException() {
            // Given
            when(financialRecordRepository.findById(1L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> financialRecordService.deleteFinancialRecord(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Financial Record not found");
        }
    }
}
