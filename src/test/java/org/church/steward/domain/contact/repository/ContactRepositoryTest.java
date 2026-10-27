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
    @DisplayName("findAutocompleteCandidates 테스트")
    class FindAutocompleteCandidatesTest {

        @Test
        @DisplayName("이름 일치, 이름이 검색어로 시작, 이름에 검색어 포함, 비고에 검색어 포함 순으로 정렬하고 같은 순위는 이름순으로 정렬한다")
        void findAutocompleteCandidates_variousMatches_returnsSortedByPriorityThenName() {
            // Given
            saveContact("홍길동", null, false);
            saveContact("김철수", "길동 친구", false);
            saveContact("길동이", null, false);
            saveContact("가길동", null, false);
            saveContact("길동", null, false);

            // When
            List<Contact> result = contactRepository.findAutocompleteCandidates("길동", List.of(),
                    PageRequest.of(0, 10));

            // Then
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("길동", "길동이", "가길동", "홍길동", "김철수");
        }

        @Test
        @DisplayName("숨긴 관계자와 이름과 비고 모두에 검색어가 없는 관계자는 제외한다")
        void findAutocompleteCandidates_hiddenOrNotMatching_excludesThem() {
            // Given
            saveContact("길동", null, false);
            saveContact("길동숨김", null, true);
            saveContact("무관", null, false);
            saveContact("무관비고", "다른 비고", false);

            // When
            List<Contact> result = contactRepository.findAutocompleteCandidates("길동", List.of(),
                    PageRequest.of(0, 10));

            // Then
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("길동");
        }

        @Test
        @DisplayName("제외 목록에 있는 관계자는 결과에서 뺀다")
        void findAutocompleteCandidates_excludedIds_excludesThem() {
            // Given
            Contact excluded = saveContact("길동1", null, false);
            saveContact("길동2", null, false);

            // When
            List<Contact> result = contactRepository.findAutocompleteCandidates("길동", List.of(excluded.getId()),
                    PageRequest.of(0, 10));

            // Then
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("길동2");
        }

        @Test
        @DisplayName("검색어의 %와 _는 패턴이 아닌 글자 그대로 찾는다")
        void findAutocompleteCandidates_keywordWithWildcards_matchesLiterally() {
            // Given
            saveContact("100%회비", null, false);
            saveContact("100원회비", null, false);
            saveContact("A_B", null, false);
            saveContact("AxB", null, false);

            // When
            List<Contact> percentResult = contactRepository.findAutocompleteCandidates("100%", List.of(),
                    PageRequest.of(0, 10));
            List<Contact> underscoreResult = contactRepository.findAutocompleteCandidates("A_B", List.of(),
                    PageRequest.of(0, 10));

            // Then
            assertThat(percentResult).extracting(Contact::getName)
                    .containsExactly("100%회비");
            assertThat(underscoreResult).extracting(Contact::getName)
                    .containsExactly("A_B");
        }

        @Test
        @DisplayName("결과는 페이지 크기만큼 앞에서부터 담긴다")
        void findAutocompleteCandidates_moreThanPageSize_returnsFirstPage() {
            // Given
            saveContact("길동다", null, false);
            saveContact("길동가", null, false);
            saveContact("길동나", null, false);

            // When
            List<Contact> result = contactRepository.findAutocompleteCandidates("길동", List.of(),
                    PageRequest.of(0, 2));

            // Then
            assertThat(result).extracting(Contact::getName)
                    .containsExactly("길동가", "길동나");
        }
    }

    @Nested
    @DisplayName("countAutocompleteCandidates 테스트")
    class CountAutocompleteCandidatesTest {

        @Test
        @DisplayName("조회와 같은 조건(숨김 제외, 제외 목록 제외, 이름이나 비고에 검색어 포함)으로 건수를 센다")
        void countAutocompleteCandidates_sameConditionAsFind_returnsCount() {
            // Given
            Contact excluded = saveContact("길동1", null, false);
            saveContact("길동2", null, false);
            saveContact("김철수", "길동 친구", false);
            saveContact("길동숨김", null, true);
            saveContact("무관", null, false);

            // When
            long count = contactRepository.countAutocompleteCandidates("길동", List.of(excluded.getId()));

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("제외 목록이 비어 있으면 조건에 맞는 관계자를 모두 센다")
        void countAutocompleteCandidates_emptyExcludedIds_countsAll() {
            // Given
            saveContact("길동1", null, false);
            saveContact("길동2", null, false);

            // When
            long count = contactRepository.countAutocompleteCandidates("길동", List.of());

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    private Contact saveContact(String name, String remarks, boolean hidden) {
        return contactRepository.save(Contact.builder()
                .name(name)
                .type(ContactType.INDIVIDUAL)
                .remarks(remarks)
                .isHidden(hidden)
                .build());
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
