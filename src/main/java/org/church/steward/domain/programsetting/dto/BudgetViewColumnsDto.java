package org.church.steward.domain.programsetting.dto;

import java.util.List;
import java.util.Map;

import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.RecordOrder;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 관리의 "예산별 보기"에서 수입 표와 지출 표의 칸 설정(칸의 순서와 숨김, 같은 날짜의 내역끼리의 순서). 프로그램 전체에
 * 하나이며 모든 사용자가 같은 설정을 쓴다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class BudgetViewColumnsDto {
    /** 표마다 칸의 순서와 숨김. 숨긴 칸도 모두 들어 있다. */
    private Map<BudgetViewTable, List<ReportColumnDto>> tables;
    /** 표마다 같은 날짜의 내역끼리의 순서 */
    private Map<BudgetViewTable, RecordOrder> recordOrders;
}
