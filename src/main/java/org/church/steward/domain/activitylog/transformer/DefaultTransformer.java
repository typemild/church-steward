package org.church.steward.domain.activitylog.transformer;

/**
 * 기본 트랜스포머다. 변환 로직이 없는 경우 사용된다.
 * 성능 최적화를 위해 LoggableMetadataExtractor에서 이 클래스는 직접 호출되지 않는다.
 */
public class DefaultTransformer implements LoggableTransformer {
    @Override
    public Object transform(Object value) {
        return value;
    }
}
