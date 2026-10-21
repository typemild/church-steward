package org.church.steward.ui.view.settings.fiscalyear;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.util.NumberUtils;

/**
 * 회계 연도 관리 화면의 목록. 총 수입, 총 지출, 차액은 마감할 때 저장한 금액이며 마감하지 않은 회계 연도는 비워 둔다.
 */
class FiscalYearTableModel extends AbstractTableModel implements TooltipTableModel {
    private static final long serialVersionUID = 1L;

    static final int COLUMN_FISCAL_YEAR = 0;
    static final int COLUMN_STATUS = 1;
    static final int COLUMN_TOTAL_INCOME = 2;
    static final int COLUMN_TOTAL_EXPENSE = 3;
    static final int COLUMN_BALANCE = 4;

    private static final String[] COLUMN_NAMES = { "회계 연도", "상태", "총 수입", "총 지출", "차액" };

    private List<FiscalYearDto> fiscalYears = new ArrayList<>();

    void setData(List<FiscalYearDto> fiscalYears) {
        this.fiscalYears = fiscalYears;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return fiscalYears.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMN_NAMES[col];
    }

    @Override
    public Object getValueAt(int row, int col) {
        FiscalYearDto fiscalYear = fiscalYears.get(row);
        return switch (col) {
        // 회계 연도 칸은 이름과 기간을 함께 그려야 하므로 회계 연도를 그대로 넘긴다.
        case COLUMN_FISCAL_YEAR -> fiscalYear;
        case COLUMN_STATUS -> getStatus(fiscalYear);
        case COLUMN_TOTAL_INCOME -> fiscalYear.getTotalIncome();
        case COLUMN_TOTAL_EXPENSE -> fiscalYear.getTotalExpense();
        case COLUMN_BALANCE -> fiscalYear.getBalance();
        default -> null;
        };
    }

    /**
     * 셀의 툴팁. 회계 연도는 이름과 기간을 한 줄로, 금액은 한글 금액으로 보여준다. 금액이 없는(마감하지 않은) 셀은 툴팁이 없다.
     */
    @Override
    public String getTooltipText(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return null;
        }

        FiscalYearDto fiscalYear = fiscalYears.get(row);
        return switch (col) {
        case COLUMN_FISCAL_YEAR -> String.format("%s (%s ~ %s)", fiscalYear.getName(), fiscalYear.getStartDate(),
                fiscalYear.getEndDate());
        case COLUMN_STATUS -> getStatus(fiscalYear);
        case COLUMN_TOTAL_INCOME -> amountTooltip(fiscalYear.getTotalIncome());
        case COLUMN_TOTAL_EXPENSE -> amountTooltip(fiscalYear.getTotalExpense());
        case COLUMN_BALANCE -> amountTooltip(fiscalYear.getBalance());
        default -> null;
        };
    }

    private String getStatus(FiscalYearDto fiscalYear) {
        return fiscalYear.isClosed() ? "마감" : "진행";
    }

    /**
     * 금액 칸의 툴팁에 보여줄 한글 금액. 마감하지 않아 금액이 없으면 툴팁도 없도록 null을 반환한다.
     */
    private String amountTooltip(Long amount) {
        return amount == null ? null : NumberUtils.toKoreanAmount(amount);
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
        case COLUMN_FISCAL_YEAR -> FiscalYearDto.class;
        case COLUMN_STATUS -> String.class;
        default -> Long.class;
        };
    }

    FiscalYearDto getFiscalYear(int row) {
        return fiscalYears.get(row);
    }

    List<FiscalYearDto> getFiscalYears() {
        return fiscalYears;
    }
}
