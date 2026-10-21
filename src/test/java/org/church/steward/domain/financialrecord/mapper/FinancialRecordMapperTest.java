package org.church.steward.domain.financialrecord.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FinancialRecordMapperTest {

    private final FinancialRecordMapper financialRecordMapper = new FinancialRecordMapper();

    @Nested
    @DisplayName("toResponseDto 테스트")
    class ToResponseDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toResponseDto_nullEntity_returnsNull() {
            // Given
            FinancialRecord entity = null;

            // When
            FinancialRecordResponseDto result = financialRecordMapper.toResponseDto(entity);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("모든 값이 채워진 엔터티를 구분, 예산, 관계자 정보를 포함한 DTO로 변환한다")
        void toResponseDto_fullEntity_returnsCorrectDto() {
            // Given
            FiscalYear fiscalYear = mock(FiscalYear.class);
            when(fiscalYear.getId()).thenReturn(7L);

            Budget budget = mock(Budget.class);
            when(budget.getId()).thenReturn(1L);
            when(budget.getHierarchyName()).thenReturn("Root > Budget");

            Contact contact1 = mock(Contact.class);
            when(contact1.getId()).thenReturn(10L);
            when(contact1.getName()).thenReturn("홍길동");

            Contact contact2 = mock(Contact.class);
            when(contact2.getId()).thenReturn(11L);
            when(contact2.getName()).thenReturn("김철수");

            FinancialRecord financialRecord = FinancialRecord.builder()
                    .id(1L)
                    .type(FinancialRecordType.INCOME)
                    .fiscalYear(fiscalYear)
                    .date(LocalDate.of(2024, 1, 1))
                    .budget(budget)
                    .description("테스트 수입")
                    .amount(100000L)
                    .remarks("비고")
                    .build();

            financialRecord.addContact(contact1);
            financialRecord.addContact(contact2);

            // When
            FinancialRecordResponseDto result = financialRecordMapper.toResponseDto(financialRecord);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getType()).isEqualTo(FinancialRecordType.INCOME);
            assertThat(result.getFiscalYearId()).isEqualTo(7L);
            assertThat(result.getDate()).isEqualTo(LocalDate.of(2024, 1, 1));
            assertThat(result.getBudgetId()).isEqualTo(1L);
            assertThat(result.getBudgetName()).isEqualTo("Root > Budget");
            assertThat(result.getDescription()).isEqualTo("테스트 수입");
            assertThat(result.getAmount()).isEqualTo(100000L);
            assertThat(result.getRemarks()).isEqualTo("비고");
            assertThat(result.getContactIds()).containsExactlyInAnyOrder(10L, 11L);
            assertThat(result.getContactNames()).containsExactlyInAnyOrder("홍길동", "김철수");
        }

        @Test
        @DisplayName("회계 연도와 예산이 없는 엔터티는 해당 필드가 null이고, 관계자가 없으면 관계자 목록이 빈 목록인 DTO로 변환한다")
        void toResponseDto_partialEntity_returnsDtoWithNullFieldsAndEmptyContacts() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .id(1L)
                    .type(FinancialRecordType.EXPENSE)
                    .fiscalYear(null)
                    .budget(null)
                    .contacts(null)
                    .build();

            // When
            FinancialRecordResponseDto result = financialRecordMapper.toResponseDto(financialRecord);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getFiscalYearId()).isNull();
            assertThat(result.getBudgetId()).isNull();
            assertThat(result.getBudgetName()).isNull();
            assertThat(result.getContactIds()).isEmpty();
            assertThat(result.getContactNames()).isEmpty();
        }
    }
}
