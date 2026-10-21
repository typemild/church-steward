package org.church.steward.domain.contact.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.church.steward.domain.activitylog.service.ActivityLogService;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.church.steward.domain.activitylog.util.EntitySnapshotCapturer;
import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.mapper.ContactMapper;
import org.church.steward.domain.contact.repository.ContactGroupMemberRepository;
import org.church.steward.domain.contact.repository.ContactGroupRepository;
import org.church.steward.domain.contact.repository.ContactRepository;
import org.church.steward.domain.financialrecord.repository.FinancialRecordContactRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ContactServiceImplTest {

    @Mock
    private ContactRepository contactRepository;

    @Mock
    private FinancialRecordContactRepository financialRecordContactRepository;

    @Mock
    private ContactGroupMemberRepository contactGroupMemberRepository;

    @Mock
    private ContactGroupRepository contactGroupRepository;

    @Mock
    private ContactMapper contactMapper;

    @Mock
    private ActivityLogService activityLogService;

    @Mock
    private EntitySnapshotCapturer entitySnapshotCapturer;

    @InjectMocks
    private ContactServiceImpl contactService;

    @Nested
    @DisplayName("getContacts 테스트")
    class GetContactsTest {

        @Test
        @DisplayName("검색 요청 시 - 연락처 목록을 반환한다")
        void getContacts_searchRequest_returnsList() {
            // Given
            ContactSearchRequestDto searchRequest = ContactSearchRequestDto.builder()
                    .includeHidden(true)
                    .build();
            Contact contact1 = Contact.builder().id(1L).build();
            Contact contact2 = Contact.builder().id(2L).build();
            ContactDto dto1 = ContactDto.builder().id(1L).build();
            ContactDto dto2 = ContactDto.builder().id(2L).build();

            when(contactRepository.searchContacts(null, null, null, true))
                    .thenReturn(List.of(contact1, contact2));
            when(contactMapper.toDto(contact1)).thenReturn(dto1);
            when(contactMapper.toDto(contact2)).thenReturn(dto2);

            // When
            List<ContactDto> result = contactService.getContacts(searchRequest);

            // Then
            assertThat(result).hasSize(2).containsExactlyInAnyOrder(dto1, dto2);
        }

        @Test
        @DisplayName("검색 요청 시 - 서비스의 getContacts를 호출한다")
        void getContacts_searchRequest_callsService() {
            // Given
            ContactSearchRequestDto searchRequest = ContactSearchRequestDto.builder()
                    .includeHidden(false)
                    .build();

            // When
            contactService.getContacts(searchRequest);

            // Then
            verify(contactRepository).searchContacts(null, null, null, false);
        }
    }

    @Nested
    @DisplayName("getContactsByIds 테스트")
    class GetContactsByIdsTest {

        @Test
        @DisplayName("ID 목록으로 조회 시 - 존재하는 관계자의 DTO 목록을 반환한다")
        void getContactsByIds_existingIds_returnsDtos() {
            // Given
            List<Long> ids = List.of(1L, 2L);
            Contact contact1 = Contact.builder().id(1L).build();
            Contact contact2 = Contact.builder().id(2L).build();
            ContactDto dto1 = ContactDto.builder().id(1L).build();
            ContactDto dto2 = ContactDto.builder().id(2L).build();

            when(contactRepository.findAllById(ids)).thenReturn(List.of(contact1, contact2));
            when(contactMapper.toDto(contact1)).thenReturn(dto1);
            when(contactMapper.toDto(contact2)).thenReturn(dto2);

            // When
            List<ContactDto> result = contactService.getContactsByIds(ids);

            // Then
            assertThat(result).containsExactly(dto1, dto2);
        }

        @Test
        @DisplayName("존재하지 않는 ID로 조회 시 - 빈 목록을 반환한다")
        void getContactsByIds_unknownIds_returnsEmptyList() {
            // Given
            List<Long> ids = List.of(999L);
            when(contactRepository.findAllById(ids)).thenReturn(List.of());

            // When
            List<ContactDto> result = contactService.getContactsByIds(ids);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("createContact 테스트")
    class CreateContactTest {

        @Test
        @DisplayName("관계자 생성_성공 시_저장된 DTO를 반환한다")
        void createContact_success_returnsSavedDto() {
            // Given
            ContactDto dto = ContactDto.builder().name("New").build();
            Contact contactToSave = Contact.builder().build();
            Contact savedContact = Contact.builder().id(1L).name("New").build();
            ContactDto savedDto = ContactDto.builder().id(1L).name("New").build();

            when(contactMapper.toEntity(dto)).thenReturn(contactToSave);
            when(contactRepository.save(contactToSave)).thenReturn(savedContact);
            when(contactMapper.toDto(savedContact)).thenReturn(savedDto);

            // When
            ContactDto result = contactService.createContact(dto);

            // Then
            assertThat(result).isSameAs(savedDto);
        }

        @Test
        @DisplayName("관계자 생성_성공 시_Repository를 호출한다")
        void createContact_success_callsRepository() {
            // Given
            ContactDto dto = ContactDto.builder().name("New").build();
            Contact contactToSave = Contact.builder().build();

            when(contactMapper.toEntity(dto)).thenReturn(contactToSave);
            when(contactRepository.save(contactToSave)).thenReturn(contactToSave);

            // When
            contactService.createContact(dto);

            // Then
            verify(contactRepository).save(contactToSave);
        }

        @Test
        @DisplayName("관계자 생성_성공 시_ActivityLogService를 호출한다")
        void createContact_success_callsActivityLog() {
            // Given
            ContactDto dto = ContactDto.builder().name("New").build();
            Contact contactToSave = Contact.builder().build();

            when(contactMapper.toEntity(dto)).thenReturn(contactToSave);
            when(contactRepository.save(contactToSave)).thenReturn(contactToSave);

            // When
            contactService.createContact(dto);

            // Then
            verify(activityLogService).logCreation(contactToSave);
        }
    }

    @Nested
    @DisplayName("updateContact 테스트")
    class UpdateContactTest {

        @Test
        @DisplayName("관계자 수정_성공 시_필드가 업데이트된다")
        void updateContact_success_updatesFields() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder()
                    .id(id)
                    .type(ContactType.INDIVIDUAL)
                    .name("Old")
                    .remarks("Old Remarks")
                    .build();
            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(null);

            // When
            contactService.updateContact(id, ContactType.OTHER, "New", "New Remarks", false);

            // Then
            assertThat(contact.getType()).isEqualTo(ContactType.OTHER);
            assertThat(contact.getName()).isEqualTo("New");
            assertThat(contact.getRemarks()).isEqualTo("New Remarks");
            assertThat(contact.isHidden()).isFalse();
        }

        @Test
        @DisplayName("관계자 수정_성공 시_ActivityLogService를 호출한다")
        void updateContact_success_callsActivityLog() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder().id(id).build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<Contact> snapshot = mock(EntitySnapshot.class);

            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(snapshot);

            // When
            contactService.updateContact(id, ContactType.INDIVIDUAL, "New", "New Remarks", false);

            // Then
            verify(activityLogService).logUpdate(snapshot, contact);
        }

        @Test
        @DisplayName("관계자 수정_실패 시_존재하지 않는 ID이면 예외가 발생한다")
        void updateContact_notFound_throwsException() {
            // Given
            Long id = 1L;
            when(contactRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(
                    () -> contactService.updateContact(id, ContactType.INDIVIDUAL, "New", "New Remarks", false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 관계자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("deleteContact 테스트")
    class DeleteContactTest {

        @Test
        @DisplayName("관계자 삭제_성공 시_Repository를 호출한다")
        void deleteContact_success_callsRepository() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder().id(id).build();

            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of());

            // When
            contactService.deleteContact(id);

            // Then
            verify(contactRepository).deleteById(id);
        }

        @Test
        @DisplayName("관계자 삭제_성공 시_ActivityLogService를 호출한다")
        void deleteContact_success_callsActivityLog() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder().id(id).build();

            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of());

            // When
            contactService.deleteContact(id);

            // Then
            verify(activityLogService).logDeletion(contact);
        }

        @Test
        @DisplayName("관계자 삭제_성공 시_그룹 멤버가 1명 남았을 때 그룹을 삭제한다")
        void deleteContact_success_deletesGroupWhenOneMemberLeft() {
            // Given
            Long id = 1L;
            Long groupId = 10L;
            ContactGroup group = ContactGroup.builder().id(groupId).build();
            ContactGroupMember member = ContactGroupMember.builder()
                    .contactGroup(group)
                    .build();

            when(contactRepository.findById(id)).thenReturn(Optional.of(Contact.builder().id(id).build()));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of(member));
            when(contactGroupMemberRepository.countByContactGroupId(groupId)).thenReturn(1L);

            // When
            contactService.deleteContact(id);

            // Then
            verify(contactGroupRepository).deleteById(groupId);
        }

        @Test
        @DisplayName("관계자 삭제_성공 시_그룹 멤버가 0명일 때 그룹을 삭제한다")
        void deleteContact_success_deletesGroupWhenZeroMembersLeft() {
            // Given
            Long id = 1L;
            Long groupId = 10L;
            ContactGroup group = ContactGroup.builder().id(groupId).build();
            ContactGroupMember member = ContactGroupMember.builder()
                    .contactGroup(group)
                    .build();

            when(contactRepository.findById(id)).thenReturn(Optional.of(Contact.builder().id(id).build()));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of(member));
            when(contactGroupMemberRepository.countByContactGroupId(groupId)).thenReturn(0L);

            // When
            contactService.deleteContact(id);

            // Then
            verify(contactGroupRepository).deleteById(groupId);
        }

        @Test
        @DisplayName("관계자 삭제_성공 시_남은 구성원이 다른 그룹과 정확히 일치하면 관계자가 삭제된 쪽의 그룹을 삭제한다")
        void deleteContact_remainingMembersMatchOtherGroup_deletesShrunkGroup() {
            // Given
            Long id = 1L;
            Long groupId = 10L;
            Long otherGroupId = 20L;
            ContactGroupMember member = createGroupMember(groupId);

            when(contactRepository.findById(id)).thenReturn(Optional.of(Contact.builder().id(id).build()));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of(member));
            when(contactGroupMemberRepository.countByContactGroupId(groupId)).thenReturn(2L);
            when(contactGroupMemberRepository.findContactIdsByContactGroupId(groupId))
                    .thenReturn(new LinkedHashSet<>(List.of(2L, 3L)));
            when(contactGroupMemberRepository.findAllByContactId(2L))
                    .thenReturn(List.of(createGroupMember(groupId), createGroupMember(otherGroupId)));
            when(contactGroupMemberRepository.findContactIdsByContactGroupId(otherGroupId))
                    .thenReturn(Set.of(2L, 3L));

            // When
            contactService.deleteContact(id);

            // Then
            verify(contactGroupRepository).deleteById(groupId);
            verify(contactGroupRepository, never()).deleteById(otherGroupId);
        }

        @Test
        @DisplayName("관계자 삭제_성공 시_남은 구성원이 다른 그룹과 일치하지 않으면 그룹을 삭제하지 않는다")
        void deleteContact_remainingMembersDifferFromOtherGroups_keepsGroup() {
            // Given
            Long id = 1L;
            Long groupId = 10L;
            Long otherGroupId = 20L;
            ContactGroupMember member = createGroupMember(groupId);

            when(contactRepository.findById(id)).thenReturn(Optional.of(Contact.builder().id(id).build()));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(false);
            when(contactGroupMemberRepository.findAllByContactId(id)).thenReturn(List.of(member));
            when(contactGroupMemberRepository.countByContactGroupId(groupId)).thenReturn(2L);
            when(contactGroupMemberRepository.findContactIdsByContactGroupId(groupId))
                    .thenReturn(new LinkedHashSet<>(List.of(2L, 3L)));
            when(contactGroupMemberRepository.findAllByContactId(2L))
                    .thenReturn(List.of(createGroupMember(groupId), createGroupMember(otherGroupId)));
            when(contactGroupMemberRepository.findContactIdsByContactGroupId(otherGroupId))
                    .thenReturn(Set.of(2L, 3L, 4L));

            // When
            contactService.deleteContact(id);

            // Then
            verify(contactGroupRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("관계자 삭제_실패 시_수입/지출 내역이 있으면 예외가 발생한다")
        void deleteContact_failsDueToFinancialRecord_throwsException() {
            // Given
            Long id = 1L;
            when(contactRepository.findById(id)).thenReturn(Optional.of(Contact.builder().id(id).build()));
            when(financialRecordContactRepository.existsByContactId(id)).thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> contactService.deleteContact(id))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("수입/지출 내역이 등록되어 있어서 삭제할 수 없습니다.");
        }

        private ContactGroupMember createGroupMember(Long groupId) {
            return ContactGroupMember.builder()
                    .contactGroup(ContactGroup.builder().id(groupId).build())
                    .build();
        }
    }

    @Nested
    @DisplayName("hideContact 테스트")
    class HideContactTest {

        @Test
        @DisplayName("관계자 숨김_성공 시_isHidden이 true가 된다")
        void hideContact_success_setsHiddenTrue() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder()
                    .id(id)
                    .isHidden(false)
                    .build();
            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(null);

            // When
            contactService.hideContact(id);

            // Then
            assertThat(contact.isHidden()).isTrue();
        }

        @Test
        @DisplayName("관계자 숨김_성공 시_ActivityLogService를 호출한다")
        void hideContact_success_callsActivityLog() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder().id(id).build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<Contact> snapshot = mock(EntitySnapshot.class);

            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(snapshot);

            // When
            contactService.hideContact(id);

            // Then
            verify(activityLogService).logUpdate(snapshot, contact);
        }

        @Test
        @DisplayName("관계자 숨김_실패 시_존재하지 않는 ID이면 예외가 발생한다")
        void hideContact_notFound_throwsException() {
            // Given
            Long id = 1L;
            when(contactRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contactService.hideContact(id))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 관계자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("unhideContact 테스트")
    class UnhideContactTest {

        @Test
        @DisplayName("관계자 숨김 해제_성공 시_isHidden이 false가 된다")
        void unhideContact_success_setsHiddenFalse() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder()
                    .id(id)
                    .isHidden(true)
                    .build();
            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(null);

            // When
            contactService.unhideContact(id);

            // Then
            assertThat(contact.isHidden()).isFalse();
        }

        @Test
        @DisplayName("관계자 숨김 해제_성공 시_ActivityLogService를 호출한다")
        void unhideContact_success_callsActivityLog() {
            // Given
            Long id = 1L;
            Contact contact = Contact.builder().id(id).build();
            @SuppressWarnings("unchecked")
            EntitySnapshot<Contact> snapshot = mock(EntitySnapshot.class);

            when(contactRepository.findById(id)).thenReturn(Optional.of(contact));
            when(entitySnapshotCapturer.capture(contact)).thenReturn(snapshot);

            // When
            contactService.unhideContact(id);

            // Then
            verify(activityLogService).logUpdate(snapshot, contact);
        }

        @Test
        @DisplayName("관계자 숨김 해제_실패 시_존재하지 않는 ID이면 예외가 발생한다")
        void unhideContact_notFound_throwsException() {
            // Given
            Long id = 1L;
            when(contactRepository.findById(id)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> contactService.unhideContact(id))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 관계자를 찾을 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("autocompleteContacts 테스트")
    class AutocompleteContactsTest {

        @Test
        @DisplayName("관계자 자동완성_성공 시_그룹 정보를 포함한 DTO를 반환한다")
        void autocompleteContacts_success_returnsDtoWithGroups() {
            // Given
            String keyword = "홍길동";
            int limit = 10;

            Contact contact = Contact.builder().id(1L).name("홍길동").remarks("비고").build();
            ContactGroup group = ContactGroup.builder().id(10L).build();
            ContactGroupMember member = ContactGroupMember.builder()
                    .contact(contact)
                    .contactGroup(group)
                    .build();

            when(contactRepository.findByNameContainingAndIsHiddenFalseOrderByNameAsc(any(), any()))
                    .thenReturn(List.of(contact));
            when(contactGroupMemberRepository.findAllByContactId(1L))
                    .thenReturn(List.of(member));
            when(contactGroupMemberRepository.findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc(10L))
                    .thenReturn(List.of(member));

            ContactAutocompleteDto expectedDto = ContactAutocompleteDto.builder()
                    .id(1L)
                    .name("홍길동")
                    .groups(List.of(GroupInfoDto.builder()
                            .members(List.of(ContactSummaryDto.builder()
                                    .name("홍길동")
                                    .build()))
                            .build()))
                    .build();
            when(contactMapper.toAutocompleteDto(any(), any())).thenReturn(expectedDto);

            // When
            List<ContactAutocompleteDto> result = contactService.autocompleteContacts(keyword, limit);

            // Then
            assertThat(result).hasSize(1);
            ContactAutocompleteDto dto = result.get(0);
            assertThat(dto.getId()).isEqualTo(1L);
            assertThat(dto.getName()).isEqualTo("홍길동");
            assertThat(dto.getGroups()).hasSize(1);
            assertThat(dto.getGroups().get(0).getMembers().get(0).getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("관계자 자동완성_결과 없음 시_빈 리스트를 반환한다")
        void autocompleteContacts_noResult_returnsEmptyList() {
            // Given
            String keyword = "없는이름";
            int limit = 10;

            when(contactRepository.findByNameContainingAndIsHiddenFalseOrderByNameAsc(any(), any()))
                    .thenReturn(List.of());

            // When
            List<ContactAutocompleteDto> result = contactService.autocompleteContacts(keyword, limit);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("autocompleteContactSummaries 테스트")
    class AutocompleteContactSummariesTest {

        @Test
        @DisplayName("관계자 요약 자동완성_성공 시_요약 DTO를 반환한다")
        void autocompleteContactSummaries_success_returnsSummaryDtos() {
            // Given
            String keyword = "홍길동";
            int limit = 10;

            Contact contact = Contact.builder().id(1L).name("홍길동").build();
            ContactSummaryDto summaryDto = ContactSummaryDto.builder().id(1L).name("홍길동").build();

            when(contactRepository.findByNameContainingAndIsHiddenFalseOrderByNameAsc(keyword,
                    PageRequest.of(0, limit)))
                    .thenReturn(List.of(contact));
            when(contactMapper.toSummaryDto(contact)).thenReturn(summaryDto);

            // When
            List<ContactSummaryDto> result = contactService.autocompleteContactSummaries(keyword, limit);

            // Then
            assertThat(result).containsExactly(summaryDto);
        }
    }
}
