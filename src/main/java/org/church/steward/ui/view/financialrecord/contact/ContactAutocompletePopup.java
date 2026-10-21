package org.church.steward.ui.view.financialrecord.contact;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.church.steward.domain.contact.dto.ContactAutocompleteDto;
import org.church.steward.domain.contact.dto.GroupInfoDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;

/**
 * 계층형 관계자 자동 완성 팝업.
 * 메인 목록에는 관계자들과 "신규 추가" 항목을 보여주고, 메인 목록에서 포커스된 관계자가 속한 그룹이 있으면
 * 그 항목의 우측에 그룹들을 담은 서브 목록을 별도 팝업으로 보여준다.
 * 입력 필드가 포커스를 유지할 수 있도록 팝업 자신은 포커스를 받지 않으며, 키 조작은 외부에서 메서드로 전달받는다.
 */
public class ContactAutocompletePopup extends JPopupMenu {

    private static final long serialVersionUID = 1L;

    private static final int MIN_MAIN_WIDTH = 260;
    private static final String SUB_HEADER_TEXT = "한 번에 추가";
    /** 서브 목록에서 스크롤 없이 보여주는 최대 그룹 수 */
    private static final int MAX_SUB_VISIBLE_ROWS = 5;

    private final ContactAutocompleteListener listener;

    private final DefaultListModel<ContactAutocompleteItem> mainModel = new DefaultListModel<>();
    private final JList<ContactAutocompleteItem> mainList = new JList<>(mainModel);
    private final JScrollPane mainScrollPane = new JScrollPane(mainList);

    private final DefaultListModel<GroupInfoDto> subModel = new DefaultListModel<>();
    private final JList<GroupInfoDto> subList = new JList<>(subModel);
    private final JScrollPane subScrollPane = new JScrollPane(subList);
    private final SubPopup subPopup = new SubPopup();

    private boolean groupEnabled = true;
    /** 키 조작 대상이 서브 목록인지 여부 */
    private boolean subActive = false;

    public ContactAutocompletePopup(ContactAutocompleteListener listener) {
        this.listener = listener;

        setLayout(new BorderLayout());
        setFocusable(false);
        setBorder(BorderFactory.createLineBorder(UIColors.BORDER));

        ContactAutocompleteCellRenderer mainRenderer = new ContactAutocompleteCellRenderer();
        mainList.setFocusable(false);
        mainList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        mainList.setCellRenderer(mainRenderer);
        mainList.setFixedCellHeight(mainRenderer.getPreferredSize().height);

        subList.setFocusable(false);
        subList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        subList.setCellRenderer(new ContactGroupCellRenderer());

        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.setFocusable(false);
        mainScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        subScrollPane.setBorder(BorderFactory.createEmptyBorder());
        subScrollPane.setFocusable(false);
        subScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        add(mainScrollPane, BorderLayout.CENTER);

        subPopup.setLayout(new BorderLayout());
        subPopup.setFocusable(false);
        subPopup.setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        subPopup.setBackground(subList.getBackground());

        // 서브 목록의 그룹을 고르면 구성원 전체가 입력된다는 것을 알려주는 안내 문구
        SubFieldLabel subHeaderLabel = new SubFieldLabel(SUB_HEADER_TEXT, ComponentSize.SMALL);
        subHeaderLabel.setBorder(BorderFactory.createEmptyBorder(6, 8, 2, 8));
        subPopup.add(subHeaderLabel, BorderLayout.NORTH);
        subPopup.add(subScrollPane, BorderLayout.CENTER);

        setupEvents();
    }

    private void setupEvents() {
        mainList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshSubPopup();
            }
        });

        MouseAdapter mainMouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = indexAt(mainList, e);
                if (index >= 0 && (subActive || index != mainList.getSelectedIndex())) {
                    subActive = false;
                    subList.clearSelection();
                    mainList.setSelectedIndex(index);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int index = indexAt(mainList, e);
                if (index >= 0) {
                    handleMainPressed(mainModel.get(index), e.getX() - mainList.getCellBounds(index, index).x);
                }
            }
        };
        mainList.addMouseListener(mainMouseHandler);
        mainList.addMouseMotionListener(mainMouseHandler);

        MouseAdapter subMouseHandler = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int index = indexAt(subList, e);
                if (index >= 0 && index != subList.getSelectedIndex()) {
                    subActive = true;
                    subList.setSelectedIndex(index);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int index = indexAt(subList, e);
                if (index >= 0) {
                    handleSubPressed(subModel.get(index), e.getX() - subList.getCellBounds(index, index).x);
                }
            }
        };
        subList.addMouseListener(subMouseHandler);
        subList.addMouseMotionListener(subMouseHandler);
    }

    /**
     * 마우스 위치에 있는 항목의 인덱스를 반환한다. 항목이 없는 빈 공간이면 -1을 반환한다.
     */
    private int indexAt(JList<?> list, MouseEvent e) {
        int index = list.locationToIndex(e.getPoint());
        if (index < 0) {
            return -1;
        }
        Rectangle bounds = list.getCellBounds(index, index);
        return (bounds != null && bounds.contains(e.getPoint())) ? index : -1;
    }

    private void handleMainPressed(ContactAutocompleteItem item, int cellX) {
        if (item.isNewEntry()) {
            listener.onNewContactRequested();
        } else if (ContactAutocompleteCellRenderer.isEditZone(cellX)) {
            listener.onContactEditRequested(item.getContact());
        } else if (ContactAutocompleteCellRenderer.isDeleteZone(cellX)) {
            listener.onContactDeleteRequested(item.getContact());
        } else {
            listener.onContactSelected(item.getContact());
        }
    }

    private void handleSubPressed(GroupInfoDto group, int cellX) {
        if (ContactGroupCellRenderer.isDeleteZone(cellX)) {
            listener.onGroupDeleteRequested(group);
        } else {
            listener.onGroupSelected(group);
        }
    }

    /**
     * 메인 목록에서 포커스된 관계자의 소속 그룹들로 서브 목록을 다시 채워서 그 항목의 우측에 보여준다.
     * 소속 그룹이 없으면 서브 목록을 닫는다.
     */
    private void refreshSubPopup() {
        subActive = false;
        subPopup.setVisible(false);
        subModel.clear();

        ContactAutocompleteItem selected = mainList.getSelectedValue();
        if (!isVisible() || !groupEnabled || selected == null || selected.isNewEntry()) {
            return;
        }

        List<GroupInfoDto> groups = selected.getContact()
                .getGroups();
        if (groups == null || groups.isEmpty()) {
            return;
        }

        for (GroupInfoDto group : groups) {
            subModel.addElement(group);
        }
        // 그룹마다 칩의 줄 수가 달라 항목 높이가 다르므로, 앞쪽 항목들의 실제 높이만큼 보여준다.
        // 항목의 너비는 고정이므로, 스크롤바가 생기면 그만큼 팝업을 넓혀서 칩이 가려지지 않게 한다.
        boolean scrollable = subModel.size() > MAX_SUB_VISIBLE_ROWS;
        int scrollBarWidth = scrollable ? subScrollPane.getVerticalScrollBar()
                .getPreferredSize().width : 0;
        subList.setVisibleRowCount(Math.min(subModel.size(), MAX_SUB_VISIBLE_ROWS));
        subScrollPane.setPreferredSize(new Dimension(ContactGroupCellRenderer.CELL_WIDTH + scrollBarWidth,
                subList.getPreferredScrollableViewportSize().height));
        subScrollPane.getViewport()
                .setViewPosition(new Point(0, 0));

        // 두 팝업의 테두리가 겹치도록 1px 당겨서, 서브 목록의 윗변이 포커스된 메인 항목의 윗변과 같은 높이에 오게 한다.
        Point location = getLocationOnScreen();
        subPopup.setLocation(location.x + getPreferredSize().width - 1,
                location.y + mainList.getSelectedIndex() * mainList.getFixedCellHeight());
        subPopup.setVisible(true);
    }

    /**
     * 그룹 기능 사용 여부를 설정한다. 사용하지 않으면 서브 목록을 보여주지 않는다.
     */
    public void setGroupEnabled(boolean groupEnabled) {
        this.groupEnabled = groupEnabled;
        if (!groupEnabled) {
            subPopup.setVisible(false);
        }
    }

    /**
     * 관계자 목록과 맨 아래의 "신규 추가" 항목을 대상 컴포넌트 바로 아래에 보여준다.
     */
    public void showContacts(Component invoker, List<ContactAutocompleteDto> contacts) {
        setVisible(false);

        mainModel.clear();
        for (ContactAutocompleteDto contact : contacts) {
            mainModel.addElement(ContactAutocompleteItem.ofContact(contact));
        }
        mainModel.addElement(ContactAutocompleteItem.newEntry());
        mainList.setSelectedIndex(0);

        int height = mainList.getFixedCellHeight() * mainModel.size();
        mainScrollPane.setPreferredSize(new Dimension(Math.max(invoker.getWidth(), MIN_MAIN_WIDTH), height));

        show(invoker, 0, invoker.getHeight());
        // 서브 목록의 위치는 메인 목록이 화면에 뜬 뒤에야 정할 수 있다.
        refreshSubPopup();
    }

    @Override
    public void setVisible(boolean visible) {
        if (!visible) {
            subPopup.setVisible(false);
        }
        super.setVisible(visible);
    }

    /**
     * 키 조작 대상 목록의 선택 항목을 위(-1) 또는 아래(+1)로 이동한다.
     */
    public void moveSelection(int delta) {
        JList<?> activeList = subActive ? subList : mainList;
        int size = activeList.getModel()
                .getSize();
        if (size == 0) {
            return;
        }
        int index = Math.clamp(activeList.getSelectedIndex() + delta, 0, size - 1);
        activeList.setSelectedIndex(index);
        activeList.ensureIndexIsVisible(index);
    }

    /**
     * 키 조작 대상을 서브 목록으로 옮긴다.
     *
     * @return 옮겼으면 true. 서브 목록이 없거나 이미 서브 목록에 있으면 false.
     */
    public boolean moveToSubList() {
        if (subActive || !subPopup.isVisible()) {
            return false;
        }
        subActive = true;
        subList.setSelectedIndex(0);
        return true;
    }

    /**
     * 키 조작 대상을 메인 목록으로 되돌린다.
     *
     * @return 되돌렸으면 true. 이미 메인 목록에 있으면 false.
     */
    public boolean moveToMainList() {
        if (!subActive) {
            return false;
        }
        subActive = false;
        subList.clearSelection();
        return true;
    }

    /**
     * 키 조작 대상 목록에서 현재 선택된 항목을 확정한다.
     */
    public void activateSelection() {
        if (subActive) {
            GroupInfoDto group = subList.getSelectedValue();
            if (group != null) {
                listener.onGroupSelected(group);
            }
            return;
        }

        ContactAutocompleteItem item = mainList.getSelectedValue();
        if (item == null) {
            return;
        }
        if (item.isNewEntry()) {
            listener.onNewContactRequested();
        } else {
            listener.onContactSelected(item.getContact());
        }
    }

    /**
     * 서브 목록 팝업.
     * {@link JPopupMenu}는 invoker를 지정해서 띄우면 이미 떠 있는 다른 팝업(메인 목록)을 닫아 버린다.
     * 그래서 {@link #show}나 {@link #setInvoker}를 쓰지 않고 {@link #getInvoker}만 재정의해서,
     * 메뉴의 서브 메뉴처럼 메인 목록과 함께 떠 있게 한다. 위치는 화면 좌표로 {@link #setLocation}에 지정한다.
     */
    private class SubPopup extends JPopupMenu {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getInvoker() {
            return mainList;
        }
    }
}
