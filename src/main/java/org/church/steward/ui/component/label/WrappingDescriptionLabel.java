package org.church.steward.ui.component.label;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.View;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 놓인 칸의 폭에 맞춰 줄바꿈하는 설명 글. 넘긴 줄바꿈("\n")은 그대로 지키고, 칸보다 긴 줄은 단어 단위로 나눈다. 기본 색은
 * {@link SubFieldLabel}과 같은 회색이다.
 * <p>
 * 선호 폭을 0으로 알려 글이 부모를 넓히지 않게 한다. 글이 칸보다 넓다고 알리면 부모의 선호 폭이 화면보다 넓어지고, 그러면
 * {@link java.awt.GridBagLayout}은 모든 컴포넌트를 최소 크기로 배치해 입력란이 거의 보이지 않게 된다. 그래서 칸의
 * 폭은 부모가 정해야 한다(예: GridBagLayout의 {@code fill = HORIZONTAL}). 높이는 지금 폭에서 줄바꿈한
 * 높이이며, 폭이 바뀌면 높이를 다시 계산하도록 레이아웃을 다시 한다.
 */
public class WrappingDescriptionLabel extends JTextArea {

    private static final long serialVersionUID = 1L;

    public WrappingDescriptionLabel(String text, ComponentSize componentSize) {
        super(text);
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        setFont(new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize));
        setForeground(UIColors.TEXT_SUB);
        setEditable(false);
        setFocusable(false);
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setBorder(null);
    }

    @Override
    public Dimension getPreferredSize() {
        Insets insets = getInsets();
        int width = getWidth() - insets.left - insets.right;
        int height;
        if (width > 0) {
            View root = getUI().getRootView(this);
            root.setSize(width, Integer.MAX_VALUE);
            height = (int) Math.ceil(root.getPreferredSpan(View.Y_AXIS));
        } else {
            // 아직 배치되지 않았으면 넘긴 줄 수만큼의 높이로 시작한다.
            height = getLineCount() * getFontMetrics(getFont()).getHeight();
        }
        return new Dimension(0, height + insets.top + insets.bottom);
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        boolean widthChanged = width != getWidth();
        super.setBounds(x, y, width, height);
        if (widthChanged) {
            SwingUtilities.invokeLater(this::revalidate);
        }
    }
}
