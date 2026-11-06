package org.church.steward.ui.component.table;

import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

/**
 * 칸의 값들을 한 덩어리로 보고 칸의 가운데에 놓는 왼쪽 정렬 셀 렌더러. 덩어리의 폭은 값들 중 가장 넓은 글자의 폭이므로, 가장 넓은
 * 값은 가운데 정렬한 머리글과 같은 가운데에 오고 나머지 값은 그 왼쪽 끝에 맞춰진다. 칸이 넓어도 글이 칸의 왼쪽 끝에 붙어 머리글과
 * 멀어지지 않게 하면서, 줄마다 시작 위치는 같게 둔다. 칸이 가장 넓은 값보다 좁으면 그냥 왼쪽 정렬이다. 보고서의
 * {@code ReportTable#setBlockCentered}와 같은 방식이다.
 * <p>
 * 덩어리의 폭은 보이는 행이 아니라 넘겨받은 값 전체로 잰다. 접힌 행을 펼칠 때마다 덩어리가 움직이지 않게 하기 위함이다.
 */
public class BlockCenteredCellRenderer extends PaddedCellRenderer {

    private static final long serialVersionUID = 1L;

    /** 덩어리의 폭을 잴 값 전체 */
    private final transient Supplier<List<String>> values;
    /** 덩어리의 폭을 잰 값 목록과 글꼴. 둘 중 하나가 바뀌면 다시 잰다. */
    private transient List<String> measuredValues;
    private Font measuredFont;
    private int blockWidth;
    /** {@code DefaultTableCellRenderer}가 정한 테두리. 덩어리를 옮길 왼쪽 여백을 바꿀 때 다시 감싼다. */
    private transient Border baseBorder;
    /** 덩어리를 칸 가운데로 옮기는 왼쪽 여백 */
    private int blockInset;

    /**
     * @param values 덩어리의 폭을 잴 값 전체. 값이 바뀌면 새 목록을 돌려주어야 다시 잰다.
     */
    public BlockCenteredCellRenderer(Supplier<List<String>> values) {
        super(SwingConstants.LEADING);
        this.values = values;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        blockInset = 0;
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        Insets insets = getInsets();
        int textWidth = table.getColumnModel()
                .getColumn(column)
                .getWidth() - insets.left - insets.right;
        blockInset = Math.max(0, (textWidth - measureBlockWidth()) / 2);
        if (blockInset > 0) {
            setBorder(baseBorder);
        }
        return this;
    }

    @Override
    public void setBorder(Border border) {
        baseBorder = border;
        super.setBorder(blockInset > 0
                ? BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(0, blockInset, 0, 0))
                : border);
    }

    private int measureBlockWidth() {
        List<String> current = values.get();
        Font font = getFont();
        if (current != measuredValues || !font.equals(measuredFont)) {
            FontMetrics metrics = getFontMetrics(font);
            int width = 0;
            for (String text : current) {
                if (text != null) {
                    width = Math.max(width, metrics.stringWidth(text));
                }
            }
            measuredValues = current;
            measuredFont = font;
            blockWidth = width;
        }
        return blockWidth;
    }
}
