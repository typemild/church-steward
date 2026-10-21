package org.church.steward.domain.financialrecord.specification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.financialrecord.repository.FinancialRecordRepository;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class FinancialRecordSpecificationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FinancialRecordRepository financialRecordRepository;

    /**
     * 이름이 "2023년"이고 기간이 그 해 1월 1일 ~ 12월 31일인 회계 연도를 저장한다.
     */
    private FiscalYear createFiscalYear(int year) {
        FiscalYear fiscalYear = FiscalYear.builder()
                .name(year + "년")
                .startDate(LocalDate.of(year, 1, 1))
                .endDate(LocalDate.of(year, 12, 31))
                .build();
        return entityManager.persist(fiscalYear);
    }

    private Budget createBudget(FiscalYear fiscalYear, BudgetType type, String name) {
        Budget budget = Budget.builder()
                .fiscalYear(fiscalYear)
                .type(type)
                .name(name)
                .sortOrder(0)
                .build();
        return entityManager.persist(budget);
    }

    private Contact createContact(ContactType type, String name) {
        Contact contact = Contact.builder()
                .type(type)
                .name(name)
                .isHidden(false)
                .build();
        return entityManager.persist(contact);
    }

    private FinancialRecord createFinancialRecord(FinancialRecordType type, FiscalYear fiscalYear, LocalDate date,
            Budget budget, String desc, long amount, String remarks) {
        FinancialRecord financialRecord = FinancialRecord.builder()
                .type(type)
                .fiscalYear(fiscalYear)
                .date(date)
                .budget(budget)
                .description(desc)
                .amount(amount)
                .remarks(remarks)
                .build();
        return entityManager.persist(financialRecord);
    }

    private FinancialRecord createIncome(FiscalYear fiscalYear, LocalDate date, Budget budget, String desc,
            long amount, String remarks) {
        return createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, budget, desc, amount, remarks);
    }

    @Nested
    @DisplayName("searchBy 테스트")
    class SearchByTest {

        @Test
        @DisplayName("구분으로 검색하면 해당 구분의 내역만 조회한다")
        void searchBy_type_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget incomeBudget = createBudget(fy, BudgetType.INCOME, "Income Budget");
            Budget expenseBudget = createBudget(fy, BudgetType.EXPENSE, "Expense Budget");
            createFinancialRecord(FinancialRecordType.INCOME, fy, LocalDate.of(2023, 5, 1), incomeBudget, "Income",
                    1000, "Remark");
            createFinancialRecord(FinancialRecordType.EXPENSE, fy, LocalDate.of(2023, 5, 1), expenseBudget, "Expense",
                    1000, "Remark");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("Expense");
        }

        @Test
        @DisplayName("구분을 지정하지 않으면 수입과 지출을 모두 조회한다")
        void searchBy_nullType_returnsIncomeAndExpense() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget incomeBudget = createBudget(fy, BudgetType.INCOME, "Income Budget");
            Budget expenseBudget = createBudget(fy, BudgetType.EXPENSE, "Expense Budget");
            createFinancialRecord(FinancialRecordType.INCOME, fy, LocalDate.of(2023, 5, 1), incomeBudget, "Income",
                    1000, "Remark");
            createFinancialRecord(FinancialRecordType.EXPENSE, fy, LocalDate.of(2023, 5, 1), expenseBudget, "Expense",
                    1000, "Remark");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactlyInAnyOrder("Income", "Expense");
        }

        @Test
        @DisplayName("회계 연도로 검색하면 해당 회계 연도의 내역만 조회한다")
        void searchBy_fiscalYear_returnsMatchingRecords() {
            // Given
            FiscalYear fy2023 = createFiscalYear(2023);
            FiscalYear fy2024 = createFiscalYear(2024);
            Budget budget = createBudget(fy2023, BudgetType.INCOME, "Budget");
            createIncome(fy2023, LocalDate.of(2023, 5, 1), budget, "Desc", 1000, "Remark");
            createIncome(fy2024, LocalDate.of(2024, 5, 1), budget, "Desc", 1000, "Remark");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .fiscalYearId(fy2023.getId())
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFiscalYear().getId()).isEqualTo(fy2023.getId());
        }

        @Test
        @DisplayName("날짜 범위로 검색하면 해당 기간 내의 내역만 조회한다")
        void searchBy_dateRange_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget = createBudget(fy, BudgetType.INCOME, "Budget");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget, "D1", 1000, "R1");
            createIncome(fy, LocalDate.of(2023, 6, 1), budget, "D2", 1000, "R2");
            createIncome(fy, LocalDate.of(2023, 12, 31), budget, "D3", 1000, "R3");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .startDate(LocalDate.of(2023, 5, 1))
                    .endDate(LocalDate.of(2023, 7, 1))
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("D2");
        }

        @Test
        @DisplayName("예산 ID 목록으로 검색하면 해당 예산에 속한 내역만 조회한다")
        void searchBy_budgetIds_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget1 = createBudget(fy, BudgetType.INCOME, "B1");
            Budget budget2 = createBudget(fy, BudgetType.INCOME, "B2");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget1, "D1", 1000, "R1");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget2, "D2", 1000, "R2");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .build();
            List<Long> budgetIds = List.of(budget1.getId());

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, budgetIds));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getBudget().getId()).isEqualTo(budget1.getId());
        }

        @Test
        @DisplayName("설명(description) 키워드로 검색하면 해당 키워드를 포함한 내역만 조회한다")
        void searchBy_description_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget = createBudget(fy, BudgetType.INCOME, "B");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget, "Apple", 1000, "R");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget, "Banana", 1000, "R");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .description("ppl")
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).contains("Apple");
        }

        @Test
        @DisplayName("비고(remarks) 키워드로 검색하면 해당 키워드를 포함한 내역만 조회한다")
        void searchBy_remarks_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget = createBudget(fy, BudgetType.INCOME, "B");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget, "D1", 1000, "Important");
            createIncome(fy, LocalDate.of(2023, 1, 1), budget, "D2", 1000, "Normal");

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .remarks("portant")
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getRemarks()).contains("Important");
        }

        @Test
        @DisplayName("관계자 ID 목록으로 검색하면 해당 관계자가 포함된 내역만 조회한다")
        void searchBy_contactIds_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget = createBudget(fy, BudgetType.INCOME, "B");
            Contact contact1 = createContact(ContactType.INDIVIDUAL, "C1");
            Contact contact2 = createContact(ContactType.INDIVIDUAL, "C2");

            FinancialRecord income1 = createIncome(fy, LocalDate.of(2023, 1, 1), budget, "D1", 1000, "R1");
            income1.addContact(contact1);
            income1.addContact(contact2);

            FinancialRecord income2 = createIncome(fy, LocalDate.of(2023, 1, 1), budget, "D2", 1000, "R2");
            income2.addContact(contact1);

            List<Long> contactIds = List.of(contact2.getId());

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .contactIds(contactIds)
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getDescription()).isEqualTo("D1");
        }

        @Test
        @DisplayName("관계자 ID 목록으로 검색하면 수입의 봉헌자와 지출의 수취인을 모두 찾는다")
        void searchBy_contactIds_returnsIncomeAndExpenseOfContact() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget incomeBudget = createBudget(fy, BudgetType.INCOME, "Income Budget");
            Budget expenseBudget = createBudget(fy, BudgetType.EXPENSE, "Expense Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "C1");
            Contact otherContact = createContact(ContactType.INDIVIDUAL, "C2");

            FinancialRecord income = createFinancialRecord(FinancialRecordType.INCOME, fy, LocalDate.of(2023, 1, 1),
                    incomeBudget, "Income", 1000, "R");
            income.addContact(contact);
            FinancialRecord expense = createFinancialRecord(FinancialRecordType.EXPENSE, fy, LocalDate.of(2023, 1, 1),
                    expenseBudget, "Expense", 1000, "R");
            expense.addContact(contact);
            FinancialRecord otherExpense = createFinancialRecord(FinancialRecordType.EXPENSE, fy,
                    LocalDate.of(2023, 1, 1), expenseBudget, "Other Expense", 1000, "R");
            otherExpense.addContact(otherContact);

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .contactIds(List.of(contact.getId()))
                    .build();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, null));

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactlyInAnyOrder("Income", "Expense");
        }

        @Test
        @DisplayName("모든 필터 조건이 일치하는 내역만 조회한다")
        void searchBy_allFilters_returnsMatchingRecords() {
            // Given
            FiscalYear fy = createFiscalYear(2023);
            Budget budget = createBudget(fy, BudgetType.INCOME, "B");
            Contact contact = createContact(ContactType.INDIVIDUAL, "C");

            FinancialRecord income = createIncome(fy, LocalDate.of(2023, 5, 5), budget, "Target Desc", 1000,
                    "Target Remark");
            income.addContact(contact);

            List<Long> contactIds = List.of(contact.getId());

            FinancialRecordSearchRequestDto dto = FinancialRecordSearchRequestDto.builder()
                    .type(FinancialRecordType.INCOME)
                    .fiscalYearId(fy.getId())
                    .startDate(LocalDate.of(2023, 5, 1))
                    .endDate(LocalDate.of(2023, 5, 31))
                    .budgetId(budget.getId())
                    .description("Target")
                    .remarks("Remark")
                    .contactIds(contactIds)
                    .build();
            List<Long> budgetIds = List.of(budget.getId());

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAll(FinancialRecordSpecification.searchBy(dto, budgetIds));

            // Then
            assertThat(result).hasSize(1);
        }
    }
}
