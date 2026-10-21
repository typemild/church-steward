package org.church.steward.domain.contact.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.church.steward.domain.contact.repository.ContactGroupMemberRepository;
import org.church.steward.domain.contact.repository.ContactGroupRepository;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactGroupServiceImplTest {

    @Mock
    private ContactGroupRepository contactGroupRepository;

    @Mock
    private ContactGroupMemberRepository contactGroupMemberRepository;

    @Mock
    private ContactRepository contactRepository;

    @InjectMocks
    private ContactGroupServiceImpl contactGroupService;

    @Nested
    @DisplayName("createGroupIfAbsent 테스트")
    class CreateGroupIfAbsentTest {

        @Test
        @DisplayName("구성원이 정확히 일치하는 그룹이 이미 있으면 그룹을 만들지 않는다")
        void createGroupIfAbsent_SameMembersGroupExists_DoesNotCreateGroup() {
            // Given
            ContactGroup existingGroup = createGroupWithMembers(100L, 1L, 2L);
            when(contactGroupMemberRepository.findAllByContactId(1L))
                    .thenReturn(List.of(existingGroup.getMembers().get(0)));

            // When
            contactGroupService.createGroupIfAbsent(List.of(1L, 2L));

            // Then
            verify(contactGroupRepository, never()).save(any(ContactGroup.class));
        }

        @Test
        @DisplayName("구성원의 순서만 다른 그룹이 이미 있으면 그룹을 만들지 않는다")
        void createGroupIfAbsent_SameMembersInDifferentOrder_DoesNotCreateGroup() {
            // Given
            ContactGroup existingGroup = createGroupWithMembers(100L, 1L, 2L);
            when(contactGroupMemberRepository.findAllByContactId(2L))
                    .thenReturn(List.of(existingGroup.getMembers().get(1)));

            // When
            contactGroupService.createGroupIfAbsent(List.of(2L, 1L));

            // Then
            verify(contactGroupRepository, never()).save(any(ContactGroup.class));
        }

        @Test
        @DisplayName("첫 번째 관계자가 어떤 그룹에도 속해 있지 않으면 그룹을 만든다")
        void createGroupIfAbsent_NoGroupExists_CreatesGroup() {
            // Given
            when(contactGroupMemberRepository.findAllByContactId(1L)).thenReturn(List.of());

            // When
            contactGroupService.createGroupIfAbsent(List.of(1L, 2L));

            // Then
            verify(contactGroupRepository).save(any(ContactGroup.class));
        }

        @Test
        @DisplayName("그룹을 만들 때 구성원을 조회된 순서대로 0번부터 그룹의 구성원 목록에 넣어 함께 저장한다")
        void createGroupIfAbsent_NoGroupExists_SavesGroupWithOrderedMembers() {
            // Given
            List<Long> contactIds = List.of(1L, 2L);
            Contact contact1 = Contact.builder().id(1L).name("홍길동").build();
            Contact contact2 = Contact.builder().id(2L).name("이순신").build();

            when(contactGroupMemberRepository.findAllByContactId(1L)).thenReturn(List.of());
            when(contactRepository.findAllById(contactIds)).thenReturn(List.of(contact1, contact2));

            ArgumentCaptor<ContactGroup> groupCaptor = ArgumentCaptor.forClass(ContactGroup.class);

            // When
            contactGroupService.createGroupIfAbsent(contactIds);

            // Then
            verify(contactGroupRepository).save(groupCaptor.capture());
            ContactGroup savedGroup = groupCaptor.getValue();
            assertThat(savedGroup.getMembers())
                    .extracting(member -> member.getContact().getId(), ContactGroupMember::getOrderIndex,
                            ContactGroupMember::getContactGroup)
                    .containsExactly(tuple(1L, 0, savedGroup), tuple(2L, 1, savedGroup));
        }

        @Test
        @DisplayName("구성원이 더 많은 그룹만 있으면 그룹을 만든다")
        void createGroupIfAbsent_OnlyLargerGroupExists_CreatesGroup() {
            // Given
            ContactGroup largerGroup = createGroupWithMembers(100L, 1L, 2L, 3L);
            when(contactGroupMemberRepository.findAllByContactId(1L))
                    .thenReturn(List.of(largerGroup.getMembers().get(0)));

            // When
            contactGroupService.createGroupIfAbsent(List.of(1L, 2L));

            // Then
            verify(contactGroupRepository).save(any(ContactGroup.class));
        }

        @Test
        @DisplayName("구성원이 더 적은 그룹만 있으면 그룹을 만든다")
        void createGroupIfAbsent_OnlySmallerGroupExists_CreatesGroup() {
            // Given
            ContactGroup smallerGroup = createGroupWithMembers(100L, 1L, 2L);
            when(contactGroupMemberRepository.findAllByContactId(1L))
                    .thenReturn(List.of(smallerGroup.getMembers().get(0)));

            // When
            contactGroupService.createGroupIfAbsent(List.of(1L, 2L, 3L));

            // Then
            verify(contactGroupRepository).save(any(ContactGroup.class));
        }

        @Test
        @DisplayName("그룹을 만들 때 중복된 관계자 ID는 한 번만 사용한다")
        void createGroupIfAbsent_DuplicateContactIds_CreatesGroupWithDistinctIds() {
            // Given
            when(contactGroupMemberRepository.findAllByContactId(1L)).thenReturn(List.of());

            // When
            contactGroupService.createGroupIfAbsent(List.of(1L, 2L, 1L));

            // Then
            verify(contactRepository).findAllById(List.of(1L, 2L));
        }

        @Test
        @DisplayName("관계자 ID 목록이 비어 있으면 그룹을 만들지 않는다")
        void createGroupIfAbsent_EmptyContactIds_DoesNotCreateGroup() {
            // Given
            List<Long> contactIds = List.of();

            // When
            contactGroupService.createGroupIfAbsent(contactIds);

            // Then
            verify(contactGroupRepository, never()).save(any(ContactGroup.class));
        }

        private ContactGroup createGroupWithMembers(Long groupId, Long... contactIds) {
            ContactGroup group = ContactGroup.builder().id(groupId).build();
            for (int i = 0; i < contactIds.length; i++) {
                Contact contact = Contact.builder().id(contactIds[i]).name("관계자" + contactIds[i]).build();
                group.getMembers().add(ContactGroupMember.builder()
                        .contactGroup(group)
                        .contact(contact)
                        .orderIndex(i)
                        .build());
            }
            return group;
        }
    }

    @Nested
    @DisplayName("deleteGroup 테스트")
    class DeleteGroupTest {

        @Test
        @DisplayName("그룹 ID를 사용하여 성공적으로 삭제한다")
        void deleteGroup_Success() {
            // Given
            Long groupId = 1L;

            // When
            contactGroupService.deleteGroup(groupId);

            // Then
            verify(contactGroupRepository).deleteById(groupId);
        }
    }
}
