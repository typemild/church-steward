package org.church.steward.domain.report.repository;

import java.util.List;

import org.church.steward.domain.report.entity.ReportTableColumn;
import org.church.steward.domain.report.entity.ReportTableColumnId;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportTableColumnRepository extends JpaRepository<ReportTableColumn, ReportTableColumnId> {

    /**
     * 표의 칸을 순서대로 조회한다.
     */
    List<ReportTableColumn> findAllByTableTypeOrderBySortOrder(ReportTemplateTable tableType);
}
