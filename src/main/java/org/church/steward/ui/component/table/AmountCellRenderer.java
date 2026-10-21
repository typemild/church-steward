package org.church.steward.ui.component.table;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 금액(Long)을 천 단위 콤마와 우측 정렬로 표시하는 테이블 셀 렌더러
 */
public class AmountCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Long) {
            setText(formatter.format(value));
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return this;
    }
}
