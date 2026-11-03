package org.church.steward.ui.view.login;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.infrastructure.backup.BackupException;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.backup.BackupManifest;
import org.church.steward.infrastructure.config.AppHome;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.view.login.DatabaseRestoreView.RestoreAction;
import org.church.steward.ui.view.login.DatabaseRestoreView.RestoreOutcome;

import lombok.extern.slf4j.Slf4j;

/**
 * 백업 파일과 관리자 비밀번호를 입력받아 복원하는 입력부. 예약된 복원을 수행하는 {@link DatabaseRestoreView}와 최초 실행 화면의
 * "백업에서 복원"({@link InitialSetupView})이 함께 쓴다. 버튼과 창의 닫기 처리는 담는 쪽이 맡는다.
 */
@Slf4j
class RestoreFormPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NO_FILE_INFO = "백업 파일을 선택해주세요.";
    private static final String ADMIN_ID = "관리자";
    private static final String MESSAGE_TITLE = "백업에서 복원";

    private final BackupManager backupManager;
    private final RestoreAction restoreAction;

    private final StyledTextField fileField = new StyledTextField(ComponentSize.MEDIUM);
    private final SecondaryButton browseButton = new SecondaryButton("찾아보기", ComponentSize.MEDIUM);
    private final SubFieldLabel fileInfoLabel = new SubFieldLabel(NO_FILE_INFO, ComponentSize.MEDIUM);
    private final ValidatedPasswordField passwordField = new ValidatedPasswordField(ComponentSize.MEDIUM);

    /** 복원할 수 있다고 확인된 백업 파일. 아직 고르지 않았거나 복원할 수 없는 파일이면 null */
    private Path backupFile;

    /**
     * @param backupManager 고른 백업 파일의 정보를 읽는 데 쓴다
     * @param restoreAction 복원과 그 기록을 수행하는 동작
     */
    RestoreFormPanel(BackupManager backupManager, RestoreAction restoreAction) {
        super(new GridBagLayout());
        this.backupManager = backupManager;
        this.restoreAction = restoreAction;
        setOpaque(false);

        fileField.setEditable(false);
        // 백업은 관리자만 할 수 있으므로 백업 파일에는 관리자 계정이 항상 있다. 그래서 관리자 계정으로만 복원한다.
        StyledTextField userIdField = new StyledTextField(ComponentSize.MEDIUM);
        userIdField.setText(ADMIN_ID);
        userIdField.setEditable(false);
        userIdField.setEnabled(false);
        // 입력 화면이므로 포커스 테두리를 보인다(DialogKeyHandler를 쓰지 않아 직접 켠다).
        FocusIndicator.show(fileField);
        FocusIndicator.show(passwordField);

        // 백업 파일
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        add(new FieldLabel("백업 파일", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        add(fileField, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.gridx = 1;
        gbc.gridy = 1;
        add(browseButton, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        add(fileInfoLabel, gbc);

        // 사용자 ID
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        add(new FieldLabel("사용자 ID", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        add(userIdField, gbc);

        // 비밀번호
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 6, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        add(new FieldLabel("비밀번호", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        add(passwordField, gbc);

        browseButton.addActionListener(_ -> chooseBackupFile());
    }

    /**
     * 복원의 위험을 알리는 주의사항. 담는 쪽이 입력부 아래에 놓는다.
     */
    static WarningNoticePanel createWarningNotice() {
        // 복원은 백업 파일의 덤프를 그대로 실행하므로, 조작된 백업은 이 PC에서 코드를 실행할 수 있다.
        // 남이 준 실행 파일을 실행하는 것과 같은 위험으로 보고 받아들였으므로, 막는 대신 신뢰할 수 있는 파일만 쓰도록 알린다.
        return new WarningNoticePanel("""
                직접 만들었거나 믿을 수 있는 사람에게 받은 백업 파일만 복원하세요. \
                조작된 백업 파일을 복원하면 이 PC가 악성 프로그램에 감염될 수 있습니다.""");
    }

    /**
     * Enter로 복원하도록 담는 쪽이 키를 연결할 때 쓴다.
     */
    ValidatedPasswordField getPasswordField() {
        return passwordField;
    }

    /**
     * 처음에 포커스를 줄 입력란. 파일을 고르지 않았으면 찾아보기 버튼, 골랐으면 비밀번호다.
     */
    void focusFirstField() {
        if (backupFile == null) {
            browseButton.requestFocusInWindow();
        } else {
            passwordField.requestFocusInWindow();
        }
    }

    /**
     * 백업 파일의 정보를 읽어 보여준다. 복원할 수 없는 파일이면 사유를 보여주고 선택하지 않은 상태로 둔다.
     */
    void selectBackupFile(Path file) {
        fileField.setText(file.toString());
        fileField.setCaretPosition(0);
        try {
            BackupManifest manifest = backupManager.inspect(file);
            backupFile = file;
            fileInfoLabel.setText("백업 일시: " + manifest.createdAt().format(CREATED_AT_FORMAT) + "  /  프로그램 버전: "
                    + manifest.programVersion());
        } catch (BackupException e) {
            backupFile = null;
            fileInfoLabel.setText(NO_FILE_INFO);
            JOptionPane.showMessageDialog(this, e.getMessage(), MESSAGE_TITLE, JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * 복원한다. 복원하는 동안 찾아보기 버튼을 끄고, 담는 쪽에도 알려 버튼과 창의 닫기를 막게 한다. 성공하면 완료 문구를 보인 뒤
     * onRestored를 부른다. 실패하면 사유를 보이고 입력한 내용을 그대로 둔다.
     *
     * @param onBusyChanged 복원을 시작하면 true, 실패해서 끝나면 false로 불린다
     * @param onRestored    복원에 성공해 완료 문구를 닫은 뒤 불린다
     */
    void restore(Consumer<Boolean> onBusyChanged, Runnable onRestored) {
        if (backupFile == null) {
            JOptionPane.showMessageDialog(this, NO_FILE_INFO, MESSAGE_TITLE, JOptionPane.ERROR_MESSAGE);
            return;
        }
        String userId = ADMIN_ID;
        String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));
        Path file = backupFile;

        setBusy(true, onBusyChanged);
        AsyncTask.run(this,
                () -> restoreAction.restore(file, userId, password),
                outcome -> {
                    JOptionPane.showMessageDialog(this, createSuccessMessage(outcome));
                    onRestored.run();
                },
                err -> {
                    // 틀린 비밀번호는 흔한 입력 실수이므로 스택 트레이스 없이 남긴다.
                    if (err instanceof InvalidPasswordException) {
                        log.warn("Restore failed for user {}: {}", userId, err.getMessage());
                    } else {
                        log.error("Failed to restore from backup", err);
                    }
                    JOptionPane.showMessageDialog(this, "복원 실패: " + err.getMessage(), MESSAGE_TITLE,
                            JOptionPane.ERROR_MESSAGE);

                    setBusy(false, onBusyChanged);
                    if (err instanceof InvalidPasswordException) {
                        passwordField.requestFocus();
                    }
                });
    }

    private void chooseBackupFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("백업 파일 선택");
        chooser.setFileFilter(new FileNameExtensionFilter("청지기 백업 파일 (*." + BackupManager.FILE_EXTENSION + ")",
                BackupManager.FILE_EXTENSION));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        selectBackupFile(chooser.getSelectedFile().toPath());
        if (backupFile != null) {
            passwordField.requestFocusInWindow();
        }
    }

    private String createSuccessMessage(RestoreOutcome outcome) {
        // 최초 실행 화면에서 복원한 경우처럼 기존 데이터가 없었으면 보관한 폴더도 없다.
        String message = "복원이 완료되었습니다.";
        if (outcome.preservedDir().isPresent()) {
            message += "\n기존 데이터는 아래 폴더에 보관했습니다.\n" + outcome.preservedDir().get().toAbsolutePath();
        }
        if (!outcome.logged()) {
            message += "\n\n복원한 내역을 활동 기록에 남기지 못했습니다. 자세한 내용은 아래 파일을 확인해주세요.\n"
                    + AppHome.logFile();
        }
        return message;
    }

    private void setBusy(boolean busy, Consumer<Boolean> onBusyChanged) {
        browseButton.setEnabled(!busy);
        onBusyChanged.accept(busy);
    }
}
