package org.church.steward.ui.view.report.output;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterAbortException;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportPaper;

/**
 * 보고서를 프린터로 인쇄한다.
 * 인쇄 대화상자는 EDT에서 {@link #showDialog}로 띄우고, 시간이 걸리는 실제 인쇄는 백그라운드에서
 * {@link #print}로 한다.
 */
public final class ReportPrinter {

    /** 좌표를 비교할 때 소수 계산의 오차로 보는 범위 */
    private static final double TOLERANCE = 0.5;

    private ReportPrinter() {
        // 인스턴스화 방지
    }

    /**
     * 운영체제의 인쇄 대화상자를 띄운다.
     *
     * @return 인쇄할 준비가 된 작업. 사용자가 취소했으면 null
     * @throws IllegalStateException 사용할 수 있는 프린터가 없는 경우
     */
    public static PrinterJob showDialog(ReportDocument document) {
        PrinterJob job = PrinterJob.getPrinterJob();
        if (job.getPrintService() == null) {
            throw new IllegalStateException("사용할 수 있는 프린터가 없습니다.");
        }
        job.setJobName(document.getTitle());

        // 여백은 보고서가 직접 두므로 용지 전체를 인쇄 영역으로 요청한다. 프린터가 인쇄할 수 없는 가장자리는 validatePage가 빼 준다.
        Paper paper = new Paper();
        paper.setSize(ReportPaper.WIDTH, ReportPaper.HEIGHT);
        paper.setImageableArea(0, 0, ReportPaper.WIDTH, ReportPaper.HEIGHT);
        PageFormat format = job.defaultPage();
        format.setOrientation(PageFormat.PORTRAIT);
        format.setPaper(paper);
        format = job.validatePage(format);

        Book book = new Book();
        book.append(new DocumentPrintable(document), format, document.getPageCount());
        job.setPageable(book);

        return job.printDialog() ? job : null;
    }

    /**
     * 인쇄를 실행한다. 인쇄가 끝날 때까지 기다리므로 EDT에서 호출하면 안 된다.
     *
     * @return 인쇄했으면 true, 인쇄 도중 사용자가 취소했으면 false
     * @throws IllegalStateException 인쇄에 실패한 경우
     */
    public static boolean print(PrinterJob job) {
        try {
            job.print();
            return true;
        } catch (PrinterAbortException e) {
            return false;
        } catch (PrinterException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    /**
     * 보고서의 페이지를 용지에 맞춰 그린다.
     * 용지가 A4이고 프린터가 보고서의 여백 안쪽을 모두 인쇄할 수 있으면 실제 크기 그대로 그린다.
     * 그렇지 않으면 내용이 잘리지 않도록 페이지 전체를 인쇄할 수 있는 영역 안으로 같은 비율로 줄인다.
     */
    private static final class DocumentPrintable implements Printable {

        private final ReportDocument document;

        private DocumentPrintable(ReportDocument document) {
            this.document = document;
        }

        @Override
        public int print(Graphics graphics, PageFormat format, int pageIndex) {
            if (pageIndex < 0 || pageIndex >= document.getPageCount()) {
                return NO_SUCH_PAGE;
            }

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                double scale = Math.min(format.getWidth() / ReportPaper.WIDTH, format.getHeight() / ReportPaper.HEIGHT);
                double offsetX = (format.getWidth() - ReportPaper.WIDTH * scale) / 2;
                double offsetY = (format.getHeight() - ReportPaper.HEIGHT * scale) / 2;

                if (!isContentPrintable(format, scale, offsetX, offsetY)) {
                    scale = Math.min(format.getImageableWidth() / ReportPaper.WIDTH,
                            format.getImageableHeight() / ReportPaper.HEIGHT);
                    offsetX = format.getImageableX() + (format.getImageableWidth() - ReportPaper.WIDTH * scale) / 2;
                    offsetY = format.getImageableY() + (format.getImageableHeight() - ReportPaper.HEIGHT * scale) / 2;
                }

                g.translate(offsetX, offsetY);
                g.scale(scale, scale);
                document.paintPage(g, pageIndex);
            } finally {
                g.dispose();
            }
            return PAGE_EXISTS;
        }

        /**
         * 페이지를 용지에 맞춰 놓았을 때 보고서의 여백 안쪽(내용이 있는 영역)이 프린터의 인쇄 가능 영역 안에 들어가는지 여부
         */
        private boolean isContentPrintable(PageFormat format, double scale, double offsetX, double offsetY) {
            double margin = ReportPaper.MARGIN * scale;
            double left = offsetX + margin;
            double top = offsetY + margin;
            double right = offsetX + ReportPaper.WIDTH * scale - margin;
            double bottom = offsetY + ReportPaper.HEIGHT * scale - margin;

            return left >= format.getImageableX() - TOLERANCE
                    && top >= format.getImageableY() - TOLERANCE
                    && right <= format.getImageableX() + format.getImageableWidth() + TOLERANCE
                    && bottom <= format.getImageableY() + format.getImageableHeight() + TOLERANCE;
        }
    }
}
