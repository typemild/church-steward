package org.church.steward.domain.datatransfer.dto;

import org.church.steward.domain.contact.entity.ContactType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관계자 가져오기에서 분석한 파일의 한 행.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactImportItemDto {

    /** 이 건의 식별자. 가져올 건을 고를 때 쓴다. */
    private String key;

    /** 파일의 행 번호 (1부터) */
    private int rowNumber;

    /** 구분. 읽지 못했으면 null */
    private ContactType type;

    /** 이름 */
    private String name;

    /** 저장할 비고 (100자를 넘으면 잘린 값). 비어 있으면 null */
    private String remarks;

    /** 숨김 여부 */
    private boolean hidden;

    private ImportItemStatus status;

    /** 상태에 대한 설명. 설명할 것이 없으면 빈 문자열 */
    private String message;

    /** 같은 관계자가 이미 있거나 파일의 앞쪽에 있어 중복으로 의심되는지 여부. 이런 건은 기본 선택에서 뺀다. */
    private boolean duplicate;

    /**
     * 가져올 대상으로 고를 수 있는 건인지 여부.
     */
    public boolean isSelectable() {
        return status != ImportItemStatus.ERROR;
    }

}
