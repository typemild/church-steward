package org.church.steward.ui.view.datamanagement.importing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.domain.backup.controller.BackupController;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.view.datamanagement.BackupFileChooser;

import lombok.extern.slf4j.Slf4j;

/**
 * 가져오기의 마지막 단계. 반영하기 전에 전체 데이터를 백업하게 한다. 백업에 성공해야 반영할 수 있다.
 * 가져오기는 한 번에 여러 건을 바꾸고 되돌리는 기능이 없으므로, 잘못 가져왔을 때 돌아갈 수 있는 지점을 만들어 두는 것이다.
 * 예산 가져오기와 수입/지출 가져오기가 함께 쓴다.
 */
@Slf4j
public class CommitStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final ImportSession session;
    private final BackupController backupController;
    private final Supplier<String> summarySupplier;
    private final Runnable onBackupChanged;

    private final FieldLabel summaryLabel = new FieldLabel(" ", ComponentSize.MEDIUM, true);
    private final SecondaryButton backupButton = new SecondaryButton("백업 파일 만들기", ComponentSize.MEDIUM);
    private final FieldLabel backupStatusLabel = new FieldLabel(" ", ComponentSize.MEDIUM);

    private File lastDirectory;

    /**
     * @param summarySupplier 무엇을 반영하게 되는지를 한 줄로 알려주는 문구를 만드는 함수
     * @param onBackupChanged 백업을 받았을 때 실행할 동작
     */
    public CommitStepPanel(ImportSession session, BackupController backupController,
            Supplier<String> summarySupplier, Runnable onBackupChanged) {
        this.session = session;
        this.backupController = backupController;
        this.summarySupplier = summarySupplier;
        this.onBackupChanged = onBackupChanged;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);

        int row = 0;
        addRow(contentPanel, row++, summaryLabel, 20);
        addRow(contentPanel, row++, new SectionHeaderLabel("1. 백업"), 8);
        addRow(contentPanel, row++, new DescriptionLabel("반영하기 전에 전체 데이터를 백업합니다. 가져온 결과가 잘못되었으면\n"
                + "데이터 관리 > 백업/복원에서 이 백업 파일로 되돌릴 수 있습니다.\n"
                + "백업한 뒤에 다른 메뉴에서 데이터를 바꾸면 백업을 다시 받아야 합니다."), 12);
        addRow(contentPanel, row++, wrap(backupButton), 8);
        addRow(contentPanel, row++, backupStatusLabel, 20);
        addRow(contentPanel, row++, new SectionHeaderLabel("2. 반영"), 8);
        addRow(contentPanel, row++, new DescriptionLabel("아래의 \"가져오기 실행\"을 누르면 선택한 건을 한꺼번에 반영합니다.\n"
                + "하나라도 반영할 수 없으면 아무것도 반영하지 않습니다."), 0);

        add(contentPanel, BorderLayout.NORTH);

        backupButton.addActionListener(_ -> createBackup());
    }

    private void addRow(JPanel panel, int row, JComponent component, int bottomGap) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, bottomGap, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(component, gbc);
    }

    /**
     * 버튼이 행의 폭만큼 늘어나지 않도록 왼쪽에 붙여 둔다.
     */
    private JComponent wrap(JComponent component) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(component, BorderLayout.WEST);
        panel.add(Box.createHorizontalGlue(), BorderLayout.CENTER);
        return panel;
    }

    /**
     * 세션의 선택과 백업 상태를 화면에 반영한다.
     */
    public void refresh() {
        summaryLabel.setText(summarySupplier.get());
        backupStatusLabel.setText(session.hasBackup() ? "백업 완료: " + session.getBackupFile()
                : "아직 백업하지 않았습니다.");
    }

    private void createBackup() {
        File file = BackupFileChooser.chooseTarget(this, lastDirectory);
        if (file == null) {
            return;
        }
        lastDirectory = file.getParentFile();

        AsyncTask.run(backupButton,
                () -> backupController.createBackup(file.toPath()),
                backupLogId -> {
                    session.setBackup(backupLogId, file.toPath());
                    refresh();
                    onBackupChanged.run();
                },
                err -> {
                    log.error("Failed to create backup before import", err);
                    JOptionPane.showMessageDialog(this, "백업에 실패했습니다: " + err.getMessage(), "백업",
                            JOptionPane.ERROR_MESSAGE);
                });
    }

}
