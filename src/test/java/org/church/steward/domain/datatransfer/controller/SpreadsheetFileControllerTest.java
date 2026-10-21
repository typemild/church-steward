package org.church.steward.domain.datatransfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.datatransfer.service.SpreadsheetFileService;
import org.church.steward.infrastructure.security.AccessDeniedException;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpreadsheetFileControllerTest {

    private static final Path FILE = Path.of("imports", "예산.xlsx");

    @Mock
    private SpreadsheetFileService spreadsheetFileService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private SpreadsheetFileController spreadsheetFileController;

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("isExcel 테스트")
    class IsExcelTest {

        @Test
        @DisplayName("서비스가 판단한 엑셀 파일 여부를 그대로 반환한다")
        void isExcel_ExcelFile_ReturnsServiceResult() {
            // Given
            when(spreadsheetFileService.isExcel(FILE)).thenReturn(true);

            // When
            boolean excel = spreadsheetFileController.isExcel(FILE);

            // Then
            assertThat(excel).isTrue();
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void isExcel_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> spreadsheetFileController.isExcel(FILE))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(spreadsheetFileService);
        }
    }

    @Nested
    @DisplayName("listSheets 테스트")
    class ListSheetsTest {

        @Test
        @DisplayName("서비스가 읽은 시트 이름 목록을 그대로 반환한다")
        void listSheets_WithPassword_ReturnsServiceResult() {
            // Given
            when(spreadsheetFileService.listSheets(FILE, "secret")).thenReturn(List.of("수입", "지출"));

            // When
            List<String> sheetNames = spreadsheetFileController.listSheets(FILE, "secret");

            // Then
            assertThat(sheetNames).containsExactly("수입", "지출");
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void listSheets_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> spreadsheetFileController.listSheets(FILE, "secret"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(spreadsheetFileService);
        }
    }

    @Nested
    @DisplayName("read 테스트")
    class ReadTest {

        @Test
        @DisplayName("서비스가 읽은 표를 그대로 반환한다")
        void read_WithOptions_ReturnsServiceResult() {
            // Given
            SpreadsheetData expected = new SpreadsheetData(List.of(), null);
            when(spreadsheetFileService.read(FILE, "secret", "수입", StandardCharsets.UTF_8)).thenReturn(expected);

            // When
            SpreadsheetData actual = spreadsheetFileController.read(FILE, "secret", "수입", StandardCharsets.UTF_8);

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void read_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> spreadsheetFileController.read(FILE, "secret", "수입", StandardCharsets.UTF_8))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(spreadsheetFileService);
        }
    }

}
