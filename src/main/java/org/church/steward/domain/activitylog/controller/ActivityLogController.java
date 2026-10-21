package org.church.steward.domain.activitylog.controller;

import java.time.LocalDateTime;

import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.dto.ActivityLogSearchRequestDto;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.infrastructure.annotation.UiController;
import org.springframework.data.domain.Page;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityLogService activityLogService;

    /**
     * 활동 이력을 가장 최근 것부터 조회한다.
     *
     * @param startDate    시작 일시 (필수)
     * @param endDate      종료 일시 (필수)
     * @param userId       사용자 ID (선택)
     * @param activityType 행위 유형 (선택)
     * @param targetName   대상 이름 (선택)
     * @param page         페이지 번호 (0부터 시작)
     * @param size         페이지 크기
     * @return 조회된 활동 이력 페이지
     */
    public Page<ActivityLogResponseDto> getLogs(
            LocalDateTime startDate,
            LocalDateTime endDate,
            String userId,
            ActivityType activityType,
            String targetName,
            int page,
            int size) {

        ActivityLogSearchRequestDto request = ActivityLogSearchRequestDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .userId(userId)
                .targetName(targetName)
                .activityType(activityType)
                .build();

        return activityLogService.getLogs(request, page, size);
    }
}
