package org.church.steward.domain.activitylog.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ACTIVITY_LOG")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 행위를 수행한 사용자의 ID (User 엔티티의 PK인 String)
     */
    @Column(nullable = false)
    private String userId;

    /**
     * 행위 유형
     */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ActivityType activityType;

    /**
     * 로그가 발생한 대상 엔터티의 논리명
     */
    @Column(nullable = false)
    private String targetName;

    /**
     * 기록된 항목 목록을 담은 JSON이다. 예:
     * {@code {"fields":[{"name":"이름","oldValue":"변경 전","value":"변경 후"}]}}
     * "oldValue"는 값이 변경된 항목에만 있다. {@code @Lob}을 사용하여 길이에 제한이 없는 CLOB 타입으로 매핑한다.
     */
    @Lob
    @Column(nullable = false)
    private String description;

    /**
     * 발생 일시
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
