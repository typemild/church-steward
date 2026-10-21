package org.church.steward.infrastructure.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * UI(프론트엔드)의 화면을 담당하는 컴포넌트에 사용된다.
 * 사용자에게 정보를 표시하고, 사용자로부터 입력을 받는 등 실제적인 UI 조작 및 인터랙션을 수행한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UiView {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
