package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import org.church.steward.domain.budget.entity.BudgetType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예산 가져오기에서 분석한 한 건. 파일의 한 행이거나, 하위 예산의 경로에만 나와서 행 없이 만들어진 상위 예산이다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetImportItemDto {

    /** 이 건의 식별자. 가져올 건을 고를 때 쓴다. */
    private String key;

    /** 상위 예산인 건의 식별자. 최상위 예산이거나 예산으로 읽지 못한 행이면 null */
    private String parentKey;

    /** 파일의 행 번호 (1부터). 행 없이 만들어진 상위 예산이면 null */
    private Integer rowNumber;

    /** 구분. 읽지 못했으면 null */
    private BudgetType type;

    /** 최상위 예산부터 이 예산까지의 이름. 읽지 못했으면 빈 목록 */
    private List<String> levelNames;

    /** 저장할 금액. 하위 예산이 있으면 하위 예산의 합계 */
    private Long amount;

    /** 파일 안에서 하위 예산이 없는 예산인지 여부 */
    private boolean leaf;

    /** 이미 있는 예산이면 그 예산의 ID, 없으면 null */
    private Long existingBudgetId;

    private ImportItemStatus status;

    private BudgetImportAction action;

    /** 상태에 대한 설명. 설명할 것이 없으면 빈 문자열 */
    private String message;

    /**
     * 가져올 대상으로 고를 수 있는 건인지 여부. 반영할 것이 있고 오류가 없어야 한다.
     */
    public boolean isSelectable() {
        return action != BudgetImportAction.NONE && status != ImportItemStatus.ERROR;
    }

}
