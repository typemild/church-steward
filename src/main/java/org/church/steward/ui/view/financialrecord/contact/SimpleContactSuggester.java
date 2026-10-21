package org.church.steward.ui.view.financialrecord.contact;

import java.awt.event.KeyEvent;
import java.util.List;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.ui.util.AsyncTask;

import lombok.extern.slf4j.Slf4j;

/**
 * "[구분] 이름"과 비고만 보여주는 단순한 관계자 자동 완성. 검색 조건의 관계자 선택 박스에서 사용한다.
 */
@Slf4j
public class SimpleContactSuggester implements ContactSuggester {

    private static final int AUTOCOMPLETE_LIMIT = 10;

    private final ContactController contactController;
    private final ContactChipField field;
    private final ContactSuggestionPopup popup;

    public SimpleContactSuggester(ContactController contactController, ContactChipField field) {
        this.contactController = contactController;
        this.field = field;
        this.popup = new ContactSuggestionPopup(this::addContact);
    }

    @Override
    public void suggest(String keyword) {
        // 입력 중인 필드가 잠기지 않도록 팝업을 잠금 대상으로 지정한다.
        AsyncTask.run(popup,
                () -> contactController.autocompleteContactSummaries(keyword, AUTOCOMPLETE_LIMIT),
                contacts -> {
                    // 응답을 기다리는 동안 입력이 바뀌었거나 포커스를 잃었으면 결과를 버린다.
                    if (!field.isInputFocusOwner() || !keyword.equals(field.getInputText())) {
                        return;
                    }
                    List<ContactSummaryDto> candidates = contacts.stream()
                            .filter(contact -> !field.containsContact(contact.getId()))
                            .toList();
                    popup.showSuggestions(field, candidates);
                },
                err -> log.error("Failed to autocomplete contacts", err));
    }

    @Override
    public void hide() {
        popup.setVisible(false);
    }

    @Override
    public boolean isShowing() {
        return popup.isVisible();
    }

    @Override
    public boolean handleKey(int keyCode) {
        if (!popup.isVisible()) {
            return false;
        }

        switch (keyCode) {
        case KeyEvent.VK_DOWN -> popup.moveSelection(1);
        case KeyEvent.VK_UP -> popup.moveSelection(-1);
        case KeyEvent.VK_ENTER -> {
            ContactSummaryDto selected = popup.getSelectedContact();
            if (selected != null) {
                addContact(selected);
            }
        }
        case KeyEvent.VK_ESCAPE -> popup.setVisible(false);
        default -> {
            return false;
        }
        }
        return true;
    }

    private void addContact(ContactSummaryDto contact) {
        field.addContact(contact.getId(), contact.getType(), contact.getName(), contact.getRemarks());
    }
}
