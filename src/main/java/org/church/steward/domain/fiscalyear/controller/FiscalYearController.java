package org.church.steward.domain.fiscalyear.controller;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.church.steward.domain.user.service.UserSettingService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.UserSessionHolder;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class FiscalYearController {

    private final FiscalYearService fiscalYearService;
    private final UserSettingService userSettingService;
    private final UserSessionHolder userSessionHolder;

    public List<FiscalYearDto> listFiscalYears() {
        return fiscalYearService.listFiscalYears();
    }

    public void createFiscalYear(String name, LocalDate startDate, LocalDate endDate) {
        fiscalYearService.createFiscalYear(name, startDate, endDate);
    }

    public void createFiscalYearWithCopy(String name, LocalDate startDate, LocalDate endDate,
            Long sourceFiscalYearId) {
        fiscalYearService.createFiscalYearWithCopy(name, startDate, endDate, sourceFiscalYearId);
    }

    public void updateFiscalYear(Long id, String name, LocalDate startDate, LocalDate endDate) {
        fiscalYearService.updateFiscalYear(id, name, startDate, endDate);
    }

    public void deleteFiscalYear(Long id) {
        fiscalYearService.deleteFiscalYear(id);
    }

    public void closeFiscalYear(Long id) {
        fiscalYearService.closeFiscalYear(id);
    }

    public void reopenFiscalYear(Long id) {
        fiscalYearService.reopenFiscalYear(id);
    }

    /**
     * 로그인한 사용자가 마지막으로 선택한 회계 연도의 ID를 조회한다.
     */
    public Long getSelectedFiscalYearId() {
        return userSettingService.getSelectedFiscalYearId(getLoggedInUserId());
    }

    /**
     * 로그인한 사용자가 선택한 회계 연도를 저장한다.
     */
    public void updateSelectedFiscalYearId(Long fiscalYearId) {
        userSettingService.updateSelectedFiscalYearId(getLoggedInUserId(), fiscalYearId);
    }

    private String getLoggedInUserId() {
        return userSessionHolder.getLoggedInUser()
                .id();
    }
}
