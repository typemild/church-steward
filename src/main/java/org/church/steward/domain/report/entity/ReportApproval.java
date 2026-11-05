package org.church.steward.domain.report.entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
 * 보고서의 결재란 양식. 사용자별 설정이 아니라 프로그램 전체 설정이며, DB에 있으므로 백업에 함께 들어간다. 직함은 칸 순서대로 저장하며
 * 칸 수보다 뒤의 직함은 비워 둔다.
 */
@Entity
@Table(name = "REPORT_APPROVAL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReportApproval {

    @Id
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "report_type", length = ReportSettingConstraints.REPORT_TYPE_MAX_LENGTH)
    private ReportType reportType;

    /** 결재란을 보고서에 그리는지 여부 */
    @Column(nullable = false)
    private boolean visible;

    @Column(nullable = false)
    private int cellCount;

    /** 칸마다의 직함. 비어 있으면 손으로 쓸 수 있게 빈칸으로 둔다. */
    @Column(length = ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH)
    private String title1;

    @Column(length = ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH)
    private String title2;

    @Column(length = ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH)
    private String title3;

    @Column(length = ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH)
    private String title4;

    @Column(length = ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH)
    private String title5;

    /**
     * @param titles 칸마다의 직함. 개수가 칸 수다. 빈 직함은 null이다.
     */
    @Builder
    public ReportApproval(ReportType reportType, boolean visible, List<String> titles) {
        this.reportType = reportType;
        update(visible, titles);
    }

    /**
     * 정한 적이 없는 보고서에 적용하는 기본 결재란. 직함이 비어 있는 칸 세 개다.
     */
    public static ReportApproval defaults(ReportType reportType) {
        return ReportApproval.builder()
                .reportType(reportType)
                .visible(ReportTemplateConstraints.DEFAULT_APPROVAL_VISIBLE)
                .titles(Collections.nCopies(ReportTemplateConstraints.DEFAULT_APPROVAL_CELL_COUNT, null))
                .build();
    }

    /**
     * @param titles 칸마다의 직함. 개수가 칸 수다. 빈 직함은 null이다.
     */
    public void update(boolean visible, List<String> titles) {
        this.visible = visible;
        this.cellCount = titles.size();
        this.title1 = titleAt(titles, 0);
        this.title2 = titleAt(titles, 1);
        this.title3 = titleAt(titles, 2);
        this.title4 = titleAt(titles, 3);
        this.title5 = titleAt(titles, 4);
    }

    /**
     * 칸마다의 직함. 개수가 칸 수이며 빈 직함은 null이다.
     */
    public List<String> getTitles() {
        List<String> all = Arrays.asList(title1, title2, title3, title4, title5);
        return new ArrayList<>(all.subList(0, Math.min(cellCount, all.size())));
    }

    private static String titleAt(List<String> titles, int index) {
        return index < titles.size() ? titles.get(index) : null;
    }
}
