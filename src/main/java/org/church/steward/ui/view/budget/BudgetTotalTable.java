package org.church.steward.ui.view.budget;

import java.awt.Font;
import java.awt.event.MouseEvent;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import org.church.steward.ui.component.table.AmountCellRenderer;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.util.UIColors;
import org.church.steward.util.NumberUtils;

/**
 * 예산 표 아래에 붙이는 총계 줄. 머리글 없는 한 줄짜리 표이며 예산 표의 칸 모델을 함께 써서 칸 폭과 위치가 예산 표와 늘 같다. 칸
 * 폭은 예산 표가 정하므로 이 표는 칸 폭을 바꾸지 않는다({@link #doLayout()}). 선택하거나 포커스를 받지 않고, 총계임을
 * 알아보도록 굵은 글자와 바탕색으로 그린다.
 */
class BudgetTotalTable extends JTable {
    private static final long serialVersionUID = 1L;

    private static final String TOTAL_NAME = "총계";

    private final TotalModel totalModel;
    private final TableCellRenderer nameRenderer = new PaddedCellRenderer(SwingConstants.CENTER);
    private final TableCellRenderer amountRenderer = new AmountCellRenderer();
    private final DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer();

    /**
     * @param budgetTable 칸 모델과 글꼴, 행 높이를 따를 예산 표. 진행 현황 칸이 있어야 한다.
     */
    BudgetTotalTable(JTable budgetTable) {
        super(new TotalModel(budgetTable.getModel()
                .getColumnCount()), budgetTable.getColumnModel());
        this.totalModel = (TotalModel) getModel();

        setFont(budgetTable.getFont()
                .deriveFont(Font.BOLD));
        setRowHeight(budgetTable.getRowHeight());
        setBackground(UIColors.TABLE_TOTAL_BACKGROUND);
        setShowGrid(false);
        setFocusable(false);
        setRowSelectionAllowed(false);
        setColumnSelectionAllowed(false);
        setAutoResizeMode(AUTO_RESIZE_OFF);
        // 예산 표의 비율 칸과 같이 안쪽 여백 없이 오른쪽에 붙여 적는다.
        rateRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    /**
     * 총계를 바꾼다. 비율은 두 총계로 구한다.
     */
    void setTotals(long budgetedAmount, long actualAmount) {
        totalModel.budgetedAmount = budgetedAmount;
        totalModel.actualAmount = actualAmount;
        totalModel.fireTableRowsUpdated(0, 0);
    }

    /**
     * 칸에 총계를 적는 데 필요한 폭. 예산 표가 칸 폭을 내용에 맞출 때 총계도 들어가도록 함께 잰다.
     */
    int measureWidth(int viewColumn) {
        return prepareRenderer(getCellRenderer(0, viewColumn), 0, viewColumn).getPreferredSize().width;
    }

    /**
     * 칸에는 예산 표의 렌더러(트리 칸 렌더러 등)가 달려 있으므로 이 표의 렌더러를 따로 쓴다.
     */
    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return switch (convertColumnIndexToModel(column)) {
        case BudgetTableModel.COLUMN_NAME -> nameRenderer;
        case BudgetTableModel.COLUMN_RATE -> rateRenderer;
        default -> amountRenderer;
        };
    }

    /**
     * 칸 폭은 예산 표가 정한다. 이 표가 자기 폭에 맞춰 칸 폭을 다시 나누면 함께 쓰는 칸 모델을 두고 두 표가 다투게 된다.
     */
    @Override
    public void doLayout() {
        // 칸 폭을 바꾸지 않는다.
    }

    /**
     * 머리글을 두지 않는다. 만들어 두기만 해도 함께 쓰는 칸 모델에 리스너로 남는다.
     */
    @Override
    protected JTableHeader createDefaultTableHeader() {
        return null;
    }

    /**
     * 예산 표와 같이 금액 칸에는 한국어 단위로 적은 금액을 툴팁으로 보인다.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        int viewColumn = columnAtPoint(e.getPoint());
        if (viewColumn < 0) {
            return null;
        }
        return switch (convertColumnIndexToModel(viewColumn)) {
        case BudgetTableModel.COLUMN_AMOUNT -> NumberUtils.toKoreanAmount(totalModel.budgetedAmount);
        case BudgetTableModel.COLUMN_ACTUAL_AMOUNT -> NumberUtils.toKoreanAmount(totalModel.actualAmount);
        default -> null;
        };
    }

    private static class TotalModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private final int columnCount;
        private long budgetedAmount;
        private long actualAmount;

        TotalModel(int columnCount) {
            this.columnCount = columnCount;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public int getColumnCount() {
            return columnCount;
        }

        @Override
        public Object getValueAt(int row, int col) {
            return switch (col) {
            case BudgetTableModel.COLUMN_NAME -> TOTAL_NAME;
            case BudgetTableModel.COLUMN_AMOUNT -> budgetedAmount;
            case BudgetTableModel.COLUMN_ACTUAL_AMOUNT -> actualAmount;
            case BudgetTableModel.COLUMN_RATE -> BudgetTableModel.formatRate(actualAmount, budgetedAmount);
            default -> null;
            };
        }
    }
}
