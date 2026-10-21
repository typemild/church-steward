package org.church.steward.ui.view.datamanagement.recordimport;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.view.datamanagement.recordimport.RecordImportSession.TypeMode;

/**
 * 수입/지출 가져오기의 2단계. 파일의 어느 열이 구분, 금액, 일자, 예산, 내용, 관계자, 비고인지 지정한다.
 * 파일의 형식을 정해 두지 않으므로 사용자가 직접 연결하며, 머리글 이름으로 추측한 값을 처음에 채워 둔다.
 */
class ColumnMappingStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String DATE_IN_ONE_COLUMN = "한 칸에 적힌 일자";
    private static final String DATE_IN_SEPARATE_COLUMNS = "연, 월, 일이 다른 열";
    private static final String BUDGET_BY_LEVELS = "단계별 열 (열마다 한 단계)";
    private static final String BUDGET_BY_PATH = "전체 예산명 열 (예산A > 예산B)";

    /**
     * 구분과 금액을 읽는 방식을 고르는 목록의 항목.
     */
    private record TypeModeOption(TypeMode mode, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final TypeModeOption[] TYPE_MODE_OPTIONS = {
            new TypeModeOption(TypeMode.ALL_INCOME, "전체가 수입"),
            new TypeModeOption(TypeMode.ALL_EXPENSE, "전체가 지출"),
            new TypeModeOption(TypeMode.TYPE_COLUMN, "구분 열에서 읽기"),
            new TypeModeOption(TypeMode.SPLIT_AMOUNTS, "수입 금액과 지출 금액의 열이 따로 있음") };

    /**
     * 열을 고르는 목록의 항목.
     *
     * @param column 열의 위치 (0부터). "(없음)"이면 null
     */
    private record ColumnOption(Integer column, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

    private static final ColumnOption NO_COLUMN = new ColumnOption(null, "(없음)");

    private final RecordImportSession session;

    private final StyledComboBox<TypeModeOption> typeModeCombo = new StyledComboBox<>(TYPE_MODE_OPTIONS,
            ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> typeColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> amountColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> incomeAmountColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> expenseAmountColumnCombo = createColumnCombo();
    private final StyledComboBox<String> dateModeCombo = new StyledComboBox<>(
            new String[] { DATE_IN_ONE_COLUMN, DATE_IN_SEPARATE_COLUMNS }, ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> dateColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> yearColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> monthColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> dayColumnCombo = createColumnCombo();
    private final StyledComboBox<String> budgetModeCombo = new StyledComboBox<>(
            new String[] { BUDGET_BY_LEVELS, BUDGET_BY_PATH }, ComponentSize.MEDIUM);
    private final List<StyledComboBox<ColumnOption>> budgetLevelColumnCombos = new ArrayList<>();
    private final StyledComboBox<ColumnOption> budgetPathColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> descriptionColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> contactColumnCombo = createColumnCombo();
    private final StyledComboBox<ColumnOption> remarksColumnCombo = createColumnCombo();

    ColumnMappingStepPanel(RecordImportSession session) {
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel budgetLevelPanel = new JPanel(new GridLayout(0, 3, 8, 6));
        budgetLevelPanel.setOpaque(false);
        for (int level = 1; level <= RecordImportSession.MAX_LEVELS; level++) {
            StyledComboBox<ColumnOption> combo = createColumnCombo();
            budgetLevelColumnCombos.add(combo);
            budgetLevelPanel.add(labeled(level + "단계", combo));
        }
        budgetLevelPanel.add(labeled("전체", budgetPathColumnCombo));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        int row = 0;
        addRow(formPanel, row++, "구분", columns(typeModeCombo, labeled("구분 열", typeColumnCombo)));
        addRow(formPanel, row++, "금액", columns(labeled("금액", amountColumnCombo),
                labeled("수입", incomeAmountColumnCombo), labeled("지출", expenseAmountColumnCombo)));
        addRow(formPanel, row++, "일자", columns(dateModeCombo, labeled("일자 열", dateColumnCombo)));
        addRow(formPanel, row++, "", columns(labeled("연", yearColumnCombo), labeled("월", monthColumnCombo),
                labeled("일", dayColumnCombo)));
        addRow(formPanel, row++, "예산", budgetModeCombo);
        addRow(formPanel, row++, "", budgetLevelPanel);
        addRow(formPanel, row++, "그 밖의 항목", columns(labeled("내용", descriptionColumnCombo),
                labeled("관계자", contactColumnCombo), labeled("비고", remarksColumnCombo)));
        add(formPanel, BorderLayout.NORTH);

        typeModeCombo.addActionListener(_ -> updateEnabled());
        dateModeCombo.addActionListener(_ -> updateEnabled());
        budgetModeCombo.addActionListener(_ -> updateEnabled());
    }

    private static StyledComboBox<ColumnOption> createColumnCombo() {
        return new StyledComboBox<>(ComponentSize.MEDIUM);
    }

    /**
     * 작은 이름표를 왼쪽에 붙인 입력란을 만든다.
     */
    private JPanel labeled(String label, JComponent field) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setOpaque(false);
        panel.add(new SubFieldLabel(label, ComponentSize.MEDIUM), BorderLayout.WEST);
        panel.add(field, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 여러 입력란을 같은 폭으로 한 줄에 놓는다.
     */
    private JPanel columns(JComponent... components) {
        JPanel panel = new JPanel(new GridLayout(1, components.length, 8, 0));
        panel.setOpaque(false);
        for (JComponent component : components) {
            panel.add(component);
        }
        return panel;
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 9, 15);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new FieldLabel(label, ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 9, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = row;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    /**
     * 세션의 파일과 열 지정을 화면에 채운다. 아직 추측하지 않은 파일이면 머리글 이름으로 먼저 추측한다.
     */
    void load() {
        session.guessMappingIfNeeded();

        List<ColumnOption> options = new ArrayList<>();
        options.add(NO_COLUMN);
        for (int column = 0; column < session.getColumnCount(); column++) {
            options.add(new ColumnOption(column, session.getColumnLabel(column)));
        }

        for (TypeModeOption option : TYPE_MODE_OPTIONS) {
            if (option.mode() == session.getTypeMode()) {
                typeModeCombo.setSelectedItem(option);
            }
        }
        dateModeCombo.setSelectedItem(session.isSeparateDate() ? DATE_IN_SEPARATE_COLUMNS : DATE_IN_ONE_COLUMN);
        budgetModeCombo.setSelectedItem(session.isBudgetPathMode() ? BUDGET_BY_PATH : BUDGET_BY_LEVELS);

        setOptions(typeColumnCombo, options, session.getTypeColumn());
        setOptions(amountColumnCombo, options, session.getAmountColumn());
        setOptions(incomeAmountColumnCombo, options, session.getIncomeAmountColumn());
        setOptions(expenseAmountColumnCombo, options, session.getExpenseAmountColumn());
        setOptions(dateColumnCombo, options, session.getDateColumn());
        setOptions(yearColumnCombo, options, session.getYearColumn());
        setOptions(monthColumnCombo, options, session.getMonthColumn());
        setOptions(dayColumnCombo, options, session.getDayColumn());
        for (int level = 0; level < budgetLevelColumnCombos.size(); level++) {
            List<Integer> levelColumns = session.getBudgetLevelColumns();
            setOptions(budgetLevelColumnCombos.get(level), options,
                    level < levelColumns.size() ? levelColumns.get(level) : null);
        }
        setOptions(budgetPathColumnCombo, options, session.getBudgetPathColumn());
        setOptions(descriptionColumnCombo, options, session.getDescriptionColumn());
        setOptions(contactColumnCombo, options, session.getContactColumn());
        setOptions(remarksColumnCombo, options, session.getRemarksColumn());
        updateEnabled();
    }

    private void setOptions(StyledComboBox<ColumnOption> combo, List<ColumnOption> options, Integer selectedColumn) {
        combo.setModel(new DefaultComboBoxModel<>(options.toArray(new ColumnOption[0])));
        for (ColumnOption option : options) {
            if (Objects.equals(option.column(), selectedColumn)) {
                combo.setSelectedItem(option);
            }
        }
    }

    /**
     * 화면의 열 지정을 세션에 담는다. 지정이 달라졌으면 이전의 분석 결과는 쓸 수 없으므로 버린다.
     * 직접 한 예산·관계자 매핑은 파일의 값에 대한 것이므로 남겨 둔다.
     */
    void store() {
        String before = describeMapping();

        session.setTypeMode(((TypeModeOption) typeModeCombo.getSelectedItem()).mode());
        session.setTypeColumn(selectedColumn(typeColumnCombo));
        session.setAmountColumn(selectedColumn(amountColumnCombo));
        session.setIncomeAmountColumn(selectedColumn(incomeAmountColumnCombo));
        session.setExpenseAmountColumn(selectedColumn(expenseAmountColumnCombo));
        session.setSeparateDate(DATE_IN_SEPARATE_COLUMNS.equals(dateModeCombo.getSelectedItem()));
        session.setDateColumn(selectedColumn(dateColumnCombo));
        session.setYearColumn(selectedColumn(yearColumnCombo));
        session.setMonthColumn(selectedColumn(monthColumnCombo));
        session.setDayColumn(selectedColumn(dayColumnCombo));
        session.setBudgetPathMode(BUDGET_BY_PATH.equals(budgetModeCombo.getSelectedItem()));
        session.setBudgetLevelColumns(budgetLevelColumnCombos.stream()
                .map(this::selectedColumn)
                .toList());
        session.setBudgetPathColumn(selectedColumn(budgetPathColumnCombo));
        session.setDescriptionColumn(selectedColumn(descriptionColumnCombo));
        session.setContactColumn(selectedColumn(contactColumnCombo));
        session.setRemarksColumn(selectedColumn(remarksColumnCombo));

        if (!describeMapping().equals(before)) {
            session.clearAnalysis();
        }
    }

    private Integer selectedColumn(StyledComboBox<ColumnOption> combo) {
        ColumnOption option = (ColumnOption) combo.getSelectedItem();
        return option == null ? null : option.column();
    }

    /**
     * 세션의 열 지정 전체를 글자로 적는다. 지정이 달라졌는지 비교하는 데 쓴다.
     */
    private String describeMapping() {
        return String.join("|", String.valueOf(session.getTypeMode()), String.valueOf(session.getTypeColumn()),
                String.valueOf(session.getAmountColumn()), String.valueOf(session.getIncomeAmountColumn()),
                String.valueOf(session.getExpenseAmountColumn()), String.valueOf(session.isSeparateDate()),
                String.valueOf(session.getDateColumn()), String.valueOf(session.getYearColumn()),
                String.valueOf(session.getMonthColumn()), String.valueOf(session.getDayColumn()),
                String.valueOf(session.isBudgetPathMode()), String.valueOf(session.getBudgetLevelColumns()),
                String.valueOf(session.getBudgetPathColumn()), String.valueOf(session.getDescriptionColumn()),
                String.valueOf(session.getContactColumn()), String.valueOf(session.getRemarksColumn()));
    }

    private void updateEnabled() {
        TypeMode typeMode = ((TypeModeOption) typeModeCombo.getSelectedItem()).mode();
        boolean splitAmounts = typeMode == TypeMode.SPLIT_AMOUNTS;
        boolean separateDate = DATE_IN_SEPARATE_COLUMNS.equals(dateModeCombo.getSelectedItem());
        boolean budgetByPath = BUDGET_BY_PATH.equals(budgetModeCombo.getSelectedItem());

        typeColumnCombo.setEnabled(typeMode == TypeMode.TYPE_COLUMN);
        amountColumnCombo.setEnabled(!splitAmounts);
        incomeAmountColumnCombo.setEnabled(splitAmounts);
        expenseAmountColumnCombo.setEnabled(splitAmounts);
        dateColumnCombo.setEnabled(!separateDate);
        yearColumnCombo.setEnabled(separateDate);
        monthColumnCombo.setEnabled(separateDate);
        dayColumnCombo.setEnabled(separateDate);
        for (StyledComboBox<ColumnOption> combo : budgetLevelColumnCombos) {
            combo.setEnabled(!budgetByPath);
        }
        budgetPathColumnCombo.setEnabled(budgetByPath);
    }

}
