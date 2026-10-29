package org.church.steward.ui.view.budget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.main.ClosedFiscalYearNoticeLabel;
import org.church.steward.ui.view.main.FiscalYearSelector;

/**
 * 예산 관리 화면. 사이드바에서 선택된 회계 연도의 수입 예산(왼쪽)과 지출 예산(오른쪽)을 진행 현황과 함께 보여주고 편집한다. 마감된
 * 회계 연도의 예산은 조회만 할 수 있다.
 */
@UiView
public class BudgetManagementView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final FiscalYearSelector fiscalYearSelector;

    private final BudgetTreePanel incomePanel;
    private final BudgetTreePanel expensePanel;
    /** 마감된 회계 연도여서 편집할 수 없음을 알리는 안내. 마감되지 않은 회계 연도에서는 숨긴다. */
    private final ClosedFiscalYearNoticeLabel closedNoticeLabel = new ClosedFiscalYearNoticeLabel();

    public BudgetManagementView(BudgetController budgetController, FiscalYearSelector fiscalYearSelector) {
        this.fiscalYearSelector = fiscalYearSelector;

        // MainView의 컨텐츠 영역 기본 크기(최소 크기도 동일) : 720 x 600
        setPreferredSize(new Dimension(720, 600));
        setLayout(new BorderLayout(0, 10));
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(closedNoticeLabel, BorderLayout.NORTH);

        // 진행 현황 칸에 폭을 내주기 위해 패널 사이 간격은 15px로 둔다.
        JPanel contentContainer = new JPanel(new GridLayout(1, 2, 15, 0));
        contentContainer.setOpaque(false);

        incomePanel = new BudgetTreePanel(budgetController, "수입 예산", BudgetType.INCOME);
        expensePanel = new BudgetTreePanel(budgetController, "지출 예산", BudgetType.EXPENSE);

        contentContainer.add(incomePanel);
        contentContainer.add(expensePanel);

        add(contentContainer, BorderLayout.CENTER);
    }

    /**
     * 사이드바에서 선택된 회계 연도의 예산을 불러온다. 이 화면이 보이는 동안 회계 연도가 바뀌어도 호출된다. 다른 화면에서 수입/지출을
     * 입력하면 진행 현황이, 회계 연도 관리에서 마감하면 편집 가능 여부가 달라지므로 매번 다시 불러온다.
     */
    @Override
    public void onMenuSelected() {
        FiscalYearDto fiscalYear = fiscalYearSelector.getSelectedFiscalYear();
        if (fiscalYear == null) {
            return;
        }

        boolean editable = !fiscalYear.isClosed();
        closedNoticeLabel.setVisible(!editable);
        incomePanel.setEditable(editable);
        expensePanel.setEditable(editable);

        incomePanel.loadData(fiscalYear.getId());
        expensePanel.loadData(fiscalYear.getId());
    }
}
