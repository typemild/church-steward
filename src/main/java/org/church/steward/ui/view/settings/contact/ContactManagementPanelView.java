package org.church.steward.ui.view.settings.contact;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.domain.contact.dto.ContactSearchRequestDto;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

@UiView
public class ContactManagementPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final ContactController contactController;

    private ListTable contactTable;
    private DefaultTableModel tableModel;
    private List<ContactDto> contactList;

    private final SubFieldLabel totalCountLabel = new SubFieldLabel("총 : 0건", ComponentSize.MEDIUM);

    private ContactSearchPanel searchPanel;
    private IconButton addBtn;
    private IconButton editBtn;
    private IconButton deleteBtn;
    private IconButton toggleBtn;

    private Long selectedContactId = null;
    /** 목록을 다시 채우는 중이면 true. 이때의 선택 변경으로 선택했던 관계자를 잃지 않게 한다. */
    private boolean updatingTable = false;

    public ContactManagementPanelView(ContactController contactController) {
        this.contactController = contactController;

        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // --- North: Search Area ---
        searchPanel = new ContactSearchPanel(this::loadContacts);
        add(searchPanel, BorderLayout.NORTH);

        // --- Center: List Area ---
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);

        // List Header (Action Buttons & Total Count)
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 0, 0, 0));
        headerPanel.setOpaque(false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setOpaque(false);

        addBtn = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.MEDIUM);
        editBtn = new IconButton(FontAwesomeSolid.EDIT, ComponentSize.MEDIUM);
        deleteBtn = new IconButton(FontAwesomeSolid.TRASH, ComponentSize.MEDIUM);
        toggleBtn = new IconButton(FontAwesomeSolid.EYE, ComponentSize.MEDIUM);

        addBtn.setToolTipText("새 관계자 추가");
        editBtn.setToolTipText("관계자 정보 수정");
        deleteBtn.setToolTipText("관계자 삭제");
        toggleBtn.setToolTipText("숨기기");

        addBtn.addActionListener(_ -> handleAdd());
        editBtn.addActionListener(_ -> handleEdit());
        deleteBtn.addActionListener(_ -> handleDelete());
        toggleBtn.addActionListener(_ -> handleToggleSelected());

        buttonPanel.add(addBtn);
        buttonPanel.add(editBtn);
        buttonPanel.add(deleteBtn);
        buttonPanel.add(toggleBtn);

        headerPanel.add(totalCountLabel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        centerPanel.add(headerPanel, BorderLayout.NORTH);

        // Table
        String[] columnNames = { "구분", "이름", "비고", "상태" };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        contactTable = new ListTable(tableModel);
        contactTable.setRowSorter(new TableRowSorter<>(tableModel));
        contactTable.getSelectionModel()
                .addListSelectionListener(e -> {
                    // 목록을 다시 채우는 동안의 선택 변경은 무시하고, 다 채운 뒤에 한 번 반영한다.
                    if (!e.getValueIsAdjusting() && !updatingTable) {
                        handleRowSelection();
                    }
                });
        contactTable.setRowDoubleClickHandler(_ -> handleEdit());
        contactTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < contactTable.getColumnCount(); i++) {
            contactTable.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // 컬럼별 너비 및 정렬 설정
        contactTable.getColumnModel().getColumn(0).setPreferredWidth(80); // 구분
        contactTable.getColumnModel().getColumn(0).setCellRenderer(createRenderer(SwingConstants.CENTER));

        contactTable.getColumnModel().getColumn(1).setPreferredWidth(80); // 이름
        contactTable.getColumnModel().getColumn(1).setCellRenderer(createRenderer(SwingConstants.CENTER));

        contactTable.getColumnModel().getColumn(2).setPreferredWidth(200); // 비고
        contactTable.getColumnModel().getColumn(2).setCellRenderer(createRenderer(SwingConstants.LEADING));

        contactTable.getColumnModel().getColumn(3).setMinWidth(60); // 상태
        contactTable.getColumnModel().getColumn(3).setMaxWidth(60);
        contactTable.getColumnModel().getColumn(3).setCellRenderer(createRenderer(SwingConstants.CENTER));

        centerPanel.add(new JScrollPane(contactTable), BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);
    }

    private DefaultTableCellRenderer createRenderer(int align) {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public void setBorder(Border border) {
                super.setBorder(BorderFactory.createCompoundBorder(
                        border, BorderFactory.createEmptyBorder(4, 8, 4, 8)));
            }
        };
        renderer.setHorizontalAlignment(align);
        return renderer;
    }

    @Override
    public void onMenuSelected() {
        loadContacts(null);
    }

    private void loadContacts(ContactSearchRequestDto request) {
        AsyncTask.run(this,
                () -> contactController.listContacts(request != null ? request : searchPanel.getSearchRequest()),
                contacts -> {
                    this.contactList = contacts;
                    updateTable();
                },
                ex -> JOptionPane.showMessageDialog(this, "목록을 불러오는데 실패했습니다: " + ex.getMessage(), "오류",
                        JOptionPane.ERROR_MESSAGE));
    }

    private void updateTable() {
        updatingTable = true;
        try {
            fillTable();
        } finally {
            updatingTable = false;
        }
        // 선택했던 관계자가 목록에 없으면(삭제, 검색 조건) 선택이 없는 상태로 반영된다.
        handleRowSelection();
    }

    private void fillTable() {
        tableModel.setRowCount(0);
        if (contactList == null)
            return;

        totalCountLabel.setText("총 : " + contactList.size() + "건");

        for (ContactDto dto : contactList) {
            tableModel.addRow(new Object[] {
                    dto.getType().getLabel(),
                    dto.getName(),
                    dto.getRemarks(),
                    dto.isHidden() ? "숨김" : "표시"
            });
        }

        if (selectedContactId != null) {
            for (int i = 0; i < contactList.size(); i++) {
                if (contactList.get(i).getId().equals(selectedContactId)) {
                    int viewIndex = contactTable.convertRowIndexToView(i);
                    contactTable.setRowSelectionInterval(viewIndex, viewIndex);
                    break;
                }
            }
        }
    }

    private void handleRowSelection() {
        int viewRow = contactTable.getSelectedRow();
        if (viewRow == -1) {
            // 선택이 해제되면 목록을 다시 불러와도 그 행을 다시 선택하지 않도록 선택된 관계자도 비운다.
            selectedContactId = null;
            setActionsEnabled(false);
            return;
        }
        setActionsEnabled(true);

        int modelRow = contactTable.convertRowIndexToModel(viewRow);
        ContactDto dto = contactList.get(modelRow);
        selectedContactId = dto.getId();

        // toggleBtn의 아이콘은 현재 상태를 보여 주는 반면, 툴팁은 버튼을 눌렀을 때 발생하는 효과(기능)를 설명한다.
        toggleBtn.setIcon(dto.isHidden() ? FontAwesomeSolid.EYE_SLASH : FontAwesomeSolid.EYE);
        toggleBtn.setToolTipText(dto.isHidden() ? "표시하기" : "숨기기");
    }

    private void setActionsEnabled(boolean enabled) {
        addBtn.setEnabled(true);
        editBtn.setEnabled(enabled);
        deleteBtn.setEnabled(enabled);
        toggleBtn.setEnabled(enabled);
    }

    private void handleAdd() {
        ContactDetailDialog dialog = new ContactDetailDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                ContactDetailDialog.Mode.ADD,
                null,
                dto -> {
                    AsyncTask.run(this, () -> {
                        contactController.createContact(dto);
                        return null;
                    }, _ -> loadContacts(null),
                            ex -> JOptionPane.showMessageDialog(this, "등록 실패: " + ex.getMessage(), "오류",
                                    JOptionPane.ERROR_MESSAGE));
                });
        dialog.setVisible(true);
    }

    private void handleEdit() {
        if (selectedContactId == null)
            return;

        ContactDto dto = contactList.stream()
                .filter(c -> c.getId().equals(selectedContactId))
                .findFirst()
                .orElse(null);

        if (dto == null)
            return;

        ContactDetailDialog dialog = new ContactDetailDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                ContactDetailDialog.Mode.EDIT,
                dto,
                newDto -> {
                    AsyncTask.run(this, () -> {
                        contactController.updateContact(newDto.getId(), newDto.getType(), newDto.getName(),
                                newDto.getRemarks(), newDto.isHidden());
                        return null;
                    }, _ -> loadContacts(null),
                            ex -> JOptionPane.showMessageDialog(this, "수정 실패: " + ex.getMessage(), "오류",
                                    JOptionPane.ERROR_MESSAGE));
                });
        dialog.setVisible(true);
    }

    private void handleDelete() {
        if (selectedContactId == null)
            return;

        int confirm = JOptionPane.showConfirmDialog(this, "정말 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        AsyncTask.run(this,
                () -> {
                    contactController.deleteContact(selectedContactId);
                    return null;
                },
                _ -> loadContacts(null),
                ex -> JOptionPane.showMessageDialog(this, "삭제 실패: " + ex.getMessage(), "오류",
                        JOptionPane.ERROR_MESSAGE));
    }

    private void handleToggleSelected() {
        if (selectedContactId == null)
            return;

        ContactDto dto = contactList.stream()
                .filter(c -> c.getId().equals(selectedContactId))
                .findFirst()
                .orElse(null);

        if (dto == null)
            return;

        AsyncTask.run(this,
                () -> {
                    if (dto.isHidden())
                        contactController.unhideContact(selectedContactId);
                    else
                        contactController.hideContact(selectedContactId);
                    return null;
                },
                _ -> loadContacts(null),
                ex -> JOptionPane.showMessageDialog(this, "상태 변경 실패: " + ex.getMessage(), "오류",
                        JOptionPane.ERROR_MESSAGE));
    }
}
