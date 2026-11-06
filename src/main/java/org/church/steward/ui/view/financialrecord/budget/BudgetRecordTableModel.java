package org.church.steward.ui.view.financialrecord.budget;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.domain.report.entity.RecordOrder;
import org.church.steward.domain.report.entity.ReportColumn;
import org.church.steward.ui.component.table.AbstractTreeTableModel;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.ui.component.table.TreeTableNode;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;
import org.church.steward.util.NumberUtils;

/**
 * "예산별 보기" 탭의 테이블 모델. 수입/지출 내역을 예산별로 묶어, 예산 행 아래에 그 예산의 내역 행들을 두는 형태로 보여준다. 예산
 * 행은 기본적으로 접혀 있다. 예산 행의 키는 예산 ID이고, 내역 행의 키는 {@link RecordKey}다.
 * <p>
 * 모델은 칸 설정과 무관하게 모든 칸을 갖는다. 첫 번째 칸(트리 칸)에는 예산 행이면 예산명, 내역 행이면 날짜와 첫 번째로 정한 칸의
 * 값을 적고, 칸의 순서와 숨김은 표의 칸 모델이 정한다({@code BudgetRecordPanel#setColumns}). 칸을 바꿀
 * 때마다 모델의 구조를 바꾸면 표가 칸을 새로 만들어 트리 칸의 렌더러와 칸 폭이 사라지기 때문이다.
 */
public class BudgetRecordTableModel extends AbstractTreeTableModel<BudgetRecordRow> implements TooltipTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COLUMN_NAME = 0;
    public static final int COLUMN_CONTACT = 1;
    public static final int COLUMN_DESCRIPTION = 2;
    public static final int COLUMN_AMOUNT = 3;
    public static final int COLUMN_REMARKS = 4;

    private final String[] columns = { "구분", "관계자", "내용", "금액", "비고" };

    /** 트리 칸에 날짜와 함께 적는 칸. 관계자 또는 내용이다. */
    private ReportColumn firstColumn = ReportColumn.CONTACT;
    /** 같은 날짜의 내역끼리의 순서 */
    private RecordOrder recordOrder = ProgramSettingConstraints.DEFAULT_RECORD_ORDER;
    /** 모든 내역의 관계자 칸 글자. 접힌 예산의 내역도 포함한다. */
    private List<String> contactTexts = List.of();

    public BudgetRecordTableModel() {
        super(false);
    }

    /**
     * 칸 설정의 칸에 해당하는 모델 기준 칸 번호
     */
    public static int columnIndexOf(ReportColumn column) {
        return switch (column) {
        case CONTACT -> COLUMN_CONTACT;
        case DESCRIPTION -> COLUMN_DESCRIPTION;
        case AMOUNT -> COLUMN_AMOUNT;
        case REMARKS -> COLUMN_REMARKS;
        default -> throw new IllegalArgumentException("예산별 보기에 없는 칸입니다: " + column);
        };
    }

    /**
     * 트리 칸의 내역 행에 날짜와 함께 적을 칸을 정한다.
     *
     * @param firstColumn 관계자({@link ReportColumn#CONTACT}) 또는
     *                    내용({@link ReportColumn#DESCRIPTION})
     */
    public void setFirstColumn(ReportColumn firstColumn) {
        if (this.firstColumn == firstColumn) {
            return;
        }
        this.firstColumn = firstColumn;
        // 선택이 풀리지 않도록 구조나 전체 데이터가 아니라 행이 바뀌었다고 알린다.
        if (getRowCount() > 0) {
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

    /**
     * 같은 날짜의 내역끼리의 순서를 정한다. 다음에 내역을 넣을 때({@link #setRecords}) 적용된다.
     */
    public void setRecordOrder(RecordOrder recordOrder) {
        this.recordOrder = recordOrder;
    }

    /**
     * 표시할 수입/지출 내역을 설정한다. 예산은 예산 설정 화면과 같은 순서로, 예산 안의 내역은 날짜 오름차순으로 정렬한다. 같은 날짜에서는
     * 정한 순서({@link #setRecordOrder})를 따르며, 가나다순은 첫 번째 칸의 값으로 견준다. 그래도 같으면 새로 입력한
     * 내역이 아래에 붙도록 ID 오름차순이다.
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
                .thenComparing(recordOrder.sameDateComparator(this::firstValueOf))
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
        contactTexts = records.stream()
                .map(record -> FinancialRecordFormats.joinNames(record.getContactNames()))
                .toList();
        setRoots(budgetNodes);
    }

    /**
     * 모든 내역의 관계자 칸 글자를 반환한다. 접힌 예산의 내역도 포함하며, 내역을 넣을 때마다({@link #setRecords}) 새 목록이
     * 된다.
     */
    public List<String> getContactTexts() {
        return contactTexts;
    }

    /**
     * 내역의 첫 번째 칸에 적는 값. 트리 칸에서 일자 뒤에 적는 값과 같다.
     */
    private String firstValueOf(FinancialRecordResponseDto record) {
        return firstColumn == ReportColumn.DESCRIPTION ? record.getDescription()
                : FinancialRecordFormats.joinNames(record.getContactNames());
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
        case COLUMN_NAME -> row.getDisplayName(firstColumn);
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
        case COLUMN_NAME -> row.getDisplayName(firstColumn);
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
        case COLUMN_CONTACT -> FinancialRecordFormats.joinNames(row.getRecord()
                .getContactNames());
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
