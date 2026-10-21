package org.church.steward.ui.view.report.composer;

import java.text.DecimalFormat;

/**
 * 보고서에 적는 숫자의 표기 형식
 */
final class ReportFormats {

    static final String NONE = "-";

    private ReportFormats() {
        // 인스턴스화 방지
    }

    /**
     * 금액을 "1,234,000"처럼 세 자리마다 쉼표를 넣어 적는다.
     */
    static String amount(long amount) {
        return String.format("%,d", amount);
    }

    /**
     * 증감액을 "+1,234,000", "-50,000"처럼 부호를 붙여 적는다. 0은 부호 없이 적는다.
     */
    static String signedAmount(long amount) {
        return amount > 0 ? "+" + amount(amount) : amount(amount);
    }

    /**
     * 금액을 "2,500", "250만", "1.5억"처럼 짧게 적는다. 소수 둘째 자리 아래는 반올림하므로 차트의 눈금처럼 어림값이면 되는
     * 곳에만 쓴다.
     */
    static String compactAmount(long amount) {
        DecimalFormat format = new DecimalFormat("#,##0.##");
        if (amount >= 1_0000_0000L) {
            return format.format(amount / 1_0000_0000.0) + "억";
        }
        if (amount >= 1_0000L) {
            return format.format(amount / 1_0000.0) + "만";
        }
        return format.format(amount);
    }

    /**
     * 비율을 "12.3%"처럼 소수 첫째 자리까지 적는다. 비율을 정할 수 없으면(null) "-"로 적는다.
     */
    static String rate(Double rate) {
        return rate == null ? NONE : String.format("%.1f%%", rate);
    }
}
