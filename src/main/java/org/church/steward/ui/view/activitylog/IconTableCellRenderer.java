package org.church.steward.ui.view.activitylog;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * FontIcon 객체를 JTable 셀 중앙에 렌더링하기 위한 커스텀 렌더러다.
 */
public class IconTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;
    private final Ikon icon;
    private final int size;
    private int hoveredRow = -1;

    public IconTableCellRenderer(Ikon icon, int size) {
        super();
        this.icon = icon;
        this.size = size;
        setHorizontalAlignment(CENTER);
    }

    /**
     * 마우스가 올라가 있는 행(뷰 기준)을 지정한다. 없으면 -1이다.
     */
    public void setHoveredRow(int hoveredRow) {
        this.hoveredRow = hoveredRow;
    }

    public int getHoveredRow() {
        return hoveredRow;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Color currentColor;
        if (isSelected) {
            currentColor = table.getSelectionForeground();
        } else if (row == hoveredRow) {
            currentColor = UIColors.TEXT_HIGHLIGHT;
        } else {
            currentColor = table.getForeground();
        }
        setIcon(FontIcon.of(icon, size, currentColor));

        return this;
    }
}
