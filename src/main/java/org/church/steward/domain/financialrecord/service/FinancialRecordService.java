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

    void deleteFinancialRecord(Long id);
}
