package org.church.steward.ui.view.datamanagement.budgetimport;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.datamanagement.budgetimport.BudgetImportSession.SelectionSummary;
import org.church.steward.ui.view.datamanagement.importing.CheckableImportTable;
import org.church.steward.ui.view.datamanagement.importing.CheckableTableModel;

/**
 * 예산 가져오기의 3단계. 분석한 건마다 상태와 사유를 보여주고 가져올 건을 체크박스로 고르게 한다. 예산은 계층이므로 하위 예산을 고르면
 * 새로 추가되는 상위 예산도 함께 골라지고, 상위 예산을 빼면 하위 예산도 빠진다.
 */
class ReviewStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int COLUMN_SELECTED = 0;
    private static final int COLUMN_ROW_NUMBER = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_NAME = 3;
    private static final int COLUMN_AMOUNT = 4;
    private static final int COLUMN_STATE = 5;
    private static final int COLUMN_MESSAGE = 6;
    private static final String[] COLUMN_NAMES = { "선택", "행", "구분", "예산", "금액", "상태", "설명" };

    private static final int CELL_PADDING = 6;

    private final BudgetImportSession session;
    private final Runnable onSelectionChanged;

    private final FieldLabel summaryLabel = new FieldLabel(" ", ComponentSize.MEDIUM, true);
    private final SubFieldLabel countLabel = new SubFieldLabel(" ", ComponentSize.MEDIUM);
    // 표는 만들어질 때 모델을 읽고 모델은 세션을 읽으므로, 세션을 받은 뒤에 생성자에서 만든다.
    private final ItemTableModel tableModel;
    private final CheckableImportTable table;

    /**
     * @param onSelectionChanged 가져올 건의 선택이 바뀌었을 때 실행할 동작
     */
    ReviewStepPanel(BudgetImportSession session, Runnable onSelectionChanged) {
        this.session = session;
        this.onSelectionChanged = onSelectionChanged;
        this.tableModel = new ItemTableModel();
        this.table = new CheckableImportTable(tableModel);

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        // 전체 체크와 해제는 표 머리글의 체크박스로 한다. 예산에는 중복 의심이 없으므로 "기본 선택으로"는 오류를 뺀
        // 모든 건을 고르며, 머리글의 전체 체크와 결과가 같다. 수입/지출 가져오기와 화면을 맞추기 위해 둔다.
        SecondaryButton defaultSelectionButton = new SecondaryButton("기본 선택으로", ComponentSize.SMALL);
        defaultSelectionButton.setToolTipText("오류인 건을 뺀 모든 건을 선택합니다.");
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(defaultSelectionButton);

        JPanel summaryPanel = new JPanel(new BorderLayout(0, 4));
        summaryPanel.setOpaque(false);
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        summaryPanel.add(buttonPanel, BorderLayout.EAST);
        summaryPanel.add(countLabel, BorderLayout.SOUTH);
        add(summaryPanel, BorderLayout.NORTH);

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        ItemCellRenderer renderer = new ItemCellRenderer();
        TableColumnModel columns = table.getColumnModel();
        for (int column = COLUMN_ROW_NUMBER; column < COLUMN_NAMES.length; column++) {
            columns.getColumn(column).setCellRenderer(renderer);
        }
        // 창이 커지면 예산과 설명 칸만 넓어진다. 나머지 칸도 사용자가 넓힐 수 있다.
        table.setCheckColumnWidth(40);
        table.setNonGrowingColumnWidth(COLUMN_ROW_NUMBER, 40);
        table.setNonGrowingColumnWidth(COLUMN_TYPE, 45);
        table.setGrowingColumnWidth(COLUMN_NAME, 170);
        table.setNonGrowingColumnWidth(COLUMN_AMOUNT, 95);
        table.setNonGrowingColumnWidth(COLUMN_STATE, 70);
        table.setGrowingColumnWidth(COLUMN_MESSAGE, 190);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        defaultSelectionButton.addActionListener(_ -> resetSelection());
    }

    /**
     * 세션의 분석 결과와 선택을 화면에 반영한다.
     */
    void refresh() {
        tableModel.fireTableDataChanged();
        updateSummary();
    }

    private void resetSelection() {
        if (session.getAnalysis() == null) {
            return;
        }
        session.resetSelection();
        onChecksChanged();
    }

    /**
     * 가져올 건의 선택이 바뀐 것을 화면에 반영한다. 상위/하위 예산의 선택도 함께 바뀌므로 모든 행을 다시 그리되, 표의 행 선택이 풀리지
     * 않도록 표 전체를 다시 읽게 하지는 않는다.
     */
    private void onChecksChanged() {
        if (tableModel.getRowCount() > 0) {
            tableModel.fireTableRowsUpdated(0, tableModel.getRowCount() - 1);
        }
        updateSummary();
        onSelectionChanged.run();
    }

    private void updateSummary() {
        if (session.getAnalysis() == null) {
            summaryLabel.setText(" ");
            countLabel.setText(" ");
            return;
        }

        SelectionSummary summary = session.summarize();
        summaryLabel.setText(String.format("선택: 추가 %,d건, 금액 갱신 %,d건 / 금액 합계 %,d원", summary.createCount(),
                summary.updateCount(), summary.totalAmount()));

        int errorCount = 0;
        int warningCount = 0;
        int skippedCount = 0;
        for (BudgetImportItemDto item : items()) {
            switch (item.getStatus()) {
            case ERROR -> errorCount++;
            case WARNING -> warningCount++;
            case SKIPPED -> skippedCount++;
            default -> {
                // 정상인 건은 따로 세지 않는다.
            }
            }
        }
        countLabel.setText(String.format("전체 %,d건 (오류 %,d건, 경고 %,d건, 건너뜀 %,d건) / 예산명이 없어 읽지 않은 행 %,d개",
                items().size(), errorCount, warningCount, skippedCount, session.getAnalysis().getBlankRowCount()));
    }

    private List<BudgetImportItemDto> items() {
        return session.getAnalysis() == null ? List.of() : session.getAnalysis().getItems();
    }

    private final class ItemTableModel extends AbstractTableModel implements CheckableTableModel {
        private static final long serialVersionUID = 1L;

        @Override
        public int getRowCount() {
            return items().size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMN_NAMES[column];
        }

        @Override
        public boolean isRowChecked(int row) {
            return session.isSelected(items().get(row).getKey());
        }

        @Override
        public boolean isRowCheckable(int row) {
            return items().get(row).isSelectable();
        }

        @Override
        public void setRowChecked(int row, boolean checked) {
            session.setSelected(items().get(row).getKey(), checked);
            onChecksChanged();
        }

        @Override
        public void setAllChecked(boolean checked) {
            session.selectAll(checked);
            onChecksChanged();
        }

        @Override
        public Object getValueAt(int row, int column) {
            BudgetImportItemDto item = items().get(row);
            return switch (column) {
            case COLUMN_SELECTED -> isRowChecked(row);
            case COLUMN_ROW_NUMBER -> item.getRowNumber() == null ? "-" : item.getRowNumber();
            case COLUMN_TYPE -> item.getType() == null ? "" : item.getType().getLabel();
            // 계층이 한눈에 보이도록 하위 예산도 마지막 이름이 아니라 전체 경로로 적는다.
            case COLUMN_NAME -> String.join(" > ", item.getLevelNames());
            case COLUMN_AMOUNT -> item.getAmount() == null ? "" : String.format("%,d", item.getAmount());
            case COLUMN_STATE -> item.getAction() == BudgetImportAction.NONE ? item.getStatus().getLabel()
                    : item.getAction().getLabel();
            default -> item.getMessage();
            };
        }
    }

    /**
     * 상태에 따라 글자색을 달리한다. 칸에 넘치는 내용은 툴팁으로 볼 수 있다.
     */
    private final class ItemCellRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            BudgetImportItemDto item = items().get(row);

            setBorder(BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
            setHorizontalAlignment(switch (column) {
            case COLUMN_AMOUNT -> SwingConstants.RIGHT;
            case COLUMN_ROW_NUMBER, COLUMN_TYPE, COLUMN_STATE -> SwingConstants.CENTER;
            default -> SwingConstants.LEFT;
            });
            if (!isSelected) {
                setForeground(colorOf(item, column));
            }
            setToolTipText(value == null || value.toString().isEmpty() ? null : value.toString());
            return this;
        }

        private Color colorOf(BudgetImportItemDto item, int column) {
            if (item.getStatus() == ImportItemStatus.ERROR) {
                return UIColors.EXPENSE;
            }
            if (item.getStatus() == ImportItemStatus.SKIPPED) {
                return UIColors.TEXT_SUB;
            }
            if (item.getStatus() == ImportItemStatus.WARNING
                    && (column == COLUMN_STATE || column == COLUMN_MESSAGE)) {
                return UIColors.ERROR_WARNING;
            }
            return UIColors.TEXT_MAIN;
        }
    }

}
