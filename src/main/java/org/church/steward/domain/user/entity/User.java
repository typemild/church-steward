package org.church.steward.domain.user.entity;

import org.church.steward.domain.activitylog.annotation.Loggable;
import org.church.steward.domain.activitylog.annotation.LoggableField;
import org.church.steward.domain.activitylog.transformer.BooleanTypeTransformer;
import org.church.steward.domain.user.transformer.UserRoleTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Loggable(name = "사용자")
public class User {

    @Id
    @Column(length = 100)
    @LoggableField(name = "사용자명", isIdentifier = true)
    private String id; // 사용자ID(= 사용자명). 관리자는 "관리자"로 고정

    @Column(nullable = false)
    private String password; // Argon2id로 해싱된 비밀번호

    @Builder.Default
    @LoggableField(name = "비밀번호 변경 여부", transformer = BooleanTypeTransformer.class)
    private boolean isPasswordChaged = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @LoggableField(name = "계정 유형", transformer = UserRoleTransformer.class)
    private UserRole role;

    @Setter
    @Column(length = 100)
    @LoggableField(name = "비고")
    private String remarks;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * 비밀번호를 변경한다.
     *
     * @param password Argon2id로 해싱된 비밀번호
     */
    public void setPassword(String password) {
        isPasswordChaged = true;
        this.password = password;
    }

}
