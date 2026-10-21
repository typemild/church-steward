package org.church.steward.ui.component.table;

import java.util.List;

/**
 * {@link TreeTable}에 보여줄 트리의 노드. 하위 노드가 있으면 접고 펼칠 수 있다.
 *
 * @param key      펼침 상태를 기억하고 행의 위치를 찾는 데 쓰는 식별자. 같은 표 안에서 겹치지 않아야 한다.
 *                 하위 노드가 없고 위치를 찾을 일도 없는 노드는 null로 둘 수 있다.
 * @param value    행의 값
 * @param children 하위 노드
 */
public record TreeTableNode<T>(Object key, T value, List<TreeTableNode<T>> children) {

    public TreeTableNode {
        children = List.copyOf(children);
    }

    /**
     * 하위 노드가 없는 노드를 만든다.
     */
    public static <T> TreeTableNode<T> leaf(Object key, T value) {
        return new TreeTableNode<>(key, value, List.of());
    }

    public boolean isExpandable() {
        return !children.isEmpty();
    }

}
