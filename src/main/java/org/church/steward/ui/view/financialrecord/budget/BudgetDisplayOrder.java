package org.church.steward.ui.view.financialrecord.budget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.dto.BudgetDto;

/**
 * 예산의 표시 순서를 계산한다. 예산 설정 화면의 트리와 같이, 같은 상위 예산 아래에서는 정렬 순서(sortOrder)를 따르고 하위
 * 예산은 상위 예산 바로 뒤에 온다.
 */
public final class BudgetDisplayOrder {

    private static final Comparator<BudgetDto> SIBLING_ORDER = Comparator.comparingInt(BudgetDto::getSortOrder)
            .thenComparing(BudgetDto::getId);

    private BudgetDisplayOrder() {
    }

    /**
     * 예산 ID별 표시 위치를 반환한다. 값이 작을수록 먼저 표시한다.
     */
    public static Map<Long, Integer> positionsOf(List<BudgetDto> budgets) {
        Map<Long, List<BudgetDto>> childrenMap = new HashMap<>();
        List<BudgetDto> roots = new ArrayList<>();
        for (BudgetDto budget : budgets) {
            if (budget.getParentId() == null) {
                roots.add(budget);
            } else {
                childrenMap.computeIfAbsent(budget.getParentId(), _ -> new ArrayList<>())
                        .add(budget);
            }
        }

        Map<Long, Integer> positions = new HashMap<>();
        collect(roots, childrenMap, positions);
        return positions;
    }

    private static void collect(List<BudgetDto> siblings, Map<Long, List<BudgetDto>> childrenMap,
            Map<Long, Integer> positions) {
        siblings.sort(SIBLING_ORDER);
        for (BudgetDto budget : siblings) {
            positions.put(budget.getId(), positions.size());

            List<BudgetDto> children = childrenMap.get(budget.getId());
            if (children != null) {
                collect(children, childrenMap, positions);
            }
        }
    }
}
