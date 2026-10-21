package org.church.steward.ui.view.report.layout;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.church.steward.ui.util.UIColors;

/**
 * 보고서의 내용을 위에서 아래로 배치해 나가는 작업대.
 * 현재 쪽과 다음 내용을 놓을 세로 위치를 기억하며, 공간이 모자라면 새 쪽으로 넘긴다.
 * 배치를 마치면 {@link #finish()}로 쪽 번호를 붙인 {@link ReportDocument}를 얻는다.
 */
public final class ReportCanvas {

    /** 쪽 번호를 적는 자리의 높이. 본문은 이 위까지만 쓴다. */
    private static final double FOOTER_HEIGHT = ReportPaper.mm(5);

    /** 절 제목과 그 아래 내용 사이의 간격 */
    private static final double SECTION_GAP = 2;

    private final String title;
    private final List<List<ReportElement>> pages = new ArrayList<>();
    private List<ReportElement> page;
    private int pageIndex;
    private double y;

    /**
     * @param title 보고서 제목. 인쇄 작업 이름과 PDF 파일 이름에 쓴다.
     */
    public ReportCanvas(String title) {
        this.title = title;
        setPosition(0, ReportPaper.MARGIN);
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
     * 현재 쪽의 남은 공간에 주어진 높이의 내용이 들어가는지 여부
     */
    public boolean fits(double height) {
        // 소수 계산의 오차 때문에 꼭 맞는 내용이 다음 쪽으로 넘어가지 않게 한다.
        return y + height <= getBottom() + 0.001;
    }

    /**
     * 다음 쪽의 맨 위로 이동한다. 다음 쪽이 아직 없으면 새로 만든다.
     */
    public void newPage() {
        setPosition(getPageIndex() + 1, ReportPaper.MARGIN);
    }

    /**
     * 현재 쪽의 순번(0부터 시작)
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * 지정한 쪽의 지정한 세로 위치로 이동한다. 그 쪽이 아직 없으면 거기까지 새로 만든다.
     * 좌우로 나란히 놓은 단을 각각 따로 배치할 때, 한 단을 마친 뒤 다른 단의 시작 위치로 돌아오는 데 쓴다.
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
     * @param x 제목의 왼쪽 끝
     */
    public void section(String text, float fontSize, double x) {
        Font font = ReportFonts.bold(fontSize);
        text(text, font, ReportAlign.LEFT, x, y + ReportText.ascent(font));
        y += ReportText.lineHeight(font) + SECTION_GAP;
    }

    /**
     * 현재 쪽에 한 줄의 글자를 놓는다.
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
     * 현재 위치에 가운데 정렬한 보고서 제목을 놓고 그 아래로 이동한다.
     */
    public void title(String text) {
        title(text, ReportFonts.SIZE_TITLE);
    }

    /**
     * 글자 크기를 지정해 보고서 제목을 놓는다.
     */
    public void title(String text, float fontSize) {
        Font font = ReportFonts.bold(fontSize);
        text(text, font, ReportAlign.CENTER, (getLeft() + getRight()) / 2, y + ReportText.ascent(font));
        y += ReportText.lineHeight(font);
    }

    /**
     * 배치를 마치고 모든 쪽의 아래 가운데에 "현재 쪽 / 전체 쪽"을 적는다.
     */
    public ReportDocument finish() {
        Font font = ReportFonts.regular(ReportFonts.SIZE_PAGE_NUMBER);
        double baseline = ReportPaper.HEIGHT - ReportPaper.MARGIN;
        for (int i = 0; i < pages.size(); i++) {
            page = pages.get(i);
            text((i + 1) + " / " + pages.size(), font, ReportAlign.CENTER, ReportPaper.WIDTH / 2, baseline,
                    UIColors.REPORT_TEXT_SUB);
        }
        return new ReportDocument(title, pages);
    }
}
