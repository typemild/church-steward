package org.church.steward.domain.financialrecord.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class FinancialRecordContactRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FinancialRecordContactRepository financialRecordContactRepository;

    private Contact createContact() {
        return entityManager.persist(Contact.builder()
                .name("홍길동")
                .type(ContactType.INDIVIDUAL)
                .build());
    }

    /**
     * 주어진 구분의 내역을 만들고 관계자를 연결한다.
     */
    private void createFinancialRecordWithContact(FinancialRecordType type, Contact contact) {
        FiscalYear fiscalYear = entityManager.persist(FiscalYear.builder()
                .name("2024년 " + type.getLabel())
                .startDate(LocalDate.of(2024, 1, 1))
                .endDate(LocalDate.of(2024, 12, 31))
                .build());

        Budget budget = entityManager.persist(Budget.builder()
                .name("테스트 예산")
                .fiscalYear(fiscalYear)
                .type(type == FinancialRecordType.INCOME ? BudgetType.INCOME : BudgetType.EXPENSE)
                .build());

        FinancialRecord financialRecord = entityManager.persist(FinancialRecord.builder()
                .type(type)
                .fiscalYear(fiscalYear)
                .date(LocalDate.now())
                .budget(budget)
                .description("테스트 내역")
                .amount(10000L)
                .build());

        entityManager.persist(FinancialRecordContact.builder()
                .financialRecord(financialRecord)
                .contact(contact)
                .contactName(contact.getName())
                .build());
        entityManager.flush();
    }

    @Nested
    @DisplayName("existsByContactId 테스트")
    class ExistsByContactIdTest {

        @Test
        @DisplayName("관계자가 수입 내역의 봉헌자로 등록되어 있으면 true를 반환한다")
        void existsByContactId_existsInIncome_returnsTrue() {
            // Given
            Contact contact = createContact();
            createFinancialRecordWithContact(FinancialRecordType.INCOME, contact);

            // When
            boolean exists = financialRecordContactRepository.existsByContactId(contact.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("관계자가 지출 내역의 수취인으로 등록되어 있으면 true를 반환한다")
        void existsByContactId_existsInExpense_returnsTrue() {
            // Given
            Contact contact = createContact();
            createFinancialRecordWithContact(FinancialRecordType.EXPENSE, contact);

            // When
            boolean exists = financialRecordContactRepository.existsByContactId(contact.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("관계자가 어떤 내역에도 등록되어 있지 않으면 false를 반환한다")
        void existsByContactId_notExists_returnsFalse() {
            // Given
            Contact contact = createContact();

            // When
            boolean exists = financialRecordContactRepository.existsByContactId(contact.getId());

            // Then
            assertThat(exists).isFalse();
        }
    }
}
