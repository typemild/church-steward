package org.church.steward.ui.component.table;

import javax.swing.table.TableModel;

/**
 * 셀마다 툴팁을 제공하는 표 모델. {@link ListTable}은 모델이 이 인터페이스를 구현하면 마우스가 놓인 셀의
 * 툴팁을 보여준다.
 */
public interface TooltipTableModel extends TableModel {

    /**
     * 셀의 툴팁을 반환한다. 행과 열은 정렬이나 열 이동과 무관한 모델 기준의 위치다.
     *
     * @return 툴팁 문구. 툴팁이 없으면 null 또는 빈 문자열
     */
    String getTooltipText(int row, int column);

}
