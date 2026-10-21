package org.church.steward.domain.report.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.church.steward.domain.budget.entity.Budget;

import lombok.Getter;

/**
 * 한 회계 연도, 한 구분(수입 또는 지출)의 예산 계층.
 * 예산 관리 화면의 트리와 같이, 같은 상위 예산 아래에서는 정렬 순서(sortOrder)를 따르고 하위 예산은 상위 예산 바로 뒤에 온다.
 */
final class BudgetTree {

    private static final Comparator<Budget> SIBLING_ORDER = Comparator.comparingInt(Budget::getSortOrder)
            .thenComparing(Budget::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final List<Node> roots = new ArrayList<>();
    private final Map<Long, Node> nodesById = new HashMap<>();

    private BudgetTree() {
    }

    /**
     * @param budgets 같은 회계 연도, 같은 구분의 예산 전체
     */
    static BudgetTree of(List<Budget> budgets) {
        Map<Long, List<Budget>> childrenByParentId = new HashMap<>();
        List<Budget> rootBudgets = new ArrayList<>();
        for (Budget budget : budgets) {
            if (budget.getParent() == null) {
                rootBudgets.add(budget);
            } else {
                childrenByParentId.computeIfAbsent(budget.getParent()
                        .getId(), _ -> new ArrayList<>())
                        .add(budget);
            }
        }

        BudgetTree tree = new BudgetTree();
        rootBudgets.sort(SIBLING_ORDER);
        for (Budget budget : rootBudgets) {
            tree.roots.add(tree.build(budget, null, childrenByParentId));
        }
        return tree;
    }

    private Node build(Budget budget, Node parent, Map<Long, List<Budget>> childrenByParentId) {
        Node node = new Node(budget, parent);
        nodesById.put(budget.getId(), node);

        List<Budget> children = childrenByParentId.get(budget.getId());
        if (children == null) {
            node.budgetedAmount = budget.getAmount() != null ? budget.getAmount() : 0L;
            return node;
        }

        // 하위 예산이 있는 예산의 금액은 하위 예산들의 합계다.
        children.sort(SIBLING_ORDER);
        for (Budget child : children) {
            Node childNode = build(child, node, childrenByParentId);
            node.children.add(childNode);
            node.budgetedAmount += childNode.budgetedAmount;
        }
        return node;
    }

    List<Node> getRoots() {
        return roots;
    }

    /**
     * 모든 예산을 표시 순서대로 나열한다.
     */
    List<Node> flatten() {
        List<Node> nodes = new ArrayList<>();
        collect(roots, nodes);
        return nodes;
    }

    private static void collect(List<Node> siblings, List<Node> nodes) {
        for (Node node : siblings) {
            nodes.add(node);
            collect(node.children, nodes);
        }
    }

    /**
     * 예산 ID로 노드를 찾는다. 없으면 null
     */
    Node find(Long budgetId) {
        return nodesById.get(budgetId);
    }

    long getTotalBudgetedAmount() {
        long total = 0;
        for (Node root : roots) {
            total += root.budgetedAmount;
        }
        return total;
    }

    @Getter
    static final class Node {

        private final Budget budget;
        private final Node parent;
        private final int depth;
        private final List<Node> children = new ArrayList<>();
        private long budgetedAmount;

        private Node(Budget budget, Node parent) {
            this.budget = budget;
            this.parent = parent;
            this.depth = parent == null ? 0 : parent.depth + 1;
        }

        /**
         * 최상위 예산부터 이 예산까지의 예산명. 직전 회계 연도의 같은 예산을 찾는 기준이다.
         */
        List<String> getPath() {
            List<String> path = parent == null ? new ArrayList<>() : parent.getPath();
            path.add(budget.getName());
            return path;
        }

        /**
         * 전체 예산명("예산A > 예산B > 예산C")
         */
        String getHierarchyName() {
            return String.join(" > ", getPath());
        }

        /**
         * 이 예산과 그 하위 예산들에 등록된 금액의 합계
         *
         * @param amountsByBudgetId 예산 ID별 내역 금액 합계
         */
        long sumActualAmount(Map<Long, Long> amountsByBudgetId) {
            long total = amountsByBudgetId.getOrDefault(budget.getId(), 0L);
            for (Node child : children) {
                total += child.sumActualAmount(amountsByBudgetId);
            }
            return total;
        }
    }
}
