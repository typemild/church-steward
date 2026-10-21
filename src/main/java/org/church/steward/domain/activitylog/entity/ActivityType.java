package org.church.steward.domain.activitylog.entity;

/**
 * 수행된 행위의 종류
 */
public enum ActivityType {

    CREATE("생성"),
    UPDATE("수정"),
    DELETE("삭제"),
    CLOSE("마감"),
    REOPEN("마감 취소"),
    PRINT("인쇄"),
    IMPORT("가져오기"),
    EXPORT("내보내기"),
    BACKUP("백업"),
    RESTORE("복원"),
    LOGIN("로그인");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
