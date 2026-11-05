package org.church.steward.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIShapes;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 경고 아이콘과 안내 문구를 주황색 테두리의 둥근 상자 안에 보여주는 주의사항 패널.
 * <p>
 * {@link BorderLayout}의 WEST에 아이콘, CENTER에 문구가 놓이며, 확인 체크박스처럼 문구 아래에 둘 것은
 * SOUTH에 추가한다.
 */
public class WarningNoticePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ICON_SIZE = 18;
    /** 모서리의 둥근 정도. 설정 탭의 카드, 수입/지출 총합 카드와 같다. */
    private static final int ARC = UIShapes.CARD_ARC;
    /** 안쪽 여백. 테두리 선의 두께(1px)를 포함한다. */
    private static final int PADDING_VERTICAL = 11;
    private static final int PADDING_HORIZONTAL = 16;

    public WarningNoticePanel(String text) {
        super(new BorderLayout(10, 10));

        // 둥근 배경과 테두리는 paintComponent에서 직접 그린다. 불투명하면 모서리 밖이 네모나게 칠해진다.
        setOpaque(false);
        // 배경색을 따로 정하지 않은 자식(확인 체크박스 등)이 불투명하게 칠해도 상자와 같은 색이 되도록 배경색은 지정해 둔다.
        setBackground(UIColors.WARNING_BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, PADDING_HORIZONTAL, PADDING_VERTICAL,
                PADDING_HORIZONTAL));

        JLabel icon = new JLabel(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, ICON_SIZE));
        icon.setForeground(UIColors.ERROR_WARNING);

        add(icon, BorderLayout.WEST);
        add(new DescriptionLabel(text), BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIColors.WARNING_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.setColor(UIColors.ERROR_WARNING);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }

}
