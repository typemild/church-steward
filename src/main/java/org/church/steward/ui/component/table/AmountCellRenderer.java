package org.church.steward.ui.component.table;

import java.awt.Component;
import java.text.DecimalFormat;
import javax.swing.JTable;
import javax.swing.SwingConstants;

/**
 * 금액(Long)을 천 단위 콤마와 우측 정렬로 표시하는 테이블 셀 렌더러. 다른 칸과 같이 안쪽 여백을 두어 숫자가 칸 끝에 닿지 않게
 * 한다.
 */
public class AmountCellRenderer extends PaddedCellRenderer {
    private static final long serialVersionUID = 1L;
    private final DecimalFormat formatter = new DecimalFormat("#,###");

    public AmountCellRenderer() {
        super(SwingConstants.RIGHT);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        if (value instanceof Long) {
            setText(formatter.format(value));
        }
        return this;
    }
}
