package org.church.steward.domain.financialrecord.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancialRecordTest {

    @Nested
    @DisplayName("builder 테스트")
    class BuilderTest {

        @Test
        @DisplayName("Builder를 통해 생성 시 모든 필드가 올바르게 설정된다")
        void builder_validParams_setsFields() {
            // Given
            FiscalYear fiscalYear = mock(FiscalYear.class);
            Budget budget = mock(Budget.class);
            LocalDate date = LocalDate.of(2023, 1, 1);
            String description = "테스트 수입";
            long amount = 10000L;
            String remarks = "비고";

            // When
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .fiscalYear(fiscalYear)
                    .date(date)
                    .budget(budget)
                    .description(description)
                    .amount(amount)
                    .remarks(remarks)
                    .build();

            // Then
            assertThat(financialRecord.getType()).isEqualTo(FinancialRecordType.INCOME);
            assertThat(financialRecord.getFiscalYear()).isEqualTo(fiscalYear);
            assertThat(financialRecord.getDate()).isEqualTo(date);
            assertThat(financialRecord.getBudget()).isEqualTo(budget);
            assertThat(financialRecord.getDescription()).isEqualTo(description);
            assertThat(financialRecord.getAmount()).isEqualTo(amount);
            assertThat(financialRecord.getRemarks()).isEqualTo(remarks);
            assertThat(financialRecord.getContacts()).isNotNull();
            assertThat(financialRecord.getContacts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("addContact 테스트")
    class AddContactTest {

        @Test
        @DisplayName("새로운 관계자를 추가하면 당시의 이름과 함께 리스트에 추가된다")
        void addContact_newContact_addsToList() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder().build();
            Contact contact = mock(Contact.class);
            when(contact.getName()).thenReturn("김철수");

            // When
            financialRecord.addContact(contact);

            // Then
            assertThat(financialRecord.getContacts()).hasSize(1);
            assertThat(financialRecord.getContacts().get(0).getContactName()).isEqualTo("김철수");
        }

        @Test
        @DisplayName("이미 존재하는 관계자를 추가하려고 하면 무시된다")
        void addContact_duplicateContact_doesNotAdd() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder().build();
            Contact contact = mock(Contact.class);
            when(contact.getName()).thenReturn("김철수");

            // When
            financialRecord.addContact(contact);
            financialRecord.addContact(contact);

            // Then
            assertThat(financialRecord.getContacts()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("update 테스트")
    class UpdateTest {

        @Test
        @DisplayName("업데이트 메소드 호출 시 구분을 제외한 모든 필드가 변경된다")
        void update_validParams_updatesFields() {
            // Given
            FiscalYear fiscalYear = mock(FiscalYear.class);
            Budget budget = mock(Budget.class);
            LocalDate date = LocalDate.of(2023, 1, 1);
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .fiscalYear(fiscalYear)
                    .date(date)
                    .budget(budget)
                    .description("old")
                    .amount(0L)
                    .remarks("old")
                    .build();

            FiscalYear newFiscalYear = mock(FiscalYear.class);
            LocalDate newDate = LocalDate.of(2023, 2, 2);
            Budget newBudget = mock(Budget.class);
            String newDescription = "new description";
            long newAmount = 50000L;
            String newRemarks = "new remarks";

            // When
            financialRecord.update(newFiscalYear, newDate, newBudget, newDescription, newAmount, newRemarks);

            // Then
            assertThat(financialRecord.getType()).isEqualTo(FinancialRecordType.EXPENSE);
            assertThat(financialRecord.getFiscalYear()).isEqualTo(newFiscalYear);
            assertThat(financialRecord.getDate()).isEqualTo(newDate);
            assertThat(financialRecord.getBudget()).isEqualTo(newBudget);
            assertThat(financialRecord.getDescription()).isEqualTo(newDescription);
            assertThat(financialRecord.getAmount()).isEqualTo(newAmount);
            assertThat(financialRecord.getRemarks()).isEqualTo(newRemarks);
        }
    }

    @Nested
    @DisplayName("getLoggableName 테스트")
    class GetLoggableNameTest {

        @Test
        @DisplayName("구분이 수입이면 활동 기록의 논리명으로 '수입'을 반환한다")
        void getLoggableName_income_returnsIncomeLabel() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .build();

            // When
            String name = financialRecord.getLoggableName();

            // Then
            assertThat(name).isEqualTo("수입");
        }

        @Test
        @DisplayName("구분이 지출이면 활동 기록의 논리명으로 '지출'을 반환한다")
        void getLoggableName_expense_returnsExpenseLabel() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .build();

            // When
            String name = financialRecord.getLoggableName();

            // Then
            assertThat(name).isEqualTo("지출");
        }
    }

    @Nested
    @DisplayName("getLoggableFieldName 테스트")
    class GetLoggableFieldNameTest {

        @Test
        @DisplayName("구분이 수입이면 일자는 '수입일', 관계자는 '봉헌자'로 바꿔 반환한다")
        void getLoggableFieldName_income_returnsIncomeFieldNames() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .build();

            // When
            String dateName = financialRecord.getLoggableFieldName("일자");
            String contactsName = financialRecord.getLoggableFieldName("관계자");

            // Then
            assertThat(dateName).isEqualTo("수입일");
            assertThat(contactsName).isEqualTo("봉헌자");
        }

        @Test
        @DisplayName("구분이 지출이면 일자는 '지출일', 관계자는 '수취인'으로 바꿔 반환한다")
        void getLoggableFieldName_expense_returnsExpenseFieldNames() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .build();

            // When
            String dateName = financialRecord.getLoggableFieldName("일자");
            String contactsName = financialRecord.getLoggableFieldName("관계자");

            // Then
            assertThat(dateName).isEqualTo("지출일");
            assertThat(contactsName).isEqualTo("수취인");
        }

        @Test
        @DisplayName("구분에 따라 달라지지 않는 필드는 선언된 논리명을 그대로 반환한다")
        void getLoggableFieldName_otherField_returnsDeclaredName() {
            // Given
            FinancialRecord financialRecord = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .build();

            // When
            String name = financialRecord.getLoggableFieldName("금액");

            // Then
            assertThat(name).isEqualTo("금액");
        }
    }
}
