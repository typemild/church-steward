package org.church.steward.domain.contact.service;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactAutocompleteResultDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;

public interface ContactService {
    List<ContactDto> getContacts(ContactSearchRequestDto searchRequest);

    /**
     * ID 목록에 해당하는 관계자의 현재 정보를 조회한다. 숨긴 관계자도 포함하며, 존재하지 않는 ID는 결과에서 빠진다.
     */
    List<ContactDto> getContactsByIds(List<Long> ids);

    ContactDto createContact(ContactDto dto);

    /**
     * 활동 기록을 남기지 않고 관계자를 등록한다. 검증은 {@link #createContact}와 같다. 여러 관계자를 한꺼번에 처리하고 활동
     * 기록을 한 건으로 남기는 가져오기에서만 쓴다.
     *
     * @return 등록된 관계자
     */
    Contact createContactWithoutLog(ContactDto dto);

    void updateContact(Long id, ContactType type, String name, String remarks, boolean isHidden);

    void deleteContact(Long id);

    void hideContact(Long id);

    void unhideContact(Long id);

    /**
     * 수입/지출 입력의 자동 완성 후보를 소속 그룹과 함께 조회한다. 숨긴 관계자와 이미 입력된 관계자({@code excludedIds})는
     * 빼고, 이름이나 비고에 검색어가 들어 있는 관계자를 이름 일치 → 이름이 검색어로 시작 → 이름에 검색어 포함 → 비고에 검색어 포함
     * 순(같은 순위는 이름순)으로 {@code limit}명까지 담는다.
     */
    ContactAutocompleteResultDto<ContactAutocompleteDto> autocompleteContacts(String keyword, List<Long> excludedIds,
            int limit);

    /**
     * 검색 조건의 자동 완성 후보를 조회한다. 조건과 순서는
     * {@link #autocompleteContacts(String, List, int)}와 같고 소속 그룹은 담지 않는다.
     */
    ContactAutocompleteResultDto<ContactSummaryDto> autocompleteContactSummaries(String keyword,
            List<Long> excludedIds, int limit);
}
