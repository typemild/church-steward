package org.church.steward.ui.component.table;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.KeyStroke;

import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.Ikon;

/**
 * 행을 접고 펼칠 수 있는 목록 표. 트리 칸(모델 기준 열 하나)에 깊이만큼 들여 쓴 접기/펼치기 화살표와 글자를 그리고,
 * 화살표를 누르면 그 행을 접거나 펼친다. 화살표 위에 마우스를 올리면 누를 수 있음을 알리도록 강조색으로 그린다.
 * <p>
 * 접거나 펼치면 모델이 행을 다시 만들면서 선택이 풀리므로, 방금 접거나 펼친 행을 다시 선택한다.
 * 화살표를 빠르게 두 번 누른 것은 행 더블클릭으로 보지 않으므로 {@link #setRowDoubleClickHandler}의 처리기가
 * 불리지 않는다.
 * <p>
 * 좌/우 화살표 키는 칸을 옮기지 않고 선택된 행을 다룬다. 오른쪽 키는 접힌 행을 펼치고, 왼쪽 키는 펼쳐진 행을 접거나
 * 접혀 있거나 하위 행이 없는 행이면 상위 행으로 선택을 옮긴다. 여러 행이 선택되어 있으면 마지막으로 고른 행을 다룬다.
 * <p>
 * 트리 칸의 값은 표시할 글자이며, 렌더러는 이 표가 정한다. 트리 칸에 다른 렌더러를 지정하지 않는다.
 */
public class TreeTable extends ListTable {

    private static final long serialVersionUID = 1L;

    /** 셀 왼쪽 끝과 최상위 행의 화살표 사이의 여백 */
    static final int LEFT_MARGIN = Math.round(UIFonts.FONT_SIZE_BASE * 0.6f);
    /** 깊이가 하나 늘 때마다 들여 쓰는 폭. 화살표 자리의 폭과 같아서 하위 행의 화살표가 상위 행의 글자 아래에 온다. */
    static final int INDENT_WIDTH = Math.round(UIFonts.FONT_SIZE_BASE * 1.8f);
    /** 화살표(또는 말단 행의 아이콘) 자리의 폭. 이 자리를 누르면 접거나 펼친다. */
    static final int TOGGLE_WIDTH = Math.round(UIFonts.FONT_SIZE_BASE * 1.8f);

    private static final String EXPAND_ACTION = "treeTable.expand";
    private static final String COLLAPSE_OR_PARENT_ACTION = "treeTable.collapseOrParent";

    private final AbstractTreeTableModel<?> treeModel;
    private final int treeColumn;
    private final TreeTableCellRenderer treeRenderer = new TreeTableCellRenderer();
    /** 마우스가 화살표 위에 올라가 있는 행(뷰 기준). 없으면 -1이다. */
    private int hoveredToggleRow = -1;

    /**
     * @param treeColumn 화살표와 들여쓰기를 그릴 칸의 모델 기준 인덱스
     */
    public TreeTable(AbstractTreeTableModel<?> model, int treeColumn) {
        super(model);
        this.treeModel = model;
        this.treeColumn = treeColumn;

        getColumnModel().getColumn(convertColumnIndexToView(treeColumn))
                .setCellRenderer(treeRenderer);

        MouseAdapter mouseHandler = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = findToggleRow(e.getPoint());
                if (viewRow >= 0) {
                    toggle(viewRow);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                setHoveredToggleRow(findToggleRow(e.getPoint()));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setHoveredToggleRow(-1);
            }
        };
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);

        installArrowKeys();
    }

    /**
     * 좌/우 화살표 키를 칸 이동 대신 접기/펼치기에 쓴다.
     */
    private void installArrowKeys() {
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), EXPAND_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), EXPAND_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), COLLAPSE_OR_PARENT_ACTION);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), COLLAPSE_OR_PARENT_ACTION);

        ActionMap actionMap = getActionMap();
        actionMap.put(EXPAND_ACTION, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                expandLeadRow();
            }
        });
        actionMap.put(COLLAPSE_OR_PARENT_ACTION, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                collapseLeadRowOrMoveToParent();
            }
        });
    }

    /**
     * 키보드로 다룰 행(선택된 행 가운데 마지막으로 고른 행)의 뷰 인덱스. 선택된 행이 없으면 -1이다.
     */
    private int getLeadRow() {
        int viewRow = getSelectionModel().getLeadSelectionIndex();
        return viewRow >= 0 && viewRow < getRowCount() && isRowSelected(viewRow) ? viewRow : -1;
    }

    /**
     * 선택된 행이 접혀 있으면 펼친다.
     */
    private void expandLeadRow() {
        int viewRow = getLeadRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = convertRowIndexToModel(viewRow);
        if (treeModel.isExpandable(modelRow) && !treeModel.isExpanded(modelRow)) {
            toggle(viewRow);
        }
    }

    /**
     * 선택된 행이 펼쳐져 있으면 접고, 아니면 상위 행으로 선택을 옮긴다.
     */
    private void collapseLeadRowOrMoveToParent() {
        int viewRow = getLeadRow();
        if (viewRow < 0) {
            return;
        }
        int modelRow = convertRowIndexToModel(viewRow);
        if (treeModel.isExpanded(modelRow)) {
            toggle(viewRow);
            return;
        }

        int parentModelRow = findParentRow(modelRow);
        if (parentModelRow >= 0) {
            int parentViewRow = convertRowIndexToView(parentModelRow);
            setRowSelectionInterval(parentViewRow, parentViewRow);
            scrollRectToVisible(getCellRect(parentViewRow, 0, true));
        }
    }

    /**
     * 상위 행의 모델 인덱스. 상위 행은 앞쪽 행 가운데 깊이가 더 얕은 가장 가까운 행이다. 최상위 행이면 -1을 반환한다.
     */
    private int findParentRow(int modelRow) {
        int depth = treeModel.getDepth(modelRow);
        for (int row = modelRow - 1; row >= 0; row--) {
            if (treeModel.getDepth(row) < depth) {
                return row;
            }
        }
        return -1;
    }

    /**
     * 하위 행이 없는 행의 화살표 자리에 그릴 아이콘을 정한다. null이면 아이콘을 그리지 않고 그 자리까지 글자를 당겨 적는다.
     */
    public void setLeafIcon(Ikon leafIcon) {
        treeRenderer.setLeafIcon(leafIcon);
        repaint();
    }

    /**
     * 하위 행이 있는 행의 트리 칸 글자를 굵게 적을지 정한다.
     */
    public void setExpandableRowsBold(boolean bold) {
        treeRenderer.setExpandableRowsBold(bold);
        repaint();
    }

    /**
     * 모든 행을 펼친다. 선택했던 행은 그대로 선택해 둔다.
     */
    public void expandAll() {
        List<TreeTableNode<?>> selectedNodes = getSelectedNodes();
        treeModel.expandAll();
        selectNodes(selectedNodes);
    }

    /**
     * 모든 행을 접는다. 선택했던 행 가운데 접혀서 보이지 않게 된 행은 선택에서 빠진다.
     */
    public void collapseAll() {
        List<TreeTableNode<?>> selectedNodes = getSelectedNodes();
        treeModel.collapseAll();
        selectNodes(selectedNodes);
    }

    private List<TreeTableNode<?>> getSelectedNodes() {
        List<TreeTableNode<?>> nodes = new ArrayList<>();
        for (int viewRow : getSelectedRows()) {
            nodes.add(treeModel.getRowNode(convertRowIndexToModel(viewRow)));
        }
        return nodes;
    }

    private void selectNodes(List<TreeTableNode<?>> nodes) {
        for (TreeTableNode<?> node : nodes) {
            int modelRow = treeModel.indexOfNode(node);
            if (modelRow >= 0) {
                int viewRow = convertRowIndexToView(modelRow);
                addRowSelectionInterval(viewRow, viewRow);
            }
        }
    }

    /**
     * 접기/펼치기 화살표를 누른 것은 행 더블클릭으로 보지 않는다.
     */
    @Override
    protected boolean acceptsRowDoubleClick(Point point) {
        return findToggleRow(point) < 0;
    }

    AbstractTreeTableModel<?> getTreeModel() {
        return treeModel;
    }

    int getHoveredToggleRow() {
        return hoveredToggleRow;
    }

    /**
     * 깊이에 따른 화살표 자리의 시작 위치(셀 왼쪽 끝 기준).
     */
    static int toggleStart(int depth) {
        return LEFT_MARGIN + depth * INDENT_WIDTH;
    }

    /**
     * 그 위치가 하위 행이 있는 행의 화살표 위이면 그 행의 뷰 인덱스를, 아니면 -1을 반환한다.
     */
    private int findToggleRow(Point point) {
        int viewRow = rowAtPoint(point);
        int viewCol = columnAtPoint(point);
        if (viewRow < 0 || viewCol < 0 || convertColumnIndexToModel(viewCol) != treeColumn) {
            return -1;
        }
        int modelRow = convertRowIndexToModel(viewRow);
        if (!treeModel.isExpandable(modelRow)) {
            return -1;
        }

        int x = point.x - getCellRect(viewRow, viewCol, true).x;
        int start = toggleStart(treeModel.getDepth(modelRow));
        return x >= start && x <= start + TOGGLE_WIDTH ? viewRow : -1;
    }

    private void toggle(int viewRow) {
        int modelRow = convertRowIndexToModel(viewRow);
        treeModel.toggleExpanded(modelRow);

        // 접거나 펼친 행의 모델 위치는 그대로이므로 그 자리를 다시 선택한다.
        int newViewRow = convertRowIndexToView(modelRow);
        setRowSelectionInterval(newViewRow, newViewRow);
    }

    private void setHoveredToggleRow(int viewRow) {
        if (hoveredToggleRow != viewRow) {
            hoveredToggleRow = viewRow;
            repaint();
        }
    }

}
