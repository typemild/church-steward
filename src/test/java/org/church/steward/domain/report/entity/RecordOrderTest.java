package org.church.steward.domain.report.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RecordOrderTest {

    private static final LocalDate DAY1 = LocalDate.of(2026, 10, 4);
    private static final LocalDate DAY2 = LocalDate.of(2026, 10, 5);

    /**
     * 정렬할 내역
     *
     * @param name 첫 번째 칸의 값
     * @param id   받은 순서(입력순)를 확인하기 위한 번호
     */
    private record Item(LocalDate date, String name, int id) {
    }

    /**
     * 받은 순서대로 늘어선 내역을 안정 정렬한 뒤 번호를 꺼낸다.
     */
    private static List<Integer> sort(RecordOrder recordOrder, Item... items) {
        List<Item> sorted = new ArrayList<>(List.of(items));
        sorted.sort(recordOrder.comparator(Item::date, Item::name));
        return sorted.stream()
                .map(Item::id)
                .toList();
    }

    @Nested
    @DisplayName("comparator 테스트")
    class ComparatorTest {

        @Test
        @DisplayName("날짜순 + 가나다순은 날짜가 앞선 내역을 먼저 두고 같은 날짜는 첫 번째 칸의 값을 한국어 순서로 늘어놓는다")
        void comparator_FirstColumn_SortsByDateThenKoreanOrder() {
            // When
            List<Integer> actual = sort(RecordOrder.FIRST_COLUMN, new Item(DAY2, "김철수", 1), new Item(DAY1, "홍길동", 2),
                    new Item(DAY1, "이영희", 3));

            // Then
            assertThat(actual).containsExactly(3, 2, 1);
        }

        @Test
        @DisplayName("가나다순에서 값이 없거나 공백뿐인 내역은 같은 날짜의 뒤에 둔다")
        void comparator_FirstColumnBlankValue_SortsLast() {
            // When
            List<Integer> actual = sort(RecordOrder.FIRST_COLUMN, new Item(DAY1, null, 1), new Item(DAY1, "홍길동", 2),
                    new Item(DAY1, " ", 3), new Item(DAY1, "김철수", 4));

            // Then
            assertThat(actual).containsExactly(4, 2, 1, 3);
        }

        @Test
        @DisplayName("날짜와 값이 같은 내역은 받은 순서를 지킨다")
        void comparator_FirstColumnSameValue_KeepsOrder() {
            // When
            List<Integer> actual = sort(RecordOrder.FIRST_COLUMN, new Item(DAY1, "홍길동", 1), new Item(DAY1, "김철수", 2),
                    new Item(DAY1, "홍길동", 3));

            // Then
            assertThat(actual).containsExactly(2, 1, 3);
        }

        @Test
        @DisplayName("날짜순 + 입력순은 날짜만 견주고 같은 날짜는 받은 순서를 그대로 둔다")
        void comparator_Entry_SortsByDateOnly() {
            // When
            List<Integer> actual = sort(RecordOrder.ENTRY, new Item(DAY2, "김철수", 1), new Item(DAY1, "홍길동", 2),
                    new Item(DAY1, "이영희", 3));

            // Then
            assertThat(actual).containsExactly(2, 3, 1);
        }

        @Test
        @DisplayName("가나다순 + 날짜순은 날짜와 무관하게 첫 번째 칸의 값을 먼저 견주고 같은 값은 날짜순으로 둔다")
        void comparator_FirstColumnDate_SortsByKoreanOrderThenDate() {
            // When
            List<Integer> actual = sort(RecordOrder.FIRST_COLUMN_DATE, new Item(DAY1, "홍길동", 1),
                    new Item(DAY2, "김철수", 2), new Item(DAY1, "김철수", 3));

            // Then
            assertThat(actual).containsExactly(3, 2, 1);
        }

        @Test
        @DisplayName("가나다순 + 날짜순에서 값이 없는 내역은 날짜와 무관하게 맨 뒤에 둔다")
        void comparator_FirstColumnDateBlankValue_SortsLast() {
            // When
            List<Integer> actual = sort(RecordOrder.FIRST_COLUMN_DATE, new Item(DAY1, null, 1),
                    new Item(DAY2, "홍길동", 2));

            // Then
            assertThat(actual).containsExactly(2, 1);
        }
    }
}
