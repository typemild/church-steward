package org.church.steward.ui.view.settings.fiscalyear;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;

import javax.swing.JPanel;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.field.StyledDatePicker;
import org.church.steward.ui.component.label.FieldLabel;

/**
 * 회계 연도의 기간을 "시작일 ~ 종료일"로 입력받는 필드
 */
class FiscalYearPeriodField extends JPanel {
    private static final long serialVersionUID = 1L;

    private final StyledDatePicker startDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);
    private final StyledDatePicker endDatePicker = new StyledDatePicker(ComponentSize.MEDIUM);

    FiscalYearPeriodField() {
        setLayout(new GridBagLayout());
        setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridy = 0;

        gbc.gridx = 0;
        gbc.weightx = 1.0;
        add(startDatePicker, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 8, 0, 8);
        add(new FieldLabel("~", ComponentSize.MEDIUM), gbc);

        gbc.gridx = 2;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(endDatePicker, gbc);
    }

    void setPeriod(LocalDate startDate, LocalDate endDate) {
        startDatePicker.setDate(startDate);
        endDatePicker.setDate(endDate);
    }

    /**
     * 키보드로 입력란을 옮겨 다니도록 다이얼로그에 등록할 때 쓴다.
     */
    StyledDatePicker getStartDatePicker() {
        return startDatePicker;
    }

    StyledDatePicker getEndDatePicker() {
        return endDatePicker;
    }

    LocalDate getStartDate() {
        return startDatePicker.getDate();
    }

    LocalDate getEndDate() {
        return endDatePicker.getDate();
    }

    /**
     * 입력된 기간을 확인한다. 다른 회계 연도와 겹치는지 등 저장된 데이터가 필요한 검증은 서비스가 한다.
     *
     * @return 문제가 있으면 사용자에게 보여줄 메시지, 없으면 null
     */
    String findInputError() {
        LocalDate startDate = getStartDate();
        LocalDate endDate = getEndDate();
        if (startDate == null || endDate == null) {
            return "회계 연도의 시작일과 종료일을 입력해주세요.";
        }
        if (startDate.isAfter(endDate)) {
            return "회계 연도의 시작일이 종료일보다 늦습니다.";
        }
        return null;
    }
}
