package org.church.steward.ui.view.financialrecord.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;

/**
 * 관계자 자동 완성 목록 팝업. 입력 필드가 포커스를 유지할 수 있도록 팝업 자신은 포커스를 받지 않는다.
 */
public class ContactSuggestionPopup extends JPopupMenu {

    private static final long serialVersionUID = 1L;

    private final DefaultListModel<ContactSummaryDto> listModel = new DefaultListModel<>();
    private final JList<ContactSummaryDto> list = new JList<>(listModel);
    private final JScrollPane scrollPane = new JScrollPane(list);

    // 목록에 다 담지 못한 관계자가 있을 때 맨 아래에 보이는 안내. 관계자가 아니어서 키보드로 고를 수 없도록 목록 밖에 두고,
    // 마우스로 누르면 모두 보여준다.
    private final SubFieldLabel moreResultsLabel = new SubFieldLabel(" ", SwingConstants.RIGHT, ComponentSize.SMALL);
    // 마우스를 올리면 밑줄을 그어 누를 수 있다는 것을 알린다.
    private final Font normalFont = moreResultsLabel.getFont();
    private final Font hoverFont = ContactSuggestions.underlined(normalFont);

    /**
     * @param onSelect  관계자를 골랐을 때 할 일
     * @param onShowAll 안내 항목을 눌렀을 때 할 일. 같은 검색어로 모두 조회해 다시 보여줘야 한다.
     */
    public ContactSuggestionPopup(Consumer<ContactSummaryDto> onSelect, Runnable onShowAll) {
        setLayout(new BorderLayout());
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(UIColors.BORDER));

        // 비고가 없는 항목은 높이가 낮으므로 항목 높이를 고정하지 않는다.
        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setCellRenderer(new ContactSuggestionCellRenderer());
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && list.getCellBounds(index, index).contains(e.getPoint())) {
                    onSelect.accept(listModel.get(index));
                }
            }
        });

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setFocusable(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        add(scrollPane, BorderLayout.CENTER);

        moreResultsLabel.setOpaque(true);
        moreResultsLabel.setBackground(list.getBackground());
        moreResultsLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        moreResultsLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        moreResultsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                moreResultsLabel.setFont(hoverFont);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                moreResultsLabel.setFont(normalFont);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                onShowAll.run();
            }
        });
        add(moreResultsLabel, BorderLayout.SOUTH);
    }

    /**
     * 자동 완성 목록을 대상 컴포넌트 바로 아래에 같은 너비로 보여준다. 목록이 비어 있으면 팝업을 닫는다. 목록에 담지 못한 관계자가 있으면
     * 맨 아래에 그 수를 안내한다.
     */
    public void showSuggestions(Component invoker, List<ContactSummaryDto> contacts, long remainingCount) {
        setVisible(false);
        listModel.clear();
        if (contacts.isEmpty()) {
            return;
        }

        for (ContactSummaryDto contact : contacts) {
            listModel.addElement(contact);
        }
        // 누른 뒤 다시 띄울 때는 마우스가 나가지 않은 채라 밑줄이 남아 있으므로 되돌린다.
        moreResultsLabel.setFont(normalFont);
        moreResultsLabel.setVisible(remainingCount > 0);
        if (remainingCount > 0) {
            moreResultsLabel.setText(ContactSuggestions.moreResultsText(remainingCount));
        }
        list.setSelectedIndex(0);
        ContactSuggestions.fitToWidth(list, scrollPane, invoker.getWidth());

        show(invoker, 0, invoker.getHeight());
    }

    /**
     * 목록의 선택 항목을 위(-1) 또는 아래(+1)로 이동한다.
     */
    public void moveSelection(int delta) {
        if (listModel.isEmpty()) {
            return;
        }
        int index = Math.clamp(list.getSelectedIndex() + delta, 0, listModel.size() - 1);
        list.setSelectedIndex(index);
        list.ensureIndexIsVisible(index);
    }

    public ContactSummaryDto getSelectedContact() {
        return list.getSelectedValue();
    }
}
