package org.church.steward.domain.activitylog.util;

import org.springframework.stereotype.Component;

/**
 * 엔터티의 상태를 스냅샷으로 포착하는 구현체다.
 */
@Component
public class EntitySnapshotCapturerImpl implements EntitySnapshotCapturer {

    @Override
    public <T> EntitySnapshot<T> capture(T entity) {
        return new EntitySnapshot<>(entity);
    }
}
