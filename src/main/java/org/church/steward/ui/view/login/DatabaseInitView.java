package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.PasswordPolicy.Violation;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubTitleLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터베이스 및 설정 파일 초기화를 위한 UI다.
 */
@Slf4j
public class DatabaseInitView extends JDialog {

    private static final long serialVersionUID = 1L;

    private TitleLabel titleLabel;
    private SubTitleLabel subTitleLabel;
    private StyledTextField userIdField;
    private ValidatedPasswordField passwordField;
    private ValidatedPasswordField confirmPasswordField;
    private JProgressBar progressBar;
    private PrimaryButton nextButton;
    private SecondaryButton restoreButton;
    private BooleanSupplier restoreAction;

    public DatabaseInitView() {
        // 0. 다이얼로그 설정
        setModal(true);
        getContentPane().setBackground(UIColors.BACKGROUND);

        JOptionPane.showMessageDialog(this, "프로그램 최초 실행을 감지했습니다.\n관리자 생성이 필요합니다.");

        // 1. 컴포넌트 초기화
        titleLabel = new TitleLabel("관리자 계정 설정", SwingConstants.CENTER);
        subTitleLabel = new SubTitleLabel("주의! 비밀번호 분실 시 복구할 수 없습니다.", SwingConstants.CENTER);

        userIdField = new StyledTextField(ComponentSize.LARGE);
        userIdField.setText("관리자");
        userIdField.setEditable(false);
        userIdField.setEnabled(false);

        passwordField = new ValidatedPasswordField(ComponentSize.LARGE);
        confirmPasswordField = new ValidatedPasswordField(ComponentSize.LARGE);
        // 입력 화면이므로 포커스 테두리를 보인다(DialogKeyHandler를 쓰지 않아 직접 켠다).
        FocusIndicator.show(passwordField);
        FocusIndicator.show(confirmPasswordField);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setForeground(UIColors.PRIMARY_BACKGROUND);

        nextButton = new PrimaryButton("다음", ComponentSize.LARGE);
        restoreButton = new SecondaryButton("백업에서 복원", ComponentSize.LARGE);
        restoreButton.setVisible(false);

        // 2. 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // Header Panel
        JPanel headerPanel = new JPanel(new GridBagLayout());
        headerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 10, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;
        headerPanel.add(titleLabel, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 1;
        headerPanel.add(subTitleLabel, gbc);

        // Input Panel
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);

        // User ID Row
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        FieldLabel label = new FieldLabel("사용자 ID", ComponentSize.LARGE);
        inputPanel.add(label, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1;
        inputPanel.add(userIdField, gbc);

        // Password Row
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        label = new FieldLabel("비밀번호", ComponentSize.LARGE);
        inputPanel.add(label, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1;
        inputPanel.add(passwordField, gbc);

        // Confirm Password Row
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        label = new FieldLabel("비밀번호 확인", ComponentSize.LARGE);
        inputPanel.add(label, gbc);
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1;
        inputPanel.add(confirmPasswordField, gbc);

        // Bottom Panel
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 0, 8));
        buttonPanel.setOpaque(false);
        buttonPanel.add(nextButton);
        buttonPanel.add(restoreButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        restoreButton.addActionListener(_ -> {
            if (restoreAction != null && restoreAction.getAsBoolean()) {
                isInitializedSuccessfully = true;
                this.dispose();
            }
        });

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);

        // 초기화하는 중에 창을 닫으면 초기화가 끝나기 전에 취소한 것으로 반환되므로, 그동안은 닫지 않는다.
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (!busy) {
                    dispose();
                }
            }
        });
    }

    private boolean isInitializedSuccessfully = false;
    /** 초기화하는 중인지 여부. 초기화하는 동안은 창을 닫을 수 없다. */
    private boolean busy = false;

    /**
     * 관리자 계정을 만드는 대신 백업에서 복원하는 동작을 지정한다. 지정하면 "백업에서 복원" 버튼이 나타난다.
     *
     * @param restoreAction 복원 화면을 띄우고, 복원했으면 true를 반환하는 동작. 복원하면 초기화가 끝난 것으로 본다.
     */
    public void setRestoreAction(BooleanSupplier restoreAction) {
        this.restoreAction = restoreAction;
        restoreButton.setVisible(restoreAction != null);
    }

    /**
     * DB 초기화 화면을 보여준다.
     *
     * @param doInit 수행할 초기화 작업 로직. 첫 번째 파라미터는 사용자 ID이고, 두 번재 파라미터는 사용자 PW다.
     * @return
     */
    public boolean showInitDialog(BiConsumer<String, String> doInit) {
        isInitializedSuccessfully = false;
        this.setTitle("데이터베이스 초기화");
        AppIcons.apply(this);
        this.setSize(450, 500);
        this.setLocationRelativeTo(null);

        // 기존 리스너 제거 (중복 등록 방지)
        for (ActionListener al : nextButton.getActionListeners()) {
            nextButton.removeActionListener(al);
        }

        nextButton.addActionListener(_ -> {
            String userId = userIdField.getText();
            String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));
            String confirmPassword = PasswordPolicy.normalize(new String(confirmPasswordField.getPassword()));

            // 1. 비밀번호 유효성 검증
            List<Violation> result = PasswordPolicy.validate(password, List.of(userId));
            if (!result.isEmpty()) {
                JOptionPane.showMessageDialog(this, result.getFirst().getMessage());
                return;
            }

            // 2. 비밀번호 일치 여부 검증
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
                return;
            }

            // 비동기 작업 시작
            busy = true;
            nextButton.setEnabled(false);
            progressBar.setVisible(true);

            AsyncTask.run(this,
                    () -> {
                        doInit.accept(userId, password);
                        return null;
                    },
                    _ -> {
                        JOptionPane.showMessageDialog(this, "초기화가 완료되었습니다.");
                        isInitializedSuccessfully = true;
                        this.dispose();
                    },
                    err -> {
                        log.error("Failed to initialize database", err);
                        JOptionPane.showMessageDialog(this, "초기화 실패: " + err.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);

                        busy = false;
                        nextButton.setEnabled(true);
                        progressBar.setVisible(false);
                    });
        });

        this.setVisible(true);
        return isInitializedSuccessfully;
    }

}
