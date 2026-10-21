package org.church.steward.domain.user.service.impl;

import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.church.steward.domain.user.entity.User;
import org.church.steward.domain.user.entity.UserSetting;
import org.church.steward.domain.user.repository.UserRepository;
import org.church.steward.domain.user.repository.UserSettingRepository;
import org.church.steward.domain.user.service.UserSettingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class UserSettingServiceImpl implements UserSettingService {

    private final UserSettingRepository userSettingRepository;
    private final UserRepository userRepository;
    private final FiscalYearService fiscalYearService;

    @Override
    public Long getSelectedFiscalYearId(String userId) {
        Long selectedFiscalYearId = userSettingRepository.findById(userId)
                .map(UserSetting::getSelectedFiscalYearId)
                .orElse(null);
        if (fiscalYearService.existsById(selectedFiscalYearId)) {
            return selectedFiscalYearId;
        }

        Long latestFiscalYearId = fiscalYearService.getLatestFiscalYearId();
        updateSelectedFiscalYearId(userId, latestFiscalYearId);
        return latestFiscalYearId;
    }

    @Override
    public void updateSelectedFiscalYearId(String userId, Long fiscalYearId) {
        UserSetting userSetting = userSettingRepository.findById(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
                    return UserSetting.builder()
                            .user(user)
                            .build();
                });

        userSetting.setSelectedFiscalYearId(fiscalYearId);
        userSettingRepository.save(userSetting);
    }

}
