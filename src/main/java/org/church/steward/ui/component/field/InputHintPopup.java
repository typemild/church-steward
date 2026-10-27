package org.church.steward.ui.component.field;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 입력란 바로 아래에 띄우는 짧은 안내다(예: 한/영 전환 확인, 최대 길이 초과).
 * 입력란이 포커스를 잃거나 화면에서 빠지면 사라지며, 시간을 지정하면 그 시간이 지난 뒤에도 사라진다.
 * 스크롤 패인 안의 입력란(여러 줄 입력란)이면 스크롤 패인 아래에 띄운다.
 */
public class InputHintPopup {

    private final JComponent owner;
    private final ComponentSize componentSize;
    private final Timer hideTimer;
    private Popup popup;

    public InputHintPopup(JComponent owner, ComponentSize componentSize) {
        this.owner = owner;
        this.componentSize = componentSize;
        this.hideTimer = new Timer(0, _ -> hide());
        hideTimer.setRepeats(false);

        owner.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                hide();
            }
        });
        owner.addAncestorListener(new AncestorListener() {
            @Override
            public void ancestorAdded(AncestorEvent event) {
            }

            @Override
            public void ancestorRemoved(AncestorEvent event) {
                hide();
            }

            @Override
            public void ancestorMoved(AncestorEvent event) {
            }
        });
    }

    /**
     * 안내를 띄운다. 다른 안내가 떠 있으면 바꾼다. 입력란이 화면에 보이지 않으면 띄우지 않는다.
     */
    public void show(String message) {
        hide();
        Component anchor = anchor();
        if (!anchor.isShowing()) {
            return;
        }

        Point location = anchor.getLocationOnScreen();
        popup = PopupFactory.getSharedInstance()
                .getPopup(owner, createContent(message), location.x, location.y + anchor.getHeight());
        popup.show();
    }

    /**
     * 안내를 띄우고 지정한 시간이 지나면 없앤다.
     */
    public void show(String message, int durationMillis) {
        show(message);
        if (popup != null) {
            hideTimer.setInitialDelay(durationMillis);
            hideTimer.restart();
        }
    }

    public void hide() {
        hideTimer.stop();
        if (popup != null) {
            popup.hide();
            popup = null;
        }
    }

    private Component anchor() {
        if (owner.getParent() instanceof JViewport) {
            Component scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane.class, owner);
            if (scrollPane != null) {
                return scrollPane;
            }
        }
        return owner;
    }

    private JPanel createContent(String message) {
        final int fontSize = switch (componentSize) {
        case LARGE -> Math.round(UIFonts.FONT_SIZE_LARGE * 0.9f);
        case MEDIUM -> Math.round(UIFonts.FONT_SIZE_BASE * 0.9f);
        case SMALL -> Math.round(UIFonts.FONT_SIZE_SMALL * 0.9f);
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Border padding = BorderFactory.createEmptyBorder(
                Math.round(fontSize * 0.38f),
                Math.round(fontSize * 0.76f),
                Math.round(fontSize * 0.38f),
                Math.round(fontSize * 0.76f));

        JLabel label = new JLabel(message);
        label.setForeground(UIColors.INPUT_HINT_TEXT);
        label.setBorder(padding);
        label.setFont(font);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(UIColors.INPUT_HINT_BACKGROUND);
        content.setBorder(BorderFactory.createLineBorder(UIColors.INPUT_HINT_BORDER, 1));
        content.add(label, BorderLayout.CENTER);
        return content;
    }

}
