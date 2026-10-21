package org.church.steward.domain.fiscalyear.transformer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FiscalYearTransformerTest {

    private final FiscalYearTransformer fiscalYearTransformer = new FiscalYearTransformer();

    @Nested
    @DisplayName("transform 테스트")
    class TransformTest {

        @Test
        @DisplayName("FiscalYear 엔티티를 전달하면 회계 연도의 이름을 반환한다")
        void transform_FiscalYear_ReturnsName() {
            // Given
            FiscalYear fiscalYear = FiscalYear.builder()
                    .name("2024년 상반기")
                    .build();

            // When
            Object result = fiscalYearTransformer.transform(fiscalYear);

            // Then
            assertThat(result).isEqualTo("2024년 상반기");
        }

        @Test
        @DisplayName("null을 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_Null_ThrowsException() {
            // Given
            Object input = null;

            // When & Then
            assertThatThrownBy(() -> fiscalYearTransformer.transform(input))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("FiscalYear이 아닌 객체를 전달하면 IllegalArgumentException을 발생시킨다")
        void transform_InvalidType_ThrowsException() {
            // Given
            Object invalidValue = "not a fiscal year";

            // When & Then
            assertThatThrownBy(() -> fiscalYearTransformer.transform(invalidValue))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
