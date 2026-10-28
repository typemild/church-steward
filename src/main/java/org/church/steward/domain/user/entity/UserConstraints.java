package org.church.steward.domain.user.entity;

/**
 * 사용자 항목의 최대 길이. 스키마의 칼럼 길이와 같으며, 엔터티의 매핑, 서비스의 검증, 화면의 입력란이 함께 쓴다.
 */
public final class UserConstraints {

    public static final int ID_MAX_LENGTH = 100;
    public static final int REMARKS_MAX_LENGTH = 100;

    private UserConstraints() {
    }

}
