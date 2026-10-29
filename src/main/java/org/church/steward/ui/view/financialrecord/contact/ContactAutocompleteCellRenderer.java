package org.church.steward.ui.view.financialrecord.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
 * 계층형 자동 완성 목록의 항목 렌더러. 좌측에 연필(수정)/휴지통(삭제) 아이콘을 두고, 우측에 "[구분] 성명"과 작은 글씨의 비고를 두
 * 줄로 표시한다. 그룹 기능을 쓸 때는 소속 그룹이 있는 관계자의 오른쪽 끝에 서브 목록이 있다는 세모(▶)를 그린다. 비고가 없으면 비고
 * 줄을 빼므로 항목마다 높이가 다르다. 다 담지 못한 관계자가 있다는 안내 항목은 아이콘 없이 작은 글씨 한 줄로 오른쪽에 붙여 따로
 * 그린다.
 */
public class ContactAutocompleteCellRenderer extends JPanel implements ListCellRenderer<ContactAutocompleteItem> {

    private static final long serialVersionUID = 1L;

    private static final int LEFT_PADDING = 6;
    private static final int ICON_ZONE_WIDTH = 24;
    private static final int ICON_SIZE = UIFonts.FONT_SIZE_SMALL;

    private final FontIcon editIcon = FontIcon.of(FontAwesomeSolid.PENCIL_ALT, ICON_SIZE, UIColors.TEXT_SUB);
    private final FontIcon deleteIcon = FontIcon.of(FontAwesomeSolid.TRASH_ALT, ICON_SIZE, UIColors.TEXT_SUB);
    private final FontIcon newEntryIcon = FontIcon.of(FontAwesomeSolid.PLUS, ICON_SIZE, UIColors.TEXT_HIGHLIGHT);
    private final FontIcon groupArrowIcon = FontIcon.of(FontAwesomeSolid.CARET_RIGHT, ICON_SIZE, UIColors.TEXT_SUB);

    private final JLabel firstIconLabel = new JLabel((String) null, SwingConstants.CENTER);
    private final JLabel secondIconLabel = new JLabel((String) null, SwingConstants.CENTER);
    // 소속 그룹이 있는 관계자의 오른쪽에 세모를 그려 서브 목록이 있다는 것을 알린다. 그룹 기능을 쓸 때만 보이며,
    // 그룹이 없는 항목도 자리는 차지해서 이름과 비고의 폭이 항목마다 달라지지 않게 한다.
    private final JLabel groupArrowLabel = new JLabel((String) null, SwingConstants.CENTER);
    private boolean groupEnabled = true;
    // 텍스트가 비어 있으면 선호 높이가 0이 되므로 공백으로 초기화한다.
    private final FieldLabel nameLabel = new FieldLabel(" ", ComponentSize.MEDIUM);
    private final SubFieldLabel remarksLabel = new SubFieldLabel(" ", ComponentSize.SMALL);

    private final SubFieldLabel moreResultsLabel = new SubFieldLabel(" ", SwingConstants.RIGHT, ComponentSize.SMALL);
    // 안내 항목에 마우스를 올리면 밑줄을 그어 누를 수 있다는 것을 알린다.
    private final Font normalFont = moreResultsLabel.getFont();
    private final Font hoverFont = ContactSuggestions.underlined(normalFont);
    private boolean moreResultsHovered = false;

    public ContactAutocompleteCellRenderer() {
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(4, LEFT_PADDING, 4, 8));

        JPanel iconPanel = new JPanel(new GridLayout(1, 2));
        iconPanel.setOpaque(false);
        iconPanel.setPreferredSize(new Dimension(ICON_ZONE_WIDTH * 2, 0));
        iconPanel.add(firstIconLabel);
        iconPanel.add(secondIconLabel);

        // BorderLayout은 숨긴 컴포넌트에 자리를 주지 않으므로, 비고 줄을 숨기면 항목이 한 줄 높이가 된다.
        JPanel textPanel = new JPanel(new BorderLayout());
        textPanel.setOpaque(false);
        textPanel.add(nameLabel, BorderLayout.CENTER);
        textPanel.add(remarksLabel, BorderLayout.SOUTH);

        groupArrowLabel.setPreferredSize(new Dimension(ICON_SIZE, 0));

        add(iconPanel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);
        add(groupArrowLabel, BorderLayout.EAST);

        moreResultsLabel.setOpaque(true);
        moreResultsLabel.setBorder(BorderFactory.createEmptyBorder(4, LEFT_PADDING, 4, 8));
    }

    /**
     * 그룹 기능 사용 여부를 설정한다. 사용하지 않으면 서브 목록이 뜨지 않으므로 세모를 그리지 않고 자리도 비우지 않는다.
     */
    public void setGroupEnabled(boolean groupEnabled) {
        this.groupEnabled = groupEnabled;
        groupArrowLabel.setVisible(groupEnabled);
    }

    /**
     * 안내 항목 위에 마우스가 있는지 설정한다. 렌더러는 마우스 위치를 알 수 없으므로 목록 쪽에서 알려 주며, 바꾼 뒤에는 목록을 다시 그려야
     * 한다.
     */
    public void setMoreResultsHovered(boolean moreResultsHovered) {
        this.moreResultsHovered = moreResultsHovered;
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
        if (item.isMoreResults()) {
            // 키보드로 고를 수 없는 안내 항목이므로 선택 색을 쓰지 않는다.
            moreResultsLabel.setText(ContactSuggestions.moreResultsText(item.getRemainingCount()));
            moreResultsLabel.setFont(moreResultsHovered ? hoverFont : normalFont);
            moreResultsLabel.setBackground(list.getBackground());
            return moreResultsLabel;
        }

        if (item.isNewEntry()) {
            firstIconLabel.setIcon(null);
            secondIconLabel.setIcon(newEntryIcon);
            nameLabel.setText("신규 추가");
            remarksLabel.setText("새 관계자를 등록합니다.");
            remarksLabel.setVisible(true);
            groupArrowLabel.setIcon(null);
            setToolTipText(null);
        } else {
            ContactAutocompleteDto contact = item.getContact();
            String remarks = contact.getRemarks();
            boolean hasRemarks = remarks != null && !remarks.isBlank();
            boolean hasGroups = contact.getGroups() != null && !contact.getGroups()
                    .isEmpty();

            groupArrowLabel.setIcon(groupEnabled && hasGroups ? groupArrowIcon : null);
            firstIconLabel.setIcon(editIcon);
            secondIconLabel.setIcon(deleteIcon);
            nameLabel.setText(FinancialRecordFormats.contactLabel(contact.getType(), contact.getName()));
            remarksLabel.setText(hasRemarks ? remarks : " ");
            remarksLabel.setVisible(hasRemarks);
            setToolTipText(hasRemarks ? remarks : null);
        }

        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        nameLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_MAIN);
        remarksLabel.setForeground(isSelected ? list.getSelectionForeground() : UIColors.TEXT_SUB);
        return this;
    }
}
