package org.church.steward.infrastructure.config;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.sql.DataSource;

import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.springframework.boot.jdbc.DataSourceBuilder;

import lombok.extern.slf4j.Slf4j;

/**
 * 프로그램의 초기 데이터베이스 및 설정 파일 초기화를 담당하는 클래스다.
 */
@Slf4j
public class DatabaseManager {

    /** 이 프로그램의 마이그레이션 위치 */
    public static final String MIGRATION_LOCATION = "classpath:db/migration";

    private final String dbDir;
    private final String dbFilePath;

    private final String dbUrl;
    private final String dbUserName;

    private final AppSettingManager appSettingManager;
    private final List<String> migrationLocations;

    private static final String PBKDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final int ITERATIONS = 700_000;
    private static final int KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String INVALID_PASSWORD_MESSAGE = "비밀번호가 일치하지 않습니다. 다시 확인해 주세요.";

    private static final String DB_FILE_EXTENSION = ".mv.db";
    /** 복원할 때 덤프를 되살리는 임시 데이터베이스의 이름에 붙이는 접미어 */
    private static final String RESTORING_SUFFIX = "_restoring";
    /** 복원 직전의 데이터를 옮겨 두는 디렉터리 이름: "before-restore-20261013-153000" */
    private static final String PRESERVED_DIR_PREFIX = "before-restore-";
    /** 마이그레이션 직전의 데이터베이스 파일을 복사해 두는 디렉터리 이름: "before-migrate-20261013-153000" */
    private static final String PRE_MIGRATION_DIR_PREFIX = "before-migrate-";
    private static final DateTimeFormatter PRESERVED_DIR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private SecureRandom secureRandom; // 사용 시 초기화 됨
    private byte[] dbFilePassword;
    private byte[] dbUserPassword;

    /**
     * 운영 환경을 위한 기본 생성자
     *
     * @param appSettingManager DB 접속 설정을 조회하고 저장하기 위한 설정 관리 객체
     */
    public DatabaseManager(AppSettingManager appSettingManager) {
        this(AppHome.dir().resolve("data").toString(), "church_steward", appSettingManager);
    }

    /**
     * 테스트 환경 등을 위해 경로를 주입받는 생성자
     *
     * @param dbDir             데이터베이스 파일이 저장될 디렉토리
     * @param dbName            데이터베이스 파일 이름 (확장자 제외)
     * @param appSettingManager DB 접속 설정을 조회하고 저장하기 위한 설정 관리 객체
     */
    public DatabaseManager(String dbDir, String dbName, AppSettingManager appSettingManager) {
        this(dbDir, dbName, appSettingManager, List.of(MIGRATION_LOCATION));
    }

    /**
     * 테스트에서 마이그레이션 위치를 바꾸기 위한 생성자. 가짜 마이그레이션을 더해 새 버전의 프로그램을 흉내 낼 때 쓴다.
     *
     * @param dbDir              데이터베이스 파일이 저장될 디렉토리
     * @param dbName             데이터베이스 파일 이름 (확장자 제외)
     * @param appSettingManager  DB 접속 설정을 조회하고 저장하기 위한 설정 관리 객체
     * @param migrationLocations Flyway가 마이그레이션을 찾을 위치 (예: "classpath:db/migration")
     */
    public DatabaseManager(String dbDir, String dbName, AppSettingManager appSettingManager,
            List<String> migrationLocations) {
        this.dbDir = dbDir;
        this.dbFilePath = dbDir + "/" + dbName;
        this.appSettingManager = appSettingManager;
        this.migrationLocations = List.copyOf(migrationLocations);

        this.dbUrl = toUrl(dbFilePath);
        this.dbUserName = "sa";
    }

    public boolean isInitializationRequired() {
        File dbFile = new File(dbFilePath + DB_FILE_EXTENSION);
        return !dbFile.exists();
    }

    public void initialize(String adminId, String adminPassword) throws SQLException {
        dbFilePassword = generateRandomBytes(16);
        dbUserPassword = generateRandomBytes(16);
        byte[] salt = generateRandomBytes(16);
        SecretKeySpec derivedKey = deriveKeyFromUserPassword(adminPassword, salt);

        String encryptedDbFilePassword = encryptWithDerivedKey(dbFilePassword, derivedKey);
        String encryptedDbUserPassword = encryptWithDerivedKey(dbUserPassword, derivedKey);

        saveConfigToYaml(adminId, encryptedDbFilePassword, encryptedDbUserPassword,
                Base64.getEncoder().encodeToString(salt));
        createH2DatabaseFile(Base64.getEncoder().encodeToString(dbFilePassword),
                Base64.getEncoder().encodeToString(dbUserPassword));
        createFlyway(dbUrl, dbFilePassword, dbUserPassword).migrate();
    }

    /**
     * 데이터베이스의 스키마를 이 프로그램의 버전에 맞게 올린다. {@link #loadDatabasePassword} 이후, Spring
     * 컨텍스트를 띄우기 전(데이터베이스가 열려 있지 않을 때)에 호출해야 한다.
     * <p>
     * H2는 DDL을 문장마다 바로 커밋하므로 마이그레이션이 중간에 실패하면 되돌릴 수 없다. 그래서 올릴 마이그레이션이 있으면 먼저
     * 데이터베이스 파일을 데이터 디렉터리 아래의 보관 디렉터리에 복사해 둔다.
     *
     * @return 마이그레이션 직전의 데이터베이스 파일을 복사해 둔 디렉터리. 올릴 마이그레이션이 없었으면 비어 있음
     * @throws SQLException 마이그레이션에 실패했을 때 발생. 메시지에 복사해 둔 디렉터리를 적는다.
     */
    public Optional<Path> migrate() throws SQLException, IOException {
        Flyway flyway = createFlyway(dbUrl, dbFilePassword, dbUserPassword);
        MigrationInfoService info = flyway.info();
        MigrationInfo[] pending = info.pending();
        if (pending.length == 0) {
            return Optional.empty();
        }

        // info()가 쓴 연결은 닫혔고 H2는 마지막 연결이 닫히면 파일을 닫으므로, 이 시점에 파일을 복사해도 된다.
        Path databaseFile = Path.of(dbFilePath + DB_FILE_EXTENSION);
        Path dir = Path.of(dbDir, PRE_MIGRATION_DIR_PREFIX + LocalDateTime.now().format(PRESERVED_DIR_FORMAT));
        Files.createDirectories(dir);
        Files.copy(databaseFile, dir.resolve(databaseFile.getFileName()));

        // 마이그레이션 도중에 프로그램이 끝나도 복사본의 위치가 남도록 시작하기 전에 기록한다.
        log.info("Migrating database schema from version {} to {}. Previous file copied to {}",
                info.current() == null ? "none" : info.current().getVersion(),
                pending[pending.length - 1].getVersion(), dir.toAbsolutePath());
        try {
            flyway.migrate();
        } catch (FlywayException e) {
            throw new SQLException("데이터베이스를 새 버전에 맞게 바꾸지 못했습니다. 바꾸기 전의 데이터베이스 파일은 "
                    + dir.toAbsolutePath() + "에 있습니다: " + e.getMessage(), e);
        }
        log.info("Database schema migrated");
        return Optional.of(dir);
    }

    /**
     * 사용자의 비밀번호를 바탕으로 DB 접속 정보를 복호화한다.
     *
     * @param userId       사용자 ID
     * @param userPassword 사용자 비밀번호
     * @throws UserNotFoundException    사용자 정보가 존재하지 않을 때 발생
     * @throws InvalidPasswordException 사용자 비밀번호가 잘못되었을 때 발생
     */
    public void loadDatabasePassword(String userId, String userPassword)
            throws UserNotFoundException, InvalidPasswordException {
        String prefix = "database-manager." + userId;

        String encryptedDbFilePassword = appSettingManager.get(prefix + ".file-password");
        if (encryptedDbFilePassword == null) {
            throw new UserNotFoundException("사용자 정보가 존재하지 않습니다: " + userId);
        }
        String encryptedDbUserPassword = appSettingManager.get(prefix + ".user-password");
        String saltBase64 = appSettingManager.get(prefix + ".salt");

        byte[] salt = decodeStoredValue(saltBase64);
        SecretKeySpec derivedKey = deriveKeyFromUserPassword(userPassword, salt);
        dbFilePassword = decryptWithDerivedKey(encryptedDbFilePassword, derivedKey);
        dbUserPassword = decryptWithDerivedKey(encryptedDbUserPassword, derivedKey);
    }

    /**
     * 사용자의 비밀번호를 바탕으로 DB 접속 정보를 YAML에 암호화하여 저장한다.
     *
     * @param userId       사용자 ID
     * @param userPassword 새로운 비밀번호
     * @throws UserNotFoundException 사용자 정보가 존재하지 않을 때 발생
     */
    public void saveOrUpdateUserPassword(String userId, String userPassword) throws UserNotFoundException {
        String prefix = "database-manager." + userId;

        byte[] salt = generateRandomBytes(16);
        SecretKeySpec newKey = deriveKeyFromUserPassword(userPassword, salt);

        String newEncryptedDbFilePassword = encryptWithDerivedKey(dbFilePassword, newKey);
        String newEncryptedDbUserPassword = encryptWithDerivedKey(dbUserPassword, newKey);

        appSettingManager.update(editor -> {
            editor.set(prefix + ".file-password", newEncryptedDbFilePassword);
            editor.set(prefix + ".user-password", newEncryptedDbUserPassword);
            editor.set(prefix + ".salt", Base64.getEncoder().encodeToString(salt));
        });
    }

    /**
     * 설정 파일에서 특정 사용자의 DB 접근 키 관련 정보를 삭제한다.
     *
     * @param userId 삭제할 사용자 ID
     * @throws UserNotFoundException 사용자 정보가 존재하지 않을 때 발생
     */
    public void removeUserPassword(String userId) throws UserNotFoundException {
        String prefix = "database-manager." + userId;

        // 존재 여부 확인
        if (appSettingManager.get(prefix + ".salt") == null) {
            throw new UserNotFoundException("삭제할 사용자 정보가 존재하지 않습니다: " + userId);
        }

        appSettingManager.remove(prefix, true);
    }

    /**
     * 설정 파일에 저장된 사용자들의 암호화된 DB 접근 키를 읽는다. 설정 파일에 키가 없는 사용자는 건너뛴다.
     *
     * @param userIds 키를 읽을 사용자 ID 목록
     */
    public List<EncryptedUserKey> readUserKeys(Collection<String> userIds) {
        List<EncryptedUserKey> userKeys = new ArrayList<>();
        for (String userId : userIds) {
            String prefix = "database-manager." + userId;
            String filePassword = appSettingManager.get(prefix + ".file-password");
            if (filePassword == null) {
                continue;
            }
            userKeys.add(new EncryptedUserKey(userId, filePassword, appSettingManager.get(prefix + ".user-password"),
                    appSettingManager.get(prefix + ".salt")));
        }
        return userKeys;
    }

    /**
     * 데이터베이스 전체를 SQL 덤프로 내보낸다. 덤프는 압축하고 DB 파일 비밀번호로 암호화한다.
     * {@link #loadDatabasePassword} 이후에 호출해야 한다.
     *
     * @param dataSource 덤프를 실행할 데이터베이스 연결
     * @param dumpFile   덤프를 저장할 파일
     */
    public void writeDump(DataSource dataSource, Path dumpFile) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            writeDump(connection, dumpFile, dbFilePassword);
        }
    }

    /**
     * 연결된 데이터베이스 전체를 주어진 DB 파일 비밀번호로 암호화한 SQL 덤프로 내보낸다. 테스트가 정해 둔 비밀번호로 덤프를 만들 때도
     * 쓴다(H2 버전 사이의 덤프 호환성 확인).
     */
    void writeDump(Connection connection, Path dumpFile, byte[] filePassword) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SCRIPT TO " + quote(dumpFile.toAbsolutePath().toString()) + dumpOptions(filePassword));
        }
    }

    /**
     * 데이터베이스를 덤프의 내용으로 교체하고, 설정 파일의 사용자별 DB 접근 키도 덤프와 함께 백업된 키로 교체한다. 데이터베이스가 열려 있지
     * 않을 때(Spring 컨텍스트를 띄우기 전)에만 호출해야 한다.
     * <p>
     * 덤프를 새 파일에 먼저 되살리고 스키마를 이 프로그램의 버전에 맞게 올린 뒤에 기존 파일과 바꾸므로, 되살리거나 스키마를 올리다가 실패하면
     * 기존 데이터는 그대로 남는다. 기존 데이터베이스 파일과 설정 파일은 데이터 디렉터리 아래의 보관 디렉터리로 옮겨 둔다.
     *
     * @param dumpFile     {@link #writeDump}로 만든 덤프 파일
     * @param userKeys     덤프와 함께 백업된 사용자별 DB 접근 키
     * @param userId       백업한 시점의 사용자 ID
     * @param userPassword 백업한 시점의 그 사용자의 비밀번호
     * @return 기존 데이터를 옮겨 둔 디렉터리. 기존 데이터베이스가 없었으면 비어 있음
     * @throws UserNotFoundException    백업에 없는 사용자일 때 발생
     * @throws InvalidPasswordException 사용자 비밀번호가 잘못되었을 때 발생
     */
    public Optional<Path> restore(Path dumpFile, List<EncryptedUserKey> userKeys, String userId, String userPassword)
            throws UserNotFoundException, InvalidPasswordException, SQLException, IOException {
        EncryptedUserKey userKey = userKeys.stream()
                .filter(key -> key.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new UserNotFoundException("백업에 없는 사용자입니다: " + userId));

        SecretKeySpec derivedKey = deriveKeyFromUserPassword(userPassword, decodeStoredValue(userKey.salt()));
        byte[] restoredFilePassword = decryptWithDerivedKey(userKey.filePassword(), derivedKey);
        byte[] restoredUserPassword = decryptWithDerivedKey(userKey.userPassword(), derivedKey);

        // 1. 덤프를 새 파일에 되살리고 스키마를 올린다. 실패하면 새 파일만 지우고 끝낸다.
        String restoringFilePath = dbFilePath + RESTORING_SUFFIX;
        Path restoringFile = Path.of(restoringFilePath + DB_FILE_EXTENSION);
        Files.createDirectories(Path.of(dbDir));
        Files.deleteIfExists(restoringFile);
        try {
            runDump(dumpFile, restoringFilePath, restoredFilePassword, restoredUserPassword);
            createFlyway(toUrl(restoringFilePath), restoredFilePassword, restoredUserPassword).migrate();
        } catch (SQLException e) {
            Files.deleteIfExists(restoringFile);
            throw e;
        } catch (FlywayException e) {
            Files.deleteIfExists(restoringFile);
            throw new SQLException("백업한 데이터를 이 버전에 맞게 바꾸지 못했습니다: " + e.getMessage(), e);
        }

        // 2. 기존 데이터베이스 파일과 설정 파일을 보관 디렉터리로 옮긴다.
        Path databaseFile = Path.of(dbFilePath + DB_FILE_EXTENSION);
        Optional<Path> preservedDir = Optional.empty();
        if (Files.exists(databaseFile)) {
            Path dir = Path.of(dbDir, PRESERVED_DIR_PREFIX + LocalDateTime.now().format(PRESERVED_DIR_FORMAT));
            // 파일을 옮기다가 프로그램이 끝나도 기존 데이터의 위치가 남도록 옮기기 전에 기록한다.
            log.info("Moving previous database and setting file to {}", dir.toAbsolutePath());
            Files.createDirectories(dir);
            Files.move(databaseFile, dir.resolve(databaseFile.getFileName()));
            if (Files.exists(appSettingManager.getFile())) {
                Files.copy(appSettingManager.getFile(), dir.resolve(appSettingManager.getFile().getFileName()));
            }
            preservedDir = Optional.of(dir);
        }

        // 3. 되살린 파일을 제자리에 놓고, 설정 파일의 키를 백업된 키로 바꾼다.
        log.info("Replacing database with restored data");
        Files.move(restoringFile, databaseFile);
        appSettingManager.update(editor -> {
            editor.remove("database-manager", true);
            for (EncryptedUserKey key : userKeys) {
                String prefix = "database-manager." + key.userId();
                editor.set(prefix + ".file-password", key.filePassword());
                editor.set(prefix + ".user-password", key.userPassword());
                editor.set(prefix + ".salt", key.salt());
            }
        });

        return preservedDir;
    }

    /**
     * 주어진 경로에 암호화된 데이터베이스를 새로 만들고 덤프를 실행한다. 테스트가 과거 H2 버전이 만든 덤프를 되살릴 때도 쓴다(H2 버전
     * 사이의 덤프 호환성 확인).
     */
    void runDump(Path dumpFile, String databaseFilePath, byte[] filePassword, byte[] userPassword)
            throws SQLException {
        try (Connection connection = DriverManager.getConnection(toUrl(databaseFilePath), dbUserName,
                toConnectionPassword(filePassword, userPassword));
                Statement statement = connection.createStatement()) {
            statement.execute("RUNSCRIPT FROM " + quote(dumpFile.toAbsolutePath().toString())
                    + dumpOptions(filePassword));
        }
    }

    /**
     * SCRIPT와 RUNSCRIPT에 똑같이 붙이는 옵션. 덤프의 암호는 DB 파일 비밀번호다.
     */
    private String dumpOptions(byte[] filePassword) {
        return " COMPRESSION DEFLATE CIPHER AES PASSWORD " + quote(Base64.getEncoder().encodeToString(filePassword))
                + " CHARSET 'UTF-8'";
    }

    private String quote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    /**
     * 스키마의 마이그레이션을 실행할 Flyway를 만든다.
     */
    private Flyway createFlyway(String url, byte[] filePassword, byte[] userPassword) {
        return Flyway.configure()
                .dataSource(url, dbUserName, toConnectionPassword(filePassword, userPassword))
                .locations(migrationLocations.toArray(String[]::new))
                .load();
    }

    private String toUrl(String databaseFilePath) {
        return "jdbc:h2:file:" + databaseFilePath + ";CIPHER=AES";
    }

    /**
     * 암호화된 H2 데이터베이스의 접속 비밀번호. 파일 비밀번호와 사용자 비밀번호를 공백으로 잇는다.
     */
    private String toConnectionPassword(byte[] filePassword, byte[] userPassword) {
        return Base64.getEncoder().encodeToString(filePassword) + " "
                + Base64.getEncoder().encodeToString(userPassword);
    }

    public DataSource createDataSource() {
        return DataSourceBuilder.create()
                .url(dbUrl)
                .username(dbUserName)
                .password(toConnectionPassword(dbFilePassword, dbUserPassword))
                .driverClassName("org.h2.Driver")
                .build();
    }

    private byte[] generateRandomBytes(int byteSize) {
        byte[] bytes = new byte[byteSize];
        getSecureRandom().nextBytes(bytes);
        return bytes;
    }

    private SecureRandom getSecureRandom() {
        if (secureRandom == null) {
            secureRandom = new SecureRandom();
        }
        return secureRandom;
    }

    /**
     * 사용자 비밀번호에서 DB 접근 키를 암호화할 키를 유도한다. 입력이 틀려서 실패하는 일은 없고 실행 환경에 알고리즘이 없을 때만
     * 실패하므로, 진단을 위해 원인 예외를 담는다. JDK의 예외 메시지에는 비밀번호나 키 값이 들어가지 않는다. 저장된 값을 다루는 코드는
     * 메시지에 값이 섞일 수 있으므로 {@code try} 안에 넣지 않는다.
     */
    private SecretKeySpec deriveKeyFromUserPassword(String password, byte[] salt) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("암호키 처리 실패", e);
        }
    }

    /**
     * 유도한 키로 데이터를 암호화한다. 원인 예외를 담는 이유는 {@link #deriveKeyFromUserPassword}와 같다.
     */
    private String encryptWithDerivedKey(byte[] data, SecretKeySpec key) {
        byte[] iv = new byte[GCM_IV_LENGTH];
        getSecureRandom().nextBytes(iv);

        byte[] ciphertext;
        try {
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            ciphertext = cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("데이터 암호화 실패", e);
        }
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 사용자 비밀번호에서 유도한 키로 DB 접근 키를 복호화한다. 보안을 위해 실패 사유(암호문 형식 오류, 인증 태그 불일치 등)를 구분하지
     * 않고 같은 예외 하나로 답하며, 원인 예외도 담지 않는다.
     */
    private byte[] decryptWithDerivedKey(String encryptedData, SecretKeySpec key) throws InvalidPasswordException {
        byte[] decoded = decodeStoredValue(encryptedData);
        if (decoded.length < GCM_IV_LENGTH) {
            throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, GCM_IV_LENGTH);

            int ciphertextLength = decoded.length - GCM_IV_LENGTH;
            byte[] ciphertext = new byte[ciphertextLength];
            System.arraycopy(decoded, GCM_IV_LENGTH, ciphertext, 0, ciphertextLength);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);

            return cipher.doFinal(ciphertext);
        } catch (GeneralSecurityException e) {
            throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
        }
    }

    /**
     * 설정 파일이나 백업에 저장된 Base64 값(salt, 암호문)을 푼다. 값이 없거나 깨져 있으면 비밀번호가 틀렸을 때와 같은 예외 하나로
     * 답하고 원인 예외도 담지 않는다. Base64 오류의 메시지에는 저장된 값의 글자가 들어가기 때문이다.
     */
    private byte[] decodeStoredValue(String value) throws InvalidPasswordException {
        if (value == null) {
            throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
        }
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
        }
        if (decoded.length == 0) {
            throw new InvalidPasswordException(INVALID_PASSWORD_MESSAGE);
        }
        return decoded;
    }

    private void saveConfigToYaml(String userId, String encryptedDbFilePassword, String encryptedDbUserPassword,
            String saltBase64) {
        String prefix = "database-manager." + userId;
        appSettingManager.update(editor -> {
            editor.set(prefix + ".file-password", encryptedDbFilePassword);
            editor.set(prefix + ".user-password", encryptedDbUserPassword);
            editor.set(prefix + ".salt", saltBase64);
        });
    }

    private void createH2DatabaseFile(String dbFilePassword, String dbUserPassword) throws SQLException {
        String dbPassword = dbFilePassword + " " + dbUserPassword;

        // 디렉토리가 없으면 생성
        File dir = new File(dbDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        try (Connection _ = DriverManager.getConnection(dbUrl, dbUserName, dbPassword)) {
            // 연결 성공 시 파일이 물리적으로 생성됨
        }
    }
}
