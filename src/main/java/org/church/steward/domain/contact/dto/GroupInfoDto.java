package org.church.steward.domain.contact.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInfoDto {
    private Long groupId;
    /** 그룹 구성원. 그룹 내 순서대로 담는다. */
    private List<ContactSummaryDto> members;
}
