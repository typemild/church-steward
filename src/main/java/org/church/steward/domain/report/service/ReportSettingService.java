package org.church.steward.domain.report.service;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportType;

public interface ReportSettingService {

    /**
     * 사용자가 보고서에 대해 정한 설정을 가져온다. 정한 적이 없으면 기본 설정을 반환하며 저장하지는 않는다.
     *
     * @param userId     사용자 ID
     * @param reportType 보고서 종류
     */
    ReportSettingDto getReportSetting(String userId, ReportType reportType);

    /**
     * 사용자가 보고서에 대해 정한 설정을 저장한다. 처음 저장하면 새로 만든다.
     *
     * @param userId  사용자 ID
     * @param setting 저장할 설정. 보고서 종류를 포함한다.
     * @throws IllegalArgumentException 값이 허용 범위를 벗어난 경우
     */
    void updateReportSetting(String userId, ReportSettingDto setting);
}
