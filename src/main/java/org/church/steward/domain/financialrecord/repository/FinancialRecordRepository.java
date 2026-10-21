package org.church.steward.domain.financialrecord.repository;

import java.util.List;
import java.util.Optional;

import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FinancialRecordRepository
        extends JpaRepository<FinancialRecord, Long>, JpaSpecificationExecutor<FinancialRecord> {
    /**
     * 회계 연도에 속한 최근 내역 100건을 조회한다. 예산은 함께 조회한다.
     * 관계자는 함께 조회하지 않는다. 컬렉션을 fetch join하면 Hibernate가 건수 제한을 쿼리가 아니라 메모리에서 적용하기
     * 때문이며, 관계자는 처음 접근할 때 batch fetch로 한 번에 조회된다.
     */
    @EntityGraph(attributePaths = "budget")
    List<FinancialRecord> findTop100ByFiscalYearIdOrderByDateDescIdDesc(Long fiscalYearId);

    /**
     * 회계 연도에 속한 내역 전체를 날짜순(같은 날짜는 등록순)으로 조회한다. 예산과 관계자는 함께 조회한다.
     */
    @EntityGraph(attributePaths = { "budget", "contacts" })
    List<FinancialRecord> findAllByFiscalYearIdOrderByDateAscIdAsc(Long fiscalYearId);

    boolean existsByBudgetId(Long budgetId);

    boolean existsByFiscalYearId(Long fiscalYearId);

    /**
     * 회계 연도에 속한 내역 중 날짜가 가장 이른 것을 찾는다.
     */
    Optional<FinancialRecord> findFirstByFiscalYearIdOrderByDateAsc(Long fiscalYearId);

    /**
     * 회계 연도에 속한 내역 중 날짜가 가장 늦은 것을 찾는다.
     */
    Optional<FinancialRecord> findFirstByFiscalYearIdOrderByDateDesc(Long fiscalYearId);

    /**
     * 회계 연도에 속한 수입 또는 지출의 금액을 모두 더한다. 내역이 없으면 0이다.
     */
    @Query("""
            SELECT COALESCE(SUM(r.amount), 0)
              FROM FinancialRecord r
             WHERE r.fiscalYear.id = :fiscalYearId
               AND r.type = :type
            """)
    long sumAmountByFiscalYearIdAndType(@Param("fiscalYearId") Long fiscalYearId,
            @Param("type") FinancialRecordType type);

    /**
     * 회계 연도에 속한 수입 또는 지출의 금액을 예산별로 더한다. 내역은 말단 예산에만 등록되므로 내역이 있는 말단 예산만 나온다.
     */
    @Query("""
            SELECT r.budget.id AS budgetId
                 , SUM(r.amount) AS amount
              FROM FinancialRecord r
             WHERE r.fiscalYear.id = :fiscalYearId
               AND r.type = :type
             GROUP BY r.budget.id
            """)
    List<BudgetAmountProjection> sumAmountGroupByBudget(@Param("fiscalYearId") Long fiscalYearId,
            @Param("type") FinancialRecordType type);
}
