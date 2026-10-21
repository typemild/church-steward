package org.church.steward.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRole {

    ADMIN("관리자"),
    USER("사용자");

    private final String label;

}
