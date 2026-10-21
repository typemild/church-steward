package org.church.steward.ui.view.datamanagement.importing;

import javax.swing.table.TableModel;

/**
 * {@link CheckableImportTable}의 모델. 첫
 * 칸({@link CheckableImportTable#CHECK_COLUMN})의 체크 상태를 모델이 가진다.
 * <p>
 * 체크 상태는 표가 아니라 모델(가져오기 세션)이 항목의 식별자로 보관하므로, 다시 분석해 행이 바뀌어도 유지된다.
 * 체크를 바꾸는 메서드는 바뀐 행에 대해 {@code fireTableRowsUpdated}를 호출해야 한다. 표 전체를 다시 읽게 하면
 * ({@code fireTableDataChanged}) 행 선택이 풀린다.
 */
public interface CheckableTableModel extends TableModel {

    boolean isRowChecked(int modelRow);

    /**
     * @return 체크를 바꿀 수 있는 행인지 여부. false인 행(오류 행 등)은 꺼진 체크박스로 그리고, 눌러도 바뀌지 않는다.
     */
    boolean isRowCheckable(int modelRow);

    void setRowChecked(int modelRow, boolean checked);

    /**
     * 체크할 수 있는 행을 모두 체크하거나 모두 해제한다. 머리글의 체크박스를 누르면 호출된다. 행마다
     * {@link #setRowChecked}를 부르지 않고 한 번에 바꾸게 하는 것은, 상위/하위 예산처럼 행 사이에 연동되는 규칙이
     * 있어도 결과가 순서에 따라 달라지지 않고 선택 요약도 한 번만 다시 계산하게 하기 위함이다.
     */
    void setAllChecked(boolean checked);
}
