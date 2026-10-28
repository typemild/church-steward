package org.church.steward.ui.view.datamanagement.contactimport;

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

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;

/**
 * 관계자 가져오기의 2단계. 파일의 어느 열이 구분, 이름, 비고, 상태인지 지정한다.
 * 파일의 형식을 정해 두지 않으므로 사용자가 직접 연결하며, 머리글 이름으로 추측한 값을 처음에 채워 둔다.
 */
class ColumnMappingStepPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String GUIDE = "구분 열의 값은 개인, 내부부서, 업체, 단체, 기타 중 하나여야 합니다.\n"
            + "상태 열의 값이 \"숨김\"이면 숨긴 관계자로, 비어 있거나 \"표시\"이면 보이는 관계자로 가져옵니다.\n"
            + "이름이 빈 행은 읽지 않습니다.";

    /**
     * 구분을 읽는 방식의 항목.
     *
     * @param type 파일 전체의 구분. 열에서 읽으면 null
     */
    private record TypeOption(ContactType type, String label) {
        @Override
        public String toString() {
            return label;
        }
    }

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

    private final ContactImportSession session;

    private final StyledComboBox<TypeOption> typeModeCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> typeColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> nameColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> remarksColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final StyledComboBox<ColumnOption> hiddenColumnCombo = new StyledComboBox<>(ComponentSize.MEDIUM);

    ColumnMappingStepPanel(ContactImportSession session) {
        this.session = session;

        setLayout(new BorderLayout());
        setOpaque(false);

        List<TypeOption> typeOptions = new ArrayList<>();
        for (ContactType type : ContactType.values()) {
            typeOptions.add(new TypeOption(type, "전체가 " + type.getLabel()));
        }
        typeOptions.add(new TypeOption(null, "열에서 읽기"));
        typeModeCombo.setModel(new DefaultComboBoxModel<>(typeOptions.toArray(new TypeOption[0])));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        JPanel typePanel = new JPanel(new GridLayout(1, 2, 8, 0));
        typePanel.setOpaque(false);
        typePanel.add(typeModeCombo);
        typePanel.add(typeColumnCombo);

        int row = 0;
        addRow(formPanel, row++, "구분", typePanel);
        addRow(formPanel, row++, "이름", nameColumnCombo);
        addRow(formPanel, row++, "비고", remarksColumnCombo);
        addRow(formPanel, row++, "상태", hiddenColumnCombo);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = row;
        formPanel.add(new DescriptionLabel(GUIDE), gbc);

        add(formPanel, BorderLayout.NORTH);

        typeModeCombo.addActionListener(_ -> updateEnabled());
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

        for (int i = 0; i < typeModeCombo.getItemCount(); i++) {
            TypeOption option = typeModeCombo.getItemAt(i);
            if (option.type() == session.getFixedType()) {
                typeModeCombo.setSelectedItem(option);
            }
        }
        setOptions(typeColumnCombo, options, session.getTypeColumn());
        setOptions(nameColumnCombo, options, session.getNameColumn());
        setOptions(remarksColumnCombo, options, session.getRemarksColumn());
        setOptions(hiddenColumnCombo, options, session.getHiddenColumn());
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

        TypeOption typeOption = (TypeOption) typeModeCombo.getSelectedItem();
        session.setFixedType(typeOption == null ? null : typeOption.type());
        session.setTypeColumn(selectedColumn(typeColumnCombo));
        session.setNameColumn(selectedColumn(nameColumnCombo));
        session.setRemarksColumn(selectedColumn(remarksColumnCombo));
        session.setHiddenColumn(selectedColumn(hiddenColumnCombo));

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
                String.valueOf(session.getNameColumn()), String.valueOf(session.getRemarksColumn()),
                String.valueOf(session.getHiddenColumn()));
    }

    private void updateEnabled() {
        TypeOption typeOption = (TypeOption) typeModeCombo.getSelectedItem();
        typeColumnCombo.setEnabled(typeOption != null && typeOption.type() == null);
    }

}
