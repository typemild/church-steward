package org.church.steward.ui.view.financialrecord.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
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

import org.church.steward.domain.contact.dto.ContactSummaryDto;
import org.church.steward.ui.util.UIColors;

/**
 * 관계자 자동 완성 목록 팝업.
 * 입력 필드가 포커스를 유지할 수 있도록 팝업 자신은 포커스를 받지 않는다.
 */
public class ContactSuggestionPopup extends JPopupMenu {

    private static final long serialVersionUID = 1L;

    private static final int MAX_VISIBLE_ROWS = 8;

    private final DefaultListModel<ContactSummaryDto> listModel = new DefaultListModel<>();
    private final JList<ContactSummaryDto> list = new JList<>(listModel);
    private final JScrollPane scrollPane = new JScrollPane(list);

    public ContactSuggestionPopup(Consumer<ContactSummaryDto> onSelect) {
        setLayout(new BorderLayout());
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(UIColors.BORDER));

        list.setFocusable(false);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        ContactSuggestionCellRenderer renderer = new ContactSuggestionCellRenderer();
        list.setCellRenderer(renderer);
        list.setFixedCellHeight(renderer.getPreferredSize().height);
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
    }

    /**
     * 자동 완성 목록을 대상 컴포넌트 바로 아래에 같은 너비로 보여준다. 목록이 비어 있으면 팝업을 닫는다.
     */
    public void showSuggestions(Component invoker, List<ContactSummaryDto> contacts) {
        setVisible(false);
        listModel.clear();
        if (contacts.isEmpty()) {
            return;
        }

        for (ContactSummaryDto contact : contacts) {
            listModel.addElement(contact);
        }
        list.setSelectedIndex(0);
        list.setVisibleRowCount(Math.min(contacts.size(), MAX_VISIBLE_ROWS));
        scrollPane.setPreferredSize(new Dimension(invoker.getWidth(),
                list.getPreferredScrollableViewportSize().height));

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
