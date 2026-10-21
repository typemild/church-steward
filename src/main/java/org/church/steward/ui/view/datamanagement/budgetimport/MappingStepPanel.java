package org.church.steward.ui.view.datamanagement.budgetimport;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;

/**
 * 예산 가져오기의 2단계. 파일의 어느 열이 구분, 예산명, 금액인지 지정한다.
 * 파일의 형식을 정해 두지 않으므로 사용자가 직접 연결하며, 머리글 이름으로 추측한 값을 처음에 채워 둔다.
 */
class MappingStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String TYPE_ALL_INCOME = "전체가 수입 예산";
    private static final String TYPE_ALL_EXPENSE = "전체가 지출 예산";
    private static final String TYPE_FROM_COLUMN = "열에서 읽기";
    private static final String NAME_BY_LEVELS = "단계별 열 (열마다 한 단계)";
    private static final String NAME_BY_PATH = "전체 예산명 열 (예산A > 예산B)";

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

    private final BudgetImportSession session;

    private final StyledComboBox<String> typeModeCombo = new StyledComboBox<>(
            new String[] { TYPE_ALL_INCOME, TYPE_ALL_EXPENSE, TYPE_FROM_COLUMN }, ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> typeColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<String> nameModeCombo = new StyledComboBox<>(
            new String[] { NAME_BY_LEVELS, NAME_BY_PATH }, ComponentSize.MEDIUM);
    private final List<StyledComboBox<ColumnOption>> levelColumnCombos = new ArrayList<>();
    private final StyledComboBox<ColumnOption> pathColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> amountColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledCheckBox fillDownCheckBox = new StyledCheckBox("빈 칸(병합된 셀)은 윗 행의 값을 이어받음",
            ComponentSize.MEDIUM);
    private final StyledCheckBox updateAmountCheckBox = new StyledCheckBox("이미 있는 예산의 금액을 파일의 값으로 갱신",
            ComponentSize.MEDIUM);

    MappingStepPanel(BudgetImportSession session) {
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        JPanel typePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        typePanel.setOpaque(false);
        typePanel.add(typeModeCombo);
        typePanel.add(typeColumnCombo);

        JPanel levelPanel = new JPanel(new GridLayout(0, 3, 8, 8));
        levelPanel.setOpaque(false);
        for (int level = 1; level <= BudgetImportSession.MAX_LEVELS; level++) {
            StyledComboBox<ColumnOption> combo = new StyledComboBox<>(ComponentSize.MEDIUM);
            levelColumnCombos.add(combo);

            JPanel cell = new JPanel(new BorderLayout(6, 0));
            cell.setOpaque(false);
            cell.add(new SubFieldLabel(level + "단계", ComponentSize.MEDIUM), BorderLayout.WEST);
            cell.add(combo, BorderLayout.CENTER);
            levelPanel.add(cell);
        }

        JPanel optionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        optionPanel.setOpaque(false);
        optionPanel.add(fillDownCheckBox);
        optionPanel.add(Box.createHorizontalStrut(20));
        optionPanel.add(updateAmountCheckBox);

        int row = 0;
        addRow(formPanel, row++, "구분", typePanel);
        addRow(formPanel, row++, "예산명 읽는 방식", nameModeCombo);
        addRow(formPanel, row++, "단계별 예산명", levelPanel);
        addRow(formPanel, row++, "전체 예산명", pathColumnCombo);
        addRow(formPanel, row++, "금액", amountColumnCombo);
        addRow(formPanel, row++, "", optionPanel);

        add(formPanel, BorderLayout.NORTH);

        typeModeCombo.addActionListener(_ -> updateEnabled());
        nameModeCombo.addActionListener(_ -> updateEnabled());
    }

    private void addRow(JPanel panel, int row, String label, JComponent field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 12, 15);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        panel.add(new FieldLabel(label, ComponentSize.MEDIUM, true), gbc);

        gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 12, 0);
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

        if (session.getFixedType() == null) {
            typeModeCombo.setSelectedItem(TYPE_FROM_COLUMN);
        } else {
            typeModeCombo.setSelectedItem(
                    session.getFixedType() == BudgetType.INCOME ? TYPE_ALL_INCOME : TYPE_ALL_EXPENSE);
        }
        nameModeCombo.setSelectedItem(session.isPathMode() ? NAME_BY_PATH : NAME_BY_LEVELS);

        setOptions(typeColumnCombo, options, session.getTypeColumn());
        for (int level = 0; level < levelColumnCombos.size(); level++) {
            Integer column = level < session.getLevelColumns().size() ? session.getLevelColumns().get(level) : null;
            setOptions(levelColumnCombos.get(level), options, column);
        }
        setOptions(pathColumnCombo, options, session.getPathColumn());
        setOptions(amountColumnCombo, options, session.getAmountColumn());

        fillDownCheckBox.setSelected(session.isFillDown());
        updateAmountCheckBox.setSelected(session.isUpdateExistingAmounts());
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
     */
    void store() {
        String before = describeMapping();

        String typeMode = (String) typeModeCombo.getSelectedItem();
        if (TYPE_FROM_COLUMN.equals(typeMode)) {
            session.setFixedType(null);
        } else {
            session.setFixedType(TYPE_ALL_INCOME.equals(typeMode) ? BudgetType.INCOME : BudgetType.EXPENSE);
        }
        session.setTypeColumn(selectedColumn(typeColumnCombo));
        session.setPathMode(NAME_BY_PATH.equals(nameModeCombo.getSelectedItem()));
        session.setLevelColumns(levelColumnCombos.stream()
                .map(this::selectedColumn)
                .toList());
        session.setPathColumn(selectedColumn(pathColumnCombo));
        session.setAmountColumn(selectedColumn(amountColumnCombo));
        session.setFillDown(fillDownCheckBox.isSelected());
        session.setUpdateExistingAmounts(updateAmountCheckBox.isSelected());

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
        return String.join("|", String.valueOf(session.getFixedType()), String.valueOf(session.getTypeColumn()),
                String.valueOf(session.isPathMode()), String.valueOf(session.getLevelColumns()),
                String.valueOf(session.getPathColumn()), String.valueOf(session.getAmountColumn()),
                String.valueOf(session.isFillDown()), String.valueOf(session.isUpdateExistingAmounts()));
    }

    private void updateEnabled() {
        boolean typeFromColumn = TYPE_FROM_COLUMN.equals(typeModeCombo.getSelectedItem());
        boolean byPath = NAME_BY_PATH.equals(nameModeCombo.getSelectedItem());

        typeColumnCombo.setEnabled(typeFromColumn);
        for (StyledComboBox<ColumnOption> combo : levelColumnCombos) {
            combo.setEnabled(!byPath);
        }
        pathColumnCombo.setEnabled(byPath);
        // 전체 예산명 열에는 단계가 모두 적혀 있으므로 이어받을 빈 칸이 없다. 구분 열의 빈 칸은 이어받을 수 있다.
        fillDownCheckBox.setEnabled(!byPath || typeFromColumn);
    }

}
