package org.church.steward.ui.view.dashboard;

import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 월별 수입과 지출을 나란히 세운 막대 차트. 마우스를 올린 달은 배경을 강조하고 툴팁으로 정확한 금액을 보여준다.
 */
class MonthlyTrendChart extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final Font AXIS_FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private static final String EMPTY_MESSAGE = "등록된 수입/지출이 없습니다.";
    private static final String MONTH_SUFFIX = "월";

    private static final int MAX_BAR_WIDTH = 12;
    private static final int MIN_BAR_WIDTH = 2;
    private static final int BAR_GAP = 2;
    private static final int BAR_ARC = 8;
    /** 달과 달 사이에 비워 두는 최소 폭 */
    private static final int GROUP_PADDING = 6;
    private static final int AXIS_LABEL_GAP = 6;
    /** 눈금 간격을 정할 때 목표로 삼는 눈금 구간 수 */
    private static final int TARGET_TICKS = 4;
    private static final int LEGEND_SWATCH_SIZE = 8;

    private List<MonthlyTrendDto> data = List.of();
    /** 가로축의 달 이름에 연도를 붙일지 여부. 기간이 열두 달을 넘어 같은 달이 두 번 나올 때만 붙인다. */
    private boolean withYear;
    private int hoveredIndex = -1;

    /** 마지막으로 그린 차트 영역의 왼쪽 끝과 한 달의 폭. 마우스 위치를 달로 바꿀 때 쓴다. */
    private int plotLeft;
    private double slotWidth;

    MonthlyTrendChart() {
        setOpaque(false);
        ToolTipManager.sharedInstance()
                .registerComponent(this);

        MouseAdapter hoverListener = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                setHoveredIndex(indexAt(e.getX()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredIndex(-1);
            }
        };
        addMouseMotionListener(hoverListener);
        addMouseListener(hoverListener);
    }

    /**
     * @param data 회계 연도 기간의 달 순서대로 놓인 월별 합계
     */
    void setData(List<MonthlyTrendDto> data) {
        this.data = List.copyOf(data);
        this.withYear = needsYear(this.data);
        hoveredIndex = -1;
        repaint();
    }

    /**
     * 회계 연도는 두 해에 걸칠 수 있으므로(예: 12월 ~ 이듬해 11월) 연도가 달라도 달 이름만 적고, 같은 달이 두 번 나올 때만 연도를
     * 붙여 구분한다.
     */
    private static boolean needsYear(List<MonthlyTrendDto> data) {
        Set<Integer> monthValues = new HashSet<>();
        for (MonthlyTrendDto month : data) {
            if (!monthValues.add(yearMonthOf(month).getMonthValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 수입과 지출의 막대 색을 알려주는 범례를 만든다.
     */
    static JComponent createLegend() {
        JPanel legend = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        legend.setOpaque(false);
        legend.add(createLegendItem("수입", UIColors.INCOME));
        legend.add(createLegendItem("지출", UIColors.EXPENSE));
        return legend;
    }

    private static SubFieldLabel createLegendItem(String text, Color color) {
        SubFieldLabel label = new SubFieldLabel(text, ComponentSize.SMALL);
        label.setIcon(new SwatchIcon(color));
        label.setIconTextGap(5);
        return label;
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        int index = indexAt(e.getX());
        if (index < 0) {
            return null;
        }

        MonthlyTrendDto month = data.get(index);
        YearMonth yearMonth = yearMonthOf(month);
        // 회계 연도가 두 해에 걸칠 수 있으므로 툴팁에는 항상 연도를 함께 적는다.
        String title = yearMonth.getYear() + "년 " + yearMonth.getMonthValue() + MONTH_SUFFIX;
        // 툴팁에서 줄을 바꾸려면 HTML을 써야 한다.
        return String.format("<html><b>%s</b><br>수입 %,d원<br>지출 %,d원</html>",
                title, month.getIncome(), month.getExpense());
    }

    private void setHoveredIndex(int index) {
        if (hoveredIndex != index) {
            hoveredIndex = index;
            repaint();
        }
    }

    private int indexAt(int x) {
        if (data.isEmpty() || slotWidth <= 0 || x < plotLeft) {
            return -1;
        }
        int index = (int) ((x - plotLeft) / slotWidth);
        return index < data.size() ? index : -1;
    }

    private static YearMonth yearMonthOf(MonthlyTrendDto month) {
        return YearMonth.parse(month.getYearMonth());
    }

    /**
     * 가로축에 쓸 달 이름. 보통은 "3월"로 적고, 연도를 붙여야 하면 "26.1"처럼 연도 두 자리를 붙인다.
     */
    private String fullLabelOf(YearMonth yearMonth) {
        if (!withYear) {
            return yearMonth.getMonthValue() + MONTH_SUFFIX;
        }
        return String.format("%02d.%d", yearMonth.getYear() % 100, yearMonth.getMonthValue());
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = DashboardGraphics.create(g);
        try {
            g2.setFont(AXIS_FONT);
            FontMetrics metrics = g2.getFontMetrics();

            long max = 0;
            for (MonthlyTrendDto month : data) {
                max = Math.max(max, Math.max(month.getIncome(), month.getExpense()));
            }
            if (max <= 0) {
                slotWidth = 0;
                paintEmptyMessage(g2, metrics);
                return;
            }

            long step = niceStep(max);
            long top = (max + step - 1) / step * step;

            int axisWidth = 0;
            for (long value = 0; value <= top; value += step) {
                axisWidth = Math.max(axisWidth, metrics.stringWidth(DashboardGraphics.compactAmount(value)));
            }

            // 맨 위 눈금의 글자가 잘리지 않도록 글자 높이의 절반만큼 위를 비운다.
            int plotTop = metrics.getAscent() / 2 + 1;
            int plotBottom = getHeight() - metrics.getHeight() - 4;
            int plotHeight = plotBottom - plotTop;
            plotLeft = axisWidth + AXIS_LABEL_GAP;
            int plotWidth = getWidth() - plotLeft;
            if (plotHeight <= 0 || plotWidth <= 0) {
                slotWidth = 0;
                return;
            }
            slotWidth = (double) plotWidth / data.size();

            if (hoveredIndex >= 0) {
                g2.setColor(UIColors.BACKGROUND);
                g2.fillRect(plotLeft + (int) Math.round(hoveredIndex * slotWidth), plotTop,
                        (int) Math.round(slotWidth), plotHeight);
            }

            for (long value = 0; value <= top; value += step) {
                int y = plotBottom - (int) Math.round((double) plotHeight * value / top);
                g2.setColor(value == 0 ? UIColors.BORDER : UIColors.CHART_GRID);
                g2.drawLine(plotLeft, y, getWidth() - 1, y);

                String label = DashboardGraphics.compactAmount(value);
                g2.setColor(UIColors.TEXT_SUB);
                g2.drawString(label, axisWidth - metrics.stringWidth(label), y + metrics.getAscent() / 2 - 1);
            }

            int barWidth = (int) ((slotWidth - GROUP_PADDING - BAR_GAP) / 2);
            barWidth = Math.max(MIN_BAR_WIDTH, Math.min(MAX_BAR_WIDTH, barWidth));
            int groupWidth = barWidth * 2 + BAR_GAP;
            // 폭이 좁으면 "월"이나 연도를 떼고 달의 숫자만 쓴다.
            boolean showsFullLabel = true;
            for (MonthlyTrendDto month : data) {
                if (metrics.stringWidth(fullLabelOf(yearMonthOf(month))) + 2 > slotWidth) {
                    showsFullLabel = false;
                    break;
                }
            }

            for (int i = 0; i < data.size(); i++) {
                MonthlyTrendDto month = data.get(i);
                YearMonth yearMonth = yearMonthOf(month);
                int slotLeft = plotLeft + (int) Math.round(i * slotWidth);
                int slotCenter = slotLeft + (int) Math.round(slotWidth / 2);
                int groupLeft = slotCenter - groupWidth / 2;

                paintBar(g2, groupLeft, barWidth, month.getIncome(), top, plotBottom, plotHeight, UIColors.INCOME);
                paintBar(g2, groupLeft + barWidth + BAR_GAP, barWidth, month.getExpense(), top, plotBottom,
                        plotHeight, UIColors.EXPENSE);

                String label = showsFullLabel ? fullLabelOf(yearMonth) : String.valueOf(yearMonth.getMonthValue());
                g2.setColor(i == hoveredIndex ? UIColors.TEXT_MAIN : UIColors.TEXT_SUB);
                g2.drawString(label, slotCenter - metrics.stringWidth(label) / 2,
                        plotBottom + 4 + metrics.getAscent());
            }
        } finally {
            g2.dispose();
        }
    }

    /**
     * 기준선에서 위로 자라는 막대를 그린다. 위쪽 끝만 둥글고 기준선 쪽은 각지게 한다.
     */
    private void paintBar(Graphics2D g2, int x, int width, long value, long top, int plotBottom, int plotHeight,
            Color color) {
        if (value <= 0) {
            return;
        }
        // 값이 아주 작아도 막대가 보이도록 최소 높이를 둔다.
        int height = Math.max(2, (int) Math.round((double) plotHeight * value / top));
        int y = plotBottom - height;
        int arc = Math.min(BAR_ARC, width);

        // 아래쪽 둥근 모서리가 기준선 밑으로 나가도록 길게 그리고 기준선에서 잘라낸다.
        Shape oldClip = g2.getClip();
        g2.clipRect(x, y, width, height);
        g2.setColor(color);
        g2.fillRoundRect(x, y, width, height + arc, arc, arc);
        g2.setClip(oldClip);
    }

    private void paintEmptyMessage(Graphics2D g2, FontMetrics metrics) {
        g2.setColor(UIColors.TEXT_SUB);
        g2.drawString(EMPTY_MESSAGE, (getWidth() - metrics.stringWidth(EMPTY_MESSAGE)) / 2,
                (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent());
    }

    /**
     * 최댓값을 {@value #TARGET_TICKS}개 안팎의 구간으로 나누는, 1·2·5로 시작하는 눈금 간격을 구한다.
     */
    private static long niceStep(long max) {
        double rough = (double) max / TARGET_TICKS;
        double magnitude = Math.pow(10, Math.floor(Math.log10(rough)));
        double normalized = rough / magnitude;

        double nice;
        if (normalized <= 1) {
            nice = 1;
        } else if (normalized <= 2) {
            nice = 2;
        } else if (normalized <= 5) {
            nice = 5;
        } else {
            nice = 10;
        }
        return Math.max(1, Math.round(nice * magnitude));
    }

    /** 범례에서 막대 색을 보여주는 작은 사각형 */
    private static final class SwatchIcon implements Icon {

        private final Color color;

        private SwatchIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = DashboardGraphics.create(g);
            g2.setColor(color);
            g2.fillRoundRect(x, y, LEGEND_SWATCH_SIZE, LEGEND_SWATCH_SIZE, 3, 3);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return LEGEND_SWATCH_SIZE;
        }

        @Override
        public int getIconHeight() {
            return LEGEND_SWATCH_SIZE;
        }
    }
}
