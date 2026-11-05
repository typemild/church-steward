package org.church.steward.domain.report.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보고서 양식의 결재란
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ReportApprovalDto {
    /** 결재란을 보고서에 그리는지 여부 */
    private boolean visible;
    /** 칸마다의 직함. 개수가 칸 수이며, 빈 직함(손으로 쓰는 칸)은 null이다. */
    private List<String> titles;
}
