package org.church.steward.ui.view.report.composer;

import java.awt.Font;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.dashboard.dto.MonthlyTrendDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportText;

/**
 * 월별 수입과 지출을 나란히 세운 막대 차트. 대시보드의 월별 차트와 같은 구조다.
 * 흑백으로 인쇄해도 구분되도록 수입은 진한 회색으로 채우고 지출은 테두리만 그린다.
 * 눈금은 어림값으로 적으며, 정확한 금액은 차트 옆의 표가 보여준다.
 */
final class MonthlyTrendChart {

    private static final String EMPTY_MESSAGE = "등록된 수입/지출이 없습니다.";
    private static final String MONTH_SUFFIX = "월";

    private static final double MAX_BAR_WIDTH = 9;
    private static final double MIN_BAR_WIDTH = 1.5;
    private static final double BAR_GAP = 1.5;
    /** 달과 달 사이에 비워 두는 최소 폭 */
    private static final double GROUP_PADDING = 4;
    private static final double AXIS_LABEL_GAP = 4;
    /** 눈금 간격을 정할 때 목표로 삼는 눈금 구간 수 */
    private static final int TARGET_TICKS = 4;
    private static final double LEGEND_SWATCH_SIZE = 6;
    private static final double LEGEND_GAP = 4;

    private static final double AXIS_LINE = 0.5;
    private static final double GRID_LINE = 0.3;
    private static final double BAR_LINE = 0.5;

    private MonthlyTrendChart() {
        // 인스턴스화 방지
    }

    /**
     * 현재 쪽의 정해진 자리에 차트를 그린다. 작업대의 위치는 바꾸지 않는다.
     *
     * @param data 회계 연도 기간의 달 순서대로 놓인 월별 합계
     */
    static void draw(ReportCanvas canvas, double left, double top, double width, double height,
            List<MonthlyTrendDto> data, float fontSize) {
        Font font = ReportFonts.regular(fontSize);
        double lineHeight = ReportText.lineHeight(font);
        double ascent = ReportText.ascent(font);
        double right = left + width;

        long max = 0;
        for (MonthlyTrendDto month : data) {
            max = Math.max(max, Math.max(month.getIncome(), month.getExpense()));
        }
        if (max <= 0) {
            canvas.text(EMPTY_MESSAGE, font, ReportAlign.CENTER, left + width / 2, top + height / 2,
                    UIColors.REPORT_TEXT_SUB);
            return;
        }

        drawLegend(canvas, right, top, font);

        long step = niceStep(max);
        long axisTop = (max + step - 1) / step * step;

        double axisWidth = 0;
        for (long value = 0; value <= axisTop; value += step) {
            axisWidth = Math.max(axisWidth, ReportText.width(ReportFormats.compactAmount(value), font));
        }

        // 범례 아래에서 시작하고, 맨 위 눈금의 글자가 범례와 겹치지 않도록 글자 높이의 절반만큼 더 내린다.
        double plotTop = top + lineHeight + LEGEND_GAP + ascent / 2;
        double plotBottom = top + height - lineHeight - 2;
        double plotLeft = left + axisWidth + AXIS_LABEL_GAP;
        double plotHeight = plotBottom - plotTop;
        double plotWidth = right - plotLeft;
        if (plotHeight <= 0 || plotWidth <= 0) {
            return;
        }

        for (long value = 0; value <= axisTop; value += step) {
            double y = plotBottom - plotHeight * value / axisTop;
            if (value == 0) {
                canvas.line(plotLeft, y, right, y, AXIS_LINE, UIColors.REPORT_LINE);
            } else {
                canvas.line(plotLeft, y, right, y, GRID_LINE, UIColors.REPORT_GRID);
            }
            canvas.text(ReportFormats.compactAmount(value), font, ReportAlign.RIGHT, left + axisWidth,
                    y + ascent / 2 - 1, UIColors.REPORT_TEXT_SUB);
        }

        double slotWidth = plotWidth / data.size();
        double barWidth = Math.max(MIN_BAR_WIDTH, Math.min(MAX_BAR_WIDTH, (slotWidth - GROUP_PADDING - BAR_GAP) / 2));
        double groupWidth = barWidth * 2 + BAR_GAP;

        // 폭이 좁으면 "월"이나 연도를 떼고 달의 숫자만 쓴다.
        boolean withYear = needsYear(data);
        boolean showsFullLabel = true;
        for (MonthlyTrendDto month : data) {
            if (ReportText.width(fullLabelOf(YearMonth.parse(month.getYearMonth()), withYear), font) + 2 > slotWidth) {
                showsFullLabel = false;
                break;
            }
        }

        for (int i = 0; i < data.size(); i++) {
            MonthlyTrendDto month = data.get(i);
            YearMonth yearMonth = YearMonth.parse(month.getYearMonth());
            double slotCenter = plotLeft + slotWidth * (i + 0.5);
            double groupLeft = slotCenter - groupWidth / 2;

            double incomeHeight = barHeight(month.getIncome(), axisTop, plotHeight);
            if (incomeHeight > 0) {
                canvas.fillRect(groupLeft, plotBottom - incomeHeight, barWidth, incomeHeight, UIColors.REPORT_BAR);
            }
            double expenseHeight = barHeight(month.getExpense(), axisTop, plotHeight);
            if (expenseHeight > 0) {
                // 테두리가 막대의 폭과 높이 밖으로 나가지 않도록 선 굵기의 절반만큼 안쪽에 그린다.
                canvas.strokeRect(groupLeft + barWidth + BAR_GAP + BAR_LINE / 2,
                        plotBottom - expenseHeight + BAR_LINE / 2, barWidth - BAR_LINE,
                        Math.max(0, expenseHeight - BAR_LINE / 2), BAR_LINE, UIColors.REPORT_LINE);
            }

            String label = showsFullLabel ? fullLabelOf(yearMonth, withYear)
                    : String.valueOf(yearMonth.getMonthValue());
            canvas.text(label, font, ReportAlign.CENTER, slotCenter, plotBottom + 2 + ascent,
                    UIColors.REPORT_TEXT_SUB);
        }
    }

    /**
     * 차트의 오른쪽 위에 "■ 수입 □ 지출" 범례를 그린다.
     */
    private static void drawLegend(ReportCanvas canvas, double right, double top, Font font) {
        double baseline = top + ReportText.ascent(font);
        double swatchTop = top + (ReportText.lineHeight(font) - LEGEND_SWATCH_SIZE) / 2;

        double x = right - ReportText.width("지출", font);
        canvas.text("지출", font, ReportAlign.LEFT, x, baseline);
        x -= LEGEND_SWATCH_SIZE + 3;
        canvas.strokeRect(x + BAR_LINE / 2, swatchTop + BAR_LINE / 2, LEGEND_SWATCH_SIZE - BAR_LINE,
                LEGEND_SWATCH_SIZE - BAR_LINE, BAR_LINE, UIColors.REPORT_LINE);

        x -= 8 + ReportText.width("수입", font);
        canvas.text("수입", font, ReportAlign.LEFT, x, baseline);
        x -= LEGEND_SWATCH_SIZE + 3;
        canvas.fillRect(x, swatchTop, LEGEND_SWATCH_SIZE, LEGEND_SWATCH_SIZE, UIColors.REPORT_BAR);
    }

    /**
     * 값이 아주 작아도 막대가 보이도록 최소 높이를 둔다. 값이 없으면 0
     */
    private static double barHeight(long value, long axisTop, double plotHeight) {
        if (value <= 0) {
            return 0;
        }
        return Math.max(1.5, plotHeight * value / axisTop);
    }

    /**
     * 달 이름에 연도를 붙여야 하는지 판단한다. 회계 연도는 두 해에 걸칠 수 있으므로(예: 12월 ~ 이듬해 11월) 연도가 달라도 달 이름만
     * 적고,
     * 기간이 열두 달을 넘어 같은 달이 두 번 나올 때만 연도를 붙여 구분한다.
     */
    static boolean needsYear(List<MonthlyTrendDto> data) {
        Set<Integer> monthValues = new HashSet<>();
        for (MonthlyTrendDto month : data) {
            if (!monthValues.add(YearMonth.parse(month.getYearMonth())
                    .getMonthValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 가로축에 쓸 달 이름. 보통은 "3월"로 적고, 연도를 붙여야 하면 "26.1"처럼 연도 두 자리를 붙인다.
     *
     * @param withYear {@link #needsYear}의 결과
     */
    static String fullLabelOf(YearMonth yearMonth, boolean withYear) {
        if (!withYear) {
            return yearMonth.getMonthValue() + MONTH_SUFFIX;
        }
        return String.format("%02d.%d", yearMonth.getYear() % 100, yearMonth.getMonthValue());
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
}
