package org.church.steward.domain.programinfo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProgramInfoDto {
    private String version;
    private String license;
    private String licenseTextEn;
    private String licenseTextKo;
    private String downloadUrl;
    private String author;
}
