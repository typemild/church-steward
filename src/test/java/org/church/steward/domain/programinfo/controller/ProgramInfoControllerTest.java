package org.church.steward.domain.programinfo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.util.List;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.domain.programinfo.service.ProgramInfoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProgramInfoControllerTest {

    @Mock
    private ProgramInfoService programInfoService;

    @InjectMocks
    private ProgramInfoController programInfoController;

    @Nested
    @DisplayName("getProgramInfo 테스트")
    class getProgramInfoTest {

        @Test
        @DisplayName("getProgramInfo_ServiceReturnsData_ReturnsDto")
        void getProgramInfo_ServiceReturnsData_ReturnsDto() {
            // Given
            ProgramInfoDto expectedDto = ProgramInfoDto.builder()
                    .version("1.0.0")
                    .build();
            given(programInfoService.getProgramInfo()).willReturn(expectedDto);

            // When
            ProgramInfoDto actualDto = programInfoController.getProgramInfo();

            // Then
            assertThat(actualDto).isEqualTo(expectedDto);
            assertThat(actualDto.getVersion()).isEqualTo("1.0.0");
        }
    }

    @Nested
    @DisplayName("getOpenSourceLicenses 테스트")
    class getOpenSourceLicensesTest {

        @Test
        @DisplayName("getOpenSourceLicenses_ServiceReturnsList_ReturnsLicenseList")
        void getOpenSourceLicenses_ServiceReturnsList_ReturnsLicenseList() {
            // Given
            List<OpenSourceLicenseDto> expectedLicenses = List.of(
                    OpenSourceLicenseDto.builder()
                            .libraryName("Library A")
                            .build(),
                    OpenSourceLicenseDto.builder()
                            .libraryName("Library B")
                            .build());
            given(programInfoService.getOpenSourceLicenses()).willReturn(expectedLicenses);

            // When
            List<OpenSourceLicenseDto> actualLicenses = programInfoController.getOpenSourceLicenses();

            // Then
            assertThat(actualLicenses).hasSize(2);
            assertThat(actualLicenses).isEqualTo(expectedLicenses);
            assertThat(actualLicenses.get(0).getLibraryName()).isEqualTo("Library A");
        }
    }
}
