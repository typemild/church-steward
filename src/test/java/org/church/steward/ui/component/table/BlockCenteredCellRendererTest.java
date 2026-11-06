package org.church.steward.ui.component.table;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.awt.Insets;
import java.util.List;

import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BlockCenteredCellRenderer 테스트")
class BlockCenteredCellRendererTest {

    private static final String WIDEST = "OO설비 주식회사";
    private static final String NARROW = "홍길동";

    /**
     * 칸 하나의 폭을 정한 표
     */
    private static JTable createTable(int columnWidth) {
        JTable table = new JTable(new DefaultTableModel(2, 1));
        table.getColumnModel()
                .getColumn(0)
                .setWidth(columnWidth);
        return table;
    }

    @Nested
    @DisplayName("getTableCellRendererComponent 테스트")
    class GetTableCellRendererComponentTest {

        @Test
        @DisplayName("칸이 넓으면 가장 넓은 값을 칸의 가운데에 둔다")
        void getTableCellRendererComponent_WideColumn_CentersWidestValue() {
            // Given
            JTable table = createTable(200);
            BlockCenteredCellRenderer renderer = new BlockCenteredCellRenderer(() -> List.of(WIDEST, NARROW));

            // When
            renderer.getTableCellRendererComponent(table, WIDEST, false, false, 0, 0);

            // Then
            Insets insets = renderer.getInsets();
            int textWidth = renderer.getFontMetrics(renderer.getFont())
                    .stringWidth(WIDEST);
            double leftSpace = insets.left;
            double rightSpace = 200 - insets.left - textWidth;
            assertThat(leftSpace).isCloseTo(rightSpace, within(1.0));
        }

        @Test
        @DisplayName("짧은 값은 가장 넓은 값과 같은 위치에서 시작한다")
        void getTableCellRendererComponent_NarrowValue_StartsAtWidestValue() {
            // Given
            JTable table = createTable(200);
            BlockCenteredCellRenderer renderer = new BlockCenteredCellRenderer(() -> List.of(WIDEST, NARROW));
            renderer.getTableCellRendererComponent(table, WIDEST, false, false, 0, 0);
            int widestLeft = renderer.getInsets().left;

            // When
            renderer.getTableCellRendererComponent(table, NARROW, false, false, 1, 0);

            // Then
            assertThat(renderer.getInsets().left).isEqualTo(widestLeft);
        }

        @Test
        @DisplayName("칸이 가장 넓은 값보다 좁으면 다른 칸과 같은 여백으로 왼쪽 정렬한다")
        void getTableCellRendererComponent_NarrowColumn_AlignsLeft() {
            // Given
            JTable table = createTable(30);
            BlockCenteredCellRenderer renderer = new BlockCenteredCellRenderer(() -> List.of(WIDEST, NARROW));
            PaddedCellRenderer padded = new PaddedCellRenderer(SwingConstants.LEADING);
            padded.getTableCellRendererComponent(table, NARROW, false, false, 1, 0);

            // When
            renderer.getTableCellRendererComponent(table, NARROW, false, false, 1, 0);

            // Then
            assertThat(renderer.getInsets().left).isEqualTo(padded.getInsets().left);
        }
    }
}
