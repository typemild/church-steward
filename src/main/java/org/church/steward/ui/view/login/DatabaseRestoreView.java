package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.SubTitleLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.UIColors;

/**
 * 데이터 관리 화면에서 예약한 복원을 다음 실행 때 수행하는 화면이다. 복원은 데이터베이스 파일을 교체하므로 로그인하기 전(Spring
 * 컨텍스트를 띄우기 전)에만 띄운다. 입력부와 복원 실행은 최초 실행 화면과 함께 쓰는 {@link RestoreFormPanel}이
 * 맡는다.
 */
public class DatabaseRestoreView extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int WINDOW_WIDTH = 450;

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

    private final RestoreFormPanel formPanel;
    private final StartupProgressBar progressBar;
    private final SecondaryButton cancelButton;
    private final PrimaryButton restoreButton;

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

        getContentPane().setBackground(UIColors.BACKGROUND);

        // 1. 컴포넌트 초기화
        TitleLabel titleLabel = new TitleLabel("백업에서 복원", SwingConstants.CENTER);
        SubTitleLabel subTitleLabel = new SubTitleLabel("백업한 시점의 관리자 비밀번호로 확인합니다.", SwingConstants.CENTER);
        formPanel = new RestoreFormPanel(backupManager, restoreAction);

        // 복원하는 동안 진행 표시가 나타나도 주의사항이 가려지지 않도록 자리를 비워 둔다.
        progressBar = new StartupProgressBar();

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

        // 주의사항을 가운데(남는 높이를 모두 차지하는 자리)에 두면 창 높이에 따라 상자가 늘어나므로, 입력부 아래에 붙인다.
        JPanel inputPanel = new JPanel(new BorderLayout(0, 20));
        inputPanel.setOpaque(false);
        inputPanel.add(formPanel, BorderLayout.NORTH);
        inputPanel.add(RestoreFormPanel.createWarningNotice(), BorderLayout.SOUTH);

        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(cancelButton);
        buttonPanel.add(restoreButton);

        JPanel bottomPanel = new JPanel(new BorderLayout(0, 15));
        bottomPanel.setOpaque(false);
        bottomPanel.add(progressBar, BorderLayout.NORTH);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(wrapAtTop(inputPanel), BorderLayout.CENTER);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        // 3. 동작 연결
        formPanel.getPasswordField()
                .addActionListener(_ -> restoreButton.doClick());
        cancelButton.addActionListener(_ -> dispose());
        restoreButton.addActionListener(_ -> formPanel.restore(this::setBusy, () -> {
            restored = true;
            dispose();
        }));

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
        setResizable(false);

        if (initialFile != null) {
            formPanel.selectBackupFile(initialFile);
        }

        // 주의사항의 글은 폭에 따라 줄 수가 달라지므로, 정한 폭으로 배치한 뒤 내용에 맞는 높이를 잰다.
        StartupWindows.layoutAtWidth(this, WINDOW_WIDTH);
        setSize(WINDOW_WIDTH, getPreferredSize().height);
        validate();
        setLocationRelativeTo(getOwner());

        setVisible(true);
        return restored;
    }

    private void setBusy(boolean busy) {
        this.busy = busy;
        cancelButton.setEnabled(!busy);
        restoreButton.setEnabled(!busy);
        progressBar.setRunning(busy);
    }

    /**
     * 내용을 위쪽에 붙이고 남는 높이는 비워 둔다.
     */
    private static JPanel wrapAtTop(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(content, BorderLayout.NORTH);
        return wrapper;
    }

}
