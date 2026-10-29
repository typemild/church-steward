package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.infrastructure.backup.BackupException;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.backup.BackupManifest;
import org.church.steward.infrastructure.config.AppHome;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.label.SubTitleLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 백업 파일로 전체 데이터를 복원하는 화면이다. 복원은 데이터베이스 파일을 교체하므로 로그인하기 전(Spring 컨텍스트를 띄우기 전)에만
 * 띄운다. 최초 실행 화면에서 직접 열거나, 데이터 관리 화면에서 예약한 복원을 다음 실행 때 수행하는 데 쓴다.
 */
@Slf4j
public class DatabaseRestoreView extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String NO_FILE_INFO = "백업 파일을 선택해주세요.";
    private static final String ADMIN_ID = "관리자";

    /**
     * 복원과 그 기록을 수행하는 동작. 데이터베이스를 교체한 뒤 활동 기록까지 남기려면 Spring 컨텍스트가 필요하므로, 컨텍스트를 띄울 수
     * 있는 애플리케이션 클래스가 만들어 넘긴다.
     */
    @FunctionalInterface
    public interface RestoreAction {
        /**
         * 백업 파일로 복원하고, 복원한 데이터베이스의 활동 기록에 복원을 기록한다.
         *
         * @param file         복원할 백업 파일
         * @param userId       백업한 시점의 사용자 ID. 복원 기록의 사용자가 된다.
         * @param userPassword 백업한 시점의 그 사용자의 비밀번호
         * @throws UserNotFoundException    백업에 없는 사용자일 때 발생
         * @throws InvalidPasswordException 사용자 비밀번호가 잘못되었을 때 발생
         */
        RestoreOutcome restore(Path file, String userId, String userPassword);
    }

    /**
     * 복원한 결과.
     *
     * @param preservedDir 복원 직전의 데이터를 옮겨 둔 디렉터리. 기존 데이터가 없었으면 비어 있음
     * @param logged       복원을 활동 기록에 남겼는지 여부. 복원은 이미 끝난 뒤이므로 기록에 실패해도 복원은 성공이다.
     */
    public record RestoreOutcome(Optional<Path> preservedDir, boolean logged) {
    }

    private final BackupManager backupManager;
    private final RestoreAction restoreAction;

    private final StyledTextField fileField;
    private final SubFieldLabel fileInfoLabel;
    private final StyledTextField userIdField;
    private final ValidatedPasswordField passwordField;
    private final JProgressBar progressBar;
    private final SecondaryButton browseButton;
    private final SecondaryButton cancelButton;
    private final PrimaryButton restoreButton;

    /** 복원할 수 있다고 확인된 백업 파일. 아직 고르지 않았거나 복원할 수 없는 파일이면 null */
    private Path backupFile;
    private boolean restored = false;
    /** 복원하는 중인지 여부. 복원하는 동안은 창을 닫을 수 없다. */
    private boolean busy = false;

    /**
     * @param backupManager 고른 백업 파일의 정보를 읽는 데 쓴다
     * @param restoreAction 복원과 그 기록을 수행하는 동작
     */
    public DatabaseRestoreView(Window owner, BackupManager backupManager, RestoreAction restoreAction) {
        super(owner, "백업에서 복원", ModalityType.APPLICATION_MODAL);
        // 예약된 복원은 부모 창 없이 띄우므로 아이콘을 직접 붙인다.
        AppIcons.apply(this);
        this.backupManager = backupManager;
        this.restoreAction = restoreAction;

        getContentPane().setBackground(UIColors.BACKGROUND);

        // 1. 컴포넌트 초기화
        TitleLabel titleLabel = new TitleLabel("백업에서 복원", SwingConstants.CENTER);
        SubTitleLabel subTitleLabel = new SubTitleLabel("백업한 시점의 관리자 비밀번호로 확인합니다.", SwingConstants.CENTER);

        fileField = new StyledTextField(ComponentSize.MEDIUM);
        fileField.setEditable(false);
        browseButton = new SecondaryButton("찾아보기", ComponentSize.MEDIUM);
        fileInfoLabel = new SubFieldLabel(NO_FILE_INFO, ComponentSize.MEDIUM);

        // 백업은 관리자만 할 수 있으므로 백업 파일에는 관리자 계정이 항상 있다. 그래서 관리자 계정으로만 복원한다.
        userIdField = new StyledTextField(ComponentSize.MEDIUM);
        userIdField.setText(ADMIN_ID);
        userIdField.setEditable(false);
        userIdField.setEnabled(false);
        passwordField = new ValidatedPasswordField(ComponentSize.MEDIUM);
        // 입력 화면이므로 포커스 테두리를 보인다(DialogKeyHandler를 쓰지 않아 직접 켠다).
        FocusIndicator.show(fileField);
        FocusIndicator.show(passwordField);

        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setVisible(false);
        progressBar.setForeground(UIColors.PRIMARY_BACKGROUND);

        cancelButton = new SecondaryButton("취소", ComponentSize.LARGE);
        restoreButton = new PrimaryButton("복원", ComponentSize.LARGE);

        // 2. 레이아웃 구성
        JPanel mainPanel = new JPanel(new BorderLayout(0, 25));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        JPanel headerPanel = new JPanel(new BorderLayout(0, 10));
        headerPanel.setOpaque(false);
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subTitleLabel, BorderLayout.SOUTH);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setOpaque(false);

        // 백업 파일
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        inputPanel.add(new FieldLabel("백업 파일", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        inputPanel.add(fileField, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.gridx = 1;
        gbc.gridy = 1;
        inputPanel.add(browseButton, gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        inputPanel.add(fileInfoLabel, gbc);

        // 사용자 ID
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        inputPanel.add(new FieldLabel("사용자 ID", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 20, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        inputPanel.add(userIdField, gbc);

        // 비밀번호
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        inputPanel.add(new FieldLabel("비밀번호", ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        inputPanel.add(passwordField, gbc);

        // 복원은 백업 파일의 덤프를 그대로 실행하므로, 조작된 백업은 이 PC에서 코드를 실행할 수 있다.
        // 남이 준 실행 파일을 실행하는 것과 같은 위험으로 보고 받아들였으므로, 막는 대신 신뢰할 수 있는 파일만 쓰도록 알린다.
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(20, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        inputPanel.add(new WarningNoticePanel("""
                직접 만들었거나 믿을 수 있는 사람에게 받은 백업 파일만 복원하세요. \
                조작된 백업 파일을 복원하면 이 PC가 악성 프로그램에 감염될 수 있습니다."""), gbc);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(restoreButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(inputPanel, BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        // 3. 동작 연결
        browseButton.addActionListener(_ -> chooseBackupFile());
        passwordField.addActionListener(_ -> restoreButton.doClick());
        cancelButton.addActionListener(_ -> dispose());
        restoreButton.addActionListener(_ -> restore());

        // 복원하는 중에 창을 닫으면 복원이 끝나기 전에 취소한 것으로 반환되어 로그인이 이어지므로, 그동안은 닫지 않는다.
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

    /**
     * 복원 화면을 보여준다.
     *
     * @param initialFile 미리 선택해 둘 백업 파일. 없으면 null
     * @return 복원했으면 true, 취소했으면 false
     */
    public boolean showRestoreDialog(Path initialFile) {
        restored = false;
        setSize(450, 660);
        setResizable(false);
        setLocationRelativeTo(getOwner());

        if (initialFile != null) {
            selectBackupFile(initialFile);
        }

        setVisible(true);
        return restored;
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
    }

    /**
     * 백업 파일의 정보를 읽어 보여준다. 복원할 수 없는 파일이면 사유를 보여주고 선택하지 않은 상태로 둔다.
     */
    private void selectBackupFile(Path file) {
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
            JOptionPane.showMessageDialog(this, e.getMessage(), "백업에서 복원", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void restore() {
        if (backupFile == null) {
            JOptionPane.showMessageDialog(this, NO_FILE_INFO, "백업에서 복원", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String userId = ADMIN_ID;
        String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));
        Path file = backupFile;

        setBusy(true);
        AsyncTask.run(this,
                () -> restoreAction.restore(file, userId, password),
                outcome -> {
                    JOptionPane.showMessageDialog(this, createSuccessMessage(outcome));
                    restored = true;
                    dispose();
                },
                err -> {
                    // 틀린 비밀번호는 흔한 입력 실수이므로 스택 트레이스 없이 남긴다.
                    if (err instanceof InvalidPasswordException) {
                        log.warn("Restore failed for user {}: {}", userId, err.getMessage());
                    } else {
                        log.error("Failed to restore from backup", err);
                    }
                    JOptionPane.showMessageDialog(this, "복원 실패: " + err.getMessage(), "백업에서 복원",
                            JOptionPane.ERROR_MESSAGE);

                    setBusy(false);
                    if (err instanceof InvalidPasswordException) {
                        passwordField.requestFocus();
                    }
                });
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

    private void setBusy(boolean busy) {
        this.busy = busy;
        browseButton.setEnabled(!busy);
        cancelButton.setEnabled(!busy);
        restoreButton.setEnabled(!busy);
        progressBar.setVisible(busy);
    }

}
