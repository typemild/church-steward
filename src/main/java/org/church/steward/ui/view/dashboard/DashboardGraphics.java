package org.church.steward.ui.view.dashboard;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.text.DecimalFormat;
import java.util.Map;

/**
 * 대시보드에서 직접 그리는 컴포넌트들이 공통으로 쓰는 그리기 도구
 */
final class DashboardGraphics {

    private static final String ELLIPSIS = "…";

    private DashboardGraphics() {
        // 인스턴스화 방지
    }

    /**
     * 도형과 글자에 안티앨리어싱을 적용한 그래픽 컨텍스트를 만든다. 다 쓴 뒤에는 dispose 해야 한다.
     */
    static Graphics2D create(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 직접 그리는 글자가 다른 Swing 컴포넌트의 글자와 같은 품질로 보이도록 데스크톱 설정을 따른다.
        Object desktopHints = Toolkit.getDefaultToolkit()
                .getDesktopProperty("awt.font.desktophints");
        if (desktopHints instanceof Map<?, ?> hints) {
            g2.addRenderingHints(hints);
        } else {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        }
        return g2;
    }

    /**
     * 글자가 폭을 넘으면 넘는 부분을 잘라내고 말줄임표를 붙인다.
     */
    static String clip(String text, FontMetrics metrics, int maxWidth) {
        if (metrics.stringWidth(text) <= maxWidth) {
            return text;
        }

        int length = text.length();
        while (length > 0 && metrics.stringWidth(text.substring(0, length) + ELLIPSIS) > maxWidth) {
            length--;
        }
        return text.substring(0, length) + ELLIPSIS;
    }

    /**
     * 금액을 "2,500", "250만", "1.5억"처럼 짧게 표기한다. 소수 둘째 자리 아래는 반올림하므로 정확한 값이 필요한 곳에는 쓰지
     * 않는다.
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
     * HTML 툴팁에 넣을 글자의 특수 문자를 바꾼다.
     */
    static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
