package org.church.steward.ui.view.financialrecord.contact;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;

import lombok.Getter;

/**
 * 계층형 자동 완성 목록의 한 항목. 관계자, "신규 추가", 맨 아래의 다 담지 못한 관계자가 있다는 안내 중 하나다. 안내 항목은
 * 키보드로 고를 수 없고, 마우스로 누르면 모두 보여준다.
 */
@Getter
public class ContactAutocompleteItem {

    /** 관계자 항목이 아니면 null */
    private final ContactAutocompleteDto contact;
    /** 안내 항목이면 목록에 담지 못한 관계자의 수, 아니면 0 */
    private final long remainingCount;

    private ContactAutocompleteItem(ContactAutocompleteDto contact, long remainingCount) {
        this.contact = contact;
        this.remainingCount = remainingCount;
    }

    public static ContactAutocompleteItem ofContact(ContactAutocompleteDto contact) {
        return new ContactAutocompleteItem(contact, 0);
    }

    public static ContactAutocompleteItem moreResults(long remainingCount) {
        return new ContactAutocompleteItem(null, remainingCount);
    }

    public static ContactAutocompleteItem newEntry() {
        return new ContactAutocompleteItem(null, 0);
    }

    public boolean isMoreResults() {
        return contact == null && remainingCount > 0;
    }

    public boolean isNewEntry() {
        return contact == null && remainingCount == 0;
    }
}
