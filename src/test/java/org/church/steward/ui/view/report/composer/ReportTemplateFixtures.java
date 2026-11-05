package org.church.steward.ui.view.report.composer;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.domain.report.entity.ReportType;

/**
 * 회계 연도 단위 보고서의 composer 테스트에서 쓰는 보고서 양식. 결재란이 있는 보고서는 직함이 빈 세 칸을 보이는 기본 결재란을
 * 담는다.
 */
final class ReportTemplateFixtures {

    private ReportTemplateFixtures() {
        // 인스턴스화 방지
    }

    /**
     * 결재란. 칸 수는 직함의 개수다.
     */
    static ReportApprovalDto createApproval(boolean visible, String... titles) {
        return ReportApprovalDto.builder()
                .visible(visible)
                .titles(Arrays.asList(titles))
                .build();
    }

    /**
     * 기본 양식에서 결재란만 바꾼다.
     */
    static ReportTemplateDto withApproval(ReportTemplateDto template, ReportApprovalDto approval) {
        return ReportTemplateDto.builder()
                .reportType(template.getReportType())
                .approval(approval)
                .tables(template.getTables())
                .rateBarVisible(template.getRateBarVisible())
                .recordOrders(template.getRecordOrders())
                .build();
    }

    /**
     * 칸들을 순서대로 담는다. 숨긴 칸은 없다.
     */
    static List<ReportColumnDto> createColumns(List<ReportColumn> columns) {
        return createColumns(columns, Set.of());
    }

    /**
     * @param hidden 숨길 칸
     */
    static List<ReportColumnDto> createColumns(List<ReportColumn> columns, Set<ReportColumn> hidden) {
        return columns.stream()
                .map(column -> ReportColumnDto.builder()
                        .column(column)
                        .hidden(hidden.contains(column))
                        .build())
                .toList();
    }

    /**
     * 기본 양식(모든 표가 기본 순서, 현황 표의 막대는 보임, 내역 표의 같은 날짜는 가나다순)
     */
    static ReportTemplateDto createDefaultTemplate(ReportType reportType) {
        return createTemplate(reportType, Map.of(), true);
    }

    /**
     * @param overrides      기본 순서 대신 쓸 표의 칸
     * @param rateBarVisible 모든 현황 표의 비율 칸 왼쪽에 막대를 함께 그리는지 여부
     */
    static ReportTemplateDto createTemplate(ReportType reportType,
            Map<ReportTemplateTable, List<ReportColumnDto>> overrides, boolean rateBarVisible) {
        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBars = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, RecordOrder> recordOrders = new EnumMap<>(ReportTemplateTable.class);
        for (ReportTemplateTable table : ReportTemplateTable.of(reportType)) {
            tables.put(table, overrides.getOrDefault(table, createColumns(table.getDefaultColumns())));
            if (table.hasRateBarOption()) {
                rateBars.put(table, rateBarVisible);
            }
            if (table.hasRecordOrderOption()) {
                recordOrders.put(table, RecordOrder.FIRST_COLUMN);
            }
        }
        return ReportTemplateDto.builder()
                .reportType(reportType)
                .approval(reportType.hasApproval() ? createApproval(true, null, null, null) : null)
                .tables(tables)
                .rateBarVisible(rateBars)
                .recordOrders(recordOrders)
                .build();
    }
}
