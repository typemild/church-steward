package org.church.steward.domain.datatransfer.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import org.church.steward.domain.budget.entity.Budget;
import org.church.steward.domain.budget.entity.BudgetConstraints;
import org.church.steward.domain.budget.entity.BudgetType;
import org.church.steward.domain.datatransfer.dto.BudgetImportAction;
import org.church.steward.domain.datatransfer.dto.BudgetImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportItemDto;
import org.church.steward.domain.datatransfer.dto.BudgetImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;

/**
 * 예산 가져오기의 분석기. 파일의 행들을 예산의 계층으로 읽고, 이미 있는 예산과 견주어 건마다 무엇을 할지 정한다. 데이터베이스를 직접
 * 읽지 않고 필요한 것을 인자로 받으므로, 같은 입력에는 항상 같은 결과를 낸다.
 * <p>
 * 분석은 세 단계다.
 * <ol>
 * <li>행 읽기: 행마다 예산명(단계별 열 또는 전체 경로 열), 구분, 금액을 읽는다. 읽지 못한 행은 오류인 건이 된다.</li>
 * <li>계층 만들기: 구분과 전체 경로가 같으면 같은 예산이다. 하위 예산의 경로에만 나오는 상위 예산은 행이 없어도 만든다.</li>
 * <li>이미 있는 예산과 견주기: 없는 예산은 추가, 있는 예산은 건너뛰거나 금액을 갱신한다. 오류는 하위 예산으로 번진다.</li>
 * </ol>
 */
final class BudgetImportAnalyzer {

    /** 전체 경로 열에서 단계를 나누는 문자 */
    private static final String PATH_SEPARATOR = ">";
    private static final String KEY_PATH_SEPARATOR = " > ";

    private final BudgetImportRequestDto request;
    private final Map<String, Budget> existingBudgets = new HashMap<>();
    private final Predicate<Long> hasRecords;

    /** 분석한 건들. 예산으로 읽은 건과 읽지 못한 행이 파일에 나온 순서대로 섞여 있다. */
    private final List<Node> nodes = new ArrayList<>();
    private final Map<String, Node> budgetNodes = new HashMap<>();
    private int blankRowCount;

    /**
     * 분석한 한 건. 예산으로 읽지 못한 행은 계층에 넣지 않고 오류 상태로만 둔다.
     */
    private static final class Node {
        private final String key;
        private final Node parent;
        private final BudgetType type;
        private final List<String> levelNames;
        private final List<Node> children = new ArrayList<>();
        private Integer rowNumber;
        private Long fileAmount;
        private Budget existing;
        private ImportItemStatus status;
        private BudgetImportAction action = BudgetImportAction.NONE;
        private String message = "";
        /** 예산으로 읽지 못한 행인지 여부 */
        private boolean unreadable;

        private Node(String key, Node parent, BudgetType type, List<String> levelNames) {
            this.key = key;
            this.parent = parent;
            this.type = type;
            this.levelNames = levelNames;
        }

        private boolean isLeaf() {
            return children.isEmpty();
        }

        /**
         * 저장할 금액. 하위 예산이 있으면 말단 예산들의 금액을 모두 더한 값이다.
         */
        private long amount() {
            if (isLeaf()) {
                return fileAmount == null ? 0L : fileAmount;
            }
            long total = 0;
            for (Node child : children) {
                total += child.amount();
            }
            return total;
        }
    }

    /**
     * 행을 예산으로 읽을 수 없을 때 던진다. 메시지가 그 행의 오류 사유가 된다.
     */
    private static final class UnreadableRowException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private UnreadableRowException(String message) {
            super(message, null, false, false);
        }
    }

    private BudgetImportAnalyzer(BudgetImportRequestDto request, List<Budget> existingBudgets,
            Predicate<Long> hasRecords) {
        this.request = request;
        this.hasRecords = hasRecords;
        for (Budget budget : existingBudgets) {
            this.existingBudgets.put(key(budget.getType(), levelNamesOf(budget)), budget);
        }
    }

    /**
     * 파일의 행들을 분석한다.
     *
     * @param request         파일에서 읽은 행과 열 지정
     * @param existingBudgets 가져올 회계 연도에 이미 있는 예산 전체
     * @param hasRecords      예산 ID를 받아 그 예산에 수입/지출 내역이 있는지 알려주는 함수
     * @throws IllegalArgumentException 열 지정이 모자랄 때 발생
     */
    static BudgetImportAnalysisDto analyze(BudgetImportRequestDto request, List<Budget> existingBudgets,
            Predicate<Long> hasRecords) {
        validate(request);

        BudgetImportAnalyzer analyzer = new BudgetImportAnalyzer(request, existingBudgets, hasRecords);
        analyzer.readRows();
        for (Node node : analyzer.nodes) {
            if (!node.unreadable) {
                analyzer.evaluate(node);
            }
        }

        List<BudgetImportItemDto> items = analyzer.nodes.stream()
                .map(BudgetImportAnalyzer::toDto)
                .toList();
        return BudgetImportAnalysisDto.builder()
                .items(items)
                .blankRowCount(analyzer.blankRowCount)
                .signature(sign(items))
                .build();
    }

    private static void validate(BudgetImportRequestDto request) {
        if (request.getPathColumn() == null && request.getLevelColumns().isEmpty()) {
            throw new IllegalArgumentException("예산명이 있는 열을 지정해주세요.");
        }
        if (request.getFixedType() == null && request.getTypeColumn() == null) {
            throw new IllegalArgumentException("구분을 지정해주세요.");
        }
        if (request.getAmountColumn() == null) {
            throw new IllegalArgumentException("금액이 있는 열을 지정해주세요.");
        }
    }

    // ------------------------------------------------------------------
    // 1. 행 읽기

    private void readRows() {
        List<String> previousLevelNames = List.of();
        BudgetType previousType = null;

        for (int i = 0; i < request.getRows().size(); i++) {
            List<Object> row = request.getRows().get(i);
            int rowNumber = request.getFirstRowNumber() + i;

            // 읽지 못한 행도 어느 행인지 알아볼 수 있도록, 읽은 데까지를 오류인 건에 담는다.
            BudgetType type = null;
            List<String> levelNames = List.of();
            try {
                levelNames = readLevelNames(row, previousLevelNames);
                if (levelNames == null) {
                    blankRowCount++;
                    continue;
                }
                previousLevelNames = levelNames;
                type = readType(row, previousType);
                previousType = type;
                validateNames(levelNames);
                place(rowNumber, type, levelNames, readAmount(row));
            } catch (UnreadableRowException e) {
                Node node = new Node("row:" + rowNumber, null, type, levelNames == null ? List.of() : levelNames);
                node.rowNumber = rowNumber;
                node.unreadable = true;
                node.status = ImportItemStatus.ERROR;
                node.message = e.getMessage();
                nodes.add(node);
            }
        }
    }

    /**
     * 행의 예산명을 최상위 단계부터 읽는다.
     *
     * @param previousLevelNames 바로 앞에서 읽은 행의 예산명. 빈 칸이 윗 행의 값을 이어받을 때 쓴다.
     * @return 예산명 목록. 예산명이 하나도 없는 행(빈 줄 등)이면 null
     */
    private List<String> readLevelNames(List<Object> row, List<String> previousLevelNames) {
        if (request.getPathColumn() != null) {
            return readPath(text(row, request.getPathColumn()));
        }

        List<String> names = request.getLevelColumns()
                .stream()
                .map(column -> text(row, column))
                .toList();
        int deepest = names.size() - 1;
        while (deepest >= 0 && names.get(deepest).isEmpty()) {
            deepest--;
        }
        if (deepest < 0) {
            return null;
        }

        // 빈 칸은 그 위 단계들이 윗 행과 같을 때만 윗 행의 값을 이어받는다. 위 단계가 바뀌었으면 다른 예산의 하위이므로
        // 이어받지 않는다.
        List<String> levelNames = new ArrayList<>();
        boolean sameAsPrevious = true;
        for (int level = 0; level <= deepest; level++) {
            String name = names.get(level);
            boolean hasPrevious = level < previousLevelNames.size();
            if (!name.isEmpty()) {
                sameAsPrevious = sameAsPrevious && hasPrevious && name.equals(previousLevelNames.get(level));
                levelNames.add(name);
            } else if (request.isFillDown() && sameAsPrevious && hasPrevious) {
                levelNames.add(previousLevelNames.get(level));
            } else {
                throw new UnreadableRowException((level + 1) + "단계의 예산명이 비어 있습니다.");
            }
        }
        return levelNames;
    }

    private List<String> readPath(String path) {
        if (path.isEmpty()) {
            return null;
        }
        List<String> levelNames = new ArrayList<>();
        for (String name : path.split(PATH_SEPARATOR, -1)) {
            if (name.isBlank()) {
                throw new UnreadableRowException("예산 경로에 빈 이름이 있습니다: " + path);
            }
            levelNames.add(name.strip());
        }
        return levelNames;
    }

    private BudgetType readType(List<Object> row, BudgetType previousType) {
        if (request.getFixedType() != null) {
            return request.getFixedType();
        }

        String text = text(row, request.getTypeColumn());
        if (text.isEmpty()) {
            if (request.isFillDown() && previousType != null) {
                return previousType;
            }
            throw new UnreadableRowException("구분이 비어 있습니다.");
        }
        for (BudgetType type : BudgetType.values()) {
            if (type.getLabel().equals(text)) {
                return type;
            }
        }
        throw new UnreadableRowException("구분을 알 수 없습니다: " + text);
    }

    private void validateNames(List<String> levelNames) {
        for (String name : levelNames) {
            if (name.length() > BudgetConstraints.NAME_MAX_LENGTH) {
                throw new UnreadableRowException("예산명이 " + BudgetConstraints.NAME_MAX_LENGTH + "자를 넘습니다: " + name);
            }
            if (name.contains("<") || name.contains(">")) {
                throw new UnreadableRowException("예산명에는 '<', '>' 문자를 사용할 수 없습니다: " + name);
            }
        }
    }

    /**
     * 금액을 읽는다. 숫자 셀과 "1,000,000원" 같은 글자를 모두 받는다.
     *
     * @return 금액. 칸이 비어 있으면 null
     */
    private Long readAmount(List<Object> row) {
        try {
            return ImportCells.amount(ImportCells.cell(row, request.getAmountColumn()));
        } catch (IllegalArgumentException e) {
            throw new UnreadableRowException(e.getMessage());
        }
    }

    private String text(List<Object> row, Integer column) {
        return ImportCells.text(row, column);
    }

    // ------------------------------------------------------------------
    // 2. 계층 만들기

    /**
     * 행을 계층에 놓는다. 경로에 나오는 상위 예산이 아직 없으면 행 없이 만든다.
     */
    private void place(int rowNumber, BudgetType type, List<String> levelNames, Long amount) {
        Node parent = null;
        for (int depth = 1; depth <= levelNames.size(); depth++) {
            List<String> path = List.copyOf(levelNames.subList(0, depth));
            String key = key(type, path);
            boolean isRowTarget = depth == levelNames.size();

            Node node = budgetNodes.get(key);
            if (node == null) {
                node = new Node(key, parent, type, path);
                budgetNodes.put(key, node);
                nodes.add(node);
                if (parent != null) {
                    parent.children.add(node);
                }
            } else if (isRowTarget && node.rowNumber != null) {
                throw new UnreadableRowException("파일 안에서 같은 예산이 두 번 나옵니다. (" + node.rowNumber + "행)");
            }
            if (isRowTarget) {
                node.rowNumber = rowNumber;
                node.fileAmount = amount;
            }
            parent = node;
        }
    }

    private static String key(BudgetType type, List<String> levelNames) {
        return type.name() + ":" + String.join(KEY_PATH_SEPARATOR, levelNames);
    }

    private static List<String> levelNamesOf(Budget budget) {
        List<String> names = new ArrayList<>();
        Budget current = budget;
        while (current != null) {
            names.add(current.getName());
            current = current.getParent();
        }
        Collections.reverse(names);
        return names;
    }

    // ------------------------------------------------------------------
    // 3. 이미 있는 예산과 비교하기

    /**
     * 건의 상태와 할 일을 정한다. 상위 예산을 먼저 정해야 하므로 반드시 파일에 나온 순서대로 호출한다.
     */
    private void evaluate(Node node) {
        node.existing = existingBudgets.get(node.key);

        if (node.parent != null && node.parent.status == ImportItemStatus.ERROR) {
            setState(node, ImportItemStatus.ERROR, BudgetImportAction.NONE, "상위 예산에 오류가 있어 가져올 수 없습니다.");
        } else if (node.existing == null) {
            evaluateNew(node);
        } else {
            evaluateExisting(node);
        }
    }

    private void evaluateNew(Node node) {
        if (!node.isLeaf() && node.fileAmount != null && node.fileAmount != node.amount()) {
            setState(node, ImportItemStatus.WARNING, BudgetImportAction.CREATE,
                    String.format("파일에 적힌 금액(%s)이 하위 예산의 합계(%s)와 다릅니다. 하위 예산의 합계로 저장합니다.",
                            won(node.fileAmount), won(node.amount())));
        } else {
            setState(node, ImportItemStatus.OK, BudgetImportAction.CREATE, "");
        }
    }

    private void evaluateExisting(Node node) {
        boolean existingIsLeaf = !node.existing.hasChildren();
        long existingAmount = node.existing.getAmount() == null ? 0L : node.existing.getAmount();

        if (node.isLeaf() && existingIsLeaf) {
            if (node.amount() == existingAmount) {
                setState(node, ImportItemStatus.SKIPPED, BudgetImportAction.NONE, "이미 있는 예산입니다.");
            } else if (request.isUpdateExistingAmounts()) {
                setState(node, ImportItemStatus.OK, BudgetImportAction.UPDATE_AMOUNT,
                        "이미 있는 예산의 금액을 갱신합니다. (기존 " + won(existingAmount) + ")");
            } else {
                setState(node, ImportItemStatus.SKIPPED, BudgetImportAction.NONE,
                        "이미 있는 예산입니다. 금액이 파일과 다릅니다. (기존 " + won(existingAmount) + ")");
            }
        } else if (node.isLeaf()) {
            setState(node, ImportItemStatus.SKIPPED, BudgetImportAction.NONE,
                    "이미 있는 예산입니다. 하위 예산이 있어 금액은 바꾸지 않습니다.");
        } else if (!existingIsLeaf) {
            setState(node, ImportItemStatus.SKIPPED, BudgetImportAction.NONE, "이미 있는 예산입니다.");
        } else if (hasRecords.test(node.existing.getId())) {
            setState(node, ImportItemStatus.ERROR, BudgetImportAction.NONE,
                    "수입/지출 내역이 있는 예산이라 하위 예산을 추가할 수 없습니다.");
        } else {
            setState(node, ImportItemStatus.WARNING, BudgetImportAction.NONE,
                    "이미 있는 예산입니다. 하위 예산을 추가하면 이 예산의 금액(" + won(existingAmount) + ")은 하위 예산의 합계로 바뀝니다.");
        }
    }

    private void setState(Node node, ImportItemStatus status, BudgetImportAction action, String message) {
        node.status = status;
        node.action = action;
        node.message = message;
    }

    private static String won(long amount) {
        return String.format("%,d원", amount);
    }

    // ------------------------------------------------------------------
    // 결과

    private static BudgetImportItemDto toDto(Node node) {
        return BudgetImportItemDto.builder()
                .key(node.key)
                .parentKey(node.parent == null ? null : node.parent.key)
                .rowNumber(node.rowNumber)
                .type(node.type)
                .levelNames(node.levelNames)
                .amount(node.unreadable ? null : node.amount())
                .leaf(node.isLeaf())
                .existingBudgetId(node.existing == null ? null : node.existing.getId())
                .status(node.status)
                .action(node.action)
                .message(node.message)
                .build();
    }

    /**
     * 분석 결과 전체를 요약한다. 건의 순서, 상태, 할 일, 금액, 설명 중 하나라도 다르면 값이 달라진다.
     */
    private static String sign(List<BudgetImportItemDto> items) {
        StringBuilder text = new StringBuilder();
        for (BudgetImportItemDto item : items) {
            text.append(item.getKey())
                    .append('|')
                    .append(item.getStatus())
                    .append('|')
                    .append(item.getAction())
                    .append('|')
                    .append(Objects.toString(item.getAmount(), ""))
                    .append('|')
                    .append(item.getMessage())
                    .append('\n');
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

}
