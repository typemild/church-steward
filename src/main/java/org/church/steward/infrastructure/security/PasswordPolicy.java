package org.church.steward.infrastructure.security;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiPredicate;

/**
 * NIST SP 800-63B-4 기준 비밀번호 검증 유틸 (완전 오프라인, 외부 의존성 없음).
 * <ul>
 * <li>문자 종류 조합 규칙 없음</li>
 * <li>길이: 15~64자 (유니코드 코드포인트 기준)</li>
 * <li>반복 패턴(aaaa, abcabc...), 연속 문자(abcd, 4321), 키보드 배열(qwer, 1q2w, asdf),
 * 아이디/서비스명 등 문맥 단어 포함 여부 검사</li>
 * </ul>
 */
public final class PasswordPolicy {

    public enum Violation {
        TOO_SHORT("비밀번호는 " + MIN_LENGTH + "자 이상이어야 합니다. 여러 단어를 이어 만든 문장형 비밀번호를 권장합니다."),
        TOO_LONG("비밀번호는 " + MAX_LENGTH + "자 이하여야 합니다."),
        REPEATED_PATTERN("같은 문자나 문자열을 반복한 비밀번호는 사용할 수 없습니다."),
        SEQUENTIAL_OR_KEYBOARD_PATTERN("연속된 문자(abcd, 1234)나 키보드 배열(qwer, asdf)을 주로 사용한 비밀번호는 사용할 수 없습니다."),
        CONTAINS_CONTEXT_WORD("아이디나 서비스 이름 등 추측하기 쉬운 단어가 포함된 비밀번호는 사용할 수 없습니다.");

        private final String message;

        Violation(String message) {
            this.message = message;
        }

        /** 사용자에게 그대로 보여줄 수 있는 한국어 안내 메시지 */
        public String getMessage() {
            return message;
        }
    }

    public static final int MIN_LENGTH = 15;
    public static final int MAX_LENGTH = 64;

    private static final int MIN_SEQUENCE_RUN = 4; // 연속/키보드 패턴으로 보는 최소 길이
    private static final int MIN_SAME_CHAR_RUN = 3; // 같은 문자 반복으로 보는 최소 길이
    private static final double MAX_WEAK_RATIO = 0.5; // 약한 패턴이 차지하는 비율 상한
    private static final int MIN_CONTEXT_WORD_LENGTH = 4;

    // QWERTY 배열 (행별 가로 오프셋은 실제 키보드의 엇갈림을 근사)
    private static final String[] ROWS = { "`1234567890-=", "qwertyuiop[]\\", "asdfghjkl;'", "zxcvbnm,./" };
    private static final String[] SHIFTED = { "~!@#$%^&*()_+", "QWERTYUIOP{}|", "ASDFGHJKL:\"", "ZXCVBNM<>?" };
    private static final double[] ROW_OFFSETS = { 0, 1.5, 1.75, 2.25 };
    private static final Map<Integer, double[]> KEY_POS = new HashMap<>();

    static {
        for (int r = 0; r < ROWS.length; r++) {
            for (int c = 0; c < ROWS[r].length(); c++) {
                double[] pos = { r, ROW_OFFSETS[r] + c };
                KEY_POS.put((int) ROWS[r].charAt(c), pos);
                KEY_POS.put((int) SHIFTED[r].charAt(c), pos);
            }
        }
    }

    private PasswordPolicy() {
        // 인스턴스화 방지
    }

    /** 검증뿐 아니라 해싱(가입/변경)과 로그인 비교 전에도 동일하게 적용해야 한다. */
    public static String normalize(String password) {
        return Normalizer.normalize(password, Normalizer.Form.NFKC);
    }

    /**
     * @param rawPassword  사용자가 입력한 비밀번호
     * @param contextWords 아이디, 이메일 로컬파트, 서비스명 등 포함되면 안 되는 단어 (null 허용)
     * @return 위반 항목 목록 (비어 있으면 통과)
     */
    public static List<Violation> validate(String rawPassword, Collection<String> contextWords) {
        List<Violation> violations = new ArrayList<>();
        if (rawPassword == null || rawPassword.isEmpty()) {
            violations.add(Violation.TOO_SHORT);
            return violations;
        }

        String pw = normalize(rawPassword);
        int length = pw.codePointCount(0, pw.length());
        if (length < MIN_LENGTH)
            violations.add(Violation.TOO_SHORT);
        if (length > MAX_LENGTH)
            violations.add(Violation.TOO_LONG);

        String lower = pw.toLowerCase(Locale.ROOT);
        int[] cps = lower.codePoints().toArray();

        if (isRepeatedUnit(lower) || Arrays.stream(cps).distinct().count() <= 2) {
            violations.add(Violation.REPEATED_PATTERN);
        }
        if (weakPatternRatio(cps) >= MAX_WEAK_RATIO) {
            violations.add(Violation.SEQUENTIAL_OR_KEYBOARD_PATTERN);
        }
        if (containsContextWord(lower, contextWords)) {
            violations.add(Violation.CONTAINS_CONTEXT_WORD);
        }
        return violations;
    }

    /** 전체가 더 짧은 단위의 반복인지 (abcabcabc, qwerqwerqwer) */
    private static boolean isRepeatedUnit(String s) {
        return s.length() > 1 && (s + s).indexOf(s, 1) < s.length();
    }

    /** 같은 문자 반복/연속 문자/키보드 인접 입력이 차지하는 비율 */
    private static double weakPatternRatio(int[] cps) {
        if (cps.length == 0)
            return 0;
        boolean[] weak = new boolean[cps.length];
        markRuns(cps, weak, MIN_SAME_CHAR_RUN, (a, b) -> a.intValue() == b.intValue());
        markRuns(cps, weak, MIN_SEQUENCE_RUN, PasswordPolicy::isKeyboardAdjacent);
        markSequences(cps, weak);

        int count = 0;
        for (boolean w : weak)
            if (w)
                count++;
        return (double) count / cps.length;
    }

    /** 인접한 두 문자가 조건을 계속 만족하는 구간이 minLen 이상이면 표시 */
    private static void markRuns(int[] cps, boolean[] weak, int minLen, BiPredicate<Integer, Integer> linked) {
        int start = 0;
        for (int i = 1; i <= cps.length; i++) {
            boolean continues = i < cps.length && linked.test(cps[i - 1], cps[i]);
            if (!continues) {
                if (i - start >= minLen)
                    Arrays.fill(weak, start, i, true);
                start = i;
            }
        }
    }

    /** 같은 방향으로 1씩 증가/감소하는 영숫자 구간 (abcd, 9876) */
    private static void markSequences(int[] cps, boolean[] weak) {
        int start = 0;
        int step = 0;
        for (int i = 1; i <= cps.length; i++) {
            int d = i < cps.length ? sequenceStep(cps[i - 1], cps[i]) : 0;
            if (d != 0 && (step == 0 || d == step)) {
                step = d;
                continue;
            }
            if (i - start >= MIN_SEQUENCE_RUN)
                Arrays.fill(weak, start, i, true);
            start = (d != 0) ? i - 1 : i; // 방향이 바뀐 경우 직전 문자부터 새 구간 시작
            step = d;
        }
    }

    private static int sequenceStep(int a, int b) {
        if (!isAsciiAlnum(a) || !isAsciiAlnum(b))
            return 0;
        int d = b - a;
        return (d == 1 || d == -1) ? d : 0;
    }

    private static boolean isAsciiAlnum(int c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'z');
    }

    private static boolean isKeyboardAdjacent(int a, int b) {
        double[] p = KEY_POS.get(a);
        double[] q = KEY_POS.get(b);
        if (p == null || q == null)
            return false;
        double dy = Math.abs(p[0] - q[0]);
        double dx = Math.abs(p[1] - q[1]);
        return (dy == 0 && dx == 1) || (dy == 1 && dx <= 0.75);
    }

    private static boolean containsContextWord(String lowerPw, Collection<String> words) {
        if (words == null)
            return false;
        for (String w : words) {
            if (w == null)
                continue;
            String lw = normalize(w).toLowerCase(Locale.ROOT).trim();
            if (lw.length() >= MIN_CONTEXT_WORD_LENGTH && lowerPw.contains(lw))
                return true;
        }
        return false;
    }
}
