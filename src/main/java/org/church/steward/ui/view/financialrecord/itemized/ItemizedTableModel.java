package org.church.steward.ui.view.financialrecord.itemized;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.ui.component.table.TooltipTableModel;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;
import org.church.steward.util.NumberUtils;

/**
 * "건별 보기" 탭의 테이블 모델
 */
public class ItemizedTableModel extends AbstractTableModel implements TooltipTableModel {

    private static final long serialVersionUID = 1L;

    public static final int COLUMN_DATE = 0;
    public static final int COLUMN_TYPE = 1;
    public static final int COLUMN_BUDGET = 2;
    public static final int COLUMN_CONTACT = 3;
    public static final int COLUMN_DESCRIPTION = 4;
    public static final int COLUMN_AMOUNT = 5;
    public static final int COLUMN_REMARKS = 6;

    private static final String[] COLUMNS = { "날짜", "구분", "예산", "봉헌자/수취인", "내용", "금액", "비고" };
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private List<FinancialRecordResponseDto> data = new ArrayList<>();

    /**
     * 표시할 수입/지출 내역을 설정한다. 테이블의 정렬기는 정렬 기준 값이 같은 행을 모델 순서대로 두므로, 그때 나중에 입력한 것이 위에
     * 오도록 ID 내림차순으로 담아 둔다.
     */
    public void setData(List<FinancialRecordResponseDto> newData) {
        List<FinancialRecordResponseDto> sorted = new ArrayList<>(newData);
        sorted.sort(Comparator.comparing(FinancialRecordResponseDto::getId, Comparator.reverseOrder()));
        this.data = sorted;
        fireTableDataChanged();
    }

    public FinancialRecordResponseDto getRecordAt(int row) {
        return data.get(row);
    }

    /**
     * ID가 같은 내역의 행을 반환한다. 없으면 -1을 반환한다.
     */
    public int indexOfRecord(Long recordId) {
        for (int i = 0; i < data.size(); i++) {
            if (data.get(i)
                    .getId()
                    .equals(recordId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int col) {
        return COLUMNS[col];
    }

    /**
     * 정렬 시 날짜는 {@link RecordDate}로, 금액은 숫자로, 나머지는 문자열로 비교하도록 컬럼의 타입을 알려준다.
     */
    @Override
    public Class<?> getColumnClass(int col) {
        return switch (col) {
        case COLUMN_DATE -> RecordDate.class;
        case COLUMN_AMOUNT -> Long.class;
        default -> String.class;
        };
    }

    @Override
    public Object getValueAt(int row, int col) {
        FinancialRecordResponseDto record = data.get(row);
        return switch (col) {
        case COLUMN_DATE -> new RecordDate(record.getDate(), record.getId());
        case COLUMN_AMOUNT -> record.getAmount();
        default -> getText(record, col);
        };
    }

    /**
     * 셀의 전체 텍스트를 툴팁으로 반환한다. 금액은 "53,000원(5만 3,000원)" 형식으로 표기한다.
     */
    @Override
    public String getTooltipText(int row, int col) {
        if (row < 0 || row >= getRowCount() || col < 0 || col >= getColumnCount()) {
            return null;
        }

        FinancialRecordResponseDto record = data.get(row);
        String text = switch (col) {
        case COLUMN_AMOUNT -> NumberUtils.toKoreanAmount(record.getAmount());
        default -> getText(record, col);
        };
        return (text == null || text.isEmpty()) ? null : text;
    }

    private String getText(FinancialRecordResponseDto record, int col) {
        return switch (col) {
        case COLUMN_DATE -> record.getDate()
                .format(DATE_FORMAT);
        case COLUMN_TYPE -> record.getType()
                .getLabel();
        case COLUMN_BUDGET -> record.getBudgetName();
        case COLUMN_CONTACT -> FinancialRecordFormats.joinNames(record.getContactNames());
        case COLUMN_DESCRIPTION -> record.getDescription();
        case COLUMN_REMARKS -> record.getRemarks();
        default -> null;
        };
    }

    /**
     * 날짜 칸의 값. 화면에는 날짜만 보이고, 정렬할 때는 같은 날짜의 내역을 ID로 가른다. 정렬기는 내림차순이면 비교 결과를 뒤집으므로,
     * 오름차순은 "날짜 오름차순, ID 오름차순", 내림차순은 "날짜 내림차순, ID 내림차순"이 된다.
     */
    public record RecordDate(LocalDate date, Long id) implements Comparable<RecordDate> {

        private static final Comparator<RecordDate> ORDER = Comparator.comparing(RecordDate::date)
                .thenComparing(RecordDate::id);

        @Override
        public int compareTo(RecordDate other) {
            return ORDER.compare(this, other);
        }

        /**
         * 셀 렌더러가 표시하는 글자다.
         */
        @Override
        public String toString() {
            return date.format(DATE_FORMAT);
        }
    }
}
