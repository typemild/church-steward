package org.church.steward.domain.contact.mapper;

import java.util.List;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactGroup;
import org.springframework.stereotype.Component;

@Component
public class ContactMapper {

    public ContactDto toDto(Contact contact) {
        if (contact == null) {
            return null;
        }
        return ContactDto.builder()
                .id(contact.getId())
                .type(contact.getType())
                .name(contact.getName())
                .remarks(contact.getRemarks())
                .isHidden(contact.isHidden())
                .build();
    }

    public Contact toEntity(ContactDto dto) {
        if (dto == null) {
            return null;
        }
        return Contact.builder()
                .type(dto.getType())
                .name(dto.getName())
                .remarks(dto.getRemarks())
                .isHidden(dto.isHidden())
                .build();
    }

    public ContactAutocompleteDto toAutocompleteDto(Contact contact, List<GroupInfoDto> groups) {
        if (contact == null) {
            return null;
        }
        return ContactAutocompleteDto.builder()
                .id(contact.getId())
                .type(contact.getType())
                .name(contact.getName())
                .remarks(contact.getRemarks())
                .groups(groups)
                .build();
    }

    public GroupInfoDto toGroupInfoDto(ContactGroup group, List<ContactSummaryDto> members) {
        if (group == null) {
            return null;
        }
        return GroupInfoDto.builder()
                .groupId(group.getId())
                .members(members)
                .build();
    }

    public ContactSummaryDto toSummaryDto(Contact contact) {
        if (contact == null) {
            return null;
        }
        return ContactSummaryDto.builder()
                .id(contact.getId())
                .type(contact.getType())
                .name(contact.getName())
                .remarks(contact.getRemarks())
                .build();
    }
}
