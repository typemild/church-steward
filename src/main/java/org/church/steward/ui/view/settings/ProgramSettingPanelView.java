package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

import org.church.steward.domain.programsetting.controller.ProgramSettingController;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.component.label.SubFieldLabel;
import org.church.steward.ui.component.label.SuccessIndicatorLabel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로그램 설정 탭. 프로그램 전체에 하나뿐인 설정을 고르며, 관리자가 아닌 사용자도 바꿀 수 있다. 값을 고르면 바로 저장한다.
 * <p>
 * 설정 항목은 왼쪽 위의 둥근 카드 안에 한 줄씩 놓는다. 한 줄은 왼쪽에 항목 이름(오른쪽 정렬), 세로 구분선, 오른쪽에 입력란과 그
 * 아래의 설명이며, 줄 사이는 가로 구분선으로 나눈다. 모든 항목이 카드의 격자 하나를 함께 써서 이름 칸의 폭이 가장 긴 이름에 맞춰지고
 * 구분선과 입력란의 위치가 항목마다 같다. 항목을 추가할 때는 {@link #addSettingRow}로 한 줄을 더한다.
 */
@UiView
@Slf4j
public class ProgramSettingPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    /** 결산 요일의 순서. 달력과 같이 일요일부터 적는다. */
    private static final DayOfWeek[] DAYS = { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY };

    private static final int CARD_ARC = 12;
    private static final int CARD_PADDING_VERTICAL = 6;
    /** 세로 구분선 양옆의 간격. 카드의 좌우 안쪽 여백도 같게 해서, 가장 긴 항목 이름의 좌우 여백이 같아지게 한다. */
    private static final int NAME_GAP = 16;
    private static final int CARD_PADDING_HORIZONTAL = NAME_GAP;
    /** 한 항목의 위아래 여백 */
    private static final int ROW_PADDING = 14;
    /** 입력란과 설명 사이, 설명 줄 사이의 간격 */
    private static final int DESCRIPTION_GAP = 8;
    private static final int DESCRIPTION_LINE_GAP = 2;
    /** 입력란과 저장 완료 표시 사이의 간격 */
    private static final int SAVED_INDICATOR_GAP = 8;

    private final ProgramSettingController programSettingController;

    private final StyledComboBox<DayOfWeek> weeklyClosingDayCombo = new StyledComboBox<>(DAYS, ComponentSize.MEDIUM);
    /** 저장이 끝났음을 알리는 체크 아이콘. 입력란 오른쪽에 서서히 나타났다가 사라진다. */
    private final SuccessIndicatorLabel savedIndicator = new SuccessIndicatorLabel(UIFonts.FONT_SIZE_BASE);
    private final JPanel card = new CardPanel();
    /** 카드에 놓은 설정 항목의 수. 앞에 가로 구분선을 그을지 정한다. */
    private int itemCount = 0;
    /** 다음 항목을 놓을 카드 격자의 행 */
    private int nextGridY = 0;

    /** 마지막으로 읽거나 저장한 결산 요일. 저장에 실패하면 이 값으로 되돌린다. */
    private DayOfWeek savedWeeklyClosingDay;
    /** 코드에서 선택란의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트로는 저장하지 않는다. */
    private boolean updatingCombo = false;

    public ProgramSettingPanelView(ProgramSettingController programSettingController) {
        this.programSettingController = programSettingController;

        setLayout(new BorderLayout());
        setBackground(UIColors.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(30, 30, 30, 30));

        weeklyClosingDayCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof DayOfWeek day) {
                    setText(day.getDisplayName(TextStyle.FULL, Locale.KOREAN));
                }
                return this;
            }
        });

        savedIndicator.setToolTipText("저장되었습니다.");

        JPanel weeklyClosingDayControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        weeklyClosingDayControl.setOpaque(false);
        weeklyClosingDayControl.add(weeklyClosingDayCombo);
        weeklyClosingDayControl.add(Box.createHorizontalStrut(SAVED_INDICATOR_GAP));
        weeklyClosingDayControl.add(savedIndicator);

        addSettingRow("주간 결산 요일", weeklyClosingDayControl,
                "본 설정은 수입/지출 관리의 조회 기간(주간)과 주간 회계 결산 보고서의 조회 기간에 적용됩니다.",
                "주일에 결산하시는 경우 일요일로 지정하시면, 전주 월요일부터 주일까지가 조회 기간이 됩니다.");

        // 카드는 위쪽에 붙이고 남는 높이는 비워 둔다.
        add(card, BorderLayout.NORTH);

        weeklyClosingDayCombo.addActionListener(_ -> onWeeklyClosingDaySelected());
    }

    /**
     * 카드에 설정 항목 한 줄을 더한다. 항목 이름과 내용 사이에는 세로 구분선을, 첫 줄이 아니면 위에 가로 구분선을 긋는다.
     *
     * @param name             항목 이름
     * @param control          값을 고르는 입력란(저장 완료 표시 등을 함께 담은 패널일 수 있다). 늘이지 않고 제 폭대로
     *                         둔다.
     * @param descriptionLines 입력란 아래에 적을 설명. 줄마다 하나씩 넘긴다.
     */
    private void addSettingRow(String name, JComponent control, String... descriptionLines) {
        boolean hasDescription = descriptionLines.length > 0;
        GridBagConstraints gbc = new GridBagConstraints();

        // 첫 항목이 아니면 앞 항목과의 사이에 카드 폭만큼 가로 구분선을 긋는다.
        if (itemCount > 0) {
            JSeparator divider = new JSeparator(SwingConstants.HORIZONTAL);
            divider.setForeground(UIColors.BORDER);
            gbc.gridx = 0;
            gbc.gridy = nextGridY++;
            gbc.gridwidth = GridBagConstraints.REMAINDER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            card.add(divider, gbc);
            gbc = new GridBagConstraints();
        }
        int firstGridY = nextGridY;

        // 항목 이름은 구분선 쪽(오른쪽)으로 붙이고, 입력란 높이의 가운데에 맞춘다. 이름 칸의 폭은 가장 긴 이름에 맞춰진다.
        FieldLabel nameLabel = new FieldLabel(name, ComponentSize.MEDIUM, true);
        gbc.gridx = 0;
        gbc.gridy = firstGridY;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        card.add(nameLabel, gbc);

        // 항목 이름과 내용 사이의 세로 구분선. 입력란부터 설명 끝까지 내용의 높이만큼 긋는다.
        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        separator.setForeground(UIColors.BORDER);
        gbc.gridx = 1;
        gbc.gridheight = hasDescription ? 2 : 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(ROW_PADDING, NAME_GAP, ROW_PADDING, NAME_GAP);
        card.add(separator, gbc);

        gbc.gridx = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(ROW_PADDING, 0, hasDescription ? 0 : ROW_PADDING, 0);
        card.add(control, gbc);

        if (hasDescription) {
            JPanel description = new JPanel();
            description.setLayout(new BoxLayout(description, BoxLayout.Y_AXIS));
            description.setOpaque(false);
            for (int i = 0; i < descriptionLines.length; i++) {
                SubFieldLabel line = new SubFieldLabel(descriptionLines[i], ComponentSize.SMALL);
                if (i > 0) {
                    line.setBorder(BorderFactory.createEmptyBorder(DESCRIPTION_LINE_GAP, 0, 0, 0));
                }
                description.add(line);
            }
            gbc.gridy = firstGridY + 1;
            gbc.insets = new Insets(DESCRIPTION_GAP, 0, ROW_PADDING, 0);
            card.add(description, gbc);
        }

        nextGridY = firstGridY + (hasDescription ? 2 : 1);
        itemCount++;
    }

    @Override
    public void onMenuSelected() {
        savedIndicator.hideNow();
        AsyncTask.run(weeklyClosingDayCombo,
                programSettingController::getWeeklyClosingDay,
                this::showWeeklyClosingDay,
                err -> {
                    log.error("Failed to load weekly closing day", err);
                    JOptionPane.showMessageDialog(this, "주간 결산 요일을 불러오는데 실패했습니다: " + err.getMessage());
                });
    }

    private void onWeeklyClosingDaySelected() {
        DayOfWeek selected = (DayOfWeek) weeklyClosingDayCombo.getSelectedItem();
        if (updatingCombo || selected == null || selected == savedWeeklyClosingDay) {
            return;
        }

        // 앞선 저장의 체크가 보이는 중이면 그대로 두고, 이번 저장이 끝나면 다시 온전히 보이게 한다.
        AsyncTask.run(weeklyClosingDayCombo,
                () -> {
                    programSettingController.updateWeeklyClosingDay(selected);
                    return null;
                },
                _ -> {
                    savedWeeklyClosingDay = selected;
                    savedIndicator.showSuccess();
                },
                err -> {
                    log.error("Failed to update weekly closing day", err);
                    savedIndicator.hideNow();
                    JOptionPane.showMessageDialog(this, "저장 실패: " + err.getMessage());
                    showWeeklyClosingDay(savedWeeklyClosingDay);
                });
    }

    private void showWeeklyClosingDay(DayOfWeek weeklyClosingDay) {
        savedWeeklyClosingDay = weeklyClosingDay;
        updatingCombo = true;
        try {
            weeklyClosingDayCombo.setSelectedItem(weeklyClosingDay);
        } finally {
            updatingCombo = false;
        }
    }

    /**
     * 설정 항목을 담는 둥근 흰색 카드. 수입/지출 관리의 총합 카드와 같은 모양이다.
     */
    private static class CardPanel extends JPanel {

        private static final long serialVersionUID = 1L;

        CardPanel() {
            super(new GridBagLayout());
            setOpaque(false); // 둥근 배경을 paintComponent에서 직접 그린다
            setBorder(BorderFactory.createEmptyBorder(CARD_PADDING_VERTICAL, CARD_PADDING_HORIZONTAL,
                    CARD_PADDING_VERTICAL, CARD_PADDING_HORIZONTAL));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UIColors.CARD_BACKGROUND);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_ARC, CARD_ARC);
            g2.setColor(UIColors.BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_ARC, CARD_ARC);
            g2.dispose();
            super.paintComponent(g);
        }
    }
}
