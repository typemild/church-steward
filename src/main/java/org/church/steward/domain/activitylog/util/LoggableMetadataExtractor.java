package org.church.steward.domain.activitylog.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.DefaultTransformer;
import org.church.steward.domain.activitylog.transformer.LoggableTransformer;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * {@link Loggable} 어노테이션을 분석하여 엔터티의 로그 대상 정보(논리명 및 필드 목록)를 추출하고, 성능 최적화를 위해
 * 캐싱하여 관리하는 유틸리티 클래스다.
 */
public class LoggableMetadataExtractor {

    // 성능 최적화를 위해 클래스별로 추출된 메타데이터를 캐싱한다.
    private static final Map<Class<?>, LoggableMetadata> CACHE = new ConcurrentHashMap<>();

    @Getter
    @AllArgsConstructor
    private static class LoggableMetadata {
        private final String name;
        private final List<LoggableFieldInfo> fields;
    }

    @Getter
    @AllArgsConstructor
    public static class LoggableFieldInfo {
        private final String name; // 논리명
        private final boolean isIdentifier; // 식별자 여부
        private final String format; // 포맷 패턴
        private final Field field; // 필드 정보 (필드인 경우)
        private final Method method; // 메소드 정보 (Getter인 경우)
        private final int orderIndex; // 선언 순서 인덱스
        private final Class<? extends LoggableTransformer> transformer; // 트랜스포머 클래스
    }

    /**
     * 대상 객체로부터 로그 가능한 필드/메소드 정보를 가져온다. 캐싱된 데이터를 사용한다.
     */
    public static List<LoggableFieldInfo> getLoggableFields(Object entity) {
        return getMetadata(entity.getClass()).getFields();
    }

    /**
     * {@link Loggable} 어노테이션이 붙은 클래스의 논리명을 가져온다. 대상 객체가 {@link DynamicLoggable}이면
     * 객체가 반환한 논리명을 사용한다.
     *
     * @param entity 대상 객체
     * @return 엔터티의 논리명, 어노테이션이 없으면 null
     */
    public static String getLoggableName(Object entity) {
        if (entity instanceof DynamicLoggable dynamicLoggable) {
            return dynamicLoggable.getLoggableName();
        }
        return getLoggableName(entity != null ? entity.getClass() : null);
    }

    /**
     * 로그에 표시할 필드의 논리명을 가져온다. 대상 객체가 {@link DynamicLoggable}이면 객체가 반환한 논리명을 사용한다.
     *
     * @param entity 대상 객체
     * @param info   필드 정보
     * @return 필드의 논리명
     */
    public static String getFieldName(Object entity, LoggableFieldInfo info) {
        if (entity instanceof DynamicLoggable dynamicLoggable) {
            return dynamicLoggable.getLoggableFieldName(info.getName());
        }
        return info.getName();
    }

    /**
     * {@link Loggable} 어노테이션이 붙은 클래스의 논리명을 가져온다.
     *
     * @param clazz 대상 클래스
     * @return 엔터티의 논리명, 어노테이션이 없으면 null
     */
    public static String getLoggableName(Class<?> clazz) {
        if (clazz == null)
            return null;
        return getMetadata(clazz).getName();
    }

    /**
     * 클래스 타입을 기반으로 로그 가능한 필드/메소드 정보를 가져오며, 캐싱을 활용한다. 식별자 항목이 먼저 오도록 정렬하며, 그 안에서도 선언
     * 순서를 유지한다.
     */
    public static List<LoggableFieldInfo> getLoggableFields(Class<?> clazz) {
        return getMetadata(clazz).getFields();
    }

    private static LoggableMetadata getMetadata(Class<?> clazz) {
        return CACHE.computeIfAbsent(clazz, c -> {
            String name = null;
            if (c.isAnnotationPresent(Loggable.class)) {
                name = c.getAnnotation(Loggable.class).name();
            }
            List<LoggableFieldInfo> extracted = extract(c);
            extracted.sort(Comparator.comparing(LoggableFieldInfo::isIdentifier, Comparator.reverseOrder())
                    .thenComparingInt(LoggableFieldInfo::getOrderIndex));
            return new LoggableMetadata(name, extracted);
        });
    }

    /**
     * 리플렉션을 사용하여 클래스 내의 @LoggableField 어노테이션이 붙은 필드 또는 메소드를 찾아 정보를 추출한다.
     */
    private static List<LoggableFieldInfo> extract(Class<?> clazz) {
        List<LoggableFieldInfo> infos = new ArrayList<>();
        int index = 0;

        // 1. 필드 분석 (Property)
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(LoggableField.class)) {
                LoggableField ann = field.getAnnotation(LoggableField.class);
                field.setAccessible(true);
                infos.add(new LoggableFieldInfo(ann.name(), ann.isIdentifier(), ann.format(), field, null, index++,
                        ann.transformer()));
            }
        }

        // 2. 메소드 분석 (Getter)
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(LoggableField.class)) {
                LoggableField ann = method.getAnnotation(LoggableField.class);
                infos.add(new LoggableFieldInfo(ann.name(), ann.isIdentifier(), ann.format(), null, method, index++,
                        ann.transformer()));
            }
        }

        return infos;
    }

    /**
     * 추출된 필드/메소드 정보를 바탕으로 대상 객체의 실제 값을 가져오고, {@link LoggableField}에 정의된 포맷을 적용하여
     * 문자열로 반환한다.
     */
    public static String getValue(Object entity, LoggableFieldInfo info) {
        try {
            // 1. 필드 또는 메소드를 통해 원본 값을 먼저 가져온다.
            Object value;
            if (info.getField() != null) {
                value = info.getField().get(entity);
            } else if (info.getMethod() != null) {
                value = info.getMethod().invoke(entity);
            } else {
                throw new IllegalArgumentException("필드 또는 메소드 정보가 없습니다.");
            }

            // 2. 트랜스포머가 있다면 값을 변환한다.
            // 성능 최적화를 위해 DefaultTransformer인 경우 리플렉션을 통한 인스턴스 생성 과정을 건너뛴다.
            if (info.getTransformer() != null && !info.getTransformer()
                    .equals(DefaultTransformer.class)) {
                LoggableTransformer transformer = info.getTransformer().getDeclaredConstructor().newInstance();
                value = transformer.transform(value);
            }

            // 3. 변환된 값에 포맷을 적용한다.
            return formatValue(value, info.getFormat());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("기록할 값을 읽지 못했습니다: " + info.getName(), e);
        }
    }

    /**
     * 값의 타입(Number, TemporalAccessor 등)에 따라 지정된 포맷 패턴을 적용하여 문자열로 변환한다. 포맷이 유효하지 않거나
     * 지원되지 않는 경우 기본 toString()을 사용한다.
     */
    private static String formatValue(Object value, String format) {
        if (value == null) {
            return "";
        }

        if (format != null && !format.isEmpty()) {
            // 숫자 포맷팅
            if (value instanceof Number) {
                return new DecimalFormat(format).format(value);
            }
            // 날짜/시간 포맷팅
            if (value instanceof TemporalAccessor) {
                return DateTimeFormatter.ofPattern(format).format((TemporalAccessor) value);
            }
        }

        return String.valueOf(value);
    }
}
