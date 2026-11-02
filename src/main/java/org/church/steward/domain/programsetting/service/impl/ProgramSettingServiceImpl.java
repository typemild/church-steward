package org.church.steward.domain.programsetting.service.impl;

import java.time.DayOfWeek;

import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.domain.programsetting.repository.ProgramSettingRepository;
import org.church.steward.domain.programsetting.service.ProgramSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

/**
 * 프로그램 전체 설정. 화면의 기준을 정하는 설정이므로 활동 기록을 남기지 않는다(보고서 설정과 같다).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProgramSettingServiceImpl implements ProgramSettingService {

    private final ProgramSettingRepository programSettingRepository;

    @Override
    @Transactional(readOnly = true)
    public DayOfWeek getWeeklyClosingDay() {
        return findOrDefaults().getWeeklyClosingDay();
    }

    @Override
    public void updateWeeklyClosingDay(DayOfWeek weeklyClosingDay) {
        if (weeklyClosingDay == null) {
            throw new IllegalArgumentException("주간 결산 요일을 골라 주세요.");
        }

        ProgramSetting setting = findOrDefaults();
        setting.update(weeklyClosingDay);
        programSettingRepository.save(setting);
    }

    private ProgramSetting findOrDefaults() {
        return programSettingRepository.findById(ProgramSettingConstraints.SETTING_ID)
                .orElseGet(ProgramSetting::defaults);
    }
}
