package org.church.steward.ui.view.activitylog;

import java.util.List;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;

/**
 * 활동 기록에 담긴 항목을 화면에 표시할 문자열로 변환한다.
 */
final class ActivityLogFieldFormatter {

    private static final String EMPTY_VALUE = "(없음)";

    private ActivityLogFieldFormatter() {
        // 인스턴스화 방지
    }

    /**
     * 기록된 항목 전체를 한 줄로 요약한다. 예: "이름 : 홍길동, 비고 : (없음) → 메모"
     */
    static String toSummary(ActivityLogResponseDto dto) {
        return toSummary(dto.getFields());
    }

    /**
     * 항목 목록을 한 줄로 요약한다. 가져오기 기록의 건별 내용을 한 줄씩 보여주는 데도 쓴다.
     */
    static String toSummary(List<ActivityLogFieldDto> fields) {
        return fields.stream()
                .map(ActivityLogFieldFormatter::toSummary)
                .collect(Collectors.joining(", "));
    }

    private static String toSummary(ActivityLogFieldDto field) {
        if (field.isChanged()) {
            return field.getName() + ": " + toDisplayValue(field.getOldValue()) + " → "
                    + toDisplayValue(field.getValue());
        }
        return field.getName() + ": " + toDisplayValue(field.getValue());
    }

    /**
     * 값이 비어 있으면 빈 칸 대신 "(없음)"으로 표시한다.
     */
    static String toDisplayValue(String value) {
        if (value == null || value.isEmpty()) {
            return EMPTY_VALUE;
        }
        return value;
    }
}
