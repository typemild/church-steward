package org.church.steward.ui.view.dashboard;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import javax.swing.JComponent;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * "수입 | 지출" 중 하나를 고르는 알약 모양의 전환 버튼
 */
class BudgetTypeToggle extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    /** 왼쪽부터 놓이는 순서 */
    private static final BudgetType[] TYPES = { BudgetType.INCOME, BudgetType.EXPENSE };
    private static final int SEGMENT_WIDTH = 40;
    private static final int HEIGHT = 20;

    private final transient Consumer<BudgetType> onSelected;
    /** 처음에는 수입을 보여준다. */
    private BudgetType selectedType = BudgetType.INCOME;

    /**
     * @param onSelected 사용자가 눌러서 선택이 바뀌었을 때 호출된다. {@link #setSelectedType}으로 바꿀 때는
     *                   호출되지 않는다.
     */
    BudgetTypeToggle(Consumer<BudgetType> onSelected) {
        this.onSelected = onSelected;

        Dimension size = new Dimension(SEGMENT_WIDTH * TYPES.length, HEIGHT);
        setPreferredSize(size);
        setMinimumSize(size);
        setMaximumSize(size);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = e.getX() / SEGMENT_WIDTH;
                if (e.getX() < 0 || index >= TYPES.length || TYPES[index] == selectedType) {
                    return;
                }
                setSelectedType(TYPES[index]);
                BudgetTypeToggle.this.onSelected.accept(selectedType);
            }
        });
    }

    BudgetType getSelectedType() {
        return selectedType;
    }

    void setSelectedType(BudgetType type) {
        selectedType = type;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = DashboardGraphics.create(g);
        try {
            g2.setColor(UIColors.METER_TRACK);
            g2.fillRoundRect(0, 0, getWidth(), HEIGHT, HEIGHT, HEIGHT);

            g2.setFont(FONT);
            FontMetrics metrics = g2.getFontMetrics();
            int baseline = (HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

            for (int i = 0; i < TYPES.length; i++) {
                int x = i * SEGMENT_WIDTH;
                boolean isSelected = TYPES[i] == selectedType;
                if (isSelected) {
                    // 선택된 칸은 목록의 진행 막대와 같은 색(수입 색 또는 지출 색)으로 칠한다.
                    g2.setColor(TYPES[i] == BudgetType.INCOME ? UIColors.INCOME : UIColors.EXPENSE);
                    g2.fillRoundRect(x, 0, SEGMENT_WIDTH, HEIGHT, HEIGHT, HEIGHT);
                }

                String label = TYPES[i].getLabel();
                g2.setColor(isSelected ? UIColors.PRIMARY_TEXT : UIColors.TEXT_SUB);
                g2.drawString(label, x + (SEGMENT_WIDTH - metrics.stringWidth(label)) / 2, baseline);
            }
        } finally {
            g2.dispose();
        }
    }
}
