package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.util.NumberUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 검색된 수입/지출의 총합 표시 영역. 둥근 카드 하나를 수입, 지출, 차액 세 칸으로 나누고, 칸마다 항목명 아래에 금액을 크게 보여준다.
 */
public class FinancialSummaryPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int ARC = 12;
    private static final int PADDING_VERTICAL = 9;
    private static final int PADDING_HORIZONTAL = 16;
    private static final int CAPTION_GAP = 3;
    private static final int ICON_TEXT_GAP = 6;

    private final FieldLabel incomeLabel = new FieldLabel("", ComponentSize.LARGE, true);
    private final FieldLabel expenseLabel = new FieldLabel("", ComponentSize.LARGE, true);
    private final FieldLabel differenceLabel = new FieldLabel("", ComponentSize.LARGE, true);

    public FinancialSummaryPanel() {
        setLayout(new GridLayout(1, 3));
        setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
        setBorder(BorderFactory.createEmptyBorder(PADDING_VERTICAL, 1, PADDING_VERTICAL, 1));

        add(createCell("수입", FontAwesomeSolid.PLUS_CIRCLE, UIColors.INCOME, incomeLabel, false));
        add(createCell("지출", FontAwesomeSolid.MINUS_CIRCLE, UIColors.EXPENSE, expenseLabel, true));
        add(createCell("차액", FontAwesomeSolid.EQUALS, UIColors.TEXT_SUB, differenceLabel, true));

        update(List.of());
    }

    /**
     * 항목명과 금액을 위아래로 배치한 칸을 만든다.
     *
     * @param hasDivider 칸의 왼쪽에 구분선을 그릴지 여부
     */
    private JPanel createCell(String caption, FontAwesomeSolid icon, Color iconColor, FieldLabel amountLabel,
            boolean hasDivider) {
        SubFieldLabel captionLabel = new SubFieldLabel(caption, ComponentSize.SMALL);
        captionLabel.setIcon(FontIcon.of(icon, UIFonts.FONT_SIZE_SMALL, iconColor));
        captionLabel.setIconTextGap(ICON_TEXT_GAP);

        JPanel cell = new JPanel(new BorderLayout(0, CAPTION_GAP));
        cell.setOpaque(false);
        // 구분선이 없는 칸도 구분선 두께만큼 여백을 더해 칸마다 내용의 시작 위치를 맞춘다.
        cell.setBorder(BorderFactory.createCompoundBorder(
                hasDivider
                        ? BorderFactory.createMatteBorder(0, 1, 0, 0, UIColors.BORDER)
                        : BorderFactory.createEmptyBorder(0, 1, 0, 0),
                BorderFactory.createEmptyBorder(0, PADDING_HORIZONTAL, 0, PADDING_HORIZONTAL)));
        cell.add(captionLabel, BorderLayout.NORTH);
        cell.add(amountLabel, BorderLayout.CENTER);
        return cell;
    }

    /**
     * 수입/지출 내역으로 수입 합계, 지출 합계, 차액을 다시 계산해 표시한다.
     */
    public void update(List<FinancialRecordResponseDto> records) {
        long incomeTotal = 0;
        long expenseTotal = 0;
        for (FinancialRecordResponseDto record : records) {
            if (record.getType() == FinancialRecordType.INCOME) {
                incomeTotal += record.getAmount();
            } else {
                expenseTotal += record.getAmount();
            }
        }
        long difference = incomeTotal - expenseTotal;

        incomeLabel.setText(NumberUtils.toKoreanAmount(incomeTotal));
        expenseLabel.setText(NumberUtils.toKoreanAmount(expenseTotal));
        differenceLabel.setText(FinancialRecordFormats.signedAmount(difference));
        differenceLabel.setForeground(getDifferenceColor(difference));
    }

    /**
     * 차액이 남으면 수입 색, 모자라면 지출 색, 0이면 기본 글자색을 반환한다.
     */
    private Color getDifferenceColor(long difference) {
        if (difference > 0) {
            return UIColors.INCOME;
        }
        if (difference < 0) {
            return UIColors.EXPENSE;
        }
        return UIColors.TEXT_MAIN;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIColors.CARD_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.setColor(UIColors.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }
}
