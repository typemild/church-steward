package org.church.steward.domain.report.repository;

import org.church.steward.domain.report.entity.ReportSetting;
import org.church.steward.domain.report.entity.ReportSettingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportSettingRepository extends JpaRepository<ReportSetting, ReportSettingId> {
}
