package org.church.steward.domain.report.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.report.dto.BudgetExecutionDto;
import org.church.steward.domain.report.dto.BudgetPlanRowDto;
import org.church.steward.domain.report.dto.ReportRecordDto;
import org.church.steward.domain.report.dto.ReportRecordGroupDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ReportMapperTest {

    private final ReportMapper reportMapper = new ReportMapper();

    private FinancialRecord record(LocalDate date, long amount, String description) {
        return FinancialRecord.builder()
                .type(FinancialRecordType.INCOME)
                .date(date)
                .amount(amount)
                .description(description)
                .build();
    }

    @Nested
    @DisplayName("toRecordDto 테스트")
    class ToRecordDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toRecordDto_nullEntity_returnsNull() {
            // Given
            FinancialRecord entity = null;

            // When
            ReportRecordDto result = reportMapper.toRecordDto(entity);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("내역의 일자, 내용, 금액, 비고와 기록된 당시의 관계자 이름을 DTO로 옮긴다")
        void toRecordDto_fullEntity_returnsCorrectDto() {
            // Given
            FinancialRecord entity = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .date(LocalDate.of(2026, 10, 4))
                    .description("감사헌금")
                    .amount(50_000L)
                    .remarks("비고")
                    .contacts(List.of(
                            FinancialRecordContact.builder()
                                    .contactName("홍길동")
                                    .build(),
                            FinancialRecordContact.builder()
                                    .contactName("김철수")
                                    .build()))
                    .build();

            // When
            ReportRecordDto result = reportMapper.toRecordDto(entity);

            // Then
            assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 10, 4));
            assertThat(result.getContactNames()).containsExactly("홍길동", "김철수");
            assertThat(result.getDescription()).isEqualTo("감사헌금");
            assertThat(result.getAmount()).isEqualTo(50_000L);
            assertThat(result.getRemarks()).isEqualTo("비고");
        }

        @Test
        @DisplayName("관계자가 없는 내역은 빈 이름 목록으로 옮긴다")
        void toRecordDto_noContacts_returnsEmptyContactNames() {
            // Given
            FinancialRecord entity = record(LocalDate.of(2026, 10, 4), 1_000L, "이자");

            // When
            ReportRecordDto result = reportMapper.toRecordDto(entity);

            // Then
            assertThat(result.getContactNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("toRecordGroupDto 테스트")
    class ToRecordGroupDtoTest {

        @Test
        @DisplayName("예산명과 합계를 담고 내역을 넘겨받은 순서 그대로 옮긴다")
        void toRecordGroupDto_records_keepsGivenOrder() {
            // Given
            List<FinancialRecord> entities = List.of(
                    record(LocalDate.of(2026, 10, 5), 200L, "둘째"),
                    record(LocalDate.of(2026, 10, 4), 100L, "첫째"));

            // When
            ReportRecordGroupDto result = reportMapper.toRecordGroupDto("헌금 > 십일조", 300L, entities);

            // Then
            assertThat(result.getBudgetName()).isEqualTo("헌금 > 십일조");
            assertThat(result.getTotalAmount()).isEqualTo(300L);
            assertThat(result.getRecords()).extracting(ReportRecordDto::getDescription)
                    .containsExactly("둘째", "첫째");
        }
    }

    @Nested
    @DisplayName("toBudgetExecutionDto 테스트")
    class ToBudgetExecutionDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toBudgetExecutionDto_nullEntity_returnsNull() {
            // Given
            Budget entity = null;

            // When
            BudgetExecutionDto result = reportMapper.toBudgetExecutionDto(entity, 0, 0L, 0L, null, null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예산명과 넘겨받은 깊이, 예산액, 집행액, 비율을 DTO로 옮긴다")
        void toBudgetExecutionDto_budgetWithValues_returnsCorrectDto() {
            // Given
            Budget entity = Budget.builder()
                    .name("교육부")
                    .amount(999L)
                    .build();

            // When
            BudgetExecutionDto result = reportMapper.toBudgetExecutionDto(entity, 1, 1_000L, 400L, 25.0, 40.0);

            // Then
            assertThat(result.getName()).isEqualTo("교육부");
            assertThat(result.getDepth()).isEqualTo(1);
            assertThat(result.getBudgetedAmount()).isEqualTo(1_000L);
            assertThat(result.getActualAmount()).isEqualTo(400L);
            assertThat(result.getShareRate()).isEqualTo(25.0);
            assertThat(result.getExecutionRate()).isEqualTo(40.0);
        }
    }

    @Nested
    @DisplayName("toBudgetPlanRowDto 테스트")
    class ToBudgetPlanRowDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toBudgetPlanRowDto_nullEntity_returnsNull() {
            // Given
            Budget entity = null;

            // When
            BudgetPlanRowDto result = reportMapper.toBudgetPlanRowDto(entity, 0, 0L, null, null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예산명과 넘겨받은 깊이, 예산액, 점유율, 직전 회계 연도 금액을 DTO로 옮긴다")
        void toBudgetPlanRowDto_budgetWithValues_returnsCorrectDto() {
            // Given
            Budget entity = Budget.builder()
                    .name("십일조")
                    .build();

            // When
            BudgetPlanRowDto result = reportMapper.toBudgetPlanRowDto(entity, 1, 700L, 70.0, 500L);

            // Then
            assertThat(result.getName()).isEqualTo("십일조");
            assertThat(result.getDepth()).isEqualTo(1);
            assertThat(result.getAmount()).isEqualTo(700L);
            assertThat(result.getShareRate()).isEqualTo(70.0);
            assertThat(result.getPreviousAmount()).isEqualTo(500L);
        }
    }
}
