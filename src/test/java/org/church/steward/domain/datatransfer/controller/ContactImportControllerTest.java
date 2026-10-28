package org.church.steward.domain.datatransfer.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.service.ContactImportService;
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
class ContactImportControllerTest {

    @Mock
    private ContactImportService contactImportService;

    @Mock
    private AdminAccessChecker adminAccessChecker;

    @InjectMocks
    private ContactImportController contactImportController;

    private final ContactImportRequestDto request = ContactImportRequestDto.builder()
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
            ContactImportAnalysisDto expected = ContactImportAnalysisDto.builder()
                    .build();
            when(contactImportService.analyze(request)).thenReturn(expected);

            // When
            ContactImportAnalysisDto actual = contactImportController.analyze(request);

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void analyze_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> contactImportController.analyze(request))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(contactImportService);
        }
    }

    @Nested
    @DisplayName("importContacts 테스트")
    class ImportContactsTest {

        @Test
        @DisplayName("서비스가 반영한 결과를 그대로 반환한다")
        void importContacts_ValidParameters_ReturnsServiceResult() {
            // Given
            Set<String> selectedKeys = Set.of("row:2");
            ImportResultDto expected = ImportResultDto.builder()
                    .build();
            when(contactImportService.importContacts(request, selectedKeys, "signature", 10L, "관계자.csv"))
                    .thenReturn(expected);

            // When
            ImportResultDto actual = contactImportController.importContacts(request, selectedKeys, "signature", 10L,
                    "관계자.csv");

            // Then
            assertThat(actual).isSameAs(expected);
        }

        @Test
        @DisplayName("관리자가 아니면 AccessDeniedException을 던지고 서비스를 호출하지 않는다")
        void importContacts_NotAdmin_ThrowsAccessDeniedException() {
            // Given
            denyAccess();

            // When & Then
            assertThatThrownBy(() -> contactImportController.importContacts(request, Set.of("row:2"), "signature",
                    10L, "관계자.csv"))
                    .isInstanceOf(AccessDeniedException.class);

            verifyNoInteractions(contactImportService);
        }
    }

}
