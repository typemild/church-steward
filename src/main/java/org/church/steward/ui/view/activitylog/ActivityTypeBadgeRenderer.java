package org.church.steward.ui.view.activitylog;

import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

import org.church.steward.domain.activitylog.entity.ActivityType;

public class ActivityTypeBadgeRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    // Map.of는 10쌍까지만 받으므로 Map.ofEntries를 쓴다.
    private static final Map<ActivityType, Color> COLOR_MAP = Map.ofEntries(
            Map.entry(ActivityType.CREATE, new Color(76, 175, 80)), // Green
            Map.entry(ActivityType.UPDATE, new Color(33, 150, 243)), // Blue
            Map.entry(ActivityType.DELETE, new Color(244, 67, 54)), // Red
            Map.entry(ActivityType.CLOSE, new Color(0, 150, 136)), // Teal
            Map.entry(ActivityType.REOPEN, new Color(96, 125, 139)), // Blue Grey
            Map.entry(ActivityType.PRINT, new Color(156, 39, 177)), // Purple
            Map.entry(ActivityType.IMPORT, new Color(0, 172, 193)), // Cyan
            Map.entry(ActivityType.EXPORT, new Color(63, 81, 181)), // Indigo
            Map.entry(ActivityType.BACKUP, new Color(121, 85, 72)), // Brown
            Map.entry(ActivityType.RESTORE, new Color(233, 30, 99)), // Pink
            Map.entry(ActivityType.LOGIN, new Color(255, 153, 0)) // Orange
    );

    private boolean selected;
    private JTable table;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
            boolean isSelected, boolean hasFocus, int row, int column) {
        this.table = table;
        this.selected = isSelected;

        setOpaque(false);
        setHorizontalAlignment(SwingConstants.CENTER);

        if (value instanceof ActivityType type) {
            setText(type.getLabel());
            setBackground(COLOR_MAP.getOrDefault(type, Color.GRAY));
            setForeground(Color.WHITE);
        } else {
            setText("");
            setBackground(TRANSPARENT);
            setForeground(table.getForeground());
        }
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 셀 배경(여백)은 선택 여부에 따라 먼저 채운다
        g2.setColor(selected ? table.getSelectionBackground() : table.getBackground());
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (getBackground().equals(TRANSPARENT)) {
            g2.dispose();
            return;
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textWidth = fm.stringWidth(text);

        int ascent = fm.getAscent();
        int descent = fm.getDescent();
        int padding = 6;
        int height = ascent + descent + padding;

        // 가장 긴 유형 이름("마감 취소")이 들어가는 폭
        int width = 78;

        int x = (getWidth() - width) / 2;
        int y = (getHeight() - height) / 2;

        // 뱃지 색은 선택 여부와 무관하게 원래 색 유지
        g2.setColor(getBackground());
        g2.fillRoundRect(x, y, width, height, height, height);

        g2.setColor(getForeground());
        g2.drawString(text, (getWidth() - textWidth) / 2, y + padding / 2 + ascent);

        g2.dispose();
    }
}
