package org.church.steward.domain.datatransfer.exception;

/**
 * 가져오기를 반영하려는데, 반영 전에 받은 백업 이후로 활동 기록이 새로 생겼을 때 발생한다.
 * 데이터가 바뀌었을 수 있어 그 백업으로는 반영 직전의 상태로 되돌릴 수 없으므로, 화면은 백업을 다시 받게 한다.
 */
public class BackupOutdatedException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public BackupOutdatedException(String message) {
        super(message);
    }
}
