package org.church.steward.util;

import java.text.NumberFormat;

public class NumberUtils {

    private NumberUtils() {
        // 인스턴스화 방지
    }

    /**
     * 숫자를 "1,234조 5,678만원"와 같이 한국어 단위 표기로 변환한다. long의 범위인 경 단위까지 커버한다. 음수는 "-3만원"처럼
     * 맨 앞에 "-"를 붙인다.
     *
     * @param amount 금액
     * @return 한국어 단위 표기로 변환된 금액
     */
    public static String toKoreanAmount(long amount) {
        if (amount == 0) {
            return "0원";
        }

        // 음수는 단위별로 나눈 뒤 각각 절댓값을 취한다. 금액 전체의 부호를 먼저 바꾸면 long의 최솟값에서 넘친다.
        long temp = amount;
        long gyeong = Math.abs(temp / 1_0000_0000_0000_0000L); // 10^16
        temp %= 1_0000_0000_0000_0000L;
        long jo = Math.abs(temp / 1_0000_0000_0000L); // 10^12
        temp %= 1_0000_0000_0000L;
        long eok = Math.abs(temp / 1_0000_0000L); // 10^8
        temp %= 1_0000_0000L;
        long man = Math.abs(temp / 1_0000L); // 10^4
        long rest = Math.abs(temp % 1_0000L);

        StringBuilder sb = new StringBuilder();
        if (amount < 0) {
            sb.append("-");
        }
        if (gyeong > 0) {
            sb.append(gyeong).append("경 ");
        }
        if (jo > 0) {
            sb.append(jo).append("조 ");
        }
        if (eok > 0) {
            sb.append(eok).append("억 ");
        }
        if (man > 0) {
            sb.append(NumberFormat.getNumberInstance().format(man)).append("만 ");
        }
        if (rest > 0 || sb.length() == 0) {
            sb.append(NumberFormat.getNumberInstance().format(rest));
        } else {
            sb.setLength(sb.length() - 1); // 마지막 공백 제거
        }

        return sb.toString().trim() + "원";
    }
}
