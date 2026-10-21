package org.church.steward.domain.backup.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 백업 파일에 기록된 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupInfoDto {

    /** 백업한 일시 */
    private LocalDateTime createdAt;

    /** 백업을 만든 프로그램의 버전 */
    private String programVersion;

}
