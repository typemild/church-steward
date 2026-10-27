package org.church.steward.domain.budget.entity;

/**
 * 예산 항목의 최대 길이. 스키마의 칼럼 길이와 같으며, 엔터티의 매핑, 서비스의 검증, 가져오기, 화면의 입력란이 함께 쓴다.
 */
public final class BudgetConstraints {

    public static final int NAME_MAX_LENGTH = 50;

    private BudgetConstraints() {
    }

}
