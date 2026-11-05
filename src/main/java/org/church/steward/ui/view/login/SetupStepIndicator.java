package org.church.steward.ui.view.login;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;

import javax.swing.JComponent;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 초기 설정 화면 맨 위의 단계 표시. 단계마다 화살표 모양의 칸(왼쪽은 파이고 오른쪽은 뾰족하다)을 가로로 이어 붙이고, 칸 안에 동그라미
 * 번호와 단계 이름을 적는다.
 * <p>
 * 지금 단계는 Primary 색 칸에 흰 글자, 지난 단계는 연한 Primary 색 칸에 본문 색 글자, 남은 단계는 흰 칸에 회색 테두리와
 * 회색 글자다. 칸은 받은 폭을 똑같이 나눠 가지며, 이름이 칸에 들어가지 않으면 글자를 한 단계 작게 쓴다.
 */
class SetupStepIndicator extends JComponent {

    private static final long serialVersionUID = 1L;

    private static final int HEIGHT = 34;
    /** 화살표 끝(오른쪽)과 파인 곳(왼쪽)의 깊이 */
    private static final int TIP = 11;
    /** 칸 사이의 간격 */
    private static final int GAP = 3;
    private static final int CIRCLE_SIZE = 18;
    /** 동그라미 번호와 이름 사이의 간격 */
    private static final int CIRCLE_GAP = 6;
    private static final float BORDER_WIDTH = 1f;

    private static final Font NUMBER_FONT = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_SMALL);

    private final String[] stepNames;
    private int currentStep = 0;

    SetupStepIndicator(String... stepNames) {
        this.stepNames = stepNames;
        setOpaque(false);
    }

    /**
     * @param currentStep 지금 단계(0부터)
     */
    void setCurrentStep(int currentStep) {
        this.currentStep = currentStep;
        repaint();
    }

    /**
     * 폭은 부모가 정하도록 0으로 알린다.
     */
    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, HEIGHT);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int count = stepNames.length;
            // 이웃한 칸은 화살표 끝과 파인 곳이 GAP만큼 떨어져 나란하도록 (TIP - GAP)만큼 겹쳐 놓는다.
            int overlap = TIP - GAP;
            double segmentWidth = (getWidth() - 1 + (count - 1) * overlap) / (double) count;
            Font nameFont = chooseNameFont(g2, segmentWidth);

            for (int i = 0; i < count; i++) {
                double x = i * (segmentWidth - overlap);
                paintSegment(g2, i, x, segmentWidth, nameFont);
            }
        } finally {
            g2.dispose();
        }
    }

    private void paintSegment(Graphics2D g2, int index, double x, double width, Font nameFont) {
        double bottom = HEIGHT - 1;
        double middle = bottom / 2;
        Path2D.Double shape = new Path2D.Double();
        shape.moveTo(x, 0);
        shape.lineTo(x + width - TIP, 0);
        shape.lineTo(x + width, middle);
        shape.lineTo(x + width - TIP, bottom);
        shape.lineTo(x, bottom);
        shape.lineTo(x + TIP, middle);
        shape.closePath();

        boolean current = index == currentStep;
        boolean done = index < currentStep;
        if (current) {
            g2.setColor(UIColors.PRIMARY_BACKGROUND);
        } else {
            g2.setColor(done ? UIColors.CHIP_BACKGROUND : UIColors.CARD_BACKGROUND);
        }
        g2.fill(shape);
        if (!current && !done) {
            g2.setColor(UIColors.BORDER);
            g2.setStroke(new BasicStroke(BORDER_WIDTH));
            g2.draw(shape);
        }

        // 동그라미 번호와 이름을 칸의 몸통(파인 곳과 화살표 끝 사이) 가운데에 놓는다.
        Font font = current ? nameFont.deriveFont(Font.BOLD) : nameFont;
        FontMetrics nameMetrics = g2.getFontMetrics(font);
        String name = stepNames[index];
        int contentWidth = CIRCLE_SIZE + CIRCLE_GAP + nameMetrics.stringWidth(name);
        int contentX = (int) Math.round(x + TIP / 2.0 + (width - TIP - contentWidth) / 2);
        int circleY = (HEIGHT - CIRCLE_SIZE) / 2;

        Color circleColor;
        Color numberColor;
        Color nameColor;
        if (current) {
            circleColor = UIColors.PRIMARY_TEXT;
            numberColor = UIColors.PRIMARY_BACKGROUND;
            nameColor = UIColors.PRIMARY_TEXT;
        } else if (done) {
            circleColor = UIColors.PRIMARY_BACKGROUND;
            numberColor = UIColors.PRIMARY_TEXT;
            nameColor = UIColors.TEXT_MAIN;
        } else {
            circleColor = UIColors.BORDER;
            numberColor = UIColors.PRIMARY_TEXT;
            nameColor = UIColors.TEXT_SUB;
        }
        g2.setColor(circleColor);
        g2.fillOval(contentX, circleY, CIRCLE_SIZE, CIRCLE_SIZE);
        drawCentered(g2, String.valueOf(index + 1), NUMBER_FONT, numberColor, contentX + CIRCLE_SIZE / 2,
                HEIGHT / 2);

        g2.setFont(font);
        g2.setColor(nameColor);
        int baseline = (HEIGHT - (nameMetrics.getAscent() + nameMetrics.getDescent())) / 2 + nameMetrics.getAscent();
        g2.drawString(name, contentX + CIRCLE_SIZE + CIRCLE_GAP, baseline);
    }

    /**
     * 가장 긴 이름이 굵은 글자로도 칸의 몸통에 들어가면 기본 크기, 아니면 한 단계 작은 크기를 쓴다. 모든 칸이 같은 크기를 쓴다.
     */
    private Font chooseNameFont(Graphics2D g2, double segmentWidth) {
        Font base = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
        FontMetrics boldMetrics = g2.getFontMetrics(base.deriveFont(Font.BOLD));
        int widest = 0;
        for (String name : stepNames) {
            widest = Math.max(widest, boldMetrics.stringWidth(name));
        }
        // 몸통의 폭에서 양쪽에 조금 여유를 둔다.
        double available = segmentWidth - TIP * 2 - CIRCLE_SIZE - CIRCLE_GAP;
        return widest <= available ? base : new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);
    }

    /**
     * 글자를 주어진 점에 가운데 맞춰 그린다.
     */
    private static void drawCentered(Graphics2D g2, String text, Font font, Color color, int centerX, int centerY) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics metrics = g2.getFontMetrics();
        int x = centerX - metrics.stringWidth(text) / 2;
        int y = centerY - (metrics.getAscent() + metrics.getDescent()) / 2 + metrics.getAscent();
        g2.drawString(text, x, y);
    }
}
