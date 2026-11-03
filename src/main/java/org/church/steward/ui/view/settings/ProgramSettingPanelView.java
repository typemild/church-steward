package org.church.steward.ui.view.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.church.steward.domain.programsetting.controller.ProgramSettingController;
import org.church.steward.infrastructure.annotation.UiView;
import org.church.steward.ui.MenuActivatable;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.label.SuccessIndicatorLabel;
import org.church.steward.ui.component.panel.CardPanel;
import org.church.steward.ui.util.AsyncTask;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로그램 설정 탭. 프로그램 전체에 하나뿐인 설정을 고르며, 관리자가 아닌 사용자도 바꿀 수 있다. 값을 고르면 바로 저장한다. 예산별
 * 보기의 칸 설정은 항목이 많아 다이얼로그({@link BudgetViewColumnDialog})에서 정하고 저장한다.
 * <p>
 * 설정 항목은 왼쪽 위의 카드({@link CardPanel})에 한 줄씩 놓는다. 항목을 추가할 때는
 * {@link CardPanel#addRow}로 한 줄을 더한다.
 */
@UiView
@Slf4j
public class ProgramSettingPanelView extends JPanel implements MenuActivatable {

    private static final long serialVersionUID = 1L;

    /** 결산 요일의 순서. 달력과 같이 일요일부터 적는다. */
    private static final DayOfWeek[] DAYS = { DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY };

    /** 입력란과 저장 완료 표시 사이의 간격 */
    private static final int SAVED_INDICATOR_GAP = 8;

    private final ProgramSettingController programSettingController;

    private final StyledComboBox<DayOfWeek> weeklyClosingDayCombo = new StyledComboBox<>(DAYS, ComponentSize.MEDIUM);
    /** 저장이 끝났음을 알리는 체크 아이콘. 입력란 오른쪽에 서서히 나타났다가 사라진다. */
    private final SuccessIndicatorLabel savedIndicator = new SuccessIndicatorLabel(UIFonts.FONT_SIZE_BASE);

    /** 예산별 보기의 칸 설정 다이얼로그를 띄우는 버튼 */
    private final SecondaryButton budgetViewColumnsButton = new SecondaryButton("칸 설정", ComponentSize.MEDIUM);
    /** 칸 설정을 저장했음을 알리는 체크 아이콘 */
    private final SuccessIndicatorLabel budgetViewColumnsSavedIndicator = new SuccessIndicatorLabel(
            UIFonts.FONT_SIZE_BASE);

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

        budgetViewColumnsSavedIndicator.setToolTipText("저장되었습니다.");

        JPanel budgetViewColumnsControl = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        budgetViewColumnsControl.setOpaque(false);
        budgetViewColumnsControl.add(budgetViewColumnsButton);
        budgetViewColumnsControl.add(Box.createHorizontalStrut(SAVED_INDICATOR_GAP));
        budgetViewColumnsControl.add(budgetViewColumnsSavedIndicator);

        CardPanel card = new CardPanel();
        card.addRow("주간 결산 요일", weeklyClosingDayControl,
                "본 설정은 수입/지출 관리의 조회 기간(주간)과 주간 회계 결산 보고서의 조회 기간에 적용됩니다.",
                "주일에 결산하시는 경우 일요일로 지정하시면, 전주 월요일부터 주일까지가 조회 기간이 됩니다.");
        card.addRow("예산별 보기 칸", budgetViewColumnsControl,
                "수입/지출 관리의 예산별 보기에서 수입과 지출 내역의 칸 순서와 숨길 칸을 정합니다.",
                "처음에는 주간 회계 결산 보고서의 주간 내역과 같은 순서이며, 보고서 양식과는 따로 저장됩니다.");

        // 카드는 위쪽에 붙이고 남는 높이는 비워 둔다.
        add(card, BorderLayout.NORTH);

        weeklyClosingDayCombo.addActionListener(_ -> onWeeklyClosingDaySelected());
        budgetViewColumnsButton.addActionListener(_ -> openBudgetViewColumnDialog());
    }

    /**
     * 지금 저장된 칸 설정을 읽어 칸 설정 다이얼로그를 띄운다. 이 화면은 칸 설정을 보관하지 않으므로 띄울 때마다 읽는다.
     */
    private void openBudgetViewColumnDialog() {
        AsyncTask.run(budgetViewColumnsButton,
                programSettingController::getBudgetViewColumns,
                columns -> {
                    BudgetViewColumnDialog dialog = new BudgetViewColumnDialog(SwingUtilities.getWindowAncestor(this),
                            columns, programSettingController::updateBudgetViewColumns,
                            budgetViewColumnsSavedIndicator::showSuccess);
                    dialog.setVisible(true);
                },
                err -> {
                    log.error("Failed to load budget view columns", err);
                    JOptionPane.showMessageDialog(this, "예산별 보기의 칸 설정을 불러오는데 실패했습니다: " + err.getMessage());
                });
    }

    @Override
    public void onMenuSelected() {
        savedIndicator.hideNow();
        budgetViewColumnsSavedIndicator.hideNow();
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
}
