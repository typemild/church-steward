package org.church.steward.ui.view.report.layout;

import java.awt.Font;
import java.io.InputStream;

import org.church.steward.ui.util.UIFonts;

import lombok.extern.slf4j.Slf4j;

/**
 * 보고서에 쓰는 글꼴.
 * 어느 PC에서나 줄바꿈과 쪽 나눔이 같도록 시스템 글꼴 대신 프로그램에 포함된 나눔고딕을 쓴다.
 */
@Slf4j
public final class ReportFonts {

    public static final String REGULAR_RESOURCE = "/fonts/nanumgothic/NanumGothic-Regular.ttf";
    public static final String BOLD_RESOURCE = "/fonts/nanumgothic/NanumGothic-Bold.ttf";

    /** 글자 크기(pt). 한 장에 많이 담기 위해 제목도 12pt를 넘기지 않는다. */
    public static final float SIZE_TITLE = 12f;
    public static final float SIZE_SECTION = 9f;
    public static final float SIZE_BODY = 8f;
    public static final float SIZE_DETAIL = 7.5f;
    public static final float SIZE_SMALL = 7f;
    /** 쪽 번호. 모든 보고서가 같은 크기를 쓴다. */
    public static final float SIZE_PAGE_NUMBER = 9f;

    private static final Font REGULAR = load(REGULAR_RESOURCE, Font.PLAIN);
    private static final Font BOLD = load(BOLD_RESOURCE, Font.BOLD);

    private ReportFonts() {
        // 인스턴스화 방지
    }

    public static Font regular(float size) {
        return REGULAR.deriveFont(size);
    }

    public static Font bold(float size) {
        return BOLD.deriveFont(size);
    }

    /**
     * {@link #bold(float)}로 만든 글꼴인지 여부. PDF로 저장할 때 어느 글꼴 파일을 써야 하는지 가리는 데 쓴다.
     */
    public static boolean isBold(Font font) {
        return BOLD.getPSName()
                .equals(font.getPSName());
    }

    /**
     * 포함된 글꼴 파일을 읽는다. 읽지 못하면 보고서를 아예 못 만드는 것보다 나으므로 시스템 글꼴로 대신한다.
     */
    private static Font load(String resource, int fallbackStyle) {
        try (InputStream in = ReportFonts.class.getResourceAsStream(resource)) {
            if (in == null) {
                throw new IllegalStateException("글꼴 파일이 없습니다: " + resource);
            }
            return Font.createFont(Font.TRUETYPE_FONT, in);
        } catch (Exception e) {
            log.error("Failed to load report font: {}", resource, e);
            return new Font(UIFonts.FONT_FAMILY, fallbackStyle, 1);
        }
    }
}
