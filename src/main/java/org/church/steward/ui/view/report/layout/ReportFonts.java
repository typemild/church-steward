package org.church.steward.ui.view.report.layout;

import java.awt.Font;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;

import org.church.steward.domain.report.entity.ReportFontFamily;
import org.church.steward.ui.util.UIFonts;

import lombok.extern.slf4j.Slf4j;

/**
 * 보고서에 쓰는 글꼴. 어느 PC에서나 줄바꿈과 페이지 나눔이 같도록 시스템 글꼴 대신 프로그램에 포함된 나눔고딕과 나눔명조를 쓴다. 아래의
 * 글자 크기는 기본 크기이며, 사용자가 조정하면 {@link ReportStyle}이 모든 글자에 같은 크기를 더하거나 뺀다.
 */
@Slf4j
public final class ReportFonts {

    /** 글자 크기(pt). 한 장에 많이 담기 위해 제목도 12pt를 넘기지 않는다. */
    public static final float SIZE_TITLE = 12f;
    public static final float SIZE_SECTION = 9f;
    public static final float SIZE_BODY = 8f;
    public static final float SIZE_DETAIL = 7.5f;
    public static final float SIZE_SMALL = 7f;
    /** 페이지 번호. 모든 보고서가 같은 크기를 쓰며 글자 크기 조정의 대상이 아니다. */
    public static final float SIZE_PAGE_NUMBER = 9f;

    /**
     * 글꼴 하나의 보통 굵기와 굵은 글꼴 파일
     */
    private record FontFiles(String regularResource, String boldResource) {
    }

    private static final Map<ReportFontFamily, FontFiles> FILES = new EnumMap<>(Map.of(
            ReportFontFamily.NANUM_GOTHIC, new FontFiles("/fonts/nanumgothic/NanumGothic-Regular.ttf",
                    "/fonts/nanumgothic/NanumGothic-Bold.ttf"),
            ReportFontFamily.NANUM_MYEONGJO, new FontFiles("/fonts/nanummyeongjo/NanumMyeongjo-Regular.ttf",
                    "/fonts/nanummyeongjo/NanumMyeongjo-Bold.ttf")));

    private static final Map<ReportFontFamily, Font> REGULAR = new EnumMap<>(ReportFontFamily.class);
    private static final Map<ReportFontFamily, Font> BOLD = new EnumMap<>(ReportFontFamily.class);

    static {
        for (ReportFontFamily family : ReportFontFamily.values()) {
            FontFiles files = FILES.get(family);
            REGULAR.put(family, load(files.regularResource(), Font.PLAIN));
            BOLD.put(family, load(files.boldResource(), Font.BOLD));
        }
    }

    private ReportFonts() {
        // 인스턴스화 방지
    }

    public static Font regular(ReportFontFamily family, float size) {
        return REGULAR.get(family)
                .deriveFont(size);
    }

    public static Font bold(ReportFontFamily family, float size) {
        return BOLD.get(family)
                .deriveFont(size);
    }

    /**
     * {@link #regular}이나 {@link #bold}로 만든 글꼴의 파일. PDF로 저장할 때 어느 글꼴 파일을 넣어야 하는지 가리는
     * 데 쓴다. 알 수 없는 글꼴이면 기본 글꼴의 보통 굵기 파일이다.
     */
    public static String resourceOf(Font font) {
        for (ReportFontFamily family : ReportFontFamily.values()) {
            if (BOLD.get(family)
                    .getPSName()
                    .equals(font.getPSName())) {
                return FILES.get(family)
                        .boldResource();
            }
            if (REGULAR.get(family)
                    .getPSName()
                    .equals(font.getPSName())) {
                return FILES.get(family)
                        .regularResource();
            }
        }
        return FILES.get(ReportFontFamily.NANUM_GOTHIC)
                .regularResource();
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
