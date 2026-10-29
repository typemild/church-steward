package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextAttribute;
import java.util.Map;

import javax.swing.JList;
import javax.swing.JScrollPane;

/**
 * 두 관계자 자동 완성 목록이 함께 쓰는 개수 제한, 안내 문구, 목록 크기 계산.
 */
public final class ContactSuggestions {

    /** 처음에 보여주는 최대 관계자 수. 목록이 길어 아래쪽 입력란을 가리지 않도록 적게 둔다. */
    public static final int LIMIT = 5;

    /** 안내 항목을 눌러 모두 보여줄 때의 조회 개수(사실상 제한 없음) */
    public static final int UNLIMITED = Integer.MAX_VALUE;

    /** 스크롤 없이 보여주는 최대 항목 수. 모두 보여줄 때 이보다 많으면 스크롤한다. */
    private static final int MAX_VISIBLE_ROWS = 8;

    private ContactSuggestions() {
    }

    /**
     * 목록에 다 담지 못한 관계자가 있을 때 안내 항목의 문구를 반환한다. 이 항목을 누르면 모두 보여준다.
     */
    public static String moreResultsText(long remainingCount) {
        return String.format("검색 결과가 %,d건 더 있습니다...", remainingCount);
    }

    /**
     * 안내 항목에 마우스를 올렸을 때 쓸, 밑줄 그은 글꼴을 만든다. 링크처럼 누를 수 있다는 것을 알리기 위함이다.
     */
    public static Font underlined(Font font) {
        return font.deriveFont(Map.of(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    }

    /**
     * 목록과 스크롤 패인의 크기를 팝업 폭에 맞춘다. 항목이 {@value #MAX_VISIBLE_ROWS}개를 넘으면 그만큼의 높이로 줄이고
     * 스크롤하게 하며, 스크롤 위치는 맨 위로 되돌린다. 항목의 폭은 팝업 폭(스크롤바가 생기면 그만큼 뺀 폭)에 고정한다. 고정하지 않으면
     * 비고가 긴 항목이 있을 때 목록이 팝업보다 넓어져서, 가로 스크롤이 없는 팝업에서 오른쪽이 잘린다(오른쪽 정렬한 안내 문구가 보이지 않고
     * 긴 비고에 말줄임표가 붙지 않는다). 항목마다 높이가 다르므로 높이는 visibleRowCount가 아니라 실제 항목 위치로 계산한다.
     */
    public static void fitToWidth(JList<?> list, JScrollPane scrollPane, int width) {
        list.setFixedCellWidth(width);
        int fullHeight = list.getPreferredSize().height;
        int height = fullHeight;
        int size = list.getModel()
                .getSize();
        if (size > MAX_VISIBLE_ROWS) {
            Rectangle lastVisible = list.getCellBounds(MAX_VISIBLE_ROWS - 1, MAX_VISIBLE_ROWS - 1);
            height = lastVisible.y + lastVisible.height;
        }
        if (height < fullHeight) {
            list.setFixedCellWidth(width - scrollPane.getVerticalScrollBar()
                    .getPreferredSize().width);
        }
        scrollPane.setPreferredSize(new Dimension(width, height));
        scrollPane.getViewport()
                .setViewPosition(new Point(0, 0));
    }
}
