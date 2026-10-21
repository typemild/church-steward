package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 가져오기의 분석 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordImportAnalysisDto {

    /** 분석한 행들. 파일에 나온 순서다. */
    private List<RecordImportItemDto> items;

    /** 구분 열에 나온 값들과 그 연결. 처음 나온 순서다. 구분을 열에서 읽지 않으면 빈 목록 */
    @Builder.Default
    private List<ImportMappingDto> typeMappings = List.of();

    /** 파일에 나온 예산 값들과 그 연결. 처음 나온 순서다. */
    private List<ImportMappingDto> budgetMappings;

    /** 파일에 나온 관계자 이름들(과 빈 칸)과 그 연결. 처음 나온 순서다. */
    private List<ImportMappingDto> contactMappings;

    /** 날짜와 금액이 모두 없어서 건너뛴 행의 수 (합계, 빈 줄 등) */
    private int blankRowCount;

    /**
     * 분석 결과 전체를 요약한 값. 반영할 때 이 값을 함께 넘기면, 그 사이에 데이터가 바뀌어 분석 결과가 달라졌는지를
     * 서비스가 확인한다.
     */
    private String signature;

}
