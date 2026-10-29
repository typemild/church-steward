package org.church.steward.ui.view.dashboard;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.ToolTipManager;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.dashboard.dto.BudgetProgressDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 최상위 예산별로 예산 금액 대비 실제 금액을 진행 막대로 보여주는 목록. 수입 예산과 지출 예산 중 한쪽만 보여주며, 예산마다 한 줄에
 * 예산명, 진행 막대, 진행률을 그린다. 진행률이 높은 예산부터 나열하고, 금액은 툴팁으로 보여준다.
 */
class BudgetProgressList extends JComponent implements Scrollable {

    private static final long serialVersionUID = 1L;

    private static final Font NAME_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
    private static final Font DETAIL_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private static final String EMPTY_MESSAGE_FORMAT = "설정된 %s 예산이 없습니다.";
    private static final String WIDEST_RATE = "100%";
    private static final int MAX_RATE = 999;

    private static final int ITEM_HEIGHT = 26;
    private static final int EMPTY_HEIGHT = 60;
    private static final int TEXT_GAP = 8;
    /** 한 줄에서 예산명이 차지하는 폭의 비율 */
    private static final double NAME_WIDTH_RATIO = 0.4;
    /** 스크롤바가 보일 때 내용과 스크롤바 사이에 두는 여백 */
    private static final int SCROLLBAR_GAP = 10;

    private List<BudgetProgressDto> allItems = List.of();
    private List<BudgetProgressDto> visibleItems = List.of();
    private BudgetType type = BudgetType.EXPENSE;

    BudgetProgressList() {
        setOpaque(false);
        ToolTipManager.sharedInstance()
                .registerComponent(this);
    }

    void setData(List<BudgetProgressDto> progressList) {
        allItems = List.copyOf(progressList);
        updateVisibleItems();
    }

    /**
     * 보여줄 예산의 구분을 바꾼다.
     */
    void setType(BudgetType type) {
        this.type = type;
        updateVisibleItems();
    }

    /**
     * 해당 구분의 예산이 하나라도 있는지 여부
     */
    boolean hasItems(BudgetType type) {
        return allItems.stream()
                .anyMatch(item -> item.getType() == type);
    }

    private void updateVisibleItems() {
        // 진행률이 같으면 서비스가 돌려준 순서(예산 관리에서 정한 순서)를 유지한다.
        visibleItems = allItems.stream()
                .filter(item -> item.getType() == type)
                .sorted(Comparator.comparingDouble(BudgetProgressDto::getProgressRate)
                        .reversed())
                .toList();
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, getContentHeight());
    }

    private int getContentHeight() {
        return visibleItems.isEmpty() ? EMPTY_HEIGHT : visibleItems.size() * ITEM_HEIGHT;
    }

    /**
     * 내용을 그릴 수 있는 폭. 내용이 길어 스크롤바가 보일 때는 스크롤바에 바짝 붙지 않게 여백을 뺀다.
     */
    private int getContentWidth() {
        Container viewport = getParent();
        boolean isScrollable = viewport != null && getContentHeight() > viewport.getHeight();
        return isScrollable ? getWidth() - SCROLLBAR_GAP : getWidth();
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int index = e.getY() / ITEM_HEIGHT;
        if (e.getY() < 0 || index >= visibleItems.size()) {
            return null;
        }
        return createToolTip(visibleItems.get(index));
    }

    private String createToolTip(BudgetProgressDto item) {
        // 툴팁에서 줄을 바꾸려면 HTML을 써야 한다.
        return String.format("<html><b>%s</b><br>예산 %,d원<br>%s %,d원 (%s)<br>차액 %,d원</html>",
                DashboardGraphics.escapeHtml(item.getBudgetName()),
                item.getBudgetedAmount(),
                item.getType().getLabel(),
                item.getActualAmount(),
                formatRate(item),
                item.getBudgetedAmount() - item.getActualAmount());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = DashboardGraphics.create(g);
        try {
            if (visibleItems.isEmpty()) {
                paintEmptyMessage(g2);
                return;
            }

            int contentWidth = getContentWidth();
            Rectangle clip = g2.getClipBounds();
            for (int i = 0; i < visibleItems.size(); i++) {
                int y = i * ITEM_HEIGHT;
                if (clip != null && (y + ITEM_HEIGHT < clip.y || y > clip.y + clip.height)) {
                    continue;
                }
                paintItem(g2, visibleItems.get(i), y, contentWidth);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintEmptyMessage(Graphics2D g2) {
        String message = String.format(EMPTY_MESSAGE_FORMAT, type.getLabel());
        g2.setFont(DETAIL_FONT);
        FontMetrics metrics = g2.getFontMetrics();
        g2.setColor(UIColors.TEXT_SUB);
        g2.drawString(message, (getWidth() - metrics.stringWidth(message)) / 2,
                (EMPTY_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    /**
     * 한 줄에 예산명(왼쪽), 진행 막대(가운데), 진행률(오른쪽)을 그린다.
     */
    private void paintItem(Graphics2D g2, BudgetProgressDto item, int y, int contentWidth) {
        FontMetrics nameMetrics = g2.getFontMetrics(NAME_FONT);
        FontMetrics detailMetrics = g2.getFontMetrics(DETAIL_FONT);
        int centerY = y + ITEM_HEIGHT / 2;

        int nameWidth = (int) Math.round(contentWidth * NAME_WIDTH_RATIO);
        g2.setFont(NAME_FONT);
        g2.setColor(UIColors.TEXT_MAIN);
        g2.drawString(DashboardGraphics.clip(item.getBudgetName(), nameMetrics, nameWidth), 0,
                centerY + (nameMetrics.getAscent() - nameMetrics.getDescent()) / 2);

        int rateWidth = detailMetrics.stringWidth(WIDEST_RATE) + TEXT_GAP;
        int meterX = nameWidth + TEXT_GAP;
        int meterWidth = contentWidth - rateWidth - meterX;
        double ratio = item.getBudgetedAmount() > 0
                ? (double) item.getActualAmount() / item.getBudgetedAmount()
                : 0;
        if (meterWidth > 0) {
            ProgressMeter.paintMeter(g2, meterX, centerY - ProgressMeter.THICKNESS / 2, meterWidth, ratio,
                    getTypeColor(item.getType()));
        }

        String rate = formatRate(item);
        g2.setFont(DETAIL_FONT);
        g2.setColor(isOverBudget(item) ? UIColors.ERROR_WARNING : UIColors.TEXT_SUB);
        g2.drawString(rate, contentWidth - detailMetrics.stringWidth(rate),
                centerY + (detailMetrics.getAscent() - detailMetrics.getDescent()) / 2);
    }

    private static Color getTypeColor(BudgetType type) {
        return type == BudgetType.INCOME ? UIColors.INCOME : UIColors.EXPENSE;
    }

    /**
     * 지출이 예산을 넘었는지 여부. 수입은 예산을 넘어도 경고할 일이 아니다.
     */
    private static boolean isOverBudget(BudgetProgressDto item) {
        return item.getType() == BudgetType.EXPENSE && item.getActualAmount() > item.getBudgetedAmount();
    }

    private static String formatRate(BudgetProgressDto item) {
        if (item.getBudgetedAmount() <= 0) {
            return "-";
        }
        long rate = Math.round(item.getProgressRate());
        return rate > MAX_RATE ? MAX_RATE + "%" : rate + "%";
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
        return ITEM_HEIGHT;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
        return orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
