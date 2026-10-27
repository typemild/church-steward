package org.church.steward.ui.view.settings.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.entity.ContactConstraints;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledTextArea;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;

public class ContactDetailDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    public enum Mode {
        ADD, EDIT
    }

    private final Consumer<ContactDto> onSave;

    private final StyledComboBox<ContactType> typeCombo;
    private final StyledTextField nameField;
    private final StyledTextArea remarksArea;
    private final StyledCheckBox hiddenCheck;
    private Long contactId;

    public ContactDetailDialog(Window owner, Mode mode, ContactDto dto,
            Consumer<ContactDto> onSave) {
        super(owner, mode == Mode.ADD ? "관계자 등록" : "관계자 상세 정보", ModalityType.APPLICATION_MODAL);

        this.onSave = onSave;

        setBackground(UIColors.BACKGROUND);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel();
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setOpaque(false);
        add(mainPanel, BorderLayout.CENTER);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 구분과 숨김 여부. 구분은 입력란 폭의 절반만 쓰고 오른쪽 절반에 숨김 여부를 둔다.
        gbc.gridx = 0;
        gbc.gridy = 0;
        formPanel.add(new FieldLabel("구분:", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        typeCombo = new StyledComboBox<>(ContactType.values(), ComponentSize.MEDIUM);
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContactType contactType) {
                    setText(contactType.getLabel());
                }
                return c;
            }
        });
        hiddenCheck = new StyledCheckBox("숨김 처리", ComponentSize.MEDIUM);
        // 포커스 이동(Tab, Enter)에서 뺀다.
        hiddenCheck.setFocusable(false);
        JPanel typeRow = new JPanel(new GridLayout(1, 2, 10, 0));
        typeRow.setOpaque(false);
        typeRow.add(typeCombo);
        typeRow.add(hiddenCheck);
        formPanel.add(typeRow, gbc);

        // 이름
        gbc.gridx = 0;
        gbc.gridy = 1;
        formPanel.add(new FieldLabel("이름:", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        nameField = new StyledTextField(ComponentSize.MEDIUM);
        nameField.setMaxLength(ContactConstraints.NAME_MAX_LENGTH);
        formPanel.add(nameField, gbc);

        // 비고. 마지막 입력란이므로 Enter를 누르면 저장한다.
        gbc.gridx = 0;
        gbc.gridy = 2;
        formPanel.add(new FieldLabel("비고:", ComponentSize.MEDIUM), gbc);
        gbc.gridx = 1;
        remarksArea = new StyledTextArea(3, 20, ComponentSize.MEDIUM);
        remarksArea.setMaxLength(ContactConstraints.REMARKS_MAX_LENGTH);
        formPanel.add(new JScrollPane(remarksArea), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 5, 5, 5);
        formPanel.add(new SubFieldLabel("줄을 바꾸려면 Shift + Enter를 누르세요.", SwingConstants.RIGHT,
                ComponentSize.SMALL), gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        SecondaryButton cancelBtn = new SecondaryButton("취소", ComponentSize.MEDIUM);

        PrimaryButton saveBtn = new PrimaryButton("저장", ComponentSize.MEDIUM);
        saveBtn.addActionListener(_ -> handleSave());

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);

        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        if (dto != null) {
            this.contactId = dto.getId();
            typeCombo.setSelectedItem(dto.getType());
            nameField.setText(dto.getName());
            remarksArea.setText(dto.getRemarks());
            hiddenCheck.setSelected(dto.isHidden());
        }
        // 비고는 여러 줄 입력란이지만 다른 입력란처럼 Tab과 Enter로 옮기고, 줄바꿈은 Shift+Enter로 한다.
        DialogKeyHandler.install(this)
                .setButtons(cancelBtn, saveBtn)
                .addField(typeCombo)
                .addField(nameField)
                .addField(remarksArea)
                .trackChanges(this::currentState);

        pack();
        setLocationRelativeTo(owner);
    }

    /**
     * 저장하지 않은 내용이 있는지 판단할 입력값
     */
    private List<Object> currentState() {
        String name = nameField.getText().trim();
        String remarks = remarksArea.getText().trim();
        return Arrays.asList(typeCombo.getSelectedItem(), name, remarks, hiddenCheck.isSelected());
    }

    private void handleSave() {
        if (nameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "이름을 입력해주세요.", "알림", JOptionPane.WARNING_MESSAGE);
            return;
        }
        ContactDto dto = ContactDto.builder()
                .id(contactId)
                .type((ContactType) typeCombo.getSelectedItem())
                .name(nameField.getText().trim())
                .remarks(remarksArea.getText().trim())
                .isHidden(hiddenCheck.isSelected())
                .build();
        onSave.accept(dto);
        dispose();
    }

}
