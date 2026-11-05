package org.church.steward.domain.programsetting.entity;

import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportTemplateConstraints;
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
 * 수입/지출 관리의 "예산별 보기"에서 칸의 순서와 숨김 말고 표 단위로 정하는 것. 지금은 같은 날짜의 내역끼리의 순서뿐이다. 프로그램
 * 전체 설정이며 DB에 있으므로 백업에 함께 들어간다.
 */
@Entity
@Table(name = "BUDGET_VIEW_OPTION")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BudgetViewOption {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "table_type", length = ProgramSettingConstraints.ENUM_MAX_LENGTH)
    private BudgetViewTable tableType;

    /** 같은 날짜의 내역끼리의 순서 */
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = ReportTemplateConstraints.RECORD_ORDER_MAX_LENGTH)
    private RecordOrder recordOrder;

    @Builder
    public BudgetViewOption(BudgetViewTable tableType, RecordOrder recordOrder) {
        this.tableType = tableType;
        this.recordOrder = recordOrder;
    }

    public void update(RecordOrder recordOrder) {
        this.recordOrder = recordOrder;
    }
}
