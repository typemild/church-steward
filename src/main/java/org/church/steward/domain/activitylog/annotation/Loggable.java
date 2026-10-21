package org.church.steward.domain.activitylog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 활동 기록(Activity Log)에 포함되는 엔터티임을 나타내는 어노테이션이다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
    /**
     * 엔터티의 논리명
     */
    String name();
}
