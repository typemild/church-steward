package org.church.steward.domain.report.entity;

import java.util.List;
import java.util.Set;

/**
 * 칸의 순서와 숨김을 정할 수 있는 표의 규칙. 칸의 구성과 기본 순서, 첫 번째 칸에 둘 수 있는 칸, 숨길 수 있는 칸을 정한다. 첫
 * 번째 칸은 숨길 수 없다. 보고서 양식의 표({@link ReportTemplateTable})와 수입/지출 관리의 예산별 보기 표가 함께
 * 쓴다.
 */
public interface ColumnOrderRule {

    /**
     * 표의 이름. 설정 화면의 탭 이름과 검증 오류 문구에 쓴다.
     */
    String getLabel();

    /**
     * 모든 칸의 기본 순서. 기본으로는 숨긴 칸이 없다.
     */
    List<ReportColumn> getDefaultColumns();

    /**
     * 숨길 수 있는 칸. 이 칸이라도 첫 번째 칸이면 숨길 수 없다.
     */
    Set<ReportColumn> getHideableColumns();

    /**
     * 칸이 첫 번째 자리에 올 수 있는지 여부
     */
    boolean canBeFirst(ReportColumn column);

    /**
     * 칸을 숨길 수 있는지 여부
     *
     * @param first 그 칸이 첫 번째 칸인지 여부. 첫 번째 칸은 숨길 수 없다.
     */
    default boolean canHide(ReportColumn column, boolean first) {
        return !first && getHideableColumns().contains(column);
    }

    /**
     * 이 표에서 칸을 부르는 이름
     */
    String labelOf(ReportColumn column);

    /**
     * 비율 칸(달성률/집행률)의 왼쪽에 막대를 함께 그릴지 정할 수 있는 표인지 여부. 보고서의 현황 표만 정할 수 있다.
     */
    default boolean hasRateBarOption() {
        return false;
    }

    /**
     * 내역의 정렬 순서({@link RecordOrder})를 정할 수 있는 표인지 여부. 내역을 나열하는 표(주간 보고서의 내역, 예산별
     * 보기)만 정할 수 있다.
     */
    default boolean hasRecordOrderOption() {
        return false;
    }

    /**
     * 첫 번째 칸 앞에 일자를 붙여 표시할지 정할 수 있는 표인지 여부. 내역을 나열하는 표(주간 보고서의 내역, 예산별 보기)만 정할 수
     * 있다.
     */
    default boolean hasDateOption() {
        return false;
    }
}
