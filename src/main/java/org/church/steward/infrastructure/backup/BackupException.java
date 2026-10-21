package org.church.steward.infrastructure.backup;

/**
 * 백업 파일을 만들거나 읽거나 복원하지 못했을 때 발생한다. 메시지는 화면에 그대로 보여준다.
 */
public class BackupException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BackupException(String message) {
        super(message);
    }

    public BackupException(String message, Throwable cause) {
        super(message, cause);
    }
}
