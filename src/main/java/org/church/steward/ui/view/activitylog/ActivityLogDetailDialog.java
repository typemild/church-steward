package org.church.steward.ui.view.activitylog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.BadgeLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

class ActivityLogDetailDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 건별 내용이 있는 기록(가져오기)을 보여줄 때의 다이얼로그 크기 */
    private static final int ITEMS_DIALOG_WIDTH = 700;
    private static final int ITEMS_DIALOG_HEIGHT = 580;
    /** 요약 표의 높이에 더하는 여유 (머리글과 테두리) */
    private static final int SUMMARY_TABLE_EXTRA_HEIGHT = 8;

    public ActivityLogDetailDialog(JComponent parent, ActivityLogResponseDto dto) {
        super(SwingUtilities.getWindowAncestor(parent), "활동 기록 상세 정보", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        add(mainPanel);

        // 상단 정보 영역
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        addInfoRow(infoPanel, 0, FontAwesomeSolid.LIST_OL, "기록 순번", String.valueOf(dto.getId()));
        addInfoRow(infoPanel, 1, FontAwesomeSolid.USER, "사용자 ID", dto.getUserId());
        addInfoRow(infoPanel, 2, FontAwesomeSolid.INFO_CIRCLE, "활동 유형",
                dto.getActivityType() != null ? dto.getActivityType().getLabel() : "");
        addInfoRow(infoPanel, 3, FontAwesomeSolid.TAG, "정보 유형", dto.getTargetName());
        addInfoRow(infoPanel, 4, FontAwesomeSolid.CLOCK, "활동 일시",
                dto.getCreatedAt() != null
                        ? dto.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        : "");

        mainPanel.add(infoPanel, BorderLayout.NORTH);

        // 중앙 상세 내용 영역
        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        contentPanel.add(new FieldLabel("상세 내용", ComponentSize.MEDIUM), BorderLayout.NORTH);

        JTable fieldTable = createFieldTable(dto.getFields());
        JScrollPane scrollPane = new JScrollPane(fieldTable);

        if (dto.getItems().isEmpty()) {
            contentPanel.add(scrollPane, BorderLayout.CENTER);
        } else {
            // 가져오기처럼 여러 건을 한 번에 처리한 기록은 요약 아래에 건별 내용을 함께 보여준다.
            setSize(ITEMS_DIALOG_WIDTH, ITEMS_DIALOG_HEIGHT);
            setLocationRelativeTo(parent);

            // 요약은 항목이 몇 개뿐이므로 그 높이만큼만 차지하게 하고, 남는 공간을 건별 내용에 준다.
            scrollPane.setPreferredSize(new Dimension(0,
                    fieldTable.getRowHeight() * (dto.getFields().size() + 1) + SUMMARY_TABLE_EXTRA_HEIGHT));

            JScrollPane itemScrollPane = new JScrollPane(createItemTable(dto.getItems()));

            JPanel itemPanel = new JPanel(new BorderLayout(5, 5));
            itemPanel.add(new FieldLabel(String.format("건별 내용 (%,d건)", dto.getItems().size()), ComponentSize.MEDIUM),
                    BorderLayout.NORTH);
            itemPanel.add(itemScrollPane, BorderLayout.CENTER);

            JPanel tablesPanel = new JPanel(new BorderLayout(10, 10));
            tablesPanel.add(scrollPane, BorderLayout.NORTH);
            tablesPanel.add(itemPanel, BorderLayout.CENTER);
            contentPanel.add(tablesPanel, BorderLayout.CENTER);
        }

        mainPanel.add(contentPanel, BorderLayout.CENTER);

        DialogKeyHandler.install(this);
    }

    /**
     * 건별 내용을 한 건에 한 줄씩 표로 만든다. 한 줄에 다 보이지 않는 내용은 툴팁으로 볼 수 있다.
     */
    private JTable createItemTable(List<List<ActivityLogFieldDto>> items) {
        ReadOnlyTableModel tableModel = new ReadOnlyTableModel(new String[] { "번호", "내용" });
        for (int i = 0; i < items.size(); i++) {
            tableModel.addRow(new Object[] { i + 1, ActivityLogFieldFormatter.toSummary(items.get(i)) });
        }

        JTable table = createTable(tableModel);

        PaddedCellRenderer renderer = new PaddedCellRenderer(SwingConstants.LEADING);
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(0).setMaxWidth(60);

        return table;
    }

    /**
     * 기록된 항목을 표로 만든다.
     * 값이 변경된 항목이 있으면 "변경 전"/"변경 후"를 나누어 보여주고, 변경된 항목의 변경 후 값을 강조한다.
     */
    private JTable createFieldTable(List<ActivityLogFieldDto> fields) {
        boolean hasChange = fields.stream()
                .anyMatch(ActivityLogFieldDto::isChanged);
        String[] columnNames = hasChange
                ? new String[] { "항목", "변경 전", "변경 후" }
                : new String[] { "항목", "내용" };

        ReadOnlyTableModel tableModel = new ReadOnlyTableModel(columnNames);
        for (ActivityLogFieldDto field : fields) {
            String value = ActivityLogFieldFormatter.toDisplayValue(field.getValue());
            if (hasChange) {
                // 변경되지 않은 항목(식별자)은 변경 전/후에 같은 값을 보여준다.
                String oldValue = field.isChanged()
                        ? ActivityLogFieldFormatter.toDisplayValue(field.getOldValue())
                        : value;
                tableModel.addRow(new Object[] { field.getName(), oldValue, value });
            } else {
                tableModel.addRow(new Object[] { field.getName(), value });
            }
        }

        JTable table = createTable(tableModel);

        int newValueColumn = 2;
        PaddedCellRenderer renderer = new PaddedCellRenderer(SwingConstants.LEADING) {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                boolean highlighted = column == newValueColumn && fields.get(row).isChanged();
                if (highlighted) {
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                if (!isSelected) {
                    setForeground(highlighted ? UIColors.TEXT_HIGHLIGHT : table.getForeground());
                }
                return this;
            }
        };
        for (int column = 0; column < table.getColumnCount(); column++) {
            table.getColumnModel().getColumn(column).setCellRenderer(renderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(110);
        table.getColumnModel().getColumn(0).setMaxWidth(160);

        return table;
    }

    private void addInfoRow(JPanel panel, int row, FontAwesomeSolid icon, String label, String value) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        panel.add(new BadgeLabel(icon, label, ComponentSize.SMALL), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new FieldLabel(value, ComponentSize.MEDIUM), gbc);
    }

    private JTable createTable(ReadOnlyTableModel tableModel) {
        ListTable table = new ListTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return table;
    }

    /**
     * 수정할 수 없는 표 모델. 값이 길어 잘리는 경우를 위해 셀의 전체 값을 툴팁으로 보여준다.
     */
    private static class ReadOnlyTableModel extends DefaultTableModel implements TooltipTableModel {

        private static final long serialVersionUID = 1L;

        ReadOnlyTableModel(String[] columnNames) {
            super(columnNames, 0);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        @Override
        public String getTooltipText(int row, int column) {
            Object value = getValueAt(row, column);
            return value != null ? value.toString() : null;
        }

    }
}
