package org.church.steward.domain.datatransfer.service;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import org.church.steward.infrastructure.spreadsheet.SpreadsheetData;

/**
 * 가져올 엑셀(xlsx, xls) 또는 CSV 파일을 읽는다. 데이터베이스는 다루지 않는다.
 */
public interface SpreadsheetFileService {

    /**
     * 엑셀 파일인지 여부. 지원하지 않는 형식이면 예외가 발생한다.
     */
    boolean isExcel(Path file);

    /**
     * 엑셀 파일의 시트 이름을 순서대로 읽는다. CSV 파일이면 빈 목록이다.
     *
     * @param password 파일의 암호. 암호가 없는 파일이면 null
     */
    List<String> listSheets(Path file, String password);

    /**
     * 파일을 표로 읽는다.
     *
     * @param password  엑셀 파일의 암호. 암호가 없는 파일이거나 CSV이면 null
     * @param sheetName 읽을 시트의 이름. null이면 첫 번째 시트
     * @param charset   CSV의 문자 인코딩. null이면 자동으로 판별
     */
    SpreadsheetData read(Path file, String password, String sheetName, Charset charset);
}
