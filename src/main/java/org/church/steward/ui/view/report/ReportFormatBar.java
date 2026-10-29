package org.church.steward.ui.view.report;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.church.steward.domain.report.dto.ReportSettingDto;
import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.domain.report.entity.ReportSettingConstraints;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledCheckBox;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.report.layout.ReportDocument;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 보고서의 서식을 고르는 줄. 왼쪽부터 글꼴, 구분선, 글자 크기(-/+), 행 간격(-/+), 한 페이지에 맞춤, 초기화 순이다. 글꼴,
 * 글자 크기, 행 간격은 이름 대신 아이콘만 보여주고, 이름과 지금 값은 아이콘의 툴팁으로 알린다. 글자 크기와 행 간격은 "- 아이콘
 * +"를 알약 모양의 바탕으로 감싼다.
 * <p>
 * 값을 보관하지 않고, 사용자가 무언가를 바꾸면 {@link Listener}에 알린다. 화면은 설정을 바꿔 다시 배치한 뒤
 * {@link #update}로 이 줄을 새 설정과 문서에 맞춘다. 한 페이지에 맞춤이 켜져 있으면 자동으로 줄인 값을 보여준다. 그
 * 상태에서 글자 크기나 행 간격 버튼을 누르면 화면이 한 페이지에 맞춤을 끄고 보이던 값에서 이어서 조정한다.
 */
class ReportFormatBar extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 글꼴, 글자 크기, 행 간격, 초기화 사이의 간격 */
    private static final int GAP = 8;
    /** 글꼴 선택란 높이에 대한 구분선 높이의 비율 */
    private static final float SEPARATOR_HEIGHT_RATIO = 0.7f;

    /**
     * 서식 변경 알림
     */
    interface Listener {

        void onFontFamilyChanged(ReportFontFamily fontFamily);

        /**
         * @param direction 1이면 키우고, -1이면 줄인다.
         */
        void onFontSizeChanged(int direction);

        /**
         * @param direction 1이면 넓히고, -1이면 좁힌다.
         */
        void onLineSpacingChanged(int direction);

        void onReset();

        void onAutoFitChanged(boolean autoFit);
    }

    private final StyledComboBox<ReportFontFamily> fontCombo = new StyledComboBox<>(ReportFontFamily.values(),
            ComponentSize.MEDIUM);
    /** 이름 대신 아이콘만 보여주며, 이름과 지금 값은 툴팁으로 알린다. */
    private final FieldLabel fontIcon = new FieldLabel("", FontAwesomeSolid.FONT, SwingConstants.CENTER,
            ComponentSize.MEDIUM);
    private final IconButton sizeDownButton = new IconButton(FontAwesomeSolid.MINUS, ComponentSize.SMALL);
    /** 글꼴 아이콘 오른쪽에 위아래 화살표를 붙여 글자의 크기를 키우고 줄이는 기능임을 나타낸다. */
    private final FieldLabel sizeIcon = new FieldLabel("",
            List.of(FontAwesomeSolid.FONT, FontAwesomeSolid.ARROWS_ALT_V), SwingConstants.CENTER,
            ComponentSize.MEDIUM);
    private final IconButton sizeUpButton = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
    private final IconButton spacingDownButton = new IconButton(FontAwesomeSolid.MINUS, ComponentSize.SMALL);
    /** 위아래 화살표 오른쪽에 가로줄(Bars)을 붙여 줄 사이를 조정하는 기능임을 나타낸다. */
    private final FieldLabel spacingIcon = new FieldLabel("",
            List.of(FontAwesomeSolid.ARROWS_ALT_V, FontAwesomeSolid.BARS), SwingConstants.CENTER,
            ComponentSize.MEDIUM);
    private final IconButton spacingUpButton = new IconButton(FontAwesomeSolid.PLUS, ComponentSize.SMALL);
    private final SecondaryButton resetButton = new SecondaryButton("초기화", ComponentSize.MEDIUM);
    private final StyledCheckBox autoFitCheckBox = new StyledCheckBox("한 페이지에 맞춤", ComponentSize.MEDIUM);
    private final SubFieldLabel overflowNotice = new SubFieldLabel(
            "글자 크기와 행 간격을 가장 작게 줄여도 한 페이지에 다 담기지 않습니다.", FontAwesomeSolid.EXCLAMATION_TRIANGLE,
            SwingConstants.LEFT, ComponentSize.SMALL);

    /** 코드에서 선택란의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updating;

    ReportFormatBar(Listener listener) {
        setLayout(new BorderLayout(0, 6));
        setOpaque(false);

        fontCombo.setToolTipText("글꼴");
        fontCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof ReportFontFamily family) {
                    setText(family.getLabel());
                }
                return this;
            }
        });
        fontIcon.setToolTipText("글꼴");
        sizeDownButton.setToolTipText("글자 작게");
        sizeUpButton.setToolTipText("글자 크게");
        spacingDownButton.setToolTipText("행 간격 좁게");
        spacingUpButton.setToolTipText("행 간격 넓게");
        resetButton.setToolTipText("이 보고서의 글꼴, 글자 크기, 행 간격을 기본값으로 되돌리고 한 페이지에 맞춤을 끕니다.");
        autoFitCheckBox.setToolTipText("내용이 한 페이지에 모두 담기도록 행 간격과 글자 크기를 자동으로 줄입니다. "
                + "글자 크기나 행 간격을 직접 바꾸면 꺼집니다.");
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        controls.setOpaque(false);
        controls.add(fontIcon);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(fontCombo);
        controls.add(Box.createHorizontalStrut(GAP));
        // 글꼴과 크기 조정을 나눈다. FlowLayout에서는 세로 구분선의 높이가 0이 되므로 높이를 직접 정한다.
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setPreferredSize(new Dimension(separator.getPreferredSize().width,
                Math.round(fontCombo.getPreferredSize().height * SEPARATOR_HEIGHT_RATIO)));
        controls.add(separator);
        controls.add(Box.createHorizontalStrut(GAP));
        // 두 아이콘의 폭이 조금씩 다르므로 넓은 쪽에 맞춰 두 알약의 크기를 같게 한다. 아이콘은 가운데에 놓인다.
        int iconWidth = Math.max(sizeIcon.getPreferredSize().width, spacingIcon.getPreferredSize().width);
        sizeIcon.setPreferredSize(new Dimension(iconWidth, sizeIcon.getPreferredSize().height));
        spacingIcon.setPreferredSize(new Dimension(iconWidth, spacingIcon.getPreferredSize().height));
        controls.add(new PillPanel(sizeDownButton, sizeIcon, sizeUpButton));
        controls.add(Box.createHorizontalStrut(GAP));
        controls.add(new PillPanel(spacingDownButton, spacingIcon, spacingUpButton));
        controls.add(Box.createHorizontalStrut(GAP));
        controls.add(autoFitCheckBox);
        controls.add(Box.createHorizontalStrut(GAP));
        controls.add(resetButton);

        add(controls, BorderLayout.CENTER);
        add(overflowNotice, BorderLayout.SOUTH);
        overflowNotice.setVisible(false);

        fontCombo.addActionListener(_ -> {
            if (!updating && fontCombo.getSelectedItem() instanceof ReportFontFamily family) {
                listener.onFontFamilyChanged(family);
            }
        });
        sizeDownButton.addActionListener(_ -> listener.onFontSizeChanged(-1));
        sizeUpButton.addActionListener(_ -> listener.onFontSizeChanged(1));
        spacingDownButton.addActionListener(_ -> listener.onLineSpacingChanged(-1));
        spacingUpButton.addActionListener(_ -> listener.onLineSpacingChanged(1));
        resetButton.addActionListener(_ -> listener.onReset());
        autoFitCheckBox.addActionListener(_ -> {
            if (!updating) {
                listener.onAutoFitChanged(autoFitCheckBox.isSelected());
            }
        });

        update(null, null, true);
    }

    /**
     * 설정과 배치한 문서에 맞춘다. 보이는 값은 문서를 실제로 배치한 값이다(한 페이지에 맞춤이면 자동으로 줄인 값).
     *
     * @param setting  보고서의 설정. 보고서가 없으면 null이며 모든 조정을 끈다.
     * @param document 배치한 문서. 아직 없으면 null이며 크기와 행 간격 버튼을 끈다.
     * @param fits     문서가 한 페이지에 담겼는지 여부. 한 페이지에 맞춤인데 담기지 않았을 때만 안내를 보여준다.
     */
    void update(ReportSettingDto setting, ReportDocument document, boolean fits) {
        boolean enabled = setting != null;
        // 한 페이지에 맞춤이 켜져 있어도 버튼은 켜 둔다. 누르면 맞춤이 꺼지고 직접 조정한다.
        boolean adjustable = enabled && document != null;

        int sizeStep = document != null ? document.getStyle()
                .fontSizeStep() : enabled ? setting.getFontSizeStep() : 0;
        int lineSpacing = document != null ? document.getStyle()
                .lineSpacingPercent()
                : enabled ? setting.getLineSpacing()
                        : ReportSettingConstraints.DEFAULT_LINE_SPACING;

        updating = true;
        try {
            fontCombo.setSelectedItem(enabled ? setting.getFontFamily() : ReportSettingConstraints.DEFAULT_FONT_FAMILY);
            autoFitCheckBox.setSelected(enabled && setting.isAutoFit());
        } finally {
            updating = false;
        }

        // 지금 값은 아이콘에 마우스를 올렸을 때 툴팁으로 보여준다.
        sizeIcon.setToolTipText("글자 크기: " + formatSizeStep(sizeStep));
        spacingIcon.setToolTipText("행 간격: " + lineSpacing + "%");

        fontCombo.setEnabled(enabled);
        autoFitCheckBox.setEnabled(enabled);
        sizeDownButton.setEnabled(adjustable && sizeStep > document.getMinFontSizeStep());
        sizeUpButton.setEnabled(adjustable && sizeStep < document.getMaxFontSizeStep());
        spacingDownButton.setEnabled(adjustable && lineSpacing > ReportSettingConstraints.LINE_SPACING_MIN);
        spacingUpButton.setEnabled(adjustable && lineSpacing < ReportSettingConstraints.LINE_SPACING_MAX);
        resetButton.setEnabled(enabled && isChangedFromDefault(setting));

        overflowNotice.setVisible(enabled && setting.isAutoFit() && document != null && !fits);
    }

    /**
     * 글자 크기 조정 단계를 "기본", "+1.5pt", "-0.5pt"처럼 적는다.
     */
    private static String formatSizeStep(int step) {
        if (step == 0) {
            return "기본";
        }
        return String.format("%+.1fpt", step * ReportSettingConstraints.FONT_SIZE_STEP_POINTS);
    }

    /**
     * 서식이 기본값(글꼴, 글자 크기, 행 간격, 한 페이지에 맞춤)과 다른지 여부. 같으면 초기화 버튼을 끈다.
     */
    private static boolean isChangedFromDefault(ReportSettingDto setting) {
        return setting.getFontFamily() != ReportSettingConstraints.DEFAULT_FONT_FAMILY
                || setting.getFontSizeStep() != ReportSettingConstraints.DEFAULT_FONT_SIZE_STEP
                || setting.getLineSpacing() != ReportSettingConstraints.DEFAULT_LINE_SPACING
                || setting.isAutoFit() != ReportSettingConstraints.DEFAULT_AUTO_FIT;
    }

    /**
     * 조정 버튼과 아이콘을 "- 아이콘 +"로 늘어놓고 알약 모양의 바탕으로 감싸는 묶음
     */
    private static final class PillPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        private static final int PADDING_X = 10;
        private static final int PADDING_Y = 4;
        private static final int ICON_GAP = 6;

        PillPanel(IconButton downButton, JLabel icon, IconButton upButton) {
            super(new FlowLayout(FlowLayout.CENTER, 0, 0));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(PADDING_Y, PADDING_X, PADDING_Y, PADDING_X));
            add(downButton);
            add(Box.createHorizontalStrut(ICON_GAP));
            add(icon);
            add(Box.createHorizontalStrut(ICON_GAP));
            add(upButton);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = getHeight();
                g2.setColor(UIColors.SECONDARY_BACKGROUND);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.setColor(UIColors.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            } finally {
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

}
