package org.church.steward.ui.view.programinfo;

import java.awt.BorderLayout;
import java.awt.Dialog;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

import org.church.steward.ui.util.DialogKeyHandler;

public class LicenseDetailDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    public LicenseDetailDialog(JComponent parent, String contentEn, String contentKo) {
        super(SwingUtilities.getWindowAncestor(parent), "라이선스 상세 정보", Dialog.ModalityType.APPLICATION_MODAL);
        setLayout(new BorderLayout(10, 10));
        setSize(500, 400);
        setLocationRelativeTo(parent);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("한국어", createLicenseTextPane(contentKo));
        tabbedPane.addTab("English", createLicenseTextPane(contentEn));

        add(tabbedPane, BorderLayout.CENTER);

        DialogKeyHandler.install(this);
    }

    private JScrollPane createLicenseTextPane(String content) {
        JEditorPane textPane = new JEditorPane();
        textPane.setContentType("text/html");
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setText("<html><body style='margin:10px; font-family:SansSerif;'>"
                + content.replace("\n", "<br>")
                + "</body></html>");

        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // 텍스트 설정 후 레이아웃 계산이 완료된 후 스크롤 위치를 상단으로 이동
        SwingUtilities.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

        return scrollPane;
    }
}
