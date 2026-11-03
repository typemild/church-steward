package org.church.steward.ui.component.panel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ColumnOrderRule;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.ListTable;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 표 하나의 칸 순서와 숨김을 정하는 패널. 보고서 양식 설정과 예산별 보기의 칸 설정이 함께 쓴다. 칸 목록의 체크를 끄면 그 칸을
 * 숨기고, 위로/아래로 버튼으로 순서를 바꾼다. 첫 번째 칸에 올 수 없는 칸은 그 자리로 옮길 수 없고, 숨길 수 없는 칸(첫 번째 칸
 * 포함)은 체크를 끌 수 없다. 숨긴 칸을 첫 번째 칸으로 옮기면 다시 보이게 한다. 비율 칸의 막대를 정할 수 있는 표(현황 표)는 목록
 * 위에 비율 칸(달성률/집행률)의 왼쪽에 막대를 함께 그릴지 고르는 콤보박스를 둔다.
 */
public class ColumnOrderPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final int CHECK_COLUMN_WIDTH = 50;
    private static final int TABLE_HEIGHT = 170;
    /** 비율 칸의 막대를 고르는 콤보박스의 항목 */
    private static final String RATE_BAR_SHOWN = "막대와 함께 표시";
    private static final String RATE_BAR_HIDDEN = "비율만 표시";

    private final ColumnOrderRule rule;
    private final ColumnTableModel model = new ColumnTableModel();
    private final ListTable listTable = new ListTable(model);
    private final IconButton upButton = new IconButton(FontAwesomeSolid.ARROW_UP, ComponentSize.SMALL);
    private final IconButton downButton = new IconButton(FontAwesomeSolid.ARROW_DOWN, ComponentSize.SMALL);
    private final StyledComboBox<String> rateBarComboBox = new StyledComboBox<>(ComponentSize.MEDIUM);

    /**
     * 칸 하나의 편집 상태
     */
    private static final class Row {
        private final ReportColumn column;
        private boolean hidden;

        Row(ReportColumn column, boolean hidden) {
            this.column = column;
            this.hidden = hidden;
        }
    }

    /**
     * @param rateBarVisible 비율 칸의 왼쪽에 막대를 함께 그리는지 여부. 막대를 정할 수 없는 표이면 쓰지 않는다.
     * @param extraHints     표의 규칙 안내 아래에 덧붙일 문장. 칸을 숨기거나 폭을 나누는 방식처럼 표를 쓰는 곳마다 다른
     *                       내용이다.
     */
    public ColumnOrderPanel(ColumnOrderRule rule, List<ReportColumnDto> columns, boolean rateBarVisible,
            List<String> extraHints) {
        this.rule = rule;
        setOpaque(false);
        setLayout(new BorderLayout(0, GAP));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        if (rule.hasRateBarOption()) {
            add(createRateBarPanel(rateBarVisible), BorderLayout.NORTH);
        }

        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.getColumnModel()
                .getColumn(ColumnTableModel.SHOWN_COLUMN)
                .setMaxWidth(CHECK_COLUMN_WIDTH);
        listTable.getColumnModel()
                .getColumn(ColumnTableModel.SHOWN_COLUMN)
                .setMinWidth(CHECK_COLUMN_WIDTH);
        // 숨길 수 없는 칸의 체크박스는 꺼진 모양으로 그린다.
        TableCellRenderer checkRenderer = listTable.getDefaultRenderer(Boolean.class);
        listTable.getColumnModel()
                .getColumn(ColumnTableModel.SHOWN_COLUMN)
                .setCellRenderer(
                        (JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) -> {
                            Component component = checkRenderer.getTableCellRendererComponent(t, value, isSelected,
                                    hasFocus,
                                    row, column);
                            component.setEnabled(model.isCellEditable(row, column));
                            return component;
                        });
        listTable.getSelectionModel()
                .addListSelectionListener(_ -> updateButtons());

        JScrollPane scrollPane = new JScrollPane(listTable);
        scrollPane.setPreferredSize(new Dimension(0, TABLE_HEIGHT));

        upButton.setToolTipText("위로 이동");
        downButton.setToolTipText("아래로 이동");
        upButton.addActionListener(_ -> move(-1));
        downButton.addActionListener(_ -> move(1));

        JPanel buttonPanel = new JPanel();
        buttonPanel.setOpaque(false);
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.add(upButton);
        buttonPanel.add(Box.createVerticalStrut(4));
        buttonPanel.add(downButton);

        JPanel listPanel = new JPanel(new BorderLayout(GAP, 0));
        listPanel.setOpaque(false);
        listPanel.add(scrollPane, BorderLayout.CENTER);
        listPanel.add(buttonPanel, BorderLayout.EAST);

        add(listPanel, BorderLayout.CENTER);
        add(new SubFieldLabel(createHint(extraHints), ComponentSize.SMALL), BorderLayout.SOUTH);

        setColumns(columns);
    }

    /**
     * 비율 칸(달성률/집행률)의 왼쪽에 막대를 함께 그릴지 고르는 줄
     */
    private JPanel createRateBarPanel(boolean rateBarVisible) {
        String rateLabel = rule.labelOf(ReportColumn.RATE);
        rateBarComboBox.addItem(RATE_BAR_SHOWN);
        rateBarComboBox.addItem(RATE_BAR_HIDDEN);
        rateBarComboBox.setSelectedItem(rateBarVisible ? RATE_BAR_SHOWN : RATE_BAR_HIDDEN);

        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panel.setOpaque(false);
        panel.add(new FieldLabel(rateLabel + " 표시", ComponentSize.MEDIUM));
        panel.add(Box.createHorizontalStrut(GAP));
        panel.add(rateBarComboBox);
        return panel;
    }

    /**
     * 표의 규칙을 알리는 안내 문구. 레이블에서 줄을 바꾸려면 HTML을 써야 한다.
     */
    private String createHint(List<String> extraHints) {
        List<String> lines = new ArrayList<>();
        List<String> candidates = rule.getDefaultColumns()
                .stream()
                .filter(rule::canBeFirst)
                .map(rule::labelOf)
                .toList();
        if (candidates.size() == 1) {
            lines.add(String.format("%s 칸은 항상 첫 번째 칸입니다.", candidates.get(0)));
        } else {
            lines.add(String.format("첫 번째 칸에는 %s만 둘 수 있고, 일자는 그 칸의 왼쪽에 붙여 적습니다.",
                    String.join(" 또는 ", candidates)));
        }
        String hideable = rule.getDefaultColumns()
                .stream()
                .filter(rule.getHideableColumns()::contains)
                .map(rule::labelOf)
                .collect(Collectors.joining(", "));
        if (!hideable.isEmpty()) {
            lines.add(String.format("체크를 끄면 칸을 숨깁니다(숨길 수 있는 칸: %s).", hideable));
        }
        lines.addAll(extraHints);
        return "<html>" + String.join("<br>", lines) + "</html>";
    }

    /**
     * 비율 칸의 왼쪽에 막대를 함께 그리는지 여부. 막대를 정할 수 없는 표이면 의미가 없다.
     */
    public boolean isRateBarVisible() {
        return RATE_BAR_SHOWN.equals(rateBarComboBox.getSelectedItem());
    }

    /**
     * 칸의 순서와 숨김. 목록의 순서가 칸의 순서다.
     */
    public List<ReportColumnDto> getColumns() {
        return model.rows.stream()
                .map(row -> ReportColumnDto.builder()
                        .column(row.column)
                        .hidden(row.hidden)
                        .build())
                .toList();
    }

    public void setColumns(List<ReportColumnDto> columns) {
        model.rows.clear();
        for (ReportColumnDto column : columns) {
            model.rows.add(new Row(column.getColumn(), column.isHidden()));
        }
        model.fireTableDataChanged();
        updateButtons();
    }

    private void move(int direction) {
        int from = listTable.getSelectedRow();
        int to = from + direction;
        if (!canMove(from, to)) {
            return;
        }

        Row row = model.rows.remove(from);
        model.rows.add(to, row);
        // 첫 번째 칸은 숨길 수 없으므로 숨긴 칸이 첫 번째로 오면 다시 보이게 한다.
        model.rows.get(0).hidden = false;
        model.fireTableDataChanged();
        listTable.setRowSelectionInterval(to, to);
        updateButtons();
    }

    /**
     * 칸을 옮길 수 있는지 여부. 옮긴 뒤의 첫 번째 칸이 그 자리에 올 수 있는 칸이어야 한다.
     */
    private boolean canMove(int from, int to) {
        if (from < 0 || to < 0 || to >= model.rows.size()) {
            return false;
        }
        if (from != 0 && to != 0) {
            return true;
        }
        ReportColumn newFirst = (to == 0 ? model.rows.get(from) : model.rows.get(to)).column;
        return rule.canBeFirst(newFirst);
    }

    private void updateButtons() {
        int selected = listTable.getSelectedRow();
        upButton.setEnabled(canMove(selected, selected - 1));
        downButton.setEnabled(canMove(selected, selected + 1));
    }

    /**
     * 칸 목록의 모델. 첫 칸은 보이는지 여부(체크박스), 둘째 칸은 칸의 이름이다.
     */
    private final class ColumnTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1L;

        static final int SHOWN_COLUMN = 0;

        private final List<Row> rows = new ArrayList<>();

        @Override
        public int getRowCount() {
            return rows.size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            return column == SHOWN_COLUMN ? "표시" : "칸";
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == SHOWN_COLUMN ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == SHOWN_COLUMN && rule.canHide(rows.get(row).column, row == 0);
        }

        @Override
        public Object getValueAt(int row, int column) {
            Row value = rows.get(row);
            return column == SHOWN_COLUMN ? !value.hidden : rule.labelOf(value.column);
        }

        @Override
        public void setValueAt(Object value, int row, int column) {
            if (column == SHOWN_COLUMN && value instanceof Boolean shown && isCellEditable(row, column)) {
                rows.get(row).hidden = !shown;
                fireTableRowsUpdated(row, row);
            }
        }
    }
}
