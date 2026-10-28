package org.church.steward.ui.view.financialrecord.contact;

import java.awt.event.KeyEvent;
import java.util.List;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.ui.util.AsyncTask;

import lombok.extern.slf4j.Slf4j;

/**
 * "[구분] 이름"과 비고만 보여주는 단순한 관계자 자동 완성. 검색 조건의 관계자 선택 박스에서 사용한다.
 * 관계자를 최대 {@link ContactSuggestions#LIMIT}명 보여주고, 다 담지 못한 관계자가 있으면 목록 맨 아래에 그
 * 수를 안내한다. 안내를 누르면 모두 보여준다.
 */
@Slf4j
public class SimpleContactSuggester implements ContactSuggester {

    private final ContactController contactController;
    private final ContactChipField field;
    private final ContactSuggestionPopup popup;

    public SimpleContactSuggester(ContactController contactController, ContactChipField field) {
        this.contactController = contactController;
        this.field = field;
        this.popup = new ContactSuggestionPopup(this::addContact, this::showAll);
    }

    @Override
    public void suggest(String keyword) {
        suggest(keyword, ContactSuggestions.LIMIT);
    }

    /**
     * 안내 항목을 눌렀을 때, 지금 보고 있는 검색어로 모두 조회해 다시 보여준다.
     */
    private void showAll() {
        suggest(field.getInputText(), ContactSuggestions.UNLIMITED);
    }

    private void suggest(String keyword, int limit) {
        // 입력 중인 필드가 잠기지 않도록 팝업을 잠금 대상으로 지정한다.
        // 이미 입력된 관계자는 조회에서 빼야 목록이 제한 수까지 채워지고 남은 건수도 맞는다.
        List<Long> excludedIds = field.getContactIds();
        AsyncTask.run(popup,
                () -> contactController.autocompleteContactSummaries(keyword, excludedIds, limit),
                result -> {
                    // 응답을 기다리는 동안 입력이 바뀌었거나 포커스를 잃었으면 결과를 버린다.
                    if (!field.isInputFocusOwner() || !keyword.equals(field.getInputText())) {
                        return;
                    }
                    popup.showSuggestions(field, result.getContacts(), result.getRemainingCount());
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
