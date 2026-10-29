package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예산 가져오기의 분석 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetImportAnalysisDto {

    /** 분석한 건들. 파일에 나온 순서이며 상위 예산이 하위 예산보다 먼저 온다. */
    private List<BudgetImportItemDto> items;

    /** 예산명이 없어서 건너뛴 행의 수 (빈 줄 등) */
    private int blankRowCount;

    /**
     * 분석 결과 전체를 요약한 값. 반영할 때 이 값을 함께 넘기면, 그 사이에 데이터가 바뀌어 분석 결과가 달라졌는지를 서비스가 확인한다.
     */
    private String signature;

}
