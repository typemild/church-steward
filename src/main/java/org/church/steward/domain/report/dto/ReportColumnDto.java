package org.church.steward.domain.report.dto;

import org.church.steward.domain.report.entity.ReportColumn;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보고서 양식에서 표의 칸 하나. 칸의 순서는 목록의 순서다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportColumnDto {
    private ReportColumn column;
    private boolean hidden;
}
