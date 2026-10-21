package org.church.steward.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpreadsheetReaderTest {

    private static final String PASSWORD = "import-password1!";
    private static final Charset MS949 = Charset.forName("MS949");

    @TempDir
    Path tempDir;

    private final SpreadsheetReader spreadsheetReader = new SpreadsheetReader();
    private final SpreadsheetWriter spreadsheetWriter = new SpreadsheetWriter();

    private SpreadsheetTable createTable() {
        return new SpreadsheetTable("예산", List.of("구분", "일자", "금액", "내용"),
                List.of(
                        Arrays.asList("수입", LocalDate.of(2026, 3, 5), 1500000L, "십일조"),
                        Arrays.asList("지출", LocalDate.of(2026, 3, 9), 50000L, null)));
    }

    private Path writeFile(String fileName, SpreadsheetFormat format, String password) {
        Path file = tempDir.resolve(fileName);
        spreadsheetWriter.write(file, format, createTable(), password);
        return file;
    }

    private Path writeText(String fileName, String text, Charset charset) throws IOException {
        return Files.write(tempDir.resolve(fileName), text.getBytes(charset));
    }

    /**
     * 시트 "첫째"(행 하나)와 "둘째"(둘째 행이 비어 있고, 수식과 소수가 든 행이 있음)를 가진 통합 문서를 저장한다.
     */
    private Path writeWorkbook(Workbook workbook, String fileName) throws IOException {
        Sheet first = workbook.createSheet("첫째");
        first.createRow(0).createCell(0).setCellValue("첫째 시트");

        Sheet second = workbook.createSheet("둘째");
        second.createRow(0).createCell(0).setCellValue("둘째 시트");
        Row row = second.createRow(2);
        row.createCell(0).setCellValue(10);
        row.createCell(1).setCellValue(2.5);
        row.createCell(2).setCellFormula("A3*2");
        workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();

        Path file = tempDir.resolve(fileName);
        try (OutputStream out = Files.newOutputStream(file)) {
            workbook.write(out);
        }
        workbook.close();
        return file;
    }

    @Nested
    @DisplayName("isExcel 테스트")
    class IsExcelTest {

        @Test
        @DisplayName("확장자가 xlsx이면 엑셀 파일이다")
        void isExcel_XlsxExtension_ReturnsTrue() {
            // Given
            Path file = tempDir.resolve("예산.XLSX");

            // When
            boolean excel = spreadsheetReader.isExcel(file);

            // Then
            assertThat(excel).isTrue();
        }

        @Test
        @DisplayName("확장자가 csv이면 엑셀 파일이 아니다")
        void isExcel_CsvExtension_ReturnsFalse() {
            // Given
            Path file = tempDir.resolve("예산.csv");

            // When
            boolean excel = spreadsheetReader.isExcel(file);

            // Then
            assertThat(excel).isFalse();
        }

        @Test
        @DisplayName("지원하지 않는 확장자는 거부한다")
        void isExcel_UnsupportedExtension_Failure() {
            // Given
            Path file = tempDir.resolve("예산.pdf");

            // When & Then
            assertThatThrownBy(() -> spreadsheetReader.isExcel(file))
                    .isInstanceOf(SpreadsheetException.class)
                    .hasMessageStartingWith("지원하지 않는 형식의 파일입니다.");
        }
    }

    @Nested
    @DisplayName("listSheets 테스트")
    class ListSheetsTest {

        @Test
        @DisplayName("엑셀 파일의 시트 이름을 순서대로 반환한다")
        void listSheets_Workbook_ReturnsSheetNamesInOrder() throws Exception {
            // Given
            Path file = writeWorkbook(new XSSFWorkbook(), "book.xlsx");

            // When
            List<String> sheetNames = spreadsheetReader.listSheets(file, null);

            // Then
            assertThat(sheetNames).containsExactly("첫째", "둘째");
        }

        @Test
        @DisplayName("CSV 파일은 시트가 없으므로 빈 목록을 반환한다")
        void listSheets_Csv_ReturnsEmptyList() throws Exception {
            // Given
            Path file = writeText("data.csv", "a,b", StandardCharsets.UTF_8);

            // When
            List<String> sheetNames = spreadsheetReader.listSheets(file, null);

            // Then
            assertThat(sheetNames).isEmpty();
        }

        @Test
        @DisplayName("암호가 걸린 파일에 암호를 주지 않으면 암호를 요구한다")
        void listSheets_EncryptedWithoutPassword_Failure() {
            // Given
            Path file = writeFile("encrypted.xlsx", SpreadsheetFormat.XLSX, PASSWORD);

            // When & Then
            assertThatThrownBy(() -> spreadsheetReader.listSheets(file, null))
                    .isInstanceOf(SpreadsheetPasswordException.class)
                    .hasMessage("암호가 걸린 파일입니다. 암호를 입력해주세요.");
        }
    }

    @Nested
    @DisplayName("read 테스트")
    class ReadTest {

        @Test
        @DisplayName("xlsx 파일의 글자, 날짜, 수, 빈 칸을 각각의 형식으로 읽는다")
        void read_Xlsx_ReadsTypedValues() {
            // Given
            Path file = writeFile("data.xlsx", SpreadsheetFormat.XLSX, null);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(
                    Arrays.asList("구분", "일자", "금액", "내용"),
                    Arrays.asList("수입", LocalDate.of(2026, 3, 5), 1500000L, "십일조"),
                    Arrays.asList("지출", LocalDate.of(2026, 3, 9), 50000L, null));
        }

        @Test
        @DisplayName("암호가 걸린 xlsx 파일을 맞는 암호로 읽는다")
        void read_EncryptedXlsxWithPassword_ReadsRows() {
            // Given
            Path file = writeFile("encrypted.xlsx", SpreadsheetFormat.XLSX, PASSWORD);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, PASSWORD, null, null);

            // Then
            assertThat(data.rows()).hasSize(3);
        }

        @Test
        @DisplayName("암호가 틀리면 암호가 맞지 않는다고 알린다")
        void read_EncryptedXlsxWithWrongPassword_Failure() {
            // Given
            Path file = writeFile("encrypted.xlsx", SpreadsheetFormat.XLSX, PASSWORD);

            // When & Then
            assertThatThrownBy(() -> spreadsheetReader.read(file, "wrong-password", null, null))
                    .isInstanceOf(SpreadsheetPasswordException.class)
                    .hasMessage("암호가 맞지 않습니다.");
        }

        @Test
        @DisplayName("시트 이름을 주면 그 시트를 읽고, 중간의 빈 행은 빈 행으로 남긴다")
        void read_NamedSheet_ReadsThatSheetKeepingBlankRows() throws Exception {
            // Given
            Path file = writeWorkbook(new XSSFWorkbook(), "book.xlsx");

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, "둘째", null);

            // Then
            assertThat(data.rows()).containsExactly(
                    Arrays.asList("둘째 시트", null, null),
                    Arrays.asList(null, null, null),
                    Arrays.asList(10L, 2.5, 20L));
        }

        @Test
        @DisplayName("시트 이름을 주지 않으면 첫 번째 시트를 읽는다")
        void read_NoSheetName_ReadsFirstSheet() throws Exception {
            // Given
            Path file = writeWorkbook(new XSSFWorkbook(), "book.xlsx");

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(List.of("첫째 시트"));
        }

        @Test
        @DisplayName("없는 시트 이름을 주면 거부한다")
        void read_UnknownSheet_Failure() throws Exception {
            // Given
            Path file = writeWorkbook(new XSSFWorkbook(), "book.xlsx");

            // When & Then
            assertThatThrownBy(() -> spreadsheetReader.read(file, null, "없는 시트", null))
                    .isInstanceOf(SpreadsheetException.class)
                    .hasMessage("시트를 찾을 수 없습니다: 없는 시트");
        }

        @Test
        @DisplayName("예전 형식의 xls 파일도 읽는다")
        void read_Xls_ReadsRows() throws Exception {
            // Given
            Path file = writeWorkbook(new HSSFWorkbook(), "book.xls");

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, "둘째", null);

            // Then
            assertThat(data.rows().get(2)).containsExactly(10L, 2.5, 20L);
        }

        @Test
        @DisplayName("프로그램이 내보낸 CSV(BOM이 붙은 UTF-8)를 읽으면 BOM은 값에 넣지 않고 모든 값을 글자로 읽는다")
        void read_ExportedCsv_ReadsTextValuesWithoutBom() {
            // Given
            Path file = writeFile("data.csv", SpreadsheetFormat.CSV, null);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(
                    Arrays.asList("구분", "일자", "금액", "내용"),
                    Arrays.asList("수입", "2026-03-05", "1500000", "십일조"),
                    Arrays.asList("지출", "2026-03-09", "50000", null));
        }

        @Test
        @DisplayName("UTF-8로 해석되지 않는 CSV는 MS949로 읽는다")
        void read_Ms949Csv_DetectsCharset() throws Exception {
            // Given
            Path file = writeText("data.csv", "구분,내용\r\n수입,십일조\r\n", MS949);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows().get(1)).containsExactly("수입", "십일조");
        }

        @Test
        @DisplayName("CSV를 읽는 데 쓴 인코딩을 함께 알려준다")
        void read_Ms949Csv_ReportsUsedCharset() throws Exception {
            // Given
            Path file = writeText("data.csv", "구분,내용\r\n수입,십일조\r\n", MS949);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.charset()).isEqualTo(MS949);
        }

        @Test
        @DisplayName("인코딩을 지정하면 자동 판별 대신 그 인코딩으로 읽는다")
        void read_CsvWithExplicitCharset_UsesThatCharset() throws Exception {
            // Given
            Path file = writeText("data.csv", "구분,내용\r\n수입,십일조\r\n", StandardCharsets.UTF_16LE);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, StandardCharsets.UTF_16LE);

            // Then
            assertThat(data.rows().get(1)).containsExactly("수입", "십일조");
        }

        @Test
        @DisplayName("큰따옴표로 묶인 값 안의 쉼표, 줄바꿈, 큰따옴표 두 개를 값의 일부로 읽는다")
        void read_QuotedCsvValues_ReadsSpecialCharacters() throws Exception {
            // Given
            Path file = writeText("data.csv", "\"홍길동,김철수\",\"첫 줄\n둘째 줄\",\"\"\"감사\"\" 헌금\"\n",
                    StandardCharsets.UTF_8);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(List.of("홍길동,김철수", "첫 줄\n둘째 줄", "\"감사\" 헌금"));
        }

        @Test
        @DisplayName("내보낼 때 수식을 막으려고 붙인 작은따옴표는 떼고 원래 값으로 읽는다")
        void read_FormulaEscapedCsv_RemovesApostrophe() throws Exception {
            // Given
            Path file = writeText("data.csv", "'=SUM(A1),'-감사,'@합계\n", StandardCharsets.UTF_8);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(List.of("=SUM(A1)", "-감사", "@합계"));
        }

        @Test
        @DisplayName("작은따옴표 뒤가 수식으로 해석되는 글자가 아니면 작은따옴표를 값의 일부로 읽는다")
        void read_ApostropheText_KeepsApostrophe() throws Exception {
            // Given
            Path file = writeText("data.csv", "'좋은 소식',''\n", StandardCharsets.UTF_8);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(List.of("'좋은 소식'", "''"));
        }

        @Test
        @DisplayName("프로그램이 내보낸 CSV를 다시 읽으면 수식처럼 보이는 값도 내보내기 전의 값과 같다")
        void read_ExportedFormulaLikeCsv_RoundTrips() {
            // Given
            Path file = tempDir.resolve("data.csv");
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV,
                    new SpreadsheetTable("수입지출", List.of("내용"), List.of(List.of("=1+2"))), null);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows().get(1)).containsExactly("=1+2");
        }

        @Test
        @DisplayName("행마다 칸 수가 달라도 가장 긴 행에 맞추고, 마지막 줄이 줄바꿈 없이 끝나도 읽는다")
        void read_RaggedCsv_PadsRowsToSameWidth() throws Exception {
            // Given
            Path file = writeText("data.csv", "a,b,c\nd\ne,f", StandardCharsets.UTF_8);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).containsExactly(
                    Arrays.asList("a", "b", "c"),
                    Arrays.asList("d", null, null),
                    Arrays.asList("e", "f", null));
        }

        @Test
        @DisplayName("내용이 없는 파일은 행이 없는 표로 읽는다")
        void read_EmptyCsv_ReturnsNoRows() throws Exception {
            // Given
            Path file = writeText("data.csv", "", StandardCharsets.UTF_8);

            // When
            SpreadsheetData data = spreadsheetReader.read(file, null, null, null);

            // Then
            assertThat(data.rows()).isEmpty();
        }
    }

}
