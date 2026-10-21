package org.church.steward.ui.view.financialrecord.period;

import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.field.StyledComboBox;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.label.FieldLabel;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

/**
 * 조회 기간 선택 영역.
 * 기간 단위를 고르고 좌/우 화살표로 해당 단위만큼 과거/미래로 이동하거나, 시작일과 종료일을 직접 고른다.
 * 조회 기간은 회계 연도의 기간({@link #setLimits})을 벗어날 수 없다.
 * 기간이 바뀌면 생성자에서 받은 동작을 실행한다.
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
    private final StyledDatePicker startDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final StyledDatePicker endDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);

    private LocalDate startDate;
    private LocalDate endDate;
    /** 조회 기간으로 고를 수 있는 날짜의 범위(양 끝 포함). 아직 지정하지 않았으면 null이며 제한하지 않는다. */
    private LocalDate firstAllowedDate;
    private LocalDate lastAllowedDate;
    /** 코드에서 날짜 피커의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updatingPickers = false;

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

        previousButton.setToolTipText("이전 기간");
        nextButton.setToolTipText("다음 기간");

        // 날짜를 지우면 직전 날짜로 되돌아가도록 빈 값을 허용하지 않는다.
        startDatePicker.getSettings()
                .setAllowEmptyDates(false);
        endDatePicker.getSettings()
                .setAllowEmptyDates(false);

        add(new FieldLabel("조회 기간", FontAwesomeSolid.CALENDAR_ALT, SwingConstants.LEFT, ComponentSize.MEDIUM));
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

        setRangeContaining(LocalDate.now());

        unitCombo.addActionListener(_ -> {
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
     * 조회 기간을 회계 연도의 기간 안으로 제한한다. 이후로는 어떤 방법으로도 이 범위 밖의 날짜를 조회 기간에 넣을 수 없다.
     * 현재 조회 기간이 새 범위와 겹치면 겹치는 부분만 남기고, 전혀 겹치지 않으면(회계 연도를 바꾼 경우) 오늘이 속한 단위 기간으로 옮긴다.
     * 오늘이 범위 밖이면 범위에서 오늘과 가장 가까운 날을 기준으로 삼는다. 기간이 바뀌어도 변경 동작은 실행하지 않는다.
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

    private PeriodUnit getUnit() {
        return (PeriodUnit) unitCombo.getSelectedItem();
    }

    /**
     * 범위가 제한되어 있을 때 "연간"은 달력의 한 해가 아니라 회계 연도의 기간 전체를 뜻한다.
     */
    private boolean isWholeFiscalYear() {
        return getUnit() == PeriodUnit.YEAR && firstAllowedDate != null;
    }

    /**
     * 단위가 바뀌면 현재 기간에 오늘이 포함된 경우 오늘을, 아니면 현재 기간의 종료일을 기준으로 기간을 다시 계산한다.
     */
    private void onUnitChanged() {
        LocalDate today = LocalDate.now();
        boolean containsToday = !today.isBefore(startDate) && !today.isAfter(endDate);
        LocalDate base = containsToday ? today : endDate;
        setRangeContaining(base);
    }

    /**
     * 현재 시작일이 속한 단위 기간을 기준으로 이동한다.
     * 날짜를 직접 골라 단위와 어긋난 기간도 이동하면 단위에 맞는 기간으로 돌아온다.
     * 이동할 기간이 허용 범위를 완전히 벗어나면 이동하지 않는다.
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

        PeriodUnit unit = getUnit();
        LocalDate shiftedStart = unit.shift(unit.startOf(startDate), direction);
        if (firstAllowedDate != null && (unit.endOf(shiftedStart)
                .isBefore(firstAllowedDate) || shiftedStart.isAfter(lastAllowedDate))) {
            return null;
        }
        return shiftedStart;
    }

    /**
     * 날짜 피커에서 날짜를 직접 바꾸면 단위는 그대로 두고 기간만 바꾼다.
     * 시작일이 종료일보다 늦어지면 방금 바꾼 쪽에 맞춰 반대쪽 날짜를 같은 날로 옮긴다.
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
        LocalDate start = getUnit().startOf(base);
        setRange(start, getUnit().endOf(start));
    }

    /**
     * 기간을 바꾼다. 허용 범위가 있으면 범위를 벗어난 부분은 잘라낸다(예: 회계 연도가 12월 15일에 시작하면 12월의 월간 기간은
     * 15일부터다).
     * 주어진 기간은 허용 범위와 하루 이상 겹쳐야 한다.
     */
    private void setRange(LocalDate start, LocalDate end) {
        if (firstAllowedDate != null) {
            if (start.isBefore(firstAllowedDate)) {
                start = firstAllowedDate;
            }
            if (end.isAfter(lastAllowedDate)) {
                end = lastAllowedDate;
            }
        }
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
    }
}
