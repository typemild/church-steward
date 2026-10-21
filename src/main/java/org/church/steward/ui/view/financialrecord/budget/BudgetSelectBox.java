package org.church.steward.ui.view.financialrecord.budget;

import java.awt.BorderLayout;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.field.CompositeInputField;
import org.church.steward.ui.component.field.StyledTextField;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 예산 선택 박스.
 * 직접 수정할 수 없는 텍스트 필드와 예산 선택 다이얼로그를 여는 버튼으로 구성된다.
 */
public class BudgetSelectBox extends JPanel implements CompositeInputField {

    private static final long serialVersionUID = 1L;

    private final BudgetController budgetController;
    private final Supplier<Long> fiscalYearIdSupplier;
    private final boolean leafOnly;

    private final StyledTextField pathField = new StyledTextField(ComponentSize.MEDIUM);
    private final IconButton selectButton = new IconButton(FontAwesomeSolid.SEARCH, ComponentSize.MEDIUM);

    private BudgetType allowedType;
    private Long budgetId;
    private BudgetType budgetType;

    /**
     * @param fiscalYearIdSupplier 예산 목록을 조회할 회계 연도의 ID 공급자. null을 반환하면 다이얼로그를 열지
     *                             않는다.
     * @param allowedType          선택할 수 있는 예산 유형. null이면 수입/지출 예산 모두 선택할 수 있다.
     * @param leafOnly             true이면 하위 예산이 없는 말단 예산만 선택할 수 있다.
     */
    public BudgetSelectBox(BudgetController budgetController, Supplier<Long> fiscalYearIdSupplier,
            BudgetType allowedType, boolean leafOnly) {
        this.budgetController = budgetController;
        this.fiscalYearIdSupplier = fiscalYearIdSupplier;
        this.allowedType = allowedType;
        this.leafOnly = leafOnly;

        setLayout(new BorderLayout(8, 0));
        setOpaque(false);

        pathField.setEditable(false);
        selectButton.setToolTipText("예산 선택");
        selectButton.addActionListener(_ -> openDialog());

        add(pathField, BorderLayout.CENTER);
        add(selectButton, BorderLayout.EAST);
    }

    /**
     * 선택된 예산을 보여주는 입력란을 반환한다. 직접 수정할 수는 없지만 포커스는 받는다.
     */
    @Override
    public JComponent getInputComponent() {
        return pathField;
    }

    /**
     * 예산 선택 다이얼로그를 열고, 확인하면 고른 예산으로 바꾼다.
     */
    public void openDialog() {
        Long fiscalYearId = fiscalYearIdSupplier.get();
        if (fiscalYearId == null) {
            JOptionPane.showMessageDialog(this, "회계 연도를 먼저 선택해주세요.");
            return;
        }

        BudgetSelectDialog dialog = new BudgetSelectDialog(SwingUtilities.getWindowAncestor(this), budgetController,
                fiscalYearId, allowedType, leafOnly);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }
        setBudget(dialog.getSelectedBudgetId(), dialog.getSelectedBudgetType(), dialog.getSelectedBudgetPath());
    }

    /**
     * 선택된 예산을 설정한다. budgetId가 null이면 선택을 해제한다.
     */
    public void setBudget(Long budgetId, BudgetType budgetType, String budgetPath) {
        this.budgetId = budgetId;
        this.budgetType = budgetId != null ? budgetType : null;

        String text = budgetId != null ? budgetPath : "";
        pathField.setText(text);
        pathField.setCaretPosition(0);
        pathField.setToolTipText(budgetId != null ? text : null);
    }

    public void clear() {
        setBudget(null, null, null);
    }

    /**
     * 선택할 수 있는 예산 유형을 바꾼다. 이미 선택된 예산의 유형과 맞지 않으면 선택을 해제한다.
     */
    public void setAllowedType(BudgetType allowedType) {
        this.allowedType = allowedType;
        if (allowedType != null && budgetType != null && budgetType != allowedType) {
            clear();
        }
    }

    public Long getBudgetId() {
        return budgetId;
    }

    /**
     * 선택된 예산의 전체 예산명을 반환한다. 선택된 예산이 없으면 null을 반환한다.
     */
    public String getBudgetPath() {
        return budgetId != null ? pathField.getText() : null;
    }

    public BudgetType getBudgetType() {
        return budgetType;
    }
}
