package org.church.steward.ui.view.financialrecord;

import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

/**
 * 탭의 추가/수정/삭제 버튼이 요청하는 동작을 처리하는 역할
 */
public interface FinancialRecordActionHandler {

    /**
     * 수입/지출 추가 다이얼로그를 연다.
     *
     * @param type       추가할 구분. null이면 다이얼로그에서 구분을 고를 수 있다.
     * @param budgetId   미리 선택해 둘 예산의 ID. 없으면 null.
     * @param budgetPath 미리 선택해 둘 예산의 전체 예산명. 없으면 null.
     */
    void addRecord(FinancialRecordType type, Long budgetId, String budgetPath);

    /**
     * 수입/지출 수정 다이얼로그를 연다.
     */
    void editRecord(FinancialRecordResponseDto record);

    /**
     * 확인을 받은 뒤 수입/지출 내역들을 삭제한다.
     */
    void deleteRecords(List<FinancialRecordResponseDto> records);
}
