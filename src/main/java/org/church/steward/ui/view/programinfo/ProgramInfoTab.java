package org.church.steward.ui.view.programinfo;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;

import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.label.WrappingDescriptionLabel;
import org.church.steward.ui.component.panel.CardPanel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로그램 소개 탭. 위에서부터 소개 카드(프로그램 아이콘, 이름, 버전, 인사말), 정보 카드(라이선스, 다운로드, 제작), 주의사항을
 * 같은 폭으로 쌓는다. 정보 카드는 설정 탭과 같은 [이름 | 세로 구분선 | 값] 형식이다({@link CardPanel#addRow}).
 */
@Slf4j
public class ProgramInfoTab extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 화면 가장자리의 여백 */
    private static final int EDGE_PADDING = 30;
    /** 카드와 카드, 카드와 주의사항 사이의 간격 */
    private static final int SECTION_GAP = 15;
    /** 소개 카드의 안쪽 여백 */
    private static final int INTRO_PADDING = 20;
    private static final int APP_ICON_SIZE = 64;
    /** 프로그램 아이콘과 이름 사이의 간격 */
    private static final int ICON_GAP = 16;
    /** 이름과 버전 사이의 간격 */
    private static final int NAME_VERSION_GAP = 4;
    /** 아이콘·이름 줄과 인사말 사이의 간격, 인사말 두 문단 사이의 간격 */
    private static final int GREETING_GAP = 16;
    private static final int GREETING_LINE_GAP = 6;
    /** 값과 그 옆 버튼 사이의 간격 */
    private static final int VALUE_BUTTON_GAP = 10;
    private static final int VALUE_ICON_BUTTON_GAP = 4;

    private ProgramInfoDto currentInfo;

    private SubFieldLabel versionLabel;
    private JLabel licenseLabel;
    private JLabel downloadLabel;
    private JLabel authorLabel;
    private IconButton copyAuthorBtn;

    public ProgramInfoTab() {
        super(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        initComponents();
    }

    private void initComponents() {
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(EDGE_PADDING, EDGE_PADDING, EDGE_PADDING, EDGE_PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridy = 0;
        mainContent.add(createIntroCard(), gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(SECTION_GAP, 0, 0, 0);
        mainContent.add(createInfoCard(), gbc);

        gbc.gridy = 2;
        mainContent.add(new WarningNoticePanel(
                "비밀번호 분실 시 데이터 복구가 불가능하며, 또한 만약 컴퓨터가 바이러스에 감염되면 데이터 손상이 발생할 수 있습니다.\n"
                        + "따라서 소중한 기록을 보호하기 위해, 매주 정기적으로 보고서를 출력하여 별도로 보관하시는 것을 강력히 권장합니다."),
                gbc);

        // 남는 세로 공간이 생겨도 내용이 위쪽에 붙도록 NORTH에 배치
        ScrollableContentPanel contentWrapper = new ScrollableContentPanel();
        contentWrapper.add(mainContent, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(contentWrapper);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport()
                .setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // mainContent가 최소한의 높이를 가질 수 있도록 설정
        mainContent.setMinimumSize(new Dimension(0, 0));

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * 프로그램 아이콘 옆에 이름과 버전을, 그 아래에 인사말을 둔 소개 카드를 만든다.
     */
    private JPanel createIntroCard() {
        CardPanel card = new CardPanel(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(INTRO_PADDING, INTRO_PADDING, INTRO_PADDING, INTRO_PADDING));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.LINE_START;

        Icon appIcon = AppIcons.createIcon(APP_ICON_SIZE);
        if (appIcon != null) {
            gbc.gridx = 0;
            gbc.insets = new Insets(0, 0, 0, ICON_GAP);
            card.add(new JLabel(appIcon), gbc);
        }

        // 이름과 버전은 아이콘 높이의 가운데에 두 줄로 놓는다.
        JPanel nameBlock = new JPanel(new GridBagLayout());
        nameBlock.setOpaque(false);
        GridBagConstraints nameGbc = new GridBagConstraints();
        nameGbc.gridx = 0;
        nameGbc.anchor = GridBagConstraints.LINE_START;
        nameGbc.gridy = 0;
        nameBlock.add(new TitleLabel("청지기", SwingConstants.LEFT), nameGbc);
        versionLabel = new SubFieldLabel("", ComponentSize.MEDIUM);
        nameGbc.gridy = 1;
        nameGbc.insets = new Insets(NAME_VERSION_GAP, 0, 0, 0);
        nameBlock.add(versionLabel, nameGbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        card.add(nameBlock, gbc);

        // 인사말은 카드 폭에 맞춰 줄바꿈한다. 두 문단 모두 본문 색이다.
        WrappingDescriptionLabel greeting = new WrappingDescriptionLabel(
                "귀 교회에 하나님의 은혜와 성도 간의 사랑이 항상 가득하길 바랍니다.", ComponentSize.MEDIUM);
        greeting.setForeground(UIColors.TEXT_MAIN);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(GREETING_GAP, 0, 0, 0);
        card.add(greeting, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(GREETING_LINE_GAP, 0, 0, 0);
        WrappingDescriptionLabel introduction = new WrappingDescriptionLabel(
                "본 프로그램은 교회 규모에 관계없이 무료로 사용할 수 있는 간편한 재정 관리 도구입니다.", ComponentSize.MEDIUM);
        introduction.setForeground(UIColors.TEXT_MAIN);
        card.add(introduction, gbc);

        return card;
    }

    /**
     * 라이선스, 다운로드, 제작을 설정 탭과 같은 형식으로 나열한 정보 카드를 만든다.
     */
    private JPanel createInfoCard() {
        CardPanel card = new CardPanel();

        licenseLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        SecondaryButton licenseBtn = new SecondaryButton("상세 보기", ComponentSize.SMALL);
        licenseBtn.addActionListener(_ -> {
            if (currentInfo != null) {
                new LicenseDetailDialog(this, currentInfo.getLicenseTextEn(), currentInfo.getLicenseTextKo())
                        .setVisible(true);
            }
        });
        card.addRow("라이선스", createValueRow(licenseLabel, VALUE_BUTTON_GAP, licenseBtn));

        downloadLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        downloadLabel.setForeground(UIColors.TEXT_HIGHLIGHT);
        downloadLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        downloadLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentInfo != null && currentInfo.getDownloadUrl() != null) {
                    try {
                        Desktop.getDesktop()
                                .browse(new URI(currentInfo.getDownloadUrl()));
                    } catch (Exception ex) {
                        log.error("Failed to open download URL", ex);
                    }
                }
            }
        });
        card.addRow("최신 버전 다운로드", downloadLabel);

        authorLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        copyAuthorBtn = new IconButton(FontAwesomeSolid.COPY, ComponentSize.MEDIUM);
        copyAuthorBtn.setToolTipText("이메일 주소 복사");
        copyAuthorBtn.addActionListener(_ -> copyAuthor());
        card.addRow("제작", createValueRow(authorLabel, VALUE_ICON_BUTTON_GAP, copyAuthorBtn));

        return card;
    }

    /**
     * 값 오른쪽에 버튼을 붙인 한 줄을 만든다.
     */
    private static JPanel createValueRow(JLabel value, int gap, JButton button) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        row.setOpaque(false);
        row.add(value);
        row.add(Box.createHorizontalStrut(gap));
        row.add(button);
        return row;
    }

    public void updateInfo(ProgramInfoDto info) {
        this.currentInfo = info;
        if (info != null) {
            versionLabel.setText("버전 " + info.getVersion());
            licenseLabel.setText(info.getLicense());
            authorLabel.setText(info.getAuthor());
            downloadLabel.setText(info.getDownloadUrl());
        }
        revalidate();
        repaint();
    }

    private void copyAuthor() {
        if (currentInfo == null || currentInfo.getAuthor() == null) {
            return;
        }
        try {
            Toolkit.getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(currentInfo.getAuthor()), null);
        } catch (IllegalStateException ex) {
            // 다른 프로그램이 클립보드를 쓰고 있으면 실패한다
            log.error("Failed to copy author to clipboard", ex);
            return;
        }
        // 복사한 뒤 잠시 완료 표시(초록 체크)를 보여주고 원래 아이콘으로 되돌린다
        copyAuthorBtn.showSuccess();
    }

    /**
     * 스크롤 영역의 내용 패널.
     * <p>
     * 줄바꿈하는 글(주의사항의 문구)은 한 번 늘어난 너비를 선호 크기로 유지하므로, 일반 패널을 뷰포트에 넣으면 창을 다시 줄여도 내용이
     * 줄어들지 않는다. 너비를 항상 뷰포트에 맞추도록 해 이를 막는다.
     */
    private static class ScrollableContentPanel extends JPanel implements Scrollable {

        private static final long serialVersionUID = 1L;

        private static final int UNIT_INCREMENT = 16;

        ScrollableContentPanel() {
            super(new BorderLayout());
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return UNIT_INCREMENT;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

    }

}
