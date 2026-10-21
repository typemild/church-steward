package org.church.steward.domain.financialrecord.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class FinancialRecordRepositoryTest {

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
            Budget budget, String desc, long amount, Contact... contacts) {
        FinancialRecord financialRecord = FinancialRecord.builder()
                .type(type)
                .fiscalYear(fiscalYear)
                .date(date)
                .budget(budget)
                .description(desc)
                .amount(amount)
                .remarks("remarks")
                .build();
        for (Contact contact : contacts) {
            financialRecord.addContact(contact);
        }
        return entityManager.persist(financialRecord);
    }

    @Nested
    @DisplayName("findTop100ByFiscalYearIdOrderByDateDescIdDesc 테스트")
    class FindTop100ByFiscalYearIdOrderByDateDescIdDescTest {
        @Test
        @DisplayName("내역이 100건을 초과할 때, 회계 연도별 상위 100건을 조회한다")
        void findTop100ByFiscalYearIdOrderByDateDescIdDesc_whenMoreThan100RecordsExist_returnTop100() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            for (int i = 1; i <= 110; i++) {
                createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1).plusDays(i),
                        budget, "Desc" + i, 1000, contact);
            }

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYear.getId());

            // Then
            assertThat(result).hasSize(100);
        }

        @Test
        @DisplayName("수입과 지출을 구분하지 않고 날짜가 최근인 내역부터 조회한다")
        void findTop100ByFiscalYearIdOrderByDateDescIdDesc_whenIncomeAndExpenseExist_returnLatestFirst() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget incomeBudget = createBudget(fiscalYear, BudgetType.INCOME, "Income Budget");
            Budget expenseBudget = createBudget(fiscalYear, BudgetType.EXPENSE, "Expense Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), incomeBudget,
                    "Oldest", 1000, contact);
            createFinancialRecord(FinancialRecordType.EXPENSE, fiscalYear, LocalDate.of(2023, 1, 3), expenseBudget,
                    "Latest", 1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 2), incomeBudget,
                    "Middle", 1000, contact);

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYear.getId());

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactly("Latest", "Middle", "Oldest");
        }

        @Test
        @DisplayName("예산을 함께 조회하므로, 내역마다 예산을 따로 조회하지 않아도 예산이 채워져 있다")
        void findTop100ByFiscalYearIdOrderByDateDescIdDesc_withBudget_fetchesBudget() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "Desc",
                    1000, contact);
            entityManager.flush();
            entityManager.clear();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYear.getId());

            // Then
            assertThat(result).allSatisfy(financialRecord -> assertThat(Hibernate.isInitialized(financialRecord
                    .getBudget())).isTrue());
        }

        @Test
        @DisplayName("관계자는 함께 조회하지 않는다. 건수 제한이 있는 쿼리에서 컬렉션을 fetch join하면 Hibernate가 제한을 메모리에서 적용하기 때문이다")
        void findTop100ByFiscalYearIdOrderByDateDescIdDesc_whenLoaded_doesNotFetchContacts() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "Desc",
                    1000, contact);
            entityManager.flush();
            entityManager.clear();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYear.getId());

            // Then
            assertThat(result).allSatisfy(financialRecord -> assertThat(Hibernate.isInitialized(financialRecord
                    .getContacts())).isFalse());
        }

        @Test
        @DisplayName("한 내역의 관계자에 처음 접근하면 조회된 100건 모두의 관계자 목록이 한 번에 채워진다")
        void findTop100ByFiscalYearIdOrderByDateDescIdDesc_whenFirstContactsAccessed_initializesContactsOfAllRecords() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact husband = createContact(ContactType.INDIVIDUAL, "Husband");
            Contact wife = createContact(ContactType.INDIVIDUAL, "Wife");
            for (int i = 1; i <= 110; i++) {
                createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1).plusDays(i),
                        budget, "Desc" + i, 1000, husband, wife);
            }
            entityManager.flush();
            entityManager.clear();
            List<FinancialRecord> result = financialRecordRepository
                    .findTop100ByFiscalYearIdOrderByDateDescIdDesc(fiscalYear.getId());

            // When
            Hibernate.initialize(result.getFirst().getContacts());

            // Then
            assertThat(result).allSatisfy(financialRecord -> assertThat(Hibernate.isInitialized(financialRecord
                    .getContacts())).isTrue());
        }
    }

    @Nested
    @DisplayName("findAllByFiscalYearIdOrderByDateAscIdAsc 테스트")
    class FindAllByFiscalYearIdOrderByDateAscIdAscTest {
        @Test
        @DisplayName("수입과 지출을 구분하지 않고 날짜가 이른 내역부터 조회한다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_whenIncomeAndExpenseExist_returnEarliestFirst() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget incomeBudget = createBudget(fiscalYear, BudgetType.INCOME, "Income Budget");
            Budget expenseBudget = createBudget(fiscalYear, BudgetType.EXPENSE, "Expense Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.EXPENSE, fiscalYear, LocalDate.of(2023, 1, 3), expenseBudget,
                    "Latest", 1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), incomeBudget,
                    "Oldest", 1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 2), incomeBudget,
                    "Middle", 1000, contact);

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactly("Oldest", "Middle", "Latest");
        }

        @Test
        @DisplayName("날짜가 같은 내역은 먼저 등록한 것부터 조회한다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_whenSameDate_returnInRegistrationOrder() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            LocalDate date = LocalDate.of(2023, 1, 1);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, budget, "First", 1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, budget, "Second", 1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, budget, "Third", 1000, contact);

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactly("First", "Second", "Third");
        }

        @Test
        @DisplayName("다른 회계 연도의 내역은 조회하지 않는다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_whenOtherFiscalYearHasRecords_excludeThem() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            FiscalYear otherFiscalYear = createFiscalYear(2024);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Budget otherBudget = createBudget(otherFiscalYear, BudgetType.INCOME, "Other Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "Mine",
                    1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, otherFiscalYear, LocalDate.of(2024, 1, 1), otherBudget,
                    "Other", 1000, contact);

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactly("Mine");
        }

        @Test
        @DisplayName("100건을 넘어도 회계 연도의 내역을 모두 조회한다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_whenMoreThan100RecordsExist_returnAll() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            for (int i = 1; i <= 110; i++) {
                createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1).plusDays(i),
                        budget, "Desc" + i, 1000, contact);
            }

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).hasSize(110);
        }

        @Test
        @DisplayName("회계 연도에 내역이 없으면 빈 목록을 반환한다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_whenYearHasNoRecord_returnEmptyList() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("예산과 관계자를 함께 조회하므로, 내역마다 따로 조회하지 않아도 예산과 관계자 목록이 채워져 있다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_withBudgetAndContacts_fetchesBoth() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "First",
                    1000, contact);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 2), budget, "Second",
                    1000, contact);
            entityManager.flush();
            entityManager.clear();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).allSatisfy(financialRecord -> {
                assertThat(Hibernate.isInitialized(financialRecord.getBudget())).isTrue();
                assertThat(Hibernate.isInitialized(financialRecord.getContacts())).isTrue();
            });
        }

        @Test
        @DisplayName("관계자를 함께 조회해도 관계자의 수만큼 같은 내역이 중복되어 나오지 않고, 날짜순을 유지한다")
        void findAllByFiscalYearIdOrderByDateAscIdAsc_withMultipleContacts_returnsEachRecordOnceInOrder() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact husband = createContact(ContactType.INDIVIDUAL, "Husband");
            Contact wife = createContact(ContactType.INDIVIDUAL, "Wife");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 2), budget, "Later",
                    1000, husband, wife);
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "Earlier",
                    1000, husband, wife);
            entityManager.flush();
            entityManager.clear();

            // When
            List<FinancialRecord> result = financialRecordRepository
                    .findAllByFiscalYearIdOrderByDateAscIdAsc(fiscalYear.getId());

            // Then
            assertThat(result).extracting(FinancialRecord::getDescription)
                    .containsExactly("Earlier", "Later");
        }
    }

    @Nested
    @DisplayName("existsByBudgetId 테스트")
    class ExistsByBudgetIdTest {
        @Test
        @DisplayName("예산에 내역이 존재하면 true를 반환한다")
        void existsByBudgetId_whenBudgetHasRecord_returnTrue() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.EXPENSE, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.EXPENSE, fiscalYear, LocalDate.of(2023, 1, 1), budget, "D1", 1000,
                    contact);

            // When
            boolean exists = financialRecordRepository.existsByBudgetId(budget.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("예산에 내역이 존재하지 않으면 false를 반환한다")
        void existsByBudgetId_whenBudgetHasNoRecord_returnFalse() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");

            // When
            boolean exists = financialRecordRepository.existsByBudgetId(budget.getId());

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByFiscalYearId 테스트")
    class ExistsByFiscalYearIdTest {
        @Test
        @DisplayName("회계 연도에 내역이 존재하면 true를 반환한다")
        void existsByFiscalYearId_whenYearHasRecord_returnTrue() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);
            Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
            Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, LocalDate.of(2023, 1, 1), budget, "D1", 1000,
                    contact);

            // When
            boolean exists = financialRecordRepository.existsByFiscalYearId(fiscalYear.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("회계 연도에 내역이 존재하지 않으면 false를 반환한다")
        void existsByFiscalYearId_whenYearHasNoRecord_returnFalse() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);

            // When
            boolean exists = financialRecordRepository.existsByFiscalYearId(fiscalYear.getId());

            // Then
            assertThat(exists).isFalse();
        }
    }

    /**
     * 회계 연도 "2023년"에 주어진 날짜들의 내역을, 다른 회계 연도 "2024년"에 2022년 1월 1일과 2025년 12월 31일의
     * 내역을 저장한다.
     *
     * @return 회계 연도 "2023년"
     */
    private FiscalYear createRecordsOn(LocalDate... dates) {
        FiscalYear fiscalYear = createFiscalYear(2023);
        FiscalYear otherFiscalYear = createFiscalYear(2024);
        Budget budget = createBudget(fiscalYear, BudgetType.INCOME, "Budget");
        Budget otherBudget = createBudget(otherFiscalYear, BudgetType.INCOME, "Other Budget");
        Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");
        for (LocalDate date : dates) {
            createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, budget, "Desc", 1000, contact);
        }
        createFinancialRecord(FinancialRecordType.INCOME, otherFiscalYear, LocalDate.of(2022, 1, 1), otherBudget,
                "Other", 1000, contact);
        createFinancialRecord(FinancialRecordType.INCOME, otherFiscalYear, LocalDate.of(2025, 12, 31), otherBudget,
                "Other", 1000, contact);
        return fiscalYear;
    }

    @Nested
    @DisplayName("findFirstByFiscalYearIdOrderByDateAsc 테스트")
    class FindFirstByFiscalYearIdOrderByDateAscTest {
        @Test
        @DisplayName("다른 회계 연도의 내역은 제외하고 그 회계 연도에서 날짜가 가장 이른 내역을 조회한다")
        void findFirstByFiscalYearIdOrderByDateAsc_whenRecordsExist_returnEarliest() {
            // Given
            FiscalYear fiscalYear = createRecordsOn(LocalDate.of(2023, 5, 1), LocalDate.of(2022, 12, 3),
                    LocalDate.of(2023, 11, 30));

            // When
            Optional<FinancialRecord> result = financialRecordRepository
                    .findFirstByFiscalYearIdOrderByDateAsc(fiscalYear.getId());

            // Then
            assertThat(result).map(FinancialRecord::getDate)
                    .contains(LocalDate.of(2022, 12, 3));
        }

        @Test
        @DisplayName("회계 연도에 내역이 없으면 빈 Optional을 반환한다")
        void findFirstByFiscalYearIdOrderByDateAsc_whenYearHasNoRecord_returnEmptyOptional() {
            // Given
            FiscalYear fiscalYear = createRecordsOn();

            // When
            Optional<FinancialRecord> result = financialRecordRepository
                    .findFirstByFiscalYearIdOrderByDateAsc(fiscalYear.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFirstByFiscalYearIdOrderByDateDesc 테스트")
    class FindFirstByFiscalYearIdOrderByDateDescTest {
        @Test
        @DisplayName("다른 회계 연도의 내역은 제외하고 그 회계 연도에서 날짜가 가장 늦은 내역을 조회한다")
        void findFirstByFiscalYearIdOrderByDateDesc_whenRecordsExist_returnLatest() {
            // Given
            FiscalYear fiscalYear = createRecordsOn(LocalDate.of(2023, 5, 1), LocalDate.of(2022, 12, 3),
                    LocalDate.of(2023, 11, 30));

            // When
            Optional<FinancialRecord> result = financialRecordRepository
                    .findFirstByFiscalYearIdOrderByDateDesc(fiscalYear.getId());

            // Then
            assertThat(result).map(FinancialRecord::getDate)
                    .contains(LocalDate.of(2023, 11, 30));
        }

        @Test
        @DisplayName("회계 연도에 내역이 없으면 빈 Optional을 반환한다")
        void findFirstByFiscalYearIdOrderByDateDesc_whenYearHasNoRecord_returnEmptyOptional() {
            // Given
            FiscalYear fiscalYear = createRecordsOn();

            // When
            Optional<FinancialRecord> result = financialRecordRepository
                    .findFirstByFiscalYearIdOrderByDateDesc(fiscalYear.getId());

            // Then
            assertThat(result).isEmpty();
        }
    }

    /**
     * 회계 연도 "2023년"의 수입 예산 A에 1,000원과 2,000원, 수입 예산 B에 4,000원, 지출 예산에 500원을 저장하고,
     * 다른 회계 연도 "2024년"의 수입 예산에 9,000원을 저장한다.
     *
     * @return 회계 연도 "2023년"
     */
    private FiscalYear createRecordsForSum() {
        FiscalYear fiscalYear = createFiscalYear(2023);
        FiscalYear otherFiscalYear = createFiscalYear(2024);
        Budget incomeBudgetA = createBudget(fiscalYear, BudgetType.INCOME, "Income A");
        Budget incomeBudgetB = createBudget(fiscalYear, BudgetType.INCOME, "Income B");
        Budget expenseBudget = createBudget(fiscalYear, BudgetType.EXPENSE, "Expense");
        Budget otherBudget = createBudget(otherFiscalYear, BudgetType.INCOME, "Other");
        Contact contact = createContact(ContactType.INDIVIDUAL, "Contact");

        LocalDate date = LocalDate.of(2023, 3, 1);
        createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, incomeBudgetA, "A1", 1000, contact);
        createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, incomeBudgetA, "A2", 2000, contact);
        createFinancialRecord(FinancialRecordType.INCOME, fiscalYear, date, incomeBudgetB, "B1", 4000, contact);
        createFinancialRecord(FinancialRecordType.EXPENSE, fiscalYear, date, expenseBudget, "E1", 500, contact);
        createFinancialRecord(FinancialRecordType.INCOME, otherFiscalYear, LocalDate.of(2024, 3, 1), otherBudget,
                "Other", 9000, contact);
        return fiscalYear;
    }

    @Nested
    @DisplayName("sumAmountByFiscalYearIdAndType 테스트")
    class SumAmountByFiscalYearIdAndTypeTest {
        @Test
        @DisplayName("다른 회계 연도와 다른 구분의 내역은 제외하고 그 회계 연도의 수입 금액을 모두 더한다")
        void sumAmountByFiscalYearIdAndType_whenIncomeRecordsExist_returnIncomeSum() {
            // Given
            FiscalYear fiscalYear = createRecordsForSum();

            // When
            long sum = financialRecordRepository.sumAmountByFiscalYearIdAndType(fiscalYear.getId(),
                    FinancialRecordType.INCOME);

            // Then
            assertThat(sum).isEqualTo(7000L);
        }

        @Test
        @DisplayName("그 회계 연도의 지출 금액을 모두 더한다")
        void sumAmountByFiscalYearIdAndType_whenExpenseRecordsExist_returnExpenseSum() {
            // Given
            FiscalYear fiscalYear = createRecordsForSum();

            // When
            long sum = financialRecordRepository.sumAmountByFiscalYearIdAndType(fiscalYear.getId(),
                    FinancialRecordType.EXPENSE);

            // Then
            assertThat(sum).isEqualTo(500L);
        }

        @Test
        @DisplayName("회계 연도에 내역이 없으면 0을 반환한다")
        void sumAmountByFiscalYearIdAndType_whenNoRecord_returnZero() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);

            // When
            long sum = financialRecordRepository.sumAmountByFiscalYearIdAndType(fiscalYear.getId(),
                    FinancialRecordType.INCOME);

            // Then
            assertThat(sum).isZero();
        }
    }

    @Nested
    @DisplayName("sumAmountGroupByBudget 테스트")
    class SumAmountGroupByBudgetTest {
        @Test
        @DisplayName("그 회계 연도의 수입 금액을 예산별로 더한다")
        void sumAmountGroupByBudget_whenIncomeRecordsExist_returnSumPerBudget() {
            // Given
            FiscalYear fiscalYear = createRecordsForSum();

            // When
            List<BudgetAmountProjection> result = financialRecordRepository.sumAmountGroupByBudget(fiscalYear.getId(),
                    FinancialRecordType.INCOME);

            // Then
            assertThat(result).extracting(BudgetAmountProjection::getAmount)
                    .containsExactlyInAnyOrder(3000L, 4000L);
        }

        @Test
        @DisplayName("회계 연도에 내역이 없으면 빈 목록을 반환한다")
        void sumAmountGroupByBudget_whenNoRecord_returnEmptyList() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2023);

            // When
            List<BudgetAmountProjection> result = financialRecordRepository.sumAmountGroupByBudget(fiscalYear.getId(),
                    FinancialRecordType.INCOME);

            // Then
            assertThat(result).isEmpty();
        }
    }

}
