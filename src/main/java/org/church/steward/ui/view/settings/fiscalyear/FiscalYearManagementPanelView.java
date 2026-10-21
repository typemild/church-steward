package org.church.steward.ui.view.settings.fiscalyear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.fiscalyear.controller.FiscalYearController;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.AmountCellRenderer;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.main.FiscalYearSelector;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 설정의 회계 연도 관리 화면. 회계 연도를 추가, 수정, 삭제하고 마감하거나 마감을 취소한다.
 * 마감된 회계 연도는 마감할 때 계산한 총 수입, 총 지출, 차액을 목록에 보여주며, 마감을 취소하기 전에는 수정하거나 삭제할 수 없다.
 * 이 목록의 선택은 사이드바의 회계 연도 선택과 독립이고, 회계 연도가 바뀌면 사이드바의 목록을 갱신한다.
 */
@UiView
@Slf4j
public class FiscalYearManagementPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    /**
     * 회계 연도, 상태, 차액 칸은 폭을 고정하고, 남는 폭은 총 수입과 총 지출 칸이 똑같이 나눠 쓴다.
     * 차액은 총 수입이나 총 지출보다 훨씬 작은 금액이어서 칸을 좁게 둔다.
     */
    private static final int FISCAL_YEAR_COLUMN_WIDTH = 195;
    private static final int STATUS_COLUMN_WIDTH = 60;
    private static final int TOTAL_COLUMN_WIDTH = 133;
    private static final int BALANCE_COLUMN_WIDTH = 95;

    private final FiscalYearController fiscalYearController;
    private final FiscalYearSelector fiscalYearSelector;

    private final FiscalYearTableModel tableModel = new FiscalYearTableModel();
    private final ListTable table = new ListTable(tableModel);
    private final SubFieldLabel totalCountLabel = new SubFieldLabel("총 : 0건", ComponentSize.MEDIUM);

    private final IconButton addButton = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.MEDIUM);
    private final IconButton editButton = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.MEDIUM);
    private final IconButton deleteButton = new IconButton(FontAwesomeSolid.TRASH_ALT, ComponentSize.MEDIUM);
    /** 마감과 마감 취소를 함께 맡는 버튼. 선택된 회계 연도의 상태에 따라 아이콘과 툴팁이 바뀐다. */
    private final IconButton closeToggleButton = new IconButton(FontAwesomeSolid.LOCK_OPEN, ComponentSize.MEDIUM);

    public FiscalYearManagementPanelView(FiscalYearController fiscalYearController,
            FiscalYearSelector fiscalYearSelector) {
        this.fiscalYearController = fiscalYearController;
        this.fiscalYearSelector = fiscalYearSelector;

        setLayout(new BorderLayout(0, 10));
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(new JScrollPane(createTable()), BorderLayout.CENTER);

        updateButtons();
    }

    private JPanel createHeaderPanel() {
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        addButton.setToolTipText("회계 연도 추가");
        editButton.setToolTipText("회계 연도 수정");
        deleteButton.setToolTipText("회계 연도 삭제");
        addButton.addActionListener(_ -> onAdd());
        editButton.addActionListener(_ -> onEdit());
        deleteButton.addActionListener(_ -> onDelete());
        closeToggleButton.addActionListener(_ -> onToggleClosed());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(closeToggleButton);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(totalCountLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JTable createTable() {
        // 회계 연도를 이름과 기간 두 줄로 적으므로 다른 표보다 행이 높다.
        table.setRowHeight(Math.round(UIFonts.FONT_SIZE_BASE * 3.2f));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        TableColumnModel columns = table.getColumnModel();
        setFixedWidth(columns.getColumn(FiscalYearTableModel.COLUMN_FISCAL_YEAR), FISCAL_YEAR_COLUMN_WIDTH);
        columns.getColumn(FiscalYearTableModel.COLUMN_FISCAL_YEAR)
                .setCellRenderer(new FiscalYearCellRenderer());
        setFixedWidth(columns.getColumn(FiscalYearTableModel.COLUMN_STATUS), STATUS_COLUMN_WIDTH);
        columns.getColumn(FiscalYearTableModel.COLUMN_STATUS)
                .setCellRenderer(new PaddedCellRenderer(SwingConstants.CENTER));
        int[] totalColumns = { FiscalYearTableModel.COLUMN_TOTAL_INCOME, FiscalYearTableModel.COLUMN_TOTAL_EXPENSE };
        for (int column : totalColumns) {
            // 폭을 고정하지 않고 선호 폭만 같게 두어, 남는 폭을 두 칸이 똑같이 나눠 갖게 한다.
            columns.getColumn(column)
                    .setPreferredWidth(TOTAL_COLUMN_WIDTH);
            columns.getColumn(column)
                    .setCellRenderer(new AmountCellRenderer());
        }
        setFixedWidth(columns.getColumn(FiscalYearTableModel.COLUMN_BALANCE), BALANCE_COLUMN_WIDTH);
        columns.getColumn(FiscalYearTableModel.COLUMN_BALANCE)
                .setCellRenderer(new AmountCellRenderer());

        table.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        updateButtons();
                    }
                });
        table.setRowDoubleClickHandler(_ -> onEdit());
        return table;
    }

    private void setFixedWidth(TableColumn column, int width) {
        column.setMinWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * 다른 화면에서 수입/지출이 바뀌어도 삭제 가능 여부 등이 달라지므로, 화면이 보일 때마다 목록을 다시 불러온다.
     */
    @Override
    public void onMenuSelected() {
        FiscalYearDto selected = getSelectedFiscalYear();
        loadFiscalYears(selected == null ? null : selected.getId());
    }

    private FiscalYearDto getSelectedFiscalYear() {
        int row = table.getSelectedRow();
        return row == -1 ? null : tableModel.getFiscalYear(row);
    }

    /**
     * @param idToSelect 목록을 불러온 뒤 선택할 회계 연도의 ID. null이거나 목록에 없으면 아무것도 선택하지 않는다.
     */
    private void loadFiscalYears(Long idToSelect) {
        AsyncTask.run(this,
                fiscalYearController::listFiscalYears,
                fiscalYears -> {
                    tableModel.setData(fiscalYears);
                    totalCountLabel.setText("총 : " + fiscalYears.size() + "건");

                    for (int row = 0; row < fiscalYears.size(); row++) {
                        if (fiscalYears.get(row)
                                .getId()
                                .equals(idToSelect)) {
                            table.setRowSelectionInterval(row, row);
                            break;
                        }
                    }
                    updateButtons();
                },
                err -> {
                    log.error("Failed to load fiscal years", err);
                    showErrorMessage("목록 로드 실패", err);
                });
    }

    /**
     * 선택된 회계 연도에 따라 버튼을 켜고 끈다. 마감된 회계 연도는 수정, 삭제할 수 없고 마감 취소만 할 수 있다.
     */
    private void updateButtons() {
        FiscalYearDto selected = getSelectedFiscalYear();
        boolean closed = selected != null && selected.isClosed();
        boolean editable = selected != null && !closed;
        editButton.setEnabled(editable);
        deleteButton.setEnabled(editable);
        closeToggleButton.setEnabled(selected != null);

        // 마감 버튼의 아이콘은 현재 상태를 보여 주는 반면, 툴팁은 버튼을 눌렀을 때 발생하는 효과(기능)를 설명한다.
        closeToggleButton.setIcon(closed ? FontAwesomeSolid.LOCK : FontAwesomeSolid.LOCK_OPEN);
        closeToggleButton.setToolTipText(closed ? "회계 연도 마감 취소" : "회계 연도 마감");
    }

    /**
     * 선택된 회계 연도가 마감되어 있으면 마감을 취소하고, 아니면 마감한다.
     */
    private void onToggleClosed() {
        FiscalYearDto selected = getSelectedFiscalYear();
        if (selected == null) {
            return;
        }
        if (selected.isClosed()) {
            onReopen();
        } else {
            onClose();
        }
    }

    private void onAdd() {
        // 저장은 다이얼로그가 실행한다. 이름 중복이나 기간 겹침으로 실패하면 다이얼로그를 연 채로 오류를 보여주기 위함이다.
        FiscalYearEntryDialog.forCreation(getOwnerFrame(), tableModel.getFiscalYears(),
                (name, startDate, endDate, sourceFiscalYearId) -> {
                    if (sourceFiscalYearId == null) {
                        fiscalYearController.createFiscalYear(name, startDate, endDate);
                    } else {
                        fiscalYearController.createFiscalYearWithCopy(name, startDate, endDate, sourceFiscalYearId);
                    }
                },
                () -> afterFiscalYearsChanged(null))
                .setVisible(true);
    }

    private void onEdit() {
        FiscalYearDto selected = getSelectedFiscalYear();
        if (selected == null) {
            return;
        }
        if (selected.isClosed()) {
            JOptionPane.showMessageDialog(this, "마감된 회계 연도는 수정할 수 없습니다. 먼저 마감을 취소해주세요.");
            return;
        }

        Long id = selected.getId();
        FiscalYearEntryDialog.forUpdate(getOwnerFrame(), selected,
                (name, startDate, endDate, _) -> fiscalYearController.updateFiscalYear(id, name, startDate, endDate),
                () -> afterFiscalYearsChanged(id))
                .setVisible(true);
    }

    private void onDelete() {
        FiscalYearDto selected = getSelectedFiscalYear();
        if (selected == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "\"" + selected.getName() + "\" 회계 연도를 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Long id = selected.getId();
        AsyncTask.run(this, () -> {
            fiscalYearController.deleteFiscalYear(id);
            return null;
        }, _ -> afterFiscalYearsChanged(null), err -> showErrorMessage("삭제 실패", err));
    }

    private void onClose() {
        FiscalYearDto selected = getSelectedFiscalYear();
        if (selected == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "\"" + selected.getName() + "\" 회계 연도를 마감하시겠습니까?\n"
                        + "마감하면 이 회계 연도의 예산과 수입/지출을 더는 수정할 수 없고 조회만 할 수 있습니다.",
                "마감 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Long id = selected.getId();
        AsyncTask.run(this, () -> {
            fiscalYearController.closeFiscalYear(id);
            return null;
        }, _ -> afterFiscalYearsChanged(id), err -> showErrorMessage("마감 실패", err));
    }

    private void onReopen() {
        FiscalYearDto selected = getSelectedFiscalYear();
        if (selected == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "\"" + selected.getName() + "\" 회계 연도의 마감을 취소하시겠습니까?\n"
                        + "마감할 때 저장한 총 수입, 총 지출, 차액은 지워집니다.",
                "마감 취소 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        Long id = selected.getId();
        AsyncTask.run(this, () -> {
            fiscalYearController.reopenFiscalYear(id);
            return null;
        }, _ -> afterFiscalYearsChanged(id), err -> showErrorMessage("마감 취소 실패", err));
    }

    /**
     * 회계 연도가 바뀐 뒤 목록을 다시 불러오고, 사이드바의 회계 연도 목록도 갱신한다.
     *
     * @param idToSelect 목록을 불러온 뒤 선택할 회계 연도의 ID. null이면 아무것도 선택하지 않는다.
     */
    private void afterFiscalYearsChanged(Long idToSelect) {
        loadFiscalYears(idToSelect);
        fiscalYearSelector.reload();
    }

    private Frame getOwnerFrame() {
        return (Frame) SwingUtilities.getWindowAncestor(this);
    }

    private void showErrorMessage(String title, Throwable err) {
        log.error("{}: {}", title, err.getMessage());
        JOptionPane.showMessageDialog(this, title + ": " + err.getMessage());
    }
}
