package org.church.steward.ui.view.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 사이드바의 회계 연도 선택란에서 회계 연도를 두 줄로 그린다. 윗줄은 이름, 아랫줄은 기간이다.
 * 사이드바가 좁아 기간은 "25.12.01 ~ 26.11.30"처럼 줄여 적고, 마감된 회계 연도는 이름 앞에 자물쇠 아이콘을 붙인다.
 * 데이터 관리 화면처럼 회계 연도 선택란을 따로 두는 화면도 같은 모양으로 보이도록 이 렌더러를 쓴다.
 */
public class FiscalYearComboRenderer extends JPanel implements ListCellRenderer<FiscalYearDto> {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yy.MM.dd");

    private final JLabel nameLabel = new JLabel();
    private final JLabel periodLabel = new JLabel();
    private final FontIcon closedIcon = FontIcon.of(FontAwesomeSolid.LOCK, UIFonts.FONT_SIZE_SMALL);

    public FiscalYearComboRenderer() {
        setLayout(new GridLayout(2, 1, 0, 1));
        setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));

        nameLabel.setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE));
        periodLabel.setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL));
        // 이름은 사용자가 입력한 글자이므로 HTML로 해석되지 않게 한다.
        nameLabel.putClientProperty("html.disable", Boolean.TRUE);

        add(nameLabel);
        add(periodLabel);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends FiscalYearDto> list, FiscalYearDto value, int index,
            boolean isSelected, boolean cellHasFocus) {
        Color background = isSelected ? list.getSelectionBackground() : UIColors.COMBO_BOX_BACKGROUND;
        Color nameColor = isSelected ? list.getSelectionForeground() : UIColors.TEXT_MAIN;
        Color periodColor = isSelected ? list.getSelectionForeground() : UIColors.TEXT_SUB;

        setBackground(background);
        nameLabel.setForeground(nameColor);
        periodLabel.setForeground(periodColor);

        if (value == null) {
            nameLabel.setText(" ");
            nameLabel.setIcon(null);
            periodLabel.setText(" ");
            return this;
        }

        nameLabel.setText(value.getName());
        if (value.isClosed()) {
            closedIcon.setIconColor(nameColor);
            nameLabel.setIcon(closedIcon);
        } else {
            nameLabel.setIcon(null);
        }
        periodLabel.setText(DATE_FORMATTER.format(value.getStartDate()) + " ~ "
                + DATE_FORMATTER.format(value.getEndDate()));
        return this;
    }
}
