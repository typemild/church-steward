package org.church.steward.domain.user.service;

public interface UserSettingService {

    /**
     * 사용자가 마지막으로 선택한 회계 연도의 ID를 가져온다. 선택한 적이 없거나 그 회계 연도가 삭제되었으면 가장 최근 회계 연도를 선택한
     * 것으로 저장하고 그 ID를 반환한다.
     *
     * @param userId 사용자 ID
     * @return 선택된 회계 연도의 ID
     */
    Long getSelectedFiscalYearId(String userId);

    /**
     * 사용자가 선택한 회계 연도를 저장한다.
     *
     * @param userId       사용자 ID
     * @param fiscalYearId 선택한 회계 연도의 ID
     */
    void updateSelectedFiscalYearId(String userId, Long fiscalYearId);
}
