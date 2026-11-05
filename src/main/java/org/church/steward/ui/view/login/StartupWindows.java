package org.church.steward.ui.view.login;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;

import org.church.steward.ui.component.label.WrappingDescriptionLabel;

/**
 * 기동 화면의 창 높이를 내용에 맞추기 위한 도우미.
 */
final class StartupWindows {

    /** 높이를 재려고 처음 배치할 때 주는 높이. 어느 기동 화면의 내용보다 커야 한다. */
    private static final int MEASURE_HEIGHT = 1000;
    /** 높이가 바뀌지 않았는지 보기 전에 적어도 다시 배치하는 횟수 */
    private static final int MIN_LAYOUT_PASSES = 2;
    /** 높이가 계속 바뀌어도 다시 배치를 멈추는 횟수 */
    private static final int MAX_LAYOUT_PASSES = 5;

    private StartupWindows() {
    }

    /**
     * 창을 정한 폭으로 배치해 둔다. 이 뒤에 잰 선호 높이는 그 폭에서 줄바꿈한 글의 높이를 반영한다.
     * <p>
     * 줄바꿈하는 글({@link WrappingDescriptionLabel}, 주의사항의 글)은 폭이 정해져야 높이를 알 수 있으므로, 먼저
     * 정한 폭으로 한 번 배치한 뒤 다시 잰다.
     */
    static void layoutAtWidth(Window window, int width) {
        // 창이 화면에 표시될 준비가 되어야 제목 표시줄을 포함한 창 테두리의 크기를 알 수 있다.
        window.pack();
        // 아직 배치되지 않은 글은 한 줄 높이로 알리므로, 배치에 넉넉한 높이를 준다.
        window.setSize(width, MEASURE_HEIGHT);
        window.validate();
        // 배치되며 정해진 폭으로 줄바꿈 높이가 바뀌었으므로 다시 잰다. 줄바꿈하는 JTextArea는 새 폭으로 한 번 배치된 뒤에도
        // 앞선 폭의 줄 수로 높이를 알리기도 하므로, 정해진 횟수만큼 다시 배치한 뒤에도 높이가 바뀌면 바뀌지 않을 때까지 되풀이한다.
        int height = -1;
        for (int pass = 0; pass < MAX_LAYOUT_PASSES; pass++) {
            invalidateTree(window);
            window.validate();
            int measured = window.getPreferredSize().height;
            if (pass >= MIN_LAYOUT_PASSES - 1 && measured == height) {
                break;
            }
            height = measured;
        }
    }

    private static void invalidateTree(Component component) {
        component.invalidate();
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                invalidateTree(child);
            }
        }
    }
}
