package org.church.steward.domain.datatransfer.exception;

/**
 * 가져오기를 반영하려는데, 화면에 보여준 분석 결과와 지금의 분석 결과가 다를 때 발생한다.
 * 분석한 뒤에 다른 메뉴에서 데이터를 바꾼 경우다. 화면은 이 예외를 받으면 다시 분석해 검토 단계로 돌아간다.
 */
public class ImportStateChangedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ImportStateChangedException(String message) {
        super(message);
    }
}
