package org.church.steward.ui.view.report.viewer;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.domain.report.entity.ReportViewMode;
import org.church.steward.domain.report.entity.ReportZoomMode;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.church.steward.ui.view.report.viewer.ReportPreviewPanel.PagePoint;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import com.formdev.flatlaf.util.SystemInfo;

/**
 * 보고서 뷰어. 가운데의 페이지 영역, 왼쪽의 페이지 목록(여닫을 수 있다), 아래의 상태 줄로 이루어진다.
 * <p>
 * 상태 줄에는 페이지 목록 여닫기와 보기 유형(이어 보기/페이지별 보기), 현재 페이지(페이지별 보기에서는 페이지 이동 버튼), 확대/축소가
 * 있다. 확대/축소는 버튼과 Ctrl(맥은 Command) + 휠로 10%씩 바꾸고, 콤보박스에서 정해진 배율이나 폭 맞춤·페이지 맞춤을
 * 고르거나 직접 적는다. 휠 단축키는 상태 줄에 따로 적지 않고 확대/축소 버튼과 콤보박스의 툴팁으로 알린다. 현재 페이지(페이지 이동
 * 버튼)는 상태 줄 전체의 가운데에 놓는다. 페이지 위에서는 커서가 손바닥 모양이 되며 끌어서 스크롤할 수 있다.
 * <p>
 * 페이지 목록 표시, 보기 유형, 배율은 보고서마다 저장하는 설정이며({@link ReportViewerState}), 사용자가 바꾸면 변경
 * 알림을 보낸다.
 */
public class ReportViewer extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final int NARROW_GAP = 2;
    private static final int UNIT_INCREMENT = 24;
    private static final int ZOOM_COMBO_WIDTH = 96;

    /** 콤보박스에서 고를 수 있는 배율(%). 이 밖의 값은 버튼이나 휠, 직접 입력으로만 지정한다. */
    private static final int[] ZOOM_PRESETS = { 25, 50, 75, 100, 125, 150, 200, 300, 400 };
    private static final String PERCENT = "%";
    /** 휠 확대/축소 단축키. 상태 줄에 따로 적지 않고 확대/축소 버튼과 배율 선택란의 툴팁으로 알린다. */
    private static final String ZOOM_SHORTCUT = (SystemInfo.isMacOS ? "Command(⌘)" : "Ctrl") + " + 휠";

    private final ReportPreviewPanel previewPanel = new ReportPreviewPanel();
    private final JScrollPane previewScrollPane = new JScrollPane(previewPanel);
    private final ReportThumbnailList thumbnailList = new ReportThumbnailList();
    private final JScrollPane thumbnailScrollPane = new JScrollPane(thumbnailList);

    private final IconButton thumbnailsButton = new IconButton(FontAwesomeSolid.TH_LIST, ComponentSize.MEDIUM);
    private final StyledComboBox<ReportViewMode> viewModeCombo = new StyledComboBox<>(ReportViewMode.values(),
            ComponentSize.SMALL);
    private final IconButton firstPageButton = new IconButton(FontAwesomeSolid.ANGLE_DOUBLE_LEFT,
            ComponentSize.MEDIUM);
    private final IconButton previousPageButton = new IconButton(FontAwesomeSolid.ANGLE_LEFT, ComponentSize.MEDIUM);
    private final FieldLabel pageLabel = new FieldLabel("", SwingConstants.CENTER, ComponentSize.SMALL);
    private final IconButton nextPageButton = new IconButton(FontAwesomeSolid.ANGLE_RIGHT, ComponentSize.MEDIUM);
    private final IconButton lastPageButton = new IconButton(FontAwesomeSolid.ANGLE_DOUBLE_RIGHT,
            ComponentSize.MEDIUM);
    private final IconButton zoomOutButton = new IconButton(FontAwesomeSolid.SEARCH_MINUS, ComponentSize.MEDIUM);
    private final StyledComboBox<String> zoomCombo = new StyledComboBox<>(zoomItems(), ComponentSize.SMALL);
    private final IconButton zoomInButton = new IconButton(FontAwesomeSolid.SEARCH_PLUS, ComponentSize.MEDIUM);

    private transient Runnable stateChangeListener = () -> {
    };
    private boolean thumbnailsVisible = ReportSettingConstraints.DEFAULT_THUMBNAILS_VISIBLE;
    /** 지금 보고 있는 페이지. 이어 보기에서는 영역의 가운데에 걸친 페이지다. */
    private int currentPage;
    /** 코드에서 선택란의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updating;
    /** 코드에서 스크롤 위치를 옮기는 중인지 여부. 이때는 스크롤 위치로 현재 페이지를 다시 정하지 않는다. */
    private boolean navigating;
    /** 트랙패드처럼 휠을 잘게 굴리는 경우를 위해 한 칸이 될 때까지 모아 두는 휠 회전량 */
    private double pendingWheelRotation;
    /** 끌기를 시작한 화면 위치와 그때의 스크롤 위치. 끌고 있지 않으면 null */
    private Point dragStartOnScreen;
    private Point dragStartViewPosition;

    public ReportViewer() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(UIColors.BORDER));
        setBackground(UIColors.BACKGROUND);

        previewScrollPane.setBorder(BorderFactory.createEmptyBorder());
        previewScrollPane.getVerticalScrollBar()
                .setUnitIncrement(UNIT_INCREMENT);
        previewScrollPane.getHorizontalScrollBar()
                .setUnitIncrement(UNIT_INCREMENT);
        previewScrollPane.getViewport()
                .setBackground(UIColors.REPORT_PREVIEW_BACKGROUND);

        thumbnailScrollPane.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, UIColors.BORDER));
        thumbnailScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        thumbnailScrollPane.getVerticalScrollBar()
                .setUnitIncrement(UNIT_INCREMENT);
        thumbnailScrollPane.getViewport()
                .setBackground(UIColors.BACKGROUND);
        thumbnailScrollPane.setPreferredSize(new Dimension(thumbnailList.getFixedCellWidth()
                + thumbnailScrollPane.getVerticalScrollBar()
                        .getPreferredSize().width,
                0));

        add(thumbnailScrollPane, BorderLayout.WEST);
        add(previewScrollPane, BorderLayout.CENTER);
        add(createStatusBar(), BorderLayout.SOUTH);

        installListeners();
        applyThumbnailsVisible();
        updateControls();
    }

    private static String[] zoomItems() {
        String[] items = new String[ZOOM_PRESETS.length + 2];
        items[0] = ReportZoomMode.FIT_WIDTH.getLabel();
        items[1] = ReportZoomMode.FIT_PAGE.getLabel();
        for (int i = 0; i < ZOOM_PRESETS.length; i++) {
            items[i + 2] = ZOOM_PRESETS[i] + PERCENT;
        }
        return items;
    }

    private JPanel createStatusBar() {
        thumbnailsButton.setFocusable(false);
        viewModeCombo.setToolTipText("보기 유형");
        viewModeCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReportViewMode mode) {
                    setText(mode.getLabel());
                }
                return this;
            }
        });

        firstPageButton.setToolTipText("첫 페이지");
        previousPageButton.setToolTipText("이전 페이지");
        nextPageButton.setToolTipText("다음 페이지");
        lastPageButton.setToolTipText("마지막 페이지");
        zoomOutButton.setToolTipText("축소 (" + ZOOM_SHORTCUT + ")");
        zoomInButton.setToolTipText("확대 (" + ZOOM_SHORTCUT + ")");
        zoomCombo.setEditable(true);
        zoomCombo.setPreferredSize(new Dimension(ZOOM_COMBO_WIDTH, zoomCombo.getPreferredSize().height));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        left.setOpaque(false);
        left.add(thumbnailsButton);
        left.add(Box.createHorizontalStrut(NARROW_GAP));
        left.add(viewModeCombo);

        JPanel pageControls = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pageControls.setOpaque(false);
        pageControls.add(firstPageButton);
        pageControls.add(previousPageButton);
        pageControls.add(Box.createHorizontalStrut(NARROW_GAP));
        pageControls.add(pageLabel);
        pageControls.add(Box.createHorizontalStrut(NARROW_GAP));
        pageControls.add(nextPageButton);
        pageControls.add(lastPageButton);

        // FlowLayout은 줄을 위에 붙이므로, 이어 보기에서 레이블만 있을 때도 세로 가운데에 오도록 GridBagLayout으로 감싼다.
        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        center.add(pageControls);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(zoomOutButton);
        right.add(Box.createHorizontalStrut(NARROW_GAP));
        right.add(zoomCombo);
        right.add(Box.createHorizontalStrut(NARROW_GAP));
        right.add(zoomInButton);

        // 가운데 영역은 양옆을 뺀 나머지의 가운데에 놓이므로, 양옆의 폭을 같게 맞춰야 상태 줄 전체의 가운데에 온다.
        int sideWidth = Math.max(left.getPreferredSize().width, right.getPreferredSize().width);
        left.setPreferredSize(new Dimension(sideWidth, left.getPreferredSize().height));
        right.setPreferredSize(new Dimension(sideWidth, right.getPreferredSize().height));

        JPanel statusBar = new JPanel(new BorderLayout(GAP, 0));
        statusBar.setBackground(UIColors.BACKGROUND);
        statusBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIColors.BORDER),
                BorderFactory.createEmptyBorder(3, 4, 3, 4)));
        statusBar.add(left, BorderLayout.WEST);
        statusBar.add(center, BorderLayout.CENTER);
        statusBar.add(right, BorderLayout.EAST);
        return statusBar;
    }

    private void installListeners() {
        thumbnailsButton.addActionListener(_ -> {
            thumbnailsVisible = !thumbnailsVisible;
            preservingPosition(null, this::applyThumbnailsVisible);
            fireStateChanged();
        });
        viewModeCombo.addActionListener(_ -> {
            if (!updating && viewModeCombo.getSelectedItem() instanceof ReportViewMode mode) {
                changeViewMode(mode);
                fireStateChanged();
            }
        });

        firstPageButton.addActionListener(_ -> showPage(0));
        previousPageButton.addActionListener(_ -> showPage(currentPage - 1));
        nextPageButton.addActionListener(_ -> showPage(currentPage + 1));
        lastPageButton.addActionListener(_ -> showPage(previewPanel.getPageCount() - 1));

        zoomOutButton.addActionListener(_ -> zoomBy(-1, null));
        zoomInButton.addActionListener(_ -> zoomBy(1, null));
        zoomCombo.addActionListener(_ -> {
            if (!updating) {
                onZoomComboChanged();
            }
        });

        thumbnailList.addListSelectionListener(e -> {
            if (!updating && !e.getValueIsAdjusting() && thumbnailList.getSelectedIndex() >= 0) {
                showPage(thumbnailList.getSelectedIndex());
            }
        });

        previewScrollPane.getViewport()
                .addChangeListener(_ -> {
                    if (!navigating) {
                        updateCurrentPageFromScroll();
                    }
                });

        previewPanel.addMouseWheelListener(this::onMouseWheel);
        MouseAdapter dragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && previewPanel.isOnPaper(e.getPoint())) {
                    dragStartOnScreen = e.getLocationOnScreen();
                    dragStartViewPosition = previewScrollPane.getViewport()
                            .getViewPosition();
                    previewPanel.setCursor(GrabCursors.grabbing());
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStartOnScreen == null) {
                    return;
                }
                Point now = e.getLocationOnScreen();
                setViewPosition(dragStartViewPosition.x - (now.x - dragStartOnScreen.x),
                        dragStartViewPosition.y - (now.y - dragStartOnScreen.y));
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragStartOnScreen = null;
                updateCursor(e.getPoint());
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                updateCursor(e.getPoint());
            }
        };
        previewPanel.addMouseListener(dragHandler);
        previewPanel.addMouseMotionListener(dragHandler);
    }

    /**
     * 사용자가 페이지 목록 표시, 보기 유형, 배율을 바꿀 때 실행할 동작을 정한다.
     */
    public void setStateChangeListener(Runnable stateChangeListener) {
        this.stateChangeListener = stateChangeListener;
    }

    public ReportViewerState getState() {
        return new ReportViewerState(thumbnailsVisible, previewPanel.getViewMode(), previewPanel.getZoomMode(),
                previewPanel.getZoomPercent());
    }

    /**
     * 저장해 둔 설정을 적용한다. 변경 알림은 보내지 않는다.
     */
    public void setState(ReportViewerState state) {
        thumbnailsVisible = state.thumbnailsVisible();
        applyThumbnailsVisible();
        previewPanel.setViewMode(state.viewMode());
        previewPanel.setZoom(state.zoomMode(), state.zoomPercent());
        previewScrollPane.validate();
        showPage(currentPage);
    }

    /**
     * 보여줄 보고서를 바꾼다. null이면 아무것도 보여주지 않는다.
     *
     * @param keepPage 보던 페이지를 그대로 보여줄지 여부. 서식만 바꿔 다시 배치한 경우에 쓴다. false이면 첫 페이지로 간다.
     */
    public void setDocument(ReportDocument document, boolean keepPage) {
        int page = keepPage ? currentPage : 0;
        previewPanel.setDocument(document);
        thumbnailList.setDocument(document);
        previewScrollPane.validate();
        if (!keepPage) {
            setViewPosition(0, 0);
        }
        showPage(page);
    }

    /**
     * 페이지로 이동한다. 이어 보기에서는 그 페이지의 맨 위로 스크롤하고, 페이지별 보기에서는 그 페이지로 바꾼다.
     */
    private void showPage(int page) {
        int count = previewPanel.getPageCount();
        currentPage = count == 0 ? 0 : Math.max(0, Math.min(page, count - 1));

        navigating = true;
        try {
            Point position = previewScrollPane.getViewport()
                    .getViewPosition();
            if (previewPanel.getViewMode() == ReportViewMode.SINGLE_PAGE) {
                previewPanel.setCurrentPage(currentPage);
                previewScrollPane.validate();
                setViewPosition(position.x, 0);
            } else if (count > 0) {
                setViewPosition(position.x, previewPanel.scrollTopOf(currentPage));
            }
        } finally {
            navigating = false;
        }
        updateControls();
    }

    private void changeViewMode(ReportViewMode mode) {
        int page = currentPage;
        previewPanel.setCurrentPage(page);
        previewPanel.setViewMode(mode);
        previewScrollPane.validate();
        showPage(page);
    }

    /**
     * 이어 보기에서 스크롤 위치에 따라 현재 페이지를 다시 정한다. 맨 위나 맨 아래까지 스크롤했으면 첫 페이지나 마지막 페이지로 본다.
     */
    private void updateCurrentPageFromScroll() {
        int count = previewPanel.getPageCount();
        if (count == 0 || previewPanel.getViewMode() == ReportViewMode.SINGLE_PAGE) {
            return;
        }

        Rectangle view = previewScrollPane.getViewport()
                .getViewRect();
        int page;
        if (view.y <= 0) {
            page = 0;
        } else if (view.y + view.height >= previewPanel.getHeight() - 1) {
            page = count - 1;
        } else {
            page = previewPanel.pageAtY(view.y + view.height / 2.0);
        }
        if (page != currentPage) {
            currentPage = page;
            updateControls();
        }
    }

    private void onMouseWheel(MouseWheelEvent e) {
        int shortcutMask = Toolkit.getDefaultToolkit()
                .getMenuShortcutKeyMaskEx();
        if ((e.getModifiersEx() & shortcutMask) == 0) {
            // 휠 처리기를 단 컴포넌트는 스크롤 패인이 휠을 받지 못하므로, 확대/축소가 아니면 스크롤 패인에 넘긴다.
            previewScrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(previewPanel, e, previewScrollPane));
            return;
        }

        e.consume();
        Point anchor = SwingUtilities.convertPoint(previewPanel, e.getPoint(), previewScrollPane.getViewport());
        pendingWheelRotation += e.getPreciseWheelRotation();
        // 휠을 위로(음수) 굴리면 확대한다.
        while (pendingWheelRotation <= -1) {
            pendingWheelRotation += 1;
            zoomBy(1, anchor);
        }
        while (pendingWheelRotation >= 1) {
            pendingWheelRotation -= 1;
            zoomBy(-1, anchor);
        }
    }

    /**
     * 배율을 10%씩 바꾼다. 맞춤 방식이었으면 지금 보이는 배율에서 시작하며, 10의 배수가 아니면 가까운 10의 배수로 맞춘다.
     *
     * @param direction 1이면 확대, -1이면 축소
     * @param anchor    배율을 바꿔도 같은 내용이 보이게 할 영역 안의 점. null이면 영역의 가운데
     */
    private void zoomBy(int direction, Point anchor) {
        int step = ReportSettingConstraints.ZOOM_STEP;
        int current = previewPanel.getZoomMode() == ReportZoomMode.CUSTOM ? previewPanel.getZoomPercent()
                : previewPanel.getEffectivePercent();
        int next = direction > 0 ? (current / step + 1) * step : ((current + step - 1) / step - 1) * step;
        applyZoom(ReportZoomMode.CUSTOM, ReportPreviewPanel.clampPercent(next), anchor);
    }

    private void onZoomComboChanged() {
        Object item = zoomCombo.getSelectedItem();
        String text = item == null ? ""
                : item.toString()
                        .trim();
        if (text.equals(ReportZoomMode.FIT_WIDTH.getLabel())) {
            applyZoom(ReportZoomMode.FIT_WIDTH, previewPanel.getZoomPercent(), null);
        } else if (text.equals(ReportZoomMode.FIT_PAGE.getLabel())) {
            applyZoom(ReportZoomMode.FIT_PAGE, previewPanel.getZoomPercent(), null);
        } else {
            try {
                int percent = Integer.parseInt(text.replace(PERCENT, "")
                        .trim());
                applyZoom(ReportZoomMode.CUSTOM, ReportPreviewPanel.clampPercent(percent), null);
            } catch (NumberFormatException e) {
                // 숫자가 아닌 글자를 적었으면 지금 배율로 되돌린다.
                updateControls();
            }
        }
    }

    private void applyZoom(ReportZoomMode mode, int percent, Point anchor) {
        boolean unchanged = mode == previewPanel.getZoomMode()
                && (mode != ReportZoomMode.CUSTOM || percent == previewPanel.getZoomPercent());
        if (unchanged) {
            updateControls();
            return;
        }
        preservingPosition(anchor, () -> previewPanel.setZoom(mode, percent));
        updateControls();
        fireStateChanged();
    }

    /**
     * 페이지 영역의 크기나 배율이 바뀌는 변경을 하면서, 기준점 아래에 보이던 내용이 변경 뒤에도 같은 자리에 보이게 스크롤한다.
     *
     * @param anchor 영역 안의 기준점. null이면 영역의 가운데
     */
    private void preservingPosition(Point anchor, Runnable change) {
        JViewport viewport = previewScrollPane.getViewport();
        Rectangle view = viewport.getViewRect();
        Point point = anchor != null ? anchor : new Point(view.width / 2, view.height / 2);
        PagePoint pagePoint = previewPanel.getPageCount() == 0 ? null
                : previewPanel.toPagePoint(new Point(view.x + point.x, view.y + point.y));

        navigating = true;
        try {
            change.run();
            validate();
            previewScrollPane.validate();
            if (pagePoint != null) {
                Point target = previewPanel.toViewPoint(pagePoint);
                setViewPosition(target.x - point.x, target.y - point.y);
            }
        } finally {
            navigating = false;
        }
        updateCurrentPageFromScroll();
    }

    /**
     * 스크롤 위치를 옮긴다. 페이지 영역을 벗어나지 않도록 범위 안으로 맞춘다.
     */
    private void setViewPosition(int x, int y) {
        JViewport viewport = previewScrollPane.getViewport();
        Dimension viewSize = viewport.getViewSize();
        Dimension extent = viewport.getExtentSize();
        int clampedX = Math.max(0, Math.min(x, viewSize.width - extent.width));
        int clampedY = Math.max(0, Math.min(y, viewSize.height - extent.height));
        viewport.setViewPosition(new Point(clampedX, clampedY));
    }

    private void updateCursor(Point point) {
        if (dragStartOnScreen != null) {
            return;
        }
        previewPanel.setCursor(previewPanel.isOnPaper(point) ? GrabCursors.grab() : Cursor.getDefaultCursor());
    }

    private void applyThumbnailsVisible() {
        thumbnailScrollPane.setVisible(thumbnailsVisible);
        thumbnailsButton.setToolTipText(thumbnailsVisible ? "페이지 목록 숨기기" : "페이지 목록 보이기");
        revalidate();
    }

    /**
     * 현재 페이지, 페이지 이동 버튼, 보기 유형, 배율 선택란을 지금 상태에 맞춘다.
     */
    private void updateControls() {
        int count = previewPanel.getPageCount();
        boolean single = previewPanel.getViewMode() == ReportViewMode.SINGLE_PAGE;
        boolean hasDocument = count > 0;

        updating = true;
        try {
            viewModeCombo.setSelectedItem(previewPanel.getViewMode());

            switch (previewPanel.getZoomMode()) {
            case FIT_WIDTH -> zoomCombo.setSelectedItem(ReportZoomMode.FIT_WIDTH.getLabel());
            case FIT_PAGE -> zoomCombo.setSelectedItem(ReportZoomMode.FIT_PAGE.getLabel());
            case CUSTOM -> zoomCombo.setSelectedItem(previewPanel.getZoomPercent() + PERCENT);
            }

            if (hasDocument) {
                thumbnailList.setSelectedIndex(currentPage);
                thumbnailList.ensureIndexIsVisible(currentPage);
            } else {
                thumbnailList.clearSelection();
            }
        } finally {
            updating = false;
        }

        if (!hasDocument) {
            pageLabel.setText("");
        } else if (single) {
            pageLabel.setText(String.format("%d / %d", currentPage + 1, count));
        } else {
            pageLabel.setText(String.format("%d / %d 페이지", currentPage + 1, count));
        }

        firstPageButton.setVisible(single);
        previousPageButton.setVisible(single);
        nextPageButton.setVisible(single);
        lastPageButton.setVisible(single);
        firstPageButton.setEnabled(hasDocument && currentPage > 0);
        previousPageButton.setEnabled(hasDocument && currentPage > 0);
        nextPageButton.setEnabled(hasDocument && currentPage < count - 1);
        lastPageButton.setEnabled(hasDocument && currentPage < count - 1);

        int effective = previewPanel.getEffectivePercent();
        zoomCombo.setToolTipText(String.format("배율 (현재 %d%%, %s로 확대/축소)", effective, ZOOM_SHORTCUT));
        zoomOutButton.setEnabled(hasDocument && effective > ReportSettingConstraints.ZOOM_MIN);
        zoomInButton.setEnabled(hasDocument && effective < ReportSettingConstraints.ZOOM_MAX);
    }

    private void fireStateChanged() {
        stateChangeListener.run();
    }
}
