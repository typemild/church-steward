package org.church.steward.infrastructure.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.church.steward.util.ProgramVersion;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeId;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 * Spring 컨테이너 없이 application.yml을 직접 조회/수정한다.
 * <p>
 * 대상은 프로그램 폴더({@link AppHome})의 application.yml(사용자별 DB 접근 키, 복원 예약 등 실행 중에
 * 기록하는 설정)이다. jar 안에 포함된 클래스패스의 application.yml(빌드할 때 정해지는 프로그램 정보)과는 다른 파일이며,
 * 프로그램 버전은 {@link ProgramVersion}이 클래스패스 쪽에서 읽는다.
 *
 * <ul>
 * <li>키: "."으로 구분한 평탄키(my.sub.key). 리스트 인덱스 미지원.</li>
 * <li>단일 문서(single document)만 지원.</li>
 * <li>주석과 항목 순서를 유지하며, 신규 키는 형제 노드 중 가장 뒤에 추가.</li>
 * <li>저장은 같은 디렉터리의 임시 파일 + 원자적 교체(ATOMIC_MOVE), Windows 잠금 대비 재시도.</li>
 * <li>공개 메서드는 인스턴스 단위로 동기화(synchronized). 같은 파일은 인스턴스 하나로만 다뤄야 한다.</li>
 * </ul>
 *
 * 본 클래스를 상속해서 일부 기능을 수정하는 경우 동기화가 올바르게 동작하지 않을 수 있으므로 final class로 하였다.
 */
public final class AppSettingManager {

    private static final int MOVE_MAX_ATTEMPTS = 5;
    private static final long MOVE_RETRY_INTERVAL_MS = 100;

    private final Path file;

    /** update() 콜백 실행 중 여부. 모니터를 잡은 스레드만 읽고 쓴다. */
    private boolean updating;

    public AppSettingManager() {
        this(AppHome.dir().resolve("application.yml"));
    }

    public AppSettingManager(Path file) {
        this.file = file.toAbsolutePath().normalize();
    }

    // ------------------------------------------------------------------ 공개 API

    /**
     * 이 객체가 다루는 설정 파일의 절대 경로.
     */
    public Path getFile() {
        return file;
    }

    /**
     * 값을 조회한다.
     *
     * @return 스칼라 값. 키가 없거나 값이 YAML null이면 null.
     * @throws IllegalStateException 값이 맵(또는 리스트)인 경우
     */
    public synchronized String get(String key) {
        parseKey(key);
        checkNotNested();
        MappingNode root = load();
        return root == null ? null : new Editor(root).get(key);
    }

    /**
     * 값을 저장한다.
     *
     * @throws IllegalStateException 경로 중간에 스칼라가 있거나, 대상이 맵(또는 리스트)인 경우
     */
    public void set(String key, String value) {
        update(e -> e.set(key, value));
    }

    /**
     * 키를 삭제한다. 없는 키면 아무것도 하지 않는다. 삭제 후 부모가 비어도 부모는 남긴다.
     *
     * @param recursive 대상이 맵(또는 리스트)일 때 하위 트리까지 삭제할지 여부
     * @throws IllegalStateException 대상이 맵(또는 리스트)인데 recursive가 false인 경우
     */
    public void remove(String key, boolean recursive) {
        update(e -> e.remove(key, recursive));
    }

    /**
     * 하나의 트랜잭션 안에서 여러 설정을 조회/저장/삭제한다. 콜백이 정상 종료되고 변경이 있을 때만 한 번 저장한다. 콜백에서 예외가 나면
     * 저장하지 않는다. 콜백 안에서는 반드시 인자로 받은 Editor를 사용해야 하며, 이 클래스의 get/set/remove/update를
     * 호출하면 예외가 발생한다.
     */
    public synchronized void update(Consumer<Editor> action) {
        Objects.requireNonNull(action, "action");
        checkNotNested();
        MappingNode root = load();
        Editor editor = new Editor(root != null ? root
                : new MappingNode(Tag.MAP, new ArrayList<NodeTuple>(), DumperOptions.FlowStyle.BLOCK));
        updating = true;
        try {
            action.accept(editor);
        } finally {
            updating = false;
            editor.closed = true;
        }
        if (editor.dirty) {
            save(editor.root);
        }
    }

    // ------------------------------------------------------------------ Editor

    /** update() 콜백 안에서만 유효한 편집기. 콜백 밖에서 사용하면 예외가 발생한다. */
    public static final class Editor {
        private final MappingNode root;
        private final Resolver resolver = new Resolver();
        private final Yaml scalarParser = new Yaml(new SafeConstructor(new LoaderOptions()));
        private boolean closed;
        private boolean dirty;

        private Editor(MappingNode root) {
            this.root = root;
        }

        /** 규칙은 {@link YamlConfigStore#get(String)}과 같다. */
        public String get(String key) {
            ensureOpen();
            String[] path = parseKey(key);
            Node node = root;
            for (String seg : path) {
                if (!(node instanceof MappingNode)) {
                    return null; // 경로 중간이 스칼라 → 키 없음
                }
                MappingNode m = (MappingNode) node;
                int idx = indexOf(m, seg);
                if (idx < 0) {
                    return null;
                }
                node = m.getValue().get(idx).getValueNode();
            }
            if (node instanceof ScalarNode) {
                ScalarNode s = (ScalarNode) node;
                return Tag.NULL.equals(s.getTag()) ? null : s.getValue();
            }
            throw new IllegalStateException("스칼라 값이 아닙니다: " + key);
        }

        /** 규칙은 {@link YamlConfigStore#set(String, String)}과 같다. */
        public void set(String key, String value) {
            ensureOpen();
            String[] path = parseKey(key);
            Objects.requireNonNull(value, "value는 null일 수 없습니다. 삭제는 remove()를 사용하세요.");

            MappingNode parent = root;
            for (int i = 0; i < path.length - 1; i++) {
                int idx = indexOf(parent, path[i]);
                if (idx < 0) {
                    MappingNode child = new MappingNode(Tag.MAP, new ArrayList<NodeTuple>(),
                            DumperOptions.FlowStyle.BLOCK);
                    append(parent, path[i], child);
                    parent = child;
                } else {
                    Node v = parent.getValue().get(idx).getValueNode();
                    if (!(v instanceof MappingNode)) {
                        throw new IllegalStateException(
                                "경로 충돌: '" + joinPath(path, i + 1) + "'은(는) 맵이 아닙니다. (키: " + key + ")");
                    }
                    parent = (MappingNode) v;
                }
            }

            String last = path[path.length - 1];
            int idx = indexOf(parent, last);
            if (idx < 0) {
                append(parent, last, newScalar(value, null));
            } else {
                NodeTuple t = parent.getValue().get(idx);
                Node old = t.getValueNode();
                if (!(old instanceof ScalarNode)) {
                    throw new IllegalStateException("스칼라 값이 아닌 항목은 덮어쓸 수 없습니다: " + key);
                }
                parent.getValue().set(idx, new NodeTuple(t.getKeyNode(), newScalar(value, (ScalarNode) old)));
            }
            dirty = true;
        }

        /** 규칙은 {@link YamlConfigStore#remove(String, boolean)}과 같다. */
        public void remove(String key, boolean recursive) {
            ensureOpen();
            String[] path = parseKey(key);
            Node node = root;
            for (int i = 0; i < path.length - 1; i++) {
                if (!(node instanceof MappingNode)) {
                    return;
                }
                MappingNode m = (MappingNode) node;
                int idx = indexOf(m, path[i]);
                if (idx < 0) {
                    return;
                }
                node = m.getValue().get(idx).getValueNode();
            }
            if (!(node instanceof MappingNode)) {
                return;
            }
            MappingNode parent = (MappingNode) node;
            int idx = indexOf(parent, path[path.length - 1]);
            if (idx < 0) {
                return;
            }
            Node target = parent.getValue().get(idx).getValueNode();
            if (!(target instanceof ScalarNode) && !recursive) {
                throw new IllegalStateException("하위 항목이 있는 키입니다. recursive=true로 호출하세요: " + key);
            }
            parent.getValue().remove(idx); // 키 위의 주석도 함께 삭제됨(SnakeYAML 기본 동작)
            dirty = true;
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("Editor는 update() 콜백 안에서만 사용할 수 있습니다.");
            }
        }

        private static void append(MappingNode parent, String key, Node value) {
            // {} 처럼 flow 스타일이던 맵에 자식을 추가하면 한 줄로 기록되므로 block으로 바꾼다.
            parent.setFlowStyle(DumperOptions.FlowStyle.BLOCK);
            ScalarNode keyNode = new ScalarNode(Tag.STR, key, null, null, DumperOptions.ScalarStyle.PLAIN);
            parent.getValue().add(new NodeTuple(keyNode, value));
        }

        /** 기존 노드가 있으면 스타일과 주석을 유지하고, 없으면 따옴표 규칙을 적용한다. */
        private ScalarNode newScalar(String value, ScalarNode old) {
            DumperOptions.ScalarStyle style = old == null ? DumperOptions.ScalarStyle.PLAIN : old.getScalarStyle();
            Tag tag = style == DumperOptions.ScalarStyle.PLAIN ? plainTag(value) : Tag.STR;
            ScalarNode node = new ScalarNode(tag, value, null, null, style);
            if (old != null) {
                node.setBlockComments(old.getBlockComments());
                node.setInLineComments(old.getInLineComments());
                node.setEndComments(old.getEndComments());
            }
            return node;
        }

        /**
         * 따옴표 규칙. int/float/bool로 해석되고 다시 문자열로 바꿨을 때 원래 값과 같으면(8080, true, 1.5) 해당 태그로
         * 두어 따옴표 없이 기록한다. 그 외(010, yes, on, 12:30, null, 빈 문자열 등)는 STR 태그로 두어, 해석이 달라지는
         * 경우 Emitter가 자동으로 따옴표를 붙이게 한다.
         */
        private Tag plainTag(String value) {
            Tag resolved = resolver.resolve(NodeId.scalar, value, true);
            if (Tag.INT.equals(resolved) || Tag.FLOAT.equals(resolved) || Tag.BOOL.equals(resolved)) {
                Object parsed = scalarParser.load(value);
                if (value.equals(String.valueOf(parsed))) {
                    return resolved;
                }
            }
            return Tag.STR;
        }
    }

    // ------------------------------------------------------------------ 파일 I/O

    private MappingNode load() {
        Node node;
        try (Reader reader = new UnicodeReader(Files.newInputStream(file))) { // BOM 자동 처리, 기본 UTF-8
            node = newYaml().compose(reader); // 문서가 2개 이상이면 ComposerException
        } catch (NoSuchFileException e) {
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (node == null) {
            return null; // 빈 파일
        }
        if (!(node instanceof MappingNode)) {
            throw new IllegalStateException("루트가 맵이 아닙니다: " + file);
        }
        return (MappingNode) node;
    }

    private void save(MappingNode root) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile(file.getParent(), file.getFileName() + ".", ".tmp");
            try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.WRITE);
                    Writer w = new BufferedWriter(
                            new OutputStreamWriter(Channels.newOutputStream(ch), StandardCharsets.UTF_8))) {
                if (!root.getValue().isEmpty()) {
                    newYaml().serialize(root, w);
                }
                w.flush();
                ch.force(true);
            }
            moveWithRetry(tmp);
            tmp = null;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignored) {
                    // 임시 파일 삭제 실패는 무시
                }
            }
        }
    }

    /** Windows에서 백신/인덱서 등이 파일을 잠깐 잡고 있으면 교체가 실패하므로 재시도한다. */
    private void moveWithRetry(Path tmp) throws IOException {
        for (int attempt = 1;; attempt++) {
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
                return;
            } catch (FileSystemException e) {
                if (!isRetryable(e) || attempt >= MOVE_MAX_ATTEMPTS) {
                    throw e;
                }
            }
            try {
                Thread.sleep(MOVE_RETRY_INTERVAL_MS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("설정 파일 저장 중 인터럽트되었습니다.");
            }
        }
    }

    /** 접근 거부(ERROR_ACCESS_DENIED)와 공유 위반(ERROR_SHARING_VIOLATION)만 재시도한다. */
    private static boolean isRetryable(FileSystemException e) {
        return e instanceof AccessDeniedException || e.getClass() == FileSystemException.class;
    }

    // ------------------------------------------------------------------ 공통

    /**
     * synchronized는 재진입을 허용하므로, 콜백 안에서 get/set/remove/update를 부르면 그대로 실행된다. 그러면 안쪽
     * 호출이 저장한 내용을 바깥 update()가 덮어쓰므로 직접 막는다.
     */
    private void checkNotNested() {
        if (updating) {
            throw new IllegalStateException("update() 콜백 안에서는 인자로 받은 Editor를 사용하세요.");
        }
    }

    private static Yaml newYaml() {
        LoaderOptions lo = new LoaderOptions();
        lo.setProcessComments(true);

        DumperOptions dop = new DumperOptions();
        dop.setProcessComments(true);
        dop.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dop.setLineBreak(DumperOptions.LineBreak.getPlatformLineBreak());
        dop.setIndent(2);
        dop.setIndicatorIndent(2); // 리스트를 " - item" 형태로 기록
        dop.setIndentWithIndicator(true);
        dop.setSplitLines(false); // 긴 문자열을 여러 줄로 자르지 않음

        return new Yaml(new SafeConstructor(lo), new Representer(dop), dop, lo);
    }

    private static String[] parseKey(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("키가 비어 있습니다.");
        }
        String[] parts = key.split("\\.", -1);
        for (String p : parts) {
            if (p.isEmpty()) {
                throw new IllegalArgumentException("잘못된 키 형식입니다: " + key);
            }
        }
        return parts;
    }

    private static int indexOf(MappingNode m, String seg) {
        List<NodeTuple> tuples = m.getValue();
        for (int i = 0; i < tuples.size(); i++) {
            Node k = tuples.get(i).getKeyNode();
            if (k instanceof ScalarNode && seg.equals(((ScalarNode) k).getValue())) {
                return i;
            }
        }
        return -1;
    }

    private static String joinPath(String[] path, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            if (i > 0) {
                sb.append('.');
            }
            sb.append(path[i]);
        }
        return sb.toString();
    }
}
