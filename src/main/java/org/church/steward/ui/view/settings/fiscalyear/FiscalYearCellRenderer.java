package org.church.steward.ui.view.settings.fiscalyear;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 회계 연도 관리 목록에서 회계 연도를 두 줄로 그린다. 사이드바의 회계 연도 선택란과 같이 윗줄은 이름, 아랫줄은 기간이며,
 * 기간은 이름보다 눈에 덜 띄도록 작고 흐린 글자로 "2025-12-01 ~ 2026-11-30"처럼 한 줄에 적는다.
 */
class FiscalYearCellRenderer extends JPanel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

    private final JLabel nameLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel periodLabel = new JLabel("", SwingConstants.CENTER);

    FiscalYearCellRenderer() {
        setLayout(new GridLayout(2, 1, 0, 1));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        nameLabel.setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE));
        periodLabel.setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL));
        // 이름은 사용자가 입력한 글자이므로 HTML로 해석되지 않게 한다.
        nameLabel.putClientProperty("html.disable", Boolean.TRUE);

        add(nameLabel);
        add(periodLabel);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        Color nameColor = isSelected ? table.getSelectionForeground() : UIColors.TEXT_MAIN;
        Color periodColor = isSelected ? table.getSelectionForeground() : UIColors.TEXT_SUB;

        setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
        nameLabel.setForeground(nameColor);
        periodLabel.setForeground(periodColor);

        if (value instanceof FiscalYearDto fiscalYear) {
            nameLabel.setText(fiscalYear.getName());
            periodLabel.setText(fiscalYear.getStartDate() + " ~ " + fiscalYear.getEndDate());
        } else {
            nameLabel.setText("");
            periodLabel.setText("");
        }
        return this;
    }
}
