package org.church.steward.domain.programinfo.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ProgramInfoServiceImplTest {

    @Mock
    private ResourceLoader resourceLoader;

    @Spy
    private final ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ProgramInfoServiceImpl programInfoService;

    @Nested
    @DisplayName("getProgramInfo 테스트")
    class GetProgramInfoTest {

        @Test
        @DisplayName("getProgramInfo_설정된값존재_정상반환")
        void getProgramInfo_valuesExist_returnsCorrectDto() throws Exception {
            // Given
            ReflectionTestUtils.setField(programInfoService, "version", "1.0.0");
            ReflectionTestUtils.setField(programInfoService, "license", "MIT");
            ReflectionTestUtils.setField(programInfoService, "downloadUrl", "https://example.com");
            ReflectionTestUtils.setField(programInfoService, "author", "Tester");

            // 영문 라이선스 Mocking
            Resource licenseResource = mock(Resource.class);
            when(resourceLoader.getResource("classpath:LICENSE")).thenReturn(licenseResource);
            when(licenseResource.exists()).thenReturn(true);
            when(licenseResource.getInputStream())
                    .thenReturn(new ByteArrayInputStream("English License Content".getBytes(StandardCharsets.UTF_8)));

            // 국문 라이선스 Mocking
            Resource licenseResourceKo = mock(Resource.class);
            when(resourceLoader.getResource("classpath:LICENSE-ko")).thenReturn(licenseResourceKo);
            when(licenseResourceKo.exists()).thenReturn(true);
            when(licenseResourceKo.getInputStream())
                    .thenReturn(new ByteArrayInputStream("한국어 라이선스 내용".getBytes(StandardCharsets.UTF_8)));

            // When
            ProgramInfoDto result = programInfoService.getProgramInfo();

            // Then
            assertThat(result.getVersion()).isEqualTo("1.0.0");
            assertThat(result.getLicense()).isEqualTo("MIT");
            assertThat(result.getLicenseTextEn()).isEqualTo("English License Content");
            assertThat(result.getLicenseTextKo()).isEqualTo("한국어 라이선스 내용");
            assertThat(result.getDownloadUrl()).isEqualTo("https://example.com");
            assertThat(result.getAuthor()).isEqualTo("Tester");
        }

        @Test
        @DisplayName("getProgramInfo_국문파일없음_영문만반환")
        void getProgramInfo_noKoreanLicense_returnsEnglishOnly() throws Exception {
            // Given
            ReflectionTestUtils.setField(programInfoService, "version", "1.0.0");
            ReflectionTestUtils.setField(programInfoService, "license", "MIT");
            ReflectionTestUtils.setField(programInfoService, "downloadUrl", "url");
            ReflectionTestUtils.setField(programInfoService, "author", "author");

            Resource licenseResource = mock(Resource.class);
            when(resourceLoader.getResource("classpath:LICENSE")).thenReturn(licenseResource);
            when(licenseResource.exists()).thenReturn(true);
            when(licenseResource.getInputStream())
                    .thenReturn(new ByteArrayInputStream("English".getBytes(StandardCharsets.UTF_8)));

            // 국문 파일은 존재하지 않음
            Resource licenseResourceKo = mock(Resource.class);
            when(resourceLoader.getResource("classpath:LICENSE-ko")).thenReturn(licenseResourceKo);
            when(licenseResourceKo.exists()).thenReturn(false);

            // When
            ProgramInfoDto result = programInfoService.getProgramInfo();

            // Then
            assertThat(result.getLicenseTextEn()).isEqualTo("English");
            assertThat(result.getLicenseTextKo()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getOpenSourceLicenses 테스트")
    class GetOpenSourceLicensesTest {

        @Test
        @DisplayName("getOpenSourceLicenses_리포트파일존재_정상파싱")
        void getOpenSourceLicenses_reportExists_returnsParsedLicenses() throws Exception {
            // Given
            String json = """
                    {
                      "dependencies":[
                        {
                          "moduleName": "Library A",
                          "moduleVersion": "1.2.3",
                          "moduleLicense": "Apache 2.0",
                          "moduleUrl": "https://lib-a.com",
                          "moduleLicenseUrl": "https://apache.org"
                        }
                      ]
                    }
                    """;
            InputStream inputStream = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
            Resource resource = mock(Resource.class);

            when(resourceLoader.getResource(any())).thenReturn(resource);
            when(resource.exists()).thenReturn(true);
            when(resource.getInputStream()).thenReturn(inputStream);

            // When
            List<OpenSourceLicenseDto> result = programInfoService.getOpenSourceLicenses();

            // Then
            // 리포트의 라이브러리 뒤에 프로그램에 포함한 글꼴이 붙는다.
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getLibraryName()).isEqualTo("Library A");
            assertThat(result.get(0).getVersion()).isEqualTo("1.2.3");
            assertThat(result.get(0).getLicenseType()).isEqualTo("Apache 2.0");
            assertThat(result.get(0).getLicenseUrl()).isEqualTo("https://apache.org");
            assertThat(result.get(0).getProjectUrl()).isEqualTo("https://lib-a.com");
        }

        @Test
        @DisplayName("getOpenSourceLicenses_리포트파일없음_포함된글꼴만반환")
        void getOpenSourceLicenses_reportDoesNotExist_returnsBundledFontOnly() throws Exception {
            // Given
            Resource resource = mock(Resource.class);
            when(resourceLoader.getResource(any())).thenReturn(resource);
            when(resource.exists()).thenReturn(false);

            // When
            List<OpenSourceLicenseDto> result = programInfoService.getOpenSourceLicenses();

            // Then
            assertThat(result).extracting(OpenSourceLicenseDto::getLicenseType)
                    .containsExactly("SIL Open Font License 1.1");
        }

        @Test
        @DisplayName("getOpenSourceLicenses_파싱에러발생_포함된글꼴만반환")
        void getOpenSourceLicenses_parsingError_returnsBundledFontOnly() throws Exception {
            // Given
            Resource resource = mock(Resource.class);
            when(resourceLoader.getResource(any())).thenReturn(resource);
            when(resource.exists()).thenReturn(true);
            when(resource.getInputStream()).thenThrow(new RuntimeException("IO Error"));

            // When
            List<OpenSourceLicenseDto> result = programInfoService.getOpenSourceLicenses();

            // Then
            assertThat(result).extracting(OpenSourceLicenseDto::getLicenseType)
                    .containsExactly("SIL Open Font License 1.1");
        }
    }
}
