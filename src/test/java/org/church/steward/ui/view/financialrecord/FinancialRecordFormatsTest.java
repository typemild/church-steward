package org.church.steward.ui.view.financialrecord;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.church.steward.domain.contact.entity.ContactType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class FinancialRecordFormatsTest {

    @Nested
    @DisplayName("signedAmount 테스트")
    class SignedAmountTest {

        @Test
        @DisplayName("양수는 한국어 단위 표기 앞에 +를 붙인다")
        void signedAmount_positive_prependsPlusSign() {
            // Given
            long amount = 53_000L;

            // When
            String result = FinancialRecordFormats.signedAmount(amount);

            // Then
            assertThat(result).isEqualTo("+5만 3,000원");
        }

        @Test
        @DisplayName("음수는 절댓값의 한국어 단위 표기 앞에 -를 붙인다")
        void signedAmount_negative_prependsMinusSign() {
            // Given
            long amount = -10_000L;

            // When
            String result = FinancialRecordFormats.signedAmount(amount);

            // Then
            assertThat(result).isEqualTo("-1만원");
        }

        @Test
        @DisplayName("0은 부호 없이 표시한다")
        void signedAmount_zero_returnsAmountWithoutSign() {
            // Given
            long amount = 0L;

            // When
            String result = FinancialRecordFormats.signedAmount(amount);

            // Then
            assertThat(result).isEqualTo("0원");
        }
    }

    @Nested
    @DisplayName("contactLabel 테스트")
    class ContactLabelTest {

        @Test
        @DisplayName("구분이 있으면 [구분] 성명 형식으로 표기한다")
        void contactLabel_withType_returnsTypeAndName() {
            // Given
            ContactType type = ContactType.INDIVIDUAL;

            // When
            String result = FinancialRecordFormats.contactLabel(type, "홍길동");

            // Then
            assertThat(result).isEqualTo("[개인] 홍길동");
        }

        @Test
        @DisplayName("구분을 알 수 없으면 성명만 반환한다")
        void contactLabel_nullType_returnsNameOnly() {
            // Given
            ContactType type = null;

            // When
            String result = FinancialRecordFormats.contactLabel(type, "홍길동");

            // Then
            assertThat(result).isEqualTo("홍길동");
        }
    }

    @Nested
    @DisplayName("contactTooltip 테스트")
    class ContactTooltipTest {

        @Test
        @DisplayName("비고가 있으면 첫 줄에 [구분] 성명, 둘째 줄에 비고를 적은 HTML을 반환한다")
        void contactTooltip_withRemarks_returnsTwoLineHtml() {
            // Given
            String remarks = "청년부";

            // When
            String result = FinancialRecordFormats.contactTooltip(ContactType.INDIVIDUAL, "홍길동", remarks);

            // Then
            assertThat(result).isEqualTo("<html>[개인] 홍길동<br>청년부</html>");
        }

        @Test
        @DisplayName("비고가 null이면 [구분] 성명만 반환한다")
        void contactTooltip_nullRemarks_returnsLabelOnly() {
            // Given
            String remarks = null;

            // When
            String result = FinancialRecordFormats.contactTooltip(ContactType.VENDOR, "가나상회", remarks);

            // Then
            assertThat(result).isEqualTo("[업체] 가나상회");
        }

        @Test
        @DisplayName("비고가 공백뿐이면 [구분] 성명만 반환한다")
        void contactTooltip_blankRemarks_returnsLabelOnly() {
            // Given
            String remarks = "   ";

            // When
            String result = FinancialRecordFormats.contactTooltip(ContactType.VENDOR, "가나상회", remarks);

            // Then
            assertThat(result).isEqualTo("[업체] 가나상회");
        }

        @Test
        @DisplayName("비고의 앞뒤 공백은 없애고 적는다")
        void contactTooltip_remarksWithSpaces_trimsRemarks() {
            // Given
            String remarks = "  청년부  ";

            // When
            String result = FinancialRecordFormats.contactTooltip(ContactType.INDIVIDUAL, "홍길동", remarks);

            // Then
            assertThat(result).isEqualTo("<html>[개인] 홍길동<br>청년부</html>");
        }

        @Test
        @DisplayName("성명과 비고에 든 HTML 특수 문자는 태그로 해석되지 않도록 바꿔 적는다")
        void contactTooltip_htmlSpecialCharacters_escapesThem() {
            // Given
            String name = "A&B <주>";
            String remarks = "<b>굵게</b>";

            // When
            String result = FinancialRecordFormats.contactTooltip(null, name, remarks);

            // Then
            assertThat(result).isEqualTo("<html>A&amp;B &lt;주&gt;<br>&lt;b&gt;굵게&lt;/b&gt;</html>");
        }
    }

    @Nested
    @DisplayName("joinNames 테스트")
    class JoinNamesTest {

        @Test
        @DisplayName("이름이 여럿이면 쉼표와 공백으로 이어 붙인다")
        void joinNames_multipleNames_joinsWithCommaAndSpace() {
            // Given
            List<String> names = List.of("홍길동", "김철수", "이영희");

            // When
            String result = FinancialRecordFormats.joinNames(names);

            // Then
            assertThat(result).isEqualTo("홍길동, 김철수, 이영희");
        }

        @Test
        @DisplayName("이름이 하나이면 그 이름만 반환한다")
        void joinNames_singleName_returnsName() {
            // Given
            List<String> names = List.of("홍길동");

            // When
            String result = FinancialRecordFormats.joinNames(names);

            // Then
            assertThat(result).isEqualTo("홍길동");
        }

        @Test
        @DisplayName("목록이 비어 있으면 빈 문자열을 반환한다")
        void joinNames_emptyList_returnsEmptyString() {
            // Given
            List<String> names = List.of();

            // When
            String result = FinancialRecordFormats.joinNames(names);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("목록이 null이면 빈 문자열을 반환한다")
        void joinNames_nullList_returnsEmptyString() {
            // Given
            List<String> names = null;

            // When
            String result = FinancialRecordFormats.joinNames(names);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
