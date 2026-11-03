package org.church.steward.ui.view.login;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.church.steward.ui.util.UIColors;

/**
 * 기동 화면(로그인, 최초 실행, 복원)의 진행 표시. 작업하지 않을 때도 막대의 자리를 비워 둔다.
 * <p>
 * 기동 화면은 창 높이가 고정되어 있거나 내용에 맞춰져 있으므로, 막대가 나타나며 자리를 새로 차지하면 그만큼 내용이 줄어 입력란이 밀리거나
 * 아래쪽의 주의사항이 가려진다.
 */
class StartupProgressBar extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JProgressBar bar = new JProgressBar();

    StartupProgressBar() {
        super(new BorderLayout());
        setOpaque(false);
        bar.setIndeterminate(true);
        bar.setForeground(UIColors.PRIMARY_BACKGROUND);
        bar.setVisible(false);
        add(bar, BorderLayout.CENTER);
    }

    /**
     * 막대가 숨어 있어도 막대의 크기를 알린다. 그대로 두면 숨은 막대는 배치에서 빠져 크기가 0이 된다.
     */
    @Override
    public Dimension getPreferredSize() {
        return bar.getPreferredSize();
    }

    /**
     * 작업하는 동안 막대를 보인다.
     */
    void setRunning(boolean running) {
        bar.setVisible(running);
    }
}
