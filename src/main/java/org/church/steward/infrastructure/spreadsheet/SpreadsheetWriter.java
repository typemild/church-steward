package org.church.steward.infrastructure.spreadsheet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.util.List;

import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * 표를 엑셀(xlsx) 또는 CSV 파일로 저장한다.
 * <ul>
 * <li>엑셀: 날짜와 수는 글자가 아니라 날짜 셀과 숫자 셀로 쓴다. 암호를 주면 엑셀에서 암호를 입력해야 열리는 파일이 된다.</li>
 * <li>CSV: UTF-8이며, 엑셀이 한글을 올바로 읽도록 BOM을 붙인다. 날짜는 "2026-03-05" 형식, 수는 자릿수 구분 없이
 * 쓴다. 수식으로 해석될 수 있는 글자 값에는 앞에 작은따옴표를 붙인다.</li>
 * </ul>
 */
@Component
public class SpreadsheetWriter {

    private static final String DATE_FORMAT = "yyyy-mm-dd";
    private static final String NUMBER_FORMAT = "#,##0";
    /** 엑셀의 열 너비 단위: 글자 하나 폭의 1/256 */
    private static final int COLUMN_WIDTH_UNIT = 256;
    private static final int MIN_COLUMN_CHARS = 8;
    private static final int MAX_COLUMN_CHARS = 60;

    /** BOM(U+FEFF). 보이지 않는 글자이므로 소스에는 글자 대신 코드 값으로 적는다. */
    private static final char CSV_BOM = (char) 0xFEFF;
    private static final String CSV_LINE_SEPARATOR = "\r\n";
    /**
     * 엑셀이 CSV의 값을 수식으로 해석하는 첫 글자: =, +, -, @, 탭, CR.
     * 가져오기({@link SpreadsheetReader})도 쓴다.
     */
    static final String FORMULA_PREFIXES = "=+-@\t\r";
    /** 수식으로 해석되지 않도록 값 앞에 붙이는 글자. 가져오기({@link SpreadsheetReader})는 이 글자를 뗀다. */
    static final char FORMULA_ESCAPE = '\'';

    /**
     * 표를 파일로 저장한다. 같은 이름의 파일이 있으면 덮어쓴다.
     *
     * @param target   저장할 파일
     * @param format   파일 형식
     * @param table    저장할 표
     * @param password 파일에 걸 암호. 암호를 걸지 않으면 null 또는 빈 문자열
     * @throws SpreadsheetException 파일을 저장하지 못했을 때, 암호를 걸 수 없는 형식에 암호를 주었을 때 발생
     */
    public void write(Path target, SpreadsheetFormat format, SpreadsheetTable table, String password) {
        boolean encrypted = password != null && !password.isEmpty();
        if (encrypted && !format.isPasswordSupported()) {
            throw new SpreadsheetException(format.getLabel() + " 형식에는 암호를 걸 수 없습니다.");
        }

        // 임시 파일에 먼저 쓴 뒤에 옮겨서, 쓰다가 실패해도 반쯤 쓰인 파일이 남지 않게 한다.
        Path absoluteTarget = target.toAbsolutePath();
        Path tempFile = absoluteTarget.resolveSibling(absoluteTarget.getFileName() + ".tmp");
        try {
            switch (format) {
            case XLSX -> writeXlsx(tempFile, table, encrypted ? password : null);
            case CSV -> writeCsv(tempFile, table);
            }
            Files.move(tempFile, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | GeneralSecurityException e) {
            throw new SpreadsheetException("파일을 저장하지 못했습니다: " + e.getMessage(), e);
        } finally {
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException e) {
                // 임시 파일을 지우지 못해도 저장 결과에는 영향이 없다.
            }
        }
    }

    private void writeXlsx(Path file, SpreadsheetTable table, String password)
            throws IOException, GeneralSecurityException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(table.name());

            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat(DATE_FORMAT));
            CellStyle numberStyle = workbook.createCellStyle();
            numberStyle.setDataFormat(workbook.createDataFormat().getFormat(NUMBER_FORMAT));

            Row headerRow = sheet.createRow(0);
            for (int column = 0; column < table.headers().size(); column++) {
                Cell cell = headerRow.createCell(column);
                cell.setCellValue(table.headers().get(column));
                cell.setCellStyle(headerStyle);
            }

            for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
                List<Object> values = table.rows().get(rowIndex);
                Row row = sheet.createRow(rowIndex + 1);
                for (int column = 0; column < values.size(); column++) {
                    Object value = values.get(column);
                    if (value == null) {
                        continue;
                    }
                    Cell cell = row.createCell(column);
                    if (value instanceof LocalDate date) {
                        cell.setCellValue(date);
                        cell.setCellStyle(dateStyle);
                    } else if (value instanceof Number number) {
                        cell.setCellValue(number.doubleValue());
                        cell.setCellStyle(numberStyle);
                    } else {
                        cell.setCellValue(value.toString());
                    }
                }
            }

            sheet.createFreezePane(0, 1);
            for (int column = 0; column < table.headers().size(); column++) {
                sheet.setColumnWidth(column, columnChars(table, column) * COLUMN_WIDTH_UNIT);
            }

            if (password == null) {
                try (OutputStream out = Files.newOutputStream(file)) {
                    workbook.write(out);
                }
            } else {
                writeEncrypted(workbook, file, password);
            }
        }
    }

    /**
     * 통합 문서를 암호화해 저장한다. 엑셀에서 "암호 설정"으로 저장한 파일과 같은 형식이다.
     */
    private void writeEncrypted(XSSFWorkbook workbook, Path file, String password)
            throws IOException, GeneralSecurityException {
        try (POIFSFileSystem fileSystem = new POIFSFileSystem()) {
            Encryptor encryptor = new EncryptionInfo(EncryptionMode.agile).getEncryptor();
            encryptor.confirmPassword(password);
            try (OutputStream encryptedOut = encryptor.getDataStream(fileSystem)) {
                workbook.write(encryptedOut);
            }
            try (OutputStream out = Files.newOutputStream(file)) {
                fileSystem.writeFilesystem(out);
            }
        }
    }

    /**
     * 열의 내용이 다 보이는 너비를 글자 수로 구한다. 한글처럼 폭이 넓은 글자는 두 글자로 센다.
     */
    private int columnChars(SpreadsheetTable table, int column) {
        int chars = displayLength(table.headers().get(column));
        for (List<Object> row : table.rows()) {
            if (column < row.size()) {
                chars = Math.max(chars, displayLength(toDisplayText(row.get(column))));
            }
        }
        return Math.clamp(chars + 2, MIN_COLUMN_CHARS, MAX_COLUMN_CHARS);
    }

    private String toDisplayText(Object value) {
        if (value instanceof Number number) {
            return String.format("%,d", number.longValue());
        }
        return value == null ? "" : value.toString();
    }

    private int displayLength(String text) {
        int length = 0;
        for (int i = 0; i < text.length(); i++) {
            length += text.charAt(i) > 0x7F ? 2 : 1;
        }
        return length;
    }

    private void writeCsv(Path file, SpreadsheetTable table) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(CSV_BOM);
            writeCsvLine(writer, table.headers());
            for (List<Object> row : table.rows()) {
                writeCsvLine(writer, row);
            }
        }
    }

    private void writeCsvLine(Writer writer, List<?> values) throws IOException {
        for (int column = 0; column < values.size(); column++) {
            if (column > 0) {
                writer.write(',');
            }
            Object value = values.get(column);
            if (value instanceof String text) {
                writer.write(escapeCsv(protectFormula(text)));
            } else if (value != null) {
                writer.write(escapeCsv(value.toString()));
            }
        }
        writer.write(CSV_LINE_SEPARATOR);
    }

    /**
     * 수식으로 해석될 수 있는 글자로 시작하는 값 앞에 작은따옴표를 붙여, 엑셀이 글자로 읽게 한다. 관계자 이름이나 내용처럼 사용자가 입력한
     * 값이 "=HYPERLINK(...)" 같은 수식으로 실행되는 것(CSV 수식 주입)을 막기 위함이다. 글자 값에만 적용하므로 음수 금액처럼
     * 수로 쓰는 값은 그대로다. 가져오기는 이 작은따옴표를 떼고 읽는다.
     */
    private String protectFormula(String value) {
        if (!value.isEmpty() && FORMULA_PREFIXES.indexOf(value.charAt(0)) >= 0) {
            return FORMULA_ESCAPE + value;
        }
        return value;
    }

    /**
     * 쉼표, 큰따옴표, 줄바꿈이 있거나 앞뒤에 공백이 있는 값은 큰따옴표로 묶고, 값 안의 큰따옴표는 두 번 쓴다.
     */
    private String escapeCsv(String value) {
        boolean needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
                || value.contains("\r") || !value.equals(value.strip());
        if (!needsQuotes) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

}
