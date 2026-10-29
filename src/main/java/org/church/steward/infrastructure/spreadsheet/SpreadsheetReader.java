package org.church.steward.infrastructure.spreadsheet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

/**
 * 엑셀(xlsx, xls) 또는 CSV 파일을 표로 읽는다. 형식은 파일의 확장자로 판단한다.
 * <ul>
 * <li>엑셀: 날짜 셀은 날짜로, 숫자 셀은 수로 읽는다. 수식은 엑셀이 마지막으로 계산해 둔 값을 읽는다. 병합된 셀은 엑셀이 저장한
 * 대로 첫 칸에만 값이 있고 나머지는 빈 칸이다.</li>
 * <li>CSV: 모든 값을 글자로 읽는다. 인코딩을 지정하지 않으면 BOM이 있거나 UTF-8로 해석되는 파일은 UTF-8로, 그렇지
 * 않으면 한글 윈도우의 기본 인코딩(MS949)으로 읽는다. 내보낼 때 수식을 막으려고 붙인 작은따옴표는 떼고 읽는다.</li>
 * </ul>
 */
@Component
public class SpreadsheetReader {

    /** CSV의 인코딩을 자동으로 판별하지 못했을 때 쓰는 인코딩. 한글 엑셀이 CSV를 저장할 때 쓰는 인코딩이다. */
    public static final Charset FALLBACK_CHARSET = Charset.forName("MS949");

    private static final char BOM = (char) 0xFEFF;
    private static final List<String> EXCEL_EXTENSIONS = List.of("xlsx", "xls", "xlsm");
    private static final List<String> CSV_EXTENSIONS = List.of("csv", "txt");

    /**
     * 엑셀 파일인지 여부. 엑셀 파일이면 시트와 암호를 고를 수 있고, 아니면 인코딩을 고를 수 있다.
     *
     * @throws SpreadsheetException 지원하지 않는 형식일 때 발생
     */
    public boolean isExcel(Path file) {
        String extension = extensionOf(file);
        if (EXCEL_EXTENSIONS.contains(extension)) {
            return true;
        }
        if (CSV_EXTENSIONS.contains(extension)) {
            return false;
        }
        throw new SpreadsheetException("지원하지 않는 형식의 파일입니다. 엑셀(xlsx, xls) 또는 CSV 파일을 선택해주세요.");
    }

    /**
     * 엑셀 파일의 시트 이름을 순서대로 읽는다. CSV 파일이면 빈 목록이다.
     *
     * @param password 파일의 암호. 암호가 없는 파일이면 null
     * @throws SpreadsheetPasswordException 암호가 필요한데 주지 않았거나 암호가 틀렸을 때 발생
     * @throws SpreadsheetException         파일을 읽지 못했을 때 발생
     */
    public List<String> listSheets(Path file, String password) {
        if (!isExcel(file)) {
            return List.of();
        }
        try (Workbook workbook = openWorkbook(file, password)) {
            List<String> sheetNames = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }
            return sheetNames;
        } catch (IOException e) {
            throw new SpreadsheetException("파일을 읽지 못했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 파일을 표로 읽는다.
     *
     * @param password  엑셀 파일의 암호. 암호가 없는 파일이거나 CSV이면 null
     * @param sheetName 읽을 시트의 이름. null이면 첫 번째 시트 (CSV에서는 쓰지 않음)
     * @param charset   CSV의 문자 인코딩. null이면 자동으로 판별 (엑셀에서는 쓰지 않음)
     * @throws SpreadsheetPasswordException 암호가 필요한데 주지 않았거나 암호가 틀렸을 때 발생
     * @throws SpreadsheetException         파일을 읽지 못했을 때 발생
     */
    public SpreadsheetData read(Path file, String password, String sheetName, Charset charset) {
        try {
            if (isExcel(file)) {
                return new SpreadsheetData(normalize(readExcel(file, password, sheetName)), null);
            }
            byte[] bytes = Files.readAllBytes(file);
            Charset usedCharset = charset != null ? charset : detectCharset(bytes);
            return new SpreadsheetData(normalize(parseCsv(new String(bytes, usedCharset))), usedCharset);
        } catch (IOException e) {
            throw new SpreadsheetException("파일을 읽지 못했습니다: " + e.getMessage(), e);
        }
    }

    private String extensionOf(Path file) {
        String name = file.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private Workbook openWorkbook(Path file, String password) throws IOException {
        try {
            return WorkbookFactory.create(file.toFile(), password, true);
        } catch (EncryptedDocumentException e) {
            throw new SpreadsheetPasswordException(password == null || password.isEmpty()
                    ? "암호가 걸린 파일입니다. 암호를 입력해주세요."
                    : "암호가 맞지 않습니다.");
        }
    }

    private List<List<Object>> readExcel(Path file, String password, String sheetName) throws IOException {
        try (Workbook workbook = openWorkbook(file, password)) {
            Sheet sheet = sheetName == null ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new SpreadsheetException("시트를 찾을 수 없습니다: " + sheetName);
            }

            List<List<Object>> rows = new ArrayList<>();
            // 중간의 빈 행도 행 번호가 어긋나지 않도록 빈 행으로 채운다.
            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                List<Object> values = new ArrayList<>();
                if (row != null) {
                    for (int column = 0; column < row.getLastCellNum(); column++) {
                        values.add(readCell(row.getCell(column)));
                    }
                }
                rows.add(values);
            }
            return rows;
        }
    }

    private Object readCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType()
                : cell.getCellType();
        return switch (type) {
        case STRING -> emptyToNull(cell.getStringCellValue());
        case NUMERIC -> readNumericCell(cell);
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        default -> null;
        };
    }

    private Object readNumericCell(Cell cell) {
        if (DateUtil.isCellDateFormatted(cell)) {
            return cell.getLocalDateTimeCellValue().toLocalDate();
        }
        double value = cell.getNumericCellValue();
        if (value == Math.rint(value) && Math.abs(value) < Long.MAX_VALUE) {
            return (long) value;
        }
        return value;
    }

    private String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    /**
     * BOM이 있거나 전체가 올바른 UTF-8이면 UTF-8로, 아니면 {@link #FALLBACK_CHARSET}으로 본다.
     */
    private Charset detectCharset(byte[] bytes) {
        try {
            StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException e) {
            return FALLBACK_CHARSET;
        }
    }

    /**
     * CSV를 해석한다. 큰따옴표로 묶인 값 안에서는 쉼표와 줄바꿈이 값의 일부이고, 큰따옴표 두 개는 큰따옴표 하나다. 줄바꿈은 CRLF,
     * LF, CR을 모두 받는다.
     */
    private List<List<Object>> parseCsv(String text) {
        List<List<Object>> rows = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        boolean rowHasContent = false;

        int start = !text.isEmpty() && text.charAt(0) == BOM ? 1 : 0;
        for (int i = start; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c != '"') {
                    value.append(c);
                } else if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = false;
                }
            } else if (c == '"') {
                quoted = true;
                rowHasContent = true;
            } else if (c == ',') {
                row.add(toCsvValue(value));
                value.setLength(0);
                rowHasContent = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
                row.add(toCsvValue(value));
                value.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
                rowHasContent = false;
            } else {
                value.append(c);
                rowHasContent = true;
            }
        }
        // 마지막 줄이 줄바꿈으로 끝나지 않은 경우
        if (rowHasContent) {
            row.add(toCsvValue(value));
            rows.add(row);
        }
        return rows;
    }

    /**
     * CSV의 값 하나를 만든다. 프로그램이 내보낼 때 수식으로 해석되지 않도록 앞에 붙였던 작은따옴표는
     * 뗀다({@link SpreadsheetWriter}). 작은따옴표 뒤가 수식으로 해석될 수 있는 글자일 때만 떼므로 "'좋은 소식'" 같은
     * 값은 그대로다.
     */
    private String toCsvValue(StringBuilder value) {
        if (value.length() >= 2 && value.charAt(0) == SpreadsheetWriter.FORMULA_ESCAPE
                && SpreadsheetWriter.FORMULA_PREFIXES.indexOf(value.charAt(1)) >= 0) {
            return value.substring(1);
        }
        return emptyToNull(value.toString());
    }

    /**
     * 모든 행의 칸 수를 가장 긴 행에 맞추고, 뒤쪽의 빈 행을 잘라낸다.
     */
    private List<List<Object>> normalize(List<List<Object>> rows) {
        int lastRow = rows.size() - 1;
        while (lastRow >= 0 && isBlank(rows.get(lastRow))) {
            lastRow--;
        }
        List<List<Object>> trimmed = rows.subList(0, lastRow + 1);

        int columnCount = 0;
        for (List<Object> row : trimmed) {
            int lastColumn = row.size() - 1;
            while (lastColumn >= 0 && row.get(lastColumn) == null) {
                lastColumn--;
            }
            columnCount = Math.max(columnCount, lastColumn + 1);
        }

        List<List<Object>> result = new ArrayList<>();
        for (List<Object> row : trimmed) {
            List<Object> normalized = new ArrayList<>(Collections.nCopies(columnCount, null));
            for (int column = 0; column < Math.min(columnCount, row.size()); column++) {
                normalized.set(column, row.get(column));
            }
            result.add(normalized);
        }
        return result;
    }

    private boolean isBlank(List<Object> row) {
        for (Object value : row) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }

}
