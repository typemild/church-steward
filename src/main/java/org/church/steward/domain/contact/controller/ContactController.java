package org.church.steward.domain.contact.controller;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactAutocompleteResultDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.service.ContactService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    public List<ContactDto> listContacts(ContactSearchRequestDto searchRequest) {
        return contactService.getContacts(searchRequest);
    }

    public List<ContactDto> getContactsByIds(List<Long> ids) {
        return contactService.getContactsByIds(ids);
    }

    public ContactDto createContact(ContactDto dto) {
        return contactService.createContact(dto);
    }

    public void updateContact(Long id, ContactType type, String name, String remarks, boolean isHidden) {
        contactService.updateContact(id, type, name, remarks, isHidden);
    }

    public void deleteContact(Long id) {
        contactService.deleteContact(id);
    }

    public void hideContact(Long id) {
        contactService.hideContact(id);
    }

    public void unhideContact(Long id) {
        contactService.unhideContact(id);
    }

    public ContactAutocompleteResultDto<ContactAutocompleteDto> autocompleteContacts(String keyword,
            List<Long> excludedIds, int limit) {
        return contactService.autocompleteContacts(keyword, excludedIds, limit);
    }

    public ContactAutocompleteResultDto<ContactSummaryDto> autocompleteContactSummaries(String keyword,
            List<Long> excludedIds, int limit) {
        return contactService.autocompleteContactSummaries(keyword, excludedIds, limit);
    }

}
