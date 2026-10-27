package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.church.steward.domain.user.controller.UserController;
import org.church.steward.domain.user.dto.UserDto;
import org.church.steward.domain.user.entity.UserConstraints;
import org.church.steward.domain.user.entity.UserRole;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.infrastructure.security.PasswordPolicy;
import org.church.steward.infrastructure.security.PasswordPolicy.Violation;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.field.StyledTextArea;
import org.church.steward.ui.component.field.StyledTextField;
import org.church.steward.ui.component.field.ValidatedPasswordField;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.table.ListTable;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

@UiView
public class UserManagementPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    private final UserController userController;
    private ListTable userTable;
    private DefaultTableModel tableModel;
    private IconButton addButton;
    private IconButton deleteButton;
    private JPanel rightPanel;
    private SubFieldLabel statusLabel;
    private StyledTextField idField;
    private ValidatedPasswordField passwordField;
    private ValidatedPasswordField confirmPasswordField;
    private StyledTextArea remarksTextArea;
    private PrimaryButton saveButton;
    private boolean isAddMode;
    private List<UserDto> currentUserList;

    public UserManagementPanelView(UserController userController) {
        this.userController = userController;
        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        // --- Left Panel ---
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();

        // Row 1: Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        SectionHeaderLabel listTitleLabel = new SectionHeaderLabel("사용자 목록", SwingConstants.LEFT);
        headerPanel.add(listTitleLabel, BorderLayout.WEST);

        JPanel actionButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actionButtonPanel.setOpaque(false);
        addButton = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
        deleteButton = new IconButton(FontAwesomeSolid.TRASH, ComponentSize.SMALL);
        actionButtonPanel.add(addButton);
        actionButtonPanel.add(deleteButton);
        headerPanel.add(actionButtonPanel, BorderLayout.EAST);

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 10, 0);
        leftPanel.add(headerPanel, gbc);

        // Row 2: Table
        tableModel = new DefaultTableModel(new Object[] { "성명(ID)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        userTable = new ListTable(tableModel);

        userTable.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        });

        userTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScrollPane = new JScrollPane(userTable);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        leftPanel.add(tableScrollPane, gbc);

        add(leftPanel, BorderLayout.CENTER);

        // --- Right Panel ---
        rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(450, 0));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(0, 30, 0, 0));
        rightPanel.setEnabled(false);

        // Row 1: Title & Status
        SectionHeaderLabel titleLabel = new SectionHeaderLabel("사용자 정보", SwingConstants.LEFT);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 20, 0);
        rightPanel.add(titleLabel, gbc);

        statusLabel = new SubFieldLabel("", ComponentSize.SMALL);
        statusLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 1.0;
        rightPanel.add(statusLabel, gbc);

        // Row 2: ID
        FieldLabel idLabel = new FieldLabel("성명(ID)", ComponentSize.MEDIUM);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 8, 8);
        rightPanel.add(idLabel, gbc);

        idField = new StyledTextField(ComponentSize.MEDIUM);
        idField.setMaxLength(UserConstraints.ID_MAX_LENGTH);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        rightPanel.add(idField, gbc);

        // Row 3: Password
        FieldLabel pwLabel = new FieldLabel("비밀번호", ComponentSize.MEDIUM);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.weightx = 0;
        rightPanel.add(pwLabel, gbc);

        passwordField = new ValidatedPasswordField(ComponentSize.MEDIUM);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        rightPanel.add(passwordField, gbc);

        // Row 4: Confirm Password
        FieldLabel confirmPwLabel = new FieldLabel("비밀번호 확인", ComponentSize.MEDIUM);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.weightx = 0;
        rightPanel.add(confirmPwLabel, gbc);

        confirmPasswordField = new ValidatedPasswordField(ComponentSize.MEDIUM);
        gbc.gridx = 1;
        gbc.gridwidth = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 8, 0);
        rightPanel.add(confirmPasswordField, gbc);

        // Row 5: Remarks Label
        FieldLabel remarksLabel = new FieldLabel("비고", ComponentSize.MEDIUM);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 8, 8);
        gbc.weightx = 0;
        rightPanel.add(remarksLabel, gbc);

        // Row 6: Remarks Field
        remarksTextArea = new StyledTextArea(5, 20, ComponentSize.MEDIUM);
        remarksTextArea.setMaxLength(UserConstraints.REMARKS_MAX_LENGTH);
        JScrollPane remarksScrollPane = new JScrollPane(remarksTextArea);
        remarksScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        rightPanel.add(remarksScrollPane, gbc);

        // Row 7: Save Button
        saveButton = new PrimaryButton("저장", ComponentSize.MEDIUM);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.insets = new Insets(20, 0, 0, 0);
        rightPanel.add(saveButton, gbc);

        add(rightPanel, BorderLayout.EAST);

        // 이벤트 리스너 등록
        addButton.addActionListener(_ -> prepareAddMode());
        deleteButton.addActionListener(_ -> deleteUser());
        saveButton.addActionListener(_ -> saveUser());

        // 테이블 선택 시 데이터 로드
        userTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedUser();
            }
        });
    }

    @Override
    public void onMenuSelected() {
        clearForm();
        loadUsers();
    }

    private void loadUsers() {
        AsyncTask.run(this, () -> {
            return userController.listUsersByRole(UserRole.USER);
        }, users -> {
            this.currentUserList = users;
            tableModel.setRowCount(0);
            for (UserDto user : users) {
                tableModel.addRow(new Object[] { user.getId() });
            }
        }, ex -> JOptionPane.showMessageDialog(this, "사용자 목록 로드 실패: " + ex.getMessage()));
    }

    private void loadSelectedUser() {
        int row = userTable.getSelectedRow();
        if (row != -1) {
            isAddMode = false;
            rightPanel.setEnabled(true);
            statusLabel.setText("(상세보기/수정)");
            String userId = (String) tableModel.getValueAt(row, 0);

            UserDto user = currentUserList == null ? null
                    : currentUserList.stream()
                            .filter(u -> u.getId().equals(userId))
                            .findFirst()
                            .orElse(null);

            idField.setEnabled(false);
            passwordField.setEnabled(true);
            confirmPasswordField.setEnabled(true);
            remarksTextArea.setEnabled(true);

            idField.setText(userId);
            passwordField.setText("");
            confirmPasswordField.setText("");
            passwordField.setPlaceholder("비밀번호를 변경하려면 새로운 비밀번호를 입력하세요.");
            confirmPasswordField.setPlaceholder("비밀번호를 변경하려면 새로운 비밀번호를 입력하세요.");

            if (user != null) {
                remarksTextArea.setText(user.getRemarks());
            } else {
                remarksTextArea.setText("");
            }
        } else {
            clearForm();
        }
    }

    private void prepareAddMode() {
        clearForm();
        isAddMode = true;
        rightPanel.setEnabled(true);
        idField.setEnabled(true);
        passwordField.setEnabled(true);
        confirmPasswordField.setEnabled(true);
        remarksTextArea.setEnabled(true);

        statusLabel.setText("(신규 등록)");
        idField.setEnabled(true);
        idField.requestFocus();
    }

    private void saveUser() {
        String id = idField.getText().trim();
        String password = PasswordPolicy.normalize(new String(passwordField.getPassword()));
        String confirmPassword = PasswordPolicy.normalize(new String(confirmPasswordField.getPassword()));
        String remarks = remarksTextArea.getText().trim();

        if (id.isEmpty()) {
            JOptionPane.showMessageDialog(this, "성명(ID)을 입력해주세요.");
            return;
        }

        if (isAddMode) {
            if (password.isEmpty() || confirmPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "비밀번호를 입력해주세요.");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
                return;
            }

            List<Violation> result = PasswordPolicy.validate(password, List.of());
            if (!result.isEmpty()) {
                JOptionPane.showMessageDialog(this, result.getFirst().getMessage());
                return;
            }

            AsyncTask.run(this, () -> {
                userController.createUser(id, password, remarks);
                return null;
            }, _ -> {
                loadUsers();
                prepareAddMode();
                JOptionPane.showMessageDialog(this, "사용자가 성공적으로 추가되었습니다.");
            }, ex -> JOptionPane.showMessageDialog(this, "사용자 추가 실패: " + ex.getMessage()));

        } else {
            // 수정 모드
            if (!password.isEmpty()) {
                if (!password.equals(confirmPassword)) {
                    JOptionPane.showMessageDialog(this, "비밀번호가 일치하지 않습니다.");
                    return;
                }

                List<Violation> result = PasswordPolicy.validate(password, List.of());
                if (!result.isEmpty()) {
                    JOptionPane.showMessageDialog(this, result.getFirst().getMessage());
                    return;
                }
            }

            AsyncTask.run(this, () -> {
                userController.updateUser(id, password, remarks);
                return null;
            }, _ -> {
                loadUsers();
                JOptionPane.showMessageDialog(this, "사용자 정보가 성공적으로 수정되었습니다.");
            }, ex -> JOptionPane.showMessageDialog(this, "사용자 수정 실패: " + ex.getMessage()));
        }
    }

    private void deleteUser() {
        int row = userTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "삭제할 사용자를 선택해주세요.");
            return;
        }
        String userId = (String) tableModel.getValueAt(row, 0);

        int confirm = JOptionPane.showConfirmDialog(this, "정말 삭제하시겠습니까?", "삭제 확인", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        AsyncTask.run(this, () -> {
            userController.deleteUser(userId);
            return null;
        }, _ -> {
            loadUsers();
            clearForm();
            JOptionPane.showMessageDialog(this, "사용자가 성공적으로 삭제되었습니다.");
        }, ex -> JOptionPane.showMessageDialog(this, "사용자 삭제 실패: " + ex.getMessage()));
    }

    private void clearForm() {
        isAddMode = false;
        rightPanel.setEnabled(false);
        statusLabel.setText("");
        idField.setText("");
        passwordField.setText("");
        confirmPasswordField.setText("");
        passwordField.setPlaceholder(null);
        confirmPasswordField.setPlaceholder(null);
        remarksTextArea.setText("");

        idField.setEnabled(false);
        passwordField.setEnabled(false);
        confirmPasswordField.setEnabled(false);
        remarksTextArea.setEnabled(false);
    }

}
