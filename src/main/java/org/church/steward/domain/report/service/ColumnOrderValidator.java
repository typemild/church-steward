package org.church.steward.domain.report.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ColumnOrderRule;
import org.church.steward.domain.report.entity.ReportColumn;

/**
 * 표의 칸 순서와 숨김이 규칙({@link ColumnOrderRule})에 맞는지 확인한다. 보고서 양식과 예산별 보기의 칸 설정이 함께
 * 쓴다.
 */
public final class ColumnOrderValidator {

    private ColumnOrderValidator() {
    }

    /**
     * 칸 목록에 빠진 항목(목록이나 칸이 null)이 있는지 여부
     */
    public static boolean hasMissing(List<ReportColumnDto> columns) {
        return columns == null || columns.stream()
                .anyMatch(column -> column == null || column.getColumn() == null);
    }

    /**
     * 표의 칸이 규칙에 맞는지 확인한다. 표의 모든 칸이 한 번씩 있어야 하고, 첫 번째 칸은 그 자리에 올 수 있는 칸이어야 하며, 숨긴 칸은
     * 숨길 수 있는 칸이어야 한다. 빠진 항목이 없는지({@link #hasMissing})는 먼저 확인해야 한다.
     *
     * @return 맞지 않으면 사용자에게 보일 이유, 맞으면 null
     */
    public static String findError(ColumnOrderRule rule, List<ReportColumnDto> columns) {
        Set<ReportColumn> distinct = new HashSet<>();
        for (ReportColumnDto column : columns) {
            distinct.add(column.getColumn());
        }
        if (columns.size() != rule.getDefaultColumns()
                .size() || !distinct.equals(Set.copyOf(rule.getDefaultColumns()))) {
            return String.format("%s의 칸 구성이 올바르지 않습니다.", rule.getLabel());
        }

        ReportColumn first = columns.get(0)
                .getColumn();
        if (!rule.canBeFirst(first)) {
            String candidates = rule.getDefaultColumns()
                    .stream()
                    .filter(rule::canBeFirst)
                    .map(rule::labelOf)
                    .collect(Collectors.joining(" 또는 "));
            return String.format("%s의 첫 번째 칸에는 %s만 둘 수 있습니다.", rule.getLabel(), candidates);
        }
        for (int i = 0; i < columns.size(); i++) {
            ReportColumnDto column = columns.get(i);
            if (column.isHidden() && !rule.canHide(column.getColumn(), i == 0)) {
                return String.format("%s의 %s 칸은 숨길 수 없습니다.", rule.getLabel(), rule.labelOf(column.getColumn()));
            }
        }
        return null;
    }
}
