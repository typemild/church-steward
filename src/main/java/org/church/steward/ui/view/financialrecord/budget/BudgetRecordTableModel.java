package org.church.steward.ui.view.financialrecord.budget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.ui.component.table.AbstractTreeTableModel;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.ui.component.table.TreeTableNode;
import org.church.steward.util.NumberUtils;

/**
 * "예산별 보기" 탭의 테이블 모델. 수입/지출 내역을 예산별로 묶어, 예산 행 아래에 그 예산의 내역 행들을 두는 형태로 보여준다. 예산
 * 행은 기본적으로 접혀 있다. 예산 행의 키는 예산 ID이고, 내역 행의 키는 {@link RecordKey}다.
 */
public class BudgetRecordTableModel extends AbstractTreeTableModel<BudgetRecordRow> implements TooltipTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_DESCRIPTION = 1;
    public static final int COLUMN_AMOUNT = 2;
    public static final int COLUMN_REMARKS = 3;

    private final String[] columns = { "구분", "내용", "금액", "비고" };

    public BudgetRecordTableModel() {
        super(false);
    }

    /**
     * 표시할 수입/지출 내역을 설정한다. 예산은 예산 설정 화면과 같은 순서로, 예산 안의 내역은 새로 입력한 내역이 아래에 붙도록 날짜
     * 오름차순, 같은 날짜에서는 ID 오름차순으로 정렬한다.
     *
     * @param budgetPositions 예산 ID별 표시 위치({@link BudgetDisplayOrder#positionsOf}).
     *                        여기에 없는 예산은 맨 뒤에 둔다.
     */
    public void setRecords(List<FinancialRecordResponseDto> records, Map<Long, Integer> budgetPositions) {
        List<FinancialRecordResponseDto> sorted = new ArrayList<>(records);
        sorted.sort(Comparator
                .<FinancialRecordResponseDto>comparingInt(
                        record -> budgetPositions.getOrDefault(record.getBudgetId(), Integer.MAX_VALUE))
                .thenComparing(FinancialRecordResponseDto::getDate)
                .thenComparing(FinancialRecordResponseDto::getId));

        // 예산 ID별 수입/지출 내역. 예산의 표시 순서대로 정렬되어 있다.
        Map<Long, List<FinancialRecordResponseDto>> recordsByBudget = new LinkedHashMap<>();
        for (FinancialRecordResponseDto record : sorted) {
            recordsByBudget.computeIfAbsent(record.getBudgetId(), _ -> new ArrayList<>())
                    .add(record);
        }

        List<TreeTableNode<BudgetRecordRow>> budgetNodes = new ArrayList<>();
        for (Map.Entry<Long, List<FinancialRecordResponseDto>> entry : recordsByBudget.entrySet()) {
            Long budgetId = entry.getKey();
            List<FinancialRecordResponseDto> budgetRecords = entry.getValue();

            long total = budgetRecords.stream()
                    .mapToLong(FinancialRecordResponseDto::getAmount)
                    .sum();
            List<TreeTableNode<BudgetRecordRow>> recordNodes = budgetRecords.stream()
                    .map(record -> TreeTableNode.leaf(new RecordKey(record.getId()), BudgetRecordRow.ofRecord(record)))
                    .toList();
            budgetNodes.add(new TreeTableNode<>(budgetId,
                    BudgetRecordRow.ofBudget(budgetId, budgetNameOf(budgetRecords.getFirst()), total), recordNodes));
        }
        setRoots(budgetNodes);
    }

    private static String budgetNameOf(FinancialRecordResponseDto record) {
        return record.getBudgetName() != null ? record.getBudgetName() : "";
    }

    /**
     * 내역이 보이도록 그 예산 행을 펼치고, 내역 행을 반환한다. 표에 없는 내역이면 -1을 반환한다.
     */
    public int revealRecord(Long recordId) {
        return reveal(new RecordKey(recordId));
    }

    /**
     * 지정한 행들의 금액 합계를 반환한다. 예산 행과 그 하위 내역 행이 함께 포함되어 있으면 중복되지 않도록 예산 행의 금액만 더한다.
     */
    public long sumAmounts(int[] rowIndexes) {
        Set<Long> selectedBudgetIds = new HashSet<>();
        for (int index : rowIndexes) {
            BudgetRecordRow row = getRowValue(index);
            if (row.isBudgetRow()) {
                selectedBudgetIds.add(row.getBudgetId());
            }
        }

        long sum = 0;
        for (int index : rowIndexes) {
            BudgetRecordRow row = getRowValue(index);
            if (row.isBudgetRow() || !selectedBudgetIds.contains(row.getBudgetId())) {
                sum += row.getAmount();
            }
        }
        return sum;
    }

    @Override
    public int getColumnCount() {
        return columns.length;
    }

    @Override
    public String getColumnName(int col) {
        return columns[col];
    }

    @Override
    public Object getValueAt(int rowIndex, int col) {
        BudgetRecordRow row = getRowValue(rowIndex);
        return switch (col) {
        case COLUMN_NAME -> row.getDisplayName();
        case COLUMN_AMOUNT -> row.getAmount();
        default -> getText(row, col);
        };
    }

    /**
     * 셀의 전체 텍스트를 툴팁으로 반환한다. 금액은 "53,000원(5만 3,000원)" 형식으로 표기한다.
     */
    @Override
    public String getTooltipText(int rowIndex, int col) {
        if (rowIndex < 0 || rowIndex >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return null;
        }

        BudgetRecordRow row = getRowValue(rowIndex);
        String text = switch (col) {
        case COLUMN_NAME -> row.getDisplayName();
        case COLUMN_AMOUNT -> NumberUtils.toKoreanAmount(row.getAmount());
        default -> getText(row, col);
        };
        return (text == null || text.isEmpty()) ? null : text;
    }

    private String getText(BudgetRecordRow row, int col) {
        if (row.isBudgetRow()) {
            return null;
        }
        return switch (col) {
        case COLUMN_DESCRIPTION -> row.getRecord()
                .getDescription();
        case COLUMN_REMARKS -> row.getRecord()
                .getRemarks();
        default -> null;
        };
    }

    /**
     * 내역 행의 키. 예산 행의 키(예산 ID)와 섞이지 않도록 타입을 나눈다.
     */
    private record RecordKey(Long recordId) {
    }
}
