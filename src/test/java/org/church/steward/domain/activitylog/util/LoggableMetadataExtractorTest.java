package org.church.steward.domain.activitylog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.BooleanTypeTransformer;
import org.church.steward.domain.activitylog.transformer.LoggableTransformer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LoggableMetadataExtractorTest {

    @Loggable(name = "테스트 엔터티")
    static class TestEntity {
        @LoggableField(name = "ID", isIdentifier = true)
        private Long id;

        @LoggableField(name = "이름")
        private String name;

        @LoggableField(name = "활성화 여부", transformer = BooleanTypeTransformer.class)
        private boolean active;

        @LoggableField(name = "날짜", format = "yyyy-MM-dd")
        private LocalDate date;

        @LoggableField(name = "금액", format = "#,###")
        private int amount;

        public TestEntity(Long id, String name, boolean active, LocalDate date, int amount) {
            this.id = id;
            this.name = name;
            this.active = active;
            this.date = date;
            this.amount = amount;
        }

        public Long getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public boolean isActive() {
            return active;
        }

        public LocalDate getDate() {
            return date;
        }

        public int getAmount() {
            return amount;
        }
    }

    static class UnannotatedEntity {
        private String name;

        public String getName() {
            return name;
        }
    }

    @Loggable(name = "빈 로그 엔터티")
    static class EmptyLoggableEntity {
        private String name;

        public String getName() {
            return name;
        }
    }

    /**
     * 기본 생성자가 없어 리플렉션으로 만들 수 없는 트랜스포머
     */
    static class NoDefaultConstructorTransformer implements LoggableTransformer {
        NoDefaultConstructorTransformer(String unused) {
        }

        @Override
        public Object transform(Object value) {
            return value;
        }
    }

    /**
     * 구분에 따라 활동 기록의 논리명이 달라지는 엔터티
     */
    @Loggable(name = "동적 엔터티")
    static class DynamicEntity implements DynamicLoggable {
        @LoggableField(name = "일자", isIdentifier = true)
        private String date;

        private final String kind;

        public DynamicEntity(String kind, String date) {
            this.kind = kind;
            this.date = date;
        }

        public String getDate() {
            return date;
        }

        @Override
        public String getLoggableName() {
            return kind;
        }

        @Override
        public String getLoggableFieldName(String declaredName) {
            return kind + declaredName;
        }
    }

    @Nested
    @DisplayName("getFieldName 테스트")
    class GetFieldNameTest {

        @Test
        @DisplayName("일반 엔터티는 @LoggableField에 선언된 논리명을 반환한다")
        void getFieldName_plainEntity_returnsDeclaredName() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.now(), 1000);
            LoggableMetadataExtractor.LoggableFieldInfo nameField = findField(TestEntity.class, "이름");

            // When
            String name = LoggableMetadataExtractor.getFieldName(entity, nameField);

            // Then
            assertThat(name).isEqualTo("이름");
        }

        @Test
        @DisplayName("DynamicLoggable 엔터티는 엔터티가 반환한 논리명을 반환한다")
        void getFieldName_dynamicLoggable_returnsNameFromEntity() {
            // Given
            DynamicEntity entity = new DynamicEntity("지출", "2025-03-02");
            LoggableMetadataExtractor.LoggableFieldInfo dateField = findField(DynamicEntity.class, "일자");

            // When
            String name = LoggableMetadataExtractor.getFieldName(entity, dateField);

            // Then
            assertThat(name).isEqualTo("지출일자");
        }
    }

    @Nested
    @DisplayName("getLoggableName 테스트")
    class GetLoggableNameTest {

        @Test
        @DisplayName("DynamicLoggable 엔터티는 @Loggable에 선언된 논리명 대신 엔터티가 반환한 논리명을 추출한다")
        void getLoggableName_dynamicLoggable_returnsNameFromEntity() {
            // Given
            DynamicEntity entity = new DynamicEntity("지출", "2025-03-02");

            // When
            String name = LoggableMetadataExtractor.getLoggableName(entity);

            // Then
            assertThat(name).isEqualTo("지출");
        }

        @Test
        @DisplayName("엔터티 객체를 통해 논리명을 올바르게 추출한다")
        void getLoggableName_entity_returns_correct_name() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.now(), 1000);

            // When
            String name = LoggableMetadataExtractor.getLoggableName(entity);

            // Then
            assertThat(name).isEqualTo("테스트 엔터티");
        }

        @Test
        @DisplayName("클래스 타입을 통해 논리명을 올바르게 추출한다")
        void getLoggableName_class_returns_correct_name() {
            // Given
            Class<TestEntity> clazz = TestEntity.class;

            // When
            String name = LoggableMetadataExtractor.getLoggableName(clazz);

            // Then
            assertThat(name).isEqualTo("테스트 엔터티");
        }

        @Test
        @DisplayName("null 입력 시 null을 반환한다")
        void getLoggableName_null_returns_null() {
            // When
            String name = LoggableMetadataExtractor.getLoggableName((Class<?>) null);

            // Then
            assertThat(name).isNull();
        }

        @Test
        @DisplayName("@Loggable 어노테이션이 없는 클래스의 경우 null을 반환한다")
        void getLoggableName_noAnnotation_returns_null() {
            // Given
            UnannotatedEntity entity = new UnannotatedEntity();

            // When
            String name = LoggableMetadataExtractor.getLoggableName(entity);

            // Then
            assertThat(name).isNull();
        }
    }

    @Nested
    @DisplayName("getLoggableFields 테스트")
    class GetLoggableFieldsTest {

        @Test
        @DisplayName("엔터티 객체를 통해 로그 가능한 필드 목록을 올바르게 추출한다")
        void getLoggableFields_entity_returns_correct_fields() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.now(), 1000);

            // When
            List<LoggableMetadataExtractor.LoggableFieldInfo> fields = LoggableMetadataExtractor
                    .getLoggableFields(entity);

            // Then
            assertThat(fields).hasSize(5);
        }

        @Test
        @DisplayName("클래스 타입을 통해 로그 가능한 필드 목록을 올바르게 추출한다")
        void getLoggableFields_class_returns_correct_fields() {
            // Given
            Class<TestEntity> clazz = TestEntity.class;

            // When
            List<LoggableMetadataExtractor.LoggableFieldInfo> fields = LoggableMetadataExtractor
                    .getLoggableFields(clazz);

            // Then
            assertThat(fields).hasSize(5);
        }

        @Test
        @DisplayName("@Loggable 어노테이션이 없는 클래스의 경우 빈 리스트를 반환한다")
        void getLoggableFields_noAnnotation_returns_empty_list() {
            // Given
            UnannotatedEntity entity = new UnannotatedEntity();

            // When
            List<LoggableMetadataExtractor.LoggableFieldInfo> fields = LoggableMetadataExtractor
                    .getLoggableFields(entity);

            // Then
            assertThat(fields).isEmpty();
        }

        @Test
        @DisplayName("@Loggable 어노테이션은 있지만 @LoggableField가 없는 경우 빈 리스트를 반환한다")
        void getLoggableFields_noLoggableFields_returns_empty_list() {
            // Given
            EmptyLoggableEntity entity = new EmptyLoggableEntity();

            // When
            List<LoggableMetadataExtractor.LoggableFieldInfo> fields = LoggableMetadataExtractor
                    .getLoggableFields(entity);

            // Then
            assertThat(fields).isEmpty();
        }
    }

    @Nested
    @DisplayName("getValue 테스트")
    class GetValueTest {

        @Test
        @DisplayName("필드 값을 통해 포맷팅된 문자열을 올바르게 가져온다")
        void getValue_field_returns_formatted_value() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.of(2023, 1, 1), 1500);
            LoggableMetadataExtractor.LoggableFieldInfo nameField = findField(TestEntity.class, "이름");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, nameField);

            // Then
            assertThat(value).isEqualTo("테스트");
        }

        @Test
        @DisplayName("메소드(Getter) 값을 통해 포맷팅된 문자열을 올바르게 가져온다")
        void getValue_method_returns_formatted_value() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.of(2023, 1, 1), 1500);
            LoggableMetadataExtractor.LoggableFieldInfo activeField = findField(TestEntity.class, "활성화 여부");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, activeField);

            // Then
            assertThat(value).isEqualTo("O");
        }

        @Test
        @DisplayName("트랜스포머가 적용된 값을 올바르게 가져온다")
        void getValue_with_transformer_returns_transformed_value() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.of(2023, 1, 1), 1500);
            LoggableMetadataExtractor.LoggableFieldInfo activeField = findField(TestEntity.class, "활성화 여부");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, activeField);

            // Then
            assertThat(value).isEqualTo("O");
        }

        @Test
        @DisplayName("포맷이 지정된 숫자 값을 올바르게 포맷팅한다")
        void getValue_number_format_returns_formatted_string() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.of(2023, 1, 1), 1500);
            LoggableMetadataExtractor.LoggableFieldInfo amountField = findField(TestEntity.class, "금액");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, amountField);

            // Then
            assertThat(value).isEqualTo("1,500");
        }

        @Test
        @DisplayName("포맷이 지정된 날짜 값을 올바르게 포맷팅한다")
        void getValue_date_format_returns_formatted_string() {
            // Given
            LocalDate date = LocalDate.of(2023, 1, 1);
            TestEntity entity = new TestEntity(1L, "테스트", true, date, 1500);
            LoggableMetadataExtractor.LoggableFieldInfo dateField = findField(TestEntity.class, "날짜");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, dateField);

            // Then
            assertThat(value).isEqualTo("2023-01-01");
        }

        @Test
        @DisplayName("값이 null인 경우 빈 문자열을 반환한다")
        void getValue_null_value_returns_empty_string() {
            // Given
            TestEntity entity = new TestEntity(1L, null, true, null, 1000);
            LoggableMetadataExtractor.LoggableFieldInfo nameField = findField(TestEntity.class, "이름");

            // When
            String value = LoggableMetadataExtractor.getValue(entity, nameField);

            // Then
            assertThat(value).isEmpty();
        }

        @Test
        @DisplayName("필드와 메소드 정보가 모두 없으면 IllegalArgumentException을 던진다")
        void getValue_noFieldAndMethod_throwsIllegalArgumentException() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.now(), 1000);
            LoggableMetadataExtractor.LoggableFieldInfo invalidInfo = new LoggableMetadataExtractor.LoggableFieldInfo(
                    "Invalid", true, null, null, null, 0, null);

            // When & Then
            assertThatThrownBy(() -> LoggableMetadataExtractor.getValue(entity, invalidInfo))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("트랜스포머를 리플렉션으로 만들 수 없으면 원인 예외를 담은 IllegalStateException을 던진다")
        void getValue_transformerWithoutDefaultConstructor_throwsIllegalStateExceptionWithCause() {
            // Given
            TestEntity entity = new TestEntity(1L, "테스트", true, LocalDate.now(), 1000);
            LoggableMetadataExtractor.LoggableFieldInfo nameField = findField(TestEntity.class, "이름");
            LoggableMetadataExtractor.LoggableFieldInfo info = new LoggableMetadataExtractor.LoggableFieldInfo(
                    "이름", false, null, nameField.getField(), null, 0, NoDefaultConstructorTransformer.class);

            // When & Then
            assertThatThrownBy(() -> LoggableMetadataExtractor.getValue(entity, info))
                    .isInstanceOf(IllegalStateException.class)
                    .hasCauseInstanceOf(NoSuchMethodException.class);
        }
    }

    private LoggableMetadataExtractor.LoggableFieldInfo findField(Class<?> clazz, String logicalName) {
        return LoggableMetadataExtractor.getLoggableFields(clazz).stream()
                .filter(info -> info.getName().equals(logicalName))
                .findFirst()
                .orElseThrow();
    }
}
