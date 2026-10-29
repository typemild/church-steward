package org.church.steward.domain.activitylog.service;

import java.time.LocalDateTime;
import java.util.List;

import org.church.steward.domain.activitylog.dto.ActivityLogFieldDto;
import org.church.steward.domain.activitylog.dto.ActivityLogResponseDto;
import org.church.steward.domain.activitylog.dto.ActivityLogSearchRequestDto;
import org.church.steward.domain.activitylog.util.EntitySnapshot;
import org.springframework.data.domain.Page;

/**
 * 활동 기록(Activity Log) 저장을 위한 서비스 인터페이스다.
 */
public interface ActivityLogService {

    /**
     * 데이터 생성 내역을 기록한다.
     *
     * @param <T>    엔터티 타입
     * @param entity 로그를 기록할 엔터티
     */
    <T> void logCreation(T entity);

    /**
     * 데이터 수정 내역을 기록한다. 값이 바뀐 항목이 없으면 기록하지 않는다.
     *
     * @param <T>               엔터티 타입
     * @param oldEntitySnapshot 변경 전 엔터티 (스냅샷)
     * @param newEntity         변경 후 엔터티
     */
    <T> void logUpdate(EntitySnapshot<T> oldEntitySnapshot, T newEntity);

    /**
     * 데이터 삭제 내역을 기록한다.
     *
     * @param <T>    엔터티 타입
     * @param entity 로그를 기록할 엔터티
     */
    <T> void logDeletion(T entity);

    /**
     * 마감 내역을 기록한다. 수정 내역과 같이 식별자와 마감으로 값이 바뀐 항목을 남긴다.
     *
     * @param <T>               엔터티 타입
     * @param oldEntitySnapshot 마감 전 엔터티 (스냅샷)
     * @param newEntity         마감 후 엔터티
     */
    <T> void logClosing(EntitySnapshot<T> oldEntitySnapshot, T newEntity);

    /**
     * 마감 취소 내역을 기록한다. 수정 내역과 같이 식별자와 마감 취소로 값이 바뀐 항목을 남긴다.
     *
     * @param <T>               엔터티 타입
     * @param oldEntitySnapshot 마감 취소 전 엔터티 (스냅샷)
     * @param newEntity         마감 취소 후 엔터티
     */
    <T> void logReopening(EntitySnapshot<T> oldEntitySnapshot, T newEntity);

    /**
     * 보고서 출력 내역을 기록한다. 종이로 인쇄한 경우와 PDF로 저장한 경우를 모두 "인쇄"로 기록하고 출력 방식으로 구분한다.
     *
     * @param reportType   보고서 종류 (예시: 회계 연도 수입/지출 결산 보고서)
     * @param target       대상. 회계 연도의 이름이며 주간 보고서는 기간을 덧붙인다 (예시: 2026년)
     * @param outputMethod 출력 방식 (예시: 인쇄, PDF 저장)
     */
    void logReportPrint(String reportType, String target, String outputMethod);

    /**
     * 전체 데이터를 백업한 내역을 기록한다.
     *
     * @param fileName 백업 파일의 이름
     */
    void logBackup(String fileName);

    /**
     * 백업 파일로 전체 데이터를 복원한 내역을 복원한 데이터베이스에 기록한다. 복원은 로그인하기 전에 하므로 로그인 세션이 아니라 복원한
     * 계정(관리자)으로 기록한다.
     *
     * @param userId          복원한 사용자의 ID (백업한 시점의 계정)
     * @param fileName        복원에 쓴 백업 파일의 이름
     * @param backupCreatedAt 그 백업을 만든 일시
     */
    void logRestore(String userId, String fileName, LocalDateTime backupCreatedAt);

    /**
     * 데이터를 파일로 내보낸 내역을 기록한다.
     *
     * @param targetName     내보낸 대상 (예시: 예산, 수입/지출, 관계자)
     * @param fiscalYearName 회계 연도의 이름. 관계자처럼 회계 연도에 속하지 않는 대상이면 null이며, 이때는 회계 연도
     *                       항목을 남기지 않는다.
     * @param fileName       내보낸 파일의 이름
     * @param count          내보낸 건수
     */
    void logExport(String targetName, String fiscalYearName, String fileName, int count);

    /**
     * 파일에서 데이터를 가져온 내역을 한 건으로 기록한다. 가져오기로 생성/수정된 엔터티의 내역을 건별 항목 목록에 담는다.
     *
     * @param targetName     가져온 대상 (예시: 예산, 수입/지출, 관계자)
     * @param fiscalYearName 회계 연도의 이름. 관계자처럼 회계 연도에 속하지 않는 대상이면 null이며, 이때는 회계 연도
     *                       항목을 남기지 않는다.
     * @param fileName       가져온 파일의 이름
     * @param result         처리 결과의 요약 (예시: 추가 12건, 금액 갱신 3건)
     * @param items          건별 항목 목록. {@link #describeCreation}과
     *                       {@link #describeUpdate}로 만든다.
     */
    void logImport(String targetName, String fiscalYearName, String fileName, String result,
            List<List<ActivityLogFieldDto>> items);

    /**
     * 엔터티를 생성한 내역에 기록할 항목 목록을 만든다. {@link #logCreation}이 기록하는 내용과 같다.
     *
     * @param <T>    엔터티 타입
     * @param entity 생성된 엔터티
     */
    <T> List<ActivityLogFieldDto> describeCreation(T entity);

    /**
     * 엔터티를 수정한 내역에 기록할 항목 목록(식별자와 값이 바뀐 항목)을 만든다. {@link #logUpdate}가 기록하는 내용과 같다.
     *
     * @param <T>               엔터티 타입
     * @param oldEntitySnapshot 수정 전 엔터티 (스냅샷)
     * @param newEntity         수정 후 엔터티
     */
    <T> List<ActivityLogFieldDto> describeUpdate(EntitySnapshot<T> oldEntitySnapshot, T newEntity);

    /**
     * 가장 마지막에 남긴 활동 기록의 ID를 조회한다. 활동 기록이 하나도 없으면 0이다. 데이터를 바꾸는 모든 작업은 활동 기록을 남기므로,
     * 이 값이 그대로이면 그 사이에 데이터가 바뀌지 않은 것이다.
     */
    long getLatestLogId();

    /**
     * 로그인 내역을 기록한다.
     *
     * @param userId 로그인한 사용자의 ID
     */
    void logLogin(String userId);

    /**
     * 활동 이력을 가장 최근 것부터 조회한다.
     *
     * @param request 조회 조건
     * @param page    페이지 번호 (0부터 시작)
     * @param size    페이지 크기
     * @return 조회된 활동 이력 페이지
     */
    Page<ActivityLogResponseDto> getLogs(ActivityLogSearchRequestDto request, int page, int size);
}
