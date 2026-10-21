package org.church.steward.infrastructure.spreadsheet;

/**
 * 표 파일을 읽거나 쓰지 못했을 때 발생한다. 메시지는 화면에 그대로 보여준다.
 */
public class SpreadsheetException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public SpreadsheetException(String message) {
        super(message);
    }

    public SpreadsheetException(String message, Throwable cause) {
        super(message, cause);
    }
}
