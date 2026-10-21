package org.church.steward.domain.contact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.church.steward.domain.contact.entity.ContactGroupMember;
import org.church.steward.domain.contact.entity.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

@DataJpaTest
class ContactGroupMemberRepositoryTest {

    @Autowired
    private ContactGroupMemberRepository contactGroupMemberRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Nested
    @DisplayName("findAllByContactId 테스트")
    class FindAllByContactIdTest {

        @Test
        @DisplayName("연락처 ID가 일치하는 모든 멤버 목록을 반환한다")
        void findAllByContactId_whenContactIdMatches_thenReturnsAllMembers() {
            // Given
            Contact contact = entityManager.persist(Contact.builder()
                    .name("홍길동")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            ContactGroup group1 = entityManager.persist(ContactGroup.builder().build());
            ContactGroup group2 = entityManager.persist(ContactGroup.builder().build());
            entityManager.persist(ContactGroupMember.builder()
                    .contactGroup(group1)
                    .contact(contact)
                    .orderIndex(1)
                    .build());
            entityManager.persist(ContactGroupMember.builder()
                    .contactGroup(group2)
                    .contact(contact)
                    .orderIndex(2)
                    .build());
            entityManager.flush();

            // When
            List<ContactGroupMember> result = contactGroupMemberRepository.findAllByContactId(contact.getId());

            // Then
            assertThat(result).hasSize(2);
        }
    }

    @Nested
    @DisplayName("countByContactGroupId 테스트")
    class CountByContactGroupIdTest {

        @Test
        @DisplayName("연락처 그룹 ID에 속한 멤버의 수를 반환한다")
        void countByContactGroupId_whenGroupIdMatches_thenReturnsCount() {
            // Given
            Contact contact1 = entityManager.persist(Contact.builder().name("A").type(ContactType.INDIVIDUAL).build());
            Contact contact2 = entityManager.persist(Contact.builder().name("B").type(ContactType.INDIVIDUAL).build());
            ContactGroup group = entityManager.persist(ContactGroup.builder().build());
            entityManager
                    .persist(ContactGroupMember.builder().contactGroup(group).contact(contact1).orderIndex(1).build());
            entityManager
                    .persist(ContactGroupMember.builder().contactGroup(group).contact(contact2).orderIndex(2).build());
            entityManager.flush();

            // When
            long count = contactGroupMemberRepository.countByContactGroupId(group.getId());

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("findContactIdsByContactGroupId 테스트")
    class FindContactIdsByContactGroupIdTest {

        @Test
        @DisplayName("연락처 그룹 ID에 속한 멤버의 연락처 ID만 반환한다")
        void findContactIdsByContactGroupId_whenGroupIdMatches_thenReturnsContactIdsOfGroup() {
            // Given
            Contact contact1 = entityManager.persist(Contact.builder().name("A").type(ContactType.INDIVIDUAL).build());
            Contact contact2 = entityManager.persist(Contact.builder().name("B").type(ContactType.INDIVIDUAL).build());
            Contact contact3 = entityManager.persist(Contact.builder().name("C").type(ContactType.INDIVIDUAL).build());
            ContactGroup group = entityManager.persist(ContactGroup.builder().build());
            ContactGroup otherGroup = entityManager.persist(ContactGroup.builder().build());
            entityManager
                    .persist(ContactGroupMember.builder().contactGroup(group).contact(contact1).orderIndex(1).build());
            entityManager
                    .persist(ContactGroupMember.builder().contactGroup(group).contact(contact2).orderIndex(2).build());
            entityManager.persist(
                    ContactGroupMember.builder().contactGroup(otherGroup).contact(contact3).orderIndex(1).build());
            entityManager.flush();

            // When
            Set<Long> result = contactGroupMemberRepository.findContactIdsByContactGroupId(group.getId());

            // Then
            assertThat(result).containsExactlyInAnyOrder(contact1.getId(), contact2.getId());
        }
    }

    @Nested
    @DisplayName("findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc 테스트")
    class FindAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAscTest {

        @Test
        @DisplayName("연락처 그룹 ID가 일치하고 숨겨지지 않은 멤버들을 orderIndex 오름차순으로 반환한다")
        void findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc_whenMatchesAndNotHidden_thenReturnsSortedMembers() {
            // Given
            Contact contactVisible = entityManager.persist(Contact.builder()
                    .name("보임")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            Contact contactHidden = entityManager.persist(Contact.builder()
                    .name("숨김")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(true)
                    .build());
            ContactGroup group = entityManager.persist(ContactGroup.builder().build());

            entityManager.persist(ContactGroupMember.builder()
                    .contactGroup(group)
                    .contact(contactVisible)
                    .orderIndex(2)
                    .build());
            entityManager.persist(ContactGroupMember.builder()
                    .contactGroup(group)
                    .contact(contactHidden)
                    .orderIndex(1)
                    .build());
            entityManager.persist(ContactGroupMember.builder()
                    .contactGroup(group)
                    .contact(contactVisible)
                    .orderIndex(3)
                    .build());
            entityManager.flush();

            // When
            List<ContactGroupMember> result = contactGroupMemberRepository
                    .findAllByContactGroupIdAndContactIsHiddenFalseOrderByOrderIndexAsc(group.getId());

            // Then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getOrderIndex()).isEqualTo(2);
            assertThat(result.get(1).getOrderIndex()).isEqualTo(3);
        }
    }
}
