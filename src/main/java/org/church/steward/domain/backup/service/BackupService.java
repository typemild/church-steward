package org.church.steward.domain.backup.service;

import java.nio.file.Path;
import java.time.LocalDateTime;

import org.church.steward.domain.backup.dto.BackupInfoDto;

public interface BackupService {

    /**
     * 전체 데이터를 백업 파일로 저장하고 활동 기록에 남긴다. 같은 이름의 파일이 있으면 덮어쓴다.
     *
     * @param target 저장할 백업 파일
     * @return 백업을 기록한 활동 기록의 ID. 이후에 이 값이 가장 마지막 활동 기록의 ID와 다르면 백업한 뒤에 데이터가 바뀌었을 수
     *         있다는 뜻이며, 가져오기는 이것으로 백업을 다시 받아야 하는지 판단한다.
     */
    long createBackup(Path target);

    /**
     * 백업 파일의 정보를 읽는다. 백업 파일이 아니거나 손상되었을 때, 더 새로운 버전의 프로그램에서 만든 백업일 때는 예외가 발생한다.
     *
     * @param file 백업 파일
     * @return 백업 파일에 기록된 정보
     */
    BackupInfoDto inspectBackup(Path file);

    /**
     * 다음 실행 때 주어진 백업 파일로 복원하도록 예약한다. 복원은 데이터베이스 파일을 교체하므로 프로그램을 실행하는 중에는 할 수 없다.
     *
     * @param file 복원할 백업 파일
     */
    void scheduleRestore(Path file);

    /**
     * 복원한 내역을 활동 기록에 남긴다. 복원을 마친 직후, 복원한 데이터베이스로 잠깐 띄운 컨텍스트에서 호출한다. 복원은 로그인하기 전에
     * 하므로 복원한 계정(관리자)으로 기록한다.
     *
     * @param userId          복원한 사용자의 ID (백업한 시점의 계정)
     * @param fileName        복원에 쓴 백업 파일의 이름
     * @param backupCreatedAt 그 백업을 만든 일시
     */
    void logRestore(String userId, String fileName, LocalDateTime backupCreatedAt);
}
