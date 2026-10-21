package org.church.steward.ui.component.table;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractTreeTableModel 테스트")
class AbstractTreeTableModelTest {

    /**
     * 행의 값을 그대로 첫 칸에 보여주는 모델.
     */
    private static class TestTreeTableModel extends AbstractTreeTableModel<String> {

        private static final long serialVersionUID = 1L;

        TestTreeTableModel(boolean expandedByDefault) {
            super(expandedByDefault);
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return getRowValue(rowIndex);
        }
    }

    /**
     * A(하위 A1, A2), B로 이루어진 트리. A1은 하위 A1a를 가진다. 노드의 키는 값과 같다.
     */
    private static List<TreeTableNode<String>> sampleTree() {
        TreeTableNode<String> a1 = new TreeTableNode<>("A1", "A1", List.of(TreeTableNode.leaf("A1a", "A1a")));
        TreeTableNode<String> a = new TreeTableNode<>("A", "A", List.of(a1, TreeTableNode.leaf("A2", "A2")));
        return List.of(a, TreeTableNode.leaf("B", "B"));
    }

    private static List<String> visibleValues(AbstractTreeTableModel<String> model) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < model.getRowCount(); i++) {
            values.add(model.getRowValue(i));
        }
        return values;
    }

    @Nested
    @DisplayName("setRoots 테스트")
    class SetRootsTest {

        @Test
        @DisplayName("기본으로 펼치는 모델은 모든 노드를 깊이 우선 순서로 보여준다")
        void setRoots_ExpandedByDefault_ShowsAllNodes() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);

            // When
            model.setRoots(sampleTree());

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A1a", "A2", "B");
        }

        @Test
        @DisplayName("기본으로 접는 모델은 최상위 노드만 보여준다")
        void setRoots_CollapsedByDefault_ShowsRootsOnly() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);

            // When
            model.setRoots(sampleTree());

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "B");
        }

        @Test
        @DisplayName("행의 깊이는 최상위 노드가 0이고 한 단계 내려갈 때마다 1씩 늘어난다")
        void setRoots_NestedNodes_ReturnsDepthPerRow() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);

            // When
            model.setRoots(sampleTree());

            // Then
            assertThat(List.of(model.getDepth(0), model.getDepth(1), model.getDepth(2), model.getDepth(3),
                    model.getDepth(4))).containsExactly(0, 1, 2, 1, 0);
        }

        @Test
        @DisplayName("트리를 다시 넣어도 같은 키의 노드는 접은 상태를 유지한다")
        void setRoots_SameKeys_KeepsToggledState() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.toggleExpanded(0);

            // When
            model.setRoots(sampleTree());

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "B");
        }

        @Test
        @DisplayName("새 트리에 없는 키의 접은 상태는 버려서, 같은 키가 다시 나타나면 기본 상태로 보인다")
        void setRoots_KeyRemovedThenRestored_ResetsToDefault() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.toggleExpanded(0);
            model.setRoots(List.of(TreeTableNode.leaf("B", "B")));

            // When
            model.setRoots(sampleTree());

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A1a", "A2", "B");
        }
    }

    @Nested
    @DisplayName("toggleExpanded 테스트")
    class ToggleExpandedTest {

        @Test
        @DisplayName("펼쳐진 노드를 접으면 그 아래의 모든 하위 노드가 숨겨진다")
        void toggleExpanded_ExpandedNode_HidesDescendants() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            model.toggleExpanded(0);

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "B");
        }

        @Test
        @DisplayName("접힌 노드를 펼치면 바로 아래 하위 노드가 나타나고, 그 하위 노드는 기본 상태를 따른다")
        void toggleExpanded_CollapsedNode_ShowsChildren() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);
            model.setRoots(sampleTree());

            // When
            model.toggleExpanded(0);

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A2", "B");
        }

        @Test
        @DisplayName("상위 노드를 접었다 펼쳐도 하위 노드를 접은 상태는 유지된다")
        void toggleExpanded_ParentToggledTwice_KeepsChildState() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.toggleExpanded(1); // A1을 접는다.
            model.toggleExpanded(0); // A를 접는다.

            // When
            model.toggleExpanded(0);

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A2", "B");
        }

        @Test
        @DisplayName("하위 노드가 없는 행은 접거나 펼쳐도 바뀌지 않는다")
        void toggleExpanded_LeafNode_DoesNothing() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            model.toggleExpanded(4);

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A1a", "A2", "B");
        }
    }

    @Nested
    @DisplayName("expandAll 테스트")
    class ExpandAllTest {

        @Test
        @DisplayName("기본으로 접는 모델에서 모두 펼치면 모든 깊이의 노드가 보인다")
        void expandAll_CollapsedByDefault_ShowsAllNodes() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);
            model.setRoots(sampleTree());

            // When
            model.expandAll();

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A1a", "A2", "B");
        }

        @Test
        @DisplayName("기본으로 펼치는 모델에서 일부를 접었어도 모두 펼치면 모든 노드가 보인다")
        void expandAll_SomeCollapsed_ShowsAllNodes() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.toggleExpanded(1); // A1을 접는다.

            // When
            model.expandAll();

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A1a", "A2", "B");
        }
    }

    @Nested
    @DisplayName("collapseAll 테스트")
    class CollapseAllTest {

        @Test
        @DisplayName("기본으로 펼치는 모델에서 모두 접으면 최상위 노드만 보인다")
        void collapseAll_ExpandedByDefault_ShowsRootsOnly() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            model.collapseAll();

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "B");
        }

        @Test
        @DisplayName("모두 접은 뒤 최상위 노드 하나를 펼치면 그 아래 하위 노드도 접힌 채로 보인다")
        void collapseAll_ThenToggleRoot_ShowsCollapsedChildren() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.collapseAll();

            // When
            model.toggleExpanded(0);

            // Then
            assertThat(visibleValues(model)).containsExactly("A", "A1", "A2", "B");
        }
    }

    @Nested
    @DisplayName("hasExpandedRow 테스트")
    class HasExpandedRowTest {

        @Test
        @DisplayName("펼쳐진 행이 하나라도 보이면 true를 반환한다")
        void hasExpandedRow_OneRowExpanded_ReturnsTrue() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);
            model.setRoots(sampleTree());
            model.toggleExpanded(0);

            // When
            boolean result = model.hasExpandedRow();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("모든 행이 접혀 있으면 false를 반환한다")
        void hasExpandedRow_AllCollapsed_ReturnsFalse() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);
            model.setRoots(sampleTree());

            // When
            boolean result = model.hasExpandedRow();

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("펼쳐진 노드가 접힌 상위 노드 아래에 숨어 있으면 세지 않는다")
        void hasExpandedRow_ExpandedNodeHiddenUnderCollapsedParent_ReturnsFalse() {
            // Given : A1은 펼친 채로 두고 A만 접는다.
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());
            model.toggleExpanded(0);

            // When
            boolean result = model.hasExpandedRow();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("hasExpandableRow 테스트")
    class HasExpandableRowTest {

        @Test
        @DisplayName("하위 노드가 있는 노드가 있으면 true를 반환한다")
        void hasExpandableRow_TreeWithChildren_ReturnsTrue() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            boolean result = model.hasExpandableRow();

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("모든 노드가 말단이면 false를 반환한다")
        void hasExpandableRow_LeavesOnly_ReturnsFalse() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(List.of(TreeTableNode.leaf("A", "A"), TreeTableNode.leaf("B", "B")));

            // When
            boolean result = model.hasExpandableRow();

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("isExpanded 테스트")
    class IsExpandedTest {

        @Test
        @DisplayName("하위 노드가 없는 행은 펼쳐진 것으로 보지 않는다")
        void isExpanded_LeafNode_ReturnsFalse() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            boolean expanded = model.isExpanded(4);

            // Then
            assertThat(expanded).isFalse();
        }
    }

    @Nested
    @DisplayName("indexOfKey 테스트")
    class IndexOfKeyTest {

        @Test
        @DisplayName("보이는 노드의 키로 찾으면 그 행의 위치를 반환한다")
        void indexOfKey_VisibleNode_ReturnsRow() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(sampleTree());

            // When
            int index = model.indexOfKey("A2");

            // Then
            assertThat(index).isEqualTo(3);
        }

        @Test
        @DisplayName("접힌 노드 아래에 있어 보이지 않는 노드는 -1을 반환한다")
        void indexOfKey_HiddenNode_ReturnsMinusOne() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(false);
            model.setRoots(sampleTree());

            // When
            int index = model.indexOfKey("A2");

            // Then
            assertThat(index).isEqualTo(-1);
        }

        @Test
        @DisplayName("키가 null이면 키가 없는 노드가 있어도 -1을 반환한다")
        void indexOfKey_NullKey_ReturnsMinusOne() {
            // Given
            TestTreeTableModel model = new TestTreeTableModel(true);
            model.setRoots(List.of(TreeTableNode.leaf(null, "키 없음")));

            // When
            int index = model.indexOfKey(null);

            // Then
            assertThat(index).isEqualTo(-1);
        }
    }

}
