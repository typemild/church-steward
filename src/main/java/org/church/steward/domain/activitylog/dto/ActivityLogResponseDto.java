package org.church.steward.domain.activitylog.dto;

import java.time.LocalDateTime;
import java.util.List;

import org.church.steward.domain.activitylog.entity.ActivityType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponseDto {
    private Long id;
    private String userId;
    private ActivityType activityType;
    private String targetName;
    private List<ActivityLogFieldDto> fields;

    /** 한 번의 작업으로 여러 건을 처리한 기록(가져오기)의 건별 항목 목록. 그런 기록이 아니면 빈 목록이다. */
    @Builder.Default
    private List<List<ActivityLogFieldDto>> items = List.of();

    private LocalDateTime createdAt;
}
