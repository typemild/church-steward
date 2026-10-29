package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.view.settings.contact.ContactDetailDialog;

import lombok.extern.slf4j.Slf4j;

/**
 * 계층형 관계자 자동 완성. 수입/지출 입력 다이얼로그의 관계자 선택 박스에서 사용한다. 관계자 목록(최대
 * {@link ContactSuggestions#LIMIT}명)과 "신규 추가" 항목을 보여주고, 목록에서 바로 관계자를 수정/삭제하거나
 * 그룹을 삭제할 수 있다. 목록에 다 담지 못한 관계자가 있으면 "신규 추가" 아래에 그 수를 안내하고, 안내를 누르면 모두 보여준다. 그룹
 * 기능을 켜면 관계자가 속한 그룹을 골라 구성원 전체를 한 번에 입력할 수 있다.
 */
@Slf4j
public class HierarchicalContactSuggester implements ContactSuggester, ContactAutocompleteListener {

    private final ContactController contactController;
    private final ContactGroupController contactGroupController;
    private final ContactChipField field;
    private final ContactAutocompletePopup popup;

    /** 가장 최근에 자동 완성을 요청한 키워드. "신규 추가" 시 이름의 초기값으로 쓴다. */
    private String lastKeyword = "";

    public HierarchicalContactSuggester(ContactController contactController,
            ContactGroupController contactGroupController, ContactChipField field) {
        this.contactController = contactController;
        this.contactGroupController = contactGroupController;
        this.field = field;
        this.popup = new ContactAutocompletePopup(this);
    }

    /**
     * 그룹 기능 사용 여부를 설정한다. 수입 입력에서만 사용한다.
     */
    public void setGroupEnabled(boolean groupEnabled) {
        popup.setVisible(false);
        popup.setGroupEnabled(groupEnabled);
    }

    @Override
    public void suggest(String keyword) {
        suggest(keyword, ContactSuggestions.LIMIT);
    }

    @Override
    public void onShowAllRequested() {
        suggest(lastKeyword, ContactSuggestions.UNLIMITED);
    }

    private void suggest(String keyword, int limit) {
        lastKeyword = keyword;

        // 입력 중인 필드가 잠기지 않도록 팝업을 잠금 대상으로 지정한다.
        // 이미 입력된 관계자는 조회에서 빼야 목록이 제한 수까지 채워지고 남은 건수도 맞는다.
        List<Long> excludedIds = field.getContactIds();
        AsyncTask.run(popup,
                () -> contactController.autocompleteContacts(keyword, excludedIds, limit),
                result -> {
                    // 응답을 기다리는 동안 입력이 바뀌었거나 포커스를 잃었으면 결과를 버린다.
                    if (!field.isInputFocusOwner() || !keyword.equals(field.getInputText())) {
                        return;
                    }
                    popup.showContacts(field, result.getContacts(), result.getRemainingCount());
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

        return switch (keyCode) {
        case KeyEvent.VK_DOWN -> {
            popup.moveSelection(1);
            yield true;
        }
        case KeyEvent.VK_UP -> {
            popup.moveSelection(-1);
            yield true;
        }
        case KeyEvent.VK_RIGHT -> popup.moveToSubList();
        case KeyEvent.VK_LEFT -> popup.moveToMainList();
        case KeyEvent.VK_ENTER -> {
            popup.activateSelection();
            yield true;
        }
        case KeyEvent.VK_ESCAPE -> {
            popup.setVisible(false);
            yield true;
        }
        default -> false;
        };
    }

    @Override
    public void onContactSelected(ContactAutocompleteDto contact) {
        field.addContact(contact.getId(), contact.getType(), contact.getName(), contact.getRemarks());
        field.requestInputFocus();
    }

    @Override
    public void onGroupSelected(GroupInfoDto group) {
        for (ContactSummaryDto member : group.getMembers()) {
            field.addContact(member.getId(), member.getType(), member.getName(), member.getRemarks());
        }
        field.requestInputFocus();
    }

    @Override
    public void onNewContactRequested() {
        final String name = lastKeyword;
        field.commitComposition();
        popup.setVisible(false);

        ContactDto initial = ContactDto.builder()
                .type(ContactType.INDIVIDUAL)
                .name(name)
                .build();

        // 팝업의 마우스 이벤트 처리가 끝난 뒤에 모달 다이얼로그를 연다.
        SwingUtilities.invokeLater(() -> new ContactDetailDialog(getOwnerWindow(), ContactDetailDialog.Mode.ADD,
                initial, this::createContact).setVisible(true));
    }

    private void createContact(ContactDto dto) {
        AsyncTask.run(popup,
                () -> contactController.createContact(dto),
                created -> {
                    // 저장 즉시 입력란에 반영한다.
                    field.addContact(created.getId(), created.getType(), created.getName(), created.getRemarks());
                    field.requestInputFocus();
                },
                err -> {
                    log.error("Failed to create contact", err);
                    JOptionPane.showMessageDialog(field, "등록 실패: " + err.getMessage(), "오류",
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    @Override
    public void onContactEditRequested(ContactAutocompleteDto contact) {
        field.commitComposition();
        popup.setVisible(false);

        ContactDto current = ContactDto.builder()
                .id(contact.getId())
                .type(contact.getType())
                .name(contact.getName())
                .remarks(contact.getRemarks())
                .isHidden(false) // 숨긴 관계자는 자동 완성 목록에 나오지 않는다.
                .build();

        SwingUtilities.invokeLater(() -> new ContactDetailDialog(getOwnerWindow(), ContactDetailDialog.Mode.EDIT,
                current, this::updateContact).setVisible(true));
    }

    private void updateContact(ContactDto dto) {
        AsyncTask.run(popup,
                () -> {
                    contactController.updateContact(dto.getId(), dto.getType(), dto.getName(), dto.getRemarks(),
                            dto.isHidden());
                    return null;
                },
                _ -> field.requestInputFocus(),
                err -> {
                    log.error("Failed to update contact", err);
                    JOptionPane.showMessageDialog(field, "수정 실패: " + err.getMessage(), "오류",
                            JOptionPane.ERROR_MESSAGE);
                });
    }

    @Override
    public void onContactDeleteRequested(ContactAutocompleteDto contact) {
        popup.setVisible(false);

        SwingUtilities.invokeLater(() -> {
            AsyncTask.run(popup,
                    () -> {
                        contactController.deleteContact(contact.getId());
                        return null;
                    },
                    _ -> field.requestInputFocus(),
                    err -> {
                        log.error("Failed to delete contact", err);
                        JOptionPane.showMessageDialog(field, "삭제 실패: " + err.getMessage(), "오류",
                                JOptionPane.ERROR_MESSAGE);
                    });
        });
    }

    @Override
    public void onGroupDeleteRequested(GroupInfoDto group) {
        popup.setVisible(false);

        SwingUtilities.invokeLater(() -> {
            AsyncTask.run(popup,
                    () -> {
                        contactGroupController.deleteGroup(group.getGroupId());
                        return null;
                    },
                    _ -> field.requestInputFocus(),
                    err -> {
                        log.error("Failed to delete contact group", err);
                        JOptionPane.showMessageDialog(field, "삭제 실패: " + err.getMessage(), "오류",
                                JOptionPane.ERROR_MESSAGE);
                    });
        });
    }

    private Window getOwnerWindow() {
        return SwingUtilities.getWindowAncestor(field);
    }
}
