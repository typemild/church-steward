package org.church.steward.ui.view.budget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.AmountField;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;

/**
 * 예산 정보를 입력하거나 수정하기 위한 커스텀 다이얼로그
 */
public class BudgetEntryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 예산명의 최대 길이. 서비스의 검증과 같은 값이다. */
    private static final int NAME_MAX_LENGTH = 50;

    private final StyledTextField parentPathField;
    private final StyledTextField nameField;
    private final AmountField amountField;
    private final PrimaryButton btnOK;
    private final SecondaryButton btnCancel;

    private boolean confirmed = false;

    /**
     * @param owner 이 다이얼로그를 띄운 창. 예산 관리 화면의 메인 창일 수도 있고, 수입/지출 가져오기의 예산 매핑
     *              다이얼로그처럼 다른 다이얼로그일 수도 있다.
     */
    public BudgetEntryDialog(Window owner, String title, String currentName, long currentAmount, String parentPath,
            boolean amountEditable) {
        super(owner, title, ModalityType.APPLICATION_MODAL);

        setLayout(new BorderLayout());

        // 메인 컨텐트 패널 (GridBagLayout 사용)
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25));
        mainPanel.setBackground(UIColors.BACKGROUND);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 상위 예산 경로 표시 (편집 불가)
        FieldLabel lblParent = new FieldLabel("상위 예산", ComponentSize.MEDIUM, true);
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        mainPanel.add(lblParent, gbc);

        parentPathField = new StyledTextField(ComponentSize.MEDIUM);
        parentPathField.setText(parentPath);
        parentPathField.setEnabled(false);
        parentPathField.setEditable(false);
        gbc.insets = new Insets(10, 0, 10, 10);
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        mainPanel.add(parentPathField, gbc);

        // 이름 레이블 및 필드
        FieldLabel lblName = new FieldLabel("예산명", ComponentSize.MEDIUM, true);
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        mainPanel.add(lblName, gbc);

        nameField = new StyledTextField(ComponentSize.MEDIUM);
        nameField.setText(currentName);
        nameField.setColumns(20);
        gbc.insets = new Insets(10, 0, 10, 10);
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(nameField, gbc);

        // 금액 레이블 및 필드
        FieldLabel lblAmount = new FieldLabel("금액", ComponentSize.MEDIUM, true);
        gbc.insets = new Insets(10, 0, 0, 10);
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        mainPanel.add(lblAmount, gbc);

        amountField = new AmountField(20, ComponentSize.MEDIUM);
        amountField.setHorizontalAlignment(JTextField.RIGHT);
        amountField.setAmount(currentAmount);
        amountField.setEnabled(amountEditable);
        gbc.insets = new Insets(10, 0, 10, 10);
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        mainPanel.add(amountField, gbc);

        add(mainPanel, BorderLayout.CENTER);

        // 버튼 패널 (Background: #F8F9FA)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);

        btnCancel = new SecondaryButton("취소", ComponentSize.MEDIUM);
        btnOK = new PrimaryButton("저장", ComponentSize.MEDIUM);

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnOK);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);

        btnOK.addActionListener(_ -> {
            String name = nameField.getText().trim();
            long amount = amountField.getAmount();

            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "예산명을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            if (name.length() > NAME_MAX_LENGTH) {
                JOptionPane.showMessageDialog(this, "예산명은 " + NAME_MAX_LENGTH + "자 이내로 입력해주세요.", "입력 오류",
                        JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            if (name.contains("<") || name.contains(">")) {
                JOptionPane.showMessageDialog(this, "예산명에는 '<', '>' 문자를 사용할 수 없습니다.", "입력 오류",
                        JOptionPane.ERROR_MESSAGE);
                nameField.requestFocus();
                return;
            }

            if (amount < 0) {
                JOptionPane.showMessageDialog(this, "올바른 금액을 입력해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                amountField.requestFocus();
                return;
            }

            confirmed = true;
            dispose();
        });

        // 상위 예산은 수정할 수 없으므로 입력란으로 등록하지 않는다. 금액을 수정할 수 없으면 예산명이 마지막 입력란이 된다.
        DialogKeyHandler.install(this)
                .setButtons(btnCancel, btnOK)
                .addField(nameField)
                .addField(amountField)
                .trackChanges(() -> List.of(getName(), getAmount()));
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getName() {
        return nameField.getText().trim();
    }

    public long getAmount() {
        return amountField.getAmount();
    }
}
