package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.util.List;

import javax.swing.BorderFactory;
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
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;

@UiView
public class PasswordChangePanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final int PASSWORD_FIELD_COLUMNS = 20;
    /** 카드와 주의사항 사이의 간격 */
    private static final int SECTION_GAP = 15;
    /** 주의사항과 버튼 사이의 간격 */
    private static final int BUTTON_GAP = 20;

    private final UserSessionHolder userSessionHolder;

    private final ValidatedPasswordField oldPwField;
    private final ValidatedPasswordField newPwField;
    private final ValidatedPasswordField confirmPwField;
    private final FieldLabel userIdLabel;
    private final StyledCheckBox confirmCheckbox;
    private final PrimaryButton changeButton;

    public PasswordChangePanelView(UserController userController, UserSessionHolder userSessionHolder) {
        this.userSessionHolder = userSessionHolder;

        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        oldPwField = new ValidatedPasswordField(PASSWORD_FIELD_COLUMNS, ComponentSize.MEDIUM);
        newPwField = new ValidatedPasswordField(PASSWORD_FIELD_COLUMNS, ComponentSize.MEDIUM);
        confirmPwField = new ValidatedPasswordField(PASSWORD_FIELD_COLUMNS, ComponentSize.MEDIUM);
        // 사용자 ID는 바꿀 수 없으므로 입력란이 아니라 글자로 보여 준다.
        userIdLabel = new FieldLabel("", ComponentSize.MEDIUM);

        changeButton = new PrimaryButton("비밀번호 변경", ComponentSize.MEDIUM);
        confirmCheckbox = new StyledCheckBox("위 주의사항을 확인하였습니다.", ComponentSize.MEDIUM);
        confirmCheckbox.setHorizontalAlignment(SwingConstants.CENTER);
        confirmCheckbox.addItemListener(e -> {
            changeButton.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
        });
        changeButton.setEnabled(false);

        // 탭 이름이 있으므로 제목은 두지 않는다. 입력란은 프로그램 설정과 같은 카드에 한 줄씩 놓는다.
        SettingsCardPanel card = new SettingsCardPanel();
        card.addRow("사용자 ID", userIdLabel);
        card.addRow("현재 비밀번호", oldPwField);
        card.addRow("새 비밀번호", newPwField,
                String.format("%d자 이상 %d자 이하로 입력해 주세요. 여러 단어를 이은 문장형 비밀번호를 권장합니다.",
                        PasswordPolicy.MIN_LENGTH, PasswordPolicy.MAX_LENGTH),
                "같은 문자나 문자열의 반복(aaaa, gogogo), 연속된 문자(abcd, 1234), 키보드 배열(qwer, asdf)을 주로 쓴 "
                        + "비밀번호는 쓸 수 없습니다.");
        card.addRow("새 비밀번호 확인", confirmPwField);

        WarningNoticePanel noticePanel = new WarningNoticePanel("""
                비밀번호를 분실할 경우 데이터에 접근할 수 없으며, 비밀번호를 찾을 수 있는 방법이 없습니다.
                비밀번호 관리에 각별히 유의하시기 바랍니다.""");
        noticePanel.add(confirmCheckbox, BorderLayout.SOUTH);

        // 다른 다이얼로그와 같이 주 버튼은 가운데에 둔다.
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonRow.setOpaque(false);
        buttonRow.add(changeButton);

        // 카드, 주의사항, 버튼을 위에서부터 같은 폭으로 쌓고 남는 높이는 비워 둔다.
        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        content.add(card, gbc);
        gbc.gridy = 1;
        gbc.insets = new Insets(SECTION_GAP, 0, 0, 0);
        content.add(noticePanel, gbc);
        gbc.gridy = 2;
        gbc.insets = new Insets(BUTTON_GAP, 0, 0, 0);
        content.add(buttonRow, gbc);

        add(content, BorderLayout.NORTH);

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
        userIdLabel.setText(userSessionHolder.getLoggedInUser().id());

        oldPwField.setText("");
        newPwField.setText("");
        confirmPwField.setText("");
        confirmCheckbox.setSelected(false);
        changeButton.setEnabled(false);
    }

}
