package org.church.steward.infrastructure.spreadsheet;

/**
 * 가져오기/내보내기에 쓰는 표 파일의 형식
 */
public enum SpreadsheetFormat {

    XLSX("엑셀", "xlsx", true),
    CSV("CSV", "csv", false);

    private final String label;
    private final String extension;
    private final boolean passwordSupported;

    SpreadsheetFormat(String label, String extension, boolean passwordSupported) {
        this.label = label;
        this.extension = extension;
        this.passwordSupported = passwordSupported;
    }

    public String getLabel() {
        return label;
    }

    /**
     * 파일 확장자 (점 제외)
     */
    public String getExtension() {
        return extension;
    }

    /**
     * 파일에 암호를 걸 수 있는 형식인지 여부
     */
    public boolean isPasswordSupported() {
        return passwordSupported;
    }

}
