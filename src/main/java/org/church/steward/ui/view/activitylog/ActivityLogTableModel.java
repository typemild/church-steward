package org.church.steward.ui.view.activitylog;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;

import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.ui.component.table.TooltipTableModel;

class ActivityLogTableModel extends AbstractTableModel implements TooltipTableModel {

    private static final long serialVersionUID = 1L;
    private final String[] columnNames = { "일시", "활동 유형", "정보 유형", "내용", "사용자 ID", "" };
    private List<ActivityLogResponseDto> data = new ArrayList<>();

    public void setData(List<ActivityLogResponseDto> newData) {
        this.data = newData;
        fireTableDataChanged();
    }

    public ActivityLogResponseDto getDtoAt(int row) {
        return data.get(row);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 5) {
            return JButton.class;
        }
        return Object.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ActivityLogResponseDto dto = data.get(rowIndex);
        switch (columnIndex) {
        case 0: // 일시
            return dto.getCreatedAt() != null
                    ? dto.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "";
        case 1: // 활동 유형
            return dto.getActivityType();
        case 2: // 정보 유형
            return dto.getTargetName();
        case 3: // 내용
            return ActivityLogFieldFormatter.toSummary(dto);
        case 4: // 사용자 ID
            return dto.getUserId();
        case 5: // 상세 보기
            return null;
        default:
            return null;
        }
    }

    @Override
    public String getTooltipText(int rowIndex, int columnIndex) {
        if (rowIndex < 0 || rowIndex >= getRowCount() || columnIndex < 0 || columnIndex >= getColumnCount()) {
            return null;
        }
        if (columnIndex == 5) { // 상세 보기 컬럼
            return "클릭하시면 세부 정보가 표시됩니다.";
        }

        ActivityLogResponseDto dto = data.get(rowIndex);
        return switch (columnIndex) {
        case 0 -> // 일시
            dto.getCreatedAt() != null
                    ? dto.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    : "";
        case 1 -> dto.getActivityType() != null ? dto.getActivityType().getLabel() : "";
        case 2 -> dto.getTargetName();
        case 3 -> ActivityLogFieldFormatter.toSummary(dto);
        case 4 -> dto.getUserId();
        default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

}
