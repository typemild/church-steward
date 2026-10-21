package org.church.steward.domain.fiscalyear.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FiscalYearRepository extends JpaRepository<FiscalYear, Long> {
    /**
     * 모든 회계 연도를 최근 것부터(시작일이 늦은 순서로) 조회한다.
     */
    List<FiscalYear> findAllByOrderByStartDateDesc();

    /**
     * 가장 최근(시작일이 가장 늦은) 회계 연도를 찾는다.
     */
    Optional<FiscalYear> findFirstByOrderByStartDateDesc();

    /**
     * 직전 회계 연도, 곧 시작일이 주어진 날짜보다 앞선 회계 연도 중 가장 최근 것을 찾는다.
     *
     * @param startDate 기준이 되는 회계 연도의 시작일
     */
    Optional<FiscalYear> findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate startDate);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    /**
     * 기간이 주어진 기간과 하루라도 겹치는 회계 연도를 찾는다.
     * 두 기간은 한쪽의 시작일이 다른 쪽의 종료일 이전이고 종료일이 다른 쪽의 시작일 이후일 때 겹친다.
     *
     * @param endDate   주어진 기간의 종료일
     * @param startDate 주어진 기간의 시작일
     */
    List<FiscalYear> findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate endDate,
            LocalDate startDate);
}
