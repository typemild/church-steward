package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.util.function.BiConsumer;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import org.church.steward.infrastructure.config.AppSettingManager;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
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
 * 프로그램 시작 시 나타나는 로그인 화면이다.
 */
@Slf4j
public class LoginView extends JFrame {

    private static final long serialVersionUID = 1L;

    private static final String DEFAULT_LAST_USED_ID = "관리자";

    private final AppSettingManager appSettingManager;

    private TitleLabel titleLabel;
    private StyledTextField idField;
    private ValidatedPasswordField passwordField;
    private JProgressBar progressBar;
    private PrimaryButton loginButton;

    public LoginView(AppSettingManager appSettingManager) {
        this.appSettingManager = appSettingManager;

        loadLastUsedId();

        // 1. 컴포넌트 초기화
        titleLabel = new TitleLabel("청지기: 교회 재정 관리", SwingConstants.CENTER);

        idField = new StyledTextField(ComponentSize.LARGE);
        idField.setText(loadLastUsedId());
        idField.addActionListener(_ -> passwordField.requestFocusInWindow());

        passwordField = new ValidatedPasswordField(ComponentSize.LARGE);
        passwordField.addActionListener(_ -> loginButton.doClick());
        // 입력 화면이므로 포커스 테두리를 보인다(DialogKeyHandler를 쓰지 않아 직접 켠다).
        FocusIndicator.show(idField);
        FocusIndicator.show(passwordField);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setForeground(UIColors.PRIMARY_BACKGROUND);
        progressBar.setVisible(false);

        loginButton = new PrimaryButton("로그인", ComponentSize.LARGE);
        loginButton.setPreferredSize(new Dimension(0, 40));

        // 2. 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setBackground(UIColors.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 40, 30, 40));

        // 상단 타이틀 영역
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.CENTER);

        SubTitleLabel subTitle = new SubTitleLabel("계정 정보를 입력하여 로그인하세요", SwingConstants.CENTER);
        subTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        headerPanel.add(subTitle, BorderLayout.SOUTH);

        // 입력 폼 패널
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc;

        // 사용자 ID 레이블 및 필드
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        FieldLabel idLabel = new FieldLabel("사용자 ID", ComponentSize.MEDIUM, true);
        formPanel.add(idLabel, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 25, 0); // 필드 간 간격 확대
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        formPanel.add(idField, gbc);

        // 비밀번호 레이블 및 필드
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 1.0;
        FieldLabel pwLabel = new FieldLabel("비밀번호", ComponentSize.MEDIUM, true);
        formPanel.add(pwLabel, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 1.0;
        formPanel.add(passwordField, gbc);

        // 하단 버튼 및 프로그레스 영역
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(loginButton, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 3. JFrame에 패널 추가
        this.setLayout(new BorderLayout());
        this.add(mainPanel);
    }

    private void saveLastUsedId(String userId) {
        try {
            appSettingManager.set("last-login-id", userId);
        } catch (Exception e) {
            log.error("Failed to save last used ID", e);
        }
    }

    private String loadLastUsedId() {
        try {
            String lastUsedId = appSettingManager.get("last-login-id");
            return lastUsedId != null ? lastUsedId : DEFAULT_LAST_USED_ID;
        } catch (Exception e) {
            return DEFAULT_LAST_USED_ID;
        }
    }

    /**
     * 로그인 화면을 보여준다.
     *
     * @param doLogin 로그인 시 수행할 작업 로직. 첫 번째 파라미터는 사용자 ID이고, 두 번재 파라미터는 사용자 PW다.
     * @return
     */
    public void showLoginDialog(BiConsumer<String, String> doLogin) {
        this.setTitle("청지기 - 로그인");
        AppIcons.apply(this);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.setSize(450, 500); // 너비를 400에서 450으로 확대
        this.setLocationRelativeTo(null);
        this.setResizable(false);

        // 기존 리스너 제거 (중복 등록 방지)
        for (ActionListener al : loginButton.getActionListeners()) {
            loginButton.removeActionListener(al);
        }

        loginButton.addActionListener(_ -> {
            String userId = idField.getText();
            String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));

            // 비동기 작업 시작
            loginButton.setEnabled(false);
            progressBar.setVisible(true);

            AsyncTask.run(this,
                    () -> {
                        doLogin.accept(userId, password);
                        saveLastUsedId(userId);
                        return null;
                    },
                    _ -> {
                        this.dispose();
                    },
                    err -> {
                        // 없는 ID나 틀린 비밀번호는 흔한 입력 실수이므로 스택 트레이스 없이 남긴다.
                        if (err instanceof UserNotFoundException || err instanceof InvalidPasswordException) {
                            log.warn("Login failed for user {}: {}", userId, err.getMessage());
                        } else {
                            log.error("Failed to log in", err);
                        }
                        JOptionPane.showMessageDialog(LoginView.this, "로그인 실패: " + err.getMessage(), "Error",
                                JOptionPane.ERROR_MESSAGE);

                        if (err instanceof UserNotFoundException) {
                            idField.requestFocus();
                        } else if (err instanceof InvalidPasswordException) {
                            passwordField.requestFocus();
                        }

                        loginButton.setEnabled(true);
                        progressBar.setVisible(false);
                    });
        });

        this.setVisible(true);

        passwordField.requestFocus();
    }

}
