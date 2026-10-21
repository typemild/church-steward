package org.church.steward.domain.activitylog.transformer;

/**
 * 필드 또는 메소드의 값을 로그용 데이터로 변환하는 전략 인터페이스
 */
public interface LoggableTransformer {
    Object transform(Object value);
}
