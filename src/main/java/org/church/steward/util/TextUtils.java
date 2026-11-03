package org.church.steward.util;

public class TextUtils {

    private TextUtils() {
        // 인스턴스화 방지
    }

    /**
     * 입력받은 글자 값(이름, 내용, 비고)을 저장할 형태로 정리한다. 앞뒤 공백(전각 공백 등 유니코드 공백 포함)을 지우고, 지운 뒤 비어
     * 있으면 null을 돌려준다.
     * <p>
     * 비워 둘 수 있는 값은 "값 없음"을 빈 문자열과 null 두 가지로 저장하지 않기 위함이고, 반드시 입력해야 하는 값(관계자 이름,
     * 예산명, 회계 연도 이름)은 화면과 가져오기가 지우는 공백이 달라도 같은 이름으로 저장되어 이름으로 비교하는 곳(가져오기의 대조, 이름
     * 중복 검사 등)이 어긋나지 않게 하기 위함이다. 반드시 입력해야 하는 값이 null이 되면 호출한 쪽의 검증이 거절한다.
     *
     * @param text 정리할 값
     * @return 앞뒤 공백을 지운 값, 비어 있으면 null
     */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        String stripped = text.strip();
        return stripped.isEmpty() ? null : stripped;
    }
}
