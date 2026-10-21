package org.church.steward.domain.datatransfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;
import org.church.steward.domain.datatransfer.service.RecordImportService;
import org.church.steward.infrastructure.security.AccessDeniedException;
import org.church.steward.infrastructure.security.AdminAccessChecker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecordImportControllerTest {

    private static final Long FISCAL_YEAR_ID = 7L;

    @Mock
    private RecordImportService recordImportService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private RecordImportController recordImportController;

    private final RecordImportRequestDto request = RecordImportRequestDto.builder()
            .build();

    private void denyAccess() {
        doThrow(new AccessDeniedException("관리자 권한이 필요합니다.")).when(adminAccessChecker).checkAdmin();
    }

    @Nested
    @DisplayName("analyze 테스트")
    class AnalyzeTest {

        @Test
        @DisplayName("서비스가 분석한 결과를 그대로 반환한다")
        void analyze_ValidParameters_ReturnsServiceResult() {
            // Given
            RecordImportAnalysisDto expected = RecordImportAnalysisDto.builder()
                    .build();
            when(recordImportService.analyze(FISCAL_YEAR_ID, request)).thenReturn(expected);

            // When
            RecordImportAnalysisDto actual = recordImportController.analyze(FISCAL_YEAR_ID, request);

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void analyze_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> recordImportController.analyze(FISCAL_YEAR_ID, request))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(recordImportService);
        }
    }

    @Nested
    @DisplayName("importRecords 테스트")
    class ImportRecordsTest {

        @Test
        @DisplayName("서비스가 반영한 결과를 그대로 반환한다")
        void importRecords_ValidParameters_ReturnsServiceResult() {
            // Given
            Set<String> selectedKeys = Set.of("row:2");
            ImportResultDto expected = ImportResultDto.builder()
                    .build();
            when(recordImportService.importRecords(FISCAL_YEAR_ID, request, selectedKeys, "signature", 10L,
                    "수입지출.xlsx")).thenReturn(expected);

            // When
            ImportResultDto actual = recordImportController.importRecords(FISCAL_YEAR_ID, request, selectedKeys,
                    "signature", 10L, "수입지출.xlsx");

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void importRecords_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> recordImportController.importRecords(FISCAL_YEAR_ID, request,
                    Set.of("row:2"), "signature", 10L, "수입지출.xlsx"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(recordImportService);
        }
    }

}
