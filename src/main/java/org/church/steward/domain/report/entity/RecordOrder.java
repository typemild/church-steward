package org.church.steward.domain.report.entity;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.Function;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 내역을 일자순으로 늘어놓을 때 같은 날짜의 내역끼리의 순서. 주간 보고서의 내역 표와 수입/지출 관리의 예산별 보기가 표마다 정한다.
 */
@Getter
@AllArgsConstructor
public enum RecordOrder {

    /** 첫 번째 칸(관계자 또는 내용)의 가나다순 */
    FIRST_COLUMN("날짜순 + 첫 번째 칸의 가나다순"),
    /** 입력한 순서(ID 오름차순) */
    ENTRY("날짜순 + 입력순");

    /** 설정 화면에 보일 이름. 날짜순이 먼저임을 함께 적어 따로 설명하지 않아도 알아보게 한다. */
    private final String label;

    /**
     * 같은 날짜의 내역끼리 견주는 비교기. 가나다순은 첫 번째 칸의 값을 한국어 순서로 견주고 값이 없는 내역을 뒤에 둔다. 입력순은 모두
     * 같다고 본다. 어느 쪽이든 견준 값이 같은 내역은 입력순이어야 하므로, 부르는 쪽이 이 뒤에 ID 순서를 잇거나 입력순으로 늘어선 목록을
     * 안정 정렬한다.
     *
     * @param firstValue 내역의 첫 번째 칸에 적는 값
     */
    public <T> Comparator<T> sameDateComparator(Function<? super T, String> firstValue) {
        if (this == ENTRY) {
            return (_, _) -> 0;
        }
        // Collator는 스레드에 안전하다고 보장되지 않으므로 비교기마다 따로 만든다.
        Collator collator = Collator.getInstance(Locale.KOREAN);
        return Comparator.<T, String>comparing(record -> blankToNull(firstValue.apply(record)),
                Comparator.nullsLast(collator::compare));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
