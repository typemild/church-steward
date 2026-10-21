package org.church.steward.ui.view.activitylog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ActivityLogFieldFormatterTest {

    private ActivityLogResponseDto createDto(ActivityLogFieldDto... fields) {
        return ActivityLogResponseDto.builder()
                .fields(List.of(fields))
                .build();
    }

    private ActivityLogFieldDto unchangedField(String name, String value) {
        return ActivityLogFieldDto.builder()
                .name(name)
                .value(value)
                .build();
    }

    private ActivityLogFieldDto changedField(String name, String oldValue, String value) {
        return ActivityLogFieldDto.builder()
                .name(name)
                .oldValue(oldValue)
                .value(value)
                .changed(true)
                .build();
    }

    @Nested
    @DisplayName("toSummary 테스트")
    class ToSummaryTest {

        @Test
        @DisplayName("변경되지 않은 항목은 \"논리명: 값\" 형식으로 요약한다")
        void toSummary_unchangedField_returnsNameAndValue() {
            // Given
            ActivityLogResponseDto dto = createDto(unchangedField("이름", "홍길동"));

            // When
            String result = ActivityLogFieldFormatter.toSummary(dto);

            // Then
            assertThat(result).isEqualTo("이름: 홍길동");
        }

        @Test
        @DisplayName("변경된 항목은 \"논리명 : 변경 전 값 → 변경 후 값\" 형식으로 요약한다")
        void toSummary_changedField_returnsOldAndNewValue() {
            // Given
            ActivityLogResponseDto dto = createDto(changedField("이름", "옛날이름", "새이름"));

            // When
            String result = ActivityLogFieldFormatter.toSummary(dto);

            // Then
            assertThat(result).isEqualTo("이름: 옛날이름 → 새이름");
        }

        @Test
        @DisplayName("항목이 여러 개이면 기록된 순서대로 쉼표로 이어 붙인다")
        void toSummary_multipleFields_joinsWithCommaInOrder() {
            // Given
            ActivityLogResponseDto dto = createDto(
                    unchangedField("ID", "1"),
                    changedField("이름", "옛날이름", "새이름"));

            // When
            String result = ActivityLogFieldFormatter.toSummary(dto);

            // Then
            assertThat(result).isEqualTo("ID: 1, 이름: 옛날이름 → 새이름");
        }

        @Test
        @DisplayName("변경 전 값이나 변경 후 값이 비어 있으면 \"(없음)\"으로 요약한다")
        void toSummary_emptyValues_returnsEmptyValueText() {
            // Given
            ActivityLogResponseDto dto = createDto(
                    changedField("비고", "", "메모"),
                    changedField("내역", "주일헌금", ""));

            // When
            String result = ActivityLogFieldFormatter.toSummary(dto);

            // Then
            assertThat(result).isEqualTo("비고: (없음) → 메모, 내역: 주일헌금 → (없음)");
        }

        @Test
        @DisplayName("기록된 항목이 없으면 빈 문자열을 반환한다")
        void toSummary_noFields_returnsEmptyString() {
            // Given
            ActivityLogResponseDto dto = createDto();

            // When
            String result = ActivityLogFieldFormatter.toSummary(dto);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("항목 목록만 주어도 같은 형식으로 요약한다 (가져오기 기록의 건별 내용)")
        void toSummary_fieldList_returnsSameFormat() {
            // Given
            List<ActivityLogFieldDto> fields = List.of(
                    ActivityLogFieldDto.builder()
                            .name("예산명")
                            .value("십일조")
                            .build(),
                    ActivityLogFieldDto.builder()
                            .name("예산")
                            .oldValue("100원")
                            .value("200원")
                            .changed(true)
                            .build());

            // When
            String result = ActivityLogFieldFormatter.toSummary(fields);

            // Then
            assertThat(result).isEqualTo("예산명: 십일조, 예산: 100원 → 200원");
        }
    }

    @Nested
    @DisplayName("toDisplayValue 테스트")
    class ToDisplayValueTest {

        @Test
        @DisplayName("값이 있으면 그대로 반환한다")
        void toDisplayValue_nonEmptyValue_returnsValue() {
            // Given
            String value = "홍길동";

            // When
            String result = ActivityLogFieldFormatter.toDisplayValue(value);

            // Then
            assertThat(result).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("값이 빈 문자열이면 \"(없음)\"을 반환한다")
        void toDisplayValue_emptyValue_returnsEmptyValueText() {
            // Given
            String value = "";

            // When
            String result = ActivityLogFieldFormatter.toDisplayValue(value);

            // Then
            assertThat(result).isEqualTo("(없음)");
        }

        @Test
        @DisplayName("값이 null이면 \"(없음)\"을 반환한다")
        void toDisplayValue_nullValue_returnsEmptyValueText() {
            // Given
            String value = null;

            // When
            String result = ActivityLogFieldFormatter.toDisplayValue(value);

            // Then
            assertThat(result).isEqualTo("(없음)");
        }

        @Test
        @DisplayName("공백만 있는 값은 비어 있는 것으로 보지 않고 그대로 반환한다")
        void toDisplayValue_blankValue_returnsValue() {
            // Given
            String value = " ";

            // When
            String result = ActivityLogFieldFormatter.toDisplayValue(value);

            // Then
            assertThat(result).isEqualTo(" ");
        }
    }
}
