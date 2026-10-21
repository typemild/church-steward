package org.church.steward.infrastructure.spreadsheet;

import java.util.List;

/**
 * 머리글 한 줄과 그 아래의 행들로 이루어진 표.
 * 칸의 값은 글자({@link String}), 수({@link Number}), 날짜({@link java.time.LocalDate})
 * 또는 빈 칸(null)이다.
 *
 * @param name    표의 이름. 엑셀에서는 시트 이름이 되며 시트 이름에 쓸 수 없는 글자(/ \ ? * [ ] :)는 넣지
 *                않는다.
 * @param headers 머리글
 * @param rows    행 목록. 각 행의 칸 수는 머리글의 칸 수와 같다.
 */
public record SpreadsheetTable(String name, List<String> headers, List<List<Object>> rows) {
}
