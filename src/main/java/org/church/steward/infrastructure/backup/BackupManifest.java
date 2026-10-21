package org.church.steward.infrastructure.backup;

import java.time.LocalDateTime;
import java.util.List;

import org.church.steward.infrastructure.config.EncryptedUserKey;

/**
 * 백업 파일에 덤프와 함께 담는 정보.
 *
 * @param formatVersion  백업 파일 형식의 버전
 * @param programVersion 백업을 만든 프로그램의 버전
 * @param createdAt      백업한 일시
 * @param userKeys       백업한 시점의 사용자별 DB 접근 키. 덤프를 풀고 복원한 데이터베이스를 여는 데 필요하다.
 */
public record BackupManifest(int formatVersion, String programVersion, LocalDateTime createdAt,
        List<EncryptedUserKey> userKeys) {
}
