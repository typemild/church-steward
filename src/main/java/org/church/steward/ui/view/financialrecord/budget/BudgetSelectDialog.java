package org.church.steward.ui.view.financialrecord.budget;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.budget.BudgetTreePanel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 수입/지출 예산 선택 다이얼로그.
 * 좌측에 수입 예산, 우측에 지출 예산 목록을 보여주며 둘 중 한 곳에서만 행을 선택할 수 있다.
 */
public class BudgetSelectDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String NO_SELECTION_TEXT = "(선택 없음)";

    private final boolean leafOnly;
    private final BudgetTreePanel incomePanel;
    private final BudgetTreePanel expensePanel;
    private final FieldLabel pathLabel = new FieldLabel(NO_SELECTION_TEXT, FontAwesomeSolid.CHART_PIE,
            SwingConstants.LEFT, ComponentSize.MEDIUM);

    private boolean confirmed = false;
    private Long selectedBudgetId;
    private BudgetType selectedBudgetType;
    private String selectedBudgetPath;

    /**
     * @param allowedType 선택할 수 있는 예산 유형. null이면 수입/지출 예산을 모두 보여준다.
     * @param leafOnly    true이면 하위 예산이 없는 말단 예산을 반드시 선택해야 한다.
     *                    false이면 상위 예산도 선택할 수 있고, 선택 없이 확인하면 예산 조건을 해제한다.
     */
    public BudgetSelectDialog(Window owner, BudgetController budgetController, Long fiscalYearId,
            BudgetType allowedType, boolean leafOnly) {
        super(owner, "예산 선택", ModalityType.APPLICATION_MODAL);
        this.leafOnly = leafOnly;

        setLayout(new BorderLayout());

        boolean showIncome = allowedType == null || allowedType == BudgetType.INCOME;
        boolean showExpense = allowedType == null || allowedType == BudgetType.EXPENSE;

        // 예산 목록
        JPanel listPanel = new JPanel(new GridLayout(1, 0, 25, 0));
        listPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        listPanel.setBackground(UIColors.BACKGROUND);

        incomePanel = new BudgetTreePanel(budgetController, "수입 예산", BudgetType.INCOME, true);
        expensePanel = new BudgetTreePanel(budgetController, "지출 예산", BudgetType.EXPENSE, true);
        incomePanel.setSelectionListener(dto -> onSelectionChanged(dto, expensePanel));
        expensePanel.setSelectionListener(dto -> onSelectionChanged(dto, incomePanel));

        if (showIncome) {
            listPanel.add(incomePanel);
            incomePanel.loadData(fiscalYearId);
        }
        if (showExpense) {
            listPanel.add(expensePanel);
            expensePanel.loadData(fiscalYearId);
        }
        add(listPanel, BorderLayout.CENTER);

        // 하단 : 선택된 예산의 전체 예산명 + 버튼
        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
        pathPanel.setBackground(UIColors.BACKGROUND);
        pathPanel.add(pathLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);

        SecondaryButton btnCancel = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton btnOK = new PrimaryButton("확인", ComponentSize.MEDIUM);
        buttonPanel.add(btnCancel);
        buttonPanel.add(btnOK);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIColors.BACKGROUND);
        bottomPanel.add(pathPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        int width = (showIncome && showExpense) ? 760 : 420;
        setSize(new Dimension(width, 520));
        setLocationRelativeTo(owner);

        btnOK.addActionListener(_ -> onConfirm());
        // 행을 더블클릭하면 그 예산으로 확인한다.
        incomePanel.setRowDoubleClickHandler(this::onConfirm);
        expensePanel.setRowDoubleClickHandler(this::onConfirm);

        // 입력란 없이 예산 표만 있으므로, 표에서든 다른 곳에서든 Enter를 누르면 선택한 예산으로 확인한다.
        DialogKeyHandler.install(this)
                .setButtons(btnCancel, btnOK)
                .pressPrimaryOnEnter(incomePanel.getTable(), expensePanel.getTable());
    }

    /**
     * 한쪽 목록에서 행이 선택되면 반대쪽 목록의 행 선택을 해제한다.
     */
    private void onSelectionChanged(BudgetDto selected, BudgetTreePanel otherPanel) {
        if (selected != null) {
            otherPanel.clearSelection();
        }
        BudgetTreePanel selectedPanel = getSelectedPanel();
        String path = selectedPanel != null ? selectedPanel.getSelectedBudgetPath() : NO_SELECTION_TEXT;
        pathLabel.setText(path);
        pathLabel.setToolTipText(selectedPanel != null ? path : null);
    }

    private BudgetTreePanel getSelectedPanel() {
        if (incomePanel.getSelectedBudget() != null) {
            return incomePanel;
        }
        if (expensePanel.getSelectedBudget() != null) {
            return expensePanel;
        }
        return null;
    }

    private void onConfirm() {
        BudgetTreePanel selectedPanel = getSelectedPanel();

        if (leafOnly) {
            if (selectedPanel == null) {
                JOptionPane.showMessageDialog(this, "예산을 선택해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (!selectedPanel.isSelectedBudgetLeaf()) {
                JOptionPane.showMessageDialog(this, "하위 예산이 없는 예산만 선택할 수 있습니다.", "입력 오류",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        if (selectedPanel != null) {
            BudgetDto selected = selectedPanel.getSelectedBudget();
            selectedBudgetId = selected.getId();
            selectedBudgetType = selected.getType();
            selectedBudgetPath = selectedPanel.getSelectedBudgetPath();
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * 선택된 예산의 ID를 반환한다. 선택 없이 확인한 경우 null을 반환한다.
     */
    public Long getSelectedBudgetId() {
        return selectedBudgetId;
    }

    public BudgetType getSelectedBudgetType() {
        return selectedBudgetType;
    }

    /**
     * 선택된 예산의 전체 예산명("예산A > 예산B > 예산C")을 반환한다.
     */
    public String getSelectedBudgetPath() {
        return selectedBudgetPath;
    }
}
