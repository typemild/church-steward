package org.church.steward.ui.view.datamanagement.recordimport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.budget.BudgetTreePanel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 수입/지출 가져오기에서 파일의 예산 값을 매핑할 예산을 고르는 다이얼로그.
 * 고르기만 하는 것이 아니라 그 자리에서 예산을 추가, 수정, 삭제하거나 순서를 바꿀 수 있다. 파일에만 있는 예산을
 * 등록하러 예산 관리 메뉴까지 다녀오지 않아도 되게 하기 위함이다.
 * <p>
 * 여기서 한 예산의 변경은 가져오기와 무관하게 즉시 저장되며, 다이얼로그를 취소해도 되돌아가지 않는다.
 * 그래서 이 다이얼로그를 닫은 뒤에는 선택한 예산이 없더라도 다시 분석해야 한다.
 */
class BudgetMappingDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String NO_SELECTION_TEXT = "(선택 없음)";

    private final BudgetTreePanel budgetPanel;
    private final FieldLabel pathLabel = new FieldLabel(NO_SELECTION_TEXT, FontAwesomeSolid.CHART_PIE,
            SwingConstants.LEFT, ComponentSize.MEDIUM);

    private Long selectedBudgetId;

    /**
     * @param type              선택할 수 있는 예산의 구분. 수입 내역은 수입 예산에, 지출 내역은 지출 예산에만 등록할 수
     *                          있다.
     * @param sourceDescription 매핑하려는 파일의 값에 대한 설명 (예: "파일의 값: 건축헌금 (12건)")
     */
    BudgetMappingDialog(Window owner, BudgetController budgetController, Long fiscalYearId, BudgetType type,
            String sourceDescription) {
        super(owner, "예산 매핑", ModalityType.APPLICATION_MODAL);

        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(UIColors.BACKGROUND);
        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 0, 25));
        topPanel.add(new DescriptionLabel(sourceDescription + "\n매핑할 말단 예산을 선택해주세요. 필요한 예산이 없으면 여기서 바로 추가할 수 있습니다."),
                BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        budgetPanel = new BudgetTreePanel(budgetController, type.getLabel() + " 예산", type);
        budgetPanel.setSelectionListener(_ -> updatePathLabel());
        budgetPanel.loadData(fiscalYearId);

        JPanel listPanel = new JPanel(new BorderLayout());
        listPanel.setBackground(UIColors.BACKGROUND);
        listPanel.setBorder(BorderFactory.createEmptyBorder(10, 25, 10, 25));
        listPanel.add(budgetPanel, BorderLayout.CENTER);
        add(listPanel, BorderLayout.CENTER);

        JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.setBackground(UIColors.BACKGROUND);
        pathPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
        pathPanel.add(pathLabel, BorderLayout.CENTER);

        SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton okButton = new PrimaryButton("이 예산에 매핑", ComponentSize.MEDIUM);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(UIColors.BACKGROUND);
        bottomPanel.add(pathPanel, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(bottomPanel, BorderLayout.SOUTH);

        setSize(new Dimension(520, 600));
        setLocationRelativeTo(owner);

        okButton.addActionListener(_ -> confirm());

        // 입력란 없이 예산 표만 있으므로, 표에서든 다른 곳에서든 Enter를 누르면 선택한 예산으로 매핑한다.
        DialogKeyHandler.install(this)
                .setButtons(cancelButton, okButton)
                .pressPrimaryOnEnter(budgetPanel.getTable());

        // 열자마자 방향키로 예산을 고르고 Enter로 매핑할 수 있게 표에 포커스를 둔다.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                budgetPanel.getTable()
                        .requestFocusInWindow();
            }
        });
    }

    /**
     * 다이얼로그를 띄운다.
     *
     * @return 선택한 말단 예산의 ID. 취소했으면 null
     */
    Long showDialog() {
        selectedBudgetId = null;
        setVisible(true);
        return selectedBudgetId;
    }

    private void updatePathLabel() {
        String path = budgetPanel.getSelectedBudgetPath();
        pathLabel.setText(path == null ? NO_SELECTION_TEXT : path);
        pathLabel.setToolTipText(path);
    }

    private void confirm() {
        if (budgetPanel.getSelectedBudget() == null) {
            JOptionPane.showMessageDialog(this, "예산을 선택해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!budgetPanel.isSelectedBudgetLeaf()) {
            JOptionPane.showMessageDialog(this, "하위 예산이 없는 예산만 선택할 수 있습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            return;
        }
        selectedBudgetId = budgetPanel.getSelectedBudget().getId();
        dispose();
    }

}
