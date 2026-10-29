package org.church.steward.infrastructure.spreadsheet;

import java.nio.charset.Charset;
import java.util.List;

/**
 * 파일에서 읽은 그대로의 표. 어느 행이 머리글인지는 정해져 있지 않다. 칸의 값은 글자({@link String}),
 * 정수({@link Long}), 소수({@link Double}), 날짜({@link java.time.LocalDate}) 또는 빈
 * 칸(null)이다. CSV에서 읽은 값은 모두 글자다.
 *
 * @param rows    행 목록. 모든 행의 칸 수는 가장 긴 행에 맞춰져 있다.
 * @param charset CSV를 읽는 데 쓴 문자 인코딩. 엑셀 파일이면 null
 */
public record SpreadsheetData(List<List<Object>> rows, Charset charset) {

    /**
     * 칸의 수. 행이 없으면 0이다.
     */
    public int columnCount() {
        return rows.isEmpty() ? 0 : rows.getFirst().size();
    }
}
