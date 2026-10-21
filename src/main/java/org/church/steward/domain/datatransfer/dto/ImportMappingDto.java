package org.church.steward.domain.datatransfer.dto;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 가져오기에서 파일의 값 하나를 구분(수입/지출), 등록된 예산 또는 관계자에 연결한 결과.
 * 같은 값은 여러 행에 나와도 한 항목이며, 여기서 정한 연결이 그 값을 쓰는 모든 행에 적용된다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportMappingDto {

    /** 이 항목의 식별자. 직접 매핑할 때 이 값을 키로 넘긴다. */
    private String key;

    /** 파일에 적힌 값. 빈 칸이면 "(빈 칸)" */
    private String label;

    /** 파일의 칸이 비어 있는 행들을 모은 항목인지 여부. 이때 label은 "(빈 칸)"이다. */
    private boolean blank;

    /** 구분("수입"/"지출"). 구분을 연결하는 항목이면 빈 문자열 */
    private String typeLabel;

    /** 이 값을 쓰는 행의 수 */
    private int count;

    /** 이 값을 쓰는 행의 금액 합계 */
    private long totalAmount;

    /** 연결된 예산 또는 관계자의 ID. 연결되지 않았거나 구분을 연결하는 항목이면 null */
    private Long targetId;

    /** 구분을 연결하는 항목에서, 파일의 값을 읽을 구분. 연결되지 않았거나 예산·관계자를 연결하는 항목이면 null */
    private FinancialRecordType targetType;

    /** 연결된 예산의 전체 이름, 관계자의 이름 또는 구분의 이름. 연결되지 않았으면 빈 문자열 */
    private String targetLabel;

    /** 사용자가 직접 연결했는지 여부. false이면 자동으로 연결되었거나 연결되지 않은 것이다. */
    private boolean manual;

    /** 자동으로 연결하지 못한 사유 또는 확인할 점(동명이인 등). 없으면 빈 문자열 */
    private String note;

    public boolean isMapped() {
        return targetId != null || targetType != null;
    }

}
