package org.church.steward.ui.view.settings.contact;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.Consumer;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.label.FieldLabel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

public class ContactSearchPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final StyledComboBox<Object> typeCombo;
    private final StyledTextField nameField;
    private final StyledTextField remarksField;
    private final StyledCheckBox includeHiddenCheck;

    public ContactSearchPanel(Consumer<ContactSearchRequestDto> onSearch) {
        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();

        // 구분 레이블
        gbc.insets = new Insets(5, 0, 5, 7);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new FieldLabel("구분", FontAwesomeSolid.LIST_ALT, SwingConstants.LEFT, ComponentSize.MEDIUM), gbc);

        // typeCombo
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ContactType[] types = ContactType.values();
        Object[] items = new Object[types.length + 1];
        items[0] = "전체";
        System.arraycopy(types, 0, items, 1, types.length);

        typeCombo = new StyledComboBox<>(items, ComponentSize.MEDIUM);
        typeCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ContactType contactType) {
                    setText(contactType.getLabel());
                } else if ("전체".equals(value)) {
                    setText("전체");
                }
                return c;
            }
        });
        add(typeCombo, gbc);

        // 이름 레이블
        gbc.insets = new Insets(5, 0, 5, 7);
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new FieldLabel("이름", FontAwesomeSolid.USER, SwingConstants.LEFT, ComponentSize.MEDIUM), gbc);

        // nameField
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        nameField = new StyledTextField(ComponentSize.MEDIUM);
        add(nameField, gbc);

        // 비고 레이블
        gbc.insets = new Insets(5, 0, 5, 7);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        add(new FieldLabel("비고", FontAwesomeSolid.EDIT, SwingConstants.LEFT, ComponentSize.MEDIUM), gbc);

        // remarksField
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        remarksField = new StyledTextField(ComponentSize.MEDIUM);
        add(remarksField, gbc);

        // includeHiddenCheck
        gbc.insets = new Insets(5, 0, 5, 20);
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1.0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        includeHiddenCheck = new StyledCheckBox("숨긴 관계자 포함", ComponentSize.MEDIUM);
        includeHiddenCheck.setSelected(true);
        add(includeHiddenCheck, gbc);

        // 조회 버튼
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        PrimaryButton searchBtn = new PrimaryButton("조회", ComponentSize.MEDIUM);
        searchBtn.addActionListener(_ -> onSearch.accept(getSearchRequest()));
        searchBtn.setPreferredSize(new Dimension(70, 0));
        searchBtn.setMinimumSize(new Dimension(70, 0));
        add(searchBtn, gbc);
    }

    public ContactSearchRequestDto getSearchRequest() {
        Object selected = typeCombo.getSelectedItem();
        ContactType type = (selected instanceof ContactType t) ? t : null;

        return ContactSearchRequestDto.builder()
                .type(type)
                .name(nameField.getText().trim())
                .remarks(remarksField.getText().trim())
                .includeHidden(includeHiddenCheck.isSelected())
                .build();
    }
}
