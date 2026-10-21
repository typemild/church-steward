package org.church.steward.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;

import org.church.steward.infrastructure.security.PasswordPolicy.Violation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("PasswordPolicy 단위 테스트")
class PasswordPolicyTest {

    private static final String STRONG_PASSPHRASE = "correcthorsebatterystaple";

    /** 반복·키보드·연속 패턴이 없는 문장을 잘라 원하는 길이의 비밀번호를 만든다. */
    private static String passwordOfLength(int length) {
        String source = "the quick brown fox jumps over a lazy dog while seven wizards hex my jolly box";
        return source.substring(0, length);
    }

    /** 한글 음절(완성형)로 이루어진 원하는 길이의 비밀번호를 만든다. */
    private static String koreanPasswordOfLength(int length) {
        String source = "봄날의햇살은따뜻하고바람은시원하다";
        return source.substring(0, length);
    }

    private static String toNfd(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD);
    }

    @Nested
    @DisplayName("validate 테스트")
    class ValidateTest {

        @ParameterizedTest(name = "[{index}] 입력: \"{0}\"")
        @NullAndEmptySource
        @DisplayName("비밀번호가 null이거나 빈 문자열이면 TOO_SHORT를 반환한다")
        void validate_nullOrEmptyPassword_returnsTooShort(String password) {
            // Given: 파라미터로 null 또는 빈 문자열이 주어짐

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).containsExactly(Violation.TOO_SHORT);
        }

        @Test
        @DisplayName("비밀번호가 최소 길이(15자)보다 짧으면 TOO_SHORT를 반환한다")
        void validate_lengthBelowMinimum_returnsTooShort() {
            // Given
            String password = passwordOfLength(PasswordPolicy.MIN_LENGTH - 1);

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.TOO_SHORT);
        }

        @Test
        @DisplayName("비밀번호가 정확히 최소 길이(15자)이면 TOO_SHORT를 반환하지 않는다")
        void validate_lengthAtMinimum_doesNotReturnTooShort() {
            // Given
            String password = passwordOfLength(PasswordPolicy.MIN_LENGTH);

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).doesNotContain(Violation.TOO_SHORT);
        }

        @Test
        @DisplayName("비밀번호가 정확히 최대 길이(64자)이면 TOO_LONG을 반환하지 않는다")
        void validate_lengthAtMaximum_doesNotReturnTooLong() {
            // Given
            String password = passwordOfLength(PasswordPolicy.MAX_LENGTH);

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).doesNotContain(Violation.TOO_LONG);
        }

        @Test
        @DisplayName("비밀번호가 최대 길이(64자)를 초과하면 TOO_LONG을 반환한다")
        void validate_lengthAboveMaximum_returnsTooLong() {
            // Given
            String password = passwordOfLength(PasswordPolicy.MAX_LENGTH + 1);

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.TOO_LONG);
        }

        @Test
        @DisplayName("한글 완성형 15글자는 코드포인트 기준 15자로 계산되어 TOO_SHORT를 반환하지 않는다")
        void validate_koreanSyllablesAtMinimum_doesNotReturnTooShort() {
            // Given
            String password = koreanPasswordOfLength(PasswordPolicy.MIN_LENGTH);

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).doesNotContain(Violation.TOO_SHORT);
        }

        @Test
        @DisplayName("조합형(NFD)으로 입력된 한글 14글자는 정규화 후 14자로 계산되어 TOO_SHORT를 반환한다")
        void validate_decomposedKoreanBelowMinimum_returnsTooShort() {
            // Given
            String password = toNfd(koreanPasswordOfLength(PasswordPolicy.MIN_LENGTH - 1));

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.TOO_SHORT);
        }

        @ParameterizedTest(name = "[{index}] 입력: \"{0}\"")
        @ValueSource(strings = { "abcabcabcabcabc", "qwerqwerqwerqwer", "Summer2024Summer2024" })
        @DisplayName("비밀번호 전체가 짧은 단위의 반복이면 REPEATED_PATTERN을 반환한다")
        void validate_repeatedUnit_returnsRepeatedPattern(String password) {
            // Given: 파라미터로 짧은 단위가 반복된 비밀번호가 주어짐

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.REPEATED_PATTERN);
        }

        @Test
        @DisplayName("대소문자만 다른 단위가 반복되어도 대소문자를 무시하고 REPEATED_PATTERN을 반환한다")
        void validate_repeatedUnitWithMixedCase_returnsRepeatedPattern() {
            // Given
            String password = "AbcaBcabCabcABC";

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.REPEATED_PATTERN);
        }

        @Test
        @DisplayName("사용된 문자 종류가 2개 이하이면 REPEATED_PATTERN을 반환한다")
        void validate_twoOrFewerDistinctChars_returnsRepeatedPattern() {
            // Given
            String password = "aabbbaababbbaab";

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.REPEATED_PATTERN);
        }

        @ParameterizedTest(name = "[{index}] 입력: \"{0}\"")
        @ValueSource(strings = { "qwertyuiopasdfgh", "1q2w3e4r5t6y7u8i", "zaq12wsxcde34rfv", "!QAZ2wsx#EDC4rfv" })
        @DisplayName("키보드 배열상 인접한 키를 이어서 입력한 비밀번호(가로·세로·대각선, Shift 포함)이면 SEQUENTIAL_OR_KEYBOARD_PATTERN을 반환한다")
        void validate_keyboardWalk_returnsSequentialOrKeyboardPattern(String password) {
            // Given: 파라미터로 키보드 배열 패턴 비밀번호가 주어짐

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.SEQUENTIAL_OR_KEYBOARD_PATTERN);
        }

        @ParameterizedTest(name = "[{index}] 입력: \"{0}\"")
        @ValueSource(strings = { "abcdefghijklmnop", "9876543210zyxwvu" })
        @DisplayName("영문자·숫자가 오름차순 또는 내림차순으로 연속되면 SEQUENTIAL_OR_KEYBOARD_PATTERN을 반환한다")
        void validate_alphanumericSequence_returnsSequentialOrKeyboardPattern(String password) {
            // Given: 파라미터로 연속 문자 비밀번호가 주어짐

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.SEQUENTIAL_OR_KEYBOARD_PATTERN);
        }

        @Test
        @DisplayName("전각 문자로 입력한 키보드 패턴도 정규화 후 검사되어 SEQUENTIAL_OR_KEYBOARD_PATTERN을 반환한다")
        void validate_fullWidthKeyboardWalk_returnsSequentialOrKeyboardPattern() {
            // Given
            String password = "ｑｗｅｒｔｙｕｉｏｐａｓｄｆｇｈ";

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).contains(Violation.SEQUENTIAL_OR_KEYBOARD_PATTERN);
        }

        @Test
        @DisplayName("약한 패턴이 전체의 50% 미만이면 SEQUENTIAL_OR_KEYBOARD_PATTERN을 반환하지 않는다")
        void validate_weakPatternBelowHalf_doesNotReturnSequentialOrKeyboardPattern() {
            // Given
            String password = "correctqwerhorsebatterystaple";

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).doesNotContain(Violation.SEQUENTIAL_OR_KEYBOARD_PATTERN);
        }

        @ParameterizedTest(name = "[{index}] 입력: \"{0}\"")
        @ValueSource(strings = { STRONG_PASSPHRASE, "Tr0ub4dor&3xyzLong", "봄날의햇살은따뜻하고바람은시원하다" })
        @DisplayName("길이를 충족하고 약한 패턴이 없는 비밀번호는 위반 항목 없이 통과한다")
        void validate_strongPassword_returnsNoViolation(String password) {
            // Given: 파라미터로 강한 비밀번호가 주어짐

            // When
            List<Violation> result = PasswordPolicy.validate(password, List.of());

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("비밀번호에 문맥 단어(아이디 등)가 포함되면 CONTAINS_CONTEXT_WORD를 반환한다")
        void validate_containsContextWord_returnsContainsContextWord() {
            // Given
            String password = "myhyominpassphrase";
            List<String> contextWords = List.of("hyomin");

            // When
            List<Violation> result = PasswordPolicy.validate(password, contextWords);

            // Then
            assertThat(result).contains(Violation.CONTAINS_CONTEXT_WORD);
        }

        @Test
        @DisplayName("문맥 단어와 대소문자가 달라도 대소문자를 무시하고 CONTAINS_CONTEXT_WORD를 반환한다")
        void validate_containsContextWordWithDifferentCase_returnsContainsContextWord() {
            // Given
            String password = "myHYOMINpassphrase";
            List<String> contextWords = List.of("HyoMin");

            // When
            List<Violation> result = PasswordPolicy.validate(password, contextWords);

            // Then
            assertThat(result).contains(Violation.CONTAINS_CONTEXT_WORD);
        }

        @Test
        @DisplayName("문맥 단어가 4자 미만이면 포함 여부를 검사하지 않는다")
        void validate_contextWordShorterThanMinimum_doesNotReturnContainsContextWord() {
            // Given
            String password = "kimchiandricecakes";
            List<String> contextWords = List.of("kim");

            // When
            List<Violation> result = PasswordPolicy.validate(password, contextWords);

            // Then
            assertThat(result).doesNotContain(Violation.CONTAINS_CONTEXT_WORD);
        }

        @Test
        @DisplayName("문맥 단어 목록이 null이면 예외 없이 문맥 단어 검사를 생략한다")
        void validate_nullContextWords_returnsNoViolation() {
            // Given
            String password = STRONG_PASSPHRASE;

            // When
            List<Violation> result = PasswordPolicy.validate(password, null);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("문맥 단어 목록에 null 요소가 있으면 해당 요소를 무시한다")
        void validate_nullElementInContextWords_returnsNoViolation() {
            // Given
            String password = STRONG_PASSPHRASE;
            List<String> contextWords = Arrays.asList((String) null);

            // When
            List<Violation> result = PasswordPolicy.validate(password, contextWords);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("normalize 테스트")
    class NormalizeTest {

        @Test
        @DisplayName("조합형(NFD) 한글을 완성형(NFC) 한글로 변환한다")
        void normalize_decomposedHangul_returnsComposedHangul() {
            // Given
            String decomposed = toNfd("한글");

            // When
            String result = PasswordPolicy.normalize(decomposed);

            // Then
            assertThat(result).isEqualTo("한글");
        }

        @Test
        @DisplayName("전각 영문자·숫자를 반각 문자로 변환한다")
        void normalize_fullWidthCharacters_returnsHalfWidthCharacters() {
            // Given
            String fullWidth = "ＡＢＣ１２３";

            // When
            String result = PasswordPolicy.normalize(fullWidth);

            // Then
            assertThat(result).isEqualTo("ABC123");
        }

        @Test
        @DisplayName("원문자·위첨자 같은 호환 문자를 기본 문자로 변환한다")
        void normalize_compatibilityCharacters_returnsBaseCharacters() {
            // Given
            String compatibility = "①²";

            // When
            String result = PasswordPolicy.normalize(compatibility);

            // Then
            assertThat(result).isEqualTo("12");
        }

        @Test
        @DisplayName("정규화 대상이 없는 ASCII 문자열은 그대로 반환한다")
        void normalize_asciiPassword_returnsSameString() {
            // Given
            String ascii = "Tr0ub4dor&3xyzLong";

            // When
            String result = PasswordPolicy.normalize(ascii);

            // Then
            assertThat(result).isEqualTo(ascii);
        }
    }
}
