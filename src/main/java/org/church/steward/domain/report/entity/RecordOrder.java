package org.church.steward.domain.report.entity;

import java.text.Collator;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 한 예산 안의 내역을 늘어놓는 순서. 주간 보고서의 내역 표와 수입/지출 관리의 예산별 보기가 표마다 정한다. 날짜순을 먼저 하고 같은
 * 날짜끼리의 순서를 고르는 것과, 일자를 숨기고 이름순으로 보려는 경우를 위해 첫 번째 칸의 가나다순을 먼저 하는 것이 있다.
 */
@Getter
@AllArgsConstructor
public enum RecordOrder {

    /** 날짜순, 같은 날짜는 첫 번째 칸(관계자 또는 내용)의 가나다순 */
    FIRST_COLUMN("날짜순 + 첫 번째 칸의 가나다순"),
    /** 날짜순, 같은 날짜는 입력한 순서(ID 오름차순) */
    ENTRY("날짜순 + 입력순"),
    /** 첫 번째 칸의 가나다순, 같은 값은 날짜순 */
    FIRST_COLUMN_DATE("첫 번째 칸의 가나다순 + 날짜순");

    /** 설정 화면에 보일 이름. 무엇을 먼저 견주는지 함께 적어 따로 설명하지 않아도 알아보게 한다. */
    private final String label;

    /**
     * 내역끼리 견주는 비교기. 가나다순은 첫 번째 칸의 값을 한국어 순서로 견주고 값이 없는 내역을 뒤에 둔다. 어느 순서든 견준 값이 모두
     * 같은 내역은 입력순이어야 하므로, 부르는 쪽이 이 뒤에 ID 순서를 잇거나 입력순으로 늘어선 목록을 안정 정렬한다.
     *
     * @param date       내역의 일자
     * @param firstValue 내역의 첫 번째 칸에 적는 값
     */
    public <T> Comparator<T> comparator(Function<? super T, LocalDate> date, Function<? super T, String> firstValue) {
        Comparator<T> byDate = Comparator.comparing(date);
        return switch (this) {
        case FIRST_COLUMN -> byDate.thenComparing(byFirstColumn(firstValue));
        case ENTRY -> byDate;
        case FIRST_COLUMN_DATE -> RecordOrder.<T>byFirstColumn(firstValue)
                .thenComparing(byDate);
        };
    }

    private static <T> Comparator<T> byFirstColumn(Function<? super T, String> firstValue) {
        // Collator는 스레드에 안전하다고 보장되지 않으므로 비교기마다 따로 만든다.
        Collator collator = Collator.getInstance(Locale.KOREAN);
        return Comparator.comparing(record -> blankToNull(firstValue.apply(record)),
                Comparator.nullsLast(collator::compare));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
