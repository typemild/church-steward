package org.church.steward.ui.view.datamanagement.recordimport;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.church.steward.domain.contact.controller.ContactController;
import org.church.steward.domain.contact.controller.ContactGroupController;
import org.church.steward.domain.contact.dto.ContactDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.financialrecord.contact.ContactChipField;
import org.church.steward.ui.view.financialrecord.contact.HierarchicalContactSuggester;

import lombok.extern.slf4j.Slf4j;

/**
 * 수입/지출 가져오기에서 파일의 이름(또는 빈 칸)을 매핑할 관계자를 고르는 다이얼로그.
 * 수입/지출 입력 다이얼로그와 같은 자동완성란을 쓰므로, 목록의 "신규 추가"로 그 자리에서 관계자를 등록하거나
 * 수정, 삭제할 수 있다. 입력 다이얼로그와 같이 수입 항목은 여러 명을 고를 수 있고 그룹 기능을 쓰며("홍길동 가정"을
 * 가족 구성원에 매핑하는 경우 등), 지출 항목은 그룹 기능 없이 한 명만 고른다.
 * <p>
 * 여기서 한 관계자의 변경은 가져오기와 무관하게 즉시 저장되므로, 이 다이얼로그를 닫은 뒤에는 선택된 관계자가 없더라도
 * 다시 분석해야 한다.
 */
@Slf4j
class ContactMappingDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private final ContactController contactController;
    private final ContactChipField contactField = new ContactChipField();

    private List<Long> selectedContactIds = List.of();

    /**
     * @param sourceDescription 매핑하려는 파일의 값에 대한 설명 (예: "파일의 값: 홍길동 (12건)")
     * @param multiple          여러 명을 고를 수 있는지 여부. 수입 항목만 매핑할 때 true이며 이때 그룹 기능도 쓴다.
     * @param initialContactIds 이미 매핑된 관계자들의 ID. 있으면 그 관계자들을 칩으로 넣어 두고 자동 완성 목록은
     *                          띄우지 않는다. 매핑되지 않았으면 빈 목록
     * @param initialName       매핑된 관계자가 없을 때 자동완성란에 미리 채워 둘 이름. 빈 칸 항목처럼 찾을 이름이 없으면
     *                          null
     */
    ContactMappingDialog(Window owner, ContactController contactController,
            ContactGroupController contactGroupController, String sourceDescription, boolean multiple,
            List<Long> initialContactIds, String initialName) {
        super(owner, "관계자 매핑", ModalityType.APPLICATION_MODAL);
        this.contactController = contactController;

        setLayout(new BorderLayout());

        HierarchicalContactSuggester suggester = new HierarchicalContactSuggester(contactController,
                contactGroupController, contactField);
        suggester.setGroupEnabled(multiple);
        contactField.setSuggester(suggester);
        contactField.setMaxContacts(multiple ? 0 : 1);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 12));
        mainPanel.setBackground(UIColors.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        String guide = multiple ? "매핑할 관계자의 이름을 입력해 목록에서 선택해주세요. 여러 명을 선택할 수 있습니다.\n"
                : "매핑할 관계자의 이름을 입력해 목록에서 선택해주세요.\n";
        mainPanel.add(new DescriptionLabel(sourceDescription + "\n" + guide
                + "등록되지 않은 관계자는 목록의 \"신규 추가\"로 바로 등록할 수 있습니다."), BorderLayout.NORTH);

        JPanel fieldPanel = new JPanel(new BorderLayout(0, 8));
        fieldPanel.setOpaque(false);
        fieldPanel.add(new FieldLabel("관계자", ComponentSize.MEDIUM, true), BorderLayout.NORTH);
        fieldPanel.add(contactField, BorderLayout.CENTER);
        mainPanel.add(fieldPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SecondaryButton cancelButton = new SecondaryButton("취소", ComponentSize.MEDIUM);
        PrimaryButton okButton = new PrimaryButton("이 관계자에 매핑", ComponentSize.MEDIUM);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // 설명 문구(JTextArea)는 폭이 정해지기 전에는 줄바꿈 없이 높이를 재므로, 실제 폭에서 줄이 바뀌면(Windows의 글꼴과
        // 화면 배율에 따라) 그만큼 관계자 입력란이 눌린다. 그래서 폭을 정해 배치한 뒤 그 폭에서의 높이로 창의 높이를 다시
        // 맞춘다. 창 테두리의 두께가 바뀌지 않도록 크기를 재기 전에 크기 조절을 끈다.
        setResizable(false);
        pack();
        setSize(new Dimension(Math.max(getWidth(), 460), getHeight()));
        validate();
        setSize(new Dimension(getWidth(), getPreferredSize().height));
        setLocationRelativeTo(owner);

        okButton.addActionListener(_ -> confirm());

        // 자동 완성 목록이 떠 있는 동안의 Enter와 Esc는 목록이 먼저 처리한다.
        DialogKeyHandler.install(this)
                .setButtons(cancelButton, okButton)
                .addField(contactField);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                contactField.requestInputFocus();
                if (!initialContactIds.isEmpty()) {
                    showCurrentContacts(initialContactIds);
                } else if (initialName != null && !initialName.isBlank()) {
                    // 창이 열려 입력란이 포커스를 가진 뒤에 이름을 채워야 자동 완성 목록이 뜬다.
                    SwingUtilities.invokeLater(() -> contactField.setInputText(initialName));
                }
            }
        });
    }

    /**
     * 이미 매핑된 관계자들을 현재 정보(구분, 이름, 비고)로 조회해 칩으로 넣어 둔다. 다른 관계자로 바꾸려면 칩을 지우고
     * 이름을 입력하면 된다. 그 사이에 삭제된 관계자는 넣지 않는다.
     */
    private void showCurrentContacts(List<Long> contactIds) {
        AsyncTask.run(contactField,
                () -> contactController.getContactsByIds(contactIds),
                contacts -> {
                    for (ContactDto contact : contacts) {
                        contactField.addContact(contact.getId(), contact.getType(), contact.getName(),
                                contact.getRemarks());
                    }
                    contactField.requestInputFocus();
                },
                err -> log.error("Failed to load mapped contacts", err));
    }

    /**
     * 다이얼로그를 띄운다.
     *
     * @return 선택된 관계자들의 ID. 취소했으면 빈 목록
     */
    List<Long> showDialog() {
        selectedContactIds = List.of();
        setVisible(true);
        return selectedContactIds;
    }

    private void confirm() {
        List<Long> contactIds = contactField.getContactIds();
        if (contactIds.isEmpty()) {
            JOptionPane.showMessageDialog(this, "관계자를 목록에서 선택해주세요.", "입력 오류", JOptionPane.ERROR_MESSAGE);
            contactField.requestInputFocus();
            return;
        }
        selectedContactIds = List.copyOf(contactIds);
        dispose();
    }

}
