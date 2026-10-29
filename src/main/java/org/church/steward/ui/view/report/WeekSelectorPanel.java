package org.church.steward.ui.view.report;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.IconButton;
import org.church.steward.ui.component.button.SecondaryButton;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.label.FieldLabel;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.ui.view.financialrecord.period.PeriodUnit;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import com.github.lgooddatepicker.components.DatePickerSettings;
import com.github.lgooddatepicker.components.DatePickerSettings.DateArea;
import com.github.lgooddatepicker.zinternaltools.HighlightInformation;

/**
 * 주간 보고서의 대상 주간(일요일~토요일)을 고르는 영역.
 * 날짜 하나만 보여주면 "그 날짜까지의 보고서"로 오해하기 쉬우므로, 날짜 입력란 없이 주간의 시작일과 종료일을 글자로 보여준다.
 * 달력 버튼으로 연 달력에서는 선택된 주간 전체가 칠해져 보이고, 어느 날짜를 눌러도 그 날짜가 속한 주간이 선택된다.
 * 좌/우 화살표로 한 주씩 이동할 수 있고, "이번 주" 버튼으로 오늘이 속한 주간으로 돌아온다.
 * 이번 주간이 회계 연도의 기간과 겹치지 않으면(지난 회계 연도를 보고 있을 때 등) "이번 주" 버튼은 꺼진다.
 * 주간은 회계 연도의 기간({@link #setLimits})을 벗어날 수 없고, 기간의 경계에 걸친 주간은 벗어난 부분을 잘라낸다.
 */
class WeekSelectorPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int GAP = 8;
    private static final int NARROW_GAP = 4;
    private static final PeriodUnit UNIT = PeriodUnit.WEEK;
    /** 주간의 양 끝임을 알 수 있게 요일을 함께 적는다. 예: "2026-10-04 (일)" */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd (E)",
            Locale.KOREAN);

    private final Runnable onWeekChanged;

    private final IconButton previousButton = new IconButton(FontAwesomeSolid.ANGLE_LEFT, ComponentSize.MEDIUM);
    private final IconButton nextButton = new IconButton(FontAwesomeSolid.ANGLE_RIGHT, ComponentSize.MEDIUM);
    private final SecondaryButton thisWeekButton = new SecondaryButton("이번 주", ComponentSize.MEDIUM);
    private final FieldLabel weekLabel = new FieldLabel("", SwingConstants.CENTER, ComponentSize.MEDIUM);
    /**
     * 달력을 띄우는 데만 쓰는 날짜 피커. 입력란은 숨기고 달력 버튼만 보여준다.
     * 달력은 이 피커의 버튼을 기준으로 뜨므로, 공용 IconButton 대신 피커의 버튼을 같은 모양으로 꾸며서 쓴다.
     */
    private final StyledDatePicker calendarPicker = new StyledDatePicker(ComponentSize.MEDIUM);

    private LocalDate startDate;
    private LocalDate endDate;
    /** 주간으로 고를 수 있는 날짜의 범위(양 끝 포함). 아직 지정하지 않았으면 null이며 제한하지 않는다. */
    private LocalDate firstAllowedDate;
    private LocalDate lastAllowedDate;
    /** 코드에서 날짜 피커의 값을 바꾸는 중인지 여부. 이때 발생한 변경 이벤트는 무시한다. */
    private boolean updatingPicker = false;

    /**
     * @param onWeekChanged 달력에서 날짜를 고르거나 화살표로 주간을 옮겼을 때 실행할 동작
     */
    WeekSelectorPanel(Runnable onWeekChanged) {
        this.onWeekChanged = onWeekChanged;

        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);

        previousButton.setToolTipText("이전 주간");
        nextButton.setToolTipText("다음 주간");
        thisWeekButton.setToolTipText("오늘이 속한 주간으로 이동합니다.");
        initCalendarPicker();

        add(new FieldLabel("주간", FontAwesomeSolid.CALENDAR_WEEK, SwingConstants.LEFT, ComponentSize.MEDIUM));
        add(Box.createHorizontalStrut(GAP));
        add(previousButton);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(weekLabel);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(calendarPicker);
        add(Box.createHorizontalStrut(NARROW_GAP));
        add(nextButton);
        add(Box.createHorizontalStrut(GAP));
        add(thisWeekButton);

        setWeekContaining(LocalDate.now());

        previousButton.addActionListener(_ -> shift(-1));
        nextButton.addActionListener(_ -> shift(1));
        thisWeekButton.addActionListener(_ -> {
            setWeekContaining(todayWithinLimits());
            onWeekChanged.run();
        });
        calendarPicker.addDateChangeListener(_ -> onDatePicked());
    }

    /**
     * 날짜 피커를 달력 버튼만 남기고, 달력에서 선택된 주간 전체가 칠해져 보이게 한다.
     */
    private void initCalendarPicker() {
        calendarPicker.setOpaque(false);

        DatePickerSettings settings = calendarPicker.getSettings();
        settings.setVisibleDateTextField(false);
        // 날짜를 지우면 주간을 알 수 없게 되므로 빈 값을 허용하지 않는다.
        settings.setAllowEmptyDates(false);
        // 달력의 한 줄이 보고서의 주간(일요일~토요일)과 같아지게 한다.
        settings.setFirstDayOfWeek(DayOfWeek.SUNDAY);
        // 선택된 주간의 날짜를 모두 같은 색으로 칠한다. 피커가 보관하는 날짜(주간의 시작일)만 따로 도드라지지 않게 색을 맞춘다.
        settings.setHighlightPolicy(date -> isInWeek(date)
                ? new HighlightInformation(UIColors.CHIP_BACKGROUND, UIColors.CHIP_TEXT, null)
                : null);
        settings.setColor(DateArea.CalendarBackgroundSelectedDate, UIColors.CHIP_BACKGROUND);
        settings.setColor(DateArea.CalendarBorderSelectedDate, UIColors.CHIP_BACKGROUND);

        // 달력 아이콘, 손 모양 커서, 마우스를 올렸을 때의 배경색은 StyledDatePicker(FieldButton)가 정한다.
        // 입력란 없이 버튼만 보이므로 입력란에 붙는 버튼의 테두리를 걷어 내고 공용 IconButton(MEDIUM)과 같은 크기로,
        // 마우스를 올렸을 때만 배경을 칠한다.
        int iconSize = UIFonts.FONT_SIZE_BASE;
        Dimension buttonSize = new Dimension(iconSize * 2, iconSize * 2);
        JButton calendarButton = calendarPicker.getComponentToggleCalendarButton();
        calendarButton.setToolTipText("달력에서 주간 선택");
        calendarButton.setOpaque(false);
        calendarButton.setBorderPainted(false);
        calendarButton.setContentAreaFilled(false);
        calendarButton.setPreferredSize(buttonSize);
        calendarButton.setMinimumSize(buttonSize);
        calendarButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                calendarButton.setContentAreaFilled(true);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                calendarButton.setContentAreaFilled(false);
            }
        });
    }

    private boolean isInWeek(LocalDate date) {
        return startDate != null && !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * 주간의 시작일. 보통 일요일이지만, 회계 연도가 주중에 시작하면 회계 연도의 시작일이다.
     */
    LocalDate getStartDate() {
        return startDate;
    }

    /**
     * 주간의 종료일. 보통 토요일이지만, 회계 연도가 주중에 끝나면 회계 연도의 종료일이다.
     */
    LocalDate getEndDate() {
        return endDate;
    }

    /**
     * 주간을 회계 연도의 기간 안으로 제한한다.
     * 현재 주간이 새 범위와 겹치면 겹치는 부분만 남기고, 전혀 겹치지 않으면(회계 연도를 바꾼 경우) 오늘이 속한 주간으로 옮긴다.
     * 오늘이 범위 밖이면 범위에서 오늘과 가장 가까운 날을 기준으로 삼는다. 주간이 바뀌어도 변경 동작은 실행하지 않는다.
     *
     * @param firstAllowedDate 회계 연도의 시작일
     * @param lastAllowedDate  회계 연도의 종료일
     */
    void setLimits(LocalDate firstAllowedDate, LocalDate lastAllowedDate) {
        if (firstAllowedDate.equals(this.firstAllowedDate) && lastAllowedDate.equals(this.lastAllowedDate)) {
            return;
        }
        this.firstAllowedDate = firstAllowedDate;
        this.lastAllowedDate = lastAllowedDate;

        // 날짜 피커는 허용 범위 밖의 날짜를 받지 않으므로, 이전 제한을 풀고 날짜를 옮긴 뒤 새 제한을 건다.
        calendarPicker.setDateRange(null, null);

        LocalDate base;
        if (!endDate.isBefore(firstAllowedDate) && !startDate.isAfter(lastAllowedDate)) {
            // 잘려 있던 주간이 새 범위에서는 온전해질 수 있으므로, 겹치는 날을 기준으로 주간을 다시 잡는다.
            base = startDate.isBefore(firstAllowedDate) ? firstAllowedDate : startDate;
        } else {
            base = todayWithinLimits();
        }
        setWeekContaining(base);

        calendarPicker.setDateRange(firstAllowedDate, lastAllowedDate);
    }

    /**
     * 오늘. 허용 범위가 있고 오늘이 그 밖이면 범위에서 오늘과 가장 가까운 날(시작일 또는 종료일)이다.
     */
    private LocalDate todayWithinLimits() {
        LocalDate today = LocalDate.now();
        if (firstAllowedDate == null) {
            return today;
        }
        if (today.isBefore(firstAllowedDate)) {
            return firstAllowedDate;
        }
        if (today.isAfter(lastAllowedDate)) {
            return lastAllowedDate;
        }
        return today;
    }

    private void onDatePicked() {
        if (updatingPicker) {
            return;
        }
        LocalDate picked = calendarPicker.getDate();
        if (picked == null) {
            return;
        }
        setWeekContaining(picked);
        onWeekChanged.run();
    }

    /**
     * 한 주씩 이동한다. 이동할 주간이 허용 범위를 완전히 벗어나면 이동하지 않는다.
     */
    private void shift(int direction) {
        LocalDate shiftedStart = shiftedStart(direction);
        if (shiftedStart == null) {
            return;
        }
        setWeekContaining(shiftedStart);
        onWeekChanged.run();
    }

    /**
     * 화살표로 이동했을 때의 주간 시작일(일요일)을 구한다.
     *
     * @return 이동할 주간이 허용 범위와 하루도 겹치지 않으면 null
     */
    private LocalDate shiftedStart(int direction) {
        LocalDate shiftedStart = UNIT.shift(UNIT.startOf(startDate), direction);
        if (firstAllowedDate != null && (UNIT.endOf(shiftedStart)
                .isBefore(firstAllowedDate) || shiftedStart.isAfter(lastAllowedDate))) {
            return null;
        }
        return shiftedStart;
    }

    /**
     * 기준일이 속한 주간으로 옮긴다. 허용 범위가 있으면 범위를 벗어난 부분은 잘라낸다.
     * 그 주간은 허용 범위와 하루 이상 겹쳐야 한다.
     */
    private void setWeekContaining(LocalDate base) {
        LocalDate start = UNIT.startOf(base);
        LocalDate end = UNIT.endOf(start);
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

        // 피커에는 주간의 시작일을 넣어 둔다. 달력을 열면 이 날짜가 있는 달이 보인다.
        updatingPicker = true;
        try {
            calendarPicker.setDate(start);
        } finally {
            updatingPicker = false;
        }
        weekLabel.setText(start.format(DATE_FORMAT) + "  ~  " + end.format(DATE_FORMAT));

        previousButton.setEnabled(shiftedStart(-1) != null);
        nextButton.setEnabled(shiftedStart(1) != null);
        // 이번 주간이 회계 연도의 기간과 하루도 겹치지 않거나, 이미 이번 주간을 보고 있으면 끈다.
        LocalDate today = todayWithinLimits();
        thisWeekButton.setEnabled(isThisWeekWithinLimits() && (today.isBefore(start) || today.isAfter(end)));
    }

    /**
     * 오늘이 속한 주간이 허용 범위와 하루 이상 겹치는지 여부. 범위를 아직 지정하지 않았으면 겹치는 것으로 본다.
     */
    private boolean isThisWeekWithinLimits() {
        if (firstAllowedDate == null) {
            return true;
        }
        LocalDate thisWeekStart = UNIT.startOf(LocalDate.now());
        LocalDate thisWeekEnd = UNIT.endOf(thisWeekStart);
        return !thisWeekEnd.isBefore(firstAllowedDate) && !thisWeekStart.isAfter(lastAllowedDate);
    }
}
