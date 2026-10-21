package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 계층형 자동 완성의 서브 목록(그룹 목록) 렌더러.
 * 좌측에 휴지통(삭제) 아이콘을 수직 가운데로 두고, 우측에 구성원들을 칩으로 표시한다.
 * 칩이 한 줄에 다 들어가지 않으면 여러 줄에 나눠 배치하며, 칩에 마우스를 올리면 그 구성원의 정보를 툴팁으로 보여준다.
 */
public class ContactGroupCellRenderer extends JPanel implements ListCellRenderer<GroupInfoDto> {

    private static final long serialVersionUID = 1L;

    /** 항목의 너비. 항목의 높이가 칩의 줄 수에 따라 정해지므로 너비는 고정한다. */
    public static final int CELL_WIDTH = 260;

    private static final int LEFT_PADDING = 8;
    private static final int RIGHT_PADDING = 8;
    private static final int VERTICAL_PADDING = 6;
    private static final int ICON_SIZE = UIFonts.FONT_SIZE_SMALL;
    private static final int ICON_CHIP_GAP = 8;
    private static final int CHIP_GAP = 4;
    private static final int CHIPS_X = LEFT_PADDING + ICON_SIZE + ICON_CHIP_GAP;
    private static final int CHIPS_WIDTH = CELL_WIDTH - CHIPS_X - RIGHT_PADDING;

    private final JLabel deleteIconLabel = new JLabel(
            FontIcon.of(FontAwesomeSolid.TRASH_ALT, ICON_SIZE, UIColors.TEXT_SUB), SwingConstants.CENTER);
    private final List<ContactChip> chips = new ArrayList<>();

    public ContactGroupCellRenderer() {
        // 칩과 아이콘은 getListCellRendererComponent에서 직접 배치한다.
        setLayout(null);
    }

    /**
     * 셀 안에서의 x 좌표가 휴지통(삭제) 아이콘 영역인지 확인한다.
     */
    public static boolean isDeleteZone(int x) {
        return x <= CHIPS_X;
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends GroupInfoDto> list, GroupInfoDto group, int index,
            boolean isSelected, boolean cellHasFocus) {
        removeAll();
        chips.clear();

        int x = CHIPS_X;
        int y = VERTICAL_PADDING;
        int rowHeight = ICON_SIZE;
        for (ContactSummaryDto member : group.getMembers()) {
            ContactChip chip = new ContactChip(member.getId(), member.getName());
            chip.setCurrentInfo(member.getType(), member.getName(), member.getRemarks());

            Dimension chipSize = chip.getPreferredSize();
            int chipWidth = Math.min(chipSize.width, CHIPS_WIDTH);
            rowHeight = Math.max(rowHeight, chipSize.height);
            if (x > CHIPS_X && x + chipWidth > CHIPS_X + CHIPS_WIDTH) {
                x = CHIPS_X;
                y += rowHeight + CHIP_GAP;
            }
            chip.setBounds(x, y, chipWidth, chipSize.height);
            x += chipWidth + CHIP_GAP;

            chips.add(chip);
            add(chip);
        }

        int height = y + rowHeight + VERTICAL_PADDING;
        deleteIconLabel.setBounds(LEFT_PADDING, 0, ICON_SIZE, height);
        add(deleteIconLabel);

        setPreferredSize(new Dimension(CELL_WIDTH, height));
        setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
        return this;
    }

    /**
     * 마우스 위치에 있는 칩의 툴팁을 반환한다. 목록은 툴팁을 렌더러에게만 묻고 그 안의 칩에게는 묻지 않기 때문에 직접 찾는다.
     */
    @Override
    public String getToolTipText(MouseEvent e) {
        for (ContactChip chip : chips) {
            if (chip.getBounds()
                    .contains(e.getPoint())) {
                return chip.getToolTipText();
            }
        }
        return null;
    }
}
