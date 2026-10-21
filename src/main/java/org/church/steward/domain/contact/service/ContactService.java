package org.church.steward.domain.contact.service;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.entity.ContactType;

public interface ContactService {
    List<ContactDto> getContacts(ContactSearchRequestDto searchRequest);

    /**
     * ID 목록에 해당하는 관계자의 현재 정보를 조회한다. 숨긴 관계자도 포함하며, 존재하지 않는 ID는 결과에서 빠진다.
     */
    List<ContactDto> getContactsByIds(List<Long> ids);

    ContactDto createContact(ContactDto dto);

    void updateContact(Long id, ContactType type, String name, String remarks, boolean isHidden);

    void deleteContact(Long id);

    void hideContact(Long id);

    void unhideContact(Long id);

    List<ContactAutocompleteDto> autocompleteContacts(String keyword, int limit);

    List<ContactSummaryDto> autocompleteContactSummaries(String keyword, int limit);
}
