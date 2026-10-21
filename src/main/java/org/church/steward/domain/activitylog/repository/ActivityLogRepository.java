package org.church.steward.domain.activitylog.repository;

import java.util.Optional;

import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long>, JpaSpecificationExecutor<ActivityLog> {

    /**
     * 가장 마지막에 남긴 활동 기록을 찾는다. ID는 자동 증가이므로 ID가 가장 큰 것이 마지막 기록이다.
     */
    Optional<ActivityLog> findFirstByOrderByIdDesc();
}
