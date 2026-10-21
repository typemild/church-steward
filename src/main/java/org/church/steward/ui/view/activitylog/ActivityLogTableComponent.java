package org.church.steward.ui.view.activitylog;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;

import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

public class ActivityLogTableComponent extends JScrollPane {

    private static final long serialVersionUID = 1L;
    private final ActivityLogTableModel tableModel;
    private final ListTable table;
    private final ActivityLogView view;
    private final IconTableCellRenderer detailRenderer = new IconTableCellRenderer(FontAwesomeSolid.SEARCH,
            UIFonts.FONT_SIZE_BASE);

    public ActivityLogTableComponent(ActivityLogView view) {
        this.view = view;
        this.tableModel = new ActivityLogTableModel();

        this.table = new ListTable(tableModel);

        this.setViewportView(table);
        this.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));

        setupTableSettings();
        setupMouseEvents();
    }

    private void setupTableSettings() {
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 컬럼별 너비 및 정렬 설정
        table.getColumnModel().getColumn(0).setPreferredWidth(160); // 일시
        table.getColumnModel().getColumn(0).setCellRenderer(createRenderer(SwingConstants.CENTER));

        table.getColumnModel().getColumn(1).setPreferredWidth(91); // 활동 유형 (배지 폭 78px + 여백)
        table.getColumnModel().getColumn(1).setCellRenderer(new ActivityTypeBadgeRenderer());

        table.getColumnModel().getColumn(2).setPreferredWidth(90); // 정보 유형
        table.getColumnModel().getColumn(2).setCellRenderer(createRenderer(SwingConstants.CENTER));

        table.getColumnModel().getColumn(3).setPreferredWidth(180); // 내용
        table.getColumnModel().getColumn(3).setCellRenderer(createRenderer(SwingConstants.LEADING));

        table.getColumnModel().getColumn(4).setPreferredWidth(70);// 사용자 ID
        table.getColumnModel().getColumn(4).setCellRenderer(createRenderer(SwingConstants.CENTER));

        // 상세 보기 컬럼 설정
        table.getColumnModel().getColumn(5).setMinWidth(40);
        table.getColumnModel().getColumn(5).setMaxWidth(40);
        table.getColumnModel().getColumn(5).setCellRenderer(detailRenderer);
    }

    private DefaultTableCellRenderer createRenderer(int align) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public void setBorder(Border border) {
                super.setBorder(BorderFactory.createCompoundBorder(
                        border, BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
        };
        renderer.setHorizontalAlignment(align);
        return renderer;
    }

    private void setupMouseEvents() {
        table.setRowDoubleClickHandler(modelRow -> view.showDetail(tableModel.getDtoAt(modelRow)));

        // 상세 보기 칸은 한 번만 눌러도 상세 정보를 연다. 상세 창은 모달이어서, 이 칸을 더블클릭해도 두 번째 클릭은 표에 오지 않는다.
        // 상세 보기 칸에 마우스를 올리면 아이콘 색을 바꾸고 손 모양 커서를 보여준다.
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                if (viewRow < 0 || viewCol < 0 || e.getClickCount() != 1) {
                    return;
                }

                if (isDetailColumn(viewCol)) {
                    table.setRowSelectionInterval(viewRow, viewRow);
                    view.showDetail(tableModel.getDtoAt(table.convertRowIndexToModel(viewRow)));
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                int viewRow = table.rowAtPoint(e.getPoint());
                int viewCol = table.columnAtPoint(e.getPoint());
                updateHoveredRow(viewRow >= 0 && isDetailColumn(viewCol) ? viewRow : -1);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                updateHoveredRow(-1);
            }
        };
        table.addMouseListener(mouseAdapter);
        table.addMouseMotionListener(mouseAdapter);
    }

    private boolean isDetailColumn(int viewCol) {
        return viewCol >= 0 && table.convertColumnIndexToModel(viewCol) == table.getColumnCount() - 1;
    }

    /**
     * 상세 보기 칸에 마우스가 올라간 행을 바꾸고, 바뀐 칸을 다시 그린다.
     */
    private void updateHoveredRow(int viewRow) {
        int oldRow = detailRenderer.getHoveredRow();
        if (oldRow == viewRow) {
            return;
        }

        detailRenderer.setHoveredRow(viewRow);
        table.setCursor(viewRow >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
        repaintDetailCell(oldRow);
        repaintDetailCell(viewRow);
    }

    private void repaintDetailCell(int viewRow) {
        if (viewRow < 0 || viewRow >= table.getRowCount()) {
            return;
        }
        int viewCol = table.convertColumnIndexToView(table.getColumnCount() - 1);
        table.repaint(table.getCellRect(viewRow, viewCol, false));
    }

    public ActivityLogTableModel getTableModel() {
        return tableModel;
    }

    public JTable getTable() {
        return table;
    }
}
