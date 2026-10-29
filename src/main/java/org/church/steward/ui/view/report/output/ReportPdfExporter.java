package org.church.steward.ui.view.report.output;

import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportElement;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportPaper;

/**
 * 보고서를 PDF 파일로 저장한다. 페이지의 요소들을 그림이 아니라 글자와 선 그대로 옮기므로 확대해도 선명하고 글자를 검색하거나 복사할 수
 * 있다. 글꼴은 쓰인 글자만 추려 파일에 넣는다.
 */
public final class ReportPdfExporter {

    /** 글꼴에 없는 글자를 대신하는 글자 */
    private static final char REPLACEMENT = '?';

    private ReportPdfExporter() {
        // 인스턴스화 방지
    }

    /**
     * @throws UncheckedIOException 파일을 쓰지 못한 경우
     */
    public static void export(ReportDocument document, Path file) {
        try (PDDocument pdf = new PDDocument()) {
            pdf.getDocumentInformation()
                    .setTitle(document.getTitle());

            // 글꼴 파일은 실제로 쓰인 것만 처음 쓰일 때 읽어 넣는다.
            Map<String, PDFont> fonts = new HashMap<>();

            for (int i = 0; i < document.getPageCount(); i++) {
                PDPage page = new PDPage(new PDRectangle((float) ReportPaper.WIDTH, (float) ReportPaper.HEIGHT));
                pdf.addPage(page);

                try (PDPageContentStream content = new PDPageContentStream(pdf, page)) {
                    for (ReportElement element : document.getElements(i)) {
                        draw(pdf, content, element, fonts);
                    }
                }
            }

            pdf.save(file.toFile());
        } catch (IOException e) {
            throw new UncheckedIOException("PDF 파일을 저장하지 못했습니다: " + e.getMessage(), e);
        }
    }

    private static PDFont loadFont(PDDocument pdf, String resource) throws IOException {
        try (InputStream in = ReportPdfExporter.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IOException("글꼴 파일이 없습니다: " + resource);
            }
            return PDType0Font.load(pdf, in, true);
        }
    }

    private static PDFont fontFor(PDDocument pdf, Font font, Map<String, PDFont> fonts) throws IOException {
        String resource = ReportFonts.resourceOf(font);
        PDFont loaded = fonts.get(resource);
        if (loaded == null) {
            loaded = loadFont(pdf, resource);
            fonts.put(resource, loaded);
        }
        return loaded;
    }

    private static void draw(PDDocument pdf, PDPageContentStream content, ReportElement element,
            Map<String, PDFont> fonts) throws IOException {
        switch (element) {
        case ReportElement.Text text -> {
            PDFont font = fontFor(pdf, text.font(), fonts);
            content.beginText();
            content.setFont(font, text.font()
                    .getSize2D());
            content.setNonStrokingColor(text.color());
            content.newLineAtOffset((float) text.x(), flipY(text.baseline()));
            content.showText(toEncodable(font, text.text()));
            content.endText();
        }
        case ReportElement.Line line -> {
            content.setStrokingColor(line.color());
            content.setLineWidth((float) line.width());
            content.moveTo((float) line.x1(), flipY(line.y1()));
            content.lineTo((float) line.x2(), flipY(line.y2()));
            content.stroke();
        }
        case ReportElement.Rect rect -> {
            if (rect.fill() != null) {
                content.setNonStrokingColor(rect.fill());
                addRect(content, rect);
                content.fill();
            }
            if (rect.stroke() != null) {
                content.setStrokingColor(rect.stroke());
                content.setLineWidth((float) rect.strokeWidth());
                addRect(content, rect);
                content.stroke();
            }
        }
        }
    }

    private static void addRect(PDPageContentStream content, ReportElement.Rect rect) throws IOException {
        content.addRect((float) rect.x(), flipY(rect.y() + rect.height()), (float) rect.width(),
                (float) rect.height());
    }

    /**
     * 보고서는 위에서 아래로, PDF는 아래에서 위로 세로 좌표가 커지므로 뒤집는다.
     */
    private static float flipY(double y) {
        return (float) (ReportPaper.HEIGHT - y);
    }

    /**
     * 글꼴에 없는 글자가 섞여 있으면 PDF에 쓸 수 없으므로 그런 글자만 물음표로 바꾼다.
     */
    private static String toEncodable(PDFont font, String text) {
        StringBuilder encodable = new StringBuilder(text.length());
        text.codePoints()
                .forEach(codePoint -> {
                    if (Character.isWhitespace(codePoint)) {
                        encodable.append(' ');
                    } else if (canEncode(font, codePoint)) {
                        encodable.appendCodePoint(codePoint);
                    } else {
                        encodable.append(REPLACEMENT);
                    }
                });
        return encodable.toString();
    }

    private static boolean canEncode(PDFont font, int codePoint) {
        try {
            font.encode(Character.toString(codePoint));
            return true;
        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }
}
