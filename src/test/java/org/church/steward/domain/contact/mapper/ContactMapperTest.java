package org.church.steward.domain.contact.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.church.steward.domain.contact.entity.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ContactMapperTest {

    private final ContactMapper contactMapper = new ContactMapper();

    @Nested
    @DisplayName("toDto 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("Contact 엔티티가 존재할 때, ContactDto로 올바르게 변환되어야 한다")
        void toDto_ValidContact_ReturnsContactDto() {
            // Given
            Contact contact = Contact.builder()
                    .id(1L)
                    .type(ContactType.INDIVIDUAL)
                    .name("John Doe")
                    .remarks("Remarks")
                    .isHidden(false)
                    .build();

            // When
            ContactDto result = contactMapper.toDto(contact);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contact.getId());
            assertThat(result.getType()).isEqualTo(contact.getType());
            assertThat(result.getName()).isEqualTo(contact.getName());
            assertThat(result.getRemarks()).isEqualTo(contact.getRemarks());
            assertThat(result.isHidden()).isEqualTo(contact.isHidden());
        }

        @Test
        @DisplayName("Contact 엔티티가 null일 때, null을 반환해야 한다")
        void toDto_NullContact_ReturnsNull() {
            // Given
            Contact contact = null;

            // When
            ContactDto result = contactMapper.toDto(contact);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toEntity 테스트")
    class ToEntityTest {

        @Test
        @DisplayName("ContactDto가 존재할 때, Contact 엔티티로 올바르게 변환되어야 한다")
        void toEntity_ValidDto_ReturnsContact() {
            // Given
            ContactDto dto = ContactDto.builder()
                    .type(ContactType.INDIVIDUAL)
                    .name("John Doe")
                    .remarks("Remarks")
                    .isHidden(false)
                    .build();

            // When
            Contact result = contactMapper.toEntity(dto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(dto.getType());
            assertThat(result.getName()).isEqualTo(dto.getName());
            assertThat(result.getRemarks()).isEqualTo(dto.getRemarks());
            assertThat(result.isHidden()).isEqualTo(dto.isHidden());
        }

        @Test
        @DisplayName("ContactDto가 null일 때, null을 반환해야 한다")
        void toEntity_NullDto_ReturnsNull() {
            // Given
            ContactDto dto = null;

            // When
            Contact result = contactMapper.toEntity(dto);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toAutocompleteDto 테스트")
    class ToAutocompleteDtoTest {

        @Test
        @DisplayName("Contact 엔티티와 그룹 목록이 존재할 때, ContactAutocompleteDto로 올바르게 변환되어야 한다")
        void toAutocompleteDto_ValidContactAndGroups_ReturnsContactAutocompleteDto() {
            // Given
            Contact contact = Contact.builder()
                    .id(1L)
                    .type(ContactType.VENDOR)
                    .name("John Doe")
                    .remarks("Remarks")
                    .build();
            List<GroupInfoDto> groups = List.of(GroupInfoDto.builder().groupId(10L).build());

            // When
            ContactAutocompleteDto result = contactMapper.toAutocompleteDto(contact, groups);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contact.getId());
            assertThat(result.getType()).isEqualTo(contact.getType());
            assertThat(result.getName()).isEqualTo(contact.getName());
            assertThat(result.getRemarks()).isEqualTo(contact.getRemarks());
            assertThat(result.getGroups()).isEqualTo(groups);
        }

        @Test
        @DisplayName("Contact 엔티티가 null일 때, null을 반환해야 한다")
        void toAutocompleteDto_NullContact_ReturnsNull() {
            // Given
            Contact contact = null;
            List<GroupInfoDto> groups = List.of();

            // When
            ContactAutocompleteDto result = contactMapper.toAutocompleteDto(contact, groups);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toGroupInfoDto 테스트")
    class ToGroupInfoDtoTest {

        @Test
        @DisplayName("ContactGroup 엔티티와 멤버 목록이 존재할 때, GroupInfoDto로 올바르게 변환되어야 한다")
        void toGroupInfoDto_ValidGroupAndMembers_ReturnsGroupInfoDto() {
            // Given
            ContactGroup group = ContactGroup.builder()
                    .id(10L)
                    .build();
            List<ContactSummaryDto> members = List.of(ContactSummaryDto.builder().id(1L).name("Member").build());

            // When
            GroupInfoDto result = contactMapper.toGroupInfoDto(group, members);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getGroupId()).isEqualTo(group.getId());
            assertThat(result.getMembers()).isEqualTo(members);
        }

        @Test
        @DisplayName("ContactGroup 엔티티가 null일 때, null을 반환해야 한다")
        void toGroupInfoDto_NullGroup_ReturnsNull() {
            // Given
            ContactGroup group = null;
            List<ContactSummaryDto> members = List.of();

            // When
            GroupInfoDto result = contactMapper.toGroupInfoDto(group, members);

            // Then
            assertThat(result).isNull();
        }
    }

    @Nested
    @DisplayName("toSummaryDto 테스트")
    class ToSummaryDtoTest {

        @Test
        @DisplayName("Contact 엔티티가 존재할 때, ContactSummaryDto로 올바르게 변환되어야 한다")
        void toSummaryDto_ValidContact_ReturnsContactSummaryDto() {
            // Given
            Contact contact = Contact.builder()
                    .id(1L)
                    .type(ContactType.VENDOR)
                    .name("John Doe")
                    .remarks("Remarks")
                    .build();

            // When
            ContactSummaryDto result = contactMapper.toSummaryDto(contact);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(contact.getId());
            assertThat(result.getType()).isEqualTo(contact.getType());
            assertThat(result.getName()).isEqualTo(contact.getName());
            assertThat(result.getRemarks()).isEqualTo(contact.getRemarks());
        }

        @Test
        @DisplayName("Contact 엔티티가 null일 때, null을 반환해야 한다")
        void toSummaryDto_NullContact_ReturnsNull() {
            // Given
            Contact contact = null;

            // When
            ContactSummaryDto result = contactMapper.toSummaryDto(contact);

            // Then
            assertThat(result).isNull();
        }
    }
}
