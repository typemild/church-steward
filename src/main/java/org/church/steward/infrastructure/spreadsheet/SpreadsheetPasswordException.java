package org.church.steward.infrastructure.spreadsheet;

/**
 * 암호가 걸린 파일인데 암호를 주지 않았거나 암호가 틀렸을 때 발생한다. 화면은 이 예외를 받으면 암호를 다시 묻는다.
 */
public class SpreadsheetPasswordException extends SpreadsheetException {
    private static final long serialVersionUID = 1L;

    public SpreadsheetPasswordException(String message) {
        super(message);
    }
}
