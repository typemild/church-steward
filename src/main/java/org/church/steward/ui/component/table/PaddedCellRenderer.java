package org.church.steward.ui.component.table;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * 셀 안쪽에 여백을 두고 지정한 정렬로 텍스트를 표시하는 테이블 셀 렌더러
 */
public class PaddedCellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    /**
     * @param horizontalAlignment {@link javax.swing.SwingConstants}의 수평 정렬 값
     */
    public PaddedCellRenderer(int horizontalAlignment) {
        setHorizontalAlignment(horizontalAlignment);
    }

    @Override
    public void setBorder(Border border) {
        super.setBorder(BorderFactory.createCompoundBorder(
                border,
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}
