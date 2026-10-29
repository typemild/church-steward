package org.church.steward.ui.view.financialrecord;

import java.util.List;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

/**
 * 탭의 일괄 변경/추가/수정/삭제/상세 보기 버튼이 요청하는 동작을 처리하는 역할
 */
public interface FinancialRecordActionHandler {

    /**
     * 예산 일괄 변경 다이얼로그를 열고, 확인하면 내역들의 예산을 한 번에 바꾼다.
     *
     * @param records 바꿀 내역들. 한 건 이상이며 모두 같은 구분이어야 한다.
     */
    void changeBudget(List<FinancialRecordResponseDto> records);

    /**
     * 관계자 일괄 변경 다이얼로그를 열고, 확인하면 내역들의 관계자를 한 번에 바꾼다.
     *
     * @param records 바꿀 내역들. 한 건 이상이며 모두 같은 구분이어야 한다.
     */
    void changeContacts(List<FinancialRecordResponseDto> records);

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
     * 확인을 받은 뒤 수입/지출 내역들을 한 번에 삭제한다.
     */
    void deleteRecords(List<FinancialRecordResponseDto> records);

    /**
     * 수입/지출 상세 보기 다이얼로그를 연다. 마감된 회계 연도처럼 내역을 바꿀 수 없을 때 쓴다.
     */
    void viewRecord(FinancialRecordResponseDto record);
}
