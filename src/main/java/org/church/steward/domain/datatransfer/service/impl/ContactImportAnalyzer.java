package org.church.steward.domain.datatransfer.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.church.steward.domain.contact.entity.Contact;
import org.church.steward.domain.contact.entity.ContactConstraints;
import org.church.steward.domain.contact.entity.ContactType;
import org.church.steward.domain.datatransfer.dto.ContactImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.ContactImportItemDto;
import org.church.steward.domain.datatransfer.dto.ContactImportRequestDto;
import org.church.steward.domain.datatransfer.dto.ImportItemStatus;

/**
 * 관계자 가져오기의 분석기. 파일의 행들을 관계자로 읽고 행마다 가져올 수 있는지를 정한다. 데이터베이스를 직접 읽지 않고 등록된 관계자를
 * 인자로 받는다.
 * <ul>
 * <li>이름이 빈 행은 건너뛴다(빈 줄 등).</li>
 * <li>이름이 100자를 넘거나, 구분이나 상태를 읽을 수 없는 행은 오류다. 비고는 100자를 넘으면 잘라서 저장하고 경고로
 * 알린다.</li>
 * <li>구분, 이름, 비고가 모두 같은 관계자가 이미 있거나 파일의 앞쪽 행에 있으면 중복 의심이다. 이미 있는 관계자와는 건수로
 * 비교한다(같은 관계자가 이미 1명 있고 파일에 2행 있으면 앞의 1행만 이미 있는 관계자와 겹친다).</li>
 * <li>중복 의심이 아니고 이름만 같은 관계자가 있으면 동명이인일 수 있다고 경고만 한다.</li>
 * </ul>
 */
final class ContactImportAnalyzer {

    /** 상태 칸에서 숨김으로 읽는 값 (대문자로 비교) */
    private static final Set<String> HIDDEN_VALUES = Set.of("숨김", "Y", "YES", "예", "O", "TRUE", "1");
    /** 상태 칸에서 표시로 읽는 값 (대문자로 비교). 빈 칸도 표시다. */
    private static final Set<String> VISIBLE_VALUES = Set.of("", "표시", "N", "NO", "아니오", "X", "FALSE", "0");

    private static final String TYPE_CHOICES = Arrays.stream(ContactType.values())
            .map(ContactType::getLabel)
            .collect(Collectors.joining(", "));

    private ContactImportAnalyzer() {
        // 인스턴스화 방지
    }

    /**
     * 읽고 있는 파일의 한 행.
     */
    private static final class Row {
        private final int rowNumber;
        private final String name;
        private ContactType type;
        private String remarks = "";
        private boolean hidden;
        /** 가져올 수 없는 사유. 가져올 수 있으면 null */
        private String error;
        private final List<String> warnings = new ArrayList<>();
        /** 중복 의심의 사유. 중복 의심이 아니면 null */
        private String duplicateReason;

        private Row(int rowNumber, String name) {
            this.rowNumber = rowNumber;
            this.name = name;
        }

        /**
         * 가져올 수 없는 사유를 적는다. 사유가 여럿이면 처음 것만 남긴다.
         */
        private void fail(String message) {
            if (error == null) {
                error = message;
            }
        }
    }

    /**
     * 파일의 행들을 분석한다.
     *
     * @param request  파일에서 읽은 행과 열 지정
     * @param contacts 등록된 관계자 전체 (숨김 포함). 중복과 동명이인을 가리는 데 쓴다.
     * @throws IllegalArgumentException 열 지정이 모자랄 때 발생
     */
    static ContactImportAnalysisDto analyze(ContactImportRequestDto request, List<Contact> contacts) {
        if (request.getFixedType() == null && request.getTypeColumn() == null) {
            throw new IllegalArgumentException("구분을 지정해주세요.");
        }
        if (request.getNameColumn() == null) {
            throw new IllegalArgumentException("이름이 있는 열을 지정해주세요.");
        }

        Map<String, Integer> existingCounts = new HashMap<>();
        Set<String> existingNames = new HashSet<>();
        for (Contact contact : contacts) {
            existingCounts.merge(identityOf(contact.getType(), contact.getName(), contact.getRemarks()), 1,
                    Integer::sum);
            existingNames.add(contact.getName());
        }

        List<Row> rows = new ArrayList<>();
        int blankRowCount = 0;
        for (int i = 0; i < request.getRows().size(); i++) {
            List<Object> values = request.getRows().get(i);
            String name = ImportCells.text(values, request.getNameColumn());
            if (name.isEmpty()) {
                blankRowCount++;
                continue;
            }
            rows.add(readRow(request, request.getFirstRowNumber() + i, name, values));
        }

        Set<String> fileIdentities = new HashSet<>();
        Set<String> fileNames = new HashSet<>();
        for (Row row : rows) {
            if (row.error != null) {
                continue;
            }
            String identity = identityOf(row.type, row.name, row.remarks);
            int remaining = existingCounts.getOrDefault(identity, 0);
            if (remaining > 0) {
                existingCounts.put(identity, remaining - 1);
                row.duplicateReason = "같은 관계자가 이미 있습니다. (중복 의심)";
            } else if (fileIdentities.contains(identity)) {
                row.duplicateReason = "파일의 앞쪽 행에 같은 관계자가 있습니다. (중복 의심)";
            } else if (existingNames.contains(row.name)) {
                row.warnings.add("이름이 같은 관계자가 이미 있습니다. (동명이인)");
            } else if (fileNames.contains(row.name)) {
                row.warnings.add("파일의 앞쪽 행에 이름이 같은 관계자가 있습니다. (동명이인)");
            }
            fileIdentities.add(identity);
            fileNames.add(row.name);
        }

        List<ContactImportItemDto> items = rows.stream()
                .map(ContactImportAnalyzer::toDto)
                .toList();
        return ContactImportAnalysisDto.builder()
                .items(items)
                .blankRowCount(blankRowCount)
                .signature(sign(items))
                .build();
    }

    private static Row readRow(ContactImportRequestDto request, int rowNumber, String name, List<Object> values) {
        Row row = new Row(rowNumber, name);
        if (name.length() > ContactConstraints.NAME_MAX_LENGTH) {
            row.fail("이름이 " + ContactConstraints.NAME_MAX_LENGTH + "자를 넘습니다.");
        }

        if (request.getFixedType() != null) {
            row.type = request.getFixedType();
        } else {
            String typeText = ImportCells.text(values, request.getTypeColumn());
            row.type = parseType(typeText);
            if (typeText.isEmpty()) {
                row.fail("구분이 비어 있습니다.");
            } else if (row.type == null) {
                row.fail("구분을 읽을 수 없습니다: " + typeText + " (" + TYPE_CHOICES + " 중 하나여야 합니다)");
            }
        }

        String remarks = ImportCells.text(values, request.getRemarksColumn());
        if (remarks.length() > ContactConstraints.REMARKS_MAX_LENGTH) {
            row.warnings.add("비고가 " + ContactConstraints.REMARKS_MAX_LENGTH + "자를 넘어 잘립니다.");
            remarks = remarks.substring(0, ContactConstraints.REMARKS_MAX_LENGTH);
        }
        row.remarks = remarks;

        String hiddenText = ImportCells.text(values, request.getHiddenColumn());
        String normalized = hiddenText.toUpperCase();
        if (HIDDEN_VALUES.contains(normalized)) {
            row.hidden = true;
        } else if (!VISIBLE_VALUES.contains(normalized)) {
            row.fail("상태를 읽을 수 없습니다: " + hiddenText + " (숨김 또는 표시여야 합니다)");
        }
        return row;
    }

    /**
     * 구분의 이름("개인", "내부 부서" 등)을 읽는다. 띄어쓰기는 무시한다.
     *
     * @return 구분. 읽을 수 없으면 null
     */
    private static ContactType parseType(String text) {
        String compact = text.replace(" ", "");
        for (ContactType type : ContactType.values()) {
            if (type.getLabel().equals(compact)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 같은 관계자인지 비교할 값. 비고가 없으면 빈 칸과 같다.
     */
    private static String identityOf(ContactType type, String name, String remarks) {
        return type + "|" + name + "|" + Objects.toString(remarks, "");
    }

    private static ContactImportItemDto toDto(Row row) {
        ImportItemStatus status;
        String message;
        if (row.error != null) {
            status = ImportItemStatus.ERROR;
            message = row.error;
        } else if (row.duplicateReason != null || !row.warnings.isEmpty()) {
            List<String> messages = new ArrayList<>();
            if (row.duplicateReason != null) {
                messages.add(row.duplicateReason);
            }
            messages.addAll(row.warnings);
            status = ImportItemStatus.WARNING;
            message = String.join(" ", messages);
        } else {
            status = ImportItemStatus.OK;
            message = "";
        }

        return ContactImportItemDto.builder()
                .key("row:" + row.rowNumber)
                .rowNumber(row.rowNumber)
                .type(row.type)
                .name(row.name)
                .remarks(row.remarks.isEmpty() ? null : row.remarks)
                .hidden(row.hidden)
                .status(status)
                .message(message)
                .duplicate(row.error == null && row.duplicateReason != null)
                .build();
    }

    /**
     * 분석 결과 전체를 요약한다. 행의 상태나 내용 중 하나라도 다르면 값이 달라진다.
     */
    private static String sign(List<ContactImportItemDto> items) {
        String text = items.stream()
                .map(item -> String.join("|", item.getKey(), String.valueOf(item.getStatus()), item.getMessage(),
                        String.valueOf(item.getType()), item.getName(), String.valueOf(item.getRemarks()),
                        String.valueOf(item.isHidden())))
                .collect(Collectors.joining("\n"));
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", e);
        }
    }

}
