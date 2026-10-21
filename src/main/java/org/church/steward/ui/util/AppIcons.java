package org.church.steward.ui.util;

import java.awt.Image;
import java.awt.Taskbar;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로그램 아이콘을 창(제목줄, 작업 표시줄)과 macOS의 Dock에 붙인다.
 * <p>
 * 원본 그림은 src/main/packaging에 있고 빌드할 때 클래스패스의 icons/로 복사된다(build.gradle의
 * processResources).
 * 실행 파일(exe)의 아이콘은 jpackage가 따로 붙이므로, 이 클래스는 실행 중인 창의 아이콘만 다룬다.
 * 아이콘을 읽지 못해도 프로그램은 계속 쓸 수 있어야 하므로 실패는 로그만 남기고 기본 아이콘으로 둔다.
 */
@Slf4j
public final class AppIcons {

    /** 크기별 창 아이콘. 실행 파일 아이콘(ChurchSteward.ico)에 든 그림을 크기마다 꺼내 둔 것이다. */
    private static final String WINDOW_ICON_RESOURCE = "/icons/ChurchSteward-%d.png";
    private static final String LARGE_ICON_RESOURCE = "/icons/ChurchSteward.png";

    /**
     * 창에 넘길 크기들. OS는 제목줄, 작업 표시줄, Alt+Tab마다 가장 가까운 크기를 골라 쓰므로,
     * 큰 그림 하나를 OS가 거칠게 줄이지 않도록 작은 크기용으로 다듬은 그림을 함께 준다.
     */
    private static final int[] WINDOW_ICON_SIZES = { 16, 24, 32, 48, 64, 128, 256 };

    private static List<Image> windowIcons;

    private AppIcons() {
    }

    /**
     * 프로그램 전체의 기본 아이콘을 지정한다. 첫 창을 띄우기 전에 한 번 호출한다.
     * <ul>
     * <li>부모 창 없이 띄우는 메시지 창과 다이얼로그는 Swing의 숨은 공용 창을 부모로 삼으므로 그 창에 아이콘을 붙인다.</li>
     * <li>macOS에서는 Dock 아이콘도 바꾼다(jar로 실행할 때).</li>
     * </ul>
     */
    public static void install() {
        apply(JOptionPane.getRootFrame());

        if (Taskbar.isTaskbarSupported() && Taskbar.getTaskbar().isSupported(Taskbar.Feature.ICON_IMAGE)) {
            BufferedImage large = read(LARGE_ICON_RESOURCE);
            if (large != null) {
                Taskbar.getTaskbar().setIconImage(large);
            }
        }
    }

    /**
     * 창에 프로그램 아이콘을 붙인다. 부모 창이 없는 창(로그인, 최초 실행, 복원, 메인 화면)에서 호출한다.
     * 부모 창이 있는 다이얼로그는 부모 창의 아이콘을 따른다.
     */
    public static void apply(Window window) {
        List<Image> icons = getWindowIcons();
        if (!icons.isEmpty()) {
            window.setIconImages(icons);
        }
    }

    private static synchronized List<Image> getWindowIcons() {
        if (windowIcons == null) {
            List<Image> icons = new ArrayList<>();
            for (int size : WINDOW_ICON_SIZES) {
                BufferedImage icon = read(String.format(WINDOW_ICON_RESOURCE, size));
                if (icon != null) {
                    icons.add(icon);
                }
            }
            windowIcons = List.copyOf(icons);
        }
        return windowIcons;
    }

    private static BufferedImage read(String resource) {
        try (InputStream in = AppIcons.class.getResourceAsStream(resource)) {
            if (in == null) {
                log.warn("Icon resource not found: {}", resource);
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException e) {
            log.warn("Failed to read icon: {}", resource, e);
            return null;
        }
    }
}
