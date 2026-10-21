package org.church.steward.domain.contact.dto;

import java.util.List;

import org.church.steward.domain.contact.entity.ContactType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContactAutocompleteDto {
    private Long id;
    private ContactType type;
    private String name;
    private String remarks;
    private List<GroupInfoDto> groups;
}
