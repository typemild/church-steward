package org.church.steward.ui.view.financialrecord.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 계층형 자동 완성 목록의 항목 렌더러.
 * 좌측에 연필(수정)/휴지통(삭제) 아이콘을 두고, 우측에 "[구분] 성명"과 작은 글씨의 비고를 두 줄로 표시한다.
 */
public class ContactAutocompleteCellRenderer extends JPanel implements ListCellRenderer<ContactAutocompleteItem> {

    private static final long serialVersionUID = 1L;

    private static final int LEFT_PADDING = 6;
    private static final int ICON_ZONE_WIDTH = 24;
    private static final int ICON_SIZE = UIFonts.FONT_SIZE_SMALL;

    private final FontIcon editIcon = FontIcon.of(FontAwesomeSolid.PENCIL_ALT, ICON_SIZE, UIColors.TEXT_SUB);
    private final FontIcon deleteIcon = FontIcon.of(FontAwesomeSolid.TRASH_ALT, ICON_SIZE, UIColors.TEXT_SUB);
    private final FontIcon newEntryIcon = FontIcon.of(FontAwesomeSolid.PLUS, ICON_SIZE, UIColors.TEXT_HIGHLIGHT);

    private final JLabel firstIconLabel = new JLabel((String) null, SwingConstants.CENTER);
    private final JLabel secondIconLabel = new JLabel((String) null, SwingConstants.CENTER);
    // 텍스트가 비어 있으면 선호 높이가 0이 되므로 공백으로 초기화한다.
    private final FieldLabel nameLabel = new FieldLabel(" ", ComponentSize.MEDIUM);
    private final SubFieldLabel remarksLabel = new SubFieldLabel(" ", ComponentSize.SMALL);

    public ContactAutocompleteCellRenderer() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(4, LEFT_PADDING, 4, 8));

        JPanel iconPanel = new JPanel(new GridLayout(1, 2));
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(ICON_ZONE_WIDTH * 2, 0));
        iconPanel.add(firstIconLabel);
        iconPanel.add(secondIconLabel);

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        textPanel.add(nameLabel);
        textPanel.add(remarksLabel);

        add(iconPanel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
    }

    /**
     * 셀 안에서의 x 좌표가 연필(수정) 아이콘 영역인지 확인한다.
     */
    public static boolean isEditZone(int x) {
        return x >= LEFT_PADDING && x < LEFT_PADDING + ICON_ZONE_WIDTH;
    }

    /**
     * 셀 안에서의 x 좌표가 휴지통(삭제) 아이콘 영역인지 확인한다.
     */
    public static boolean isDeleteZone(int x) {
        return x >= LEFT_PADDING + ICON_ZONE_WIDTH && x < LEFT_PADDING + ICON_ZONE_WIDTH * 2;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends ContactAutocompleteItem> list,
            ContactAutocompleteItem item, int index, boolean isSelected, boolean cellHasFocus) {
        if (item.isNewEntry()) {
            firstIconLabel.setIcon(null);
            secondIconLabel.setIcon(newEntryIcon);
            nameLabel.setText("신규 추가");
            remarksLabel.setText("새 관계자를 등록합니다.");
            setToolTipText(null);
        } else {
            ContactAutocompleteDto contact = item.getContact();
            String remarks = contact.getRemarks();
            boolean hasRemarks = remarks != null && !remarks.isBlank();

            firstIconLabel.setIcon(editIcon);
            secondIconLabel.setIcon(deleteIcon);
            nameLabel.setText(FinancialRecordFormats.contactLabel(contact.getType(), contact.getName()));
            remarksLabel.setText(hasRemarks ? remarks : " ");
            setToolTipText(hasRemarks ? remarks : null);
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        nameLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_MAIN);
        remarksLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_SUB);
        return this;
    }
}
