package org.church.steward.domain.activitylog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 활동 기록에 담긴 항목 하나
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogFieldDto {

    /**
     * 항목의 논리명
     */
    private String name;

    /**
     * 변경 전 값 (값이 변경된 경우에만 존재)
     */
    private String oldValue;

    /**
     * 기록 시점의 값 (값이 변경된 경우에는 변경 후 값)
     */
    private String value;

    /**
     * 값의 변경 여부
     */
    private boolean changed;
}
