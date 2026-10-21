package org.church.steward.domain.activitylog.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ActivityLogRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ActivityLogRepository activityLogRepository;

    private ActivityLog createActivityLog(ActivityType activityType, String targetName) {
        ActivityLog activityLog = ActivityLog.builder()
                .userId("test-user")
                .activityType(activityType)
                .targetName(targetName)
                .description("{\"fields\":[]}")
                .build();
        return entityManager.persist(activityLog);
    }

    @Nested
    @DisplayName("findFirstByOrderByIdDesc 테스트")
    class FindFirstByOrderByIdDescTest {
        @Test
        @DisplayName("활동 기록이 여럿 있으면 가장 마지막에 남긴 것을 조회한다")
        void findFirstByOrderByIdDesc_whenLogsExist_returnLatest() {
            // Given
            createActivityLog(ActivityType.CREATE, "First");
            createActivityLog(ActivityType.BACKUP, "Second");
            ActivityLog latest = createActivityLog(ActivityType.IMPORT, "Third");

            // When
            Optional<ActivityLog> result = activityLogRepository.findFirstByOrderByIdDesc();

            // Then
            assertThat(result)
                    .map(ActivityLog::getId)
                    .contains(latest.getId());
        }

        @Test
        @DisplayName("활동 기록이 없으면 빈 Optional을 반환한다")
        void findFirstByOrderByIdDesc_whenNoLog_returnEmptyOptional() {
            // Given

            // When
            Optional<ActivityLog> result = activityLogRepository.findFirstByOrderByIdDesc();

            // Then
            assertThat(result).isEmpty();
        }
    }

}
