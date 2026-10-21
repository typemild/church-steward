package org.church.steward.ui.view.datamanagement;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.domain.backup.dto.BackupInfoDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 데이터 관리의 "백업/복원" 탭. 전체 데이터를 백업 파일로 저장하고, 백업 파일로 복원하도록 예약한다.
 * 복원은 데이터베이스 파일을 교체하므로 여기서는 예약만 하고 프로그램을 끝내며, 다음 실행 때 로그인 전에 수행된다.
 */
@UiView
@Slf4j
public class BackupRestorePanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final BackupController backupController;

    private final PrimaryButton backupButton;
    private final SecondaryButton restoreButton;

    /** 마지막으로 백업 파일을 저장하거나 고른 폴더. 처음에는 null이어서 파일 선택 창이 기본 폴더에서 열린다. */
    private File lastDirectory;

    public BackupRestorePanelView(BackupController backupController) {
        this.backupController = backupController;

        setLayout(new BorderLayout(0, 25));
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        TitleLabel titleLabel = new TitleLabel("전체 데이터 백업/복원");
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        backupButton = new PrimaryButton("백업 파일 만들기", ComponentSize.MEDIUM);
        restoreButton = new SecondaryButton("백업 파일에서 복원", ComponentSize.MEDIUM);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        addSection(contentPanel, 0, "백업",
                "모든 데이터를 파일 하나로 저장합니다.\n"
                        + "백업 파일은 암호화되어 있으며, 복원하려면 백업한 시점의 계정과 비밀번호가 필요합니다.\n"
                        + "백업한 뒤에 비밀번호를 바꾸더라도 그 백업 파일은 예전 비밀번호로 복원합니다.",
                backupButton);
        addSection(contentPanel, 3, "복원",
                "현재 데이터 전체를 백업 파일의 내용으로 바꿉니다. 백업한 뒤에 입력한 내용은 사라집니다.\n"
                        + "복원은 프로그램을 종료한 뒤 다시 실행할 때 진행됩니다.\n"
                        + "복원 직전의 데이터는 data 폴더 아래에 따로 보관합니다.",
                restoreButton);

        // 남는 공간을 아래쪽에 두어 내용을 위로 붙인다.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weighty = 1.0;
        contentPanel.add(Box.createVerticalGlue(), gbc);

        add(contentPanel, BorderLayout.CENTER);

        backupButton.addActionListener(_ -> createBackup());
        restoreButton.addActionListener(_ -> scheduleRestore());
    }

    /**
     * 제목, 설명, 버튼으로 이루어진 영역 하나를 startRow부터 세 행에 걸쳐 놓는다.
     */
    private void addSection(JPanel panel, int startRow, String header, String description, JButton button) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 8, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = startRow;
        gbc.weightx = 1.0;
        panel.add(new SectionHeaderLabel(header), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 15, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = startRow + 1;
        gbc.weightx = 1.0;
        panel.add(new DescriptionLabel(description), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 25, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = startRow + 2;
        panel.add(button, gbc);
    }

    @Override
    public void onMenuSelected() {
        // 화면에 들어올 때 갱신할 내용이 없다.
    }

    private void createBackup() {
        File file = BackupFileChooser.chooseTarget(this, lastDirectory);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();

        AsyncTask.run(backupButton,
                () -> {
                    backupController.createBackup(file.toPath());
                    return null;
                },
                _ -> JOptionPane.showMessageDialog(this, "백업 파일을 저장했습니다.\n" + file.getAbsolutePath()),
                err -> {
                    log.error("Failed to create backup", err);
                    JOptionPane.showMessageDialog(this, "백업에 실패했습니다: " + err.getMessage(), "백업",
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    /**
     * 복원할 백업 파일을 고르게 하고, 파일의 정보를 보여준 뒤 동의하면 복원을 예약하고 프로그램을 끝낸다.
     */
    private void scheduleRestore() {
        File file = BackupFileChooser.chooseSource(this, lastDirectory);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();
        Path path = file.toPath();

        AsyncTask.run(restoreButton,
                () -> backupController.inspectBackup(path),
                info -> {
                    if (confirmRestore(info)) {
                        scheduleRestoreAndExit(path);
                    }
                },
                err -> {
                    log.error("Failed to read backup file", err);
                    JOptionPane.showMessageDialog(this, err.getMessage(), "복원", JOptionPane.ERROR_MESSAGE);
                });
    }

    private boolean confirmRestore(BackupInfoDto info) {
        String message = "백업 일시: " + info.getCreatedAt().format(CREATED_AT_FORMAT) + "\n"
                + "프로그램 버전: " + info.getProgramVersion() + "\n\n"
                + "복원하면 현재 데이터 전체가 이 백업의 내용으로 바뀝니다.\n\n"
                + "\"예\"를 누르면 프로그램이 종료되며, 프로그램을 다시 실행하시면 복원 화면이 나타납니다.\n"
                + "복원 화면에서 백업한 시점의 계정과 비밀번호를 입력하시면 복원이 완료됩니다.";
        int answer = JOptionPane.showConfirmDialog(this, message, "복원", JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        return answer == JOptionPane.YES_OPTION;
    }

    private void scheduleRestoreAndExit(Path path) {
        AsyncTask.run(restoreButton,
                () -> {
                    backupController.scheduleRestore(path);
                    return null;
                },
                _ -> {
                    log.info("Exiting to restore on next start");
                    System.exit(0);
                },
                err -> {
                    log.error("Failed to schedule restore", err);
                    JOptionPane.showMessageDialog(this, "복원을 예약하지 못했습니다: " + err.getMessage(), "복원",
                            JOptionPane.ERROR_MESSAGE);
                });
    }

}
