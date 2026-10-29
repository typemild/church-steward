package org.church.steward.ui.view.financialrecord;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.financialrecord.entity.FinancialRecordType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.contact.ContactChipField;
import org.church.steward.ui.view.financialrecord.contact.HierarchicalContactSuggester;

/**
 * 선택한 여러 수입/지출 내역의 관계자를 한 번에 바꾸기 위해 관계자를 고르는 다이얼로그. 입력 다이얼로그와 같이 수입은 봉헌자를 여러 명
 * 고를 수 있고 그룹 기능을 쓰며, 지출은 그룹 기능 없이 수취인 한 명만 고른다.
 */
class BulkContactChangeDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ContactChipField contactField = new ContactChipField();
    /** 관계자를 고르지 않고 확인했을 때의 안내 */
    private final String emptyContactMessage;

    private boolean confirmed = false;

    /**
     * @param type            내역들의 구분
     * @param count           바꿀 내역의 건수
     * @param initialContacts 미리 칩으로 넣어 둘 관계자들(현재 정보). 선택한 내역들의 관계자가 모두 같을 때 그
     *                        관계자들이며, 아니면 빈 목록이다. 칩에는 현재 이름을 보여준다.
     */
    BulkContactChangeDialog(Window owner, ContactController contactController,
            ContactGroupController contactGroupController, FinancialRecordType type, int count,
            List<ContactDto> initialContacts) {
        super(owner, type == FinancialRecordType.INCOME ? "봉헌자 일괄 변경" : "수취인 일괄 변경",
                ModalityType.APPLICATION_MODAL);
        boolean income = type == FinancialRecordType.INCOME;
        String contactLabel = income ? "봉헌자" : "수취인";
        this.emptyContactMessage = income ? "봉헌자를 목록에서 선택해주세요." : "수취인을 목록에서 선택해주세요.";

        HierarchicalContactSuggester suggester = new HierarchicalContactSuggester(contactController,
                contactGroupController, contactField);
        suggester.setGroupEnabled(income);
        contactField.setSuggester(suggester);
        contactField.setMaxContacts(income ? 0 : 1);
        // 띄우기 전에 넣어야 닫을 때 변경 여부 판단의 기준이 된다.
        for (ContactDto contact : initialContacts) {
            contactField.addContact(contact.getId(), contact.getType(), contact.getName(), contact.getRemarks());
        }

        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIColors.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        String description = income
                ? "선택한 수입 " + count + "건의 봉헌자를 모두 아래 봉헌자로 변경합니다.\n봉헌자는 여러 명을 선택할 수 있습니다."
                : "선택한 지출 " + count + "건의 수취인을 모두 아래 수취인으로 변경합니다.\n수취인은 한 명만 선택할 수 있습니다.";
        mainPanel.add(new DescriptionLabel(description), BorderLayout.NORTH);

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 8));
        fieldPanel.setOpaque(false);
        fieldPanel.add(new FieldLabel(contactLabel, ComponentSize.MEDIUM, true), BorderLayout.NORTH);
        fieldPanel.add(contactField, BorderLayout.CENTER);
        mainPanel.add(fieldPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton okButton = new PrimaryButton("변경", ComponentSize.MEDIUM);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 설명 문구(JTextArea)는 폭이 정해진 뒤에야 줄바꿈된 높이를 알 수 있으므로, 폭을 정해 배치한 뒤 높이를 다시 맞춘다.
        // (ContactMappingDialog와 같다)
        setResizable(false);
        pack();
        setSize(new Dimension(Math.max(getWidth(), 460), getHeight()));
        validate();
        setSize(new Dimension(getWidth(), getPreferredSize().height));
        setLocationRelativeTo(owner);

        okButton.addActionListener(_ -> confirm());

        // 자동 완성 목록이 떠 있는 동안의 Enter와 Esc는 목록이 먼저 처리한다. 관계자는 칩으로 확정된 것만 변경으로 본다.
        DialogKeyHandler.install(this)
                .setButtons(cancelButton, okButton)
                .addField(contactField)
                .trackChanges(contactField::getContactIds);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                contactField.requestInputFocus();
            }
        });
    }

    private void confirm() {
        if (contactField.getContactIds()
                .isEmpty()) {
            JOptionPane.showMessageDialog(this, emptyContactMessage, "입력 오류", JOptionPane.ERROR_MESSAGE);
            contactField.requestInputFocus();
            return;
        }
        confirmed = true;
        dispose();
    }

    boolean isConfirmed() {
        return confirmed;
    }

    /**
     * 칩으로 확정된 관계자들의 ID를 칩의 순서대로 반환한다.
     */
    List<Long> getContactIds() {
        return List.copyOf(contactField.getContactIds());
    }
}
