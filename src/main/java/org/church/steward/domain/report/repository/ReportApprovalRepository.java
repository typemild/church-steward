package org.church.steward.domain.report.repository;

import org.church.steward.domain.report.entity.ReportApproval;
import org.church.steward.domain.report.entity.ReportType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportApprovalRepository extends JpaRepository<ReportApproval, ReportType> {
}
