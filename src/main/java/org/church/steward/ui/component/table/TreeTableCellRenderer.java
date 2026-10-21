package org.church.steward.ui.component.table;

import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * {@link TreeTable}의 트리 칸 렌더러. 깊이만큼 들여 쓴 자리에 접기/펼치기 화살표(말단 행은 지정한 아이콘)를 그리고 그
 * 오른쪽에 글자를 적는다.
 * 아이콘이 글자 위치를 밀지 않도록 아이콘은 테두리 여백 안에 직접 그린다.
 */
class TreeTableCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final int ICON_SIZE = Math.round(UIFonts.FONT_SIZE_BASE);
    private static final int RIGHT_PADDING = 4;

    private Ikon leafIcon;
    private boolean expandableRowsBold;

    /** 지금 그리는 셀의 아이콘과 그 자리. 아이콘이 없으면 null이다. */
    private FontIcon currentIcon;
    private int currentToggleStart;

    void setLeafIcon(Ikon leafIcon) {
        this.leafIcon = leafIcon;
    }

    void setExpandableRowsBold(boolean expandableRowsBold) {
        this.expandableRowsBold = expandableRowsBold;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        TreeTable treeTable = (TreeTable) table;
        AbstractTreeTableModel<?> model = treeTable.getTreeModel();
        int modelRow = table.convertRowIndexToModel(row);
        boolean expandable = model.isExpandable(modelRow);

        currentToggleStart = TreeTable.toggleStart(model.getDepth(modelRow));
        currentIcon = createIcon(model, modelRow, row == treeTable.getHoveredToggleRow());
        int textStart = currentIcon != null ? currentToggleStart + TreeTable.TOGGLE_WIDTH : currentToggleStart;

        setIcon(null);
        setBorder(BorderFactory.createEmptyBorder(0, textStart, 0, RIGHT_PADDING));
        setFont(expandable && expandableRowsBold ? table.getFont()
                .deriveFont(Font.BOLD) : table.getFont());
        return this;
    }

    private FontIcon createIcon(AbstractTreeTableModel<?> model, int modelRow, boolean hovered) {
        if (!model.isExpandable(modelRow)) {
            return leafIcon != null ? FontIcon.of(leafIcon, ICON_SIZE, UIColors.TEXT_MAIN) : null;
        }
        FontAwesomeSolid arrow = model.isExpanded(modelRow) ? FontAwesomeSolid.CHEVRON_DOWN
                : FontAwesomeSolid.CHEVRON_RIGHT;
        return FontIcon.of(arrow, ICON_SIZE, hovered ? UIColors.TEXT_HIGHLIGHT : UIColors.TEXT_MAIN);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (currentIcon != null) {
            int iconX = currentToggleStart + (TreeTable.TOGGLE_WIDTH - currentIcon.getIconWidth()) / 2;
            int iconY = (getHeight() - currentIcon.getIconHeight()) / 2;
            currentIcon.paintIcon(this, g, iconX, iconY);
        }
    }

}
