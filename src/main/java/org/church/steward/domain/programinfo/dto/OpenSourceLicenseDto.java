package org.church.steward.domain.programinfo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OpenSourceLicenseDto {
    private String libraryName;
    private String version;
    private String licenseType;
    private String licenseUrl;
    private String projectUrl;
}
