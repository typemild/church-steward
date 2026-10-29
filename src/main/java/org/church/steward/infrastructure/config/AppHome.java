package org.church.steward.infrastructure.config;

import java.io.File;
import java.nio.file.Path;

import org.springframework.boot.system.ApplicationHome;

/**
 * 프로그램 폴더를 정한다. 설정 파일(application.yml), 데이터베이스(data/), 로그(logs/)는 모두 이 폴더 아래에
 * 생긴다.
 * <ul>
 * <li>jpackage로 만든 실행 파일로 실행하면 실행 파일이 있는 폴더다. 바로 가기의 "시작 위치"와 무관하게 압축을 푼 폴더에
 * 데이터가 모이게 하기 위함이다.</li>
 * <li>jar로 실행하면 jar가 있는 폴더다. 더블클릭으로 실행하면 작업 디렉터리가 OS마다 다르기 때문이다.</li>
 * <li>그 밖의 경우(IDE, {@code bootRun}, 테스트)는 작업 디렉터리다.</li>
 * </ul>
 * Logback이 로그 파일의 위치를 정할 때도 쓰므로({@link LogFilePropertyDefiner}) 이 클래스는 로그를 남기지
 * 않는다.
 */
public final class AppHome {

    /** jpackage로 만든 실행 파일이 JVM에 넘겨 주는 시스템 속성. 값은 실행 파일의 경로다. */
    static final String JPACKAGE_APP_PATH = "jpackage.app-path";

    private static final Path DIR = determine(System.getProperty(JPACKAGE_APP_PATH),
            new ApplicationHome(AppHome.class).getSource(), Path.of(""));

    private AppHome() {
    }

    /**
     * 프로그램 폴더의 절대 경로.
     */
    public static Path dir() {
        return DIR;
    }

    /**
     * 로그 파일의 절대 경로. 오류 안내 문구에도 이 경로를 적는다.
     */
    public static Path logFile() {
        return DIR.resolve("logs").resolve("app.log");
    }

    /**
     * 실행 환경에서 프로그램 폴더를 정한다.
     *
     * @param jpackageAppPath jpackage 실행 파일의 경로. jpackage로 실행하지 않았으면 null
     * @param codeSource      이 클래스를 읽어 들인 jar 파일 또는 클래스 디렉터리. 알 수 없으면 null
     * @param workingDir      작업 디렉터리
     */
    static Path determine(String jpackageAppPath, File codeSource, Path workingDir) {
        if (jpackageAppPath != null && !jpackageAppPath.isBlank()) {
            return Path.of(jpackageAppPath).toAbsolutePath().getParent();
        }
        if (codeSource != null && codeSource.isFile()) {
            return codeSource.toPath().toAbsolutePath().getParent();
        }
        return workingDir.toAbsolutePath();
    }
}
