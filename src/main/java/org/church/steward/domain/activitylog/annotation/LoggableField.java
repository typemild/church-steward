package org.church.steward.domain.activitylog.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.church.steward.domain.activitylog.transformer.DefaultTransformer;
import org.church.steward.domain.activitylog.transformer.LoggableTransformer;

/**
 * 활동 기록(Activity Log)에 포함할 필드를 지정하는 어노테이션이다.
 */
@Target({ ElementType.FIELD, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
public @interface LoggableField {
    /**
     * 컬럼(프로퍼티 또는 Getter 메소드)의 논리명 (예: "예산명")
     */
    String name();

    /**
     * 식별자 여부. true인 경우 값의 변경 여부와 관계없이 로그에 항상 표시된다. 다른 데이터와 구분하기 위한 용도로 사용된다.
     */
    boolean isIdentifier() default false;

    /**
     * 값을 변환하기 위해 호출할 Transformer 클래스를 지정한다. Transformer는 format 보다 먼저 적용된다.
     */
    Class<? extends LoggableTransformer> transformer() default DefaultTransformer.class;

    /**
     * 값을 출력할 때 사용할 포맷 패턴 (예: "#,###원", "yyyy-MM-dd"). 값이 비어있으면 기본 toString()을 사용한다.
     *
     * format은 Transformer보다 이후에 적용된다.
     */
    String format() default "";
}
