package org.church.steward.domain.activitylog.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.activitylog.util.LoggableMetadataExtractor.LoggableFieldInfo;

/**
 * 엔터티의 특정 시점을 나타내는 스냅샷 (활동 기록용)
 */
public class EntitySnapshot<T> {

    private final Map<String, String> snapshot;

    /**
     * 엔터티의 상태를 스냅샷으로 저장한다. 외부에서 직접 생성하는 것을 방지하기 위해 protected로 설정한다.
     */
    protected EntitySnapshot(T oldEntity) {
        List<LoggableFieldInfo> fields = LoggableMetadataExtractor.getLoggableFields(oldEntity);

        snapshot = new HashMap<>();
        for (LoggableFieldInfo info : fields) {
            snapshot.put(info.getName(), LoggableMetadataExtractor.getValue(oldEntity, info));
        }
    }

    public String getFieldValue(String name) {
        return snapshot.get(name);
    }

}
