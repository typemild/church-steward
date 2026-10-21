package org.church.steward.infrastructure.spreadsheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.poifs.crypt.Decryptor;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SpreadsheetWriterTest {

    private static final String PASSWORD = "export-password1!";

    @TempDir
    Path tempDir;

    private final SpreadsheetWriter spreadsheetWriter = new SpreadsheetWriter();

    /**
     * 글자, 날짜, 수, 빈 칸이 모두 든 표. 둘째 행의 관계자 칸에는 쉼표가, 내용 칸에는 큰따옴표가 들어 있다.
     */
    private SpreadsheetTable createTable() {
        return new SpreadsheetTable("수입지출", List.of("구분", "일자", "금액", "내용", "봉헌자/수취인"),
                List.of(
                        Arrays.asList("수입", LocalDate.of(2026, 3, 5), 1500000L, "십일조", "홍길동"),
                        Arrays.asList("수입", LocalDate.of(2026, 3, 8), 30000L, "\"감사\" 헌금", "홍길동, 김철수"),
                        Arrays.asList("지출", LocalDate.of(2026, 3, 9), 50000L, null, "OO상사")));
    }

    private XSSFWorkbook openEncrypted(Path file, String password) throws IOException, GeneralSecurityException {
        try (POIFSFileSystem fileSystem = new POIFSFileSystem(file.toFile(), true)) {
            Decryptor decryptor = Decryptor.getInstance(new EncryptionInfo(fileSystem));
            if (!decryptor.verifyPassword(password)) {
                throw new GeneralSecurityException("wrong password");
            }
            try (InputStream in = decryptor.getDataStream(fileSystem)) {
                return new XSSFWorkbook(in);
            }
        }
    }

    @Nested
    @DisplayName("write 테스트")
    class WriteTest {

        @Test
        @DisplayName("엑셀로 저장하면 표의 이름이 시트 이름이 되고 첫 행에 머리글이 들어간다")
        void write_Xlsx_WritesSheetNameAndHeaders() throws Exception {
            // Given
            Path file = tempDir.resolve("export.xlsx");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.XLSX, createTable(), null);

            // Then
            try (InputStream in = Files.newInputStream(file);
                    XSSFWorkbook workbook = new XSSFWorkbook(in)) {
                Row headerRow = workbook.getSheet("수입지출").getRow(0);
                assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("구분");
                assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("봉헌자/수취인");
            }
        }

        @Test
        @DisplayName("엑셀로 저장하면 날짜는 날짜 셀, 수는 숫자 셀, 빈 값은 빈 칸이 된다")
        void write_Xlsx_WritesTypedCells() throws Exception {
            // Given
            Path file = tempDir.resolve("export.xlsx");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.XLSX, createTable(), null);

            // Then
            try (InputStream in = Files.newInputStream(file);
                    XSSFWorkbook workbook = new XSSFWorkbook(in)) {
                Row firstRow = workbook.getSheetAt(0).getRow(1);
                Row thirdRow = workbook.getSheetAt(0).getRow(3);
                assertThat(DateUtil.isCellDateFormatted(firstRow.getCell(1))).isTrue();
                assertThat(firstRow.getCell(1).getLocalDateTimeCellValue().toLocalDate())
                        .isEqualTo(LocalDate.of(2026, 3, 5));
                assertThat(firstRow.getCell(2).getNumericCellValue()).isEqualTo(1500000.0);
                assertThat(thirdRow.getCell(3)).isNull();
            }
        }

        @Test
        @DisplayName("암호를 주어 엑셀로 저장하면 그 암호로 열 수 있다")
        void write_XlsxWithPassword_CanBeOpenedWithPassword() throws Exception {
            // Given
            Path file = tempDir.resolve("export.xlsx");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.XLSX, createTable(), PASSWORD);

            // Then
            try (XSSFWorkbook workbook = openEncrypted(file, PASSWORD)) {
                assertThat(workbook.getSheetAt(0).getRow(1).getCell(3).getStringCellValue()).isEqualTo("십일조");
            }
        }

        @Test
        @DisplayName("암호를 주어 엑셀로 저장하면 다른 암호로는 열 수 없다")
        void write_XlsxWithPassword_CannotBeOpenedWithWrongPassword() {
            // Given
            Path file = tempDir.resolve("export.xlsx");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.XLSX, createTable(), PASSWORD);

            // Then
            assertThatThrownBy(() -> openEncrypted(file, "wrong-password"))
                    .isInstanceOf(GeneralSecurityException.class);
        }

        @Test
        @DisplayName("CSV로 저장하면 BOM을 붙인 UTF-8로 쓰고 날짜는 2026-03-05 형식, 수는 자릿수 구분 없이 적는다")
        void write_Csv_WritesBomAndPlainValues() throws Exception {
            // Given
            Path file = tempDir.resolve("export.csv");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, createTable(), null);

            // Then
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.get(0)).isEqualTo((char) 0xFEFF + "구분,일자,금액,내용,봉헌자/수취인");
            assertThat(lines.get(1)).isEqualTo("수입,2026-03-05,1500000,십일조,홍길동");
        }

        @Test
        @DisplayName("CSV로 저장하면 쉼표가 든 값은 큰따옴표로 묶고 값 안의 큰따옴표는 두 번 쓴다")
        void write_Csv_QuotesSpecialValues() throws Exception {
            // Given
            Path file = tempDir.resolve("export.csv");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, createTable(), null);

            // Then
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.get(2)).isEqualTo("수입,2026-03-08,30000,\"\"\"감사\"\" 헌금\",\"홍길동, 김철수\"");
        }

        @Test
        @DisplayName("CSV로 저장하면 빈 값은 빈 칸으로 적는다")
        void write_Csv_WritesNullAsEmpty() throws Exception {
            // Given
            Path file = tempDir.resolve("export.csv");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, createTable(), null);

            // Then
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.get(3)).isEqualTo("지출,2026-03-09,50000,,OO상사");
        }

        @Test
        @DisplayName("CSV로 저장하면 =, +, -, @로 시작하는 글자 값은 엑셀이 수식으로 실행하지 않도록 앞에 작은따옴표를 붙인다")
        void write_CsvFormulaLikeText_PrefixesApostrophe() throws Exception {
            // Given
            Path file = tempDir.resolve("export.csv");
            SpreadsheetTable table = new SpreadsheetTable("수입지출", List.of("내용", "관계자", "비고", "메모"),
                    List.of(List.of("=HYPERLINK(\"http://example.com\")", "+홍길동", "-감사", "@합계")));

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, table, null);

            // Then
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.get(1)).isEqualTo("\"'=HYPERLINK(\"\"http://example.com\"\")\",'+홍길동,'-감사,'@합계");
        }

        @Test
        @DisplayName("CSV로 저장하면 음수처럼 수로 쓰는 값에는 작은따옴표를 붙이지 않는다")
        void write_CsvNegativeNumber_WritesAsIs() throws Exception {
            // Given
            Path file = tempDir.resolve("export.csv");
            SpreadsheetTable table = new SpreadsheetTable("예산", List.of("금액"), List.of(List.of(-50000L)));

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, table, null);

            // Then
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            assertThat(lines.get(1)).isEqualTo("-50000");
        }

        @Test
        @DisplayName("CSV 형식에 암호를 주면 저장하지 않고 거부한다")
        void write_CsvWithPassword_Failure() {
            // Given
            Path file = tempDir.resolve("export.csv");

            // When & Then
            assertThatThrownBy(() -> spreadsheetWriter.write(file, SpreadsheetFormat.CSV, createTable(), PASSWORD))
                    .isInstanceOf(SpreadsheetException.class)
                    .hasMessage("CSV 형식에는 암호를 걸 수 없습니다.");
        }

        @Test
        @DisplayName("같은 이름의 파일이 있으면 덮어쓴다")
        void write_ExistingFile_Overwrites() throws Exception {
            // Given
            Path file = Files.writeString(tempDir.resolve("export.csv"), "예전 내용");

            // When
            spreadsheetWriter.write(file, SpreadsheetFormat.CSV, createTable(), null);

            // Then
            assertThat(Files.readString(file, StandardCharsets.UTF_8)).doesNotContain("예전 내용");
        }
    }

}
