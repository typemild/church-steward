package org.church.steward.ui.component.field;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MaxLengthDocumentFilterTest {

    /**
     * 최대 길이가 5이고, 잘린 횟수를 세는 문서를 만든다.
     */
    private static PlainDocument createDocument(AtomicInteger truncatedCount) {
        PlainDocument document = new PlainDocument();
        document.setDocumentFilter(new MaxLengthDocumentFilter(5, truncatedCount::incrementAndGet));
        return document;
    }

    private static String textOf(PlainDocument document) throws BadLocationException {
        return document.getText(0, document.getLength());
    }

    @Nested
    @DisplayName("insertString 테스트")
    class InsertStringTest {

        @Test
        @DisplayName("최대 길이 이내의 글자는 그대로 넣는다")
        void insertString_withinLimit_insertsAll() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());

            // When
            document.insertString(0, "가나다", null);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다");
        }

        @Test
        @DisplayName("최대 길이와 같아지는 글자는 그대로 넣고 잘렸다고 알리지 않는다")
        void insertString_reachesLimit_doesNotNotify() throws BadLocationException {
            // Given
            AtomicInteger truncatedCount = new AtomicInteger();
            PlainDocument document = createDocument(truncatedCount);

            // When
            document.insertString(0, "가나다라마", null);

            // Then
            assertThat(truncatedCount.get()).isZero();
        }

        @Test
        @DisplayName("붙여넣은 글자가 넘치면 남은 칸만큼만 넣는다")
        void insertString_exceedsLimit_insertsOnlyAvailable() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나", null);

            // When
            document.insertString(2, "다라마바사", null);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다라마");
        }

        @Test
        @DisplayName("글자가 잘리면 잘렸다고 알린다")
        void insertString_exceedsLimit_notifiesTruncation() throws BadLocationException {
            // Given
            AtomicInteger truncatedCount = new AtomicInteger();
            PlainDocument document = createDocument(truncatedCount);

            // When
            document.insertString(0, "가나다라마바", null);

            // Then
            assertThat(truncatedCount.get()).isEqualTo(1);
        }

        @Test
        @DisplayName("이미 가득 차 있으면 글자를 넣지 않는다")
        void insertString_full_insertsNothing() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나다라마", null);

            // When
            document.insertString(5, "바", null);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다라마");
        }

        @Test
        @DisplayName("입력기가 조합 중인 글자는 최대 길이를 넘어도 넣는다")
        void insertString_composedText_insertsAll() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나다라마", null);
            SimpleAttributeSet composed = new SimpleAttributeSet();
            composed.addAttribute(StyleConstants.ComposedTextAttribute, "바");

            // When
            document.insertString(5, "바", composed);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다라마바");
        }

        @Test
        @DisplayName("잘리는 자리가 서로게이트 쌍의 가운데이면 그 글자를 통째로 뺀다")
        void insertString_cutInsideSurrogatePair_dropsWholeCharacter() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나다라", null);

            // When
            document.insertString(4, "😀", null);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다라");
        }
    }

    @Nested
    @DisplayName("replace 테스트")
    class ReplaceTest {

        @Test
        @DisplayName("선택한 글자를 바꿀 때는 지워지는 글자 수만큼 칸을 더 쓸 수 있다")
        void replace_selection_countsRemovedLength() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나다라마", null);

            // When
            document.replace(3, 2, "바사", null);

            // Then
            assertThat(textOf(document)).isEqualTo("가나다바사");
        }

        @Test
        @DisplayName("전체를 바꾸는 글자(setText)가 넘치면 최대 길이까지만 넣는다")
        void replace_wholeTextExceedsLimit_truncates() throws BadLocationException {
            // Given
            PlainDocument document = createDocument(new AtomicInteger());
            document.insertString(0, "가나", null);

            // When
            document.replace(0, 2, "ABCDEFG", null);

            // Then
            assertThat(textOf(document)).isEqualTo("ABCDE");
        }
    }
}
