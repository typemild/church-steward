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
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "USERS")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Loggable(name = "사용자")
public class User {

    @Id
    @Column(length = UserConstraints.ID_MAX_LENGTH)
    @LoggableField(name = "사용자명", isIdentifier = true)
    private String id; // 사용자ID(= 사용자명). 관리자는 "관리자"로 고정

    @Column(nullable = false)
    private String password; // Argon2id로 해싱된 비밀번호

    // 이번 수정에서 비밀번호를 바꿨는지 나타낸다. 저장하지 않으므로 DB에서 읽은 직후(스냅샷)에는 항상 false이고,
    // 비밀번호를 바꾸면 true가 되어 바꿀 때마다 활동 기록에 남는다.
    @Transient
    @LoggableField(name = "비밀번호 변경 여부", transformer = BooleanTypeTransformer.class)
    private boolean passwordChanged = false;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    @LoggableField(name = "계정 유형", transformer = UserRoleTransformer.class)
    private UserRole role;

    @Setter
    @Column(length = UserConstraints.REMARKS_MAX_LENGTH)
    @LoggableField(name = "비고")
    private String remarks;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSetting userSetting;

    @Builder
    public User(String id, String password, UserRole role, String remarks) {
        this.id = id;
        this.password = password;
        this.role = role;
        this.remarks = remarks;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    /**
     * 비밀번호를 변경한다.
     *
     * @param password Argon2id로 해싱된 비밀번호
     */
    public void setPassword(String password) {
        passwordChanged = true;
        this.password = password;
    }

}
