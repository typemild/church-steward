package org.church.steward.ui.component.field;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;

/**
 * 입력란의 글자 수를 최대 길이 이내로 제한하는 필터다. 길이는 {@link String#length()}로 센다(DB 칼럼 길이와 같은
 * 기준).
 * <p>
 * 넘치는 글자는 거부하지 않고 남은 칸만큼만 잘라 넣으며(붙여넣기, {@code setText} 포함), 잘렸으면
 * {@code onTruncated}를 부른다. 입력기(IME)가 조합 중인 글자는 제한하지 않고 통과시킨다. 조합 중에 거부하면 입력기의
 * 상태가 어긋날 수 있기 때문이다. 조합이 끝나 글자가 확정될 때 다시 이 필터를 거치므로 그때 잘린다.
 */
public class MaxLengthDocumentFilter extends DocumentFilter {

    /** 잘렸을 때 띄우는 안내를 보여주는 시간 */
    private static final int HINT_DURATION_MILLIS = 3000;

    private final int maxLength;
    private final Runnable onTruncated;

    public MaxLengthDocumentFilter(int maxLength, Runnable onTruncated) {
        this.maxLength = maxLength;
        this.onTruncated = onTruncated;
    }

    /**
     * 입력란에 최대 길이를 지정한다. 넘치는 글자가 잘리면 입력란 아래에 안내를 잠깐 띄운다.
     */
    static void install(JTextComponent field, int maxLength, InputHintPopup hintPopup) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new MaxLengthDocumentFilter(maxLength,
                () -> hintPopup.show("최대 " + maxLength + "자까지 입력할 수 있습니다.", HINT_DURATION_MILLIS)));
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr)
            throws BadLocationException {
        replace(fb, offset, 0, string, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
            throws BadLocationException {
        if (text == null || text.isEmpty() || isComposedText(attrs)) {
            super.replace(fb, offset, length, text, attrs);
            return;
        }

        int available = Math.max(0, maxLength - (fb.getDocument().getLength() - length));
        if (text.length() <= available) {
            super.replace(fb, offset, length, text, attrs);
            return;
        }

        int cut = available;
        // 서로게이트 쌍(이모지 등)의 앞쪽 절반만 남지 않게 한다.
        if (cut > 0 && Character.isHighSurrogate(text.charAt(cut - 1))) {
            cut--;
        }
        super.replace(fb, offset, length, text.substring(0, cut), attrs);
        onTruncated.run();
    }

    private static boolean isComposedText(AttributeSet attrs) {
        return attrs != null && attrs.isDefined(StyleConstants.ComposedTextAttribute);
    }

}
