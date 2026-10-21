package org.church.steward.domain.activitylog.util;

/**
 * 엔터티의 현재 상태를 스냅샷으로 포착하는 역할을 수행한다.
 */
public interface EntitySnapshotCapturer {

    /**
     * 엔터티의 상태를 스냅샷으로 생성한다.
     *
     * @param<T> 엔터티 타입
     * @param entity 로그를 기록할 엔터티
     * @return 생성된 스냅샷
     */
    <T> EntitySnapshot<T> capture(T entity);
}
