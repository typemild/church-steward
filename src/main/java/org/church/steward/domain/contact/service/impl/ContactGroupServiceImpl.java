package org.church.steward.domain.contact.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.church.steward.domain.contact.repository.ContactGroupMemberRepository;
import org.church.steward.domain.contact.repository.ContactGroupRepository;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.contact.service.ContactGroupService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ContactGroupServiceImpl implements ContactGroupService {

    private final ContactGroupRepository contactGroupRepository;
    private final ContactGroupMemberRepository contactGroupMemberRepository;
    private final ContactRepository contactRepository;

    @Override
    public void createGroupIfAbsent(List<Long> contactIds) {
        Set<Long> memberIds = new LinkedHashSet<>(contactIds);
        if (memberIds.isEmpty()) {
            return;
        }

        // 구성원이 일치하는 그룹이라면 첫 번째 관계자도 반드시 속해 있으므로, 그 관계자가 속한 그룹만 비교한다.
        Long firstContactId = memberIds.iterator()
                .next();
        boolean exists = contactGroupMemberRepository.findAllByContactId(firstContactId)
                .stream()
                .map(ContactGroupMember::getContactGroup)
                .anyMatch(group -> hasSameMembers(group, memberIds));
        if (exists) {
            return;
        }

        createGroup(new ArrayList<>(memberIds));
    }

    private boolean hasSameMembers(ContactGroup group, Set<Long> memberIds) {
        Set<Long> groupMemberIds = group.getMembers()
                .stream()
                .map(member -> member.getContact()
                        .getId())
                .collect(Collectors.toSet());
        return groupMemberIds.equals(memberIds);
    }

    private void createGroup(List<Long> contactIds) {
        ContactGroup group = ContactGroup.builder().build();
        List<Contact> contacts = contactRepository.findAllById(contactIds);
        for (int i = 0; i < contacts.size(); i++) {
            group.addMember(ContactGroupMember.builder()
                    .contactGroup(group)
                    .contact(contacts.get(i))
                    .orderIndex(i)
                    .build());
        }
        contactGroupRepository.save(group);
    }

    @Override
    public void deleteGroup(Long groupId) {
        contactGroupRepository.deleteById(groupId);
    }
}
