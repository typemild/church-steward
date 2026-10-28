package org.church.steward.ui.view.report.output;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.ui.view.report.layout.ReportAlign;
import org.church.steward.ui.view.report.layout.ReportCanvas;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.layout.ReportFonts;
import org.church.steward.ui.view.report.layout.ReportStyle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReportPdfExporterTest {

    @TempDir
    Path tempDir;

    /**
     * 제목과 본문 한 줄이 있는 페이지를 지정한 수만큼 가진 보고서를 만든다.
     */
    private ReportDocument document(int pageCount, String body) {
        return document(pageCount, body, ReportStyle.DEFAULT);
    }

    private ReportDocument document(int pageCount, String body, ReportStyle style) {
        ReportCanvas canvas = new ReportCanvas("시험 보고서", style);
        for (int i = 0; i < pageCount; i++) {
            if (i > 0) {
                canvas.newPage();
            }
            canvas.title("시험 보고서", ReportFonts.SIZE_TITLE);
            canvas.text(body, style.regular(ReportFonts.SIZE_BODY), ReportAlign.LEFT, canvas.getLeft(),
                    canvas.getY() + 20);
            canvas.line(canvas.getLeft(), canvas.getY() + 30, canvas.getRight(), canvas.getY() + 30, 0.5,
                    Color.BLACK);
            canvas.fillRect(canvas.getLeft(), canvas.getY() + 40, 50, 10, Color.GRAY);
        }
        return canvas.finish();
    }

    @Nested
    @DisplayName("export 테스트")
    class ExportTest {

        @Test
        @DisplayName("보고서의 페이지 수만큼 A4 크기의 페이지를 가진 PDF 파일을 만든다")
        void export_TwoPages_WritesA4PdfWithSamePageCount() throws IOException {
            // Given
            Path file = tempDir.resolve("report.pdf");
            ReportDocument document = document(2, "본문");

            // When
            ReportPdfExporter.export(document, file);

            // Then
            try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
                assertThat(pdf.getNumberOfPages()).isEqualTo(2);
                PDRectangle size = pdf.getPage(0)
                        .getMediaBox();
                assertThat(Math.round(size.getWidth())).isEqualTo(Math.round(PDRectangle.A4.getWidth()));
                assertThat(Math.round(size.getHeight())).isEqualTo(Math.round(PDRectangle.A4.getHeight()));
            }
        }

        @Test
        @DisplayName("한글을 그림이 아닌 글자로 넣어 PDF에서 검색하고 복사할 수 있게 한다")
        void export_KoreanText_KeepsTextSearchable() throws IOException {
            // Given
            Path file = tempDir.resolve("report.pdf");
            ReportDocument document = document(1, "십일조 1,234,000");

            // When
            ReportPdfExporter.export(document, file);

            // Then
            try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
                String text = new PDFTextStripper().getText(pdf);
                assertThat(text).contains("시험 보고서")
                        .contains("십일조 1,234,000");
            }
        }

        @Test
        @DisplayName("글꼴에 없는 글자가 섞여 있어도 그 글자만 물음표로 바꿔 저장한다")
        void export_TextWithUnsupportedCharacter_ReplacesItWithQuestionMark() throws IOException {
            // Given
            Path file = tempDir.resolve("report.pdf");
            ReportDocument document = document(1, "감사😀헌금");

            // When
            ReportPdfExporter.export(document, file);

            // Then
            try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
                String text = new PDFTextStripper().getText(pdf);
                assertThat(text).contains("감사?헌금");
            }
        }

        @Test
        @DisplayName("나눔명조로 배치한 보고서는 나눔명조 글꼴을 넣어 저장한다")
        void export_MyeongjoStyle_EmbedsMyeongjoFont() throws IOException {
            // Given
            Path file = tempDir.resolve("report.pdf");
            ReportDocument document = document(1, "감사헌금",
                    ReportStyle.DEFAULT.withFontFamily(ReportFontFamily.NANUM_MYEONGJO));

            // When
            ReportPdfExporter.export(document, file);

            // Then
            try (PDDocument pdf = Loader.loadPDF(file.toFile())) {
                List<String> fontNames = new ArrayList<>();
                for (COSName name : pdf.getPage(0)
                        .getResources()
                        .getFontNames()) {
                    fontNames.add(pdf.getPage(0)
                            .getResources()
                            .getFont(name)
                            .getName());
                }
                assertThat(fontNames).allMatch(fontName -> fontName.contains("NanumMyeongjo"));
            }
        }
    }
}
