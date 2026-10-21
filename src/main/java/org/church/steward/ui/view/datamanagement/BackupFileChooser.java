package org.church.steward.ui.view.datamanagement;

import java.awt.Component;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.church.steward.infrastructure.backup.BackupManager;

/**
 * 백업 파일을 고르는 파일 선택 창. 백업/복원 탭과 가져오기의 백업 단계가 함께 쓴다.
 */
public final class BackupFileChooser {

    private static final String EXTENSION = "." + BackupManager.FILE_EXTENSION;
    private static final String FILTER_DESCRIPTION = "청지기 백업 파일 (*" + EXTENSION + ")";
    private static final DateTimeFormatter FILE_NAME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm");

    private BackupFileChooser() {
        // 인스턴스화 방지
    }

    /**
     * 저장할 백업 파일을 고르게 한다. 파일 이름은 "청지기_백업_20261013_1530"처럼 지금 시각으로 미리 채워 둔다.
     *
     * @param directory 처음에 보여줄 폴더. null이면 기본 폴더
     * @return 고른 파일. 취소했거나 기존 파일을 덮어쓰지 않기로 했으면 null
     */
    public static File chooseTarget(Component parent, File directory) {
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setDialogTitle("백업 파일 저장");
        chooser.setFileFilter(new FileNameExtensionFilter(FILTER_DESCRIPTION, BackupManager.FILE_EXTENSION));
        chooser.setSelectedFile(new File(directory, "청지기_백업_" + LocalDateTime.now().format(FILE_NAME_FORMAT)
                + EXTENSION));
        if (chooser.showSaveDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }

        File file = chooser.getSelectedFile();
        if (!file.getName()
                .toLowerCase()
                .endsWith(EXTENSION)) {
            file = new File(file.getParentFile(), file.getName() + EXTENSION);
        }
        if (file.exists()) {
            int answer = JOptionPane.showConfirmDialog(parent, "같은 이름의 파일이 이미 있습니다. 덮어쓸까요?\n" + file.getName(),
                    "백업 파일 저장", JOptionPane.YES_NO_OPTION);
            if (answer != JOptionPane.YES_OPTION) {
                return null;
            }
        }
        return file;
    }

    /**
     * 복원할 백업 파일을 고르게 한다.
     *
     * @param directory 처음에 보여줄 폴더. null이면 기본 폴더
     * @return 고른 파일. 취소했으면 null
     */
    public static File chooseSource(Component parent, File directory) {
        JFileChooser chooser = new JFileChooser(directory);
        chooser.setDialogTitle("복원할 백업 파일 선택");
        chooser.setFileFilter(new FileNameExtensionFilter(FILTER_DESCRIPTION, BackupManager.FILE_EXTENSION));
        if (chooser.showOpenDialog(parent) != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        return chooser.getSelectedFile();
    }

}
