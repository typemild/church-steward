package org.church.steward.domain.datatransfer.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetReader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpreadsheetFileServiceImplTest {

    private static final Path FILE = Path.of("imports", "예산.xlsx");

    @Mock
    private SpreadsheetReader spreadsheetReader;

    @InjectMocks
    private SpreadsheetFileServiceImpl spreadsheetFileService;

    @Nested
    @DisplayName("isExcel 테스트")
    class IsExcelTest {

        @Test
        @DisplayName("파일 읽기 도구가 판단한 엑셀 파일 여부를 그대로 반환한다")
        void isExcel_ExcelFile_ReturnsReaderResult() {
            // Given
            when(spreadsheetReader.isExcel(FILE)).thenReturn(true);

            // When
            boolean excel = spreadsheetFileService.isExcel(FILE);

            // Then
            assertThat(excel).isTrue();
        }
    }

    @Nested
    @DisplayName("listSheets 테스트")
    class ListSheetsTest {

        @Test
        @DisplayName("암호를 넘겨 읽은 시트 이름 목록을 그대로 반환한다")
        void listSheets_WithPassword_ReturnsReaderResult() {
            // Given
            when(spreadsheetReader.listSheets(FILE, "secret")).thenReturn(List.of("수입", "지출"));

            // When
            List<String> sheetNames = spreadsheetFileService.listSheets(FILE, "secret");

            // Then
            assertThat(sheetNames).containsExactly("수입", "지출");
        }
    }

    @Nested
    @DisplayName("read 테스트")
    class ReadTest {

        @Test
        @DisplayName("암호, 시트, 인코딩을 넘겨 읽은 표를 그대로 반환한다")
        void read_WithOptions_ReturnsReaderResult() {
            // Given
            SpreadsheetData expected = new SpreadsheetData(List.of(), null);
            when(spreadsheetReader.read(FILE, "secret", "수입", StandardCharsets.UTF_8)).thenReturn(expected);

            // When
            SpreadsheetData actual = spreadsheetFileService.read(FILE, "secret", "수입", StandardCharsets.UTF_8);

            // Then
            assertThat(actual).isSameAs(expected);
        }
    }

}
