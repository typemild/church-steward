package org.church.steward.ui.view.financialrecord;

import java.util.List;

import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.util.NumberUtils;

/**
 * 수입/지출 화면에서 공통으로 쓰는 표시 형식
 */
public final class FinancialRecordFormats {

    /** 전체 예산명("예산A > 예산B > 예산C")의 구분자 */
    public static final String BUDGET_PATH_SEPARATOR = " > ";

    private static final String NAME_SEPARATOR = ", ";

    private FinancialRecordFormats() {
        // 인스턴스화 방지
    }

    /**
     * 금액을 부호가 붙은 한국어 단위 표기("+5만 3,000원", "-1만원")로 변환한다. 0은 부호 없이 표시한다.
     */
    public static String signedAmount(long amount) {
        String sign = "";
        if (amount > 0) {
            sign = "+";
        } else if (amount < 0) {
            sign = "-";
        }
        return sign + NumberUtils.toKoreanAmount(Math.abs(amount));
    }

    /**
     * 관계자를 "[구분] 성명" 형식으로 표기한다. 구분을 알 수 없으면 성명만 반환한다.
     */
    public static String contactLabel(ContactType type, String name) {
        return type != null ? "[" + type.getLabel() + "] " + name : name;
    }

    /**
     * 관계자를 툴팁용 형식(첫 줄에 "[구분] 성명", 둘째 줄에 비고)으로 표기한다. 비고가 없으면 "[구분] 성명"만 반환한다.
     */
    public static String contactTooltip(ContactType type, String name, String remarks) {
        String label = contactLabel(type, name);
        boolean hasRemarks = remarks != null && !remarks.isBlank();
        if (!hasRemarks) {
            return label;
        }
        // 툴팁에서 줄을 바꾸려면 HTML을 써야 한다.
        return "<html>" + escapeHtml(label) + "<br>" + escapeHtml(remarks.trim()) + "</html>";
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    /**
     * 관계자 이름 목록을 ", "로 이어 붙인다. 목록이 없으면 빈 문자열을 반환한다.
     */
    public static String joinNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "";
        }
        return String.join(NAME_SEPARATOR, names);
    }
}
