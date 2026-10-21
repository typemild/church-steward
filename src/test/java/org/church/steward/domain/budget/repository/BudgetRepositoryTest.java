package org.church.steward.domain.budget.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class BudgetRepositoryTest {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findAllByFiscalYearId 테스트")
    class FindAllByFiscalYearIdTest {

        @Test
        @DisplayName("특정 회계 연도의 모든 예산을 조회한다")
        void findAllByFiscalYearId_ShouldReturnBudgetsForFiscalYear() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            FiscalYear otherFiscalYear = createFiscalYear(2025);
            createBudget(fiscalYear, BudgetType.INCOME, "수입1", null, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "지출1", null, 1);
            createBudget(otherFiscalYear, BudgetType.INCOME, "수입2025", null, 1);
            entityManager.flush();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearId(fiscalYear.getId());

            // Then
            assertThat(result).extracting(Budget::getName)
                    .containsExactlyInAnyOrder("수입1", "지출1");
        }

        @Test
        @DisplayName("하위 예산 목록을 함께 조회하므로, 예산마다 하위 예산 목록을 따로 조회하지 않아도 모든 예산의 하위 예산 목록이 채워져 있다")
        void findAllByFiscalYearId_withChildren_fetchesChildrenOfAllBudgets() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.INCOME, "상위", null, 1);
            Budget child = createBudget(fiscalYear, BudgetType.INCOME, "중간", parent, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "말단", child, 1);
            entityManager.clear();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearId(fiscalYear.getId());

            // Then
            assertThat(result).allSatisfy(budget -> assertThat(Hibernate.isInitialized(budget.getChildren()))
                    .isTrue());
        }

        @Test
        @DisplayName("하위 예산 목록을 함께 조회해도 하위 예산의 수만큼 같은 예산이 중복되어 나오지 않는다")
        void findAllByFiscalYearId_withMultipleChildren_returnsEachBudgetOnce() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.EXPENSE, "상위", null, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "하위1", parent, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "하위2", parent, 2);
            entityManager.clear();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearId(fiscalYear.getId());

            // Then
            assertThat(result).extracting(Budget::getName)
                    .containsExactlyInAnyOrder("상위", "하위1", "하위2");
        }
    }

    @Nested
    @DisplayName("findAllByFiscalYearIdAndType 테스트")
    class FindAllByFiscalYearIdAndTypeTest {

        @Test
        @DisplayName("특정 회계 연도와 유형의 예산을 조회한다")
        void findAllByFiscalYearIdAndType_ShouldReturnBudgetsByFiscalYearAndType() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            FiscalYear otherFiscalYear = createFiscalYear(2025);
            BudgetType type = BudgetType.INCOME;
            createBudget(fiscalYear, type, "수입1", null, 1);
            createBudget(fiscalYear, type, "수입2", null, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "지출1", null, 1);
            createBudget(otherFiscalYear, type, "수입2025", null, 1);
            entityManager.flush();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearIdAndType(fiscalYear.getId(), type);

            // Then
            assertThat(result).extracting(Budget::getName)
                    .containsExactlyInAnyOrder("수입1", "수입2");
        }

        @Test
        @DisplayName("하위 예산 목록을 함께 조회하므로, 예산마다 하위 예산 목록을 따로 조회하지 않아도 모든 예산의 하위 예산 목록이 채워져 있다")
        void findAllByFiscalYearIdAndType_withChildren_fetchesChildrenOfAllBudgets() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.INCOME, "상위", null, 1);
            Budget child = createBudget(fiscalYear, BudgetType.INCOME, "중간", parent, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "말단", child, 1);
            entityManager.clear();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearIdAndType(fiscalYear.getId(), BudgetType.INCOME);

            // Then
            assertThat(result).allSatisfy(budget -> assertThat(Hibernate.isInitialized(budget.getChildren()))
                    .isTrue());
        }

        @Test
        @DisplayName("하위 예산 목록을 함께 조회해도 하위 예산의 수만큼 같은 예산이 중복되어 나오지 않는다")
        void findAllByFiscalYearIdAndType_withMultipleChildren_returnsEachBudgetOnce() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.EXPENSE, "상위", null, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "하위1", parent, 1);
            createBudget(fiscalYear, BudgetType.EXPENSE, "하위2", parent, 2);
            entityManager.clear();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearIdAndType(fiscalYear.getId(),
                    BudgetType.EXPENSE);

            // Then
            assertThat(result).extracting(Budget::getName)
                    .containsExactlyInAnyOrder("상위", "하위1", "하위2");
        }
    }

    @Nested
    @DisplayName("existsByParentId 테스트")
    class ExistsByParentIdTest {

        @Test
        @DisplayName("상위 예산 ID가 존재하는 경우 true를 반환한다")
        void existsByParentId_ShouldReturnTrueIfParentExists() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.INCOME, "상위", null, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "자식", parent, 1);
            entityManager.flush();

            // When
            boolean exists = budgetRepository.existsByParentId(parent.getId());

            // Then
            assertThat(exists).isTrue();
        }
    }

    @Nested
    @DisplayName("findMaxSortOrder 테스트")
    class FindMaxSortOrderTest {

        @Test
        @DisplayName("특정 회계 연도 및 상위 예산 내의 최대 sortOrder를 조회한다")
        void findMaxSortOrder_ShouldReturnMaxOrder() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.INCOME, "상위", null, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "자식1", parent, 5);
            createBudget(fiscalYear, BudgetType.INCOME, "자식2", parent, 10);
            createBudget(fiscalYear, BudgetType.INCOME, "다른노드", null, 15); // parent와 상관없음
            entityManager.flush();

            // When
            Integer maxOrder = budgetRepository.findMaxSortOrder(fiscalYear.getId(), parent.getId());

            // Then
            assertThat(maxOrder).isEqualTo(10);
        }

        @Test
        @DisplayName("최상위 예산(parent IS NULL)의 최대 sortOrder를 조회한다")
        void findMaxSortOrder_ShouldReturnMaxOrderForRoot() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            createBudget(fiscalYear, BudgetType.INCOME, "루트1", null, 3);
            createBudget(fiscalYear, BudgetType.INCOME, "루트2", null, 7);
            createBudget(fiscalYear, BudgetType.INCOME, "자식",
                    createBudget(fiscalYear, BudgetType.INCOME, "루트3", null, 1), 1);
            entityManager.flush();

            // When
            Integer maxOrder = budgetRepository.findMaxSortOrder(fiscalYear.getId(), null);

            // Then
            assertThat(maxOrder).isEqualTo(7);
        }

        @Test
        @DisplayName("다른 회계 연도의 예산은 최대 sortOrder에 넣지 않는다")
        void findMaxSortOrder_ShouldIgnoreOtherFiscalYears() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            FiscalYear otherFiscalYear = createFiscalYear(2025);
            createBudget(fiscalYear, BudgetType.INCOME, "루트1", null, 3);
            createBudget(otherFiscalYear, BudgetType.INCOME, "다른 회계 연도의 루트", null, 9);
            entityManager.flush();

            // When
            Integer maxOrder = budgetRepository.findMaxSortOrder(fiscalYear.getId(), null);

            // Then
            assertThat(maxOrder).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc 테스트")
    class FindAllByFiscalYearIdAndParentIdOrderBySortOrderAscTest {

        @Test
        @DisplayName("특정 상위 예산의 자식들을 정렬 순서대로 조회한다")
        void findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc_ShouldReturnSortedChildren() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget parent = createBudget(fiscalYear, BudgetType.INCOME, "상위", null, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "자식2", parent, 2);
            createBudget(fiscalYear, BudgetType.INCOME, "자식1", parent, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "자식3", parent, 3);
            entityManager.flush();

            // When
            List<Budget> result = budgetRepository
                    .findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(fiscalYear.getId(), parent.getId());

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Budget::getName).containsExactly("자식1", "자식2", "자식3");
        }
    }

    @Nested
    @DisplayName("findAllByFiscalYearIdAndTypeAndNameContaining 테스트")
    class FindAllByFiscalYearIdAndTypeAndNameContainingTest {

        @Test
        @DisplayName("특정 조건에 맞는 이름 포함 예산을 조회한다")
        void findAllByFiscalYearIdAndTypeAndNameContaining_ShouldReturnFilteredBudgets() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            BudgetType type = BudgetType.EXPENSE;
            createBudget(fiscalYear, type, "식비", null, 1);
            createBudget(fiscalYear, type, "교통비", null, 1);
            createBudget(fiscalYear, type, "비품비", null, 1);
            createBudget(fiscalYear, BudgetType.INCOME, "식비(수입)", null, 1);
            entityManager.flush();

            // When
            List<Budget> result = budgetRepository.findAllByFiscalYearIdAndTypeAndNameContaining(fiscalYear.getId(),
                    type, "비품");

            // Then
            assertThat(result).hasSize(1);
            assertThat(result).extracting(Budget::getName).containsExactlyInAnyOrder("비품비");
        }
    }

    @Nested
    @DisplayName("findAllDescendantIds 테스트")
    class FindAllDescendantIdsTest {
        @Test
        @DisplayName("예산 계층이 존재할 때, 본인을 포함한 모든 하위 계층 ID를 조회한다")
        void findAllDescendantIds_whenHierarchyExists_returnAllIdsIncludingSelf() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget root = createBudget(fiscalYear, BudgetType.INCOME, "Root", null, 1);
            Budget child = createBudget(fiscalYear, BudgetType.INCOME, "Child", root, 1);
            Budget grandchild = createBudget(fiscalYear, BudgetType.INCOME, "Grandchild", child, 1);
            entityManager.flush();

            // When
            List<Long> result = budgetRepository.findAllDescendantIds(root.getId());

            // Then
            assertThat(result).containsExactlyInAnyOrder(root.getId(), child.getId(), grandchild.getId());
        }

        @Test
        @DisplayName("하위 항목이 없는 예산의 경우, 본인의 ID만 조회한다")
        void findAllDescendantIds_whenNoChildren_returnOnlySelfId() {
            // Given
            FiscalYear fiscalYear = createFiscalYear(2024);
            Budget leaf = createBudget(fiscalYear, BudgetType.INCOME, "Leaf", null, 1);
            entityManager.flush();

            // When
            List<Long> result = budgetRepository.findAllDescendantIds(leaf.getId());

            // Then
            assertThat(result).containsExactly(leaf.getId());
        }
    }

    /**
     * 이름이 "2024년"이고 기간이 그 해 1월 1일 ~ 12월 31일인 회계 연도를 저장한다.
     */
    private FiscalYear createFiscalYear(int year) {
        return entityManager.persistAndFlush(FiscalYear.builder()
                .name(year + "년")
                .startDate(LocalDate.of(year, 1, 1))
                .endDate(LocalDate.of(year, 12, 31))
                .build());
    }

    private Budget createBudget(FiscalYear fiscalYear, BudgetType type, String name, Budget parent, int sortOrder) {
        Budget budget = Budget.builder()
                .fiscalYear(fiscalYear)
                .type(type)
                .name(name)
                .parent(parent)
                .sortOrder(sortOrder)
                .amount(10000L)
                .build();
        return entityManager.persistAndFlush(budget);
    }
}
