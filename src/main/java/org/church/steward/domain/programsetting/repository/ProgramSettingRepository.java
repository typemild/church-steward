package org.church.steward.domain.programsetting.repository;

import org.church.steward.domain.programsetting.entity.ProgramSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProgramSettingRepository extends JpaRepository<ProgramSetting, Long> {
}
