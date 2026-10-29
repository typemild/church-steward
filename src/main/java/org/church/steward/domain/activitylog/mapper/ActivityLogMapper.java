package org.church.steward.domain.activitylog.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 활동 기록 엔터티와 DTO를 서로 변환한다. 엔터티에는 항목 목록을 JSON으로 저장하므로, JSON 형식도 이 클래스가 관리한다.
 */
@Component
@RequiredArgsConstructor
public class ActivityLogMapper {

    // 활동 기록 JSON의 키
    private static final String JSON_FIELDS = "fields";
    private static final String JSON_ITEMS = "items";
    private static final String JSON_NAME = "name";
    private static final String JSON_OLD_VALUE = "oldValue";
    private static final String JSON_VALUE = "value";

    private final ObjectMapper objectMapper;

    public ActivityLog toEntity(String userId, ActivityType activityType, String targetName,
            List<ActivityLogFieldDto> fields) {
        return toEntity(userId, activityType, targetName, fields, List.of());
    }

    /**
     * 한 번의 작업으로 여러 건을 처리한 기록(가져오기)을 엔터티로 만든다.
     *
     * @param fields 작업 전체에 대한 항목 목록 (요약)
     * @param items  처리한 건별 항목 목록. 건마다 생성 또는 수정 기록과 같은 형식의 항목 목록을 가진다.
     */
    public ActivityLog toEntity(String userId, ActivityType activityType, String targetName,
            List<ActivityLogFieldDto> fields, List<List<ActivityLogFieldDto>> items) {
        return ActivityLog.builder()
                .userId(userId)
                .activityType(activityType)
                .targetName(targetName)
                .description(toJson(fields, items))
                .build();
    }

    public ActivityLogResponseDto toResponseDto(ActivityLog entity) {
        if (entity == null) {
            return null;
        }

        JsonNode root = objectMapper.readTree(entity.getDescription());
        return ActivityLogResponseDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .activityType(entity.getActivityType())
                .targetName(entity.getTargetName())
                .fields(parseFields(root))
                .items(parseItems(root))
                .createdAt(entity.getCreatedAt())
                .build();
    }

    /**
     * 항목 목록을 저장용 JSON으로 변환한다. "oldValue"는 값이 변경된 항목에만 기록한다. "items"는 건별 항목 목록이 있을
     * 때만 기록한다.
     */
    private String toJson(List<ActivityLogFieldDto> fields, List<List<ActivityLogFieldDto>> items) {
        ObjectNode root = objectMapper.createObjectNode();
        writeFields(root, fields);
        if (!items.isEmpty()) {
            ArrayNode itemsNode = root.putArray(JSON_ITEMS);
            for (List<ActivityLogFieldDto> item : items) {
                writeFields(itemsNode.addObject(), item);
            }
        }

        return objectMapper.writeValueAsString(root);
    }

    private void writeFields(ObjectNode parent, List<ActivityLogFieldDto> fields) {
        ArrayNode fieldsNode = parent.putArray(JSON_FIELDS);
        for (ActivityLogFieldDto field : fields) {
            ObjectNode fieldNode = fieldsNode.addObject();
            fieldNode.put(JSON_NAME, Objects.toString(field.getName(), ""));
            if (field.isChanged()) {
                fieldNode.put(JSON_OLD_VALUE, Objects.toString(field.getOldValue(), ""));
            }
            fieldNode.put(JSON_VALUE, Objects.toString(field.getValue(), ""));
        }
    }

    /**
     * 저장된 JSON에서 건별 항목 목록을 읽는다. 건별 기록이 없는 로그이면 빈 목록이다.
     */
    private List<List<ActivityLogFieldDto>> parseItems(JsonNode root) {
        List<List<ActivityLogFieldDto>> items = new ArrayList<>();
        for (JsonNode itemNode : root.path(JSON_ITEMS)) {
            items.add(parseFields(itemNode));
        }
        return items;
    }

    /**
     * 저장된 JSON에서 항목 목록을 읽는다.
     */
    private List<ActivityLogFieldDto> parseFields(JsonNode parent) {
        List<ActivityLogFieldDto> fields = new ArrayList<>();
        for (JsonNode fieldNode : parent.path(JSON_FIELDS)) {
            boolean changed = fieldNode.has(JSON_OLD_VALUE);
            fields.add(ActivityLogFieldDto.builder()
                    .name(fieldNode.path(JSON_NAME).asString())
                    .oldValue(changed ? fieldNode.path(JSON_OLD_VALUE).asString() : null)
                    .value(fieldNode.path(JSON_VALUE).asString())
                    .changed(changed)
                    .build());
        }

        return fields;
    }
}
