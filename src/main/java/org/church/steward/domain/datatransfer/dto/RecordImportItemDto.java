package org.church.steward.domain.datatransfer.dto;

import java.time.LocalDate;
import java.util.List;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 가져오기에서 분석한 파일의 한 행.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordImportItemDto {

    /** 이 건의 식별자. 가져올 건을 고를 때 쓴다. */
    private String key;

    /** 파일의 행 번호 (1부터) */
    private int rowNumber;

    /** 구분. 읽지 못했으면 null */
    private FinancialRecordType type;

    /** 일자. 읽지 못했으면 null */
    private LocalDate date;

    /** 연결된 예산의 ID. 연결되지 않았으면 null */
    private Long budgetId;

    /** 연결된 예산의 전체 이름. 연결되지 않았으면 파일에 적힌 값 */
    private String budgetLabel;

    /** 금액. 읽지 못했으면 null */
    private Long amount;

    /** 저장할 내용 (100자를 넘으면 잘린 값) */
    private String description;

    /** 연결된 관계자의 ID. 하나라도 연결되지 않았으면 빈 목록 */
    private List<Long> contactIds;

    /** 연결된 관계자의 이름을 쉼표로 이은 것. 연결되지 않은 이름은 파일에 적힌 대로 둔다. */
    private String contactLabel;

    /** 저장할 비고 (100자를 넘으면 잘린 값) */
    private String remarks;

    private ImportItemStatus status;

    /** 상태에 대한 설명. 설명할 것이 없으면 빈 문자열 */
    private String message;

    /** 같은 내역이 이미 있어 중복으로 의심되는지 여부. 이런 건은 기본 선택에서 뺀다. */
    private boolean duplicate;

    /**
     * 가져올 대상으로 고를 수 있는 건인지 여부.
     */
    public boolean isSelectable() {
        return status != ImportItemStatus.ERROR;
    }

}
