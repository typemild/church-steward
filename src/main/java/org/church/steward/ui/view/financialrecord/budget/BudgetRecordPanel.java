package org.church.steward.ui.view.financialrecord.budget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.programsetting.entity.BudgetViewTable;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.entity.ReportColumn;
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
 * "예산별 보기" 탭의 수입 또는 지출 예산 목록. 조회된 내역이 속한 예산만 보여주며, 예산 행을 펼치면 그 예산의 내역들이 나타난다.
 */
public class BudgetRecordPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final FinancialRecordType type;
    /** 칸 설정의 규칙과 칸 이름 */
    private final BudgetViewTable viewTable;
    private final BudgetRecordTableModel tableModel;
    private final TreeTable table;
    /** 트리 칸을 뺀 칸들. 칸 설정에 따라 표의 칸 모델에서 빼거나 순서를 바꿔 다시 넣는다. */
    private final Map<ReportColumn, TableColumn> recordColumns = new EnumMap<>(ReportColumn.class);
    /** 지금 표에 적용한 칸 설정 */
    private List<ReportColumnDto> appliedColumns;

    /** 예산 일괄 변경 버튼. 선택된 내역이 없으면 꺼진다. */
    private final IconButton btnChangeBudget = new IconButton(FontAwesomeSolid.COINS, ComponentSize.SMALL);
    /** 관계자 일괄 변경 버튼. 선택된 내역이 없으면 꺼진다. */
    private final IconButton btnChangeContacts = new IconButton(FontAwesomeSolid.USER_EDIT, ComponentSize.SMALL);
    /** 일괄 변경 버튼과 추가 버튼 사이의 간격 */
    private final JSeparator bulkChangeSeparator1 = new JSeparator(SwingConstants.VERTICAL);
    private final JSeparator bulkChangeSeparator2 = new JSeparator(SwingConstants.VERTICAL);
    private final IconButton btnAdd = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
    private final IconButton btnEdit = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    private final IconButton btnDelete = new IconButton(FontAwesomeSolid.TRASH_ALT, ComponentSize.SMALL);
    /** 상세 보기 버튼. 편집할 수 없을 때(마감된 회계 연도) 일괄 변경/추가/수정/삭제 버튼 대신 보인다. */
    private final IconButton btnView = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    /** 칸 설정 버튼. 설정은 프로그램 설정에 있으므로 누르면 그 화면으로 이동할지 묻는다. */
    private final IconButton btnColumnSettings = new IconButton(FontAwesomeSolid.COG, ComponentSize.SMALL);
    private boolean editable = true;
    private final FieldLabel selectionSumLabel = new FieldLabel("", SwingConstants.RIGHT, ComponentSize.MEDIUM);

    private final FinancialRecordActionHandler actionHandler;

    public BudgetRecordPanel(FinancialRecordType type, FinancialRecordActionHandler actionHandler) {
        this.type = type;
        this.viewTable = BudgetViewTable.of(type);
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
        updateBulkChangeButtons();
    }

    private JPanel createHeaderPanel() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        toolbarPanel.setOpaque(false);

        btnChangeBudget.setToolTipText("예산 일괄 변경");
        btnChangeContacts.setToolTipText((type == FinancialRecordType.INCOME ? "봉헌자" : "수취인") + " 일괄 변경");
        btnAdd.setToolTipText(type.getLabel() + " 추가");
        btnEdit.setToolTipText(type.getLabel() + " 수정");
        btnDelete.setToolTipText(type.getLabel() + " 삭제");
        btnView.setToolTipText(type.getLabel() + " 상세 보기");
        btnView.setVisible(false);

        // 일괄 변경은 추가/수정/삭제와 구분되도록 예산 관리의 위치 변경 버튼처럼 왼쪽에 떨어뜨려 둔다.
        toolbarPanel.add(btnChangeBudget);
        toolbarPanel.add(btnChangeContacts);
        toolbarPanel.add(bulkChangeSeparator1);
        toolbarPanel.add(bulkChangeSeparator2);
        toolbarPanel.add(btnAdd);
        toolbarPanel.add(btnEdit);
        toolbarPanel.add(btnDelete);
        toolbarPanel.add(btnView);

        // 제목 옆의 모두 펼치기/접기와 칸 설정은 보기만 바꾸므로 마감된 회계 연도에서도 쓸 수 있다(setEditable의 대상이 아니다).
        btnColumnSettings.setToolTipText("칸 설정");
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        titlePanel.add(new SectionHeaderLabel(type.getLabel(), SwingConstants.LEFT), gbc);
        gbc.insets = new Insets(0, 4, 0, 0);
        titlePanel.add(new ExpandCollapseAllButton(table, ComponentSize.SMALL), gbc);
        titlePanel.add(btnColumnSettings, gbc);

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
        // 관계자가 첫 번째 칸이면 트리 칸에 적으므로 이 칸은 첫 번째 칸이 아닐 때만 보인다. 이름은 짧아 가운데에 둔다.
        setupColumn(ReportColumn.CONTACT, 70, new PaddedCellRenderer(SwingConstants.CENTER));
        setupColumn(ReportColumn.DESCRIPTION, 70, new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(ReportColumn.AMOUNT, 70, new AmountCellRenderer());
        setupColumn(ReportColumn.REMARKS, 50, new PaddedCellRenderer(SwingConstants.LEADING));
        // 칸 설정을 읽기 전에는 기본 순서로 둔다.
        setColumns(viewTable.getDefaultColumns()
                .stream()
                .map(column -> ReportColumnDto.builder()
                        .column(column)
                        .hidden(false)
                        .build())
                .toList());

        // 가로 스크롤 없이 컬럼 폭을 테이블 폭에 맞춘다.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        return scrollPane;
    }

    /**
     * 칸의 폭과 렌더러를 정하고, 칸 설정에 따라 빼거나 다시 넣을 수 있게 잡아 둔다. 칸을 표에서 빼도 폭과 렌더러는 이 객체에 남는다.
     */
    private void setupColumn(ReportColumn column, int preferredWidth, TableCellRenderer renderer) {
        TableColumn tableColumn = table.getColumnModel()
                .getColumn(table.convertColumnIndexToView(BudgetRecordTableModel.columnIndexOf(column)));
        tableColumn.setPreferredWidth(preferredWidth);
        tableColumn.setCellRenderer(renderer);
        tableColumn.setHeaderValue(viewTable.labelOf(column));
        recordColumns.put(column, tableColumn);
    }

    /**
     * 칸의 순서와 숨김을 정한다. 첫 번째 칸은 따로 두지 않고 트리 칸에 날짜와 함께 적으며, 나머지 칸은 정한 순서대로 트리 칸 뒤에 두고
     * 숨긴 칸은 뺀다. 설정이 없거나 지금과 같으면 그대로 둔다.
     */
    public void setColumns(List<ReportColumnDto> columns) {
        if (columns == null || columns.isEmpty() || columns.equals(appliedColumns)) {
            return;
        }
        appliedColumns = columns;

        ReportColumn first = columns.getFirst()
                .getColumn();
        tableModel.setFirstColumn(first);

        TableColumnModel columnModel = table.getColumnModel();
        for (TableColumn tableColumn : recordColumns.values()) {
            columnModel.removeColumn(tableColumn);
        }
        // 트리 칸만 남았으므로 나머지 칸을 차례로 붙이면 트리 칸 뒤에 정한 순서대로 놓인다.
        for (ReportColumnDto column : columns.subList(1, columns.size())) {
            TableColumn tableColumn = recordColumns.get(column.getColumn());
            if (!column.isHidden() && tableColumn != null) {
                columnModel.addColumn(tableColumn);
            }
        }
        columnModel.getColumn(table.convertColumnIndexToView(BudgetRecordTableModel.COLUMN_NAME))
                .setHeaderValue("일자 · " + viewTable.labelOf(first));
        table.getTableHeader()
                .repaint();
    }

    private void setupEvents() {
        table.getSelectionModel()
                .addListSelectionListener(e -> {
                    if (!e.getValueIsAdjusting()) {
                        updateSelectionSum();
                        updateBulkChangeButtons();
                    }
                });

        table.setRowDoubleClickHandler(this::onRowDoubleClicked);

        btnChangeBudget.addActionListener(_ -> actionHandler.changeBudget(getSelectedRecords()));
        btnChangeContacts.addActionListener(_ -> actionHandler.changeContacts(getSelectedRecords()));
        btnAdd.addActionListener(_ -> onAdd());
        btnEdit.addActionListener(_ -> onEdit());
        btnDelete.addActionListener(_ -> onDelete());
        btnView.addActionListener(_ -> onView());
        btnColumnSettings.addActionListener(_ -> actionHandler.showColumnSettings());
    }

    /**
     * 내역을 추가, 수정, 삭제할 수 있는지 정한다. 마감된 회계 연도의 내역은 조회만 할 수 있으므로 false로 둔다. 편집할 수 없으면
     * 일괄 변경/추가/수정/삭제 버튼과 그 사이의 간격을 숨기고 상세 보기 버튼을 보인다.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        btnChangeBudget.setVisible(editable);
        btnChangeContacts.setVisible(editable);
        bulkChangeSeparator1.setVisible(editable);
        bulkChangeSeparator2.setVisible(editable);
        btnAdd.setVisible(editable);
        btnEdit.setVisible(editable);
        btnDelete.setVisible(editable);
        btnView.setVisible(!editable);
    }

    /**
     * 일괄 변경 버튼은 내역이 한 건 이상 선택되었을 때만 켠다. 예산 행만 선택된 경우는 선택된 내역이 없는 것으로 본다.
     */
    private void updateBulkChangeButtons() {
        boolean hasRecord = !getSelectedRecords().isEmpty();
        btnChangeBudget.setEnabled(hasRecord);
        btnChangeContacts.setEnabled(hasRecord);
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
     * 내역 행을 더블클릭하면 수정한다. 편집할 수 없으면(마감된 회계 연도) 상세 보기를 연다. 예산 행은 더블클릭해도 선택만 된다.
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
        updateBulkChangeButtons();
    }

    /**
     * 행 선택을 해제한다.
     */
    public void clearSelection() {
        table.clearRowSelection();
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
