package org.church.steward.domain.programinfo.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.domain.programinfo.service.ProgramInfoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgramInfoServiceImpl implements ProgramInfoService {

    /**
     * 라이선스 리포트에 나오지 않는 글꼴과 아이콘. 직접 목록에 넣는다.
     * <ul>
     * <li>보고서에 쓰려고 프로그램에 포함한 글꼴(src/main/resources/fonts/nanumgothic,
     * nanummyeongjo). 라이브러리가 아니어서 리포트에 나오지 않는다.</li>
     * <li>화면의 아이콘인 Font Awesome Free. ikonli-fontawesome5-pack의 jar에 글꼴 파일이 들어 있지만,
     * 리포트에는 Ikonli 자신의 라이선스(Apache-2.0)만 나온다. Ikonli를 올려 포함된 Font Awesome의 버전이 바뀌면
     * 여기의 버전도 고친다(jar의 META-INF/resources/fontawesome5/{버전}).</li>
     * </ul>
     */
    private static final List<OpenSourceLicenseDto> BUNDLED_FONTS = List.of(
            OpenSourceLicenseDto.builder()
                    .libraryName("나눔고딕 글꼴 (NanumGothic, 보고서에 사용)")
                    .version("3.020")
                    .licenseType("SIL Open Font License 1.1")
                    .licenseUrl("https://openfontlicense.org")
                    .projectUrl("https://hangeul.naver.com/font")
                    .build(),
            OpenSourceLicenseDto.builder()
                    .libraryName("나눔명조 글꼴 (NanumMyeongjo, 보고서에 사용)")
                    .version("2.032")
                    .licenseType("SIL Open Font License 1.1")
                    .licenseUrl("https://openfontlicense.org")
                    .projectUrl("https://hangeul.naver.com/font")
                    .build(),
            OpenSourceLicenseDto.builder()
                    .libraryName("Font Awesome Free (화면의 아이콘, Ikonli에 포함)")
                    .version("5.15.3")
                    .licenseType("CC BY 4.0 (아이콘), SIL Open Font License 1.1 (글꼴)")
                    .licenseUrl("https://fontawesome.com/license/free")
                    .projectUrl("https://fontawesome.com")
                    .build());

    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${program.info.version}")
    private String version;

    @Value("${program.info.license}")
    private String license;

    @Value("${program.info.download-url}")
    private String downloadUrl;

    @Value("${program.info.author}")
    private String author;

    @Override
    public ProgramInfoDto getProgramInfo() {
        String licenseTextEn = "";
        try {
            Resource resource = resourceLoader.getResource("classpath:LICENSE");
            if (resource.exists()) {
                licenseTextEn = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.error("Failed to load LICENSE file", e);
        }

        String licenseTextKo = "";
        try {
            Resource resourceKo = resourceLoader.getResource("classpath:LICENSE-ko");
            if (resourceKo.exists()) {
                licenseTextKo = StreamUtils.copyToString(resourceKo.getInputStream(), StandardCharsets.UTF_8);
            }
        } catch (Exception e) {
            log.warn("Failed to load LICENSE-ko file, proceeding with default license", e);
        }

        return ProgramInfoDto.builder()
                .version(version)
                .license(license)
                .licenseTextEn(licenseTextEn)
                .licenseTextKo(licenseTextKo)
                .downloadUrl(downloadUrl)
                .author(author)
                .build();
    }

    @Override
    public List<OpenSourceLicenseDto> getOpenSourceLicenses() {
        List<OpenSourceLicenseDto> licenses = new ArrayList<>(loadDependencyLicenses());
        licenses.addAll(BUNDLED_FONTS);
        return licenses;
    }

    /**
     * 빌드할 때 만들어 둔 라이선스 리포트에서 의존 라이브러리의 라이선스를 읽는다. 읽지 못하면 빈 목록을 반환한다.
     */
    private List<OpenSourceLicenseDto> loadDependencyLicenses() {
        try {
            // build.gradle에서 설정한 대로 클래스패스 내의 고정된 경로에서 리소스를 읽는다.
            Resource resource = resourceLoader.getResource("classpath:META-INF/resources/license-report.json");

            if (!resource.exists()) {
                log.warn("License report file not found at classpath:META-INF/resources/license-report.json");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(resource.getInputStream());
            List<OpenSourceLicenseDto> licenses = new ArrayList<>();

            JsonNode dependencies = root.path("dependencies");
            if (dependencies.isArray()) {
                for (JsonNode dep : dependencies) {
                    licenses.add(OpenSourceLicenseDto.builder()
                            .libraryName(dep.path("moduleName").asString())
                            .version(dep.path("moduleVersion").asString())
                            .licenseType(dep.path("moduleLicense").asString())
                            .licenseUrl(dep.path("moduleLicenseUrl").asString())
                            .projectUrl(dep.path("moduleUrl").asString())
                            .build());
                }
            }

            return licenses;
        } catch (Exception e) {
            log.error("Failed to load open source licenses from JSON report", e);
            return Collections.emptyList();
        }
    }

}
