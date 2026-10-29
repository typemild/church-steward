package org.church.steward.ui.view.financialrecord.budget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.table.AmountCellRenderer;
import org.church.steward.ui.component.table.ExpandCollapseAllButton;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.component.table.TreeTable;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.FinancialRecordActionHandler;
import org.church.steward.util.NumberUtils;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * "예산별 보기" 탭의 수입 또는 지출 예산 목록.
 * 조회된 내역이 속한 예산만 보여주며, 예산 행을 펼치면 그 예산의 내역들이 나타난다.
 */
public class BudgetRecordPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final FinancialRecordType type;
    private final BudgetRecordTableModel tableModel;
    private final TreeTable table;

    private final IconButton btnAdd = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
    private final IconButton btnEdit = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    private final IconButton btnDelete = new IconButton(FontAwesomeSolid.TRASH_ALT, ComponentSize.SMALL);
    /** 상세 보기 버튼. 편집할 수 없을 때(마감된 회계 연도) 추가/수정/삭제 버튼 대신 보인다. */
    private final IconButton btnView = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    private boolean editable = true;
    private final FieldLabel selectionSumLabel = new FieldLabel("", SwingConstants.RIGHT, ComponentSize.MEDIUM);

    private final FinancialRecordActionHandler actionHandler;

    public BudgetRecordPanel(FinancialRecordType type, FinancialRecordActionHandler actionHandler) {
        this.type = type;
        this.actionHandler = actionHandler;
        this.tableModel = new BudgetRecordTableModel();

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        table = new TreeTable(tableModel, BudgetRecordTableModel.COLUMN_NAME);
        table.setExpandableRowsBold(true);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePane(), BorderLayout.CENTER);
        add(selectionSumLabel, BorderLayout.SOUTH);

        setupEvents();
        updateSelectionSum();
    }

    private JPanel createHeaderPanel() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        toolbarPanel.setOpaque(false);

        btnAdd.setToolTipText(type.getLabel() + " 추가");
        btnEdit.setToolTipText(type.getLabel() + " 수정");
        btnDelete.setToolTipText(type.getLabel() + " 삭제");
        btnView.setToolTipText(type.getLabel() + " 상세 보기");
        btnView.setVisible(false);

        toolbarPanel.add(btnAdd);
        toolbarPanel.add(btnEdit);
        toolbarPanel.add(btnDelete);
        toolbarPanel.add(btnView);

        // 제목 옆의 모두 펼치기/접기는 보기만 바꾸므로 마감된 회계 연도에서도 쓸 수 있다(setEditable의 대상이 아니다).
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        titlePanel.add(new SectionHeaderLabel(type.getLabel(), SwingConstants.LEFT), gbc);
        gbc.insets = new Insets(0, 4, 0, 0);
        titlePanel.add(new ExpandCollapseAllButton(table, ComponentSize.SMALL), gbc);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(toolbarPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JScrollPane createTablePane() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        // 예산명 칸의 렌더러는 TreeTable이 정한다.
        table.getColumnModel()
                .getColumn(BudgetRecordTableModel.COLUMN_NAME)
                .setPreferredWidth(130);
        setupColumn(BudgetRecordTableModel.COLUMN_DESCRIPTION, 70, new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(BudgetRecordTableModel.COLUMN_AMOUNT, 70, new AmountCellRenderer());
        setupColumn(BudgetRecordTableModel.COLUMN_REMARKS, 50, new PaddedCellRenderer(SwingConstants.LEADING));

        // 가로 스크롤 없이 컬럼 폭을 테이블 폭에 맞춘다.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        return scrollPane;
    }

    private void setupColumn(int column, int preferredWidth, TableCellRenderer renderer) {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(column)
                .setPreferredWidth(preferredWidth);
        columnModel.getColumn(column)
                .setCellRenderer(renderer);
    }

    private void setupEvents() {
        table.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        updateSelectionSum();
                    }
                });

        table.setRowDoubleClickHandler(this::onRowDoubleClicked);

        btnAdd.addActionListener(_ -> onAdd());
        btnEdit.addActionListener(_ -> onEdit());
        btnDelete.addActionListener(_ -> onDelete());
        btnView.addActionListener(_ -> onView());
    }

    /**
     * 내역을 추가, 수정, 삭제할 수 있는지 정한다. 마감된 회계 연도의 내역은 조회만 할 수 있으므로 false로 둔다.
     * 편집할 수 없으면 추가/수정/삭제 버튼을 숨기고 상세 보기 버튼을 보인다.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        btnAdd.setVisible(editable);
        btnEdit.setVisible(editable);
        btnDelete.setVisible(editable);
        btnView.setVisible(!editable);
    }

    /**
     * 행이 하나만 선택되어 있으면 그 행의 예산을 추가 다이얼로그에 미리 채운다.
     */
    private void onAdd() {
        List<BudgetRecordRow> selected = getSelectedRows();
        if (selected.size() == 1) {
            BudgetRecordRow row = selected.getFirst();
            actionHandler.addRecord(type, row.getBudgetId(), row.getBudgetName());
            return;
        }
        actionHandler.addRecord(type, null, null);
    }

    private void onEdit() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.size() != 1) {
            JOptionPane.showMessageDialog(this, "수정할 " + type.getLabel() + " 내역을 한 건만 선택해주세요.");
            return;
        }
        actionHandler.editRecord(selected.getFirst());
    }

    private void onView() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.size() != 1) {
            JOptionPane.showMessageDialog(this, "상세 보기할 " + type.getLabel() + " 내역을 한 건만 선택해주세요.");
            return;
        }
        actionHandler.viewRecord(selected.getFirst());
    }

    private void onDelete() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제할 " + type.getLabel() + " 내역을 선택해주세요.");
            return;
        }
        actionHandler.deleteRecords(selected);
    }

    /**
     * 선택된 행들 중 예산 행을 제외한 수입/지출 내역을 반환한다.
     */
    private List<FinancialRecordResponseDto> getSelectedRecords() {
        return getSelectedRows().stream()
                .filter(row -> !row.isBudgetRow())
                .map(BudgetRecordRow::getRecord)
                .toList();
    }

    /**
     * 내역 행을 더블클릭하면 수정한다. 편집할 수 없으면(마감된 회계 연도) 상세 보기를 연다.
     * 예산 행은 더블클릭해도 선택만 된다.
     */
    private void onRowDoubleClicked(int modelRow) {
        if (tableModel.getRowValue(modelRow)
                .isBudgetRow()) {
            return;
        }
        if (editable) {
            onEdit();
        } else {
            onView();
        }
    }

    /**
     * 표시할 수입/지출 내역을 설정한다. 이 패널의 구분(수입 또는 지출)에 해당하는 내역만 넘겨야 한다.
     *
     * @param budgetPositions 예산 ID별 표시 위치
     */
    public void setRecords(List<FinancialRecordResponseDto> records, Map<Long, Integer> budgetPositions) {
        tableModel.setRecords(records, budgetPositions);
        updateSelectionSum();
    }

    /**
     * 내역이 표에 있으면 그 예산 행을 펼치고 내역 행을 선택해 보이도록 스크롤한다.
     */
    public void revealRecord(Long recordId) {
        int modelRow = tableModel.revealRecord(recordId);
        if (modelRow >= 0) {
            table.selectAndScrollToModelRow(modelRow);
        }
    }

    /**
     * 선택된 행들을 반환한다. 예산 행과 내역 행이 섞여 있을 수 있다.
     */
    public List<BudgetRecordRow> getSelectedRows() {
        List<BudgetRecordRow> selected = new ArrayList<>();
        for (int modelRow : getSelectedModelRows()) {
            selected.add(tableModel.getRowValue(modelRow));
        }
        return selected;
    }

    private int[] getSelectedModelRows() {
        int[] viewRows = table.getSelectedRows();
        int[] modelRows = new int[viewRows.length];
        for (int i = 0; i < viewRows.length; i++) {
            modelRows[i] = table.convertRowIndexToModel(viewRows[i]);
        }
        return modelRows;
    }

    private void updateSelectionSum() {
        long sum = tableModel.sumAmounts(getSelectedModelRows());
        selectionSumLabel.setText("선택 합계 : " + NumberUtils.toKoreanAmount(sum));
    }
}
