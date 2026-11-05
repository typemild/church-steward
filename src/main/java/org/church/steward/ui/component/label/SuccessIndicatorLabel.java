package org.church.steward.ui.component.label;

import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JLabel;
import javax.swing.Timer;

import org.church.steward.ui.util.SuccessFeedback;

/**
 * 작업이 끝났음을 알리는 체크 아이콘. {@link #showSuccess()}를 부르면 바로 나타났다가 끝에 서서히 사라진다. 전체 시간은
 * {@link SuccessFeedback#DURATION_MILLIS}다. 누른 버튼이 없어 입력란 옆에 띄우는 완료 표시에 쓴다(아이콘
 * 버튼은 {@code IconButton#showSuccess}).
 */
public class SuccessIndicatorLabel extends JLabel {

    private static final long serialVersionUID = 1L;

    /** 사라지는 시간(ms). 나타날 때는 바로 보인다. */
    private static final int FADE_OUT_MILLIS = 200;
    /** 투명도를 바꾸는 간격(ms). 초당 60번 정도다. */
    private static final int FRAME_MILLIS = 16;

    private final Timer frameTimer = new Timer(FRAME_MILLIS, _ -> updateAlpha());
    /** 이번 표시를 시작한 시각(ms) */
    private long startedAt;
    /** 지금 그리는 불투명도(0 ~ 1) */
    private float alpha = 0f;

    /**
     * @param iconSize 체크 아이콘의 크기(px)
     */
    public SuccessIndicatorLabel(int iconSize) {
        super(SuccessFeedback.createIcon(iconSize));
        setVisible(false);
    }

    /**
     * 체크를 바로 보였다가 끝에 서서히 사라지게 한다. 보이는 동안 다시 부르면 다시 온전히 보이고 시간을 처음부터 센다.
     */
    public void showSuccess() {
        startedAt = System.currentTimeMillis();
        setVisible(true);
        frameTimer.restart();
        updateAlpha();
    }

    /**
     * 체크를 바로 숨긴다.
     */
    public void hideNow() {
        frameTimer.stop();
        alpha = 0f;
        setVisible(false);
    }

    private void updateAlpha() {
        long elapsed = System.currentTimeMillis() - startedAt;
        long fadeOutStart = SuccessFeedback.DURATION_MILLIS - FADE_OUT_MILLIS;
        if (elapsed >= SuccessFeedback.DURATION_MILLIS) {
            hideNow();
            return;
        }
        if (elapsed < fadeOutStart) {
            alpha = 1f;
        } else {
            alpha = 1f - (float) (elapsed - fadeOutStart) / FADE_OUT_MILLIS;
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        super.paintComponent(g2);
        g2.dispose();
    }
}
