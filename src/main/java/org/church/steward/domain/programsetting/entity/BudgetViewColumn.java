package org.church.steward.domain.programsetting.entity;

import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateConstraints;
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
 * 수입/지출 관리의 "예산별 보기"에서 표의 칸 하나의 순서와 숨김. 사용자별 설정이 아니라 프로그램 전체 설정이며, DB에 있으므로 백업에
 * 함께 들어간다. 표마다 그 표의 모든 칸을 한 행씩 저장한다.
 */
@Entity
@Table(name = "BUDGET_VIEW_COLUMN")
@IdClass(BudgetViewColumnId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetViewColumn {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "table_type", length = ProgramSettingConstraints.ENUM_MAX_LENGTH)
    private BudgetViewTable tableType;

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
    public BudgetViewColumn(BudgetViewTable tableType, ReportColumn columnType, int sortOrder, boolean hidden) {
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
