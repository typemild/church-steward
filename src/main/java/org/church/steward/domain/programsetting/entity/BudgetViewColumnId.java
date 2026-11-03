package org.church.steward.domain.programsetting.entity;

import java.io.Serializable;

import org.church.steward.domain.report.entity.ReportColumn;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link BudgetViewColumn}의 키. 표와 칸의 조합이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BudgetViewColumnId implements Serializable {

    private static final long serialVersionUID = 1L;

    private BudgetViewTable tableType;
    private ReportColumn columnType;
}
