package org.church.steward.domain.fiscalyear.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FiscalYearMapperTest {

    private final FiscalYearMapper fiscalYearMapper = new FiscalYearMapper();

    @Nested
    @DisplayName("toDto 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("FiscalYear 엔티티가 null이면 null을 반환한다")
        void toDto_nullEntity_returnsNull() {
            // Given
            FiscalYear fiscalYear = null;

            // When
            FiscalYearDto result = fiscalYearMapper.toDto(fiscalYear);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("FiscalYear 엔티티의 ID와 이름을 DTO에 옮긴다")
        void toDto_validEntity_returnsDtoWithIdAndName() {
            // Given
            FiscalYear fiscalYear = FiscalYear.builder()
                    .id(7L)
                    .name("2024년")
                    .build();

            // When
            FiscalYearDto result = fiscalYearMapper.toDto(fiscalYear);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(7L);
            assertThat(result.getName()).isEqualTo("2024년");
        }

        @Test
        @DisplayName("FiscalYear 엔티티의 기간(시작일, 종료일)을 DTO에 옮긴다")
        void toDto_entityWithPeriod_returnsDtoWithPeriod() {
            // Given
            LocalDate startDate = LocalDate.of(2023, 12, 1);
            LocalDate endDate = LocalDate.of(2024, 11, 30);
            FiscalYear fiscalYear = FiscalYear.builder()
                    .id(7L)
                    .name("2024년")
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            // When
            FiscalYearDto result = fiscalYearMapper.toDto(fiscalYear);

            // Then
            assertThat(result.getStartDate()).isEqualTo(startDate);
            assertThat(result.getEndDate()).isEqualTo(endDate);
        }

        @Test
        @DisplayName("마감하지 않은 회계 연도는 마감 여부가 false이고 총 수입, 총 지출, 차액이 null이다")
        void toDto_openEntity_returnsDtoWithoutTotals() {
            // Given
            FiscalYear fiscalYear = FiscalYear.builder()
                    .id(7L)
                    .name("2024년")
                    .build();

            // When
            FiscalYearDto result = fiscalYearMapper.toDto(fiscalYear);

            // Then
            assertThat(result.isClosed()).isFalse();
            assertThat(result.getTotalIncome()).isNull();
            assertThat(result.getTotalExpense()).isNull();
            assertThat(result.getBalance()).isNull();
        }

        @Test
        @DisplayName("마감한 회계 연도의 마감 여부와 총 수입, 총 지출, 차액을 DTO에 옮긴다")
        void toDto_closedEntity_returnsDtoWithTotals() {
            // Given
            FiscalYear fiscalYear = FiscalYear.builder()
                    .id(7L)
                    .name("2024년")
                    .build();
            fiscalYear.close(5_000L, 3_000L);

            // When
            FiscalYearDto result = fiscalYearMapper.toDto(fiscalYear);

            // Then
            assertThat(result.isClosed()).isTrue();
            assertThat(result.getTotalIncome()).isEqualTo(5_000L);
            assertThat(result.getTotalExpense()).isEqualTo(3_000L);
            assertThat(result.getBalance()).isEqualTo(2_000L);
        }
    }
}
