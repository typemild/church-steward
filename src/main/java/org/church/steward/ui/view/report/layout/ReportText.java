package org.church.steward.ui.view.report.layout;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.util.ArrayList;
import java.util.List;

/**
 * 보고서의 글자 크기를 재고 줄을 나누는 도구. 화면이나 프린터의 해상도와 무관하게 항상 같은 기준으로 재므로, 한 번 정한 배치가 어디에
 * 그려도 유지된다.
 */
public final class ReportText {

    private static final FontRenderContext CONTEXT = new FontRenderContext(null, true, true);
    private static final String ELLIPSIS = "…";

    private ReportText() {
        // 인스턴스화 방지
    }

    public static double width(String text, Font font) {
        return font.getStringBounds(text, CONTEXT)
                .getWidth();
    }

    /**
     * 기준선에서 글자 위쪽 끝까지의 높이
     */
    public static double ascent(Font font) {
        return metrics(font).getAscent();
    }

    /**
     * 줄간격 1배일 때 한 줄의 높이
     */
    public static double lineHeight(Font font) {
        LineMetrics metrics = metrics(font);
        return metrics.getAscent() + metrics.getDescent();
    }

    private static LineMetrics metrics(Font font) {
        return font.getLineMetrics("가", CONTEXT);
    }

    /**
     * 글자가 폭을 넘지 않도록 여러 줄로 나눈다. 가능하면 띄어쓰기에서 나누고, 한 단어가 폭보다 길면 글자 단위로 나눈다.
     *
     * @return 한 줄 이상. 글자가 없으면 빈 줄 하나
     */
    public static List<String> wrap(String text, Font font, double maxWidth) {
        List<String> lines = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            lines.add("");
            return lines;
        }

        for (String paragraph : text.split("\n", -1)) {
            String rest = paragraph;
            boolean broken = false;
            while (rest.length() > 1 && width(rest, font) > maxWidth) {
                int count = countFitting(rest, font, maxWidth);
                int space = rest.lastIndexOf(' ', count);
                int breakAt = space > 0 ? space : count;

                lines.add(rest.substring(0, breakAt)
                        .stripTrailing());
                rest = rest.substring(breakAt)
                        .stripLeading();
                broken = true;
            }
            // 줄을 나눈 뒤 공백만 남았으면 빈 줄을 만들지 않는다.
            if (!broken || !rest.isEmpty()) {
                lines.add(rest);
            }
        }
        return lines;
    }

    /**
     * 글자가 폭을 넘으면 넘는 부분을 잘라내고 말줄임표를 붙인다.
     */
    public static String clip(String text, Font font, double maxWidth) {
        if (width(text, font) <= maxWidth) {
            return text;
        }

        int length = text.length();
        while (length > 0 && width(text.substring(0, length) + ELLIPSIS, font) > maxWidth) {
            length--;
        }
        return text.substring(0, length) + ELLIPSIS;
    }

    /**
     * 앞에서부터 폭 안에 들어가는 글자 수. 한 글자도 들어가지 않더라도 줄이 비지 않도록 1을 반환한다.
     */
    private static int countFitting(String text, Font font, double maxWidth) {
        int count = 1;
        while (count < text.length() && width(text.substring(0, count + 1), font) <= maxWidth) {
            count++;
        }
        return count;
    }
}
