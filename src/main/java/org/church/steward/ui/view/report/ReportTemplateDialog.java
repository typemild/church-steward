package org.church.steward.ui.view.report;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.church.steward.domain.report.dto.ReportApprovalDto;
import org.church.steward.domain.report.dto.ReportColumnDto;
import org.church.steward.domain.report.dto.ReportTemplateDto;
import org.church.steward.domain.report.entity.ReportTemplateConstraints;
import org.church.steward.domain.report.entity.ReportTemplateTable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;

import lombok.extern.slf4j.Slf4j;

/**
 * 보고서 양식 설정 다이얼로그. 결재란(표시 여부, 칸 수, 칸마다의 직함)과 표마다 칸의 순서와 숨김을 탭으로 나눠 정한다. 양식은
 * 프로그램 전체 설정이므로 모든 사용자의 보고서에 함께 적용된다. 저장은 다이얼로그가 직접 실행하며, 실패하면 닫지 않고 오류를 보여준다.
 */
@Slf4j
class ReportTemplateDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final int TITLE_FIELD_COLUMNS = 10;

    private final ReportTemplateDto original;
    private final Consumer<ReportTemplateDto> saveAction;
    private final Runnable onSaved;

    private final StyledCheckBox approvalVisibleCheckBox = new StyledCheckBox("결재란 표시", ComponentSize.MEDIUM);
    private final StyledComboBox<Integer> cellCountComboBox = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final List<StyledTextField> titleFields = new ArrayList<>();
    private final Map<ReportTemplateTable, ReportColumnOrderPanel> columnPanels = new EnumMap<>(
            ReportTemplateTable.class);

    private final PrimaryButton saveButton = new PrimaryButton("저장", ComponentSize.MEDIUM);
    private final SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);

    /**
     * @param template   지금 저장된 양식
     * @param saveAction 양식을 저장하는 동작. 백그라운드 스레드에서 실행되며, 저장할 수 없으면 사용자에게 보여줄 메시지를 담은
     *                   예외를 던져야 한다.
     * @param onSaved    저장에 성공한 뒤 실행할 동작
     */
    ReportTemplateDialog(Window owner, ReportTemplateDto template, Consumer<ReportTemplateDto> saveAction,
            Runnable onSaved) {
        super(owner, template.getReportType()
                .getLabel() + " 양식 설정", Dialog.ModalityType.APPLICATION_MODAL);
        this.original = template;
        this.saveAction = saveAction;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        getContentPane().setBackground(UIColors.BACKGROUND);

        JTabbedPane tabbedPane = new JTabbedPane();
        if (template.getApproval() != null) {
            tabbedPane.addTab("결재란", createApprovalPanel(template.getApproval()));
        }
        for (ReportTemplateTable table : ReportTemplateTable.of(template.getReportType())) {
            ReportColumnOrderPanel panel = new ReportColumnOrderPanel(table, template.getTables()
                    .get(table),
                    Boolean.TRUE.equals(template.getRateBarVisible()
                            .get(table)));
            columnPanels.put(table, panel);
            tabbedPane.addTab(table.getLabel(), panel);
        }

        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setOpaque(false);
        contentPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 0, 20));
        contentPanel.add(new SubFieldLabel("양식은 모든 사용자의 보고서에 함께 적용됩니다.", ComponentSize.SMALL),
                BorderLayout.NORTH);
        contentPanel.add(tabbedPane, BorderLayout.CENTER);
        add(contentPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));
        saveButton.addActionListener(_ -> onSave());
        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        DialogKeyHandler handler = DialogKeyHandler.install(this)
                .setButtons(cancelButton, saveButton);
        if (template.getApproval() != null) {
            handler.addField(cellCountComboBox);
            for (StyledTextField field : titleFields) {
                handler.addField(field);
            }
        }
        handler.trackChanges(this::currentTemplate);

        pack();
        setLocationRelativeTo(owner);
    }

    private JPanel createApprovalPanel(ReportApprovalDto approval) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 0, 12, 0);
        panel.add(approvalVisibleCheckBox, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.insets = new Insets(0, 0, 8, 8);
        panel.add(new FieldLabel("칸 수", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        gbc.insets = new Insets(0, 0, 8, 0);
        for (int count = ReportTemplateConstraints.APPROVAL_CELL_COUNT_MIN; count <= ReportTemplateConstraints.APPROVAL_CELL_COUNT_MAX; count++) {
            cellCountComboBox.addItem(count);
        }
        panel.add(cellCountComboBox, gbc);

        // 칸 수를 줄였다가 늘려도 적어 둔 직함이 남도록, 입력란은 가장 많은 칸 수만큼 두고 칸 수 밖의 것은 끈다.
        for (int i = 0; i < ReportTemplateConstraints.APPROVAL_CELL_COUNT_MAX; i++) {
            StyledTextField field = new StyledTextField(TITLE_FIELD_COLUMNS, ComponentSize.MEDIUM);
            field.setMaxLength(ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH);
            field.setBackground(UIColors.TEXT_FIELD_BACKGROUND);
            titleFields.add(field);

            gbc.gridx = 0;
            gbc.gridy = 2 + i;
            gbc.insets = new Insets(0, 0, 8, 8);
            panel.add(new FieldLabel(String.format("%d번째 칸 직함", i + 1), ComponentSize.MEDIUM), gbc);
            gbc.gridx = 1;
            gbc.insets = new Insets(0, 0, 8, 0);
            panel.add(field, gbc);
        }

        gbc.gridx = 0;
        gbc.gridy = 2 + ReportTemplateConstraints.APPROVAL_CELL_COUNT_MAX;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(4, 0, 0, 0);
        panel.add(new SubFieldLabel(String.format("<html>직함은 %d자 이내이며, 비워 두면 손으로 쓸 수 있게 빈칸으로 둡니다.<br>"
                + "칸에 들어가지 않는 직함은 글자를 작게 줄여 적습니다.</html>",
                ReportTemplateConstraints.APPROVAL_TITLE_MAX_LENGTH), ComponentSize.SMALL), gbc);

        // 남는 높이는 아래로 보내 입력란을 위에 모은다.
        gbc.gridy++;
        gbc.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        panel.add(filler, gbc);

        approvalVisibleCheckBox.setSelected(approval.isVisible());
        cellCountComboBox.setSelectedItem(approval.getTitles()
                .size());
        for (int i = 0; i < approval.getTitles()
                .size() && i < titleFields.size(); i++) {
            String title = approval.getTitles()
                    .get(i);
            titleFields.get(i)
                    .setText(title == null ? "" : title);
        }

        approvalVisibleCheckBox.addActionListener(_ -> updateApprovalFields());
        cellCountComboBox.addActionListener(_ -> updateApprovalFields());
        updateApprovalFields();
        return panel;
    }

    /**
     * 결재란을 숨기면 칸 수와 직함을 끄고, 보이면 칸 수만큼의 직함 입력란을 켠다.
     */
    private void updateApprovalFields() {
        boolean visible = approvalVisibleCheckBox.isSelected();
        int cellCount = selectedCellCount();
        cellCountComboBox.setEnabled(visible);
        for (int i = 0; i < titleFields.size(); i++) {
            titleFields.get(i)
                    .setEnabled(visible && i < cellCount);
        }
    }

    private int selectedCellCount() {
        Object selected = cellCountComboBox.getSelectedItem();
        return selected instanceof Integer count ? count : ReportTemplateConstraints.DEFAULT_APPROVAL_CELL_COUNT;
    }

    /**
     * 화면에서 정한 양식. 직함은 칸 수만큼이며 앞뒤 공백을 지우고 빈 직함은 null이다. 저장하지 않은 내용이 있는지 판단하는 데에도 쓴다.
     */
    private ReportTemplateDto currentTemplate() {
        ReportApprovalDto approval = null;
        if (original.getApproval() != null) {
            List<String> titles = new ArrayList<>();
            for (int i = 0; i < selectedCellCount(); i++) {
                String title = titleFields.get(i)
                        .getText()
                        .strip();
                titles.add(title.isEmpty() ? null : title);
            }
            approval = ReportApprovalDto.builder()
                    .visible(approvalVisibleCheckBox.isSelected())
                    .titles(titles)
                    .build();
        }

        Map<ReportTemplateTable, List<ReportColumnDto>> tables = new EnumMap<>(ReportTemplateTable.class);
        Map<ReportTemplateTable, Boolean> rateBarVisible = new EnumMap<>(ReportTemplateTable.class);
        for (Map.Entry<ReportTemplateTable, ReportColumnOrderPanel> entry : columnPanels.entrySet()) {
            tables.put(entry.getKey(), entry.getValue()
                    .getColumns());
            if (entry.getKey()
                    .hasRateBarOption()) {
                rateBarVisible.put(entry.getKey(), entry.getValue()
                        .isRateBarVisible());
            }
        }

        return ReportTemplateDto.builder()
                .reportType(original.getReportType())
                .approval(approval)
                .tables(tables)
                .rateBarVisible(rateBarVisible)
                .build();
    }

    private void onSave() {
        final ReportTemplateDto template = currentTemplate();
        // 저장하는 동안 저장 버튼을 잠가 두 번 저장되지 않게 한다.
        // 취소 버튼도 잠가 저장 중에는 Esc나 창의 닫기 버튼으로도 닫히지 않게 한다.
        cancelButton.setEnabled(false);
        AsyncTask.run(saveButton,
                () -> {
                    saveAction.accept(template);
                    return null;
                },
                _ -> {
                    dispose();
                    onSaved.run();
                },
                err -> {
                    cancelButton.setEnabled(true);
                    log.error("Failed to save report template: {}", err.getMessage());
                    JOptionPane.showMessageDialog(this, "저장 실패: " + err.getMessage());
                });
    }
}
