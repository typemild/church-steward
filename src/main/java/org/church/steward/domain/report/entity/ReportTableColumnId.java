package org.church.steward.domain.report.entity;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * {@link ReportTableColumn}의 키. 표와 칸의 조합이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportTableColumnId implements Serializable {

    private static final long serialVersionUID = 1L;

    private ReportTemplateTable tableType;
    private ReportColumn columnType;
}
