package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordRequestDto;
import org.church.steward.domain.financialrecord.dto.FinancialRecordResponseDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.AmountField;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.budget.BudgetSelectBox;
import org.church.steward.ui.view.financialrecord.contact.ContactChipField;
import org.church.steward.ui.view.financialrecord.contact.HierarchicalContactSuggester;

import lombok.extern.slf4j.Slf4j;

/**
 * 수입/지출을 추가하거나 수정하기 위한 다이얼로그
 */
@Slf4j
public class FinancialRecordEntryDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final FiscalYearDto fiscalYear;
    private final ContactController contactController;

    private final StyledComboBox<FinancialRecordType> typeCombo = new StyledComboBox<>(FinancialRecordType.values(),
            ComponentSize.MEDIUM);
    private final StyledDatePicker datePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final BudgetSelectBox budgetSelectBox;
    private final FieldLabel contactLabel = new FieldLabel("", ComponentSize.MEDIUM, true);
    private final ContactChipField contactField = new ContactChipField();
    private final HierarchicalContactSuggester contactSuggester;
    private final StyledTextField descriptionField = new StyledTextField(28, ComponentSize.MEDIUM);
    private final AmountField amountField = new AmountField(ComponentSize.MEDIUM);
    private final StyledTextField remarksField = new StyledTextField(ComponentSize.MEDIUM);

    private final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
    private final DialogKeyHandler keyHandler;

    private boolean confirmed = false;
    private boolean continueRequested = false;

    /**
     * @param fiscalYear   수입/지출이 속할 회계 연도. 예산은 이 회계 연도에서 선택하고, 날짜는 이 회계 연도의 기간 안에서만
     *                     입력할 수 있다.
     * @param type         초기 구분(수입 또는 지출)
     * @param typeEditable 구분을 바꿀 수 있는지 여부
     */
    public FinancialRecordEntryDialog(Window owner, String title, BudgetController budgetController,
            ContactController contactController, ContactGroupController contactGroupController,
            FiscalYearDto fiscalYear, FinancialRecordType type, boolean typeEditable) {
        super(owner, title, ModalityType.APPLICATION_MODAL);
        this.fiscalYear = fiscalYear;
        this.contactController = contactController;
        this.budgetSelectBox = new BudgetSelectBox(budgetController, fiscalYear::getId, toBudgetType(type), true);
        this.contactSuggester = new HierarchicalContactSuggester(contactController, contactGroupController,
                contactField);
        contactField.setSuggester(contactSuggester);

        setLayout(new BorderLayout());
        this.keyHandler = DialogKeyHandler.install(this);

        initComponents(type, typeEditable);
        add(createFormPanel(), BorderLayout.CENTER);
        buttonPanel.setBackground(UIColors.BACKGROUND);
        layoutButtons(false);
        add(buttonPanel, BorderLayout.SOUTH);

        keyHandler.addField(typeCombo)
                .addField(datePicker)
                .addField(budgetSelectBox, this::openBudgetDialogIfEmpty)
                .addField(contactField)
                .addField(descriptionField)
                .addField(amountField)
                .addField(remarksField)
                .trackChanges(this::currentState);

        pack();
        setLocationRelativeTo(owner);
        setResizable(false);
    }

    private void initComponents(FinancialRecordType type, boolean typeEditable) {
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FinancialRecordType recordType) {
                    setText(recordType.getLabel());
                }
                return this;
            }
        });
        typeCombo.setSelectedItem(type);
        typeCombo.setEnabled(typeEditable);
        typeCombo.addActionListener(_ -> applyFinancialRecordType(getFinancialRecordType()));

        datePicker.setDateRange(fiscalYear.getStartDate(), fiscalYear.getEndDate());
        datePicker.getComponentDateTextField()
                .setToolTipText(periodText());
        datePicker.setDate(defaultDate());

        applyFinancialRecordType(type);
    }

    /**
     * 날짜의 초기값. 오늘이 회계 연도의 기간 안에 있으면 오늘, 아니면 기간에서 오늘과 가장 가까운 날(시작일 또는 종료일)이다.
     */
    private LocalDate defaultDate() {
        LocalDate today = LocalDate.now();
        if (today.isBefore(fiscalYear.getStartDate())) {
            return fiscalYear.getStartDate();
        }
        if (today.isAfter(fiscalYear.getEndDate())) {
            return fiscalYear.getEndDate();
        }
        return today;
    }

    private String periodText() {
        return String.format("%s 회계 연도: %s ~ %s", fiscalYear.getName(), fiscalYear.getStartDate(),
                fiscalYear.getEndDate());
    }

    private JPanel createFormPanel() {
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 15, 25));
        formPanel.setBackground(UIColors.BACKGROUND);

        int row = 0;
        row = addRow(formPanel, new FieldLabel("구분", ComponentSize.MEDIUM, true), typeCombo, row);
        row = addRow(formPanel, new FieldLabel("날짜", ComponentSize.MEDIUM, true), datePicker, row);
        row = addRow(formPanel, new FieldLabel("예산", ComponentSize.MEDIUM, true), budgetSelectBox, row);
        row = addRow(formPanel, contactLabel, contactField, row);
        row = addRow(formPanel, new FieldLabel("내용", ComponentSize.MEDIUM, true), descriptionField, row);
        row = addRow(formPanel, new FieldLabel("금액", ComponentSize.MEDIUM, true), amountField, row);
        addRow(formPanel, new FieldLabel("비고", ComponentSize.MEDIUM, true), remarksField, row);

        return formPanel;
    }

    /**
     * 레이블과 입력 컴포넌트를 위아래로 배치한다.
     *
     * @return 다음에 사용할 행 번호
     */
    private int addRow(JPanel formPanel, FieldLabel label, Component field, int row) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.gridx = 0;

        gbc.insets = new Insets(8, 0, 0, 0);
        gbc.gridy = row;
        formPanel.add(label, gbc);

        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.gridy = row + 1;
        formPanel.add(field, gbc);

        return row + 2;
    }

    /**
     * 버튼을 배치한다. 주 버튼은 항상 맨 오른쪽에 둔다.
     * 연속 입력을 쓰면 여러 건을 이어서 입력하는 것이 주된 흐름이므로 "저장 후 계속"이 주 버튼이 되고, "저장"은 "저장 후 닫기"라는
     * 보조 버튼이 된다.
     */
    private void layoutButtons(boolean continuousEntry) {
        buttonPanel.removeAll();

        SecondaryButton btnCancel = new SecondaryButton("취소", ComponentSize.MEDIUM);
        JButton btnSave = continuousEntry
                ? new SecondaryButton("저장 후 닫기", ComponentSize.MEDIUM)
                : new PrimaryButton("저장", ComponentSize.MEDIUM);
        btnSave.addActionListener(_ -> onConfirm(false));

        buttonPanel.add(btnCancel);
        buttonPanel.add(btnSave);

        JButton btnPrimary = btnSave;
        if (continuousEntry) {
            PrimaryButton btnContinue = new PrimaryButton("저장 후 계속", ComponentSize.MEDIUM);
            btnContinue.setToolTipText("저장한 뒤 같은 구분, 날짜, 예산으로 계속 입력합니다.");
            btnContinue.addActionListener(_ -> onConfirm(true));
            buttonPanel.add(btnContinue);
            btnPrimary = btnContinue;
        }
        // 버튼을 새로 만들었으므로 다시 지정한다. 연속 입력이면 마지막 입력란(비고)의 Enter가 "저장 후 계속"이 된다.
        keyHandler.setButtons(btnCancel, btnPrimary);
    }

    /**
     * 예산 입력란의 Enter. 예산이 선택되어 있으면 다음 입력란으로 옮기고, 없으면 예산 선택 다이얼로그를 연다.
     *
     * @return 다이얼로그를 열었으면 true
     */
    private boolean openBudgetDialogIfEmpty() {
        if (budgetSelectBox.getBudgetId() != null) {
            return false;
        }
        budgetSelectBox.openDialog();
        return true;
    }

    /**
     * 저장하지 않은 내용이 있는지 판단할 입력값. 날짜는 아직 날짜로 읽히지 않은 글자도 수정으로 보도록 입력란의 글자로
     * 비교하고, 관계자는 칩으로 확정된 것만 본다.
     */
    private List<Object> currentState() {
        String description = descriptionField.getText()
                .trim();
        String remarks = remarksField.getText()
                .trim();
        return Arrays.asList(getFinancialRecordType(), datePicker.getDateText(), budgetSelectBox.getBudgetId(),
                contactField.getContactIds(), description, amountField.getAmount(), remarks);
    }

    /**
     * "저장 후 계속" 버튼을 보여준다. 추가 다이얼로그에서만 사용한다.
     */
    public void enableContinuousEntry() {
        layoutButtons(true);
        pack();
        setLocationRelativeTo(getOwner());
    }

    /**
     * 구분에 맞게 예산 유형, 관계자 명칭과 입력 가능 인원, 그룹 기능 사용 여부를 바꾼다.
     * 수입은 봉헌자를 여러 명 입력할 수 있고 그룹 기능을 쓰지만, 지출은 수취인 한 명만 입력하며 그룹 기능을 쓰지 않는다.
     * 어느 쪽이든 최소 한 명은 입력해야 한다.
     */
    private void applyFinancialRecordType(FinancialRecordType type) {
        boolean income = type == FinancialRecordType.INCOME;

        budgetSelectBox.setAllowedType(toBudgetType(type));
        contactLabel.setText(income ? "봉헌자" : "수취인");
        contactField.setMaxContacts(income ? 0 : 1);
        contactSuggester.setGroupEnabled(income);
    }

    private static BudgetType toBudgetType(FinancialRecordType type) {
        return type == FinancialRecordType.INCOME ? BudgetType.INCOME : BudgetType.EXPENSE;
    }

    /**
     * 예산을 미리 선택해 둔다. 예산은 현재 구분과 같은 유형이어야 한다.
     *
     * @param budgetPath 전체 예산명("예산A > 예산B > 예산C")
     */
    public void setBudget(Long budgetId, String budgetPath) {
        budgetSelectBox.setBudget(budgetId, toBudgetType(getFinancialRecordType()), budgetPath);
    }

    public void setDate(LocalDate date) {
        datePicker.setDate(date);
    }

    public LocalDate getDate() {
        return datePicker.getDate();
    }

    public Long getBudgetId() {
        return budgetSelectBox.getBudgetId();
    }

    /**
     * 선택된 예산의 전체 예산명을 반환한다. 선택된 예산이 없으면 null을 반환한다.
     */
    public String getBudgetPath() {
        return budgetSelectBox.getBudgetPath();
    }

    /**
     * 수정할 수입/지출 내역의 값으로 입력 필드를 채운다.
     */
    public void setRecord(FinancialRecordResponseDto record) {
        datePicker.setDate(record.getDate());
        setBudget(record.getBudgetId(), record.getBudgetName());
        descriptionField.setText(record.getDescription());
        amountField.setAmount(record.getAmount());
        remarksField.setText(record.getRemarks());

        // 칩에는 내역에 기록된 당시의 이름을 그대로 보여준다.
        contactField.clear();
        List<Long> contactIds = record.getContactIds();
        List<String> contactNames = record.getContactNames();
        if (contactIds != null && contactNames != null) {
            for (int i = 0; i < contactIds.size() && i < contactNames.size(); i++) {
                contactField.addRecordedContact(contactIds.get(i), contactNames.get(i));
            }
        }
        loadCurrentContactInfo(contactField.getContactIds());
    }

    /**
     * 칩의 툴팁에 보여줄 관계자의 현재 정보를 조회한다. 조회에 실패해도 입력은 계속할 수 있으며, 툴팁에는 기록된 이름이 남는다.
     */
    private void loadCurrentContactInfo(List<Long> contactIds) {
        if (contactIds.isEmpty()) {
            return;
        }

        // 입력 필드가 잠기지 않도록 루트 페인을 잠금 대상으로 지정한다.
        AsyncTask.run(getRootPane(),
                () -> contactController.getContactsByIds(contactIds),
                contacts -> {
                    for (ContactDto contact : contacts) {
                        contactField.updateCurrentInfo(contact.getId(), contact.getType(), contact.getName(),
                                contact.getRemarks());
                    }
                },
                err -> log.error("Failed to load current contact info", err));
    }

    /**
     * @param continueAfter true이면 확정한 뒤 이어서 입력하겠다는 뜻이다. 다이얼로그를 다시 여는 것은 호출한 쪽의 몫이다.
     */
    private void onConfirm(boolean continueAfter) {
        // 기간 밖의 날짜는 날짜 피커가 받아들이지 않으므로, 날짜가 비어 있으면 기간도 함께 알려준다.
        if (datePicker.getDate() == null) {
            showInputError("날짜를 입력해주세요.\n(" + periodText() + ")");
            datePicker.requestFocus();
            return;
        }

        if (budgetSelectBox.getBudgetId() == null) {
            showInputError("예산을 선택해주세요.");
            return;
        }

        if (contactField.getContactIds()
                .isEmpty()) {
            showInputError(getFinancialRecordType() == FinancialRecordType.INCOME ? "봉헌자를 입력해주세요."
                    : "수취인을 입력해주세요.");
            contactField.requestInputFocus();
            return;
        }

        if (amountField.getAmount() <= 0) {
            showInputError("금액을 입력해주세요.");
            amountField.requestFocus();
            return;
        }

        confirmed = true;
        continueRequested = continueAfter;
        dispose();
    }

    private void showInputError(String message) {
        JOptionPane.showMessageDialog(this, message, "입력 오류", JOptionPane.ERROR_MESSAGE);
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * "저장 후 계속" 버튼으로 확정했는지 여부를 반환한다.
     */
    public boolean isContinueRequested() {
        return continueRequested;
    }

    public FinancialRecordType getFinancialRecordType() {
        return (FinancialRecordType) typeCombo.getSelectedItem();
    }

    /**
     * 입력된 값으로 수입/지출 요청을 만든다.
     */
    public FinancialRecordRequestDto toRequest() {
        return FinancialRecordRequestDto.builder()
                .type(getFinancialRecordType())
                .fiscalYearId(fiscalYear.getId())
                .date(datePicker.getDate())
                .budgetId(budgetSelectBox.getBudgetId())
                .description(descriptionField.getText()
                        .trim())
                .amount(amountField.getAmount())
                .remarks(remarksField.getText()
                        .trim())
                .contactIds(new ArrayList<>(contactField.getContactIds()))
                .build();
    }
}
