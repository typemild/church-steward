package org.church.steward.ui.view.datamanagement.contactimport;

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

import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.datamanagement.contactimport.ContactImportSession.SelectionSummary;
import org.church.steward.ui.view.datamanagement.importing.CheckableImportTable;
import org.church.steward.ui.view.datamanagement.importing.CheckableTableModel;

/**
 * 관계자 가져오기의 3단계. 행마다 상태와 사유를 보여주고 가져올 행을 체크박스로 고르게 한다.
 * 오류인 행은 고를 수 없고, 중복 의심인 행은 처음에 선택에서 빠져 있다. "기본 선택으로"는 이 처음 선택으로 되돌린다.
 */
class ContactReviewStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int COLUMN_SELECTED = 0;
    private static final int COLUMN_ROW_NUMBER = 1;
    private static final int COLUMN_TYPE = 2;
    private static final int COLUMN_NAME = 3;
    private static final int COLUMN_REMARKS = 4;
    private static final int COLUMN_HIDDEN = 5;
    private static final int COLUMN_STATUS = 6;
    private static final int COLUMN_MESSAGE = 7;
    private static final String[] COLUMN_NAMES = { "선택", "행", "구분", "이름", "비고", "숨김", "상태", "설명" };

    private final ContactImportSession session;
    private final Runnable onSelectionChanged;

    private final FieldLabel summaryLabel = new FieldLabel(" ", ComponentSize.MEDIUM, true);
    private final SubFieldLabel countLabel = new SubFieldLabel(" ", ComponentSize.MEDIUM);
    // 표는 만들어질 때 모델을 읽고 모델은 세션을 읽으므로, 세션을 받은 뒤에 생성자에서 만든다.
    private final ItemTableModel tableModel;
    private final CheckableImportTable table;

    /**
     * @param onSelectionChanged 가져올 행의 선택이 바뀌었을 때 실행할 동작
     */
    ContactReviewStepPanel(ContactImportSession session, Runnable onSelectionChanged) {
        this.session = session;
        this.onSelectionChanged = onSelectionChanged;
        this.tableModel = new ItemTableModel();
        this.table = new CheckableImportTable(tableModel);

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        // 전체 체크와 해제는 표 머리글의 체크박스로 한다.
        SecondaryButton defaultSelectionButton = new SecondaryButton("기본 선택으로", ComponentSize.SMALL);
        defaultSelectionButton.setToolTipText("오류 행과 중복 의심 행을 뺀 모든 행을 선택합니다.");
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
        // 체크박스, 행, 구분, 숨김, 상태 칸은 내용의 길이가 일정하므로 창이 커져도 넓히지 않고, 나머지 칸이 늘어난 폭을 나눠 쓴다.
        table.setCheckColumnWidth(36);
        table.setNonGrowingColumnWidth(COLUMN_ROW_NUMBER, 38);
        table.setNonGrowingColumnWidth(COLUMN_TYPE, 60);
        table.setGrowingColumnWidth(COLUMN_NAME, 100);
        table.setGrowingColumnWidth(COLUMN_REMARKS, 120);
        table.setNonGrowingColumnWidth(COLUMN_HIDDEN, 42);
        table.setNonGrowingColumnWidth(COLUMN_STATUS, 42);
        table.setGrowingColumnWidth(COLUMN_MESSAGE, 160);

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
        if (!session.hasAnalysis()) {
            return;
        }
        session.resetSelection();
        onChecksChanged();
    }

    /**
     * 가져올 행의 선택이 바뀐 것을 화면에 반영한다. 표의 행 선택이 풀리지 않도록 표 전체를 다시 읽게 하지 않고 행만
     * 다시 그린다.
     */
    private void onChecksChanged() {
        if (tableModel.getRowCount() > 0) {
            tableModel.fireTableRowsUpdated(0, tableModel.getRowCount() - 1);
        }
        updateSummary();
        onSelectionChanged.run();
    }

    private void updateSummary() {
        if (!session.hasAnalysis()) {
            summaryLabel.setText(" ");
            countLabel.setText(" ");
            return;
        }

        SelectionSummary summary = session.summarize();
        summaryLabel.setText(String.format("선택: 관계자 %,d명 (숨김 %,d명)", summary.count(), summary.hiddenCount()));

        int errorCount = 0;
        int duplicateCount = 0;
        for (ContactImportItemDto item : items()) {
            if (item.getStatus() == ImportItemStatus.ERROR) {
                errorCount++;
            } else if (item.isDuplicate()) {
                duplicateCount++;
            }
        }
        countLabel.setText(String.format("전체 %,d건 (오류 %,d건, 중복 의심 %,d건) / 이름이 없어 읽지 않은 행 %,d개", items().size(),
                errorCount, duplicateCount, session.getAnalysis().getBlankRowCount()));
    }

    private List<ContactImportItemDto> items() {
        return session.hasAnalysis() ? session.getAnalysis().getItems() : List.of();
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
            ContactImportItemDto item = items().get(row);
            return switch (column) {
            case COLUMN_SELECTED -> isRowChecked(row);
            case COLUMN_ROW_NUMBER -> item.getRowNumber();
            case COLUMN_TYPE -> item.getType() == null ? "" : item.getType().getLabel();
            case COLUMN_NAME -> item.getName();
            case COLUMN_REMARKS -> item.getRemarks() == null ? "" : item.getRemarks();
            case COLUMN_HIDDEN -> item.isHidden() ? "숨김" : "";
            case COLUMN_STATUS -> item.getStatus().getLabel();
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
            ContactImportItemDto item = items().get(row);

            setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
            setHorizontalAlignment(switch (column) {
            case COLUMN_ROW_NUMBER, COLUMN_TYPE, COLUMN_HIDDEN, COLUMN_STATUS -> SwingConstants.CENTER;
            default -> SwingConstants.LEFT;
            });
            if (!isSelected) {
                setForeground(colorOf(item, column));
            }
            setToolTipText(value == null || value.toString().isEmpty() ? null : value.toString());
            return this;
        }

        private Color colorOf(ContactImportItemDto item, int column) {
            if (item.getStatus() == ImportItemStatus.ERROR) {
                return UIColors.EXPENSE;
            }
            if (item.getStatus() == ImportItemStatus.WARNING
                    && (column == COLUMN_STATUS || column == COLUMN_MESSAGE)) {
                return UIColors.ERROR_WARNING;
            }
            return UIColors.TEXT_MAIN;
        }
    }

}
