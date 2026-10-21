package org.church.steward.domain.activitylog.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.BooleanTypeTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EntitySnapshotCapturerImplTest {

    private EntitySnapshotCapturerImpl capturer;

    @BeforeEach
    void setUp() {
        capturer = new EntitySnapshotCapturerImpl();
    }

    @Loggable(name = "테스트 엔터티")
    static class TestEntity {
        @LoggableField(name = "ID", isIdentifier = true)
        private Long id;

        @LoggableField(name = "이름")
        private String name;

        @LoggableField(name = "활성화 여부", transformer = BooleanTypeTransformer.class)
        private boolean active;

        @LoggableField(name = "계산된 값")
        public String getCalculatedValue() {
            return "computed";
        }

        private TestEntity(Long id, String name, boolean active) {
            this.id = id;
            this.name = name;
            this.active = active;
        }

        public static TestEntity create(Long id, String name, boolean active) {
            return new TestEntity(id, name, active);
        }
    }

    @Nested
    @DisplayName("capture 테스트")
    class CaptureTest {

        @Test
        @DisplayName("엔터티의 필드 값들이 스냅샷에 올바르게 저장되어야 한다")
        void capture_validEntity_shouldCaptureAllLoggableFields() {
            // Given
            TestEntity entity = TestEntity.create(1L, "테스트", true);

            // When
            EntitySnapshot<TestEntity> snapshot = capturer.capture(entity);

            // Then
            assertThat(snapshot.getFieldValue("ID")).isEqualTo("1");
            assertThat(snapshot.getFieldValue("이름")).isEqualTo("테스트");
            assertThat(snapshot.getFieldValue("활성화 여부")).isEqualTo("O");
            assertThat(snapshot.getFieldValue("계산된 값")).isEqualTo("computed");
        }

        @Test
        @DisplayName("엔터티의 값이 null인 경우 빈 문자열로 저장되어야 한다")
        void capture_nullFields_shouldHandleNullValues() {
            // Given
            TestEntity entity = TestEntity.create(null, null, false);

            // When
            EntitySnapshot<TestEntity> snapshot = capturer.capture(entity);

            // Then
            assertThat(snapshot.getFieldValue("ID")).isEqualTo("");
            assertThat(snapshot.getFieldValue("이름")).isEqualTo("");
            assertThat(snapshot.getFieldValue("활성화 여부")).isEqualTo("X");
        }

        @Test
        @DisplayName("null 엔터티를 전달하면 예외가 발생해야 한다")
        void capture_nullEntity_shouldThrowException() {
            // Given
            TestEntity entity = null;

            // When & Then
            assertThatThrownBy(() -> capturer.capture(entity))
                    .isInstanceOf(RuntimeException.class);
        }
    }
}
