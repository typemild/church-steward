package org.church.steward.ui.view.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.PrimaryButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.SectionHeaderLabel;
import org.church.steward.ui.component.label.WrappingDescriptionLabel;
import org.church.steward.ui.util.DialogKeyHandler;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

/**
 * 회계 연도나 예산이 준비되지 않았음을 알리고 그 메뉴로 바로 이동하게 하는 팝업. 왼쪽에 안내 아이콘, 오른쪽에 제목과 설명을 두고, 아래
 * 가운데에 "닫기"와 바로가기 버튼(주 버튼)을 둔다. Enter는 바로가기, Esc는 닫기다.
 */
class SetupReminderDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    /** 다이얼로그의 폭. 설명은 이 폭에 맞춰 줄바꿈한다. */
    private static final int DIALOG_WIDTH = 440;
    private static final int ICON_SIZE = 32;

    private final WrappingDescriptionLabel messageLabel;

    private boolean shortcutSelected = false;

    /**
     * @param heading       한 줄 제목. 무엇이 준비되지 않았는지 적는다.
     * @param message       해야 할 일을 적은 설명
     * @param shortcutLabel 바로가기 버튼의 글자("예산 관리로 이동" 등)
     */
    SetupReminderDialog(Window owner, String heading, String message, String shortcutLabel) {
        super(owner, "안내", ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout());

        JLabel iconLabel = new JLabel(
                FontIcon.of(FontAwesomeSolid.INFO_CIRCLE, ICON_SIZE, UIColors.PRIMARY_BACKGROUND));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);

        messageLabel = new WrappingDescriptionLabel(message, ComponentSize.MEDIUM);
        messageLabel.setForeground(UIColors.TEXT_MAIN);

        JPanel textPanel = new JPanel(new BorderLayout(0, 8));
        textPanel.setOpaque(false);
        textPanel.add(new SectionHeaderLabel(heading), BorderLayout.NORTH);
        textPanel.add(messageLabel, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new BorderLayout(16, 0));
        mainPanel.setBackground(UIColors.BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(25, 25, 10, 25));
        mainPanel.add(iconLabel, BorderLayout.WEST);
        mainPanel.add(textPanel, BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);

        SecondaryButton closeButton = new SecondaryButton("닫기", ComponentSize.MEDIUM);
        PrimaryButton shortcutButton = new PrimaryButton(shortcutLabel, ComponentSize.MEDIUM);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 10));
        buttonPanel.setBackground(UIColors.BACKGROUND);
        buttonPanel.add(closeButton);
        buttonPanel.add(shortcutButton);
        add(buttonPanel, BorderLayout.SOUTH);

        shortcutButton.addActionListener(_ -> {
            shortcutSelected = true;
            dispose();
        });

        // 입력란이 없으므로 창 어디서든 Enter를 누르면 바로가기를 누른다.
        DialogKeyHandler.install(this)
                .setButtons(closeButton, shortcutButton)
                .pressPrimaryOnEnter();

        // 설명은 폭이 정해진 뒤에야 줄바꿈된 높이를 알 수 있으므로, 폭을 정해 배치한 뒤 높이를 다시 맞춘다.
        setResizable(false);
        pack();
        setSize(Math.max(getWidth(), DIALOG_WIDTH), getHeight());
        validate();
        messageLabel.invalidate();
        setSize(getWidth(), getPreferredSize().height);
        validate();
        setLocationRelativeTo(owner);
    }

    /**
     * 바로가기 버튼으로 닫았는지 여부. "닫기", Esc, 창의 닫기 버튼으로 닫았으면 false다.
     */
    boolean isShortcutSelected() {
        return shortcutSelected;
    }
}
