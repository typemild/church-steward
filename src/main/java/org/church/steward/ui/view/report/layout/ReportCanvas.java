package org.church.steward.ui.view.report.layout;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.ui.util.UIColors;

/**
 * 보고서의 내용을 위에서 아래로 배치해 나가는 작업대. 현재 페이지와 다음 내용을 놓을 세로 위치를 기억하며, 공간이 모자라면 새 페이지로
 * 넘긴다. 배치를 마치면 {@link #finish()}로 페이지 번호를 붙인 {@link ReportDocument}를 얻는다.
 */
public final class ReportCanvas {

    /** 페이지 번호를 적는 자리의 높이. 본문은 이 위까지만 쓴다. */
    private static final double FOOTER_HEIGHT = ReportPaper.mm(5);

    /** 절 제목과 그 아래 내용 사이의 간격 */
    private static final double SECTION_GAP = 2;

    private final String title;
    private final ReportStyle style;
    private final List<List<ReportElement>> pages = new ArrayList<>();
    private List<ReportElement> page;
    private int pageIndex;
    private double y;
    /** 지금까지 놓은 글자 중 가장 작은 글자의 크기(페이지 번호 제외). 글자 크기를 조정할 수 있는 범위를 정하는 데 쓴다. */
    private float smallestFontSize = Float.MAX_VALUE;

    /**
     * @param title 보고서 제목. 인쇄 작업 이름과 PDF 파일 이름에 쓴다.
     * @param style 글꼴, 글자 크기, 행 간격
     */
    public ReportCanvas(String title, ReportStyle style) {
        this.title = title;
        this.style = style;
        setPosition(0, ReportPaper.MARGIN);
    }

    public ReportStyle getStyle() {
        return style;
    }

    public double getLeft() {
        return ReportPaper.MARGIN;
    }

    public double getRight() {
        return ReportPaper.WIDTH - ReportPaper.MARGIN;
    }

    public double getContentWidth() {
        return getRight() - getLeft();
    }

    /**
     * 본문을 쓸 수 있는 가장 아래 위치
     */
    public double getBottom() {
        return ReportPaper.HEIGHT - ReportPaper.MARGIN - FOOTER_HEIGHT;
    }

    /**
     * 다음 내용을 놓을 세로 위치
     */
    public double getY() {
        return y;
    }

    public void moveDown(double distance) {
        y += distance;
    }

    /**
     * 현재 페이지의 남은 공간에 주어진 높이의 내용이 들어가는지 여부
     */
    public boolean fits(double height) {
        // 소수 계산의 오차 때문에 꼭 맞는 내용이 다음 페이지로 넘어가지 않게 한다.
        return y + height <= getBottom() + 0.001;
    }

    /**
     * 다음 페이지의 맨 위로 이동한다. 다음 페이지가 아직 없으면 새로 만든다.
     */
    public void newPage() {
        setPosition(getPageIndex() + 1, ReportPaper.MARGIN);
    }

    /**
     * 현재 페이지의 순번(0부터 시작)
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * 지정한 페이지의 지정한 세로 위치로 이동한다. 그 페이지가 아직 없으면 거기까지 새로 만든다. 좌우로 나란히 놓은 단을 각각 따로 배치할
     * 때, 한 단을 마친 뒤 다른 단의 시작 위치로 돌아오는 데 쓴다.
     */
    public void setPosition(int pageIndex, double y) {
        while (pages.size() <= pageIndex) {
            pages.add(new ArrayList<>());
        }
        this.pageIndex = pageIndex;
        this.page = pages.get(pageIndex);
        this.y = y;
    }

    /**
     * 현재 위치에 굵은 글씨의 절 제목을 놓고 그 아래로 이동한다.
     *
     * @param baseSize 글자의 기본 크기. 서식의 글자 크기 조정이 적용된다.
     * @param x        제목의 왼쪽 끝
     */
    public void section(String text, float baseSize, double x) {
        Font font = style.bold(baseSize);
        text(text, font, ReportAlign.LEFT, x, y + ReportText.ascent(font));
        y += sectionHeight(baseSize);
    }

    /**
     * {@link #section}이 차지하는 높이
     */
    public double sectionHeight(float baseSize) {
        return ReportText.lineHeight(style.bold(baseSize)) + SECTION_GAP;
    }

    /**
     * 현재 페이지에 한 줄의 글자를 놓는다.
     *
     * @param anchorX  정렬의 기준이 되는 가로 위치. 왼쪽 정렬이면 글자의 왼쪽 끝, 가운데 정렬이면 중심, 오른쪽 정렬이면 오른쪽
     *                 끝
     * @param baseline 글자의 기준선
     */
    public void text(String text, Font font, ReportAlign align, double anchorX, double baseline, Color color) {
        if (text == null || text.isEmpty()) {
            return;
        }
        double x = switch (align) {
        case LEFT -> anchorX;
        case CENTER -> anchorX - ReportText.width(text, font) / 2;
        case RIGHT -> anchorX - ReportText.width(text, font);
        };
        page.add(new ReportElement.Text(text, font, x, baseline, color));
        smallestFontSize = Math.min(smallestFontSize, font.getSize2D());
    }

    public void text(String text, Font font, ReportAlign align, double anchorX, double baseline) {
        text(text, font, align, anchorX, baseline, UIColors.REPORT_TEXT);
    }

    public void line(double x1, double y1, double x2, double y2, double width, Color color) {
        page.add(new ReportElement.Line(x1, y1, x2, y2, width, color));
    }

    public void fillRect(double x, double y, double width, double height, Color fill) {
        page.add(new ReportElement.Rect(x, y, width, height, fill, null, 0));
    }

    public void strokeRect(double x, double y, double width, double height, double strokeWidth, Color stroke) {
        page.add(new ReportElement.Rect(x, y, width, height, null, stroke, strokeWidth));
    }

    /**
     * 글자 크기를 지정해 가운데 정렬한 보고서 제목을 놓고 그 아래로 이동한다.
     *
     * @param baseSize 글자의 기본 크기. 서식의 글자 크기 조정이 적용된다.
     */
    public void title(String text, float baseSize) {
        Font font = style.bold(baseSize);
        text(text, font, ReportAlign.CENTER, (getLeft() + getRight()) / 2, y + ReportText.ascent(font));
        y += ReportText.lineHeight(font);
    }

    /**
     * 배치를 마치고 모든 페이지의 아래 가운데에 "현재 페이지 / 전체 페이지"를 적는다. 페이지 번호는 글자 크기 조정의 대상이 아니므로 가장
     * 작은 글자를 셀 때 넣지 않는다.
     */
    public ReportDocument finish() {
        // 글자가 하나도 없는 문서는 없지만, 있더라도 범위 계산이 깨지지 않게 보고서의 기본 글자 크기로 본다.
        float smallest = smallestFontSize == Float.MAX_VALUE ? style.size(ReportFonts.SIZE_BODY) : smallestFontSize;
        Font font = style.fixedRegular(ReportFonts.SIZE_PAGE_NUMBER);
        double baseline = ReportPaper.HEIGHT - ReportPaper.MARGIN;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.get(i);
            text((i + 1) + " / " + pages.size(), font, ReportAlign.CENTER, ReportPaper.WIDTH / 2, baseline,
                    UIColors.REPORT_TEXT_SUB);
        }
        return new ReportDocument(title, pages, style, smallest);
    }
}
