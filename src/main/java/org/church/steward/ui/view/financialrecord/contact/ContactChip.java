package org.church.steward.ui.view.financialrecord.contact;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.financialrecord.FinancialRecordFormats;

/**
 * 관계자 선택 박스에서 선택된 관계자 한 명을 나타내는 칩(chip). 칩에 표시하는 이름과 툴팁의 이름은 다를 수 있다. 칩에는 수입/지출
 * 내역에 기록된 과거 이름이 올 수 있지만, 툴팁은 항상 관계자의 현재 정보를 첫 줄에 "[구분] 성명", 둘째 줄에 비고 형식으로
 * 보여준다.
 */
public class ContactChip extends JLabel {

    private static final long serialVersionUID = 1L;

    private static final Font FONT = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, UIFonts.FONT_SIZE_SMALL);

    private final Long contactId;
    private final String contactName;

    /**
     * @param contactName 칩에 표시할 이름. 현재 정보를 설정하기 전까지는 툴팁에도 이 이름을 쓴다.
     */
    public ContactChip(Long contactId, String contactName) {
        super(contactName);
        this.contactId = contactId;
        this.contactName = contactName;

        setFont(FONT);
        setForeground(UIColors.CHIP_TEXT);
        setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
        setBorder(BorderFactory.createEmptyBorder(1, 8, 1, 8));
        setToolTipText(contactName);
    }

    /**
     * 관계자의 현재 정보로 툴팁을 갱신한다. 칩에 표시하는 이름은 바꾸지 않는다.
     */
    public void setCurrentInfo(ContactType type, String currentName, String remarks) {
        setToolTipText(FinancialRecordFormats.contactTooltip(type, currentName, remarks));
    }

    public Long getContactId() {
        return contactId;
    }

    public String getContactName() {
        return contactName;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(UIColors.CHIP_BACKGROUND);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }
}
