package org.church.steward;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.church.steward.domain.backup.service.BackupService;
import org.church.steward.domain.fiscalyear.service.FiscalYearService;
import org.church.steward.domain.user.service.UserService;
import org.church.steward.infrastructure.backup.BackupManager;
import org.church.steward.infrastructure.backup.BackupManifest;
import org.church.steward.infrastructure.config.AppHome;
import org.church.steward.infrastructure.config.AppSettingManager;
import org.church.steward.infrastructure.config.DatabaseManager;
import org.church.steward.ui.util.AppIcons;
import org.church.steward.ui.util.UIColors;
import org.church.steward.ui.view.login.DatabaseInitView;
import org.church.steward.ui.view.login.DatabaseRestoreView;
import org.church.steward.ui.view.login.LoginView;
import org.church.steward.ui.view.main.MainView;
import org.church.steward.util.ProgramVersion;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class ChurchStewardApplication {

    private static final String PROGRAM_VERSION = new ProgramVersion().current();
    private static final AppSettingManager appSettingManager = new AppSettingManager();
    private static final DatabaseManager databaseManager = new DatabaseManager(appSettingManager);
    private static final BackupManager backupManager = new BackupManager(databaseManager, appSettingManager,
            PROGRAM_VERSION);
    private static ConfigurableApplicationContext mainContext;

    /**
     * 최초 실행 화면에서 사용자가 초기화를 취소했을 때 던진다. 실패가 아니므로 오류 로그를 남기지 않는다.
     */
    private static class InitializationCancelledException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        InitializationCancelledException() {
            super("데이터베이스 초기화가 취소되었습니다.");
        }
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        // 데이터가 어느 폴더에 있는지 알 수 있도록 프로그램 폴더와 작업 디렉터리를 함께 남긴다.
        log.info("Starting Church Steward {} (Java {}, {} {}), home: {}, working directory: {}", PROGRAM_VERSION,
                System.getProperty("java.version"), System.getProperty("os.name"), System.getProperty("os.version"),
                AppHome.dir(), Path.of("").toAbsolutePath());

        // 1. 테마 설정
        if (SystemInfo.isMacOS) {
            // [macOS 툴팁 깜빡임 우회] 2026-10-13
            //
            // 1. 증상: 최신 macOS에서 버튼 툴팁이 처음 표시될 때 깜빡이며 바로 닫히고,
            // 마우스를 다시 올려야만 정상 표시됨. (Windows에서는 재현 안 됨)
            //
            // 2. 원인: FlatLaf의 macOS 네이티브 라운드 팝업 테두리 적용 과정과 최신 macOS 간의 충돌로 추정.
            // 이 속성을 false로 하면 증상 사라짐.
            //
            // 3. 본 우회 조치에 의한 영향: macOS에서 툴팁, 메뉴, 콤보박스 팝업이 사각 테두리로 표시됨.
            // (그림자는 macOS가 계속 그려줌)
            //
            // 4. 주의: 반드시 FlatLaf.setup() 이전에 설정해야 함.
            //
            // 5. TODO: FlatLaf 업데이트 후 이 설정 없이도 증상이 없으면 삭제.
            //
            // 6. 확인 환경: macOS 27, FlatLaf 3.7.2, JDK 25
            System.setProperty("flatlaf.useRoundedPopupBorder", "false");
        }
        // 포커스 색을 프로그램의 Primary 색으로 맞춘다. setup() 전에 지정해야 이 값을 참조하는 다른 색에도 반영된다.
        String focusColor = String.format("#%06X", UIColors.PRIMARY_BACKGROUND.getRGB() & 0xFFFFFF);
        FlatLaf.setGlobalExtraDefaults(Map.of(
                "Component.focusedBorderColor", focusColor,
                "Component.focusColor", focusColor));
        FlatLightLaf.setup();
        AppIcons.install();

        try {
            // 2. 최초 실행인 경우 DB 초기화 수행
            if (databaseManager.isInitializationRequired()) {
                runDatabaseInitialization(args);
            } else {
                // 지난 실행에서 예약한 복원이 있으면 로그인 전에 수행
                runScheduledRestore(args);
            }

            // 3. 로그인 UI 표시 (스키마는 DB 비밀번호를 알게 되는 로그인 단계에서 올린다)
            showLoginView(args);
        } catch (Exception e) {
            // 최초 실행과 예약 복원은 EDT에서 하므로 그 예외는 InvocationTargetException에 싸여 온다.
            Throwable cause = e instanceof InvocationTargetException && e.getCause() != null ? e.getCause() : e;
            if (cause instanceof InitializationCancelledException) {
                // 사용자가 직접 취소했으므로 알림 없이 끝낸다.
                log.info("Database initialization cancelled. Exiting");
                System.exit(0);
            }
            log.error("Failed to start application", cause);
            showErrorDialog("애플리케이션 실행 중 오류가 발생했습니다.", cause);
            System.exit(1);
        }
    }

    /**
     * 데이터베이스 초기화 프로세스를 수행한다. 초기화가 실패하거나 사용자가 취소한 경우 예외를 던진다.
     */
    private static void runDatabaseInitialization(String[] args)
            throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(() -> {
            DatabaseInitView initView = new DatabaseInitView();
            // 관리자 계정을 만드는 대신 백업에서 복원할 수도 있다.
            initView.setRestoreAction(() -> new DatabaseRestoreView(initView, backupManager, restoreAction(args))
                    .showRestoreDialog(null));
            boolean success = initView.showInitDialog((adminId, adminPassword) -> {
                try {
                    databaseManager.initialize(adminId, adminPassword);
                } catch (SQLException e) {
                    throw new IllegalStateException(e.getMessage(), e);
                }

                // 초기 정보 생성하기 위해서 스프링 부트를 시작하고 작업 완료 후 중지한다. 스키마는 initialize가 이미 만들었다.
                try (ConfigurableApplicationContext context = SpringApplication.run(ChurchStewardApplication.class,
                        args)) {
                    // 관리자 계정 생성
                    UserService userService = context.getBean(UserService.class);
                    userService.createAdmin(adminId, adminPassword, "초기 관리자 계정 생성");

                    // 회계 연도 생성 (이름은 "2026년" 형식, 기간은 올해 1월 1일 ~ 12월 31일이며, 회계 연도 관리에서 바꿀 수 있다)
                    FiscalYearService fiscalYearService = context.getBean(FiscalYearService.class);
                    int year = LocalDate.now().getYear();
                    String fiscalYearName = year + "년";
                    fiscalYearService.createFiscalYear(fiscalYearName, LocalDate.of(year, 1, 1),
                            LocalDate.of(year, 12, 31));
                    log.info("Database initialized. Admin: {}, first fiscal year: {}", adminId, fiscalYearName);
                }
            });

            if (!success) {
                throw new InitializationCancelledException();
            }
        });
    }

    /**
     * 데이터 관리 화면에서 예약한 복원이 있으면 복원 화면을 띄운다. 복원은 데이터베이스 파일을 교체하므로 Spring 컨텍스트를 띄우기 전인
     * 이 시점에 수행한다. 예약은 한 번만 유효하며, 복원을 취소하면 기존 데이터로 로그인 화면이 이어진다.
     */
    private static void runScheduledRestore(String[] args) throws InvocationTargetException, InterruptedException {
        Optional<Path> scheduledFile = backupManager.getScheduledRestore();
        if (scheduledFile.isEmpty()) {
            return;
        }
        log.info("Scheduled restore found: {}", scheduledFile.get());
        backupManager.cancelScheduledRestore();

        SwingUtilities.invokeAndWait(() -> {
            boolean restored = new DatabaseRestoreView(null, backupManager, restoreAction(args))
                    .showRestoreDialog(scheduledFile.get());
            if (!restored) {
                log.info("Scheduled restore cancelled. Continuing with existing data");
            }
        });
    }

    /**
     * 백업 파일로 복원하고 그 사실을 활동 기록에 남기는 동작을 만든다.
     *
     * 복원은 데이터베이스 파일을 교체하므로 Spring 컨텍스트 없이 수행한다. 그 뒤 복원한 데이터베이스로 컨텍스트를 잠깐 띄워 "복원"
     * 기록을 남기고 닫는다(최초 실행 때 관리자 계정을 만드는 방식과 같다). 기록자는 복원 화면에 입력한 계정이다. 복원은 이미 끝났으므로
     * 기록에 실패해도 복원은 성공으로 보고, 실패했다는 것만 알린다. 과거 버전의 백업은 복원 단계에서 이미 현재 스키마로 올라가
     * 있다({@link DatabaseManager#restore}).
     */
    private static DatabaseRestoreView.RestoreAction restoreAction(String[] args) {
        return (file, userId, userPassword) -> {
            BackupManifest manifest = backupManager.inspect(file);
            Optional<Path> preservedDir = backupManager.restore(file, userId, userPassword);

            boolean logged = true;
            try {
                databaseManager.loadDatabasePassword(userId, userPassword);
                try (ConfigurableApplicationContext restoredContext = SpringApplication
                        .run(ChurchStewardApplication.class, args)) {
                    restoredContext.getBean(BackupService.class)
                            .logRestore(userId, file.getFileName().toString(), manifest.createdAt());
                }
            } catch (RuntimeException e) {
                log.error("Failed to log restore", e);
                logged = false;
            }
            return new DatabaseRestoreView.RestoreOutcome(preservedDir, logged);
        };
    }

    /**
     * 로그인 화면을 표시하고 로그인을 처리한다.
     */
    private static void showLoginView(String[] args) {
        SwingUtilities.invokeLater(() -> {
            LoginView loginView = new LoginView(appSettingManager);
            loginView.showLoginDialog((userId, userPassword) -> {
                try {
                    databaseManager.loadDatabasePassword(userId, userPassword);
                    // 새 버전의 프로그램이면 스키마를 올린다. 데이터베이스가 열리기 전이어야 하므로 컨텍스트보다 먼저 한다.
                    databaseManager.migrate();
                    mainContext = SpringApplication.run(ChurchStewardApplication.class, args);

                    UserService userService = mainContext.getBean(UserService.class);
                    userService.login(userId, userPassword);

                    // 로그인 성공 시 메인 화면 표시
                    SwingUtilities.invokeLater(() -> {
                        MainView mainView = mainContext.getBean(MainView.class);
                        mainView.showMainView();
                    });
                } catch (RuntimeException e) {
                    if (mainContext != null) {
                        mainContext.close();
                        mainContext = null;
                    }

                    throw e;
                } catch (SQLException | IOException e) {
                    if (mainContext != null) {
                        mainContext.close();
                        mainContext = null;
                    }

                    throw new IllegalStateException(e.getMessage(), e);
                }
            });
        });
    }

    /**
     * 사용자에게 에러 메시지를 알림창으로 표시하고, 사용자가 닫을 때까지 기다린다. 호출한 쪽이 곧바로 프로그램을 끝내므로, 기다리지 않으면
     * 알림창이 뜨기 전에 끝날 수 있다.
     */
    private static void showErrorDialog(String message, Throwable cause) {
        String detail = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        String errorMessage = String.format("%s\n\n상세 내용: %s\n\n자세한 내용은 아래 파일을 확인해주세요.\n%s", message,
                detail, AppHome.logFile());
        try {
            SwingUtilities.invokeAndWait(() -> {
                JOptionPane.showMessageDialog(null, errorMessage, "오류 발생", JOptionPane.ERROR_MESSAGE);
            });
        } catch (InvocationTargetException | InterruptedException e) {
            log.error("Failed to show error dialog", e);
        }
    }

    @Bean
    DatabaseManager databaseManager() {
        return databaseManager;
    }

    @Bean
    BackupManager backupManager() {
        return backupManager;
    }

    @Bean
    DataSource dataSource() {
        return databaseManager.createDataSource();
    }
}
