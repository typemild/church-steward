package org.church.steward.domain.fiscalyear.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.church.steward.domain.fiscalyear.entity.FiscalYear;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class FiscalYearRepositoryTest {

    @Autowired
    private FiscalYearRepository fiscalYearRepository;

    @Autowired
    private TestEntityManager entityManager;

    /**
     * 이름이 "2024년"이고 기간이 그 해 1월 1일 ~ 12월 31일인 회계 연도를 저장한다.
     */
    private FiscalYear persistFiscalYear(int year) {
        return persistFiscalYear(year + "년", LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
    }

    private FiscalYear persistFiscalYear(String name, LocalDate startDate, LocalDate endDate) {
        return entityManager.persist(FiscalYear.builder()
                .name(name)
                .startDate(startDate)
                .endDate(endDate)
                .build());
    }

    @Nested
    @DisplayName("findAllByOrderByStartDateDesc 테스트")
    class FindAllByOrderByStartDateDescTest {

        @Test
        @DisplayName("시작일이 늦은 순서로 모든 회계 연도를 조회한다")
        void findAllByOrderByStartDateDesc_whenExistMultipleFiscalYears_thenReturnLatestFirst() {
            // Given
            persistFiscalYear(2023);
            persistFiscalYear(2025);
            persistFiscalYear(2024);
            entityManager.flush();

            // When
            List<FiscalYear> result = fiscalYearRepository.findAllByOrderByStartDateDesc();

            // Then
            assertThat(result).extracting(FiscalYear::getName)
                    .containsExactly("2025년", "2024년", "2023년");
        }

        @Test
        @DisplayName("한 해에 회계 연도가 여럿이어도 시작일이 늦은 순서로 조회한다")
        void findAllByOrderByStartDateDesc_whenTwoFiscalYearsInSameYear_thenReturnLatestFirst() {
            // Given
            persistFiscalYear("2025년 상반기", LocalDate.of(2025, 1, 1), LocalDate.of(2025, 6, 30));
            persistFiscalYear("2025년 하반기", LocalDate.of(2025, 7, 1), LocalDate.of(2025, 12, 31));
            entityManager.flush();

            // When
            List<FiscalYear> result = fiscalYearRepository.findAllByOrderByStartDateDesc();

            // Then
            assertThat(result).extracting(FiscalYear::getName)
                    .containsExactly("2025년 하반기", "2025년 상반기");
        }
    }

    @Nested
    @DisplayName("findFirstByOrderByStartDateDesc 테스트")
    class FindFirstByOrderByStartDateDescTest {

        @Test
        @DisplayName("시작일이 가장 늦은 회계 연도를 하나 조회한다")
        void findFirstByOrderByStartDateDesc_whenExistMultipleFiscalYears_thenReturnLatest() {
            // Given
            persistFiscalYear(2022);
            persistFiscalYear(2024);
            entityManager.flush();

            // When
            Optional<FiscalYear> result = fiscalYearRepository.findFirstByOrderByStartDateDesc();

            // Then
            assertThat(result).map(FiscalYear::getName)
                    .contains("2024년");
        }

        @Test
        @DisplayName("데이터가 없을 경우 빈 Optional을 반환한다")
        void findFirstByOrderByStartDateDesc_whenNoneExist_thenReturnEmptyOptional() {
            // When
            Optional<FiscalYear> result = fiscalYearRepository.findFirstByOrderByStartDateDesc();

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findFirstByStartDateLessThanOrderByStartDateDesc 테스트")
    class FindFirstByStartDateLessThanOrderByStartDateDescTest {

        @Test
        @DisplayName("시작일이 주어진 날짜보다 앞선 회계 연도 중 가장 최근 것을 조회한다")
        void findPrevious_whenEarlierFiscalYearsExist_thenReturnClosestOne() {
            // Given
            persistFiscalYear(2022);
            persistFiscalYear(2023);
            persistFiscalYear(2025);
            entityManager.flush();

            // When
            Optional<FiscalYear> result = fiscalYearRepository
                    .findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate.of(2025, 1, 1));

            // Then
            assertThat(result).map(FiscalYear::getName)
                    .contains("2023년");
        }

        @Test
        @DisplayName("시작일이 주어진 날짜와 같은 회계 연도는 조회하지 않는다")
        void findPrevious_whenOnlySameStartDateExists_thenReturnEmptyOptional() {
            // Given
            persistFiscalYear(2025);
            entityManager.flush();

            // When
            Optional<FiscalYear> result = fiscalYearRepository
                    .findFirstByStartDateLessThanOrderByStartDateDesc(LocalDate.of(2025, 1, 1));

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("existsByName 테스트")
    class ExistsByNameTest {

        @Test
        @DisplayName("같은 이름의 회계 연도가 있으면 true를 반환한다")
        void existsByName_whenNameExists_thenReturnTrue() {
            // Given
            persistFiscalYear(2024);
            entityManager.flush();

            // When
            boolean exists = fiscalYearRepository.existsByName("2024년");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("같은 이름의 회계 연도가 없으면 false를 반환한다")
        void existsByName_whenNameDoesNotExist_thenReturnFalse() {
            // When
            boolean exists = fiscalYearRepository.existsByName("2024년");

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("existsByNameAndIdNot 테스트")
    class ExistsByNameAndIdNotTest {

        @Test
        @DisplayName("다른 회계 연도가 같은 이름을 쓰고 있으면 true를 반환한다")
        void existsByNameAndIdNot_whenOtherFiscalYearHasName_thenReturnTrue() {
            // Given
            persistFiscalYear(2024);
            FiscalYear other = persistFiscalYear(2025);
            entityManager.flush();

            // When
            boolean exists = fiscalYearRepository.existsByNameAndIdNot("2024년", other.getId());

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("같은 이름을 쓰는 것이 자기 자신뿐이면 false를 반환한다")
        void existsByNameAndIdNot_whenOnlyItselfHasName_thenReturnFalse() {
            // Given
            FiscalYear fiscalYear = persistFiscalYear(2024);
            entityManager.flush();

            // When
            boolean exists = fiscalYearRepository.existsByNameAndIdNot("2024년", fiscalYear.getId());

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual 테스트")
    class FindAllByStartDateLessThanEqualAndEndDateGreaterThanEqualTest {

        private void persistFiscalYear2024() {
            persistFiscalYear("2024년", LocalDate.of(2023, 12, 1), LocalDate.of(2024, 11, 30));
            entityManager.flush();
        }

        private List<FiscalYear> findOverlapped(LocalDate startDate, LocalDate endDate) {
            return fiscalYearRepository.findAllByStartDateLessThanEqualAndEndDateGreaterThanEqual(endDate, startDate);
        }

        @Test
        @DisplayName("주어진 기간의 시작일이 다른 회계 연도의 종료일과 같으면 겹치는 회계 연도로 조회한다")
        void findOverlapped_whenStartDateEqualsOtherEndDate_thenReturnOverlappedFiscalYear() {
            // Given
            persistFiscalYear2024();

            // When
            List<FiscalYear> result = findOverlapped(LocalDate.of(2024, 11, 30), LocalDate.of(2025, 11, 29));

            // Then
            assertThat(result).extracting(FiscalYear::getName)
                    .containsExactly("2024년");
        }

        @Test
        @DisplayName("주어진 기간이 다른 회계 연도의 기간 안에 완전히 들어 있으면 겹치는 회계 연도로 조회한다")
        void findOverlapped_whenPeriodInsideOtherPeriod_thenReturnOverlappedFiscalYear() {
            // Given
            persistFiscalYear2024();

            // When
            List<FiscalYear> result = findOverlapped(LocalDate.of(2024, 3, 1), LocalDate.of(2024, 3, 31));

            // Then
            assertThat(result).extracting(FiscalYear::getName)
                    .containsExactly("2024년");
        }

        @Test
        @DisplayName("주어진 기간이 다른 회계 연도의 종료일 바로 다음 날에 시작하면 겹치지 않는다")
        void findOverlapped_whenPeriodStartsRightAfterOtherEndDate_thenReturnEmptyList() {
            // Given
            persistFiscalYear2024();

            // When
            List<FiscalYear> result = findOverlapped(LocalDate.of(2024, 12, 1), LocalDate.of(2025, 11, 30));

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("주어진 기간이 다른 회계 연도의 시작일 바로 전날에 끝나면 겹치지 않는다")
        void findOverlapped_whenPeriodEndsRightBeforeOtherStartDate_thenReturnEmptyList() {
            // Given
            persistFiscalYear2024();

            // When
            List<FiscalYear> result = findOverlapped(LocalDate.of(2022, 12, 1), LocalDate.of(2023, 11, 30));

            // Then
            assertThat(result).isEmpty();
        }
    }
}
