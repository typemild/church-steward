package org.church.steward.infrastructure.security;

import org.springframework.stereotype.Component;

/**
 * UserSessionHolder 인터페이스의 구현체다.
 * 애플리케이션 실행 동안 싱글톤으로 유지되며 로그인된 사용자 정보를 보관한다.
 */
@Component
public class UserSessionHolderImpl implements UserSessionHolder {

    private LoggedInUser currentUser;

    @Override
    public void setLoggedInUser(LoggedInUser user) {
        this.currentUser = user;
    }

    @Override
    public LoggedInUser getLoggedInUser() {
        return currentUser;
    }

    @Override
    public void clear() {
        this.currentUser = null;
    }
}
