package org.church.steward.ui.component.field;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.text.NumberFormat;

import javax.swing.BorderFactory;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;

import org.church.steward.ui.component.ComponentSize;
import org.church.steward.ui.util.FocusIndicator;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.util.UIFonts;
import org.church.steward.util.NumberUtils;

/**
 * 금액 입력 전용 텍스트 필드.
 * - 숫자만 입력 가능
 * - 타이핑 중 실시간 천 단위 콤마(,) 표시
 * - 포커스 상태에서 하단에 한글 금액 팝업 표시
 * - Long 타입 값으로 get/set (빈 값을 허용하는 경우 null 가능)
 */
public class AmountField extends JFormattedTextField {

    private static final long serialVersionUID = 1L;

    /** 창 이동이나 크기 변경이 이 시간(ms) 동안 없으면 끝난 것으로 보고 팝업을 다시 띄운다. */
    private static final int POPUP_RESHOW_DELAY_MS = 200;

    private final NumberFormat displayFormat = NumberFormat.getNumberInstance();
    private final boolean allowEmpty;

    private Popup koreanPopup;
    private JLabel koreanLabel;
    /** 팝업을 띄운 시점의 필드 화면 좌표와 크기. 필드가 움직였는지 판단하는 기준이다. */
    private Rectangle koreanPopupAnchor;
    private Timer koreanPopupReshowTimer;

    public AmountField(ComponentSize componentSize) {
        this(componentSize, false);
    }

    /**
     * @param allowEmpty true이면 빈 값(빈 문자열)을 허용하며, 이 때 {@link #getAmount()}는 null을
     *                   반환한다.
     */
    public AmountField(ComponentSize componentSize, boolean allowEmpty) {
        super();
        this.allowEmpty = allowEmpty;
        applyStyle(componentSize);
        setHorizontalAlignment(RIGHT);
        displayFormat.setGroupingUsed(true);

        ((AbstractDocument) getDocument()).setDocumentFilter(new AmountDocumentFilter());
        getDocument().addDocumentListener(new KoreanUpdateListener());

        initKoreanPopup();
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                showKoreanPopup();
            }

            @Override
            public void focusLost(FocusEvent e) {
                hideKoreanPopup();
            }
        });
        // 필드 자신이나 상위 컨테이너(창 포함)가 이동하거나 크기 변경되는 동안은 팝업을 숨기고, 끝나면 다시 띄운다
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                relocateKoreanPopup();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                relocateKoreanPopup();
            }
        });
        addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
            @Override
            public void ancestorMoved(HierarchyEvent e) {
                relocateKoreanPopup();
            }

            @Override
            public void ancestorResized(HierarchyEvent e) {
                relocateKoreanPopup();
            }
        });

        setText(allowEmpty ? "" : "0");
    }

    public AmountField(int columns, ComponentSize componentSize) {
        this(columns, componentSize, false);
    }

    public AmountField(int columns, ComponentSize componentSize, boolean allowEmpty) {
        this(componentSize, allowEmpty);
        setColumns(columns);
    }

    private void applyStyle(ComponentSize componentSize) {
        final int fontSize = switch (componentSize) {
        case LARGE -> UIFonts.FONT_SIZE_LARGE;
        case MEDIUM -> UIFonts.FONT_SIZE_BASE;
        case SMALL -> UIFonts.FONT_SIZE_SMALL;
        };
        final Font font = new Font(UIFonts.FONT_FAMILY, Font.PLAIN, fontSize);

        final Insets padding = switch (componentSize) {
        case LARGE -> new Insets(
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f),
                Math.round(fontSize * 0.8f));
        case MEDIUM -> new Insets(
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.46f),
                Math.round(fontSize * 0.4f),
                Math.round(fontSize * 0.46f));
        case SMALL -> new Insets(
                Math.round(fontSize * 0.2f),
                Math.round(fontSize * 0.27f),
                Math.round(fontSize * 0.2f),
                Math.round(fontSize * 0.27f));
        };

        setFont(font);
        setBackground(UIColors.TEXT_FIELD_BACKGROUND);
        // 테두리는 FlatLaf의 것을 그대로 쓴다(둥근 모서리, 포커스 색). 안쪽 여백만 margin으로 지정한다.
        setMargin(padding);
        // 포커스 테두리는 입력/수정 다이얼로그에서만 보인다(DialogKeyHandler가 켠다).
        FocusIndicator.hide(this);
    }

    /**
     * 현재 입력된 금액을 반환한다.
     * 비어있으면 빈 값을 허용하는 경우 null을, 그렇지 않은 경우 0을 반환한다.
     */
    public Long getAmount() {
        String digits = getText().replace(",", "");
        if (digits.isEmpty()) {
            return emptyAmount();
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return emptyAmount();
        }
    }

    /**
     * 금액을 설정한다.
     * null이면 빈 값을 허용하는 경우 필드를 비우고, 그렇지 않은 경우 0으로 설정한다.
     */
    public void setAmount(Long amount) {
        if (amount == null) {
            setText(allowEmpty ? "" : "0");
            return;
        }
        setText(displayFormat.format(Math.max(amount, 0L)));
    }

    private Long emptyAmount() {
        return allowEmpty ? null : 0L;
    }

    // ------------------------------------------------------------------
    // 한글 금액 팝업
    // ------------------------------------------------------------------

    private void initKoreanPopup() {
        koreanLabel = new JLabel(" ", JLabel.RIGHT);
        koreanLabel.setOpaque(true);
        koreanLabel.setBackground(new Color(255, 255, 225));
        koreanLabel.setBorder(new javax.swing.border.CompoundBorder(
                new LineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));

        koreanPopupReshowTimer = new Timer(POPUP_RESHOW_DELAY_MS, _ -> {
            if (isFocusOwner()) {
                showKoreanPopup();
            }
        });
        koreanPopupReshowTimer.setRepeats(false);
    }

    private void showKoreanPopup() {
        if (!isShowing()) {
            return;
        }

        Long amount = getAmount();
        if (amount == null) {
            hideKoreanPopup();
            return;
        }

        koreanLabel.setText(NumberUtils.toKoreanAmount(amount) + " ");
        if (koreanPopup != null) {
            if (koreanPopupWidth() <= koreanLabel.getWidth()) {
                // 이미 떠 있는 팝업은 라벨이 화면에 붙어 있으므로 텍스트만 바꾸면 스스로 다시 그려진다
                return;
            }
            // 글자가 팝업보다 길어졌으면 넓혀서 다시 띄운다
            hideKoreanPopup();
        }
        openKoreanPopup();
    }

    private void openKoreanPopup() {
        int width = koreanPopupWidth();
        int height = koreanLabel.getPreferredSize().height;
        koreanLabel.setPreferredSize(new Dimension(width, height));
        koreanLabel.setSize(width, height);

        // 금액이 우측 정렬이므로 팝업도 필드의 오른쪽 끝에 맞추고, 필드보다 넓으면 왼쪽으로 늘린다
        Point loc = getLocationOnScreen();
        koreanPopup = PopupFactory.getSharedInstance()
                .getPopup(this, koreanLabel, loc.x + getWidth() - width, loc.y + getHeight());
        koreanPopup.show();
        koreanPopupAnchor = new Rectangle(loc, getSize());
    }

    /**
     * 팝업 너비는 텍스트 필드 너비와 같게 하되, 한글 금액이 그보다 길면 글자가 잘리지 않을 만큼 넓힌다.
     */
    private int koreanPopupWidth() {
        Insets insets = koreanLabel.getInsets();
        int textWidth = koreanLabel.getFontMetrics(koreanLabel.getFont()).stringWidth(koreanLabel.getText());
        return Math.max(getWidth(), textWidth + insets.left + insets.right);
    }

    private void hideKoreanPopup() {
        if (koreanPopup != null) {
            koreanPopup.hide();
            koreanPopup = null;
            koreanPopupAnchor = null;
        }
    }

    /**
     * 필드의 화면 위치나 크기가 바뀌면 팝업을 숨기고, 변경이 멈춘 뒤 새 위치에 다시 띄운다.
     * 드래그 중에는 이벤트가 연달아 오므로 그때마다 타이머를 다시 시작한다.
     */
    private void relocateKoreanPopup() {
        if (koreanPopup == null) {
            if (koreanPopupReshowTimer.isRunning()) {
                koreanPopupReshowTimer.restart();
            }
            return;
        }
        if (isShowing() && new Rectangle(getLocationOnScreen(), getSize()).equals(koreanPopupAnchor)) {
            return;
        }
        hideKoreanPopup();
        koreanPopupReshowTimer.restart();
    }

    private class KoreanUpdateListener implements DocumentListener {
        @Override
        public void insertUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            update();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            update();
        }

        private void update() {
            SwingUtilities.invokeLater(() -> {
                if (isFocusOwner()) {
                    showKoreanPopup();
                }
            });
        }
    }

    private class AmountDocumentFilter extends DocumentFilter {

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            handleChange(fb, offset, 0, text);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            handleChange(fb, offset, length, text);
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            handleChange(fb, offset, length, "");
        }

        private void handleChange(FilterBypass fb, int offset, int length, String insertedText)
                throws BadLocationException {
            Document doc = fb.getDocument();
            String currentText = doc.getText(0, doc.getLength());

            String digitsInsert = insertedText == null ? "" : insertedText.replaceAll("[^0-9]", "");

            String before = currentText.substring(0, offset).replaceAll("[^0-9]", "");
            String after = currentText.substring(offset + length).replaceAll("[^0-9]", "");
            String rawDigits = before + digitsInsert + after;

            String normalized = rawDigits.replaceFirst("^0+(?=\\d)", "");

            String formatted;
            int digitsBeforeCaret = before.length() + digitsInsert.length();

            if (normalized.isEmpty()) {
                formatted = allowEmpty ? "" : "0";
                digitsBeforeCaret = allowEmpty ? 0 : 1;
            } else {
                long value = Long.parseLong(normalized);
                formatted = displayFormat.format(value);
            }

            int newCaretPos = positionAfterNDigits(formatted, digitsBeforeCaret);

            fb.remove(0, doc.getLength());
            fb.insertString(0, formatted, null);

            setCaretLater(newCaretPos);
        }

        private int positionAfterNDigits(String text, int n) {
            if (n <= 0) {
                return 0;
            }
            int count = 0;
            for (int i = 0; i < text.length(); i++) {
                if (Character.isDigit(text.charAt(i))) {
                    count++;
                }
                if (count == n) {
                    return i + 1;
                }
            }
            return text.length();
        }

        private void setCaretLater(int pos) {
            SwingUtilities.invokeLater(() -> {
                try {
                    setCaretPosition(Math.min(pos, getText().length()));
                } catch (Exception ignored) {
                }
            });
        }
    }
}
