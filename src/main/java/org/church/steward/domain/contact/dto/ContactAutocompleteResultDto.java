package org.church.steward.domain.contact.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관계자 자동 완성의 조회 결과. 개수 제한까지의 관계자와, 목록에 다 담지 못한 것까지 센 전체 건수를 담는다.
 *
 * @param <T> 관계자 DTO의 종류
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactAutocompleteResultDto<T> {
    private List<T> contacts;
    private long totalCount;

    /**
     * 목록에 담지 못한 관계자의 수를 반환한다.
     */
    public long getRemainingCount() {
        return totalCount - contacts.size();
    }
}
