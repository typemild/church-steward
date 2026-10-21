package org.church.steward.ui.view.financialrecord.budget;

import java.awt.GridLayout;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.view.financialrecord.FinancialRecordActionHandler;

/**
 * "예산별 보기" 탭. 좌측에 수입 예산 목록, 우측에 지출 예산 목록을 보여준다.
 */
public class BudgetTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final BudgetRecordPanel incomePanel;
    private final BudgetRecordPanel expensePanel;

    public BudgetTabPanel(FinancialRecordActionHandler actionHandler) {
        this.incomePanel = new BudgetRecordPanel(FinancialRecordType.INCOME, actionHandler);
        this.expensePanel = new BudgetRecordPanel(FinancialRecordType.EXPENSE, actionHandler);

        setLayout(new GridLayout(1, 2, 25, 0));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(false);

        add(incomePanel);
        add(expensePanel);
    }

    /**
     * 내역을 추가, 수정, 삭제할 수 있는지 정한다. 마감된 회계 연도의 내역은 조회만 할 수 있으므로 false로 둔다.
     */
    public void setEditable(boolean editable) {
        incomePanel.setEditable(editable);
        expensePanel.setEditable(editable);
    }

    /**
     * 검색된 수입/지출 내역을 구분에 따라 좌우 목록에 나누어 표시한다.
     *
     * @param budgetPositions 예산 ID별 표시 위치
     */
    public void setRecords(List<FinancialRecordResponseDto> records, Map<Long, Integer> budgetPositions) {
        incomePanel.setRecords(records.stream()
                .filter(record -> record.getType() == FinancialRecordType.INCOME)
                .toList(), budgetPositions);
        expensePanel.setRecords(records.stream()
                .filter(record -> record.getType() == FinancialRecordType.EXPENSE)
                .toList(), budgetPositions);
    }
}
