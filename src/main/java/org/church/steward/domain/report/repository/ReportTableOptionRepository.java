package org.church.steward.domain.report.repository;

import org.church.steward.domain.report.entity.ReportTableOption;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTableOptionRepository extends JpaRepository<ReportTableOption, ReportTemplateTable> {
}
