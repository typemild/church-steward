package org.church.steward.domain.contact.entity;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.BooleanTypeTransformer;
import org.church.steward.domain.contact.transformer.ContactTypeTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 관계자(수입에서의 봉헌자, 지출에서의 수취인)에 대한 정보를 나타내는 엔터티
 */
@Entity
@Table(name = "CONTACT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Loggable(name = "관계자")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    @LoggableField(name = "구분", isIdentifier = true, transformer = ContactTypeTransformer.class)
    private ContactType type;

    @Setter
    @Column(nullable = false, length = ContactConstraints.NAME_MAX_LENGTH)
    @LoggableField(name = "이름", isIdentifier = true)
    private String name;

    @Setter
    @Column(length = ContactConstraints.REMARKS_MAX_LENGTH)
    @LoggableField(name = "비고")
    private String remarks;

    @Setter
    @Column(nullable = false)
    @LoggableField(name = "숨김 여부", transformer = BooleanTypeTransformer.class)
    private boolean isHidden;

}
