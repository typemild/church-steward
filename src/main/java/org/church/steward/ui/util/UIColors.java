package org.church.steward.ui.util;

import java.awt.Color;

/**
 * 🎨 청지기(Steward) UI 디자인 가이드라인의 컬러 팔레트를 정의한 유틸리티 클래스
 */
public final class UIColors {

    private UIColors() {
        // 인스턴스화 방지
    }

    // Primary Button
    public static final Color PRIMARY_BACKGROUND = new Color(52, 152, 219); // #3498DB
    public static final Color PRIMARY_BACKGROUND_HOVER = new Color(41, 128, 185); // #2980B9
    public static final Color PRIMARY_TEXT = Color.WHITE; // #FFFFFF

    // Secondary Button
    public static final Color SECONDARY_BACKGROUND = Color.WHITE; // #FFFFFF
    public static final Color SECONDARY_BACKGROUND_HOVER = new Color(236, 240, 241); // #ECF0F1
    public static final Color SECONDARY_TEXT = new Color(44, 62, 80); // #2C3E50

    // Icon Button
    public static final Color ICON_DISABLED = new Color(189, 195, 199); // #BDC3C7

    // Text Field
    public static final Color TEXT_FIELD_BACKGROUND = Color.WHITE; // #FFFFFF

    // ComboBox
    public static final Color COMBO_BOX_BACKGROUND = Color.WHITE; // #FFFFFF

    // Background
    public static final Color BACKGROUND = new Color(248, 249, 250); // #F8F9FA

    // Sidebar
    public static final Color SIDEBAR = new Color(44, 62, 80); // #2C3E50
    public static final Color SIDEBAR_HOVER = new Color(52, 73, 94); // #34495E

    // Text
    public static final Color TEXT_MAIN = new Color(44, 62, 80); // #2C3E50
    public static final Color TEXT_SUB = new Color(127, 140, 141); // #7F8C8D
    public static final Color TEXT_LIGHT = new Color(236, 240, 241); // #ECF0F1
    public static final Color TEXT_HIGHLIGHT = new Color(52, 152, 219); // #3498DB

    // Chip
    public static final Color CHIP_BACKGROUND = new Color(214, 234, 248); // #D6EAF8
    public static final Color CHIP_TEXT = new Color(44, 62, 80); // #2C3E50

    // Card
    public static final Color CARD_BACKGROUND = Color.WHITE; // #FFFFFF

    // Income/Expense
    public static final Color INCOME = new Color(52, 152, 219); // #3498DB
    public static final Color EXPENSE = new Color(231, 76, 60); // #E74C3C
    public static final Color BALANCE = new Color(39, 174, 96); // #27AE60

    // Chart
    public static final Color CHART_GRID = new Color(236, 240, 241); // #ECF0F1
    public static final Color METER_TRACK = new Color(236, 240, 241); // #ECF0F1

    // Border
    public static final Color BORDER = new Color(210, 215, 220); // #D2D7DC

    // Report (흑백으로 인쇄해도 구분되도록 회색조만 쓴다)
    public static final Color REPORT_PAPER = Color.WHITE; // #FFFFFF
    public static final Color REPORT_TEXT = Color.BLACK; // #000000
    public static final Color REPORT_TEXT_SUB = new Color(85, 85, 85); // #555555
    public static final Color REPORT_LINE = Color.BLACK; // #000000
    public static final Color REPORT_GRID = new Color(170, 170, 170); // #AAAAAA
    public static final Color REPORT_FILL = new Color(235, 235, 235); // #EBEBEB
    public static final Color REPORT_BAR = new Color(85, 85, 85); // #555555
    public static final Color REPORT_PREVIEW_BACKGROUND = new Color(200, 204, 208); // #C8CCD0

    // Error/Warning
    public static final Color ERROR_WARNING = new Color(230, 126, 34); // #E67E22
    public static final Color WARNING_BACKGROUND = new Color(255, 245, 230); // #FFF5E6

    // Input hint (입력란 아래에 잠깐 띄우는 안내)
    public static final Color INPUT_HINT_BACKGROUND = new Color(255, 243, 230); // #FFF3E6
    public static final Color INPUT_HINT_BORDER = new Color(255, 193, 7); // #FFC107
    public static final Color INPUT_HINT_TEXT = new Color(139, 69, 19); // #8B4513

}
