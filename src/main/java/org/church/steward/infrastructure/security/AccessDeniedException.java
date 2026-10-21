package org.church.steward.infrastructure.security;

/**
 * 로그인한 사용자에게 기능을 쓸 권한이 없을 때 발생하는 예외다.
 */
public class AccessDeniedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public AccessDeniedException(String message) {
        super(message);
    }
}
