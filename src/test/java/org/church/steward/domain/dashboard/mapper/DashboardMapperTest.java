package org.church.steward.domain.dashboard.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.domain.dashboard.dto.RecentTransactionDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecord;
import org.church.steward.domain.financialrecord.entity.FinancialRecordContact;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DashboardMapperTest {

    private final DashboardMapper dashboardMapper = new DashboardMapper();

    @Nested
    @DisplayName("toBudgetProgressDto 테스트")
    class ToBudgetProgressDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toBudgetProgressDto_nullEntity_returnsNull() {
            // Given
            Budget entity = null;

            // When
            BudgetProgressDto result = dashboardMapper.toBudgetProgressDto(entity, 1000L, 800L, 80.0);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("예산의 이름과 구분, 서비스가 계산해 넘긴 예산액, 실제 금액, 비율을 DTO로 옮긴다")
        void toBudgetProgressDto_fullEntity_returnsCorrectDto() {
            // Given
            Budget entity = Budget.builder()
                    .id(1L)
                    .name("주일헌금")
                    .type(BudgetType.INCOME)
                    .build();

            // When
            BudgetProgressDto result = dashboardMapper.toBudgetProgressDto(entity, 1000L, 800L, 80.0);

            // Then
            assertThat(result.getBudgetName()).isEqualTo("주일헌금");
            assertThat(result.getType()).isEqualTo(BudgetType.INCOME);
            assertThat(result.getBudgetedAmount()).isEqualTo(1000L);
            assertThat(result.getActualAmount()).isEqualTo(800L);
            assertThat(result.getProgressRate()).isEqualTo(80.0);
        }
    }

    @Nested
    @DisplayName("toRecentTransactionDto 테스트")
    class ToRecentTransactionDtoTest {

        @Test
        @DisplayName("엔터티가 null이면 null을 반환한다")
        void toRecentTransactionDto_nullEntity_returnsNull() {
            // Given
            FinancialRecord entity = null;

            // When
            RecentTransactionDto result = dashboardMapper.toRecentTransactionDto(entity);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("내역의 일자, 구분 이름, 예산명, 금액, 내용, 비고와 기록된 당시의 관계자 이름을 DTO로 옮긴다")
        void toRecentTransactionDto_fullEntity_returnsCorrectDto() {
            // Given
            FinancialRecord entity = FinancialRecord.builder()
                    .type(FinancialRecordType.INCOME)
                    .date(LocalDate.of(2026, 10, 4))
                    .budget(Budget.builder()
                            .name("감사헌금")
                            .build())
                    .description("추수감사")
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
            RecentTransactionDto result = dashboardMapper.toRecentTransactionDto(entity);

            // Then
            assertThat(result.getDate()).isEqualTo(LocalDate.of(2026, 10, 4));
            assertThat(result.getType()).isEqualTo("수입");
            assertThat(result.getCategory()).isEqualTo("감사헌금");
            assertThat(result.getContactNames()).containsExactly("홍길동", "김철수");
            assertThat(result.getAmount()).isEqualTo(50_000L);
            assertThat(result.getDescription()).isEqualTo("추수감사");
            assertThat(result.getRemarks()).isEqualTo("비고");
        }

        @Test
        @DisplayName("관계자가 없으면 관계자 이름은 빈 목록이다")
        void toRecentTransactionDto_nullContacts_returnsEmptyContactNames() {
            // Given
            FinancialRecord entity = FinancialRecord.builder()
                    .type(FinancialRecordType.EXPENSE)
                    .budget(Budget.builder()
                            .name("관리비")
                            .build())
                    .contacts(null)
                    .build();

            // When
            RecentTransactionDto result = dashboardMapper.toRecentTransactionDto(entity);

            // Then
            assertThat(result.getContactNames()).isEmpty();
        }
    }
}
