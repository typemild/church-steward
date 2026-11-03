package org.church.steward.domain.report.entity;

/**
 * 보고서 양식에서 순서를 바꾸거나 숨길 수 있는 표의 칸. 칸의 이름은 표에 따라 다르므로
 * {@link ReportTemplateTable#labelOf(ReportColumn)}로 얻는다.
 */
public enum ReportColumn {

    /** 현황 표의 예산 항목 */
    BUDGET_NAME,
    /** 현황 표의 수입액/집행액 */
    ACTUAL_AMOUNT,
    /** 현황 표의 예산액 */
    BUDGETED_AMOUNT,
    /** 현황 표의 달성률/집행률. 양식에 따라 왼쪽에 비율 막대를 함께 그린다. */
    RATE,
    /** 내역의 봉헌자/수취인 */
    CONTACT,
    /** 내역의 내용 */
    DESCRIPTION,
    /** 내역의 금액 */
    AMOUNT,
    /** 내역의 비고 */
    REMARKS
}
