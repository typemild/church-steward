package org.church.steward.domain.contact.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactAutocompleteResultDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.contact.service.ContactService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContactControllerTest {

    @Mock
    private ContactService contactService;

    @InjectMocks
    private ContactController contactController;

    @Nested
    @DisplayName("listContacts 테스트")
    class ListContactsTest {

        @Test
        @DisplayName("검색 요청 시 - 연락처 목록을 반환한다")
        void listContacts_searchRequest_returnsList() {
            // Given
            ContactSearchRequestDto searchRequest = ContactSearchRequestDto.builder()
                    .includeHidden(true)
                    .build();
            List<ContactDto> expectedList = List.of(
                    ContactDto.builder().id(1L).name("테스트").build());
            when(contactService.getContacts(searchRequest)).thenReturn(expectedList);

            // When
            List<ContactDto> actualList = contactController.listContacts(searchRequest);

            // Then
            assertThat(actualList).isEqualTo(expectedList);
        }

    }

    @Nested
    @DisplayName("getContactsByIds 테스트")
    class GetContactsByIdsTest {

        @Test
        @DisplayName("ID 목록 전달 시 - 해당 관계자 목록을 반환한다")
        void getContactsByIds_validIds_returnsList() {
            // Given
            List<Long> ids = List.of(1L, 2L);
            List<ContactDto> expectedList = List.of(
                    ContactDto.builder().id(1L).name("테스트1").build(),
                    ContactDto.builder().id(2L).name("테스트2").build());
            when(contactService.getContactsByIds(ids)).thenReturn(expectedList);

            // When
            List<ContactDto> actualList = contactController.getContactsByIds(ids);

            // Then
            assertThat(actualList).isEqualTo(expectedList);
        }

    }

    @Nested
    @DisplayName("createContact 테스트")
    class CreateContactTest {

        @Test
        @DisplayName("유효한 DTO 전달 시 - 생성된 DTO를 반환한다")
        void createContact_validDto_returnsCreatedDto() {
            // Given
            ContactDto dto = ContactDto.builder().name("새로운 봉헌자").build();
            when(contactService.createContact(dto)).thenReturn(dto);

            // When
            ContactDto result = contactController.createContact(dto);

            // Then
            assertThat(result).isEqualTo(dto);
        }

        @Test
        @DisplayName("유효한 DTO 전달 시 - 서비스의 createContact를 호출한다")
        void createContact_validDto_callsService() {
            // Given
            ContactDto dto = ContactDto.builder().name("새로운 봉헌자").build();

            // When
            contactController.createContact(dto);

            // Then
            verify(contactService).createContact(dto);
        }
    }

    @Nested
    @DisplayName("updateContact 테스트")
    class UpdateContactTest {

        @Test
        @DisplayName("ID와 정보 전달 시 - 서비스의 updateContact를 호출한다")
        void updateContact_validParams_callsService() {
            // Given
            Long id = 1L;
            ContactType type = ContactType.OTHER;
            String name = "수정된 이름";
            String remarks = "수정된 비고";
            boolean isHidden = false;

            // When
            contactController.updateContact(id, type, name, remarks, isHidden);

            // Then
            verify(contactService).updateContact(id, type, name, remarks, isHidden);
        }
    }

    @Nested
    @DisplayName("deleteContact 테스트")
    class DeleteContactTest {

        @Test
        @DisplayName("ID 전달 시 - 서비스의 deleteContact를 호출한다")
        void deleteContact_validId_callsService() {
            // Given
            Long id = 1L;

            // When
            contactController.deleteContact(id);

            // Then
            verify(contactService).deleteContact(id);
        }
    }

    @Nested
    @DisplayName("hideContact 테스트")
    class HideContactTest {

        @Test
        @DisplayName("ID 전달 시 - 서비스의 hideContact를 호출한다")
        void hideContact_validId_callsService() {
            // Given
            Long id = 1L;

            // When
            contactController.hideContact(id);

            // Then
            verify(contactService).hideContact(id);
        }
    }

    @Nested
    @DisplayName("unhideContact 테스트")
    class UnhideContactTest {

        @Test
        @DisplayName("ID 전달 시 - 서비스의 unhideContact를 호출한다")
        void unhideContact_validId_callsService() {
            // Given
            Long id = 1L;

            // When
            contactController.unhideContact(id);

            // Then
            verify(contactService).unhideContact(id);
        }
    }

    @Nested
    @DisplayName("autocompleteContacts 테스트")
    class AutocompleteContactsTest {

        @Test
        @DisplayName("키워드, 제외 목록, 제한 수 전달 시 - 자동완성 결과를 반환한다")
        void autocompleteContacts_validKeywordAndLimit_returnsAutocompleteResult() {
            // Given
            String keyword = "테스트";
            List<Long> excludedIds = List.of(2L);
            int limit = 5;
            ContactAutocompleteResultDto<ContactAutocompleteDto> expected = ContactAutocompleteResultDto
                    .<ContactAutocompleteDto>builder()
                    .contacts(List.of(ContactAutocompleteDto.builder().id(1L).name("테스트").build()))
                    .totalCount(1)
                    .build();
            when(contactService.autocompleteContacts(keyword, excludedIds, limit)).thenReturn(expected);

            // When
            ContactAutocompleteResultDto<ContactAutocompleteDto> actual = contactController
                    .autocompleteContacts(keyword, excludedIds, limit);

            // Then
            assertThat(actual).isEqualTo(expected);
        }

    }

    @Nested
    @DisplayName("autocompleteContactSummaries 테스트")
    class AutocompleteContactSummariesTest {

        @Test
        @DisplayName("키워드, 제외 목록, 제한 수 전달 시 - 요약 자동완성 결과를 반환한다")
        void autocompleteContactSummaries_validKeywordAndLimit_returnsSummaryResult() {
            // Given
            String keyword = "테스트";
            List<Long> excludedIds = List.of(2L);
            int limit = 5;
            ContactAutocompleteResultDto<ContactSummaryDto> expected = ContactAutocompleteResultDto
                    .<ContactSummaryDto>builder()
                    .contacts(List.of(ContactSummaryDto.builder().id(1L).name("테스트").remarks("비고").build()))
                    .totalCount(1)
                    .build();
            when(contactService.autocompleteContactSummaries(keyword, excludedIds, limit)).thenReturn(expected);

            // When
            ContactAutocompleteResultDto<ContactSummaryDto> actual = contactController
                    .autocompleteContactSummaries(keyword, excludedIds, limit);

            // Then
            assertThat(actual).isEqualTo(expected);
        }

    }
}
