package org.church.steward.domain.datatransfer.dto;

/**
 * 가져오기에서 파일의 한 건을 분석한 결과
 */
public enum ImportItemStatus {

    /** 그대로 가져올 수 있음 */
    OK("정상"),
    /** 가져올 수 있지만 확인이 필요함 */
    WARNING("경고"),
    /** 가져올 수 없음 */
    ERROR("오류"),
    /** 가져올 것이 없음 (이미 있는 데이터 등) */
    SKIPPED("건너뜀");

    private final String label;

    ImportItemStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
