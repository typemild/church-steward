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
 * 보고서 양식에서 칸의 순서와 숨김 말고 표 단위로 정하는 것. 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지
 * 여부({@link ReportTemplateTable#hasRateBarOption()})와 주간 내역에서 같은 날짜의 내역끼리의
 * 순서({@link ReportTemplateTable#hasRecordOrderOption()})다. 표마다 정할 수 있는 것만 쓰고,
 * 나머지 칸에는 기본값을 둔다. 프로그램 전체 설정이며 DB에 있으므로 백업에 함께 들어간다.
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

    /** 같은 날짜의 내역끼리의 순서 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ReportTemplateConstraints.RECORD_ORDER_MAX_LENGTH)
    private RecordOrder recordOrder;

    @Builder
    public ReportTableOption(ReportTemplateTable tableType, boolean rateBarVisible, RecordOrder recordOrder) {
        this.tableType = tableType;
        this.rateBarVisible = rateBarVisible;
        this.recordOrder = recordOrder;
    }

    /**
     * 아직 저장한 적이 없는 표의 기본 설정
     */
    public static ReportTableOption defaults(ReportTemplateTable tableType) {
        return ReportTableOption.builder()
                .tableType(tableType)
                .rateBarVisible(ReportTemplateConstraints.DEFAULT_RATE_BAR_VISIBLE)
                .recordOrder(ReportTemplateConstraints.DEFAULT_RECORD_ORDER)
                .build();
    }

    public void update(boolean rateBarVisible, RecordOrder recordOrder) {
        this.rateBarVisible = rateBarVisible;
        this.recordOrder = recordOrder;
    }
}
