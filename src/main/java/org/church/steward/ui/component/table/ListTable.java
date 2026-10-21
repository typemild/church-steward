package org.church.steward.ui.component.table;

import java.awt.Font;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.IntConsumer;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableModel;

import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

/**
 * 공용 목록 표. 프로그램의 목록 표는 모두 이 표를 써서 같은 모양과 동작을 따른다.
 * <p>
 * 모양의 기본값으로 기본 글꼴, 행 높이({@link #DEFAULT_ROW_HEIGHT}), 격자 없음, 스크롤 영역 높이 채우기, 머리글
 * 순서 변경 끄기를 정한다.
 * 화면마다 다른 선택 방식(한 행/여러 행)은 각 화면이 정하고, 행 높이가 달라야 하면 화면에서 다시 지정한다.
 * <p>
 * 선택을 해제할 수 있다.
 * <ul>
 * <li>하나만 선택되어 있는 행을 Ctrl/Shift 없이 다시 클릭하면 선택이 풀린다.</li>
 * <li>표의 빈 영역을 클릭하면 선택이 풀린다.</li>
 * </ul>
 * 여러 행을 선택할 수 있는 표에서도 쓸 수 있다. 여러 행이 선택되어 있거나 Ctrl/Shift를 누른 채 클릭하면
 * 여러 행 선택의 기본 동작을 따른다.
 * <p>
 * 행을 더블클릭했을 때 할 일은 {@link #setRowDoubleClickHandler}로 등록한다. 두 번째 클릭에서 선택이 풀리므로,
 * 처리기를 부르기 전에 더블클릭한 행을 다시 선택한다. 화면에서 마우스 이벤트로 더블클릭을 직접 처리하지 않는다.
 * 처리기를 등록하면 행을 고른 채 누른 Enter도 다음 행으로 옮기지 않고 같은 처리기를 부른다.
 * <p>
 * 모델이 {@link TooltipTableModel}을 구현하면 마우스가 놓인 셀의 툴팁을 모델에서 받아 보여준다.
 */
public class ListTable extends JTable {

    private static final long serialVersionUID = 1L;

    /** 목록 표의 기본 행 높이. 기본 글자 크기의 2.3배다. */
    public static final int DEFAULT_ROW_HEIGHT = Math.round(UIFonts.FONT_SIZE_BASE * 2.3f);

    private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_BASE);
    private static final String ROW_ACTION_KEY = "listTable.rowAction";
    private static final KeyStroke ENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    private transient IntConsumer rowDoubleClickHandler;

    public ListTable(TableModel model) {
        super(model);

        setFont(FONT);
        setRowHeight(DEFAULT_ROW_HEIGHT);
        setShowGrid(false);
        // 행이 적어도 표가 스크롤 영역을 채워, 빈 영역을 눌러 선택을 풀 수 있게 한다.
        setFillsViewportHeight(true);
        getTableHeader()
                .setReorderingAllowed(false);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    clearRowSelection();
                    return;
                }
                if (e.getClickCount() == 2 && rowDoubleClickHandler != null && acceptsRowDoubleClick(e.getPoint())) {
                    setRowSelectionInterval(viewRow, viewRow);
                    rowDoubleClickHandler.accept(convertRowIndexToModel(viewRow));
                }
            }
        });
    }

    /**
     * 행을 더블클릭하거나 행을 고른 채 Enter를 눌렀을 때 할 일을 정한다. 더블클릭은 처리기를 부르기 전에 더블클릭한 행
     * 하나만 선택된 상태로 만들고, Enter는 선택을 바꾸지 않는다(여러 행을 고른 채 누를 수 있다).
     *
     * @param handler 더블클릭한 행이나, Enter를 누를 때 마지막으로 고른 행의 모델 기준 인덱스를 받는 처리기. null이면
     *                더블클릭에 아무 일도 하지 않고 Enter는 표의 기본 동작(다음 행으로 이동)으로 돌아간다.
     */
    public void setRowDoubleClickHandler(IntConsumer handler) {
        this.rowDoubleClickHandler = handler;
        // 표의 기본 Enter는 UI가 넣은 부모 InputMap에 있으므로, 표 자신의 InputMap에 넣으면 덮어쓰고 지우면 돌아간다.
        InputMap inputMap = getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        if (handler == null) {
            inputMap.remove(ENTER);
            return;
        }
        inputMap.put(ENTER, ROW_ACTION_KEY);
        getActionMap().put(ROW_ACTION_KEY, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                int viewRow = getSelectionModel().getLeadSelectionIndex();
                if (viewRow < 0 || !isRowSelected(viewRow)) {
                    viewRow = getSelectedRow();
                }
                if (viewRow >= 0 && rowDoubleClickHandler != null) {
                    rowDoubleClickHandler.accept(convertRowIndexToModel(viewRow));
                }
            }
        });
    }

    /**
     * 그 위치의 더블클릭을 행 더블클릭으로 처리할지 정한다. 하위 클래스는 표 자체가 처리하는 영역(접기/펼치기 화살표 등)을 제외한다.
     */
    protected boolean acceptsRowDoubleClick(Point point) {
        return true;
    }

    /**
     * 표를 감싼 스크롤 패인의 테두리가 FlatLaf 기본값이면 회색 선으로 바꾼다. FlatLaf의 테두리는 표에 포커스가 있으면
     * 포커스 색이 되는데, 목록 표에는 포커스 테두리를 두지 않는다. 화면에서 테두리를 따로 정했으면 그대로 둔다.
     */
    @Override
    protected void configureEnclosingScrollPane() {
        super.configureEnclosingScrollPane();
        if (SwingUtilities.getAncestorOfClass(JScrollPane.class, this) instanceof JScrollPane scrollPane
                && scrollPane.getViewport()
                        .getView() == this
                && scrollPane.getBorder() instanceof UIResource) {
            scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        }
    }

    @Override
    public void changeSelection(int row, int col, boolean toggle, boolean extend) {
        if (!toggle && !extend && getSelectedRowCount() == 1 && isRowSelected(row)) {
            clearRowSelection();
            return;
        }
        super.changeSelection(row, col, toggle, extend);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        if (!(getModel() instanceof TooltipTableModel tooltipModel)) {
            return super.getToolTipText(e);
        }

        int viewRow = rowAtPoint(e.getPoint());
        int viewCol = columnAtPoint(e.getPoint());
        if (viewRow < 0 || viewCol < 0) {
            return null;
        }

        String text = tooltipModel.getTooltipText(convertRowIndexToModel(viewRow), convertColumnIndexToModel(viewCol));
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * 행 선택을 해제한다. 선택만 지우면 마지막으로 누른 셀에 포커스 테두리가 남으므로 기준 위치도 함께 지운다.
     */
    public void clearRowSelection() {
        clearSelection();
        getSelectionModel().setAnchorSelectionIndex(-1);
        getSelectionModel().setLeadSelectionIndex(-1);
        getColumnModel().getSelectionModel().setAnchorSelectionIndex(-1);
        getColumnModel().getSelectionModel().setLeadSelectionIndex(-1);
    }

}
