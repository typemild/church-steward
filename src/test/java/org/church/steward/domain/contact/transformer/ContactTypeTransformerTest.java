package org.church.steward.domain.contact.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.contact.entity.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ContactTypeTransformerTest {

    private final ContactTypeTransformer contactTypeTransformer = new ContactTypeTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @ParameterizedTest
        @EnumSource(ContactType.class)
        @DisplayName("ContactType의 각 요소를 전달하면 해당 라벨을 반환한다")
        void transform_ContactType_ReturnsLabel(ContactType type) {
            // Given
            ContactType input = type;

            // When
            Object result = contactTypeTransformer.transform(input);

            // Then
            assertThat(result).isEqualTo(type.getLabel());
        }

        @Test
        @DisplayName("null을 전달하면 null을 반환한다")
        void transform_Null_ReturnsNull() {
            // Given
            Object input = null;

            // When
            Object result = contactTypeTransformer.transform(input);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("ContactType이 아닌 객체를 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_InvalidType_ThrowsIllegalArgumentException() {
            // Given
            Object input = "not a contact type";

            // When & Then
            assertThatThrownBy(() -> contactTypeTransformer.transform(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
