package org.church.steward.infrastructure.backup;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.church.steward.infrastructure.config.AppSettingManager;
import org.church.steward.infrastructure.config.DatabaseManager;
import org.church.steward.infrastructure.config.EncryptedUserKey;
import org.church.steward.infrastructure.security.InvalidPasswordException;
import org.church.steward.infrastructure.security.UserNotFoundException;
import org.church.steward.util.ProgramVersion;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeType;
import tools.jackson.databind.node.ObjectNode;

/**
 * 전체 데이터의 백업 파일을 만들고, 백업 파일로 데이터를 복원한다.
 * <p>
 * 백업 파일은 ZIP이며 두 항목을 담는다.
 * <ul>
 * <li>{@value #MANIFEST_ENTRY}: 백업 정보와 사용자별 DB 접근 키
 * ({@link BackupManifest})</li>
 * <li>{@value #DUMP_ENTRY}: 데이터베이스 전체의 SQL 덤프. DB 파일 비밀번호로 암호화되어 있다.</li>
 * </ul>
 * DB 파일 비밀번호는 사용자별 DB 접근 키 안에 각 사용자의 비밀번호로 암호화되어 있으므로, 백업 파일은 백업한 시점의 계정과 비밀번호가
 * 있어야 풀 수 있다. 별도의 백업 암호는 두지 않는다.
 * <p>
 * 복원은 데이터베이스 파일을 교체하므로 Spring 컨텍스트를 띄우기 전에만 할 수 있다. 실행 중에는 복원을 예약만 하고
 * ({@link #scheduleRestore}), 다음 실행 때 로그인 전에 수행한다. {@link DatabaseManager}처럼
 * Spring 빈이 아니라 애플리케이션 클래스가 직접 만든다.
 */
@Slf4j
public class BackupManager {

    /** 백업 파일의 확장자 (점 제외) */
    public static final String FILE_EXTENSION = "csbackup";

    private static final int FORMAT_VERSION = 1;
    private static final String MANIFEST_ENTRY = "manifest.json";
    private static final String DUMP_ENTRY = "database.sql";
    private static final String TEMP_DIR_PREFIX = "church-steward-backup";
    private static final String SCHEDULED_RESTORE_KEY = "scheduled-restore-file";

    // 백업 정보 JSON의 키
    private static final String JSON_FORMAT_VERSION = "formatVersion";
    private static final String JSON_PROGRAM_VERSION = "programVersion";
    private static final String JSON_CREATED_AT = "createdAt";
    private static final String JSON_USER_KEYS = "userKeys";
    private static final String JSON_USER_ID = "userId";
    private static final String JSON_FILE_PASSWORD = "filePassword";
    private static final String JSON_USER_PASSWORD = "userPassword";
    private static final String JSON_SALT = "salt";

    private static final String INVALID_FILE_MESSAGE = "청지기의 백업 파일이 아니거나 손상된 파일입니다.";

    private final DatabaseManager databaseManager;
    private final AppSettingManager appSettingManager;
    private final String programVersion;
    private final JsonMapper jsonMapper = JsonMapper.builder()
            .build();

    /**
     * @param programVersion 실행 중인 프로그램의 버전. 백업 파일에 기록하고, 복원할 때 백업의 버전과 비교한다.
     */
    public BackupManager(DatabaseManager databaseManager, AppSettingManager appSettingManager,
            String programVersion) {
        this.databaseManager = databaseManager;
        this.appSettingManager = appSettingManager;
        this.programVersion = programVersion;
    }

    /**
     * 전체 데이터를 백업 파일로 저장한다. 같은 이름의 파일이 있으면 덮어쓴다.
     *
     * @param target     저장할 백업 파일
     * @param dataSource 백업할 데이터베이스
     * @param userIds    데이터베이스에 등록된 모든 사용자의 ID. 이 사용자들의 DB 접근 키를 함께 백업한다.
     * @return 백업 파일에 기록한 정보
     * @throws BackupException 백업 파일을 만들지 못했을 때 발생
     */
    public BackupManifest createBackup(Path target, DataSource dataSource, Collection<String> userIds) {
        List<EncryptedUserKey> userKeys = databaseManager.readUserKeys(userIds);
        if (userKeys.isEmpty()) {
            throw new BackupException("설정 파일에 사용자의 DB 접근 키가 없어 백업할 수 없습니다.");
        }
        BackupManifest manifest = new BackupManifest(FORMAT_VERSION, programVersion,
                LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS), userKeys);

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            Path dumpFile = tempDir.resolve(DUMP_ENTRY);
            databaseManager.writeDump(dataSource, dumpFile);
            writeArchive(target, manifest, dumpFile);
        } catch (IOException | SQLException e) {
            throw new BackupException("백업 파일을 만들지 못했습니다: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempDir);
        }
        return manifest;
    }

    /**
     * 백업 파일의 정보를 읽고, 이 프로그램에서 복원할 수 있는 파일인지 확인한다.
     *
     * @throws BackupException 백업 파일이 아니거나 손상되었을 때, 더 새로운 버전의 프로그램에서 만든 백업일 때 발생
     */
    public BackupManifest inspect(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new BackupException("백업 파일을 찾을 수 없습니다: " + file);
        }

        BackupManifest manifest;
        try (ZipFile zipFile = new ZipFile(file.toFile())) {
            ZipEntry manifestEntry = zipFile.getEntry(MANIFEST_ENTRY);
            if (manifestEntry == null || zipFile.getEntry(DUMP_ENTRY) == null) {
                throw new BackupException(INVALID_FILE_MESSAGE);
            }
            try (InputStream in = zipFile.getInputStream(manifestEntry)) {
                manifest = parseManifest(in.readAllBytes());
            }
        } catch (IOException | JacksonException | DateTimeParseException e) {
            throw new BackupException(INVALID_FILE_MESSAGE, e);
        }

        if (manifest.formatVersion() > FORMAT_VERSION
                || ProgramVersion.isNewer(manifest.programVersion(), programVersion)) {
            throw new BackupException("더 새로운 버전(" + manifest.programVersion()
                    + ")의 프로그램에서 만든 백업입니다. 프로그램을 업데이트한 뒤에 복원해주세요.");
        }
        return manifest;
    }

    /**
     * 현재 데이터를 백업 파일의 내용으로 덮어쓴다. Spring 컨텍스트를 띄우기 전에만 호출해야 한다.
     *
     * @param file         복원할 백업 파일
     * @param userId       백업한 시점의 사용자 ID
     * @param userPassword 백업한 시점의 그 사용자의 비밀번호
     * @return 복원 직전의 데이터를 옮겨 둔 디렉터리. 기존 데이터가 없었으면 비어 있음
     * @throws BackupException          백업 파일을 읽지 못했거나 복원하지 못했을 때 발생
     * @throws UserNotFoundException    백업에 없는 사용자일 때 발생
     * @throws InvalidPasswordException 사용자 비밀번호가 잘못되었을 때 발생
     */
    public Optional<Path> restore(Path file, String userId, String userPassword)
            throws UserNotFoundException, InvalidPasswordException {
        BackupManifest manifest = inspect(file);
        log.info("Restoring from backup {} (created at {}, program version {}) as user {}", file.toAbsolutePath(),
                manifest.createdAt(), manifest.programVersion(), userId);

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory(TEMP_DIR_PREFIX);
            Path dumpFile = tempDir.resolve(DUMP_ENTRY);
            try (ZipFile zipFile = new ZipFile(file.toFile());
                    InputStream in = zipFile.getInputStream(zipFile.getEntry(DUMP_ENTRY))) {
                Files.copy(in, dumpFile);
            }
            Optional<Path> preservedDir = databaseManager.restore(dumpFile, manifest.userKeys(), userId,
                    userPassword);
            log.info("Restore completed");
            return preservedDir;
        } catch (IOException | SQLException e) {
            throw new BackupException("백업 파일로 복원하지 못했습니다: " + e.getMessage(), e);
        } finally {
            deleteQuietly(tempDir);
        }
    }

    /**
     * 다음 실행 때 복원할 백업 파일을 설정 파일에 기록한다.
     */
    public void scheduleRestore(Path file) {
        appSettingManager.set(SCHEDULED_RESTORE_KEY, file.toAbsolutePath().toString());
        log.info("Restore scheduled for next start: {}", file.toAbsolutePath());
    }

    /**
     * 복원이 예약된 백업 파일. 예약이 없으면 비어 있다.
     */
    public Optional<Path> getScheduledRestore() {
        String file = appSettingManager.get(SCHEDULED_RESTORE_KEY);
        return file == null || file.isBlank() ? Optional.empty() : Optional.of(Path.of(file));
    }

    public void cancelScheduledRestore() {
        appSettingManager.remove(SCHEDULED_RESTORE_KEY, false);
    }

    /**
     * 임시 파일에 먼저 쓴 뒤에 옮겨서, 쓰다가 실패해도 반쯤 쓰인 백업 파일이 남지 않게 한다.
     */
    private void writeArchive(Path target, BackupManifest manifest, Path dumpFile) throws IOException {
        Path absoluteTarget = target.toAbsolutePath();
        Path tempFile = absoluteTarget.resolveSibling(absoluteTarget.getFileName() + ".tmp");
        try {
            try (OutputStream out = Files.newOutputStream(tempFile);
                    ZipOutputStream zip = new ZipOutputStream(out)) {
                zip.putNextEntry(new ZipEntry(MANIFEST_ENTRY));
                zip.write(toJson(manifest));
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry(DUMP_ENTRY));
                Files.copy(dumpFile, zip);
                zip.closeEntry();
            }
            Files.move(tempFile, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private byte[] toJson(BackupManifest manifest) {
        ObjectNode root = jsonMapper.createObjectNode();
        root.put(JSON_FORMAT_VERSION, manifest.formatVersion());
        root.put(JSON_PROGRAM_VERSION, manifest.programVersion());
        root.put(JSON_CREATED_AT, manifest.createdAt().toString());
        ArrayNode userKeysNode = root.putArray(JSON_USER_KEYS);
        for (EncryptedUserKey userKey : manifest.userKeys()) {
            ObjectNode userKeyNode = userKeysNode.addObject();
            userKeyNode.put(JSON_USER_ID, userKey.userId());
            userKeyNode.put(JSON_FILE_PASSWORD, userKey.filePassword());
            userKeyNode.put(JSON_USER_PASSWORD, userKey.userPassword());
            userKeyNode.put(JSON_SALT, userKey.salt());
        }
        return jsonMapper.writeValueAsBytes(root);
    }

    private BackupManifest parseManifest(byte[] json) {
        JsonNode root = jsonMapper.readTree(json);
        JsonNode formatVersionNode = root.get(JSON_FORMAT_VERSION);
        if (formatVersionNode == null || !formatVersionNode.isInt()) {
            throw new BackupException(INVALID_FILE_MESSAGE);
        }

        List<EncryptedUserKey> userKeys = new ArrayList<>();
        for (JsonNode userKeyNode : root.path(JSON_USER_KEYS)) {
            userKeys.add(new EncryptedUserKey(text(userKeyNode, JSON_USER_ID), text(userKeyNode, JSON_FILE_PASSWORD),
                    text(userKeyNode, JSON_USER_PASSWORD), text(userKeyNode, JSON_SALT)));
        }
        if (userKeys.isEmpty()) {
            throw new BackupException(INVALID_FILE_MESSAGE);
        }

        return new BackupManifest(formatVersionNode.intValue(), text(root, JSON_PROGRAM_VERSION),
                LocalDateTime.parse(text(root, JSON_CREATED_AT)), userKeys);
    }

    private String text(JsonNode node, String name) {
        JsonNode value = node.get(name);
        if (value == null || value.getNodeType() != JsonNodeType.STRING) {
            throw new BackupException(INVALID_FILE_MESSAGE);
        }
        return value.asString();
    }

    private void deleteQuietly(Path tempDir) {
        if (tempDir == null) {
            return;
        }
        try (Stream<Path> files = Files.list(tempDir)) {
            for (Path file : files.toList()) {
                Files.deleteIfExists(file);
            }
            Files.deleteIfExists(tempDir);
        } catch (IOException e) {
            // 임시 파일을 지우지 못해도 백업과 복원의 결과에는 영향이 없다.
        }
    }

}
