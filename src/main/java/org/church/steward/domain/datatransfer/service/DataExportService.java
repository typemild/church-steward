package org.church.steward.domain.datatransfer.service;

import java.nio.file.Path;

import org.church.steward.infrastructure.spreadsheet.SpreadsheetFormat;

/**
 * 예산과 수입/지출 내역을 엑셀 또는 CSV 파일로 내보낸다.
 * 내보낸 파일과 양식은 머리글이 같아서, 가져오기에서 열을 자동으로 알아볼 수 있다.
 */
public interface DataExportService {

    /**
     * 회계 연도의 예산 전체를 파일로 내보내고 활동 기록에 남긴다.
     * 예산 하나가 한 행이며(상위 예산 포함), 구분, 단계별 예산명, 예산액, 수입액/집행액을 적는다.
     *
     * @param fiscalYearId 회계 연도 ID
     * @param target       저장할 파일
     * @param format       파일 형식
     * @param password     파일에 걸 암호. 암호를 걸지 않으면 null (엑셀 형식에만 걸 수 있음)
     * @return 내보낸 예산의 수
     */
    int exportBudgets(Long fiscalYearId, Path target, SpreadsheetFormat format, String password);

    /**
     * 회계 연도의 수입/지출 내역 전체를 파일로 내보내고 활동 기록에 남긴다.
     * 내역 하나가 한 행이며, 구분, 일자, 단계별 예산명, 금액, 내용, 봉헌자/수취인, 비고를 적는다.
     *
     * @param fiscalYearId 회계 연도 ID
     * @param target       저장할 파일
     * @param format       파일 형식
     * @param password     파일에 걸 암호. 암호를 걸지 않으면 null (엑셀 형식에만 걸 수 있음)
     * @return 내보낸 내역의 수
     */
    int exportFinancialRecords(Long fiscalYearId, Path target, SpreadsheetFormat format, String password);

    /**
     * 예산을 가져올 때 쓸 수 있는, 머리글만 있는 빈 양식을 저장한다.
     */
    void exportBudgetTemplate(Path target, SpreadsheetFormat format);

    /**
     * 수입/지출 내역을 가져올 때 쓸 수 있는, 머리글만 있는 빈 양식을 저장한다.
     */
    void exportFinancialRecordTemplate(Path target, SpreadsheetFormat format);
}
