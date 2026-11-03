package org.church.steward.domain.programsetting.repository;

import java.util.List;

import org.church.steward.domain.programsetting.entity.BudgetViewColumn;
import org.church.steward.domain.programsetting.entity.BudgetViewColumnId;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetViewColumnRepository extends JpaRepository<BudgetViewColumn, BudgetViewColumnId> {

    /**
     * 표의 칸을 순서대로 조회한다.
     */
    List<BudgetViewColumn> findAllByTableTypeOrderBySortOrder(BudgetViewTable tableType);
}
