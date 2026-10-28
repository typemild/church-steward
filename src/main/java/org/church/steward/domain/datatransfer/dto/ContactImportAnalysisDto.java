package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관계자 가져오기의 분석 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactImportAnalysisDto {

    /** 분석한 행들. 파일에 나온 순서다. */
    private List<ContactImportItemDto> items;

    /** 이름이 비어 있어서 건너뛴 행의 수 (빈 줄 등) */
    private int blankRowCount;

    /**
     * 분석 결과 전체를 요약한 값. 반영할 때 이 값을 함께 넘기면, 그 사이에 데이터가 바뀌어 분석 결과가 달라졌는지를
     * 서비스가 확인한다.
     */
    private String signature;

}
