package org.church.steward.ui.view.datamanagement.importing;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.domain.fiscalyear.dto.FiscalYearDto;
import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;

import lombok.Getter;
import lombok.Setter;

/**
 * 진행 중인 가져오기 한 번의 상태 중에서 대상(관계자, 예산, 수입/지출)과 무관한 부분: 회계 연도, 읽어 둔 파일, 머리글 행, 받아
 * 둔 백업. 화면 컴포넌트와 무관하게 상태와 규칙만 가진다. 열 지정과 분석 결과는 대상마다 다르므로 하위 클래스가 가진다.
 * <p>
 * 상태는 두 가지로 나뉜다. 다른 메뉴에 다녀와도 이 구분에 따라 이어서 진행한다.
 * <ul>
 * <li>사용자가 정한 것: 읽어 둔 파일 내용, 머리글 행, 열 지정, 직접 한 매핑과 선택. 그대로 유지한다.</li>
 * <li>데이터베이스에서 나온 것: 분석 결과와 그에 따른 선택. 언제든 다시 계산한다.</li>
 * </ul>
 */
@Getter
public abstract class ImportSession {

    /** 가져올 회계 연도. 관계자처럼 회계 연도에 속하지 않는 대상이면 null */
    @Setter
    private FiscalYearDto fiscalYear;

    private Path file;
    private String password;
    private boolean excel;
    private List<String> sheetNames = List.of();
    private String sheetName;
    /** CSV를 읽을 인코딩. 자동으로 판별하게 하면 null */
    private Charset charset;
    private SpreadsheetData data;
    /** 머리글이 있는 행 (1부터). 머리글이 없으면 0이며 이때는 첫 행부터 데이터다. */
    private int headerRowNumber = 1;

    /** 반영 전에 받은 백업의 활동 기록 ID. 아직 받지 않았으면 null */
    private Long backupLogId;
    private Path backupFile;

    /**
     * 파일의 내용이나 머리글 행이 바뀌었을 때 호출된다. 하위 클래스는 열 추측과 분석 결과를 버려야 한다.
     */
    protected abstract void onDataChanged();

    /**
     * 분석 결과가 있는지 여부.
     */
    public abstract boolean hasAnalysis();

    /**
     * 분석 결과와 선택, 받아 둔 백업을 버린다. 파일이나 열 지정이 바뀌어 처음부터 다시 분석해야 할 때 쓴다.
     */
    public abstract void clearAnalysis();

    /**
     * 가져오기를 끝내거나 취소했을 때 읽어 둔 파일 내용과 열 지정, 분석 결과를 모두 버린다.
     */
    public abstract void reset();

    /**
     * 읽어 둔 파일과 받아 둔 백업을 버린다. 하위 클래스의 {@link #reset()}에서 호출한다.
     */
    protected void resetFile() {
        fiscalYear = null;
        file = null;
        password = null;
        excel = false;
        sheetNames = List.of();
        sheetName = null;
        charset = null;
        data = null;
        headerRowNumber = 1;
        clearBackup();
    }

    /**
     * 읽은 파일의 내용을 담는다.
     */
    public void setData(Path file, String password, boolean excel, List<String> sheetNames, String sheetName,
            Charset charset, SpreadsheetData data) {
        this.file = file;
        this.password = password;
        this.excel = excel;
        this.sheetNames = sheetNames;
        this.sheetName = sheetName;
        this.charset = charset;
        this.data = data;
        this.headerRowNumber = Math.min(headerRowNumber, data.rows().size());
        onDataChanged();
    }

    public void setHeaderRowNumber(int headerRowNumber) {
        if (this.headerRowNumber == headerRowNumber) {
            return;
        }
        this.headerRowNumber = headerRowNumber;
        onDataChanged();
    }

    public boolean hasData() {
        return data != null;
    }

    public int getColumnCount() {
        return data == null ? 0 : data.columnCount();
    }

    /**
     * 머리글 아래의 데이터 행 수.
     */
    public int getDataRowCount() {
        return data == null ? 0 : Math.max(0, data.rows().size() - headerRowNumber);
    }

    /**
     * 머리글 아래의 데이터 행. 분석과 반영에는 이 행들만 넘긴다.
     */
    protected List<List<Object>> getDataRows() {
        return data.rows().subList(Math.min(headerRowNumber, data.rows().size()), data.rows().size());
    }

    /**
     * 첫 데이터 행이 파일에서 몇 번째 행인지 (1부터).
     */
    protected int getFirstDataRowNumber() {
        return headerRowNumber + 1;
    }

    /**
     * 열을 고르는 목록에 보여줄 이름. 엑셀의 열 문자에 머리글을 덧붙인다 (예: "B: 예산 1단계").
     */
    public String getColumnLabel(int column) {
        String header = getHeader(column);
        return header.isEmpty() ? toColumnLetter(column) : toColumnLetter(column) + ": " + header;
    }

    /**
     * 머리글 행에서 그 열에 적힌 글자. 머리글이 없으면 빈 문자열이다.
     */
    protected String getHeader(int column) {
        if (data == null || headerRowNumber < 1 || headerRowNumber > data.rows().size()) {
            return "";
        }
        Object value = data.rows().get(headerRowNumber - 1).get(column);
        return value == null ? "" : value.toString().strip();
    }

    /**
     * 0부터 세는 열 위치를 엑셀의 열 문자로 바꾼다 (0 -> A, 25 -> Z, 26 -> AA).
     */
    public static String toColumnLetter(int column) {
        StringBuilder letters = new StringBuilder();
        int remaining = column;
        do {
            letters.insert(0, (char) ('A' + remaining % 26));
            remaining = remaining / 26 - 1;
        } while (remaining >= 0);
        return letters.toString();
    }

    public void setBackup(long backupLogId, Path backupFile) {
        this.backupLogId = backupLogId;
        this.backupFile = backupFile;
    }

    public void clearBackup() {
        this.backupLogId = null;
        this.backupFile = null;
    }

    public boolean hasBackup() {
        return backupLogId != null;
    }

}
