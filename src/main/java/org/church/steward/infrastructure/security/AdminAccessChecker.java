package org.church.steward.infrastructure.security;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 관리자만 쓸 수 있는 기능의 컨트롤러가 호출 전에 권한을 확인할 때 쓴다.
 * 메뉴를 관리자에게만 보여주는 것과 별개로, 컨트롤러 경계에서 한 번 더 막기 위함이다.
 */
@Component
@RequiredArgsConstructor
public class AdminAccessChecker {

    private final UserSessionHolder userSessionHolder;

    /**
     * 로그인한 사용자가 관리자인지 확인한다.
     *
     * @throws AccessDeniedException 로그인한 사용자가 없거나 관리자가 아닐 때 발생
     */
    public void checkAdmin() {
        LoggedInUser currentUser = userSessionHolder.getLoggedInUser();
        if (currentUser == null || !currentUser.isAdmin()) {
            throw new AccessDeniedException("관리자 권한이 필요합니다.");
        }
    }
}
