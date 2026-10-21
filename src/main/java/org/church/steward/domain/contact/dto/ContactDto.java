package org.church.steward.domain.contact.dto;

import org.church.steward.domain.contact.entity.ContactType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContactDto {
    private Long id;
    private ContactType type;
    private String name;
    private String remarks;
    private boolean isHidden;
}
