package org.church.steward.ui.view.dashboard;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.util.NumberUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 금액 하나를 크게 보여주는 요약 카드.
 * 위에서부터 항목명, 금액, 보조 정보를 배치한다. 보조 정보는 한 줄 설명이거나 예산 대비 진행 막대다.
 */
class SummaryCard extends DashboardCard {

    private static final long serialVersionUID = 1L;

    private static final Font AMOUNT_FONT = new Font(UIFonts.FONT_FAMILY, Font.BOLD, UIFonts.FONT_SIZE_EXTRA_LARGE);
    private static final int ROW_GAP = 5;
    private static final int METER_GAP = 8;
    private static final int ICON_TEXT_GAP = 6;
    /** 보조 정보가 없을 때도 카드 높이가 변하지 않게 채워 두는 글자 */
    private static final String BLANK = " ";

    private final AutoFitLabel amountLabel = new AutoFitLabel(AMOUNT_FONT, UIFonts.FONT_SIZE_BASE);
    private final SubFieldLabel noteLabel = new SubFieldLabel(BLANK, ComponentSize.SMALL);
    private final SubFieldLabel rateLabel = new SubFieldLabel("", SwingConstants.RIGHT, ComponentSize.SMALL);
    private final ProgressMeter meter;
    private final JPanel footer = new JPanel(new BorderLayout(METER_GAP, 0));

    /**
     * @param accentColor 항목 아이콘, 금액, 진행 막대에 쓰는 색
     */
    SummaryCard(String title, FontAwesomeSolid icon, Color accentColor) {
        super(new BorderLayout(0, ROW_GAP));

        SubFieldLabel titleLabel = new SubFieldLabel(title, ComponentSize.MEDIUM);
        titleLabel.setIcon(FontIcon.of(icon, UIFonts.FONT_SIZE_BASE, accentColor));
        titleLabel.setIconTextGap(ICON_TEXT_GAP);

        amountLabel.setForeground(accentColor);

        meter = new ProgressMeter(accentColor);
        meter.setVisible(false);
        rateLabel.setVisible(false);

        footer.setOpaque(false);
        footer.add(noteLabel, BorderLayout.WEST);
        footer.add(meter, BorderLayout.CENTER);
        footer.add(rateLabel, BorderLayout.EAST);

        add(titleLabel, BorderLayout.NORTH);
        add(amountLabel, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setAmount(0);
    }

    void setAmount(long amount) {
        amountLabel.setText(String.format("%,d원", amount));
        // 금액이 길어 글자가 작아지거나 잘려도 값을 확인할 수 있게 한다.
        amountLabel.setToolTipText(NumberUtils.toKoreanAmount(amount));
    }

    void setAmountColor(Color color) {
        amountLabel.setForeground(color);
    }

    /**
     * 보조 정보로 한 줄 설명을 보여준다. 설명이 없으면 빈 줄로 둔다.
     */
    void setNote(String note) {
        noteLabel.setText(note == null || note.isEmpty() ? BLANK : note);
        meter.setVisible(false);
        rateLabel.setVisible(false);
        footer.setToolTipText(null);
        footer.revalidate();
    }

    /**
     * 보조 정보로 예산 대비 진행 막대를 보여준다. 예산이 없으면 그 사실을 알리는 설명으로 대신한다.
     *
     * @param actual   실제 금액
     * @param budgeted 예산 금액
     */
    void setBudgetProgress(long actual, long budgeted) {
        if (budgeted <= 0) {
            setNote("예산 미설정");
            return;
        }

        double ratio = (double) actual / budgeted;
        noteLabel.setText("예산 대비");
        meter.setRatio(ratio);
        meter.setVisible(true);
        rateLabel.setText(Math.round(ratio * 100) + "%");
        rateLabel.setVisible(true);
        footer.setToolTipText(String.format("예산 %,d원", budgeted));
        footer.revalidate();
    }

    /** 차액 카드의 금액 색. 남으면 차액 색, 모자라면 지출 색이다. */
    static Color balanceColor(long balance) {
        return balance < 0 ? UIColors.EXPENSE : UIColors.BALANCE;
    }
}
