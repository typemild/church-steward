package org.church.steward.ui.component.table;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.table.AbstractTableModel;

/**
 * 행을 접고 펼칠 수 있는 표의 모델. 트리({@link TreeTableNode})를 받아, 펼쳐진 노드의 하위 노드까지만 행으로
 * 보여준다.
 * 하위 클래스는 {@link #getRowValue}로 행의 값을 얻어 칸의 수와 칸의 값을 정한다.
 * <p>
 * 펼침 상태는 노드의 키로 기억하므로, 트리를 새로 넣어도 같은 키의 노드는 접거나 펼친 상태를 유지한다.
 * 새 트리에 없는 키의 상태는 버린다.
 */
public abstract class AbstractTreeTableModel<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private final boolean expandedByDefault;
    /** 펼침 상태가 기본값과 다른 노드의 키 */
    private final Set<Object> toggledKeys = new HashSet<>();
    private List<TreeTableNode<T>> roots = List.of();
    /** 펼쳐진 노드의 하위 노드까지 순서대로 늘어놓은, 표에 보이는 행 */
    private List<VisibleRow<T>> rows = new ArrayList<>();

    /**
     * @param expandedByDefault 처음 보이는 노드를 펼쳐 둘지 여부
     */
    protected AbstractTreeTableModel(boolean expandedByDefault) {
        this.expandedByDefault = expandedByDefault;
    }

    /**
     * 보여줄 트리를 바꾼다.
     */
    public void setRoots(List<TreeTableNode<T>> roots) {
        Set<Object> keys = new HashSet<>();
        collectKeys(roots, keys);
        toggledKeys.retainAll(keys);

        List<VisibleRow<T>> newRows = new ArrayList<>();
        for (TreeTableNode<T> root : roots) {
            addVisibleRows(root, 0, newRows);
        }
        this.rows = newRows;
        this.roots = List.copyOf(roots);
        fireTableDataChanged();
    }

    /**
     * 행을 펼치거나 접는다. 하위 노드가 없는 행이면 아무 일도 하지 않는다.
     * 바뀌는 것은 그 행의 하위 행뿐이므로, 그 행과 앞의 행들의 위치는 그대로다.
     */
    public void toggleExpanded(int row) {
        TreeTableNode<T> node = rows.get(row)
                .node();
        if (!node.isExpandable()) {
            return;
        }
        if (!toggledKeys.remove(node.key())) {
            toggledKeys.add(node.key());
        }
        setRoots(roots);
    }

    /**
     * 하위 노드가 있는 모든 노드를 펼친다.
     */
    public void expandAll() {
        setAllExpanded(true);
    }

    /**
     * 하위 노드가 있는 모든 노드를 접는다. 최상위 노드만 보이게 된다.
     */
    public void collapseAll() {
        setAllExpanded(false);
    }

    /**
     * 보이는 행 가운데 펼쳐진 행이 있는지 확인한다. 접힌 노드 아래에 펼쳐진 노드가 숨어 있어도 보이지 않으므로 세지 않는다.
     */
    public boolean hasExpandedRow() {
        return rows.stream()
                .anyMatch(row -> isExpanded(row.node()));
    }

    /**
     * 접거나 펼칠 수 있는 행(하위 노드가 있는 노드)이 있는지 확인한다.
     */
    public boolean hasExpandableRow() {
        return roots.stream()
                .anyMatch(TreeTableNode::isExpandable);
    }

    private void setAllExpanded(boolean expanded) {
        toggledKeys.clear();
        if (expanded != expandedByDefault) {
            collectExpandableKeys(roots, toggledKeys);
        }
        setRoots(roots);
    }

    public T getRowValue(int row) {
        return rows.get(row)
                .node()
                .value();
    }

    /**
     * 행의 깊이. 최상위 노드는 0이다.
     */
    public int getDepth(int row) {
        return rows.get(row)
                .depth();
    }

    public boolean isExpandable(int row) {
        return rows.get(row)
                .node()
                .isExpandable();
    }

    public boolean isExpanded(int row) {
        return isExpanded(rows.get(row)
                .node());
    }

    /**
     * 키가 같은 노드의 행을 반환한다. 접힌 노드 아래에 있어 보이지 않거나 없으면 -1을 반환한다.
     */
    public int indexOfKey(Object key) {
        if (key == null) {
            return -1;
        }
        for (int i = 0; i < rows.size(); i++) {
            if (Objects.equals(rows.get(i)
                    .node()
                    .key(), key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 키가 같은 노드가 보이도록 그 노드의 상위 노드를 모두 펼치고, 그 노드의 행을 반환한다. 없으면 -1을 반환한다.
     */
    public int reveal(Object key) {
        if (key == null) {
            return -1;
        }
        List<TreeTableNode<T>> path = new ArrayList<>();
        if (!findPath(roots, key, path)) {
            return -1;
        }

        boolean changed = false;
        for (TreeTableNode<T> ancestor : path.subList(0, path.size() - 1)) {
            if (!isExpanded(ancestor)) {
                if (!toggledKeys.remove(ancestor.key())) {
                    toggledKeys.add(ancestor.key());
                }
                changed = true;
            }
        }
        if (changed) {
            setRoots(roots);
        }
        return indexOfKey(key);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    /**
     * 키가 같은 노드까지의 경로(최상위 노드부터 그 노드까지)를 path에 담는다. 찾지 못하면 path를 비워 두고 false를 반환한다.
     */
    private static <N> boolean findPath(List<TreeTableNode<N>> nodes, Object key, List<TreeTableNode<N>> path) {
        for (TreeTableNode<N> node : nodes) {
            path.add(node);
            if (Objects.equals(node.key(), key) || findPath(node.children(), key, path)) {
                return true;
            }
            path.removeLast();
        }
        return false;
    }

    private boolean isExpanded(TreeTableNode<T> node) {
        return node.isExpandable() && expandedByDefault != toggledKeys.contains(node.key());
    }

    private void addVisibleRows(TreeTableNode<T> node, int depth, List<VisibleRow<T>> visibleRows) {
        visibleRows.add(new VisibleRow<>(node, depth));
        if (isExpanded(node)) {
            for (TreeTableNode<T> child : node.children()) {
                addVisibleRows(child, depth + 1, visibleRows);
            }
        }
    }

    private static void collectKeys(List<? extends TreeTableNode<?>> nodes, Set<Object> keys) {
        for (TreeTableNode<?> node : nodes) {
            if (node.key() != null) {
                keys.add(node.key());
            }
            collectKeys(node.children(), keys);
        }
    }

    private static void collectExpandableKeys(List<? extends TreeTableNode<?>> nodes, Set<Object> keys) {
        for (TreeTableNode<?> node : nodes) {
            if (node.isExpandable()) {
                keys.add(node.key());
                collectExpandableKeys(node.children(), keys);
            }
        }
    }

    /**
     * 보이는 행 가운데 그 노드의 행을 반환한다. 접힌 노드 아래에 있어 보이지 않으면 -1을 반환한다.
     * 접거나 펼쳐도 노드 객체는 그대로이므로, 키가 없는 노드도 이것으로 찾을 수 있다.
     */
    int indexOfNode(TreeTableNode<?> node) {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i)
                    .node() == node) {
                return i;
            }
        }
        return -1;
    }

    TreeTableNode<T> getRowNode(int row) {
        return rows.get(row)
                .node();
    }

    private record VisibleRow<N>(TreeTableNode<N> node, int depth) {
    }

}
