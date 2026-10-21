package org.church.steward.domain.budget.repository;

import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    /**
     * 회계 연도에 속한 예산을 모두 조회한다. 하위 예산 목록(children)도 함께 조회한다.
     * 하위 예산은 같은 회계 연도에 속해 결과에 함께 들어 있으므로, 쿼리 한 번으로 모든 예산의 하위 예산 목록이 채워진다.
     */
    @EntityGraph(attributePaths = "children")
    List<Budget> findAllByFiscalYearId(Long fiscalYearId);

    /**
     * 회계 연도에 속한 수입 또는 지출 예산을 모두 조회한다. 하위 예산 목록(children)도 함께 조회한다.
     * 하위 예산은 상위 예산과 회계 연도와 유형이 같아 결과에 함께 들어 있으므로, 쿼리 한 번으로 모든 예산의 하위 예산 목록이 채워진다.
     */
    @EntityGraph(attributePaths = "children")
    List<Budget> findAllByFiscalYearIdAndType(Long fiscalYearId, BudgetType type);

    boolean existsByParentId(Long parentId);

    @Query("""
            SELECT MAX(b.sortOrder)
              FROM Budget b
             WHERE b.fiscalYear.id = :fiscalYearId
               AND (
                       (:parentId IS NULL AND b.parent IS NULL)
                       OR (b.parent.id = :parentId)
                   )
            """)
    Integer findMaxSortOrder(@Param("fiscalYearId") Long fiscalYearId, @Param("parentId") Long parentId);

    List<Budget> findAllByFiscalYearIdAndParentIdOrderBySortOrderAsc(Long fiscalYearId, Long parentId);

    List<Budget> findAllByFiscalYearIdAndTypeAndNameContaining(Long fiscalYearId, BudgetType type, String name);

    @Query(value = """
            WITH RECURSIVE budget_tree(id) AS (
                SELECT id
                  FROM budget
                 WHERE id = :budgetId
                 UNION ALL
                SELECT b.id
                  FROM budget b
                 INNER JOIN budget_tree bt
                    ON b.parent_id = bt.id
            )
            SELECT id FROM budget_tree
            """, nativeQuery = true)
    List<Long> findAllDescendantIds(@Param("budgetId") Long budgetId);
}
