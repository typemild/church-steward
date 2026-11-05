package org.church.steward.domain.programsetting.entity;

import java.time.DayOfWeek;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로그램 전체에 하나뿐인 설정. 사용자별 설정과 달리 모든 사용자가 같은 값을 쓰며, DB에 있으므로 백업에 함께 들어간다. 행은
 * {@link ProgramSettingConstraints#SETTING_ID} 하나뿐이다.
 */
@Entity
@Table(name = "PROGRAM_SETTING")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProgramSetting {

    @Id
    private Long id;

    /** 주간 결산 요일. 주간은 이 요일의 다음 날부터 이 요일까지다. */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ProgramSettingConstraints.ENUM_MAX_LENGTH)
    private DayOfWeek weeklyClosingDay;

    @Builder
    public ProgramSetting(Long id, DayOfWeek weeklyClosingDay) {
        this.id = id;
        this.weeklyClosingDay = weeklyClosingDay;
    }

    /**
     * 아직 저장한 적이 없을 때 적용하는 기본 설정
     */
    public static ProgramSetting defaults() {
        return ProgramSetting.builder()
                .id(ProgramSettingConstraints.SETTING_ID)
                .weeklyClosingDay(ProgramSettingConstraints.DEFAULT_WEEKLY_CLOSING_DAY)
                .build();
    }

    public void update(DayOfWeek weeklyClosingDay) {
        this.weeklyClosingDay = weeklyClosingDay;
    }
}
