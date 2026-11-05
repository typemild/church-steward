package org.church.steward.domain.programsetting.repository;

import org.church.steward.domain.programsetting.entity.BudgetViewOption;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetViewOptionRepository extends JpaRepository<BudgetViewOption, BudgetViewTable> {
}
