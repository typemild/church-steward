package org.church.steward.ui.view.report.viewer;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;

import org.church.steward.ui.util.UIColors;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.swing.FontIcon;

import lombok.extern.slf4j.Slf4j;

/**
 * 미리보기의 페이지를 끌어서 옮길 때 쓰는 커서. 이름은 CSS의 커서 이름(grab, grabbing)을 따랐다.
 * 끌 수 있는 곳 위에서는 펼친 손(grab)을, 끄는 동안에는 쥔 손(grabbing)을 아이콘으로 직접 만들어 쓴다. 만들지 못하면 뜻이
 * 같은 이동 커서({@link Cursor#MOVE_CURSOR})를 쓴다.
 * <p>
 * 화면 배율이 1배보다 큰 화면(Retina, Windows의 125%·150% 등)에서 1배 그림을 늘려 그리면 흐려지므로, 배율별 그림을
 * 함께 넘긴다.
 * 흰 테두리는 두르지 않는다. 테두리를 아이콘을 여러 번 밀어 그려 만들면 가장자리가 번져 더 흐려 보이고, 진한 손 모양은 테두리 없이도
 * 회색 바탕과 흰 페이지 위에서 잘 보인다.
 */
@Slf4j
final class GrabCursors {

    /** 커서 크기에 대한 아이콘 크기의 비율 */
    private static final double ICON_RATIO = 18.0 / 32.0;
    /** 함께 넘기는 그림의 배율. 운영체제가 화면 배율에 맞는 것을 고른다. */
    private static final double[] SCALES = { 1, 1.25, 1.5, 2, 3 };

    private static final Cursor GRAB = create(FontAwesomeSolid.HAND_PAPER, "report-grab");
    private static final Cursor GRABBING = create(FontAwesomeSolid.HAND_ROCK, "report-grabbing");

    private GrabCursors() {
        // 인스턴스화 방지
    }

    /**
     * 끌 수 있는 곳 위에 있을 때의 커서(펼친 손)
     */
    static Cursor grab() {
        return GRAB;
    }

    /**
     * 끄는 동안의 커서(쥔 손)
     */
    static Cursor grabbing() {
        return GRABBING;
    }

    private static Cursor create(FontAwesomeSolid icon, String name) {
        try {
            Toolkit toolkit = Toolkit.getDefaultToolkit();
            Dimension size = toolkit.getBestCursorSize(32, 32);
            if (size.width <= 0 || size.height <= 0) {
                return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
            }

            Image[] variants = new Image[SCALES.length];
            for (int i = 0; i < SCALES.length; i++) {
                variants[i] = draw(icon, (int) Math.round(size.width * SCALES[i]),
                        (int) Math.round(size.height * SCALES[i]));
            }
            // 첫 그림(1배)이 기준이며, 커서의 크기와 기준점은 이 그림의 좌표로 정한다.
            Image image = new BaseMultiResolutionImage(variants);
            return toolkit.createCustomCursor(image, new Point(size.width / 2, size.height / 2), name);
        } catch (RuntimeException e) {
            log.warn("Failed to create hand cursor: {}", name, e);
            return Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
        }
    }

    /**
     * 주어진 크기의 투명한 그림 가운데에 아이콘을 그린다.
     */
    private static BufferedImage draw(FontAwesomeSolid icon, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int iconSize = (int) Math.round(Math.min(width, height) * ICON_RATIO);
            FontIcon.of(icon, iconSize, UIColors.TEXT_MAIN)
                    .paintIcon(null, g, (width - iconSize) / 2, (height - iconSize) / 2);
        } finally {
            g.dispose();
        }
        return image;
    }
}
