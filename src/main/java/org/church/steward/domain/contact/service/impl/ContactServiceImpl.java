package org.church.steward.domain.contact.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactAutocompleteResultDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactConstraints;
import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.mapper.ContactMapper;
import org.church.steward.domain.contact.repository.ContactGroupMemberRepository;
import org.church.steward.domain.contact.repository.ContactGroupRepository;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactService;
import org.church.steward.domain.financialrecord.repository.FinancialRecordContactRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactServiceImpl implements ContactService {

    private final ContactRepository contactRepository;
    private final FinancialRecordContactRepository financialRecordContactRepository;
    private final ContactGroupMemberRepository contactGroupMemberRepository;
    private final ContactGroupRepository contactGroupRepository;
    private final ContactMapper contactMapper;
    private final ActivityLogService activityLogService;
    private final EntitySnapshotCapturer entitySnapshotCapturer;

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getContacts(ContactSearchRequestDto searchRequest) {
        boolean includeHidden = searchRequest.getIncludeHidden() != null ? searchRequest.getIncludeHidden() : true;

        List<Contact> contacts = contactRepository.searchContacts(
                searchRequest.getType(),
                searchRequest.getName(),
                searchRequest.getRemarks(),
                includeHidden);

        return contacts.stream()
                .map(contactMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContactDto> getContactsByIds(List<Long> ids) {
        return contactRepository.findAllById(ids)
                .stream()
                .map(contactMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public ContactDto createContact(ContactDto dto) {
        validateContact(dto.getType(), dto.getName(), dto.getRemarks());
        Contact contact = contactMapper.toEntity(dto);

        contact = contactRepository.save(contact);
        activityLogService.logCreation(contact);
        return contactMapper.toDto(contact);
    }

    @Override
    public void updateContact(Long id, ContactType type, String name, String remarks, boolean isHidden) {
        validateContact(type, name, remarks);
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 관계자를 찾을 수 없습니다."));

        EntitySnapshot<Contact> oldSnapshot = entitySnapshotCapturer.capture(contact);

        // 내역에는 관계자의 이름만 스냅샷으로 남으므로 구분을 바꿔도 과거 내역은 변하지 않는다.
        contact.setType(type);
        contact.setName(name);
        contact.setRemarks(remarks);
        contact.setHidden(isHidden);

        activityLogService.logUpdate(oldSnapshot, contact);
    }

    @Override
    public void deleteContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 관계자를 찾을 수 없습니다."));

        validateDeletion(id);
        activityLogService.logDeletion(contact);

        List<ContactGroupMember> members = contactGroupMemberRepository.findAllByContactId(id);

        for (ContactGroupMember member : members) {
            Long groupId = member.getContactGroup().getId();
            contactGroupMemberRepository.delete(member);

            if (contactGroupMemberRepository.countByContactGroupId(groupId) <= 1 || hasDuplicateGroup(groupId)) {
                contactGroupRepository.deleteById(groupId);
            }
        }

        contactRepository.deleteById(id);
    }

    /**
     * 구성원이 줄어든 그룹과 구성원이 정확히 일치하는 다른 그룹이 있는지 확인한다.
     */
    private boolean hasDuplicateGroup(Long groupId) {
        Set<Long> memberIds = contactGroupMemberRepository.findContactIdsByContactGroupId(groupId);

        // 구성원이 일치하는 그룹이라면 첫 번째 관계자도 반드시 속해 있으므로, 그 관계자가 속한 그룹만 비교한다.
        Long firstContactId = memberIds.iterator()
                .next();
        return contactGroupMemberRepository.findAllByContactId(firstContactId)
                .stream()
                .map(member -> member.getContactGroup()
                        .getId())
                .filter(otherGroupId -> !otherGroupId.equals(groupId))
                .anyMatch(otherGroupId -> contactGroupMemberRepository.findContactIdsByContactGroupId(otherGroupId)
                        .equals(memberIds));
    }

    @Override
    public void hideContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 관계자를 찾을 수 없습니다."));

        EntitySnapshot<Contact> oldSnapshot = entitySnapshotCapturer.capture(contact);
        contact.setHidden(true);
        activityLogService.logUpdate(oldSnapshot, contact);
    }

    @Override
    public void unhideContact(Long id) {
        Contact contact = contactRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 관계자를 찾을 수 없습니다."));

        EntitySnapshot<Contact> oldSnapshot = entitySnapshotCapturer.capture(contact);
        contact.setHidden(false);
        activityLogService.logUpdate(oldSnapshot, contact);
    }

    @Override
    @Transactional(readOnly = true)
    public ContactAutocompleteResultDto<ContactAutocompleteDto> autocompleteContacts(String keyword,
            List<Long> excludedIds, int limit) {
        List<Contact> contacts = findAutocompleteCandidates(keyword, excludedIds, limit);

        List<ContactAutocompleteDto> dtos = contacts.stream().map(contact -> {
            List<GroupInfoDto> groups = contactGroupMemberRepository.findAllByContactId(contact.getId()).stream()
                    .map(cm -> cm.getContactGroup())
                    .distinct()
                    .map(group -> {
                        List<ContactSummaryDto> members = contactGroupMemberRepository
                                .findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc(group.getId())
                                .stream()
                                .map(m -> contactMapper.toSummaryDto(m.getContact()))
                                .collect(Collectors.toList());
                        return contactMapper.toGroupInfoDto(group, members);
                    })
                    .collect(Collectors.toList());

            return contactMapper.toAutocompleteDto(contact, groups);
        }).collect(Collectors.toList());

        return ContactAutocompleteResultDto.<ContactAutocompleteDto>builder()
                .contacts(dtos)
                .totalCount(countAutocompleteCandidates(keyword, excludedIds, limit, contacts.size()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ContactAutocompleteResultDto<ContactSummaryDto> autocompleteContactSummaries(String keyword,
            List<Long> excludedIds, int limit) {
        List<Contact> contacts = findAutocompleteCandidates(keyword, excludedIds, limit);

        List<ContactSummaryDto> dtos = contacts.stream()
                .map(contactMapper::toSummaryDto)
                .collect(Collectors.toList());

        return ContactAutocompleteResultDto.<ContactSummaryDto>builder()
                .contacts(dtos)
                .totalCount(countAutocompleteCandidates(keyword, excludedIds, limit, contacts.size()))
                .build();
    }

    private List<Contact> findAutocompleteCandidates(String keyword, List<Long> excludedIds, int limit) {
        return contactRepository.findAutocompleteCandidates(keyword, excludedIds, PageRequest.of(0, limit));
    }

    /**
     * 자동 완성 후보의 전체 건수를 구한다. 조회한 건수가 제한보다 적으면 그것이 전부이므로 다시 세지 않는다.
     */
    private long countAutocompleteCandidates(String keyword, List<Long> excludedIds, int limit, int foundCount) {
        if (foundCount < limit) {
            return foundCount;
        }
        return contactRepository.countAutocompleteCandidates(keyword, excludedIds);
    }

    /**
     * 구분과 이름은 필수이고, 이름과 비고는 칼럼 길이 이내여야 한다.
     */
    private void validateContact(ContactType type, String name, String remarks) {
        if (type == null) {
            throw new IllegalArgumentException("구분을 선택해주세요.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름을 입력해주세요.");
        }
        if (name.length() > ContactConstraints.NAME_MAX_LENGTH) {
            throw new IllegalArgumentException("이름은 " + ContactConstraints.NAME_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
        if (remarks != null && remarks.length() > ContactConstraints.REMARKS_MAX_LENGTH) {
            throw new IllegalArgumentException("비고는 " + ContactConstraints.REMARKS_MAX_LENGTH + "자 이내로 입력해주세요.");
        }
    }

    private void validateDeletion(Long id) {
        if (financialRecordContactRepository.existsByContactId(id)) {
            throw new IllegalStateException("수입/지출 내역이 등록되어 있어서 삭제할 수 없습니다.");
        }
    }

}
