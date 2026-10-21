package org.church.steward.ui.view.datamanagement.importing;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.table.ListTable;

/**
 * 가져오기 검토 단계의 표. 모델의 첫 칸을 가져올 행을 고르는 체크박스 칸으로 쓴다.
 * <ul>
 * <li>체크박스를 누르면 그 행의 체크가 바뀐다. 행 선택은 바뀌지 않는다. 체크박스 칸이라도 체크박스 바깥을 누르면 다른
 * 칸처럼 행을 고른다.</li>
 * <li>Space를 누르면 선택된 행의 체크가 바뀐다.</li>
 * <li>머리글의 체크박스는 체크할 수 있는 행이 모두 체크되었을 때만 체크로 보인다. 체크박스를 누르면 체크로 보일 때는
 * 모두 해제하고, 아닐 때(일부만 체크된 때 포함)는 모두 체크한다. 체크할 수 있는 행이 없으면 꺼진 모양이고 눌러도 바뀌지
 * 않는다. 행과 같이 머리글 칸의 체크박스 바깥을 누르면 아무 일도 하지 않는다.</li>
 * <li>체크할 수 없는 행(오류 행 등)의 체크박스는 꺼진 모양으로 그린다.</li>
 * </ul>
 * 체크 상태는 모델({@link CheckableTableModel})이 가진다. 머리글을 눌러 정렬하는 표에는 쓰지 않는다(머리글의 정렬
 * 아이콘을 그리지 않는다).
 */
public class CheckableImportTable extends ListTable {

    private static final long serialVersionUID = 1L;

    /** 체크박스 칸의 모델 기준 위치 */
    public static final int CHECK_COLUMN = 0;

    private static final String TOGGLE_ACTION_KEY = "checkableImportTable.toggle";
    /** 체크박스를 눌렀다고 보는 범위를 아이콘 바깥으로 넓히는 폭. 아이콘 가장자리를 살짝 벗어나 눌러도 토글된다. */
    private static final int CHECK_BOX_HIT_MARGIN = 2;
    /** 창이 커져도 넓어지지 않는 칸을 사용자가 넓힐 수 있는 한도. 기본 폭의 배수다. */
    private static final int NON_GROWING_MAX_RATIO = 3;

    private final transient CheckableTableModel checkModel;
    /** 행과 머리글의 체크박스 아이콘. 둘 다 이 아이콘을 칸 가운데에 그린다. */
    private final transient Icon checkIcon = UIManager.getIcon("CheckBox.icon");
    /** 머리글의 체크박스 아이콘을 그릴 때 상태를 담아 넘기는 체크박스. 화면에 놓지 않는다. */
    private final JCheckBox headerCheckBox = new JCheckBox();
    /** 마지막으로 누른 곳이 체크박스인지 여부. 그 누름에 이어지는 놓기, 클릭, 끌기도 표에 넘기지 않는다. */
    private boolean checkBoxPressed;

    public CheckableImportTable(CheckableTableModel model) {
        super(model);
        this.checkModel = model;

        getColumnModel().getColumn(CHECK_COLUMN)
                .setCellRenderer(new CheckCellRenderer());
        getColumnModel().getColumn(CHECK_COLUMN)
                .setResizable(false);

        JTableHeader header = getTableHeader();
        header.setDefaultRenderer(new CheckHeaderRenderer(header.getDefaultRenderer()));
        header.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = header.columnAtPoint(e.getPoint());
                if (SwingUtilities.isLeftMouseButton(e) && isCheckColumn(viewColumn)
                        && isOnCheckBox(header.getHeaderRect(viewColumn), e.getPoint())) {
                    toggleAll();
                }
            }
        });
        // 체크가 바뀌면 머리글의 체크박스도 다시 그린다.
        model.addTableModelListener(_ -> header.repaint());

        // 표의 기본 Space(선택에 행 추가) 대신 선택된 행의 체크를 바꾼다.
        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), TOGGLE_ACTION_KEY);
        getActionMap()
                .put(TOGGLE_ACTION_KEY, new AbstractAction() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for (int viewRow : getSelectedRows()) {
                            toggle(convertRowIndexToModel(viewRow));
                        }
                    }
                });
    }

    /**
     * 체크박스 칸의 폭을 정한다. 체크박스만 있는 칸이므로 사용자가 폭을 바꿀 수 없다.
     */
    public void setCheckColumnWidth(int width) {
        TableColumn column = getColumnModel().getColumn(CHECK_COLUMN);
        column.setPreferredWidth(width);
        column.setMinWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * 창이 커지면 함께 넓어지는 칸의 기본 폭을 정한다. 사용자가 폭을 바꿀 수 있다.
     */
    public void setGrowingColumnWidth(int modelColumn, int width) {
        getColumnModel().getColumn(convertColumnIndexToView(modelColumn))
                .setPreferredWidth(width);
    }

    /**
     * 창이 커져도 넓어지지 않는 칸의 기본 폭을 정한다. 사용자는 기본 폭부터 {@link #NON_GROWING_MAX_RATIO}배까지
     * 넓힐 수 있다.
     * <p>
     * 표는 늘어난 폭을 칸마다 최대 폭까지 남은 여유에 비례해 나눠 준다. 최대 폭이 정해진 이 칸의 여유는 최대 폭이 없는
     * 칸({@link #setGrowingColumnWidth})에 비해 무시할 만큼 작으므로, 늘어난 폭은 사실상 모두 그런 칸이 가져간다.
     * 최소 폭을 기본 폭으로 두어 창이 줄어도 좁아지지 않는다.
     */
    public void setNonGrowingColumnWidth(int modelColumn, int width) {
        TableColumn column = getColumnModel().getColumn(convertColumnIndexToView(modelColumn));
        column.setPreferredWidth(width);
        column.setMinWidth(width);
        column.setMaxWidth(width * NON_GROWING_MAX_RATIO);
    }

    /**
     * 체크는 체크박스를 누르거나 Space로만 바꾼다. 편집기를 띄우지 않으므로 F2 같은 편집 키로도 바뀌지 않는다.
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return !isCheckColumn(column) && super.isCellEditable(row, column);
    }

    /**
     * 체크박스를 누른 것은 여기서 체크만 바꾸고 표에 넘기지 않는다. 넘기면 표가 그 행을 고르거나(선택이 하나뿐이면
     * 해제하거나) 더블클릭으로 처리한다. 체크박스 칸이라도 체크박스 바깥을 누르면 다른 칸처럼 표가 처리한다.
     */
    @Override
    protected void processMouseEvent(MouseEvent e) {
        switch (e.getID()) {
        case MouseEvent.MOUSE_PRESSED -> {
            int viewRow = rowAtPoint(e.getPoint());
            int viewColumn = columnAtPoint(e.getPoint());
            checkBoxPressed = viewRow >= 0 && isCheckColumn(viewColumn)
                    && isOnCheckBox(getCellRect(viewRow, viewColumn, false), e.getPoint());
            if (checkBoxPressed) {
                requestFocusInWindow();
                if (SwingUtilities.isLeftMouseButton(e)) {
                    toggle(convertRowIndexToModel(viewRow));
                }
                return;
            }
        }
        case MouseEvent.MOUSE_RELEASED, MouseEvent.MOUSE_CLICKED -> {
            if (checkBoxPressed) {
                return;
            }
        }
        default -> {
            // 들어오고 나가는 것은 툴팁 등이 쓰므로 그대로 넘긴다.
        }
        }
        super.processMouseEvent(e);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        // 체크박스를 누른 채 끌어도 행을 고르지 않는다.
        if (e.getID() == MouseEvent.MOUSE_DRAGGED && checkBoxPressed) {
            return;
        }
        super.processMouseMotionEvent(e);
    }

    private boolean isCheckColumn(int viewColumn) {
        return viewColumn >= 0 && convertColumnIndexToModel(viewColumn) == CHECK_COLUMN;
    }

    /**
     * 누른 곳이 칸 가운데에 그린 체크박스 아이콘 위인지 판단한다. 행의 체크박스(가운데 정렬, 위아래와 좌우 여백이 같다)와
     * 머리글의 아이콘(가운데 정렬) 모두 아이콘이 칸의 가운데에 놓인다.
     *
     * @param cell 체크박스를 그린 칸의 영역
     */
    private boolean isOnCheckBox(Rectangle cell, Point point) {
        int width = checkIcon.getIconWidth();
        int height = checkIcon.getIconHeight();
        Rectangle checkBox = new Rectangle(cell.x + (cell.width - width) / 2, cell.y + (cell.height - height) / 2,
                width, height);
        checkBox.grow(CHECK_BOX_HIT_MARGIN, CHECK_BOX_HIT_MARGIN);
        return checkBox.contains(point);
    }

    private void toggle(int modelRow) {
        if (checkModel.isRowCheckable(modelRow)) {
            checkModel.setRowChecked(modelRow, !checkModel.isRowChecked(modelRow));
        }
    }

    private void toggleAll() {
        if (hasCheckableRow()) {
            checkModel.setAllChecked(!isAllChecked());
        }
    }

    private boolean hasCheckableRow() {
        for (int row = 0; row < checkModel.getRowCount(); row++) {
            if (checkModel.isRowCheckable(row)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return 체크할 수 있는 행이 하나 이상이고 그 행이 모두 체크되었는지 여부
     */
    private boolean isAllChecked() {
        boolean hasCheckable = false;
        for (int row = 0; row < checkModel.getRowCount(); row++) {
            if (!checkModel.isRowCheckable(row)) {
                continue;
            }
            if (!checkModel.isRowChecked(row)) {
                return false;
            }
            hasCheckable = true;
        }
        return hasCheckable;
    }

    /**
     * 행의 체크박스. 체크할 수 없는 행은 꺼진 모양으로 그린다.
     */
    private final class CheckCellRenderer implements TableCellRenderer {
        private final JCheckBox checkBox = new StyledCheckBox("", ComponentSize.MEDIUM);

        private CheckCellRenderer() {
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            checkBox.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            int modelRow = convertRowIndexToModel(row);
            checkBox.setSelected(checkModel.isRowChecked(modelRow));
            checkBox.setEnabled(checkModel.isRowCheckable(modelRow));
            checkBox.setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
            return checkBox;
        }
    }

    /**
     * 머리글 렌더러. FlatLaf의 머리글 모양(배경, 구분선)을 그대로 쓰도록 기본 렌더러를 감싸, 체크박스 칸에만 글자 대신
     * 체크박스 아이콘을 넣는다. 기본 렌더러의 컴포넌트는 모든 칸이 함께 쓰므로 다른 칸에서는 아이콘을 지운다.
     */
    private final class CheckHeaderRenderer implements TableCellRenderer {
        private final TableCellRenderer base;
        private final Icon headerIcon = new HeaderCheckIcon();

        private CheckHeaderRenderer(TableCellRenderer base) {
            this.base = base;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            boolean checkColumn = isCheckColumn(column);
            Component component = base.getTableCellRendererComponent(table, checkColumn ? "" : value, isSelected,
                    hasFocus, row, column);
            if (component instanceof JLabel label) {
                label.setIcon(checkColumn ? headerIcon : null);
            }
            return component;
        }
    }

    /**
     * 머리글의 체크박스 아이콘. FlatLaf의 체크박스 아이콘은 그리는 대상 컴포넌트에서 체크와 켜짐 상태를 읽으므로,
     * 그릴 때마다 상태를 담은 체크박스를 대상으로 넘긴다.
     */
    private final class HeaderCheckIcon implements Icon {

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            headerCheckBox.setSelected(isAllChecked());
            headerCheckBox.setEnabled(hasCheckableRow());
            checkIcon.paintIcon(headerCheckBox, g, x, y);
        }

        @Override
        public int getIconWidth() {
            return checkIcon.getIconWidth();
        }

        @Override
        public int getIconHeight() {
            return checkIcon.getIconHeight();
        }
    }
}
