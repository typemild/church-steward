package org.church.steward.domain.report.entity;

/**
 * 보고서 양식(결재란, 표의 칸 순서와 숨김) 값의 범위, 기본값과 칼럼 길이. 서비스의 검증과 양식 설정 화면이 함께 쓴다.
 */
public final class ReportTemplateConstraints {

    /** 결재란의 칸 수 */
    public static final int APPROVAL_CELL_COUNT_MIN = 1;
    public static final int APPROVAL_CELL_COUNT_MAX = 5;
    /** 결재란 직함의 글자 수 상한 */
    public static final int APPROVAL_TITLE_MAX_LENGTH = 7;

    public static final boolean DEFAULT_APPROVAL_VISIBLE = true;
    public static final int DEFAULT_APPROVAL_CELL_COUNT = 3;
    /** 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부의 기본값 */
    public static final boolean DEFAULT_RATE_BAR_VISIBLE = true;
    /** 주간 내역에서 내역의 정렬 순서의 기본값 */
    public static final RecordOrder DEFAULT_RECORD_ORDER = RecordOrder.FIRST_COLUMN;
    /** 주간 내역에서 첫 번째 칸 앞에 일자를 붙여 표시하는지 여부의 기본값 */
    public static final boolean DEFAULT_DATE_VISIBLE = true;

    /** enum 칼럼의 길이 */
    public static final int TABLE_MAX_LENGTH = 30;
    public static final int COLUMN_MAX_LENGTH = 20;
    public static final int RECORD_ORDER_MAX_LENGTH = 20;

    private ReportTemplateConstraints() {
    }

}
