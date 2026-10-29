package org.church.steward.domain.financialrecord.service;

import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;

public interface FinancialRecordService {
    List<FinancialRecordResponseDto> listFinancialRecords(FinancialRecordSearchRequestDto request);

    FinancialRecordResponseDto createFinancialRecord(FinancialRecordRequestDto dto);

    /**
     * 활동 기록을 남기지 않고 수입 또는 지출을 등록한다. 검증과 관계자 그룹 자동 생성은
     * {@link #createFinancialRecord}와 같다.
     * 여러 내역을 한꺼번에 처리하고 활동 기록을 한 건으로 남기는 가져오기에서만 쓴다.
     *
     * @return 등록된 내역
     */
    FinancialRecord createFinancialRecordWithoutLog(FinancialRecordRequestDto dto);

    FinancialRecordResponseDto updateFinancialRecord(Long id, FinancialRecordRequestDto dto);

    /**
     * 여러 내역의 예산을 한 번에 바꾼다. 하나의 트랜잭션으로 처리하므로 한 건이라도 검증에 걸리면 아무것도 바뀌지 않는다.
     * 예산이 이미 같은 내역도 똑같이 처리하며, 활동 기록은 내역마다 수정으로 남긴다.
     *
     * @param budgetId 바꿀 예산. 내역과 같은 구분, 같은 회계 연도의 말단 예산이어야 한다.
     */
    void updateFinancialRecordsBudget(List<Long> ids, Long budgetId);

    /**
     * 여러 내역의 관계자를 한 번에 바꾼다. 하나의 트랜잭션으로 처리하므로 한 건이라도 검증에 걸리면 아무것도 바뀌지 않는다.
     * 관계자가 이미 같은 내역도 똑같이 처리하므로 관계자의 이름은 모두 현재 이름으로 기록되며, 활동 기록은 내역마다 수정으로
     * 남긴다.
     *
     * @param contactIds 바꿀 관계자들. 수입은 1명 이상, 지출은 정확히 1명이어야 한다.
     */
    void updateFinancialRecordsContacts(List<Long> ids, List<Long> contactIds);

    /**
     * 여러 내역을 한 번에 삭제한다. 하나의 트랜잭션으로 처리하므로 한 건이라도 삭제할 수 없으면 아무것도 삭제되지 않는다.
     * 활동 기록은 내역마다 삭제로 남긴다.
     */
    void deleteFinancialRecords(List<Long> ids);
}
