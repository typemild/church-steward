package org.church.steward.domain.contact.dto;

import org.church.steward.domain.contact.entity.ContactType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactSearchRequestDto {
    private ContactType type;
    private String name;
    private String remarks;
    private Boolean includeHidden;
}
