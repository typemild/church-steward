package org.church.steward.domain.datatransfer.controller;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.service.ContactImportService;
import org.church.steward.infrastructure.annotation.UiController;
import org.church.steward.infrastructure.security.AdminAccessChecker;

import lombok.RequiredArgsConstructor;

@UiController
@RequiredArgsConstructor
public class ContactImportController {

    private final ContactImportService contactImportService;
    private final AdminAccessChecker adminAccessChecker;

    public ContactImportAnalysisDto analyze(ContactImportRequestDto request) {
        adminAccessChecker.checkAdmin();
        return contactImportService.analyze(request);
    }

    public ImportResultDto importContacts(ContactImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        adminAccessChecker.checkAdmin();
        return contactImportService.importContacts(request, selectedKeys, expectedSignature, backupLogId, fileName);
    }
}
