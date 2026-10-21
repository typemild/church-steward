package org.church.steward.domain.activitylog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.dto.ActivityLogSearchRequestDto;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class ActivityLogControllerTest {

    @Mock
    private ActivityLogService activityLogService;

    @InjectMocks
    private ActivityLogController activityLogController;

    @Nested
    @DisplayName("getLogs 테스트")
    class getLogsTest {

        @Test
        @DisplayName("모든 조건이 주어졌을 때, 올바른 검색 요청 DTO와 페이징 정보를 사용하여 로그를 조회한다")
        void getLogs_AllConditionsProvided_ReturnsPage() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now();
            String userId = "testUser";
            ActivityType activityType = ActivityType.CREATE;
            String targetName = "target";
            int page = 0;
            int size = 10;

            ActivityLogResponseDto responseDto = ActivityLogResponseDto.builder()
                    .id(1L)
                    .userId(userId)
                    .activityType(activityType)
                    .targetName(targetName)
                    .createdAt(LocalDateTime.now())
                    .build();

            Page<ActivityLogResponseDto> expectedPage = new PageImpl<>(List.of(responseDto));

            when(activityLogService.getLogs(any(ActivityLogSearchRequestDto.class), eq(page), eq(size)))
                    .thenReturn(expectedPage);

            // When
            Page<ActivityLogResponseDto> result = activityLogController.getLogs(
                    startDate, endDate, userId, activityType, targetName, page, size);

            // Then
            assertThat(result).isEqualTo(expectedPage);
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getUserId()).isEqualTo(userId);

            // Verify request DTO construction
            verify(activityLogService).getLogs(
                    org.mockito.ArgumentMatchers.argThat(request -> request.getStartDate().equals(startDate) &&
                            request.getEndDate().equals(endDate) &&
                            request.getUserId().equals(userId) &&
                            request.getActivityType().equals(activityType) &&
                            request.getTargetName().equals(targetName)),
                    eq(page),
                    eq(size));
        }

        @Test
        @DisplayName("선택적 조건들이 null일 때, null을 포함한 검색 요청 DTO를 사용하여 로그를 조회한다")
        void getLogs_OptionalConditionsAreNull_ReturnsPage() {
            // Given
            LocalDateTime startDate = LocalDateTime.now().minusDays(1);
            LocalDateTime endDate = LocalDateTime.now();
            int page = 0;
            int size = 20;

            Page<ActivityLogResponseDto> expectedPage = new PageImpl<>(List.of());

            when(activityLogService.getLogs(any(ActivityLogSearchRequestDto.class), eq(page), eq(size)))
                    .thenReturn(expectedPage);

            // When
            Page<ActivityLogResponseDto> result = activityLogController.getLogs(
                    startDate, endDate, null, null, null, page, size);

            // Then
            assertThat(result).isEqualTo(expectedPage);

            verify(activityLogService).getLogs(
                    org.mockito.ArgumentMatchers.argThat(request -> request.getStartDate().equals(startDate) &&
                            request.getEndDate().equals(endDate) &&
                            request.getUserId() == null &&
                            request.getActivityType() == null &&
                            request.getTargetName() == null),
                    eq(page),
                    eq(size));
        }
    }
}
