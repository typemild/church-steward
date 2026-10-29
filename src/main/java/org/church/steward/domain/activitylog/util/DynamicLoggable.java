package org.church.steward.domain.activitylog.util;

/**
 * 엔터티의 상태에 따라 활동 기록(Activity Log)의 논리명이 달라지는 엔터티가 구현하는 인터페이스다. 구현하지 않은
 * 엔터티는 @Loggable, @LoggableField에 선언한 논리명을 그대로 사용한다.
 */
public interface DynamicLoggable {

    /**
     * {@code @Loggable}에 선언한 논리명 대신 사용할 엔터티의 논리명을 반환한다.
     */
    String getLoggableName();

    /**
     * {@code @LoggableField}에 선언한 논리명 대신 사용할 필드의 논리명을 반환한다.
     *
     * @param declaredName {@code @LoggableField}에 선언한 논리명
     */
    String getLoggableFieldName(String declaredName);
}
