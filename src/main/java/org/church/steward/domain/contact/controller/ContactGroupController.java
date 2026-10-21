package org.church.steward.domain.contact.controller;

import org.church.steward.domain.contact.service.ContactGroupService;
import org.church.steward.infrastructure.annotation.UiController;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ContactGroupController {

    private final ContactGroupService contactGroupService;

    public void deleteGroup(Long groupId) {
        contactGroupService.deleteGroup(groupId);
    }
}
