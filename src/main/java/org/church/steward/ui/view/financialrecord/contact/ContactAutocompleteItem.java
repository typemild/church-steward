package org.church.steward.ui.view.financialrecord.contact;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;

import lombok.Getter;

/**
 * 계층형 자동 완성 목록의 한 항목. 관계자이거나 맨 아래의 "신규 추가" 항목이다.
 */
@Getter
public class ContactAutocompleteItem {

    /** "신규 추가" 항목이면 null */
    private final ContactAutocompleteDto contact;

    private ContactAutocompleteItem(ContactAutocompleteDto contact) {
        this.contact = contact;
    }

    public static ContactAutocompleteItem ofContact(ContactAutocompleteDto contact) {
        return new ContactAutocompleteItem(contact);
    }

    public static ContactAutocompleteItem newEntry() {
        return new ContactAutocompleteItem(null);
    }

    public boolean isNewEntry() {
        return contact == null;
    }
}
