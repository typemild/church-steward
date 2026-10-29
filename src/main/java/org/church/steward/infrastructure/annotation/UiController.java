package org.church.steward.infrastructure.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

/**
 * UI(프론트엔드)의 요청을 받아 처리하는 역할을 담당한다. 스프링 MVC의 @Controller와 유사하게, UI로부터 전달된 데이터를
 * 바탕으로 비즈니스 로직을 수행하거나 서비스를 호출하지만, 직접적으로 UI 컴포넌트를 조작하지는 않는다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface UiController {
    @AliasFor(annotation = Component.class)
    String value() default "";
}
