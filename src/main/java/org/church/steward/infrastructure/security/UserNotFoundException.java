package org.church.steward.infrastructure.security;

/**
 * 사용자 ID를 찾을 수 없을 때 발생하는 예외다.
 */
public class UserNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public UserNotFoundException(String message) {
        super(message);
    }
}
