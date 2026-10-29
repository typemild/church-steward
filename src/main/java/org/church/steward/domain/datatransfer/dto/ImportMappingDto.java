package org.church.steward.domain.datatransfer.dto;

import java.util.List;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 가져오기에서 파일의 값 하나를 구분(수입/지출), 등록된 예산 또는 관계자에 연결한 결과. 같은 값은 여러 행에 나와도 한
 * 항목이며, 여기서 정한 연결이 그 값을 쓰는 모든 행에 적용된다.
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

    /** 연결된 예산의 ID. 연결되지 않았거나 구분·관계자를 연결하는 항목이면 null */
    private Long targetId;

    /**
     * 연결된 관계자들의 ID. 관계자 칸 전체("홍길동, 김철수")가 한 항목이며, 칸에 적힌 이름마다 찾은 관계자가 순서대로 담긴다. 수입은
     * 이름 하나도 여러 명(예: "홍길동 가정" → 가족 구성원)에 연결할 수 있다. 연결되지 않았거나 구분·예산을 연결하는 항목이면 빈 목록
     */
    @Builder.Default
    private List<Long> targetIds = List.of();

    /**
     * 관계자 칸의 이름 중 일부만 자동으로 찾아 연결되지 않은 항목에서, 찾은 관계자들의 ID. 직접 매핑할 때 미리 넣어 두는 데 쓴다. 그
     * 밖의 항목이면 빈 목록
     */
    @Builder.Default
    private List<Long> suggestedTargetIds = List.of();

    /** 관계자 칸의 이름 중 자동으로 찾지 못한 이름들. 모두 찾았거나 직접 매핑했거나 관계자 항목이 아니면 빈 목록 */
    @Builder.Default
    private List<String> unresolvedNames = List.of();

    /** 구분을 연결하는 항목에서, 파일의 값을 읽을 구분. 연결되지 않았거나 예산·관계자를 연결하는 항목이면 null */
    private FinancialRecordType targetType;

    /** 연결된 예산의 전체 이름, 관계자의 이름 또는 구분의 이름. 연결되지 않았으면 빈 문자열 */
    private String targetLabel;

    /** 사용자가 직접 연결했는지 여부. false이면 자동으로 연결되었거나 연결되지 않은 것이다. */
    private boolean manual;

    /** 자동으로 연결하지 못한 사유 또는 확인할 점(동명이인 등). 없으면 빈 문자열 */
    private String note;

    public boolean isMapped() {
        return targetId != null || targetType != null || !targetIds.isEmpty();
    }

}
