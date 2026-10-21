package org.church.steward.domain.activitylog.dto;

import java.time.LocalDateTime;

import org.church.steward.domain.activitylog.entity.ActivityType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogSearchRequestDto {
    private LocalDateTime startDate; // 필수 (검증은 컨트롤러/서비스에서 수행)
    private LocalDateTime endDate; // 필수
    private String userId; // 선택
    private ActivityType activityType; // 선택
    private String targetName; // 선택
}
