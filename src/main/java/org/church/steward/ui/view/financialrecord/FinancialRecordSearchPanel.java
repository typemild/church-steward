package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.budget.controller.BudgetController;
import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.financialrecord.dto.FinancialRecordSearchRequestDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.AmountField;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.financialrecord.budget.BudgetSelectBox;
import org.church.steward.ui.view.financialrecord.contact.ContactChipField;
import org.church.steward.ui.view.financialrecord.contact.SimpleContactSuggester;
import org.church.steward.ui.view.financialrecord.period.PeriodSelectorPanel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 수입/지출 검색 조건 영역
 */
public class FinancialRecordSearchPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Supplier<Long> fiscalYearIdSupplier;

    private final PeriodSelectorPanel periodSelector;
    private final StyledComboBox<FinancialRecordType> typeCombo = new StyledComboBox<>(ComponentSize.MEDIUM);
    private final BudgetSelectBox budgetSelectBox;
    private final ContactChipField contactField;
    private final AmountField minAmountField = new AmountField(ComponentSize.MEDIUM, true);
    private final AmountField maxAmountField = new AmountField(ComponentSize.MEDIUM, true);
    private final StyledTextField descriptionField = new StyledTextField(ComponentSize.MEDIUM);
    private final StyledTextField remarksField = new StyledTextField(ComponentSize.MEDIUM);
    private final PrimaryButton searchButton = new PrimaryButton("조회", ComponentSize.MEDIUM);
    /** 상세 조건을 모두 비우는 버튼 */
    private final SecondaryButton resetButton = new SecondaryButton("초기화", ComponentSize.MEDIUM);
    /** 조회 기간을 제외한 검색 조건(상세 조건)을 펼치거나 접는 버튼 */
    private final SecondaryButton detailToggleButton = new SecondaryButton("상세 조건", ComponentSize.MEDIUM);
    /** 상세 조건 영역. 평소에는 숨겨 둔다. */
    private final JPanel detailPanel = new JPanel(new GridBagLayout());

    /**
     * @param fiscalYearIdSupplier 사이드바에서 선택된 회계 연도의 ID 공급자
     * @param onSearch             조회 버튼을 누르거나, 텍스트 필드에서 Enter를 입력하거나, 조회 기간이 바뀌었을 때
     *                             실행할 동작
     */
    public FinancialRecordSearchPanel(BudgetController budgetController, ContactController contactController,
            Supplier<Long> fiscalYearIdSupplier, Runnable onSearch) {
        this.fiscalYearIdSupplier = fiscalYearIdSupplier;
        this.periodSelector = new PeriodSelectorPanel(onSearch);
        this.budgetSelectBox = new BudgetSelectBox(budgetController, fiscalYearIdSupplier, null, false);
        this.contactField = new ContactChipField();
        this.contactField.setSuggester(new SimpleContactSuggester(contactController, contactField));

        setLayout(new BorderLayout());
        setOpaque(false);

        initComponents();
        layoutComponents();
        updateDetailToggleButton();

        detailToggleButton.addActionListener(_ -> setDetailExpanded(!detailPanel.isVisible()));
        searchButton.addActionListener(_ -> onSearch.run());
        resetButton.addActionListener(_ -> resetDetailConditions());
        minAmountField.addActionListener(_ -> onSearch.run());
        maxAmountField.addActionListener(_ -> onSearch.run());
        descriptionField.addActionListener(_ -> onSearch.run());
        remarksField.addActionListener(_ -> onSearch.run());
    }

    private void initComponents() {
        // 펼침 방향을 나타내는 화살표 아이콘을 글자 오른쪽에 둔다.
        detailToggleButton.setHorizontalTextPosition(SwingConstants.LEFT);
        detailToggleButton.setIconTextGap(8);

        resetButton.setToolTipText("상세 조건을 모두 지웁니다. 조회 기간은 유지됩니다.");

        typeCombo.setModel(new DefaultComboBoxModel<>(new FinancialRecordType[] {
                null, FinancialRecordType.INCOME, FinancialRecordType.EXPENSE }));
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof FinancialRecordType type) {
                    setText(type.getLabel());
                } else if (value == null) {
                    setText("전체");
                }
                return this;
            }
        });
    }

    private void layoutComponents() {
        // 조회 기간 행. 항상 보이며, 좌측에 조회 기간을, 우측에 상세 조건을 펼치는 버튼을 둔다.
        JPanel periodRow = new JPanel(new BorderLayout(8, 0));
        periodRow.setOpaque(false);
        periodRow.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        periodRow.add(periodSelector, BorderLayout.WEST);
        periodRow.add(detailToggleButton, BorderLayout.EAST);
        add(periodRow, BorderLayout.NORTH);

        // 상세 조건 영역. 평소에는 숨겨 두고 버튼으로 펼친다.
        detailPanel.setOpaque(false);
        detailPanel.setVisible(false);
        add(detailPanel, BorderLayout.CENTER);

        JPanel amountPanel = createAmountPanel();

        addLabel("구분", FontAwesomeSolid.LIST_ALT, 0, 0);
        addField(typeCombo, 1, 0);
        addLabel("예산", FontAwesomeSolid.CHART_PIE, 2, 0);
        addField(budgetSelectBox, 3, 0);

        addLabel("관계자", FontAwesomeSolid.USER, 0, 1);
        addField(contactField, 1, 1);
        addLabel("금액", FontAwesomeSolid.WON_SIGN, 2, 1);
        addField(amountPanel, 3, 1);

        addLabel("내역", FontAwesomeSolid.FILE_ALT, 0, 2);
        addField(descriptionField, 1, 2);
        addLabel("비고", FontAwesomeSolid.STICKY_NOTE, 2, 2);
        addField(remarksField, 3, 2);

        // 조회 버튼 : 상세 조건의 위쪽 두 행을 차지한다.
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        searchButton.setPreferredSize(new Dimension(70, 0));
        searchButton.setMinimumSize(new Dimension(70, 0));
        detailPanel.add(searchButton, gbc);

        // 초기화 버튼 : 조회 버튼 아래, 상세 조건의 마지막 행을 차지한다.
        gbc.gridy = 2;
        gbc.gridheight = 1;
        resetButton.setPreferredSize(new Dimension(70, 0));
        resetButton.setMinimumSize(new Dimension(70, 0));
        detailPanel.add(resetButton, gbc);
    }

    /**
     * 상세 조건을 모두 비운다. 조회 기간은 상세 조건이 아니므로 그대로 둔다. 다른 상세 조건을 바꿀 때와 마찬가지로 자동 조회하지 않는다.
     */
    private void resetDetailConditions() {
        typeCombo.setSelectedIndex(0);
        budgetSelectBox.clear();
        contactField.clear();
        minAmountField.setAmount(null);
        maxAmountField.setAmount(null);
        descriptionField.setText("");
        remarksField.setText("");
        updateDetailToggleButton();
    }

    /**
     * 상세 조건 영역을 펼치거나 접는다. 접어도 입력해 둔 조건은 지우지 않으며 조회에 그대로 적용된다.
     */
    private void setDetailExpanded(boolean expanded) {
        detailPanel.setVisible(expanded);
        updateDetailToggleButton();
        revalidate();
        repaint();
    }

    /**
     * 상세 조건 버튼의 모양을 현재 상태에 맞춘다. 접힌 동안에는 숨겨진 조건이 적용 중임을 알 수 있도록 입력된 조건의 개수를 함께 보여준다.
     */
    private void updateDetailToggleButton() {
        boolean expanded = detailPanel.isVisible();
        int conditionCount = expanded ? 0 : countDetailConditions();

        detailToggleButton.setText(conditionCount > 0 ? "상세 조건 (" + conditionCount + ")" : "상세 조건");
        detailToggleButton.setIcon(FontIcon.of(
                expanded ? FontAwesomeSolid.CHEVRON_UP : FontAwesomeSolid.CHEVRON_DOWN,
                UIFonts.FONT_SIZE_SMALL, UIColors.TEXT_MAIN));
        if (expanded) {
            detailToggleButton.setToolTipText("상세 조건 접기");
        } else if (conditionCount > 0) {
            detailToggleButton.setToolTipText("상세 조건 " + conditionCount + "개가 조회에 적용되어 있습니다.");
        } else {
            detailToggleButton.setToolTipText("상세 조건 펼치기");
        }
    }

    /**
     * 조회 기간을 제외하고 입력된 검색 조건의 개수. 시작 금액과 종료 금액은 하나의 조건으로 센다.
     */
    private int countDetailConditions() {
        int count = 0;
        if (typeCombo.getSelectedItem() != null) {
            count++;
        }
        if (budgetSelectBox.getBudgetId() != null) {
            count++;
        }
        if (!contactField.getContactIds().isEmpty()) {
            count++;
        }
        if (minAmountField.getAmount() != null || maxAmountField.getAmount() != null) {
            count++;
        }
        if (blankToNull(descriptionField.getText()) != null) {
            count++;
        }
        if (blankToNull(remarksField.getText()) != null) {
            count++;
        }
        return count;
    }

    /**
     * 시작 금액 필드 + "~" + 종료 금액 필드. 두 필드는 남는 폭을 똑같이 나눠 갖는다.
     */
    private JPanel createAmountPanel() {
        JPanel amountPanel = new JPanel(new GridBagLayout());
        amountPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        amountPanel.add(minAmountField, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.0;
        gbc.insets = new Insets(0, 8, 0, 8);
        amountPanel.add(new FieldLabel("~", SwingConstants.CENTER, ComponentSize.MEDIUM), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        amountPanel.add(maxAmountField, gbc);

        return amountPanel;
    }

    private void addLabel(String text, FontAwesomeSolid icon, int x, int y) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 7);
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(new FieldLabel(text, icon, SwingConstants.LEFT, ComponentSize.MEDIUM), gbc);
    }

    private void addField(Component field, int x, int y) {
        // 좌우 입력 컬럼의 폭이 내용과 무관하게 같아지도록 선호 폭을 통일한다.
        field.setPreferredSize(new Dimension(0, field.getPreferredSize().height));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(field, gbc);
    }

    /**
     * 현재 입력된 검색 조건으로 검색 요청을 만든다. 관계자 선택 박스에서 칩으로 확정되지 않은 텍스트는 조건에 포함되지 않는다. 시작 금액과
     * 종료 금액은 각각 비워 둘 수 있으며, 비운 쪽은 조건에 포함되지 않는다.
     */
    public FinancialRecordSearchRequestDto getSearchRequest() {
        return FinancialRecordSearchRequestDto.builder()
                .type((FinancialRecordType) typeCombo.getSelectedItem())
                .fiscalYearId(fiscalYearIdSupplier.get())
                .startDate(periodSelector.getStartDate())
                .endDate(periodSelector.getEndDate())
                .budgetId(budgetSelectBox.getBudgetId())
                .contactIds(contactField.getContactIds())
                .minAmount(minAmountField.getAmount())
                .maxAmount(maxAmountField.getAmount())
                .description(blankToNull(descriptionField.getText()))
                .remarks(blankToNull(remarksField.getText()))
                .build();
    }

    /**
     * 조회 기간을 회계 연도의 기간 안으로 제한한다. 회계 연도가 바뀌면 조회 기간도 새 회계 연도의 기간 안으로 옮겨진다.
     */
    public void setFiscalYearPeriod(LocalDate startDate, LocalDate endDate) {
        periodSelector.setLimits(startDate, endDate);
    }

    /**
     * 선택된 예산을 해제한다. 예산은 회계 연도에 속하므로 회계 연도가 바뀌면 호출해야 한다.
     */
    public void clearBudget() {
        budgetSelectBox.clear();
        updateDetailToggleButton();
    }

    private String blankToNull(String text) {
        String trimmed = text.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
