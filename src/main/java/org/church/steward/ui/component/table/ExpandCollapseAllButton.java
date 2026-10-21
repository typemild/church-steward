package org.church.steward.ui.component.table;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * {@link TreeTable}의 모든 행을 펼치거나 접는 버튼. 펼쳐진 행이 하나라도 보이면 "모두 접기", 모두 접혀 있으면 "모두
 * 펼치기"가 된다.
 * 아이콘은 지금 상태를 나타낸다. 펼쳐진 행이 있으면(누르면 모두 접기) 열린 폴더, 모두 접혀 있으면(누르면 모두 펼치기) 닫힌 폴더다.
 * 표의 행이 바뀔 때마다(화살표로 접거나 펼칠 때 포함) 상태를 다시 맞추며, 접거나 펼칠 행이 없으면 꺼진다.
 */
public class ExpandCollapseAllButton extends IconButton {

    private static final long serialVersionUID = 1L;

    private final TreeTable table;
    private boolean collapseMode;

    public ExpandCollapseAllButton(TreeTable table, ComponentSize componentSize) {
        super(FontAwesomeSolid.FOLDER, componentSize);
        this.table = table;

        addActionListener(_ -> {
            if (collapseMode) {
                table.collapseAll();
            } else {
                table.expandAll();
            }
        });
        table.getModel()
                .addTableModelListener(_ -> updateState());
        updateState();
    }

    private void updateState() {
        AbstractTreeTableModel<?> model = table.getTreeModel();
        collapseMode = model.hasExpandedRow();
        setIcon(collapseMode ? FontAwesomeSolid.FOLDER_OPEN : FontAwesomeSolid.FOLDER);
        setToolTipText(collapseMode ? "모두 접기" : "모두 펼치기");
        setEnabled(model.hasExpandableRow());
    }

}
