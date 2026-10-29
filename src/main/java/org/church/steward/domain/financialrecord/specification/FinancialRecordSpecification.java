package org.church.steward.domain.financialrecord.specification;

import java.util.ArrayList;
import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;

public class FinancialRecordSpecification {

    /**
     * 검색 조건에 맞는 수입/지출 내역을 찾는 Specification을 만든다. 금액 조건은 여기서 다루지 않는다.
     *
     * @param dto       검색 조건
     * @param budgetIds 하위 예산까지 확장한 예산 ID 목록. null이거나 비어 있으면 예산 조건을 적용하지 않는다.
     */
    public static Specification<FinancialRecord> searchBy(FinancialRecordSearchRequestDto dto, List<Long> budgetIds) {
        return (root, query, cb) -> {
            List<Long> contactIds = dto.getContactIds();
            List<Predicate> predicates = new ArrayList<>();

            // N+1 방지를 위한 Fetch Join (count 쿼리 시에는 제외)
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("budget", JoinType.LEFT);
                root.fetch("contacts", JoinType.LEFT);
                query.distinct(true);
            }

            // 1. 구분
            if (dto.getType() != null) {
                predicates.add(cb.equal(root.get("type"), dto.getType()));
            }

            // 2. 회계 연도
            if (dto.getFiscalYearId() != null) {
                predicates.add(cb.equal(root.get("fiscalYear").get("id"), dto.getFiscalYearId()));
            }

            // 3. 날짜 범위
            if (dto.getStartDate() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), dto.getStartDate()));
            }
            if (dto.getEndDate() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), dto.getEndDate()));
            }

            // 4. 예산 ID 목록 (IN 절)
            if (budgetIds != null && !budgetIds.isEmpty()) {
                predicates.add(root.get("budget").get("id").in(budgetIds));
            }

            // 5. 설명 (LIKE)
            if (dto.getDescription() != null && !dto.getDescription().isBlank()) {
                predicates.add(cb.like(root.get("description"), "%" + dto.getDescription() + "%"));
            }

            // 6. 비고 (LIKE)
            if (dto.getRemarks() != null && !dto.getRemarks().isBlank()) {
                predicates.add(cb.like(root.get("remarks"), "%" + dto.getRemarks() + "%"));
            }

            // 7. 관계자 ID 목록 (IN 서브쿼리)
            // 관계자로 검색할 때(기부금영수증 발급 등)는 기간을 회계 연도 전체로 두는 경우가 많아 날짜로는 거의 걸러지지 않는다.
            // 내역마다 실행되는 상관 서브쿼리(EXISTS) 대신, 관계자의 내역 ID를 한 번에 찾아 그 내역만 읽도록 한다.
            if (contactIds != null && !contactIds.isEmpty()) {
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<FinancialRecordContact> contactRoot = subquery.from(FinancialRecordContact.class);
                subquery.select(contactRoot.get("financialRecord").get("id"))
                        .where(contactRoot.get("contact").get("id").in(contactIds));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
