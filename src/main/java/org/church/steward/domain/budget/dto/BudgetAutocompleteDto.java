package org.church.steward.domain.budget.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetAutocompleteDto {
    private Long id;
    private String name;
    private String hierarchyName;
}
