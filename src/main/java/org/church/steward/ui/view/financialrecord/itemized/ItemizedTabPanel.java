package org.church.steward.ui.view.financialrecord.itemized;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;

import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.AmountCellRenderer;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.component.table.PaddedCellRenderer;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.FinancialRecordActionHandler;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * "건별 보기" 탭. 검색된 수입/지출 내역을 날짜 오름차순으로 보여주며, 컬럼 헤더를 클릭해 정렬을 바꿀 수 있다.
 */
public class ItemizedTabPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String MIXED_TYPE_TOOLTIP = "수입과 지출이 함께 선택되어 일괄 변경할 수 없습니다.";

    private final ItemizedTableModel tableModel = new ItemizedTableModel();
    private final ListTable table;

    private final SubFieldLabel totalCountLabel = new SubFieldLabel("", ComponentSize.SMALL);
    // 툴바 버튼. 화면의 왼쪽부터 놓이는 순서대로 적는다.
    /** 예산 일괄 변경 버튼. 선택된 내역이 없거나 수입과 지출이 함께 선택되면 꺼진다. */
    private final IconButton btnChangeBudget = new IconButton(FontAwesomeSolid.COINS, ComponentSize.SMALL);
    /** 관계자 일괄 변경 버튼. 선택된 내역이 없거나 수입과 지출이 함께 선택되면 꺼진다. */
    private final IconButton btnChangeContacts = new IconButton(FontAwesomeSolid.USER_EDIT, ComponentSize.SMALL);
    /** 일괄 변경 버튼과 추가 버튼 사이의 간격 */
    private final JSeparator bulkChangeSeparator1 = new JSeparator(SwingConstants.VERTICAL);
    private final JSeparator bulkChangeSeparator2 = new JSeparator(SwingConstants.VERTICAL);
    private final IconButton btnAdd = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
    private final IconButton btnEdit = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    private final IconButton btnDelete = new IconButton(FontAwesomeSolid.TRASH_ALT, ComponentSize.SMALL);
    /** 상세 보기 버튼. 편집할 수 없을 때(마감된 회계 연도) 일괄 변경/추가/수정/삭제 버튼 대신 보인다. */
    private final IconButton btnView = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
    private boolean editable = true;
    private final FieldLabel selectionSumLabel = new FieldLabel("", SwingConstants.RIGHT, ComponentSize.MEDIUM);

    private final FinancialRecordActionHandler actionHandler;

    public ItemizedTabPanel(FinancialRecordActionHandler actionHandler) {
        this.actionHandler = actionHandler;

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setOpaque(false);

        table = new ListTable(tableModel);

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createTablePane(), BorderLayout.CENTER);
        add(selectionSumLabel, BorderLayout.SOUTH);

        setupEvents();
        updateTotalCount();
        updateSelectionSum();
        updateBulkChangeButtons();
    }

    private JPanel createHeaderPanel() {
        JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        toolbarPanel.setOpaque(false);

        btnAdd.setToolTipText("수입/지출 추가");
        btnEdit.setToolTipText("수입/지출 수정");
        btnDelete.setToolTipText("수입/지출 삭제");
        btnView.setToolTipText("수입/지출 상세 보기");
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

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(totalCountLabel, BorderLayout.WEST);
        headerPanel.add(toolbarPanel, BorderLayout.EAST);
        return headerPanel;
    }

    private JScrollPane createTablePane() {
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        setupColumn(ItemizedTableModel.COLUMN_DATE, 120, new PaddedCellRenderer(SwingConstants.CENTER));
        setupColumn(ItemizedTableModel.COLUMN_TYPE, 45, new PaddedCellRenderer(SwingConstants.CENTER));
        setupColumn(ItemizedTableModel.COLUMN_BUDGET, 140, new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(ItemizedTableModel.COLUMN_CONTACT, 100, new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(ItemizedTableModel.COLUMN_DESCRIPTION, 80, new PaddedCellRenderer(SwingConstants.LEADING));
        setupColumn(ItemizedTableModel.COLUMN_AMOUNT, 85, new AmountCellRenderer());
        setupColumn(ItemizedTableModel.COLUMN_REMARKS, 80, new PaddedCellRenderer(SwingConstants.LEADING));

        // 컬럼 헤더를 클릭하면 그 컬럼으로 정렬한다. 기본은 "예산별 보기"와 같은 날짜 오름차순이다.
        // 정렬 기준을 하나만 유지해서, 값이 같은 행은 이전 정렬 컬럼이 아니라 모델 순서(ID 내림차순)를 따르게 한다.
        // 날짜 칸은 값에 ID가 들어 있어 같은 날짜의 내역도 정렬 방향을 따른다(ItemizedTableModel.RecordDate).
        TableRowSorter<ItemizedTableModel> sorter = new TableRowSorter<>(tableModel);
        sorter.setMaxSortKeys(1);
        sorter.setSortKeys(List.of(new RowSorter.SortKey(ItemizedTableModel.COLUMN_DATE, SortOrder.ASCENDING)));
        table.setRowSorter(sorter);

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
                        updateBulkChangeButtons();
                    }
                });

        // 편집할 수 있으면 수정, 아니면 상세 보기를 연다.
        table.setRowDoubleClickHandler(_ -> {
            if (editable) {
                onEdit();
            } else {
                onView();
            }
        });

        // 구분은 추가 다이얼로그에서 고른다.
        btnChangeBudget.addActionListener(_ -> actionHandler.changeBudget(getSelectedRecords()));
        btnChangeContacts.addActionListener(_ -> actionHandler.changeContacts(getSelectedRecords()));
        btnAdd.addActionListener(_ -> actionHandler.addRecord(null, null, null));
        btnEdit.addActionListener(_ -> onEdit());
        btnDelete.addActionListener(_ -> onDelete());
        btnView.addActionListener(_ -> onView());
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
     * 일괄 변경 버튼은 같은 구분의 내역이 한 건 이상 선택되었을 때만 켠다. 예산과 관계자의 규칙이 구분마다 다르므로 수입과 지출이 함께
     * 선택되면 끄고, 그 이유를 툴팁으로 알린다.
     */
    private void updateBulkChangeButtons() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        boolean mixed = selected.stream()
                .map(FinancialRecordResponseDto::getType)
                .distinct()
                .count() > 1;
        boolean enabled = !selected.isEmpty() && !mixed;
        btnChangeBudget.setEnabled(enabled);
        btnChangeContacts.setEnabled(enabled);
        btnChangeBudget.setToolTipText(mixed ? MIXED_TYPE_TOOLTIP : "예산 일괄 변경");
        btnChangeContacts.setToolTipText(mixed ? MIXED_TYPE_TOOLTIP : "관계자 일괄 변경");
    }

    private void onEdit() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.size() != 1) {
            JOptionPane.showMessageDialog(this, "수정할 내역을 한 건만 선택해주세요.");
            return;
        }
        actionHandler.editRecord(selected.getFirst());
    }

    private void onView() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.size() != 1) {
            JOptionPane.showMessageDialog(this, "상세 보기할 내역을 한 건만 선택해주세요.");
            return;
        }
        actionHandler.viewRecord(selected.getFirst());
    }

    private void onDelete() {
        List<FinancialRecordResponseDto> selected = getSelectedRecords();
        if (selected.isEmpty()) {
            JOptionPane.showMessageDialog(this, "삭제할 내역을 선택해주세요.");
            return;
        }
        actionHandler.deleteRecords(selected);
    }

    /**
     * 테이블에 표시할 수입/지출 내역을 설정한다.
     */
    public void setRecords(List<FinancialRecordResponseDto> records) {
        tableModel.setData(records);
        updateTotalCount();
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
     * 내역이 표에 있으면 그 행을 선택하고 보이도록 스크롤한다.
     */
    public void revealRecord(Long recordId) {
        int modelRow = tableModel.indexOfRecord(recordId);
        if (modelRow >= 0) {
            table.selectAndScrollToModelRow(modelRow);
        }
    }

    /**
     * 선택된 행들의 수입/지출 내역을 반환한다.
     */
    public List<FinancialRecordResponseDto> getSelectedRecords() {
        List<FinancialRecordResponseDto> selected = new ArrayList<>();
        for (int viewRow : table.getSelectedRows()) {
            selected.add(tableModel.getRecordAt(table.convertRowIndexToModel(viewRow)));
        }
        return selected;
    }

    private void updateTotalCount() {
        totalCountLabel.setText("총 " + tableModel.getRowCount() + "건");
    }

    /**
     * 선택된 행들의 금액을 합산한다. 수입은 더하고 지출은 뺀다.
     */
    private void updateSelectionSum() {
        long sum = 0;
        for (FinancialRecordResponseDto record : getSelectedRecords()) {
            if (record.getType() == FinancialRecordType.INCOME) {
                sum += record.getAmount();
            } else {
                sum -= record.getAmount();
            }
        }
        selectionSumLabel.setText("선택 합계 : " + FinancialRecordFormats.signedAmount(sum));
    }
}
