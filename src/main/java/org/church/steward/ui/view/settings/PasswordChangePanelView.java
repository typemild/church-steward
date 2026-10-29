package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.user.controller.UserController;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.PasswordPolicy.Violation;
import org.church.steward.infrastructure.security.UserSessionHolder;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;

@UiView
public class PasswordChangePanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final UserSessionHolder userSessionHolder;

    private final ValidatedPasswordField oldPwField;
    private final ValidatedPasswordField newPwField;
    private final ValidatedPasswordField confirmPwField;
    private final StyledTextField userIdField;
    private final StyledCheckBox confirmCheckbox;
    private final PrimaryButton changeButton;

    public PasswordChangePanelView(UserController userController, UserSessionHolder userSessionHolder) {
        this.userSessionHolder = userSessionHolder;

        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header
        TitleLabel titleLabel = new TitleLabel("비밀번호 변경");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        oldPwField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        newPwField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        confirmPwField = new ValidatedPasswordField(20, ComponentSize.MEDIUM);
        userIdField = new StyledTextField(20, ComponentSize.MEDIUM);
        userIdField.setEditable(false);
        userIdField.setEnabled(false);

        changeButton = new PrimaryButton("비밀번호 변경", ComponentSize.MEDIUM);
        confirmCheckbox = new StyledCheckBox("위 주의사항을 확인하였습니다.", ComponentSize.MEDIUM);
        confirmCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        confirmCheckbox.addItemListener(e -> {
            changeButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        changeButton.setEnabled(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // User ID Row
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(30, 0, 15, 0);
        formPanel.add(new FieldLabel("사용자 ID", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(30, 10, 15, 0);
        formPanel.add(userIdField, gbc);

        // Current Password Row
        gbc.gridy = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        formPanel.add(new FieldLabel("현재 비밀번호", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 15, 0);
        formPanel.add(oldPwField, gbc);

        // New Password Row
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        formPanel.add(new FieldLabel("새 비밀번호", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 15, 0);
        formPanel.add(newPwField, gbc);

        // Confirm Password Row
        gbc.gridy = 3;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 15, 0);
        formPanel.add(new FieldLabel("새 비밀번호 확인", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.insets = new Insets(0, 10, 15, 0);
        formPanel.add(confirmPwField, gbc);

        // Warning (Notice)
        gbc.gridy = 4;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(15, 0, 0, 0);
        WarningNoticePanel noticePanel = new WarningNoticePanel("""
                비밀번호를 분실할 경우 데이터에 접근할 수 없으며, 비밀번호를 찾을 수 있는 방법이 없습니다.
                비밀번호 관리에 각별히 유의하시기 바랍니다.""");
        noticePanel.add(confirmCheckbox, BorderLayout.SOUTH);
        formPanel.add(noticePanel, gbc);

        gbc.gridy = 5;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 0, 0, 0);
        formPanel.add(changeButton, gbc);

        formPanel.setPreferredSize(new Dimension(400, 0));
        formPanel.setMaximumSize(new Dimension(400, 0));

        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        centerWrapper.add(formPanel, gbc);

        add(centerWrapper, BorderLayout.CENTER);

        changeButton.addActionListener(_ -> {
            String userId = userSessionHolder.getLoggedInUser().id();
            String oldPw = PasswordPolicy.normalize(new String(oldPwField.getPassword()));
            String newPw = PasswordPolicy.normalize(new String(newPwField.getPassword()));
            String confPw = PasswordPolicy.normalize(new String(confirmPwField.getPassword()));

            if (oldPw.isEmpty() || newPw.isEmpty()) {
                JOptionPane.showMessageDialog(this, "모든 필드를 입력해주세요.", "입력 오류", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!newPw.equals(confPw)) {
                JOptionPane.showMessageDialog(this, "새 비밀번호가 일치하지 않습니다.", "비밀번호 불일치", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newPw.equals(oldPw)) {
                JOptionPane.showMessageDialog(this, "새 비밀번호가 현재 비밀번호와 같습니다.", "입력 오류",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            List<Violation> result = PasswordPolicy.validate(newPw, List.of());
            if (!result.isEmpty()) {
                JOptionPane.showMessageDialog(this, result.getFirst().getMessage());
                return;
            }

            AsyncTask.run(this,
                    () -> {
                        userController.updateUserPassword(userId, oldPw, newPw);
                        return null;
                    },
                    _ -> {
                        JOptionPane.showMessageDialog(this, "비밀번호가 성공적으로 변경되었습니다.", "성공",
                                JOptionPane.INFORMATION_MESSAGE);
                        oldPwField.setText("");
                        newPwField.setText("");
                        confirmPwField.setText("");
                        confirmCheckbox.setSelected(false);
                        changeButton.setEnabled(false);
                    },
                    ex -> JOptionPane.showMessageDialog(this, "비밀번호 변경에 실패했습니다: " + ex.getMessage(), "오류 발생",
                            JOptionPane.ERROR_MESSAGE));
        });
    }

    @Override
    public void onMenuSelected() {
        userIdField.setText(userSessionHolder.getLoggedInUser().id());

        oldPwField.setText("");
        newPwField.setText("");
        confirmPwField.setText("");
        confirmCheckbox.setSelected(false);
        changeButton.setEnabled(false);
    }

}
