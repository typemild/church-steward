package org.church.steward.ui.view.financialrecord.period;

import java.awt.Component;
import java.awt.FlowLayout;
import java.time.DayOfWeek;
import java.time.LocalDate;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.domain.programsetting.entity.ProgramSettingConstraints;
import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.label.FieldLabel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import com.formdev.flatlaf.FlatClientProperties;

/**
 * 조회 기간 선택 영역. 기간 단위를 고르고 좌/우 화살표로 해당 단위만큼 과거/미래로 이동하거나, 시작일과 종료일을 직접 고른다. 날짜를
 * 직접 고르면 단위가 "기타"로 바뀌고, "기타"에서는 고른 기간의 날 수만큼 이동한다({@link CustomPeriod}). "오늘"
 * 버튼은 지금 단위에서 오늘이 속한 기간으로 옮긴다. 조회 기간은 회계 연도의 기간({@link #setLimits})을 벗어날 수 없다.
 * 주간은 결산 요일({@link #setWeeklyClosingDay})의 다음 날부터 결산 요일까지다. 기간이 바뀌면 생성자에서 받은
 * 동작을 실행한다.
 */
public class PeriodSelectorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final int NARROW_GAP = 4;

    private final Runnable onPeriodChanged;

    private final StyledComboBox<PeriodUnit> unitCombo = new StyledComboBox<>(PeriodUnit.values(),
            ComponentSize.MEDIUM);
    private final IconButton previousButton = new IconButton(FontAwesomeSolid.ANGLE_LEFT, ComponentSize.MEDIUM);
    private final IconButton nextButton = new IconButton(FontAwesomeSolid.ANGLE_RIGHT, ComponentSize.MEDIUM);
    private final SecondaryButton todayButton = new SecondaryButton("오늘", ComponentSize.MEDIUM);
    private final StyledDatePicker startDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final StyledDatePicker endDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);

    private LocalDate startDate;
    private LocalDate endDate;
    /** 조회 기간으로 고를 수 있는 날짜의 범위(양 끝 포함). 아직 지정하지 않았으면 null이며 제한하지 않는다. */
    private LocalDate firstAllowedDate;
    private LocalDate lastAllowedDate;
    /** 주간 결산 요일. 주간 단위의 기간을 정한다. */
    private DayOfWeek weeklyClosingDay = ProgramSettingConstraints.DEFAULT_WEEKLY_CLOSING_DAY;
    /** "기타" 단위의 기간. 단위가 "기타"가 될 때 정해지며 그 전에는 null이다. */
    private CustomPeriod customPeriod;
    /** 코드에서 날짜 피커의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updatingPickers = false;
    /** 코드에서 단위 콤보박스의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updatingUnit = false;

    /**
     * @param onPeriodChanged 단위를 바꾸거나, 화살표로 기간을 옮기거나, 시작일/종료일을 직접 바꿨을 때 실행할 동작
     */
    public PeriodSelectorPanel(Runnable onPeriodChanged) {
        this.onPeriodChanged = onPeriodChanged;

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);

        unitCombo.setRenderer(new DefaultListCellRenderer() {
            private static final long serialVersionUID = 1L;

            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof PeriodUnit unit) {
                    setText(unit.getLabel());
                }
                return this;
            }
        });
        unitCombo.setSelectedItem(PeriodUnit.WEEK);
        // 항목이 모두 두 글자이므로 FlatLaf의 콤보박스 최소 폭(72px)을 풀어 글자 폭에 맞춘다.
        unitCombo.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        previousButton.setToolTipText("이전 기간");
        nextButton.setToolTipText("다음 기간");
        todayButton.setToolTipText("오늘이 속한 기간으로 이동합니다.");

        // 날짜를 지우면 직전 날짜로 되돌아가도록 빈 값을 허용하지 않는다.
        startDatePicker.getSettings()
                .setAllowEmptyDates(false);
        endDatePicker.getSettings()
                .setAllowEmptyDates(false);
        // 주간의 기준이 결산 요일에 따라 달라지므로, 기간의 양 끝이 무슨 요일인지 보이게 한다.
        startDatePicker.showDayOfWeek();
        endDatePicker.showDayOfWeek();

        FieldLabel periodLabel = new FieldLabel("", FontAwesomeSolid.CALENDAR_ALT, SwingConstants.LEFT,
                ComponentSize.MEDIUM);
        periodLabel.setToolTipText("조회 기간");
        add(periodLabel);
        add(Box.createHorizontalStrut(GAP));
        add(unitCombo);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(previousButton);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(startDatePicker);
        add(Box.createHorizontalStrut(GAP));
        add(new FieldLabel("~", SwingConstants.CENTER, ComponentSize.MEDIUM));
        add(Box.createHorizontalStrut(GAP));
        add(endDatePicker);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(nextButton);
        add(Box.createHorizontalStrut(GAP));
        add(todayButton);

        setRangeContaining(LocalDate.now());

        unitCombo.addActionListener(_ -> {
            if (updatingUnit) {
                return;
            }
            onUnitChanged();
            onPeriodChanged.run();
        });
        previousButton.addActionListener(_ -> {
            shift(-1);
            onPeriodChanged.run();
        });
        nextButton.addActionListener(_ -> {
            shift(1);
            onPeriodChanged.run();
        });
        todayButton.addActionListener(_ -> {
            setRangeContaining(LocalDate.now());
            onPeriodChanged.run();
        });
        startDatePicker.addDateChangeListener(_ -> onDatePicked(true));
        endDatePicker.addDateChangeListener(_ -> onDatePicked(false));
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    /**
     * 조회 기간을 회계 연도의 기간 안으로 제한한다. 이후로는 어떤 방법으로도 이 범위 밖의 날짜를 조회 기간에 넣을 수 없다. 현재 조회
     * 기간이 새 범위와 겹치면 겹치는 부분만 남기고, 전혀 겹치지 않으면(회계 연도를 바꾼 경우) 오늘이 속한 단위 기간으로 옮긴다. 오늘이
     * 범위 밖이면 범위에서 오늘과 가장 가까운 날을 기준으로 삼는다. 기간이 바뀌어도 변경 동작은 실행하지 않는다.
     *
     * @param firstAllowedDate 회계 연도의 시작일
     * @param lastAllowedDate  회계 연도의 종료일
     */
    public void setLimits(LocalDate firstAllowedDate, LocalDate lastAllowedDate) {
        if (firstAllowedDate.equals(this.firstAllowedDate) && lastAllowedDate.equals(this.lastAllowedDate)) {
            return;
        }
        this.firstAllowedDate = firstAllowedDate;
        this.lastAllowedDate = lastAllowedDate;

        // 날짜 피커는 허용 범위 밖의 날짜를 받지 않으므로, 이전 제한을 풀고 날짜를 옮긴 뒤 새 제한을 건다.
        startDatePicker.setDateRange(null, null);
        endDatePicker.setDateRange(null, null);

        boolean overlaps = !endDate.isBefore(firstAllowedDate) && !startDate.isAfter(lastAllowedDate);
        if (overlaps && !isWholeFiscalYear()) {
            setRange(startDate, endDate);
        } else {
            LocalDate base = LocalDate.now();
            if (base.isBefore(firstAllowedDate)) {
                base = firstAllowedDate;
            } else if (base.isAfter(lastAllowedDate)) {
                base = lastAllowedDate;
            }
            setRangeContaining(base);
        }

        startDatePicker.setDateRange(firstAllowedDate, lastAllowedDate);
        endDatePicker.setDateRange(firstAllowedDate, lastAllowedDate);
    }

    /**
     * 주간 결산 요일을 바꾼다. 요일이 바뀌었고 단위가 주간이면 보고 있던 주간을 새 기준으로 다시 잡는다(기준일은 단위를 바꿀 때와 같다).
     * 기간이 바뀌어도 변경 동작은 실행하지 않는다.
     *
     * @param weeklyClosingDay 주간 결산 요일
     */
    public void setWeeklyClosingDay(DayOfWeek weeklyClosingDay) {
        if (weeklyClosingDay == this.weeklyClosingDay) {
            return;
        }
        this.weeklyClosingDay = weeklyClosingDay;
        if (getUnit() == PeriodUnit.WEEK) {
            onUnitChanged();
        }
    }

    private PeriodUnit getUnit() {
        return (PeriodUnit) unitCombo.getSelectedItem();
    }

    private LocalDate startOfUnit(LocalDate base) {
        if (getUnit() == PeriodUnit.CUSTOM) {
            return customPeriod.startOf(base);
        }
        return getUnit().startOf(base, weeklyClosingDay);
    }

    private LocalDate endOfUnit(LocalDate start) {
        if (getUnit() == PeriodUnit.CUSTOM) {
            return customPeriod.endOf(start);
        }
        return getUnit().endOf(start);
    }

    private LocalDate shiftUnit(LocalDate start, int direction) {
        if (getUnit() == PeriodUnit.CUSTOM) {
            return customPeriod.shift(start, direction);
        }
        return getUnit().shift(start, direction);
    }

    /**
     * 범위가 제한되어 있을 때 "연간"은 달력의 한 해가 아니라 회계 연도의 기간 전체를 뜻한다.
     */
    private boolean isWholeFiscalYear() {
        return getUnit() == PeriodUnit.YEAR && firstAllowedDate != null;
    }

    /**
     * 단위가 바뀌면 현재 기간에 오늘이 포함된 경우 오늘을, 아니면 현재 기간의 종료일을 기준으로 기간을 다시 계산한다. "기타"로 바꾸면
     * 기간은 그대로 두고 그 날 수를 이동 단위로 삼는다.
     */
    private void onUnitChanged() {
        if (getUnit() == PeriodUnit.CUSTOM) {
            customPeriod = CustomPeriod.of(startDate, endDate);
            setRange(startDate, endDate);
            return;
        }
        LocalDate today = LocalDate.now();
        boolean containsToday = !today.isBefore(startDate) && !today.isAfter(endDate);
        LocalDate base = containsToday ? today : endDate;
        setRangeContaining(base);
    }

    /**
     * 현재 시작일이 속한 단위 기간을 기준으로 이동한다. 이동할 기간이 허용 범위를 완전히 벗어나면 이동하지 않는다.
     */
    private void shift(int direction) {
        LocalDate shiftedStart = shiftedStart(direction);
        if (shiftedStart != null) {
            setRangeContaining(shiftedStart);
        }
    }

    /**
     * 화살표로 이동했을 때의 단위 기간 시작일을 구한다.
     *
     * @return 이동할 기간이 허용 범위와 하루도 겹치지 않거나, 회계 연도 전체를 보고 있어 이동할 곳이 없으면 null
     */
    private LocalDate shiftedStart(int direction) {
        if (isWholeFiscalYear()) {
            return null;
        }

        LocalDate shiftedStart = shiftUnit(startOfUnit(startDate), direction);
        if (firstAllowedDate != null && (endOfUnit(shiftedStart).isBefore(firstAllowedDate)
                || shiftedStart.isAfter(lastAllowedDate))) {
            return null;
        }
        return shiftedStart;
    }

    /**
     * 날짜 피커에서 날짜를 직접 바꾸면 단위를 "기타"로 바꾸고 고른 기간의 날 수를 이동 단위로 삼는다. 시작일이 종료일보다 늦어지면 방금
     * 바꾼 쪽에 맞춰 반대쪽 날짜를 같은 날로 옮긴다.
     *
     * @param startChanged 시작일을 바꿨으면 true, 종료일을 바꿨으면 false
     */
    private void onDatePicked(boolean startChanged) {
        if (updatingPickers) {
            return;
        }

        LocalDate start = startDatePicker.getDate();
        LocalDate end = endDatePicker.getDate();
        if (start == null || end == null) {
            return;
        }

        if (start.isAfter(end)) {
            if (startChanged) {
                end = start;
            } else {
                start = end;
            }
        }

        updatingUnit = true;
        try {
            unitCombo.setSelectedItem(PeriodUnit.CUSTOM);
        } finally {
            updatingUnit = false;
        }
        customPeriod = CustomPeriod.of(start, end);
        setRange(start, end);
        onPeriodChanged.run();
    }

    /**
     * 기준일이 속한 단위 기간으로 옮긴다.
     */
    private void setRangeContaining(LocalDate base) {
        if (isWholeFiscalYear()) {
            setRange(firstAllowedDate, lastAllowedDate);
            return;
        }
        LocalDate start = startOfUnit(base);
        setRange(start, endOfUnit(start));
    }

    /**
     * "오늘" 버튼으로 옮길 수 있는지 여부. 단위가 "연간"이거나(회계 연도 기간 전체라 옮길 곳이 없다), 오늘이 속한 기간이 허용 범위와
     * 하루도 겹치지 않거나, 이미 그 기간을 보고 있으면 옮길 수 없다. "기타"에서 오늘이 속한 기간은 고른 기간에서 화살표로 이어지는 기간
     * 중 오늘을 포함하는 것이다.
     */
    private boolean canMoveToToday() {
        if (getUnit() == PeriodUnit.YEAR) {
            return false;
        }
        LocalDate start = startOfUnit(LocalDate.now());
        LocalDate end = endOfUnit(start);
        if (firstAllowedDate != null && (end.isBefore(firstAllowedDate) || start.isAfter(lastAllowedDate))) {
            return false;
        }
        return !clipStart(start).equals(startDate) || !clipEnd(end).equals(endDate);
    }

    /**
     * 허용 범위가 있으면 범위보다 이른 시작일을 범위의 시작일로 자른다.
     */
    private LocalDate clipStart(LocalDate start) {
        return firstAllowedDate != null && start.isBefore(firstAllowedDate) ? firstAllowedDate : start;
    }

    /**
     * 허용 범위가 있으면 범위보다 늦은 종료일을 범위의 종료일로 자른다.
     */
    private LocalDate clipEnd(LocalDate end) {
        return lastAllowedDate != null && end.isAfter(lastAllowedDate) ? lastAllowedDate : end;
    }

    /**
     * 기간을 바꾼다. 허용 범위가 있으면 범위를 벗어난 부분은 잘라낸다(예: 회계 연도가 12월 15일에 시작하면 12월의 월간 기간은
     * 15일부터다). 주어진 기간은 허용 범위와 하루 이상 겹쳐야 한다.
     */
    private void setRange(LocalDate start, LocalDate end) {
        start = clipStart(start);
        end = clipEnd(end);
        this.startDate = start;
        this.endDate = end;

        updatingPickers = true;
        try {
            startDatePicker.setDate(start);
            endDatePicker.setDate(end);
        } finally {
            updatingPickers = false;
        }

        previousButton.setEnabled(shiftedStart(-1) != null);
        nextButton.setEnabled(shiftedStart(1) != null);
        todayButton.setEnabled(canMoveToToday());
    }
}
