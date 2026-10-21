package org.church.steward.domain.user.repository;

import org.church.steward.domain.user.entity.UserSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserSettingRepository extends JpaRepository<UserSetting, String> {
}
