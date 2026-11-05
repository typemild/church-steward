package org.church.steward.domain.programsetting.service;

import java.time.DayOfWeek;

import org.church.steward.domain.programsetting.dto.BudgetViewColumnsDto;

public interface ProgramSettingService {

    /**
     * 주간 결산 요일을 가져온다. 정한 적이 없으면 기본값(일요일)을 반환하며 저장하지는 않는다.
     *
     * @return 주간 결산 요일
     */
    DayOfWeek getWeeklyClosingDay();

    /**
     * 주간 결산 요일을 저장한다. 처음 저장하면 설정 행을 새로 만든다.
     *
     * @param weeklyClosingDay 주간 결산 요일
     * @throws IllegalArgumentException 요일이 없는 경우
     */
    void updateWeeklyClosingDay(DayOfWeek weeklyClosingDay);

    /**
     * 수입/지출 관리의 "예산별 보기"에서 수입 표와 지출 표의 칸 설정을 가져온다. 저장한 적이 없거나 저장된 칸이 지금의 칸 구성이나 규칙과
     * 맞지 않는 표는 기본 순서(숨긴 칸 없음)이며, 기본값을 저장하지는 않는다.
     */
    BudgetViewColumnsDto getBudgetViewColumns();

    /**
     * 예산별 보기의 칸 설정을 저장한다. 모든 표를 검증한 뒤에 저장하므로, 한 표라도 규칙에 맞지 않으면 아무것도 바꾸지 않는다.
     *
     * @throws IllegalArgumentException 표나 칸이 빠졌거나, 칸의 구성·첫 번째 칸·숨긴 칸이 규칙에 맞지 않는 경우
     */
    void updateBudgetViewColumns(BudgetViewColumnsDto columns);
}
