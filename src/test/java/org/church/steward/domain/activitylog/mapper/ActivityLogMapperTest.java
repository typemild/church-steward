package org.church.steward.domain.activitylog.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.List;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.entity.ActivityLog;
import org.church.steward.domain.activitylog.entity.ActivityType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

class ActivityLogMapperTest {

    private final ActivityLogMapper activityLogMapper = new ActivityLogMapper(new ObjectMapper());

    @Nested
    @DisplayName("toEntity 테스트")
    class ToEntityTest {

        @Test
        @DisplayName("사용자 ID, 행위 유형, 대상 논리명을 엔터티에 그대로 담는다")
        void toEntity_validArguments_setsBasicFields() {
            // Given
            List<ActivityLogFieldDto> fields = List.of();

            // When
            ActivityLog result = activityLogMapper.toEntity("test-user", ActivityType.CREATE, "테스트 엔터티", fields);

            // Then
            assertThat(result.getUserId()).isEqualTo("test-user");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.CREATE);
            assertThat(result.getTargetName()).isEqualTo("테스트 엔터티");
        }

        @Test
        @DisplayName("항목 목록을 순서대로 JSON으로 저장하고, 변경된 항목에만 변경 전 값을 기록한다")
        void toEntity_changedAndUnchangedFields_writesOldValueOnlyForChangedFields() {
            // Given
            List<ActivityLogFieldDto> fields = List.of(
                    ActivityLogFieldDto.builder()
                            .name("ID")
                            .value("1")
                            .build(),
                    ActivityLogFieldDto.builder()
                            .name("이름")
                            .oldValue("옛날이름")
                            .value("새이름")
                            .changed(true)
                            .build());

            // When
            ActivityLog result = activityLogMapper.toEntity("test-user", ActivityType.UPDATE, "테스트 엔터티", fields);

            // Then
            assertThat(result.getDescription()).isEqualTo("{\"fields\":[{\"name\":\"ID\",\"value\":\"1\"},"
                    + "{\"name\":\"이름\",\"oldValue\":\"옛날이름\",\"value\":\"새이름\"}]}");
        }

        @Test
        @DisplayName("건별 항목 목록을 주면 요약 항목 뒤에 건마다 항목 목록을 순서대로 저장한다")
        void toEntity_withItems_writesItemsAfterFields() {
            // Given
            List<ActivityLogFieldDto> fields = List.of(
                    ActivityLogFieldDto.builder()
                            .name("결과")
                            .value("추가 1건, 금액 갱신 1건")
                            .build());
            List<List<ActivityLogFieldDto>> items = List.of(
                    List.of(ActivityLogFieldDto.builder()
                            .name("예산명")
                            .value("십일조")
                            .build()),
                    List.of(ActivityLogFieldDto.builder()
                            .name("예산")
                            .oldValue("100원")
                            .value("200원")
                            .changed(true)
                            .build()));

            // When
            ActivityLog result = activityLogMapper.toEntity("test-user", ActivityType.IMPORT, "예산", fields, items);

            // Then
            assertThat(result.getDescription()).isEqualTo(
                    "{\"fields\":[{\"name\":\"결과\",\"value\":\"추가 1건, 금액 갱신 1건\"}],"
                            + "\"items\":[{\"fields\":[{\"name\":\"예산명\",\"value\":\"십일조\"}]},"
                            + "{\"fields\":[{\"name\":\"예산\",\"oldValue\":\"100원\",\"value\":\"200원\"}]}]}");
        }

        @Test
        @DisplayName("값이 null인 항목은 빈 문자열로 저장한다")
        void toEntity_nullValues_writesEmptyStrings() {
            // Given
            List<ActivityLogFieldDto> fields = List.of(
                    ActivityLogFieldDto.builder()
                            .name("비고")
                            .changed(true)
                            .build());

            // When
            ActivityLog result = activityLogMapper.toEntity("test-user", ActivityType.UPDATE, "테스트 엔터티", fields);

            // Then
            assertThat(result.getDescription())
                    .isEqualTo("{\"fields\":[{\"name\":\"비고\",\"oldValue\":\"\",\"value\":\"\"}]}");
        }
    }

    @Nested
    @DisplayName("toResponseDto 테스트")
    class ToResponseDtoTest {

        @Test
        @DisplayName("ActivityLog 엔터티가 null이면 null을 반환한다")
        void toResponseDto_nullEntity_returnsNull() {
            // Given
            ActivityLog entity = null;

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("엔터티의 기본 정보를 DTO로 변환한다")
        void toResponseDto_validEntity_mapsBasicFields() {
            // Given
            LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
            ActivityLog entity = createEntity("{\"fields\":[]}", createdAt);

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getUserId()).isEqualTo("test-user");
            assertThat(result.getActivityType()).isEqualTo(ActivityType.UPDATE);
            assertThat(result.getTargetName()).isEqualTo("테스트 엔터티");
            assertThat(result.getCreatedAt()).isEqualTo(createdAt);
        }

        @Test
        @DisplayName("저장된 JSON을 항목 목록으로 변환하고, 변경 전 값이 있는 항목만 변경된 것으로 표시한다")
        void toResponseDto_jsonDescription_returnsParsedFields() {
            // Given
            ActivityLog entity = createEntity("{\"fields\":[{\"name\":\"ID\",\"value\":\"1\"},"
                    + "{\"name\":\"이름\",\"oldValue\":\"옛날이름\",\"value\":\"새이름\"}]}", LocalDateTime.now());

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result.getFields())
                    .extracting(ActivityLogFieldDto::getName, ActivityLogFieldDto::getOldValue,
                            ActivityLogFieldDto::getValue, ActivityLogFieldDto::isChanged)
                    .containsExactly(
                            tuple("ID", null, "1", false),
                            tuple("이름", "옛날이름", "새이름", true));
        }

        @Test
        @DisplayName("변경 전 값이 빈 문자열이어도 변경된 항목으로 표시한다")
        void toResponseDto_emptyOldValue_marksFieldAsChanged() {
            // Given
            ActivityLog entity = createEntity(
                    "{\"fields\":[{\"name\":\"비고\",\"oldValue\":\"\",\"value\":\"메모\"}]}", LocalDateTime.now());

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result.getFields())
                    .extracting(ActivityLogFieldDto::getOldValue, ActivityLogFieldDto::isChanged)
                    .containsExactly(tuple("", true));
        }

        @Test
        @DisplayName("건별 항목 목록이 저장된 JSON이면 건마다 항목 목록으로 변환한다")
        void toResponseDto_jsonWithItems_returnsParsedItems() {
            // Given
            ActivityLog entity = createEntity("{\"fields\":[{\"name\":\"결과\",\"value\":\"추가 2건\"}],"
                    + "\"items\":[{\"fields\":[{\"name\":\"예산명\",\"value\":\"십일조\"}]},"
                    + "{\"fields\":[{\"name\":\"예산\",\"oldValue\":\"100원\",\"value\":\"200원\"}]}]}",
                    LocalDateTime.now());

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result.getItems())
                    .extracting(item -> item.getFirst().getName(), item -> item.getFirst().getOldValue(),
                            item -> item.getFirst().getValue(), item -> item.getFirst().isChanged())
                    .containsExactly(
                            tuple("예산명", null, "십일조", false),
                            tuple("예산", "100원", "200원", true));
        }

        @Test
        @DisplayName("건별 항목 목록이 없는 JSON이면 건별 항목 목록은 비어 있다")
        void toResponseDto_jsonWithoutItems_returnsEmptyItems() {
            // Given
            ActivityLog entity = createEntity("{\"fields\":[{\"name\":\"ID\",\"value\":\"1\"}]}", LocalDateTime.now());

            // When
            ActivityLogResponseDto result = activityLogMapper.toResponseDto(entity);

            // Then
            assertThat(result.getItems()).isEmpty();
        }

        private ActivityLog createEntity(String description, LocalDateTime createdAt) {
            return ActivityLog.builder()
                    .id(1L)
                    .userId("test-user")
                    .activityType(ActivityType.UPDATE)
                    .targetName("테스트 엔터티")
                    .description(description)
                    .createdAt(createdAt)
                    .build();
        }
    }
}
