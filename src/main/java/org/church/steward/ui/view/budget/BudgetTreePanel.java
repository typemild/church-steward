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
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
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
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 예산 테이블을 포함하는 패널. 예산 관리 화면에서는 예산마다 진행 현황(수입액/집행액, 달성률/집행률)을 함께 보여주고 툴바와 더블클릭으로
 * 편집한다. 마감된 회계 연도처럼 편집을 막아야 할 때는 {@link #setEditable}로 편집 툴바를 상세 보기 버튼으로 바꾸고,
 * 더블클릭도 상세 보기로 바꾼다. 읽기 전용 모드에서는 편집 툴바와 진행 현황 없이 예산 선택 용도로만 쓸 수 있다. 예산을 고르는
 * 다이얼로그는 {@link #setRowDoubleClickHandler}로 더블클릭을 수정 대신 선택 확인으로 바꾼다.
 */
@Slf4j
public class BudgetTreePanel extends JPanel {
    private static final long serialVersionUID = 1L;

    private final BudgetController budgetController;
    private List<BudgetDto> allDtos = new ArrayList<>();

    /**
     * 예산 관리 화면에서 폭을 내용에 맞추는 칸(예산, 수입액/집행액, 달성률/집행률). 수입 예산과 지출 예산을 좌우로 나란히 놓으면 표
     * 하나가 최소 370px 남짓이어서, 이 칸들은 머리글과 가장 긴 값이 들어갈 만큼만 두고 남는 폭은 항목명 칸이 모두 쓴다.
     */
    private static final int[] FITTED_COLUMNS = { BudgetTableModel.COLUMN_AMOUNT,
            BudgetTableModel.COLUMN_ACTUAL_AMOUNT, BudgetTableModel.COLUMN_RATE };
    /**
     * 내용에 맞추는 칸의 최대 폭(내용 폭의 배수). 사용자가 넓힐 수 있는 한도이자, 창을 넓혔을 때 이 칸들이 늘어나지 않게 하는 값이다.
     */
    private static final int FITTED_MAX_WIDTH_RATIO = 3;

    private final String title;
    private final BudgetType type;
    private final boolean readOnly;
    private final TreeTable table;
    private final BudgetTableModel tableModel;
    /** 예산을 추가, 수정, 삭제하거나 순서를 바꾸는 툴바 버튼과 그 사이의 구분선. 읽기 전용 모드에서는 비어 있다. */
    private final List<JComponent> editControls = new ArrayList<>();
    /** 상세 보기 버튼. 편집할 수 없을 때(마감된 회계 연도) 편집 툴바 대신 보인다. 읽기 전용 모드에서는 null이다. */
    private IconButton btnView;
    private Long currentFiscalYearId;
    private boolean editable = true;
    private Long selectedIdToRestore = null;
    private Consumer<BudgetDto> selectionListener;
    /** 예산 표 바로 아래에 붙이는 총계 줄. 진행 현황이 없는 읽기 전용 모드에서는 null이다. */
    private BudgetTotalTable totalTable;
    /** 총계 줄을 감싸 예산 표의 테두리에 이어 붙이는 패널. 기본으로 숨긴다. 읽기 전용 모드에서는 null이다. */
    private JPanel totalPane;
    /** 사용자가 머리글을 끌어 칸 폭을 바꿨는지 여부. 바꿨으면 예산을 다시 읽어도 칸 폭을 내용에 다시 맞추지 않는다. */
    private boolean columnsResizedByUser;

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
            columns.getColumn(BudgetTableModel.COLUMN_ACTUAL_AMOUNT).setCellRenderer(new AmountCellRenderer());
            // 칸이 좁으므로 안쪽 여백 없이 오른쪽에 붙여 적는다.
            DefaultTableCellRenderer rateRenderer = new DefaultTableCellRenderer();
            rateRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
            columns.getColumn(BudgetTableModel.COLUMN_RATE).setCellRenderer(rateRenderer);
            totalTable = new BudgetTotalTable(table);
            // 예산을 읽기 전에는 머리글에 맞춘다.
            fitColumnsToContent();
            // 머리글을 끄는 동안에만 끌고 있는 칸이 있으므로, 그때의 폭 변경을 사용자의 조정으로 본다.
            columns.addColumnModelListener(new TableColumnModelListener() {
                @Override
                public void columnMarginChanged(ChangeEvent e) {
                    if (table.getTableHeader()
                            .getResizingColumn() != null) {
                        columnsResizedByUser = true;
                    }
                }

                @Override
                public void columnAdded(TableColumnModelEvent e) {
                    // 칸 구성은 바뀌지 않는다.
                }

                @Override
                public void columnRemoved(TableColumnModelEvent e) {
                    // 칸 구성은 바뀌지 않는다.
                }

                @Override
                public void columnMoved(TableColumnModelEvent e) {
                    // 칸 순서는 바꿀 수 없다.
                }

                @Override
                public void columnSelectionChanged(ListSelectionEvent e) {
                    // 칸 선택은 쓰지 않는다.
                }
            });
        }
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && selectionListener != null) {
                selectionListener.accept(getSelectedDto());
            }
        });

        if (!readOnly) {
            // 편집할 수 있으면 수정, 아니면 상세 보기를 연다.
            table.setRowDoubleClickHandler(_ -> {
                if (editable) {
                    onEdit();
                } else {
                    onView();
                }
            });
        }

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setOpaque(false);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        if (totalTable != null) {
            // 예산 표의 테두리(위쪽 포함)에 이어지도록 위쪽 선 없이 감싼다. 예산 표의 아래쪽 선이 예산과 총계를 나눈다.
            totalPane = new JPanel(new BorderLayout());
            totalPane.setBackground(UIColors.TABLE_TOTAL_BACKGROUND);
            totalPane.setBorder(BorderFactory.createMatteBorder(0, 1, 1, 1, UIColors.BORDER));
            totalPane.add(totalTable, BorderLayout.CENTER);
            totalPane.setVisible(false);
            tablePanel.add(totalPane, BorderLayout.SOUTH);
        }
        add(createHeaderPanel(), BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
    }

    /**
     * 예산 표 바로 아래에 총계 줄(예산액, 수입액/집행액, 달성률/집행률)을 보일지 정한다. 예산 관리 화면만 켠다. 읽기 전용 모드에서는
     * 진행 현황이 없으므로 아무 효과가 없다.
     */
    public void setTotalVisible(boolean visible) {
        if (totalPane == null) {
            return;
        }
        totalPane.setVisible(visible);
        // 총계가 예산 하나보다 넓을 수 있으므로 칸 폭을 다시 잰다.
        fitColumnsToContent();
    }

    private boolean isTotalVisible() {
        return totalPane != null && totalPane.isVisible();
    }

    /**
     * 총계 줄을 갱신한다. 상위 예산의 예산액과 수입액(집행액)은 하위 예산까지 합한 값이므로 최상위 예산만 더한다.
     */
    private void updateTotal() {
        if (totalTable == null) {
            return;
        }
        long budgetedTotal = 0;
        long actualTotal = 0;
        for (BudgetDto dto : allDtos) {
            if (dto.getParentId() == null) {
                budgetedTotal += tableModel.getBudgetedAmount(dto);
                actualTotal += Objects.requireNonNullElse(dto.getActualAmount(), 0L);
            }
        }
        totalTable.setTotals(budgetedTotal, actualTotal);
    }

    /**
     * 제목과 모두 펼치기/접기 버튼, 편집 툴바(읽기 전용 모드가 아닐 때)를 담은 머리 영역을 만든다. 모두 펼치기/접기는 보기만 바꾸므로
     * 읽기 전용 모드나 마감된 회계 연도에서도 쓸 수 있다.
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

            btnView = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.SMALL);
            btnView.setToolTipText("예산 상세 보기");
            btnView.addActionListener(_ -> onView());
            btnView.setVisible(false);

            JSeparator separator1 = new JSeparator(SwingConstants.VERTICAL);
            JSeparator separator2 = new JSeparator(SwingConstants.VERTICAL);

            toolbarPanel.add(btnUp);
            toolbarPanel.add(btnDown);
            toolbarPanel.add(separator1);
            toolbarPanel.add(separator2);
            toolbarPanel.add(btnAdd);
            toolbarPanel.add(btnEdit);
            toolbarPanel.add(btnDelete);
            toolbarPanel.add(btnView);

            editControls.addAll(List.of(btnUp, btnDown, separator1, separator2, btnAdd, btnEdit, btnDelete));

            headerPanel.add(toolbarPanel, BorderLayout.EAST);
        }
        return headerPanel;
    }

    /**
     * 금액과 비율 칸의 폭을 머리글과 모든 예산의 값 중 가장 넓은 것에 맞추고, 남는 폭은 항목명 칸이 모두 쓰게 한다. 접힌 예산의 값도
     * 재므로 펼치거나 접어도 폭이 바뀌지 않는다. 창을 넓혀도 금액이 잘리지 않고, 금액이 작으면 항목명에 폭을 더 남긴다. 총계 줄이 보이면
     * 총계도 함께 잰다.
     * <p>
     * 사용자는 머리글의 경계를 끌어 칸 폭을 바꿀 수 있다(머리글 폭까지 줄일 수 있다). 한 번 바꾼 뒤에는 예산을 다시 읽어도 사용자가 정한
     * 폭을 그대로 둔다. 표는 늘어난 폭을 칸마다 최대 폭까지 남은 여유에 비례해 나누므로, 금액과 비율 칸의 최대 폭을 내용 폭의 몇 배로
     * 묶어 두면 창을 넓혀도 이 칸들은 사실상 늘어나지 않고 최대 폭이 없는 항목명 칸이 늘어난 폭을 가져간다.
     */
    private void fitColumnsToContent() {
        TableColumnModel columns = table.getColumnModel();
        for (int modelColumn : FITTED_COLUMNS) {
            int viewColumn = table.convertColumnIndexToView(modelColumn);
            TableColumn column = columns.getColumn(viewColumn);
            int headerWidth = measureHeaderWidth(viewColumn);
            int width = headerWidth;
            TableCellRenderer cellRenderer = column.getCellRenderer();
            for (BudgetDto dto : allDtos) {
                Object value = tableModel.valueOf(dto, modelColumn);
                width = Math.max(width, cellRenderer
                        .getTableCellRendererComponent(table, value, false, false, 0, viewColumn)
                        .getPreferredSize().width);
            }
            if (isTotalVisible()) {
                width = Math.max(width, totalTable.measureWidth(viewColumn));
            }
            // 최소 폭을 바꾸면 폭이 최대 폭에 묶이지 않도록 최대 폭을 먼저 푼다.
            column.setMaxWidth(Integer.MAX_VALUE);
            column.setMinWidth(headerWidth);
            if (!columnsResizedByUser) {
                column.setPreferredWidth(width);
            }
            column.setMaxWidth(Math.max(width * FITTED_MAX_WIDTH_RATIO, column.getPreferredWidth()));
        }
        int nameViewColumn = table.convertColumnIndexToView(BudgetTableModel.COLUMN_NAME);
        columns.getColumn(nameViewColumn)
                .setMinWidth(measureHeaderWidth(nameViewColumn));
    }

    /**
     * 칸의 머리글을 줄이지 않고 적는 데 필요한 폭. 사용자가 칸을 줄일 수 있는 최소 폭이다.
     */
    private int measureHeaderWidth(int viewColumn) {
        TableColumn column = table.getColumnModel()
                .getColumn(viewColumn);
        return table.getTableHeader()
                .getDefaultRenderer()
                .getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, viewColumn)
                .getPreferredSize().width;
    }

    /**
     * 예산을 추가, 수정, 삭제하거나 순서를 바꿀 수 있는지 정한다. 마감된 회계 연도의 예산은 조회만 할 수 있으므로 false로 둔다.
     * 편집할 수 없으면 편집 툴바를 숨기고 상세 보기 버튼을 보인다. 읽기 전용 모드에서는 아무 효과가 없다.
     */
    public void setEditable(boolean editable) {
        this.editable = editable;
        for (JComponent control : editControls) {
            control.setVisible(editable);
        }
        if (btnView != null) {
            btnView.setVisible(!editable);
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
        if (!readOnly) {
            updateTotal();
            fitColumnsToContent();
        }

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
                "",
                parentPath,
                true);
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final String name = dialog.getName();
        final long amount = dialog.getAmount();
        final String remarks = dialog.getRemarks();

        AsyncTask.run(this,
                () -> {
                    budgetController.createBudget(currentFiscalYearId, parentId, name, amount, remarks, type);
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

        BudgetEntryDialog dialog = createDialogFor(selectedDto, "예산 수정");
        dialog.setVisible(true);

        if (!dialog.isConfirmed()) {
            return;
        }

        final String newName = dialog.getName();
        final long newAmount = dialog.getAmount();
        final String newRemarks = dialog.getRemarks();

        AsyncTask.run(this,
                () -> {
                    budgetController.updateBudget(selectedDto.getId(), newName, newAmount, newRemarks);
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

    private void onView() {
        BudgetDto selectedDto = getSelectedDto();
        if (selectedDto == null) {
            JOptionPane.showMessageDialog(this, "상세 보기할 예산을 선택해주세요.");
            return;
        }

        BudgetEntryDialog dialog = createDialogFor(selectedDto, "예산 상세 보기");
        dialog.enableViewOnly();
        dialog.setVisible(true);
    }

    /**
     * 기존 예산의 값을 채운 다이얼로그를 만든다. 상위 예산의 금액은 하위 예산의 합계이므로 바꿀 수 없다.
     */
    private BudgetEntryDialog createDialogFor(BudgetDto dto, String title) {
        boolean isLeaf = isLeaf(dto);
        String parentPath = (dto.getParentId() != null) ? getParentPath(dto) : "(없음)";
        long amountToShow = isLeaf ? dto.getAmount() : dto.getTotalAmount();

        return new BudgetEntryDialog(
                SwingUtilities.getWindowAncestor(this),
                title,
                dto.getName(),
                amountToShow,
                dto.getRemarks(),
                parentPath,
                isLeaf);
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
     * 행을 더블클릭했을 때 할 일을 정한다. 등록하면 편집 모드의 더블클릭 수정 대신 이 처리기를 부른다. 예산을 고르는 다이얼로그가 더블클릭한
     * 예산으로 확인할 때 쓴다. 표가 더블클릭한 행을 선택한 뒤에 부르므로, 처리기는 {@link #getSelectedBudget()}을
     * 그대로 쓰면 된다.
     * <p>
     * 표는 행을 고른 채 누른 Enter에도 같은 처리기를 부르므로, 다이얼로그가 Enter를 따로 다루려면 이 메서드를 부른 뒤에 표의 키
     * 동작을 바꾼다.
     */
    public void setRowDoubleClickHandler(Runnable handler) {
        table.setRowDoubleClickHandler(_ -> handler.run());
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
