package org.church.steward.domain.datatransfer.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactService;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.exception.BackupOutdatedException;
import org.church.steward.domain.datatransfer.exception.ImportStateChangedException;
import org.church.steward.domain.datatransfer.service.ContactImportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactImportServiceImpl implements ContactImportService {

    /** 활동 기록에 적는 대상명 */
    private static final String TARGET_NAME = "관계자";

    private final ContactRepository contactRepository;
    private final ContactService contactService;
    private final ActivityLogService activityLogService;

    @Override
    @Transactional(readOnly = true)
    public ContactImportAnalysisDto analyze(ContactImportRequestDto request) {
        return analyzeWithCurrentData(request);
    }

    @Override
    public ImportResultDto importContacts(ContactImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName) {
        if (selectedKeys.isEmpty()) {
            throw new IllegalArgumentException("가져올 관계자를 선택해주세요.");
        }
        if (activityLogService.getLatestLogId() != backupLogId) {
            throw new BackupOutdatedException("백업한 뒤에 데이터가 바뀌었습니다. 백업을 다시 받아주세요.");
        }

        ContactImportAnalysisDto analysis = analyzeWithCurrentData(request);
        if (!analysis.getSignature().equals(expectedSignature)) {
            throw new ImportStateChangedException("가져오기를 준비한 뒤에 데이터가 바뀌었습니다. 내용을 다시 확인해주세요.");
        }

        List<List<ActivityLogFieldDto>> logItems = new ArrayList<>();
        for (ContactImportItemDto item : analysis.getItems()) {
            if (!selectedKeys.contains(item.getKey())) {
                continue;
            }
            if (!item.isSelectable()) {
                throw new IllegalArgumentException("가져올 수 없는 행이 선택되었습니다: " + item.getRowNumber() + "행");
            }

            Contact created = contactService.createContactWithoutLog(ContactDto.builder()
                    .type(item.getType())
                    .name(item.getName())
                    .remarks(item.getRemarks())
                    .isHidden(item.isHidden())
                    .build());
            logItems.add(activityLogService.describeCreation(created));
        }

        activityLogService.logImport(TARGET_NAME, null, fileName, String.format("추가 %,d건", logItems.size()),
                logItems);

        return ImportResultDto.builder()
                .createdCount(logItems.size())
                .updatedCount(0)
                .build();
    }

    private ContactImportAnalysisDto analyzeWithCurrentData(ContactImportRequestDto request) {
        return ContactImportAnalyzer.analyze(request, contactRepository.findAll());
    }

}
