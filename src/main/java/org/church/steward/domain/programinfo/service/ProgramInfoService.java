package org.church.steward.domain.programinfo.service;

import java.util.List;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;

public interface ProgramInfoService {
    ProgramInfoDto getProgramInfo();

    List<OpenSourceLicenseDto> getOpenSourceLicenses();
}
