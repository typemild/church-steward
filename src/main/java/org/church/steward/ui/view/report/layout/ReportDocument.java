package org.church.steward.ui.view.report.layout;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

/**
 * 배치가 끝난 보고서. 쪽마다 그릴 요소와 그 자리가 확정되어 있어, 미리보기·인쇄·PDF 어디에 그려도 같은 결과가 나온다.
 */
public final class ReportDocument {

    private final String title;
    private final List<List<ReportElement>> pages;

    ReportDocument(String title, List<List<ReportElement>> pages) {
        this.title = title;
        this.pages = pages.stream()
                .map(List::copyOf)
                .toList();
    }

    /**
     * 보고서 제목. 인쇄 작업 이름과 PDF 파일 이름에 쓴다.
     */
    public String getTitle() {
        return title;
    }

    public int getPageCount() {
        return pages.size();
    }

    /**
     * 한 쪽에 그릴 요소들을 그리는 순서대로 반환한다.
     */
    public List<ReportElement> getElements(int pageIndex) {
        return pages.get(pageIndex);
    }

    /**
     * 한 쪽을 그린다. 그래픽 컨텍스트는 쪽의 왼쪽 위가 원점이고 1단위가 1포인트가 되도록 맞춰져 있어야 한다.
     */
    public void paintPage(Graphics2D g, int pageIndex) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            // 글자 폭을 잴 때와 같은 기준으로 그려야 배치할 때 계산한 자리와 어긋나지 않는다.
            g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            for (ReportElement element : pages.get(pageIndex)) {
                element.paint(g2);
            }
        } finally {
            g2.dispose();
        }
    }
}
