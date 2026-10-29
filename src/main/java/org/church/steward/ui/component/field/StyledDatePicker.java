package org.church.steward.ui.component.field;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.time.LocalDate;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.component.button.FieldButton;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIFonts;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;

import com.github.lgooddatepicker.components.DatePicker;
import com.github.lgooddatepicker.components.DatePickerSettings;

public class StyledDatePicker extends DatePicker implements CompositeInputField {

    private static final long serialVersionUID = 1L;

    private final Insets padding;

    public StyledDatePicker(ComponentSize componentSize) {
        super();

        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        this.padding = switch (componentSize) {
        case LARGE -> new Insets(
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.53f),
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.53f));
        case MEDIUM -> new Insets(
                Math.round(fontSize * 0.3f),
                Math.round(fontSize * 0.46f),
                Math.round(fontSize * 0.3f),
                Math.round(fontSize * 0.46f));
        case SMALL -> new Insets(
                Math.round(fontSize * 0.18f),
                Math.round(fontSize * 0.36f),
                Math.round(fontSize * 0.18f),
                Math.round(fontSize * 0.36f));
        };

        DatePickerSettings dateSettings = new DatePickerSettings();
        dateSettings.setFormatForDatesCommonEra("yyyy-MM-dd");
        dateSettings.setFormatForDatesBeforeCommonEra("yyyy-MM-dd");
        dateSettings.setFontValidDate(font);
        dateSettings.setFontInvalidDate(font);
        setSettings(dateSettings);

        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(getComponentDateTextField());

        initCalendarButton(componentSize);
    }

    /**
     * 달력 버튼을 입력란에 붙는 버튼(달력 아이콘)으로 꾸민다.
     */
    private void initCalendarButton(ComponentSize componentSize) {
        // 버튼 크기를 입력란 높이에 맞추므로 입력란의 테두리와 여백을 먼저 정한다.
        zDrawTextFieldIndicators();

        JButton calendarButton = getComponentToggleCalendarButton();
        FieldButton.decorate(calendarButton, FontAwesomeSolid.CALENDAR_ALT, componentSize,
                getComponentDateTextField());
        calendarButton.setToolTipText("달력에서 날짜 선택");
    }

    /**
     * 달력 버튼이 꺼져 있으면 달력을 열지 않는다. 라이브러리는 버튼의 mousePressed 리스너(→ togglePopup →
     * openPopup)로 달력을 열면서 버튼이 아니라 피커 전체가 켜져 있는지만 확인하므로, 버튼만 끄면(상세 보기) 눌러도 달력이 뜬다.
     * 리스너는 등록 순서대로 모두 불려 다른 리스너로 막을 수 없으므로, 달력을 여는 유일한 경로인 이 메서드에서 막는다.
     */
    @Override
    public void openPopup() {
        if (!getComponentToggleCalendarButton().isEnabled()) {
            return;
        }
        super.openPopup();
    }

    /**
     * 선택할 수 있는 날짜를 지정한 기간(양 끝 포함)으로 제한한다. 달력에서 기간 밖의 날짜는 고를 수 없고, 직접 입력해도 받아들이지
     * 않는다. 이미 선택된 날짜는 건드리지 않으므로, 호출하는 쪽에서 기간 안의 날짜로 맞춰야 한다. 둘 다 null이면 제한을 푼다.
     */
    public void setDateRange(LocalDate firstAllowedDate, LocalDate lastAllowedDate) {
        getSettings().setDateRangeLimits(firstAllowedDate, lastAllowedDate);
    }

    /**
     * 날짜 입력란을 반환한다.
     */
    @Override
    public JComponent getInputComponent() {
        return getComponentDateTextField();
    }

    /**
     * 날짜 입력란의 글자를 반환한다. 아직 날짜로 읽히지 않은(형식이 틀리거나 기간 밖인) 글자도 그대로 반환한다.
     */
    public String getDateText() {
        return getComponentDateTextField().getText();
    }

    @Override
    public void zDrawTextFieldIndicators() {
        super.zDrawTextFieldIndicators();
        // 상위 생성자가 호출할 때는 아직 여백이 정해지지 않았다.
        if (padding == null) {
            return;
        }

        // LGoodDatePicker가 입력란에 단 사각 테두리를 FlatLaf의 입력란 테두리로 되돌리고, 안쪽 여백만 margin으로 지정한다.
        JTextField textField = getComponentDateTextField();
        Border flatBorder = UIManager.getBorder("TextField.border");
        if (textField.getBorder() != flatBorder) {
            textField.setBorder(flatBorder);
        }
        if (!padding.equals(textField.getMargin())) {
            textField.setMargin(padding);
        }

        // border insets을 반영해서 preferred size를 다시 계산
        Insets insets = textField.getInsets();
        FontMetrics fm = textField.getFontMetrics(textField.getFont());
        int neededHeight = fm.getHeight() + insets.top + insets.bottom;
        int neededWidth = textField.getPreferredSize().width; // 폭은 기존 계산값 유지

        Dimension newSize = new Dimension(neededWidth, neededHeight);
        textField.setPreferredSize(newSize);
        textField.setMinimumSize(newSize);

        // DatePicker(JPanel) 레이아웃 자체를 다시 잡아줘야 실제로 반영됨
        revalidate();
        repaint();
    }
}
