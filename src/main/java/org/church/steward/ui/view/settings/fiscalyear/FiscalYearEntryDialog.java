package org.church.steward.ui.view.settings.fiscalyear;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
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
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 회계 연도의 이름과 기간을 입력받아 저장하는 다이얼로그.
 * 새 회계 연도를 추가할 때는 기존 회계 연도의 예산을 복사할지도 함께 정한다.
 * 저장은 다이얼로그가 직접 실행하며({@link SaveAction}), 저장에 실패하면 닫지 않고 오류를 보여준다.
 */
@Slf4j
class FiscalYearEntryDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final StyledTextField nameField = new StyledTextField(ComponentSize.MEDIUM);
    private final FiscalYearPeriodField periodField = new FiscalYearPeriodField();
    private final StyledCheckBox copyCheckBox = new StyledCheckBox("기존 회계 연도의 예산 복사", ComponentSize.MEDIUM);
    private final StyledComboBox<String> sourceComboBox = new StyledComboBox<>(ComponentSize.MEDIUM);

    /** 예산을 복사해 올 수 있는 기존 회계 연도. 수정할 때는 비어 있다. */
    private final List<FiscalYearDto> existingFiscalYears;
    private final SaveAction saveAction;
    /** 저장에 성공한 뒤 실행할 동작 */
    private final Runnable onSaved;
    private final PrimaryButton saveButton = new PrimaryButton("저장", ComponentSize.MEDIUM);
    private final SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);

    /**
     * 입력한 내용을 저장하는 동작. 백그라운드 스레드에서 실행되며, 저장할 수 없으면 사용자에게 보여줄 메시지를 담은 예외를 던져야 한다.
     */
    @FunctionalInterface
    interface SaveAction {
        /**
         * @param sourceFiscalYearId 예산을 복사해 올 회계 연도의 ID. 예산을 복사하지 않거나 수정하는 경우에는 null이다.
         */
        void save(String name, LocalDate startDate, LocalDate endDate, Long sourceFiscalYearId);
    }

    /**
     * 새 회계 연도를 추가하는 다이얼로그를 만든다.
     * 기간의 기본값은 가장 최근 회계 연도의 종료일 다음 날부터 1년간이며, 회계 연도가 하나도 없으면 올해 1월 1일 ~ 12월 31일이다.
     *
     * @param existingFiscalYears 기존 회계 연도 목록. 최근 것(시작일이 늦은 것)이 앞에 와야 한다.
     * @param onSaved             저장에 성공한 뒤 실행할 동작
     */
    static FiscalYearEntryDialog forCreation(Frame owner, List<FiscalYearDto> existingFiscalYears,
            SaveAction saveAction, Runnable onSaved) {
        FiscalYearEntryDialog dialog = new FiscalYearEntryDialog(owner, "새 회계 연도 추가", existingFiscalYears, true,
                saveAction, onSaved);

        LocalDate startDate = existingFiscalYears.isEmpty() ? LocalDate.now()
                .withDayOfYear(1)
                : existingFiscalYears.get(0)
                        .getEndDate()
                        .plusDays(1);
        dialog.periodField.setPeriod(startDate, startDate.plusYears(1)
                .minusDays(1));
        return dialog;
    }

    /**
     * 회계 연도의 이름과 기간을 수정하는 다이얼로그를 만든다.
     *
     * @param onSaved 저장에 성공한 뒤 실행할 동작
     */
    static FiscalYearEntryDialog forUpdate(Frame owner, FiscalYearDto fiscalYear, SaveAction saveAction,
            Runnable onSaved) {
        FiscalYearEntryDialog dialog = new FiscalYearEntryDialog(owner, "회계 연도 수정", List.of(), false, saveAction,
                onSaved);
        dialog.nameField.setText(fiscalYear.getName());
        dialog.periodField.setPeriod(fiscalYear.getStartDate(), fiscalYear.getEndDate());
        return dialog;
    }

    private FiscalYearEntryDialog(Frame owner, String title, List<FiscalYearDto> existingFiscalYears,
            boolean creating, SaveAction saveAction, Runnable onSaved) {
        super(owner, title, true);
        this.existingFiscalYears = existingFiscalYears;
        this.saveAction = saveAction;
        this.onSaved = onSaved;

        setLayout(new BorderLayout());
        getContentPane().setBackground(UIColors.BACKGROUND);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createEmptyBorder(30, 30, 0, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;

        // 1. 이름
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(new FieldLabel("이름", FontAwesomeSolid.CALENDAR_ALT, SwingConstants.LEFT, ComponentSize.MEDIUM,
                true), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        nameField.setBackground(UIColors.TEXT_FIELD_BACKGROUND);
        formPanel.add(nameField, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 22, 0);
        formPanel.add(new SubFieldLabel("예: 2026년, 2025년 상반기, 2024년 1차", ComponentSize.SMALL), gbc);

        // 2. 기간
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(new FieldLabel("기간", FontAwesomeSolid.CALENDAR_DAY, SwingConstants.LEFT, ComponentSize.MEDIUM,
                true), gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(periodField, gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 0, 0);
        // 레이블에서 줄을 바꾸려면 HTML을 써야 한다.
        String periodHint = creating ? "다른 회계 연도와 겹칠 수 없습니다."
                : "<html>다른 회계 연도와 겹칠 수 없으며,<br>등록된 수입/지출의 날짜를 모두 포함해야 합니다.</html>";
        formPanel.add(new SubFieldLabel(periodHint, ComponentSize.SMALL), gbc);

        // 3. 예산 복사 (추가할 때만)
        if (creating) {
            addCopySection(formPanel, gbc);
        }

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 30, 30));

        saveButton.addActionListener(_ -> onSave());

        buttonPanel.add(cancelButton);
        buttonPanel.add(saveButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 예산 복사란은 추가할 때만 화면에 있다. 화면에 없거나 꺼진 입력란은 Enter로 옮길 때 건너뛴다.
        DialogKeyHandler.install(this)
                .setButtons(cancelButton, saveButton)
                .addField(nameField)
                .addField(periodField.getStartDatePicker())
                .addField(periodField.getEndDatePicker())
                .addField(copyCheckBox)
                .addField(sourceComboBox)
                .trackChanges(this::currentState);

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 저장하지 않은 내용이 있는지 판단할 입력값. 날짜는 아직 날짜로 읽히지 않은 글자도 수정으로 보도록 입력란의 글자로
     * 비교한다.
     */
    private List<Object> currentState() {
        String name = nameField.getText()
                .trim();
        String startDateText = periodField.getStartDatePicker()
                .getDateText();
        String endDateText = periodField.getEndDatePicker()
                .getDateText();
        return Arrays.asList(name, startDateText, endDateText, copyCheckBox.isSelected(),
                sourceComboBox.getSelectedIndex());
    }

    private void addCopySection(JPanel formPanel, GridBagConstraints gbc) {
        gbc.gridy = 6;
        gbc.insets = new Insets(22, 0, 8, 0);
        copyCheckBox.addActionListener(_ -> sourceComboBox.setEnabled(copyCheckBox.isSelected()));
        // 복사해 올 회계 연도가 없으면 고를 수 없다.
        copyCheckBox.setEnabled(!existingFiscalYears.isEmpty());
        formPanel.add(copyCheckBox, gbc);

        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, 8);
        formPanel.add(new FieldLabel("복사할 회계 연도 :", ComponentSize.MEDIUM), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        // 이름은 회계 연도마다 다르므로, 목록에는 이름만 넣고 선택된 위치로 회계 연도를 찾는다.
        for (FiscalYearDto fiscalYear : existingFiscalYears) {
            sourceComboBox.addItem(fiscalYear.getName());
        }
        sourceComboBox.setSelectedIndex(-1);
        sourceComboBox.setEnabled(false);
        formPanel.add(sourceComboBox, gbc);
    }

    /**
     * 입력한 내용을 저장한다. 저장에 성공해야 다이얼로그를 닫으며, 이름이 중복되거나 기간이 겹치는 등 저장할 수 없으면 입력한
     * 내용을 고칠 수 있도록 다이얼로그를 연 채로 오류를 보여준다.
     */
    private void onSave() {
        String name = nameField.getText()
                .trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "회계 연도의 이름을 입력해주세요.");
            return;
        }
        String periodError = periodField.findInputError();
        if (periodError != null) {
            JOptionPane.showMessageDialog(this, periodError);
            return;
        }
        Long sourceFiscalYearId = null;
        if (copyCheckBox.isSelected()) {
            int sourceIndex = sourceComboBox.getSelectedIndex();
            if (sourceIndex < 0) {
                JOptionPane.showMessageDialog(this, "예산을 복사할 회계 연도를 선택해주세요.");
                return;
            }
            sourceFiscalYearId = existingFiscalYears.get(sourceIndex)
                    .getId();
        }

        LocalDate startDate = periodField.getStartDate();
        LocalDate endDate = periodField.getEndDate();
        final Long sourceId = sourceFiscalYearId;
        // 저장하는 동안 저장 버튼을 잠가 두 번 저장되지 않게 한다.
        // 취소 버튼도 잠가 저장 중에는 Esc나 창의 닫기 버튼으로도 닫히지 않게 한다.
        cancelButton.setEnabled(false);
        AsyncTask.run(saveButton,
                () -> {
                    saveAction.save(name, startDate, endDate, sourceId);
                    return null;
                },
                _ -> {
                    dispose();
                    onSaved.run();
                },
                err -> {
                    cancelButton.setEnabled(true);
                    log.error("Failed to save fiscal year: {}", err.getMessage());
                    JOptionPane.showMessageDialog(this, "저장 실패: " + err.getMessage());
                });
    }
}
