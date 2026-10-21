package org.church.steward.ui.view.budget;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.dto.BudgetDto;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.table.AmountCellRenderer;
import org.church.steward.ui.component.table.ExpandCollapseAllButton;
import org.church.steward.ui.component.table.TreeTable;
import org.church.steward.ui.component.table.TreeTableNode;
import org.church.steward.ui.util.AsyncTask;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 예산 테이블을 포함하는 패널.
 * 예산 관리 화면에서는 예산마다 진행 현황(수입액/집행액, 달성률/집행률)을 함께 보여주고 툴바와 더블클릭으로 편집한다.
 * 마감된 회계 연도처럼 편집을 막아야 할 때는 {@link #setEditable}로 툴바와 더블클릭 수정을 끈다.
 * 읽기 전용 모드에서는 편집 툴바와 진행 현황 없이 예산 선택 용도로만 쓸 수 있다.
 */
@Slf4j
public class BudgetTreePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BudgetController budgetController;
    private List<BudgetDto> allDtos = new ArrayList<>();

    /**
     * 예산 관리 화면에서 쓰는 금액 칸과 비율 칸의 폭. 수입 예산과 지출 예산을 좌우로 나란히 놓으면 표 하나가 320px 남짓이어서,
     * 항목명에 폭을 남기도록 금액 칸은 억 단위("100,000,000")까지, 비율 칸은 "100.0%"까지 들어갈 만큼만 둔다.
     * 칸에 넘치는 금액은 툴팁으로 확인할 수 있다.
     */
    private static final int AMOUNT_COLUMN_WIDTH = 78;
    private static final int RATE_COLUMN_WIDTH = 54;

    private final String title;
    private final BudgetType type;
    private final boolean readOnly;
    private final TreeTable table;
    private final BudgetTableModel tableModel;
    /** 예산을 추가, 수정, 삭제하거나 순서를 바꾸는 툴바 버튼. 읽기 전용 모드에서는 비어 있다. */
    private final List<IconButton> editButtons = new ArrayList<>();
    private Long currentFiscalYearId;
    private boolean editable = true;
    private Long selectedIdToRestore = null;
    private Consumer<BudgetDto> selectionListener;

    public BudgetTreePanel(BudgetController budgetController, String title, BudgetType type) {
        this(budgetController, title, type, false);
    }

    public BudgetTreePanel(BudgetController budgetController, String title, BudgetType type, boolean readOnly) {
        this.budgetController = budgetController;
        this.title = title;
        this.type = type;
        this.readOnly = readOnly;

        setLayout(new BorderLayout());
        setOpaque(false);

        // 테이블 설정 (진행 현황은 예산을 관리할 때만 보여준다)
        tableModel = new BudgetTableModel(type, !readOnly);
        table = new TreeTable(tableModel, BudgetTableModel.COLUMN_NAME);
        table.setLeafIcon(FontAwesomeSolid.CHART_PIE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableColumnModel columns = table.getColumnModel();
        columns.getColumn(BudgetTableModel.COLUMN_AMOUNT).setCellRenderer(new AmountCellRenderer());
        if (!readOnly) {
            // 항목명 칸이 남는 폭을 모두 쓰도록 금액과 비율 칸은 폭을 고정한다.
            setFixedWidth(columns.getColumn(BudgetTableModel.COLUMN_AMOUNT), AMOUNT_COLUMN_WIDTH);
            setFixedWidth(columns.getColumn(BudgetTableModel.COLUMN_ACTUAL_AMOUNT), AMOUNT_COLUMN_WIDTH);
            columns.getColumn(BudgetTableModel.COLUMN_ACTUAL_AMOUNT).setCellRenderer(new AmountCellRenderer());
            setFixedWidth(columns.getColumn(BudgetTableModel.COLUMN_RATE), RATE_COLUMN_WIDTH);
            // 칸이 좁으므로 금액 칸과 같이 안쪽 여백 없이 오른쪽에 붙여 적는다.
            DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer();
            rateRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
            columns.getColumn(BudgetTableModel.COLUMN_RATE).setCellRenderer(rateRenderer);
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionListener != null) {
                selectionListener.accept(getSelectedDto());
            }
        });

        if (!readOnly) {
            table.setRowDoubleClickHandler(_ -> {
                if (editable) {
                    onEdit();
                }
            });
        }

        JScrollPane tableScrollPane = new JScrollPane(table);
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    /**
     * 제목과 모두 펼치기/접기 버튼, 편집 툴바(읽기 전용 모드가 아닐 때)를 담은 머리 영역을 만든다.
     * 모두 펼치기/접기는 보기만 바꾸므로 읽기 전용 모드나 마감된 회계 연도에서도 쓸 수 있다.
     */
    private JPanel createHeaderPanel() {
        // 제목과 버튼을 머리 영역의 세로 가운데에 나란히 둔다.
        JPanel titlePanel = new JPanel(new GridBagLayout());
        titlePanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        titlePanel.add(new SectionHeaderLabel(title, SwingConstants.LEFT), gbc);
        gbc.insets = new Insets(0, 4, 0, 0);
        titlePanel.add(new ExpandCollapseAllButton(table, ComponentSize.SMALL), gbc);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        if (!readOnly) {
            // 상단 아이콘 툴바
            JPanel toolbarPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 5));
            toolbarPanel.setOpaque(false);

            IconButton btnAdd = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
            IconButton btnEdit = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
            IconButton btnDelete = new IconButton(FontAwesomeSolid.TRASH_ALT, ComponentSize.SMALL);
            IconButton btnUp = new IconButton(FontAwesomeSolid.ARROW_UP, ComponentSize.SMALL);
            IconButton btnDown = new IconButton(FontAwesomeSolid.ARROW_DOWN, ComponentSize.SMALL);

            btnAdd.setToolTipText("새 예산 추가");
            btnEdit.setToolTipText("예산 수정");
            btnDelete.setToolTipText("예산 삭제");
            btnUp.setToolTipText("위로 이동");
            btnDown.setToolTipText("아래로 이동");

            btnAdd.addActionListener(_ -> onAdd());
            btnEdit.addActionListener(_ -> onEdit());
            btnDelete.addActionListener(_ -> onDelete());
            btnUp.addActionListener(_ -> onMove(-1));
            btnDown.addActionListener(_ -> onMove(1));

            toolbarPanel.add(btnUp);
            toolbarPanel.add(btnDown);
            toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
            toolbarPanel.add(new JSeparator(SwingConstants.VERTICAL));
            toolbarPanel.add(btnAdd);
            toolbarPanel.add(btnEdit);
            toolbarPanel.add(btnDelete);

            editButtons.addAll(List.of(btnUp, btnDown, btnAdd, btnEdit, btnDelete));

            headerPanel.add(toolbarPanel, BorderLayout.EAST);
        }
        return headerPanel;
    }

    private void setFixedWidth(TableColumn column, int width) {
        column.setMinWidth(width);
        column.setMaxWidth(width);
    }

    /**
     * 예산을 추가, 수정, 삭제하거나 순서를 바꿀 수 있는지 정한다. 마감된 회계 연도의 예산은 조회만 할 수 있으므로 false로 둔다.
     * 읽기 전용 모드에서는 아무 효과가 없다.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        for (IconButton button : editButtons) {
            button.setEnabled(editable);
        }
    }

    /**
     * 회계 연도의 예산을 불러온다. 읽기 전용 모드가 아니면 진행 현황도 함께 조회한다.
     */
    public void loadData(Long fiscalYearId) {
        this.currentFiscalYearId = fiscalYearId;
        AsyncTask.run(this,
                () -> readOnly ? budgetController.getBudgetsByFiscalYearAndType(fiscalYearId, type)
                        : budgetController.getBudgetsWithActualAmount(fiscalYearId, type),
                dtos -> flattenAndPopulate(dtos),
                err -> {
                    selectedIdToRestore = null;
                    log.error("Failed to load {} data", title, err);
                    JOptionPane.showMessageDialog(this, title + " 데이터를 불러오는데 실패했습니다.");
                });
    }

    private void flattenAndPopulate(List<BudgetDto> dtos) {
        this.allDtos = dtos;
        this.tableModel.setRoots(buildTree(dtos));

        if (selectedIdToRestore != null) {
            restoreSelection(selectedIdToRestore);
            selectedIdToRestore = null;
        }
    }

    private void restoreSelection(Long id) {
        int modelRow = tableModel.indexOfKey(id);
        if (modelRow >= 0) {
            int viewRow = table.convertRowIndexToView(modelRow);
            table.setRowSelectionInterval(viewRow, viewRow);
        }
    }

    /**
     * 예산 목록을 상위/하위 관계의 트리로 만든다. 같은 상위 예산 아래에서는 정렬 순서를 따른다.
     */
    private List<TreeTableNode<BudgetDto>> buildTree(List<BudgetDto> dtos) {
        Map<Long, List<BudgetDto>> childrenMap = new HashMap<>();
        List<BudgetDto> roots = new ArrayList<>();

        for (BudgetDto dto : dtos) {
            if (dto.getParentId() == null) {
                roots.add(dto);
            } else {
                childrenMap.computeIfAbsent(dto.getParentId(), _ -> new ArrayList<>()).add(dto);
            }
        }

        roots.sort(Comparator.comparingInt(BudgetDto::getSortOrder));
        for (List<BudgetDto> children : childrenMap.values()) {
            children.sort(Comparator.comparingInt(BudgetDto::getSortOrder));
        }
        return toNodes(roots, childrenMap);
    }

    private List<TreeTableNode<BudgetDto>> toNodes(List<BudgetDto> dtos, Map<Long, List<BudgetDto>> childrenMap) {
        List<TreeTableNode<BudgetDto>> nodes = new ArrayList<>();
        for (BudgetDto dto : dtos) {
            List<BudgetDto> children = childrenMap.getOrDefault(dto.getId(), List.of());
            nodes.add(new TreeTableNode<>(dto.getId(), dto, toNodes(children, childrenMap)));
        }
        return nodes;
    }

    private void onAdd() {
        BudgetDto selectedDto = getSelectedDto();
        Long parentId = (selectedDto != null) ? selectedDto.getId() : null;
        String parentPath = (selectedDto != null) ? getIncludingPath(selectedDto) : "(없음)";

        BudgetEntryDialog dialog = new BudgetEntryDialog(
                SwingUtilities.getWindowAncestor(this),
                "새 예산 추가",
                "",
                0,
                parentPath,
                true);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final String name = dialog.getName();
        final long amount = dialog.getAmount();

        AsyncTask.run(this,
                () -> {
                    budgetController.createBudget(currentFiscalYearId, parentId, name, amount, type);
                    return null;
                },
                _ -> loadData(currentFiscalYearId),
                err -> {
                    log.error("Failed to add budget", err);
                    JOptionPane.showMessageDialog(this, "추가 실패: " + err.getMessage());
                });
    }

    private void onEdit() {
        BudgetDto selectedDto = getSelectedDto();
        if (selectedDto == null)
            return;

        boolean isLeaf = isLeaf(selectedDto);
        String parentPath = (selectedDto.getParentId() != null) ? getParentPath(selectedDto) : "(없음)";

        long amountToShow;
        boolean amountEditable;

        if (isLeaf) {
            amountToShow = selectedDto.getAmount();
            amountEditable = true;
        } else {
            amountToShow = selectedDto.getTotalAmount();
            amountEditable = false;
        }

        BudgetEntryDialog dialog = new BudgetEntryDialog(
                SwingUtilities.getWindowAncestor(this),
                "예산 수정",
                selectedDto.getName(),
                amountToShow,
                parentPath,
                amountEditable);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final String newName = dialog.getName();
        final long newAmount = dialog.getAmount();

        AsyncTask.run(this,
                () -> {
                    budgetController.updateBudget(selectedDto.getId(), newName, newAmount);
                    return null;
                },
                _ -> {
                    selectedIdToRestore = selectedDto.getId();
                    loadData(currentFiscalYearId);
                },
                err -> {
                    log.error("Failed to edit budget", err);
                    JOptionPane.showMessageDialog(this, "수정 실패: " + err.getMessage());
                });
    }

    private void onDelete() {
        BudgetDto selectedDto = getSelectedDto();
        if (selectedDto == null) {
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, selectedDto.getName() + " 항목을 삭제하시겠습니까?", "삭제 확인",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        AsyncTask.run(this,
                () -> {
                    budgetController.deleteBudget(selectedDto.getId());
                    return null;
                },
                _ -> loadData(currentFiscalYearId),
                err -> {
                    log.error("Failed to delete budget", err);
                    JOptionPane.showMessageDialog(this, "삭제 실패: " + err.getMessage());
                });
    }

    private void onMove(int direction) {
        BudgetDto selectedDto = getSelectedDto();
        if (selectedDto == null)
            return;

        AsyncTask.run(this,
                () -> {
                    if (direction > 0) {
                        budgetController.moveBudgetDown(selectedDto.getId());
                    } else {
                        budgetController.moveBudgetUp(selectedDto.getId());
                    }
                    return null;
                },
                _ -> {
                    selectedIdToRestore = selectedDto.getId();
                    loadData(currentFiscalYearId);
                },
                err -> {
                    log.error("Failed to move budget", err);
                    JOptionPane.showMessageDialog(this, "이동 실패: " + err.getMessage());
                });
    }

    private BudgetDto getSelectedDto() {
        int viewRow = table.getSelectedRow();
        if (viewRow == -1) {
            return null;
        }
        return tableModel.getRowValue(table.convertRowIndexToModel(viewRow));
    }

    private boolean isLeaf(BudgetDto dto) {
        return allDtos.stream()
                .noneMatch(d -> dto.getId().equals(d.getParentId()));
    }

    /**
     * 행 선택이 바뀔 때 호출할 리스너를 등록한다. 선택이 해제되면 null이 전달된다.
     */
    public void setSelectionListener(Consumer<BudgetDto> selectionListener) {
        this.selectionListener = selectionListener;
    }

    /**
     * 현재 선택된 예산을 반환한다. 선택된 행이 없으면 null을 반환한다.
     */
    public BudgetDto getSelectedBudget() {
        return getSelectedDto();
    }

    /**
     * 현재 선택된 예산이 하위 예산이 없는 말단 예산인지 확인한다.
     */
    public boolean isSelectedBudgetLeaf() {
        BudgetDto selectedDto = getSelectedDto();
        return selectedDto != null && isLeaf(selectedDto);
    }

    /**
     * 현재 선택된 예산의 전체 예산명("예산A > 예산B > 예산C")을 반환한다. 선택된 행이 없으면 null을 반환한다.
     */
    public String getSelectedBudgetPath() {
        BudgetDto selectedDto = getSelectedDto();
        return selectedDto != null ? getIncludingPath(selectedDto) : null;
    }

    public void clearSelection() {
        table.clearRowSelection();
    }

    /**
     * 예산 표를 반환한다. 예산을 고르는 다이얼로그가 표의 키 동작(Enter)을 바꿀 때 쓴다.
     */
    public JComponent getTable() {
        return table;
    }

    private String getParentPath(BudgetDto dto) {
        List<String> path = new ArrayList<>();
        Long currentId = dto.getParentId();
        while (currentId != null) {
            final Long pid = currentId;
            BudgetDto current = allDtos.stream()
                    .filter(d -> d.getId().equals(pid))
                    .findFirst()
                    .orElse(null);
            if (current == null) {
                break;
            }
            path.add(current.getName());
            currentId = current.getParentId();
        }
        Collections.reverse(path);
        return String.join(" > ", path);
    }

    private String getIncludingPath(BudgetDto dto) {
        List<String> path = new ArrayList<>();
        BudgetDto current = dto;
        while (current != null) {
            path.add(current.getName());
            Long pid = current.getParentId();
            if (pid == null) {
                break;
            }
            final Long finalPid = pid;
            current = allDtos.stream()
                    .filter(d -> d.getId().equals(finalPid))
                    .findFirst()
                    .orElse(null);
        }
        Collections.reverse(path);
        return String.join(" > ", path);
    }

}
