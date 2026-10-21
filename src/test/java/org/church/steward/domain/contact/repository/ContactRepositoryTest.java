package org.church.steward.domain.contact.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
class ContactRepositoryTest {

    @Autowired
    private ContactRepository contactRepository;

    @Nested
    @DisplayName("findByNameContainingOrderByNameAsc 테스트")
    class FindByNameContainingOrderByNameAscTest {

        @Test
        @DisplayName("이름에 특정 문자열이 포함된 연락처를 이름순으로 조회한다")
        void findByNameContainingOrderByNameAsc_whenNameContains_returnsSortedContacts() {
            // Given
            contactRepository.save(Contact.builder()
                    .name("가나다")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("마바사")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("아자차")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());

            // When
            List<Contact> result = contactRepository.findByNameContainingOrderByNameAsc("가", PageRequest.of(0, 10));

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("가나다");
        }
    }

    @Nested
    @DisplayName("findByNameContainingAndIsHiddenFalseOrderByNameAsc 테스트")
    class FindByNameContainingAndIsHiddenFalseOrderByNameAscTest {

        @Test
        @DisplayName("이름에 특정 문자열이 포함되고 숨김 상태가 아닌 연락처를 이름순으로 조회한다")
        void findByNameContainingAndIsHiddenFalseOrderByNameAsc_whenNameContainsAndNotHidden_returnsSortedVisibleContacts() {
            // Given
            contactRepository.save(Contact.builder()
                    .name("검색대상1")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("검색대상2")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("검색대상_숨김")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(true)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("기타")
                    .type(ContactType.INDIVIDUAL)
                    .isHidden(false)
                    .build());

            // When
            List<Contact> result = contactRepository.findByNameContainingAndIsHiddenFalseOrderByNameAsc("검색대상",
                    PageRequest.of(0, 10));

            // Then
            assertThat(result).hasSize(2);
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("검색대상1", "검색대상2");
        }
    }

    @Nested
    @DisplayName("searchContacts 테스트")
    class SearchContactsTest {

        @Test
        @DisplayName("모든 조건이 일치하는 연락처를 조회한다")
        void searchContacts_allConditionsMatch_returnsMatchedContacts() {
            // Given
            contactRepository.save(Contact.builder()
                    .name("홍길동")
                    .type(ContactType.INDIVIDUAL)
                    .remarks("비고")
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("이순신")
                    .type(ContactType.INDIVIDUAL)
                    .remarks("비고")
                    .isHidden(false)
                    .build());
            contactRepository.save(Contact.builder()
                    .name("홍길동")
                    .type(ContactType.VENDOR)
                    .remarks("다른비고")
                    .isHidden(false)
                    .build());

            // When
            List<Contact> result = contactRepository.searchContacts(ContactType.INDIVIDUAL, "홍길동", "비고", true);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("이름 조건만 일치하는 연락처를 조회한다")
        void searchContacts_onlyNameMatches_returnsMatchedContacts() {
            // Given
            contactRepository.save(Contact.builder().name("테스트").type(ContactType.INDIVIDUAL).isHidden(false).build());
            contactRepository.save(Contact.builder().name("다른이름").type(ContactType.INDIVIDUAL).isHidden(false).build());

            // When
            List<Contact> result = contactRepository.searchContacts(null, "테스트", null, true);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("테스트");
        }

        @Test
        @DisplayName("숨김 포함 여부가 false일 때 숨겨진 연락처는 제외한다")
        void searchContacts_includeHiddenFalse_excludesHiddenContacts() {
            // Given
            contactRepository
                    .save(Contact.builder().name("보이는사람").type(ContactType.INDIVIDUAL).isHidden(false).build());
            contactRepository.save(Contact.builder().name("숨겨진사람").type(ContactType.INDIVIDUAL).isHidden(true).build());

            // When
            List<Contact> result = contactRepository.searchContacts(null, null, null, false);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("보이는사람");
        }

        @Test
        @DisplayName("숨김 포함 여부가 true일 때 숨겨진 연락처를 포함한다")
        void searchContacts_includeHiddenTrue_includesHiddenContacts() {
            // Given
            contactRepository
                    .save(Contact.builder().name("보이는사람").type(ContactType.INDIVIDUAL).isHidden(false).build());
            contactRepository.save(Contact.builder().name("숨겨진사람").type(ContactType.INDIVIDUAL).isHidden(true).build());

            // When
            List<Contact> result = contactRepository.searchContacts(null, null, null, true);

            // Then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("조건에 맞는 연락처가 없을 경우 빈 리스트를 반환한다")
        void searchContacts_noMatch_returnsEmptyList() {
            // Given
            contactRepository.save(Contact.builder().name("홍길동").type(ContactType.INDIVIDUAL).isHidden(false).build());

            // When
            List<Contact> result = contactRepository.searchContacts(ContactType.VENDOR, "홍길동", null, true);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("검색 결과가 이름순(오름차순)으로 정렬되어 반환된다")
        void searchContacts_returnsSortedByNames() {
            // Given
            contactRepository.save(Contact.builder().name("다라").type(ContactType.INDIVIDUAL).isHidden(false).build());
            contactRepository.save(Contact.builder().name("가나").type(ContactType.INDIVIDUAL).isHidden(false).build());
            contactRepository.save(Contact.builder().name("마바").type(ContactType.INDIVIDUAL).isHidden(false).build());

            // When
            List<Contact> result = contactRepository.searchContacts(null, null, null, true);

            // Then
            assertThat(result).hasSize(3);
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("가나", "다라", "마바");
        }
    }
}
