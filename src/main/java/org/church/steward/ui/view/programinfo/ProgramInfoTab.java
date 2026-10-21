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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import org.church.steward.domain.programinfo.dto.ProgramInfoDto;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.label.BadgeLabel;
import org.church.steward.ui.component.label.DescriptionLabel;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.TitleLabel;
import org.church.steward.ui.component.panel.WarningNoticePanel;
import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProgramInfoTab extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int COPY_FEEDBACK_MILLIS = 1500;

    private ProgramInfoDto currentInfo;

    private TitleLabel titleLabel;
    private JLabel versionLabel;
    private JLabel licenseLabel;
    private JLabel downloadLabel;
    private JLabel authorLabel;
    private IconButton copyAuthorBtn;
    private Timer copyFeedbackTimer;

    private WarningNoticePanel noticePanel;

    public ProgramInfoTab() {
        super(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        initComponents();
    }

    private void initComponents() {
        JPanel mainContent = new JPanel(new GridBagLayout());
        mainContent.setOpaque(false);
        mainContent.setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));
        GridBagConstraints gbc;

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // 1. Header 영역
        titleLabel = new TitleLabel("청지기", SwingConstants.CENTER);
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(0, 0, 40, 0);
        mainContent.add(titleLabel, gbc);

        // 2. Description 영역 (JTextArea 사용으로 줄바꿈 및 간격 조정)
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(0, 0, 10, 0);
        // 2. Description 영역 (JTextArea 사용으로 줄바꿈 및 간격 조정)
        DescriptionLabel description = new DescriptionLabel(
                "귀 교회에 하나님의 은혜와 성도 간의 사랑이 항상 가득하길 바랍니다.");
        mainContent.add(description, gbc);

        gbc.insets = new Insets(0, 0, 30, 0);
        description = new DescriptionLabel(
                "본 프로그램은 교회 규모에 관계없이 무료로 사용할 수 있는 간편한 재정 관리 도구입니다.");
        mainContent.add(description, gbc);

        // 3. Notice 영역 (JTextArea 사용)
        noticePanel = new WarningNoticePanel(
                "비밀번호 분실 시 데이터 복구가 불가능하며, 또한 만약 컴퓨터가 바이러스에 감염되면 데이터 손상이 발생할 수 있습니다.\n"
                        + "따라서 소중한 기록을 보호하기 위해, 매주 정기적으로 보고서를 출력하여 별도로 보관하시는 것을 강력히 권장합니다.");
        gbc.insets = new Insets(0, 0, 0, 0);
        mainContent.add(noticePanel, gbc);

        // 4. Metadata 영역 (Left Alignment 유지)
        JPanel detailPanel = new JPanel(new GridBagLayout());
        detailPanel.setOpaque(false);
        GridBagConstraints detailGbc = new GridBagConstraints();

        // 버전
        detailGbc.gridx = 0;
        detailGbc.anchor = GridBagConstraints.WEST;
        detailGbc.weightx = 0;
        detailGbc.insets = new Insets(5, 0, 5, 0);
        detailPanel.add(new BadgeLabel(FontAwesomeSolid.TAG, "버전", ComponentSize.MEDIUM), detailGbc);
        versionLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        detailGbc.gridx = 1;
        detailGbc.insets = new Insets(5, 10, 5, 0);
        detailGbc.weightx = 1.0;
        detailGbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(versionLabel, detailGbc);

        // 라이선스
        detailGbc.gridx = 0;
        detailGbc.insets = new Insets(5, 0, 5, 0);
        detailGbc.weightx = 0;
        detailGbc.fill = GridBagConstraints.NONE;
        detailPanel.add(new BadgeLabel(FontAwesomeSolid.COPYRIGHT, "라이선스", ComponentSize.MEDIUM), detailGbc);

        // FlowLayout의 간격을 10으로 변경하여 구성 요소 간 간격 확보
        JPanel licenseRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        licenseRowPanel.setOpaque(false);

        licenseLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        licenseRowPanel.add(licenseLabel);

        SecondaryButton licenseBtn = new SecondaryButton("상세 보기", ComponentSize.SMALL);
        licenseBtn.addActionListener(_ -> {
            if (currentInfo != null) {
                new LicenseDetailDialog(this, currentInfo.getLicenseTextEn(), currentInfo.getLicenseTextKo())
                        .setVisible(true);
            }
        });
        licenseRowPanel.add(licenseBtn);

        detailGbc.gridx = 1;
        detailGbc.insets = new Insets(5, 0, 5, 0);
        detailGbc.weightx = 1.0;
        detailGbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(licenseRowPanel, detailGbc);

        // 다운로드
        detailGbc.gridx = 0;
        detailGbc.insets = new Insets(5, 0, 5, 0);
        detailGbc.weightx = 0;
        detailGbc.fill = GridBagConstraints.NONE;
        detailPanel.add(new BadgeLabel(FontAwesomeSolid.DOWNLOAD, "다운로드", ComponentSize.MEDIUM), detailGbc);
        downloadLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        downloadLabel.setForeground(UIColors.TEXT_HIGHLIGHT);
        downloadLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        downloadLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (currentInfo != null && currentInfo.getDownloadUrl() != null) {
                    try {
                        Desktop.getDesktop().browse(new URI(currentInfo.getDownloadUrl()));
                    } catch (Exception ex) {
                        log.error("Failed to open download URL", ex);
                    }
                }
            }
        });
        detailGbc.gridx = 1;
        detailGbc.insets = new Insets(5, 10, 5, 0);
        detailGbc.weightx = 1.0;
        detailGbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(downloadLabel, detailGbc);

        // 제작자
        detailGbc.gridx = 0;
        detailGbc.insets = new Insets(5, 0, 5, 0);
        detailGbc.weightx = 0;
        detailGbc.fill = GridBagConstraints.NONE;
        detailPanel.add(new BadgeLabel(FontAwesomeSolid.USER, "제작", ComponentSize.MEDIUM), detailGbc);

        // 복사 버튼을 이메일 주소에 붙이기 위해 간격을 5로 줄이고, 왼쪽 여백은 insets로 채워 다른 줄과 맞춘다
        JPanel authorRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        authorRowPanel.setOpaque(false);

        authorLabel = new FieldLabel("-", ComponentSize.MEDIUM);
        authorRowPanel.add(authorLabel);

        copyAuthorBtn = new IconButton(FontAwesomeSolid.COPY, ComponentSize.MEDIUM);
        copyAuthorBtn.setToolTipText("이메일 주소 복사");
        copyAuthorBtn.addActionListener(_ -> copyAuthor());
        authorRowPanel.add(copyAuthorBtn);

        // 복사한 뒤 잠시 체크 아이콘을 보여주고 원래 아이콘으로 되돌린다
        copyFeedbackTimer = new Timer(COPY_FEEDBACK_MILLIS, _ -> copyAuthorBtn.setIcon(FontAwesomeSolid.COPY));
        copyFeedbackTimer.setRepeats(false);

        detailGbc.gridx = 1;
        detailGbc.insets = new Insets(5, 5, 5, 0);
        detailGbc.weightx = 1.0;
        detailGbc.fill = GridBagConstraints.HORIZONTAL;
        detailPanel.add(authorRowPanel, detailGbc);

        // Metadata를 mainContent 아래에 추가하기 위한 Wrapper
        gbc.insets = new Insets(30, 10, 0, 0);
        gbc.gridy = 5;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        mainContent.add(detailPanel, gbc);

        // 남는 세로 공간이 생겨도 내용이 위쪽에 붙도록 NORTH에 배치
        ScrollableContentPanel contentWrapper = new ScrollableContentPanel();
        contentWrapper.add(mainContent, BorderLayout.NORTH);

        // contentWrapper를 JScrollPane으로 감싸기
        JScrollPane scrollPane = new JScrollPane(contentWrapper);
        scrollPane.setBorder(null); // 테두리 제거
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); // 가로 스크롤 방지
        scrollPane.getVerticalScrollBar().setUnitIncrement(16); // 스크롤 속도 개선

        // mainContent가 최소한의 높이를 가질 수 있도록 설정
        mainContent.setMinimumSize(new Dimension(0, 0));

        // ProgramInfoTab의 중앙에 스크롤 패널 배치
        add(scrollPane, BorderLayout.CENTER);
    }

    public void updateInfo(ProgramInfoDto info) {
        this.currentInfo = info;
        if (info != null) {
            versionLabel.setText(info.getVersion());
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
        copyAuthorBtn.setIcon(FontAwesomeSolid.CHECK);
        copyFeedbackTimer.restart();
    }

    /**
     * 스크롤 영역의 내용 패널.
     * <p>
     * 줄바꿈하는 {@link DescriptionLabel}은 한 번 늘어난 너비를 선호 크기로 유지하므로, 일반 패널을 뷰포트에 넣으면 창을
     * 다시
     * 줄여도 내용이 줄어들지 않는다. 너비를 항상 뷰포트에 맞추도록 해 이를 막는다.
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
