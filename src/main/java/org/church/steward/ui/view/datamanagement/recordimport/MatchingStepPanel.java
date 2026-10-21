package org.church.steward.ui.view.datamanagement.recordimport;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.datatransfer.dto.ImportMappingDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.util.UIColors;

import com.formdev.flatlaf.util.SystemInfo;

/**
 * 수입/지출 가져오기의 3단계. 파일에 나온 예산 값과 관계자 이름을 등록된 예산과 관계자에 매핑한다.
 * <p>
 * 예산과 관계자는 자동으로 만들지 않으므로, 자동 매핑에 실패한 값을 여기서 사용자가 직접 매핑한다.
 * 같은 값은 여러 행에 나와도 한 줄이고, 그 한 줄을 매핑하면 그 값을 쓰는 모든 행에 적용된다.
 * 여러 줄을 골라 한 대상에 한꺼번에 매핑할 수도 있다.
 */
class MatchingStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int COLUMN_TYPE = 0;
    private static final int COLUMN_LABEL = 1;
    private static final int COLUMN_COUNT = 2;
    private static final int COLUMN_AMOUNT = 3;
    private static final int COLUMN_TARGET = 4;
    private static final int COLUMN_NOTE = 5;
    private static final String[] COLUMN_NAMES = { "구분", "파일의 값", "건수", "금액 합계", "매핑", "비고" };
    private static final String UNMAPPED_TEXT = "(매핑 안 됨)";
    /** 여러 줄을 고르는 방법. Swing의 표는 맥에서 Ctrl 대신 Command 키로 한 줄씩 더 고른다. */
    private static final String MULTI_SELECT_GUIDE = SystemInfo.isMacOS
            ? "(Command(⌘) 또는 Shift를 누른 채 클릭)"
            : "(Ctrl 또는 Shift를 누른 채 클릭)";
    private static final int TAB_TYPE = 0;
    private static final int TAB_BUDGET = 1;
    private static final int TAB_CONTACT = 2;

    private final RecordImportSession session;
    private final BudgetController budgetController;
    private final ContactController contactController;
    private final ContactGroupController contactGroupController;
    private final Runnable onMappingChanged;

    private final StyledCheckBox failedOnlyCheckBox = new StyledCheckBox("자동 매핑에 실패한 항목만 표시",
            ComponentSize.MEDIUM);
    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final MappingTablePanel typeTablePanel;
    private final MappingTablePanel budgetTablePanel;
    private final MappingTablePanel contactTablePanel;

    /**
     * @param onMappingChanged 매핑을 바꿨거나, 선택 다이얼로그에서 예산이나 관계자를 고쳤을 수 있어 다시 분석해야 할 때
     *                         실행할 동작
     */
    MatchingStepPanel(RecordImportSession session, BudgetController budgetController,
            ContactController contactController, ContactGroupController contactGroupController,
            Runnable onMappingChanged) {
        this.session = session;
        this.budgetController = budgetController;
        this.contactController = contactController;
        this.contactGroupController = contactGroupController;
        this.onMappingChanged = onMappingChanged;

        setLayout(new BorderLayout(0, 8));
        setOpaque(false);

        add(new SubFieldLabel("<html>매핑되지 않은 값을 쓰는 행은 가져올 수 없습니다.<br>여러 줄을 골라 한꺼번에 매핑할 수 있습니다. "
                + MULTI_SELECT_GUIDE + "</html>", ComponentSize.MEDIUM), BorderLayout.NORTH);

        // 구분 탭의 파일의 값과 매핑은 "세입", "수입"처럼 짧은 낱말이고, 다른 탭의 구분 칸은 "수입"/"지출"뿐이어서 가운데에 맞춘다.
        // 구분 탭은 구분 자체를 정하는 곳이어서 구분 칸에 적을 값이 없으므로 그 칸을 빼고 보여준다.
        typeTablePanel = new MappingTablePanel(() -> analysisTypeMappings(), this::mapTypes, this::unmapTypes,
                Set.of(COLUMN_LABEL, COLUMN_TARGET), false);
        budgetTablePanel = new MappingTablePanel(() -> analysisBudgetMappings(), this::mapBudgets,
                this::unmapBudgets, Set.of(COLUMN_TYPE), true);
        contactTablePanel = new MappingTablePanel(() -> analysisContactMappings(), this::mapContacts,
                this::unmapContacts, Set.of(COLUMN_TYPE), true);
        // 구분이 정해져야 그 행의 예산과 관계자를 연결할 수 있으므로 구분 탭을 맨 앞에 둔다.
        tabbedPane.addTab("구분 매핑", typeTablePanel);
        tabbedPane.addTab("예산 매핑", budgetTablePanel);
        tabbedPane.addTab("관계자 매핑", contactTablePanel);
        add(tabbedPane, BorderLayout.CENTER);

        failedOnlyCheckBox.setOpaque(false);
        failedOnlyCheckBox.addActionListener(_ -> refresh());

        tabbedPane.addChangeListener(_ -> attachFilterToSelectedTab());
        attachFilterToSelectedTab();
    }

    private void attachFilterToSelectedTab() {
        if (tabbedPane.getSelectedComponent() instanceof MappingTablePanel tablePanel) {
            tablePanel.attachFilter(failedOnlyCheckBox);
        }
    }

    /**
     * 가져오기를 새로 시작할 때 구분 탭이 보이게 한다. 구분이 정해져야 그 행의 예산과 관계자를 연결할 수 있기 때문이다.
     */
    void reset() {
        tabbedPane.setSelectedIndex(TAB_TYPE);
    }

    private List<ImportMappingDto> analysisTypeMappings() {
        return session.hasAnalysis() ? session.getAnalysis().getTypeMappings() : List.of();
    }

    private List<ImportMappingDto> analysisBudgetMappings() {
        return session.hasAnalysis() ? session.getAnalysis().getBudgetMappings() : List.of();
    }

    private List<ImportMappingDto> analysisContactMappings() {
        return session.hasAnalysis() ? session.getAnalysis().getContactMappings() : List.of();
    }

    /**
     * 세션의 분석 결과를 화면에 반영한다.
     */
    void refresh() {
        typeTablePanel.refresh();
        budgetTablePanel.refresh();
        contactTablePanel.refresh();
        tabbedPane.setTitleAt(TAB_TYPE, String.format("구분 매핑 (오류 %,d건)", countUnmapped(analysisTypeMappings())));
        tabbedPane.setTitleAt(TAB_BUDGET,
                String.format("예산 매핑 (오류 %,d건)", countUnmapped(analysisBudgetMappings())));
        tabbedPane.setTitleAt(TAB_CONTACT,
                String.format("관계자 매핑 (오류 %,d건)", countUnmapped(analysisContactMappings())));

        // 구분을 열에서 읽지 않는 파일은 매핑할 구분이 없으므로 구분 탭을 끄고 예산 탭을 보여준다.
        boolean hasTypeMappings = !analysisTypeMappings().isEmpty();
        tabbedPane.setEnabledAt(TAB_TYPE, hasTypeMappings);
        if (!hasTypeMappings && tabbedPane.getSelectedIndex() == TAB_TYPE) {
            tabbedPane.setSelectedIndex(TAB_BUDGET);
        }
    }

    private long countUnmapped(List<ImportMappingDto> mappings) {
        return mappings.stream()
                .filter(mapping -> !mapping.isMapped())
                .count();
    }

    private List<String> keysOf(List<ImportMappingDto> mappings) {
        return mappings.stream()
                .map(ImportMappingDto::getKey)
                .toList();
    }

    private String describe(List<ImportMappingDto> mappings) {
        ImportMappingDto first = mappings.getFirst();
        String description = "파일의 값: " + first.getLabel();
        if (mappings.size() > 1) {
            description += " 외 " + (mappings.size() - 1) + "개";
        }
        int count = mappings.stream()
                .mapToInt(ImportMappingDto::getCount)
                .sum();
        return description + String.format(" (%,d건)", count);
    }

    /**
     * 구분 열의 값을 수입으로 읽을지 지출로 읽을지 고르게 한다.
     */
    private void mapTypes(List<ImportMappingDto> mappings) {
        String[] options = { FinancialRecordType.INCOME.getLabel(), FinancialRecordType.EXPENSE.getLabel(), "취소" };
        int answer = JOptionPane.showOptionDialog(this, describe(mappings) + "\n이 값을 어느 구분으로 읽을까요?", "구분 매핑",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (answer != 0 && answer != 1) {
            return;
        }
        session.setTypeMapping(keysOf(mappings),
                answer == 0 ? FinancialRecordType.INCOME : FinancialRecordType.EXPENSE);
        onMappingChanged.run();
    }

    private void unmapTypes(List<ImportMappingDto> mappings) {
        session.setTypeMapping(keysOf(mappings), null);
        onMappingChanged.run();
    }

    private void mapBudgets(List<ImportMappingDto> mappings) {
        // 수입 내역은 수입 예산에, 지출 내역은 지출 예산에만 등록할 수 있으므로 한 번에 한 구분만 매핑한다.
        String typeLabel = mappings.getFirst().getTypeLabel();
        boolean sameType = mappings.stream()
                .allMatch(mapping -> mapping.getTypeLabel().equals(typeLabel));
        if (!sameType) {
            JOptionPane.showMessageDialog(this, "수입과 지출의 예산은 따로 매핑해주세요.", "예산 매핑", JOptionPane.ERROR_MESSAGE);
            return;
        }
        BudgetType budgetType = BudgetType.INCOME.getLabel().equals(typeLabel) ? BudgetType.INCOME
                : BudgetType.EXPENSE;

        Long budgetId = new BudgetMappingDialog(SwingUtilities.getWindowAncestor(this), budgetController,
                session.getFiscalYear().getId(), budgetType, describe(mappings)).showDialog();
        if (budgetId != null) {
            session.setBudgetMapping(keysOf(mappings), budgetId);
        }
        // 고르지 않고 닫았어도 다이얼로그에서 예산을 추가하거나 고쳤을 수 있으므로 다시 분석한다.
        onMappingChanged.run();
    }

    private void unmapBudgets(List<ImportMappingDto> mappings) {
        session.setBudgetMapping(keysOf(mappings), null);
        onMappingChanged.run();
    }

    private void mapContacts(List<ImportMappingDto> mappings) {
        ImportMappingDto first = mappings.getFirst();
        // 고른 항목이 모두 같은 관계자들에 매핑되어 있으면 그 관계자들을 칩으로 넣어 둔다. 아니면 첫 항목에서 자동으로 찾은
        // 관계자를 칩으로 넣고, 찾지 못한 첫 이름을 자동완성란에 채워 찾게 한다. 빈 칸 항목은 찾을 이름이 없으므로 비워 둔다.
        List<Long> currentContactIds = first.getTargetIds();
        boolean sameTarget = !currentContactIds.isEmpty() && mappings.stream()
                .allMatch(mapping -> currentContactIds.equals(mapping.getTargetIds()));
        List<Long> initialContactIds = sameTarget ? currentContactIds : first.getSuggestedTargetIds();
        String initialName = sameTarget || first.getUnresolvedNames().isEmpty() ? null
                : first.getUnresolvedNames().getFirst();
        // 수입의 봉헌자는 여러 명일 수 있지만 지출의 수취인은 한 명이므로, 지출 항목이 섞여 있으면 한 명만 고르게 한다.
        boolean incomeOnly = mappings.stream()
                .allMatch(mapping -> FinancialRecordType.INCOME.getLabel().equals(mapping.getTypeLabel()));

        List<Long> contactIds = new ContactMappingDialog(SwingUtilities.getWindowAncestor(this), contactController,
                contactGroupController, describe(mappings), incomeOnly, initialContactIds, initialName).showDialog();
        if (!contactIds.isEmpty()) {
            session.setContactMapping(keysOf(mappings), contactIds);
        }
        // 고르지 않고 닫았어도 다이얼로그에서 관계자를 등록하거나 고쳤을 수 있으므로 다시 분석한다.
        onMappingChanged.run();
    }

    private void unmapContacts(List<ImportMappingDto> mappings) {
        session.setContactMapping(keysOf(mappings), List.of());
        onMappingChanged.run();
    }

    /**
     * 매핑 항목의 표와 "매핑", "매핑 해제" 버튼. 구분, 예산, 관계자 탭이 함께 쓴다.
     */
    private final class MappingTablePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final Supplier<List<ImportMappingDto>> source;
        /** 가운데 정렬할 칸(모델 기준). 금액과 건수는 오른쪽, 나머지는 왼쪽에 맞춘다. */
        private final Set<Integer> centeredColumns;
        private final List<ImportMappingDto> visibleMappings = new ArrayList<>();
        private final MappingTableModel tableModel = new MappingTableModel();
        private final ListTable table = new ListTable(tableModel);
        private final PrimaryButton mapButton = new PrimaryButton("선택한 항목 매핑", ComponentSize.SMALL);
        private final SecondaryButton unmapButton = new SecondaryButton("직접 한 매핑 해제", ComponentSize.SMALL);
        /** 표 아래 줄. 왼쪽에 매핑 버튼, 오른쪽에 보이는 탭일 때 표시 방법 체크박스를 둔다. */
        private final JPanel bottomPanel = new JPanel(new BorderLayout());

        /**
         * @param showTypeColumn 구분 칸을 보여줄지 여부. 표에서 칸을 빼도 모델의 칸 번호는 그대로이므로, 렌더러는 모델
         *                       기준 칸 번호로 판단한다.
         */
        private MappingTablePanel(Supplier<List<ImportMappingDto>> source, Consumer<List<ImportMappingDto>> onMap,
                Consumer<List<ImportMappingDto>> onUnmap, Set<Integer> centeredColumns, boolean showTypeColumn) {
            this.source = source;
            this.centeredColumns = centeredColumns;

            setLayout(new BorderLayout(0, 8));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

            table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

            MappingCellRenderer renderer = new MappingCellRenderer();
            TableColumnModel columns = table.getColumnModel();
            for (int column = 0; column < COLUMN_NAMES.length; column++) {
                columns.getColumn(column).setCellRenderer(renderer);
            }
            columns.getColumn(COLUMN_TYPE).setMaxWidth(45);
            columns.getColumn(COLUMN_LABEL).setPreferredWidth(150);
            columns.getColumn(COLUMN_COUNT).setMaxWidth(50);
            columns.getColumn(COLUMN_AMOUNT).setPreferredWidth(90);
            columns.getColumn(COLUMN_AMOUNT).setMaxWidth(110);
            columns.getColumn(COLUMN_TARGET).setPreferredWidth(170);
            columns.getColumn(COLUMN_NOTE).setPreferredWidth(170);
            // 칸 설정은 모델과 표의 칸 번호가 같을 때 끝내고, 그 뒤에 칸을 뺀다.
            if (!showTypeColumn) {
                table.removeColumn(columns.getColumn(COLUMN_TYPE));
            }

            JScrollPane scrollPane = new JScrollPane(table);
            scrollPane.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
            add(scrollPane, BorderLayout.CENTER);

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            buttonPanel.setOpaque(false);
            buttonPanel.add(mapButton);
            buttonPanel.add(Box.createHorizontalStrut(8));
            buttonPanel.add(unmapButton);
            bottomPanel.setOpaque(false);
            bottomPanel.add(buttonPanel, BorderLayout.WEST);
            add(bottomPanel, BorderLayout.SOUTH);

            table.getSelectionModel().addListSelectionListener(_ -> updateButtons());
            // 행을 고른 채 누른 Enter도 이 처리기를 부른다. 여러 줄을 고른 채 누르면 고른 줄을 모두 매핑한다.
            table.setRowDoubleClickHandler(_ -> onMap.accept(selectedMappings()));
            mapButton.addActionListener(_ -> onMap.accept(selectedMappings()));
            unmapButton.addActionListener(_ -> onUnmap.accept(selectedMappings()));
            updateButtons();
        }

        /**
         * 기본으로는 모든 항목을 보여주고, "자동 매핑에 실패한 항목만 표시"를 켜면 아직 매핑되지 않은 항목만 보여준다.
         * <p>
         * 고르고 있던 항목은 다시 고른 상태로 둔다. 매핑 다이얼로그를 닫으면 다시 분석해 표를 새로 채우는데, 그 뒤에도
         * 마지막으로 고른 항목에서 이어서 작업할 수 있게 하기 위함이다. 표에서 빠진 항목(실패한 항목만 표시할 때
         * 매핑된 항목)은 고르지 않는다.
         */
        private void refresh() {
            Set<String> selectedKeys = new HashSet<>(keysOf(selectedMappings()));
            visibleMappings.clear();
            for (ImportMappingDto mapping : source.get()) {
                if (!failedOnlyCheckBox.isSelected() || !mapping.isMapped()) {
                    visibleMappings.add(mapping);
                }
            }
            tableModel.fireTableDataChanged();
            for (int row = 0; row < visibleMappings.size(); row++) {
                if (selectedKeys.contains(visibleMappings.get(row).getKey())) {
                    table.addRowSelectionInterval(row, row);
                }
            }
            updateButtons();
        }

        /**
         * 세 탭이 함께 쓰는 체크박스를 이 표의 오른쪽 아래에 단다. 다른 탭에 달려 있던 것은 Swing이 알아서 떼어 낸다.
         */
        private void attachFilter(Component filter) {
            bottomPanel.add(filter, BorderLayout.EAST);
            bottomPanel.revalidate();
            bottomPanel.repaint();
        }

        private List<ImportMappingDto> selectedMappings() {
            List<ImportMappingDto> selected = new ArrayList<>();
            for (int row : table.getSelectedRows()) {
                selected.add(visibleMappings.get(row));
            }
            return selected;
        }

        private void updateButtons() {
            List<ImportMappingDto> selected = selectedMappings();
            mapButton.setEnabled(!selected.isEmpty());
            unmapButton.setEnabled(selected.stream()
                    .anyMatch(ImportMappingDto::isManual));
        }

        private final class MappingTableModel extends AbstractTableModel {
            private static final long serialVersionUID = 1L;

            @Override
            public int getRowCount() {
                return visibleMappings.size();
            }

            @Override
            public int getColumnCount() {
                return COLUMN_NAMES.length;
            }

            @Override
            public String getColumnName(int column) {
                return COLUMN_NAMES[column];
            }

            @Override
            public Object getValueAt(int row, int column) {
                ImportMappingDto mapping = visibleMappings.get(row);
                return switch (column) {
                case COLUMN_TYPE -> mapping.getTypeLabel();
                case COLUMN_LABEL -> mapping.getLabel();
                case COLUMN_COUNT -> String.format("%,d", mapping.getCount());
                case COLUMN_AMOUNT -> String.format("%,d", mapping.getTotalAmount());
                case COLUMN_TARGET -> mapping.isMapped() ? mapping.getTargetLabel() : UNMAPPED_TEXT;
                default -> mapping.isManual() ? "직접 매핑함" : mapping.getNote();
                };
            }
        }

        /**
         * 매핑되지 않은 항목의 매핑 칸을 눈에 띄게 그린다.
         */
        private final class MappingCellRenderer extends DefaultTableCellRenderer {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
                ImportMappingDto mapping = visibleMappings.get(row);
                // 구분 탭은 구분 칸을 빼고 보여주므로 표의 칸 번호를 모델 기준으로 바꿔 판단한다.
                int modelColumn = table.convertColumnIndexToModel(column);

                setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 6));
                if (modelColumn == COLUMN_COUNT || modelColumn == COLUMN_AMOUNT) {
                    setHorizontalAlignment(SwingConstants.RIGHT);
                } else if (centeredColumns.contains(modelColumn)) {
                    setHorizontalAlignment(SwingConstants.CENTER);
                } else {
                    setHorizontalAlignment(SwingConstants.LEFT);
                }
                if (!isSelected) {
                    boolean unmappedTarget = modelColumn == COLUMN_TARGET && !mapping.isMapped();
                    setForeground(unmappedTarget ? UIColors.EXPENSE
                            : modelColumn == COLUMN_NOTE ? UIColors.TEXT_SUB : UIColors.TEXT_MAIN);
                }
                setToolTipText(value == null || value.toString().isEmpty() ? null : value.toString());
                return this;
            }
        }
    }

}
