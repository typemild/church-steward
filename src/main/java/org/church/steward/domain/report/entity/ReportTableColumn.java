package org.church.steward.domain.report.entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 보고서 양식에서 표의 칸 하나의 순서와 숨김. 사용자별 설정이 아니라 프로그램 전체 설정이며, DB에 있으므로 백업에 함께 들어간다.
 * 표마다 그 표의 모든 칸을 한 행씩 저장한다.
 */
@Entity
@Table(name = "REPORT_TABLE_COLUMN")
@IdClass(ReportTableColumnId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportTableColumn {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "table_type", length = ReportTemplateConstraints.TABLE_MAX_LENGTH)
    private ReportTemplateTable tableType;

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "column_type", length = ReportTemplateConstraints.COLUMN_MAX_LENGTH)
    private ReportColumn columnType;

    /** 표 안에서의 순서(0부터 시작) */
    @Column(nullable = false)
    private int sortOrder;

    @Column(nullable = false)
    private boolean hidden;

    @Builder
    public ReportTableColumn(ReportTemplateTable tableType, ReportColumn columnType, int sortOrder, boolean hidden) {
        this.tableType = tableType;
        this.columnType = columnType;
        this.sortOrder = sortOrder;
        this.hidden = hidden;
    }

    public void update(int sortOrder, boolean hidden) {
        this.sortOrder = sortOrder;
        this.hidden = hidden;
    }
}
