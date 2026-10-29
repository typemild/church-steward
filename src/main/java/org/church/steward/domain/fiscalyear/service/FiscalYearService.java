package org.church.steward.domain.fiscalyear.service;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;

public interface FiscalYearService {
    /**
     * 모든 회계 연도를 최근 것부터(시작일이 늦은 순서로) 조회한다.
     */
    List<FiscalYearDto> listFiscalYears();

    /**
     * 회계 연도를 추가한다. 이름은 다른 회계 연도와 같을 수 없고, 기간은 다른 회계 연도와 겹칠 수 없다.
     */
    void createFiscalYear(String name, LocalDate startDate, LocalDate endDate);

    /**
     * 회계 연도를 추가하고 원본 회계 연도의 예산을 복사한다. 원본 회계 연도는 마감되어 있어도 된다.
     */
    void createFiscalYearWithCopy(String name, LocalDate startDate, LocalDate endDate, Long sourceFiscalYearId);

    /**
     * 회계 연도의 이름과 기간을 바꾼다. 기간은 다른 회계 연도와 겹칠 수 없고, 이미 등록된 수입/지출의 날짜를 모두 포함해야 한다. 마감된
     * 회계 연도는 바꿀 수 없다.
     */
    void updateFiscalYear(Long id, String name, LocalDate startDate, LocalDate endDate);

    /**
     * 회계 연도를 삭제한다. 마감된 회계 연도는 삭제할 수 없다.
     */
    void deleteFiscalYear(Long id);

    /**
     * 회계 연도를 마감한다. 마감 시점의 총 수입, 총 지출, 차액을 계산해 저장하며, 이후로는 마감을 취소할 때까지 회계 연도와 그에 속한
     * 예산, 수입/지출을 바꿀 수 없다.
     */
    void closeFiscalYear(Long id);

    /**
     * 회계 연도의 마감을 취소한다. 마감할 때 저장한 총 수입, 총 지출, 차액은 지운다.
     */
    void reopenFiscalYear(Long id);

    /**
     * 가장 최근(시작일이 가장 늦은) 회계 연도의 ID를 반환한다.
     */
    Long getLatestFiscalYearId();

    boolean existsById(Long id);
}
