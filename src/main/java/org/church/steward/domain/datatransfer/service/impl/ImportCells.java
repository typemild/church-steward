package org.church.steward.domain.datatransfer.service.impl;

import java.math.BigDecimal;
import java.util.List;

/**
 * 가져오기에서 파일의 칸을 읽는 도구. 예산 가져오기와 수입/지출 가져오기가 함께 쓴다. 칸의 값은 글자, 정수({@link Long}),
 * 소수({@link Double}), 날짜 또는 빈 칸(null)이다.
 */
final class ImportCells {

    private ImportCells() {
        // 인스턴스화 방지
    }

    /**
     * 행에서 칸의 값을 꺼낸다. 열을 지정하지 않았거나(null) 행에 없는 열이면 null이다.
     */
    static Object cell(List<Object> row, Integer column) {
        return column != null && column < row.size() ? row.get(column) : null;
    }

    /**
     * 행에서 칸의 값을 앞뒤 공백을 뺀 글자로 꺼낸다. 빈 칸은 빈 문자열이다.
     */
    static String text(List<Object> row, Integer column) {
        return toText(cell(row, column));
    }

    /**
     * 칸의 값을 앞뒤 공백을 뺀 글자로 바꾼다. 빈 칸은 빈 문자열이다.
     */
    static String toText(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double number) {
            return BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
        }
        return value.toString().strip();
    }

    /**
     * 금액을 읽는다. 숫자 셀과 "1,000,000원" 같은 글자를 모두 받는다.
     *
     * @return 금액. 칸이 비어 있으면 null
     * @throws IllegalArgumentException 금액으로 읽을 수 없거나 음수일 때 발생. 메시지가 오류 사유다.
     */
    static Long amount(Object value) {
        Long amount;
        if (value == null) {
            return null;
        } else if (value instanceof Long number) {
            amount = number;
        } else if (value instanceof String text) {
            String digits = text.replaceAll("[,\\s원₩]", "");
            if (digits.isEmpty()) {
                return null;
            }
            if (!digits.matches("-?\\d{1,18}")) {
                throw new IllegalArgumentException("금액을 읽을 수 없습니다: " + text.strip());
            }
            amount = Long.parseLong(digits);
        } else {
            throw new IllegalArgumentException("금액을 읽을 수 없습니다: " + toText(value));
        }

        if (amount < 0) {
            throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
        }
        return amount;
    }

}
