package org.church.steward.ui.view.datamanagement;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;

/**
 * 내보낼 파일의 형식과 암호를 고르는 다이얼로그. 암호는 엑셀 형식에만 걸 수 있고 비워 두면 걸지 않는다. 빈 양식처럼 암호가 필요 없는
 * 경우에는 형식만 고르게 한다.
 */
public class ExportOptionsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String PASSWORD_HINT = "암호를 비워 두면 암호 없이 저장합니다.";
    private static final String NO_PASSWORD_HINT = "CSV 형식에는 암호를 걸 수 없습니다.";

    /**
     * 고른 내보내기 옵션.
     *
     * @param format   파일 형식
     * @param password 파일에 걸 암호. 걸지 않으면 null
     */
    public record ExportOptions(SpreadsheetFormat format, String password) {
    }

    private final boolean passwordAllowed;
    private final StyledComboBox<SpreadsheetFormat> formatCombo;
    private final ValidatedPasswordField passwordField;
    private final ValidatedPasswordField confirmPasswordField;
    private final SubFieldLabel hintLabel;

    private ExportOptions result;

    /**
     * @param passwordAllowed 암호를 입력받을지 여부. false이면 형식만 고르게 한다.
     */
    public ExportOptionsDialog(Window owner, String title, boolean passwordAllowed) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.passwordAllowed = passwordAllowed;

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        mainPanel.setBackground(UIColors.BACKGROUND);

        formatCombo = new StyledComboBox<>(SpreadsheetFormat.values(), ComponentSize.MEDIUM);
        formatCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SpreadsheetFormat format) {
                    setText(format.getLabel() + " (." + format.getExtension() + ")");
                }
                return this;
            }
        });
        passwordField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        confirmPasswordField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        hintLabel = new SubFieldLabel(PASSWORD_HINT, ComponentSize.MEDIUM);

        int row = 0;
        addField(mainPanel, row++, "형식", formatCombo);
        if (passwordAllowed) {
            addField(mainPanel, row++, "암호", passwordField);
            addField(mainPanel, row++, "암호 확인", confirmPasswordField);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(0, 0, 10, 0);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0;
            gbc.gridy = row;
            gbc.gridwidth = 2;
            mainPanel.add(hintLabel, gbc);
        }
        add(mainPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton okButton = new PrimaryButton("다음", ComponentSize.MEDIUM);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        formatCombo.addActionListener(_ -> updatePasswordFields());
        okButton.addActionListener(_ -> confirm());
        updatePasswordFields();

        // 암호를 받지 않거나 CSV 형식이면 암호 입력란이 없거나 꺼져 있으므로, 형식에서 Enter를 누르면 바로 다음으로 넘어간다.
        DialogKeyHandler.install(this)
                .setButtons(cancelButton, okButton)
                .addField(formatCombo)
                .addField(passwordField)
                .addField(confirmPasswordField);

        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }

    private void addField(JPanel panel, int row, String label, Component field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 15);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new FieldLabel(label, ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    /**
     * 다이얼로그를 띄운다.
     *
     * @return 고른 옵션. 취소했으면 비어 있음
     */
    public Optional<ExportOptions> showDialog() {
        result = null;
        setVisible(true);
        return Optional.ofNullable(result);
    }

    private SpreadsheetFormat getSelectedFormat() {
        return (SpreadsheetFormat) formatCombo.getSelectedItem();
    }

    /**
     * 암호를 걸 수 없는 형식을 고르면 암호 입력란을 비우고 잠근다.
     */
    private void updatePasswordFields() {
        boolean passwordSupported = getSelectedFormat().isPasswordSupported();
        passwordField.setEnabled(passwordSupported);
        confirmPasswordField.setEnabled(passwordSupported);
        if (!passwordSupported) {
            passwordField.setText("");
            confirmPasswordField.setText("");
        }
        hintLabel.setText(passwordSupported ? PASSWORD_HINT : NO_PASSWORD_HINT);
    }

    private void confirm() {
        SpreadsheetFormat format = getSelectedFormat();
        String password = null;
        if (passwordAllowed && format.isPasswordSupported()) {
            String entered = new String(passwordField.getPassword());
            String confirmed = new String(confirmPasswordField.getPassword());
            if (!entered.equals(confirmed)) {
                JOptionPane.showMessageDialog(this, "암호와 암호 확인이 일치하지 않습니다.", "입력 오류", JOptionPane.ERROR_MESSAGE);
                confirmPasswordField.requestFocus();
                return;
            }
            password = entered.isEmpty() ? null : entered;
        }

        result = new ExportOptions(format, password);
        dispose();
    }

}
