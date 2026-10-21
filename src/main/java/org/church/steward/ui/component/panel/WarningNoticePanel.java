package org.church.steward.ui.component.panel;

import java.awt.BorderLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 경고 아이콘과 안내 문구를 주황색 테두리 안에 보여주는 주의사항 패널.
 * <p>
 * {@link BorderLayout}의 WEST에 아이콘, CENTER에 문구가 놓이며, 확인 체크박스처럼 문구 아래에 둘 것은
 * SOUTH에 추가한다.
 */
public class WarningNoticePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ICON_SIZE = 18;

    public WarningNoticePanel(String text) {
        super(new BorderLayout(10, 10));

        setBackground(UIColors.WARNING_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(UIColors.ERROR_WARNING, 1, true),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        JLabel icon = new JLabel(FontIcon.of(FontAwesomeSolid.EXCLAMATION_TRIANGLE, ICON_SIZE));
        icon.setForeground(UIColors.ERROR_WARNING);

        add(icon, BorderLayout.WEST);
        add(new DescriptionLabel(text), BorderLayout.CENTER);
    }

}
