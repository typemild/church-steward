package org.church.steward.domain.financialrecord.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.service.FinancialRecordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialRecordControllerTest {

    @Mock
    private FinancialRecordService financialRecordService;

    @InjectMocks
    private FinancialRecordController financialRecordController;

    @Nested
    @DisplayName("listFinancialRecords 테스트")
    class ListFinancialRecordsTest {

        @Test
        @DisplayName("내역 목록 조회 시 요청된 조건에 맞는 목록을 반환한다")
        void listFinancialRecords_withRequest_returnsList() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder()
                    .description("test description")
                    .remarks("test remarks")
                    .build();
            FinancialRecordResponseDto response = FinancialRecordResponseDto.builder().id(1L).build();
            when(financialRecordService.listFinancialRecords(request)).thenReturn(List.of(response));

            // When
            List<FinancialRecordResponseDto> result = financialRecordController.listFinancialRecords(request);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("내역 목록 조회 시 서비스의 listFinancialRecords를 호출한다")
        void listFinancialRecords_withRequest_callsService() {
            // Given
            FinancialRecordSearchRequestDto request = FinancialRecordSearchRequestDto.builder().build();

            // When
            financialRecordController.listFinancialRecords(request);

            // Then
            verify(financialRecordService).listFinancialRecords(request);
        }
    }

    @Nested
    @DisplayName("createFinancialRecord 테스트")
    class CreateFinancialRecordTest {

        @Test
        @DisplayName("내역 생성 시 입력된 DTO를 바탕으로 생성된 정보를 반환한다")
        void createFinancialRecord_withDto_returnsCreatedDto() {
            // Given
            FinancialRecordRequestDto requestDto = FinancialRecordRequestDto.builder()
                    .type(FinancialRecordType.INCOME)
                    .amount(50000L)
                    .date(LocalDate.now())
                    .build();

            FinancialRecordResponseDto responseDto = FinancialRecordResponseDto.builder()
                    .id(1L)
                    .type(FinancialRecordType.INCOME)
                    .amount(50000L)
                    .build();

            when(financialRecordService.createFinancialRecord(requestDto)).thenReturn(responseDto);

            // When
            FinancialRecordResponseDto result = financialRecordController.createFinancialRecord(requestDto);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAmount()).isEqualTo(50000L);
        }

        @Test
        @DisplayName("내역 생성 시 서비스의 createFinancialRecord를 호출한다")
        void createFinancialRecord_withDto_callsService() {
            // Given
            FinancialRecordRequestDto requestDto = FinancialRecordRequestDto.builder().build();

            // When
            financialRecordController.createFinancialRecord(requestDto);

            // Then
            verify(financialRecordService).createFinancialRecord(requestDto);
        }
    }

    @Nested
    @DisplayName("updateFinancialRecord 테스트")
    class UpdateFinancialRecordTest {

        @Test
        @DisplayName("내역 수정 시 입력된 ID와 DTO를 바탕으로 수정된 정보를 반환한다")
        void updateFinancialRecord_withIdAndDto_returnsUpdatedDto() {
            // Given
            Long id = 1L;
            FinancialRecordRequestDto requestDto = FinancialRecordRequestDto.builder()
                    .amount(60000L)
                    .date(LocalDate.now())
                    .build();

            FinancialRecordResponseDto responseDto = FinancialRecordResponseDto.builder()
                    .id(1L)
                    .amount(60000L)
                    .build();

            when(financialRecordService.updateFinancialRecord(id, requestDto)).thenReturn(responseDto);

            // When
            FinancialRecordResponseDto result = financialRecordController.updateFinancialRecord(id, requestDto);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getAmount()).isEqualTo(60000L);
        }

        @Test
        @DisplayName("내역 수정 시 서비스의 updateFinancialRecord를 호출한다")
        void updateFinancialRecord_withIdAndDto_callsService() {
            // Given
            Long id = 1L;
            FinancialRecordRequestDto requestDto = FinancialRecordRequestDto.builder().build();

            // When
            financialRecordController.updateFinancialRecord(id, requestDto);

            // Then
            verify(financialRecordService).updateFinancialRecord(id, requestDto);
        }
    }

    @Nested
    @DisplayName("updateFinancialRecordsBudget 테스트")
    class UpdateFinancialRecordsBudgetTest {

        @Test
        @DisplayName("예산 일괄 변경 시 서비스의 updateFinancialRecordsBudget를 호출한다")
        void updateFinancialRecordsBudget_withIdsAndBudgetId_callsService() {
            // Given
            List<Long> ids = List.of(1L, 2L);

            // When
            financialRecordController.updateFinancialRecordsBudget(ids, 3L);

            // Then
            verify(financialRecordService).updateFinancialRecordsBudget(ids, 3L);
        }
    }

    @Nested
    @DisplayName("updateFinancialRecordsContacts 테스트")
    class UpdateFinancialRecordsContactsTest {

        @Test
        @DisplayName("관계자 일괄 변경 시 서비스의 updateFinancialRecordsContacts를 호출한다")
        void updateFinancialRecordsContacts_withIdsAndContactIds_callsService() {
            // Given
            List<Long> ids = List.of(1L, 2L);
            List<Long> contactIds = List.of(10L, 11L);

            // When
            financialRecordController.updateFinancialRecordsContacts(ids, contactIds);

            // Then
            verify(financialRecordService).updateFinancialRecordsContacts(ids, contactIds);
        }
    }

    @Nested
    @DisplayName("deleteFinancialRecords 테스트")
    class DeleteFinancialRecordsTest {

        @Test
        @DisplayName("내역 일괄 삭제 시 서비스의 deleteFinancialRecords를 호출한다")
        void deleteFinancialRecords_withIds_callsService() {
            // Given
            List<Long> ids = List.of(1L, 2L);

            // When
            financialRecordController.deleteFinancialRecords(ids);

            // Then
            verify(financialRecordService).deleteFinancialRecords(ids);
        }
    }
}
