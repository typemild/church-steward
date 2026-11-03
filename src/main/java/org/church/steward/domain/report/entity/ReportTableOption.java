package org.church.steward.domain.report.entity;

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
 * 보고서 양식에서 칸의 순서와 숨김 말고 표 단위로 정하는 것. 지금은 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부뿐이다
 * ({@link ReportTemplateTable#hasRateBarOption()}). 프로그램 전체 설정이며 DB에 있으므로 백업에
 * 함께 들어간다.
 */
@Entity
@Table(name = "REPORT_TABLE_OPTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportTableOption {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "table_type", length = ReportTemplateConstraints.TABLE_MAX_LENGTH)
    private ReportTemplateTable tableType;

    /** 비율 칸의 왼쪽에 막대를 함께 그리는지 여부 */
    @Column(nullable = false)
    private boolean rateBarVisible;

    @Builder
    public ReportTableOption(ReportTemplateTable tableType, boolean rateBarVisible) {
        this.tableType = tableType;
        this.rateBarVisible = rateBarVisible;
    }

    public void update(boolean rateBarVisible) {
        this.rateBarVisible = rateBarVisible;
    }
}
