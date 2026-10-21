package org.church.steward.domain.datatransfer.dto;

import java.util.List;
import java.util.Map;

import org.church.steward.domain.financialrecord.entity.FinancialRecordType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수입/지출 가져오기의 입력: 파일에서 읽은 행, 그 열들을 내역의 어느 항목으로 읽을지에 대한 지정,
 * 그리고 사용자가 직접 한 예산과 관계자의 매핑. 열은 0부터 세는 위치로 가리킨다.
 * <p>
 * 구분과 금액은 세 가지 방식 중 하나로 읽는다.
 * <ul>
 * <li>파일 전체가 수입 또는 지출: {@link #fixedType}과 {@link #amountColumn}</li>
 * <li>구분 열이 있음: {@link #typeColumn}과 {@link #amountColumn}</li>
 * <li>수입 금액과 지출 금액의 열이 따로 있음: {@link #incomeAmountColumn}과
 * {@link #expenseAmountColumn}</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordImportRequestDto {

    /** 머리글 아래의 데이터 행. 칸의 값은 글자, 정수, 소수, 날짜 또는 null이다. */
    private List<List<Object>> rows;

    /** 첫 데이터 행이 파일에서 몇 번째 행인지 (1부터) */
    private int firstRowNumber;

    private FinancialRecordType fixedType;
    private Integer typeColumn;
    private Integer amountColumn;
    private Integer incomeAmountColumn;
    private Integer expenseAmountColumn;

    /** 날짜가 한 칸에 적힌 열. 연/월/일이 따로 있으면 null */
    private Integer dateColumn;
    /** 연도가 적힌 열. 없으면 null이며 이때는 회계 연도의 기간으로 연도를 정한다. */
    private Integer yearColumn;
    private Integer monthColumn;
    private Integer dayColumn;

    /** 단계별 예산명이 적힌 열들 (최상위 단계부터). 전체 경로가 한 열에 적혀 있으면 빈 목록 */
    @Builder.Default
    private List<Integer> budgetLevelColumns = List.of();
    /** "예산A > 예산B"처럼 전체 경로가 적힌 열. 단계별 열을 쓰면 null */
    private Integer budgetPathColumn;

    /** 내용이 적힌 열. 없으면 null */
    private Integer descriptionColumn;
    /** 봉헌자/수취인이 적힌 열. 여러 명은 쉼표로 구분한다. 없으면 null이며 이때는 모든 행이 빈 칸으로 취급된다. */
    private Integer contactColumn;
    /** 비고가 적힌 열. 없으면 null */
    private Integer remarksColumn;

    /**
     * 사용자가 직접 한 구분 매핑. 구분 열에 적힌 값 -> 그 값을 읽을 구분.
     * "수입"/"입금"/"지출"/"출금"이 아닌 값(다른 프로그램의 "세입"/"세출" 등)을 읽으려면 여기에 지정해야 한다.
     */
    @Builder.Default
    private Map<String, FinancialRecordType> typeMappings = Map.of();

    /**
     * 사용자가 직접 한 예산 매핑. 매핑 항목의 식별자({@link ImportMappingDto#getKey()}) -> 예산 ID.
     * 자동 매핑보다 우선한다.
     */
    @Builder.Default
    private Map<String, Long> budgetMappings = Map.of();

    /**
     * 사용자가 직접 한 관계자 매핑. 매핑 항목의 식별자({@link ImportMappingDto#getKey()}) -> 관계자 ID.
     * 자동 매핑보다 우선한다.
     */
    @Builder.Default
    private Map<String, Long> contactMappings = Map.of();

}
