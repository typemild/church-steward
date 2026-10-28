package org.church.steward.ui.component.table;

import static org.assertj.core.api.Assertions.assertThat;

import java.awt.event.ActionEvent;

import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ListTable 테스트")
class ListTableTest {

    /**
     * 행이 세 개인 표. 첫 행을 고른 상태로 만든다.
     */
    private static ListTable createTable() {
        ListTable table = new ListTable(new DefaultTableModel(3, 2));
        table.setRowSelectionInterval(0, 0);
        return table;
    }

    private static void perform(ListTable table, String actionKey) {
        table.getActionMap()
                .get(actionKey)
                .actionPerformed(new ActionEvent(table, ActionEvent.ACTION_PERFORMED, actionKey));
    }

    @Nested
    @DisplayName("selectNextRow 테스트")
    class SelectNextRowTest {

        @Test
        @DisplayName("마지막 행이 선택된 상태에서 ↓ 키를 누르면 선택이 풀리지 않고 첫 행으로 넘어간다")
        void selectNextRow_lastRowSelected_selectsFirstRow() {
            // Given
            ListTable table = createTable();
            table.setRowSelectionInterval(2, 2);

            // When
            perform(table, ListTable.SELECT_NEXT_ROW_ACTION);

            // Then
            assertThat(table.getSelectedRows()).containsExactly(0);
        }

        @Test
        @DisplayName("중간 행이 선택된 상태에서 ↓ 키를 누르면 다음 행으로 옮긴다")
        void selectNextRow_middleRowSelected_selectsNextRow() {
            // Given
            ListTable table = createTable();
            table.setRowSelectionInterval(1, 1);

            // When
            perform(table, ListTable.SELECT_NEXT_ROW_ACTION);

            // Then
            assertThat(table.getSelectedRows()).containsExactly(2);
        }
    }

    @Nested
    @DisplayName("selectPreviousRow 테스트")
    class SelectPreviousRowTest {

        @Test
        @DisplayName("첫 행이 선택된 상태에서 ↑ 키를 누르면 선택이 풀리지 않고 마지막 행으로 넘어간다")
        void selectPreviousRow_firstRowSelected_selectsLastRow() {
            // Given
            ListTable table = createTable();

            // When
            perform(table, ListTable.SELECT_PREVIOUS_ROW_ACTION);

            // Then
            assertThat(table.getSelectedRows()).containsExactly(2);
        }

        @Test
        @DisplayName("중간 행이 선택된 상태에서 ↑ 키를 누르면 이전 행으로 옮긴다")
        void selectPreviousRow_middleRowSelected_selectsPreviousRow() {
            // Given
            ListTable table = createTable();
            table.setRowSelectionInterval(1, 1);

            // When
            perform(table, ListTable.SELECT_PREVIOUS_ROW_ACTION);

            // Then
            assertThat(table.getSelectedRows()).containsExactly(0);
        }
    }

    @Nested
    @DisplayName("changeSelection 테스트")
    class ChangeSelectionTest {

        @Test
        @DisplayName("마우스 클릭이 아니면 하나만 선택된 행으로 다시 불려도 선택을 풀지 않는다")
        void changeSelection_sameRowWithoutMouse_keepsSelection() {
            // Given
            ListTable table = createTable();

            // When
            table.changeSelection(0, 0, false, false);

            // Then
            assertThat(table.getSelectedRows()).containsExactly(0);
        }
    }

}
