package org.church.steward.infrastructure.security;

/**
 * 로그인된 사용자 세션 정보를 관리하는 인터페이스다.
 */
public interface UserSessionHolder {

    /**
     * 현재 로그인된 사용자를 설정한다.
     *
     * @param user 로그인된 사용자 정보
     */
    void setLoggedInUser(LoggedInUser user);

    /**
     * 현재 로그인된 사용자를 가져온다.
     *
     * @return 로그인된 사용자 정보 (로그인되지 않은 경우 null)
     */
    LoggedInUser getLoggedInUser();

    /**
     * 세션 정보를 삭제한다 (로그아웃 시 사용).
     */
    void clear();
}
