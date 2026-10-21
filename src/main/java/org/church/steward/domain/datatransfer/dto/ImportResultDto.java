package org.church.steward.domain.datatransfer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 가져오기를 반영한 결과
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultDto {

    /** 새로 추가한 건수 */
    private int createdCount;

    /** 이미 있던 데이터를 갱신한 건수 */
    private int updatedCount;

}
