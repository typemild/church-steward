package org.church.steward.infrastructure.security;

/**
 * 비밀번호가 일치하지 않을 때 발생하는 예외다.
 */
public class InvalidPasswordException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidPasswordException(String message) {
        super(message);
    }
}
