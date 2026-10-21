package org.church.steward.domain.programinfo.controller;

import java.util.List;

import org.church.steward.domain.programinfo.dto.OpenSourceLicenseDto;
import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.domain.programinfo.service.ProgramInfoService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ProgramInfoController {

    private final ProgramInfoService programInfoService;

    public ProgramInfoDto getProgramInfo() {
        return programInfoService.getProgramInfo();
    }

    public List<OpenSourceLicenseDto> getOpenSourceLicenses() {
        return programInfoService.getOpenSourceLicenses();
    }
}
