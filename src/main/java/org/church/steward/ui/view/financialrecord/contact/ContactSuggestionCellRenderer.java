package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Component;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;

/**
 * 단순 자동 완성 목록의 항목 렌더러.
 * {@link ContactAutocompleteCellRenderer}에서 아이콘을 뺀 형태로, "[구분] 성명"과 작은 글씨의 비고를 두
 * 줄로 표시한다.
 */
public class ContactSuggestionCellRenderer extends JPanel implements ListCellRenderer<ContactSummaryDto> {

    private static final long serialVersionUID = 1L;

    // 텍스트가 비어 있으면 선호 높이가 0이 되므로 공백으로 초기화한다.
    private final FieldLabel nameLabel = new FieldLabel(" ", ComponentSize.MEDIUM);
    private final SubFieldLabel remarksLabel = new SubFieldLabel(" ", ComponentSize.SMALL);

    public ContactSuggestionCellRenderer() {
        setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        add(nameLabel);
        add(remarksLabel);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ContactSummaryDto> list, ContactSummaryDto contact,
            int index, boolean isSelected, boolean cellHasFocus) {
        String remarks = contact.getRemarks();
        boolean hasRemarks = remarks != null && !remarks.isBlank();

        nameLabel.setText(FinancialRecordFormats.contactLabel(contact.getType(), contact.getName()));
        remarksLabel.setText(hasRemarks ? remarks : " ");
        setToolTipText(hasRemarks ? remarks : null);

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        nameLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_MAIN);
        remarksLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_SUB);
        return this;
    }
}
