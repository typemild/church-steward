package org.church.steward.ui.view.budget;

import java.util.Objects;

import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.table.AbstractTreeTableModel;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.util.NumberUtils;

/**
 * 예산 목록의 표 모델. 진행 현황을 보여주는 경우에는 예산 옆에 수입액(지출 예산은 집행액)과 달성률(지출 예산은 집행률) 칸이 더 있다.
 * 예산은 상위/하위 관계대로 접고 펼칠 수 있으며 처음에는 모두 펼쳐 둔다. 노드의 키는 예산 ID다.
 */
class BudgetTableModel extends AbstractTreeTableModel<BudgetDto> implements TooltipTableModel {
    private static final long serialVersionUID = 1L;

    static final int COLUMN_NAME = 0;
    static final int COLUMN_AMOUNT = 1;
    static final int COLUMN_ACTUAL_AMOUNT = 2;
    static final int COLUMN_RATE = 3;

    private static final String NO_RATE = "-";

    private final String[] columns;

    /**
     * @param type         예산 유형. 진행 현황 칸의 이름을 정한다.
     * @param showProgress 진행 현황(수입액/집행액, 달성률/집행률) 칸을 보여줄지 여부
     */
    BudgetTableModel(BudgetType type, boolean showProgress) {
        super(true);
        if (!showProgress) {
            columns = new String[] { "항목명", "예산(원)" };
        } else if (type == BudgetType.INCOME) {
            columns = new String[] { "항목명", "예산(원)", "수입액(원)", "달성률" };
        } else {
            columns = new String[] { "항목명", "예산(원)", "집행액(원)", "집행률" };
        }
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        BudgetDto dto = getRowValue(row);
        return switch (col) {
        case COLUMN_NAME -> dto.getName();
        case COLUMN_AMOUNT -> getBudgetedAmount(dto);
        case COLUMN_ACTUAL_AMOUNT -> dto.getActualAmount();
        case COLUMN_RATE -> formatRate(dto);
        default -> null;
        };
    }

    @Override
    public String getTooltipText(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount() || columnIndex < 0 || columnIndex >= getColumnCount()) {
            return null;
        }

        BudgetDto dto = getRowValue(rowIndex);
        return switch (columnIndex) {
        case COLUMN_NAME -> dto.getName();
        case COLUMN_AMOUNT -> NumberUtils.toKoreanAmount(getBudgetedAmount(dto));
        case COLUMN_ACTUAL_AMOUNT -> dto.getActualAmount() == null ? null
                : NumberUtils.toKoreanAmount(dto.getActualAmount());
        default -> null;
        };
    }

    /**
     * 예산액. 하위 예산이 있으면 하위 예산의 합이다.
     */
    private long getBudgetedAmount(BudgetDto dto) {
        return Objects.requireNonNullElse(dto.getAmount(), dto.getTotalAmount());
    }

    /**
     * 예산액 대비 수입액(집행액)의 비율을 "12.3%"처럼 적는다. 예산액이 0이면 비율을 구할 수 없으므로 "-"로 적는다.
     */
    private String formatRate(BudgetDto dto) {
        long budgetedAmount = getBudgetedAmount(dto);
        if (dto.getActualAmount() == null || budgetedAmount == 0) {
            return NO_RATE;
        }
        return String.format("%.1f%%", dto.getActualAmount() * 100.0 / budgetedAmount);
    }

}
