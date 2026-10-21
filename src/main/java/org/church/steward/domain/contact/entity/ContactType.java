package org.church.steward.domain.contact.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContactType {

    INDIVIDUAL("개인"),
    INTERNAL_DEPARTMENT("내부부서"),
    VENDOR("업체"),
    ORGANIZATION("단체");

    private final String label;

}
