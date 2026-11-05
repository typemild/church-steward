package org.church.steward.domain.report.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecordOrderTest {

    /**
     * 값을 그대로 첫 번째 칸의 값으로 쓰는 비교기로 정렬한다. 정렬은 안정 정렬이다.
     */
    private static List<String> sort(RecordOrder recordOrder, String... values) {
        List<String> sorted = new ArrayList<>(Arrays.asList(values));
        Comparator<String> comparator = recordOrder.sameDateComparator(Function.identity());
        sorted.sort(comparator);
        return sorted;
    }

    @Nested
    @DisplayName("sameDateComparator 테스트")
    class SameDateComparatorTest {

        @Test
        @DisplayName("가나다순은 첫 번째 칸의 값을 한국어 순서로 늘어놓는다")
        void sameDateComparator_FirstColumn_SortsInKoreanOrder() {
            // When
            List<String> actual = sort(RecordOrder.FIRST_COLUMN, "홍길동", "김철수", "이영희");

            // Then
            assertThat(actual).containsExactly("김철수", "이영희", "홍길동");
        }

        @Test
        @DisplayName("가나다순에서 값이 없거나 공백뿐인 내역은 뒤에 둔다")
        void sameDateComparator_FirstColumnBlankValue_SortsLast() {
            // When
            List<String> actual = sort(RecordOrder.FIRST_COLUMN, null, "홍길동", " ", "김철수");

            // Then
            assertThat(actual).containsExactly("김철수", "홍길동", null, " ");
        }

        @Test
        @DisplayName("가나다순에서 값이 같은 내역은 받은 순서를 지킨다")
        void sameDateComparator_FirstColumnSameValue_KeepsOrder() {
            // Given
            List<String[]> records = new ArrayList<>(List.of(new String[] { "홍길동", "1" },
                    new String[] { "김철수", "2" }, new String[] { "홍길동", "3" }));
            Comparator<String[]> comparator = RecordOrder.FIRST_COLUMN
                    .sameDateComparator((String[] record) -> record[0]);

            // When
            records.sort(comparator);

            // Then
            assertThat(records).extracting(record -> record[1])
                    .containsExactly("2", "1", "3");
        }

        @Test
        @DisplayName("입력순은 모든 내역을 같다고 보므로 받은 순서를 그대로 둔다")
        void sameDateComparator_Entry_KeepsOrder() {
            // When
            List<String> actual = sort(RecordOrder.ENTRY, "홍길동", "김철수", "이영희");

            // Then
            assertThat(actual).containsExactly("홍길동", "김철수", "이영희");
        }
    }
}
