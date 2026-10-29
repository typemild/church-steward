package org.church.steward.domain.datatransfer.service;

import java.util.Set;

import org.church.steward.domain.datatransfer.dto.ImportResultDto;
import org.church.steward.domain.datatransfer.dto.RecordImportAnalysisDto;
import org.church.steward.domain.datatransfer.dto.RecordImportRequestDto;

/**
 * 파일에서 읽은 수입/지출 내역을 회계 연도에 가져온다. 화면은 먼저 {@link #analyze}로 행마다 가져올 수 있는지와
 * 예산·관계자의 연결을 확인하고, 연결되지 않은 값을 직접 매핑해 다시 분석한 뒤, 가져올 행을 골라
 * {@link #importRecords}로 반영한다. 서비스는 상태를 보관하지 않으므로 화면은 같은 입력으로 몇 번이든 다시 분석할 수
 * 있다.
 */
public interface RecordImportService {

    /**
     * 파일의 행들을 분석한다. 데이터를 바꾸지 않는다. 예산과 관계자는 등록된 것에만 연결하며 새로 만들지 않는다.
     *
     * @param fiscalYearId 가져올 회계 연도 ID
     * @param request      파일에서 읽은 행, 열 지정, 사용자가 직접 한 매핑
     * @throws IllegalArgumentException 회계 연도가 없거나 마감되었을 때, 열 지정이 모자랄 때 발생
     */
    RecordImportAnalysisDto analyze(Long fiscalYearId, RecordImportRequestDto request);

    /**
     * 고른 행들을 한 트랜잭션으로 등록하고 활동 기록에 "가져오기" 한 건으로 기록한다. 기존 내역은 바꾸지 않는다. 반영하기 전에 다시
     * 분석하며, 다음 경우에는 아무것도 반영하지 않고 예외를 던진다.
     * <ul>
     * <li>백업한 뒤에 활동 기록이 새로 생겼을 때: {@code BackupOutdatedException}</li>
     * <li>분석 결과가 화면에 보여준 것과 달라졌을 때: {@code ImportStateChangedException}</li>
     * </ul>
     *
     * @param fiscalYearId      가져올 회계 연도 ID
     * @param request           분석할 때 쓴 것과 같은 입력
     * @param selectedKeys      가져올 행의 식별자
     * @param expectedSignature 화면에 보여준 분석 결과의 요약 값
     * @param backupLogId       반영 전에 받은 백업의 활동 기록 ID
     * @param fileName          가져온 파일의 이름 (활동 기록에 남긴다)
     * @return 등록한 건수
     */
    ImportResultDto importRecords(Long fiscalYearId, RecordImportRequestDto request, Set<String> selectedKeys,
            String expectedSignature, long backupLogId, String fileName);
}
