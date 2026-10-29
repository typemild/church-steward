package org.church.steward.infrastructure.security;

import org.church.steward.domain.user.entity.UserRole;

/**
 * 로그인 세션에 보관하는 사용자 정보다. 세션은 프로그램이 끝날 때까지 유지되므로 {@code User} 엔터티 대신 이 값을 보관한다.
 * 비밀번호 해시를 메모리에 남기지 않고, 트랜잭션 밖에서 엔터티를 쓰거나 오래된 값으로 저장하는 일을 막기 위함이다.
 *
 * @param id   사용자 ID
 * @param role 사용자 역할
 */
public record LoggedInUser(String id, UserRole role) {

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }
}
