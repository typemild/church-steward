package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.budget.BudgetSelectBox;

/**
 * 선택한 여러 수입/지출 내역의 예산을 한 번에 바꾸기 위해 예산을 고르는 다이얼로그. 내역과 같은 구분, 같은 회계 연도의 말단 예산만
 * 고를 수 있다.
 */
class BulkBudgetChangeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final BudgetSelectBox budgetSelectBox;
    private final BudgetType budgetType;

    private boolean confirmed = false;

    /**
     * @param fiscalYear 내역들이 속한 회계 연도. 이 회계 연도의 예산에서 고른다.
     * @param type       내역들의 구분
     * @param count      바꿀 내역의 건수
     */
    BulkBudgetChangeDialog(Window owner, BudgetController budgetController, FiscalYearDto fiscalYear,
            FinancialRecordType type, int count) {
        super(owner, "예산 일괄 변경", ModalityType.APPLICATION_MODAL);
        this.budgetType = type == FinancialRecordType.INCOME ? BudgetType.INCOME : BudgetType.EXPENSE;
        this.budgetSelectBox = new BudgetSelectBox(budgetController, fiscalYear::getId, budgetType, true);

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIColors.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        mainPanel.add(new DescriptionLabel("선택한 " + type.getLabel() + " " + count + "건의 예산을 아래 예산으로 변경합니다."),
                BorderLayout.NORTH);

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 8));
        fieldPanel.setOpaque(false);
        fieldPanel.add(new FieldLabel("예산", ComponentSize.MEDIUM, true), BorderLayout.NORTH);
        fieldPanel.add(budgetSelectBox, BorderLayout.CENTER);
        mainPanel.add(fieldPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton okButton = new PrimaryButton("변경", ComponentSize.MEDIUM);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        okButton.addActionListener(_ -> confirm());

        DialogKeyHandler.install(this)
                .setButtons(cancelButton, okButton)
                .addField(budgetSelectBox, this::openBudgetDialogIfEmpty)
                .trackChanges(budgetSelectBox::getBudgetId);

        // 설명 문구(JTextArea)는 폭이 정해진 뒤에야 줄바꿈된 높이를 알 수 있으므로, 폭을 정해 배치한 뒤 높이를 다시 맞춘다.
        // (ContactMappingDialog와 같다)
        setResizable(false);
        pack();
        setSize(new Dimension(Math.max(getWidth(), 460), getHeight()));
        validate();
        setSize(new Dimension(getWidth(), getPreferredSize().height));
        setLocationRelativeTo(owner);
    }

    /**
     * 예산을 미리 선택해 둔다. 선택한 내역들의 예산이 모두 같을 때 쓴다. 띄우기 전에 호출해야 변경 여부 판단의 기준이 된다.
     *
     * @param budgetPath 전체 예산명("예산A > 예산B > 예산C")
     */
    void setBudget(Long budgetId, String budgetPath) {
        budgetSelectBox.setBudget(budgetId, budgetType, budgetPath);
    }

    /**
     * 예산 입력란의 Enter. 예산이 선택되어 있으면 변경을 확인하고, 없으면 예산 선택 다이얼로그를 연다.
     *
     * @return 다이얼로그를 열었으면 true
     */
    private boolean openBudgetDialogIfEmpty() {
        if (budgetSelectBox.getBudgetId() != null) {
            return false;
        }
        budgetSelectBox.openDialog();
        return true;
    }

    private void confirm() {
        if (budgetSelectBox.getBudgetId() == null) {
            JOptionPane.showMessageDialog(this, "변경할 예산을 선택해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            budgetSelectBox.getInputComponent()
                    .requestFocusInWindow();
            return;
        }
        confirmed = true;
        dispose();
    }

    boolean isConfirmed() {
        return confirmed;
    }

    Long getBudgetId() {
        return budgetSelectBox.getBudgetId();
    }
}
